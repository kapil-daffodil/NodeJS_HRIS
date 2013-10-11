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
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.HrisHelper;

public class EsiDeductionExcelReportServlet extends HttpServlet {
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
			String decompositionParameter = "attachment;filename=ESI_Deduction_Report.xls";
			resp.setHeader("Content-disposition", decompositionParameter);
			resp.setHeader("Content-Type", "application/octet-stream");

			WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 22, WritableFont.BOLD);
			WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
			headerFormat.setAlignment(Alignment.CENTRE);

			String key = req.getParameter("keys");

			if (key.length() > 0) {
				key = key.substring(1, key.length() - 1);
				JSONArray pfReportArray = getPFReportArray(key);
				if (pfReportArray != null && pfReportArray.length() > 0) {
					int monthId = Translator.integerValue(pfReportArray.getJSONObject(0).opt("monthid"));
					int yearId = Translator.integerValue(pfReportArray.getJSONObject(0).opt("yearid"));
					Object branchId = pfReportArray.getJSONObject(0).opt("branchid");
					String yearName = Translator.stringValue(pfReportArray.getJSONObject(0).opt("yearid.name"));
					String monthName = Translator.stringValue(pfReportArray.getJSONObject(0).opt("monthid.name"));

					WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
					WritableCellFormat cellFormat = new WritableCellFormat(font);
					cellFormat.setBackground(Colour.GRAY_25);
					cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
					WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
					WritableCellFormat cellFormat1 = new WritableCellFormat(font1);
					cellFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);

					WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
					WritableSheet sheet = null, sheet1 = null;

					JSONArray esiDeductionArray = getEmployeeMonthlySalaryComponentArray(yearId, monthId, branchId);
					sheet = workbook.createSheet("ESI Deduction", 0);
					sheet1 = workbook.createSheet("ESI Cycle Deduction", 1);

					int row = 2;
					int column = 0;

					int row1 = 2;
					int column1 = 0;
					sheet.mergeCells(column, row - 1, column + 10, row - 1);
					HrisHelper.setHeaderValue(column, row - 1, "Month: " + monthName + " - " + yearName, cellFormat, sheet);

					sheet1.mergeCells(column1, row1 - 1, column1 + 10, row1 - 1);
					HrisHelper.setHeaderValue(column, row - 1, "Month: " + monthName + " - " + yearName, cellFormat, sheet1);

					HrisHelper.setHeaderValue(column++, row, "Sr. No. ", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Code", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Name", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Month", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Year", cellFormat, sheet);
					HrisHelper.setHeaderValue(column++, row, "Amount", cellFormat, sheet);

					HrisHelper.setHeaderValue(column1++, row1, "Sr. No. ", cellFormat, sheet1);
					HrisHelper.setHeaderValue(column1++, row1, "Code", cellFormat, sheet1);
					HrisHelper.setHeaderValue(column1++, row1, "Name", cellFormat, sheet1);
					HrisHelper.setHeaderValue(column1++, row1, "Month", cellFormat, sheet1);
					HrisHelper.setHeaderValue(column1++, row1, "Year", cellFormat, sheet1);
					HrisHelper.setHeaderValue(column1++, row1, "Amount", cellFormat, sheet1);

					HashMap<Integer, Object[]> employeeEsiDetails = new HashMap<Integer, Object[]>();
					HashMap<Integer, Object[]> employeeEsiDetailsCycle = new HashMap<Integer, Object[]>();

					for (int counter = 0; counter < esiDeductionArray.length(); counter++) {

						int employeeId = Translator.integerValue(esiDeductionArray.getJSONObject(counter).opt("employeeid"));
						Object employeeName = esiDeductionArray.getJSONObject(counter).opt("employeeid.name");
						Object employeeCode = esiDeductionArray.getJSONObject(counter).opt("employeeid.employeecode");
						Object amount = esiDeductionArray.getJSONObject(counter).opt("amount");
						boolean isEsiCycle = Translator.booleanValue(esiDeductionArray.getJSONObject(counter).opt("is_esi_cycle"));

						Object[] details = new Object[3];
						details[0] = employeeName;
						details[1] = employeeCode;
						details[2] = amount;
						if (!isEsiCycle) {

							if (employeeEsiDetails.containsKey(employeeId)) {
								details[2] = Translator.doubleValue(amount) + Translator.doubleValue(employeeEsiDetails.get(employeeId)[2]);
							}
							employeeEsiDetails.put(employeeId, details);
						} else {
							if (employeeEsiDetailsCycle.containsKey(employeeId)) {
								details[2] = Translator.doubleValue(amount) + Translator.doubleValue(employeeEsiDetailsCycle.get(employeeId)[2]);
							}
							employeeEsiDetailsCycle.put(employeeId, details);
						}
					}
					int srNo = 0;
					int srNo1 = 0;
					for (Integer employeeId : employeeEsiDetails.keySet()) {
						row++;
						column = 0;
						srNo++;
						Object[] details = employeeEsiDetails.get(employeeId);
						putHeader(cellFormat1, sheet, srNo, row, column++);
						putHeader(cellFormat1, sheet, details[1], row, column++);
						putHeader(cellFormat1, sheet, details[0], row, column++);
						putHeader(cellFormat1, sheet, monthName, row, column++);
						putHeader(cellFormat1, sheet, yearName, row, column++);
						putHeader(cellFormat1, sheet, details[2], row, column++);

					}
					for (Integer employeeId : employeeEsiDetailsCycle.keySet()) {
						row1++;
						column1 = 0;
						srNo1++;
						Object[] details = employeeEsiDetailsCycle.get(employeeId);
						putHeader(cellFormat1, sheet1, srNo1, row, column++);
						putHeader(cellFormat1, sheet1, details[1], row, column++);
						putHeader(cellFormat1, sheet1, details[0], row, column++);
						putHeader(cellFormat1, sheet1, monthName, row, column++);
						putHeader(cellFormat1, sheet1, yearName, row, column++);
						putHeader(cellFormat1, sheet1, details[2], row, column++);

					}
					workbook.write();
					workbook.close();
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("EsiDeductionExcelReport >> calculateAndWriteToSheet Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "EsiDeductionExcelReport >> calculateAndWriteToSheet Exception Trace");
		}
	}

	private static void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, cellFormat);
		sheet.setColumnView(column, 20);
		sheet.addCell(label);
	}

	private JSONArray getPFReportArray(String key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "pfreportforyear");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("yearid");
		columnArray.put("yearid.name");
		columnArray.put("monthid");
		columnArray.put("monthid.name");
		columnArray.put("branchid");
		columnArray.put("branchid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray pfReportArray = employeeObject.getJSONArray("pfreportforyear");
		return pfReportArray;
	}

	public static JSONArray getEmployeeMonthlySalaryComponentArray(int yearId, int monthId, Object branchId) throws JSONException {
		String extraFilter = "";
		if (branchId != null) {
			extraFilter += " and branchid = " + branchId;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("amount");
		columnArray.put("is_esi_cycle");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarymonthid = " + monthId + " AND salaryyearid = " + yearId + " AND salarycomponentid.salarycomponenttypeid = " + HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE + extraFilter);
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("employeemonthlysalarycomponents");
		return employeeArray;
	}
}
