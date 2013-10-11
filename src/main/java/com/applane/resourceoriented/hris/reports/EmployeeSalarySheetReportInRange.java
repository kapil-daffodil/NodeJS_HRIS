package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

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

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;

public class EmployeeSalarySheetReportInRange extends HttpServlet {

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
		String decompositionParameter = "attachment;filename=Salary_Sheet.xls";
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
			// String monthName = null;
			//
			// String yearName = null;
			String key = req.getParameter("keys");
			if (key.length() > 2) {
				key = key.substring(1, key.length() - 1);
			}
			String[] keys = key.split(",");
			key = keys[0];

			JSONArray salaryComponentArray = getSalaryComponents();

			int salaryComponentArrayCount = (salaryComponentArray == null || salaryComponentArray.length() == 0) ? 0 : salaryComponentArray.length();
			LinkedHashMap<Integer, String> componentsMap = new LinkedHashMap<Integer, String>();
			LinkedHashMap<Integer, LinkedHashMap<String, Object[]>> employRecordsForFirstComponents = new LinkedHashMap<Integer, LinkedHashMap<String, Object[]>>();
			if (salaryComponentArrayCount > 0) {

				for (int counter = 0; counter < salaryComponentArrayCount; counter++) {
					// 16 salary components like HRA, basic
					String salaryComponent = salaryComponentArray.getJSONObject(counter).optString("name");
					int salaryComponentId = salaryComponentArray.getJSONObject(counter).optInt("__key__");
					componentsMap.put(salaryComponentId, salaryComponent);
				}
			}
			LogUtility.writeError("componentsMap >> " + componentsMap);
			JSONArray takeSalarySheetArray = getPFReportArray(key);

			int takeSalarySheetArrayCount = (takeSalarySheetArray == null || takeSalarySheetArray.length() == 0) ? 0 : takeSalarySheetArray.length();
			String organizationName = "";
			String organizationAddress = "";
			// String organizationLogo = "";
			Object branchName = "";
			if (takeSalarySheetArrayCount > 0) {

				int branchId = Translator.integerValue(takeSalarySheetArray.getJSONObject(0).opt("branchid"));
				Date fromDate = Translator.dateValue(takeSalarySheetArray.getJSONObject(0).opt("fromdate"));
				Date toDate = Translator.dateValue(takeSalarySheetArray.getJSONObject(0).opt("todate"));

				long months = DataTypeUtilities.monthsBetweenDates(fromDate, toDate);
				Calendar cal = Calendar.getInstance();
				String extraFilter = "";
				if (branchId != 0) {
					extraFilter = " AND branchid = " + branchId;
				}
				String filter = "";
				JSONArray optionFilter = new JSONArray();
				for (int counter = 0; counter < months; counter++) {
					cal.setTime(fromDate);
					cal.add(Calendar.MONTH, counter);
					// Date calendarDate = cal.getTime();
					int monthId = cal.get(Calendar.MONTH) + 1;
					long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
					if (filter.length() == 0) {
						filter = "monthid = " + monthId + " and yearid = " + yearId + extraFilter;// + " AND employeeid=151";
					} else {
						optionFilter.put("monthid = " + monthId + " and yearid = " + yearId + extraFilter);
					}

				}
				// LogUtility.writeError("optionFilter >> " + optionFilter + " << filter >> " + filter + " << months >> " + months);
				JSONArray salarySheetArray = getSalarySheetRecords(filter, optionFilter);
				JSONArray configurationSetup = getConfigurationSetup();
				if (configurationSetup != null && configurationSetup.length() > 0) {
					organizationName = configurationSetup.getJSONObject(0).optString("organizationname");
					organizationAddress = configurationSetup.getJSONObject(0).optString("address");
					// organizationLogo = getLogo(configurationSetup.getJSONObject(0).optString("organizationlogo"));
				}
				int salarySheetArrayCount = (salarySheetArray == null || salarySheetArray.length() == 0) ? 0 : salarySheetArray.length();
				if (salarySheetArrayCount > 0) {

					for (int counter = 0; counter < salarySheetArrayCount; counter++) {
						LinkedHashMap<String, Object[]> employeeMonthlySalaryDetails = new LinkedHashMap<String, Object[]>();
						HashMap<Integer, Object[]> employComponentFirstDetail = new HashMap<Integer, Object[]>();

						Object[] employeeDetailFirst = new Object[16];

						int employeeId = salarySheetArray.getJSONObject(counter).optInt("employeeid");

						String monthName = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("monthid.name"));
						String yearName = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("yearid.name"));
						boolean contain = false;
						if (employRecordsForFirstComponents.containsKey(employeeId)) {
							employeeMonthlySalaryDetails = employRecordsForFirstComponents.get(employeeId);
							if (employeeMonthlySalaryDetails.containsKey(monthName + " - " + yearName)) {
								contain = true;
								employeeMonthlySalaryDetails = new LinkedHashMap<String, Object[]>();
							}
						}
						if (!contain) {

							int monthId = salarySheetArray.getJSONObject(counter).optInt("monthid");
							int yearId = salarySheetArray.getJSONObject(counter).optInt("yearid");
							String employeeName = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.name"));
							String employeeCode = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.employeecode"));
							String accountNo = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.accountno"));
							String currentBranchName = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("sub_branchid.name"));

							String invoiceNumber = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("invoicenumber"));
							invoiceNumber = "";
							double presentDays = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("present"));
							double leaves = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("leaves"));
							// double totalOff = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("totaloff"));
							double nonWorkingDays = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("nonworkingdays"));
							double ewds = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("extraworkingday"));
							double nonPayableLeaves = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("nonpayableleaves"));

							Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
							long maxDayInMonth = 0;
							if (monthFirstDate != null) {
								maxDayInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
							}

							double presentDaysFirst = presentDays;
							double leavesFirst = leaves;
							double nonWorkingDaysFirst = nonWorkingDays;
							double nonPayableLeavesFirst = nonPayableLeaves;
							double ewdsFirst = ewds;
							JSONArray monthlySalaryArray = getEmployeeMonthlySalaryRecords(employeeId, monthId, yearId, invoiceNumber);
							double payableAmountFirst = 0.0;
							double totalActualAmountFirst = 0d;
							double totalGrossAmountFirst = 0d;
							JSONArray monthlyAttendance = getMonthlyAttendance(employeeId, yearId, monthId);
							LogUtility.writeError("monthlySalaryArray >> " + monthlySalaryArray);
							if (monthlySalaryArray != null && monthlySalaryArray.length() > 0 && monthlyAttendance != null && monthlyAttendance.length() > 0) {
								for (int counter1 = 0; counter1 < monthlySalaryArray.length(); counter1++) {
									Object[] employComponentArray = new Object[3];

									int salaryComponentId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid");
									int paymentTypeId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid.paymenttypeid");
									int payToId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid." + SalaryComponentKinds.AMOUNT_PAY_TO_ID);
									String monthlySalaryComponent = monthlySalaryArray.getJSONObject(counter1).optString("salarycomponentid.name");
									double receivedAmount = DataTypeUtilities.doubleValue(monthlySalaryArray.getJSONObject(counter1).opt("amount"));
									double actualAmount = DataTypeUtilities.doubleValue(monthlySalaryArray.getJSONObject(counter1).opt("actualamount"));

									employComponentArray[0] = monthlySalaryComponent;
									employComponentArray[1] = receivedAmount;
									employComponentArray[2] = actualAmount;
									if (employComponentFirstDetail.containsKey(salaryComponentId)) {
										employComponentArray = employComponentFirstDetail.get(salaryComponentId);
										employComponentArray[1] = Translator.doubleValue(employComponentArray[1]) + receivedAmount;
										employComponentArray[2] = Translator.doubleValue(employComponentArray[2]) + actualAmount;
										// employComponentArray[1] = receivedAmount;
										// employComponentArray[2] = actualAmount;
									}
									LogUtility.writeError("monthlySalaryComponent >> " + monthlySalaryComponent + " << receivedAmount" + receivedAmount + " << actualAmount >> " + actualAmount);
									if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
										totalActualAmountFirst += actualAmount;
										totalGrossAmountFirst += receivedAmount;
										if (payToId != HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
											payableAmountFirst += receivedAmount;
										}
									}
									if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE) {
										payableAmountFirst -= receivedAmount;
									}
									employComponentFirstDetail.put(salaryComponentId, employComponentArray);
								}
							}
							LogUtility.writeError("employComponentFirstDetail >> " + employComponentFirstDetail);
							employeeDetailFirst[0] = employeeName;
							employeeDetailFirst[1] = counter + 1;
							employeeDetailFirst[2] = employeeCode;
							employeeDetailFirst[3] = employComponentFirstDetail;
							employeeDetailFirst[4] = accountNo;
							employeeDetailFirst[5] = (int) (payableAmountFirst + 0.5);
							employeeDetailFirst[6] = (int) (totalActualAmountFirst + 0.5);

							employeeDetailFirst[7] = maxDayInMonth; // no of days in month
							employeeDetailFirst[8] = presentDaysFirst;
							employeeDetailFirst[9] = leavesFirst;
							employeeDetailFirst[10] = nonWorkingDaysFirst;
							employeeDetailFirst[11] = ewdsFirst;
							employeeDetailFirst[12] = (presentDaysFirst + (leavesFirst - nonPayableLeavesFirst) + nonWorkingDaysFirst + ewdsFirst);
							employeeDetailFirst[13] = nonPayableLeavesFirst;
							employeeDetailFirst[14] = (int) (totalGrossAmountFirst + 0.5);
							employeeDetailFirst[15] = currentBranchName;
							if (employRecordsForFirstComponents.containsKey(employeeId)) {
								employeeMonthlySalaryDetails = employRecordsForFirstComponents.get(employeeId);
							}
							employeeMonthlySalaryDetails.put(monthName + " - " + yearName, employeeDetailFirst);
							employRecordsForFirstComponents.put(employeeId, employeeMonthlySalaryDetails);
						}
					}
				}
			}
			// LogUtility.writeLog("employRecordsForFirstComponents >> " + employRecordsForFirstComponents + " << employRecordsForSecondComponents >> " + employRecordsForSecondComponents);
			int[] row = { 9 };
			int column = 0;
			WritableFont font1 = new WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD);
			WritableCellFormat hdFormat1 = new WritableCellFormat(font1);

			hdFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
			hdFormat1.setAlignment(Alignment.CENTRE);

			sheet.mergeCells(column + 4, 4, column + 10, 6);
			setValueInSheet(sheet, font1, hdFormat1, 4, column + 4, organizationName);

			sheet.mergeCells(column + 4, 7, column + 10, 7);
			setValueInSheet(sheet, font, hdFormat, 7, column + 4, organizationAddress);

			sheet.mergeCells(column + 4, row[0] - 1, column + 10, row[0] - 1);
			setValueInSheet(sheet, font1, hdFormat1, row[0] - 1, column + 4, "Employee Salary Sheet");
			sheet.setColumnView(column, 8);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Sr. No.");
			sheet.setColumnView(column, 25);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Employee Name");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Employee Code");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Month - Year");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Branch Name");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Current Location");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Account No.");

			for (Integer componentId : componentsMap.keySet()) {
				String componentName = componentsMap.get(componentId);
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row[0], column++, componentName);
			}
			sheet.setColumnView(column, 11);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Total");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "No. of Days in Month");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Presents Days");
			sheet.setColumnView(column, 11);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Leaves");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Non Working Days");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Non Payable Leaves");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "EWD(s)");
			sheet.setColumnView(column, 12);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Salary Days");
			for (Integer componentId : componentsMap.keySet()) {
				String componentName = componentsMap.get(componentId);
				// received package component name write into sheet
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row[0], column++, componentName);
			}
			sheet.setColumnView(column, 25);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Gross Amount");
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Net Payable Amount");
			for (Integer employeeId : employRecordsForFirstComponents.keySet()) {
				putAllValuesIntoSheet(sheet, numbercellformat, componentsMap, employRecordsForFirstComponents, row, employeeId, branchName);
			}
			workbook.write();

			workbook.close();
		} catch (Exception e) {
			LogUtility.writeLog("Salary sheet Excel Report Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new BusinessLogicException(e.getMessage());
		}
	}

	private JSONArray getMonthlyAttendance(int employeeId, Object yearId, Object monthId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		columnArray.put("nonpayableleaves");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND monthid = " + monthId + " AND yearid = " + yearId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeemonthlyattendance");
	}

	private JSONArray getConfigurationSetup() throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		columnArray.put("organizationname");
		columnArray.put("organizationlogo");
		columnArray.put("address");
		query.put(Data.Query.COLUMNS, columnArray);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_mailconfigurations");
	}

	private void putAllValuesIntoSheet(WritableSheet sheet, WritableCellFormat numbercellformat, LinkedHashMap<Integer, String> componentsMap, LinkedHashMap<Integer, LinkedHashMap<String, Object[]>> employRecordsForFirstComponents, int[] row, Integer employeeId, Object branchName) throws RowsExceededException, WriteException, NumberFormatException {
		LinkedHashMap<String, Object[]> a = employRecordsForFirstComponents.get(employeeId);
		for (String monthYear : a.keySet()) {
			int column = 0;
			row[0]++;
			Object[] employeeDetail = a.get(monthYear);

			putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[1], row[0], column++); // serial no.
			putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[0], row[0], column++); // name of employee
			putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[2], row[0], column++); // e.code

			putHeaderDefault(numbercellformat, sheet, "" + monthYear, row[0], column++); // e.code

			putHeaderDefault(numbercellformat, sheet, "" + branchName, row[0], column++); // branch name
			putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[15], row[0], column++); // current branch name
			putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[4], row[0], column++); // a/c no.
			@SuppressWarnings("unchecked")
			HashMap<Integer, Object[]> employComponentDetail = (HashMap<Integer, Object[]>) employeeDetail[3];
			LogUtility.writeError("employComponentDetail << " + employComponentDetail);
			for (Integer componentId : componentsMap.keySet()) {
				Object[] componentArray = employComponentDetail.get(componentId);
				if (componentArray != null) {
					LogUtility.writeError("componentArray[2] > [" + componentArray[2] + "]");
					putHeader(numbercellformat, sheet, DataTypeUtilities.doubleValue(componentArray[2]), row[0], column++); // write actual amount
				} else {
					LogUtility.writeError("componentArray[2] > null");

					putHeaderDefault(numbercellformat, sheet, "", row[0], column++); // write blank if component not assigned to employee
				}
			}
			putHeader(numbercellformat, sheet, (int) (Double.parseDouble("" + employeeDetail[6]) + 0.50), row[0], column++);
			putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[7]), row[0], column++);
			putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[8]), row[0], column++);
			putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[9]), row[0], column++);
			putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[10]), row[0], column++);
			putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[13]), row[0], column++);
			putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[11]), row[0], column++);
			putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[12]), row[0], column++);
			// putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[15]), row[0], column++);
			// putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[14]), row[0], column++);
			for (Integer componentId : componentsMap.keySet()) {
				Object[] componentArray = employComponentDetail.get(componentId);
				if (componentArray != null && ("" + componentArray[1]).length() > 0) {
					LogUtility.writeError("componentArray[1] > [" + componentArray[1] + "]");
					putHeader(numbercellformat, sheet, (int) (Double.parseDouble("" + componentArray[1]) + 0.50), row[0], column++); // write received amount
				} else {
					LogUtility.writeError("componentArray[1] > null");
					putHeaderDefault(numbercellformat, sheet, "", row[0], column++); // write blank if component not assigned to employee
				}
			}
			putHeader(numbercellformat, sheet, (int) Double.parseDouble("" + employeeDetail[14]), row[0], column++);
			putHeader(numbercellformat, sheet, (int) Double.parseDouble("" + employeeDetail[5]), row[0], column++);
		}
	}

	// private JSONArray getAttendanceFromComponentMonthlyAmount(int employeeId, int escId, int monthIdFromComponents, int yearIdFromComponents, int isInFirstComponents) throws JSONException {
	// JSONObject query = new JSONObject();
	// JSONArray columnArray = new JSONArray();
	// query.put(Data.Query.RESOURCE, "componentmonthlyamount");
	// columnArray.put("present");
	// columnArray.put("absent");
	// columnArray.put("actualnonworkingdays");
	// columnArray.put("ewd");
	// columnArray.put("leaves");
	// columnArray.put("nonpayableleaves");
	// columnArray.put("isinfirstcomponents");
	// query.put(Data.Query.COLUMNS, columnArray);
	// query.put(Data.Query.FILTERS, "employeeid=" + employeeId + " and employeesalarycomponentid=" + escId + " and monthid = " + monthIdFromComponents + " and yearid=" + yearIdFromComponents);
	// return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("componentmonthlyamount");
	// }

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
		String filters = "";
		if (invoiceNumber.length() > 0) {
			filters += " and invoicenumber = '" + invoiceNumber + "'";
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents ");
		JSONArray columnArray = new JSONArray();
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid.paymenttypeid");
		columnArray.put("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID);
		columnArray.put("salarycomponentid." + SalaryComponentKinds.AMOUNT_PAY_TO_ID);
		columnArray.put("amount");
		columnArray.put("actualamount");
		columnArray.put("actualamount");
		columnArray.put("salarymonthid");
		columnArray.put("salaryyearid");
		columnArray.put("employeesalarycomponentid");
		columnArray.put("employeesalarycomponentid.applicablefrom");
		columnArray.put("employeesalarycomponentid.applicableto");
		columnArray.put("isinfirstcomponents");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "invoicenumber != null AND employeeid = " + employeeId + " and salarymonthid = " + monthId + " and salaryyearid = " + yearId + filters);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("employeemonthlysalarycomponents ");
		return array;
	}

	JSONArray getSalarySheetRecords(String filter, JSONArray optionFilter) throws JSONException {
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
		columnArray.put("monthid");
		columnArray.put("yearid");
		columnArray.put("monthid.name");
		columnArray.put("yearid.name");
		columnArray.put("totalworkingdays");
		columnArray.put("totaloff");
		columnArray.put("extraworkingday");
		columnArray.put("holidays");
		columnArray.put("nonworkingdays");
		columnArray.put("attendancebasedamount");
		columnArray.put("nonpayableleaves");
		columnArray.put("performancebasedamount");
		columnArray.put("fixedamount");
		columnArray.put("deductionamount");
		columnArray.put("payableamount");
		columnArray.put("invoicenumber");
		columnArray.put("sub_branchid.name");
		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "employeeid.name");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		JSONArray orderByExpression = new JSONArray();
		orderByExpression.put(order);
		query.put(Data.Query.ORDERS, orderByExpression);

		query.put(Data.Query.COLUMNS, columnArray);
		// query.put(Data.Query.FILTERS, "branchid= " + branchId + " and monthid = " + monthId + " and yearid = " + yearId + " and employeeid=124");
		query.put(Data.Query.FILTERS, filter);
		query.put(Data.Query.OPTION_FILTERS, optionFilter);
		JSONObject object;
		object = ResourceEngine.query(query);
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
}
