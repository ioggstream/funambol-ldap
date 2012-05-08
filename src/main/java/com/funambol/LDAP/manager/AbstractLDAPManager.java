package com.funambol.LDAP.manager;



import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.dao.impl.ContactDAO;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.source.SyncSourceException;


/**
 * The class handles the communication with the LDAP server. It is
 * based on JNDI
 *  
 * @author  <a href='mailto:fipsfuchs _@ users.sf.net'>Philipp Kamps</a>
 * @author  <a href='mailto:julien.buratto _@ subitosms.it'>Julien Buratto</a>
 * @author  <a href='mailto:gdartigu _@ smartjog.com'>Gilles Dartiguelongue</a>
 * @author  <a  href='mailto:rpolli _@ babel.it'>Roberto Polli</a>
 * @author  <a  href='mailto:pventura _@ babel.it'>Pietro Ventura</a>
 * @version $Id$
 *
 * Change log:
 * - Julien Buratto: Removed password from logs
 */
public abstract class AbstractLDAPManager  extends BasicLdapManager implements LdapManagerInterface {

	// -------------------------------------------------------------- Constants

	private  String ldapId;

	private static final String modifyAttr   = "modifyTimestamp";
	private static final String createAttr   = "createTimestamp";
	public static final String BASIC_FILTER = "(%s=%s)";
	public static final String GE_FILTER = "(%s>=%s)";
	public static final String LE_FILTER = "(%s<=%s)";

	private String entryFilter = "objectclass=person";
	public void setEntryFilter(String entryFilter) {
		this.entryFilter = entryFilter;
	}
	public String getEntryFilter() {
		return entryFilter;
	}

	protected ArrayList<String> ldapUserAttributes;
	
	public String[] getLdapUserAttributesAsArray() throws SyncSourceException {
		if (ldapUserAttributes!=null) {
			return ldapUserAttributes.toArray(new String[0]);
		}
		throw new SyncSourceException("Undefined ContactDAO. UserAttributes are null");
	}
	protected TimeZone tz     = null;

	protected ContactDAOInterface cdao;
	public void setCdao(ContactDAOInterface cdao) {
		this.cdao = cdao;
	}
	public ContactDAOInterface getCdao() {
		return cdao;
	}


	/**
	 * Empty constructor to create object from bean
	 * then you have to run init(...) 
	 */
	public AbstractLDAPManager() {

	}


	/**
	 * Create the binding context
	 * @param providerUrl
	 * @param baseDn
	 * @param binddn
	 * @param pass
	 * @param followReferral
	 * @param ldapId
	 * @throws SyncSourceException 
	 */
	protected void init(String providerUrl, String baseDn, String binddn, String pass, boolean followReferral, boolean poolingConnection, ContactDAOInterface cdao, String ldapId) 
	throws LDAPAccessException 
	{
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("LDAPInterface(AbstractLDAPManager): %s\nbase:%s\nuser: %s\npass: ****\nref: %s\npooling: %s\nid: %s",	 
					providerUrl, baseDn, binddn, followReferral, poolingConnection, ldapId)
			);
		}
		super.init(providerUrl, baseDn, binddn, pass, followReferral,poolingConnection);
		setCdao(cdao);
		setLdapId(ldapId);

		if (getCdao() != null) {
			ldapUserAttributes = new ArrayList<String> (Arrays.asList(getCdao().getSupportedAttributes()));
			ldapUserAttributes.add(0, ldapId);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Initialized LDAPInterface without DAO");
			}
		}

		// test connection to ldap
		getContext();
	}

	/**
	 * Standard Constructor
	 * @param providerUrl
	 * @param baseDn
	 * @param binddn
	 * @param pass
	 * @param followReferral
	 * @param ldapId
	 * @throws SyncSourceException
	 */
	public AbstractLDAPManager(String providerUrl, String baseDn, String binddn, String pass, boolean followReferral, boolean connectionPooling, ContactDAOInterface cdao2, String ldapId) 
	throws LDAPAccessException 
	{
		init(providerUrl, baseDn, binddn, pass, followReferral, connectionPooling, cdao2,  ldapId);
	}

	// --------------------------------------------------------- Public methods

	/**
	 * Get all LDAP entries based on the <i>personFilter</i>
	 * @return Array of ids of LDAP entries
	 * @throws SyncSourceException 
	 */
	public List<String> getAllUids() throws LDAPAccessException {
		return idSearch(entryFilter);
	}

	/**
	 * Get the one LDAP entry by uid
	 * @param uid entryUUID of the LDAP entry
	 * @return one sized <i>List</i> containing the entry id
	 * @throws SyncSourceException 
	 */
	public List<String> getOneEntry(String uid) throws LDAPAccessException {
		return idSearch(String.format("(&(%s)(%s=%s))",entryFilter,getLdapId(), uid));
	}

	/**
	 * Creates a LDAP filter based on a timestamp to get all modified LDAP entries
	 * @param since Timestamp of last sync
	 * @return Array of ids of LDAP entries
	 * @throws SyncSourceException 
	 */
	public List<String> getModifiedEntries(Timestamp since, Timestamp to) throws LDAPAccessException {
		String lts = LdapUtils.Timestamp2UTC(since);
		String toFilter = null;

		if (to != null) {
			toFilter = String.format(LE_FILTER, modifyAttr, LdapUtils.Timestamp2UTC(to));		
		}
		String filter = String.format("(&(%s)(%s>=%s)(%s<=%s)%s)", 
				entryFilter, modifyAttr, lts, createAttr, lts, toFilter) ;

		return idSearch(filter);
	}

	/**
	 * Creates a LDAP filter based on a timestamp to get all new LDAP entries
	 *  LDAP timestamp is in UTC, so avoid using localized timestamp 
	 * @param since Timestamp of last sync
	 * @return Array of ids of LDAP entries
	 * @throws SyncSourceException 
	 */
	public List<String> getNewEntries(Timestamp since, Timestamp to) throws LDAPAccessException {

		String lts = LdapUtils.Timestamp2UTC(since);
		String toFilter = null;

		if (to != null) {
			toFilter = String.format(LE_FILTER, modifyAttr, LdapUtils.Timestamp2UTC(to));		
		}
		String filter = String.format("(&(%s)(%s>=%s)%s)", entryFilter, createAttr, lts, toFilter);

		if( logger.isDebugEnabled()) {		// XXX this is done in the idSearch 	
			logger.debug(filter);
		}

		return idSearch(filter);
	}

	/**
	 * With the given id this method reads a LDAP entry and returns the entry.
	 * @param filter id of an LDAP entry
	 * @return LDAP entry attributes or null if id doesn't exist
	 */
	private SearchResult getLDAPEntry(String filter) {
		SearchResult nextEntry = null;
		logger.debug("Get Entry with " + getLdapId()+ " = " + filter);

		try {
			SearchControls ctls = new SearchControls();
			ctls.setReturningAttributes( getLdapUserAttributesAsArray() );
			ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
			NamingEnumeration<SearchResult> results=null;

			results = getContext().search(baseDn, filter, ctls);
			close();
			nextEntry = results.nextElement();

		} catch (SyncSourceException e) {
			logger.error(e.getMessage());
		} catch (Exception e) {
			logger.warn("Entry not found: "+e);
		}

		return nextEntry;

	}

	/**
	 * Retrieve one entry as a search result from Ldap
	 * @throws SyncSourceException 
	 * @throws SyncSourceException 
	 */
	public Attributes getLDAPEntryById(String uid) throws LDAPAccessException, SyncSourceException {
		SearchResult sr = searchOneEntry(
				String.format(BASIC_FILTER, getLdapId(),uid), 
				getLdapUserAttributesAsArray(), 
				SearchControls.ONELEVEL_SCOPE
		);
		if (sr != null) {
			return sr.getAttributes();
		}
		return null;
	}
	/**
	 * Retrieve one entry as a search result from Ldap
	 * @throws SyncSourceException 
	 * @throws SyncSourceException 
	 */
	public SearchResult searchLDAPEntryById(String uid) throws LDAPAccessException, SyncSourceException {
		return searchOneEntry(
				String.format(BASIC_FILTER, getLdapId(),uid), 
				getLdapUserAttributesAsArray(), 
				SearchControls.ONELEVEL_SCOPE
		);
	}


	public SearchResult searchOneEntry(String filter, String[] attributes, int scope) throws LDAPAccessException {
		SearchResult nextEntry = null;

		NamingEnumeration<SearchResult> results;
		try {
			results = search(filter,  attributes, scope,1);
			if (results.hasMoreElements()) {
				nextEntry = results.nextElement();
			}
		} catch (NamingException e) {
			logger.warn(e.getMessage(), e);
		}
		return nextEntry;
	}

	/**
	 * Adds a new entry to the LDAP server 
	 *  modifying the sincItemKey with the ldapId
	 * @param si <i>SyncItem</i> to get the attributes to be inserted. si's  SincItemKey is replaced with the ldapI<d (GUID) of the newly inserted entry
	 * @return the ldapId set by the server, null if errors
	 * TODO think about that code
	 */
	public String  addNewEntry(SyncItem si) throws NameAlreadyBoundException {

		String newEntryNsUniqueId = null;

		newEntryNsUniqueId = addNewEntry(cdao.syncItemToLdapAttributes(si));
		si.getKey().setKeyValue(newEntryNsUniqueId); // XXX un-needed?

		return newEntryNsUniqueId;
	}

	/**
	 * Create an entry returning its context
	 * @param entryAttributes
	 * @return
	 * @throws NameAlreadyBoundException
	 * @throws SyncSourceException
	 */
	protected DirContext createEntry(Attributes entryAttributes) throws NameAlreadyBoundException, LDAPAccessException, SyncSourceException {
		String rdnValue = getCdao().getRdnValue(entryAttributes);
		String rdn = getCdao().getRdnAttribute()+"=" + rdnValue;

		if (StringUtils.isEmpty(rdn)) {
			if (logger.isWarningEnabled()) {
				logger.warn("Entry has no RDN, can't create");
				return null; // TODO why don't throw exception?
			}
		}

		String mySubcontext = rdn;
		if (StringUtils.isNotEmpty(baseDn)) {
			mySubcontext = mySubcontext.concat("," + baseDn);
		}
		if (logger.isInfoEnabled())
			logger.info("Trying to write LDAPEntry in Ldap DN: "+ mySubcontext);

		DirContext newContext = null;
		try {
			newContext = getContext().createSubcontext(mySubcontext, entryAttributes);
			close();
			// this call retrieves all attribute from the newly bound context
			if (logger.isTraceEnabled()) 
				logger.trace ( "Added entry with id: " + newContext.getAttributes(""));

			if (newContext != null)
				return newContext;

			throw new SyncSourceException("Strangely I can't create the entry");
		}  catch (NamingException e) {
			throw new SyncSourceException("Can't create entry " + mySubcontext + ": " + e.getMessage(), e);
		}



	}
	/** 
	 * A testable method for adding entries to ldap. This method won't add rdn if not defined in the attributes
	 * @param entryAttributes
	 * @return newEntryNsUniqueId or null on error
	 */
	protected String  addNewEntry(Attributes entryAttributes) throws NameAlreadyBoundException {
		String newEntryNsUniqueId = null;
		DirContext newContext = null;
		try {			
			String rdnValue = getCdao().getRdnValue(entryAttributes);
			String rdn = getCdao().getRdnAttribute()+"=" + rdnValue;

			if (StringUtils.isEmpty(rdn)) {
				if (logger.isWarningEnabled()) {
					logger.warn("Entry has no RDN, can't create");
					return null;
				}
			}

			String mySubcontext = rdn;
			if (StringUtils.isNotEmpty(baseDn)) {
				mySubcontext = mySubcontext.concat("," + baseDn);
			}
			if (logger.isDebugEnabled())
				logger.debug("Trying to write LDAPEntry in Ldap DN: "+ mySubcontext);

			newContext = getContext().createSubcontext(mySubcontext, entryAttributes);
			close();
			// this call retrieves all attribute from the newly bound context
			if (logger.isTraceEnabled()) 
				logger.trace( "Added entry with id: " + newContext.getAttributes(""));

			List<String> newId = idSearch("("+ rdn+ ")");

			if (newId.isEmpty()) {
				// TODO maybe with a replicated ldap environment the just inserted entry 
				// may not be retrieved ? 
				logger.info("Entry not inserted...");
				throw new Exception();
			}
			if (logger.isInfoEnabled()) {
				logger.info("The GUID(" +getLdapId() + ") of new item is " +newId.get(0) );
				logger.info("saved into ldap with cn: "+ rdnValue);
			}
			newEntryNsUniqueId = newId.get(0);		

		}  catch (NameAlreadyBoundException e) {
			logger.warn("Cannot insert into LDAP Entry: still existing " + e.getMessage());
		} catch (NamingException e) {
			logger.warn("Cannot insert into LDAP Entry: " + e.getMessage());
			if (logger.isDebugEnabled()) {
				try {
					logger.debug(String.format("Context ns: %s " , getContext().getNameInNamespace()));
					logger.debug("Entry: " + entryAttributes,e);
				} catch (Exception e1) {
					e1.printStackTrace();
				} 
			}
		} catch(Exception e1){
			logger.warn("Cannot insert into Ldap Entry: ",e1);
		} finally {
			try {
				if (newContext != null) {
					newContext.close();
					newContext = null;
				}	
			} catch (NamingException e) {
				logger.error("Can't close context:" ,e );
			}
		}
		return newEntryNsUniqueId;
	}

	/**
	 * Modify an entry on the LDAP server
	 * @param si <i>SyncItem</i> to get modifications from
	 * @throws SyncSourceException 
	 */
	public void updateEntry(SyncItem si) throws LDAPAccessException, SyncSourceException {

		String uid    = si.getKey().getKeyAsString();

		logger.info("Modify LDAP entry: " + getLdapId() + ": " + uid);

		Attributes proposedEntry = cdao.syncItemToLdapAttributes(si);	
		//TODO add syncItem entry
		si.getKey().setKeyValue(updateEntry(uid, proposedEntry));		
	}	

	/**
	 * Update an entry with the given uid
	 * @param uid
	 * @param proposedEntry
	 * @return ldapId of the updated entry
	 * @throws SyncSourceException
	 */
	protected String updateEntry(String uid, Attributes proposedEntry) throws SyncSourceException {

		String ret = uid;

		if (getCdao() == null)
			throw new SyncSourceException("This method requires a ContactDAO, but it's null!");

		try {
			if (logger.isInfoEnabled())
				logger.info("Modify LDAP entry: " + getCdao().getRdnAttribute() + ": " + uid);

			// get old  ldap entry from ldap  and create a new entry from funambol object 
			SearchResult ldapEntry=searchLDAPEntryById(uid); //(uid, LDAP_USER_ATTRIBUTES, SearchControls.ONELEVEL_SCOPE);

			String oldCN = cdao.getRdnValue(ldapEntry.getAttributes());
			String newRdn = cdao.getRdnValue(proposedEntry);

			if (oldCN.equals("") || newRdn.equals("")) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Entry: uid: %s\t values: %s ",	uid, proposedEntry));
				}
				throw new SyncSourceException("Entry has void rdn attribute");
			}
			// compute changes to do
			Map<String, Attributes> modifications = cdao.compareAttributeSets(
					proposedEntry,ldapEntry.getAttributes());

			if (logger.isDebugEnabled()) {
				logger.debug("Modifications to be applied: " +modifications);
			}
			// cn is rdn in our LDAP structure
			// if ldapId is changed, rename the entry
			if ( !newRdn.equals( oldCN ) ) {
				if (logger.isInfoEnabled())
					logger.info("Renaming: from " + oldCN + "," + ldapEntry.getNameInNamespace() );

				LdapName oldName=new LdapName(ldapEntry.getNameInNamespace());
				LdapName newName= new LdapName(ldapEntry.getNameInNamespace());


				//ldapId is the last component
				newName.remove(newName.size()-1);
				String cnFilter=String.format(BASIC_FILTER, getCdao().getRdnAttribute(),Rdn.escapeValue(newRdn));
				if (logger.isDebugEnabled())
					logger.debug("Escaped cn: "+cnFilter);
				newName.add(newName.size(), newRdn);

				//si deve mettere solo la parte da cambiare, la radice deve essere comune
				//es se devo cambiare solo il cn   da una entry cn=...,o=...
				//metto solo il cn
				logger.info("OLDNAME: "+oldName.toString()+" NEWNAME: "+newName.toString());

				getContext().rename(oldName,newName);
				close();
				// update our reference			
				// cerca il cn
				ldapEntry=getLDAPEntry(cnFilter);
				ret = getCdao().getRdnValue(ldapEntry.getAttributes()) ;

			}


			//Apply modifications on attributes
			if (ldapEntry != null){
				// NOTE : never pass getNameInNamespace() to context method
				String dn = ldapEntry.getName();
				if (StringUtils.isNotEmpty(getBaseDn())) {
					dn += ","+getBaseDn();
				}
				getContext().modifyAttributes(dn, LdapContext.REPLACE_ATTRIBUTE, modifications.get(ContactDAO.REPLACE_ATTRIBUTE));
				getContext().modifyAttributes(dn, LdapContext.ADD_ATTRIBUTE, modifications.get(ContactDAO.ADD_ATTRIBUTE));
				getContext().modifyAttributes(dn, LdapContext.REMOVE_ATTRIBUTE, modifications.get(ContactDAO.DEL_ATTRIBUTE));
			}


		} catch (NamingException e) {
			logger.error("Error on LDAP modify. " + uid);
			logger.error("Error is: " + e.getMessage(),e);
			throw new SyncSourceException(e);
		} catch (Exception e){
			logger.error("Error on LDAP modify. " + uid);
			logger.error("Error is: " + e.getMessage(),e);
			throw new SyncSourceException(e);
		}
		return ret;
	}	

	/**
	 * Delete a contact on the LDAP server by dn(ldapId) XXX complex way to achieve it
	 * @param si ID of the contact to delete
	 * TODO with a good exception catching we can ensure the item is no more on the server
	 */
	 
	public void deleteEntry(SyncItem si, boolean soft) {

		String uid = si.getKey().getKeyAsString();
		logger.info("Removing " + getLdapId() + " : " + uid);

		try {
			String dn0 = String.format("%s=%s,%s",getLdapId(),uid,baseDn);
			delete(dn0,soft);
			/*
			for (String dn : searchDn(String.format("(%s=%s)",getLdapId(), uid), SearchControls.ONELEVEL_SCOPE)) {
				if (dn != null) {
					delete(dn, soft);
				} else {
					logger.warn("Can't find entry with uid "+ uid);
				}
			}
			*/

		} catch (Exception e){
			logger.warn("Error contacting LDAP server",e);
		}
	}

	/**
	 * Deletes (eventually soft) an entry given its dn. It removes trailing context namespace when needed
	 * @param rdn
	 * @param soft
	 * @throws LDAPAccessException 
	 */
	public void delete(String rdn, boolean soft) throws LDAPAccessException {

		try {
			if (rdn != null) {					
				// if I got a DN, convert it to RDN
				String ns = getContext().getNameInNamespace();
				if (StringUtils.isNotEmpty(ns)) {
					rdn = rdn.replace(ns, "").trim();
					rdn = rdn.substring(0, rdn.lastIndexOf(","));
				}
				if (!soft || getCdao().getSoftDeleteAttribute() == null ) {
					if (logger.isDebugEnabled()) {
						logger.debug("Destroying subcontext:"+ rdn);
					}
					getContext().destroySubcontext(rdn);	
					close();
					if (logger.isDebugEnabled()) {
						logger.debug("Subcontext destroyed:"+ rdn);
					}
				} else {
					LdapName n = new LdapName(rdn);
					Attributes attrs = new BasicAttributes();
					attrs.put(getCdao().getSoftDeleteAttribute(), "1");
					getContext().modifyAttributes(n, LdapContext.REPLACE_ATTRIBUTE, attrs);
				}

			} else {
				logger.warn("Can't find entry with uid "+ rdn);
			}
		} catch (NamingException e) {
			// TODO the entry may be no more on the server. in this case it should be ok. this way we'll avoid one more search
			logger.warn("Cannot delete LDAP Entry:" + rdn +" from "+baseDn,e);
		} 
	}

	/**
	 * Sets the timezone for timestamp convertion to LDAPTimestamps
	 * @param tz LDAP server timezone (should be the same than synch server)
	 */
	public void setTimeZone(TimeZone tz) {
		this.tz = tz;
	}

	/**
	 * Retrieve Ids or DN of contacts matching the search filter
	 * @param filter search filter
	 * @return array of ldapId or DN
	 * TODO rpolli: change this name and substitute with LDAP_ID_ATTIBUTE
	 * TODO try to retrieve only an arrayList of Ids
	 * @throws SyncSourceException 
	 */
	private List<String> idSearch(String filter, int scope, boolean getDn) throws LDAPAccessException {

		List<String> returnValues = new ArrayList<String>();
		String[] attributes = { getLdapId() };

		NamingEnumeration<SearchResult> results;
		try {
			results = search(filter, attributes, scope);
			if (getDn) {
				while (results.hasMoreElements()) {
					SearchResult sr=null;
					try{
						sr = results.nextElement();
						String dn = sr.getNameInNamespace();
						if (dn == null) {
							logger.warn("Error in LDAP entry: null value in dn");
						} else {
							returnValues.add(dn.trim());
						}
					} catch(Exception e){
						logger.warn("Error reading LDAP entry: " + e.toString());
						continue;
					}
				}
			} else {
				while (results.hasMoreElements()) {
					SearchResult sr=null;
					try{
						sr = results.nextElement();
						Attribute attribute = sr.getAttributes().get(getLdapId());
						if (attribute == null) {
							logger.warn("Error in LDAP entry: null value in "+ getLdapId());
						} else {
							returnValues.add((String)attribute.get());
						}
					} catch(Exception e){
						logger.warn("Error reading LDAP entry: " + e.toString());
						continue;
					}
				}
			}
		} catch (NamingException e1) {
			logger.warn(e1.getMessage());
		}		

		return returnValues;
	}


	/**
	 * this is a simple cache for entries
	 * @param filter
	 * @param scope
	 * @return
	 * @throws NamingException 
	 * @throws SyncSourceException 
	 * @throws NamingException 
	 */
	public HashMap<String,Attributes> getAllEntries(String filter) throws LDAPAccessException, SyncSourceException {
		HashMap<String,Attributes> ret =  new HashMap<String,Attributes>();
		List<String> missingUids = new ArrayList<String>();
		List<String> exceptionList = new ArrayList<String>();

		NamingEnumeration<SearchResult> results;
		try {
			results = search(filter, getLdapUserAttributesAsArray(), SearchControls.ONELEVEL_SCOPE);

			while (results.hasMoreElements()) {
				SearchResult sr=null;
				try{
					sr = results.nextElement();
					Attribute attribute = sr.getAttributes().get(getLdapId());
					if (attribute == null) {
						missingUids.add(getLdapId()+":"+sr.getNameInNamespace());
					} else {
						ret.put(LdapUtils.getPrintableAttribute(attribute), sr.getAttributes());
					}
				} catch(Exception e){
					exceptionList.add(e.getMessage());
					continue;
				}
			}
		} catch (NamingException e1) {
			throw new SyncSourceException("Error while getting entries: "+e1.getMessage());
		}		
		if (logger.isWarningEnabled()) {
			if (missingUids.size() > 0)
				logger.warn("Missing attributes with dn:" + missingUids);
			if (exceptionList.size() > 0)
				logger.warn("The following errors happened while getting items:" + exceptionList);
		}
		return ret;
	}

	public HashMap<String,String> getLastModifiedMap(String filter) throws LDAPAccessException, NamingException {
		HashMap<String,String> ret =  new HashMap<String,String>();
		List<String> missingUids = new ArrayList<String>();
		List<String> exceptionList = new ArrayList<String>();

		NamingEnumeration<SearchResult> results = search(filter, new String[] { getLdapId(), "modifyTimestamp"}, SearchControls.ONELEVEL_SCOPE);		
		while (results.hasMoreElements()) {
			SearchResult sr=null;
			try{
				sr = results.nextElement();
				Attributes attrs = sr.getAttributes();
				String uid = LdapUtils.getPrintableAttribute(attrs.get(getLdapId()));
				String lastModified =  LdapUtils.getPrintableAttribute(attrs.get("modifyTimestamp"));
				if (StringUtils.isEmpty(uid)) {
					missingUids.add(getLdapId());
				} else {
					ret.put(uid,lastModified);
				}
			} catch(Exception e){
				exceptionList.add(e.getMessage());
				continue;
			}
		}

		if (logger.isWarningEnabled()) {
			if (missingUids.size() > 0)
				logger.warn("Missing attributes with id:" + missingUids);
			if (exceptionList.size() > 0)
				logger.warn("The following errors happened while getting items:" + exceptionList);
		}
		return ret;
	}
	
	/**
	 * Return the modifyTimestamp of one ldap entry
	 * @param filter
	 * @return
	 * @throws LDAPAccessException
	 * @throws NamingException
	 */
	protected String getLastModified(String filter) throws LDAPAccessException, NamingException {

		NamingEnumeration<SearchResult> results = search(filter, new String[] { getLdapId(),  "modifyTimestamp"}, SearchControls.ONELEVEL_SCOPE);	
		if (results.hasMoreElements()) {
			SearchResult sr = results.nextElement();
				Attributes attrs = sr.getAttributes();
				return LdapUtils.getPrintableAttribute(attrs.get( "modifyTimestamp"));
		}		
		return null;
	}
	/**
	 * Retrieve DNs of contacts matching the search filter
	 * @param filter search filter
	 * @return array of dn
	 * @throws SyncSourceException 
	 */
	public List<String> searchDn(String filter, int scope) throws LDAPAccessException {		
		return idSearch(filter, SearchControls.SUBTREE_SCOPE, true);
	}


	public List<String> getAllDns() throws LDAPAccessException {
		return idSearch(entryFilter, SearchControls.SUBTREE_SCOPE, true);
	}

	//
	// helper private methods
	//
	private List<String> idSearch(String filter) throws LDAPAccessException {
		return idSearch(filter, SearchControls.ONELEVEL_SCOPE, false);
	}

	protected HashMap<String,String> cache = new HashMap<String,String>();
	public HashMap<String, String> getCache() {
		return cache;
	}
	public void setCache(HashMap<String, String> cache) {
		this.cache = cache;
	}



	public void setLdapId(String id) {
		this.ldapId = id;
	}
	public String getLdapId() {
		if (ldapId == null) {
			if (logger.isWarningEnabled())
				logger.warn("ldapId is null");
		}

		return ldapId;
	}

	public List<String> getTwins(SyncItem si) {
        if (logger.isTraceEnabled()) {
            logger.trace("AbstractLDAPManager getTwinItems begin");
        }
        List<String> uids = null;
		try {        
			// TODO Auto-generated method stub
			String  twins= cdao.getTwinItems(si);
			if (twins != null) {
				// make a search to retrieve all uids
					uids = this.idSearch(twins);
			}
		} catch (LDAPAccessException e) {
			// TODO Auto-generated catch block
			logger.info("Error while finding twin items", e);
			e.printStackTrace();
		}
		return uids;
	}
	
} // end class
