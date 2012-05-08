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
	Contact createContact(Attributes attrs);

	/**
	 * this have tobe re-implemented from scratch
	 * TODO verify postOfficeBox, homePostalAddress
	 */
	Attributes createEntry(Contact contact);

	String getSoftDeleteAttribute();
	String getSoftDeleteFilter();

	/**
	 * The list of attributes to retrieve 
	 * @return
	 */
	String[]  getSupportedAttributes();
	
	/**
	 * The name of the rdn attribute to use as GUID 
	 * @return
	 */
	String getRdnAttribute();
	/**
	 * The name of the rdn attribute to use as GUID 
	 * @return
	 */
	String getRdnValue(Attributes attrs);
	/**
	 * The name of the attribute where to store timestamp 
	 * @return
	 */
	String getTimestampAttribute();
	
	/**
	 * convert a syncitem to an entry of the given objectclasses
	 * @param si
	 * @return
	 */
	Attributes syncItemToLdapAttributes(SyncItem si);

	Map<String, Attributes> compareAttributeSets(
			Attributes proposedEntry, Attributes attributes) throws NamingException;
	
	/**
	 * return an ldapsearch filter with matching items 
	 * @param si
	 * @return
	 */
	String getTwinItems(SyncItem si);
	
}