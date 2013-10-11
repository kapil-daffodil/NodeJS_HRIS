package com.applane.resourceoriented.hris.performance;

import java.text.DecimalFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.google.apphosting.api.DeadlineExceededException;

public class KeyPerformanceRatingReportBusinessLogic implements QueryJob {

	@Override
	public void onQuery(Query arg0) throws DeadlineExceededException {

	}

	@Override
	public void onResult(Result result) throws DeadlineExceededException {

		LogUtility.writeLog(getClass().getName() + " >> onResult Begin");

		try {
			JSONArray records = result.getRecords();
			int noOfRecords = (records == null) ? 0 : records.length();

			long totalKeyPerformaceNumberObtained = 0;

			for (int i = 0; i < noOfRecords; i++) {

				JSONObject jsonObject = records.optJSONObject(i);

				long keyPerformanceIndicatorNumber = jsonObject.optLong(HrisKinds.KeyPerformanceRating.KEY_PERFORMANCE_INDICATOR_NUMBER);
				totalKeyPerformaceNumberObtained += keyPerformanceIndicatorNumber;
			}

			long totalKeyPerformanceNumber = noOfRecords * HrisKinds.KeyPerformanceRating.KeyPerformanceIndicator.MAX_NUMBER;

			double averageKeyPerformaceNumber = (double) totalKeyPerformaceNumberObtained / noOfRecords;
			DecimalFormat decimalFormat = new DecimalFormat("###.##");
			String averageKeyPerformanceNumberStr = decimalFormat.format(averageKeyPerformaceNumber);

			int round = (int) Math.round(averageKeyPerformaceNumber);
			String averageKeyPerformanceValue = HrisKinds.KeyPerformanceRating.KeyPerformanceIndicator.VALUES[round - 1];

			JSONObject keyPerformanceRatingReport = new JSONObject();
			keyPerformanceRatingReport.put("Total_Key_Performance", totalKeyPerformanceNumber);
			keyPerformanceRatingReport.put("Obtained_Key_Performance", totalKeyPerformaceNumberObtained);
			keyPerformanceRatingReport.put("Average_Key_Performance", averageKeyPerformanceNumberStr);
			keyPerformanceRatingReport.put("Average_Key_Performance_Value", averageKeyPerformanceValue);

			JSONArray array = new JSONArray();
			array.put(keyPerformanceRatingReport);

			records.put(new JSONObject().put("records", array));

			LogUtility.writeLog("Reports Array >> " + array);

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError(getClass().getName() + " >> Exception Trace >> " + trace);
		}

		LogUtility.writeLog(getClass().getName() + " >> onResult End");
	}
}
