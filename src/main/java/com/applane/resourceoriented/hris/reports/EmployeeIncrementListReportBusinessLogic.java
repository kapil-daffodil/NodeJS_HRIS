package com.applane.resourceoriented.hris.reports;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeIncrementListReportBusinessLogic implements QueryJob {

	public void onQuery(Query query) throws DeadlineExceededException {

	}

	public void onResult(Result result) throws DeadlineExceededException {
		try {
			JSONArray array = result.getRecords();
			if (array != null && array.length() > 0) {
				int branchId = Translator.integerValue(array.getJSONObject(0).opt("branchid"));
				int monthId = Translator.integerValue(array.getJSONObject(0).opt("monthid"));
				int yearId = Translator.integerValue(array.getJSONObject(0).opt("yearid"));

				String monthName = Translator.stringValue(array.getJSONObject(0).opt("monthid.name"));
				String yearName = Translator.stringValue(array.getJSONObject(0).opt("yearid.name"));
				JSONObject fields = getUsefullFields(branchId, monthId, yearId, monthName, yearName);

				JSONArray finalResult = getIncrementedEmployeeList(fields);
				JSONObject object = new JSONObject();
				object.put("finalResult", finalResult);
				array.put(object);

			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeIncrementListReportBusinessLogic.class.getName(), e);
			LogUtility.writeLog("EmployeeIncrementListReportBusinessLogic Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Error Occured, Please Contace to Admin");
		}
	}

	private JSONArray getIncrementedEmployeeList(JSONObject fields) throws JSONException, ParseException {
		Date monthFirstDate = (Date) fields.opt("monthFirstDate");
		Date monthLastDate = (Date) fields.opt("monthLastDate");
		String monthFirstDateInString = fields.optString("monthFirstDateInString");
		String monthLastDateInString = fields.optString("monthLastDateInString");

		Date nextMonthFirstDate = (Date) fields.opt("nextMonthFirstDate");
		Date nextMonthLastDate = (Date) fields.opt("nextMonthLastDate");
		String nextMonthFirstDateInString = fields.optString("nextMonthFirstDateInString");
		String nextMonthLastDateInString = fields.optString("nextMonthLastDateInString");

		int branchId = fields.optInt("branchId");
		JSONArray employeeArray = getActiveAndReleivedEmployeeRecords(branchId, monthFirstDateInString);
		JSONArray incrementedEmployeeListInCurrentMonth = new JSONArray();
		JSONArray incrementDueEmployeeListInNextMonth = new JSONArray();

		JSONArray salaryComponents = new EmployeeSalarySheetReport().getSalaryComponents();
		ArrayList<Integer> ignoreableComponentList = getIgnoreAbleComponentList(salaryComponents);

		for (int counter = 0; counter < employeeArray.length(); counter++) {

			Object employeeId = employeeArray.getJSONObject(counter).opt(Updates.KEY);
			Object employeeName = employeeArray.getJSONObject(counter).opt("name");
			JSONArray employeeSalaryComponentsCurrentMonth = EmployeeSalaryGenerationServlet.getEmployeeSalaryComponents(monthFirstDateInString, monthLastDateInString, employeeId);
			JSONArray employeeSalaryComponentsNextMonth = EmployeeSalaryGenerationServlet.getEmployeeSalaryComponents(nextMonthFirstDateInString, nextMonthLastDateInString, employeeId);

			String incrementDateCurrentMonth = "";
			boolean isIncrementCurrentMonth = false;
			String incrementDateNextMonth = "";
			boolean isIncrementNextMonth = false;
			boolean checkForNextMonth = true;
			for (int salaryComponentCounter = 0; salaryComponentCounter < employeeSalaryComponentsCurrentMonth.length(); salaryComponentCounter++) {
				Integer salaryComponentId = Translator.integerValue(employeeSalaryComponentsCurrentMonth.getJSONObject(salaryComponentCounter).opt("salarycomponentid"));

				Object amount = employeeSalaryComponentsCurrentMonth.getJSONObject(salaryComponentCounter).opt("amount");
				if (!ignoreableComponentList.contains(salaryComponentId) && amount != null && Translator.doubleValue(amount) > 0.0) {
					Date applicableToDate = Translator.dateValue(employeeSalaryComponentsCurrentMonth.getJSONObject(salaryComponentCounter).opt("applicableto"));
					Date applicableFromDate = Translator.dateValue(employeeSalaryComponentsCurrentMonth.getJSONObject(salaryComponentCounter).opt("applicablefrom"));
					if ((applicableToDate != null && monthFirstDate.before(applicableToDate) && monthLastDate.after(applicableToDate)) || (monthFirstDate.equals(applicableToDate))) {
						isIncrementCurrentMonth = true;
						incrementDateCurrentMonth = DataTypeUtilities.getNextDate(EmployeeSalaryGenerationServlet.getDateInString(applicableToDate));
						break;
					} else if (applicableFromDate != null && applicableFromDate.equals(monthFirstDate)) {
						isIncrementCurrentMonth = true;
						incrementDateCurrentMonth = monthFirstDateInString;
						break;
					} else if (applicableToDate != null && applicableToDate.equals(monthLastDate)) {
						isIncrementNextMonth = true;
						checkForNextMonth = false;
						incrementDateNextMonth = nextMonthFirstDateInString;
						break;
					}
				}
			}
			if (checkForNextMonth) {
				for (int salaryComponentCounter = 0; salaryComponentCounter < employeeSalaryComponentsNextMonth.length(); salaryComponentCounter++) {
					Integer salaryComponentId = Translator.integerValue(employeeSalaryComponentsNextMonth.getJSONObject(salaryComponentCounter).opt("salarycomponentid"));
					Object amount = employeeSalaryComponentsNextMonth.getJSONObject(salaryComponentCounter).opt("amount");
					if (!ignoreableComponentList.contains(salaryComponentId) && amount != null && Translator.doubleValue(amount) > 0.0) {
						Date applicableToDate = Translator.dateValue(employeeSalaryComponentsNextMonth.getJSONObject(salaryComponentCounter).opt("applicableto"));
						Date applicableFromDate = Translator.dateValue(employeeSalaryComponentsNextMonth.getJSONObject(salaryComponentCounter).opt("applicablefrom"));
						if ((applicableToDate != null && nextMonthFirstDate.before(applicableToDate) && nextMonthLastDate.after(applicableToDate)) || (monthFirstDate.equals(applicableToDate))) {
							isIncrementNextMonth = true;
							incrementDateNextMonth = DataTypeUtilities.getNextDate(EmployeeSalaryGenerationServlet.getDateInString(applicableToDate));
							break;
						} else if (applicableFromDate != null && applicableFromDate.equals(nextMonthFirstDate)) {
							isIncrementNextMonth = true;
							incrementDateNextMonth = nextMonthFirstDateInString;
							break;
						}
					}
				}
			}
			if (isIncrementCurrentMonth) {
				JSONObject object = new JSONObject();
				object.put("employeeName", employeeName);
				object.put("incrementDate", incrementDateCurrentMonth);
				incrementedEmployeeListInCurrentMonth.put(object);
			}
			if (isIncrementNextMonth) {
				JSONObject object = new JSONObject();
				object.put("employeeName", employeeName);
				object.put("incrementDate", incrementDateNextMonth);
				incrementDueEmployeeListInNextMonth.put(object);
			}
		}
		JSONObject object = new JSONObject();
		object.put("incrementedEmployeeListInCurrentMonth", incrementedEmployeeListInCurrentMonth);
		object.put("incrementDueEmployeeListInNextMonth", incrementDueEmployeeListInNextMonth);
		object.put("currentMonthName", fields.optString("monthName"));
		object.put("nextMonthName", fields.optString("nextMonthName"));

		JSONArray finalResult = new JSONArray();
		finalResult.put(object);
		return finalResult;
	}

	private ArrayList<Integer> getIgnoreAbleComponentList(JSONArray salaryComponents) throws JSONException {
		ArrayList<Integer> ignoreableComponentList = new ArrayList<Integer>();
		for (int counter = 0; counter < salaryComponents.length(); counter++) {
			Integer salaryComponentId = DataTypeUtilities.integerValue(salaryComponents.getJSONObject(counter).opt(Updates.KEY));
			Object ignoreInExceptionReport = DataTypeUtilities.integerValue(salaryComponents.getJSONObject(counter).opt("ignoreinexceptionreport"));
			if (salaryComponentId != null && ignoreInExceptionReport != null && Translator.integerValue(ignoreInExceptionReport) == HRISApplicationConstants.EmployeeDecision.YES) {
				ignoreableComponentList.add(salaryComponentId);
			}
		}
		return ignoreableComponentList;
	}

	private JSONObject getUsefullFields(int branchId, int monthId, int yearId, String monthName, String yearName) throws ParseException, JSONException {

		Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
		String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
		String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);

		Calendar cal = Calendar.getInstance();
		cal.setTime(monthFirstDate);
		cal.add(Calendar.MONTH, 1);

		String nextMonthName = EmployeeSalaryGenerationServlet.getMonthName(cal.get(Calendar.MONTH) + 1);
		String nextYearName = "" + cal.get(Calendar.YEAR);

		Date nextMonthFirstDate = DataTypeUtilities.getMonthFirstDate(nextYearName, nextMonthName);
		Date nextMonthLastDate = DataTypeUtilities.getMonthLastDate(nextMonthFirstDate);
		String nextMonthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(nextMonthFirstDate);
		String nextMonthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(nextMonthLastDate);

		JSONObject fields = new JSONObject();
		fields.put("branchId", branchId);
		fields.put("monthId", monthId);
		fields.put("yearId", yearId);
		fields.put("monthName", monthName);
		fields.put("yearName", yearName);
		fields.put("nextMonthName", nextMonthName);
		fields.put("nextYearName", nextYearName);
		fields.put("monthFirstDate", monthFirstDate);
		fields.put("monthLastDate", monthLastDate);
		fields.put("monthFirstDateInString", monthFirstDateInString);
		fields.put("monthLastDateInString", monthLastDateInString);
		fields.put("nextMonthFirstDate", nextMonthFirstDate);
		fields.put("nextMonthLastDate", nextMonthLastDate);
		fields.put("nextMonthFirstDateInString", nextMonthFirstDateInString);
		fields.put("nextMonthLastDateInString", nextMonthLastDateInString);
		return fields;
	}

	public JSONArray getActiveAndReleivedEmployeeRecords(int branchId, String monthFirstDateInString) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("name");
		columnArray.put("joiningdate");
		columnArray.put("relievingdate");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + " and branchid = " + branchId);
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and branchid = " + branchId + " and relievingdate >= '" + monthFirstDateInString + "'");
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, Updates.KEY);
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		query.put(Data.Query.ORDERS, orders);
		// query.put(Data.Query.MAX_ROWS, 10);

		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}
}
