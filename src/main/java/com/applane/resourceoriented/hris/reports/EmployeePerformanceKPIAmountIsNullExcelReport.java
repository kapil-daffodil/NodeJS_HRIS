package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;

public class EmployeePerformanceKPIAmountIsNullExcelReport extends HttpServlet {

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
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeePerformanceKPIAmountIsNullExcelReport.class.getName(), e);
			LogUtility.writeLog("EmployeePerformanceReportInExcel >> trace >> " + trace);
		}
	}

	private void putPerformanceIntoSheet(String key, WritableSheet sheet) throws JSONException, Exception {

		WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
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

		JSONArray kpiArray = getKPIs();
		HashMap<Integer, String> kpisMap = new HashMap<Integer, String>();
		putKPIintoMap(kpisMap, kpiArray);
		JSONArray performanceRecords = getPerformanceRecords(key);
		for (int counter = 0; counter < performanceRecords.length(); counter++) {
			int monthId = Translator.integerValue(performanceRecords.getJSONObject(counter).opt("monthid"));
			int yearId = Translator.integerValue(performanceRecords.getJSONObject(counter).opt("yearid"));

			Date monthFirstDate = EmployeeSalaryGenerationServlet.getMonthFirstDate(yearId, monthId);
			String monthFirstDateString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
			String monthLastDateString = EmployeeSalaryGenerationServlet.getDateInString(DataTypeUtilities.getMonthLastDate(monthFirstDate));

			JSONArray employeeSalaryComponents = getEmployeeSalaryComponents(monthFirstDateString, monthLastDateString);
			Map<Integer, List<Integer>> employeeKPIsMap = new HashMap<Integer, List<Integer>>();
			Map<Integer, String> kPIsNameMap = new HashMap<Integer, String>();
			Map<Integer, String[]> employeeDetailsMap = new HashMap<Integer, String[]>();
			putKpisInEmployeeMap(employeeKPIsMap, employeeSalaryComponents, kPIsNameMap, employeeDetailsMap);

			int row = 5;
			int column = 0;
			sheet.mergeCells(column + 2, row - 4, column + 8, row - 4);
			putHeaderDefault(feemergeFormat, sheet, "Employee Performance Sheet Amount is NULL or 0 for " + EmployeeSalaryGenerationServlet.getMonthName((long) Translator.integerValue(monthId)) + " - " + EmployeeSalaryGenerationServlet.getYearName((long) Translator.integerValue(yearId)), row - 4, column + 2);

			putHeaderDefault(feemergeFormat, sheet, "S. No.", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "Employee Code", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "Employee Name", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "KPI Name", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "Status", row, column++);
			int srNo = 1;
			for (Integer employeeId : employeeKPIsMap.keySet()) {
				List<Integer> list = employeeKPIsMap.get(employeeId);
				if (list != null && list.size() > 0) {
					String kpiIds = list.toString();
					kpiIds = kpiIds.substring(1, list.toString().length() - 1);
					String[] employeeDetails = employeeDetailsMap.get(employeeId);
					JSONArray employeePerformanceRecords = getEmployeePerformanceRecords(monthId, yearId, kpiIds, employeeId);

					column = 0;
					int empRecRow = row;
					boolean putRecord = false;

					if (employeePerformanceRecords != null && employeePerformanceRecords.length() > 0) {
						List<Integer> foundKpi = new ArrayList<Integer>();
						for (int performanceKpiCounter = 0; performanceKpiCounter < employeePerformanceRecords.length(); performanceKpiCounter++) {
							Object amount = employeePerformanceRecords.getJSONObject(performanceKpiCounter).opt("amount");
							int kpiId = employeePerformanceRecords.getJSONObject(performanceKpiCounter).optInt("keyperformanceindicatorid");
							foundKpi.add(kpiId);
							if (amount == null) {
								putRecord = true;
								row++;
								column = 3;
								putHeaderDefault(headerFormat, sheet, kPIsNameMap.get(kpiId), row, column++);
								putHeaderDefault(headerFormat, sheet, "Amount Is Null", row, column++);
							} else if (Translator.doubleValue(amount) == 0.0) {
								putRecord = true;
								row++;
								column = 3;
								putHeaderDefault(headerFormat, sheet, kPIsNameMap.get(kpiId), row, column++);
								putHeaderDefault(headerFormat, sheet, "Amount Is Null", row, column++);
							}
						}
						if (foundKpi.size() != list.size()) {
							for (int kpiCounter = 0; kpiCounter < list.size(); kpiCounter++) {
								if (!foundKpi.contains(list.get(kpiCounter))) {
									putRecord = true;
									row++;
									column = 3;
									putHeaderDefault(headerFormat, sheet, kPIsNameMap.get(list.get(kpiCounter)), row, column++);
									HrisHelper.setHeaderValue(column++, row, "KPI Not Found in Employee Performance", headerFormat, sheet);
								}
							}
						}
					} else {
						for (int kpiCounter = 0; kpiCounter < list.size(); kpiCounter++) {
							putRecord = true;
							row++;
							column = 3;
							putHeaderDefault(headerFormat, sheet, kPIsNameMap.get(list.get(kpiCounter)), row, column++);
							HrisHelper.setHeaderValue(column++, row, "KPI Not Found in Employee Performance", headerFormat, sheet);
						}
					}
					if (putRecord) {
						row++;
						column = 0;
						putHeaderDefault(headerFormat, sheet, srNo++, empRecRow, column++);
						putHeaderDefault(headerFormat, sheet, employeeDetails[1], empRecRow, column++);
						putHeaderDefault(headerFormat, sheet, employeeDetails[0], empRecRow, column++);
					}
				}
			}
		}
	}

	private void putKpisInEmployeeMap(Map<Integer, List<Integer>> employeeKPIsMap, JSONArray employeeSalaryComponents, Map<Integer, String> kPIsNameMap, Map<Integer, String[]> employeeDetailsMap) throws JSONException {
		for (int counter = 0; counter < employeeSalaryComponents.length(); counter++) {
			int employeeId = employeeSalaryComponents.getJSONObject(counter).optInt("employeeid", 0);
			int kpiId = employeeSalaryComponents.getJSONObject(counter).optInt("salarycomponentid.keyperformanceindicatorid", 0);
			String kpiName = employeeSalaryComponents.getJSONObject(counter).optString("salarycomponentid.keyperformanceindicatorid.name", "");

			String employeeName = employeeSalaryComponents.getJSONObject(counter).optString("employeeid.name", "");
			String employeeCode = employeeSalaryComponents.getJSONObject(counter).optString("employeeid.employeecode", "");
			if (!kPIsNameMap.containsKey(kpiId)) {
				kPIsNameMap.put(kpiId, kpiName);
			}
			if (!employeeDetailsMap.containsKey(employeeId)) {
				employeeDetailsMap.put(employeeId, new String[] { employeeName, employeeCode });
			}
			List<Integer> list = new ArrayList<Integer>();
			if (employeeKPIsMap.containsKey(employeeId)) {
				list = employeeKPIsMap.get(employeeId);
			}
			list.add(kpiId);
			employeeKPIsMap.put(employeeId, list);
		}

	}

	public static JSONArray getEmployeeSalaryComponents(String monthFirstDate, String monthLastDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.keyperformanceindicatorid");// KPI belongs to organization level salary components
		columnArray.put("salarycomponentid.keyperformanceindicatorid.name");
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, "applicablefrom");
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		orders.put(orderObject);
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid.employeestatusid = 1 AND employeeid.joiningdate <= '" + monthLastDate + "' and applicablefrom <= '" + monthFirstDate + "' and applicableto >= '" + monthFirstDate + "' and amount > 0.0 and salarycomponentid.componentcriteriaid IN(" + HRISApplicationConstants.PERFORMANCE_BASED + "," + HRISApplicationConstants.EXPERIENCE_BASED + ")");
		query.put(Data.Query.OPTION_FILTERS, "employeeid.employeestatusid = 2 AND employeeid.relievingdate >= '" + monthFirstDate + "' and applicablefrom <= '" + monthLastDate + "' and applicableto >= '" + monthLastDate + "' and amount > 0.0 and salarycomponentid.componentcriteriaid IN(" + HRISApplicationConstants.PERFORMANCE_BASED + "," + HRISApplicationConstants.EXPERIENCE_BASED + ")");
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("employeesalarycomponents");

		String keys = "";
		for (int counter = 0; counter < rows.length(); counter++) {
			int key = Translator.integerValue(rows.getJSONObject(counter).opt("__key__"));
			if (keys.length() > 0) {
				keys += ("," + key);
			} else {
				keys += ("" + key);
			}
		}
		String filters = "";
		if (keys.length() > 0) {
			filters += (" and __key__ not in (" + keys + ")");
		}
		query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.keyperformanceindicatorid");// KPI belongs to organization level salary components
		columnArray.put("salarycomponentid.keyperformanceindicatorid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid.employeestatusid = 1 AND employeeid.joiningdate <= '" + monthLastDate + "' and applicablefrom >= '" + monthFirstDate + "' and applicableto <= '" + monthLastDate + "' and amount > 0.0" + filters + " and salarycomponentid.componentcriteriaid IN(" + HRISApplicationConstants.PERFORMANCE_BASED + "," + HRISApplicationConstants.EXPERIENCE_BASED + ")");
		query.put(Data.Query.FILTERS, "employeeid.employeestatusid = 2 AND employeeid.relievingdate >= '" + monthLastDate + "' and applicablefrom >= '" + monthFirstDate + "' and applicableto <= '" + monthLastDate + "' and amount > 0.0" + filters + " and salarycomponentid.componentcriteriaid IN(" + HRISApplicationConstants.PERFORMANCE_BASED + "," + HRISApplicationConstants.EXPERIENCE_BASED + ")");
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows1 = object.getJSONArray("employeesalarycomponents");

		for (int counter = 0; counter < rows1.length(); counter++) {
			JSONObject object1 = rows1.getJSONObject(counter);
			rows.put(object1);
		}

		return rows;
	}

	private JSONArray getEmployeePerformanceRecords(Object monthId, Object yearId, String kpiIds, Integer employeeId) throws JSONException {
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
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND performancemonthid = " + monthId + " AND performanceyearid = " + yearId + " AND keyperformanceindicatorid IN(" + kpiIds + ")");
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
