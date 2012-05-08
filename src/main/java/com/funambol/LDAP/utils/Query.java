package com.funambol.LDAP.utils;

public class Query {
    //------------------------------------------------------------- fnbl_ldap_item

    
    /**
     * 
     */
    public static final String SELECT_ACCOUNT_ITEM_IDS =
            "select guid " +
            " from fnbl_ldap_item where principal=? and sync_source=?" ;
    
    /**
     *
     */
    public static final String SELECT_ACCOUNT_ITEMS =
            "select guid, status, last_update " +
            "  from fnbl_ldap_item where principal=? and sync_source=?" ;

    
    
    /**
     *
     */
    public static final String INSERT_ITEMS =
            "insert into fnbl_ldap_item " +
            "(principal, sync_source, status, guid, last_update) " +
            " values (?,?,?,?,?) ";

    /**
     *
     */
    public static final String DELETE_ITEMS =
            "delete from fnbl_ldap_item where principal=? and  sync_source=?";

    /**
     *
     */
    public static final String DELETE_ITEM =
            "delete from fnbl_ldap_item where principal=? and  sync_source=? and guid=?";


}
