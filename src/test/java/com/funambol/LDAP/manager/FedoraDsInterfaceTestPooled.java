package com.funambol.LDAP.manager;

import java.util.ArrayList;
import java.util.List;

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

public class FedoraDsInterfaceTestPooled extends BaseTestCase {
	public FedoraDsInterface ldapInterface;
	private String ENTRY_UID = "123-123-123-123";


	protected String USER_BASEDN = "ou=people, dc=bigdomain.net,o=bigcompany," + ROOT_DN;
	private static String USER_DN ="uid=Aaccf.Amar,ou=People,dc=bigdomain.net,o=bigcompany,dc=babel,dc=it";
	private static String USER_PASS = "password";
	private static String USER_MAIL = "aaccf.amar@bigdomain.net";
	private static String PSROOT = "ou=Aaccf.Amar@bigdomain.net, dc=bigdomain.net, dc=PAB";

	private  ContactDAOInterface piTypeCdao = new PiTypeContactDAO();
	private  ContactDAOInterface personCdao = new PersonContactDAO();
	private  ContactDAOInterface standardCdao = new ContactDAO();


	private List<String> addedEntries;

	@Override
	@Before
protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		addedEntries = new ArrayList<String>();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		addedEntries.add("("+ldapInterface.getLdapId()+"=-2)");
		addedEntries.add("(cn=" + PiTypeContactDAOTest.USER_FULLNAME+ ")");
		for (String filter : addedEntries) {
			try {
				for (String dn : ldapInterface.searchDn(filter, SearchControls.SUBTREE_SCOPE) ) {
					logger.info("deleting entry: "+ dn);
					ldapInterface.delete(dn, false);	
					//addedEntries.remove(filter);					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * test various ways of constructing LdapManager
	 */

	@Test
public void testConstructor() {
		// test variuos type of manager constructor
		boolean isPooled = false;
		// direct constructor without dao
		try {
			ldapInterface = new FedoraDsInterface(LDAP_URI, ROOT_DN, null, null, false, isPooled);
			String ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());			
			assertNotNull(ldapId);
			ldapInterface.close();


			// ... with dao without key
			ldapInterface = new FedoraDsInterface(LDAP_URI, ROOT_DN, null, null, false, isPooled, this.standardCdao);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);
			ldapInterface.close();
			// .. with complex dao
			ldapInterface = new FedoraDsInterface(LDAP_URI, ROOT_DN, null, null, false, isPooled, this.piTypeCdao);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);
			ldapInterface.close();

			/*
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
*/
		} catch (LDAPAccessException e) {
			fail(e.getMessage());
		}
	}

	@Test
public void testIterateConstructor() throws Exception {
		for (int i=0; i<10; i++) {
			testConstructor();
		}
	}
}
