package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
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

public class MonthlyPayrollExcelReports extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=R2T_Monthly_Payroll.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");
		try {
			String key = req.getParameter("keys");
			if (key.length() > 2) {
				key = key.substring(1, key.length() - 1);

				WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());

				putR2TPayrollIntoSheet(key, workbook);

				workbook.write();
				workbook.close();

			}
		} catch (BusinessLogicException e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeePerformanceReportInExcel.class.getName(), e);
			LogUtility.writeLog("EmployeePerformanceReportInExcel >> trace >> " + trace);
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeePerformanceReportInExcel.class.getName(), e);
			LogUtility.writeLog("EmployeePerformanceReportInExcel >> trace >> " + trace);
		}
	}

	private void putR2TPayrollIntoSheet(String key, WritableWorkbook workbook) throws Exception {
		WritableSheet sheet = null;
		JSONArray reportParametersArray = getReportParametersFromPfReports(key);
		if (reportParametersArray != null && reportParametersArray.length() > 0) {
			Object branchId = reportParametersArray.getJSONObject(0).opt("branchid");
			Object monthId = reportParametersArray.getJSONObject(0).opt("monthid");
			Object yearId = reportParametersArray.getJSONObject(0).opt("yearid");
			String currentYearName = Translator.stringValue(reportParametersArray.getJSONObject(0).opt("yearid.name"));
			String currentMonthName = Translator.stringValue(reportParametersArray.getJSONObject(0).opt("monthid.name"));
			if (yearId != null && monthId != null) {
				Date currentMonthFirstDate = DataTypeUtilities.getMonthFirstDate(currentYearName, currentMonthName);
				Date currentMonthLastDate = DataTypeUtilities.getMonthLastDate(currentMonthFirstDate);
				String currentMonthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(currentMonthFirstDate);
				String currentMonthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(currentMonthLastDate);

				JSONArray employeeArray = getEmployeeArray(branchId, currentMonthFirstDateInString, currentMonthLastDateInString);
				if (employeeArray != null && employeeArray.length() > 0) {
					String employeeIdsForMonthlyAttendance = "";
					HashMap<Integer, Double> employeePayDays = new HashMap<Integer, Double>();
					HashMap<Integer, Object[]> employeeDetails = new HashMap<Integer, Object[]>();
					for (int counter = 0; counter < employeeArray.length(); counter++) {
						Object employeeId = employeeArray.getJSONObject(counter).opt(Updates.KEY);
						Object employeeName = employeeArray.getJSONObject(counter).opt("name");
						Object employeeCode = employeeArray.getJSONObject(counter).opt("employeecode");
						Object joiningDate = employeeArray.getJSONObject(counter).opt("joiningdate");
						Object relievingDate = employeeArray.getJSONObject(counter).opt("relievingdate");
						Object noticePeriodStartDate = employeeArray.getJSONObject(counter).opt("noticeperiodstartdate");
						Object noticePeriodEndDate = employeeArray.getJSONObject(counter).opt("noticeperiodenddate");

						Object resignApplicationDate = employeeArray.getJSONObject(counter).opt("resign_application_date");
						Object accountOpeningDate = employeeArray.getJSONObject(counter).opt("account_opening_date");
						Object accountIsActive = employeeArray.getJSONObject(counter).opt("account_is_active.name");
						Object accountNo = employeeArray.getJSONObject(counter).opt("accountno");
						Object ctc = employeeArray.getJSONObject(counter).opt("ctc");

						if (employeeDetails != null && !employeeDetails.containsKey(Translator.integerValue(employeeId))) {
							employeeDetails.put(Translator.integerValue(employeeId), new Object[] { employeeName, employeeCode, joiningDate, relievingDate, noticePeriodStartDate, noticePeriodEndDate, resignApplicationDate, accountOpeningDate, accountIsActive, accountNo, ctc });
						}
						// employeeDetails
						if (employeeIdsForMonthlyAttendance.length() > 0) {
							employeeIdsForMonthlyAttendance += ",";
						}
						employeeIdsForMonthlyAttendance += employeeId;
					}
					if (employeeIdsForMonthlyAttendance.length() > 0) {
						JSONArray employeeMonthlyAttendanceArray = getEmployeeMonthlyAttendanceArray(employeeIdsForMonthlyAttendance, yearId, monthId);
						JSONArray employeeFnfDetailArray = getEmployeeFnfDetailArray(employeeIdsForMonthlyAttendance, yearId, monthId);

						sheet = workbook.createSheet("Paid_Days", 0);
						putPaidDays(employeeMonthlyAttendanceArray, employeeDetails, sheet, employeePayDays);

						sheet = workbook.createSheet("New_Joinees", 1);
						putNewJoinees(employeeDetails, sheet, currentMonthFirstDate, currentMonthLastDate);

						sheet = workbook.createSheet("Resigned_Cases", 2);
						putResigned(employeeDetails, sheet, currentMonthFirstDate, currentMonthLastDate, employeePayDays);

						sheet = workbook.createSheet("New_Bank_Account_Details", 3);
						putNewBankAccountDetails(employeeDetails, sheet, currentMonthFirstDate, currentMonthLastDate);

						sheet = workbook.createSheet("Hold", 4);
						putHoldDetails(employeeDetails, sheet, employeeFnfDetailArray, employeePayDays);

						sheet = workbook.createSheet("FnF", 5);
						putFnFDetails(employeeDetails, sheet, employeeFnfDetailArray, employeePayDays);

					}
				}
			}
		}
	}

	private void putFnFDetails(HashMap<Integer, Object[]> employeeDetails, WritableSheet sheet, JSONArray employeeFnfDetailArray, HashMap<Integer, Double> employeePayDays) throws Exception {
		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(font);
		cellFormat.setBackground(Colour.GRAY_25);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
		cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
		int row = 2;
		int column = 0;
		setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
		setHeaderValue(column++, row, "Emp. Code", cellFormat, sheet);
		setHeaderValue(column++, row, "Name", cellFormat, sheet);
		setHeaderValue(column++, row, "Pay Days", cellFormat, sheet);
		setHeaderValue(column++, row, "Resigned date", cellFormat, sheet);
		setHeaderValue(column++, row, "LDW", cellFormat, sheet);
		setHeaderValue(column++, row, "Effetive from", cellFormat, sheet);
		setHeaderValue(column++, row, "Salary Hold month", cellFormat, sheet);
		setHeaderValue(column++, row, "FnF date", cellFormat, sheet);
		int srNo = 1;
		for (int counter = 0; counter < employeeFnfDetailArray.length(); counter++) {
			Object fnfDate = employeeFnfDetailArray.getJSONObject(counter).opt("fnf_date");
			if (fnfDate != null) {
				int employeeId = Translator.integerValue(employeeFnfDetailArray.getJSONObject(counter).opt("employeeid"));
				Object[] details = employeeDetails.get(employeeId);
				Date noticePeriodEndDate = Translator.dateValue(details[5]);
				String salaryHoldMonth = "";
				if (noticePeriodEndDate != null) {
					salaryHoldMonth = new SimpleDateFormat("MMMM").format(noticePeriodEndDate);
				}
				Object payDays = employeePayDays.get(employeeId) == null ? "" : employeePayDays.get(employeeId);
				row++;
				column = 0;
				putValue(cellFormat1, sheet, srNo++, row, column++);
				putValue(cellFormat1, sheet, details[1] == null ? "" : details[1], row, column++);// employee code
				putValue(cellFormat1, sheet, details[0] == null ? "" : details[0], row, column++); // employee name
				putValue(cellFormat1, sheet, payDays == null ? "" : payDays, row, column++);
				putValue(cellFormat1, sheet, details[6] == null ? "" : details[6], row, column++);
				putValue(cellFormat1, sheet, details[3] == null ? "" : details[3], row, column++);
				putValue(cellFormat1, sheet, details[4] == null ? "" : details[4], row, column++);
				putValue(cellFormat1, sheet, salaryHoldMonth, row, column++);
				putValue(cellFormat1, sheet, fnfDate == null ? "" : fnfDate, row, column++);
			}
		}
	}

	private void putHoldDetails(HashMap<Integer, Object[]> employeeDetails, WritableSheet sheet, JSONArray employeeFnfDetailArray, HashMap<Integer, Double> employeePayDays) throws Exception {
		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(font);
		cellFormat.setBackground(Colour.GRAY_25);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
		cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
		int row = 2;
		int column = 0;
		setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
		setHeaderValue(column++, row, "Emp. Code", cellFormat, sheet);
		setHeaderValue(column++, row, "Name", cellFormat, sheet);
		setHeaderValue(column++, row, "Pay Days", cellFormat, sheet);
		setHeaderValue(column++, row, "Comments", cellFormat, sheet);
		setHeaderValue(column++, row, "Salary Hold Cases", cellFormat, sheet);
		setHeaderValue(column++, row, "% of hold", cellFormat, sheet);
		int srNo = 0;
		for (int counter = 0; counter < employeeFnfDetailArray.length(); counter++) {
			Object fnfDate = employeeFnfDetailArray.getJSONObject(counter).opt("fnf_date");
			if (fnfDate == null) {
				int employeeId = Translator.integerValue(employeeFnfDetailArray.getJSONObject(counter).opt("employeeid"));
				Object payDays = employeePayDays.get(employeeId) == null ? "" : employeePayDays.get(employeeId);
				Object remarks = employeeFnfDetailArray.getJSONObject(counter).opt("remarks");
				Object percentSalaryHold = employeeFnfDetailArray.getJSONObject(counter).opt("percent_salary_hold");
				Object[] details = employeeDetails.get(employeeId);
				srNo++;
				row++;
				column = 0;
				putValue(cellFormat1, sheet, srNo, row, column++);
				putValue(cellFormat1, sheet, details[1] == null ? "" : details[1], row, column++);// employee code
				putValue(cellFormat1, sheet, details[0] == null ? "" : details[0], row, column++); // employee name
				putValue(cellFormat1, sheet, payDays == null ? "" : payDays, row, column++);
				putValue(cellFormat1, sheet, remarks == null ? "" : remarks, row, column++);
				putValue(cellFormat1, sheet, "Hold Salary", row, column++);
				putValue(cellFormat1, sheet, percentSalaryHold == null ? "" : percentSalaryHold, row, column++);
			}
		}
	}

	private void putNewBankAccountDetails(HashMap<Integer, Object[]> employeeDetails, WritableSheet sheet, Date currentMonthFirstDate, Date currentMonthLastDate) throws Exception {
		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(font);
		cellFormat.setBackground(Colour.GRAY_25);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
		cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
		int row = 2;
		int column = 0;

		setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
		setHeaderValue(column++, row, "Emp. Code", cellFormat, sheet);
		setHeaderValue(column++, row, "Name", cellFormat, sheet);
		setHeaderValue(column++, row, "A/C No.", cellFormat, sheet);
		setHeaderValue(column++, row, "A/C Status", cellFormat, sheet);
		int srNo = 0;
		for (Integer employeeId : employeeDetails.keySet()) {
			Object[] details = employeeDetails.get(employeeId);
			if (details != null && details[7] != null && ((Translator.dateValue(details[7]).after(currentMonthFirstDate) && Translator.dateValue(details[7]).before(currentMonthLastDate)) || (Translator.dateValue(details[7]).equals(currentMonthFirstDate) || Translator.dateValue(details[7]).equals(currentMonthLastDate)))) {
				srNo++;
				row++;
				column = 0;
				putValue(cellFormat1, sheet, srNo, row, column++);
				putValue(cellFormat1, sheet, details[1] == null ? "" : details[1], row, column++);// employee code
				putValue(cellFormat1, sheet, details[0] == null ? "" : details[0], row, column++); // employee name
				putValue(cellFormat1, sheet, details[9] == null ? "" : details[9], row, column++); // account no
				putValue(cellFormat1, sheet, details[8] == null ? "" : details[8], row, column++); // account status
			}
		}
	}

	private void putResigned(HashMap<Integer, Object[]> employeeDetails, WritableSheet sheet, Date currentMonthFirstDate, Date currentMonthLastDate, HashMap<Integer, Double> employeePayDays) throws Exception {
		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(font);
		cellFormat.setBackground(Colour.GRAY_25);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
		cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
		int row = 2;
		int column = 0;

		setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
		setHeaderValue(column++, row, "Emp. Code", cellFormat, sheet);
		setHeaderValue(column++, row, "Name", cellFormat, sheet);
		setHeaderValue(column++, row, "Pay Days", cellFormat, sheet);

		setHeaderValue(column++, row, "Resigned Date", cellFormat, sheet);
		setHeaderValue(column++, row, "LDW", cellFormat, sheet);
		setHeaderValue(column++, row, "Effective From", cellFormat, sheet);
		setHeaderValue(column++, row, "Salary Hold Month", cellFormat, sheet);

		int srNo = 0;
		// new Object[] { employeeName, employeeCode, joiningDate,
		// relievingDate, noticePeriodStartDate, noticePeriodEndDate }
		for (Integer employeeId : employeeDetails.keySet()) {
			Object[] details = employeeDetails.get(employeeId);
			if (details != null) {
				Date resignedDate = Translator.dateValue(details[3]);
				Date noticePeriodStartDate = Translator.dateValue(details[4]);
				Date noticePeriodEndDate = Translator.dateValue(details[5]);
				String salaryHoldMonth = "";
				if (noticePeriodEndDate != null) {
					salaryHoldMonth = new SimpleDateFormat("MMMM").format(noticePeriodEndDate);
				}
				if (resignedDate != null) {
					salaryHoldMonth = new SimpleDateFormat("MMMM").format(resignedDate);
					srNo++;
					row++;
					column = 0;
					putValue(cellFormat1, sheet, srNo, row, column++);
					putValue(cellFormat1, sheet, details[1] == null ? "" : details[1], row, column++); // employee code
					putValue(cellFormat1, sheet, details[0] == null ? "" : details[0], row, column++); // employee name
					putValue(cellFormat1, sheet, Translator.doubleValue(employeePayDays.get(employeeId)), row, column++);
					putValue(cellFormat1, sheet, details[6] == null ? "" : details[6], row, column++);
					putValue(cellFormat1, sheet, details[3] == null ? "" : details[3], row, column++);
					putValue(cellFormat1, sheet, details[5] == null ? "" : details[5], row, column++);
					putValue(cellFormat1, sheet, salaryHoldMonth, row, column++);
				} else if (noticePeriodStartDate != null && noticePeriodEndDate != null) {
					if ((currentMonthFirstDate.after(noticePeriodStartDate) && currentMonthFirstDate.before(noticePeriodEndDate)) || (currentMonthLastDate.after(noticePeriodStartDate) && currentMonthFirstDate.before(noticePeriodEndDate))) {
						srNo++;
						row++;
						column = 0;
						putValue(cellFormat1, sheet, srNo, row, column++);
						putValue(cellFormat1, sheet, details[1] == null ? "" : details[1], row, column++); // employee code
						putValue(cellFormat1, sheet, details[0] == null ? "" : details[0], row, column++); // employee name
						putValue(cellFormat1, sheet, Translator.doubleValue(employeePayDays.get(employeeId)), row, column++);
						putValue(cellFormat1, sheet, details[6] == null ? "" : details[6], row, column++);
						putValue(cellFormat1, sheet, details[3] == null ? "" : details[3], row, column++);
						putValue(cellFormat1, sheet, details[5] == null ? "" : details[5], row, column++);
						putValue(cellFormat1, sheet, salaryHoldMonth, row, column++);
					}
				}
			}
		}
	}

	private void putNewJoinees(HashMap<Integer, Object[]> employeeDetails, WritableSheet sheet, Date currentMonthFirstDate, Date currentMonthLastDate) throws Exception {
		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(font);
		cellFormat.setBackground(Colour.GRAY_25);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
		cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
		int row = 2;
		int column = 0;

		setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
		setHeaderValue(column++, row, "Emp. Code", cellFormat, sheet);
		setHeaderValue(column++, row, "Name", cellFormat, sheet);
		setHeaderValue(column++, row, "Pay Days", cellFormat, sheet);
		int srNo = 0;
		for (Integer employeeId : employeeDetails.keySet()) {
			Object[] details = employeeDetails.get(employeeId);
			if (details != null) {
				Date joiningDate = Translator.dateValue(details[2]);
				if (joiningDate.equals(currentMonthFirstDate) || joiningDate.equals(currentMonthLastDate) || (joiningDate.after(currentMonthFirstDate) && joiningDate.before(currentMonthLastDate))) {
					srNo++;
					row++;
					column = 0;
					putValue(cellFormat1, sheet, srNo, row, column++);
					putValue(cellFormat1, sheet, details[1] == null ? "" : details[1], row, column++);// employee code
					putValue(cellFormat1, sheet, details[0] == null ? "" : details[0], row, column++); // employee name
					putValue(cellFormat1, sheet, details[2] == null ? "" : details[2], row, column++);
				}
			}
		}
	}

	private void putPaidDays(JSONArray employeeMonthlyAttendanceArray, HashMap<Integer, Object[]> employeeDetails, WritableSheet sheet, HashMap<Integer, Double> employeePayDays) throws Exception {

		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(font);
		cellFormat.setBackground(Colour.GRAY_25);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
		cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
		int row = 2;
		int column = 0;

		setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
		setHeaderValue(column++, row, "Emp. Code", cellFormat, sheet);
		setHeaderValue(column++, row, "Name", cellFormat, sheet);
		setHeaderValue(column++, row, "Pay Days", cellFormat, sheet);
		int srNo = 0;
		for (int counter = 0; counter < employeeMonthlyAttendanceArray.length(); counter++) {
			Object presentDays = employeeMonthlyAttendanceArray.getJSONObject(counter).opt("presentdays");
			Object paidAbsent = employeeMonthlyAttendanceArray.getJSONObject(counter).opt("paidabsents");
			Object payableLeaves = employeeMonthlyAttendanceArray.getJSONObject(counter).opt("payableleaves");
			Object netExtraWorkingDays = employeeMonthlyAttendanceArray.getJSONObject(counter).opt("carryextraworkingday");
			Object actualNonWorkingDays = employeeMonthlyAttendanceArray.getJSONObject(counter).opt("actualnonworkingdays");
			double payDays = Translator.doubleValue(presentDays) + Translator.doubleValue(paidAbsent) + Translator.doubleValue(payableLeaves) + Translator.doubleValue(netExtraWorkingDays) + Translator.doubleValue(actualNonWorkingDays);
			int employeeId = Translator.integerValue(employeeMonthlyAttendanceArray.getJSONObject(counter).opt("employeeid"));
			Object[] details = employeeDetails.get(employeeId);
			if (details != null) {
				srNo++;
				row++;
				column = 0;
				putValue(cellFormat1, sheet, srNo, row, column++);
				putValue(cellFormat1, sheet, details[1] == null ? "" : details[1], row, column++);// employee code
				putValue(cellFormat1, sheet, details[0] == null ? "" : details[0], row, column++); // employee name
				putValue(cellFormat1, sheet, payDays, row, column++);
				if (employeePayDays != null && !employeePayDays.containsKey(employeeId)) {
					employeePayDays.put(employeeId, payDays);
				}
			}
		}
	}

	public static void setHeaderValue(int column, int rowNo, String value, WritableCellFormat cellFormat, WritableSheet sheet) throws RowsExceededException, WriteException {
		Label label = new Label(column, rowNo, value, cellFormat);
		sheet.addCell(label);

	}

	private static void putValue(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, cellFormat);
		sheet.setColumnView(column, 20);
		sheet.addCell(label);
	}

	private JSONArray getEmployeeMonthlyAttendanceArray(String employeeIdsForMonthlyAttendance, Object yearId, Object monthId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("employeeid");
		array.put("ismanualupdate");
		array.put("presentdays");
		array.put("leaves");
		array.put("paidabsents");
		array.put("payableleaves");
		array.put("carryextraworkingday");
		array.put("actualnonworkingdays");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid IN(" + employeeIdsForMonthlyAttendance + ")  and monthid = " + monthId + " and yearid = " + yearId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeemonthlyattendance");
	}

	private JSONArray getEmployeeFnfDetailArray(String employeeIdsForMonthlyAttendance, Object yearId, Object monthId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_salary_hold");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("employeeid");
		array.put("percent_salary_hold");
		array.put("monthid.name");
		array.put("yearid.name");
		array.put("remarks");
		array.put("fnf_date");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid IN(" + employeeIdsForMonthlyAttendance + ")  and monthid = " + monthId + " and yearid = " + yearId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_salary_hold");
	}

	private JSONArray getEmployeeArray(Object branchId, String currentMonthFirstDateInString, String currentMonthLastDateInString) throws Exception {
		String filters = "";
		if (branchId != null) {
			filters = " AND branchid = " + branchId;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("joiningdate");
		columnArray.put("relievingdate");
		columnArray.put("branchid");
		columnArray.put("subbranchid");
		columnArray.put("employeecode");
		columnArray.put("accountno");
		columnArray.put("name");
		columnArray.put("ctc");
		columnArray.put("noticeperiodstartdate");
		columnArray.put("noticeperiodenddate");
		columnArray.put("accountno");

		columnArray.put("resign_application_date");
		columnArray.put("account_opening_date");
		columnArray.put("account_is_active.name");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = 1 AND joiningdate <= '" + currentMonthLastDateInString + "'" + filters);
		query.put(Data.Query.OPTION_FILTERS, "relievingdate>= '" + currentMonthFirstDateInString + "'" + filters);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	private JSONArray getReportParametersFromPfReports(String key) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put("branchid");
		columnArray.put("monthid");
		columnArray.put("yearid");
		columnArray.put("monthid.name");
		columnArray.put("yearid.name");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "pfreportforyear");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + key);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("pfreportforyear");
	}
}
