package com.funambol.LDAP.engine.source;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.naming.NameAlreadyBoundException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.junit.Test;

import com.funambol.LDAP.BaseTestCase;
import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.dao.impl.PiTypeContactDAO;
import com.funambol.LDAP.dao.impl.PiTypeContactDAOTest;
import com.funambol.LDAP.exception.DBAccessException;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.LdapManagerInterface;
import com.funambol.LDAP.manager.TestableLdapManager;
import com.funambol.LDAP.manager.impl.FedoraDsInterface;
import com.funambol.LDAP.security.LDAPUser;
import com.funambol.common.pim.contact.Contact;
import com.funambol.framework.core.AlertCode;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.SyncItemState;
import com.funambol.framework.engine.source.SyncContext;
import com.funambol.framework.engine.source.SyncSourceException;
import com.funambol.framework.security.Sync4jPrincipal;
import com.funambol.framework.server.Sync4jDevice;
import com.funambol.framework.server.Sync4jUser;
import com.funambol.framework.tools.beans.BeanException;
import com.funambol.server.config.Configuration;

public class TestSoftDelete extends BaseTestCase {
	private static final String BEAN_NAME = "./ldap/ldap/ldap-7.0/ldap-softdelete.xml";


	protected String USER_BASEDN = "ou=people, dc=bigdomain.net,o=bigcompany," + ROOT_DN;
	private static String USER_DN ="uid=Aaccf.Amar,ou=People,dc=bigdomain.net,o=bigcompany,dc=babel,dc=it";
	private static String USER_PASS = "password";
	private static String USER_MAIL = "aaccf.amar@bigdomain.net";
	private static String PSROOT = "ou=Aaccf.Amar@bigdomain.net, dc=bigdomain.net, dc=PAB";

	protected static final Sync4jUser USER = new Sync4jUser(USER_MAIL, "password", 
			USER_MAIL, "", "", new String[] {"sync_user"});

	Sync4jDevice device = new Sync4jDevice("junit-device");
	Sync4jPrincipal principal = new Sync4jPrincipal(100L, USER, device);
	SyncContext context = new SyncContext(principal, 0, null, null, 0);


	private LDAPUser ldapUser;
	private Sync4jPrincipal ldapPrincipal;

	private List<String> addedItems;
	TestableLdapManager manager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ldapUser = new LDAPUser(this.USER);
		ldapUser.setPsRoot("ldap://pab.example.com/"+PSROOT);

		ldapPrincipal = new Sync4jPrincipal(100L, ldapUser, device);

		addedItems = new ArrayList<String> ();
	}


	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		for( String s: addedItems) {
			removeItemById(s);
		}
	}

	private void removeItemById(String s) throws Exception {
		if (manager == null) {
			logger.warn("Null manager" );
		}  else {
			SearchResult sr = manager.searchLDAPEntryById(s);
			if (sr != null) {
				logger.info("deleting entry:" +sr.getNameInNamespace() );
				manager.delete(sr.getNameInNamespace(), false);
			} else {
				logger.warn("can't find entry with uid:" + s);
			}
		}
	}

	public void testCreateSyncSource() throws DBAccessException {
		try {
			ExtendedLDAPContactsSyncSource syncSource = (ExtendedLDAPContactsSyncSource) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);
			logger.info(syncSource);
			
			syncSource.beginSync(context);
		} catch (BeanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		} catch (SyncSourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}



	/*
	 * strategy:
	 * - addSI add (entry, funTS=t0)
	 * - getNew search: (&(createTS>=t0)(funTS>t0))
	 * - getUp search ( (createTS<=t0) (modifyTS>=t0)
	 * 
	 * case1: ldapserver.time == fun.time
	 * - createTS=t'>t0
	 * -- modifyTS=t'>t0 and createTS<t'
	 * - funTS = t0
	 * - getNew returns all entries created by funambol, while requiring funTS unset removes bad items
	 * -- getUp return all entries updated by funambol funTs>t0
	 * 
	 * case2: ldaptime > funtime
	 * - createTS=t' >> t0
	 * - funTS= t0
	 * - getNew returns all entries created by funambol, funTS solves
	 * 
	 * case3: ldaptime < funtime
	 * - createTS=t' <= t0
	 * - funTS = t0
	 * - getNew can return less items than expected
	 */

	/** 
	 * simulate a sync 
	 * @throws SyncSourceException 
	 */
	public void _testNUD_FAST() throws SyncSourceException {
		ExtendedLDAPContactsSyncSource syncSource = null;
		SyncItem syncItem1 = null;
		String funItem1 = "funItem1";
		String funItem2 = "funItem2";
		String ldapItem = "ldapItem";
		try {
			syncSource = (ExtendedLDAPContactsSyncSource) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);


			// make the first sync[0, t0]... N!=0, U=D=0
			Timestamp t0 = new Timestamp(System.currentTimeMillis()); // sync start time
			logger.warn("First sync");
			context = new SyncContext(ldapPrincipal, AlertCode.TWO_WAY, null, null, 0, null, t0);
			syncSource.beginSync(context);
			manager = new  TestableLdapManager((FedoraDsInterface) syncSource.ldapState.getManager());

			// ..lasting 1 sec...
			Thread.sleep(1000);
			// .. ad an item from mobile, and an item from ldap...
			Attributes entryAttributes = PiTypeContactDAOTest.getMockEntry();	
			Contact c = syncSource.contactDAO.createContact(entryAttributes);
			c.setUid(funItem1);
			syncItem1 = syncSource.createItem(funItem1, c, SyncItemState.NEW);			
			syncItem1.getKey().setKeyValue(funItem1);

			c.setUid(funItem2);
			SyncItem syncItem2 = syncSource.createItem(funItem2, c, SyncItemState.NEW);			
			syncItem2.getKey().setKeyValue(funItem2);
			try { // if still present on server, delete it
				addedItems.add(funItem1);
				addedItems.add(funItem2);
				syncSource.addSyncItem(syncItem1);
				syncSource.addSyncItem(syncItem2);

			} catch (SyncSourceException e) {
				if (syncItem1!= null) {
					syncSource.removeSyncItem(syncItem1.getKey(), null, false);
					syncSource.endSync();
				}
				fail(e.getMessage());
			}

			entryAttributes.put("piEntryId",ldapItem);
			addedItems.add(ldapItem);
			manager.addNewEntry(entryAttributes);
			// .. end sync
			syncSource.endSync();

			// expected N!=0, U=D=0
			assertEquals(0,syncSource.deletedUids.size());
			assertEquals(0,syncSource.updatedUids.size());
			assertNotSame(0, syncSource.newUids.size());


			// update entry by ldap
			entryAttributes.put("piEntryId",funItem2);
			manager.updateEntry(funItem2,entryAttributes);




			// make a subsequent sync after 1 sec [t0, t1]
			Thread.sleep(1000);
			logger.warn("Subsequent sync");
			Timestamp t1 = new Timestamp(System.currentTimeMillis()); // sync start time
			context = new SyncContext(ldapPrincipal, AlertCode.TWO_WAY, null, null, 0, t0, t1);
			syncSource.beginSync(context);


			// expected: N=1, U=1, D=0
			assertEquals(1, syncSource.newUids.size());

			syncSource.removeSyncItem(syncItem1.getKey(), null);
			syncSource.endSync();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {

		}

	}

	private void populateAddressBook(int n) throws LDAPAccessException {
		ContactDAOInterface piTypeCdao = new PiTypeContactDAO();

		LdapManagerInterface ldapInterface = new FedoraDsInterface("ldap://be-mmt.babel.it/"+PSROOT, "", DM_USER, DM_PASS, false, false, piTypeCdao);
		manager = new  TestableLdapManager((FedoraDsInterface) ldapInterface);
		manager.setLdapId(piTypeCdao.getRdnAttribute());
		Attributes entryAttributes = PiTypeContactDAOTest.getMockEntry();	


		for (int i=0; i<n; i++) {
			try {
				entryAttributes.put(piTypeCdao.getRdnAttribute(), UUID.randomUUID().toString());
				String uid = manager.addNewEntry(entryAttributes);
				addedItems.add(uid);
			} catch (NameAlreadyBoundException e) {
				logger.info("entry still existin");
			}
		}
		logger.info("added @entries:" + addedItems.size());
	}


	@Test
	public void _testNUD_Stress() throws SyncSourceException {
		int nItems = 1;

		try {
			populateAddressBook(nItems);
		} catch (LDAPAccessException e) {
			fail();
		}

		ExtendedLDAPContactsSyncSource syncSource = null;
		SyncItem syncItem1 = null;
		String funItem1 = "mobileItem";
		String funItem2 = "funItem2";
		String funItem3 = addedItems.get(nItems-1);

		String ldapItem = "ldapItem";
		try {
			syncSource = (ExtendedLDAPContactsSyncSource) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);


			// make the first sync[0, t0]... N!=0, U=D=0
			Timestamp t0 = new Timestamp(System.currentTimeMillis()); // sync start time
			logger.warn("\n\nFirst sync\n\n");
			context = new SyncContext(ldapPrincipal, AlertCode.TWO_WAY, null, null, 0, null, t0);
			syncSource.beginSync(context);

			logger.info(String.format("Syncsource status:\n" +
					" allKeys:%s\nnew: %s\nupdate:%s\ndeleted:%s",
					syncSource.getAllSyncItemKeys(), syncSource.getNewSyncItemKeys(null, t0),
					syncSource.getUpdatedSyncItemKeys(null, t0), syncSource.getDeletedSyncItemKeys(null, t0)
			)
			);

			assertNotNull(syncSource.newUids);
			assertTrue(syncSource.newUids.size()>=nItems);

			manager = new  TestableLdapManager((FedoraDsInterface) syncSource.ldapState.getManager());

			// ..lasting 1 sec...
			Thread.sleep(1000);
			// pass items to mobile
			for (String uid: syncSource.newUids) {
				syncSource.getSyncItemFromId(new SyncItemKey(uid));
			}
			// .. ad an item from mobile, and an item from ldap...
			Attributes entryAttributes = PiTypeContactDAOTest.getMockEntry();	
			Contact c = syncSource.contactDAO.createContact(entryAttributes);
			c.setUid(funItem1);
			syncItem1 = syncSource.createItem(funItem1, c, SyncItemState.NEW);			
			syncItem1.getKey().setKeyValue(funItem1);

			c.setUid(funItem2);
			SyncItem syncItem2 = syncSource.createItem(funItem2, c, SyncItemState.NEW);			
			syncItem2.getKey().setKeyValue(funItem2);
			try { // if still present on server, delete it
				addedItems.add(funItem1);
				addedItems.add(funItem2);
				syncSource.addSyncItem(syncItem1);
				syncSource.addSyncItem(syncItem2);

			} catch (SyncSourceException e) {
				if (syncItem1!= null) {
					syncSource.removeSyncItem(syncItem1.getKey(), null, false);
					syncSource.endSync();
				}
				fail(e.getMessage());
			}

			entryAttributes.put("piEntryId",ldapItem);
			addedItems.add(ldapItem);
			manager.addNewEntry(entryAttributes);
			// .. end sync
			syncSource.endSync();



			// expected N!=0, U=D=0
			assertEquals(0,syncSource.deletedUids.size());
			assertEquals(0,syncSource.updatedUids.size());
			assertNotSame(0, syncSource.newUids.size());


			// update entry by ldap, remove one entry from ldap
			Thread.sleep(1000);
			entryAttributes.put("piEntryId",funItem2);
			manager.updateEntry(funItem2,entryAttributes);
			removeItemById(funItem3);
			addedItems.remove(funItem3);



			// make a subsequent sync after 1 sec [t0, t1]
			Thread.sleep(1000);
			logger.warn("\n\nSubsequent sync\n\n");
			Timestamp t1 = new Timestamp(System.currentTimeMillis()); // sync start time
			context = new SyncContext(ldapPrincipal, AlertCode.TWO_WAY, null, null, 0, t0, t1);
			syncSource.beginSync(context);

			// expected: N=1, U=1, D=1
			assertEquals(1, syncSource.newUids.size());
			assertEquals(1, syncSource.updatedUids.size());
			assertEquals(1, syncSource.deletedUids.size());

			syncSource.removeSyncItem(syncItem1.getKey(), null);
			syncSource.endSync();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {

		}

	}





 public void testAddSoftDelete() {
		ExtendedLDAPContactsSyncSource syncSource;
		SyncItem syncItem1 = null;
		String funItem1 = "funItem1";
		String funItem2 = "funItem2";

		try {
			// create ss
			syncSource = (ExtendedLDAPContactsSyncSource) Configuration.getConfiguration().getBeanInstanceByName(BEAN_NAME);
			logger.info(syncSource);

			// first sync: create item
			Timestamp t0 = new Timestamp(System.currentTimeMillis());
			context = new SyncContext(ldapPrincipal, AlertCode.TWO_WAY, null, null, 0, null, t0);

			syncSource.beginSync(context);
			manager = new  TestableLdapManager((FedoraDsInterface) syncSource.ldapState.getManager());

			// .. ad an item from mobile, and an item from ldap...
			Attributes entryAttributes = PiTypeContactDAOTest.getMockEntry();	
			Contact c = syncSource.contactDAO.createContact(entryAttributes);
			c.setUid(funItem1);
			syncItem1 = syncSource.createItem(funItem1, c, SyncItemState.NEW);			
			syncItem1.getKey().setKeyValue(funItem1);
			
			addedItems.add(funItem1);
			syncSource.addSyncItem(syncItem1);
			syncSource.endSync();
			
			// second sync, delete item (soft)
			Thread.sleep(1000);
			Timestamp t1 = new Timestamp(System.currentTimeMillis());
			context = new SyncContext(ldapPrincipal, AlertCode.TWO_WAY, null, null, 0, t0, t1);

			syncSource.beginSync(context);
			syncSource.removeSyncItem(syncItem1.getKey(), null,false);
			syncSource.endSync();
			


			// check item is still on ldap
			List<String> softDeletedItems = manager.searchDn(
					String.format("(&(%s=%s)(%s=1))", 
							syncSource.contactDAO.getRdnAttribute(),funItem1,syncSource.contactDAO.getSoftDeleteAttribute()), 
							SearchControls.SUBTREE_SCOPE
					);
			assertNotNull(softDeletedItems);
			assertNotSame(0, softDeletedItems.size());
			
			// control sync: check stored map and internal data 
			Thread.sleep(1000);
			t0=t1;
			t1 = new Timestamp(System.currentTimeMillis());
			context = new SyncContext(ldapPrincipal, AlertCode.TWO_WAY, null, null, 0, t0, t1);
			syncSource.beginSync(context);
			
			// check item is removed from ItemMap
			// and that item is not found as a newly deleted item
			assertNull(syncSource.itemMap.loadMap().get(funItem1));
			assertEquals(0, syncSource.deletedUids.size());
			
			// side effect: check if at next sync item is in some NUD list

		} catch (BeanException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (SyncSourceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (LDAPAccessException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (DBAccessException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
	
}



}
