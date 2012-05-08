package com.funambol.LDAP.security;

import com.funambol.framework.server.Sync4jUser;

/**
 * This class is needed to pass Ldap Attributes to various connectors
 * @author rpolli
 *
 */
public class LDAPUser extends Sync4jUser {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2289819027086231005L;
	
	private String userDn;
	private String psRoot;
	private String calUri;
	
	public LDAPUser(Sync4jUser u) {
		setUsername(u.getUsername());
		setEmail(u.getEmail());
		setFirstname(u.getFirstname());
		setLastname(u.getLastname());
		setPassword(u.getPassword());
		setRoles(u.getRoles());		
	}
	
	public LDAPUser() {
		super();
	}

	public void setUserDn(String userDn) {
		this.userDn = userDn;
	}
	public String getUserDn() {
		return userDn;
	}
	public void setPsRoot(String psRoot) {
		this.psRoot = psRoot;
	}
	public String getPsRoot() {
		return psRoot;
	}
	public void setCalUri(String calUri) {
		this.calUri = calUri;
	}
	public String getCalUri() {
		return calUri;
	}
	
}
