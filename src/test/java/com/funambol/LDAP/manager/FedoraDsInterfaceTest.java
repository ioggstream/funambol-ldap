package com.funambol.LDAP.manager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.junit.Before;
import org.junit.Test;

import com.funambol.LDAP.BaseTestCase;
import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.dao.impl.ContactDAO;
import com.funambol.LDAP.dao.impl.PersonContactDAO;
import com.funambol.LDAP.dao.impl.PiTypeContactDAO;
import com.funambol.LDAP.dao.impl.PiTypeContactDAOTest;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.impl.FedoraDsInterface;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemImpl;
import com.funambol.framework.engine.SyncItemState;
import com.funambol.framework.engine.source.SyncSourceException;

public class FedoraDsInterfaceTest extends BaseTestCase  {


	public FedoraDsInterface ldapInterface;
	private String ENTRY_UID = "123-123-123-123";


	protected static String USER_BASEDN = "ou=People,dc=demo1.net,o=rpolli,node=isola1," + ROOT_DN;
	private static String USER_DN ="uid=daniele," + USER_BASEDN;
	private static String PSROOT = "ou=daniele@demo1.net,dc=demo1.net,o=db1";
	private static String PSSERVER = "ldap://mailtopaagg:389/";

	private  ContactDAOInterface piTypeCdao = new PiTypeContactDAO();
	private  ContactDAOInterface personCdao = new PersonContactDAO();
	private  ContactDAOInterface standardCdao = new ContactDAO();


	private List<String> addedEntries;

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		addedEntries = new ArrayList<String>();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		addedEntries.add("("+ldapInterface.getLdapId()+"=-2)");
		addedEntries.add("(cn=" + PiTypeContactDAOTest.USER_FULLNAME+ ")");
		for (String filter : addedEntries) {
			if (filter != null && !"null".equals(filter)) {
				try {
					for (String dn : ldapInterface.searchDn(filter, SearchControls.SUBTREE_SCOPE) ) {
						logger.info("deleting entry: "+ dn);
						if (dn != null) {
							ldapInterface.delete(dn, false);
						}
						//addedEntries.remove(filter);					
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * test various ways of constructing LdapManager
	 */
	@Test
	public void testConstructor() {
		// test variuos type of manager constructor

		// direct constructor without dao
		try {
			ldapInterface = new FedoraDsInterface(LDAP_URI, ROOT_DN, null, null, false, true);
			String ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());			
			assertNotNull(ldapId);
			ldapInterface.close();


			// ... with dao without key
			ldapInterface = new FedoraDsInterface(LDAP_URI, ROOT_DN, null, null, false, true, this.standardCdao);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);
			ldapInterface.close();
			// .. with complex dao
			ldapInterface = new FedoraDsInterface(LDAP_URI, ROOT_DN, null, null, false, true, this.piTypeCdao);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);
			ldapInterface.close();

			// bean constructor without init
			ldapInterface = new FedoraDsInterface();
			// we shouldn't be able to set context
			try {
				assertNull(ldapInterface.getContext());
				fail("With empty constructor we should not be able to raise context");
			} catch (LDAPAccessException e) {
				// noop
			} finally {
				ldapInterface.close();
			}
			assertNotNull(ldapInterface);
			ldapInterface.close();

			// then with init
			ldapInterface.init(LDAP_URI, ROOT_DN, null, null, false,true, null);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);

			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);
			ldapInterface.close();

			// then re-initializing
			ldapInterface.init(LDAP_URI, ROOT_DN, null, null, false,true, piTypeCdao);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);

			// and again
			ldapInterface.init(LDAP_URI, ROOT_DN, null, null, false,true, piTypeCdao);
			ldapId = ldapInterface.getLdapId(); 
			ldapInterface.setLdapId(ldapInterface.getCdao().getRdnAttribute());
			logger.info("Ldapid is: " + ldapInterface.getLdapId());
			assertNotNull(ldapInterface.getContext());	
			assertNotSame(ldapId, ldapInterface.getLdapId());

		} catch (LDAPAccessException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * test with auth binding
	 * @throws SyncSourceException 
	 * @throws LDAPAccessException 
	 */
	@Test
	public void testSearchDn() throws SyncSourceException, LDAPAccessException {
		// create anonymous ldapinterface context
		ldapInterface = new FedoraDsInterface(LDAP_URI, ROOT_DN, null, null, false, false);

		// search dn by filter
		String filter = String.format(AbstractLDAPManager.BASIC_FILTER, "mail", USER_MAIL);
		List<String> dn = ldapInterface.searchDn(filter, SearchControls.SUBTREE_SCOPE);

		// check
		assertNotNull(dn); 
		assertEquals(1, dn.size());
	}

	/**
	 * Retrieve one entry from user's psRoot. Credentials are not taken from the main bean
	 * @throws SyncSourceException 
	 * @throws NamingException 
	 * @throws LDAPAccessException 
	 */
	@Test
	public void testSearchOneEntryFromPsroot() throws SyncSourceException, NamingException, LDAPAccessException {
		logger.info("testSearchOneEntry");

		// retrieve attrs from DN
		ldapInterface = new FedoraDsInterface(LDAP_URI, USER_BASEDN, USER_DN, USER_MAIL_PASSWORD, false, false);

		Attributes attrs =  (ldapInterface.searchOneEntry("(objectclass=*)", 
				new String[] { "dn", "psRoot"} , 
				SearchControls.ONELEVEL_SCOPE).getAttributes());
		logger.trace(attrs);
		assertNotNull(attrs.get("psRoot"));
		String psroot = (String) attrs.get("psRoot").get();


		ldapInterface = null;
		ldapInterface = new FedoraDsInterface(psroot,"", credential.adminUser, credential.adminPassword, false, false);
		ldapInterface.setEntryFilter("(piEntryId=*)");

		List<String> entries = ldapInterface.getAllUids();
		logger.info("found #entries: " + entries.size());
		assertNotSame("No entries found in the psRoot. You need to populate it! ", 0, entries.size());

	}
	/** 
	 * retrieve all entries
	 */
	@Test
	public void testGetAllUids() {
		try {
			logger.info("testGetAllEntries");
			ldapInterface = (FedoraDsInterface) LDAPManagerFactory.createLdapInterface(SERVER_FEDORA);
			ldapInterface.init(LDAP_URI, USER_BASEDN, null, null, false, false, piTypeCdao);

			List<String> allUids = ldapInterface.getAllUids();
			logger.info("found #entries:" + allUids.size());
			assertNotSame(0, allUids.size());

		} catch (SyncSourceException e) {
			fail(e.getMessage());
		} catch (LDAPAccessException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCacheAllEntries() throws NamingException {
		logger.info("testGetAllEntries");
		try {
			ldapInterface = (FedoraDsInterface) LDAPManagerFactory.createLdapInterface(SERVER_FEDORA);
			ldapInterface.init(LDAP_URI, USER_BASEDN, null, null, false, false, piTypeCdao);
			HashMap<String, Attributes> allEntries = ldapInterface.getAllEntries("objectclass=person");

			// logger.info(allEntries);
		} catch (SyncSourceException e) {
			fail(e.getMessage());
		} catch (LDAPAccessException e) {
			fail(e.getMessage());
		} 					
	}

	/**
	 * are there performance issues between list and hashmap?
	 */
	@Test
	public void testCacheAllEntriesAsList() {
		logger.info("testGetAllEntries");
		try {
			ldapInterface = (FedoraDsInterface) LDAPManagerFactory.createLdapInterface(SERVER_FEDORA);
			ldapInterface.init(LDAP_URI, USER_BASEDN, null, null, false, false, piTypeCdao);
			List<Attributes> allEntries = ldapInterface.getAllEntriesAsList("objectclass=person");

			// logger.info(allEntries);
		} catch (Exception e) {
			fail(e.getMessage());
		}		
	}

	/**
	 * add, update, remove an entry, needs DAO
	 * @throws NamingException 
	 * @throws SyncSourceException 
	 */
	@Test
	public void testAddUpdateRemove() throws Exception {
		try {
			logger.info("testAddUpdateRemove");

			for (ContactDAOInterface dao : new ContactDAOInterface[] {/*standardCdao, personCdao,*/ piTypeCdao} ) {
				logger.info("testing with DAO: " + dao.getClass().getName());
				ldapInterface = new FedoraDsInterface(LDAP_URI, USER_BASEDN, 
						credential.adminUser, 
						credential.adminPassword, false, false, dao);
				Attributes entryAttributes;

				String mailAttribute;
				if (dao instanceof com.funambol.LDAP.dao.impl.PiTypeContactDAO ) {
					// if DAO provides a custom attribute for storing UID, retrieve item from psRoot and use it as a key

					Attributes attrs =  (ldapInterface.searchOneEntry("(objectclass=*)", 
							new String[] { "dn", "psRoot"} , 
							SearchControls.ONELEVEL_SCOPE).getAttributes());
					logger.trace(attrs);
					assertNotNull(attrs.get("psRoot"));
					String psRoot = (String) attrs.get("psRoot").get();
					ldapInterface.close();
					ldapInterface.init(psRoot, "", credential.adminUser, credential.adminPassword, false, false);

					ldapInterface.setLdapId(dao.getRdnAttribute());

					mailAttribute = "piEmail1";
					entryAttributes = PiTypeContactDAOTest.getMockEntry();

				} else {
					entryAttributes = PiTypeContactDAOTest.getMockSimpleEntry();
					mailAttribute = "mail";
				}


				String myKey = ldapInterface.addNewEntry(entryAttributes);
				assertNotNull(myKey);
				addedEntries.add(String.format("(%s=%s)", ldapInterface.getLdapId(), myKey));

				entryAttributes.remove(mailAttribute);
				entryAttributes.put(mailAttribute, "newmail@babel.it");

				ldapInterface.updateEntry(myKey, entryAttributes);

				assertEquals("newmail@babel.it", (String) ldapInterface.searchLDAPEntryById(myKey).getAttributes().get(mailAttribute).get() );
				SyncItem si = new SyncItemImpl(null, myKey);			
				ldapInterface.deleteEntry(si, false);
			}
		} catch (SyncSourceException e) {
			e.printStackTrace();
			fail("Test Error: "+ e.getMessage());
		} catch (NamingException e) {
			e.printStackTrace();

			fail("LDAP Error: "+ e.getMessage());
		} 

	}

	@Test
	public void testGetLastModified() throws SyncSourceException, LDAPAccessException, NamingException {
		PiTypeContactDAO dao =  (PiTypeContactDAO)piTypeCdao;

		ldapInterface = new FedoraDsInterface(PSSERVER, PSROOT, credential.adminUser, credential.adminPassword, false, false, dao);
		//	ldapInterface.setLdapId(dao.getRdnAttribute());
		String filter = String.format(ldapInterface.BASIC_FILTER, ldapInterface.getLdapId(),"*");
		HashMap<String,String> map = new HashMap<String, String>();
		map.putAll(ldapInterface.getLastModifiedMap(filter));
		assertNotNull(map);
		logger.info(map);

		for (String s : map.keySet()) {
			String lastMod =  LdapUtils.getPrintableAttribute(ldapInterface.getLDAPEntryById(s).get("modifyTimestamp"));
			assertEquals(lastMod, map.get(s));
			assertEquals(ldapInterface.cache.get(s), map.get(s));
		}

	}


	@Test
	public void testAddAndSoftDelete() throws LDAPAccessException {
		String idfilter = "";
		try {
			// work only with PiTypeContactDAO
			PiTypeContactDAO dao =  (PiTypeContactDAO)piTypeCdao;

			ldapInterface = new FedoraDsInterface(PSSERVER, PSROOT, credential.adminUser, credential.adminPassword, false, false, dao);
			ldapInterface.setLdapId(dao.getRdnAttribute());

			Attributes entryAttributes = PiTypeContactDAOTest.getMockEntry();

			String myKey = ldapInterface.addNewEntry(entryAttributes);
			assertNotNull(myKey);
			idfilter= "("+ldapInterface.getLdapId()+"="+dao.getRdnValue(entryAttributes)+")";
			String dn = ldapInterface.searchDn(idfilter, SearchControls.SUBTREE_SCOPE).get(0);

			ldapInterface.delete(dn, true);

			assertEquals(0,ldapInterface.searchDn("(& (!("+ dao.getSoftDeleteAttribute()+"=1)) (objectclass=person)"+idfilter+")", SearchControls.SUBTREE_SCOPE).size());
			assertNotNull(ldapInterface.searchDn(idfilter, SearchControls.SUBTREE_SCOPE).get(0));
		} catch (NameAlreadyBoundException e) {
			fail(e.getMessage());
		} finally {
			// delete entry
			try {
				String dn = ldapInterface.searchDn(idfilter, SearchControls.SUBTREE_SCOPE).get(0);
				logger.info("deleting entry: "+ dn);
				ldapInterface.delete(dn, false);
			} catch (Exception e) {
				// spada
			}
		}
	}


	//	private void testSearchSoftDelete() {
	//		return;
	//	}
	//	/**
	//	 * add 2 entries
	//	 * update 1 entry
	//	 * retrieve new and updated
	//	 */
	//	private void testAddUpdateAndGetNewUpdated() {
	//		return;
	//	}



	/**
	 * this should create a new syncItemKey to be returned
	 * @param si
	 * @return
	 */
	@Test
	public void testAddUpdateRemoveSyncItem() {
		try {
			ldapInterface = new FedoraDsInterface(PSSERVER+PSROOT, "", credential.adminUser, credential.adminPassword, false, false, piTypeCdao);

			ldapInterface.setLdapId(ldapInterface.getCdao().getRdnAttribute());
			String vcards[] = { 
					"vcard-1.vcf"
					, "vcard-2.vcf","vcard-3.vcf", "vcard-4.vcf", "vcard-5.vcf"
			};

			Timestamp t0 = new Timestamp(System.currentTimeMillis());
			for (String vcf : vcards) {
				logger.info("testAddUpdateRemoveSyncItem: "+ vcf);
				try {
					SyncItem item = getResourceAsSyncItem(FCTF_BASIC +  vcf,  TYPE_VCF2);
					String key = ldapInterface.addNewEntry(item);
					assertNotNull(key);
					addedEntries.add(String.format("(%s=%s)", ldapInterface.getLdapId(), key));

					item.setState(SyncItemState.UPDATED);
					item.setTimestamp(t0);
					item.getKey().setKeyValue(key);
					ldapInterface.updateEntry(item);



				} catch (Exception e) {
					logger.error("missing file: "+ e.getMessage());
				}
			}
		} catch (LDAPAccessException e1) {
			fail(e1.getMessage());
		}		
	}
	/*

	private void testOpenClose() {
		// TODO instantiate the class, 
		// raise with init
		// connect
		// close
		// check what happens
		// reopen

		//finally close

	}







	public List<String> getModifiedEntries(Timestamp ts) {
		// TODO Auto-generated method stub
		return null;
	}


	public List<String> getNewEntries(Timestamp ts) {
		// TODO Auto-generated method stub
		return null;
	}


	private List<String> testGetOneEntry(String uid) {
		// TODO Auto-generated method stub
		return null;
	}

	 */










}
