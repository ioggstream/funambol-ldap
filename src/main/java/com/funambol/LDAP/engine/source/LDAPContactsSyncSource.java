package com.funambol.LDAP.engine.source;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.funambol.LDAP.converter.ContactToVcard3;
import com.funambol.LDAP.dao.impl.ItemMapImpl;
import com.funambol.LDAP.exception.DBAccessException;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.LDAPManagerFactory;
import com.funambol.LDAP.manager.LdapManagerInterface;
import com.funambol.LDAP.manager.impl.LDAPState;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.converter.ContactToSIFC;
import com.funambol.common.pim.converter.ContactToVcard;
import com.funambol.common.pim.converter.ConverterException;
import com.funambol.framework.core.AlertCode;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemImpl;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.SyncItemState;
import com.funambol.framework.engine.source.SyncContext;
import com.funambol.framework.engine.source.SyncSource;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.tools.Base64;
import com.funambol.framework.tools.beans.LazyInitBean;

/**
 * this class improves the stantard Ldap SyncSource behavior
 * 
 * Roadmap:
 * - support for soft delete
 * - support for backup sync source (just store entries on ldap)
 * @author rpolli
 *
 * This class is a reimplementation of the old LDAPContactsSyncSource (thx to)
 * @author  <a href='mailto:fipsfuchs _@ users.sf.net'>Philipp Kamps</a>
 * @author  <a href='mailto:julien.buratto _@ subitosms.it'>Julien Buratto</a>
 * @author  <a href='mailto:gdartigu _@ smartjog.com'>Gilles Dartiguelongue</a>

 *
 */
public class LDAPContactsSyncSource extends AbstractLDAPSyncSource 
implements SyncSource, Serializable, LazyInitBean {

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

	public LDAPContactsSyncSource() {
		super(null);
	}
	
	public LDAPContactsSyncSource(String name) {
		super(name);
	}
	
	public void beginSync(SyncContext syncContext) throws SyncSourceException {
		logger.info("LDAPContactsSyncSource beginSync");
		super.beginSync(syncContext);

		try {
			syncLogic(syncContext);
		} catch (DBAccessException e) {
			throw new SyncSourceException(e);
		}
		logger.info("LDAPContactsSyncSource beginSync end");
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
		logger.info("syncLogic()");
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

				if (logger.isInfoEnabled()) {
					logger.info(String.format("Found %d items on server\n" +
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
		logger.info("syncLogic end");
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
	
	
	
	
	
	
	
	/**
	 * @see AbstractLDAPSyncSource
	 */
	protected SyncItem[] setSyncItems(SyncItem[] syncItems, Contact contact)
	throws SyncSourceException {
		if (logger.isInfoEnabled())
			logger.info("setSyncItems(" + principal + " , ...)");

		SyncItem[] ret = new SyncItem[syncItems.length];
		for (int i = 0; i < syncItems.length; ++i) {
			ret[i] = new SyncItemImpl(this, syncItems[i].getKey().getKeyAsString() + "-1");
		}
		
		return ret;
	}

	/**
	 * @see AbstractLDAPSyncSource
	 */
	protected List<SyncItem> getSyncItems(List<Contact> contacts, char state)
	throws SyncSourceException {
		List<SyncItem> syncItems = new ArrayList<SyncItem>();
		if (logger.isInfoEnabled())
			logger.info("Count of contacts " + contacts.size());

		Iterator<Contact> it = contacts.iterator();

		while (it.hasNext()) {
			Contact ct = it.next();
			if (ct != null && ct.getUid() != null) {
				syncItems.add(createItem(ct.getUid(), ct, state));
			} else {
				logger.warn("Found a null contact or key");
			}
		}
		
		return syncItems;
	}

	// ------------------------------------------------------------ Private methods

	/**
	 * Creates a <i>SyncItem</i> with the given state from a <i>Contact</i> object.
	 * @param contact data to create the <i>SyncItem</i>
	 * @param state State of the synchronisation of this contact
	 * @return <i>SyncItem</i> associated with the contact
	 * @throws SyncSourceException
	 */
	public SyncItem createItem(String uid, Contact contact,  char state)
	throws SyncSourceException {
		SyncItem syncItem = null;
		String content = null;
		if (uid == null)
			throw new SyncSourceException("Can't create a syncitem with null key");
		
		if (logger.isInfoEnabled())
			logger.info("createItem(" +  uid + " , ...)");
		
		syncItem = new SyncItemImpl(this, uid, state);
		
		if (info.getPreferredType().type.equals("text/x-vcard")) {
			content = contact2vcard(contact);
		} else if (info.getPreferredType().type.equals("text/vcard")) {
			content = contact2vcard3(contact);
		} else {
			content = contact2sifc(contact);
		}
		
		if (false)// || isEncode()) 
		{
			syncItem.setContent(Base64.encode((content).getBytes()));
			syncItem.setType(info.getPreferredType().type);
			syncItem.setFormat("b64");
		} else {
			syncItem.setContent(content.getBytes());
			syncItem.setType(info.getPreferredType().type);
		}

		return syncItem;
	}

	/**
	 * Converts the given contact into a vcard String
	 * @param c the contact to convert
	 * @return the vcard
	 * @throws SyncSourceException in case of convertion errors
	 */
	private String contact2vcard(Contact c) throws SyncSourceException {
		if (logger.isDebugEnabled())
			logger.debug("contact2vcard(" + c.getUid() + " , ...)");
		
		try {
			return new ContactToVcard( serverTimeZone , null).convert(c);
		} catch (Exception e) {
			throw new SyncSourceException("Conversion error for item "
					+ c.getUid() + ": " + e.getMessage(), e);
		}
	}
	/**
	 * Converts the given contact into a vcard String
	 * @param c the contact to convert
	 * @return the vcard
	 * @throws SyncSourceException in case of convertion errors
	 */
	private String contact2vcard3(Contact c) throws SyncSourceException {
		if (logger.isDebugEnabled())
			logger.debug("contact2vcard3(" + c.getUid() + " , ...)");
		
		try {
			return new ContactToVcard3( serverTimeZone , null).convert(c);
		} catch (Exception e) {
			throw new SyncSourceException("Conversion error for item "
					+ c.getUid() + ": " + e.getMessage(), e);
		}
	}
	/**
	 * Converts the given contact into a sifc String
	 * @param c the contact to convert
	 * @return the sifc document
	 * @throws SyncSourceException in case of convertion errors
	 */
	private String contact2sifc(Contact c) throws SyncSourceException {
		if (logger.isDebugEnabled())
			logger.debug("contact2sifc(" + c.getUid() + " , ...)");
		
		try {
			return new ContactToSIFC(null, null).convert(c);
		} catch (ConverterException e) {
			e.printStackTrace();
			throw new SyncSourceException("Convertion error for item "
					+ c.getUid() + ": " + e.getMessage(), e);
		}
	}
	
    /**
     * Extracts the content from a syncItem.
     *
     * @param syncItem
     * @return as a String object (same as
     *         PIMSyncSource#getContentFromSyncItem(String), but trimmed)
     * @see PIMContactSyncSource.java
     */
    protected String getContentFromSyncItem(SyncItem syncItem) {

        String raw = super.getContentFromSyncItem(syncItem);

        return raw.trim();
    }



	
}
