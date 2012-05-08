package com.funambol.LDAP.utils;

public interface Constants {
	public static final int LDAP_DEFAULT_PORT = 389;
	public static final int LDAP_DEFAULT_SSL_PORT =636;
	
	public static final String EMAIL_PATTERN = "^[a-zA-Z]([\\w\\.-]*[a-zA-Z0-9])*@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]$";
    public static final String LOGGER_LDAP = "funambol.ldap";
    public static final String LOGGER_LDAP_DAO = "funambol.ldap.dao";
    public static final String LOGGER_LDAP_ENGINE = "funambol.ldap.engine";
    public static final String LOGGER_LDAP_SOURCE = "funambol.ldap.source";
    public static final String LOGGER_LDAP_MANAGER = "funambol.ldap.manager";

    public static final String STATUS_NEW = "N";
    public static final String STATUS_UPDATED = "U";
    
    public static final String PROTO_IMAP ="imap";
    public static final String PROTO_IMAPS ="imaps";
    public static final String PROTO_POP ="pop";
    public static final String PROTO_POPS ="pops";
    public static final String PROTO_SMTP ="smtp";
    public static final String PROTO_SMTPS ="smtps";
    public static final String DEFAULT_MS_ID = "100";
    
    public static final String URL_SCHEME_SEPARATOR = "://";
    // supported types
	public static final String[]         SUPPORTED_TYPES    = {
		"text/x-vcard",
		"text/vcard"
	};
	public static final String[]         SUPPORTED_TYPES_VERSION    = {
		"2.1",
		"3.0"
	};
    // supported servers
    public static final String SERVER_FEDORA = "FedoraDs";
    public static final String SERVER_OPENLDAP = "OpenLdap";
    public static final String SERVER_ACTIVEDIRECTORY = "ActiveDirectory";
    public final String[] SUPPORTED_SERVERS = {SERVER_FEDORA, SERVER_OPENLDAP, SERVER_ACTIVEDIRECTORY };
    
    // supported objectClass
    public static final String DAO_PITYPEPERSON = "piTypePerson";
    public static final String DAO_INETORGPERSON = "inetOrgPerson";
    public static final String DAO_ORGANIZATIONALPERSON = "organizationalPerson";
    public static final String[]         SUPPORTED_DAO    = {
		DAO_INETORGPERSON,
		DAO_PITYPEPERSON,
		DAO_ORGANIZATIONALPERSON
	};
	// fields to be mapped to ldap attributes in Officer.xml
	public static final String USER_EMAIL = "email";
	public static final String USER_FIRSTNAME = "firstName";
	public static final String USER_LASTNAME = "lastName";
	public static final String USER_ADDRESSBOOK = "addressBookUri";
	public static final String USER_CALENDAR = "calDavUri";
	public static final String USER_FREEBUSY = "freeBusyUri";
	public static final String USER_IMAP = "imapServer";
	public static final String USER_SMTP = "smtpServer";
	
    public String  CORE_DATASOURCE_JNDINAME = "jdbc/fnblcore"  ;
    public String  USER_DATASOURCE_JNDINAME = "jdbc/fnbluser"  ;
    
	public static final String PORTAL_DEVICE_ID = "portal-ui";

}
