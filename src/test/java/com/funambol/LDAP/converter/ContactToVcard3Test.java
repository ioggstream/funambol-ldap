package com.funambol.LDAP.converter;

import java.io.InputStream;

import com.funambol.LDAP.BaseTestCase;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.converter.ContactToVcard;
import com.funambol.common.pim.vcard.ParseException;
import com.funambol.common.pim.vcard.VcardParser;


public class ContactToVcard3Test extends BaseTestCase {

	public void testParse_1() throws Exception {
			// convert a vcard entry to ldap
			String vcards[] = { 
			"card-0.vcard","card-1.vcard" };

			for (String vcf : vcards) {
				try {
					InputStream stream =  this.getClass().getClassLoader()
					.getResourceAsStream(FCTF_ADVANCED_VCARD   +  vcf);
					String c0 = getResourceAsString(FCTF_ADVANCED_VCARD   +  vcf);
					VcardParser parser = new VcardParser(stream);

					Contact pimC = parser.vCard();
					assertNotNull(pimC);
					
					pimC.getPersonalDetail().getAddress();
					ContactToVcard3 serializer = new ContactToVcard3(null, "UTF-8");
					logger.warn(serializer.convert(pimC));
					ContactToVcard v21serializer = new ContactToVcard(null, "UTF-8");
					logger.warn(v21serializer.convert(pimC));
					
				} catch (ParseException e) {
					logger.error(e.getMessage());
					fail("Bad test data:" + e);
				}  catch (Exception e) {
					logger.error("missing file: "+ e.getMessage());
					fail("missing file:" + e.getMessage());
				}
			}

	}
}
