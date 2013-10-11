package com.applane.afb;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.ui.browser.client.exception.ResourceException;

public class UniqueCodeGenerator {

	/**
	 * @param args
	 * 
	 */
	// Method use to encode hashCode by some characters.
	public static String convert(int hashcode) {
		char[] arr = new char[] { 's', 'd', 'g', 'a', 'z', 'h', 'i', 'p', 'k', 'l' };
		String uniquekey = "";
		while (hashcode > 0) {
			int remainder = hashcode % 10;
			hashcode = hashcode / 10;
			for (int i = 0; i < arr.length; i++) {
				if (remainder == i) {
					uniquekey = uniquekey.concat(String.valueOf(arr[i]));
					break;
				}
			}
		}
		return uniquekey;
	}

	// Method use to create a unique key by joining user id, date time and primary key.
	public static String createUniqueCode(String s, Object key) {
		StringBuffer uniquekey = new StringBuffer();
		int hashcode = Math.abs(s.hashCode());
		uniquekey.append(convert(hashcode));
		uniquekey.append(String.valueOf('c'));
		Calendar currentDate = Calendar.getInstance();
		String ukdate = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss").format(currentDate.getTime());
		int hashdate = Math.abs(ukdate.hashCode());
		uniquekey.append(convert(hashdate));
		uniquekey.append(String.valueOf('c'));
		uniquekey.append(convert(Integer.parseInt(key.toString())));
		return uniquekey.toString();
	}

	public static String getEncryptedUniqueCode(String string, Object key) {
		try {
			LogUtility.writeError("string: " + string + " ----- key: " + key);
			String uniqueCode = createUniqueCode(string, key);
			MessageDigest mdEncription = MessageDigest.getInstance("MD5");
			mdEncription.update(uniqueCode.getBytes(), 0, uniqueCode.length());
			return new BigInteger(1, mdEncription.digest()).toString(16);
		} catch (NoSuchAlgorithmException e) {
			throw new ResourceException("problem while " + ExceptionUtils.getExceptionTraceMessage("UniqueCodeGenerator", e));
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage("UniqueCodeGenerator", e));
		}
	}
}
