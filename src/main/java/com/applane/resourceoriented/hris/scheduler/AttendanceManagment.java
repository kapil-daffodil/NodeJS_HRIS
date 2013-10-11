package com.applane.resourceoriented.hris.scheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneAsyncTaskManager;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.CurrentRequest;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.ResourceNotFoundException;
import com.applane.databaseengine.TableNotFoundException;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.CalculateCTCForAllEmployees;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeMonthlyAttendance;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.MarkAttendanceServlet;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.service.cron.ApplaneCron;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.cachehandler.OrganizationCacheHandler;
import com.applane.user.exception.OrganizationNotFoundException;
import com.applane.user.shared.constants.UserPermission.Kind.Organizations;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class AttendanceManagment implements ApplaneTask, ApplaneCron {

	private static final String ORGANIZATION_INDEX = "organization_index";

	private static final String EMPLOYEE_CURSOR = "employee_cursor";

	private static final String RESTART_FROM = "restart_from";

	private static final int MAX_ROWS = 15;

	@Override
	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		try {
			JSONObject taskInfo = applaneTaskInfo.getTaskInfo();
			if (taskInfo.isNull(ORGANIZATION_INDEX)) {
				throw new BusinessLogicException("Please provide organization detail.");
			}
			int index = taskInfo.getInt(ORGANIZATION_INDEX);
			iniitateSession(HRISApplicationConstants.ORGANIZATIONS[index], index);
			// iniitateSession(HRISApplicationConstants.ORGANIZATIONS[3], 3);
			/* Segment to run Business Logic */
			String employeeCursor = (String) taskInfo.remove(EMPLOYEE_CURSOR);
			String restartFrom = taskInfo.optString(RESTART_FROM, null);
			employeeCursor = excersiceEmployees(employeeCursor, restartFrom);
			/* To run new task Queue */
			if (isProcessEnable() && employeeCursor != null && employeeCursor.trim().length() > 0) {
				taskInfo.put(EMPLOYEE_CURSOR, employeeCursor);
				applaneTaskInfo.enQueueAgain(false);
			} else if (hasMoreOrganization(index)) {
				taskInfo.put(ORGANIZATION_INDEX, ++index);
				taskInfo.remove(RESTART_FROM);
				applaneTaskInfo.enQueueAgain(false);
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	@Override
	public void cronCall() throws DeadlineExceededException {
		try {
			JSONObject parameters = new JSONObject();
			parameters.put(ORGANIZATION_INDEX, 0);
			ApplaneTaskService.enQueueTaskInPrallel(getClass().getCanonicalName(), HRISApplicationConstants.QueueConstantsHRIS.POPULATE_ATTENDANCE, parameters);
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	/**** ----------------------------------------Private Methods--------------------------------------------- *****/

	/**
	 * 
	 * @param employeeCursor
	 * @param restartFrom
	 * @return
	 * @throws Exception
	 */
	private String excersiceEmployees(String employeeCursor, String restartFrom) throws Exception {
		String backDate = DataTypeUtilities.getBackDate(new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime()));
		JSONObject employeesDetail = getEmployeeAttendance(employeeCursor, restartFrom, backDate);
		JSONArray employeeAttendance = employeesDetail.getJSONArray(HrisKinds.EMPLOYEES);
		int length = employeeAttendance.length();
		for (int i = 0; i < length; i++) {
			JSONObject employeeAttaendanceInfo = employeeAttendance.getJSONObject(i);
			if (isAttendanceEnable()) {
				boolean isOffDaysNotFixed = Translator.booleanValue(employeeAttaendanceInfo.opt("employeescheduleid.is_off_day_not_fixed"));
				boolean markOffDay = isOffDaysNotFixed ? false : isOffToday(employeeAttaendanceInfo, backDate);
				populateAttendance(backDate, employeeAttaendanceInfo, markOffDay, isOffDaysNotFixed);
			}
			if (isCtcCalculationRequired()) {
				CalculateCTCForAllEmployees.populateSalaryComponents(employeeAttaendanceInfo, getFieldsDetails(backDate));
			}
			if (isCarryForwardEnable()) {
				executeCarryForwardProcess(employeeAttaendanceInfo);
			}
		}
		return employeesDetail.optString(Data.Query.Result.CURSOR, null);
	}

	private boolean isOffToday(JSONObject employeeAttaendanceInfo, String backDate) throws JSONException {
		Date attendanceDate = Translator.dateValue(backDate);
		Object scheduleId = employeeAttaendanceInfo.opt("employeescheduleid");
		Object holidayCalendarId = employeeAttaendanceInfo.opt("holidaycalendarid");

		Calendar instance = Calendar.getInstance();
		instance.setTime(attendanceDate);
		int weekDayNumeric = instance.get(Calendar.DAY_OF_WEEK) - 1;
		String dayOfWeek = dayOfWeek(weekDayNumeric);

		return (fetchDayScheduleStatus(scheduleId, dayOfWeek) || fetchMonthlySchedule(scheduleId, instance) || fetchHoliDaySchedule(holidayCalendarId, backDate));
	}

	private boolean fetchHoliDaySchedule(Object holidayCalendarId, String date) throws JSONException {
		String key = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "__" + holidayCalendarId + "__dateofmonth" + date;
		Object sessionVeriable = CurrentRequest.getVariable(key);
		if (sessionVeriable != null) {
			return (Boolean) sessionVeriable;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidays");
		JSONArray array = new JSONArray();
		array.put("holidaydate");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate = '" + date + "'");
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("hris_holidays");
		boolean isOff = rows.length() > 0;
		CurrentRequest.setRequestVariable(key, isOff);
		return isOff;
	}

	private boolean fetchMonthlySchedule(Object scheduleId, Calendar instance) throws JSONException {
		int weekOfMonth = instance.get(Calendar.WEEK_OF_MONTH);
		int weekDayNumeric = instance.get(Calendar.DAY_OF_WEEK);
		String key = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "__" + scheduleId + "__monthweek" + weekOfMonth + "_day" + weekDayNumeric;
		Object sessionVeriable = CurrentRequest.getVariable(key);
		if (sessionVeriable != null) {
			return (Boolean) sessionVeriable;
		}
		JSONObject parmeters = new JSONObject();
		parmeters.put("s_deule", scheduleId);
		parmeters.put("weekOfMonth_", weekOfMonth);
		parmeters.put("weekDayNumeric", weekDayNumeric);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_monthlyoff");
		query.put(Data.Query.FILTERS, "scheduleid={s_deule}" + " AND weeknoid={weekOfMonth_} AND weekdayid={weekDayNumeric}");
		query.put(Data.Query.COLUMNS, new JSONArray().put(Data.Query.KEY));
		query.put(Data.Query.PARAMETERS, parmeters);
		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_monthlyoff");
		boolean isOff = result.length() > 0;
		CurrentRequest.setRequestVariable(key, isOff);
		return isOff;
	}

	private boolean fetchDayScheduleStatus(Object scheduleId, String dayOfWeek) throws JSONException {
		String key = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "__" + scheduleId + "__dayday" + dayOfWeek + "_";
		Object sessionVeriable = CurrentRequest.getVariable(key);
		if (sessionVeriable != null) {
			return (Boolean) sessionVeriable;
		}
		JSONObject parmeters = new JSONObject();
		parmeters.put("s_deule", scheduleId);
		JSONArray columns = new JSONArray();
		columns.put(dayOfWeek);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_schedules");
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, Data.Query.KEY + "={s_deule}");
		query.put(Data.Query.PARAMETERS, parmeters);
		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_schedules");
		boolean isOff = false;
		if (result.length() > 0) {
			isOff = !Translator.booleanValue(result.getJSONObject(0).opt(dayOfWeek));
		}
		CurrentRequest.setRequestVariable(key, isOff);
		return isOff;
	}

	private static String dayOfWeek(int i) {
		switch (i) {
		case 0:
			return "sunday";
		case 1:
			return "monday";
		case 2:
			return "tuesday";
		case 3:
			return "wednesday";
		case 4:
			return "thursday";
		case 5:
			return "friday";
		case 6:
			return "saturday";
		default:
			throw new RuntimeException("Invalid value ......");
		}
	}

	// private boolean isMarkOffDay(HashMap<Date, Integer> offDaysMap, HashMap<Date, Integer> holidaysMap, HashMap<Date, Object[]> dailyAttendanceMap, JSONObject employeeAttaendanceInfo, String backDate, int[] tempScheduleId, int[] tempHolidayCalendarId) throws Exception {
	// Date attendanceDate = Translator.dateValue(backDate);
	// Object employeeId = employeeAttaendanceInfo.opt(Data.Query.KEY);
	// Object scheduleId = employeeAttaendanceInfo.opt("employeescheduleid");
	// Object holidayCalendarId = employeeAttaendanceInfo.opt("holidaycalendarid");
	//
	// int originalScheduleId = Translator.integerValue(scheduleId);
	// int originalHolidayCalendarId = Translator.integerValue(holidayCalendarId);
	// if (originalScheduleId != tempScheduleId[0]) {
	// tempScheduleId[0] = originalScheduleId;
	// offDaysMap.clear();
	// new EmployeeMonthlyAttendance().getTotalOffAndHolidaysMaps(attendanceDate, backDate, attendanceDate, backDate, offDaysMap, null, dailyAttendanceMap, scheduleId, holidayCalendarId, employeeId);
	// }
	// if (originalHolidayCalendarId != tempHolidayCalendarId[0]) {
	// tempHolidayCalendarId[0] = originalHolidayCalendarId;
	// holidaysMap.clear();
	// new EmployeeMonthlyAttendance().getTotalOffAndHolidaysMaps(attendanceDate, backDate, attendanceDate, backDate, null, holidaysMap, dailyAttendanceMap, scheduleId, holidayCalendarId, employeeId);
	// }
	// LogUtility.writeError("offDaysMap >> " + offDaysMap + " << holidaysMap >> " + holidaysMap);
	// return (offDaysMap.containsKey(attendanceDate) || holidaysMap.containsKey(attendanceDate));
	// }

	private void executeCarryForwardProcess(JSONObject employeeAttaendanceInfo) throws JSONException {
		Object employeeid = employeeAttaendanceInfo.get(Data.Query.KEY);
		Object leavePolicyId = employeeAttaendanceInfo.opt("leavepolicyid");
		String currentDate = Translator.getDateInString(SystemParameters.getCurrentDate());
		try {
			JSONArray leaveRules = leavePolicyId == null ? null : getLeavePolicyRules(leavePolicyId, currentDate);
			int length = leaveRules == null ? 0 : leaveRules.length();
			for (int i = 0; i < length; i++) {
				JSONObject rule = leaveRules.getJSONObject(i);
				String lapsDate = rule.optString("lapsdate", null);
				Object leaveType = rule.opt("leavetypeid");
				double maxAllowed = rule.optDouble("maxallowed", -1d);
				String carryForwardType = rule.optString("carry_forward_type", null);
				boolean carryForward = Translator.booleanValue(rule.opt("carryforward"));
				if (lapsDate != null && lapsDate.equals(currentDate) && carryForward && maxAllowed != -1d) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(Translator.dateValue(lapsDate));
					int monthid = cal.get(Calendar.MONTH) + 1;
					long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));

					JSONArray accumulatedLeaves = getAccumulatedLeaves(employeeid, leaveType, lapsDate, monthid, yearId);
					JSONArray employeeMonthlyAttendance = getEmployeeMonthlyAttendance(employeeid, monthid, yearId);
					if (accumulatedLeaves.length() > 0 && employeeMonthlyAttendance != null && employeeMonthlyAttendance.length() > 0) {
						Object monthlyAttendanceId = employeeMonthlyAttendance.getJSONObject(0).opt("__key__");
						double earnedOpeningBalance = Translator.doubleValue(employeeMonthlyAttendance.getJSONObject(0).opt("earnedleaveopeningbalance"));
						double casualOpeningBalance = Translator.doubleValue(employeeMonthlyAttendance.getJSONObject(0).opt("casualleaveopeningbalance"));
						double medicalOpeningBalance = Translator.doubleValue(employeeMonthlyAttendance.getJSONObject(0).opt("medicalleaveopeningbalance"));

						JSONObject leaves = accumulatedLeaves.getJSONObject(0);
						double leaveBalance = leaves.optDouble("openingbalance", 0);
						String remarks = leaves.optString("remarks", "");

						if ((maxAllowed < leaveBalance || (carryForwardType != null && carryForwardType.equals("Percentage"))) && leaveBalance > 0 && maxAllowed > 0) {
							double leavesDeducted = 0;
							if (carryForwardType != null && carryForwardType.equals("Percentage")) {
								leavesDeducted = (leaveBalance - ((double) (leaveBalance * maxAllowed) / 100));
								maxAllowed = (double) (leaveBalance * maxAllowed) / 100;
							} else {
								leavesDeducted = leaveBalance - maxAllowed;
							}

							int leaveTypeId = Translator.integerValue(leaveType);

							if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE) {
								earnedOpeningBalance -= leavesDeducted;
							} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE) {
								casualOpeningBalance -= leavesDeducted;
							} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE) {
								medicalOpeningBalance -= leavesDeducted;
							}

							remarks += leavesDeducted + " leaves have been deducted from your balance leaves by carry forward policy. Please contact to the concerned person. " + remarks;
							leaves.put("openingbalance", maxAllowed);
							leaves.put("remarks", remarks);
							updateAccumulatedleaves(leaves);
							insertCarryForwardLeaves(employeeid, leaveType, leavesDeducted, monthid, yearId);
							insertIntoEncashLeaves(employeeid, leavesDeducted, monthid, yearId, leaveTypeId, maxAllowed, lapsDate);
							updateMonthlyAttendance(monthlyAttendanceId, earnedOpeningBalance, casualOpeningBalance, medicalOpeningBalance, leaveType);
						}
					}
				}
			}
		} catch (Exception e) {
			String exceptionTraceMessage = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			sendExcpetionMail("Carry Forward Process Failed for " + employeeAttaendanceInfo.optString("name"), exceptionTraceMessage);
		}
	}

	private void insertIntoEncashLeaves(Object employeeid, double leavesDeducted, int monthid, long yearId, int leaveTypeId, double maxAllowed, String lapsDate) throws Exception {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_encash_leaves");
		JSONObject row = new JSONObject();
		row.put("statusid", 2);
		row.put("request_date", lapsDate);
		row.put("encash_leaves", leavesDeducted);
		row.put("employeeid", employeeid);
		row.put("balance_leaves", maxAllowed);
		row.put("leave_type_id", leaveTypeId);
		row.put("yearid", yearId);
		row.put("monthid", monthid);
		row.put("temp", "laps_leaves");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateMonthlyAttendance(Object monthlyAttendanceId, Object earnedOpeningBalance, Object casualOpeningBalance, Object medicalOpeningBalance, Object leaveType) throws Exception {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		int leaveTypeId = Translator.integerValue(leaveType);

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", monthlyAttendanceId);
		if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE) {
			row.put("earnedleaveopeningbalance", earnedOpeningBalance);
		} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE) {
			row.put("casualleaveopeningbalance", casualOpeningBalance);
		} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE) {
			row.put("medicalleaveopeningbalance", medicalOpeningBalance);
		}
		row.put("temp", "laps_leaves");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private JSONArray getEmployeeMonthlyAttendance(Object employeeid, int monthid, long yearId) throws Exception {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("earnedleaveopeningbalance");
		array.put("casualleaveopeningbalance");
		array.put("medicalleaveopeningbalance");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeid + " and monthid = " + monthid + " and yearid = " + yearId);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeemonthlyattendance");
		return rows;
	}

	private void insertCarryForwardLeaves(Object employeeid, Object leaveType, double leavesDeducted, int monthid, long yearId) throws JSONException {
		JSONObject updateColumns = new JSONObject();
		updateColumns.put("employeeid", employeeid);
		updateColumns.put("monthid", monthid);
		updateColumns.put("yearid", yearId);
		updateColumns.put("leavetypeid", leaveType);
		updateColumns.put("paidstatusid", HRISConstants.LEAVE_NEW);
		updateColumns.put("numberofleaves", leavesDeducted);
		JSONObject updates = new JSONObject().put(Data.Update.UPDATES, updateColumns).put(Data.Update.RESOURCE, "leavesafterlaps");
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private void updateAccumulatedleaves(JSONObject leaves) throws JSONException {
		JSONObject updateLeaves = new JSONObject();
		updateLeaves.put(Data.Update.RESOURCE, "leaveregister");
		updateLeaves.put(Data.Update.UPDATES, leaves);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updateLeaves);
	}

	private JSONArray getAccumulatedLeaves(Object employeeid, Object leaveType, String lapsDate, int monthid, long yearId) throws JSONException {

		JSONObject parameters = new JSONObject();
		parameters.put("employ_sid", employeeid);
		parameters.put("liv_tip", leaveType);
		parameters.put("mo_nth_id", monthid);
		parameters.put("y_ear_id", yearId);

		JSONArray columnArray = new JSONArray();
		columnArray.put("leavebalance");
		columnArray.put("openingbalance");
		columnArray.put("remarks");

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "leaveregister");
		query.put(Data.Query.PARAMETERS, parameters);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = {employ_sid} AND leavetypeid = {liv_tip} AND monthid = {mo_nth_id} AND yearid = {y_ear_id}");
		query.put(Data.Query.MAX_ROWS, 1);

		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("leaveregister");
	}

	private JSONArray getLeavePolicyRules(Object leavePolicyId, String currentDate) throws JSONException {
		JSONArray rules = (JSONArray) CurrentRequest.getVariable(currentDate + leavePolicyId);
		if (rules != null) {
			return rules;
		}
		JSONObject parameters = new JSONObject();
		parameters.put("leav__pol_id", leavePolicyId);
		parameters.put("current_date", currentDate);

		JSONArray columnArray = new JSONArray();
		columnArray.put("carryforward");
		columnArray.put("maxallowed");
		columnArray.put("lapsdate");
		columnArray.put("leavetypeid");
		columnArray.put("carry_forward_type");

		String filter = "leavepolicyid = {leav__pol_id} AND fromdate <= {current_date} AND todate >= {todate}";
		String optionFilter = "leavepolicyid = {leav__pol_id} AND fromdate <= {current_date} AND todate =null";

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverule");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, filter);
		query.put(Data.Query.OPTION_FILTERS, optionFilter);
		query.put(Data.Query.PARAMETERS, parameters);

		rules = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_leaverule");
		CurrentRequest.setRequestVariable("currentDate + leavePolicyId", rules);
		return rules;
	}

	private boolean isCarryForwardEnable() throws JSONException {
		JSONObject mailConfigurationSetup = getMailConfigurationSetup();
		return !Translator.booleanValue(mailConfigurationSetup.opt("disable_carry_forward_leaves"));
	}

	private boolean isCtcCalculationRequired() throws ParseException, JSONException {
		boolean isTodayFriday = Translator.getTodayIs(SystemParameters.getCurrentDate(), "friday");
		if (isTodayFriday) {
			JSONObject mailConfigurationSetup = getMailConfigurationSetup();
			return !Translator.booleanValue(mailConfigurationSetup.opt("disable_populate_attendance"));
		}
		return false;
	}

	private boolean isAttendanceEnable() throws JSONException {
		JSONObject mailConfigurationSetup = getMailConfigurationSetup();
		return !(Translator.booleanValue(mailConfigurationSetup.opt("disable_populate_attendance")));
	}

	private void populateAttendance(String backDate, JSONObject employeeAttaendanceInfo, boolean markOffDay, boolean isOffDaysNotFixed) {
		try {
			JSONArray attendanceRows = employeeAttaendanceInfo.getJSONArray("employeeattendance");
			int employeeScheduleId = employeeAttaendanceInfo.optInt("employeescheduleid", 0);

			String halfDayTime = employeeAttaendanceInfo.optString("employeescheduleid.half_day_time", null);
			String timeShouldBeInOffice = employeeAttaendanceInfo.optString("employeescheduleid.time_should_be_in_office", null);

			int leavePolicyId = employeeAttaendanceInfo.optInt("leavepolicyid", 0);
			int subBranchId = employeeAttaendanceInfo.optInt("subbranchid", 0);
			String deviceId = employeeAttaendanceInfo.optString("deviceid", null);
			int employeeId = employeeAttaendanceInfo.optInt(Updates.KEY, 0);

			if (attendanceRows.length() == 0) {
				/* Mark Absent as there is no record of attendance */
				int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
				int attendanceTypeId = (!isMarkAttendanceAbsent() || (organizationId == 5 && subBranchId == HRISApplicationConstants.BRANCH_CLINT_SIDE)) ? HRISApplicationConstants.ATTENDANCE_UNKNOWN : HRISApplicationConstants.ATTENDANCE_ABSENT;
				if (!isOffDaysNotFixed) {
					if (organizationId == 10719 && deviceId != null && deviceId.equalsIgnoreCase("REMOTE EMP")) {
						attendanceTypeId = HRISApplicationConstants.ATTENDANCE_TOUR;
					}
					if (markOffDay) {
						Calendar cal = Calendar.getInstance();
						cal.setTime(Translator.dateValue(backDate));
						int weekDayNumeric = cal.get(Calendar.DAY_OF_WEEK);
						int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
						JSONArray halfOffDay = EmployeeMonthlyAttendance.getHalfOffDay(employeeScheduleId, weekDayNumeric, weekOfMonth);
						if (halfOffDay != null && halfOffDay.length() > 0) {
							attendanceTypeId = HRISApplicationConstants.ATTENDANCE_HALF_DAY_ABSENT;
						} else {
							attendanceTypeId = HRISApplicationConstants.ATTENDANCE_OFF;
						}
					}
				} else {
					Date firstDayOfMonth = DataTypeUtilities.getMonthFirstDate(Translator.dateValue(backDate));
					Date lastDayOfMonth = DataTypeUtilities.getMonthLastDate(firstDayOfMonth);
					String monthFirstString = Translator.getDateInString(firstDayOfMonth);
					String monthLastString = Translator.getDateInString(lastDayOfMonth);
					double totalOffDaysInAMonth = isOffDaysNotFixed ? getTotalOffDaysInAMonth(employeeScheduleId, firstDayOfMonth, lastDayOfMonth, monthFirstString, monthLastString) : -1;
					LogUtility.writeError("totalOffDaysInAMonth >> " + totalOffDaysInAMonth);
					if (totalOffDaysInAMonth > 0) {
						double totalOff = getTotalMarkedOff(employeeId, monthFirstString, monthLastString, organizationId);
						LogUtility.writeError("totalOff >> " + totalOff);
						if (totalOff < totalOffDaysInAMonth) {
							attendanceTypeId = HRISApplicationConstants.ATTENDANCE_OFF;
							if ((totalOffDaysInAMonth - totalOff) < 1) {
								attendanceTypeId = HRISApplicationConstants.ATTENDANCE_HALF_DAY_ABSENT;
							}
						}
					}
				}
				Object[] details = new Object[1];
				checkLeave(employeeId, backDate, details);
				if (details[0] != null) {
					updateAttendanceRest(employeeId, attendanceTypeId, backDate, details);
				}

			} else {
				/* Mark Half/Short Day Leave for the present employees if minimum office time is not complete */
				JSONObject attendance = attendanceRows.getJSONObject(0);
				boolean isCabDelay = getIsCabDelay(employeeId, backDate);
				if (!isCabDelay) {
					int leaveTypeId = getLeaveRuleType(leavePolicyId, backDate, backDate);
					markHalfOrShortLeave(employeeScheduleId, leaveTypeId, attendance, timeShouldBeInOffice, halfDayTime);
				}
			}
		} catch (Exception e) {
			String exceptionTraceMessage = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			sendExcpetionMail("Attendance Marking Failed " + employeeAttaendanceInfo.optString("name"), exceptionTraceMessage);
		}
	}

	private boolean getIsCabDelay(int employeeId, String backDate) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_cab_delay_information");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employee_id = " + employeeId + " and date = '" + backDate + "' AND approve_by_admin_status_id = 2");
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("hris_cab_delay_information");
		if (rows != null && rows.length() > 0) {
			return true;
		}
		return false;
	}

	private double getTotalMarkedOff(int employeeId, String monthFirstString, String monthLastString, int organizationId) throws Exception {

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("attendancetypeid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate >= '" + monthFirstString + "' and attendancedate <= '" + monthLastString + "' and attendancetypeid IN(" + HRISApplicationConstants.ATTENDANCE_OFF + "," + HRISApplicationConstants.ATTENDANCE_HALF_DAY_OFF + "," + HRISApplicationConstants.ATTENDANCE_HALF_DAY_ABSENT + ")");
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("employeeattendance");
		if (rows != null && rows.length() > 0) {
			double total = 0.0;
			for (int counter = 0; counter < rows.length(); counter++) {
				int attendanceTypeId = Translator.integerValue(rows.getJSONObject(counter).opt("attendancetypeid"));
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_OFF) {
					total += 1;
				} else if ((organizationId != 7783 && attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HALF_DAY_ABSENT) || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HALF_DAY_OFF) {
					total += 0.50;
				}
			}
			return total;
		}
		return 0;
	}

	private double getTotalOffDaysInAMonth(int employeeScheduleId, Date firstDayOfMonth, Date lastDayOfMonth, String monthFirstString, String monthLastString) throws Exception {
		String key = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "__" + employeeScheduleId + "__total_off";
		Object sessionVeriable = CurrentRequest.getVariable(key);
		if (sessionVeriable != null) {
			return Translator.doubleValue(("" + sessionVeriable));
		}

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_schedules");
		JSONArray array = new JSONArray();
		array.put("__key__");
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
		innerColumns.put("weeknoid.name");
		innerColumns.put("weekdayid.name");
		innerColumns.put("off_day_type");

		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_monthlyoff");

		innerObject.put("relatedcolumn", "scheduleid");
		innerObject.put("alias", "hris_monthlyoff");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);

		query.put(Data.Query.CHILDS, new JSONArray().put(innerObject));

		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + employeeScheduleId);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("hris_schedules");
		double totalOff = 0.0;
		if (rows != null && rows.length() > 0) {
			boolean mondayOff = !Translator.booleanValue(rows.getJSONObject(0).opt("monday"));
			boolean tuesdayOff = !Translator.booleanValue(rows.getJSONObject(0).opt("tuesday"));
			boolean wednesdayOff = !Translator.booleanValue(rows.getJSONObject(0).opt("wednesday"));
			boolean thursdayOff = !Translator.booleanValue(rows.getJSONObject(0).opt("thursday"));
			boolean fridayOff = !Translator.booleanValue(rows.getJSONObject(0).opt("friday"));
			boolean saturdayOff = !Translator.booleanValue(rows.getJSONObject(0).opt("saturday"));
			boolean sundayOff = !Translator.booleanValue(rows.getJSONObject(0).opt("sunday"));
			JSONArray monthlyOffArray = rows.getJSONObject(0).optJSONArray("hris_monthlyoff");
			HashMap<Integer, HashMap<String, String>> monthlyOffMap = new HashMap<Integer, HashMap<String, String>>();

			if (monthlyOffArray != null && monthlyOffArray.length() > 0) {
				for (int counter = 0; counter < monthlyOffArray.length(); counter++) {
					int weekNumber = Translator.integerValue(monthlyOffArray.getJSONObject(counter).opt("weeknoid.name"));
					String day = Translator.stringValue(monthlyOffArray.getJSONObject(counter).opt("weekdayid.name")).toLowerCase();
					String offDayType = monthlyOffArray.getJSONObject(counter).optString("off_day_type", null);
					HashMap<String, String> offDays = new HashMap<String, String>();
					if (monthlyOffMap.containsKey(weekNumber)) {
						offDays = monthlyOffMap.get(weekNumber);
					}
					offDays.put(day, offDayType);
					monthlyOffMap.put(weekNumber, offDays);
				}
			}
			Calendar cal = Calendar.getInstance();
			long numberOfDaysInAMonth = DataTypeUtilities.getNumberOfDaysInMonth(firstDayOfMonth);
			for (int counter = 0; counter < numberOfDaysInAMonth; counter++) {
				cal.setTime(firstDayOfMonth);
				cal.add(Calendar.DAY_OF_MONTH, counter);
				int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
				if (dayOfWeek == Calendar.SUNDAY && sundayOff) {
					totalOff += 1;
				} else if (dayOfWeek == Calendar.MONDAY && mondayOff) {
					totalOff += 1;
				} else if (dayOfWeek == Calendar.TUESDAY && tuesdayOff) {
					totalOff += 1;
				} else if (dayOfWeek == Calendar.WEDNESDAY && wednesdayOff) {
					totalOff += 1;
				} else if (dayOfWeek == Calendar.THURSDAY && thursdayOff) {
					totalOff += 1;
				} else if (dayOfWeek == Calendar.FRIDAY && fridayOff) {
					totalOff += 1;
				} else if (dayOfWeek == Calendar.SATURDAY && saturdayOff) {
					totalOff += 1;
				} else if (monthlyOffMap != null && monthlyOffMap.size() > 0) {
					int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
					if (monthlyOffMap.containsKey(weekOfMonth)) {
						HashMap<String, String> offDaysMap = monthlyOffMap.get(weekOfMonth);
						String weekDay = dayOfWeek((dayOfWeek - 1)).toLowerCase();
						if (offDaysMap.containsKey(weekDay)) {
							String type = offDaysMap.get(weekDay);
							if (type != null && type.equalsIgnoreCase("Half Day")) {
								totalOff += 0.50;
							} else {
								totalOff += 1;
							}
						}
					}
				}
			}
		}
		CurrentRequest.setRequestVariable(key, totalOff);
		return totalOff;
	}

	private void checkLeave(int employeeId, String backDate, Object[] details) throws Exception {
		JSONArray leavesArray = MarkAttendanceServlet.getLeaveRequestRecords(employeeId, backDate);
		int status = 0;
		if (leavesArray != null && leavesArray.length() > 0) {
			status = leavesArray.getJSONObject(0).optInt("leavestatusid");
		}
		if (status == HRISConstants.LEAVE_APPROVED) {
			int tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_LEAVE;
			int durationType = MarkAttendanceServlet.isHalfDayLeave(Translator.dateValue(backDate), leavesArray);
			if (durationType == HRISConstants.DURATION_FIRSTHALF) {
				tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE;
			} else if (durationType == HRISConstants.DURATION_SECONDHALF) {
				tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE;
			}
			Object leaveRequestId = null;
			leaveRequestId = leavesArray.getJSONObject(0).opt("__key__");
			Object leaveTypeId = leavesArray.getJSONObject(0).opt("leavetypeid");
			Object paidStatus = leavesArray.getJSONObject(0).opt("paidstatus");
			MarkAttendanceServlet.updateLeaveAttendance(tempAttendanceTypeId, employeeId, backDate, leaveRequestId, null, leaveTypeId, paidStatus);
			details[0] = null;
		} else if (status == HRISConstants.LEAVE_NEW) {
			details[0] = "Leave Applied";
		} else {
			details[0] = "";
		}
	}

	private boolean isMarkAttendanceAbsent() throws JSONException {
		JSONObject mailConfigurationSetup = getMailConfigurationSetup();
		return !Translator.booleanValue(mailConfigurationSetup.opt("disable_mark_absent"));
	}

	private void sendExcpetionMail(String subject, String trace) {
		try {
			ApplaneMail mail = new ApplaneMail();
			StringBuilder builder = new StringBuilder();
			builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
			builder.append("<br><br><b>Exception traces are given below :</b><br></br><hr></hr><br></br>").append(trace);
			mail.setMessage(subject, builder.toString(), true);
			mail.setTo("kapil.dalal@daffodilsw.com");
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception Come while send mail in Employee Salary Servlet.");
		}
	}

	public int getLeaveRuleType(Object leavePolicyId, String monthFirstDateInString, String monthLastDateInString) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverule");
		JSONArray array = new JSONArray();
		array.put("leavetypeid");
		array.put("noofleaves");
		array.put("accrueaftermonths");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "leavepolicyid = " + leavePolicyId + " and fromdate <= '" + monthFirstDateInString + "' and todate >= '" + monthLastDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, "leavepolicyid = " + leavePolicyId + " and fromdate <= '" + monthFirstDateInString + "' and todate = null");
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("hris_leaverule");
		return (rows.length() > 0 ? (rows.getJSONObject(0).optInt("leavetypeid", 0)) : 0);
	}

	private JSONObject getFieldsDetails(String TODAY_DATE) throws Exception {
		long yearId = 0;
		int monthId = 0;
		Calendar cal = Calendar.getInstance();
		cal.setTime(Translator.dateValue(TODAY_DATE));

		yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
		monthId = (cal.get(Calendar.MONTH) + 1);
		String monthName = CalculateCTCForAllEmployees.getMonthName(monthId);
		String yearName = CalculateCTCForAllEmployees.getYearName(yearId);
		Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
		String monthFirstDateInString = CalculateCTCForAllEmployees.getDateInString(monthFirstDate);
		String monthLastDateInString = CalculateCTCForAllEmployees.getDateInString(monthLastDate);
		double maxDaysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
		// EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = null;
		int yearIdInt = (int) yearId;

		JSONObject fields = new JSONObject();
		fields.put("monthId", monthId);
		fields.put("yearId", yearIdInt);
		fields.put("monthName", monthName);
		fields.put("yearName", yearName);
		fields.put("monthFirstDate", monthFirstDate);
		fields.put("monthLastDate", monthLastDate);
		fields.put("monthFirstDateInString", monthFirstDateInString);
		fields.put("monthLastDateInString", monthLastDateInString);
		fields.put("maxDaysInMonth", maxDaysInMonth);
		return fields;
	}

	private void markHalfOrShortLeave(int employeeScheduleId, int leaveTypeId, JSONObject attendance, String timeShouldBeInOffice, String halfDayTime) throws Exception {
		int attendanceTypeId = attendance.optInt("attendancetypeid", 0);
		String timeInOffice = attendance.optString("totaltimeinoffice", null);
		String lastOutTime = attendance.optString("lastouttime", null);
		if ((attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) && timeInOffice != null) {
			Object key = attendance.optInt(Updates.KEY);
			int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			if (halfDayTime == null) {
				halfDayTime = "5:00";
			}
			if (timeShouldBeInOffice == null) {
				timeShouldBeInOffice = "0:00";
			}
			boolean markHalfDayLeave = (timeInOffice == null || timeInOffice.length() == 0 || isHalfDayLeaveMarkingEnable()) ? compareTime(timeInOffice, halfDayTime) : false;
			if (markHalfDayLeave || (organizationId == 7783 && lastOutTime == null)) {
				if (organizationId == 7783 && lastOutTime != null) {
					attendanceTypeId = HRISApplicationConstants.ATTENDANCE_HALF_DAY_ABSENT;
					updateHalfDayAttendance(key, attendanceTypeId);
				} else {
					updateAttendance(key, leaveTypeId, attendanceTypeId);
				}
			} else if (!markHalfDayLeave && (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) && isShortLeaveMarkingEnable()) {
				boolean markShortLeave = (timeInOffice != null && timeInOffice.length() != 0 && leaveTypeId != 0) ? compareTime(timeInOffice, timeShouldBeInOffice) : false;
				if (markShortLeave) {
					updateAttendance(key, leaveTypeId);
				}
			}
		}
	}

	private void updateHalfDayAttendance(Object attendanceId, int attendanceTypeId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		if (attendanceId != null) {
			row.put("__key__", attendanceId);
			row.put("attendancetypeid", attendanceTypeId);
			row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
			updates.put(Data.Update.UPDATES, row);
			ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
			ApplaneAsyncTaskManager.initiateTasksSynchronously();
		}
	}

	private boolean isShortLeaveMarkingEnable() throws JSONException {
		JSONObject mailConfigurationSetup = getMailConfigurationSetup();
		return !Translator.booleanValue(mailConfigurationSetup.opt("disable_mark_short_leave"));
	}

	private boolean isHalfDayLeaveMarkingEnable() throws JSONException {
		JSONObject mailConfigurationSetup = getMailConfigurationSetup();
		return !Translator.booleanValue(mailConfigurationSetup.opt("disable_mark_half_day_leave"));
	}

	public void updateAttendanceRest(Object employeeid, int attendanceTypeId, String backDate, Object[] details) throws JSONException, ResourceNotFoundException, ClassNotFoundException, InstantiationException, IllegalAccessException, TableNotFoundException {
		JSONObject row = new JSONObject();
		row.put("attendancetypeid", attendanceTypeId);
		row.put("employeeid", employeeid);
		row.put("attendancedate", backDate);
		row.put("attendance_status", 2);
		row.put("remarks", details[0]);
		if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
			row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		}
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
		if (attendanceTypeId != HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
			ApplaneAsyncTaskManager.initiateTasksSynchronously();
		}
	}

	public void updateAttendance(Object attendanceId, Object leaveTypeId, int attendanceTypeId) throws JSONException, ResourceNotFoundException, ClassNotFoundException, InstantiationException, IllegalAccessException, TableNotFoundException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		if (attendanceId != null && (leaveTypeId != null || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY)) {
			row.put("__key__", attendanceId);
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT) {
				row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE);
				row.put("leavetypeid", leaveTypeId);
			} else if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) {
				row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF);
			}
			row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
			updates.put(Data.Update.UPDATES, row);
			ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
			ApplaneAsyncTaskManager.initiateTasksSynchronously();
		}
	}

	private void updateAttendance(Object attendanceId, Object leaveTypeId) throws JSONException, ResourceNotFoundException, ClassNotFoundException, InstantiationException, IllegalAccessException, TableNotFoundException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		if (attendanceId != null) {
			row.put("__key__", attendanceId);
		}
		if (leaveTypeId != null) {
			row.put("leavetypeid", leaveTypeId);
		}
		row.put("attendance_type_short_leave_id", HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
		ApplaneAsyncTaskManager.initiateTasksSynchronously();
	}

	private boolean compareTime(String actualTimeInOffice, String timeShouleBeInOffice) {
		if (actualTimeInOffice == null || actualTimeInOffice.length() == 0) {
			return false;
		}
		String[] timeArray = { "00:00" };
		if (actualTimeInOffice.contains(" ")) {
			timeArray = actualTimeInOffice.split(" ");
		} else {
			timeArray = actualTimeInOffice.split(":");
		}
		String[] hoursMinutsInOffice = timeShouleBeInOffice.split(":");
		double hoursShouldBeInOffice = Integer.parseInt(hoursMinutsInOffice[0]);
		double minutsShouldBeInOffice = 0.0;
		if (hoursMinutsInOffice.length == 2) {
			minutsShouldBeInOffice = Integer.parseInt(hoursMinutsInOffice[1]);
		}

		String hours = "";
		String minuts = "";
		for (int timeCounter = 0; timeCounter < timeArray[0].length(); timeCounter++) {
			if (timeArray[0].charAt(timeCounter) >= 48 && timeArray[0].charAt(timeCounter) <= 57) {
				hours += timeArray[0].charAt(timeCounter);
			}
		}

		for (int timeCounter = 0; timeCounter < timeArray[1].length(); timeCounter++) {
			if (timeArray[1].charAt(timeCounter) >= 48 && timeArray[1].charAt(timeCounter) <= 57) {
				minuts += timeArray[1].charAt(timeCounter);
			}
		}
		if ((hoursShouldBeInOffice > Double.parseDouble(hours)) || (hoursShouldBeInOffice == Double.parseDouble(hours) && minutsShouldBeInOffice > Double.parseDouble(minuts))) {
			return true;
		}
		return false;
	}

	private JSONObject getEmployeeAttendance(String employeeCursor, String restartfrom, String backDate) throws JSONException, ParseException {

		StringBuilder filter = new StringBuilder("employeestatusid = ").append(HRISApplicationConstants.EMPLOYEE_ACTIVE);
		StringBuilder optionFilter = new StringBuilder("employeestatusid = ").append(HRISApplicationConstants.EMPLOYEE_INACTIVE);
		optionFilter.append(" AND ");
		optionFilter.append("relievingdate >= '").append(backDate).append("'");

		JSONObject parameters = null;
		if (restartfrom != null && restartfrom.trim().length() > 0) {
			parameters = new JSONObject();
			parameters.put("restartFrom", Long.valueOf(restartfrom));
			filter.append(" AND ");
			filter.append(Data.Query.KEY).append(" >= ").append("{restartFrom}");

			optionFilter.append(" AND ");
			optionFilter.append(Data.Query.KEY).append(" >= ").append("{restartFrom}");
		}

		JSONArray columns = new JSONArray();
		columns.put("employeescheduleid.is_off_day_not_fixed");
		columns.put("employeescheduleid");
		columns.put("employeescheduleid.half_day_time");
		columns.put("employeescheduleid.time_should_be_in_office");
		columns.put("holidaycalendarid");
		columns.put("leavepolicyid");
		columns.put("subbranchid");
		columns.put("deviceid");
		columns.put("name");

		JSONObject employeeQuery = new JSONObject();
		employeeQuery.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		employeeQuery.put(Data.Query.CHILDS, populateChildQuery(backDate));
		employeeQuery.put(Data.Query.COLUMNS, columns);
		// employeeQuery.put(Data.Query.FILTERS, "__key__ IN(1312)");
		employeeQuery.put(Data.Query.FILTERS, filter.toString());
		employeeQuery.put(Data.Query.OPTION_FILTERS, optionFilter.toString());
		employeeQuery.put(Data.Query.START_CURSOR, ((employeeCursor == null || employeeCursor.trim().length() == 0) ? null : employeeCursor));
		employeeQuery.put(Data.Query.MAX_ROWS, MAX_ROWS);
		employeeQuery.put(Data.Query.PARAMETERS, parameters);

		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(employeeQuery);
	}

	private JSONArray populateChildQuery(String backDate) throws JSONException {
		JSONArray columnsRequired = new JSONArray();
		columnsRequired.put("__key__");
		columnsRequired.put("attendancetypeid");
		columnsRequired.put("totaltimeinoffice");
		columnsRequired.put("lastouttime");

		JSONObject attendanceQuery = new JSONObject();
		attendanceQuery.put(Data.Query.RESOURCE, "employeeattendance");
		attendanceQuery.put(Data.Query.COLUMNS, columnsRequired);
		attendanceQuery.put(Data.Query.FILTERS, "attendancedate='" + backDate + "'");

		JSONObject child = new JSONObject();
		child.put(Data.Query.Childs.RELATED_COLUMN, "employeeid");
		child.put(Data.Query.Childs.QUERY, attendanceQuery);
		child.put(Data.Query.Childs.ALIAS, "employeeattendance");

		return new JSONArray().put(child);
	}

	public JSONObject getMailConfigurationSetup() throws JSONException {
		JSONObject attendanceConfiguration = (JSONObject) CurrentRequest.getVariable("attendance_configuration");
		if (attendanceConfiguration != null) {
			return attendanceConfiguration;
		}
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("disable_exception_report_mail");
		columnArray.put("disable_attendance_report_mail");
		columnArray.put("disable_punch_client_report_mail");
		columnArray.put("disable_daily_short_attendance_report_mail");
		columnArray.put("disable_monthly_short_attendance_report_mail");
		columnArray.put("disable_calculate_ctc");
		columnArray.put("disable_mark_short_leave");
		columnArray.put("disable_mark_half_day_leave");
		columnArray.put("disable_mark_absent");
		columnArray.put("disable_populate_attendance");
		columnArray.put("disable_carry_forward_leaves");

		JSONArray keys = new JSONArray();
		keys.put(Updates.KEY);

		JSONObject branches = new JSONObject();
		branches.put("disable_d_att_m_b_w", keys);
		columnArray.put(branches);

		branches = new JSONObject();
		branches.put("disable_d_m_s_att_m_b_w", keys);
		columnArray.put(branches);

		branches = new JSONObject();
		branches.put("disable_d_s_att_m_b_w", keys);
		columnArray.put(branches);

		branches = new JSONObject();
		branches.put("disable_p_c_m_b_w", keys);
		columnArray.put(branches);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_mailconfigurations");

		attendanceConfiguration = employeeArray.length() > 0 ? employeeArray.getJSONObject(0) : new JSONObject();
		CurrentRequest.setRequestVariable("attendance_configuration", attendanceConfiguration);
		return attendanceConfiguration;
	}

	private boolean hasMoreOrganization(int index) throws JSONException {
		return HRISApplicationConstants.ORGANIZATIONS.length > (index + 1);
	}

	private boolean isProcessEnable() throws JSONException {
		JSONObject row = getMailConfigurationSetup();
		boolean notCalculateCTC = Translator.booleanValue(row.opt("disable_calculate_ctc"));
		boolean notMarkShortLeave = Translator.booleanValue(row.opt("disable_mark_short_leave"));
		boolean notMarkHalfDayLeave = Translator.booleanValue(row.opt("disable_mark_half_day_leave"));
		boolean populateAttendanceDisabled = Translator.booleanValue(row.opt("disable_populate_attendance"));
		boolean carryForwardLeaveDisabled = Translator.booleanValue(row.opt("disable_carry_forward_leaves"));
		return !(notCalculateCTC && notMarkShortLeave && notMarkHalfDayLeave && populateAttendanceDisabled && carryForwardLeaveDisabled);
	}

	private static void iniitateSession(String organization, int index) throws OrganizationNotFoundException, JSONException {
		JSONObject organizationDetail = OrganizationCacheHandler.getOrganizationWithName(organization);
		if (index == 3 || index == 4) {
			CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_ID, 25229);
			CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_EMAIL, ApplicationConstants.ADMIN_NAVIGANT);
		} else if (index == 2) {
			CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_ID, 25230);
			CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_EMAIL, ApplicationConstants.ADMIN_NAVIGANT);
		} else {
			CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_ID, 13652);
			CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_EMAIL, ApplicationConstants.OWNER);
		}
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID, organizationDetail.get(Data.Query.KEY));
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION, organizationDetail.get(Organizations.ORGANIZATION));

		// CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID, 2);
		// CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION, "DaffodilCRM");
		// CurrentState.setCurrentVariable(CurrentSession.ENABLE_LOGS, true);

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION_ID, 1033);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION, "hris_services_a");

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPGROUP_ID, 1033);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPGROUP, "hris_services_a");

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_TIME_ZONE, -19800000);
	}
}
