package com.funambol.LDAP.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.StringUtils;

import com.funambol.common.pim.common.Property;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;

public class LdapUtils {

	private static FunambolLogger logger =  FunambolLoggerFactory.getLogger(Constants.LOGGER_LDAP);
	
	public static SyncItemKey[] ListToSyncItemKey(List<String> list)
	{
		SyncItemKey[] keys = new SyncItemKey[list.size()];
		int i=0;
		for (String s: list) {
			keys[i++] =  new SyncItemKey(s);
		}
		return keys;
	}
	
	
	/**
	 * Converts an generalized times to a java.util.Timestamp object
	 * @param s timestamp string from OpenLDAP 
	 * @return t
	 */
	public static Timestamp generalized2timestamp(String s, TimeZone tz) {
		s = s.substring(0, 14); // remove the ending char 'Z';
	
		Timestamp t = null;
		SimpleDateFormat mySimpleFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	
		try {
			t = new Timestamp(mySimpleFormat.parse(s).getTime());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	
		return t;
	}

	/**
	 * Converts a java.util.Timestamp to a generalized time
	 * @param mytime input timestamp
	 * @return localized timestamp ?
	 */
	public static String Timestamp2generalized(Timestamp mytime, TimeZone tz) {
		Timestamp b = new Timestamp(mytime.getTime());
		SimpleDateFormat mySimpleFormat = new SimpleDateFormat("yyyyMMddHHmmssZ");
		mySimpleFormat.setTimeZone(tz);
	
		return mySimpleFormat.format(b);
	}

	/**
	 * Converts a java.util.Timestamp to UTC
	 * needed if the "Generalized Time Syntax Plug-in" of FedoraDS is not enabled
	 * @param mytime input timestamp
	 * @return UTC SQL timestamp
	 */
	public static String Timestamp2UTC(Timestamp mytime) {
		Timestamp b = new Timestamp(mytime.getTime());
		SimpleDateFormat mySimpleFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		mySimpleFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String ret = mySimpleFormat.format(b) + "Z";
	
		return ret;
	}

	public static String getPrintableAttribute(Attribute p) {
		String ret = null;
		try {
			if (p != null) {
				ret = (String) p.get();
			} 
		} catch (NamingException e) {
			//noop
		}
		return ret != null ? ret : "";
	}

	public static String getPrettyAttribute(SearchResult sr, String attributeName) throws NamingException {
		String ret = null;
		if ( (attributeName != null) && (sr != null)) {
			Attribute attr  = sr.getAttributes().get(attributeName);
			if (attr != null) {
				ret =  (String) attr.get();
			}
		}
		return (ret != null) ? ret : ""; 
	}

	public static String getPrintableProperty(Property p) {
		return p != null ? (String) p.getPropertyValue() : "";
	}


	public static  List<String> compareMultiLine(String first, String second) {
		String[] a1 = first.split("[\r\n]+");
		String[] a2 = second.split("[\r\n]+");
		List<String> firstList =  new ArrayList<String>();
	
		for (String s: a1) {
			if (StringUtils.isNotEmpty(s)) {
				firstList.add(s);
			}
		}
		
		for (String s: a2) {	
				
			if (firstList.indexOf(s) < 0) {
				logger.info("+"+ s);
			} else {
				firstList.remove(s);
			}
		}
			
		for (String a: firstList) { 
				logger.info("-"+a);
		}
	
		return firstList;
	}


	//
	// helper method for difference between strings
	//
	public static boolean containsMultiLine(String first, String second) {
		String[] a1 = first.split("[\r\n]");
		boolean ret = true;
		
		for (int i = 0; i < a1.length; i++) {		
			if (! second.contains(a1[i])) {
				logger.info(a1[i] +"\t\t");
				ret =false;
			}			
		}
		return ret;
	}

}
