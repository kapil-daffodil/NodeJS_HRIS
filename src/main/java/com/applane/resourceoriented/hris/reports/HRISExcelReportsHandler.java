package com.applane.resourceoriented.hris.reports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;

@Path("/handlehrisexcelreports")
public class HRISExcelReportsHandler {

	@GET
	@Path("/employeemonthlyattendance")
	public Response employeeMonthlyAttendanceGet(@Context HttpServletRequest request) throws IOException {
		return employeeMonthlyAttendancePost(request);
	}

	@POST
	@Path("/employeemonthlyattendance")
	public Response employeeMonthlyAttendancePost(@Context HttpServletRequest request) throws IOException {
		String selectedKeys = request.getParameter("keys");
		try {
			if (selectedKeys != null) {
				JSONObject parameters = getParameters(new JSONArray(selectedKeys).get(0));
				LogUtility.writeError("parameters: " + parameters);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				EmployeeMonthlyAttendanceReport.generateReport(outputStream, parameters);
				if (outputStream.size() > 0) {
					ResponseBuilder responseBuilder = Response.ok(outputStream.toByteArray(), "application/vnd.ms-excel");
					responseBuilder.header("Content-Disposition", "attachment; Filename=\"" + getMonthName(parameters.get("fromdate")) + "-Attendance" + "\"");
					return responseBuilder.build();
				}
			}
			return Response.ok(200).entity("Please try after some time.").build();
		} catch (Exception e) {
			LogUtility.writeError(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			return Response.ok(200).entity("Please try after some time.").build();
		}
	}

	private JSONObject getParameters(Object selectedKey) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "pfreportforyear");
		query.put(Data.Query.FILTERS, Data.Query.KEY + "={selectedKey__}");
		query.put(Data.Query.COLUMNS, new JSONArray().put("branchid").put("departmentid").put("fromdate").put("todate"));
		query.put(Data.Query.PARAMETERS, new JSONObject().put("selectedKey__", selectedKey));
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("pfreportforyear").getJSONObject(0);
	}

	public static String getMonthName(Object date) throws JSONException {
		Calendar calendarDate = getCalendarDate(date);
		int monthId = calendarDate.get(Calendar.MONTH);
		JSONArray monthsArray = new JSONArray("[January,February,March,April,May,June,July,August,September,October,November,December]");
		int length = monthsArray.length();
		int i;
		for (i = 0; i < length; i++) {
			if (i == monthId) {
				return monthsArray.getString(i) + ", " + calendarDate.get(Calendar.YEAR);
			}
		}
		return "";
	}

	public static int getNoOfDays(Object date) throws JSONException {
		Calendar calendarDate = getCalendarDate(date);
		return getTotalDaysInMonth(calendarDate);
	}

	public static int getTotalDaysInMonth(Calendar calendarFromDate) {
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(calendarFromDate.getTime());
		startDate.set(Calendar.MONTH, calendarFromDate.get(Calendar.MONTH));
		startDate.set(Calendar.DAY_OF_MONTH, 1);
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(calendarFromDate.getTime());
		endDate.set(Calendar.MONTH, calendarFromDate.get(Calendar.MONTH) + 1);
		endDate.set(Calendar.DAY_OF_MONTH, 1);
		endDate.setTimeInMillis(endDate.getTimeInMillis() - 24 * 60 * 60 * 1000);
		int firstDay = startDate.get(Calendar.DAY_OF_MONTH);
		int lastDay = endDate.get(Calendar.DAY_OF_MONTH);
		return (lastDay - firstDay + 1);
	}

	public static Calendar getCalendarDate(Object date) {
		if (date instanceof Calendar) {
			date = new SimpleDateFormat("yyyy-MM-dd").format(((Calendar) date).getTime());
		} else if (date instanceof Date) {
			date = new SimpleDateFormat("yyyy-MM-dd").format(date);
		}
		String[] temp = date.toString().split(" ");
		String[] parts = temp[0].split("-");
		if (temp[0].contains("/")) {
			parts = temp[0].split("/");
		}
		Calendar calendarDate = Calendar.getInstance();
		calendarDate.set(Calendar.YEAR, Integer.valueOf(parts[0]));
		calendarDate.set(Calendar.MONTH, Integer.valueOf(parts[1]) - 1);
		calendarDate.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parts[2]));
		calendarDate.set(Calendar.HOUR, 0);
		calendarDate.set(Calendar.MINUTE, 0);
		calendarDate.set(Calendar.SECOND, 0);
		calendarDate.set(Calendar.MILLISECOND, 0);
		return calendarDate;
	}

	public static Object getPreviousMonth() {
		Integer currentMonth = (Integer) SystemParameters.getCurrentMonth();
		return currentMonth - 1 == 0 ? 12 : currentMonth - 1;
	}
}