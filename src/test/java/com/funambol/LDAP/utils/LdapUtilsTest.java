package com.funambol.LDAP.utils;

import java.sql.Timestamp;
import java.util.TimeZone;

import com.funambol.LDAP.BaseTestCase;

public class LdapUtilsTest extends BaseTestCase {

	/**
	 * TODO assertSomething
	 */
	public void testTimestamp() {
		Long ms = System.currentTimeMillis();
		
		Timestamp ts = new Timestamp(ms);

		logger.info(String.format("ms: %s\nts: %s\n" +
				"UTC:\t%s\nLocal:\t%s\nLondon:\t%s", ms.toString().substring(0,10), ts,
				LdapUtils.Timestamp2UTC(ts), LdapUtils.Timestamp2generalized(ts, TimeZone.getDefault()),
					LdapUtils.Timestamp2generalized(ts, TimeZone.getTimeZone("Europe/London"))
		));

	}
}
