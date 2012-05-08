package com.funambol.LDAP.dao.impl;


import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;

import com.funambol.LDAP.BaseTestCase;
import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.utils.LdapUtils;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Phone;
import com.funambol.common.pim.converter.ContactToVcard;
import com.funambol.common.pim.converter.ConverterException;
import com.funambol.common.pim.vcard.ParseException;
import com.funambol.common.pim.vcard.VcardParser;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemImpl;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.SyncItemState;

public class ContactDAOTest extends BaseTestCase {

	ContactToVcard vcardConverter = new ContactToVcard(null, ContactToVcard.CHARSET_UTF8);

	ContactDAO cdao = new ContactDAO();

	public static final String USER_FULLNAME = "Roberto Utenteditest Polli"; 

	public void testCreateContact() {
		// convert an ldap entry made of Attributes to a vcard
		ContactDAOInterface cdao = new ContactDAO();
		try {
			Attributes attrs= getMockEntry();
			attrs.put(cdao.getRdnAttribute(), "1111");

			Contact contact = cdao.createContact(attrs);
			assertNotNull(contact);

			logger.debug(vcardConverter.convert(contact));

		} catch (ConverterException e) {
			fail("bad item"+ e.getMessage());
		}

	}

	public void testGetter() {
		logger.info("ContactDAO has no softDelete attribute");
		ContactDAOInterface cdao = new ContactDAO();

		assertNull(cdao.getSoftDeleteAttribute());
	}

	/**
	 * test conversion ldap2Contact and back
	 * @throws NamingException
	 */
	public void testLdapPIM_goAndForth_1() throws NamingException {

		Attributes attrs= getMockSimpleEntry();		
		Contact contact = cdao.createContact(attrs);
		String logmeOnError = "";
		try {
			logger.info(vcardConverter.convert(contact));

			Attributes attrs1 = cdao.createEntry(contact);
			logger.debug("Compare Attributes");
			logmeOnError = cdao.compareAttributeSets(attrs, attrs1).toString();

			// there should be no changes after roundtrip
			for (String type : new String[] {"ADD", "REPLACE", "DEL"} ) {
				logger.debug("type:"+type);
				switch (cdao.compareAttributeSets(attrs, attrs1).get(type).size() )	{
				case 0:
					continue;
				case 1:
					if (cdao.compareAttributeSets(attrs, attrs1).get(type).get(cdao.getRdnAttribute()) != null )
						continue;
				case 2:
					logger.error(logmeOnError);
					fail("bad roundtrip: " + cdao.compareAttributeSets(attrs, attrs1));
				}

			}
		} catch (ConverterException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Convert basic vcf to to Ldap Attributes, then back again to vcf
	 * this check entry rdn and timestamp
	 */
	public void testVcs2ToLdap_basic() {
		// convert a vcard entry to ldap
		String vcards[] = { 
				"vcard-1.vcf", "vcard-2.vcf",
				"vcard-3.vcf", "vcard-4.vcf", "vcard-5.vcf"};

		for (String vcf : vcards) {
			try {
				InputStream stream =  this.getClass().getClassLoader()
				.getResourceAsStream(FCTF_BASIC +  vcf);
				String c0 = getResourceAsString(FCTF_BASIC +  vcf);
				VcardParser parser = new VcardParser(stream);

				Contact pimC = parser.vCard();
				Attributes attrs = cdao.createEntry(pimC);
				Contact pimC1 =cdao.createContact(attrs);
				assertNotNull(pimC1);
				String c1  = vcardConverter.convert(pimC1);
				assertEquals(0, LdapUtils.compareMultiLine(c0, c1).size());

			} catch (ParseException e) {
				logger.error(e.getMessage());
				fail("Bad test data:" + e);
			} catch (ConverterException e) {
				e.printStackTrace();
				fail("error converting data"+e.getMessage());
			}  catch (Exception e) {
				logger.error("missing file: "+ e.getMessage());
			}
		}
	}
	@Ignore
	public void _testVcs3ToLdap_basic() {
		// convert a vcard entry to ldap
		String vcards[] = { 
		"vcard3.0-1.vcf" };

		for (String vcf : vcards) {
			try {
				InputStream stream =  this.getClass().getClassLoader()
				.getResourceAsStream(FCTF_ADVANCED   +  vcf);
				String c0 = getResourceAsString(FCTF_ADVANCED   +  vcf);
				VcardParser parser = new VcardParser(stream);

				Contact pimC = parser.vCard();
				Attributes attrs = cdao.createEntry(pimC);
				Contact pimC1 =cdao.createContact(attrs);
				assertNotNull(pimC1);
				String c1  = vcardConverter.convert(pimC1);
				assertEquals(0, LdapUtils.compareMultiLine(c0, c1).size());

			} catch (ParseException e) {
				logger.error(e.getMessage());
				fail("Bad test data:" + e);
			} catch (ConverterException e) {
				e.printStackTrace();
				fail("error converting data"+e.getMessage());
			}  catch (Exception e) {
				logger.error("missing file: "+ e.getMessage());
				fail("missing file:" + e.getMessage());
			}
		}
	}
	/**
	 * Convert basic syncitem  to to Ldap Attributes, then back again to vcf
	 */
	public void testSyncItemToLdap_basic() {
		// convert a vcard entry to ldap
		String vcards[] = { 
				"vcard-1.vcf", "vcard-2.vcf",
				"vcard-3.vcf", "vcard-4.vcf", "vcard-5.vcf"};

		Timestamp t0    = new Timestamp(System.currentTimeMillis());
		for (String vcf : vcards) {
			try {
				String c0 = getResourceAsString(FCTF_BASIC +  vcf);
				SyncItem item = new SyncItemImpl(	
						null, 
						new SyncItemKey("-1"),
						null, 
						SyncItemState.NEW, 
						c0.getBytes(),
						null,
						"text/x-vcard",
						t0
				);


				Attributes attrs = cdao.syncItemToLdapAttributes(item);
				// this test checks Timestamp and RDN are set
				assertNotNull(attrs.get(cdao.getTimestampAttribute()));
				assertTrue(StringUtils.isNotEmpty(cdao.getRdnValue(attrs)));

				Contact pimC1 =cdao.createContact(attrs);
				assertNotNull(pimC1);
				String c1  = vcardConverter.convert(pimC1);
				assertEquals(0, LdapUtils.compareMultiLine(c0, c1).size());

			} catch (ConverterException e) {
				e.printStackTrace();
				fail("error converting data"+e.getMessage());
			}  catch (Exception e) {
				logger.error("missing file: "+ e.getMessage());
			}
		}
	}


	public void createSampleContact() throws ConverterException {
		Contact contact = new Contact();

		List<Phone> phones = new ArrayList<Phone>();
		int i=0;
		for (String b : new String[] { "Home", "Business" , "Car", "Other" , ""}) {

			for (String m : new String[] { "Mobile", ""}) {
				Phone tel = new Phone();
				tel.setPhoneType( m +b +"TelephoneNumber");
				tel.setPropertyValue(""+i++);
				phones.add(tel);
			}
		}
		contact.getPersonalDetail().setPhones(phones);
		// contact.getBusinessDetail().setPhones(phones);
		logger.info(vcardConverter.convert(contact));

	}
	/**
	 * use all vcards tested in pim
	 */
	@Ignore
	public void _testVcsToLdap_advanced() {
		// convert a vcard entry to ldap
		String vcf="";

		for (int i=1; i<16; i++) {
			try {
				vcf = String.format("vcard-%d.vcf",i);
				InputStream stream =  this.getClass().getClassLoader()
				.getResourceAsStream(FCTF_ADVANCED +  vcf);
				String c0 = getResourceAsString(FCTF_ADVANCED +  vcf);
				VcardParser parser = new VcardParser(stream);

				Contact pimC = parser.vCard();
				Attributes attrs = cdao.createEntry(pimC);
				Contact pimC1 =cdao.createContact(attrs);
				assertNotNull(pimC1);
				String c1  = vcardConverter.convert(pimC1);
				LdapUtils.compareMultiLine(c0, c1).size();
				// assertEquals(0, this.compareMultiLine(c0, c1).size());
			} catch (ParseException e) {
				logger.error(e.getMessage());
				fail("Bad test data:" + e);
			} catch (ConverterException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}  catch (Exception e) {
				logger.error("missing file: "+vcf);
			}
		}
	}










	//	
	//	helper
	//	
	public static Attributes getMockEntry() {
		BasicAttributes attrs = new BasicAttributes();

		attrs.put(PiTypeContactDAO.getObjectClasses());

		attrs.put("piEntryId", "aaa");	

		attrs.put("fullname", "Roberto Polli"); 
		attrs.put("givenName", "Roberto");
		attrs.put("sn", "Polli");
		attrs.put("company", "Babel");
		attrs.put("middleName", "J.");  
		attrs.put("suffix", "mr.");

		// mail
		attrs.put("piEmail1", "roberto.polli@babel.it");
		attrs.put("piEmail2", "rpolli@babell.com");
		attrs.put("piEmail3", "roberto.polli@libero.it");

		//phone
		for (int i=1; i<=13; i++ ) {
			attrs.put("piPhone"+i, ""+(i+4));
		}

		// address
		attrs.put("workPostalAddress", "postalAddress s.benedetto");
		attrs.put("workCity", "pomezia");		
		attrs.put("workPostalCode", "00040");
		attrs.put("workPOBox", "pobox babel");
		attrs.put("workState", "UK");		
		attrs.put("workCountry", "United Kingdom");

		attrs.put("title", "dr.");
		//		attrs.put("nickname", "ioggstream");


		attrs.put("homeCity", "Latina"); 
		attrs.put("homeState", "IT"); 
		attrs.put("homePostalCode", "04100"); 
		attrs.put("homePostalAddress", "via del monte"); 
		attrs.put("homeCountry", "Italy");

		attrs.put("dateOfBirth", "05.12.1977");
		attrs.put("notes", "That's me");
		attrs.put("inetFreeBusy", "http://caldav.babel.it/fburl?rpolli");
		//		attrs.put("inetCalendar", "http://caldav.babel.it/calendar?rpolli");

		return attrs;
	}

	public static Attributes getMockSimpleEntry() {
		Attributes entryAttributes = new BasicAttributes();
		// entryAttributes.put(ldapInterface.getLdapId(), ENTRY_UID);
		entryAttributes.put("objectClass", "person");
		entryAttributes.put("objectClass", "pabPerson");
		entryAttributes.put("cn", USER_FULLNAME);
		entryAttributes.put("uid", "Roberto Polli");
		entryAttributes.put("mail", "rpolli@babel.it");
		entryAttributes.put("givenName", "Roberto");
		entryAttributes.put("sn", "Polli");
		entryAttributes.put("mobile", "347123123132");
		entryAttributes.put("telephoneNumber", "06123123123");
		return entryAttributes;
	}

	public static Attributes getMockInetOrgPersonEntry() {
		BasicAttributes attrs = new BasicAttributes();

		attrs.put(PiTypeContactDAO.getObjectClasses());

		attrs.put("piEntryId", "-1");	
		attrs.put("fullname", USER_FULLNAME); 
		attrs.put("givenName", "Roberto");
		attrs.put("sn", "Polli");
		attrs.put("o", "Babel");

		// mail
		//	attrs.put("mail", "rpolli@babel.it");
		//	attrs.put("mailAlternateAddress", "roberto.polli@babel.it");
		attrs.put("piEmail1", "roberto.polli@email.it");
		attrs.put("piEmail2", "robipolli@gmail.com");
		attrs.put("piEmail3", "roberto.polli@email.it");
		attrs.put("piEmail4", "robipolli@gmail.com");
		//phone
		attrs.put("homePhone", "1");
		attrs.put("telephoneNumber", "2");
		attrs.put("facsimileTelephoneNumber", "3");
		attrs.put("mobile", "4");

		for (int i=1; i<10; i++ ) {
			attrs.put("piPhone"+i, ""+(i+4));
		}

		// address
		attrs.put("postalAddress", "postalAddress s.benedetto");
		attrs.put("l", "pomezia");		
		attrs.put("postalCode", "00040");
		attrs.put("middleName", "J.");  
		attrs.put("title", "dr.");
		attrs.put("nickname", "ioggstream");//TODO

		attrs.put("street", "street p.zza s.benedetto");//TODO
		//		attrs.put("st", "IT");//TODO

		attrs.put("homeCity", "Latina"); 
		attrs.put("homeState", "IT"); 
		attrs.put("homePostalCode", "04100"); 
		attrs.put("homePostalAddress", "via del monte"); 
		attrs.put("co", "Italy");//TODO
		attrs.put("dateOfBirth", "05.12.1977");
		attrs.put("description", "That's me");//TODO
		attrs.put("calFbUrl", "http://caldav.babel.it/fburl?rpolli");//TODO

		return attrs;
	}


	/* (Other, Home, Business) x (Telephone,Mobile) x Number
	 * Email1 Email2 Email3 x Address
	 * 
	 * TEL;FAX;WORK: v

TEL;CELL: 			v 
TEL;VOICE;HOME: v
TEL;VOICE;WORK: v

TEL;CELL;HOME: MobileHomeTelephoneNumber
TEL;CELL;WORK: MobileBusinessTelephoneNumber
TEL;FAX:
TEL;FAX;HOME:
TEL;PAGER:
TEL;VOICE: OtherTelephoneNumber
TEL;VOICE;CAR:
TEL;VOICE;PREF:
TEL;WORK;PREF:
	 */
}
