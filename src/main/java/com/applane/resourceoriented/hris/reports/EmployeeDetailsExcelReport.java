package com.applane.resourceoriented.hris.reports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

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
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants;

@Path("/employeedetailsexcelreport")
public class EmployeeDetailsExcelReport {

	@GET
	@Path("/employeedetails")
	public Response employeeMonthlyAttendanceGet(@Context HttpServletRequest request) throws IOException {
		return employeeDetailsPost(request);
	}

	@POST
	@Path("/employeedetails")
	public Response employeeDetailsPost(@Context HttpServletRequest request) throws IOException {
		String selectedKeys = request.getParameter("keys");
		try {
			if (selectedKeys != null) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				employeeDetailsExcelReport(outputStream, selectedKeys);
				if (outputStream.size() > 0) {
					ResponseBuilder responseBuilder = Response.ok(outputStream.toByteArray(), "application/vnd.ms-excel");
					responseBuilder.header("Content-Disposition", "attachment; Filename=\"Employee-Details" + "\"");
					return responseBuilder.build();
				}
			}
		} catch (Exception e) {
		}
		return Response.ok(200).entity("Please try after some time.").build();
	}

	private void employeeDetailsExcelReport(ByteArrayOutputStream outputStream, String selectedKeys) {
		try {
			WritableWorkbook workbook = Workbook.createWorkbook(outputStream);
			WritableSheet sheet = workbook.createSheet("AttendanceReport", 0);

			addTableRowHeader(sheet);
			addTableData(selectedKeys, sheet);

			workbook.write();
			workbook.close();
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AttendanceExcelReport >> Exception >> " + trace);
		}
	}

	private void addTableData(String selectedKeys, WritableSheet sheet) throws JSONException, RowsExceededException, WriteException {

		String key = selectedKeys;
		if (key != null && key.length() > 2) {
			key = key.substring(1, (key.length() - 1));
		}
		JSONArray employeeDetailArray = getEmployeeDetail(key);
		// LogUtility.writeLog("salarySheetExcelReport >> Attendance Report Array : " + attendanceReportArray);

		int attendanceReportLength = (employeeDetailArray == null) ? 0 : employeeDetailArray.length();
		LogUtility.writeLog("salarySheetExcelReport >> Attendance Report Length : " + attendanceReportLength);

		for (int i = 0; i < attendanceReportLength; i++) {

			JSONObject jsonObject = employeeDetailArray.optJSONObject(i);

			JSONArray profitCenterIdJsonArray = jsonObject.optJSONArray("profitcenterid");
			int employeeIdJsonArrayLength = (profitCenterIdJsonArray == null) ? 0 : profitCenterIdJsonArray.length();

			String profitCenterName = "";
			for (int j = 0; j < employeeIdJsonArrayLength; j++) {
				JSONObject profitCenterJsonObject = profitCenterIdJsonArray.optJSONObject(j);
				if (j != 0) {
					profitCenterName += ";";
				}
				profitCenterName += profitCenterJsonObject.optString(HrisKinds.ProfitCenter.NAME);
			}

			String employeeCode = jsonObject.optString("employeecode");
			String employeeName = jsonObject.optString("name");
			Object dateOfBirthObject = jsonObject.opt("dateofbirth");
			Object fatherName = jsonObject.opt("fathername") == null ? "" : jsonObject.opt("fathername");
			Object designation = jsonObject.opt("designationid.name") == null ? "" : jsonObject.opt("designationid.name");
			Object department = jsonObject.opt("departmentid.name") == null ? "" : jsonObject.opt("departmentid.name");
			Object mobNumber = jsonObject.opt("mobileno") == null ? "" : jsonObject.opt("mobileno");
			Object address = jsonObject.opt("address") == null ? "" : jsonObject.opt("address");
			Date currentDateTime = SystemParameters.getCurrentDateTime();
			Date dateOfBirthDate = Translator.dateValue(dateOfBirthObject);
			long monthsBetweenDates = 0;
			if (dateOfBirthDate != null) {
				monthsBetweenDates = DataTypeUtilities.monthsBetweenDates(dateOfBirthDate, currentDateTime);
			}
			String age = "";
			age += monthsBetweenDates / 12;
			age += " Year ";
			age += monthsBetweenDates % 12;
			age += " Months";
			int column = 0;

			addTextCell(sheet, ("" + (i + 1)), i + 1, column++);
			addTextCell(sheet, employeeCode, i + 1, column++);
			addTextCell(sheet, employeeName, i + 1, column++);
			addTextCell(sheet, "" + fatherName, i + 1, column++);

			addTextCell(sheet, age, i + 1, column++);
			addTextCell(sheet, profitCenterName, i + 1, column++);
			addTextCell(sheet, "" + designation, i + 1, column++);
			addTextCell(sheet, "" + department, i + 1, column++);

			addTextCell(sheet, "" + mobNumber, i + 1, column++);
			addTextCell(sheet, "" + address, i + 1, column++);

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

		addTableHeader(headerCellformat, sheet, "S.No.", row, column++);
		addTableHeader(headerCellformat, sheet, "Employee Code", row, column++);
		addTableHeader(headerCellformat, sheet, "Name", row, column++);
		addTableHeader(headerCellformat, sheet, "Father Name", row, column++);
		addTableHeader(headerCellformat, sheet, "Age", row, column++);
		addTableHeader(headerCellformat, sheet, "Business Unit", row, column++);
		addTableHeader(headerCellformat, sheet, "Designation", row, column++);
		addTableHeader(headerCellformat, sheet, "Department", row, column++);
		addTableHeader(headerCellformat, sheet, "Mobile Number", row, column++);
		addTableHeader(headerCellformat, sheet, "Address", row, column++);

		for (int i = 0; i < column; i++) {
			CellView columnView = sheet.getColumnView(i);
			columnView.setAutosize(true);
			sheet.setColumnView(i, columnView);
		}
	}

	private JSONArray getEmployeeDetail(String key) throws JSONException {

		LogUtility.writeLog("getAttendanceReportDetail >> Selected Key : " + key);

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.PFReportForYear.BRANCH_ID);
		columns.put(HrisKinds.PFReportForYear.DEPARTMENT_ID);
		columns.put(HrisKinds.PFReportForYear.FROM_DATE);
		columns.put(HrisKinds.PFReportForYear.TO_DATE);

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
			Object fromDate = jsonObject.opt(HrisKinds.PFReportForYear.FROM_DATE);
			Object toDate = jsonObject.opt(HrisKinds.PFReportForYear.TO_DATE);

			return getEmployeeDetailsData(branchId, departmentId, fromDate, toDate);
		}

		return null;
	}

	private JSONArray getEmployeeDetailsData(Object branchId, Object departmentId, Object fromDate, Object toDate) throws JSONException {

		LogUtility.writeLog("getEmployeeAttendanceReportData >> Branch : " + branchId + ", Department : " + departmentId + ", toDate : " + toDate + ", fromDate : " + fromDate);
		String extraFilter = "";
		if (branchId != null) {
			extraFilter += " AND branchid = " + branchId;
		}
		if (departmentId != null) {
			extraFilter += " AND departmentid = " + departmentId;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		columnArray.put("address");
		columnArray.put("mobileno");
		columnArray.put("fathername");
		columnArray.put("dateofbirth");
		columnArray.put("employeecode");
		columnArray.put("relievingdate");
		columnArray.put("departmentid.name");
		columnArray.put("designationid.name");
		columnArray.put(new JSONObject().put("profitcenterid", new JSONArray().put("name")));
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + extraFilter);
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and relievingdate >= '" + fromDate + "'" + extraFilter);

		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public void putHeader(WritableCellFormat numberCellFormat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, numberCellFormat);
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
