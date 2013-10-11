package com.applane.resourceoriented.hris;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.InterAppCommunication;
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
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.cachehandler.UserCacheHandler;
import com.applane.user.exception.UserNotFoundException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeKPIServlet implements ApplaneTask {

	private static final long serialVersionUID = -8634765326738382207L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		JSONObject taskInfo = applaneTaskInfo.getTaskInfo();
		Object monthId = taskInfo.opt("monthid");
		Object yearId = taskInfo.opt("yearid");
		boolean isUpdate = Translator.booleanValue(taskInfo.opt("isUpdate"));
		Object key = taskInfo.opt(Updates.KEY);
		try {

			String monthName = getMonthName(monthId);
			String yearName = getYearName(yearId);
			Object lastKey = null;
			if (monthName != null && monthName.length() > 0 && yearName != null && yearName.length() > 0) {
				Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);

				String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);

				Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
				String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
				JSONArray employeeArray = getEmployeeRecords(monthFirstDateInString, monthLastDateInString, key);
				lastKey = resolveEmployeeKPIs(monthId, yearId, employeeArray, true, isUpdate);
				if (lastKey != null) {
					initiateTaskQueue(monthId, yearId, lastKey, isUpdate);
				} else {
					sendMailOnCompletion();
				}
			}
		} catch (Exception e) {
			sendMail(e);
		}
	}

	public void populateEmployeePerformanceInvode(Object[] selectedKeys) {
		try {
			for (int counter = 0; counter < selectedKeys.length; counter++) {
				Object performanceKey = selectedKeys[counter];
				JSONArray perfromanceArray = getPerfromanceArray(performanceKey);
				if (perfromanceArray != null && perfromanceArray.length() > 0) {
					Object monthId = perfromanceArray.getJSONObject(0).opt("performancemonthid");
					Object yearId = perfromanceArray.getJSONObject(0).opt("performanceyearid");
					Object employeeId = perfromanceArray.getJSONObject(0).opt("employeeid");
					JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(employeeId);
					boolean isUpdate = true;
					resolveEmployeeKPIs(monthId, yearId, employeeArray, false, isUpdate);
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("EmployeeKPIServlet >> populateEmployeePerformanceInvode >> trace >> " + trace);
			throw new BusinessLogicException("Some Error Occured Please Contace to Admin.");
		}
	}

	private static JSONArray getPerfromanceArray(Object performanceKey) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeperformance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("performanceyearid");
		columnArray.put("performancemonthid");
		columnArray.put("employeeid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " in (" + performanceKey + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("employeeperformance");
		return employeeArray;
	}

	private static void sendMailOnCompletion() {
		String title = "\"Employee Performance KPI Populate\" Queue Completed";
		String mailValue = "Dear: " + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		mailValue += "<BR /><BR />";
		mailValue += "\"Employee Performance KPI Populate\" Queue Completed";
		mailValue += "<BR /><BR />";
		mailValue += "Regards";
		mailValue += "<BR />";
		mailValue += "Applane";
		try {
			PunchingUtility.sendMailsWithoutCC(new String[] { "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL) }, mailValue, title);
		} catch (Exception e) {
			LogUtility.writeLog("Employee Performance KPI Population completion mail failed");
		}
	}

	private void sendMail(Exception e) {
		String traces = ExceptionUtils.getExceptionTraceMessage("EmployeeKPIServlet", e);
		LogUtility.writeError(traces);
		ApplaneMail mail = new ApplaneMail();
		StringBuilder builder = new StringBuilder();
		builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
		builder.append("<br><br><b>Exception traces are given below :</b><br><br>").append(traces);
		mail.setMessage("Employee monthly performance (Insert KPI) Queue Failed", builder.toString(), true);
		try {
			mail.setTo("" + CurrentState.getCurrentState().get(CurrentSession.CURRENT_USER_EMAIL));
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception Come while send mail in employee kpi servlet.");
		} catch (JSONException e2) {
			LogUtility.writeLog("Exception Come while send mail in employee kpi servlet.");
		}

	}

	private void initiateTaskQueue(Object monthId, Object yearId, Object lastKey, boolean isUpdate) throws JSONException {
		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("monthid", "" + monthId);
		taskQueueParameters.put("yearid", "" + yearId);
		taskQueueParameters.put("isUpdate", isUpdate);
		taskQueueParameters.put(Updates.KEY, "" + lastKey);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeKPIServlet", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);
	}

	private Object resolveEmployeeKPIs(Object monthId, Object yearId, JSONArray employeeArray, boolean isFromTaskQueue, boolean isUpdate) throws Exception {
		if (monthId != null && yearId != null) {
			String monthName = getMonthName(monthId);
			String yearName = getYearName(yearId);
			Object lastKey = null;
			if (monthName != null && monthName.length() > 0 && yearName != null && yearName.length() > 0) {
				Calendar cal = Calendar.getInstance();
				Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
				String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
				cal.setTime(monthFirstDate);

				Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
				String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
				int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
				long totalDaysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
				for (int counter = 0; counter < employeeArrayCount; counter++) {

					JSONObject employee = employeeArray.getJSONObject(counter);
					Object employeeId = employee.opt(Updates.KEY);
					String officialEmailId = employee.optString(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
					lastKey = employeeId;
					Object employeeUserId = null;
					try {
						employeeUserId = UserCacheHandler.getUser(officialEmailId).get(Data.Query.KEY);
					} catch (UserNotFoundException e) {
						/* No user exist with this email id. Take attendance of next employee */
						// continue;
					}

					JSONArray employeeKPIArray = getEmployeeKPIs(employeeId);
					int kpiArrayCount = (employeeKPIArray == null || employeeKPIArray.length() == 0) ? 0 : employeeKPIArray.length();
					for (int counter1 = 0; counter1 < kpiArrayCount; counter1++) {
						Object businessFunctionId = employeeKPIArray.getJSONObject(counter1).opt("businessfunctionid");
						Object rateObject = null;
						Object targetObject = null;
						Object kpiId = employeeKPIArray.getJSONObject(counter1).opt("keyperformanceindicatorid");
						Object targetFromEmployeeKPI = employeeKPIArray.getJSONObject(counter1).opt("target");
						int kpiTypeId = Translator.integerValue(employeeKPIArray.getJSONObject(counter1).opt("keyperformanceindicatorid.kpitypeid"));
						if (kpiId != null) {
							JSONArray employeeSalaryComponentArray = getEmployeeSalaryComponentArray(employeeId, kpiId, monthFirstDateInString, monthLastDateInString);
							if (employeeSalaryComponentArray != null && employeeSalaryComponentArray.length() > 0) {
								double totalRate = 0.0;
								double totalTarget = 0.0;
								for (int componentCounter = 0; componentCounter < employeeSalaryComponentArray.length(); componentCounter++) {
									double rate = Translator.doubleValue(employeeSalaryComponentArray.getJSONObject(componentCounter).opt("rate"));
									double target = Translator.doubleValue(employeeSalaryComponentArray.getJSONObject(componentCounter).opt("target"));
									Date applicableFrom = Translator.dateValue(employeeSalaryComponentArray.getJSONObject(componentCounter).opt("applicablefrom"));
									Date applicableTo = Translator.dateValue(employeeSalaryComponentArray.getJSONObject(componentCounter).opt("applicableto"));
									if (applicableFrom.before(monthFirstDate)) {
										applicableFrom = monthFirstDate;
									}
									if (applicableTo.after(monthLastDate)) {
										applicableTo = monthLastDate;
									}
									if (employeeSalaryComponentArray.length() > 1) {
										long diff = DataTypeUtilities.differenceBetweenDates(applicableFrom, applicableTo) + 1;
										totalRate += ((float) (rate * diff) / totalDaysInMonth);
										totalTarget += ((float) (target * diff) / totalDaysInMonth);
									} else {
										totalRate += rate;
										totalTarget += target;
									}
								}
								if (totalRate > 0) {
									rateObject = totalRate;
								}
								if (totalTarget > 0) {
									targetObject = totalTarget;
								}
							}
						}
						// if (isFromTaskQueue) {
						// insertEmployeePerformance(monthId, yearId, employeeId, employeeUserId, businessFunctionId, kpiId, rateObject, targetObject);
						// } else {
						if (kpiTypeId == HRISApplicationConstants.kpiTypes.NORMAL) {
							targetObject = targetFromEmployeeKPI;
						}
						insertEmployeePerformanceRateTarget(monthId, yearId, employeeId, employeeUserId, businessFunctionId, kpiId, rateObject, targetObject, yearName, (cal.get(Calendar.MONTH) + 1), officialEmailId, kpiTypeId, isFromTaskQueue, isUpdate);
						// }
					}
				}
				if (employeeArrayCount < 20) {
					lastKey = null;
				}
			}
			return lastKey;
		} else {
			return null;
		}
	}

	private void insertEmployeePerformanceRateTarget(Object monthId, Object yearId, Object employeeId, Object employeeUserId, Object businessFunctionId, Object kpiId, Object rateObject, Object targetObject, String yearName, int month, String officialEmailId, int kpiTypeId, boolean isFromTaskQueue, boolean isUpdate) throws Exception {
		JSONArray employeePerformanceAlreadyExist = isAlreadyExist(employeeId, monthId, yearId, kpiId);
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeperformance");
		JSONObject row = new JSONObject();
		Object achieve = null;
		Object amount = null;
		Object key = null;
		Object achieveTemp = null;
		boolean isManualUpdate = false;
		if (employeePerformanceAlreadyExist != null && employeePerformanceAlreadyExist.length() > 0) {
			key = employeePerformanceAlreadyExist.getJSONObject(0).opt(Updates.KEY);
			achieveTemp = employeePerformanceAlreadyExist.getJSONObject(0).opt("achieved");
			isManualUpdate = Translator.booleanValue(employeePerformanceAlreadyExist.getJSONObject(0).opt("is_manual"));
		}
		boolean isEmployeeBelongsToApplane = false;
		if (isUpdate && isFromTaskQueue && Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID)) == 5) {
			isEmployeeBelongsToApplane = getIsEmployeeBelongsToApplane(employeeId);
		}
		if (!isManualUpdate && ((isUpdate && !isEmployeeBelongsToApplane) || achieveTemp == null)) {
			String performanceMonth = "month-" + yearName + "-" + month;
			if (kpiTypeId == HRISApplicationConstants.kpiTypes.PERFORMANCE) {
				try {
					achieve = InterAppCommunication.invokeMethod("com.applane.app.jobs.KPIQuery", "getKpiValue", new Object[] { Long.valueOf(kpiId.toString()), performanceMonth, officialEmailId });
					if (rateObject != null && achieve != null) {
						amount = (Translator.doubleValue(achieve) * Translator.doubleValue(rateObject));
					}
				} catch (Exception e) {
					achieve = null;
					amount = null;
					String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
					LogUtility.writeLog("EmployeeKPIServlet >> Exception Trace >> " + trace);
				}
			}

			if (key != null) {
				row.put(Updates.KEY, key);
			} else {
				row.put("employeeid", employeeId);
				row.put("employeeuserid", employeeUserId == null ? JSONObject.NULL : employeeUserId);
				row.put("keyperformanceindicatorid", kpiId);
				row.put("performanceyearid", yearId);
				row.put("performancemonthid", monthId);
				row.put("businessfunctionid", businessFunctionId);
			}
			if (rateObject != null) {
				row.put("rate", rateObject);
			}
			if (targetObject != null) {
				if (kpiTypeId == HRISApplicationConstants.kpiTypes.NORMAL && (employeePerformanceAlreadyExist == null || (employeePerformanceAlreadyExist.length() > 0 && employeePerformanceAlreadyExist.getJSONObject(0).opt("target") == null))) {
					row.put("target", targetObject);
				} else if (kpiTypeId != HRISApplicationConstants.kpiTypes.NORMAL) {
					row.put("target", targetObject);
				}
			}
			if (achieve != null) {
				row.put("achieved", achieve);
			}
			if (amount != null && targetObject != null) {
				row.put("amount", amount);
			}
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		}
	}

	private boolean getIsEmployeeBelongsToApplane(Object employeeId) throws Exception {
		int APPLANE_ID = 4;
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees__profitcenterid");
		JSONArray array = new JSONArray();
		array.put(Data.Query.KEY);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "hris_employees = " + employeeId + " AND profitcenterid = " + APPLANE_ID);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_employees__profitcenterid");
		return (rows == null || rows.length() == 0) ? false : true;
	}

	private JSONArray getEmployeeSalaryComponentArray(Object employeeId, Object kpiId, String monthFirstDateInString, String monthLastDateInString) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("rate");
		columnArray.put("target");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom <= '" + monthFirstDateInString + "' and applicableto >= '" + monthFirstDateInString + "' and salarycomponentid." + SalaryComponentKinds.KPI_ID + " = " + kpiId + " and salarycomponentid.performancetypeid = " + HRISApplicationConstants.TARGET_ACHIEVED);
		query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId + " and applicablefrom <= '" + monthLastDateInString + "' and applicableto >= '" + monthLastDateInString + "' and salarycomponentid." + SalaryComponentKinds.KPI_ID + " = " + kpiId + " and salarycomponentid.performancetypeid = " + HRISApplicationConstants.TARGET_ACHIEVED);
		JSONObject employeeObject = ResourceEngine.query(query);
		JSONArray employeeSalaryComponentArray = employeeObject.getJSONArray("employeesalarycomponents");
		return employeeSalaryComponentArray;
	}

	private JSONArray getEmployeeRecords(String monthFirstDateInString, String monthLastDateInString, Object key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		String extraFilter = "";
		if (key != null) {
			extraFilter = " and " + Updates.KEY + " > " + key;
		}
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, Updates.KEY);
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		orders.put(orderObject);
		query.put(Data.Query.ORDERS, orders);

		query.put(Data.Query.COLUMNS, columnArray);
		// query.put(Data.Query.FILTERS, "__key__= 202");
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + extraFilter);
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and relievingdate >= '" + monthFirstDateInString + "'" + extraFilter);// and relievingdate <= '" + monthLastDateInString + "'");
		query.put(Data.Query.MAX_ROWS, 20);
		JSONObject employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public static String getYearName(Object yearId) throws JSONException {
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
	}

	public static String getMonthName(Object monthId) throws JSONException {
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
	}

	// private void insertEmployeePerformance(Object monthId, Object yearId, Object employeeId, Object employeeUserId, Object businessFunctionId, Object kpiId, Object rateObject, Object targetObject) throws JSONException {
	// JSONArray employeePerformanceAlreadyExist = isAlreadyExist(employeeId, monthId, yearId, kpiId);
	// if (employeePerformanceAlreadyExist == null || employeePerformanceAlreadyExist.length() == 0) {
	// JSONObject updates = new JSONObject();
	// updates.put(Data.Query.RESOURCE, "employeeperformance");
	// JSONObject row = new JSONObject();
	// row.put("employeeid", employeeId);
	// row.put("employeeuserid", employeeUserId == null ? JSONObject.NULL : employeeUserId);
	// row.put("keyperformanceindicatorid", kpiId);
	// row.put("performanceyearid", yearId);
	// row.put("performancemonthid", monthId);
	// row.put("businessfunctionid", businessFunctionId);
	// if (rateObject != null) {
	// row.put("rate", rateObject);
	// }
	// if (targetObject != null) {
	// row.put("target", targetObject);
	// }
	// updates.put(Data.Update.UPDATES, row);
	// ResourceEngine.update(updates);
	// }
	// }

	private JSONArray isAlreadyExist(Object employeeId, Object monthId, Object yearId, Object kpiId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeperformance");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		array.put("target");
		array.put("achieved");
		array.put("is_manual");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and keyperformanceindicatorid = " + kpiId + " and performanceyearid = " + yearId + " and performancemonthid = " + monthId);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeeperformance");
		if (rows != null && rows.length() > 0) {
			return rows;
		} else {
			return null;
		}
	}

	private JSONArray getEmployeeKPIs(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeekpi");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("businessfunctionid");
		columnArray.put("target");
		columnArray.put("keyperformanceindicatorid");
		columnArray.put("keyperformanceindicatorid.kpitypeid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray employeeKPIArray = object.getJSONArray("employeekpi");
		return employeeKPIArray;
	}

}
