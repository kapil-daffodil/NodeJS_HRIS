package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class PFDatailReportAccountWise extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		calculateAndWriteToSheet(req, resp);
	}

	private void calculateAndWriteToSheet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			resp.setContentType("application/vnd.ms-excel");
			String decompositionParameter = "attachment;filename=PF_Form_Account_Wise.xls";
			resp.setHeader("Content-disposition", decompositionParameter);
			resp.setHeader("Content-Type", "application/octet-stream");

			WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 22, WritableFont.BOLD);
			WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
			headerFormat.setAlignment(Alignment.CENTRE);

			String key = req.getParameter("keys");

			if (key.length() > 0) {
				key = key.substring(1, key.length() - 1);
				JSONArray pfReportArray = getPFReportArray(key);
				int year = 0;
				int branchId = 0;
				String branchName = "";
				Date fromDate = null;
				Date toDate = null;
				String firstDateInString = "";
				String lastDateInString = "";
				JSONArray branchArray = new JSONArray();
				if (pfReportArray != null && pfReportArray.length() > 0) {
					year = Translator.integerValue(pfReportArray.getJSONObject(0).opt("yearid.name"));
					branchId = Translator.integerValue(pfReportArray.getJSONObject(0).opt("branchid"));
					branchName = Translator.stringValue(pfReportArray.getJSONObject(0).opt("branchid.name"));
					firstDateInString = "" + pfReportArray.getJSONObject(0).opt("fromdate");
					lastDateInString = "" + pfReportArray.getJSONObject(0).opt("todate");
					fromDate = Translator.dateValue(pfReportArray.getJSONObject(0).opt("fromdate"));
					toDate = Translator.dateValue(pfReportArray.getJSONObject(0).opt("todate"));
				}
				if (branchId != 0) {
					JSONObject object = new JSONObject();
					object.put("__key__", branchId);
					object.put("name", branchName);
					branchArray.put(object);
				} else {
					branchArray = getBranche();
				}
				Date TODAY_DATE = Translator.dateValue(new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime()));
				WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
				WritableCellFormat cellFormat = new WritableCellFormat(font);
				cellFormat.setBackground(Colour.GRAY_25);
				cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
				WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
				WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
				cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
				WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
				WritableSheet sheet = workbook.createSheet("Sheet_1", 0);
				Object basicSalaryComponentId = PunchingUtility.getId("salarycomponents", "salarycomponenttypeid", "" + HRISApplicationConstants.SalaryComponentTypes.BASIC);
				Object pfSalaryComponentId = PunchingUtility.getId("salarycomponents", "salarycomponenttypeid", "" + HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYEE);
				if (fromDate == null || toDate == null) {
					Calendar pfReportPeriod = Calendar.getInstance();
					pfReportPeriod.setTime(TODAY_DATE);
					year = pfReportPeriod.get(Calendar.YEAR);
					if (fromDate == null) {
						pfReportPeriod.set(Calendar.DAY_OF_MONTH, 1);
						pfReportPeriod.set(Calendar.MONTH, 4 - 1); // Calendar Start Month from 0 sir for April we subtract 1
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
				}
				long dateDiff = DataTypeUtilities.monthsBetweenDates(fromDate, toDate);
				// JSONArray employeeArray = getEmployeeArray(firstDateInString, branchId);
				Calendar forBaicAmount = Calendar.getInstance();
				int row = 1;
				int column = 0;
				LinkedHashMap<String, Object[]> grandTotalMap = new LinkedHashMap<String, Object[]>();
				sheet.mergeCells(column + 1, row, column + 8, row);
				putHeader(sheet, "Daffodil PF amount from: " + firstDateInString + " to " + lastDateInString, cellFormat1, row, column + 1);
				row++;
				for (int counter = 0; counter < branchArray.length(); counter++) {
					row += 2;
					column = 0;
					Object branchIdForSheet = branchArray.getJSONObject(counter).opt("__key__");
					Object branchNameForSheet = branchArray.getJSONObject(counter).opt("name");

					HrisHelper.setHeaderValue(column++, row, "P.F detail" + branchNameForSheet, cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Basic wages due", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Employer's share", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "A/C no 10", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "A/C no 21", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Employee's share", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "A.D.M charges", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "A/C no 22", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Total", cellFormat, sheet);

					double totalBasic = 0.0;
					double totalPercent3_67 = 0.0;
					double totalPercent8_33 = 0.0;
					double totalPercent0_5 = 0.0;
					double totalPercent8_33__3_67 = 0.0;
					double totalPercent1_1 = 0.0;
					double totalPercent0_01 = 0.0;
					double totalPercent = 0.0;

					for (int basicMonthCounter = 0; basicMonthCounter < dateDiff; basicMonthCounter++) {
						row++;
						column = 0;
						forBaicAmount.setTime(fromDate);
						forBaicAmount.add(Calendar.MONTH, basicMonthCounter);
						int monthId = forBaicAmount.get(Calendar.MONTH) + 1;
						long yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(forBaicAmount.get(Calendar.YEAR)));
						String monthName = EmployeeSalaryGenerationServlet.getMonthName(monthId);
						double paidBasicAmount = getPaidBasicAmount(monthId, yearId, basicSalaryComponentId, pfSalaryComponentId, branchIdForSheet);

						double percent3_67 = (float) ((paidBasicAmount * 3.67) / 100);
						double percent8_33 = (float) ((paidBasicAmount * 8.33) / 100);
						double percent0_50 = (float) ((paidBasicAmount * 0.5) / 100);
						double percent1_1 = (float) ((paidBasicAmount * 1.1) / 100);
						double percent0_01 = (float) ((paidBasicAmount * 0.01) / 100);

						double totalAllCalculated = percent3_67 + percent8_33 + percent0_50 + percent1_1 + percent0_01;
						double total8_33__3_67 = percent3_67 + percent8_33;

						totalBasic += paidBasicAmount;
						totalPercent3_67 += percent3_67;
						totalPercent8_33 += percent8_33;
						totalPercent0_5 += percent0_50;
						totalPercent8_33__3_67 += total8_33__3_67;
						totalPercent1_1 += percent1_1;
						totalPercent0_01 += percent0_01;
						totalPercent += totalAllCalculated;

						putHeader(sheet, monthName, cellFormat1, row, column++);
						putHeader(sheet, (int) (paidBasicAmount + 0.50), cellFormat1, row, column++);
						putHeader(sheet, (int) (percent3_67 + 0.50), cellFormat1, row, column++);
						putHeader(sheet, (int) (percent8_33 + 0.50), cellFormat1, row, column++);
						putHeader(sheet, (int) (percent0_50 + 0.50), cellFormat1, row, column++);
						putHeader(sheet, (int) (total8_33__3_67 + 0.50), cellFormat1, row, column++);
						putHeader(sheet, (int) (percent1_1 + 0.50), cellFormat1, row, column++);
						putHeader(sheet, (int) (percent0_01 + 0.50), cellFormat1, row, column++);
						putHeader(sheet, (int) (totalAllCalculated + 0.50), cellFormat1, row, column++);
						Object[] details = new Object[8];
						details[0] = paidBasicAmount;
						details[1] = percent3_67;
						details[2] = percent8_33;
						details[3] = percent0_50;
						details[4] = total8_33__3_67;
						details[5] = percent1_1;
						details[6] = percent0_01;
						details[7] = totalAllCalculated;

						if (grandTotalMap.containsKey(monthId)) {
							details = grandTotalMap.get(monthId);
							details[0] = Translator.doubleValue(details[0]) + paidBasicAmount;
							details[1] = Translator.doubleValue(details[1]) + percent3_67;
							details[2] = Translator.doubleValue(details[2]) + percent8_33;
							details[3] = Translator.doubleValue(details[3]) + percent0_50;
							details[4] = Translator.doubleValue(details[4]) + total8_33__3_67;
							details[5] = Translator.doubleValue(details[5]) + percent1_1;
							details[6] = Translator.doubleValue(details[6]) + percent0_01;
							details[7] = Translator.doubleValue(details[7]) + totalAllCalculated;
						}
						grandTotalMap.put(("" + monthId + "/" + yearId), details);

					}
					row++;
					column = 0;
					putHeader(sheet, "", cellFormat1, row, column++);
					putHeader(sheet, (int) (totalBasic + 0.50), cellFormat1, row, column++);
					putHeader(sheet, (int) (totalPercent3_67 + 0.50), cellFormat1, row, column++);
					putHeader(sheet, (int) (totalPercent8_33 + 0.50), cellFormat1, row, column++);
					putHeader(sheet, (int) (totalPercent0_5 + 0.50), cellFormat1, row, column++);
					putHeader(sheet, (int) (totalPercent8_33__3_67 + 0.50), cellFormat1, row, column++);
					putHeader(sheet, (int) (totalPercent1_1 + 0.50), cellFormat1, row, column++);
					putHeader(sheet, (int) (totalPercent0_01 + 0.50), cellFormat1, row, column++);
					putHeader(sheet, (int) (totalPercent + 0.50), cellFormat1, row, column++);

				}

				int finalRow = (branchArray.length() * (int) dateDiff) + (branchArray.length() * 4) + 3;
				int finalColumn = 0;
				sheet.mergeCells(finalColumn + 1, finalRow, finalColumn + 8, finalRow);

				HrisHelper.setHeaderValue(finalColumn + 1, finalRow, "Total Amount of P.F.", cellFormat, sheet);
				finalRow++;
				HrisHelper.setHeaderValue(finalColumn++, finalRow, "P.F detail", cellFormat, sheet);
				HrisHelper.setHeaderValue(finalColumn++, finalRow, "Basic wages due", cellFormat, sheet);
				HrisHelper.setHeaderValue(finalColumn++, finalRow, "Employer's share", cellFormat, sheet);
				HrisHelper.setHeaderValue(finalColumn++, finalRow, "A/C no 10", cellFormat, sheet);
				HrisHelper.setHeaderValue(finalColumn++, finalRow, "A/C no 21", cellFormat, sheet);
				HrisHelper.setHeaderValue(finalColumn++, finalRow, "Employee's share", cellFormat, sheet);
				HrisHelper.setHeaderValue(finalColumn++, finalRow, "A.D.M charges", cellFormat, sheet);
				HrisHelper.setHeaderValue(finalColumn++, finalRow, "A/C no 22", cellFormat, sheet);
				HrisHelper.setHeaderValue(finalColumn++, finalRow, "Total", cellFormat, sheet);
				double totalBasic = 0.0;
				double totalPercent3_67 = 0.0;
				double totalPercent8_33 = 0.0;
				double totalPercent0_5 = 0.0;
				double totalPercent8_33__3_67 = 0.0;
				double totalPercent1_1 = 0.0;
				double totalPercent0_01 = 0.0;
				double totalPercent = 0.0;
				for (String monthIdCounter : grandTotalMap.keySet()) {
					finalRow++;
					finalColumn = 0;
					Object[] details = grandTotalMap.get(monthIdCounter);
					String[] monthAndYear = monthIdCounter.split("/");
					String monthName = EmployeeSalaryGenerationServlet.getMonthName(Translator.integerValue(monthAndYear[0]));
					double paidBasicAmount = Translator.doubleValue(details[0]);
					double percent3_67 = Translator.doubleValue(details[1]);
					double percent8_33 = Translator.doubleValue(details[2]);
					double percent0_50 = Translator.doubleValue(details[3]);
					double total8_33__3_67 = Translator.doubleValue(details[4]);
					double percent1_1 = Translator.doubleValue(details[5]);
					double percent0_01 = Translator.doubleValue(details[6]);
					double totalAllCalculated = Translator.doubleValue(details[7]);

					totalBasic += paidBasicAmount;
					totalPercent3_67 += percent3_67;
					totalPercent8_33 += percent8_33;
					totalPercent0_5 += percent0_50;
					totalPercent8_33__3_67 += total8_33__3_67;
					totalPercent1_1 += percent1_1;
					totalPercent0_01 += percent0_01;
					totalPercent += totalAllCalculated;

					putHeader(sheet, "" + monthName, cellFormat1, finalRow, finalColumn++);
					putHeader(sheet, (int) (paidBasicAmount + 0.50), cellFormat1, finalRow, finalColumn++);
					putHeader(sheet, (int) (percent3_67 + 0.50), cellFormat1, finalRow, finalColumn++);
					putHeader(sheet, (int) (percent8_33 + 0.50), cellFormat1, finalRow, finalColumn++);
					putHeader(sheet, (int) (percent0_50 + 0.50), cellFormat1, finalRow, finalColumn++);
					putHeader(sheet, (int) (total8_33__3_67 + 0.50), cellFormat1, finalRow, finalColumn++);
					putHeader(sheet, (int) (percent1_1 + 0.50), cellFormat1, finalRow, finalColumn++);
					putHeader(sheet, (int) (percent0_01 + 0.50), cellFormat1, finalRow, finalColumn++);
					putHeader(sheet, (int) (totalAllCalculated + 0.50), cellFormat1, finalRow, finalColumn++);

				}
				finalRow++;
				finalColumn = 0;
				putHeader(sheet, "", cellFormat1, finalRow, finalColumn++);
				putHeader(sheet, (int) (totalBasic + 0.50), cellFormat1, finalRow, finalColumn++);
				putHeader(sheet, (int) (totalPercent3_67 + 0.50), cellFormat1, finalRow, finalColumn++);
				putHeader(sheet, (int) (totalPercent8_33 + 0.50), cellFormat1, finalRow, finalColumn++);
				putHeader(sheet, (int) (totalPercent0_5 + 0.50), cellFormat1, finalRow, finalColumn++);
				putHeader(sheet, (int) (totalPercent8_33__3_67 + 0.50), cellFormat1, finalRow, finalColumn++);
				putHeader(sheet, (int) (totalPercent1_1 + 0.50), cellFormat1, finalRow, finalColumn++);
				putHeader(sheet, (int) (totalPercent0_01 + 0.50), cellFormat1, finalRow, finalColumn++);
				putHeader(sheet, (int) (totalPercent + 0.50), cellFormat1, finalRow, finalColumn++);
				workbook.write();
				workbook.close();
			}
		} catch (Exception e) {
			LogUtility.writeLog("PFAnnualReturnReportForm3ARevised >> calculateAndWriteToSheet Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "PFAnnualReturnReportForm3ARevised >> calculateAndWriteToSheet Exception Trace");
		}
	}

	private static void putHeader(WritableSheet sheet, Object value, WritableCellFormat cellFormat, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, cellFormat);
		sheet.setColumnView(column, 20);
		sheet.addCell(label);
	}

	private double getPaidBasicAmount(int monthId, long yearId, Object basicSalaryComponentId, Object pfSalaryComponentId, Object branchIdForSheet) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("amount");
		columnArray.put("employeeid");
		columnArray.put("salarycomponentid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarymonthid = " + monthId + "  and salaryyearid = " + yearId + " and salarycomponentid = " + basicSalaryComponentId + " AND employeeid.branchid=" + branchIdForSheet);
		query.put(Data.Query.OPTION_FILTERS, "salarymonthid = " + monthId + "  and salaryyearid = " + yearId + " and salarycomponentid = " + pfSalaryComponentId + " AND employeeid.branchid=" + branchIdForSheet);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray amountArray = employeeObject.getJSONArray("employeemonthlysalarycomponents");
		double amount = 0.0;
		if (amountArray != null) {
			HashMap<Integer, HashMap<Integer, Double>> map = new HashMap<Integer, HashMap<Integer, Double>>();
			HashMap<Integer, Double> innerMap = null;

			for (int counter = 0; counter < amountArray.length(); counter++) {
				int componentId = Translator.integerValue(amountArray.getJSONObject(counter).opt("salarycomponentid"));
				int employeeId = Translator.integerValue(amountArray.getJSONObject(counter).opt("employeeid"));
				double payableAmount = Translator.doubleValue(amountArray.getJSONObject(counter).opt("amount"));
				if (payableAmount > 6500) {
					payableAmount = 6500;
				}
				innerMap = new HashMap<Integer, Double>();
				if (map.containsKey(employeeId)) {
					innerMap = map.get(employeeId);
					if (innerMap.containsKey(componentId)) {
						payableAmount += innerMap.get(componentId);
					}
				}
				innerMap.put(componentId, payableAmount);
				map.put(employeeId, innerMap);
			}
			int pfId = Translator.integerValue(pfSalaryComponentId);
			for (Integer employeeId : map.keySet()) {
				innerMap = map.get(employeeId);
				if (innerMap.containsKey(pfId)) {
					amount += Translator.doubleValue(innerMap.get(Translator.integerValue(basicSalaryComponentId)));
				}
			}
		}
		return amount;
	}

	private JSONArray getPFReportArray(String key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "pfreportforyear");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("yearid.name");
		columnArray.put("branchid");
		columnArray.put("fromdate");
		columnArray.put("todate");
		columnArray.put("branchid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray pfReportArray = employeeObject.getJSONArray("pfreportforyear");
		return pfReportArray;
	}

	// public static JSONArray getEmployeeArray(String firstDateInString, int branchId) throws JSONException {
	// String extraFilter = "";
	// if (branchId != 0) {
	// extraFilter += " and branchid = " + branchId;
	// }
	// JSONObject query = new JSONObject();
	// query.put(Data.Query.RESOURCE, "hris_employees");
	// JSONArray columnArray = new JSONArray();
	// columnArray.put("__key__");
	// columnArray.put("name");
	// columnArray.put("employeecode");
	// columnArray.put("accountno");
	// columnArray.put("fathername");
	// query.put(Data.Query.COLUMNS, columnArray);
	// query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + extraFilter);
	// query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + extraFilter + " and relievingdate >= '" + firstDateInString + "'");
	// JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
	// JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
	// return employeeArray;
	// }

	public static JSONArray getBranche() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_branches");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray branchArray = object.getJSONArray("hris_branches");
		return branchArray;
	}
}
