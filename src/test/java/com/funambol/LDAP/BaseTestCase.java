package com.funambol.LDAP;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.naming.NameNotFoundException;

import junit.framework.TestCase;


import com.funambol.LDAP.utils.Constants;
import com.funambol.email.console.dao.ConsoleDAO;
import com.funambol.email.exception.InboxListenerConfigException;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.tools.DataSourceTools;
import com.funambol.server.admin.UserManager;
import com.funambol.server.config.Configuration;
import com.funambol.server.db.DataSourceContextHelper;



public abstract class BaseTestCase extends TestCase implements Constants {

	protected static FunambolLogger logger =  new FunambolLogger("BaseTestCase");
	
	protected final String LDAP_URI = "ldap://ldap.example.com/";
	protected final String LDAP_SEARCH_MAIL = "(mail=%s)";
	protected final String LDAP_SEARCH_UID = "(uid=%u)";
	protected final String BASE_DN = "o=bigcompany,dc=babel,dc=it";
	protected final String USER1_DN = "ou=people"+BASE_DN;
	protected final String ROOT_DN = "dc=babel,dc=it";
	
	protected final String USER_MAIL = "daniele@babel.it";
	protected final String USER_MAIL_UID = "daniele";
	protected final String USER_MAIL_PASSWORD = "daniele";
	
	protected final String DM_USER="uid=admin,dc=babel,dc=it";
	protected final String DM_PASS="admin";
	
	public final String FCTF_BASIC = "basic/text/x-vcard/";
	public final String FCTF_ADVANCED = "advanced/text/x-vcard/";
	public final String FCTF_ADVANCED_VCARD = "advanced/text/vcard/";
	
	public final String CORE_DS = "jdbc/fnblcore";
	public final String USER_DS = "jdbc/fnbluser";
	public final String STANDARD_DS = "jdbc/fnblds";
	boolean initialized = false;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();	
		System.setProperty("funambol.home","./src/test/resources");
		System.setProperty("java.naming.factory.initial", "org.apache.naming.java.javaURLContextFactory");
		bindToTestDataSource();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		DataSourceContextHelper.closeDataSources();
		resetConsoleDao();
	}
		
	public  void bindToTestDataSource( ) throws Exception {
		
		if (!initialized) {
			try {
				DataSourceTools.lookupDataSource(CORE_DS);				
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
			return inputStreamAsString(stream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

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
