package com.funambol.LDAP.dao;

import java.util.HashMap;

import javax.naming.NamingException;

import com.funambol.LDAP.exception.DBAccessException;

/**
 * Manage the ItemMap stored on the db
 * @author rpolli
 *
 */
public interface ItemMap {

	/**
	 * Load the item map stored on server (the last synced items)
	 * @return
	 * @throws DBAccessException 
	 */
	 HashMap<String,String> loadMap() throws DBAccessException;
	
	/**
	 * Deletes all items from server
	 * @throws DBAccessException 
	 */
	void clearMap() throws DBAccessException;
	
	void updateMap(HashMap<String, String> mappa) throws DBAccessException;
	
	
	//
	// getter/setter
	//
	void setPrincipal(Long s);
	Long getPrincipal();
	
	void setSourceUri(String s);
	String getSourceUri();

	void setUsername(String string);

	void init() throws NamingException;


}
