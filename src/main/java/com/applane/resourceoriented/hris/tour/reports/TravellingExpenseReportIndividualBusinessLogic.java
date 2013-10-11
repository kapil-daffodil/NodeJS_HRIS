package com.applane.resourceoriented.hris.tour.reports;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class TravellingExpenseReportIndividualBusinessLogic implements QueryJob {

	@Override
	public void onQuery(Query query) throws DeadlineExceededException {

	}

	@Override
	public void onResult(Result result) throws DeadlineExceededException {
		try {
			JSONArray records = result.getRecords();
			if (records != null && records.length() > 0) {
				JSONObject recordObject = records.getJSONObject(0);
				// Object key = recordObject.opt(Updates.KEY);

				double advance = Translator.doubleValue(recordObject.opt("advanceamount") == null ? 0.0 : recordObject.getJSONObject("advanceamount").opt("amount"));
				advance += Translator.doubleValue(recordObject.opt("advance_against_tickting") == null ? 0.0 : recordObject.opt("advance_against_tickting"));

				JSONObject employeeObject = recordObject.getJSONObject("employeeid");
				JSONArray tourDetailsArray = recordObject.getJSONArray("hris_tourdetails");
				JSONArray lodgingDetailsArray = recordObject.getJSONArray("hris_tour_details_lodging");
				JSONArray localExpenseDetailsArray = recordObject.getJSONArray("hris_tour_local_expense");
				JSONArray foodingDetailsArray = recordObject.getJSONArray("hris_tour_details_fooding");
				JSONArray travelDetailsArray = recordObject.getJSONArray("hris_tour_details_travel");
				JSONArray hotelDetailsArray = recordObject.getJSONArray("hris_tour_details_hotel");
				JSONArray tourOtherExpensesArray = recordObject.getJSONArray("hris_tour_other_expenses");
				Object accessAmount = recordObject.opt("access_amount");
				Object excessClameReason = recordObject.opt("excess_claim_reason");
				Object tourCode = recordObject.opt("tourcode");

				double[] totalAmount = { 0.0, 0.0, 0.0 };

				Object lodgingObject = getLodgingObject(lodgingDetailsArray, totalAmount);
				Object localExpenseDetails = getLocalExpenseObject(localExpenseDetailsArray, totalAmount);
				Object foodingDetails = getFoodingObject(foodingDetailsArray, totalAmount);
				Object travelDetails = getTravelObject(travelDetailsArray, totalAmount);
				Object hotelDetails = getHotelObject(hotelDetailsArray, totalAmount);
				Object tourOtherDetails = getLocalExpenseObject(tourOtherExpensesArray, totalAmount);

				JSONArray configurationSetup = getConfigurationSetup();
				JSONObject tourDetailsObject = new JSONObject();
				tourDetailsObject.put("tourPlaces", getTourPlaces(tourDetailsArray));
				tourDetailsObject.put("recordObject", recordObject);
				tourDetailsObject.put("employeeObject", employeeObject);
				tourDetailsObject.put("lodgingObject", lodgingObject);
				tourDetailsObject.put("localExpenseDetails", localExpenseDetails);
				tourDetailsObject.put("foodingDetails", foodingDetails);
				tourDetailsObject.put("travelDetails", travelDetails);
				tourDetailsObject.put("hotelDetails", hotelDetails);
				tourDetailsObject.put("tourOtherDetails", tourOtherDetails);
				if (configurationSetup != null && configurationSetup.length() > 0) {
					tourDetailsObject.put("configurationSetup", configurationSetup.getJSONObject(0));
				}

				tourDetailsObject.put("approverAmount", new DecimalFormat("#.00").format(totalAmount[0]));
				tourDetailsObject.put("expectedAmount", new DecimalFormat("#.00").format(totalAmount[1]));
				tourDetailsObject.put("actualAmount", new DecimalFormat("#.00").format(totalAmount[2]));
				tourDetailsObject.put("advance", new DecimalFormat("#.00").format(advance));
				tourDetailsObject.put("tourCode", tourCode);

				tourDetailsObject.put("accessAmount", new DecimalFormat("#.00").format(Translator.doubleValue(accessAmount)));
				if (Translator.doubleValue(accessAmount) > 0.0) {
					tourDetailsObject.put("excessClameReason", excessClameReason);
				}

				tourDetailsObject.put("netPayable", new DecimalFormat("#.00").format((totalAmount[0] - advance)));
				records.put(new JSONObject().put("tourDetailsObject", tourDetailsObject));

			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("TravellingExpenseReportIndividualBusinessLogic >> Exception >> " + trace);
		}
	}

	private Object getTourPlaces(JSONArray tourDetailsArray) throws Exception {
		String placeOfTours = "";
		List<Integer> list = new ArrayList<Integer>();
		for (int counter = 0; counter < tourDetailsArray.length(); counter++) {
			int cityId = Translator.integerValue(tourDetailsArray.getJSONObject(counter).opt("cityid"));
			String place = Translator.stringValue(tourDetailsArray.getJSONObject(counter).opt("cityid.name"));
			if (!list.contains(cityId)) {
				list.add(cityId);
				if (placeOfTours.length() > 0) {
					placeOfTours += ",";
				}
				placeOfTours += place;
			}
		}
		return placeOfTours;
	}

	private Object getHotelObject(JSONArray hotelDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < hotelDetailsArray.length(); counter++) {
			JSONObject object = hotelDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("#.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("#.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("actual_amount", new DecimalFormat("#.00").format(Translator.doubleValue(object.opt("actual_amount"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("actual_amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private Object getTravelObject(JSONArray travelDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < travelDetailsArray.length(); counter++) {
			JSONObject object = travelDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("actual_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("actual_amount"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("actual_amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private Object getFoodingObject(JSONArray foodingDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < foodingDetailsArray.length(); counter++) {
			JSONObject object = foodingDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("actual_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("actual_amount"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("actual_amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private Object getLocalExpenseObject(JSONArray localExpenseDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < localExpenseDetailsArray.length(); counter++) {
			JSONObject object = localExpenseDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("amount"))));
			object.put("killo_meters", new DecimalFormat("#.##").format(Translator.doubleValue(object.opt("killo_meters"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private Object getLodgingObject(JSONArray lodgingDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < lodgingDetailsArray.length(); counter++) {
			JSONObject object = lodgingDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("actual_amount"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("actual_amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private JSONArray getConfigurationSetup() throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		columnArray.put("organizationname");
		columnArray.put("organizationlogo");
		columnArray.put("address");
		query.put(Data.Query.COLUMNS, columnArray);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_mailconfigurations");
	}
}
