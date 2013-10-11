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
import com.applane.resourceoriented.hris.HRISApplicationConstants;

@Path("/absentexcelreportbusinesslogic")
public class AbsentExcelReportBusinessLogic {

	@GET
	@Path("/employeeAbsentPost")
	public Response getEmployeeMonthlyAbsent(@Context HttpServletRequest request) throws IOException {
		return employeeAbsentPost(request);
	}

	@POST
	@Path("/employeeAbsentPost")
	public Response employeeAbsentPost(@Context HttpServletRequest request) throws IOException {
		String selectedKeys = request.getParameter("keys");
		try {
			if (selectedKeys != null) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				employeeDetailsExcelReport(outputStream, selectedKeys);
				if (outputStream.size() > 0) {
					ResponseBuilder responseBuilder = Response.ok(outputStream.toByteArray(), "application/vnd.ms-excel");
					responseBuilder.header("Content-Disposition", "attachment; Filename=\"Employee-Absent-Report" + "\"");
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

	private void addTableData(String selectedKeys, WritableSheet sheet) throws Exception {

		String key = selectedKeys;
		if (key != null && key.length() > 2) {
			key = key.substring(1, (key.length() - 1));
		}
		JSONObject employeeDetailObject = getEmployeeDetail(key);
		// LogUtility.writeLog("salarySheetExcelReport >> Attendance Report Array : " + attendanceReportArray);
		if (employeeDetailObject != null) {
			String monthName = "" + employeeDetailObject.opt("monthName");
			String yearName = "" + employeeDetailObject.opt("yearName");

			JSONArray absentArray = employeeDetailObject.optJSONArray("absentArray");
			int attendanceReportLength = (absentArray == null) ? 0 : absentArray.length();
			LogUtility.writeLog("absentExcelReport >> Attendance Report Length : " + attendanceReportLength);
			int i = 0;
			int srNo = 0;
			addTextCell(sheet, monthName, i, 1);
			addTextCell(sheet, yearName, i, 2);
			for (int counter = 0; counter < attendanceReportLength; counter++) {
				JSONObject jsonObject = absentArray.optJSONObject(counter);
				JSONArray attendanceRows = jsonObject.optJSONArray("employeeattendance");
				if (attendanceRows != null && attendanceRows.length() > 0) {
					int column = 0;
					i++;
					srNo++;
					String employeeCode = jsonObject.optString("employeecode");
					String employeeName = jsonObject.optString("name");
					Object department = jsonObject.opt("departmentid.name") == null ? "" : jsonObject.opt("departmentid.name");
					addTextCell(sheet, ("" + srNo), i + 1, column++);
					addTextCell(sheet, employeeCode, i + 1, column++);
					addTextCell(sheet, employeeName, i + 1, column++);
					addTextCell(sheet, "" + department, i + 1, column++);

					for (int attendanceCounter = 0; attendanceCounter < attendanceRows.length(); attendanceCounter++) {
						i++;
						JSONObject attendance = attendanceRows.getJSONObject(attendanceCounter);
						int attendanceTypeId = attendance.optInt("attendancetypeid", 0);
						Object date = attendance.opt("attendancedate");
						String attendanceType = "";
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
							attendanceType = "Absent";
						} else {
							attendanceType = "Half Day Absent";

						}
						addTextCell(sheet, "" + date, i, column);
						addTextCell(sheet, "" + attendanceType, i, column + 1);

					}
					for (int j = 0; j < column; j++) {
						CellView columnView = sheet.getColumnView(j);
						columnView.setAutosize(true);
						sheet.setColumnView(j, columnView);
					}
				}
			}
		}
	}

	private void addTableRowHeader(WritableSheet sheet) throws RowsExceededException, WriteException {

		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat headerCellformat = new WritableCellFormat();
		headerCellformat.setFont(font);

		int row = 1;
		int column = 0;

		addTableHeader(headerCellformat, sheet, "S.No.", row, column++);
		addTableHeader(headerCellformat, sheet, "Employee Code", row, column++);
		addTableHeader(headerCellformat, sheet, "Name", row, column++);
		addTableHeader(headerCellformat, sheet, "Department", row, column++);
		addTableHeader(headerCellformat, sheet, "Date", row, column++);
		addTableHeader(headerCellformat, sheet, "Attendance Status", row, column++);

		for (int i = 0; i < column; i++) {
			CellView columnView = sheet.getColumnView(i);
			columnView.setAutosize(true);
			sheet.setColumnView(i, columnView);
		}
	}

	private JSONObject getEmployeeDetail(String key) throws JSONException, Exception {

		LogUtility.writeLog("getAttendanceReportDetail >> Selected Key : " + key);

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.PFReportForYear.BRANCH_ID);
		columns.put(HrisKinds.PFReportForYear.DEPARTMENT_ID);
		columns.put(HrisKinds.PFReportForYear.MONTH_ID);
		columns.put(HrisKinds.PFReportForYear.YEAR_ID);
		columns.put(HrisKinds.PFReportForYear.MONTH_ID + ".name");
		columns.put(HrisKinds.PFReportForYear.YEAR_ID + ".name");

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
			String monthName = "" + jsonObject.opt(HrisKinds.PFReportForYear.MONTH_ID + ".name");
			String yearName = "" + jsonObject.opt(HrisKinds.PFReportForYear.YEAR_ID + ".name");

			Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
			Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
			String monthFirstDateInString = Translator.getDateInString(monthFirstDate);
			String monthLastDateInString = Translator.getDateInString(monthLastDate);
			JSONObject object = new JSONObject();
			object.put("absentArray", getEmployeeDetailsData(branchId, departmentId, monthFirstDateInString, monthLastDateInString));
			object.put("monthName", monthName);
			object.put("yearName", yearName);
			return object;
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

		StringBuilder filter = new StringBuilder("employeestatusid = ").append(HRISApplicationConstants.EMPLOYEE_ACTIVE).append(" AND joiningdate<='").append(toDate).append("'");
		StringBuilder optionFilter = new StringBuilder("employeestatusid = ").append(HRISApplicationConstants.EMPLOYEE_INACTIVE);
		optionFilter.append(" AND ");
		optionFilter.append("relievingdate >= '").append(fromDate).append("'");

		JSONObject parameters = null;

		JSONArray columns = new JSONArray();
		columns.put("name");
		columns.put("relievingdate");
		columns.put("joiningdate");
		columns.put("employeecode");
		columns.put("departmentid.name");
		columns.put("deviceid");
		columns.put("name");

		JSONObject employeeQuery = new JSONObject();
		employeeQuery.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		employeeQuery.put(Data.Query.CHILDS, populateChildQuery("" + fromDate, "" + toDate));
		employeeQuery.put(Data.Query.COLUMNS, columns);
		// employeeQuery.put(Data.Query.FILTERS, "__key__ IN(1312)");
		employeeQuery.put(Data.Query.FILTERS, filter.toString() + extraFilter);
		employeeQuery.put(Data.Query.OPTION_FILTERS, optionFilter.toString() + extraFilter);
		employeeQuery.put(Data.Query.PARAMETERS, parameters);

		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(employeeQuery).getJSONArray(HrisKinds.EMPLOYEES);

	}

	private JSONArray populateChildQuery(String fromDate, String toDate) throws JSONException {
		JSONArray columnsRequired = new JSONArray();
		columnsRequired.put("__key__");
		columnsRequired.put("attendancetypeid");
		columnsRequired.put("attendancedate");

		JSONObject attendanceQuery = new JSONObject();
		attendanceQuery.put(Data.Query.RESOURCE, "employeeattendance");
		attendanceQuery.put(Data.Query.COLUMNS, columnsRequired);
		attendanceQuery.put(Data.Query.FILTERS, "attendancedate>='" + fromDate + "' AND attendancedate<='" + toDate + "' AND attendancetypeid IN(" + HRISApplicationConstants.ATTENDANCE_ABSENT + "," + HRISApplicationConstants.ATTENDANCE_HALF_DAY_ABSENT + ")");

		JSONObject child = new JSONObject();
		child.put(Data.Query.Childs.RELATED_COLUMN, "employeeid");
		child.put(Data.Query.Childs.QUERY, attendanceQuery);
		child.put(Data.Query.Childs.ALIAS, "employeeattendance");

		return new JSONArray().put(child);
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
