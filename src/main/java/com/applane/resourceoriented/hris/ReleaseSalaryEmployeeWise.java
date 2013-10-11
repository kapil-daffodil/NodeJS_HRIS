package com.applane.resourceoriented.hris;

import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.google.apphosting.api.DeadlineExceededException;

public class ReleaseSalaryEmployeeWise implements ApplaneTask {

	private static final String GENERATING = "Generating";
	private static final String RELEASING = "Releasing";

	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		JSONObject taskQueueInfo = applaneTaskInfo.getTaskInfo();
		String tempValue = (String) taskQueueInfo.opt("tempValue");

		try {
			// String TODAY_DATE1 = new
			// SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(SystemParameters.getCurrentDateTime());
			JSONArray monthlyAttendanceArray = taskQueueInfo.getJSONArray("monthlyAttendanceArray");
			int monthlyAttendanceArrayLength = (Integer) taskQueueInfo.opt("monthlyAttendanceArrayLength");
			int counter = (Integer) taskQueueInfo.opt("counter");
			JSONObject monthlyAttendanceObject = monthlyAttendanceArray.getJSONObject(counter);
			int employeeid = Translator.integerValue(monthlyAttendanceObject.opt("employeeid"));

			JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(employeeid);

			if (employeeArray != null && employeeArray.length() > 0) {
				int yearId = Translator.integerValue(monthlyAttendanceObject.opt("yearid"));
				int monthId = Translator.integerValue(monthlyAttendanceObject.opt("monthid"));
				int branchId = Translator.integerValue(monthlyAttendanceObject.opt("employeeid.branchid"));

				String monthName = EmployeeReleasingSalaryServlet.getMonthName(monthId);
				String yearName = EmployeeReleasingSalaryServlet.getYearName(yearId);
				Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
				Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
				String monthFirstDateInString = Translator.getDateInString(monthFirstDate);
				String monthLastDateInString = Translator.getDateInString(monthLastDate);
				if (tempValue.equals(RELEASING)) {
					boolean generateInvoice = getGenerateInvoice(yearId, monthId, branchId);
					new EmployeeReleasingSalaryServlet().releaseSalarySheet(employeeArray, monthId, yearId, monthFirstDate, monthLastDate, monthName, yearName, monthFirstDateInString, monthLastDateInString, generateInvoice);
				} else if (tempValue.equals(GENERATING)) {

					JSONObject fields = new JSONObject();
					double maxDaysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
					EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = null;
					HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns> monthlyAttendanceMap = new HashMap<Integer, EmployeeMonthlyAttandanceUpdateAllColumns>();
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

					EmployeeSalaryGenerationServlet.populateSalaryComponents(employeeArray, fields);
				}
			}
			counter++;
			if (counter < (monthlyAttendanceArrayLength)) {
				initiateTaqskQueue(monthlyAttendanceArray, monthlyAttendanceArrayLength, counter, RELEASING, null);
			}
		} catch (Exception e) {
			new EmployeeReleasingSalaryServlet().sendMailInCaseOfException(e);
		}
	}

	public static boolean getGenerateInvoice(int yearId, int monthId, int branchId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takesalarysheets");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("make_invoice");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "branchid = " + branchId + " and monthid=" + monthId + " and yearid=" + yearId);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("takesalarysheets");
		if (rows != null && rows.length() > 0) {
			return Translator.booleanValue(rows.getJSONObject(0).opt("make_invoice"));
		}
		return false;
	}

	public void releaseSelectdEmployeeSalaryInvokeMethodFromSalarySheet(Object[] keys) {
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
				JSONArray monthlyAttendanceArray = getEmployeeIdFromSalarySheet(selectedKeys);
				int salarySheetArrayLength = monthlyAttendanceArray.length();
				int counter = 0;
				String tempValue = RELEASING;
				JSONObject monthlyAttendanceObject = monthlyAttendanceArray.getJSONObject(counter);
				int yearId = Translator.integerValue(monthlyAttendanceObject.opt("yearid"));
				int monthId = Translator.integerValue(monthlyAttendanceObject.opt("monthid"));
				int branchId = Translator.integerValue(monthlyAttendanceObject.opt("employeeid.branchid"));

				boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.SALARY);
				if (isFreezed) {
					throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
				}

				initiateTaqskQueue(monthlyAttendanceArray, salarySheetArrayLength, counter, tempValue, null);
			}
		} catch (Exception e) {
			new EmployeeReleasingSalaryServlet().sendMailInCaseOfException(e);
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public void releaseSelectdEmployeeSalaryInvokeMethodFromSetupMonthlyAmount(Object[] keys) {
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
				JSONArray monthlyAttendanceArray = getEmployeeMonthlyAttendanceToRelease(selectedKeys);
				{
					int salarySheetArrayLength = monthlyAttendanceArray.length();
					int counter = 0;
					String tempValue = RELEASING;
					JSONObject monthlyAttendanceObject = monthlyAttendanceArray.getJSONObject(counter);
					int yearId = Translator.integerValue(monthlyAttendanceObject.opt("yearid"));
					int monthId = Translator.integerValue(monthlyAttendanceObject.opt("monthid"));
					int branchId = Translator.integerValue(monthlyAttendanceObject.opt("employeeid.branchid"));

					boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.SALARY);
					if (isFreezed) {
						throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
					}

					initiateTaqskQueue(monthlyAttendanceArray, salarySheetArrayLength, counter, tempValue, null);
				}
			}
		} catch (Exception e) {
			new EmployeeReleasingSalaryServlet().sendMailInCaseOfException(e);
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public void generateSelectdEmployeeSalaryInvokeMethodFromSetupMonthlyAmount(Object[] keys) {
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
				JSONArray monthlyAttendanceArray = getEmployeeMonthlyAttendanceToRelease(selectedKeys);
				{
					int salarySheetArrayLength = monthlyAttendanceArray.length();
					int counter = 0;
					String tempValue = GENERATING;

					JSONObject monthlyAttendanceObject = monthlyAttendanceArray.getJSONObject(counter);
					int yearId = Translator.integerValue(monthlyAttendanceObject.opt("yearid"));
					int monthId = Translator.integerValue(monthlyAttendanceObject.opt("monthid"));
					int branchId = Translator.integerValue(monthlyAttendanceObject.opt("employeeid.branchid"));

					boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.SALARY);
					if (isFreezed) {
						throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
					}

					initiateTaqskQueue(monthlyAttendanceArray, salarySheetArrayLength, counter, tempValue, null);
				}
			}
		} catch (Exception e) {
			new EmployeeReleasingSalaryServlet().sendMailInCaseOfException(e);
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private JSONArray getEmployeeMonthlyAttendanceToRelease(String selectedKeys) throws JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("employeeid");
		array.put("yearid");
		array.put("monthid");
		array.put("employeeid.branchid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, Updates.KEY + " in (" + selectedKeys + ")");
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeemonthlyattendance");
		return rows;
	}

	private void initiateTaqskQueue(JSONArray monthlyAttendanceArray, int monthlyAttendanceArrayLength, int counter, String tempValue, JSONArray employeeList) throws JSONException {
		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("monthlyAttendanceArrayLength", monthlyAttendanceArrayLength);
		taskQueueParameters.put("monthlyAttendanceArray", monthlyAttendanceArray);
		taskQueueParameters.put("counter", counter);
		taskQueueParameters.put("tempValue", tempValue);
		if (employeeList != null) {
			taskQueueParameters.put("employeeList", employeeList);
		}
		ApplaneTaskService.enQueueTaskInPrallel("com.applane.resourceoriented.hris.ReleaseSalaryEmployeeWise", QueueConstantsHRIS.SALARY_PROCESS, taskQueueParameters);
	}

	public JSONArray getEmployeeIdFromSalarySheet(String keys) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.branchid");
		columnArray.put("monthid");
		columnArray.put("yearid");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + keys + ")");
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("salarysheet");
		return salarySheetArray;

	}

}
