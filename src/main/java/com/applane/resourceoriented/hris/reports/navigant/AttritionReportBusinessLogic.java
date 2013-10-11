package com.applane.resourceoriented.hris.reports.navigant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.google.apphosting.api.DeadlineExceededException;

public class AttritionReportBusinessLogic implements QueryJob {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public void onQuery(Query arg0) throws DeadlineExceededException {
	}

	@Override
	public void onResult(Result result) throws DeadlineExceededException {
		try {
			JSONArray records = result.getRecords();

			Query query = result.getQuery();

			String fromDateStr = (String) query.getParameter("from");
			String toDateStr = (String) query.getParameter("to");

			LogUtility.writeLog("From Filter Str : " + fromDateStr);
			LogUtility.writeLog("To Filter Str : " + toDateStr);

			Date fromFilter = getDate(fromDateStr);
			Date toFilter = getDate(toDateStr);

			// LogUtility.writeLog("From Filter Date : [" + fromFilter + "]");
			// LogUtility.writeLog("To Filter Date : [" + toFilter + "]");

			int noOfRecords = (records == null) ? 0 : records.length();

			Map<String, AttritionDetail> profitCenterWiseAttritionMapping = new HashMap<String, AttritionDetail>();
			Map<String, Map<String, AttritionDetail>> departmentAndReportingWiseAttritionMapping = new HashMap<String, Map<String, AttritionDetail>>();
			Map<Integer, Integer> relievedEmployeesWorkingYears = new HashMap<Integer, Integer>();

			for (int i = 0; i < noOfRecords; i++) {

				JSONObject jsonObject = records.optJSONObject(i);

				String joiningDateStr = jsonObject.optString(HrisKinds.Employess.JOINING_DATE);
				Date joiningDate = getDate(joiningDateStr);

				String relievingDateStr = jsonObject.optString(HrisKinds.Employess.RELIEVING_DATE);
				Date relievingDate = getDate(relievingDateStr);

				String departmentName = jsonObject.optString(HrisKinds.Employess.DEPARTMENT_NAME);
				String reportingToName = jsonObject.optString(HrisKinds.Employess.REPORTING_TO_NAME);
				String reportingToEmployeeCode = jsonObject.optString(HrisKinds.Employess.REPORTING_TO_EMPLOYEE_CODE);

				LogUtility.writeLog("Joining Date : " + joiningDate + ", Relieving Date : " + relievingDate + ", Department Name : " + departmentName + ", Reporting To Name : " + reportingToName + ", Reporting To Employee Code : " + reportingToEmployeeCode);

				JSONArray profitCenterJSONArray = jsonObject.optJSONArray(HrisKinds.Employess.PROFIT_CENTER_ID);
				int noOfProfitCenter = (profitCenterJSONArray == null) ? 0 : profitCenterJSONArray.length();

				for (int j = 0; j < noOfProfitCenter; j++) {

					JSONObject profitCenterJSONObject = profitCenterJSONArray.optJSONObject(j);

					if (profitCenterJSONObject != null) {

						String profitCenterName = profitCenterJSONObject.optString("name", null);

						AttritionDetail profitCenterWiseAttritionDetail = profitCenterWiseAttritionMapping.get(profitCenterName);
						if (profitCenterWiseAttritionDetail == null) {
							profitCenterWiseAttritionDetail = new AttritionDetail();
							profitCenterWiseAttritionMapping.put(profitCenterName, profitCenterWiseAttritionDetail);
						}

						if (joiningDate != null && !joiningDate.before(fromFilter) && !joiningDate.after(toFilter)) {
							profitCenterWiseAttritionDetail.increaseNoOfJoining();
						}

						if (relievingDate != null && !relievingDate.before(fromFilter) && !relievingDate.after(toFilter)) {
							profitCenterWiseAttritionDetail.increaseNoOfRelieving();
						}
					}
				}

				Map<String, AttritionDetail> reportingWiseIterationMapping = departmentAndReportingWiseAttritionMapping.get(departmentName);
				if (reportingWiseIterationMapping == null) {
					reportingWiseIterationMapping = new HashMap<String, AttritionDetail>();
					departmentAndReportingWiseAttritionMapping.put(departmentName, reportingWiseIterationMapping);
				}

				String reportingTo = reportingToName + " " + reportingToEmployeeCode;
				if (reportingToName == null || reportingToName.length() <= 0) {
					reportingTo = "Other";
				}

				AttritionDetail reportingWiseIterationDetail = reportingWiseIterationMapping.get(reportingTo);
				if (reportingWiseIterationDetail == null) {
					reportingWiseIterationDetail = new AttritionDetail();
					reportingWiseIterationMapping.put(reportingTo, reportingWiseIterationDetail);
				}

				// LogUtility.writeLog("Reporting To : " + reportingTo + ", Attrition Mapping : " + departmentAndReportingWiseAttritionMapping);

				if (joiningDate != null && !joiningDate.before(fromFilter) && !joiningDate.after(toFilter)) {
					reportingWiseIterationDetail.increaseNoOfJoining();
				}

				if (relievingDate != null && !relievingDate.before(fromFilter) && !relievingDate.after(toFilter)) {
					reportingWiseIterationDetail.increaseNoOfRelieving();
				}

				if (joiningDate != null && relievingDate != null && !relievingDate.before(fromFilter) && !relievingDate.after(toFilter) && relievingDate.after(joiningDate)) {

					long joiningTimeInMillis = joiningDate.getTime();
					long relievingTimeInMillis = relievingDate.getTime();

					long diff = relievingTimeInMillis - joiningTimeInMillis;
					long noOfDays = diff / (1000 * 60 * 60 * 24);

					long remainingDays = noOfDays % 365;
					int noOfYears = (int) noOfDays / 365;

					// LogUtility.writeLog("Remaining Days : " + remainingDays + ", No Of Years : " + noOfYears);

					if (remainingDays > 0) {
						noOfYears++;
					}

					Integer noOfRelievedEmployees = relievedEmployeesWorkingYears.get(noOfYears);
					if (noOfRelievedEmployees == null) {
						noOfRelievedEmployees = 0;
					}
					relievedEmployeesWorkingYears.put(noOfYears, noOfRelievedEmployees + 1);
				}
			}

			JSONArray profitCenterReport = new JSONArray();

			profitCenterWiseAttritionMapping = getSortedAttritionDetailMap(profitCenterWiseAttritionMapping);

			for (Iterator<String> iterator = profitCenterWiseAttritionMapping.keySet().iterator(); iterator.hasNext();) {

				String profitCenter = iterator.next();

				AttritionDetail iterationDetail = profitCenterWiseAttritionMapping.get(profitCenter);
				int noOfJoining = iterationDetail.getNoOfJoining();
				int noOfRelieving = iterationDetail.getNoOfRelieving();

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("ProfitCenter", profitCenter);
				jsonObject.put("NoOfJoining", noOfJoining + "");
				jsonObject.put("NoOfRelieving", noOfRelieving + "");

				profitCenterReport.put(jsonObject);
			}

			JSONArray departmentAndReportingWiseReport = new JSONArray();

			departmentAndReportingWiseAttritionMapping = getSortedDepartmentAndReportingWiseAttritionMapping(departmentAndReportingWiseAttritionMapping);

			for (Iterator<String> iterator = departmentAndReportingWiseAttritionMapping.keySet().iterator(); iterator.hasNext();) {

				String departmentName = iterator.next();

				JSONObject departmentNameJsonObject = new JSONObject();
				departmentNameJsonObject.put("DepartmentName", departmentName);

				departmentAndReportingWiseReport.put(departmentNameJsonObject);

				Map<String, AttritionDetail> reportingWiseAttritionMapping = departmentAndReportingWiseAttritionMapping.get(departmentName);

				for (Iterator<String> reportingWiseIterator = reportingWiseAttritionMapping.keySet().iterator(); reportingWiseIterator.hasNext();) {

					String reportingTo = reportingWiseIterator.next();

					AttritionDetail iterationDetail = reportingWiseAttritionMapping.get(reportingTo);

					int noOfJoining = iterationDetail.getNoOfJoining();
					int noOfRelieving = iterationDetail.getNoOfRelieving();

					JSONObject jsonObject = new JSONObject();
					jsonObject.put("ReportingTo", reportingTo);
					jsonObject.put("NoOfJoining", noOfJoining + "");
					jsonObject.put("NoOfRelieving", noOfRelieving + "");

					departmentAndReportingWiseReport.put(jsonObject);
				}
			}

			JSONArray relievedEmployeesReport = new JSONArray();

			for (Iterator<Integer> iterator = relievedEmployeesWorkingYears.keySet().iterator(); iterator.hasNext();) {

				Integer workingYears = iterator.next();
				Integer noOfEmployees = relievedEmployeesWorkingYears.get(workingYears);

				JSONObject jsonObject = new JSONObject();

				jsonObject.put("WorkingYearsSlab", workingYears - 1 + " - " + workingYears);
				jsonObject.put("NoOfEmployees", noOfEmployees);

				relievedEmployeesReport.put(jsonObject);
			}

			JSONObject reportsJsonObject = new JSONObject();
			reportsJsonObject.put("profitCenterReport", profitCenterReport);
			reportsJsonObject.put("reportingWiseReport", departmentAndReportingWiseReport);
			reportsJsonObject.put("relievedEmployeesReport", relievedEmployeesReport);

			JSONArray array = new JSONArray();
			array.put(reportsJsonObject);
			records.put(new JSONObject().put("records", array));

			LogUtility.writeError("Reports Array >> " + array);

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError("IterationReportBusinessLogic >> Exception Trace >> " + trace);
		}
	}

	private Map<String, AttritionDetail> getSortedAttritionDetailMap(Map<String, AttritionDetail> profitCenterWiseAttritionMapping) {

		Set<Entry<String, AttritionDetail>> profitCenterWiseAttritionEntrySet = profitCenterWiseAttritionMapping.entrySet();
		List<Entry<String, AttritionDetail>> profitCenterWiseAttritionEntryList = new ArrayList<Entry<String, AttritionDetail>>(profitCenterWiseAttritionEntrySet);

		Collections.sort(profitCenterWiseAttritionEntryList, new Comparator<Entry<String, AttritionDetail>>() {

			@Override
			public int compare(Entry<String, AttritionDetail> o1, Entry<String, AttritionDetail> o2) {

				String key1 = o1.getKey();
				String key2 = o2.getKey();

				return key1.compareTo(key2);
			}
		});

		Map<String, AttritionDetail> sortedProfitCenterWiseAttritionMapping = new LinkedHashMap<String, AttritionDetail>();

		for (Entry<String, AttritionDetail> entry : profitCenterWiseAttritionEntryList) {

			String key = entry.getKey();
			AttritionDetail value = entry.getValue();

			sortedProfitCenterWiseAttritionMapping.put(key, value);
		}

		return sortedProfitCenterWiseAttritionMapping;
	}

	private Map<String, Map<String, AttritionDetail>> getSortedDepartmentAndReportingWiseAttritionMapping(Map<String, Map<String, AttritionDetail>> departmentAndReportingWiseAttritionMapping) {

		Set<Entry<String, Map<String, AttritionDetail>>> departmentAndReportingWiseAttritionEntrySet = departmentAndReportingWiseAttritionMapping.entrySet();
		List<Entry<String, Map<String, AttritionDetail>>> departmentAndReportingWiseAttritionEntryList = new ArrayList<Entry<String, Map<String, AttritionDetail>>>(departmentAndReportingWiseAttritionEntrySet);

		Collections.sort(departmentAndReportingWiseAttritionEntryList, new Comparator<Entry<String, Map<String, AttritionDetail>>>() {

			@Override
			public int compare(Entry<String, Map<String, AttritionDetail>> o1, Entry<String, Map<String, AttritionDetail>> o2) {

				String key1 = o1.getKey();
				String key2 = o2.getKey();

				return key1.compareTo(key2);
			}
		});

		Map<String, Map<String, AttritionDetail>> sortedDepartmentAndReportingWiseAttritionMapping = new LinkedHashMap<String, Map<String, AttritionDetail>>();

		for (Entry<String, Map<String, AttritionDetail>> entry : departmentAndReportingWiseAttritionEntryList) {

			String key = entry.getKey();

			Map<String, AttritionDetail> value = entry.getValue();
			value = getSortedAttritionDetailMap(value);

			sortedDepartmentAndReportingWiseAttritionMapping.put(key, value);
		}

		return sortedDepartmentAndReportingWiseAttritionMapping;
	}

	private Date getDate(String dateStr) throws ParseException {

		if (dateStr == null || dateStr.length() < 1) {
			return null;
		}

		Date date = dateFormat.parse(dateStr);

		return date;
	}

	private class AttritionDetail {

		private int noOfJoining = 0;

		private int noOfRelieving = 0;

		public int getNoOfJoining() {
			return noOfJoining;
		}

		public int getNoOfRelieving() {
			return noOfRelieving;
		}

		public void increaseNoOfJoining() {
			noOfJoining++;
		}

		public void increaseNoOfRelieving() {
			noOfRelieving++;
		}

		@Override
		public String toString() {
			return "No of Joining : " + noOfJoining + ", No of Relieving : " + noOfRelieving;
		}
	}
}
