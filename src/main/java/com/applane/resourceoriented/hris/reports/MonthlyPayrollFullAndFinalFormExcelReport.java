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

public class MonthlyPayrollFullAndFinalFormExcelReport extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		doPost(req, resp);
	}

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
				fullAndFinalForm(key, workbook);

				workbook.write();
				workbook.close();

			}
		} catch (BusinessLogicException e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeePerformanceReportInExcel.class.getName(), e);
			LogUtility.writeLog("MonthlyPayrollFullAndFinalFormExcelReport >> trace >> " + trace);
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeePerformanceReportInExcel.class.getName(), e);
			LogUtility.writeLog("MonthlyPayrollFullAndFinalFormExcelReport >> trace >> " + trace);
		}

	}

	public void fullAndFinalForm(String key, WritableWorkbook workbook) throws Exception {
		JSONArray employeeFnfFormDetailArray = getEmployeeFnfFormDetailArray(key);
		LogUtility.writeError("employeeFnfFormDetailArray >> " + employeeFnfFormDetailArray);
		WritableSheet sheet = workbook.createSheet("Paid_Days", 0);
		if (employeeFnfFormDetailArray != null && employeeFnfFormDetailArray.length() > 0) {
			// Object employeeId = employeeFnfFormDetailArray.getJSONObject(0).opt("employeeid");
			// Object branchId = employeeFnfFormDetailArray.getJSONObject(0).opt("employeeid.branchid");
			// Object monthId = employeeFnfFormDetailArray.getJSONObject(0).opt("monthid");
			// Object yearId = employeeFnfFormDetailArray.getJSONObject(0).opt("yearid");
			// String currentYearName = Translator.stringValue(employeeFnfFormDetailArray.getJSONObject(0).opt("yearid.name"));
			// String currentMonthName = Translator.stringValue(employeeFnfFormDetailArray.getJSONObject(0).opt("monthid.name"));
			// Date currentMonthFirstDate = DataTypeUtilities.getMonthFirstDate(currentYearName, currentMonthName);
			// Date currentMonthLastDate = DataTypeUtilities.getMonthLastDate(currentMonthFirstDate);
			// String currentMonthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(currentMonthFirstDate);
			// String currentMonthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(currentMonthLastDate);

			putFnFFormDetails(sheet, employeeFnfFormDetailArray);

		}
	}

	private void putFnFFormDetails(WritableSheet sheet, JSONArray employeeFnfFormDetailArray) throws Exception {

		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(font);
		cellFormat.setBackground(Colour.GRAY_25);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
		cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font2 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat cellFormat2 = new WritableCellFormat(font2);
		cellFormat2.setAlignment(Alignment.RIGHT);
		cellFormat2.setBorder(Border.ALL, BorderLineStyle.THIN);

		int row = 2;
		int column = 0;

		for (int counter = 0; counter < employeeFnfFormDetailArray.length(); counter++) {
			int employeeId = Translator.integerValue(employeeFnfFormDetailArray.getJSONObject(counter).opt("employeeid"));
			Object employeeName = employeeFnfFormDetailArray.getJSONObject(counter).opt("employeeid.name");
			Object employeeCode = employeeFnfFormDetailArray.getJSONObject(counter).opt("employeeid.employeecode");
			Object employeeDesignation = employeeFnfFormDetailArray.getJSONObject(counter).opt("employeeid.designationid.name");
			Object employeeCtc = employeeFnfFormDetailArray.getJSONObject(counter).opt("employeeid.ctc");
			Object fromDate = employeeFnfFormDetailArray.getJSONObject(counter).opt("from_date");
			Object toDate = employeeFnfFormDetailArray.getJSONObject(counter).opt("to_date");

			Object resignationApplicationDate = employeeFnfFormDetailArray.getJSONObject(counter).opt("employeeid.resign_application_date");
			Object employeeLastWorkingDay = employeeFnfFormDetailArray.getJSONObject(counter).opt("employeeid.relievingdate");
			Object employeeActualLastWorkingDay = employeeFnfFormDetailArray.getJSONObject(counter).opt("employeeid.noticeperiodenddate");

			Object fnfAdd = employeeFnfFormDetailArray.getJSONObject(counter).opt("hris_salary_hold_fnf_add");
			Object fnfLess = employeeFnfFormDetailArray.getJSONObject(counter).opt("hris_salary_hold_fnf_less");

			Object yearId = employeeFnfFormDetailArray.getJSONObject(counter).opt("yearid");
			Object monthId = employeeFnfFormDetailArray.getJSONObject(counter).opt("monthid");

			JSONArray salarySheetArray = getSalarySheetArray(employeeId, yearId, monthId);
			LogUtility.writeError("salarySheetArray >> " + salarySheetArray);
			HashMap<Integer, Double> salaryDetailsMap = new HashMap<Integer, Double>();
			putIntoSalarySheetMap(salaryDetailsMap, salarySheetArray);

			sheet.mergeCells(column, row, column + 3, row);
			setHeaderValue(column++, row++, "Full and Final For " + employeeName, cellFormat, sheet);

			column = 0;
			sheet.mergeCells(column, row, column + 3, row);
			setHeaderValue(column++, row, "F & F Sheet", cellFormat, sheet);

			column = 0;
			row++;
			putValue(cellFormat1, sheet, "NAME OF EMPLOYEE", row, column++);
			putValue(cellFormat1, sheet, employeeName, row, column++);

			putValue(cellFormat1, sheet, "DATE OF RESIGNATION", row, column++);
			putValue(cellFormat1, sheet, resignationApplicationDate == null ? "" : ("" + resignationApplicationDate), row, column++);

			column = 0;
			row++;

			putValue(cellFormat1, sheet, "EMOLOYEE CODE", row, column++);
			putValue(cellFormat1, sheet, employeeCode, row, column++);

			putValue(cellFormat1, sheet, "LAST WORK. DAY", row, column++);
			putValue(cellFormat1, sheet, "" + employeeLastWorkingDay == null ? "" : ("" + employeeLastWorkingDay), row, column++);

			column = 0;
			row++;

			putValue(cellFormat1, sheet, "DESIGNATION", row, column++);
			putValue(cellFormat1, sheet, employeeDesignation == null ? "" : ("" + employeeDesignation), row, column++);

			putValue(cellFormat1, sheet, "ACTUAL LWD", row, column++);
			putValue(cellFormat1, sheet, employeeActualLastWorkingDay == null ? "" : ("" + employeeActualLastWorkingDay), row, column++);

			column = 0;
			row++;

			putValue(cellFormat1, sheet, "", row, column++);
			putValue(cellFormat1, sheet, "", row, column++);

			putValue(cellFormat1, sheet, "Short notice ", row, column++);
			putValue(cellFormat1, sheet, "", row, column++);

			column = 0;
			row++;

			putValue(cellFormat1, sheet, " ", row, column++);
			putValue(cellFormat2, sheet, "Rs.", row, column++);

			column = 0;
			row++;

			putValue(cellFormat1, sheet, "MONTHLY SALARY", row, column++);
			putValue(cellFormat2, sheet, employeeCtc == null ? "" : ("" + employeeCtc), row, column++);

			column = 0;
			row += 2;

			putValue(cellFormat1, sheet, "SALARY FOR THE PERIOD OF", row, column++);
			putValue(cellFormat2, sheet, "", row, column++);

			column = 0;
			row++;
			putValue(cellFormat1, sheet, fromDate + " to " + toDate, row, column++);
			putValue(cellFormat2, sheet, "" + salaryDetailsMap.get(employeeId), row, column++);

			column = 0;
			row++;
			putValue(cellFormat1, sheet, "ADD:", row, column++);
			putValue(cellFormat2, sheet, "", row, column++);
			double totalPayableSalary = salaryDetailsMap.get(employeeId);
			if (fnfAdd != null && fnfAdd instanceof JSONArray) {
				for (int coutner = 0; coutner < ((JSONArray) fnfAdd).length(); coutner++) {
					Object name = ((JSONArray) fnfAdd).getJSONObject(coutner).opt("name");
					Object amount = ((JSONArray) fnfAdd).getJSONObject(coutner).opt("amount");
					totalPayableSalary += Translator.doubleValue(amount);
					column = 0;
					row++;
					putValue(cellFormat1, sheet, name, row, column++);
					putValue(cellFormat2, sheet, amount, row, column++);
				}
			}

			column = 0;
			row++;
			putValue(cellFormat, sheet, "A Total", row, column++);
			putValue(cellFormat2, sheet, "" + totalPayableSalary, row, column++);

			column = 0;
			row++;
			putValue(cellFormat1, sheet, "LESS:", row, column++);
			putValue(cellFormat2, sheet, "", row, column++);

			double bTotal = 0.0;
			if (fnfLess != null && fnfLess instanceof JSONArray) {
				for (int coutner = 0; coutner < ((JSONArray) fnfLess).length(); coutner++) {
					Object name = ((JSONArray) fnfLess).getJSONObject(coutner).opt("name");
					Object amount = ((JSONArray) fnfLess).getJSONObject(coutner).opt("amount");
					bTotal += Translator.doubleValue(amount);
					column = 0;
					row++;
					putValue(cellFormat1, sheet, name, row, column++);
					putValue(cellFormat2, sheet, amount, row, column++);
				}
			}
			column = 0;
			row++;
			putValue(cellFormat, sheet, "B Total", row, column++);
			putValue(cellFormat2, sheet, "" + bTotal, row, column++);

			column = 0;
			row++;
			putValue(cellFormat, sheet, "Net payable/Recovery(A-B)", row, column++);
			putValue(cellFormat2, sheet, "" + (totalPayableSalary - bTotal), row, column++);

			column = 0;
			row++;
			sheet.mergeCells(column, row, column + 3, row);
			putValue(cellFormat1, sheet, "I " + employeeName + " hereby acknowledge that I have received my full & final settlement from Daffodil Software Ltd.", row, column++);

			column = 0;
			row++;
			sheet.mergeCells(column, row, column + 3, row);
			putValue(cellFormat1, sheet, "and no further dues are payable to me by the company.", row, column++);

			column = 0;
			row += 2;
			sheet.mergeCells(column, row, column + 3, row);
			putValue(cellFormat1, sheet, "Signature…………………………………………………………………", row, column++);

			column = 0;
			row++;
			sheet.mergeCells(column, row, column + 3, row);
			putValue(cellFormat1, sheet, "Date……………………………………………….", row, column++);

			putValue(cellFormat2, sheet, "" + (totalPayableSalary - bTotal), row, column++);

		}
	}

	private void putIntoSalarySheetMap(HashMap<Integer, Double> salaryDetailsMap, JSONArray salarySheetArray) throws Exception {
		for (int counter = 0; counter < salarySheetArray.length(); counter++) {
			int employeeid = Translator.integerValue(salarySheetArray.getJSONObject(counter).opt("employeeid"));
			double payableAmount = Translator.doubleValue(salarySheetArray.getJSONObject(counter).opt("payableamount"));
			if (salaryDetailsMap.containsKey(employeeid)) {
				payableAmount += salaryDetailsMap.get(employeeid);
			}
			salaryDetailsMap.put(employeeid, payableAmount);
		}

	}

	private JSONArray getSalarySheetArray(Object employeeId, Object yearId, Object monthId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("employeeid");
		array.put("payableamount");

		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid IN(" + employeeId + ")  and monthid = " + monthId + " and yearid = " + yearId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("salarysheet");
	}

	private JSONArray getEmployeeFnfFormDetailArray(Object key) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_salary_hold_fnf_form");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("employeeid");
		array.put("employeeid.name");
		array.put("employeeid.branchid");
		array.put("employeeid.employeecode");
		array.put("employeeid.ctc");
		array.put("employeeid.designationid.name");
		array.put("employeeid.resign_application_date");
		array.put("employeeid.relievingdate");
		array.put("employeeid.noticeperiodenddate");
		// array.put("employeeid.resign_application_date");
		// array.put("employeeid.resign_application_date");
		array.put("monthid");
		array.put("yearid");
		array.put("monthid.name");
		array.put("yearid.name");
		array.put("from_date");
		array.put("to_date");

		JSONArray childs = new JSONArray();
		JSONObject innerObject = new JSONObject();
		JSONObject innerQuery = new JSONObject();
		JSONArray innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("name");
		innerColumns.put("amount");
		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_salary_hold_fnf_add");
		innerObject.put("relatedcolumn", "salary_hold_fnf_form_id");
		innerObject.put("alias", "hris_salary_hold_fnf_add");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);
		childs.put(innerObject);

		innerObject = new JSONObject();
		innerQuery = new JSONObject();
		innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("name");
		innerColumns.put("amount");
		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_salary_hold_fnf_less");
		innerObject.put("relatedcolumn", "salary_hold_fnf_form_id");
		innerObject.put("alias", "hris_salary_hold_fnf_less");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);
		childs.put(innerObject);

		query.put(Data.Query.CHILDS, childs);

		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + key);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_salary_hold_fnf_form");
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
}
