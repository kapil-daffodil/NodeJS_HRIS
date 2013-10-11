package com.applane.resourceoriented.hris.reports;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.format.Alignment;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;

public class CtcReportInExcel extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			resp.setContentType("application/vnd.ms-excel");
			String decompositionParameter = "attachment;filename=PF_3a_Form.xls";
			resp.setHeader("Content-disposition", decompositionParameter);
			resp.setHeader("Content-Type", "application/octet-stream");

			WritableFont headerfont = new WritableFont(WritableFont.ARIAL, 22, WritableFont.BOLD);
			WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
			headerFormat.setAlignment(Alignment.CENTRE);

			String key = req.getParameter("keys");

			if (key.length() > 0) {
				key = key.substring(1, key.length() - 1);
				JSONArray pfReportArray = getPFReportArray(key);
				if (pfReportArray != null && pfReportArray.length() > 0) {
					Object yearId = pfReportArray.getJSONObject(0).opt("yearid");
					Object monthId = pfReportArray.getJSONObject(0).opt("monthid");
					Object yearName = pfReportArray.getJSONObject(0).opt("yearid.name");
					Object monthName = pfReportArray.getJSONObject(0).opt("monthid.name");
					Object branchId = pfReportArray.getJSONObject(0).opt("branchid");
					JSONArray ctcHistoryArray = getCtcHistoryArray(branchId);
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			LogUtility.writeLog("PFAnnualReturnReportForm3ARevised >> calculateAndWriteToSheet Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "PFAnnualReturnReportForm3ARevised >> calculateAndWriteToSheet Exception Trace");
		}
	}

	private JSONArray getCtcHistoryArray(Object branchId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_ctc_history");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("ctc");
		columnArray.put("month_first_date");
		columnArray.put("applicable_last_date");
		columnArray.put("is_increment");
		query.put(Data.Query.COLUMNS, columnArray);
		if (branchId != null) {
			query.put(Data.Query.FILTERS, "employeeid.branchid = " + branchId);
		}
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray pfReportArray = employeeObject.getJSONArray("hris_employees_ctc_history");
		return pfReportArray;
	}

	private JSONArray getPFReportArray(String key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "pfreportforyear");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("monthid");
		columnArray.put("monthid.name");
		columnArray.put("yearid");
		columnArray.put("yearid.name");
		columnArray.put("branchid");
		columnArray.put("branchid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray pfReportArray = employeeObject.getJSONArray("pfreportforyear");
		return pfReportArray;
	}
}
