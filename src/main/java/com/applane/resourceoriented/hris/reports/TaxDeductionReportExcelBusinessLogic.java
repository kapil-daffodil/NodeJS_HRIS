package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.write.NumberFormats;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import org.json.JSONArray;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;

public class TaxDeductionReportExcelBusinessLogic extends HttpServlet {

	private static final long serialVersionUID = 1L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			TaxDeductionExcelReport(req, resp);
		} catch (Exception e) {
			LogUtility.writeLog("Salary sheet Excel Report Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doGet(req, resp);
	}

	private void TaxDeductionExcelReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Tax_Deduction_Report.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");
		try {
			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			WritableSheet sheet = workbook.createSheet("First Sheet", 0);
			WritableFont font = new WritableFont(WritableFont.ARIAL, 12, WritableFont.BOLD);
			WritableCellFormat hdFormat = new WritableCellFormat(font);
			hdFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
			hdFormat.setAlignment(Alignment.CENTRE);

			WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
			numbercellformat.setFont(font);
			numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);

			String monthName = null;
			String yearName = null;
			String branchName = null;
			String key = req.getParameter("keys");
			if (key.length() > 2) {
				key = key.substring(1, key.length() - 1);
			}
			String[] keys = key.split(",");
			key = keys[0];
			EmployeeSalarySheetReport employeSalarySheet = new EmployeeSalarySheetReport();
			LinkedHashMap<Integer, Object[]> employRecords = new LinkedHashMap<Integer, Object[]>();
			JSONArray takeSalarySheetArray = employeSalarySheet.getTakeSalarySheetRecord(key);
			int takeSalarySheetArrayCount = (takeSalarySheetArray == null || takeSalarySheetArray.length() == 0) ? 0 : takeSalarySheetArray.length();
			if (takeSalarySheetArrayCount > 0) {
				Object branchId = takeSalarySheetArray.getJSONObject(0).opt("branchid");
				Object monthId = takeSalarySheetArray.getJSONObject(0).opt("monthid");
				Object yearId = takeSalarySheetArray.getJSONObject(0).opt("yearid");
				monthName = DataTypeUtilities.stringValue(takeSalarySheetArray.getJSONObject(0).opt("monthid.name"));
				yearName = DataTypeUtilities.stringValue(takeSalarySheetArray.getJSONObject(0).opt("yearid.name"));
				branchName = DataTypeUtilities.stringValue(takeSalarySheetArray.getJSONObject(0).opt("branchid.name"));
				JSONArray salarySheetArray = employeSalarySheet.getSalarySheetRecords(monthId, yearId, branchId);
				if (salarySheetArray != null && salarySheetArray.length() > 0) {
					for (int counter = 0; counter < salarySheetArray.length(); counter++) {
						Object[] employeeDetail = new Object[6];
						int employeeId = salarySheetArray.getJSONObject(counter).optInt("employeeid");
						Object employeeName = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.name"));
						String employeeCode = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.employeecode"));
						String accountNo = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.accountno"));
						Object penNo = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.panno"));
						JSONArray monthlySalaryArray = employeSalarySheet.getEmployeeMonthlySalaryRecords(employeeId, monthId, yearId, "");
						double totalTaxAmount = 0d;
						if (monthlySalaryArray != null && monthlySalaryArray.length() > 0) {
							for (int counter1 = 0; counter1 < monthlySalaryArray.length(); counter1++) {
								int salaryComponentId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid");
								String monthlySalaryComponentName = monthlySalaryArray.getJSONObject(counter1).optString("salarycomponentid.name");
								double receivedAmount = ((Number) (monthlySalaryArray.getJSONObject(counter1).opt("amount") == null ? 0.0 : monthlySalaryArray.getJSONObject(counter1).opt("amount"))).doubleValue();
								if (monthlySalaryComponentName.toLowerCase().indexOf("tax") != -1) {
									totalTaxAmount += receivedAmount;
								}
							}
						}
						employeeDetail[0] = employeeName;
						employeeDetail[1] = counter + 1;
						employeeDetail[2] = employeeCode;
						employeeDetail[3] = (int) (totalTaxAmount + 0.5);
						employeeDetail[4] = accountNo;
						employeeDetail[5] = penNo;
						employRecords.put(employeeId, employeeDetail);
					}
				}
			}
			int row = 2;
			int column = 0;
			sheet.mergeCells(column, row - 1, column + 5, row - 1);
			employeSalarySheet.setValueInSheet(sheet, font, hdFormat, row - 1, column, "Tax Deduction Report for Month " + monthName + ", " + yearName + " (" + branchName + ")");
			sheet.setColumnView(column, 10);
			employeSalarySheet.setValueInSheet(sheet, font, hdFormat, row, column++, "Sr. No.");
			sheet.setColumnView(column, 25);
			employeSalarySheet.setValueInSheet(sheet, font, hdFormat, row, column++, "Employee Name");
			sheet.setColumnView(column, 20);
			employeSalarySheet.setValueInSheet(sheet, font, hdFormat, row, column++, "PAN No.");
			sheet.setColumnView(column, 25);
			employeSalarySheet.setValueInSheet(sheet, font, hdFormat, row, column++, "Tax Deduction Amount");
			for (Integer employeeId : employRecords.keySet()) {
				row++;
				column = 0;
				Object[] employeeDetail = employRecords.get(employeeId);
				employeSalarySheet.putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[1], row, column++); // serial no.
				employeSalarySheet.putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[0], row, column++); // name of employee
				employeSalarySheet.putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[5], row, column++); // pen no.
				employeSalarySheet.putHeader(numbercellformat, sheet, employeeDetail[3] == null ? 0d : Double.parseDouble("" + employeeDetail[3]), row, column++); // Tax Amount.
			}
			workbook.write();
			workbook.close();
		} catch (Exception e) {
			LogUtility.writeLog("Salary sheet Excel Report Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}
}