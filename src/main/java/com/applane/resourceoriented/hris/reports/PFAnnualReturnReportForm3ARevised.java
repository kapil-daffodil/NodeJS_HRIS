package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
import java.util.Calendar;

public class PFAnnualReturnReportForm3ARevised extends HttpServlet {
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
			String decompositionParameter = "attachment;filename=PF_3a_Form.xls";
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
				// if (year != 0 || (fromDate != null & toDate != null))
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
					Object basicSalaryComponentId = PunchingUtility.getId("salarycomponents", "salarycomponenttypeid", "" + HRISApplicationConstants.SalaryComponentTypes.BASIC);
					Object pfSalaryComponentId = PunchingUtility.getId("salarycomponents", "salarycomponenttypeid", "" + HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYEE);
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

					long dateDiff = DataTypeUtilities.monthsBetweenDates(fromDate, toDate);
					JSONArray employeeArray = getEmployeeArray(firstDateInString, branchId);
					Calendar forBaicAmount = Calendar.getInstance();
					for (int counter = 0; counter < employeeArray.length(); counter++) {
						int row = 5;
						int column = 0;
						Object employeeId = employeeArray.getJSONObject(counter).opt("__key__");
						Object employeeName = employeeArray.getJSONObject(counter).opt("name");
						Object employeeCode = employeeArray.getJSONObject(counter).opt("employeecode");
						Object fatherName = employeeArray.getJSONObject(counter).opt("fathername");
						Object accountNo = employeeArray.getJSONObject(counter).opt("accountno");
						sheet = workbook.createSheet("" + employeeName, counter);
						addSomeInformationToSheet(cellFormat1, sheet, employeeName, employeeCode, fatherName, firstDateInString, lastDateInString);

						sheet.mergeCells(column, row, column + 1, row);
						putHeader(cellFormat1, sheet, "A/C No.: " + accountNo, row, column);
						putHeader(cellFormat1, sheet, "" + employeeCode, row, column + 3);
						sheet.mergeCells(column + 5, row, column + 6, row);
						putHeader(cellFormat1, sheet, "Name / Surname: " + employeeName, row, column + 5);
						sheet.mergeCells(column + 8, row, column + 10, row);
						putHeader(cellFormat1, sheet, "Father's / Husband's Name: " + fatherName, row, column + 8);
						row++;
						sheet.mergeCells(column, row, column + 4, row);
						putHeader(cellFormat1, sheet, "Name & Address for the Factory/ Establishment: " + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION), row, column);

						sheet.mergeCells(column + 5, row, column + 7, row);
						putHeader(cellFormat1, sheet, "Statutory rate of Contribution: ", row, column + 5);

						putHeader(cellFormat1, sheet, "12%", row, column + 8);
						sheet.mergeCells(column + 9, row, column + 11, row);
						putHeader(cellFormat1, sheet, "Voluntary higher rate of employee's Contirbution if any", row, column + 9);

						row++;
						sheet.mergeCells(column, row, column, row + 1);
						putHeader(cellFormat1, sheet, "Month", row, column++);

						sheet.mergeCells(column, row, column + 1, row);
						putHeader(cellFormat1, sheet, "Worker's Share", row, column);

						column += 2;
						sheet.mergeCells(column, row, column + 1, row);
						putHeader(cellFormat1, sheet, "Employeer's Share wef 16/11/95", row, column);

						row++;
						column = 1;
						HrisHelper.setHeaderValue(column++, row, "Amount of Wages", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "EPF", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "EPF diff. between 10% &8.33% (If any)", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "Pension Fund Contribution 8.33%", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "Refund of Advance", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "No.of days/period of non-contributing service (if nay)", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "Remarks", cellFormat, sheet);

						row++;
						column = 0;
						HrisHelper.setHeaderValue(column++, row, "1", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "2", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "3", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "4a", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "4b", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "5", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "6", cellFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "7", cellFormat, sheet);

						double totalBasic = 0.0;
						double totalPercent12 = 0.0;
						double totalPercent3_67 = 0.0;
						double totalPercent8_33 = 0.0;

						for (int basicMonthCounter = 0; basicMonthCounter < dateDiff; basicMonthCounter++) {
							forBaicAmount.setTime(fromDate);
							forBaicAmount.add(Calendar.MONTH, basicMonthCounter);
							int monthId = forBaicAmount.get(Calendar.MONTH) + 1;
							long yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(forBaicAmount.get(Calendar.YEAR)));
							String monthName = EmployeeSalaryGenerationServlet.getMonthName(monthId);
							double paidBasicAmount = getPaidBasicAmount(employeeId, monthId, yearId, basicSalaryComponentId, pfSalaryComponentId);
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

							row++;
							column = 0;
							putHeader(cellFormat1, sheet, monthName, row, column++);
							putHeader(cellFormat1, sheet, (int) (paidBasicAmount + 0.50), row, column++);
							putHeader(cellFormat1, sheet, (int) (percent12 + 0.50), row, column++);
							putHeader(cellFormat1, sheet, (int) (percent3_67 + 0.50), row, column++);
							putHeader(cellFormat1, sheet, (int) (percent8_33 + 0.50), row, column++);

						}
						row++;
						column = 0;
						putHeader(cellFormat1, sheet, "Total", row, column++);
						putHeader(cellFormat1, sheet, (int) (totalBasic + 0.50), row, column++);
						putHeader(cellFormat1, sheet, (int) (totalPercent12 + 0.50), row, column++);
						putHeader(cellFormat1, sheet, (int) (totalPercent3_67 + 0.50), row, column++);
						putHeader(cellFormat1, sheet, (int) (totalPercent8_33 + 0.50), row, column++);

						row += 2;
						column = 0;
						sheet.mergeCells(column, row, column + 10, row);
						putHeader(cellFormat1, sheet, "Certified that the total amount of contributions (both shares) indicated in this card i.e. Rs. " + (int) (totalPercent12 + totalPercent3_67 + 0.50) + "/- has already been remitted in full in", row, column);

						row++;
						column = 0;
						sheet.mergeCells(column, row, column + 10, row);
						putHeader(cellFormat1, sheet, "EPF A/c No. 1 and Pension Fund A/c No.Rs." + totalPercent8_33 + "/- (Vide note below).", row, column);

						row += 2;
						column = 0;
						sheet.mergeCells(column, row, column + 10, row);
						putHeader(cellFormat1, sheet, "Certified that the difference between the total of the contribution shown under Cols. 3 and 4a & 4b of the above table and that arrived at on the ", row, column);

						row++;
						column = 0;
						sheet.mergeCells(column, row, column + 10, row);
						putHeader(cellFormat1, sheet, "total wages shown in column 2 at the perscribed rate in solely due to rounding off of contributions to the nearest rupee under the rule.", row, column);

						row += 3;
						column = 0;
						putHeader(cellFormat1, sheet, "Dated", row, column++);
						putHeader(cellFormat1, sheet, "......................", row, column++);
						putHeader(cellFormat1, sheet, "Sibnature Of Employees", row, column + 2);
						putHeader(cellFormat1, sheet, "with office seal", row, column + 4);

						row++;
						column = 0;
						sheet.mergeCells(column, row, column + 10, row);
						putHeader(cellFormat1, sheet, "1.  In respect of the from 3a sent to the Regional  office during the course of the currency period for the purpose of final settlement of the ", row, column);

						row++;
						column = 0;
						sheet.mergeCells(column, row, column + 10, row);
						putHeader(cellFormat1, sheet, "accounts of the member who has left seervice, details of date & reasons for leaving service should be furnished under col.7(a) & (b).", row, column);

						row += 2;
						column = 0;
						sheet.mergeCells(column, row, column + 10, row);
						putHeader(cellFormat1, sheet, "2.  In respect of those who are not members of the Pension fund the employers share of contribution to the EPF will be 8.33% or10% as the ", row, column);

						row++;
						column = 0;
						sheet.mergeCells(column, row, column + 10, row);
						putHeader(cellFormat1, sheet, "case may be is to be shown under column - 4(a).", row, column);
					}
					workbook.write();
					workbook.close();
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("PFAnnualReturnReportForm3ARevised >> calculateAndWriteToSheet Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "PFAnnualReturnReportForm3ARevised >> calculateAndWriteToSheet Exception Trace");
		}
	}

	private void addSomeInformationToSheet(WritableCellFormat cellFormat1, WritableSheet sheet, Object employeeName, Object employeeCode, Object fatherName, String firstDateInString, String lastDateInString) throws RowsExceededException, WriteException {
		int row = 0;
		int column = 0;
		sheet.mergeCells(column, row, column + 4, row);
		putHeader(cellFormat1, sheet, "For unexempted establishments only", row, column);
		sheet.mergeCells(column + 5, row, column + 8, row);
		putHeader(cellFormat1, sheet, "Form 3A (Revised)", row, column + 5);
		row++;
		sheet.mergeCells(column, row, column + 6, row);
		putHeader(cellFormat1, sheet, "The Employee's Provident funds Scheme 1952 ( Paras 35 & 42)", row, column);
		row++;
		sheet.mergeCells(column, row, column + 6, row);
		putHeader(cellFormat1, sheet, "and The Employee's Pension Scheme, 1995 (Para 19)", row, column);
		row++;
		sheet.mergeCells(column + 2, row, column + 8, row);
		putHeader(cellFormat1, sheet, "Contribution Card for Currency Period From " + firstDateInString + " to " + lastDateInString, row, column + 2);
	}

	private static void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, cellFormat);
		sheet.setColumnView(column, 20);
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
		double amount = 0.0;
		if (amountArray != null) {
			boolean pf = false;
			for (int counter = 0; counter < amountArray.length(); counter++) {
				int pfId = Translator.integerValue(amountArray.getJSONObject(counter).opt("salarycomponentid"));
				if (("" + pfSalaryComponentId).equals("" + pfId)) {
					pf = true;
					break;
				}
			}
			if (pf) {
				for (int counter = 0; counter < amountArray.length(); counter++) {
					int basicId = Translator.integerValue(amountArray.getJSONObject(counter).opt("salarycomponentid"));
					if (("" + basicSalaryComponentId).equals("" + basicId)) {
						amount += Translator.doubleValue(amountArray.getJSONObject(counter).opt("amount"));
					}
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

	public static JSONArray getEmployeeArray(String firstDateInString, int branchId) throws JSONException {
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
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + extraFilter);
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + extraFilter + " and relievingdate >= '" + firstDateInString + "'");
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}
}
