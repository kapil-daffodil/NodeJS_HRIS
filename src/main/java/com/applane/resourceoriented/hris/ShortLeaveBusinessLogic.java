package com.applane.resourceoriented.hris;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
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
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.ShortLeaveUtility;
import com.applane.resourceoriented.hris.utility.constants.ShortLeaveKinds;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class ShortLeaveBusinessLogic implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			String operationType = record.getOperationType();
			if (operationType.equalsIgnoreCase(Updates.Types.INSERT)) {
				workOnBeforeInsert(record);
			}
			if (operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {
				workOnBeforeInsert(record);
				workOnBeforeUpdates(record);

			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			LogUtility.writeLog("ShortLeaveBusinessLogic onBeforeUpdate Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}

	}

	private void workOnBeforeInsert(Record record) throws Exception {
		Object employeeId = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID);
		Object leaveDateObject = record.getValue(ShortLeaveKinds.SHORT_LEAVE_DATE);
		Object fromTimeObject = record.getValue(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME);
		Object toTimeObject = record.getValue(ShortLeaveKinds.SHORT_LEAVE_TO_TIME);
		if (employeeId != null && fromTimeObject != null && toTimeObject != null && leaveDateObject != null) {
			if (employeeId instanceof JSONObject) {
				employeeId = ((JSONObject) employeeId).opt(Updates.KEY);
			}
			int[] halfOffDay = new int[] { 0 };
			boolean isOffDay = EmployeeCardPunchDataBusinessLogic.getOffDayToMarkEWD(employeeId, Translator.dateValue(leaveDateObject), halfOffDay);
			if (!isOffDay && halfOffDay[0] != 1) {
				Object departmentName = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".departmentid.name");
				Object departmentAbb = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".departmentid.abbreviations");
				record.removeUpdate(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".departmentid.name");
				record.removeUpdate(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".departmentid.abbreviations");
				String fromTime = "";
				String fromTimeMeridiem = "";
				String toTime = "";
				String toTimeMeridiem = "";
				if (fromTimeObject instanceof JSONObject) {
					fromTime = Translator.stringValue(((JSONObject) fromTimeObject).opt("time"));
					fromTimeMeridiem = Translator.stringValue(((JSONObject) fromTimeObject).opt("meridiem"));
				}

				if (toTimeObject instanceof JSONObject) {
					toTime = Translator.stringValue(((JSONObject) toTimeObject).opt("time"));
					toTimeMeridiem = Translator.stringValue(((JSONObject) toTimeObject).opt("meridiem"));
				}
				EmployeeCardPunchDataBusinessLogic ecpd = new EmployeeCardPunchDataBusinessLogic();
				String fromTimeInFormat = ecpd.getCutOffTimeInFormat(fromTime, fromTimeMeridiem);
				String toTimeInFormat = ecpd.getCutOffTimeInFormat(toTime, toTimeMeridiem);
				Date fromDateTime = ecpd.getDateTime(fromTimeInFormat, leaveDateObject);
				Date toDateTime = ecpd.getDateTime(toTimeInFormat, leaveDateObject);
				if (fromDateTime.equals(toDateTime) || fromDateTime.after(toDateTime)) {
					throw new BusinessLogicException("From Time must be greater then To Time");
				}
				String filters = ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + " = " + employeeId + " and " + ShortLeaveKinds.SHORT_LEAVE_DATE + " = '" + leaveDateObject + "'";
				if (record.getOperationType().equalsIgnoreCase(Updates.Types.UPDATE)) {
					Object key = record.getKey();
					filters = filters + " and " + Updates.KEY + " != " + key;
				}
				JSONArray shortLeavesForRequestDate = getShortLeavesForRequestDate(filters);
				boolean isAlreadyTaken = false;
				if (shortLeavesForRequestDate != null && shortLeavesForRequestDate.length() > 0) {
					// isAlreadyTaken = isAlreadyTakenInBetweenApplyiedDuration(shortLeavesForRequestDate, leaveDateObject, fromDateTime, toDateTime);
					isAlreadyTaken = true;

				}
				if (!isAlreadyTaken && record.getOperationType().equalsIgnoreCase(Updates.Types.INSERT)) {
					Object leaveCode = ApplicationsGenerateCode.generateCode("" + departmentName, "" + departmentAbb, 1);
					record.addUpdate(ShortLeaveKinds.SHORT_LEAVE_CODE, leaveCode);
				} else if (isAlreadyTaken) {
					throw new BusinessLogicException("Leave Already Applyied for " + leaveDateObject);
					// throw new BusinessLogicException("Leave Already Applyied between " + fromTimeObject + " to " + toTimeObject);
				}
			} else {
				throw new BusinessLogicException("You can not apply Leave for OFF day " + leaveDateObject.toString());
			}
		} else {
			throw new BusinessLogicException("Employee, Leave Date, From Time and To Time can not be null");
		}
	}

	private void workOnBeforeUpdates(Record record) throws Exception {

	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			String operationType = record.getOperationType();
			if (operationType.equalsIgnoreCase(Updates.Types.INSERT)) {
				workOnAfterInsert(record);
				ShortLeaveUtility.sendLeaveEmail(record);
			} else if (operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {
				workOnAfterUpdates(record);
			}
		} catch (Exception e) {
			LogUtility.writeLog("ShortLeaveBusinessLogic onAfterUpdate Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
		}
	}

	private void workOnAfterInsert(Record record) throws Exception {
		Object key = record.getValue(Updates.KEY);
		Object userEmail = record.getValue("employeeid.officialemailid");
		Object directReportingToEmail = record.getValue("employeeid.reportingtoid.officialemailid");
		Object inDirectReportingToEmail = record.getValue("employeeid.indirectreportingto.officialemailid");

		if (userEmail != null) {
			Object employeeUserId = PunchingUtility.getId("up_users", "emailid", "" + userEmail);
			if (employeeUserId != null) {
				insertUserIdInApproverResource(key, employeeUserId);
			}
		}

		if (directReportingToEmail != null) {
			Object employeeUserId = PunchingUtility.getId("up_users", "emailid", "" + directReportingToEmail);
			if (employeeUserId != null) {
				insertUserIdInApproverResource(key, employeeUserId);
			}
		}
		if (inDirectReportingToEmail != null) {
			if (inDirectReportingToEmail instanceof JSONArray) {
				for (int counter = 0; counter < ((JSONArray) inDirectReportingToEmail).length(); counter++) {
					Object employeeUserId = PunchingUtility.getId("up_users", "emailid", "" + ((JSONArray) inDirectReportingToEmail).get(counter));
					if (employeeUserId != null) {
						insertUserIdInApproverResource(key, employeeUserId);
					}
				}
			} else {
				Object employeeUserId = PunchingUtility.getId("up_users", "emailid", "" + inDirectReportingToEmail);
				if (employeeUserId != null) {
					insertUserIdInApproverResource(key, employeeUserId);
				}
			}
		}

	}

	private void workOnAfterUpdates(Record record) throws Exception {
		// Object key = record.getValue(Updates.KEY);
		Object leaveDate = record.getValue(ShortLeaveKinds.SHORT_LEAVE_DATE);
		Object employeeId = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID);
		Object leavePolicyId = record.getValue(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID + ".leavepolicyid");
		Object leaveTypeId = record.getValue(ShortLeaveKinds.SHORT_LEAVE_TYPE_ID);
		int leaveStatusID = Translator.integerValue(record.getValue(ShortLeaveKinds.SHORT_LEAVE_STATUS_ID));
		String leaveStatus = "";
		if (leaveStatusID == HRISConstants.LEAVE_APPROVED) {
			leaveStatus = "Approved";
		} else if (leaveStatusID == HRISConstants.LEAVE_REJECTED) {
			leaveStatus = "Rejected";
		}
		if (leaveStatus.length() > 0) {
			ShortLeaveUtility.sendLeaveEmailOnUpdation(record, leaveStatus);
		}
		if (leaveStatusID == HRISConstants.LEAVE_APPROVED) {
			JSONArray employeeAttendance = getEmployeeAttendanceRecord(employeeId, leaveDate.toString());
			Object attendanceId = null;
			if (employeeAttendance != null && employeeAttendance.length() > 0) {
				attendanceId = employeeAttendance.getJSONObject(0).opt(Updates.KEY);
			}

			int organizationId = Translator.integerValue(CurrentState.getCurrentState().get(CurrentSession.CURRENT_ORGANIZATION_ID));
			if (organizationId == 606) {
				Date attendanceDate = Translator.dateValue("" + leaveDate);
				Calendar cal = Calendar.getInstance();
				cal.setTime(attendanceDate);

				Date monthFirstDate = DataTypeUtilities.getMonthFirstDate("" + cal.get(Calendar.YEAR), EmployeeSalaryGenerationServlet.getMonthName(cal.get(Calendar.MONTH) + 1));
				Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
				String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
				String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
				JSONArray leaveRule = new EmployeeMonthlyAttendance().getLeaveRuleRecord(leavePolicyId, monthFirstDateInString, monthLastDateInString);
				if (leaveRule != null && leaveRule.length() > 0) {
					leaveTypeId = leaveRule.getJSONObject(0).opt("leavetypeid");
				}
			}
			updateAttendance(attendanceId, leaveTypeId, employeeId, leaveDate);
		}
	}

	private JSONArray getEmployeeAttendanceRecord(Object employeeId, String date) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		array.put("attendancetypeid");
		array.put("attendancedate");
		query.put(Data.Query.COLUMNS, array);

		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate = '" + date + "'");
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeeattendance");
		return rows;
	}

	// public void updateAttendance(Object attendanceId, Object leaveTypeId, Object unApprovesLeaves) throws JSONException {
	public void updateAttendance(Object attendanceId, Object leaveTypeId, Object employeeId, Object leaveDate) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		if (attendanceId != null) {
			row.put("__key__", attendanceId);
		} else {
			row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_UNKNOWN);
			row.put("employeeid", employeeId);
			row.put("leavedate", leaveDate);
		}
		if (leaveTypeId != null) {
			row.put("leavetypeid", leaveTypeId);
		}
		// row.put("shortleaves", unApprovesLeaves);
		row.put("attendance_type_short_leave_id", HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	// private boolean isAlreadyTakenInBetweenApplyiedDuration(JSONArray shortLeavesForRequestDate, Object leaveDateObject, Date fromDateTimeParameter, Date toDateTimeParameter) throws Exception {
	// for (int counter = 0; counter < shortLeavesForRequestDate.length(); counter++) {
	// String fromTime = Translator.stringValue(shortLeavesForRequestDate.getJSONObject(counter).opt(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_TIME));
	// String fromTimeMeridiem = Translator.stringValue(shortLeavesForRequestDate.getJSONObject(counter).opt(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_MERIDIEM));
	// String toTime = Translator.stringValue(shortLeavesForRequestDate.getJSONObject(counter).opt(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_TIME));
	// String toTimeMeridiem = Translator.stringValue(shortLeavesForRequestDate.getJSONObject(counter).opt(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_MERIDIEM));
	// EmployeeCardPunchDataBusinessLogic ecpd = new EmployeeCardPunchDataBusinessLogic();
	// String fromTimeInFormat = ecpd.getCutOffTimeInFormat(fromTime, fromTimeMeridiem);
	// String toTimeInFormat = ecpd.getCutOffTimeInFormat(toTime, toTimeMeridiem);
	// Date fromDateTime = ecpd.getDateTime(fromTimeInFormat, leaveDateObject);
	// Date toDateTime = ecpd.getDateTime(toTimeInFormat, leaveDateObject);
	// if ((fromDateTime.equals(fromDateTimeParameter) || fromDateTime.equals(toDateTimeParameter) || toDateTime.equals(fromDateTimeParameter) || toDateTime.equals(toDateTimeParameter)) || ((fromDateTimeParameter.after(fromDateTime) && fromDateTimeParameter.before(toDateTime)) || (toDateTimeParameter.after(fromDateTime) && toDateTimeParameter.before(toDateTime)))) {
	// return true;
	// }
	// }
	// return false;
	// }

	private JSONArray getShortLeavesForRequestDate(String filters) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, ShortLeaveKinds.SHORT_LEAVE_RESOURCE);
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_EMPLOYEE_ID);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_TYPE_ID);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_TYPE_ID + ".name");
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_DATE);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_TIME);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_FROM_TIME_MERIDIEM);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_TIME);
		columnArray.put(ShortLeaveKinds.SHORT_LEAVE_TO_TIME_MERIDIEM);
		query.put(Data.Query.COLUMNS, columnArray);
		if (filters.length() > 0) {
			query.put(Data.Query.FILTERS, filters);
		}
		JSONObject object = ResourceEngine.query(query);
		JSONArray shortLeaveArray = object.getJSONArray(ShortLeaveKinds.SHORT_LEAVE_RESOURCE);
		return shortLeaveArray;
	}

	private void insertUserIdInApproverResource(Object key, Object employeeUserId) throws Exception {

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_shortleaves__approverid");
		JSONObject row = new JSONObject();
		row.put("hris_shortleaves", key);
		row.put("approverid", employeeUserId);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}
}
