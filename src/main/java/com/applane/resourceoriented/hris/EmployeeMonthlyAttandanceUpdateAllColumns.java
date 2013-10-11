package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;
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
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.moduleimpl.SystemParameters;

public class EmployeeMonthlyAttandanceUpdateAllColumns {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	private Object employeeId;

	private String employeeEmailId;

	private double totalPresentsInMonth;

	private double totalAbsentInMonth;

	private double totalLeavesInMonth;

	private double totalExtraWorkingDaysInMonth;

	private double actualExtraWorkingDaysInMonth;

	private double totalWorkingDaysInMonth;

	private double totalCreditCasualLeavesInMonth;

	private double totalCreditEarnedLeaveInMonth;

	private double totalCreditMedicalLeaveInMonth;

	private double casualLeaveMonthlyOpeningBalance;

	private double medicalLeaveMonthlyOpeningBalance;

	private double earnedLeaveMonthlyOpeningBalance;

	private double specialLeaveMonthlyOpeningBalance;

	private double casualLeaveMonthlyClosingBalance;

	private double medicalLeaveMonthlyClosingBalance;

	private double earnedLeaveMonthlyClosingBalance;

	private double specialLeaveMonthlyClosingBalance;

	private double extraWorkingDaysMonthlyOpeningBalance;

	private double paybleLeave;

	private double nonPaybleLeave;

	private double paybleAbsent;

	private double nonPaybleAbsent;

	private double takenEarnedLeavedInThisMonth;

	private double takenCasualLeavedInThisMonth;

	private double takenMedicalLeavedInThisMonth;

	private double takenSpecialLeavedInThisMonth;

	private double totalExtraWorkingDayNetBalance;

	private double actualNonWorkingDaysInMonth;

	private boolean isExtraWorkingDayEncashable;

	private boolean isAbsentAdjustWithLeaves;

	public EmployeeMonthlyAttandanceUpdateAllColumns() {
	}

	public EmployeeMonthlyAttandanceUpdateAllColumns(Object employeeId, String employeeEmailId, double totalPresentsInMonth, double totalAbsentInMonth, double totalLeavesInMonth, double totalExtraWorkingDaysInMonth, double totalWorkingDaysInMonth, double totalCreditCasualLeavesInMonth, double totalCreditEarnedLeaveInMonth, double totalCreditMedicalLeaveInMonth, double casualLeaveMonthlyOpeningBalance, double medicalLeaveMonthlyOpeningBalance, double earnedLeaveMonthlyOpeningBalance, double specialLeaveMonthlyOpeningBalance, double extraWorkingDaysMonthlyOpeningBalance, double paybleLeave, double nonPaybleLeave, double paybleAbsent, double nonPaybleAbsent, double takenEarnedLeavedInThisMonth, double takenCasualLeavedInThisMonth, double takenMedicalLeavedInThisMonth,
			double takenSpecialLeavedInThisMonth, double totalExtraWorkingDayNetBalance) {
		super();
		this.employeeId = employeeId;
		this.employeeEmailId = employeeEmailId;
		this.totalPresentsInMonth = totalPresentsInMonth;
		this.totalAbsentInMonth = totalAbsentInMonth;
		this.totalLeavesInMonth = totalLeavesInMonth;
		this.totalExtraWorkingDaysInMonth = totalExtraWorkingDaysInMonth;
		this.totalWorkingDaysInMonth = totalWorkingDaysInMonth;
		this.totalCreditCasualLeavesInMonth = totalCreditCasualLeavesInMonth;
		this.totalCreditEarnedLeaveInMonth = totalCreditEarnedLeaveInMonth;
		this.totalCreditMedicalLeaveInMonth = totalCreditMedicalLeaveInMonth;
		this.casualLeaveMonthlyOpeningBalance = casualLeaveMonthlyOpeningBalance;
		this.medicalLeaveMonthlyOpeningBalance = medicalLeaveMonthlyOpeningBalance;
		this.earnedLeaveMonthlyOpeningBalance = earnedLeaveMonthlyOpeningBalance;
		this.specialLeaveMonthlyOpeningBalance = specialLeaveMonthlyOpeningBalance;
		this.extraWorkingDaysMonthlyOpeningBalance = extraWorkingDaysMonthlyOpeningBalance;
		this.paybleLeave = paybleLeave;
		this.nonPaybleLeave = nonPaybleLeave;
		this.paybleAbsent = paybleAbsent;
		this.nonPaybleAbsent = nonPaybleAbsent;
		this.takenEarnedLeavedInThisMonth = takenEarnedLeavedInThisMonth;
		this.takenCasualLeavedInThisMonth = takenCasualLeavedInThisMonth;
		this.takenMedicalLeavedInThisMonth = takenMedicalLeavedInThisMonth;
		this.takenSpecialLeavedInThisMonth = takenSpecialLeavedInThisMonth;
		this.totalExtraWorkingDayNetBalance = totalExtraWorkingDayNetBalance;
	}

	public boolean getIsAbsentAdjustWithLeaves() {
		return isAbsentAdjustWithLeaves;
	}

	public void setIsAbsentAdjustWithLeaves(boolean isAbsentAdjustWithLeaves) {
		this.isAbsentAdjustWithLeaves = isAbsentAdjustWithLeaves;
	}

	public double getActualExtraWorkingDaysInMonth() {
		return actualExtraWorkingDaysInMonth;
	}

	public void setActualExtraWorkingDaysInMonth(double actualExtraWorkingDaysInMonth) {
		this.actualExtraWorkingDaysInMonth = actualExtraWorkingDaysInMonth;
	}

	public double getActualNonWorkingDaysInMonth() {
		return actualNonWorkingDaysInMonth;
	}

	public void setActualNonWorkingDaysInMonth(double actualNonWorkingDaysInMonth) {
		this.actualNonWorkingDaysInMonth = actualNonWorkingDaysInMonth;
	}

	public Object getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(Object employeeId) {
		this.employeeId = employeeId;
	}

	public String getEmployeeEmailId() {
		return employeeEmailId;
	}

	public void setEmployeeEmailId(String employeeEmailId) {
		this.employeeEmailId = employeeEmailId;
	}

	public double getTotalPresentsInMonth() {
		return totalPresentsInMonth;
	}

	public void setTotalPresentsInMonth(double totalPresentsInMonth) {
		this.totalPresentsInMonth = totalPresentsInMonth;
	}

	public double getTotalAbsentInMonth() {
		return totalAbsentInMonth;
	}

	public void setTotalAbsentInMonth(double totalAbsentInMonth) {
		this.totalAbsentInMonth = totalAbsentInMonth;
	}

	public double getTotalLeavesInMonth() {
		return totalLeavesInMonth;
	}

	public void setTotalLeavesInMonth(double totalLeavesInMonth) {
		this.totalLeavesInMonth = totalLeavesInMonth;
	}

	public double getTotalExtraWorkingDaysInMonth() {
		return totalExtraWorkingDaysInMonth;
	}

	public void setTotalExtraWorkingDaysInMonth(double totalExtraWorkingDaysInMonth) {
		this.totalExtraWorkingDaysInMonth = totalExtraWorkingDaysInMonth;
	}

	public double getTotalWorkingDaysInMonth() {
		return totalWorkingDaysInMonth;
	}

	public void setTotalWorkingDaysInMonth(double totalWorkingDaysInMonth) {
		this.totalWorkingDaysInMonth = totalWorkingDaysInMonth;
	}

	public double getTotalCreditCasualLeavesInMonth() {
		return totalCreditCasualLeavesInMonth;
	}

	public void setTotalCreditCasualLeavesInMonth(double totalCreditCasualLeavesInMonth) {
		this.totalCreditCasualLeavesInMonth = totalCreditCasualLeavesInMonth;
	}

	public double getTotalCreditEarnedLeaveInMonth() {
		return totalCreditEarnedLeaveInMonth;
	}

	public void setTotalCreditEarnedLeaveInMonth(double totalCreditEarnedLeaveInMonth) {
		this.totalCreditEarnedLeaveInMonth = totalCreditEarnedLeaveInMonth;
	}

	public double getTotalCreditMedicalLeaveInMonth() {
		return totalCreditMedicalLeaveInMonth;
	}

	public void setTotalCreditMedicalLeaveInMonth(double totalCreditMedicalLeaveInMonth) {
		this.totalCreditMedicalLeaveInMonth = totalCreditMedicalLeaveInMonth;
	}

	public double getCasualLeaveMonthlyOpeningBalance() {
		return casualLeaveMonthlyOpeningBalance;
	}

	public void setCasualLeaveMonthlyOpeningBalance(double casualLeaveMonthlyOpeningBalance) {
		this.casualLeaveMonthlyOpeningBalance = casualLeaveMonthlyOpeningBalance;
	}

	public double getMedicalLeaveMonthlyOpeningBalance() {
		return medicalLeaveMonthlyOpeningBalance;
	}

	public void setMedicalLeaveMonthlyOpeningBalance(double medicalLeaveMonthlyOpeningBalance) {
		this.medicalLeaveMonthlyOpeningBalance = medicalLeaveMonthlyOpeningBalance;
	}

	public double getEarnedLeaveMonthlyOpeningBalance() {
		return earnedLeaveMonthlyOpeningBalance;
	}

	public void setEarnedLeaveMonthlyOpeningBalance(double earnedLeaveMonthlyOpeningBalance) {
		this.earnedLeaveMonthlyOpeningBalance = earnedLeaveMonthlyOpeningBalance;
	}

	public double getExtraWorkingDaysMonthlyOpeningBalance() {
		return extraWorkingDaysMonthlyOpeningBalance;
	}

	public void setExtraWorkingDaysMonthlyOpeningBalance(double extraWorkingDaysMonthlyOpeningBalance) {
		this.extraWorkingDaysMonthlyOpeningBalance = extraWorkingDaysMonthlyOpeningBalance;
	}

	public double getSpecialLeaveMonthlyOpeningBalance() {
		return specialLeaveMonthlyOpeningBalance;
	}

	public void setSpecialLeaveMonthlyOpeningBalance(double specialLeaveMonthlyOpeningBalance) {
		this.specialLeaveMonthlyOpeningBalance = specialLeaveMonthlyOpeningBalance;
	}

	public double getPaybleLeave() {
		return paybleLeave;
	}

	public void setPaybleLeave(double paybleLeave) {
		this.paybleLeave = paybleLeave;
	}

	public double getNonPaybleLeave() {
		return nonPaybleLeave;
	}

	public void setNonPaybleLeave(double nonPaybleLeave) {
		this.nonPaybleLeave = nonPaybleLeave;
	}

	public double getPaybleAbsent() {
		return paybleAbsent;
	}

	public void setPaybleAbsent(double paybleAbsent) {
		this.paybleAbsent = paybleAbsent;
	}

	public double getNonPaybleAbsent() {
		return nonPaybleAbsent;
	}

	public void setNonPaybleAbsent(double nonPaybleAbsent) {
		this.nonPaybleAbsent = nonPaybleAbsent;
	}

	public double getTakenEarnedLeavedInThisMonth() {
		return takenEarnedLeavedInThisMonth;
	}

	public void setTakenEarnedLeavedInThisMonth(double takenEarnedLeavedInThisMonth) {
		this.takenEarnedLeavedInThisMonth = takenEarnedLeavedInThisMonth;
	}

	public double getTakenCasualLeavedInThisMonth() {
		return takenCasualLeavedInThisMonth;
	}

	public void setTakenCasualLeavedInThisMonth(double takenCasualLeavedInThisMonth) {
		this.takenCasualLeavedInThisMonth = takenCasualLeavedInThisMonth;
	}

	public double getTakenMedicalLeavedInThisMonth() {
		return takenMedicalLeavedInThisMonth;
	}

	public void setTakenMedicalLeavedInThisMonth(double takenMedicalLeavedInThisMonth) {
		this.takenMedicalLeavedInThisMonth = takenMedicalLeavedInThisMonth;
	}

	public double getTakenSpecialLeavedInThisMonth() {
		return takenSpecialLeavedInThisMonth;
	}

	public void setTakenSpecialLeavedInThisMonth(double takenSpecialLeavedInThisMonth) {
		this.takenSpecialLeavedInThisMonth = takenSpecialLeavedInThisMonth;
	}

	public double getTotalExtraWorkingDayNetBalance() {
		return totalExtraWorkingDayNetBalance;
	}

	public void setTotalExtraWorkingDayNetBalanc(double totalExtraWorkingDayNetBalance) {
		this.totalExtraWorkingDayNetBalance = totalExtraWorkingDayNetBalance;
	}

	public boolean getIsExtraWorkingDayEncashable() {
		return isExtraWorkingDayEncashable;
	}

	public void setIsExtraWorkingDayEncashable(boolean isExtraWorkingDayEncashable) {
		this.isExtraWorkingDayEncashable = isExtraWorkingDayEncashable;
	}

	public void updateAttandanceInfo(JSONArray employeeInfo, JSONArray leaveRulesInfo, Object employeeMonthlyAttendanceId, Record record, double paidStatusCount, double unPaidStatusCount, double comboOffEWDsCount, boolean isJoining) throws Exception {
		// this method will iterate on leaverules for type
		int leaveRuleArrayCount = (leaveRulesInfo == null || leaveRulesInfo.length() == 0) ? 0 : leaveRulesInfo.length();
		if (leaveRuleArrayCount > 0) {
			double nonPayableAbsents = 0.0;
			Object employeeId = employeeInfo.getJSONObject(0).opt("__key__");
			int employeeArrayCount = (employeeInfo == null || employeeInfo.length() == 0) ? 0 : employeeInfo.length();
			double totalExtraInMonthOpening = extraWorkingDaysMonthlyOpeningBalance;
			paybleLeave = 0.0;
			totalExtraWorkingDayNetBalance = 0.0;
			totalExtraWorkingDaysInMonth += extraWorkingDaysMonthlyOpeningBalance;
			extraWorkingDaysMonthlyOpeningBalance = 0.0;
			isAbsentAdjustWithLeaves = false;
			// totalExtraWorkingDaysInMonth -= comboOffEWDsCount;
			if (employeeArrayCount > 0) {

				Object isExtraWorkingDaysAdjustAgainstAbsentsObject = employeeInfo.getJSONObject(0).opt("leavepolicyid.isextraworkingdayadjustagainstabsents");
				isAbsentAdjustWithLeaves = Translator.booleanValue(employeeInfo.getJSONObject(0).opt("leavepolicyid.is_absent_adjust_with_leave"));
				// setIsAbsentAdjustWithLeaves(Translator.booleanValue(employeeInfo.getJSONObject(0).opt("leavepolicyid.is_absent_adjust_with_leave")));

				boolean isExtraWorkingDaysAdjustAgainstAbsents = DataTypeUtilities.booleanValue(isExtraWorkingDaysAdjustAgainstAbsentsObject);
				LogUtility.writeError("1 isExtraWorkingDaysAdjustAgainstAbsents >> " + isExtraWorkingDaysAdjustAgainstAbsents + " << totalAbsentInMonth >> " + totalAbsentInMonth + " << totalExtraWorkingDaysInMonth >> " + totalExtraWorkingDaysInMonth + " << nonPaybleAbsent >> " + nonPaybleAbsent + " << payableAbsent >> " + paybleAbsent);
				if (isExtraWorkingDaysAdjustAgainstAbsents) {
					if (totalAbsentInMonth > 0.0 && totalExtraWorkingDaysInMonth > 0.0) {
						double tempEWD = totalExtraWorkingDaysInMonth - totalAbsentInMonth;
						if (tempEWD < 0) {
							paybleAbsent = totalExtraWorkingDaysInMonth;
							// updateMonthlyPayableAbsents(employeeMonthlyAttendanceId, totalExtraWorkingDaysInMonth);
							totalExtraWorkingDaysInMonth = 0.0;
							// update extraWorkingDays as paid absents in EMA
							nonPayableAbsents = tempEWD * (-1.0);

							nonPaybleAbsent = nonPayableAbsents;
							totalAbsentInMonth = nonPaybleAbsent;
							// updateNonPayableAbsents(employeeMonthlyAttendanceId, nonPayableAbsents);
						} else {
							// update absents as paid absents in EMA.
							nonPayableAbsents = 0d;
							paybleAbsent = totalAbsentInMonth;
							nonPaybleAbsent = nonPayableAbsents;
							totalAbsentInMonth = 0.0;
							// updateNonPayableAbsents(employeeMonthlyAttendanceId, nonPayableAbsents);
							// updateMonthlyPayableAbsents(employeeMonthlyAttendanceId, totalAbsentInMonth);
							totalExtraWorkingDaysInMonth = tempEWD;
						}
					} else if (totalAbsentInMonth > 0.0 && (totalExtraWorkingDaysInMonth == 0.0) && !isAbsentAdjustWithLeaves) {
						// update 0.0 as paidabsents in EMA.
						nonPayableAbsents = totalAbsentInMonth;
						// update non Adjusted Absents as a non payable absents
						paybleAbsent = 0.0;
						nonPaybleAbsent = nonPayableAbsents;
						// updateNonPayableAbsents(employeeMonthlyAttendanceId, nonPayableAbsents);
						// updateMonthlyPayableAbsents(employeeMonthlyAttendanceId, 0.0);
					} else if (totalAbsentInMonth == 0.0) {
						paybleAbsent = 0.0;
						nonPaybleAbsent = 0.0;
						// updateNonPayableAbsents(employeeMonthlyAttendanceId, 0.0);
						// updateMonthlyPayableAbsents(employeeMonthlyAttendanceId, 0.0);
					}
				} else if (!isAbsentAdjustWithLeaves) {
					paybleAbsent = 0.0;
					nonPaybleAbsent = totalAbsentInMonth;
				}
			}

			LogUtility.writeError("2 totalAbsentInMonth >> " + totalAbsentInMonth + " << totalExtraWorkingDaysInMonth >> " + totalExtraWorkingDaysInMonth + " << nonPaybleAbsent >> " + nonPaybleAbsent + " << payableAbsent >> " + paybleAbsent);
			// double totalExtraInMonth = totalExtraWorkingDaysInMonth;

			double[] totalBalanceLeaves = { 0d };
			double[] ewdAdded = { 0.0 };
			HashMap<Integer, Double> leaveTypeOpeningBalance = new HashMap<Integer, Double>();
			for (int counter = 0; counter < leaveRuleArrayCount; counter++) {
				JSONObject leaveRule = leaveRulesInfo.getJSONObject(counter);
				updateLeaveInfo(employeeInfo, leaveRule, employeeMonthlyAttendanceId, employeeId, paidStatusCount, unPaidStatusCount, record, totalBalanceLeaves, comboOffEWDsCount, ewdAdded, leaveTypeOpeningBalance, isJoining);
			}
			// double balanceLeaves = earnedLeaveMonthlyClosingBalance + casualLeaveMonthlyClosingBalance + medicalLeaveMonthlyClosingBalance + specialLeaveMonthlyClosingBalance;
			boolean isExtraWorkingDayEncashable = DataTypeUtilities.booleanValue(employeeInfo.getJSONObject(0).opt("leavepolicyid.isextraworkingdayencashable"));
			// if (!isExtraWorkingDayEncashable) {
			// balanceLeaves += totalExtraWorkingDaysInMonth;
			// updateEmployeeExtraWorkingDayCount(employeeId, (totalExtraWorkingDaysInMonth + comboOffEWDsCount));
			// }
			if (!isExtraWorkingDayEncashable) {
				totalExtraWorkingDayNetBalance = 0.0;
			} else if (totalExtraWorkingDaysInMonth < totalExtraInMonthOpening) {
				totalExtraWorkingDayNetBalance = 0.0;
			} else {
				// + totalExtraWorkingDaysInMonth +
				// " << totalExtraInMonthOpening >> " + totalExtraInMonthOpening
				// + " << totalExtraWorkingDayNetBalance > > " +
				// totalExtraWorkingDayNetBalance);
				totalExtraWorkingDayNetBalance = totalExtraWorkingDaysInMonth - totalExtraInMonthOpening;
			}
			updateEmployeeLeaveBalance(employeeId, totalBalanceLeaves[0]);
			updateNonPayableAndPayableLeaves(employeeMonthlyAttendanceId, isExtraWorkingDayEncashable);
			if (leaveTypeOpeningBalance != null && leaveTypeOpeningBalance.containsKey(0)) {
				int monthId = (int) Translator.doubleValue(leaveTypeOpeningBalance.get(5));
				int yearId = (int) Translator.doubleValue(leaveTypeOpeningBalance.get(6));
				Object monthlyAttendanceId = getEmployeeMonthlyAttendanceId(employeeId, monthId, yearId);
				if (monthlyAttendanceId != null) {
					String currentMonth = EmployeeSalaryGenerationServlet.getMonthName(monthId);
					String currentYear = EmployeeSalaryGenerationServlet.getYearName(yearId);
					Date attendanceDate = DataTypeUtilities.getMonthFirstDate(currentYear, currentMonth);
					// String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
					// Date attendanceDate = Translator.dateValue(TODAY_DATE);
					boolean isFromEmployeeUpdate = false;
					boolean isFromMonthlyUpdateAllColumns = true;
					new EmployeeMonthlyAttendance().updateNewMonthlyAttendanceRecord(monthlyAttendanceId, employeeId, monthId, yearId, currentYear, currentMonth, new Date(), isFromEmployeeUpdate, attendanceDate, isFromMonthlyUpdateAllColumns, leaveTypeOpeningBalance);
				}
			}
		}
	}

	private Object getEmployeeMonthlyAttendanceId(Object employeeId, int monthId, int yearId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeemonthlyattendance");
		if (rows != null && rows.length() > 0) {
			Object monthlyAttendanceId = rows.getJSONObject(0).opt(Updates.KEY);
			return monthlyAttendanceId;
			// JSONObject updates = new JSONObject();
			// updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
			// JSONObject row = new JSONObject();
			// row.put("__key__", monthlyAttendanceId);
			// row.put("__type__", "delete");
			// updates.put(Data.Update.UPDATES, row);
			// ResourceEngine.update(updates);

		}
		return null;

	}

	private void updateNonPayableAndPayableLeaves(Object employeeMonthlyAttendanceId, boolean isExtraWorkingDayEncashableParameter) throws JSONException {

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("nonpayableleaves", totalLeavesInMonth - paybleLeave);
		row.put("payableleaves", paybleLeave);
		row.put("earnedleaveclosingbalance", earnedLeaveMonthlyClosingBalance);
		row.put("casualleaveclosingbalance", casualLeaveMonthlyClosingBalance);
		row.put("medicalleaveclosingbalance", medicalLeaveMonthlyClosingBalance);
		row.put("specialleaveclosingbalance", specialLeaveMonthlyClosingBalance);
		row.put("carryextraworkingday", totalExtraWorkingDayNetBalance);
		row.put("paidabsents", paybleAbsent);
		row.put("nonpaidabsents", nonPaybleAbsent);
		// if (!isExtraWorkingDayEncashableParameter) {
		// row.put("extraworkingdaybalance", totalExtraWorkingDaysInMonth);
		// }
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);

	}

	private void updateLeaveInfo(JSONArray employeeInfo, JSONObject leaveRule, Object employeeMonthlyAttendanceId, Object employeeId, double paidStatusCount, double unPaidStatusCount, Record record, double[] totalBalanceLeaves, double comboOffEWDsCount, double[] ewdAdded, HashMap<Integer, Double> leaveTypeOpeningBalance, boolean isJoining) throws Exception {
		// this method will iterate on individual
		boolean isPayable = DataTypeUtilities.booleanValue(leaveRule.opt("payable"));
		Integer leaveCreditId = (Integer) leaveRule.opt("leavecreditid");
		boolean isEWDAdjustAgainstLeave = DataTypeUtilities.booleanValue(leaveRule.opt("isextraworkingdaysadjustagainstleave"));
		boolean notAvailInCurrentMonth = DataTypeUtilities.booleanValue(leaveRule.opt("not_avail_in_current_month"));
		if (!isJoining) {
			notAvailInCurrentMonth = false;
		}
		Integer leaveTypeId = DataTypeUtilities.integerValue(leaveRule.opt("leavetypeid"));
		int accrueTypeId = DataTypeUtilities.integerValue(leaveRule.opt("accruetypeid"));
		double maxInAMonth = DataTypeUtilities.doubleValue(leaveRule.opt("maxleaveperapplication"));
		boolean isExtraWorkingDayEncashable = DataTypeUtilities.booleanValue(employeeInfo.getJSONObject(0).opt("leavepolicyid.isextraworkingdayencashable"));
		double[] earnLeavesGloble = { 0.0 };
		double[] netBalanceLeaves = { 0d };
		double debit = 0.0;
		if (leaveTypeId == HRISApplicationConstants.LeaveTypes.EARNED_LEAVE) {
			if (ewdAdded[0] == 1.0) {
				comboOffEWDsCount = 0.0;
			}
			ewdAdded[0] = 1.0;
			earnLeavesGloble[0] = 0.0;
			netBalanceLeaves[0] = 0.0;
			debit = takenEarnedLeavedInThisMonth;
			if (maxInAMonth != 0.0 && takenEarnedLeavedInThisMonth > maxInAMonth) {
				takenEarnedLeavedInThisMonth = maxInAMonth;
			}
			leaveTypeOpeningBalance.put(leaveTypeId, earnedLeaveMonthlyOpeningBalance);
			updateEarnedLeaveInfo(isPayable, leaveCreditId, isEWDAdjustAgainstLeave, employeeMonthlyAttendanceId, isExtraWorkingDayEncashable, employeeId, paidStatusCount, unPaidStatusCount, earnLeavesGloble, netBalanceLeaves, accrueTypeId, notAvailInCurrentMonth);
			calculateLeaveRegister(employeeId, leaveTypeId, record, earnedLeaveMonthlyClosingBalance, earnLeavesGloble, netBalanceLeaves, totalBalanceLeaves, debit, comboOffEWDsCount, leaveTypeOpeningBalance);
		} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.CASUAL_LEAVE) {
			if (ewdAdded[0] == 1.0) {
				comboOffEWDsCount = 0.0;
			}
			ewdAdded[0] = 1.0;
			earnLeavesGloble[0] = 0.0;
			netBalanceLeaves[0] = 0.0;
			debit = takenCasualLeavedInThisMonth;
			if (maxInAMonth != 0.0 && takenCasualLeavedInThisMonth > maxInAMonth) {
				takenCasualLeavedInThisMonth = maxInAMonth;
			}
			leaveTypeOpeningBalance.put(leaveTypeId, casualLeaveMonthlyOpeningBalance);
			updateCasualLeaveInfo(isPayable, leaveCreditId, isEWDAdjustAgainstLeave, employeeMonthlyAttendanceId, isExtraWorkingDayEncashable, employeeId, paidStatusCount, unPaidStatusCount, earnLeavesGloble, netBalanceLeaves, accrueTypeId, notAvailInCurrentMonth);
			calculateLeaveRegister(employeeId, leaveTypeId, record, casualLeaveMonthlyClosingBalance, earnLeavesGloble, netBalanceLeaves, totalBalanceLeaves, debit, comboOffEWDsCount, leaveTypeOpeningBalance);
		} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.MEDICAL_LEAVE) {
			if (ewdAdded[0] == 1.0) {
				comboOffEWDsCount = 0.0;
			}
			ewdAdded[0] = 1.0;
			earnLeavesGloble[0] = 0.0;
			netBalanceLeaves[0] = 0.0;
			debit = takenMedicalLeavedInThisMonth;
			if (maxInAMonth != 0.0 && takenMedicalLeavedInThisMonth > maxInAMonth) {
				takenMedicalLeavedInThisMonth = maxInAMonth;
			}
			leaveTypeOpeningBalance.put(leaveTypeId, medicalLeaveMonthlyOpeningBalance);
			updateMedicalLeaveInfo(isPayable, leaveCreditId, isEWDAdjustAgainstLeave, employeeMonthlyAttendanceId, isExtraWorkingDayEncashable, employeeId, paidStatusCount, unPaidStatusCount, earnLeavesGloble, netBalanceLeaves, accrueTypeId, notAvailInCurrentMonth);
			calculateLeaveRegister(employeeId, leaveTypeId, record, medicalLeaveMonthlyClosingBalance, earnLeavesGloble, netBalanceLeaves, totalBalanceLeaves, debit, comboOffEWDsCount, leaveTypeOpeningBalance);
		} else if (leaveTypeId == HRISApplicationConstants.LeaveTypes.SPECIAL_LEAVE) {
			updateSpecialLeaveInfo(isPayable, leaveCreditId, isEWDAdjustAgainstLeave, employeeMonthlyAttendanceId, isExtraWorkingDayEncashable, employeeId, paidStatusCount, unPaidStatusCount);
		} else {
			throw new RuntimeException("Unsupported type of leave found for employee: " + employeeEmailId);
		}
	}

	private void calculateLeaveRegister(Object employeeId, Integer leaveTypeId, Record record, double presentClosingBalance, double[] earnLeavesGloble, double[] netBalanceLeaves, double[] totalBalanceLeaves, double debit, double comboOffEWDsCount, HashMap<Integer, Double> leaveTypeOpeningBalance) throws Exception {
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		Calendar cal = Calendar.getInstance();
		cal.setTime(Translator.dateValue(TODAY_DATE));
		int currentMonthId = cal.get(Calendar.MONTH) + 1;
		int currentYearId = (int) (long) EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));

		int monthId = Translator.integerValue(record.getValue("monthid"));
		String monthName = EmployeeSalaryGenerationServlet.getMonthName(monthId);
		String currentMonthName = EmployeeSalaryGenerationServlet.getMonthName(currentMonthId);
		int yearId = Translator.integerValue(record.getValue("yearid"));

		JSONArray leaveRegisterArray = new EmployeeMonthlyAttendance().getLeaveRegisterArray(employeeId, monthId, yearId, leaveTypeId);
		if (leaveRegisterArray != null && leaveRegisterArray.length() > 0) {
			Object key = leaveRegisterArray.getJSONObject(0).opt("__key__");
			Object closingBalanceRecord = leaveRegisterArray.getJSONObject(0).opt("closingbalance");
			double leaveBalanceRecord = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("leavebalance"));
			double openingBalanceRecord = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("openingbalance"));
			String remarksRecord = Translator.stringValue(leaveRegisterArray.getJSONObject(0).opt("remarks"));
			int monthIdTemp = Translator.integerValue(leaveRegisterArray.getJSONObject(0).opt("newrecordmonthid") == null ? leaveRegisterArray.getJSONObject(0).opt("monthid") : leaveRegisterArray.getJSONObject(0).opt("newrecordmonthid"));
			int yearIdTemp = Translator.integerValue(leaveRegisterArray.getJSONObject(0).opt("newrecordyearid") == null ? leaveRegisterArray.getJSONObject(0).opt("yearid") : leaveRegisterArray.getJSONObject(0).opt("newrecordyearid"));
			double creditTemp = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("credit"));
			double totalEWDsOld = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("totalewds"));
			double previousClosingBalance = 0.0;
			// double allClosingBalance = earnedLeaveMonthlyClosingBalance +
			// casualLeaveMonthlyClosingBalance +
			// medicalLeaveMonthlyClosingBalance;
			String remarks = "";
			if (currentMonthId == monthId && currentYearId == yearId) {
				if (closingBalanceRecord != null) {
					previousClosingBalance = Translator.doubleValue(closingBalanceRecord);
					if (previousClosingBalance < presentClosingBalance) {
						leaveBalanceRecord += (presentClosingBalance - previousClosingBalance);
						previousClosingBalance = presentClosingBalance;
					} else {
						leaveBalanceRecord -= (previousClosingBalance - presentClosingBalance);
						previousClosingBalance = presentClosingBalance;
					}

				} else {
					previousClosingBalance = presentClosingBalance;
				}
				// finalLeaveBalance =
				if (comboOffEWDsCount < totalEWDsOld) {
					remarksRecord += "[" + (totalEWDsOld - comboOffEWDsCount) + " EWD updated in month " + monthName + " and subtract Leave balance from " + currentMonthName + "'s final leave balance]";
				} else if (comboOffEWDsCount > totalEWDsOld) {
					remarksRecord += "[" + (comboOffEWDsCount - totalEWDsOld) + " EWD updated in month " + monthName + " and added Leave balance in " + currentMonthName + "'s final leave balance]";
				}
				leaveBalanceRecord += (comboOffEWDsCount - totalEWDsOld);
				updateLeaveRegister(key, presentClosingBalance, leaveBalanceRecord, remarksRecord, earnLeavesGloble, netBalanceLeaves, debit, comboOffEWDsCount);
				totalBalanceLeaves[0] += leaveBalanceRecord;
			} else {
				leaveRegisterArray = new EmployeeMonthlyAttendance().getLeaveRegisterArray(employeeId, currentMonthId, currentYearId, leaveTypeId);
				if (leaveRegisterArray.length() > 0) {
					Object currentLeaveRegisterKey = leaveRegisterArray.getJSONObject(0).opt(Updates.KEY);
					double leaveBalanceTemp = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("leavebalance"));
					double openingForCurrentMonth = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("openingbalance"));
					double difference = 0.0;
					JSONArray previousLeaveRegisterArray = getLeaveRegisterArray(employeeId, currentLeaveRegisterKey, leaveTypeId);
					if (previousLeaveRegisterArray != null && previousLeaveRegisterArray.length() > 0) {
						// opening balance for current newly record
						openingBalanceRecord = Translator.doubleValue(previousLeaveRegisterArray.getJSONObject(0).opt("leavebalance")); // opening balance for current newly record
						leaveBalanceRecord = Translator.doubleValue(previousLeaveRegisterArray.getJSONObject(0).opt("leavebalance"));
					}
					if (closingBalanceRecord != null) {
						previousClosingBalance = Translator.doubleValue(closingBalanceRecord);
						difference = presentClosingBalance - previousClosingBalance;
						if (previousClosingBalance < presentClosingBalance) {
							if (remarksRecord.length() > 0) {

								remarks += remarksRecord + ", [Leave updated in month " + monthName + " and add leave balance in " + currentMonthName + "'s leave balance]";
							} else {
								remarks += "[Leave updated in month " + monthName + " and add leave balance in " + currentMonthName + "'s leave balance]";
							}
							leaveBalanceTemp += (presentClosingBalance - previousClosingBalance);
							leaveBalanceRecord += (presentClosingBalance - previousClosingBalance);
							previousClosingBalance = presentClosingBalance;
						} else {
							if (remarksRecord.length() > 0) {

								remarks += remarksRecord + ", [Leave updated in month " + monthName + " and subtract leave balance from " + currentMonthName + "'s leave balance]";
							} else {
								remarks += "[Leave updated in month " + monthName + " and subtract leave balance from " + currentMonthName + "'s leave balance]";
							}
							leaveBalanceTemp -= (previousClosingBalance - presentClosingBalance);
							leaveBalanceRecord -= (previousClosingBalance - presentClosingBalance);
							previousClosingBalance = presentClosingBalance;
						}

					} else {
						previousClosingBalance = presentClosingBalance;
					}
					if (comboOffEWDsCount < totalEWDsOld) {
						leaveBalanceRecord -= (totalEWDsOld - comboOffEWDsCount);
						remarks += "[" + (totalEWDsOld - comboOffEWDsCount) + " EWD updated in month " + monthName + " and subtract EWD balance from " + currentMonthName + "'s final leave balance]";
					} else if (comboOffEWDsCount > totalEWDsOld) {
						leaveBalanceRecord += (comboOffEWDsCount - totalEWDsOld);
						remarks += "[" + (comboOffEWDsCount - totalEWDsOld) + " EWD updated in month " + monthName + " and added EWD balance in " + currentMonthName + "'s final leave balance]";
					}

					if (monthIdTemp != currentMonthId && yearIdTemp != currentYearId) {
						insertNewOrUpdateExistingLeaveReister(null, employeeId, monthId, yearId, openingBalanceRecord, currentMonthId, currentYearId, creditTemp, previousClosingBalance, totalLeavesInMonth, leaveBalanceRecord, earnLeavesGloble, netBalanceLeaves, leaveTypeId, remarks, debit, comboOffEWDsCount);
					} else {
						insertNewOrUpdateExistingLeaveReister(key, employeeId, monthId, yearId, openingBalanceRecord, currentMonthId, currentYearId, creditTemp, previousClosingBalance, totalLeavesInMonth, leaveBalanceRecord, earnLeavesGloble, netBalanceLeaves, leaveTypeId, remarks, debit, comboOffEWDsCount);
					}
					openingForCurrentMonth = openingForCurrentMonth + difference;
					openingForCurrentMonth = openingForCurrentMonth < 0.0 ? 0.0 : openingForCurrentMonth;
					leaveBalanceTemp += (comboOffEWDsCount - totalEWDsOld);
					updateLeaveRegisterForPreviousMonth(currentLeaveRegisterKey, leaveBalanceTemp, earnLeavesGloble, netBalanceLeaves, openingForCurrentMonth);
					leaveTypeOpeningBalance.put(leaveTypeId, openingForCurrentMonth);
					if (!leaveTypeOpeningBalance.containsKey(0)) {
						leaveTypeOpeningBalance.put(0, 0.0);
						leaveTypeOpeningBalance.put(5, (double) Translator.integerValue(currentMonthId));
						leaveTypeOpeningBalance.put(6, (double) Translator.integerValue(currentYearId));
					}
					totalBalanceLeaves[0] += leaveBalanceTemp;
				} else {
					// if not found for current month
					int monthCounter = 0;
					cal = Calendar.getInstance();
					// TODAY_DATE = "2013-08-25";
					int lastMonthId = 0;
					int lastYearId = 0;
					for (;;) {
						cal.setTime(Translator.dateValue(TODAY_DATE));
						cal.add(Calendar.MONTH, -monthCounter);
						lastMonthId = cal.get(Calendar.MONTH) + 1;
						lastYearId = (int) (long) EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
						leaveRegisterArray = new EmployeeMonthlyAttendance().getLeaveRegisterArray(employeeId, lastMonthId, lastYearId, leaveTypeId);
						if (leaveRegisterArray != null && leaveRegisterArray.length() > 0) {
							break;
						}
						monthCounter++;
						if (monthCounter > 100) {
							return;
						}
					}
					int lastKey = Translator.integerValue(leaveRegisterArray.getJSONObject(0).opt(Updates.KEY));
					int underUpdationKey = Translator.integerValue(key);
					double leaveBalanceTemp = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("leavebalance"));
					double openingForCurrentMonth = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("openingbalance"));

					monthIdTemp = Translator.integerValue(leaveRegisterArray.getJSONObject(0).opt("newrecordmonthid") == null ? leaveRegisterArray.getJSONObject(0).opt("monthid") : leaveRegisterArray.getJSONObject(0).opt("newrecordmonthid"));
					yearIdTemp = Translator.integerValue(leaveRegisterArray.getJSONObject(0).opt("newrecordyearid") == null ? leaveRegisterArray.getJSONObject(0).opt("yearid") : leaveRegisterArray.getJSONObject(0).opt("newrecordyearid"));
					double difference = 0.0;
					if (closingBalanceRecord != null) {
						previousClosingBalance = Translator.doubleValue(closingBalanceRecord);
						difference = presentClosingBalance - previousClosingBalance;
						if (previousClosingBalance < presentClosingBalance) {
							if (remarksRecord.length() > 0) {
								remarks += remarksRecord + ", [Leave updated in month " + monthName + " and add leave balance in " + currentMonthName + "'s leave balance]";
							} else {
								remarks += "[Leave updated in month " + monthName + " and add leave balance in " + currentMonthName + "'s leave balance]";
							}
							leaveBalanceRecord += (presentClosingBalance - previousClosingBalance);
							previousClosingBalance = presentClosingBalance;
						} else {
							if (remarksRecord.length() > 0) {
								remarks += remarksRecord + ", [Leave updated in month " + monthName + " and subtract leave balance from " + currentMonthName + "'s leave balance]";
							} else {
								remarks += "[Leave updated in month " + monthName + " and subtract leave balance from " + currentMonthName + "'s leave balance]";
							}
							leaveBalanceRecord -= (previousClosingBalance - presentClosingBalance);
							previousClosingBalance = presentClosingBalance;
						}

					} else {
						leaveBalanceRecord = previousClosingBalance = presentClosingBalance;
					}
					if (lastKey != underUpdationKey) {

						if (monthIdTemp != currentMonthId && yearIdTemp != currentYearId) {
							key = null;
						}
						insertNewOrUpdateExistingLeaveReister(key, employeeId, monthId, yearId, openingBalanceRecord, currentMonthId, currentYearId, creditTemp, previousClosingBalance, totalLeavesInMonth, leaveBalanceRecord, earnLeavesGloble, netBalanceLeaves, leaveTypeId, remarks, debit, comboOffEWDsCount);

						leaveBalanceTemp = leaveBalanceTemp + difference;
						openingForCurrentMonth = (openingForCurrentMonth + difference) < 0.0 ? 0.0 : (openingForCurrentMonth + difference);

						updateLeaveRegisterForPreviousMonth(lastKey, leaveBalanceTemp, earnLeavesGloble, netBalanceLeaves, openingForCurrentMonth);
						leaveTypeOpeningBalance.put(leaveTypeId, openingForCurrentMonth);
						if (!leaveTypeOpeningBalance.containsKey(0)) {
							leaveTypeOpeningBalance.put(0, 0.0);
							leaveTypeOpeningBalance.put(5, (double) Translator.integerValue(lastMonthId));
							leaveTypeOpeningBalance.put(6, (double) Translator.integerValue(lastYearId));
						}
					} else {
						JSONArray leaveRegister = getNewLeaveRegister(employeeId, currentMonthId, currentYearId, leaveTypeId, lastMonthId, lastYearId);
						if (leaveRegister != null && leaveRegister.length() > 0) {
							Object keyForUpdate = leaveRegister.getJSONObject(0).opt("__key__");
							updateLeaveRegister(keyForUpdate, presentClosingBalance, leaveBalanceRecord, remarksRecord, earnLeavesGloble, netBalanceLeaves, debit, comboOffEWDsCount);
						} else {
							insertNewOrUpdateExistingLeaveReister(lastKey, employeeId, monthId, yearId, openingBalanceRecord, currentMonthId, currentYearId, creditTemp, previousClosingBalance, totalLeavesInMonth, leaveBalanceRecord, earnLeavesGloble, netBalanceLeaves, leaveTypeId, remarks, debit, comboOffEWDsCount);
						}
					}
				}
			}
		}
	}

	private JSONArray getNewLeaveRegister(Object employeeId, int currentMonthId, int currentYearId, Integer leaveTypeId, int lastMonthId, int lastYearId) throws Exception {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "leaveregister");
		JSONArray array = new JSONArray();

		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);

		query.put(Data.Query.FILTERS, "leavetypeid = " + leaveTypeId + " AND employeeid = " + employeeId + " AND newrecordmonthid = " + currentMonthId + " AND newrecordyearid = " + currentYearId + " AND monthid = " + lastMonthId + " AND yearid = " + lastYearId);
		JSONObject attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("leaveregister");

		return rows;
	}

	private JSONArray getLeaveRegisterArray(Object employeeId, Object currentLeaveRegisterKey, Integer leaveTypeId) throws JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
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
		query.put(Data.Query.COLUMNS, array);

		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "__key__");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		query.put(Data.Query.ORDERS, new JSONArray().put(order));
		query.put(Data.Query.FILTERS, Updates.KEY + " != " + currentLeaveRegisterKey + " and leavetypeid = " + leaveTypeId + " and employeeid = " + employeeId);
		query.put(Data.Query.MAX_ROWS, 1);
		JSONObject attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("leaveregister");

		return rows;
	}

	private void updateLeaveRegisterForPreviousMonth(Object key, double leaveBalance, double[] earnLeavesGloble, double[] netBalanceLeavesGloble, Object openingForCurrentMonth) throws JSONException {
		if (leaveBalance < 0.0) {
			leaveBalance = 0.0;
		}
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "leaveregister");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		// row.put("leavebalance", leaveBalance);
		// row.put("totalewds", comboOffEWDsCount);
		if (openingForCurrentMonth != null) {
			row.put("openingbalance", openingForCurrentMonth);
		}
		// row.put("remarks", remarks);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void insertNewOrUpdateExistingLeaveReister(Object key, Object employeeId, int monthId, int yearId, double openingBalanceRecord, int currentMonthId, int currentYearId, double creditTemp, double closingBalance, double debitTemp, double leaveBalance, double[] earnLeavesGloble, double[] netBalanceLeaves, Object leaveTypeIdTemp, String remarks, double debit, double comboOffEWDsCount) throws Exception {
		if (leaveBalance < 0.0) {
			leaveBalance = 0.0;
		}
		if (openingBalanceRecord < 0.0) {
			openingBalanceRecord = 0.0;
		}
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "leaveregister");
		JSONObject row = new JSONObject();
		if (key != null) {
			row.put("__key__", key);
		}
		row.put("employeeid", employeeId);
		row.put("yearid", yearId);
		row.put("monthid", monthId);
		row.put("newrecordyearid", currentYearId);
		row.put("newrecordmonthid", currentMonthId);
		row.put("credit", creditTemp);
		row.put("openingbalance", openingBalanceRecord);
		row.put("closingbalance", closingBalance);
		row.put("debit", debit);
		row.put("leavebalance", leaveBalance);
		row.put("earnleaves", earnLeavesGloble[0]);
		row.put("leavetypeid", leaveTypeIdTemp);
		row.put("remarks", remarks);
		row.put("totalewds", comboOffEWDsCount);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateLeaveRegister(Object key, double closingBalance, double leaveBalance, String remarks, double[] earnLeaves, double[] netBalanceLeaves, double debit, double comboOffEWDsCount) throws JSONException {
		if (leaveBalance < 0.0) {
			leaveBalance = 0.0;
		}
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "leaveregister");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		row.put("earnleaves", earnLeaves[0]);
		row.put("closingbalance", closingBalance);
		row.put("leavebalance", leaveBalance);
		row.put("remarks", remarks);
		row.put("debit", debit);
		row.put("totalewds", comboOffEWDsCount);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateEarnedLeaveInfo(boolean isPayable, Integer leaveCreditId, boolean isEWDAdjustAgainstLeave, Object employeeMonthlyAttendanceId, boolean isExtraWorkingDayEncashable, Object employeeId, double paidStatusCount, double unPaidStatusCount, double[] earnLeavesGloble, double[] netBalanceLeaves, int accrueTypeId, boolean notAvailInCurrentMonth) throws JSONException {
		if (leaveCreditId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
			double presentPercentage = (totalPresentsInMonth / totalWorkingDaysInMonth) * 100;
			double earnedLeaves = totalCreditEarnedLeaveInMonth;
			if (accrueTypeId != HRISApplicationConstants.LEAVE_MONTHLY_ACCRUE) {
				earnedLeaves = (presentPercentage * totalCreditEarnedLeaveInMonth) / 100;
			}
			if (leaveCreditId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
				earnedLeaves = totalCreditEarnedLeaveInMonth;
			}
			earnLeavesGloble[0] += earnedLeaves;

			updateMonthlyEarnEL(employeeMonthlyAttendanceId, earnedLeaves);
			if (unPaidStatusCount > 0) {
				takenEarnedLeavedInThisMonth -= unPaidStatusCount;
			}
			if (takenEarnedLeavedInThisMonth == 0.0) {
				earnedLeaveMonthlyClosingBalance += (earnedLeaveMonthlyOpeningBalance + earnedLeaves);
			}
			double tEarnedLeaves = earnedLeaves;
			if (notAvailInCurrentMonth && takenEarnedLeavedInThisMonth > 0.0) {
				earnedLeaveMonthlyClosingBalance += tEarnedLeaves;
			}
			if (notAvailInCurrentMonth) {
				earnedLeaves = 0.0;
			}
			boolean isTakenLeaves = false;
			if (takenEarnedLeavedInThisMonth > 0) {
				isTakenLeaves = true;
			}
			if (isPayable) {
				double tempBalanceLeavesExtra = 0.0;
				if (isEWDAdjustAgainstLeave && totalExtraWorkingDaysInMonth > 0.0 && takenEarnedLeavedInThisMonth > 0.0) {
					tempBalanceLeavesExtra = totalExtraWorkingDaysInMonth - takenEarnedLeavedInThisMonth;
					if (tempBalanceLeavesExtra < 0) {
						tempBalanceLeavesExtra = tempBalanceLeavesExtra * (-1);
						paybleLeave += totalExtraWorkingDaysInMonth;
						totalExtraWorkingDaysInMonth = 0.0;
						totalExtraWorkingDayNetBalance = 0.0;
						takenEarnedLeavedInThisMonth = tempBalanceLeavesExtra;
					} else {
						paybleLeave += takenEarnedLeavedInThisMonth;
						totalExtraWorkingDaysInMonth = tempBalanceLeavesExtra;
						totalExtraWorkingDayNetBalance = tempBalanceLeavesExtra;
						takenEarnedLeavedInThisMonth = 0.0;
						earnedLeaveMonthlyClosingBalance += (earnedLeaves + earnedLeaveMonthlyOpeningBalance);
					}
				}
				if (takenEarnedLeavedInThisMonth > 0) {
					double totalBalanceLeaves = earnedLeaves + earnedLeaveMonthlyOpeningBalance;
					int totalBalanceLeavesINTEGER = (int) totalBalanceLeaves;
					// double totalBalanceLeavesDOUBLE = totalBalanceLeaves - totalBalanceLeavesINTEGER;
					int takenEarnLeavesINTEGER = (int) takenEarnedLeavedInThisMonth;
					double takenEarnLeavesDOUBLE = takenEarnedLeavedInThisMonth - takenEarnLeavesINTEGER;

					if (takenEarnedLeavedInThisMonth <= totalBalanceLeaves) {
						paybleLeave += takenEarnLeavesINTEGER;
						totalBalanceLeavesINTEGER -= takenEarnLeavesINTEGER;
						totalBalanceLeaves -= takenEarnLeavesINTEGER;
						if (totalBalanceLeaves >= 0.50 && takenEarnLeavesDOUBLE >= 0.50) {
							paybleLeave += 0.50;
							// totalBalanceLeavesDOUBLE -= 0.50;
							totalBalanceLeaves -= 0.50;
						}
						earnedLeaveMonthlyClosingBalance += totalBalanceLeaves;

					} else {
						paybleLeave += totalBalanceLeavesINTEGER;
						totalBalanceLeaves -= totalBalanceLeavesINTEGER;
						takenEarnedLeavedInThisMonth -= totalBalanceLeavesINTEGER;
						totalBalanceLeavesINTEGER = 0;
						if (totalBalanceLeaves >= 0.50 && takenEarnedLeavedInThisMonth >= 0.50) {
							paybleLeave += 0.50;
							totalBalanceLeaves -= 0.50;
						}
						earnedLeaveMonthlyClosingBalance += totalBalanceLeaves;
					}
				}
				LogUtility.writeError("3 isAbsentAdjustWithLeaves >> " + isAbsentAdjustWithLeaves + " << totalAbsentInMonth >> " + totalAbsentInMonth + " << totalExtraWorkingDaysInMonth >> " + totalExtraWorkingDaysInMonth + " << nonPaybleAbsent >> " + nonPaybleAbsent + " << payableAbsent >> " + paybleAbsent);
				if (isAbsentAdjustWithLeaves && totalAbsentInMonth > 0.0 && ((int) earnedLeaveMonthlyClosingBalance) > 0) {
					if (totalAbsentInMonth < earnedLeaveMonthlyClosingBalance) {
						paybleAbsent += totalAbsentInMonth;
						nonPaybleAbsent = 0.0;
						earnedLeaveMonthlyClosingBalance -= totalAbsentInMonth;
						totalAbsentInMonth = 0.0;
					} else {
						paybleAbsent += ((int) earnedLeaveMonthlyClosingBalance);
						nonPaybleAbsent += totalAbsentInMonth - earnedLeaveMonthlyClosingBalance;
						earnedLeaveMonthlyClosingBalance = 0;
						totalAbsentInMonth -= earnedLeaveMonthlyClosingBalance;
					}
				} else if (isAbsentAdjustWithLeaves && totalAbsentInMonth > 0.0 && earnedLeaveMonthlyClosingBalance >= 0.50) {
					double tempBalance = 0.50;
					earnedLeaveMonthlyClosingBalance -= tempBalance;
					if (totalAbsentInMonth < tempBalance) {
						paybleAbsent += totalAbsentInMonth;
						nonPaybleAbsent = 0.0;
						tempBalance -= totalAbsentInMonth;
						totalAbsentInMonth = 0.0;
					} else {
						paybleAbsent += tempBalance;
						nonPaybleAbsent += (totalAbsentInMonth - tempBalance);
						totalAbsentInMonth -= tempBalance;
					}
				}
				LogUtility.writeError("4 isAbsentAdjustWithLeaves >> " + isAbsentAdjustWithLeaves + " << totalAbsentInMonth >> " + totalAbsentInMonth + " << totalExtraWorkingDaysInMonth >> " + totalExtraWorkingDaysInMonth + " << nonPaybleAbsent >> " + nonPaybleAbsent + " << payableAbsent >> " + paybleAbsent + " << earnedLeaveMonthlyClosingBalance >> " + earnedLeaveMonthlyClosingBalance + " << earnedLeaveMonthlyOpeningBalance >> " + earnedLeaveMonthlyOpeningBalance);
				if ((!isTakenLeaves && !isAbsentAdjustWithLeaves) || (!isTakenLeaves && isAbsentAdjustWithLeaves && totalAbsentInMonth == 0.0 && paybleAbsent == 0.0)) {
					earnedLeaveMonthlyClosingBalance = earnedLeaveMonthlyOpeningBalance;
					earnedLeaveMonthlyClosingBalance += tEarnedLeaves;
					netBalanceLeaves[0] = tEarnedLeaves;
					updateELNetBalance(employeeMonthlyAttendanceId, tEarnedLeaves);
				} else if (earnedLeaveMonthlyClosingBalance >= earnedLeaveMonthlyOpeningBalance) {
					double tempLeaveBalance = earnedLeaveMonthlyClosingBalance - earnedLeaveMonthlyOpeningBalance;
					if (notAvailInCurrentMonth) {
						tempLeaveBalance -= tEarnedLeaves;
					}
					tEarnedLeaves = tempLeaveBalance;
					netBalanceLeaves[0] = tEarnedLeaves;
					updateELNetBalance(employeeMonthlyAttendanceId, tEarnedLeaves);
				} else {
					double tempLeaveBalance = earnedLeaveMonthlyOpeningBalance - earnedLeaveMonthlyClosingBalance;
					LogUtility.writeError("5 << tempLeaveBalance >> " + tempLeaveBalance + " << notAvailInCurrentMonth >> " + notAvailInCurrentMonth + "<< tEarnedLeaves >> " + tEarnedLeaves);
					if (notAvailInCurrentMonth) {
						tempLeaveBalance += tEarnedLeaves;
					}
					if (tEarnedLeaves > tempLeaveBalance) {
						tEarnedLeaves = tEarnedLeaves - tempLeaveBalance;
					} else {
						tEarnedLeaves = 0.0;
					}
					netBalanceLeaves[0] = tEarnedLeaves;
					LogUtility.writeError("6 << tempLeaveBalance >> " + tempLeaveBalance + " << notAvailInCurrentMonth >> " + notAvailInCurrentMonth + " << tEarnedLeaves >> " + tEarnedLeaves);
					updateELNetBalance(employeeMonthlyAttendanceId, tEarnedLeaves);
				}
				LogUtility.writeError("7 << earnedLeaveMonthlyClosingBalance >> " + earnedLeaveMonthlyClosingBalance + " << earnedLeaveMonthlyOpeningBalance >> " + earnedLeaveMonthlyOpeningBalance);
			} else {
				if (isEWDAdjustAgainstLeave) {
					if ((totalExtraWorkingDaysInMonth - takenEarnedLeavedInThisMonth) > 0) {
						totalExtraWorkingDaysInMonth -= takenEarnedLeavedInThisMonth;
					} else {
						totalExtraWorkingDaysInMonth = 0.0;
					}
				}
			}
		} else if (leaveCreditId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {

		}
	}

	private void updateMedicalLeaveInfo(boolean isPayable, Integer leaveCreditId, boolean isEWDAdjustAgainstLeave, Object employeeMonthlyAttendanceId, boolean isExtraWorkingDayEncashable, Object employeeId, double paidStatusCount, double unPaidStatusCount, double[] earnLeavesGloble, double[] netBalanceLeaves, int accrueTypeId, boolean notAvailInCurrentMonth) throws JSONException {
		if (leaveCreditId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
			double presentPercentage = (totalPresentsInMonth / totalWorkingDaysInMonth) * 100;
			double earnedLeaves = totalCreditMedicalLeaveInMonth;
			if (accrueTypeId != HRISApplicationConstants.LEAVE_MONTHLY_ACCRUE) {
				earnedLeaves = (presentPercentage * totalCreditMedicalLeaveInMonth) / 100;
			}

			if (leaveCreditId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
				earnedLeaves = totalCreditMedicalLeaveInMonth;
			}
			earnLeavesGloble[0] += earnedLeaves;
			updateMonthlyEarnML(employeeMonthlyAttendanceId, earnedLeaves);
			if (unPaidStatusCount > 0) {
				takenMedicalLeavedInThisMonth -= unPaidStatusCount;
			}
			if (takenMedicalLeavedInThisMonth == 0) {
				medicalLeaveMonthlyClosingBalance += (medicalLeaveMonthlyOpeningBalance + earnedLeaves);
			}
			double tEarnedLeaves = earnedLeaves;
			if (notAvailInCurrentMonth && takenMedicalLeavedInThisMonth > 0) {
				medicalLeaveMonthlyClosingBalance += earnedLeaves;
			}
			if (notAvailInCurrentMonth) {
				earnedLeaves = 0.0;
			}
			boolean isTakenLeaves = false;
			if (takenMedicalLeavedInThisMonth > 0) {
				isTakenLeaves = true;
			}
			if (isPayable) {
				double tempBalanceLeavesExtra = 0.0;
				if (isEWDAdjustAgainstLeave && totalExtraWorkingDaysInMonth > 0.0 && takenMedicalLeavedInThisMonth > 0.0) {
					tempBalanceLeavesExtra = totalExtraWorkingDaysInMonth - takenMedicalLeavedInThisMonth;
					if (tempBalanceLeavesExtra < 0) {
						tempBalanceLeavesExtra = tempBalanceLeavesExtra * (-1);
						paybleLeave += totalExtraWorkingDaysInMonth;
						totalExtraWorkingDaysInMonth = 0.0;
						totalExtraWorkingDayNetBalance = 0.0;
						takenMedicalLeavedInThisMonth = tempBalanceLeavesExtra;
					} else {
						paybleLeave += takenMedicalLeavedInThisMonth;
						totalExtraWorkingDaysInMonth = tempBalanceLeavesExtra;
						totalExtraWorkingDayNetBalance = tempBalanceLeavesExtra;
						takenMedicalLeavedInThisMonth = 0.0;
						medicalLeaveMonthlyClosingBalance += earnedLeaves + medicalLeaveMonthlyOpeningBalance;
					}
				}

				if (takenMedicalLeavedInThisMonth > 0) {
					double totalBalanceLeaves = earnedLeaves + medicalLeaveMonthlyOpeningBalance;
					int totalBalanceLeavesINTEGER = (int) totalBalanceLeaves;
					int takenEarnLeavesINTEGER = (int) takenMedicalLeavedInThisMonth;
					double takenEarnLeavesDOUBLE = takenMedicalLeavedInThisMonth - takenEarnLeavesINTEGER;

					if (takenMedicalLeavedInThisMonth <= totalBalanceLeaves) {
						paybleLeave += takenEarnLeavesINTEGER;
						totalBalanceLeavesINTEGER -= takenEarnLeavesINTEGER;
						totalBalanceLeaves -= takenEarnLeavesINTEGER;
						if (totalBalanceLeaves >= 0.50 && takenEarnLeavesDOUBLE >= 0.50) {
							paybleLeave += 0.50;
							totalBalanceLeaves -= 0.50;
						}
						medicalLeaveMonthlyClosingBalance += totalBalanceLeaves;

					} else {
						paybleLeave += totalBalanceLeavesINTEGER;
						totalBalanceLeaves -= totalBalanceLeavesINTEGER;
						takenMedicalLeavedInThisMonth -= totalBalanceLeavesINTEGER;
						totalBalanceLeavesINTEGER = 0;
						if (totalBalanceLeaves >= 0.50 && takenMedicalLeavedInThisMonth >= 0.50) {
							paybleLeave += 0.50;
							totalBalanceLeaves -= 0.50;
						}
						medicalLeaveMonthlyClosingBalance += totalBalanceLeaves;
					}
				}
				if (isAbsentAdjustWithLeaves && totalAbsentInMonth > 0.0 && ((int) medicalLeaveMonthlyClosingBalance) > 0) {
					if (totalAbsentInMonth < medicalLeaveMonthlyClosingBalance) {
						paybleAbsent += totalAbsentInMonth;
						nonPaybleAbsent = 0.0;
						medicalLeaveMonthlyClosingBalance -= totalAbsentInMonth;
						totalAbsentInMonth = 0.0;
					} else {
						paybleAbsent += ((int) medicalLeaveMonthlyClosingBalance);
						nonPaybleAbsent += totalAbsentInMonth - medicalLeaveMonthlyClosingBalance;
						medicalLeaveMonthlyClosingBalance = 0;
						totalAbsentInMonth -= medicalLeaveMonthlyClosingBalance;
					}
				} else if (isAbsentAdjustWithLeaves && totalAbsentInMonth > 0.0 && medicalLeaveMonthlyClosingBalance >= 0.50) {
					double tempBalance = 0.50;
					medicalLeaveMonthlyClosingBalance -= tempBalance;
					if (totalAbsentInMonth < tempBalance) {
						paybleAbsent += totalAbsentInMonth;
						nonPaybleAbsent = 0.0;
						tempBalance -= totalAbsentInMonth;
						totalAbsentInMonth = 0.0;
					} else {
						paybleAbsent += tempBalance;
						nonPaybleAbsent += (totalAbsentInMonth - tempBalance);
						totalAbsentInMonth -= tempBalance;
					}
				}
				if ((!isTakenLeaves && !isAbsentAdjustWithLeaves) || (!isTakenLeaves && isAbsentAdjustWithLeaves && totalAbsentInMonth == 0.0 && paybleAbsent == 0.0)) {
					medicalLeaveMonthlyClosingBalance = medicalLeaveMonthlyOpeningBalance;
					medicalLeaveMonthlyClosingBalance += tEarnedLeaves;
					netBalanceLeaves[0] = tEarnedLeaves;
					updateELNetBalance(employeeMonthlyAttendanceId, tEarnedLeaves);
				} else if (medicalLeaveMonthlyClosingBalance >= medicalLeaveMonthlyOpeningBalance) {
					double tempLeaveBalance = medicalLeaveMonthlyClosingBalance - medicalLeaveMonthlyOpeningBalance;
					if (notAvailInCurrentMonth) {
						tempLeaveBalance -= tEarnedLeaves;
					}
					tEarnedLeaves = tempLeaveBalance;
					netBalanceLeaves[0] = tEarnedLeaves;
					updateMLNetBalance(employeeMonthlyAttendanceId, tEarnedLeaves);
				} else {
					double tempLeaveBalance = medicalLeaveMonthlyOpeningBalance - medicalLeaveMonthlyClosingBalance;
					if (notAvailInCurrentMonth) {
						tempLeaveBalance += tEarnedLeaves;
					}
					if (tEarnedLeaves > tempLeaveBalance) {
						tEarnedLeaves = tEarnedLeaves - tempLeaveBalance;
					} else {
						tEarnedLeaves = 0.0;
					}
					netBalanceLeaves[0] = tEarnedLeaves;
					updateMLNetBalance(employeeMonthlyAttendanceId, tEarnedLeaves);
				}
			} else {

				if (isEWDAdjustAgainstLeave) {
					if ((totalExtraWorkingDaysInMonth - takenMedicalLeavedInThisMonth) > 0) {
						totalExtraWorkingDaysInMonth -= takenMedicalLeavedInThisMonth;
					} else {
						totalExtraWorkingDaysInMonth = 0.0;
					}
				}
			}
		} else if (leaveCreditId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {

		}
	}

	private void updateSpecialLeaveInfo(boolean isPayable, Integer leaveCreditId, boolean isEWDAdjustAgainstLeave, Object employeeMonthlyAttendanceId, boolean isExtraWorkingDayEncashable, Object employeeId, double paidStatusCount, double unPaidStatusCount) throws JSONException {
		if (leaveCreditId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT) {
			if (unPaidStatusCount > 0) {
				takenSpecialLeavedInThisMonth -= unPaidStatusCount;
			}
			if (takenSpecialLeavedInThisMonth > 0) {
				boolean isExtraWorkingGreaterThenZero = false;
				double tempBalanceLeave = 0d;
				if (isEWDAdjustAgainstLeave && totalExtraWorkingDaysInMonth > 0.0) {
					isExtraWorkingGreaterThenZero = true;
					tempBalanceLeave = totalExtraWorkingDaysInMonth - takenSpecialLeavedInThisMonth;
					if (tempBalanceLeave > 0) {
						specialLeaveMonthlyClosingBalance = specialLeaveMonthlyOpeningBalance;
						if (isPayable) {
							paybleLeave += takenSpecialLeavedInThisMonth;
						} else {
							nonPaybleLeave += takenSpecialLeavedInThisMonth;
						}
						totalExtraWorkingDaysInMonth -= takenSpecialLeavedInThisMonth;
					} else {
						tempBalanceLeave = tempBalanceLeave * (-1);
						if (isPayable) {
							paybleLeave += totalExtraWorkingDaysInMonth;
						} else {
							nonPaybleLeave += totalExtraWorkingDaysInMonth;
						}
						totalExtraWorkingDaysInMonth = 0.0;
					}
				}
				if (tempBalanceLeave > 0 || !isExtraWorkingGreaterThenZero) {
					if (specialLeaveMonthlyOpeningBalance > 0) {

						double tempBalanceLeaveExtra = 0.0;
						if (isExtraWorkingGreaterThenZero) {
							tempBalanceLeaveExtra = specialLeaveMonthlyOpeningBalance - tempBalanceLeave;
						} else {
							tempBalanceLeaveExtra = specialLeaveMonthlyOpeningBalance - takenSpecialLeavedInThisMonth;
						}

						if (tempBalanceLeaveExtra > 0) {
							specialLeaveMonthlyClosingBalance = tempBalanceLeaveExtra;
							if (isPayable) {
								paybleLeave += (specialLeaveMonthlyOpeningBalance - tempBalanceLeaveExtra);
							} else {
								nonPaybleLeave += tempBalanceLeave;
							}
							specialLeaveMonthlyOpeningBalance -= tempBalanceLeave;
						} else {
							specialLeaveMonthlyClosingBalance = 0.0;
							if (isPayable) {
								paybleLeave += specialLeaveMonthlyOpeningBalance;
							} else {
								nonPaybleLeave += tempBalanceLeave;
							}
							specialLeaveMonthlyOpeningBalance = 0.0;
						}
					} else {
						if (isExtraWorkingGreaterThenZero) {
							nonPaybleLeave += tempBalanceLeave;
						} else {
							nonPaybleLeave += takenSpecialLeavedInThisMonth;
						}
					}
				}
			}
		}
		// updateEmployeeSpecialLeaveBalance(employeeId, specialLeaveMonthlyClosingBalance);
	}

	private void updateCasualLeaveInfo(boolean isPayable, Integer leaveCreditId, boolean isEWDAdjustAgainstLeave, Object employeeMonthlyAttendanceId, boolean isExtraWorkingDayEncashable, Object employeeId, double paidStatusCount, double unPaidStatusCount, double[] earnLeavesGloble, double[] netBalanceLeaves, int accrueTypeId, boolean notAvailInCurrentMonth) throws JSONException {
		if (leaveCreditId == HRISApplicationConstants.LeaveCredit.MONTHLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
			double presentPercentage = (totalPresentsInMonth / totalWorkingDaysInMonth) * 100;
			double earnedLeaves = totalCreditCasualLeavesInMonth;
			if (accrueTypeId != HRISApplicationConstants.LEAVE_MONTHLY_ACCRUE) {
				earnedLeaves = (presentPercentage * totalCreditCasualLeavesInMonth) / 100;
			}

			if (leaveCreditId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.HALF_YEARLY_CREDIT || leaveCreditId == HRISApplicationConstants.LeaveCredit.QUATERLY_YEARLY_CREDIT) {
				earnedLeaves = totalCreditCasualLeavesInMonth;
			}

			earnLeavesGloble[0] += earnedLeaves;
			updateMonthlyEarnCL(employeeMonthlyAttendanceId, earnedLeaves);
			if (unPaidStatusCount > 0) {
				takenCasualLeavedInThisMonth -= unPaidStatusCount;
			}
			if (takenCasualLeavedInThisMonth == 0) {
				casualLeaveMonthlyClosingBalance += (casualLeaveMonthlyOpeningBalance + earnedLeaves);
			}
			double tEarnedLeaves = earnedLeaves;
			if (notAvailInCurrentMonth && takenCasualLeavedInThisMonth > 0) {
				casualLeaveMonthlyClosingBalance += earnedLeaves;
			}
			if (notAvailInCurrentMonth) {
				earnedLeaves = 0.0;
			}
			boolean isTakenLeaves = false;
			if (takenCasualLeavedInThisMonth > 0) {
				isTakenLeaves = true;
			}

			if (isPayable) {
				double tempBalanceLeavesExtra = 0.0;
				if (isEWDAdjustAgainstLeave && totalExtraWorkingDaysInMonth > 0.0 && takenCasualLeavedInThisMonth > 0.0) {
					tempBalanceLeavesExtra = totalExtraWorkingDaysInMonth - takenCasualLeavedInThisMonth;
					if (tempBalanceLeavesExtra < 0) {
						tempBalanceLeavesExtra = tempBalanceLeavesExtra * (-1);
						paybleLeave += totalExtraWorkingDaysInMonth;
						totalExtraWorkingDaysInMonth = 0.0;
						totalExtraWorkingDayNetBalance = 0.0;
						takenCasualLeavedInThisMonth = tempBalanceLeavesExtra;
					} else {
						paybleLeave += takenCasualLeavedInThisMonth;
						totalExtraWorkingDaysInMonth = tempBalanceLeavesExtra;
						totalExtraWorkingDayNetBalance = tempBalanceLeavesExtra;
						takenCasualLeavedInThisMonth = 0.0;
						casualLeaveMonthlyClosingBalance += earnedLeaves + casualLeaveMonthlyOpeningBalance;
					}
				}

				if (takenCasualLeavedInThisMonth > 0) {
					double totalBalanceLeaves = earnedLeaves + casualLeaveMonthlyOpeningBalance;
					int totalBalanceLeavesINTEGER = (int) totalBalanceLeaves;
					int takenEarnLeavesINTEGER = (int) takenCasualLeavedInThisMonth;
					double takenEarnLeavesDOUBLE = takenCasualLeavedInThisMonth - takenEarnLeavesINTEGER;

					if (takenCasualLeavedInThisMonth <= totalBalanceLeaves) {
						paybleLeave += takenEarnLeavesINTEGER;
						totalBalanceLeavesINTEGER -= takenEarnLeavesINTEGER;
						totalBalanceLeaves -= takenEarnLeavesINTEGER;
						if (totalBalanceLeaves >= 0.50 && takenEarnLeavesDOUBLE >= 0.50) {
							paybleLeave += 0.50;
							totalBalanceLeaves -= 0.50;
						}
						casualLeaveMonthlyClosingBalance += totalBalanceLeaves;

					} else {
						paybleLeave += totalBalanceLeavesINTEGER;
						totalBalanceLeaves -= totalBalanceLeavesINTEGER;
						takenCasualLeavedInThisMonth -= totalBalanceLeavesINTEGER;
						totalBalanceLeavesINTEGER = 0;
						if (totalBalanceLeaves >= 0.50 && takenCasualLeavedInThisMonth >= 0.50) {
							paybleLeave += 0.50;
							totalBalanceLeaves -= 0.50;
						}
						casualLeaveMonthlyClosingBalance += totalBalanceLeaves;
					}
				}

				if (isAbsentAdjustWithLeaves && totalAbsentInMonth > 0.0 && ((int) casualLeaveMonthlyClosingBalance) > 0) {
					if (totalAbsentInMonth < casualLeaveMonthlyClosingBalance) {
						paybleAbsent += totalAbsentInMonth;
						nonPaybleAbsent = 0.0;
						casualLeaveMonthlyClosingBalance -= totalAbsentInMonth;
						totalAbsentInMonth = 0.0;
					} else {
						paybleAbsent += ((int) casualLeaveMonthlyClosingBalance);
						nonPaybleAbsent += totalAbsentInMonth - casualLeaveMonthlyClosingBalance;
						casualLeaveMonthlyClosingBalance = 0;
						totalAbsentInMonth -= casualLeaveMonthlyClosingBalance;
					}
				} else if (isAbsentAdjustWithLeaves && totalAbsentInMonth > 0.0 && casualLeaveMonthlyClosingBalance >= 0.50) {
					double tempBalance = 0.50;
					casualLeaveMonthlyClosingBalance -= tempBalance;
					if (totalAbsentInMonth < tempBalance) {
						paybleAbsent += totalAbsentInMonth;
						nonPaybleAbsent = 0.0;
						tempBalance -= totalAbsentInMonth;
						totalAbsentInMonth = 0.0;
					} else {
						paybleAbsent += tempBalance;
						nonPaybleAbsent += (totalAbsentInMonth - tempBalance);
						totalAbsentInMonth -= tempBalance;
					}
				}

				if ((!isTakenLeaves && !isAbsentAdjustWithLeaves) || (!isTakenLeaves && isAbsentAdjustWithLeaves && totalAbsentInMonth == 0.0 && paybleAbsent == 0.0)) {
					casualLeaveMonthlyClosingBalance = casualLeaveMonthlyOpeningBalance;
					casualLeaveMonthlyClosingBalance += tEarnedLeaves;
					netBalanceLeaves[0] = tEarnedLeaves;
					updateELNetBalance(employeeMonthlyAttendanceId, tEarnedLeaves);
				} else if (casualLeaveMonthlyClosingBalance >= casualLeaveMonthlyOpeningBalance) {
					double tempLeaveBalance = casualLeaveMonthlyClosingBalance - casualLeaveMonthlyOpeningBalance;
					if (notAvailInCurrentMonth) {
						tempLeaveBalance -= tEarnedLeaves;
					}
					tEarnedLeaves = tempLeaveBalance;
					netBalanceLeaves[0] = tEarnedLeaves;
					updateCLNetBalance(employeeMonthlyAttendanceId, tEarnedLeaves);
				} else {
					double tempLeaveBalance = casualLeaveMonthlyOpeningBalance - casualLeaveMonthlyClosingBalance;
					if (notAvailInCurrentMonth) {
						tempLeaveBalance += tEarnedLeaves;
					}
					if (tEarnedLeaves > tempLeaveBalance) {
						tEarnedLeaves = tEarnedLeaves - tempLeaveBalance;
					} else {
						tEarnedLeaves = 0.0;
					}
					netBalanceLeaves[0] = tEarnedLeaves;
					updateCLNetBalance(employeeMonthlyAttendanceId, tEarnedLeaves);
				}
			} else {
				if (isEWDAdjustAgainstLeave) {
					if ((totalExtraWorkingDaysInMonth - takenMedicalLeavedInThisMonth) > 0) {
						totalExtraWorkingDaysInMonth -= takenMedicalLeavedInThisMonth;
					} else {
						totalExtraWorkingDaysInMonth = 0.0;
					}
				}
			}
		} else if (leaveCreditId == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {

		}
	}

	// private void updateEmployeeSpecialLeaveBalance(Object employeeId, double specialLeaveMonthlyClosingBalance) throws JSONException {
	// JSONObject updates = new JSONObject();
	// updates.put(Data.Query.RESOURCE, "hris_employees");
	// JSONObject row = new JSONObject();
	// row.put("__key__", employeeId);
	// row.put("specialleavebalance", specialLeaveMonthlyClosingBalance);
	// updates.put(Data.Update.UPDATES, row);
	// ResourceEngine.update(updates);
	// }

	public static void updateMonthlyEarnML(Object employeeMonthlyAttendanceId, double casualLeave) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("earnmedicalleave", casualLeave);
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateMLNetBalance(Object employeeMonthlyAttendanceId, double leaveNetBalance) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("medicalleavenetbalance", leaveNetBalance);
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static void updateMonthlyEarnCL(Object employeeMonthlyAttendanceId, double casualLeave) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("earncasualleave", casualLeave);
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateCLNetBalance(Object employeeMonthlyAttendanceId, double leaveNetBalance) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("casualleavenetbalance", leaveNetBalance);
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateNonPayableAbsents(Object employeeMonthlyAttendanceId, double nonPayabelAbsents) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("nonpaidabsents", nonPayabelAbsents);
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static void updateMonthlyPayableAbsents(Object employeeMonthlyAttendanceId, double payableAbsents) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("paidabsents", payableAbsents);
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static void updateMonthlyEarnEL(Object employeeMonthlyAttendanceId, double earnLeave) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("earnearnedleave", earnLeave);
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	// private void updateEmployeeExtraWorkingDayCount(Object employeeId, double extraWorkingDayBalance) throws JSONException {
	// JSONObject updates = new JSONObject();
	// updates.put(Data.Query.RESOURCE, "hris_employees");
	// JSONObject row = new JSONObject();
	// row.put("__key__", employeeId);
	// row.put("extraworkingdaybalance", extraWorkingDayBalance);
	// row.put("temp", "Updating From BL Hence Ignore JOB's Process");
	// updates.put(Data.Update.UPDATES, row);
	// ResourceEngine.update(updates);
	// }

	private void updateEmployeeLeaveBalance(Object employeeId, double extraWorkingDayBalance) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeId);
		row.put("balanceleaves", extraWorkingDayBalance);
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateELNetBalance(Object employeeMonthlyAttendanceId, double leaveNetBalance) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeMonthlyAttendanceId);
		row.put("earnedleavenetbalance", leaveNetBalance);
		row.put("temp", "Updating From BL Hence Ignore JOB's Process");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}
}
