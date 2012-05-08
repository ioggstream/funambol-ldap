--
-- Initialization data for the LDAP module
--
-- @version $Id: create_schema.sql,v 1.0 2009/04/22 rpolli $


create table fnbl_ldap_item (
    principal   bigint       not null,
    sync_source varchar(128) not null,
    guid        varchar(200) not null,
    status      char,
    last_update varchar(200),
    constraint pk_ldap_item primary key (principal, sync_source, guid)        
);


