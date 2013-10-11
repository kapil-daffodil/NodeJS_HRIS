package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.Workbook;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.write.Label;
import jxl.write.Number;
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
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;

public class InvoiceGrossMatchWithSalarySheetGrossExcelReport extends HttpServlet {

	private static final long serialVersionUID = 1L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		createLWFExcelReport(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doGet(req, resp);
	}

	private void createLWFExcelReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Match_Invoice_Amount_With_HRM_Salary.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");
		try {

			// String monthName = null;
			// String yearName = null;
			String key = req.getParameter("keys");
			if (key.length() > 2) {
				key = key.substring(1, key.length() - 1);
			}
			String[] keys = key.split(",");
			key = keys[0];
			JSONArray pfReportArray = getPFReportArray(key);
			int yearId = 0;
			int branchId = 0;
			int monthId = 0;

			if (pfReportArray != null && pfReportArray.length() > 0) {
				yearId = Translator.integerValue(pfReportArray.getJSONObject(0).opt("yearid"));
				branchId = Translator.integerValue(pfReportArray.getJSONObject(0).opt("branchid"));
				monthId = Translator.integerValue(pfReportArray.getJSONObject(0).opt("monthid"));
			}

			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
			WritableCellFormat cellFormat = new WritableCellFormat(font);
			cellFormat.setBackground(Colour.GRAY_25);
			cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
			WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
			WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
			cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);

			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			WritableSheet sheet = workbook.createSheet("Sheet_1", 0);

			JSONArray salarySheetArray = getSalarySheetArray(yearId, monthId, branchId);
			Map<String, Double> salarySheetInvoiceDetails = new HashMap<String, Double>();
			Map<String, String[]> employeeDetailsMap = new HashMap<String, String[]>();
			List<String> invoiceNumbersList = new ArrayList<String>();
			putInvoiceDetailsInListAndMap(invoiceNumbersList, salarySheetInvoiceDetails, salarySheetArray, employeeDetailsMap);
			int row = 2;
			int column = 0;

			HrisHelper.setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "employee", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Employee Code", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Invoice Number", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "HRM Gross", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "P2P Gross", cellFormat, sheet);
			if (invoiceNumbersList != null && invoiceNumbersList.size() > 0) {

				Map<String, Double> invoiceDetails = getInvoiceDetails(invoiceNumbersList);
				// LogUtility.writeError("invoiceDetails >> " + invoiceDetails + "<< salarySheetInvoiceDetails >> " + salarySheetInvoiceDetails);
				int counter = 1;
				for (String invoiceNumber : salarySheetInvoiceDetails.keySet()) {
					Double grossAmount = salarySheetInvoiceDetails.get(invoiceNumber);
					Double invoiceAmount = invoiceDetails.get(invoiceNumber);
					String[] employeeDetails = employeeDetailsMap.get(invoiceNumber);
					if (grossAmount != null) {
						if (invoiceAmount == null) {
							row++;
							column = 0;
							putHeader(cellFormat1, sheet, counter++, row, column++);
							putHeader(cellFormat1, sheet, employeeDetails[0], row, column++);
							putHeader(cellFormat1, sheet, employeeDetails[1], row, column++);
							putHeader(cellFormat1, sheet, invoiceNumber, row, column++);
							putHeader(cellFormat1, sheet, grossAmount, row, column++);
							HrisHelper.setHeaderValue(column++, row, "Not Found", cellFormat, sheet);
						} else {
							if (Math.round(grossAmount) != Math.round(invoiceAmount)) {
								row++;
								column = 0;
								putHeader(cellFormat1, sheet, counter++, row, column++);
								putHeader(cellFormat1, sheet, employeeDetails[0], row, column++);
								putHeader(cellFormat1, sheet, employeeDetails[1], row, column++);
								putHeader(cellFormat1, sheet, invoiceNumber, row, column++);
								putHeader(cellFormat1, sheet, grossAmount, row, column++);
								putHeader(cellFormat1, sheet, invoiceAmount, row, column++);
							}
						}
					}
				}
			}
			workbook.write();
			workbook.close();
		} catch (Exception e) {
			LogUtility.writeError("PFAnnualReturnReportForm6ARevised >> calculateAndWriteToSheet Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
		}
	}

	public void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, cellFormat);
		sheet.addCell(numobj);
	}

	private Map<String, Double> getInvoiceDetails(List<String> invoiceNumbersList) throws Exception {
		String invoices = "";// invoiceNumbersList.toString().substring(1, invoiceNumbersList.toString().length() - 1);
		// invoices = invoices.replaceAll(",", "','");
		// invoices = invoices.replaceAll(" ", "");
		for (int counter = 0; counter < invoiceNumbersList.size(); counter++) {
			if (counter == 0) {
				invoices += "";// "'";
			}
			invoices += invoiceNumbersList.get(counter);
			if (invoices.length() > 0 && counter != (invoiceNumbersList.size() - 1)) {
				invoices += "','";
			}
			if (counter == (invoiceNumbersList.size() - 1)) {
				invoices += "";// "'";
			}
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "procurement_vendorinvoices");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("invoiceno");
		columnArray.put("amount_amount");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "invoiceno IN('" + invoices + "')");
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray invoiceArray = employeeObject.getJSONArray("procurement_vendorinvoices");
		Map<String, Double> invoiceMap = new HashMap<String, Double>();

		if (invoiceArray != null && invoiceArray.length() > 0) {
			for (int counter = 0; counter < invoiceArray.length(); counter++) {
				JSONObject object = invoiceArray.getJSONObject(counter);
				invoiceMap.put(object.optString("invoiceno", ""), Translator.doubleValue(object.opt("amount_amount")));
			}
		}
		return invoiceMap;

	}

	private void putInvoiceDetailsInListAndMap(List<String> invoiceNumbersList, Map<String, Double> salarySheetInvoiceDetails, JSONArray salarySheetArray, Map<String, String[]> employeeDetailsMap) throws Exception {
		for (int counter = 0; counter < salarySheetArray.length(); counter++) {
			JSONObject object = salarySheetArray.getJSONObject(counter);
			String invoiceNumber = object.optString("invoicenumber");
			double grossAmount = Translator.doubleValue(object.opt("gross_amount"));
			salarySheetInvoiceDetails.put(invoiceNumber, grossAmount);
			employeeDetailsMap.put(invoiceNumber, new String[] { object.optString("employeeid.name", ""), object.optString("employeeid.employeecode", "") });
			invoiceNumbersList.add(invoiceNumber);
		}

	}

	private JSONArray getSalarySheetArray(int yearId, int monthId, int branchId) throws JSONException {
		String extraFilter = "";
		if (branchId != 0) {
			extraFilter += " and employeeid.branchid = " + branchId;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("deductionamount");
		columnArray.put("payableamount");
		columnArray.put("invoicenumber");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "monthid = " + monthId + " AND yearid=" + yearId + extraFilter + " AND payableamount > 0");
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("salarysheet");
		if (salarySheetArray != null && salarySheetArray.length() > 0) {
			for (int counter = 0; counter < salarySheetArray.length(); counter++) {
				JSONObject object = salarySheetArray.getJSONObject(counter);
				object.put("gross_amount", (Translator.doubleValue(object.opt("payableamount")) + Translator.doubleValue(object.opt("deductionamount"))));
			}
		}
		return salarySheetArray;
	}

	private static void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, cellFormat);
		sheet.setColumnView(column, 20);
		sheet.addCell(label);
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
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + extraFilter + " AND joiningdate <= '" + lastDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + extraFilter + " and relievingdate >= '" + firstDateInString + "'");
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public JSONArray getPFReportArray(String key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "pfreportforyear");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("yearid.name");
		columnArray.put("branchid");
		columnArray.put("branchid.name");
		columnArray.put("yearid");
		columnArray.put("monthid");
		columnArray.put("monthid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray pfReportArray = employeeObject.getJSONArray("pfreportforyear");
		return pfReportArray;
	}
}
