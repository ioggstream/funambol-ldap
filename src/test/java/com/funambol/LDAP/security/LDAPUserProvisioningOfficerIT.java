package com.funambol.LDAP.security;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.junit.Ignore;
import org.junit.Test;

import com.funambol.LDAP.BaseITCase;
import com.funambol.framework.tools.beans.BeanException;
import com.funambol.server.config.Configuration;

/*
@ApplyLdifFiles ({
	"./ldif/55ims-ical.ldif"
})
*/
@ApplyLdifs( {
	"dn: cn=schema",
	"changetype: modify" +
	"add: attributetypes" +
	"attributeTypes: ( psRoot-oid NAME 'psRoot' DESC 'Standard Attribute' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SINGLE-VALUE X-ORIGIN 'iPlanet Messaging Server' )" +
	"-" +
	"add: objectclasses" +
	"objectclasses: ( 2.16.840.1.113730.3.2.135 " +
	"NAME 'ipUser' " +
	"DESC 'auxiliary class for extending a inetuser entry. Used by calendar and messaging servers' " +
	"SUP top " +
	"AUXILIARY MAY (psRoot)  " +
	"X-ORIGIN 'iPlanet Messaging Server' )" +
	"-" +
	"",

	
	
	
    "dn: ou=test,ou=system", 
    "objectClass: top", 
    "objectClass: organizationalUnit", 
    "ou: test",
    
    "dn: dc=bigdomain,ou=system", 
    "objectClass: top", 
    "objectClass: domain", 
    "dc: bigdomain",
    
    "dn: uid=daniele,dc=bigdomain,ou=system", 
    "objectClass: inetOrgPerson",
    "objectClass: top",
    "objectClass: ipUser", 
    "uid: daniele",
    "sn: daniele",
    "cn: daniele Daniele",
    "mail: daniele@demo1.net",
    "userPassword: daniele",

}
) 
public class LDAPUserProvisioningOfficerIT extends BaseITCase {

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
