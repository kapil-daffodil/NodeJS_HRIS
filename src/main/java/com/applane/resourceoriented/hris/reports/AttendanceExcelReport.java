package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.CellView;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.Number;
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
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.DataTypeUtilities;

@SuppressWarnings("serial")
public class AttendanceExcelReport extends HttpServlet {

	private static String KEYS = "keys";

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		attendanceExcelReport(request, response);
	}

	private void attendanceExcelReport(HttpServletRequest request, HttpServletResponse response) {
		try {
			response.setContentType("application/vnd.ms-excel");
			String decompositionParameter = "attachment;filename=Attendance_Report.xls";
			response.setHeader("Content-disposition", decompositionParameter);
			response.setHeader("Content-Type", "application/octet-stream");

			WritableWorkbook workbook = Workbook.createWorkbook(response.getOutputStream());
			WritableSheet sheet = workbook.createSheet("AttendanceReport", 0);

			addTableRowHeader(sheet);
			addTableData(request, sheet);

			workbook.write();
			workbook.close();
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AttendanceExcelReport >> Exception >> " + trace);
		}
	}

	private void addTableData(HttpServletRequest request, WritableSheet sheet) throws JSONException, RowsExceededException, WriteException, Exception {

		String key = getSelectedKey(request);
		JSONArray attendanceReportArray = getAttendanceReportDetail(key);
		// LogUtility.writeLog("salarySheetExcelReport >> Attendance Report Array : " + attendanceReportArray);

		int attendanceReportLength = (attendanceReportArray == null) ? 0 : attendanceReportArray.length();
		LogUtility.writeLog("salarySheetExcelReport >> Attendance Report Length : " + attendanceReportLength);

		for (int i = 0; i < attendanceReportLength; i++) {

			JSONObject jsonObject = attendanceReportArray.optJSONObject(i);

			String department = jsonObject.optString(HrisKinds.EmployeeMonthlyAttendance.DEPARTMENT_NAME);

			JSONArray profitCenterIdJsonArray = jsonObject.optJSONArray(HrisKinds.EmployeeMonthlyAttendance.PROFIT_CENTER_ID);
			int employeeIdJsonArrayLength = (profitCenterIdJsonArray == null) ? 0 : profitCenterIdJsonArray.length();

			String profitCenterName = "";
			for (int j = 0; j < employeeIdJsonArrayLength; j++) {
				JSONObject profitCenterJsonObject = profitCenterIdJsonArray.optJSONObject(j);
				if (j != 0) {
					profitCenterName += ";";
				}
				profitCenterName += profitCenterJsonObject.optString(HrisKinds.ProfitCenter.NAME);
			}

			String employeeCode = jsonObject.optString(HrisKinds.EmployeeMonthlyAttendance.EMPLOYEE_CODE);
			String employeeName = jsonObject.optString(HrisKinds.EmployeeMonthlyAttendance.EMPLOYEE_NAME);
			String yearName = jsonObject.optString(HrisKinds.EmployeeMonthlyAttendance.YEAR_ID + ".name");
			String monthName = jsonObject.optString(HrisKinds.EmployeeMonthlyAttendance.MONTH_ID + ".name");
			Object joiningDateObject = jsonObject.opt(HrisKinds.EmployeeMonthlyAttendance.EMPLOYEE_ID + ".joiningdate");
			long daysInMonth = jsonObject.optLong(HrisKinds.EmployeeMonthlyAttendance.DAYS_IN_MONTH);
			double leaves = jsonObject.optDouble(HrisKinds.EmployeeMonthlyAttendance.LEAVES);
			double nonPayableLeaves = jsonObject.optDouble(HrisKinds.EmployeeMonthlyAttendance.NON_PAYABLE_LEAVES);
			double presentDays = jsonObject.optDouble(HrisKinds.EmployeeMonthlyAttendance.PRESENT_DAYS);
			double absentDays = jsonObject.optDouble(HrisKinds.EmployeeMonthlyAttendance.ABSENTS);
			double nonPayableDbsentDays = jsonObject.optDouble(HrisKinds.EmployeeMonthlyAttendance.NON_PAYABLE_ABSENTS);
			double actualNonWorkingDays = jsonObject.optDouble(HrisKinds.EmployeeMonthlyAttendance.ACTUAL_NON_WORKING_DAYS);
			double payableLeaves = jsonObject.optDouble(HrisKinds.EmployeeMonthlyAttendance.PAYABLE_LEAVES);
			double paidAbsents = jsonObject.optDouble(HrisKinds.EmployeeMonthlyAttendance.PAID_ABSENTS);
			double carryExtraWorkingDay = jsonObject.optDouble(HrisKinds.EmployeeMonthlyAttendance.CARRY_EXTRA_WORKING_DAY);
			double daysWorked = presentDays + actualNonWorkingDays;
			double salaryDays = presentDays + actualNonWorkingDays + payableLeaves + paidAbsents + carryExtraWorkingDay;
			double lop = nonPayableLeaves + nonPayableDbsentDays;
			Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
			Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
			int column = 0;
			if (joiningDateObject != null) {
				Date joiningDate = Translator.dateValue(joiningDateObject);
				if (joiningDate.after(monthFirstDate)) {
					daysInMonth = Translator.daysBetween(monthLastDate, joiningDate) + 1;
				}
			}
			addTextCell(sheet, department, i + 1, column++);
			addTextCell(sheet, profitCenterName, i + 1, column++);
			addTextCell(sheet, employeeCode, i + 1, column++);
			addTextCell(sheet, employeeName, i + 1, column++);
			addNumericCell(sheet, daysInMonth, i + 1, column++);
			addNumericCell(sheet, daysWorked, i + 1, column++);
			addNumericCell(sheet, absentDays, i + 1, column++);
			addNumericCell(sheet, leaves, i + 1, column++);
			addNumericCell(sheet, nonPayableLeaves, i + 1, column++);
			addNumericCell(sheet, lop, i + 1, column++);
			addNumericCell(sheet, salaryDays, i + 1, column++);

			for (int j = 0; j < column; j++) {
				CellView columnView = sheet.getColumnView(j);
				columnView.setAutosize(true);
				sheet.setColumnView(j, columnView);
			}
		}
	}

	private void addTableRowHeader(WritableSheet sheet) throws RowsExceededException, WriteException {

		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat headerCellformat = new WritableCellFormat();
		headerCellformat.setFont(font);

		int row = 0;
		int column = 0;

		addTableHeader(headerCellformat, sheet, "Department", row, column++);
		addTableHeader(headerCellformat, sheet, "Business Unit", row, column++);
		addTableHeader(headerCellformat, sheet, "Employee Code", row, column++);
		addTableHeader(headerCellformat, sheet, "Name", row, column++);
		addTableHeader(headerCellformat, sheet, "Days in Month", row, column++);
		addTableHeader(headerCellformat, sheet, "Days Worked", row, column++);
		addTableHeader(headerCellformat, sheet, "Absents", row, column++);
		addTableHeader(headerCellformat, sheet, "Leaves", row, column++);
		addTableHeader(headerCellformat, sheet, "Non Payable Leaves", row, column++);
		addTableHeader(headerCellformat, sheet, "LOP", row, column++);
		addTableHeader(headerCellformat, sheet, "Salary Days", row, column++);

		for (int i = 0; i < column; i++) {
			CellView columnView = sheet.getColumnView(i);
			columnView.setAutosize(true);
			sheet.setColumnView(i, columnView);
		}
	}

	private String getSelectedKey(HttpServletRequest request) {

		String key = request.getParameter(AttendanceExcelReport.KEYS);
		LogUtility.writeLog("getSelectedKey >> 1 >> Key : " + key);

		if (key.length() > 2) {
			key = key.substring(1, key.length() - 1);
		}

		String[] keys = key.split(",");
		key = keys[0];

		LogUtility.writeLog("getSelectedKey >> 2 >> Key : " + key);

		return key;
	}

	private JSONArray getAttendanceReportDetail(String key) throws JSONException {

		LogUtility.writeLog("getAttendanceReportDetail >> Selected Key : " + key);

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.PFReportForYear.BRANCH_ID);
		columns.put(HrisKinds.PFReportForYear.DEPARTMENT_ID);
		columns.put(HrisKinds.PFReportForYear.MONTH_ID);
		columns.put(HrisKinds.PFReportForYear.YEAR_ID);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.PF_REPORT_FOR_YEAR);
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, Data.Query.KEY + "=" + key);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.PF_REPORT_FOR_YEAR);

		int resultLength = (resultJSONArray == null) ? 0 : resultJSONArray.length();
		LogUtility.writeLog("Result Length : " + resultLength);

		if (resultLength > 0) {

			JSONObject jsonObject = resultJSONArray.optJSONObject(0);

			Object branchId = jsonObject.opt(HrisKinds.PFReportForYear.BRANCH_ID);
			Object departmentId = jsonObject.opt(HrisKinds.PFReportForYear.DEPARTMENT_ID);
			Object monthId = jsonObject.opt(HrisKinds.PFReportForYear.MONTH_ID);
			Object yearId = jsonObject.opt(HrisKinds.PFReportForYear.YEAR_ID);

			return getEmployeeAttendanceReportData(branchId, departmentId, monthId, yearId);
		}

		return null;
	}

	private JSONArray getEmployeeAttendanceReportData(Object branchId, Object departmentId, Object monthId, Object yearId) throws JSONException {

		LogUtility.writeLog("getEmployeeAttendanceReportData >> Branch : " + branchId + ", Department : " + departmentId + ", Year : " + yearId + ", Month : " + monthId);

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.EmployeeMonthlyAttendance.EMPLOYEE_ID);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.EMPLOYEE_ID + ".joiningdate");
		columns.put(HrisKinds.EmployeeMonthlyAttendance.DEPARTMENT_NAME);
		columns.put(new JSONObject().put(HrisKinds.EmployeeMonthlyAttendance.PROFIT_CENTER_ID, new JSONArray().put(HrisKinds.ProfitCenter.NAME)));
		columns.put(HrisKinds.EmployeeMonthlyAttendance.EMPLOYEE_CODE);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.EMPLOYEE_NAME);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.MONTH_ID + ".name");
		columns.put(HrisKinds.EmployeeMonthlyAttendance.YEAR_ID + ".name");
		columns.put(HrisKinds.EmployeeMonthlyAttendance.DAYS_IN_MONTH);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.LEAVES);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.ABSENTS);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.NON_PAYABLE_ABSENTS);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.NON_PAYABLE_LEAVES);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.PRESENT_DAYS);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.ACTUAL_NON_WORKING_DAYS);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.PAYABLE_LEAVES);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.PAID_ABSENTS);
		columns.put(HrisKinds.EmployeeMonthlyAttendance.CARRY_EXTRA_WORKING_DAY);

		String filterString = HrisKinds.EmployeeMonthlyAttendance.MONTH_ID + "=" + monthId + " AND " + HrisKinds.EmployeeMonthlyAttendance.YEAR_ID + "=" + yearId;
		if (branchId != null) {
			filterString += " AND " + HrisKinds.EmployeeMonthlyAttendance.BRANCH_ID + "=" + branchId;
		}
		if (departmentId != null) {
			filterString += " AND " + HrisKinds.EmployeeMonthlyAttendance.DEPARTMENT_ID + "=" + departmentId;
		}

		LogUtility.writeLog("getEmployeeAttendanceReportData >> Filter : " + filterString);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEE_MONTHLY_ATTENDANCE);
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, filterString);

		LogUtility.writeLog("getEmployeeAttendanceReportData >> Query : " + query);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		return result.optJSONArray(HrisKinds.EMPLOYEE_MONTHLY_ATTENDANCE);
	}

	private void addNumericCell(WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value);
		sheet.addCell(numobj);
	}

	private void addTableHeader(WritableCellFormat headerCellformat, WritableSheet sheet, String value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, value, headerCellformat);
		sheet.addCell(label);
	}

	private void addTextCell(WritableSheet sheet, String value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, value);
		sheet.addCell(label);
	}
}
