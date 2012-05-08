--
-- Initialization data for the LDAP 1.2 module
--
-- @author Philipp Kamps
-- @author 
-- @version $Id$
--

--
-- Module structure registration
--

delete from fnbl_sync_source_type where id='ldap-1.2';
insert into fnbl_sync_source_type(id, description, class, admin_class)
values('ldap-1.2','LDAP SyncSource','com.funambol.LDAP.engine.source.LDAPSyncSourceContacts','com.funambol.LDAP.admin.LDAPSyncSourceConfigPanel');

delete from fnbl_module where id='ldap-1.2';
insert into fnbl_module (id, name, description)
values('ldap-1.2','ldap-1.2','LDAP 1.2');

delete from fnbl_connector where id='ldap-1.2';
insert into fnbl_connector(id, name, description, admin_class)
values('ldap-1.2','Sync4jLDAPConnector','Sync4j LDAP Connector','');

delete from fnbl_connector_source_type where connector='ldap-1.2' and sourcetype='ldap-1.2';
insert into fnbl_connector_source_type(connector, sourcetype)
values('ldap-1.2','ldap-1.2');

delete from fnbl_module_connector where module='ldap-1.2' and connector='ldap-1.2';
insert into fnbl_module_connector(module, connector)
values('ldap-1.2','ldap-1.2');
  
  
