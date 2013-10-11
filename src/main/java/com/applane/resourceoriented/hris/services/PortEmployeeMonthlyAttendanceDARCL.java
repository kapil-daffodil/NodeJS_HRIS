package com.applane.resourceoriented.hris.services;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.applaneurlfetch.ApplaneURLFetch;
import com.applane.service.cron.ApplaneCron;
import com.google.apphosting.api.DeadlineExceededException;

public class PortEmployeeMonthlyAttendanceDARCL implements ApplaneTask, ApplaneCron {

	private static final String EMPLOYEE_DETAIL_LIST = "EmployeeDetailList";

	public void cronCall() throws DeadlineExceededException {
		try {
			String organization = "Darcl Logistics Limited";
			Object userId = 13652;
			PunchingUtility.setValueInSession(organization, userId, ApplicationConstants.OWNER);
			portEmployeeMonthlyAttendance(null);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("PortEmployeeFromSapToHrmDarcl >> Trace >> " + trace);
			throw new RuntimeException(trace);
		}
	}

	private void portEmployeeMonthlyAttendance(Object lastEmployeeId) throws Exception {

		JSONArray employeeArray = getEmployees(lastEmployeeId);

		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		Date currentDate = Translator.dateValue(TODAY_DATE);

		Calendar cal = Calendar.getInstance();
		cal.setTime(currentDate);
		cal.add(Calendar.MONTH, -1);

		long monthId = cal.get(Calendar.MONTH) + 1;
		long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
		int employeeArrayLength = (employeeArray == null) ? 0 : employeeArray.length();

		for (int counter = 0; counter < employeeArrayLength; counter++) {

			String lastEmployeeCode = Translator.stringValue(employeeArray.getJSONObject(counter).opt("employeecode"));
			Object lastKey = employeeArray.getJSONObject(counter).opt("__key__");

			if (lastEmployeeCode.length() < 8) {
				lastEmployeeCode = "00" + lastEmployeeCode;
			}

			lastEmployeeId = lastKey;

			// String urlString = "http://apps.darcl.com:9080/EmployeeDetailWS/employeedata/getEmployeeMonthlyAttendanceDetail?EmployeeID=00100691&Month=04&Year=2013";
			String urlString = "http://apps.darcl.com:9080/EmployeeDetailWS/employeedata/getEmployeeMonthlyAttendanceDetail?EmployeeID=" + lastEmployeeCode + "&Month=" + monthId + "&Year=" + cal.get(Calendar.YEAR);

			String employeesJsonObjectInString = ApplaneURLFetch.requestToRemoteService(urlString);
			// LogUtility.writeLog("Employee Json Object : " + employeesJsonObjectInString);

			JSONObject employeeJSONObject = null;
			JSONArray monthlyAttendanceArray = new JSONArray();

			try {
				employeeJSONObject = new JSONObject(employeesJsonObjectInString);
				monthlyAttendanceArray = employeeJSONObject.getJSONArray(EMPLOYEE_DETAIL_LIST);

				if (monthlyAttendanceArray != null && monthlyAttendanceArray.length() > 0) {

					JSONArray employeeMonthlyAttendance = getEmployeeMonthlyAttendance(lastKey, monthId, yearId);
					Object monthlyAttendanceId = null;

					if (employeeMonthlyAttendance != null && employeeMonthlyAttendance.length() > 0) {
						monthlyAttendanceId = employeeMonthlyAttendance.getJSONObject(0).opt("__key__");
					}

					insertOrUpdateMonthlyAttendance(monthlyAttendanceId, monthlyAttendanceArray, lastKey, monthId, yearId);
				}
			} catch (Exception e) {
				LogUtility.writeLog("exception 1 >>" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
				throw new RuntimeException(e);
			}
		}

		if (employeeArrayLength == 50) {
			initiateTaskQueueAgain(lastEmployeeId);
		}
	}

	private void insertOrUpdateMonthlyAttendance(Object monthlyAttendanceId, JSONArray monthlyAttendanceArray, Object lastKey, long monthId, long yearId) throws Exception {

		JSONObject monthlyAttendanceObject = monthlyAttendanceArray.getJSONObject(0);

		Object NetEWDBal = monthlyAttendanceObject.opt("NetEWDBal");
		// Object ManualUpdate = monthlyAttendanceObject.opt("ManualUpdate");
		Object MLOpeningBal = monthlyAttendanceObject.opt("MLOpeningBal");
		Object ELOpeningBal = monthlyAttendanceObject.opt("ELOpeningBal");
		Object CLOpeningBal = monthlyAttendanceObject.opt("CLOpeningBal");
		Object ExtraWorkingDays = monthlyAttendanceObject.opt("ExtraWorkingDays");
		// Object month = monthlyAttendanceObject.opt("Month");
		Object TotalOff = monthlyAttendanceObject.opt("TotalOff");
		Object TotalWorkingDays = monthlyAttendanceObject.opt("TotalWorkingDays");
		Object EWDOpeningBal = monthlyAttendanceObject.opt("EWDOpeningBal");
		Object SLOpeningBal = monthlyAttendanceObject.opt("SLOpeningBal");
		Object SalaryDays = monthlyAttendanceObject.opt("SalaryDays");
		Object CreditEL = monthlyAttendanceObject.opt("CreditEL");
		Object Holiday = monthlyAttendanceObject.opt("Holiday");
		Object CLClosingBal = monthlyAttendanceObject.opt("CLClosingBal");
		Object SLClosingBal = monthlyAttendanceObject.opt("SLClosingBal");
		Object UsedCL = monthlyAttendanceObject.opt("UsedCL");
		Object PayableAbsents = monthlyAttendanceObject.opt("PayableAbsents");
		Object NonPayableLeaves = monthlyAttendanceObject.opt("NonPayableLeaves");
		Object MLClosingBal = monthlyAttendanceObject.opt("MLClosingBal");
		Object NonPayableAbsents = monthlyAttendanceObject.opt("NonPayableAbsents");
		Object UsedEL = monthlyAttendanceObject.opt("UsedEL");
		Object Leaves = monthlyAttendanceObject.opt("Leaves");
		Object UsedSL = monthlyAttendanceObject.opt("UsedSL");
		Object PresentDays = monthlyAttendanceObject.opt("PresentDays");
		// Object Employee = monthlyAttendanceObject.opt("Employee");
		// Object EmployeeCode = monthlyAttendanceObject.opt("EmployeeCode");
		Object Absents = monthlyAttendanceObject.opt("Absents");
		Object ELClosingBal = monthlyAttendanceObject.opt("ELClosingBal");
		Object PayableLeaves = monthlyAttendanceObject.opt("PayableLeaves");
		Object EWDClosingBalance = monthlyAttendanceObject.opt("EWDClosingBalance");
		Object NonWorkingDay = monthlyAttendanceObject.opt("NonWorkingDay");

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");

		JSONObject row = new JSONObject();
		if (monthlyAttendanceId != null) {
			row.put(Updates.KEY, monthlyAttendanceId);
		}
		row.put("carryextraworkingday", NetEWDBal);
		row.put("medicalleaveopeningbalance", MLOpeningBal);
		row.put("earnedleaveopeningbalance", ELOpeningBal);
		row.put("casualleaveopeningbalance", CLOpeningBal);
		row.put("extraworkingdays", ExtraWorkingDays);
		row.put("nonworkingdays", TotalOff);
		row.put("totalworkingdays", TotalWorkingDays);
		row.put("extraworkingdayopeningbalance", EWDOpeningBal);
		row.put("specialleaveopeningbalance", SLOpeningBal);
		// Salary Days Not in Applane
		row.put("creditearnedleaves", CreditEL);
		row.put("holidays", Holiday);
		row.put("casualleaveclosingbalance", CLClosingBal);
		row.put("specialleaveclosingbalance", SLClosingBal);
		row.put("usedcasualleaves", UsedCL);
		row.put("paidabsents", PayableAbsents);
		row.put("nonpayableleaves", NonPayableLeaves);
		row.put("medicalleaveclosingbalance", MLClosingBal);
		row.put("nonpaidabsents", NonPayableAbsents);
		row.put("usedearnedleaves", UsedEL);
		row.put("leaves", Leaves);
		row.put("usedspecialleaves", UsedSL);
		row.put("presentdays", PresentDays);
		row.put("absents", Absents);
		row.put("earnedleaveclosingbalance", ELClosingBal);
		row.put("payableleaves", PayableLeaves);
		row.put("extraworkingdaybalance", EWDClosingBalance);
		row.put("actualnonworkingdays", NonWorkingDay);

		row.put("employeeid", lastKey);
		row.put("yearid", yearId);
		row.put("monthid", monthId);

		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		row.put("ismanualupdate", 1);

		updates.put(Data.Update.UPDATES, row);

		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private JSONArray getEmployeeMonthlyAttendance(Object lastKey, long monthId, long yearId) throws Exception {

		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");

		JSONArray columns = new JSONArray();
		columns.put("__key__");
		columns.put("ismanualupdate");
		columns.put("presentdays");
		columns.put("leaves");
		columns.put("usedearnedleaves");
		columns.put("usedcasualleaves");
		columns.put("usedmedicalleaves");

		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, "employeeid = " + lastKey + " and monthid = " + monthId + " and yearid = " + yearId);

		JSONObject attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeemonthlyattendance");

		// LogUtility.writeLog("Employee Monthly Attendance : " + rows);

		return rows;
	}

	private JSONArray getEmployees(Object lastEmployeeId) throws Exception {

		JSONArray columns = new JSONArray();
		columns.put(Data.Query.KEY);
		columns.put("employeecode");

		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, Updates.KEY);
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);

		JSONArray orders = new JSONArray();
		orders.put(order);

		JSONObject query = new JSONObject();
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.COLUMNS, columns);
		if (lastEmployeeId != null) {
			query.put(Data.Query.FILTERS, "__key__ > " + lastEmployeeId);
		}
		query.put(Data.Query.RESOURCE, "hris_employees");
		query.put(Data.Query.MAX_ROWS, 50);

		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_employees");
	}

	private void initiateTaskQueueAgain(Object lastEmployeeId) throws Exception {
		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("lastKey", lastEmployeeId);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.services.PortEmployeeMonthlyAttendanceDARCL", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);
	}

	@Override
	public void initiate(ApplaneTaskInfo taskInfo) throws DeadlineExceededException {
		try {
			JSONObject parameters = taskInfo.getTaskInfo();
			portEmployeeMonthlyAttendance(parameters.opt("lastKey"));
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError("Port monthly attendance DARCL: exception trace >> " + trace);
			throw new RuntimeException(trace);
		}
	}
}
