package com.applane.resourceoriented.hris.reports.darcl;

import java.io.IOException;
import java.util.HashMap;

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
import jxl.write.Number;
import jxl.write.NumberFormats;
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
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.reports.EmployeePerformanceReportInExcel;

public class PerformancePercentageExcelReport extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			resp.setContentType("application/vnd.ms-excel");
			String decompositionParameter = "attachment;filename=Employee_Performance_Report.xls";
			resp.setHeader("Content-disposition", decompositionParameter);
			resp.setHeader("Content-Type", "application/octet-stream");
			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			String key = req.getParameter("keys");
			generateEmployeePerformanceReport(workbook, key);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("PerformancePercentageExcelReport trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured Please Contact To Admin.");
		}
	}

	private void generateEmployeePerformanceReport(WritableWorkbook workbook, String key) throws Exception {
		if (key.length() > 2) {
			WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 12, WritableFont.NO_BOLD);
			WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
			headerFormat.setAlignment(Alignment.CENTRE);

			WritableFont feemergefont = new WritableFont(WritableFont.ARIAL, 13, WritableFont.BOLD);
			WritableCellFormat feemergeFormat = new WritableCellFormat(feemergefont);
			feemergeFormat.setAlignment(Alignment.CENTRE);
			feemergeFormat.setBackground(Colour.GRAY_25);
			feemergeFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
			WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
			numbercellformat.setFont(font);
			numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);

			key = key.substring(1, key.length() - 1);
			JSONArray performanceRecords = new EmployeePerformanceReportInExcel().getPerformanceRecords(key);
			for (int counter = 0; counter < performanceRecords.length(); counter++) {
				Object monthId = performanceRecords.getJSONObject(counter).opt("monthid");
				Object yearId = performanceRecords.getJSONObject(counter).opt("yearid");
				String monthName = EmployeeSalaryGenerationServlet.getMonthName((long) Translator.integerValue(monthId));
				String yearName = EmployeeSalaryGenerationServlet.getYearName((long) Translator.integerValue(yearId));
				WritableSheet sheet = workbook.createSheet(monthName.trim().replaceAll(" ", "_") + "-" + yearName.trim().replaceAll(" ", "_"), counter);

				JSONArray employeePerformanceRecords = getEmployeePerformanceRecords(monthId, yearId);
				HashMap<Integer, HashMap<Integer, Object[]>> employeePerformanceMap = new HashMap<Integer, HashMap<Integer, Object[]>>();
				HashMap<Integer, String[]> employeeDetailsMap = new HashMap<Integer, String[]>();
				putRecordsIntoMap(employeePerformanceMap, employeePerformanceRecords, employeeDetailsMap);
				int row = 5;
				int column = 0;
				sheet.mergeCells(column + 2, row - 4, column + 8, row - 4);

				putHeaderDefault(feemergeFormat, sheet, "Employee Performance Sheet for " + monthName + " - " + yearName, row - 4, column + 2);

				putHeaderDefault(feemergeFormat, sheet, "S. No.", row, column++);
				putHeaderDefault(feemergeFormat, sheet, "Employee Code", row, column++);
				putHeaderDefault(feemergeFormat, sheet, "Employee Name", row, column++);
				putHeaderDefault(feemergeFormat, sheet, "Designation", row, column++);
				putHeaderDefault(feemergeFormat, sheet, "Department", row, column++);
				putHeaderDefault(feemergeFormat, sheet, "Name of H.O.D.", row, column++);
				putHeaderDefault(feemergeFormat, sheet, "KPI's", row, column++);
				putHeaderDefault(feemergeFormat, sheet, "Target", row, column++);
				putHeaderDefault(feemergeFormat, sheet, "Achieve", row, column++);
				putHeaderDefault(feemergeFormat, sheet, "Percentage", row, column++);

				int serNo = 0;

				for (Integer employeeId : employeePerformanceMap.keySet()) {
					HashMap<Integer, Object[]> kpiDetails = employeePerformanceMap.get(employeeId);
					if (kpiDetails != null) {
						String[] employeeDetails = employeeDetailsMap.get(employeeId);
						for (Integer kpiId : kpiDetails.keySet()) {
							serNo++;
							column = 0;
							row++;

							putHeaderDefault(headerFormat, sheet, serNo, row, column++);
							putHeaderDefault(headerFormat, sheet, employeeDetails[1], row, column++);
							putHeaderDefault(headerFormat, sheet, employeeDetails[0], row, column++);
							putHeaderDefault(headerFormat, sheet, employeeDetails[2], row, column++);
							putHeaderDefault(headerFormat, sheet, employeeDetails[3], row, column++);
							putHeaderDefault(headerFormat, sheet, employeeDetails[4], row, column++);

							if (kpiDetails.get(kpiId) != null) {
								Object[] details = kpiDetails.get(kpiId);
								putHeaderDefault(headerFormat, sheet, details[1], row, column++);
								putHeader(numbercellformat, sheet, Translator.doubleValue(details[0]), row, column++);
								putHeader(numbercellformat, sheet, Translator.doubleValue(details[2]), row, column++);
								putHeader(numbercellformat, sheet, Translator.doubleValue(details[4]), row, column++);
							} else {
								putHeaderDefault(headerFormat, sheet, "", row, column++);
								putHeaderDefault(headerFormat, sheet, "", row, column++);
								putHeaderDefault(headerFormat, sheet, "", row, column++);
								putHeaderDefault(headerFormat, sheet, "", row, column++);
							}
						}
					}
				}
			}
			workbook.write();
			workbook.close();

		}
	}

	public void putHeaderDefault(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		sheet.setColumnView(column, 20);
		Label label = new Label(column, row, "" + value, cellFormat);
		sheet.addCell(label);
	}

	public void putHeader(WritableCellFormat numbercellformat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, numbercellformat);
		sheet.addCell(numobj);
	}

	private void putRecordsIntoMap(HashMap<Integer, HashMap<Integer, Object[]>> employeePerformanceMap, JSONArray employeePerformanceRecords, HashMap<Integer, String[]> employeeDetailsMap) throws JSONException {
		for (int counter = 0; counter < employeePerformanceRecords.length(); counter++) {
			Integer employeeId = 0;
			double target = Translator.doubleValue(employeePerformanceRecords.getJSONObject(counter).opt("target"));
			double achieved = Translator.doubleValue(employeePerformanceRecords.getJSONObject(counter).opt("achieved"));
			double amount = Translator.doubleValue(employeePerformanceRecords.getJSONObject(counter).opt("amount"));
			Integer keyperformanceindicatorid = Translator.integerValue(employeePerformanceRecords.getJSONObject(counter).opt("keyperformanceindicatorid"));
			String kpiName = Translator.stringValue(employeePerformanceRecords.getJSONObject(counter).opt("keyperformanceindicatorid.name"));
			Object employeeDetailsObject = employeePerformanceRecords.getJSONObject(counter).opt("employeeid");

			JSONArray employeeRecords = new JSONArray();
			if (employeeDetailsObject != null && employeeDetailsObject instanceof JSONArray) {
				employeeRecords = (JSONArray) employeeDetailsObject;
			} else if (employeeDetailsObject != null && employeeDetailsObject instanceof JSONObject) {
				employeeRecords = new JSONArray().put(employeeDetailsObject);
			}
			String employeeName = "";
			String employeeCode = "";
			String employeeDepartment = "";
			String employeeDesignation = "";
			String reportingTo = "";
			if (employeeRecords != null && employeeRecords.length() > 0) {
				employeeName = Translator.stringValue(employeeRecords.getJSONObject(0).opt("name"));
				employeeCode = Translator.stringValue(employeeRecords.getJSONObject(0).opt("employeecode"));
				employeeId = Translator.integerValue(employeeRecords.getJSONObject(0).opt(Updates.KEY));
				employeeDepartment = Translator.stringValue(employeeRecords.getJSONObject(0).opt("departmentid.name"));
				employeeDesignation = Translator.stringValue(employeeRecords.getJSONObject(0).opt("designationid.name"));
				reportingTo = Translator.stringValue(employeeRecords.getJSONObject(0).opt("reportingtoid.name"));
			}
			if (employeeId != 0) {
				HashMap<Integer, Object[]> kpis = new HashMap<Integer, Object[]>();
				Object[] details = new Object[5];
				details[0] = target;
				details[1] = kpiName;
				details[2] = achieved;
				details[3] = amount;
				details[4] = target == 0.0 ? 0.0 : (((float) achieved * 100) / target);
				if (!employeeDetailsMap.containsKey(employeeId)) {
					String[] employeeDetails = new String[] { employeeName, employeeCode, employeeDepartment, employeeDesignation, reportingTo };
					employeeDetailsMap.put(employeeId, employeeDetails);

				}
				if (employeePerformanceMap.containsKey(employeeId)) {
					kpis = employeePerformanceMap.get(employeeId);
				}
				kpis.put(keyperformanceindicatorid, details);
				employeePerformanceMap.put(employeeId, kpis);
			}
		}
	}

	private JSONArray getEmployeePerformanceRecords(Object monthId, Object yearId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "employeeperformance");
		columnArray.put(Updates.KEY);

		JSONArray innerColumns = new JSONArray();
		innerColumns.put("name");
		innerColumns.put("employeecode");
		innerColumns.put("departmentid.name");
		innerColumns.put("designationid.name");
		innerColumns.put("reportingtoid.name");
		innerColumns.put(Updates.KEY);
		columnArray.put(new JSONObject().put("employeeid", innerColumns));

		columnArray.put("target");
		columnArray.put("achieved");
		columnArray.put("keyperformanceindicatorid");
		columnArray.put("keyperformanceindicatorid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "performancemonthid = " + monthId + " and performanceyearid = " + yearId + " and target != null");
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeeperformance");
		return rows;
	}

}
