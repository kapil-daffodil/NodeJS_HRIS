package com.applane.resourceoriented.hris.reports;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class DoubleMonthlyAttendanceReportBusinessLogic implements QueryJob {

	public void onQuery(Query query) throws DeadlineExceededException {

	}

	public void onResult(Result result) throws DeadlineExceededException {
		JSONArray array = result.getRecords();
		try {
			ArrayList<Integer> employeeList = new ArrayList<Integer>();
			JSONArray exceptionRecords = new JSONArray();
			for (int counter = 0; counter < array.length(); counter++) {
				JSONObject exceptionDetails = new JSONObject();
				Integer employeeId = Translator.integerValue(array.getJSONObject(counter).opt("employeeid"));
				String employeeName = Translator.stringValue(array.getJSONObject(counter).opt("employeeid.name"));
				String monthName = Translator.stringValue(array.getJSONObject(counter).opt("monthid.name"));
				String yearName = Translator.stringValue(array.getJSONObject(counter).opt("yearid.name"));
				if (employeeList.contains(employeeId)) {
					exceptionDetails.put("employeeName", employeeName);
					exceptionDetails.put("monthName", monthName);
					exceptionDetails.put("yearName", yearName);
					exceptionRecords.put(exceptionDetails);
				} else {
					employeeList.add(employeeId);
				}

			}
			JSONObject finalDetails = new JSONObject();
			finalDetails.put("exceptionRecords", exceptionRecords);
			array.put(finalDetails);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(DoubleMonthlyAttendanceReportBusinessLogic.class.getName(), e);
			LogUtility.writeLog("ExceptionReportsBusinessLogic Exception Trace >> " + trace);
			throw new RuntimeException(e);
		}
	}
}