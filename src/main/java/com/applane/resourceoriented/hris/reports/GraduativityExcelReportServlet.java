package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;
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
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;

public class GraduativityExcelReportServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		graduativityReport(req, resp);
	}

	private void graduativityReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Graduativity_Report.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");

		try {
			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			WritableSheet activeEmployeeSheet = workbook.createSheet("Active Employee", 0);
			WritableSheet inActiveEmployeeSheet = workbook.createSheet("In-Active Employee", 1);

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

			JSONArray takeSalarySheetArray = new EmployeeSalarySheetReport().getTakeSalarySheetRecord(key);

			int takeSalarySheetArrayCount = (takeSalarySheetArray == null || takeSalarySheetArray.length() == 0) ? 0 : takeSalarySheetArray.length();
			if (takeSalarySheetArrayCount > 0) {

				Object branchId = takeSalarySheetArray.getJSONObject(0).opt(HRISApplicationConstants.TakeSalarySheet.BRANCH_ID);
				int monthId = Translator.integerValue(takeSalarySheetArray.getJSONObject(0).opt(HRISApplicationConstants.TakeSalarySheet.MONTH_ID));
				int yearId = Translator.integerValue(takeSalarySheetArray.getJSONObject(0).opt(HRISApplicationConstants.TakeSalarySheet.YEAR_ID));
				if (monthId != 0) {
					monthName = EmployeeReleasingSalaryServlet.getMonthName(monthId);
				}
				if (yearId != 0) {
					yearName = EmployeeReleasingSalaryServlet.getYearName(yearId);
				}
				Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
				Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
				String monthFirstDateString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
				String monthLastDateString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
				long maxDayInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
				String filter = "branchid = " + branchId;// + " and __key__=130";
				JSONArray employeeArray = gatEmployees(filter);

				// HashMap<Integer, Double> basicAmount = new HashMap<Integer, Double>();
				int row = 5;
				int inactiveRow = 5;
				int column = 0;
				setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Sr. No.");

				setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Employee Code");
				setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Employee Name");
				setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Joining Date");
				setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Relieving Date");
				setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Basic");
				column = 0;
				setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Sr. No.");
				setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Employee Code");
				setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Employee Name");
				setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Joining Date");
				setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Relieving Date");
				setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Basic");
				int srNo = 1;
				int inActiveSrNo = 1;

				for (int employeeCounter = 0; employeeCounter < employeeArray.length(); employeeCounter++) {
					Object employeeId = employeeArray.getJSONObject(employeeCounter).opt(Updates.KEY);
					Object employeeCode = employeeArray.getJSONObject(employeeCounter).opt(HrisKinds.Employess.EMPLOYEE_CODE);
					Object employeeName = employeeArray.getJSONObject(employeeCounter).opt(HrisKinds.Employess.NAME);
					Object relievingDateObject = employeeArray.getJSONObject(employeeCounter).opt("relievingdate");
					Object joiningDateObject = employeeArray.getJSONObject(employeeCounter).opt("joiningdate");
					int employeeStatusId = Translator.integerValue(employeeArray.getJSONObject(employeeCounter).opt("employeestatusid"));
					Date monthFirstDateTemp = monthFirstDate;
					Date monthLastDateTemp = monthLastDate;
					String monthFirstDateStringTemp = monthFirstDateString;
					String monthLastDateStringTemp = monthLastDateString;
					long maxDayInMonthTemp = maxDayInMonth;
					if (relievingDateObject != null) {
						Date relievingDate = Translator.dateValue(employeeArray.getJSONObject(employeeCounter).opt("relievingdate"));
						if (relievingDate.before(monthFirstDate)) {
							Calendar cal = Calendar.getInstance();
							cal.setTime(relievingDate);
							int monthIdTemp = cal.get(Calendar.MONTH) + 1;
							String monthNameTemp = EmployeeReleasingSalaryServlet.getMonthName(monthIdTemp);
							String yearNameTemp = "" + cal.get(Calendar.YEAR);
							monthFirstDateTemp = DataTypeUtilities.getMonthFirstDate(yearNameTemp, monthNameTemp);
							monthLastDateTemp = DataTypeUtilities.getMonthLastDate(monthFirstDateTemp);
							monthFirstDateStringTemp = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDateTemp);
							monthLastDateStringTemp = EmployeeSalaryGenerationServlet.getDateInString(monthLastDateTemp);
							maxDayInMonthTemp = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDateTemp);
						}
					}

					if (employeeId != null) {
						JSONArray basicComponents = getBasicComponents(employeeId, monthFirstDateStringTemp, monthLastDateStringTemp);
						if (basicComponents != null && basicComponents.length() > 0) {
							Calendar cl1 = Calendar.getInstance();
							Calendar cl2 = Calendar.getInstance();
							double calculatedAmount = 0.0;
							for (int basicComponentCounter = 0; basicComponentCounter < basicComponents.length(); basicComponentCounter++) {
								// String applicableFromDateString = Translator.stringValue(basicComponents.getJSONObject(basicComponentCounter).opt("applicablefrom"));
								// String applicableToDateString = Translator.stringValue(basicComponents.getJSONObject(basicComponentCounter).opt("applicableto"));
								Date applicableFromDate = Translator.dateValue(basicComponents.getJSONObject(basicComponentCounter).opt("applicablefrom"));
								Date applicableToDate = Translator.dateValue(basicComponents.getJSONObject(basicComponentCounter).opt("applicableto"));
								double amount = Translator.doubleValue(basicComponents.getJSONObject(basicComponentCounter).opt("amount"));
								if (applicableFromDate.after(monthFirstDateTemp)) {
									monthFirstDateTemp = applicableFromDate;
								}
								if (applicableToDate.before(monthLastDateTemp)) {
									monthLastDateTemp = applicableToDate;
								}
								cl1.setTime(monthFirstDateTemp);
								cl2.setTime(monthLastDateTemp);
								long milliseconds1 = cl1.getTimeInMillis();
								long milliseconds2 = cl2.getTimeInMillis();
								long diff = milliseconds2 - milliseconds1;
								double incrementDateDifference = 0.0;
								if (cl1.before(cl2)) {
									incrementDateDifference = (diff / (24 * 60 * 60 * 1000) + 1);
								} else if (cl1.equals(cl2)) {
									incrementDateDifference = 1;
								}
								calculatedAmount += ((float) (amount * incrementDateDifference) / maxDayInMonthTemp);
							}

							column = 0;
							if (employeeStatusId == HRISApplicationConstants.EMPLOYEE_ACTIVE) {
								row++;
								putHeaderDefault(hdFormat, activeEmployeeSheet, srNo, row, column++);
								putHeaderDefault(hdFormat, activeEmployeeSheet, employeeCode, row, column++);
								putHeaderDefault(hdFormat, activeEmployeeSheet, employeeName, row, column++);
								putHeaderDefault(hdFormat, activeEmployeeSheet, joiningDateObject == null ? "" : joiningDateObject, row, column++);
								putHeaderDefault(hdFormat, activeEmployeeSheet, relievingDateObject == null ? "" : relievingDateObject, row, column++);
								putHeader(numbercellformat, activeEmployeeSheet, calculatedAmount, row, column++);
								srNo++;
							} else {
								inactiveRow++;
								putHeaderDefault(hdFormat, inActiveEmployeeSheet, inActiveSrNo, inactiveRow, column++);
								putHeaderDefault(hdFormat, inActiveEmployeeSheet, employeeCode, inactiveRow, column++);
								putHeaderDefault(hdFormat, inActiveEmployeeSheet, employeeName, inactiveRow, column++);
								putHeaderDefault(hdFormat, inActiveEmployeeSheet, joiningDateObject == null ? "" : joiningDateObject, inactiveRow, column++);
								putHeaderDefault(hdFormat, inActiveEmployeeSheet, relievingDateObject == null ? "" : relievingDateObject, inactiveRow, column++);
								putHeader(numbercellformat, inActiveEmployeeSheet, calculatedAmount, inactiveRow, column++);
								inActiveSrNo++;
							}

						}
					}
				}
			}
			workbook.write();
			workbook.close();
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("GraduativityExcelReportServlet >> exception Report >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured Please Contact To Admin.");
		}
	}

	public void putHeaderDefault(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value);
		sheet.addCell(label);
	}

	public void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		// Label label = new Label(column, row, value, cellFormat);
		// sheet.addCell(label);

		Number numobj = new Number(column, row, value, cellFormat);
		sheet.addCell(numobj);
	}

	public void setValueInSheet(WritableSheet sheet, WritableFont font, WritableCellFormat hdFormat, int rowNo, int cellNo, String value) throws WriteException, RowsExceededException {
		WritableCellFormat newFormat = new WritableCellFormat(hdFormat);
		Label label;
		newFormat.setFont(font);
		newFormat.setWrap(true);
		label = new Label(cellNo, rowNo, value, newFormat);
		sheet.addCell(label);
	}

	private JSONArray getBasicComponents(Object employeeId, String monthFirstDateString, String monthLastDateString) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("employeeid");
		columnArray.put("salarycomponentid.name");
		columnArray.put("amount");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, "applicablefrom");
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		orders.put(orderObject);
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom <= '" + monthFirstDateString + "' and applicableto >= '" + monthFirstDateString + "' and salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID + "=" + HRISApplicationConstants.SalaryComponentTypes.BASIC);
		query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId + " and applicablefrom <= '" + monthLastDateString + "' and applicableto >= '" + monthLastDateString + "' and salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID + "=" + HRISApplicationConstants.SalaryComponentTypes.BASIC);

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
		String extraFilters = "";
		if (keys.length() > 0) {
			extraFilters += (" and __key__ not in (" + keys + ")");
		}
		query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("employeeid");
		columnArray.put("amount");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom >= '" + monthFirstDateString + "' and applicableto <= '" + monthLastDateString + "' and salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID + "=" + HRISApplicationConstants.SalaryComponentTypes.BASIC + extraFilters);
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows1 = object.getJSONArray("employeesalarycomponents");

		for (int counter = 0; counter < rows1.length(); counter++) {
			JSONObject object1 = rows1.getJSONObject(counter);
			rows.put(object1);
		}

		return rows;
	}

	private JSONArray gatEmployees(String filter) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put(HrisKinds.Employess.EMPLOYEE_CODE);
		columnArray.put(HrisKinds.Employess.NAME);
		columnArray.put("relievingdate");
		columnArray.put("joiningdate");
		columnArray.put("employeestatusid.name");
		columnArray.put("employeestatusid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, filter);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray array = object.getJSONArray(HrisKinds.EMPLOYEES);
		return array;
	}
}
