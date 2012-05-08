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
	 public HashMap<String,String> loadMap() throws DBAccessException;
	
	/**
	 * Deletes all items from server
	 * @throws DBAccessException 
	 */
	public void clearMap() throws DBAccessException;
	
	public void updateMap(HashMap<String, String> mappa) throws DBAccessException;
	
	
	//
	// getter/setter
	//
	public void setPrincipal(Long s);
	public Long getPrincipal();
	
	public void setSourceUri(String s);
	public String getSourceUri();

	public void setUsername(String string);

	public void init() throws NamingException;


}
