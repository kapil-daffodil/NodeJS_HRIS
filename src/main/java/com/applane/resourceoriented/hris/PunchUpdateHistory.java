package com.applane.resourceoriented.hris;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class PunchUpdateHistory extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		readAttendanceParameters(req, resp);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void readAttendanceParameters(HttpServletRequest req, HttpServletResponse resp) {
		Map<String, Object> paramMap = req.getParameterMap();
		String params = "";
		try {
			Iterator it = paramMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				params = params + "Parameter1 is [" + pairs.getKey() + "] and value is [" + pairs.getValue() + "]\n";
			}

			if (paramMap == null || paramMap.size() == 0) {
				resp.getWriter().write("No parameter found in Attendance servlet..");
			} else {

				/*
				 * Get parameters from client program cardno, IOdate, IOTime,Gate No.,IO Status
				 */

				// String organizationId = ((String[]) paramMap.get("organizationid"))[0];

				String organizationName = ((String[]) paramMap.get("organizationname"))[0];
				String branch = ((String[]) paramMap.get("branch"))[0];

				/*
				 * Select __key__,name from hris_employees where deviceid='cardno',employeestatus='Active';
				 */

				Object userId = PunchingUtility.getId("up_users", "emailid", ApplicationConstants.OWNER);
				PunchingUtility.setValueInSession(organizationName, userId, ApplicationConstants.OWNER);

				JSONArray record = getLastPunchDateStatusForBranch(branch, organizationName);
				if (record != null && record.length() > 0) {
					Object date = record.getJSONObject(0).opt("date");
					JSONObject lastUpdateInfo = new JSONObject();
					lastUpdateInfo.put("date", date);
					resp.getWriter().write(lastUpdateInfo.toString());
				} else {
				}
			}
		} catch (Exception e) {
			String traces = ExceptionUtils.getExceptionTraceMessage("PunchingDataServlet", e);
			LogUtility.writeError("Some error ocuured while update employee punching data >> [" + traces + "] >> " + e);
			DataTypeUtilities.sendExceptionMail(traces);
			throw new BusinessLogicException("Some error ocuured while update employee punching data");
		}
	}

	public static JSONArray getLastPunchDateStatusForBranch(String branch, String organizationName) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "punchdatehistory");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("date");
		array.put("branchid");
		query.put(Data.Query.COLUMNS, array);
		if (!organizationName.equals("girnarsoft.com")) {
			query.put(Data.Query.FILTERS, "branchid.name = '" + branch + "'");
		}
		JSONObject orders = new JSONObject();
		orders.put(Data.Query.Orders.EXPERSSION, "date");
		orders.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		query.put(Data.Query.ORDERS, new JSONArray().put(orders));
		query.put(Data.Query.MAX_ROWS, 1);
		JSONObject object = ResourceEngine.query(query);
		JSONArray employeeArray = object.getJSONArray("punchdatehistory");
		return employeeArray;
	}
}
