//var APPS_STUDIO_SERVER = "http://127.0.0.1:5100";
//var APPS_STUDIO_SERVER = "http://127.0.0.1:5100";
var BAAS_SERVER = "http://127.0.0.1:1337";
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


'use strict';
appStrapDirectives.directive('ref', function () {
    return function (scope, elm, attrs) {
        var keyDownFn = scope.$eval(attrsn);
        elm.bind('keydown', function (evt) {

            //$apply makes sure that angular knows
            //we're changing something
            scope.$apply(function () {

                keyDownFn.call(scope, evt.which);
            });
        });
    };
});

// override the default input to update on blur


appStrapDirectives.directive('appMenus', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div>" +
            "<div ng-repeat='menu in menus' class='app-menu app-menu-border'>" +
            "<app-menu></app-menu>" +
            "</div> " +
            "</div>",
        compile:function () {
            return  {


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


appStrapDirectives.directive('appMenu', ["$compile", "$appDomService", function ($compile, $appDomService) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div  class='app-menu-padding' ng-click='onMenuClick(menu)'>" +
            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement) {
                    var menu = $scope.menu.label;
                    var imageUrl = $scope.menu.imageurl;
                    var html = '';
                    if (imageUrl) {
                        html = '<div><img src="' + imageUrl + '" class="app-setting"></div>';
                    } else if (menu != null && menu != undefined) {
                        html = "<div>" + menu + "</div>";
                    }

                    iElement.append($compile(html)($scope))


                    $scope.onMenuClick = function (menu) {

                        var table = menu.table;


                        if (table) {
                            $appDomService.addView({"viewid":table.id, "applicationid":menu.applicationid.id, "ask":menu.applicationid.ask, "osk":$scope.currentorganization.oauthkey}, $scope);
                        } else {
                            var view = menu.view;
                            if (!view) {
                                alert("No view / table defined in menu");
                                return;
                            }
                            $appDomService.addView(view, $scope);
                        }
                    }
                }

            };
        }
    }
}]);


angular.module('$strap.directives').directive('appMultipleRefrence', ['$timeout', function ($timeout) {
    'use strict';
    return {
        restrict:'E',
        scope:true,
        replace:true,
        template:"<div class='app-multirefrence-parent' ng-show='!inputCellTemplate'>" +
            "<span ng-bind='option.name' class='app-multirefrence-text'></span>" +
            "<span class='app-multirefrence-cross' ng-click='cancel()'>X</span>" +
            "</div>",
        compile:function () {
            return{
                pre:function ($scope, iElement) {

                    $scope.cancel = function () {
                        var keyTemp = $scope.option[KEY];
                        var nameTemp = $scope.option.name;
                        var options = $scope.row.businessfunction_id;
                        var length = options.length;

                        if (length > 0) {
                            for (var i = 0; i < length; i++) {
                                var indexKey = options[i][KEY];
                                var indexName = options[i].name;

                                if ((indexKey != null && indexKey == keyTemp) || (indexName != null && indexName == nameTemp)) {
                                    $scope.row.businessfunction_id.splice(i, 1);
                                    console.log("options : " + JSON.stringify($scope.row.businessfunction_id));
                                    break;
                                }
                            }
                        }
                    };

                }
            };
        }
    };
}
]);


appStrapDirectives.directive("appCalender", ["$compile", function ($compile) {
    return {
        restrict:'E',
        replace:true,
        scope:true,
        template:"<div class='input-append date datepicker parent-calender'  data-date-format='dd-mm-yyyy'>" +
            "<span class='navigating-arrows prev calender-arrows' id='prev'><img src='images/left-arrow.png'/></span>" +
            "<input ng-click='showPopUp()' size='16' type='text' class='calender-input date-input' placeholder='Select Date'/>" +
            "<span class='navigating-arrows next calender-arrows' id='next'><img src='images/right-arrow.png'/></span>" +
            "<span class='add-on show-calender-icon calender-parent'><i class='icon-th calender-parent-img' style='background-position: 0px 0px;'></i></span>" +
            "<div class='calender-drop-down'>" +
            "<ul>" +
            "<li class='date'>Date</li>" +
            "<li class='month'>Month</li>" +
            "<li class='year'>Year</li>" +
            "<li class='week'>Week</li>" +
            "</ul>" +
            "</div>" +
            "</div>",

        compile:function () {
            return  {
                post:function ($scope, iElement) {

                    $scope.showPopUp = function () {
                        angular.element(iElement).find('div').fadeIn('slow');
                    }


//                    var width = $scope.colmetadata.width;
//                    if (!width) {
//                        width = 50;
//                    }
//                    $(iElement).width(width);

                    $scope.nextElement = angular.element(iElement).find('span#next');
                    $scope.previousElement = angular.element(iElement).find('span#prev');
                    $scope.inputElement = angular.element(iElement).find('input');
                    $scope.divElement = angular.element(iElement).find('div');

                    $scope.liDateElement = angular.element(iElement).find('li.date');
                    $scope.liMonthElement = angular.element(iElement).find('li.month');
                    $scope.liYearElement = angular.element(iElement).find('li.year');
                    $scope.liWeekElement = angular.element(iElement).find('li.week');


                    $scope.selected = {dateSelected:true, monthSelected:false, yearSelected:false, weekSelected:false};

                    $scope.liDateElement.on('click', function () {
                        $scope.divElement.fadeOut('slow');
                        $scope.currentDate = new Date;

                        $scope.selected.dateSelected = true;
                        $scope.selected.weekSelected = false;
                        $scope.selected.monthSelected = false;
                        $scope.selected.yearSelected = false;

                        if ($scope.selected.dateSelected) {
                            $scope.inputElement.val($scope.currentDate);
                        }


                    });

                    $scope.liMonthElement.on('click', function () {
                        $scope.divElement.fadeOut('slow');
                        $scope.currentDate = new Date;

                        $scope.selected.monthSelected = true;
                        $scope.selected.weekSelected = false
                        $scope.selected.dateSelected = false
                        $scope.selected.yearSelected = false;


                        if ($scope.selected.monthSelected) {
                            $scope.currentMonth = $scope.currentDate.getMonth();
                            var firstDayT = new Date($scope.currentDate.getFullYear(), $scope.currentMonth, 1);
                            var lastDayT = new Date($scope.currentDate.getFullYear(), $scope.currentMonth + 1, 0);
                            $scope.inputElement.val(firstDayT.getMonth());
                        }
                    });

                    $scope.liYearElement.on('click', function () {
                        $scope.divElement.fadeOut('slow');
                        $scope.currentDate = new Date;


                        $scope.selected.yearSelected = true;
                        $scope.selected.weekSelected = false;
                        $scope.selected.monthSelected = false;
                        $scope.selected.dateSelected = false;


                        if ($scope.selected.yearSelected) {
                            $scope.currentYear = $scope.currentDate.getFullYear();
                            $scope.inputElement.val($scope.currentYear);
                        }
                    });

                    $scope.liWeekElement.on('click', function () {
                        $scope.divElement.fadeOut('slow');
                        $scope.currentDate = new Date;


                        $scope.selected.weekSelected = true;
                        $scope.selected.dateSelected = false;
                        $scope.selected.monthSelected = false;
                        $scope.selected.yearSelected = false;


                        if ($scope.selected.weekSelected) {
                            var first = $scope.currentDate.getDate() - $scope.currentDate.getDay();
                            var last = first + 6;
                            $scope.firstDay = new Date($scope.currentDate.setDate(first));
                            $scope.lastDay = new Date($scope.currentDate.setDate(last));

                            $scope.inputElement.val($scope.firstDay + ' ---- ' + $scope.lastDay);
                        }
                    });

                    $scope.nextElement.on('click', function () {

                        if ($scope.selected.weekSelected) {
                            $scope.currentDate = $scope.firstDay;
                            var first = $scope.currentDate.getDate() + 7;
                            var last = first + 6;
                            $scope.firstDay = new Date($scope.currentDate.setDate(first));
                            $scope.lastDay = new Date($scope.currentDate.setDate(last));

                            $scope.inputElement.val($scope.firstDay + ' --- ' + $scope.lastDay);
                        } else if ($scope.selected.dateSelected) {
                            $scope.currentDate = new Date($scope.currentDate.setDate($scope.currentDate.getDate() + 1));
                            $scope.inputElement.val($scope.currentDate);
                        } else if ($scope.selected.monthSelected) {
                            $scope.currentMonth += 1;
                            var firstDayTemp = new Date($scope.currentDate.getFullYear(), $scope.currentMonth, 1);
                            var lastDayTemp = new Date($scope.currentDate.getFullYear(), $scope.currentMonth + 1, 0);
                            $scope.inputElement.val(firstDayTemp);
                        } else if ($scope.selected.yearSelected) {
                            $scope.currentDate.setFullYear($scope.currentDate.getFullYear() + 1);
                            $scope.currentYear = $scope.currentDate.getFullYear();
                            $scope.inputElement.val($scope.currentYear);
                        }
                    });

                    $scope.previousElement.on('click', function () {

                        if ($scope.selected.weekSelected) {
                            $scope.currentDate = $scope.firstDay;
                            var first = $scope.currentDate.getDate() - 7;
                            var last = first + 6;
                            $scope.firstDay = new Date($scope.currentDate.setDate(first));
                            $scope.lastDay = new Date($scope.currentDate.setDate(last));
                            $scope.inputElement.val($scope.firstDay + ' ---- ' + $scope.lastDay);

                        } else if ($scope.selected.dateSelected) {
                            $scope.currentDate = new Date($scope.currentDate.setDate($scope.currentDate.getDate() - 1));  //  for previous date
                            $scope.inputElement.val($scope.currentDate);

                        } else if ($scope.selected.monthSelected) {
                            $scope.currentMonth -= 1;
                            var firstDayT = new Date($scope.currentDate.getFullYear(), $scope.currentMonth, 1);
                            var lastDayT = new Date($scope.currentDate.getFullYear(), $scope.currentMonth + 1, 0);
                            $scope.inputElement.val(firstDayT);


                        } else if ($scope.selected.yearSelected) {
                            $scope.currentDate.setFullYear($scope.currentDate.getFullYear() - 1);
                            $scope.currentYear = $scope.currentDate.getFullYear();
                            $scope.inputElement.val($scope.currentYear);
                        }

                    });

                    setTimeout(function () {
                        $('.datepicker').datepicker({autoclose:true})

                    }, 0)
                }
            };
        }
    };
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


appStrapDirectives.directive('appActionCollection', ["$compile", "$dataModel", "$appDomService", function ($compile, $dataModel, $appDomService) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<span>" +

            "<span ng-click='onActionCollectionClick()' class=\"app-action-collection\" ng-bind='action.label'></span>" +
            "<span class='app-action-collection-down-arrow-parent'><img ng-click='onActionCollectionClick($event)' class='app-action-collection-down-arrow-image' src='images/down_arrow_new.png'/></span>" +
            "<span class='app-action-collection-setting-parent'><img class='app-setting' ng-src='{{action.settingimage}}' ng-click='onSettingClick()'/></span>" +
            "</span>",

        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    $scope.onActionCollectionClick = function () {

                        var optionsHtml = "<div><app-action-collection-option  ng-repeat=\"option in action.options\" ng-init=\"level=1\" >" +
                            "</app-action-collection-option></div>";

                        var popup = {
                            template:optionsHtml,
                            scope:$scope.$new(),
                            hideonclick:true,
                            element:iElement,
                            width:$(iElement).width()
                        };
                        $appDomService.showPopup(popup);

                    }
                    $scope.onSettingClick = function () {
                        $appDomService.addView($scope.action.settingview, $scope);
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

appStrapDirectives.directive('appActionCollectionOption', ["$compile", "$dataModel", "$appDomService", function ($compile, $dataModel, $appDomService) {
    return {
        restrict:"E",
        replace:false,
        scope:true,
        template:"<div class='app-action-collection-option' ng-style='{paddingLeft:level*5}' ng-bind='getOptionLabel(option)' ng-click='onActionCollectionOptionClick()'>" +
            "</div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    $scope.onActionCollectionOptionClick = function () {
                        if ($scope.action.callback) {
                            $scope.action.callback($scope.option);
                        }
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


appStrapDirectives.directive('appView', ["$compile", "$dataModel", "$appDomService", "$appService", function ($compile, $dataModel, $appDomService, $appService) {
    'use strict';
    var template = '<div class="app-view">' +
        '</div>';
    return {
        restrict:"E",
        scope:true,
        replace:true,
        template:template,
        compile:function () {
            return {
                pre:function ($scope, iElement, attrs) {
                    var componentId = attrs.componentid;
                    $scope.componentid = componentId;
                    $scope.view = $dataModel.getView(componentId);
                    $scope.childview = function ($event) {
                        $scope.setStartTime();
                        $scope.log(new Date().getTime() % 10000, true);

                        var columns = [
                            {"label":"Label", "expression":"label", "type":"text", "width":200, "visibility":"Table"},
                            {"label":"Table", "expression":"table", "type":"lookup", "columns":[
                                {"expression":"id", "type":"string"}
                            ], "width":200, "table":"tables__baas", "visibility":"Table"},
                            {"label":"Visibility", "expression":"visibility", "type":"lookup", "width":200, "options":["Both", "Table", "Panel"], "visibility":"Table"}
                        ];
                        var data = {"data":$scope.view.metadata.childs};
                        var metadata = {"label":"Manage child", "columns":columns, "parameters":{"table":$scope.view.metadata.table}, "type":"table"};
                        metadata.enablequickviewaction = false;
                        metadata.enablemetadataaction = false;
                        metadata.enablerowaction = false;

                        metadata.savecallback = function (childs) {
                            var url = BAAS_SERVER + "/custom/module";
                            var parameters = {"ask":$scope.view.ask, "customization":JSON.stringify({"childs":childs}),"viewid":$scope.view.viewid};
                            var param = {"ask":"appsstudio", "module":"customizeviewservice", "method":"saveViewCustomization","parameters":JSON.stringify(parameters)}

                            $appService.getDataFromJQuery(url, param, "GET", "JSON", "Loading...", function (data) {
                                $appDomService.addView(data, $scope);
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
                        $appDomService.addView(viewInfo, $scope);
                    }
                    $scope.columnview = function ($event) {
                        $scope.setStartTime();
                        $scope.log(new Date().getTime() % 10000, true);


                        var columns = [
                            {"label":"Up", "expression":"up", "type":"up", "width":40, "visibility":"Table"},
                            {"label":"Down", "expression":"down", "type":"down", "width":40, "visibility":"Table"},
                            {"expression":"label", "type":"string", "label":"Label", "width":100, "visibility":"Both"},
                            {"label":"Width", "expression":"width", "type":"text", "width":50, "visibility":"Table"},
                            {"label":"Visibility", "expression":"visibility", "type":"lookup", "width":100, "options":["Both", "Table", "Panel","Child","Query", "None"], "visibility":"Table"}
                        ];

                        var data = {"data":$scope.view.metadata.columnsclone};
                        var metadata = {"label":"Manage Column", "columns":columns, "type":"table"};

                        metadata.enablequickviewaction = false;
                        metadata.enablemetadataaction = false;
                        metadata.delete = false;
                        metadata.insert = false;
                        metadata.enableselection = false;
                        metadata.enablerowaction = false;
                        metadata.savecallback = function (customization) {

                            var url = BAAS_SERVER + "/custom/module";
                            var parameters = {"ask":$scope.view.ask, "customization":JSON.stringify({"columns":customization}),"viewid":$scope.view.viewid};
                            var param = {"ask":"appsstudio", "module":"customizeviewservice", "method":"saveViewCustomization","parameters":JSON.stringify(parameters)}

                            $appService.getDataFromJQuery(url, param, "GET", "JSON", "Loading...", function (data) {
                                $appDomService.addView(data, $scope);
                            });
                        };
                        metadata.closeonsave = true;


                        var viewInfo = {};
                        viewInfo.data = data;
                        viewInfo.metadata = metadata;
                        viewInfo.showas = 'popup';
                        viewInfo.width = 374;
                        viewInfo.height = 500;
                        viewInfo.element = $event.target;
                        viewInfo[PARENT_COMPONENT_ID] = $scope.componentid;

                        $appDomService.addView(viewInfo, $scope);
                    }
                    $scope.resetfilter = function () {

                    }
                    $scope.applyfilter = function () {
                        //iterate applied filters

                        var modelFilters = [];

                        var advanceFilters = $scope.view.metadata.viewfilters;
                        var count = advanceFilters.length;
                        var appliedFilters = $scope.view.metadata.appliedfilters;
                        var filterCustomization = [];
                        for (var i = 0; i < count; i++) {
                            var advanceFilter = advanceFilters[i];
                            var expression = advanceFilter.expression;

                            if ($scope.view.filterparameters[expression]) {

                                var appliedFilterCount = appliedFilters.length;
                                var appliedFilterIndex = -1;
                                for (var j = 0; j < appliedFilterCount; j++) {
                                    var appliedFilter = appliedFilters[j];
                                    if (appliedFilter.expression == advanceFilter.expression) {
                                        appliedFilterIndex = j;
                                        break;
                                    }
                                }

                                if (appliedFilterIndex < 0) {
                                    appliedFilters.push(advanceFilter);
                                }


                                var paramValue = $scope.view.filterparameters[expression];

//                                var customizationFilter = {};
//                                customizationFilter[expression] = filter;
//                                var customizationParam = {};
//                                customizationParam[expression] = paramValue;

//                                filterCustomization.push({"expression":expression, "filters":[customizationFilter], "parameters":customizationParam});
//                                filterCustomization.push({"filters":[customizationFilter], "parameters":customizationParam});
                                $dataModel.addFilter($scope[COMPONENT_ID], expression, paramValue);
                            }

                        }

                        //save filter customization


//                        var url = "http://backend.applane.com/applanestudio/" + $scope.view.applicationid + "/view/" + $scope.view.viewid + "/customization";
//                        var param = {"ask":$scope.view.ask, "osk":$scope.currentorganization.oauthkey, "customization":JSON.stringify({"appliedfilters":filterCustomization, "callback":false})};
//

//
//                        $appService.getDataFromJQuery(url, param, "POST", "JSON", false, function (data) {
//
//                        });

                        $dataModel.refresh($scope[COMPONENT_ID]);
                        if ($scope.filterPopup) {
                            $scope.filterPopup.hide();
                            delete $scope.filterPopup;
                        }
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

                                "<div  ng-repeat='advancefilter in view.metadata.viewfilters'>" +

                                "<div ng-switch on='advancefilter.type'>" +

                                "<div ng-switch-when='lookup'>" +
                                "<bs-typeahead style = \"height: 30px;\" class='applied-search-filter-input' ng-init=\"row=view.filterparameters;colmetadata=advancefilter\" onselection =\"onSelection\" ></bs-typeahead>" +
                                "</div>" +
                                "<div ng-switch-when='date'>" +
                                "<app-calender ng-init=\"colmetadata=advancefilter\"></app-calender>" +
                                "</div>" +
                                "<div ng-switch-default></div>" +
                                "</div>" +
                                "</div>" +
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

                        $scope.filterPopup = $appDomService.showPopup(config);

                    }

                    $scope.onRowActionClick = function (action) {
                        var actionType = action.type;
                        if (actionType == 'detail') {
                            var currentRow = $dataModel.getCurrentRow($scope.componentid);

                                var data = {};
                                data.data = [currentRow];
                                var viewinfo = {};
                                var panelColumns = [];
                                var columnCount = $scope.view.metadata.columnsclone.length;
                                for (var i = 0; i < columnCount; i++) {
                                    var col = $scope.view.metadata.columnsclone[i];
                                    var visibility = col.visibility;
                                    if (visibility == 'Panel' || visibility == 'Both') {
                                        panelColumns.push(col);
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
                                        panelColumns.push(childClone);
                                    }

                                }




                                var filter = {"_id":"{_id}"};
                                var parameters = angular.copy(currentRow);
                                var panelmetadata = {"table":$scope.view.metadata.table, "columns":panelColumns, "type":"panel", "layout":"onecolumns","filter":filter,"parameters":parameters};
                                panelmetadata.save = true;
                                panelmetadata.refresh = true;
                                panelmetadata.enablemetadataaction = false;


                                viewinfo.metadata = panelmetadata;

                                viewinfo.data = data;
                                viewinfo.applicationid = $scope.view.applicationid;
                                viewinfo.ask = $scope.view.ask;
                                viewinfo.osk = $scope.view.osk;
                                viewinfo[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                                $appDomService.addView(viewinfo, $scope);



                        } else if (actionType == 'view') {
                            var currentRow = $dataModel.getCurrentRow($scope[COMPONENT_ID]);

                            var clone = angular.copy(action);

                            var currentRowClone = angular.copy(currentRow);
                            var relatedColumn = clone.relatedcolumn;
                            if (relatedColumn) {
                                var filter = {};
                                filter [relatedColumn] = "{" + relatedColumn + "}";
                                currentRowClone[relatedColumn] = currentRow[$scope.view.metadata[PRIMARY_COLUMN]];   // task_id={task_id} and put current key as task_id in parameter
                                clone.filter = filter;

                            }

                            clone[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                            clone.parameters = currentRowClone;
                            clone.ask = $scope.view.ask;
                            clone.osk = $scope.view.osk;
                            clone.applicationid = $scope.view.applicationid;

                            $appDomService.addView(clone, $scope);
                        }
                    }


                    $scope.close = function (action) {
                        $appDomService.closeView($scope.componentid);
                    };

                    $scope.insert = function (action) {
                        $dataModel.insert($scope.componentid);
                    };


                    $scope.save = function () {

                        var callback = $scope.view.metadata.savecallback;
                        var closeonsave = $scope.view.metadata.closeonsave;

                        if (callback) {
                            var data = $scope.view.data.data;

                            if (closeonsave) {
                                $scope.close();
                            }
                            callback(data);
                            return;
                        }

                        $dataModel.save($scope.componentid);
                    };
                    $scope.refresh = function () {


                        var newInserts = $dataModel.getUpdates($scope.componentid);
                        var newInsertCount = newInserts.length;

                        if (newInsertCount > 0) {

                            var $confirmationScope = $scope.$new();
                            $confirmationScope.onconfirmation = function (option) {

                                $confirmationScope.confirmationPopUp.hide();

                                if (option == 'Ok') {
                                    $dataModel.refresh($scope.componentid);
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

                            $confirmationScope.confirmationPopUp = $appDomService.showPopup(modal);
                        } else {
                            $dataModel.refresh($scope.componentid);
                        }
                    };

                    $scope.deleteData = function () {
                        $dataModel.deleteData($scope.componentid);
                    };

                    $scope.loadView = function (action) {

                        var useCurrentRow = action[USER_CURRENT_ROW];
                        if (useCurrentRow) {


                        } else {
                            var clone = angular.copy(action.view);
                            clone[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                            $appDomService.addView(clone, $scope);
                        }
                    }
                    $scope.update = function (action) {
                        var resource = $dataModel.getView($scope.componentid).metadata.resource;
                        var selectedKeys = $dataModel.getSelectedKeys($scope.componentid);

                        var updateColumns = action.updatelineitems;
                        var data = {};
                        data[query.resource] = [
                            {}
                        ];


                        var metadata = {"label":action.label, "columngroups":[
                            {"columns":action.updatelineitems}
                        ], "resource":resource, "footeractions":[
                            {"type":"save"},
                            {"type":"close", "label":"Cancel"}
                        ], "type":"panel"};


                        var viewInfo = {};
                        viewInfo.data = data;
                        viewInfo.metadata = metadata;
                        viewInfo.selectedkeys = selectedKeys;
                        viewInfo.updateselectedkeys = true;
                        viewInfo.showas = 'popup';
                        viewInfo[PARENT_COMPONENT_ID] = $scope.componentid;
                        $appDomService.addView(viewInfo, $scope);
                    }
                },
                post:function ($scope, iElement) {


                    var viewType = $scope.view.metadata.type;

                    var viewTemplate;
                    if (viewType == "table") {
                        viewTemplate = "<app-grid></app-grid>";
                    } else if (viewType == "panel") {
                        viewTemplate = "<app-panel></app-panel>";
                    } else if (viewType == "group") {
                        viewTemplate = "<div class='app-bottom-position-zero app-top-position-zero app-overflow-auto app-position-absolute app-width-full'>" +
                            "<table app-group-table ng-init='rows=view.data.data' border='0' class='app-box-shadow app-width-full'></table>" +
                            "</div>";
                    } else {
                        alert("Not valid view type[" + viewType + "]");
                    }


//                    $dataModel.putView($scope.componentid, $scope.view, $scope);
                    $dataModel.setScope($scope[COMPONENT_ID], $scope);


                    var viewElement = $compile(viewTemplate)($scope);
                    iElement.append(viewElement);
                }
            }
        }
    };
}]);

appStrapDirectives.directive('appGroupTable', [ "$compile", "$dataModel", "$appDomService", "$appService",
    function ($compile, $dataModel, $appDomService, $appService) {
        'use strict';
        return {
            restrict:'A',
            scope:true,
            replace:false,
            template:"<tr app-group-table-row ng-repeat='row in rows' class='app-header-background-color app-group-tr-{{even}}' style='broder-left:1px solid white;broder-bottom:1px solid white;'>" +
                "</tr>",
            compile:function () {
                return {
                    pre:function ($scope, iElement) {

                    },
                    post:function ($scope, iElement) {

                        if ($scope.rows != null && $scope.rows != undefined) {
                            var level = $scope.rows[0].__level__;
                            if ((level % 2) == 0) {
                                $scope.even = true;
                            } else {
                                $scope.even = false;
                            }
                        }
                    }
                }
            }
        };
    }
]);
appStrapDirectives.directive('appGroupTableRow', [ "$compile", "$dataModel", "$appDomService", "$appService",
    function ($compile, $dataModel, $appDomService, $appService) {
        'use strict';
        return {
            restrict:'A',
            scope:true,
            replace:false,
            template:"<td style='padding:5px;' class='app-group-table-arrow-parent app-vertical-align-top'>" +
                "<div ng-click='expand()'  class='app-group-table-arrow-{{open}} app-float-left app-group-table-arrow'></div>" +
                "</td>" +
                "<td app-group-table-cell style='padding:5px;'>" +
                "</td>",
            compile:function () {
                return {
                    pre:function ($scope, iElement) {
                        $scope.open = false;
                        $scope.expand = function () {
                            $scope.open = !$scope.open;

                            if ($scope.open) {                  // show data
                                var childGroupTemplate = "<tr  style='line-height:20px;' class='app-header-background-color'>" +
                                    "<td style='padding:0px;'>&nbsp;</td>" +
                                    "<td style='padding:0px;'><table border='0' class='app-width-full' app-group-table ng-init='rows = row.children;'></table></td>" +
                                    "</tr>";
                                var childView = $compile(childGroupTemplate)($scope);
                                $(iElement).after(childView);
                            } else if (!$scope.open) {             // remove data
                                $(iElement).next().remove();
                            }
                        }
                    },
                    post:function ($scope, iElement) {

                    }
                }
            }
        };
    }
]);

appStrapDirectives.directive('appGroupTableCell', [ "$compile", "$dataModel", "$appDomService", "$appService",
    function ($compile, $dataModel, $appDomService, $appService) {
        'use strict';
        return {
            restrict:'A',
            scope:true,
            replace:false,
            compile:function () {
                return {
                    post:function ($scope, iElement) {
                        var template = "<span>";
                        var columns = $scope.view.metadata.groupcolumns;
                        var count = columns.length;
                        for (var i = 0; i < count; i++) {
                            var column = columns[i];
                            var val = $scope.row[column.expression];
                            if (val) {
                                template += $scope.row[column.expression] + "&nbsp;";
                            }


                        }
                        template += "</span>";
                        var cellElement = $compile(template)($scope);
                        iElement.append(cellElement);
                        var groupByColumn = $scope.view.metadata.groupbycolumn;
                        var resource = $scope.view.metadata.tablequery.resource;
                        var dataColumn = groupByColumn + "." + resource;
                        var selfDataRecords = $scope.row[dataColumn]
//                        selfDataRecords = false;
                        if (selfDataRecords) {
                            var metaData = $scope.view.metadata;
                            var selfMetadata = {"tablequery":metaData.tablequery, "columns":metaData.columns, "type":"table", "headerscroll":true}
                            var selfData = {};
                            selfData[metaData.tablequery.resource] = selfDataRecords;
                            var view = {"metadata":selfMetadata, "data":selfData};


                            var containerTemplate = "<table style='width: 100%' border='0'><tr><td class='group-self-data-cell' style='width: 100%;height: 100%'></td></tr></table>";
                            var containerTemplateCell = $compile(containerTemplate)($scope);
                            $(iElement).append(containerTemplateCell);
                            var selfDataCells = $(containerTemplateCell).find(".group-self-data-cell");
                            var selfDataCell = $(selfDataCells[0]);

                            view.element = selfDataCell;
                            view[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                            $appDomService.addView(view, $scope);


                        }


                    },
                    pre:function ($scope, iElement) {

                    }
                }
            }
        };
    }
]);


appStrapDirectives.directive('appToolBar', [ "$compile", "$dataModel", "$appDomService", "$appService",
    function ($compile, $dataModel, $appDomService, $appService) {
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

                        if ($scope.view.metadata.quickviews) {
                            var saveCallBack = function (customization) {
                                var url = "http://backend.applane.com/applanestudio/" + $scope.view.applicationid + "/view/" + $scope.view.viewid + "/customization";
                                var param = {"ask":$scope.view.ask, "osk":$scope.currentorganization.oauthkey, "customization":JSON.stringify(customization[0])};
                                $appService.getDataFromJQuery(url, param, "POST", "JSON", "Loading...", function (data) {
                                    $appDomService.addView(data, $scope);
                                });
                            }
                            var quickviewcollection = {"label":$scope.view.metadata.quickviews[$scope.view.metadata.quickviewindex].quickviewid.id, "display":["quickviewid", "id"], "callback":"onQuickViewSelection", "settingimage":"images/savequickview.png", "settingview":{"data":{"viewgroups__applanedeveloper":[]}, "metadata":{"label":"Quick view", "enablemetadataaction":false, "savecallback":saveCallBack, "closeonsave":true, "columns":[
                                {"expression":"label", "type":"text", "width":200, "visibility":"Both", "label":"label"},
                                {"expression":"apply", "type":"lookup", "width":200, "visibility":"Both", "label":"Apply for", "values":["Application", "Organization", "Self"]},
                                {"expression":"override", "type":"checkbox", "width":15, "height":20, "visibility":"Both", "label":"Override"}
                            ], "type":"panel", "tablequery":{"resource":"viewgroups__applanedeveloper"}, "panelquery":{"resource":"viewgroups__applanedeveloper"}, "layout":"onecolumns", "delete":false, "refresh":false, "insert":false}, "applicationid":"applanedeveloper", "ask":"0530db61-c658-4864-856b-6747e3a00476", "showas":"popup", "width":500, "height":200}};
                            quickviewcollection.options = $scope.view.metadata.quickviews;
                            $scope.view.metadata.quickviewcollection = quickviewcollection;
                            html += "<app-bar-quickview></app-bar-quickview>";
                        }
                        html += "<app-bar-basic></app-bar-basic>";

                        if ($dataModel.isTrueOrUndefined($scope.view.metadata.enablemetadataaction)) {
                            html += "<app-bar-metadata></app-bar-metadata>";
                        }
                        iElement.append($compile(html)($scope));

                    }
                }
            }
        };
    }
]);


appStrapDirectives.directive('appBarQuickview', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-bar-quickview'>" +
            "<div class='app-bar-quickview-wrapper'>" +
            "<app-action-collection ng-init=\"action=view.metadata.quickviewcollection\"></app-action-collection>" +
            "</div>" +
            "</div>"
    }
}]);


appStrapDirectives.directive('appBarBasic', [
    '$compile', '$dataModel',
    function ($compile, $dataModel) {
        return {
            restrict:'E',
            replace:true,
            scope:true,
            template:'<div class="app-bar-basic"></div>',
            compile:function () {
                return  {
                    pre:function ($scope, iElement) {
                        var actions = [];

                        if ($dataModel.isTrueOrUndefined($scope.view.metadata.close)) {
                            actions.push({"method":"close(action)", "label":"Close", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-close-button app-bar-button"});
                        }

                        if ($dataModel.isTrueOrUndefined($scope.view.metadata.insert)) {
                            actions.push({"method":"insert(action)", "label":"Insert", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-insert-button app-bar-button"});
                        }

                        if ($dataModel.isTrueOrUndefined($scope.view.metadata.refresh)) {
                            actions.push({"method":"refresh(action)", "label":"Refresh", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-refresh-button app-bar-button"});
                        }

                        if ($dataModel.isTrueOrUndefined($scope.view.metadata.save)) {
                            var saveAction = {"method":"save(action)", "label":"Save", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-save-button app-bar-button"};
                            saveAction.callback = $scope.view.metadata.savecallback;
                            saveAction.closeonsave = $scope.view.metadata.closeonsave;
                            actions.push(saveAction);
                        }

                        if ($dataModel.isTrueOrUndefined($scope.view.metadata.delete)) {
                            actions.push({"method":"deleteData(action)", "label":"Delete", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-delete-button app-bar-button"});
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


appStrapDirectives.directive('appBarMetadata', ["$compile", "$dataModel", "$appDomService", function ($compile, $dataModel, $appDomService) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-bar-metadata'></div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {

                    var html = '';
                    html += "<div class='app-bar-button app-filter-view-button' ng-show='view.metadata.viewfilters' ng-click=\"filterview($event)\" title='Manage filters'></div>";
                    html += "<div class='app-bar-button app-child-view-button'  ng-click=\"childview($event)\" title='Manage child'></div>";
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

            "<div class=\"app-button app-button-border app-button-margin app-button-padding app-button-shadow\">" +
            "<div ng-bind-template='{{appliedFilter.label + \":\" + getColumnValue(view.filterparameters,appliedFilter,false)}}' class='app-applied-filter-label' ></div>" +
            "<div ng-click=\"removeFilter(appliedFilter)\" class='app-remove-filter'>X</div>" +
            "</div>" +

            "</div>",


        compile:function () {
            return {
                pre:function ($scope, iElement) {

                    $scope.removeFilter = function (appliedFilter) {


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


appStrapDirectives.directive("appText", ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<input type=\"text\" ng-model='row[colmetadata.expression]' placeholder='{{colmetadata.label}}' ng-model-onblur/>"
    }
}]);

appStrapDirectives.directive("appCheckbox", ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<input class='app-height-auto' style='padding: 0px;margin: 0px;outline: none;' type=\"checkbox\" ng-model='row[colmetadata.expression]'/>"
    }
}]);

appStrapDirectives.directive("appRowAction", ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:false,
        scope:true,
        template:"<div class='app-row-action' ng-repeat='action in actions' ng-bind='action.label' ng-click='onRowActionClick(action)'></div>"
    }
}]);


appStrapDirectives.directive("appGrid", ["$compile", "$dataModel", "$appDomService", function ($compile, $dataModel, $appDomService) {

    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-grid'></div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {
                    $scope.rowActionButtonClick = function (row, $event) {
                        $dataModel.setCurrentRow($scope[COMPONENT_ID], row);
                        var rowActionScope = $scope.$new();
                        var modal = {
                            template:"<div class=\"app-popup-body\"><app-row-action  ng-init=\"actions=view.metadata.rowactions\"></app-row-action></div>",
                            scope:rowActionScope,
                            hideonclick:true,
                            width:120,
                            element:$event.target
                        }
                        $appDomService.showPopup(modal);
                    };

                    $scope.onDownClick = function (row) {
                        $dataModel.moveDown(row, $scope.componentid);
                    };

                    $scope.onUpClick = function (row) {
                        $dataModel.moveUp(row, $scope.componentid);

                    };
                },
                post:function ($scope, iElement) {
                    var rowactions = [];
//                    if (metadata.panelquery) {
                        rowactions.push({"type":"detail", "label":"View detail"});
//                    }
                    var metadata = $scope.view.metadata;


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
                        if (visibility == 'Child' ) {

                            var child = {};
                            child.viewid = column.viewid;
                            child.label = column.label;
                            child.ask= column.ask;
                            child.osk = column.osk;
                            child.type = "view";
                            child.relatedcolumn = KEY;
                            rowactions.push(child);
                        }


                    }


                    metadata.rowactions = rowactions;

                    var newColumns = [];

                    if ($dataModel.isTrueOrUndefined(metadata.enableselection)) {
                        var selectionColumn = {"visibility":"Table", "width":20, enableCellEdit:false, cellTemplate:"<input style='margin-left: 3px;' type='checkbox' ng-model='row.__selected__'/>", "type":"selection", "tabindex":-1};
                        newColumns.push(selectionColumn);
                    }

                    if (rowactions.length > 0) {
                        var rowAction = {"visibility":"Table", "width":20, enableCellEdit:false, cellTemplate:"<div style=\"width:100%;height:20px;\" ng-click='rowActionButtonClick(row,$event)' class='app-row-action-arrow'></div>"};
                        newColumns.push(rowAction);
                    }

                    var filters = [];
                    var appliedfilters = [];


                    var filterparameters = $scope.view.filterparameters;

                    for (var i = 0; i < columnCount; i++) {
                        var column = columns[i];

                        var visibility = column.visibility;
                        if (!(visibility == 'Table' || visibility == 'Both')) {
                            continue;
                        }

                        var type = column.type;
                        if (type == 'lookup') {
                            column.editableCellTemplate = "<bs-typeahead style=\"border:none;\" ng-init=\"row=row;colmetadata=col\"></bs-typeahead>";

                            var filter = angular.copy(column)
                            var expression = filter.expression;

                            if ($dataModel.isFilterApplied($scope.componentid, expression)) {
                                appliedfilters.push(filter);
                            }
                            filters.push(filter);

                        } else if (type == 'duration') {
                            column.editableCellTemplate = "<app-duration ng-init = \"colmetadata=col;\"  ></app-duration>";
                        } else if (type == 'date' || type == 'datetime') {
                            column.editableCellTemplate = "<div class='app-grid-datepicker-parent'>" +
                                "<input class='app-grid-date-picker-input' type='text' ng-init=\"colmetadata=col\"  ng-model='row[col.expression]' placeholder='{{colmetadata.label}}' app-datepicker data-date-format='dd/mm/yyyy'/>" +
                                "<input type='text' data-toggle='datepicker' class='app-grid-date-picker-calender-image' tabIndex = '-1'/>" +
                                "</div>";
                        } else if (type == 'up') {
                            column.cellTemplate = "<span ng-click='onUpClick(row)' class='app-panel-arrows-parent'><img width='15px' height='15px' src='images/up-arrow.png'></span>";
                        } else if (type == 'down') {
                            column.cellTemplate = "<span ng-click='onDownClick(row)' class='app-panel-arrows-parent'><img width='15px' height='15px' src='images/down_arrow_new.png'></span>";
                        } else if (type == 'boolean') {
                            column.editableCellTemplate = "<app-checkbox ng-init=\"colmetadata=col\" ></app-checkbox>";
                        } else {
                            column.editableCellTemplate = "<app-text style=\"border:none;padding:0px;margin:0px\" ng-init=\"colmetadata=col\" ></app-text>";
                        }
                        newColumns.push(column);
                    }

                    var zeroWidthColumn = {"cellTemplate":"", "tabindex":-1};
                    newColumns.push(zeroWidthColumn);

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


                    var toolBarTemplate = "<div class='app-tool-bar' app-tool-bar></div>";
                    var toolBarElement = $compile(toolBarTemplate)($scope);
                    iElement.append(toolBarElement);

                    var headerscroll = $scope.view.metadata.headerscroll;
                    var template;
                    if (headerscroll) {
                        template = "<div class='app-container'><div class='app-wrapper'><div applane-freeze-grid></div></div></div>";
                    } else {
                        template = "<div class='app-container'><div class='app-wrapper'><div class='app-wrapper-child' applane-grid></div></div></div>";
                    }


                    var element = $compile(template)($scope);
                    iElement.append(element);
                }
            }
        }

    }
}]);


appStrapDirectives.directive("appPanel", ["$compile", "$dataModel", function ($compile, $dataModel) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-panel'></div>",
        compile:function () {
            return {
                post:function ($scope, iElement) {
                    var metadata = $scope.view.metadata;

                    var columns = metadata.columns;

                    var layoutColumns = metadata.layout;

                    var className = '';

                    if (layoutColumns == "onecolumns") {
                        className = "app-panel-full-row";
                    } else {
                        className = "app-panel-half-row";
                    }


                    var columnCount = columns.length;
                    for (var j = 0; j < columnCount; j++) {
                        var column = columns[j];
                        if (!column.height) {
                            column.height = 30;
                        }

                        var type = column.type;

                        if (type === 'lookup') {
                            column.editableCellTemplate = "<bs-typeahead  ng-init=\"row=row;colmetadata=colmetadata;\"></bs-typeahead>";

                        } else if (type === 'duration') {
                            continue;
                            column.editableCellTemplate = "<app-duration  ng-init = \"row=row;colmetadata=colmetadata;\"></app-duration>";
                        } else if (type === 'date' || type == 'datetime') {
                            column.editableCellTemplate = '<div class="app-grid-datepicker-parent">' +
                                '<input type="text" ng-model="row[colmetadata.expression]" data-date-format="dd/mm/yyyy" app-datepicker class="app-grid-date-picker-input" style="min-height: 20px;">' +
                                '<input type="text" data-toggle="datepicker" class="app-grid-date-picker-calender-image" />' +
                                '</div>';
                        } else if (type === 'textarea') {
                            column.editableCellTemplate = '<textarea></textarea>';

                        } else if (type === 'checkbox') {
                            column.editableCellTemplate = "<app-checkbox class='app-panel-input' ng-init=\"row=row;colmetadata=colmetadata\" ></app-checkbox>";

                        } else if (type == 'view') {
                            column.editableCellTemplate = '<div style="width:100%;"><app-nested-view ng-init = \"row=row;colmetadata=colmetadata;\"></app-nested-view></div>';

                        } else {
                            column.editableCellTemplate = "<app-text class='app-panel-input' ng-init=\"row=row;colmetadata=colmetadata\" ></app-text>";
                        }
                    }




                    var dataRecords = $scope.view.data.data
                    var dataRecordCount = dataRecords.length;
                    if (dataRecordCount == 0) {
                        $dataModel.insert($scope[COMPONENT_ID]);
                    }

                    $scope.row = $scope.view.data.data[0];


                    var toolBarTemplate = "<div class='app-tool-bar' app-tool-bar></div>";
                    var toolBarElement = $compile(toolBarTemplate)($scope);
                    iElement.append(toolBarElement);


                    var html = '';


                    if (columnCount > 0) {
                        html += '<div class="app-container">' +
                            '<div class="app-wrapper">' +
                            '<div class="app-wrapper-child app-wrapper-child-overflow">';
                        html += "<div>";


                        for (var j = 0; j < columnCount; j++) {

                            var label = columns[j].label;
                            var type = columns[j].type;

                            var classNameTemp = className;

                            if (type === 'textarea') {
                                classNameTemp = "app-panel-full-row";
                            } else if (type == 'view') {
                                classNameTemp = "app-panel-full-row";
                            }

                            html += "<div class='" + classNameTemp + "'>";
                            var width = columns[j].width;
                            if (!width) {
                                width = 200;
                            }
                            width += "px";

                            if (type == 'view') {
                                html += "<app-column style=\"width:100%;\" ng-init='colmetadata=view.metadata.columns[" + j + "]'></app-column>";
                                html += "</div>";
                            } else {
                                if (label != null && label != undefined) {
                                    html += '<div class="app-panel-label">' + label + '</div>';
                                }

                                html += "<app-column style='width:" + width + "' class='app-panel-input-parent' ng-init='colmetadata=view.metadata.columns[" + j + "]'></app-column>";
                                html += "</div>";
                            }


                        }
                        html += "</div>";
                        html += "</div></div></div>";
                    }


                    iElement.append($compile(html)($scope));


                    var toolBarTemplate = "<app-bar ng-init = \"actions=view.metadata.footeractions; \" ></app-bar>";
                    var toolBarElement = $compile(toolBarTemplate)($scope);
                    iElement.append(toolBarElement);


                },
                pre:function ($scope, iElement) {

                }
            }
        }

    }
}]);


appStrapDirectives.directive('appNestedView', [
    '$compile', '$appDomService',
    function ($compile, $appDomService) {
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
                        var row = $scope.row;
                        var nestedMetaData = column.metadata;
                        nestedMetaData.headerscroll = true;
                        var expression = column.expression;


                        if (!row[expression]) {

                            row[expression] = [];

                        }
                        var nestedData = {};
                        nestedData[nestedMetaData.resource] = row[expression];
                        var view = {"metadata":nestedMetaData, "data":nestedData};
                        view[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                        view.element = iElement;
                        $appDomService.addView(view, $scope);

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


appStrapDirectives.directive('appDuration', [
    '$compile',
    function ($compile) {
        return {
            restrict:"E",
            replace:true,
            scope:true,
            template:"<div class ='duration-container' style='height:32px;border:none;' composite-data-container='true'>" +
                "<div class='duration-time-container'>" +
//                "<app-text class='duration-input' ng-init = \"row=row[colmetadata.expression];colmetadata=colmetadata.timemetadata\" ></app-text>" +
                "</div>" +
                "<div class='duration-ref-container'>" +


//                "<bs-typeahead ng-init=\"row=row[colmetadata.expression];colmetadata=colmetadata.timeunitmetadata;\" class='border-style' ></bs-typeahead>" +

                "</div>" +
                "</div>",
            compile:function () {
                return {

                    post:function ($scope, iElement) {

                        var durationValue = $scope.row[$scope.colmetadata.expression];
                        if (!durationValue) {
                            $scope.row[$scope.colmetadata.expression] = {};
                        }

                        var durationTemplate = "<app-text class='duration-input' ng-init = \"row=row[colmetadata.expression];colmetadata=colmetadata.timemetadata\" ></app-text>";
                        var durationField = ($compile(durationTemplate)($scope));
                        var divElements = iElement.find("div");
                        $(divElements[0]).append(durationField);
                        var durationTypeTemplate = "<bs-typeahead ng-init=\"row=row[colmetadata.expression];colmetadata=colmetadata.timeunitmetadata;\" class='border-style' ></bs-typeahead>";
                        var durationTypeField = $compile(durationTypeTemplate)($scope);
                        $(divElements[1]).append(durationTypeField);

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
    '$rootScope',
    '$http',
    '$timeout',
    function ($rootScope, $http, $timeout) {
        var $appService = {
            "__currentstate__":{"_CurrentTimeZone":-19800000, "_CurrentUserEmail":"rohit.bansal@daffodilsw.com", "_CurrentUserId":1, "_CurrentUserRecordLimit":90, "_CurrentUAGName":"afb", "enable_logs":true}
        };


        $appService.currentUser = function (callback) {
            var params = {};
            params.ask = this.ask;
            params.osk = this.osk;
            this.getDataFromService("/backend/task_manager/currentuser", params, "Loading...", callback);
        };


        $appService.login = function (username, password, callback) {
            var params = {};
            params.username = username;
            params.password = password;
            params.usergroup = 'apps_studio';

//            this.getDataFromService("/backend/task_manager/login", params, "Login...", callback);

            this.getDataFromJQuery(BAAS_SERVER + "/login", params, "GET", "JSON", "Loading...", function (callBackData) {
                callback(callBackData);
            }, function (jqxhr, error) {
                alert("Error:" + JSON.stringify(jqxhr));
            });
        };


        $appService.signUp = function (username, password, callback) {
            var params = {};
            params.ask = this.ask;
            params.osk = this.osk;
            params.username = username;
            params.password = password;

            //alert(JSON.stringify(params));

            this.getDataFromService("/backend/task_manager/createuser", params, "Signup...", callback);
        };


        $appService.getView = function (pView, callBack) {




            var url = BAAS_SERVER + "/custom/module" ;


            var viewParameters = {};
            viewParameters.ask = pView.ask;
            viewParameters.osk = pView.osk;
            viewParameters.viewid = pView.viewid;

            var viewIdInfo = {};
            if (pView.parameters) {
                viewIdInfo.parameters = pView.parameters;
            }
            if (pView.filter) {
                viewIdInfo.filter = pView.filter;
            }
            viewParameters.viewinfo = viewIdInfo;
            var data = {};
            data.ask = "appsstudio";
            data.module = "viewservice";
            data.method = "getView";
            data.parameters = JSON.stringify(viewParameters);


            this.getDataFromJQuery(url, data, "POST", "JSON", "Loading...", function (callBackData) {
                callBack(callBackData);
            });


        }

        $appService.logOut = function (callBack) {
            this.getDataFromService("/backend/task_manager/logout", {}, "Logout...", callBack);
        }

        $appService.isHostedMode = function () {
            var href = window.location.href;

            if (href.indexOf("FrontEnd") >= 0) {
                return true;
            } else {
                return false;
            }
        }
        $appService.getUser = function (state, callBack) {
            var hostedMode = this.isHostedMode();
            hostedMode = false;
            if (hostedMode) {
                var user = {"response":{"userid":"51f26dc3e52f0dcc16000001","username":"pawan.phutela@daffodilsw.com","usergroup":{"_id":"51f118b1e024b93411000003","id":"apps_studio"},"login":true,"organizations":[{"label":"Global","id":"global","applications":[{"_id":"51f11804e024b93411000002","id":"appsstudio","label":"Apps Studio","ask":"appsstudio","orgenabled":null}],"global":true}],"organizationindex":0,"viewgroups":[{"label":"Apps studio","menus":[{"_id":"52020ac83bea3efc24000001","label":"Applications","table":{"_id":"51ece1cdacf26b5018000005","id":"applications__baas"},"applicationid":{"_id":"51f11804e024b93411000002","id":"appsstudio","label":"Apps Studio","ask":"appsstudio","orgenabled":null}}],"_id":"52020ac83bea3efc24000002"}],"viewgroupindex":0,"menus":[{"_id":"52020ac83bea3efc24000001","label":"Applications","table":{"_id":"51ece1cdacf26b5018000005","id":"applications__baas"},"applicationid":{"_id":"51f11804e024b93411000002","id":"appsstudio","label":"Apps Studio","ask":"appsstudio","orgenabled":null}}],"menuindex":0,"view":{"ask":"appsstudio","viewid":"applications__baas","metadata":{"columns":[{"expression":"id","type":"string","replicate":true,"_id":"51f0e2d6829808681c000004","visibility":"Both","label":"id","width":200},{"expression":"ask","type":"string","replicate":true,"_id":"51f0e2d6829808681c000006","visibility":"Both","label":"ask","width":200},{"expression":"orgenabled","type":"boolean","replicate":true,"_id":"51f0e2d6829808681c000007","visibility":"Both","label":"orgenabled","width":200},{"expression":"label","type":"string","replicate":true,"_id":"51f0e2d6829808681c000005","visibility":"Both","label":"label","width":200},{"expression":"developers","type":"lookup","table":"users__baas","multiple":true,"_id":"51f0e2d6829808681c000008","columns":[{"expression":"username","type":"string","replicate":true,"_id":"51f0e2d6829808681c000015"},{"expression":"role","type":"lookup","options":["admin","developer"],"_id":"51f0e2d6829808681c000009"}],"visibility":"Child","label":"developers","width":200,"ask":"appsstudio","viewid":"developers__applications__baas"},{"expression":"developers.role","type":"lookup","options":["admin","developer"],"_id":"51f0e2d6829808681c000009","visibility":"Both","label":"developers.role","width":200},{"expression":"usergroups","type":"lookup","table":"usergroups__baas","autosave":true,"multiple":true,"_id":"51f0e2d6829808681c00000a","columns":[{"expression":"id","type":"string","replicate":true,"_id":"51f0e2d6829808681c000002"}],"visibility":"Both","label":"usergroups","width":200},{"expression":"db","type":"string","_id":"51f0e2d6829808681c00000b","visibility":"Both","label":"db","width":200},{"_id":"51f0e62c2c8657341f000001","type":"lookup","expression":"tables","table":{"_id":"51ece15facf26b5018000003","id":"tables__baas"},"multiple":true,"columns":[{"expression":"id","type":"string","replicate":true,"_id":"51f0e27905435ef815000002"},{"_id":"52035305a94f8c7017000004","type":"boolean","expression":"orgenabled","replicate":true}],"visibility":"Both","label":"tables","width":200},{"_id":"51f918c63fdf09bc0e000001","type":"string","expression":"customcodeurl","visibility":"Both","label":"customcodeurl","width":200}],"quickviews":[{"table":{"_id":"51ece1cdacf26b5018000005","id":"applications__baas"},"quickviewid":{"_id":"5202080a2e8badb018000001","id":"applications__baas"},"_id":"520210482e8badb018000003"}],"quickviewindex":0,"type":"table","table":"applications__baas"},"data":{"data":[{"_id":"51f25a1a4f258a3003000001","ask":"testing2","id":"myapp","label":"My app","tables":[{"_id":"51f25c4d4f258a3003000003","id":"hotels__apps"},{"_id":"51f264514f258a3003000005","id":"vouchers"}]},{"_id":"51ea479c2f00bfa41c00000a","ask":"baas","customcodeurl":"127.0.0.1:1337","db":"baas","developers":[{"_id":"51ea45932f00bfa41c000007","username":"rohit.bansal@daffodilsw.com"},{"_id":"51f232c688c40b2811000001","username":"R K Bansal","role":"admin"}],"id":"baas","label":"BaaS","tables":[{"_id":"51ece15facf26b5018000003","id":"tables__baas"},{"_id":"51ece1cdacf26b5018000005","id":"applications__baas"},{"_id":"51ece2b6acf26b5018000008","id":"teachers__baas"},{"_id":"51ece199acf26b5018000004","id":"usergroups__baas"},{"_id":"51ece1f5acf26b5018000006","id":"organizations__baas"},{"_id":"51ece227acf26b5018000007","id":"users__baas"},{"_id":"51f1ff1f76dc5ff406000003","id":"views__baas"},{"_id":"51fa2cfde7c6093813000006","id":"modules__baas"}],"usergroups":[{"_id":"51f90621f86dfee017000001","id":"baas"}]},{"_id":"51f11804e024b93411000002","ask":"appsstudio","customcodeurl":"127.0.0.1:4100","db":"baas","developers":[{"_id":"51ea45932f00bfa41c000007","username":"rohit.bansal@daffodilsw.com"}],"id":"appsstudio","label":"Apps Studio","tables":[{"_id":"51f1ff1f76dc5ff406000003","id":"views__baas"},{"_id":"51f0d38ca6bfcc5808000001","[object Object]":null,"id":"quickviews__appsstudio"},{"_id":"51f11bede024b9341100000c","id":"viewgroups__appsstudio"},{"_id":"51fca3b30b9c9d6828000007","id":"views__appsstudio"},{"_id":"51ece15facf26b5018000003","id":"tables__baas"},{"_id":"51ece1cdacf26b5018000005","id":"applications__baas"}],"usergroups":[{"_id":"51f118b1e024b93411000003","id":"apps_studio"}]},{"_id":"51f10707b155541417000004","ask":"book","id":"book","label":"Book","orgenabled":"true","tables":[{"_id":"5200f66b285913dc0d000002","id":"accountgroups__book"},{"_id":"5200f74946eb4ba017000002","id":"accounts__book"}]},{"_id":"51fbaf529ff68c1013000001","ask":"crm","id":"crm","label":"CRM","orgenabled":true,"tables":[{"_id":"52034fe3a94f8c7017000001","id":"relationships_crm","orgenabled":true}]},{"_id":"52032347a400547010000001","ask":"frontend","db":"baas","id":"frontend","label":"FrontEnd","tables":[{"_id":"520329d721afd57017000001","id":"domains__frontend"},{"_id":"52032abaa23b2cf815000004","id":"alternatedomains__frontend"},{"_id":"520380a380de05a006000001","id":"resources__frontend","orgenabled":null},{"_id":"520380bd80de05a006000003","id":"templates__frontend","orgenabled":null},{"_id":"520380d080de05a006000005","id":"pages__frontend","orgenabled":null}]}]}}},"status":"ok","code":200};
                callBack(user.response);
                return;
            }


            this.getDataFromJQuery(BAAS_SERVER + "/custom/module", {"ask":"appsstudio", "module":"userservice", "method":"getUserState", parameters:JSON.stringify({"state":state})}, "GET", "JSON", "Loading...", function (callBackData) {
                callBack(callBackData);
            }, function (jqxhr, error) {
                callBack({"login":false});
            });
        }
        $appService.getData = function (query, ask, osk, bussyMessage, callBack) {
            if (!ask) {
                throw "No ask found for saving";
            }

            var data = {"query":JSON.stringify(query), "ask":ask, "osk":osk};

            var that = this;
            var url = BAAS_SERVER + "/data";
            this.getDataFromJQuery(url, data, "GET", "JSON", bussyMessage, function (callBackData) {
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
            this.getDataFromJQuery(url, params, "GET", "JSON", "Saving...", function (callBackData) {
                callBack(callBackData);
            });


        }


        $appService.getDataFromService = function (pUrl, pData, pBusyMessage, pCallBack) {
            var payLoad;
            if (pData == null) {
                pData = {};
            }
            pData.osk = this.osk;
            pData.ask = this.ask;
            var payLoad = $.param(pData);
            var config = {
                headers:{"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8"}
            }
            if (pBusyMessage) {
                $rootScope.busymessage = pBusyMessage;
                $rootScope.showbusymessage = true
                if (!$rootScope.$$phase) {
                    $rootScope.$apply();
                }
            }
            $http.post(pUrl, payLoad, config).success(
                function (data) {
                    $rootScope.showbusymessage = false;
                    pCallBack(data);

                }).error(function (data) {
                    //alert("Error in Fetching service:[" + pUrl + "], Data[" + data + "]");
                    $rootScope.showbusymessage = false
                    if (!$rootScope.$$phase) {
                        $rootScope.$apply();
                    }
                    pCallBack(data);
                });
        }

        $appService.getDataFromJQuery = function (url, requestBody, callType, dataType, pBusyMessage, callback, errcallback) {

            requestBody.__currentstate__ = JSON.stringify(this.__currentstate__);

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
//                xhrFields:{
//                    withCredentials:true
//                },
                success:function (returnData, status, xhr) {

                    callback(returnData.response ? returnData.response : returnData);
                    $rootScope.showbusymessage = false
                    if (!$rootScope.$$phase) {
                        $rootScope.$apply();
                    }


                },
                error:function (jqXHR, exception) {

                    $rootScope.showbusymessage = false
                    if (!$rootScope.$$phase) {
                        $rootScope.$apply();
                    }
                    if (errcallback) {
                        errcallback(jqXHR, exception);
                    } else {
                        alert("exception in making [" + url + "] :[" + exception + "]");
                    }

                },
                timeout:1200000,
                dataType:dataType,
                async:true
            });


        }

        return $appService;

    }
]);


/**
 * DOM Services
 */
appStrapServices.factory('$appDomService', [
    '$rootScope',
    '$compile',
    '$http',
    '$timeout',
    '$dataModel',
    '$appService',

    function ($rootScope, $compile, $http, $timeout, $dataModel, $appService) {
        var $appDomService = {
            "views":{},
            "popupviews":{}
        };


        $appDomService.showPopup = function (config) {

            var autoHide = config.autohide != undefined ? config.autohide : true;
            var deffered = config.deffered != undefined ? config.deffered : true;
            var modal = !autoHide;
            var escape = autoHide;
            var callBack = config.callBack;
            var position = config.position ? config.position : "center";
            var hideOnClick = config.hideonclick != undefined ? config.hideonclick : false;
            if (modal) {
                hideOnClick = false;
            }
            var element = config.element;

            if (element) {
                position = null;
            }
            var p = new Popup(autoHide, modal, callBack);
            p.setHTML($compile(config.template)(config.scope));
            p.setPosition(position);
            p.setEscEnabled(escape);
            p.setHideOnClick(hideOnClick);
            p.setDeffered(deffered);

            if (config.width) {
                p.setWidth(config.width);
            }


            if (config.height) {
                p.setHeight(config.height);
            }
            p.showPopup(config.element);

            return p;

        }

        $appDomService.addView = function (view, $scope) {

            var componentId;
            if (view.metadata) {
                var parameters = view.metadata.parameters;
                if (!parameters) {
                    view.metadata.parameters = view.parameters;
                }

                if (!view[COMPONENT_ID]) {
                    componentId = $dataModel.getComponentId();
                } else {
                    componentId = view[COMPONENT_ID];
                }
                $appDomService.addViewToDom(view, componentId, $scope);
            } else {
                $appService.getView(view, function (v) {
                    v.metadata.filter = view.filter;
                    v.metadata.parameters = view.parameters;

                    v[SHOW_AS] = view[SHOW_AS];
                    if (view[COMPONENT_ID]) {
                        componentId = view[COMPONENT_ID];
                    } else {
                        componentId = $dataModel.getComponentId();
                    }
                    v[PARENT_COMPONENT_ID] = view[PARENT_COMPONENT_ID];


                    $appDomService.addViewToDom(v, componentId, $scope);
                });
            }
        }

        /***
         * Documentation Pending
         * @param view
         * @param $parentScope
         */
        $appDomService.addViewToDom = function (view, componentId, $parentScope) {
//            $parentScope.log($parentScope.getTotalTime(),true);
//            $parentScope.setStartTime();

            $parentScope.log(new Date().getTime() % 10000, false);
            $timeout(function () {
                $parentScope.log(new Date().getTime() % 10000, false);

            }, 100);
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
                if (parentViewInfo[CHILD_COMPONENT_ID]) {
                    this.closeViewFromDom(parentViewInfo[CHILD_COMPONENT_ID]);
                }
                parentViewInfo[CHILD_COMPONENT_ID] = componentId;
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
                var parentViewInfo = this.views[parentComponentId];
                var superParentId = parentViewInfo[PARENT_COMPONENT_ID];
                if (superParentId) {
                    $("#" + superParentId).css({left:0, top:'0px', right:'100%'});
                }
                $("#" + parentComponentId).css({left:0, top:'0px', right:'50%'});
                var template = "<div id='" + view[COMPONENT_ID] + "' class='app-view-wrapper' style='position:absolute;left:50%; top:0; bottom: 0; right:0; overflow: hidden;'><app-view componentId='" + view[COMPONENT_ID] + "'></app-view></div>"
                var templateElement = $compile(template)($parentScope);
                var viewContainer = $("#view-container");
                viewContainer.append(templateElement);
                $("#" + parentComponentId).resize();
            } else {
                //it is a root, we will not show close action here
                view.metadata.close = false;
                var template = "<div id='" + view[COMPONENT_ID] + "' class='app-view-wrapper' style='position:absolute;left:0; top:0; bottom: 0; right:0; overflow: hidden;'><app-view componentId='" + view[COMPONENT_ID] + "'></app-view></div>"
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


        $appDomService.hidePopupView = function (componentId) {
            var popup = this.popupviews[componentId];
            if (!popup) {
                throw 'Popup not found for Id:' + componentId;
            }
            popup.hide();
        }

        $appDomService.closeAllViews = function () {
            var views = $appDomService.views;
            $(VIEW_CONTAINER).html("");
            for (var componentId in views) {
                if (views.hasOwnProperty(componentId)) {
                    var view = $appDomService.views[componentId];
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
            $appDomService.views = {};
        }

        $appDomService.closeViewFromDom = function (componentId) {

            var view = $appDomService.views[componentId];
            if (!view) {
                alert("closeView:: View not found while closing for Id- " + componentId);
                throw "closeView:: View not found while closing for Id- " + componentId;
            }
            if (view[CHILD_COMPONENT_ID]) {
                $appDomService.closeViewFromDom(view[CHILD_COMPONENT_ID]);
            }
            var showInPopup = view[SHOW_AS] && view[SHOW_AS] === 'popup';
            if (showInPopup) {
                this.hidePopupView(componentId);
            } else {
                $("#" + componentId).remove();
            }

            var parentComponentId = view[PARENT_COMPONENT_ID];
            if (parentComponentId) {
                delete $appDomService.views[parentComponentId][CHILD_COMPONENT_ID];
            }
            $dataModel.removeView(componentId);
            delete $appDomService.views[componentId];
            view = undefined;
        }

        $appDomService.closeView = function (componentId) {

            var view = $appDomService.views[componentId];
            if (!view) {
                alert("closeView:: View not found while closing for Id- " + componentId);
                throw "closeView:: View not found while closing for Id- " + componentId;
            }
            $appDomService.closeViewFromDom(componentId);
            var showInPopup = view[SHOW_AS] && view[SHOW_AS] === 'popup';
            if (showInPopup) {
                return;
            } else if (view[PARENT_COMPONENT_ID]) {
                var parentViewComponentId = view[PARENT_COMPONENT_ID];
                var parentView = $appDomService.views[parentViewComponentId];

                var superParentComponentId = parentView[PARENT_COMPONENT_ID];
                if (superParentComponentId) {
                    var elem = $("#" + superParentComponentId);

                    $("#" + superParentComponentId).css({left:0, right:'50%'});

                    $("#" + parentViewComponentId).css({left:'50%', right:0});
                    $("#" + superParentComponentId).resize();
                    $("#" + parentViewComponentId).resize();
                } else {
                    $("#" + parentViewComponentId).css({left:0, right:0});
                    $("#" + parentViewComponentId).resize();
                }
            }
        }
        return $appDomService;
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
        $dataModel.tempComponents = 0;

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

        $dataModel.moveUp = function (row, id) {
            var model = this[id];

            var key = row[model.view.metadata[PRIMARY_COLUMN]];
            var index = this.getIndex(key, id);
            var prevIndex = index - 1;

            if (model == null || model == undefined) {
                alert("No view exists with id[" + id + "]");
                return;
            }

            var view = model.view;
            var data = view.data.data;

            var currentRow = data[index];

            if (prevIndex >= 0) {
                var previous = data[prevIndex];

                data[index] = previous;
                data[prevIndex] = currentRow;
            }
        }

        $dataModel.moveDown = function (row, id) {
            var model = this[id];
            var view = model.view;
            var key = row[model.view.metadata[PRIMARY_COLUMN]];
            var index = this.getIndex(key, id);


            if (model == null || model == undefined) {
                alert("No view exists with id[" + id + "]");
                return;
            }

            var data = view.data.data;
            var dataLength = data.length;
            var nextIndex = index + 1;
            var currentRow = data[index];

            if (nextIndex < dataLength) {
                var next = data[index + 1];
                data[index] = next;
                data[index + 1] = currentRow;
            }
        }


        $dataModel.getIndex = function (key, id) {
            var model = this.getModel(id);


            var view = model.view;
            var data = view.data.data;

            var dataCount = data.length;
            for (var i = 0; i < dataCount; i++) {
                var record = data[i];
                var recordKey = record[KEY];
                var isEqual = angular.equals(recordKey, key);
                if (isEqual) {
                    return i;
                }
            }
            return -1;
        }


        $dataModel.getComponentId = function () {
            this.tempComponents = this.tempComponents + 1;
            return COMPONENT_ID_KEY + this.tempComponents;
        }
        $dataModel.getTempKey = function (primaryColumn) {
            this.tempKey = this.tempKey + 1;
            var temp = {};
            temp[primaryColumn] = this.tempKey + "temp";
            return temp;

        }
        $dataModel.setScope = function (componentid, scope) {
            var model = this[componentid];
            model.scope = scope;
            scope.$watch("view.data", function (newvalue, oldvalue) {
//                    alert("New value["+JSON.stringify(newvalue)+"]");
//                    alert("Old value["+JSON.stringify(oldvalue)+"]");
            }, true);
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
            view.dataclone = angular.copy(view.data.data);
            var selectedKeys = view.selectedkeys;
            if (selectedKeys == null || selectedKeys == undefined) {
                selectedKeys = [];
            }

            var filterparameters = view.filterparameters;
            if (!filterparameters) {
                filterparameters = {};
                view.filterparameters = filterparameters;
            }
            view.selectedkeys = selectedKeys;
        }

        $dataModel.getView = function (componentid) {
            var model = this[componentid];
            return model.view;

        }

        $dataModel.removeView = function (componentid) {
            var model = this[componentid];
            delete this[componentid];
//          model.scope.$destroy();
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
        $dataModel.refresh = function (componentid) {

            var model = this.getModel(componentid);

            if (model == null || model == undefined) {
                throw "No model found for refresh[" + componentid + "]";
            }

            var view = model.view;
            var ask = view.ask;
            var osk = view.osk;

            var that = this;
            var callBack = function (response) {
                view.data = response;
                view.dataclone = angular.copy(view.data.data);
                if (!model.scope.$$phase) {
                    model.scope.$apply();
                }
            };
            var parameters = view.metadata.parameters;

            var query = {"table":view.metadata.table, "columns":view.metadata.columnsclone, "filter":view.metadata.filter, "parameters":parameters};

            $appService.getData(query, ask, osk, "Loading...", callBack);
        }

        $dataModel.insert = function (componentid) {
            var model = this[componentid];
            var tempKey = this.getTempKey(model.view.metadata[PRIMARY_COLUMN]);
            var data = model.view.data.data;
            data.splice(0, 0, tempKey);
        }

        $dataModel.deleteData = function (componentid) {
            var model = this[componentid];
            var view = model.view;
            var primaryColumn = view.metadata[PRIMARY_COLUMN];

            var selectedItems = this.getSelectedKeys(componentid);
            var selectedCount = selectedItems.length;
            if (selectedCount > 0) {
                var deletedItems = [];
                for (var i = 0; i < selectedCount; i++) {
                    var selectedItem = selectedItems[i];
                    var key = selectedItem[primaryColumn];
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

                var that = this;
                var callBack = function (data) {

                    that.refresh(componentid);
                };



                $appService.save({"table":view.metadata.table, "filter":view.metadata.filter, "parameters":view.metadata.parameters, "operations":deletedItems}, view.ask, view.osk, callBack);

            } else {
                alert("No row found for delete");
            }
        }

        $dataModel.isNullOrUndefined = function (obj) {
            return (obj === undefined || obj === null) ? true : false;

        }

        $dataModel.isTrueOrUndefined = function (obj) {
            return (obj === undefined || obj === null || obj == true) ? true : false;

        }


        $dataModel.addFilter = function (componentId, pFilterExp, pParamValue) {


            var model = this.getModel(componentId);
            var view = model.view;
            var metadata = view.metadata;
            var filter = metadata.filter;

            if (this.isNullOrUndefined(filter)) {
                filter = {};
            }

            metadata.filter = filter;

            filter[pFilterExp] = "{" + pFilterExp + "}";

            var parameters = metadata.parameters;
            if (!angular.isObject(parameters)) {
                parameters = {};
                metadata.parameters = parameters;
            }
            parameters[pFilterExp] = pParamValue;
        };

        $dataModel.save = function (componentid) {

            var model = this.getModel(componentid);

            var view = model.view;

            var ask = view.ask;
            var osk = view.osk;

            var newInserts = $dataModel.getUpdates(componentid);

            var newInsertsCount = newInserts.length;
            if (newInsertsCount > 0) {

                var that = this;
                var callBack = function (data) {
                    that.refresh(componentid);
                };
                $appService.save({"table":view.metadata.table, "filter":view.metadata.filter, "parameters":view.metadata.parameters, "operations":newInserts}, ask, osk, callBack);
            } else {
                alert("No data found for saving");
            }
        }


        $dataModel.getUpdates = function (componentid) {
            var model = this.getModel(componentid);


            var view = model.view;


            var cloneData = view.dataclone;
            var data = view.data.data;

            var primaryColumn = view.metadata[PRIMARY_COLUMN];

            if (!primaryColumn) {
                throw new Error("Primary column not defined");
            }

            var columns = model.view.metadata.columnsclone;
            var noOfColumns = columns.length;

            var newInserts = [];
            var noOfDataRecords = data.length;
            for (var i = 0; i < noOfDataRecords; i++) {
                var record = data[i];

                //check for update
                var selectedUpdates = view[UPDATE_SELECTED_KEYS];
                if (selectedUpdates == true) {
                    if (noOfDataRecords != 1) {
                        if (noOfDataRecords == 0) {
                            alert("Nothing to update");
                        } else if (noOfDataRecords > 1) {
                            alert("Too much for update in update action. Its size should be 1 but found[" + noOfDataRecords + "]");
                        }
                    } else {
                        //check record updates
                    }
                    var selectedKeys = $dataModel.getSelectedKeys(componentid);
                    var selectedKeysCount = selectedKeys.length;
                    for (var j = 0; j < selectedKeysCount; j++) {
                        var selectedKey = selectedKeys[j];
                        var key = selectedKey[KEY];
                        var recordClone = angular.copy(record);
                        recordClone[KEY] = key;
                        newInserts.push(recordClone);
                    }
                } else {
                    var key = record[primaryColumn];
                    var cloneRecord = getRecord(cloneData, key, primaryColumn);
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
                        var oldValue = cloneRecord[expression];
                        var newValue = record[expression];
                        if (!angular.equals(oldValue, newValue)) {
                            updates[expression] = newValue;
                            hasUpdates = true;
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
            }
            return newInserts;
        }

        function getRecord(data, pKey, primaryColumn) {
            var dataCount = data.length;
            for (var i = 0; i < dataCount; i++) {
                var record = data[i];
                var recordKey = record[primaryColumn];
                var isEqual = angular.equals(recordKey, pKey);
                if (isEqual) {
                    return record;
                }
            }
            return null;
        }


        return $dataModel;

    }
]);

appStrapDirectives.directive('appDatepicker', [
    '$timeout',
    '$strapConfig',
    function ($timeout, $strapConfig) {
        'use strict';
        var isAppleTouch = /(iPad|iPho(ne|d))/g.test(navigator.userAgent);
        var regexpMap = function regexpMap(language) {
            language = language || 'en';
            return {
                '/':'[\\/]',
                '-':'[-]',
                '.':'[.]',
                ' ':'[\\s]',
                'dd':'(?:(?:[0-2]?[0-9]{1})|(?:[3][01]{1}))',
                'd':'(?:(?:[0-2]?[0-9]{1})|(?:[3][01]{1}))',
                'mm':'(?:[0]?[1-9]|[1][012])',
                'm':'(?:[0]?[1-9]|[1][012])',
                'DD':'(?:' + $.fn.datepicker.dates[language].days.join('|') + ')',
                'D':'(?:' + $.fn.datepicker.dates[language].daysShort.join('|') + ')',
                'MM':'(?:' + $.fn.datepicker.dates[language].months.join('|') + ')',
                'M':'(?:' + $.fn.datepicker.dates[language].monthsShort.join('|') + ')',
                'yyyy':'(?:(?:[1]{1}[0-9]{1}[0-9]{1}[0-9]{1})|(?:[2]{1}[0-9]{3}))(?![[0-9]])',
                'yy':'(?:(?:[0-9]{1}[0-9]{1}))(?![[0-9]])'
            };
        };
        var regexpForDateFormat = function regexpForDateFormat(format, language) {
            var re = format, map = regexpMap(language), i;
            i = 0;
            angular.forEach(map, function (v, k) {
                re = re.split(k).join('${' + i + '}');
                i++;
            });
            i = 0;
            angular.forEach(map, function (v, k) {
                re = re.split('${' + i + '}').join(v);
                i++;
            });
            return new RegExp('^' + re + '$', ['i']);
        };
        return {
            restrict:'A',
            require:'?ngModel',
            link:function postLink(scope, element, attrs, controller) {
                var options = angular.extend({ autoclose:true, "modelformat":"yyyy-mm-dd", "updater":function (val) {
                    var modelValue = this.getFormattedDate(this.modelformat);
                    scope.row[scope.colmetadata.expression] = modelValue
                    if (!scope.$$phase) {
                        scope.$apply();
                    }

                }, "isFirstTimeUpdate":true}, $strapConfig.datepicker || {}), type = attrs.dateType || options.type || 'string';
                angular.forEach([
                    'format',
                    'weekStart',
                    'calendarWeeks',
                    'startDate',
                    'endDate',
                    'daysOfWeekDisabled',
                    'autoclose',
                    'startView',
                    'minViewMode',
                    'todayBtn',
                    'todayHighlight',
                    'keyboardNavigation',
                    'language',
                    'forceParse'
                ], function (key) {
                    if (angular.isDefined(attrs[key]))
                        options[key] = attrs[key];
                });
                var language = options.language || 'en', format = isAppleTouch ? 'yyyy-mm-dd' : attrs.dateFormat || options.format || $.fn.datepicker.dates[language] && $.fn.datepicker.dates[language].format || 'yyyy-mm-dd', dateFormatRegexp = regexpForDateFormat(format, language);
                var modelFormat = "yyyy-mm-dd";
                var firstTimeRendering = true;
                if (controller) {
                    controller.$formatters.unshift(function (modelValue) {
                        if (modelValue != null && modelValue != undefined && type === 'string' && modelValue.length > 0) {
                            var dateValue = $.fn.datepicker.DPGlobal.parseDate(modelValue, $.fn.datepicker.DPGlobal.parseFormat(modelFormat), language);
                            return $.fn.datepicker.DPGlobal.formatDate(dateValue, $.fn.datepicker.DPGlobal.parseFormat(format), language);
                        }
                        return type === 'date' && angular.isString(modelValue) ? new Date(modelValue) : modelValue;
                    });
                    controller.$parsers.unshift(function (viewValue) {
                        if (!viewValue) {
                            controller.$setValidity('date', true);
                            return null;
                        } else if (type === 'date' && angular.isDate(viewValue)) {
                            controller.$setValidity('date', true);
                            return viewValue;
                        } else if (angular.isString(viewValue) && dateFormatRegexp.test(viewValue)) {
                            controller.$setValidity('date', true);
                            if (isAppleTouch)
                                return new Date(viewValue);
                            return type === 'string' ? viewValue : $.fn.datepicker.DPGlobal.parseDate(viewValue, $.fn.datepicker.DPGlobal.parseFormat(format), language);
                        } else {
                            controller.$setValidity('date', false);
                            return undefined;
                        }
                    });
                    controller.$render = function ngModelRender() {

                        if (isAppleTouch) {
                            var date = $.fn.datepicker.DPGlobal.formatDate(controller.$viewValue, $.fn.datepicker.DPGlobal.parseFormat(format), language);
                            element.val(date);
                            return date;
                        }
                        if (type == 'string' && firstTimeRendering) {
                            firstTimeRendering = false;
                            var parsedDate = $.fn.datepicker.DPGlobal.parseDate(controller.$viewValue, $.fn.datepicker.DPGlobal.parseFormat(format), language);
                            var dateValue = $.fn.datepicker.DPGlobal.formatDate(parsedDate, $.fn.datepicker.DPGlobal.parseFormat(format), language);
                            element.val(dateValue);
                            return dateValue;
                        }
                        return controller.$viewValue && element.datepicker('update', controller.$viewValue);
                    };
                }
                if (isAppleTouch) {
                    element.prop('type', 'date').css('-webkit-appearance', 'textfield');
                } else {

                    element.on('changeDate', function (ev) {
                        scope.$apply(function () {
                            console.log("Change date called and ev.date :" + ev.date);
                            if (type == 'string') {
                                scope.row[scope.colmetadata.expression] = $.fn.datepicker.DPGlobal.formatDate(ev.date, $.fn.datepicker.DPGlobal.parseFormat(modelFormat), language);
                            } else {
                                scope.row[scope.colmetadata.expression] = ev.date;
                            }
                        });
                    });

                    element.attr('data-toggle', 'datepicker');

                    element.datepicker(angular.extend(options, {
                        format:format,
                        language:language
                    }));


                    scope.$on('$destroy', function () {
                        var datepicker = element.data('datepicker');
                        if (datepicker) {
                            datepicker.picker.remove();
                            element.data('datepicker', null);
                        }
                    });
                }
                var component = element.siblings('[data-toggle="datepicker"]');
                if (component.length) {
                    component.on('click', function () {
                        element.datepicker(angular.extend(options, {
                            format:format,
                            language:language
                        }));
                        element.trigger('focus');
                    });
                }
            }
        };
    }
]);
