package com.funambol.LDAP;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import javax.naming.NameNotFoundException;
import javax.sql.DataSource;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.vcard.VcardParser;
import com.funambol.email.console.dao.ConsoleDAO;
import com.funambol.email.exception.InboxListenerConfigException;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemImpl;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.SyncItemState;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.tools.DataSourceTools;
import com.funambol.server.admin.UserManager;
import com.funambol.server.config.Configuration;
import com.funambol.server.db.DataSourceContextHelper;
import com.funambol.server.db.RoutingDataSource;
import com.funambol.tools.database.DBHelper;



public abstract class BaseTestCase extends TestCase implements TestConstants {

	protected static FunambolLogger logger =  new FunambolLogger(BaseTestCase.class);
	boolean initialized = false;

	public LdapCredentials credential = null;

	String create_ldap_table = getResourceAsString(HYPERSONIC_CREATE_SCHEMA_SQL);	
	String drop_ldap_table = getResourceAsString(HYPERSONIC_DROP_SCHEMA_SQL);

	String init_fnbl_table = getResourceAsString(SQL_FUNAMBOL_SQL);	
	String drop_fnbl_table = getResourceAsString(SQL_DROP_FUNAMBOL_SQL);

	public String create_email_schema = getResourceAsString(SQL_EMAIL_FUNAMBOL_SQL);
	public String drop_email_schema = getResourceAsString(SQL_EMAIL_DROP_FUNAMBOL_SQL);

	public RoutingDataSource userds;
	public DataSource coreds;

	@Before
	protected void setUp() throws Exception {
		super.setUp();	
		System.setProperty("funambol.home","./src/test/resources");
		System.setProperty("java.naming.factory.initial", "org.apache.naming.java.javaURLContextFactory");
		// createMockDataSource();
		DataSourceContextHelper.configureAndBindDataSources();
		bindToTestDataSource();
		userds = (RoutingDataSource) DataSourceTools.lookupDataSource(USER_DATASOURCE_JNDINAME);		
		coreds = DataSourceTools.lookupDataSource(CORE_DATASOURCE_JNDINAME);
		DBHelper.executeStatement(userds.getRoutedConnection(USER_DATASOURCE_JNDINAME), create_ldap_table ); 
		DBHelper.executeStatement(coreds.getConnection(), init_fnbl_table ); 

		credential = (LdapCredentials) Configuration.getConfiguration().getBeanInstanceByName(CREDENTIAL_BEAN_NAME);
	}

	@After
	protected void tearDown() throws Exception {
		super.tearDown();
		DBHelper.executeStatement(userds.getRoutedConnection(USER_EMAIL), drop_ldap_table);
		DBHelper.executeStatement(coreds.getConnection(), drop_fnbl_table ); 

		DataSourceContextHelper.closeDataSources();
		resetConsoleDao();		
	}


	@Test
	public  void bindToTestDataSource( ) throws Exception {

		if (!initialized) {
			try {
				DataSourceTools.lookupDataSource(CORE_DATASOURCE_JNDINAME);				
			} catch (NameNotFoundException e) {
				// noop
				DataSourceContextHelper.configureAndBindDataSources();
				initialized=true;
				logger.info("Datasource initialized");
			}
		} 
	}

	private ConsoleDAO cdao ;
	public ConsoleDAO getConsoleDAO() {
		if (cdao == null) {
			try {
				cdao = new ConsoleDAO();
			} catch (InboxListenerConfigException e) {
				e.printStackTrace();
				fail("Can't create ConsoleDAO: " + e.getMessage());
			}
		}
		return cdao;
	}

	public void resetConsoleDao() {
		cdao = null;
	}

	private UserManager manager;
	public UserManager getUserManager() {
		if (manager == null) {
			Configuration config = Configuration.getConfiguration();

			manager = (UserManager)config.getUserManager();	
		}
		return manager;
	}



	protected String getResourceAsString(String resourceName) {
		InputStream stream = this.getClass().getClassLoader()
		.getResourceAsStream(resourceName);
		try {
			if (stream ==null)
				throw new IOException();
			return inputStreamAsString(stream);
		} catch (IOException e) {
			logger.error("can't find "+ resourceName);
			return null;
		}

	}

	protected Contact getResourceAsContact(String resourceName) throws Exception {
		InputStream stream =  this.getClass().getClassLoader()
		.getResourceAsStream(resourceName);
		VcardParser parser = new VcardParser(stream);

		return  parser.vCard();
	}

	/**
	 * 
	 * @param resourceName
	 * @return a new syncitem with TYPE unset containing the given content
	 * @throws Exception
	 */
	protected SyncItem getResourceAsSyncItem(String resourceName, String type) throws Exception {
		return  new SyncItemImpl(	
				null, 
				new SyncItemKey("-1"),
				null, 
				SyncItemState.NEW, 
				getResourceAsString(resourceName).getBytes(),
				null,
				type,
				new Timestamp(System.currentTimeMillis())
		);
	}

	public SyncItem getResourceAsSyncItem(String path, String uid, String type) {
		String c0 = getResourceAsString(path);
		return new SyncItemImpl(	
				null, 
				uid,
				null, 
				SyncItemState.NEW, 
				c0.getBytes(),
				null,
				type,
				null
		);
	}

	protected static String inputStreamAsString(InputStream stream)
	throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		StringBuilder sb = new StringBuilder();
		String line = null;

		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}

		br.close();
		return sb.toString();
	}
}
