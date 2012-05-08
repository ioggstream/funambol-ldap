package com.funambol.LDAP.security;


import org.junit.Ignore;
import org.junit.Test;

import com.funambol.LDAP.BaseTestCase;
import com.funambol.framework.tools.beans.BeanException;
import com.funambol.server.config.Configuration;

public class LDAPUserProvisioningOfficerTest extends BaseTestCase {

	private String beanName = "./com/funambol/server/security/LdapUserProvisioningOfficer.xml";

	@Test
	public void testLdapSearch() {
		logger.info("testLdapSearch()");
		try {
			// by mail
			logger.info("testLdapSearch()");

			Configuration conf = Configuration.getConfiguration();
			LDAPUserProvisioningOfficer lupo = (LDAPUserProvisioningOfficer) conf.getBeanInstanceByName(beanName);
			assertNotNull( "Cannot bind as " + USER_MAIL, lupo.bindUserToLdap(USER_MAIL, USER_MAIL_PASSWORD) );


			lupo.setUserSearch("(uid=%s)");
			assertNotNull( "Cannot bind as " + USER_MAIL_UID,  lupo.bindUserToLdap(USER_MAIL_UID, USER_MAIL_PASSWORD) );

		} catch (BeanException e) {
			fail(e.getMessage());
		}	
	}

	//	// bind as an unexisting user, check creation,  delete user
	//	
	@Test
	@Ignore
	public void testProvisionUser_Creation() {
		//		
	}
	//	
	//	// bind as an existing user with a new password. check password update in fun user, delete MSA, delete user
	//	
	@Test
	@Ignore
	public void testProvisionUser_Update() {
		//		
	}

}
