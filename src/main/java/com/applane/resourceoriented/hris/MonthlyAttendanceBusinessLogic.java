package com.applane.resourceoriented.hris;

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
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @resource employeemonthlyattendance
 */
public class MonthlyAttendanceBusinessLogic implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onAfterUpdate(Record record) {
		String operationType = record.getOperationType();
		if (operationType.equalsIgnoreCase(Updates.Types.UPDATE) || operationType.equalsIgnoreCase(Updates.Types.INSERT)) {
			try {
				Object temp = record.getValue("temp");
				Object employeeId = record.getValue("employeeid");
				Object joiningDateObject = record.getValue("employeeid.joiningdate");
				if (employeeId == null) {
					throw new BusinessLogicException("Employee Id found null while upating MonthlyAttendanceRecord having Id [" + record.getKey() + "] on " + new Date());
				}
				if (temp == null || temp.toString().equalsIgnoreCase(HRISApplicationConstants.TEMP_VALUE_RUN_JOBS) || temp.toString().equalsIgnoreCase(HRISApplicationConstants.TEMP_VALUE_RUN_JOBS_ON_LEAVE_APPROVAL)) {
					if (record.has("presentdays") || record.has("absents") || record.has("leaves") || record.has("extraworkingdays") || record.has("usedearnedleaves") || record.has("usedcasualleaves") || record.has("usedmedicalleaves") || record.has("usedspecialleaves") || (temp != null && temp.toString().equalsIgnoreCase("laps_leaves"))) {
						Object employeeMonthlyAttendanceId = record.getValue(Updates.KEY);
						Object monthId = record.getValue("monthid");
						Object yearId = record.getValue("yearid");
						Date joiningDate = DataTypeUtilities.checkDateFormat(joiningDateObject);
						boolean isJoining = EmployeeMonthlyAttendance.isNewJoineeOrReleaved(joiningDate, monthId, yearId);
						double totalWorkingDays = ((Number) (record.getValue("totalworkingdays") == null ? 0.0 : record.getValue("totalworkingdays"))).doubleValue();
						if (totalWorkingDays > 0.0) {
							String monthName = getMonthName(monthId);
							String yearName = getYearName(yearId);
							Date firstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
							Date lastDate = DataTypeUtilities.getMonthLastDate(firstDate);
							String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(firstDate);
							String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(lastDate);
							double paidStatusCount = getPaidUnPaidStatusCount(employeeId, monthFirstDateInString, monthLastDateInString, HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_PAYABLE);
							double unPaidStatusCount = getPaidUnPaidStatusCount(employeeId, monthFirstDateInString, monthLastDateInString, HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE);
							double comboOffEWDsCount = getComboOffEWDsCount(employeeId, monthFirstDateInString, monthLastDateInString, HRISApplicationConstants.LEAVE_EWD_COMBO_OFF);
							JSONArray employeeInfo = getEmployeeInfo(employeeId);
							Object leavePolicyId = getLeavePolicyId(employeeInfo);
							JSONArray leaveRulesInfo = getLeaveRuleRecords(leavePolicyId, monthFirstDateInString, monthLastDateInString);

							EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandanceUpdateAllColumns = instantiateMonthlyAttandanceObject(record, employeeInfo, leaveRulesInfo);
							employeeMonthlyAttandanceUpdateAllColumns.updateAttandanceInfo(employeeInfo, leaveRulesInfo, employeeMonthlyAttendanceId, record, paidStatusCount, unPaidStatusCount, comboOffEWDsCount, isJoining);

							if (temp == null) {
								updateManualUpdate(employeeMonthlyAttendanceId);
							}
						} else {
							LogUtility.writeLog("Total Working days must be greater than zero...");
						}
					}
					// }
				}
			} catch (Exception e) {
				String message = ExceptionUtils.getExceptionTraceMessage(MonthlyAttendanceBusinessLogic.class.getName(), e);
				LogUtility.writeError(message);
				ApplaneMail mail = new ApplaneMail();
				StringBuilder builder = new StringBuilder();
				builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
				builder.append("<br><br><b>Exception traces are given below :</b><br><br>").append(message);
				mail.setMessage("Error in Monthly Attendance business Logic: >>", builder.toString(), true);
				try {
					mail.setTo("kapil.dalal@daffodilsw.com");
					mail.sendAsAdmin();
				} catch (ApplaneMailException e1) {
				}
				e.printStackTrace();
				throw new BusinessLogicException("Some unknown error occured while calculate monthly attendance.");
			}
		}
	}

	private double getComboOffEWDsCount(Object employeeID, String firstDate, String lastDate, int paidStatus) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("attendancetypeid");
		query.put(Data.Query.COLUMNS, array);

		query.put(Data.Query.FILTERS, "employeeid = " + employeeID + " and attendancedate >= '" + firstDate + "' and attendancedate <= '" + lastDate + "' and paidstatus = " + paidStatus + " and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY + "," + HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF + ")");
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeeattendance");
		double totalEWDs = 0;
		for (int i = 0; i < rows.length(); i++) {
			int attendanceType = Translator.integerValue(rows.getJSONObject(i).opt("attendancetypeid"));
			if (attendanceType == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) {
				totalEWDs++;
			} else if (attendanceType == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
				totalEWDs += 0.5;
			}
		}

		return totalEWDs;
	}

	private double getPaidUnPaidStatusCount(Object employeeID, String firstDate, String lastDate, int paidStatus) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("attendancetypeid");
		query.put(Data.Query.COLUMNS, array);

		query.put(Data.Query.FILTERS, "employeeid = " + employeeID + " and attendancedate >= '" + firstDate + "' and attendancedate <= '" + lastDate + "' and paidstatus = " + paidStatus + " and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_LEAVE + "," + HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE + "," + HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE + "," + HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY + ")");
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeeattendance");
		double totalLeaves = 0;
		for (int i = 0; i < rows.length(); i++) {
			int attendanceType = Translator.integerValue(rows.getJSONObject(i).opt("attendancetypeid"));
			if (attendanceType == HRISApplicationConstants.ATTENDANCE_LEAVE) {
				totalLeaves++;
			} else if (attendanceType == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE) {
				totalLeaves += 0.5;
			} else if (attendanceType == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE) {
				totalLeaves += 0.5;
			}
		}

		return totalLeaves;
	}

	public static void updateManualUpdate(Object employeeMonthlyAttendanceId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("ismanualupdate", true);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private EmployeeMonthlyAttandanceUpdateAllColumns instantiateMonthlyAttandanceObject(Record monthlyAttandanceRecord, JSONArray employeeInfo, JSONArray leaveRulesInfo) {
		// Object employeeId = monthlyAttandanceRecord.getOldValue("employeeid");
		// Object employeeMonthlyAttendanceId = monthlyAttandanceRecord.getValue("__key__");
		// double monthlyOpeningBalanceLeaves = ((Number) (monthlyAttandanceRecord.getOldValue("monthlyopeningbalance") == null ? 0.0 : monthlyAttandanceRecord.getOldValue("monthlyopeningbalance"))).doubleValue();
		double earnedLeaveOpeningBalance = ((Number) (monthlyAttandanceRecord.getValue("earnedleaveopeningbalance") == null ? 0.0 : monthlyAttandanceRecord.getValue("earnedleaveopeningbalance"))).doubleValue();
		double casualLeaveOpeningBalance = ((Number) (monthlyAttandanceRecord.getValue("casualleaveopeningbalance") == null ? 0.0 : monthlyAttandanceRecord.getValue("casualleaveopeningbalance"))).doubleValue();
		double medicalLeaveOpeningBalance = ((Number) (monthlyAttandanceRecord.getValue("medicalleaveopeningbalance") == null ? 0.0 : monthlyAttandanceRecord.getValue("medicalleaveopeningbalance"))).doubleValue();
		double specialLeaveOpeningBalance = ((Number) (monthlyAttandanceRecord.getValue("specialleaveopeningbalance") == null ? 0.0 : monthlyAttandanceRecord.getValue("specialleaveopeningbalance"))).doubleValue();
		double extraWorkingDaysOpeningBalance = ((Number) (monthlyAttandanceRecord.getValue("extraworkingdayopeningbalance") == null ? 0.0 : monthlyAttandanceRecord.getValue("extraworkingdayopeningbalance"))).doubleValue();
		double presentDays = ((Number) (monthlyAttandanceRecord.getNewValue("presentdays") == null ? (monthlyAttandanceRecord.getValue("presentdays") == null ? 0.0 : monthlyAttandanceRecord.getValue("presentdays")) : monthlyAttandanceRecord.getNewValue("presentdays"))).doubleValue();
		double absents = ((Number) (monthlyAttandanceRecord.getNewValue("absents") == null ? (monthlyAttandanceRecord.getValue("absents") == null ? 0.0 : monthlyAttandanceRecord.getValue("absents")) : monthlyAttandanceRecord.getNewValue("absents"))).doubleValue();
		double totalTakenLeaves = ((Number) (monthlyAttandanceRecord.getNewValue("leaves") == null ? (monthlyAttandanceRecord.getValue("leaves") == null ? 0.0 : monthlyAttandanceRecord.getValue("leaves")) : monthlyAttandanceRecord.getNewValue("leaves"))).doubleValue();
		double usedEarnedLeave = ((Number) (monthlyAttandanceRecord.getNewValue("usedearnedleaves") == null ? (monthlyAttandanceRecord.getValue("usedearnedleaves") == null ? 0.0 : monthlyAttandanceRecord.getValue("usedearnedleaves")) : monthlyAttandanceRecord.getNewValue("usedearnedleaves"))).doubleValue();
		double usedCasualLeave = ((Number) (monthlyAttandanceRecord.getNewValue("usedcasualleaves") == null ? (monthlyAttandanceRecord.getValue("usedcasualleaves") == null ? 0.0 : monthlyAttandanceRecord.getValue("usedcasualleaves")) : monthlyAttandanceRecord.getNewValue("usedcasualleaves"))).doubleValue();
		double usedMedicalLeave = ((Number) (monthlyAttandanceRecord.getNewValue("usedmedicalleaves") == null ? (monthlyAttandanceRecord.getValue("usedmedicalleaves") == null ? 0.0 : monthlyAttandanceRecord.getValue("usedmedicalleaves")) : monthlyAttandanceRecord.getNewValue("usedmedicalleaves"))).doubleValue();
		double usedSpecialLeave = ((Number) (monthlyAttandanceRecord.getNewValue("usedspecialleaves") == null ? (monthlyAttandanceRecord.getValue("usedspecialleaves") == null ? 0.0 : monthlyAttandanceRecord.getValue("usedspecialleaves")) : monthlyAttandanceRecord.getNewValue("usedspecialleaves"))).doubleValue();
		double extraWorkingDays = ((Number) (monthlyAttandanceRecord.getNewValue("extraworkingdays") == null ? (monthlyAttandanceRecord.getValue("extraworkingdays") == null ? 0.0 : monthlyAttandanceRecord.getValue("extraworkingdays")) : monthlyAttandanceRecord.getNewValue("extraworkingdays"))).doubleValue();
		double totalWorkingDays = ((Number) (monthlyAttandanceRecord.getNewValue("totalworkingdays") == null ? (monthlyAttandanceRecord.getValue("totalworkingdays") == null ? 0.0 : monthlyAttandanceRecord.getValue("totalworkingdays")) : monthlyAttandanceRecord.getNewValue("totalworkingdays"))).doubleValue();
		double creditEarnedLeaves = ((Number) (monthlyAttandanceRecord.getNewValue("creditearnedleaves") == null ? (monthlyAttandanceRecord.getValue("creditearnedleaves") == null ? 0.0 : monthlyAttandanceRecord.getValue("creditearnedleaves")) : monthlyAttandanceRecord.getNewValue("creditearnedleaves"))).doubleValue();
		double creditCasualLeave = ((Number) (monthlyAttandanceRecord.getNewValue("creditcasualleaves") == null ? (monthlyAttandanceRecord.getValue("creditcasualleaves") == null ? 0.0 : monthlyAttandanceRecord.getValue("creditcasualleaves")) : monthlyAttandanceRecord.getNewValue("creditcasualleaves"))).doubleValue();
		double creditMedicalLeave = ((Number) (monthlyAttandanceRecord.getNewValue("creditmedicalleaves") == null ? (monthlyAttandanceRecord.getValue("creditmedicalleaves") == null ? 0.0 : monthlyAttandanceRecord.getValue("creditmedicalleaves")) : monthlyAttandanceRecord.getNewValue("creditmedicalleaves"))).doubleValue();
		EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = new EmployeeMonthlyAttandanceUpdateAllColumns();
		employeeMonthlyAttandance.setCasualLeaveMonthlyOpeningBalance(casualLeaveOpeningBalance);
		employeeMonthlyAttandance.setEarnedLeaveMonthlyOpeningBalance(earnedLeaveOpeningBalance);
		employeeMonthlyAttandance.setMedicalLeaveMonthlyOpeningBalance(medicalLeaveOpeningBalance);
		employeeMonthlyAttandance.setSpecialLeaveMonthlyOpeningBalance(specialLeaveOpeningBalance);
		employeeMonthlyAttandance.setExtraWorkingDaysMonthlyOpeningBalance(extraWorkingDaysOpeningBalance);
		employeeMonthlyAttandance.setTakenEarnedLeavedInThisMonth(usedEarnedLeave);
		employeeMonthlyAttandance.setTakenCasualLeavedInThisMonth(usedCasualLeave);
		employeeMonthlyAttandance.setTakenMedicalLeavedInThisMonth(usedMedicalLeave);
		employeeMonthlyAttandance.setTakenSpecialLeavedInThisMonth(usedSpecialLeave);
		// employeeMonthlyAttandance.setExtraWorkingdayOpeningBalance(extraWorkingdayOpeningBalance);
		// employeeMonthlyAttandance.setNonPaybleLeave(nonPaybleLeave);
		// employeeMonthlyAttandance.setPaybleAbsent(paybleAbsent);
		// employeeMonthlyAttandance.setPaybleLeave(paybleLeave);
		employeeMonthlyAttandance.setTotalAbsentInMonth(absents);
		employeeMonthlyAttandance.setTotalCreditCasualLeavesInMonth(creditCasualLeave);
		employeeMonthlyAttandance.setTotalCreditEarnedLeaveInMonth(creditEarnedLeaves);
		employeeMonthlyAttandance.setTotalCreditMedicalLeaveInMonth(creditMedicalLeave);
		employeeMonthlyAttandance.setTotalExtraWorkingDaysInMonth(extraWorkingDays);
		employeeMonthlyAttandance.setTotalLeavesInMonth(totalTakenLeaves);
		employeeMonthlyAttandance.setTotalPresentsInMonth(presentDays);
		employeeMonthlyAttandance.setTotalWorkingDaysInMonth(totalWorkingDays);
		return employeeMonthlyAttandance;
	}

	/**
	 * This method will return Leave policy information of passed employee
	 * 
	 * @param employeeInfo
	 * @return Object leavePolicyId
	 */
	private Object getLeavePolicyId(JSONArray employeeInfo) {
		if (employeeInfo == null) {
			throw new BusinessLogicException("Employee information is passed null to fetch its leave policy infomrmation");
		}
		try {
			Object leavePolicyId = employeeInfo.getJSONObject(0).opt("leavepolicyid");
			if (leavePolicyId == null) {
				throw new BusinessLogicException("No Leave policy found for: " + employeeInfo.getJSONObject(0).opt("officialemailid"));
			}
			return leavePolicyId;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private JSONArray getEmployeeInfo(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("leavepolicyid");
		array.put("leavepolicyid.isextraworkingdayencashable");
		array.put("leavepolicyid.isextraworkingdayadjustagainstabsents");
		array.put("leavepolicyid.is_absent_adjust_with_leave");
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + employeeId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_employees");
		return rows;
	}

	private JSONArray getLeaveRuleRecords(Object leavePolicyId, String monthFirstDateInString, String monthLastDateInString) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverule");
		JSONArray array = new JSONArray();
		array.put("leavetypeid");
		array.put("noofleaves");
		array.put("payable");
		array.put("leavecreditid");
		array.put("isextraworkingdaysadjustagainstleave");
		array.put("accruetypeid");
		array.put("accrueaftermonths");
		array.put("maxleaveperapplication");
		array.put("not_avail_in_current_month");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "leavepolicyid = " + leavePolicyId + " and fromdate <= '" + monthFirstDateInString + "' and todate >= '" + monthLastDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, "leavepolicyid = " + leavePolicyId + " and fromdate <= '" + monthFirstDateInString + "' and todate = null");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_leaverule");
		return rows;
	}

	// public static void updateMonthlyClosingBalance(Object employeeMonthlyAttendanceId, double monthlyClosingBalance) throws JSONException {
	// JSONObject updates = new JSONObject();
	// updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
	// JSONObject row = new JSONObject();
	// row.put("__key__", employeeMonthlyAttendanceId);
	// row.put("monthlyclosingbalance", monthlyClosingBalance);
	// updates.put(Data.Update.UPDATES, row);
	// ResourceEngine.update(updates);
	// }
	//
	// public static void updateMonthlyPayableAbsents(Object employeeMonthlyAttendanceId, double paidAbsents) throws JSONException {
	// JSONObject updates = new JSONObject();
	// updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
	// JSONObject row = new JSONObject();
	// row.put("__key__", employeeMonthlyAttendanceId);
	// row.put("paidabsents", paidAbsents);
	// updates.put(Data.Update.UPDATES, row);
	// ResourceEngine.update(updates);
	// }

	/*
	 * private void updateLeaveAndAbsentsDays(Object employeeMonthlyAttendanceId, Double leaves, Double absents) throws JSONException { LogUtility.writeLog("updateLeaveAndAbsentsDays Method call..."); JSONObject updates = new JSONObject(); updates.put(Data.Query.RESOURCE, "employeemonthlyattendance"); JSONObject row = new JSONObject(); row.put("__key__", employeeMonthlyAttendanceId); row.put("leaves", leaves); row.put("absents", absents); row.put("temp", HRISApplicationConstants.TEMP_VALUE); updates.put(Data.Update.UPDATES, row); ResourceEngine.update(updates); LogUtility.writeLog("updateLeaveAndAbsentsDays Suceessfully...."); }
	 */
	public void onBeforeUpdate(Record record) {
		String operationType = record.getOperationType();
		if (operationType.equalsIgnoreCase(Updates.Types.UPDATE) && record.getValue("temp") == null) {
			try {
				double presentDays = ((Number) (record.getValue("presentdays") == null ? 0.0 : record.getValue("presentdays"))).doubleValue();
				double absents = ((Number) (record.getValue("absents") == null ? 0.0 : record.getValue("absents"))).doubleValue();
				double leaves = ((Number) (record.getValue("leaves") == null ? 0.0 : record.getValue("leaves"))).doubleValue();
				double nonWorkingDays = ((Number) (record.getValue("actualnonworkingdays") == null ? 0.0 : record.getValue("actualnonworkingdays"))).doubleValue();
				double extraWorkingDays = ((Number) (record.getValue("extraworkingdays") == null ? 0.0 : record.getValue("extraworkingdays"))).doubleValue();
				double totalWorkingDays = ((Number) (record.getValue("totalworkingdays") == null ? 0.0 : record.getValue("totalworkingdays"))).doubleValue();
				double usedEl = ((Number) (record.getValue("usedearnedleaves") == null ? 0.0 : record.getValue("usedearnedleaves"))).doubleValue();
				double usedCl = ((Number) (record.getValue("usedcasualleaves") == null ? 0.0 : record.getValue("usedcasualleaves"))).doubleValue();
				double usedMl = ((Number) (record.getValue("usedmedicalleaves") == null ? 0.0 : record.getValue("usedmedicalleaves"))).doubleValue();
				double usedSl = ((Number) (record.getValue("usedspecialleaves") == null ? 0.0 : record.getValue("usedspecialleaves"))).doubleValue();
				double totalCalendarDays = totalWorkingDays + nonWorkingDays;
				double workingDays = presentDays + absents + leaves + nonWorkingDays;
				double totalUsedLeaves = usedEl + usedCl + usedMl + usedSl;
				if (totalUsedLeaves != leaves) {
					throw new BusinessLogicException("Total of Used EL, Used CL, Used ML and Used SL must be equal to " + leaves + " days.");
				}
				if (record.has("presentdays") || record.has("absents") || record.has("leaves") || record.has("extraworkingdays")) {
					if (workingDays != totalCalendarDays) {
						throw new BusinessLogicException("Total of Present Days, Absents, Leaves and Non Working Days must be equal to " + totalCalendarDays + " days.");
					}
					if (extraWorkingDays > nonWorkingDays) {
						throw new BusinessLogicException("Extra Working Days must be less OR equal to non working days(" + nonWorkingDays + ").");
					}
				}
			} catch (DeadlineExceededException e) {
				LogUtility.writeLog("First exception block...");
				throw e;
			} catch (BusinessLogicException e) {
				LogUtility.writeLog("Second exception block");
				throw e;
			} catch (Exception e) {
				LogUtility.writeLog("Third Exception block...");
				e.printStackTrace();
				throw new BusinessLogicException("Some unknown error occured while calculate monthly attendance.");
			}
		}
	}

	/*
	 * private JSONArray getMonthlyAttendanceRecords(Object employeeId, Integer monthId, Object yearId) throws JSONException { JSONObject query = new JSONObject(); query.put(Data.Query.RESOURCE, "employeemonthlyattendance"); JSONArray array = new JSONArray(); array.put("presentdays"); array.put("absents"); array.put("leaves"); array.put("extraworkingdays"); array.put("creditleaves"); array.put("totalworkingdays"); query.put(Data.Query.COLUMNS, array); query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId); JSONObject object; object = ResourceEngine.query(query); JSONArray rows = object.getJSONArray("employeemonthlyattendance"); return rows; }
	 */

	private String getYearName(Object yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_years");
		JSONArray array = new JSONArray();
		array.put("name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + yearId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("organization_years");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			String yearName = rows.getJSONObject(0).optString("name");
			return yearName;
		}
		return null;
	}

	private String getMonthName(Object monthId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_months");
		JSONArray array = new JSONArray();
		array.put("name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + monthId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("organization_months");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			String monthName = rows.getJSONObject(0).optString("name");
			return monthName;
		}
		return null;
	}

	public static void updateCarryExtraWorkingDays(Object monthlyAttendanceId, double extraWorkingDays) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", monthlyAttendanceId);
		row.put("carryextraworkingday", extraWorkingDays);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static void updateNonPayableLeaves(Object monthlyAttendanceId, double nonPayableLeaves) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", monthlyAttendanceId);
		row.put("nonpayableleaves", nonPayableLeaves);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static void updateCarryLeave(Object monthlyAttendanceId, double carryLeaves) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", monthlyAttendanceId);
		row.put("carryleaves", carryLeaves);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static void updateCarryLeaveAndCarryExtraWorkingDay(Object monthlyAttendanceId, double carryLeaves, double carryExtraWorkingDay, double monthlyClosingBalance) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", monthlyAttendanceId);
		row.put("carryleaves", carryLeaves);
		row.put("carryextraworkingday", carryExtraWorkingDay);
		row.put("monthlyclosingbalance", monthlyClosingBalance);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static void updateAccuredLeave(Object monthlyAttendanceId, double accuredLeaves, Integer leaveTypeId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", monthlyAttendanceId);

		if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE) {
			row.put("earnearnedleave", accuredLeaves);
		} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE) {
			row.put("earncasualleave", accuredLeaves);
		} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE) {
			row.put("earnmedicalleave", accuredLeaves);
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static void updateEmployeeTotalBalanceLeave(Object employeeId, double totalBalanceLeave) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeId);
		row.put("balanceleaves", totalBalanceLeave);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}
}
