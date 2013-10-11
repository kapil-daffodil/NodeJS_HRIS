//TODO
/**
 * _val, VAL in filter,
 * menu flters adn open it as a view
 * setCurrentRow in tbale on click of edit row
 * object handling for menu filter
 */
var TOOL_BAR_ID = 'app-tool-bar';
var BAAS_SERVER;
var windowUrl = (window.location.href);
if (windowUrl.indexOf("127.0.0.1") >= 0) {
    BAAS_SERVER = "http://127.0.0.1:1337";
} else {
    BAAS_SERVER = "http://173.255.119.199:1337";
}

var PRIMARY_COLUMN = "primarycolumn";
var KEY = "_id";
var COMPONENT_ID_KEY = "view-component";
var ACTION_METHOD = /ACTION_METHOD/g;
var ACTION_LABEL = /ACTION_LABEL/g;
var ACTION_CLASS = /ACTION_CLASS/g;
var UPDATE_SELECTED_KEYS = "updateselectedkeys";
var COMPONENT_ID = "componentid";
var USER_CURRENT_ROW = "usecurrentrow";
var appStrapDirectives = angular.module('$appstrap.directives', []);
var appStrapServices = angular.module('$appstrap.services', []);
//var DEFAULT_ACTION_TEMPLATE = "<div class='app-button app-button-border app-button-margin app-button-padding app-button-shadow' ang-click='ACTION_METHOD'>ACTION_LABEL</div>";
var SHOW_AS = "showas";
var CHILD_COMPONENT_ID = "childcomponentid";
var PARENT_COMPONENT_ID = "parentcomponentid";
var VIEW_CONTAINER = "#view-container";
var DEFAULT_ACTION_TEMPLATE = "<div class='ACTION_CLASS' title='ACTION_LABEL' ng-click='ACTION_METHOD'></div>";
var APPLANE_DEVELOPER_APP_ID = "applanedeveloper";
var APPLANE_DEVELOPER_ASK = "appsstudio";
var DATE_TYPE = "date";
var DATE_TIME_TYPE = "datetime";
var STRING_TYPE = "string";
var LOOK_UP_TYPE = 'lookup';
var FILE_KEY = 'key';
var FILE_NAME = 'name';
var SCHEDULE_TYPE = 'schedule';
var DURATION_TYPE = 'duration';
var SPAN = 'span';
var FREQUENCY = 'frequency';
var DATE_FORMAT = 'dd/mm/yyyy';
var LESS_THEN = "$lt";
var GREATER_EQ = "$gte";

var DATA_NOT_FOUND = 'No Data Found'; // when data is not found in lookup column, show no data found
var CELL_TEMPLATE = "<span ng-bind-html-unsafe='getColumnValue(row,col,true)'></span>";
var CURRENCY_TYPE = 'currency';

function AppUtil() {
}
AppUtil.tempComponents = 0;
AppUtil.getComponentId = function () {
    AppUtil.tempComponents = AppUtil.tempComponents + 1;
    return COMPONENT_ID_KEY + AppUtil.tempComponents;
}
AppUtil.isNullOrUndefined = function (obj) {
    return (obj === undefined || obj === null) ? true : false;

}

AppUtil.isTrueOrUndefined = function (obj) {
    return (obj === undefined || obj === null || obj == true) ? true : false;

}

AppUtil.getRecord = function (data, pKey, primaryColumn) {
    var dataCount = data ? data.length : 0;
    for (var i = 0; i < dataCount; i++) {
        var record = data[i];
        var recordKey = AppUtil.resolveDot(record, primaryColumn);
        var isEqual = angular.equals(recordKey, pKey);
        if (isEqual) {
            return record;
        }
    }
    return null;
}
AppUtil.rebindFieldExpression = function ($scope, model, field) {
    var fieldFirstExpression;
    var fieldLastDotExpression;
    var dottIndex = field.lastIndexOf(".");
    if (dottIndex >= 0) {
        model = model + "." + field.substring(0, dottIndex);
        field = field.substring(dottIndex + 1);
    }
    $scope.modelexpression = model;
    $scope.fieldexpression = field;
}

AppUtil.removeDottedValue = function (model, expression) {
    if (!model) {
        return;
    }
    var firstDottedIndex = expression.indexOf(".");
    if (firstDottedIndex >= 0) {
        expression = expression.substring(0, firstDottedIndex);
    }
    delete model[expression];

}
AppUtil.putDottedValue = function (model, expression, value) {
    if (!model) {
        alert("Model does not exits for putting dotted value")
        return;
    }
    var lastDottedIndex = expression.lastIndexOf(".");
    if (lastDottedIndex >= 0) {
        var firstExpression = expression.substring(0, lastDottedIndex);
        expression = expression.substring(lastDottedIndex + 1);
        model = AppUtil.resolveDot(model, firstExpression, true);
    }
    model[expression] = value;

}
AppUtil.resolveDot = function (model, expression, confirm, confirmType) {
    if (!model) {
        return;
    }

    while (expression !== undefined) {
        var fieldIndex = expression.toString().indexOf(".");
        var exp;
        if (fieldIndex >= 0) {
            exp = expression.substring(0, fieldIndex);
            expression = expression.substring(fieldIndex + 1);

        } else {
            exp = expression;
            expression = undefined;

        }

        //

        var arrayFirstIndex = exp.toString().indexOf("[");
        var arrayEndIndex = exp.toString().indexOf("]");

        if (arrayFirstIndex >= 0 && arrayEndIndex >= 0) {
            var arrayIndex = Number(exp.substring(arrayFirstIndex + 1, arrayEndIndex));
            exp = exp.substring(0, arrayFirstIndex);
            if (expression) {
                expression = arrayIndex + "." + expression;
            } else {
                expression = arrayIndex;
            }
        }


        if (model[exp] === undefined && !confirm) {
            return;
        }
        if (model[exp] !== undefined) {
            model = model[exp];
        } else {
            if (expression) {
                model[exp] = {}
            } else {
                if (confirmType == 'array') {
                    model[exp] = [];
                } else {
                    model[exp] = {}
                }
            }
            model = model[exp];
        }

    }
    return model;
}

AppUtil.getModel = function ($scope, modelExpression, confirm, confirmType) {
    return this.resolveDot($scope, modelExpression, confirm, confirmType);
}
AppUtil.getDataFromService = function (url, requestBody, callType, dataType, pBusyMessage, callback, errcallback) {

    var usk;
    if (typeof(Storage) !== "undefined") {
        usk = localStorage.usk;
    }
    requestBody.usk = usk;
    requestBody.enablelogs = true;

    if (pBusyMessage) {
        AppUtil.$rootScope.busymessage = pBusyMessage;
        AppUtil.$rootScope.showbusymessage = true
        if (!AppUtil.$rootScope.$$phase) {
            AppUtil.$rootScope.$apply();
        }
    }


    $.ajax({
        type:callType,
        url:url,
        data:requestBody,
        crossDomain:true,
        success:function (returnData, status, xhr) {
            AppUtil.$rootScope.showbusymessage = false
            if (!AppUtil.$rootScope.$$phase) {
                AppUtil.$rootScope.$apply();
            }
            callback(returnData.response ? returnData.response : returnData);


        },
        error:function (jqXHR, exception) {

            AppUtil.$rootScope.showbusymessage = false
            if (!AppUtil.$rootScope.$$phase) {
                AppUtil.$rootScope.$apply();
            }
            var stack = jqXHR.responseText;
            var jsonError
            try {
                jsonError = JSON.parse(jqXHR.responseText)
                stack = jsonError.stack
            } catch (e) {
                jsonError = {code:-1, stack:stack};
            }

            if (errcallback) {
                errcallback(jsonError);
            } else {
                alert(stack);
            }

        },
        timeout:1200000,
        dataType:dataType,
        async:true
    });


}


/*************************************User Data Source*******************************************/
appStrapDirectives.factory('$userDataSource', ['$http', '$timeout',
    function ($http, $timeout) {
        var $userDataSource = {
        };
        $userDataSource.init = function () {

        }

        $userDataSource.getAppData = function (usk, state, callBack, errorCallBack) {
            state.keepstructure = true;

            if (false) {
                var usr = {};
                callBack(usr.response);
                return;
            }
            AppUtil.getDataFromService(BAAS_SERVER + "/custom/module", {usk:usk, ask:"appsstudio", "module":"UserService", "method":"getUserState", parameters:JSON.stringify({"state":state, "usk":usk})}, "POST", "JSON", "Loading...", callBack, errorCallBack);
        }

        $userDataSource.logOut = function (callBack) {
            var url = BAAS_SERVER + "/logout";
            AppUtil.getDataFromService(url, {}, "POST", "JSON", "Sign out...", callBack);

        }


        return $userDataSource;

    }
]);

/************************************* End of User Data Source*******************************************/

/*************************************User Data Model*******************************************/

appStrapDirectives.factory('$userModel', ['$http', '$timeout',
    function ($http, $timeout) {
        var $userModel = {
        };
        $userModel.init = function ($scope, $dataSource) {
            this.$scope = $scope;
            this.$dataSource = $dataSource;
            $dataSource.init();
            var appData = $scope.appData;
            appData.viewGroups = {label:"", "display":["label"], options:[]};
            appData.organizations = {label:"", "display":["label"], options:[]}
            appData.userActions = {label:"", "display":["label"], options:[]}

            var usk;
            if (typeof(Storage) !== "undefined") {
                usk = localStorage.usk;
            }

            if (!usk) {
                $scope.appData.userLogin = false;
                return;
            }
            this.usk = usk;
            var that = this;
            //apply on change listener on child view group selectedIndex
            $scope.$watch("appData.childViewGroupIndex", function (newValue, oldValue) {
                if (newValue != undefined && newValue >= 0) {
                    var state = {organization:that.$scope.appData.organizations.options[that.$scope.appData.organizations.selectedIndex].id, viewgroup:that.$scope.appData.childViewGroups[newValue].label};
                    delete that.$scope.appData.currentMenuIndex;
                    that.getAppData(state);
                }

            })

            //apply on change listener on view group selectedIndex
            $scope.$watch("appData.viewGroups.selectedIndex", function (newValue, oldValue) {

                if (oldValue !== undefined && newValue != undefined && newValue >= 0) {
                    var state = {organization:that.$scope.appData.organizations.options[that.$scope.appData.organizations.selectedIndex].id, viewgroup:that.$scope.appData.viewGroups.options[newValue].label};
                    delete that.$scope.appData.currentMenuIndex;
                    that.getAppData(state);
                }
            })

            //apply on change listener on organization selectedIndex
            $scope.$watch("appData.organizations.selectedIndex", function (newValue, oldValue) {
                if (newValue != oldValue && oldValue !== undefined && newValue !== undefined) {
                    //we have to remove selectedIndex otherwise $scope will take it as value change in viewgroup
                    delete that.$scope.appData.viewGroups.selectedIndex;
                    delete that.$scope.appData.currentMenuIndex;
                    var state = {"organization":that.$scope.appData.organizations.options[newValue].id};
                    that.getAppData(state);
                }
            });

            $scope.$watch("appData.currentMenuIndex", function (newValue, oldValue) {
                if (newValue != oldValue && oldValue !== undefined && newValue !== undefined && newValue >= 0) {
                    var menu = $scope.appData.menus[newValue];
                    $scope.appData.currentView = {"viewgroup":$scope.appData.viewGroups.options[$scope.appData.viewGroups.selectedIndex].label, "menulabel":menu.menus.label, "viewid":menu.menus.viewid.id, "filter":menu.menus.filter, "applicationid":menu.menus.applicationid.id, "ask":menu.menus.applicationid.ask, "osk":$scope.appData.organizations.options[$scope.appData.organizations.selectedIndex].osk};
                }
            });

            $scope.onViewGroupSetting = function () {
                that.$scope.appData.currentMenuIndex = -1;
                that.$scope.appData.currentView = {"viewid":"viewgroups__appsstudio", "ask":"appsstudio", "filter":{"_creator_":"{_CurrentUserId}"}};
                if (!that.$scope.$$phase) {
                    that.$scope.$apply();
                }
            }
            $scope.onMenuSetting = function () {

                that.$scope.appData.currentMenuIndex = -1;
                that.$scope.appData.currentView = {"data":{"data":[]}, "metadata":{"aftersavecallback":function (menus) {
//                    var viewGroupIndex = that.$scope.appData.viewGroups.selectedIndex;
//                    that.$scope.appData.viewGroups.selectedIndex = -1;
//                    that.$scope.appData.viewGroups.selectedIndex = viewGroupIndex;
                }, "orders":{"menus.index":"asc"}, refreshonload:true, "label":"Menus", "primarycolumn":"menus._id", "filter":{"_id":"{viewgroupid}"}, "parameters":{"viewgroupid":that.$scope.appData.viewGroups.options[that.$scope.appData.viewGroups.selectedIndex]._id}, "enablemetadataaction":false, "columns":[

                    {"expression":"menus.index", "type":"index", "width":50, "visibility":"Both", "label":"Index"},
                    {"expression":"menus.label", "type":"string", "width":100, "visibility":"Both", "label":"Label"},
                    {"expression":"menus.applicationid", "type":"lookup", "width":150, "visibility":"Both", "label":"Application", "table":"applications__baas", "primarycolumns":[
                        {"expression":"id", "type":"string"}
                    ]},
                    {"expression":"menus.table", "filter":{"applications":"{menus_applicationid}"}, "parametermappings":{"menus_applicationid":"menus.applicationid"}, "type":"lookup", "width":150, "visibility":"Both", "label":"Table", "table":"tables__baas", "primarycolumns":[
                        {"expression":"id", "type":"string"}
                    ]},
                    {"expression":"menus.filter", "type":"object", "width":100, "visibility":"Both", "label":"Filter"}
                ], "type":"table", "table":"viewgroups__appsstudio", insertview:"table"}, "applicationid":"applanedeveloper", "ask":"appsstudio"};

            }

//            this.getAppData({viewgroup:"Northwind", organization:"global"});
            this.getAppData();


        };
        $userModel.getAppData = function (state) {
            var that = this;
            if (!state) {
                state = {};
            }
            that.$dataSource.getAppData(this.usk, state, function (user) {
                that.$scope.appData.userLogin = true;
                that.$scope.appData.username = user.username;
                var organizations = user.organizations;

                if (!organizations) {
                    organizations = [];
                }

                that.$scope.appData.organizations.options = organizations;
                if (organizations.length > 0) {
                    that.$scope.appData.organizations.selectedIndex = user.organizationindex;
                    that.$scope.appData.organizations.label = organizations[user.organizationindex].label;
                } else {
                    that.$scope.appData.organizations.label = "Organizations";
                }

                var userActions = user.userActions;

                if (!userActions) {
                    userActions = [
                        {"label":"Change Password", "template":"<app-change-password></app-change-password>"}
                    ];
                }
                that.$scope.appData.userActions.options = userActions;

                var viewGroups = user.viewgroups;
                if (!viewGroups) {
                    viewGroups = [];
                }
                that.$scope.appData.viewGroups.options = viewGroups;
                that.$scope.appData.childViewGroups = [];
                that.$scope.appData.childViewGroupIndex = undefined;
                var baasDeveloper = false;
                var developer = true;
                if (viewGroups.length > 0) {
                    that.$scope.appData.viewGroups.selectedIndex = user.viewgroupindex;
                    that.$scope.appData.viewGroups.label = viewGroups[user.viewgroupindex].label;
                    if (viewGroups[user.viewgroupindex]._id == '5205dd8f0c24531c65000004') {
                        baasDeveloper = true;
                    }
                    //check child view group

                    var selectedViewGroupLevel = viewGroups[user.viewgroupindex]._level;
                    var selectedViewGroupMode = viewGroups[user.viewgroupindex]._mode;
                    if (selectedViewGroupMode >= 0) {
                        for (var i = user.viewgroupindex + 1; i < viewGroups.length; i++) {
                            var obj = viewGroups[i];
                            if (obj._level <= selectedViewGroupLevel) {
                                break;
                            } else if (obj._level == selectedViewGroupLevel + 1) {
                                that.$scope.appData.childViewGroups.push(obj);
                            }

                        }
                    }
                } else {
                    that.$scope.appData.viewGroups.label = "View groups";
                    developer = false;
                }
                that.$scope.appData.menus = user.menus;

                if (user.menus && user.menus.length > 0) {
                    developer = user.menus[0].menus.applicationid.developer;
                    that.$scope.appData.currentMenuIndex = user.menuindex;
                }

                that.$scope.appData.applicationDeveloper = developer;
                that.$scope.appData.baasDeveloper = baasDeveloper;
                that.$scope.appData.organizationAdmin = user.organizationadmin;

                if (user.view) {
                    that.$scope.appData.currentView = user.view;
                } else {
                    that.$scope.appData.currentView = "clear";
                }

                that.$scope.appData.conversionrate = user.conversionrate;
                if (!that.$scope.$$phase) {
                    that.$scope.$apply();
                }

            }, function (error) {
                if (error.code == 1 || error.code == 34) {
                    delete localStorage.usk;
                    that.$scope.appData.userLogin = false;
                    if (!that.$scope.$$phase) {
                        that.$scope.$apply();
                    }
                } else {
                    alert(error.stack);
                }
            })

        }


        return $userModel;

    }
]);

/*********************************Meta data model********************/
appStrapDirectives.factory('$metaDataSource', ['$http', '$timeout',
    function ($http, $timeout) {
        var $metaDataSource = {
        };
        $metaDataSource.init = function () {

        }

        $metaDataSource.getView = function (pView, options, callBack) {


            var url = BAAS_SERVER + "/custom/module";


            var viewParameters = {};
            viewParameters.pask = pView.ask;
            viewParameters.posk = pView.osk;
            viewParameters.viewid = pView.viewid;

            var viewIdInfo = {keepstructure:true};
            if (options) {
                Object.keys(options).forEach(function (k) {
                    viewIdInfo[k] = options[k];
                })
            }
            if (pView.viewgroup) {
                viewIdInfo.viewgroup = pView.viewgroup;
            }
            if (pView.menulabel) {
                viewIdInfo.menulabel = pView.menulabel;
            }
            if (pView.parameters) {
                viewIdInfo.parameters = pView.parameters;
            }
            if (pView.parametermappings) {
                viewIdInfo.parametermappings = pView.parametermappings;
            }
            if (pView.filter) {
                viewIdInfo.filter = pView.filter;
            }
            if (pView.childcolumn) {
                viewIdInfo.childcolumn = pView.childcolumn;
            }
            viewParameters.viewinfo = viewIdInfo;
            var data = {};
            data.ask = "appsstudio";
            data.module = "ViewService";
            data.method = "getView";
            data.parameters = JSON.stringify(viewParameters);


            AppUtil.getDataFromService(url, data, "POST", "JSON", "Loading...", function (responseView) {
                if (responseView.dataexception) {
                    alert(responseView.dataexception);
                }
                callBack(responseView);
            });


        }


        return $metaDataSource;

    }
]);

appStrapDirectives.factory('$metaDataModel', ['$http', '$timeout', '$viewStack',
    function ($http, $timeout, $viewStack) {
        var $metaDataModel = {
        };
        $metaDataModel.init = function ($scope, $metaDataSource) {
            this.$scope = $scope;
            this.$metaDataSource = $metaDataSource;
            $metaDataSource.init();
            var appData = $scope.appData;

            var that = this;

            $scope.$watch("appData.currentView", function (newValue, oldValue) {
//                if (newValue && !angular.equals(newValue, oldValue)) {
                if (newValue === "clear") {
                    $viewStack.closeAllViews();
                } else if (newValue) {

                    if (newValue.metadata) {
                        $viewStack.addView(angular.copy(newValue), $scope.$new());
                    } else {
                        that.$metaDataSource.getView(newValue, {organizationadmin:$scope.appData.organizationAdmin, applicationdeveloper:$scope.appData.applicationDeveloper}, function (view) {
                            view[SHOW_AS] = newValue[SHOW_AS];
                            if (newValue[COMPONENT_ID]) {
                                view[COMPONENT_ID] = newValue[COMPONENT_ID];
                            }
                            view[PARENT_COMPONENT_ID] = newValue[PARENT_COMPONENT_ID];


                            $viewStack.addView(view, $scope.$new());
                        });
                    }
                    $scope.appData.currentView = false;

                }
            }, true)

        };

        return $metaDataModel;

    }
]);
/*********************************End of Meta data model********************/
/*****************Lookup********************************/


appStrapDirectives.factory('$appDataSource', ['$http', '$timeout', '$dataModel',
    function ($http, $timeout, $dataModel) {
        var $appDataSource = {
        };

        $appDataSource.getLookupDataSource = function (lookupOptions) {
            this.lookupOptions = lookupOptions;

            this.getData = function (query, callBack, errorCallBack) {
                var that = this;
                var componentId = this.lookupOptions["componentid"];
                var currentRow;
                if (componentId) {
                    currentRow = $dataModel.getCurrentRow(componentId);
                }


                var displayExpression = this.lookupOptions.primarycolumns[0].expression;

                var resourceTable = this.lookupOptions.table;
                if (angular.isObject(resourceTable)) {
                    resourceTable = resourceTable.id;
                }
                var resourceQuery = {"table":resourceTable, "columns":this.lookupOptions.primarycolumns};
                var filter;
                if (this.lookupOptions.filter) {
                    filter = this.lookupOptions.filter
                } else {
                    filter = {};
                }
                filter[displayExpression] = {"$regex":"^(" + query + ")", "$options":"-i"};
                var parameters = {};
                if (currentRow) {
                    parameters = angular.copy(currentRow);
                }
                var currentView = $dataModel.getView(componentId);
                if (currentView) {
                    var currentViewParameters = currentView.metadata.parameters;
                    if (currentViewParameters) {
                        Object.keys(currentViewParameters).forEach(function (k) {
                            if (parameters[k] === undefined) {
                                parameters[k] = currentViewParameters[k];
                            }
                        })
                    }
                }
                if (this.lookupOptions.parametermappings) {
                    Object.keys(that.lookupOptions.parametermappings).forEach(function (k) {
                        var param = that.lookupOptions.parametermappings[k];
                        if (parameters[k] === undefined) {
                            parameters[k] = AppUtil.resolveDot(parameters, param);
                        }
                    })
                }
                resourceQuery.filter = filter;
                resourceQuery.parameters = parameters;
                var usk;
                if (typeof(Storage) !== "undefined") {
                    usk = localStorage.usk;
                }
                var urlParams = {ask:this.lookupOptions.ask, osk:this.lookupOptions.osk, query:JSON.stringify(resourceQuery) };
                if (usk) {
                    urlParams.usk = usk;
                }
                AppUtil.getDataFromService(BAAS_SERVER + "/data", urlParams, "GET", "JSON", false, callBack, errorCallBack);
            }
        }

        return $appDataSource;

    }
]);

/*********************************************************************Lookup type ahead**********************************************************************************/

appStrapDirectives.directive('ngModelRemove', function () {
    return {
        restrict:'A',
        require:'ngModel',
        link:function (scope, elm, attr, ngModelCtrl) {
            if (attr.type === 'radio' || attr.type === 'checkbox') return;

//            elm.unbind('input').unbind('keydown').unbind('change');
            elm.unbind('input').unbind('change');
//            elm.bind('blur', function () {
//                scope.$apply(function () {
//                    ngModelCtrl.$setViewValue(elm.val());
//                });
//            });
        }
    };
});

appStrapDirectives.directive('appMultipleRefrence', ['$timeout', function ($timeout) {
    'use strict';
    return {
        restrict:'E',
        scope:true,
        replace:true,
        template:"<div class='app-multirefrence-parent'>" +
            "<span ng-bind='getValue' class='app-multirefrence-text'></span>" +
            "<input  type='text' tabindex='-1' class='app-multirefrence-cross app-cursor-pointer' style='color:#000000;width:10px;background: none;border:none;padding: 0px;margin: 0px;' ng-click='cancel($event)' value='X'/>" +
            "</div>",
        compile:function () {
            return{
                pre:function ($scope, iElement) {
                    $scope.cancel = function ($event) {
                        var selectedIndex = ($scope.$index);
                        var model = AppUtil.getModel($scope, $scope.modelexpression, false)

                        if (model) {
                            model.splice(selectedIndex, 1);
                            $event.preventDefault();
                            $event.stopPropagation();
                        }


                    };
                    $scope.getValue = function () {
                        if ($scope.option instanceof Object) {
                            return $scope.option[$scope.fieldexpression];
                        } else {
                            return $scope.option;
                        }
                    };
                }
            };
        }
    };
}
]);

'use strict';
appStrapDirectives.directive('appLookup', ['$compile', '$timeout', function ($compile, $timeout) {
    'use strict';
    return {
        restrict:'E',
        template:"<div class='app-height-full'></div>",
        scope:true,
        replace:true,
        compile:function () {
            return  {

                post:function ($scope, iElement, attrs) {
                    var optionsDiv = $('#app_popup');
                    if (optionsDiv.length == 0) {
                        $(document.body).append("<div id='app_popup'></div>");
                    }

                    var modelExpression = attrs.model;
                    var fieldExpression = attrs.field;

                    var bindType = attrs.bindtype;
                    var ds = attrs.datasource;
                    if (!bindType) {
                        if (ds && $scope[ds] && $scope[ds].length > 0 && (typeof $scope[ds][0] !== 'object')) {
                            bindType = "string";
                        } else if (fieldExpression.indexOf(".") >= 0) {
                            bindType = "object";
                        } else {
                            bindType = "string";
                        }
                    }
                    var multiple = attrs.multiple;

                    var fieldFirstExpression;
                    var fieldLastDotExpression;
                    var dottIndex = fieldExpression.lastIndexOf(".");
                    if (dottIndex >= 0) {
                        fieldFirstExpression = fieldExpression.substring(0, dottIndex);
                        fieldLastDotExpression = fieldExpression.substring(dottIndex + 1);

                    } else {
                        fieldFirstExpression = fieldExpression;
                    }


                    if (multiple === undefined) {
                        var modelValue = AppUtil.getModel($scope, modelExpression, false);
                        if (!modelValue) {
                            multiple = false;
                        } else {
                            modelValue = AppUtil.getModel(modelValue, fieldFirstExpression, false);
                            if (modelValue) {
                                if (modelValue instanceof Array) {
                                    multiple = true
                                } else {
                                    multiple = false;
                                }
                            }
                        }
                    }
                    if (multiple == "false" || multiple == 'undefined') {
                        multiple = false;
                    } else if (multiple == "true") {
                        multiple = true;
                    }
                    var toBind;


                    toBind = modelExpression + "." + fieldFirstExpression;
                    if (multiple) {
                        if (bindType == 'string' && fieldLastDotExpression) {
                            toBind += "." + fieldLastDotExpression;
                            fieldLastDotExpression = undefined;
                        }
                        modelExpression = toBind;
                    } else {
                        if (fieldLastDotExpression) {
                            modelExpression = toBind;
                            toBind += "." + fieldLastDotExpression;
                        } else {
                            fieldLastDotExpression = fieldFirstExpression;
                        }
                    }

//                    if (multiple == "true") {
//                        multiple = true;
//                    } else {
//                        multiple = false;
//                    }


                    $scope.multiple = multiple;
                    $scope.bindtype = bindType;
                    $scope.modelexpression = modelExpression;
                    $scope.fieldexpression = fieldLastDotExpression;


                    var placeholder = attrs.placeholder;
                    if (!placeholder) {
                        placeholder = '';
                    }

                    if (multiple) {
                        placeholder = fieldFirstExpression;
                    }

                    var border = attrs.border;
                    if (border === undefined || border == true || border == 'true') {
                        border = true
                    } else {
                        border = false;
                    }

                    var lookUpTemplate = "<div  class ='app-look-up-container app-look-up-container-background' ng-class=\"{'app-border':" + border + "}\">" +
                        "<div class='app-look-up-text-container'>" +
                        "<input  ng-model-remove class='ref-input focus-element' type = 'text'   placeholder='" + placeholder + "' ng-model='" + toBind + "' data-popup-id='app_popup' />" +

                        "</div>" +
                        "<input type='text' class='second-input' style='color: white' tabindex='-1'  class='ref-button app-cursor-pointer'>" +
                        "</div>" +
                        "</div>";

                    var multipleTemplate = "<div style='height: auto;border:1px solid #CCCCCC;display: table;' class='app-width-full'>" +

                        "<div style='display:table-cell;right:20px;left: 0px;' class='app-white-backgroud-color'>" +
                        "<app-multiple-refrence ng-repeat='option in " + toBind + "'></app-multiple-refrence>" +

                        '<input  type ="text" placeholder="' + placeholder + '" class="app-float-left app-border-none app-padding-zero"  size="' + fieldFirstExpression.length + '"  data-popup-id="app_popup" style="margin-left:2px;min-height:29px;"/>' +
                        "</div>" +

                        "<div style='display:table-cell;width:33px;vertical-align: middle;padding-left: 5px;' class='app-white-backgroud-color'>" +
                        "<input type='text' style='color: white' class='app-multi-refrence-down-arrow' tabindex='-1' >" +
                        "</div>" +

                        "</div>";

                    if (multiple) {
                        $(iElement).append($compile(multipleTemplate)($scope));
                    } else {
                        $(iElement).append($compile(lookUpTemplate)($scope));
                    }

                    $scope.$menu = $('<ul class="typeahead dropdown-menu"></ul>');
                    $scope.item = '<li><a href="#"></a></li>';
                    $scope.minLength = 1;
                    var inputElements = angular.element(iElement).find('input');
                    var inputElement = inputElements[0];
                    var showAllDivElement = inputElements[1];
                    $scope.timeout = null;
                    $scope.$showAllElement = $(showAllDivElement);
                    $scope.$element = $(inputElement)
                    $scope.shown = false
                    $scope.showingLoadingImage = false
                    $scope.lastSelected = null;
                    $scope.select = function () {
                        var selectedIndex = $scope.$menu.find('.active').index();
                        $scope.updater($scope.data[selectedIndex]);
                        return $scope.hide()
                    };
                    $scope.show = function () {

                        var dataPopupId = $scope.$element.attr("data-popup-id");
                        var dataPopupElement = $("#" + dataPopupId);

                        var offset = $scope.$element.offset();

                        var posY = offset.top - $(window).scrollTop();
                        var posX = offset.left - $(window).scrollLeft();
                        var elemHeight = $scope.$element[0].offsetHeight;

                        var top = offset.top
                        var windowHeight = $(window).height();


                        dataPopupElement
                            .append($scope.$menu)
                            .css({
                                top:((top + $scope.$menu.height() + $scope.$element.height()) >= windowHeight) ? (top - $scope.$element.height() - $scope.$menu.height() ) : posY + elemHeight,
                                left:posX,
                                position:"absolute"
                            });
                        $scope.$menu.show();

                        $scope.shown = true

                        return this
                    };
                    $scope.hide = function () {
                        $scope.$menu.hide();
                        var dataPopupId = $scope.$element.attr("data-popup-id");
                        var dataPopupElement = $("#" + dataPopupId);
                        $scope.shown = false
                        return this
                    };

                    $scope.highlighter = function (item) {
                        var query = $scope.query.replace(/[\-\[\]{}()*+?.,\\\^$|#\s]/g, '\\$&')
                        return item.replace(new RegExp('(' + query + ')', 'ig'), function ($1, match) {
                            return '<strong>' + match + '</strong>'
                        })
                    };
                    $scope.render = function (items) {
                        $scope.data = items;
                        items = $(items).map(function (i, item) {
                            if (angular.isObject(item)) {
                                item = item[$scope.fieldexpression];
                            }

                            i = $($scope.item);
                            i.find('a').html($scope.highlighter(item))
                            return i[0]
                        })

                        items.first().addClass('active')
                        $scope.$menu.html(items)
                        return this
                    };
                    $scope.next = function (event) {
                        var active = $scope.$menu.find('.active').removeClass('active')
                            , next = active.next()

                        var ulHeight = $scope.$menu.height();
                        var ulTop = $scope.$menu.scrollTop();
                        var activeLiTop = active.position().top;
                        var liHeight = $scope.$menu.find('li').height();


                        if (!next.length) {
                            next = $($scope.$menu.find('li')[0])
                            $scope.$menu.scrollTop(0);
                        } else {
                            if ((activeLiTop + liHeight) > ulHeight) {
                                var setScrollTop = ulTop + liHeight;
                                $scope.$menu.scrollTop(setScrollTop);
                            }
                        }

                        next.addClass('active')
                    };
                    $scope.prev = function (event) {
                        var active = $scope.$menu.find('.active').removeClass('active')
                            , prev = active.prev()

                        var ulHeight = $scope.$menu.height();
                        var ulTop = $scope.$menu.scrollTop();
                        var liHeight = $scope.$menu.find('li').height();

                        if (!prev.length) {
                            prev = $scope.$menu.find('li').last()
                            $scope.$menu.scrollTop($scope.$menu.find('li').last().position().top);
                        } else {
                            var activeLiTop = prev.position().top;
                            if (activeLiTop < 0) {
                                var setScrollTop = ulTop - liHeight;
                                $scope.$menu.scrollTop(setScrollTop);
                            }
                        }

                        prev.addClass('active')
                    };
                    $scope.listen = function () {
                        $scope.$element
                            .on('focus', $scope.focus)
                            .on('blur', $scope.blur)
                            .on('keypress', $scope.keypress)
                            .on('keyup', $scope.keyup)
                            .on('keydown', $scope.keydown)
                        $scope.$menu
                            .on('click', $scope.click)
                            .on('mouseenter', 'li', $scope.mouseenter)
                            .on('mouseleave', 'li', $scope.mouseleave)

                        $scope.$showAllElement
                            .on('click', $scope.showAllElementClick)
                    };

                    $scope.move = function (e) {
                        if (!$scope.shown) return

                        switch (e.keyCode) {
                            case 9: // tab
                            case 13: // enter
                            case 27: // escape
                                e.preventDefault()
                                break

                            case 38: // up arrow
                                e.preventDefault()
                                $scope.prev()
                                break

                            case 40: // down arrow
                                e.preventDefault()
                                $scope.next()
                                break
                        }

                        e.stopPropagation()
                    };
                    $scope.keydown = function (e) {
                        $scope.suppressKeyPressRepeat = ~$.inArray(e.keyCode, [40, 38, 9, 13, 27])
                        $scope.move(e)
                    };
                    $scope.keypress = function (e) {
                        if ($scope.suppressKeyPressRepeat) return
                        $scope.move(e)
                    };
                    $scope.keyup = function (e) {
                        switch (e.keyCode) {
                            case 40: // down arrow
                            case 38: // up arrow
                            case 16: // shift
                            case 17: // ctrl
                            case 18: // alt
                                break

                            case 9: // tab
                            case 13: // enter

                                if (!$scope.shown) return
                                $scope.select()
                                break

                            case 27: // escape
                                if (!$scope.shown) return
                                $scope.hide()
                                break

                            default:
                                $scope.lookup(e, false)
                        }

                        e.stopPropagation()
                        e.preventDefault()
                    };
                    $scope.focus = function (e) {
                        $scope.focused = true
                    };
                    $scope.blur = function (e) {
                        $timeout(function () {

                            if ($scope.$element.is(":focus")) {
                                return;
                            }
                            $scope.focused = false;
                            var value = $scope.$element.val();
                            $scope.updater(value);

                            if (!$scope.mousedover && $scope.shown) $scope.hide()
                        }, 200);

                    };
                    $scope.click = function (e) {
                        e.stopPropagation()
                        e.preventDefault()
                        $scope.select()
                        $scope.$element.focus()
                    };
                    $scope.mouseenter = function (e) {
                        $scope.mousedover = true
                        $scope.$menu.find('.active').removeClass('active')
                        $(e.currentTarget).addClass('active')
                    };
                    $scope.mouseleave = function (e) {
                        $scope.mousedover = false
                        if (!$scope.focused && $scope.shown) $scope.hide()
                    };
                    $scope.showAllElementClick = function (e) {
                        $scope.$element.focus();
                        $scope.lookup(e, true);
                    };
                    $scope.lookup = function (ev, showAll) {
                        var items;
                        if (showAll) {
                            $scope.query = '';
                        } else {
                            $scope.query = $scope.$element.val();
                        }
                        $scope.source($scope.query, $scope.process);

                    };
                    $scope.source = function (query, callBack) {

                        if ($scope[attrs.datasource] instanceof Array) {
                            var options = $scope[attrs.datasource];
                            var optionsCount = options.length;
                            var optionsToShow = [];

                            if (query && query.length > 0) {
                                for (var i = 0; i < optionsCount; i++) {
                                    var option = options[i];
                                    try {
                                        if (option.toLowerCase().match("^" + query.toLowerCase())) {
                                            optionsToShow.push(option);
                                        }
                                    } catch (e) {
                                        //ignore exception in matching
                                    }
                                }
                            } else {
                                optionsToShow = options;
                            }

                            callBack(optionsToShow);
                            return;
                        }
                        var items;

                        if ($scope.timeout != null) {
                            $timeout.cancel($scope.timeout);
                        }


                        $scope.timeout = $timeout(function () {

                            var loadingHtml = '<img src="../images/loading.gif" class="app-input-loading-image">';
                            $(loadingHtml).insertAfter($scope.$element);
                            $scope.showingLoadingImage = true;

                            $scope[attrs.datasource].getData(query, function (data) {
                                $timeout.cancel($scope.timeout);
                                $scope.timeout = null;

                                $($scope.$element).next().remove();
                                $scope.showingLoadingImage = false;

                                if (!$scope.$element.is(":focus")) {
                                    return;
                                }

                                if (query == '' || query == $scope.$element.val()) {
                                    var optionData = data.data;
                                    if (optionData.length == 0) {
                                        optionData.push(DATA_NOT_FOUND);
                                    }
                                    callBack(optionData);
                                }
                            });
                        }, 200);


                    };
                    $scope.process = function (items) {

                        if (!items.length) {
                            return $scope.shown ? $scope.hide() : this
                        }

                        return $scope.render(items.slice(0, $scope.items)).show()
                    };

                    $scope.updater = function (value) {

                        var multiple = $scope.multiple;
                        var modelExpression = $scope.modelexpression;
                        var fieldExpression = $scope.fieldexpression;
                        var bindType = $scope.bindtype;
                        var confirmType;
                        if (multiple) {
                            confirmType = "array";
                        } else {
                            confirmType = "object";
                        }


                        var lastValue = $scope.lastUpdatedValue;
                        if (lastValue) {
                            if ((lastValue instanceof Object) && fieldExpression) {
                                lastValue = lastValue[fieldExpression];
                            }
                            var v = value;
                            if ((v instanceof Object) && fieldExpression) {
                                v = v[fieldExpression];
                            }
                            if (v == lastValue && v == $scope.$element.val()) {
                                return;
                            }

                        }
                        $scope.lastUpdatedValue = value;


                        var model = AppUtil.getModel($scope, modelExpression, true, confirmType);


                        if (multiple) {
                            $scope.$element.val("");
                            if (value === undefined || value.toString().trim().length == 0) {
                                return;
                            }
                            if (bindType == 'object' && !(value instanceof Object)) {
                                var v = {}
                                v[fieldExpression] = value;
                                value = v;
                            }
                            model.push(value);
                        } else {
                            var v = value;
                            if ((v instanceof Object) && fieldExpression) {
                                v = v[fieldExpression];
                            }
                            $scope.$element.val(v);
                            if (bindType == 'object') {
                                Object.keys(model).forEach(function (k) {
                                    delete model[k];
                                })
                                if (value instanceof Object) {
                                    Object.keys(value).forEach(function (k) {
                                        model[k] = value[k];
                                    })
                                } else {
                                    if (value === undefined || value === null || value.trim().length == 0) {

                                        if (attrs.field) {
                                            AppUtil.removeDottedValue(AppUtil.resolveDot($scope, attrs.model), attrs.field);
                                        }

                                    } else {
                                        model[fieldExpression] = value;
                                    }

                                }

                            } else {
                                model[fieldExpression] = value;

                            }
                        }


                        var onChangeFn = $scope.$element.attr('onselection');
                        if (onChangeFn) {
//                            scope[onChangeFn](updatedValue, scope.column);
                        }
                        if (!$scope.$$phase) {
                            $scope.$apply();
                        }

                        return value;


                    }
                    $scope.listen();
                }

            };
        }

    };
}
]);
/*********************************************************************End of Lookup type ahead**********************************************************************************/

/*****************END of Lookup************************/


appStrapDirectives.directive('appChangePassword', ["$compile", "$viewStack", function ($compile, $viewStack) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        compile:function () {
            return  {
                post:function ($scope, iElement) {

                    var config = {
                        template:"<div class='app-popup-title'>Change Password</div>" +
                            "<div class=\"app-popup-body app-text-align-center\">" +
                            "<input type='password' placeholder='Old Password' ng-model='oldpwd' class='app-border app-change-password'/>" +
                            "<input type='password' placeholder='New Password' ng-model='newpwd' class='app-border app-change-password'/>" +
                            "<input type='password' placeholder='Confirm Password' ng-model='confirmpwd' class='app-border app-change-password'/>" +
                            "<span class='app-change-password-message' ng-bind='message'></span>" +
                            "</div>" +


                            "<div class=\"app-width-full app-float-left app-text-align-center\" style='margin:0px 0px 15px 0px;'>" +
                            "<input type=\"button\" ng-click='reset()' value='Reset' class=\"app-button app-button-border app-button-margin app-button-shadow app-advance-search-bttn-padding\" />" +
                            "<input type=\"button\" ng-click='cancel()' value='Cancel' class=\"app-button app-button-border app-button-margin app-button-shadow app-advance-search-bttn-padding\"/>" +
                            "</div>" +

                            "</div>",
                        scope:$scope,
                        autohide:false,
                        width:350
                    };

                    $scope.popup = $viewStack.showPopup(config);

                    $scope.cancel = function () {
                        if ($scope.popup) {
                            $scope.popup.hide();
                            delete $scope.popup;
                        }
                    }

                    $scope.reset = function () {
                        var oldPassword = $scope.oldpwd;
                        var newPassword = $scope.newpwd;
                        var confirmPassword = $scope.confirmpwd;
                        if (!oldPassword || oldPassword.trim().length == 0) {
                            $scope.message = 'Old Password cannot be blank';
                            return;
                        }
                        if (!newPassword || newPassword.trim().length == 0) {
                            $scope.message = 'New Password cannot be blank';
                            return;
                        }
                        if (!confirmPassword || confirmPassword.trim().length == 0) {
                            $scope.message = 'Confirm Password cannot be blank';
                            return;
                        }
                        if (!angular.equals(newPassword, confirmPassword)) {
                            $scope.message = 'New Password and Confirm Password are not matched';
                            return;
                        }
                        $scope.message = '';

                        var url = BAAS_SERVER + '/resetpassword';
                        var params = {};
                        params.pwd = newPassword;
                        params.oldpwd = oldPassword;
                        AppUtil.getDataFromService(url, params, "POST", "JSON", "Password Reset..", function () {
                            $scope.cancel();
                        });
                    }
                }
            };
        }
    }
}]);


appStrapDirectives.directive('appMenus', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div>" +
            '<app-action-collection ng-init="action=appData.viewGroups" class="app-float-left app-padding-left-five-px app-padding-right-five-px app-font-weight-bold app-background-seperator app-line-height-thirty-six"></app-action-collection>' +
            "<div ng-click='appData.childViewGroupIndex=$index' class='app-cursor-pointer app-float-left app-padding-left-five-px app-padding-right-five-px app-font-weight-bold app-background-seperator app-line-height-thirty-six' ng-repeat='childViewGroup in appData.childViewGroups' ng-bind='childViewGroup.label'></div>" +
            "<div class='app-menu' ng-show='appData.baasDeveloper' ng-click='onViewGroupSetting()'><div class='app-menu-setting'></div></div>" +
            "<div ng-repeat='menu in appData.menus' class='app-menu app-menu-border'>" +
            "<app-menu ng-class=\"{'selectedmenu':$index==appData.currentMenuIndex}\" ></app-menu>" +
            "</div> " +
            "<div class='app-menu' ng-show='appData.applicationDeveloper' ng-click='onMenuSetting()'><div class='app-menu-setting'></div></div>" +
            "</div>",
        compile:function () {
            return  {
                post:function ($scope) {

                }

            };
        }
    }
}]);


appStrapDirectives.directive('appConfirmation', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div>" +
            "<div class='app-width-full app-float-left app-header-background-color app-text-align-center app-font-weight-bold' style='line-height: 35px;' ng-bind='confirmationoptions.title'></div>" +
            "<div class='app-float-left app-width-full app-text-align-center' style='padding: 15px 0px;line-height: 20px;' ng-bind-html-unsafe='confirmationoptions.body'>" +
            "</div>" +
            "<div class='app-float-left app-width-full app-text-align-center'>" +
            "<span ng-repeat='option in confirmationoptions.options'>" +
            "<div class='app-button app-button-border app-button-margin app-button-shadow app-button-padding' ng-click='onConfirmClick(option)' ng-bind='option'></div>" +
            "</span>" +

            "</div>" +
            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement, attrs) {

                    $scope.onConfirmClick = function (option) {
                        if (attrs.onconfirm) {
                            $scope[attrs.onconfirm](option);
                        }
                    }
                }
            }
        }
    }
}]);


appStrapDirectives.directive('appMenu', ["$compile", "$viewStack", function ($compile, $viewStack) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div ng-click='onMenuClick(menu)' class='app-menu-label'>" +
            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement) {

                    var menu = $scope.menu.menus.label;
                    var imageUrl = $scope.menu.menus.imageurl;
                    var html = '';
                    if (imageUrl) {
                        html = '<div><img src="' + imageUrl + '" class="app-menu-setting"></div>';
                    } else if (menu != null && menu != undefined) {
                        html = "<div>" + menu + "</div>";
                    }

                    iElement.append($compile(html)($scope))
                    $scope.onMenuClick = function (menu) {
                        $scope.appData.currentMenuIndex = $scope.$index;
                    }
                }

            };
        }
    }
}]);


appStrapDirectives.directive('ngModelOnblur', function () {
    return {
        restrict:'A',
        require:'ngModel',
        link:function (scope, elm, attr, ngModelCtrl) {
            if (attr.type === 'radio' || attr.type === 'checkbox') return;

            elm.unbind('input').unbind('keydown').unbind('change');
            elm.bind('blur', function () {
                scope.$apply(function () {
                    ngModelCtrl.$setViewValue(elm.val());
                });
            });
        }
    };
});


appStrapDirectives.directive('appActionCollection', ["$compile", "$viewStack", function ($compile, $viewStack) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-cursor-pointer'>" +

            "<span ng-click='onActionCollectionClick()' class=\"app-action-collection\" ng-bind='action.label'></span>" +
            "<span class='app-action-collection-down-arrow-parent'><img ng-click='onActionCollectionClick($event)' class='app-action-collection-down-arrow-image' src='images/down_arrow_new.png'/></span>" +
            "<span ng-show='action.showsettingimage' class='app-action-collection-setting-parent'><img class='app-setting' ng-src='{{action.settingimage}}' ng-click='onSettingClick()'/></span>" +
            "</div>",

        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    $scope.onActionCollectionClick = function () {

                        var optionsHtml = "<div><app-action-collection-option  ng-repeat=\"option in action.options\" ng-init=\"level=1\" >" +
                            "</app-action-collection-option></div>";
                        var popupScope = $scope.$new();
                        var p = new Popup({
                            autoHide:true,
                            deffered:true,
                            escEnabled:true,
                            hideOnClick:true,
                            html:$compile(optionsHtml)(popupScope),
                            scope:popupScope,
                            element:iElement
                        });
                        p.showPopup();

                    }
                    $scope.onSettingClick = function () {
                        $viewStack.addView($scope.action.settingview, $scope);
                    }
                    $scope.getOptionLabel = function (option) {

                        var lbl;
                        var index;

                        if (angular.isObject(option)) {
                            var display = $scope.action.display;
                            lbl = option[display[0]];


                            if (angular.isObject(lbl)) {
                                lbl = lbl[display[1]];
                            }
                        } else {
                            lbl = option;
                        }
                        return lbl;
                    }

                }
            }
        }
    }
}]);

appStrapDirectives.directive('appActionCollectionOption', ["$compile", "$viewStack", function ($compile, $viewStack) {
    return {
        restrict:"E",
        replace:false,
        scope:true,
        template:"<div class='app-action-collection-option app-cursor-pointer app-white-space-nowrap app-light-gray-backgroud-color app-padding-five-px' ng-class='{\"app-font-weight-bold\":option._mode>=0}' ng-style='{\"padding-left\":5+10*option._level+\"px\"}' ng-bind='getOptionLabel(option)' ng-click='onActionCollectionOptionClick()' g-init=''>" +
            "</div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    $scope.onActionCollectionOptionClick = function () {
                        $scope.action.selectedIndex = ($scope.$index);
                        var template = $scope.option.template;
                        if (template) {
                            var html = ($compile)(template)($scope);
                            $(iElement).append(html);
                        }


//                        if ($scope.action.callback) {
//                            $scope.action.callback($scope.option);
//                        }
                    }
                },
                post:function ($scope, iElement) {
//
//                    var childs = $scope.option.childs;
//                    if (childs && childs.length > 0) {
//                        var optionsHtml = "<div><app-action-collection-option  ng-repeat=\"option in option.childs\" ng-init='level=level+1'>" +
//                            "</app-action-collection-option></div>";
//                        iElement.append($compile(optionsHtml)($scope));
//                    }
                }
            }
        }
    }
}]);


appStrapDirectives.directive('appView', ["$compile", "$dataModel", "$viewStack", "$appService", "$timeout", function ($compile, $dataModel, $viewStack, $appService, $timeout) {
    'use strict';
    return {
        restrict:"E",
        scope:true,
        replace:true,
        template:"<div class='app-view'></div>",
        compile:function () {
            return {
                pre:function ($scope, iElement, attrs) {
                    var componentId = attrs.componentid;
                    $scope.componentid = componentId;
                    $scope.view = $dataModel.getView(componentId);
                    $scope.childview = function ($event) {

                        var columns = [
                            {"label":"Label", "expression":"label", "type":"text", "width":200, "visibility":"Table"},
                            {"label":"Table", "expression":"table", "type":"lookup", "primarycolumns":[
                                {"expression":"id", "type":"string"}
                            ], "width":200, "table":"tables__baas", "visibility":"Table"},
                            {"expression":"filter", "type":"object", "label":"Filter", "width":100, "visibility":"Both"},
                            {"label":"Visibility", "expression":"visibility", "type":"lookup", "width":200, "options":["Both", "Table", "Panel", "None"], "visibility":"Table"},
                            {"label":"Parameter", "expression":"parametermappings", "type":"string", "width":200, "visibility":"Table", "multiple":true}
                        ];
                        var data = {"data":$scope.view.metadata.childs};
                        var metadata = {"label":"Manage child", "columns":columns, "parameters":{"table":$scope.view.metadata.table}, "type":"table"};
                        metadata.enablequickviewaction = false;
                        metadata.enablemetadataaction = false;
                        metadata.enablerowaction = false;
                        metadata.resize = false;

                        metadata.savecallback = function (childs) {
                            var viewPrimaryColumn = $scope.view.metadata[PRIMARY_COLUMN]
                            var table = $scope.view.metadata.table;
                            var tableParam = table + "_" + viewPrimaryColumn

                            var url = BAAS_SERVER + "/custom/module";
                            var parameters = {"pask":$scope.view.ask, "posk":$scope.view.osk, "customization":JSON.stringify({"childs":childs, "callback":false, "relatedcolumnparamname":tableParam}), "viewid":$scope.view.viewid};
                            var param = {"ask":"appsstudio", "module":"CustomizeViewService", "method":"saveViewCustomization", "parameters":JSON.stringify(parameters)}

                            AppUtil.getDataFromService(url, param, "POST", "JSON", "Loading...", function (data) {
                                var v = $dataModel.getView($scope[COMPONENT_ID]);
                                $viewStack.reOpen(v);
                            });
                        };
                        metadata.closeonsave = true;


                        var viewInfo = {};
                        viewInfo.data = data;
                        viewInfo.metadata = metadata;
                        viewInfo.showas = 'popup';
                        viewInfo.width = 700;
                        viewInfo.height = 200;
                        viewInfo.applicationid = APPLANE_DEVELOPER_APP_ID;
                        viewInfo.ask = APPLANE_DEVELOPER_ASK;
                        viewInfo.element = $event.target;
                        viewInfo[PARENT_COMPONENT_ID] = $scope.componentid;
                        $scope.childviewinfo = viewInfo;
                        $viewStack.addView(viewInfo, $scope);
                    }

                    $scope.orderby = function ($event) {

                        var columnsClone = $scope.view.metadata.columnsclone;
                        var columnsTemp = [];
                        if (columnsClone && columnsClone.length > 0) {
                            for (var i = 0; i < columnsClone.length; i++) {
                                var visibility = columnsClone[i].visibility;
                                if (visibility == 'Table' || visibility == 'Both') {
                                    columnsTemp.push(columnsClone[i].label);
                                }
                            }
                        }

                        var columns = [
                            {"label":"Column", "expression":"label", "type":"lookup", "width":150, "options":columnsTemp, "visibility":"Table"},
                            {"label":"Order", "expression":"$order", "type":"lookup", "width":100, "options":["asc", "desc", "none"], "visibility":"Table"} ,
                            {"label":"Recursive", "expression":"$recursive", "type":"boolean", "width":100, "visibility":"Table"}
                        ];

                        var orderData = [];

                        if ($scope.view.metadata.orders != null && $scope.view.metadata.orders != undefined) {
                            var orders = $scope.view.metadata.orders;
                            columnsClone = $scope.view.metadata.columns;

                            for (var i = 0; i < orders.length; i++) {
                                var order = orders[i];
                                var orderExp = Object.keys(order)[0];
                                for (var j = 0; j < columnsClone.length; j++) {
                                    var columnExp = columnsClone[j].expression;
                                    if (orderExp == columnExp) {
                                        var obj = {};
                                        obj.label = columnsClone[j].label;
                                        obj.$order = order[orderExp].$order;
                                        obj.$recursive = order[orderExp].$recursive;
                                        orderData.push(obj);
                                        break;
                                    }
                                }

                            }
                        }

                        var data = {"data":orderData};
                        var metadata = {"label":"Orders", "columns":columns, "type":"table"};

                        metadata.delete = false;
                        metadata.refresh = false;
                        metadata.navigation = false;
                        metadata.enablequickviewaction = false;
                        metadata.enablemetadataaction = false;
                        metadata.enablerowaction = false;
                        metadata.closeonsave = true;
                        metadata.resize = false;

                        metadata.savecallback = function (orders) {

                            var count = orders.length;
                            var populatedOrder = [];
                            var oldRecursive = $scope.view.metadata.$recursivecolumn;
                            var newRecursive;
                            for (var i = 0; i < count; i++) {
                                var label = orders[i].label;
                                var order = orders[i].$order;
                                var recursive = orders[i].$recursive;


                                if (!recursive) {
                                    recursive = false;
                                }

                                if (!label) {
                                    continue;
                                }

                                if (!order) {
                                    order = "asc";
                                }

                                columnsClone = $scope.view.metadata.columns;

                                for (var k = 0; k < columnsClone.length; k++) {

                                    if (columnsClone[k].label == label) {
                                        var exp = columnsClone[k].expression;
                                        if (order != 'none') {
                                            var obj = {};
                                            if (recursive) {
                                                newRecursive = exp;
                                            }
                                            obj[exp] = {$order:order, $recursive:recursive};
                                            columnsClone[k].order = order;
                                            populatedOrder.push(obj);
                                        } else {
                                            delete columnsClone[k].order;
                                        }

                                        break;
                                    }
                                }
                            }

                            if (populatedOrder.length > 0) {
                                $scope.view.metadata.orders = populatedOrder;
                            } else {
                                delete $scope.view.metadata.orders;
                            }

                            if (angular.equals(oldRecursive, newRecursive)) {
                                $dataModel.refresh($scope.componentid);
                            }


                            //save customization
                            var url = BAAS_SERVER + "/custom/module";
                            var parameters = {"pask":$scope.view.ask, "posk":$scope.view.osk, "customization":JSON.stringify({"orders":populatedOrder, "callback":false}), "viewid":$scope.view.viewid};
                            var param = {"ask":"appsstudio", "module":"CustomizeViewService", "method":"saveViewCustomization", "parameters":JSON.stringify(parameters)}
                            AppUtil.getDataFromService(url, param, "POST", "JSON", "Loading...", function (data) {
                                if (!angular.equals(oldRecursive, newRecursive)) {
                                    var v = $dataModel.getView($scope[COMPONENT_ID]);
                                    $viewStack.reOpen(v);
                                }

                            });
                            //End of save customization


                        };


                        var viewInfo = {};
                        viewInfo.data = data;
                        viewInfo.metadata = metadata;
                        viewInfo.showas = 'popup';
                        viewInfo.width = 500;
                        viewInfo.height = 441;
                        viewInfo.applicationid = APPLANE_DEVELOPER_APP_ID;
                        viewInfo.ask = APPLANE_DEVELOPER_ASK;
                        viewInfo.element = $event.target;
                        viewInfo[PARENT_COMPONENT_ID] = $scope.componentid;
                        $scope.childviewinfo = viewInfo;
                        $viewStack.addView(viewInfo, $scope);
                    }

                    $scope.columnview = function ($event) {

                        var columns = [
                            {"label":"Index", "expression":"index", "type":"index", "width":40, "visibility":"Table"},
                            {"expression":"label", "type":"string", "label":"Label", "width":100, "visibility":"Both"},
                            {"expression":"expression", "type":"string", "label":"Expression", "width":200, "visibility":"Both"},
                            {"label":"Width", "expression":"width", "type":"text", "width":50, "visibility":"Table"}


                        ];
                        if ($scope.appData.applicationDeveloper) {
                            columns.push({"label":"Aggregate", "expression":"totalaggregates", "type":"lookup", "width":100, "options":["sum"], "visibility":"Table"});
                            columns.push({"label":"Visibility", "expression":"visibility", "type":"lookup", "width":100, "options":["Both", "Table", "Panel", "Child", "Off", "Query", "None", "Embed"], "visibility":"Table"});
                            columns.push({"label":"Update", "expression":"update", "type":"boolean", "width":50, "visibility":"Table"});
                            columns.push({"expression":"filter", "type":"object", "label":"Filter", "width":100, "visibility":"Both"});
                            columns.push({"label":"Visible Exp", "expression":"visibleexpression", "type":"string", "width":100, "visibility":"Table"});
                        } else if ($scope.appData.organizationAdmin) {
                            columns.push({"label":"Visibility", "expression":"visibility", "type":"lookup", "width":100, "options":["Both", "Table", "Panel", "Child", "Off", "Query", "None", "Embed"], "visibility":"Table"});
                        } else {
                            columns.push({"label":"Visibility", "expression":"visibility", "type":"lookup", "width":100, "options":["Both", "Table", "Panel", "None"], "visibility":"Table"});
                        }

                        var data = {"data":angular.copy($scope.view.metadata.columnsclone)};

                        var metadata = {"label":"Manage Column", "columns":columns, "type":"table"};
                        metadata.enablequickviewaction = false;
                        metadata.enablemetadataaction = false;
                        metadata.delete = false;
                        metadata.insert = false;
                        metadata.enableselection = false;
                        metadata.enablerowaction = false;
                        metadata.resize = false;
                        metadata.savecallback = function (customization) {

                            var url = BAAS_SERVER + "/custom/module";
                            var parameters = {"pask":$scope.view.ask, "posk":$scope.view.osk, "customization":JSON.stringify({"columns":customization, "callback":false}), "viewid":$scope.view.viewid};
                            var param = {"ask":"appsstudio", "module":"CustomizeViewService", "method":"saveViewCustomization", "parameters":JSON.stringify(parameters)}

                            AppUtil.getDataFromService(url, param, "POST", "JSON", "Loading...", function (data) {
                                var v = $dataModel.getView($scope[COMPONENT_ID]);

                                $viewStack.reOpen(v);

                            });
                        };
                        metadata.closeonsave = true;


                        var viewInfo = {};
                        viewInfo.data = data;
                        viewInfo.metadata = metadata;
                        viewInfo.showas = 'popup';
                        viewInfo.width = 900;
                        viewInfo.height = 500;
                        viewInfo.element = $event.target;
                        viewInfo[PARENT_COMPONENT_ID] = $scope.componentid;

                        $viewStack.addView(viewInfo, $scope);
                    }
                    $scope.scheduleView = function ($event) {

                        var columns = [
                            {"label":"Name", "expression":"scheduleid.name", "type":"string", "width":100, "visibility":"Table"},
                            {"label":"Date column", "expression":"scheduleid.datecolumn", "type":"string", "width":100, "visibility":"Table"},
                            {"label":"Date filter", "expression":"scheduleid.datefilter", "type":"lookup", "options":["last7days"], "width":100, "visibility":"Table"},
                            {"label":"When", "expression":"when", "type":"schedule", "width":200, "visibility":"Table"},
                            {"label":"Visibility", "expression":"visibility", "type":"lookup", "width":100, "options":["On", "Off", "None"], "visibility":"Table"}


                        ];

                        if (!$scope.view.metadata.schedules) {
                            $scope.view.metadata.schedules = [];
                        }
                        var data = {"data":$scope.view.metadata.schedules};

                        var metadata = {"label":"Manage Schedule", "columns":columns, "type":"table"};
                        metadata.enablequickviewaction = false;
                        metadata.enablemetadataaction = false;
                        metadata.delete = false;
                        metadata.enableselection = false;
                        metadata.enablerowaction = false;
                        metadata.resize = false;
                        metadata.savecallback = function (customization) {

                            var url = BAAS_SERVER + "/custom/module";
                            var parameters = {"pask":$scope.view.ask, "posk":$scope.view.osk, "customization":JSON.stringify({"schedules":customization, "callback":false}), "viewid":$scope.view.viewid};
                            var param = {"ask":"appsstudio", "module":"CustomizeViewService", "method":"saveViewCustomization", "parameters":JSON.stringify(parameters)}

                            AppUtil.getDataFromService(url, param, "POST", "JSON", "Loading...", function (data) {
                                var v = $dataModel.getView($scope[COMPONENT_ID]);

//                                $viewStack.reOpen(v);

                            });
                        };
                        metadata.closeonsave = true;


                        var viewInfo = {};
                        viewInfo.data = data;
                        viewInfo.metadata = metadata;
                        viewInfo.showas = 'popup';
                        viewInfo.width = 700;
                        viewInfo.height = 400;
                        viewInfo.element = $event.target;
                        viewInfo[PARENT_COMPONENT_ID] = $scope.componentid;

                        $viewStack.addView(viewInfo, $scope);
                    }
                    $scope.resetfilter = function () {
                        alert("Reset filter called");
                        return;
                    }
                    $scope.applyfilter = function () {
                        $timeout(function () {
                            //iterate applied filters

                            var modelFilters = [];

                            var advanceFilters = $scope.view.metadata.viewfilters;
                            var count = advanceFilters.length;
                            var appliedFilters = $scope.view.metadata.appliedfilters;
                            for (var i = 0; i < count; i++) {
                                var advanceFilter = advanceFilters[i];
                                var expression = advanceFilter.expression;
                                var paramValue = $scope.view.filterparameters[expression];

                                var appliedFilterCount = appliedFilters.length;
                                var appliedFilterIndex = -1;
                                for (var j = 0; j < appliedFilterCount; j++) {
                                    var appliedFilter = appliedFilters[j];
                                    if (appliedFilter.expression == advanceFilter.expression) {
                                        appliedFilterIndex = j;
                                        break;
                                    }
                                }

                                if (paramValue !== undefined) {
                                    if (appliedFilterIndex < 0) {
                                        appliedFilters.push(advanceFilter);
                                    }
                                    paramValue = angular.copy(paramValue);
                                    delete paramValue.label;
                                } else {
                                    if (appliedFilterIndex >= 0) {
                                        appliedFilters.splice(appliedFilterIndex, 1);
                                    }
                                }


                                $dataModel.addFilter($scope[COMPONENT_ID], expression, paramValue);

                            }

                            $dataModel.refresh($scope[COMPONENT_ID]);

                            //save customization


                            var url = BAAS_SERVER + "/custom/module";
                            var parameters = {"pask":$scope.view.ask, "posk":$scope.view.osk, "customization":JSON.stringify({"appliedfilters":$scope.view.filterparameters, "callback":false}), "viewid":$scope.view.viewid};
                            var param = {"ask":"appsstudio", "module":"CustomizeViewService", "method":"saveViewCustomization", "parameters":JSON.stringify(parameters)}

                            AppUtil.getDataFromService(url, param, "POST", "JSON", "Loading...", function (data) {
                            });

                            //End of save customization


                            if ($scope.filterPopup) {
                                $scope.filterPopup.hide();
                                delete $scope.filterPopup;
                            }
                        }, 200);

                    }
                    $scope.cancelfilter = function () {
                        if ($scope.filterPopup) {
                            $scope.filterPopup.hide();
                            delete $scope.filterPopup;
                        }
                    }

                    $scope.filterview = function ($event) {

                        var config = {
                            template:"<div class='app-popup-title'>Manage filter</div>" +
                                "<div class=\"app-popup-body advance-search-parent-div\">" +
                                "<app-filter ng-repeat = 'column in view.metadata.viewfilters'></app-filter>" +


                                "<div class=\"advance-search-button-row app-width-full app-float-left app-text-align-center\">" +
                                "<input type=\"button\" ng-click='resetfilter()' value='Reset' class=\"app-button app-button-border app-button-margin app-button-shadow app-advance-search-bttn-padding\" />" +
                                "<input type=\"button\" ng-click='applyfilter()' value='Apply' class=\"app-button app-button-border app-button-margin app-button-shadow app-advance-search-bttn-padding\"/>" +
                                "<input type=\"button\" ng-click='cancelfilter()' value='Cancel' class=\"app-button app-button-border app-button-margin app-button-shadow app-advance-search-bttn-padding\"/>" +
                                "</div>" +

                                "</div>",
                            scope:$scope,
                            autohide:false,
                            element:$event.target,
                            width:500
                        };

                        $scope.filterPopup = $viewStack.showPopup(config);

                    }

                    $scope.openPanel = function (insert) {
                        var viewinfo = {};
                        var panelColumns = [];
                        var columnCount = $scope.view.metadata.columnsclone.length;
                        for (var i = 0; i < columnCount; i++) {
                            var col = $scope.view.metadata.columnsclone[i];
                            var visibility = col.visibility;
                            if (visibility == 'Panel' || visibility == 'Both') {
                                if (col.subvisibility && col.subvisibility == 'Embed') {
                                    continue;
                                }
                                panelColumns.push(angular.copy(col));
                            } else if (visibility == 'Embed') {
                                var embedColumn = angular.copy(col);
                                embedColumn.visibility = "Panel";
                                embedColumn.type = "view";
                                panelColumns.push(embedColumn);
                            }

                        }
                        var childs = $scope.view.metadata.childs;
                        var childCount = childs ? childs.length : 0;
                        for (var i = 0; i < childCount; i++) {
                            var child = childs[i];
                            var visibility = child.visibility;
                            if (visibility == 'Both' || visibility == 'Panel') {
                                var expression = child.table.id;
                                var childClone = angular.copy(child);
                                childClone.expression = expression;
                                childClone.type = "view";
//                                panelColumns.push(childClone);
                            }

                        }

                        var data = {"data":[]};
                        var filter = $scope.view.metadata.filter;
                        if (!filter) {
                            filter = {};
                        } else {
                            filter = angular.copy(filter);
                        }
                        var parameters = $scope.view.metadata.parameters;
                        if (!parameters) {
                            parameters = {};
                        } else {
                            parameters = angular.copy(parameters);
                        }

                        if (insert) {
                            data.data.push(AppUtil.resolveDot($scope,$scope.view.metadata.dataExpression)[0]);
                        } else {
                            var currentRow = $dataModel.getCurrentRow($scope.componentid);
                            data.data.push(currentRow);

                            filter[$scope.view.metadata[PRIMARY_COLUMN]] = "{" + $scope.view.metadata[PRIMARY_COLUMN] + "}";
                            parameters[$scope.view.metadata[PRIMARY_COLUMN]] = AppUtil.resolveDot(currentRow, $scope.view.metadata[PRIMARY_COLUMN]);
                            Object.keys(currentRow).forEach(function (k) {
                                parameters[k] = angular.copy(currentRow[k]);
                            });

                        }
                        var panelRow = data.data[0];
                        var panelRowKey = AppUtil.resolveDot(panelRow, $scope.view.metadata[PRIMARY_COLUMN]);
                        var panelRowClone = AppUtil.getRecord($scope.view.dataclone, panelRowKey, $scope.view.metadata[PRIMARY_COLUMN]);

                        var panelmetadata = {"table":$scope.view.metadata.table, "columns":panelColumns, "type":"panel", "layout":"onecolumns", "filter":filter, "parameters":parameters};
                        panelmetadata.basicAndQuickViewPosition = "right";
                        panelmetadata[PRIMARY_COLUMN] = $scope.view.metadata[PRIMARY_COLUMN];
                        panelmetadata.save = true;
                        panelmetadata.saveParent = true;
                        panelmetadata.insertParent = true;


                        panelmetadata.insert = true;
                        panelmetadata.delete = false;
                        panelmetadata.refresh = false;
                        panelmetadata.navigation = false;
                        panelmetadata.enablemetadataaction = false;
                        var row = data.data[0];
                        var rowId = AppUtil.resolveDot(row, panelmetadata[PRIMARY_COLUMN]);

                        if (!$dataModel.isTempKey(rowId)) {
                            panelmetadata.refreshonload = true;
                        }

                        viewinfo.metadata = panelmetadata;
                        viewinfo.data = data;
                        viewinfo.dataclone = [panelRowClone];
                        viewinfo.applicationid = $scope.view.applicationid;
                        viewinfo.ask = $scope.view.ask;
                        viewinfo.osk = $scope.view.osk;
                        viewinfo[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                        $viewStack.addView(viewinfo, $scope);


                    }
                    $scope.onRowActionClick = function (action) {
                        $scope.view.metadata.lastRowAction = action;

                        $scope.view.metadata.editMode = false;
                        var actionType = action.type;
                        if (actionType == 'detail') {
                            $scope.openPanel(false);
                        } else if (actionType == 'view') {
                            var currentRow = $dataModel.getCurrentRow($scope[COMPONENT_ID]);
                            var clone = angular.copy(action);

                            var currentRowClone = angular.copy(currentRow);
                            var relatedColumn = clone.relatedcolumn;
                            var v = $scope.view;
                            var viewPrimaryColumn = v.metadata[PRIMARY_COLUMN]
                            var parameterMappings = {};
                            var viewParameterMappings = v.metadata.parametermappings;
                            if (viewParameterMappings) {
                                Object.keys(viewParameterMappings).forEach(function (k) {
                                    parameterMappings[k] = k;
                                });
                            }

                            var parameters = v.metadata.parameters;
                            if (!parameters) {
                                parameters = {};
                            }
                            var filter = {};
                            if (clone.filter && angular.isObject(clone.filter)) {
                                filter = clone.filter;
                            }

                            if (relatedColumn) {
                                var table = v.metadata.table;
                                var tableParam = table + "_" + viewPrimaryColumn
                                filter [relatedColumn] = "{" + tableParam + "}";
                                clone.filter = filter;

                                parameterMappings[tableParam] = viewPrimaryColumn;
                                parameterMappings[relatedColumn] = viewPrimaryColumn;

                            }
                            //pass parameters mappings


                            Object.keys(parameterMappings).forEach(function (k) {
                                var param = parameterMappings[k];
                                var val = AppUtil.resolveDot(currentRowClone, param);
                                if (val !== undefined) {
                                    parameters[param] = val;
                                }
                            })
                            Object.keys(currentRowClone).forEach(function (k) {
                                parameters[k] = currentRowClone[k];
                            })
                            clone[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                            clone.parameters = parameters;
                            clone.ask = $scope.view.ask;
                            clone.osk = $scope.view.osk;
                            clone.applicationid = $scope.view.applicationid;
                            clone.parametermappings = parameterMappings;
                            clone.basicAndQuickViewPosition = 'right';
                            $viewStack.addView(clone, $scope);
                        } else {
                            alert("Invalid Row action");
                        }
                    }


                    $scope.close = function (action) {
                        $viewStack.closeView($scope.componentid);
                    };

                    $scope.insert = function (action) {
                        var showPanel = true;
                        if ($scope.view.metadata.insertview && $scope.view.metadata.insertview == 'table') {
                            showPanel = false;
                        }
                        if ($scope.view[SHOW_AS] && $scope.view[SHOW_AS] == "popup") {
                            showPanel = false;
                        }

                        if ($scope.view.metadata.insertParent) {
                            $dataModel.getScope($scope.view[PARENT_COMPONENT_ID]).insert();

                        } else {
                            var componentId = $scope[COMPONENT_ID];
                            if (showPanel) {
                                $dataModel.insert(componentId);
                                $scope.openPanel(true);
                            } else {
                                $dataModel.insert(componentId);
                            }
                        }


                    };


                    $scope.save = function () {
                        $timeout(function () {
                            var callback = $scope.view.metadata.savecallback;
                            var closeonsave = $scope.view.metadata.closeonsave;

                            if (callback) {
                                var data = AppUtil.resolveDot($scope, $scope.view.metadata.dataExpression);
                                var updates = $dataModel.getUpdates($scope[COMPONENT_ID]);

                                if (closeonsave) {
                                    $scope.close();
                                }
                                callback(data, updates);
                                return;
                            }

                            var compId = $scope.componentid;
                            if ($scope.view.metadata.saveParent) {
                                compId = $scope.view[PARENT_COMPONENT_ID];
                            }
                            $dataModel.save(compId, function (data) {
                                $viewStack.closeChildView(compId);
                                if ($scope.view.metadata.aftersavecallback) {
                                    $scope.view.metadata.aftersavecallback(data);
                                }
                            });
                        }, 100)
                    };

                    $scope.refresh = function () {


                        var newInserts = $dataModel.getUpdates($scope.componentid);
                        var newInsertCount = newInserts.length;

                        if (newInsertCount > 0) {

                            var $confirmationScope = $scope.$new();
                            $confirmationScope.onconfirmation = function (option) {

                                $confirmationScope.confirmationPopUp.hide();

                                if (option == 'Ok') {
                                    $dataModel.refresh($scope.componentid, function () {
                                        $viewStack.closeChildView($scope.componentid);
                                    });
                                }
                            }

                            var modal = {
                                template:"<app-confirmation onconfirm = 'onconfirmation' ></app-confirmation>",
                                scope:$confirmationScope,
                                autohide:false,
                                width:300,
                                height:150,
                                hideOnClick:false
                            };

                            $confirmationScope.confirmationoptions = {"body":"Your data has been modified. <br/>  Abandon changes?", "title":"Confirmation Box", "options":["Ok", "Cancel"]};

                            $confirmationScope.confirmationPopUp = $viewStack.showPopup(modal);
                        } else {
                            $dataModel.refresh($scope.componentid, function () {
                                $viewStack.closeChildView($scope.componentid);
                            });
                        }
                    };

                    $scope.deleteData = function () {
                        $dataModel.deleteData($scope.componentid);
                    };

                    $scope.viewResize = function () {
                        $viewStack.viewResize($scope.componentid);
                    }

                    $scope.loadView = function (action) {

                        var useCurrentRow = action[USER_CURRENT_ROW];
                        if (useCurrentRow) {


                        } else {
                            var clone = angular.copy(action.view);
                            clone[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                            $viewStack.addView(clone, $scope);
                        }
                    }
                    $scope.update = function () {
                        var selectedKeys = $dataModel.getSelectedKeys($scope[COMPONENT_ID]);
                        var selectedKeyCount = selectedKeys.length;
                        if (selectedKeyCount == 0) {
                            alert("Please select record to update");
                            return;
                        }
                        var viewinfo = {};
                        var panelColumns = [];
                        var columnCount = $scope.view.metadata.columnsclone.length;
                        for (var i = 0; i < columnCount; i++) {
                            var col = $scope.view.metadata.columnsclone[i];
                            if (col.update) {
                                panelColumns.push(angular.copy(col));
                            }

                        }
                        if (panelColumns.length == 0) {
                            alert("No column has update property set to true");
                            return;
                        }


                        var data = {"data":[]};

                        var panelmetadata = {"table":$scope.view.metadata.table, "columns":panelColumns, "type":"panel", "layout":"onecolumns"};
                        panelmetadata.label = "Update";
                        panelmetadata.save = true;
                        panelmetadata.savecallback = function (updates, newUpdates) {

                            var updateCount = newUpdates ? newUpdates.length : 0;
                            for (var i = 0; i < updateCount; i++) {
                                var update = newUpdates[i];
                                delete update._id__temp;
                                for (var j = 0; j < selectedKeyCount; j++) {
                                    var selectedKey = selectedKeys[j];
                                    Object.keys(update).forEach(function (k) {
                                        selectedKey[k] = update[k];
                                    })

                                }

                            }
                            $scope.save();
                        }
                        panelmetadata.refresh = false;
                        panelmetadata.delete = false;
                        panelmetadata.insert = false;
                        panelmetadata.navigation = false;
                        panelmetadata.closeonsave = true;


                        panelmetadata.enablemetadataaction = false;
                        viewinfo.metadata = panelmetadata;
                        viewinfo.data = data;
                        viewinfo.applicationid = $scope.view.applicationid;
                        viewinfo.ask = $scope.view.ask;
                        viewinfo.osk = $scope.view.osk;
                        viewinfo.width = 400;
                        viewinfo.height = 400;
                        viewinfo[SHOW_AS] = "popup";
                        viewinfo[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                        $viewStack.addView(viewinfo, $scope);


                    }
                },
                post:function ($scope, iElement) {


                    var viewType = $scope.view.metadata.type;

                    var viewTemplate;
                    if (viewType == "table") {
                        viewTemplate = "<app-grid></app-grid>";
                    } else if (viewType == "panel") {
                        viewTemplate = "<app-panel></app-panel>";
                    } else {
                        alert("Not valid view type[" + viewType + "]");
                    }
                    $dataModel.setScope($scope[COMPONENT_ID], $scope);
                    var viewElement = $compile(viewTemplate)($scope);
                    iElement.append(viewElement);
                }
            }
        }
    }
        ;
}])

appStrapDirectives.directive('appTime', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        compile:function () {
            return  {
                pre:function ($scope, element) {

                },
                post:function ($scope, element, attrs) {
                    $scope.dataSourceTime = ["00:00 AM", "00:30 AM", "1:00 AM", "1:30 AM", "2:00 AM", "2:30 AM",
                        "3:00 AM", "3:30 AM", "4:00 AM", "4:30 AM", "5:00 AM", "5:30 AM", "6:00 AM",
                        "6:30 AM", "7:00 AM", "7:30 AM", "8:00 AM", "8:30 AM", "9:00 AM", "9:30 AM", "10:00 AM",
                        "10:30 AM", "11:00 AM", "11:30 AM", "12:00 PM", "12:30 PM", "1:00 PM", "1:30 PM", "2:00 PM", "2:30 PM",
                        "3:00 PM", "3:30 PM", "4:00 PM", "4:30 PM", "5:00 PM", "5:30 PM", "6:00 PM",
                        "6:30 PM", "7:00 PM", "7:30 PM", "8:00 PM", "8:30 PM", "9:00 PM", "9:30 PM", "10:00 PM",
                        "10:30 PM", "11:00 PM", "11:30 PM"];
                    var apptimeTemplate = "<div><app-lookup style='float: left;width:200px;height: 30px;' " +
                        "datasource='dataSourceTime' model='" + attrs.model + "' field='" + attrs.field +
                        "' placeholder=\"HH:MM AM\">" +
                        "</app-lookup></div>";
                    var elem = $compile(apptimeTemplate)($scope);
                    $(element).append(elem);

                }
            };

        }
    }
}]);

appStrapDirectives.directive('appToolBar', [ "$compile", "$dataModel", "$viewStack", "$appService",
    function ($compile, $dataModel, $viewStack, $appService) {
        'use strict';
        return {
            restrict:'A',
            scope:true,
            compile:function () {
                return {
                    pre:function ($scope) {


                    },
                    post:function ($scope, iElement) {

                        var html = "";

                        if ($scope.view.metadata.label) {
                            html += '<div class="app-font-weight-bold app-border-right-white app-float-left app-blue-text-color app-padding-five-px" ng-bind="view.metadata.label"></div>';
                        }

                        html += '<div class="app-float-right" style="height:30px;">';
                        html += '<app-bar-close-resize-view></app-bar-close-resize-view>';

                        if (AppUtil.isTrueOrUndefined($scope.view.metadata.enablemetadataaction)) {
                            html += "<app-bar-metadata></app-bar-metadata>";
                        }
                        html += '</div>';


                        html += '<div class="app-float-left" style="height:30px;" ng-class=\'{"app-float-right-important":view.metadata.basicAndQuickViewPosition == "right"}\'>';
                        if ($scope.view.metadata.quickviews) {
                            var saveCallBack = function (customization) {

                                var newViewInfo = customization[0];

                                var viewPrimaryColumn = $scope.view.metadata[PRIMARY_COLUMN]
                                var table = $scope.view.metadata.table;
                                var tableParam = table + "_" + viewPrimaryColumn

                                var url = BAAS_SERVER + "/custom/module";
                                var parameters = {"pask":$scope.view.ask, "posk":$scope.view.osk, "customization":JSON.stringify({"label":newViewInfo.label, "callback":false, "level":newViewInfo.apply, "override":newViewInfo.override}), "viewid":$scope.view.viewid};
                                var param = {"ask":"appsstudio", "module":"CustomizeViewService", "method":"saveCopyView", "parameters":JSON.stringify(parameters)}

                                AppUtil.getDataFromService(url, param, "POST", "JSON", "Loading...", function (data) {
                                    var v = $dataModel.getView($scope[COMPONENT_ID]);
                                    $viewStack.reOpen(v);
                                });


                            }
                            var quickviewcollection = {"label":$scope.view.metadata.quickviews[$scope.view.metadata.quickviewindex].label, "display":["label"], "callback":function (option) {
                                $viewStack.addView({viewid:option.id, ask:option.ask, osk:option.osk}, $dataModel.getScope($scope[COMPONENT_ID]).$parent);
                            }, "settingimage":"images/savequickview.png"};

                            var overrideOptions = ["Self"];

                            if ($scope.appData.organizationAdmin) {
                                overrideOptions.push("Organization");
                            }
                            if ($scope.appData.applicationDeveloper) {
                                overrideOptions.push("Application");
                            }
                            var quickViewColumns = [
                                {"expression":"label", "type":"string", "width":200, "visibility":"Both", "label":"label"},
                                {"expression":"apply", "type":"lookup", "width":200, "visibility":"Both", "label":"Apply for", "options":overrideOptions},
                                {"expression":"override", "type":"boolean", "width":15, "height":20, "visibility":"Both", "label":"Override"}
                            ];
                            var quickSettingView = {"data":{"data":[]}, "metadata":{"label":"Quick view", "enablemetadataaction":false, "savecallback":saveCallBack, "closeonsave":true, "columns":quickViewColumns, "type":"panel", "table":"viewgroups__applanedeveloper", "layout":"onecolumns", "delete":false, "refresh":false, "insert":false}, "applicationid":"applanedeveloper", "showas":"popup", "width":500, "height":200}

                            quickviewcollection.settingview = quickSettingView;
                            quickviewcollection.options = $scope.view.metadata.quickviews;
                            quickviewcollection.showsettingimage = true;
                            $scope.view.metadata.quickviewcollection = quickviewcollection;
                            html += "<app-bar-quickview></app-bar-quickview>";
                        }


                        html += "<app-bar-basic></app-bar-basic>";
                        html += '</div>';


                        iElement.append($compile(html)($scope));

                    }
                }
            }
        };
    }
]);


appStrapDirectives.directive('appBarCloseResizeView', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-float-right app-height-inherit'></div>",
        compile:function () {
            return{
                pre:function ($scope, iElement) {
                    var actions = [];
                    var metadata = $scope.view.metadata;

                    if (AppUtil.isTrueOrUndefined($scope.view.metadata.close)) {
                        var template = "<div class='app-height-inherit app-close-button-background app-float-left'>" + DEFAULT_ACTION_TEMPLATE + "</div>";
                        actions.push({"method":"close(action)", "label":"Close", "template":template, "class":"app-close-button app-bar-button"})
                    }


                    if (AppUtil.isTrueOrUndefined(metadata.resize)) {
                        var template = "<div class='app-height-inherit app-resize-button-background app-float-left'>" + DEFAULT_ACTION_TEMPLATE + "</div>";
                        actions.push({"method":"viewResize()", "label":"Resize", "template":template, "class":"app-resize-button app-bar-button"});
                    }
                    var actionCount = actions.length;
                    for (var i = 0; i < actionCount; i++) {
                        var action = actions[i];
                        var actionLabel = action.label;
                        var actionMethod = action.method;
                        var html = action.template;
                        var actionClass = action.class;

                        html = html.replace(ACTION_METHOD, actionMethod);
                        html = html.replace(ACTION_LABEL, actionLabel);
                        html = html.replace(ACTION_CLASS, actionClass);
                        var cellElement = $compile(html)($scope);
                        iElement.append(cellElement);

                    }
                }
            }
        }
    }
}]);


appStrapDirectives.directive('appBarQuickview', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div  class='app-bar-quickview app-blue-text-color'>" +
            "<div class='app-bar-quickview-wrapper'>" +
            "<app-action-collection ng-init=\"action=view.metadata.quickviewcollection\"></app-action-collection>" +
            "</div>" +
            "</div>"
    }
}]);


appStrapDirectives.directive('appBarBasic', [
    '$compile',
    function ($compile) {
        return {
            restrict:'E',
            replace:true,
            scope:true,
            template:"<div class='app-bar-basic'></div>",
            compile:function () {
                return  {
                    pre:function ($scope, iElement) {
                        var actions = [];
                        var metadata = $scope.view.metadata;


                        if (AppUtil.isTrueOrUndefined($scope.view.metadata.insert)) {
                            actions.push({"method":"insert(action)", "label":"Insert", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-insert-button app-bar-button"});
                        }

                        if (AppUtil.isTrueOrUndefined($scope.view.metadata.refresh)) {
                            actions.push({"method":"refresh(action)", "label":"Refresh", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-refresh-button app-bar-button"});
                        }

                        if (AppUtil.isTrueOrUndefined($scope.view.metadata.save)) {

                            var saveAction = {"method":"save(action)", "label":"Save", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-save-button app-bar-button app-float-left"};
                            saveAction.callback = $scope.view.metadata.savecallback;
                            saveAction.closeonsave = $scope.view.metadata.closeonsave;
                            actions.push(saveAction);
                        }

                        if (AppUtil.isTrueOrUndefined($scope.view.metadata.delete)) {
                            actions.push({"method":"deleteData(action)", "label":"Delete", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-delete-button app-bar-button"});
                        }
                        if ($scope.view.metadata.update) {
                            actions.push({"method":"update(action)", "label":"Update", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-update-button app-bar-button"});
                        }

                        if (AppUtil.isTrueOrUndefined($scope.view.metadata.navigation)) {
                            actions.push({"method":"", "label":"", "template":"<app-navigation></app-navigation>", "class":""});
                        }

                        if ($scope.view.metadata.appliedfilters) {
                            actions.push({"method":"", "label":"", "template":"<app-applied-filters></app-applied-filters>", "class":""});
                        }


                        var actionCount = actions.length;
                        for (var i = 0; i < actionCount; i++) {
                            var action = actions[i];
                            var actionLabel = action.label;
                            var actionMethod = action.method;
                            var html = action.template;
                            var actionClass = action.class;

                            html = html.replace(ACTION_METHOD, actionMethod);
                            html = html.replace(ACTION_LABEL, actionLabel);
                            html = html.replace(ACTION_CLASS, actionClass);
                            var cellElement = $compile(html)($scope);
                            iElement.append(cellElement);

                        }
                    }
                }
            }
        }
    }]);


appStrapDirectives.directive('appBarMetadata', ["$compile", "$viewStack", function ($compile, $viewStack) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-bar-metadata'></div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {

                    var html = '';
                    html += "<div class='app-bar-button app-column-schedule-button' ng-click=\"scheduleView($event)\" title='Manage schedules'></div>";
                    html += "<div class='app-bar-button app-column-order-by-button' ng-click=\"orderby($event)\" title='Order By'></div>";
                    html += "<div class='app-bar-button app-filter-view-button' ng-show='view.metadata.viewfilters' ng-click=\"filterview($event)\" title='Manage filters'></div>";
                    if ($scope.appData.applicationDeveloper) {
                        html += "<div class='app-bar-button app-child-view-button'  ng-click=\"childview($event)\" title='Manage child'></div>";
                    }
                    html += "<div class='app-bar-button app-column-view-button' ng-click=\"columnview($event)\" title='Manage column'></div>";

                    iElement.append($compile(html)($scope));

                }
            }
        }
    }
}]);


appStrapDirectives.directive("appAppliedFilters", ["$compile", "$dataModel", function ($compile, $dataModel) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='applied-filter-parent' ng-repeat='appliedFilter in view.metadata.appliedfilters'>" +

            "<div class=\"app-button app-button-border app-applied-filter-padding app-applied-filter-margin app-button-shadow\">" +
            "<app-applied-filter-label> </app-applied-filter-label>" +
            "<div ng-click=\"removeFilter(appliedFilter)\" class='app-remove-filter'>X</div>" +
            "</div>" +

            "</div>",


        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    $scope.removeFilter = function (appliedFilter) {
                        var exp = appliedFilter.expression;

                        var primaryColumn = $scope.view.metadata[PRIMARY_COLUMN]
                        var appliedFilters = $scope.view.metadata.appliedfilters;
                        var appliedFiltersCount = appliedFilters.length;
                        var applyFilterKey = appliedFilter[primaryColumn];


                        if (appliedFiltersCount > 0) {
                            for (var i = 0; i < appliedFiltersCount; i++) {
                                var record = appliedFilters[i];
                                var key = record[primaryColumn];
                                if (key == applyFilterKey) {
                                    $scope.view.metadata.appliedfilters.splice(i, 1);
                                    delete $scope.view.metadata.filter[exp];
                                    delete $scope.view.filterparameters[exp];
                                    break;
                                }
                            }
                            $dataModel.refresh($scope[COMPONENT_ID]);
                        }
                    };

                    $scope.onFilterSelection = function (value, action) {

                    };

                    $scope.onFilterBlur = function (value, action) {

                    }
                }
            }
        }
    }
}]);

appStrapDirectives.directive("appAppliedFilterLabel", ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:false,

        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    var filter = $scope.appliedFilter;

                    var filterType = filter.type;
                    var toBind = "view.filterparameters." + filter.expression;
                    if (filterType == 'lookup') {
                        var options = filter.options;
                        if (!options || options.length == 0) {
                            toBind += "." + filter.primarycolumns[0].expression;
                        }
                    } else {
                        toBind += ".label";
                    }
                    var template = "<div ng-bind-template='{{appliedFilter.label + \":\" + " + toBind + "}}' class='app-applied-filter-label' ></div>";
                    $(iElement).append($compile(template)($scope));

                }
            }
        }
    }
}]);


appStrapDirectives.directive("appText", ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-height-full'></div>",
        compile:function () {
            return{
                pre:function ($scope, iElement, attrs) {

                    var model = attrs.model;
                    var field = attrs.field;

                    var toBind = model + "." + field;

                    var fieldValue = AppUtil.resolveDot($scope, toBind);
                    if (fieldValue && fieldValue instanceof Object) {
                        AppUtil.putDottedValue($scope, toBind, JSON.stringify(fieldValue));
                    }

                    var border = attrs.border;
                    if (border === undefined || border == true || border == 'true') {
                        border = true
                    } else {
                        border = false;
                    }

                    var placeholder = attrs.placeholder;

                    if (placeholder === undefined || placeholder == true || placeholder == 'true') {
                        placeholder = field;
                    } else if (placeholder == false || placeholder == 'false') {
                        placeholder = '';
                    }

                    var template = "<input type='text'  ng-model='" + toBind + "' ng-model-onblur  ng-class=\"{'app-border':" + border + "}\" " +
                        "class='app-border-none app-zero-padding app-zero-margin app-height-full app-width-full'" +
                        "placeholder='" + placeholder + "'/>";

                    $(iElement).append($compile(template)($scope));
                }
            }
        }
    }
}]);


appStrapDirectives.directive("appCheckbox", ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,

        compile:function () {
            return{
                pre:function ($scope, iElement, attrs) {

                    var model = attrs.model;
                    var field = attrs.field;

                    var toBind = model + "." + field;

                    var template = "<input class='app-height-auto' style='padding: 0px;margin: 5px 0px 0px;outline: none;' type=\"checkbox\" ng-model='" + toBind + "'/>";

                    $(iElement).append($compile(template)($scope));
                }}
        }
    }
}]);

appStrapDirectives.directive("appRowAction", ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:false,
        scope:true,
        template:"<div style='padding: 5px 10px' class='app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color' ng-repeat='action in actions' ng-bind='action.label' ng-click='onRowActionClick(action)'></div>"
    }
}]);


appStrapDirectives.directive("appGrid", ["$compile", "$dataModel", "$viewStack", "$appDataSource", "$timeout", function ($compile, $dataModel, $viewStack, $appDataSource, $timeout) {

    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-grid'></div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {

                    $scope.editMode = false;
                    $scope.__selectall__ = false;

                    $scope.setEditMode = function (editMode) {
                        $scope.editMode = editMode;
                    };
                    $scope.setCurrentRow = function (row) {
                        $scope.currentRow = row;
                        $dataModel.setCurrentRow($scope[COMPONENT_ID], row);
                    }

                    $scope.rowActionButtonClick = function (row, $event) {
                        $dataModel.setCurrentRow($scope[COMPONENT_ID], row);
                        var rowActionScope = $scope.$new();
                        var modal = {
                            template:"<div><app-row-action  ng-init=\"actions=view.metadata.rowactions\"></app-row-action></div>",
                            scope:rowActionScope,
                            hideonclick:true,
                            element:$event.target
                        }
                        $viewStack.showPopup(modal);
                    };

                    $scope.onDownClick = function (row) {
                        $dataModel.moveDown(row, $scope.componentid);
                    };

                    $scope.onUpClick = function (row) {
                        $dataModel.moveUp(row, $scope.componentid);

                    };

                    $scope.toggleTree = function (row) {
                        $dataModel.toggleTree(row, $scope.componentid);

                    };
                },
                post:function ($scope, iElement) {
                    var metadata = $scope.view.metadata;

                    var prentComponentId = $scope.view[PARENT_COMPONENT_ID];
                    if (prentComponentId) {
                        $scope.view.metadata.basicAndQuickViewPosition = 'right';
                    }
                    var rowactions = [];
                    if (AppUtil.isTrueOrUndefined(metadata.enablerowaction)) {
                        rowactions.push({"type":"detail", "label":"View detail"});
                    }


                    if (!metadata.childs) {
                        metadata.childs = [];
                    }
                    var childs = metadata.childs;
                    var childCount = childs ? childs.length : 0;
                    for (var i = 0; i < childCount; i++) {
                        var child = childs[i];
                        if (child.visibility == 'Table' || child.visibility == "Both") {
                            child.type = "view";
                            rowactions.push(child);
                        }
                    }

                    //iterate for child in columns
                    var columns = metadata.columns;

                    var columnCount = columns.length;

                    for (var i = 0; i < columnCount; i++) {
                        var column = columns[i];
                        var visibility = column.visibility;
                        if (visibility == 'Child') {

                            var child = {};
                            child.viewid = column.viewid;
                            child.label = column.label;
                            child.ask = column.ask;
                            child.osk = column.osk;
                            child.type = "view";
                            var primaryCol = metadata[PRIMARY_COLUMN];
                            var dotCount = primaryCol.split(".");
                            if (primaryCol) {

                            }
                            child.relatedcolumn = metadata[PRIMARY_COLUMN];
                            child.childcolumn = column.childcolumn;
                            if (column.filter) {
                                child.filter = column.filter;
                            }
                            rowactions.push(child);
                        }


                    }


                    metadata.rowactions = rowactions;

                    var orders = $scope.view.metadata.orders;

                    if (orders && orders.length > 0) {
                        for (var i = 0; i < orders.length; i++) {
                            var order = orders[i];
                            var orderExp = Object.keys(order)[0];
                            for (var j = 0; j < columnCount; j++) {
                                var columnExp = columns[j].expression;
                                if (columns[j].type == 'index') {
                                    continue;
                                }
                                if (orderExp == columnExp) {
                                    columns[j].order = order[orderExp].$order;
                                    if (order[orderExp].$recursive) {
                                        $scope.view.metadata.$recursivecolumn = orderExp;
                                    }
                                    break;
                                }
                            }

                        }
                    }
                    var newColumns = [];

                    if (AppUtil.isTrueOrUndefined(metadata.enableselection)) {
                        var selectionColumn = {"visibility":"Table", "width":20, enableCellEdit:false, cellTemplate:"<input style='margin-left: 3px;' tabindex='-1' type='checkbox' ng-model='row.__selected__' />", "type":"selection", "tabindex":-1};
                        newColumns.push(selectionColumn);
                    }

                    if (rowactions.length > 0) {
                        var rowAction = {"visibility":"Table", "width":20, enableCellEdit:false, cellTemplate:"<div style=\"width:100%;height:20px;\" ng-click='rowActionButtonClick(row,$event)' class='app-row-action-arrow app-cursor-pointer'></div>", "tabindex":-1};
                        newColumns.push(rowAction);
                    }

                    var filters = [];
                    var appliedfilters = [];

                    var filterparameters = $scope.view.filterparameters;

                    var update = false;

                    for (var i = 0; i < columnCount; i++) {

                        var column = columns[i];
                        if (column.update) {
                            update = true;
                        }

                        if (!column.style) {
                            column.style = {};
                        }

                        if (!column.width) {
                            column.minWidth = 200;
                        }

                        if (!column.id) {
                            column.id = $scope[COMPONENT_ID] + "_" + i;
                        }

                        var visibility = column.visibility;
                        if (!(visibility == 'Table' || visibility == 'Both')) {
                            continue;
                        }
                        if (column.subvisibility && column.subvisibility == 'Embed') {
                            continue;
                        }


                        var type = column.type;

                        if (type == 'lookup' || (column.multiple && type != 'object')) {
                            var field = column.expression;
                            var lookupSource = $scope[COMPONENT_ID] + "_" + i
                            if (type != 'lookup') {
                                $scope[lookupSource] = [""];
                            } else
                            if (column.options && angular.isArray(column.options) && column.options.length > 0) {
                                $scope[lookupSource] = column.options;
                            } else {
                                column[COMPONENT_ID] = $scope[COMPONENT_ID];
                                column.ask = $scope.view.ask;
                                column.osk = $scope.view.osk;
                                $scope[lookupSource] = new $appDataSource.getLookupDataSource(column)
                                field = field + "." + column.primarycolumns[0].expression;
                            }


                            var multiple = column.multiple;
                            if (multiple === undefined) {
                                multiple = false;
                            }
                            var model = "row";
                            column.editableCellTemplate = "<app-lookup border=false model='row' field='" + field + "' datasource='" + lookupSource + "' multiple=" + multiple + "></app-lookup>";
                            var filter = angular.copy(column)
                            var expression = filter.expression;

                            if ($dataModel.isFilterApplied($scope.componentid, expression)) {
                                appliedfilters.push(filter);
                            }
                            filters.push(filter);
                        } else if (type == 'duration') {
                            column.editableCellTemplate = "<app-duration border=false model='row' field='" + column.expression + "' ></app-duration>";
                        } else if (type == CURRENCY_TYPE) {
                            column.editableCellTemplate = "<app-currency border=false model='row' field='" + column.expression + "' ></app-currency>";
                        } else if (type === 'date' || type == 'datetime') {
                            column.editableCellTemplate = "<app-datepicker border=false model='row' field='" + column.expression + "'></app-datepicker>"

                            var filter = angular.copy(column)
                            filter.type = "date";

                            var expression = filter.expression;

                            if ($dataModel.isFilterApplied($scope.componentid, expression)) {
                                appliedfilters.push(filter);
                            }
                            filters.push(filter);

                        } else if (type == 'index') {
                            column.width = 50;
                            column.label = "";
                            column.cellTemplate = "<span ng-show='$parent.$parent.$first==false' ng-click='onUpClick(row)' style='cursor: pointer;float: left;text-align: center;width: 20px'><img width='15px' height='15px' src='images/up-arrow.png'></span><span ng-show='$parent.$parent.$last==false' ng-click='onDownClick(row)' style='cursor: pointer;float: left;text-align: center;width: 20px'><img width='15px' height='15px' src='images/down_arrow_new.png'></span>";
                            column.editableCellTemplate = "<app-text model='row' field='" + column.expression + ".index' border=false></app-text>";
                        } else if (type == 'up') {
                            column.cellTemplate = "<span ng-click='onUpClick(row)' class='app-panel-arrows-parent'><img width='15px' height='15px' src='images/up-arrow.png'></span>";
                        } else if (type == 'down') {
                            column.cellTemplate = "<span ng-click='onDownClick(row)' class='app-panel-arrows-parent'><img width='15px' height='15px' src='images/down_arrow_new.png'></span>";
                        } else if (type == 'boolean') {
                            column.editableCellTemplate = "<div class='app-text-align-center app-height-full'><app-checkbox  model='row' field='" + column.expression + "' ></app-checkbox>";
                        } else if (type == 'file') {
                            column.editableCellTemplate = "<app-file-upload model='row' field='" + column.expression + "' class='app-float-left app-width-full app-height-full'></app-file-upload>";

                            column.width = 300;
                        } else if (type == 'schedule') {
                            column.editableCellTemplate = "<app-datepicker border=false model='row' field='" + column.expression + "' schedule=true></app-datepicker>"
                        } else {
                            column.editableCellTemplate = "<app-text model='row' field='" + column.expression + "' border=false></app-text>";
                        }
                        newColumns.push(column);
                    }


                    metadata.update = update;

                    var zeroWidthColumn = {editable:false, "cellTemplate":"", "tabindex":-1, style:{}, id:$scope.componentid + "_zerowidth"};

                    newColumns.push(zeroWidthColumn);
                    metadata.update = update;

                    metadata.viewfilters = filters;
                    metadata.appliedfilters = appliedfilters;
                    metadata.columns = newColumns;

                    var rowActions = metadata.rowactions;
                    if (rowActions) {
                        var rowActionCount = rowActions.length;
                        for (var i = 0; i < rowActionCount; i++) {
                            rowActions[i][USER_CURRENT_ROW] = true;
                        }
                    }

                    var id = TOOL_BAR_ID + '_' + $scope.view[COMPONENT_ID];
                    var toolBarTemplate = "<div class='app-tool-bar' app-tool-bar id='" + id + "'></div>";
                    var toolBarElement = $compile(toolBarTemplate)($scope);

                    var showAs = $scope.view[SHOW_AS];
                    if (showAs == 'popup' || metadata.embed) {
                        iElement.append(toolBarElement);
                    } else {
                        if ($scope.view[PARENT_COMPONENT_ID]) {
                            var parentId = TOOL_BAR_ID + '_' + $scope.view[PARENT_COMPONENT_ID];
                            $('#' + parentId).hide();
                        }
                        $('#' + TOOL_BAR_ID).append(toolBarElement);
                    }


                    var headerscroll = $scope.view.metadata.headerscroll;
                    var dataExpression = $scope.view.metadata.dataExpression;
                    var template;


                    if (headerscroll) {
                        $scope.view.metadata.basicAndQuickViewPosition = 'left';
                        template = "<table style='width: 100%;' cellpadding='0' cellspacing='0'>" +
                            "<thead class='applane-grid-header'>" +
                            "<th applane-grid-header-cell ng-repeat='col in view.metadata.columns'></th>" +
                            "</thead>" +
                            "<tbody class='applane-grid-body'>" +
                            "<tr style='height:25px;'  ng-class-even=\"'applane-grid-row-even'\" ng-class-odd=\"'applane-grid-row-odd'\"  ng-repeat='row in " + dataExpression + "'>" +
                            "<td applane-grid-cell class='applane-grid-cell' ng-repeat=' col in view.metadata.columns ' tabindex=0>" +
                            "</td>" +
                            "</tr>" +
                            "</tbody>" +
                            "</table>";


                    } else {
                        template = "<div class='app-container'>" +
                            "<div class='app-wrapper'><div class='app-wrapper-child'>" +
                            "<div style='display: table;table-layout:fixed;width: 100%;height: 100%;'>" +
                            "<div style='overflow: hidden;display: table-row;'>" +
                            "<div style='position: relative;width: 100%;'>" +
                            "<div style='overflow-x: hidden;left: 0px;right: 0px;' id='{{componentid}}_head' applane-grid-header></div>" +
                            "</div>" +
                            "</div>" +
                            "<div style='display: table-row;height: 100%;'>" +
                            "<div style='position: relative;height: 100%;'>" +
                            "<div style='position: absolute;top:0px;left:0px;right:0px;bottom:0px;' class='grid-scroll' applane-grid-body id='{{componentid}}_body'></div>" +
                            "</div></div></div></div></div></div>";

                    }

                    var element = $compile(template)($scope);
                    iElement.append(element);
                    if (metadata.refreshonload) {
                        $scope.refresh();
                    }


                    $timeout(function () {
                        var tableBodyId = "#" + $scope.componentid + "_body";
                        var tableHeaderId = "#" + $scope.componentid + "_head";
                        $(tableBodyId).scroll(function () {
                            $(tableHeaderId).scrollLeft($(tableBodyId).scrollLeft());
                        });
                        //check for minWidth
                        var columCount = newColumns.length;
                        for (var i = 0; i < columCount; i++) {
                            var column = newColumns[i];
                            var id = column.id;
                            var width = $("#" + id).width();

                            if (column.minWidth && width < column.minWidth) {
                                column.style.width = column.minWidth + "px";
                            }


                        }
                    }, 0);


                }
            }
        }

    }
}]);


appStrapDirectives.directive('appNavigation', ['$dataModel', '$appService' , function ($dataModel, $appService) {
    return {
        restrict:'E',
        replace:true,
        scope:true,
        template:"<div class='app-height-full app-font-weight-bold app-navigation app-border-right-white app-border-left-white app-padding-left-five-px app-padding-right-five-px'>" +
            "<div class='app-height-full app-left-arrow app-float-left app-height-twenty-px app-width-twenty-px app-cursor-pointer' ng-click='showRecords(false)'  ng-show='view.metadata.datastate.prev' ></div>" +
            "<div class='app-float-left'>{{view.metadata.datastate.fromindex +'-' + view.metadata.datastate.toindex}}</div>" +
            "<div class='app-height-full app-right-arrow app-float-left app-height-twenty-px app-width-twenty-px app-cursor-pointer' ng-click='showRecords(true)' ng-show='view.metadata.datastate.next'></div>" +
            "</div>",
        compile:function () {
            return  {

                post:function ($scope, iElement) {

                    $scope.showRecords = function (next) {
                        if (next) {
                            $dataModel.next($scope.componentid);
                        } else {
                            $dataModel.prev($scope.componentid);
                        }

                    }
                }
            };
        }
    }
}]);


appStrapDirectives.directive('appFileUpload', ['$compile', function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        compile:function () {
            return {
                pre:function ($scope, iElement, attrs) {
                    var modelExp = attrs.model;
                    var fieldExp = attrs.field;

                    var toBind = modelExp + "." + fieldExp;

                    AppUtil.rebindFieldExpression($scope, modelExp, fieldExp);
                    var model = AppUtil.getModel($scope, $scope.modelexpression, true);
                    var value = model[$scope.fieldexpression];
                    var length = value ? value.length : 0;

                    var zerolengthToBind = "!" + toBind + " || " + toBind + ".length==0";
                    var onelengthToBind = toBind + " && " + toBind + ".length>0";

                    var template = "<div>" +
                        "<input ng-show='" + zerolengthToBind + "'  style='width: 80px;outline: none;'  class='app-float-left' type='file' />" +
                        "<div ng-show='" + onelengthToBind + "' class='app-float-left' tabindex='-1'style='padding-top:3px;padding-right: 24px;'><a ng-href='http://baas.applane.com:1337/file/download?filekey={{" + toBind + "[0].key}}&ask=frontend'>{{" + toBind + "[0].name}}</a></div>" +
                        "<div  ng-show='" + zerolengthToBind + "'  class='app-float-left app-padding-top-four-px app-padding-left-ten-px'>No file Choosen</div>" +
                        "<input ng-show='" + onelengthToBind + "'  type='button' class='app-none-background app-none-border app-text-decoration-underline app-remove-file-button' value='remove' ng-click='removeFile()'/>" +
                        "</div>";


                    $(iElement).append($compile(template)($scope));

                    $(iElement).bind('change', function () {
                        $scope.$apply(function () {
                            $scope.oFReader = new FileReader();
                            $scope.oFile = angular.element(iElement).find('input')[0].files[0];
                            $scope.oFReader.onload = $scope.loadFile;
                            $scope.oFReader.readAsDataURL($scope.oFile);
                        });
                    });

                    $scope.removeFile = function () {
                        model[$scope.fieldexpression].splice(0, 1);
                        if (!$scope.$$phase) {
                            $scope.$apply();
                        }
                    };

                    $scope.showFile = function (file) {
                        $scope.fileurl = BAAS_SERVER + "/file/download?filekey=" + file[0][FILE_KEY] + "&ask=frontend";
                        $scope.filename = file[0][FILE_NAME];
                        model[$scope.fieldexpression] = file;

                        if (!$scope.$$phase) {
                            $scope.$apply();
                        }
                    };

                    $scope.loadFile = function (evt) {
                        var current_file = {};
                        current_file.name = $scope.oFile.name;
                        current_file.type = $scope.oFile.type;
                        current_file.contents = evt.target.result;
                        current_file.ask = 'frontend';
                        AppUtil.getDataFromService(BAAS_SERVER + '/file/upload', current_file, "POST", "JSON", "Uploading...", function (data) {
                            if (data != null && data != undefined && data.length > 0) {
                                $scope.showFile(data);
                            }
                        });
                    };
                }
            };
        }
    }
}]);

appStrapDirectives.directive('appNestedView', [
    '$compile', '$viewStack',
    function ($compile, $viewStack) {
        return {
            restrict:'E',
            replace:true,
            scope:true,

            compile:function () {
                return  {
                    pre:function ($scope, iElement) {

                    },
                    post:function ($scope, iElement) {
                        var column = $scope.colmetadata;
                        var currentView = $scope.view;
                        var row = $scope.row;


                        var nestedMetaData = {columns:column.columns, table:currentView.metadata.table, "type":"table"};

                        nestedMetaData.headerscroll = true;
                        nestedMetaData.embed = true;
                        nestedMetaData.enablequickviewaction = false;
                        nestedMetaData.enablemetadataaction = false;
                        nestedMetaData.delete = true;
                        nestedMetaData.insert = true;
                        nestedMetaData.close = false;
                        nestedMetaData.refresh = false;
                        nestedMetaData.save = false;
                        nestedMetaData.enableselection = true;
                        nestedMetaData.enablerowaction = false;
                        nestedMetaData.insertview = 'table';
                        nestedMetaData.resize = false;
                        nestedMetaData.label = column.label;


                        var expression = column.expression;

                        var nestedData = {};

                        var nestedModel = AppUtil.resolveDot(row, expression, true, "array");
                        nestedData.data = nestedModel;

                        var view = {"metadata":nestedMetaData, "data":nestedData};
                        view.ask = currentView.ask;
                        view.osk = currentView.osk;
                        view.applicationid = currentView.applicationid;

                        view[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                        view.element = iElement;
                        $viewStack.addView(view, $scope);

                    }

                };
            }
        };
    }
]);

appStrapDirectives.directive('appColumn', [
    '$compile',
    function ($compile) {
        return {
            restrict:'E',
            replace:true,
            scope:true,

            compile:function () {
                return  {
                    pre:function ($scope, iElement) {

                    },
                    post:function ($scope, iElement) {
                        var column = $scope.colmetadata;
                        var html = column.editableCellTemplate;
                        var cellElement = $compile(html)($scope);

                        var width = column.width;
                        if (!width) {
                            width = 200;
                        }

                        var height = column.height;


                        if (column.type != "view") {
                            $(cellElement).width(width);
                            if (height) {
                                $(cellElement).height(height);
                            }

                        }


                        iElement.append(cellElement);
                    }

                };
            }
        };
    }
]);


appStrapDirectives.directive('appNumber', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-number-container'></div>",
        compile:function () {
            return  {
                pre:function ($scope, element) {
                },
                post:function ($scope, element, attrs) {

                    var modelExpression = attrs.model;
                    var fieldExpression = attrs.field;
                    var toBind = modelExpression + "." + fieldExpression;


                    var html = "<input type='text' ng-model-onblur ng-model='" + toBind + "' ng-change='operatorValidation()' ng-click='operatorValidation()' />";
                    element.append($compile(html)($scope));


                    $scope.operatorValidation = function (event) {

                        var model = AppUtil.getModel($scope, $scope.modelexpression, true);


                        var val = model[$scope.fieldexpression];
                        if (!(val == "+" || val == "-")) {
                            if (!Number(val)) {
                                model[$scope.fieldexpression] = "";
                            }
                        }
                    }
                    /*-- for preventing the Alphabets and special characters to be printed--*/
                    element.bind("keydown", function (event) {
                            var model = AppUtil.getModel($scope, $scope.modelexpression, true);
                            var val = model[$scope.fieldexpression];
                            var i = event.which;
                            //65 - A  to  90-Z
                            if ((i >= 65 && i <= 90) || (i >= 186 && i <= 189) || (i == 111) || (i == 106) || (i >= 191 && i <= 192) || (i >= 219 && i <= 222) || (i == 32)) {
                                if (!((event.ctrlKey == true && i == 65) || (event.ctrlKey == true && i == 67) || (event.ctrlKey == true && i == 86) || (event.ctrlKey == true && i == 88))) {
                                    event.preventDefault();
                                }
                            }
                            else {
                                if (i == 110 || i == 190) {
                                    if (val.indexOf(".") != -1) {
                                        event.preventDefault();
                                    }
                                }
                                if (i == 107 || i == 109) {
                                    if (val !== undefined && val.toString().length > 0) {
                                        event.preventDefault();
                                    }
                                }

                            }
                        }
                    )
                }
            };
        }
    }
}]);


appStrapDirectives.directive('appDuration', [
    '$compile',
    function ($compile) {
        return {
            restrict:"E",
            replace:true,
            scope:true,
            template:"<div class='app-height-full'></div>",

            compile:function () {
                return {

                    pre:function ($scope, iElement, attrs) {
                        $scope.ds = ["Hrs", "Days"];

                        var modelExpression = attrs.model;
                        var fieldExpression = attrs.field;

                        var model = modelExpression + "." + fieldExpression;


                        var border = attrs.border;
                        if (border === undefined || border == true || border == 'true') {
                            border = true
                        } else {
                            border = false;
                        }

                        var template = "<div class='app-position-relative app-width-full app-height-full' ng-class=\"{'app-border':" + border + "}\">" +

                            "<app-number model='" + model + "' field='time' " +
                            "class='app-position-absolute app-height-full app-left-zero' style='right:60px;'>" +
                            "</app-number>" +

                            "<app-lookup model='" + model + "' field='timeunit' border=false datasource='ds' " +
                            "class='app-left-border app-position-absolute app-height-full app-right-zero' style='width:60px;'>" +
                            "</app-lookup>" +
                            "</div>"

                        $(iElement).append(($compile)(template)($scope));
                    }
                }
            }
        }
    }
]);

appStrapDirectives.directive('appCurrency', [
    '$compile', '$appDataSource',
    function ($compile, $appDataSource) {
        return {
            restrict:"E",
            replace:true,
            scope:true,
            template:"<div class='app-height-full'></div>",

            compile:function () {
                return {

                    pre:function ($scope, iElement, attrs) {

                        $scope.ds = new $appDataSource.getLookupDataSource({"primarycolumns":[
                            {expression:"currency"}
                        ], table:"currencies__baas", ask:"baas", "componentid":$scope[COMPONENT_ID]});
                        var modelExpression = attrs.model;
                        var fieldExpression = attrs.field;
                        var model = modelExpression + "." + fieldExpression;


                        var border = attrs.border;
                        if (border === undefined || border == true || border == 'true') {
                            border = true
                        } else {
                            border = false;
                        }

                        var template = "<div class='app-position-relative app-width-full app-height-full' ng-class=\"{'app-border':" + border + "}\">" +

                            "<app-number model='" + model + "' field='amount' " +
                            "class='app-position-absolute app-height-full app-left-zero' style='right:60px;'>" +
                            "</app-number>" +

                            "<app-lookup model='" + model + "' field='type.currency' border=false datasource='ds' " +
                            "class='app-left-border app-position-absolute app-height-full app-right-zero' style='width:60px;'>" +
                            "</app-lookup>" +
                            "</div>"

                        $(iElement).append(($compile)(template)($scope));
                    }
                }
            }
        }
    }
]);


/**
 * App Services
 */
appStrapServices.factory('$appService', [

    function () {
        var $appService = {

        };

        $appService.login = function (username, password, callback) {
            var params = {};
            params.username = username;
            params.password = password;
            params.usergroup = 'baas';

            AppUtil.getDataFromService(BAAS_SERVER + "/login", params, "POST", "JSON", "Loading...", function (callBackData) {
                callback(callBackData);
            }, function (jqxhr, error) {
                alert("Error:" + JSON.stringify(jqxhr));
            });
        };

        $appService.isHostedMode = function () {
            var href = window.location.href;

            if (href.indexOf("FrontEnd") >= 0) {
                return true;
            } else {
                return false;
            }
        }

        $appService.getData = function (query, ask, osk, bussyMessage, callBack) {
            if (!ask) {
                throw "No ask found for saving";
            }

            var data = {"query":JSON.stringify(query), "ask":ask, "osk":osk};


            var that = this;
            var url = BAAS_SERVER + "/data";
            AppUtil.getDataFromService(url, data, "POST", "JSON", bussyMessage, function (callBackData) {
                callBack(callBackData);
            });


        };
        $appService.save = function (data, ask, osk, callBack) {
            if (!ask) {
                throw "No ask found for saving";
            }

            var params = {"updates":JSON.stringify(data), "ask":ask, "osk":osk};
            var that = this;

            var url = BAAS_SERVER + "/data";
            AppUtil.getDataFromService(url, params, "POST", "JSON", "Saving...", function (callBackData) {
                callBack(callBackData);
            });


        }

        return $appService;

    }
]);


/**
 * DOM Services
 */
appStrapServices.factory('$viewStack', [
    '$rootScope',
    '$compile',
    '$http',
    '$timeout',
    '$dataModel',
    function ($rootScope, $compile, $http, $timeout, $dataModel) {
        var $viewStack = {
            "views":{},
            "popupviews":{}
        };


        $viewStack.showPopup = function (config) {
            var autoHide = config.autohide != undefined ? config.autohide : true;
            var deffered = config.deffered != undefined ? config.deffered : true;
            var modal = !autoHide;
            var escape = autoHide;
            var callBack = config.callBack;
            var position = config.position ? config.position : "center";
            var hideOnClick = config.hideonclick != undefined ? config.hideonclick : false;


            var p = new Popup({
                autoHide:autoHide,
                deffered:deffered,
                escEnabled:escape,
                hideOnClick:hideOnClick,
                html:$compile(config.template)(config.scope),
                scope:config.scope,
                element:config.element,
                position:position,
                width:config.width,
                height:config.height,
                callBack:callBack
            });
            p.showPopup();
            return p;

        }
        $viewStack.reOpen = function (v) {
            var newV = {};
            newV.viewid = v.viewid;
            newV.ask = v.ask;
            newV.osk = v.osk;
            newV.parameters = v.metadata.parameters;
            newV.filter = v.metadata.filter;
            newV[PARENT_COMPONENT_ID] = v[PARENT_COMPONENT_ID];
            var vScope = $dataModel.getScope(v[COMPONENT_ID]);
            this.addView(newV, vScope.$parent);
        }


        $viewStack.addView = function (view, $scope) {

            var componentId;
            if (view.metadata) {
                var parameters = view.metadata.parameters;
                if (!parameters) {
                    view.metadata.parameters = view.parameters;
                }

                if (!view[COMPONENT_ID]) {
                    componentId = AppUtil.getComponentId();
                } else {
                    componentId = view[COMPONENT_ID];
                }
                $viewStack.addViewToDom(view, componentId, $scope);
            } else {
                $scope.appData.currentView = view;
                if (!$scope.$$phase) {
                    $scope.$apply();
                }
            }
        }


        /***
         * Documentation Pending
         * @param view
         * @param $parentScope
         */


        $viewStack.addViewToDom = function (view, componentId, $parentScope) {

            view[COMPONENT_ID] = componentId;
            var viewInfo = {};

            viewInfo[COMPONENT_ID] = componentId;
            var parentComponentId = view[PARENT_COMPONENT_ID];
            if (parentComponentId) {
                viewInfo[PARENT_COMPONENT_ID] = parentComponentId;
                var parentViewInfo = this.views[parentComponentId];
                if (parentViewInfo[SHOW_AS] === 'popup') {
                    view[SHOW_AS] = parentViewInfo[SHOW_AS];
                }

                //check if parent has already some child or not, if yes then remove all child
                if (parentViewInfo[CHILD_COMPONENT_ID] && !view.metadata.embed) {
                    this.closeViewFromDom(parentViewInfo[CHILD_COMPONENT_ID]);
                }
                parentViewInfo[CHILD_COMPONENT_ID] = componentId;
                $dataModel.getView(parentComponentId)[CHILD_COMPONENT_ID] = componentId;
            }
            viewInfo[SHOW_AS] = view[SHOW_AS];
            var showInPopup = view[SHOW_AS] && view[SHOW_AS] === 'popup';
            if (!parentComponentId && !showInPopup) {
                this.closeAllViews();
            }
            this.views[componentId] = viewInfo;
            $dataModel.putView(componentId, view);

            var showInPopup = view[SHOW_AS] && view[SHOW_AS] === 'popup';

            if (showInPopup) {
                var scope = $parentScope.$new();
                view.metadata.resize = false;
                var modal = {
                    template:"<div style='display: table;height: 100%;width: 100%'><div  style='display: table-row;width: 100%' class='app-popup-title'>" + view.metadata.label + "</div>" +
                        "<div  style='display: table-row;height: 100%;width: 100%' class='app-popup-body'><app-view componentId='" + view[COMPONENT_ID] + "'></app-view></div></div>",
                    scope:scope,
                    autohide:false,
                    width:view.width,
                    height:view.height,
                    element:view.element
                };
                var popup = this.showPopup(modal);
                this.popupviews[view[COMPONENT_ID]] = popup;
            } else if (view.element) {
                var viewHtml = "<app-view componentId='" + view[COMPONENT_ID] + "'></app-view>";
                var viewElement = $compile(viewHtml)($parentScope);
                view.element.append(viewElement);

            } else if (parentComponentId) {

                view.metadata.fullMode = false;

                var parentViewInfo = this.views[parentComponentId];
                var superParentId = parentViewInfo[PARENT_COMPONENT_ID];
                if (superParentId) {
                    $("#" + superParentId).css({left:0, top:'0px', right:'100%', 'display':'none'});
                }

                if (!view.metadata.fullMode) {
                    $("#" + parentComponentId).css({left:0, top:'0px', right:'50%'});
                }

                var style;
                if (view.metadata.fullMode) {
                    style = "position:absolute;left:0; top:0; bottom: 0; right:0; overflow: hidden;"
                } else {
                    style = "position:absolute;left:50%; top:0; bottom: 0; right:0; overflow: hidden;>"
                }


                var template = "<div id='" + view[COMPONENT_ID] + "' class='app-view-wrapper' style='" + style + "'><app-view componentId='" + view[COMPONENT_ID] + "'></app-view></div>";

                var templateElement = $compile(template)($parentScope);
                var viewContainer = $("#view-container");
                viewContainer.append(templateElement);
                $("#" + parentComponentId).resize();
                view.metadata.resize = true;
            } else {
                //it is a root, we will not show close action here
                view.metadata.close = false;
                view.metadata.resize = false;

                var style = "position:absolute;left:0; top:0; bottom: 0; right:0; overflow: hidden;"

                var template = "<div id='" + view[COMPONENT_ID] + "' class='app-view-wrapper' style='" + style + "'><app-view componentId='" + view[COMPONENT_ID] + "'></app-view></div>"
                var templateElement = $compile(template)($parentScope);
                var viewContainer = $("#view-container");
                viewContainer.html("");
                viewContainer.append(templateElement);
                setTimeout(function () {
                    $("#" + view[COMPONENT_ID]).resize();
                }, 0);

            }
            if (!$parentScope.$$phase) {
                //$digest or $apply
                $parentScope.$apply();
            }
        }

        $viewStack.viewResize = function (componentid) {
            var view = $viewStack.views[componentid];
            if (!view) {
                alert("closeView:: View not found while closing for Id- " + componentId);
                throw "closeView:: View not found while closing for Id- " + componentId;
            }

            var parentComponentId = view[PARENT_COMPONENT_ID];
            var childComponentId = view[CHILD_COMPONENT_ID];
            var componentId = view[COMPONENT_ID];

            if (!parentComponentId && !childComponentId) {
                return;
            }

            var componentElement = $('#' + componentId);
            var childElement = $('#' + childComponentId);
            var parentElement = $('#' + parentComponentId);

            var componentRight = parseInt(componentElement.css('right'));
            var componentLeft = parseInt(componentElement.css('left'));

            if (componentLeft > 0) {
                componentElement.css({left:'0'});
                parentElement.css({left:'100%', right:'0', 'display':'none'});
            } else if (componentRight > 0) {
                componentElement.css({right:'0', left:'0'});
                childElement.css({left:'0', right:'100%', 'display':'none'});
            }

            if (componentLeft == 0 && componentRight == 0) {
                if (childComponentId && childElement.length > 0) {
                    componentElement.css({right:'50%', left:'0'});
                    childElement.css({left:'50%', right:'0', 'display':'block'});
                } else if (parentComponentId) {
                    componentElement.css({right:'0', left:'50%'});
                    parentElement.css({left:'0', right:'50%', 'display':'block'});
                }
            }
            var scope = $dataModel.getScope(componentid);
            if (scope) {
                scope.resized = !scope.resized;
            }


        }


        $viewStack.hidePopupView = function (componentId) {
            var popup = this.popupviews[componentId];
            if (!popup) {
                throw 'Popup not found for Id:' + componentId;
            }
            popup.hide();
        }

        $viewStack.closeAllViews = function () {
            var views = $viewStack.views;
            $(VIEW_CONTAINER).html("");
            for (var componentId in views) {
                if (views.hasOwnProperty(componentId)) {
                    var view = $viewStack.views[componentId];
                    if (!view) {
                        alert("closeView:: View not found while closing for Id- " + componentId);
                        throw "closeView:: View not found while closing for Id- " + componentId;
                    }
                    var showInPopup = view[SHOW_AS] && view[SHOW_AS] === 'popup';
                    if (showInPopup) {
                        this.hidePopupView(componentId);
                    }
                    $dataModel.removeView(componentId);
                    view = undefined;
                }
            }
            $viewStack.views = {};
            $('#' + TOOL_BAR_ID).html('');
        }

        $viewStack.closeViewFromDom = function (componentId) {

            var view = $viewStack.views[componentId];
            if (!view) {
                alert("closeView:: View not found while closing for Id- " + componentId);
                throw "closeView:: View not found while closing for Id- " + componentId;
            }
            if (view[CHILD_COMPONENT_ID]) {
                $viewStack.closeViewFromDom(view[CHILD_COMPONENT_ID]);
            }
            var showInPopup = view[SHOW_AS] && view[SHOW_AS] === 'popup';
            if (showInPopup) {
                this.hidePopupView(componentId);
            } else {
                $("#" + componentId).remove();
                $("#" + TOOL_BAR_ID + '_' + componentId).remove();
            }

            var parentComponentId = view[PARENT_COMPONENT_ID];
            if (parentComponentId) {
                delete $viewStack.views[parentComponentId][CHILD_COMPONENT_ID];
                $dataModel.getView(parentComponentId)[CHILD_COMPONENT_ID] = undefined;
            }
            $dataModel.removeView(componentId);
            delete $viewStack.views[componentId];
            view = undefined;
        }

        $viewStack.closeView = function (componentId) {

            var view = $viewStack.views[componentId];
            if (!view) {
                alert("closeView:: View not found while closing for Id- " + componentId);
                throw "closeView:: View not found while closing for Id- " + componentId;
            }
            var dataModelView = $dataModel.getView(componentId);
            $viewStack.closeViewFromDom(componentId);
            var showInPopup = view[SHOW_AS] && view[SHOW_AS] === 'popup';
            if (showInPopup) {
                return;
            } else if (view[PARENT_COMPONENT_ID]) {
                var parentViewComponentId = view[PARENT_COMPONENT_ID];
                var parentView = $viewStack.views[parentViewComponentId];

                var superParentComponentId = parentView[PARENT_COMPONENT_ID];
                if (superParentComponentId) {
                    var elem = $("#" + superParentComponentId);

                    if (!dataModelView.metadata.fullMode) {
                        $("#" + superParentComponentId).css({left:0, right:'50%', 'display':"block"});
                        $("#" + parentViewComponentId).css({left:'50%', right:0, 'display':"block"});
                    } else {
                        $("#" + superParentComponentId).css({left:0, right:'0', 'display':"block"});
                        $("#" + parentViewComponentId).css({left:'0', right:0, 'display':"block"});
                    }
                    $("#" + superParentComponentId).resize();
                    $("#" + parentViewComponentId).resize();
                } else {
                    $("#" + parentViewComponentId).css({left:0, right:0, 'display':"block"});
//                    var parentScope = $dataModel.getScope(parentViewComponentId);
//                    if (parentScope) {
//                        parentScope.resizeenabled = false;
//                    }
                    $("#" + parentViewComponentId).resize();
                }
                $("#" + TOOL_BAR_ID + '_' + parentViewComponentId).show();
            }
        }

        $viewStack.closeChildView = function (componentId) {
            var parentViewInfo = this.views[componentId];

            //check if parent has already some child or not, if yes then remove all child
            if (parentViewInfo && parentViewInfo[CHILD_COMPONENT_ID]) {
                this.closeView(parentViewInfo[CHILD_COMPONENT_ID]);
            }
        }
        return $viewStack;
    }
]);

/**
 * Data Model
 */
appStrapServices.factory('$dataModel', [
    '$http',
    '$timeout',
    '$rootScope',
    '$appService',


    function ($http, $timeout, $rootScope, $appService) {
        var $dataModel = {};
        $dataModel.tempKey = 0;


        $dataModel.isFilterApplied = function (componentid, expression) {
            var model = this.getModel(componentid);
            var filters = model.view.metadata.filters;
            if (!filters) {
                return false;
            }
        }


        /**
         * For update popup
         * @param id
         * @param popup
         */

        $dataModel.toggleTree = function (row, id) {

            var model = this[id];
            var data = AppUtil.resolveDot(model.scope, model.scope.view.metadata.dataExpression);
            var currentLevel = row._level;
            var currentMode = row._mode;
            var currentId = row._id;
            var visible = false;
            if (row._mode == 0) {
                row._mode = 1;
                visible = true;
            } else {
                row._mode = 0;
            }
            var idFound = false;
            var dataCount = data ? data.length : 0;
            var primaryColumn = model.view.metadata.primarycolumn;
            var key = AppUtil.resolveDot(row, model.view.metadata[PRIMARY_COLUMN]);
            var index = this.getIndex(key, id, model.view.metadata[PRIMARY_COLUMN]);
            for (var i = index + 1; i < dataCount; i++) {
                var o = data[i];
                if (o._level <= currentLevel) {
                    break;
                } else if (o._level == currentLevel + 1) {
                    if (visible) {
                        o._hidden = false;

                    } else {
                        o._hidden = true;
                        if (o._mode == 1) {
                            o._mode = 0;
                        }
                    }
                } else {
                    if (!visible) {
                        o._hidden = true;
                        if (o._mode == 1) {
                            o._mode = 0;
                        }
                    }
                }


            }


        }


        $dataModel.moveUp = function (row, id) {
            var model = this[id];

            //check index column if exists
            var columns = model.view.metadata.columnsclone;
            var columnCount = columns.length;
            var indexColumn;
            for (var i = 0; i < columnCount; i++) {
                if (columns[i].type == 'index') {
                    indexColumn = columns[i];
                }
            }
            if (!indexColumn) {
                alert("Index column is not defined")
                throw Error("Index column is not defined");
            }

            var expression = indexColumn.expression;
            var indexExp = expression + ".index";
            var subIndexExp = expression + ".subindex";
            var indexValue = AppUtil.resolveDot(row, indexExp);
            var subIndexValue = AppUtil.resolveDot(row, subIndexExp);

            var key = AppUtil.resolveDot(row, model.view.metadata[PRIMARY_COLUMN]);
            var index = this.getIndex(key, id, model.view.metadata[PRIMARY_COLUMN]);
            var prevIndex = index - 1;

            if (model == null || model == undefined) {
                alert("No view exists with id[" + id + "]");
                throw new Error("No view exists with id[" + id + "]");
            }

            var view = model.view;
            var data = AppUtil.resolveDot(model.scope, model.scope.view.metadata.dataExpression);

            var currentRow = data[index];

            if (prevIndex >= 0) {
                var previous = data[prevIndex];
                var preIndexValue = AppUtil.resolveDot(previous, indexExp);
                var preSubIndexValue = AppUtil.resolveDot(previous, subIndexExp);
                if (angular.equals(preIndexValue, indexValue)) {
                    AppUtil.putDottedValue(row, subIndexExp, preSubIndexValue);
                    AppUtil.putDottedValue(previous, subIndexExp, subIndexValue);
                } else {
                    AppUtil.putDottedValue(row, indexExp, preIndexValue);
                    AppUtil.putDottedValue(previous, indexExp, indexValue);
                }

                data[index] = previous;
                data[prevIndex] = currentRow;
            }
        }

        $dataModel.moveDown = function (row, id) {

            var model = this[id];

            //check index column if exists
            var columns = model.view.metadata.columnsclone;
            var columnCount = columns.length;
            var indexColumn;
            for (var i = 0; i < columnCount; i++) {
                if (columns[i].type == 'index') {
                    indexColumn = columns[i];
                }
            }
            if (!indexColumn) {
                alert("Index column is not defined")
                throw Error("Index column is not defined");
            }

            var expression = indexColumn.expression;
            var indexExp = expression + ".index";
            var subIndexExp = expression + ".subindex";
            var indexValue = AppUtil.resolveDot(row, indexExp);
            var subIndexValue = AppUtil.resolveDot(row, subIndexExp);

            var key = AppUtil.resolveDot(row, model.view.metadata[PRIMARY_COLUMN]);
            var index = this.getIndex(key, id, model.view.metadata[PRIMARY_COLUMN]);
            var nextIndex = index + 1;


            var view = model.view;
            var data = AppUtil.resolveDot(model.scope, model.scope.view.metadata.dataExpression);

            var currentRow = data[index];

            if (nextIndex < data.length) {
                var previous = data[nextIndex];
                var preIndexValue = AppUtil.resolveDot(previous, indexExp);
                var preSubIndexValue = AppUtil.resolveDot(previous, subIndexExp);
                if (angular.equals(preIndexValue, indexValue)) {
                    AppUtil.putDottedValue(row, subIndexExp, preSubIndexValue);
                    AppUtil.putDottedValue(previous, subIndexExp, subIndexValue);
                } else {
                    AppUtil.putDottedValue(row, indexExp, preIndexValue);
                    AppUtil.putDottedValue(previous, indexExp, indexValue);
                }

                data[index] = previous;
                data[nextIndex] = currentRow;
            }

        }


        $dataModel.getIndex = function (key, id, primaryColumn) {
            var model = this.getModel(id);
            var view = model.view;
            var data = AppUtil.resolveDot(model.scope, model.scope.view.metadata.dataExpression);

            var dataCount = data.length;
            for (var i = 0; i < dataCount; i++) {
                var record = data[i];
                var recordKey = AppUtil.resolveDot(record, primaryColumn);
                var isEqual = angular.equals(recordKey, key);
                if (isEqual) {
                    return i;
                }
            }
            return -1;
        }


        $dataModel.getTempKey = function (primaryColumn) {
            this.tempKey = this.tempKey + 1;
            var temp = {};
            AppUtil.putDottedValue(temp, primaryColumn, this.tempKey + "temp")
            return temp;

        }
        $dataModel.setScope = function (componentid, scope) {
            var model = this[componentid];
            model.scope = scope;

            var dataExpression = model.view.metadata.dataExpression;
            if (!dataExpression) {
                dataExpression = "view.data.data";
                model.view.metadata.dataExpression = dataExpression;
            }
            var dataParameterExpression = model.view.metadata.dataParameterExpression;
            if (!dataParameterExpression) {
                dataParameterExpression = "view.data.moduleresult.data";
                model.view.metadata.dataParameterExpression = dataParameterExpression;
            }
            var data = AppUtil.resolveDot(scope, dataExpression);
            var dataLength = data ? data.length : 0;
            if (!model.view.dataclone) {
                model.view.dataclone = angular.copy(data);
            }
            this.populateDataState(componentid);

            scope.$watch(dataExpression, function (newvalue, oldvalue) {
                var totalAggregates = AppUtil.resolveDot(scope, model.view.metadata.dataParameterExpression + ".aggregates");
                if (!angular.equals(newvalue, oldvalue) && totalAggregates) {

                    var columns = model.view.metadata.columnsclone;

                    var columnCount = columns.length;
                    var updates = $dataModel.getColumnUpdates(newvalue, oldvalue, columns, model.view.metadata[PRIMARY_COLUMN], true);
                    var updateCount = updates ? updates.length : 0;

                    for (var i = 0; i < updateCount; i++) {
                        var updatedRow = updates[i];
                        for (var j = 0; j < columnCount; j++) {
                            var column = columns[j];
                            var type = column.type;
                            var expression = column.expression;
                            var oldExpression = "old_" + expression;
                            var totalAggregate = column.totalaggregates;
                            var oldColumnValue = AppUtil.resolveDot(updatedRow, oldExpression);
                            var newColumnvalue = AppUtil.resolveDot(updatedRow, expression);
                            if (newColumnvalue) {

                                if (totalAggregates && totalAggregate && totalAggregate == "sum") {

                                    var conversionRate = scope.appData.conversionrate[type];
                                    var aggregateColumnValue = AppUtil.resolveDot(totalAggregates, expression);

                                    var valExp;
                                    var typeExp;
                                    var mainTypeExp;
                                    if (type == "duration") {
                                        valExp = "time";
                                        typeExp = "timeunit";
                                        mainTypeExp = "timeunit";
                                    } else if (type == "currency") {
                                        valExp = "amount";
                                        typeExp = "type.currency";
                                        mainTypeExp = "type";
                                    }
                                    var oldVal = AppUtil.resolveDot(oldColumnValue, valExp);
                                    var oldType = AppUtil.resolveDot(oldColumnValue, typeExp);
                                    var newVal = AppUtil.resolveDot(newColumnvalue, valExp);
                                    var newType = AppUtil.resolveDot(newColumnvalue, typeExp);
                                    var aggregateVal = AppUtil.resolveDot(aggregateColumnValue, valExp);
                                    var aggregateType = conversionRate.baseunit;
                                    var oldConversionRate = conversionRate.conversionvalue[oldType];
                                    var newConversionRate = conversionRate.conversionvalue[newType];
                                    var leafValue = conversionRate.leafunit;

                                    if (!aggregateVal) {
                                        aggregateVal = 0;
                                    } else {
                                        aggregateVal = aggregateVal * leafValue;
                                    }
                                    if (!oldVal || !oldConversionRate) {
                                        oldVal = 0;
                                    } else {
                                        oldVal = (oldVal * leafValue) / oldConversionRate;
                                    }
                                    if (!newVal || !newConversionRate) {
                                        newVal = 0;
                                    } else {
                                        newVal = (newVal * leafValue) / newConversionRate;
                                    }


                                    aggregateVal = aggregateVal + newVal - oldVal;
                                    aggregateVal = aggregateVal / leafValue;
                                    aggregateVal = aggregateVal.toFixed(2);
                                    var newAggregateVal = {};
                                    newAggregateVal[mainTypeExp] = aggregateType;
                                    newAggregateVal[valExp] = aggregateVal;
                                    AppUtil.putDottedValue(totalAggregates, expression, newAggregateVal);

                                }
                            }


                        }


                    }

                }
            }, true);

        }

        $dataModel.getScope = function (componentid) {
            var model = this[componentid];
            return model.scope;

        }

        $dataModel.populateDataState = function (componentid) {
            var model = this.getModel(componentid)
            var data = model.view.data;
            var metadata = model.view.metadata;

            var dataCount = data.data ? data.data.length : 0;

            var maxRow = metadata.max_rows;

            var dataState = metadata.datastate;
            if (!dataState) {
                dataState = {fromindex:0, toindex:0, prev:false, next:false, querycursor:metadata.cursor, datacursor:data.cursor}
                metadata.datastate = dataState;
            }

            var queryCursor = dataState.querycursor;
            if (!queryCursor) {
                queryCursor = 0;
            }

            var dataCursor = data.cursor;
            if (!dataCursor) {
                dataCursor = 0;
            }


            var fromIndex = dataState.fromindex;
            var toIndex = dataState.toindex;
            var prev = dataState.prev;
            var next = dataState.next;

            if (dataCount > 0) {
                fromIndex = queryCursor + 1;
                toIndex = fromIndex - 1 + dataCount;

                if (queryCursor == 0) {
                    prev = false
                } else {
                    prev = true;
                }
                if (dataCursor) {
                    next = true;
                } else {
                    next = false;
                }

            } else {
                prev = false;
                next = false;
            }

            dataState.next = next;
            dataState.prev = prev;
            dataState.fromindex = fromIndex;
            dataState.toindex = toIndex;
            dataState.querycursor = queryCursor;
            dataState.datacursor = dataCursor;
        }

        $dataModel.next = function (componentid) {
            var model = this.getModel(componentid)
            model.view.metadata.datastate.querycursor = model.view.metadata.datastate.querycursor + model.view.metadata.max_rows;
            $dataModel.refresh(componentid);
        }

        $dataModel.prev = function (componentid) {
            var model = this.getModel(componentid)
            model.view.metadata.datastate.querycursor = model.view.metadata.datastate.querycursor - model.view.metadata.max_rows;
            $dataModel.refresh(componentid);
        }

        $dataModel.putView = function (id, view) {
            var model = this[id];
            if (!model) {
                model = {};
                this[id] = model;
            }
            model.view = view;
            var primaryColumn = view.metadata[PRIMARY_COLUMN];
            if (!primaryColumn) {
                primaryColumn = KEY;
                view.metadata[PRIMARY_COLUMN] = primaryColumn;
            }

            view.metadata.columnsclone = angular.copy(view.metadata.columns);
            var selectedKeys = view.selectedkeys;
            if (selectedKeys == null || selectedKeys == undefined) {
                selectedKeys = [];
            }
            view.selectedkeys = selectedKeys;
            var filterparameters = view.filterparameters;
            if (!filterparameters) {
                filterparameters = {};
                view.filterparameters = filterparameters;
            }

        }


        $dataModel.getView = function (componentid) {
            var model = this[componentid];
            return model.view;

        }

        $dataModel.removeView = function (componentid) {
            var model = this[componentid];
            delete this[componentid];
            if (model.scope) {
                model.scope.$destroy();
            }

            model = undefined;
        }

        $dataModel.setCurrentRow = function (componentid, row) {
            var model = this[componentid];
            model.view.currentrow = row;
        };
        $dataModel.getCurrentRow = function (componentid) {
            var model = this[componentid];
            return model.view.currentrow;
        };


        $dataModel.getSelectedKeys = function (componentid) {
            var model = this[componentid];
            var selectedKeys = model.view.selectedkeys;
            if (selectedKeys == null || selectedKeys == undefined) {
                alert("selectedKeys does not exists for componentid [" + componentid + "]");
            }
            return selectedKeys;
        }

        $dataModel.getData = function (componentid, query, busyMessage, callback) {
            var model = this[componentid];
            var view = model.view;
            var applicationId = view.applicationid;
            var ask = view.ask;
            var osk = view.osk;

            var params = view.metadata.parameters;
            if (!params) {
                params = {};
            }
            var queryParams = query.parameters;
            if (!queryParams) {
                queryParams = {};
            }
            Object.keys(queryParams).forEach(function (k) {
                params[k] = queryParams[k];
            })
            query.parameters = params;
            $appService.getData(query, ask, osk, busyMessage, callback);

        }

        $dataModel.getModel = function (componentid) {
            var model = this[componentid];
            if (model == null || model == undefined) {
                alert("Model does not exists for componentid [" + componentid + "]");
                throw "Model does not exists for componentid [" + componentid + "]";
            }
            return model;
        }
        $dataModel.refresh = function (componentid, refreshCallback) {

            var model = this.getModel(componentid);

            if (model == null || model == undefined) {
                throw "No model found for refresh[" + componentid + "]";
            }

            var view = model.view;
            var ask = view.ask;
            var osk = view.osk;

            var that = this;
            var viewType = view.metadata.type;
            var parameters = view.metadata.parameters;
            var columns;
            var cursor = view.data.cursor;


            if (viewType == 'table') {
                columns = [];
                var viewColumns = view.metadata.columnsclone;
                var viewColumnCount = viewColumns ? viewColumns.length : 0;
                for (var i = 0; i < viewColumnCount; i++) {
                    var viewColumn = viewColumns[i];
                    if (viewColumn.visibility == 'Table' || viewColumn.visibility == 'Both' || viewColumn.visibility == 'Query') {
                        if (viewColumn.subvisibility && viewColumn.subvisibility == 'Embed') {
                            continue;
                        }
                        columns.push(viewColumn);
                    }

                }
            } else if (viewType == 'panel') {
                columns = [];
                var viewColumns = view.metadata.columnsclone;
                var viewColumnCount = viewColumns ? viewColumns.length : 0;
                for (var i = 0; i < viewColumnCount; i++) {
                    var viewColumn = viewColumns[i];
                    if (viewColumn.visibility == 'Panel') {
                        if (viewColumn.subvisibility && viewColumn.subvisibility == 'Embed') {
                            continue;
                        }
                        columns.push(viewColumn);
                    }

                }
            }

            var callBack = function (response) {
                try {
                    var parentComponentId = view[PARENT_COMPONENT_ID];


                    if (viewType == "panel") {
                        //We have to merge the data in table
                        //get Old Record


                        if (response && response.data && response.data.length > 0) {
                            var panelData = response.data[0];
                            var currentData = AppUtil.resolveDot(model.scope, model.scope.view.metadata.dataExpression)[0];
                            var currentDataClone = model.scope.view.dataclone[0];

                            Object.keys(panelData).forEach(function (k) {
                                $dataModel.mergeData(k, panelData[k], currentData);
                                $dataModel.mergeData(k, angular.copy(panelData[k]), currentDataClone);
                            })

                        }


                    } else {
                        view.data = response;
                        view.dataclone = angular.copy(AppUtil.resolveDot(model.scope, model.scope.view.metadata.dataExpression));
                    }

                    that.populateDataState(componentid);

                    if (refreshCallback) {
                        refreshCallback(response.data);
                    }

                    if (!model.scope.$$phase) {
                        model.scope.$apply();
                    }
                } catch (e) {
                    alert(e.message);
                }
            };

            if (columns.length > 0) {
                var query = {"keepstructure":true, "table":view.metadata.table, "columns":columns, "filter":view.metadata.filter, "parameters":parameters};
                if (view.metadata.max_rows !== undefined) {
                    query.max_rows = view.metadata.max_rows;
                }

                if (view.metadata.orders != null && view.metadata.orders != undefined) {
                    query.orders = view.metadata.orders;
                }

                query.cursor = view.metadata.datastate.querycursor;


                $appService.getData(query, ask, osk, "Loading...", callBack);
            } else {
                callBack();
            }


        }

        $dataModel.mergeData = function (key, value, record) {
            var recordValue = record[key];
            if (recordValue === undefined) {
                record[key] = value;
            } else if ((recordValue instanceof Array) && (value instanceof Array)) {
                recordValue.splice(0);
                for (var i = 0; i < value.length; i++) {
                    recordValue.push(value[i]);
                }
            } else if ((recordValue instanceof Object) && (value instanceof Object)) {
                Object.keys(value).forEach(function (k) {
                    $dataModel.mergeData(k, value[k], recordValue);
                })
            } else {
                record[key] = value;
            }
        }

        $dataModel.insert = function (componentid) {
            var model = this[componentid];
            var tempKey = this.getTempKey(model.view.metadata[PRIMARY_COLUMN]);
            var data = AppUtil.resolveDot(model.scope, model.scope.view.metadata.dataExpression);
            data.splice(0, 0, tempKey);
            model.view.dataclone.splice(0, 0, angular.copy(tempKey));
        }

        $dataModel.deleteData = function (componentid) {
            var model = this[componentid];
            var view = model.view;
            var data = AppUtil.resolveDot(model.scope, model.scope.view.metadata.dataExpression);
            var primaryColumn = view.metadata[PRIMARY_COLUMN];

            var selectedItems = this.getSelectedKeys(componentid);

            var selectedCount = selectedItems.length;
            if (selectedCount > 0) {
                var deletedItems = [];

                for (var i = 0; i < selectedCount; i++) {
                    var selectedItem = selectedItems[i];
                    var key = AppUtil.resolveDot(selectedItem, primaryColumn);
                    var dataIndex = $dataModel.getIndex(key, componentid, primaryColumn);
                    if (dataIndex < 0) {
                        alert("No row exists for delete");
                        throw new Error("No row exists for delete");
                    }
                    data.splice(dataIndex, 1);
                    var deletedOperation = {};
                    deletedOperation[primaryColumn] = key;
                    var dottedIndex = primaryColumn.lastIndexOf(".");
                    var operationPrefix = "";
                    if (dottedIndex >= 0) {
                        operationPrefix = primaryColumn.substring(0, dottedIndex) + ".";
                    }
                    operationPrefix += "__type__";
                    deletedOperation[operationPrefix] = "delete";
                    deletedItems.push(deletedOperation);
                }

                //empty selected items
                selectedItems.splice(0);
                if (view.metadata.embed) {
                    if (!model.scope.$$phase) {
                        model.scope.$apply();
                    }
                } else {
                    var that = this;
                    var callBack = function (data) {
                        that.refresh(componentid);
                    };
                    $appService.save({"table":view.metadata.table, "filter":view.metadata.filter, "parameters":view.metadata.parameters, "operations":deletedItems}, view.ask, view.osk, callBack);
                }

            } else {
                alert("No row found for delete");
            }
        }


        $dataModel.addFilter = function (componentId, pFilterExp, pParamValue) {
            var model = this.getModel(componentId);
            var view = model.view;
            var metadata = view.metadata;
            var filter = metadata.filter;

            if (AppUtil.isNullOrUndefined(filter)) {
                filter = {};
            }

            metadata.filter = filter;
            if (pParamValue === undefined) {
                delete filter[pFilterExp];
            } else {
                filter[pFilterExp] = pParamValue;
            }


        };

        $dataModel.isTempKey = function (id) {
            var dottedPattern = /.+(temp)$/;
            return (dottedPattern.test(id));
        }

        $dataModel.save = function (componentid, saveCallBack) {


            var model = this.getModel(componentid);
            var view = model.view;
            var ask = view.ask;
            var osk = view.osk;

            var newInserts = $dataModel.getUpdates(componentid);

            var newInsertsCount = newInserts.length;
            if (newInsertsCount > 0) {

                var that = this;
                var callBack = function (data) {

                    that.refresh(componentid, function (data) {
                        if (saveCallBack) {
                            saveCallBack(data);
                        }
                    });
                };
                $appService.save({"table":view.metadata.table, "filter":view.metadata.filter, "parameters":view.metadata.parameters, "operations":newInserts}, ask, osk, callBack);
            } else {
                alert("No data found for saving");
            }

        }

        $dataModel.getColumnUpdates = function (data, cloneData, columns, primaryColumn, requireOldValue) {
            if (!primaryColumn) {
                throw new Error("Primary column not defined");
            }


            var noOfColumns = columns.length;

            var newInserts = [];
            var noOfDataRecords = data.length;
            for (var i = 0; i < noOfDataRecords; i++) {
                var record = data[i];
                var key = AppUtil.resolveDot(record, primaryColumn);
                var cloneRecord = AppUtil.getRecord(cloneData, key, primaryColumn);
                var hasUpdates = false;

                if (cloneRecord == null) {
                    // case of insert
                    cloneRecord = {};
                }
                var updates = {};

                for (var j = 0; j < noOfColumns; j++) {
                    var column = columns[j];
                    var expression = column.expression;
                    var type = column.type;
                    var multiple = column.multiple;


                    var oldValue = AppUtil.resolveDot(cloneRecord, expression);
                    var newValue = AppUtil.resolveDot(record, expression);

                    if (!angular.equals(oldValue, newValue)) {
                        //clone newValue so that changes in this will nt reflect in data
                        if (newValue) {
                            newValue = angular.copy(newValue);
                        }


                        if (type == LOOK_UP_TYPE) {

                            if (multiple) {
                                newValue = {'data':newValue, "override":true};
                            }
                        } else if (type == STRING_TYPE && column.multiple) {
                            if (!(newValue instanceof Array)) {
                                if (newValue.toString().trim().length > 0) {
                                    newValue = JSON.parse(newValue.toString().trim());
                                } else {
                                    newValue = undefined
                                }

                            }

                        } else if (type == DATE_TYPE || type == DATE_TIME_TYPE) {
                            if (newValue.indexOf("/") >= 0) {
                                var splitValue = newValue.split("/")
                                newValue = splitValue[2] + "-" + splitValue[1] + "-" + splitValue[0];
                            }

                        } else if (type == SCHEDULE_TYPE) {
                            var dueDate = AppUtil.resolveDot(newValue, "duedate");
                            if (dueDate && dueDate.indexOf("/") >= 0) {
                                var splitValue = dueDate.split("/")
                                dueDate = splitValue[2] + "-" + splitValue[1] + "-" + splitValue[0];
                                AppUtil.putDottedValue(newValue, "duedate", dueDate);
                            }

                        } else if (type == 'object') {
                            if (newValue) {
                                if (multiple && angular.isArray(newValue)) {
                                    newValue = $dataModel.getColumnUpdates(newValue, oldValue, column.columns, KEY)

                                } else if (!(angular.isArray(newValue) || angular.isObject(newValue)) && newValue.toString().trim().length > 0) {
                                    newValue = JSON.parse(newValue);
                                }
                                //check if newValue and oldValue are same
                                if (angular.equals(newValue, oldValue)) {
                                    newValue = undefined;
                                }
                            }
                        }

                        if (newValue !== undefined) {
                            hasUpdates = true;
                            updates[expression] = newValue;
                            if (requireOldValue) {
                                updates["old_" + expression] = oldValue;
                            }
                            //check for dotted expression
                            var dottexExpression = expression;
                            var dottIndex = dottexExpression.indexOf(".");
                            var dottedConcatExpression = "";
                            while (dottIndex >= 0) {
                                //we have to send _id value along with updates
                                var firstExpression = dottexExpression.substring(0, dottIndex);
                                var firstIndex = dottedConcatExpression + firstExpression + "._id";

                                var updatedFirstIndex = AppUtil.resolveDot(updates, firstIndex);
                                var oldRecordFirstIndex = AppUtil.resolveDot(cloneRecord, firstIndex);
                                var currentRecordFirstIndex = AppUtil.resolveDot(record, firstIndex);

                                if (!updatedFirstIndex) {
                                    if (currentRecordFirstIndex) {
                                        updates[firstIndex] = currentRecordFirstIndex;
                                    } else if (oldRecordFirstIndex) {
                                        updates[firstIndex] = oldRecordFirstIndex;
                                    }
                                }
                                dottexExpression = dottexExpression.substring(dottIndex + 1);
                                dottIndex = dottexExpression.indexOf(".");
                                dottedConcatExpression = dottedConcatExpression + firstExpression + ".";

                            }
                        }
                    }
                }


                if (hasUpdates) {
                    updates[primaryColumn] = key;
                    var primaryColumnValue = updates[primaryColumn];
                    if (primaryColumnValue && /.+(temp)$/.test(primaryColumnValue)) {
                        delete updates[primaryColumn];
                        updates[primaryColumn + "__temp"] = primaryColumnValue;
                    }
                    newInserts.push(updates);
                }

            }

            //check for delete
            if (cloneData) {
                var cloneRecordsCount = cloneData.length;
                for (var i = 0; i < cloneRecordsCount; i++) {
                    var record = cloneData[i];
                    var key = AppUtil.resolveDot(record, primaryColumn);
                    var dataRecord = AppUtil.getRecord(data, key, primaryColumn);
                    if (dataRecord == null && key && key.indexOf("temp") < 0) {
                        //case of delete
                        var deletedOperation = {};
                        deletedOperation[primaryColumn] = key;
                        var dottedIndex = primaryColumn.lastIndexOf(".");
                        var operationPrefix = "";
                        if (dottedIndex >= 0) {
                            operationPrefix = primaryColumn.substring(0, dottedIndex) + ".";
                        }
                        operationPrefix += "__type__";
                        deletedOperation[operationPrefix] = "delete";
                        newInserts.push(deletedOperation);
                    }

                }
            }

            return newInserts;

        }

        $dataModel.getUpdates = function (componentid) {
            var model = this.getModel(componentid);
            var view = model.view;


            var cloneData = view.dataclone;
            var data = AppUtil.resolveDot(model.scope, model.scope.view.metadata.dataExpression);

            var primaryColumn = view.metadata[PRIMARY_COLUMN];

            var columns = model.view.metadata.columnsclone;
            return $dataModel.getColumnUpdates(data, cloneData, columns, primaryColumn);
        }


//        $dataModel.updateColumnMetadata = function (componentid, columns) {
//            if (!columns) {
//                var model = this.getModel(componentid);
//                columns = model.view.metadata.columns;
//            }
//
//            var columnsLength = columns ? columns.length : 0;
//
//            for (var i = 0; i < columnsLength; i++) {
//                var column = columns[i];
//                var type = column.type;
//                var exp = column.expression;
//
//                if (type == LOOK_UP_TYPE) {
//                    if (!column.primarycolumns) {
//                        column.primarycolumns = [
//                            {expression:"_id", "type":"string"}
//                        ];
//                    }
//                    var lookupColumnCount = column.primarycolumns.length;
//                    var newLookupColumns = [];
//                    for (var j = 0; j < lookupColumnCount; j++) {
//                        var c = column.primarycolumns[j];
//                        if (c.type == 'lookup') {
//                            continue;
//                        }
//                        newLookupColumns.push(c);
//                    }
//                    if (newLookupColumns.length == 0) {
//                        alert("No simple column exists in [" + column.expression + "]");
//                        throw new Error("No simple column exists in [" + column.expression + "]");
//                    }
//                    column.primarycolumns = newLookupColumns;
//
//                } else if (type == 'view' && column.columns) {
//                    this.updateColumnMetadata(componentid, column.columns);
//                }
//            }
//        }

        return $dataModel;

    }
]);

appStrapDirectives.directive("appDatepicker", ["$compile" , '$viewStack', function ($compile, $viewStack) {
    return {
        restrict:'E',
        template:"<div class='app-height-full app-width-full'></div>",
        scope:true,
        replace:true,

        compile:function () {
            return{
                post:function ($scope, iElement, attrs) {
                    if (!attrs.format) {
                        attrs.format = 'dd/mm/yyyy';
                    }

                    var showWhenComponent = attrs.schedule;
                    var modelExpression = attrs.model;
                    var fieldExpression = attrs.field;
                    $scope.schedule = false;


                    var toBind = modelExpression + "." + fieldExpression;
                    var placeholder = attrs.placeholder;

                    if (placeholder) {
                        // do nothing
                    } else if (placeholder === undefined || placeholder == true || placeholder == 'true') {
                        placeholder = fieldExpression;
                    } else {
                        placeholder = '';
                    }

                    if (showWhenComponent || showWhenComponent === 'true') {
                        toBind += '.duedate';
                        placeholder = 'duedate';
                    }

                    AppUtil.rebindFieldExpression($scope, modelExpression, fieldExpression);


                    var border = attrs.border;
                    if (border === undefined || border == true || border == 'true') {
                        border = true
                    } else {
                        border = false;
                    }

                    $scope.model = attrs.model;
                    $scope.field = attrs.field;
                    $scope.borderonpopup = attrs.borderonpopup;

                    var dateTemplate = "<div class='app-grid-datepicker-parent app-height-full app-width-full app-min-height' ng-class=\"{'app-border':" + border + "}\">" +
                        '<input type="text" class="app-grid-date-picker-input" ng-model="' + toBind + '" placeholder="' + placeholder + '" >' +
                        '<input type="text" ng-show="' + showWhenComponent + '" class="app-schedule-image" tabindex="-1" ng-click="schedule = !schedule"/>' +
                        '<input type="text" data-toggle="datepicker" class="app-grid-date-picker-calender-image" tabindex="-1" ng-click="schedule = false"/>' +
                        '</div>';
                    if (showWhenComponent || showWhenComponent === 'true') {
                        dateTemplate += '<app-schedule class="app-float-left app-position-relative app-width-full" ng-show="schedule" ></app-schedule>';
                    }


                    $(iElement).append($compile(dateTemplate)($scope));

                    var dateInputElement = angular.element(iElement).find('input');
                    dateInputElement = dateInputElement[0];
                    dateInputElement = $(dateInputElement);


                    dateInputElement.datepicker({autoclose:true, format:attrs.format});
                    dateInputElement.on('changeDate', function (e) {
                        var model = $scope.modelexpression;
                        var field = $scope.fieldexpression;
                        if (showWhenComponent || showWhenComponent === 'true') {
                            model = model + "." + field;
                            field = "duedate";
                        }

                        var model = AppUtil.getModel($scope, model, true);
                        model[field] = e.date.getFormattedDate(attrs.format);

                        if (!$scope.$$phase) {
                            $scope.$apply();
                        }
                    });
                }
            }
        }
    }
}]);


appStrapDirectives.directive('appSchedule', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        compile:function () {
            return  {
                post:function ($scope, iElement) {
                    var model = $scope.model;
                    var field = $scope.field;

                    var frequency = 'frequency';
                    var repeatedOn = 'repeatedon';
                    var time = 'time';

                    var componentModel = model + "." + field;
                    var spanField = "span";

                    $scope.spanOptions = ["None", "Daily", "Weekly", "Monthly", "Yearly", "Hourly"];
                    $scope.frequency = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31"];

                    var spanValue = componentModel + "." + spanField;

                    $scope.$watch(spanValue, function (newValue, oldValue) {
                        if (!newValue) {
                            return;
                        }

                        if (newValue == 'Weekly') {
                            $scope.options = ["Mon", "Tue", "Wed", "Thr", "Fri", "Sat", "Sun"];
                        } else if (newValue == 'Yearly') {
                            $scope.options = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                        } else if (newValue == 'Monthly') {
                            $scope.options = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31"];
                        }
                    }, true);


                    var border = $scope.borderonpopup;
                    if (border === undefined || border == true || border == 'true') {
                        border = true
                    } else {
                        border = false;
                    }

                    var template = "<div style='top:2px;z-index:1;' class='app-float-left app-white-backgroud-color app-schedule app-padding-five-px app-position-absolute' ng-class=\"{'app-border':" + border + "}\">" +

                        "<div class='app-float-left app-width-full app-padding-top-bottom-five-px'>" +
                        "<app-lookup style='width:200px;height: 30px;'" +
                        "model='" + componentModel + "' field='" + spanField + "' dataSource='spanOptions' border=true placeholder=\'Repeat\'></app-lookup>" +
                        "</div>" +

                        "<div class='app-float-left app-width-full app-padding-top-bottom-five-px' ng-show='" + spanValue + " != \"None\"'>" +
                        "<app-time model='" + componentModel + "' field='" + time + "' ></app-time>" +
                        "</div>" +

                        "<div class='app-float-left app-width-full app-padding-top-bottom-five-px' ng-show='" + spanValue + " == \"Yearly\" || " + spanValue + " == \"Monthly\" || " + spanValue + " == \"Weekly\"'>" +
                        "<app-lookup model='" + componentModel + "' field='" + repeatedOn + "' dataSource='options' multiple=true placeholder=\'Repeat On\'></app-lookup>" +
                        "</div>" +

                        "<div class='app-float-left app-width-full app-padding-top-bottom-five-px' ng-show='" + spanValue + " == \"Yearly\" || " + spanValue + " == \"Monthly\" || " + spanValue + " == \"Weekly\" || " + spanValue + " == \"Hourly\"|| " + spanValue + " == \"Daily\"'>" +
                        "<app-datepicker model='" + componentModel + "' field='duedate' ></app-datepicker>" +
                        "</div>" +

                        "</div>";

                    $(iElement).append($compile(template)($scope))
                }
            }
        }
    }
}]);

appStrapDirectives.directive('appFilter', ["$compile", "$appDataSource", function ($compile, $appDataSource) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        compile:function () {
            return  {
                post:function ($scope, iElement) {

                    var column = $scope.column;
                    var columnType = column.type;
                    var field = column.expression;

                    if (columnType == 'lookup') {
                        var lookupSource = $scope[COMPONENT_ID] + "_" + field;
                        if (column.options && angular.isArray(column.options) && column.options.length > 0) {
                            $scope[lookupSource] = column.options;
                        } else {
                            column[COMPONENT_ID] = $scope[COMPONENT_ID];
                            column.ask = $scope.view.ask;
                            column.osk = $scope.view.osk;
                            $scope[lookupSource] = new $appDataSource.getLookupDataSource(column)
                            var lookUpField = field + "." + column.primarycolumns[0].expression;
                        }
                        column.multiple = false;

                        filterTemplate = "<app-lookup class='app-float-left app-filter-view-margin' style='width: 235px;height: 30px;' datasource='" + lookupSource + "' model='view.filterparameters' field='" + lookUpField + "' multiple=false placeholder = '" + column.label + "'></app-lookup>";

                    } else if (column.type == 'date') {
                        filterTemplate = "<app-date-filter class='app-float-left app-filter-view-margin' style='width: 235px;height: 30px;' model='view.filterparameters' field='" + field + "' placeholder = '" + column.label + "'></app-date-filter>";
                    } else if (column.type == 'datefilter') {
//                        filterTemplate = "<app-date-filter class='app-float-left app-filter-view-margin' style='width: 235px;height: 30px;' model='view.filterparameters' field='" + field + "' placeholder = '" + column.label + "' schedule=true></app-date-filter>";
                    }

                    $(iElement).append($compile(filterTemplate)($scope))
                }
            }
        }
    }
}]);


appStrapDirectives.directive('appDateFilter', ["$compile", '$viewStack' , function ($compile, $viewStack) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        compile:function () {
            return  {
                post:function ($scope, iElement, attrs) {
                    if (!attrs.format) {
                        attrs.format = 'dd/mm/yyyy';
                    }
                    var model = attrs.model;
                    var field = attrs.field;
                    var toBind = model + "." + field;

//                    var schedule = attrs.schedule;
//                    if(schedule == true || schedule == 'true'){
//                        toBind += '.duedate';
//                    }
                    AppUtil.rebindFieldExpression($scope, model, field);

                    var placeholder = attrs.placeholder;
                    if (!placeholder) {
                        placeholder = '';
                    }

                    var template = "<div class='input-append date datepicker app-text-box-border app-zero-border-radius app-zero-padding app-height-full' style='position: relative;'>" +
                        '<span class="app-date-filter-left-arrow app-vertical-align-middle app-cursor-pointer" ng-click="dateFilterNavigation(false)"></span>' +
                        "<input ng-click='showPopUp()'  ng-model='" + toBind + ".label' type='text' class='calender-input date-input' style='height: 100%;left: 21px;padding: 0;position: absolute;right: 26px;' placeholder='" + placeholder + "'/>" +
                        '<span class="app-date-filter-right-arrow app-vertical-align-middle app-cursor-pointer" ng-click="dateFilterNavigation(true)"></span>' +
                        '<input type="text" data-toggle="datepicker" class="app-grid-date-picker-calender-image app-position-absolute" tabindex="-1" style="right: 4px;"/>' +
                        "</div>";

                    $(iElement).append($compile(template)($scope));

                    var dateInputElement = angular.element(iElement).find('input');
                    dateInputElement = dateInputElement[0];
                    dateInputElement = $(dateInputElement);


                    dateInputElement.datepicker({autoclose:true, format:attrs.format});
                    dateInputElement.on('changeDate', function (e) {
                        $scope.currentDate = new Date(e.date);
                        $scope.dateSelected = true;
                        $scope.getFirstAndNextDate();
                    })

                    dateInputElement.on('keyup', function (e) {
                        var value = dateInputElement.val();
                        if (!value) {
                            var modelExp = $scope.modelexpression;
                            var fieldExp = $scope.fieldexpression;
//                            if (schedule || schedule === 'true') {
//                                modelExp = modelExp + "." + fieldExp;
//                                fieldExp = "duedate";
//                            }

                            var model = AppUtil.getModel($scope, model, true);
                            model[fieldExp] = undefined;
                        }
                    })


                    if ($scope.view.metadata.firstday) {
                        $scope.currentDate = new Date($scope.view.metadata.firstday);
                    } else {
                        $scope.currentDate = new Date();
                    }


                    $scope.currentYear = $scope.currentDate.getFullYear();
                    $scope.currentMonth = $scope.currentDate.getMonth();
                    $scope.months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

                    var selected = $scope.view.metadata.selectedDateFilter;
                    if (selected == 'date') {
                        $scope.dateSelected = true;
                    } else if (selected == 'year') {
                        $scope.yearSelected = true;
                    } else if (selected == 'week') {
                        $scope.weekSelected = true;
                    } else if (selected == 'month') {
                        $scope.monthSelected = true;
                    }

                    $scope.weekFilter = function () {
                        $scope.view.metadata.selectedDateFilter = 'week';
                        $scope.weekSelected = true;
                        $scope.yearSelected = false;
                        $scope.monthSelected = false;
                        $scope.dateSelected = false;

                        $scope.currentDate = new Date();

                        var first = $scope.currentDate.getDate() - $scope.currentDate.getDay();
                        var last = first + 6;
                        $scope.weekFirstDay = new Date($scope.currentDate);
                        $scope.weekFirstDay.setDate(first)
                        $scope.weekLastDay = new Date($scope.currentDate);
                        $scope.weekLastDay.setDate(last)


                        $scope.firstDay = $scope.weekFirstDay.getFormattedDate(attrs.format);
                        var nextWeekObj = new Date($scope.weekLastDay.getFullYear(), $scope.weekLastDay.getMonth(), $scope.weekLastDay.getDate() + 1);
                        $scope.lastDay = nextWeekObj.getFormattedDate(attrs.format);
                        var value = $scope.weekFirstDay.getFormattedDate(attrs.format) + ' - ' + $scope.weekLastDay.getFormattedDate(attrs.format);
                        dateInputElement.val(value);
                        $scope.setValue(value);
                    }

                    $scope.setValue = function (val) {


                        var modelExp = $scope.modelexpression;
                        var fieldExp = $scope.fieldexpression;
//                        if (schedule || schedule === 'true') {
//                            modelExp = modelExp + "." + fieldExp;
//                            fieldExp = "duedate";
//                        }

                        var model = AppUtil.getModel($scope, modelExp, true);

                        model[fieldExp] = {};
                        var firstSplit = $scope.firstDay.split("/");
                        model[fieldExp][GREATER_EQ] = firstSplit[2] + "-" + firstSplit[1] + "-" + firstSplit[0];
                        var secondSplit = $scope.lastDay.split("/");
                        model[fieldExp][LESS_THEN] = secondSplit[2] + "-" + secondSplit[1] + "-" + secondSplit[0];
                        model[fieldExp].label = val
                        $scope.view.metadata.firstday = model[fieldExp][GREATER_EQ];
                        $scope.hideDateFilterPopUp();
                    }

                    $scope.yearFilter = function () {
                        $scope.view.metadata.selectedDateFilter = 'year';
                        $scope.yearSelected = true;
                        $scope.monthSelected = false;
                        $scope.dateSelected = false;
                        $scope.weekSelected = false;

                        $scope.currentDate = new Date();
                        $scope.currentYear = $scope.currentDate.getFullYear();
                        dateInputElement.val($scope.currentYear);
                        $scope.getFirstAndNextDate($scope.currentYear);
                    }

                    $scope.monthFilter = function () {
                        $scope.view.metadata.selectedDateFilter = 'month';
                        $scope.monthSelected = true;
                        $scope.dateSelected = false;
                        $scope.yearSelected = false;
                        $scope.weekSelected = false

                        $scope.currentDate = new Date();
                        $scope.currentMonth = $scope.currentDate.getMonth();

                        var currentMonthFirstDay = new Date($scope.currentDate.getFullYear(), $scope.currentMonth, 1);
                        var nextMonthFirstDay = new Date($scope.currentDate.getFullYear(), $scope.currentMonth + 1, 1);
                        $scope.firstDay = currentMonthFirstDay.getFormattedDate(attrs.format);
                        $scope.lastDay = nextMonthFirstDay.getFormattedDate(attrs.format);
                        var value = $scope.months[currentMonthFirstDay.getMonth()] + ', ' + $scope.currentYear;
                        dateInputElement.val(value);
                        $scope.setValue(value);
                    }

                    $scope.getFirstAndNextDate = function (val) {
                        if ($scope.dateSelected) {
                            $scope.firstDay = $scope.currentDate.getFormattedDate(attrs.format);
                            val = $scope.firstDay;
                            var nextDate = new Date($scope.currentDate.getFullYear(), $scope.currentDate.getMonth(), $scope.currentDate.getDate() + 1);
                            $scope.lastDay = nextDate.getFormattedDate(attrs.format);
                        }
                        if ($scope.yearSelected) {
                            $scope.firstDay = "01/01/" + $scope.currentYear;
                            var next = new Date(($scope.currentYear + 1), $scope.currentMonth, $scope.currentDate.getDate());
                            $scope.lastDay = "01/01/" + next.getFullYear();
                        }

                        $scope.setValue(val);
                    }

                    $scope.dateFilter = function () {
                        $scope.view.metadata.selectedDateFilter = 'date';
                        $scope.dateSelected = true;
                        $scope.yearSelected = false;
                        $scope.monthSelected = false;
                        $scope.weekSelected = false;

                        $scope.currentDate = new Date();
                        var value = $scope.currentDate.getFormattedDate(attrs.format);
                        dateInputElement.val(value);
                        $scope.getFirstAndNextDate(value);
                    }
                    $scope.fromtopopup = false;
                    $scope.ndayspopup = false;

                    $scope.showPopUp = function () {

                        var html = "<ul>" +
                            "<li class='app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color app-padding-five-px' ng-click='dateFilter()' >Date</li>" +
                            "<li class='app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color app-padding-five-px' ng-click='monthFilter()'>Month</li>" +
                            "<li class='app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color app-padding-five-px' ng-click='yearFilter()' >Year</li>" +
                            "<li class='app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color app-padding-five-px' ng-click='weekFilter()' >Week</li>" +

                            "<li ng-click='fromtopopup = !fromtopopup;ndayspopup = false; ' class='app-position-relative app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color app-padding-five-px' >Custom</li>" +
                            "<li ng-show='fromtopopup' class='app-position-relative ng-binding app-padding-five-px app-zero-padding' >" +
                            '<ul  class="app-pop-up-border app-pop-up-box-shadow app-position-absolute app-light-gray-backgroud-color app-padding-five-px app-cursor-auto" style="top: 0px; width: 362px;z-index:1;">' +
                            '<li>' +
                            "<app-datepicker model='" + toBind + "' field='fromlabel' placeholder='From' class='app-float-left' style='width:150px;margin-right:5px;'></app-datepicker>" +
                            "<app-datepicker model='" + toBind + "' field='tolabel' placeholder='To' class='app-float-left' style='width:150px;'></app-datepicker>" +
                            '<div class="app-float-left app-cursor-pointer app-color-black" style="margin-left:12px;margin-top:4px;" ng-click="rengeFilter()">Apply</div></li>' +
                            '</ul>' +

                            "<li ng-click='ndayspopup = !ndayspopup; fromtopopup = false' class='app-position-relative app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color app-padding-five-px' >Last N Days</li>" +
                            "<li ng-show='ndayspopup' class='app-position-relative ng-binding app-padding-five-px app-zero-padding' >" +
                            '<ul  class="app-pop-up-border app-pop-up-box-shadow app-position-absolute app-light-gray-backgroud-color app-padding-five-px app-cursor-auto" style="top: 0px; width: 200px;z-index:1;">' +
                            '<li>' +
                            "<app-text model='" + toBind + "' field='nLastDays' class='app-float-left' style='width:150px; height:24px;' placeholder='Days'></app-text>" +
                            '<div class="app-float-left app-cursor-pointer app-color-black" style="margin-left:12px;margin-top:4px;" ng-click="nLastDaysFilter()">Apply</div></li>' +
                            '</ul>' +


                            "</li>" +

                            "</ul>";

                        var popup = {
                            template:html,
                            scope:$scope.$new(),
                            hideonclick:false,
                            element:dateInputElement,
                            width:100
                        }
                        $scope.dateFilterPopUp = $viewStack.showPopup(popup);

                    }

                    $scope.nLastDaysFilter = function () {
                        $scope.hideDateFilterPopUp();
                        var modelExp = $scope.modelexpression;
                        var fieldExp = $scope.fieldexpression;
//                        if (schedule || schedule === 'true') {
//                            modelExp = modelExp + "." + fieldExp;
//                            fieldExp = "duedate";
//                        }
                        var model = AppUtil.getModel($scope, modelExp, true);
                        if (model && model[fieldExp]) {
                            var days = model[fieldExp].nLastDays;
                            model[fieldExp] = {};
                            var dateObj = angular.copy($scope.currentDate);
                            console.log(JSON.stringify(dateObj));
                            console.log(JSON.stringify($scope.currentDate));
                            dateObj = new Date(dateObj.setDate(dateObj.getDate() - days));
                            var firstSplit = dateObj.getFormattedDate(attrs.format).split("/");
                            model[fieldExp][GREATER_EQ] = firstSplit[2] + "-" + firstSplit[1] + "-" + firstSplit[0];
                            model[fieldExp].label = dateObj.getFormattedDate(attrs.format) + ' - ' + $scope.currentDate.getFormattedDate(attrs.format);
                        }
                    }

                    $scope.hideDateFilterPopUp = function () {
                        if ($scope.dateFilterPopUp) {
                            $scope.dateFilterPopUp.hide();
                            delete $scope.dateFilterPopUp;
                        }
                    }
                    $scope.rengeFilter = function () {
                        $scope.hideDateFilterPopUp();
                        var modelExp = $scope.modelexpression;
                        var fieldExp = $scope.fieldexpression;
//                        if (schedule || schedule === 'true') {
//                            modelExp = modelExp + "." + fieldExp;
//                            fieldExp = "duedate";
//                        }
                        var model = AppUtil.getModel($scope, modelExp, true);

                        if (model && model[fieldExp]) {
                            $scope.firstDay = model[fieldExp].fromlabel;
                            $scope.lastDay = model[fieldExp].tolabel;
                        }

                        model[fieldExp] = {};

                        if ($scope.firstDay) {
                            var firstSplit = $scope.firstDay.split("/");
                            model[fieldExp][GREATER_EQ] = firstSplit[2] + "-" + firstSplit[1] + "-" + firstSplit[0];
                            model[fieldExp].label = $scope.firstDay;
                            $scope.view.metadata.firstday = model[fieldExp][GREATER_EQ];
                        }

                        if ($scope.lastDay) {
                            var secondSplit = $scope.lastDay.split("/");
                            model[fieldExp][LESS_THEN] = secondSplit[2] + "-" + secondSplit[1] + "-" + secondSplit[0];
                            model[fieldExp].label = $scope.lastDay;
                            $scope.view.metadata.firstday = model[fieldExp][LESS_THEN];
                        }

                        if ($scope.firstDay && $scope.lastDay) {
                            model[fieldExp].label = $scope.firstDay + " - " + $scope.lastDay
                            $scope.view.metadata.firstday = model[fieldExp][GREATER_EQ];
                            $scope.view.metadata.lastday = model[fieldExp][LESS_THEN];
                        }
                    }

                    $scope.dateFilterNavigation = function (next) {

                        if ($scope.weekSelected) {
                            if ($scope.weekFirstDay) {
                                $scope.currentDate = $scope.weekFirstDay;
                            }

                            var first;
                            if (next) {
                                first = $scope.currentDate.getDate() + 7;
                            } else {
                                first = $scope.currentDate.getDate() - 7;
                            }
                            var last = first + 6;
                            $scope.weekFirstDay = new Date($scope.currentDate);
                            $scope.weekFirstDay.setDate(first);
                            $scope.weekLastDay = new Date($scope.currentDate);
                            $scope.weekLastDay.setDate(last);

                            var formattedFirstDay = $scope.weekFirstDay.getFormattedDate(attrs.format);
                            var formattedLastDay = $scope.weekLastDay.getFormattedDate(attrs.format);

                            $scope.firstDay = $scope.weekFirstDay.getFormattedDate(attrs.format);
                            var nextWeekObj = new Date($scope.weekLastDay.getFullYear(), $scope.weekLastDay.getMonth(), $scope.weekLastDay.getDate() + 1);
                            $scope.lastDay = nextWeekObj.getFormattedDate(attrs.format);
                            var value = formattedFirstDay + ' - ' + formattedLastDay;
                            dateInputElement.val(value);
                            $scope.setValue(value);
                        } else if ($scope.dateSelected) {
                            if (next) {
                                $scope.currentDate = new Date($scope.currentDate.setDate($scope.currentDate.getDate() + 1));
                            } else {
                                $scope.currentDate = new Date($scope.currentDate.setDate($scope.currentDate.getDate() - 1));
                            }
                            dateInputElement.val($scope.currentDate.getFormattedDate(attrs.format));
                            $scope.getFirstAndNextDate($scope.currentDate.getFormattedDate(attrs.format));

                        } else if ($scope.monthSelected) {
                            if (next) {
                                $scope.currentMonth += 1;
                            } else {
                                $scope.currentMonth -= 1;
                            }

                            var currentMonthFirstDay = new Date($scope.currentDate.getFullYear(), $scope.currentMonth, 1);
                            var nextMonthFirstDay = new Date($scope.currentDate.getFullYear(), $scope.currentMonth + 1, 1);

                            $scope.firstDay = currentMonthFirstDay.getFormattedDate(attrs.format)
                            $scope.lastDay = nextMonthFirstDay.getFormattedDate(attrs.format);

                            var value = $scope.months[currentMonthFirstDay.getMonth()] + ', ' + currentMonthFirstDay.getFullYear();
                            dateInputElement.val(value);
                            $scope.setValue(value);
                        } else if ($scope.yearSelected) {

                            if (next) {
                                $scope.currentDate.setFullYear($scope.currentDate.getFullYear() + 1);
                            } else {
                                $scope.currentDate.setFullYear($scope.currentDate.getFullYear() - 1);
                            }
                            $scope.currentYear = $scope.currentDate.getFullYear();
                            dateInputElement.val($scope.currentYear);
                            $scope.getFirstAndNextDate($scope.currentYear);
                        }
                    }
                }
            }
        }
    }
}]);


appStrapDirectives.directive('applaneGridBody', [
    '$compile',
    function ($compile) {
        return {
            restrict:'A',
            scope:false,
            compile:function () {
                return {
                    post:function ($scope, iElement, iAttr) {
                        var selectedRowRule = "\"{'app-selected-tr':row." + $scope.view.metadata.primarycolumn + "==currentRow." + $scope.view.metadata.primarycolumn + "}\"";
                        var dataExpression = $scope.view.metadata.dataExpression;
                        var template = "<table style='table-layout: fixed;width: 100%;' cellpadding='0' cellspacing='0'  class='applane-grid-body'><tbody>";
                        template += '<tr ';
                        if ($scope.view.metadata.$recursivecolumn) {
                            template += "  ng-show='!row._hidden'";
                        }

                        template += " style='height:25px;'  ng-class-even=\"'applane-grid-row-even'\" ng-class-odd=\"'applane-grid-row-odd'\"  ng-class=" + selectedRowRule + " ng-repeat='row in " + dataExpression + "'>" +
                            "<td applane-grid-cell class='applane-grid-cell' ng-repeat='col in view.metadata.columns' tabindex=0 ng-style='col.style' >" +
                            "</td>" +
                            "</tr>" +

                            "<tr ng-show='view.data.moduleresult.data.aggregates'  class='app-border-top-black app-border-bottom-black'>" +
                            "<td style='border:none;' class='applane-grid-cell' ng-repeat='col in view.metadata.columns' ng-bind-html-unsafe='getColumnValue(view.data.moduleresult.data.aggregates,col,false)'>" +
                            "</tr>" +

                            "</tbody></table>";
                        $(iElement).append($compile(template)($scope));
                    }
                }
            }
        };
    }
]);

appStrapDirectives.directive('applaneGridHeader', [
    '$compile',
    function ($compile) {
        return {
            restrict:'A',
            scope:false,
            template:"<table style='table-layout: fixed;width: 100%;' class='applane-grid-header' cellpadding='0' cellspacing='0' ><thead>" +
                "<th applane-grid-header-cell ng-repeat='col in view.metadata.columns' ng-style='col.style' id='{{col.id}}'></th>" +
                "<th style='width:17px;border:none;'>&nbsp;</th>" +
                "</thead></table>"
        };
    }
]);

appStrapDirectives.directive('applaneGridCell', [
    '$compile', '$timeout', '$dataModel', '$viewStack',
    function ($compile, $timeout, $dataModel, $viewStack) {
        return {
            restrict:'A',
            replace:true,
            scope:true,
            compile:function () {
                return  {
                    pre:function ($scope, iElement, iAttr) {

                        $scope.bindKeyDownOnTdElement = function () {
                            iElement.bind('keydown', function (evt) { // bind the keydown event on td
                                if (evt.keyCode == 37) {              // left arrow
                                    evt.preventDefault();
                                    evt.stopPropagation()
                                    $scope.bindLeftKeyDownEvent();
                                } else if (evt.keyCode == 38) {            // up arrow
                                    evt.preventDefault();
                                    evt.stopPropagation()
                                    $scope.bindUpKeyDownEvent();
                                } else if (evt.keyCode == 39) {           // right arrow
                                    evt.preventDefault();
                                    evt.stopPropagation()
                                    $scope.bindRightKeyDownEvent();
                                } else if (evt.keyCode == 40) {           // down arrow
                                    evt.preventDefault();
                                    evt.stopPropagation()
                                    $scope.bindDownKeyDownEvent();
                                } else if (evt.keyCode == 113) {         // for F2 key press
                                    $scope.editCell(evt);
                                }
                            });
                        }

                        $scope.bindDownKeyDownEvent = function () {

                            var parentTRElement = iElement.parent();
                            var nextTRElement = $(parentTRElement).next();
                            var length = nextTRElement.length;
                            var currentElementIndex = $(iElement).index();

                            if (length > 0) {
                                var tdElement = $(nextTRElement[0]).find('td');
                                var element = $(tdElement[currentElementIndex]);
                                element.focus();
                            } else {
                                var tBodyElement = $(parentTRElement[0]).parent();
                                var TRElement = $(tBodyElement[0]).find('tr');
                                var allTDElement = $(TRElement[0]).find('td');
                                var TDElement = $(allTDElement[currentElementIndex + 1]);
                                var tabIndex = TDElement.attr("tabindex");

                                if (tabIndex >= 0) {
                                    TDElement.focus();
                                    if (!$scope.$$phase) {
                                        $scope.$apply();
                                    }
                                }
                            }
                        }

                        $scope.bindUpKeyDownEvent = function () {

                            var parentElement = iElement.parent();
                            var prevTRElement = $(parentElement).prev();
                            var length = prevTRElement.length;
                            var currentElementIndex = $(iElement).index();

                            if (length > 0) {

                                var tdElement = $(prevTRElement[0]).find('td');
                                var element = $(tdElement[currentElementIndex]);
                                element.focus();
                            } else {
                                var tBodyElement = $(parentElement[0]).parent();
                                var TRElement = $(tBodyElement[0]).find('tr');
                                var TRElementLength = TRElement.length;

                                var requiredTRElement = $(TRElement[TRElementLength - 1]);
                                var allTDElement = requiredTRElement.find('td')

                                var TDElement = $(allTDElement[currentElementIndex - 1]);
                                var tabIndex = TDElement.attr("tabindex");

                                if (tabIndex >= 0) {
                                    TDElement.focus();
                                    if (!$scope.$$phase) {
                                        $scope.$apply();
                                    }
                                }
                            }
                        }

                        $scope.bindRightKeyDownEvent = function () {

                            var nextElement = iElement.next();
                            var length = nextElement.length;

                            if (length > 0) {

                                var nextElementTabIndex = $(nextElement[0]).attr("tabindex");

                                if (nextElementTabIndex >= 0) {
                                    $(nextElement[0]).focus();
                                } else {
                                    var parentTRElement = iElement.parent();
                                    var nextTRElement = parentTRElement.next();
                                    if (nextTRElement.length == 0) {
                                        return;
                                    }
                                    var tdElements = $(nextTRElement[0]).find('td');
                                    var tdLength = tdElements.length;

                                    for (var i = 0; i < tdLength; i++) {
                                        var tdAttr = $(tdElements[i]).attr("tabindex");
                                        if (tdAttr >= 0) {
                                            $(tdElements[i]).focus();
                                            if (!$scope.$$phase) {
                                                $scope.$apply();
                                            }
                                            break;
                                        }
                                    }
                                }
                            } else {
                                alert("Next Element Length is zero");
                            }
                        }


                        $scope.bindLeftKeyDownEvent = function () {

                            var prevElement = iElement.prev();
                            var length = prevElement.length;

                            if (length > 0) {
                                var prevElementTabIndex = $(prevElement[0]).attr("tabindex");
                                if (prevElementTabIndex >= 0) {
                                    $(prevElement[0]).focus();
                                } else {
                                    var parentTRElement = iElement.parent();
                                    var prevTRElement = parentTRElement.prev();

                                    if (prevTRElement.length == 0) {
                                        return;
                                    }

                                    var tdElements = $(prevTRElement[0]).find('td');
                                    var tdLength = tdElements.length;

                                    for (var i = (tdLength - 1); i >= 0; i--) {
                                        var tdAttr = $(tdElements[i]).attr("tabindex");
                                        if (tdAttr >= 0) {
                                            $(tdElements[i]).focus();
                                            if (!$scope.$$phase) {
                                                $scope.$apply();
                                            }
                                            break;
                                        }
                                    }
                                }
                            } else {
                                alert("Previous Element Length is zero");
                            }
                        }

                        $scope.showRenderingCell = function () {


                            var columnCellTemplate = $scope.col.cellTemplate;
                            if ((!columnCellTemplate) && ($scope.col.expression)) {
                                columnCellTemplate = CELL_TEMPLATE;
                            }


                            if ($scope.col.primary && $scope.view.metadata.$recursivecolumn) {
                                var treeNodeTemplate = "<span ng-style='{\"padding-left\":row._level * 20+\"px\"}' class='app-float-left app-text-align-center' >&nbsp;<img ng-click='toggleTree(row)' ng-class=\"{'app-right-down-arrow-transform':row._mode == 1, 'app-right-arrow-transform':row._mode == 0}\"  ng-show='row._mode == 0 || row._mode == 1' class='app-cursor-pointer'   width='12px' height='12px' src='images/up-arrow.png'></span>";
                                columnCellTemplate = treeNodeTemplate + columnCellTemplate
                            }
                            var cellTemplate = "<div style='white-space: nowrap;overflow: hidden;' ng-dblclick='editCell($event)' class='applane-grid-cell-inner'>" + columnCellTemplate + "</div>";

                            var cell = $compile(cellTemplate)($scope);
                            var width = $scope.col.width;

                            if (width) {
                                $(cell).width(width);
                                $(iElement).width(width);
                            }
                            $(iElement).html("");
                            iElement.append(cell);
                            if ($scope.col.type == 'selection') {
                                $scope.$watch('row.__selected__', function (newValue, oldValue) {
                                    var selectedKeys = $scope.view.selectedkeys;
                                    var selectedRowCount = selectedKeys.length;
                                    var index = -1;
                                    for (var i = 0; i < selectedRowCount; i++) {
                                        var record = selectedKeys[i];
                                        var recordKey = AppUtil.resolveDot(record, $scope.view.metadata[PRIMARY_COLUMN]);
                                        var isEqual = angular.equals(recordKey, AppUtil.resolveDot($scope.row, $scope.view.metadata[PRIMARY_COLUMN]));
                                        if (isEqual) {
                                            index = i;
                                            break;
                                        }
                                    }
                                    if (index == -1 && newValue) {
                                        selectedKeys.push($scope.row);
                                    } else if (index > -1 && !newValue) {
                                        selectedKeys.splice(i, 1);
                                    }
                                    if (!newValue) {
                                        $scope.view.__selectall__ = false;
                                    }
                                });
                            }

                            var tabIndex = $scope.col.tabindex;
                            if (tabIndex) {
                                iElement.attr("tabindex", tabIndex);
                            } else {
                                iElement.attr("tabindex", "0");
                            }


                            if (!$scope.$$phase) {
                                $scope.$apply();
                            }
                            $(iElement).focus(function () {

                                var currentRow = $dataModel.getCurrentRow($scope[COMPONENT_ID]);

                                $scope.setCurrentRow($scope.row);

                                if ($scope.view[CHILD_COMPONENT_ID]) {
                                    if ($scope.view.metadata.editMode === false && $scope.view.metadata.lastRowAction) {
                                        //assume click as row action click if child view is open
                                        if ((!angular.equals(currentRow, $scope.row))) {
                                            $scope.onRowActionClick($scope.view.metadata.lastRowAction)
                                        }
                                        return;
                                    }
                                } else {
                                    $scope.view.metadata.editMode = true;
                                    $scope.view.metadata.lastRowAction = undefined;
                                }

                                if (!$scope.$$phase) {
                                    $scope.$apply();
                                }


                            });
                            $scope.bindKeyDownOnTdElement();
                        };


                        $scope.showEditorCellIfAny = function ($event) {        // Add Listener on td element to show editor if any defined

                            if ($scope.view.metadata.editMode === false) {
                                return;
                            }
                            if (!$scope.editMode) {
                                return;
                            }

                            $scope.setCurrentRow($scope.row);
                            var editableCellTemplate = $scope.col.editableCellTemplate;

                            if (editableCellTemplate) {

                                $(iElement).attr("tabIndex", "-1");
                                $(iElement).html("");
                                $(iElement).unbind('keydown');

                                var popup = {
                                    template:editableCellTemplate,
                                    scope:$scope.$new(),
                                    autohide:true,
                                    element:iElement,
                                    width:$scope.col.width,
                                    position:'onchild',
                                    deffered:false,
                                    callBack:function (open, e) {

                                        if (!open) {
                                            $scope.showRenderingCell();
                                            if (this.scope) {
                                                this.scope.$destroy();
                                            }
                                        }

                                        if (e != null && e != undefined && e.keyCode == 27) {
                                            $scope.setEditMode(false);
                                            $(e.target).blur();
                                            $(iElement).focus();
                                        }
                                    }
                                };


                                $viewStack.showPopup(popup);
                                var focusElement = $(iElement).find("input")[0];
                                if (focusElement) {
                                    $(focusElement).focus();
                                }
                                if (!$scope.$$phase) {
                                    $scope.$apply();
                                }
                            }
                        }

                        $scope.editCell = function ($event) {
                            $scope.setEditMode(true);
                            $scope.showEditorCellIfAny($event);
                        };

                        $scope.showRenderingCell();
                        $(iElement).focus($scope.showEditorCellIfAny);
                    }
                };
            }
        };
    }
]);

appStrapDirectives.directive('applaneGridHeaderCell', [
    '$compile',
    function ($compile) {
        return {
            restrict:'A',
            replace:true,
            scope:true,
            compile:function () {
                return  {
                    pre:function ($scope, iElement, iAttr) {

                        var cellTemplate = '<div >{{col.label}}' +
                            '<img style="margin-left: 10px;" width="11px" height="11px" ng-src="images/{{col.order}}-arrow.png" ng-show="col.order"/>' +
                            '</div>';

                        if ($scope.col.type == 'selection') {
                            cellTemplate = "<div><input type='checkbox' ng-model='view.__selectall__'></div>";
                        }
                        var headerCell = $compile(cellTemplate)($scope);
                        var width = $scope.col.width;
                        if (width) {
                            $(headerCell).width(width);
                            $(iElement).width(width);
                        }
                        iElement.append(headerCell);
                        if ($scope.col.type == 'selection') {
                            var selectionElement = angular.element(headerCell).find("input");
                            var rows = AppUtil.resolveDot($scope, $scope.view.metadata.dataExpression);
                            var checkBoxElement;
                            if (selectionElement.length == 0) {
                                checkBoxElement = selectionElement;
                            } else {
                                checkBoxElement = selectionElement[0];
                            }
                            $(checkBoxElement).change(function () {
                                var selected = $(this).is(":checked");
                                for (var i = 0; i < rows.length; i++) {
                                    rows[i].__selected__ = selected;
                                }
                                if (!$scope.$$phase) {
                                    $scope.$apply();
                                }
                            });
                        }
                    }
                };
            }
        };
    }
]);


appStrapDirectives.directive('appColumnGroup', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div style='margin-bottom: 5px;' class='app-width-full app-float-left' >" +

            "<div ng-show = 'columnGroup.showtitle'  " +
            "class='cg-title app-width-full app-dark-gray-backgroud-color app-color-white app-font-weight-bold' " +
            "style='padding:5px 0px 5px 5px;line-height:15px;' ng-bind='columnGroup.title' >" +
            "</div>" +


            "<div ng-repeat='column in columnGroup.columns'>" +

            '<div style="width:{{columnGroup.cellWidth}}%;padding-top:10px;min-width:{{columnGroup.columnWidth }};"' + ' class="app-float-left cell-width">' +

            '<div title="{{column.label}}" ng-show="columnGroup.showcolumnlabel"  ' +
            'class="app-float-left app-font-weight-bold {{alignclass}} app-overflow-hiiden app-white-space-nowrap"  style="font-size:13px;width:{{columnLabelStyle}}px;;padding-left:7px;">' +
            '{{column.label}}:</div>' +

            '<div style="width:{{columnGroup.columnWidth}}px;;height:{{columnGroup.height}}px;;padding-left:7px;" class="app-float-left">' +
            "<app-column ng-init='colmetadata=column'></app-column>" +
            "<div>" +

            "</div>" +
            "</div>" +

            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement) {
                    $scope.resize = function () {

                        var parentWidth = iElement.width();
                        parentWidth = parentWidth / $scope.columnGroup.columnPerRow;
                        var minWidth = $scope.columnGroup.columnWidth + $scope.columnGroup.labelWidth + 14; // 7px padding on both label and column div

                        if (parentWidth > minWidth) {
                            $scope.alignclass = 'app-text-align-right';
                            $scope.columnLabelStyle = $scope.columnGroup.labelWidth;
                        } else {
                            $scope.alignclass = 'app-text-align-left app-padding-bottom-five-px';
                            $scope.columnLabelStyle = $scope.columnGroup.columnWidth;
                        }
                        if (!$scope.$$phase) {
                            $scope.$apply();
                        }
                    }
                    $(window).resize(function () {
                        $scope.resize();
                    });

                    $scope.$watch("resized", function (newValue, oldValue) {
                        if (!angular.equals(newValue, oldValue)) {
                            $scope.resize();
                        }
                    }, true)

                    $scope.resize();
                }
            };
        }
    }
}]);


appStrapDirectives.directive("appPanel", ["$compile", "$dataModel", "$appDataSource", function ($compile, $dataModel, $appDataSource) {
    return {
        restrict:"E",
        replace:true,
        template:"<div class='app-panel'>" +
            '<div class="app-container">' +
            '<div class="app-wrapper">' +
            '<div class="app-wrapper-child app-overflow-auto">' +
            '<div id="app-panel-tool-bar_{{view.componentid}}" class="app-tool-bar-background app-float-left app-width-full" ng-show="view.showas == \'popup\'" app-tool-bar></div>' + // Tool Bar for panel view
            '<app-column-group ng-repeat="columnGroup in view.metadata.newcolumnGroups"></app-column-group>' +
            '<app-bar ng-init="actions=view.metadata.footeractions" ></app-bar>' +
            '</div>' +
            '</div>' +
            '</div>' +
            "</div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    var id = TOOL_BAR_ID + '_' + $scope.view[COMPONENT_ID];

                    var toolBarTemplate = "<div class='app-tool-bar' app-tool-bar id='" + id + "'></div>";
                    var toolBarElement = $compile(toolBarTemplate)($scope);


                    var showAs = $scope.view[SHOW_AS];
                    if (showAs == 'popup') {
                        //    do nothing
                    } else if ($scope.view.metadata.embed) {
                        iElement.append(toolBarElement);
                    } else {
                        $('#' + TOOL_BAR_ID).append(toolBarElement);

                        if ($scope.view[PARENT_COMPONENT_ID]) {
                            var parentId = TOOL_BAR_ID + '_' + $scope.view[PARENT_COMPONENT_ID];
                            $('#' + parentId).hide();
                        }
                    }

                    var metadata = $scope.view.metadata;
                    var columnGroups = metadata.columnGroups;


                    if (!columnGroups) {
                        columnGroups = {
                            "onenlabel":{"showcolumnlabel":false, "title":"", "showtitle":false, "columnPerRow":1, "columnWidth":($(window).width() / 2) - 12, "labelWidth":150, "placeholder":"true", "height":"auto"},
                            "richtext":{"showcolumnlabel":true, "title":"", "showtitle":false, "columnPerRow":1, "columnWidth":($(window).width() / 2) - 59, "labelWidth":150, "placeholder":"true", "height":"auto"},
                            "twowlabel":{"showcolumnlabel":true, "title":"", "showtitle":false, "columnPerRow":2, "columnWidth":200, "labelWidth":150, "placeholder":"true", "height":30 },
                            "onewlabel":{"showcolumnlabel":true, "title":"", "showtitle":false, "columnPerRow":1, "columnWidth":($(window).width() / 2) - 59, "labelWidth":150, "placeholder":"true", "height":"auto" }
                        };
                        metadata.columnGroups = columnGroups;
                    }


                    var columns = metadata.columns;
                    var newcolumnGroups = [];
                    var columnCount = columns ? columns.length : 0;
                    var lastcolumnGroupID = "";
                    var lastcolumnGroup;
                    var lastColumns;

                    for (var i = 0; i < columnCount; i++) {
                        var column = columns[i];

                        var columnType = column.type;
                        if (columnType == 'lookup' || (column.multiple && (columnType != 'object') && columnType != 'view')) {
                            var field = column.expression;
                            var lookupSource = $scope[COMPONENT_ID] + "_" + i
                            if (columnType != 'lookup') {
                                $scope[lookupSource] = [""];
                            } else if (column.options && angular.isArray(column.options) && column.options.length > 0) {
                                $scope[lookupSource] = column.options;
                            } else {
                                column[COMPONENT_ID] = $scope[COMPONENT_ID];
                                column.ask = $scope.view.ask;
                                column.osk = $scope.view.osk;
                                $scope[lookupSource] = new $appDataSource.getLookupDataSource(column)
                                field = field + "." + column.primarycolumns[0].expression;
                            }

                            var multiple = column.multiple;

                            var model = $scope.view.metadata.dataExpression + "[0]";
                            column.editableCellTemplate = "<app-lookup model='" + model + "' field='" + field + "' datasource='" + lookupSource + "' multiple=" + multiple + "></app-lookup>";

                        } else if (columnType === 'duration') {
                            column.editableCellTemplate = "<app-duration model='row' field='" + column.expression + "'></app-duration>";
                        } else if (columnType == CURRENCY_TYPE) {
                            column.editableCellTemplate = "<app-currency model='row' field='" + column.expression + "'></app-currency>";
                        } else if (columnType === 'date' || columnType == 'datetime') {
                            column.editableCellTemplate = "<app-datepicker  model='row' field='" + column.expression + "'></app-datepicker>"

                        } else if (columnType === 'textarea') {
                            column.editableCellTemplate = '<textarea></textarea>';
                        } else if (columnType === 'boolean') {
                            column.editableCellTemplate = "<app-checkbox class='app-panel-input' model='row' field='" + column.expression + "'></app-checkbox>";
                        } else if (columnType == 'view') {
                            column.editableCellTemplate = '<div class="app-width-full"><app-nested-view ></app-nested-view></div>';

                        } else if (columnType == 'file') {
                            column.editableCellTemplate = '<app-file-upload class="app-float-left" model="row" field="' + column.expression + '"></app-file-upload>';
                        } else if (columnType == 'schedule') {
                            column.editableCellTemplate = "<app-datepicker model='row' field='" + column.expression + "' schedule=true></app-datepicker>"
                        } else if (columnType == 'text') {
                            column.editableCellTemplate = "<app-text-area model='row' field='" + column.expression + "'></app-text-area>";
                        } else if (columnType == 'richtext') {
                            column.editableCellTemplate = "<app-rich-text-area model='row' field='" + column.expression + "'></app-rich-text-area>";
                        } else if (columnType == 'index') {
                            column.editableCellTemplate = "<app-text model='row' field='" + column.expression + ".index' border=false></app-text>";
                        } else {
                            column.editableCellTemplate = "<app-text model='row' field='" + column.expression + "'></app-text>";
                        }

                        if (columnType == 'view') {
                            column.columnGroup = 'onenlabel';
                        } else if (columnType == 'richtext' || columnType == 'text') {
                            column.columnGroup = 'richtext';
                        } else if ((column.multiple)) {
                            column.columnGroup = "onewlabel";
                        } else {
                            column.columnGroup = 'twowlabel';
                        }

                        var columnGroup = column.columnGroup;
                        if (columnGroup != lastcolumnGroupID) {
                            lastcolumnGroupID = columnGroup;
                            var cg = angular.copy(columnGroups[columnGroup]);
                            lastcolumnGroup = cg;
                            var columnsPerRow = cg.columnPerRow ? cg.columnPerRow : 1;

                            cg.labelWidth = cg.labelWidth ? cg.labelWidth : 200;
                            cg.columnWidth = cg.columnWidth ? cg.columnWidth : 200;
                            cg.height = cg.height ? cg.height : 24;
                            cg.cellWidth = 100 / columnsPerRow;


                            newcolumnGroups.push(cg);
                            lastColumns = [];
                            cg.columns = lastColumns
                        }
                        column.width = lastcolumnGroup.columnWidth;
                        if (columnType == 'boolean') {
                            column.width = '15';
                        }
                        column.height = lastcolumnGroup.height + "px";

                        if ((columnType == 'lookup' && column.multiple) || columnType == 'richtext' || columnType == 'text') {
                            column.height = "auto";
                        }
                        lastColumns.push(column);
                    }

                    metadata.newcolumnGroups = newcolumnGroups;

                    var dataRecords = AppUtil.resolveDot($scope, $scope.view.metadata.dataExpression);
                    var dataRecordCount = dataRecords.length;
                    if (dataRecordCount == 0) {
                        $dataModel.insert($scope[COMPONENT_ID]);
                    }

                    $scope.row = AppUtil.resolveDot($scope, $scope.view.metadata.dataExpression)[0];

                    if (metadata.refreshonload) {
                        $dataModel.refresh($scope.view[COMPONENT_ID]);
                    }
                    $dataModel.setCurrentRow($scope[COMPONENT_ID], $scope.row);

                }
            }
        }
    }
}]);

/************************************************RTE**************************************************************/
appStrapDirectives.directive('appRichTextArea', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div></div>",

        compile:function () {
            return{
                pre:function ($scope, iElement, attrs) {

                    var modelExp = attrs.model;
                    var fieldExp = attrs.field;
                    var toBind = modelExp + "." + fieldExp;


                    var textareaTemplate = '<textarea name="textarea" ng-bind="' + toBind + '"></textarea>' +
                        '<div class="jqte">' +
                        '<div class="jqte_toolbar"  role="toolbar" unselectable></div><div class="jqte_linkform" style="display:none" role="dialog">' +
                        '<div class="jqte_linktypeselect" unselectable>' +
                        '<div class="jqte_linktypeview" unselectable></div><div class="jqte_linktypes" role="menu" unselectable></div>' +
                        '</div><input class="jqte_linkinput" type="text/css" value=""><div class="jqte_linkbutton" unselectable>OK</div> <div style="height:1px;float:none;clear:both"></div>' +
                        '</div><div class="jqte_editor" ng-bind-html-unsafe ="' + toBind + '"></div>' +
                        '<div class="jqte_source jqte_hiddenField"></div>' +
                        '</div>';
                    $(iElement).append($compile(textareaTemplate)($scope));

                    var textAreaElement = angular.element(iElement).find('textarea');


                    $scope.richTextBoxEditor = function (options) {

                        // default titles of buttons
                        var varsTitle = [
                            {title:"Text Format"},
                            {title:"Font Size"},
                            {title:"Color"},
                            {title:"Bold", hotkey:"B"},
                            {title:"Italic", hotkey:"I"},
                            {title:"Underline", hotkey:"U"},
                            {title:"Ordered List", hotkey:"."},
                            {title:"Unordered List", hotkey:","},
                            {title:"Subscript", hotkey:"down arrow"},
                            {title:"Superscript", hotkey:"up arrow"},
                            {title:"Outdent", hotkey:"left arrow"},
                            {title:"Indent", hotkey:"right arrow"},
                            {title:"Justify Left"},
                            {title:"Justify Center"},
                            {title:"Justify Right"},
                            {title:"Strike Through", hotkey:"K"},
                            {title:"Add Link", hotkey:"L"},
                            {title:"Remove Link"},
                            {title:"Cleaner Style", hotkey:"Delete"},
                            {title:"Horizontal Rule", hotkey:"H"},
                            {title:"Source"}
                        ];

                        // default text formats
                        var formats = [
                            ["p", "Normal"],
                            ["h1", "Header 1"],
                            ["h2", "Header 2"],
                            ["h3", "Header 3"],
                            ["h4", "Header 4"],
                            ["h5", "Header 5"],
                            ["h6", "Header 6"],
                            ["pre", "Preformatted"]
                        ];

                        // default font sizes
                        var fsizes = ["10", "12", "16", "18", "20", "24", "28"];

                        // default rgb values of colors
                        var colors = [
                            "0,0,0", "68,68,68", "102,102,102", "153,153,153", "204,204,204", "238,238,238", "243,243,243", "255,255,255",
                            null,
                            "255,0,0", "255,153,0", "255,255,0", "0,255,0", "0,255,255", "0,0,255", "153,0,255", "255,0,255",
                            null,
                            "244,204,204", "252,229,205", "255,242,204", "217,234,211", "208,224,227", "207,226,243", "217,210,233", "234,209,220",
                            "234,153,153", "249,203,156", "255,229,153", "182,215,168", "162,196,201", "159,197,232", "180,167,214", "213,166,189",
                            "224,102,102", "246,178,107", "255,217,102", "147,196,125", "118,165,175", "111,168,220", "142,124,195", "194,123,160",
                            "204,0,0", "230,145,56", "241,194,50", "106,168,79", "69,129,142", "61,133,198", "103,78,167", "166,77,121",
                            "153,0,0", "180,95,6", "191,144,0", "56,118,29", "19,79,92", "11,83,148", "53,28,117", "116,27,71",
                            "102,0,0", "120,63,4", "127,96,0", "39,78,19", "12,52,61", "7,55,99", "32,18,77", "76,17,48"
                        ];

                        // default link-type names
                        var linktypes = ["Web Address"];

                        var vars = angular.extend({
                            // options
                            'status':true,
                            'css':"jqte",
                            'title':true,
                            'titletext':varsTitle,
                            'button':"OK",
                            'format':true,
                            'formats':formats,
                            'fsize':true,
                            'fsizes':fsizes,
                            'funit':"px",
                            'color':true,
                            'linktypes':linktypes,
                            'b':true,
                            'i':true,
                            'u':true,
                            'ol':true,
                            'ul':true,
                            'sub':true,
                            'sup':true,
                            'outdent':true,
                            'indent':true,
                            'left':true,
                            'center':true,
                            'right':true,
                            'strike':true,
                            'link':true,
                            'unlink':true,
                            'remove':true,
                            'rule':true,
                            'source':true,
                            'placeholder':false,
                            'br':true,
                            'p':true,

                            // events
                            'change':"",
                            'focus':"",
                            'blur':""
                        }, options);

                        // methods
                        $scope.richTextBoxEditorValue = function (value) {
                            textAreaElement.val(value)
                            textAreaElement.closest("." + vars.css).find("." + vars.css + "_editor").html(value);
                        }

                        // browser information is received
                        var thisBrowser = navigator.userAgent.toLowerCase();

                        var buttons = [];

                        // insertion function for parameters to toolbar
                        function addParams(name, command, key, tag, emphasis) {
                            var thisCssNo = buttons.length + 1;
                            return buttons.push({name:name, cls:thisCssNo, command:command, key:key, tag:tag, emphasis:emphasis});
                        }

                        ;

                        // add parameters for toolbar buttons
                        addParams('format', 'formats', '', '', false); // text format button  --> no hotkey
                        addParams('fsize', 'fSize', '', '', false); // font size button --> no hotkey
                        addParams('color', 'colors', '', '', false); // text color button  --> no hotkey
                        addParams('b', 'Bold', 'B', ["b", "strong"], true); // bold --> ctrl + b
                        addParams('i', 'Italic', 'I', ["i", "em"], true); // italic --> ctrl + i
                        addParams('u', 'Underline', 'U', ["u"], true); // underline --> ctrl + u
                        addParams('ol', 'insertorderedlist', '¾', ["ol"], true); // ordered list --> ctrl + .(dot)
                        addParams('ul', 'insertunorderedlist', '¼', ["ul"], true); // unordered list --> ctrl + ,(comma)
                        addParams('sub', 'subscript', '(', ["sub"], true); // sub script --> ctrl + down arrow
                        addParams('sup', 'superscript', '&', ["sup"], true); // super script --> ctrl + up arrow
                        addParams('outdent', 'outdent', '%', ["blockquote"], false); // outdent --> ctrl + left arrow
                        addParams('indent', 'indent', '\'', ["blockquote"], true); // indent --> ctrl + right arrow
                        addParams('left', 'justifyLeft', '', '', false); // justify Left --> no hotkey
                        addParams('center', 'justifyCenter', '', '', false); // justify center --> no hotkey
                        addParams('right', 'justifyRight', '', '', false); // justify right --> no hotkey
                        addParams('strike', 'strikeThrough', 'K', ["strike"], true); // strike through --> ctrl + K
                        addParams('link', 'linkcreator', 'L', ["a"], true); // insertion link  --> ctrl + L
                        addParams('unlink', 'unlink', '', ["a"], false); // remove link --> ctrl + N
                        addParams('remove', 'removeformat', '.', '', false); // remove all styles --> ctrl + delete
                        addParams('rule', 'inserthorizontalrule', 'H', ["hr"], false); // insertion horizontal rule --> ctrl + H
                        addParams('source', 'displaysource', '', '', false); // feature of displaying source


                        return textAreaElement.each(function () {

                            if (!$(this).data("jqte") || $(this).data("jqte") == null || $(this).data("jqte") == "undefined")
                                $(this).data("jqte", true);
                            else
                                $(this).data("jqte", false);

                            // is the status false of the editor
                            if (!vars.status || !$(this).data("jqte")) {
                                // if wanting the false status later
                                if ($(this).closest("." + vars.css).length > 0) {
                                    var editorValue = $(this).closest("." + vars.css).find("." + vars.css + "_editor").html();

                                    // add all attributes of element
                                    var thisElementAttrs = "";

                                    $($(this)[0].attributes).each(function () {
                                        if (this.nodeName != "style")
                                            thisElementAttrs = thisElementAttrs + " " + this.nodeName + '="' + this.nodeValue + '"';
                                    });

                                    var thisElementTag = $(this).is("[data-origin]") && $(this).attr("data-origin") != "" ? $(this).attr("data-origin") : "textarea";

                                    // the contents of this element
                                    var createValue = '>' + editorValue;

                                    // if this element is input or option
                                    if (thisElementTag == "input" || thisElementTag == "option") {
                                        // encode special html characters
                                        editorValue = editorValue.replace(/"/g, '&#34;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

                                        // the value of this element
                                        createValue = 'value="' + editorValue + '">';
                                    }

                                    var thisClone = $(this).clone();

                                    $(this).data("jqte", false).closest("." + vars.css).before(thisClone).remove();
                                    thisClone.replaceWith('<' + thisElementTag + thisElementAttrs + createValue + '</' + thisElementTag + '>');
                                }
                                return;
                            }

                            // element will converted to the jqte editor
                            var thisElement = $(this);

                            // tag name of the element
                            var thisElementTag = $(this).prop('tagName').toLowerCase();

                            // tag name of origin
                            $(this).attr("data-origin", thisElementTag);

                            // contents of the element
                            var thisElementVal = $(this).is("[value]") || thisElementTag == "textarea" ? $(this).val() : $(this).html();

                            // decode special html characters
                            thisElementVal = thisElementVal.replace(/&#34;/g, '"').replace(/&#39;/g, "'").replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');

                            // start jqte editor to after the element
//                            $(this).after('');

                            // jqte
                            var jQTE = $(this).next('.' + vars.css);

                            // insert toolbar in jqte editor
//                            jQTE.html('<div class="jqte_toolbar"  role="toolbar" unselectable></div><div class="jqte_linkform" style="display:none" role="dialog"></div><div class="jqte_editor"></div>');

                            var toolbar = jQTE.find('.' + vars.css + "_toolbar"); // the toolbar variable
                            var linkform = jQTE.find('.' + vars.css + "_linkform"); // the link-form-area in the toolbar variable
                            var editor = jQTE.find('.' + vars.css + "_editor"); // the text-field of jqte editor
                            var emphasize = vars.css + "_tool_depressed"; // highlight style of the toolbar buttons

                            // add to some tools in link form area
//                            linkform.append('<div class="jqte_linktypeselect" unselectable></div><input class="jqte_linkinput" type="text/css" value=""><div class="jqte_linkbutton" unselectable>OK</div> <div style="height:1px;float:none;clear:both"></div>');

                            var linktypeselect = linkform.find("." + vars.css + "_linktypeselect"); // the tool of link-type-selector
                            var linkinput = linkform.find("." + vars.css + "_linkinput"); // the input of insertion link
                            var linkbutton = linkform.find("." + vars.css + "_linkbutton"); // the button of insertion link

                            // add to the link-type-selector sub tool parts
//                            linktypeselect.append('<div class="jqte_linktypeview" unselectable></div><div class="jqte_linktypes" role="menu" unselectable></div>');

                            var linktypes = linktypeselect.find("." + vars.css + "_linktypes"); // the select box of link types
                            var linktypeview = linktypeselect.find("." + vars.css + "_linktypeview"); // the link type preview
                            var setdatalink = vars.css + "-setlink"; // the selected text add to mark as "link will be added"

                            // create to the source-area
//                            editor.after('<div class="jqte_source ' + vars.css + '_hiddenField"></div>');

                            var sourceField = jQTE.find("." + vars.css + "_source"); // the source-area variable

                            // move the element to the source-area
                            thisElement.appendTo(sourceField);

                            // if the element isn't a textarea, convert this to textarea
                            if (thisElementTag != "textarea") {
                                // add all attributes of element to new textarea (type and value except)
                                var thisElementAttrs = "";

                                $(thisElement[0].attributes).each(function () {
                                    if (this.nodeName != "type" && this.nodeName != "value")
                                        thisElementAttrs = thisElementAttrs + " " + this.nodeName + '="' + this.nodeValue + '"';
                                });

                                // convert the element to textarea
                                thisElement.replaceWith('<textarea ' + thisElementAttrs + '>' + thisElementVal + '</textarea>');

                                // update to variable of thisElement
                                thisElement = sourceField.find("textarea");
                            }

                            // add feature editable to the text-field ve copy from the element's value to text-field
//                            editor.attr("contenteditable", "true").html(thisElementVal);
                            editor.attr("contenteditable", "true")
//                          editor.attr("ng-bind", toBind);

//                            if (!$scope.$$phase) {
//                                $scope.$apply();
//                            }

                            // insertion the toolbar button
                            for (var n = 0; n < buttons.length; n++) {
                                // if setting of this button is activated (is it true?)
                                if (vars[buttons[n].name]) {
                                    // if it have a title, add to this button
                                    var buttonHotkey = buttons[n].key.length > 0 ? vars.titletext[n].hotkey != null && vars.titletext[n].hotkey != "undefined" && vars.titletext[n].hotkey != "" ? ' (Ctrl+' + vars.titletext[n].hotkey + ')' : '' : '';
                                    var buttonTitle = vars.titletext[n].title != null && vars.titletext[n].title != "undefined" && vars.titletext[n].title != "" ? vars.titletext[n].title + buttonHotkey : '';

                                    // add this button to the toolbar
                                    toolbar.append('<div class="' + vars.css + '_tool ' + vars.css + '_tool_' + buttons[n].cls + '" role="button" data-tool="' + n + '" unselectable><a class="' + vars.css + '_tool_icon" unselectable></a></div>');

                                    // add the parameters to this button
                                    toolbar.find('.' + vars.css + '_tool[data-tool=' + n + ']').data({tag:buttons[n].tag, command:buttons[n].command, emphasis:buttons[n].emphasis, title:buttonTitle});

                                    // format-selector field
                                    if (buttons[n].name == "format" && angular.isArray(vars.formats)) {
                                        // selected text format
                                        var toolLabel = vars.formats[0][1].length > 0 && vars.formats[0][1] != "undefined" ? vars.formats[0][1] : "";

                                        toolbar.find("." + vars.css + '_tool_' + buttons[n].cls).find("." + vars.css + "_tool_icon").replaceWith('<a class="' + vars.css + '_tool_label" unselectable><span class="' + vars.css + '_tool_text" unselectable>' + toolLabel + '</span><span class="' + vars.css + '_tool_icon" unselectable></span></a>');

                                        toolbar.find("." + vars.css + '_tool_' + buttons[n].cls)
                                            .append('<div class="' + vars.css + '_formats" unselectable></div>');

                                        // add font-sizes to font-size-selector
                                        for (var f = 0; f < vars.formats.length; f++) {
                                            toolbar.find("." + vars.css + "_formats").append('<a ' + vars.css + '-formatval="' + vars.formats[f][0] + '" class="' + vars.css + '_format' + ' ' + vars.css + '_format_' + f + '" role="menuitem" unselectable>' + vars.formats[f][1] + '</a>');
                                        }

                                        toolbar.find("." + vars.css + "_formats").data("status", false);
                                    }

                                    // font-size-selector field
                                    else if (buttons[n].name == "fsize" && angular.isArray(vars.fsizes)) {
                                        toolbar.find("." + vars.css + '_tool_' + buttons[n].cls)
                                            .append('<div class="' + vars.css + '_fontsizes" unselectable></div>');

                                        // add font-sizes to font-size-selector
                                        for (var f = 0; f < vars.fsizes.length; f++) {
                                            toolbar.find("." + vars.css + "_fontsizes").append('<a ' + vars.css + '-styleval="' + vars.fsizes[f] + '" class="' + vars.css + '_fontsize' + '" style="font-size:' + vars.fsizes[f] + vars.funit + '" role="menuitem" unselectable>Abcdefgh...</a>');
                                        }
                                    }

                                    // color-selector field
                                    else if (buttons[n].name == "color" && angular.isArray(colors)) {
                                        toolbar.find("." + vars.css + '_tool_' + buttons[n].cls)
                                            .append('<div class="' + vars.css + '_cpalette" unselectable></div>');

                                        // create color palette to color-selector field
                                        for (var c = 0; c < colors.length; c++) {
                                            if (colors[c] != null)
                                                toolbar.find("." + vars.css + "_cpalette").append('<a ' + vars.css + '-styleval="' + colors[c] + '" class="' + vars.css + '_color' + '" style="background-color: rgb(' + colors[c] + ')" role="gridcell" unselectable></a>');
                                            else
                                                toolbar.find("." + vars.css + "_cpalette").append('<div class="' + vars.css + "_colorSeperator" + '"></div>');
                                        }
                                    }
                                }
                            }

                            // the default value of the link-type
                            linktypes.data("linktype", "0");

                            // add link types to link-type-selector
                            for (var n = 0; n < vars.linktypes.length; n++) {
                                linktypes.append('<a ' + vars.css + '-linktype="' + n + '" unselectable>' + vars.linktypes[n] + '</a>');

                                linktypeview.html('<div class="' + vars.css + '_linktypearrow" unselectable></div>' +
                                    '<div class="' + vars.css + '_linktypetext">' + linktypes.find('a:eq(' + linktypes.data("linktype") + ')').text() + '</div>');
                            }


                            // the feature of placeholder
                            if (vars.placeholder && vars.placeholder != "") {
                                jQTE.prepend('<div class="' + vars.css + '_placeholder" unselectable><div class="' + vars.css + '_placeholder_text">' + vars.placeholder + '</div></div>');

                                var placeHolder = jQTE.find("." + vars.css + "_placeholder");

                                placeHolder.click(function () {
                                    editor.focus();
                                });
                            }

                            // make unselectable to unselectable attribute ones
                            jQTE.find("[unselectable]")
                                .css("user-select", "none")
                                .addClass("unselectable")
                                .attr("unselectable", "on")
                                .on("selectstart mousedown", false);

                            // each button of the toolbar
                            var toolbutton = toolbar.find("." + vars.css + "_tool");

                            // format menu
                            var formatbar = toolbar.find("." + vars.css + "_formats");

                            // font-size filed
                            var fsizebar = toolbar.find("." + vars.css + "_fontsizes");

                            // color palette
                            var cpalette = toolbar.find("." + vars.css + "_cpalette");

                            // get the selected text as plain format
                            function selectionGet() {
                                // for webkit, mozilla, opera
                                if (window.getSelection)
                                    return window.getSelection();
                                // for ie
                                else if (document.selection && document.selection.createRange && document.selection.type != "None")
                                    return document.selection.createRange();
                            }

                            // the function of changing to the selected text with "execCommand" method
                            function selectionSet(addCommand, thirdParam) {
                                var range,
                                    sel = selectionGet();

                                // for webkit, mozilla, opera
                                if (window.getSelection) {
                                    if (sel.anchorNode && sel.getRangeAt)
                                        range = sel.getRangeAt(0);

                                    if (range) {
                                        sel.removeAllRanges();
                                        sel.addRange(range);
                                    }


                                    document.execCommand(addCommand, false, thirdParam);
                                }

                                // for ie
                                else if (document.selection && document.selection.createRange && document.selection.type != "None") {
                                    range = document.selection.createRange();
                                    range.execCommand(addCommand, false, thirdParam);
                                }

                                // change styles to around tags
                                affectStyleAround(false, false);
                            }

                            // the function of changing to the selected text with tags and tags's attributes
                            function replaceSelection(tTag, tAttr, tVal) {

                                // first, prevent to conflict of different jqte editors
                                if (editor.not(":focus"))
                                    editor.focus();

                                // for webkit, mozilla, opera
                                if (window.getSelection) {
                                    var selObj = selectionGet(), selRange, newElement, documentFragment;

                                    if (selObj.anchorNode && selObj.getRangeAt) {
                                        selRange = selObj.getRangeAt(0);

                                        // create to new element
                                        newElement = document.createElement(tTag);

                                        // add the attribute to the new element
                                        $(newElement).attr(tAttr, tVal);

                                        // extract to the selected text
                                        documentFragment = selRange.extractContents();

                                        // add the contents to the new element
                                        newElement.appendChild(documentFragment);

                                        selRange.insertNode(newElement);
                                        selObj.removeAllRanges();

                                        // if the attribute is "style", change styles to around tags
                                        if (tAttr == "style")
                                            affectStyleAround($(newElement), tVal);
                                        // for other attributes
                                        else
                                            affectStyleAround($(newElement), false);
                                    }
                                }
                                // for ie
                                else if (document.selection && document.selection.createRange && document.selection.type != "None") {
                                    var range = document.selection.createRange();
                                    var selectedText = range.htmlText;

                                    var newText = '<' + tTag + ' ' + tAttr + '="' + tVal + '">' + selectedText + '</' + tTag + '>';

                                    document.selection.createRange().pasteHTML(newText);
                                }
                            }

                            // the function of getting to the parent tag
                            var getSelectedNode = function () {
                                var node, selection;
                                if (window.getSelection) {
                                    selection = getSelection();
                                    node = selection.anchorNode;
                                }
                                if (!node && document.selection && document.selection.createRange && document.selection.type != "None") {
                                    selection = document.selection;
                                    var range = selection.getRangeAt ? selection.getRangeAt(0) : selection.createRange();
                                    node = range.commonAncestorContainer ? range.commonAncestorContainer :
                                        range.parentElement ? range.parentElement() : range.item(0);
                                }
                                if (node) {
                                    return (node.nodeName == "#text" ? $(node.parentNode) : $(node));
                                }
                                else
                                    return false;
                            };

                            // the function of replacement styles to the around tags (parent and child)
                            function affectStyleAround(element, style) {
                                var selectedTag = getSelectedNode(); // the selected node

                                selectedTag = selectedTag ? selectedTag : element;

                                // (for replacement with execCommand) affect to child tags with parent tag's styles
                                if (selectedTag && style == false) {
                                    // apply to the selected node with parent tag's styles
                                    if (selectedTag.parent().is("[style]"))
                                        selectedTag.attr("style", selectedTag.parent().attr("style"));

                                    // apply to child tags with parent tag's styles
                                    if (selectedTag.is("[style]"))
                                        selectedTag.find("*").attr("style", selectedTag.attr("style"));
                                }
                                // (for replacement with html changing method)
                                else if (element && style && element.is("[style]")) {
                                    var styleKey = style.split(";"); // split the styles

                                    styleKey = styleKey[0].split(":") // get the key of first style feature

                                    // apply to child tags with parent tag's styles
                                    if (element.is("[style*=" + styleKey[0] + "]"))
                                        element.find("*").css(styleKey[0], styleKey[1]);

                                    // select to the selected node again
                                    selectText(element);
                                }
                            }

                            // the function of making selected to a element
                            function selectText(element) {
                                if (element) {
                                    var element = element[0];

                                    if (document.body.createTextRange) {
                                        var range = document.body.createTextRange();
                                        range.moveToElementText(element);
                                        range.select();
                                    }
                                    else if (window.getSelection) {
                                        var selection = window.getSelection();
                                        var range = document.createRange();

                                        if (element != "undefined" && element != null) {
                                            range.selectNodeContents(element);

                                            selection.removeAllRanges();
                                            selection.addRange(range);

                                            if ($(element).is(":empty")) {
                                                $(element).append("&nbsp;");
                                                selectText($(element));
                                            }
                                        }
                                    }
                                }
                            }

                            // the function of converting text to link
                            function selected2link() {
                                if (!toolbar.data("sourceOpened")) {
                                    var selectedTag = getSelectedNode(); // the selected node
                                    var thisHrefLink = "http://"; // default the input value of the link-form-field

                                    // display the link-form-field
                                    linkAreaSwitch(true);

                                    if (selectedTag) {

                                        var thisTagName = selectedTag.prop('tagName').toLowerCase();

                                        // if tag name of the selected node is "a" and the selected node have "href" attribute
                                        if (thisTagName == "a" && selectedTag.is('[href]')) {
                                            thisHrefLink = selectedTag.attr('href');

                                            selectedTag.attr(setdatalink, "");
                                        }
                                        // if it don't have "a" tag name
                                        else
                                            replaceSelection("a", setdatalink, "");

                                    }
                                    else
                                        linkinput.val(thisHrefLink).focus();

                                    // the method of displaying-hiding to link-types
                                    linktypeselect.click(function (e) {
                                        if ($(e.target).hasClass(vars.css + "_linktypetext") || $(e.target).hasClass(vars.css + "_linktypearrow"))
                                            linktypeSwitch(true);
                                    });

                                    // the method of selecting to link-types
                                    linktypes.find("a").click(function () {
                                        var thisLinkType = $(this).attr(vars.css + "-linktype");

                                        linktypes.data("linktype", thisLinkType)

                                        linktypeview.find("." + vars.css + "_linktypetext").html(linktypes.find('a:eq(' + linktypes.data("linktype") + ')').text());

                                        linkInputSet(thisHrefLink);

                                        linktypeSwitch();
                                    });

                                    linkInputSet(thisHrefLink);

                                    // the method of link-input
                                    linkinput
                                        // auto focus
                                        .focus()
                                        // update to value
                                        .val(thisHrefLink)
                                        // the event of key to enter in link-input
                                        .bind("keypress keyup", function (e) {
                                            if (e.keyCode == 13) {
                                                linkRecord(jQTE.find("[" + setdatalink + "]"));
                                                return false;
                                            }
                                        });

                                    // the event of click link-button
                                    linkbutton.click(function () {
                                        linkRecord(jQTE.find("[" + setdatalink + "]"));
                                    });
                                }
                                else
                                // hide the link-form-field
                                    linkAreaSwitch(false);
                            }

                            function linkRecord(thisSelection) {
                                // focus to link-input
                                linkinput.focus();

                                // select to the selected node
                                selectText(thisSelection);

                                // remove pre-link attribute (mark as "link will be added") of the selected node
                                thisSelection.removeAttr(setdatalink);

                                // if not selected to link-type of picture
                                if (linktypes.data("linktype") != "2")
                                    selectionSet("createlink", linkinput.val()); // insert link url of link-input to the selected node
                                // if selected to link-type of picture
                                else {
                                    selectionSet("insertImage", linkinput.val()); // insert image url of link-input to the selected node

                                    // the method of all pictures in the editor
                                    editor.find("img").each(function () {
                                        var emptyPrevLinks = $(this).prev("a");
                                        var emptyNextLinks = $(this).next("a");

                                        // if "a" tags of the front and rear of the picture is empty, remove
                                        if (emptyPrevLinks.length > 0 && emptyPrevLinks.html() == "")
                                            emptyPrevLinks.remove();
                                        else if (emptyNextLinks.length > 0 && emptyNextLinks.html() == "")
                                            emptyNextLinks.remove();
                                    });
                                }

                                // hide the link-form-field
                                linkAreaSwitch();

                                // export contents of the text to the sources
                                editor.trigger("change");
                            }

                            // the function of switching link-form-field
                            function linkAreaSwitch(status) {
                                // remove all pre-link attribute (mark as "link will be added")
                                clearSetElement("[" + setdatalink + "]:not([href])");
                                jQTE.find("[" + setdatalink + "][href]").removeAttr(setdatalink);

                                if (status) {
                                    toolbar.data("linkOpened", true);
                                    linkform.show();
                                }
                                else {
                                    toolbar.data("linkOpened", false);
                                    linkform.hide();
                                }

                                linktypeSwitch();
                            }

                            // the function of switching link-type-selector
                            function linktypeSwitch(status) {
                                if (status)
                                    linktypes.show();
                                else
                                    linktypes.hide();
                            }

                            // the function of updating the link-input according to the link-type
                            function linkInputSet(thisHrefLink) {
                                var currentType = linktypes.data("linktype");

                                // if selected type of e-mail
                                if (currentType == "1" && (linkinput.val() == "http://" || linkinput.is("[value^=http://]") || !linkinput.is("[value^=mailto]")))
                                    linkinput.val("mailto:");
                                else if (currentType != "1" && !linkinput.is("[value^=http://]"))
                                    linkinput.val("http://");
                                else
                                    linkinput.val(thisHrefLink);
                            }

                            // the function of adding style to selected text
                            function selected2style(styleCommand) {
                                if (!toolbar.data("sourceOpened")) {

                                    // if selected to changing the font-size value
                                    if (styleCommand == "fSize")
                                        styleField = fsizebar;

                                    // if selected to changing the text-color value
                                    else if (styleCommand == "colors")
                                        styleField = cpalette;

                                    // display the style-field
                                    styleFieldSwitch(styleField, true);

                                    // the event of click to style button
                                    styleField.find("a").unbind("click").click(function () {
                                        var styleValue = $(this).attr(vars.css + "-styleval"); // the property of style value to be added

                                        // if selected to changing the font-size value
                                        if (styleCommand == "fSize") {
                                            styleType = "font-size";
                                            styleValue = styleValue + vars.funit; // combine the value with size unit
                                        }
                                        // if selected to changing the text-color value
                                        else if (styleCommand == "colors") {
                                            styleType = "color";
                                            styleValue = "rgb(" + styleValue + ")"; // combine color value with rgb
                                        }

                                        var prevStyles = refuseStyle(styleType); // affect styles to child tags (and extract to the new style attributes)

                                        // change to selected text
                                        replaceSelection("span", "style", styleType + ":" + styleValue + ";" + prevStyles);

                                        // hide all style-fields
                                        styleFieldSwitch("", false);

                                        // remove title bubbles
                                        $('.' + vars.css + '_title').remove();

                                        // export contents of the text to the sources
                                        editor.trigger("change");
                                    });

                                }
                                else
                                // hide the style-field
                                    styleFieldSwitch(styleField, false);

                                // hide the link-form-field
                                linkAreaSwitch(false);
                            }

                            // the function of switching the style-field
                            function styleFieldSwitch(styleField, status) {
                                var mainData = "", // the style data of the actual wanted
                                    allData = [
                                        {"d":"fsizeOpened", "f":fsizebar},
                                        {"d":"cpallOpened", "f":cpalette}
                                    ]; // all style datas

                                // if the style data of the actual wanted isn't empty
                                if (styleField != "") {
                                    // return to all datas and find the main data
                                    for (var si = 0; si < allData.length; si++) {
                                        if (styleField == allData[si]["f"])
                                            mainData = allData[si];
                                    }
                                }
                                // display the style-field
                                if (status) {
                                    toolbar.data(mainData["d"], true); // stil seçme alanının açıldığını belirten parametre yaz
                                    mainData["f"].slideDown(100); // stil seçme alanını aç

                                    // return to all datas and close the fields of external datas
                                    for (var si = 0; si < allData.length; si++) {
                                        if (mainData["d"] != allData[si]["d"]) {
                                            toolbar.data(allData[si]["d"], false);
                                            allData[si]["f"].slideUp(100);
                                        }
                                    }
                                }
                                // hide all style-fields
                                else {
                                    // return to all datas and close all style fields
                                    for (var si = 0; si < allData.length; si++) {
                                        toolbar.data(allData[si]["d"], false);
                                        allData[si]["f"].slideUp(100);
                                    }
                                }
                            }

                            // the function of removing all pre-link attribute (mark as "link will be added")
                            function clearSetElement(elem) {
                                jQTE.find(elem).each(function () {
                                    $(this).before($(this).html()).remove();
                                });
                            }

                            // the function of refusing some styles
                            function refuseStyle(refStyle) {
                                var selectedTag = getSelectedNode(); // the selected node

                                // if the selected node have attribute of "style" and it have unwanted style
                                if (selectedTag && selectedTag.is("[style]") && selectedTag.css(refStyle) != "") {
                                    var refValue = selectedTag.css(refStyle); // first get key of unwanted style

                                    selectedTag.css(refStyle, ""); // clear unwanted style

                                    var cleanStyle = selectedTag.attr("style"); // cleaned style

                                    selectedTag.css(refStyle, refValue); // add unwanted style to the selected node again

                                    return cleanStyle; // print cleaned style
                                }
                                else
                                    return "";
                            }

                            // the function of adding style to selected text
                            function selected2format() {
                                formatFieldSwitch(true);

                                formatbar.find("a").click(function () {
                                    $("*", this).click(function (e) {
                                        e.preventDefault();
                                        return false;
                                    });

                                    formatLabelView($(this).text());

                                    var formatValue = $(this).attr(vars.css + "-formatval"); // the type of format value

                                    // convert to selected format
                                    selectionSet("formatBlock", '<' + formatValue + '>');

                                    formatFieldSwitch(false);
                                });
                            }

                            // the function of switching the style-field
                            function formatFieldSwitch(status) {
                                var thisStatus = status ? true : false;

                                thisStatus = status && formatbar.data("status") ? true : false;

                                if (thisStatus || !status)
                                    formatbar.data("status", false).slideUp(200);
                                else
                                    formatbar.data("status", true).slideDown(200);
                            }

                            // change format label
                            function formatLabelView(str) {
                                var formatLabel = formatbar.closest("." + vars.css + "_tool").find("." + vars.css + "_tool_label").find("." + vars.css + "_tool_text");

                                if (str.length > 10)
                                    str = str.substr(0, 7) + "...";

                                // change format label of button
                                formatLabel.html(str);
                            }

                            // the function of insertion a specific form to texts
                            function extractToText(strings) {
                                var $htmlContent, $htmlPattern, $htmlReplace;

                                // first remove to unnecessary gaps
                                $htmlContent = strings.replace(/\n/gim, '').replace(/\r/gim, '').replace(/\t/gim, '').replace(/&nbsp;/gim, ' ');

                                $htmlPattern = [
                                    /\<span(|\s+.*?)><span(|\s+.*?)>(.*?)<\/span><\/span>/gim, // trim nested spans
//                                    /<(\w*[^p])\s*[^\/>]*>\s*<\/\1>/gim, // remove empty or white-spaces tags (ignore paragraphs (<p>) and breaks (<br>))
//                                    [ COMMENT BCZ REMOVE THE <B> TAG -- ASHISH]

                                    /\<div(|\s+.*?)>(.*?)\<\/div>/gim, // convert div to p
                                    /\<strong(|\s+.*?)>(.*?)\<\/strong>/gim, // convert strong to b
                                    /\<em(|\s+.*?)>(.*?)\<\/em>/gim // convert em to i
                                ];

                                $htmlReplace = [
                                    '<span$2>$3</span>',
//                                    '',   //[ COMMENT BCZ REMOVE THE <B> TAG -- ASHISH]
                                    '<p$1>$2</p>',
                                    '<b$1>$2</b>',
                                    '<i$1>$2</i>'
                                ];

                                // repeat the cleaning process 5 times
                                for (c = 0; c < 5; c++) {
                                    // create loop as the number of pattern
                                    for (var i = 0; i < $htmlPattern.length; i++) {
                                        var pattern = $htmlPattern[i];
                                        var replaceby = $htmlReplace[i];
                                        $htmlContent = $htmlContent.replace(pattern, replaceby);
                                    }
                                }

                                // if paragraph is false (<p>), convert <p> to <br>
                                if (!vars.p)
                                    $htmlContent = $htmlContent.replace(/\<p(|\s+.*?)>(.*?)\<\/p>/ig, '<br/>$2');

                                // if break is false (<br>), convert <br> to <p>
                                if (!vars.br) {
                                    $htmlPattern = [
                                        /\<br>(.*?)/ig,
                                        /\<br\/>(.*?)/ig
                                    ];

                                    $htmlReplace = [
                                        '<p>$1</p>',
                                        '<p>$1</p>'
                                    ];

                                    // create loop as the number of pattern (for breaks)
                                    for (var i = 0; i < $htmlPattern.length; i++) {
                                        $htmlContent = $htmlContent.replace($htmlPattern[i], $htmlReplace[i]);
                                    }
                                }

                                // if paragraph and break is false (<p> && <br>), convert <p> to <div>
                                if (!vars.p && !vars.br)
                                    $htmlContent = $htmlContent.replace(/\<p>(.*?)\<\/p>/ig, '<div>$1</div>');

                                return $htmlContent;
                            }

                            // the function of exporting contents of the text field to the source field (to be the standard in all browsers)
                            function postToSource() {
                                // clear unnecessary tags when editor view empty
                                var sourceStrings = editor.text() == "" && editor.html().length < 12 ? "" : editor.html();
                                var value = extractToText(sourceStrings);
                                thisElement.val(value);
                            }

                            // the function of exporting contents of the source field to the text field (to be the standard in all browsers)
                            function postToEditor() {
                                editor.html(extractToText(thisElement.val()));
                            }

                            // the function of getting parent (or super parent) tag name of the selected node
                            function detectElement(tags) {

                                var resultdetect = false, $node = getSelectedNode(), parentsTag;

                                if ($node) {
                                    $.each(tags, function (i, val) {
                                        parentsTag = $node.prop('tagName').toLowerCase();

                                        if (parentsTag == val)
                                            resultdetect = true;
                                        else {
                                            $node.parents().each(function () {
                                                parentsTag = $(this).prop('tagName').toLowerCase();
                                                if (parentsTag == val)
                                                    resultdetect = true;
                                            });
                                        }
                                    });

                                    return resultdetect;
                                }
                                else
                                    return false;
                            }

                            ;

                            // the function of highlighting the toolbar buttons according to the cursor position in jqte editor
                            function buttonEmphasize(e) {
                                for (var n = 0; n < buttons.length; n++) {
                                    if (vars[buttons[n].name] && buttons[n].emphasis && buttons[n].tag != '')
                                        detectElement(buttons[n].tag) ? toolbar.find('.' + vars.css + '_tool_' + buttons[n].cls).addClass(emphasize) : $('.' + vars.css + '_tool_' + buttons[n].cls).removeClass(emphasize);
                                }
                                // showing text format
                                if (vars.format && angular.isArray(vars.formats)) {
                                    var isFoundFormat = false;

                                    for (var f = 0; f < vars.formats.length; f++) {
                                        var thisFormat = [];
                                        thisFormat[0] = vars.formats[f][0];

                                        if (vars.formats[f][0].length > 0 && detectElement(thisFormat)) {
                                            formatLabelView(vars.formats[f][1]);

                                            isFoundFormat = true;
                                            break;
                                        }
                                    }

                                    if (!isFoundFormat)
                                        formatLabelView(vars.formats[0][1]);
                                }

                                // hide all style-fields
                                styleFieldSwitch("", false);
                                formatFieldSwitch(false);
                            }

                            // the event of click to the toolbar buttons
                            toolbutton
                                .unbind("click")
                                .click(function (e) {
                                    // if source button is clicked
                                    if ($(this).data('command') == 'displaysource' && !toolbar.data("sourceOpened")) {
                                        // hide all the toolbar buttons (except the source button)
                                        toolbar.find("." + vars.css + "_tool").addClass(vars.css + "_hiddenField");
                                        $(this).removeClass(vars.css + "_hiddenField");

                                        // update to data of source displaying
                                        toolbar.data("sourceOpened", true);

                                        // equalize height of the text field with height of the source field
                                        thisElement.css("height", editor.outerHeight());

                                        sourceField.removeClass(vars.css + "_hiddenField");
                                        editor.addClass(vars.css + "_hiddenField");
                                        thisElement.focus();

                                        // hide the link-form-field
                                        linkAreaSwitch(false);

                                        // hide all style-fields
                                        styleFieldSwitch("", false);

                                        // hide format field
                                        formatFieldSwitch();

                                        // hide placeholder
                                        if (vars.placeholder && vars.placeholder != "")
                                            placeHolder.hide();
                                    }
                                    // if other buttons is clicked
                                    else {
                                        // if source field is closed
                                        if (!toolbar.data("sourceOpened")) {
                                            // if insert-link-button is clicked
                                            if ($(this).data('command') == 'linkcreator') {
                                                if (!toolbar.data("linkOpened"))
                                                    selected2link();
                                                else {
                                                    // hide the link-form-field
                                                    linkAreaSwitch(false);

                                                    // hide format field
                                                    formatFieldSwitch(false);
                                                }
                                            }

                                            // if the format button is clicked
                                            else if ($(this).data('command') == 'formats') {
                                                if ($(this).data('command') == 'formats' && !$(e.target).hasClass(vars.css + "_format"))
                                                    selected2format();

                                                // hide all style-fields
                                                styleFieldSwitch("", false);

                                                if (editor.not(":focus"))
                                                    editor.focus();
                                            }

                                            // if the style buttons are clicked
                                            else if ($(this).data('command') == 'fSize' || $(this).data('command') == 'colors') {
                                                if (
                                                    ($(this).data('command') == 'fSize' && !$(e.target).hasClass(vars.css + "_fontsize")) || // the font-size button
                                                        ($(this).data('command') == 'colors' && !$(e.target).hasClass(vars.css + "_color")) // the color button
                                                    )
                                                    selected2style($(this).data('command'));

                                                // hide format field
                                                formatFieldSwitch(false);

                                                if (editor.not(":focus"))
                                                    editor.focus();
                                            }

                                            // if other buttons is clicked
                                            else {
                                                // first, prevent to conflict of different jqte editors
                                                if (editor.not(":focus"))
                                                    editor.focus();

                                                // apply command of clicked button to the selected text
                                                selectionSet($(this).data('command'), null);

                                                // hide all menu-fields
                                                styleFieldSwitch("", false);
                                                formatFieldSwitch(false);
                                                linktypeSwitch();

                                                // to highlight the toolbar buttons according to the cursor position in jqte editor
                                                $(this).data('emphasis') == true && !$(this).hasClass(emphasize) ? $(this).addClass(emphasize) : $(this).removeClass(emphasize);

                                                sourceField.addClass(vars.css + "_hiddenField");
                                                editor.removeClass(vars.css + "_hiddenField");
                                            }

                                        }
                                        // hide the source field and display the text field
                                        else {
                                            // update to data of source hiding
                                            toolbar.data("sourceOpened", false);

                                            // display all the toolbar buttons
                                            toolbar.find("." + vars.css + "_tool").removeClass(vars.css + "_hiddenField");

                                            sourceField.addClass(vars.css + "_hiddenField");
                                            editor.removeClass(vars.css + "_hiddenField");
                                        }

                                        if (vars.placeholder && vars.placeholder != "")
                                            editor.html() != "" ? placeHolder.hide() : placeHolder.show();
                                    }

                                    // export contents of the text to the sources
                                    editor.trigger("change");
                                })
                                // the event of showing to the title bubble when mouse over of the toolbar buttons
                                .hover(function (e) {
                                    if (vars.title && $(this).data("title") != "" && ( $(e.target).hasClass(vars.css + "_tool") || $(e.target).hasClass(vars.css + "_tool_icon") )) {
                                        $('.' + vars.css + '_title').remove();

                                        // create the title bubble
                                        jQTE.append('<div class="' + vars.css + '_title"><div class="' + vars.css + '_titleArrow"><div class="' + vars.css + '_titleArrowIcon"></div></div><div class="' + vars.css + '_titleText">' + $(this).data("title") + '</div></div>');

                                        var thisTitle = $('.' + vars.css + '_title:first');
                                        var thisArrow = thisTitle.find('.' + vars.css + '_titleArrowIcon');
                                        var thisPosition = $(this).position();
                                        var thisAlignX = thisPosition.left + $(this).outerWidth() - (thisTitle.outerWidth() / 2) - ($(this).outerWidth() / 2);
                                        var thisAlignY = (thisPosition.top + $(this).outerHeight() + 5);

                                        // show the title bubble and set to its position
                                        thisTitle.delay(400).css({'top':thisAlignY, 'left':thisAlignX}).fadeIn(200);
                                    }
                                }, function () {
                                    $('.' + vars.css + '_title').remove();
                                });

                            // prevent multiple calling postToSource()
                            var editorChangeTimer = null;


                            // the methods of the TEXT FIELD [ RICH TEXT AREA ]

                            editor
                                .bind("keypress keyup keydown drop cut copy paste DOMCharacterDataModified DOMSubtreeModified", function () {   // trigger change method of the text field when the text field modified
                                if (!toolbar.data("sourceOpened"))                                     // export contents of the text to the sources
                                    $(this).trigger("change");
                                linktypeSwitch();                                                 // hide the link-type-field

                                if ($.isFunction(vars.change))                                       // if the change method is added run the change method
                                    vars.change();

                                if (vars.placeholder && vars.placeholder != "")                     // the feature of placeholder
                                    $(this).text() != "" ? placeHolder.hide() : placeHolder.show();
                            })
                                .bind("change", function () {
                                    if (!toolbar.data("sourceOpened")) {
                                        clearTimeout(editorChangeTimer);
                                        editorChangeTimer = setTimeout(postToSource, 0);
                                    }
                                })

                                // run to keyboard shortcuts
                                .keydown(function (e) {
                                    // if ctrl key is clicked
                                    if (e.ctrlKey) {
                                        // check all toolbar buttons
                                        for (var n = 0; n < buttons.length; n++) {
                                            // if this settings of this button is activated (is it true)
                                            // if the keyed button with ctrl is same of hotkey of this button
                                            if (vars[buttons[n].name] && e.keyCode == buttons[n].key.charCodeAt(0)) {
                                                if (buttons[n].command != '' && buttons[n].command != 'linkcreator')
                                                    selectionSet(buttons[n].command, null);

                                                else if (buttons[n].command == 'linkcreator')
                                                    selected2link();

                                                return false;
                                            }
                                        }
                                    }
                                })


                                .bind("mouseup keyup", buttonEmphasize)// method of triggering to the highlight button

                                .focus(function () {                                    // the event of focus to the text field
                                    if ($.isFunction(vars.focus))                       // if the focus method is added run the focus method
                                        vars.focus();

                                    jQTE.addClass(vars.css + "_focused");               // add onfocus class

                                    if (/opera/.test(thisBrowser)) {                    // prevent focus problem on opera
                                        var range = document.createRange();
                                        range.selectNodeContents(editor[0]);
                                        range.collapse(false);
                                        var selection = window.getSelection();
                                        selection.removeAllRanges();
                                        selection.addRange(range);
                                    }
                                })

                                .focusout(function () {
                                    // the event of focus out from the text field
                                    toolbutton.removeClass(emphasize);                      // remove to highlights of all toolbar buttons

                                    styleFieldSwitch("", false);                            // hide all menu-fields
                                    formatFieldSwitch(false);
                                    linktypeSwitch();

                                    if ($.isFunction(vars.blur))                           // if the blur method is added run the blur method
                                        vars.blur();

                                    jQTE.removeClass(vars.css + "_focused");               // remove onfocus class

                                    if (angular.isArray(vars.formats))                    // show default text format
                                        formatLabelView(vars.formats[0][1]);

                                    AppUtil.putDottedValue($scope.row, $scope.colmetadata.expression, thisElement.val());
                                    if (!$scope.$$phase) {
                                        $scope.$apply();
                                    }

                                });


                            thisElement// the event of key in the SOURCE FIELD [ TEXTAREA FIELD ]
                                .bind("keydown keyup", function () {
                                setTimeout(postToEditor, 0);                                       // export contents of the source to the text field
//                        $(this).height($(this)[0].scrollHeight);                                 // auto extension for the source field
//                        if ($(this).val() == "") {                                               // if the source field is empty, shorten to the source field
//                            $(this).height(0);
//                        }
                            })
                                .focus(function () {
                                    jQTE.addClass(vars.css + "_focused");                         // add onfocus class
                                })
                                .focusout(function () {
                                    jQTE.removeClass(vars.css + "_focused");                     // remove onfocus class
                                    AppUtil.putDottedValue($scope.row, $scope.colmetadata.expression, thisElement.val());
                                    if (!$scope.$$phase) {
                                        $scope.$apply();
                                    }
                                });
                        });
                    };


                    $scope.richTextBoxEditor();

                }
            }
        }
    }
}]);

/*******************************END of RTE********************************************/
/*****************************Auto Resize Text Area************************/

appStrapDirectives.directive("appTextArea", ["$compile" , function ($compile) {
    return {
        restrict:'E',
        template:"<div></div>",
        scope:true,
        replace:true,
        compile:function () {
            return{
                post:function ($scope, iElement, attrs) {

                    var modelExp = attrs.model;
                    var fieldExp = attrs.field;
                    var toBind = modelExp + "." + fieldExp;

                    var textareaTemplate = '<textarea class="app-auto-resize-teztarea" ng-model="' + toBind + '"></textarea>' +
                        '<div ng-bind="' + toBind + '" class="app-auto-resize-div"></div>';

                    $(iElement).append($compile(textareaTemplate)($scope));

                    AppUtil.rebindFieldExpression($scope, modelExp, fieldExp);

                    var textAreaElement = angular.element(iElement).find('textarea');
                    var $clone = angular.element(iElement).find('div');
                    $clone = $($clone);


                    var hasBoxModel = textAreaElement.css('box-sizing') == 'border-box' || textAreaElement.css('-moz-box-sizing') == 'border-box' || textAreaElement.css('-webkit-box-sizing') == 'border-box';
                    var heightCompensation = parseInt(textAreaElement.css('border-top-width')) + parseInt(textAreaElement.css('padding-top')) + parseInt(textAreaElement.css('padding-bottom')) + parseInt(textAreaElement.css('border-bottom-width'));
                    var textareaHeight = parseInt(textAreaElement.css('height'), 10);
                    var lineHeight = parseInt(textAreaElement.css('line-height'), 10) || parseInt(textAreaElement.css('font-size'), 10);
                    var minheight = lineHeight * 2 > textareaHeight ? lineHeight * 2 : textareaHeight;
                    var maxheight = parseInt(textAreaElement.css('max-height'), 10) > -1 ? parseInt(textAreaElement.css('max-height'), 10) : Number.MAX_VALUE;

                    function updateHeight() {
                        var textareaContent = textAreaElement.val().replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/&/g, '&amp;').replace(/\n/g, '<br/>');
                        setHeightAndOverflow();
                    }

                    function setHeightAndOverflow() {
                        var cloneHeight = $clone.height();
                        var overflow = 'hidden';
                        var height = hasBoxModel ? cloneHeight + lineHeight + heightCompensation : cloneHeight + lineHeight;
                        if (height > maxheight) {
                            height = maxheight;
                            overflow = 'auto';
                        } else if (height < minheight) {
                            height = minheight;
                        }
                        if (textAreaElement.height() !== height) {
                            textAreaElement.css({'overflow':overflow, 'height':height + 'px'});
                        }
                    }


                    textAreaElement.bind('keyup change cut paste', function () {
                        updateHeight();
                    });

                    textAreaElement.bind('blur', function () {
                        setHeightAndOverflow();
                    });


                    var model = AppUtil.getModel($scope, $scope.modelexpression, true);

                    if (model[$scope.fieldexpression]) {
                        textAreaElement.val(model[$scope.fieldexpression]);
                        updateHeight();
                    }

                }
            }
        }
    }
}]);


/***************************End of auto resize text area**************************************/
/*************************************Controller********************************************************************/
var app = angular.module('applane', ['$appstrap.directives', '$appstrap.services'], function ($routeProvider, $locationProvider) {
});
app.controller('AppCtrl', function ($scope, $compile, $location, $viewStack, $rootScope, $timeout, $userDataSource, $userModel, $metaDataSource, $metaDataModel) {
    AppUtil.$rootScope = $rootScope;
    $rootScope.busymessage = "...";
    $rootScope.showbusymessage = false;
    $scope.appData = {viewGroups:{label:"", "display":["label"], options:[]}, organizations:{label:"", "display":["label"], options:[]}};
    $scope.$watch('appData.userLogin', function (newValue, oldValue) {

        if (newValue === false) {
            delete localStorage.usk;
            window.location.href = "/login.html?callback=/";
        }


    }, true);
    $metaDataModel.init($scope, $metaDataSource);
    $userModel.init($scope, $userDataSource);

    $scope.logOut = function () {
        $userDataSource.logOut(function (data) {
            //destroy usk here
            $scope.appData.userLogin = false;
            if (!$scope.$$phase) {
                $scope.$apply();
            }

        })
    }

    $scope.onQuickViewSelection = function (quickView) {

        $viewStack.addView({"viewid":quickView.viewid.viewid, "applicationid":quickView.applicationid.id, "ask":quickView.applicationid.oauthkey, "osk":$scope.currentorganization.oauthkey}, $scope);

    }

    $scope.getColumnValue = function (row, col, asHTML) {
        var val;
        if (asHTML) {
            val = "&nbsp;";
        } else {
            val = "";
        }

        var exp = col.expression;
        var colValue = AppUtil.resolveDot(row, exp, false)
        if (row && exp && colValue !== undefined) {
            var type = col.type;
            var multiple = col.multiple;
            if (type == 'lookup') {

                if (multiple) {
                    val = colValue;
                    var expression = col.primarycolumns[0].expression;
                    if (angular.isArray(val)) {
                        if (val && val.length > 0) {
                            var valTemp = '';

                            var lastIndex = val.length - 1;
                            for (var i = 0; i < val.length; i++) {
                                valTemp += val[i][expression];
                                if (i != lastIndex) {
                                    valTemp += ' ; ';
                                }
                            }
                            val = valTemp;
                        } else {
                            val = '&nbsp;';
                        }
                    }
                } else {
                    val = colValue;
                    if (angular.isObject(val)) {
                        val = val[col.primarycolumns[0].expression];
                    }
                }
            } else if (col.multiple) {
                val = colValue;
                if (angular.isArray(val)) {
                    if (val && val.length > 0) {
                        var valTemp = '';

                        var lastIndex = val.length - 1;
                        for (var i = 0; i < val.length; i++) {
                            valTemp += val[i];
                            if (i != lastIndex) {
                                valTemp += ' ; ';
                            }
                        }
                        val = valTemp;
                    } else {
                        val = '&nbsp;';
                    }
                }


            } else if (type == 'file') {

                if (colValue.length > 0) {
                    var url = BAAS_SERVER + '/file/download?filekey=' + colValue[0][FILE_KEY] + '&ask=frontend';
                    val = "<a tabindex='-1' href='" + url + "'>" + colValue[0][FILE_NAME] + "</a>";
                }


            } else if (type == 'datefilter') {
                val = colValue;

            } else if (type == 'string' || type == 'text' || type == 'boolean' || type == 'number' || type == 'decimal' || type == 'date') {

                val = colValue;
                if (multiple && val instanceof Array) {
                    val = JSON.stringify(val);
                }
            } else if (type == 'object') {
                val = colValue;
                if (val instanceof Object) {
                    val = JSON.stringify(val);
                }
            } else if (type == 'duration') {

                if (colValue.time !== undefined || colValue.timeunit !== undefined) {
                    val = "";
                    if (colValue.time !== undefined) {
                        val = colValue.time;
                    }
                    if (colValue.timeunit !== undefined) {
                        val += " " + colValue.timeunit;
                    }
                }


            } else if (type == CURRENCY_TYPE) {

                if (colValue.amount !== undefined || colValue.type !== undefined) {
                    val = "";
                    if (colValue.amount !== undefined) {
                        val = colValue.amount;
                    }
                    if (colValue.type && colValue.type.currency) {
                        val += " " + colValue.type.currency;
                    }
                }
            } else if (type == 'schedule') {
                val = colValue.duedate;
            } else {
                val = JSON.stringify(colValue)
            }
        }

        return val;
    }
});
/*************************************END Controller********************************************************************/

