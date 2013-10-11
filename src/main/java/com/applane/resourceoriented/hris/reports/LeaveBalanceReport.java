package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.util.HashMap;

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

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;

public class LeaveBalanceReport extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// private static final Object JSONArray = null;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		leaveBalanceExcelReport(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doGet(req, resp);
	}

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	private void leaveBalanceExcelReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Leave_Balance.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");

		try {
			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			WritableSheet sheet = workbook.createSheet("Leave Balance", 0);

			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
			WritableCellFormat hdFormat = new WritableCellFormat(font);
			hdFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
			hdFormat.setAlignment(Alignment.CENTRE);

			WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
			numbercellformat.setFont(font);
			numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);
			String organizationName = "";
			String organizationAddress = "";
			// String organizationLogo = "";
			{

				JSONArray leaveBalanceArray = getLeaveBalanceRecords();
				JSONArray configurationSetup = getConfigurationSetup();
				if (configurationSetup != null && configurationSetup.length() > 0) {
					organizationName = configurationSetup.getJSONObject(0).optString("organizationname");
					organizationAddress = configurationSetup.getJSONObject(0).optString("address");
					// organizationLogo = getLogo(configurationSetup.getJSONObject(0).optString("organizationlogo"));
				}

				WritableFont font1 = new WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD);
				WritableCellFormat hdFormat1 = new WritableCellFormat(font1);

				hdFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
				hdFormat1.setAlignment(Alignment.CENTRE);

				int row = 6;
				int column = 0;

				// sheet.mergeCells(column + 4, row - 6, column + 10, row - 4);
				// setValueInSheet(sheet, font1, hdFormat1, row - 6, column + 4, organizationName);
				//
				// sheet.mergeCells(column + 4, row - 3, column + 10, row - 1);
				// setValueInSheet(sheet, font, hdFormat, row - 3, column + 4, organizationAddress);

				sheet.setColumnView(column, 8);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Sr. No.");
				sheet.setColumnView(column, 25);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Employee Name");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Employee Code");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Department");

				sheet.mergeCells(column, row, column + 7, row);
				setValueInSheet(sheet, font, hdFormat, row, column, "October");

				sheet.mergeCells(column, row + 1, column + 3, row + 1);
				setValueInSheet(sheet, font, hdFormat, row + 1, column, "PL");

				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Opening Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Closing Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Credit");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Debit");

				sheet.mergeCells(column, row + 1, column + 3, row + 1);
				setValueInSheet(sheet, font, hdFormat, row + 1, column, "Casual Leave");

				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Opening Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Closing Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Credit");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Debit");

				sheet.mergeCells(column, row, column + 7, row);
				setValueInSheet(sheet, font, hdFormat, row, column, "November");

				sheet.mergeCells(column, row + 1, column + 3, row + 1);
				setValueInSheet(sheet, font, hdFormat, row + 1, column, "PL");

				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Opening Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Closing Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Credit");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Debit");

				sheet.mergeCells(column, row + 1, column + 3, row + 1);
				setValueInSheet(sheet, font, hdFormat, row + 1, column, "Casual Leave");

				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Opening Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Closing Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Credit");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Debit");

				sheet.mergeCells(column, row, column + 7, row);
				setValueInSheet(sheet, font, hdFormat, row, column, "December");

				sheet.mergeCells(column, row + 1, column + 3, row + 1);
				setValueInSheet(sheet, font, hdFormat, row + 1, column, "PL");

				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Opening Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Closing Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Credit");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Debit");

				sheet.mergeCells(column, row + 1, column + 3, row + 1);
				setValueInSheet(sheet, font, hdFormat, row + 1, column, "Casual Leav");

				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Opening Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Closing Balance");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Credit");
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row + 2, column++, "Debit");

				row += 3;
				column = 0;

				if (leaveBalanceArray.length() > 0) {
					HashMap<Integer, String[]> employeeDetailMap = new HashMap<Integer, String[]>();
					HashMap<Integer, HashMap<Integer, HashMap<Integer, Double[]>>> employLeaveBalanceDetails = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Double[]>>>();
					for (int counter = 0; counter < leaveBalanceArray.length(); counter++) {
						HashMap<Integer, HashMap<Integer, Double[]>> monthDetail = new HashMap<Integer, HashMap<Integer, Double[]>>();
						HashMap<Integer, Double[]> leaveTypeDetail = new HashMap<Integer, Double[]>();
						Double[] leaveDetails = new Double[4];

						int employeeId = leaveBalanceArray.getJSONObject(counter).optInt("employeeid");
						String employeeName = DataTypeUtilities.stringValue(leaveBalanceArray.getJSONObject(counter).opt("employeeid.name"));
						String employeeCode = DataTypeUtilities.stringValue(leaveBalanceArray.getJSONObject(counter).opt("employeeid.employeecode"));
						String departmentName = DataTypeUtilities.stringValue(leaveBalanceArray.getJSONObject(counter).opt("employeeid.departmentid.name"));

						double openingbalance = DataTypeUtilities.doubleValue(leaveBalanceArray.getJSONObject(counter).opt("openingbalance"));
						double closingbalance = DataTypeUtilities.doubleValue(leaveBalanceArray.getJSONObject(counter).opt("closingbalance"));
						double credit = DataTypeUtilities.doubleValue(leaveBalanceArray.getJSONObject(counter).opt("credit"));
						double debit = DataTypeUtilities.doubleValue(leaveBalanceArray.getJSONObject(counter).opt("debit"));

						leaveDetails[0] = openingbalance;
						leaveDetails[1] = closingbalance;
						leaveDetails[2] = credit;
						leaveDetails[3] = debit;

						int monthId = DataTypeUtilities.integerValue(leaveBalanceArray.getJSONObject(counter).opt("monthid"));
						int leaveTypeId = DataTypeUtilities.integerValue(leaveBalanceArray.getJSONObject(counter).opt("leavetypeid"));
						if (!employeeDetailMap.containsKey(employeeId)) {
							employeeDetailMap.put(employeeId, new String[] { employeeCode, employeeName, departmentName });
						}
						if (employLeaveBalanceDetails.containsKey(employeeId)) {
							monthDetail = employLeaveBalanceDetails.get(employeeId);
							if (monthDetail.containsKey(monthId)) {
								leaveTypeDetail = monthDetail.get(monthId);
							}
						}
						leaveTypeDetail.put(leaveTypeId, leaveDetails);
						monthDetail.put(monthId, leaveTypeDetail);
						employLeaveBalanceDetails.put(employeeId, monthDetail);
					}
					int counter = 0;
					for (Integer employeeId : employLeaveBalanceDetails.keySet()) {
						column = 0;
						String[] employeeDetails = employeeDetailMap.get(employeeId);
						if (employeeDetails != null) {
							row++;
							counter++;
							putHeaderDefault(numbercellformat, sheet, counter, row, 0);
							putHeaderDefault(numbercellformat, sheet, employeeDetails[0], row, column + 1);
							putHeaderDefault(numbercellformat, sheet, employeeDetails[1], row, column + 2);
							putHeaderDefault(numbercellformat, sheet, employeeDetails[2], row, column + 3);
							column++;
							HashMap<Integer, HashMap<Integer, Double[]>> monthDetail = employLeaveBalanceDetails.get(employeeId);
							if (monthDetail != null) {
								for (Integer monthId : monthDetail.keySet()) {
									HashMap<Integer, Double[]> leaveTypeDetail = monthDetail.get(monthId);
									if (leaveTypeDetail != null) {
										for (Integer leaveTypeId : leaveTypeDetail.keySet()) {
											Double[] leaveDetail = leaveTypeDetail.get(leaveTypeId);
											if (monthId == 10) {
												if (leaveTypeId == 2) {
													putHeader(numbercellformat, sheet, leaveDetail[0], row, column + 3);
													putHeader(numbercellformat, sheet, leaveDetail[1], row, column + 4);
													putHeader(numbercellformat, sheet, leaveDetail[2], row, column + 5);
													putHeader(numbercellformat, sheet, leaveDetail[3], row, column + 6);
												}
												if (leaveTypeId == 1) {
													putHeader(numbercellformat, sheet, leaveDetail[0], row, column + 7);
													putHeader(numbercellformat, sheet, leaveDetail[1], row, column + 8);
													putHeader(numbercellformat, sheet, leaveDetail[2], row, column + 9);
													putHeader(numbercellformat, sheet, leaveDetail[3], row, column + 10);
												}
											}
											if (monthId == 11) {
												if (leaveTypeId == 2) {
													putHeader(numbercellformat, sheet, leaveDetail[0], row, column + 11);
													putHeader(numbercellformat, sheet, leaveDetail[1], row, column + 12);
													putHeader(numbercellformat, sheet, leaveDetail[2], row, column + 13);
													putHeader(numbercellformat, sheet, leaveDetail[3], row, column + 14);
												}
												if (leaveTypeId == 1) {
													putHeader(numbercellformat, sheet, leaveDetail[0], row, column + 15);
													putHeader(numbercellformat, sheet, leaveDetail[1], row, column + 16);
													putHeader(numbercellformat, sheet, leaveDetail[2], row, column + 17);
													putHeader(numbercellformat, sheet, leaveDetail[3], row, column + 18);
												}
											}
											if (monthId == 12) {
												if (leaveTypeId == 2) {
													putHeader(numbercellformat, sheet, leaveDetail[0], row, column + 19);
													putHeader(numbercellformat, sheet, leaveDetail[1], row, column + 20);
													putHeader(numbercellformat, sheet, leaveDetail[2], row, column + 21);
													putHeader(numbercellformat, sheet, leaveDetail[3], row, column + 22);
												}
												if (leaveTypeId == 1) {
													putHeader(numbercellformat, sheet, leaveDetail[0], row, column + 23);
													putHeader(numbercellformat, sheet, leaveDetail[1], row, column + 24);
													putHeader(numbercellformat, sheet, leaveDetail[2], row, column + 25);
													putHeader(numbercellformat, sheet, leaveDetail[3], row, column + 26);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			workbook.write();
			workbook.close();
		} catch (Exception e) {
			LogUtility.writeLog("Salary sheet Excel Report Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new BusinessLogicException(e.getMessage());
		}
	}

	private JSONArray getConfigurationSetup() throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		columnArray.put("organizationname");
		columnArray.put("organizationlogo");
		columnArray.put("address");
		query.put(Data.Query.COLUMNS, columnArray);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_mailconfigurations");
	}

	public void setValueInSheet(WritableSheet sheet, WritableFont font, WritableCellFormat hdFormat, int rowNo, int cellNo, String value) throws WriteException, RowsExceededException {
		WritableCellFormat newFormat = new WritableCellFormat(hdFormat);
		Label label;
		newFormat.setFont(font);
		newFormat.setWrap(true);
		label = new Label(cellNo, rowNo, value, newFormat);
		sheet.addCell(label);
	}

	public void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, cellFormat);
		sheet.addCell(numobj);
	}

	public void putHeaderDefault(WritableCellFormat numbercellformat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, numbercellformat);
		sheet.addCell(label);
	}

	JSONArray getLeaveBalanceRecords() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "leaveregister");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("employeeid.departmentid.name");
		columnArray.put("openingbalance");
		columnArray.put("monthid");
		columnArray.put("yearid");
		columnArray.put("credit");
		columnArray.put("debit");
		columnArray.put("leavebalance");
		columnArray.put("closingbalance");
		columnArray.put("leavetypeid");

		query.put(Data.Query.COLUMNS, columnArray);

		query.put(Data.Query.FILTERS, "monthid IN(10,11,12)");
		// query.put(Data.Query.OPTION_FILTERS, "monthid = 10");

		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("leaveregister");
		return array;
	}
}
