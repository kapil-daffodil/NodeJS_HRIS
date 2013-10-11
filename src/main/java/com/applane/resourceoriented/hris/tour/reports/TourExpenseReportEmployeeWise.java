package com.applane.resourceoriented.hris.tour.reports;

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
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;

public class TourExpenseReportEmployeeWise extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			resp.setContentType("application/vnd.ms-excel");
			String decompositionParameter = "attachment;filename=Tour_Expense_Report.xls";
			resp.setHeader("Content-disposition", decompositionParameter);
			resp.setHeader("Content-Type", "application/octet-stream");
			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			String key = req.getParameter("keys");
			generateEmployeePerformanceReport(workbook, key);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("TourExpenseExcelReport trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured Please Contact To Admin.");
		}
	}

	private void generateEmployeePerformanceReport(WritableWorkbook workbook, String key) throws Exception {
		if (key.length() > 2) {
			WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 12, WritableFont.NO_BOLD);
			WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
			headerFormat.setAlignment(Alignment.CENTRE);

			WritableFont feemergefont = new WritableFont(WritableFont.ARIAL, 13, WritableFont.BOLD);
			WritableCellFormat feemergeFormat = new WritableCellFormat(feemergefont);
			feemergeFormat.setAlignment(Alignment.CENTRE);
			feemergeFormat.setBackground(Colour.GRAY_25);
			feemergeFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
			WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
			numbercellformat.setFont(font);
			numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);

			key = key.substring(1, key.length() - 1);

			WritableSheet sheet = workbook.createSheet("Report For Selected Employees", 0);

			JSONArray employeeTourRecords = getEmployeeTourRecords(key);

			HashMap<Integer, HashMap<Integer, Object[]>> employeeTourDetailsMap = new HashMap<Integer, HashMap<Integer, Object[]>>();
			HashMap<Integer, HashMap<Integer, Object[]>> employeeTourRequestAndDetailsMap = new HashMap<Integer, HashMap<Integer, Object[]>>();
			putRecordsIntoMap(employeeTourDetailsMap, employeeTourRecords, employeeTourRequestAndDetailsMap);
			int row = 5;
			int column = 0;
			sheet.mergeCells(column + 2, row - 4, column + 8, row - 4);

			putHeaderDefault(feemergeFormat, sheet, "Employee Tour Expense Sheet For Selected Employees", row - 4, column + 2);

			putHeaderDefault(feemergeFormat, sheet, "S. No.", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "Employee Code", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "Employee Name", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "Tour Code", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "Advance", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "City", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "From Date", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "To Date", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "Expected Amount", row, column++);
			putHeaderDefault(feemergeFormat, sheet, "Actual Amount", row, column++);

			int serNo = 0;
			for (Integer employeeId : employeeTourRequestAndDetailsMap.keySet()) {
				HashMap<Integer, Object[]> tourRequestDetailsMap = employeeTourRequestAndDetailsMap.get(employeeId);
				for (Integer tourId : tourRequestDetailsMap.keySet()) {
					Object[] details = tourRequestDetailsMap.get(tourId);
					if (details != null) {
						column = 0;
						serNo++;
						row++;
						// int tourId = Translator.integerValue(details[0]);
						Object employeeName = details[1];
						Object employeeCode = details[2];
						Object advance = details[3];
						Object departOn = details[4];
						Object ariveOn = details[5];
						Object tourCode = details[6];
						Object totalEstimatedAmount = details[7];
						Object totalActualAmount = details[8];
						putHeaderDefault(headerFormat, sheet, serNo, row, column++);
						putHeaderDefault(headerFormat, sheet, employeeCode, row, column++);
						putHeaderDefault(headerFormat, sheet, employeeName, row, column++);
						putHeaderDefault(headerFormat, sheet, tourCode, row, column++);
						putHeader(numbercellformat, sheet, Translator.doubleValue(advance), row, column++);
						putHeaderDefault(headerFormat, sheet, "", row, column++);
						putHeaderDefault(headerFormat, sheet, departOn, row, column++);
						putHeaderDefault(headerFormat, sheet, ariveOn, row, column++);
						putHeaderDefault(headerFormat, sheet, "", row, column++);
						putHeaderDefault(headerFormat, sheet, "", row, column++);

						HashMap<Integer, Object[]> tourDetailsMap = employeeTourDetailsMap.get(tourId);
						if (tourDetailsMap != null) {
							for (Integer tourDetailKeys : tourDetailsMap.keySet()) {
								details = tourDetailsMap.get(tourDetailKeys);
								// details[0] = fromDate;
								// details[1] = toDate;
								// details[2] = estmateAmount;
								// details[3] = actualAmount;
								// details[4] = cityName;
								column = 5;
								row++;
								putHeaderDefault(headerFormat, sheet, details[4], row, column++);
								putHeaderDefault(headerFormat, sheet, details[0], row, column++);
								putHeaderDefault(headerFormat, sheet, details[1], row, column++);
								putHeader(numbercellformat, sheet, Translator.doubleValue(details[2]), row, column++);
								putHeader(numbercellformat, sheet, Translator.doubleValue(details[3]), row, column++);
							}
							column = 7;
							row++;
							putHeaderDefault(headerFormat, sheet, "Total", row, column++);
							putHeader(numbercellformat, sheet, Translator.doubleValue(totalEstimatedAmount), row, column++);
							putHeader(numbercellformat, sheet, Translator.doubleValue(totalActualAmount), row, column++);
						}
					}
				}
			}
			workbook.write();
			workbook.close();

		}
	}

	public void putHeaderDefault(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		sheet.setColumnView(column, 20);
		Label label = new Label(column, row, "" + value, cellFormat);
		sheet.addCell(label);
	}

	public void putHeader(WritableCellFormat numbercellformat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, numbercellformat);
		sheet.addCell(numobj);
	}

	private void putRecordsIntoMap(HashMap<Integer, HashMap<Integer, Object[]>> employeeTourDetailsMap, JSONArray employeeTourRequestRecords, HashMap<Integer, HashMap<Integer, Object[]>> employeeTourRequestAndDetailsMap) throws JSONException {
		for (int counter = 0; counter < employeeTourRequestRecords.length(); counter++) {
			int tourId = Translator.integerValue(employeeTourRequestRecords.getJSONObject(counter).opt(Updates.KEY));
			int employeeId = Translator.integerValue(employeeTourRequestRecords.getJSONObject(counter).opt("employeeid"));
			String employeeCode = Translator.stringValue(employeeTourRequestRecords.getJSONObject(counter).opt("employeeid.employeecode"));
			String employeeName = Translator.stringValue(employeeTourRequestRecords.getJSONObject(counter).opt("employeeid.name"));

			Object departOn = employeeTourRequestRecords.getJSONObject(counter).opt("startdate");
			Object ariveOn = employeeTourRequestRecords.getJSONObject(counter).opt("enddate");

			Object advance = employeeTourRequestRecords.getJSONObject(counter).opt("advanceamount_amount");
			Object tourCode = employeeTourRequestRecords.getJSONObject(counter).opt("tourcode");

			Object employeeDetailsObject = employeeTourRequestRecords.getJSONObject(counter).opt("hris_tourdetails");

			if (departOn != null) {
				String fromDateString = "" + departOn;
				if (fromDateString.contains(" ")) {
					departOn = fromDateString.split(" ")[0];
				}
			}
			if (ariveOn != null) {
				String toDateString = "" + ariveOn;
				if (toDateString.contains(" ")) {
					ariveOn = toDateString.split(" ")[0];
				}
			}

			if (employeeId != 0) {
				Object[] details = new Object[9];
				details[0] = tourId;
				details[1] = employeeName;
				details[2] = employeeCode;
				details[3] = advance;
				details[4] = departOn;
				details[5] = ariveOn;
				details[6] = tourCode;
				HashMap<Integer, Object[]> tourRequestDetails = new HashMap<Integer, Object[]>();
				if (employeeTourRequestAndDetailsMap.containsKey(employeeId)) {
					tourRequestDetails = employeeTourRequestAndDetailsMap.get(employeeId);
				}
				tourRequestDetails.put(tourId, details);
				employeeTourRequestAndDetailsMap.put(employeeId, tourRequestDetails);

				HashMap<Integer, Object[]> tourDetails = new HashMap<Integer, Object[]>();
				JSONArray employeeRecords = new JSONArray();
				if (employeeDetailsObject != null && employeeDetailsObject instanceof JSONArray) {
					employeeRecords = (JSONArray) employeeDetailsObject;
				} else if (employeeDetailsObject != null && employeeDetailsObject instanceof JSONObject) {
					employeeRecords = new JSONArray().put(employeeDetailsObject);
				}
				if (employeeRecords != null && employeeRecords.length() > 0) {
					double totalEstimatedAmount = 0.0;
					double totalActualAmount = 0.0;
					for (int tourDetailCounter = 0; tourDetailCounter < employeeRecords.length(); tourDetailCounter++) {
						Object[] nestedTourDetails = new Object[5];
						int tourDatailId = Translator.integerValue(employeeRecords.getJSONObject(tourDetailCounter).opt(Updates.KEY));
						Object fromDate = employeeRecords.getJSONObject(tourDetailCounter).opt("fromdate");
						Object toDate = employeeRecords.getJSONObject(tourDetailCounter).opt("todate");

						double estmateAmount = Translator.doubleValue(employeeRecords.getJSONObject(tourDetailCounter).opt("expectedamount_amount"));
						double actualAmount = Translator.doubleValue(employeeRecords.getJSONObject(tourDetailCounter).opt("actual_amount_amount"));
						Object cityName = employeeRecords.getJSONObject(tourDetailCounter).opt("cityid.name");
						totalEstimatedAmount += estmateAmount;
						totalActualAmount += actualAmount;
						if (fromDate != null) {
							String fromDateString = "" + fromDate;
							if (fromDateString.contains(" ")) {
								fromDate = fromDateString.split(" ")[0];
							}
						}
						if (toDate != null) {
							String toDateString = "" + toDate;
							if (toDateString.contains(" ")) {
								toDate = toDateString.split(" ")[0];
							}
						}
						nestedTourDetails[0] = fromDate;
						nestedTourDetails[1] = toDate;
						nestedTourDetails[2] = estmateAmount;
						nestedTourDetails[3] = actualAmount;
						nestedTourDetails[4] = cityName;
						tourDetails.put(tourDatailId, nestedTourDetails);
					}
					details[7] = totalEstimatedAmount;
					details[8] = totalActualAmount;
				}
				employeeTourDetailsMap.put(tourId, tourDetails);
			}
		}
	}

	private JSONArray getEmployeeTourRecords(String selectedKey) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_tourrequests");
		columnArray.put(Updates.KEY);

		JSONObject innerObject = new JSONObject();
		JSONObject innerQuery = new JSONObject();
		JSONArray innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("cityid.name");
		innerColumns.put("fromdate");
		innerColumns.put("todate");
		innerColumns.put("expectedamount_amount");
		innerColumns.put("actual_amount_amount");

		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_tourdetails");

		innerObject.put("relatedcolumn", "tourrequestid");
		innerObject.put("alias", "hris_tourdetails");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);

		query.put(Data.Query.CHILDS, new JSONArray().put(innerObject));

		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("startdate");
		columnArray.put("enddate");
		columnArray.put("tourcode");
		columnArray.put("advanceamount_amount");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " IN(" + selectedKey + ")");
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_tourrequests");

		return rows;
	}

}
