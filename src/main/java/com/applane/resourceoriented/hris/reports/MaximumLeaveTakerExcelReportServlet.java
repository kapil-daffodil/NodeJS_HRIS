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
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.utility.HrisHelper;

public class MaximumLeaveTakerExcelReportServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			maxLeaveTakerListPutIntoSheet(req, resp);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(MaximumLeaveTakerExcelReportServlet.class.getName(), e);
			LogUtility.writeLog("MaximumLeaveTakerExcelReportServlet >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Error Occured Please Contact To Admin.");
		}
	}

	private void maxLeaveTakerListPutIntoSheet(HttpServletRequest req, HttpServletResponse resp) throws Exception {

		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Max_Leave_Taker.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");

		String key = req.getParameter("keys");
		if (key.length() > 2) {
			key = key.substring(1, key.length() - 1);
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
		}
	}

	private void workOnRecords(JSONArray reportRecord, WritableSheet sheet, WritableCellFormat feemergeFormat) throws Exception {
		int branchId = 0;
		Date fromDate = null;
		Date toDate = null;
		if (reportRecord != null && reportRecord.length() > 0) {
			branchId = Translator.integerValue(reportRecord.getJSONObject(0).opt("branchid"));
			fromDate = Translator.dateValue(reportRecord.getJSONObject(0).opt("fromdate"));
			toDate = Translator.dateValue(reportRecord.getJSONObject(0).opt("todate"));

			if (fromDate == null || toDate == null) {
				throw new BusinessLogicException("From Date and To Date is Mandatory Fields. for Maximum Leave Taken Report");
			}

			long diff = DataTypeUtilities.monthsBetweenDates(fromDate, toDate);
			Calendar cal = Calendar.getInstance();
			int monthId = 0;
			long yearIdLong = 0;
			int yearId = 0;
			HashMap<Integer, String> monthNamesMap = new HashMap<Integer, String>();
			HashMap<Integer, String> yearNamesMap = new HashMap<Integer, String>();
			HashMap<Integer, ArrayList<Integer>> yearAndMonths = new HashMap<Integer, ArrayList<Integer>>();
			HashMap<Integer, HashMap<Integer, HashMap<Integer, Double[]>>> employeeLeaveDetails = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Double[]>>>();
			HashMap<Integer, String[]> employeePersonalDetails = new HashMap<Integer, String[]>();
			for (int counter = 0; counter < diff; counter++) {
				cal.setTime(fromDate);
				cal.add(Calendar.MONTH, counter);
				monthId = cal.get(Calendar.MONTH) + 1;
				yearIdLong = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
				yearId = (int) yearIdLong;
				ArrayList<Integer> list = new ArrayList<Integer>();
				if (yearAndMonths.containsKey(yearId)) {
					list = yearAndMonths.get(yearId);
				}
				if (!list.contains(monthId)) {
					list.add(monthId);
					yearAndMonths.put(yearId, list);
				}
				if (monthId != 0 && yearId != 0) {
					JSONArray employeeMonthlyAttendance = getMonthlyLeaves(monthId, yearId, branchId);
					String monthName = EmployeeSalaryGenerationServlet.getMonthName(monthId);
					String yearName = EmployeeSalaryGenerationServlet.getYearName(yearId);
					if (!monthNamesMap.containsKey(monthId)) {
						monthNamesMap.put(monthId, monthName);
					}
					if (!yearNamesMap.containsKey(yearId)) {
						yearNamesMap.put(yearId, yearName);
					}
					for (int attendanceCounter = 0; attendanceCounter < employeeMonthlyAttendance.length(); attendanceCounter++) {
						HashMap<Integer, HashMap<Integer, Double[]>> yearDetails = new HashMap<Integer, HashMap<Integer, Double[]>>();
						HashMap<Integer, Double[]> monthLeaveDetails = new HashMap<Integer, Double[]>();
						int employeeId = Translator.integerValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("employeeid"));
						double totalLeaves = Translator.doubleValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("leaves"));
						double earnEarnedLeave = Translator.doubleValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("earnearnedleave"));
						double payableLeaves = Translator.doubleValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("payableleaves"));
						double nonPayableLeaves = Translator.doubleValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("nonpayableleaves"));
						double absents = Translator.doubleValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("absents"));
						String employeeName = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("employeeid.name"));
						String employeeCode = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("employeeid.employeecode"));

						if (!employeePersonalDetails.containsKey(employeeId)) {
							String[] details = { employeeCode, employeeName };
							employeePersonalDetails.put(employeeId, details);
						}
						if (employeeLeaveDetails.containsKey(employeeId)) {
							yearDetails = employeeLeaveDetails.get(employeeId);
							if (yearDetails.containsKey(yearId)) {
								monthLeaveDetails = yearDetails.get(yearId);
							}
						}
						if (!monthLeaveDetails.containsKey(monthId)) {
							Double[] leaveDetails = new Double[5];
							leaveDetails[0] = totalLeaves;
							leaveDetails[1] = earnEarnedLeave;
							leaveDetails[2] = payableLeaves;
							leaveDetails[3] = nonPayableLeaves;
							leaveDetails[4] = absents;
							monthLeaveDetails.put(monthId, leaveDetails);
							yearDetails.put(yearId, monthLeaveDetails);
							employeeLeaveDetails.put(employeeId, yearDetails);
						}
					}
				}
			}
			int row = 4;
			int column = 0;
			HrisHelper.setHeaderValue(column++, row, "Sr. No.", feemergeFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Employee Code", feemergeFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Employee Name", feemergeFormat, sheet);
			for (Integer yearIdKey : yearAndMonths.keySet()) {
				ArrayList<Integer> list = yearAndMonths.get(yearIdKey);
				String yearName = yearNamesMap.get(yearIdKey);
				for (int monthCounter = 0; monthCounter < list.size(); monthCounter++) {
					String monthName = monthNamesMap.get(list.get(monthCounter));
					// sheet.mergeCells(column, row, column + 3, row);
					HrisHelper.setHeaderValue(column++, row, monthName + " - " + yearName, feemergeFormat, sheet);
					// HrisHelper.setHeaderValue(column, row + 1, "Total Leaves", feemergeFormat, sheet);
					// HrisHelper.setHeaderValue(column + 1, row + 1, "Earn Leaves", feemergeFormat, sheet);
					// HrisHelper.setHeaderValue(column + 2, row + 1, "Payable", feemergeFormat, sheet);
					// HrisHelper.setHeaderValue(column + 3, row + 1, "Non Payable", feemergeFormat, sheet);
				}
			}
			HrisHelper.setHeaderValue(column++, row, "Total", feemergeFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Earned", feemergeFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Payable", feemergeFormat, sheet);
			HrisHelper.setHeaderValue(column++, row, "Non Payable", feemergeFormat, sheet);
			row++;
			int srNo = 0;
			for (Integer employeeID : employeeLeaveDetails.keySet()) {
				double totalLeaves = 0.0;
				double totalEarnLeaves = 0.0;
				double totalPayableLeaves = 0.0;
				double totalNonPayableLeaves = 0.0;
				column = 0;
				String[] employeeDetails = employeePersonalDetails.get(employeeID);
				if (employeeDetails != null && employeeDetails.length > 0) {
					srNo++;
					putHeader(sheet, srNo, row, column++);
					putHeader(sheet, employeeDetails[0], row, column++);
					putHeader(sheet, employeeDetails[1], row, column++);
					HashMap<Integer, HashMap<Integer, Double[]>> yearDetails = employeeLeaveDetails.get(employeeID);
					for (Integer yearIdKey : yearAndMonths.keySet()) {
						HashMap<Integer, Double[]> monthLeaveDetails = yearDetails.get(yearIdKey);
						ArrayList<Integer> list = yearAndMonths.get(yearIdKey);
						if (monthLeaveDetails != null) {
							for (int monthCounter = 0; monthCounter < list.size(); monthCounter++) {
								Double[] leaves = monthLeaveDetails.get(list.get(monthCounter));
								if (leaves != null) {
									totalLeaves += (Translator.doubleValue(leaves[0]) + Translator.doubleValue(leaves[4]));
									totalEarnLeaves += Translator.doubleValue(leaves[1]);
									totalPayableLeaves += Translator.doubleValue(leaves[2]);
									totalNonPayableLeaves += Translator.doubleValue(leaves[3]);
									putHeader(sheet, Translator.doubleValue(leaves[0]), row, column++);

								} else {
									putHeader(sheet, "", row, column++);
								}
							}
						} else {
							for (int monthCounter = 0; monthCounter < list.size(); monthCounter++) {
								putHeader(sheet, "", row, column++);
							}
						}

					}
					putHeader(sheet, totalLeaves, row, column++);
					putHeader(sheet, totalEarnLeaves, row, column++);
					putHeader(sheet, totalPayableLeaves, row, column++);
					putHeader(sheet, totalNonPayableLeaves, row, column++);
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

	private JSONArray getMonthlyLeaves(int monthId, long yearId, int branchId) throws JSONException {
		String extraFilter = "";
		if (branchId != 0) {
			extraFilter += " AND employeeid.branchid=" + branchId;
		}
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("leaves");
		columnArray.put("earnearnedleave");
		columnArray.put("payableleaves");
		columnArray.put("nonpayableleaves");
		columnArray.put("absents");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "yearid = " + yearId + " and monthid = " + monthId + extraFilter);// + " and leaves > 0");
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeemonthlyattendance");
	}
}