package com.funambol.LDAP.manager;

import java.util.Arrays;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.listeners.LdapUnsolicitedListener;
import com.funambol.LDAP.utils.Constants;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;

/**
 * This is a basic LDAP interface for binding to a context with user, password, basedn
 * @author rpolli
 *
 */
public class BasicLdapManager  {

	protected static final String INITIAL_CONTEXT_FACTORY="com.sun.jndi.ldap.LdapCtxFactory";
	protected static final String LDAP_CONNECTION_POOL = "com.sun.jndi.ldap.connect.pool";


	private boolean followReferral = false;
	// ---------------------------------------------------------- Private data	
	protected FunambolLogger logger = FunambolLoggerFactory.getLogger(Constants.LOGGER_LDAP_MANAGER);


	//
	// Ldap Credential
	//
	protected String bindDn    = null;
	protected String password   = null;
	protected String providerUrl;
	protected String baseDn = null;
	protected boolean isPoolingConnection = false;
	//PV
	private LdapContext context;
	// manage Listener
	private LdapUnsolicitedListener unsolicitedNotificationListener = null;
	private boolean isReconnectionNecessary = true;	
	private EventContext eventContext =null;
	private Hashtable<String, String> env;


	protected boolean isPoolingConnection() {
		return isPoolingConnection;
	}
	protected void setPoolingConnection(boolean isPoolingConnection) {
		this.isPoolingConnection = isPoolingConnection;
	}
	protected boolean isFollowReferral() {
		return followReferral;
	}
	protected void setProviderUrl(String providerUrl) {
		this.providerUrl = providerUrl.replaceAll(", ", ",");
	}
	protected String getBaseDn() {
		return baseDn;
	}
	protected String getProviderUrl() {
		return providerUrl;
	}
	protected void setFollowReferral(boolean followReferral) {
		this.followReferral = followReferral;
	}
	protected void setBindDn(String bindDn) {
		this.bindDn = bindDn;
	}
	protected void setPassword(String password) {
		this.password = password;
	}
	// Getter/Setter for Listener
	public void setReconnectionNecessary(boolean isReconnectionNecessary) {
		this.isReconnectionNecessary = isReconnectionNecessary;
	}
	public boolean isReconnectionNecessary() {
		return isReconnectionNecessary;
	}
	public void setUnsolicitedNotificationListener(
			LdapUnsolicitedListener unsolicitedNotificationListener) {
		this.unsolicitedNotificationListener = unsolicitedNotificationListener;
	}
	public LdapUnsolicitedListener getUnsolicitedNotificationListener() {
		return unsolicitedNotificationListener;
	}

	/**
	 * - create a context if null or empty
	 * - 
	 * @return
	 * @throws LDAPAccessException
	 */
	protected LdapContext getContext() throws LDAPAccessException {
		try {

			if (isContextVanished() || isReconnectionNecessary() || getUnsolicitedNotificationListener() == null) { 
				logger.debug("Refresh context");
				
				if (isContextVanished()) { // create context and reconnect				
					if (providerUrl == null) {
						throw new LDAPAccessException("ERROR: Ldap server is null. Please fix configuration.");
					}
					this.context = new InitialLdapContext(env, null);
					setReconnectionNecessary(false);
				} 

				if (isReconnectionNecessary()) { // eventually just reconnect
					logger.debug("Reconnect");
					context.reconnect(null);		
					// manage listener
					if (eventContext != null && getUnsolicitedNotificationListener() != null ) { // remove old listener
						eventContext.removeNamingListener(getUnsolicitedNotificationListener());
						logger.debug("Old listener removed");
					}					
				}
				
				if (isPoolingConnection()) {
					logger.debug("Recreating listener..");
					LdapUnsolicitedListener listener = new LdapUnsolicitedListener();
					EventContext ec = (EventContext) context.lookup("");
					eventContext = ec;
					ec.addNamingListener("", EventContext.ONELEVEL_SCOPE, listener);
					setUnsolicitedNotificationListener(listener);
				}
				setReconnectionNecessary(false);
			
				logger.debug("Context restored");
			}
			
			logger.trace("Returning context");
			
			return context;

		} catch (NamingException e) {
			throw new LDAPAccessException("Error while (re)connecting and binding to LDAP: "+ e.getMessage(), e);
		}
	} // getContext

	//* normalize baseDn
	public void setBaseDn(String searchBase) {
		this.baseDn = searchBase.replaceAll(", ", ",");
	}
	/**
	 * Closes the LDAP connection
	 */
	public void close() {

		if (eventContext != null) {
			try {
				if (getUnsolicitedNotificationListener() != null) {
					eventContext.removeNamingListener(getUnsolicitedNotificationListener());
					logger.debug("Removing listener..");
				}
			} catch (NamingException e) {
				logger.error("Error removing listener", e);
			} 
			
			try {
				eventContext.close();
			} catch (NamingException e) {
				logger.error("Error closing eventContext", e);
			}

		}
		if (context != null) { 
			try {
				context.close();
			} catch (NamingException e) {
				logger.error("Error closing context", e);
			}
		}
		eventContext = null;
		context = null;
	}
	/**
	 * Ldap Search Helper
	 * @param filter
	 * @param attributes
	 * @param scope
	 * @return null on error
	 * @throws NamingException 
	 * @throws SyncSourceException 
	 */
	protected NamingEnumeration<SearchResult> search(String filter, String[] attributes, int scope) throws LDAPAccessException, NamingException {
		return search(filter, attributes, scope, 0);
	}
	/**
	 * Ldap Search Helper
	 * @param filter
	 * @param attributes
	 * @param scope
	 * @return null on error
	 * @throws NamingException 
	 * @throws SyncSourceException 
	 */
	protected NamingEnumeration<SearchResult> search(String filter, String[] attributes, int scope, long countLimit) throws LDAPAccessException, NamingException {

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("search[%s, %s, %d] " , filter, Arrays.asList(attributes), scope));			
		}
		NamingEnumeration<SearchResult> result = null;
		SearchControls ctls = new SearchControls();
		ctls.setReturningAttributes(attributes);
		ctls.setSearchScope(scope);
		ctls.setCountLimit(countLimit); 
		ctls.setTimeLimit(10000); // FIXME messo un timeout di 10 secondi
		result = getContext().search(baseDn, filter, ctls);
		this.close();
//		try {
//			result = getContext().search(baseDn, filter, ctls);
//		} catch (SyncSourceException e) {
//			logger.error("Can't connect to LDAP server:" + e.getMessage());
//		} catch (NamingException e) {
//			logger.warn("Error in ldap search: " + e.getMessage());
//		}

		return result;
	}

	/**
	 * Initalize a simple Ldap Manager for binding to a server: this should not be used outside!
	 * @param providerUrl
	 * @param baseDn
	 * @param binddn
	 * @param pass
	 * @param followReferral
	 * @throws LDAPAccessException 
	 * @throws SyncSourceException
	 */
	protected void init(String providerUrl, String baseDn, String binddn,
			String pass, boolean followReferral, boolean poolingConnection) throws LDAPAccessException  {
		// TODO Set the keystore to get access to our certificates
		// System.setProperty("javax.net.ssl.trustStore", "/home/eva/.keystore");
		// System.setProperty("javax.net.ssl.keyStorePassword", "unmotdepasse");
		setProviderUrl(providerUrl);
		setBaseDn( (baseDn != null) ? baseDn : "");
		setBindDn(binddn);
		setPassword(pass);
		setFollowReferral(followReferral);
		setPoolingConnection(poolingConnection);
		
		env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
		env.put(Context.PROVIDER_URL, providerUrl );
		if (isPoolingConnection()) {
			env.put(LDAP_CONNECTION_POOL, "true");
		}
		if (isFollowReferral()) {
			env.put(Context.REFERRAL, "follow");
		}

		if ( StringUtils.isNotEmpty(bindDn) && StringUtils.isNotEmpty(password)) {
			env.put(Context.SECURITY_PRINCIPAL, bindDn);
			env.put(Context.SECURITY_CREDENTIALS, password);
			logger.debug("Binding as: "+ bindDn);
		} else {
			logger.debug("Anonymous bind");
		}
		return;
	}
	
	/**
	 * Controlla che il contesto sia ancora valido (per risolvere problema jboss)
	 * @return
	 */
	public boolean isContextVanished () {
		if (context == null) {
			return true;
		}
		try {
			// logger.debug("Test context environment");
			context.getEnvironment();
			// logger.debug("Test context environment done");

		} catch (NamingException e) {
			return true;
		} 
		return false;
	}
	
}
