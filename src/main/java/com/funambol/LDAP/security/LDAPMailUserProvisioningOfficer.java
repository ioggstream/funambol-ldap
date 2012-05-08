/**
 * Copyright (C) 2009 Babel srl.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA 
 */
package com.funambol.LDAP.security;

import java.net.URI;
import java.net.URISyntaxException;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.LDAPManagerFactory;
import com.funambol.LDAP.manager.LdapManagerInterface;
import com.funambol.LDAP.utils.Constants;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.email.console.dao.ConsoleDAO;
import com.funambol.email.engine.source.EmailConnectorConfig;
import com.funambol.email.exception.DBAccessException;
import com.funambol.email.exception.EmailConfigException;
import com.funambol.email.exception.InboxListenerConfigException;
import com.funambol.email.model.MailServer;
import com.funambol.email.model.MailServerAccount;
import com.funambol.email.util.Def;
import com.funambol.framework.core.Authentication;
import com.funambol.framework.core.Cred;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.server.Sync4jUser;
import com.funambol.framework.server.store.PersistentStoreException;
import com.funambol.framework.tools.Base64;
import com.funambol.framework.tools.beans.BeanException;
import com.funambol.server.admin.AdminException;
import com.funambol.server.config.Configuration;

/**
 * This is an implementation of the <i>Officer</i> interface 
 * If an user can authenticate on an LDAP server 
 * defined in LDAPUserProvisioningOfficer.xml it will
 *  be added to the database.
 * It requires basic authentication
 *
 * @author  <a href='mailto:rpolli _@ babel.it'>Roberto Polli</a>
 *
 * @version $Id: LDAPMailUserProvisioningOfficer.java,v 1.00 2007/11/26 10:40:27 rpolli@babel.it Exp 
 * 
 * @changelog adding mail provisioning support
 * 
 */
public class LDAPMailUserProvisioningOfficer
extends LDAPUserProvisioningOfficer implements Constants {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1230521568442385573L;
	private String timeoutStore      = Def.DEFAULT_TIMEOUT_STORE;
	private String timeoutConnection = Def.DEFAULT_TIMEOUT_CONNECTION;
	private final static int DEFAULT_REFRESH_TIME = 5;



	//    address of an imap server in the form of imaps://imap.example.com:port
	protected String imapServer =null;
	protected String smtpServer = null;
	
	private URI imapServerUri = null;
	private URI smtpServerUri = null;


	


	// ------------------------------------------------------------ Constructors
	public LDAPMailUserProvisioningOfficer() throws SyncSourceException {
		super();
	}

	// ---------------------------------------------------------- Public methods
	// getter/setter
	public String getImapServer() {
		return imapServer;
	}
	public void setImapServer(String imapServer) {
		if (imapServer != null)
			this.imapServer = imapServer;
	}
	public String getSmtpServer() {
		return smtpServer;
	}
	public void setSmtpServer(String smtpServer) {
		if (smtpServer != null)
			this.smtpServer = smtpServer;
	}

	// methods for parsing strings to uris
	private URI getImapServerUri() {
		if (imapServerUri == null && getImapServer() != null) {
			imapServerUri = getServerUri(getImapServer(), PROTO_IMAP);
		}
		return imapServerUri;
	}
	private URI getSmtpServerUri() {
		if (smtpServerUri == null && getSmtpServer() != null) {
			smtpServerUri = getServerUri(getSmtpServer(), PROTO_SMTP);
		}
		return smtpServerUri;
	}


	/**
	 * return an URI from the server, eventually using a default protocol
	 */
	protected URI getServerUri(String s, String proto) {
		URI u = null;
		String protocol;
		int port;
		try {
			if (s.indexOf(URL_SCHEME_SEPARATOR) < 0 ) {
				s = proto +URL_SCHEME_SEPARATOR +s;
			}
			u = new URI(s);
			protocol = (u.getSchemeSpecificPart() != null)  ? u.getScheme() : proto ;
			
			if (u.getPort() == -1) {
				if (PROTO_IMAP.equals(protocol)) {
					port = 143;
				} else if ( PROTO_IMAPS.equals(protocol)) {
					port = 993;
				} else if ( PROTO_SMTP.equals(protocol)) {
					port = 25;
				} else if (PROTO_SMTPS.equals(protocol)) {
					port = 465;
				} else if (PROTO_POP.equals(protocol)) {
					port = 110;
				} else if (PROTO_POPS.equals(protocol)) {
					port = 995;
				} else  {
					throw new URISyntaxException(protocol, "Invalid protocol: " + protocol);
				} 
//				else {
//					port = -1;
//					if (log.isDebugEnabled()) {
//						log.debug("no protocol defined, an error will rise if no default protocol defined in DefaultMailServer.xml");
//					}
//				}
			} else {
				port = u.getPort();
			}
			u = new URI(protocol,null, u.getHost(), port, null,null,null);

		} catch (URISyntaxException e) {
			log.error("can't parse uri from string: " + s);
			e.printStackTrace();		
		}  
		return u;
	}


	// ------------------------------------------------------- Protected Methods
	/**
	 * Checks the given credential thru LDAP bind. 
	 * If the user or the principal isn't found, *but the user can bind*,
	 * they are created.
	 * 
	 * This method implements a smart way of binding using the followin
	 * attributes: 
	 *   - ldapUrl: ldaps://ldap.babel.it:389/
	 *   - baseDn: dc=%d,dc=babel, dc=it
	 *   - filtering: (&(mail={0})(enabled=1))
	 *   
	 *   in this way you can search an user by a custom field, 
	 *   directly into a branch of your ldap tree
	 *
	 * @param credential the credential to check
	 *
	 * @return the Sync4jUser if the credential is autenticated, null otherwise
	 */
	protected LDAPUser authenticateBasicCredential(Cred credential) {
		String username = null, password = null;

		Authentication auth = credential.getAuthentication();
		String deviceId = auth.getDeviceId();

		String userpwd = new String(Base64.decode(auth.getData()));

		int p = userpwd.indexOf(':');

		if (p == -1) {
			username = userpwd;
			password = "";
		} else {
			username = (p > 0) ? userpwd.substring(0, p) : "";
			password = (p == (userpwd.length() - 1)) ? "" :
				userpwd.substring(p + 1);
		}

		if (log.isTraceEnabled()) {
			log.trace("User to check: " + username);
		}


		// try the binding or die ;)
		LDAPUser ldapUser = bindUserToLdap(username, password);		
		if (ldapUser == null) {
			return null;
		}  


		//
		// Gets the user without checking the password
		//
		Sync4jUser dsUser = this.getUser(username, null);
		try {

			if (dsUser == null) {
				if (log.isTraceEnabled()) {
					log.trace("User '" +
							username +
					"' not found. A new user will be created");
				}

				dsUser = (LDAPUser) insertUser(ldapUser);
				if (log.isTraceEnabled()) {
					log.trace("User '" + username + "' created");
				}

			} else { // if user exists, update password and MSA
				if (log.isTraceEnabled()) {
					log.trace("User '" + username + "' found");
				}
				// update password on account and msaccount
				String storedPassword = dsUser.getPassword();
				if (!password.equals(storedPassword)) {
					dsUser.setPassword(password);
					try {
						userManager.setUser(dsUser);
						if (log.isTraceEnabled()) {
							log.trace( "The sent password is different from the stored "
									+ "one. Update it");
						}	
					} catch (AdminException e) {
						if (log.isTraceEnabled()) {
							log.trace( "Can't update password for user." + e.getMessage());
						}	
					}
				}
				
				
				// Just take the roles from Funambol
				ldapUser.setRoles(dsUser.getRoles());
				
				if (PORTAL_DEVICE_ID.equals(deviceId))
					return ldapUser;
				
				// fail if user is not a SyncUser
				if (! checkUserRole(dsUser)) {
					return null;
				}

			}
		} catch (PersistentStoreException e) {
			log.error("Error inserting a new user", e);
			return null;
		}

		//
		// Verify that the principal for the specify deviceId and username exists
		// Otherwise a new principal will be created
		//
		try {
    		handlePrincipal(username, deviceId);
			handleMailServerAccount(dsUser);
		} catch (PersistentStoreException e) {
			log.error("Error handling the principal", e);
			return null;
		}

		return ldapUser;
	}

	private boolean checkUserRole(Sync4jUser user) {
		boolean isASyncUser = isASyncUser(user);

		if (isASyncUser) {
			//
			// User authenticate
			//
			if (log.isTraceEnabled()) {
				log.trace("User authenticate");
			}
		} else {
			//
			// User not authenticate
			//
			if (log.isTraceEnabled()) {
				log.trace("The user is not a '" + ROLE_USER + "'");
			}
		}
		return isASyncUser;
	}


	protected boolean handleMailServerAccount(Sync4jUser user) {
		boolean ret = false;
		try {
//			this.jndiDataSourceName =
//				EmailConnectorConfig.getConfigInstance().getDataSource();
			this.timeoutStore =
				EmailConnectorConfig.getConfigInstance().getTimeoutStore();
			this.timeoutConnection =
				EmailConnectorConfig.getConfigInstance().getTimeoutConnection();
		} catch (EmailConfigException e) {
			log.error("Error Getting EmailConnector Parameters ", e);
		}

		try {
			MailServer mailserver = generateMailServer(getImapServerUri(), getSmtpServerUri());

			ret = insertMailServerAccount(user, mailserver);
		} catch (BeanException e) {
			log.error("Error in bean configuration: " + getMailServerConfigBean());
			log.error(e.getMessage());
		} catch (Exception e) {
			log.error("Error Creating/Updating MailServerAccount ", e);
		} 

		return ret;
	}





	/**
	 * Return a S4J user if successful bind to ldap
	 * null if user or password is wrong
	 *      
	 * TODO if I don't need to provision user on ldap,  I could avoid some of that stuff.. 
	 * when binding, it retrieves imap/smtp server data to provision mail push
	 * @param username
	 * @param password
	 * @return the {@link Sync4jUser} created from ldap fields
	 */
	public LDAPUser bindUserToLdap(String username, String password) { 
		LDAPUser ldapUser = null;
		LdapManagerInterface ldapInterface = null;
		LdapManagerInterface ldapBindInterface = null;
		String userDN = null;
		/* TODO
		 * this is now done creating an eventually authenticated context specified in 
		 *  configuration file.
		 *  moreover this context is shared between all ldap connections,
		 *  so could be better defined at application server level 
		 */
		try {
			TempParams t = new TempParams();
			// if username  is an email substitute %u e %d in baseDn:  
			expandSearchAndBaseDn(username,t);

			// setup the default LdapInterface configured with bean data
			// use a bean configuration file
			ldapInterface = LDAPManagerFactory.createLdapInterface(getLdapInterfaceClassName());
			ldapInterface.init(t.tmpLdapUrl, t.tmpBaseDn, 
						getSearchBindDn(), getSearchBindPassword(), 
						isFollowReferral(), isConnectionPooling(), null);


			// set the userDN when custom user search
			if (! StringUtils.isEmpty(getUserSearch())) {
				// search the user binding with default ldap credential defined in the Officer.xml
				ldapInterface.setBaseDn(t.tmpBaseDn);
				SearchResult sr = ldapInterface.searchOneEntry(t.tmpUserSearch, new String[] {"dn"}, SearchControls.SUBTREE_SCOPE);	

				if (sr != null) {    			
					userDN = sr.getNameInNamespace().trim();
					log.info("binding with dn:"+ userDN);	
				} else {
					log.info("Username [" + username + "] not found");
					ldapInterface.close();
					return null;
				}		
			} else { // use append
				userDN = "uid="+username+","+t.tmpBaseDn;
			}
			
			ldapInterface.close();
			ldapBindInterface = LDAPManagerFactory.createLdapInterface(getLdapInterfaceClassName());
			ldapBindInterface.init(t.tmpLdapUrl, userDN, userDN, password, false, false, null);
			SearchResult sr = ldapBindInterface.searchOneEntry("(objectclass=*)", getLdapAttributesToRetrieve(), SearchControls.OBJECT_SCOPE); 


			if (sr != null) {
				ldapUser = new LDAPUser();
				ldapUser.setUsername(username);
				ldapUser.setPassword(password);

				if ( StringUtils.isNotEmpty(getAttributeMap().get(Constants.USER_EMAIL)) ) {
					ldapUser.setEmail(LdapUtils.getPrettyAttribute(sr, getAttributeMap().get(Constants.USER_EMAIL)));
				}
				if ( StringUtils.isNotEmpty(getAttributeMap().get(Constants.USER_FIRSTNAME)) ) {
					ldapUser.setFirstname(LdapUtils.getPrettyAttribute(sr, getAttributeMap().get(Constants.USER_FIRSTNAME)));
				}
				if ( StringUtils.isNotEmpty(getAttributeMap().get(Constants.USER_LASTNAME)) ) {
					ldapUser.setLastname(LdapUtils.getPrettyAttribute(sr, getAttributeMap().get(Constants.USER_LASTNAME)));
				}

				// set attributes to be passed to LDAP and CalDAV connector
				ldapUser.setUserDn(userDN);
				if ( StringUtils.isNotEmpty(getAttributeMap().get(Constants.USER_ADDRESSBOOK)) ) {
					ldapUser.setPsRoot(LdapUtils.getPrettyAttribute(sr, getAttributeMap().get(Constants.USER_ADDRESSBOOK)));
				}
				if ( StringUtils.isNotEmpty(getAttributeMap().get(Constants.USER_CALENDAR)) ) {
					ldapUser.setCalUri(LdapUtils.getPrettyAttribute(sr, getAttributeMap().get(Constants.USER_CALENDAR)));
				}

				// get server attributes from LDAP if not void
				if (getImapServer() == null && StringUtils.isNotEmpty(getAttributeMap().get(Constants.USER_IMAP))) {
					setImapServer(LdapUtils.getPrettyAttribute(sr, getAttributeMap().get(Constants.USER_IMAP)));
				}
				if (getSmtpServer() == null && StringUtils.isNotEmpty(getAttributeMap().get(Constants.USER_SMTP))) {
					setSmtpServer(LdapUtils.getPrettyAttribute(sr, getAttributeMap().get(Constants.USER_SMTP)));
				}
				
				if (Configuration.getConfiguration().isDebugMode()) {
					if (log.isTraceEnabled()) {
						StringBuffer sb = new StringBuffer(64);
						sb.append("psRoot: ").append(ldapUser.getPsRoot()).append("\n")
						.append("calUri: ").append(ldapUser.getCalUri()).append("\n")
						.append("imapServer: ").append(getImapServer()).append("\n")
						.append("smtpServer: ").append(getSmtpServer());
						
						log.trace(sb.toString());
						
					}
				}
			} else {
				ldapUser = null;
			}
			ldapBindInterface.close();

		} catch (SyncSourceException e1) {
			log.error("Can't instantiate context: " + e1.getMessage());
			ldapUser = null;
		} catch (NamingException e) {
			log.warn("Can't retrieve mailserver attributes from ldap: " + e.getMessage());
			ldapUser = null;
		} catch (LDAPAccessException e) {
			log.error("Can't instantiate context: " + e.getMessage());
			ldapUser = null;
		} finally {
			if (ldapInterface !=null) {
				ldapInterface.close();
			}
			if (ldapBindInterface != null) {
				ldapBindInterface.close();
			}
		}
		return ldapUser;
	}







	/**
	 * Create a MailServerAccount into fnbl_email_account using the given user, msa. If mailserver.id ==null, then set mailserver.id=100
	 * 
	 * @param user
	 * @param mailserver
	 * @return true on success
	 */
	public boolean insertMailServerAccount(Sync4jUser user, MailServer mailserver) 
	throws InboxListenerConfigException, AdminException, DBAccessException
	{

		boolean result = false;

		ConsoleDAO ws = new ConsoleDAO();
		try {      
			MailServerAccount account = ws.getUser(user.getUsername());

			if (account == null) {
				account = new MailServerAccount();
				account.setUsername(user.getUsername());
				account.setMsLogin(user.getUsername());
				account.setMsAddress(user.getEmail());
				account.setMsPassword(user.getPassword());

				account.setPush(true);

				account.setMaxImapEmail(Def.MAX_SENT_EMAIL_NUMBER);
				account.setMaxEmailNumber(Def.MAX_SENT_EMAIL_NUMBER);

				account.setPeriod(DEFAULT_REFRESH_TIME*60*1000L);
				account.setActive(true);
				account.setStatus(STATUS_NEW);

				account.setTaskBeanFile(Def.DEFAULT_INBOX_LISTENER_BEAN_FILE);	          
				account.setMailServer(mailserver);

				// XXX set mailServerId as 100 for Custom Server
				if (mailserver.getMailServerId() == null) {
					mailserver.setMailServerId(DEFAULT_MS_ID);
				}
				if (ws.insertUser(account) > 0) {
					result = true;
				}

			} else {
				if (log.isTraceEnabled()) {
					log.trace("MailServerAccount for user '" + user.getUsername() +
					" still exists, can't create");
				}
				// get password and eventually update
				account.setMsPassword(user.getPassword());
				account.setStatus(STATUS_UPDATED);
				account.setPeriod(account.getPeriod()*60*1000L);
				ws.updateUser(account);
			}
			return result;
		}	catch (Exception e) {
			throw new AdminException("Can't create/update MailServerAccount." ,e);
		} finally {
			ws = null;
		}
	}

	/**
	 * Generate a MailServer from imap and smtp uri 
	 * @param myImapServer
	 * @param mySmtpServer
	 * @return
	 * @throws BeanException 
	 */
	protected MailServer generateMailServer(URI myImapServer, URI mySmtpServer) throws BeanException {
		MailServer mailserver;
		if (StringUtils.isNotEmpty(getMailServerConfigBean())) {
			mailserver = (MailServer) Configuration.getConfiguration().getBeanInstanceByName(getMailServerConfigBean());
		} else {
			mailserver = new MailServer();	
			mailserver.setDescription("Generated from LDAPMailUserProvisioningOfficer");
			mailserver.setMailServerType("Custom");
			mailserver.setIsPublic(false);
			mailserver.setInboxPath(Def.FOLDER_INBOX_ENG);
			mailserver.setInboxActivation(true);

			// mailserver default attributes (could be put in the officer configuration)
			mailserver.setSentPath(Def.FOLDER_SENT_ENG);
			mailserver.setSentActivation(true);
			mailserver.setOutboxPath(Def.FOLDER_OUTBOX_ENG);
			mailserver.setOutboxActivation(true);
			mailserver.setDraftsPath(Def.FOLDER_DRAFTS_ENG);
			mailserver.setDraftsActivation(false);
			mailserver.setTrashPath(Def.FOLDER_TRASH_ENG);
			mailserver.setTrashActivation(false);
			mailserver.setOutAuth(false);
		}
		
		// imap
		if (myImapServer != null) {
			mailserver.setInServer(myImapServer.getHost());
			if (StringUtils.indexOf(getImapServer(),myImapServer.getScheme().concat(URL_SCHEME_SEPARATOR))==0 || 
					StringUtils.isEmpty(mailserver.getProtocol())) {
				mailserver.setProtocol(myImapServer.getScheme().replace(URL_SCHEME_SEPARATOR, ""));
				mailserver.setIsSSLIn(myImapServer.getScheme().equals("imaps://") || myImapServer.getScheme().equals("pops://") );
			}
			if (mailserver.getInPort()<0 || 
					StringUtils.indexOf(getImapServer(),":"+Integer.toString(myImapServer.getPort()))>0) {
					mailserver.setInPort(myImapServer.getPort());
			}
		}

		// smtp
		// - if port is specified in smtpServer, override default one
		if (mySmtpServer != null) {
			mailserver.setOutServer(mySmtpServer.getHost());
			if (mailserver.getOutPort()<0 || 
					StringUtils.indexOf(getSmtpServer(), ":"+Integer.toString(mySmtpServer.getPort())) >0) {
				if (log.isTraceEnabled())
					log.trace("DefaultMailServer port set to:" + mailserver.getOutPort() + ";" 
							+ "Server port set to:" + Integer.toString(mySmtpServer.getPort()));
				mailserver.setOutPort(mySmtpServer.getPort());
			}
			if (getSmtpServer().startsWith(mySmtpServer.getScheme()+URL_SCHEME_SEPARATOR)) {
				mailserver.setIsSSLOut(mySmtpServer.getScheme().equals("smtps://"));
			}
		}
		
		if (StringUtils.isEmpty(mailserver.getInServer()) || StringUtils.isEmpty(mailserver.getProtocol()) 
			  || StringUtils.isEmpty(mailserver.getInboxPath())) {
			if (log.isWarningEnabled()) {
				log.warn("Imapserver: " + myImapServer);
				log.warn("SmtpServer: " + mySmtpServer);
			}
			throw new BeanException("One or more of the following fields are empty: InServer, Protocol, InboxPath: please check bean configuration or LDAP values");
		}
		return mailserver;
	}



}
