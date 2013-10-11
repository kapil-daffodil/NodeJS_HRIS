package com.applane.resourceoriented.hris;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.google.apphosting.api.DeadlineExceededException;

public class PopulateSalaryComponentsBusinessLogic implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		String opertaionType = record.getOperationType();
		if (opertaionType.equalsIgnoreCase("insert")) {
			Object fromDate = record.getValue("applicablefrom");
			Object toDate = record.getValue("applicableto");
			Object employeeId = record.getValue("employeeid");
			Object dueMonthId = record.getValue("duemonthid");
			try {
				Date applicableFrom = DataTypeUtilities.checkDateFormat(fromDate);
				Date applicatbleTo = DataTypeUtilities.checkDateFormat(toDate);
				if (applicatbleTo.before(applicableFrom) || applicatbleTo.equals(applicableFrom)) {
					throw new BusinessLogicException("Application To date must be greater than Applicable From date.");
				}
				if (fromDate != null && toDate != null && dueMonthId != null) {
					if (Translator.dateValue(fromDate).before(Translator.dateValue(toDate))) {
						JSONArray salaryComponentsArray = getSalaryComponentsRecords();
						int arrayCount = (salaryComponentsArray == null || salaryComponentsArray.length() == 0) ? 0 : salaryComponentsArray.length();
						if (arrayCount > 0) {
							for (int counter = 0; counter < arrayCount; counter++) {
								Object salaryComponentId = salaryComponentsArray.getJSONObject(counter).opt("__key__");
								JSONArray componentAlreadyExistArray = EmployeeNameBusinessLogic.getSalaryComponentsToSkipInsert(null, employeeId, salaryComponentId, "" + fromDate, "" + toDate);
								if (componentAlreadyExistArray == null || componentAlreadyExistArray.length() == 0) {
									Object amount = null;
									Object rateObject = null;
									Object targetObject = null;
									int criteriaId = Translator.integerValue(salaryComponentsArray.getJSONObject(counter).opt("componentcriteriaid"));
									int performanceTypeId = Translator.integerValue(salaryComponentsArray.getJSONObject(counter).opt("performancetypeid"));
									if (criteriaId == HRISApplicationConstants.PERFORMANCE_BASED && performanceTypeId == HRISApplicationConstants.TARGET_ACHIEVED) {
										double rate = Translator.doubleValue(salaryComponentsArray.getJSONObject(counter).opt("rate"));
										double target = Translator.doubleValue(salaryComponentsArray.getJSONObject(counter).opt("target"));
										rateObject = rate;
										targetObject = target;
										amount = rate * target;
									}
									insertEmployeeSalaryComponents(salaryComponentId, fromDate, toDate, employeeId, amount, rateObject, targetObject, dueMonthId);
								}
							}
						}
					} else {
						throw new BusinessLogicException("From Date should be less then To Date");
					}
				} else {
					throw new BusinessLogicException("From Date, To Date and Due Month Can not be null");
				}
			} catch (DeadlineExceededException e) {
				LogUtility.writeLog("First exception block...");
				throw e;
			} catch (BusinessLogicException e) {
				LogUtility.writeLog("Second exception block");
				throw e;
			} catch (Exception e) {
				LogUtility.writeLog("Third Exception block...");
				e.printStackTrace();
				throw new BusinessLogicException("Some error ocuured while populate employee salary components.");
			}

		}
	}

	private void insertEmployeeSalaryComponents(Object salaryComponentId, Object fromDate, Object toDate, Object employeeId, Object amount, Object rateObject, Object targetObject, Object dueMonthId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("salarycomponentid", salaryComponentId);
		if (amount != null) {
			row.put("amount", amount);
		}
		if (rateObject != null) {
			row.put("rate", rateObject);
		}
		if (targetObject != null) {
			row.put("target", targetObject);
		}
		row.put("applicablefrom", fromDate);
		row.put("applicableto", toDate);
		row.put("duemonthid", dueMonthId);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);

	}

	private JSONArray getSalaryComponentsRecords() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("rate");
		array.put("target");
		array.put("performancetypeid");
		array.put("componentcriteriaid");
		query.put(Data.Query.COLUMNS, array);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("salarycomponents");
		return rows;
	}

	boolean isExists(Object fromDate, Object toDate, Object employeeId, Object salaryComponentId) throws JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom = '" + fromDate + "' and applicableto = '" + toDate + "' and salarycomponentid = " + salaryComponentId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeesalarycomponents");
		if (rows != null && rows.length() > 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onAfterUpdate(Record record) {
		// TODO Auto-generated method stub

	}

}
