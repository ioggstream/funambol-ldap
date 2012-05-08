/*
 * Copyright (C) 2006-2007 Funambol, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License, as published by
 * Funambol, either version 1 or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY, TITLE, NONINFRINGEMENT or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package com.funambol.LDAP.security;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.LDAPManagerFactory;
import com.funambol.LDAP.manager.LdapManagerInterface;
import com.funambol.LDAP.utils.Constants;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.framework.core.Authentication;
import com.funambol.framework.core.Cred;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.security.Sync4jPrincipal;
import com.funambol.framework.server.Sync4jUser;
import com.funambol.framework.server.store.NotFoundException;
import com.funambol.framework.server.store.PersistentStoreException;
import com.funambol.framework.tools.Base64;
import com.funambol.server.admin.AdminException;

/**
 * This is an implementation of the <i>Officer</i> interface 
 * If an user can authenticate on an LDAP server 
 * defined in LDAPUserProvisioningOfficer.xml it will
 *  be added to the database.
 * It requires basic authentication
 *
 * @author  <a href='mailto:rpolli _@ babel.it'>Roberto Polli</a>
 *
 *
 * @version $Id: LDAPUserProvisioningOfficer.java,v 1.00 2007/11/26 10:40:27 rpolli@babel.it Exp $
 * from UserProvisioningOfficer.java
 */
public class LDAPUserProvisioningOfficer extends AbstractLdapOfficer
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5107374396448703307L;


//	protected String tmpLdapUrl;
//	protected String tmpBaseDn;
//	protected String tmpUserSearch;

	// ------------------------------------------------------------ Constructors
	public LDAPUserProvisioningOfficer() {
		super();
	}

	// ---------------------------------------------------------- Public methods

	/**
	 * We don't instantiate here the ldapInterface because it should have properly set the attributes! can't throw exceptions
	 */
	public void init() {
		super.init();
		if (log.isTraceEnabled()) {
			log.trace(String.format("uri: %s\nbase:%s\nsearch: %s\nuser: %s\npass: %s\nreferral: %s\n",	 
					getLdapUrl(), getBaseDn(),getUserSearch(), getSearchBindDn(), getSearchBindPassword(), isFollowReferral())
			);
		}
		
		// initialize here attribute map
		initializeLdapAttributesToRetrieve();
	}

	/**
	 * Authenticates a credential.
	 *
	 * @param credential the credential to be authenticated
	 *
	 * @return the Sync4jUser if the credential is autenticated, null otherwise
	 */
	public Sync4jUser authenticateUser(Cred credential) {
/* TEST verificare se funge ancora
		Configuration config = Configuration.getConfiguration();
		ps = config.getStore();

		userManager = (UserManager)config.getUserManager();
*
*/
        Sync4jUser user = null;

		String type = credential.getType();

		if ((Cred.AUTH_TYPE_BASIC).equals(type)) {
			user = authenticateBasicCredential(credential);
		} else if ((Cred.AUTH_TYPE_MD5).equals(type)) {
			user = authenticateMD5Credential(credential);
        }


		return user;
	}

	/**
	 * Gets the supported authentication type
	 *
	 * @return the basic authentication type
	 */
	public String getClientAuth() {
		return Cred.AUTH_TYPE_BASIC;
	}

	protected class TempParams {
		public String tmpBaseDn;
		public String tmpLdapUrl;
		public String tmpUserSearch;
	}
	/**
	 * Expand userSearch, baseDn, ldapUri by username:
	 *  - username =~ %u@%d
	 *  - username = %s
	 * @param username
	 */
	/*
	protected void expandSearchAndBaseDn(String username) {
		// if username  is an email substitute %u e %d in baseDn:  
		// eg. basedn: dc=%d, o=babel, dc=top
		// eg. basedn: dc=%d, o=referral
		// eg. ldapUrl: ldap://%d.babel.it:234/
		if (username.matches(Constants.EMAIL_PATTERN)) {
			String tmp[] = username.split("@");
			String myUsername = tmp[0];
			String myDomain   = (tmp.length >1) ? tmp[1] : ""; // if domain is not set, use just %u
			if (log.isTraceEnabled()) {
				log.trace("username is [" +username+"," + myUsername +", " + myDomain + "]");				
			}
 

			// expand %u and %d in ldapUrl and baseDn
			// this enables elastic funambol support:D
			tmpBaseDn = baseDn.replaceAll("%u",myUsername)
			.replaceAll("%d",myDomain);
			tmpLdapUrl = ldapUrl.replaceAll("%u",myUsername)
			.replaceAll("%d",myDomain);
			tmpUserSearch = getUserSearch().replaceAll("%u", myUsername)
					.replaceAll("%d", myDomain);
		} else {
			tmpBaseDn = baseDn;
			tmpLdapUrl = ldapUrl;
			tmpUserSearch = getUserSearch();
		}

		// now expand %s
		if (tmpBaseDn.contains("%s"))
			tmpBaseDn = tmpBaseDn.replaceAll("%s",username);
		if (tmpLdapUrl.contains("%s"))
			tmpLdapUrl = tmpLdapUrl.replaceAll("%s",username);
		if (tmpUserSearch.contains("%s"))
			tmpUserSearch = tmpUserSearch.replaceAll("%s", username);

		if (log.isTraceEnabled()) {
			log.trace("Connecting to "+ tmpLdapUrl + "/" + tmpBaseDn + "?" + tmpUserSearch);
		}

	}
	*/
	/**
	 * Expand userSearch, baseDn, ldapUri by username:
	 *  - username =~ %u@%d
	 *  - username = %s
	 * @param username
	 */
	protected void expandSearchAndBaseDn(String username, TempParams t) {
		// if username  is an email substitute %u e %d in baseDn:  
		// eg. basedn: dc=%d, o=babel, dc=top
		// eg. basedn: dc=%d, o=referral
		// eg. ldapUrl: ldap://%d.babel.it:234/
		if (username.matches(Constants.EMAIL_PATTERN)) {
			String tmp[] = username.split("@");
			String myUsername = tmp[0];
			String myDomain   = (tmp.length >1) ? tmp[1] : ""; // if domain is not set, use just %u
			if (log.isTraceEnabled()) {
				log.trace("username is [" +username+"," + myUsername +", " + myDomain + "]");				
			}
 

			// expand %u and %d in ldapUrl and baseDn
			// this enables elastic funambol support:D
			t.tmpBaseDn = baseDn.replaceAll("%u",myUsername)
			.replaceAll("%d",myDomain);
			t.tmpLdapUrl = ldapUrl.replaceAll("%u",myUsername)
			.replaceAll("%d",myDomain);
			t.tmpUserSearch = getUserSearch().replaceAll("%u", myUsername)
					.replaceAll("%d", myDomain);
		} else {
			t.tmpBaseDn = baseDn;
			t.tmpLdapUrl = ldapUrl;
			t.tmpUserSearch = getUserSearch();
		}

		// now expand %s
		if (t.tmpBaseDn.contains("%s"))
			t.tmpBaseDn = t.tmpBaseDn.replaceAll("%s",username);
		if (t.tmpLdapUrl.contains("%s"))
			t.tmpLdapUrl = t.tmpLdapUrl.replaceAll("%s",username);
		if (t.tmpUserSearch.contains("%s"))
			t.tmpUserSearch = t.tmpUserSearch.replaceAll("%s", username);

		if (log.isTraceEnabled()) {
			log.trace("Connecting to "+ t.tmpLdapUrl + "/" + t.tmpBaseDn + "?" + t.tmpUserSearch);
		}

	}
	// ------------------------------------------------------- Protected Methods
	/**
	 * Checks the given credential thru LDAP bind. 
	 * If the user or the principal isn't found, *but the user can bind*,
	 * they are created.
	 *
	 * @param credential the credential to check
	 *
	 * @return the Sync4jUser if the credential is autenticated, null otherwise
	 */
	protected Sync4jUser authenticateBasicCredential(Cred credential) {
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
		Sync4jUser user = getUser(username, null);

		if (user == null) {

			if (log.isTraceEnabled()) {
				log.trace("User '" +
						username +
				"' not found. A new user will be created");
			}
			// TODO set myUserSearch to retrieve the user by mail (eg. search '(mail=rpolli@babel.it)' )

			// TODO set the baseDn to retrieve the user by domain (eg. rpolli@babel.it -> rpolli+babel.it)

			try {
				user = insertUser(ldapUser);
				if (log.isTraceEnabled()) {
					log.trace("User '" + username + "' created");
				}
			} catch (PersistentStoreException e) {
				log.error("Error inserting a new user", e);
				return null;
			}

		} else {
			if (log.isTraceEnabled()) {
				log.trace("User '" + username + "' found");				
			}
			
			//
			// Check the password
			//
			String storedPassword = user.getPassword();
			if (!password.equals(storedPassword)) {
				//
				// The password is old
				//
				if (log.isTraceEnabled()) {
					log.trace( "The sent password is different from the stored "
							+ "one. Update it.");
				}
				try {
					user = updatePassword(user, password);
					if (log.isTraceEnabled()) {
						log.trace("User '" + username + "' password updated");
					}
				} catch (PersistentStoreException e) {
					log.error("Error updating password for user", e);
					return null;
				}

			} 
			
			// Just take the roles from Funambol
			ldapUser.setRoles(user.getRoles());

            // skip principal creation in case of  Portal
            if (PORTAL_DEVICE_ID.equals(deviceId)){
                return ldapUser;
            }

			//
			// Check the roles
			//
			boolean isASyncUser = isASyncUser(user);

			if (isASyncUser) {

				// Just take roles from funambol db
				ldapUser.setRoles(user.getRoles());

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
				return null;
			}

		}

		//
		// Verify that the principal for the specify deviceId and username exists
		// Otherwise a new principal will be created
		//
		try {
			handlePrincipal(username, deviceId);
		} catch (PersistentStoreException e) {
			log.error("Error handling the principal", e);
			return null;
		}

		return ldapUser;
	}

	/**
	 * Insert a new user with the given username and password
	 *
	 * @param userName the username
	 * @param password the password
	 *
	 * @return the new user
	 *
	 * @throws PersistentStoreException if an error occurs
	 */
	protected Sync4jUser insertUser(String userName, String password)
	throws PersistentStoreException {

		Sync4jUser user = new Sync4jUser();
		user.setUsername(userName);
		user.setPassword(password);
		user.setRoles(new String[] {ROLE_USER});        
		try {
			userManager.insertUser(user);
		} catch (AdminException e) {
			log.error("Error while adding new user: " + e.getCause());
		}
		return user;
	}
	protected Sync4jUser insertUser(Sync4jUser user)
	throws PersistentStoreException {

		user.setRoles(new String[] {ROLE_USER});
		try {
			userManager.insertUser(user);
		} catch (AdminException e) {
			log.error("Error while adding new user: " + e.getCause());
		}
		return user;
	}
	/**
	 * Returns the principal with the given username and deviceId.
	 * <code>null</code> if not found
	 * @param userName the username
	 * @param deviceId the device id
	 * @return the principal found or null.
	 */
	protected Sync4jPrincipal getPrincipal(String userName, String deviceId)
	throws PersistentStoreException {

		Sync4jPrincipal principal = null;

		//
		// Verify that exist the principal for the specify deviceId and username
		//
		principal = Sync4jPrincipal.createPrincipal(userName, deviceId);

		try {
			ps.read(principal);
		} catch (NotFoundException ex) {
			return null;
		}

		return principal;
	}

	/**
	 * Inserts a new principal with the given userName and deviceId
	 * @param userName the username
	 * @param deviceId the device id
	 * @return the principal created
	 * @throws PersistentStoreException if an error occurs creating the principal
	 */
	protected Sync4jPrincipal insertPrincipal(String userName, String deviceId)
	throws PersistentStoreException {

		//
		// We must create a new principal
		//
		Sync4jPrincipal principal =
			Sync4jPrincipal.createPrincipal(userName, deviceId);

		ps.store(principal);

		return principal;
	}


	/**
	 * Searches if there is a principal with the given username and device id.
	 * if no principal is found, a new one is created.
	 * @param userName the user name
	 * @param deviceId the device id
	 * @return the found principal or the new one
	 */
	protected Sync4jPrincipal handlePrincipal(String username, String deviceId)
	throws PersistentStoreException {

		Sync4jPrincipal principal = null;

		//
		// Verify if the principal for the specify deviceId and username exists
		//

		principal = getPrincipal(username, deviceId);

		if (log.isTraceEnabled()) {
			log.trace("Principal '" + username +
					"/" +
					deviceId + "' " +
					((principal != null) ?
							"found"             :
					"not found. A new principal will be created")
			);
		}

		if (principal == null) {
			principal = insertPrincipal(username, deviceId);
			if (log.isTraceEnabled()) {
				log.trace("Principal '" + username +
						"/" +
						deviceId + "' created");
			}
		}

		return principal;
	}


	/**
	 * return false if user or password is wrong
	 * 	
	 * here we expand attributes: %u, %d, %s
	 * 	if defined userSearch, retrieve user's DN  and try to bind with it
	 * @param username
	 * @param password
	 * @return
	 */
	private boolean ldapBind(String username, String password) {    	
		String userDN = null;
		try {
			TempParams t = new TempParams();
			// if username  is an email substitute %u e %d in baseDn:  
			expandSearchAndBaseDn(username,t);
			
			// setup the default LdapInterface configured with bean data
			ldapInterface = LDAPManagerFactory.createLdapInterface(getLdapInterfaceClassName());
			ldapInterface.init(getLdapUrl(), getBaseDn(), 
					getSearchBindDn(), getSearchBindPassword(), 
					isFollowReferral(), isConnectionPooling(), null);

			// set the userDN when custom user search
			if (! StringUtils.isEmpty(getUserSearch())) {
				// customize the field used to search the user.

				SearchResult sr = ldapInterface.searchOneEntry(getUserSearch(), new String[] {"dn"}, SearchControls.SUBTREE_SCOPE);

				if (sr == null) {
					log.info("Username " + username + " not found");
					return false;
				}

				userDN = sr.getNameInNamespace().trim();
				log.info("binding with dn:"+ userDN);

			} 
			// on failure, set the user DN with append
			if ( userDN == null) {
				userDN = "uid="+username+","+baseDn;
			}
		} catch (Exception e) {
			log.error("Can't instantiate LdapInterface: " + e.getMessage());
			return false;
		}
		// Set up environment for creating initial context
		Hashtable<String, String> env = new Hashtable<String, String>(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, 
		"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, getLdapUrl() );


		// Authenticate as  User and password  
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, userDN);
		env.put(Context.SECURITY_CREDENTIALS, password);

		try {
			DirContext ctx = new InitialDirContext(env);
			log.debug(ctx.lookup(userDN));
			ctx.close();
		} catch (AuthenticationException e) {
			log.info("User not authenticated: " + e.getMessage());
			return false;
		} catch (NamingException e) {
			log.warn("User not authenticated: problem while accessing ldap " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
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
		String userDN = null;
		LdapManagerInterface ldapBindInterface = null;
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
					log.info("Username" + username + "not found");
					return null;
				}		
			} else { // use append
				userDN = "uid="+username+","+baseDn;
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
					ldapUser.setPsRoot(LdapUtils.getPrettyAttribute(sr, getAttributeMap().get(Constants.USER_CALENDAR)));
				}
			} else {
				return null;
			}

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
	 * return the user dn of an ldap entry
	 * 
	 * search: base, filter, attrs, user, pass
	 * @return
	 */
	protected SearchResult ldapSearch(String bindUser, String bindPass, String base,
			String filter, String[] attributes)
	{
		SearchResult ret = null;
		Hashtable<String, Object> bindEnv = new Hashtable<String, Object>(11);
		bindEnv.put(Context.INITIAL_CONTEXT_FACTORY, 
		"com.sun.jndi.ldap.LdapCtxFactory");
		bindEnv.put(Context.PROVIDER_URL, getLdapUrl() );

		// remove null attributes
		List<String> goodAttributes = new ArrayList<String>();
		for (String s : attributes) {
			if (s!=null) {
				goodAttributes.add(s);
			}
		}

		// get the DN 
		DirContext authenticationContext;
		try {
			SearchControls ctls = new SearchControls();
			ctls.setCountLimit(1);
			ctls.setReturningObjFlag(true);					 
			ctls.setReturningAttributes(goodAttributes.toArray(new String[0]));
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// Authenticate as  User and password  
			if (bindUser!= null && bindPass != null) {
				log.debug("NBinding with credential as user: " + bindUser  );
				bindEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
				bindEnv.put(Context.SECURITY_PRINCIPAL, bindUser);
				bindEnv.put(Context.SECURITY_CREDENTIALS, bindPass);
			}
			authenticationContext = new InitialDirContext(bindEnv);
			// %u, %d in baseDN are still expanded 
			NamingEnumeration<SearchResult> answer;
			try {
				answer = authenticationContext.search(base, filter, ctls);

				if (answer.hasMore()) {
					ret = (SearchResult) answer.next();	 
				}
			} catch (NamingException e) {
				log.warn("Error while searching user with filter ["+ filter +"]: "+ e.getMessage() );
			}
			authenticationContext.close();
			return ret; 

		} catch (NamingException e) {
			log.error("Error while creating context: " + e.getMessage());	
			if (e.getCause() != null) {
				log.error("Error is: " + e.getCause().getMessage());
			}
			return null;
		}
	}

	private String[] ldapAttributesToRetrieve;
	/**
	 * Return attributes defined in Officer.xml
	 * TODO this should be lazily initialized
	 * @return
	 */
	protected void initializeLdapAttributesToRetrieve() {
		List<String> tmp = new ArrayList<String>();
		for (String k:  getAttributeMap().keySet()) {
			String v = getAttributeMap().get(k);
			if ( StringUtils.isNotEmpty(v)) {
				tmp.add(v);
			}
		}
		tmp.add("dn");
		ldapAttributesToRetrieve = tmp.toArray(new String[0]);
	}
	protected String[] getLdapAttributesToRetrieve() {
		return ldapAttributesToRetrieve;
	}
    /**
    * Update user password from LDAP stored
    * @param user the user structure
    * @param password the password
    *
    * @return the updated user
    *
    * @throws PersistentStoreException if an error occurs
    */
	protected Sync4jUser updatePassword(Sync4jUser user, String password)
	throws PersistentStoreException {
		user.setPassword(password);
		try {
			userManager.setUser(user);
		} catch (AdminException e) {
			log.error("Error while updating password for user: " + e.getCause());
		}
		return user;
	}
}
