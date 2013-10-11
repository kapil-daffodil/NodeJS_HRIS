package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.text.DecimalFormat;

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
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;

public class BankSalaryAdviceExcelReport extends HttpServlet {
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
		String decompositionParameter = "attachment;filename=Salary_Advice.xls";
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
			String key = req.getParameter("keys");
			if (key.length() > 2) {
				key = key.substring(1, key.length() - 1);
			}
			String[] keys = key.split(",");
			key = keys[0];

			JSONArray salaryDetailArray = getSalaryDetailstRecords(key);

			int salarySheetArrayCount = (salaryDetailArray == null || salaryDetailArray.length() == 0) ? 0 : salaryDetailArray.length();
			if (salarySheetArrayCount > 0) {
				String bankName = Translator.stringValue(salaryDetailArray.getJSONObject(0).opt("receiptcenter_id.name"));
				String chequeNo = salaryDetailArray.getJSONObject(0).opt("chequeno") == null ? "(Cheque No. Not Found)" : Translator.stringValue(salaryDetailArray.getJSONObject(0).opt("chequeno"));
				String chequeDate = salaryDetailArray.getJSONObject(0).opt("chequedate") == null ? "(Date Not Found)" : Translator.stringValue(salaryDetailArray.getJSONObject(0).opt("chequedate"));
				int transferSalaryId = Translator.integerValue(salaryDetailArray.getJSONObject(0).opt("transfer_salary_id"));
				int row = 2, tempRow = 0;
				int column = 0, tempColumn = 0;

				String todayDate = EmployeeSalaryGenerationServlet.getDateInString(SystemParameters.getCurrentDate());
				putHeaderDefault(sheet, todayDate, row++, column);
				putHeaderDefault(sheet, "To", row++, column);
				putHeaderDefault(sheet, bankName, row++, column);
				putHeaderDefault(sheet, "Sub:- Transfer of Amount to the Credit Of Following employees account", row++, column);

				putHeaderDefault(sheet, "Dear Sir,", row++, ++column);

				tempRow = row;
				tempColumn = column;
				row++;

				putHeaderDefault(sheet, "Vide Cheque no. " + chequeNo + ", Dt. " + chequeDate + " to employees", row++, column);
				putHeaderDefault(sheet, "account for below mentioned purposes", row++, column);
				putHeaderDefault(sheet, "This credit list is a true copy of the mail/pendrive list sent to you.", row++, column);
				column = 0;
				row++;
				JSONArray salaryDetailLineItemArray = salaryDetailArray.getJSONObject(0).optJSONArray("procurement_salary_advise_lineitems");
				putHeaderDefault(sheet, "Sr. No.", row, column++);
				putHeaderDefault(sheet, "Employee Code", row, column++);
				putHeaderDefault(sheet, "Employee Name", row, column++);
				putHeaderDefault(sheet, "Account No.", row, column++);
				putHeaderDefault(sheet, "Payable Amount", row, column++);
				double totalPayableAmount = 0.0;
				for (int counter = 0; counter < salaryDetailLineItemArray.length(); counter++) {
					column = 0;
					String employeeCode = Translator.stringValue(salaryDetailLineItemArray.getJSONObject(counter).opt("vendor_id.contactname"));
					String employeeName = Translator.stringValue(salaryDetailLineItemArray.getJSONObject(counter).opt("vendor_id.name_in_bank"));
					String employeeAccNo = Translator.stringValue(salaryDetailLineItemArray.getJSONObject(counter).opt("bank_account"));
					double payableAmount = Translator.doubleValue(salaryDetailLineItemArray.getJSONObject(counter).opt("payble_amount_amount"));
					boolean isOnHole = Translator.booleanValue(salaryDetailLineItemArray.getJSONObject(counter).opt("is_on_hold"));
					if (((employeeAccNo != null && employeeAccNo.length() > 0) || transferSalaryId == 2) && payableAmount > 0.0 && !isOnHole) {
						payableAmount += 0.50;
						int payableAmount1 = (int) payableAmount;
						row++;
						totalPayableAmount += payableAmount1;
						putHeaderDefault(sheet, (counter + 1), row, column++);
						putHeaderDefault(sheet, employeeCode, row, column++);
						putHeaderDefault(sheet, employeeName, row, column++);
						putHeaderDefault(sheet, employeeAccNo, row, column++);
						putHeader(numbercellformat, sheet, payableAmount1, row, column++);
					}
				}
				putHeaderDefault(sheet, "Total", ++row, 2);
				putHeader(numbercellformat, sheet, totalPayableAmount, row, 4);
				putHeaderDefault(sheet, "Please credit the amount of Rs. " + new DecimalFormat().format(totalPayableAmount) + "/-", tempRow, tempColumn);
			}
			workbook.write();

			workbook.close();
		} catch (

		Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("BankAdviseExcelReport >> Exception >> " + trace);
		}
	}

	private JSONArray getSalaryDetailstRecords(String selectedKey) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "procurement_salary_advise");
		columnArray.put(Updates.KEY);

		JSONObject innerObject = new JSONObject();
		JSONObject innerQuery = new JSONObject();
		JSONArray innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("vendor_id.orgname");
		innerColumns.put("vendor_id.contactname");
		innerColumns.put("vendor_id.name_in_bank");
		innerColumns.put("bank_account");
		innerColumns.put("payble_amount_amount");
		innerColumns.put("is_on_hold");

		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "procurement_salary_advise_lineitems");

		innerObject.put("relatedcolumn", "salary_advise_id");
		innerObject.put("alias", "procurement_salary_advise_lineitems");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);

		query.put(Data.Query.CHILDS, new JSONArray().put(innerObject));

		columnArray.put("receiptcenter_id.name");
		columnArray.put("chequeno");
		columnArray.put("chequedate");
		columnArray.put("transfer_salary_id");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " IN(" + selectedKey + ")");
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("procurement_salary_advise");

		return rows;
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
