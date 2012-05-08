package com.funambol.LDAP.manager;

import com.funambol.LDAP.admin.LDAPSyncSourceConfigPanel;
import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.dao.impl.ContactDAO;
import com.funambol.LDAP.dao.impl.OrganizationalPersonContactDAO;
import com.funambol.LDAP.dao.impl.PiTypeContactDAO;
import com.funambol.LDAP.manager.impl.ActiveDirectoryInterface;
import com.funambol.LDAP.manager.impl.FedoraDsInterface;
import com.funambol.LDAP.manager.impl.OpenLdapInterface;
import com.funambol.LDAP.utils.Constants;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;

/**
 * This is a factory for LdapManager
 * @author rpolli
 *
 */
public class LDAPManagerFactory {
	protected static FunambolLogger logger = FunambolLoggerFactory.getLogger(Constants.LOGGER_LDAP_ENGINE);

	public static final LdapManagerInterface createLdapInterface(String interfaceName) throws SyncSourceException {
			LdapManagerInterface li;
			
			if (interfaceName.equals(Constants.SERVER_FEDORA)) {
				li = (LdapManagerInterface) new FedoraDsInterface();
			} else if (interfaceName.equals(Constants.SERVER_OPENLDAP)) {
				li = (LdapManagerInterface) new OpenLdapInterface();
			} else if (interfaceName.equals(Constants.SERVER_ACTIVEDIRECTORY)){
				li = (LdapManagerInterface) new ActiveDirectoryInterface();
			} else {
				logger.error("Allowed names are: 'FedoraDs', 'OpenLdap', 'ActiveDirectory' ");
				throw new SyncSourceException("Bad interface in SyncSource configuration file: " + interfaceName);
			}
			return li;
	}
	
	
	public static final ContactDAOInterface createContactDAO(String daoName) throws SyncSourceException {
		ContactDAOInterface ret;
		if (daoName.equals(Constants.DAO_PITYPEPERSON)) {			
			ret = (ContactDAOInterface) new PiTypeContactDAO();
		} else if (daoName.equals(Constants.DAO_INETORGPERSON)) {
			ret = (ContactDAOInterface) new ContactDAO();
		} else if (daoName.equals(Constants.DAO_ORGANIZATIONALPERSON)) {
			ret = (ContactDAOInterface) new OrganizationalPersonContactDAO();
		} else {
			logger.error(String.format("Allowed names are: %s", (Object[]) Constants.SUPPORTED_DAO) );
			throw new SyncSourceException("Bad interface in SyncSource configuration file: " + daoName);
		}
		return ret;
	}
}
