package com.applane.resourceoriented.hris.utility;

/**
 * @author Ajay Pal Singh
 * @version 1.1
 * @category HRIS businesslogic
 */

public interface HRISConstants {

	/**
	 * Reimbursement Claim Status
	 * */
	public static int REIMBURSEMENT_NEW = 1;

	public static int REIMBURSEMENT_APPROVED = 2;

	public static int REIMBURSEMENT_REJECTED = 3;

	public static int REIMBURSEMENT_PAID = 4;

	/**
	 * Resignation Status
	 * */

	public static int RESIGNATION_NEW = 1;

	public static int RESIGNATION_ACCEPTED = 2;

	public static int RESIGNATION_REJECTED = 3;

	public static int RESIGNATION_ONHOLD = 4;

	/**
	 * Tour Status
	 * */

	public static int TOUR_NEW = 1;

	public static int TOUR_APPROVED = 2;

	public static int TOUR_REJECTED = 3;

	public static int TOUR_CANCEL = 4;

	public static int TOUR_POSTPONED = 5;

	/**
	 * Appraisal Status
	 * */

	public static int APPRAISAL_NEW = 1;

	public static int APPRAISAL_INITIATED = 2;

	public static int APPRAISAL_COMPLETED = 3;

	/**
	 * Maritial Status
	 * */

	public static int MARITIAL_SINGLE = 1;

	public static int MARITIAL_MARRIED = 2;

	public static int MARITIAL_Divorcee = 3;

	/**
	 * Leave Status
	 * */

	public static int LEAVE_NEW = 1;

	public static int LEAVE_APPROVED = 2;

	public static int LEAVE_REJECTED = 3;

	public static int LEAVE_CANCEL = 4;

	/**
	 * Short Leave Status
	 * */

	public static int SHORT_LEAVE_NEW = 1;

	public static int SHORT_LEAVE_APPROVED = 2;

	public static int SHORT_LEAVE_REJECTED = 3;

	public static int SHORT_LEAVE_CANCEL = 4;

	/**
	 * Duration Type
	 * */

	public static int DURATION_FULLDAY = 1;

	public static int DURATION_FIRSTHALF = 2;

	public static int DURATION_SECONDHALF = 3;

	/**
	 * SMS Utility Constant
	 */
	public static String SMS_FROM_NAME = "HRIS";

	/**
	 * Resources using SMS code
	 */
	public static String LEAVE_REQUEST = "hris_leaverequests";

	public static String TOUR_REQUEST = "hris_tourrequests";

	public static String SHORT_LEAVE_REQUEST = "hris_shortleaverequests";

	/**
	 * Attendance Types
	 */

	public static int ATTENDANCE_PRESENT = 1;

	public static int ATTENDANCE_ABSENT = 2;

	public static int ATTENDANCE_LEAVE = 3;

	public static int ATTENDANCE_TOUR = 4;

	public static int ATTENDANCE_HOLIDAY = 5;

	public static int ATTENDANCE_OFF = 6;

	public static int ATTENDANCE_EXTRAWORKINGDAY = 7;

	public static int ATTENDANCE_EXTRAWORKINGDAY_HALF = 8;

	public static int ATTENDANCE_HALFDAY_LEAVE = 9;

	public static int ATTENDANCE_COMP_OFF = 10;

	public static int ATTENDANCE_HALF_COMP_OFF = 11;

	/**
	 * Employee Status
	 */

	public static int EMPLOYEE_ACTIVE = 1;

	public static int EMPLOYEE_INACTIVE = 2;

	interface ApproveAttendanceStatus {

		int STATUS_APPROVED = 2;

		int STATUS_REJECTED = 3;

		interface FormParameterNames {

			String STATUS = "status";

			String KEY = "key";

			String ORGANIZATION = "organization";

			String APPROVER_OFFICIAL_EMAIL_ID = "approver_official_email_id";
		}
	}
}
