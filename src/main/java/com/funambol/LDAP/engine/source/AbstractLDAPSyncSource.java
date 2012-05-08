/**
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA 
 */
package com.funambol.LDAP.engine.source;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.naming.directory.Attributes;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.LDAPManagerFactory;
import com.funambol.LDAP.manager.LdapManagerInterface;
import com.funambol.LDAP.manager.impl.LDAPState;
import com.funambol.LDAP.security.LDAPUser;
import com.funambol.LDAP.utils.Constants;
import com.funambol.common.pim.contact.Contact;
import com.funambol.framework.core.AlertCode;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemImpl;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.SyncItemState;
import com.funambol.framework.engine.source.AbstractSyncSource;
import com.funambol.framework.engine.source.SyncContext;
import com.funambol.framework.engine.source.SyncSource;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.framework.security.Sync4jPrincipal;
import com.funambol.framework.server.Sync4jDevice;
import com.funambol.framework.server.store.PersistentStore;
import com.funambol.framework.server.store.PersistentStoreException;
import com.funambol.framework.tools.beans.LazyInitBean;
import com.funambol.server.config.Configuration;

/**
 * This class implements a LDAP <i>SyncSource</i>
 *
 * @author  <a href='mailto:fipsfuchs _@ users.sf.net'>Philipp Kamps</a>
 * @author  <a href='mailto:julien.buratto _@ subitosms.it'>Julien Buratto</a>
 * @author  <a href='mailto:gdartigu _@ smartjog.com'>Gilles Dartiguelongue</a>
 * @author  <a href='mailto:robipolli _@ gmail.com'>Roberto Polli</a>
 * @version $Id$
 *
 * Change log:
 * - Julien Buratto: reintroduced the use of %principal to include the principal ID in the sync tree (ldap base)
 * - Julien Buratto: introduced %e to include the email address in the sync tree (ldap base)
 */
public abstract class AbstractLDAPSyncSource extends AbstractSyncSource
implements SyncSource, Serializable, LazyInitBean {
	// ----------------------------------------------------------- Constants

	/**
	 * 
	 */
	private static final long serialVersionUID = -6842763250018970751L;
	protected FunambolLogger logger = FunambolLoggerFactory.getLogger(Constants.LOGGER_LDAP_SOURCE);


	// -------------------------------------------------------- Protected Bean Data
	protected TimeZone serverTimeZone = null;
	protected String baseDn;
	protected String ldapUser;
	protected String ldapPass;

	//
	// dynamic data
	//
	protected int syncMode;

	// manager
	protected ContactDAOInterface contactDAO = null;
	protected LDAPState ldapState = null;
	
	/**
	 * softDelete should be enabled and supported by the DAO
	 */
	private boolean softDelete = false;
	public boolean isSoftDelete() {
		return softDelete;
	}
	public void setSoftDelete(boolean softDelete) {
		this.softDelete = softDelete;
	}

	private String entryFilter;
	public void setEntryFilter(String entryFilter) {
		this.entryFilter = entryFilter;
	}
	public String getEntryFilter() {
		return entryFilter;
	}

	private String providerUrl;
	public String getProviderUrl() {
		return providerUrl;
	}
	public void setProviderUrl(String providerUrl) {
		this.providerUrl = providerUrl;
	}

	protected String userSearch = null;
	public void setUserSearch(String s) {
		userSearch=s;
	}
	public String getUserSearch() {		
		return (userSearch!=null) ?  userSearch : "";
	}

	private String ldapInterfaceClassName;
	public void setLdapInterfaceClassName(String ldapInterfaceClassName) {
		this.ldapInterfaceClassName = ldapInterfaceClassName;
	}
	public String getLdapInterfaceClassName() {
		return ldapInterfaceClassName;
	}

	private String daoName;
	public String getDaoName() {
		return daoName;
	}
	public void setDaoName(String daoName) {
		this.daoName = daoName;
	}
	protected String deviceTimeZoneDescription = null;
	protected TimeZone deviceTimeZone = null;
	public TimeZone getDeviceTimeZone() {
		return deviceTimeZone;
	}
	public void setDeviceTimeZone(TimeZone deviceTimeZone) {
		this.deviceTimeZone = deviceTimeZone;
	}
	private String dbName;
	private boolean followReferral;
	private boolean connectionPooling;

	public void setFollowReferral(boolean followReferral) {
		this.followReferral = followReferral;
	}
	public boolean isFollowReferral() {
		return followReferral;
	}
	public void setConnectionPooling(boolean isConnectionPooling) {
		this.connectionPooling = isConnectionPooling;
	}
	public boolean isConnectionPooling() {
		return connectionPooling;
	}

	private String deviceCharset = null;
	public String getDeviceCharset() {
		return deviceCharset;
	}
	public void setDeviceCharset(String deviceCharset) {
		this.deviceCharset = deviceCharset;
	}
	protected Sync4jPrincipal principal = null;
	public Sync4jPrincipal getPrincipal() {
		if (logger.isTraceEnabled()) {
			logger.trace("getPrincipal(" + principal + " , ...)");
			if (principal != null)
				logger.trace("my username is [" + principal.getUsername() + "]"); //rpolli
		}
		return principal;
	}
	public void setPrincipal(Sync4jPrincipal principal) {
		this.principal = principal;
	}
	/**
	 * The context of the sync
	 */
	protected SyncContext syncContext = null;
	protected SyncContext getSyncContext() {
		if (logger.isDebugEnabled())
			logger.debug("getSyncContext(" + syncContext + " , ...)");
		return syncContext;
	}



	// TODO: cache variables. think about how to implement a cache mechanism
	protected List<SyncItem> allItems = new ArrayList<SyncItem>();
	protected List<SyncItem> newItems = new ArrayList<SyncItem>();
	protected List<SyncItem> deletedItems = new ArrayList<SyncItem>();
	protected List<SyncItem> updatedItems = new ArrayList<SyncItem>();

	protected List<String> allUids = new ArrayList<String>();
	protected List<String> newUids = new ArrayList<String>();
	protected List<String> deletedUids = new ArrayList<String>();
	protected List<String> updatedUids = new ArrayList<String>();
	private boolean initialized;

	// ---------------------------------------------------------- Properties

	/**
	 * Set the class to use ad ContactDAO
	 * - PiTypeContactDAO
	 * - ContactDAO
	 */
	protected void initContactDAO(String className) throws SyncSourceException {
		if (logger.isDebugEnabled()) {
			logger.debug("Getting class instance: " + getDaoName());
		}
		this.contactDAO = (ContactDAOInterface) LDAPManagerFactory.createContactDAO(getDaoName());
	}


	// -------------------------------------------------------- Constructors
	public AbstractLDAPSyncSource() {
		super();
		if (logger.isDebugEnabled())
			logger.debug("AbstractLDAPSyncSource()");
	}

	public AbstractLDAPSyncSource(String name) {
		super(name);
		if (logger.isDebugEnabled())
			logger.debug("AbstractLDAPSyncSource(" + name + " , ...)");
	}

	// ------------------------------------------------------ Public methods
	/**
	 * Initialize the ContactDAO and manage soft delete filter
	 */
	public void init() {
		if (!initialized) { // needed to initialize bean in FCTF
			/*
			 * this is a lazy init bean: here you should set attributes
			 * 1- not set by the standard bean
			 * 2- independent from the principal (eg. user), which is still null
			 */

			if (logger.isInfoEnabled()) {
				logger.info("AbstractLDAPSyncSource.init()");
			}

			if (getDaoName() != null) {
				try {
					initContactDAO(getDaoName());
					
					if (isSoftDelete() && contactDAO.getSoftDeleteAttribute() != null) {
						setEntryFilter(String.format("(&%s%s)", getEntryFilter(),contactDAO.getSoftDeleteFilter() ));	
					}
					
				} catch (SyncSourceException e) {
					if (logger.isErrorEnabled())
						logger.error(e.getMessage());
				}
			}

			if (getLdapInterfaceClassName() == null) {
				logger.error("No LDAP Server type retrieved!!!");
			}
			initialized = true;
		}
	}
	/**
	 * Adds a SyncItem object (representing a contact).
	 *
	 * @param syncItem the SyncItem representing the contact
	 *
	 * @return a newly created syncItem based on the input object but with its
	 *         status set at SyncItemState.NEW and the GUID retrieved by the
	 *         ldapId of the newly created LDAP entry
	 */
	public SyncItem addSyncItem(SyncItem syncItem)
	throws SyncSourceException {
		logger.info("addSyncItem(" + principal + " , " + syncItem.getKey().getKeyAsString() + ")");

		try {
			SyncItem itemOnServer = getSyncItemFromId(syncItem.getKey());

			// Adds the contact, wraps it in sync information and uses it to
			// create a new SyncItem which is the return value of this method
			if (itemOnServer != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Error: item still present on server" + 
							syncItem);
				}
				throw new SyncSourceException("Error: item still present on server" + syncItem.getKey());
			}

			String content = getContentFromSyncItem(syncItem);
			String contentType = syncItem.getType();

			SyncItemImpl serverSyncItem = new SyncItemImpl(
					this,//syncSource
					syncItem.getKey().getKeyAsString(), //key
					null, //mappedKey
					SyncItemState.NEW,//state
					content.getBytes(), //content
					null, //format
					contentType, //type
					syncItem.getTimestamp() //timestamp
			);
			serverSyncItem.setType(info.getPreferredType().type);

			++howManyAdded;
			ArrayList<SyncItem> aSyncItem = new ArrayList<SyncItem>();
			aSyncItem.add(serverSyncItem);

			ldapState.commitNewItem(aSyncItem);


			serverSyncItem.setType(info.getPreferredType().type);

			//allItems.add(serverSyncItem);
			newItems.add(serverSyncItem); // here just to avoid another ldapsearch

			return serverSyncItem;
		} catch (Exception e) {
			// TODO here we must put an LDAPStateException to make this class independent from the chosen driver
			// TODO add logging
			e.printStackTrace();
			throw new SyncSourceException("Error adding the item " + syncItem +
					"LDAP Error: ", e);
		}
	}

	/**
	 * Updates a SyncItem object (representing a contact).
	 *
	 * @param syncItem the SyncItem representing the contact
	 *
	 * @return a newly created syncItem based on the input object but with its
	 *         status set at SyncItemState.UPDATED and the GUID retrieved by the
	 *         back-end
	 */
	public SyncItem updateSyncItem(SyncItem syncItem) throws SyncSourceException {
		logger.info("updateSyncItem(" + principal + " , " + syncItem.getKey().getKeyAsString() + ")");

		SyncItem itemOnServer = getSyncItemFromId(syncItem.getKey());

		if (itemOnServer != null) {
			String content = getContentFromSyncItem(syncItem);

			SyncItemImpl serverSyncItem = new SyncItemImpl(
					this,//syncSource
					syncItem.getKey().getKeyAsString(), //key
					//					syncItem.getParentKey()	, // not specified in
					null, //mappedKey
					SyncItemState.UPDATED,//state
					content.getBytes(), //content
					syncItem.getFormat(), //format
					syncItem.getType(), //type
					null //timestamp
			);

			++howManyUpdated;
			ArrayList<SyncItem> aSyncItem = new ArrayList<SyncItem>();
			aSyncItem.add(serverSyncItem);
			ldapState.commitUpdatedItems(aSyncItem);

			// updatedItems.add(syncItem);
			// itemOnServer = syncItem;
			syncItem.setType(info.getPreferredType().type);
			return syncItem;
		} else {
			throw new SyncSourceException("Item is not on server");
		}
	}

	/**
	 * get item with the given GUID
	 * uses allItems as a cache to avoid further ldapsearch
	 * TODO this method searches twice: syncItemKey -> ldapUid -> ldapItem
	 * @see SyncSource
	 */
	public SyncItem getSyncItemFromId(SyncItemKey syncItemKey)
	throws SyncSourceException {
		String id = null;
		SyncItem syncItem = null;

		if (logger.isInfoEnabled())
			logger.info("getSyncItemsFromId(" +
					principal +
					", " +
					syncItemKey +
			")");
		List<String> item = ldapState.getOneItem(syncItemKey);		// this method is redundant, search directly for the key
		if (! item.isEmpty()) {
			Attributes entry = ldapState.getLDAPEntryById(item.get(0));
			if (entry != null) {
				Contact c = contactDAO.createContact(entry);
				syncItem = createItem(syncItemKey.getKeyAsString(), c, SyncItemState.NEW);
			} else {
				logger.warn("Unable to get the LDAP entry for id: " + id);
			}

		}
		return syncItem;
		//return getSyncItems(contactDAO.getContact( ldapState.getOneItem(syncItemKey).get(0) ), SyncItemState.NEW);
	}

	/**
	 * get the item GUIDs newly created on server:
	 * it freshen the newItems array
	 * TODO come gestisce il timezone questa classe?
	 * @see SyncSource
	 * @return GUIDs
	 */
	public SyncItemKey[] getNewSyncItemKeys(Timestamp since, Timestamp until)
	throws SyncSourceException {
		logger.info("getNewSyncItemKeys(" + principal + " , " + since + " , " + until + ")");
		Timestamp sinceUTC = since;

		// timestamp to UTC if the device has a timezone. FIXME this should'n work
		if (deviceTimeZone != null) {
			sinceUTC = normalizeTimestamp(since);
		}

		if (syncMode == AlertCode.ONE_WAY_FROM_SERVER) {
			return getAllSyncItemKeys();
		}

		List<String> ids = ldapState.getNewItems(sinceUTC, until);
		logger.info("Get NEW sync items ( " + ids.size() + " ) since ( " + sinceUTC + " ) until ( " + until + " )");

		List<Contact> contacts = new ArrayList<Contact>();
		for (String id: ids) {
			Attributes entry = ldapState.getLDAPEntryById(id);
			contacts.add(contactDAO.createContact(entry));
		}
		newItems = getSyncItems(contacts, SyncItemState.NEW);

		return extractKeys(newItems);
	}

	/**
	 * @see SyncSource
	 */
	public SyncItemKey[] getDeletedSyncItemKeys(Timestamp since, Timestamp until)
	throws SyncSourceException {
		logger.info("getDeletedSyncItemKeys(" + principal + " , " + since + " , " + until + ")");

		if (deviceTimeZone != null) {
			since = normalizeTimestamp(since);
		}

		List<String> ids = ldapState.getDeletedItems(this, since);

		logger.info("Got DELETED sync items ( " + ids.size() + " ) since ( " + since + " )");

		deletedItems = new ArrayList<SyncItem>();

		Iterator<String> it = ids.iterator();
		while (it.hasNext()) {
			deletedItems.add(new SyncItemImpl(this, it.next(), SyncItemState.DELETED));
		}

		return extractKeys(deletedItems);
	}

	/**
	 * @see SyncSource
	 * @return GUIDs of updated items
	 */
	public SyncItemKey[] getUpdatedSyncItemKeys(Timestamp since, Timestamp until)
	throws SyncSourceException {
		logger.info("getUpdatedSyncItemKeys(" + principal + " , " + since + " , " + until + ")");

		if (deviceTimeZone != null) {
			since = normalizeTimestamp(since);
		}

		List<String> ids = ldapState.getModifiedItems(since, until);
		logger.info("Get UPDATED sync items ( " + ids.size() + " ) since ( " + since + " )");
		List<Contact> contacts = new ArrayList<Contact>();
		for (String id: ids) {
			Attributes entry = ldapState.getLDAPEntryById(id);
			contacts.add(contactDAO.createContact(entry));
		}
		updatedItems = getSyncItems(contacts, SyncItemState.UPDATED);
		return extractKeys(updatedItems);
	}

	/**
	 * 1- fill allItems with all the LDAP Contacts
	 * 2- return an array of GUID
	 * @see SyncSource
	 * @return array of GUID
	 */
	/*public SyncItemKey[] getAllSyncItemKeys_ori()
    throws SyncSourceException {
    logger.info("getAllSyncItemKeys(" + principal + ")");

    List<String> ids = ldapState.getAllEntriesLdapId(); // this function returns the LdapId of the entries
    logger.info("Got ALL sync items ( " + ids.size() + " )");
    allItems = getSyncItems(contactDAO.getContacts(ids), SyncItemState.NEW);

    SyncItemKey[] keys = new SyncItemKey[allItems.size()];

    for (int i=0; i<allItems.size(); i++) {
    keys[i] = allItems.get(i).getKey();
    }
    return keys; //extractKeys(allItems);
    }*/
	/**
	 * 1- fill allItems with all the LDAP Contacts
	 * 2- return an array of GUID
	 * @see SyncSource
	 * @return array of GUID
	 */
	public SyncItemKey[] getAllSyncItemKeys()
	throws SyncSourceException {
		logger.info("getAllSyncItemKeys(" + principal + ")");

		List<String> ids = ldapState.getAllEntriesLdapId(); // this function returns the LdapId of the entries. this is bad
		if (logger.isInfoEnabled())
			logger.info("Got ALL sync items ( " + ids.size() + " )");
		//		allItems = getSyncItems(contactDAO.getContacts(ids), SyncItemState.NEW);

		SyncItemKey[] keys = new SyncItemKey[ids.size()];

		for (int i = 0; i < ids.size(); i++) {
			keys[i] = new SyncItemKey(ids.get(i));
		}

		return keys; //extractKeys(allItems);
	}

	/**
	 * @see SyncSource
	 * TODO returns the key of the syncItem passed
	 */
	public SyncItemKey[] getSyncItemKeysFromTwin(SyncItem syncItem)
	throws SyncSourceException {
		logger.info("getSyncItemKeysFromTwin()");


		return new SyncItemKey[0];
	}

	/**
	 * @see SyncSource
	 */
	public void removeSyncItem(SyncItemKey syncItemKey,
			Timestamp time,
			boolean softDelete)
	throws SyncSourceException {
		if (softDelete) {
			// Ignore soft delete
			logger.warn("Soft Delete not implemented yet!");
			return;
		} else {
			removeSyncItem(syncItemKey, time);
		}
	}

	/**
	 * A string representation of this object
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append(" - {name: ").append(getName());
		sb.append(" type: ").append(type);
		sb.append(" uri: ").append(getSourceURI());
		sb.append("}");
		return sb.toString();
	}

	/**
	 * SyncSource's beginSync()
	 * @param syncContext @see SyncSource
	 */
	public void beginSync(SyncContext syncContext) throws SyncSourceException {
		logger.info("AbstractLDAPSyncSource beginSync()");
		 // FCTF won't call init() so we need it for testing
		init();
		super.beginSync(syncContext);

		this.syncMode = syncContext.getSyncMode();
		this.syncContext = syncContext;
		this.principal = syncContext.getPrincipal();

		if (contactDAO == null){
			throw new SyncSourceException("ContactDAO is unset. Please provide a value in configuration");
		}
		if (! initializeManager()) {
			throw new SyncSourceException("LdapState is unset. Please provide a value in configuration");
		}

		if(logger.isInfoEnabled())
			logger.info("beginSync for LDAPConnector ( " + this.principal + " ) mode ( " + syncContext.getSyncMode() + " )" 
					+ " since (" + syncContext.getSince()  +";" + syncContext.getTo()+ ")");

		setDeviceInfo(syncContext);


		logger.info("AbstractLDAPSyncSource beginSync end");
	}

	/**
	 * SyncSource's endSync()
	 */
	public void endSync() throws SyncSourceException {
		logger.info("endSync()");
		super.endSync();
		/*
        if ( !newItems.isEmpty() ) { // XXX now it should be empty items are added in real-time. think about drawbacks!!!!
        logger.info("endSync: Commiting " + howManyAdded + " new items to the LDAP server");
        ldapState.commitNewItems( newItems );
        } */

		if (!deletedItems.isEmpty()) {
			if (logger.isInfoEnabled())
				logger.info("endSync: Commiting " + howManyDeleted + " deleted items to the LDAP server");
			ldapState.commitDeletedItems(deletedItems);
		}

		/*
        if ( !updatedItems.isEmpty() ) {
        logger.info("endSync: Commiting " + howManyUpdated + " updated items to the LDAP server");
        ldapState.commitUpdatedItems( updatedItems );
        }
		 */
		
		ldapState.getManager().close();
		
		if (logger.isInfoEnabled())
			logger.info("endSync for LDAPConnector (" + this.principal + ")");
	}

	/**
	 * @see SyncSource
	 */
	public void commitSync() throws SyncSourceException {
		logger.info("commitSync =========================================");
		// TODO not useful yet but keeping for eventual future use
	}

	/**
	 * @see SyncSource
	 */
	public void setOperationStatus(String operation, int statusCode, SyncItemKey[] keys) {

		StringBuffer message = new StringBuffer("Received status code '");
		message.append(statusCode).append("' for a '");
		message.append(operation).append("'");
		message.append(" for this items: ");

		for (int i = 0; i < keys.length; i++) {
			message.append("\n- " + keys[i].getKeyAsString());
		}

		logger.info(message.toString());
	}


	//
	// bean getters/setters
	//

	public String getLdapBase() {
		return baseDn;
	}

	public String getLdapPass() {
		return ldapPass;
	}

	public String getLdapUser() {
		return ldapUser;
	}

	public void setLdapBase(String string) {
		baseDn = string;
	}

	public void setLdapPass(String string) {
		ldapPass = string;
	}

	public void setLdapUser(String string) {
		ldapUser = string;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String string) {
		dbName = string;
	}

	public TimeZone getServerTimeZone() {
		return serverTimeZone;
	}

	public void setServerTimeZone(TimeZone t) {
		this.serverTimeZone = t;
	}


	public void setLdapUrl(String ldapUrl) {
		this.providerUrl = ldapUrl;
	}
	public String getLdapUrl() {
		return providerUrl;
	}
	// ------------------------------------------------------ Protected methods
	/**
	 * Removes the item with the given itemKey marking the item deleted with the
	 * give time.
	 * @param syncItemKey the key of the item to remove
	 * @param time the deletion time
	 */
	protected void removeSyncItem(SyncItemKey syncItemKey, Timestamp time)
	throws SyncSourceException {

		if (logger.isInfoEnabled()) {
			logger.info("removeSyncItem(" +
					principal +
					" , " +
					syncItemKey +
					" , " +
					time +
			")");
		}
		SyncItem syncItem = getSyncItemFromId(syncItemKey);
		if (logger.isTraceEnabled())
			logger.trace("SyncItem is : " + syncItem);



		if (syncItem != null) // && allItems.contains(syncItem))
		{

			++howManyDeleted;
			deletedItems.add(syncItem);

			//allItems.remove(syncItem);
		}

	}


	/**
	 * Expand bean fields userSearch, baseDn, providerUrl with funambol principal data.
	 * With a standard officer, replace 
	 *  - username =~ %u@%d
	 *  - username = %s
	 *  - principal = %principal
	 *  
	 * Using LdapUserProvisioningOfficer (LDAPUser) enables 
	 * - setting providerUrl=psRoot .
	 * - basedn = %D
	 * @param principal
	 */
	protected void expandSearchAndBaseDn(Sync4jPrincipal principal ) {
		if (principal != null)   {
			String myPrincipalID = "" + principal.getId();
			baseDn = baseDn.replaceAll("%principal", myPrincipalID);
	
			String username = principal.getUsername();
	
			// manage LDAPUser attributes
			if (principal.getUser() instanceof LDAPUser) {
				LDAPUser user = (LDAPUser) principal.getUser();
	
				if (StringUtils.isNotEmpty(user.getPsRoot())) {
					providerUrl = user.getPsRoot();
					baseDn = "";
					if (logger.isDebugEnabled()) {
						logger.debug("Connecting to "+ providerUrl + "/" + baseDn + "?" + getUserSearch());
					}
					return;
				} else {
					if (StringUtils.isNotEmpty(user.getUserDn())) {
						baseDn = baseDn.replaceAll("%D", user.getUserDn());
					}
				}
			} 
	
			if (username != null) {
				// if username  is an email substitute %u e %d in baseDn:  
				// eg. basedn: dc=%d, o=babel, dc=top
				// eg. basedn: dc=%d, o=referral
				// eg. ldapUrl: ldap://%d.babel.it:234/
				if (username.matches(Constants.EMAIL_PATTERN)) {
					String tmp[] = username.split("@");
					String myUsername = tmp[0];
					String myDomain   = (tmp.length >1) ? tmp[1] : ""; // if domain is not set, use just %u
					if (logger.isTraceEnabled()) {
						logger.trace("username is [" +username+"," + myUsername +", " + myDomain + "]");				
					}
	
	
					// expand %u and %d in ldapUrl and baseDn
					// this enables elastic funambol support:D
					baseDn = baseDn.replaceAll("%u",myUsername)
					.replaceAll("%d",myDomain);
					providerUrl = providerUrl.replaceAll("%u",myUsername)
					.replaceAll("%d",myDomain);
					setUserSearch(getUserSearch().replaceAll("%u", myUsername)
							.replaceAll("%d", myDomain));
				} 
	
				// now expand %s
				if (baseDn.contains("%s"))
					baseDn = baseDn.replaceAll("%s",username);
				if (providerUrl.contains("%s"))
					providerUrl = providerUrl.replaceAll("%s",username);
				if (getUserSearch().contains("%s"))
					setUserSearch(getUserSearch().replaceAll("%s", username));
				if (principal.getUser() != null) {
					baseDn = baseDn.replaceAll("%e", principal.getUser().getEmail());
				}
			}
	
		}
	
		if (logger.isTraceEnabled()) {
			logger.trace("Connecting to "+ providerUrl + "/" + baseDn + "?" + getUserSearch());
		}
	
	}
	/**
	 * Extracts the content from a syncItem.
	 *
	 * @param syncItem
	 * @return as a String object
	 * @see com.funambol.foundation.engine.source
	 */
	protected String getContentFromSyncItem(SyncItem syncItem) {

		byte[] itemContent = syncItem.getContent();

		// Add content processing here, if needed

		return new String(itemContent == null ? new byte[0] : itemContent);
	}




	/**
	 * Create Ldap/DB manager for the syncsource
	 * with dynamic information about the syncSource
	 */
	protected boolean initializeManager() {
		boolean ret = false;
		if (logger.isDebugEnabled())
			logger.debug("initializeManager: LDAPInterface, LDAPState");
	
		try {
			// replace standard field in providerUrl and baseDn: %D, %u, %d,  %s, %e, %principal, psRoot
			expandSearchAndBaseDn(principal);
	
			// Create Ldap Interface
			LdapManagerInterface li = LDAPManagerFactory.createLdapInterface(getLdapInterfaceClassName());
			li.init(providerUrl, baseDn, ldapUser, ldapPass, followReferral, connectionPooling, contactDAO);
			li.setTimeZone(serverTimeZone);
			// set ldapinterface.entryFilter
	
			logger.info("createSyncSource[new LDAPState]");
			ldapState = new LDAPState(li, dbName);
	
			if (ldapState != null) { 
				ret =  true;
			}
		} catch (SyncSourceException e) {
			logger.warn("Ldapinterface error:", e);
			e.printStackTrace();
		} catch (LDAPAccessException e) {
			logger.warn("LDAPInterface somehow failed, please check your settings", e);
		} 
	
		return ret;
	}
	/**
	 * @param List <syncItem> syncItems
	 * @return
	 */
	private SyncItemKey[] extractKeys(List<SyncItem> syncItems) {
		logger.info("extractKeys(" + syncItems.toString() + ")");
		SyncItemKey[] keys = new SyncItemKey[syncItems.size()];
		/*
        Iterator<SyncItem> it = syncItems.iterator();
        int i = 0;
        while( it.hasNext() ) {
        keys[i] = it.next().getKey();
        i++;
        }
		 */
		for (int i = 0; i < syncItems.size(); i++) {
			keys[i] = syncItems.get(i).getKey();
		}
		if (logger.isTraceEnabled())
			logger.trace("keys=" + keys.toString());
		return keys;
	}

	/**
	 * Returns a timestamp aligned to UTC
	 */
	private Timestamp normalizeTimestamp(Timestamp t) {
		return new Timestamp(t.getTime() - getServerTimeZone().getOffset(t.getTime()));
	}

	
	/**
	 * Return the device with the given deviceId
	 * @param deviceId String
	 * @return Sync4jDevice
	 * @throws PersistentStoreException
	 */
	private Sync4jDevice getDevice(String deviceId) throws PersistentStoreException {
		logger.info("getDevice()");
		Sync4jDevice device = new Sync4jDevice(deviceId);
		PersistentStore store = Configuration.getConfiguration().getStore();
		store.read(device);
		return device;
	}
	/**
	 *
	 */
	private void setDeviceInfo(SyncContext context) throws SyncSourceException {
		try {
			Sync4jDevice device    = context.getPrincipal().getDevice();
			String       timezone  = device.getTimeZone()  ;
			if (device.getConvertDate()) {
				if (timezone != null && timezone.length() > 0) {
					deviceTimeZoneDescription = timezone;
					setDeviceTimeZone(TimeZone.getTimeZone(deviceTimeZoneDescription));
				}
			}
			setDeviceCharset(device.getCharset());
		} catch(Exception e) {
			throw new SyncSourceException("Error settings the device information." + e.getMessage());
		}
	}
	
	
	// ------------------------------------------------------- Abstract methods
	/**
	 * Returns a SyncItem with the content of the given file and the given state.
	 * @param contacts the contacts to be read
	 * @param state the item state
	 * @return SyncItem an item with the content of the given file and the given state
	 * @throws SyncSourceException if an error occurs
	 */
	protected abstract List<SyncItem> getSyncItems(List<Contact> contacts, char state)
	throws SyncSourceException;

	/**
	 * Sets the given syncItem in the given file.
	 * @param syncItem the item to set
	 * @param contact the contact where to set the item (LDAP Store)
	 * @return SyncItem
	 * @throws SyncSourceException
	 */
	protected abstract SyncItem[] setSyncItems(SyncItem[] syncItem,
			Contact contact) throws SyncSourceException;
	// rpolli
	/**
	 * Sets the given syncItem in the given file.
	 * @param syncItem the item to set
	 * @param contact the contact where to set the item (LDAP Store)
	 * @return SyncItem
	 * @throws SyncSourceException
	 */
	protected abstract SyncItem createItem(String uid, Contact contact, char state)
	throws SyncSourceException;
}
