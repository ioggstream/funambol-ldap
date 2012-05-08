package com.funambol.LDAP.dao;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.funambol.common.pim.contact.Contact;
import com.funambol.framework.engine.SyncItem;

public interface ContactDAOInterface {


	/**
	 * Create a Contact from ldap
	 * extended ldap attributes for converting from/to ldap
	 */
	public abstract Contact createContact(Attributes attrs);

	/**
	 * this have tobe re-implemented from scratch
	 * TODO verify postOfficeBox, homePostalAddress
	 */
	public abstract Attributes createEntry(Contact contact);

	public abstract String getSoftDeleteAttribute();
	public abstract String getSoftDeleteFilter();

	/**
	 * The list of attributes to retrieve 
	 * @return
	 */
	public abstract String[]  getSupportedAttributes();
	
	/**
	 * The name of the rdn attribute to use as GUID 
	 * @return
	 */
	public abstract String getRdnAttribute();
	/**
	 * The name of the rdn attribute to use as GUID 
	 * @return
	 */
	public abstract String getRdnValue(Attributes attrs);
	/**
	 * The name of the attribute where to store timestamp 
	 * @return
	 */
	public abstract String getTimestampAttribute();
	
	/**
	 * convert a syncitem to an entry of the given objectclasses
	 * @param si
	 * @return
	 */
	public abstract Attributes syncItemToLdapAttributes(SyncItem si);

	public abstract Map<String, Attributes> compareAttributeSets(
			Attributes proposedEntry, Attributes attributes) throws NamingException;
	
	/**
	 * return an ldapsearch filter with matching items 
	 * @param si
	 * @return
	 */
	String getTwinItems(SyncItem si);
	
}