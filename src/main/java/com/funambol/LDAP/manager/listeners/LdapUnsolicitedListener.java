package com.funambol.LDAP.manager.listeners;


import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;

import com.funambol.LDAP.manager.LdapManagerInterface;
import com.funambol.LDAP.utils.Constants;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;

public class LdapUnsolicitedListener  implements UnsolicitedNotificationListener {
	private LdapManagerInterface ldapInfo;
	private static final FunambolLogger logger = FunambolLoggerFactory.getLogger(Constants.LOGGER_LDAP_MANAGER);

	public void notificationReceived(UnsolicitedNotificationEvent evt) {
		if (logger.isDebugEnabled()) {
			logger.debug("Ignoring event received from ldap: " + evt.getNotification() );
		}
	}


	public void namingExceptionThrown(NamingExceptionEvent evt) {
		NamingException exception = evt.getException();
		
		if (exception instanceof CommunicationException ||
				exception instanceof ServiceUnavailableException) {
			
			logger.warn("Connection with ldap server closed unexpectedly. "+ exception.getMessage());
			ldapInfo.setReconnectionNecessary(true);
		}
	}

	public LdapManagerInterface getLdapInfo() {
		return ldapInfo;
	}

	public void setLdapInfo(LdapManagerInterface ldapInfo) {
		this.ldapInfo = ldapInfo;
	}

}