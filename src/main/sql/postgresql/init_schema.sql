--
-- Initialization data for the LDAP module
--
-- @author Roberto Polli (rpolli@babel.it)
-- @version $Id$
--

--
-- Module structure registration
--

delete from fnbl_sync_source_type where id='ldap-71';
insert into fnbl_sync_source_type(id, description, class, admin_class)
values('ldap-71','LDAP SyncSource Contacts','com.funambol.LDAP.engine.source.LDAPContactsSyncSource','com.funambol.LDAP.admin.LDAPSyncSourceConfigPanel');

delete from fnbl_connector where id='ldap';
insert into fnbl_connector(id, name, description, admin_class)
values('ldap','FunambolLDAPConnector','Funambol LDAP Connector 7.1.0','');

delete from fnbl_connector_source_type where connector='ldap' and sourcetype='ldap-71';
insert into fnbl_connector_source_type(connector, sourcetype)
values('ldap','ldap-71');

delete from fnbl_module where id='ldap';
insert into fnbl_module (id, name, description)
values('ldap','ldap','LDAP 7.1.0');

delete from fnbl_module_connector where module='ldap' and connector='ldap';
insert into fnbl_module_connector(module, connector)
values('ldap','ldap');
