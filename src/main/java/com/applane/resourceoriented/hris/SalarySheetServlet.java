package com.applane.resourceoriented.hris;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class SalarySheetServlet extends HttpServlet {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.err.println("Call on MonthlyAttendanceServlet doPost()....");
		generateSalarySheet(req, resp);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		System.err.println("In doGet Method...");
	}

	public static void generateSalarySheet(HttpServletRequest req, HttpServletResponse resp) {
		System.err.println("SalarySheetServlet class ---->generateSalarySheet Method call...");
		Map<String, Object> paramMap = req.getParameterMap();
		try {
			if (paramMap == null || paramMap.size() == 0) {
				System.err.println("No Parameters found.....");
				resp.getWriter().write("No parameter found in Attendance servlet..");
			} else {
				System.err.println("Parameters found...");
				String empId = ((String[]) paramMap.get("employeeid"))[0];
				System.err.println("empId is : >> " + empId);

				long employeeId = 0;
				if (empId != null && empId.length() > 0) {
					System.err.println("empId not equal to null...");
					employeeId = Long.parseLong(empId);
					System.err.println("employeeId is : >> " + employeeId);
				}

				String monthIdInString = ((String[]) paramMap.get("monthid"))[0];
				System.err.println("monthIdInString is : >> " + monthIdInString);

				long monthId = 0;
				if (monthIdInString != null && monthIdInString.length() > 0) {
					System.err.println("monthIdInString not equal to null...");
					monthId = Long.parseLong(monthIdInString);
					System.err.println("monthId is : >> " + monthId);
				}
				String yearIdInString = ((String[]) paramMap.get("yearid"))[0];
				System.err.println("yearIdInString is : >> " + yearIdInString);

				long yearId = 0;
				if (yearIdInString != null && yearIdInString.length() > 0) {
					System.err.println("yearIdInString not equal to null...");
					yearId = Long.parseLong(yearIdInString);
					System.err.println("yearId is : >> " + yearId);
				}

				String branchIdInstring = ((String[]) paramMap.get("branchid"))[0];
				System.err.println("branchIdInstring is : >> " + branchIdInstring);

				long branchId = 0;
				if (branchIdInstring != null && branchIdInstring.length() > 0) {
					System.err.println("branchIdInstring not equal to null...");
					branchId = Long.parseLong(branchIdInstring);
					System.err.println("branchId is : >> " + branchId);
				}

				JSONArray monthlyattendanceArray = getEmployeeMonthlyAttendanceRecords(employeeId, monthId, yearId);
				System.err.println("monthlyattendanceArray is : >>> " + monthlyattendanceArray);

				int monthlyattendanceArrayCount = (monthlyattendanceArray == null || monthlyattendanceArray.length() == 0) ? 0 : monthlyattendanceArray.length();
				System.err.println("monthlyattendanceArrayCount is : >> " + monthlyattendanceArrayCount);

				if (monthlyattendanceArrayCount > 0) {
					System.err.println("monthlyattendanceArrayCount is greater than zero.....");

					Object monthlyAttendanceId = monthlyattendanceArray.getJSONObject(0).opt("__key__");
					System.err.println("monthlyAttendanceId is : >>> " + monthlyAttendanceId);

					Object presentDays = monthlyattendanceArray.getJSONObject(0).opt("presentdays") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).opt("presentdays");
					System.err.println("presentDays is : >> " + presentDays);

					Object absentDays = monthlyattendanceArray.getJSONObject(0).opt("absents") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).opt("absents");
					System.err.println("absentDays is ; >> " + absentDays);

					Object leaves = monthlyattendanceArray.getJSONObject(0).opt("leaves") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).opt("leaves");
					System.err.println("leaves is : >> " + leaves);

					Object extraWorkingDays = monthlyattendanceArray.getJSONObject(0).opt("extraworkingdays") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).opt("extraworkingdays");
					System.err.println("extraWorkingDays is : >>> " + extraWorkingDays);

					updateAttendanceInSalarySheet(employeeId, monthId, yearId, presentDays, absentDays, leaves, extraWorkingDays);
				} else {
					System.err.println("No monthly attendance found for employee [" + employeeId + "]");
				}
			}
		} catch (DeadlineExceededException e) {
			System.err.println("Throw deadline exception.");
			String message = ExceptionUtils.getExceptionTraceMessage("SalarySheetServlet", e);
			System.err.println("message is : >> " + message);
			LogUtility.writeError(message);
			throw e;
		} catch (BusinessLogicException e) {
			System.err.println("Throw business logic exception");
			String message = ExceptionUtils.getExceptionTraceMessage("SalarySheetServlet", e);
			System.err.println("message is : >> " + message);
			LogUtility.writeError(message);
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			String message = ExceptionUtils.getExceptionTraceMessage("SalarySheetServlet", e);
			System.err.println("message is : >> " + message);
			LogUtility.writeError(message);
			throw new BusinessLogicException("Some unknown error occured while update employee salary sheet.");
		}

	}

	private static void updateAttendanceInSalarySheet(long employeeId, long monthId, long yearId, Object presentDays, Object absentDays, Object leaves, Object extraWorkingDays) throws JSONException {
		System.err.println("updateAttendanceInSalarySheet Method call....");
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "salarysheet");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("monthid", monthId);
		row.put("yearid", yearId);
		row.put("present", presentDays);
		row.put("absent", absentDays);
		row.put("leaves", leaves);
		row.put("extraworkingday", extraWorkingDays);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
		System.err.println("updateAttendanceInSalarySheet succesfully....");
	}

	private static JSONArray getEmployeeMonthlyAttendanceRecords(long employeeId, long monthId, long yearId) throws JSONException {
		System.err.println("getEmployeeMonthlyAttendanceRecords Method call....");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("presentdays");
		columnArray.put("absents");
		columnArray.put("leaves");
		columnArray.put("extraworkingdays");
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and yearid = " + yearId + " and  monthid = " + monthId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeemonthlyattendance");
		return rows;
	}
}
