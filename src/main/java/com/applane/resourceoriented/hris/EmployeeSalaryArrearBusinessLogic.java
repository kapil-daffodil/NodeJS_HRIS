package com.applane.resourceoriented.hris;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.InterAppCommunication;
import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.applane.moduleimpl.SystemParameters;
import com.applane.procurement.Procurement;
import com.applane.resourceoriented.hris.HRISApplicationConstants.SalaryComponents;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;

public class EmployeeSalaryArrearBusinessLogic {

	public static String PENDING = "pending";
	public static String PAID = "paid";
	public static String INVOICE_NOT_FOUND = "invoice_not_found";

	double fixedAmount = 0d;
	double attendanceBasedAmount = 0d;
	double performanceBasedAmount = 0d;
	double experienceBasedAmount = 0d;
	double payToGovernment = 0d;
	double totalSalary = 0d;
	double totalGross = 0d;
	double totalDeductable = 0d;
	double totalDeductableForNegative = 0d;

	public void salaryArrearInvoke(Object[] selectedKeys, Object arrearDate) {

		if (selectedKeys != null && selectedKeys.length > 0) {
			for (int counter = 0; counter < selectedKeys.length; counter++) {
				try {
					fixedAmount = 0d;
					attendanceBasedAmount = 0d;
					performanceBasedAmount = 0d;
					experienceBasedAmount = 0d;
					payToGovernment = 0d;
					totalSalary = 0d;
					totalGross = 0d;
					totalDeductable = 0d;
					totalDeductableForNegative = 0.0;

					Date invoiceGenerationDate = Translator.dateValue(arrearDate);
					JSONArray salarySheetArray = new EmployeeSalaryGenerationServlet().getEmployeeIdFromSalarySheet("" + selectedKeys[counter]);
					if (salarySheetArray != null && salarySheetArray.length() > 0) {
						List<Integer> keyList = new ArrayList<Integer>();
						for (int i = 0; i < salarySheetArray.length(); i++) {
							JSONObject salarySheetObject = salarySheetArray.getJSONObject(i);
							int __key__ = Translator.integerValue(salarySheetObject.opt(Updates.KEY));
							if (!keyList.contains(__key__)) {
								keyList.add(__key__);

								String employeecode = Translator.stringValue(salarySheetObject.opt("employeeid.employeecode"));
								String employeeName = Translator.stringValue(salarySheetObject.opt("employeeid.name"));
								long employeeId = Translator.integerValue(salarySheetObject.opt("employeeid"));
								long yearId = Translator.integerValue(salarySheetObject.opt("yearid"));
								long monthId = Translator.integerValue(salarySheetObject.opt("monthid"));
								Object accountno = salarySheetObject.opt("employeeid.accountno");
								// Object branchid = salarySheetObject.opt("employeeid.branchid");
								Object profitcenterid = salarySheetObject.opt("employeeid.profitcenterid");
								Object businessfunctionid = salarySheetObject.opt("employeeid.businessfunctionid");
								Object modeOfSalaryId = salarySheetObject.opt("employeeid.modeofsalaryid");

								Object branchId = salarySheetObject.opt("branchid");
								Object currentBranchId = salarySheetObject.opt("sub_branchid");

								if (profitcenterid != null && profitcenterid instanceof JSONArray && ((JSONArray) profitcenterid).length() > 0) {
									profitcenterid = ((JSONArray) profitcenterid).get(0);
								}

								if (businessfunctionid != null && businessfunctionid instanceof JSONArray && ((JSONArray) businessfunctionid).length() > 0) {
									businessfunctionid = ((JSONArray) businessfunctionid).get(0);
								}
								boolean generateInvoice = ReleaseSalaryEmployeeWise.getGenerateInvoice((int) yearId, (int) monthId, Translator.integerValue(branchId));
								double attendanceBaseAmountOld = 0.0;
								double performanceBasedAmountOld = 0.0;
								double fixedAmountOld = 0.0;
								double deductionAmountOld = 0.0;
								double payableAmountOld = 0.0;
								String monthName = EmployeeReleasingSalaryServlet.getMonthName(monthId);
								String yearName = EmployeeReleasingSalaryServlet.getYearName(yearId);
								Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
								Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
								String monthFirstDateString = Translator.getDateInString(monthFirstDate);
								String monthLastDateString = Translator.getDateInString(monthLastDate);
								Date invoiceDate = invoiceGenerationDate;

								String invoiceNumber = "" + employeecode + "/" + monthName + "/" + yearName;
								String status = cheackReReleasedInvoice(invoiceNumber);
								HashMap<Integer, Object[]> componentAmountPayableDetail = null;
								HashMap<Integer, String> paidCompoenents = new HashMap<Integer, String>();
								try {
									componentAmountPayableDetail = startArrearProcess(employeeId, monthId, yearId, monthFirstDate, monthFirstDateString, monthLastDateString, paidCompoenents);
								} catch (Exception e) {
									LogUtility.writeError("[" + employeeName + "] Exception Trace >> " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
									WarningCollector.collect("[" + employeeName + "] Some Error Occured.");
									continue;
								}
								String warning = "Component Removed from Applicable Date [";
								if (paidCompoenents != null && paidCompoenents.size() > 0) {
									for (Integer id : paidCompoenents.keySet()) {
										String name = paidCompoenents.get(id);
										warning += name + ", ";
									}
									warning += "]";
									// WarningCollector.collect("[" + employeeName + "] " + warning);
									throw new BusinessLogicException("[" + employeeName + "] " + warning);
								}
								Object invoiceWillBeUpdateKey = null;
								if (componentAmountPayableDetail != null && componentAmountPayableDetail.size() > 0) {
									JSONObject invoiceJsonObject = new JSONObject();

									invoiceJsonObject.put("transfer_mode", modeOfSalaryId);
									invoiceJsonObject.put("profitcenter_id", profitcenterid);
									invoiceJsonObject.put("officeid", branchId);

									JSONArray employeeRecordsFromSalarySheet = getRecordsFromSalarySheet(employeeId, monthId, yearId);
									for (int invoiceCounter = 0; invoiceCounter < employeeRecordsFromSalarySheet.length(); invoiceCounter++) {
										JSONObject invoiceDetails = employeeRecordsFromSalarySheet.getJSONObject(invoiceCounter);

										double attendanceBaseAmountOldInvoice = Translator.doubleValue(invoiceDetails.opt("attendancebasedamount"));
										double performanceBasedAmountOldInvoice = Translator.doubleValue(invoiceDetails.opt("performancebasedamount"));
										double fixedAmountOldInvoice = Translator.doubleValue(invoiceDetails.opt("fixedamount"));
										double deductionAmountOldInvoice = Translator.doubleValue(invoiceDetails.opt("deductionamount"));
										double payableAmountOldInvoice = Translator.doubleValue(invoiceDetails.opt("payableamount"));

										attendanceBaseAmountOld += attendanceBaseAmountOldInvoice;
										performanceBasedAmountOld += performanceBasedAmountOldInvoice;
										fixedAmountOld += fixedAmountOldInvoice;
										deductionAmountOld += deductionAmountOldInvoice;
										payableAmountOld += payableAmountOldInvoice;

									}
									invoiceNumber = "" + employeecode + "/" + monthName + "/" + yearName + "/" + employeeRecordsFromSalarySheet.length();
									LogUtility.writeLog("invoiceNumber >> " + invoiceNumber + " << invoiceWillBeUpdateKey >> " + invoiceWillBeUpdateKey);

									invoiceJsonObject.put("invoiceno", invoiceNumber);
									invoiceJsonObject.put("invoicedate", EmployeeSalaryGenerationServlet.getDateInString(invoiceDate));
									invoiceJsonObject.put(Procurement.ProcurementVendorinvoices.VENDOR_ID, employeeId);
									String TODAY_DATE1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());
									Date currentDateToday = Translator.dateValue(TODAY_DATE1);
									int salaryDifferenceTemp = (int) Translator.doubleValue(new DecimalFormat("#.##").format(totalSalary - totalDeductable));
									int payableAmountOldTemp = (int) Translator.doubleValue(new DecimalFormat("#.##").format(payableAmountOld));
									if (!currentDateToday.equals(invoiceDate)) {
										WarningCollector.collect("[" + employeeName + "] Arrear Date Should be same " + (invoiceDate == null ? "" : EmployeeSalaryGenerationServlet.getDateInString(invoiceDate)));
									} else if (salaryDifferenceTemp != payableAmountOldTemp) {
										double salaryDifference = (totalSalary - totalDeductable) - payableAmountOld;
										attendanceBasedAmount = (attendanceBasedAmount - attendanceBaseAmountOld);
										performanceBasedAmount = (performanceBasedAmount - performanceBasedAmountOld);
										fixedAmount = (fixedAmount - fixedAmountOld);
										totalDeductable = (totalDeductable - deductionAmountOld);
										totalSalary = (attendanceBasedAmount + performanceBasedAmount + fixedAmount);
										salaryDifference = Translator.doubleValue(new DecimalFormat("#.##").format(salaryDifference));
										JSONArray monthlyattendanceArray = EmployeeReleasingSalaryServlet.getEmployeeMonthlyAttendanceRecords(employeeId, monthId, yearId);
										if (monthlyattendanceArray.length() > 0) {
											String arrearDateInString = EmployeeSalaryGenerationServlet.getDateInString(invoiceGenerationDate);

											insertUpdateRecords(invoiceWillBeUpdateKey, monthlyattendanceArray, componentAmountPayableDetail, monthId, yearId, employeeId, invoiceNumber, arrearDateInString, branchId, currentBranchId);

											HashMap<Integer, Object[]> componentOldAmountPayableDetail = getComponentOldAmountPayableDetail(employeeId, monthId, yearId, invoiceNumber);

											JSONArray employeeComponentsArrayForInvoice = new JSONArray();
											JSONArray employeeComponentsArrayForInvoiceDeductable = new JSONArray();
											int j = 0;
											for (Integer salaryComponentId : componentAmountPayableDetail.keySet()) {
												j++;
												Object[] details = componentAmountPayableDetail.get(salaryComponentId);

												double netPayable = Translator.doubleValue(details[0]);
												double grossAmount = Translator.doubleValue(details[1]);
												Object employeeSalaryComponentId = details[3];
												int amountPayToId = Translator.integerValue(details[5]);
												int paymentTypeId = Translator.integerValue(details[6]);
												int isInFirstComponents = Translator.integerValue(details[7]);
												boolean esiCycle = Translator.booleanValue(details[8]);
												int payableExpenseTypeId = Translator.integerValue(details[9]);
												int supplementryId = Translator.integerValue(details[10]);

												if (componentOldAmountPayableDetail.containsKey(salaryComponentId)) {
													Object[] oldDetails = componentOldAmountPayableDetail.get(salaryComponentId);
													grossAmount = grossAmount - Translator.doubleValue(oldDetails[1]);
													netPayable = netPayable - Translator.doubleValue(oldDetails[2]);
												}
												netPayable = Translator.doubleValue(new DecimalFormat("#.##").format(netPayable));
												grossAmount = Translator.doubleValue(new DecimalFormat("#.##").format(grossAmount));

												if (netPayable != 0.0 || grossAmount != 0.0) {
													EmployeeReleasingSalaryServlet.updateCalculatedAmountsInEmployeeMonthlySalary(employeeId, salaryComponentId, monthId, yearId, netPayable, employeeSalaryComponentId, grossAmount, invoiceNumber, isInFirstComponents, esiCycle);
												}

												// netPayable = netPayable < 0 ? (netPayable * (-1)) : netPayable;
												if (paymentTypeId == HRISApplicationConstants.PAYABLE && netPayable > 0) {// && amountPayToId != HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
													JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
													tempObjectToGenerateEnvoiceArray.put("expense_type", details[4]); // component name
													if (payableExpenseTypeId != 0) {
														tempObjectToGenerateEnvoiceArray.put("expense_type_id", payableExpenseTypeId);
													}
													tempObjectToGenerateEnvoiceArray.put("amount", netPayable);
													tempObjectToGenerateEnvoiceArray.put("title", invoiceNumber + "/" + j);
													tempObjectToGenerateEnvoiceArray.put("business_function_id", businessfunctionid);
													tempObjectToGenerateEnvoiceArray.put("expensedate", EmployeeSalaryGenerationServlet.getDateInString(invoiceDate));
													employeeComponentsArrayForInvoice.put(tempObjectToGenerateEnvoiceArray);
												}

												if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE && netPayable < 0) {// && amountPayToId != HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
													JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
													tempObjectToGenerateEnvoiceArray.put("expense_type", details[4]); // component name
													if (payableExpenseTypeId != 0) {
														tempObjectToGenerateEnvoiceArray.put("expense_type_id", payableExpenseTypeId);
													}
													tempObjectToGenerateEnvoiceArray.put("amount", (netPayable * (-1)));
													tempObjectToGenerateEnvoiceArray.put("title", invoiceNumber + "/" + j);
													tempObjectToGenerateEnvoiceArray.put("business_function_id", businessfunctionid);
													tempObjectToGenerateEnvoiceArray.put("expensedate", EmployeeSalaryGenerationServlet.getDateInString(invoiceDate));
													employeeComponentsArrayForInvoice.put(tempObjectToGenerateEnvoiceArray);
												}

												if (paymentTypeId == HRISApplicationConstants.PAYABLE && netPayable < 0) {
													totalDeductableForNegative += (netPayable * (-1));
													JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
													tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.NAME, details[4]); // component name
													if (supplementryId != 0)
														tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.NAME_ID, supplementryId); // component name
													tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.AMOUNT, (netPayable * (-1)));
													employeeComponentsArrayForInvoiceDeductable.put(tempObjectToGenerateEnvoiceArray);
												}
												if ((paymentTypeId == HRISApplicationConstants.DEDUCTABLE || amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) && netPayable > 0) {
													JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
													tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.NAME, details[4]); // component name
													if (supplementryId != 0) {
														tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.NAME_ID, supplementryId); // component name
													}
													tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.AMOUNT, netPayable);
													employeeComponentsArrayForInvoiceDeductable.put(tempObjectToGenerateEnvoiceArray);
												}

											}
											// insert or update into salary sheet
											// insertUpdateRecords(invoiceWillBeUpdateKey, monthlyattendanceArray, componentAmountPayableDetail, monthId, yearId, employeeId, invoiceNumber, arrearDateInString);

											JSONObject procurementSalaryDetail = new JSONObject();
											procurementSalaryDetail.put("gross_salary", salaryDifference < 0 ? (salaryDifference * (-1)) : salaryDifference);
											// procurementSalaryDetail.put("tds", arg1);
											// procurementSalaryDetail.put("advance", arg1);
											procurementSalaryDetail.put("payble_amount", salaryDifference < 0 ? (salaryDifference * (-1)) : salaryDifference);
											procurementSalaryDetail.put("bank_account", accountno);
											procurementSalaryDetail.put("salary_date", EmployeeSalaryGenerationServlet.getDateInString(invoiceDate));
											procurementSalaryDetail.put(Procurement.ProcurementSalaryDetail.SALARY_DEDUCTIONS, employeeComponentsArrayForInvoiceDeductable);
											invoiceJsonObject.put(Procurement.PROCUREMENT_VENDORINVOICELINEITEMS, employeeComponentsArrayForInvoice);
											invoiceJsonObject.put("procurement_salary_detail", procurementSalaryDetail);

											invoiceJsonObject.put(Procurement.RE_RELEASE, false);
											LogUtility.writeLog("invoiceJsonObject >> " + invoiceJsonObject.toString());
											if (salaryDifference > 0) {
												if (employeeComponentsArrayForInvoice != null && employeeComponentsArrayForInvoice.length() > 0 && generateInvoice) {
													InterAppCommunication.invokeMethod("com.applane.procurement.SalaryDisbursement", "createNewExpenseInvoice", new Object[] { invoiceJsonObject });
												}
											} else {
												salaryDifference = salaryDifference * (-1);
												String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());
												Date currentDate = DataTypeUtilities.checkDateFormat(TODAY_DATE);
												if (currentDate != null) {
													Calendar cal = Calendar.getInstance();
													int invoiceCounter = 0;
													for (;; invoiceCounter++) {
														cal.setTime(currentDate);
														cal.add(Calendar.MONTH, invoiceCounter);

														monthName = EmployeeReleasingSalaryServlet.getMonthName(cal.get(Calendar.MONTH) + 1);
														yearName = "" + cal.get(Calendar.YEAR);
														invoiceNumber = "" + employeecode + "/" + monthName + "/" + yearName;
														status = cheackReReleasedInvoice(invoiceNumber);
														if (status.equals(INVOICE_NOT_FOUND)) {
															monthId = cal.get(Calendar.MONTH) + 1;
															break;
														}
													}
													monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
													monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
													String monthFirstDateInString = Translator.getDateInString(monthFirstDate);
													String monthLastDateInString = Translator.getDateInString(monthLastDate);

													Object componentId = PunchingUtility.getId("salarycomponents", "name", SalaryComponents.SALARY_ADVANCE_DUE_TO_ARREAR);

													Object key = null;
													// if (invoiceWillBeUpdateKey != null) {
													// key = getEmployeeSalaryComponentKey(employeeId, componentId, monthFirstDateInString, monthLastDateInString);
													// }

													EmployeeNameBusinessLogic.insertEmployeeSalaryPackageComponentsDetails(key, employeeId, componentId, monthFirstDateInString, monthLastDateInString, salaryDifference, monthId, 0.0, 0.0);

													if (generateInvoice) {
														insertAdvanceInvoice(employeeName, branchId, profitcenterid, invoiceDate, invoiceNumber, salaryDifference);
													}
												}
											}
										} else {
											WarningCollector.collect("[" + employeeName + "] Monthly Attendance Not Found");
										}
									} else {
										WarningCollector.collect("[" + employeeName + "] Not Required Arrear Process, There is not difference between Old Invoice");
									}
								}
							}
						}
					}
				} catch (BusinessLogicException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
				}
			}
		}
	}

	private void insertAdvanceInvoice(String employeeName, Object branchid, Object profitcenterid, Date invoiceDate, String invoiceNumber, double salaryDifference) throws JSONException {
		JSONObject advanceObject = new JSONObject();
		advanceObject.put(Procurement.ProcurementPayments.PROFITCENTER_ID, profitcenterid);
		advanceObject.put(Procurement.ProcurementPayments.OFFICE_ID, branchid);
		advanceObject.put(Procurement.ProcurementPayments.PAYMENT_NO, invoiceNumber + "/Advance");
		advanceObject.put(Procurement.ProcurementPayments.PAYMENT_DATE, invoiceDate);
		advanceObject.put(Procurement.ProcurementPayments.VENDOR_ID, employeeName);
		advanceObject.put(Procurement.ProcurementPayments.AMOUNT, salaryDifference);

		InterAppCommunication.invokeMethod("com.applane.procurement.SalaryDisbursement", "payAdvanceSalaryOfEmployee", new Object[] { advanceObject });
	}

	public Object getEmployeeSalaryComponentKey(long employeeId, Object componentId, String monthFirstDate, String monthLastDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom = '" + monthFirstDate + "' and applicableto = '" + monthLastDate + "' and salarycomponentid = " + componentId);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("employeesalarycomponents");

		if (rows != null && rows.length() > 0) {
			return rows.getJSONObject(0).opt(Updates.KEY);
		}
		return null;
	}

	private HashMap<Integer, Object[]> getComponentOldAmountPayableDetail(long employeeId, long monthId, long yearId, String invoiceNumber) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("salarycomponentid");
		columnArray.put("amount");
		columnArray.put("actualamount");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and salarymonthid = " + monthId + " and salaryyearid = " + yearId + " and invoicenumber != '" + invoiceNumber + "'");
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("employeemonthlysalarycomponents");

		HashMap<Integer, Object[]> componentOldAmountPayableDetail = new HashMap<Integer, Object[]>();

		for (int counter = 0; counter < salarySheetArray.length(); counter++) {
			Integer salaryComponentId = Translator.integerValue(salarySheetArray.getJSONObject(counter).opt("salarycomponentid"));
			double actualamount = Translator.doubleValue(salarySheetArray.getJSONObject(counter).opt("actualamount"));
			double amount = Translator.doubleValue(salarySheetArray.getJSONObject(counter).opt("amount"));
			Object[] details = new Object[3];
			details[0] = salaryComponentId;
			details[1] = actualamount;
			details[2] = amount;
			if (componentOldAmountPayableDetail.containsKey(salaryComponentId)) {
				details = componentOldAmountPayableDetail.get(salaryComponentId);
				details[1] = Translator.doubleValue(details[1]) + actualamount;
				details[2] = Translator.doubleValue(details[2]) + amount;
			}
			componentOldAmountPayableDetail.put(salaryComponentId, details);
		}
		return componentOldAmountPayableDetail;

	}

	private void insertUpdateRecords(Object invoiceWillBeUpdateKey, JSONArray monthlyattendanceArray, HashMap<Integer, Object[]> componentAmountPayableDetail, long monthId, long yearId, long employeeId, String invoiceNumber, String arrearDateInString, Object branchId, Object currentBranchId) throws JSONException {

		double presentDays = monthlyattendanceArray.getJSONObject(0).opt("presentdays") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("presentdays");
		double absentDays = monthlyattendanceArray.getJSONObject(0).opt("absents") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("absents");
		double monthlyLeaves = monthlyattendanceArray.getJSONObject(0).opt("leaves") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("leaves");
		double extraWorkingDays = monthlyattendanceArray.getJSONObject(0).opt("extraworkingdays") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("extraworkingdays");
		double totalWorkingDays = monthlyattendanceArray.getJSONObject(0).opt("totalworkingdays") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("totalworkingdays");
		double totalHoliday = monthlyattendanceArray.getJSONObject(0).opt("holidays") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("holidays");
		double totalOffs = monthlyattendanceArray.getJSONObject(0).opt("nonworkingdays") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("nonworkingdays");
		double nonPayableLeaves = monthlyattendanceArray.getJSONObject(0).opt("nonpayableleaves") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("nonpayableleaves");
		double actualNonWorkingDays = monthlyattendanceArray.getJSONObject(0).opt("actualnonworkingdays") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("actualnonworkingdays");
		double monthlyClosingBalance = monthlyattendanceArray.getJSONObject(0).opt("monthlyclosingbalance") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("monthlyclosingbalance");
		double paidAbsents = monthlyattendanceArray.getJSONObject(0).opt("paidabsents") == null ? 0.0 : monthlyattendanceArray.getJSONObject(0).optDouble("paidabsents");

		insertAttendanceInSalarySheet(invoiceWillBeUpdateKey, employeeId, monthId, yearId, presentDays, absentDays, monthlyLeaves, extraWorkingDays, totalWorkingDays, totalHoliday, totalOffs, actualNonWorkingDays, nonPayableLeaves, monthlyClosingBalance, paidAbsents, invoiceNumber, arrearDateInString, branchId, currentBranchId);
	}

	public void insertAttendanceInSalarySheet(Object invoiceWillBeUpdateKey, Object employeeId, long monthId, long yearId, Object presentDays, Object absentDays, Object leaves, Object extraWorkingDays, double totalWorkingDays, double totalHoliday, double totalOffs, double actualNonWorkingDays, double nonPayableLeaves, double monthlyClosingBalance, double paidAbsent, String invoiceNumber, String arrearDateInString, Object branchId, Object currentBranchId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "salarysheet");
		JSONObject row = new JSONObject();
		if (invoiceWillBeUpdateKey != null) {
			row.put(Updates.KEY, invoiceWillBeUpdateKey);
		} else {
			row.put("invoicenumber", invoiceNumber);
			row.put("invoicedate", arrearDateInString);
		}
		row.put("employeeid", employeeId);
		row.put("monthid", monthId);
		row.put("yearid", yearId);
		row.put("present", presentDays);
		row.put("absent", absentDays);
		row.put("leaves", leaves);
		row.put("extraworkingday", extraWorkingDays);
		row.put("totalworkingdays", totalWorkingDays);
		row.put("totaloff", totalOffs);
		row.put("holidays", totalHoliday);
		row.put("nonworkingdays", actualNonWorkingDays);
		row.put("nonpayableleaves", nonPayableLeaves);
		row.put("monthlyclosingbalance", monthlyClosingBalance);
		row.put("paidabsents", paidAbsent);
		row.put("payableamount", totalSalary - totalDeductable);
		row.put("attendancebasedamount", attendanceBasedAmount);
		row.put("performancebasedamount", performanceBasedAmount);
		row.put("fixedamount", fixedAmount);
		row.put("deductionamount", totalDeductable);// + totalDeductableForNegative);
		row.put("isarrear", true);
		row.put("status", 1);
		row.put("branchid", branchId);
		row.put("sub_branchid", currentBranchId);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private HashMap<Integer, Object[]> startArrearProcess(long employeeId, long monthId, long yearId, Date monthFirstDate, String monthFirstDateString, String monthLastDateString, HashMap<Integer, String> paidCompoenents) throws Exception {
		JSONArray paymentHistory = EmployeeReleasingSalaryServlet.getPaymentHistory(employeeId, monthId, yearId);
		String mainFilter = "";
		if ((paymentHistory == null ? 0 : paymentHistory.length()) > 0) {
			for (int i = 0; i < paymentHistory.length(); i++) {
				if (paymentHistory.getJSONObject(i).opt("employeesalarycomponentid.salarycomponentid.name") != null) {
					paidCompoenents.put(Translator.integerValue(paymentHistory.getJSONObject(i).opt("employeesalarycomponentid.salarycomponentid")), paymentHistory.getJSONObject(i).optString("employeesalarycomponentid.salarycomponentid.name", ""));
				}
				if (mainFilter.length() > 0) {
					mainFilter += "," + paymentHistory.getJSONObject(i).opt("employeesalarycomponentid");
				} else {
					mainFilter += paymentHistory.getJSONObject(i).opt("employeesalarycomponentid");
				}
			}
			mainFilter = " and employeesalarycomponentid in (" + mainFilter + ")";
		}
		String optionFilter = " and employeesalarycomponentid.duemonthid = " + monthId;
		JSONArray componentMonthlyAmountArray = new EmployeeReleasingSalaryServlet().getComponentMonthlyAmount(employeeId, yearId, monthId, mainFilter, optionFilter);

		HashMap<Integer, Object[]> componentMonthlyDetail = new HashMap<Integer, Object[]>();
		HashMap<Integer, Object[]> componentAmountPayableDetail = new HashMap<Integer, Object[]>();
		if ((componentMonthlyAmountArray == null ? 0 : componentMonthlyAmountArray.length()) > 0) {
			List<String> list = new ArrayList<String>();
			Calendar cal1 = Calendar.getInstance();
			Calendar cal2 = Calendar.getInstance();

			Calendar cal3 = Calendar.getInstance();
			Calendar cal4 = Calendar.getInstance();
			EmployeeReleasingSalaryServlet erss = new EmployeeReleasingSalaryServlet();
			for (int j = 0; j < componentMonthlyAmountArray.length(); j++) {
				JSONObject componentAmountRecord = componentMonthlyAmountArray.getJSONObject(j);
				String componentName = Translator.stringValue(componentAmountRecord.opt("salarycomponentid.name"));
				int criteriaId = componentAmountRecord.optInt("salarycomponentid.componentcriteriaid");
				int paymentTypeId = componentAmountRecord.optInt("salarycomponentid.paymenttypeid");
				int paymentCycleId = componentAmountRecord.optInt("salarycomponentid.paymentcycleid");
				int salaryComponentId = componentAmountRecord.optInt("salarycomponentid");

				int payableExpenseTypeId = componentAmountRecord.optInt("salarycomponentid.payable_expense_type_id");
				int supplementryTypeId = componentAmountRecord.optInt("salarycomponentid.deductable_supplementary_id");

				int employeeSalaryComponentId = componentAmountRecord.optInt("employeesalarycomponentid");
				int dueMonthId = componentAmountRecord.optInt("employeesalarycomponentid.duemonthid");
				int paidAfterMonth = componentAmountRecord.optInt("salarycomponentid.paidaftermonth");
				Date applicableFromDate = Translator.dateValue(componentAmountRecord.opt("employeesalarycomponentid.applicablefrom"));
				Date applicableUpToDate = Translator.dateValue(componentAmountRecord.opt("employeesalarycomponentid.applicableto"));
				double amount = Translator.doubleValue(componentAmountRecord.opt("payableamount"));
				double grossAmount = Translator.doubleValue(componentAmountRecord.opt("grossamount"));
				int amountPayToid = Translator.integerValue(componentAmountRecord.opt("salarycomponentid.amountpaytoid"));
				double ctcPercentage = Translator.doubleValue(componentAmountRecord.opt("salarycomponentid.ctcpercentage"));
				int isInFirstComponents = Translator.integerValue(componentAmountRecord.opt("isinfirstcomponents"));
				int componentTypeId = Translator.integerValue(componentAmountRecord.opt("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID));
				int addInESICalculation = Translator.integerValue(componentAmountRecord.opt("salarycomponentid.add_in_esi_calculation"));
				if (applicableUpToDate != null) {
					boolean isPayableInSalaryReleasingMonth = false;
					cal1.setTime(monthFirstDate);
					cal2.setTime(applicableUpToDate);
					cal2.add(Calendar.MONTH, paidAfterMonth);

					cal3.setTime(Translator.dateValue(monthLastDateString));
					cal4.setTime(applicableFromDate);
					cal4.add(Calendar.MONTH, paidAfterMonth);
					if ((cal1.before(cal2) || cal1.equals(cal2)) && (cal3.after(cal4) || cal3.equals(cal4))) {
						isPayableInSalaryReleasingMonth = true;
					}
					if (isPayableInSalaryReleasingMonth) {
						if (paidCompoenents.containsKey(salaryComponentId)) {
							paidCompoenents.remove(salaryComponentId);
						}
						erss.updateHistory(employeeId, employeeSalaryComponentId, dueMonthId, yearId);
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
						detail[13] = isInFirstComponents;
						detail[14] = componentTypeId;
						detail[15] = addInESICalculation;
						detail[16] = payableExpenseTypeId;
						detail[17] = supplementryTypeId;
						componentMonthlyDetail.put(salaryComponentId, detail);

						list.add(componentName);
					}
				}
			}

			for (Integer salaryComponentId : componentMonthlyDetail.keySet()) {
				Object[] detail = componentMonthlyDetail.get(salaryComponentId);
				double netPayable = Translator.doubleValue(detail[0]); // net Payable amount
				double grossAmount = Translator.doubleValue(detail[1]); // net Gross amount
				Object employeeSalaryComponentId = detail[3];
				Object paymentCycleId = detail[4];
				int paidAfterMonth = Translator.integerValue(detail[6]);
				int paymentTypeId = Translator.integerValue(detail[8]);
				int criteriaId = Translator.integerValue(detail[9]);
				String componentName = Translator.stringValue(detail[10]);
				int amountPayToId = Translator.integerValue(detail[11]);
				int isInFirstSalaryComponent = Translator.integerValue(detail[13]);
				int addInESICalculation = Translator.integerValue(detail[15]);
				int payableExpenseTypeId = Translator.integerValue(detail[16]);
				int supplementryTypeId = Translator.integerValue(detail[17]);
				Calendar cal = Calendar.getInstance();
				if (paymentCycleId == HRISApplicationConstants.YEARLY) {
					netPayable = 0d;
					grossAmount = 0d;
					for (int i = 0; i < 12; i++) {
						cal.setTime(monthFirstDate);
						cal.add(Calendar.MONTH, (-paidAfterMonth));
						cal.add(Calendar.MONTH, (-i));
						long monthId1 = cal.get(Calendar.MONTH) + 1;

						long yearId1 = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
						double[] payableAndGrossAmount = new EmployeeReleasingSalaryServlet().getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1, false, "", 0);
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
						long yearId1 = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
						double[] payableAndGrossAmount = new EmployeeReleasingSalaryServlet().getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1, false, "", 0);
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
						long yearId1 = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
						double[] payableAndGrossAmount = new EmployeeReleasingSalaryServlet().getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1, false, "", 0);
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
						long yearId1 = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
						double[] payableAndGrossAmount = new EmployeeReleasingSalaryServlet().getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1, false, "", 0);
						grossAmount += payableAndGrossAmount[0];
						netPayable += payableAndGrossAmount[1];

					}
				}

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
				Object[] detail1 = new Object[11];
				detail1[0] = netPayable; // net Payable amount
				detail1[1] = grossAmount; // net Gross amount
				detail1[2] = salaryComponentId;
				detail1[3] = employeeSalaryComponentId;
				detail1[4] = componentName;
				detail1[5] = amountPayToId;
				detail1[6] = paymentTypeId;
				detail1[7] = isInFirstSalaryComponent;
				detail1[8] = false;
				detail1[9] = payableExpenseTypeId;
				detail1[10] = supplementryTypeId;
				componentAmountPayableDetail.put(salaryComponentId, detail1);

			}
			JSONArray esiArray = getEsiArray(monthFirstDateString, monthLastDateString);
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
							Long esiYearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
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
							Object payableExpenseTypeId = esiComponentArray.getJSONObject(0).opt("payable_expense_type_id");
							Object supplementryTypeId = esiComponentArray.getJSONObject(0).opt("deductable_supplementary_id");

							Object[] detail = new Object[18];
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
							detail[15] = payableExpenseTypeId;
							detail[16] = supplementryTypeId;
							componentMonthlyDetail.put(salaryComponentId, detail);
						}
					}
				}
			}
			for (Integer salaryComponentId : componentMonthlyDetail.keySet()) {
				Object[] detail = componentMonthlyDetail.get(salaryComponentId);
				double grossAmount = Translator.doubleValue(detail[1]); // net Gross amount
				Object employeeSalaryComponentId = detail[3];
				int paymentTypeId = Translator.integerValue(detail[8]);
				int criteriaId = Translator.integerValue(detail[9]);
				String componentName = Translator.stringValue(detail[10]);
				int amountPayToId = Translator.integerValue(detail[11]);
				double ctcPercentage = Translator.doubleValue(detail[12]);
				int isInFirstSalaryComponent = Translator.integerValue(detail[13]);
				int salaryCompontntTypeId = Translator.integerValue(detail[14]);

				int paybleExpenseTypeId = Translator.integerValue(detail[15]);
				int supplementryId = Translator.integerValue(detail[16]);

				if (criteriaId == HRISApplicationConstants.NET_PAYABLE_BASED) {

					double netPayable = totalSalary - payToGovernment;
					netPayable = (netPayable * ctcPercentage) / 100;
					if ((salaryCompontntTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE || salaryCompontntTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYER) && netPayable > 15000.0 && esiCycle) {
						netPayable = 0.0;
					}
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
					Object[] detail1 = new Object[11];
					detail1[0] = netPayable; // net Payable amount
					detail1[1] = grossAmount; // net Gross amount
					detail1[2] = salaryComponentId;
					detail1[3] = employeeSalaryComponentId;
					detail1[4] = componentName;
					detail1[5] = amountPayToId;
					detail1[6] = paymentTypeId;
					detail1[7] = isInFirstSalaryComponent;
					detail1[8] = esiCycle;
					detail1[9] = paybleExpenseTypeId;
					detail1[10] = supplementryId;
					componentAmountPayableDetail.put(salaryComponentId, detail1);
				}
			}

		}
		return componentAmountPayableDetail;

	}

	private JSONArray getEsiPaidComponentArray(Object employeeId, int esiMonthId, Long esiYearId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and salarymonthid = " + esiMonthId + " and salaryyearid = " + esiYearId);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
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
		columnArray.put("payable_expense_type_id");
		columnArray.put("deductable_supplementary_id");
		columnArray.put("salarycomponenttypeid");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarycomponenttypeid = " + HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("salarycomponents");
		return rows;
	}

	private JSONArray getEsiArray(String monthFirstDateInString, String monthLastDateInString) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_esi_cycle_master");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "from_date <= '" + monthFirstDateInString + "' and  to_date >= '" + monthFirstDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, "from_date <= '" + monthLastDateInString + "' and  to_date >= '" + monthLastDateInString + "'");
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("hris_esi_cycle_master");
		return rows;
	}

	public String cheackReReleasedInvoice(String invoiceNo) throws JSONException {
		String filter = Procurement.ProcurementVendorinvoices.INVOICE_NO + "='" + invoiceNo + "'  AND  " + Procurement.ProcurementVendorinvoices.REFERENCE_NUMBER + "='" + invoiceNo + "' ";
		JSONArray invoiceDetails = getInvoiceDetails(null, null, filter);

		if (invoiceDetails.length() > 0) {
			JSONObject invoiceInfo = invoiceDetails.getJSONObject(0);
			if (invoiceInfo.getString(Procurement.ProcurementVendorinvoices.STATUS + "." + Procurement.ProcurementStatus.STATUS_TYPE).equals(Procurement.ProcurementStatus.Types.PENDING)) {
				return PENDING;
			} else {
				return PAID;
			}
		}
		return INVOICE_NOT_FOUND;
	}

	public static JSONArray getInvoiceDetails(Object id, String queryFilter, String completeFilter) throws JSONException {
		String filter = completeFilter;
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, Procurement.PROCUREMENT_VENDORINVOICES);
		query.put(Data.Query.FILTERS, filter);
		JSONArray columns = new JSONArray();
		columns.put(Procurement.ProcurementVendorinvoices.STATUS + "." + Procurement.ProcurementStatus.STATUS_TYPE);
		query.put(Data.Query.COLUMNS, columns);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(Procurement.PROCUREMENT_VENDORINVOICES);

	}

	public JSONArray getRecordsFromSalarySheet(long employeeId, long monthId, long yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("employeeid.employeecode");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid");
		columnArray.put("monthid");
		columnArray.put("yearid");

		columnArray.put("invoicenumber");
		columnArray.put("invoicedate");

		columnArray.put("attendancebasedamount");
		columnArray.put("performancebasedamount");
		columnArray.put("fixedamount");
		columnArray.put("deductionamount");
		columnArray.put("payableamount");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "monthid = " + monthId + " and yearid = " + yearId + " and employeeid = " + employeeId);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("salarysheet");
		return salarySheetArray;

	}
}