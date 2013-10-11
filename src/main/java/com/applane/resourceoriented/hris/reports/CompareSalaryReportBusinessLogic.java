package com.applane.resourceoriented.hris.reports;

import java.text.DecimalFormat;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class CompareSalaryReportBusinessLogic implements QueryJob {

	public void onQuery(Query query) throws DeadlineExceededException {

	}

	public void onResult(Result result) throws DeadlineExceededException {
		JSONArray array = result.getRecords();
		Query query = result.getQuery();
		int compareMonthId = Translator.integerValue(query.getParameter("cmid"));
		int compareYearId = Translator.integerValue(query.getParameter("cyid"));
		try {
			JSONArray exceptionRecords = new JSONArray();
			HashMap<Integer, Object[]> compareWithMap = new HashMap<Integer, Object[]>();
			HashMap<Integer, Object[]> compareForMap = new HashMap<Integer, Object[]>();
			for (int counter = 0; counter < array.length(); counter++) {
				int employeeId = Translator.integerValue(array.getJSONObject(counter).get("employeeid"));
				int monthId = Translator.integerValue(array.getJSONObject(counter).get("monthid"));
				int yearId = Translator.integerValue(array.getJSONObject(counter).get("yearid"));
				String employeeName = Translator.stringValue(array.getJSONObject(counter).get("employeeid.name"));
				String monthName = Translator.stringValue(array.getJSONObject(counter).get("monthid.name"));
				String yearName = Translator.stringValue(array.getJSONObject(counter).get("yearid.name"));
				double amount = Translator.doubleValue(array.getJSONObject(counter).get("payableamount"));

				Object[] details = new Object[4];

				if (monthId == compareMonthId && yearId == compareYearId) {
					if (compareWithMap.containsKey(employeeId)) {
						details = compareWithMap.get(employeeId);
					}
				} else {
					if (compareForMap.containsKey(employeeId)) {
						details = compareForMap.get(employeeId);
					}
				}
				details[0] = employeeName;
				details[1] = monthName;
				details[2] = yearName;
				details[3] = Translator.doubleValue(details[3]) + amount;

				if (monthId == compareMonthId && yearId == compareYearId) {
					compareWithMap.put(employeeId, details);
				} else {
					compareForMap.put(employeeId, details);
				}

			}
			for (Integer emplpoyeeId : compareForMap.keySet()) {
				Object[] compareForDetails = compareForMap.get(emplpoyeeId);
				Object[] comparewithDetails = compareWithMap.get(emplpoyeeId);
				if (comparewithDetails != null && compareForDetails != null) {
					JSONObject exceptionDetails = new JSONObject();
					double compareForAmount = Translator.doubleValue(compareForDetails[3]);
					double compareWithAmount = Translator.doubleValue(comparewithDetails[3]);
					double difference = compareForAmount - compareWithAmount;
					if (compareForAmount < compareWithAmount) {
						compareForAmount = compareWithAmount;
					}
					if (compareForAmount > 0.0) {
						double percentage = compareForAmount == 0.0 ? 0.0 : (float) ((difference * 100) / compareForAmount);
						exceptionDetails.put("employeeName", compareForDetails[0]);
						exceptionDetails.put("monthName", compareForDetails[1]);
						exceptionDetails.put("yearName", compareForDetails[2]);
						exceptionDetails.put("percentageDifference", new DecimalFormat("0.##").format(percentage));
						exceptionRecords.put(exceptionDetails);
					}
				}

			}
			JSONObject finalDetails = new JSONObject();
			finalDetails.put("exceptionRecords", exceptionRecords);
			array.put(finalDetails);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(CompareSalaryReportBusinessLogic.class.getName(), e);
			LogUtility.writeLog("ExceptionReportsBusinessLogic Exception Trace >> " + trace);
			throw new RuntimeException(e);
		}
	}
}