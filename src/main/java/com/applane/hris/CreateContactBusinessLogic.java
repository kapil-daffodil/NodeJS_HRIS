package com.applane.hris;

import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */
 
public class CreateContactBusinessLogic implements OperationJob {
	Object CURRENT_APPLICATION_ID = (Object) CurrentState.getCurrentVariable(CurrentSession.CURRENT_APPLICATION_ID);
	ApplaneDatabaseEngine engine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		String operation = record.getOperationType();
		/* Insert Record into Organization_Contacts */
		if (operation.equalsIgnoreCase("insert")) {
			recordInsertIntoContacts(record);
		}
		/* Insertion Completed */

	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		// TODO Auto-generated method stub

	}

	private void recordInsertIntoContacts(Record record) {
		Object imageobject = record.getValue("image");
		Object nameObject = record.getValue("name");
		Object emailidObject = record.getValue("officialemailid");
		String name = "";
		String emailid = "";
		String image = "";
		JSONObject contactsQuery = new JSONObject();
		try {
			contactsQuery.put(Data.Query.RESOURCE, "organization_organizationandcontacts");
			JSONObject contactRow = new JSONObject();
			if (nameObject != null) {
				name = (String) nameObject;
				if (name.trim().length() > 0) {
					contactRow.put("contactname", name);
				}
			}
			if (imageobject != null) {
				image = (String) imageobject;
				if (image.trim().length() > 0) {
					contactRow.put("image", image);
				}
			}
			if (emailidObject != null) {
				emailid = (String) emailidObject;
				if (emailid.trim().length() > 0) {
					contactRow.put("emailid", emailid);
				}
			}
			JSONObject contactTypeID = new JSONObject();
			contactTypeID.put("name", "Contact");
			contactRow.put("contacttypeid", contactTypeID);
			contactRow.put("tags", CURRENT_APPLICATION_ID);
			contactsQuery.put(Data.Update.UPDATES, contactRow);
			engine.update(contactsQuery);

		} catch (JSONException e) {
			e.printStackTrace();
			throw new BusinessLogicException("Error while inserting record in organization_organizationandcontacts" + e.getMessage());
		}
	}

}
