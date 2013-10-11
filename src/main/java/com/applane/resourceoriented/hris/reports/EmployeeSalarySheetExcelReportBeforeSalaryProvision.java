package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;

public class EmployeeSalarySheetExcelReportBeforeSalaryProvision extends HttpServlet {

	private static final long serialVersionUID = 1L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			instantiateClassVariablesAndGenerateSalary(req, resp);
		} catch (Exception e) {
			sendMailInCaseOfException(e);
		}
	}

	private void sendMailInCaseOfException(Exception e) {
		String traces = ExceptionUtils.getExceptionTraceMessage("EmployeeSalaryGenerationServlet Exception >> ", e);
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

	private void instantiateClassVariablesAndGenerateSalary(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			resp.setContentType("application/vnd.ms-excel");
			String decompositionParameter = "attachment;filename=Salary_Sheet_Report_Before_Provesioning.xls";
			resp.setHeader("Content-disposition", decompositionParameter);
			resp.setHeader("Content-Type", "application/octet-stream");
			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			WritableSheet sheet = null;

			WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 22, WritableFont.BOLD);
			WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
			headerFormat.setAlignment(Alignment.CENTRE);

			String key = req.getParameter("keys");

			if (key.length() > 0) {
				key = key.substring(1, key.length() - 1);
				JSONArray employeeTakeSalaryRecords = getEmployeeTakeSalaryRecords(key);
				// HashMap<Integer, String> salaryComponentsMap = new HashMap<Integer, String>();
				JSONArray salaryComponentsRecords = getSalaryComponentsRecords();
				if (salaryComponentsRecords != null && salaryComponentsRecords.length() > 0) {

					if (employeeTakeSalaryRecords != null && employeeTakeSalaryRecords.length() > 0) {
						String branchIdInstring = employeeTakeSalaryRecords.getJSONObject(0).opt("branchid").toString();
						String monthIdInString = employeeTakeSalaryRecords.getJSONObject(0).opt("monthid").toString();
						String yearIdInString = employeeTakeSalaryRecords.getJSONObject(0).opt("yearid").toString();

						long branchId = Long.parseLong(branchIdInstring);
						long monthId = Long.parseLong(monthIdInString);
						long yearId = Long.parseLong(yearIdInString);
						String monthName = EmployeeReleasingSalaryServlet.getMonthName(monthId);
						String yearName = EmployeeReleasingSalaryServlet.getYearName(yearId);

						Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
						Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
						String monthFirstDateInString = getDateInString(monthFirstDate);
						String monthLastDateInString = getDateInString(monthLastDate);
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

						generateSalarySheet(sheet, workbook, fields);
						workbook.write();
						workbook.close();
					}
				}
			} else {
				throw new BusinessLogicException("Salary Components Not found Kindly Create Salary Components To Generate Report");
			}
		} catch (Exception e) {
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "");
		}
	}

	public static JSONArray getSalaryComponentsRecords() throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray salaryComponents = employeeObject.getJSONArray("salarycomponents");
		// sendMailDateAndBranchIdsIsExist(salaryComponents);
		return salaryComponents;
	}

	private JSONArray getEmployeeTakeSalaryRecords(String keys) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takesalarysheets");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("branchid");
		columnArray.put("monthid");
		columnArray.put("yearid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + keys + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray takeSalaryArrays = employeeObject.getJSONArray("takesalarysheets");
		return takeSalaryArrays;
	}

	public static void generateSalarySheet(WritableSheet sheet, WritableWorkbook workbook, JSONObject fields) throws Exception {
		JSONArray employeeArray = getActiveAndReleivedEmployeeRecords(fields);
		if (employeeArray.length() > 0) {
			populateSalaryComponentsToSheet(sheet, employeeArray, workbook, fields);
		}

	}

	public static void populateSalaryComponentsToSheet(WritableSheet sheet, JSONArray employeeArray, WritableWorkbook workbook, JSONObject fields) throws JSONException, ParseException, WriteException {
		int rows = 5;
		int columns = 0;
		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(font);
		cellFormat.setBackground(Colour.GRAY_25);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
		WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 22, WritableFont.BOLD);
		WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
		headerFormat.setAlignment(Alignment.CENTRE);

		for (int counter = 0; counter < employeeArray.length(); counter++) {
			columns = 0;
			rows = 5;

			JSONObject employeeRecord = employeeArray.getJSONObject(counter);
			Object employeeId = employeeRecord.opt("__key__");
			Object employeeName = employeeRecord.opt("name");
			Object employeeCode = employeeRecord.opt("employeecode");
			sheet = workbook.createSheet("" + employeeName, counter);

			HrisHelper.setHeaderValue(1, 1, "Month --> " + fields.opt("monthName"), cellFormat, sheet);
			HrisHelper.setHeaderValue(4, 1, "Year --> " + fields.opt("yearName"), cellFormat, sheet);

			sheet.mergeCells(0, 3, 2, 3);
			HrisHelper.setHeaderValue(0, 3, "Employee Code : " + employeeCode, cellFormat, sheet);
			sheet.mergeCells(4, 3, 6, 3);
			HrisHelper.setHeaderValue(4, 3, "Employee Name : " + employeeName, cellFormat, sheet);

			HrisHelper.setHeaderValue(0, rows, "S.No.", cellFormat, sheet);
			HrisHelper.setHeaderValue(1, rows, "Component Name", cellFormat, sheet);
			HrisHelper.setHeaderValue(2, rows, "From Date", cellFormat, sheet);
			HrisHelper.setHeaderValue(3, rows, "To Date", cellFormat, sheet);
			HrisHelper.setHeaderValue(4, rows, "Amount", cellFormat, sheet);

			JSONArray employeeSalaryComponentArray = EmployeeSalaryGenerationServlet.getEmployeeSalaryComponents("" + fields.opt("monthFirstDateInString"), "" + fields.opt("monthLastDateInString"), employeeId); // globle employee id used
			for (int componentCounter = 0; componentCounter < employeeSalaryComponentArray.length(); componentCounter++) {
				JSONObject details = employeeSalaryComponentArray.getJSONObject(componentCounter);
				if (details.opt("amount") != null && Translator.doubleValue(details.opt("amount")) > 0.0) {
					columns = 0;
					rows++;

					String componentName = details.optString("salarycomponentid.name");
					String applicableFromDate = details.optString("applicablefrom");
					String applicableToDate = details.optString("applicableto");
					String amount = "" + details.opt("amount");

					putHeader(sheet, "" + (componentCounter + 1), rows, columns++);
					putHeader(sheet, "" + componentName, rows, columns++);
					putHeader(sheet, "" + applicableFromDate, rows, columns++);
					putHeader(sheet, "" + applicableToDate, rows, columns++);
					putHeader(sheet, "" + amount, rows, columns++);
				}
			}

			double presendDays = 0.0;
			double absentDays = 0.0;
			double leaveDays = 0.0;
			double ewdDays = 0.0;
			double tourDays = 0.0;
			double unKnown = 0.0;
			double workFromHome = 0.0;
			double offDays = 0.0;

			String filter = "employeeid = " + employeeId + " and attendancedate >= '" + fields.opt("monthFirstDateInString") + "' and attendancedate <= '" + fields.opt("monthLastDateInString") + "'";
			JSONArray employeeAttendanceArray = new EmployeeSalarySheetReport().getEmployeeAttendanceArray(filter);
			for (int innerCounter = 0; innerCounter < employeeAttendanceArray.length(); innerCounter++) {
				int attendanceTypeId = Translator.integerValue(employeeAttendanceArray.getJSONObject(innerCounter).opt("attendancetypeid"));
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT) {
					presendDays += 1.0;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
					absentDays += 1.0;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
					leaveDays += 1.0;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE) {
					leaveDays += 0.50;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) {
					ewdDays += 1.0;
					offDays += 1.0;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
					ewdDays += 0.50;
					offDays += 1.0;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR) {
					tourDays += 1.0;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
					unKnown += 1.0;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
					workFromHome += 1.0;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_OFF) {
					offDays += 1.0;
				}
			}
			rows++;
			columns = 0;
			HrisHelper.setHeaderValue(columns++, rows, "Presnts", cellFormat, sheet);
			HrisHelper.setHeaderValue(columns++, rows, "Absents", cellFormat, sheet);
			HrisHelper.setHeaderValue(columns++, rows, "Leaves", cellFormat, sheet);
			HrisHelper.setHeaderValue(columns++, rows, "EWDs", cellFormat, sheet);
			HrisHelper.setHeaderValue(columns++, rows, "Tours", cellFormat, sheet);
			HrisHelper.setHeaderValue(columns++, rows, "Work From Home", cellFormat, sheet);
			HrisHelper.setHeaderValue(columns++, rows, "Un Known", cellFormat, sheet);
			HrisHelper.setHeaderValue(columns++, rows, "Off Days", cellFormat, sheet);

			rows++;
			columns = 0;
			putHeader(sheet, "" + presendDays, rows, columns++);
			putHeader(sheet, "" + absentDays, rows, columns++);
			putHeader(sheet, "" + leaveDays, rows, columns++);
			putHeader(sheet, "" + ewdDays, rows, columns++);
			putHeader(sheet, "" + tourDays, rows, columns++);
			putHeader(sheet, "" + workFromHome, rows, columns++);
			putHeader(sheet, "" + unKnown, rows, columns++);
			putHeader(sheet, "" + offDays, rows, columns++);

		}

	}

	/**
	 * This method is calculate monthly amount of all components and insert in calculate monthly amount DO.
	 * 
	 * @param employeeSalaryComponentArray
	 * @param employeeRecord
	 * @throws JSONException
	 */

	private static void putHeader(WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value);
		sheet.addCell(label);
	}

	public static Date getMonthFirstDate(long yearId, long monthId) throws Exception {
		String monthName = EmployeeReleasingSalaryServlet.getMonthName(monthId);
		String yearName = EmployeeReleasingSalaryServlet.getYearName(yearId);
		Date date = null;
		date = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		return date;
	}

	/**
	 * This method will return all active and non-active employees(only those who have been relieved in this month)
	 * 
	 * @param fields
	 * 
	 * @param yearName
	 * @param monthName
	 * @return
	 * @throws JSONException
	 */
	public static JSONArray getActiveAndReleivedEmployeeRecords(JSONObject fields) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("officialemailid");
		columnArray.put("name");
		columnArray.put("incrementduedate");
		columnArray.put("employeescheduleid");
		columnArray.put("holidaycalendarid");
		columnArray.put("leavepolicyid");
		columnArray.put("leavepolicyid.issandwich");
		columnArray.put("joiningdate");
		columnArray.put("branchid");
		columnArray.put("employeecode");
		columnArray.put("profitcenterid");
		columnArray.put("businessfunctionid");
		columnArray.put("accountno");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + " and branchid = " + fields.opt("branchId"));
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and branchid = " + fields.opt("branchId") + " and relievingdate >= '" + fields.opt("monthFirstDateInString") + "'"); // + "' and relievingdate <= '" + monthLastDateInString + "'");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);

		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public static JSONArray getEmployeeRecordsForSalarySheetReGenerate(Object key) throws JSONException {
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
		columnArray.put("branchid");
		columnArray.put("employeecode");
		columnArray.put("profitcenterid");
		columnArray.put("businessfunctionid");
		columnArray.put("accountno");
		columnArray.put("name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + key + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public static String getDateInString(Date date) {
		if (date != null) {
			return new SimpleDateFormat("yyyy-MM-dd").format(date);
		} else {
			return "";
		}
	}

	// public static String getMonthName(long monthId) {
	// try {
	// JSONObject query = new JSONObject();
	// query.put(Data.Query.RESOURCE, "organization_months");
	// JSONArray array = new JSONArray();
	// array.put("name");
	// query.put(Data.Query.COLUMNS, array);
	// query.put(Data.Query.FILTERS, "__key__ = " + monthId);
	// JSONObject object;
	// object = ResourceEngine.query(query);
	// JSONArray rows = object.getJSONArray("organization_months");
	// int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
	// if (rowCount > 0) {
	// String monthName = rows.getJSONObject(0).optString("name");
	// return monthName;
	// }
	// return null;
	// } catch (Exception e) {
	// throw new RuntimeException("Error occured while geting month name from monthid: " + e.getMessage());
	// }
	// }

	// public static String getYearName(long yearId) {
	// try {
	// JSONObject query = new JSONObject();
	// query.put(Data.Query.RESOURCE, "organization_years");
	// JSONArray array = new JSONArray();
	// array.put("name");
	// query.put(Data.Query.COLUMNS, array);
	// query.put(Data.Query.FILTERS, "__key__ = " + yearId);
	// JSONObject object;
	// object = ResourceEngine.query(query);
	// JSONArray rows = object.getJSONArray("organization_years");
	// int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
	// if (rowCount > 0) {
	// String yearName = rows.getJSONObject(0).optString("name");
	// return yearName;
	// }
	// return null;
	// } catch (Exception e) {
	// throw new RuntimeException("Error occured while converting yearid into year name.." + e.getMessage());
	// }
	// }
}
