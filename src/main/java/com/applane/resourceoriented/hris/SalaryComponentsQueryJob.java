package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;

import org.json.JSONArray;

import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.moduleimpl.SystemParameters;
import com.google.apphosting.api.DeadlineExceededException;

public class SalaryComponentsQueryJob implements QueryJob {

	public void onQuery(Query query) throws DeadlineExceededException {
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		String filter = "applicablefrom <= '" + TODAY_DATE + "' AND applicableto>='" + TODAY_DATE + "'";
		query.addFilter(filter);
	}

	public void onResult(Result result) throws DeadlineExceededException {
		// TODO Auto-generated method stub

	}

}
