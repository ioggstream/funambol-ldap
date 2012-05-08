package com.funambol.LDAP.utils;

public interface Constants {
	static final int LDAP_DEFAULT_PORT = 389;
	static final int LDAP_DEFAULT_SSL_PORT =636;
	
	static final String EMAIL_PATTERN = "^[a-zA-Z]([\\w\\.-]*[a-zA-Z0-9])*@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]$";
    static final String LOGGER_LDAP = "funambol.ldap";
    static final String LOGGER_LDAP_DAO = "funambol.ldap.dao";
    static final String LOGGER_LDAP_ENGINE = "funambol.ldap.engine";
    static final String LOGGER_LDAP_SOURCE = "funambol.ldap.source";
    static final String LOGGER_LDAP_MANAGER = "funambol.ldap.manager";

    static final String STATUS_NEW = "N";
    static final String STATUS_UPDATED = "U";
    
    static final String PROTO_IMAP ="imap";
    static final String PROTO_IMAPS ="imaps";
    static final String PROTO_POP ="pop";
    static final String PROTO_POPS ="pops";
    static final String PROTO_SMTP ="smtp";
    static final String PROTO_SMTPS ="smtps";
    static final String DEFAULT_MS_ID = "100";
    
    static final String URL_SCHEME_SEPARATOR = "://";
    // supported types
    static final String TYPE_VCF2 = "text/x-vcard";
    static final String TYPE_VCF3 = "text/vcard";
	static final String[]         SUPPORTED_TYPES    = {
		TYPE_VCF2,
		TYPE_VCF3
	};
	static final String[]         SUPPORTED_TYPES_VERSION    = {
		"2.1",
		"3.0"
	};
    // supported servers
    static final String SERVER_FEDORA = "FedoraDs";
    static final String SERVER_OPENLDAP = "OpenLdap";
    static final String SERVER_ACTIVEDIRECTORY = "ActiveDirectory";
    public final String[] SUPPORTED_SERVERS = {SERVER_FEDORA, SERVER_OPENLDAP, SERVER_ACTIVEDIRECTORY };
    
    // supported objectClass
    static final String DAO_PITYPEPERSON = "piTypePerson";
    static final String DAO_INETORGPERSON = "inetOrgPerson";
    static final String DAO_ORGANIZATIONALPERSON = "organizationalPerson";
    static final String[]         SUPPORTED_DAO    = {
		DAO_INETORGPERSON,
		DAO_PITYPEPERSON,
		DAO_ORGANIZATIONALPERSON
	};
	// fields to be mapped to ldap attributes in Officer.xml
	static final String USER_EMAIL = "email";
	static final String USER_FIRSTNAME = "firstName";
	static final String USER_LASTNAME = "lastName";
	static final String USER_ADDRESSBOOK = "addressBookUri";
	static final String USER_CALENDAR = "calDavUri";
	static final String USER_FREEBUSY = "freeBusyUri";
	static final String USER_IMAP = "imapServer";
	static final String USER_SMTP = "smtpServer";
	
    static final String  CORE_DATASOURCE_JNDINAME = "jdbc/fnblcore"  ;
    static final String  USER_DATASOURCE_JNDINAME = "jdbc/fnbluser"  ;
	static final String PORTAL_DEVICE_ID = "portal-ui";

}
