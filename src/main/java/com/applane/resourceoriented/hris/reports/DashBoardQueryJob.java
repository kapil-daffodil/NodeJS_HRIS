package com.applane.resourceoriented.hris.reports;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.app.shared.constants.View;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class DashBoardQueryJob implements QueryJob {

	@Override
	public void onQuery(Query arg0) throws DeadlineExceededException {

	}

	@Override
	public void onResult(Result result) throws DeadlineExceededException {
		try {
			JSONObject parameters = result.getQuery().getParameters();
			LogUtility.writeError("Parameters>>>" + parameters);
			JSONObject loadViewActions = new JSONObject();
			JSONObject view = new JSONObject();
			view.put(View.Action.TYPE, View.Action.Types.LOADVIEW);
			view.put(View.PRIMARY_EXPRESSION, "name");
			view.put(View.Action.Types.LoadView.VIEWID, "employeetableview");
			if (!parameters.isNull("_name_") && parameters.getString("_name_").equalsIgnoreCase("CMJ")) {
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "New Joinings");
				view.put(View.Action.Types.LoadView.FILTERS, "MONTH(joiningdate)={_CurrentMonth} AND YEAR(joiningdate)={_CurrentYear} AND departmentid={depidparam}");
				view.put(View.Action.Types.LoadView.OPTION_FILTERS, "MONTH(joiningdate)={_CurrentMonth} AND YEAR(joiningdate)={_CurrentYear} AND profitcenterid={profitcenteridparam}");
			} else if (!parameters.isNull("_name_") && parameters.getString("_name_").equalsIgnoreCase("CMR")) {
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "New Joinings");
				view.put(View.Action.Types.LoadView.FILTERS, "MONTH(relievingdate)={_CurrentMonth} AND YEAR(relievingdate)={_CurrentYear} AND departmentid={depidparam}");
				view.put(View.Action.Types.LoadView.OPTION_FILTERS, "MONTH(relievingdate)={_CurrentMonth} AND YEAR(relievingdate)={_CurrentYear} AND profitcenterid={profitcenteridparam}");
			} else if (!parameters.isNull("_name_") && parameters.getString("_name_").equalsIgnoreCase("LMS")) {
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "New Joinings");
				view.put(View.Action.Types.LoadView.FILTERS, "employeestatusid=1 AND MONTH(joiningdate)<{_CurrentMonth} AND YEAR(joiningdate)<={_CurrentYear} AND departmentid={depidparam}");
				JSONArray optionFilter = new JSONArray();
				optionFilter.put("employeestatusid=2 AND MONTH(relievingdate)>={com.applane.resourceoriented.hris.reports.HRISExcelReportsHandler.getPreviousMonth()} AND YEAR(relievingdate)>={_CurrentYear} AND departmentid={depidparam}");
				optionFilter.put("employeestatusid=1 AND MONTH(joiningdate)<{_CurrentMonth} AND YEAR(joiningdate)<={_CurrentYear} AND profitcenterid={profitcenteridparam}");
				optionFilter.put("employeestatusid=2 AND MONTH(relievingdate)>={com.applane.resourceoriented.hris.reports.HRISExcelReportsHandler.getPreviousMonth()} AND YEAR(relievingdate)>={_CurrentYear} AND profitcenterid={profitcenteridparam}");
				view.put(View.Action.Types.LoadView.OPTION_FILTERS, optionFilter);
			}
			loadViewActions.put("newjoin", view);
			result.addResultInfo("loadviews", loadViewActions);
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}
}