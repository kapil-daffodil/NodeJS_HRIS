/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 7/19/13
 * Time: 11:30 AM
 * To change this template use File | Settings | File Templates.
 */

exports.Baas = new function () {
    this.BAASDB = "baas";
    this.DEFAULT_GLOBALDB = "baasglobal";
    this.DEFAULT_ORGSHAREDDB = "baasshared";
    this.ASK = "baas";

    this.MODULES = "modules__baas";
    this.Modules = new function () {
        this.ID = "id";
        this.MODULE_PATH = "modulepath";
        this.INDEX = "index";
        this.LOGIC = "logic";
        this.Logic = new function () {
            this.OUT = "out";
            this.CODE = "code";
            this.TYPE = "type";
            this.Type = new function () {
                this.UPDATE = "update";
                this.TRANSFORM = "transform";
                this.QUERY = "query";
                this.COMMANDQUERY = "command query";
                this.CONFIGURE = "configure";
            }
        }
    }
    this.TABLES = "tables__baas";
    this.Tables = new function () {
        this.ID = "id";
        this.ORGENABLED = "orgenabled";
        this.DB = "db";
        this.DBSTATUS = "dbstatus";

        this.INDEXES = "indexes";
        this.Indexes = new function () {
            this.NAME = "name";
            this.FIELDS = "fields";
        }

        this.COLUMNS = "columns";
        this.SYSTEM_COLUMNS = "systemcolumns";
        this.Columns = new function () {
            this.EXPRESSION = "expression";
            this.ALIAS = "alias";
            this.TYPE = "type";
            this.TOTAL_AGGREGATES = "totalaggregates";
            this.TotalAggregates = new function () {
                this.SUM = "sum";
                this.AVG = "avg";
                this.COUNT = "count";
            }
            this.Type = new function () {
                this.STRING = "string";
                this.NUMBER = "number";
                this.DECIMAL = "decimal";
                this.LOOKUP = "lookup";
                this.BOOLEAN = "boolean";
                this.OBJECT = "object";
                this.DATE = "date";
                this.DATETIME = "datetime";
                //udt
                this.FILE = "file";
                this.IMAGE = "image";
                this.TEXT = "text";
                this.RICHTEXT = "richtext";
                this.EMAILID = "emailid";
                this.PHONE_NO = "phoneno";
                this.DURATION = "duration";
                this.Duration = new function () {
                    this.TIME = "time";
                    this.TIME_UNIT = "timeunit";
                    this.TimeUnits = new function () {
                        this.HRS = "Hrs";
                        this.MINUTES = "Minutes";
                        this.DAYS = "Days";
                    }
                    this.CONVERTED_VALUE = "convertedvalue"
                }
                this.CURRENCY = "currency";
                this.Currency = new function () {
                    this.AMOUNT = "amount";
                    this.TYPE = "type";
                    this.CONVERTED_VALUE = "convertedvalue";
                    this.BASE_CURRENCY = "basecurrency";
                    this.CONVERSION_RATE = "conversionrate";
                }
                this.INDEX = "index";
                this.Index = new function () {
                    this.INDEX = "index";
                    this.SUB_INDEX = "subindex";
                }
                this.SCHEDULE = "schedule";
                this.Schedule = new function () {
                    this.SPAN = "span";
                    this.Span = new function () {
                        this.NONE = "None";
                        this.HOURLY = "Hourly";
                        this.DAILY = "Daily";
                        this.WEEKLY = "Weekly";
                        this.MONTHLY = "Monthly";
                        this.YEARLY = "Yearly";
                    }
                    this.DUE_DATE = "duedate";
                    this.TIME = "time";
                    this.REPEAT = "repeat";
                    this.REPEATED_ON = "repeatedon";
                    this.FREQUENCY = "frequency";
                };
            };
            this.TABLE = "table";
            this.MULTIPLE = "multiple";
            this.REPLICATE = "replicate";
            this.OPTIONS = "options";
            this.PRIMARY = "primary";
            this.PRIMARY_COLUMNS = "primarycolumns";
            this.REPLICATE_COLUMNS = "replicatecolumns";
            this.REVERSE_RELATION_COLUMN = "reverserelationcolumn";
            this.MANDATORY = "mandatory";
            this.AUTOSAVE = "autosave";
            this.FLEXIBLE = "flexible";

        };
        this.FLEXIBLE = "flexible";
        this.JOBS = "jobs";
        this.Jobs = new function () {
            this.ID = "id";
            this.MODULE = "module";
            this.CODE = "code";
            this.OUT = "out";
            this.APPLICATION = "application";
            this.TYPE = "type";
            this.Type = new function () {
                this.UPDATE = "update";
                this.TRANSFORM = "transform";
                this.QUERY = "query";
                this.COMMANDQUERY = "command query";
            }
            this.OPERATION = "operation";
            this.Operation = new function () {
                this.INSERT = "insert";
                this.UPDATE = "update";
                this.DELETE = "delete";
                this.QUERY = "query";
            };
            this.WHEN = "when";
            this.When = new function () {
                this.BEFORE = "before";
                this.AFTER = "after";
            };
        };
        this.ENABLE_MODULES = "enablemodules";
        this.APPLICATIONS = "applications";
        this.ALIAS = "alias";
        this.MAINAPPLICATION = "mainapplication";
        this.PUBLIC = "public";
    };

    this.USERGROUPS = "usergroups__baas";
    this.UserGroups = new function () {
        this.ID = "id";
    };

    this.APPLICATIONS = "applications__baas";
    this.Applications = new function () {
        this.ID = "id";
        this.LABEL = "label";
        this.ASK = "ask";
        this.ORGENABLED = "orgenabled";
        this.DEVELOPERS = "developers";
        this.Developers = new function () {
            this.ROLE = "role";
        };
        this.USERGROUPS = "usergroups";
        this.DB = "db";
        this.TABLES = "tables";
        this.CUSTOMCODEURL = "customcodeurl";
        this.WELCOMEMAILTEMPLATE_SUBJECT = "welcomemailtemplatesubject";
        this.WELCOMEMAILTEMPLATE = "welcomemailtemplate";
        this.FORGOT_PASSWORD_MAILTEMPLATE_SUBJECT = "forgotpasswordmailtemplatesubject";
        this.FORGOT_PASSWORD_MAILTEMPLATE = "forgotpasswordmailtemplate";
        this.RESET_PASSWORD_MAILTEMPLATE_SUBJECT = "resetpasswordmailtemplatesubject";
        this.RESET_PASSWORD_MAILTEMPLATE = "resetpasswordmailtemplate";
        this.ENABLE_LOGS = "enablelogs";
        this.ORGANIZATIONS__BAAS__APPLICATIONS = "organizations__baas__applications";
        this.BASE_CURRENCY = "basecurrency";
        this.ALLOWED_CURRENCIES = "allowedcurrencies";
        this.MAIL_CREDENTIALS = "mailcredentials";
        this.MailCredentials = new function () {
            this.NAME = "name";
            this.EMAILID = "emailid";
            this.PASSWORD = "password";
            this.SERVICE = "service";
            this.DEFAULT = "default";
        }
    };

    this.CURRENCIES = "currencies__baas";
    this.Currencies = new function () {
        this.CURRENCY = "currency";
    }

    this.CURRENCY_CONVERSION_RATES = "currencyconversionrates__baas";
    this.CurrencyConversionRates = new function () {
        this.FROM = "from";
        this.TO = "to";
        this.RATE = "rate";
    }

    this.ORGANIZATIONS = "organizations__baas";
    this.Organizations = new function () {
        this.ID = "id";
        this.LABEL = "label";
        this.OSK = "osk";
        this.ADMINS = "admins";
        this.APPLICATIONS = "applications";
        this.DB = "db";
        this.DBSTATUS = "dbstatus";
        this.DBStatus = new function () {
            this.DEDICATED = "dedicated";
            this.SHARED = "shared";
        };
        this.DEACTIVATED = "deactivated";
        this.BASE_CURRENCY = "basecurrency";
        this.ALLOWED_CURRENCIES = "allowedcurrencies";
        this.GLOBAL_ORGANIZATION = {osk:"52301c08dda05ef40700001f", id:"global", label:"Global", "_id":"52301c09dda05ef407000020", "global":true};
        this.MAIL_CREDENTIALS = "mailcredentials";
        this.MailCredentials = new function () {
            this.NAME = "name";
            this.EMAILID = "emailid";
            this.PASSWORD = "password";
            this.SERVICE = "service";
            this.DEFAULT = "default";
        }
    };

    this.EVENT_NOTIFICATIONS = "eventnotifications__baas";
    this.EventNotifications = new function () {
        this.TABLE = "table";
        this.OPERATION = "operation";
        this.UPDATED_COLUMNS = "updatedcolumns";
        this.CONDITION = "condition";
        this.SUBJECT = "subject";
        this.APPLICATION = "application";
        this.TO = "to";
        this.CC = "cc";
        this.BCC = "bcc";
        this.TEMPLATE = "template";
    }

    this.USERS = "users__baas";
    this.Users = new function () {
        this.USERNAME = "username";
        this.PASSWORD = "password";
        this.EMAILID = "emailid";
        this.PHONE_NO = "phoneno";
        this.PICTURE = "picture";
        this.LAST_NAME = "lastname";
        this.FIRST_NAME = "firstname";
        this.DESIGNATION = "designation";
        this.ORGANIZATION = "organization";
        this.RENEWAL_DATE = "renewaldate";
        this.MEMBERSHIP_DATE = "membershipdate";


        this.APPLICATIONS = "applications";
        this.Applications = new function () {
            this.ORGANIZATIONS = "organizations";
        };
        this.ORGANIZATIONS = "organizations";
        this.USERGROUPID = "usergroupid";
        this.RECORD_LIMIT = "recordlimit";
        this.FORGOT_PASSWORD_CODE = "forgotpasswordcode";
        this.USER_STATE = "userstate";
        this.UserState = new function () {
            this.LAST_ORGANIZATION = "lastorganization";
            this.ORGANIZATION_WISE_STATE = "organizationwisestate";
            this.OrganizationWiseState = new function () {
                this.ORGANIZATION = "organization";
                this.APPLICATION = "application";
            }
            this.APPLICATION_WISE_STATE = "applicationwisestate";
            this.ApplicationWiseState = new function () {
                this.ORGANIZATION = "organization";
                this.APPLICATION = "application";
                this.MENU = "menu";
                this.QUICK_VIEW = "quickview";
            }
        }
    };

    this.VIEWS = "views__baas";
    this.Views = new function () {
        this.QUERY = "query";
        this.TABLE = "table";
        this.ID = "id";
        this.APPLICATION = "application";
    };

    this.FLEXFIELDS = "flexfields__baas"
    this.Flexfields = new function () {
        this.TABLE = "flextable";
        this.COLUMNVALUE = "flexvalue";
        this.LABEL = "label";
        this.EXPRESSION = "expression";
        this.ORGANIZATION = "organizationid"
    }

    this.LOGS = "logs__baas";
    this.Logs = new function () {
        this.USERID = "userid";
        this.ORGANIZATIONID = "organizationid";
        this.APPLICATIONID = "applicationid";
        this.DATE = "date";
        this.PARAMS = "params";
        this.URI = "uri";
        this.DOMAIN = "domain";
        this.LOGS = "logs";
        this.Logs = new function () {
            this.LOG = "log";
            this.LOGDATE = "logdate";
        }
    }
};


exports.Cache = new function () {
    this.Table = new function () {
        this.COLUMNSCACHE = "columnscache";
    }
};


exports.ModulesConstants = new function () {
    this.OrganizationModule = new function () {
        this.ID = "OrganizationModule";
        this.MODULE_PATH = "../../modules/OrganizationModule.js";
        this.ORGANIZATIONID = "_organizationid_";
    }

    this.ValidationModule = new function () {
        this.ID = "ValidationModule";
        this.MODULE_PATH = "../../modules/ValidationModule.js";
    }

    this.WhenModule = new function () {
        this.ID = "WhenModule";
        this.MODULE_PATH = "../../modules/WhenModule.js";
//        this.WEEKS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
//        this.MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
//        this.DAYS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31"];
    }

    this.UDTModule = new function () {
        this.ID = "UDTModule";
        this.MODULE_PATH = "../../modules/UDTModule.js";
    }

    this.AggregateModule = new function () {
        this.ID = "AggregateModule";
        this.MODULE_PATH = "../../modules/AggregateModule.js";
        this.AGGREGATES = 'aggregates';
    }

    this.FlexfieldModule = new function () {
        this.ID = "FlexfieldModule";
        this.MODULE_PATH = "../../modules/FlexfieldModule.js";
    }

    this.EventNotificationModule = new function () {
        this.ID = "EventNotificationModule";
        this.MODULE_PATH = "../../modules/EventNotificationModule.js";
    }

    this.ReplicateModule = new function () {
        this.ID = "ReplicateModule";
        this.MODULE_PATH = "../../modules/ReplicateModule.js";
    }

    this.FileModule = new function () {
        this.ID = "FileModule";
        this.MODULE_PATH = "../../modules/FileModule.js";
    }

    this.AuditTrail = new function () {
        this.ID = "AuditTrail";
        this.MODULE_PATH = "../../modules/AuditTrail.js";
        this.Columns = new function () {
            this.CREATED_BY = "__createdby";
            this.CREATED_ON = "__createdon";
            this.UPDATED_ON = "__updatedon";
            this.UPDATED_BY = "__updatedby";
        }
    }

    this.DateModule = new function () {
        this.ID = "DateModule";
        this.MODULE_PATH = "../../modules/DateModule.js";
        this.Property = new function () {
            this.FORMAT = "format";
            this.Format = new function () {
                this.FROM_NOW = "fromnow";
                this.CALENDAR_TIME = "calendartime";
            }
        }
    }

}

exports.MailService = new function () {
    this.Credential = new function () {
        this.USER_NAME = "user";
        this.PASSWORD = "pass";
        this.SERVICE = "service";
    };
    this.Options = new function () {
        this.FROM = "from";
        this.TO = "to";
        this.CC = "cc";
        this.BCC = "bcc";
        this.SUBJECT = "subject";
        this.TEXT = "text";
        this.HTML = "html"
        this.TEMPLATE = "template";
        this.TEMPLATE_DATA = "templatedata";
        this.ATTACHMENTS = "attachments";
        this.AttachmentOptions = new function () {
            this.FILE_NAME = "fileName";
            this.CONTENTS = "contents";
            this.CONTENT_TYPE = "contentType";
            this.CID = "cid";
        };
    };
}

exports.CurrentSession = new function () {
    this.CURRENT_USER_ID = "_CurrentUserId";
    this.CURRENT_USERGROUP_ID = "_CurrentUserGroupId";
    this.CURRENT_ORGANIZATIONID = "_CurrentOrganizationId";
}

exports.UserService = new function () {
    this.USER_NAME = "username";
    this.USER_ID = "userid";
    this.LOGIN = "login";
    this.VIEW_GROUP_INDEX = "viewgroupindex";
    this.VIEW_GROUPS = "viewgroups";
    this.MENU_INDEX = "menuindex";
    this.MENUS = "menus";
    this.ORGANIZATION_INDEX = "organizationindex";
    this.ORGANIZATIONS = "organizations";
    this.USER_GROUP = "usergroup";
    this.STATE = "state";
    this.VIEW = "view";
    this.ORGANIZATION_ADMIN = "organizationadmin";
    this.CONVERSION_RATE = "conversionrate";
}

exports.AppsStudio = new function () {
    this.ASK = "appsstudio";
    this.VIEW_GROUPS = "viewgroups__appsstudio";
    this.ViewGroups = new function () {
        this.LABEL = "label";
        this.MENUS = "menus";
        this.Menus = new function () {
            this.INDEX = "index";
            this.LABEL = "label";
            this.TABLE = "table";
            this.APPLICATIONID = "applicationid";
            this.VIEWID = "viewid";
            this.FILTER = "filter";
        }
        this.CREATOR = "_creator_";
        this.PARENT_VIEW_GROUP = "parentviewgroup";
    };

    this.VIEWS = "views__appsstudio";
    this.Views = new function () {
        this.ID = "id";
        this.BAAS_VIEWID = "baasviewid";
        this.CUSTOMIZATION = "customization";
        this.Customization = new function () {
            this.COLUMNS = "columns";
            this.SEQUENCE = "sequence";
            this.CHILDS = "childs";
            this.APPLIED_FILTERS = "appliedfilters";
            this.MAIN_TABLE_ID = "maintableid";
            this.FILTER = "filter";
            this.ORDERS = "orders";
        }
        this.USERID = "userid";
        this.ORGANIZATIONID = "organizationid";
        this.LABEL = "label";
        this.QUICK_VIEWID = "quickviewid";
        this.SCHEDULES = "schedules";
        this.Schedules = new function () {
            this.SCHEDULEID = "scheduleid";
            this.WHEN = "when";
            this.VISIBILITY = "visibility";
            this.Visibility = new function () {
                this.ON = "On";
                this.OFF = "Off";
                this.NONE = "None";
            }
        }
    };

    this.SCHEDULES = "schedules__appsstudio";
    this.Schedules = new function () {
        this.NAME = "name";
        this.VIEWID = "viewid";
        this.DATE_FILTER = "datefilter";
        this.DATE_COLUMN = "datecolumn";
    }

    this.ViewColumns = new function () {
        this.EXPRESSION = "expression";
        this.LABEL = "label";
        this.WIDTH = "width";
        this.VISIBILITY = "visibility";
        this.TYPE = "type";
        this.ID = "id";
        this.TIME_METADATA = "timemetadata";
        this.TIMEUNIT_METADATA = "timeunitmetadata";
        this.AMOUNT_METADATA = "amountmetadata";
        this.TYPE_METADATA = "typemetadata";
        this.TABLE = "table";
        this.RELATED_COLUMN = "relatedcolumn";
        this.VIEWID = "viewid";
        this.FILTER = "filter";
        this.MULTIPLE = "multiple";
        this.UPDATE = "update";
        this.VISIBLE_EXPRESSION = "visibleexpression";
        this.CHILD_COLUMN = "childcolumn";
        this.PARAMETER_MAPPINGS = "parametermappings";
        this.FILTER = "filter";
        this.FORMAT = "format";
        this.TOTAL_AGGREGATE = "totalaggregates";
    };
};

exports.ErrorCode = new function () {
    this.UNKNOWN_ERROR = {code:17, message:"Unknown error took place."};
    this.FIELDS_BLANK = {code:5, message:"'Please provide values of mandatory parameters['+params[0]+']'"};
    this.FIELDS_BLANK_IN_OBJECT = {code:29, message:"'Please provide values of mandatory parameters['+params[0]+'] in '+params[1]+'['+params[2]+']'"};
    this.NOT_VALID_FIELDS_IN_OBJECT = {code:30, message:"'Please provide values of mandatory parameters['+params[0]+'] in '+params[1]+'['+params[2]+']'"};
    this.NO_APP_SECRET_KEY = {code:1, message:"Application Secret key not provided."};
    this.NO_ORG_SECRET_KEY = {code:2, message:"Organization Secret key not provided."};
    this.APPLICATION_NOT_FOUND = {code:9, message:"'Application not found ['+params[0]+']'"};
    this.ORGANIZATION_NOT_FOUND = {code:10, message:"'Organization not found ['+params[0]+']'"};
    this.NO_APP_MAPPED_WITH_ORG = {code:7, message:"'Application ['+params[0]+'] not mapped in organization['+params[1]+']'"};
    this.ORG_DEACTIVATED = {code:11, message:"'Organization['+params[0]+'] is not active.'"};
    this.TABLE_NOT_FOUND = {code:24, message:"'Table not found ['+params[0]+']'"};
    this.VIEW_NOT_FOUND = {code:101, message:"'View not found ['+params[0]+'] in Table ['+params[1]+']'"};
    this.TABLE_APPLICATION_NOT_MAPPED = {code:18, message:"'Table['+params[0]+'] is not mapped with application['+params[1]+']'"};
    this.DATABASE_NOT_FOUND = {code:25, message:"Database not found"};
    this.UPDATES_NOT_FOUND = {code:26, message:"Operation not found"};
    this.IMPROPER_UPDATES = {code:27, message:"Improper updates"};
    this.UPDATE_TYPE_NOT_SUPPORTED = {code:28, message:"'Operation type not supported['+params[0]+']'"};
    this.MODULE_NOT_FOUND = {code:30, message:"'Can not find module ['+params[0]+']'"};
    this.MAIL_SENDING_FAIL = {code:31, message:"'Mail Sending Failed ['+params[0]+']'"};
    this.INVALID_DATA_TYPE = {code:32, message:"'Invalid Data Type ['+params[0]+']'"};
    this.UNIQUE_VALIDATION_FAILED = {code:32, message:"'Duplicate value for primary columns ['+params[0]+']'"};
    this.VIEW_NOT_FOUND = {code:33, message:"'View not found for id ['+params[0]+']'"};
    this.SESSION_NOT_FOUND = {code:34, message:"Session Not found.Please login."};


    this.User = new function () {
        this.USER_NOT_FOUND = {code:1, message:"User Not Found."};
        this.UAG_NOT_FOUND = {code:12, message:"'User Authenticated Group not found['+params[0]+']'"};
        this.CREDENTIAL_MISSMATCH = {code:3, message:"Username/Password did not match."};
        this.NOT_APP_ACCESS = {code:8, message:"'User have no access rights in application['+params[0]+']'"};
        this.NOT_ORG_ACCESS = {code:6, message:"'User have no access right in organization['+params[0]+']'"};
        this.USER_ALREADY_EXISTS_IN_APP = {code:29, message:"'User already have access rights in application['+params[0]+']'"};
        this.USER_ALREADY_EXISTS_IN_ORG = {code:30, message:"'User already have access right in organization['+params[0]+']'"};

        this.PAGE_NOT_FOUND = {code:404, message:"Page Not Found"};

        this.PAGE_NOT_FOUND = "404";


    }
}
