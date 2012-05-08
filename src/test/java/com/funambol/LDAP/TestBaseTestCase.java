package com.funambol.LDAP;

import java.sql.Connection;

import javax.naming.NameNotFoundException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.funambol.framework.tools.DataSourceTools;
import com.funambol.server.db.RoutingDataSource;

/**
 * This class tests basic methods of basetestcase
 * @author rpolli
 *
 */
public class TestBaseTestCase extends BaseTestCase{

	Logger logger = Logger.getLogger(this.getClass());

	@Test
	public void testMockDataSource() throws Exception {
		super.bindToTestDataSource();
		String source = null;
		try {

			source = CORE_DATASOURCE_JNDINAME;
			logger.info("testMockDataSource: " + source);
			DataSource testCoreDs = DataSourceTools.lookupDataSource(source);
			assertNotNull(testCoreDs);


			Connection conn = testCoreDs.getConnection();
			assertNotNull(conn);
			conn.close();
			source = USER_DATASOURCE_JNDINAME;

			logger.info("testMockDataSource: " + source);			
			RoutingDataSource testUserDs = (RoutingDataSource) DataSourceTools.lookupDataSource(source);
			assertNotNull(testUserDs);

			conn = testUserDs.getRoutedConnection("mockUserName");
			assertNotNull(conn);
			conn.close();
		} catch (NameNotFoundException e) {
			logger.error("Unavailable datasource:" + source);
			assertNull(e);
		} catch (ClassCastException e) {
			logger.error("Runtime error:" + source);
			assertNull(e);
		}

	}

}
