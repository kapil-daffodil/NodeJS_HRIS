package com.applane.resourceoriented.hris.navigant;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.google.apphosting.api.DeadlineExceededException;

public class PortalDashboardQueryJob implements QueryJob {

	public void onQuery(Query query) throws DeadlineExceededException {
		// TODO Auto-generated method stub

	}

	public void onResult(Result result) throws DeadlineExceededException {
		JSONArray records = result.getRecords();
		int recordLength = records.length();
		for (int i = 0; i < recordLength; i++) {
			JSONObject data;
			try {
				data = records.getJSONObject(i);
				Object employeeId = data.get(Data.Query.KEY);
				Object joiningDateObject = data.get("joiningdate");
				Date joiningDate = Translator.dateValue(joiningDateObject);
				Date currentDate = SystemParameters.getCurrentDate();
				long months = DataTypeUtilities.monthsBetweenDates(joiningDate, currentDate);
				int year = (int) (months / 12);
				int month = (int) (months % 12);
				String yearCompleted = year + " Years and " + month + " Months";

				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.add(Calendar.MONTH, -3);
				String threeMonthsBefore = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
				Date threeMonthsBeforDate = Translator.dateValue(threeMonthsBefore);
				String currentTime = new SimpleDateFormat("yyyy-MM-dd").format(currentDate);
				currentDate = Translator.dateValue(currentTime);

				JSONArray attendanceDetails = getAttendance(threeMonthsBeforDate, currentDate, employeeId);
				data.put("yearcompleted", yearCompleted);
				data.put("leavedetails", attendanceDetails);
			} catch (JSONException e) {
				throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
			}
		}

	}

	private JSONArray getAttendance(Date threeMonthsBeforDate, Date currentDate, Object employeeId) throws JSONException {
		JSONArray columns = new JSONArray();
		columns.put("attendancedate");
		columns.put("attendancetypeid.name");

		String filter = "employeeid " + "={employeeid} and attendancedate " + " >={threemonthsbefore} and attendancedate " + "<={currentdate} and attendancetypeid IN (3,9,12)";

		JSONObject parameters = new JSONObject();
		parameters.put("employeeid", employeeId);
		parameters.put("threemonthsbefore", threeMonthsBeforDate);
		parameters.put("currentdate", currentDate);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, filter);
		query.put(Data.Query.PARAMETERS, parameters);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeeattendance");
	}

	public static void main(String[] args) {
	}
}
