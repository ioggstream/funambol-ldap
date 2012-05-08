package com.funambol.LDAP.utils;

import javax.naming.ldap.BasicControl;

public class ProxyAuthorizationControl extends BasicControl {
    public static final String  PROXY_AUTHORIZATION_CONTROL_OID = "2.16.840.1.113730.3.4.18";

	public ProxyAuthorizationControl(String value) {
		super(PROXY_AUTHORIZATION_CONTROL_OID, false, value.getBytes());
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 4261215542280618610L;

}
