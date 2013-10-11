package com.applane.resourceoriented.hris.scheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.services.PortEmployeesFromSAPandPortInHrmDarcl;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.applaneurlfetch.ApplaneURLFetch;
import com.applane.service.cron.ApplaneCron;
import com.google.apphosting.api.DeadlineExceededException;

public class PortEmployeeFromSapToHrmDarcl implements ApplaneTask, ApplaneCron {

	private static final String EMPLOYEE_DETAIL_LIST = "EmployeeDetailList";
	public static final String UPDATE_EMPLOYEE = "EmployeeDetailList";

	@Override
	public void cronCall() throws DeadlineExceededException {
		Object userId = 13652;
		String organization = "Darcl Logistics Limited";
		try {
			PunchingUtility.setValueInSession(organization, userId, ApplicationConstants.OWNER);
			// String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			JSONArray lastKeyArray = getLastSapId();
			String lastKey = "";
			if (lastKeyArray != null && lastKeyArray.length() > 0) {
				lastKey = Translator.stringValue(lastKeyArray.getJSONObject(0).opt("employeecode"));
			}
			if (lastKey.length() < 8) {
				lastKey = "00" + lastKey;
			}
			String urlString = "http://apps.darcl.com:9080/EmployeeDetailWS/employeedata/getEmployeeDetail?EmpID=" + lastKey;
			String employeesJsonObjectInString = ApplaneURLFetch.requestToRemoteService(urlString);
			LogUtility.writeError("on insert >> employeesJsonObjectInString >> " + employeesJsonObjectInString + " << urlString >> " + urlString);
			JSONObject employeeJSONObject = null;
			JSONArray employeeArray = new JSONArray();
			boolean isInsert = true;
			try {
				employeeJSONObject = new JSONObject(employeesJsonObjectInString);
				employeeArray = employeeJSONObject.getJSONArray(EMPLOYEE_DETAIL_LIST);
			} catch (Exception e) {
				LogUtility.writeLog("exception 1 >>" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
				throw new RuntimeException(e);
			}
			if (employeeArray != null && employeeArray.length() > 0) {
				new PortEmployeesFromSAPandPortInHrmDarcl().portEmployeesFromSapToHrm(employeeArray, 0, isInsert);
			} else {
				initiateTaskQueueAgain();
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("PortEmployeeFromSapToHrmDarcl >> Trace >> " + trace);
			throw new RuntimeException(trace);
		}
	}

	public static String getBackDate(String dateInString) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
		Date currentDate = (Date) formatter.parse(dateInString);
		Calendar cal = new GregorianCalendar();
		cal.setTime(currentDate);
		cal.add(Calendar.DATE, -1);
		String backDate = formatter.format(cal.getTime());
		return backDate;
	}

	private JSONArray getLastSapId() throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put("employeecode");
		JSONArray orders = new JSONArray();
		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, Updates.KEY);
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		orders.put(order);
		JSONObject query = new JSONObject();
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.RESOURCE, "hris_employees");
		query.put(Data.Query.MAX_ROWS, 1);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_employees");
	}

	public void initiateTaskQueueAgain() throws Exception {
		JSONObject taskQueueParameters = new JSONObject();
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.scheduler.PortEmployeeFromSapToHrmDarcl", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);
	}

	@Override
	public void initiate(ApplaneTaskInfo taskInfoObject) throws DeadlineExceededException {
		try {
			Object userId = 13652;
			String organization = "Darcl Logistics Limited";
			PunchingUtility.setValueInSession(organization, userId, ApplicationConstants.OWNER);
			boolean isInsert = false;

			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			String backDate = DataTypeUtilities.getBackDate(TODAY_DATE);
			backDate = backDate.substring(8) + "-" + backDate.substring(5, 7) + "-" + backDate.substring(0, 4);
			// LogUtility.writeError("backDate >> " + backDate + " << on update.");
			String urlString = "http://apps.darcl.com:9080/EmployeeDetailWS/employeedata/updateEmployeeDetail?DateFrom=" + backDate + "&DateTo=" + backDate;
			// String urlString = "http://apps.darcl.com:9080/EmployeeDetailWS/employeedata/updateEmployeeDetail?DateFrom=01-05-2013&DateTo=" + backDate;
			// String urlString = "http://apps.darcl.com:9080/EmployeeDetailWS/employeedata/updateEmployeeDetail?DateFrom=08-07-2013&DateTo=18-07-2013";

			String employeesJsonObjectInString = ApplaneURLFetch.requestToRemoteService(urlString);
			// LogUtility.writeError("employeesJsonObjectInString >> " + employeesJsonObjectInString + " << urlString >> " + urlString + " << backDate >> " + backDate);
			JSONObject employeeJSONObject = null;
			JSONArray employeeArray = new JSONArray();
			try {
				employeeJSONObject = new JSONObject(employeesJsonObjectInString);
				employeeArray = employeeJSONObject.getJSONArray(EMPLOYEE_DETAIL_LIST);
			} catch (Exception e) {
				LogUtility.writeError("exception update >>" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
				throw new RuntimeException(e);
			}
			LogUtility.writeError("employeeArray.length >> " + (employeeArray.length() - 0));
			if (employeeArray != null && employeeArray.length() > 0) {
				int counter = 0;
				// int counter = 400;
				new PortEmployeesFromSAPandPortInHrmDarcl().portEmployeesFromSapToHrm(employeeArray, counter, isInsert);
			}

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError("Employee Porting Exception (Sap to Hrm) Exception >> " + trace);
			throw new RuntimeException("Employee Porting Exception (Sap to Hrm) >> " + trace);
		}
	}
}
