package com.applane.resourceoriented.hris.navigant;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.google.apphosting.api.DeadlineExceededException;

public class PortalDashboardCompletedYearsQueryJob implements QueryJob {

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
				Object joiningDateObject = data.get("joiningdate");
				Date joiningDate = Translator.dateValue(joiningDateObject);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
//				calendar.add(Calendar.MONTH, 1);
				calendar.set(Calendar.DAY_OF_MONTH, 1);
				// calendar.add(Calendar.DATE, -1);
				Date firstDayOfMonth = calendar.getTime();
				long months = DataTypeUtilities.monthsBetweenDates(joiningDate, firstDayOfMonth);
				int year = (int) (months / 12);
				String yearCompleted = year + " Years";
				data.put("yearscompleted", yearCompleted);
			}

		} catch (JSONException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
		}

	}

	public static void main(String[] args) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		// calendar.add(Calendar.MONTH,);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		// calendar.add(Calendar.DATE, -1);
		Date firstDayOfMonth = calendar.getTime();
		System.out.println(firstDayOfMonth);
	}

}
