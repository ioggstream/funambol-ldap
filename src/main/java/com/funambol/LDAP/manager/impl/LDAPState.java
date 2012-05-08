package com.funambol.LDAP.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.sql.DataSource;

import com.funambol.LDAP.engine.source.AbstractLDAPSyncSource;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.LdapManagerInterface;
import com.funambol.LDAP.utils.Constants;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.framework.tools.DBTools;
import com.funambol.framework.tools.DataSourceTools;

/**
 * The class keeps track of all necessary changes in LDAP into a database
 * (method update). Based on this database the methods getAllItems, getNewItems,
 * getModifiedItems, getDeletedItems return an array of ids (entryUUID) for LDAP
 * entries.
 * 
 * @author  <a href='mailto:fipsfuchs _@ users.sf.net'>Philipp Kamps</a>
 * @author  <a href='mailto:julien.buratto _@ subitosms.it'>Julien Buratto</a>
 * @author  <a href='mailto:gdartigu _@ smartjog.com'>Gilles Dartiguelongue</a>
 * @version $Id$
 */
public class LDAPState {
	
	// ----------------------------------------------------------- Private data
	
	private static String deletedItemsSQL = "SELECT guid FROM fnbl_client_mapping WHERE principal = ? AND sync_source = ?";
	
	private Connection db;
	private LdapManagerInterface li;
	private String dbName;

	
	private DataSource datasource; // the datasource created by init()
	private FunambolLogger logger = FunambolLoggerFactory.getLogger(Constants.LOGGER_LDAP_ENGINE);

	public LdapManagerInterface getManager() {
		return li;
	}
	// ------------------------------------------------------------ Constructor
	
	/**
	 * The constructor saves the referenz to the <i>LDAPInterface</i> and the
	 * syn4j database name.
	 * 
	 * @param ldapInterface LDAPInterface connection to LDAP
	 * @param name String database name where the funambol framework saves its data
	 * TODO we could initialize here a pool of LDAP connection
	 */
	public LDAPState(LdapManagerInterface ldapInterface, String name) {
		li = ldapInterface;
		dbName = name;
		
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Ldap id attribute is " + ldapInterface.getLdapId() +"; dbname: "+ dbName);
			}
			datasource = (DataSource) DataSourceTools.lookupDataSource("jdbc/" + dbName);

		} catch (NamingException e) {
			logger.error("Name exception in LDAPState.java", e);
			e.printStackTrace();
		} catch (Exception e) {
			logger.error("OMG !!! Crashed ! Don't know what happened though...");
			e.printStackTrace();
		}
	}

	public Connection getConnection() throws SQLException {		
		if (datasource == null)
			throw new SQLException("Can't find datasource: "+ dbName);

		if (db == null || db.isClosed() ) {
			db = datasource.getConnection();
		}
		return db;

	}
	// --------------------------------------------------------- Public methods
	
	/**
	 * Get all items keys stored on the LDAP server using search base configured in module admin
	 * @throws SyncSourceException 
	 */
	public List<String> getAllEntriesLdapId() throws SyncSourceException {
		try {
			return li.getAllUids();
		} catch (LDAPAccessException e) {
			throw new SyncSourceException(e);
		}
	}
	public List<String> getAllEntries(boolean retrieveDn) throws SyncSourceException {
		try {	
			if (retrieveDn) {

				return li.getAllDns();

			} else {
				return li.getAllUids();
			}			} catch (LDAPAccessException e) {
				throw new SyncSourceException(e);

			}
	}
	
	// change the default entry filter
	public void setEntryFilter(String f) {
		li.setEntryFilter(f);
	}	
	public String getEntryFilter() {
		return li.getEntryFilter();
	}
	
	public void delete(String dn, boolean soft) {
		try {
			li.delete(dn, soft);
		} catch (LDAPAccessException e) {
			logger.error("Can't connect to ldap server: " + e.getMessage());
		}
	}
	/**
	 * Get one item stored on the LDAP server 
	 * @param syncItemKey ldapId of the item
	 * @return one sized list containing the item
	 * @throws SyncSourceException 
	 */
	public List<String> getOneItem(SyncItemKey syncItemKey) throws SyncSourceException {
		String sik = syncItemKey.getKeyAsString();
		
		try {
			return li.getOneEntry(sik);
		} catch (LDAPAccessException e) {
			throw new SyncSourceException(e);

		}
	}
	
	public Attributes getLDAPEntryById(String id) throws SyncSourceException {
		try {
			return li.searchLDAPEntryById(id).getAttributes();
		} catch (LDAPAccessException e) {
			throw new SyncSourceException(e);

		}		
	}
	/**
	 * Get items that were created since 'since'
	 * @throws SyncSourceException 
	 */
	public List<String> getNewItems(Timestamp since, Timestamp to) throws SyncSourceException {
		try {
			return li.getNewEntries(since, to);
		} catch (LDAPAccessException e) {
			throw new SyncSourceException(e);

		}
	}

	/**
	 * Get items that were modified since 'since'
	 * @throws SyncSourceException 
	 */
	public List<String> getModifiedItems(Timestamp since, Timestamp to) throws SyncSourceException {
		try {
			return li.getModifiedEntries(since, to);
		} catch (LDAPAccessException e) {
			throw new SyncSourceException(e);
		}
	}

	/**
	 * TODO try to retrieve deleted item directly from LDAP server
	 * Get items that are on the Funambol database FNBL_CLIENT_MAPPING but not on the LDAP server and consider the
	 * difference as deleted items
	 * @throws SyncSourceException 
	 */
	public List<String> getDeletedItems(AbstractLDAPSyncSource syncSource, Timestamp since) throws SyncSourceException {
		String clientId = "" + syncSource.getPrincipal().getId();
		
		// Get all entries from the mapping database
		List<String> mappingEntries = new ArrayList<String>();
		PreparedStatement query = null;	
		ResultSet rset =null;
		try {
			
			query = getConnection().prepareStatement(deletedItemsSQL);
			query.setString(1, clientId);
			query.setString(2, syncSource.getSourceURI() );
			rset = query.executeQuery();
						
			while (rset.next()) {
				try {
					// for the ldap UUID see http://www.ietf.org/rfc/rfc4530.txt it should be 36 chars long
					mappingEntries.add(rset.getString(1));
				} catch (Exception ex) {
					logger.warn("Got invalid ldapId from fnbl_client_mapping table ("	+ rset.getString(1) + ")");
				}
			}

		} catch (SQLException ex) {
			logger.warn("deletedItemsSQL Error: " + ex.toString());
		} finally {
			DBTools.close(db, query, rset);
		}
		
		// Get all entries from the LDAP server
		List<String> ldapEntries;
		try {
			ldapEntries = li.getAllUids();
		} catch (LDAPAccessException e) {
			throw new SyncSourceException(e);
		}

		if (logger.isDebugEnabled())
			logger.debug("Count of entries: LDAP=" + ldapEntries.size() + " REMOVE=" + mappingEntries.size() );

		// Get the difference (removed items on LDAP side)
		mappingEntries.removeAll( ldapEntries );

		return mappingEntries;
	}



	/**
	 * Send new contacts to the LDAP server and replace its key with the ldapId
	 * @param newItems list of new contacts to add to the server
	 * @throws Exception 
	 * @see commitNewItems()
	 */
	public void commitNewItem(List<SyncItem> newItems) throws Exception {
		//Iterator<SyncItem> it = newItems.iterator();
		
		for (SyncItem si : newItems) {
			//SyncItem si = it.next();
			
			// The entry is already on the LDAP server, skip it
			// TODO this check is made in the  addSyncItem. 
//			if (li.searchLDAPEntryById(si.getKey().getKeyAsString())!=null) {
//				continue;
//			}
			
			if (li.addNewEntry(si) == null) { 
				throw new Exception("Error adding item to LDAP: null uid");
			}
		}
	}
	

	
	/**
	 * Send updated contacts to the LDAP server
	 * @param updatedItems list of contacts to update on the server
	 * @throws SyncSourceException if something goes wrong updating LDAP // XXX
	 */
	public void commitUpdatedItems (List<SyncItem> updatedItems) throws SyncSourceException {
		Iterator<SyncItem> it = updatedItems.iterator();
		try {	
			while(it.hasNext()) {
				SyncItem si = it.next();
				li.updateEntry(si);
			}			
		} catch (LDAPAccessException e) {
			throw new SyncSourceException(e);
		}
	}
	
	public void commitUpdatedItem(SyncItem it) throws SyncSourceException {
		try {
			li.updateEntry(it);
		} catch (LDAPAccessException e) {
			throw new SyncSourceException(e);
		}
	}
	/**
	 * Delete contacts on the LDAP server
	 * @param deletedItems list of contacts to delete on the server
	 */
	public void commitDeletedItems(List<SyncItem> deletedItems) {
		Iterator<SyncItem> it = deletedItems.iterator();
		
		while(it.hasNext()) {
			SyncItem si = it.next();
			li.deleteEntry( si, false );
		}
	}
	
	/**
	 * Close connection to the database(?) server
	 */
	public void close() {
		try {
			db.close();
		} catch (SQLException ex) {
			logger.warn("Error on close");
			logger.warn("modifiedItemsSQL Error: " + ex.toString());
		}
	}

	// -------------------------------------------------------- Private methods
	
}
