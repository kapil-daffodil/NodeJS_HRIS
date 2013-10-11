package com.applane.resourceoriented.hris.tour;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeCardPunchDataBusinessLogic;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.MarkAttendanceServlet;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.2
 * @category HRIS businesslogic
 */

public class TourRequestDateValidation implements OperationJob {
	// Job Define on "hris_tourrequests" Resource

	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		try {
			if (record.has("temp")) {
				return;
			}
			// String operationType = record.getOperationType();
			Date startDate = Translator.dateValue(record.getValue("startdate"));
			Date endDate = Translator.dateValue(record.getValue("enddate"));
			if (startDate.equals(endDate)) {
				boolean isLodging = Translator.booleanValue(record.getValue("includelodging"));
				if (isLodging) {
					throw new BusinessLogicException("Start Date and End Date Of Tour is Same So You Can Not Include Lodging.");
				}
			}
			if (startDate.after(endDate)) {
				throw new BusinessLogicException("Start Date Should Be Less Or Equal To End Date.");
			}

			Object advanceAmountObject = record.getValue("advanceamount");
			double advanceAmount = 0.0;
			double advanceAgainstTicktingAmount = Translator.doubleValue(record.getValue("advance_against_tickting"));
			if (advanceAmountObject != null && advanceAmountObject instanceof JSONObject) {
				advanceAmount = ((JSONObject) advanceAmountObject).optDouble("amount", 0.0);
			} else if (advanceAmountObject != null && advanceAmountObject instanceof JSONArray && ((JSONArray) advanceAmountObject).length() > 0) {
				advanceAmount = ((JSONArray) advanceAmountObject).getJSONObject(0).optDouble("amount", 0.0);
			}

			double totalAdvance = advanceAmount + advanceAgainstTicktingAmount;
			record.addUpdate("total_advance", totalAdvance);

			// Handle date validation while apply new tour requests
			Object hris_tour_details_lodging = record.getValue("hris_tour_details_lodging");
			Object hris_tour_details_fooding = record.getValue("hris_tour_details_fooding");
			Object hris_tour_details_travel = record.getValue("hris_tour_details_travel");
			Object hris_tour_details_hotel = record.getValue("hris_tour_details_hotel");

			Object hris_tour_local_expense = record.getValue("hris_tour_local_expense");
			Object hris_tour_other_expenses = record.getValue("hris_tour_other_expenses");

			if (hris_tour_details_lodging != null && hris_tour_details_lodging instanceof JSONArray) {
				JSONArray array = (JSONArray) hris_tour_details_lodging;
				for (int counter = 0; counter < array.length(); counter++) {

					Date fromDate = Translator.dateValue(array.getJSONObject(counter).opt("from_date"));
					Date toDate = Translator.dateValue(array.getJSONObject(counter).opt("to_date"));

					if (fromDate != null && startDate.after(fromDate)) {
						throw new BusinessLogicException("(Lodging Details) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
					if (toDate != null && endDate.before(toDate)) {
						throw new BusinessLogicException("(Lodging Details) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
				}
			}
			if (hris_tour_details_fooding != null && hris_tour_details_fooding instanceof JSONArray) {
				JSONArray array = (JSONArray) hris_tour_details_fooding;
				for (int counter = 0; counter < array.length(); counter++) {
					Date fromDate = Translator.dateValue(array.getJSONObject(counter).opt("from_date"));
					Date toDate = Translator.dateValue(array.getJSONObject(counter).opt("to_date"));
					if (fromDate != null && startDate.after(fromDate)) {
						throw new BusinessLogicException("(Fooding Details) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
					if (toDate != null && endDate.before(toDate)) {
						throw new BusinessLogicException("(Fooding Details) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
				}
			}

			if (hris_tour_details_travel != null && hris_tour_details_travel instanceof JSONArray) {
				JSONArray array = (JSONArray) hris_tour_details_travel;
				for (int counter = 0; counter < array.length(); counter++) {
					Date fromDate = Translator.dateValue(array.getJSONObject(counter).opt("from_date"));
					Date toDate = Translator.dateValue(array.getJSONObject(counter).opt("to_date"));
					if (fromDate != null && startDate.after(fromDate)) {
						throw new BusinessLogicException("(Travel Details) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
					if (toDate != null && endDate.before(toDate)) {
						throw new BusinessLogicException("(Travel Details) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
				}
			}

			if (hris_tour_details_hotel != null && hris_tour_details_hotel instanceof JSONArray) {
				JSONArray array = (JSONArray) hris_tour_details_hotel;
				for (int counter = 0; counter < array.length(); counter++) {
					Date fromDate = Translator.dateValue(array.getJSONObject(counter).opt("from_date"));
					Date toDate = Translator.dateValue(array.getJSONObject(counter).opt("to_date"));
					if (fromDate != null && startDate.after(fromDate)) {
						throw new BusinessLogicException("(Hotel Room Rent) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
					if (toDate != null && endDate.before(toDate)) {
						throw new BusinessLogicException("(Hotel Room Rent) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
				}
			}

			if (hris_tour_local_expense != null && hris_tour_local_expense instanceof JSONArray) {
				JSONArray array = (JSONArray) hris_tour_local_expense;
				for (int counter = 0; counter < array.length(); counter++) {
					Date date = Translator.dateValue(array.getJSONObject(counter).opt("from_date"));
					if (date != null && (startDate.after(date) || endDate.before(date))) {
						throw new BusinessLogicException("(Local Conveyance) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
				}
			}
			if (hris_tour_other_expenses != null && hris_tour_other_expenses instanceof JSONArray) {
				JSONArray array = (JSONArray) hris_tour_other_expenses;
				for (int counter = 0; counter < array.length(); counter++) {
					Date fromDate = Translator.dateValue(array.getJSONObject(counter).opt("from_date"));
					// Date toDate = Translator.dateValue(array.getJSONObject(counter).opt("to_date"));
					if (fromDate != null && startDate.after(fromDate)) {
						throw new BusinessLogicException("(Other Excenses) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					}
					// if (toDate != null && endDate.before(toDate)) {
					// throw new BusinessLogicException("From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
					// }
				}
			}

			tourRequestDateValidation(record);

			// Handle Advance required validation
			AdvanceRequiredValidation(record);

			// Hanlde any Tour request of employee not duplicate
			tourRequestDuplicateValidation(record);

			// Set Current date on tourrequests when request will be approved
			tourRequestApproveDate(record);

			// Set advancepaid is "1" when advance paid amount will be paid
			// tourAdvancePaidAmount(record);

			// Total no. of tour days must be equal to total days
			tourDaysValidation(record);

			// Set Current Date While Rejected Tour request
			tourRequestRejectDate(record);

			//
			advancePaidAmontValidation(record);
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			throw new RuntimeException("Some Error Occured. Trace :- [" + trace + "]");
		}
	}

	private void advancePaidAmontValidation(Record record) {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("update")) {
			Object tourRequestId = record.getValue("__key__");
			JSONObject advancePaidObject = (JSONObject) record.getValue("advancepaidamount");
			if (advancePaidObject != null) {
				ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
				try {
					Number advancePaidAmount = (Number) (advancePaidObject.opt("amount") == null ? 0 : advancePaidObject.opt("amount"));
					JSONObject currencyJsonObject = (JSONObject) advancePaidObject.opt("type");
					String advancePaidAmountType = currencyJsonObject.optString("currency");
					JSONObject query = new JSONObject();
					query.put(Data.Query.RESOURCE, "hris_tourrequests");
					JSONArray columns = new JSONArray();
					JSONObject advanceAmountColumnJson = new JSONObject();
					advanceAmountColumnJson.put("expression", "advanceamount");
					advanceAmountColumnJson.put("type", "currency");
					columns.put(advanceAmountColumnJson);
					query.put(Data.Query.COLUMNS, columns);
					query.put(Data.Query.FILTERS, "__key__ = " + tourRequestId);
					JSONObject expenseObject;
					expenseObject = ResourceEngine.query(query);
					JSONArray tourRequestArray = expenseObject.getJSONArray("hris_tourrequests");

					int tourRequestArrayCount = tourRequestArray == null ? 0 : tourRequestArray.length();
					if (tourRequestArrayCount > 0) {
						JSONObject advanceAmountObject = (JSONObject) tourRequestArray.getJSONObject(0).opt("advanceamount");
						if (advanceAmountObject != null) {
							Number advanceAmount = 0.0;
							String advanceAmountType = null;
							advanceAmount = (Number) advanceAmountObject.opt("amount");
							JSONObject advanceAmountTypeObject = (JSONObject) advanceAmountObject.opt("type");
							advanceAmountType = (String) advanceAmountTypeObject.opt("currency");
							if (advancePaidAmount.doubleValue() > advanceAmount.doubleValue()) {
								throw new BusinessLogicException("Paid Amount must be equal to OR less than Advance Amount.");
							} else if (advancePaidAmount.doubleValue() <= advanceAmount.doubleValue() && (!advancePaidAmountType.equalsIgnoreCase(advanceAmountType))) {
								throw new BusinessLogicException("Paid Amount Type must be same as Advance Amount Type.");
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BusinessLogicException("" + e.getMessage());
				}
				record.addUpdate("advancepaid", 1);
			}
		}
	}

	private void AdvanceRequiredValidation(Record record) {
		Object advanceRequired = record.getValue("advancerequired");
		JSONObject advanceAmountObject = (JSONObject) record.getValue("advanceamount");
		boolean isAdvanceRequired;
		isAdvanceRequired = CommunicationUtility.booleanValue(advanceRequired);
		if (isAdvanceRequired && advanceAmountObject == null) {
			throw new BusinessLogicException("Kindly Enter Advance Amount.");
		} else if (!isAdvanceRequired && advanceAmountObject != null) {
			throw new BusinessLogicException("Kindly Check Advance Required Checkbox.");
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		if (record.has("temp")) {
			return;
		}
		String operationType = record.getOperationType();
		try {
			Object key = record.getKey();
			JSONArray tourDetailArray = getTourDetails(key, "hris_tour_details_lodging");
			double[] expectedActualAmount = { 0.0, 0.0, 0.0, 0.00 };// [0] expected amount and [1] actual amount
			getAmount(tourDetailArray, expectedActualAmount);
			tourDetailArray = getTourDetails(key, "hris_tour_details_fooding");
			getAmount(tourDetailArray, expectedActualAmount);
			tourDetailArray = getTourDetails(key, "hris_tour_details_travel");
			getAmount(tourDetailArray, expectedActualAmount);
			tourDetailArray = getTourDetails(key, "hris_tour_details_hotel");
			getAmount(tourDetailArray, expectedActualAmount);

			tourDetailArray = getTourDetailsLocalOther(key, "hris_tour_local_expense");
			getAmountLocalOther(tourDetailArray, expectedActualAmount);

			tourDetailArray = getTourDetailsLocalOther(key, "hris_tour_other_expenses");
			getAmountLocalOther(tourDetailArray, expectedActualAmount);

			updateExpectedAndActualAmount(key, expectedActualAmount);
			if (operationType.equals(Updates.Types.UPDATE)) {
				tourBalanceAmount(record);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private void updateExpectedAndActualAmount(Object key, double[] expectedActualAmount) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_tourrequests");
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, key);
		row.put("expected_amount", expectedActualAmount[0]);
		row.put("actual_amount", expectedActualAmount[1]);
		row.put("approved_amount", expectedActualAmount[2]);
		row.put("access_amount", expectedActualAmount[3]);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);

	}

	private void getAmountLocalOther(JSONArray tourDetailArray, double[] expectedActualAmount) throws JSONException {
		if (tourDetailArray != null && tourDetailArray.length() > 0) {
			for (int i = 0; i < tourDetailArray.length(); i++) {
				expectedActualAmount[1] += tourDetailArray.getJSONObject(i).opt("amount") == null ? 0 : Translator.doubleValue(tourDetailArray.getJSONObject(i).opt("amount"));
				expectedActualAmount[2] += tourDetailArray.getJSONObject(i).opt("approved_amount") == null ? 0 : Translator.doubleValue(tourDetailArray.getJSONObject(i).opt("approved_amount"));
			}
		}
	}

	private void getAmount(JSONArray tourDetailArray, double[] expectedActualAmount) throws JSONException {
		if (tourDetailArray != null && tourDetailArray.length() > 0) {
			for (int i = 0; i < tourDetailArray.length(); i++) {
				expectedActualAmount[0] += tourDetailArray.getJSONObject(i).opt("expected_amount") == null ? 0 : Translator.doubleValue(tourDetailArray.getJSONObject(i).opt("expected_amount"));
				expectedActualAmount[1] += tourDetailArray.getJSONObject(i).opt("actual_amount") == null ? 0 : Translator.doubleValue(tourDetailArray.getJSONObject(i).opt("actual_amount"));
				expectedActualAmount[2] += tourDetailArray.getJSONObject(i).opt("approved_amount") == null ? 0 : Translator.doubleValue(tourDetailArray.getJSONObject(i).opt("approved_amount"));
				double accessAmount = Translator.doubleValue(tourDetailArray.getJSONObject(i).opt("actual_amount")) - Translator.doubleValue(tourDetailArray.getJSONObject(i).opt("expected_amount"));
				if (accessAmount > 0) {
					expectedActualAmount[3] += accessAmount;
				}
			}
		}
	}

	private JSONArray getTourDetailsLocalOther(Object key, String resource) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, resource);
		JSONArray columns = new JSONArray();
		columns.put("amount");
		columns.put("approved_amount");
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, "tourid = " + key);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray(resource);
		return rows;
	}

	private JSONArray getTourDetails(Object key, String resource) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, resource);
		JSONArray columns = new JSONArray();
		columns.put("expected_amount");
		columns.put("actual_amount");
		columns.put("approved_amount");
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, "tourid = " + key);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray(resource);
		return rows;
	}

	private void tourRequestDateValidation(Record record) {
		// get start date and end date from record of hris_tourrequest
		Object creationDateObject = record.getValue("creationdate");
		Object departDateObject = record.getValue("startdate");
		Object arriveDateObject = record.getValue("enddate");
		String operation = record.getOperationType();

		// java.util.Date creationDate = null;
		java.util.Date departedDate = null;
		java.util.Date arrivedDate = null;

		if (operation.equalsIgnoreCase("insert")) {
			// check depart date must be greater or equal to creation date
			if (creationDateObject != null && departDateObject != null && arriveDateObject != null) {
				if ((creationDateObject instanceof String) && (departDateObject instanceof String) && (arriveDateObject instanceof String)) {
					try {
						// creationDate = CommunicationUtility.checkDateFormat(creationDateObject);
						departedDate = CommunicationUtility.checkDateFormat(departDateObject);
						arrivedDate = CommunicationUtility.checkDateFormat(arriveDateObject);
					} catch (Exception e) {
						e.printStackTrace();
						throw new BusinessLogicException("Some unknown error occured while date parsing." + e.getMessage());
					}
					if (arrivedDate.before(departedDate)) {
						throw new BusinessLogicException("Arrive Date must be greater than or equal to Depart Date.");
					}

				}
			}
		} else if (operation.equalsIgnoreCase("update")) {
			if (departDateObject != null && arriveDateObject != null && creationDateObject != null) {
				// creationDate = CommunicationUtility.checkDateFormat(creationDateObject);
				departedDate = CommunicationUtility.checkDateFormat(departDateObject);
				arrivedDate = CommunicationUtility.checkDateFormat(arriveDateObject);
				// / compare parsed arrivedDate and departedDate
				if (arrivedDate.before(departedDate)) {
					throw new BusinessLogicException("Arrived Date must be greater than or equal to Deprated Date.");
				}
			}
		}
	}

	private void tourRequestDuplicateValidation(Record record) {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("insert")) {
			Object tourRequestStartDateObject = record.getValue("startdate");
			Object tourRequestEndDateObject = record.getValue("enddate");
			Object employeeId = record.getValue("employeeid");
			isDateRangeConflict(tourRequestStartDateObject, tourRequestEndDateObject, employeeId);
		}
	}

	public void isDateRangeConflict(Object tourRequestStartDate, Object tourRequestEndDate, Object employeeID) {

		try {
			JSONObject tourQuery = new JSONObject();
			tourQuery.put(Data.Query.RESOURCE, "hris_tourrequests");
			JSONArray tourColumnArray = new JSONArray();
			tourColumnArray.put("__key__");
			tourColumnArray.put("startdate");
			tourColumnArray.put("enddate");
			tourQuery.put(Data.Query.COLUMNS, tourColumnArray);
			tourQuery.put(Data.Query.FILTERS, ("employeeid= '" + employeeID + "' and startdate >= '" + tourRequestStartDate + "' and  startdate <= '" + tourRequestEndDate + "' and tourstatusid IN(1, 2)"));
			JSONArray groupby = new JSONArray();
			groupby.put("__key__");
			tourQuery.put(Data.Query.GROUPS, groupby);
			JSONObject tourOject;
			tourOject = ResourceEngine.query(tourQuery);
			JSONArray tourRequestArray = tourOject.getJSONArray("hris_tourrequests");
			int tourRequestCount = (tourRequestArray == null || tourRequestArray.length() == 0) ? 0 : tourRequestArray.length();
			if (tourRequestCount > 0) {
				throw new BusinessLogicException("You have already requested for tour in this duration.");
			} else {
				tourQuery = new JSONObject();
				tourQuery.put(Data.Query.RESOURCE, "hris_tourrequests");
				tourColumnArray = new JSONArray();
				tourColumnArray.put("__key__");
				tourColumnArray.put("startdate");
				tourColumnArray.put("enddate");
				tourQuery.put(Data.Query.COLUMNS, tourColumnArray);
				tourQuery.put(Data.Query.FILTERS, ("employeeid= '" + employeeID + "' and  startdate <= '" + tourRequestStartDate + "' and  enddate >= '" + tourRequestStartDate + "' and tourstatusid != 4"));
				JSONArray groupbyArray = new JSONArray();
				groupbyArray.put("__key__");
				tourQuery.put(Data.Query.GROUPS, groupbyArray);
				tourOject = ResourceEngine.query(tourQuery);
				tourRequestArray = tourOject.getJSONArray("hris_tourrequests");
				tourRequestCount = (tourRequestArray == null || tourRequestArray.length() == 0) ? 0 : tourRequestArray.length();
				if (tourRequestCount > 0) {
					throw new BusinessLogicException("You have already requested for tour in this duration.");
				}
			}
		} catch (JSONException e) {
			throw new BusinessLogicException("Error come while check range conflict" + e.getMessage());
		}

	}

	private void tourRequestApproveDate(Record record) {
		try {
			String operation = record.getOperationType();
			if (operation.equalsIgnoreCase("update")) {
				Object employeeId = record.getValue("employeeid");
				Object startDate = record.getValue("startdate");
				Object endDate = record.getValue("enddate");
				int newStatus = (Integer) record.getValue("tourstatusid");
				int oldStatus = (Integer) record.getOldValue("tourstatusid");

				if (startDate != null && endDate != null && ((oldStatus == HRISConstants.TOUR_NEW && newStatus == HRISConstants.TOUR_APPROVED) || (oldStatus == HRISConstants.TOUR_REJECTED && newStatus == HRISConstants.TOUR_APPROVED))) {
					String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());
					record.addUpdate("approveddate", TODAY_DATE);
					if (!record.has("approvedbyid")) {
						String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
						int currentUserId = getCurrentUserId(CURRENT_USER_EMAILID);
						record.addUpdate("approvedbyid", currentUserId);
					}
					long diff = DataTypeUtilities.differenceBetweenDates(Translator.dateValue(startDate), Translator.dateValue(endDate)) + 1;
					Calendar cal = Calendar.getInstance();
					for (int counter = 0; counter < diff; counter++) {
						cal.setTime(Translator.dateValue(startDate));
						cal.add(Calendar.DATE, counter);
						Date tourDate = cal.getTime();
						int[] halfOffDay = new int[] { 0 };
						boolean tourDateIsOffDay = EmployeeCardPunchDataBusinessLogic.getOffDayToMarkEWD(employeeId, tourDate, halfOffDay);
						int attendanceTypeId = HRISApplicationConstants.ATTENDANCE_TOUR;
						if (tourDateIsOffDay) {
							if (halfOffDay[0] == 1) {
								attendanceTypeId = HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF;
							} else {
								attendanceTypeId = HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY;
							}
						}
						JSONArray attendanceArray = getAttendance(employeeId, EmployeeSalaryGenerationServlet.getDateInString(tourDate));
						if (attendanceArray == null || attendanceArray.length() == 0) {
							MarkAttendanceServlet.updateStaffAttendance(attendanceTypeId, employeeId, EmployeeSalaryGenerationServlet.getDateInString(tourDate), null, "");
						}
					}
				} else if (startDate != null && endDate != null && ((oldStatus == HRISConstants.TOUR_APPROVED && newStatus == HRISConstants.TOUR_CANCEL) || (oldStatus == HRISConstants.TOUR_APPROVED && newStatus == HRISConstants.TOUR_REJECTED))) {
					long diff = DataTypeUtilities.differenceBetweenDates(Translator.dateValue(startDate), Translator.dateValue(endDate)) + 1;
					Calendar cal = Calendar.getInstance();
					for (int counter = 0; counter < diff; counter++) {
						cal.setTime(Translator.dateValue(startDate));
						cal.add(Calendar.DATE, counter);
						Date tourDate = cal.getTime();
						int[] halfOffDay = new int[] { 0 };
						boolean tourDateIsOffDay = EmployeeCardPunchDataBusinessLogic.getOffDayToMarkEWD(employeeId, tourDate, halfOffDay);
						int attendanceTypeId = HRISApplicationConstants.ATTENDANCE_UNKNOWN;
						if (tourDateIsOffDay) {
							if (halfOffDay[0] == 1) {
								attendanceTypeId = HRISApplicationConstants.ATTENDANCE_HALF_DAY_ABSENT;
							} else {
								attendanceTypeId = HRISApplicationConstants.ATTENDANCE_OFF;
							}
						}
						JSONArray attendanceArray = getAttendance(employeeId, EmployeeSalaryGenerationServlet.getDateInString(tourDate));
						if (attendanceArray == null || attendanceArray.length() == 0) {
							MarkAttendanceServlet.updateStaffAttendance(attendanceTypeId, employeeId, EmployeeSalaryGenerationServlet.getDateInString(tourDate), null, "");
						}
					}
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(TourRequestDateValidation.class.getName(), e);
			LogUtility.writeLog("TourRequestDateValidation >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured, Please Contace to Admin.");
		}
	}

	private JSONArray getAttendance(Object employeeId, String dateInString) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "employeeattendance");
			JSONArray columns = new JSONArray();
			columns.put("__key__");
			query.put(Data.Query.COLUMNS, columns);
			query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND attendancedate = '" + dateInString + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			return object.getJSONArray("employeeattendance");

		} catch (Exception e) {
			throw new BusinessLogicException("Exception come while get current userId" + e.getMessage());
		}
	}

	private void tourRequestRejectDate(Record record) {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("update")) {
			int newStatus = (Integer) record.getValue("tourstatusid");
			int oldStatus = (Integer) record.getOldValue("tourstatusid");
			if (oldStatus == HRISConstants.TOUR_NEW && newStatus == HRISConstants.TOUR_REJECTED) {
				String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
				record.addUpdate("approveddate", TODAY_DATE);
				if (!record.has("approvedbyid")) {
					String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
					int currentUserId = getCurrentUserId(CURRENT_USER_EMAILID);
					record.addUpdate("approvedbyid", currentUserId);
				}
			}
		}
	}

	private void tourAdvancePaidAmount(Record record) {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("update")) {
			if (record.has("advancepaidamount")) {
				Object advancePaidAmountObject = record.getValue("advancepaidamount");
				if (advancePaidAmountObject != null) {
					record.addUpdate("advancepaid", 1);
				}
			}
		}
	}

	private void tourBalanceAmount(Record record) {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("update")) {
			Object tourRequestId = record.getValue("__key__");
			// JSONObject advancePaidAmountObject = (JSONObject) record.getValue("advancepaidamount");
			JSONObject totalPaidAmountObject = (JSONObject) record.getValue("totalpaidamount");
			if (totalPaidAmountObject != null) {
				calculateBalanaceAmount(tourRequestId);
			}
		}
	}

	private void calculateBalanaceAmount(Object tourRequestId) {
		try {
			JSONObject tourQuery = new JSONObject();
			tourQuery.put(Data.Query.RESOURCE, "hris_tourrequests");
			JSONArray tourColumnArray = new JSONArray();

			JSONObject advancePaidAmountColumnJson = new JSONObject();
			advancePaidAmountColumnJson.put("expression", "advancepaidamount");
			advancePaidAmountColumnJson.put("type", "currency");
			tourColumnArray.put(advancePaidAmountColumnJson);

			JSONObject totalpaidAmountColumnJson = new JSONObject();
			totalpaidAmountColumnJson.put("expression", "totalpaidamount");
			totalpaidAmountColumnJson.put("type", "currency");
			tourColumnArray.put(totalpaidAmountColumnJson);

			tourQuery.put(Data.Query.COLUMNS, tourColumnArray);
			tourQuery.put(Data.Query.FILTERS, "__key__ = " + tourRequestId);
			JSONObject tourObject;
			tourObject = ResourceEngine.query(tourQuery);
			JSONArray tourRequestArray = tourObject.getJSONArray("hris_tourrequests");
			int tourRequestArrayCount = tourRequestArray == null ? 0 : tourRequestArray.length();
			if (tourRequestArrayCount > 0) {
				JSONObject totalPaidAmountObject = (JSONObject) tourRequestArray.getJSONObject(0).opt("totalpaidamount");
				Double totalPaidAmount = (Double) totalPaidAmountObject.get("amount");
				JSONObject totalPaidAmountTypeObject = (JSONObject) totalPaidAmountObject.get("type");
				// String paidAmountType = (String) totalPaidAmountTypeObject.get("currency");
				int paidAmountTypeId = (Integer) totalPaidAmountTypeObject.get("__key__");
				Double tourAdvancePaidAmount = null;
				// String advancePaidAmountType;
				int advancePaidAmountTypeId = 0;

				JSONObject advancePaidAmountObject = (JSONObject) tourRequestArray.getJSONObject(0).opt("advancepaidamount");
				if (advancePaidAmountObject != null) {
					tourAdvancePaidAmount = (Double) advancePaidAmountObject.get("amount");
					JSONObject advancePaidAmountTypeObject = (JSONObject) advancePaidAmountObject.get("type");
					// advancePaidAmountType = (String) advancePaidAmountTypeObject.get("currency");
					advancePaidAmountTypeId = (Integer) advancePaidAmountTypeObject.get("__key__");
				} else {
					tourAdvancePaidAmount = 0.0;
					// advancePaidAmountType = paidAmountType;
					advancePaidAmountTypeId = paidAmountTypeId;

				}
				Double balanceAmount = totalPaidAmount - tourAdvancePaidAmount;

				JSONObject updates = new JSONObject();
				updates.put(Data.Query.RESOURCE, "hris_tourrequests");
				JSONObject row = new JSONObject();
				row.put("__key__", tourRequestId);
				row.put("balanceamount_amount", balanceAmount);
				row.put("balanceamount_type", advancePaidAmountTypeId);
				row.put("temp", "temp");
				updates.put(Data.Update.UPDATES, row);
				ResourceEngine.update(updates);
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(TourRequestDateValidation.class.getName(), e);
			LogUtility.writeLog("TourRequestDateValidation >> calculateBalanaceAmount >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Error come while calculate balance amount." + e.getMessage());
		}
	}

	@SuppressWarnings("rawtypes")
	private void tourDaysValidation(Record record) {

		Object tourRequestId = record.getValue("__key__") == null ? " " : record.getValue("__key__");
		int totalNoOfDays = 0;
		String operation = record.getOperationType();

		int totalDays = (Integer) record.getValue("totaldays") == null ? 0 : (Integer) record.getValue("totaldays");
		if (totalDays != 0) {
			if (operation.equalsIgnoreCase("insert")) {
				try {
					Object tourDetails = record.getValue("hris_tourdetails");

					JSONArray tourDetailArray = new JSONArray();
					if (tourDetails != null && tourDetails instanceof JSONArray) {
						tourDetailArray = (JSONArray) tourDetails;
					} else if (tourDetails != null) {
						tourDetailArray.put(tourDetails);
					}
					// JSONArray leaveRuleArray = (JSONArray) leaveruleDetails;
					int tourDetailCount = (tourDetailArray == null || tourDetailArray.length() == 0) ? 0 : tourDetailArray.length();
					if (tourDetailCount > 0) {
						if (tourDetails instanceof JSONArray) {
							for (int counter = 0; counter < tourDetailArray.length(); counter++) {
								totalNoOfDays = totalNoOfDays + tourDetailArray.getJSONObject(counter).optInt("numberofdays");
							}
						} else if (tourDetails instanceof JSONObject) {
							totalNoOfDays = ((JSONObject) tourDetails).optInt("numberofdays");
						}
						if (totalNoOfDays > totalDays || totalNoOfDays < totalDays) {
							throw new BusinessLogicException("Number of Days must be equal to Total Days.");
						}
					}
				} catch (JSONException e) {
					String trace = ExceptionUtils.getExceptionTraceMessage(TourRequestDateValidation.class.getName(), e);
					LogUtility.writeLog("TourRequestDateValidation >> tourDaysValidation >> Exception Trace >> " + trace);
					throw new BusinessLogicException("Error Come." + e.getMessage());
				}
			} else if (operation.equalsIgnoreCase("update")) {
				Object updatedTourDetails = record.getValue("hris_tourdetails");
				JSONArray updatedTourDetailArray = new JSONArray();
				if (updatedTourDetails != null && updatedTourDetails instanceof JSONArray) {
					updatedTourDetailArray = (JSONArray) updatedTourDetails;
				} else if (updatedTourDetails != null) {
					updatedTourDetailArray.put(updatedTourDetails);
				}
				int updatedTourDetailCount = updatedTourDetailArray == null ? 0 : updatedTourDetailArray.length();
				if (updatedTourDetailCount > 0) {
					Map<Integer, Integer> tourMap = new HashMap<Integer, Integer>();
					try {
						// populate weightage map from record set
						for (int counter = 0; counter < updatedTourDetailCount; counter++) {
							Integer tourDetailpk = getKey(updatedTourDetailArray.getJSONObject(counter).get("__key__"));
							if (updatedTourDetailArray.getJSONObject(counter).has("numberofdays")) {
								int noOfDays = updatedTourDetailArray.getJSONObject(counter).optInt("numberofdays");
								tourMap.put(tourDetailpk, noOfDays);
							} else {
								tourMap.put(tourDetailpk, 0);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new BusinessLogicException("Error Come .." + e.getMessage());
					}

					// populate Tour map from query set
					try {
						JSONObject tourQuery = new JSONObject();
						tourQuery.put(Data.Query.RESOURCE, "hris_tourdetails");
						JSONArray tourColumnArray = new JSONArray();
						tourColumnArray.put("__key__");
						tourColumnArray.put("numberofdays");
						tourQuery.put(Data.Query.COLUMNS, tourColumnArray);
						tourQuery.put(Data.Query.FILTERS, "tourrequestid = " + tourRequestId);
						ApplaneDatabaseEngine resourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
						JSONObject tourObject;
						tourObject = resourceEngine.query(tourQuery);
						JSONArray tourArray = tourObject.getJSONArray("hris_tourdetails");
						int tourCount = (tourArray == null || tourArray.length() == 0) ? 0 : tourArray.length();
						for (int tourCounter = 0; tourCounter < tourCount; tourCounter++) {
							Integer tourDetailKey = getKey(tourArray.getJSONObject(tourCounter).optInt("__key__"));
							int noofDays = tourArray.getJSONObject(tourCounter).optInt("numberofdays");
							if (!tourMap.containsKey(tourDetailKey)) {
								tourMap.put(tourDetailKey, noofDays);
							}
						}

						// now iterate map and calculate sum of NO OF LEAVES
						Iterator it = tourMap.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry pairs = (Map.Entry) it.next();
							totalNoOfDays = totalNoOfDays + (Integer) pairs.getValue();
						}
						if (totalNoOfDays > totalDays || totalNoOfDays < totalDays) {
							throw new BusinessLogicException("Number of Days must be equal to Total Days.");
						}
					} catch (JSONException e) {
						String trace = ExceptionUtils.getExceptionTraceMessage(TourRequestDateValidation.class.getName(), e);
						LogUtility.writeLog("TourRequestDateValidation >> tourDaysValidation >> Exception Trace >> " + trace);
						throw new BusinessLogicException("Error Come..." + e.getMessage());
					}
				}
			}
		}
	}

	private int getKey(Object keyObject) {
		Integer key = null;
		try {
			if (keyObject instanceof JSONObject) {
				key = ((JSONObject) keyObject).getInt("key");
			} else {
				key = (Integer) keyObject;
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(TourRequestDateValidation.class.getName(), e);
			LogUtility.writeLog("TourRequestDateValidation >> getKey >> Exception Trace >> " + trace);
		}
		return key;
	}

	private int getCurrentUserId(String CURRENT_USER_EMAILID) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees");
			JSONArray columns = new JSONArray();
			columns.put("__key__");
			query.put(Data.Query.COLUMNS, columns);
			query.put(Data.Query.FILTERS, "officialemailid = '" + CURRENT_USER_EMAILID + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("hris_employees");
			int currentUserId = rows.getJSONObject(0).optInt("__key__");
			return currentUserId;

		} catch (Exception e) {
			throw new BusinessLogicException("Exception come while get current userId" + e.getMessage());
		}
	}

}
