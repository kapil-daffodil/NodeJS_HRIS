package com.applane.resourceoriented.hris;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.CurrentRequest;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.scheduler.AttendanceManagment;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.shared.constants.UserPermission;
import com.google.appengine.api.utils.SystemProperty;

/**
 * @author Ajay Pal Singh
 * @Resource employeeattendance
 */
public class EmployeeMonthlyAttendance implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	// double creditLeaves = 0.0;
	@Override
	public void onBeforeUpdate(Record record) {
		try {
			Object temp = record.getValue("temp");
			if (temp != null && !temp.toString().equalsIgnoreCase(HRISApplicationConstants.TEMP_VALUE_RUN_JOBS_ON_LEAVE_APPROVAL)) {
				return;
			}

			Object employeeId = record.getValue("employeeid");

			Object attDateObject = record.getValue("attendancedate");
			int attendanceTypeId = DataTypeUtilities.integerValue(record.getNewValue("attendancetypeid"));
			int attendanceTypeIdOldStatus = DataTypeUtilities.integerValue(record.getOldValue("attendancetypeid"));
			Date attendanceDate = DataTypeUtilities.checkDateFormat(attDateObject);
			String attendanceDateInString = getDateInString(attendanceDate);
			attendanceDate = Translator.dateValue(attendanceDateInString);

			Calendar cal = Calendar.getInstance();
			cal.setTime(attendanceDate);
			int monthId = cal.get(Calendar.MONTH) + 1;
			long yearIdLong = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
			int yearId = (int) yearIdLong;

			int branchId = Translator.integerValue(record.getValue("employeeid.branchid"));
			if (branchId == 0) {
				JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(Translator.integerValue(employeeId));
				if (employeeArray != null && employeeArray.length() > 0) {
					branchId = Translator.integerValue(employeeArray.getJSONObject(0).opt("branchid"));
				}
			}
			if (temp == null && record.getValue("attendance_status") == null) {
				record.addUpdate("attendance_status", 2);
			}
			if (record.getValue("attendance_status") != null && Translator.integerValue(record.getValue("attendance_status")) == 1) {
				record.addUpdate("attendancetypeid_old", attendanceTypeIdOldStatus);
			}
			if (record.getValue("attendance_status") != null && Translator.integerValue(record.getValue("attendance_status")) == 3) {
				record.addUpdate("attendancetypeid", record.getValue("attendancetypeid_old"));
			}
			boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.ATTENDANCE);
			if (isFreezed) {
				throw new BusinessLogicException("Attendance Freezed Please contact Admin Department");
			}
			if (record.has("leavecancled")) {
				return;
			}
			// String organization =
			// Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION));
			if (attendanceTypeId != 0 && (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE)) {
				if (record.getNewValue("leavetypeid") == null) {
					throw new BusinessLogicException("[" + attendanceDateInString + "] Leave Type Can Not be Null");
				}
			}
			if (record.has("attendance_type_short_leave_id") && record.getNewValue("attendance_type_short_leave_id") != null && Translator.integerValue(record.getNewValue("attendance_type_short_leave_id")) == HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE) {
				if (record.getNewValue("leavetypeid") == null) {
					throw new BusinessLogicException("[" + attendanceDateInString + "] Leave Type Can Not be Null");
				}
			}
			int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			boolean isNevigant = false;
			if (currentOrganizationId == 7783 || currentOrganizationId == 10719) {// organization.toLowerCase().indexOf("navigant")
																					// !=
																					// -1)
																					// {
				isNevigant = true;
			}
			int[] halfDayOff = { 0 };
			boolean isOffDay = EmployeeCardPunchDataBusinessLogic.getOffDayToMarkEWD(employeeId, attendanceDate, halfDayOff);
			if (!isNevigant && isOffDay && record.has("attendance_type_short_leave_id") && record.getNewValue("attendance_type_short_leave_id") != null) {
				throw new BusinessLogicException("[" + attendanceDateInString + "] Can not mark short leave on off day");
			}

			if (!isNevigant) {
				if (attendanceTypeId != 0 && (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_OFF || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HOLIDAY)) {
					if (!isOffDay) {
						throw new BusinessLogicException("[" + attendanceDateInString + "] working day Can not mark as Extra Working Day / OFF");
					}
				} else if (attendanceTypeId != 0 && (attendanceTypeId != HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_OFF)) {
					if (isOffDay) {
						boolean isThrowException = true;
						boolean isLeaveOnBothSide = false;
						Calendar dateCal = Calendar.getInstance();
						for (int counter = 0; counter < 7; counter++) {
							dateCal.setTime(attendanceDate);
							dateCal.add(Calendar.DAY_OF_MONTH, counter);
							Date attendanceDateForCheck = dateCal.getTime();
							halfDayOff[0] = 0;
							boolean isOffDayForCheckDate = EmployeeCardPunchDataBusinessLogic.getOffDayToMarkEWD(employeeId, attendanceDateForCheck, halfDayOff);
							if (!isOffDayForCheckDate) {
								String filter = "employeeid = " + employeeId + " and attendancedate = '" + EmployeeSalaryGenerationServlet.getDateInString(attendanceDateForCheck) + "' and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_ABSENT + ", " + HRISApplicationConstants.ATTENDANCE_LEAVE + ")";
								JSONArray employeeAttendanceArray = new EmployeeSalarySheetReport().getEmployeeAttendanceArray(filter);
								if (employeeAttendanceArray != null && employeeAttendanceArray.length() > 0) {
									isLeaveOnBothSide = true;
								}
								break;
							}

						}
						if (isLeaveOnBothSide) {
							for (int counter = 0; counter < 7; counter++) {
								dateCal.setTime(attendanceDate);
								dateCal.add(Calendar.DAY_OF_MONTH, -counter);
								Date attendanceDateForCheck = dateCal.getTime();
								boolean isOffDayForCheckDate = EmployeeCardPunchDataBusinessLogic.getOffDayToMarkEWD(employeeId, attendanceDateForCheck, halfDayOff);
								if (!isOffDayForCheckDate) {
									String filter = "employeeid = " + employeeId + " and attendancedate = '" + EmployeeSalaryGenerationServlet.getDateInString(attendanceDateForCheck) + "' and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_ABSENT + ", " + HRISApplicationConstants.ATTENDANCE_LEAVE + ")";
									JSONArray employeeAttendanceArray = new EmployeeSalarySheetReport().getEmployeeAttendanceArray(filter);
									if (employeeAttendanceArray != null && employeeAttendanceArray.length() > 0) {
										isThrowException = false;
									}
									break;
								}

							}
						}
						if (isThrowException) {
							throw new BusinessLogicException("[" + attendanceDateInString + "] Off day Can not mark attendance type other then Extra Working Day / OFF");
						}
					}
				}
			}
			// if (record.has("attendance_type_short_leave_id") &&
			// record.getNewValue("attendance_type_short_leave_id") != null &&
			// Translator.integerValue(record.getNewValue("attendance_type_short_leave_id"))
			// == HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE
			// && record.getNewValue("leavetypeid") == null) {
			// cal = Calendar.getInstance();
			// cal.setTime(attendanceDate);
			// int leavePolicyId =
			// Translator.integerValue(record.getValue("employeeid.leavepolicyid"));
			// Date monthFirstDate = DataTypeUtilities.getMonthFirstDate("" +
			// cal.get(Calendar.YEAR),
			// EmployeeSalaryGenerationServlet.getMonthName(cal.get(Calendar.MONTH)
			// + 1));
			// Date monthLastDate =
			// DataTypeUtilities.getMonthLastDate(monthFirstDate);
			// String monthFirstDateInString =
			// EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
			// String monthLastDateInString =
			// EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
			// JSONArray leaveRule = new
			// EmployeeMonthlyAttendance().getLeaveRuleRecord(leavePolicyId,
			// monthFirstDateInString, monthLastDateInString);
			// if (leaveRule != null && leaveRule.length() > 0) {
			// int leaveTypeId =
			// Translator.integerValue(leaveRule.getJSONObject(0).opt("leavetypeid"));
			// record.addUpdate("leavetypeid", leaveTypeId);
			// }
			// }
			if (record.has("attendance_type_short_leave_id") && record.getNewValue("attendance_type_short_leave_id") == null) {
				record.addUpdate("leavetypeid", JSONObject.NULL);
			}

			if ((attendanceTypeIdOldStatus == HRISApplicationConstants.ATTENDANCE_LEAVE || attendanceTypeIdOldStatus == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeIdOldStatus == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeIdOldStatus == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY || attendanceTypeIdOldStatus == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF)
					&& (record.has("attendancetypeid") && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_LEAVE && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF)) {
				record.addUpdate("paidstatus", JSONObject.NULL);
				record.addUpdate("leavetypeid", JSONObject.NULL);
			}
			if ((attendanceTypeIdOldStatus == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeIdOldStatus == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME || attendanceTypeIdOldStatus == HRISApplicationConstants.ATTENDANCE_TOUR) && (attendanceTypeId != 0 && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_PRESENT && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_TOUR)) {
				record.addUpdate("attendance_type_short_leave_id", JSONObject.NULL);
				if (attendanceTypeId != HRISApplicationConstants.ATTENDANCE_LEAVE && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
					record.addUpdate("paidstatus", JSONObject.NULL);
				}
			}

		} catch (BusinessLogicException e) {
			sendMailInCaseOfException(e, "From EmployeeMonthlyAttendance Exception BusinessLogicE", record.getValues());
			throw e;
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeMonthlyAttendance >> onBeforeUpdate Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new BusinessLogicException(e.getMessage());
		}
	}

	private void sendMailInCaseOfException(BusinessLogicException e, String title, JSONObject values) {
		try {
			String traces = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			ApplaneMail mail = new ApplaneMail();
			StringBuilder builder = new StringBuilder();
			builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
			builder.append("Values >> ").append(values).append("<BR /><BR />");
			builder.append("<br><br><b>Exception traces are given below :</b><br></br><hr></hr><br></br>").append(traces);
			mail.setMessage(title, builder.toString(), true);
			mail.setTo("kapil.dalal@daffodilsw.com");
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception Come while send mail in Employee Salary Servlet.");
		}
	}

	private JSONArray getEmployeeNextLeaveOrAbsentRecord(Object employeeId, String attendanceDateInString, String organization, int attendanceTypeId) throws Exception {

		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("attendancedate");
		array.put("attendancetypeid");
		query.put(Data.Query.COLUMNS, array);
		// query.put(Data.Query.FILTERS, "employeeid = " + employeeId +
		// " and attendancedate > '" + attendanceDateInString +
		// "' and attendancetypeid in (2, 3)");
		if (organization.toLowerCase().indexOf("navigant") != -1 && attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
			query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate > '" + attendanceDateInString + "' and attendancetypeid in (2, 3, 9, 12)");
		} else {
			query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate > '" + attendanceDateInString + "' and attendancetypeid in (2, 3)");
		}
		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "attendancedate");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		JSONArray orderByExpression = new JSONArray();
		orderByExpression.put(order);
		query.put(Data.Query.ORDERS, orderByExpression);
		query.put(Data.Query.MAX_ROWS, 1);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeeattendance");
		return rows;

	}

	private void updateStatusOfAttendanceForSandwich(Object employeeId, String date, int attendanceTypeId, Object leaveType) throws JSONException {
		JSONArray employeeAttendanceRecord = getEmployeeAttendanceRecord(employeeId, date);
		if (employeeAttendanceRecord.length() > 0) {
			Object key = employeeAttendanceRecord.getJSONObject(0).opt("__key__");
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "employeeattendance");
			JSONObject row = new JSONObject();
			row.put("__key__", key);
			if (leaveType != null) {
				row.put("leavetypeid", leaveType);
			}
			row.put("attendancetypeid", attendanceTypeId);
			row.put("issandwich", true);
			// row.put("temp", HRISApplicationConstants.TEMP_VALUE);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		}
	}

	private JSONArray getEmployeeLastLeaveOrAbsentRecord(Object employeeId, String attendanceDateInString, String organization, int attendanceTypeId) throws JSONException {

		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("attendancedate");
		array.put("attendancetypeid");
		query.put(Data.Query.COLUMNS, array);
		if (organization.toLowerCase().indexOf("navigant") != -1 && attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
			query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate < '" + attendanceDateInString + "' and attendancetypeid in (2, 3, 9, 12)");
		} else {
			query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate < '" + attendanceDateInString + "' and attendancetypeid in (2, 3)");
		}
		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "attendancedate");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		JSONArray orderByExpression = new JSONArray();
		orderByExpression.put(order);
		query.put(Data.Query.ORDERS, orderByExpression);
		query.put(Data.Query.MAX_ROWS, 1);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeeattendance");
		return rows;

	}

	@Override
	public void onAfterUpdate(Record record) {
		String operationType = record.getOperationType();
		Object temp = record.getValue("temp");
		if (temp != null && !temp.toString().equalsIgnoreCase(HRISApplicationConstants.TEMP_VALUE_RUN_JOBS) && !temp.toString().equalsIgnoreCase(HRISApplicationConstants.TEMP_VALUE_RUN_JOBS_ON_LEAVE_APPROVAL) && !temp.toString().equalsIgnoreCase(HRISApplicationConstants.TEMP_VALUE_STOP_POPULATEMONTHLY_ATTENDANCE)) {
			return;
		}
		Object employeeId = record.getValue("employeeid");
		Object leavePolicyId = record.getValue("employeeid.leavepolicyid");
		Object attDateObject = record.getValue("attendancedate");
		Object employeeSchedule = record.getValue("employeeid.employeescheduleid");
		Date attendanceDate = DataTypeUtilities.checkDateFormat(attDateObject);
		int attendanceTypeId = DataTypeUtilities.integerValue(record.getNewValue("attendancetypeid"));
		int attendanceTypeIdOld = DataTypeUtilities.integerValue(record.getOldValue("attendancetypeid"));
		String attendanceDateInString = getDateInString(attendanceDate);
		String organization = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION));
		boolean isMarkSandwich = Translator.booleanValue(record.getValue("employeeid.leavepolicyid.issandwich"));
		boolean isNevigant = false;
		if (organization.toLowerCase().indexOf("navigant") != -1) {
			isNevigant = true;
		}

		boolean enterInIfCondition = false;
		if (isNevigant) {
			if (isMarkSandwich && (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY)) {
				enterInIfCondition = true;
			}
		} else {
			if (isMarkSandwich && (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT)) {
				enterInIfCondition = true;
			}
		}
		try {
			Date backAttendanceDate = Translator.dateValue(DataTypeUtilities.getBackDate(attendanceDateInString));
			if (enterInIfCondition) {
				JSONArray employeeLastLeaveOrAbsentRecord = getEmployeeLastLeaveOrAbsentRecord(employeeId, attendanceDateInString, organization, attendanceTypeId);
				Object leaveType = null;
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
					leaveType = record.getValue("leavetypeid");
				}
				if (leaveType == null) {
					leaveType = new AttendanceManagment().getLeaveRuleType(leavePolicyId, attendanceDateInString, attendanceDateInString);
				}
				if (employeeLastLeaveOrAbsentRecord.length() > 0) {
					JSONObject employeeLastLeaveOrAbsentRecordObject = employeeLastLeaveOrAbsentRecord.getJSONObject(0);
					Object absentLeaveDateObject = employeeLastLeaveOrAbsentRecordObject.opt("attendancedate");
					Date absentLeaveDate = DataTypeUtilities.checkDateFormat(absentLeaveDateObject);
					String absentLeaveDateInString = getDateInString(absentLeaveDate);
					absentLeaveDate = Translator.dateValue(DataTypeUtilities.getNextDate(absentLeaveDateInString));
					long numOfDaysBetweenTwoDates = DataTypeUtilities.daysBetween(backAttendanceDate, absentLeaveDate) + 1;
					JSONArray employeeArray = getEmployeeRecord(employeeId);

					if (employeeArray.length() > 0) {
						Object employeeScheduleId = employeeArray.getJSONObject(0).opt("employeescheduleid");
						Object holidayCalendarId = employeeArray.getJSONObject(0).opt("holidaycalendarid");

						int offDays = getActualNonWorkingDaysForSandWich(employeeId, employeeScheduleId, holidayCalendarId, absentLeaveDate, backAttendanceDate);
						if (offDays == numOfDaysBetweenTwoDates) {
							Calendar cal2 = new GregorianCalendar();
							for (int counter = 0; counter < numOfDaysBetweenTwoDates; counter++) {
								cal2.setTime(absentLeaveDate);
								cal2.add(Calendar.DATE, counter);
								Date date = cal2.getTime();
								String dateInString = getDateInString(date);
								updateStatusOfAttendanceForSandwich(employeeId, dateInString, attendanceTypeId, leaveType);
								// double subtractNonWorkingDays = 1;
								// EmployeeLeaveBusinessLogic.updateMonthlyAttendanceWithSandwich(date,
								// employeeId, subtractNonWorkingDays);
							}
						}
					}
				}

				employeeLastLeaveOrAbsentRecord = getEmployeeNextLeaveOrAbsentRecord(employeeId, attendanceDateInString, organization, attendanceTypeId);
				if (employeeLastLeaveOrAbsentRecord.length() > 0) {
					JSONObject employeeLastLeaveOrAbsentRecordObject = employeeLastLeaveOrAbsentRecord.getJSONObject(0);
					Object absentLeaveDateObject = employeeLastLeaveOrAbsentRecordObject.opt("attendancedate");
					Object attendanceTypeIdObject = employeeLastLeaveOrAbsentRecordObject.opt("attendancetypeid");
					Date absentLeaveDate = DataTypeUtilities.checkDateFormat(absentLeaveDateObject);
					// String absentLeaveDateInString =
					// getDateInString(absentLeaveDate);
					backAttendanceDate = Translator.dateValue(DataTypeUtilities.getNextDate(attendanceDateInString));
					long numOfDaysBetweenTwoDates = DataTypeUtilities.daysBetween(absentLeaveDate, backAttendanceDate) + 1;
					JSONArray employeeArray = getEmployeeRecord(employeeId);

					if (employeeArray.length() > 0) {
						Object employeeScheduleId = employeeArray.getJSONObject(0).opt("employeescheduleid");
						Object holidayCalendarId = employeeArray.getJSONObject(0).opt("holidaycalendarid");
						int offDays = getActualNonWorkingDaysForSandWich(employeeId, employeeScheduleId, holidayCalendarId, backAttendanceDate, absentLeaveDate);
						if (offDays == numOfDaysBetweenTwoDates) {
							Calendar cal2 = new GregorianCalendar();
							for (int counter = 0; counter < numOfDaysBetweenTwoDates; counter++) {
								cal2.setTime(backAttendanceDate);
								cal2.add(Calendar.DATE, counter);
								Date date = cal2.getTime();
								String dateInString = getDateInString(date);
								updateStatusOfAttendanceForSandwich(employeeId, dateInString, Translator.integerValue(attendanceTypeIdObject), leaveType);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			throw new BusinessLogicException(" >> [ {from update} Some unknown error occured while calculate employee monthly attendance.>>>" + trace + " << error trace ]");
		}
		if (operationType.equalsIgnoreCase("insert")) {
			try {

				String currentMonth = DataTypeUtilities.getCurrentMonth(attendanceDate);
				Object monthId = getMonthId(currentMonth);
				String currentYear = DataTypeUtilities.getCurrentYear(attendanceDate);
				Object yearId = getYearId(currentYear);
				Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(currentYear, currentMonth);
				Date todayDate = SystemParameters.getCurrentDateTime();
				if (!monthFirstDate.after(todayDate)) {

					if ((employeeId != null && yearId != null && monthId != null) && (temp == null || !temp.toString().equalsIgnoreCase(HRISApplicationConstants.TEMP_VALUE_STOP_POPULATEMONTHLY_ATTENDANCE))) {

						JSONArray monthlyAttendanceArray = getEmployeeMonthlyAttendanceRecord(employeeId, yearId, monthId);
						int rowCount = (monthlyAttendanceArray == null || monthlyAttendanceArray.length() == 0) ? 0 : monthlyAttendanceArray.length();
						if (rowCount > 0) {
							Object monthlyAttendanceId = monthlyAttendanceArray.getJSONObject(0).opt("__key__");
							Object isManualUpdateObject = monthlyAttendanceArray.getJSONObject(0).opt("ismanualupdate");
							boolean isManualUpdate = DataTypeUtilities.booleanValue(isManualUpdateObject);
							if (!isManualUpdate) {
								Map<String, Object> attendanceParameterMap = getAttendanceParameters(employeeId, currentYear, currentMonth, employeeSchedule);

								if (attendanceParameterMap != null && attendanceParameterMap.size() > 0) {
									Object totalPresentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_PRESENTS);
									Object totalAbsentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_ABSENTS);
									Object totalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_LEAVES);
									Object totalExtraWorkingDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EXTRAWORKINGDAYS);

									Object totalEarnedLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EARNED_LEAVES);
									Object totalCasualLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_CASUAL_LEAVES);
									Object totalMedicalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_MEDICAL_LEAVES);
									Object totalSpecialLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_SPECIAL_LEAVES);
									Object totalNonWorkingDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_NON_WORKING_DAYS);
									updateEmployeeMonthlyAttendance(monthlyAttendanceId, totalPresentDays, totalAbsentDays, totalLeaves, totalExtraWorkingDays, totalEarnedLeaves, totalCasualLeaves, totalMedicalLeaves, totalSpecialLeaves, totalNonWorkingDays);
								}
							}
						} else {
							boolean isFromMonthlyUpdateAllColumns = false;
							updateNewMonthlyAttendanceRecord(null, employeeId, monthId, yearId, currentYear, currentMonth, new Date(), false, attendanceDate, isFromMonthlyUpdateAllColumns, null);
						}
					}
				}
			} catch (Exception e) {
				LogUtility.writeLog(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
				throw new BusinessLogicException(" >> [ {from insert} Some unknown error occured while calculate employee monthly attendance.>>>" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + " << error trace ]");
			}
		} else if (operationType.equalsIgnoreCase("update")) {

			try {
				Object employeeAttendanceKey = record.getKey();
				// Object employeeId = record.getOldValue("employeeid");
				// Object attDateObject = record.getOldValue("attendancedate");
				// Object ewdApproved = record.getOldValue("ewdapproved");
				int attendanceTypeNewID = Translator.integerValue(record.getNewValue("attendancetypeid"));
				int attendanceTypeOldID = Translator.integerValue(record.getOldValue("attendancetypeid"));

				// Date attendanceDate =
				// DataTypeUtilities.checkDateFormat(attDateObject);
				String currentMonth = DataTypeUtilities.getCurrentMonth(attendanceDate);
				Object monthId = getMonthId(currentMonth);
				String currentYear = DataTypeUtilities.getCurrentYear(attendanceDate);
				Object yearId = getYearId(currentYear);

				if (record.has("leavecancled")) {
					checkAndUpdateForCanclation(employeeAttendanceKey, employeeId, attendanceDate, record);

					return;
				}

				if (temp == null && !record.has("leavecancled") && ((attendanceTypeOldID == HRISApplicationConstants.ATTENDANCE_ABSENT || attendanceTypeOldID == HRISApplicationConstants.ATTENDANCE_LEAVE) && (attendanceTypeNewID == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeNewID == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeNewID == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeNewID == HRISApplicationConstants.ATTENDANCE_TOUR || attendanceTypeNewID == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME))) {
					HashMap<Date, Integer> offDaysMap = new HashMap<Date, Integer>();
					HashMap<Date, Integer> holidaysMap = new HashMap<Date, Integer>();
					HashMap<Date, Object[]> dailyAttendanceMap = new HashMap<Date, Object[]>();

					Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(currentYear, currentMonth);
					String monthFirstDateInString = getDateInString(monthFirstDate);
					Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
					String monthLastDateInString = getDateInString(monthLastDate);
					Object scheduleId = record.getValue("employeeid.employeescheduleid");
					Object holidayCalendarId = record.getValue("employeeid.holidaycalendarid");
					getTotalOffAndHolidaysMaps(monthFirstDate, monthFirstDateInString, monthLastDate, monthLastDateInString, offDaysMap, holidaysMap, dailyAttendanceMap, scheduleId, holidayCalendarId, employeeId);

					Calendar cal = Calendar.getInstance();
					cal.setTime(attendanceDate);
					boolean whileLoop = true;

					while (whileLoop) {
						cal.add(Calendar.DAY_OF_MONTH, -1);
						Date date = cal.getTime();
						if (dailyAttendanceMap.containsKey(date)) {
							Object[] details = dailyAttendanceMap.get(date);
							Object key = details[0];
							int attendanceTypeFromMap = Translator.integerValue(details[1]);
							if (attendanceTypeFromMap == HRISApplicationConstants.ATTENDANCE_ABSENT || attendanceTypeFromMap == HRISApplicationConstants.ATTENDANCE_LEAVE) {
								if (offDaysMap.containsKey(date)) {
									updateEmployeeAttendance(key, HRISApplicationConstants.ATTENDANCE_OFF);
								} else if (holidaysMap.containsKey(date)) {
									updateEmployeeAttendance(key, HRISApplicationConstants.ATTENDANCE_HOLIDAY);
								} else {
									whileLoop = false;
								}
							} else {
								whileLoop = false;
							}
						} else {
							whileLoop = false;
						}
					}

					cal.setTime(attendanceDate);
					whileLoop = true;
					while (whileLoop) {
						cal.add(Calendar.DAY_OF_MONTH, 1);
						Date date = cal.getTime();
						if (dailyAttendanceMap.containsKey(date)) {
							Object[] details = dailyAttendanceMap.get(date);
							Object key = details[0];
							int attendanceTypeFromMap = Translator.integerValue(details[1]);
							if (attendanceTypeFromMap == HRISApplicationConstants.ATTENDANCE_ABSENT || attendanceTypeFromMap == HRISApplicationConstants.ATTENDANCE_LEAVE) {
								if (offDaysMap.containsKey(date)) {
									updateEmployeeAttendance(key, HRISApplicationConstants.ATTENDANCE_OFF);
								} else if (holidaysMap.containsKey(date)) {
									updateEmployeeAttendance(key, HRISApplicationConstants.ATTENDANCE_OFF);
								} else {
									whileLoop = false;
								}
							} else {
								whileLoop = false;
							}
						} else {
							whileLoop = false;
						}
					}
				}
				if (record.has("attendance_status") && Translator.integerValue(record.getValue("attendance_status")) == 1) {
					Object reportingToEmailid = record.getValue("employeeid.reportingtoid.officialemailid");
					Object reportingToName = record.getValue("employeeid.reportingtoid.name");
					Object employeeName = record.getValue("employeeid.name");
					Object attendanceNewName = record.getNewValue("attendancetypeid.name");
					Object attendanceOldName = record.getOldValue("attendancetypeid.name");
					Object remarks = record.getValue("remarks");
					// LogUtility.writeError("record.has('attendance_status') >> "
					// + record.has("attendance_status") +
					// " << reportingToEmailid >> " + reportingToEmailid +
					// " << reportingToName >> " + reportingToName +
					// " << employeeName >> " + employeeName);
					sendEmailToReportingToApproveAttendanceChangeRequest(reportingToEmailid, reportingToName, employeeName, attendanceDateInString, attendanceOldName, attendanceNewName, employeeAttendanceKey, remarks);
					// sendEmailToReportingToApproveAttendanceChangeRequest(reportingToEmailid,
					// reportingToName, employeeName);
				}
				if (!record.has("leavecancled") && ((attendanceTypeIdOld != attendanceTypeId && record.has("attendancetypeid")) || record.has("paidstatus") || record.has("attendance_type_short_leave_id")) || (record.has("attendance_status"))) {
					currentMonth = DataTypeUtilities.getCurrentMonth(attendanceDate);
					monthId = getMonthId(currentMonth);
					currentYear = DataTypeUtilities.getCurrentYear(attendanceDate);
					yearId = getYearId(currentYear);

					Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(currentYear, currentMonth);
					Date todayDate = SystemParameters.getCurrentDateTime();
					if (!monthFirstDate.after(todayDate)) {
						if (employeeId != null && yearId != null && monthId != null) {
							JSONArray monthlyAttendanceArray = getEmployeeMonthlyAttendanceRecord(employeeId, yearId, monthId);
							int rowCount = (monthlyAttendanceArray == null || monthlyAttendanceArray.length() == 0) ? 0 : monthlyAttendanceArray.length();
							if (rowCount > 0) {
								Object monthlyAttendanceId = monthlyAttendanceArray.getJSONObject(0).opt("__key__");
								Object isManualUpdateObject = monthlyAttendanceArray.getJSONObject(0).opt("ismanualupdate");
								boolean isManualUpdate = DataTypeUtilities.booleanValue(isManualUpdateObject);
								if (!isManualUpdate) {

									Map<String, Object> attendanceParameterMap = getAttendanceParameters(employeeId, currentYear, currentMonth, employeeSchedule);

									if (attendanceParameterMap != null && attendanceParameterMap.size() > 0) {
										Object totalPresentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_PRESENTS);
										Object totalAbsentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_ABSENTS);
										Object totalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_LEAVES);
										Object totalExtraWorkingDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EXTRAWORKINGDAYS);
										Object totalEarnedLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EARNED_LEAVES);
										Object totalCasualLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_CASUAL_LEAVES);
										Object totalMedicalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_MEDICAL_LEAVES);
										Object totalSpecialLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_SPECIAL_LEAVES);
										Object totalNonWorkingDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_NON_WORKING_DAYS);
										updateEmployeeMonthlyAttendance(monthlyAttendanceId, totalPresentDays, totalAbsentDays, totalLeaves, totalExtraWorkingDays, totalEarnedLeaves, totalCasualLeaves, totalMedicalLeaves, totalSpecialLeaves, totalNonWorkingDays);
									}
								}
							} else {
								boolean isFromMonthlyUpdateAllColumns = false;
								updateNewMonthlyAttendanceRecord(null, employeeId, monthId, yearId, currentYear, currentMonth, new Date(), false, attendanceDate, isFromMonthlyUpdateAllColumns, null);
							}
						}
					}
				}
			} catch (Exception e) {
				LogUtility.writeLog(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
				throw new BusinessLogicException(" >> [ {from update} Some unknown error occured while calculate employee monthly attendance.>>>" + e + " << error trace ]");
			}
		}
	}

	private void sendEmailToReportingToApproveAttendanceChangeRequest(Object reportingToEmailid, Object reportingToName, Object employeeName, String attendanceDateInString, Object attendanceOldName, Object attendanceNewName, Object employeeAttendanceKey, Object remarks) throws Exception {

		String title = "Attendance Change Request By " + employeeName;

		StringBuilder message = new StringBuilder();
		message.append("Dear " + reportingToName).append("<br /><br />");
		message.append(employeeName).append(" has changed his/her attendance of ").append(attendanceDateInString).append(" with attendance status from <b>").append(attendanceOldName).append("</b> to <b>").append(attendanceNewName).append("</b> and request to approve the same.").append("<br /><br />");
		message.append("<b>Remarks :</b> ").append(remarks).append("<br /><br />");

		addContentToApproveByMail(message, reportingToEmailid.toString(), employeeAttendanceKey);

		message.append("Regards").append("<br />");
		message.append(employeeName);

		PunchingUtility.sendMailsAccToProfitCenters(new String[] { reportingToEmailid.toString() }, message.toString(), title, null);
	}

	private String addContentToApproveByMail(StringBuilder message, String reportingToEmailId, Object employeeAttendanceKey) throws JSONException {

		Object currentOrganization = getCurrentOrganization();

		String action = "http://apps.applane.com/escape/approveattendancestatus";

		message.append("<table border='0' width='98%' align='center'>");
		message.append("<tr>");
		message.append("<td align='left' style=\"width: 100px;\" >");
		message.append("<form action='" + action + "'>");
		message.append("<input type='hidden' name='" + HRISConstants.ApproveAttendanceStatus.FormParameterNames.KEY + "' value='").append(employeeAttendanceKey).append("' />");
		message.append("<input type='hidden' name='" + HRISConstants.ApproveAttendanceStatus.FormParameterNames.ORGANIZATION + "' value='").append(currentOrganization).append("' />");
		message.append("<input type='hidden' name='" + HRISConstants.ApproveAttendanceStatus.FormParameterNames.APPROVER_OFFICIAL_EMAIL_ID + "' value='").append(reportingToEmailId).append("' />");
		message.append("<input type='hidden' name='" + HRISConstants.ApproveAttendanceStatus.FormParameterNames.STATUS + "' value='").append(HRISConstants.ApproveAttendanceStatus.STATUS_APPROVED).append("' />");
		message.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		message.append("</form>");
		message.append("</td>");

		message.append("<td align='left'>");
		message.append("<form action='" + action + "'>");
		message.append("<input type='hidden' name='" + HRISConstants.ApproveAttendanceStatus.FormParameterNames.KEY + "' value='").append(employeeAttendanceKey).append("' />");
		message.append("<input type='hidden' name='" + HRISConstants.ApproveAttendanceStatus.FormParameterNames.ORGANIZATION + "' value='").append(currentOrganization).append("' />");
		message.append("<input type='hidden' name='" + HRISConstants.ApproveAttendanceStatus.FormParameterNames.APPROVER_OFFICIAL_EMAIL_ID + "' value='").append(reportingToEmailId).append("' />");
		message.append("<input type='hidden' name='" + HRISConstants.ApproveAttendanceStatus.FormParameterNames.STATUS + "' value='").append(HRISConstants.ApproveAttendanceStatus.STATUS_REJECTED).append("' />");
		message.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		message.append("</form>");
		message.append("</td>");
		message.append("</tr>");
		message.append("</table>");

		message.append("<br />");

		return message.toString();
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
		employeeObject = ResourceEngine.query(query);
		JSONArray organizationArray = employeeObject.getJSONArray("up_organizations");
		if (organizationArray != null && organizationArray.length() > 0) {
			return organizationArray.getJSONObject(0).opt("organization");
		}
		return null;
	}

	public void invokeUpdateMonthlyAttendance(Object[] selectedKeys, Object dateObject) throws Exception {
		if (dateObject == null) {
			dateObject = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		}
		Date date = Translator.dateValue(dateObject);// "2013-04-05");

		String currentMonth = DataTypeUtilities.getCurrentMonth(date);
		Object monthId = getMonthId(currentMonth);
		String currentYear = DataTypeUtilities.getCurrentYear(date);
		Object yearId = getYearId(currentYear);

		for (int counter = 0; counter < selectedKeys.length; counter++) {
			Object employeeId = selectedKeys[counter];
			Object employeeSchedule = getEmployeeScheduleId(employeeId);
			JSONArray monthlyAttendanceArray = getEmployeeMonthlyAttendanceRecord(employeeId, yearId, monthId);
			int rowCount = (monthlyAttendanceArray == null || monthlyAttendanceArray.length() == 0) ? 0 : monthlyAttendanceArray.length();
			if (rowCount > 0) {
				Object monthlyAttendanceId = monthlyAttendanceArray.getJSONObject(0).opt("__key__");
				Map<String, Object> attendanceParameterMap = getAttendanceParameters(employeeId, currentYear, currentMonth, employeeSchedule);

				if (attendanceParameterMap != null && attendanceParameterMap.size() > 0) {
					Object totalPresentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_PRESENTS);
					Object totalAbsentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_ABSENTS);
					Object totalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_LEAVES);
					Object totalExtraWorkingDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EXTRAWORKINGDAYS);

					Object totalEarnedLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EARNED_LEAVES);
					Object totalCasualLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_CASUAL_LEAVES);
					Object totalMedicalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_MEDICAL_LEAVES);
					Object totalSpecialLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_SPECIAL_LEAVES);
					Object totalNonWorkingDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_NON_WORKING_DAYS);
					updateEmployeeMonthlyAttendance(monthlyAttendanceId, totalPresentDays, totalAbsentDays, totalLeaves, totalExtraWorkingDays, totalEarnedLeaves, totalCasualLeaves, totalMedicalLeaves, totalSpecialLeaves, totalNonWorkingDays);
				}
			} else {
				boolean isFromMonthlyUpdateAllColumns = false;
				updateNewMonthlyAttendanceRecord(null, employeeId, monthId, yearId, currentYear, currentMonth, new Date(), false, date, isFromMonthlyUpdateAllColumns, null);
			}
		}
	}

	private Object getEmployeeScheduleId(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("employeescheduleid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + employeeId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_employees");
		if (rows != null && rows.length() > 0) {
			return rows.getJSONObject(0).opt("employeescheduleid");
		}
		return null;
	}

	private void checkAndUpdateForCanclation(Object employeeAttendanceKey, Object employeeId, Date attendanceDate, Record record) throws Exception {
		int[] halfDayOff = { 0 };
		boolean isOffDay = EmployeeCardPunchDataBusinessLogic.getOffDayToMarkEWD(employeeId, attendanceDate, halfDayOff);
		Object attendanceType = HRISApplicationConstants.ATTENDANCE_UNKNOWN;
		Object leaveType = null;
		if (isOffDay) {
			if (halfDayOff[0] == 1) {
				attendanceType = HRISApplicationConstants.ATTENDANCE_HALF_DAY_OFF;
			} else {
				attendanceType = HRISApplicationConstants.ATTENDANCE_OFF;
			}
		}
		JSONArray leaveRequest = getLeaveRequest(employeeId, EmployeeSalaryGenerationServlet.getDateInString(attendanceDate));
		String remarks = "";
		Object leaveRequestId = null;
		if (leaveRequest != null && leaveRequest.length() > 0) {
			remarks += leaveRequest.getJSONObject(0).optString("remarks", "");
			leaveRequestId = leaveRequest.getJSONObject(0).opt("__key__");
		}
		if (!Translator.booleanValue(record.getNewValue("leavecancled"))) {
			JSONArray leavesArray = MarkAttendanceServlet.getLeaveRequestRecords(employeeId, EmployeeSalaryGenerationServlet.getDateInString(attendanceDate));
			if (leavesArray != null && leavesArray.length() > 0) {
				attendanceType = HRISApplicationConstants.ATTENDANCE_LEAVE;
				int durationType = MarkAttendanceServlet.isHalfDayLeave(attendanceDate, leavesArray);
				if (durationType == HRISConstants.DURATION_FIRSTHALF) {
					attendanceType = HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE;
				} else if (durationType == HRISConstants.DURATION_SECONDHALF) {
					attendanceType = HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE;
				}
				leaveType = leavesArray.getJSONObject(0).opt("leavetypeid");
			}
			remarks += "[Leave request for " + EmployeeSalaryGenerationServlet.getDateInString(attendanceDate) + " apply again.]";
		} else {
			remarks += "[Leave request for " + EmployeeSalaryGenerationServlet.getDateInString(attendanceDate) + " cancled.]";
		}
		if (leaveRequestId != null) {
			updateLeaveRequestRemarks(leaveRequestId, remarks);
		}
		updateAttendanceCancleLeave(employeeAttendanceKey, attendanceType, leaveType);
	}

	private void updateLeaveRequestRemarks(Object leaveRequestId, String remarks) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_leaverequests");
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, leaveRequestId);
		row.put("remarks", remarks);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private JSONArray getLeaveRequest(Object employeeId, String attendanceDate) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverequests");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("remarks");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and fromdate <= '" + attendanceDate + "' and todate >= '" + attendanceDate + "'");
		JSONObject object = ResourceEngine.query(query);
		return object.getJSONArray("hris_leaverequests");
	}

	private void updateAttendanceCancleLeave(Object employeeAttendanceKey, Object attendanceType, Object leaveType) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, employeeAttendanceKey);
		row.put("attendancetypeid", attendanceType);
		row.put("leavetypeid", leaveType == null ? JSONObject.NULL : leaveType);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private static void updateEmployeeAttendance(Object employeeAttendanceId, int attendanceTypeId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, employeeAttendanceId);
		row.put("attendancetypeid", attendanceTypeId);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public void getTotalOffAndHolidaysMaps(Date monthFirstDate, String monthFirstDateInString, Date monthLastDate, String monthLastDateInString, HashMap<Date, Integer> offDaysMap, HashMap<Date, Integer> holidaysMap, HashMap<Date, Object[]> dailyAttendanceMap, Object scheduleId, Object holidayCalendarId, Object employeeId) throws Exception {
		if (dailyAttendanceMap != null) {
			putDailyAttendanceArray(monthFirstDateInString, monthLastDateInString, employeeId, dailyAttendanceMap);
		}
		if (offDaysMap != null) {
			putOffDaysMap(monthFirstDate, monthLastDate, employeeId, scheduleId, offDaysMap);
		}
		if (holidaysMap != null) {
			putHolidayMap(monthFirstDateInString, monthLastDateInString, employeeId, holidayCalendarId, holidaysMap);
		}
	}

	private void putHolidayMap(String monthFirstDate, String monthLastDate, Object employeeId, Object holidayCalendarId, HashMap<Date, Integer> holidaysMap) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidays");
		JSONArray array = new JSONArray();
		array.put("holidaydate");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate >= '" + monthFirstDate + "' and holidaydate <= '" + monthLastDate + "'");
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_holidays");
		for (int counter = 0; counter < rows.length(); counter++) {
			Date holidayDate = Translator.dateValue(rows.getJSONObject(counter).opt("holidaydate"));
			holidaysMap.put(holidayDate, 1);

		}
	}

	private void putOffDaysMap(Date monthFirstDate, Date monthLastDate, Object employeeId, Object scheduleId, HashMap<Date, Integer> offDaysMap) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_schedules");
		JSONArray array = new JSONArray();
		array.put("monday");
		array.put("tuesday");
		array.put("wednesday");
		array.put("thursday");
		array.put("friday");
		array.put("saturday");
		array.put("sunday");

		JSONObject innerObject = new JSONObject();
		JSONObject innerQuery = new JSONObject();
		JSONArray innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		// innerColumns.put("weeknoid.name");
		// innerColumns.put("weekdayid.name");
		innerColumns.put("weeknoid");
		innerColumns.put("weekdayid");

		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_monthlyoff");

		innerObject.put("relatedcolumn", "scheduleid");
		innerObject.put("alias", "hris_monthlyoff");
		innerObject.put("parentcolumnalias", Updates.KEY);
		innerObject.put("parentcolumn", Updates.KEY);
		innerObject.put(Data.QUERY, innerQuery);

		query.put(Data.Query.CHILDS, new JSONArray().put(innerObject));

		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + scheduleId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_schedules");

		if (rows.length() > 0) {
			Object employeeDetailsObject = rows.getJSONObject(0).opt("hris_monthlyoff");
			JSONArray alternateRecords = new JSONArray();
			if (employeeDetailsObject != null && employeeDetailsObject instanceof JSONArray) {
				alternateRecords = (JSONArray) employeeDetailsObject;
			} else if (employeeDetailsObject != null && employeeDetailsObject instanceof JSONObject) {
				alternateRecords = new JSONArray().put(employeeDetailsObject);
			}
			long daysDifference = DataTypeUtilities.differenceBetweenDates(monthFirstDate, monthLastDate) + 1;
			String strDateFormat = "EEEE";
			SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
			Calendar cal = Calendar.getInstance();
			for (int counter = 0; counter < daysDifference; counter++) {
				cal.setTime(monthFirstDate);
				cal.add(Calendar.DAY_OF_MONTH, counter);
				int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
				int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
				Date date = cal.getTime();
				String weekDay = sdf.format(date).toLowerCase();
				boolean value = Translator.booleanValue(rows.getJSONObject(0).opt(weekDay));
				if (!value) {
					offDaysMap.put(date, 0);
				}
				if (alternateRecords != null && alternateRecords.length() > 0) {
					for (int alternateCounter = 0; alternateCounter < alternateRecords.length(); alternateCounter++) {
						JSONObject alternateObject = alternateRecords.getJSONObject(alternateCounter);
						int weekOfMonthTemp = Translator.integerValue(alternateObject.opt("weeknoid"));
						int dayOfWeekTemp = Translator.integerValue(alternateObject.opt("weekdayid"));
						if (weekOfMonth == weekOfMonthTemp && dayOfWeek == dayOfWeekTemp) {
							offDaysMap.put(date, 0);
						}
					}
				}
			}
		}
	}

	private void putDailyAttendanceArray(String monthFirstDateInString, String monthLastDateInString, Object employeeId, HashMap<Date, Object[]> dailyAttendanceMap) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("attendancetypeid");
		array.put("attendancedate");
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);

		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate >= '" + monthFirstDateInString + "' and attendancedate <= '" + monthLastDateInString + "'");
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeeattendance");
		for (int counter = 0; counter < rows.length(); counter++) {
			Date attendanceDate = Translator.dateValue(rows.getJSONObject(counter).opt("attendancedate"));
			int attendanceTypeId = Translator.integerValue(rows.getJSONObject(counter).opt("attendancetypeid"));
			Object key = rows.getJSONObject(counter).opt("__key__");
			Object[] details = new Object[2];
			details[0] = key;
			details[1] = attendanceTypeId;
			dailyAttendanceMap.put(attendanceDate, details);
		}
	}

	private Map<String, Object> getAttendanceParameters(Object employeeId, String currentYear, String currentMonth, Object employeeScheduleId) throws Exception {
		Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(currentYear, currentMonth);
		String monthFirstDateInString = getDateInString(monthFirstDate);
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
		String monthLastDateInString = getDateInString(monthLastDate);
		int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		double presentDays = 0.0;
		double totalAbsentDays = 0.0;
		double totalLeaves = 0.0;
		double totalExtraWorkingdays = 0.0;
		double totalEarnedLeaves = 0.0;
		double totalCasualLeave = 0.0;
		double totalMedicalLeaves = 0.0;
		double totalSpecialLeaves = 0.0;
		double nonWorkingDays = 0.0;
		int shortAttendanceCount = 0;
		Calendar cal = Calendar.getInstance();
		JSONArray employeeAttendanceArray = getEmployeeAttendance(employeeId, monthFirstDateInString, monthLastDateInString);
		for (int counter = 0; counter < employeeAttendanceArray.length(); counter++) {
			Date attendanceDate = Translator.dateValue(employeeAttendanceArray.getJSONObject(counter).opt("attendancedate"));
			int attendanceTypeId = Translator.integerValue(employeeAttendanceArray.getJSONObject(counter).opt("attendancetypeid"));
			int leaveTypeId = Translator.integerValue(employeeAttendanceArray.getJSONObject(counter).opt("leavetypeid"));
			int paidStatus = Translator.integerValue(employeeAttendanceArray.getJSONObject(counter).opt("paidstatus"));
			int shortLeaveTypeId = Translator.integerValue(employeeAttendanceArray.getJSONObject(counter).opt("attendance_type_short_leave_id"));
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
				presentDays++;
				if (shortLeaveTypeId == HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE) {
					shortAttendanceCount++;
					if (organizationId == 7783) { // Navigant Organization (work approved by CEO on mail given by Kushal to Kapil)
						if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE && shortAttendanceCount > 2) {
							presentDays -= 1.00;
							totalEarnedLeaves += 1.00;
							totalLeaves += 1.00;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE && shortAttendanceCount > 2) {
							presentDays -= 1.00;
							totalCasualLeave += 1.00;
							totalLeaves += 1.00;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE && shortAttendanceCount > 2) {
							presentDays -= 1.00;
							totalMedicalLeaves += 1.00;
							totalLeaves += 1.00;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.SPECIAL_LEAVE && shortAttendanceCount > 2) {
							presentDays -= 1.00;
							totalSpecialLeaves += 1.00;
							totalLeaves += 1.00;
						}
					} else { // Work discussed with Amit Singh and Approved
						if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE && shortAttendanceCount % 3 == 0) {
							presentDays -= 0.50;
							totalEarnedLeaves += 0.50;
							totalLeaves += 0.50;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE && shortAttendanceCount % 3 == 0) {
							presentDays -= 0.50;
							totalCasualLeave += 0.50;
							totalLeaves += 0.50;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE && shortAttendanceCount % 3 == 0) {
							presentDays -= 0.50;
							totalMedicalLeaves += 0.50;
							totalLeaves += 0.50;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.SPECIAL_LEAVE && shortAttendanceCount % 3 == 0) {
							presentDays -= 0.50;
							totalSpecialLeaves += 0.50;
							totalLeaves += 0.50;
						}
					}
				}
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
				totalAbsentDays++;
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
				totalLeaves++;
				if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE) {
					totalEarnedLeaves++;
				}
				if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE) {
					totalCasualLeave++;
				}
				if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE) {
					totalMedicalLeaves++;
				}
				if (leaveTypeId == HRISApplicationConstants.LeaveTypes.SPECIAL_LEAVE) {
					totalSpecialLeaves++;
				}
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
				totalLeaves += 0.50;
				cal.setTime(attendanceDate);
				int weekDayNumeric = cal.get(Calendar.DAY_OF_WEEK);
				int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
				JSONArray offDay = getHalfOffDay(employeeScheduleId, weekDayNumeric, weekOfMonth);
				if (offDay != null && offDay.length() > 0) {
					nonWorkingDays += 0.50;
				} else {
					presentDays += 0.50;
				}
				if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE) {
					totalEarnedLeaves += 0.50;
				}
				if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE) {
					totalCasualLeave += 0.50;
				}
				if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE) {
					totalMedicalLeaves += 0.50;
				}
				if (leaveTypeId == HRISApplicationConstants.LeaveTypes.SPECIAL_LEAVE) {
					totalSpecialLeaves += 0.50;
				}
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HALF_DAY_OFF) {
				nonWorkingDays += 0.50;
				presentDays += 0.50;
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_OFF || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HOLIDAY) {
				nonWorkingDays++;
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
				nonWorkingDays++;
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY && paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_PAYABLE) {
				totalExtraWorkingdays++;
				if (shortLeaveTypeId == HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE) {
					shortAttendanceCount++;
					if (organizationId == 7783) { // Navigant Organization (work approved by CEO on mail given by Kushal to Kapil)
						if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE && shortAttendanceCount > 2) {
							totalExtraWorkingdays -= 1.00;
							totalEarnedLeaves += 1.00;
							totalLeaves += 1.00;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE && shortAttendanceCount > 2) {
							totalExtraWorkingdays -= 1.00;
							totalCasualLeave += 1.00;
							totalLeaves += 1.00;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE && shortAttendanceCount > 2) {
							totalExtraWorkingdays -= 1.00;
							totalMedicalLeaves += 1.00;
							totalLeaves += 1.00;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.SPECIAL_LEAVE && shortAttendanceCount > 2) {
							totalExtraWorkingdays -= 1.00;
							totalSpecialLeaves += 1.00;
							totalLeaves += 1.00;
						}
					} else { // Work discussed with Amit Singh and Approved
						if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE && shortAttendanceCount % 3 == 0) {
							totalExtraWorkingdays -= 0.50;
							totalEarnedLeaves += 0.50;
							totalLeaves += 0.50;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE && shortAttendanceCount % 3 == 0) {
							totalExtraWorkingdays -= 0.50;
							totalCasualLeave += 0.50;
							totalLeaves += 0.50;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE && shortAttendanceCount % 3 == 0) {
							totalExtraWorkingdays -= 0.50;
							totalMedicalLeaves += 0.50;
							totalLeaves += 0.50;
						} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.SPECIAL_LEAVE && shortAttendanceCount % 3 == 0) {
							totalExtraWorkingdays -= 0.50;
							totalSpecialLeaves += 0.50;
							totalLeaves += 0.50;
						}
					}
				}
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF && paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_PAYABLE) {
				totalExtraWorkingdays += 0.50;
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HALF_DAY_ABSENT) {
				totalAbsentDays += 0.50;
				nonWorkingDays += 0.50;
			}
		}

		Map<String, Object> attendanceParameterMap = new HashMap<String, Object>();
		attendanceParameterMap.put(HRISApplicationConstants.AttendanceParameters.TOTAL_PRESENTS, presentDays);
		attendanceParameterMap.put(HRISApplicationConstants.AttendanceParameters.TOTAL_ABSENTS, totalAbsentDays);
		attendanceParameterMap.put(HRISApplicationConstants.AttendanceParameters.TOTAL_LEAVES, totalLeaves);
		attendanceParameterMap.put(HRISApplicationConstants.AttendanceParameters.TOTAL_EXTRAWORKINGDAYS, totalExtraWorkingdays);
		attendanceParameterMap.put(HRISApplicationConstants.AttendanceParameters.TOTAL_EARNED_LEAVES, totalEarnedLeaves);
		attendanceParameterMap.put(HRISApplicationConstants.AttendanceParameters.TOTAL_CASUAL_LEAVES, totalCasualLeave);
		attendanceParameterMap.put(HRISApplicationConstants.AttendanceParameters.TOTAL_MEDICAL_LEAVES, totalMedicalLeaves);
		attendanceParameterMap.put(HRISApplicationConstants.AttendanceParameters.TOTAL_SPECIAL_LEAVES, totalSpecialLeaves);
		attendanceParameterMap.put(HRISApplicationConstants.AttendanceParameters.TOTAL_NON_WORKING_DAYS, nonWorkingDays);
		return attendanceParameterMap;
	}

	public static JSONArray getHalfOffDay(Object employeeScheduleId, int weekDayNumeric, int weekOfMonth) throws Exception {
		String key = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "__" + employeeScheduleId + "__halfdayoff" + weekDayNumeric + "_" + weekOfMonth;
		Object sessionVeriable = CurrentRequest.getVariable(key);
		if (sessionVeriable != null) {
			return (JSONArray) sessionVeriable;
		}
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_monthlyoff");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "scheduleid = " + employeeScheduleId + " and weeknoid = " + weekOfMonth + " and weekdayid = " + weekDayNumeric + " AND off_day_type='Half Day'");
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_monthlyoff");
		CurrentRequest.setRequestVariable(key, rows);
		return rows;
	}

	private JSONArray getEmployeeAttendance(Object employeeId, String monthFirstDateInString, String monthLastDateInString) throws Exception {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("attendancetypeid");
		array.put("leavetypeid");
		array.put("attendancedate");
		array.put("paidstatus");
		array.put("attendance_type_short_leave_id");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate >= '" + monthFirstDateInString + "'" + " and attendancedate <= '" + monthLastDateInString + "' and attendance_status IN(2,3)");
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeeattendance");
		return rows;
	}

	public void updateNewMonthlyAttendanceRecord(Object monthlyAttendanceId, Object employeeId, Object monthId, Object yearId, String currentYear, String currentMonth, Object relievingDateParameter, boolean isFromEmployeeUpdate, Date attendanceDate, boolean isFromMonthlyUpdateAllColumns, HashMap<Integer, Double> leaveTypeOpeningBalance) throws ParseException, Exception {
		Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(currentYear, currentMonth);
		String monthFirstDateInString = getDateInString(monthFirstDate);
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
		String monthLastDateInString = getDateInString(monthLastDate);
		JSONArray employeeArray = getEmployeeRecord(employeeId);
		int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
		if (employeeArrayCount > 0) {
			Object holidayCalendarId = employeeArray.getJSONObject(0).opt("holidaycalendarid");
			Object employeeScheduleId = employeeArray.getJSONObject(0).opt("employeescheduleid");
			Object leavePolicyId = employeeArray.getJSONObject(0).opt("leavepolicyid");
			Calendar cal = Calendar.getInstance();
			cal.setTime(monthFirstDate);
			cal.add(Calendar.MONTH, -1);
			int previousMonthId = (cal.get(Calendar.MONTH) + 1);
			long previousYearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));

			double earnedLeaveBalance = DataTypeUtilities.doubleValue(employeeArray.getJSONObject(0).opt("earnedleavebalance"));
			double casualLeaveBalance = DataTypeUtilities.doubleValue(employeeArray.getJSONObject(0).opt("casualleavebalance"));
			double medicalLeaveBalance = DataTypeUtilities.doubleValue(employeeArray.getJSONObject(0).opt("medicalleavebalance"));
			double specialLeaveBalance = DataTypeUtilities.doubleValue(employeeArray.getJSONObject(0).opt("specialleavebalance"));
			double extraWorkingDayBalance = DataTypeUtilities.doubleValue(employeeArray.getJSONObject(0).opt("extraworkingdaybalance"));
			try {
				JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, previousMonthId, previousYearId, null);
				for (int counter = 0; counter < leaveRegisterArray.length(); counter++) {
					int leaveTypeId = leaveRegisterArray.getJSONObject(counter).optInt("leavetypeid");
					Object leavebalance = leaveRegisterArray.getJSONObject(counter).opt("leavebalance");
					if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE) {
						if (leavebalance != null) {
							earnedLeaveBalance = Translator.doubleValue(leavebalance);
						}
						if (isFromMonthlyUpdateAllColumns && leaveTypeOpeningBalance.containsKey(leaveTypeId)) {
							earnedLeaveBalance = leaveTypeOpeningBalance.get(leaveTypeId);
						}
					}
					if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE) {
						if (leavebalance != null) {
							casualLeaveBalance = Translator.doubleValue(leavebalance);
						}
						if (isFromMonthlyUpdateAllColumns && leaveTypeOpeningBalance.containsKey(leaveTypeId)) {
							casualLeaveBalance = leaveTypeOpeningBalance.get(leaveTypeId);
						}
					}
					if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE) {
						if (leavebalance != null) {
							medicalLeaveBalance = Translator.doubleValue(leavebalance);
						}
						if (isFromMonthlyUpdateAllColumns && leaveTypeOpeningBalance.containsKey(leaveTypeId)) {
							medicalLeaveBalance = leaveTypeOpeningBalance.get(leaveTypeId);
						}
					}
					if (leaveTypeId == HRISApplicationConstants.LeaveTypes.SPECIAL_LEAVE) {
						if (leavebalance != null) {
							specialLeaveBalance = Translator.doubleValue(leavebalance);
						}
						if (isFromMonthlyUpdateAllColumns && leaveTypeOpeningBalance.containsKey(leaveTypeId)) {
							specialLeaveBalance = leaveTypeOpeningBalance.get(leaveTypeId);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			Object joiningDateObject = employeeArray.getJSONObject(0).opt("joiningdate");
			Object relievingDateObject = employeeArray.getJSONObject(0).opt("relievingdate");
			// EmployeeSalaryGenerationServlet.monthId =
			// Translator.integerValue(monthId);
			// EmployeeSalaryGenerationServlet.yearId =
			// Translator.integerValue(yearId);
			// boolean isJoiningOrRelieving = false;
			boolean isJoiningOnFirstDayOfMonth = false;
			Calendar monthFirstDateCalendar = Calendar.getInstance();
			monthFirstDateCalendar.setTime(DataTypeUtilities.checkDateFormat(joiningDateObject));

			Date mfd = DataTypeUtilities.getMonthFirstDate("" + monthFirstDateCalendar.get(Calendar.YEAR), EmployeeReleasingSalaryServlet.getMonthName((monthFirstDateCalendar.get(Calendar.MONTH) + 1)));
			if (mfd.equals(DataTypeUtilities.checkDateFormat(joiningDateObject))) {
				isJoiningOnFirstDayOfMonth = true;
			}
			Date joiningDate = null;
			boolean isJoining = false;
			if (joiningDateObject != null) {
				joiningDate = DataTypeUtilities.checkDateFormat(joiningDateObject);
				if (joiningDate != null && isNewJoineeOrReleaved(joiningDate, monthId, yearId)) {
					isJoining = true;
					monthFirstDate = joiningDate;
					monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(joiningDate);
				}
			}
			Date relievingDate = null;
			if (relievingDateObject != null && relievingDateParameter != null && !isFromEmployeeUpdate) {
				relievingDate = DataTypeUtilities.checkDateFormat(relievingDateObject);
				if (relievingDate != null && isNewJoineeOrReleaved(relievingDate, monthId, yearId)) {
					// isJoiningOrRelieving = true;
					monthLastDate = relievingDate;
					monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(relievingDate);

				}
			}
			if (isFromEmployeeUpdate && relievingDateParameter != null) {
				relievingDate = DataTypeUtilities.checkDateFormat(relievingDateParameter);
				if (relievingDate != null && isNewJoineeOrReleaved(relievingDate, monthId, yearId)) {
					// isJoiningOrRelieving = true;
					monthLastDate = relievingDate;
					monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(relievingDate);

				}
			}
			long numOfDaysInMonthToCreditLeaves = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
			long numOfDaysInMonth = DataTypeUtilities.daysBetween(monthLastDate, monthFirstDate) + 1;
			JSONArray leavePoliciesArray = getLeavePoliciesArray(employeeId, monthFirstDateInString, monthLastDateInString);
			LogUtility.writeError("leavePoliciesArray >> " + leavePoliciesArray);

			if (leavePoliciesArray != null && leavePoliciesArray.length() > 0) {
				creditBalanceIfLeavePolicyDatedDefined(leavePoliciesArray, monthlyAttendanceId, employeeId, monthId, yearId, currentYear, currentMonth, attendanceDate, monthFirstDate, monthFirstDateInString, monthLastDate, monthLastDateInString, holidayCalendarId, employeeScheduleId, leavePolicyId, earnedLeaveBalance, casualLeaveBalance, medicalLeaveBalance, specialLeaveBalance, extraWorkingDayBalance, joiningDateObject, isJoiningOnFirstDayOfMonth, joiningDate, isJoining, numOfDaysInMonthToCreditLeaves, numOfDaysInMonth);

			} else {
				creditBalanceIfLeavePolicyDatedNotDefined(monthlyAttendanceId, employeeId, monthId, yearId, currentYear, currentMonth, attendanceDate, monthFirstDate, monthFirstDateInString, monthLastDate, monthLastDateInString, holidayCalendarId, employeeScheduleId, leavePolicyId, earnedLeaveBalance, casualLeaveBalance, medicalLeaveBalance, specialLeaveBalance, extraWorkingDayBalance, joiningDateObject, isJoiningOnFirstDayOfMonth, joiningDate, isJoining, numOfDaysInMonthToCreditLeaves, numOfDaysInMonth);
			}
		}
	}

	private void creditBalanceIfLeavePolicyDatedDefined(JSONArray leavePoliciesArray, Object monthlyAttendanceId, Object employeeId, Object monthId, Object yearId, String currentYear, String currentMonth, Date attendanceDate, Date monthFirstDate, String monthFirstDateInString, Date monthLastDate, String monthLastDateInString, Object holidayCalendarId, Object employeeScheduleId, Object leavePolicyId, double earnedLeaveBalance, double casualLeaveBalance, double medicalLeaveBalance, double specialLeaveBalance, double extraWorkingDayBalance, Object joiningDateObject, boolean isJoiningOnFirstDayOfMonth, Date joiningDate, boolean isJoining, long numOfDaysInMonthToCreditLeaves, long numOfDaysInMonth) throws Exception {
		double creditEarnedLeaves = 0.0;
		double creditCasualLeave = 0.0;
		double creditMedicalLeave = 0.0;
		int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		boolean isNevigant = false;
		if (currentOrganizationId == 7783) {// organization.toLowerCase().indexOf("navigant") != -1) {
			isNevigant = true;
		}
		for (int leavePolicyCounter = 0; leavePolicyCounter < leavePoliciesArray.length(); leavePolicyCounter++) {
			Object leavePolicyIdCurrent = leavePoliciesArray.getJSONObject(leavePolicyCounter).opt("leavepolicyid");
			Object fromDateLeavePolicyObject = leavePoliciesArray.getJSONObject(leavePolicyCounter).opt("from_date");
			Object toDateLeavePolicyObject = leavePoliciesArray.getJSONObject(leavePolicyCounter).opt("to_date");
			Date fromDateLeavePolicyDate = Translator.dateValue(fromDateLeavePolicyObject);
			Date toDateLeavePolicyDate = Translator.dateValue(toDateLeavePolicyObject);

			JSONArray leaveRuleArray = getLeaveRuleRecord(leavePolicyId, monthFirstDateInString, monthLastDateInString);
			int leaveRuleaArrayCount = (leaveRuleArray == null || leaveRuleArray.length() == 0) ? 0 : leaveRuleArray.length();
			if (leaveRuleaArrayCount > 0) {

				Calendar cal1 = Calendar.getInstance();
				double diff = DataTypeUtilities.monthsBetweenDates(joiningDate, monthLastDate);
				for (int counter = 0; counter < leaveRuleaArrayCount; counter++) {
					long tempNumOfDaysInMonth = DataTypeUtilities.daysBetween(monthLastDate, monthFirstDate) + 1;
					Integer leaveTypeId = (Integer) leaveRuleArray.getJSONObject(counter).opt("leavetypeid");
					Integer creditTypeId = (Integer) leaveRuleArray.getJSONObject(counter).opt("leavecreditid");

					String proportionateBase = Translator.stringValue(leaveRuleArray.getJSONObject(counter).opt("proportionate_base"));
					String accrualBase = Translator.stringValue(leaveRuleArray.getJSONObject(counter).opt("accrual_base"));

					Calendar period = Calendar.getInstance();

					String yearName = "" + period.get(Calendar.YEAR);

					String firstDateOfYearString = "";
					String lastDateOfYearString = "";
					Date firstDateOfYear = null;
					Date lastDateOfYear = null;
					if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {
						if (accrualBase.equals("Financial Year")) {
							firstDateOfYearString = yearName + "-04-01";
							period.setTime(Translator.dateValue(firstDateOfYearString));
							period.add(Calendar.YEAR, 1);
							lastDateOfYearString = period.get(Calendar.YEAR) + "-03-31";
							firstDateOfYear = Translator.dateValue(firstDateOfYearString);
							lastDateOfYear = Translator.dateValue(lastDateOfYearString);
						} else {
							if (accrualBase.equals("Calendar Year")) {
								firstDateOfYearString = yearName + "-01-01";
								lastDateOfYearString = yearName + "-12-31";
								firstDateOfYear = Translator.dateValue(firstDateOfYearString);
								lastDateOfYear = Translator.dateValue(lastDateOfYearString);
							}
						}
					} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT) {
						period.setTime(joiningDate);
						firstDateOfYearString = "" + joiningDateObject;
						period.add(Calendar.MONTH, 6);
						lastDateOfYearString = EmployeeSalaryGenerationServlet.getDateInString(period.getTime());
						firstDateOfYear = Translator.dateValue(firstDateOfYearString);
						lastDateOfYear = Translator.dateValue(lastDateOfYearString);
					} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
						period.setTime(joiningDate);
						firstDateOfYearString = "" + joiningDateObject;
						period.add(Calendar.MONTH, 3);
						lastDateOfYearString = EmployeeSalaryGenerationServlet.getDateInString(period.getTime());
						firstDateOfYear = Translator.dateValue(firstDateOfYearString);
						lastDateOfYear = Translator.dateValue(lastDateOfYearString);
					}
					double noOfLeaves = DataTypeUtilities.doubleValue(leaveRuleArray.getJSONObject(counter).opt("noofleaves"));
					double creditAfterMonths = DataTypeUtilities.doubleValue(leaveRuleArray.getJSONObject(counter).opt("accrueaftermonths"));
					boolean isCreditAfterDefinedMonths = false;
					cal1.setTime(joiningDate);
					if (creditAfterMonths != 0.0) {
						if (!isJoiningOnFirstDayOfMonth && isNevigant) {
							creditAfterMonths += 1.0;
						}
						if (diff > creditAfterMonths) {
							isCreditAfterDefinedMonths = true;
						}
						if (!isNevigant && (diff - 1) == creditAfterMonths) {
							cal1.add(Calendar.MONTH, (int) creditAfterMonths);
							tempNumOfDaysInMonth = DataTypeUtilities.daysBetween(monthLastDate, cal1.getTime()) + 1;
							if (tempNumOfDaysInMonth < 0) {
								tempNumOfDaysInMonth = 0;
							}
						}
					} else {
						isCreditAfterDefinedMonths = true;
					}
					if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE) {
						if (isCreditAfterDefinedMonths) {
							if (creditTypeId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT) {
								creditEarnedLeaves = noOfLeaves / 12;
								creditEarnedLeaves = (float) (creditEarnedLeaves * tempNumOfDaysInMonth) / numOfDaysInMonthToCreditLeaves;
							} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
								JSONArray employeeYearlyLeaveAccrualArray = getEmployeeYearlyLeaveAccrualArray(employeeId, firstDateOfYearString, lastDateOfYearString, leaveTypeId);
								if (employeeYearlyLeaveAccrualArray == null || employeeYearlyLeaveAccrualArray.length() == 0) {
									if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {
										if (proportionateBase.equals("At Actual")) {
											creditEarnedLeaves = noOfLeaves;
										} else if (proportionateBase.equals("Joining Base")) {
											if (joiningDate.after(firstDateOfYear) && joiningDate.before(lastDateOfYear)) {
												long monthsBetweenDates = DataTypeUtilities.monthsBetweenDates(joiningDate, lastDateOfYear);
												creditEarnedLeaves = ((noOfLeaves / 12) * monthsBetweenDates);
											} else {
												creditEarnedLeaves = noOfLeaves;
											}
										}
										insertEmployeeYearlyLeaveAccrual(employeeId, EmployeeSalaryGenerationServlet.getDateInString(attendanceDate), leaveTypeId);
									} else if (isJoining) {
										creditEarnedLeaves = noOfLeaves;
									}
								} else {
									creditEarnedLeaves = 0.0;
								}
							}

						}
						JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, monthId, yearId, leaveTypeId);
						if (leaveRegisterArray == null || leaveRegisterArray.length() == 0) {
							insertNewLeaveReister(employeeId, monthId, yearId, leaveTypeId, creditEarnedLeaves, monthFirstDate);
						}
					} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE) {
						if (isCreditAfterDefinedMonths) {
							if (creditTypeId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT) {
								creditCasualLeave = noOfLeaves / 12;
								creditCasualLeave = (float) (creditCasualLeave * tempNumOfDaysInMonth) / numOfDaysInMonthToCreditLeaves;
							} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
								JSONArray employeeYearlyLeaveAccrualArray = getEmployeeYearlyLeaveAccrualArray(employeeId, firstDateOfYearString, lastDateOfYearString, leaveTypeId);
								if (employeeYearlyLeaveAccrualArray == null || employeeYearlyLeaveAccrualArray.length() == 0) {
									if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {
										if (proportionateBase.equals("At Actual")) {
											creditCasualLeave = noOfLeaves;
										} else if (proportionateBase.equals("Joining Base")) {
											if (joiningDate.after(firstDateOfYear) && joiningDate.before(lastDateOfYear)) {
												long monthsBetweenDates = DataTypeUtilities.monthsBetweenDates(joiningDate, lastDateOfYear);
												creditCasualLeave = ((noOfLeaves / 12) * monthsBetweenDates);
											} else {
												creditCasualLeave = noOfLeaves;
											}
										}
										insertEmployeeYearlyLeaveAccrual(employeeId, EmployeeSalaryGenerationServlet.getDateInString(attendanceDate), leaveTypeId);
									} else if (isJoining) {
										creditCasualLeave = noOfLeaves;
									}
								} else {
									creditCasualLeave = 0.0;
								}
							}
						}
						JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, monthId, yearId, leaveTypeId);
						if (leaveRegisterArray == null || leaveRegisterArray.length() == 0) {
							insertNewLeaveReister(employeeId, monthId, yearId, leaveTypeId, creditCasualLeave, monthFirstDate);
						}
					} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE) {
						if (isCreditAfterDefinedMonths) {
							if (creditTypeId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT) {
								creditMedicalLeave = noOfLeaves / 12;
								creditMedicalLeave = (float) (creditMedicalLeave * tempNumOfDaysInMonth) / numOfDaysInMonthToCreditLeaves;
							} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
								JSONArray employeeYearlyLeaveAccrualArray = getEmployeeYearlyLeaveAccrualArray(employeeId, firstDateOfYearString, lastDateOfYearString, leaveTypeId);
								if (employeeYearlyLeaveAccrualArray == null || employeeYearlyLeaveAccrualArray.length() == 0) {
									if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {
										if (proportionateBase.equals("At Actual")) {
											creditMedicalLeave = noOfLeaves;
										} else if (proportionateBase.equals("Joining Base")) {
											if (joiningDate.after(firstDateOfYear) && joiningDate.before(lastDateOfYear)) {
												long monthsBetweenDates = DataTypeUtilities.monthsBetweenDates(joiningDate, lastDateOfYear);
												creditMedicalLeave = ((noOfLeaves / 12) * monthsBetweenDates);
											} else {
												creditMedicalLeave = noOfLeaves;
											}
										}
										insertEmployeeYearlyLeaveAccrual(employeeId, EmployeeSalaryGenerationServlet.getDateInString(attendanceDate), leaveTypeId);
									} else if (isJoining) {
										creditMedicalLeave = noOfLeaves;
									}
								} else {
									creditMedicalLeave = 0.0;
								}
							}
						}
						JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, monthId, yearId, leaveTypeId);
						if (leaveRegisterArray == null || leaveRegisterArray.length() == 0) {
							insertNewLeaveReister(employeeId, monthId, yearId, leaveTypeId, creditMedicalLeave, monthFirstDate);
						}
					}
				}
				Map<String, Object> attendanceParameterMap = getAttendanceParameters(employeeId, currentYear, currentMonth, employeeScheduleId);
				if (attendanceParameterMap != null && attendanceParameterMap.size() > 0) {

					Object totalPresentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_PRESENTS);
					Object totalAbsentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_ABSENTS);
					Object totalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_LEAVES);
					Object totalExtraWorkingdays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EXTRAWORKINGDAYS);

					Object totalEarnedLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EARNED_LEAVES);
					Object totalCasualLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_CASUAL_LEAVES);
					Object totalMedicalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_MEDICAL_LEAVES);
					Object totalSpecialLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_SPECIAL_LEAVES);
					long offDays = getOffDays(employeeId, employeeScheduleId, monthFirstDate, monthLastDate);
					double monthlyHolidays = getNumberOfHolidays(employeeId, holidayCalendarId, monthFirstDateInString, monthLastDateInString);
					// long actualNonWorkingDays =
					// Translator.longValue(attendanceParameterMap.get(HRISApplicationConstants.TOTAL_NON_WORKING_DAY_IN_MONTH));
					Object actualNonWorkingDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_NON_WORKING_DAYS);
					long nonWorkingDays = getActualNonWorkingDays(employeeId, employeeScheduleId, holidayCalendarId, monthFirstDate, monthLastDate, Integer.parseInt(currentYear), (Integer) monthId, monthFirstDateInString, monthLastDateInString);
					long totalWorkingDays = numOfDaysInMonth - nonWorkingDays;
					updateNewMonthEmployeeAttendance(monthlyAttendanceId, employeeId, yearId, monthId, totalPresentDays, totalAbsentDays, totalLeaves, totalExtraWorkingdays, creditEarnedLeaves, creditCasualLeave, creditMedicalLeave, monthlyHolidays, offDays, actualNonWorkingDays, totalWorkingDays, earnedLeaveBalance, casualLeaveBalance, medicalLeaveBalance, specialLeaveBalance, extraWorkingDayBalance, totalEarnedLeaves, totalCasualLeaves, totalMedicalLeaves, totalSpecialLeaves);
				}
			}

		}

	}

	private void creditBalanceIfLeavePolicyDatedNotDefined(Object monthlyAttendanceId, Object employeeId, Object monthId, Object yearId, String currentYear, String currentMonth, Date attendanceDate, Date monthFirstDate, String monthFirstDateInString, Date monthLastDate, String monthLastDateInString, Object holidayCalendarId, Object employeeScheduleId, Object leavePolicyId, double earnedLeaveBalance, double casualLeaveBalance, double medicalLeaveBalance, double specialLeaveBalance, double extraWorkingDayBalance, Object joiningDateObject, boolean isJoiningOnFirstDayOfMonth, Date joiningDate, boolean isJoining, long numOfDaysInMonthToCreditLeaves, long numOfDaysInMonth) throws JSONException, Exception, ParseException {
		double creditEarnedLeaves = 0.0;
		double creditCasualLeave = 0.0;
		double creditMedicalLeave = 0.0;
		JSONArray leaveRuleArray = getLeaveRuleRecord(leavePolicyId, monthFirstDateInString, monthLastDateInString);
		int leaveRuleaArrayCount = (leaveRuleArray == null || leaveRuleArray.length() == 0) ? 0 : leaveRuleArray.length();
		if (leaveRuleaArrayCount > 0) {

			Calendar cal1 = Calendar.getInstance();
			double diff = DataTypeUtilities.monthsBetweenDates(joiningDate, monthLastDate);
			int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			boolean isNevigant = false;
			if (currentOrganizationId == 7783) {// organization.toLowerCase().indexOf("navigant") != -1) {
				isNevigant = true;
			}
			for (int counter = 0; counter < leaveRuleaArrayCount; counter++) {
				long tempNumOfDaysInMonth = DataTypeUtilities.daysBetween(monthLastDate, monthFirstDate) + 1;
				Integer leaveTypeId = (Integer) leaveRuleArray.getJSONObject(counter).opt("leavetypeid");
				Integer creditTypeId = (Integer) leaveRuleArray.getJSONObject(counter).opt("leavecreditid");

				String proportionateBase = Translator.stringValue(leaveRuleArray.getJSONObject(counter).opt("proportionate_base"));
				String accrualBase = Translator.stringValue(leaveRuleArray.getJSONObject(counter).opt("accrual_base"));

				Calendar period = Calendar.getInstance();

				String yearName = "" + period.get(Calendar.YEAR);

				String firstDateOfYearString = "";
				String lastDateOfYearString = "";
				Date firstDateOfYear = null;
				Date lastDateOfYear = null;
				if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {
					if (accrualBase.equals("Financial Year")) {
						firstDateOfYearString = yearName + "-04-01";
						period.setTime(Translator.dateValue(firstDateOfYearString));
						period.add(Calendar.YEAR, 1);
						lastDateOfYearString = period.get(Calendar.YEAR) + "-03-31";
						firstDateOfYear = Translator.dateValue(firstDateOfYearString);
						lastDateOfYear = Translator.dateValue(lastDateOfYearString);
					} else {
						if (accrualBase.equals("Calendar Year")) {
							firstDateOfYearString = yearName + "-01-01";
							lastDateOfYearString = yearName + "-12-31";
							firstDateOfYear = Translator.dateValue(firstDateOfYearString);
							lastDateOfYear = Translator.dateValue(lastDateOfYearString);
						}
					}
				} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT) {
					period.setTime(joiningDate);
					firstDateOfYearString = "" + joiningDateObject;
					period.add(Calendar.MONTH, 6);
					lastDateOfYearString = EmployeeSalaryGenerationServlet.getDateInString(period.getTime());
					firstDateOfYear = Translator.dateValue(firstDateOfYearString);
					lastDateOfYear = Translator.dateValue(lastDateOfYearString);
				} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
					period.setTime(joiningDate);
					firstDateOfYearString = "" + joiningDateObject;
					period.add(Calendar.MONTH, 3);
					lastDateOfYearString = EmployeeSalaryGenerationServlet.getDateInString(period.getTime());
					firstDateOfYear = Translator.dateValue(firstDateOfYearString);
					lastDateOfYear = Translator.dateValue(lastDateOfYearString);
				}
				// if (firstDateOfYear.before(fromDateLeavePolicyDate)) {
				// firstDateOfYear.setTime(fromDateLeavePolicyDate.getTime());
				// }
				// if (lastDateOfYear.after(toDateLeavePolicyDate)) {
				// lastDateOfYear.setTime(toDateLeavePolicyDate.getTime());
				// }
				// Date monthLastDateTemp = new Date();
				// monthLastDateTemp.setTime(monthLastDate.getTime());
				// Date monthFirstDateTemp = new Date();
				// monthFirstDateTemp.setTime(monthFirstDate.getTime());
				// if (monthLastDateTemp.after(toDateLeavePolicyDate)) {
				// monthLastDateTemp.setTime(toDateLeavePolicyDate.getTime());
				// }
				double noOfLeaves = DataTypeUtilities.doubleValue(leaveRuleArray.getJSONObject(counter).opt("noofleaves"));
				double creditAfterMonths = DataTypeUtilities.doubleValue(leaveRuleArray.getJSONObject(counter).opt("accrueaftermonths"));
				boolean isCreditAfterDefinedMonths = false;
				cal1.setTime(joiningDate);
				if (creditAfterMonths != 0.0) {
					if (!isJoiningOnFirstDayOfMonth && isNevigant) {
						creditAfterMonths += 1.0;
					}
					if (diff > creditAfterMonths) {
						isCreditAfterDefinedMonths = true;
					}
					if (!isNevigant && (diff - 1) == creditAfterMonths) {
						cal1.add(Calendar.MONTH, (int) creditAfterMonths);
						tempNumOfDaysInMonth = DataTypeUtilities.daysBetween(monthLastDate, cal1.getTime()) + 1;
						if (tempNumOfDaysInMonth < 0) {
							tempNumOfDaysInMonth = 0;
						}
					}
				} else {
					isCreditAfterDefinedMonths = true;
				}
				if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE) {
					if (isCreditAfterDefinedMonths) {
						if (creditTypeId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT) {
							creditEarnedLeaves = noOfLeaves / 12;
							creditEarnedLeaves = (float) (creditEarnedLeaves * tempNumOfDaysInMonth) / numOfDaysInMonthToCreditLeaves;
						} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
							JSONArray employeeYearlyLeaveAccrualArray = getEmployeeYearlyLeaveAccrualArray(employeeId, firstDateOfYearString, lastDateOfYearString, leaveTypeId);
							if (employeeYearlyLeaveAccrualArray == null || employeeYearlyLeaveAccrualArray.length() == 0) {
								if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {
									if (proportionateBase.equals("At Actual")) {
										creditEarnedLeaves = noOfLeaves;
									} else if (proportionateBase.equals("Joining Base")) {
										if (joiningDate.after(firstDateOfYear) && joiningDate.before(lastDateOfYear)) {
											long monthsBetweenDates = DataTypeUtilities.monthsBetweenDates(joiningDate, lastDateOfYear);
											creditEarnedLeaves = ((noOfLeaves / 12) * monthsBetweenDates);
										} else {
											creditEarnedLeaves = noOfLeaves;
										}
									}
									insertEmployeeYearlyLeaveAccrual(employeeId, EmployeeSalaryGenerationServlet.getDateInString(attendanceDate), leaveTypeId);
								} else if (isJoining) {
									creditEarnedLeaves = noOfLeaves;
								}
							} else {
								creditEarnedLeaves = 0.0;
							}
						}

					}
					JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, monthId, yearId, leaveTypeId);
					if (leaveRegisterArray == null || leaveRegisterArray.length() == 0) {
						insertNewLeaveReister(employeeId, monthId, yearId, leaveTypeId, creditEarnedLeaves, monthFirstDate);
					}
				} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE) {
					if (isCreditAfterDefinedMonths) {
						if (creditTypeId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT) {
							creditCasualLeave = noOfLeaves / 12;
							creditCasualLeave = (float) (creditCasualLeave * tempNumOfDaysInMonth) / numOfDaysInMonthToCreditLeaves;
						} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
							JSONArray employeeYearlyLeaveAccrualArray = getEmployeeYearlyLeaveAccrualArray(employeeId, firstDateOfYearString, lastDateOfYearString, leaveTypeId);
							if (employeeYearlyLeaveAccrualArray == null || employeeYearlyLeaveAccrualArray.length() == 0) {
								if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {
									if (proportionateBase.equals("At Actual")) {
										creditCasualLeave = noOfLeaves;
									} else if (proportionateBase.equals("Joining Base")) {
										if (joiningDate.after(firstDateOfYear) && joiningDate.before(lastDateOfYear)) {
											long monthsBetweenDates = DataTypeUtilities.monthsBetweenDates(joiningDate, lastDateOfYear);
											creditCasualLeave = ((noOfLeaves / 12) * monthsBetweenDates);
										} else {
											creditCasualLeave = noOfLeaves;
										}
									}
									insertEmployeeYearlyLeaveAccrual(employeeId, EmployeeSalaryGenerationServlet.getDateInString(attendanceDate), leaveTypeId);
								} else if (isJoining) {
									creditCasualLeave = noOfLeaves;
								}
							} else {
								creditCasualLeave = 0.0;
							}
						}
					}
					JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, monthId, yearId, leaveTypeId);
					if (leaveRegisterArray == null || leaveRegisterArray.length() == 0) {
						insertNewLeaveReister(employeeId, monthId, yearId, leaveTypeId, creditCasualLeave, monthFirstDate);
					}
				} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE) {
					if (isCreditAfterDefinedMonths) {
						if (creditTypeId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT) {
							creditMedicalLeave = noOfLeaves / 12;
							creditMedicalLeave = (float) (creditMedicalLeave * tempNumOfDaysInMonth) / numOfDaysInMonthToCreditLeaves;
						} else if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || creditTypeId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
							JSONArray employeeYearlyLeaveAccrualArray = getEmployeeYearlyLeaveAccrualArray(employeeId, firstDateOfYearString, lastDateOfYearString, leaveTypeId);
							if (employeeYearlyLeaveAccrualArray == null || employeeYearlyLeaveAccrualArray.length() == 0) {
								if (creditTypeId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {
									if (proportionateBase.equals("At Actual")) {
										creditMedicalLeave = noOfLeaves;
									} else if (proportionateBase.equals("Joining Base")) {
										if (joiningDate.after(firstDateOfYear) && joiningDate.before(lastDateOfYear)) {
											long monthsBetweenDates = DataTypeUtilities.monthsBetweenDates(joiningDate, lastDateOfYear);
											creditMedicalLeave = ((noOfLeaves / 12) * monthsBetweenDates);
										} else {
											creditMedicalLeave = noOfLeaves;
										}
									}
									insertEmployeeYearlyLeaveAccrual(employeeId, EmployeeSalaryGenerationServlet.getDateInString(attendanceDate), leaveTypeId);
								} else if (isJoining) {
									creditMedicalLeave = noOfLeaves;
								}
							} else {
								creditMedicalLeave = 0.0;
							}
						}
					}
					JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, monthId, yearId, leaveTypeId);
					if (leaveRegisterArray == null || leaveRegisterArray.length() == 0) {
						insertNewLeaveReister(employeeId, monthId, yearId, leaveTypeId, creditMedicalLeave, monthFirstDate);
					}
				}
			}
			Map<String, Object> attendanceParameterMap = getAttendanceParameters(employeeId, currentYear, currentMonth, employeeScheduleId);
			if (attendanceParameterMap != null && attendanceParameterMap.size() > 0) {

				Object totalPresentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_PRESENTS);
				Object totalAbsentDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_ABSENTS);
				Object totalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_LEAVES);
				Object totalExtraWorkingdays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EXTRAWORKINGDAYS);

				Object totalEarnedLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_EARNED_LEAVES);
				Object totalCasualLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_CASUAL_LEAVES);
				Object totalMedicalLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_MEDICAL_LEAVES);
				Object totalSpecialLeaves = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_SPECIAL_LEAVES);
				long offDays = getOffDays(employeeId, employeeScheduleId, monthFirstDate, monthLastDate);
				double monthlyHolidays = getNumberOfHolidays(employeeId, holidayCalendarId, monthFirstDateInString, monthLastDateInString);
				// long actualNonWorkingDays =
				// Translator.longValue(attendanceParameterMap.get(HRISApplicationConstants.TOTAL_NON_WORKING_DAY_IN_MONTH));
				Object actualNonWorkingDays = attendanceParameterMap.get(HRISApplicationConstants.AttendanceParameters.TOTAL_NON_WORKING_DAYS);
				long nonWorkingDays = getActualNonWorkingDays(employeeId, employeeScheduleId, holidayCalendarId, monthFirstDate, monthLastDate, Integer.parseInt(currentYear), (Integer) monthId, monthFirstDateInString, monthLastDateInString);
				long totalWorkingDays = numOfDaysInMonth - nonWorkingDays;
				updateNewMonthEmployeeAttendance(monthlyAttendanceId, employeeId, yearId, monthId, totalPresentDays, totalAbsentDays, totalLeaves, totalExtraWorkingdays, creditEarnedLeaves, creditCasualLeave, creditMedicalLeave, monthlyHolidays, offDays, actualNonWorkingDays, totalWorkingDays, earnedLeaveBalance, casualLeaveBalance, medicalLeaveBalance, specialLeaveBalance, extraWorkingDayBalance, totalEarnedLeaves, totalCasualLeaves, totalMedicalLeaves, totalSpecialLeaves);
			}
		}
	}

	private JSONArray getLeavePoliciesArray(Object employeeId, String monthFirstDateInString, String monthLastDateInString) throws Exception {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONArray optionFilters = new JSONArray();
		String optionalFilters = "employeeid = {employee_id} and from_date <= {last_date} and to_date >= {last_date}";
		optionFilters.put(optionalFilters);
		optionalFilters = "employeeid = {employee_id} and to_date >= {first_date} and to_date <= {last_date}";
		optionFilters.put(optionalFilters);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_leave_policy_history");
		JSONArray array = new JSONArray();
		JSONObject parameters = new JSONObject();
		parameters.put("employee_id", employeeId);
		parameters.put("first_date", monthFirstDateInString);
		parameters.put("last_date", monthLastDateInString);
		array.put("__key__");
		array.put("leavepolicyid");
		array.put("from_date");
		array.put("to_date");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.PARAMETERS, parameters);

		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "to_date");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		query.put(Data.Query.ORDERS, new JSONArray().put(order));
		query.put(Data.Query.FILTERS, "employeeid = {employee_id} and from_date >= {first_date} and from_date <= {last_date}");
		query.put(Data.Query.OPTION_FILTERS, optionFilters);
		JSONObject attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_employees_leave_policy_history");

		return rows;
	}

	private void insertEmployeeYearlyLeaveAccrual(Object employeeId, String attendanceDateInString, Integer leaveTypeId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.LeaveAccrualYearly.RESOURCE);
		JSONObject row = new JSONObject();

		row.put(HRISApplicationConstants.LeaveAccrualYearly.EMPLOYEE_ID, employeeId);
		row.put(HRISApplicationConstants.LeaveAccrualYearly.DATE, attendanceDateInString);
		row.put("leavetypeid", leaveTypeId);

		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private JSONArray getEmployeeYearlyLeaveAccrualArray(Object employeeId, String firstDateOfYear, String lastDateOfYear, Integer leaveTypeId) throws Exception {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.LeaveAccrualYearly.RESOURCE);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.LeaveAccrualYearly.EMPLOYEE_ID + " = " + employeeId + " AND " + HRISApplicationConstants.LeaveAccrualYearly.DATE + " >= '" + firstDateOfYear + "' AND " + HRISApplicationConstants.LeaveAccrualYearly.DATE + " <= '" + lastDateOfYear + "' AND leavetypeid = " + leaveTypeId);
		JSONObject attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray(HRISApplicationConstants.LeaveAccrualYearly.RESOURCE);
		return rows;
	}

	public static boolean isNewJoineeOrReleaved(Date joiningDate, Object monthId, Object yearId) {
		try {
			if (joiningDate != null) {
				String currentMonth = DataTypeUtilities.getCurrentMonth(joiningDate);
				long joiningMonthId = ((Number) EmployeeMonthlyAttendance.getMonthId(currentMonth)).longValue();
				String joiningYear = DataTypeUtilities.getCurrentYear(joiningDate);
				long joiningYearId = ((Number) EmployeeMonthlyAttendance.getYearId(joiningYear)).longValue();
				if (joiningMonthId == Long.parseLong("" + monthId) && joiningYearId == Long.parseLong("" + yearId)) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		} catch (JSONException e) {
			throw new RuntimeException("Some JSON error occured while checking new joinee condition: " + e.getMessage());
		} catch (Exception e) {
			throw new RuntimeException("Some error occured while checking new joinee condition: " + e.getMessage());
		}
	}

	private void insertNewLeaveReister(Object employeeId, Object monthId, Object yearId, Object leaveTypeId, Object creditLeaves, Date monthFirstDate) throws Exception {

		Calendar cal = Calendar.getInstance();
		cal.setTime(monthFirstDate);
		cal.add(Calendar.MONTH, -1);
		long monthIdTemp = cal.get(Calendar.MONTH) + 1;
		long yearIdTemp = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
		JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, monthIdTemp, yearIdTemp, leaveTypeId);
		double leaveBalance = 0.0;
		if (leaveRegisterArray != null && leaveRegisterArray.length() > 0) {
			leaveBalance = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("leavebalance"));
		}
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "leaveregister");
		JSONObject row = new JSONObject();

		row.put("employeeid", employeeId);
		row.put("yearid", yearId);
		row.put("monthid", monthId);
		row.put("credit", creditLeaves);
		row.put("openingbalance", leaveBalance);
		row.put("leavebalance", leaveBalance);
		row.put("closingbalance", leaveBalance);
		row.put("leavetypeid", leaveTypeId);

		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public JSONArray getLeaveRegisterArray(Object employeeId, Object monthId, Object yearId, Object leaveTypeId) throws JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		String filters = "";
		JSONObject query = new JSONObject();
		if (leaveTypeId != null) {
			filters = " and leavetypeid = " + leaveTypeId;
			query.put(Data.Query.MAX_ROWS, 1);
		}
		query.put(Data.Query.RESOURCE, "leaveregister");
		JSONArray array = new JSONArray();

		array.put("__key__");
		array.put("leavebalance");
		array.put("closingbalance");
		array.put("remarks");
		array.put("leavetypeid");
		array.put("openingbalance");
		array.put("newrecordmonthid");
		array.put("newrecordyearid");
		array.put("credit");
		array.put("debit");
		array.put("leavebalance");
		array.put("earnleaves");
		array.put("leavetypeid");
		array.put("totalewds");
		array.put("finalleavebalance");
		query.put(Data.Query.COLUMNS, array);

		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "__key__");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		query.put(Data.Query.ORDERS, new JSONArray().put(order));
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId + filters);
		JSONObject attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("leaveregister");

		return rows;
	}

	// private double getAttendanceCount(Object employeeId, String
	// monthFirstDate, String monthLastDate, Integer leavetypeId, int
	// attendaceTypeId, double[] shortLeaveCounts) throws JSONException {
	// String filter;
	// if (leavetypeId == null) {
	// filter = "employeeid = " + employeeId + " and attendancetypeid = " +
	// attendaceTypeId + " and attendancedate >= '" + monthFirstDate +
	// "' and  attendancedate <= '" + monthLastDate + "'";
	// } else {
	// filter = "employeeid = " + employeeId + " and attendancetypeid = " +
	// attendaceTypeId + " and attendancedate >= '" + monthFirstDate +
	// "' and  attendancedate <= '" + monthLastDate + "' and leavetypeid = " +
	// leavetypeId;
	// }
	// if (attendaceTypeId ==
	// HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY || attendaceTypeId ==
	// HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
	// filter += " and paidstatus = " + HRISApplicationConstants.LEAVE_EWD_PAID;
	// }
	// JSONObject query = new JSONObject();
	// query.put(Data.Query.RESOURCE, "employeeattendance");
	// JSONArray columnArray = new JSONArray();
	// columnArray.put("__key__");
	// columnArray.put("shortleaves");
	// columnArray.put("leavetypeid");
	// columnArray.put("attendance_type_short_leave_id");
	// query.put(Data.Query.COLUMNS, columnArray);
	// query.put(Data.Query.FILTERS, filter);
	// if (attendaceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT) {
	// query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId +
	// " and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_TOUR +
	// "," + HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME +
	// ") and attendancedate >= '" + monthFirstDate +
	// "' and  attendancedate <= '" + monthLastDate + "'");
	// } else if (attendaceTypeId == HRISApplicationConstants.ATTENDANCE_OFF) {
	// query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId +
	// " and attendancetypeid in (" +
	// HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY + "," +
	// HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF +
	// ") and attendancedate >= '" + monthFirstDate +
	// "' and  attendancedate <= '" + monthLastDate + "'");
	// } else if (attendaceTypeId ==
	// HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE) {
	// query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId +
	// " and attendancetypeid in (" +
	// HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE + "," +
	// HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY +
	// ") and attendancedate >= '" + monthFirstDate +
	// "' and  attendancedate <= '" + monthLastDate + "' and leavetypeid = " +
	// leavetypeId);
	// }
	// JSONObject object;
	// object = ResourceEngine.query(query);
	// JSONArray rows = object.getJSONArray("employeeattendance");
	// double rowCount = (rows == null || rows.length() == 0) ? 0 :
	// rows.length();
	// if (shortLeaveCounts != null) {
	// int shortAttendanceCount = 0;
	// for (int counter = 0; counter < rows.length(); counter++) {
	// int shortAttendanceId =
	// Translator.integerValue(rows.getJSONObject(counter).opt("attendance_type_short_leave_id"));
	// int leaveTypeId =
	// Translator.integerValue(rows.getJSONObject(counter).opt("leavetypeid"));
	// // double shortLeaves =
	// Translator.doubleValue(rows.getJSONObject(counter).opt("attendance_type_short_leave_id"));
	// if (shortAttendanceId ==
	// HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE) {
	// shortAttendanceCount++;
	// if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE &&
	// shortAttendanceCount % 3 == 0) {
	// // shortLeaveCounts[0] += shortLeaves;
	// shortLeaveCounts[0] += 0.50;
	// } else if (leaveTypeId ==
	// HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE && shortAttendanceCount
	// % 3 == 0) {
	// // shortLeaveCounts[1] += shortLeaves;
	// shortLeaveCounts[1] += 0.50;
	// } else if (leaveTypeId ==
	// HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE && shortAttendanceCount
	// % 3 == 0) {
	// // shortLeaveCounts[2] += shortLeaves;
	// shortLeaveCounts[2] += 0.50;
	// } else if (leaveTypeId ==
	// HRISApplicationConstants.LeaveTypes.SPECIAL_LEAVE && shortAttendanceCount
	// % 3 == 0) {
	// // shortLeaveCounts[3] += shortLeaves;
	// shortLeaveCounts[3] += 0.50;
	// }
	// }
	// }
	// rowCount = rowCount - (shortLeaveCounts[0] + shortLeaveCounts[1] +
	// shortLeaveCounts[2] + shortLeaveCounts[3]);
	// }
	// return rowCount;
	// }

	private void updateNewMonthEmployeeAttendance(Object monthlyAttendanceId, Object employeeId, Object yearId, Object monthId, Object totalPresentDays, Object totalAbsentDays, Object totalLeaves, Object totalExtraWorkingdays, double creditEarnedLeaves, double creditCasualLeave, double creditMedicalLeave, double monthlyHolidays, long offDays, Object actualNonWorkingDays, long totalWorkingDays, double earnedLeaveBalance, double casualLeaveBalance, double medicalLeaveBalance, double specialLeaveBalance, double extraWorkingDayBalance, Object totalEarnedLeaves, Object totalCasualLeaves, Object totalMedicalLeaves, Object totalSpecialLeaves) throws JSONException {
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		Object daysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(Translator.dateValue(TODAY_DATE));
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		if (monthlyAttendanceId != null) {
			row.put(Updates.KEY, monthlyAttendanceId);
		}
		row.put("employeeid", employeeId);
		row.put("yearid", yearId);
		row.put("monthid", monthId);

		row.put("presentdays", totalPresentDays);
		row.put("absents", totalAbsentDays);
		row.put("leaves", totalLeaves);
		row.put("extraworkingdays", totalExtraWorkingdays);

		row.put("creditearnedleaves", creditEarnedLeaves);
		row.put("creditcasualleaves", creditCasualLeave);
		row.put("creditmedicalleaves", creditMedicalLeave);

		row.put("usedearnedleaves", totalEarnedLeaves);
		row.put("usedcasualleaves", totalCasualLeaves);
		row.put("usedmedicalleaves", totalMedicalLeaves);
		row.put("usedspecialleaves", totalSpecialLeaves);

		row.put("holidays", monthlyHolidays);
		row.put("nonworkingdays", offDays);
		row.put("actualnonworkingdays", actualNonWorkingDays);
		row.put("totalworkingdays", totalWorkingDays);

		row.put("earnedleaveopeningbalance", earnedLeaveBalance);
		row.put("casualleaveopeningbalance", casualLeaveBalance);
		row.put("medicalleaveopeningbalance", medicalLeaveBalance);
		row.put("specialleaveopeningbalance", specialLeaveBalance);
		row.put("extraworkingdayopeningbalance", extraWorkingDayBalance);
		row.put("days_in_month", daysInMonth);

		row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static void updateEmployeeMonthlyAttendance(Object monthlyAttendanceId, Object totalPresentDays, Object totalAbsentDays, Object totalLeaves, Object totalExtraWorkingdays, Object totalEarnedLeaves, Object totalCasualLeaves, Object totalMedicalLeaves, Object totalSpecialLeaves, Object totalNonWorkingDays) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();

		row.put("__key__", monthlyAttendanceId);

		row.put("presentdays", totalPresentDays);
		row.put("absents", totalAbsentDays);
		row.put("leaves", totalLeaves);
		row.put("extraworkingdays", totalExtraWorkingdays);

		row.put("usedearnedleaves", totalEarnedLeaves);
		row.put("usedcasualleaves", totalCasualLeaves);
		row.put("usedmedicalleaves", totalMedicalLeaves);
		row.put("usedspecialleaves", totalSpecialLeaves);
		row.put("actualnonworkingdays", totalNonWorkingDays);

		row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static String getDateInString(Date date) {
		if (date != null) {
			return new SimpleDateFormat("yyyy-MM-dd").format(date);
		} else {
			return "";
		}
	}

	public JSONArray getEmployeeMonthlyAttendanceRecord(Object employeeId, Object yearId, Object monthId) throws JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("ismanualupdate");
		array.put("presentdays");
		array.put("leaves");
		array.put("usedearnedleaves");
		array.put("usedcasualleaves");
		array.put("usedmedicalleaves");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeemonthlyattendance");
		return rows;
	}

	public static Object getMonthId(String currentMonth) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_months");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "name = '" + currentMonth + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("organization_months");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			Object monthId = rows.getJSONObject(0).opt("__key__");
			return monthId;
		}
		return null;
	}

	public static Object getYearId(String yearName) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_years");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "name = '" + yearName + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("organization_years");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			Object yearId = rows.getJSONObject(0).opt("__key__");
			return yearId;
		}
		return null;
	}

	public static int getActualNonWorkingDaysForSandWich(Object employeeId, Object employeeScheduleId, Object holidayCalendarId, Date monthFirstDate, Date monthLastDate) throws JSONException, ParseException {
		int totalOffForSandwich = 0;

		if (employeeScheduleId != null && holidayCalendarId != null) {
			long dayBetween = DataTypeUtilities.daysBetween(monthLastDate, monthFirstDate);
			for (int counter1 = 0; counter1 < dayBetween + 1; counter1++) {
				Calendar cal2 = new GregorianCalendar();
				cal2.setTime(monthFirstDate);
				cal2.add(Calendar.DATE, counter1);
				Date date = cal2.getTime();
				String dateInString = getDateInString(date);
				SimpleDateFormat simpleDateformat = new SimpleDateFormat("EEEE");
				String weekDay = simpleDateformat.format(date);
				String weekDayInLower = weekDay.toLowerCase();
				boolean isOffOrHoliday = false;
				boolean isWeekDay = getWeekDayFromSchedule(weekDayInLower, employeeScheduleId);
				if (!isWeekDay) {
					isOffOrHoliday = true;
					totalOffForSandwich++;
				}
				int count = getHolidayRecordsForSandwich(holidayCalendarId, dateInString);
				if (isWeekDay && count > 0) {
					isOffOrHoliday = true;
					totalOffForSandwich++;
				}
				JSONArray employeeAttendanceRecord = getEmployeeAttendanceRecord(employeeId, dateInString);
				if (employeeAttendanceRecord.length() > 0) {
					int attendanceTypeId = Translator.integerValue(employeeAttendanceRecord.getJSONObject(0).opt("attendancetypeid"));
					if (isOffOrHoliday) {
						if (attendanceTypeId != HRISApplicationConstants.ATTENDANCE_HOLIDAY && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_LEAVE && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_ABSENT && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_OFF) {
							return 0;
						}
					} else if ((attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_OFF || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN)) {
						totalOffForSandwich++;
					}
				}
			}
		}
		return totalOffForSandwich;
	}

	private static JSONArray getEmployeeAttendanceRecord(Object employeeId, String attendanceDateInString) throws JSONException {

		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("attendancetypeid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate = '" + attendanceDateInString + "'");// +
																																// "' and attendancetypeid not in (2, 3, 6)");

		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeeattendance");
		return rows;

	}

	private static int getHolidayRecordsForSandwich(Object holidayCalendarId, String monthDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidays");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate = '" + monthDate + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_holidays");
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		return arrayCount;
	}

	public static long getActualNonWorkingDays(Object employeeId, Object employeeScheduleId, Object holidayCalendarId, Date monthFirstDate, Date monthLastDate, Integer currentYear, Integer monthId, String monthFirstDateInString, String monthLastDateInstring) throws JSONException, ParseException {
		List<String> daysOfWeek = new ArrayList<String>();
		daysOfWeek.add("monday");
		daysOfWeek.add("tuesday");
		daysOfWeek.add("wednesday");
		daysOfWeek.add("thursday");
		daysOfWeek.add("friday");
		daysOfWeek.add("saturday");
		daysOfWeek.add("sunday");
		int weekListSize = daysOfWeek.size();
		Set<Object> nonWorkingDayList = new LinkedHashSet<Object>();

		if (employeeScheduleId != null) {
			for (int counter = 0; counter < weekListSize; counter++) {
				boolean isWeekDay = getWeekDayFromSchedule(daysOfWeek.get(counter), employeeScheduleId);
				if (!isWeekDay) {
					long dayBetween = DataTypeUtilities.daysBetween(monthLastDate, monthFirstDate);
					for (int counter1 = 0; counter1 < dayBetween + 1; counter1++) {
						Calendar cal2 = new GregorianCalendar();
						cal2.setTime(monthFirstDate);
						cal2.add(Calendar.DATE, counter1);
						Date date = cal2.getTime();

						SimpleDateFormat simpleDateformat = new SimpleDateFormat("EEEE");

						String weekDay = simpleDateformat.format(date);

						String weekDayInLower = weekDay.toLowerCase();

						if (daysOfWeek.get(counter).equalsIgnoreCase(weekDayInLower)) {
							nonWorkingDayList.add(date);
						}
					}
				}
			}

			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_monthlyoff");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put("weeknoid");
			columnArray.put("weekdayid");
			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, "scheduleid = " + employeeScheduleId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("hris_monthlyoff");
			int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();

			if (arrayCount > 0) {
				for (int counter = 0; counter < arrayCount; counter++) {
					Integer weekNoId = (Integer) array.getJSONObject(counter).opt("weeknoid");
					Integer weekDayId = (Integer) array.getJSONObject(counter).opt("weekdayid");
					Date date = getDate(currentYear, monthId, weekNoId, weekDayId);
					String dateInString = getDateInString(date);
					DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
					Date monthlyOffDate = formatter.parse(dateInString);
					nonWorkingDayList.add(monthlyOffDate);
				}
			}
		}
		if (holidayCalendarId != null) {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_holidays");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put("holidaydate");
			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate >= '" + monthFirstDateInString + "' and holidaydate <= '" + monthLastDateInstring + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("hris_holidays");
			int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
			if (arrayCount > 0) {
				for (int counter = 0; counter < arrayCount; counter++) {
					Object holidayDateObject = array.getJSONObject(counter).opt("holidaydate");
					Date holidayDate = DataTypeUtilities.checkDateFormat(holidayDateObject);
					nonWorkingDayList.add(holidayDate);
				}
			}
		}
		long nonWorkingDays = nonWorkingDayList.size();
		return nonWorkingDays;
	}

	/**
	 * this method will return the date of desired day of any month and any week
	 * 
	 * @param year
	 *            Pass the year string like 2011
	 * @param month
	 *            consider month of Jan having index 1
	 * @param week
	 *            consider first week as having index 1
	 * @param day
	 *            consider week starting day as Sunday as having index 1
	 */
	public static Date getDate(Integer year, Integer month, Integer week, Integer day) {
		Calendar calendar = Calendar.getInstance();
		if (year != null) {
			calendar.set(Calendar.YEAR, year);
		}
		if (month != null) {
			month = month - 1;
			calendar.set(Calendar.MONTH, month);
		}
		if (week != null) {
			calendar.set(Calendar.WEEK_OF_MONTH, week);
		}
		if (day != null) {
			calendar.set(Calendar.DAY_OF_WEEK, day);
		}
		return calendar.getTime();
	}

	public static long getOffDays(Object employeeId, Object employeeScheduleId, Date monthFirstDate, Date monthLastDate) throws JSONException {
		List<String> daysOfWeek = new ArrayList<String>();
		daysOfWeek.add("monday");
		daysOfWeek.add("tuesday");
		daysOfWeek.add("wednesday");
		daysOfWeek.add("thursday");
		daysOfWeek.add("friday");
		daysOfWeek.add("saturday");
		daysOfWeek.add("sunday");

		int weekListSize = daysOfWeek.size();
		long offDays = 0;
		long dayBetween = DataTypeUtilities.daysBetween(monthLastDate, monthFirstDate);
		if (employeeScheduleId != null) {
			for (int counter = 0; counter < weekListSize; counter++) {
				boolean isWeekDay = getWeekDayFromSchedule(daysOfWeek.get(counter), employeeScheduleId);
				if (!isWeekDay) {
					for (int counter1 = 0; counter1 < dayBetween + 1; counter1++) {
						String weekDay = getWeekday(monthFirstDate, counter1);
						if (daysOfWeek.get(counter).equalsIgnoreCase(weekDay)) {
							offDays++;
						}
					}
				}
			}

			long monthlyOffCount = getMonthlyOffRecords(employeeScheduleId);
			offDays = offDays + monthlyOffCount;
			return offDays;
		}
		return 0;
	}

	public static long getMonthlyOffRecords(Object scheduleId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_monthlyoff");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "scheduleid = " + scheduleId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_monthlyoff");
		long arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		return arrayCount;
	}

	public static String getWeekday(Date monthFirstDate, int counter) {
		Calendar cal2 = new GregorianCalendar();
		cal2.setTime(monthFirstDate);
		cal2.add(Calendar.DATE, counter);
		Date date = cal2.getTime();
		SimpleDateFormat simpleDateformat = new SimpleDateFormat("EEEE");
		String weekDay = simpleDateformat.format(date);
		String weekDayInLower = weekDay.toLowerCase();
		return weekDayInLower;
	}

	public static boolean getWeekDayFromSchedule(String weekDay, Object scheduleId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_schedules");
		JSONArray array = new JSONArray();
		array.put(weekDay);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + scheduleId);
		JSONObject data = ResourceEngine.query(query);
		JSONArray rows = data.getJSONArray("hris_schedules");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			Object weekDayObject = rows.getJSONObject(0).opt(weekDay);
			boolean isWeekDay = DataTypeUtilities.booleanValue(weekDayObject);
			return isWeekDay;
		} else {
			return true;
		}
	}

	public JSONArray getLeaveRuleRecord(Object leavePolicyId, String monthFirstDateInString, String monthLastDateInString) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverule");
		JSONArray array = new JSONArray();
		array.put("leavetypeid");
		array.put("noofleaves");
		array.put("accrueaftermonths");

		array.put("leavecreditid");
		array.put("proportionate_base");
		array.put("accrual_base");

		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "leavepolicyid = " + leavePolicyId + " and fromdate <= '" + monthFirstDateInString + "' and todate >= '" + monthLastDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, "leavepolicyid = " + leavePolicyId + " and fromdate <= '" + monthFirstDateInString + "' and todate = null");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_leaverule");
		return rows;
	}

	public static double getAssignTotalLeave(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("leavepolicyid");
		array.put("leavepolicyid.totalleaves");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + employeeId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_employees");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			// Object leavePolicyId =
			// rows.getJSONObject(0).opt("leavepolicyid");
			double totalLeaves = rows.getJSONObject(0).optDouble("leavepolicyid.totalleaves");
			return totalLeaves;
		}
		return 0.0;
	}

	public static double getNumberOfHolidays(Object employeeId, Object holidayCalendarId, String monthFirstDate, String monthLastDate) throws JSONException {
		if (holidayCalendarId != null) {
			double holidayCount = getHolidayRecords(holidayCalendarId, monthFirstDate, monthLastDate);
			return holidayCount;
		}
		return 0.0;
	}

	private static double getHolidayRecords(Object holidayCalendarId, String monthFirstDate, String monthLastDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidays");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate >= '" + monthFirstDate + "' and holidaydate <= '" + monthLastDate + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_holidays");
		double arrayCount = (array == null || array.length() == 0) ? 0.0 : array.length();
		return arrayCount;
	}

	private JSONArray getEmployeeRecord(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("earnedleavebalance");
		array.put("casualleavebalance");
		array.put("medicalleavebalance");
		array.put("specialleavebalance");
		array.put("extraworkingdaybalance");
		array.put("holidaycalendarid");
		array.put("employeescheduleid");
		array.put("leavepolicyid");
		array.put("joiningdate");
		array.put("relievingdate");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + employeeId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_employees");
		return rows;
	}

	public static void insertAttendanceFromJoiningToToday(Object employeeId, Date joiningDate) throws JSONException {
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		Date todayDate = Translator.dateValue(TODAY_DATE);
		if (joiningDate.before(todayDate) || joiningDate.equals(todayDate)) {
			long difference = DataTypeUtilities.differenceBetweenDates(joiningDate, todayDate) + 1;
			Calendar cal = Calendar.getInstance();
			JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(employeeId);
			Object employeeUserId = null;
			if (employeeArray != null && employeeArray.length() > 0) {
				String officialEmailId = employeeArray.getJSONObject(0).optString("officialemailid");
				employeeUserId = PunchingUtility.getId(UserPermission.Kind.USERS, UserPermission.Kind.Users.EMAIL_ID, officialEmailId);
			}
			for (int counter = 0; counter < difference; counter++) {
				cal.setTime(joiningDate);
				cal.add(Calendar.DATE, counter);
				String attendanceDate = EmployeeSalaryGenerationServlet.getDateInString(cal.getTime());
				new EmployeeCardPunchDataBusinessLogic();
				int[] halfDayOff = { 0 };
				boolean isOffDay = EmployeeCardPunchDataBusinessLogic.getOffDayToMarkEWD(employeeId, cal.getTime(), halfDayOff);
				int attendanceTypeId = HRISApplicationConstants.ATTENDANCE_UNKNOWN;
				if (isOffDay) {
					if (halfDayOff[0] == 1) {
						attendanceTypeId = HRISApplicationConstants.ATTENDANCE_HALF_DAY_OFF;
					} else {
						attendanceTypeId = HRISApplicationConstants.ATTENDANCE_OFF;
					}
				}
				try {
					MarkAttendanceServlet.updateStaffAttendance(attendanceTypeId, employeeId, attendanceDate, employeeUserId, "");
				} catch (Exception e) {
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "Attendance Insert Failed Due To Joining Date Updation");
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			EmployeeMonthlyAttendance employeeMonthlyAttendance = new EmployeeMonthlyAttendance();
			employeeMonthlyAttendance.sendEmailToReportingToApproveAttendanceChangeRequest("nitin.goyal@daffodilsw.com", "Nitin Goyal", "Pankaj Jain", "2013-08-19", "Absent", "Present", null, "Testing");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}