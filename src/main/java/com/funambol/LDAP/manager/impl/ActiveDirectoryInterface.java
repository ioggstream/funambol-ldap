package com.funambol.LDAP.manager.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Rdn;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.AbstractLDAPManager;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.source.SyncSourceException;

/**
 * Active Directory Server Interface
 * @author dyerger@stcservices.com
 * modifed from FedoraDsInterface.java by:
 * @author rpolli@babel.it
 *
 */
public class ActiveDirectoryInterface extends AbstractLDAPManager {

	//the correct unique ID in Active Directory is objectGUID, 
	public static String LDAPID =  "objectGUID";
	
	
	// bean constructor
	public ActiveDirectoryInterface(){
	}

	// standard constructor for accessing ldap
	public ActiveDirectoryInterface(String providerUrl, String baseDn, String binddn,
			String pass, boolean followReferral, boolean pooling) throws LDAPAccessException {
		super(providerUrl, baseDn, binddn, pass, followReferral, pooling, null, LDAPID);
		
	}
	public ActiveDirectoryInterface(String providerUrl, String baseDn, String binddn,
			String pass, boolean followReferral, boolean pooling, ContactDAOInterface cdao) throws LDAPAccessException {
		super(providerUrl, baseDn, binddn, pass, followReferral, pooling, cdao, LDAPID);
		
	}
	// initialization for use in SyncSource
	public void init(String providerUrl, String baseDn, String binddn,
			String pass, boolean followReferral, boolean pooling, ContactDAOInterface cdao)
			throws LDAPAccessException {
		super.init(providerUrl, baseDn, binddn, pass, followReferral, pooling, cdao, LDAPID);		
	}


	/**
	 * With cache
	 * @throws NamingException 
	 * @throws LDAPAccessException 
	 * @throws SyncSourceException 
	 */
	@Override
	public HashMap<String, String> getLastModifiedMap(String filter) throws LDAPAccessException, NamingException  {
		// load all items setting the cache
		
		setCache(super.getLastModifiedMap(filter)); 

		return getCache();
	}
	
	/**
	 * Adds a new entry to the LDAP server 
	 *  eventually creating a GUID and a timestamp
	 * @param si <i>SyncItem</i> to get the attributes to be inserted. 
	 * @return the ldapId set by the server, null if errors
	 * 
	 */
	public String  addNewEntry(SyncItem si) {

		String newEntryNsUniqueId = null;
		Attributes attrs = cdao.syncItemToLdapAttributes(si);

		// add attribute if timestamp is set
		if (si.getTimestamp() != null) {
			String sinceUtc = LdapUtils.Timestamp2UTC(si.getTimestamp());
			attrs.put(getCdao().getTimestampAttribute(), sinceUtc);
		}
		
		try {
			DirContext context = createEntry(attrs);
			
			Attributes mapAttributes = context.getAttributes("", new String[]{getLdapId(), "modifyTimestamp"} );
			if (mapAttributes != null) {
				String uid = LdapUtils.getPrintableAttribute(mapAttributes.get(getLdapId()));
				String ts =  LdapUtils.getPrintableAttribute(mapAttributes.get("modifyTimestamp" ));
				if (! StringUtils.isEmpty(uid)) {
					cache.put(uid,ts);					
				}
				
				// update UID
				si.getKey().setKeyValue(uid);		
				newEntryNsUniqueId = uid;
			} else			
				throw new SyncSourceException("Error retrieving modifyTimestamp on newly created entry:" + context.getNameInNamespace());
		} catch (NameAlreadyBoundException e) {
			logger.error("Entry already exist");
		} catch (SyncSourceException e) {
			logger.error(e.getMessage());
		} catch (NamingException e) {
			logger.error(e.getMessage());
		} catch (LDAPAccessException e) {
			logger.error("Can't connect to LDAP server " + e.getMessage());
		}

		return newEntryNsUniqueId;
	}
	
	/**
	 * Modify an entry on the LDAP server
	 * @param si <i>SyncItem</i> to get modifications from
	 */
	public void updateEntry(SyncItem si) throws SyncSourceException {

		String uid    = si.getKey().getKeyAsString();

		if (logger.isInfoEnabled())
			logger.info("Modify LDAP entry: " + uid);

		Attributes proposedEntry = cdao.syncItemToLdapAttributes(si);	
		String newUid = updateEntry(uid, proposedEntry);
		try {
			String filter=String.format(BASIC_FILTER, getLdapId(),Rdn.escapeValue(newUid));

			cache.put(newUid, getLastModified(filter));
		} catch (LDAPAccessException e) {
			logger.error("Can't connect to LDAP server: "+ e.getMessage());
		} catch (NamingException e) {
			logger.error("Can't set entry timestamp: "+ e.getMessage());
		}
		si.getKey().setKeyValue(newUid);		
	}	

	
	@Override
	public void deleteEntry(SyncItem si, boolean soft) {
		String uid = si.getKey().getKeyAsString();
		super.deleteEntry(si, soft);
		cache.remove(uid);
	}
	
	/**
	 * Update cache
	 * @throws LDAPAccessException 
	 * @throws SyncSourceException 
	 */
	@Override	
	public Attributes getLDAPEntryById(String uid) throws LDAPAccessException, SyncSourceException   {
		Attributes entry;
			entry = super.getLDAPEntryById(uid);

			if (entry != null) {
				String lastModified = LdapUtils.getPrintableAttribute(entry.get("modifyTimestamp"));
				cache.put(uid, lastModified);
			}
			return entry;		
		
	}
	
	public List<Attributes> getAllEntriesAsList(String filter) throws LDAPAccessException, NamingException  {
		List<Attributes> ret =  new ArrayList<Attributes>();
		List<String> missingUids = new ArrayList<String>();
		List<String> exceptionList = new ArrayList<String>();

		NamingEnumeration<SearchResult> results;
		results = search(filter, ldapUserAttributes.toArray(new String[0]), SearchControls.ONELEVEL_SCOPE);

		while (results.hasMoreElements()) {
			SearchResult sr=null;
			try{
				sr = results.nextElement();
				Attribute attribute = sr.getAttributes().get(getLdapId());
				if (attribute == null) {
					missingUids.add(getLdapId());
				} else {
					Attributes attrs = sr.getAttributes();
					byte[] GUID = (byte[])attrs.get(getLdapId()).get();
					attrs.remove(getLdapId());
					attrs.put(getLdapId(), ConvertBinaryGUID(GUID));
					ret.add(attrs);
					//ret.add(sr.getAttributes());
				}
			} catch(Exception e){
				exceptionList.add(e.getMessage());
				continue;
			}
		}

		if (logger.isWarningEnabled()) {
			if (!missingUids.isEmpty())
				logger.warn("Missing attributes with id:" + missingUids);
			if (!exceptionList.isEmpty())
				logger.warn("The following errors happened while getting items:" + exceptionList);
		}
		return ret;
	}

	static String ConvertBinaryGUID(byte[] GUID) {
		
		String byteGUID = "";
	
		//Convert the GUID into string using the byte format
		for (int c=0;c<GUID.length;c++) {
			byteGUID = byteGUID + "\\" + AddLeadingZero((int)GUID[c] & 0xFF);
		}	
		
		return byteGUID;
	}	

	static String AddLeadingZero(int k) {
    		return (k <= 0xF) ? "0" + Integer.toHexString(k) : Integer.toHexString(k); 
	}
 
}
