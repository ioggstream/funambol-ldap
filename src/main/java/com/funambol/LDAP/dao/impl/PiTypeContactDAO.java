package com.funambol.LDAP.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.common.pim.contact.Address;
import com.funambol.common.pim.contact.BusinessDetail;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Email;
import com.funambol.common.pim.contact.Name;
import com.funambol.common.pim.contact.Note;
import com.funambol.common.pim.contact.PersonalDetail;
import com.funambol.common.pim.contact.Phone;
import com.funambol.common.pim.contact.Title;
import com.funambol.common.pim.contact.WebPage;
import com.funambol.framework.engine.SyncItem;

/**
 * Implements DAO for PiTypePerson, an extended objectclass with 1-1 mapping between vcard and ldap
 * 
 * The rdn attribute is piEntryId, and there's the ability to store the syncItem timestamp in a custom attribute
 * 
 * TODO support for QP
 * @author rpolli
 *
 */
public class PiTypeContactDAO extends ContactDAO implements ContactDAOInterface {
	private static final String TIMESTAMP_ATTR = "piLastModifiedBy";
	private static final String FILTER_CREATED_ENTRY = String.format("(!(%s=*))", TIMESTAMP_ATTR);
//	private static final String FILTER_UPDATED_ENTRY = String.format("(%s>=%s)", TIMESTAMP_ATTR);
	protected static final String RDN_ATTR = "piEntryId";
	
	protected static final String FIRSTNAME = "givenName";
	protected static final String LASTNAME = "sn";
	protected static final String FULLNAME = "fullname";
	protected static final String EMAIL1ADDRESS = "Email1Address";
	protected static final String EMAIL2ADDRESS = "Email2Address";
	protected static final String EMAIL3ADDRESS = "Email3Address";
	protected static final String PHONEMOBILEHOME = "MobileHomeTelephoneNumber";
	protected static final String PHONEMOBILE = "MobileTelephoneNumber";
	protected static final String PHONEPRIMARY = "PrimaryTelephoneNumber";


	// supported more attributes
	public static final String SUPPORTED_ATTRIBUTES[] = {
		RDN_ATTR, TIMESTAMP_ATTR, "modifyTimestamp",

		// piTypePerson
		"givenName",	"sn",  "middleName", "fullname", "title", "displayName", "nickname",

		"dateOfBirth", "company", "notes",

		"workPOBox", "workPostalAddress",	 "workPostalCode", "workCity", "workState", "workCountry", 		// work address
		"homePOBox", "homePostalAddress",	 "homePostalCode", "homeCity", "homeState", "homeCountry", 		// home address

		"piEmail1", "piEmail2", "piEmail3",																			// email
		"piPhone1", "piPhone2", "piPhone3", "piPhone4", "piPhone5", "piPhone6", "piPhone7", "piPhone8",
		"piPhone9",	"piPhone10",		"piPhone11", "piPhone12", "piPhone13",						// phones
		"piWebsite1","piWebsite2",

		"inetCalendar", "inetFreeBusy"
	};


	protected static String personObjectClasses[] = {
		"piTypePerson",
		"piEntry",
		"top"
	};

	HashMap<String,String> attributeMap = new HashMap<String, String>();


	public  String getRdnAttribute() {
		return RDN_ATTR;
	}
	/**
	 * Constructor
	 */
	public PiTypeContactDAO() {

		// create ldap-vcard mapping
		attributeMap.put("Email1Address", "piEmail1");
		attributeMap.put("Email2Address", "piEmail2");
		attributeMap.put("Email3Address", "piEmail3");
		//	attributeMap.put("Email4Address", "piEmail4");

		//piTypePerson
		attributeMap.put("BusinessTelephoneNumber", "piPhone1");
		attributeMap.put("MobileHomeTelephoneNumber", "piPhone2");
		attributeMap.put("MobileBusinessTelephoneNumber", "piPhone3");

		attributeMap.put("OtherTelephoneNumber", "piPhone4");
		attributeMap.put("CarTelephoneNumber", "piPhone5");
		attributeMap.put("CompanyMainTelephoneNumber", "piPhone6");
		attributeMap.put("HomeFaxNumber", "piPhone7");
		attributeMap.put("OtherFaxNumber", "piPhone8");
		attributeMap.put("TelephoneNumber", "piPhone9");
		attributeMap.put("HomeTelephoneNumber", "piPhone10");
		attributeMap.put("PrimaryTelephoneNumber", "piPhone11");
		attributeMap.put("MobileTelephoneNumber", "piPhone12");
		attributeMap.put("BusinessFaxNumber", "piPhone13");
	}


	public String getTimestampAttribute() {
		return TIMESTAMP_ATTR;
	}






	/**
	 * Call this method with a given <i>SyncItem</i> to get the equivalent attributes to inject in LDAP
	 * 
	 * This method sets the RDN and the TimeStamp field for funambol
	 * @param si <i>SyncItem</i> to convert to LDAP attributes
	 * @return LDAP attributes representation of the <i>SyncItem</i>
	 */
	public Attributes syncItemToLdapAttributes(SyncItem si)
	{
		Contact contact = getContact(si);
		Timestamp ts = si.getTimestamp();

		if (contact == null) {
			logger.info("Unexpected error while extracting contact from SyncItem");
			return null;
		}

		Attributes attrs = createEntry(contact);
				
		
		// if the entry has a timestamp, put it on ldap
		if (ts != null) {
			String sinceUtc = LdapUtils.Timestamp2UTC(ts);
			attrs.put(getTimestampAttribute(), sinceUtc);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Item has no Timestamp");
			}
		}

		return attrs;
	}





	private Name createName(Attributes attrs) {
		Name name = new Name();
		String val = LdapUtils.getPrintableAttribute(attrs.get("fullname")); // defined in PiTypePerson
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
		val = LdapUtils.getPrintableAttribute(attrs.get("middleName")); // defined in PiTypePerson
		if (! val.equals("")) {
			name.getMiddleName().setPropertyValue((String) val);
		}
		val = LdapUtils.getPrintableAttribute(attrs.get("suffix")); // defined in PiTypePerson
		if (! val.equals("")) {
			name.getSuffix().setPropertyValue((String) val);
		}
		val = LdapUtils.getPrintableAttribute(attrs.get("title")); // defined in newPilotPerson
		if (! val.equals("")) {
			name.getSalutation().setPropertyValue((String) val);
		}
		return name;
	}



	private Address createAddress(Attributes attrs, String prefix) {

		Address address = new Address();
		// pesonal data:  homePOBox,  homePostalCode, homePostalAddress, homeCity, homeState, homeCountry
		String val = LdapUtils.getPrintableAttribute(attrs.get(prefix+"POBox"));
		if (! val.equals(""))
			address.getPostOfficeAddress().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get(prefix+"PostalCode"));
		if (! val.equals(""))
			address.getPostalCode().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get(prefix+"PostalAddress"));
		if (! val.equals(""))
			address.getStreet().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get(prefix+"City"));
		if (! val.equals(""))
			address.getCity().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get(prefix+"State"));
		if (! val.equals(""))
			address.getState().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get(prefix+"Country"));
		if (! val.equals(""))
			address.getCountry().setPropertyValue((String) val);

		return address;
	}





	/* (non-Javadoc)
	 * @see com.funambol.LDAP.dao.ContactDAOInterface#createContact(javax.naming.directory.Attributes)
	 */
	@Override
	public Contact createContact(Attributes attrs) {

		// Create contact with inetOrgPerson attributes. Return null if missing required attributes
		Contact contact =  new Contact();

		// create name
		contact.setName(createName(attrs));

		// business and personal detail
		PersonalDetail personal = contact.getPersonalDetail();
		BusinessDetail business = contact.getBusinessDetail();

		business.setAddress(createAddress(attrs, "work"));
		personal.setAddress(createAddress(attrs, "home"));

		//
		// other stuffs
		//
		String val = LdapUtils.getPrintableAttribute(attrs.get("dateOfBirth"));
		if (! val.equals(""))
			personal.setBirthday((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("notes"));
		if (! val.equals("")) {
			Note note = new Note();
			note.setPropertyValue(val);
			contact.addNote(note);
		}


		//
		// business info: title, organization
		//
		if(attrs.get("jobTitle")!=null) {
			NamingEnumeration<?> values;
			try {
				values = attrs.get("jobTitle").getAll();
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
		val = LdapUtils.getPrintableAttribute(attrs.get("company"));
		if (! val.equals(""))
			business.getCompany().setPropertyValue((String) val);

		val = LdapUtils.getPrintableAttribute(attrs.get("inetFreeBusy"));
		if (! val.equals(""))
			contact.setFreeBusy((String) val);

		// telephone: home, work, 

		// personal email
		for (int i=1; i<4; i++) {
			String attribute = "piEmail"+i;
			val = LdapUtils.getPrintableAttribute(attrs.get(attribute));
			String type = reverseAttributeMap(attribute);
			if (StringUtils.isNotEmpty(val) && type!=null) {
				Email email = new Email();
				email.setEmailType(String.format("Email%dAddress",i));
				email.setPropertyValue(val);
				personal.addEmail(email); 
			}
		}
		for (int i=1; i<=13; i++) {
			String attribute = "piPhone"+i;
			val = LdapUtils.getPrintableAttribute(attrs.get(attribute));
			String type = reverseAttributeMap(attribute);
			if (StringUtils.isNotEmpty(val) && type!=null) {
				Phone phone = new Phone();
				phone.setPhoneType(type);
				phone.setPropertyValue(val);
				personal.addPhone(phone); 
			}
		}


		val = LdapUtils.getPrintableAttribute(attrs.get("piWebsite1"));
		if (! val.equals("")) {
			WebPage page = new WebPage();
			page.setPropertyValue((String) val);
			page.setPropertyType("WebPage");
			business.addWebPage(page);

		}
		val = LdapUtils.getPrintableAttribute(attrs.get("piWebsite2"));
		if (! val.equals("")) {
			WebPage page = new WebPage();
			page.setPropertyValue((String) val);
			page.setPropertyType("WebPage");
			business.addWebPage(page);
		}
		return contact;
	}

	/**
	 * Return the ObjectClasses for this DAO 
	 * @return
	 */
	public static Attribute getObjectClasses() {
		Attribute objClass=new BasicAttribute("objectClass");
		for (String o : personObjectClasses) {
			objClass.add(o);
		}
		return objClass;
	}

	private void addAddressAttributes(Address addr, String addressType, Attributes attrs) {
		if (logger.isTraceEnabled()) {
			logger.trace( 
					String.format("addr type:%s", addr.getExtendedAddress().getPropertyValueAsString())
			);
		}
		String str = LdapUtils.getPrintableProperty(addr.getPostOfficeAddress());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put(addressType+"POBox", str);
		}
		str = LdapUtils.getPrintableProperty(addr.getStreet());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put(addressType+"PostalAddress", str);
		}
		str = LdapUtils.getPrintableProperty(addr.getPostalCode());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put(addressType+"PostalCode", str);
		}
		str = LdapUtils.getPrintableProperty(addr.getCity());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put(addressType+"City", str);
		}
		str = LdapUtils.getPrintableProperty(addr.getState());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put(addressType+"State", str);
		}
		str = LdapUtils.getPrintableProperty(addr.getCountry());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put(addressType+"Country", str);
		}	

	}

	/* (non-Javadoc)
	 * @see com.funambol.LDAP.dao.ContactDAOInterface#createEntry(com.funambol.common.pim.contact.Contact)
	 */
	@SuppressWarnings("unchecked")
	public Attributes createEntry(Contact contact) {
		Attributes attrs = new BasicAttributes();

		// create standard objectclasses
		Attribute objClass=new BasicAttribute("objectClass");
		for (String o : personObjectClasses) {
			objClass.add(o);
		}
		attrs.put(objClass);

		// set the RDN value, eventually creating it
		String uid = contact.getUid();
		if (StringUtils.isEmpty(uid)) {
			uid = UUID.randomUUID().toString();
			if (logger.isDebugEnabled()) {
				logger.debug("Generating random UUID for entry: " + uid); 
			}
		}
		attrs.put(getRdnAttribute(), uid);

		// parse contact and add fields to attribute
		PersonalDetail personal = contact.getPersonalDetail();
		BusinessDetail business = contact.getBusinessDetail();

		// parse name: sn, cn, givenName, middleName, jobTitle, suffix, nickname
		Name name = contact.getName();
		String str = LdapUtils.getPrintableProperty(name.getFirstName());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("givenName", str);
		}
		str = LdapUtils.getPrintableProperty(name.getLastName());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("sn", str);
		}
		str = LdapUtils.getPrintableProperty(name.getMiddleName());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("middleName", str);
		}
		str = LdapUtils.getPrintableProperty(name.getNickname());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("nickname", str);
		}
		str = LdapUtils.getPrintableProperty(name.getDisplayName());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("fullname", str);
		}
		str = LdapUtils.getPrintableProperty(name.getSalutation());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("title", str);
		}
		str = LdapUtils.getPrintableProperty(name.getSuffix());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("suffix", str);
		}
		str = LdapUtils.getPrintableProperty(name.getInitials());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("initials", str);
		}

		// personal data: dateOfBirth
		str = personal.getBirthday();
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("dateOfBirth", str);
		}

		Address addr = personal.getAddress();
		addAddressAttributes(addr, "home", attrs);

		addr = business.getAddress();
		addAddressAttributes(addr, "work", attrs);




		// get email
		List<Email> emails = personal.getEmails();
		for (Email e : emails) {
			str = LdapUtils.getPrintableProperty(e);
			if (logger.isTraceEnabled()) {
				logger.trace( 
						String.format("type:%s;prop:%s", e.getPropertyType(), e.getPropertyValueAsString())
				);
			}
			String mappedAttribute = attributeMap.get(e.getPropertyType());
			if (StringUtils.isNotEmpty(str) && mappedAttribute != null ) {
				attrs.put(mappedAttribute, str);
			}
		}

		// get phone
		List<Phone> phones = personal.getPhones();		
		phones.addAll(business.getPhones());
		for (Phone e : phones) {
			str = LdapUtils.getPrintableProperty(e);
			if (logger.isTraceEnabled()) {
				logger.trace( 
						String.format("type:%s;prop:%s", e.getPropertyType(), e.getPhoneType())
				);
			}
			String mappedAttribute = attributeMap.get(e.getPropertyType());
			if (StringUtils.isNotEmpty(str)  && mappedAttribute != null) {
				attrs.put(mappedAttribute, str);
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("Missing phone property in map:"+ e.getPropertyType());
				}
			}
		}




		// websites
		List<WebPage> sites = business.getWebPages();
		for (WebPage s: sites) {
			if (logger.isTraceEnabled()) {
				logger.trace( 
						String.format("type:%s;prop:%s", s.getPropertyType(), s.getWebPageType())
				);
			}
			attrs.put("piWebsite1", LdapUtils.getPrintableProperty(s));
			break; // get just first site
		}
		sites = personal.getWebPages();
		for (WebPage s: sites) {
			if (logger.isTraceEnabled()) {
				logger.trace( 
						String.format("type:%s;prop:%s", s.getPropertyType(), s.getWebPageType())
				);
			}
			attrs.put("piWebsite2", LdapUtils.getPrintableProperty(s));		
			break; // get just first site
		}



		// get other values
		str = contact.getFreeBusy();
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("inetFreeBusy", str);
		}		
		str = LdapUtils.getPrintableProperty(business.getCompany());
		if (StringUtils.isNotEmpty(str)) {
			attrs.put("company", str);
		}


		List<Title> titles = business.getTitles();
		if (titles != null) {
			for (Title t: titles) {
				attrs.put("jobTitle", LdapUtils.getPrintableProperty(t));
			}
		}

		List<Note> notes = contact.getNotes();
		if (notes != null) {
			for (Note n : notes) {
				attrs.put("notes", LdapUtils.getPrintableProperty((Note) n));
			}
		}
		return attrs;
	}
	
	/**
	 * Return an ldapFilter retrieving twin items of the given one
	 * @param c
	 * @return
	 */
	public String getTwinsFilter(Contact c) {
		String filter = null;
		
        String firstName =
            c.getName().getFirstName().getPropertyValueAsString();
        String lastName =
            c.getName().getLastName().getPropertyValueAsString();
        String companyName = null;
        if (c.getBusinessDetail().getCompany() != null) {
            companyName = c.getBusinessDetail()
                           .getCompany().getPropertyValueAsString();
        }

		if (StringUtils.isNotEmpty(firstName)) {
			filter = String.format("(|(%s=%s)(%s=%s))","givenname",firstName,"sn", firstName);			
		}
		if (StringUtils.isNotEmpty(lastName)) {
			filter += String.format("(|(%s=%s)(%s=%s))","givenname",lastName,"sn", lastName);			
		}

		return "(&"+filter+")";
	}

	public String getSoftDeleteAttribute() {
		return "deleted";
	}

	@Override
	public String[] getSupportedAttributes() {
		return SUPPORTED_ATTRIBUTES;
	}

	//
	// helper methods
	//
	private String reverseAttributeMap(String v) {
		if (v!=null) {
			for (String k : attributeMap.keySet()) {
				if (v.equals(attributeMap.get(k)))
					return k;
			}
		}
		return null;
	}
	
	@Override
	public String getTwinItems(SyncItem si) {		
		return getTwinItemsByAttribute(syncItemToLdapAttributes(si));
	}
	
	/**
	 * Testable method for retrieving twin items from attribute
	 * @param attrs
	 * @return
	 */
	protected String getTwinItemsByAttribute(Attributes attrs) {
		/*
		 * twinning conditions
		 *  -  if no name, skip
		 */
		String firstName=null, lastName=null, email=null, mobile=null, companyName=null;		
		boolean hasNeededFields = false, hasOnlyCompany = false;
		
		String val = LdapUtils.getPrintableAttribute(attrs.get(FIRSTNAME));
		if (StringUtils.isNotEmpty(val)) {
			firstName =  val;
		}
		val = LdapUtils.getPrintableAttribute(attrs.get(LASTNAME));
		if (StringUtils.isNotEmpty(val)) {
			lastName =  val;
		}
		val = LdapUtils.getPrintableAttribute(attrs.get("company"));
		if (StringUtils.isNotEmpty(val)) {
			companyName =  val;
		}
		val = LdapUtils.getPrintableAttribute(attrs.get(FIRSTNAME));
		if (StringUtils.isNotEmpty(val)) {
			firstName =  val;
		}
		for (String a:	new String[] { attributeMap.get(EMAIL1ADDRESS), attributeMap.get(EMAIL2ADDRESS), attributeMap.get(EMAIL3ADDRESS)}) {
			val = LdapUtils.getPrintableAttribute(attrs.get(a));
			if (StringUtils.isNotEmpty(val)) {
				email = val;
				break;
			}
		}
		for (String a:	new String[] { attributeMap.get(PHONEMOBILE), attributeMap.get(PHONEMOBILEHOME), attributeMap.get(PHONEPRIMARY)}) {
			val = LdapUtils.getPrintableAttribute(attrs.get(a));
			if (StringUtils.isNotEmpty(val)) {
				mobile = val;
				break;
			}
		}
				
		// no fields to match, return
		if (firstName==null && lastName==null && email==null && mobile==null) {

			if (companyName == null) {
	            if (logger.isTraceEnabled()) {
	                logger.trace("Item with no email addresses, company name, first "
	                          + "and last names: twin search skipped.");
	            }
	
				return null;
			} else {
				hasOnlyCompany = true;
			}
		}
		
		/// if home address and email work address, look into empty contacts
        //
        // Checks email home address and email work address
        //
		String filter = null;
		if (hasOnlyCompany) {
			// try to match an un-named contact using phones
			// TODO write-me
		} else {
			// complete matching by first name, last name or nickname
			String nameMatches = String.format("(&(%s=%s)(%s=%s))",
					FIRSTNAME,firstName,LASTNAME, lastName);
			String emailMatches = String.format("(|" +
					"(%s=%s)(%s=%s)(%s=%s)" +
					"(%s=%s)(%s=%s)(%s=%s)" +
					")", attributeMap.get(EMAIL1ADDRESS),email,attributeMap.get(EMAIL2ADDRESS),email, attributeMap.get(EMAIL3ADDRESS),email,
					attributeMap.get(PHONEMOBILE),mobile,attributeMap.get(PHONEMOBILEHOME),mobile,attributeMap.get(PHONEPRIMARY),mobile
			);
			
			filter = String.format("(&%s%s)", nameMatches,emailMatches);
									
			if (logger.isTraceEnabled()) {
				logger.trace("Twinning items matching "+filter);
			}
		}
		return filter;
	}
	public Attributes mergeAttributes(Attributes a0, Attributes a1) {
        logger.trace("PiTypeContactDAO mergeAttributes begin");

		// TODO Auto-generated method stub
		return null;
	}
}
