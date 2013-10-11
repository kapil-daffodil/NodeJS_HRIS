package com.applane.resourceoriented.hris;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.InterAppCommunication;
import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneAsyncTaskManager;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.procurement.Procurement;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeReleasingSalaryServlet extends HttpServlet implements ApplaneTask {

	double fixedAmount = 0d;
	double attendanceBasedAmount = 0d;
	double performanceBasedAmount = 0d;
	double experienceBasedAmount = 0d;

	double totalSalary = 0d;
	double payToGovernment = 0d;
	double totalGross = 0d;
	double totalDeductable = 0d;
	boolean updateStatus = true;

	private static final long serialVersionUID = 1L;

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// releaseSalary(req, resp, true);
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

	public void generateSalaryInvokeMethod(Object[] keys) {
		try {
			String selectedKeys = "";
			for (int counter = 0; counter < keys.length; counter++) {
				if (selectedKeys.length() == 0) {
					selectedKeys += keys[counter];
				} else {
					selectedKeys += ("," + keys[counter]);
				}
			}
			if (selectedKeys.length() > 0) {
				JSONArray salarySheetArray = new EmployeeSalaryGenerationServlet().getEmployeeIdFromSalarySheet(selectedKeys);
				{
					if (salarySheetArray != null && salarySheetArray.length() > 0) {
						for (int i = 0; i < salarySheetArray.length(); i++) {
							JSONObject salarySheetObject = salarySheetArray.getJSONObject(i);
							JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(Translator.integerValue(salarySheetObject.opt("employeeid")));
							if (employeeArray != null && employeeArray.length() > 0) {

								long branchId = Translator.integerValue(employeeArray.getJSONObject(0).opt("branchid"));
								long yearId = Translator.integerValue(salarySheetObject.opt("yearid"));
								long monthId = Translator.integerValue(salarySheetObject.opt("monthid"));

								boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.SALARY);
								if (isFreezed) {
									throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
								}

								String monthName = getMonthName(monthId);
								String yearName = getYearName(yearId);
								Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
								Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
								String monthFirstDateInString = Translator.getDateInString(monthFirstDate);
								String monthLastDateInString = Translator.getDateInString(monthLastDate);
								boolean reReleaseInvoice = true;
								releaseSalarySheet(employeeArray, monthId, yearId, monthFirstDate, monthLastDate, monthName, yearName, monthFirstDateInString, monthLastDateInString, reReleaseInvoice);

							}
						}
					}
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			sendMailInCaseOfException(e);
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public void releaseSelectdEmployeeSalaryInvokeMethodFromSetupMonthlyAmount(Object[] keys) {
		try {
			String selectedKeys = "";
			for (int counter = 0; counter < keys.length; counter++) {
				if (selectedKeys.length() == 0) {
					selectedKeys += keys[counter];
				} else {
					selectedKeys += ("," + keys[counter]);
				}
			}
			if (selectedKeys.length() > 0) {
				JSONArray salarySheetArray = getComponentMonthlyAmountToRelease(selectedKeys);
				{
					if (salarySheetArray != null && salarySheetArray.length() > 0) {
						List<Integer> ides = new ArrayList<Integer>();
						for (int i = 0; i < salarySheetArray.length(); i++) {
							JSONObject salarySheetObject = salarySheetArray.getJSONObject(i);
							Integer employeeid = Translator.integerValue(salarySheetObject.opt("employeeid"));
							if (!ides.contains(employeeid)) {
								ides.add(employeeid);
								JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(employeeid);
								if (employeeArray != null && employeeArray.length() > 0) {

									long branchId = Translator.integerValue(employeeArray.getJSONObject(0).opt("branchid"));
									long yearId = Translator.integerValue(salarySheetObject.opt("yearid"));
									long monthId = Translator.integerValue(salarySheetObject.opt("monthid"));

									boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.SALARY);
									if (isFreezed) {
										throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
									}

									String monthName = getMonthName(monthId);
									String yearName = getYearName(yearId);
									Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
									Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
									String monthFirstDateInString = Translator.getDateInString(monthFirstDate);
									String monthLastDateInString = Translator.getDateInString(monthLastDate);
									boolean reReleaseInvoice = false;
									releaseSalarySheet(employeeArray, monthId, yearId, monthFirstDate, monthLastDate, monthName, yearName, monthFirstDateInString, monthLastDateInString, reReleaseInvoice);

								}
							}
						}
					}
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			sendMailInCaseOfException(new RuntimeException(e));
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public JSONArray getComponentMonthlyAmountToRelease(String lesectedKeys) throws JSONException {

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "componentmonthlyamount");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("yearid");
		columnArray.put("monthid");
		columnArray.put("employeeid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " in (" + lesectedKeys + ")");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("componentmonthlyamount");
		return rows;
	}

	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		Object __key__ = null;
		try {
			JSONObject taskInfo = applaneTaskInfo.getTaskInfo();

			Object takeSalarySheetKey = taskInfo.opt("takeSalarySheetKey");
			long monthId = Translator.integerValue(taskInfo.opt("monthid"));
			long yearId = Translator.integerValue(taskInfo.opt("yearid"));
			long branchId = Translator.integerValue(taskInfo.opt("branchid"));
			boolean generateInvoice = Translator.booleanValue(taskInfo.opt("generateInvoice"));
			__key__ = taskInfo.opt(Updates.KEY);

			initiateValuesAndStartRleasing(branchId, yearId, monthId, generateInvoice, __key__, takeSalarySheetKey);
		} catch (Exception e) {
			try {
				sendStatus(new JSONObject().put("Error -->", ExceptionUtils.getExceptionTraceMessage(EmployeeReleasingSalaryServlet.class.getName(), e)), ("Error While Salary Releasing (Employee List) on initiate method " + __key__));
			} catch (Exception jsonException) {
			}
		}
	}

	private void initiateValuesAndStartRleasing(long branchId, long yearId, long monthId, boolean generateInvoice, Object __key__, Object takeSalarySheetKey) throws Exception {

		String monthName = getMonthName(monthId);
		String yearName = getYearName(yearId);
		Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
		String monthFirstDateInString = Translator.getDateInString(monthFirstDate);
		String monthLastDateInString = Translator.getDateInString(monthLastDate);

		JSONArray employeeArray = getActiveAndReleivedEmployeeRecords(branchId, monthFirstDateInString, monthLastDateInString, __key__);
		__key__ = releaseSalarySheet(employeeArray, monthId, yearId, monthFirstDate, monthLastDate, monthName, yearName, monthFirstDateInString, monthLastDateInString, generateInvoice);
		ApplaneAsyncTaskManager.initiateTasksSynchronously();
		if (employeeArray.length() > 0) {
			initiateTaskQueueAgain(branchId, monthId, yearId, generateInvoice, __key__, takeSalarySheetKey);
		} else {
			boolean updateIsReleasing = false;
			PopulateEmployeeSalaryBusinessLogic.updateReleasingStatus(takeSalarySheetKey, updateIsReleasing);
			sendMailOnCompletion();
		}

	}

	private void sendMailOnCompletion() {
		String title = "Rsleasing Salary Process Completed";
		String mailValue = "Dear: " + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		mailValue += "<BR /><BR />";
		mailValue += "Salary Releasing Process Completed";
		mailValue += "<BR /><BR />";
		mailValue += "Regards";
		mailValue += "<BR />";
		mailValue += "Applane";
		try {
			PunchingUtility.sendMailsWithoutCC(new String[] { "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL) }, mailValue, title);
		} catch (Exception e) {
			LogUtility.writeLog("Employee Releasing Salary completion mail failed");
		}
	}

	private void initiateTaskQueueAgain(long branchId, long monthId, long yearId, boolean generateInvoice, Object __key__, Object takeSalarySheetKey) throws JSONException {
		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("monthid", monthId);
		taskQueueParameters.put("yearid", yearId);
		taskQueueParameters.put("branchid", branchId);
		taskQueueParameters.put("takeSalarySheetKey", takeSalarySheetKey);
		taskQueueParameters.put("generateInvoice", "" + generateInvoice);
		taskQueueParameters.put(Updates.KEY, "" + __key__);

		ApplaneTaskService.enQueueTaskInPrallel("com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet", QueueConstantsHRIS.SALARY_PROCESS, taskQueueParameters);

	}

	public Object releaseSalarySheet(JSONArray employeeArray, long monthId, long yearId, Date monthFirstDate, Date monthLastDate, String monthName, String yearName, String monthFirstDateInString, String monthLastDateInString, boolean generateInvoice) throws JSONException {
		int employeeArrayCount = employeeArray == null ? 0 : employeeArray.length();
		Object __key__ = null;
		if (employeeArrayCount > 0) {
			JSONObject employeeListExceptionInCaseOfInvoiceNotGenerated = new JSONObject();
			JSONArray errorWhileReleasingSalaryForEmployees = new JSONArray();
			String employeeNameWhileError = "";
			List<Integer> eList = new ArrayList<Integer>();
			for (int counter = 0; counter < employeeArrayCount; counter++) {
				try {
					JSONObject employeeRecord = employeeArray.getJSONObject(counter);
					Object employeeId = employeeRecord.opt("__key__");
					__key__ = employeeId;

					if (!eList.contains(Translator.integerValue(__key__))) {
						eList.add(Translator.integerValue(__key__));
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
						Object currentBranchid = employeeRecord.opt("subbranchid");
						Object employeecode = employeeRecord.opt("employeecode");
						Object name = employeeRecord.opt("name");
						employeeNameWhileError = "" + name;
						Object accountno = employeeRecord.opt("accountno");
						Object modeOfSalaryId = employeeRecord.opt("modeofsalaryid");
						Object profitcenterid = employeeRecord.opt("profitcenterid");
						Object businessfunctionid = employeeRecord.opt("businessfunctionid");

						if (profitcenterid != null && profitcenterid instanceof JSONArray && ((JSONArray) profitcenterid).length() > 0) {
							profitcenterid = ((JSONArray) profitcenterid).get(0);
						}

						if (businessfunctionid != null && businessfunctionid instanceof JSONArray && ((JSONArray) businessfunctionid).length() > 0) {
							businessfunctionid = ((JSONArray) businessfunctionid).get(0);
						}

						JSONArray branchArray = getBranchArray(employeeId, monthFirstDateInString);
						JSONArray subBranchArray = getSubBranchArray(employeeId, monthFirstDateInString);

						if (branchArray != null && branchArray.length() > 0) {
							branchid = branchArray.getJSONObject(0).opt("branchid");
						}
						if (subBranchArray != null && subBranchArray.length() > 0) {
							currentBranchid = subBranchArray.getJSONObject(0).opt("sub_branchid");
						}
						String invoiceNumber = "" + employeecode + "/" + monthName + "/" + yearName;
						String status = new EmployeeSalaryArrearBusinessLogic().cheackReReleasedInvoice(invoiceNumber);
						if (status.equals(EmployeeSalaryArrearBusinessLogic.PAID)) {
							WarningCollector.collect("[" + name.toString() + " , " + invoiceNumber + "] Invoice is Paid. Please Release Arrear if Required");
							JSONObject errorObject = new JSONObject();
							errorObject.put("Name", employeeNameWhileError);
							errorObject.put("Error", "[" + invoiceNumber + "] Invoice is Paid. Please Release Arrear if Required");

							errorWhileReleasingSalaryForEmployees.put(errorObject);
							// return __key__;
							continue;
						}
						boolean reReleaseInvoice = false;
						if (status.equals(EmployeeSalaryArrearBusinessLogic.INVOICE_NOT_FOUND)) {
							reReleaseInvoice = false;
						}
						if (status.equals(EmployeeSalaryArrearBusinessLogic.PENDING)) {
							reReleaseInvoice = true;
						}

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
								invoiceJsonObject.put("profitcenter_id", profitcenterid);
								invoiceJsonObject.put("officeid", branchid);
								invoiceJsonObject.put("invoiceno", invoiceNumber);
								invoiceJsonObject.put("invoicedate", monthLastDateInString);
								invoiceJsonObject.put("vendorid", employeeId);
								getAndDeleteEmployeeMonthlyComponents(employeeId, monthId, yearId, invoiceNumber);

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

											updateHistory(employeeId, employeeSalaryComponentId, dueMonthId, yearId);

											cal1 = Calendar.getInstance();
											cal2 = Calendar.getInstance();
											cal1.setTime(monthFirstDate);
											cal2.setTime(applicableUpToDate);
											cal2.add(Calendar.MONTH, paidAfterMonth);
											if (paymentCycleId == HRISApplicationConstants.YEARLY) {
												cal1.add(Calendar.MONTH, 12);// +
																				// 1);
												if (cal1.before(cal2) || cal1.equals(cal2)) {
													updateSalaryComponentDueMonth(employeeSalaryComponentId, (cal1.get(Calendar.MONTH) + 1));
												}
											}
											cal1.setTime(monthFirstDate);
											cal2.setTime(applicableUpToDate);
											cal2.add(Calendar.MONTH, paidAfterMonth);
											if (paymentCycleId == HRISApplicationConstants.HALF_YEARLY) {
												cal1.add(Calendar.MONTH, 6);// +
																			// 1);
												if (cal1.before(cal2) || cal1.equals(cal2)) {
													updateSalaryComponentDueMonth(employeeSalaryComponentId, (cal1.get(Calendar.MONTH) + 1));
												}
											}
											cal1.setTime(monthFirstDate);
											cal2.setTime(applicableUpToDate);
											cal2.add(Calendar.MONTH, paidAfterMonth);
											if (paymentCycleId == HRISApplicationConstants.QUARTERLY) {
												cal1.add(Calendar.MONTH, 3);// +
																			// 1);
												if (cal1.before(cal2) || cal1.equals(cal2)) {
													updateSalaryComponentDueMonth(employeeSalaryComponentId, (cal1.get(Calendar.MONTH) + 1));
												}
											}
											cal1.setTime(monthFirstDate);
											cal2.setTime(applicableUpToDate);
											cal2.add(Calendar.MONTH, paidAfterMonth);
											if (paymentCycleId == HRISApplicationConstants.MONTHLY) {
												cal1.add(Calendar.MONTH, 1);// +
																			// paidAfterMonth);
												if (cal1.before(cal2) || cal1.equals(cal2)) {
													updateSalaryComponentDueMonth(employeeSalaryComponentId, (cal1.get(Calendar.MONTH) + 1));
												}

											}
										}
									}
								}
								JSONArray employeeComponentsArrayForInvoice = new JSONArray();
								JSONArray employeeComponentsArrayForInvoiceDeductable = new JSONArray();
								HashMap<Integer, Object[]> deductionDetails = new HashMap<Integer, Object[]>();
								int j = 0;
								for (Integer salaryComponentId : componentMonthlyDetail.keySet()) {
									j++;
									Object[] detail = componentMonthlyDetail.get(salaryComponentId);
									double netPayable = Translator.doubleValue(detail[0]); // net Payable amount
									double grossAmount = Translator.doubleValue(detail[1]); // net Gross amount
									// int salaryComponentId = Translator.integerValue(detail[2]);
									Object employeeSalaryComponentId = detail[3];
									// Integer dueMonthId = Translator.integerValue(detail[5]);
									Object paymentCycleId = detail[4];
									int paidAfterMonth = Translator.integerValue(detail[6]);
									// Date applicableUpToDate = (Date) (detail[7]);
									int paymentTypeId = Translator.integerValue(detail[8]);
									int criteriaId = Translator.integerValue(detail[9]);
									String componentName = Translator.stringValue(detail[10]);
									int amountPayToId = Translator.integerValue(detail[11]);
									int salaryCompontntTypeId = Translator.integerValue(detail[13]);
									int inInFirstComponents = Translator.integerValue(detail[14]);
									int addInESICalculation = Translator.integerValue(detail[15]);

									int payableExpenseTypeId = Translator.integerValue(detail[16]);
									int supplementryTypeId = Translator.integerValue(detail[17]);

									boolean valueForDoubleRecord = false;
									if (paidAfterMonth == 0 && criteriaId != HRISApplicationConstants.NET_PAYABLE_BASED && paymentCycleId == HRISApplicationConstants.MONTHLY) {
										valueForDoubleRecord = true;
									}
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
											double[] payableAndGrossAmount = getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1, valueForDoubleRecord, invoiceNumber, inInFirstComponents);
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
											double[] payableAndGrossAmount = getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1, valueForDoubleRecord, invoiceNumber, inInFirstComponents);
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
											double[] payableAndGrossAmount = getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1, valueForDoubleRecord, invoiceNumber, inInFirstComponents);
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
											double[] payableAndGrossAmount = getAmountFromComponentMonthlyAmount(employeeId, salaryComponentId, monthId1, yearId1, valueForDoubleRecord, invoiceNumber, inInFirstComponents);
											grossAmount += payableAndGrossAmount[0];
											netPayable += payableAndGrossAmount[1];

										}
									}
									if (salaryCompontntTypeId == HRISApplicationConstants.SalaryComponentTypes.LWF_EMPLOYEE) {
										netPayable = 10;
									}
									if (paymentTypeId == HRISApplicationConstants.PAYABLE && criteriaId != HRISApplicationConstants.NET_PAYABLE_BASED && netPayable > 0) {// &&
																																											// amountPayToId
										JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
										tempObjectToGenerateEnvoiceArray.put("expense_type", componentName);
										if (payableExpenseTypeId != 0) {
											tempObjectToGenerateEnvoiceArray.put("expense_type_id", payableExpenseTypeId);
										}
										tempObjectToGenerateEnvoiceArray.put("amount", netPayable);
										tempObjectToGenerateEnvoiceArray.put("title", invoiceNumber + "/" + j);
										tempObjectToGenerateEnvoiceArray.put("business_function_id", businessfunctionid);
										tempObjectToGenerateEnvoiceArray.put("expensedate", monthLastDateInString);
										employeeComponentsArrayForInvoice.put(tempObjectToGenerateEnvoiceArray);
									}
									if ((paymentTypeId == HRISApplicationConstants.DEDUCTABLE || amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) && criteriaId != HRISApplicationConstants.NET_PAYABLE_BASED) {
										if (netPayable > 0) {
											// JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
											// tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.NAME, componentName);
											// tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.AMOUNT, netPayable);
											// employeeComponentsArrayForInvoiceDeductable.put(tempObjectToGenerateEnvoiceArray);

											double amount = netPayable;
											if (deductionDetails.containsKey(salaryComponentId)) {
												amount += Translator.doubleValue(deductionDetails.get(salaryComponentId)[1]);
											}
											deductionDetails.put(salaryComponentId, new Object[] { componentName, amount, supplementryTypeId });
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
									if (criteriaId != HRISApplicationConstants.NET_PAYABLE_BASED && !valueForDoubleRecord) {
										updateCalculatedAmountsInEmployeeMonthlySalary(employeeId, salaryComponentId, monthId, yearId, netPayable, employeeSalaryComponentId, grossAmount, invoiceNumber, inInFirstComponents, false);
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
										j++;
										double grossAmount = Translator.doubleValue(detail[1]); // net Gross amount
										Object employeeSalaryComponentId = detail[3];
										int paymentTypeId = Translator.integerValue(detail[8]);
										String componentName = Translator.stringValue(detail[10]);
										int amountPayToId = Translator.integerValue(detail[11]);
										double ctcPercentage = Translator.doubleValue(detail[12]);
										int salaryCompontntTypeId = Translator.integerValue(detail[13]);
										int inInFirstComponents = Translator.integerValue(detail[14]);
										int payableExpenseTypeId = Translator.integerValue(detail[16]);
										int supplementryTypeId = Translator.integerValue(detail[17]);
										double netPayable = (totalSalary - payToGovernment);
										if ((salaryCompontntTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE || salaryCompontntTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYER) && netPayable > 15000.0 && !esiCycle) {
											netPayable = 0.0;
										}
										netPayable = (netPayable * ctcPercentage) / 100;
										if (paymentTypeId == HRISApplicationConstants.PAYABLE && netPayable > 0) { // && paymentTypeId != HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
											JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
											tempObjectToGenerateEnvoiceArray.put("expense_type", componentName);

											if (payableExpenseTypeId != 0) {
												tempObjectToGenerateEnvoiceArray.put("expense_type_id", payableExpenseTypeId);
											}
											tempObjectToGenerateEnvoiceArray.put("amount", netPayable);
											tempObjectToGenerateEnvoiceArray.put("title", invoiceNumber + "/" + j);
											tempObjectToGenerateEnvoiceArray.put("business_function_id", businessfunctionid);
											tempObjectToGenerateEnvoiceArray.put("expensedate", monthLastDateInString);
											employeeComponentsArrayForInvoice.put(tempObjectToGenerateEnvoiceArray);
										}
										if ((paymentTypeId == HRISApplicationConstants.DEDUCTABLE || amountPayToId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT)) {
											if (netPayable > 0) {

												// JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
												// tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.NAME, componentName);
												// tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.AMOUNT, netPayable);
												// employeeComponentsArrayForInvoiceDeductable.put(tempObjectToGenerateEnvoiceArray);

												double amount = netPayable;
												if (deductionDetails.containsKey(salaryComponentId)) {
													amount += Translator.doubleValue(deductionDetails.get(salaryComponentId)[1]);
												}
												deductionDetails.put(salaryComponentId, new Object[] { componentName, amount, supplementryTypeId });
											}
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
										updateCalculatedAmountsInEmployeeMonthlySalary(employeeId, salaryComponentId, monthId, yearId, netPayable, employeeSalaryComponentId, grossAmount, invoiceNumber, inInFirstComponents, esiCycle);
									}
								}
								for (Integer salaryComponentId : deductionDetails.keySet()) {
									Object[] details = deductionDetails.get(salaryComponentId);
									JSONObject tempObjectToGenerateEnvoiceArray = new JSONObject();
									tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.NAME, details[0]);
									if (Translator.integerValue(details[2]) != 0) {
										tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.NAME_ID, details[2]);
									}
									tempObjectToGenerateEnvoiceArray.put(Procurement.ProcurementSalaryDeduction.DeductionColumns.AMOUNT, details[1]);
									employeeComponentsArrayForInvoiceDeductable.put(tempObjectToGenerateEnvoiceArray);
								}
								JSONObject procurementSalaryDetail = new JSONObject();
								procurementSalaryDetail.put("gross_salary", totalSalary);
								// procurementSalaryDetail.put("tds", arg1);
								// procurementSalaryDetail.put("advance", arg1);
								procurementSalaryDetail.put("payble_amount", totalSalary - totalDeductable);
								procurementSalaryDetail.put("bank_account", accountno);
								procurementSalaryDetail.put("salary_date", monthLastDateInString);
								procurementSalaryDetail.put(Procurement.ProcurementSalaryDetail.SALARY_DEDUCTIONS, employeeComponentsArrayForInvoiceDeductable);
								invoiceJsonObject.put(Procurement.PROCUREMENT_VENDORINVOICELINEITEMS, employeeComponentsArrayForInvoice);
								invoiceJsonObject.put("procurement_salary_detail", procurementSalaryDetail);
								invoiceJsonObject.put(Procurement.RE_RELEASE, reReleaseInvoice);
								try {
									if (generateInvoice && (totalSalary - totalDeductable) > 0) {
										LogUtility.writeLog("invoiceJsonObject>> " + invoiceJsonObject);
										InterAppCommunication.invokeMethod("com.applane.procurement.SalaryDisbursement", "createNewExpenseInvoice", new Object[] { invoiceJsonObject });
									}
								} catch (Exception e) {
									updateStatus = false;
									String exceptionTraceMessage = ExceptionUtils.getExceptionTraceMessage(EmployeeReleasingSalaryServlet.class.getName(), e);
									employeeListExceptionInCaseOfInvoiceNotGenerated.put(invoiceNumber, "[" + exceptionTraceMessage + "] >> " + e.getStackTrace());
								}
								putAttendanceAndSalaryInfo(employeeId, monthId, yearId, employeeRecord, monthlyattendanceArray, invoiceNumber, monthLastDateInString, branchid, currentBranchid);
							} else {
								JSONObject errorObject = new JSONObject();
								errorObject.put("Name", employeeNameWhileError);
								errorObject.put("Salary Components", "- Not Found");
								errorWhileReleasingSalaryForEmployees.put(errorObject);
							}
						} else {
							JSONObject errorObject = new JSONObject();
							errorObject.put("Name", employeeNameWhileError);
							errorObject.put("Monthly Attendance", "- Not Found");
							errorWhileReleasingSalaryForEmployees.put(errorObject);
						}
					}
				} catch (Exception e) {
					JSONObject errorObject = new JSONObject();
					errorObject.put("Name", employeeNameWhileError);
					// errorObject.put("Error", e.getStackTrace());
					errorObject.put("Error", ExceptionUtils.getExceptionTraceMessage(EmployeeReleasingSalaryServlet.class.getName(), e));

					errorWhileReleasingSalaryForEmployees.put(errorObject);
				}
			}
			if (employeeListExceptionInCaseOfInvoiceNotGenerated.length() > 0) {
				sendStatus(employeeListExceptionInCaseOfInvoiceNotGenerated, "Invoice Generation Error");
			}
			if (errorWhileReleasingSalaryForEmployees.length() > 0) {
				sendStatus(new JSONObject().put("Error -->", errorWhileReleasingSalaryForEmployees), "Error While Salary Releasing (Employee List)");
			}
		}
		return __key__;
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

	private JSONArray getSubBranchArray(Object employeeId, String monthFirstDateInString) throws Exception {
		JSONArray filters = new JSONArray();
		filters.put("employeeid = " + employeeId + " AND from_date = null AND to_date<='" + monthFirstDateInString + "'");
		filters.put("employeeid = " + employeeId + " AND to_date = null AND from_date>='" + monthFirstDateInString + "'");

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_sub_branch_id_history");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("sub_branchid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND from_date >='" + monthFirstDateInString + "' AND to_date <= '" + monthFirstDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, filters);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees_sub_branch_id_history");
		return employeeArray;
	}

	private JSONArray getBranchArray(Object employeeId, String monthFirstDateInString) throws Exception {
		JSONArray filters = new JSONArray();
		filters.put("employeeid = " + employeeId + " AND from_date = null AND to_date<='" + monthFirstDateInString + "'");
		filters.put("employeeid = " + employeeId + " AND to_date = null AND from_date>='" + monthFirstDateInString + "'");

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_branch_id_history");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("branchid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND from_date >='" + monthFirstDateInString + "' AND to_date <= '" + monthFirstDateInString + "'");
		query.put(Data.Query.OPTION_FILTERS, filters);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees_branch_id_history");
		return employeeArray;
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

	public void updateHistory(Object employeeId, Object employeeSalaryComponentId, long monthId, long yearId) throws JSONException {
		Object paymentHistoryRecordKey = getPaymentHistoryRecord(employeeId, employeeSalaryComponentId, monthId, yearId);
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "paymenthistory");
		JSONObject row = new JSONObject();
		if (paymentHistoryRecordKey == null) {
			// row.put("__key__", paymentHistoryRecordKey);
			row.put("paymentmonthid", monthId);
			row.put("paymentyearid", yearId);
			row.put("employeeid", employeeId);
			row.put("employeesalarycomponentid", employeeSalaryComponentId);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		}
	}

	private Object getPaymentHistoryRecord(Object employeeId, Object employeeSalaryComponentId, long monthId, long yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "paymenthistory");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and paymentyearid = " + yearId + " and paymentmonthid = " + monthId + " and employeesalarycomponentid =" + employeeSalaryComponentId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("paymenthistory");
		if ((rows == null ? 0 : rows.length()) > 0) {
			return rows.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}

	}

	public double[] getAmountFromComponentMonthlyAmount(Object employeeId, int salaryComponentId, long monthId, long yearId, boolean forDoubleRecords, String invoiceNumber, int inInFirstComponents) throws JSONException {

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
				if (forDoubleRecords) {
					updateCalculatedAmountsInEmployeeMonthlySalary(employeeId, salaryComponentId, monthId, yearId, Translator.doubleValue(rows.getJSONObject(i).opt("payableamount")), rows.getJSONObject(i).opt("employeesalarycomponentid"), Translator.doubleValue(rows.getJSONObject(i).opt("grossamount")), invoiceNumber, Translator.doubleValue(rows.getJSONObject(i).opt("isinfirstcomponents")), false);
				}
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

	private void putAttendanceAndSalaryInfo(Object employeeId, long monthId, long yearId, JSONObject employeeRecord, JSONArray monthlyattendanceArray, String invoiceNumber, String monthLastDateInString, Object branchid, Object currentBranchid) throws JSONException {

		// Object monthlyAttendanceId =
		// monthlyattendanceArray.getJSONObject(0).opt("__key__");
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
		Object leavePolicyId = employeeRecord.opt("leavepolicyid");
		Object holidayCalendarId = employeeRecord.opt("holidaycalendarid");
		Object scheduleId = employeeRecord.opt("employeescheduleid");

		// insert attandance in salary sheet i.e leaves, absent ewd etc
		insertAttendanceInSalarySheet(employeeId, leavePolicyId, holidayCalendarId, scheduleId, monthId, yearId, presentDays, absentDays, monthlyLeaves, extraWorkingDays, totalWorkingDays, totalHoliday, totalOffs, actualNonWorkingDays, nonPayableLeaves, monthlyClosingBalance, paidAbsents, invoiceNumber, monthLastDateInString, branchid, currentBranchid);
	}

	private void getAndDeleteEmployeeMonthlyComponents(Object employeeId, Object monthId, Object yearId, String invoiceNumber) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and salarymonthid = " + monthId + " and salaryyearid = " + yearId + " and invoicenumber = '" + invoiceNumber + "'");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray array = employeeObject.getJSONArray("employeemonthlysalarycomponents");

		for (int counter = 0; counter < array.length(); counter++) {
			Object key = array.getJSONObject(counter).opt(Updates.KEY);
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
			JSONObject row = new JSONObject();
			row.put("__key__", key);
			row.put("__type__", "delete");
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		}
	}

	public static void updateCalculatedAmountsInEmployeeMonthlySalary(Object employeeId, Object salaryComponentId, long monthId, long yearId, double payableAmount, Object employeeSalaryComponentId, double grossAmount, String invoiceNumber, double isinFirstComponents, boolean esiCycle) throws JSONException {
		updateEmployeMonthlySalaryComponent(null, employeeId, employeeSalaryComponentId, salaryComponentId, monthId, yearId, payableAmount, grossAmount, invoiceNumber, isinFirstComponents, esiCycle);
		// JSONArray array = monthlySalaryComponentsRecords(employeeId,
		// salaryComponentId, monthId, yearId, invoiceNumber,
		// employeeSalaryComponentId);
		// int arrayCount = (array == null || array.length() == 0) ? 0 :
		// array.length();
		// if (arrayCount > 0) {
		// Object monthlySalaryComponentId =
		// array.getJSONObject(0).opt("__key__");
		// if (monthlySalaryComponentId != null) {
		// updateEmployeMonthlySalaryComponent(monthlySalaryComponentId,
		// employeeId, employeeSalaryComponentId, salaryComponentId, monthId,
		// yearId, payableAmount, grossAmount, invoiceNumber);
		// }
		// } else {
		// updateEmployeMonthlySalaryComponent(null, employeeId,
		// employeeSalaryComponentId, salaryComponentId, monthId, yearId,
		// payableAmount, grossAmount, invoiceNumber);
		// }
	}

	private static void updateEmployeMonthlySalaryComponent(Object key, Object employeeId, Object employeeSalaryComponentId, Object salaryComponentId, long monthId, long yearId, double payableAmount, double grossAmount, String invoiceNumber, double isinFirstComponents, boolean esiCycle) {
		try {
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
			JSONObject row = new JSONObject();
			if (key != null) {
				row.put("__key__", key);
			}
			row.put("employeeid", employeeId);
			row.put("employeesalarycomponentid", employeeSalaryComponentId);
			row.put("salarycomponentid", salaryComponentId);
			row.put("salarymonthid", monthId);
			row.put("salaryyearid", yearId);
			row.put("amount", payableAmount);
			row.put("actualamount", grossAmount);
			row.put("invoicenumber", invoiceNumber);
			row.put("isinfirstcomponents", isinFirstComponents);
			row.put("is_esi_cycle", esiCycle);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		} catch (Exception e) {
			throw new RuntimeException("Error occured while updating Employe Monthly Salary Component: " + e.getMessage());
		}
	}

	public void insertAttendanceInSalarySheet(Object employeeId, Object leavePolicyId, Object holidayCalendarId, Object scheduleId, long monthId, long yearId, Object presentDays, Object absentDays, Object leaves, Object extraWorkingDays, double totalWorkingDays, double totalHoliday, double totalOffs, double actualNonWorkingDays, double nonPayableLeaves, double monthlyClosingBalance, double paidAbsent, String invoiceNumber, String monthLastDateInString, Object branchid, Object currentBranchid) throws JSONException {
		Object __key__ = getSalarySheetRecordIfExist(employeeId, monthId, yearId);
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "salarysheet");
		JSONObject row = new JSONObject();
		if (__key__ != null) {
			row.put("__key__", __key__);
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
		row.put("leavepolicyid", leavePolicyId);
		row.put("holidaycalendarid", holidayCalendarId);
		row.put("scheduleid", scheduleId);
		row.put("branchid", branchid);
		row.put("sub_branchid", currentBranchid);
		// int organizationId = Translator.integerValue(CurrentState.getCurrentState().get(CurrentSession.CURRENT_ORGANIZATION_ID));
		// if (organizationId == 7783) {
		// totalDeductable += ((float) Translator.doubleValue(absentDays) * 750);
		// }
		row.put("invoicenumber", invoiceNumber);
		if (updateStatus) {
			row.put("status", 1);
		}
		row.put("invoicedate", monthLastDateInString);
		row.put("payableamount", totalSalary - totalDeductable);
		row.put("attendancebasedamount", attendanceBasedAmount);
		row.put("performancebasedamount", performanceBasedAmount);
		row.put("fixedamount", fixedAmount);
		row.put("deductionamount", totalDeductable);

		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private Object getSalarySheetRecordIfExist(Object employeeId, long monthId, long yearId) throws JSONException {

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and yearid = " + yearId + " and  monthid = " + monthId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("salarysheet");
		if (rows.length() > 0) {
			return rows.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}

	}

	public static void updateSalaryComponentPaymentHistory(Object employeeId, Object employeesalarycomponentId, long monthId, long yearId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "paymenthistory");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("employeesalarycomponentid", employeesalarycomponentId);
		row.put("paymentmonthid", monthId);
		row.put("paymentyearid", yearId);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public void updateSalaryComponentDueMonth(Object employeeSalaryComponentId, int duemonthId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeSalaryComponentId);
		row.put("duemonthid", duemonthId);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	// private static JSONArray monthlySalaryComponentsRecords(Object
	// employeeId, Object salaryComponentId, long monthId, long yearId, String
	// invoiceNumber, Object employeeSalaryComponentId) throws JSONException {
	// JSONObject query = new JSONObject();
	// query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
	// JSONArray columnArray = new JSONArray();
	// columnArray.put("__key__");
	// query.put(Data.Query.COLUMNS, columnArray);
	// query.put(Data.Query.FILTERS, "employeeid = " + employeeId +
	// " and salarymonthid = " + monthId + " and salaryyearid = " + yearId +
	// " and salarycomponentid = " + salaryComponentId +
	// " and invoicenumber = '" + invoiceNumber +
	// "' and employeesalarycomponentid = " + employeeSalaryComponentId);
	// JSONObject employeeObject;
	// employeeObject = ResourceEngine.query(query);
	// JSONArray array =
	// employeeObject.getJSONArray("employeemonthlysalarycomponents");
	// return array;
	// }

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
			Long yearId = ((Number) rows.getJSONObject(0).opt("__key__")).longValue();
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
				Long monthId = ((Number) rows.getJSONObject(0).opt("__key__")).longValue();
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

	public static JSONArray getActiveAndReleivedEmployeeRecords(long branchId, String monthFirstDateInString, String monthLastDateInString, Object __key__) {
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
			columnArray.put("businessfunctionid");
			columnArray.put("accountno");
			columnArray.put("modeofsalaryid");
			query.put(Data.Query.COLUMNS, columnArray);
			// query.put(Data.Query.FILTERS, "__key__ = 11");
			String extraFilter = "";
			if (__key__ != null) {
				extraFilter = " and " + Updates.KEY + " > " + __key__;
			}
			query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + " and branchid = " + branchId + extraFilter + " AND salary_on_hold != 1");
			query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and branchid = " + branchId + " and relievingdate >= '" + monthFirstDateInString + "'" + extraFilter + " AND salary_on_hold != 1"); // +
			// "' and relievingdate <= '"
			// +
			// monthLastDateInString
			// + "'");
			query.put(Data.Query.MAX_ROWS, 1);
			JSONObject employeeObject;
			employeeObject = ResourceEngine.query(query);
			JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
			return employeeArray;
		} catch (JSONException e) {
			e.printStackTrace();
			throw new RuntimeException("Error occured while fetching active and releived employee records" + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error occured while fetching active and releived employee records" + e.getMessage());
		}
	}
}
