package com.applane.resourceoriented.hris.reports;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
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
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.google.apphosting.api.DeadlineExceededException;

public class RevenewLossBusinessLogic implements QueryJob {

	@Override
	public void onQuery(Query query) throws DeadlineExceededException {
		JSONObject parameters = query.getParameters();
		Object monthId = parameters.opt("monthid");
		Object yearId = parameters.opt("yearid");
		if (monthId == null || yearId == null) {
			throw new RuntimeException("Please Select Month Year.");
		}

		String yearName = EmployeeSalaryGenerationServlet.getYearName((long) Translator.integerValue(yearId));
		String fromDateString = yearName + "-" + monthId + "-1";
		Date fromDate = Translator.dateValue(fromDateString);
		Date toDate = DataTypeUtilities.getMonthLastDate(fromDate);
		String toDateString = EmployeeSalaryGenerationServlet.getDateInString(toDate);
		String filter = "date>='" + fromDateString + "' AND date<='" + toDateString + "'";
		query.addFilter(filter);

		JSONArray filters = query.getFilters();
		LogUtility.writeLog("parameters >> " + parameters + " << filters >> " + filters);

	}

	@Override
	public void onResult(Result result) throws DeadlineExceededException {
		try {
			JSONArray records = result.getRecords();
			JSONObject parameters = result.getQuery().getParameters();

			Object monthId = parameters.opt("monthid");
			Object yearId = parameters.opt("yearid");

			LogUtility.writeLog("records >> " + records);
			Map<Integer, Double> resourceRevenewDetails = new HashMap<Integer, Double>();
			Map<Integer, String[]> resourceNameMap = new HashMap<Integer, String[]>();
			List<Integer> list = new ArrayList<Integer>();
			if (records != null) {
				for (int counter = 0; counter < records.length(); counter++) {
					JSONObject engagementDetailsObject;
					engagementDetailsObject = records.getJSONObject(counter);
					int resourceId = Translator.integerValue(engagementDetailsObject.opt("resourceid"));
					String resourceName = Translator.stringValue(engagementDetailsObject.opt("resourceid.name"));
					String employeeCode = Translator.stringValue(engagementDetailsObject.opt("resourceid.employeecode"));
					double rate = Translator.doubleValue(engagementDetailsObject.opt("deliveryid.rate"));
					double billableEfforts = Translator.doubleValue(engagementDetailsObject.opt("billableefforts"));
					double amount = rate * billableEfforts;
					if (!resourceNameMap.containsKey(resourceId)) {
						resourceNameMap.put(resourceId, new String[] { resourceName, employeeCode });
					}
					if (resourceRevenewDetails.containsKey(resourceId)) {
						amount += resourceRevenewDetails.get(resourceId);
					}
					resourceRevenewDetails.put(resourceId, amount);
					if (!list.contains(resourceId)) {
						list.add(resourceId);
					}
				}
				String listString = list.toString();
				if (listString.length() > 0) {
					listString = listString.substring(1, listString.length() - 1);
					HashMap<Integer, Double> employeeSalaryPaidDetsils = getEmployeeSalaryPaidDetsils(listString, monthId, yearId);
					JSONArray array = new JSONArray();
					for (Integer resourceId : resourceNameMap.keySet()) {
						String[] employeeDetails = resourceNameMap.get(resourceId);
						double paidSalary = employeeSalaryPaidDetsils.get(resourceId) == null ? 0.0 : employeeSalaryPaidDetsils.get(resourceId);
						double revenew = resourceRevenewDetails.get(resourceId);
						JSONObject details = new JSONObject();
						details.put("resourceName", employeeDetails[0]);
						details.put("resourceCode", employeeDetails[1]);
						details.put("paidSalary", new DecimalFormat("#.##").format(paidSalary));
						details.put("revenew", new DecimalFormat("#.##").format(revenew));
						if (paidSalary < revenew) {
							details.put("profit", new DecimalFormat("#.##").format(revenew - paidSalary));
						} else {
							details.put("loss", new DecimalFormat("#.##").format(paidSalary - revenew));
						}
						array.put(details);
					}
					records.put(new JSONObject().put("revenewReport", array));
				}

			}
		} catch (JSONException e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("RevenewLossBusinessLogic >>  Exception >> Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured Please Contact To Admin.");
		}

	}

	private HashMap<Integer, Double> getEmployeeSalaryPaidDetsils(String listString, Object monthId, Object yearId) throws JSONException {
		JSONArray columnArray = new JSONArray();
		columnArray.put("employeeid");
		columnArray.put("payableamount");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid IN(" + listString + ") AND monthid=" + monthId + " AND yearid=" + yearId);
		JSONArray employeeList = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("salarysheet");
		HashMap<Integer, Double> employeeMap = new HashMap<Integer, Double>();
		for (int counter = 0; counter < employeeList.length(); counter++) {
			int employeeId = employeeList.getJSONObject(counter).optInt("employeeid");
			double paidSalary = employeeList.getJSONObject(counter).optDouble("payableamount");
			if (employeeMap.containsKey(employeeId)) {
				double amount = employeeMap.get(employeeId);
				paidSalary += amount;
			}
			employeeMap.put(employeeId, paidSalary);
		}
		return employeeMap;
	}

	public static void main(String[] args) {
		List<Integer> list = new ArrayList<Integer>();
		list.add(1);
		list.add(2);
		System.out.println("list >> " + list.toString());
	}
}
