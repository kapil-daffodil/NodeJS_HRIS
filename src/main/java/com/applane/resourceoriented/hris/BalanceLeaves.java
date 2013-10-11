package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.moduleimpl.SystemParameters;
import com.applane.ui.browser.shared.constants.CurrentSession;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class BalanceLeaves {

	// job define on "hris_leaverequest" view new leave request column
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public static JSONArray employeeBalanceLeave(Object employeeid, Object requestdate) {

		try {

			// GET EMPLOYEEID FROM KEY
			if ((employeeid instanceof JSONObject)) {
				employeeid = ((JSONObject) employeeid).get(Data.Query.KEY);
			}

			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Date todayDate = Translator.dateValue(TODAY_DATE);
			Calendar cal = Calendar.getInstance();
			cal.setTime(todayDate);
			Object monthId = cal.get(Calendar.MONTH) + 1;
			Object yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
			JSONArray leaveRegister = getLeaveRegister(employeeid, monthId, yearId);
			JSONArray returnBalanceLeaveArray = new JSONArray();
			int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			for (int counter = 0; counter < leaveRegister.length(); counter++) {
				Object leaveTypeId = leaveRegister.getJSONObject(counter).opt("leavetypeid");
				Object leaveTypeName = leaveRegister.getJSONObject(counter).opt("leavetypeid.name");
				if (organizationId == 10719) {
					leaveTypeName = leaveRegister.getJSONObject(counter).opt("leavetypeid.name_girnarsoft");
				}
				Object leaveBalance = leaveRegister.getJSONObject(counter).opt("leavebalance");
				JSONObject object = new JSONObject();
				JSONObject objectTemp = new JSONObject();
				objectTemp.put("__key__", leaveTypeId);
				if (organizationId == 10719) {
					objectTemp.put("name_girnarsoft", leaveTypeName);
				} else {
					objectTemp.put("name", leaveTypeName);
				}
				object.put("leavetypeid", objectTemp);
				object.put("balanceleaves", String.valueOf(leaveBalance));
				LogUtility.writeLog("KD >> object >> " + object);
				returnBalanceLeaveArray.put(object);
			}
			LogUtility.writeLog("KD >> returnBalanceLeaveArray >> " + returnBalanceLeaveArray);
			return returnBalanceLeaveArray;

		} catch (Exception e) {
			TakeAndMarkAttendanceServlet.sendMailInCaseOfException(e, "KD >> Balance Leave Return Failed");
			throw new BusinessLogicException("You are not added as an employee. Kindly consult with your HR.");
		}
	}

	private static JSONArray getLeaveRegister(Object employeeId, Object monthId, Object yearId) throws JSONException {
		LogUtility.writeLog("KD >> getLeaveRegister method called()");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "leaveregister");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("leavebalance");
		columnArray.put("leavetypeid");
		columnArray.put("leavetypeid.name");
		columnArray.put("leavetypeid.name_girnarsoft");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray branchArray = object.getJSONArray("leaveregister");
		LogUtility.writeLog("KD >> branchArray >> " + branchArray);
		return branchArray;

	}
}
