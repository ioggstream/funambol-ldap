package com.funambol.LDAP.exception;

/**
 * 
 * @author rpolli
 *
 */
public class DBAccessException extends Exception {

	public DBAccessException(String string, Exception ne) {
		super(string,ne);		
	}

	public DBAccessException(Exception e) {
		super(e);
	}

	public DBAccessException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2665976501196441174L;

}
