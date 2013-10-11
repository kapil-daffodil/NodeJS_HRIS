package com.applane.resourceoriented.hris.leave;

import java.text.SimpleDateFormat;
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
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeCardPunchDataBusinessLogic;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.1
 * @category HRIS businesslogic
 */
public class LeaveRequestValidation implements OperationJob {
	private static final String LEAVE_CAN_APPLY_FOR_FUTURE = "For Future";
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		boolean isOffDaysNotFixed = Translator.booleanValue(record.getValue("employeeid.employeescheduleid.is_off_day_not_fixed"));
		record.removeUpdate("employeeid.employeescheduleid.is_off_day_not_fixed");
		if (record.has("temp")) {
			return;
		}
		int organizationId = 0;
		try {
			organizationId = Translator.integerValue(CurrentState.getCurrentState().get(CurrentSession.CURRENT_ORGANIZATION_ID));
		} catch (Exception e) {
			String message = ExceptionUtils.getExceptionTraceMessage("LeaveRequestValidation", e);
			LogUtility.writeLog("LeaveRequestValidation >> onBeforeUpdate >> geting organization id >> message is : >> " + message);
			throw new BusinessLogicException("Some unknown error ocuured while applying leave validation.");
		}
		Object leaveBalance = record.getValue("hris_balanceleaves");
		JSONArray leaveBalanceArray = new JSONArray();
		String operation = record.getOperationType();
		Object leaveRequestFromDateObject = record.getValue("fromdate");
		Object leaveRequestToDateObject = record.getValue("todate");
		Date fromDate = Translator.dateValue(leaveRequestFromDateObject);
		Date toDate = Translator.dateValue(leaveRequestToDateObject);
		Integer fromDurationtypeId = (Integer) record.getValue("fromdurationtypeid");
		Integer toDurationtypeId = (Integer) record.getValue("todurationtypeid");
		Object leaveCanApply = record.removeUpdate("employeeid.leavepolicyid.leave_can_apply");
		int statusId = Translator.integerValue(record.getValue("statusid"));
		if (leaveBalance != null && leaveBalance instanceof JSONObject) {
			leaveBalanceArray.put(leaveBalance);
		} else if (leaveBalance != null && leaveBalance instanceof JSONArray) {
			leaveBalanceArray = (JSONArray) leaveBalance;
		}

		long d = DataTypeUtilities.differenceBetweenDates(fromDate, toDate) + 1;
		double difference = (double) d;
		if (organizationId == 7783 && difference >= 2 && statusId == HRISConstants.LEAVE_NEW) {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Date todayDate = Translator.dateValue(TODAY_DATE);
			long before = DataTypeUtilities.differenceBetweenDates(todayDate, fromDate) + 1;
			double beforeDays = (double) before;
			if (beforeDays < 14) {
				throw new BusinessLogicException("You can not apply 2 or more days leave with in two weeks. You have to request before two weeks.");
			}
		}
		if (leaveBalanceArray != null && leaveBalanceArray.length() > 0) {
			try {
				double balance = Translator.doubleValue(leaveBalanceArray.getJSONObject(0).opt("balanceleaves"));
				if (fromDurationtypeId == HRISApplicationConstants.DURATION_FIRSTHALF || fromDurationtypeId == HRISApplicationConstants.DURATION_SECONDHALF) {
					difference -= 0.50;
				}
				if (toDurationtypeId == HRISApplicationConstants.DURATION_FIRSTHALF || toDurationtypeId == HRISApplicationConstants.DURATION_SECONDHALF) {
					difference -= 0.50;
				}
				if (balance < difference) {
					WarningCollector.collect("You have applied more then balance leaves.");
				}
			} catch (DeadlineExceededException e) {
				throw new BusinessLogicException("DeadlineExceededException occured.");
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				String message = ExceptionUtils.getExceptionTraceMessage("LeaveRequestValidation", e);
				LogUtility.writeLog("LeaveRequestValidation >> onBeforeUpdate >> message is : >> " + message);
				throw new BusinessLogicException("Some unknown error ocuured while applying leave validation.");
			}
		}
		Object employeeId = record.getValue("employeeid");
		try {
			if (employeeId instanceof JSONObject) {
				employeeId = ((JSONObject) employeeId).opt(Updates.KEY);
			}
			if (employeeId instanceof JSONArray) {
				employeeId = ((JSONArray) employeeId).length() == 0 ? 0 : ((JSONArray) employeeId).getJSONObject(0).opt(Updates.KEY);
			}
		} catch (DeadlineExceededException e) {
			throw new BusinessLogicException("DeadlineExceededException occured.");
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String message = ExceptionUtils.getExceptionTraceMessage("LeaveRequestValidation", e);
			LogUtility.writeLog("LeaveRequestValidation >> onBeforeUpdate >> message is : >> " + message);
			throw new BusinessLogicException("Some unknown error ocuured while applying leave validation.");
		}

		if (leaveCanApply != null && ("" + leaveCanApply).equals(LEAVE_CAN_APPLY_FOR_FUTURE)) {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());
			Date todayDate = DataTypeUtilities.checkDateFormat(TODAY_DATE);
			JSONArray cutOffTimeFromShiftHistoryArray = new EmployeeCardPunchDataBusinessLogic().getCutOffTimeFromShiftHistory(employeeId, leaveRequestFromDateObject);
			Object fromTime = null;
			Object fromMeridiem = null;
			if (cutOffTimeFromShiftHistoryArray != null && cutOffTimeFromShiftHistoryArray.length() > 0) {
				try {
					fromTime = cutOffTimeFromShiftHistoryArray.getJSONObject(0).opt("shift_id.fromtime_time");
					fromMeridiem = cutOffTimeFromShiftHistoryArray.getJSONObject(0).opt("shift_id.fromtime_meridiem");
					if (fromTime != null) {
						String[] time = ("" + fromTime).split(":");

						int hour = Translator.integerValue(time[0]);
						if (("" + fromMeridiem).equalsIgnoreCase("PM")) {
							if (hour < 12) {
								hour += 12;
							}
						}
						Calendar cal = Calendar.getInstance();
						cal.setTime(fromDate);
						cal.set(Calendar.HOUR_OF_DAY, hour);
						if (time.length > 1) {
							cal.set(Calendar.MINUTE, Translator.integerValue(time[1]));
						}
						fromDate = cal.getTime();
					}
				} catch (DeadlineExceededException e) {
					throw new BusinessLogicException("DeadlineExceededException occured.");
				} catch (BusinessLogicException e) {
					throw e;
				} catch (Exception e) {
					String message = ExceptionUtils.getExceptionTraceMessage("LeaveRequestValidation", e);
					LogUtility.writeLog("LeaveRequestValidation >> onBeforeUpdate >> assigned shift history >> message is : >> " + message);
					throw new BusinessLogicException("Some unknown error ocuured while applying leave validation.");
				}
			} else {
				JSONArray cutOffTimeFromAssignedShift = new EmployeeCardPunchDataBusinessLogic().getCutOffTimeFromAssignedShift(employeeId);
				if (cutOffTimeFromAssignedShift != null && cutOffTimeFromAssignedShift.length() > 0) {
					try {
						fromTime = cutOffTimeFromAssignedShift.getJSONObject(0).opt("shift_id.fromtime_time");
						fromMeridiem = cutOffTimeFromAssignedShift.getJSONObject(0).opt("shift_id.fromtime_meridiem");
						if (fromTime != null) {
							String[] time = ("" + fromTime).split(":");

							int hour = Translator.integerValue(time[0]);
							if (("" + fromMeridiem).equalsIgnoreCase("PM")) {
								if (hour < 12) {
									hour += 12;
								}
							}
							Calendar cal = Calendar.getInstance();
							cal.setTime(fromDate);
							cal.set(Calendar.HOUR_OF_DAY, hour);
							if (time.length > 1) {
								cal.set(Calendar.MINUTE, Translator.integerValue(time[1]));
							}
							fromDate = cal.getTime();
						}
					} catch (DeadlineExceededException e) {
						throw new BusinessLogicException("DeadlineExceededException occured.");
					} catch (BusinessLogicException e) {
						throw e;
					} catch (Exception e) {
						String message = ExceptionUtils.getExceptionTraceMessage("LeaveRequestValidation", e);
						LogUtility.writeLog("LeaveRequestValidation >> onBeforeUpdate >> assignedShift >> message is : >> " + message);
						throw new BusinessLogicException("Some unknown error ocuured while applying leave validation.");
					}
				}
			}
			if (!todayDate.before(fromDate)) {
				throw new BusinessLogicException("You can not apply leave for past and current date.");
			}
		}
		if (operation.equalsIgnoreCase("insert")) {
			try {
				Object holidayCalendarId = record.getValue("employeeid.holidaycalendarid");
				Integer alternateHoliday = (Integer) record.getValue("alternateholiday");
				record.removeUpdate("employeeid.holidaycalendarid");
				JSONArray rows = getEmployeeRecords(employeeId);
				int rowCount = rows == null ? 0 : rows.length();
				JSONObject employeeRecord = new JSONObject();
				if (rowCount > 0) {
					employeeRecord = rows.getJSONObject(0);
				}
				// Handle all leave validations while leave applying
				if (leaveRequestFromDateObject != null && leaveRequestToDateObject != null) {
					if (alternateHoliday == HRISApplicationConstants.EmployeeDecision.YES) {

						if (!fromDate.equals(Translator.dateValue(leaveRequestToDateObject))) {
							throw new BusinessLogicException("For Alternate Holiday Leave Request \"From Date\" and \"To Date\" should be same.");
						}
						Object leaveKey = null;
						boolean isHoliday = false;
						double maximumHolidays = 0.0;
						Object limitStartDate = null;
						Object limitEndDate = null;
						JSONArray holidayCalenderArray = getHolidayCalenderArray(holidayCalendarId);
						if (holidayCalenderArray != null && holidayCalenderArray.length() > 0) {

							Object limit = holidayCalenderArray.getJSONObject(0).opt("alternateholidaysmaxallowed");
							if (limit != null) {
								maximumHolidays = Translator.doubleValue(limit);
							}
							limitStartDate = holidayCalenderArray.getJSONObject(0).opt("fromdate");
							limitEndDate = holidayCalenderArray.getJSONObject(0).opt("todate");
							if (organizationId != 606) {
								JSONArray alternateHolidaysArray = getAlternateHolidaysArray(holidayCalendarId);
								for (int counter = 0; counter < alternateHolidaysArray.length(); counter++) {
									Object holiDay = alternateHolidaysArray.getJSONObject(counter).opt("holiday");
									if (holiDay != null && Translator.dateValue(holiDay).equals(Translator.dateValue(leaveRequestFromDateObject))) {
										isHoliday = true;
										break;
									}
								}
								if (!isHoliday) {
									throw new BusinessLogicException(leaveRequestFromDateObject + " is not your alternate Holiday, Select '\"NO\" in Alternate Holiday' column or Contact to HR Department.");
								}
							}
							JSONArray takenAlternateHoliday = getTakenAlternateHolidays(leaveKey, employeeId, limitStartDate, limitEndDate);
							if (((float) takenAlternateHoliday.length()) >= maximumHolidays) {
								throw new BusinessLogicException("You Have Already Applied " + takenAlternateHoliday.length() + " Alternate Leaves");
							}
						}
					}
					com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.leaveRequestValidation(leaveRequestFromDateObject, leaveRequestToDateObject, employeeId, fromDurationtypeId, toDurationtypeId, employeeRecord, isOffDaysNotFixed);
					// Handle request validation on already appliying dates
					com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.leaveRequestDuplicateValidation(leaveRequestFromDateObject, leaveRequestToDateObject, employeeId);
				}
			} catch (DeadlineExceededException e) {
				throw new BusinessLogicException("DeadlineExceededException occured.");
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				String message = ExceptionUtils.getExceptionTraceMessage("LeaveRequestValidation", e);
				LogUtility.writeLog("LeaveRequestValidation >> onBeforeUpdate >> message is : >> " + message);
				throw new BusinessLogicException("Some unknown error ocuured while applying leave validation.");
			}
		} else if (operation.equalsIgnoreCase("update")) {
			try {
				Object employeeLeaveId = record.getValue(Updates.KEY);
				int newStatus = (Integer) record.getValue("leavestatusid");
				int oldStatus = (Integer) record.getOldValue("leavestatusid");
				Object leaveTypeId = record.getOldValue("leavetypeid");
				String approverComment = (String) record.getValue("approvercomment");
				Object leaveFromDateObject = record.getValue("fromdate");
				Object leaveToDateObject = record.getValue("todate");
				// Integer fromDurationtypeId = (Integer) record.getValue("fromdurationtypeid");
				// Integer toDurationtypeId = (Integer) record.getValue("todurationtypeid");
				JSONArray rows = getEmployeeRecords(employeeId);
				int rowCount = rows == null ? 0 : rows.length();
				JSONObject employeeRecord = new JSONObject();
				if (rowCount > 0) {
					employeeRecord = rows.getJSONObject(0);
				}
				if (record.has("alternateholiday")) {
					Integer alternateHoliday = (Integer) record.getValue("alternateholiday");

					Object holidayCalendarId = getHolidayCalendarId(employeeId);
					if (alternateHoliday == HRISApplicationConstants.EmployeeDecision.YES) {
						if (!fromDate.equals(Translator.dateValue(leaveToDateObject))) {
							throw new BusinessLogicException("For Alternate Holiday Leave Request \"From Date\" and \"To Date\" should be same.");
						}
						Object leaveKey = null;
						boolean isHoliday = false;
						double maximumHolidays = 0.0;
						Object limitStartDate = null;
						Object limitEndDate = null;
						JSONArray holidayCalenderArray = getHolidayCalenderArray(holidayCalendarId);
						if (holidayCalenderArray != null && holidayCalenderArray.length() > 0) {

							Object limit = holidayCalenderArray.getJSONObject(0).opt("alternateholidaysmaxallowed");
							if (limit != null) {
								maximumHolidays = Translator.doubleValue(limit);
							}
							limitStartDate = holidayCalenderArray.getJSONObject(0).opt("fromdate");
							limitEndDate = holidayCalenderArray.getJSONObject(0).opt("todate");
							if (organizationId != 606) {
								JSONArray alternateHolidaysArray = getAlternateHolidaysArray(holidayCalendarId);
								for (int counter = 0; counter < alternateHolidaysArray.length(); counter++) {
									Object holiDay = alternateHolidaysArray.getJSONObject(counter).opt("holiday");
									if (holiDay != null && Translator.dateValue(holiDay).equals(Translator.dateValue(leaveFromDateObject))) {
										isHoliday = true;
										break;
									}
								}
								if (!isHoliday) {
									throw new BusinessLogicException(leaveToDateObject + " is not your alternate Holiday, Select '\"NO\" in Alternate Holiday' column or Contact to HR Department.");
								}
							}
							JSONArray takenAlternateHoliday = getTakenAlternateHolidays(leaveKey, employeeId, limitStartDate, limitEndDate);
							if (((float) takenAlternateHoliday.length()) >= maximumHolidays) {
								throw new BusinessLogicException("You Have Already Applied " + takenAlternateHoliday.length() + " Alternate Leaves");
							}
						}
					}
				}
				String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
				com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.leaveRequestValidation(leaveFromDateObject, leaveToDateObject, employeeId, fromDurationtypeId, toDurationtypeId, employeeRecord, isOffDaysNotFixed);
				com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.leaveRequestDuplicateValidationInUpdate(leaveFromDateObject, leaveToDateObject, employeeId, employeeLeaveId);
				com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.approvedLeaveUpdation(newStatus, oldStatus, record, employeeLeaveId, approverComment, currentDate, leaveFromDateObject, leaveToDateObject, fromDurationtypeId, toDurationtypeId, employeeId, employeeRecord, leaveTypeId);
				com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.rejectedLeaveUpdation(newStatus, oldStatus, record, employeeLeaveId, approverComment, currentDate);
				com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.cancelLeaveUpdation(newStatus, oldStatus, record, employeeLeaveId, approverComment, currentDate);
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				String message = ExceptionUtils.getExceptionTraceMessage("LeaveRequestValidation", e);
				LogUtility.writeLog("message is : >> " + message);
				throw new BusinessLogicException("Some unknown error ocuured while applying leave validation.");
			}
		}
	}

	private Object getHolidayCalendarId(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_employees");
		columnArray.put("holidaycalendarid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + employeeId);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_employees");
		if (rows != null && rows.length() > 0) {
			return rows.getJSONObject(0).opt("holidaycalendarid");
		} else {
			return null;
		}
	}

	private JSONArray getHolidayCalenderArray(Object holidayCalendarId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_holidaycalendars");
		columnArray.put("alternateholidaysmaxallowed");
		columnArray.put("fromdate");
		columnArray.put("todate");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + holidayCalendarId);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_holidaycalendars");
		return rows;
	}

	private JSONArray getAlternateHolidaysArray(Object holidayCalendarId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_alternate_holidays");
		columnArray.put("holiday");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_alternate_holidays");
		return rows;
	}

	private JSONArray getTakenAlternateHolidays(Object leaveKey, Object employeeId, Object limitStartDate, Object limitEndDate) throws JSONException {
		String extraFilter = "";
		if (leaveKey != null) {
			extraFilter = " and " + Updates.KEY + " != " + leaveKey;
		}
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_leaverequests");
		columnArray.put(Updates.KEY);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and alternateholiday = " + HRISApplicationConstants.EmployeeDecision.YES + " and fromdate >= '" + limitStartDate + "' and fromdate <= '" + limitEndDate + "' and leavestatusid IN (" + HRISConstants.LEAVE_NEW + ", " + HRISConstants.LEAVE_APPROVED + ")" + extraFilter);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_leaverequests");
		return rows;
	}

	public static JSONArray getEmployeeRecords(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("employeescheduleid");
		array.put("holidaycalendarid");
		array.put("balanceleaves");
		array.put("leavepolicyid.issandwich");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__  = " + employeeId);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_employees");
		return rows;
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		if (record.has("temp")) {
			return;
		}
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("insert")) {
			Object employeeLeaveId = record.getValue("__key__");
			Object directReportingToId = record.getValue("approverid");
			String currentUserEmailId = Translator.stringValue(record.getValue("employeeid.officialemailid"));
			Object currentUserId = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.getReportingToUserId(currentUserEmailId);
			String directReportingToEmailId = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.getEmployeeEmailId(directReportingToId);
			Object directReportingUserId = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.getReportingToUserId(directReportingToEmailId);
			com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.updateAllUsers(employeeLeaveId, currentUserId);
			com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.updateAllUsers(employeeLeaveId, directReportingUserId);
			JSONArray inDirectReportingToArray = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.getEmployeeInDirectReportingTo(employeeLeaveId);
			int inDirectReportingToArrayCount = inDirectReportingToArray == null ? 0 : inDirectReportingToArray.length();
			if (inDirectReportingToArrayCount > 0) {
				for (int counter = 0; counter < inDirectReportingToArrayCount; counter++) {
					Object inDirectReportingToId = null;
					try {
						inDirectReportingToId = inDirectReportingToArray.getJSONObject(counter).opt("indirectreportingtoid");
					} catch (Exception e) {
						throw new BusinessLogicException("Some unknown error occured while get indirect reportingto.");
					}
					if (inDirectReportingToId != null) {
						String inDirectReportingToEmailId = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.getEmployeeEmailId(inDirectReportingToId);
						Object indirectReportingUserId = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.getReportingToUserId(inDirectReportingToEmailId);
						com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.updateAllUsers(employeeLeaveId, indirectReportingUserId);
					}
				}
			}
		} else if (operation.equalsIgnoreCase("update")) {
			Object employeeLeaveId = record.getValue("__key__");
			JSONArray inDirectReportingToArray = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.getEmployeeInDirectReportingTo(employeeLeaveId);
			int inDirectReportingToArrayCount = inDirectReportingToArray == null ? 0 : inDirectReportingToArray.length();
			if (inDirectReportingToArrayCount > 0) {
				Object inDirectReportingToId = null;
				;
				Object leaveRequestId;
				for (int counter = 0; counter < inDirectReportingToArrayCount; counter++) {
					try {
						leaveRequestId = inDirectReportingToArray.getJSONObject(counter).opt("hris_leaverequests");
						inDirectReportingToId = inDirectReportingToArray.getJSONObject(counter).opt("indirectreportingtoid");
					} catch (Exception e) {
						throw new BusinessLogicException("Some unknown error occured while get indirect reportingto.");
					}
					if (inDirectReportingToId != null) {
						String inDirectReportingToEmailId = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.getEmployeeEmailId(inDirectReportingToId);
						Object indirectReportingUserId = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.getReportingToUserId(inDirectReportingToEmailId);
						boolean duplicateEntry = com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.findDuplicateUserEntry(leaveRequestId, indirectReportingUserId);
						if (!duplicateEntry) {
							com.applane.resourceoriented.hris.EmployeeLeaveBusinessLogic.updateAllUsers(employeeLeaveId, indirectReportingUserId);
						}
					}
				}
			}
		}
	}

	public void isDateRangeConflict(Object leaveRequestFromDate, Object leaveRequestToDate, Object employeeID) {
		try {
			JSONObject leaveQuery = new JSONObject();
			leaveQuery.put(Data.Query.RESOURCE, "hris_leaverequests");
			JSONArray leaveColumnArray = new JSONArray();
			leaveColumnArray.put("__key__");
			leaveColumnArray.put("fromdate");
			leaveColumnArray.put("todate");
			leaveQuery.put(Data.Query.COLUMNS, leaveColumnArray);
			leaveQuery.put(Data.Query.FILTERS, ("employeeid= '" + employeeID + "' and fromdate >= '" + leaveRequestFromDate + "' and  fromdate <= '" + leaveRequestToDate + "' and leavestatusid! = 4"));
			JSONArray groupby = new JSONArray();
			groupby.put("__key__");
			leaveQuery.put(Data.Query.GROUPS, groupby);
			JSONObject leaveOject;
			leaveOject = ResourceEngine.query(leaveQuery);
			JSONArray leaveRequestArray = leaveOject.getJSONArray("hris_leaverequests");
			int leaveRequestCount = (leaveRequestArray == null || leaveRequestArray.length() == 0) ? 0 : leaveRequestArray.length();
			if (leaveRequestCount > 0) {
				throw new BusinessLogicException("You have already requested for leave in this duration.");
			} else {
				leaveQuery = new JSONObject();
				leaveQuery.put(Data.Query.RESOURCE, "hris_leaverequests");
				leaveColumnArray = new JSONArray();
				leaveColumnArray.put("__key__");
				leaveColumnArray.put("fromdate");
				leaveColumnArray.put("todate");
				leaveQuery.put(Data.Query.COLUMNS, leaveColumnArray);
				leaveQuery.put(Data.Query.FILTERS, ("employeeid= '" + employeeID + "' and  fromdate <= '" + leaveRequestFromDate + "' and  todate >= '" + leaveRequestFromDate + "' and leavestatusid! = 4"));
				JSONArray groupbyArray = new JSONArray();
				groupbyArray.put("__key__");
				leaveQuery.put(Data.Query.GROUPS, groupbyArray);
				leaveOject = ResourceEngine.query(leaveQuery);
				leaveRequestArray = leaveOject.getJSONArray("hris_leaverequests");
				leaveRequestCount = (leaveRequestArray == null || leaveRequestArray.length() == 0) ? 0 : leaveRequestArray.length();
				if (leaveRequestCount > 0) {
					throw new BusinessLogicException("You have already requested for leave in this duration.");
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			throw new BusinessLogicException("Error come while check range conflicting" + e.getMessage());
		}
	}

	long differenceBetweenDates(Date leaveFromDate, Date leaveToDate) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(leaveToDate);
		cal2.setTime(leaveFromDate);
		long milis1 = cal1.getTimeInMillis();
		long milis2 = cal2.getTimeInMillis();
		long diff = milis1 - milis2;
		long diffDays = diff / (24 * 60 * 60 * 1000);
		return diffDays;
	}
}
