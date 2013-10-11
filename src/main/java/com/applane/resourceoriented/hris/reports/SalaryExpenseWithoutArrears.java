package com.applane.resourceoriented.hris.reports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import jxl.CellView;
import jxl.Workbook;
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
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.DataTypeUtilities;

@Path("/SalaryExpenseWithoutArrears")
public class SalaryExpenseWithoutArrears {
	@GET
	@Path("/uri")
	public Response salaryExpenseWithoutArrears(@Context HttpServletRequest request) throws IOException {
		return salaryExpenseWithoutArrearsPost(request);
	}

	@POST
	@Path("/uri")
	public Response salaryExpenseWithoutArrearsPost(@Context HttpServletRequest request) throws IOException {
		String selectedKeys = request.getParameter("keys");
		try {
			if (selectedKeys != null) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				tdsReportExcelReport(outputStream, selectedKeys);
				if (outputStream.size() > 0) {
					ResponseBuilder responseBuilder = Response.ok(outputStream.toByteArray(), "application/vnd.ms-excel");
					responseBuilder.header("Content-Disposition", "attachment; Filename=\"Salary_Expense_Without_Arrears" + "\"");
					return responseBuilder.build();
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("TdsDeductionExcelReport >> Exception >> tdsReportPost >> " + trace);
		}
		return Response.ok(200).entity("Please try after some time.").build();
	}

	private void tdsReportExcelReport(ByteArrayOutputStream outputStream, String selectedKeys) {
		try {
			WritableWorkbook workbook = Workbook.createWorkbook(outputStream);
			WritableSheet sheet = workbook.createSheet("AttendanceReport", 0);

			addTableRowHeader(sheet);
			addTableData(selectedKeys, sheet);

			workbook.write();
			workbook.close();
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("TdsDeductionExcelReport >> Exception >> " + trace);
		}
	}

	@SuppressWarnings("unchecked")
	private void addTableData(String selectedKeys, WritableSheet sheet) throws Exception {

		String key = selectedKeys;
		if (key != null && key.length() > 2) {
			key = key.substring(1, (key.length() - 1));
		}
		JSONObject tdsObject = getSalaryDetail(key);

		LogUtility.writeLog("tdsObject >> " + tdsObject);
		if (tdsObject != null) {
			HashMap<Integer, Object[]> employeeDetails = (HashMap<Integer, Object[]>) tdsObject.opt("employeeDetails");
			HashMap<Integer, JSONArray> grossSalaryMap = (HashMap<Integer, JSONArray>) tdsObject.opt("grossSalaryMap");
			List<Integer> grossSalaryList = (List<Integer>) tdsObject.opt("grossSalaryList");
			Object newBusinessFunctionId = tdsObject.opt("newBusinessFunctionId");
			Collections.sort(grossSalaryList, Collections.reverseOrder());
			int counter = 0;
			int row = 0;
			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
			WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
			numbercellformat.setFont(font);
			numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);
			for (int grossAmountListCounter = 0; grossAmountListCounter < grossSalaryList.size(); grossAmountListCounter++) {
				JSONArray employeeIds = grossSalaryMap.get(grossSalaryList.get(grossAmountListCounter));
				for (int employeeIdsCounter = 0; employeeIdsCounter < employeeIds.length(); employeeIdsCounter++) {
					int employeeId = (Integer) employeeIds.opt(employeeIdsCounter);
					if (employeeDetails.containsKey(employeeId)) {

						Object[] details = employeeDetails.get(employeeId);

						Object newBusinessBunction = newBusinessFunctionId == null ? "" : getNewBusinessfunction(employeeId, newBusinessFunctionId);
						if (newBusinessBunction != null) {
							counter++;
							row++;
							int column = 0;
							Object employeeCode = details[1];
							Object employeeName = details[0];
							Object department = details[2];
							Object paidAmount = details[3];
							Object grossAmount = details[4];
							Object fixedAmount = details[5];
							Object attendanceBasedAmount = details[6];
							Object performanceBasedAmount = details[7];

							addTextCell(sheet, ("" + counter), row, column++);
							addTextCell(sheet, "" + employeeCode, row, column++);
							addTextCell(sheet, "" + employeeName, row, column++);
							addTextCell(sheet, "" + department, row, column++);
							addTextCell(sheet, "" + newBusinessBunction, row, column++);
							addNumericCell(sheet, Translator.doubleValue(grossAmount), row, column++, numbercellformat);
							addNumericCell(sheet, Translator.doubleValue(attendanceBasedAmount), row, column++, numbercellformat);
							addNumericCell(sheet, Translator.doubleValue(performanceBasedAmount), row, column++, numbercellformat);
							addNumericCell(sheet, Translator.doubleValue(fixedAmount), row, column++, numbercellformat);
							addNumericCell(sheet, Translator.doubleValue(paidAmount), row, column++, numbercellformat);

							for (int j = 0; j < column; j++) {
								CellView columnView = sheet.getColumnView(j);
								columnView.setAutosize(true);
								sheet.setColumnView(j, columnView);
							}
						}
					}
				}
			}
		}
	}

	private Object getNewBusinessfunction(Integer employeeId, Object newBusinessFunctionId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_additional_information");
		JSONArray columnArray = new JSONArray();
		columnArray.put("businessfunction_id");
		columnArray.put("businessfunction_id.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray array = object.getJSONArray("hris_employees_additional_information");
		if (array != null && array.length() > 0) {

			int newBfId = Translator.integerValue(array.getJSONObject(0).opt("businessfunction_id"));
			int newBfIdParameter = Translator.integerValue(newBusinessFunctionId);
			if (newBfId == 0) {
				return "";
			} else if (newBfId == newBfIdParameter) {
				return array.getJSONObject(0).opt("businessfunction_id.name") == null ? "" : array.getJSONObject(0).opt("businessfunction_id.name");
			} else {
				return null;
			}
		}
		return "";
	}

	private void addTableRowHeader(WritableSheet sheet) throws RowsExceededException, WriteException {

		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat headerCellformat = new WritableCellFormat();
		headerCellformat.setFont(font);

		int row = 0;
		int column = 0;

		addTableHeader(headerCellformat, sheet, "S.No.", row, column++);
		addTableHeader(headerCellformat, sheet, "Employee Code", row, column++);
		addTableHeader(headerCellformat, sheet, "Name", row, column++);
		addTableHeader(headerCellformat, sheet, "Department", row, column++);
		addTableHeader(headerCellformat, sheet, "New Business Function", row, column++);
		addTableHeader(headerCellformat, sheet, "Gross Salary", row, column++);

		addTableHeader(headerCellformat, sheet, "Attendance Based", row, column++);
		addTableHeader(headerCellformat, sheet, "Variable", row, column++);
		addTableHeader(headerCellformat, sheet, "Fixed", row, column++);

		addTableHeader(headerCellformat, sheet, "Payable Salary", row, column++);

		for (int i = 0; i < column; i++) {
			CellView columnView = sheet.getColumnView(i);
			columnView.setAutosize(true);
			sheet.setColumnView(i, columnView);
		}
	}

	private JSONObject getSalaryDetail(String key) throws Exception {

		LogUtility.writeLog("getAttendanceReportDetail >> Selected Key : " + key);

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.PFReportForYear.BRANCH_ID);
		columns.put(HrisKinds.PFReportForYear.DEPARTMENT_ID);
		columns.put(HrisKinds.PFReportForYear.MONTH_ID);
		columns.put(HrisKinds.PFReportForYear.YEAR_ID);
		columns.put("new_business_function_id");
		columns.put("profitcenter_id");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.PF_REPORT_FOR_YEAR);
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, Data.Query.KEY + "=" + key);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.PF_REPORT_FOR_YEAR);

		int resultLength = (resultJSONArray == null) ? 0 : resultJSONArray.length();
		LogUtility.writeLog("Result Length : " + resultLength);

		if (resultLength > 0) {

			JSONObject jsonObject = resultJSONArray.optJSONObject(0);

			Object branchId = jsonObject.opt(HrisKinds.PFReportForYear.BRANCH_ID);
			Object monthId = jsonObject.opt(HrisKinds.PFReportForYear.MONTH_ID);
			Object yearId = jsonObject.opt(HrisKinds.PFReportForYear.YEAR_ID);
			Object newBusinessFunctionId = jsonObject.opt("new_business_function_id");
			Object profitcenterId = jsonObject.opt("profitcenter_id");
			JSONArray salarySheetArray = getSalarySheetArray(branchId, monthId, yearId, profitcenterId);
			List<Integer> grossSasaryList = new ArrayList<Integer>();
			HashMap<Integer, JSONArray> grossSalaryMap = new HashMap<Integer, JSONArray>();
			if (salarySheetArray != null && salarySheetArray.length() > 0) {
				String invoiceNumber = "";
				HashMap<Integer, Object[]> employeeDetails = new HashMap<Integer, Object[]>();
				JSONArray employeeIds = new JSONArray();
				for (int arrearsCounter = 0; arrearsCounter < salarySheetArray.length(); arrearsCounter++) {
					int employeeId = DataTypeUtilities.integerValue(salarySheetArray.getJSONObject(arrearsCounter).opt("employeeid"));
					Object name = salarySheetArray.getJSONObject(arrearsCounter).opt("employeeid.name");
					Object code = salarySheetArray.getJSONObject(arrearsCounter).opt("employeeid.employeecode");
					Object departmentName = salarySheetArray.getJSONObject(arrearsCounter).opt("employeeid.departmentid.name");
					Object deductionAmount = salarySheetArray.getJSONObject(arrearsCounter).opt("deductionamount");
					Object paidAmount = salarySheetArray.getJSONObject(arrearsCounter).opt("payableamount");
					Object attendanceBasedAmount = salarySheetArray.getJSONObject(arrearsCounter).opt("attendancebasedamount");
					Object performanceBasedAmount = salarySheetArray.getJSONObject(arrearsCounter).opt("performancebasedamount");
					Object fixedAmount = salarySheetArray.getJSONObject(arrearsCounter).opt("fixedamount");

					double grossAmount = Translator.doubleValue(paidAmount) + Translator.doubleValue(deductionAmount);

					JSONArray employeeIdArray = new JSONArray();
					if (grossSalaryMap.containsKey((int) grossAmount)) {
						employeeIdArray = grossSalaryMap.get((int) grossAmount);
					}
					employeeIdArray.put(employeeId);
					grossSalaryMap.put((int) grossAmount, employeeIdArray);
					if (!grossSasaryList.contains((int) grossAmount)) {
						grossSasaryList.add((int) grossAmount);
					}
					Object[] details = new Object[8];
					details[0] = name;
					details[1] = code;
					details[2] = departmentName;
					details[3] = paidAmount;
					details[4] = grossAmount;
					details[5] = fixedAmount;
					details[6] = attendanceBasedAmount;
					details[7] = performanceBasedAmount;
					if (employeeDetails.containsKey(employeeId)) {
						details = employeeDetails.get(employeeId);
						details[3] = Translator.doubleValue(details[3]) + Translator.doubleValue(paidAmount);
						details[4] = Translator.doubleValue(details[4]) + Translator.doubleValue(grossAmount);
						details[5] = Translator.doubleValue(details[5]) + Translator.doubleValue(fixedAmount);
						details[6] = Translator.doubleValue(details[6]) + Translator.doubleValue(attendanceBasedAmount);
						details[7] = Translator.doubleValue(details[7]) + Translator.doubleValue(performanceBasedAmount);
					}
					employeeDetails.put(employeeId, details);

					String invoiceNumber1 = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(arrearsCounter).opt("invoicenumber"));
					employeeIds.put(employeeId);
					if (invoiceNumber.length() > 0 && invoiceNumber1 != null && invoiceNumber1.length() > 0) {
						invoiceNumber += "','";
					}
					if (invoiceNumber1 != null && invoiceNumber1.length() > 0) {
						invoiceNumber += invoiceNumber1;
					}
				}
				JSONObject object = new JSONObject();
				object.put("employeeDetails", employeeDetails);
				object.put("grossSalaryMap", grossSalaryMap);
				object.put("grossSalaryList", grossSasaryList);
				if (newBusinessFunctionId != null) {
					object.put("newBusinessFunctionId", newBusinessFunctionId);
				}
				return object;
			}
		}
		return null;
	}

	private JSONArray getSalarySheetArray(Object branchId, Object monthId, Object yearId, Object profitcenterId) throws Exception {
		String filter = "";
		if (branchId != null) {
			filter += " AND branchid = " + branchId;
		}
		if (profitcenterId != null) {
			filter += " AND employeeid.profitcenterid = " + profitcenterId;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		// columnArray.put(new JSONObject().put("employeeid.profitcenterid", new JSONArray().put("name")));
		columnArray.put("invoicenumber");
		columnArray.put("employeeid.departmentid.name");
		columnArray.put("attendancebasedamount");
		columnArray.put("performancebasedamount");
		columnArray.put("deductionamount");
		columnArray.put("payableamount");
		columnArray.put("fixedamount");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "isarrear = 0 AND monthid = " + monthId + " AND yearid = " + yearId + filter);
		query.put(Data.Query.OPTION_FILTERS, "isarrear = null AND monthid = " + monthId + " AND yearid = " + yearId + filter);

		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("salarysheet");
		return employeeArray;
	}

	private void addNumericCell(WritableSheet sheet, double value, int row, int column, WritableCellFormat numbercellformat) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, numbercellformat);
		sheet.addCell(numobj);
	}

	private void addTableHeader(WritableCellFormat headerCellformat, WritableSheet sheet, String value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, value, headerCellformat);
		sheet.addCell(label);
	}

	private void addTextCell(WritableSheet sheet, String value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, value);
		sheet.addCell(label);
	}
}
