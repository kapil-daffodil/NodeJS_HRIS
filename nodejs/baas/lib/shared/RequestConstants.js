/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 29/7/13
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */

exports.AppsStudio = new function () {
    this.ASK = "pask";
    this.OSK = "posk";
    this.USK = "usk";
    this.USER = "user";
    this.LOGID = "logid";
    this.VIEWID = "viewid";
    this.VIEW_INFO = "viewinfo";
    this.ViewInfo = new function () {
        this.DATA_NOT_REQUIRED = "datanotrequired";
        this.FILTER = "filter";
        this.PARAMETERS = "parameters";
        this.PARAMETER_MAPPINGS = "parametermappings";
        this.ParameterMappings = new function () {
            this.EXPRESSION = "expression";
            this.PARAMETER = "parameter";
        }
        this.TYPE = "type";
        this.CHILD_COLUMN = "childcolumn";
        this.MENU_LABEL = "menulabel";
        this.VIEW_GROUP = "viewgroup";
        this.USER_STATE = "userstate";
        this.ORGANIZATION_ADMIN = "organizationadmin";
        this.APPLICATION_DEVELOPER = "applicationdeveloper";
    };
    this.METADATA = "metadata";
    this.Metadata = new function () {
        this.COLUMNS = "columns";
        this.QUICK_VIEWS = "quickviews";
        this.QUICK_VIEW_INDEX = "quickviewindex";
        this.TYPE = "type";
        this.TABLE = "table";
        this.BAAS_VIEW_ID = "baasviewid";
        this.CHILDS = "childs";
        this.FILTER = "filter";
        this.PARAMETERS = "parameters";
        this.PARAMETER_MAPPINGS = "parametermappings";
        this.ORDERS = "orders";
        this.SCHEDULES = "schedules";
    }
    this.CUSTOMIZATION = "customization";
    this.Customization = new function () {
        this.COLUMNS = "columns";
        this.Columns = new function () {
            this.EXPRESSION = "expression";
            this.LABEL = "label";
            this.WIDTH = "width";
            this.VISIBILITY = "visibility";
            this.TYPE = "type";
        };
        this.CHILDS = "childs";
        this.LABEL = "label";
        this.OVERRIDE = "override";
        this.CALLBACK = "callback";
        this.APPLIED_FILTERS = "appliedfilters";
        this.ORDERS = "orders";
        this.SCHEDULES = "schedules";
        this.LEVEL = "level";
        this.Level = new function () {
            this.SELF = "Self";
            this.APPLICATION = "Application";
            this.ORGANIZATION = "Organization";
        }
        this.CHILD_VIEW = "childview";
        this.ChildView = new function () {
            this.COLUMNS = "columns";
        }
    };
    this.CHILDS = "childs";

}

exports.UserService = new function () {
    this.ASK = "ask";
    this.OSK = "osk";
    this.STATE = "state";
    this.State = new function () {
        this.MENU = "menu;"
        this.ORGANIZATION = "organization";
        this.VIEW_GROUP = "viewgroup";
        this.QUICK_VIEW = "quickview";
    }

}