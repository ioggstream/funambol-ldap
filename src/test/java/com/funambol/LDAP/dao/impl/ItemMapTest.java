package com.funambol.LDAP.dao.impl;

import java.util.HashMap;

import javax.naming.NamingException;

import com.funambol.LDAP.BaseTestCase;
import com.funambol.LDAP.dao.ItemMap;
import com.funambol.LDAP.exception.DBAccessException;

public class ItemMapTest extends BaseTestCase {

	@Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();		
	}
	public void testInit() {
		
		ItemMap item = new ItemMapImpl();
		item.setPrincipal(-1L);
		item.setSourceUri("mockSourceUri");
		item.setUsername("mockUser");
		
		try {
			item.init();
		} catch (NamingException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		item = null;
		
	}
	
	public void testClean() {
		ItemMap item = new ItemMapImpl();
		item.setPrincipal(-1L);
		item.setSourceUri("mockSourceUri");
		item.setUsername("mockUser");
		HashMap<String, String> map = new HashMap<String, String>();
		HashMap<String, String> newmap = null;

		map.put("sampleGuid", "sampleTimestamp");
		try {
			item.init();			
			item.updateMap(map);
			newmap = item.loadMap();
			assertNotNull(newmap);
			assertNotSame(0, newmap.size());
			
			item.clearMap();
			newmap = item.loadMap();
			assertNotNull(newmap);
			assertEquals(0, newmap.size());

		} catch (NamingException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (DBAccessException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}		
	}
}
