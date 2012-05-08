package com.funambol.LDAP.manager;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.source.SyncSourceException;

/**
 * Connect to LDAP.
 * This class throws LDAPAccessException when can't connect to LDAP
 * Other exceptions could be thrown for syntax errors...
 * @author rpolli
 *
 */
public interface LdapManagerInterface {

	
	/**
	 * Get all LDAP entries based on the <i>personFilter</i>
	 * @return Array of ids of LDAP entries
	 * @throws SyncSourceException if can't get connection
	 */
	abstract List<String> getAllUids() throws LDAPAccessException;

	/**
	 * Return all DNs of the addressbook
	 * @return
	 * @throws SyncSourceException 
	 * @throws LDAPAccessException 
	 */
	abstract List<String> getAllDns() throws  LDAPAccessException;
	
	/**
	 * Get the one LDAP entry by uid
	 * @param uid entryUUID of the LDAP entry
	 * @return one sized <i>List</i> containing the entry
	 * @throws SyncSourceException 
	 */
	abstract List<String> getOneEntry(String uid) throws LDAPAccessException;

	/**
	 * Creates a LDAP filter based on a timestamp to get all modified LDAP entries
	 * @param since Timestamp of last sync
	 * @return Array of ids of LDAP entries
	 * @throws SyncSourceException 
	 * @throws LDAPAccessException 
	 */
	abstract List<String> getModifiedEntries(Timestamp since, Timestamp to) throws  LDAPAccessException;

	/**
	 * Creates a LDAP filter based on a timestamp to get all new LDAP entries
	 *  LDAP timestamp is in UTC, so avoid using localized timestamp 
	 * @param since Timestamp of last sync
	 * @return Array of ids of LDAP entries
	 * @throws SyncSourceException 
	 */
	abstract List<String> getNewEntries(Timestamp since, Timestamp to) throws LDAPAccessException;

	abstract SearchResult searchLDAPEntryById(String uid) throws  LDAPAccessException, SyncSourceException;
	Attributes getLDAPEntryById(String uid) throws LDAPAccessException, SyncSourceException;
	
	abstract SearchResult searchOneEntry(String filter,
			String[] attributes, int scope) throws LDAPAccessException;

	/**
	 * Retrieve a map <uid, modifyTimestamp>
	 * @param filter
	 * @return
	 * @throws NamingException 
	 * @throws SyncSourceException 
	 */
	HashMap<String,String> getLastModifiedMap(String filter) throws LDAPAccessException, NamingException;
	
	/**
	 * Adds a new entry to the LDAP server 
	 *  modifying the sincItemKey with the ldapId
	 * @param si <i>SyncItem</i> to get the attributes to be inserted. si's  SincItemKey is replaced with the ldapI<d (GUID) of the newly inserted entry
	 * @return the ldapId set by the server, null if errors
	 * TODO think about that code
	 * @throws NameAlreadyBoundException if still exists
	 */
	abstract String addNewEntry(SyncItem si) throws NameAlreadyBoundException;

	/**
	 * Modify an entry on the LDAP server
	 * @param si <i>SyncItem</i> to get modifications from
	 * @throws SyncSourceException 
	 */
	abstract void updateEntry(SyncItem si) throws LDAPAccessException, SyncSourceException;

	/**
	 * Delete a contact on the LDAP server by dn(ldapId) XXX complex way to achieve it
	 * @param si ID of the contact to delete
	 * TODO with a good exception catching we can ensure the item is no more on the server
	 */
	abstract void deleteEntry(SyncItem si, boolean soft);
	HashMap<String, String> getCache() ;
	void setCache(HashMap<String, String> cache);

	/**
	 * Sets the timezone for timestamp convertion to LDAPTimestamps
	 * @param tz LDAP server timezone (should be the same than synch server)
	 */
	abstract void setTimeZone(TimeZone tz);

	abstract void setBaseDn(String s);
	abstract void setEntryFilter(String s);
	abstract String getEntryFilter();

	abstract String getLdapId();
	abstract void setLdapId(String s);

	/**
	 * Initialize the Manager setting basic properties and the ContactDAO
	 * @param providerUrl
	 * @param baseDn
	 * @param ldapUser
	 * @param ldapPass
	 * @param followReferral
	 * @param pooling
	 * @param contactDAO
	 * @throws LDAPAccessException 
	 */
	abstract void init(String providerUrl, String baseDn,
			String ldapUser, String ldapPass, boolean followReferral,
			boolean pooling, ContactDAOInterface contactDAO) throws LDAPAccessException;

	/**
	 * Close the context and set this.context=null
	 * subsequent call cause the context to be recreated with new parameter 
	 */
	abstract void close();

	/**
	 * Retrieve the uids of the twins of the given contact
	 * @param c
	 * @return
	 */
	List<String> getTwins(SyncItem si);

	abstract void delete(String dn, boolean soft) throws LDAPAccessException;

	abstract void setReconnectionNecessary(boolean b);
	
}