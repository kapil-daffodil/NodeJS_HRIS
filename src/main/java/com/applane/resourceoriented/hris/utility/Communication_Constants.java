package com.applane.resourceoriented.hris.utility;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public interface Communication_Constants {

	String ORGANIZATION_NAME = "organization_name";

	String ORGANIZATION_LOGO = "organization_logo";

	String ORGANIZATION_SIGNATURE = "organization_signature";

	interface Type {

		String NOTIFICATION_CONFIG = "notification_config";

		String LEAVE_CONFIG = "leave_config";

		String TOUR_CONFIG = "tour_config";

		String EVENT_CONFIG = "event_config";

		String RESIGNATION_CONFIG = "resignation_config";

	}

	interface Properties {

		String DISABLE_SMS = "enable_sms";

		String DISABLE_MAIL = "enable_mail";

		String REPLY_TO_ID = "reply_to";

		String REPLY_TO_MOBILE = "reply_to_mobile";

		String CONCERNED_PERSONS = "concerned_persons";
	}

}
