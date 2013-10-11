package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.moduleimpl.SystemParameters;

public class ApplicationsGenerateCode {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public static synchronized String generateCode(String departmentName, String departmentAbbreviation, Object numbergenerationapplicationid) throws JSONException {
		String fixedstring = "";
		Integer organizationAbbreviationSequence = new Integer(0);
		Integer CourseAbbreviationSequence = new Integer(0);
		Integer SessionAbbreviationSequence = new Integer(0);
		Integer FixedStringAbbreviationSequence = new Integer(0);

		JSONArray ApplicationSequencesList = getNumberGenerationSchemeDetail(numbergenerationapplicationid);
		if (ApplicationSequencesList.length() == 0) {
			throw new BusinessLogicException("There is no scheme defined for Receipt No.");
		}

		int currentSequenceNumber = ApplicationSequencesList.getJSONObject(0).opt("lastno") == null ? 0 : ApplicationSequencesList.getJSONObject(0).optInt("lastno");
		int numberstartfrom = ApplicationSequencesList.getJSONObject(0).opt("numberstartfrom") == null ? 0 : ApplicationSequencesList.getJSONObject(0).optInt("numberstartfrom");
		Object key = ApplicationSequencesList.getJSONObject(0).optInt("__key__");
		if (numberstartfrom > currentSequenceNumber) {
			currentSequenceNumber = numberstartfrom;
			currentSequenceNumber--;
		}
		currentSequenceNumber = currentSequenceNumber + 1;
		String seprator = "";
		if (ApplicationSequencesList.getJSONObject(0).opt("seprator") != null) {
			seprator = (String) ApplicationSequencesList.getJSONObject(0).opt("seprator");
		}

		Integer paddingNumber = 0;
		if (ApplicationSequencesList.getJSONObject(0).opt("noofdigits") != null) {
			paddingNumber = (Integer) ApplicationSequencesList.getJSONObject(0).opt("noofdigits");
		}
		String str;

		str = ZeroPad(currentSequenceNumber, paddingNumber);
		HashMap schemeParts = new HashMap();

		// Organization short name
		Integer isOrganizationName = ApplicationSequencesList.getJSONObject(0).optInt("isorganizationshortname");
		Boolean isOrganizationShortName = true;
		if (isOrganizationName == null || isOrganizationName.equals(new Integer(0))) {
			isOrganizationShortName = Boolean.FALSE;
		}
		if (isOrganizationShortName.equals(Boolean.TRUE)) {
			String organizationName = "";
			JSONArray organizationList = getInstituteInformation();
			int length = organizationList.length();
			if (length > 0) {
				organizationName = (String) organizationList.getJSONObject(0).optString("organizationname") == null ? "" : organizationList.getJSONObject(0).optString("organizationname");
			}

			if (ApplicationSequencesList.getJSONObject(0).optInt("orgnizationshortnamedigits") > 0) {
				Integer orgdigit = ApplicationSequencesList.getJSONObject(0).opt("orgnizationshortnamedigits") == null ? 0 : ApplicationSequencesList.getJSONObject(0).optInt("orgnizationshortnamedigits");
				if (orgdigit < organizationName.length()) {
					organizationName = organizationName.substring(0, orgdigit);
				}
			}

			if (ApplicationSequencesList.getJSONObject(0).opt("orgnizationshortnamesequence") != null) {
				organizationAbbreviationSequence = (Integer) Integer.valueOf(ApplicationSequencesList.getJSONObject(0).optString("orgnizationshortnamesequence"));
			} else {
				organizationAbbreviationSequence = 0;
			}
			addPart(schemeParts, organizationAbbreviationSequence, organizationName, seprator);

		}

		// Current year
		SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		// find current year from current date
		Date todayDate = (SystemParameters.getSystemParameters().getCurrentDateTime());
		/*
		 * Date currentDate = null;
		 * 
		 * if (todayDate != null) { LogUtility.writeLog("Today date is not equal to null"); try { LogUtility.writeLog("todayDate PARSE IN QUERY DATE FORMAT"); currentDate = queryDateFormat.parse("" + todayDate); } catch (ParseException e) { e.printStackTrace(); try { LogUtility.writeLog("todayDate PARSE IN UPDATE FORMAT"); currentDate = updateDateFormat.parse("" + todayDate); } catch (ParseException e1) { e1.printStackTrace(); throw new BusinessLogicException("Today date is not parsable" + e.getMessage()); } } }
		 */
		SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
		String currentYearName = yearFormat.format(todayDate);
		Integer isCurrentYear = ApplicationSequencesList.getJSONObject(0).optInt("iscurrentyear");
		Boolean isYear = Boolean.TRUE;
		if (isCurrentYear == null || isCurrentYear.equals(new Integer(0))) {
			isYear = Boolean.FALSE;
		}
		if (isYear.equals(Boolean.TRUE)) {
			/*
			 * if (ApplicationSequencesList.getJSONObject(0).optInt("currentyeardigits") > 0) { Integer yearDigit = ApplicationSequencesList.getJSONObject(0).optInt("currentyeardigits"); if (yearDigit < academicYearName.length()) { academicYearName = academicYearName.substring(0, yearDigit);
			 * 
			 * } }
			 */

			if (ApplicationSequencesList.getJSONObject(0).opt("currentyearsequence") != null) {
				SessionAbbreviationSequence = (Integer) Integer.valueOf(ApplicationSequencesList.getJSONObject(0).optString("currentyearsequence"));
			} else {
				SessionAbbreviationSequence = 0;
			}
			addPart(schemeParts, SessionAbbreviationSequence, currentYearName, seprator);
		}

		// department name
		Integer isDepartmentName = ApplicationSequencesList.getJSONObject(0).optInt("isdepartmentname");
		Boolean isDepartment = Boolean.TRUE;
		if (isDepartmentName == null || isDepartmentName.equals(new Integer(0))) {
			isDepartment = Boolean.FALSE;
		}

		if (isDepartment.equals(Boolean.TRUE)) {

			if (ApplicationSequencesList.getJSONObject(0).optInt("departmentnamedigits") > 0) {
				Integer departmentDigit = ApplicationSequencesList.getJSONObject(0).optInt("departmentnamedigits");

				if (departmentAbbreviation != null && departmentAbbreviation.trim().length() > 0) {
					if (departmentDigit < departmentAbbreviation.length()) {
						departmentAbbreviation = departmentAbbreviation.substring(0, departmentDigit);
					}
				} else {
					if (departmentDigit < departmentName.length()) {
						departmentName = departmentName.substring(0, departmentDigit);
					}
					departmentAbbreviation = departmentName;
				}
			}

			if (ApplicationSequencesList.getJSONObject(0).opt("departmentnamesequence") != null) {
				CourseAbbreviationSequence = (Integer) Integer.valueOf(ApplicationSequencesList.getJSONObject(0).optString("departmentnamesequence"));
			} else {
				CourseAbbreviationSequence = 0;
			}
			addPart(schemeParts, CourseAbbreviationSequence, departmentAbbreviation, seprator);
		}

		// Fixed string

		Integer isfixedstring = ApplicationSequencesList.getJSONObject(0).optInt("isfixedstring");

		Boolean IsFixedStringAbbreviation = Boolean.TRUE;
		if (isfixedstring == null || isfixedstring.equals(new Integer(0))) {
			IsFixedStringAbbreviation = Boolean.FALSE;
		}

		if (IsFixedStringAbbreviation.equals(Boolean.TRUE)) {
			if (ApplicationSequencesList.getJSONObject(0).opt("fixedstring") != null && ApplicationSequencesList.getJSONObject(0).optString("fixedstring").length() > 0) {
				if (ApplicationSequencesList.getJSONObject(0).opt("fixedstringsequence") != null) {
					FixedStringAbbreviationSequence = (Integer) Integer.valueOf(ApplicationSequencesList.getJSONObject(0).optString("fixedstringsequence"));
				} else {
					FixedStringAbbreviationSequence = 0;
				}

				fixedstring = (String) ApplicationSequencesList.getJSONObject(0).opt("fixedstring");
				addPart(schemeParts, FixedStringAbbreviationSequence, fixedstring, seprator);
			}
		}
		Set sortedSchemeParts = new TreeSet(schemeParts.keySet());

		StringBuffer generatedNo = new StringBuffer();

		for (Iterator itr = (Iterator) sortedSchemeParts.iterator(); itr.hasNext();) {

			Object keyEle = itr.next();

			String part = (String) schemeParts.get(keyEle);

			generatedNo.append(part);

			generatedNo.append(seprator);

		}

		generatedNo.append(str);

		updateLastNo(key, currentSequenceNumber);
		return generatedNo.toString();

	}

	public static JSONArray getNumberGenerationSchemeDetail(Object numbergenerationapplicationid) throws JSONException {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_numbergenerationschemes");
			JSONArray array = new JSONArray();
			array.put("isorganizationshortname");
			array.put("orgnizationshortnamedigits");
			array.put("orgnizationshortnamesequence");
			array.put("iscurrentyear");
			array.put("currentyeardigits");
			array.put("currentyearsequence");
			array.put("isdepartmentname");
			array.put("departmentnamedigits");
			array.put("departmentnamesequence");
			array.put("isfixedstring");
			array.put("fixedstring");
			array.put("fixedstringsequence");
			array.put("numberstartfrom");
			array.put("noofdigits");
			array.put("seprator");
			array.put("lastno");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "numbergenerationapplicationid = " + numbergenerationapplicationid + " and numbergenerationtypeid = 1");
			ApplaneDatabaseEngine resourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
			JSONObject object = resourceEngine.query(query);
			JSONArray rows = object.getJSONArray("hris_numbergenerationschemes");
			return rows;
		} catch (Exception e) {
			LogUtility.writeLog("Come in catch");
			throw new BusinessLogicException("Error come while generate number generation scheme data");
		}
	}

	static String ZeroPad(long number, int width) {
		StringBuffer result = new StringBuffer("");
		for (int i = 0; i < width - Long.toString(number).length(); i++)
			result.append("0");
		result.append(Long.toString(number));
		return result.toString();
	}

	public static JSONArray getInstituteInformation() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		JSONArray array = new JSONArray();
		array.put("organizationname");
		query.put(Data.Query.COLUMNS, array);
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_mailconfigurations");
		return rows;
	}

	private static void addPart(HashMap schemeParts, Integer sequence, String abbr, String seprator) {
		if (abbr != null && abbr.length() != 0) {
			String existingVal = (String) schemeParts.get(sequence);
			if (existingVal == null) {
				schemeParts.put(sequence, abbr);
			} else {
				existingVal = existingVal + seprator + abbr;
				schemeParts.put(sequence, existingVal);
			}
		}
	}

	private static void updateLastNo(Object key, int lastNo) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_numbergenerationschemes");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		row.put("lastno", lastNo);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);

	}

	private static String getYear(Date date) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy");
		String s = format.format(date);
		return s;
	}

}
