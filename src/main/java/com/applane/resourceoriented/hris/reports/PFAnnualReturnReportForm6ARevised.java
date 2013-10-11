package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class PFAnnualReturnReportForm6ARevised extends HttpServlet {
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
			String decompositionParameter = "attachment;filename=PF_6a_Form.xls";
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
				// if (year != 0)
				{
					Date TODAY_DATE = Translator.dateValue(new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime()));

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
					Object basicSalaryComponentId = PunchingUtility.getId("salarycomponents", "salarycomponenttypeid", "" + HRISApplicationConstants.SalaryComponentTypes.BASIC);
					Object pfSalaryComponentId = PunchingUtility.getId("salarycomponents", "salarycomponenttypeid", "" + HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYEE);
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
					Calendar forBaicAmount = Calendar.getInstance();

					addSomeInformationToSheet(cellFormat1, sheet, firstDateInString, lastDateInString);
					int row = 5;
					int column = 0;
					row++;
					sheet.mergeCells(column, row, column + 4, row);
					putHeader(cellFormat1, sheet, "Name & Address of the Establishment :-" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION), row, column);

					row++;
					sheet.mergeCells(column, row, column + 7, row);
					putHeader(cellFormat1, sheet, "Statutory rate of Contribution: 12%", row, column);

					row++;
					sheet.mergeCells(column, row, column + 4, row);
					putHeader(cellFormat1, sheet, "Code No. of the establishment :-", row, column);

					row++;
					HrisHelper.setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Account No.", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Name of the member \n (in block letters)", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Wages, relating \n allowance ( if nay) and \n DA including cash value \n of food concession", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Amount of Workers \n contribution deducted \n from the wages (EPF)", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Employers EPF \n diffrence between \n 12 & 8.33% (a)", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Contribution \n EPS (8.33%)", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Refund of \n advance", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Rate of \n higher \n voluntary \n (if nay)", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Remarks", cellFormat, sheet);
					row++;
					column = 0;
					HrisHelper.setHeaderValue(column++, row, "1", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "2", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "3", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "4", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "5", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "6", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "7", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "8", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "9", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "10", cellFormat, sheet);

					double grandTotalBasic = 0.0;
					double grandTotalPercent12 = 0.0;
					double grandTotalPercent3_67 = 0.0;
					double grandTotalPercent8_33 = 0.0;
					long dateDiff = DataTypeUtilities.monthsBetweenDates(fromDate, toDate);
					for (int counter = 0; counter < employeeArray.length(); counter++) {

						boolean isEmployeeHavePF = false;
						Object employeeId = employeeArray.getJSONObject(counter).opt("__key__");
						Object employeeName = Translator.stringValue(employeeArray.getJSONObject(counter).opt("name"));
						// Object employeeCode =
						// Translator.stringValue(employeeArray.getJSONObject(counter).opt("employeecode"));
						// Object fatherName =
						// Translator.stringValue(employeeArray.getJSONObject(counter).opt("fathername"));
						String accountNo = Translator.stringValue(employeeArray.getJSONObject(counter).opt("accountno"));

						double totalBasic = 0.0;
						double totalPercent12 = 0.0;
						double totalPercent3_67 = 0.0;
						double totalPercent8_33 = 0.0;

						for (int basicMonthCounter = 0; basicMonthCounter < dateDiff; basicMonthCounter++) {
							forBaicAmount.setTime(fromDate);
							forBaicAmount.add(Calendar.MONTH, basicMonthCounter);
							int monthId = forBaicAmount.get(Calendar.MONTH) + 1;
							long yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(forBaicAmount.get(Calendar.YEAR)));
							double paidBasicAmount = getPaidBasicAmount(employeeId, monthId, yearId, basicSalaryComponentId, pfSalaryComponentId);
							if (!isEmployeeHavePF && paidBasicAmount != -1.0) {
								isEmployeeHavePF = true;
							}
							if (paidBasicAmount != -1.0) {
								if (paidBasicAmount > 6500.0) {
									paidBasicAmount = 6500.0;
								}
								double percent12 = (float) ((paidBasicAmount * 12) / 100);
								double percent3_67 = (float) ((paidBasicAmount * 3.67) / 100);
								double percent8_33 = (float) ((paidBasicAmount * 8.33) / 100);
								totalBasic += paidBasicAmount;
								totalPercent12 += percent12;
								totalPercent3_67 += percent3_67;
								totalPercent8_33 += percent8_33;
							}
						}
						if (isEmployeeHavePF) {
							row++;
							column = 0;
							grandTotalBasic += totalBasic;
							grandTotalPercent12 += totalPercent12;
							grandTotalPercent3_67 += totalPercent3_67;
							grandTotalPercent8_33 += totalPercent8_33;
							putHeader(cellFormat1, sheet, counter + 1, row, column++);
							putHeader(cellFormat1, sheet, accountNo, row, column++);
							putHeader(cellFormat1, sheet, employeeName, row, column++);
							putHeader(cellFormat1, sheet, (int) (totalBasic + 0.50), row, column++);
							putHeader(cellFormat1, sheet, (int) (totalPercent12 + 0.50), row, column++);
							putHeader(cellFormat1, sheet, (int) (totalPercent3_67 + 0.50), row, column++);
							putHeader(cellFormat1, sheet, (int) (totalPercent8_33 + 0.50), row, column++);
						}
					}
					row++;
					column = 1;
					putHeader(cellFormat1, sheet, "Grand Total", row, column++);
					column++;
					putHeader(cellFormat1, sheet, (int) (grandTotalBasic + 0.50), row, column++);
					putHeader(cellFormat1, sheet, (int) (grandTotalPercent12 + 0.50), row, column++);
					putHeader(cellFormat1, sheet, (int) (grandTotalPercent3_67 + 0.50), row, column++);
					putHeader(cellFormat1, sheet, (int) (grandTotalPercent8_33 + 0.50), row, column++);

					workbook.write();
					workbook.close();
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("PFAnnualReturnReportForm6ARevised >> calculateAndWriteToSheet Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "PFAnnualReturnReportForm3ARevised >> calculateAndWriteToSheet Exception Trace");
		}
	}

	private void addSomeInformationToSheet(WritableCellFormat cellFormat1, WritableSheet sheet, String firstDateInString, String lastDateInString) throws RowsExceededException, WriteException {
		int row = 0;
		int column = 0;
		sheet.mergeCells(column + 2, row, column + 6, row);
		putHeader(cellFormat1, sheet, "For unexempted establishments only", row, column + 2);
		row++;
		sheet.mergeCells(column + 3, row, column + 7, row);
		putHeader(cellFormat1, sheet, "Form 6A (Revised)", row, column + 3);
		row++;
		sheet.mergeCells(column + 2, row, column + 6, row);
		putHeader(cellFormat1, sheet, "THE EMPLOYEES PENSION SCHEME - 1995 (PARAGRAPH 20  (4)", row, column + 2);
		row++;
		sheet.mergeCells(column, row, column + 6, row);
		putHeader(cellFormat1, sheet, "Annual statement of contribution for the currency period  " + firstDateInString + " to " + lastDateInString, row, column);
	}

	private static void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, cellFormat);
		sheet.addCell(label);
	}

	private double getPaidBasicAmount(Object employeeId, int monthId, long yearId, Object basicSalaryComponentId, Object pfSalaryComponentId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("amount");
		columnArray.put("salarycomponentid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and salarymonthid = " + monthId + "  and salaryyearid = " + yearId + " and salarycomponentid = " + basicSalaryComponentId);
		query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId + " and salarymonthid = " + monthId + "  and salaryyearid = " + yearId + " and salarycomponentid = " + pfSalaryComponentId);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray amountArray = employeeObject.getJSONArray("employeemonthlysalarycomponents");
		double amount = -1.0;
		if (amountArray != null) {
			boolean pf = false;
			for (int counter = 0; counter < amountArray.length(); counter++) {
				Object pfId = amountArray.getJSONObject(counter).opt("salarycomponentid");
				if (("" + pfSalaryComponentId).equals("" + pfId)) {
					pf = true;
					break;
				}
			}
			if (pf) {
				for (int counter = 0; counter < amountArray.length(); counter++) {
					Object basicId = amountArray.getJSONObject(counter).opt("salarycomponentid");
					if (("" + basicSalaryComponentId).equals("" + basicId)) {
						amount += Translator.doubleValue(amountArray.getJSONObject(counter).opt("amount"));
					}
				}
			}
		}
		return amount;
	}

	public JSONArray getPFReportArray(String key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "pfreportforyear");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("yearid.name");
		columnArray.put("branchid");
		columnArray.put("branchid.name");
		columnArray.put("fromdate");
		columnArray.put("todate");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray pfReportArray = employeeObject.getJSONArray("pfreportforyear");
		return pfReportArray;
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
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + extraFilter + " and joiningdate <= '" + lastDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + extraFilter + " and relievingdate >= '" + firstDateInString + "'");
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}
}
