package com.applane.resourceoriented.hris.cabdelay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.HrisUserDefinedFunctions;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class CabDelayInformationBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

		String operationTyString = record.getOperationType();
		Object date = record.getValue("date");

		LogUtility.writeLog("After Update of CabDelayInformationBusinessLogic");

		Object employeeName = record.getValue("employee_id.name");

		Object fromTime_Time = record.getValue("from_time_time");
		Object fromTime_Meridiem = record.getValue("from_time_meridiem");
		String fromTime = fromTime_Time.toString() + " " + fromTime_Meridiem.toString();

		Object toTime_Time = record.getValue("to_time_time");
		Object toTime_Meridiem = record.getValue("to_time_meridiem");
		String toTime = toTime_Time.toString() + " " + toTime_Meridiem.toString();

		Object reportingToName = record.getValue("reportingto_id.name");
		Object reportingToOfficialMailId = record.getValue("reportingto_id.officialemailid");

		Object remarks = record.getValue(HrisKinds.CabDelayInformation.CAB_DELAY_REMARKS);

		LogUtility.writeLog("Reporting To Official Mail Id : [" + reportingToOfficialMailId + "]");

		if (operationTyString.equalsIgnoreCase(Updates.Types.INSERT)) {
			sendEmailToReporter(record, date, employeeName, fromTime, toTime, reportingToName, reportingToOfficialMailId, remarks);
		}

		if (operationTyString.equalsIgnoreCase(Updates.Types.UPDATE)) {
			sendMailToAdmin(record, date, employeeName, fromTime, toTime, reportingToName, remarks);
			updateAttendance(record, date);
		}
	}

	private void sendEmailToReporter(Record record, Object date, Object employeeName, String fromTime, String toTime, Object reportingToName, Object reportingToOfficialMailId, Object remarks) {
		try {
			StringBuilder sb = new StringBuilder();
			StringBuilder link = new StringBuilder();
			StringBuilder footer = new StringBuilder();
			sb.append("Hello ").append(reportingToName).append("<br /><br />");
			sb.append(employeeName).append(" has requested to approve the Cab Delay time.");
			sb.append(" Cab Delay information is as follows").append("<br />");
			sb.append("<ul>");
			sb.append("<li>");
			sb.append("<b>Date</b>: ").append(date);
			sb.append("</li>");
			sb.append("<li>");
			sb.append("<b>From Time</b>: ").append(fromTime);
			sb.append("</li>");
			sb.append("<li>");
			sb.append("<b>To Time</b>: ").append(toTime);
			sb.append("</li>");
			sb.append("<li>");
			sb.append("<b>Remarks</b>: ").append(remarks);
			sb.append("</li>");
			sb.append("</ul>");
			link.append(getLink(record));
			footer.append("<br />Regards<br /><br /><b>" + employeeName + "</b>");

			LogUtility.writeLog("Message : " + sb);

			String[] cabDelayNotificationCCEmployees = getCabDelayNotificationCCEmployees();
			try {
				ApplaneMail mailSender = new ApplaneMail();
				mailSender.setMessage("Cab Delay Information by " + employeeName, (sb.toString() + link.toString() + footer.toString()), true);
				mailSender.setTo(reportingToOfficialMailId.toString());
				mailSender.setCc(cabDelayNotificationCCEmployees);
				mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());

				if (cabDelayNotificationCCEmployees != null && cabDelayNotificationCCEmployees.length > 0) {
					PunchingUtility.sendMailsAccToProfitCenters(cabDelayNotificationCCEmployees, (sb.toString() + footer), ("Cab Delay Information by " + employeeName), null);
				}
			} catch (ApplaneMailException e) {
				throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
		}
	}

	private Object getAdminLink(Record record, String cabAdminId) throws Exception {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		messageContents.append("<table border='0' width='98%' align='center'> <tr valign='bottom'><td align='left' width='50%'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvecabrequest'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approvecabrequest'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(record.getKey()).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(cabAdminId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apby' value='admin'").append("/></ br>");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");

		messageContents.append("</td>");
		messageContents.append("<td align='left' width='50%'>");

		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvecabrequest'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approvecabrequest'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(record.getKey()).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(cabAdminId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apby' value='admin'").append("/></ br>");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("<INPUT TYPE='text' name='comments' style=\"width:100%;\"").append("' />");
		messageContents.append("</form>");
		messageContents.append("</td></tr>");
		messageContents.append("</table>");

		messageContents.append("<BR />");
		return messageContents.toString();
	}

	private Object getLink(Record record) throws Exception {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		// messageContents.append("Approve or Reject leave(s):").append("<BR />");
		messageContents.append("<table border='0' width='98%' align='center'> <tr valign='bottom'><td align='left' width='50%'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvecabrequest'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approvecabrequest'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(record.getKey()).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(record.getValue("reportingto_id")).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apby' value='tl'").append("/></ br>");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");

		messageContents.append("</td>");
		messageContents.append("<td align='left' width='50%'>");

		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvecabrequest'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approvecabrequest'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(record.getKey()).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(record.getValue("reportingto_id")).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apby' value='tl'").append("/></ br>");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("<INPUT TYPE='text' name='comments' style=\"width:100%;\"").append("' />");
		messageContents.append("</form>");
		messageContents.append("</td></tr>");
		messageContents.append("</table>");

		messageContents.append("<BR />");
		return messageContents.toString();
	}

	public Object getCurrentOrganization() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "up_organizations");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("organization");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + ")");
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray organizationArray = employeeObject.getJSONArray("up_organizations");
		if (organizationArray != null && organizationArray.length() > 0) {
			return organizationArray.getJSONObject(0).opt("organization");
		}
		return null;
	}

	private void sendMailToAdmin(Record record, Object date, Object employeeName, String fromTime, String toTime, Object reportingToName, Object remarks) {
		try {
			int oldStatus = Translator.integerValue(record.getOldValue(HrisKinds.CabDelayInformation.STATUS_ID));
			int newStatus = Translator.integerValue(record.getNewValue(HrisKinds.CabDelayInformation.STATUS_ID));

			if (oldStatus == HRISConstants.LEAVE_NEW && newStatus == HRISConstants.LEAVE_APPROVED) {

				String[] cabAdminNameAndOfficialEmailId = HrisUserDefinedFunctions.getCabAdminNameAndOfficialEmailId();

				String cabAdminName = cabAdminNameAndOfficialEmailId[0];
				String cabAdminOfficialEmailId = cabAdminNameAndOfficialEmailId[1];
				String cabAdminId = cabAdminNameAndOfficialEmailId[2];

				if (cabAdminOfficialEmailId != null && cabAdminOfficialEmailId.length() > 0) {

					StringBuilder sb = new StringBuilder();
					sb.append("Hello ").append(cabAdminName).append("<br /><br />");
					sb.append(reportingToName).append(" has approved the Cab Delay time.");
					sb.append(" Cab Delay information is as follows").append("<br />");
					sb.append("<ul>");
					sb.append("<li>");
					sb.append("<b>Employee Name</b>: ").append(employeeName);
					sb.append("</li>");
					sb.append("<li>");
					sb.append("<b>Date</b>: ").append(date);
					sb.append("</li>");
					sb.append("<li>");
					sb.append("<b>From Time</b>: ").append(fromTime);
					sb.append("</li>");
					sb.append("<li>");
					sb.append("<b>To Time</b>: ").append(toTime);
					sb.append("</li>");
					sb.append("<li>");
					sb.append("<b>Remarks</b>: ").append(remarks);
					sb.append("</li>");
					sb.append("<li>");
					sb.append(getAdminLink(record, cabAdminId));
					sb.append("</li>");
					sb.append("</ul>");
					sb.append("<br /><br />Regards,<br /><br /><b>" + "Applane Team" + "</b>");

					LogUtility.writeLog("Cab Delay Admin Message : " + sb);

					try {
						ApplaneMail mailSender = new ApplaneMail();
						mailSender.setMessage("Cab Delay Information Approved by " + reportingToName, sb.toString(), true);
						mailSender.setTo(cabAdminOfficialEmailId);
						mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
					} catch (ApplaneMailException e) {
						throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
		}
	}

	private void updateAttendance(Record record, Object date) {
		try {
			int oldStatus = Translator.integerValue(record.getOldValue(HrisKinds.CabDelayInformation.APPROVE_BY_ADMIN_STATUS_ID));
			int newStatus = Translator.integerValue(record.getNewValue(HrisKinds.CabDelayInformation.APPROVE_BY_ADMIN_STATUS_ID));
			if (oldStatus == HRISConstants.LEAVE_NEW && newStatus == HRISConstants.LEAVE_APPROVED) {
				Object employeeId = record.getValue("employee_id");
				Object attendanceTypeId = record.getValue("attendancetypeid");
				Object leaveTypeId = record.getValue("leavetypeid");
				Object paidStatusId = record.getValue("paidstatus");
				updateAttendance(employeeId, date, attendanceTypeId, leaveTypeId, paidStatusId);
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
		}
	}

	private void updateAttendance(Object employeeId, Object date, Object attendanceTypeId, Object leaveTypeId, Object paidStatusId) throws Exception {
		JSONArray employeeAttendance = getEmployeeAttendance(employeeId, date);
		JSONObject updates = new JSONObject();
		JSONObject row = new JSONObject();
		if (employeeAttendance != null && employeeAttendance.length() > 0) {
			row.put("__key__", employeeAttendance.getJSONObject(0).opt("__key__"));
		} else {
			row.put("employeeid", employeeId);
			row.put("attendancedate", date);
		}
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		row.put("attendancetypeid", attendanceTypeId);
		row.put("attendance_type_short_leave_id", JSONObject.NULL);
		row.put("attendance_status", 2);
		if (leaveTypeId != null) {
			row.put("leavetypeid", leaveTypeId);
		}
		if (paidStatusId != null) {
			row.put("paidstatus", paidStatusId);
		}
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private JSONArray getEmployeeAttendance(Object employeeId, Object date) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid=" + employeeId + " AND attendancedate='" + date + "'");
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray array = object.getJSONArray("employeeattendance");
		return array;
	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {

		String operationType = record.getOperationType();
		LogUtility.writeLog("On Before Update >> Operation Type : " + operationType);

		if (operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {

			int oldAdminStatus = Translator.integerValue(record.getOldValue(HrisKinds.CabDelayInformation.APPROVE_BY_ADMIN_STATUS_ID));
			int newAdminStatus = Translator.integerValue(record.getNewValue(HrisKinds.CabDelayInformation.APPROVE_BY_ADMIN_STATUS_ID));

			LogUtility.writeLog("Old Admin Status : " + oldAdminStatus + ", New Admin Status : " + newAdminStatus);

			if (oldAdminStatus != 0 && oldAdminStatus != HRISConstants.LEAVE_NEW && newAdminStatus != 0) {
				throw new BusinessLogicException("The admin status has already changed. You can't change it again.");
			}

			int oldStatus = Translator.integerValue(record.getOldValue(HrisKinds.CabDelayInformation.STATUS_ID));
			int newStatus = Translator.integerValue(record.getNewValue(HrisKinds.CabDelayInformation.STATUS_ID));

			LogUtility.writeLog("Old Status : " + oldStatus + ", New Status : " + newStatus);

			if (newStatus != 0) {

				if (oldAdminStatus == HRISConstants.LEAVE_APPROVED) {
					throw new BusinessLogicException("Admin has already approved it. You can't change it's status now.");
				}

				if (oldAdminStatus == HRISConstants.LEAVE_REJECTED) {
					throw new BusinessLogicException("Admin has already rejected it. You can't change it's status now.");
				}

				if (oldStatus != 0 && oldStatus != HRISConstants.LEAVE_NEW) {
					throw new BusinessLogicException("The status has already changed. You can't change it again.");
				}
			}
		}
	}

	private String[] getCabDelayNotificationCCEmployees() throws Exception {

		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put(new JSONObject().put("employee_cab_delay_notification_id", new JSONArray().put("officialemailid")));

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		query.put(Data.Query.COLUMNS, array);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray mailConfigurationJsonArray = object.optJSONArray("hris_mailconfigurations");

		LogUtility.writeLog("Mail Configuration Json Array : [" + mailConfigurationJsonArray + "]");

		StringBuilder sb = new StringBuilder();

		if (mailConfigurationJsonArray != null && mailConfigurationJsonArray.length() > 0) {

			JSONObject jsonObject = mailConfigurationJsonArray.optJSONObject(0);
			JSONArray jsonArray = jsonObject.optJSONArray("employee_cab_delay_notification_id");
			LogUtility.writeLog("jsonArray : [" + jsonArray + "]");

			JSONArray ccEmployees = jsonArray == null ? null : jsonArray;
			int ccEmployeesLength = (ccEmployees == null) ? 0 : ccEmployees.length();
			LogUtility.writeLog("ccEmployees : [" + ccEmployees + "], ccEmployeesLength : " + ccEmployeesLength);

			if (ccEmployeesLength > 0) {
				for (int i = 0; i < ccEmployeesLength; i++) {
					String ccEmployeeOfficialMailId = ccEmployees.optJSONObject(i).optString("officialemailid", null);
					if (ccEmployeeOfficialMailId != null) {
						sb.append(ccEmployeeOfficialMailId).append(",");
					}
					LogUtility.writeLog("Cab Delay Notification Employee Official Mail Id : [" + ccEmployeeOfficialMailId + "]");
				}
			}
		}

		LogUtility.writeLog("CC Employee Official Mail Ids : " + sb);

		String[] ccEmployeeOfficialMailIds = null;
		if (sb.length() > 0) {
			ccEmployeeOfficialMailIds = sb.toString().split(",");
		}

		return ccEmployeeOfficialMailIds;
	}

	public boolean getUpdateAdminStatusViewActionVisibility() {
		try {
			String currentUserEmailId = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL));
			String cabAdminOfficialEmailId = HrisUserDefinedFunctions.getCabAdminOfficialEmailId();

			LogUtility.writeLog("Current User Email Id : " + currentUserEmailId);
			LogUtility.writeLog("Cab Admin Official Email Id : " + cabAdminOfficialEmailId);

			return (cabAdminOfficialEmailId == null) ? false : cabAdminOfficialEmailId.equals(currentUserEmailId);

		} catch (JSONException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(CabDelayInformationBusinessLogic.class.getName(), e));
		}
	}

	public Object getCabAdminEmployeeId() {
		try {
			Object cabAdminEmployeeId = HrisUserDefinedFunctions.getCabAdminEmployeeId();
			LogUtility.writeLog("Cab Admin Employee Id : " + cabAdminEmployeeId);

			return cabAdminEmployeeId;

		} catch (JSONException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(CabDelayInformationBusinessLogic.class.getName(), e));
		}
	}

	public boolean showAddCabDelayInfoButton() {
		try {
			String currentUserEmailId = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL));

			JSONArray columns = new JSONArray();
			columns.put(HrisKinds.EmployeesAdditionalInformation.CAB_STATUS);

			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES_ADDITIONAL_INFORMATION);
			query.put(Data.Query.COLUMNS, columns);
			query.put(Data.Query.FILTERS, HrisKinds.EmployeesAdditionalInformation.EMPLOYEE_OFFICIAL_EMAIL_ID + "='" + currentUserEmailId + "'");

			JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
			JSONArray resultJSONArray = result.optJSONArray(HrisKinds.EMPLOYEES_ADDITIONAL_INFORMATION);

			int noOfResult = (resultJSONArray == null) ? 0 : resultJSONArray.length();

			if (noOfResult > 0) {

				JSONObject jsonObject = resultJSONArray.optJSONObject(0);
				String cabStatus = jsonObject.optString(HrisKinds.EmployeesAdditionalInformation.CAB_STATUS);

				LogUtility.writeLog("showAddCabDelayInfoButton >> Cab Status : " + cabStatus);

				if (cabStatus != null && cabStatus.equalsIgnoreCase(HrisKinds.EmployeesAdditionalInformation.CabStatus.CAB)) {
					return true;
				}
			}

			return false;

		} catch (JSONException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(CabDelayInformationBusinessLogic.class.getName(), e));
		}
	}

	public Object getHrisShiftRequestForAdminFilter() {
		try {
			String currentUserEmailId = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL));
			String cabAdminOfficialEmailId = HrisUserDefinedFunctions.getCabAdminOfficialEmailId();

			LogUtility.writeLog("Current User Email Id : " + currentUserEmailId);
			LogUtility.writeLog("Cab Admin Official Email Id : " + cabAdminOfficialEmailId);

			boolean isCabAdmin = (cabAdminOfficialEmailId == null) ? false : cabAdminOfficialEmailId.equals(currentUserEmailId);

			LogUtility.writeLog("Is Cab Admin : " + isCabAdmin);

			if (isCabAdmin) {
				return 2;
			} else {
				return -1;
			}
		} catch (JSONException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(CabDelayInformationBusinessLogic.class.getName(), e));
		}
	}

	public static void main(String[] args) {

		StringBuilder sb = new StringBuilder();

		sb.append("Hello ").append("Amit  Singh").append("<br /><br />");
		sb.append("Kapil  Dalal").append(" has requested to approve the Cab Delay time.");
		sb.append("Cab Delay information is as follows").append("<br />");
		sb.append("<ul>");
		sb.append("<li>");
		sb.append("<b>Date</b>: ").append("2013-07-23");
		sb.append("</li>");
		sb.append("<li>");
		sb.append("<b>From Time</b>: ").append("09:00 AM");
		sb.append("</li>");
		sb.append("<li>");
		sb.append("<b>To Time</b>: ").append("10:00 AM");
		sb.append("</li>");
		sb.append("<li>");
		sb.append("<b>Reason</b>: ").append("Cab");
		sb.append("</li>");
		sb.append("</ul>");
		sb.append("<br /><br />Regards,<br /><br /><b>(Applane Team)</b>");

		System.out.println(sb.toString());
	}
}
