/**
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA 
 */

package com.funambol.LDAP.dao.impl;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.utils.Constants;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.common.pim.common.Property;
import com.funambol.common.pim.contact.Address;
import com.funambol.common.pim.contact.BusinessDetail;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Email;
import com.funambol.common.pim.contact.Name;
import com.funambol.common.pim.contact.Note;
import com.funambol.common.pim.contact.PersonalDetail;
import com.funambol.common.pim.contact.Phone;
import com.funambol.common.pim.contact.Title;
import com.funambol.common.pim.vcard.ParseException;
import com.funambol.common.pim.vcard.VcardParser;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemState;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.server.config.Configuration;

/**
 * This class implements the Data Access Object for <i>Contacts</i>. Based on a
 * LDAP entry this class creates a <i>Contact</i> object
 * (sync4j.foundation.pdi.contact.Contact). The <i>Contact</i> object allows to
 * be transformed to a vcard of a SIFC.
 * 
 * TODO support vcard3.0, support for entryKey
 * 
 * @author <a href='mailto:fipsfuchs _@ users.sf.net'>Philipp Kamps</a>
 * @author <a href='mailto:julien.buratto _@ subitosms.it'>Julien Buratto</a>
 * @author <a href='mailto:gdartigu _@ smartjog.com'>Gilles Dartiguelongue</a>
 * @author <a href='mailto:pventura _@ babel.it>Pietro Ventura</a>
 * @author <a hreg='mailto:rpolli _@ babel.it'>Roberto Polli</a>
 * 
 * @version $Id$
 */
public class ContactDAO implements ContactDAOInterface {
	// ----------------------------------------------------------- Private data
	public String SUPPORTED_ATTRIBUTES[] = { "givenName", "sn", "cn",
			"telephoneNumber", "description", "title",
			"facsimileTelephoneNumber", "postalCode", "postalAddress", "st",
			"l", "homePhone", "mobile", "homePostalAddress", "initials",
			"mail", "street", "modifyTimestamp" };

	// PV
	// constants for modification attributesets
	public static final String ADD_ATTRIBUTE = "ADD";
	public static final String DEL_ATTRIBUTE = "DEL";
	public static final String REPLACE_ATTRIBUTE = "REPLACE";

	protected FunambolLogger logger = FunambolLoggerFactory
	.getLogger(Constants.LOGGER_LDAP_DAO);

	protected String rdnAttribute = "cn";

	// ------------------------------------------------------------ Constructor

	/**
	 * The constructor is called the the LDAPInterface object and it stores this
	 * object as a private object variable This class works on all fields except
	 * of the LDAPID, which is managed directly into the LDAPInterface
	 * 
	 * @param ldapInterface
	 *            Referenz to the LDAP Interface to read single LDAP entries
	 */
	public ContactDAO() {

	}

	// --------------------------------------------------------- Public methods

	/**
	 * Call this method with a given array of ids for LDAP entries and it will
	 * return a corresponding <i>Contact[]</i> array.
	 * 
	 * @param ids
	 *            lists of IDs
	 * @return list of contacts corresponding to the provided IDs
	 * @deprecated this shouldn't access ldap
	 */
	// public List<Contact> getContacts( List<String> ids ){
	// List<Contact> result = new ArrayList<Contact>();
	//
	// Iterator<String> it = ids.iterator();
	//
	// while(it.hasNext()) {
	// result.add( getContact( it.next() ));
	// }
	//
	// return result;
	// }
	/**
	 * Call this method with a given id for a LDAP entry and it will return a
	 * corresponding <i>Contact</i> object.
	 * 
	 * @param id
	 *            id of the LDAP entry
	 * @return the contact corresponding to the id
	 * @deprecated this class shouldn't access ldap TODO this method makes an
	 *             LDAPSearch
	 */
	// private Contact getContact( String id )
	// {
	// Attributes entry = li.getLDAPEntryById(id).getAttributes();
	//
	// if (entry == null) {
	// logger.warn("Unable to get the LDAP entry for id: " + id);
	// return null;
	// }
	//
	// return createContact(entry);
	// }
	/**
	 * Call this method with a given <i>SyncItem</i> to get the equivalent
	 * attributes to inject in LDAP
	 * 
	 * @param si
	 *            <i>SyncItem</i> to convert to LDAP attributes
	 * @return LDAP attributes representation of the <i>SyncItem</i>
	 */
	public Attributes syncItemToLdapAttributes(SyncItem si) {
		Contact contact = getContact(si);

		if (contact == null) {
			logger
			.info("Unexpected error while retrieving contact from SyncItem");
			return null;
		}

		return createEntry(contact);
	}

	/**
	 * Call this method with a given <i>SyncItem</i>, it will return a
	 * corresponding <i>Contact</i> object.
	 * 
	 * @param syncItem
	 *            <i>SyncItem</i> to get the <i>Contact</i> from
	 * @return the contact corresponding to the <i>SyncItem</i>
	 * @throws SyncSourceException
	 *             FIXME rename this function with a more explicit name (ex.
	 *             syncItemToContact, convertItem, ..)
	 */
	protected Contact getContact(SyncItem syncItem) {

		if ( ! Arrays.asList(Constants.SUPPORTED_TYPES).contains(syncItem.getType())) {
			logger.warn("Error parsing the item "
					+ syncItem.getKey().getKeyAsString()
					+ " from vCard to a Contact object of type: " + syncItem.getType());
			
            if (Configuration.getConfiguration().isDebugMode() && logger.isTraceEnabled()) {
            	logger.trace("Item content follows: " + (new String(syncItem.getContent())));
            }
			return null;
		}
		byte[] content = syncItem.getContent();
		VcardParser parser = new VcardParser(new ByteArrayInputStream(content));
		Contact contact;

		try {
			// Convert the SyncItem to Contact
			contact = (Contact) parser.vCard();

		} catch (ParseException e) {
			logger.warn("Error parsing the item "
					+ syncItem.getKey().getKeyAsString()
					+ " from vCard to a Contact object: " + e.getMessage());
			
            if (Configuration.getConfiguration().isDebugMode() && logger.isTraceEnabled()) { 
            	logger.trace(" vCard follows: " + (new String(content)));
            }
			return null;
		}

		if (syncItem.getState() == SyncItemState.UPDATED) {
			contact.setUid((String) syncItem.getKey().getKeyValue());
		}
        if (Configuration.getConfiguration().isDebugMode() && logger.isTraceEnabled()) {
			logger.trace("Original content");
			logger.trace("----------ORIGINAL------");
			logger.trace(content.toString());

			for (int i = 0; i < content.length; i++)
				logger.trace("" + (char) (content[i]));

			logger.trace("Converted SyncItem to Contact");
			logger.trace("----------ORIGINAL------");
			logger.trace(new String(content));
		}
		return contact;
	}

	// ------------------------------------------------------ Protected methods

	/**
	 * Here's the place where all the magic happens: This method transforms a
	 * LDAP entry to a <i>Contact</i> object.
	 * 
	 * @param entry
	 *            The LDAP entry
	 * @return contact from the LDAP server
	 */
	public Contact createContact(Attributes attrs) {

		if (attrs == null) {
			logger
			.warn("Entry from LDAP is null so won't be able to create a contact");
			return null;
		}
		try {
			if (logger.isDebugEnabled())
				logger.debug("Getting attribute cn");

			String cn = (attrs.get("cn") != null) ? (String) attrs.get("cn")
					.get() : "null";
					// logger.info("Getting attribute " + ldapId);
					// String entryId = ( attrs.get(ldapId) != null ) ? (String)
					// attrs.get(ldapId).get() : "null";

					if (logger.isDebugEnabled()) {
						logger.debug("Creating Contact for " + cn
								// + ". Id is ( "+ entryId + " )"
						);
					}
					Contact contact = new Contact();

					/* Set name */
					Name name = new Name();
					if (attrs.get("cn") != null) {
						name.getDisplayName().setPropertyValue(cn);
					}
					if (attrs.get("givenName") != null) {
						name.getFirstName().setPropertyValue(
								(String) attrs.get("givenName").get());
					}
					if (attrs.get("sn") != null) {
						name.getLastName().setPropertyValue(
								(String) attrs.get("sn").get());
					}
					if (attrs.get("middleName") != null) {
						name.getMiddleName().setPropertyValue(
								(String) attrs.get("middleName").get());
					}
					if (attrs.get("title") != null) {
						name.getSalutation().setPropertyValue(
								(String) attrs.get("title").get());
					}
					if (attrs.get("nickName") != null) {
						name.getNickname().setPropertyValue(
								(String) attrs.get("nickName").get());
					}
					contact.setName(name);

					/* Set personal details */
					PersonalDetail personal = new PersonalDetail();
					BusinessDetail business = new BusinessDetail();

					/* Set email */
					if (attrs.get("mail") != null) {
						Email email = new Email();
						email.setEmailType("Email1Address");
						email.setPropertyValue((String) attrs.get("mail").get());
						personal.addEmail(email);
					}
					if (attrs.get("mailAlternateAddress") != null) {
						Email email = new Email();
						email.setEmailType("Email2Address");
						email.setPropertyValue((String) attrs.get(
						"mailAlternateAddress").get());
						personal.addEmail(email);
					}

					// (Other, Home, Business) x (Telephone,Mobile) x Number

					/* Set phone phones */
					ArrayList<Phone> allPhones = new ArrayList<Phone>();

					// telephone: home, work,
					if (attrs.get("homePhone") != null) {
						Phone homePhone = new Phone();
						homePhone.setPropertyValue((String) attrs.get("homePhone")
								.get());
						homePhone.setPhoneType("HomeTelephoneNumber");
						allPhones.add(homePhone);
					}

					if (attrs.get("mobile") != null) {
						Phone mobile = new Phone();
						mobile.setPropertyValue((String) attrs.get("mobile").get());
						mobile.setPhoneType("MobileTelephoneNumber");
						allPhones.add(mobile);
					}

					String val = LdapUtils.getPrintableAttribute(attrs.get("telephoneNumber"));
					if (StringUtils.isNotEmpty(val)) {
						Phone phone = new Phone();
						phone.setPhoneType("BusinessTelephoneNumber");
						phone.setPropertyValue(val);
						business.addPhone(phone); 
					}
					val = LdapUtils.getPrintableAttribute(attrs.get("facsimileTelephoneNumber"));
					if (StringUtils.isNotEmpty(val)) {
						Phone phone = new Phone();
						phone.setPhoneType("BusinessFaxNumber");
						phone.setPropertyValue(val);
						business.addPhone(phone); 
					}


					personal.setPhones(allPhones);

					/*
					 * Set address
					 *//*
					 * if(entry.getAttribute("postalCode")!= null) {
					 * personal.getAddress().getPostalCode().setPropertyValue(
					 * entry.getAttribute("postalCode").getStringValue()); }
					 * 
					 * if(entry.getAttribute("l")!= null) {
					 * personal.getAddress().getCity().setPropertyValue(
					 * entry.getAttribute("l").getStringValue()); }
					 * 
					 * if(entry.getAttribute("postalAddress")!= null) {
					 * personal.getAddress().getStreet().setPropertyValue(
					 * entry.getAttribute("postalAddress").getStringValue()); }
					 */

					/* title */
					if (attrs.get("title") != null) {
						NamingEnumeration<?> values = attrs.get("title").getAll();
						ArrayList<Title> titles = new ArrayList<Title>();
						while (values.hasMoreElements()) {
							titles.add(new Title((String) values.nextElement()));

						}
						business.setTitles(titles);
					}

					if (attrs.get("street") != null) {

						NamingEnumeration<?> streets = attrs.get("street").getAll();
						while (streets.hasMoreElements()) {
							business.getAddress().getStreet().setPropertyValue(
									(String) streets.nextElement());
						}

					}
						if (attrs.get("c") != null) {
							business.getAddress().getCountry().setPropertyValue(
									(String) attrs.get("c").get());
						}

						if (attrs.get("l") != null) {
							business.getAddress().getCity().setPropertyValue(
									(String) attrs.get("l").get());
						}

						if (attrs.get("postalCode") != null) {
							business.getAddress().getPostalCode().setPropertyValue(
									(String) attrs.get("postalCode").get());
						}

						if (attrs.get("postalAddress") != null) {
							business.getAddress().getStreet().setPropertyValue(
									(String) attrs.get("postalAddress").get());
						}
					

					if (attrs.get("o") != null) {
						business.getCompany().setPropertyValue(
								(String) attrs.get("o").get());
					}
					if (attrs.get("calFbUrl") != null) {
						contact.setFreeBusy((String) attrs.get("calFbUrl").get());
					}
					contact.setPersonalDetail(personal);
					contact.setBusinessDetail(business);

					// contact.setUid(entryId);

					return contact;
		} catch (Exception e) {
			logger.warn("Error in getting entry values: ", e);
			return null;
		}
	}

	/**
	 * Convert a <i>Contact</i> into a LDAP inetOrgPerson set of attributes.
	 * This method is used in from Client to Server
	 * 
	 * @param contact
	 *            contact to transform into Attributes
	 * @return Attributes representation of the contact
	 */
	public Attributes createEntry(Contact contact) {

		if (logger.isTraceEnabled())
				logger.trace("Working on contact:" + contact.getUid());

		Attributes attributeSet = new BasicAttributes();
		Attribute objClass = new BasicAttribute("objectClass");
		if (logger.isDebugEnabled())
			logger.debug("Ok let's add objectclass");
		
		objClass.add("inetOrgPerson");
		objClass.add("person");

		attributeSet.put(objClass);
		try {

			if (contact.getUid() == null) {
				contact.setUid(createUniqueId(contact));
				logger.info("UID is now: " + contact.getUid());
			}

			// Split contact object into sub-objects
			Name name = contact.getName();
			PersonalDetail personal = contact.getPersonalDetail();
			BusinessDetail business = contact.getBusinessDetail();

			List phones = personal.getPhones();
			List businessPhones = business.getPhones();

			List mails = personal.getEmails();
			List note = contact.getNotes();

			// personal address
			Address addr = personal.getAddress();

			// if displayname doesn't exist and the firstname and the lastname
			// are not both defined, this will result in a NullPointerException
			// I don't want to support any other ways of doing this right now.
			// a solution could be to use an UID for the rdn
			if (name != null) {

				if (propertyCheck(name.getLastName())) {
					attributeSet.put(new BasicAttribute("sn", name
							.getLastName().getPropertyValueAsString()));
				} else {
					attributeSet.put(new BasicAttribute("sn", ""));
				}

				if (propertyCheck(name.getFirstName())) {
					attributeSet.put(new BasicAttribute("givenName", name
							.getFirstName().getPropertyValueAsString()));
				} else {
					attributeSet.put(new BasicAttribute("givenName", ""));
				}

				attributeSet.put(new BasicAttribute("cn", name.getFirstName()
						.getPropertyValueAsString()
						+ " " + name.getLastName().getPropertyValueAsString()));
			}

			// Company name
			if (business != null && propertyCheck(business.getCompany())) {
				attributeSet.put(new BasicAttribute("o", business.getCompany()
						.getPropertyValueAsString()));
			}

			// Adding phones
			if (phones != null && !phones.isEmpty()) {

				Iterator iter2 = phones.iterator();
				while (iter2.hasNext()) {
					Phone phone = (Phone) iter2.next();

					// if empty, no need to check type
					if (!propertyCheck(phone))
						continue;

					// Home phones
					if (phone.getPhoneType().equals("HomeTelephoneNumber")) {
						attributeSet.put(new BasicAttribute("homePhone", phone
								.getPropertyValueAsString()));
					}

					// MobilePhones
					if (phone.getPhoneType().equals("MobileTelephoneNumber"))
						attributeSet.put(new BasicAttribute("mobile", phone
								.getPropertyValueAsString()));

				}
			}

			// Adding business phones
			if (businessPhones != null && !businessPhones.isEmpty()) {

				Iterator iter2 = businessPhones.iterator();
				while (iter2.hasNext()) {
					Phone phone = (Phone) iter2.next();

					// if empty, no need to check type
					if (!propertyCheck(phone))
						continue;

					// Business phones
					if (phone.getPhoneType().equals("BusinessTelephoneNumber")) {
						attributeSet.put(new BasicAttribute("telephoneNumber",
								phone.getPropertyValueAsString()));
					}
					// Fax
					if (phone.getPhoneType().equals("BusinessFaxNumber")) {
						attributeSet.put(new BasicAttribute(
								"facsimiletelephonenumber", phone
								.getPropertyValueAsString()));
					}
				}
			}

			if (mails != null && !mails.isEmpty()) {

				Iterator iter1 = mails.iterator();

				// For each email address, add it
				while (iter1.hasNext()) {
					Email mail = (Email) iter1.next();
					if (propertyCheck(mail))
						attributeSet.put(new BasicAttribute("mail", mail
								.getPropertyValueAsString()));
				}
			}

			// Address
			if (addr != null) {
				if (propertyCheck(personal.getAddress().getPostalCode()))
					attributeSet.put(new BasicAttribute("postalCode", personal
							.getAddress().getPostalCode()
							.getPropertyValueAsString()));

				if (propertyCheck(personal.getAddress().getStreet()))
					attributeSet.put(new BasicAttribute("postalAddress",
							personal.getAddress().getStreet()
							.getPropertyValueAsString()));

				if (propertyCheck(personal.getAddress().getCity()))
					attributeSet
					.put(new BasicAttribute("l", personal.getAddress()
							.getCity().getPropertyValueAsString()));
			}

			// Notes
			if (note != null && !note.isEmpty()) {
				Iterator note1 = note.iterator();
				while (note1.hasNext()) {
					Note nota = (Note) note1.next();
					if (propertyCheck(nota))
						attributeSet.put(new BasicAttribute("description", nota
								.getPropertyValueAsString()));
				}
			}

			logger.info("Resulting LDAPAttributeSet is:");

			NamingEnumeration<String> ids = attributeSet.getIDs();

			while (ids.hasMoreElements()) {
				String attrID = ids.nextElement();
				logger.info(attrID + ": "
						+ ((String) attributeSet.get(attrID).get()));

			}

			// Create the LDAPEntry with dn and attributes
			// THE DN is the DisplayName
			return attributeSet;

		} catch (Exception e) {
			logger.warn("Unable to create LDAPEntry from Contact: "
					+ e.toString(), e);
			return null;
		}
	}

	// -------------------------------------------------------- Private methods

	/**
	 * Check if the property has a value
	 * TODO use StringUtils
	 * @param pro
	 *            Property to be checked
	 * @return returns true is the value is empty, false if not.
	 */
	private boolean propertyCheck(Property pro) {
		if (pro.getPropertyValueAsString() != null
				&& !pro.getPropertyValueAsString().equals("")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates a unique identifier
	 * 
	 * @param contact
	 *            contact needing an id
	 * @return id of the contact
	 */
	protected String createUniqueId(Contact contact) {
		Date ora = new Date();

		if (contact.getUid() == null)
			return "fnbl-id-"
			+ Math
			.abs(((contact.toString() + ora.getTime())
					.hashCode()));
		else
			return contact.getUid() + "-" + ora.getTime();
	}

	// --------------------------------------------------------- Static methods

	/**
	 * Compares two attribute sets
	 * 
	 * @param authoritativeSet
	 *            reference set
	 * @param compareSet
	 *            comparative set
	 * @return list of modifications to commit
	 * @throws NamingException
	 */
	public Map<String, Attributes> compareAttributeSets(
			Attributes authoritativeSet, Attributes compareSet)
			throws NamingException {

		Map<String, Attributes> modifications = new HashMap<String, Attributes>();
		Attributes delAttributes = new BasicAttributes();
		Attributes addAttributes = new BasicAttributes();
		Attributes replaceAttributes = new BasicAttributes();
		// List<LDAPModification> modifications = new
		// ArrayList<LDAPModification>();
		List<String> supportedAttrs = Arrays.asList(getSupportedAttributes());

		Iterator<String> it = supportedAttrs.iterator();

		// loop over supported attributes
		while (it.hasNext()) {
			String attribute = it.next();

			// skip unmodifiable attrs
			if (attribute.equals("modifyTimestamp"))
				continue;

			Attribute authoritaveAttribute = authoritativeSet.get(attribute);
			Attribute compareAttribute = compareSet.get(attribute);

			if (authoritaveAttribute == null || compareAttribute == null) {
				// remove an old attribute
				if (authoritaveAttribute == null && compareAttribute != null) {
					delAttributes.put(compareAttribute);
				}

				// add a new attribute
				if (authoritaveAttribute != null && compareAttribute == null) {
					addAttributes.put(authoritaveAttribute);
				}
			} else {
				// replace an attribute
				String authValue = (String) authoritaveAttribute.get();
				String compareValue = (String) compareAttribute.get();
				if (!authValue.equals(compareValue)) {
					replaceAttributes.put(authoritaveAttribute);
				}
			}
		}
		modifications.put(DEL_ATTRIBUTE, delAttributes);
		modifications.put(REPLACE_ATTRIBUTE, replaceAttributes);
		modifications.put(ADD_ATTRIBUTE, addAttributes);

		return modifications;
	}

	public String getSoftDeleteAttribute() {
		return null;
	}

	public String getSoftDeleteFilter() {
		if (getSoftDeleteAttribute() != null) {
			return String.format("(!(%s=*))", getSoftDeleteAttribute());
		} else {
			return null;
		}
	}

	public String[] getSupportedAttributes() {
		return SUPPORTED_ATTRIBUTES;
	}

	public String getRdnAttribute() {
		return rdnAttribute;
	}

	public String getRdnValue(Attributes attrs) {
		String ret = null;
		if (attrs != null) {
			ret = LdapUtils.getPrintableAttribute(attrs.get(getRdnAttribute()));
		}
		return ret;
	}

	public String getTimestampAttribute() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTwinItems(SyncItem si) {
		// TODO Auto-generated method stub
		return null;
	}

}
