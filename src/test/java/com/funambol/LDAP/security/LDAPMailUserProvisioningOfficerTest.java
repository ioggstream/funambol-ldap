package com.funambol.LDAP.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.funambol.LDAP.TestConstants;
import com.funambol.email.exception.DBAccessException;
import com.funambol.email.exception.InboxListenerConfigException;
import com.funambol.email.model.MailServer;
import com.funambol.email.model.MailServerAccount;
import com.funambol.framework.core.Authentication;
import com.funambol.framework.core.Cred;
import com.funambol.framework.server.Sync4jUser;
import com.funambol.framework.server.store.PersistentStoreException;
import com.funambol.framework.tools.Base64;
import com.funambol.framework.tools.beans.BeanException;
import com.funambol.server.admin.AdminException;
import com.funambol.server.config.Configuration;
import com.funambol.server.db.DataSourceContextHelper;
import com.funambol.tools.database.DBHelper;

public class LDAPMailUserProvisioningOfficerTest extends LDAPUserProvisioningOfficerTest  implements TestConstants {

	private static final String SMTP_SERVER = "smtp.babel.it";
	private static final String IMAP_SERVER = "imap.babel.it";
	private static final String SMTP_SERVER_PORT = "smtp://smtp.babel.it:25";
	private static final String IMAP_SERVER_PORT = "imap://imap.babel.it:143";
	/**
	 * 
	 */
	private static final long serialVersionUID = 2731811407017600151L;
	private static final String BEAN_NAME = "./com/funambol/server/security/LdapMailUserProvisioningOfficer.xml";
	private static final String MSA_BEAN_NAME = "./com/funambol/server/security/DefaultMailServerAccount.xml";
	
	protected LDAPMailUserProvisioningOfficer lupo = null;
	protected static final Sync4jUser USER = new Sync4jUser(USER_MAIL, USER_MAIL_PASSWORD, 
			USER_MAIL, "", "", new String[] {"sync_user"});
	protected static final Sync4jUser USER_UID = new Sync4jUser(USER_MAIL_UID, USER_MAIL_PASSWORD, 
			USER_MAIL, "", "", new String[] {"sync_user"});

	private Logger logger = Logger.getLogger(this.getClass());

	@Override
	@Before
protected void setUp() throws Exception {
		super.setUp();
		
		DBHelper.executeStatement(this.userds.getRoutedConnection(USER_MAIL), create_email_schema);
		
		MailServer defaultMsa = (MailServer) Configuration.getConfiguration().getBeanInstanceByName(MSA_BEAN_NAME);
		lupo = (LDAPMailUserProvisioningOfficer) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);
		logger.info(lupo);

	}



	@Override
	protected void tearDown() throws Exception {
		this.getUserManager().deleteUser(USER);
		this.getUserManager().deleteUser(USER_UID);
		DBHelper.executeStatement(userds.getRoutedConnection(USER_MAIL), drop_email_schema);
		lupo = null;
		DataSourceContextHelper.closeDataSources();
		super.tearDown();
	}




	@Test
public void testGetServerUri() {

		List<String> a= new ArrayList<String> ();
		a.add("posta.babel.it");
		a.add("posta.babel.it:143");
		a.add("imap://posta.babel.it");
		a.add("imap://posta.babel.it/ciao");
		a.add("imap://posta.babel.it:144/ciao");
//		a.add("imaps://posta.babel.it:144/ciao");
//		a.add("imaps://posta.babel.it/ciao");

		for (String i : a) {
			URI u = lupo.getServerUri(i, "imap");
			assertEquals("posta.babel.it", u.getHost());
			if (!i.contains("144")) {
				assertEquals(143, u.getPort());
			} else {
				assertEquals(144, u.getPort());
			}
			System.out.println( u.getScheme() + "|" + u.getHost() + "|" + u.getPort());			
		}		
	}

	@Test
	public MailServer testGenerateMailServer() throws URISyntaxException, BeanException {
		
		// 1- override bean params
		URI imap = new URI(IMAP_SERVER_PORT);
		URI smtp = new URI(SMTP_SERVER_PORT);

		MailServer ms = lupo.generateMailServer(imap, smtp);
		assertNotNull(ms);
		assertEquals(imap.getHost(), ms.getInServer());
		
		// 2- override just hostname, not port/proto
		imap = new URI(IMAP_SERVER);
		smtp = new URI(SMTP_SERVER);

		ms = lupo.generateMailServer(imap, smtp);
		assertNotNull(ms);
		assertEquals(imap.getHost(), ms.getInServer());
		assertNotSame( "fail: overridden smtp port",25, ms.getOutPort());

		return ms;
	}


	// bind as an unexisting user, check creation, check MSA, delete MSA, delete user
	@Test
public void testBindUserToLdap() {
		try {
			// this method won't check user credential but retrieves DN 
			//		Sync4jUser myUser = lupo.bindUserToLdap(USER.getUsername(), "badPassword");
			//		assertNull(myUser);

			// ..with a good one
			Sync4jUser  myUser = lupo.bindUserToLdap(USER.getUsername(), USER.getPassword());
			assertNotNull(myUser);	
			this.getUserManager().deleteUser(myUser);

			// check if unexistinguser won't auth
			lupo = (LDAPMailUserProvisioningOfficer) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);
			myUser = lupo.bindUserToLdap("NOBODY", USER.getPassword());
			assertNull(myUser);

			// check bad password
			lupo = (LDAPMailUserProvisioningOfficer) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);
			myUser = lupo.bindUserToLdap(USER.getUsername(), "badPassword");
			assertNull(myUser);

			// check if baseDN is badly set
			lupo = (LDAPMailUserProvisioningOfficer) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);
			lupo.setBaseDn("dc=baddomain.it,o=babel,dc=babel,dc=it");
			myUser = lupo.bindUserToLdap(USER.getUsername(), USER.getPassword());
			assertNull(myUser);
		} catch (BeanException e) {
			fail(e.getMessage());
		} catch (PersistentStoreException e) {
			fail(e.getMessage());
		} catch (AdminException e) {
			fail(e.getMessage());

		}
	}

	/**
	 * Authenticate user searching by email 
	 */
	@Test
public void testFindUser_byMail() {
		logger.info("testFindUser");
		try {
			// prepare data
			lupo.setSearchBindDn(credential.adminUser);
			lupo.setSearchBindPassword(credential.adminPassword);

			Authentication auth = new Authentication();
			auth.setUsername(USER.getUsername());
			auth.setPassword(USER.getPassword());
			auth.setType(Cred.AUTH_TYPE_BASIC);
			auth.setPassword(USER.getPassword());


			// authenticate valid user binding with good credential
			byte [] authData = new String(USER.getUsername()+":"+auth.getPassword()).getBytes();
			authData = new String(USER.getUsername()+":"+auth.getPassword()).getBytes();
			auth.setData(new String(Base64.encode(authData)) );
			Sync4jUser ret  = lupo.authenticateUser(new Cred(auth));
			assertNotNull(ret);
			this.getUserManager().deleteUser(ret);

			// 	authenticate valid user binding with bad credential
			lupo = null;
			lupo = (LDAPMailUserProvisioningOfficer) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);
			lupo.setSearchBindDn(DM_USER);
			lupo.setSearchBindPassword("badPassword");
			ret  = lupo.authenticateUser(new Cred(auth));
			assertNull(ret);

		} catch (Exception e) {
			fail(e.getMessage());
		}

	}

	/**
	 * Authenticate user searching by uid 
	 */
	@Test
public void testFindUser_ByUid() {
		logger.info("testFindUser_ByUid");
		try {
			lupo.setUserSearch("(uid=%s)");
			lupo.setBaseDn(ROOT_DN);
			lupo.setSearchBindDn(credential.adminUser);
			lupo.setSearchBindPassword(credential.adminPassword);

			Authentication auth = new Authentication();
			auth.setUsername(USER_UID.getUsername());
			auth.setPassword(USER_UID.getPassword());
			auth.setType(Cred.AUTH_TYPE_BASIC);
			auth.setPassword(USER_UID.getPassword());

			// authenticate valid user binding with good credential
			byte [] authData = new String(USER_UID.getUsername()+":"+auth.getPassword()).getBytes();
			authData = new String(USER_UID.getUsername()+":"+auth.getPassword()).getBytes();
			auth.setData(new String(Base64.encode(authData)) );
			Sync4jUser ret  = lupo.authenticateUser(new Cred(auth));
			logger.info("Authenticating user: "+ USER_UID.getUsername());
			assertNotNull(ret);
			this.getUserManager().deleteUser(ret);

			// authenticate valid user binding with bad credential
			lupo = null;
			lupo = (LDAPMailUserProvisioningOfficer) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);
			lupo.setUserSearch("(uid=%s)");
			lupo.setBaseDn("o=babel s.r.l," + ROOT_DN);
			lupo.setSearchBindDn(DM_PASS);
			lupo.setSearchBindPassword("badPassword");
			ret  = lupo.authenticateUser(new Cred(auth));
			assertNull(ret);

		} catch (Exception e) {
			fail(e.getMessage());
		}

	}
	// bind as an existing user with a new password. check password update in MSA, delete MSA, delete user
	/**
	 * Authenticate and provision MailServerAccount for user searching by mail. 
	 * @throws DBAccessException 
	 */
	@Test
public void testAuthenticateUser() throws DBAccessException {
		logger.warn("testAuthenticateUser");


		Authentication auth = new Authentication();
		auth.setUsername(USER.getUsername());
		auth.setPassword(USER.getPassword());
		auth.setType(Cred.AUTH_TYPE_BASIC);

		auth.setPassword("badPassword");   
		byte [] authData = new String(USER.getUsername()+":"+auth.getPassword()).getBytes();
		auth.setData(new String(Base64.encode(authData)) );

		// bad password: no login, no provision
		logger.debug(new String(Base64.decode(auth.getData())));
		Sync4jUser ret = lupo.authenticateUser(new Cred(auth));
		assertNull(ret);
		// TODO check no provision


		// good password, first login and creation
		auth.setPassword(USER.getPassword());     
		authData = new String(USER.getUsername()+":"+auth.getPassword()).getBytes();
		auth.setData(new String(Base64.encode(authData)) );
		ret = lupo.authenticateUser(new Cred(auth));
		assertNotNull(ret);
		logger.info("\tMSA created for "+ret.getUsername());

		MailServerAccount retMsa;
		try {
			// check mailServerAccount and its credential
			retMsa = getConsoleDAO().getUser(ret.getUsername());
			assertNotNull(retMsa);
			assertEquals(USER.getPassword(), retMsa.getMsPassword());

			// remove mailserveraccount
			getConsoleDAO().deleteUser(retMsa.getId(), getConsoleDAO().getPrincipals(retMsa.getId()));
		} catch (DBAccessException e) {
			fail(e.getMessage());
		} 
	}



	@Test
public void testInsertAndUpdateMailServerAccount() throws DBAccessException {
		URI imap, smtp;
		Long msid = null;
		try {
			imap = new URI(IMAP_SERVER_PORT);
			smtp = new URI(SMTP_SERVER_PORT);
			lupo.smtpServer = SMTP_SERVER_PORT;
			lupo.imapServer = IMAP_SERVER_PORT;
			// create a mailserver
			MailServer ms = lupo.generateMailServer(imap, smtp);
			ms.setMailServerId("-1");

			// create a MSA binding user and MSA. NOTE: this won't create the user in fnbl_user table!
			lupo.insertMailServerAccount(USER, ms);
			logger.info("Creating account with login: "+USER.getUsername() );

			// retrieve MSA by User 
			MailServerAccount newMsa = getConsoleDAO().getUser(USER.getUsername());
			assertNotNull(newMsa);
			logger.info("\t...retrieved account with login: "+newMsa.getMsLogin() );

			// check MailServer
			MailServer newMs = newMsa.getMailServer();
			assertEquals(ms.getMailServerId(), newMs.getMailServerId());
			logger.info("MailServer matches.");
			newMsa = null;

			// update user password
			USER.setPassword("newPassword");
			lupo.insertMailServerAccount(USER, ms);
			logger.info("Updating Password to MSA...");
			newMsa = getConsoleDAO().getUser(USER.getUsername());
			assertEquals(newMsa.getMsPassword(), "newPassword");
			logger.info("\t...password updated in MSA");
			msid = newMsa.getId();
			// clean MSA and User
			getConsoleDAO().deleteUser(msid, getConsoleDAO().getPrincipals(msid));
			logger.info("Removed test account from MSA");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail("Error in test code: " + e.getMessage());
		} catch (InboxListenerConfigException e) {
			fail(e.getMessage());
		} catch (AdminException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (DBAccessException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (BeanException e) {
			e.printStackTrace();
			fail("bad configuration: "+ e.getMessage());
		} finally {
			// clean MSA and User
			if (msid != null)
				getConsoleDAO().deleteUser(msid, getConsoleDAO().getPrincipals(msid));
		}

	}

	@Test
public void testInsertAndUpdateMailServerAccount_2() throws DBAccessException {
		URI imap,smtp;
		Long msid = null;
		MailServer ms = null;
		try {
			imap = new URI("imap://imap.babel.net:143");
			smtp = new URI("smtp://smtp.babel.net:25");
			lupo.imapServer = "imap.babel.net";
			lupo.smtpServer = "smtp.babel.net";

			// create a mailserver
			ms = lupo.generateMailServer(imap, smtp);
			ms.setMailServerId("-1");
			
			// check that only host is overridden
			assertTrue(ms.getOutServer().equals(lupo.smtpServer));
			assertTrue(ms.getInServer().equals(lupo.imapServer));
			assertNotSame(ms.getOutPort(), 25);
			
			// now override everything
			imap = new URI("imap://imap.babel.net:333");
			smtp = new URI("smtp://smtp.babel.net:465");
			lupo.imapServer = "imap.babel.net:333";
			lupo.smtpServer = "smtp.babel.net:465";
			ms = lupo.generateMailServer(imap, smtp);
			ms.setMailServerId("-1");
			// check that only host is overridden
			assertTrue(ms.getOutServer().equals(smtp.getHost()));
			assertTrue(ms.getInServer().equals(imap.getHost()));
			assertEquals(465, ms.getOutPort());
			assertEquals(333,ms.getInPort());



			// create a MSA binding user and MSA. NOTE: this won't create the user in fnbl_user table!
			lupo.insertMailServerAccount(USER, ms);
			logger.info("Creating account with login: "+USER.getUsername() );

			// retrieve MSA by User 
			MailServerAccount newMsa = getConsoleDAO().getUser(USER.getUsername());
			assertNotNull(newMsa);
			logger.info("\t...retrieved account with login: "+newMsa.getMsLogin() );

			// check MailServer
			MailServer newMs = newMsa.getMailServer();
			assertEquals(ms.getMailServerId(), newMs.getMailServerId());
			assertEquals(ms.getOutPort(), 465);
			assertEquals(ms.getIsSSLOut(), true);
			logger.info("MailServer matches.");
			newMsa = null;

			// update user password
			USER.setPassword("newPassword");
			lupo.insertMailServerAccount(USER, ms);
			logger.info("Updating Password to MSA...");
			newMsa = getConsoleDAO().getUser(USER.getUsername());
			assertEquals(newMsa.getMsPassword(), "newPassword");
			logger.info("\t...password updated in MSA");
			msid = newMsa.getId();
			// clean MSA and User
			getConsoleDAO().deleteUser(msid, getConsoleDAO().getPrincipals(msid));
			logger.info("Removed test account from MSA");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail("Error in test code: " + e.getMessage());
		} catch (InboxListenerConfigException e) {
			fail(e.getMessage());
		} catch (AdminException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (DBAccessException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (BeanException e) {
			e.printStackTrace();
			fail("bad configuration: "+ e.getMessage());
		} finally {
			// clean MSA and User
			if (msid != null)
				getConsoleDAO().deleteUser(msid, getConsoleDAO().getPrincipals(msid));
		}

	}

}
