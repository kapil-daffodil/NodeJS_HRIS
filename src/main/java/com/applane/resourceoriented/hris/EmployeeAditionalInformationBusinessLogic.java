package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeAditionalInformationBusinessLogic implements OperationJob {

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		String operationType = record.getOperationType();
		if (operationType.equals(Updates.Types.UPDATE) || operationType.equals(Updates.Types.INSERT)) {
			try {
				if (record.has("specialremarks")) {
					Object key = record.getKey();
					String specialRemarks = Translator.stringValue(record.getValue("specialremarks"));
					updateSpecialRemarksHistory(key, specialRemarks);
					// String oldRemarks = Translator.stringValue(record.getOldValue("specialremarks"));
					// String updatedRemarks = "Old Vallue: [" + oldRemarks + "] - New Remarks: [" + newRemarks;
				}
				if (record.has("financialyearid")) {
					Object employeeId = record.getValue("employeeid");
					Object financialYearId = record.getValue("financialyearid");
					Object fromDateObject = record.getValue("fromdate");
					Object toDateObject = record.getValue("todate");
					if (employeeId == null) {
						employeeId = record.getValue("employeeid.__key__");
					}

					Date fromDate = Translator.dateValue(fromDateObject);
					Date toDate = Translator.dateValue(toDateObject);
					record.removeUpdate("fromdate");
					record.removeUpdate("todate");
					JSONArray employeeAdditionalInformation = getEmployeeAdditionalInformation(employeeId);
					if (employeeAdditionalInformation != null && employeeAdditionalInformation.length() > 0) {
						record.addUpdate(Updates.KEY, employeeAdditionalInformation.getJSONObject(0).opt(Updates.KEY));
						record.addUpdate(Updates.TYPE, Updates.Types.UPDATE);
					}
					double totalAmount = 0.0;
					double totalBasicAmount = 0.0;
					double totalHRAAmount = 0.0;
					JSONArray employeeSalaryComponents = EmployeeSalaryGenerationServlet.getEmployeeSalaryComponents(EmployeeSalaryGenerationServlet.getDateInString(fromDate), EmployeeSalaryGenerationServlet.getDateInString(toDate), employeeId);
					for (int counter = 0; counter < employeeSalaryComponents.length(); counter++) {
						int paymentTypeId = Translator.integerValue(employeeSalaryComponents.getJSONObject(counter).opt("salarycomponentid." + SalaryComponentKinds.PAYMENT_TYPE_ID));
						int componentTypeId = Translator.integerValue(employeeSalaryComponents.getJSONObject(counter).opt("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID));
						double amount = Translator.doubleValue(employeeSalaryComponents.getJSONObject(counter).opt("amount"));
						if (paymentTypeId != HRISApplicationConstants.DEDUCTABLE && amount > 0.0) {
							Date componentFromDateObject = Translator.dateValue(employeeSalaryComponents.getJSONObject(counter).opt("applicablefrom"));
							Date componentToDateObject = Translator.dateValue(employeeSalaryComponents.getJSONObject(counter).opt("applicableto"));
							// String componentName = Translator.stringValue(employeeSalaryComponents.getJSONObject(counter).opt("salarycomponentid." + SalaryComponentKinds.NAME));
							if (componentFromDateObject.before(fromDate)) {
								componentFromDateObject = fromDate;
							}
							if (componentToDateObject.after(toDate)) {
								componentToDateObject = toDate;
							}
							long months = DataTypeUtilities.monthsBetweenDates(componentFromDateObject, componentToDateObject);
							Calendar cal = Calendar.getInstance();
							for (int monthsCounter = 0; monthsCounter < months; monthsCounter++) {
								cal.setTime(componentFromDateObject);
								cal.add(Calendar.MONTH, monthsCounter);
								String monthName = EmployeeSalaryGenerationServlet.getMonthName((cal.get(Calendar.MONTH) + 1));
								Date monthFirstDateThisMonth = DataTypeUtilities.getMonthFirstDate("" + cal.get(Calendar.YEAR), monthName);
								Date monthLastDateThisMonth = DataTypeUtilities.getMonthLastDate(monthFirstDateThisMonth);

								if (componentFromDateObject.after(monthFirstDateThisMonth)) {
									monthFirstDateThisMonth = componentFromDateObject;
								}
								if (componentToDateObject.before(monthLastDateThisMonth)) {
									monthLastDateThisMonth = componentToDateObject;
								}

								long numberOfDaysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDateThisMonth);
								long diff = (DataTypeUtilities.differenceBetweenDates(monthFirstDateThisMonth, monthLastDateThisMonth) + 1);
								double percentage = ((float) diff * 100) / numberOfDaysInMonth;
								double calculatedAmount = ((float) (amount * percentage) / 100);
								if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.BASIC) {
									totalBasicAmount += calculatedAmount;
								}
								if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.HRA) {
									totalHRAAmount += calculatedAmount;
								}
								totalAmount += calculatedAmount;
							}
						}
					}
					double monthlySalary = (float) totalAmount / 12;
					long months = DataTypeUtilities.monthsBetweenDates(fromDate, toDate);
					Calendar cal = Calendar.getInstance();
					double totalTaxPaid = 0.0;
					double totalTransportAllowanceAmountPaid = 0.0;
					int paidForMonths = 0;
					for (int monthsCounter = 0; monthsCounter < months; monthsCounter++) {
						cal.setTime(fromDate);
						cal.add(Calendar.MONTH, monthsCounter);
						long monthId = EmployeeReleasingSalaryServlet.getMonthId(EmployeeSalaryGenerationServlet.getMonthName((cal.get(Calendar.MONTH) + 1)));
						long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
						JSONArray salarySheet = getSalarySheet(employeeId, monthId, yearId);
						double paidMonthlyAmount = 0.0;
						if (salarySheet != null && salarySheet.length() > 0) {
							for (int counter = 0; counter < salarySheet.length(); counter++) {
								// paidMonthlyAmount += Translator.doubleValue(salarySheet.getJSONObject(counter).opt("payableamount"));
								paidMonthlyAmount += Translator.doubleValue(salarySheet.getJSONObject(counter).opt("attendancebasedamount"));
								paidMonthlyAmount += Translator.doubleValue(salarySheet.getJSONObject(counter).opt("performancebasedamount"));
								paidMonthlyAmount += Translator.doubleValue(salarySheet.getJSONObject(counter).opt("fixedamount"));
							}
							JSONArray taxPaidAmountArray = getTaxPaidAmountArray(employeeId, monthId, yearId);
							paidForMonths++;
							if (taxPaidAmountArray != null && taxPaidAmountArray.length() > 0) {
								for (int counter = 0; counter < taxPaidAmountArray.length(); counter++) {
									int componentTypeID = Translator.integerValue(taxPaidAmountArray.getJSONObject(counter).opt("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID));
									if (componentTypeID == HRISApplicationConstants.SalaryComponentTypes.TDS) {
										totalTaxPaid += Translator.doubleValue(taxPaidAmountArray.getJSONObject(counter).opt("amount"));
									} else if (componentTypeID == HRISApplicationConstants.SalaryComponentTypes.TA) {
										totalTransportAllowanceAmountPaid += Translator.doubleValue(taxPaidAmountArray.getJSONObject(counter).opt("amount"));
									}
								}
							}
							if (paidMonthlyAmount > monthlySalary) {
								totalAmount = totalAmount + (paidMonthlyAmount - monthlySalary);
							} else {
								totalAmount = totalAmount - (monthlySalary - paidMonthlyAmount);
							}
						}
					}
					JSONArray tdsDeclarationByEmployee = getTdsDeclarationByEmployee(employeeId, financialYearId);
					JSONArray tdsRules = getTdsRules(financialYearId);
					double taxAbleAmount = 0.0;
					double taxAmountPerAnume = 0.0;
					boolean calculate = true;
					if (tdsRules != null && tdsRules.length() > 0) {
						Object taxPercentageDeclaration = tdsRules.getJSONObject(0).opt(HRISApplicationConstants.TDSRulesPercentages.RESOURCE);
						JSONArray taxPercentageDeclarationArray = new JSONArray();
						if (taxPercentageDeclaration instanceof JSONArray) {
							taxPercentageDeclarationArray = (JSONArray) taxPercentageDeclaration;
						} else if (taxPercentageDeclaration instanceof JSONObject) {
							taxPercentageDeclarationArray.put((JSONObject) taxPercentageDeclaration);
						}
						Map<Double, Double[]> taxPercentageDeclarationMap = new HashMap<Double, Double[]>();
						List<Double> toAmountList = new ArrayList<Double>();
						if (taxPercentageDeclarationArray != null && taxPercentageDeclarationArray.length() > 0) {
							double rebateByGovernment = Translator.doubleValue(tdsRules.getJSONObject(0).opt(HRISApplicationConstants.TDSRules.REBATE_BY_GOVERNMENT));
							for (int counter = 0; counter < taxPercentageDeclarationArray.length(); counter++) {
								double fromAmount = Translator.doubleValue(taxPercentageDeclarationArray.getJSONObject(counter).opt(HRISApplicationConstants.TDSRulesPercentages.FROM_AMOUNT));
								double toAmount = Translator.doubleValue(taxPercentageDeclarationArray.getJSONObject(counter).opt(HRISApplicationConstants.TDSRulesPercentages.TO_AMOUNT));
								double percentage = Translator.doubleValue(taxPercentageDeclarationArray.getJSONObject(counter).opt(HRISApplicationConstants.TDSRulesPercentages.PERCENTAGE));
								toAmountList.add(toAmount);
								taxPercentageDeclarationMap.put(toAmount, new Double[] { fromAmount, percentage });
							}
							Collections.sort(toAmountList);

							if (tdsDeclarationByEmployee != null && tdsDeclarationByEmployee.length() > 0) {
								if (totalAmount > 0) {
									taxAbleAmount = calculateTaxAbleAmount(tdsDeclarationByEmployee, totalAmount, tdsRules, totalHRAAmount, totalBasicAmount);
								}
							}
							if (toAmountList.size() > 0 && taxAbleAmount > 0.0) {
								for (int counter = 0; counter < toAmountList.size(); counter++) {
									double toAmount = toAmountList.get(counter);
									double amoutToCalculateTax = 0.0;
									if (taxAbleAmount <= toAmount && counter == 0) {
										Double[] details = taxPercentageDeclarationMap.get(toAmount);
										double percentage = details[1];
										taxAbleAmount = taxAbleAmount - rebateByGovernment;
										taxAmountPerAnume += taxAbleAmount == 0.0 ? 0.0 : ((float) (taxAbleAmount * percentage) / 100);
										break;
									} else {
										Double[] details = taxPercentageDeclarationMap.get(toAmount);
										double fromAmount = details[0];
										double percentage = details[1];
										boolean isBreakTime = false;
										if (taxAbleAmount > toAmount) {
											amoutToCalculateTax = toAmount;
										} else {
											amoutToCalculateTax = taxAbleAmount - fromAmount;
											isBreakTime = true;
										}
										if (counter == 0) {
											amoutToCalculateTax = amoutToCalculateTax - rebateByGovernment;
										}
										taxAmountPerAnume += amoutToCalculateTax == 0.0 ? 0.0 : ((float) (amoutToCalculateTax * percentage) / 100);
										if (isBreakTime) {
											break;
										}
									}
								}
							}

							if (taxAbleAmount < 0.0) {
								taxAbleAmount = 0.0;
							}

							// taxAmountPerAnume = taxAbleAmount == 0.0 ? 0.0 : ((float) (taxAbleAmount * percentage) / 100);

							double actualTax = taxAmountPerAnume - totalTaxPaid;
							double taxPerMonth = (actualTax / (12 - paidForMonths));
							cal = Calendar.getInstance();
							cal.setTime(fromDate);
							cal.add(Calendar.MONTH, paidForMonths);
							int monthId = cal.get(Calendar.MONTH) + 1;
							String monthName = EmployeeSalaryGenerationServlet.getMonthName(monthId);

							Date monthFirstDateThisMonth = DataTypeUtilities.getMonthFirstDate("" + cal.get(Calendar.YEAR), monthName);
							// Date monthLastDateThisMonth = DataTypeUtilities.getMonthLastDate(monthFirstDateThisMonth);
							Object componentId = getTDSComponentId();

							if (componentId != null && componentId instanceof JSONArray && ((JSONArray) componentId).length() > 0 && calculate) {
								componentId = ((JSONArray) componentId).getJSONObject(0).opt(Updates.KEY);
								String fromDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDateThisMonth);
								String toDateInString = EmployeeSalaryGenerationServlet.getDateInString(toDate);
								Object key = null;
								key = new EmployeeSalaryArrearBusinessLogic().getEmployeeSalaryComponentKey(Translator.integerValue(employeeId), componentId, EmployeeSalaryGenerationServlet.getDateInString(monthFirstDateThisMonth), EmployeeSalaryGenerationServlet.getDateInString(toDate));
								if (key == null) {
									JSONArray keyForUpdate = getComponentIdIfDefintdInRange(employeeId, fromDateInString, toDateInString, componentId);
									if (keyForUpdate != null && keyForUpdate.length() > 0) {
										key = keyForUpdate.getJSONObject(0).opt(Updates.KEY);
										cal.setTime(monthFirstDateThisMonth);
										cal.add(Calendar.DATE, -1);
										String previousDate = EmployeeSalaryGenerationServlet.getDateInString(cal.getTime());
										updateComponentUpToDate(key, previousDate);
										key = null;
									}
								}
								EmployeeNameBusinessLogic.insertEmployeeSalaryPackageComponentsDetails(key, employeeId, componentId, EmployeeSalaryGenerationServlet.getDateInString(monthFirstDateThisMonth), EmployeeSalaryGenerationServlet.getDateInString(toDate), taxPerMonth, monthId, 0.0, 0.0);
							}
						}
					}
				}
			} catch (Exception e) {
				String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeAditionalInformationBusinessLogic.class.getName(), e);
				LogUtility.writeLog("EmployeeAditionalInformationBusinessLogic exception >> " + trace);
				throw new BusinessLogicException("Some Unknown Error Occurred, Please Try After Some Time or Contact to Admin.");
			}
		}
	}

	private JSONArray getComponentIdIfDefintdInRange(Object employeeId, String fromDateInString, String toDateInString, Object componentId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.salarycomponenttypeid"); // basic=8
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid.paymentcycleid");// Yearly, Half
		columnArray.put("salarycomponentid.paymenttypeid");// Payable,Deductable
		columnArray.put("salarycomponentid.componentcriteriaid"); // Fixed, Attendance Based, Performance Based
		columnArray.put("salarycomponentid.keyperformanceindicatorid");// KPI belongs to organization level salary components
		columnArray.put("salarycomponentid.performancetypeid");// Performace type i.e. target achieved and performance percentage
		columnArray.put("salarycomponentid.paidaftermonth");
		columnArray.put("duemonthid");
		columnArray.put("amount");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, "applicablefrom");
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		orders.put(orderObject);
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom < '" + fromDateInString + "' and applicableto > '" + fromDateInString + "' and salarycomponentid=" + componentId);
		query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId + " and applicablefrom < '" + toDateInString + "' and applicableto > '" + toDateInString + "' and salarycomponentid=" + componentId);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("employeesalarycomponents");

		String keys = "";
		for (int counter = 0; counter < rows.length(); counter++) {
			int key = Translator.integerValue(rows.getJSONObject(counter).opt("__key__"));
			if (keys.length() > 0) {
				keys += ("," + key);
			} else {
				keys += ("" + key);
			}
		}
		String filters = "";
		if (keys.length() > 0) {
			filters += (" and __key__ not in (" + keys + ")");
		}
		query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid.salarycomponenttypeid"); // basic=8
		columnArray.put("salarycomponentid.paymentcycleid");// Yearly, Half
		columnArray.put("salarycomponentid.paymenttypeid");// Payable,Deductable
		columnArray.put("salarycomponentid.componentcriteriaid"); // Fixed, Attendance Based, Performance Based
		columnArray.put("salarycomponentid.keyperformanceindicatorid");// KPI belongs to organization level salary components
		columnArray.put("salarycomponentid.performancetypeid");// Performace type i.e. target achieved and performance percentage
		columnArray.put("salarycomponentid.paidaftermonth");
		columnArray.put("duemonthid");
		columnArray.put("amount");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom >= '" + fromDateInString + "' and applicableto <= '" + toDateInString + "' and salarycomponentid=" + componentId + filters);
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows1 = object.getJSONArray("employeesalarycomponents");

		for (int counter = 0; counter < rows1.length(); counter++) {
			JSONObject object1 = rows1.getJSONObject(counter);
			rows.put(object1);
		}

		return rows;
	}

	private void updateComponentUpToDate(Object key, String previousDate) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, key);
		row.put("applicableto", previousDate);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private Object getTDSComponentId() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, SalaryComponentKinds.COMPONENT_TYPE_ID + " = " + HRISApplicationConstants.SalaryComponentTypes.TDS);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("salarycomponents");
		return salarySheetArray;
	}

	private JSONArray getTaxPaidAmountArray(Object employeeId, long monthId, long yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("employeeid");
		columnArray.put("amount");
		columnArray.put("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and salarymonthid=" + monthId + " and salaryyearid=" + yearId + " and salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID + " IN(" + HRISApplicationConstants.SalaryComponentTypes.TDS + "," + HRISApplicationConstants.SalaryComponentTypes.TA + ")");
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("employeemonthlysalarycomponents");
		return salarySheetArray;
	}

	private JSONArray getSalarySheet(Object employeeId, long monthId, long yearId) throws JSONException {

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("invoicenumber");
		columnArray.put("payableamount");

		columnArray.put("attendancebasedamount");
		columnArray.put("performancebasedamount");
		columnArray.put("fixedamount");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid=" + monthId + " and yearid=" + yearId);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("salarysheet");
		return salarySheetArray;
	}

	private double calculateTaxAbleAmount(JSONArray tdsDeclarationByEmployee, double totalAmount, JSONArray tdsRules, double totalHRAAmount, double totalBasicAmount) throws JSONException {
		double sqd = Translator.doubleValue(tdsRules.getJSONObject(0).opt(HRISApplicationConstants.TDSRules.SQD));
		double sqr = Translator.doubleValue(tdsRules.getJSONObject(0).opt(HRISApplicationConstants.TDSRules.SQR));
		double childrenEducationAllowance = Translator.doubleValue(tdsRules.getJSONObject(0).opt(HRISApplicationConstants.TDSRules.CHILDREN_EDUCATION_ALLOWANCE));
		int maxChildrenAllowed = Translator.integerValue(tdsRules.getJSONObject(0).opt(HRISApplicationConstants.TDSRules.MAX_CHILDREN_ALLOWED));

		double totalSQDamount = 0.0;
		double totalSQRamount = 0.0;

		double A_80_D = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.A_80_D));
		double A_80_DD = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.A_80_DD));
		double A_80_DDB = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.A_80_DDB));
		double A_80_E = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.A_80_E));
		double A_80_U = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.A_80_U));
		totalSQDamount = A_80_D + A_80_DD + A_80_DDB + A_80_E + A_80_U;

		double B_80_C_LIC = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_LIC));
		double B_80_C_PPF = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_PPF));
		double B_80_C_PF = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_PF));
		double B_80_C_NATIONAL_SAVING = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_NATIONAL_SAVING));
		double B_80_C_PENSION_SCHEME = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_PENSION_SCHEME));
		double B_80_C_DEPOSIT_IN_5_YEAR_TERM = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_DEPOSIT_IN_5_YEAR_TERM));
		double B_80_C_HOUSE_LONE = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_HOUSE_LONE));
		double B_80_C_TUTION_FEES = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_TUTION_FEES));
		double B_80_C_FIXED_DEPOSIT = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_FIXED_DEPOSIT));
		double B_80_C_EQUITY_LINKED_SAVING = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_EQUITY_LINKED_SAVING));
		totalSQRamount = B_80_C_LIC + B_80_C_PPF + B_80_C_PF + B_80_C_NATIONAL_SAVING + B_80_C_PENSION_SCHEME + B_80_C_DEPOSIT_IN_5_YEAR_TERM + B_80_C_HOUSE_LONE + B_80_C_TUTION_FEES + B_80_C_FIXED_DEPOSIT + B_80_C_EQUITY_LINKED_SAVING;

		Object loanInterestDeclaration = tdsDeclarationByEmployee.getJSONObject(0).opt("employee_loandetails_for_incometax");
		double totalInterestAmount = 0.0;
		if (loanInterestDeclaration != null) {
			JSONArray lonInterestDeclarationArray = new JSONArray();
			if (loanInterestDeclaration instanceof JSONArray) {
				lonInterestDeclarationArray = (JSONArray) loanInterestDeclaration;
			} else if (loanInterestDeclaration instanceof JSONObject) {
				lonInterestDeclarationArray.put((JSONObject) loanInterestDeclaration);
			}
			if (lonInterestDeclarationArray != null) {
				for (int counter = 0; counter < lonInterestDeclarationArray.length(); counter++) {
					totalInterestAmount += Translator.doubleValue(lonInterestDeclarationArray.getJSONObject(counter).opt("interest_amount_amount"));
				}
			}
		}

		double hraTotal = Translator.doubleValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.HRA_TOTAL));
		double hraDeduction = 0.0;
		if (hraTotal > 0.0) {
			hraTotal -= (((float) totalBasicAmount * 10) / 100);
			double basic_40_Percent = (((float) totalBasicAmount * 40) / 100);
			if (basic_40_Percent < hraTotal && basic_40_Percent < totalHRAAmount) {
				hraDeduction = basic_40_Percent;
			} else if (hraTotal < basic_40_Percent && hraTotal < totalHRAAmount) {
				hraDeduction = hraTotal;
			} else {
				hraDeduction = totalHRAAmount;
			}
		}
		int childrens = Translator.integerValue(tdsDeclarationByEmployee.getJSONObject(0).opt(HRISApplicationConstants.TDSDeclarationSheet.CHILDRENS));
		if (totalSQDamount > sqd) {
			totalSQDamount = sqd;
		}

		if (totalSQRamount > sqr) {
			totalSQRamount = sqr;
		}

		if (childrens > maxChildrenAllowed) {
			childrens = maxChildrenAllowed;
		}

		childrenEducationAllowance = childrenEducationAllowance * childrens;

		totalAmount -= totalSQDamount;
		totalAmount -= totalSQRamount;
		totalAmount -= childrenEducationAllowance;
		totalAmount -= hraDeduction;
		totalAmount -= totalInterestAmount;
		return totalAmount;
	}

	private JSONArray getTdsDeclarationByEmployee(Object employeeId, Object financialYearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.TDSDeclarationSheet.RESOURCE);
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.HRA_TOTAL);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.A_80_D);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.A_80_DD);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.A_80_DDB);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.A_80_E);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.A_80_U);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_LIC);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_PPF);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_PF);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_NATIONAL_SAVING);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_PENSION_SCHEME);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_DEPOSIT_IN_5_YEAR_TERM);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_HOUSE_LONE);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_TUTION_FEES);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_FIXED_DEPOSIT);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.B_80_C_EQUITY_LINKED_SAVING);
		columnArray.put(HRISApplicationConstants.TDSDeclarationSheet.CHILDRENS);

		JSONObject innerObject = new JSONObject();
		JSONObject innerQuery = new JSONObject();
		JSONArray innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("interest_amount_amount");

		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "employee_loandetails_for_incometax");

		innerObject.put("relatedcolumn", "employee_incometax_id");
		innerObject.put("alias", "employee_loandetails_for_incometax");
		innerObject.put("parentcolumnalias", Updates.KEY);
		innerObject.put("parentcolumn", Updates.KEY);
		innerObject.put(Data.QUERY, innerQuery);

		query.put(Data.Query.CHILDS, new JSONArray().put(innerObject));

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.TDSRules.FINANCIAL_YEAR_ID + "=" + financialYearId + " and " + HRISApplicationConstants.TDSDeclarationSheet.EMPLOYEE_ID + "=" + employeeId);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeTaxDeclarationArray = employeeObject.getJSONArray(HRISApplicationConstants.TDSDeclarationSheet.RESOURCE);
		return employeeTaxDeclarationArray;
	}

	private JSONArray getTdsRules(Object financialYearId) throws JSONException {

		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.TDSRules.RESOURCE);
		columnArray.put(Updates.KEY);

		JSONObject innerObject = new JSONObject();
		JSONObject innerQuery = new JSONObject();
		JSONArray innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put(HRISApplicationConstants.TDSRulesPercentages.TDS_RULES_ID);
		innerColumns.put(HRISApplicationConstants.TDSRulesPercentages.FROM_AMOUNT);
		innerColumns.put(HRISApplicationConstants.TDSRulesPercentages.TO_AMOUNT);
		innerColumns.put(HRISApplicationConstants.TDSRulesPercentages.PERCENTAGE);

		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, HRISApplicationConstants.TDSRulesPercentages.RESOURCE);

		innerObject.put("relatedcolumn", HRISApplicationConstants.TDSRulesPercentages.TDS_RULES_ID);
		innerObject.put("alias", HRISApplicationConstants.TDSRulesPercentages.RESOURCE);
		innerObject.put("parentcolumnalias", Updates.KEY);
		innerObject.put("parentcolumn", Updates.KEY);
		innerObject.put(Data.QUERY, innerQuery);

		query.put(Data.Query.CHILDS, new JSONArray().put(innerObject));

		columnArray.put(HRISApplicationConstants.TDSRules.SQD);
		columnArray.put(HRISApplicationConstants.TDSRules.SQR);
		columnArray.put(HRISApplicationConstants.TDSRules.REBATE_BY_GOVERNMENT);
		columnArray.put(HRISApplicationConstants.TDSRules.HRA_ON_BASIC);
		columnArray.put(HRISApplicationConstants.TDSRules.CHILDREN_EDUCATION_ALLOWANCE);
		columnArray.put(HRISApplicationConstants.TDSRules.MAX_CHILDREN_ALLOWED);
		query.put(Data.Query.COLUMNS, columnArray);

		query.put(Data.Query.FILTERS, HRISApplicationConstants.TDSRules.FINANCIAL_YEAR_ID + "=" + financialYearId);

		JSONObject tdsRuleObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray tdsRuleArray = tdsRuleObject.getJSONArray(HRISApplicationConstants.TDSRules.RESOURCE);
		return tdsRuleArray;
	}

	private JSONArray getEmployeeAdditionalInformation(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_additional_information");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray salarySheetArray = employeeObject.getJSONArray("hris_employees_additional_information");
		return salarySheetArray;
	}

	private void updateSpecialRemarksHistory(Object key, String newRemarks) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_special_remarks_history");
		JSONObject row = new JSONObject();
		row.put("employeeadditionalinformationid", key);
		row.put("specialremarks", newRemarks);
		row.put("updatedby", CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL));
		row.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime()));
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			int applicationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_APPLICATION_ID));
			LogUtility.writeLog("onAfterUpdate >> Application Id : " + applicationId);

			if (applicationId == HrisKinds.HR_PORTAL_APPLICATION_ID) {

				String operationType = record.getOperationType();
				LogUtility.writeLog("Record Values : " + record.getValues());

				if (operationType.equals(Updates.Types.UPDATE)) {

					Object employeeName = record.getValue(HrisKinds.EmployeesAdditionalInformation.EMPLOYEE_NAME);
					Object employeeCode = record.getValue(HrisKinds.EmployeesAdditionalInformation.EMPLOYEE_CODE);
					Integer employeeBranchId = Translator.integerValue(record.getValue(HrisKinds.EmployeesAdditionalInformation.EMPLOYEE_BRANCH_ID));
					boolean salaryOnHold = Translator.booleanValue(record.getValue(HrisKinds.EmployeesAdditionalInformation.SALARY_ON_HOLD));
					Object relievingDate = record.getValue(HrisKinds.EmployeesAdditionalInformation.RELIEVING_DATE);
					Object relievingRemarks = record.getValue(HrisKinds.EmployeesAdditionalInformation.RELIEVING_REMARKS);
					boolean isNotRehirable = Translator.booleanValue(record.getValue(HrisKinds.EmployeesAdditionalInformation.IS_REHIRABLE));

					String currentUserEmailId = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL));
					JSONObject employeeDetail = HrisUserDefinedFunctions.getEmployeeDetail(currentUserEmailId);

					String currentEmployeeName = "";
					String currentEmployeeCode = "";

					if (employeeDetail != null) {
						currentEmployeeName = employeeDetail.optString(HrisKinds.Employess.NAME);
						currentEmployeeCode = employeeDetail.optString(HrisKinds.Employess.EMPLOYEE_CODE);
					}

					JSONObject mailConfigurationDetail = HrisUserDefinedFunctions.getMailConfigurationDetail();
					String cmdOfficialEmailId = "";
					if (mailConfigurationDetail != null) {
						cmdOfficialEmailId = mailConfigurationDetail.optString(HrisKinds.MailConfigurations.CMD_OFFICIAL_EMAIL_ID);
					}
					LogUtility.writeLog("CMD Official Email Id : " + cmdOfficialEmailId);

					Map<Integer, String[]> hrEmailIdsMap = getHREmailIds(employeeBranchId);
					String[] hrEmailIds = hrEmailIdsMap.get(employeeBranchId);

					StringBuilder sb = new StringBuilder();
					sb.append("Hello Sir").append("<br /><br />");
					sb.append("<b>").append(employeeName).append(",").append(employeeCode).append("</b> has deactivated by the <b>").append(currentEmployeeName).append(",").append(currentEmployeeCode).append("</b>.");
					sb.append(" The employee information is as follows").append("<br />");
					sb.append("<ul>");
					sb.append("<li>");
					sb.append("<b>Relieving Date</b>: ").append(relievingDate);
					sb.append("</li>");
					sb.append("<li>");
					sb.append("<b>Salary On Hold</b>: ").append((salaryOnHold ? "Yes" : "No"));
					sb.append("</li>");
					sb.append("<li>");
					sb.append("<b>Rehirable</b>: ").append((isNotRehirable ? "No" : "Yes"));
					sb.append("</li>");
					sb.append("<li>");
					sb.append("<b>Relieving Remarks</b>: ").append(relievingRemarks);
					sb.append("</li>");
					sb.append("</ul>");
					sb.append("<br />Regards<br /><br /><b>Applane Team</b>");

					LogUtility.writeLog("Employee Deactivate Message : " + sb);

					try {
						ApplaneMail mailSender = new ApplaneMail();
						mailSender.setMessage(employeeName + "," + employeeCode + " deactivated by " + currentEmployeeName + "," + currentEmployeeCode, sb.toString(), true);
						if (cmdOfficialEmailId != null && cmdOfficialEmailId.length() > 0) {
							mailSender.setTo(cmdOfficialEmailId);
						}
						if (hrEmailIds != null && hrEmailIds.length > 0) {
							mailSender.setCc(hrEmailIds);
						}
						mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
					} catch (ApplaneMailException e) {
						throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(EmployeeAditionalInformationBusinessLogic.class.getName(), e));
		}
	}

	private Map<Integer, String[]> getHREmailIds(Integer employeeBranchId) throws Exception, JSONException {

		int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));

		Map<Integer, String[]> branchHREmailIds = null;

		if (organizationId == HrisKinds.NAVIGANT_ORGANIZATION_ID) {
			branchHREmailIds = HrisUserDefinedFunctions.getNavigantBranchHREmailIds(employeeBranchId);
		} else {
			branchHREmailIds = HrisUserDefinedFunctions.getOtherBranchHREmailIds(employeeBranchId);
		}

		LogUtility.writeLog("getHREmailIds >> HR Email Ids : " + branchHREmailIds);

		return branchHREmailIds;
	}
}
