package com.applane.resourceoriented.hris.navigant;

import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class PortalDashboardBirthdayQueryJob implements QueryJob {

	public void onQuery(Query query) throws DeadlineExceededException {
		// TODO Auto-generated method stub

	}

	public void onResult(Result result) throws DeadlineExceededException {
		try {
			JSONArray records = result.getRecords();
			int recordLength = records.length();
			for (int i = 0; i < recordLength; i++) {
				JSONObject data;
				data = records.getJSONObject(i);
				Object date = data.get("date");
				LogUtility.writeLog("date>>" + new SimpleDateFormat("dd-MMMM").format(date));
				data.put("date", new SimpleDateFormat("dd-MMMM").format(date));

			}
		} catch (JSONException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
		}
	}
}
