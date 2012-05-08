package com.funambol.LDAP;

import junit.framework.Assert;

import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.junit.Test;

@ApplyLdifFiles ({
	"file:///home/rpolli/workspace/ldap-connector/src/test/resources/ldif/55ims-ical.ldif"
})


@ApplyLdifs( {
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
    "objectClass: mailrecipient", 
    "uid: daniele",
    "mail: daniele@demo1.net",
    "userPassword: daniele"

}
) 
public class BaseITCaseIT {

	@Test
	public void emptyTest() {
		Assert.assertTrue(true);
	}
	
	@Test
	public void findEntry() {
		
	}
}
