package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.utility.HrisHelper;

public class LeaveBalanceReportBusinessLogicServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=LeaveBalance.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");

		String key = req.getParameter("keys");
		if (key.length() > 2) {
			key = key.substring(1, key.length() - 1);
			try {
				JSONArray reportRecord = new PFAnnualReturnReportForm6ARevised().getPFReportArray(key);
				if (reportRecord != null) {
					WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
					WritableSheet sheet = workbook.createSheet("First Sheet", 0);

					WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 22, WritableFont.BOLD);
					WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
					headerFormat.setAlignment(Alignment.CENTRE);

					WritableFont feemergefont = new WritableFont(WritableFont.ARIAL, 13, WritableFont.BOLD);
					WritableCellFormat feemergeFormat = new WritableCellFormat(feemergefont);
					feemergeFormat.setAlignment(Alignment.CENTRE);
					feemergeFormat.setBackground(Colour.GRAY_25);
					feemergeFormat.setBorder(Border.ALL, BorderLineStyle.THIN);

					workOnRecords(reportRecord, sheet, feemergeFormat);
					workbook.write();
					workbook.close();
				}
			} catch (BusinessLogicException e) {
				throw new BusinessLogicException(e.getMessage());
			} catch (Exception e) {
				String trace = ExceptionUtils.getExceptionTraceMessage(LeaveBalanceReportBusinessLogicServlet.class.getName(), e);
				LogUtility.writeLog("LeaveBalanceReportBusinessLogicServlet >> trace >> " + trace);
				throw new BusinessLogicException("Some unknown error occured. Please contace to Admin");
			}
		}
	}

	private void workOnRecords(JSONArray reportRecord, WritableSheet sheet, WritableCellFormat feemergeFormat) throws Exception {

		int branchId = 0;
		Date fromDate = null;
		Date toDate = null;
		// String firstDateInString = "";
		// String lastDateInString = "";

		if (reportRecord != null && reportRecord.length() > 0) {
			branchId = Translator.integerValue(reportRecord.getJSONObject(0).opt("branchid"));
			fromDate = Translator.dateValue(reportRecord.getJSONObject(0).opt("fromdate"));
			toDate = Translator.dateValue(reportRecord.getJSONObject(0).opt("todate"));

			if (branchId == 0 || fromDate == null || toDate == null) {
				throw new BusinessLogicException("Branch, From Date and To Date is Mandatory Fields. for Leave Balance Report");
			}

			// firstDateInString = "" + reportRecord.getJSONObject(0).opt("fromdate");
			// lastDateInString = "" + reportRecord.getJSONObject(0).opt("todate");

			long diff = DataTypeUtilities.monthsBetweenDates(fromDate, toDate);
			Calendar cal = Calendar.getInstance();
			String monthIds = "";
			String yearIds = "";
			ArrayList<Integer> yearList = new ArrayList<Integer>();
			HashMap<Integer, HashMap<Integer, String>> mapToGetAndPutIntoSheet = new HashMap<Integer, HashMap<Integer, String>>();
			for (int dateDifferenceCounter = 0; dateDifferenceCounter < diff; dateDifferenceCounter++) {
				cal.setTime(fromDate);
				cal.add(Calendar.MONTH, dateDifferenceCounter);
				int monthId = cal.get(Calendar.MONTH) + 1;
				long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
				int yearIdInt = (int) yearId;
				HashMap<Integer, String> monthsList = new HashMap<Integer, String>();
				if (mapToGetAndPutIntoSheet.containsKey(yearIdInt)) {
					monthsList = mapToGetAndPutIntoSheet.get(yearIdInt);
				}
				monthsList.put(monthId, EmployeeSalaryGenerationServlet.getMonthName(monthId));
				mapToGetAndPutIntoSheet.put(yearIdInt, monthsList);
				if (monthIds.length() > 0) {
					monthIds += ", ";
				}
				monthIds += monthId;
				if (!yearList.contains(yearIdInt)) {
					yearList.add(yearIdInt);
					if (yearIds.length() > 0) {
						yearIds += ", ";
					}
					yearIds += yearIdInt;
				}
			}
			HashMap<Integer, String> employeeNameMap = new HashMap<Integer, String>();
			HashMap<Integer, HashMap<Integer, HashMap<Integer, Double[]>>> leaveRegisterDetail = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Double[]>>>();
			if (monthIds.length() > 0 && yearIds.length() > 0) {
				JSONArray employeeArray = getgetEmployeeRecords(branchId);
				HashMap<Integer, String[]> employeeDetails = new HashMap<Integer, String[]>();
				for (int employeeCounter = 0; employeeCounter < employeeArray.length(); employeeCounter++) {
					int employeeId = Translator.integerValue(employeeArray.getJSONObject(employeeCounter).opt(Updates.KEY));
					String employeeName = Translator.stringValue(employeeArray.getJSONObject(employeeCounter).opt("name"));
					String employeeCode = Translator.stringValue(employeeArray.getJSONObject(employeeCounter).opt("employeecode"));
					JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, yearIds, monthIds);
					if (!employeeDetails.containsKey(employeeId)) {
						String[] details = new String[2];
						details[0] = employeeName;
						details[1] = employeeCode;
						employeeDetails.put(employeeId, details);
					}
					if (!employeeNameMap.containsKey(employeeId)) {
						employeeNameMap.put(employeeId, employeeName);
					}
					for (int leaveRegisterCounter = 0; leaveRegisterCounter < leaveRegisterArray.length(); leaveRegisterCounter++) {
						Object openingBalance = leaveRegisterArray.getJSONObject(leaveRegisterCounter).opt("openingbalance");
						Object leaveBalance = leaveRegisterArray.getJSONObject(leaveRegisterCounter).opt("leavebalance");
						int lrMonthId = leaveRegisterArray.getJSONObject(leaveRegisterCounter).optInt("monthid");
						int lrYearId = leaveRegisterArray.getJSONObject(leaveRegisterCounter).optInt("yearid");
						HashMap<Integer, HashMap<Integer, Double[]>> yearDetails = new HashMap<Integer, HashMap<Integer, Double[]>>();
						HashMap<Integer, Double[]> monthDetails = new HashMap<Integer, Double[]>();
						if (leaveRegisterDetail.containsKey(employeeId)) {
							yearDetails = leaveRegisterDetail.get(employeeId);
							if (yearDetails.containsKey(lrYearId)) {
								monthDetails = yearDetails.get(lrYearId);
							}
						}
						Double[] openingAndBalanceLeaves = new Double[2];
						if (openingBalance != null) {
							openingAndBalanceLeaves[0] = Translator.doubleValue(openingBalance);
						}
						if (leaveBalance != null) {
							openingAndBalanceLeaves[1] = Translator.doubleValue(leaveBalance);
						}
						monthDetails.put(lrMonthId, openingAndBalanceLeaves);
						yearDetails.put(lrYearId, monthDetails);
						leaveRegisterDetail.put(employeeId, yearDetails);
					}
				}
				int row = 4;
				int column = 0;
				// add sr. no
				// add emp code
				// add emp name

				HrisHelper.setHeaderValue(column++, row, "Sr. No.", feemergeFormat, sheet);
				HrisHelper.setHeaderValue(column++, row, "Employee Code", feemergeFormat, sheet);
				HrisHelper.setHeaderValue(column++, row, "Employee Name", feemergeFormat, sheet);

				for (Integer yearId : mapToGetAndPutIntoSheet.keySet()) {
					HashMap<Integer, String> monthsList = mapToGetAndPutIntoSheet.get(yearId);
					for (Integer monthId : monthsList.keySet()) {
						String monthName = monthsList.get(monthId);
						sheet.mergeCells(column, row - 1, column + 1, row - 1);

						HrisHelper.setHeaderValue(column, row - 1, EmployeeReleasingSalaryServlet.getYearName(yearId) + " - " + monthName, feemergeFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "Opening Balance", feemergeFormat, sheet);
						HrisHelper.setHeaderValue(column++, row, "Closing Balance", feemergeFormat, sheet);
					}
				}
				row++;
				int srNo = 0;
				for (Integer employeeId : leaveRegisterDetail.keySet()) {
					srNo++;
					column = 0;
					String[] details = employeeDetails.get(employeeId);
					putHeader(sheet, srNo, row, column++);
					putHeader(sheet, details[1], row, column++); // Employee Code
					putHeader(sheet, details[0], row, column++); // Employee Name

					HashMap<Integer, HashMap<Integer, Double[]>> yearDetails = leaveRegisterDetail.get(employeeId);
					if (yearDetails != null) {
						for (Integer yearId : mapToGetAndPutIntoSheet.keySet()) {
							HashMap<Integer, String> monthsList = mapToGetAndPutIntoSheet.get(yearId);
							HashMap<Integer, Double[]> monthDetails = yearDetails.get(yearId);
							if (monthDetails != null) {
								for (Integer monthId : monthsList.keySet()) {
									Double[] openingAndBalanceLeaves = monthDetails.get(monthId);
									if (openingAndBalanceLeaves != null) {
										double openingBalance = openingAndBalanceLeaves[0] == null ? 0d : openingAndBalanceLeaves[0];
										double leaveBalance = openingAndBalanceLeaves[1] == null ? 0d : openingAndBalanceLeaves[1];
										putHeader(sheet, openingBalance, row, column++);
										putHeader(sheet, leaveBalance, row, column++);
									} else {
										putHeader(sheet, "", row, column++);
										putHeader(sheet, "", row, column++);
									}
								}
							}
						}
					}
					row++;
				}
			}
		}
	}

	private void putHeader(WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {

		Label label = new Label(column, row, "" + value);
		sheet.setColumnView(column, 20);
		sheet.addCell(label);
	}

	private JSONArray getLeaveRegisterArray(Object employeeId, String yearIds, String monthIds) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "leaveregister");
		columnArray.put(Updates.KEY);
		columnArray.put("openingbalance");
		columnArray.put("leavebalance");
		columnArray.put("monthid");
		columnArray.put("yearid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and yearid in (" + yearIds + ") and monthid in (" + monthIds + ")");
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("leaveregister");
	}

	private JSONArray getgetEmployeeRecords(int branchId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		columnArray.put(Updates.KEY);
		columnArray.put("name");
		columnArray.put("employeecode");
		query.put(Data.Query.COLUMNS, columnArray);
		// query.put(Data.Query.MAX_ROWS, 4);
		query.put(Data.Query.FILTERS, "branchid = " + branchId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
	}
}
