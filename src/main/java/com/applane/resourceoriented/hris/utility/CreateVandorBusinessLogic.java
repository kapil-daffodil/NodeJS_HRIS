package com.applane.resourceoriented.hris.utility;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.afb.AFBConstants;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;

public class CreateVandorBusinessLogic {

	public void createVandorInvokeMethod(Object[] keys) {
		try {
			if (keys != null) {
				String key = "";
				for (int counter = 0; counter < keys.length; counter++) {
					// Object key = keys[counter];
					if (key.length() > 0 && (keys[counter] + "").length() > 0 && !(keys[counter] + "").equalsIgnoreCase("null")) {
						key += "," + keys[counter];
					} else if ((keys[counter] + "").length() > 0 && !(keys[counter] + "").equalsIgnoreCase("null")) {
						key += "" + keys[counter];
					}

				}
				JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(key);
				for (int counter = 0; counter < employeeArray.length(); counter++) {
					Object employeeCode = employeeArray.getJSONObject(counter).opt("employeecode");
					Object vendorName = employeeArray.getJSONObject(counter).opt("name");
					Object employeeId = employeeArray.getJSONObject(counter).opt("__key__");
					Object nameInBank = employeeArray.getJSONObject(counter).opt("name_in_bank");
					JSONArray vandorArray = getVandorArray(employeeCode);
					if (vandorArray == null || vandorArray.length() == 0) {
						EmployeeNameBusinessLogic.createVendor(vendorName, employeeCode, 2, employeeId, nameInBank);
					}
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("CreateVandorBusinessLogic >> createVandorInvokeMethod >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new BusinessLogicException("Some Problem Occured");
		}
	}

	private JSONArray getVandorArray(Object employeeCode) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, AFBConstants.ORGANIZATION_ORGANIZATIONANDCONTACTS);
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, AFBConstants.OrganizationAndContacts.CONTACT_NAME + " = '" + employeeCode + "'");
		JSONObject vandorObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray vandorArray = vandorObject.getJSONArray(AFBConstants.ORGANIZATION_ORGANIZATIONANDCONTACTS);
		return vandorArray;
	}

}
