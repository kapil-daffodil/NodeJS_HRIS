package com.applane.resourceoriented.hris;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.CurrentRequest;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.google.apphosting.api.DeadlineExceededException;

public class EncashBalanceLeaves implements OperationJob {
	public void calculateAmountInvikeMethod(Object[] selectedKeys) {
		try {
			if (selectedKeys != null && selectedKeys.length > 0) {
				String keys = "";
				for (int counter = 0; counter < selectedKeys.length; counter++) {
					if (keys.length() > 0) {
						keys += ",";
					}
					keys += selectedKeys[counter];
				}
				if (keys.length() > 0) {
					JSONArray encashLeavesRecords = getEncashLeavesRecords(keys);
					for (int counter = 0; counter < encashLeavesRecords.length(); counter++) {
						int statusId = Translator.integerValue(encashLeavesRecords.getJSONObject(counter).opt(HRISApplicationConstants.EncashBalanceLeaves.STATUS_ID));
						String employeeName = Translator.stringValue(encashLeavesRecords.getJSONObject(counter).opt(HRISApplicationConstants.EncashBalanceLeaves.EMPLOYEE_ID + ".name"));
						if (statusId == HRISConstants.LEAVE_NEW) {
							Object key = encashLeavesRecords.getJSONObject(counter).opt(Updates.KEY);
							Object employeeId = encashLeavesRecords.getJSONObject(counter).opt(HRISApplicationConstants.EncashBalanceLeaves.EMPLOYEE_ID);
							long monthId = Translator.integerValue(encashLeavesRecords.getJSONObject(counter).opt(HRISApplicationConstants.EncashBalanceLeaves.MONTH_ID));
							long yearId = Translator.integerValue(encashLeavesRecords.getJSONObject(counter).opt(HRISApplicationConstants.EncashBalanceLeaves.YEAR_ID));
							double encashLeaves = Translator.doubleValue(encashLeavesRecords.getJSONObject(counter).opt(HRISApplicationConstants.EncashBalanceLeaves.ENCASH_LEAVES));
							double amount = calculateAmount(employeeId, monthId, yearId, encashLeaves);
							updateAmount(key, amount);
						} else {
							String statusName = Translator.stringValue(encashLeavesRecords.getJSONObject(counter).opt(HRISApplicationConstants.EncashBalanceLeaves.STATUS_ID + ".name"));
							WarningCollector.collect("[" + employeeName + "] Encash Leave " + statusName);
						}
					}
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("EncashBalanceLeaves >> calculateAmountInvikeMethod >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured, Please Contace To Admin.");
		}
	}

	private void updateAmount(Object key, double amount) throws JSONException {
		JSONObject updates = new JSONObject();
		JSONObject columns = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.EncashBalanceLeaves.RESOURCE);
		columns.put(Updates.KEY, key);
		columns.put(HRISApplicationConstants.EncashBalanceLeaves.AMOUNT, amount);
		updates.put(Data.Update.UPDATES, columns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private JSONArray getEncashLeavesRecords(String keys) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.EncashBalanceLeaves.RESOURCE);
		columnArray.put(Updates.KEY);
		columnArray.put(HRISApplicationConstants.EncashBalanceLeaves.EMPLOYEE_ID);
		columnArray.put(HRISApplicationConstants.EncashBalanceLeaves.EMPLOYEE_ID + ".name");
		columnArray.put(HRISApplicationConstants.EncashBalanceLeaves.ENCASH_LEAVES);
		columnArray.put(HRISApplicationConstants.EncashBalanceLeaves.MONTH_ID);
		columnArray.put(HRISApplicationConstants.EncashBalanceLeaves.YEAR_ID);
		columnArray.put(HRISApplicationConstants.EncashBalanceLeaves.STATUS_ID);
		columnArray.put(HRISApplicationConstants.EncashBalanceLeaves.STATUS_ID + ".name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " IN(" + keys + ")");
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.EncashBalanceLeaves.RESOURCE);
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			Object key = record.getKey();
			Object employeeId = record.getValue(HRISApplicationConstants.EncashBalanceLeaves.EMPLOYEE_ID);
			long monthId = Translator.integerValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.MONTH_ID));
			long yearId = Translator.integerValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.YEAR_ID));
			Object leaves = record.getValue(HRISApplicationConstants.EncashBalanceLeaves.LEAVES);
			double encashLeaves = Translator.doubleValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.ENCASH_LEAVES));
			if (record.has(HRISApplicationConstants.EncashBalanceLeaves.ENCASH_LEAVES)) {
				// double balanceLeaves = Translator.doubleValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.BALANCE_LEAVES));
				double amount = calculateAmount(employeeId, monthId, yearId, encashLeaves);
				updateAmount(key, amount);
			}
			if (record.has(HRISApplicationConstants.EncashBalanceLeaves.STATUS_ID)) {
				int statusId = Translator.integerValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.STATUS_ID));
				if (statusId == HRISConstants.LEAVE_APPROVED) {
					// Date requestDate = Translator.dateValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.REQUEST_DATE));
					JSONArray balanceLeaveArray = getEmployeeMonthlyAttendanceId(employeeId, monthId, yearId);
					if (balanceLeaveArray != null && balanceLeaveArray.length() > 0) {
						Object monthlyAttendanceId = balanceLeaveArray.getJSONObject(0).opt(Updates.KEY);
						double openingBalance = (Translator.doubleValue(balanceLeaveArray.getJSONObject(0).opt("earnedleaveopeningbalance")) - encashLeaves);
						updateMonthlyAttendance(monthlyAttendanceId, openingBalance, leaves);
					}
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("EncashBalanceLeaves >> onAfterUpdate >> Exception Trace >> " + trace);
		}
	}

	private void updateMonthlyAttendance(Object monthlyAttendanceId, double openingBalance, Object leaves) throws JSONException {
		JSONObject updates = new JSONObject();
		JSONObject columns = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		columns.put(Updates.KEY, monthlyAttendanceId);
		columns.put("earnedleaveopeningbalance", openingBalance);
		columns.put("leaves", leaves);
		// columns.put("ismanualupdate", true);
		columns.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
		updates.put(Data.Update.UPDATES, columns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private double calculateAmount(Object employeeId, long monthId, long yearId, double encashLeaves) throws JSONException, ParseException {
		String monthName = EmployeeSalaryGenerationServlet.getMonthName(monthId);
		String yearName = EmployeeSalaryGenerationServlet.getYearName(yearId);
		Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
		String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
		String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
		JSONArray employeeSalaryComponents = EmployeeSalaryGenerationServlet.getEmployeeSalaryComponents(monthFirstDateInString, monthLastDateInString, employeeId);
		JSONObject fields = new JSONObject();
		long maxDaysInMonth = DataTypeUtilities.differenceBetweenDates(DataTypeUtilities.checkDateFormat(monthFirstDateInString), DataTypeUtilities.checkDateFormat(monthLastDateInString));
		long maxDaysInMonthTotal = DataTypeUtilities.getNumberOfDaysInMonth(DataTypeUtilities.checkDateFormat(monthFirstDateInString));
		fields.put("maxDaysInMonth", maxDaysInMonth + 1);
		fields.put("maxDaysInMonthTotal", maxDaysInMonthTotal);
		fields.put("monthName", monthName);
		fields.put("yearName", yearName);
		fields.put("monthFirstDate", monthFirstDate);
		fields.put("monthLastDate", monthLastDate);
		fields.put("monthFirstDateInString", monthFirstDateInString);
		fields.put("monthLastDateInString", monthLastDateInString);
		return getPayableAmount(employeeSalaryComponents, fields, encashLeaves);
	}

	private double getPayableAmount(JSONArray employeeSalaryComponents, JSONObject fields, double encashLeaves) throws JSONException {
		double ctc = 0.0;
		for (int counter = 0; counter < employeeSalaryComponents.length(); counter++) {
			int componentType = Translator.integerValue(employeeSalaryComponents.getJSONObject(counter).opt("salarycomponentid.salarycomponenttypeid"));
			JSONObject employeeSalaryComponentRecord = employeeSalaryComponents.getJSONObject(counter);

			Integer paymentCycleId = (Integer) employeeSalaryComponentRecord.opt("salarycomponentid.paymentcycleid");
			double componentAmount = employeeSalaryComponentRecord.opt("amount") == null ? 0.0 : employeeSalaryComponentRecord.optDouble("amount");
			if (componentType == HRISApplicationConstants.SalaryComponentTypes.BASIC && componentAmount > 0.0) {

				if (paymentCycleId == HRISApplicationConstants.QUARTERLY) {
					componentAmount = (componentAmount * 4) / 12;
				} else if (paymentCycleId == HRISApplicationConstants.HALF_YEARLY) {
					componentAmount = (componentAmount * 2) / 12;
				} else if (paymentCycleId == HRISApplicationConstants.YEARLY) {
					componentAmount = (componentAmount * 1) / 12;
				} else if (paymentCycleId == HRISApplicationConstants.MONTHLY) {
					componentAmount = (componentAmount * 12) / 12;
				}

				Date applicableFromDate = Translator.dateValue(employeeSalaryComponentRecord.opt("applicablefrom"));
				Date applicableToDate = Translator.dateValue(employeeSalaryComponentRecord.opt("applicableto"));

				Date monthFirstDate = (Date) fields.get("monthFirstDate");
				Date monthLastDate = (Date) fields.get("monthLastDate");

				if (applicableFromDate.after(monthFirstDate)) {
					monthFirstDate = applicableFromDate;
				}
				if (applicableToDate.before(monthLastDate)) {
					monthLastDate = applicableToDate;
				}
				Calendar cl1 = Calendar.getInstance();
				Calendar cl2 = Calendar.getInstance();
				cl1.setTime(monthFirstDate);
				cl2.setTime(monthLastDate);
				long milliseconds1 = cl1.getTimeInMillis();
				long milliseconds2 = cl2.getTimeInMillis();
				long diff = milliseconds2 - milliseconds1;
				double incrementDateDifference = 0.0;
				if (cl1.before(cl2)) {
					incrementDateDifference = (diff / (24 * 60 * 60 * 1000) + 1);
				} else if (cl1.equals(cl2)) {
					incrementDateDifference = 1;
				}
				componentAmount = (float) (componentAmount * (incrementDateDifference) / (Long) fields.opt("maxDaysInMonth"));
				ctc += componentAmount;
			}
		}
		return ctc == 0.0 ? 0.0 : ((double) (ctc * encashLeaves) / (Long) fields.opt("maxDaysInMonth"));

	}

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			if (!record.has(HRISApplicationConstants.EncashBalanceLeaves.AMOUNT)) {

				String operationType = record.getOperationType();
				Object employeeId = record.getValue(HRISApplicationConstants.EncashBalanceLeaves.EMPLOYEE_ID);
				double balanceLeaves = Translator.doubleValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.BALANCE_LEAVES));
				double encashLeaves = Translator.doubleValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.ENCASH_LEAVES));
				Date requestDate = Translator.dateValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.REQUEST_DATE));
				if (operationType.equalsIgnoreCase(Updates.Types.INSERT)) {
					Object statusId = HRISConstants.LEAVE_NEW;
					String filter = HRISApplicationConstants.EncashBalanceLeaves.EMPLOYEE_ID + "=" + employeeId + " AND " + HRISApplicationConstants.EncashBalanceLeaves.STATUS_ID + " = " + statusId;
					String optionFilter = "";
					JSONArray encashLeaveRecord = getEncashLeaveRecord(filter, optionFilter);
					if (encashLeaveRecord != null && encashLeaveRecord.length() > 0) {
						throw new BusinessLogicException("You Have Already Requested.");
					}
				}
				if (balanceLeaves == 0) {
					throw new BusinessLogicException("You Have Not Suffecient Balance For Leave Encash.");
				}
				if (encashLeaves == 0 || encashLeaves > balanceLeaves) {
					throw new BusinessLogicException("Encash Leave Should be greater then 0 and less then Balance Leaves.");
				}
				Object monthid = CurrentRequest.getSessionVeriable("_MonthId");
				Object yearid = CurrentRequest.getSessionVeriable("_YearId");
				Object leaves = CurrentRequest.getSessionVeriable("_Leaves");
				CurrentRequest.clearSessionVariable("_MonthId");
				CurrentRequest.clearSessionVariable("_YearId");
				CurrentRequest.clearSessionVariable("_Leaves");
				if (yearid != null) {
					record.addUpdate(HRISApplicationConstants.EncashBalanceLeaves.YEAR_ID, yearid);
				} else {
					yearid = record.getValue(HRISApplicationConstants.EncashBalanceLeaves.YEAR_ID);
				}
				if (monthid != null) {
					record.addUpdate(HRISApplicationConstants.EncashBalanceLeaves.MONTH_ID, monthid);
				} else {
					monthid = record.getValue(HRISApplicationConstants.EncashBalanceLeaves.MONTH_ID);
				}
				if (leaves != null) {
					record.addUpdate(HRISApplicationConstants.EncashBalanceLeaves.LEAVES, leaves);
				} else {
					leaves = record.getValue(HRISApplicationConstants.EncashBalanceLeaves.LEAVES);
				}
				if (record.has(HRISApplicationConstants.EncashBalanceLeaves.STATUS_ID)) {
					int statusId = Translator.integerValue(record.getValue(HRISApplicationConstants.EncashBalanceLeaves.STATUS_ID));
					if (statusId == HRISConstants.LEAVE_APPROVED) {
						JSONArray balanceLeaveArray = getEmployeeMonthlyAttendanceId(employeeId, monthid, yearid);
						if (balanceLeaveArray != null && balanceLeaveArray.length() > 0) {

							double takenLeaves = Translator.doubleValue(balanceLeaveArray.getJSONObject(0).opt("leaves"));
							if (takenLeaves > Translator.doubleValue(leaves)) {
								throw new BusinessLogicException("You Have Taken Leaves After Request For Encash Leave, Please Cancle This and Request Again.");
							} else {
								String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
								Date todayDate = Translator.dateValue(TODAY_DATE);
								Calendar cal1 = Calendar.getInstance();
								cal1.setTime(requestDate);
								Calendar cal2 = Calendar.getInstance();
								cal2.setTime(todayDate);
								if (cal1.get(Calendar.MONTH) != cal2.get(Calendar.MONTH) || cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)) {
									throw new BusinessLogicException("You Have Requested For Encash Leave in Previous Month, Please Cancle This and Request Again.");
								}
							}
						}
					}
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("EncashBalanceLeaves >> onAfterUpdate >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured, Please Contace To Admin.");
		}
	}

	private JSONArray getEncashLeaveRecord(String filter, String optionFilter) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.EncashBalanceLeaves.RESOURCE);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, filter);
		query.put(Data.Query.OPTION_FILTERS, optionFilter);
		query.put(Data.Query.MAX_ROWS, 1);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.EncashBalanceLeaves.RESOURCE);
		return rows;
	}

	public Object getEmployeeBalanceLeaves(Object employeeId, Object requestDateObject) {
		try {
			if (employeeId != null && requestDateObject != null) {
				Date requestDate = Translator.dateValue(requestDateObject);
				Calendar cal = Calendar.getInstance();
				int counter = 0;
				JSONArray balanceLeaveArray = null;
				JSONArray checkMonthlyAttendanceArray = checkEmployeeMonthlyAttendanceRecords(employeeId);
				if (checkMonthlyAttendanceArray != null && checkMonthlyAttendanceArray.length() > 0) {
					for (;;) {
						cal.setTime(requestDate);
						cal.add(Calendar.MONTH, -counter);
						int monthId = cal.get(Calendar.MONTH) + 1;
						int yearId = (int) ((long) EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR)));
						balanceLeaveArray = getEmployeeMonthlyAttendanceId(employeeId, monthId, yearId);
						counter++;
						if (balanceLeaveArray != null && balanceLeaveArray.length() > 0) {
							CurrentRequest.setSessionVariable("_MonthId", monthId);
							CurrentRequest.setSessionVariable("_YearId", yearId);
							break;
						}
					}

					if (balanceLeaveArray != null && balanceLeaveArray.length() > 0) {
						double balanceLeaves = Translator.doubleValue(balanceLeaveArray.getJSONObject(0).opt("earnedleaveopeningbalance"));
						double leaves = Translator.doubleValue(balanceLeaveArray.getJSONObject(0).opt("leaves"));
						CurrentRequest.setSessionVariable("_Leaves", leaves);
						double exectBalance = (balanceLeaves - leaves);
						return exectBalance < 0.0 ? 0.0 : exectBalance;
					} else {
						throw new BusinessLogicException("No Records Found for Balance Leaves.");
					}
				} else {
					throw new BusinessLogicException("No Records Found for Balance Leaves.");
				}
			} else {
				throw new BusinessLogicException("Employee and Request Date is Compulsory.");
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("EncashBalanceLeaves >> getEmployeeBalanceLeaves >> Excaption Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured, While Getting Balance Leaves.");
		}
	}

	public JSONArray getEmployeeMonthlyAttendanceId(Object employeeId, Object monthId, Object yearId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("earnedleaveopeningbalance");
		columnArray.put("leaves");
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeemonthlyattendance");
		return rows;
	}

	public JSONArray checkEmployeeMonthlyAttendanceRecords(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("earnedleaveopeningbalance");
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId);
		query.put(Data.Query.MAX_ROWS, 1);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeemonthlyattendance");
		return rows;
	}
}
