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

package com.funambol.LDAP.engine.source;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.funambol.LDAP.converter.ContactToVcard3;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.converter.ContactToSIFC;
import com.funambol.common.pim.converter.ContactToVcard;
import com.funambol.common.pim.converter.ConverterException;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemImpl;
import com.funambol.framework.engine.source.SyncSource;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.tools.Base64;
import com.funambol.framework.tools.beans.LazyInitBean;

/**
 * This class implements a LDAP <i>SyncSource</i>
 *
 * @author  <a href='mailto:fipsfuchs _@ users.sf.net'>Philipp Kamps</a>
 * @author  <a href='mailto:julien.buratto _@ subitosms.it'>Julien Buratto</a>
 * @author  <a href='mailto:gdartigu _@ smartjog.com'>Gilles Dartiguelongue</a>
 * @version $Id$
 */
public class LDAPContactsSyncSource extends AbstractLDAPSyncSource 
implements SyncSource, Serializable, LazyInitBean {

	// -------------------------------------------------------------- Constants

	private static final long serialVersionUID = 8079237849034290126L;

	// ----------------------------------------------------------- Constructors

	public LDAPContactsSyncSource() {
		super(null); // rpolli super(null);
	}

	public LDAPContactsSyncSource(String name) {
		super(name);
	}

	// --------------------------------------------------------- Public methods

	
	
	
	
	
	
	
	
	
	
	
	

	// ------------------------------------------------------ Protected methods

	/**
	 * @see AbstractLDAPSyncSource
	 */
	protected SyncItem[] setSyncItems(SyncItem[] syncItems, Contact contact)
	throws SyncSourceException {
		if (logger.isInfoEnabled())
			logger.info("setSyncItems(" + principal + " , ...)");

		SyncItem[] ret = new SyncItem[syncItems.length];
		for (int i = 0; i < syncItems.length; ++i) {
			ret[i] = new SyncItemImpl(this, syncItems[i].getKey().getKeyAsString() + "-1");
		}
		
		return ret;
	}

	/**
	 * @see AbstractLDAPSyncSource
	 */
	protected List<SyncItem> getSyncItems(List<Contact> contacts, char state)
	throws SyncSourceException {
		List<SyncItem> syncItems = new ArrayList<SyncItem>();
		if (logger.isInfoEnabled())
			logger.info("Count of contacts " + contacts.size());

		Iterator<Contact> it = contacts.iterator();

		while (it.hasNext()) {
			Contact ct = it.next();
			if (ct != null && ct.getUid() != null) {
				syncItems.add(createItem(ct.getUid(), ct, state));
			} else {
				logger.warn("Found a null contact or key");
			}
		}
		
		return syncItems;
	}

	// ------------------------------------------------------------ Private methods

	/**
	 * Creates a <i>SyncItem</i> with the given state from a <i>Contact</i> object.
	 * @param contact data to create the <i>SyncItem</i>
	 * @param state State of the synchronisation of this contact
	 * @return <i>SyncItem</i> associated with the contact
	 * @throws SyncSourceException
	 */
	public SyncItem createItem(String uid, Contact contact,  char state)
	throws SyncSourceException {
		SyncItem syncItem = null;
		String content = null;
		if (uid == null)
			throw new SyncSourceException("Can't create a syncitem with null key");
		
		if (logger.isInfoEnabled())
			logger.info("createItem(" +  uid + " , ...)");
		
		syncItem = new SyncItemImpl(this, uid, state);
		
		if (info.getPreferredType().type.equals("text/x-vcard")) {
			content = contact2vcard(contact);
		} else if (info.getPreferredType().type.equals("text/vcard")) {
			content = contact2vcard3(contact);
		} else {
			content = contact2sifc(contact);
		}
		
		if (false)// || isEncode()) 
		{
			syncItem.setContent(Base64.encode((content).getBytes()));
			syncItem.setType(info.getPreferredType().type);
			syncItem.setFormat("b64");
		} else {
			syncItem.setContent(content.getBytes());
			syncItem.setType(info.getPreferredType().type);
		}

		return syncItem;
	}

	/**
	 * Converts the given contact into a vcard String
	 * @param c the contact to convert
	 * @return the vcard
	 * @throws SyncSourceException in case of convertion errors
	 */
	private String contact2vcard(Contact c) throws SyncSourceException {
		if (logger.isDebugEnabled())
			logger.debug("contact2vcard(" + c.getUid() + " , ...)");
		
		try {
			return new ContactToVcard( serverTimeZone , null).convert(c);
		} catch (Exception e) {
			throw new SyncSourceException("Conversion error for item "
					+ c.getUid() + ": " + e.getMessage(), e);
		}
	}
	/**
	 * Converts the given contact into a vcard String
	 * @param c the contact to convert
	 * @return the vcard
	 * @throws SyncSourceException in case of convertion errors
	 */
	private String contact2vcard3(Contact c) throws SyncSourceException {
		if (logger.isDebugEnabled())
			logger.debug("contact2vcard3(" + c.getUid() + " , ...)");
		
		try {
			return new ContactToVcard3( serverTimeZone , null).convert(c);
		} catch (Exception e) {
			throw new SyncSourceException("Conversion error for item "
					+ c.getUid() + ": " + e.getMessage(), e);
		}
	}
	/**
	 * Converts the given contact into a sifc String
	 * @param c the contact to convert
	 * @return the sifc document
	 * @throws SyncSourceException in case of convertion errors
	 */
	private String contact2sifc(Contact c) throws SyncSourceException {
		if (logger.isDebugEnabled())
			logger.debug("contact2sifc(" + c.getUid() + " , ...)");
		
		try {
			return new ContactToSIFC(null, null).convert(c);
		} catch (ConverterException e) {
			e.printStackTrace();
			throw new SyncSourceException("Convertion error for item "
					+ c.getUid() + ": " + e.getMessage(), e);
		}
	}
	
    /**
     * Extracts the content from a syncItem.
     *
     * @param syncItem
     * @return as a String object (same as
     *         PIMSyncSource#getContentFromSyncItem(String), but trimmed)
     * @see PIMContactSyncSource.java
     */
    protected String getContentFromSyncItem(SyncItem syncItem) {

        String raw = super.getContentFromSyncItem(syncItem);

        return raw.trim();
    }



}
