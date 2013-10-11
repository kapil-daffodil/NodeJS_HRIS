package com.applane.resourceoriented.hris.reports;

import java.io.IOException;

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

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.utils.ExceptionUtils;

public class BankAdviseExcelReport extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		salarySheetExcelReport(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doGet(req, resp);
	}

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	private void salarySheetExcelReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Bank_Advise.xls";
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

			JSONArray takeSalarySheetArray = new EmployeeSalarySheetReport().getTakeSalarySheetRecord(key);

			int takeSalarySheetArrayCount = (takeSalarySheetArray == null || takeSalarySheetArray.length() == 0) ? 0 : takeSalarySheetArray.length();
			if (takeSalarySheetArrayCount > 0) {

				Object branchId = takeSalarySheetArray.getJSONObject(0).opt("branchid");
				Object monthId = takeSalarySheetArray.getJSONObject(0).opt("monthid");
				Object yearId = takeSalarySheetArray.getJSONObject(0).opt("yearid");
				// if (monthId != null) {
				// monthName = EmployeeSalarySheetReport.getMonthName(monthId);
				// }
				// if (yearId != null) {
				// yearName = EmployeeSalarySheetReport.getYearName(yearId);
				// }

				JSONArray salarySheetArray = new EmployeeSalarySheetReport().getSalarySheetRecords(monthId, yearId, branchId);

				int salarySheetArrayCount = (salarySheetArray == null || salarySheetArray.length() == 0) ? 0 : salarySheetArray.length();
				if (salarySheetArrayCount > 0) {
					int row = 5;
					int column = 0;

					putHeaderDefault(sheet, "Sr. No.", row, column++);
					putHeaderDefault(sheet, "Employee Code", row, column++);
					putHeaderDefault(sheet, "Employee Name", row, column++);
					putHeaderDefault(sheet, "Account No.", row, column++);
					putHeaderDefault(sheet, "Payable Amount", row, column++);
					for (int counter = 0; counter < salarySheetArrayCount; counter++) {
						column = 0;
						String employeeCode = Translator.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.employeecode"));
						String employeeName = Translator.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.name"));
						String employeeAccNo = Translator.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.accountno"));
						double payableAmount = Translator.doubleValue(salarySheetArray.getJSONObject(counter).opt("payableamount"));
						if (employeeAccNo != null && employeeAccNo.length() > 0 && payableAmount > 0.0) {
							row++;
							putHeaderDefault(sheet, (counter + 1), row, column++);
							putHeaderDefault(sheet, employeeCode, row, column++);
							putHeaderDefault(sheet, employeeName, row, column++);
							putHeaderDefault(sheet, employeeAccNo, row, column++);
							putHeader(numbercellformat, sheet, payableAmount, row, column++);
						}
					}
				}
				workbook.write();

				workbook.close();
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("BankAdviseExcelReport >> Exception >> " + trace);
		}
	}

	public void putHeader(WritableCellFormat numbercellformat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, numbercellformat);
		sheet.addCell(numobj);
	}

	public void putHeaderDefault(WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value);
		sheet.addCell(label);
	}
}
