package com.applane.resourceoriented.hris.utility;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.leave.LeaveMail;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.utility.constants.ShortLeaveKinds;
import com.applane.resourceoriented.hris.utility.constants.ShortLeaveKinds.ShortLeaveInAMonth;
import com.applane.service.mail.ApplaneMail;
import com.applane.user.service.UserUtility;

public class ShortLeaveUtility {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public JSONArray getShortLeavesInAMonth(Object employeeId, Object requestedDateObject) {
		try {
			if (employeeId != null && requestedDateObject != null) {
				Calendar cal = Calendar.getInstance();
				Date leaveRequestedDate = Translator.dateValue(requestedDateObject);
				cal.setTime(leaveRequestedDate);
				String monthName = EmployeeSalaryGenerationServlet.getMonthName(cal.get(Calendar.MONTH) + 1);
				String yearName = "" + cal.get(Calendar.YEAR);
				Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
				Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
				String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
				String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
				JSONArray shortLeaveArrayInAMonthRecords = getShortLeaveArrayInAMonthRecords(employeeId, monthFirstDateInString, monthLastDateInString);
				JSONArray shortLeaveArrayInAMonth = getShortLeaveArrayInAMonth(shortLeaveArrayInAMonthRecords, monthFirstDateInString, monthLastDateInString);
				return shortLeaveArrayInAMonth;
			} else {
				throw new BusinessLogicException("Employee and Requested Date can not be NULL employee > " + employeeId + " < request date > " + requestedDateObject);
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			LogUtility.writeLog("ShortLeaveUtility getShortLeavesInAMonth Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			// throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			throw new BusinessLogicException("Some error occured while fetching your leaves in current month, Please try after some time");
		}
	}

	private JSONArray getShortLeaveArrayInAMonth(JSONArray shortLeaveArrayInAMonthRecords, String monthFirstDateInString, String monthLastDateInString) throws JSONException {
		HashMap<Integer, Object[]> leaveCountMap = new HashMap<Integer, Object[]>();
		for (int counter = 0; counter < shortLeaveArrayInAMonthRecords.length(); counter++) {
			int leaveTypeId = Translator.integerValue(shortLeaveArrayInAMonthRecords.getJSONObject(counter).opt(ShortLeaveKinds.SHORT_LEAVE_TYPE_ID));
			Object leaveTypeName = shortLeaveArrayInAMonthRecords.getJSONObject(counter).opt(ShortLeaveKinds.SHORT_LEAVE_TYPE_ID + ".name");
			int leaveCount = 1;
			Object[] details = new Object[2];
			details[0] = leaveTypeName;
			details[1] = leaveCount;
			if (leaveCountMap.containsKey(leaveTypeId)) {
				details = leaveCountMap.get(leaveTypeId);
				leaveCount += Translator.integerValue(details[1]);
				details[1] = leaveCount;
			}
			leaveCountMap.put(leaveTypeId, details);
		}
		JSONArray leaveCountArray = new JSONArray();
		JSONObject leaveObject = null;
		JSONObject leaveCountObject = null;
		for (Integer leaveTypeId : leaveCountMap.keySet()) {
			leaveCountObject = new JSONObject();
			leaveObject = new JSONObject();
			Object[] details = leaveCountMap.get(leaveTypeId);
			Object leaveName = details[0];
			Object leaveCount = details[1];
			leaveCountObject.put(Updates.KEY, leaveTypeId);
			leaveCountObject.put("name", leaveName);
			leaveObject.put(ShortLeaveInAMonth.LEAVE_TYPE_ID, leaveCountObject);
			leaveObject.put(ShortLeaveInAMonth.FROM_DATE, monthFirstDateInString);
			leaveObject.put(ShortLeaveInAMonth.TO_DATE, monthLastDateInString);
			leaveObject.put(ShortLeaveInAMonth.NO_OF_LEAVES, leaveCount);
			leaveCountArray.put(leaveObject);
		}
		return leaveCountArray;
	}

	private JSONArray getShortLeaveArrayInAMonthRecords(Object employeeId, String monthFirstDateInString, String monthLastDateInString) throws Exception {

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, ShortLeaveKinds.SHORT_LEAVE_RESOURCE);
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_TYPE_ID);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_TYPE_ID + ".name");
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_DATE);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_TIME);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_MERIDIEM);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_TIME);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_MERIDIEM);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + " = " + employeeId + " and " + ShortLeaveKinds.SHORT_LEAVE_DATE + " >= '" + monthFirstDateInString + "' and " + ShortLeaveKinds.SHORT_LEAVE_DATE + " <= '" + monthLastDateInString + "' and " + ShortLeaveKinds.SHORT_LEAVE_STATUS_ID + "=" + HRISConstants.LEAVE_APPROVED);
		JSONObject object = ResourceEngine.query(query);
		JSONArray shortLeaveArray = object.getJSONArray(ShortLeaveKinds.SHORT_LEAVE_RESOURCE);
		return shortLeaveArray;

	}

	public static void sendLeaveEmail(Record record) throws Exception {
		Object employeeId = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID);
		Object employeeCode = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".employeecode");
		int genderId = Translator.integerValue(record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".genderid"));
		String branchName = Translator.stringValue(record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".branchid.name"));
		// Object designationName = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".designationid.name");
		Object employeeName = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".name");
		// Object reportingHeadName = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".reportingtoid.name");
		Object userEmail = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".officialemailid");
		Object directReportingId = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID);
		Object directReportingToEmail = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".reportingtoid.officialemailid");
		Object shortLeaveDate = record.getValue(ShortLeaveKinds.SHORT_LEAVE_DATE);

		Calendar cal = Calendar.getInstance();
		Date leaveRequestedDate = Translator.dateValue(shortLeaveDate);
		cal.setTime(leaveRequestedDate);
		cal.add(Calendar.MONTH, -3);
		String monthName = EmployeeSalaryGenerationServlet.getMonthName(cal.get(Calendar.MONTH) + 1);
		String yearName = "" + cal.get(Calendar.YEAR);
		Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(leaveRequestedDate);
		String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
		String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);

		JSONArray indirectReportingToArray = getIndirectReportingToArray(employeeId);

		Object leaveCode = record.getValue(ShortLeaveKinds.SHORT_LEAVE_CODE);
		Object leaveDate = record.getValue(ShortLeaveKinds.SHORT_LEAVE_DATE);
		Object reason = record.getValue(ShortLeaveKinds.SHORT_LEAVE_REASON);
		Object fromTime = record.getValue(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_TIME);
		Object fromTimeMeridiem = record.getValue(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_MERIDIEM);
		Object toTime = record.getValue(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_TIME);
		Object toTimeMeridiem = record.getValue(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_MERIDIEM);
		JSONArray mailConfigurationSetup = getMailConfigurationSetup();
		String logo = "";
		if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
			Object organizationlogo = mailConfigurationSetup.getJSONObject(0).opt("organizationlogo");
			if (organizationlogo != null) {
				logo = new EmployeeSalarySheetReport().getLogo(organizationlogo);
			}
		}
		String gender = "";
		String gender1 = "";
		if (genderId == 1) {
			gender = "Mr. ";
			gender1 = "His ";
		} else if (genderId == 2) {
			gender = "Ms. ";
			gender1 = "Her ";
		} else {
			gender = "Mr./Ms. ";
			gender1 = "His/Her ";
		}
		// JSONArray leaveTakenInAMonth = new JSONArray();
		// if (record.getValue("shortleaveinamonth") != null && record.getValue("shortleaveinamonth") instanceof JSONArray) {
		// leaveTakenInAMonth = (JSONArray) record.getValue("shortleaveinamonth");
		// }
		String title = "Short Leave Request by " + employeeName;
		StringBuilder messageForMail = new StringBuilder();
		messageForMail.append("<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>");
		messageForMail.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>").append("<BR /><BR />");

		messageForMail.append("Dear Sir / Madam,").append("<BR /><BR />");// .append(reportingHeadName).append("<BR /><BR />");
		messageForMail.append(gender).append(" ").append(employeeName).append(" (Employee Code ").append(employeeCode).append(") working in ").append(branchName).append(" has requested for Short Leave. Detail as given below :").append("<BR /><BR />");

		messageForMail.append("<table border='0' width='99%'>");

		messageForMail.append("<tr><td colspan='2'><b>Leave Code : </b>").append(" ").append(leaveCode.toString()).append("</td></tr>");
		messageForMail.append("<tr><td colspan='2'><b>Leave Date : </b>").append(" ").append(leaveDate.toString()).append("</td></tr>");
		messageForMail.append("<tr><td width='50%'><b>From Time  : </b>").append(fromTime.toString()).append(" ").append(fromTimeMeridiem);
		messageForMail.append("<td width='50%'><b>To Time    : </b>").append(toTime).append(" ").append(toTimeMeridiem).append("</td></tr>");
		messageForMail.append("<tr><td colspan='2'><b>Reason     : </b>").append(reason.toString()).append("</td></tr>");

		messageForMail.append("</table>");
		messageForMail.append("<BR />");
		messageForMail.append(gender1).append(" Previous Leave details are shown below <BR />");

		// if (leaveTakenInAMonth != null && leaveTakenInAMonth.length() > 0) {
		// Object fromDate = leaveTakenInAMonth.getJSONObject(0).opt("fromdate");
		// Object toDate = leaveTakenInAMonth.getJSONObject(0).opt("todate");
		// String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		// Date todayDate = DataTypeUtilities.checkDateFormat(TODAY_DATE);
		// if (todayDate.before(DataTypeUtilities.checkDateFormat(toDate))) {
		// toDate = TODAY_DATE;
		// }
		JSONArray shortLeaveArrayInAMonthRecords = new ShortLeaveUtility().getShortLeaveArrayInAMonthRecords(employeeId, "" + monthFirstDateInString, "" + monthLastDateInString);
		if (shortLeaveArrayInAMonthRecords != null && shortLeaveArrayInAMonthRecords.length() > 0) {

			messageForMail.append("<b>Leave Taken  : </b>").append(monthFirstDateInString).append(" to ").append(monthLastDateInString).append(" List Given Below<BR />");
			for (int counter = 0; counter < shortLeaveArrayInAMonthRecords.length(); counter++) {
				JSONObject details = shortLeaveArrayInAMonthRecords.getJSONObject(counter);
				Object leaveTypeName = details.opt(ShortLeaveKinds.SHORT_LEAVE_TYPE_ID + ".name");
				Object date = details.opt(ShortLeaveKinds.SHORT_LEAVE_DATE);
				Object fromTimeTaken = details.opt(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_TIME);
				Object fromTimeTakenMed = details.opt(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_MERIDIEM);
				Object toTimeTaken = details.opt(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_TIME);
				Object toTimeTakenMed = details.opt(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_MERIDIEM);
				messageForMail.append("<b>").append(leaveTypeName).append("  : </b>").append(date).append(" -- ").append(fromTimeTaken).append(" ").append(fromTimeTakenMed).append(" to ").append(toTimeTaken).append(" ").append(toTimeTakenMed).append("<BR />");
			}
		} else {
			messageForMail.append("No Short Leaves.");
		}
		messageForMail.append("<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
		String approveLinkButtons = new ShortLeaveUtility().getLinkToApproveByMail(record.getValue(Updates.KEY), directReportingId);
		sendMails(new String[] { "" + userEmail }, messageForMail.toString(), title, userEmail, null, "", "", true);
		sendMails(new String[] { directReportingToEmail.toString() }, messageForMail.toString(), title, "", indirectReportingToArray, "", approveLinkButtons, true);
	}

	private static JSONArray getIndirectReportingToArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		columnArray.put("indirectreportingto.officialemailid");
		query.put(Data.Query.RESOURCE, "hris_employees__indirectreportingto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "hris_employees = " + employeeId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_employees__indirectreportingto");
	}

	private String getLinkToApproveByMail(Object key, Object approverId) throws JSONException {
		StringBuffer messageContents = new StringBuffer();
		String currentOrganization = Translator.stringValue(new LeaveMail().getCurrentOrganization());
		messageContents.append("<BR />");
		// messageContents.append("Approve or Reject leave(s):").append("<BR />");
		messageContents.append("<table border='0' width='50%'> <tr><td>");
		messageContents.append("<form action='http://applanecrmhrd.appspot.com/escape/approveshortleave'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(approverId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='unpaid' VALUE='").append(HRISApplicationConstants.LeavePaymentType.PAID).append("' />");
		// messageContents.append("<input type='submit' style=\"font-face: 'Comic Sans MS'; font-size: 14px; color: black; background-color: #AFEEEE; border: 1.3pt ridge #5F9EA0;padding: 3px;\" value='  Approve(Paid)  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069;\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td><td>");
		messageContents.append("<form action='http://applanecrmhrd.appspot.com/escape/approveshortleave' method='GET'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(approverId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");
		// messageContents.append("<input type='submit' style=\"font-face: 'Comic Sans MS'; font-size: 14px; color: black; background-color: #AFEEEE; border: 1.3pt ridge #5F9EA0;padding: 3px;\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069;\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

		// messageContents.append("<a href='http://apps.applane.com/escape/approveleave?lr=").append(key).append("&orn=").append(currentOrganization).append("&apid=").append(approverId).append("'>").append("http://apps.applane.com/Approve_Leaves").append("</a>");
		messageContents.append("<BR />");
		return messageContents.toString();
	}

	public static void sendLeaveEmailOnUpdation(Record record, String leaveStatus) throws Exception {
		JSONArray mailConfigurationSetup = getMailConfigurationSetup();
		String logo = "";
		if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
			Object organizationlogo = mailConfigurationSetup.getJSONObject(0).opt("organizationlogo");
			if (organizationlogo != null) {
				logo = new EmployeeSalarySheetReport().getLogo(organizationlogo);
			}
		}
		Object employeeId = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID);
		Object branchId = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".branchid");
		Object userName = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".name");
		// Object reportingHeadName = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".reportingtoid.name");
		Object userEmail = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".officialemailid");
		Object directReportingToEmail = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".reportingtoid.officialemailid");
		JSONArray indirectReportingToArray = getIndirectReportingToArray(employeeId);
		Object leaveCode = record.getValue(ShortLeaveKinds.SHORT_LEAVE_CODE);
		Object leaveDate = record.getValue(ShortLeaveKinds.SHORT_LEAVE_DATE);
		Object reason = record.getValue(ShortLeaveKinds.SHORT_LEAVE_REASON);
		Object fromTime = record.getValue(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_TIME);
		Object fromTimeMeridiem = record.getValue(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_MERIDIEM);
		Object toTime = record.getValue(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_TIME);
		Object toTimeMeridiem = record.getValue(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_MERIDIEM);

		String title = "Short Leave " + leaveStatus;
		StringBuilder messageForMail = new StringBuilder();
		messageForMail.append("<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>");
		messageForMail.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>").append("<BR /><BR />");
		messageForMail.append("Dear ").append(userName).append("<BR /><BR />");
		messageForMail.append("Your short leave request detail as per below has been ").append(leaveStatus).append("<BR /><BR />");

		messageForMail.append("<table border='0' width='99%'>");
		messageForMail.append("<tr><td colspan='2'><b>Leave Code : </b>").append(" ").append(leaveCode.toString()).append("</td></tr>");
		messageForMail.append("<tr><td colspan='2'><b>Leave Date : </b>").append(" ").append(leaveDate.toString()).append("</td></tr>");
		messageForMail.append("<tr><td width='50%'><b>From Time  : </b>").append(fromTime.toString()).append(" ").append(fromTimeMeridiem).append("</td>");
		messageForMail.append("<td width='50%'><b>To Time    : </b>").append(toTime).append(" ").append(toTimeMeridiem).append("</td></tr>");
		messageForMail.append("<tr><td colspan='2'><b>Reason     : </b>").append(reason.toString()).append("</td></tr>");
		messageForMail.append("</table>");

		JSONArray branches = EmployeeSalarySheetReport.getHrMailIdDetail("branchid = " + branchId);
		String hrEmailId = "";
		if (branches != null && branches.length() > 0) {
			hrEmailId = branches.getJSONObject(0).optString("employeeid.officialemailid");
		}
		messageForMail.append("<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
		sendMails(new String[] { userEmail.toString() }, messageForMail.toString(), title, directReportingToEmail, indirectReportingToArray, hrEmailId, "", false);
	}

	public static void sendMails(String[] toMailIds, String mailValue, String title, Object userEmail, JSONArray inDirectReportingToEmail, String hrEmailId, String approveLinkButtons, boolean onInsert) throws Exception {
		for (int counter = 0; counter < toMailIds.length; counter++) {
			ApplaneMail mailSender = new ApplaneMail();
			if (approveLinkButtons.length() > 0) {
				mailSender.setMessage(title, (mailValue.toString() + approveLinkButtons), true);
			} else {
				mailSender.setMessage(title, (mailValue.toString()), true);
			}
			mailSender.setTo(toMailIds[counter]);
			if (!("" + userEmail).equals(toMailIds[counter]) || !onInsert) {

				int size = 0;
				if (inDirectReportingToEmail != null) {
					size += inDirectReportingToEmail.length();
					// size++;
				}
				if (hrEmailId.length() > 0) {
					size++;
				}
				String[] mailIdes = new String[size];
				int i = 0;
				if (size > 0 && inDirectReportingToEmail != null) {
					for (i = 0; i < inDirectReportingToEmail.length(); i++) {
						mailIdes[i] = "" + inDirectReportingToEmail.getJSONObject(i).opt("indirectreportingto.officialemailid");
					}

				}
				// if (size > 0 && inDirectReportingToEmail == null) {
				if (i == 0 && size > 0) {
					mailIdes[0] = hrEmailId;
				} else if (i > 0 && hrEmailId.length() > 0) {
					mailIdes[i] = hrEmailId;
				}
				for (int co = 0; co < mailIdes.length; co++) {
				}
				if (mailIdes.length > 0) {
					mailSender.setCc(mailIdes);
				}
			}
			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
		}
	}

	public static JSONArray getMailConfigurationSetup() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("organizationlogo");
		columnArray.put("organizationname");
		columnArray.put("address");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_mailconfigurations");
		return employeeArray;
	}
}
