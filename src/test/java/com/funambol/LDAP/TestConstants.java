package com.funambol.LDAP;

import com.funambol.LDAP.utils.Constants;

public interface TestConstants extends Constants {
	final String LDAP_URI = "ldap://ldap.example.com/";
	final String ROOT_DN = "ou=system";
	final String LDAP_SEARCH_MAIL = "(mail=%s)";
	final String LDAP_SEARCH_UID = "(uid=%u)";
	final String BASE_DN = "o=bigcompany" + ROOT_DN;
	final String USER1_DN = "ou=people"+BASE_DN;


	final String USER_MAIL = "daniele@demo1.net";
	final String USER_MAIL_UID = "daniele";
	final String USER_MAIL_PASSWORD = "daniele";

	// TODO DM_* to be removed
	final String DM_USER="uid=admin,dc=babel,dc=it";
	final String DM_PASS="admin";

	public final String FCTF_BASIC = "data/basic/text/x-vcard/";
	public final String FCTF_ADVANCED = "data/advanced/text/x-vcard/";
	public final String FCTF_ADVANCED_VCARD = "data/advanced/text/vcard/";


	public final String STANDARD_DS = "jdbc/fnblds";
	public static String MOCKDS = "jdbc/MockDataSource";

	public static final String SQL_DROP_FUNAMBOL_SQL = "sql/drop_funambol.sql";
	public static final String SQL_FUNAMBOL_SQL = "sql/funambol.sql";
	public static final String SQL_EMAIL_DROP_FUNAMBOL_SQL = "sql/email_drop_schema.sql";
	public static final String SQL_EMAIL_FUNAMBOL_SQL = "sql/email_create_schema.sql";

	public static final String HYPERSONIC_DROP_SCHEMA_SQL = "hypersonic/drop_schema.sql";
	public static final String HYPERSONIC_CREATE_SCHEMA_SQL = "hypersonic/create_schema.sql";

	final String CREDENTIAL_BEAN_NAME = "./CredentialBean.xml";


}
