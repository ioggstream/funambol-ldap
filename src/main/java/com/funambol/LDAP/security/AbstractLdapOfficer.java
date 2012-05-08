package com.funambol.LDAP.security;

import java.io.Serializable;
import java.util.HashMap;

import com.funambol.LDAP.manager.LdapManagerInterface;
import com.funambol.LDAP.utils.Constants;
import com.funambol.framework.tools.beans.LazyInitBean;
import com.funambol.server.security.DBOfficer;

public class AbstractLdapOfficer extends DBOfficer
implements LazyInitBean, Serializable, Constants{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2656106970195777163L;

	/**
	 * 
	 */

	// set ldap url if given
	protected String ldapUrl = "ldap://localhost:389/";
	public void setLdapUrl(String s) {
		ldapUrl = s;
	}
	public String getLdapUrl() {
		return ldapUrl;
	}		

	protected String baseDn =null;
	public void setBaseDn(String s) {
		baseDn = s;
	}
	public String getBaseDn() {
		return baseDn;
	}


	// authentication info

	// if userSearch is set then uses this field to retrieve the user's DN 
	protected String userSearch = null;
	public void setUserSearch(String s) {
		userSearch=s;
	}
	public String getUserSearch() {		
		return (userSearch!=null) ?  userSearch : "";
	}


	// credential to bind with when searching the user
	private String searchBindDn;
	private String searchBindPassword;
	public void setSearchBindDn(String searchBindDn) {
		this.searchBindDn = searchBindDn;
	}
	public String getSearchBindDn() {
		return searchBindDn;
	}

	public void setSearchBindPassword(String searchBindPassword) {
		this.searchBindPassword = searchBindPassword;
	}
	public String getSearchBindPassword() {
		return searchBindPassword;
	}

	public void setFollowReferral(boolean followReferral) {
		this.followReferral = followReferral;
	}
	public boolean isFollowReferral() {
		return followReferral;
	}

	public void setAttributeMap(HashMap<String, String> attributeMap) {
		this.attributeMap = attributeMap;
	}
	public HashMap<String, String> getAttributeMap() {
		return attributeMap;
	}

	public void setConnectionPooling(boolean isConnectionPooling) {
		this.connectionPooling = isConnectionPooling;
	}
	public boolean isConnectionPooling() {
		return connectionPooling;
	}

	private boolean followReferral;
	private boolean connectionPooling;

	private HashMap<String, String> attributeMap = new HashMap<String, String>();

	protected LdapManagerInterface ldapInterface;

	private String ldapInterfaceClassName;
	public void setLdapInterfaceClassName(String ldapInterfaceClassName) {
		this.ldapInterfaceClassName = ldapInterfaceClassName;
	}
	public String getLdapInterfaceClassName() {
		return ldapInterfaceClassName;
	}

	private String mailServerConfigBean;
	public String getMailServerConfigBean() {
		return mailServerConfigBean;
	}
	public void setMailServerConfigBean(String mailServerConfigBean) {
		this.mailServerConfigBean = mailServerConfigBean;
	}
}
