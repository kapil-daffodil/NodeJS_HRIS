package com.applane.resourceoriented.hris.reports;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.user.service.UserUtility;

public class DepartmentStrengthNotificationMail {

	private static final String HRIS_DEPARTMENTS = "hris_departments";

	private static final String HRIS_MAIL_CONFIGURATIONS = "hris_mailconfigurations";

	private static final String KEY = "__key__";

	private static final String DEPARTMENT_NAME = "name";

	private static final String MINIMUM_STRENGTH = "minimum_strength";

	private static final String EMPLOYEE_DEPARTMENT_STRENGTH_NOTIFICATION_ID = "emp_dpt_strength_notif_id";

	public static void mailDepartmentStrengthNotification() {
		try {
			JSONArray departmentDetails = getDepartmentDetails();
			LogUtility.writeLog("Department Details : " + departmentDetails);

			if (departmentDetails != null && departmentDetails.length() > 0) {

				int noOfDepartments = departmentDetails.length();

				boolean isSendMail = false;
				StringBuilder sb = new StringBuilder();

				for (int i = 0; i < noOfDepartments; i++) {

					JSONObject jsonObject = departmentDetails.optJSONObject(i);

					Object departmentId = jsonObject.opt(KEY);
					String departmentName = jsonObject.optString(DEPARTMENT_NAME);
					int minimumStrength = jsonObject.optInt(MINIMUM_STRENGTH);

					if (minimumStrength > 0) {
						int actualStrength = getDepartmentStrength(departmentId);
						if (minimumStrength > actualStrength) {
							addDepartmentDetailInMail(sb, departmentName, minimumStrength, actualStrength);
							isSendMail = true;
						}
					}
				}

				if (isSendMail) {
					sb.append("</table><br /><br />Regards,<br /><br /><b>Applane Team</b>");
					String[] departmentStrengthNotificationMailEmployeeIds = getDepartmentStrengthNotificationMailEmployeeIds();
					sendDepartmentStrengthNotificationMail(sb, departmentStrengthNotificationMailEmployeeIds);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(DepartmentStrengthNotificationMail.class.getName(), e));
		}
	}

	private static JSONArray getDepartmentDetails() throws JSONException {

		JSONArray columnArray = new JSONArray();
		columnArray.put(KEY);
		columnArray.put(DEPARTMENT_NAME);
		columnArray.put(MINIMUM_STRENGTH);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRIS_DEPARTMENTS);
		query.put(Data.Query.COLUMNS, columnArray);

		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRIS_DEPARTMENTS);
	}

	private static int getDepartmentStrength(Object departmentId) throws Exception {

		LogUtility.writeLog("Department Id : [" + departmentId + "]");

		JSONArray columnArray = new JSONArray();
		columnArray.put(new JSONObject().put("expression", "__key__").put("aggregate", "count").put("alias", "count"));

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "departmentid" + " = " + departmentId + " AND employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE);

		int departmentStrength = 0;

		JSONArray jsonArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);

		LogUtility.writeLog("Department Strength Json Array : " + jsonArray);

		if (jsonArray != null && jsonArray.length() > 0) {
			JSONObject jsonObject = jsonArray.optJSONObject(0);
			departmentStrength = jsonObject.optInt("count");
		}

		return departmentStrength;
	}

	private static String[] getDepartmentStrengthNotificationMailEmployeeIds() throws Exception {

		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put(new JSONObject().put(EMPLOYEE_DEPARTMENT_STRENGTH_NOTIFICATION_ID, new JSONArray().put("officialemailid")));

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRIS_MAIL_CONFIGURATIONS);
		query.put(Data.Query.COLUMNS, array);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray mailConfigurationJsonArray = object.optJSONArray(HRIS_MAIL_CONFIGURATIONS);

		LogUtility.writeLog("Mail Configuration Json Array : [" + mailConfigurationJsonArray + "]");

		StringBuilder sb = new StringBuilder();

		if (mailConfigurationJsonArray != null && mailConfigurationJsonArray.length() > 0) {

			JSONObject jsonObject = mailConfigurationJsonArray.getJSONObject(0);
			JSONArray jsonArray = jsonObject.optJSONArray(EMPLOYEE_DEPARTMENT_STRENGTH_NOTIFICATION_ID);
			LogUtility.writeLog("jsonArray : [" + jsonArray + "]");

			JSONArray employees = jsonArray == null ? null : jsonArray;
			int employeesLength = (employees == null) ? 0 : employees.length();

			LogUtility.writeLog("Employee Ids : [" + employees + "], Employee Ids Length : " + employeesLength);

			if (employeesLength > 0) {
				for (int i = 0; i < employeesLength; i++) {
					String ccEmployeeOfficialMailId = employees.getJSONObject(i).optString("officialemailid", null);
					if (ccEmployeeOfficialMailId != null) {
						sb.append(ccEmployeeOfficialMailId).append(",");
					}
				}
			}
		}

		LogUtility.writeLog("Department Strength Notification Employee Official Mail Ids : " + sb);

		String[] employeeOfficialMailIds = null;
		if (sb.length() > 0) {
			employeeOfficialMailIds = sb.toString().split(",");
		}

		return employeeOfficialMailIds;
	}

	private static void addDepartmentDetailInMail(StringBuilder sb, String departmentName, int minimumStrength, int actualStrength) {

		if (sb.toString().length() <= 0) {
			sb.append("Hello Sir").append("<br /><br />");
			sb.append("Department strength information is as follows").append("<br /><br />");
			sb.append("<table cellspacing=\"0\" cellpadding=\"0\" style=\"border: 1px solid black;\">");
			sb.append("<tr>");
			sb.append("<th width=\"160px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Department Name");
			sb.append("</th>");
			sb.append("<th width=\"140px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Minimum Strength");
			sb.append("</th>");
			sb.append("<th width=\"140px\" style=\"border-bottom: 1px solid black;\">");
			sb.append("Actual Strength");
			sb.append("</th>");
			sb.append("</tr>");
		}

		sb.append("<tr>");
		sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
		sb.append(departmentName);
		sb.append("</td>");
		sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
		sb.append(minimumStrength);
		sb.append("</td>");
		sb.append("<td align=\"center\">");
		sb.append(actualStrength);
		sb.append("</td>");
		sb.append("</tr>");
	}

	private static void sendDepartmentStrengthNotificationMail(StringBuilder sb, String[] departmentStrengthNotificationMailEmployeeIds) {

		LogUtility.writeLog("Department Strength Notification Mail Message : " + sb);

		try {
			if (departmentStrengthNotificationMailEmployeeIds != null && departmentStrengthNotificationMailEmployeeIds.length > 0) {
				ApplaneMail mailSender = new ApplaneMail();
				mailSender.setMessage("Department Strength Information", sb.toString(), true);
				mailSender.setTo(departmentStrengthNotificationMailEmployeeIds);
				mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
			}
		} catch (ApplaneMailException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(DepartmentStrengthNotificationMail.class.getName(), e));
		}
	}

	public static void main(String[] args) {

		StringBuilder sb = new StringBuilder();

		addDepartmentDetailInMail(sb, "Information Technology", 32, 31);
		addDepartmentDetailInMail(sb, "Accounts", 20, 15);

		sb.append("</table><br /><br />Regards,<br /><br /><b>Applane Team</b>");

		System.out.println(sb);
	}
}
