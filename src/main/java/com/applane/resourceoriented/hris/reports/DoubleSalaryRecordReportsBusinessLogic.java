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

public class DoubleSalaryRecordReportsBusinessLogic implements QueryJob {

	public void onQuery(Query query) throws DeadlineExceededException {

	}

	public void onResult(Result result) throws DeadlineExceededException {
		JSONArray array = result.getRecords();
		try {
			ArrayList<String> employeeList = new ArrayList<String>();
			JSONArray exceptionRecords = new JSONArray();
			for (int counter = 0; counter < array.length(); counter++) {
				JSONObject exceptionDetails = new JSONObject();
				String employeeName = Translator.stringValue(array.getJSONObject(counter).get("employeeid.name"));
				String monthName = Translator.stringValue(array.getJSONObject(counter).get("monthid.name"));
				String yearName = Translator.stringValue(array.getJSONObject(counter).get("yearid.name"));
				String invoiceNumber = Translator.stringValue(array.getJSONObject(counter).get("invoicenumber"));
				if (employeeList.contains(invoiceNumber)) {
					exceptionDetails.put("employeeName", employeeName);
					exceptionDetails.put("monthName", monthName);
					exceptionDetails.put("yearName", yearName);
					exceptionDetails.put("invoiceNumber", invoiceNumber);
					exceptionRecords.put(exceptionDetails);
				} else {
					employeeList.add(invoiceNumber);
				}

			}
			
			JSONObject finalDetails = new JSONObject();
			finalDetails.put("exceptionRecords", exceptionRecords);
			array.put(finalDetails);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(DoubleSalaryRecordReportsBusinessLogic.class.getName(), e);
			LogUtility.writeLog("ExceptionReportsBusinessLogic Exception Trace >> " + trace);
			throw new RuntimeException(e);
		}
	}
}