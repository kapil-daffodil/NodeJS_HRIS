package com.applane.resourceoriented.hris;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class AssignPackagesBusinessLogic implements ApplaneTask, OperationJob {

	private static String EMPLOYEE_COUNTER = "0";

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {

	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			int packageCounter = 0;
			Object key = record.getKey();
			Object hris_salarypackagecomponents_1 = record.getValue("hris_salarypackagecomponents_1");
			Object hris_salarypackagecomponents_2 = record.getValue("hris_salarypackagecomponents_2");
			Object hris_salarypackagecomponents_3 = record.getValue("hris_salarypackagecomponents_3");
			Object hris_salarypackagecomponents_4 = record.getValue("hris_salarypackagecomponents_4");
			Object hris_salarypackagecomponents_5 = record.getValue("hris_salarypackagecomponents_5");
			Object hris_salarypackagecomponents_6 = record.getValue("hris_salarypackagecomponents_6");

			deleteRecords(key);

			if (hris_salarypackagecomponents_1 != null) {
				JSONArray hris_salarypackagecomponents = (JSONArray) hris_salarypackagecomponents_1;
				for (int coutner = 0; coutner < hris_salarypackagecomponents.length(); coutner++) {
					deleteNestedRecords(hris_salarypackagecomponents, coutner);
				}
			}
			if (hris_salarypackagecomponents_2 != null) {
				JSONArray hris_salarypackagecomponents = (JSONArray) hris_salarypackagecomponents_2;
				for (int coutner = 0; coutner < hris_salarypackagecomponents.length(); coutner++) {
					deleteNestedRecords(hris_salarypackagecomponents, coutner);
				}
			}
			if (hris_salarypackagecomponents_3 != null) {
				JSONArray hris_salarypackagecomponents = (JSONArray) hris_salarypackagecomponents_3;
				for (int coutner = 0; coutner < hris_salarypackagecomponents.length(); coutner++) {
					deleteNestedRecords(hris_salarypackagecomponents, coutner);
				}
			}
			if (hris_salarypackagecomponents_4 != null) {
				JSONArray hris_salarypackagecomponents = (JSONArray) hris_salarypackagecomponents_4;
				for (int coutner = 0; coutner < hris_salarypackagecomponents.length(); coutner++) {
					deleteNestedRecords(hris_salarypackagecomponents, coutner);
				}
			}
			if (hris_salarypackagecomponents_5 != null) {
				JSONArray hris_salarypackagecomponents = (JSONArray) hris_salarypackagecomponents_5;
				for (int coutner = 0; coutner < hris_salarypackagecomponents.length(); coutner++) {
					deleteNestedRecords(hris_salarypackagecomponents, coutner);
				}
			}
			if (hris_salarypackagecomponents_6 != null) {
				JSONArray hris_salarypackagecomponents = (JSONArray) hris_salarypackagecomponents_6;
				for (int coutner = 0; coutner < hris_salarypackagecomponents.length(); coutner++) {
					deleteNestedRecords(hris_salarypackagecomponents, coutner);
				}
			}

			assignPackage(record, packageCounter);

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AssignPackagesBusinessLogic >> onAfterUpdate >> Exception Trace >> " + trace);
		}
	}

	private void deleteNestedRecords(JSONArray hris_salarypackagecomponents, int coutner) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_salarypackagecomponents_assign");
		JSONObject row = new JSONObject();
		row.put("__key__", hris_salarypackagecomponents.getJSONObject(coutner).opt(Updates.KEY));
		row.put("__type__", "delete");
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private void deleteRecords(Object key) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_special_remarks_history");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		row.put("__type__", "delete");
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private void assignPackage(Record record, int packageCounter) throws Exception {
		Object packageId_1 = record.getValue("packageid");
		Object packageId_2 = record.getValue("packageid_2");
		Object packageId_3 = record.getValue("packageid_3");
		Object packageId_4 = record.getValue("packageid_4");
		Object packageId_5 = record.getValue("packageid_5");
		Object packageId_6 = record.getValue("packageid_6");

		Object duration_1 = record.getValue("duration_1");
		Object duration_2 = record.getValue("duration_2");
		Object duration_3 = record.getValue("duration_3");
		Object duration_4 = record.getValue("duration_4");
		Object duration_5 = record.getValue("duration_5");
		Object duration_6 = record.getValue("duration_6");

		Object hris_salarypackagecomponents_1 = record.getValue("hris_salarypackagecomponents_1");
		Object hris_salarypackagecomponents_2 = record.getValue("hris_salarypackagecomponents_2");
		Object hris_salarypackagecomponents_3 = record.getValue("hris_salarypackagecomponents_3");
		Object hris_salarypackagecomponents_4 = record.getValue("hris_salarypackagecomponents_4");
		Object hris_salarypackagecomponents_5 = record.getValue("hris_salarypackagecomponents_5");
		Object hris_salarypackagecomponents_6 = record.getValue("hris_salarypackagecomponents_6");

		Object dateObject = record.getValue("date");
		Object dueMonthId = record.getValue("duemonthid");
		Object employeeIdsObject = record.getValue("employeeids");
		JSONObject fields = new JSONObject();

		fields.put("packageId_1", packageId_1);
		fields.put("packageId_2", packageId_2);
		fields.put("packageId_3", packageId_3);
		fields.put("packageId_4", packageId_4);
		fields.put("packageId_5", packageId_5);
		fields.put("packageId_6", packageId_6);

		fields.put("duration_1", duration_1);
		fields.put("duration_2", duration_2);
		fields.put("duration_3", duration_3);
		fields.put("duration_4", duration_4);
		fields.put("duration_5", duration_5);
		fields.put("duration_6", duration_6);

		fields.put("hris_salarypackagecomponents_1", hris_salarypackagecomponents_1);
		fields.put("hris_salarypackagecomponents_2", hris_salarypackagecomponents_2);
		fields.put("hris_salarypackagecomponents_3", hris_salarypackagecomponents_3);
		fields.put("hris_salarypackagecomponents_4", hris_salarypackagecomponents_4);
		fields.put("hris_salarypackagecomponents_5", hris_salarypackagecomponents_5);
		fields.put("hris_salarypackagecomponents_6", hris_salarypackagecomponents_6);
		fields.put(EMPLOYEE_COUNTER, "" + packageCounter);

		fields.put("dateObject", dateObject);
		fields.put("dueMonthId", dueMonthId);
		fields.put("employeeIdsObject", employeeIdsObject);

		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.AssignPackagesBusinessLogic", QueueConstantsHRIS.POPULATE_ATTENDANCE, fields);

	}

	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		try {
			JSONObject fields = applaneTaskInfo.getTaskInfo();
			Object dateObject = fields.opt("dateObject");
			Object dueMonthIdObject = fields.opt("dueMonthId");
			int employeeCounter = Translator.integerValue(fields.opt(EMPLOYEE_COUNTER));
			Object packageId_1 = fields.opt("packageId_1");
			Object packageId_2 = fields.opt("packageId_2");
			Object packageId_3 = fields.opt("packageId_3");
			Object packageId_4 = fields.opt("packageId_4");
			Object packageId_5 = fields.opt("packageId_5");
			Object packageId_6 = fields.opt("packageId_6");

			Object hris_salarypackagecomponents_1 = fields.opt("hris_salarypackagecomponents_1");
			Object hris_salarypackagecomponents_2 = fields.opt("hris_salarypackagecomponents_2");
			Object hris_salarypackagecomponents_3 = fields.opt("hris_salarypackagecomponents_3");
			Object hris_salarypackagecomponents_4 = fields.opt("hris_salarypackagecomponents_4");
			Object hris_salarypackagecomponents_5 = fields.opt("hris_salarypackagecomponents_5");
			Object hris_salarypackagecomponents_6 = fields.opt("hris_salarypackagecomponents_6");

			Object duration_1 = fields.opt("duration_1");
			Object duration_2 = fields.opt("duration_2");
			Object duration_3 = fields.opt("duration_3");
			Object duration_4 = fields.opt("duration_4");
			Object duration_5 = fields.opt("duration_5");
			Object duration_6 = fields.opt("duration_6");

			Object employeeIds = fields.opt("employeeIdsObject");

			if (dateObject != null && employeeIds != null && hris_salarypackagecomponents_1 != null && packageId_1 != null) {
				JSONArray employeeIdArray = new JSONArray("" + employeeIds);
				Object employeeId = employeeIdArray.get(employeeCounter);
				JSONArray rows = EmployeeSalaryComponentsBusinessLogic.getJoiningDateOFEmployee(employeeId);
				Object joiningDate = null;
				if (rows != null && rows.length() > 0 && rows.getJSONObject(0).opt("joiningdate") != null) {
					joiningDate = rows.getJSONObject(0).opt("joiningdate");
				}
				if (duration_1 != null) {
					Date fromDate = Translator.dateValue(dateObject);
					Calendar cal = Calendar.getInstance();
					cal.setTime(fromDate);
					Object dueMonthId = dueMonthIdObject;
					if (dueMonthId == null) {
						dueMonthId = (cal.get(Calendar.MONTH) + 1);
					}
					cal.add(Calendar.MONTH, Translator.integerValue(duration_1));
					Date fromDateForNext = cal.getTime();
					String toDate = DataTypeUtilities.getBackDate(Translator.getDateInString(fromDateForNext));
					Object grossAmount = getPackageGrossAmount(packageId_1);
					if (grossAmount != null) {

						JSONObject parameters = new JSONObject();
						parameters.put("dueMonthId", dueMonthId);
						parameters.put("grossAmount", grossAmount);
						parameters.put("fromDateObject", dateObject);
						parameters.put("toDateObject", toDate);
						parameters.put("fromDateForNext", fromDateForNext);
						parameters.put("toDateObject", toDate);
						parameters.put("employeeId", employeeId);
						parameters.put("joiningDate", joiningDate);
						parameters.put("hris_salarypackagecomponents", hris_salarypackagecomponents_1);
						initiateTaskQueueAndAssignProcess(parameters);

					}

					if (packageId_2 != null && duration_2 != null && hris_salarypackagecomponents_2 != null) {
						cal = Calendar.getInstance();
						cal.setTime(fromDateForNext);
						String fromDateStr = Translator.getDateInString(fromDateForNext);
						dueMonthId = (cal.get(Calendar.MONTH) + 1);
						cal.add(Calendar.MONTH, Translator.integerValue(duration_2));
						fromDateForNext = cal.getTime();
						toDate = DataTypeUtilities.getBackDate(Translator.getDateInString(fromDateForNext));
						grossAmount = getPackageGrossAmount(packageId_2);
						if (grossAmount != null) {

							JSONObject parameters = new JSONObject();
							parameters.put("dueMonthId", dueMonthId);
							parameters.put("grossAmount", grossAmount);
							parameters.put("fromDateObject", fromDateStr);
							parameters.put("toDateObject", toDate);

							parameters.put("fromDateForNext", fromDateForNext);
							parameters.put("toDateObject", toDate);
							parameters.put("employeeId", employeeId);
							parameters.put("joiningDate", joiningDate);
							parameters.put("hris_salarypackagecomponents", hris_salarypackagecomponents_2);
							initiateTaskQueueAndAssignProcess(parameters);
						}

						if (packageId_3 != null && duration_3 != null && hris_salarypackagecomponents_3 != null) {
							cal = Calendar.getInstance();
							cal.setTime(fromDateForNext);
							fromDateStr = Translator.getDateInString(fromDateForNext);
							dueMonthId = (cal.get(Calendar.MONTH) + 1);
							cal.add(Calendar.MONTH, Translator.integerValue(duration_3));
							fromDateForNext = cal.getTime();
							toDate = DataTypeUtilities.getBackDate(Translator.getDateInString(fromDateForNext));
							grossAmount = getPackageGrossAmount(packageId_3);
							if (grossAmount != null) {

								JSONObject parameters = new JSONObject();
								parameters.put("dueMonthId", dueMonthId);
								parameters.put("grossAmount", grossAmount);
								parameters.put("fromDateObject", fromDateStr);
								parameters.put("toDateObject", toDate);
								parameters.put("fromDateForNext", fromDateForNext);
								parameters.put("toDateObject", toDate);
								parameters.put("employeeId", employeeId);
								parameters.put("joiningDate", joiningDate);
								parameters.put("hris_salarypackagecomponents", hris_salarypackagecomponents_3);
								initiateTaskQueueAndAssignProcess(parameters);
							}

							if (packageId_4 != null && duration_4 != null && hris_salarypackagecomponents_4 != null) {
								cal = Calendar.getInstance();
								cal.setTime(fromDateForNext);
								fromDateStr = Translator.getDateInString(fromDateForNext);
								dueMonthId = (cal.get(Calendar.MONTH) + 1);
								cal.add(Calendar.MONTH, Translator.integerValue(duration_4));
								fromDateForNext = cal.getTime();
								toDate = DataTypeUtilities.getBackDate(Translator.getDateInString(fromDateForNext));
								grossAmount = getPackageGrossAmount(packageId_4);
								if (grossAmount != null) {

									JSONObject parameters = new JSONObject();
									parameters.put("dueMonthId", dueMonthId);
									parameters.put("grossAmount", grossAmount);
									parameters.put("fromDateObject", fromDateStr);
									parameters.put("toDateObject", toDate);
									parameters.put("fromDateForNext", fromDateForNext);
									parameters.put("toDateObject", toDate);
									parameters.put("employeeId", employeeId);
									parameters.put("joiningDate", joiningDate);
									parameters.put("hris_salarypackagecomponents", hris_salarypackagecomponents_4);
									initiateTaskQueueAndAssignProcess(parameters);
								}

								if (packageId_5 != null && duration_5 != null && hris_salarypackagecomponents_5 != null) {
									cal = Calendar.getInstance();
									cal.setTime(fromDateForNext);
									fromDateStr = Translator.getDateInString(fromDateForNext);
									dueMonthId = (cal.get(Calendar.MONTH) + 1);
									cal.add(Calendar.MONTH, Translator.integerValue(duration_5));
									fromDateForNext = cal.getTime();
									toDate = DataTypeUtilities.getBackDate(Translator.getDateInString(fromDateForNext));
									grossAmount = getPackageGrossAmount(packageId_5);
									if (grossAmount != null) {

										JSONObject parameters = new JSONObject();
										parameters.put("dueMonthId", dueMonthId);
										parameters.put("grossAmount", grossAmount);
										parameters.put("fromDateObject", fromDateStr);
										parameters.put("toDateObject", toDate);
										parameters.put("fromDateForNext", fromDateForNext);
										parameters.put("toDateObject", toDate);
										parameters.put("employeeId", employeeId);
										parameters.put("joiningDate", joiningDate);
										parameters.put("hris_salarypackagecomponents", hris_salarypackagecomponents_5);
										initiateTaskQueueAndAssignProcess(parameters);
									}
									if (packageId_6 != null && duration_6 != null && hris_salarypackagecomponents_6 != null) {
										cal = Calendar.getInstance();
										cal.setTime(fromDateForNext);
										fromDateStr = Translator.getDateInString(fromDateForNext);
										dueMonthId = (cal.get(Calendar.MONTH) + 1);
										cal.add(Calendar.MONTH, Translator.integerValue(duration_6));
										fromDateForNext = cal.getTime();
										toDate = DataTypeUtilities.getBackDate(Translator.getDateInString(fromDateForNext));
										grossAmount = getPackageGrossAmount(packageId_6);
										if (grossAmount != null) {

											JSONObject parameters = new JSONObject();
											parameters.put("dueMonthId", dueMonthId);
											parameters.put("grossAmount", grossAmount);
											parameters.put("fromDateObject", fromDateStr);
											parameters.put("toDateObject", toDate);
											parameters.put("fromDateForNext", fromDateForNext);
											parameters.put("toDateObject", toDate);
											parameters.put("employeeId", employeeId);
											parameters.put("joiningDate", joiningDate);
											parameters.put("hris_salarypackagecomponents", hris_salarypackagecomponents_6);
											initiateTaskQueueAndAssignProcess(parameters);
										}
									}
								}
							}
						}
					}

				}
				if ((employeeCounter + 1) < employeeIdArray.length()) {
					fields.put(EMPLOYEE_COUNTER, (employeeCounter + 1));
					ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.AssignPackagesBusinessLogic", QueueConstantsHRIS.POPULATE_ATTENDANCE, fields);
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AssignPackagesBusinessLogic >> initiate >> Exception Trace >> " + trace);
		}

	}

	private void initiateTaskQueueAndAssignProcess(JSONObject fields) throws Exception {
		try {
			if (fields != null) {
				Object dueMonthId = fields.opt("dueMonthId");
				Object grossAmount = fields.opt("grossAmount");
				Object employeeId = fields.opt("employeeId");
				Object fromDateObject = fields.opt("fromDateObject");
				Object toDateObject = fields.opt("toDateObject");
				Object joiningDate = fields.opt("joiningDate");
				JSONArray packageComponents = (JSONArray) fields.opt("hris_salarypackagecomponents");
				// JSONArray packageComponents = getPackageComponents(packageId);
				double totalAmountFromPackage = 0.0;
				JSONArray overlapingList = new JSONArray();
				double totalPercentage = 0.0;

				for (int counter = 0; counter < packageComponents.length(); counter++) {
					Object salaryComponentIdObject = packageComponents.getJSONObject(counter).opt("componentid");
					Object salaryComponentId = null;
					Object componentName = "";
					if (salaryComponentIdObject instanceof JSONObject) {
						salaryComponentId = ((JSONObject) salaryComponentIdObject).opt(Updates.KEY);
						componentName = ((JSONObject) salaryComponentIdObject).opt("name");
					} else if (salaryComponentIdObject instanceof JSONArray) {
						salaryComponentId = ((JSONArray) salaryComponentIdObject).getJSONObject(0).opt(Updates.KEY);
						componentName = ((JSONArray) salaryComponentIdObject).getJSONObject(0).opt("name");
					} else {
						salaryComponentId = packageComponents.getJSONObject(counter).opt("componentid");
					}
					Object percentage = packageComponents.getJSONObject(counter).opt("percentage");
					Object paymentTypeIdObject = packageComponents.getJSONObject(counter).opt("componentid.paymenttypeid");
					int paymentTypeId = 0;
					if (paymentTypeIdObject instanceof JSONObject) {
						paymentTypeId = DataTypeUtilities.integerValue(((JSONObject) paymentTypeIdObject).opt(Updates.KEY));
					} else if (paymentTypeIdObject instanceof JSONArray) {
						paymentTypeId = DataTypeUtilities.integerValue(((JSONArray) paymentTypeIdObject).getJSONObject(0).opt(Updates.KEY));
					} else {
						paymentTypeId = DataTypeUtilities.integerValue(paymentTypeIdObject);
					}

					if (packageComponents.getJSONObject(counter).opt("amount") == null) {
						totalPercentage += Translator.doubleValue(percentage);
					}
					if (paymentTypeId != HRISApplicationConstants.DEDUCTABLE) {
						double amount = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
						totalAmountFromPackage += amount;
						JSONArray salaryComponentsForSkip = getSalaryComponentsToSkipInsert(null, employeeId, salaryComponentId, "" + fromDateObject, "" + toDateObject);
						if (salaryComponentsForSkip != null && salaryComponentsForSkip.length() > 0) {
							overlapingList.put(componentName);
						}
					}
				}
				boolean onlyForDeductable = false;
				if (overlapingList.length() == 0 && totalPercentage <= 100.0) {
					double balanceAmount = DataTypeUtilities.doubleValue(grossAmount) - totalAmountFromPackage;
					if (balanceAmount < 0.0) {
						balanceAmount = 0.0;
					}
					for (int counter = 0; counter < packageComponents.length(); counter++) {
						Object salaryComponentId = packageComponents.getJSONObject(counter).opt("componentid");
						Object componentName = packageComponents.getJSONObject(counter).opt("componentid.name");
						Object amount = packageComponents.getJSONObject(counter).opt("amount");
						Object percentage = packageComponents.getJSONObject(counter).opt("percentage");
						double rate = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("rate"));
						double target = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("target"));
						Object paymentTypeIdObject = packageComponents.getJSONObject(counter).opt("componentid.paymenttypeid");
						int paymentTypeId = 0;
						if (paymentTypeIdObject instanceof JSONObject) {
							paymentTypeId = DataTypeUtilities.integerValue(((JSONObject) paymentTypeIdObject).opt(Updates.KEY));
						} else if (paymentTypeIdObject instanceof JSONArray) {
							paymentTypeId = DataTypeUtilities.integerValue(((JSONArray) paymentTypeIdObject).getJSONObject(0).opt(Updates.KEY));
						} else {
							paymentTypeId = DataTypeUtilities.integerValue(paymentTypeIdObject);
						}

						Object componentCriteriaObject = packageComponents.getJSONObject(counter).opt("componentid.componentcriteriaid");
						int componentCriteria = 0;
						if (componentCriteriaObject instanceof JSONObject) {
							componentCriteria = DataTypeUtilities.integerValue(((JSONObject) componentCriteriaObject).opt(Updates.KEY));
						} else if (componentCriteriaObject instanceof JSONArray) {
							componentCriteria = DataTypeUtilities.integerValue(((JSONArray) componentCriteriaObject).getJSONObject(0).opt(Updates.KEY));
						} else {
							componentCriteria = DataTypeUtilities.integerValue(componentCriteriaObject);
						}

						Object performanceTypeIdObject = packageComponents.getJSONObject(counter).opt("componentid.performancetypeid");
						int performanceTypeId = 0;
						if (performanceTypeIdObject instanceof JSONObject) {
							performanceTypeId = DataTypeUtilities.integerValue(((JSONObject) performanceTypeIdObject).opt(Updates.KEY));
						} else if (performanceTypeIdObject instanceof JSONArray) {
							performanceTypeId = DataTypeUtilities.integerValue(((JSONArray) performanceTypeIdObject).getJSONObject(0).opt(Updates.KEY));
						} else {
							performanceTypeId = DataTypeUtilities.integerValue(performanceTypeIdObject);
						}
						if (componentCriteria == HRISApplicationConstants.FIXED_BASED) {
							if (joiningDate != null) {
								Date joiningDate1 = Translator.dateValue(joiningDate);
								if (joiningDate1.equals(Translator.dateValue(fromDateObject))) {
									String firstDate = EmployeeSalaryGenerationServlet.getDateInString(DataTypeUtilities.getFirstDayOfMonth(joiningDate1));
									fromDateObject = firstDate;
								}
							}
						}
						if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE) {
							JSONArray salaryComponentsForSkip = getSalaryComponentsToSkipInsert(null, employeeId, salaryComponentId, "" + fromDateObject, "" + toDateObject);
							if (salaryComponentsForSkip == null || salaryComponentsForSkip.length() == 0) {
								if (amount == null && percentage != null) {
									amount = (float) (balanceAmount * Translator.doubleValue(percentage)) / 100;
								}
								EmployeeNameBusinessLogic.insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, amount, dueMonthId, rate, target);
							} else {
								overlapingList.put(componentName);
								onlyForDeductable = true;
							}
						} else {
							if (componentCriteria == HRISApplicationConstants.PERFORMANCE_BASED && performanceTypeId == HRISApplicationConstants.TARGET_ACHIEVED) {
								amount = rate * target;
							} else if (amount == null && percentage != null) {
								amount = (float) (balanceAmount * Translator.doubleValue(percentage)) / 100;
							}
							EmployeeNameBusinessLogic.insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, amount, dueMonthId, rate, target);
						}
					}
				}

				if (overlapingList.length() > 0 || totalPercentage > 100.0) {
					JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(employeeId);

					String title = "";
					String mailValue = "";
					if (employeeArray != null && employeeArray.length() > 0) {
						mailValue += "Employee Name:- " + employeeArray.getJSONObject(0).opt("name");
						mailValue += "<BR />Employee Code:- " + employeeArray.getJSONObject(0).opt("employeecode");
					}
					if (onlyForDeductable) {
						mailValue += "<BR /><BR />List only for Deductable Components <BR /><BR /> ";
					}
					if (overlapingList.length() > 0) {
						title += "Component Over Laping";
						mailValue += "<BR /><BR />Component Lists: <BR /><BR /> " + overlapingList.toString();
					}
					if (totalPercentage > 100.0) {
						if (title.length() > 0) {
							title += " and Total Percentage is above 100% (" + totalPercentage + ")";
							mailValue += "<BR /><BR /> Total Percentage Should Less or Equal To 100% but you have assigned (" + totalPercentage + ")";
						} else {
							title += "Total Percentage is above 100% (" + totalPercentage + ")";
							mailValue += "Total Percentage Should Less or Equal To 100% but you have assigned (" + totalPercentage + ")";
						}
					}

					PunchingUtility.sendMailsWithoutCC(new String[] { "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL) }, mailValue, title);
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeNameBusinessLogic.class.getName(), e);
			LogUtility.writeLog("AssigningPackageBusinessLogic >> initiateTaskQueueAndAssignProcess >> Exception Trace >> " + trace);

		}
	}

	private Object getPackageGrossAmount(Object packageid) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columns = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_salarypackages");
		columns.put("__key__");
		columns.put("fixamount");
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + packageid);
		query.put(Data.Query.COLUMNS, columns);
		JSONObject data = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = data.getJSONArray("hris_salarypackages");
		if (rows != null && rows.length() > 0) {
			return rows.getJSONObject(0).opt("fixamount");
		}
		return null;
	}

	public static JSONArray getSalaryComponentsToSkipInsert(Object updateKey, Object employeeId, Object salaryComponentId, String fromDate, String toDate) throws Exception {
		String extreFilter = "";
		if (updateKey != null) {
			extreFilter = " and " + Updates.KEY + " != " + updateKey;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("salarycomponentid.name");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarycomponentid = " + salaryComponentId + " and employeeid = " + employeeId + " and applicablefrom <= '" + fromDate + "' and applicableto >= '" + fromDate + "'" + extreFilter);
		query.put(Data.Query.OPTION_FILTERS, "salarycomponentid = " + salaryComponentId + " and employeeid = " + employeeId + " and applicablefrom <= '" + toDate + "' and applicableto >= '" + toDate + "'" + extreFilter);
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
			if (updateKey != null) {
				keys += ("," + updateKey);
			}
			filters += (" and " + Updates.KEY + " not in (" + keys + ")");
		} else if (updateKey != null) {
			keys += ("" + updateKey);
			filters += (" and " + Updates.KEY + " not in (" + keys + ")");

		}
		query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarycomponentid = " + salaryComponentId + " and employeeid = " + employeeId + " and applicablefrom >= '" + fromDate + "' and applicableto <= '" + toDate + "'" + filters);
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows1 = object.getJSONArray("employeesalarycomponents");

		for (int counter = 0; counter < rows1.length(); counter++) {
			JSONObject object1 = rows1.getJSONObject(counter);
			rows.put(object1);
		}
		// LogUtility.writeLog("rows >> " + rows);
		return rows;
	}
}
