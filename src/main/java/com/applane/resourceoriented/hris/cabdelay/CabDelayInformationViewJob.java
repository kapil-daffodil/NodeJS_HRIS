package com.applane.resourceoriented.hris.cabdelay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.app.jobs.ViewJob;
import com.applane.assistance.Translator;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HrisUserDefinedFunctions;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class CabDelayInformationViewJob implements ViewJob {

	@Override
	public void onBeforeExecuteQuery(JSONObject viewdata) throws VetoException, DeadlineExceededException {
		try {
			if (isCurrentUserCabAdmin()) {
				JSONObject metaData = viewdata.getJSONObject("metadata");
				JSONObject query = metaData.getJSONObject("query");
				JSONArray absoluteFilters = query.optJSONArray("absolute_filters");

				if (absoluteFilters != null && absoluteFilters.length() > 0) {
					JSONObject filterJSONObject = absoluteFilters.optJSONObject(0);
					filterJSONObject.put("filters", "");
					filterJSONObject.put("filters", "status_id IN(2)");
				}
			}
		} catch (JSONException e) {
			LogUtility.writeError(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			throw new BusinessLogicException("Something Unexpected at server, Please try After some time.(CabDelayInformationViewJob)");
		}
	}

	public boolean isCurrentUserCabAdmin() {
		try {
			String currentUserEmailId = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL));
			String cabAdminOfficialEmailId = HrisUserDefinedFunctions.getCabAdminOfficialEmailId();

			LogUtility.writeLog("Current User Email Id : " + currentUserEmailId);
			LogUtility.writeLog("Cab Admin Official Email Id : " + cabAdminOfficialEmailId);

			return (cabAdminOfficialEmailId == null) ? false : cabAdminOfficialEmailId.equals(currentUserEmailId);

		} catch (JSONException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(CabDelayInformationBusinessLogic.class.getName(), e));
		}
	}

	@Override
	public void onBeforeRender(JSONObject arg0) throws VetoException, DeadlineExceededException {

	}
}
