package com.funambol.LDAP.engine.source;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.funambol.LDAP.dao.impl.ItemMapImpl;
import com.funambol.LDAP.exception.DBAccessException;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.LDAPManagerFactory;
import com.funambol.LDAP.manager.LdapManagerInterface;
import com.funambol.LDAP.manager.impl.LDAPState;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.common.pim.contact.Contact;
import com.funambol.framework.core.AlertCode;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemImpl;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.SyncItemState;
import com.funambol.framework.engine.source.SyncContext;
import com.funambol.framework.engine.source.SyncSource;
import com.funambol.framework.engine.source.SyncSourceException;

/**
 * this class improves the stantard Ldap SyncSource behavior
 * 
 * Roadmap:
 * - support for soft delete
 * - support for backup sync source (just store entries on ldap)
 * @author rpolli
 *
 */
public class ExtendedLDAPContactsSyncSource extends LDAPContactsSyncSource
implements SyncSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1848317821216464209L;

	protected HashMap<String,String> cache;
	protected ItemMapImpl itemMap; 

	private String sinceUtc;
	public String getSinceUtc() {
		return sinceUtc;
	}

	public ExtendedLDAPContactsSyncSource() {
		super(null);
	}
	
	public ExtendedLDAPContactsSyncSource(String name) {
		super(name);
	}
	
	public void beginSync(SyncContext syncContext) throws SyncSourceException {
		super.beginSync(syncContext);

		try {
			syncLogic(syncContext);
		} catch (DBAccessException e) {
			throw new SyncSourceException(e);
		}

	}
	
    /**
     * @see super
	 */
	public boolean initializeManager() {
		if (logger.isDebugEnabled())
			logger.debug("initializeManager()");

		boolean ret = false;
		try {
			// replace standard field in providerUrl and baseDn: %D, %u, %d,  %s, %e, %principal, psRoot
			expandSearchAndBaseDn(principal);

			// create stored Cache
			itemMap = new ItemMapImpl();
			itemMap.setPrincipal(getPrincipal().getId());
			itemMap.setSourceUri(this.getSourceURI());
			itemMap.setUsername(getPrincipal().getUsername());
			itemMap.init();


			// Create Ldap Interface
			LdapManagerInterface li = LDAPManagerFactory.createLdapInterface(getLdapInterfaceClassName());
			li.init(getProviderUrl(), baseDn, ldapUser, ldapPass, isFollowReferral(), isConnectionPooling(), contactDAO);
			li.setLdapId(contactDAO.getRdnAttribute());
			li.setTimeZone(serverTimeZone);

			logger.info("createSyncSource[new LDAPState]");
			ldapState = new LDAPState(li, getDbName());
			if (ldapState!=null) {
				ret = true;
			}

		} catch (SyncSourceException e) {
			logger.warn("LDAPInterface somehow failed, please check your settings");
			logger.warn("Ldapinterface error:", e);
		} catch (NamingException e) {
			logger.warn("Can't bind to DataSource");
			logger.warn("DBerror:", e);
		} catch (LDAPAccessException e) {
			logger.warn("LDAPInterface somehow failed, please check your settings: " ,e);
		} 

		if (logger.isInfoEnabled())
			logger.info("AbstractLDAP init called. dbName:" + getDbName());
		
		return ret;
	}

	
	
	/**
	 * This method describes the sync logic to be applied to various sync modes
	 * @param context
	 * @throws SyncSourceException
	 * @throws DBAccessException 
	 */
	protected void syncLogic(SyncContext context) throws SyncSourceException, DBAccessException {
		
		// first of all pass the syncSource entryFilter to the LdapManager
		ldapState.setEntryFilter(getEntryFilter());

		try {

			if (syncMode == AlertCode.SLOW || 
					syncMode == AlertCode.REFRESH_FROM_SERVER ||
					syncMode == AlertCode.REFRESH_FROM_SERVER_BY_SERVER) {

				// get all from -server (no updates) - performed by the server calls    

				allUids =  ldapState.getAllEntriesLdapId();
				newUids = allUids;
				// clear cache from database
				itemMap.clearMap();

				// no "updates" method required 

			} else if (syncMode == AlertCode.REFRESH_FROM_CLIENT || 
					syncMode == AlertCode.REFRESH_FROM_CLIENT_BY_SERVER ) {

				// physically remove all item from server            
				for (String dn: ldapState.getAllEntries(true)) {
					ldapState.delete(dn, false);
				}

				// clear cache from database
				itemMap.clearMap();

				// no "updates" method required 

			} else if (syncMode == AlertCode.ONE_WAY_FROM_CLIENT || 
					syncMode == AlertCode.ONE_WAY_FROM_CLIENT_BY_SERVER ) {

				// no operation performed by the server

			} else {

				// ONE_WAY_FROM_SERVER 
				// ONE_WAY_FROM_SERVER_BY_SERVER
				// TWO_WAY
				// TWO_WAY_BY_SERVER               

				// get "updates" from server ...
				// we cannot get the "updates" from the server because we need the
				// since .. see getNewItems for instance
				// we cannot assume that since = previous Start SyncTime
				// the new ds-server + server-framework will provide the "since" 
				// and the "to" values in the beginSync
				Timestamp since = context.getSince();
	//			Timestamp to = context.getTo();
				// Timestamp to    = new Timestamp(System.currentTimeMillis());

				// get NUD items from ldap
				if (since==null) {
					itemMap.clearMap();
				}
				cache = ldapState.getManager().getLastModifiedMap(getEntryFilter());


				// N,U,D
				allUids =  new ArrayList<String> (cache.keySet());
				newUids.addAll(allUids);
				
				HashMap<String,String> storedCache =  itemMap.loadMap();
				List<String> oldKeys =  new ArrayList<String> (storedCache.keySet());

				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Found %d items on server\n" +
							"and %d items on db",
							cache.size(), storedCache.size())
					);
				}

				// check if old items are still on server
				for (String n : oldKeys) {
					if (cache.get(n) != null) {
						// if so, then check etags of changed
						if ( ! cache.get(n).equals(storedCache.get(n))) {
							if (logger.isTraceEnabled())
								logger.trace("adding event "+ n +" to update map: ("+cache.get(n)+";"+ storedCache.get(n)+")");

							updatedUids.add(n);
						}
					} else {
						// is deleted
						deletedUids.add(n);
					}
				}		
				// newItems aren't on storedEtag
				newUids.removeAll(oldKeys);	

			}

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Items on server: %d\n" +
						"New items: %s\n" +
						"Items to update: %d\n" +
						"Items to delete: %d\n"  , 
						allUids.size(), newUids.size(), updatedUids.size(), deletedUids.size()));
			}
		} catch (LDAPAccessException e) {
			throw new SyncSourceException("Can't connect to LDAP: " + e.getMessage() );
		} catch (NamingException e) {
			// e.printStackTrace();
			if (logger.isErrorEnabled()) {
				logger.error(e.getMessage());
			}
			throw new SyncSourceException("LDAP error: " , e);
		}
	}

	@Override
	public SyncItemKey[] getAllSyncItemKeys() throws SyncSourceException {				
		return LdapUtils.ListToSyncItemKey(allUids);
	}
	
	@Override
	public SyncItemKey[] getDeletedSyncItemKeys(Timestamp arg0, Timestamp arg1) throws SyncSourceException {
		return LdapUtils.ListToSyncItemKey(deletedUids);
	}
	
	@Override
	public SyncItemKey[] getNewSyncItemKeys(Timestamp arg0, Timestamp arg1) throws SyncSourceException {
		return LdapUtils.ListToSyncItemKey(newUids);
	}
	
	@Override
	public SyncItemKey[] getUpdatedSyncItemKeys(Timestamp arg0, Timestamp arg1) throws SyncSourceException {
		return LdapUtils.ListToSyncItemKey(updatedUids);
	}
	
	/**
	 * Update syncitem stores funambol timestamp on a custom ldap field
	 */
	@Override
	public SyncItem getSyncItemFromId(SyncItemKey syncItemKey)
	throws SyncSourceException {

		// retrieve the item from ldap
		// set to 0 the piModifiedBy
		SyncItem syncItem = null;

		if (logger.isInfoEnabled())
			logger.info("getSyncItemsFromId(" +
					principal +
					", " +
					syncItemKey +
			")");
		try {

			String uid = syncItemKey.getKeyAsString();
			if (uid != null) {
				Attributes entry;
				entry = ldapState.getManager().getLDAPEntryById(uid);

				if (entry != null) {
					Contact c = contactDAO.createContact(entry);
					syncItem = createItem(contactDAO.getRdnValue(entry), c, SyncItemState.NEW);
				} else {
					logger.warn("Unable to get the LDAP entry for id: " + uid);
				}
			}			
		} catch (LDAPAccessException e) {
			throw new SyncSourceException("Can't connect to LDAP: " + e.getMessage());
		}				
		return syncItem;
	}

	/**
	 * Add syncitem stores funambol timestamp on a custom ldap field
	 */

	/**
	 * Retrieval of syncItems has a further filter for
	 * - get new item
	 * - get updated item
	 */

	@Override
	public void endSync() throws SyncSourceException {
		super.endSync();
		
		try {
			if (itemMap == null) {
				throw new SyncSourceException("null itemMap. Maybe uninitialized in beginSync?");
			}
			
			if (ldapState == null) {								
				throw new SyncSourceException("null ldapState. Maybe uninitialized in beginSync?");
			}
				
				
			itemMap.updateMap(ldapState.getManager().getCache());
			
		} catch (DBAccessException e) {
			throw new SyncSourceException(e);
		}
	}
	
	
	@Override
	public void removeSyncItem(SyncItemKey syncItemKey, Timestamp time,
			boolean softDelete) throws SyncSourceException {
		String uid = syncItemKey.getKeyAsString();
		logger.info("Removing item with key : " + uid);
		LdapManagerInterface manager = ldapState.getManager();
		
		manager.deleteEntry(new SyncItemImpl(this,syncItemKey.getKeyValue()), isSoftDelete());

	}
	
	@Override
	public SyncItemKey[] getSyncItemKeysFromTwin(SyncItem syncItem)
			throws SyncSourceException {
		/*
		 * connect to ldap and find an item matching with the given one
		 * 
		 * I need to implement a method to retrieve matching ldap items
		 */
		LdapManagerInterface manager = ldapState.getManager();
		List<String> twinList = manager.getTwins(syncItem);
		if (twinList != null) {
			return LdapUtils.ListToSyncItemKey(twinList);
		} else {
			return super.getSyncItemKeysFromTwin(syncItem);
		}
	}
}
