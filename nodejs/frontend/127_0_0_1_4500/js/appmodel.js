var appModule = angular.module('$appModule', []);
var BAAS_SERVER;
var windowUrl = (window.location.href);
if (windowUrl.indexOf("127.0.0.1") >= 0 || windowUrl.indexOf("localhost") >= 0) {
    BAAS_SERVER = "http://127.0.0.1:1337";
} else {
    BAAS_SERVER = "http://173.255.119.199:1337";
}
var FOCUS_ELEMENT_CLASS = 'focus-element';

/*************************************App Data Source*******************************************/
appModule.factory('$appDataSource', ['$http', '$timeout', '$rootScope',
    function ($http, $timeout, $rootScope) {
        var $appDataSource = {
        };
        $appDataSource.init = function () {

        }

        $appDataSource.getAppData = function (usk, state, callBack, errorCallBack) {
            this.getDataFromService(BAAS_SERVER + "/custom/module", {"usk":usk, "ask":"appsstudio", "module":"UserService", "method":"getUserState", parameters:JSON.stringify({"state":state, "usk":usk})}, "GET", "JSON", "Loading...", callBack, errorCallBack);
        }

        $appDataSource.getDataFromService = function (url, requestBody, callType, dataType, pBusyMessage, callback, errcallback) {

            var usk;
            if (typeof(Storage) !== "undefined") {
                usk = localStorage.usk;
            }
            requestBody.usk = usk;

            if (pBusyMessage) {
                $rootScope.busymessage = pBusyMessage;
                $rootScope.showbusymessage = true
                if (!$rootScope.$$phase) {
                    $rootScope.$apply();
                }
            }


            $.ajax({
                type:callType,
                url:url,
                data:requestBody,
                crossDomain:true,
                success:function (returnData, status, xhr) {
                    $rootScope.showbusymessage = false
                    if (!$rootScope.$$phase) {
                        $rootScope.$apply();
                    }
                    callback(returnData.response ? returnData.response : returnData);


                },
                error:function (jqXHR, exception) {

                    $rootScope.showbusymessage = false
                    if (!$rootScope.$$phase) {
                        $rootScope.$apply();
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
        return $appDataSource;

    }
]);

/************************************* End of App Data Source*******************************************/

/*************************************App Data Model*******************************************/

appModule.factory('$appModel', ['$http', '$timeout',
    function ($http, $timeout) {
        var $appModel = {
        };
        $appModel.init = function ($scope, $dataSource) {
            this.$scope = $scope;
            this.$dataSource = $dataSource;
            $dataSource.init();
            var appData = $scope.appData;
            appData.viewGroups = {label:"", "display":["label"], options:[]};
            appData.organizations = {label:"", "display":["label"], options:[]}

            var usk;
            if (typeof(Storage) !== "undefined") {
                usk = localStorage.usk;
            }
            usk = "1234";
            if (!usk) {
                alert("USK is not defined");
                return;
            }
            this.usk = usk;
            var that = this;
            //apply on change listener on view group selectedIndex
            $scope.$watch("appData.viewGroups.selectedIndex", function (newValue, oldValue) {
                if (newValue != oldValue && oldValue !== undefined && newValue != undefined) {
                    var state = {organization:that.$scope.appData.organizations.options[that.$scope.appData.organizations.selectedIndex].id, viewgroup:that.$scope.appData.viewGroups.options[newValue].label};
                    that.getAppData(state);
                }
            })

            //apply on change listener on organization selectedIndex
            $scope.$watch("appData.organizations.selectedIndex", function (newValue, oldValue) {
                if (newValue != oldValue && oldValue !== undefined && newValue !== undefined) {
                    //we have to remove selectedIndex otherwise $scope will take it as value change in viewgroup
                    delete that.$scope.appData.viewGroups.selectedIndex;
                    var state = {"organization":that.$scope.appData.organizations.options[newValue].id};
                    that.getAppData(state);
                }
            })

            this.getAppData();


        };
        $appModel.getAppData = function (state) {
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
                var viewGroups = user.viewgroups;
                if (!viewGroups) {
                    viewGroups = [];
                }
                that.$scope.appData.viewGroups.options = viewGroups;
                if (viewGroups.length > 0) {
                    that.$scope.appData.viewGroups.selectedIndex = user.viewgroupindex;
                    that.$scope.appData.viewGroups.label = viewGroups[user.viewgroupindex].label;
                } else {
                    that.$scope.appData.viewGroups.label = "View groups";
                }
                that.$scope.appData.menus = user.menus;
                var view = user.view;
                that.$scope.appData.currentview = view;
                if (view) {
                    that.$scope.appData.quickviews = user.view;
                }
                if (!that.$scope.$$phase) {
                    that.$scope.$apply();
                }

            }, function (error) {
                if (error.code == 1) {
                    that.$scope.appData.userLogin = false;
                    if (!that.$scope.$$phase) {
                        that.$scope.$apply();
                    }
                } else {
                    alert(error.stack);
                }
            })

        }


        return $appModel;

    }
]);

/*************************************End of App Data Model*******************************************/
/*********************************************************************Action collection**********************************************************************************/
appModule.directive('appActionCollection', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<span>" +

            "<span ng-click='onActionCollectionClick()' class=\"app-action-collection\" ng-bind='action.label'></span>" +
            "<span class='app-action-collection-down-arrow-parent'><img ng-click='onActionCollectionClick($event)' class='app-action-collection-down-arrow-image' src='images/down_arrow_new.png'/></span>" +
            "<span ng-show='action.showsettingimage' class='app-action-collection-setting-parent'><img class='app-setting' ng-src='{{action.settingimage}}' ng-click='onSettingClick()'/></span>" +
            "</span>",

        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    $scope.onActionCollectionClick = function () {
                        var optionsHtml = "<div><app-action-collection-option  ng-repeat='option in action.options' ng-init=\"level=1\" >" +
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

appModule.directive('appActionCollectionOption', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:false,
        scope:true,
        template:"<div class='app-action-collection-option app-cursor-pointer app-white-space-nowrap app-light-gray-backgroud-color app-padding-five-px' ng-style='{paddingLeft:level*5}' ng-bind='getOptionLabel(option)' ng-click='onActionCollectionOptionClick()'>" +
            "</div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    $scope.onActionCollectionOptionClick = function () {
                        $scope.action.selectedIndex = ($scope.$index);
//                        if ($scope.action.callback) {
//                            $scope.action.callback($scope.option);
//                        }
                    }
                },
                post:function ($scope, iElement) {

                    var childs = $scope.option.childs;
                    if (childs && childs.length > 0) {
                        var optionsHtml = "<div><app-action-collection-option  ng-repeat=\"option in option.childs\" ng-init='level=level+1'>" +
                            "</app-action-collection-option></div>";
                        iElement.append($compile(optionsHtml)($scope));
                    }
                }
            }
        }
    }
}]);

/*********************************************************************Menus**********************************************************************************/
appModule.directive('appMenus', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div>" +
            "<div ng-repeat='menu in appData.menus' class='app-menu app-menu-border'>" +
            "<app-menu></app-menu>" +
            "</div> " +
            "</div>",
        compile:function () {
            return  {

            };
        }
    }
}]);

appModule.directive('appMenu', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div  class='app-menu-padding' ng-click='onMenuClick(menu)'>" +
            "<div><img ng-show='menu.imageurl' ng-src='menu.imageurl' class='app-setting'>{{ menu.label }}</div>" +
            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement) {
                    $scope.onMenuClick = function (menu) {
                    }
                }

            };
        }
    }
}]);
