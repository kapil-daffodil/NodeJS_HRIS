package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class SalaryNetPayableLessAndEqual15000ButESINotDeducted extends HttpServlet {

	private static final long serialVersionUID = 1L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		createESINotDeductedExcelReport(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doGet(req, resp);
	}

	private void createESINotDeductedExcelReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Salary_Net_Payable_Less_And_Equal_15000_But_ESI_Not_Deducted.xls";
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
			List<Integer> employeeList = new ArrayList<Integer>();
			HashMap<Integer, String[]> employeeDetailMap = new HashMap<Integer, String[]>();
			putEmployeeList(employeeList, salarySheetArray, employeeDetailMap);
			int row = 2;
			int column = 0;
			int srNo = 1;
			HrisHelper.setHeaderValue(column++, row, "Sr. No.", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Employee Name", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Employee Code", cellFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "ESI Status", cellFormat, sheet);
			for (int counter = 0; counter < employeeList.size(); counter++) {
				int employeeid = employeeList.get(counter);
				boolean isEsiNotDeducted = getEsiNotDeducted(employeeid, monthId, yearId);
				if (isEsiNotDeducted) {
					row++;
					column = 0;
					String employeeName = employeeDetailMap.get(employeeid)[0];
					String employeeCode = employeeDetailMap.get(employeeid)[1];

					putHeader(cellFormat1, sheet, srNo++, row, column++);
					putHeader(cellFormat1, sheet, employeeName, row, column++);
					putHeader(cellFormat1, sheet, employeeCode, row, column++);
					putHeader(cellFormat1, sheet, "ESI Not Deducted", row, column++);
				}
			}
			workbook.write();
			workbook.close();
		} catch (Exception e) {
			LogUtility.writeError("SalaryNetPayableLessAndEqual15000ButESINotDeducted >> calculateAndWriteToSheet Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
		}
	}

	private void putEmployeeList(List<Integer> employeeList, JSONArray salarySheetArray, HashMap<Integer, String[]> employeeDetailMap) throws Exception {
		HashMap<Integer, Double> payableAmountMap = new HashMap<Integer, Double>();
		for (int counter = 0; counter < salarySheetArray.length(); counter++) {
			int employeeId = Translator.integerValue(salarySheetArray.getJSONObject(counter).opt("employeeid"));
			String employeeName = Translator.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.name"));
			String employeeCode = Translator.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.employeecode"));
			double payableAmount = Translator.doubleValue(salarySheetArray.getJSONObject(counter).opt("payableamount"));
			if (payableAmountMap.containsKey(employeeId)) {
				payableAmount += payableAmountMap.get(employeeId);
			}
			employeeDetailMap.put(employeeId, new String[] { employeeName, employeeCode });
			payableAmountMap.put(employeeId, payableAmount);
		}
		for (Integer employeeId : payableAmountMap.keySet()) {
			double payableAmount = payableAmountMap.get(employeeId);
			if (payableAmount > 0 && payableAmount <= 15000) {
				employeeList.add(employeeId);
			}
		}
		// for (Integer employeeId : employeeDetailMap.keySet()) {
		// if (!employeeList.contains(employeeId)) {
		// employeeDetailMap.remove(employeeId);
		// }
		// }
	}

	private boolean getEsiNotDeducted(int employeeid, int monthId, int yearId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("amount");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid=" + employeeid + " AND salarymonthid = " + monthId + " AND salaryyearid=" + yearId + " AND salarycomponentid.salarycomponenttypeid=" + HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE);
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("employeemonthlysalarycomponents");
		if (salarySheetArray != null && salarySheetArray.length() > 0) {
			return false;
		} else {
			return true;
		}
	}

	public void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, cellFormat);
		sheet.addCell(numobj);
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
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("deductionamount");
		columnArray.put("payableamount");
		columnArray.put("invoicenumber");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "monthid = " + monthId + " AND yearid=" + yearId + extraFilter);
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("salarysheet");
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
