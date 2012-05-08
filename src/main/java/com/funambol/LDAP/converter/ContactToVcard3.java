/**
 * The vcard-3.0 parser/converter (Stub)
 * 
 * @author rpolli _@ babel.it
 * 
 * stub taken from ContactToVcard, waiting for protected methods :P
 */
package com.funambol.LDAP.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.funambol.common.pim.common.Property;
import com.funambol.common.pim.common.XTag;
import com.funambol.common.pim.contact.Address;
import com.funambol.common.pim.contact.BusinessDetail;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Email;
import com.funambol.common.pim.contact.Name;
import com.funambol.common.pim.contact.Note;
import com.funambol.common.pim.contact.PersonalDetail;
import com.funambol.common.pim.contact.Phone;
import com.funambol.common.pim.contact.SIFC;
import com.funambol.common.pim.contact.Title;
import com.funambol.common.pim.contact.WebPage;
import com.funambol.common.pim.converter.ContactToVcard;
import com.funambol.common.pim.converter.ConverterException;
import com.funambol.common.pim.utility.TimeUtils;

public class ContactToVcard3 extends ContactToVcard {
    private String newLine = "\r\n"; // default
        private static String TELEPHONE_NUMBER = "TelephoneNumber";
        private static String FAX_NUMBER       = "FaxNumber";

    
    private static String[][] PHONE_TYPE = {

            // Ordering counts! Longest ones go first in every group starting with

            // the same substring.

            { "MobileBusiness", TELEPHONE_NUMBER, ";TYPE=CELL,WORK"           },
            { "MobileHome",     TELEPHONE_NUMBER, ";TYPE=CELL,HOME"           },
            { "MobileDC",       TELEPHONE_NUMBER, ";TYPE=CELL;X-DC"           },
            { "Mobile",         TELEPHONE_NUMBER, ";TYPE=CELL"                },
            { "Business",       TELEPHONE_NUMBER, ";TYPE=WORK,VOICE"          },
            { "Home",           TELEPHONE_NUMBER, ";TYPE=HOME,VOICE"          },
            { "OtherCustom",    TELEPHONE_NUMBER, ";X-OTHER"             },
            { "Other",          TELEPHONE_NUMBER, ";TYPE=VOICE"               },
            { "Business",       FAX_NUMBER,       ";TYPE=FAX,WORK"            },
            { "Home",           FAX_NUMBER,       ";TYPE=FAX,HOME"            },
            { "Other",          FAX_NUMBER,       ";TYPE=FAX"                 },
            { "PagerNumber",    "",               ";TYPE=PAGER"               },
            { "Car",            TELEPHONE_NUMBER, ";TYPE=CAR,VOICE"           },
            { "CompanyMain",    TELEPHONE_NUMBER, ";TYPE=WORK,PREF"           },
            { "Primary",        TELEPHONE_NUMBER, ";TYPE=PREF,VOICE"          },
            { "Callback",       TELEPHONE_NUMBER, ";X-FUNAMBOL-CALLBACK" },
            { "Radio",          TELEPHONE_NUMBER, ";X-FUNAMBOL-RADIO"    },
            { "Telex",          "Number",         ";X-FUNAMBOL-TELEX"    },
            { "DCOnly",         TELEPHONE_NUMBER, ";X-DC"                },
            { "Assistant",      TELEPHONE_NUMBER, null                   }

        };

    

	public ContactToVcard3(TimeZone timezone, String charset) {
		super(timezone, charset);
	}
	
    // ---------------------------------------------------------- Public methods

    /**
     * Performs the conversion.
     *
     * @param obj the Contact to be converted in vCard format
     * @return a string containing the vCard representation of this Contact
     * @throws ConverterException
     */
    public String convert (Object obj) throws ConverterException {

        Contact contact = (Contact)obj;
        StringBuffer output = new StringBuffer("BEGIN:VCARD" + newLine 
                + "VERSION:3.0" + newLine);

        if (contact.getName()!=null) {
            output.append(composeFieldName(contact.getName()));
            output.append(composeFieldFormalName(contact.getName().getDisplayName()));
            output.append(composeFieldNickname(contact.getName().getNickname()));
        }
        PersonalDetail personalDetail = contact.getPersonalDetail();
        if (personalDetail!= null) {
            output.append(composeFieldAddress(personalDetail.getAddress(), "HOME"));
            output.append(composeFieldAddress(personalDetail.getOtherAddress(), "OTHER"));
            output.append(composeFieldBirthday(personalDetail.getBirthday()));
            output.append(composeFieldOtherLabel(personalDetail.getOtherAddress().getLabel()));
            output.append(composeFieldPersonalLabel(personalDetail.getAddress().getLabel()));
            output.append(composeFieldTelephone(personalDetail.getPhones()));
            output.append(composeFieldEmail(personalDetail.getEmails()));
            output.append(composeFieldWebPage(personalDetail.getWebPages()));
            output.append(composeFieldAnniversary(personalDetail.getAnniversary()));
            output.append(composeFieldChildren(personalDetail.getChildren()));
            output.append(composeFieldSpouse(personalDetail.getSpouse()));
    //        output.append(composeFieldGeo(personalDetail.getGeo()));
        }
        BusinessDetail businessDetail = contact.getBusinessDetail();
        if (businessDetail!= null) {
            output.append(composeFieldAddress(businessDetail.getAddress(), "WORK"));
            output.append(composeFieldRole(businessDetail.getRole()));
            output.append(composeFieldTitle(businessDetail.getTitles()));
            output.append(composeFieldOrg(businessDetail.getCompany(),
                                          businessDetail.getDepartment(),
                                          businessDetail.getOfficeLocation()));
            output.append(composeFieldBusinessLabel(businessDetail.getAddress().getLabel()));
            output.append(composeFieldTelephone(businessDetail.getPhones()));
            output.append(composeFieldEmail(businessDetail.getEmails()));
            output.append(composeFieldWebPage(businessDetail.getWebPages()));
            output.append(composeFieldCompanies(businessDetail.getCompanies()));
            output.append(composeFieldManager(businessDetail.getManager()));
//            output.append(composeFieldAssistant(businessDetail.getAssistant(),
//                                                businessDetail.getPhones(),
//                                                businessDetail.isAssistantURI()));
        }
        output.append(composeFieldNote(contact.getNotes()));
        output.append(composeFieldXTag(contact.getXTags()));
        output.append(composeFieldRevision(contact.getRevision()));
        output.append(composeFieldCategories(contact.getCategories()));
        output.append(composeFieldPhoto(contact.getPersonalDetail().getPhoto()));
        output.append(composeFieldUid(contact.getUid()));
        output.append(composeFieldFolder(contact.getFolder()));
        output.append(composeFieldFreeBusy(contact.getFreeBusy()));
        output.append(composeFieldLanguages(contact.getLanguages()));
        output.append(composeFieldMileage(contact.getMileage()));
        output.append(composeFieldSubject(contact.getSubject()));
        output.append(composeFieldTimezone(contact.getTimezone()));
//        output.append(composeFieldAccessClass(contact.getSensitivity()));
//        output.append(composeFieldMailer(contact.getMailer()));
//        output.append(composeFieldIMPPs(contact.getIMPPs()));
//        output.append(composeFieldProductID(contact.getProductID()));
//        output.append(composeFieldKey(contact.getKey()));
        output.append("END:VCARD").append(newLine);
        return output.toString();
    }
    
    // --------------------------------------------------------- Private methods

    /**
     * @return a representation of the v-card field N:
     */
    private StringBuffer composeFieldName(Name name) throws ConverterException {
        if (name.getLastName().getPropertyValue()   == null &&
            name.getFirstName().getPropertyValue()  == null &&
            name.getMiddleName().getPropertyValue() == null &&
            name.getSalutation().getPropertyValue() == null &&
            name.getSuffix().getPropertyValue()     == null   ) {
            return new StringBuffer(0);
        }

        StringBuffer output  = new StringBuffer(120); // Estimate 120 as needed
        ArrayList<Property> properties = new ArrayList<Property>();

        if (name.getLastName().getPropertyValue() != null) {
            output.append(
                escapeSeparator((String)name.getLastName().getPropertyValue())
            );
            properties.add(name.getLastName());
        }
        output.append(';');
        if (name.getFirstName().getPropertyValue() != null) {
            output.append(
                escapeSeparator((String)name.getFirstName().getPropertyValue())
            );
            properties.add(name.getFirstName());
        }
        output.append(';');
        if (name.getMiddleName().getPropertyValue() != null) {
            output.append(
                escapeSeparator((String)name.getMiddleName().getPropertyValue())
            );
            properties.add(name.getMiddleName());
        }
        output.append(';');
        if (name.getSalutation().getPropertyValue() != null) {
            output.append(
                escapeSeparator((String)name.getSalutation().getPropertyValue())
            );
            properties.add(name.getSalutation());
        }
        output.append(';');
        if (name.getSuffix().getPropertyValue() != null) {
            output.append(
                escapeSeparator((String)name.getSuffix().getPropertyValue())
            );
            properties.add(name.getSuffix());
        }

        return composeVCardComponent(output.toString(), properties, "N");
    }

    /**
     * @return a representation of the v-card field FN:
     */
    private StringBuffer composeFieldFormalName(Property displayName)
    throws ConverterException {

        return composeSimpleEscapedPropertyField("FN", displayName);
    }

    /**
     * @return a representation of the v-card field NICKNAME:
     */
    private StringBuffer composeFieldNickname(Property nickname)
    throws ConverterException {

        return composeSimpleEscapedPropertyField("NICKNAME", nickname);
    }

    /**
     * @return a representation of the v-card field ADR, ADR;HOME, ADR;WORK
     * if type = HOME  then set ADR;HOME
     * if type = OTHER then set ADR
     * if type = WORK  then set ADR;WORK
     */
    private StringBuffer composeFieldAddress(Address address, String type)
    throws ConverterException {
        if ((address == null) ||
            (address.getPostOfficeAddress().getPropertyValue() == null &&
             address.getRoomNumber().getPropertyValue()        == null &&
             address.getStreet().getPropertyValue()            == null &&
             address.getCity().getPropertyValue()              == null &&
             address.getState().getPropertyValue()             == null &&
             address.getPostalCode().getPropertyValue()        == null &&
             address.getCountry().getPropertyValue()           == null &&
             address.getExtendedAddress().getPropertyValue()   == null   ))
        {
            return new StringBuffer(0);
        }

        StringBuffer output  = new StringBuffer();
        ArrayList<Property> properties = new ArrayList<Property>();

        if (address.getPostOfficeAddress().getPropertyValue() != null) {
            output.append(escapeSeparator(
                (String)address.getPostOfficeAddress().getPropertyValue())
            );
            properties.add(address.getPostOfficeAddress());
        }
        output.append(';');
        if (address.getExtendedAddress().getPropertyValue() != null) {
            output.append(escapeSeparator(
                (String)address.getExtendedAddress().getPropertyValue())
            );
            properties.add(address.getExtendedAddress());
        }
        output.append(';');
        if (address.getStreet().getPropertyValue() != null) {
            output.append(
                escapeSeparator((String)address.getStreet().getPropertyValue())
            );
            properties.add(address.getStreet());
        }
        output.append(';');
        if (address.getCity().getPropertyValue() != null) {
            output.append(
                escapeSeparator((String)address.getCity().getPropertyValue())
            );
            properties.add(address.getCity());
        }
        output.append(';');
        if (address.getState().getPropertyValue() != null) {
            output.append(
                escapeSeparator((String)address.getState().getPropertyValue())
            );
            properties.add(address.getState());
        }
        output.append(';');
        if (address.getPostalCode().getPropertyValue() != null) {
            output.append(escapeSeparator(
                (String)address.getPostalCode().getPropertyValue())
            );
            properties.add(address.getPostalCode());
        }
        output.append(';');
        if (address.getCountry().getPropertyValue() != null) {
            output.append(
                escapeSeparator((String)address.getCountry().getPropertyValue())
            );
            properties.add(address.getCountry());
        }

        if (("HOME").equals(type)) {
            return composeVCardComponent(output.toString(),
                                         properties       ,
                                         "ADR;TYPE=HOME"       );
        } else if (("OTHER").equals(type)) {
            return composeVCardComponent(output.toString(),
                                         properties       ,
                                         "ADR"            );
        } else if (("WORK").equals(type)) {
            return composeVCardComponent(output.toString(),
                                         properties       ,
                                         "ADR;TYPE=WORK"       );
        }

        return new StringBuffer(0);
    }

    /**
     * @return a representation of the v-card field PHOTO:
     */
    private StringBuffer composeFieldPhoto(Property photo) throws ConverterException {
        if (photo.getPropertyValue() != null) {

            ArrayList<Property> properties = new ArrayList<Property>();
            properties.add(photo);

            //
            // The charset must be null (not set) since:
            // 1. it is useless since the content is in base64
            // 2. on some Nokia phone it doesn't work since for some reason the phone
            //    adds a new photo and the result is that a contact has two photos
            //    Examples of wrong phones: Nokia N91, 7610, 6630
            //
            return composeVCardComponent(photo.getPropertyValueAsString(),
                                         properties,
                                         "PHOTO"   ,
                                         true);   // true means "exclude the charset"
        }
        return new StringBuffer(0);
    }

    /**
     * @return a representation of the v-card field BDAY:
     */
    private String composeFieldBirthday(String birthday) {

        if (birthday == null) {
            return "";
        }

        try {
            return ("BDAY:" + TimeUtils.normalizeToISO8601(birthday, timezone)
                    + newLine);
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * @return a representation of the v-card field TEL:
     */
    private String composeFieldTelephone(List phones)
    throws ConverterException {

        if ((phones == null) || phones.isEmpty()) {
            return "";
        }

        StringBuffer output  = new StringBuffer();
        ArrayList<Property> properties = new ArrayList<Property>();

        Phone telephone = null;
        String phoneType   = null;

        int size = phones.size();
        for (int i=0; i<size; i++) {

            telephone = (Phone)phones.get(i);
            phoneType = composePhoneType(telephone.getPhoneType());
            if (phoneType == null) { // Skips it
                continue;
            } // but if it's *empty* instead of null, it's OK

            properties.clear();
            properties.add(0, telephone);

            output.append(
                composeVCardComponent(escapeSeparator((String)telephone.getPropertyValue()),
                                      properties       ,
                                      "TEL" + phoneType)
            );
        }

        return output.toString();
    }

    /**
     * @return the v-card representation of a telephone type
     */
    private String composePhoneType(String type) {
        if (type == null) {
            return "";
        }

        for (String[] phoneTypeMapping : PHONE_TYPE) {
            if (type.startsWith(phoneTypeMapping[0]) && // SIF-C prefix
                type.endsWith  (phoneTypeMapping[1])) { // SIF-C suffix
                return phoneTypeMapping[2]; // vCard attribute(s)
            }
        }
        return "";
    }

    /**
     * @return a representation of the v-card field EMAIL:
     */
    private String composeFieldEmail(List emails) throws ConverterException {

        if ((emails == null) || emails.isEmpty()) {
            return "";
        }

        StringBuffer output  = new StringBuffer();
        ArrayList<Property> properties = new ArrayList<Property>();

        Email email = null;
        String emailType = null;

        int size = emails.size();
        for (int i=0; i<size; i++) {

            email = (Email)emails.get(i);
            emailType   = composeEmailType(email.getEmailType());

            properties.clear();
            properties.add(0, email);

            output.append(
                composeVCardComponent(escapeSeparator((String)email.getPropertyValue()),
                                      properties         ,
                                      "EMAIL" + emailType)
            );
        }

        return output.toString();
    }

    /**
     * @return the v-card representation of a email type
     */
    private String composeEmailType(String type) {
        if (type == null) {
            return "";
        }

        if (("Email1Address").equals(type)) {
            return ";TYPE=INTERNET";
        } else if (("Email2Address").equals(type)) {
            return ";TYPE=INTERNET,HOME";
        } else if (("Email3Address").equals(type)) {
            return ";TYPE=INTERNET,WORK";
        }

        for (int j=2; j<=10; j++) {
            if (("Other" + j + "EmailAddress").equals(type)) {
                return ";TYPE=INTERNET";
            } else if (("HomeEmail" + j +"Address").equals(type)) {
                return ";TYPE=INTERNET,HOME";
            } else if (("BusinessEmail" + j +"Address").equals(type)) {
                return ";TYPE=INTERNET,WORK";
            }
        }
        
        if (("IMAddress").equals(type)) {
            return ";TYPE=INTERNET,HOME,X-FUNAMBOL-INSTANTMESSENGER";
        }

        if (("MobileEmailAddress").equals(type)) {
            return ";TYPE=X-CELL";
        }

        return "";
    }

    private String composeFieldWebPage(List webpages)
    throws ConverterException {

        if ((webpages == null) || webpages.isEmpty()) {
            return "";
        }

        StringBuffer output  = new StringBuffer();
        ArrayList<Property> properties = new ArrayList<Property>();

        WebPage address   = null;
        String webpageType = null;

        int size = webpages.size();
        for (int i=0; i<size; i++) {

            address = (WebPage)webpages.get(i);
            webpageType = composeWebPageType(address.getWebPageType());

            properties.add(0, address);

            output.append(
                composeVCardComponent(escapeSeparator((String)address.getPropertyValue()),
                                      properties ,
                                      webpageType)
            );
        }

        return output.toString();
    }

   /**
    * @return the v-card representation of a web page type
    */
    private String composeWebPageType(String type) {
        if (type == null) {
            return "";
        } else if (("WebPage").equals(type)) {
            return "URL";
        } else if (("HomeWebPage").equals(type)) {
            return "URL;HOME";
        } else if (("BusinessWebPage").equals(type)) {
            return "URL;WORK";
        }

        for (int j=2; j<=10; j++) {
            if (("WebPage" + j).equals(type)) {
                return "URL";
            } else if (("Home" + j +"WebPage").equals(type)) {
                return "URL;HOME";
            } else if (("Business" + j +"WebPage").equals(type)) {
                return "URL;WORK";
            }
        }

        return "";
   }
    
    private String composeFieldFreeBusy(String freeBusy) throws ConverterException {
        if (freeBusy != null && freeBusy.length() > 0 ) {
            ArrayList<Property> properties = new ArrayList<Property>();
            StringBuffer output =
                    composeVCardComponent(freeBusy, properties, "FBURL");
            return output.toString();
        }
        return "";
    }

    /**
     * @return a representation of the v-card field LABEL;HOME:
     */
    private StringBuffer composeFieldPersonalLabel(Property label)
    throws ConverterException {

        return composeSimpleEscapedPropertyField("LABEL;TYPE=HOME", label);
    }

    /**
     * @return a representation of the v-card field LABEL;OTHER:
     */
    private StringBuffer composeFieldOtherLabel(Property label)
    throws ConverterException {

        return composeSimpleEscapedPropertyField("LABEL;TYPE=OTHER", label);
    }

    /**
     * @return a representation of the v-card field LABEL;WORK:
     */
     private StringBuffer composeFieldBusinessLabel(Property label)
    throws ConverterException {

        return composeSimpleEscapedPropertyField("LABEL;TYPE=WORK", label);
    }

    /**
     * @return a representation of the v-card field ROLE:
     */
    private StringBuffer composeFieldRole(Property role) throws ConverterException {

        return composeSimpleEscapedPropertyField("ROLE", role);
    }

    /**
     * @return a representation of the v-card field TITLE:
     */
    private String composeFieldTitle(List titles) throws ConverterException {
        if ((titles == null) || titles.isEmpty()) {
            return "";
        }

        StringBuffer output  = new StringBuffer();
        ArrayList<Property> properties = new ArrayList<Property>();

        Title title = null;

        int size = titles.size();
        for (int i=0; i<size; i++) {

            title = (Title)titles.get(i);
            properties.add(0, title);

            output.append(
                composeVCardComponent(escapeSeparator((String)title.getPropertyValue()),
                                       properties,
                                       "TITLE"   )
            );
        }

        return output.toString();
    }

    /**
     * @return a representation of the v-card field ORG:
     */
    private StringBuffer composeFieldOrg(Property company, 
                                         Property department,
                                         String office)
    throws ConverterException {
        if (company.getPropertyValue()    == null &&
            department.getPropertyValue() == null &&
            office                        == null) {
            return new StringBuffer(0);
        }

        StringBuffer output  = new StringBuffer();
        ArrayList<Property> properties = new ArrayList<Property>();

        if (company.getPropertyValue() != null) {
            output.append(escapeSeparator((String)company.getPropertyValue()));
            properties.add(company);
        }
        output.append(';');
        if (department.getPropertyValue() != null) {
            output.append(escapeSeparator((String)department.getPropertyValue()));
            properties.add(department);
        }

        if (office != null) {
            output.append(';');
            output.append(escapeSeparator(office));
        }
        
        return composeVCardComponent(output.toString(),
                                     properties       ,
                                     "ORG"            );
    }

    /**
     * @return a representation of the v-card field XTag:
     */
    private String composeFieldXTag(List xTags)
    throws ConverterException {
        if ((xTags == null) || xTags.isEmpty()) {
            return "";
        }

        StringBuffer output  = new StringBuffer();
        ArrayList<Property> properties = new ArrayList<Property>();

        Property xtag = null;

        int size = xTags.size();
        for (int i=0; i<size; i++) {

            XTag xtagObj = (XTag)xTags.get(i);

            xtag  = xtagObj.getXTag();

            properties.clear();
            properties.add(0, xtag);

            String propertyValue = (String) xtag.getPropertyValue();
         //   if (!xtagObj.isStructured()) {
                propertyValue = escapeSeparator(propertyValue);
         //   }

            output.append(
                composeVCardComponent(propertyValue         ,
                                      properties            ,
                                      xtagObj.getXTagValue())
            );
        }
        return output.toString();
    }

    /**
     * @return a representation of the v-card field NOTE:
     */
    private String composeFieldNote(List notes) throws ConverterException {

        if ((notes == null) || notes.isEmpty()) {
            return "";
        }

        StringBuffer output = new StringBuffer();
        ArrayList<Property> properties = new ArrayList<Property>();

        Note note = null;

        int size = notes.size();
        for (int i=0; i<size; i++) {

            note = (Note)notes.get(i);
            properties.add(0, note);

            output.append(
                composeVCardComponent(escapeSeparator((String)note.getPropertyValue()),
                                       properties,
                                       "NOTE"    )
            );
        }

        return output.toString();
    }

    /**
     * @param uid the UID that might include a type expressed like "{TYPE=type}"
     * @return a representation of the v-card field UID, including the TYPE
     *         attribute if present
     */
    private String composeFieldUid(String uid) {
        if (uid != null) {
            Pattern pattern = Pattern.compile("\\{(TYPE=[A-Z,a-z,0-9]+)\\}");
            Matcher matcher = pattern.matcher(uid);
            StringBuffer sb = new StringBuffer("UID");
            if (matcher.find()) {
                sb.append(';');
                matcher.appendReplacement(sb, matcher.group(1));
                sb.append(':');
                matcher.appendTail(sb);
            } else {
                sb.append(':').append(uid);
            }
            return sb.toString() + newLine;
        }
        return "";
    }

    /**
     * @return a representation of the v-card field TZ:
     */
    private String composeFieldTimezone(String tz) {
        return composeSimpleStringField("TZ:", tz);
    }

    /**
     * @return a representation of the v-card field PRODID
     */
    private String composeFieldProductID(String productID) {
        return composeSimpleEscapedStringField("PRODID:", productID);
    }

    /**
     * @return a representation of the v-card field KEY
     */
    private StringBuffer composeFieldKey(Property key) throws ConverterException {

        return composeSimpleEscapedPropertyField("KEY", key);
    }

    /**
     * @return a representation of the v-card field REV:
     */
    private String composeFieldRevision(String revision) {
        return composeSimpleStringField("REV:", revision);
    }

    /**
     * @return a representation of the v-card field CATEGORIES:
     */
    private StringBuffer composeFieldCategories(Property categories)
    throws ConverterException {

        return composeSimpleEscapedPropertyField("CATEGORIES", categories);
    }

    /**
     * @return a representation of the v-card field CLASS:
     */
/*    private String composeFieldAccessClass(Short sensitivity) {

        if (sensitivity == null) {
            return "";
        }

        String accessClass;
        if (Contact.SENSITIVITY_NORMAL.equals(sensitivity)) {
                accessClass = Contact.CLASS_PUBLIC;
        } else if (Contact.SENSITIVITY_PRIVATE.equals(sensitivity)) {
                accessClass = Contact.CLASS_PRIVATE;
        } else if (Contact.SENSITIVITY_CONFIDENTIAL.equals(sensitivity)) {
                accessClass = Contact.CLASS_CONFIDENTIAL;
        } else {
                accessClass = Contact.CLASS_CUSTOM;
        }
        return "CLASS:" + accessClass + newLine;
    }
*/
    /**
     * @return a representation of the vCard field GEO:
     */
    private StringBuffer composeFieldGeo(Property geo)
    throws ConverterException {

        return composeSimplePropertyField("GEO", geo);
    }

    /**
     * @return a representation of the vCard field MAILER
     */
    private String composeFieldMailer(String mailer) {
        return composeSimpleEscapedStringField("MAILER:", mailer);
    }
    
    /**
     * @param impps a list of IM/PP addresses
     * @return a representation of the vCard field MAILER
     */
 /*   private String composeFieldIMPPs(List<IMPPAddress> impps) {

        StringBuffer sb = new StringBuffer();
        for (IMPPAddress impp : impps) {
            sb.append("IMPP");
            boolean start = true;
            for (String type : impp.getVCardTypeList()) {
                if (start) {
                    sb.append(";TYPE=");
                    start = false;
                } else {
                    sb.append(',');
                }
                sb.append(type);
            }

            // URI
            sb.append(':').append(escapeSeparator(impp.getUri())).append(newLine);
        }
        return sb.toString(); 
    }
*/
    /**
     * @return a representation of the vCard field X-FUNAMBOL-FOLDER
     */
    private String composeFieldFolder(String folder) {
        return composeSimpleEscapedStringField("X-FUNAMBOL-FOLDER:", folder);
    }
    
    /**
     * Returns a custom vCard representation of the Anniversary property.
     * 
     * @return a representation of the vCard field X-ANNIVERSARY
     */
    private String composeFieldAnniversary(String anniversary) {
        return composeSimpleEscapedStringField("X-ANNIVERSARY:", anniversary);
    }


    private String composeFieldAssistant(String assistant,
                                         List<Phone> workPhones,
                                         boolean isURI) {
        if (assistant != null) {
            if (isURI) {
                return "AGENT;VALUE=uri:" + escapeSeparator(assistant) + newLine;
            } else {
                String phoneNumber = "";
                for (Phone phone : workPhones) {
                    if (SIFC.ASSISTANT_TELEPHONE_NUMBER.equals(phone.getPhoneType())) {
                        phoneNumber = escapeSeparator(phone.getPropertyValueAsString());
                        break;
                    }
                }
                return "AGENT:BEGIN:VCARD\\n" +
                             "FN:" + escapeSeparator(assistant) + "\\n" +
                             "TEL;WORK:" + phoneNumber + "\\n" +
                             "END:VCARD" + newLine;

            }
        }
        return "";
    }
    /**
     * Returns a custom vCard representation of the Children property.
     * 
     * @return a representation of the vCard field X-CHILDREN
     */
    private String composeFieldChildren(String children) {
        return composeSimpleStringField("X-FUNAMBOL-CHILDREN:", children);
                       // because there could be many semicolon-separated values
    }

    /**
     * Returns a custom vCard representation of the Companies property.
     * 
     * @return a representation of the vCard field X-FUNAMBOL-COMPANIES
     */
    private String composeFieldCompanies(String companies) {
        return composeSimpleStringField("X-FUNAMBOL-COMPANIES:", companies);
                       // because there could be many semicolon-separated values
    }
    
    /**
     * Returns a custom vCard representation of the Language property.
     * 
     * @return a representation of the vCard field X-FUNAMBOL-LANGUAGES
     */
    private String composeFieldLanguages(String languages) {
        return composeSimpleStringField("X-FUNAMBOL-LANGUAGES:", languages);
                       // because there could be many semicolon-separated values
    }
    
    /**
     * Returns a custom vCard representation of the ManagerName property.
     * 
     * @return a representation of the vCard field X-MANAGER
     */
    private String composeFieldManager(String manager) {
        return composeSimpleEscapedStringField("X-MANAGER:", manager);
    }
    
    /**
     * Returns a custom vCard representation of the Mileage property.
     * 
     * @return a representation of the vCard field X-FUNAMBOL-MILEAGE
     */
    private String composeFieldMileage(String mileage) {
        return composeSimpleEscapedStringField("X-FUNAMBOL-MILEAGE:", mileage);
    }
    
    /**
     * Returns a custom vCard representation of the Spouse property.
     * 
     * @return a representation of the vCard field X-SPOUSE
     */
    private String composeFieldSpouse(String spouse) {
        return composeSimpleEscapedStringField("X-SPOUSE:", spouse);
    }
    
    /**
     * Returns a custom vCard representation of the Subject property.
     * 
     * @return a representation of the vCard field X-FUNAMBOL-SUBJECT
     */
    private String composeFieldSubject(String subject) {
        return composeSimpleEscapedStringField("X-FUNAMBOL-SUBJECT:", subject);
    }

    /**
     * Returns a representation of a simple vCard field containing a string.
     * Some characters in the string will be escaped if necessary.
     *
     * @param fieldName the vCard field name, optionally followed by parameters,
     *                  mandatorily followed by a colon
     * @param value the property value (will be escaped)
     * @return the whole vCard line including the new-line character
     */
    private String composeSimpleEscapedStringField(String fieldName, String value) {
        if (value != null) {
            return fieldName + escapeSeparator(value) + newLine;
        }
        return "";
    }

    /**
     * Returns a representation of a simple vCard field containing a string.
     * The string will not be escaped because it may contain delimiters.
     *
     * @param fieldName the vCard field name, optionally followed by parameters,
     *                  mandatorily followed by a colon
     * @param value the property value (will not be escaped)
     * @return the whole vCard line including the new-line character
     */
    private String composeSimpleStringField(String fieldName, String value) {
        if (value != null) {
            return fieldName + value + newLine;
        }
        return "";
    }

    /**
     * Returns a representation of a simple vCard field based on a Property
     * object. Some characters in the content will be escaped if necessary.
     *
     * @param fieldName the vCard field name without parameters or colon
     * @param property the property
     * @return the whole vCard property including the last new-line character
     * @throws ConverterException if anything goes wrong in translating the
     *                            property (with parameters) to vCard format
     */
    private StringBuffer composeSimpleEscapedPropertyField(String fieldName,
                                                           Property property)
    throws ConverterException {

        if ((property != null) && (property.getPropertyValue() != null)) {

            ArrayList<Property> properties = new ArrayList<Property>(1);
            properties.add(property);

            return composeVCardComponent(escapeSeparator(property.getPropertyValueAsString()),
                                         properties,
                                         fieldName);
        }
        return new StringBuffer(0);
    }

    /**
     * Returns a representation of a simple vCard field based on a Property
     * object. The content will not be escaped because it may contain
     * delimiters.
     *
     * @param fieldName the vCard field name without parameters or colon
     * @param property the property
     * @return the whole vCard property including the last new-line character
     * @throws ConverterException if anything goes wrong in translating the
     *                            property (with parameters) to vCard format
     */
    private StringBuffer composeSimplePropertyField(String fieldName,
                                                           Property property)
    throws ConverterException {

        if (property.getPropertyValue() != null) {

            ArrayList<Property> properties = new ArrayList<Property>(1);
            properties.add(property);

            return composeVCardComponent(property.getPropertyValueAsString(),
                                         properties,
                                         fieldName);
        }
        return new StringBuffer(0);
    }

}
