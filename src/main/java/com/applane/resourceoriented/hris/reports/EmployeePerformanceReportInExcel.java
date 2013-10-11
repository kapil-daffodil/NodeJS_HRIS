package com.applane.resourceoriented.hris.reports;

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

public class EmployeePerformanceReportInExcel extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			resp.setContentType("application/vnd.ms-excel");
			String decompositionParameter = "attachment;filename=EmployeePerformance.xls";
			resp.setHeader("Content-disposition", decompositionParameter);
			resp.setHeader("Content-Type", "application/octet-stream");

			String key = req.getParameter("keys");
			if (key.length() > 2) {
				key = key.substring(1, key.length() - 1);

				WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
				WritableSheet sheet = workbook.createSheet("Performance", 0);

				putPerformanceIntoSheet(key, sheet);

				workbook.write();
				workbook.close();

			}
		} catch (BusinessLogicException e) {

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeePerformanceReportInExcel.class.getName(), e);
			LogUtility.writeLog("EmployeePerformanceReportInExcel >> trace >> " + trace);
		}
	}

	private void putPerformanceIntoSheet(String key, WritableSheet sheet) throws JSONException, Exception {

		WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
		// headerFormat.setAlignment(Alignment.CENTRE);

		WritableFont feemergefont = new WritableFont(WritableFont.ARIAL, 13, WritableFont.BOLD);
		WritableCellFormat feemergeFormat = new WritableCellFormat(feemergefont);
		feemergeFormat.setAlignment(Alignment.CENTRE);
		feemergeFormat.setBackground(Colour.GRAY_25);
		feemergeFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
		numbercellformat.setFont(font);
		numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);

		JSONArray kpiArray = getKPIs();
		HashMap<Integer, String> kpisMap = new HashMap<Integer, String>();
		putKPIintoMap(kpisMap, kpiArray);
		JSONArray performanceRecords = getPerformanceRecords(key);
		for (int counter = 0; counter < performanceRecords.length(); counter++) {
			Object monthId = performanceRecords.getJSONObject(counter).opt("monthid");
			Object yearId = performanceRecords.getJSONObject(counter).opt("yearid");
			JSONArray employeePerformanceRecords = getEmployeePerformanceRecords(monthId, yearId);
			HashMap<Integer, HashMap<Integer, Double[]>> employeePerformanceMap = new HashMap<Integer, HashMap<Integer, Double[]>>();
			HashMap<Integer, String[]> employeeDetailsMap = new HashMap<Integer, String[]>();
			putRecordsIntoMap(employeePerformanceMap, employeePerformanceRecords, employeeDetailsMap);
			int row = 5;
			int column = 0;
			sheet.mergeCells(column + 2, row - 4, column + 8, row - 4);
			putHeaderDefault(feemergeFormat, sheet, "Employee Performance Sheet for " + EmployeeSalaryGenerationServlet.getMonthName((long) Translator.integerValue(monthId)) + " - " + EmployeeSalaryGenerationServlet.getYearName((long) Translator.integerValue(yearId)), row - 4, column + 2);

			putHeaderDefault(feemergeFormat, sheet, "S. No.", row - 1, column++);
			putHeaderDefault(feemergeFormat, sheet, "Employee Code", row - 1, column++);
			putHeaderDefault(feemergeFormat, sheet, "Employee Name", row - 1, column++);

			for (Integer kpiId : kpisMap.keySet()) {
				String kpiName = kpisMap.get(kpiId);
				sheet.mergeCells(column, row - 2, column + 3, row - 2);
				putHeaderDefault(feemergeFormat, sheet, kpiName, row - 2, column);
				putHeaderDefault(feemergeFormat, sheet, "Target", row - 1, column);
				putHeaderDefault(feemergeFormat, sheet, "Achieve", row - 1, column + 1);
				putHeaderDefault(feemergeFormat, sheet, "Percentage", row - 1, column + 2);
				putHeaderDefault(feemergeFormat, sheet, "Amount", row - 1, column + 3);
				column = column + 4;
			}
			int serNo = 0;

			for (Integer employeeId : employeePerformanceMap.keySet()) {
				column = 0;
				HashMap<Integer, Double[]> kpiDetails = employeePerformanceMap.get(employeeId);
				if (kpiDetails != null) {
					serNo++;
					String[] employeeDetails = employeeDetailsMap.get(employeeId);
					putHeaderDefault(headerFormat, sheet, serNo, row, column++);
					putHeaderDefault(headerFormat, sheet, employeeDetails[1], row, column++);
					putHeaderDefault(headerFormat, sheet, employeeDetails[0], row, column++);

					for (Integer kpiId : kpisMap.keySet()) {
						if (kpiDetails.get(kpiId) != null) {
							Double[] details = kpiDetails.get(kpiId);
							putHeader(numbercellformat, sheet, details[0], row, column++);
							putHeader(numbercellformat, sheet, details[1], row, column++);
							putHeader(numbercellformat, sheet, details[3], row, column++);
							putHeader(numbercellformat, sheet, details[2], row, column++);
						} else {
							putHeaderDefault(headerFormat, sheet, "", row, column++);
							putHeaderDefault(headerFormat, sheet, "", row, column++);
							putHeaderDefault(headerFormat, sheet, "", row, column++);
							putHeaderDefault(headerFormat, sheet, "", row, column++);
						}
					}
					row++;
				}
			}
		}
	}

	private void putRecordsIntoMap(HashMap<Integer, HashMap<Integer, Double[]>> employeePerformanceMap, JSONArray employeePerformanceRecords, HashMap<Integer, String[]> employeeDetailsMap) throws JSONException {
		for (int counter = 0; counter < employeePerformanceRecords.length(); counter++) {
			Integer employeeId = Translator.integerValue(employeePerformanceRecords.getJSONObject(counter).opt("employeeid"));
			double target = Translator.doubleValue(employeePerformanceRecords.getJSONObject(counter).opt("target"));
			double achieved = Translator.doubleValue(employeePerformanceRecords.getJSONObject(counter).opt("achieved"));
			double amount = Translator.doubleValue(employeePerformanceRecords.getJSONObject(counter).opt("amount"));
			Integer keyperformanceindicatorid = Translator.integerValue(employeePerformanceRecords.getJSONObject(counter).opt("keyperformanceindicatorid"));
			String employeeName = Translator.stringValue(employeePerformanceRecords.getJSONObject(counter).opt("employeeid.name"));
			String employeeCode = Translator.stringValue(employeePerformanceRecords.getJSONObject(counter).opt("employeeid.employeecode"));
			HashMap<Integer, Double[]> kpis = new HashMap<Integer, Double[]>();
			Double[] details = new Double[6];
			details[0] = target;
			details[1] = achieved;
			details[2] = amount;
			details[3] = target == 0.0 ? 0.0 : (((float) achieved * 100) / target);
			if (!employeeDetailsMap.containsKey(employeeId)) {
				String[] employeeDetails = new String[] { employeeName, employeeCode };
				employeeDetailsMap.put(employeeId, employeeDetails);

			}
			if (employeePerformanceMap.containsKey(employeeId)) {
				kpis = employeePerformanceMap.get(employeeId);
			}
			kpis.put(keyperformanceindicatorid, details);
			employeePerformanceMap.put(employeeId, kpis);
		}
	}

	private JSONArray getEmployeePerformanceRecords(Object monthId, Object yearId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "employeeperformance");
		columnArray.put(Updates.KEY);
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("target");
		columnArray.put("achieved");
		columnArray.put("amount");
		columnArray.put("keyperformanceindicatorid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "performancemonthid = " + monthId + " and performanceyearid = " + yearId + " and achieved != null");
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeeperformance");
	}

	public JSONArray getPerformanceRecords(String key) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "populateemployeekpi");
		columnArray.put(Updates.KEY);
		columnArray.put("monthid");
		columnArray.put("yearid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " in (" + key + ")");
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("populateemployeekpi");
	}

	private void putKPIintoMap(HashMap<Integer, String> kpisMap, JSONArray kpiArray) throws JSONException {
		for (int counter = 0; counter < kpiArray.length(); counter++) {
			Integer key = Translator.integerValue(kpiArray.getJSONObject(counter).opt(Updates.KEY));
			String name = Translator.stringValue(kpiArray.getJSONObject(counter).opt("name"));
			if (!kpisMap.containsKey(key)) {
				kpisMap.put(key, name);
			}
		}

	}

	private JSONArray getKPIs() throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "keyperformanceindicators");
		columnArray.put(Updates.KEY);
		columnArray.put("name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "kpitypeid = 1");
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("keyperformanceindicators");
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
}
