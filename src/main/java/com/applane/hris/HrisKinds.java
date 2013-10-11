package com.applane.hris;

public interface HrisKinds {

	int NAVIGANT_ORGANIZATION_ID = 7783;

	int HR_PORTAL_APPLICATION_ID = 1032;

	String EMPLOYEES = "hris_employees";

	String MAIL_CONFIGURATIONS = "hris_mailconfigurations";

	String GREETING_CONFIGURATION = "hris_greeting_configuration";

	String HR_ASSIGNING = "hris_hrassigning";

	String BRANCHES = "hris_branches";

	String EMPLOYEE_ATTENDANCE = "employeeattendance";

	String CAB_DELAY_INFORMATION = "hris_cab_delay_information";

	String CAB_DELAY_INFORMATION_TL = "hris_cab_delay_information_tl";

	String CAB_DELAY_REMARKS = "hris_cab_delay_remarks";

	String HRIS_EMPLOYEE_PROFILE_CHANGE_REQUEST = "hris_employee_profile_change_request";

	String EMPLOYEES_ADDITIONAL_INFORMATION = "hris_employees_additional_information";

	String ROSTER_REQUESTS = "hris_roster_requests";

	String SHIFT_REQUESTS_OFF_DAYS = "hris_shift_requests_off_days";

	String SHIFT_CHANGE_BY_TL_OFF_DAYS = "hris_shift_change_by_tl_off_days";

	String SHIFT_CHANGE_BY_TL = "hris_shift_change_by_tl";

	String PF_REPORT_FOR_YEAR = "pfreportforyear";

	String EMPLOYEE_MONTHLY_ATTENDANCE = "employeemonthlyattendance";

	String PROFIT_CENTER = "organization_profitcenter";

	String KEY_PERFORMANCE_INDICATOR_NUMBER = "key_performance_indicator_number";

	String KEY_PERFORMANCE_RATING = "hris_key_performance_rating";

	interface Employess {

		String OFFICIAL_EMAIL_ID = "officialemailid";

		String FIRST_NAME = "firstname";

		String LAST_NAME = "lastname";

		String BUSINESS_FUNCTION_ID = "businessfunctionid";

		String PROFIT_CENTER_ID = "profitcenterid";

		String PROFIT_CENTER_NAME = "profitcenterid.name";

		String PROFIT_CENTER_ID_ALIAS = "hris_employees_profit_centre";

		String HIGH_PRIVILEGE = "highprivilege";

		String REPORTING_TO = "reportingtoid";

		String CREATE_LOGIN_ACCOUNT = "createloginaccount";

		String EMPLOYEE_STATUS = "employeestatusid";

		String WHITELISTIED_IP_ADDRESS = "whitelistiedipaddress";

		String NAME = "name";

		String EMPLOYEE_CODE = "employeecode";

		String JOINING_DATE = "joiningdate";

		String RELIEVING_DATE = "relievingdate";

		String DESIGNATION_NAME = "designationid.name";

		String DEPARTMENT_NAME = "departmentid.name";

		String REPORTING_TO_NAME = "reportingtoid.name";

		String REPORTING_TO_EMPLOYEE_CODE = "reportingtoid.employeecode";

		String BRANCH_ID = "branchid";

		String ANNIVERSARY_DATE = "anniversarydate";

		String DATE_OF_BIRTH = "dateofbirth";
	}

	String EMPLOYEE_PROFIT_CENTRE = "hris_employees__profitcenterid";

	interface EmployeesProfitCentre {

		String EMPLOYEE = "hris_employees";

		String PROFIT_CENTER_ID = "profitcenterid";

		String ORGANIZATION_LEVEL_SCOPE = "organizationlevelscop";
	}

	String BUSINESS_FUNCTIONS = "businessfunctions";

	interface BusinessFunction {

		String NAME = "name";

		String PARENT_BUSINESSFUNCTION = "parentbusinessfunctionid";
	}

	interface MailConfigurations {

		String EMPLOYEE_GREETING_NOTIFICATION_SEND_TO = "emp_greeting_notification_send_to";

		String DISABLE_GREETING_NOTIFICATION_MAIL = "disable_greeting_notification_mail";

		String EMPLOYEE_PERIOD_COMPLETION_NOTIFICATION_SEND_TO = "emp_period_completion_notification_send_to";

		String DISABLE_PERIOD_COMPLETION_NOTIFICATION_MAIL = "disable_period_completion_notification_mail";

		String CAB_ADMIN_EMPLOYEE_ID = "cab_admin_employee_id";

		String CAB_ADMIN_EMPLOYEE_NAME = "cab_admin_employee_id.name";

		String CAB_ADMIN_EMPLOYEE_OFFICIAL_EMAIL_ID = "cab_admin_employee_id.officialemailid";

		String CMD_ID = "cmd_id";

		String CMD_OFFICIAL_EMAIL_ID = "cmd_id.officialemailid";
	}

	interface GreetingConfiguration {

		String GREETING_TYPE = "greeting_type";

		interface GreetingTypes {

			String BIRTHDAY = "Birthday";

			String ANNIVERSARY = "Anniversary";

			String PERIOD_COMPLETION = "Period Completion";
		}

		String IMAGE_URL = "image_url";
	}

	interface HrAssigning {

		String BRANCH_ID = "branchid";

		String EMPLOYEE_ID = "employeeid";

		String OFFICIAL_EMAIL_ID = "employeeid.officialemailid";
	}

	interface Branches {

		String KEY = "__key__";

		String HR_EMAIL_IDS = "hr_email_ids";
	}

	interface EmployeeAttendance {

		String ATTENDANCE_TYPE_ID = "attendancetypeid";

		String ATTENDANCE_TYPE_SHORT_LEAVE_ID = "attendance_type_short_leave_id";

		String ATTENDANCE_STATUS = "attendance_status";

		String LEAVE_TYPE_ID = "leavetypeid";

		String PAID_STATUS = "paidstatus";

		String EMPLOYEE_ID = "employeeid";

		String ATTENDANCE_DATE = "attendancedate";

		String TEMP = "temp";
	}

	interface CabDelayInformation {

		String EMPLOYEE_ID = "employee_id";

		String REPORTINGTO_ID = "reportingto_id";

		String REASON = "reason";

		String DATE = "date";

		String FROM_TIME_MERIDIEM = "from_time_meridiem";

		String FROM_TIME_TIME = "from_time_time";

		String TO_TIME_MERIDIEM = "to_time_meridiem";

		String TO_TIME_TIME = "to_time_time";

		String STATUS_ID = "status_id";

		String ATTENDANCE_TYPE_ID = "attendancetypeid";

		String LEAVE_TYPE_ID = "leavetypeid";

		String PAID_STATUS = "paidstatus";

		String APPROVE_BY_ADMIN_STATUS_ID = "approve_by_admin_status_id";

		String CAB_ADMIN_EMPLOYEE_ID = "cab_admin_employee_id";

		String CAB_DELAY_REMARKS_ID = "cab_delay_remarks_id";

		String CAB_DELAY_REMARKS = "cab_delay_remarks_id.remarks";
	}

	interface CabDelayInformationTL {

		String EMPLOYEE_ID = "employee_id";

		String FROM_TIME_MERIDIEM = "from_time_meridiem";

		String FROM_TIME_TIME = "from_time_time";

		String REPORTINGTO_ID = "reportingto_id";

		String TO_TIME_MERIDIEM = "to_time_meridiem";

		String TO_TIME_TIME = "to_time_time";

		String REASON = "reason";

		String DATE = "date";

		String STATUS_ID = "status_id";

		String ATTENDANCE_TYPE_ID = "attendancetypeid";

		String LEAVE_TYPE_ID = "leavetypeid";

		String PAID_STATUS = "paidstatus";

		String APPROVE_BY_ADMIN_STATUS_ID = "approve_by_admin_status_id";

		String CAB_ADMIN_EMPLOYEE_ID = "cab_admin_employee_id";

		String CAB_DELAY_REMARKS_ID = "cab_delay_remarks_id";
	}

	interface CabDelayRemarks {

		String REMARKS = "remarks";
	}

	interface EmployeeProfileChangeRequest {

		String EMPLOYEE_ID = "employee_id";

		String EMPLOYEE_NAME = "employee_id.name";

		String EMPLOYEE_CODE = "employee_id.employeecode";

		String PROFILE_CHANGE_REQUEST_MESSAGE = "profile_change_request_message";

		String HR_EMPLOYEE_ID = "hr_employee_id";

		String HR_EMPLOYEE_OFFICIAL_EMAIL_ID = "hr_employee_id.officialemailid";

		String REQUEST_DATE = "request_date";
	}

	interface EmployeesAdditionalInformation {

		String EMPLOYEE_ID = "employeeid";

		String EMPLOYEE_OFFICIAL_EMAIL_ID = "employeeid.officialemailid";

		String CAB_STATUS = "cab_status";

		String EMPLOYEE_NAME = "employeeid.name";

		String EMPLOYEE_CODE = "employeeid.employeecode";

		String EMPLOYEE_BRANCH_ID = "employeeid.branchid";

		String SALARY_ON_HOLD = "employeeid.salary_on_hold";

		String EMPLOYEE_STATUS_ID = "employeeid.employeestatusid";

		String EMPLOYEE_STATUS = "employeeid.employeestatusid.name";

		String RELIEVING_DATE = "employeeid.relievingdate";

		String RELIEVING_REMARKS = "relievingremarks";

		String IS_REHIRABLE = "isrehireable";

		interface CabStatus {

			String SELF = "SELF";

			String CAB = "CAB";
		}
	}

	interface RosterRequests {

		String EMPLOYEE_ID = "employee_id";

		String EMPLOYEE_NAME = "employee_id.name";

		String EMPLOYEE_CODE = "employee_id.employeecode";

		String REQUEST_TYPE = "request_type";

		String REQUEST_DATE = "request_date";

		String DATE = "date";

		String STATUS_ID = "status_id";

		String APPROVER_ID = "approver_id";

		String APPROVER_NAME = "approver_id.name";

		String APPROVER_EMPLOYEE_CODE = "approver_id.employeecode";

		String APPROVER_OFFICIAL_EMAIL_ID = "approver_id.officialemailid";

		String REASON = "reason";

		String APPROVE_DATE = "approve_date";

		String APPROVER_REMARKS = "approver_remarks";

		interface RequestType {

			String ROSTER_OFF = "Roster Off";

			String WORK_FROM_HOME = "Work From Home";
		}
	}

	interface ShiftRequestsOffDays {

		String SHIFT_REQUESTS_ID = "shift_requests_id";

		String OFF_DAY = "off_day";
	}

	interface ShiftChangeByTL {

		String EMPLOYEE_ID = "employee_id";

		String SHIFT_ID = "shift_id";

		String FROM_DATE = "from_date";

		String TO_DATE = "to_date";

		String CHANGE_BY_ID = "change_by_id";
	}

	interface ShiftChangeByTLOffDays {

		String SHIFT_CHANGE_BY_TL_ID = "shift_change_by_tl_id";

		String OFF_DAY = "off_day";
	}

	interface PFReportForYear {

		String MONTH_ID = "monthid";

		String BRANCH_ID = "branchid";

		String YEAR_ID = "yearid";

		String FROM_DATE = "fromdate";

		String TO_DATE = "todate";

		String DEPARTMENT_ID = "departmentid";

		String REPORT_TYPE_NUMBER = "report_type_number";
	}

	interface EmployeeMonthlyAttendance {

		String EMPLOYEE_ID = "employeeid";

		String EMPLOYEE_CODE = "employeeid.employeecode";

		String EMPLOYEE_NAME = "employeeid.name";

		String MONTH_ID = "monthid";

		String YEAR_ID = "yearid";

		String BRANCH_ID = "employeeid.branchid";

		String BRANCH_NAME = "employeeid.branchid.name";

		String DEPARTMENT_ID = "employeeid.departmentid";

		String DEPARTMENT_NAME = "employeeid.departmentid.name";

		String PROFIT_CENTER_ID = "employeeid.profitcenterid";

		String PROFIT_CENTER_NAME = "employeeid.profitcenterid.name";

		String DAYS_IN_MONTH = "days_in_month";

		String LEAVES = "leaves";

		String ABSENTS = "absents";

		String NON_PAYABLE_ABSENTS = "nonpaidabsents";

		String NON_PAYABLE_LEAVES = "nonpayableleaves";

		String PRESENT_DAYS = "presentdays";

		String PAID_ABSENTS = "paidabsents";

		String PAYABLE_LEAVES = "payableleaves";

		String ACTUAL_NON_WORKING_DAYS = "actualnonworkingdays";

		String CARRY_EXTRA_WORKING_DAY = "carryextraworkingday";
	}

	interface ProfitCenter {

		String NAME = "name";
	}

	interface KeyPerformanceIndicatorNumber {

		String KEY_PERFORMANCE_NUMBER = "key_performance_number";
	}

	interface KeyPerformanceRating {

		String EMPLOYEE_ID = "employee_id";

		String BUSINESS_FUNCTION_ID = "business_function_id";

		String BUSINESS_FUNCTION_NAME = "business_function_id.name";

		String KEY_PERFORMANCE_INDICATOR_ID = "key_performance_indicator_id";

		String KEY_PERFORMANCE_INDICATOR_NAME = "key_performance_indicator_id.name";

		String KEY_PERFORMANCE_INDICATOR_VALUE = "key_performance_indicator_value";

		String KEY_PERFORMANCE_INDICATOR_NUMBER_ID = "key_performance_indicator_number_id";

		String KEY_PERFORMANCE_INDICATOR_NUMBER = "key_performance_indicator_number_id.key_performance_number";

		String REQUEST_DATE = "request_date";

		String REMARKS_DATE = "remarks_date";

		String REMARKS = "remarks";

		interface KeyPerformanceIndicator {

			int MAX_NUMBER = 5;

			String[] VALUES = { "Poor", "Satisfactory", "Above Average", "Good", "Extremely Well" };
		}
	}
}
