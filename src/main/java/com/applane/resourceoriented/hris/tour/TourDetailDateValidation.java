package com.applane.resourceoriented.hris.tour;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic BusinessLogic defined on "hris_tourdetails" resource
 */

public class TourDetailDateValidation implements OperationJob {
	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		Object fromDateObject = record.getValue("fromdate");
		Object toDateObject = record.getValue("todate");
		Object checkInObject = record.getValue("checkin");
		Object checkOutObject = record.getValue("checkout");
		Object tourRequestId = record.getValue("tourrequestid");
		String operation = record.getOperationType();

		Date fromDate = null;
		Date toDate = null;
		Date checkInDate = null;
		Date checkOutDate = null;

		// handle case of default row enabled in nested view
		// which insert a blank row, so skip if such blank row found
		if (fromDateObject == null && toDateObject == null && checkInObject == null && checkOutObject == null) {
			return;
		}

		try {
			JSONObject tourQuery = new JSONObject();
			tourQuery.put(Data.Query.RESOURCE, "hris_tourrequests");
			JSONArray tourColumnArray = new JSONArray();
			tourColumnArray.put("startdate");
			tourColumnArray.put("enddate");
			tourQuery.put(Data.Query.COLUMNS, tourColumnArray);
			tourQuery.put(Data.Query.FILTERS, "__key__= " + tourRequestId);
			ApplaneDatabaseEngine resourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
			JSONObject tourObject;
			tourObject = resourceEngine.query(tourQuery);
			JSONArray tourArray = tourObject.getJSONArray("hris_tourrequests");

			Object startDateObject = tourArray.getJSONObject(0).opt("startdate");
			Object endDateObject = tourArray.getJSONObject(0).opt("enddate");

			Date startDate = null;
			Date endDate = null;

			if (startDateObject != null && endDateObject != null) {
				startDate = Translator.dateValue(startDateObject);
				endDate = Translator.dateValue(endDateObject);
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
			String departDate = dateFormat.format(startDate);
			String arriveDate = dateFormat.format(endDate);

			if (operation.equalsIgnoreCase("insert")) {
				if (fromDateObject != null && toDateObject != null) {
					fromDate = Translator.dateValue(fromDateObject);
					toDate = Translator.dateValue(toDateObject);
					if (fromDate.before(startDate)) {
						throw new BusinessLogicException("From Date must be greater or equal to Tour Request Depart Date(" + departDate + ").");
					}
					if (endDate.before(toDate)) {
						throw new BusinessLogicException("To Date must be less or equal to Tour Request Arrive Date (" + arriveDate + ").");
					}
					if (toDate.before(fromDate)) {
						throw new BusinessLogicException("Tour To Date must be greater or equal to From Date.");
					}
				}

				String tourDetailFromDate = dateFormat.format(fromDate);
				String tourDetailToDate = dateFormat.format(toDate);

				if (checkInObject != null && checkOutObject != null) {
					checkInDate = Translator.dateValue(checkInObject);
					checkOutDate = Translator.dateValue(checkOutObject);

					if (checkInDate.before(fromDate)) {
						throw new BusinessLogicException("CheckIn Date must be greater or equal to tour From Date(" + tourDetailFromDate + ").");
					}

					if (toDate.before(checkOutDate)) {
						throw new BusinessLogicException("CheckOut Date must be less or equal to tour To Date (" + tourDetailToDate + ").");
					}

					if (checkOutDate.before(checkInDate)) {
						throw new BusinessLogicException("Check Out Date must be greater or equal to Check In Date.");
					}
				}

			} else if (operation.equalsIgnoreCase("update")) {
				if (fromDateObject != null && toDateObject != null) {
					fromDate = Translator.dateValue(fromDateObject);
					toDate = Translator.dateValue(toDateObject);
					// / compare parsed valid from date and valid to date
					if (fromDate.before(startDate)) {
						throw new BusinessLogicException("From Date must be greater or equal to Tour Request Depart Date(" + departDate + ").");
					}
					if (endDate.before(toDate)) {
						throw new BusinessLogicException("To Date must be less or equal to Tour Request Arrive Date (" + arriveDate + ").");
					}
					if (toDate.before(fromDate)) {
						throw new BusinessLogicException("Tour To Date must be greater or equal to From Date.");
					}
				}

				String tourDetailFromDate = dateFormat.format(fromDate);
				String tourDetailToDate = dateFormat.format(toDate);

				if (checkInObject != null && checkOutObject != null) {
					checkInDate = Translator.dateValue(checkInObject);
					checkOutDate = Translator.dateValue(checkOutObject);

					if (checkInDate.before(fromDate)) {
						throw new BusinessLogicException("CheckIn Date must be greater or equal to tour From Date(" + tourDetailFromDate + ").");
					}

					if (toDate.before(checkOutDate)) {
						throw new BusinessLogicException("CheckOut Date must be less or equal to tour To Date (" + tourDetailToDate + ").");
					}

					if (checkOutDate.before(checkInDate)) {
						throw new BusinessLogicException("Check Out Date must be greater or equal to Check In Date.");
					}
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("TourDetailDateValidation >> Exception >> " + trace);
			throw new BusinessLogicException("" + e.getMessage());
		}
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
