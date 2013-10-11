package com.applane.resourceoriented.hris;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeSalaryGenerationServlet extends HttpServlet implements ApplaneTask {

	private static final long serialVersionUID = 1L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			// instantiateClassVariablesAndGenerateSalary(req, resp);
		} catch (Exception e) {
			sendMailInCaseOfException(e);
		}
	}

	private static void setMonthlyAttendanceMap(HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns> monthlyAttendanceMap, Object employeeId, JSONObject fields) throws Exception {
		JSONArray monthlyattendanceArray = getEmployeeMonthlyAttendanceRecords(employeeId, fields);
		for (int counter = 0; counter < monthlyattendanceArray.length(); counter++) {
			if (employeeId != null) {
				monthlyAttendanceMap.put(Integer.parseInt("" + employeeId), instantiateMonthlyAttandanceObject(monthlyattendanceArray.getJSONObject(counter)));
			}
		}
	}

	private static void sendMailInCaseOfException(Exception e) {
		String traces = ExceptionUtils.getExceptionTraceMessage("EmployeeSalaryGenerationServlet Exception >> ", e);
		LogUtility.writeError(traces);
		ApplaneMail mail = new ApplaneMail();
		StringBuilder builder = new StringBuilder();
		builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
		builder.append("<br><br><b>Exception traces are given below :</b><br></br><hr></hr><br></br>").append(traces);
		mail.setMessage("Employee Salary Servlet Task Queue Failed", builder.toString(), true);
		try {
			mail.setTo("kapil.dalal@daffodilsw.com");
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception Come while send mail in Employee Salary Servlet.");
		}
	}

	// private void instantiateClassVariablesAndGenerateSalary(HttpServletRequest req, HttpServletResponse resp) throws Exception {
	// @SuppressWarnings("unchecked")
	// Map<String, Object> paramMap = req.getParameterMap();
	// if (paramMap == null || paramMap.size() == 0) {
	// resp.getWriter().write("No parameter found in Attendance servlet..");
	// sendMailIfAnyValueNull(paramMap);
	// } else if (paramMap.get("branchid") == null || paramMap.get("monthid") == null || paramMap.get("yearid") == null) {
	// sendMailIfAnyValueNull(paramMap);
	// } else {
	// String branchIdInstring = ((String[]) paramMap.get("branchid"))[0];
	// String monthIdInString = ((String[]) paramMap.get("monthid"))[0];
	// String yearIdInString = ((String[]) paramMap.get("yearid"))[0];
	// if (branchIdInstring != null && branchIdInstring.length() > 0) {
	// branchId = Long.parseLong(branchIdInstring);
	// }
	//
	// if (monthIdInString != null && monthIdInString.length() > 0) {
	// monthId = Long.parseLong(monthIdInString);
	// }
	//
	// if (yearIdInString != null && yearIdInString.length() > 0) {
	// yearId = Long.parseLong(yearIdInString);
	// }
	// intMoreVariables();
	// initializeVariablesAndGenerateSalary();
	// }
	// }

	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		try {
			JSONObject taskInfo = applaneTaskInfo.getTaskInfo();

			Object takeSalarySheetKey = taskInfo.opt("takeSalarySheetKey");
			int monthId = Translator.integerValue(taskInfo.opt("monthid"));
			int yearId = Translator.integerValue(taskInfo.opt("yearid"));
			int branchId = Translator.integerValue(taskInfo.opt("branchid"));
			Object __key__ = taskInfo.opt(Updates.KEY);

			String monthName = getMonthName(monthId);
			String yearName = getYearName(yearId);
			Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
			Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
			String monthFirstDateInString = getDateInString(monthFirstDate);
			String monthLastDateInString = getDateInString(monthLastDate);
			EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = null;
			HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns> monthlyAttendanceMap = new HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns>();
			JSONObject fields = new JSONObject();
			fields.put("takeSalarySheetKey", takeSalarySheetKey);
			fields.put("branchId", branchId);
			fields.put("monthId", monthId);
			fields.put("yearId", yearId);
			fields.put("monthName", monthName);
			fields.put("yearName", yearName);
			fields.put("monthFirstDate", monthFirstDate);
			fields.put("monthLastDate", monthLastDate);
			fields.put("monthFirstDateInString", monthFirstDateInString);
			fields.put("monthLastDateInString", monthLastDateInString);
			fields.put("monthlyAttendanceMap", monthlyAttendanceMap);
			fields.put("employeeMonthlyAttandance", employeeMonthlyAttandance);

			initializeVariablesAndGenerateSalary(__key__, fields);
		} catch (Exception e) {
			try {
				EmployeeReleasingSalaryServlet.sendStatus(new JSONObject().put("Error -->", e.toString()), "Error While Salary Genetating Salary (Employee List) on initiate method");
			} catch (Exception jsonException) {
			}
		}
	}

	public void generateSalaryInvokeMethod(Object[] keys) {
		try {
			String selectedKeys = "";
			for (int counter = 0; counter < keys.length; counter++) {
				if (selectedKeys.length() == 0) {
					selectedKeys += keys[counter];
				} else {
					selectedKeys += ("," + keys[counter]);
				}
			}
			if (selectedKeys.length() > 0) {
				JSONArray salarySheetArray = getEmployeeIdFromSalarySheet(selectedKeys);
				{
					if (salarySheetArray != null && salarySheetArray.length() > 0) {
						for (int i = 0; i < salarySheetArray.length(); i++) {
							JSONObject salarySheetObject = salarySheetArray.getJSONObject(i);
							JSONArray employeeArray = getEmployeeRecordsForSalarySheetReGenerate(Translator.integerValue(salarySheetObject.opt("employeeid")));
							if (employeeArray != null && employeeArray.length() > 0) {
								int branchId = Translator.integerValue(employeeArray.getJSONObject(0).opt("branchid"));
								int yearId = Translator.integerValue(salarySheetObject.opt("yearid"));
								int monthId = Translator.integerValue(salarySheetObject.opt("monthid"));

								boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.SALARY);
								if (isFreezed) {
									throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
								}

								String monthName = getMonthName(monthId);
								String yearName = getYearName(yearId);
								Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
								Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
								String monthFirstDateInString = getDateInString(monthFirstDate);
								String monthLastDateInString = getDateInString(monthLastDate);
								double maxDaysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
								EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = null;
								HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns> monthlyAttendanceMap = new HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns>();
								JSONObject fields = new JSONObject();
								fields.put("branchId", branchId);
								fields.put("monthId", monthId);
								fields.put("yearId", yearId);
								fields.put("monthName", monthName);
								fields.put("yearName", yearName);
								fields.put("monthFirstDate", monthFirstDate);
								fields.put("monthLastDate", monthLastDate);
								fields.put("monthFirstDateInString", monthFirstDateInString);
								fields.put("monthLastDateInString", monthLastDateInString);
								fields.put("monthlyAttendanceMap", monthlyAttendanceMap);
								fields.put("employeeMonthlyAttandance", employeeMonthlyAttandance);
								fields.put("maxDaysInMonth", maxDaysInMonth);

								populateSalaryComponents(employeeArray, fields);
							}
						}
					}
				}
			}
		} catch (BusinessLogicException e) {
			// String exception = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			throw new BusinessLogicException(e.getMessage());
		} catch (Exception e) {
			String exception = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("Salary Generation Exception  >> : [" + exception + "]");
			sendMailInCaseOfException(e);
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public void generateSalaryInvokeMethodFromComponentAmount(Object[] keys) {
		try {
			String selectedKeys = "";
			for (int counter = 0; counter < keys.length; counter++) {
				if (selectedKeys.length() == 0) {
					selectedKeys += keys[counter];
				} else {
					selectedKeys += ("," + keys[counter]);
				}
			}
			if (selectedKeys.length() > 0) {
				JSONArray salarySheetArray = getComponentMonthlyAmountRecord(selectedKeys);
				{
					if (salarySheetArray != null && salarySheetArray.length() > 0) {
						for (int i = 0; i < salarySheetArray.length(); i++) {
							JSONObject salarySheetObject = salarySheetArray.getJSONObject(i);
							JSONArray employeeArray = getEmployeeRecordsForSalarySheetReGenerate(Translator.integerValue(salarySheetObject.opt("employeeid")));
							if (employeeArray != null && employeeArray.length() > 0) {
								int branchId = Translator.integerValue(employeeArray.getJSONObject(0).opt("branchid"));
								int yearId = Translator.integerValue(salarySheetObject.opt("yearid"));
								int monthId = Translator.integerValue(salarySheetObject.opt("monthid"));

								boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.SALARY);
								if (isFreezed) {
									throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
								}

								String monthName = getMonthName(monthId);
								String yearName = getYearName(yearId);
								Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
								Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
								String monthFirstDateInString = getDateInString(monthFirstDate);
								String monthLastDateInString = getDateInString(monthLastDate);
								double maxDaysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
								EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = null;
								HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns> monthlyAttendanceMap = new HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns>();
								JSONObject fields = new JSONObject();
								fields.put("branchId", branchId);
								fields.put("monthId", monthId);
								fields.put("yearId", yearId);
								fields.put("monthName", monthName);
								fields.put("yearName", yearName);
								fields.put("monthFirstDate", monthFirstDate);
								fields.put("monthLastDate", monthLastDate);
								fields.put("monthFirstDateInString", monthFirstDateInString);
								fields.put("monthLastDateInString", monthLastDateInString);
								fields.put("monthlyAttendanceMap", monthlyAttendanceMap);
								fields.put("employeeMonthlyAttandance", employeeMonthlyAttandance);
								fields.put("maxDaysInMonth", maxDaysInMonth);

								populateSalaryComponents(employeeArray, fields);
							}
						}
					}
				}
			}
		} catch (BusinessLogicException e) {
			// String exception = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			throw new BusinessLogicException(e.getMessage());
		} catch (Exception e) {
			LogUtility.writeLog("Salary Generation Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			sendMailInCaseOfException(e);
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public JSONArray getEmployeeIdFromSalarySheet(String keys) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid.employeecode");
		columnArray.put("employeeid.name");

		columnArray.put(new JSONObject().put("expression", "employeeid.businessfunctionid.__key__").put("alias", "employeeid.businessfunctionid"));
		columnArray.put(new JSONObject().put("expression", "employeeid.profitcenterid.__key__").put("alias", "employeeid.profitcenterid"));

		columnArray.put("employeeid");
		columnArray.put("employeeid.branchid");
		columnArray.put("monthid");
		columnArray.put("yearid");
		columnArray.put("branchid");
		columnArray.put("sub_branchid");
		columnArray.put("employeeid.accountno");
		columnArray.put("employeeid.modeofsalaryid");

		// columnArray.put("employeeid:['','']");
		// JSONObject employeeR = new JSONObject();
		// employeeR.put("employeeid", new JSONArray().put("name"));

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + keys + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("salarysheet");
		return salarySheetArray;

	}

	private void initializeVariablesAndGenerateSalary(Object __key__, JSONObject fields) throws Exception {
		generateSalarySheet(__key__, fields);
	}

	private static EmployeeMonthlyAttandanceUpdateAllColumns instantiateMonthlyAttandanceObject(JSONObject monthlyAttendanceObject) {
		double earnedLeaveOpeningBalance = ((Number) (monthlyAttendanceObject.opt("earnedleaveopeningbalance") == null ? 0.0 : monthlyAttendanceObject.opt("earnedleaveopeningbalance"))).doubleValue();
		double casualLeaveOpeningBalance = ((Number) (monthlyAttendanceObject.opt("casualleaveopeningbalance") == null ? 0.0 : monthlyAttendanceObject.opt("casualleaveopeningbalance"))).doubleValue();
		double medicalLeaveOpeningBalance = ((Number) (monthlyAttendanceObject.opt("medicalleaveopeningbalance") == null ? 0.0 : monthlyAttendanceObject.opt("medicalleaveopeningbalance"))).doubleValue();
		double specialLeaveOpeningBalance = ((Number) (monthlyAttendanceObject.opt("specialleaveopeningbalance") == null ? 0.0 : monthlyAttendanceObject.opt("specialleaveopeningbalance"))).doubleValue();
		double extraWorkingDaysOpeningBalance = ((Number) (monthlyAttendanceObject.opt("extraworkingdayopeningbalance") == null ? 0.0 : monthlyAttendanceObject.opt("extraworkingdayopeningbalance"))).doubleValue();
		double presentDays = ((Number) (monthlyAttendanceObject.opt("presentdays") == null ? 0.0 : monthlyAttendanceObject.opt("presentdays"))).doubleValue();
		double absents = ((Number) (monthlyAttendanceObject.opt("absents") == null ? 0.0 : monthlyAttendanceObject.opt("absents"))).doubleValue();
		double totalTakenLeaves = ((Number) (monthlyAttendanceObject.opt("leaves") == null ? 0.0 : monthlyAttendanceObject.opt("leaves"))).doubleValue();
		double usedEarnedLeave = ((Number) (monthlyAttendanceObject.opt("usedearnedleaves") == null ? 0.0 : monthlyAttendanceObject.opt("usedearnedleaves"))).doubleValue();
		double usedCasualLeave = ((Number) (monthlyAttendanceObject.opt("usedcasualleaves") == null ? 0.0 : monthlyAttendanceObject.opt("usedcasualleaves"))).doubleValue();
		double usedMedicalLeave = ((Number) (monthlyAttendanceObject.opt("usedmedicalleaves") == null ? 0.0 : monthlyAttendanceObject.opt("usedmedicalleaves"))).doubleValue();
		double usedSpecialLeave = ((Number) (monthlyAttendanceObject.opt("usedspecialleaves") == null ? 0.0 : monthlyAttendanceObject.opt("usedspecialleaves"))).doubleValue();
		double extraWorkingDays = ((Number) (monthlyAttendanceObject.opt("extraworkingdays") == null ? 0.0 : monthlyAttendanceObject.opt("extraworkingdays"))).doubleValue();
		double carryextraworkingday = ((Number) (monthlyAttendanceObject.opt("carryextraworkingday") == null ? 0.0 : monthlyAttendanceObject.opt("carryextraworkingday"))).doubleValue();
		double actualnonworkingdays = ((Number) (monthlyAttendanceObject.opt("actualnonworkingdays") == null ? 0.0 : monthlyAttendanceObject.opt("actualnonworkingdays"))).doubleValue();
		double totalWorkingDays = ((Number) (monthlyAttendanceObject.opt("totalworkingdays") == null ? 0.0 : monthlyAttendanceObject.opt("totalworkingdays"))).doubleValue();
		double creditEarnedLeaves = ((Number) (monthlyAttendanceObject.opt("creditearnedleaves") == null ? 0.0 : monthlyAttendanceObject.opt("creditearnedleaves"))).doubleValue();
		double creditCasualLeave = ((Number) (monthlyAttendanceObject.opt("creditcasualleaves") == null ? 0.0 : monthlyAttendanceObject.opt("creditcasualleaves"))).doubleValue();
		double creditMedicalLeave = ((Number) (monthlyAttendanceObject.opt("creditmedicalleaves") == null ? 0.0 : monthlyAttendanceObject.opt("creditmedicalleaves"))).doubleValue();
		double payableleaves = ((Number) (monthlyAttendanceObject.opt("payableleaves") == null ? 0.0 : monthlyAttendanceObject.opt("payableleaves"))).doubleValue();
		double paidAbsent = ((Number) (monthlyAttendanceObject.opt("paidabsents") == null ? 0.0 : monthlyAttendanceObject.opt("paidabsents"))).doubleValue();
		boolean isEWDEnCashable = Translator.booleanValue(monthlyAttendanceObject.opt("employeeid.leavepolicyid.isextraworkingdayencashable"));
		double nonpayableleaves = ((Number) (monthlyAttendanceObject.opt("nonpayableleaves") == null ? 0.0 : monthlyAttendanceObject.opt("nonpayableleaves"))).doubleValue();
		double nonpaidabsents = ((Number) (monthlyAttendanceObject.opt("nonpaidabsents") == null ? 0.0 : monthlyAttendanceObject.opt("nonpaidabsents"))).doubleValue();

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
		employeeMonthlyAttandance.setTotalAbsentInMonth(absents);
		employeeMonthlyAttandance.setTotalCreditCasualLeavesInMonth(creditCasualLeave);
		employeeMonthlyAttandance.setTotalCreditEarnedLeaveInMonth(creditEarnedLeaves);
		employeeMonthlyAttandance.setTotalCreditMedicalLeaveInMonth(creditMedicalLeave);
		employeeMonthlyAttandance.setTotalExtraWorkingDaysInMonth(extraWorkingDays);
		employeeMonthlyAttandance.setActualExtraWorkingDaysInMonth(carryextraworkingday);
		employeeMonthlyAttandance.setTotalLeavesInMonth(totalTakenLeaves);
		employeeMonthlyAttandance.setTotalPresentsInMonth(presentDays);
		employeeMonthlyAttandance.setTotalWorkingDaysInMonth(totalWorkingDays);
		employeeMonthlyAttandance.setActualNonWorkingDaysInMonth(actualnonworkingdays);
		employeeMonthlyAttandance.setPaybleLeave(payableleaves);
		employeeMonthlyAttandance.setPaybleAbsent(paidAbsent);
		employeeMonthlyAttandance.setIsExtraWorkingDayEncashable(isEWDEnCashable);
		employeeMonthlyAttandance.setNonPaybleLeave(nonpayableleaves);
		employeeMonthlyAttandance.setNonPaybleAbsent(nonpaidabsents);
		return employeeMonthlyAttandance;
	}

	public static void generateSalarySheet(Object __key__, JSONObject fields) throws Exception {
		JSONArray employeeArray = getActiveAndReleivedEmployeeRecords(__key__, fields);
		// LogUtility.writeLog("method >> generateSalarySheet >> employeeArray >> " + employeeArray);
		if (employeeArray.length() > 0) {
			Object employeeId = populateSalaryComponents(employeeArray, fields);
			initiateTaskQueue(fields, employeeId);
		} else {
			boolean updateIsGenerating = false;
			PopulateEmployeeSalaryBusinessLogic.updateGeneratingStatus(fields.opt("takeSalarySheetKey"), updateIsGenerating);
			sendMailOnCompletion();
		}
	}

	private static void sendMailOnCompletion() {
		String title = "Salary Generation Process Completed";
		String mailValue = "Dear: " + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		mailValue += "<BR /><BR />";
		mailValue += "Salary Generation Process Completed";
		mailValue += "<BR /><BR />";
		mailValue += "Regards";
		mailValue += "<BR />";
		mailValue += "Applane";
		try {
			PunchingUtility.sendMailsWithoutCC(new String[] { "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL) }, mailValue, title);
		} catch (Exception e) {
			LogUtility.writeLog("Employee Generation Salary completion mail failed");
		}
	}

	private static void initiateTaskQueue(JSONObject fields, Object employeeId) throws JSONException {

		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("monthid", fields.opt("monthId"));
		taskQueueParameters.put("yearid", fields.opt("yearId"));
		taskQueueParameters.put("branchid", fields.opt("branchId"));
		taskQueueParameters.put("takeSalarySheetKey", fields.opt("takeSalarySheetKey"));
		taskQueueParameters.put(Updates.KEY, employeeId);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet", QueueConstantsHRIS.SALARY_PROCESS, taskQueueParameters);

	}

	public static Object populateSalaryComponents(JSONArray employeeArray, JSONObject fields) throws JSONException, Exception {
		String exception = "";
		Object employeeId = null;
		int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		for (int counter = 0; counter < employeeArray.length(); counter++) {
			try {
				JSONObject employeeRecord = employeeArray.getJSONObject(counter);
				employeeId = employeeRecord.opt("__key__");
				Object joiningDateObject = employeeRecord.opt("joiningdate");
				Object relievingDateObject = employeeRecord.opt("relievingdate");

				Object noticePeriodStartDateObject = employeeRecord.opt("noticeperiodstartdate");
				Object noticePeriodEndDateObject = employeeRecord.opt("noticeperiodenddate");
				Date noticePeriodStartDate = null;
				Date noticePeriodEndDate = null;
				boolean isGenerateSalary = true;
				if (organizationId != 7783 && organizationId != 7043) { // Navigant / Dapine
					noticePeriodStartDateObject = null;
					noticePeriodEndDateObject = null;
				} else {
					if (noticePeriodStartDateObject != null && noticePeriodEndDateObject != null) {
						noticePeriodStartDate = Translator.dateValue(noticePeriodStartDateObject);
						noticePeriodEndDate = Translator.dateValue(noticePeriodEndDateObject);
					}
				}
				Date joiningDate = null;
				Date fromDate = (Date) fields.opt("monthFirstDate");
				Date lastDate = (Date) fields.opt("monthLastDate");
				String fromDateStr = fields.optString("monthFirstDateInString");
				String lastDateStr = fields.optString("monthLastDateInString");
				boolean isNewJoinee = false;
				if (joiningDateObject != null) {
					joiningDate = Translator.dateValue(joiningDateObject);
					if (joiningDate != null && isNewJoineeOrReleaved(joiningDate, fields)) {
						fromDateStr = getDateInString(joiningDate);
						fields.put("isJoining", true);
						fields.put("joiningDate", joiningDate);
						fields.put("joiningDateString", fromDateStr);
					}
				}
				JSONArray employeeAdditionalInformation = null;
				double salaryOnHoldDays = 0;
				if (isNewJoinee) {
					employeeAdditionalInformation = getEmployeeAdditionalInformation(employeeId);
					if (employeeAdditionalInformation != null && employeeAdditionalInformation.length() > 0) {
						salaryOnHoldDays = Translator.integerValue(employeeAdditionalInformation.getJSONObject(0).opt("solary_on_hold_days"));
					}
				}
				Date relievingDate = null;
				if (relievingDateObject != null) {
					relievingDate = DataTypeUtilities.checkDateFormat(relievingDateObject);
					if (relievingDate != null && isNewJoineeOrReleaved(relievingDate, fields)) {
						lastDateStr = getDateInString(relievingDate);
						fields.put("isRelieving", true);
					}
				}

				long maxDaysInMonth = DataTypeUtilities.differenceBetweenDates(DataTypeUtilities.checkDateFormat(fromDateStr), DataTypeUtilities.checkDateFormat(lastDateStr));
				long maxDaysInMonthTotal = DataTypeUtilities.getNumberOfDaysInMonth(DataTypeUtilities.checkDateFormat(fromDateStr));
				fields.put("maxDaysInMonth", maxDaysInMonth + 1);

				fields.put("maxDaysInMonth", maxDaysInMonth + 1);
				fields.put("maxDaysInMonthTotal", maxDaysInMonthTotal);

				JSONArray employeeSalaryComponentArray = getEmployeeSalaryComponents(fromDateStr, lastDateStr, employeeId); // globle employee id used
				if (employeeSalaryComponentArray.length() > 0) {

					HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns> monthlyAttendanceMap = new HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns>();
					setMonthlyAttendanceMap(monthlyAttendanceMap, employeeId, fields);
					fields.put("monthlyAttendanceMap", monthlyAttendanceMap);
					EmployeeMonthlyAttandanceUpdateAllColumns eMAUAC = monthlyAttendanceMap.get(employeeId);
					double totalLeaves = 0.0;
					double actualNonPayableLeaves = 0.0;
					if (monthlyAttendanceMap != null && monthlyAttendanceMap.containsKey(employeeId)) {
						totalLeaves = eMAUAC.getTotalLeavesInMonth();
						actualNonPayableLeaves = eMAUAC.getNonPaybleLeave();
					}

					boolean increment = false;

					fields.put("increment", increment);
					deleteOldCalculatedComponents(employeeId, fields);
					ArrayList<Integer> componentList = new ArrayList<Integer>();
					JSONObject attendanceDetails1 = new JSONObject();
					JSONObject attendanceDetails2 = new JSONObject();
					double totalLeaves1 = 0.0;
					double totalLeaves2 = 0.0;
					int shortLeaves = 0;
					for (int salaryComponentCounter = 0; salaryComponentCounter < employeeSalaryComponentArray.length(); salaryComponentCounter++) {
						int salaryComponentKey = employeeSalaryComponentArray.getJSONObject(salaryComponentCounter).optInt("salarycomponentid");
						int key = employeeSalaryComponentArray.getJSONObject(salaryComponentCounter).optInt(Updates.KEY);
						int componentTypeId = employeeSalaryComponentArray.getJSONObject(salaryComponentCounter).optInt("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID);
						if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.BASIC) {
							Date applicableFromDate = Translator.dateValue(employeeSalaryComponentArray.getJSONObject(salaryComponentCounter).opt("applicablefrom"));
							String applicableFromDateStr = "" + employeeSalaryComponentArray.getJSONObject(salaryComponentCounter).opt("applicablefrom");
							Date toDate = Translator.dateValue(employeeSalaryComponentArray.getJSONObject(salaryComponentCounter).opt("applicableto"));
							String applicableTillDateStr = "" + employeeSalaryComponentArray.getJSONObject(salaryComponentCounter).opt("applicableto");
							String fromDateStrTemp = fromDateStr;
							String lastDateStrTemp = lastDateStr;
							if (applicableFromDate.after(fromDate)) {
								fromDateStrTemp = applicableFromDateStr;
							}
							if (toDate.before(lastDate)) {
								lastDateStrTemp = applicableTillDateStr;
							}
							String filter = "employeeid = " + employeeId + " and attendancedate >= '" + fromDateStrTemp + "' and attendancedate <= '" + lastDateStrTemp + "'";
							JSONArray employeeAttendanceArray = new EmployeeSalarySheetReport().getEmployeeAttendanceArray(filter);
							double present = 0.0;
							double absent = 0.0;
							double leaves = 0.0;
							double nonPayableLeaves = 0.0;
							double actualNonWorkingDays = 0.0;
							double ewd = 0.0;
							for (int attendanceCounter = 0; attendanceCounter < employeeAttendanceArray.length(); attendanceCounter++) {
								Date attendanceDate = Translator.dateValue(employeeAttendanceArray.getJSONObject(attendanceCounter).opt("attendancedate"));
								int attendanceTypeId = employeeAttendanceArray.getJSONObject(attendanceCounter).optInt("attendancetypeid");
								int paidStatusId = employeeAttendanceArray.getJSONObject(attendanceCounter).optInt("paidstatus");
								int shortLeaveId = employeeAttendanceArray.getJSONObject(attendanceCounter).optInt("attendance_type_short_leave_id");
								if (noticePeriodStartDate != null && noticePeriodEndDate != null && ((attendanceDate.equals(noticePeriodStartDate) || attendanceDate.equals(noticePeriodEndDate)) || (attendanceDate.after(noticePeriodStartDate) && attendanceDate.before(noticePeriodEndDate)))
										&& (attendanceTypeId != HRISApplicationConstants.ATTENDANCE_PRESENT && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_TOUR && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_OFF && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_HOLIDAY && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_UNKNOWN && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_HALF_DAY_OFF)) {
									isGenerateSalary = false;
								}
								if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
									present += 1;
									if (shortLeaveId == HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE) {
										shortLeaves++;
										if ((shortLeaves % 3) == 0) {
											present = present - 0.50;
											leaves += 0.50;
										}
									}
								}
								if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
									absent++;
								}
								if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
									leaves++;
								}
								if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
									leaves += 0.50;
									present += 0.50;
								}
								if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE && paidStatusId == HRISApplicationConstants.LEAVE_EWD_UN_PAID) {
									nonPayableLeaves++;
								}
								if ((attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE) && paidStatusId == HRISApplicationConstants.LEAVE_EWD_UN_PAID) {
									nonPayableLeaves += 0.50;
								}
								if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_OFF || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HOLIDAY) {
									actualNonWorkingDays++;
								}
								if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY && paidStatusId == HRISApplicationConstants.LEAVE_EWD_PAID) {
									ewd++;
								}
								if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF && paidStatusId == HRISApplicationConstants.LEAVE_EWD_PAID) {
									ewd += 0.50;
								}
							}
							if (attendanceDetails1 == null || attendanceDetails1.length() == 0) {
								totalLeaves1 = leaves;
								attendanceDetails1.put("present", present);
								attendanceDetails1.put("leaves", leaves);
								attendanceDetails1.put("actualNonWorkingDays", actualNonWorkingDays);
								attendanceDetails1.put("ewd", ewd);
								attendanceDetails1.put("nonPayableLeaves", nonPayableLeaves);
								attendanceDetails1.put("absent", absent);
								attendanceDetails1.put("toDate", toDate);
								attendanceDetails1.put("key", key);
							} else {
								totalLeaves2 = leaves;
								attendanceDetails2.put("present", present);
								attendanceDetails2.put("leaves", leaves);
								attendanceDetails2.put("actualNonWorkingDays", actualNonWorkingDays);
								attendanceDetails2.put("ewd", ewd);
								attendanceDetails2.put("nonPayableLeaves", nonPayableLeaves);
								attendanceDetails2.put("absent", absent);
								attendanceDetails2.put("toDate", toDate);
								attendanceDetails2.put("key", key);
							}
						}
						if (componentList.contains(salaryComponentKey)) {
							increment = true;
							fields.put("increment", increment);
						}
						componentList.add(salaryComponentKey);
					}
					if (Translator.booleanValue(fields.opt("increment"))) {
						Date toDate1 = attendanceDetails1.opt("toDate") == null ? null : (Date) attendanceDetails1.opt("toDate");
						Date toDate2 = attendanceDetails2.opt("toDate") == null ? null : (Date) attendanceDetails2.opt("toDate");
						if (toDate1 != null && toDate2 != null) {
							if (toDate1.before(toDate2)) {
								attendanceDetails1.put("leaves", totalLeaves1);
								attendanceDetails2.put("leaves", totalLeaves2);
								if (totalLeaves2 >= actualNonPayableLeaves) {
									attendanceDetails1.put("nonPayableLeaves", 0.0);
									attendanceDetails2.put("nonPayableLeaves", actualNonPayableLeaves);
								} else {
									attendanceDetails2.put("nonPayableLeaves", totalLeaves2);
									attendanceDetails1.put("nonPayableLeaves", actualNonPayableLeaves - totalLeaves2);
								}
								fields.put("1", attendanceDetails1);
								fields.put("2", attendanceDetails2);
							} else {
								attendanceDetails1.put("leaves", totalLeaves2);
								attendanceDetails2.put("leaves", totalLeaves1);
								if (totalLeaves1 >= actualNonPayableLeaves) {
									attendanceDetails2.put("nonPayableLeaves", actualNonPayableLeaves);
									attendanceDetails1.put("nonPayableLeaves", 0.0);
								} else {
									attendanceDetails2.put("nonPayableLeaves", totalLeaves1);
									attendanceDetails1.put("nonPayableLeaves", actualNonPayableLeaves - totalLeaves1);
								}
								fields.put("1", attendanceDetails2);
								fields.put("2", attendanceDetails1);
							}
						} else {
							fields.put("1", attendanceDetails1);
							fields.put("2", attendanceDetails2);
						}
					} else {
						attendanceDetails1.put("leaves", totalLeaves);
						attendanceDetails1.put("nonPayableLeaves", actualNonPayableLeaves);
						fields.put("1", attendanceDetails1);
					}
					// LogUtility.writeLog("fields >> " + fields.toString() + " << attendanceDetails1 >> " + attendanceDetails1 + " << attendanceDetails2 >>" + attendanceDetails2);
					if (isGenerateSalary) {
						salaryCalculationForRegularEmployee(employeeSalaryComponentArray, employeeRecord, fields, employeeId, salaryOnHoldDays);
					}
				}
			} catch (Exception e) {
				String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeSalaryGenerationServlet.class.getName(), e);
				exception += (" << employeeid >> " + employeeId + " << exception >> message [" + e.getMessage() + " << >> " + trace + "]");
			}
		}
		if (exception.length() > 0) {
			sendMailInCaseOfException(new BusinessLogicException(exception));
		}
		return employeeId;
	}

	private static JSONArray getEmployeeAdditionalInformation(Object employeeId) throws Exception {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_additional_information");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("solary_on_hold_days");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid=" + employeeId);

		JSONObject attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_employees_additional_information");
		return rows;
	}

	private static void deleteOldCalculatedComponents(Object employeeId, JSONObject fields) throws JSONException {
		int monthId = fields.optInt("monthId");
		int yearId = fields.optInt("yearId");
		JSONArray oldCalculatedComponents = getOldCalculatedComponents(employeeId, monthId, yearId);
		for (int counter = 0; counter < oldCalculatedComponents.length(); counter++) {
			Object key = oldCalculatedComponents.getJSONObject(counter).opt(Updates.KEY);
			deleteCalculatedComponent(key);
		}
	}

	private static void deleteCalculatedComponent(Object key) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "componentmonthlyamount");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		row.put("__type__", "delete");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);

	}

	private static JSONArray getOldCalculatedComponents(Object employeeId, int monthId, int yearId) throws JSONException {

		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "componentmonthlyamount");
		columnArray.put(Updates.KEY);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId);

		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("componentmonthlyamount");

		return rows;
	}

	/**
	 * This method is calculate monthly amount of all components and insert in calculate monthly amount DO.
	 * 
	 * @param employeeSalaryComponentArray
	 * @param employeeRecord
	 * @param fields
	 * @param employeeId
	 * @param salaryOnHoldDays
	 * @throws JSONException
	 */
	@SuppressWarnings("unchecked")
	public static void salaryCalculationForRegularEmployee(JSONArray employeeSalaryComponentArray, JSONObject employeeRecord, JSONObject fields, Object employeeId, double salaryOnHoldDays) throws Exception {
		// String officialEmailId = employeeRecord.optString("officialemailid");

		HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns> monthlyAttendanceMap = (HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns>) fields.opt("monthlyAttendanceMap");
		HashMap<Integer, Object[]> componentKeyMap = new HashMap<Integer, Object[]>();
		fields.put("componentKeyMap", componentKeyMap);
		if (monthlyAttendanceMap != null && monthlyAttendanceMap.containsKey(employeeId)) {
			EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = monthlyAttendanceMap.get(employeeId);
			fields.put("employeeMonthlyAttandance", employeeMonthlyAttandance);
			HashMap<Integer, Double[]> employeeAttendanceMap = new HashMap<Integer, Double[]>();

			// LogUtility.writeLog("employeeMonthlyAttandance.getNonPaybleLeave() >> " + employeeMonthlyAttandance.getNonPaybleLeave() + " << employeeMonthlyAttandance.getNonPaybleAbsent() >> " + employeeMonthlyAttandance.getNonPaybleAbsent());
			int basicCount = 0;
			int pfEmployeeCount = 0;
			int pfEmployerCount = 0;
			HashMap<String, Object[]> componentMap = new HashMap<String, Object[]>();
			// boolean isIncrement = fields.optBoolean("increment");
			for (int counter = 0; counter < employeeSalaryComponentArray.length(); counter++) {
				JSONObject employeeSalaryComponentRecord = employeeSalaryComponentArray.getJSONObject(counter);
				double componentAmount = employeeSalaryComponentRecord.opt("amount") == null ? 0.0 : employeeSalaryComponentRecord.optDouble("amount");
				int componentCriteriaId = (Integer) employeeSalaryComponentRecord.opt("salarycomponentid.componentcriteriaid");
				if (componentAmount > 0.0 || componentCriteriaId == HRISApplicationConstants.NET_PAYABLE_BASED) {
					int componentTypeId = Translator.integerValue(employeeSalaryComponentRecord.opt("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID));
					if (componentTypeId != HRISApplicationConstants.SalaryComponentTypes.USL) {
						Object salaryComponentId = employeeSalaryComponentRecord.opt("salarycomponentid");
						Object salaryComponentName = employeeSalaryComponentRecord.opt("salarycomponentid.name");
						Object employeeSalaryComponentId = employeeSalaryComponentRecord.opt(Updates.KEY);
						Integer paymentCycleId = (Integer) employeeSalaryComponentRecord.opt("salarycomponentid.paymentcycleid");
						int performanceTypeId = employeeSalaryComponentRecord.optInt("salarycomponentid.performancetypeid");

						Date applicableFromDate = Translator.dateValue(employeeSalaryComponentRecord.opt("applicablefrom"));
						Date applicableToDate = Translator.dateValue(employeeSalaryComponentRecord.opt("applicableto"));

						if (!employeeAttendanceMap.containsKey(Translator.integerValue(salaryComponentId))) {
							Double[] attendanceDetail = new Double[3];
							attendanceDetail[0] = employeeMonthlyAttandance.getNonPaybleLeave();
							attendanceDetail[1] = employeeMonthlyAttandance.getNonPaybleAbsent();
							attendanceDetail[2] = employeeMonthlyAttandance.getActualExtraWorkingDaysInMonth();
							employeeAttendanceMap.put(Translator.integerValue(salaryComponentId), attendanceDetail);
						}

						Date monthFirstDate = (Date) fields.get("monthFirstDate");
						Date monthLastDate = (Date) fields.get("monthLastDate");

						if (applicableFromDate.after(monthFirstDate)) {
							monthFirstDate = applicableFromDate;
						}
						if (applicableToDate.before(monthLastDate)) {
							monthLastDate = applicableToDate;
						}
						if (componentCriteriaId != HRISApplicationConstants.FIXED_BASED) {
							Date joiningDate = (Date) fields.opt("joiningDate");
							if (joiningDate != null && joiningDate.after(monthFirstDate)) {
								monthFirstDate = joiningDate;
							}
						}
						// if (applicableToDate.before(monthLastDate)) {
						// monthLastDate = applicableToDate;
						// }

						// fields.put("joiningDate", joiningDate);
						// fields.put("joiningDateString", fromDateStr);

						fields.put("applicableFromDateInString", EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate));
						fields.put("applicableToDateInString", EmployeeSalaryGenerationServlet.getDateInString(monthLastDate));

						// fields.put("componentTypeId", componentTypeId);
						fields.put("applicableToDate", applicableToDate);

						Calendar cl1 = Calendar.getInstance();
						Calendar cl2 = Calendar.getInstance();
						cl1.setTime(monthFirstDate);
						cl2.setTime(monthLastDate);
						long milliseconds1 = cl1.getTimeInMillis();
						long milliseconds2 = cl2.getTimeInMillis();
						long diff = milliseconds2 - milliseconds1;
						double incrementDateDifference = 0.0;
						if (cl1.before(cl2)) {
							incrementDateDifference = (diff / (24 * 60 * 60 * 1000) + 1);
						} else if (cl1.equals(cl2)) {
							incrementDateDifference = 1;
						}
						incrementDateDifference -= salaryOnHoldDays;
						if (paymentCycleId == HRISApplicationConstants.QUARTERLY) {
							componentAmount = (componentAmount * 4) / 12;
						} else if (paymentCycleId == HRISApplicationConstants.HALF_YEARLY) {
							componentAmount = (componentAmount * 2) / 12;
						} else if (paymentCycleId == HRISApplicationConstants.YEARLY) {
							componentAmount = (componentAmount * 1) / 12;
						} else if (paymentCycleId == HRISApplicationConstants.MONTHLY) {
							componentAmount = (componentAmount * 12) / 12;
						}
						JSONArray componentExtraInformation = getComponentExtraInformation(salaryComponentId);
						double[] calculatedAmount = calculateRegularEmployeeComponentAmount(employeeId, componentAmount, employeeSalaryComponentRecord, applicableToDate, componentExtraInformation, fields, incrementDateDifference, employeeAttendanceMap);
						if (salaryComponentName.toString().replaceAll(" ", "").equalsIgnoreCase("Basic")) {
							Object[] object = new Object[4];
							basicCount++;
							Double amount = calculatedAmount[0];
							object[0] = amount;
							object[1] = calculatedAmount[1];
							object[2] = employeeSalaryComponentId;
							object[3] = salaryComponentId;
							componentMap.put(salaryComponentName.toString().replaceAll(" ", "") + basicCount, object);
						}

						if (salaryComponentName.toString().replaceAll(" ", "").equalsIgnoreCase("PFEmployee")) {
							Object[] object = new Object[4];
							pfEmployeeCount++;
							Double amount = calculatedAmount[0];
							object[0] = amount;
							object[1] = calculatedAmount[1];
							object[2] = employeeSalaryComponentId;
							object[3] = salaryComponentId;
							componentMap.put(salaryComponentName.toString().replaceAll(" ", "") + pfEmployeeCount, object);
						}

						if (salaryComponentName.toString().replaceAll(" ", "").equalsIgnoreCase("PFEmployer")) {
							Object[] object = new Object[4];
							pfEmployerCount++;
							Double amount = calculatedAmount[0];
							object[0] = amount;
							object[1] = calculatedAmount[1];
							object[2] = employeeSalaryComponentId;
							object[3] = salaryComponentId;
							componentMap.put(salaryComponentName.toString().replaceAll(" ", "") + pfEmployerCount, object);
						}

						if (!salaryComponentName.toString().replaceAll(" ", "").equalsIgnoreCase("PFEmployer") && !salaryComponentName.toString().replaceAll(" ", "").equalsIgnoreCase("PFEmployee") && performanceTypeId != HRISApplicationConstants.TARGET_ACHIEVED) {
							if (componentCriteriaId == HRISApplicationConstants.NET_PAYABLE_BASED) {
								calculatedAmount[0] = 0.0;
								calculatedAmount[1] = 0.0;
							}
							insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, calculatedAmount[1], calculatedAmount[0], fields);
						}
					}
				}
			}
			int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			if (currentOrganizationId == 7783) {// organization.toLowerCase().indexOf("navigant") != -1) {
				double absents = employeeMonthlyAttandance.getTotalAbsentInMonth();
				if (absents > 0) {
					double grossAmount = 750;
					double deductableAmount = ((float) 750 * absents);
					Object uslComponentId = PunchingUtility.getId("salarycomponents", SalaryComponentKinds.COMPONENT_TYPE_ID, "" + HRISApplicationConstants.SalaryComponentTypes.USL);
					String monthFirstDateInString = "" + fields.opt("monthFirstDateInString");
					String monthLastDateInString = "" + fields.opt("monthLastDateInString");
					Object employeeSalaryComponentId = getEmployeeSalaryComponentId(employeeId, uslComponentId, monthFirstDateInString, monthLastDateInString);
					try {

						if (employeeSalaryComponentId == null) {
							employeeSalaryComponentId = insertEmployeeSalaryPackageComponentsDetails(employeeId, uslComponentId, monthFirstDateInString, monthLastDateInString, grossAmount, fields.opt("monthId"));
						}
					} catch (Exception e) {
						LogUtility.writeLog("adding component error >> " + e.getMessage());
					}
					insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, uslComponentId, grossAmount, deductableAmount, fields);
				}
			}
			for (Integer salaryComponentId : componentKeyMap.keySet()) {
				Object[] details = componentKeyMap.get(salaryComponentId);
				if (details != null) {
					Object employeeSalaryComponentId = details[1];
					double calculatedAmount = Translator.doubleValue(details[2]);
					double grossAmount = Translator.doubleValue(details[3]);
					insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, grossAmount, calculatedAmount, fields);
				}
			}
			calculatePFAndInsert(fields, employeeId, componentMap);
		} else {
			// LogUtility.writeLog("in else >> monthlyattendanceArray length is zero or less....<<");
		}
	}

	private static Object getEmployeeSalaryComponentId(Object employeeId, Object uslComponentId, String monthFirstDateInString, String monthLastDateInString) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND salarycomponentid = " + uslComponentId + " AND applicablefrom='" + monthFirstDateInString + "' AND applicableto='" + monthLastDateInString + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeesalarycomponents");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			Object yearId = rows.getJSONObject(0).opt("__key__");
			return yearId;
		}
		return null;
	}

	public static Object insertEmployeeSalaryPackageComponentsDetails(Object employeeId, Object componentId, Object fromDate, Object toDate, Object amount, Object dueMonthId) throws JSONException {
		JSONObject updates = new JSONObject();
		JSONObject columns = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeesalarycomponents");
		columns.put("temp", HRISApplicationConstants.TEMP_VALUE);
		columns.put("employeeid", employeeId);
		columns.put("salarycomponentid", componentId);
		columns.put("applicablefrom", fromDate);
		columns.put("applicableto", toDate);
		columns.put("amount", amount);
		columns.put("duemonthid", dueMonthId);
		updates.put(Data.Update.UPDATES, columns);
		JSONObject update = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
		return update.getJSONObject(Data.Query.KEY).get(Data.Update.Updates.Key.ACTUAL);
	}

	private static void calculatePFAndInsert(JSONObject fields, Object employeeId, HashMap<String, Object[]> componentMap) throws JSONException {
		if (componentMap.containsKey("Basic1") && componentMap.containsKey("PFEmployee1")) {
			double grossBasicAmount = Translator.doubleValue(componentMap.get("Basic1")[0]);
			if (componentMap.containsKey("Basic2")) {
				grossBasicAmount += Translator.doubleValue(componentMap.get("Basic2")[0]);
			}
			if (grossBasicAmount <= 6500) {
				double basicAmount = Translator.doubleValue(componentMap.get("Basic1")[0]);
				if (componentMap.containsKey("Basic2") && !componentMap.containsKey("PFEmployee2")) {
					basicAmount += Translator.doubleValue(componentMap.get("Basic2")[0]);
				}
				double grossAmount = Translator.doubleValue(componentMap.get("PFEmployee1")[1]);
				double amount = 0.0;
				if (grossAmount > 0) {
					amount = ((float) basicAmount * 12) / 100;
				}
				Object employeeSalaryComponentId = componentMap.get("PFEmployee1")[2];
				Object salaryComponentId = componentMap.get("PFEmployee1")[3];

				insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, grossAmount, amount, fields);

				if (componentMap.containsKey("PFEmployee2")) {
					basicAmount = Translator.doubleValue(componentMap.get("Basic2")[0]);
					grossAmount = Translator.doubleValue(componentMap.get("PFEmployee2")[1]);
					amount = 0.0;
					if (grossAmount > 0) {
						amount = ((float) basicAmount * 12) / 100;
					}
					employeeSalaryComponentId = componentMap.get("PFEmployee2")[2];
					salaryComponentId = componentMap.get("PFEmployee2")[3];
					insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, grossAmount, amount, fields);
				}

				if (componentMap.containsKey("PFEmployer1")) {
					basicAmount = Translator.doubleValue(componentMap.get("Basic1")[0]);
					if (componentMap.containsKey("Basic2") && !componentMap.containsKey("PFEmployer2")) {
						basicAmount += Translator.doubleValue(componentMap.get("Basic2")[0]);
					}
					grossAmount = Translator.doubleValue(componentMap.get("PFEmployer1")[1]);
					amount = 0.0;
					if (grossAmount > 0) {
						amount = ((float) basicAmount * 12) / 100;
					}
					employeeSalaryComponentId = componentMap.get("PFEmployer1")[2];
					salaryComponentId = componentMap.get("PFEmployer1")[3];
					insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, grossAmount, amount, fields);
				}

				if (componentMap.containsKey("PFEmployer2")) {
					basicAmount = Translator.doubleValue(componentMap.get("Basic1")[0]);
					if (componentMap.containsKey("Basic2")) {
						basicAmount = Translator.doubleValue(componentMap.get("Basic2")[0]);
					}
					grossAmount = Translator.doubleValue(componentMap.get("PFEmployer2")[1]);
					amount = 0.0;
					if (grossAmount > 0) {
						amount = ((float) basicAmount * 12) / 100;
					}
					employeeSalaryComponentId = componentMap.get("PFEmployer2")[2];
					salaryComponentId = componentMap.get("PFEmployer2")[3];
					insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, grossAmount, amount, fields);
				}

			} else {
				double grossAmount = Translator.doubleValue(componentMap.get("PFEmployee1")[1]);
				Object employeeSalaryComponentId = componentMap.get("PFEmployee1")[2];
				Object salaryComponentId = componentMap.get("PFEmployee1")[3];
				insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, grossAmount, grossAmount, fields);

				if (componentMap.containsKey("PFEmployee2")) {
					grossAmount = Translator.doubleValue(componentMap.get("PFEmployee2")[1]);
					employeeSalaryComponentId = componentMap.get("PFEmployee2")[2];
					salaryComponentId = componentMap.get("PFEmployee2")[3];
					insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, grossAmount, grossAmount, fields);
				}

				if (componentMap.containsKey("PFEmployer1")) {
					grossAmount = Translator.doubleValue(componentMap.get("PFEmployer1")[1]);
					employeeSalaryComponentId = componentMap.get("PFEmployer1")[2];
					salaryComponentId = componentMap.get("PFEmployer1")[3];
					insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, grossAmount, grossAmount, fields);
				}

				if (componentMap.containsKey("PFEmployer2")) {
					grossAmount = Translator.doubleValue(componentMap.get("PFEmployer2")[1]);
					employeeSalaryComponentId = componentMap.get("PFEmployer2")[2];
					salaryComponentId = componentMap.get("PFEmployer2")[3];
					insertCalculatedMonthlyAmount(employeeId, employeeSalaryComponentId, salaryComponentId, grossAmount, grossAmount, fields);
				}
			}
		}
	}

	private static JSONArray getComponentExtraInformation(Object salaryComponentId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponentsextrainformation");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("salarycriteriaid");
		columnArray.put("percentage");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarycomponentid = " + salaryComponentId);
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray array = employeeObject.getJSONArray("salarycomponentsextrainformation");
		return array;
	}

	private static void insertCalculatedMonthlyAmount(Object employeeId, Object employeeSalaryComponentId, Object salaryComponentId, double componentAmount, double calculatedAmount, JSONObject fields) throws JSONException {
		// JSONArray array = getCalculatedMonthlyAmountRecord(employeeId, employeeSalaryComponentId, salaryComponentId, fields);
		// if (array.length() > 0) {
		// Object componentMonthlyAmountId = array.getJSONObject(0).opt("__key__");
		// if (componentMonthlyAmountId != null) {
		// updateCalculatedMonthlyComponentAmount(employeeId, componentMonthlyAmountId, employeeSalaryComponentId, salaryComponentId, calculatedAmount, componentAmount, fields);
		// }
		// } else {
		updateCalculatedMonthlyComponentAmount(employeeId, null, employeeSalaryComponentId, salaryComponentId, calculatedAmount, componentAmount, fields);
		// }
	}

	private static void updateCalculatedMonthlyComponentAmount(Object employeeId, Object key, Object employeeSalaryComponentId, Object salaryComponentId, double payableAmount, double grossAmount, JSONObject fields) {
		try {
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "componentmonthlyamount");
			JSONObject row = new JSONObject();
			JSONObject attendance1 = fields.optJSONObject("1");
			JSONObject attendance2 = fields.optJSONObject("2");
			if (attendance1 != null && attendance1.length() > 0) {
				row.put("present", attendance1.opt("present"));
				row.put("absent", attendance1.opt("absent"));
				row.put("actualnonworkingdays", attendance1.opt("actualNonWorkingDays"));
				row.put("ewd", attendance1.opt("ewd"));
				row.put("leaves", attendance1.opt("leaves"));
				row.put("nonpayableleaves", attendance1.opt("nonPayableLeaves"));
				row.put("isinfirstcomponents", HRISApplicationConstants.EmployeeDecision.YES);
				row.put("isincrement", Translator.booleanValue(fields.opt("increment")));
			}
			if (Translator.booleanValue(fields.opt("increment")) && attendance1 != null && attendance1.length() > 0 && attendance2 != null && attendance2.length() > 0) {
				Date toDate1 = attendance1.opt("toDate") == null ? null : (Date) attendance1.opt("toDate");
				Date toDate2 = attendance2.opt("toDate") == null ? null : (Date) attendance2.opt("toDate");
				Date applicableToDate = fields.opt("applicableToDate") == null ? null : (Date) fields.opt("applicableToDate");
				JSONObject attendanceToBeUse = null;
				if (toDate1.after(applicableToDate) || toDate1.equals(applicableToDate)) {
					if ((toDate2.after(applicableToDate) || toDate2.equals(applicableToDate)) && toDate2.before(toDate1)) {
						attendanceToBeUse = fields.optJSONObject("2");
						row.put("isinfirstcomponents", HRISApplicationConstants.EmployeeDecision.NO);
					} else {
						attendanceToBeUse = fields.optJSONObject("1");
						row.put("isinfirstcomponents", HRISApplicationConstants.EmployeeDecision.YES);
					}
				} else {
					attendanceToBeUse = fields.optJSONObject("2");
					row.put("isinfirstcomponents", HRISApplicationConstants.EmployeeDecision.NO);
				}
				if (attendanceToBeUse != null && attendanceToBeUse.length() > 0) {
					row.put("present", attendanceToBeUse.opt("present"));
					row.put("absent", attendanceToBeUse.opt("absent"));
					row.put("actualnonworkingdays", attendanceToBeUse.opt("actualNonWorkingDays"));
					row.put("ewd", attendanceToBeUse.opt("ewd"));
					row.put("leaves", attendanceToBeUse.opt("leaves"));
					row.put("nonpayableleaves", attendanceToBeUse.opt("nonPayableLeaves"));
					row.put("isincrement", Translator.booleanValue(fields.opt("increment")));
				}
			}
			if (key != null) {
				row.put("__key__", key);
			}
			row.put("employeeid", employeeId);
			row.put("employeesalarycomponentid", employeeSalaryComponentId);
			row.put("salarycomponentid", salaryComponentId);
			row.put("grossamount", grossAmount);
			row.put("payableamount", payableAmount);
			row.put("monthid", fields.opt("monthId"));
			row.put("yearid", fields.opt("yearId"));
			row.put("temp", HRISApplicationConstants.TEMP_VALUE);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		} catch (Exception e) {
			throw new RuntimeException("Error occured while updating monthly component amount: " + e.getMessage());
		}
	}

	// private static JSONArray getCalculatedMonthlyAmountRecord(Object employeeId, Object employeeSalaryComponentId, Object salaryComponentId, JSONObject fields) throws JSONException {
	// JSONObject query = new JSONObject();
	// query.put(Data.Query.RESOURCE, "componentmonthlyamount");
	// JSONArray columnArray = new JSONArray();
	// columnArray.put("__key__");
	// query.put(Data.Query.COLUMNS, columnArray);
	// query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and employeesalarycomponentid = " + employeeSalaryComponentId + " and monthid = " + fields.opt("monthId") + " and yearid = " + fields.opt("yearId") + " and salarycomponentid = " + salaryComponentId);
	// JSONObject employeeObject;
	// employeeObject = ResourceEngine.query(query);
	// JSONArray array = employeeObject.getJSONArray("componentmonthlyamount");
	// return array;
	// }

	private static JSONArray getComponentMonthlyAmountRecord(String key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "componentmonthlyamount");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid.branchid");
		columnArray.put("employeeid");
		columnArray.put("monthid");
		columnArray.put("yearid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + key + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray array = employeeObject.getJSONArray("componentmonthlyamount");
		return array;

	}

	@SuppressWarnings("unchecked")
	public static double[] calculateRegularEmployeeComponentAmount(Object employeeId, double componentAmount, JSONObject employeeSalaryComponentRecord, Date applicableToDate, JSONArray componentExtraInformation, JSONObject fields, double incrementDateDifference, HashMap<Integer, Double[]> employeeAttendanceMap) throws JSONException {

		long maxDaysInMonth = (Long) fields.opt("maxDaysInMonthTotal");
		HashMap<Integer, Object[]> componentKeyMap = (HashMap<Integer, Object[]>) fields.opt("componentKeyMap");
		EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = (EmployeeMonthlyAttandanceUpdateAllColumns) fields.opt("employeeMonthlyAttandance");

		double presentDays = employeeMonthlyAttandance.getTotalPresentsInMonth();
		int componentCriteriaId = (Integer) employeeSalaryComponentRecord.opt("salarycomponentid.componentcriteriaid");
		Object kpiId = employeeSalaryComponentRecord.opt("salarycomponentid.keyperformanceindicatorid");
		int salaryComponentId = employeeSalaryComponentRecord.optInt("salarycomponentid");
		int employeeSalaryComponentId = employeeSalaryComponentRecord.optInt(Updates.KEY);
		double[] calculatedAmount = { 0.0, 0.0 };
		if (presentDays > 0.0) {
			if (componentCriteriaId == HRISApplicationConstants.FIXED_BASED) {
				calculatedAmount[0] = (componentAmount * ((float) (incrementDateDifference * 100) / maxDaysInMonth) / 100);
				calculatedAmount[1] = calculatedAmount[0];
				if (componentExtraInformation.length() > 0) {
					int salaryCriteriaExtraInfoID = componentExtraInformation.getJSONObject(0).optInt("salarycriteriaid");
					double percentage = Translator.doubleValue(componentExtraInformation.getJSONObject(0).opt("percentage"));
					if (salaryCriteriaExtraInfoID == HRISApplicationConstants.ATTENDANCE_BASED) {
						double actualNonWorkingDays = employeeMonthlyAttandance.getActualNonWorkingDaysInMonth();
						double totalPresentDays = presentDays + actualNonWorkingDays;
						double presentPercentage = (totalPresentDays / maxDaysInMonth) * 100;
						if (presentPercentage < percentage) {
							calculatedAmount[0] = 0.0;
							calculatedAmount[1] = 0.0;
						}
					}
				}
			} else if (componentCriteriaId == HRISApplicationConstants.ATTENDANCE_BASED) {
				calculatedAmount = calculateAttendanceBasedAmount(componentAmount, employeeMonthlyAttandance, incrementDateDifference, fields);
				boolean isIncrement = fields.optBoolean("increment");
				if (isIncrement) {
					calculateAttendanceBasedAmountIncremented(employeeId, componentAmount, employeeMonthlyAttandance, incrementDateDifference, fields, calculatedAmount, employeeAttendanceMap, salaryComponentId);
				}
			} else if (componentCriteriaId == HRISApplicationConstants.PERFORMANCE_BASED) {
				JSONArray performanceBasedArray = getEmployeePerformanceAmount(kpiId, employeeId, fields);
				Object[] details = new Object[4];
				if (componentKeyMap.containsKey(salaryComponentId)) {
					details = componentKeyMap.get(salaryComponentId);
					if (details != null) {
						details[3] = Translator.doubleValue(details[3]) + (float) (componentAmount * (incrementDateDifference) / (Long) fields.opt("maxDaysInMonth"));
						Date applicableToDateOldComponent = (Date) details[0];
						if (applicableToDateOldComponent.before(applicableToDate)) {
							details[1] = employeeSalaryComponentId;
						}
					}
				}
				if (!componentKeyMap.containsKey(salaryComponentId)) {

					calculatedAmount = getPerformanceBasedAmount(performanceBasedArray, employeeSalaryComponentId, componentAmount, incrementDateDifference, fields);
					details = new Object[4];
					details[0] = applicableToDate;
					details[1] = employeeSalaryComponentId;
					details[2] = calculatedAmount[0];
					details[3] = calculatedAmount[1];
				}
				componentKeyMap.put(salaryComponentId, details);
				fields.put("componentKeyMap", componentKeyMap);
			} else {
				return calculatedAmount;
			}

		}
		return calculatedAmount;
	}

	private static void calculateAttendanceBasedAmountIncremented(Object employeeId, double componentAmount, EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance, double incrementDateDifference, JSONObject fields, double[] calculatedAmount, HashMap<Integer, Double[]> employeeAttendanceMap, int salaryComponentId) throws JSONException {
		String applicableFromDateInString = fields.optString("applicableFromDateInString");
		String applicableToDateInString = fields.optString("applicableToDateInString");
		Double[] nonPayableLeavesAbsents = employeeAttendanceMap.get(salaryComponentId);
		double nonPayableLeaves = nonPayableLeavesAbsents[0];
		double nonPayableAbsent = nonPayableLeavesAbsents[1];
		double actualExtraWorkingDay = nonPayableLeavesAbsents[2];
		// LogUtility.writeLog("nonPayableLeaves >> " + nonPayableLeaves + " < <nonPayableAbsent >> " + nonPayableAbsent);
		long maxDaysInMonthTotal = (Long) fields.opt("maxDaysInMonthTotal");
		double amount = 0.0;
		String filters = "employeeId = " + employeeId + " and attendancedate >= '" + applicableFromDateInString + "'" + " and attendancedate <= '" + applicableToDateInString + "'";// + " and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_LEAVE + ", " + HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE + ", " + HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE + ")";
		double totalLeaves = 0.0;
		double totalAbsent = 0.0;
		double totalEWD = 0.0;
		int shortLeaves = 0;
		boolean hasComponent = false;
		if (!fields.has("salaryComponentId" + salaryComponentId)) {
			fields.put("salaryComponentId" + salaryComponentId, "0");
		} else {
			hasComponent = true;
		}
		JSONArray dailyAttendance = getDailyAttendance(employeeId, filters);
		for (int counter = 0; counter < dailyAttendance.length(); counter++) {
			int attendanceType = Translator.integerValue(dailyAttendance.getJSONObject(counter).opt("attendancetypeid"));
			int paidStatus = Translator.integerValue(dailyAttendance.getJSONObject(counter).opt("paidstatus"));
			int shortLeaveId = dailyAttendance.getJSONObject(counter).optInt("attendance_type_short_leave_id");
			if (attendanceType == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceType == HRISApplicationConstants.ATTENDANCE_TOUR || attendanceType == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
				if (shortLeaveId == HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE) {
					shortLeaves++;
				}
				if (shortLeaves > 0 && (shortLeaves % 3) == 0) {
					totalLeaves += 0.50;
				}
			}

			if (attendanceType == HRISApplicationConstants.ATTENDANCE_LEAVE) {// && paidStatus == HRISApplicationConstants.LEAVE_EWD_UN_PAID) {
				totalLeaves += 1.0;
			} else if ((attendanceType == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceType == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceType == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY)) {// && paidStatus == HRISApplicationConstants.LEAVE_EWD_UN_PAID) {
				totalLeaves += 0.50;
			}
			if (attendanceType == HRISApplicationConstants.ATTENDANCE_ABSENT) {
				totalAbsent += 1.0;
			}
			if (attendanceType == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY && paidStatus == HRISApplicationConstants.LEAVE_EWD_PAID) {
				totalEWD += 1.0;
			}
			if (attendanceType == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF && paidStatus == HRISApplicationConstants.LEAVE_EWD_PAID) {
				totalEWD += 0.50;
			}
		}
		if (hasComponent || (nonPayableLeaves > 0 && totalLeaves > 0)) {
			if (hasComponent || nonPayableLeaves <= totalLeaves) {
				amount = (float) (componentAmount * nonPayableLeaves) / maxDaysInMonthTotal;
				nonPayableLeavesAbsents[0] = 0.0;
			} else {
				// amount = (float) (componentAmount * (nonPayableLeaves - totalLeaves)) / maxDaysInMonthTotal;
				amount = (float) (componentAmount * (totalLeaves)) / maxDaysInMonthTotal;
				nonPayableLeavesAbsents[0] = (nonPayableLeaves - totalLeaves);
			}
			// LogUtility.writeLog("nonPayableLeaves >> " + nonPayableLeaves + " << totalLeaves >> " + totalLeaves + " << amount >> " + amount + " << componentAmount >> " + componentAmount);
			calculatedAmount[0] = calculatedAmount[0] - amount;
		}
		if (hasComponent || (nonPayableAbsent > 0 && totalAbsent > 0)) {
			if (hasComponent || nonPayableAbsent <= totalAbsent) {
				amount = (float) (componentAmount * nonPayableAbsent) / maxDaysInMonthTotal;
				nonPayableLeavesAbsents[1] = 0.0;
			} else {
				// amount = (float) (componentAmount * (nonPayableAbsent - totalAbsent)) / maxDaysInMonthTotal;
				amount = (float) (componentAmount * (totalAbsent)) / maxDaysInMonthTotal;
				nonPayableLeavesAbsents[1] = (nonPayableAbsent - totalAbsent);
			}
			calculatedAmount[0] = calculatedAmount[0] - amount;
		}
		employeeAttendanceMap.put(salaryComponentId, nonPayableLeavesAbsents);
		// LogUtility.writeLog("totalEWD >> " + totalEWD + " << employeeMonthlyAttandance.getIsExtraWorkingDayEncashable() >> " + employeeMonthlyAttandance.getIsExtraWorkingDayEncashable() + " << componentAmount >> " + componentAmount + " << maxDaysInMonthTotal >> " + maxDaysInMonthTotal + " << calculatedAmount[0] >>" + calculatedAmount[0]);
		if (actualExtraWorkingDay > 0 && totalEWD > 0) {
			amount = (float) (componentAmount * totalEWD) / maxDaysInMonthTotal;
			// LogUtility.writeLog("amount >> " + amount);
			if (employeeMonthlyAttandance.getIsExtraWorkingDayEncashable()) {
				calculatedAmount[0] = calculatedAmount[0] + amount;
			}
		}
		// LogUtility.writeLog("calculatedAmount[0] >> " + calculatedAmount[0] + " << calculatedAmount[1] >> " + calculatedAmount[1]);
	}

	private static JSONArray getDailyAttendance(Object employeeId, String filters) throws JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("attendancedate");
		array.put("attendancetypeid");
		array.put("paidstatus");
		array.put("attendance_type_short_leave_id");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, filters);

		JSONObject attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeeattendance");
		return rows;
	}

	private static double[] getPerformanceBasedAmount(JSONArray performanceBasedArray, Object employeeSalaryComponentId, double componentAmount, double incrementDateDifference, JSONObject fields) throws JSONException {
		double[] totalDueAmount = { 0.0, 0.0 };
		if (performanceBasedArray.length() > 0) {
			double performanceAmount = performanceBasedArray.getJSONObject(0).opt("amount") == null ? 0.0 : Translator.doubleValue(performanceBasedArray.getJSONObject(0).opt("amount"));
			// double grossAmount = performanceBasedArray.getJSONObject(0).opt("grossamount") == null ? 0.0 : Translator.doubleValue(performanceBasedArray.getJSONObject(0).opt("grossamount"));
			totalDueAmount[1] = (float) (componentAmount * (incrementDateDifference) / (Long) fields.opt("maxDaysInMonth"));
			totalDueAmount[0] = performanceAmount;
			// totalDueAmount[1] = grossAmount;
			return totalDueAmount;
		} else {
			return totalDueAmount;
		}
	}

	private static JSONArray getEmployeePerformanceAmount(Object kpiId, Object employeeId, JSONObject fields) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeperformance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("achieved");
		columnArray.put("target");
		columnArray.put("amount");
		columnArray.put("grossamount");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and keyperformanceindicatorid = " + kpiId + " and performanceyearid = " + fields.opt("yearId") + " and performancemonthid = " + fields.opt("monthId"));
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray array = employeeObject.getJSONArray("employeeperformance");
		return array;
	}

	private static double[] calculateAttendanceBasedAmount(double componentAmount, EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance, double incrementDateDifference, JSONObject fields) {
		boolean isIncrement = fields.optBoolean("increment");
		// boolean isRelieving = Translator.booleanValue(fields.opt("isRelieving"));
		boolean isJoining = Translator.booleanValue(fields.opt("isJoining"));
		// LogUtility.writeLog("calculateAttendanceBasedAmount >> isIncrement >> " + isIncrement);
		double presentDays = employeeMonthlyAttandance.getTotalPresentsInMonth();
		double actualNonWorkingDays = employeeMonthlyAttandance.getActualNonWorkingDaysInMonth();
		double totalPresentDays = presentDays + actualNonWorkingDays;

		double actualExtraWorkingDays = employeeMonthlyAttandance.getActualExtraWorkingDaysInMonth();
		double paidAbsents = employeeMonthlyAttandance.getPaybleAbsent();
		double paidLeaves = employeeMonthlyAttandance.getPaybleLeave();
		totalPresentDays += paidLeaves + paidAbsents;
		// LogUtility.writeLog("fields " + fields + "employeeMonthlyAttandance.getIsExtraWorkingDayEncashable() >> " + employeeMonthlyAttandance.getIsExtraWorkingDayEncashable() + " << isIncrement >> " + isIncrement + " << isJoining >> " + isJoining + " << actualExtraWorkingDays >> " + actualExtraWorkingDays);
		if (!isIncrement) {
			if (employeeMonthlyAttandance.getIsExtraWorkingDayEncashable()) {
				totalPresentDays += actualExtraWorkingDays;
			}
		} else {
			double nonPaidAbsents = employeeMonthlyAttandance.getNonPaybleAbsent();
			double nonPaidLeaves = employeeMonthlyAttandance.getNonPaybleLeave();
			totalPresentDays += nonPaidLeaves + nonPaidAbsents;
		}
		double presentPercentage = 0.0;
		if (isJoining) {
			presentPercentage = ((float) (totalPresentDays * 100)) / ((Long) fields.opt("maxDaysInMonth"));
		} else {
			presentPercentage = ((float) (totalPresentDays * 100)) / ((Long) fields.opt("maxDaysInMonthTotal"));
		}
		double incrementDiffPercentage = ((float) (incrementDateDifference * 100)) / ((Long) fields.opt("maxDaysInMonthTotal"));
		double incrementDiffSalaryPer = ((float) (incrementDiffPercentage * presentPercentage)) / 100;

		// LogUtility.writeLog("totalPresentDays >> " + totalPresentDays + " << presentPercentage >> " + presentPercentage + " << incrementDiffPercentage >> " + incrementDiffPercentage + " << incrementDateDifference >> " + incrementDateDifference + " << incrementDiffSalaryPer >> " + incrementDiffSalaryPer);
		double[] totalDueAmount = { 0.0, 0.0 };
		// totalDueAmount[1] = (float) (componentAmount * (incrementDateDifference) / (Long) fields.opt("maxDaysInMonth"));
		if (!isIncrement) {
			totalDueAmount[1] = componentAmount;
		} else {
			totalDueAmount[1] = (float) (componentAmount * (incrementDateDifference) / (Long) fields.opt("maxDaysInMonthTotal"));
		}
		totalDueAmount[0] = (componentAmount * incrementDiffSalaryPer) / 100;
		// LogUtility.writeLog("totalDueAmount[0] >> " + totalDueAmount[0] + "<< totalDueAmount[1] >>" + totalDueAmount[1]);
		return totalDueAmount;
	}

	public static JSONArray getEmployeeMonthlyAttendanceRecords(Object employeeId, JSONObject fields) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.leavepolicyid.isextraworkingdayencashable");
		columnArray.put("presentdays");
		columnArray.put("absents");
		columnArray.put("leaves");
		columnArray.put("extraworkingdays");
		columnArray.put("totalworkingdays");
		columnArray.put("carryextraworkingday");
		columnArray.put("holidays");
		columnArray.put("nonworkingdays");
		columnArray.put("nonpayableleaves");
		columnArray.put("nonpaidabsents");
		columnArray.put("actualnonworkingdays");
		columnArray.put("monthlyclosingbalance");
		columnArray.put("paidabsents");
		columnArray.put("payableleaves");
		columnArray.put("yearid");
		columnArray.put("monthid");
		columnArray.put("nonpaidabsents");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "yearid = " + fields.opt("yearId") + " and  monthid = " + fields.opt("monthId") + " and employeeid = " + employeeId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeemonthlyattendance");
		return rows;
	}

	public static boolean isNewJoineeOrReleaved(Date joiningDate, JSONObject fields) {
		try {
			LogUtility.writeError("joiningDate >> " + joiningDate + " << fields >> " + fields);
			if (joiningDate != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(joiningDate);
				LogUtility.writeError("cal >> " + cal + " << year >> " + cal.get(Calendar.YEAR));
				String currentMonth = DataTypeUtilities.getCurrentMonth(joiningDate);
				long joiningMonthId = ((Number) EmployeeMonthlyAttendance.getMonthId(currentMonth)).longValue();
				String joiningYear = "" + cal.get(Calendar.YEAR);
				long joiningYearId = ((Number) EmployeeMonthlyAttendance.getYearId(joiningYear)).longValue();
				if (joiningMonthId == (Integer) fields.opt("monthId") && joiningYearId == (Integer) fields.opt("yearId")) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		} catch (JSONException e) {
			throw new RuntimeException("Some JSON error occured while checking new joinee condition: " + e.getMessage());
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage("EmployeeSalaryGenerationServlet", e);
			LogUtility.writeError("isNewJoineeOrReleaved >> generate exception Trace >> " + trace);
			throw new RuntimeException("Some error occured while checking new joinee condition: " + e.getMessage());
		}
	}

	public static Date getMonthFirstDate(long yearId, long monthId) throws Exception {
		String monthName = getMonthName(monthId);
		String yearName = getYearName(yearId);
		Date date = null;
		date = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		return date;
	}

	/**
	 * This method will return all active and non-active employees(only those who have been relieved in this month)
	 * 
	 * @param __key__
	 * @param fields
	 * 
	 * @param yearName
	 * @param monthName
	 * @return
	 * @throws JSONException
	 */
	public static JSONArray getActiveAndReleivedEmployeeRecords(Object __key__, JSONObject fields) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("officialemailid");
		columnArray.put("incrementduedate");
		columnArray.put("employeescheduleid");
		columnArray.put("holidaycalendarid");
		columnArray.put("leavepolicyid");
		columnArray.put("leavepolicyid.issandwich");
		columnArray.put("joiningdate");
		columnArray.put("relievingdate");
		columnArray.put("noticeperiodstartdate");
		columnArray.put("noticeperiodenddate");
		String extraFilter = "";
		if (__key__ != null) {
			extraFilter = " and " + Updates.KEY + " > " + __key__;
		}

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + " and branchid = " + fields.opt("branchId") + " AND salary_on_hold !=1" + extraFilter + " AND salary_on_hold != 1");
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and branchid = " + fields.opt("branchId") + " and relievingdate >= '" + fields.opt("monthFirstDateInString") + "'" + " AND salary_on_hold != 1" + extraFilter + " AND salary_on_hold != 1");
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, Updates.KEY);
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.MAX_ROWS, 1);

		JSONObject employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public static JSONArray getEmployeeRecordsForSalarySheetReGenerate(Object key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("officialemailid");
		columnArray.put("name_in_bank");
		columnArray.put("incrementduedate");
		columnArray.put("employeescheduleid");
		columnArray.put("holidaycalendarid");
		columnArray.put("employeescheduleid");
		columnArray.put("leavepolicyid");
		columnArray.put("leavepolicyid.issandwich");
		columnArray.put("joiningdate");
		columnArray.put("relievingdate");
		columnArray.put("branchid");
		columnArray.put("subbranchid");
		columnArray.put("employeecode");
		columnArray.put("profitcenterid");
		columnArray.put("businessfunctionid");
		columnArray.put("accountno");
		columnArray.put("modeofsalaryid");
		columnArray.put("name");
		columnArray.put("noticeperiodstartdate");
		columnArray.put("noticeperiodenddate");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + key + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		// LogUtility.writeLog("getActiveAndReleivedEmployeeRecords >> employeeArray >> " + employeeArray);
		return employeeArray;
	}

	public static String getDateInString(Date date) {
		if (date != null) {
			return new SimpleDateFormat("yyyy-MM-dd").format(date);
		} else {
			return "";
		}
	}

	public static String getMonthName(long monthId) {
		try {
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
		} catch (Exception e) {
			throw new RuntimeException("Error occured while geting month name from monthid: " + e.getMessage());
		}
	}

	public static JSONArray getEmployeeSalaryComponents(String monthFirstDate, String monthLastDate, Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.salarycomponenttypeid"); // basic=8
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid.paymentcycleid");// Yearly, Half
		columnArray.put("salarycomponentid.paymenttypeid");// Payable,Deductable
		columnArray.put("salarycomponentid.componentcriteriaid"); // Fixed, Attendance Based, Performance Based
		columnArray.put("salarycomponentid.keyperformanceindicatorid");// KPI belongs to organization level salary components
		columnArray.put("salarycomponentid.performancetypeid");// Performace type i.e. target achieved and performance percentage
		columnArray.put("salarycomponentid.paidaftermonth");
		columnArray.put("duemonthid");
		columnArray.put("amount");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, "applicablefrom");
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		orders.put(orderObject);
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom <= '" + monthFirstDate + "' and applicableto >= '" + monthFirstDate + "'");
		query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId + " and applicablefrom <= '" + monthLastDate + "' and applicableto >= '" + monthLastDate + "'");
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeesalarycomponents");

		String keys = "";
		for (int counter = 0; counter < rows.length(); counter++) {
			int key = Translator.integerValue(rows.getJSONObject(counter).opt("__key__"));
			if (keys.length() > 0) {
				keys += ("," + key);
			} else {
				keys += ("" + key);
			}
		}
		String filters = "";
		if (keys.length() > 0) {
			filters += (" and __key__ not in (" + keys + ")");
		}
		query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid.salarycomponenttypeid"); // basic=8
		columnArray.put("salarycomponentid.paymentcycleid");// Yearly, Half
		columnArray.put("salarycomponentid.paymenttypeid");// Payable,Deductable
		columnArray.put("salarycomponentid.componentcriteriaid"); // Fixed, Attendance Based, Performance Based
		columnArray.put("salarycomponentid.keyperformanceindicatorid");// KPI belongs to organization level salary components
		columnArray.put("salarycomponentid.performancetypeid");// Performace type i.e. target achieved and performance percentage
		columnArray.put("salarycomponentid.paidaftermonth");
		columnArray.put("duemonthid");
		columnArray.put("amount");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom >= '" + monthFirstDate + "' and applicableto <= '" + monthLastDate + "' " + filters);
		object = ResourceEngine.query(query);
		JSONArray rows1 = object.getJSONArray("employeesalarycomponents");

		for (int counter = 0; counter < rows1.length(); counter++) {
			JSONObject object1 = rows1.getJSONObject(counter);
			rows.put(object1);
		}

		return rows;
	}

	public static String getYearName(long yearId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_years");
			JSONArray array = new JSONArray();
			array.put("name");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + yearId);
			JSONObject object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("organization_years");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				String yearName = rows.getJSONObject(0).optString("name");
				return yearName;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error occured while converting yearid into year name.." + e.getMessage());
		}
	}
}