package com.applane.afb;

import com.applane.user.shared.constants.UserPermission;

public interface AFBConstants {

	String CURRENCY_FLUCTUATION = "Currency Fluctuation";
	String PROVISION_FOR_EXPENSES = "Provision For Expenses";
	String ASSET_SALES_LOSS_INCOME = "Asset Sales Loss/Income";
	String EmployeeAdvance = "EmployeeAdvance";
	String Employee_Advance = "Employee Advance";
	String INDIRECT_EXPENSES = "Indirect Expenses";
	String PROVISION = "Provision";
	String SERVICE_CHARGE_ON_EXPORT = "Service Charge on Export";
	String SERVICE_CHARGE_ON_DOMESTIC = "Service Charge on Domestic";
	String EMPLOYEE_TDS = "Employee TDS";

	String REVENUE = "Revenue";

	String SUPPLEMENTARY_TRANSECTIONS = "supplementarytransections";

	public interface AfbUserService {
		String PROFIT_CENTER = "profitcenter";

		String ACTIVATE_ACCOUNT = "1";

		Object DEACTIVATE_ACCOUNT = "2";
	}

	public interface SupplementaryTransections {
		String PAYMENT_ID = "paymentid";

		String RECEIPT_ID = "receiptid";

		String VENDOR_INVOICE_ID = "vendorinvoiceid";

		String INVOICE_ID = "invoiceid";

		String SUPPLEMENTARY_TRANSECTION_TYPE_ID = "supplementarytransectiontypeid";

		String AMOUNT = "amount";

		String PERCENTAGE = "percentage";

		String CONVERTED_AMOUNT = "converted_amount";

		String PROFITCENTER_ID = "profitcenter_id";

		String INVOICEID = "invoice_id";

		String ORDERID = "orderid";

		String SUPPLEMENTARY_VENDOR_INVOICE_ID = "supplementary_vendorinvoiceid";

		String SUPPLEMENTARY_ASSET_ID = "supplementary_assetid";

		String SUPPLEMENTARY_EXPENSE_ID = "supplementary_expenseid";

		String VOUCHER_AMOUNT = "voucher_amount";

		String TAX_PAYMENT_DATE = "tax_payment_date";
		String ADD_TO_ASSET = "addtoasset";
		String FUND_TRANSFER_ID = "fundtransfer_id";
		String POD_INVOICE_ID = "podinvoiceid";
		String POD_PAYMENT_ID = "podpaymentid";
	}

	String SUPPLEMENTARY_TRANSECTION_TYPE = "supplementarytransectiontype";

	public interface SupplementaryTransectionType {
		String NAME = "name";

		String MAIN_TRANSECTION_TYPE_ID = "maintransectiontypeid";

		String ACCOUNT_ID = "accountid";

		String PERCENTAGE = "percentage";

		String ISSERVICETAX = "isservicetax";
		String ISDEDUCTION = "isdeduction";
		String REPORT_COLUMN = "reportcolumn";

		String ACCOUNT_TYPE = "accounttype";
		String ASSET_ACCOUNT_ID = "asset_accountid";
		String INTEREST_ACCOUNT_ID = "interest_accountid";
	}

	String BUSINESS_FUNCTIONS = "businessfunctions";

	interface BusinessFunctions {
		String NAME = "name";

		String PARENT_BUSINESS_FUNCTION = "parentbusinessfunctionid";

		String APPGROUP_ID = "appgroupid";

		interface InterInstanceColumns {
			String KEY = "appgroupid_key";

			String LABEL = "appgroupid_label";
		}
	}

	String TAX_PAYMENT = "procurement_tax_payments";

	interface TaxPayment {

		String PAYMENT_DATE = "payment_date";

		String MONTH = "month";

		String YEAR = "year";

		String TAX_TYPEID = "tax_typeid";

		String TOTAL_TAX = "total_tax";

		String BALANCE = "balance";

		String TOTAL_PAYMENT = "total_payment";

		String ADJUSTED_CENVAT_TAX = "adjusted_cenvat_tax";

		String PF_CHALLAN_NO = "pf_challan_no";

		String PF_CHALLAN_DATE = "pf_challan_date";

		String ST_CHALLAN_NO = "st_challan_no";

		String ST_CHALLAN_DATE = "st_challan_date";

		String REMITANCE_DATE = "remitance_date";

		String REMITANCE_NO = "remitance_no";

		String VOUCHER_ID = "voucher_id";

		String TAX_PAYMENT_NO = "tax_payment_no";
		String INTEREST_PAID = "interest_paid";
		String EXTRA_AMOUNT_PAID = "extraamount_paid";
		String AMOUNT_TO_ADJUST = "amount_to_adjust";
		String SECTION = "section";

	}

	String TAX_PAYMENT_LINEITEMS = "procurement_tax_payment_lineitems";

	interface TaxPaymentLineItems {

		String SUPPLEMENTARY_TRANSACTION_ID = "supplementary_transactions_id";

		String SUPPLEMENTARY_TRANSACTION_TYPE_ID = "supplimentary_transactionid";

		String TAX_PAYMENT_ID = "tax_paymentid";

		String NAME = "name";

		String BUSINESS_PARTNER = "business_partner";

		String PROFIT_CENTER = "profit_center";

		String LOCATION = "location";

		String DATE = "date";

		String VENDOR_INVOICE = "invoice_no";

		String CUSTOMER_INVOICE = "invoiceno";

		String RECEIPT_NO = "reciept_no";

		String PAYMENT_NO = "payment_no";

		String AMOUNT = "amount";

		String TAX_AMOUNT = "tax_amount";

		String VOUCHER_LINEITEMID = "voucherlineitemid";

		String ACCOUNTID = "accountid";

		String NARATION = "naration";

	}

	String PAYMENT_LINEITEMS = "paymentlineitems";

	public interface Paymentlineitems {
		String RECEIPT_ID = "receipt_id";

		String PAYMENT_ID = "payment_id";

		String RECEIPT_CENTER = "receiptcenter";

		String AMOUNT = "amount";

		String CONVERTED_AMOUNT = "converted_amount";

		String CHEQUE_DATE = "chequedate";

		String CHEQUE_NO = "chequeno";

		String VALUE_DATE = "valuedate";

		String RECEIPT_CENTER_ACCOUNT_ID = "receiptcenter.accountid";

		String TAX_PAYMENT_ID = "tax_paymentid";
		String FD_RECEIPT_ID = "fdreceiptid";
		String TRANSACTION_DATE = "transaction_date";

		String BULKREIMBURSEMENTID = "bulkreimbursementid";
		String ADVANCE_RECEIPT_ID = "advancereceiptid";
		String PAYMENT_MODE = "paymentmode";
		String TEMP_VOUCHER_ID = "tempvoucherid";
		String TEMP_NEWCHEQUE_NO = "tempnewchequeno";
		String TEMP_NEWCHEQUE_DATE = "tempnewchequedate";
		String POD_PAYMENT_ID = "pod_paymentid";

		public interface PaymentModeTypes {
			String CHEQUE = "Cheque";
			String ECS = "Ecs";
			String ACCOUNT_TRANSFER = "Account Transfer";
		}
	}

	String BUSINESS_FUNCTION_APPLICATIONS = "businessfunctionapplications";

	interface BusinessFunctionApplications {

		String BF_FUNCTION_ID = "bfunctionid";

		String APPGROUP_ID = "appgroupid";

		interface DisplayColumn {
			String ID = "id";
		}
	}

	String ORG_PROFIT_CENTER = UserPermission.Kind.ORGANIZATION_PROFIT_CENTER;

	public interface OrganizationProfitCenter {
		String NAME = UserPermission.Kind.OrganizationProfitCenter.NAME;

		String ORGANIZATION_LEVEL = UserPermission.Kind.OrganizationProfitCenter.ORGANIZATION_LEVEL;

		String BUCODE = UserPermission.Kind.OrganizationProfitCenter.BUCODE;
	}

	String ORGANIZATION_OFFICES = UserPermission.Kind.ORGANIZATION_OFFICES;

	public interface OrganizationOffices {
		String NAME = UserPermission.Kind.OrganizationOffices.NAME;
		String ACCOUNTID = UserPermission.Kind.OrganizationOffices.ACCOUNTID;
	}

	String DEFAULT_APPLICATIONS = "defaultapplications";

	interface DefaultApplications {
		String APPGROUP_ID = "appgroupid";

		interface InterInstanceColumns {
			String KEY = "appgroupid_key";

			String LABEL = "appgroupid_label";
		}

	}

	String ORGANIZATION_ORGANIZATIONANDCONTACTS = UserPermission.Kind.ORGANIZATION_ORGANIZATIONANDCONTACTS;

	public interface OrganizationAndContacts {
		String ORGNAME = UserPermission.Kind.OrganizationAndContacts.ORGNAME;

		String ORGANIZATION_CATEGORIES = UserPermission.Kind.OrganizationAndContacts.ORGANIZATION_CATEGORIES;

		String ACCOUNT_ID = UserPermission.Kind.OrganizationAndContacts.ACCOUNT_ID;

		String IS_FOREIGN = UserPermission.Kind.OrganizationAndContacts.IS_FOREIGN;

		String CONTACT_TYPE_ID = UserPermission.Kind.OrganizationAndContacts.CONTACT_TYPE_ID;

		String CONTACT_NAME = UserPermission.Kind.OrganizationAndContacts.CONTACT_NAME;

		String ADDRESS_COUNTRY = UserPermission.Kind.OrganizationAndContacts.ADDRESS_COUNTRY;

		String IS_SERVICETAX_APPLICABLE = UserPermission.Kind.OrganizationAndContacts.IS_SERVICETAX_APPLICABLE;

		String EMPLOYEE_ID = "employeeid";
		String ORG_CONTACT_EMAIL_ID = "orgcontactemailid";

	}

	String ORGCONTACTS_TAXESINFO = "orgcontacts_taxesinfo";

	public interface OrgContactsTaxesInfo {
		String ORGCONTACTS_ID = "orgcontacts_id";
		String SUPPLEMENTARY_TRANSECTION_TYPE_ID = "supplementarytransectiontypeid";
		String PERCENTAGE = "percentage";
	}

	String CONTACT_PERSONS = "contact_persons";

	interface ContactPersons {
		String CONTACT_NAME = "contactname";

		String JOB_TITLE = "jobtitle";

		String EMAIL_ID = "orgcontactemailid";

		String PHONE = "phone_no";

		String MOBILE = "mob_no";

		String FAX_NO = "fax_no";

		String SKYPE_ID = "skypeid";

		String ORGNAME = "orgname";

		String DEPARTMENT = "department";
	}

	String OTC_WIP = "otc_wip";

	public interface Otc_WIP {
		String PROFIT_CENTER_ID_KEY = "profit_center_id_key";
		String BUSINESS_PARTNER_ID_KEY = "business_partner_id_key";
		String DELIVERY_NO_KEY = "delivery_no_key";
		String CREATION_DATE = "creation_date";
		String PROFIT_CENTER_ID = "profit_center_id";
		String BUSINESS_PARTNER_ID = "business_partner_id";
		String DELIVERY_NO = "delivery_no";
		String AMOUNT = "amount";
		String MONTH = "month";
		String MANAGE_WIP_ID = "manage_wip_id";
	}

	String MANAGE_WIP = "manage_wip";

	public interface ManageWIP {
		String YEAR = "year";
		String MONTH = "month";
		String STATUS = "status";

		public interface StatusTypes {
			String FRESH = "Fresh";
			String FREEZE = "Freeze";
		}
	}

	String CURRENCY_DENOMINATION_VALUES = "currency_denomination_values";

	public interface CurrencyDenominationValues {
		String CURRENCY = "currency";
		String DENOMINATION_VALUE = "denomination_value";
	}

	String RECEIPTCENTER_CHEQUEDETAILS = "receiptcenter_chequedetails";

	public interface ReceiptCenterChequeDetails {
		String RECEIPT_CENTER_ID = "receiptcenterid";
		String ACCOUNT_ID = "accountid";
		String FIRST_CHEQUE_NO = "firstchequeno";
		String LAST_CHEQUE_NO = "lastchequeno";
	}

	public interface NumberSeriesColumns {
		String COLUMN_NAME = "columnname";
		String COLUMN_LABEL = "columnlabel";
	}

	String AFB_NOTIFICATIONS = "afb_notifications";

	public interface AFBNotifications {
		String NAME = "name";

		public interface NotiFicationType {
			Integer OVERDUE_DELIVERIES = 1;// Overdue Deliveries
		}
	}

	String AFB_NOTIFICATIONS_SETTING = "afb_notificationssetting";

	public interface AFBNotificationsSetting {
		String AFB_NOTIFICATION_ID = "afb_notificationid";
		String WORKING_HRS = "workinghrs";
		String WORKING_DAYS = "workingdays";
	}

	String PAYMENT_REQUEST_HISTORY = "payment_request_history";

	public interface PaymentRequestHistory {
		String INVOICE_ID = "invoiceid";
		String REQ_DATE = "reqdate";
		String REQUESTED_AMOUNT = "requestedamount";
		String PAYMENT_LINK = "paymentlink";
		String REQUEST_CODE = "requestcode";
		String ORGANIZATION_ID = "organizationid";
		String INVOICE_CODE = "invoicecode";

	}

	String PAYMENT_AUTHORIZED_HISTORY = "payment_authorized_history";

	public interface PaymentAuthorizedHistory {
		String INVOICE_ID = "invoiceid";
		String PAYMENT_DATE = "paymentdate";
		String AMOUNT_PAID = "amountpaid";
		String TRANSACTION_ID = "transactionid";
		String RESPONSE_CODE = "responsecode";
		String RESPONSE_MESSAGE = "responsemessage";
		String PAYMENT_ID = "paymentid";
		String MERCHANT_REF_NO = "merchantrefno";
		String DESCRIPTION = "description";
		String ISFLAGGED = "isflagged";
		String PAYMENT_METHOD = "paymentmethod";
	}

	String AFB_UNSAVED_TRANSACTIONS = "afb_unsavedtransactions";

	public interface AFBUnsavedTransactions {
		String RESPONSE_DATA = "responsedata";
		String CAUSE="cause";
	}
}
