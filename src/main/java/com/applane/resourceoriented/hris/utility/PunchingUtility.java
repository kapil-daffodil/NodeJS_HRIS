package com.applane.resourceoriented.hris.utility;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.service.mail.ApplaneMail;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;

public class PunchingUtility {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public static void updateLastPunchDateStatus(String inOutDate, Object branchId, String organizationName) throws JSONException {
		JSONArray lastUpdateDaterecord = getLastDateUpdate(inOutDate, branchId);
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "punchdatehistory");
		JSONObject row = new JSONObject();
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());
		Date todayDate = Translator.dateValue(TODAY_DATE);
		if (lastUpdateDaterecord != null) {
			row.put("__key__", lastUpdateDaterecord.getJSONObject(0).opt("__key__"));

		}
		if (lastUpdateDaterecord == null) {
			row.put("date", inOutDate);
			if (!organizationName.equals("girnarsoft.com")) {
				row.put("branchid", branchId);
			}
		}
		if (todayDate.equals(Translator.dateValue(inOutDate))) {
			row.put("datetime", TODAY_DATE);
		} else {
			row.put("datetime", inOutDate);
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static JSONArray getLastDateUpdate(String inOutDate, Object branchId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "punchdatehistory");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("datetime");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "branchid = " + branchId + " and date = '" + inOutDate + "'");
		JSONObject object = ResourceEngine.query(query);
		JSONArray employeeArray = object.getJSONArray("punchdatehistory");
		if (employeeArray.length() > 0) {
			return employeeArray;
		} else {
			return null;
		}
	}

	public static JSONArray getLastDateUpdateForBranch(Object branchId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "punchdatehistory");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("datetime");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "branchid = " + branchId);
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, "date");
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		orders.put(orderObject);
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.MAX_ROWS, 1);
		JSONObject object = ResourceEngine.query(query);
		JSONArray employeeArray = object.getJSONArray("punchdatehistory");
		if (employeeArray.length() > 0) {
			return employeeArray;
		} else {
			return null;
		}
	}

	public static JSONArray getEmployeeRecords(String cardNo, String branch, String organizationName) throws JSONException {
		String filter = "";
		if (!organizationName.equals("girnarsoft.com")) {
			filter = " AND subbranchid.name = '" + branch + "'";
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("name");
		array.put("officialemailid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "deviceid = '" + cardNo + "' and employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + filter);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees");
		return employeeArray;
	}

	public static Object getInOutStatusId(String inOutStatus) throws JSONException {
		ApplaneDatabaseEngine resourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "punchinoutstatus");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "name = '" + inOutStatus + "'");
		JSONObject object = new JSONObject();
		object = resourceEngine.query(query);
		JSONArray rows = object.getJSONArray("punchinoutstatus");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			Object inOutStatusId = rows.getJSONObject(0).opt("__key__");
			return inOutStatusId;
		}
		return null;
	}

	public static void insertPunchingRecord(String IODate, String inOutTime, Object inOutStatusId, Object employeeId, Object employeeUserId, String inOutGateName) {
		try {
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "employeecardpunchdata");
			JSONObject row = new JSONObject();
			row.put("employeeid", employeeId);
			row.put("inoutdate", IODate);
			row.put("inouttime", inOutTime);
			row.put("inoutstatusid", inOutStatusId == null ? JSONObject.NULL : inOutStatusId);
			row.put("employeeuserid", employeeUserId == null ? JSONObject.NULL : employeeUserId);
			row.put("doorname", inOutGateName);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setValueInSession(String organizationName, Object usreId, String emailid) throws JSONException {

		Object organizationId = getId("up_organizations", "organization", organizationName);

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_ID, usreId);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_EMAIL, emailid);

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID, organizationId);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION, organizationName);

		// CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID, 2);
		// CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION, "DaffodilCRM");
		// CurrentState.setCurrentVariable(CurrentSession.ENABLE_LOGS, true);

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION_ID, 1033);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION, "hris_services_a");

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPGROUP_ID, 1033);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPGROUP, "hris_services_a");

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_TIME_ZONE, -19800000);

	}

	public static Object getId(String tableName, String columnName, String value) throws JSONException {
		JSONArray array = new JSONArray();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, tableName);
		array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, columnName + "='" + value + "'");
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject object = ResourceEngine.query(query);
		Object id = null;
		JSONArray rows = object.getJSONArray(tableName);
		if (rows != null && rows.length() > 0) {
			id = rows.getJSONObject(0).get("__key__");
		}
		return id;
	}

	public static void sendMailToHr(String title, String hrName, String[] mailId, String branchName, String lastUpdatedDataTime, String logo, String adminMail) throws Exception {
		StringBuilder message = new StringBuilder();
		message.append("<table><tr><td><img src=\"").append(logo).append("\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>");
		message.append("<hr></hr><br></br>");
		message.append("Dear ").append(hrName).append("<BR /><BR />");
		message.append("Punch Client of your branch <b>").append(branchName).append("</b> not running.").append("<BR /><BR />");
		message.append("Last Run Status -->> <b>").append(lastUpdatedDataTime).append("</b><BR /><BR />");
		message.append("Thanks & Regards").append("<BR />");
		message.append("Applane");
		// int length = mailId.length;
		// String[] mailIDs = new String[length];
		// for (int counter = 0; counter < mailId.length; counter++) {
		// mailIDs[counter] = mailId[counter];
		// }
		sendMails(mailId, message.toString(), title, adminMail);
		// sendMails(new String[] { ApplicationConstants.OWNER }, message.toString(), title);
	}

	public static void sendMails(String[] toMailIds, String mailValue, String title, String adminMail) throws Exception {
		for (int counter = 0; counter < toMailIds.length; counter++) {
			ApplaneMail mailSender = new ApplaneMail();
			mailSender.setMessage(title, mailValue.toString(), true);
			mailSender.setTo(toMailIds[counter]);
			if (adminMail.length() > 0) {
				mailSender.setCc(ApplicationConstants.OWNER, ApplicationConstants.USER, adminMail);
			} else {
				mailSender.setCc(ApplicationConstants.OWNER, ApplicationConstants.USER);
			}
			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
		}

	}

	public static void sendMailsAccToProfitCenters(String[] toMailIds, String mailValue, String title, String[] adminMail) throws Exception {
		for (int counter = 0; counter < toMailIds.length; counter++) {
			ApplaneMail mailSender = new ApplaneMail();
			mailSender.setMessage(title, mailValue.toString(), true);
			mailSender.setTo(toMailIds[counter]);
			if (adminMail != null && adminMail.length > 0) {
				mailSender.setCc(adminMail);
			}
			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
		}

	}

	public static void sendMailsWithoutCC(String[] toMailIds, String mailValue, String title) throws Exception {
		for (int counter = 0; counter < toMailIds.length; counter++) {
			ApplaneMail mailSender = new ApplaneMail();
			mailSender.setMessage(title, mailValue.toString(), true);
			mailSender.setTo(toMailIds[counter]);
			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
		}

	}

	public static boolean checkDuplicate(String tableName, String columnName, String value, Object key) throws JSONException {
		JSONArray array = new JSONArray();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, tableName);
		array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		String extraFilter = "";
		if (key != null) {
			extraFilter = " and " + Updates.KEY + " != " + key;
		}
		query.put(Data.Query.FILTERS, columnName + "='" + value + "'" + extraFilter);
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray(tableName);
		if (rows != null && rows.length() > 0) {
			return true;
		}
		return false;
	}
}
