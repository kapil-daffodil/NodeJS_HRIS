package com.applane.resourceoriented.hris.reports;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

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
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.procurement.Procurement;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class SalarySheetBeforeReleaseExcelReport implements ApplaneTask {

	double fixedAmount = 0d;
	double attendanceBasedAmount = 0d;
	double performanceBasedAmount = 0d;
	double experienceBasedAmount = 0d;

	double totalSalary = 0d;
	double payToGovernment = 0d;
	double totalGross = 0d;
	double totalDeductable = 0d;
	boolean updateStatus = true;

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void downloadSalarySheetInvoke(Object[] keys) {
		try {
			if (keys != null && keys.length > 0) {
				initiateTaskQueue(keys[0]);
			} else {
				throw new BusinessLogicException("Please Select Atleast One Row.");
			}
		} catch (JSONException e) {
			throw new RuntimeException("Error occured while geting month name from monthid: " + e.getMessage());
		}
	}

	public void initiateTaskQueue(Object key) throws JSONException {
		JSONObject taskQueueInfo = new JSONObject();
		taskQueueInfo.put("key", key);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.reports.SalarySheetBeforeReleaseExcelReport", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueInfo);
	}

	@Override
	public void initiate(ApplaneTaskInfo taskQueueInfo) throws DeadlineExceededException {
		String key = taskQueueInfo.getTaskInfo().optString("key");
		if (key != null) {
			employeeMonthlySalaryPost(key);
		}
	}

	public void employeeMonthlySalaryPost(Object keys) {
		String selectedKeys = "[" + keys + "]";
		try {
			if (selectedKeys != null) {
				String[] fileName = { "" };
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				initiate(outputStream, selectedKeys, fileName);
				if (outputStream.size() > 0) {
					// ResponseBuilder responseBuilder = Response.ok(outputStream.toByteArray(), "application/vnd.ms-excel");
					// responseBuilder.header("Content-Disposition", "attachment; Filename=\"Salary-Sheet-Before-Release" + "\"");
					// responseBuilder.build();

					StringBuffer mailBody = new StringBuffer();
					mailBody.append("<div>Dear Sir/Mam ").append("</div><div>	<br><div> Please find attached your Salary Sheet Before Release Process.").append(".</div>");
					mailBody.append("<br><div> Regards</div><div>Applane Team</div><br>");
					mailBody.append("<div>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account</div>");

					com.applane.service.mail.Attachment xlsAttachment = new com.applane.service.mail.Attachment("Salary-Sheet-" + fileName[0] + ".xls", "application/vnd.ms-excel", outputStream.toByteArray());
					ApplaneMail mail = new ApplaneMail();

					mail.setAttachments(xlsAttachment);
					mail.setMessage("Salary Sheet For " + fileName[0], mailBody.toString(), true);
					// mail.setTo("yogesh@daffodilsw.com", "nitin.goyal@daffodilsw.com", "shobhit.elhance@applane.com", "amit@daffodilsw.com");
					mail.setTo("kapil.dalal@daffodilsw.com");
					mail.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
				}
			}
		} catch (Exception e) {

		}
	}

	public static String getMonthName(long monthId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_months");
			JSONArray array = new JSONArray();
			array.put("name");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + monthId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("organization_months");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				String monthName = rows.getJSONObject(0).optString("name");
				return monthName;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error occured while geting month name from monthid: " + e.getMessage());
		}
	}

	public static String getYearName(long yearId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_years");
			JSONArray array = new JSONArray();
			array.put("name");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + yearId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("organization_years");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				String yearName = rows.getJSONObject(0).optString("name");
				return yearName;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error occured while converting yearid into year name.." + e.getMessage());
		}
	}

	public void initiate(ByteArrayOutputStream outputStream, String selectedKeys, String[] fileName) {
		try {
			WritableWorkbook workbook = Workbook.createWorkbook(outputStream);
			WritableSheet sheet = workbook.createSheet("AttendanceReport", 0);
			String key = selectedKeys;
			if (key != null && key.length() > 2) {
				key = key.substring(1, (key.length() - 1));
			}
			JSONObject taskInfo = getTakeSalarySheetRecords(key);

			long monthId = Translator.integerValue(taskInfo.opt("monthid"));
			long yearId = Translator.integerValue(taskInfo.opt("yearid"));
			long branchId = Translator.integerValue(taskInfo.opt("branchid"));

			String monthName = "" + taskInfo.opt("monthid.name");
			String yearName = "" + taskInfo.opt("yearid.name");
			String branchName = "" + taskInfo.opt("branchid.name");
			fileName[0] += branchName + " - " + monthName + " - " + yearName;
			initiateValuesAndStartRleasing(branchId, yearId, monthId, sheet);
			workbook.write();
			workbook.close();
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError("SalarySheetBeforeReleasingExcelReport Exception Trace >> " + trace);
		}
	}

	private JSONObject getTakeSalarySheetRecords(Object key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takesalarysheets");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("branchid");
		array.put("monthid");
		array.put("yearid");
		array.put("branchid.name");
		array.put("monthid.name");
		array.put("yearid.name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("takesalarysheets");
		if (rows != null && rows.length() > 0) {
			return rows.getJSONObject(0);
		}
		return null;
	}

	private void initiateValuesAndStartRleasing(long branchId, long yearId, long monthId, WritableSheet sheet) throws Exception {

		String monthName = getMonthName(monthId);
		String yearName = getYearName(yearId);
		Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
		String monthFirstDateInString = Translator.getDateInString(monthFirstDate);
		String monthLastDateInString = Translator.getDateInString(monthLastDate);

		JSONArray employeeArray = getActiveAndReleivedEmployeeRecords(branchId, monthFirstDateInString, monthLastDateInString);
		JSONArray releaseSalarySheetArray = releaseSalarySheet(employeeArray, monthId, yearId, monthFirstDate, monthLastDate, monthName, yearName, monthFirstDateInString, monthLastDateInString);
		JSONArray salaryComponentArray = getSalaryComponents();
		LinkedHashMap<Integer, String> componentsMap = new LinkedHashMap<Integer, String>();

		for (int counter = 0; counter < salaryComponentArray.length(); counter++) {
			// 16 salary components like HRA, basic
			String salaryComponent = salaryComponentArray.getJSONObject(counter).optString("name");
			int salaryComponentId = salaryComponentArray.getJSONObject(counter).optInt("__key__");
			componentsMap.put(salaryComponentId, salaryComponent);
		}
		WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat hdFormat = new WritableCellFormat(font);
		hdFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
		hdFormat.setAlignment(Alignment.CENTRE);

		WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
		numbercellformat.setFont(font);
		numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);

		WritableFont font1 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat hdFormat1 = new WritableCellFormat(font1);
		hdFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
		hdFormat1.setAlignment(Alignment.LEFT);

		WritableCellFormat stringCellFormat = new WritableCellFormat(NumberFormats.TEXT);
		stringCellFormat.setFont(font1);
		stringCellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
		putHeaderIntoSheet(componentsMap, sheet, numbercellformat, stringCellFormat);

		if (releaseSalarySheetArray != null && releaseSalarySheetArray.length() > 0) {
			putSalaryIntoSheet(releaseSalarySheetArray, sheet, componentsMap, numbercellformat, stringCellFormat);
		}
	}

	private void putHeaderIntoSheet(LinkedHashMap<Integer, String> componentsMap, WritableSheet sheet, WritableCellFormat numbercellformat, WritableCellFormat stringCellFormat) throws Exception {
		int row = 1;
		int column = 0;
		addTextCell(stringCellFormat, sheet, "S. No.", row, column++);
		addTextCell(stringCellFormat, sheet, "Invoice Number", row, column++);
		addTextCell(stringCellFormat, sheet, "Code", row, column++);
		addTextCell(stringCellFormat, sheet, "Name", row, column++);
		addTextCell(stringCellFormat, sheet, "Account Number", row, column++);
		for (Integer salaryComponentId : componentsMap.keySet()) {
			String componentName = componentsMap.get(salaryComponentId);
			addTextCell(stringCellFormat, sheet, componentName, row, column++);
		}
		addTextCell(stringCellFormat, sheet, "--------Blank-------", row, column++);
		for (Integer salaryComponentId : componentsMap.keySet()) {
			String componentName = componentsMap.get(salaryComponentId);
			addTextCell(stringCellFormat, sheet, componentName, row, column++);
		}
		addTextCell(stringCellFormat, sheet, "Gross Salary", row, column++);
		addTextCell(stringCellFormat, sheet, "Payable Salary", row, column++);
	}

	public void putHeader(WritableCellFormat numberCellFormat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, numberCellFormat);
		sheet.addCell(numobj);
	}

	private void addTextCell(WritableCellFormat stringCellFormat, WritableSheet sheet, String value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, value);
		sheet.addCell(label);
	}

	public JSONArray getSalaryComponents() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		columnArray.put("ignoreinexceptionreport");
		columnArray.put("compulsoryifdefined");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("salarycomponents");
		return array;
	}

	@SuppressWarnings("unchecked")
	private void putSalaryIntoSheet(JSONArray releaseSalarySheetArray, WritableSheet sheet, LinkedHashMap<Integer, String> componentsMap, WritableCellFormat numberCellFormat, WritableCellFormat stringCellFormat) throws Exception {
		int row = 2;
		int column = 0;
		int sNo = 0;
		for (int counter = 0; counter < releaseSalarySheetArray.length(); counter++) {
			row++;
			column = 0;
			JSONObject object = releaseSalarySheetArray.optJSONObject(counter);
			if (object != null) {
				sNo++;
				Object payableSalary = object.opt("payble_amount");
				Object grossSalary = object.opt("gross_salary");
				Object invoiceno = object.opt("invoiceno");
				Object bankAccount = object.opt("bank_account");
				Object name = object.opt("name");
				Object employeeCode = object.opt("employeecode");
				HashMap<Integer, JSONObject> salaryDetailsMap = (HashMap<Integer, JSONObject>) object.opt("procurement_vendorinvoicelineitems");
				addTextCell(stringCellFormat, sheet, "" + sNo, row, column++);
				addTextCell(stringCellFormat, sheet, "" + invoiceno, row, column++);
				addTextCell(stringCellFormat, sheet, "" + employeeCode, row, column++);
				addTextCell(stringCellFormat, sheet, "" + name, row, column++);
				addTextCell(stringCellFormat, sheet, "" + bankAccount, row, column++);

				for (Integer salaryComponentId : componentsMap.keySet()) {
					if (salaryDetailsMap.containsKey(salaryComponentId)) {
						JSONObject componentDetailedObject = salaryDetailsMap.get(salaryComponentId);
						Object grossAmount = componentDetailedObject.opt("grossAmount");
						putHeader(numberCellFormat, sheet, Translator.doubleValue(grossAmount), row, column++);
					} else {
						addTextCell(stringCellFormat, sheet, "", row, column++);
					}
				}
				addTextCell(stringCellFormat, sheet, "", row, column++);
				for (Integer salaryComponentId : componentsMap.keySet()) {
					if (salaryDetailsMap.containsKey(salaryComponentId)) {
						JSONObject componentDetailedObject = salaryDetailsMap.get(salaryComponentId);
						Object payableAmount = componentDetailedObject.opt("amount");
						putHeader(numberCellFormat, sheet, Translator.doubleValue(payableAmount), row, column++);
					} else {
						addTextCell(stringCellFormat, sheet, "", row, column++);
					}
				}
				putHeader(numberCellFormat, sheet, Translator.doubleValue(grossSalary), row, column++);
				putHeader(numberCellFormat, sheet, Translator.doubleValue(payableSalary), row, column++);
			}
		}
	}

	public JSONArray releaseSalarySheet(JSONArray employeeArray, long monthId, long yearId, Date monthFirstDate, Date monthLastDate, String monthName, String yearName, String monthFirstDateInString, String monthLastDateInString) throws JSONException {
		int employeeArrayCount = employeeArray == null ? 0 : employeeArray.length();
		if (employeeArrayCount > 0) {
			JSONArray errorWhileReleasingSalaryForEmployees = new JSONArray();
			String employeeNameWhileError = "";
			List<Integer> eList = new ArrayList<Integer>();
			JSONArray employeeSalaryArray = new JSONArray();
			for (int counter = 0; counter < employeeArray.length(); counter++) {
				try {
					JSONObject employeeRecord = employeeArray.getJSONObject(counter);
					Object employeeId = employeeRecord.opt("__key__");

					if (!eList.contains(Translator.integerValue(employeeId))) {
						eList.add(Translator.integerValue(employeeId));
						fixedAmount = 0d;
						attendanceBasedAmount = 0d;
						performanceBasedAmount = 0d;
						experienceBasedAmount = 0d;
						totalSalary = 0d;
						payToGovernment = 0d;
						totalGross = 0d;
						totalDeductable = 0d;
						updateStatus = true;
						Object branchid = employeeRecord.opt("branchid");
						Object employeecode = employeeRecord.opt("employeecode");
						Object name = employeeRecord.opt("name");
						employeeNameWhileError = "" + name;
						Object accountno = employeeRecord.opt("accountno");
						Object modeOfSalaryId = employeeRecord.opt("modeofsalaryid");

						String invoiceNumber = "" + employeecode + "/" + monthName + "/" + yearName;
						JSONArray monthlyattendanceArray = getEmployeeMonthlyAttendanceRecords(employeeId, monthId, yearId);

						if ((monthlyattendanceArray == null ? 0 : monthlyattendanceArray.length()) > 0) {
							String mainFilter = "";

							JSONArray paymentHistory = getPaymentHistory(employeeId, monthId, yearId);

							if ((paymentHistory == null ? 0 : paymentHistory.length()) > 0) {
								for (int i = 0; i < paymentHistory.length(); i++) {
									if (mainFilter.length() > 0) {
										mainFilter += "," + paymentHistory.getJSONObject(i).opt("employeesalarycomponentid");
									} else {
										mainFilter += paymentHistory.getJSONObject(i).opt("employeesalarycomponentid");
									}
								}
								mainFilter = " and employeesalarycomponentid in (" + mainFilter + ")";
							}
							String optionFilter = " and employeesalarycomponentid.duemonthid = " + monthId;
							JSONArray componentMonthlyAmountArray = getComponentMonthlyAmount(employeeId, yearId, monthId, mainFilter, optionFilter);
							HashMap<Integer, Object[]> componentMonthlyDetail = new HashMap<Integer, Object[]>(); // used to update component wise payable salary
							if ((componentMonthlyAmountArray == null ? 0 : componentMonthlyAmountArray.length()) > 0) {
								JSONObject invoiceJsonObject = new JSONObject();
								invoiceJsonObject.put("transfer_mode", modeOfSalaryId);
								invoiceJsonObject.put("officeid", branchid);
								invoiceJsonObject.put("invoiceno", invoiceNumber);
								invoiceJsonObject.put("invoicedate", monthLastDateInString);
								invoiceJsonObject.put("vendorid", employeeId);
								invoiceJsonObject.put("employeecode", employeecode);
								invoiceJsonObject.put("name", name);
								Calendar cal1 = Calendar.getInstance();
								Calendar cal2 = Calendar.getInstance();
								for (int j = 0; j < componentMonthlyAmountArray.length(); j++) {
									JSONObject componentAmountRecord = componentMonthlyAmountArray.getJSONObject(j);
									String componentName = Translator.stringValue(componentAmountRecord.opt("salarycomponentid.name"));
									int criteriaId = componentAmountRecord.optInt("salarycomponentid.componentcriteriaid");
									int paymentTypeId = componentAmountRecord.optInt("salarycomponentid.paymenttypeid");
									int paymentCycleId = componentAmountRecord.optInt("salarycomponentid.paymentcycleid");
									int salaryComponentId = componentAmountRecord.optInt("salarycomponentid");
									int employeeSalaryComponentId = componentAmountRecord.optInt("employeesalarycomponentid");
									int dueMonthId = componentAmountRecord.optInt("employeesalarycomponentid.duemonthid");
									int paidAfterMonth = Translator.integerValue(componentAmountRecord.opt("salarycomponentid.paidaftermonth"));
									Date applicableUpToDate = Translator.dateValue(componentAmountRecord.opt("employeesalarycomponentid.applicableto"));
									double amount = Translator.doubleValue(componentAmountRecord.opt("payableamount"));
									double grossAmount = Translator.doubleValue(componentAmountRecord.opt("grossamount"));
									int amountPayToid = Translator.integerValue(componentAmountRecord.opt("salarycomponentid.amountpaytoid"));
									int isInFirstComponents = Translator.integerValue(componentAmountRecord.opt("isinfirstcomponents"));
									double ctcPercentage = Translator.doubleValue(componentAmountRecord.opt("salarycomponentid.ctcpercentage"));
									int salaryCompontntTypeId = Translator.integerValue(componentAmountRecord.opt("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID));
									int addInESICalculation = Translator.integerValue(componentAmountRecord.opt("salarycomponentid.add_in_esi_calculation"));

									int paybleTypeId = Translator.integerValue(componentAmountRecord.opt("salarycomponentid.payable_expense_type_id"));
									int supplementryTypeId = Translator.integerValue(componentAmountRecord.opt("salarycomponentid.deductable_supplementary_id"));

									if (applicableUpToDate != null) {
										boolean isPayableInSalaryReleasingMonth = false;
										cal1.setTime(monthFirstDate);
										cal2.setTime(applicableUpToDate);
										cal2.add(Calendar.MONTH, paidAfterMonth);

										if (cal1.before(cal2) || cal1.equals(cal2)) {
											isPayableInSalaryReleasingMonth = true;
										}
										if (isPayableInSalaryReleasingMonth) {
											Object[] detail = new Object[18];
											detail[0] = Translator.doubleValue(detail[0]) + amount; // net Payable amount
											detail[1] = Translator.doubleValue(detail[1]) + grossAmount; // net Gross amount
											detail[2] = salaryComponentId;
											detail[3] = employeeSalaryComponentId;
											detail[4] = paymentCycleId;
											detail[5] = dueMonthId;
											detail[6] = paidAfterMonth;
											detail[7] = applicableUpToDate;
											detail[8] = paymentTypeId;
											detail[9] = criteriaId;
											detail[10] = componentName;
											detail[11] = amountPayToid;
											detail[12] = ctcPercentage;
											detail[13] = salaryCompontntTypeId;
											detail[14] = isInFirstComponents;
											detail[15] = addInESICalculation;
											detail[16] = paybleTypeId;
											detail[17] = supplementryTypeId;
											componentMonthlyDetail.put(salaryComponentId, detail);

										}
									}
								}
								HashMap<Integer, JSONObject> employeeComponentsArrayForInvoice = new HashMap<Integer, JSONObject>();
								int j = 0;
								for (Integer salaryComponentId : componentMonthlyDetail.keySet()) {
									j++;
									Object[] detail = componentMonthlyDetail.get(salaryComponentId);
									double netPayable = Translator.doubleValue(detail[0]); // net Payable amount
									double grossAmount = Translator.doubleValue(detail[1]); // net Gross amount
									// int salaryComponentId = Translator.integerValue(detail[2]);
									// Integer dueMonthId = Translator.integerValue(detail[5]);
									// Date applicableUpToDate = (Date) (detail[7]);
									// int inInFirstComponents = Translator.integerValue(detail[14]);
									// Object employeeSalaryComponentId = detail[3];
									Object paymentCycleId = detail[4];
									int paidAfterMonth = Translator.integerValue(detail[6]);
									int paymentTypeId = Translator.integerValue(detail[8]);
									int criteriaId = Translator.integerValue(detail[9]);
									String componentName = Translator.stringValue(detail[10]);
									int amountPayToId = Translator.integerValue(detail[11]);
									int salaryCompontntTypeId = Translator.integerValue(detail[13]);
									int addInESICalculation = Translator.integerValue(detail[15]);

									// boolean valueForDoubleRecord = false;
									// if (paidAfterMonth == 0 && criteriaId != HRISApplicationConstants.NET_PAYABLE_BASED && paymentCycleId == HRISApplicationConstants.MONTHLY) {
									// valueForDoubleRecord = true;
									// }
									Calendar cal = Calendar.getInstance();
									if (paymentCycleId == HRISApplicationConstants.YEARLY) {
										netPayable = 0d;
										grossAmount = 0d;
										for (int i = 0; i < 12; i++) {
											cal.setTime(monthFirstDate);
											cal.add(Calendar.MONTH, (-paidAfterMonth));
											cal.add(Calendar.MONTH, (-i));
											long monthId1 = cal.get(Calendar.MONTH) + 1;

											long yearId1 = getYearId(String.valueOf(cal.get(Calendar.YEAR)));
											double[] payableAndGrossAmount = getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1);
											grossAmount += payableAndGrossAmount[0];
											netPayable += payableAndGrossAmount[1];

										}
									}
									if (paymentCycleId == HRISApplicationConstants.HALF_YEARLY) {
										netPayable = 0d;
										grossAmount = 0d;
										for (int i = 0; i < 6; i++) {
											cal.setTime(monthFirstDate);
											cal.add(Calendar.MONTH, (-paidAfterMonth));
											cal.add(Calendar.MONTH, (-i));
											long monthId1 = cal.get(Calendar.MONTH) + 1;
											long yearId1 = getYearId(String.valueOf(cal.get(Calendar.YEAR)));
											double[] payableAndGrossAmount = getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1);
											grossAmount += payableAndGrossAmount[0];
											netPayable += payableAndGrossAmount[1];

										}
									}
									if (paymentCycleId == HRISApplicationConstants.QUARTERLY) {
										netPayable = 0d;
										grossAmount = 0d;
										for (int i = 0; i < 3; i++) {
											cal.setTime(monthFirstDate);
											cal.add(Calendar.MONTH, (-paidAfterMonth));
											cal.add(Calendar.MONTH, (-i));
											long monthId1 = cal.get(Calendar.MONTH) + 1;
											long yearId1 = getYearId(String.valueOf(cal.get(Calendar.YEAR)));
											double[] payableAndGrossAmount = getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1);
											grossAmount += payableAndGrossAmount[0];
											netPayable += payableAndGrossAmount[1];

										}
									}

									if (paymentCycleId == HRISApplicationConstants.MONTHLY) {
										netPayable = 0d;
										grossAmount = 0d;
										for (int i = 0; i < 1; i++) {
											cal.setTime(monthFirstDate);
											cal.add(Calendar.MONTH, (-paidAfterMonth));
											cal.add(Calendar.MONTH, (-i));
											long monthId1 = cal.get(Calendar.MONTH) + 1;
											long yearId1 = getYearId(String.valueOf(cal.get(Calendar.YEAR)));
											double[] payableAndGrossAmount = getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1);
											grossAmount += payableAndGrossAmount[0];
											netPayable += payableAndGrossAmount[1];

										}
									}
									if (salaryCompontntTypeId == HRISApplicationConstants.SalaryComponentTypes.LWF_EMPLOYEE) {
										netPayable = 10;
									}
									JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
									tempObjectToGenerateEnvoiceArray.put("expense_type", componentName);
									tempObjectToGenerateEnvoiceArray.put("amount", netPayable);
									tempObjectToGenerateEnvoiceArray.put("grossAmount", grossAmount);
									tempObjectToGenerateEnvoiceArray.put("salaryComponentId", salaryComponentId);
									tempObjectToGenerateEnvoiceArray.put("title", invoiceNumber + "/" + j);
									tempObjectToGenerateEnvoiceArray.put("expensedate", monthLastDateInString);
									employeeComponentsArrayForInvoice.put(salaryComponentId, tempObjectToGenerateEnvoiceArray);
									if (paymentTypeId != HRISApplicationConstants.DEDUCTABLE && (paymentTypeId == HRISApplicationConstants.PAYABLE || amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT)) {
										totalSalary += netPayable;
										if (amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT || addInESICalculation != HRISApplicationConstants.EmployeeDecision.YES) {
											payToGovernment += netPayable;
										}
									}
									if (paymentTypeId == HRISApplicationConstants.PAYABLE || amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
										totalGross += grossAmount;
									}
									if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE || amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
										totalDeductable += netPayable;
									}
									if (criteriaId == HRISApplicationConstants.ATTENDANCE_BASED && paymentTypeId == HRISApplicationConstants.PAYABLE) {
										attendanceBasedAmount += netPayable;
									}
									if (criteriaId == HRISApplicationConstants.FIXED_BASED && paymentTypeId == HRISApplicationConstants.PAYABLE) {
										fixedAmount += netPayable;
									}
									if (criteriaId == HRISApplicationConstants.PERFORMANCE_BASED && paymentTypeId == HRISApplicationConstants.PAYABLE) {
										performanceBasedAmount += netPayable;
									}
									if (criteriaId == HRISApplicationConstants.EXPERIENCE_BASED && paymentTypeId == HRISApplicationConstants.PAYABLE) {
										experienceBasedAmount += netPayable;
									}
								}

								JSONArray esiArray = getEsiArray(monthFirstDateInString, monthLastDateInString);
								boolean esiCycle = false;
								if (esiArray != null && esiArray.length() > 0) {
									JSONArray esiComponentArray = getEsiSalaryComponentId();
									if (esiComponentArray != null && esiComponentArray.length() > 0) {
										int salaryComponentId = Translator.integerValue(esiComponentArray.getJSONObject(0).opt("__key__"));
										if (!componentMonthlyDetail.containsKey(salaryComponentId)) {
											Date fromDate = Translator.dateValue(esiComponentArray.getJSONObject(0).opt("from_date"));
											Date toDate = Translator.dateValue(esiComponentArray.getJSONObject(0).opt("to_date"));
											Calendar cal = Calendar.getInstance();

											long monthsBetweenDates = DataTypeUtilities.monthsBetweenDates(fromDate, toDate);
											boolean esiDeducted = false;
											for (int esiCounter = 0; esiCounter < monthsBetweenDates; esiCounter++) {
												cal.setTime(fromDate);
												cal.add(Calendar.MONTH, esiCounter);
												int esiMonthId = cal.get(Calendar.MONTH) + 1;
												Long esiYearId = getYearId("" + cal.get(Calendar.YEAR));
												JSONArray esiPaidComponentArray = getEsiPaidComponentArray(employeeId, esiMonthId, esiYearId);
												if (esiPaidComponentArray != null && esiPaidComponentArray.length() > 0) {
													esiDeducted = true;
													break;
												}
											}
											if (esiDeducted) {
												esiCycle = true;
												Object componentName = esiComponentArray.getJSONObject(0).opt("name");
												Object criteriaId = esiComponentArray.getJSONObject(0).opt("componentcriteriaid");
												Object paymentTypeId = esiComponentArray.getJSONObject(0).opt("paymenttypeid");
												Object amountPayToId = esiComponentArray.getJSONObject(0).opt("amountpaytoid");
												Object ctcPercentage = esiComponentArray.getJSONObject(0).opt("ctcpercentage");
												Object componentTypeId = esiComponentArray.getJSONObject(0).opt("salarycomponenttypeid");

												Object[] detail = new Object[16];
												detail[1] = 0;
												detail[2] = salaryComponentId;
												detail[3] = null;
												detail[8] = paymentTypeId;
												detail[9] = criteriaId;
												detail[10] = componentName;
												detail[11] = amountPayToId;
												detail[12] = ctcPercentage;
												detail[13] = componentTypeId;
												detail[14] = 1;
												componentMonthlyDetail.put(salaryComponentId, detail);
											}
										}
									}
								}
								for (Integer salaryComponentId : componentMonthlyDetail.keySet()) {
									Object[] detail = componentMonthlyDetail.get(salaryComponentId);
									int criteriaId = Translator.integerValue(detail[9]);

									if (criteriaId == HRISApplicationConstants.NET_PAYABLE_BASED) {
										double grossAmount = Translator.doubleValue(detail[1]); // net Gross amount
										// Object employeeSalaryComponentId = detail[3];
										// int inInFirstComponents = Translator.integerValue(detail[14]);
										int paymentTypeId = Translator.integerValue(detail[8]);
										String componentName = Translator.stringValue(detail[10]);
										int amountPayToId = Translator.integerValue(detail[11]);
										double ctcPercentage = Translator.doubleValue(detail[12]);
										int salaryCompontntTypeId = Translator.integerValue(detail[13]);
										double netPayable = (totalSalary - payToGovernment);
										if ((salaryCompontntTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE || salaryCompontntTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYER) && netPayable > 15000.0 && !esiCycle) {
											netPayable = 0.0;
										}
										netPayable = (netPayable * ctcPercentage) / 100;
										JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
										tempObjectToGenerateEnvoiceArray.put("expense_type", componentName);

										tempObjectToGenerateEnvoiceArray.put("grossAmount", grossAmount);
										tempObjectToGenerateEnvoiceArray.put("salaryComponentId", salaryComponentId);
										tempObjectToGenerateEnvoiceArray.put("amount", netPayable);
										tempObjectToGenerateEnvoiceArray.put("title", invoiceNumber + "/" + j);
										tempObjectToGenerateEnvoiceArray.put("expensedate", monthLastDateInString);
										employeeComponentsArrayForInvoice.put(salaryComponentId, tempObjectToGenerateEnvoiceArray);
										if (paymentTypeId != HRISApplicationConstants.DEDUCTABLE && (paymentTypeId == HRISApplicationConstants.PAYABLE || amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT)) {
											totalSalary += netPayable;
										}
										if (paymentTypeId == HRISApplicationConstants.PAYABLE || amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
											totalGross += grossAmount;
										}
										if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE || amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
											totalDeductable += netPayable;
										}
										if (criteriaId == HRISApplicationConstants.ATTENDANCE_BASED && paymentTypeId == HRISApplicationConstants.PAYABLE) {
											attendanceBasedAmount += netPayable;
										}
										if (criteriaId == HRISApplicationConstants.FIXED_BASED && paymentTypeId == HRISApplicationConstants.PAYABLE) {
											fixedAmount += netPayable;
										}
										if (criteriaId == HRISApplicationConstants.PERFORMANCE_BASED && paymentTypeId == HRISApplicationConstants.PAYABLE) {
											performanceBasedAmount += netPayable;
										}
										if (criteriaId == HRISApplicationConstants.EXPERIENCE_BASED && paymentTypeId == HRISApplicationConstants.PAYABLE) {
											experienceBasedAmount += netPayable;
										}
									}
								}
								invoiceJsonObject.put("gross_salary", totalSalary);
								invoiceJsonObject.put("payble_amount", totalSalary - totalDeductable);
								invoiceJsonObject.put("bank_account", accountno);
								invoiceJsonObject.put("salary_date", monthLastDateInString);
								invoiceJsonObject.put(Procurement.PROCUREMENT_VENDORINVOICELINEITEMS, employeeComponentsArrayForInvoice);
								LogUtility.writeLog("invoiceJsonObject>> " + invoiceJsonObject);
								employeeSalaryArray.put(invoiceJsonObject);
							} else {
								JSONObject errorObject = new JSONObject();
								errorObject.put("Name", employeeNameWhileError);
								errorObject.put("Problem", "Generated Salary Components Not Found.");
								errorWhileReleasingSalaryForEmployees.put(errorObject);
							}
						} else {
							JSONObject errorObject = new JSONObject();
							errorObject.put("Name", employeeNameWhileError);
							errorObject.put("Problem", "Monthly Attendance Not Found.");
							errorWhileReleasingSalaryForEmployees.put(errorObject);
						}
					}
				} catch (Exception e) {
					JSONObject errorObject = new JSONObject();
					errorObject.put("Name", employeeNameWhileError);
					// errorObject.put("Error", e.getStackTrace());
					errorObject.put("Error", ExceptionUtils.getExceptionTraceMessage(SalarySheetBeforeReleaseExcelReport.class.getName(), e));

					errorWhileReleasingSalaryForEmployees.put(errorObject);
				}
			}
			LogUtility.writeError("employeeSalaryArray >> " + employeeSalaryArray);
			if (errorWhileReleasingSalaryForEmployees.length() > 0) {
				sendStatus(new JSONObject().put("Error -->", errorWhileReleasingSalaryForEmployees), "Error While Salary Releasing (Employee List)");
			}
			return employeeSalaryArray;
		}
		return null;
	}

	private JSONArray getEsiPaidComponentArray(Object employeeId, int esiMonthId, Long esiYearId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and salarymonthid = " + esiMonthId + " and salaryyearid = " + esiYearId);
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray array = employeeObject.getJSONArray("employeemonthlysalarycomponents");
		return array;
	}

	private JSONArray getEsiSalaryComponentId() throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		columnArray.put("componentcriteriaid");
		columnArray.put("paymenttypeid");
		columnArray.put("amountpaytoid");
		columnArray.put("ctcpercentage");
		columnArray.put("salarycomponenttypeid");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarycomponenttypeid = " + HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("salarycomponents");
		return rows;
	}

	private JSONArray getEsiArray(String monthFirstDateInString, String monthLastDateInString) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_esi_cycle_master");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("from_date");
		columnArray.put("to_date");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "from_date <= '" + monthFirstDateInString + "' and  to_date >= '" + monthFirstDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, "from_date <= '" + monthLastDateInString + "' and  to_date >= '" + monthLastDateInString + "'");
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("hris_esi_cycle_master");
		return rows;
	}

	public static void sendStatus(JSONObject employeeList, String title) throws JSONException {
		if (employeeList.length() > 0) {
			StringBuilder message = new StringBuilder(employeeList.length() + " " + title);
			message.append(employeeList.length() + " " + title + " List Given below:-<br>");
			message.append(employeeList.toString()).append("<br><br>");
			if (CurrentState.getCurrentState().get(CurrentSession.CURRENT_USER_EMAIL).equals("kapil.dalal@daffodilsw.com")) {
				new ApplaneMail().sendExceptionMail(new String[] { "kapil.dalal@daffodilsw.com" }, null, null, title, message.toString(), true);
			} else {
				new ApplaneMail().sendExceptionMail(new String[] { "kapil.dalal@daffodilsw.com", "" + CurrentState.getCurrentState().get(CurrentSession.CURRENT_USER_EMAIL) }, null, null, title, message.toString(), true);
			}
		}
	}

	public double[] getAmountFromComponentMonthlyAmount(Object employeeId, int salaryComponentId, long monthId, long yearId) throws JSONException {

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "componentmonthlyamount");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("grossamount");
		columnArray.put("employeesalarycomponentid");
		columnArray.put("isinfirstcomponents");
		columnArray.put("payableamount"); // payable to employee

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and yearid = " + yearId + " and monthid = " + monthId + " and salarycomponentid =" + salaryComponentId);// +
																																												// " and employeesalarycomponentid = "
																																												// +
																																												// employeeSalaryComponentId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("componentmonthlyamount");
		double[] amount = { 0d, 0d };
		if ((rows == null ? 0 : rows.length()) > 0) {
			for (int i = 0; i < rows.length(); i++) {
				amount[0] += Translator.doubleValue(rows.getJSONObject(i).opt("grossamount"));
				amount[1] += Translator.doubleValue(rows.getJSONObject(i).opt("payableamount"));
			}
			return amount;
		} else {
			return amount;
		}

	}

	public JSONArray getComponentMonthlyAmount(Object employeeId, long yearId, long monthId, String mainFilter, String optionFolter) throws JSONException {

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "componentmonthlyamount");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeesalarycomponentid");
		columnArray.put("employeesalarycomponentid.duemonthid");
		columnArray.put("employeesalarycomponentid.applicablefrom");
		columnArray.put("employeesalarycomponentid.applicableto");
		columnArray.put("salarycomponentid"); // basic, ...
		columnArray.put("salarycomponentid.ctcpercentage");
		columnArray.put("salarycomponentid.amountpaytoid");
		columnArray.put("salarycomponentid.paidaftermonth");
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid.payable_expense_type_id");
		columnArray.put("salarycomponentid.deductable_supplementary_id");
		columnArray.put("salarycomponentid.add_in_esi_calculation");
		columnArray.put("grossamount");
		columnArray.put("isinfirstcomponents");
		columnArray.put("payableamount"); // payable to employee
		columnArray.put("salarycomponentid.componentcriteriaid"); // Fixed,
																	// Attendance
																	// Based,
																	// Performance
																	// Based ...
		columnArray.put("salarycomponentid.paymenttypeid"); // payable,
															// deductable
		columnArray.put("salarycomponentid.paymentcycleid"); // yearly, half
																// yearly ......
		columnArray.put("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID);
		query.put(Data.Query.COLUMNS, columnArray);
		if (mainFilter != null && mainFilter.length() > 0) {
			query.put(Data.Query.FILTERS, "employeeid = " + employeeId + "" + mainFilter);
			if (optionFolter != null && optionFolter.length() > 0) {
				query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId + "" + optionFolter);
			}
		} else if (optionFolter != null && optionFolter.length() > 0) {
			query.put(Data.Query.FILTERS, "employeeid = " + employeeId + "" + optionFolter);
		} else {
			return new JSONArray();
		}
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("componentmonthlyamount");
		return rows;

	}

	public static JSONArray getPaymentHistory(Object employeeId, long monthId, long yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "paymenthistory");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("paymentmonthid");
		columnArray.put("paymentyearid");
		columnArray.put("employeeid");
		columnArray.put("employeesalarycomponentid.salarycomponentid");
		columnArray.put("employeesalarycomponentid");
		columnArray.put("employeesalarycomponentid.salarycomponentid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and paymentyearid = " + yearId + " and  paymentmonthid = " + monthId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("paymenthistory");
		return rows;
	}

	public static Long getYearId(String yearName) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_years");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "name = '" + yearName + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("organization_years");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			Long yearId = Long.parseLong("" + rows.getJSONObject(0).opt("__key__"));
			return yearId;
		}
		return null;
	}

	public static Long getMonthId(String monthhName) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_months");
			JSONArray array = new JSONArray();
			array.put("__key__");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "name = '" + monthhName + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("organization_months");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				Long monthId = Long.parseLong("" + rows.getJSONObject(0).opt("__key__"));
				return monthId;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error while retrieving month id from month name: " + monthhName + " : " + e.getMessage());
		}
	}

	public static JSONArray getEmployeeMonthlyAttendanceRecords(Object employeeId, long monthId, long yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("presentdays");
		columnArray.put("absents");
		columnArray.put("leaves");
		columnArray.put("extraworkingdays");
		columnArray.put("totalworkingdays");
		columnArray.put("holidays");
		columnArray.put("nonworkingdays");
		columnArray.put("nonpayableleaves");
		columnArray.put("actualnonworkingdays");
		columnArray.put("monthlyclosingbalance");
		columnArray.put("paidabsents");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and yearid = " + yearId + " and  monthid = " + monthId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeemonthlyattendance");
		return rows;
	}

	public void sendMailInCaseOfException(Exception e) {
		e.printStackTrace();
		String traces = ExceptionUtils.getExceptionTraceMessage(EmployeeSalaryGenerationServlet.class.getName(), e);
		ApplaneMail mail = new ApplaneMail();
		StringBuilder builder = new StringBuilder();
		builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
		builder.append("<br><br><b>Exception traces are given below :</b><br></br><hr></hr><br></br>").append(traces);
		mail.setMessage("Employee Salary Servlet Task Queue Failed", builder.toString(), true);
		try {
			mail.setTo("kapil.dalal@daffodilsw.com");
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
		}
	}

	public static JSONArray getActiveAndReleivedEmployeeRecords(long branchId, String monthFirstDateInString, String monthLastDateInString) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put("officialemailid");
			columnArray.put("name");
			columnArray.put("incrementduedate");
			columnArray.put("employeescheduleid");
			columnArray.put("holidaycalendarid");
			columnArray.put("leavepolicyid");
			columnArray.put("leavepolicyid.issandwich");
			columnArray.put("joiningdate");
			columnArray.put("branchid");
			columnArray.put("subbranchid");
			columnArray.put("employeecode");
			columnArray.put("profitcenterid");
			columnArray.put("accountno");
			columnArray.put("modeofsalaryid");
			query.put(Data.Query.COLUMNS, columnArray);
			// query.put(Data.Query.FILTERS, "__key__ = 130");
			query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + " and branchid = " + branchId + " AND salary_on_hold != 1");
			query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and branchid = " + branchId + " and relievingdate >= '" + monthFirstDateInString + "'" + " AND salary_on_hold != 1"); // +
			// query.put(Data.Query.MAX_ROWS, 1);
			JSONObject employeeObject;
			employeeObject = ResourceEngine.query(query);
			JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
			return employeeArray;
		} catch (JSONException e) {
			throw new RuntimeException("Error occured while fetching active and releived employee records" + e.getMessage());
		} catch (Exception e) {
			throw new RuntimeException("Error occured while fetching active and releived employee records" + e.getMessage());
		}
	}
}
