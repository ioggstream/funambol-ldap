package com.funambol.LDAP.exception;

/**
 * 
 * @author rpolli
 *
 */
public class LDAPAccessException extends Exception {

	public LDAPAccessException(String string, Exception ne) {
		super(string,ne);		
	}

	public LDAPAccessException(Exception e) {
		super(e);
	}

	public LDAPAccessException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2665976501196441174L;

}
