package com.applane.resourceoriented.hris;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneAsyncTaskManager;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.scheduler.NightSchedulerHRIS;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.cachehandler.UserCacheHandler;
import com.applane.user.exception.UserNotFoundException;
import com.google.apphosting.api.DeadlineExceededException;

public class MarkAttendanceServlet extends HttpServlet implements ApplaneTask {

	/**
	 * This task queue servlet define on insert of takeattendance DO.
	 */
	private static final long serialVersionUID = -8634765326738382207L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		// String attDate = request.getParameter("attendancedate");// ((String[]) paramMap.get("attendancedate"))[0];
		// String attTypeId = request.getParameter("attendanceTypeId");// ((String[]) paramMap.get("attendanceTypeId"))[0];
		// String branchIdString = request.getParameter("branchid");// ((String[]) paramMap.get("branchid"))[0];
		// String temp = request.getParameter("temp");// paramMap.get("temp");
		// String __key__ = request.getParameter("__key__");
		//
		// String dayBetween = request.getParameter("dayBetween");// ((String[]) paramMap.get("branchid"))[0];
		// String counter = request.getParameter("counter");// paramMap.get("temp");

		// readAttendanceParameters(attDate, attTypeId, branchIdString, temp, __key__, dayBetween, counter, );
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	}

	@Override
	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		JSONObject taskInfo = applaneTaskInfo.getTaskInfo();

		String attDate = taskInfo.optString("attendancedate");
		String attTypeId = taskInfo.optString("attendanceTypeId");
		String branchIdString = taskInfo.optString("branchid");
		String temp = taskInfo.optString("temp");
		String __key__ = taskInfo.optString(Updates.KEY);

		String dayBetween = taskInfo.optString("dayBetween");
		String counter = taskInfo.optString("counter");

		if (temp != null && temp.equals(HRISApplicationConstants.TEMP_VALUE_FROM_NIGHT_SCHEDULER_POPULATE_ATTENDANCE)) {
			Object organizationCounter = taskInfo.opt(NightSchedulerHRIS.ORGANIZATION_COUNTER);
			String[] organizations = HRISApplicationConstants.ORGANIZATIONS;
			int orgCounter = Integer.valueOf("" + organizationCounter);
			String orgName = organizations[orgCounter];
			String owner = "kapil.dalal@daffodilsw.com";
			Object userId = 13652;
			try {
				PunchingUtility.setValueInSession(orgName, userId, owner);
			} catch (Exception e) {
				new MorningSchdularHRIS().sendMailInCaseOfException(e, "NightSchedulerHRIS Mark Attendance Servlet Failed");
			}
		}
		readAttendanceParameters(attDate, attTypeId, branchIdString, temp, __key__, dayBetween, counter, taskInfo);
	}

	public static void readAttendanceParameters(String attDateString, String attTypeId, String branchIdString, String temp, String __key__, String dayBetweenString, String counterString, JSONObject taskInfo) {
		// String values = "";
		int employeeArrayCount = 0;
		String employeeArrayString = "";
		try {
			long attendanceTypeId = 0;
			if (attTypeId != null && attTypeId.length() > 0) {
				attendanceTypeId = Long.parseLong(attTypeId);
			}
			long branchId = 0;
			if (branchIdString != null && branchIdString.length() > 0) {
				branchId = Long.parseLong(branchIdString);
			}

			JSONArray employeeArray = getEmployeeArray(attDateString, branchId, __key__);

			employeeArrayString += employeeArray.toString();
			employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
			if (attDateString != null && attDateString.length() > 0) {
				Date attendanceDate = Translator.dateValue(attDateString);
				HashMap<Date, Integer> offDaysMap = new HashMap<Date, Integer>();
				HashMap<Date, Integer> holidaysMap = new HashMap<Date, Integer>();
				HashMap<Date, Object[]> dailyAttendanceMap = null;

				// Date monthFirstDate = DataTypeUtilities.getFirstDayOfMonth(attendanceDate);
				// Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
				// String monthFirstDateInString = EmployeeMonthlyAttendance.getDateInString(monthFirstDate);
				// String monthLastDateInString = EmployeeMonthlyAttendance.getDateInString(monthLastDate);
				int tempScheduleId = 0;
				int tempHolidayCalendarId = 0;
				Calendar cal = Calendar.getInstance();
				int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
				for (int counter = 0; counter < employeeArrayCount; counter++) {
					JSONObject employee = employeeArray.getJSONObject(counter);
					try {
						long tempAttendanceTypeId = attendanceTypeId;
						Object employeeId = employee.opt("__key__");
						__key__ = "" + employeeId;
						Object attendanceId = isExist(employeeId, attDateString);
						if (attendanceId == null) {// || organizationId == 10719) {
							Object scheduleId = employee.opt("employeescheduleid");
							Object holidayCalendarId = employee.opt("holidaycalendarid");

							int originalScheduleId = Translator.integerValue(scheduleId);
							int originalHolidayCalendarId = Translator.integerValue(holidayCalendarId);

							String officialEmailId = employee.optString("officialemailid");
							String deviceId = employee.optString("deviceid", null);
							if (organizationId == 10719 && deviceId != null && deviceId.equalsIgnoreCase("REMOTE EMP")) {
								tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_TOUR;
							}
							// else if (attendanceId != null) {
							// continue;
							// }
							Object employeeUserId = null;
							try {
								employeeUserId = PunchingUtility.getId("up_users", "emailid", officialEmailId);
							} catch (Exception e) {
								/* No user exist with this email id. Take attendance of next employee */
								// continue;
							}

							JSONArray leavesArray = getLeaveRequestRecords(employeeId, attDateString);
							int status = 0;
							if (leavesArray != null && leavesArray.length() > 0) {
								status = leavesArray.getJSONObject(0).optInt("leavestatusid");
							}
							if (status == HRISConstants.LEAVE_APPROVED) {
								tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_LEAVE;
								int durationType = isHalfDayLeave(attendanceDate, leavesArray);
								if (durationType == HRISConstants.DURATION_FIRSTHALF) {
									tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE;
								} else if (durationType == HRISConstants.DURATION_SECONDHALF) {
									tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE;
								}
								Object leaveRequestId = null;
								leaveRequestId = leavesArray.getJSONObject(0).opt("__key__");
								Object leaveTypeId = leavesArray.getJSONObject(0).opt("leavetypeid");
								Object paidStatus = leavesArray.getJSONObject(0).opt("paidstatus");
								updateLeaveAttendance(tempAttendanceTypeId, employeeId, attDateString, leaveRequestId, employeeUserId, leaveTypeId, paidStatus);
							} else {
								String remarks = "";
								if (status == HRISConstants.LEAVE_NEW) {
									remarks = "Leave Applied";
								}
								if (originalScheduleId != tempScheduleId) {
									tempScheduleId = originalScheduleId;
									offDaysMap = new HashMap<Date, Integer>();
									new EmployeeMonthlyAttendance().getTotalOffAndHolidaysMaps(attendanceDate, attDateString, attendanceDate, attDateString, offDaysMap, null, dailyAttendanceMap, scheduleId, holidayCalendarId, employeeId);
								}
								if (originalHolidayCalendarId != tempHolidayCalendarId) {
									tempHolidayCalendarId = originalHolidayCalendarId;
									holidaysMap = new HashMap<Date, Integer>();
									new EmployeeMonthlyAttendance().getTotalOffAndHolidaysMaps(attendanceDate, attDateString, attendanceDate, attDateString, null, holidaysMap, dailyAttendanceMap, scheduleId, holidayCalendarId, employeeId);
								}

								if (offDaysMap != null && offDaysMap.containsKey(attendanceDate)) {
									cal.setTime(attendanceDate);
									int weekDayNumeric = cal.get(Calendar.DAY_OF_WEEK);
									int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
									JSONArray offDay = EmployeeMonthlyAttendance.getHalfOffDay(scheduleId, weekDayNumeric, weekOfMonth);
									if (offDay != null && offDay.length() > 0) {
										tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_HALF_DAY_OFF;
									} else {
										tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_OFF;
									}
								} else if (holidaysMap.containsKey(attendanceDate)) {
									tempAttendanceTypeId = HRISApplicationConstants.ATTENDANCE_HOLIDAY;
								}
								// values += "offDaysMap >> " + offDaysMap + " << holidaysMap >> " + holidaysMap + " << tempAttendanceTypeId >> " + tempAttendanceTypeId + " << employeeeid >> " + employeeId;
								updateStaffAttendance(tempAttendanceTypeId, employeeId, attDateString, employeeUserId, remarks);
								// updateStaffAttendance(tempAttendanceTypeId, employeeId, attDateString, employeeUserId, remarks, attendanceId);
							}
						}
					} catch (Exception e) {
						int key = 0;
						int tempKey = 0;
						if (__key__ != null && __key__.length() > 0) {
							tempKey = key = (Integer.parseInt(__key__));
							__key__ = "" + (key + 1);
						}
						sendExceptionTraceMessage(employee.toString(), e, "Error for Employee with key >> " + tempKey);
					}
				}
			}
			ApplaneAsyncTaskManager.initiateTasksSynchronously();
			if (employeeArray.length() > 0) {
				initiateAgainTaskQueue(attDateString, __key__, attendanceTypeId, branchId, temp, dayBetweenString, counterString, taskInfo);
			} else if (temp != null && temp.equals(HRISApplicationConstants.TEMP_VALUE_FROM_POPULATE_MONTHLY_ATTENDANCE)) {
				int daysBetween = Integer.parseInt(dayBetweenString);
				int counter = Integer.parseInt(counterString);
				if (counter < daysBetween) {
					counter++;
					Calendar cal2 = new GregorianCalendar();
					cal2.setTime(DataTypeUtilities.checkDateFormat(attDateString));
					cal2.add(Calendar.DATE, 1);
					Date date = cal2.getTime();
					String dateInString = EmployeeSalaryGenerationServlet.getDateInString(date);
					String fromMonthlyAttendance = HRISApplicationConstants.TEMP_VALUE_FROM_POPULATE_MONTHLY_ATTENDANCE;

					PopulateMonthlyAttendanceBusinessLogic.insertTakeAttendanceRecord(dateInString, branchId, (int) attendanceTypeId, (long) daysBetween, counter, fromMonthlyAttendance);
				}

			} else if (temp != null && temp.equals(HRISApplicationConstants.TEMP_VALUE_FROM_NIGHT_SCHEDULER_POPULATE_ATTENDANCE)) {
				NightSchedulerHRIS.initiateTaskQueue("" + taskInfo.opt(NightSchedulerHRIS.ORGANIZATION_COUNTER), NightSchedulerHRIS.POPULATE_ATTENDANCE, "" + branchId, true, null, null);
			}

		} catch (Exception e) {
			sendExceptionTraceMessage(employeeArrayString, e, "outside for loop exception");
		}
	}

	private static void sendExceptionTraceMessage(String employeeArrayString, Exception e, String extraMessage) {
		String traces = ExceptionUtils.getExceptionTraceMessage("MarkAttendanceServlet", e);
		HrisHelper.sendMails(new String[] { "kapil.dalal@daffodilsw.com" }, employeeArrayString + " < trace > " + traces + " << extraMessage >> " + extraMessage, "employees values");
	}

	private static void initiateAgainTaskQueue(String attDate, String __key__, long attendanceTypeId, long branchId, Object temp, String dayBetweenString, String counterString, JSONObject taskInfo) throws JSONException {

		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("attendancedate", "" + attDate);
		taskQueueParameters.put("attendanceTypeId", "" + attendanceTypeId);
		taskQueueParameters.put("branchid", "" + branchId);
		taskQueueParameters.put("dayBetween", dayBetweenString);
		taskQueueParameters.put("counter", counterString);
		taskQueueParameters.put(Updates.KEY, __key__);
		taskQueueParameters.put("temp", temp == null ? "" : ("" + temp));
		taskQueueParameters.put(NightSchedulerHRIS.ORGANIZATION_COUNTER, taskInfo.opt(NightSchedulerHRIS.ORGANIZATION_COUNTER));
		taskQueueParameters.put(NightSchedulerHRIS.PURPOSE, taskInfo.opt(NightSchedulerHRIS.PURPOSE));
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.MarkAttendanceServlet", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);
	}

	private static JSONArray getEmployeeArray(String attDate, long branchId, String __key__) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("officialemailid");
		columnArray.put("employeescheduleid");
		columnArray.put("holidaycalendarid");
		columnArray.put("deviceid");
		columnArray.put("leavepolicyid.issandwich");
		query.put(Data.Query.COLUMNS, columnArray);
		String extraFilter = "";
		if (__key__ != null && __key__.length() > 0) {
			extraFilter = " and " + Updates.KEY + " > " + __key__;
		}
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + " and branchid = " + branchId + " and joiningdate <= '" + attDate + "'" + extraFilter);
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and relievingdate >= '" + attDate + "' and joiningdate <= '" + attDate + "' and branchid = " + branchId + extraFilter);
		query.put(Data.Query.MAX_ROWS, 15);
		JSONObject employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	// private static void checkLeaveRecordsForOffDays(Object employeeId, String attDate, long attendanceTypeId, List<String> attendanceMarked, Date attendanceDate, Object employeeUserId, String officialEmailId) throws JSONException {
	// JSONArray leavesArray = getLeaveRequestRecords(employeeId, attDate);
	// int status = 0;
	// if (leavesArray != null && leavesArray.length() > 0) {
	// status = leavesArray.getJSONObject(0).optInt("leavestatusid");
	// }
	// if (status == HRISConstants.LEAVE_APPROVED) {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_LEAVE;
	// int durationType = isHalfDayLeave(attendanceDate, leavesArray);
	// if (durationType == HRISConstants.DURATION_FIRSTHALF) {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE;
	// } else if (durationType == HRISConstants.DURATION_SECONDHALF) {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE;
	// }
	//
	// int leaveArrayCount = (leavesArray == null || leavesArray.length() == 0) ? 0 : leavesArray.length();
	// Object leaveRequestId = null;
	// if (leaveArrayCount > 0) {
	// leaveRequestId = leavesArray.getJSONObject(0).opt("__key__");
	// Object leaveTypeId = leavesArray.getJSONObject(0).opt("leavetypeid");
	// updateLeaveAttendance(attendanceTypeId, employeeId, attDate, leaveRequestId, employeeUserId, leaveTypeId);
	// attendanceMarked.add(officialEmailId);
	// }
	// } else if (status == HRISConstants.LEAVE_NEW) {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_OFF;
	// String remarks = "Leave Applied";
	// updateStaffAttendance(attendanceTypeId, employeeId, attDate, employeeUserId, remarks);
	// } else {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_OFF;
	// updateStaffAttendance(attendanceTypeId, employeeId, attDate, employeeUserId, "");
	// attendanceMarked.add(officialEmailId);
	// }
	// }

	// private static void checkLeaveRecordsForHoliday(Object employeeId, String attDate, long attendanceTypeId, List<String> attendanceMarked, Date attendanceDate, Object employeeUserId, String officialEmailId) throws JSONException {
	// JSONArray leavesArray = getLeaveRequestRecords(employeeId, attDate);
	//
	// int status = 0;
	// if (leavesArray != null && leavesArray.length() > 0) {
	// status = leavesArray.getJSONObject(0).optInt("leavestatusid");
	// }
	// if (status == HRISConstants.LEAVE_APPROVED) {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_LEAVE;
	// int durationType = isHalfDayLeave(attendanceDate, leavesArray);
	// if (durationType == HRISConstants.DURATION_FIRSTHALF) {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE;
	// } else if (durationType == HRISConstants.DURATION_SECONDHALF) {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE;
	// }
	//
	// int leaveArrayCount = (leavesArray == null || leavesArray.length() == 0) ? 0 : leavesArray.length();
	// Object leaveRequestId = null;
	// if (leaveArrayCount > 0) {
	// leaveRequestId = leavesArray.getJSONObject(0).opt("__key__");
	// Object leaveTypeId = leavesArray.getJSONObject(0).opt("leavetypeid");
	// updateLeaveAttendance(attendanceTypeId, employeeId, attDate, leaveRequestId, employeeUserId, leaveTypeId);
	// attendanceMarked.add(officialEmailId);
	// }
	// } else if (status == HRISConstants.LEAVE_NEW) {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_HOLIDAY;
	// String remarks = "Leave Applied";
	// updateStaffAttendance(attendanceTypeId, employeeId, attDate, employeeUserId, remarks);
	// } else {
	// attendanceTypeId = HRISApplicationConstants.ATTENDANCE_HOLIDAY;
	// updateStaffAttendance(attendanceTypeId, employeeId, attDate, employeeUserId, "");
	// attendanceMarked.add(officialEmailId);
	// }
	// }

	// private static void sendStatus(List<String> attendanceMarked, List<String> attendanceFailed, String branch, int employeeCount) {
	// if (attendanceFailed.size() > 0) {
	// StringBuilder message = new StringBuilder(attendanceFailed.size() + " Employees whose attendance could not be taken. List given below:-<br>");
	// message.append(attendanceFailed.toString()).append("<br><br>");
	// message.append(attendanceMarked.size() + " Employees whose attendance has been taken successfully. List Given below:-<br>");
	// message.append(attendanceMarked.toString());
	// new ApplaneMail().sendExceptionMail(new String[] { "kapil.dalal@daffodilsw.com", "gaurav.sharma@daffodilsw.com" }, null, null, "Daily Attendance status of " + employeeCount + " employees for this branch " + branch, message.toString(), true);
	// }
	// }

	public static void updateStaffAttendance(long attendanceTypeId, Object employeeId, String attendanceDate, Object employeeUserId, String remarks, Object attendanceId) throws JSONException {

		// if (!isExist(employeeId, attendanceDate)) {

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		if (attendanceId != null) {
			row.put("__key__", attendanceId);
		}row.put("attendance_status", 2);
		row.put("attendancetypeid", attendanceTypeId);
		row.put("employeeid", employeeId);
		row.put("attendancedate", attendanceDate);
		if (remarks.length() > 0) {
			row.put("remarks", remarks);
		}
		row.put("employeeuserid", employeeUserId == null ? JSONObject.NULL : employeeUserId);
		if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
			row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		} else {
			row.put("temp", HRISApplicationConstants.TEMP_VALUE_STOP_POPULATEMONTHLY_ATTENDANCE);
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
		// }
	}

	public static void updateStaffAttendance(long attendanceTypeId, Object employeeId, String attendanceDate, Object employeeUserId, String remarks) throws JSONException {

		// if (!isExist(employeeId, attendanceDate)) {

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("attendancetypeid", attendanceTypeId);
		row.put("employeeid", employeeId);
		row.put("attendancedate", attendanceDate);
		row.put("attendance_status", 2);
		if (remarks.length() > 0) {
			row.put("remarks", remarks);
		}
		row.put("employeeuserid", employeeUserId == null ? JSONObject.NULL : employeeUserId);
		if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
			row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		} else {
			row.put("temp", HRISApplicationConstants.TEMP_VALUE_STOP_POPULATEMONTHLY_ATTENDANCE);
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
		// }
	}

	public static void updateLeaveAttendance(long attendanceTypeId, Object employeeId, String attendanceDate, Object leaveRequestId, Object employeeUserId, Object leaveTypeId, Object paidStatus) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("attendancetypeid", attendanceTypeId);
		row.put("employeeid", employeeId);
		row.put("attendancedate", attendanceDate);
		row.put("employeeleaveid", leaveRequestId);
		row.put("leavetypeid", leaveTypeId);
		row.put("paidstatus", paidStatus);
		row.put("attendance_status", 2);
		row.put("employeeuserid", employeeUserId == null ? JSONObject.NULL : employeeUserId);
		if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
			row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		} else {
			// row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
			row.put("temp", HRISApplicationConstants.TEMP_VALUE_STOP_POPULATEMONTHLY_ATTENDANCE);
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static JSONArray getMonthlyOffRecords(Object scheduleId, int weekNo, int weekDayNo) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_monthlyoff");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("off_day_type");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "scheduleid = " + scheduleId + " and weeknoid = " + weekNo + " and weekdayid = " + weekDayNo);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_monthlyoff");
		return array;
		// int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		// return arrayCount;
	}

	public static JSONArray getScheduleRecord(String weekDay, Object scheduleId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_schedules");
		JSONArray columnArray = new JSONArray();
		columnArray.put(weekDay);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + scheduleId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_schedules");
		return array;

	}

	public static int getHolidayRecords(Object holidayCalendarId, String attDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidays");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate = '" + attDate + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_holidays");
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		return arrayCount;

	}

	public static JSONArray getLeaveRequestRecords(Object employeeId, String attDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverequests");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("leavestatusid");
		columnArray.put("leavetypeid");
		columnArray.put("fromdate");
		columnArray.put("todate");
		columnArray.put("paidstatus");
		columnArray.put("fromdurationtypeid");
		columnArray.put("todurationtypeid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and leavestatusid IN(" + HRISConstants.LEAVE_APPROVED + "," + HRISConstants.LEAVE_NEW + ") and todate >= '" + attDate + "' and fromdate <= '" + attDate + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_leaverequests");
		return array;

	}

	public static int isHalfDayLeave(Date attandanceDate, JSONArray leavesArray) throws JSONException {
		int leaveArrayCount = (leavesArray == null || leavesArray.length() == 0) ? 0 : leavesArray.length();
		if (leaveArrayCount > 0) {
			// Object leaveRequestId = leavesArray.getJSONObject(0).opt("__key__");
			Object fromDateObject = leavesArray.getJSONObject(0).opt("fromdate");
			Date fromDate = DataTypeUtilities.checkDateFormat(fromDateObject);
			Object toDateObject = leavesArray.getJSONObject(0).opt("todate");
			Date toDate = DataTypeUtilities.checkDateFormat(toDateObject);
			Integer fromDurationTypeId = (Integer) leavesArray.getJSONObject(0).opt("fromdurationtypeid");
			Integer toDurationTypeId = (Integer) leavesArray.getJSONObject(0).opt("todurationtypeid");
			if (attandanceDate.equals(fromDate) && fromDurationTypeId == HRISConstants.DURATION_SECONDHALF) {
				return HRISConstants.DURATION_SECONDHALF;
			} else if (attandanceDate.equals(toDate) && toDurationTypeId == HRISConstants.DURATION_FIRSTHALF) {
				return HRISConstants.DURATION_FIRSTHALF;
			} else {
				return 0;
			}
		}
		return 0;
	}

	public static Object getEmployeeUserId(String officialemailid) throws JSONException {
		try {
			return UserCacheHandler.getUser(officialemailid).get(Data.Query.KEY);
		} catch (UserNotFoundException e) {
			return null;
		}
	}

	private static Object isExist(Object employeeId, String attendanceDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeId = " + employeeId + " and attendanceDate = '" + attendanceDate + "'");
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeeattendance");
		if (rows != null && rows.length() > 0) {
			return rows.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	// private static boolean isExist(Object employeeId, String attendanceDate) throws JSONException {
	// JSONObject query = new JSONObject();
	// query.put(Data.Query.RESOURCE, "employeeattendance");
	// JSONArray array = new JSONArray();
	// array.put("__key__");
	// query.put(Data.Query.COLUMNS, array);
	// query.put(Data.Query.FILTERS, "employeeId = " + employeeId + " and attendanceDate = '" + attendanceDate + "'");
	// JSONObject attendanceObject;
	// attendanceObject = ResourceEngine.query(query);
	// JSONArray rows = attendanceObject.getJSONArray("employeeattendance");
	// if (rows != null && rows.length() > 0) {
	// return true;
	// } else {
	// return false;
	// }
	// }
}
