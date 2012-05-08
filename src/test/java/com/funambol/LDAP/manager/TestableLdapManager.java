package com.funambol.LDAP.manager;

import javax.naming.NameAlreadyBoundException;
import javax.naming.directory.Attributes;

import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.impl.FedoraDsInterface;
import com.funambol.framework.engine.source.SyncSourceException;

/**
 * This class is used to expose protected methods of LdapManager for testing purposes
 * @author rpolli
 *
 */

public class TestableLdapManager extends FedoraDsInterface {

	public TestableLdapManager(AbstractLDAPManager i) throws LDAPAccessException {
		super();
		init(i.providerUrl,i.baseDn,i.bindDn,i.password,i.isFollowReferral(),i.isPoolingConnection(),i.getCdao(),i.getLdapId());
	}
	public String addNewEntry(Attributes a) throws NameAlreadyBoundException {
		return super.addNewEntry(a);
	}
	
	public String updateEntry(String uid, Attributes proposedEntry)
			throws SyncSourceException {
		return super.updateEntry(uid, proposedEntry);
	}


}
