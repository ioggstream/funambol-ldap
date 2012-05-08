package com.funambol.LDAP.dao.impl;

import java.util.ArrayList;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.common.pim.contact.Address;
import com.funambol.common.pim.contact.BusinessDetail;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Email;
import com.funambol.common.pim.contact.Name;
import com.funambol.common.pim.contact.PersonalDetail;
import com.funambol.common.pim.contact.Phone;
import com.funambol.common.pim.contact.Title;

/**
 * Contact DAO for inetOrgPerson objectClass
 * @author rpolli
 *
 */
public class PersonContactDAO extends ContactDAO implements ContactDAOInterface {
	// supported more attributes
	public static final String SUPPORTED_ATTRIBUTES[] = {
		// inetOrgPerson
		"cn",
		"givenName",	"sn",  "description",  "title", "initials", "displayName", 


		"telephoneNumber", "facsimileTelephoneNumber", // work phone
		"homePhone",		"mobile",								// personal phone
		"postOfficeBox", "postalCode", "postalAddress", "street", "l", "st", // work address
		"homePostalAddress",										// home address
		"mail",

		// mailaccount
		"mailAlternateAddress",	
		"modifyTimestamp"

	};

	protected String personObjectClasses[] = {
			"inetOrgPerson",
			"person",
	};
	public PersonContactDAO() {

	}

	@Override
	/**
	 * Create a PIMContact from Ldif. 
	 * TODO This class supports more than permitted attributes
	 */
	public Contact createContact(Attributes attrs){

		if (attrs==null){
			logger.warn("Entry from LDAP is null so won't be able to create a contact");
			return null;
		}

		String cn = LdapUtils.getPrintableAttribute(attrs.get("cn"));
	//	String entryId = LdapUtils.getPrintableAttribute(attrs.get(ldapId));
		if (StringUtils.isEmpty(cn) 
				// || StringUtils.isEmpty(entryId)
				) {
			logger.warn("Entry from LDAP has void cn so won't be able to create a contact");
			if (logger.isTraceEnabled()) {
				logger.trace("Entry attributes are: " + attrs);
			}
		}


		if (logger.isDebugEnabled()) {
			logger.debug("Creating Contact for "+ cn );
		}
		Contact contact = new Contact();

		// Set name 
		Name name = new Name();
		String val = LdapUtils.getPrintableAttribute(attrs.get("cn"));
		if (! val.equals("")) {
			name.getDisplayName().setPropertyValue((String) val);
		}
		 val = LdapUtils.getPrintableAttribute(attrs.get("givenName"));
		if (! val.equals("")) {
			name.getFirstName().setPropertyValue((String) val);
		}
		val = LdapUtils.getPrintableAttribute(attrs.get("sn"));
		if (! val.equals("")) {
			name.getLastName().setPropertyValue((String) val);
		}

		val = LdapUtils.getPrintableAttribute(attrs.get("initials"));
		if (! val.equals("")) {
			name.getInitials().setPropertyValue((String) val);
		}
		contact.setName(name);		

		//
		// business data:  street, postalCode, postalAddress, postOfficeBox, localityName, State, Country
		//
		BusinessDetail business =  contact.getBusinessDetail();
		Address businessAddress =business.getAddress();
		val = LdapUtils.getPrintableAttribute(attrs.get("st"));
		if (! val.equals(""))
			businessAddress.getState().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("co")); // this is not set in inetOrg
		if (! val.equals(""))
			businessAddress.getCountry().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("l"));
		if (! val.equals(""))
			businessAddress.getCity().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("street"));
		if (! val.equals(""))
			businessAddress.getStreet().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("postalAddress"));
		if (! val.equals(""))
			businessAddress.getExtendedAddress().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("postalCode"));
		if (! val.equals(""))
			businessAddress.getPostalCode().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("postOfficeBox"));
		if (! val.equals(""))
			businessAddress.getPostOfficeAddress().setPropertyValue((String) val);

		//
		// business info: title, organization
		//
		if(attrs.get("title")!=null) {
			NamingEnumeration<?> values;
			try {
				values = attrs.get("title").getAll();
				ArrayList<Title> titles = new ArrayList<Title>();
				while (values.hasMoreElements()) {
					titles.add(new Title( (String) values.nextElement()));
				}
				business.setTitles( titles );
			} catch (NamingException e) {
				logger.error("Error while retrieving title from contact");
			}
		}

		// organization
		val = LdapUtils.getPrintableAttribute(attrs.get("o"));
		if (! val.equals(""))
			business.getCompany().setPropertyValue((String) val);

		
		
		//
		// Phones are all personal, as we care only of vcard representation of the object
		// 
		PersonalDetail personal = contact.getPersonalDetail();

		ArrayList<Phone> allPhones = new ArrayList<Phone>();

		// telephone: home, work, 
		val = LdapUtils.getPrintableAttribute(attrs.get("homePhone"));
		if (! val.equals("")){
			Phone homePhone = new Phone();
			homePhone.setPropertyValue((String)  val);
			homePhone.setPhoneType("HomeTelephoneNumber");
			allPhones.add(homePhone);
		}
		val = LdapUtils.getPrintableAttribute(attrs.get("mobile"));
		if (! val.equals("")){
			Phone mobile = new Phone();
			mobile.setPropertyValue((String)  val);
			mobile.setPhoneType("MobileTelephoneNumber");
			allPhones.add(mobile);
		}
		val = LdapUtils.getPrintableAttribute(attrs.get("telephoneNumber"));
		if (! val.equals("")){
			Phone workPhone = new Phone();
			workPhone.setPropertyValue((String) val);
			workPhone.setPhoneType("PrimaryTelephoneNumber");
			allPhones.add(workPhone);
		}

		val = LdapUtils.getPrintableAttribute(attrs.get("facsimileTelephoneNumber"));
		if (! val.equals("")){
			Phone fax = new Phone();
			fax.setPropertyValue((String)  val);
			fax.setPhoneType("BusinessFaxNumber");
			allPhones.add(fax);
		}
		personal.setPhones(allPhones);		

		// email
		val = LdapUtils.getPrintableAttribute(attrs.get("mail"));
		if (! val.equals("")){
			Email email = new Email();
			email.setEmailType("Email1Address");
			email.setPropertyValue((String)  val);
			personal.addEmail(email);
		}
		val = LdapUtils.getPrintableAttribute(attrs.get("mailAlternateAddress"));
		if (! val.equals("")){
			Email email = new Email();
			email.setEmailType("Email2Address");
			email.setPropertyValue((String)  val);
			personal.addEmail(email);
		}
		
		
		
		//
		// other stuffs
		//
		val = LdapUtils.getPrintableAttribute(attrs.get("description"));
		if (! val.equals("")) {
			contact.setSubject(val);
		}

		return contact;
	}

	
	@Override
	public String[] getSupportedAttributes() {
		return SUPPORTED_ATTRIBUTES;
	}
	
	private Attributes createBusinessAddressAttribute(Address addr) {
		Attributes attrs = new BasicAttributes();
		
		// business address (inetOrgPerson):  postOfficeBox, postalAddress,  postalCode, street,  l, st, c 
		String str = LdapUtils.getPrintableProperty(addr.getPostOfficeAddress());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("postOfficeBox", str); 
		}
		str = LdapUtils.getPrintableProperty(addr.getExtendedAddress());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("postalAddress", str);
		}
		str = LdapUtils.getPrintableProperty(addr.getStreet());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("street", str);
		}
		str = LdapUtils.getPrintableProperty(addr.getPostalCode());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("postalCode", str);
		}
		str = LdapUtils.getPrintableProperty(addr.getCity());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("l", str);
		}
		str = LdapUtils.getPrintableProperty(addr.getState());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("st", str); 
		}
		str = LdapUtils.getPrintableProperty(addr.getCountry());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("co", str);
		}
		return attrs;
	}
	
	private Address createBusinessAddress(Attributes attrs) {
		Address businessAddress = new Address();
		
		String val = LdapUtils.getPrintableAttribute(attrs.get("st"));
		if (! val.equals(""))
			businessAddress.getState().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("co")); // this is not set in inetOrg
		if (! val.equals(""))
			businessAddress.getCountry().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("l"));
		if (! val.equals(""))
			businessAddress.getCity().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("street"));
		if (! val.equals(""))
			businessAddress.getStreet().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("postalAddress"));
		if (! val.equals(""))
			businessAddress.getExtendedAddress().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("postalCode"));
		if (! val.equals(""))
			businessAddress.getPostalCode().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("postOfficeBox"));
		if (! val.equals(""))
			businessAddress.getPostOfficeAddress().setPropertyValue((String) val);
		
		return businessAddress;
	}
	
//	val = LdapUtils.getPrintableAttribute(attrs.get("homePhone"));
//	if (! val.equals("")){
//		Phone homePhone = new Phone();
//		homePhone.setPropertyValue((String)  val);
//		homePhone.setPhoneType("HomeTelephoneNumber");
//		allPhones.add(homePhone);
//	}
//	val = LdapUtils.getPrintableAttribute(attrs.get("mobile"));
//	if (! val.equals("")){
//		Phone mobile = new Phone();
//		mobile.setPropertyValue((String)  val);
//		mobile.setPhoneType("MobileTelephoneNumber");
//		allPhones.add(mobile);
//	}
//	val = LdapUtils.getPrintableAttribute(attrs.get("telephoneNumber"));
//	if (! val.equals("")){
//		Phone workPhone = new Phone();
//		workPhone.setPropertyValue((String) val);
//		workPhone.setPhoneType("PrimaryTelephoneNumber");
//		allPhones.add(workPhone);
//	}
//
//	val = LdapUtils.getPrintableAttribute(attrs.get("facsimileTelephoneNumber"));
//	if (! val.equals("")){
//		Phone fax = new Phone();
//		fax.setPropertyValue((String)  val);
//		fax.setPhoneType("BusinessFaxNumber");
//		allPhones.add(fax);
//	}
//	personal.setPhones(allPhones);		

	
//	// email
//	val = LdapUtils.getPrintableAttribute(attrs.get("mail"));
//	if (! val.equals("")){
//		Email email = new Email();
//		email.setEmailType("Email1Address");
//		email.setPropertyValue((String)  val);
//		personal.addEmail(email);
//	}
//	val = LdapUtils.getPrintableAttribute(attrs.get("mailAlternateAddress"));
//	if (! val.equals("")){
//		Email email = new Email();
//		email.setEmailType("Email2Address");
//		email.setPropertyValue((String)  val);
//		personal.addEmail(email);
//	}
//	
}
