package com.applane.vender_portal;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.app.shared.constants.View;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class VendorBusinessVolumeReportQueryJob implements QueryJob {

	@Override
	public void onQuery(Query arg0) throws DeadlineExceededException {

	}

	@Override
	public void onResult(Result result) throws DeadlineExceededException {
		try {
			JSONObject parameters = result.getQuery().getParameters();
			LogUtility.writeError("Parameters>>>" + parameters);
			Object invoiceDate = parameters.get("invoicedate");
			JSONArray records = result.getRecords();
			int length = records.length();
			for (int i = 0; i < length; i++) {
				JSONObject recordData = records.getJSONObject(i);
				recordData.put("invoicedateparam", invoiceDate);
				recordData.put("vendoridparam", recordData.get("vendorid"));
				recordData.put("vendornameparam", recordData.get("vendorid.orgname"));
			}
			JSONObject loadViewActions = new JSONObject();
			JSONObject view = new JSONObject();
			view.put(View.Action.TYPE, View.Action.Types.LOADVIEW);
			view.put(View.PRIMARY_EXPRESSION, "vendorname");
			view.put(View.Action.Types.LoadView.VIEWID, "vendorportal_invoice_list_submitted");
			view.put(View.Action.Types.LoadView.VIEW_LABEL, "Vendor Invoices");
			String filter = "vp_invoice_type!=NULL AND invoicedate={invoicedateparam} AND vendorid={vendoridparam}";
			if (!parameters.isNull("_PI_")) {
				filter += " AND vp_status!=26 AND vp_status!=30";
			}
			view.put(View.Action.Types.LoadView.FILTERS, filter);
			loadViewActions.put("vendorinvoice", view);
			result.addResultInfo("loadviews", loadViewActions);
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

}
