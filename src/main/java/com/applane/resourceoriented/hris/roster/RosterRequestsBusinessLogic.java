/**
 * 
 */
package com.applane.resourceoriented.hris.roster;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.HrisUserDefinedFunctions;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class RosterRequestsBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

		LogUtility.writeLog("onAfterUpdate of RosterRequestsBusinessLogic");

		String operationTyString = record.getOperationType();

		Object employeeName = record.getValue(HrisKinds.RosterRequests.EMPLOYEE_NAME);
		Object employeeCode = record.getValue(HrisKinds.RosterRequests.EMPLOYEE_CODE);
		Object employee = employeeName + "," + employeeCode;

		Object date = record.getValue(HrisKinds.RosterRequests.DATE);
		String requestType = Translator.stringValue(record.getValue(HrisKinds.RosterRequests.REQUEST_TYPE));
		Object reason = record.getValue(HrisKinds.RosterRequests.REASON);

		Object approverName = record.getValue(HrisKinds.RosterRequests.APPROVER_NAME);
		Object approverOfficialEmailId = record.getValue(HrisKinds.RosterRequests.APPROVER_OFFICIAL_EMAIL_ID);

		LogUtility.writeLog("Approver Official Mail Id : [" + approverOfficialEmailId + "]");

		if (operationTyString.equalsIgnoreCase(Updates.Types.INSERT)) {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("Hello ").append(approverName).append("<br /><br />");
				sb.append("<b>").append(employee).append("</b> has requested to approve the Roster Request. Roster request information is as follows");
				sb.append("<br />");
				sb.append("<ul>");
				sb.append("<li>");
				sb.append("<b>Date</b>: ").append(date);
				sb.append("</li>");
				sb.append("<li>");
				sb.append("<b>Request Type</b>: ").append(requestType);
				sb.append("</li>");
				sb.append("<li>");
				sb.append("<b>Reason</b>: ").append(reason);
				sb.append("</li>");
				sb.append("</ul>");
				sb.append("<br />Regards<br /><br /><b>" + employeeName + "</b>");

				LogUtility.writeLog("Message : " + sb);

				try {
					ApplaneMail mailSender = new ApplaneMail();
					mailSender.setMessage("Roster request by " + employee, sb.toString(), true);
					mailSender.setTo(approverOfficialEmailId.toString());
					mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
				} catch (ApplaneMailException e) {
					throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
				}
			} catch (Exception e) {
				throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
			}
		}

		if (operationTyString.equalsIgnoreCase(Updates.Types.UPDATE)) {
			updateAttendance(record, date, requestType);
		}
	}

	private void updateAttendance(Record record, Object date, String requestType) {
		try {
			int oldStatus = Translator.integerValue(record.getOldValue(HrisKinds.RosterRequests.STATUS_ID));
			int newStatus = Translator.integerValue(record.getNewValue(HrisKinds.RosterRequests.STATUS_ID));

			if (oldStatus == HRISConstants.LEAVE_NEW && newStatus == HRISConstants.LEAVE_APPROVED) {
				Object employeeId = record.getValue(HrisKinds.RosterRequests.EMPLOYEE_ID);
				updateAttendance(employeeId, date, requestType);
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
		}
	}

	private void updateAttendance(Object employeeId, Object date, String requestType) throws Exception {

		LogUtility.writeLog(getClass().getSimpleName() + " >> updateAttendance >> Employee Id : " + employeeId + ", date : " + date + ", Request Type : " + requestType);

		JSONArray employeeAttendance = HrisUserDefinedFunctions.getEmployeeAttendance(employeeId, date);

		JSONObject row = new JSONObject();

		if (employeeAttendance != null && employeeAttendance.length() > 0) {
			row.put(Data.Query.KEY, employeeAttendance.getJSONObject(0).opt(Data.Query.KEY));
		} else {
			row.put(HrisKinds.EmployeeAttendance.EMPLOYEE_ID, employeeId);
			row.put(HrisKinds.EmployeeAttendance.ATTENDANCE_DATE, date);
		}

		int attendanceTypeId = requestType.equals(HrisKinds.RosterRequests.RequestType.ROSTER_OFF) ? HRISApplicationConstants.ATTENDANCE_OFF : HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME;
		row.put(HrisKinds.EmployeeAttendance.ATTENDANCE_TYPE_ID, attendanceTypeId);

		row.put(HrisKinds.EmployeeAttendance.ATTENDANCE_STATUS, HRISConstants.LEAVE_APPROVED);
		row.put(HrisKinds.EmployeeAttendance.TEMP, HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEE_ATTENDANCE);
		updates.put(Data.Update.UPDATES, row);

		LogUtility.writeLog(getClass().getSimpleName() + " >> updateAttendance >> Updates JSON Object : " + updates);

		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {

	}
}
