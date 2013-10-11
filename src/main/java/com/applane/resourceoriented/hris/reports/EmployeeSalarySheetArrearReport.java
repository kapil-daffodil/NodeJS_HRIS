package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
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

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants;

public class EmployeeSalarySheetArrearReport extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// private static final Object JSONArray = null;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		salarySheetExcelReport(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doGet(req, resp);
	}

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	private void salarySheetExcelReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Salary_Sheet_Arrear.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");

		try {
			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			WritableSheet sheet = workbook.createSheet("First Sheet", 0);

			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
			WritableCellFormat hdFormat = new WritableCellFormat(font);
			hdFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
			hdFormat.setAlignment(Alignment.CENTRE);

			WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
			numbercellformat.setFont(font);
			numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);
			String monthName = null;

			String yearName = null;
			String key = req.getParameter("keys");
			if (key.length() > 2) {
				key = key.substring(1, key.length() - 1);
			}
			String[] keys = key.split(",");
			key = keys[0];

			JSONArray salaryComponentArray = getSalaryComponents();

			int salaryComponentArrayCount = (salaryComponentArray == null || salaryComponentArray.length() == 0) ? 0 : salaryComponentArray.length();
			LinkedHashMap<Integer, String> componentsMap = new LinkedHashMap<Integer, String>();
			LinkedHashMap<String, Object[]> employeeSalaryRecords = new LinkedHashMap<String, Object[]>();
			HashMap<Integer, List<String>> employeeRecords = new HashMap<Integer, List<String>>();
			if (salaryComponentArrayCount > 0) {

				for (int counter = 0; counter < salaryComponentArrayCount; counter++) {
					// 16 salary components like HRA, basic
					String salaryComponent = salaryComponentArray.getJSONObject(counter).optString("name");
					int salaryComponentId = salaryComponentArray.getJSONObject(counter).optInt("__key__");
					componentsMap.put(salaryComponentId, salaryComponent);
				}
			}

			JSONArray takeSalarySheetArray = getTakeSalarySheetRecord(key);

			int takeSalarySheetArrayCount = (takeSalarySheetArray == null || takeSalarySheetArray.length() == 0) ? 0 : takeSalarySheetArray.length();
			if (takeSalarySheetArrayCount > 0) {

				Object branchId = takeSalarySheetArray.getJSONObject(0).opt("branchid");
				Object monthId = takeSalarySheetArray.getJSONObject(0).opt("monthid");
				if (monthId != null) {
					monthName = getMonthName(monthId);
				}
				Object yearId = takeSalarySheetArray.getJSONObject(0).opt("yearid");
				if (yearId != null) {
					yearName = getYearName(yearId);
				}

				Date monthFirstDate = null;
				if (monthName != null && monthName.length() > 0 && yearName != null && yearName.length() > 0) {
					monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
				}

				long maxDayInMonth = 0;
				if (monthFirstDate != null) {
					maxDayInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
				}

				JSONArray salarySheetArray = getSalarySheetRecords(monthId, yearId, branchId);

				int salarySheetArrayCount = (salarySheetArray == null || salarySheetArray.length() == 0) ? 0 : salarySheetArray.length();
				if (salarySheetArrayCount > 0) {

					for (int counter = 0; counter < salarySheetArrayCount; counter++) {
						HashMap<Integer, Object[]> employComponentDetail = new HashMap<Integer, Object[]>();
						Object[] employeeDetail = new Object[17];

						int employeeId = salarySheetArray.getJSONObject(counter).optInt("employeeid");
						String employeeName = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.name"));
						String invoiceDate = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("invoicedate"));
						String employeeCode = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.employeecode"));
						String accountNo = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.accountno"));
						String invoiceNumber = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("invoicenumber"));
						double presentDays = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("present"));
						double leaves = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("leaves"));
						double totalWorkingDays = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("totalworkingdays"));
						double totalOff = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("totaloff"));
						double holidays = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("holidays"));
						double nonWorkingDays = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("nonworkingdays"));
						// double attendanceBasedAmount = ((Number) (salarySheetArray.getJSONObject(counter).opt("attendancebasedamount") == null ? 0.0 : salarySheetArray.getJSONObject(counter).optDouble("attendancebasedamount"))).doubleValue();
						// double performanceBasedAmount = ((Number) (salarySheetArray.getJSONObject(counter).opt("performancebasedamount") == null ? 0.0 : salarySheetArray.getJSONObject(counter).optDouble("performancebasedamount"))).doubleValue();
						// double fixedBasedAmount = ((Number) (salarySheetArray.getJSONObject(counter).opt("fixedamount") == null ? 0.0 : salarySheetArray.getJSONObject(counter).opt("fixedamount"))).doubleValue();
						// double deductionAmount = ((Number) (salarySheetArray.getJSONObject(counter).opt("deductionamount") == null ? 0.0 : salarySheetArray.getJSONObject(counter).opt("deductionamount"))).doubleValue();
						double payableAmount = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("payableamount"));
						JSONArray monthlySalaryArray = getEmployeeMonthlySalaryRecords(employeeId, monthId, yearId, invoiceNumber);

						double nonPayableLeaves = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("nonpayableleaves"));

						double totalActualAmount = 0d;
						if (monthlySalaryArray != null && monthlySalaryArray.length() > 0) {
							// boolean isComponentTwoTimes = false;
							for (int counter1 = 0; counter1 < monthlySalaryArray.length(); counter1++) {
								Object[] employComponentArray = new Object[3];

								int salaryComponentId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid");
								int paymentTypeId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid.paymenttypeid");
								String monthlySalaryComponent = monthlySalaryArray.getJSONObject(counter1).optString("salarycomponentid.name");
								double receivedAmount = DataTypeUtilities.doubleValue(monthlySalaryArray.getJSONObject(counter1).opt("amount"));
								double actualAmount = DataTypeUtilities.doubleValue(monthlySalaryArray.getJSONObject(counter1).opt("actualamount"));

								if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
									totalActualAmount += actualAmount;
								}
								if (employComponentDetail.containsKey(salaryComponentId)) {
									employComponentArray = employComponentDetail.get(salaryComponentId); // if component already contain
									double receivedAmountInner = (Double) employComponentArray[1];
									double actualAmountInner = (Double) employComponentArray[2];
									employComponentArray[1] = receivedAmountInner + receivedAmount;
									employComponentArray[2] = actualAmountInner + actualAmount;
								} else {
									employComponentArray[0] = monthlySalaryComponent;
									employComponentArray[1] = receivedAmount;
									employComponentArray[2] = actualAmount;
								}

								employComponentDetail.put(salaryComponentId, employComponentArray);
							}
						}
						employeeDetail[0] = employeeName;
						employeeDetail[1] = counter + 1;
						employeeDetail[2] = employeeCode;
						employeeDetail[3] = employComponentDetail;
						employeeDetail[4] = accountNo;
						employeeDetail[5] = (int) (payableAmount + 0.5);
						employeeDetail[6] = (int) (totalActualAmount + 0.5);

						employeeDetail[7] = maxDayInMonth; // no of days in month
						employeeDetail[8] = presentDays;
						employeeDetail[9] = leaves;
						employeeDetail[10] = totalWorkingDays;
						employeeDetail[11] = totalOff;
						employeeDetail[12] = holidays;
						employeeDetail[13] = nonWorkingDays;
						employeeDetail[14] = (presentDays + (leaves - nonPayableLeaves) + nonWorkingDays);
						employeeDetail[15] = nonPayableLeaves;
						employeeDetail[16] = invoiceDate;
						employeeSalaryRecords.put(invoiceNumber, employeeDetail);
						List<String> invoiceList = new ArrayList<String>();
						if (employeeRecords.containsKey(employeeId)) {
							invoiceList = employeeRecords.get(employeeId);
						}
						invoiceList.add(invoiceNumber);
						employeeRecords.put(employeeId, invoiceList);
					}
				}
			}
			int row = 2;
			int column = 0;
			sheet.mergeCells(column + 4, row - 1, column + 10, row - 1);
			WritableFont font1 = new WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD);
			WritableCellFormat hdFormat1 = new WritableCellFormat(font1);
			setValueInSheet(sheet, font1, hdFormat1, row - 1, column + 4, "Employee Salary Sheet for " + monthName + "-" + yearName);
			sheet.setColumnView(column, 8);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Sr. No.");
			sheet.setColumnView(column, 25);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Employee Name");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Employee Code");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Account No.");

			for (Integer componentId : componentsMap.keySet()) {
				String componentName = componentsMap.get(componentId);
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row, column++, componentName);
			}
			sheet.setColumnView(column, 11);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Total");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row, column++, "No. of Days in Month");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Presents Days");
			sheet.setColumnView(column, 11);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Leaves");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Total Working Days");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Total Off");
			sheet.setColumnView(column, 11);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Holidays");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Non Working Days");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Non Payable Leaves");
			sheet.setColumnView(column, 12);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Salary Days");
			for (Integer componentId : componentsMap.keySet()) {
				String componentName = componentsMap.get(componentId);
				// received package component name write into sheet
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row, column++, componentName);
			}
			sheet.setColumnView(column, 25);
			setValueInSheet(sheet, font, hdFormat, row, column++, "Arrear Date");
			setValueInSheet(sheet, font, hdFormat, row, column++, "Net Payable Amount");
			for (Integer employeeId : employeeRecords.keySet()) {
				List<String> invliceList = employeeRecords.get(employeeId);
				for (int counter = 0; counter < invliceList.size(); counter++) {
					String invliceNumber = invliceList.get(counter);
					row++;
					column = 0;
					Object[] employeeDetail = employeeSalaryRecords.get(invliceNumber);
					putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[1], row, column++); // serial no.
					putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[0], row, column++); // name of employee
					putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[2], row, column++); // e.code
					putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[4], row, column++); // a/c no.
					@SuppressWarnings("unchecked")
					HashMap<Integer, Object[]> employComponentDetail = (HashMap<Integer, Object[]>) employeeDetail[3];
					for (Integer componentId : componentsMap.keySet()) {
						Object[] componentArray = employComponentDetail.get(componentId);
						if (componentArray != null) {
							putHeader(numbercellformat, sheet, DataTypeUtilities.doubleValue(componentArray[2]), row, column++); // write actual amount
						} else {
							putHeaderDefault(numbercellformat, sheet, "", row, column++); // write blank if component not assigned to employee
						}
					}
					putHeader(numbercellformat, sheet, (int) (Double.parseDouble("" + employeeDetail[6]) + 0.50), row, column++);
					putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[7]), row, column++);
					putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[8]), row, column++);
					putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[9]), row, column++);
					putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[10]), row, column++);
					putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[11]), row, column++);
					putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[12]), row, column++);
					putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[13]), row, column++);
					putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[15]), row, column++);
					putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[14]), row, column++);
					for (Integer componentId : componentsMap.keySet()) {
						Object[] componentArray = employComponentDetail.get(componentId);
						if (componentArray != null && ("" + componentArray[1]).length() > 0) {
							putHeader(numbercellformat, sheet, (int) (Double.parseDouble("" + componentArray[1]) + 0.50), row, column++); // write received amount
						} else {
							putHeaderDefault(numbercellformat, sheet, "", row, column++); // write blank if component not assigned to employee
						}
					}
					putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[16], row, column++);
					putHeader(numbercellformat, sheet, (int) Double.parseDouble("" + employeeDetail[5]), row, column++);
				}
			}
			workbook.write();

			workbook.close();
			// } catch (DeadlineExceededException e) {
			// LogUtility.writeLog("First exception block...");
			// throw e;
			// } catch (BusinessLogicException e) {
			// LogUtility.writeLog("Second exception block");
			// throw e;
		} catch (Exception e) {
			LogUtility.writeLog("Salary sheet Excel Report Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new BusinessLogicException(e.getMessage());
		}
	}

	public void setValueInSheet(WritableSheet sheet, WritableFont font, WritableCellFormat hdFormat, int rowNo, int cellNo, String value) throws WriteException, RowsExceededException {
		WritableCellFormat newFormat = new WritableCellFormat(hdFormat);
		Label label;
		newFormat.setFont(font);
		newFormat.setWrap(true);
		label = new Label(cellNo, rowNo, value, newFormat);
		sheet.addCell(label);
	}

	public void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		// Label label = new Label(column, row, value, cellFormat);
		// sheet.addCell(label);

		Number numobj = new Number(column, row, value, cellFormat);
		sheet.addCell(numobj);
	}

	public void putHeaderDefault(WritableCellFormat numbercellformat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, numbercellformat);
		sheet.addCell(label);
	}

	public JSONArray getSalaryComponents() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		columnArray.put("ignoreinexceptionreport");
		columnArray.put("compulsoryifdefined");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("salarycomponents");
		return array;
	}

	public JSONArray getEmployeeMonthlySalaryRecords(Object employeeId, Object monthId, Object yearId, String invoiceNumber) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents ");
		JSONArray columnArray = new JSONArray();
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid.paymenttypeid");
		columnArray.put("amount");
		columnArray.put("actualamount");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and salarymonthid = " + monthId + " and salaryyearid = " + yearId + " and invoicenumber = '" + invoiceNumber + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("employeemonthlysalarycomponents ");
		return array;
	}

	JSONArray getSalarySheetRecords(Object monthId, Object yearId, Object branchId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.panno");
		columnArray.put("employeeid.employeecode");
		columnArray.put("employeeid.accountno");
		columnArray.put("employeeid.bankname");
		columnArray.put("present");
		columnArray.put("leaves");
		columnArray.put("totalworkingdays");
		columnArray.put("totaloff");
		columnArray.put("holidays");
		columnArray.put("nonworkingdays");
		columnArray.put("attendancebasedamount");
		columnArray.put("nonpayableleaves");
		columnArray.put("performancebasedamount");
		columnArray.put("fixedamount");
		columnArray.put("deductionamount");
		columnArray.put("payableamount");
		columnArray.put("invoicenumber");
		columnArray.put("invoicedate");
		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "employeeid.name");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		JSONArray orderByExpression = new JSONArray();
		orderByExpression.put(order);
		query.put(Data.Query.ORDERS, orderByExpression);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "branchid= " + branchId + " and monthid = " + monthId + " and yearid = " + yearId + " and isarrear = 1");
		JSONObject object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("salarysheet");
		return array;
	}

	public JSONArray getTakeSalarySheetRecord(String key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takesalarysheets");
		JSONArray columnArray = new JSONArray();
		columnArray.put("branchid");
		columnArray.put("monthid");
		columnArray.put("yearid");
		columnArray.put("monthid.name");
		columnArray.put("yearid.name");
		columnArray.put("branchid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("takesalarysheets");
		return array;
	}

	public static String getMonthName(Object monthId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_months");
		JSONArray array = new JSONArray();
		array.put("name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + monthId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("organization_months");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			String monthName = rows.getJSONObject(0).optString("name");
			return monthName;
		}
		return null;
	}

	public static String getYearName(Object yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_years");
		JSONArray array = new JSONArray();
		array.put("name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + yearId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("organization_years");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			String yearName = rows.getJSONObject(0).optString("name");
			return yearName;
		}
		return null;
	}
}
