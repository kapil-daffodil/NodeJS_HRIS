package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.Workbook;
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
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class EmployeeWelFareFundReport extends HttpServlet {

	private static final long serialVersionUID = 1L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		createLWFExcelReport(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doGet(req, resp);
	}

	private void createLWFExcelReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=LWF_Report.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");
		try {

			// String monthName = null;
			// String yearName = null;
			String key = req.getParameter("keys");
			if (key.length() > 2) {
				key = key.substring(1, key.length() - 1);
			}
			String[] keys = key.split(",");
			key = keys[0];
			JSONArray pfReportArray = new PFAnnualReturnReportForm6ARevised().getPFReportArray(key);
			int year = 0;
			int branchId = 0;
			Date fromDate = null;
			Date toDate = null;
			String firstDateInString = "";
			String lastDateInString = "";

			if (pfReportArray != null && pfReportArray.length() > 0) {
				year = Translator.integerValue(pfReportArray.getJSONObject(0).opt("yearid.name"));
				branchId = Translator.integerValue(pfReportArray.getJSONObject(0).opt("branchid"));
				firstDateInString = "" + pfReportArray.getJSONObject(0).opt("fromdate");
				lastDateInString = "" + pfReportArray.getJSONObject(0).opt("todate");
				fromDate = Translator.dateValue(pfReportArray.getJSONObject(0).opt("fromdate"));
				toDate = Translator.dateValue(pfReportArray.getJSONObject(0).opt("todate"));
			}

			Date TODAY_DATE = Translator.dateValue(new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime()));
			Object lwfComponentId = PunchingUtility.getId("salarycomponents", "salarycomponenttypeid", "" + 5);
			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
			WritableCellFormat cellFormat = new WritableCellFormat(font);
			cellFormat.setBackground(Colour.GRAY_25);
			cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
			WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
			WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
			cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);

			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			WritableSheet sheet = null;
			sheet = workbook.createSheet("Sheet_1", 0);
			Calendar pfReportPeriod = Calendar.getInstance();
			pfReportPeriod.setTime(TODAY_DATE);
			year = pfReportPeriod.get(Calendar.YEAR);
			if (fromDate == null) {
				pfReportPeriod.set(Calendar.DAY_OF_MONTH, 1);
				pfReportPeriod.set(Calendar.MONTH, 4 - 1); // Calendar Month from 0 sir for April we subtract 1
				pfReportPeriod.set(Calendar.YEAR, (year - 1));
				fromDate = pfReportPeriod.getTime();
				firstDateInString = EmployeeSalaryGenerationServlet.getDateInString(fromDate);
			}
			if (toDate == null) {
				pfReportPeriod = Calendar.getInstance();
				pfReportPeriod.set(Calendar.DAY_OF_MONTH, 31);
				pfReportPeriod.set(Calendar.MONTH, 3 - 1);
				pfReportPeriod.set(Calendar.YEAR, year);
				toDate = pfReportPeriod.getTime();
				lastDateInString = EmployeeSalaryGenerationServlet.getDateInString(toDate);
			}
			JSONArray employeeArray = getEmployeeArray(firstDateInString, branchId, lastDateInString);

			int row = 1;
			int column = 0;

			HrisHelper.setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Name of worker/ employee", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Father’s/Husband’s Name of worker/employee", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Designation of worker/ employee", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Token No.(if any)", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Date of Joining", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Date of Relieving", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "From", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "To", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Cont. period", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Total Employee share", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Total Employer share", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "TotalAmount", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Re- marks", cellFormat, sheet);
			int grandTotal = 0;
			for (int counter = 0; counter < employeeArray.length(); counter++) {

				column = 0;
				String remarks = "";
				String fromDateTemp = firstDateInString;
				String lastDateTemp = lastDateInString;
				Object employeeid = employeeArray.getJSONObject(counter).opt("__key__");
				String name = employeeArray.getJSONObject(counter).optString("name");
				String fatherName = employeeArray.getJSONObject(counter).optString("fathername");
				String designation = employeeArray.getJSONObject(counter).optString("designationid.name");
				Object dateOfJoining = employeeArray.getJSONObject(counter).opt("joiningdate");
				Object dateOfRealising = employeeArray.getJSONObject(counter).opt("relievingdate");

				if (dateOfJoining != null && fromDate.before(Translator.dateValue(dateOfJoining))) {
					fromDateTemp = String.valueOf(dateOfJoining);
				}

				if (dateOfRealising != null && toDate.after(Translator.dateValue(dateOfRealising))) {
					lastDateTemp = String.valueOf(dateOfRealising);
					String monthName = new SimpleDateFormat("MMMM").format(Translator.dateValue(dateOfRealising));
					remarks = "Left in " + monthName;
				}
				String startFrom = new SimpleDateFormat("MMMM").format(Translator.dateValue(fromDateTemp));
				String endTo = new SimpleDateFormat("MMMM").format(Translator.dateValue(lastDateTemp));
				long monthDifference = DataTypeUtilities.monthsBetweenDates(Translator.dateValue(fromDateTemp), Translator.dateValue(lastDateTemp));
				if (monthDifference > 0) {
					row++;
					double[] amountEmployeeEmployeer = { 0.0, 0.0 };
					Calendar cal = Calendar.getInstance();
					for (int innerCounter = 0; innerCounter < monthDifference; innerCounter++) {
						cal.setTime(fromDate);
						cal.add(Calendar.MONTH, innerCounter);
						int monthId = cal.get(Calendar.MONTH) + 1;
						long yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
						getAmount(employeeid, lwfComponentId, monthId, yearId, amountEmployeeEmployeer);
					}
					double total = amountEmployeeEmployeer[0] + amountEmployeeEmployeer[1];
					grandTotal += total;
					putHeader(cellFormat1, sheet, counter + 1, row, column++);
					putHeader(cellFormat1, sheet, name, row, column++);
					putHeader(cellFormat1, sheet, fatherName, row, column++);
					putHeader(cellFormat1, sheet, designation, row, column++);
					putHeader(cellFormat1, sheet, "", row, column++);
					putHeader(cellFormat1, sheet, dateOfJoining == null ? "" : dateOfJoining, row, column++);
					putHeader(cellFormat1, sheet, remarks.length() == 0 ? "" : dateOfRealising, row, column++);
					putHeader(cellFormat1, sheet, fromDateTemp, row, column++);
					putHeader(cellFormat1, sheet, lastDateTemp, row, column++);
					// putHeader(cellFormat1, sheet, monthDifference, row, column++);
					putHeader(cellFormat1, sheet, startFrom + " - " + endTo, row, column++);
					putHeader(cellFormat1, sheet, amountEmployeeEmployeer[0], row, column++);
					putHeader(cellFormat1, sheet, amountEmployeeEmployeer[1], row, column++);
					putHeader(cellFormat1, sheet, total, row, column++);
					putHeader(cellFormat1, sheet, remarks, row, column++);
					if (counter == (employeeArray.length() - 1)) {
						row++;
						putHeader(cellFormat1, sheet, grandTotal, row, --column);
					}
				}
			}
			workbook.write();
			workbook.close();

		} catch (Exception e) {
			LogUtility.writeLog("PFAnnualReturnReportForm6ARevised >> calculateAndWriteToSheet Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
		}
	}

	private void getAmount(Object employeeid, Object lwfComponentId, int monthId, long yearId, double[] amountEmployeeEmployeer) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("amount");
		columnArray.put("salarycomponentid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeid + " and salarymonthid = " + monthId + "  and salaryyearid = " + yearId + " and salarycomponentid = " + lwfComponentId);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray amountArray = employeeObject.getJSONArray("employeemonthlysalarycomponents");
		if (amountArray != null && amountArray.length() > 0) {
			amountEmployeeEmployeer[0] += 10;
			amountEmployeeEmployeer[1] += 20;
		}
		// for (int counter = 0; counter < amountArray.length(); counter++) {
		// double componentPayableAmount = Translator.doubleValue(amountArray.getJSONObject(counter).opt("amount"));
		// amountEmployeeEmployeer[0] += componentPayableAmount;
		// amountEmployeeEmployeer[1] += (componentPayableAmount * 2);
		// }

	}

	private static void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, cellFormat);
		sheet.setColumnView(column, 20);
		sheet.addCell(label);
	}

	public static JSONArray getEmployeeArray(String firstDateInString, int branchId, String lastDateInString) throws JSONException {
		String extraFilter = "";
		if (branchId != 0) {
			extraFilter += " and branchid = " + branchId;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		columnArray.put("employeecode");
		columnArray.put("accountno");
		columnArray.put("fathername");
		columnArray.put("designationid.name");
		columnArray.put("joiningdate");
		columnArray.put("relievingdate");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + extraFilter + " AND joiningdate <= '" + lastDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + extraFilter + " and relievingdate >= '" + firstDateInString + "'");
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}
}
