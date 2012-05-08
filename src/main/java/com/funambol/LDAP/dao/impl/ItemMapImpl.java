package com.funambol.LDAP.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import javax.naming.NamingException;

import com.funambol.LDAP.dao.ItemMap;
import com.funambol.LDAP.exception.DBAccessException;
import com.funambol.LDAP.utils.Constants;
import com.funambol.LDAP.utils.Query;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.framework.tools.DBTools;
import com.funambol.framework.tools.DataSourceTools;
import com.funambol.server.db.RoutingDataSource;


public class ItemMapImpl implements ItemMap {

	private Long principal;
	private String sourceUri;
	protected FunambolLogger logger = FunambolLoggerFactory.getLogger(Constants.LOGGER_LDAP);
	private RoutingDataSource userDataSource;
	private String username;

	public void init() throws NamingException {
		if (userDataSource == null ) {
			userDataSource = (RoutingDataSource) DataSourceTools.lookupDataSource(Constants.USER_DATASOURCE_JNDINAME);
		}
	}

	public void clearMap() throws DBAccessException {
		if (logger.isTraceEnabled()) {
			logger.trace("Clear LDAP cached items");
		}

		Connection userConnection = null;
		PreparedStatement userPs = null;

		try {

			userConnection = userDataSource.getRoutedConnection(username);

			userPs = userConnection.prepareStatement(Query.DELETE_ITEMS);
			userPs.setLong(1, getPrincipal());
			userPs.setString(2, getSourceUri());

			userPs.executeUpdate();

		} catch (Exception ne) {
			throw new DBAccessException("Error remove single info from local DB", ne);
		} finally {
			try {
				DBTools.close(userConnection, userPs, null);
			} catch (Exception e) {
				throw new DBAccessException("Error setting ServerItems into DataBase (" +
						e.getMessage() + ")", e);
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("ItemMapImp end clearMap");
		}

	}

	public Long getPrincipal() {
		return principal;
	}

	public String getSourceUri() {
		return sourceUri;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Return a map of the items loaded from cache
	 * @throws DBAccessException 
	 */
	public HashMap<String, String> loadMap() throws DBAccessException {
		HashMap<String, String> ret = new HashMap<String, String>();
		Connection userConnection = null;
		PreparedStatement userPs = null;
		ResultSet userRs = null;
		try {
			if (userDataSource == null) {
				throw new DBAccessException("Datasource not initialized:" + Constants.USER_DATASOURCE_JNDINAME);
			}
			// get NUD items for the account
			userConnection = userDataSource.getRoutedConnection(username);
			userConnection.setReadOnly(true);

			userPs = userConnection.prepareStatement(Query.SELECT_ACCOUNT_ITEMS);
			userPs.setLong(1, getPrincipal());
			userPs.setString(2, getSourceUri());
			userRs = userPs.executeQuery();

			String status = null;
			String GUID = null;
			String lastModified = null;

			while (userRs.next()) {

				GUID = userRs.getString(1);
				// status = userRs.getString(2);
				lastModified = userRs.getString(3);

				ret.put(GUID, lastModified);
			}

		} catch (Exception e) {
			throw new DBAccessException(e);
		} finally {
			DBTools.close(userConnection, userPs, userRs);
		}
		return ret;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public void setPrincipal(Long l) {
		principal=l;
	}

	public void setSourceUri(String s) {
		sourceUri=s;		
	}

	public void updateMap(HashMap<String, String> mappa) throws DBAccessException {
		 Connection userConnection = null;
	        PreparedStatement userPs = null;

	        try {

	            userConnection = userDataSource.getRoutedConnection(username);
	            userConnection.setAutoCommit(false);

	            // remove data 
	            userPs = userConnection.prepareStatement(Query.DELETE_ITEMS);
	            userPs.setLong(1, getPrincipal());
	            userPs.setString(2, getSourceUri());
	            userPs.executeUpdate();


	            // insert data modified in the current sync

	            String GUID = null;

	            userPs = userConnection.prepareStatement(Query.INSERT_ITEMS);

	            // insert data AddedItems
	            Iterator<String> itemKeys = mappa.keySet().iterator();
	            while (itemKeys.hasNext()) {
	                GUID = (String) itemKeys.next();
	                if (GUID != null) {
	                    userPs.setLong(1, getPrincipal());
	                    userPs.setString(2, getSourceUri());
	                    userPs.setString(3, "N");
	                    userPs.setString(4, GUID);
	                    userPs.setString(5, mappa.get(GUID));
	                    userPs.executeUpdate();
	                }
	            }
	         
	            userConnection.commit();

	        } catch (SQLException sqle) {
	            try {
	            	if (userConnection != null) {
	            		userConnection.rollback();	            		
	            	}
	            } catch (SQLException sqlee) {
	                throw new DBAccessException("Error rollbacking", sqlee);
	            }
	            throw new DBAccessException("Error refreshing Local Items", sqle);
	        } finally {
	            try {
	                DBTools.close(userConnection, userPs, null);
	            } catch (Exception e) {
	                throw new DBAccessException("Error setting ServerItems into DataBase (" +
	                        e.getMessage() + ")", e);
	            }
	        }

	        if (logger.isTraceEnabled()) {
	            logger.trace("Refresh Cached N-U-D Items");
	        }

	}

}
