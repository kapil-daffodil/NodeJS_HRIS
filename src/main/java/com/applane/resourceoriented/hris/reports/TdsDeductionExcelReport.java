package com.applane.resourceoriented.hris.reports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

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
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;

@Path("/tdsdeductionexcelreport")
public class TdsDeductionExcelReport {

	@GET
	@Path("/tds")
	public Response employeeMonthlyAttendanceGet(@Context HttpServletRequest request) throws IOException {
		return tdsReportPost(request);
	}

	@POST
	@Path("/tds")
	public Response tdsReportPost(@Context HttpServletRequest request) throws IOException {
		String selectedKeys = request.getParameter("keys");
		try {
			if (selectedKeys != null) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				tdsReportExcelReport(outputStream, selectedKeys);
				if (outputStream.size() > 0) {
					ResponseBuilder responseBuilder = Response.ok(outputStream.toByteArray(), "application/vnd.ms-excel");
					responseBuilder.header("Content-Disposition", "attachment; Filename=\"TDS_Deduction" + "\"");
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
			HashMap<Integer, Object[]> tdsMap = (HashMap<Integer, Object[]>) tdsObject.opt("tdsMap");
			Object newBusinessFunctionId = tdsObject.opt("newBusinessFunctionId");
			int counter = 0;
			int row = 0;
			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
			WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
			numbercellformat.setFont(font);
			numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);
			for (Integer employeeId : employeeDetails.keySet()) {

				Object[] details = employeeDetails.get(employeeId);

				if (tdsMap.containsKey(employeeId)) {
					Object[] deductionAmount = tdsMap.get(employeeId);
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
						Object attendanceBasedAmount = details[6];
						Object performanceBasedAmount = details[7];

						Object taxAmount = deductionAmount[0];
						Object lwfAmount = deductionAmount[1];
						Object pfAmount = deductionAmount[2];
						Object advanceAmount = deductionAmount[3];
						Object esiAmount = deductionAmount[4];
						Object otherDeductionAmount = deductionAmount[5];

						addTextCell(sheet, ("" + counter), row, column++);
						addTextCell(sheet, "" + employeeCode, row, column++);
						addTextCell(sheet, "" + employeeName, row, column++);
						addTextCell(sheet, "" + department, row, column++);
						addTextCell(sheet, "" + newBusinessBunction, row, column++);
						addNumericCell(sheet, Translator.doubleValue(grossAmount), row, column++, numbercellformat);
						addNumericCell(sheet, Translator.doubleValue(attendanceBasedAmount), row, column++, numbercellformat);
						addNumericCell(sheet, Translator.doubleValue(performanceBasedAmount), row, column++, numbercellformat);

						addNumericCell(sheet, Translator.doubleValue(taxAmount), row, column++, numbercellformat);
						addNumericCell(sheet, Translator.doubleValue(lwfAmount), row, column++, numbercellformat);
						addNumericCell(sheet, Translator.doubleValue(pfAmount), row, column++, numbercellformat);
						addNumericCell(sheet, Translator.doubleValue(advanceAmount), row, column++, numbercellformat);
						addNumericCell(sheet, Translator.doubleValue(esiAmount), row, column++, numbercellformat);
						addNumericCell(sheet, Translator.doubleValue(otherDeductionAmount), row, column++, numbercellformat);
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
		addTableHeader(headerCellformat, sheet, "TDS Deducted", row, column++);

		addTableHeader(headerCellformat, sheet, "LWF", row, column++);
		addTableHeader(headerCellformat, sheet, "PF Employee", row, column++);
		addTableHeader(headerCellformat, sheet, "Advance", row, column++);
		addTableHeader(headerCellformat, sheet, "ESI Employee", row, column++);
		addTableHeader(headerCellformat, sheet, "Any Other Deductions", row, column++);

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

			String monthName = EmployeeSalaryGenerationServlet.getMonthName(Translator.integerValue(monthId));
			String yearName = EmployeeSalaryGenerationServlet.getYearName(Translator.integerValue(yearId));
			Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
			Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
			String monthFirstDateInString = Translator.getDateInString(monthFirstDate);
			String monthLastDateInString = Translator.getDateInString(monthLastDate);

			getArrearsInBetweenDates(salarySheetArray, monthFirstDateInString, monthLastDateInString, branchId, profitcenterId);

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

					double grossAmount = Translator.doubleValue(paidAmount) + Translator.doubleValue(deductionAmount);
					String profitCenterName = "";
					JSONArray profitCenterIdJsonArray = salarySheetArray.getJSONObject(arrearsCounter).optJSONArray("employeeid.profitcenterid");
					if (profitCenterIdJsonArray != null) {
						for (int j = 0; j < profitCenterIdJsonArray.length(); j++) {
							JSONObject profitCenterJsonObject = profitCenterIdJsonArray.optJSONObject(j);
							if (j != 0) {
								profitCenterName += ";";
							}
							profitCenterName += profitCenterJsonObject.optString(HrisKinds.ProfitCenter.NAME);
						}
					}
					Object[] details = new Object[8];
					details[0] = name;
					details[1] = code;
					details[2] = departmentName;
					details[3] = paidAmount;
					details[4] = grossAmount;
					details[5] = profitCenterName;
					details[6] = attendanceBasedAmount;
					details[7] = performanceBasedAmount;
					if (employeeDetails.containsKey(employeeId)) {
						details = employeeDetails.get(employeeId);
						details[3] = Translator.doubleValue(details[3]) + Translator.doubleValue(paidAmount);
						details[4] = Translator.doubleValue(details[4]) + Translator.doubleValue(grossAmount);
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
				JSONArray monthlySalaryArray = getEmployeeMonthlySalaryRecords(monthId, yearId, invoiceNumber);
				LogUtility.writeError("monthlySalaryArray >> " + monthlySalaryArray);
				HashMap<Integer, Object[]> tdsMap = new HashMap<Integer, Object[]>();
				for (int counter = 0; counter < monthlySalaryArray.length(); counter++) {
					int employeeId = monthlySalaryArray.getJSONObject(counter).optInt("employeeid");
					Object payableAmount = monthlySalaryArray.getJSONObject(counter).opt("amount");
					int componentTypeid = Translator.integerValue(monthlySalaryArray.getJSONObject(counter).opt("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID));
					// Object grossAmount = monthlySalaryArray.getJSONObject(counter).opt("actualamount");
					Object[] details = new Object[6];
					details[0] = 0;
					details[1] = 0;
					details[2] = 0;
					details[3] = 0;
					details[4] = 0;
					if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.TDS) {
						details[0] = payableAmount;
					} else if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.LWF_EMPLOYEE) {
						details[1] = payableAmount;
					} else if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYEE) {
						details[2] = payableAmount;
					} else if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.ADVANCE) {
						details[3] = payableAmount;
					} else if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE) {
						details[4] = payableAmount;
					} else {
						details[5] = payableAmount;
					}
					if (tdsMap.containsKey(employeeId)) {
						if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.TDS) {
							details = tdsMap.get(employeeId);
							details[0] = Translator.doubleValue(payableAmount) + Translator.doubleValue(details[0]);
						} else if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.LWF_EMPLOYEE) {
							details = tdsMap.get(employeeId);
							details[1] = Translator.doubleValue(payableAmount) + Translator.doubleValue(details[1]);
						} else if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYEE) {
							details = tdsMap.get(employeeId);
							details[2] = Translator.doubleValue(payableAmount) + Translator.doubleValue(details[2]);
						} else if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.ADVANCE) {
							details = tdsMap.get(employeeId);
							details[3] = Translator.doubleValue(payableAmount) + Translator.doubleValue(details[3]);
						} else if (componentTypeid == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE) {
							details = tdsMap.get(employeeId);
							details[4] = Translator.doubleValue(payableAmount) + Translator.doubleValue(details[4]);
						} else {
							details = tdsMap.get(employeeId);
							details[5] = Translator.doubleValue(payableAmount) + Translator.doubleValue(details[5]);
						}
					}
					tdsMap.put(employeeId, details);
				}
				JSONObject object = new JSONObject();
				object.put("employeeDetails", employeeDetails);
				object.put("tdsMap", tdsMap);
				if (newBusinessFunctionId != null) {
					object.put("newBusinessFunctionId", newBusinessFunctionId);
				}
				return object;
			}
		}
		return null;
	}

	private JSONArray getEmployeeMonthlySalaryRecords(Object monthId, Object yearId, String invoiceNumber) throws Exception {
		String filters = "";
		if (invoiceNumber.length() > 0) {
			filters += " and invoicenumber in ('" + invoiceNumber + "')";
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("employeeid");
		columnArray.put("amount");
		columnArray.put("actualamount");
		columnArray.put("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarycomponentid." + SalaryComponentKinds.PAYMENT_TYPE_ID + " = 2" + filters);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray array = object.getJSONArray("employeemonthlysalarycomponents");
		return array;
	}

	private void getArrearsInBetweenDates(JSONArray salarySheetArray, String monthFirstDateInString, String monthLastDateInString, Object branchId, Object profitcenterId) throws Exception {
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
		query.put(Data.Query.COLUMNS, columnArray);

		query.put(Data.Query.FILTERS, "isarrear = 1 AND invoicedate >= '" + monthFirstDateInString + "' AND invoicedate <= '" + monthLastDateInString + "'" + filter);

		JSONObject arrearObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray arrearArray = arrearObject.getJSONArray("salarysheet");
		for (int counter = 0; counter < arrearArray.length(); counter++) {
			salarySheetArray.put(arrearArray.getJSONObject(counter));
		}
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
