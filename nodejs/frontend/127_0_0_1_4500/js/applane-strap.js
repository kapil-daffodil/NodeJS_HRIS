var BAAS_SERVER;
var windowUrl = (window.location.href);
if (windowUrl.indexOf("127.0.0.1") >= 0 || windowUrl.indexOf("localhost") >= 0) {
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
var VAL = '_val';
var FOCUS_ELEMENT_CLASS = 'focus-element';
var DATA_NOT_FOUND = 'No Data Found'; // when data is not found in lookup column, show no data found
var CELL_TEMPLATE = "<span ng-bind-html-unsafe='getColumnValue(row,col,true)'></span>";
var CURRENCY_TYPE = 'currency';


'use strict';
appStrapDirectives.directive('bsTypeahead', ['$parse', '$dataModel', '$timeout', function ($parse, $dataModel, $timeout) {
    'use strict';
    return {
        restrict:'E',
        template:"<div class ='ref-container app-white-backgroud-color' composite-data-container=\"true\" >" +
            "<div class='ref-input-container'>" +
            '<input class="ref-input ' + FOCUS_ELEMENT_CLASS + '" type = "text"   placeholder="{{colmetadata.label}}" ng-model="row[colmetadata.expression]._val" data-popup-id="app_popup" />' +
            "</div>" +
            "<input type='text' style='color: white' tabindex='-1'  class='ref-button app-cursor-pointer'>" +
            "</div>" +
            "</div>",


        scope:true,
        replace:true,
        require:'?ngModel',
        link:function postLink(scope, element, attrs, controller) {
            var width = scope.colmetadata.width;
            var height = scope.colmetadata.height;

            if (!height) {
                height = 24;
            }

            if (!width) {
                width = 200;
            }
            $(element).width(width);
            $(element).height(height);

            element.attr('data-provide', 'typeahead');
            element.typeahead({

                source:function (query, callBack) {

                    var selectColumn = scope.colmetadata;
                    var exp = scope.colmetadata.primarycolumns[0].expression;

                    if (selectColumn.options) {
                        var options = selectColumn.options;
                        var optionsCount = options.length;
                        var optionsToShow = [];

                        if (query && query.length > 0) {
                            for (var i = 0; i < optionsCount; i++) {
                                var option = options[i][exp];
                                if (option.toLowerCase().match("^" + query.toLowerCase())) {
                                    optionsToShow.push(option);
                                }
                            }
                        } else {
                            optionsToShow = selectColumn.options;
                        }

                        callBack(optionsToShow);
                        return;
                    }
                    var items;

                    if (this.timeout != null) {
                        $timeout.cancel(this.timeout);
                    }

                    var that = this;
                    this.timeout = $timeout(function () {

                        var displayExpression = scope.colmetadata.primarycolumns[0].expression;

                        var resourceTable = scope.colmetadata.table;
                        if (angular.isObject(resourceTable)) {
                            resourceTable = resourceTable.id;
                        }
                        var resourceQuery = {"table":resourceTable, "columns":scope.colmetadata.primarycolumns};
                        var filter;
                        if (scope.colmetadata.filter) {
                            filter = scope.colmetadata.filter
                        } else {
                            filter = {};
                        }
                        filter[displayExpression] = {"$regex":"^(" + query + ")", "$options":"-i"};
                        resourceQuery.filter = filter
                        resourceQuery.parameters = $dataModel.getCurrentRow(scope.componentid);


                        var loadingHtml = '<img src="../images/loading.gif" class="app-input-loading-image">';
                        $(loadingHtml).insertAfter(that.$element);
                        that.showingLoadingImage = true;


                        $dataModel.getData(scope.componentid, resourceQuery, false, function (data) {
                            $timeout.cancel(that.timeout);
                            that.timeout = null;

                            $(that.$element).next().remove();
                            that.showingLoadingImage = false;

                            if (!that.$element.is(":focus")) {
                                return;
                            }

                            if (query == '' || query == that.$element.val()) {
                                var optionData = data.data;
                                if (optionData.length == 0) {
                                    optionData.push(DATA_NOT_FOUND);
                                }
                                callBack(optionData);
                            }
                        });
                    }, 200);


                },
                process:function (items) {
                    var that = this
                    if (!items.length) {
                        return this.shown ? this.hide() : this
                    }

                    return this.render(items.slice(0, this.options.items)).show()
                },

                sorter:function (items) {
                    return items;
                },

                preRender:function (items) {

                    var that = this

                    items = $(items).map(function (i, item) {
                        var itemId;
                        if (angular.isObject(item)) {
                            itemId = item[KEY];
                            item = item[scope.colmetadata.primarycolumns[0].expression];
                        }

                        i = $(that.options.item).attr('data-value', item)
                        if (itemId) {
                            i.attr("data-value-id", itemId);
                        }
                        i.find('a').html(that.highlighter(item))
                        return i[0]
                    })


                    return items
                },
                minLength:attrs.minLength || 1,
                items:attrs.items,
                updater:function (value, id) {
                    var that = this;

                    var lastSelected = this.lastselected;
                    if (lastSelected && lastSelected._id && lastSelected.value == value) {
                        return;
                    }
                    that.lastselected = {"value":value, "_id":id};

                    scope.$apply(function () {
                        var updatedValue = {};

                        updatedValue[scope.colmetadata.primarycolumns[0].expression] = value;
                        updatedValue._val = value;
                        if (id) {
                            updatedValue[KEY] = id;
                        }

                        if (value.trim().length == 0) {
                            updatedValue = {_val:null};
                        }
                        scope.row[scope.colmetadata.expression] = updatedValue;

                        var onChangeFn = element.attr('onselection');
                        if (onChangeFn) {
//                            scope[onChangeFn](updatedValue, scope.colmetadata);
                        }
                    });
                    scope.$emit('typeahead-updated', value);
                    return value;


                }
            });
            var typeahead = element.data('typeahead');
            typeahead.lookup = function (ev, showAll) {
                var items;
                if (showAll) {
                    this.query = '';
                } else {
                    this.query = this.$element.val();
                }
                this.source(this.query, $.proxy(this.process, this));

            };
        }
    };
}
]);

'use strict';
appStrapDirectives.directive('bsMultiRefrenceTypeahead', ['$parse', '$dataModel', '$timeout', function ($parse, $dataModel, $timeout) {
    'use strict';
    return {
        restrict:'E',
        template:"<div style='height: auto;border:1px solid #CCCCCC;display: table;max-width: 300px;'>" +

            "<div style='display:table-cell;right:20px;left: 0px;' class='app-white-backgroud-color'>" +
            "<app-multiple-refrence ng-repeat='option in row[colmetadata.expression]'></app-multiple-refrence>" +

            '<input  type ="text" placeholder="{{colmetadata.label}}" class="app-float-left ' + FOCUS_ELEMENT_CLASS + '"  size="{{colmetadata.label.length}}"  data-popup-id="app_popup" style="margin-left:2px;margin-top:2px;border: none;;"/>' +
            "</div>" +

            "<div style='display:table-cell;width:33px;vertical-align: middle;padding-left: 5px;' class='app-white-backgroud-color'>" +
            "<input type='text' style='color: white' class='app-multi-refrence-down-arrow' tabindex='-1' >" +
            "</div>" +

            "</div>",
        scope:true,
        replace:true,
        require:'?ngModel',
        link:function postLink(scope, element, attrs, controller) {
            element.attr('data-provide', 'typeahead');

            element.typeahead({
                multiple:true,
                source:function (query, callBack) {

                    var selectColumn = scope.colmetadata;
                    if (selectColumn.options) {
                        callBack(selectColumn.options);
                        return;
                    }
                    var items;

                    if (this.timeout != null) {
                        $timeout.cancel(this.timeout);
                    }

                    var that = this;

                    this.timeout = $timeout(function () {
                        var displayExpression = scope.colmetadata.primarycolumns[0].expression;

                        var resourceTable = scope.colmetadata.table;
                        if (angular.isObject(resourceTable)) {
                            resourceTable = resourceTable.id;
                        }
                        var resourceQuery = {"table":resourceTable, "columns":scope.colmetadata.primarycolumns};
                        var filter;
                        if (scope.colmetadata.filter) {
                            filter = scope.colmetadata.filter
                        } else {
                            filter = {};
                        }

                        filter[displayExpression] = {"$regex":"^(" + query + ")"};
                        resourceQuery.filter = filter;

                        if (!that.showingLoadingImage) {
                            var loadingHtml = '<img src="../images/loading.gif" >';
                            that.$element.parent().next().prepend(loadingHtml);
                            that.showingLoadingImage = true;
                        }


                        $dataModel.getData(scope.componentid, resourceQuery, false, function (data) {
                            $timeout.cancel(that.timeout);
                            that.timeout = null;

                            if (that.showingLoadingImage) {
                                var firstElement = $(that.$element).parent().next().children()[0];
                                $(firstElement).remove();
                                that.showingLoadingImage = false;

                            }

                            if (!that.$element.is(":focus")) {
                                return;
                            }

                            if (query == '' || query == that.$element.val()) {
                                var optionData = data.data;
                                callBack(optionData);
                            }
                        });
                    }, 150);
                },
                process:function (items) {
                    var that = this
                    if (!items.length) {
                        return this.shown ? this.hide() : this
                    }
                    return this.render(items.slice(0, this.options.items)).show()
                },

                sorter:function (items) {
                    return items;
                },

                preRender:function (items) {

                    var that = this

                    items = $(items).map(function (i, item) {
                        var itemId;
                        if (angular.isObject(item)) {
                            itemId = item[KEY];
                            item = item[scope.colmetadata.primarycolumns[0].expression];
                        }

                        i = $(that.options.item).attr('data-value', item)
                        if (itemId) {
                            i.attr("data-value-id", itemId);
                        }
                        i.find('a').html(that.highlighter(item))
                        return i[0]
                    })
                    return items
                },
                minLength:attrs.minLength || 1,
                items:attrs.items,
                updater:function (value, id) {
                    var that = this;

                    scope.$apply(function () {

                        var updatedValue = {};
                        if ($dataModel.isNullOrUndefined(id)) {
                            updatedValue[scope.colmetadata.primarycolumns[0].expression] = value;
                        } else {
                            updatedValue[scope.colmetadata.primarycolumns[0].expression] = value;
                            updatedValue[KEY] = id;
                        }


                        if (scope.row[scope.colmetadata.expression]) {
                            scope.row[scope.colmetadata.expression].push(updatedValue);
                        } else {
                            scope.row[scope.colmetadata.expression] = [updatedValue];
                        }


                    });
                    scope.$emit('typeahead-updated', value);
                    return '';
                },

                keyup:function (e) {
                    var that = this;
                    switch (e.keyCode) {
                        case 40: // down arrow
                        case 38: // up arrow
                        case 16: // shift
                        case 17: // ctrl
                        case 18: // alt
                            break


                        case 13: // enter
//                            if (!this.shown) return
                            if (!that.shown) {
                                that.focused = false;
                                var value = that.$element.val();


                                if (value != undefined && value.trim().length > 0) {
                                    that.updater(value, null);
                                    that.$element.val('')
                                }
                            } else {
                                that.select()
                            }

                            break

                        case 9: // tab
//                        case 13: // enter
                            if (!that.shown) return
                            that.select()
                            break

                        case 27: // escape
                            if (!that.shown) return
                            that.hide()
                            break

                        default:
                            that.lookup(e, false)
                    }

                    e.stopPropagation()
                    e.preventDefault()
                }
            });
            var typeahead = element.data('typeahead');
            typeahead.lookup = function (ev, showAll) {
                var items;

                if (showAll) {
                    this.query = '';
                } else {
                    this.query = this.$element.val();
                }
                this.source(this.query, $.proxy(this.process, this));

            };
        }
    };
}
]);


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
        template:"<div ng-click='onMenuClick(menu)'>" +
            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement) {

                    var menu = $scope.menu.label;
                    var imageUrl = $scope.menu.imageurl;
                    var html = '';
                    if (imageUrl) {
                        html = '<div><img src="' + imageUrl + '" class="app-menu-setting"></div>';
                    } else if (menu != null && menu != undefined) {
                        html = "<div>" + menu + "</div>";
                    }

                    iElement.append($compile(html)($scope))


                    $scope.onMenuClick = function (menu) {

                        var table = menu.table;
                        var currentMenu = $scope.currentmenu;
                        if (currentMenu) {
                            currentMenu.label = menu.label;
                        }

                        if (table) {
                            $appDomService.addView({"viewid":table.id, "filter":menu.filter, "applicationid":menu.applicationid.id, "ask":menu.applicationid.ask, "osk":menu.osk}, $scope);
                        } else {
                            var view = menu.view;
                            if (!view) {
                                alert("No view / table defined in menu");
                                return;
                            }
                            $appDomService.addView(angular.copy(view), $scope);
                        }
                    }
                }

            };
        }
    }
}]);


appStrapDirectives.directive('appMultipleRefrence', ['$timeout', function ($timeout) {
    'use strict';
    return {
        restrict:'E',
        scope:true,
        replace:true,
        template:"<div class='app-multirefrence-parent'>" +
            "<span ng-bind='option[colmetadata.primarycolumns[0].expression]' class='app-multirefrence-text'></span>" +
            "<input type='text' tabindex='-1' class='app-multirefrence-cross app-cursor-pointer' style='color:#000000;width:10px;background: none;border:none;padding: 0px;margin: 0px;' ng-click='cancel($event)' value='X'/>" +
            "</div>",
        compile:function () {
            return{
                pre:function ($scope, iElement) {
                    $scope.cancel = function ($event) {
                        $event.preventDefault();
                        $event.stopPropagation();
                        var key = $scope.option[KEY];

                        var nameTemp = $scope.option[$scope.colmetadata.primarycolumns[0].expression];
                        var options = $scope.row[$scope.colmetadata.expression];
                        var length = options.length;

                        if (length > 0) {
                            for (var i = 0; i < length; i++) {
                                var indexKey = options[i][KEY];
                                var indexName = options[i][$scope.colmetadata.primarycolumns[0].expression];

                                if ((indexKey != null && indexKey == key) || (indexName != null && indexName == nameTemp)) {
                                    $scope.row[$scope.colmetadata.expression].splice(i, 1);
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
        template:"<div style='height: inherit;cursor: pointer;'>" +

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

                        var popup = {
                            template:optionsHtml,
                            scope:$scope.$new(),
                            hideonclick:true,
                            element:iElement
//                            width:$(iElement).width()
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
        template:"<div class='app-action-collection-option app-cursor-pointer app-white-space-nowrap app-light-gray-backgroud-color app-padding-five-px' ng-style='{paddingLeft:level*5}' ng-bind='getOptionLabel(option)' ng-click='onActionCollectionOptionClick()'>" +
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

                            $appService.getDataFromJQuery(url, param, "POST", "JSON", "Loading...", function (data) {
                                var v = $dataModel.getView($scope[COMPONENT_ID]);
                                $appDomService.reOpen(v);
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

                    $scope.orderby = function ($event) {

                        var columnsClone = $scope.view.metadata.columnsclone;
                        var columnsTemp = [];
                        if (columnsClone && columnsClone.length > 0) {
                            for (var i = 0; i < columnsClone.length; i++) {
                                var visibility = columnsClone[i].visibility;
                                if (visibility == 'Table' || visibility == 'Both') {
                                    columnsTemp.push(angular.copy(columnsClone[i]));
                                }
                            }
                        }

                        var columns = [
                            {"label":"Column", "expression":"label", "type":"lookup", "width":200, "options":columnsTemp, "visibility":"Table"},
                            {"label":"order", "expression":"order", "type":"lookup", "width":200, "options":["asc", "desc", "none"], "visibility":"Table"}
                        ];

                        var orderData = [];

                        if ($scope.view.metadata.orders != null && $scope.view.metadata.orders != undefined) {
                            var orders = $scope.view.metadata.orders;
                            columnsClone = $scope.view.metadata.columns;

                            for (var i = 0; i < orders.length; i++) {
                                var order = orders[i];
                                angular.forEach(order, function (value, exp) {
                                    for (var j = 0; j < columnsClone.length; j++) {
                                        var columnExp = columnsClone[j].expression;
                                        var type = columnsClone[j].type;

                                        if (type == 'lookup') {
                                            columnExp = columnsClone[j].expression + '.' + columnsClone[j].primarycolumns[0].expression;
                                        }

                                        if (exp == columnExp) {
                                            var obj = {};
                                            obj.label = columnsClone[j].label;
                                            obj.order = value;
                                            orderData.push(obj);
                                        }
                                    }
                                });
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

                            if (count > 0) {
                                for (var i = 0; i < count; i++) {
                                    var label = orders[i].label._val;
                                    var order = orders[i].order._val;

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
                                            var type = columnsClone[k].type;
                                            var obj = {};

                                            if (order == 'none') {
                                                delete $scope.view.metadata.columns[k].order;
                                                continue;
                                            }

                                            if (type == 'lookup') {
                                                var primaryExp = columnsClone[k].primarycolumns[0].expression;
                                                var dottedExpression = [exp] + "." + [primaryExp];
                                                obj[dottedExpression] = order;
                                            } else {
                                                obj[exp] = order;
                                            }

                                            $scope.view.metadata.columns[k].order = order;
                                            populatedOrder.push(obj);
                                        }
                                    }
                                }

                                if (populatedOrder.length > 0) {
                                    $scope.view.metadata.orders = populatedOrder;
                                } else {
                                    if ($scope.view.metadata.orders != null && $scope.view.metadata.orders != undefined) {
                                        delete $scope.view.metadata.orders;
                                    }
                                }
                            }

                            $dataModel.refresh($scope.componentid);
                        };


                        var viewInfo = {};
                        viewInfo.data = data;
                        viewInfo.metadata = metadata;
                        viewInfo.showas = 'popup';
                        viewInfo.width = 441;
                        viewInfo.height = 441;
                        viewInfo.applicationid = APPLANE_DEVELOPER_APP_ID;
                        viewInfo.ask = APPLANE_DEVELOPER_ASK;
                        viewInfo.element = $event.target;
                        viewInfo[PARENT_COMPONENT_ID] = $scope.componentid;
                        $scope.childviewinfo = viewInfo;
                        $appDomService.addView(viewInfo, $scope);
                    }

                    $scope.columnview = function ($event) {

                        var columns = [
                            {"label":"Up", "expression":"up", "type":"up", "width":40, "visibility":"Table"},
                            {"label":"Down", "expression":"down", "type":"down", "width":40, "visibility":"Table"},
                            {"expression":"label", "type":"string", "label":"Label", "width":100, "visibility":"Both"},
                            {"expression":"expression", "type":"string", "label":"Expression", "width":200, "visibility":"Both"},
                            {"label":"Width", "expression":"width", "type":"text", "width":50, "visibility":"Table"}


                        ];
                        if ($scope.developer) {
                            columns.push({"label":"Visibility", "expression":"visibility", "type":"lookup", "width":100, "options":["Both", "Table", "Panel", "Child", "Off", "Query", "None", "Embed"], "visibility":"Table"});
                            columns.push({"label":"Update", "expression":"update", "type":"boolean", "width":50, "visibility":"Table"});
                            columns.push({"expression":"filter", "type":"object", "label":"Filter", "width":100, "visibility":"Both"});
                            columns.push({"label":"Visible Exp", "expression":"visibleexpression", "type":"string", "width":100, "visibility":"Table"});
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

                            $appService.getDataFromJQuery(url, param, "POST", "JSON", "Loading...", function (data) {
                                var v = $dataModel.getView($scope[COMPONENT_ID]);

                                $appDomService.reOpen(v);

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

                        $appDomService.addView(viewInfo, $scope);
                    }
                    $scope.resetfilter = function () {
                        alert("Reset filter called");
                        return;
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
                                paramValue = angular.copy(paramValue);

                                if (angular.isObject(paramValue) && advanceFilter.options && advanceFilter.options.length > 0) {
                                    paramValue = paramValue[VAL];
                                }
                                if (angular.isObject(paramValue)) {
                                    delete paramValue[VAL];
                                }

//                                var customizationFilter = {};
//                                customizationFilter[expression] = filter;
//                                var customizationParam = {};
//                                customizationParam[expression] = paramValue;

//                                filterCustomization.push({"expression":expression, "filters":[customizationFilter], "parameters":customizationParam});
//                                filterCustomization.push({"filters":[customizationFilter], "parameters":customizationParam});

                                $dataModel.addFilter($scope[COMPONENT_ID], expression, paramValue);
                            }
                        }

//                        save filter customization


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
                                "<bs-typeahead class='app-float-left app-filter-view-margin' ng-init=\"row=view.filterparameters;colmetadata=advancefilter\" onselection =\"onSelection\" ></bs-typeahead>" +
                                "</div>" +

                                "<div ng-switch-when='datefilter'>" +
                                "<app-date-filter class='app-float-left app-filter-view-margin' ng-init='row=view.filterparameters;colmetadata=advancefilter;'></app-date-filter>" +
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
                            data.data.push($scope.view.data.data[0]);
                        } else {
                            var currentRow = $dataModel.getCurrentRow($scope.componentid);
                            data.data.push(currentRow);

                            filter[$scope.view.metadata[PRIMARY_COLUMN]] = "{" + $scope.view.metadata[PRIMARY_COLUMN] + "}";

                            Object.keys(currentRow).forEach(function (k) {
                                parameters[k] = angular.copy(currentRow[k]);
                            });

                        }


                        var panelmetadata = {"table":$scope.view.metadata.table, "columns":panelColumns, "type":"panel", "layout":"onecolumns", "filter":filter, "parameters":parameters};
                        panelmetadata[PRIMARY_COLUMN] = $scope.view.metadata[PRIMARY_COLUMN];
                        panelmetadata.save = false;
                        panelmetadata.insert = false;
                        panelmetadata.delete = false;
                        panelmetadata.refresh = false;
                        panelmetadata.navigation = false;
                        panelmetadata.enablemetadataaction = false;
                        var row = data.data[0];
                        var rowId = row[panelmetadata[PRIMARY_COLUMN]];
                        if (!$dataModel.isTempKey(rowId)) {
                            panelmetadata.refreshonload = true;
                        }

                        viewinfo.metadata = panelmetadata;

                        viewinfo.data = data;
                        viewinfo.applicationid = $scope.view.applicationid;
                        viewinfo.ask = $scope.view.ask;
                        viewinfo.osk = $scope.view.osk;
                        viewinfo[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                        $appDomService.addView(viewinfo, $scope);


                    }
                    $scope.onRowActionClick = function (action) {
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

                            Object.keys(currentRowClone).forEach(function (k) {
                                parameters[k] = currentRowClone[k];
                            })
                            clone[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                            clone.parameters = parameters;
                            clone.ask = $scope.view.ask;
                            clone.osk = $scope.view.osk;
                            clone.applicationid = $scope.view.applicationid;
                            clone.parametermappings = parameterMappings;
                            $appDomService.addView(clone, $scope);
                        }
                    }


                    $scope.close = function (action) {
                        $appDomService.closeView($scope.componentid);
                    };

                    $scope.insert = function (action) {
                        //check if current record has some columns where visibility  = panel then we need to show panel form to insert
//                        var v = $dataModel.getView($scope.componentid);
//                        var columnsClone = v.metadata.columnsclone;
//                        var count = columnsClone.length;
//                        var showPanel = false;
//                        for (var i = 0; i < count; i++) {
//                            var c = columnsClone[i];
//
//                            if ((c.visibility && c.visibility == 'Panel')) {
//                                showPanel = true;
//                                break;
//                            }
//
//                        }
                        var showPanel = true;
                        if ($scope.view.metadata.insertview && $scope.view.metadata.insertview == 'table') {
                            showPanel = false;
                        }
                        if ($scope.view[SHOW_AS] && $scope.view[SHOW_AS] == "popup") {
                            showPanel = false;
                        }
                        if (showPanel) {
                            $dataModel.insert($scope.componentid);
                            $scope.openPanel(true);
                        } else {
                            $dataModel.insert($scope.componentid);
                        }

                    };


                    $scope.save = function () {

                        var callback = $scope.view.metadata.savecallback;
                        var closeonsave = $scope.view.metadata.closeonsave;

                        if (callback) {
                            var data = $scope.view.data.data;
                            var updates = $dataModel.getUpdates($scope[COMPONENT_ID]);

                            if (closeonsave) {
                                $scope.close();
                            }
                            callback(data, updates);
                            return;
                        }

                        $dataModel.save($scope.componentid, function () {
                            $appDomService.closeChildView($scope.componentid);
                        });
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
                                        $appDomService.closeChildView($scope.componentid);
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

                            $confirmationScope.confirmationPopUp = $appDomService.showPopup(modal);
                        } else {
                            $dataModel.refresh($scope.componentid, function () {
                                $appDomService.closeChildView($scope.componentid);
                            });
                        }
                    };

                    $scope.deleteData = function () {
                        $dataModel.deleteData($scope.componentid);
                    };

                    $scope.viewResize = function () {
                        $appDomService.viewResize($scope.componentid);
                    }

                    $scope.loadView = function (action) {

                        var useCurrentRow = action[USER_CURRENT_ROW];
                        if (useCurrentRow) {


                        } else {
                            var clone = angular.copy(action.view);
                            clone[PARENT_COMPONENT_ID] = $scope[COMPONENT_ID];
                            $appDomService.addView(clone, $scope);
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
                        $appDomService.addView(viewinfo, $scope);


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

                                var newViewInfo = customization[0];

                                var viewPrimaryColumn = $scope.view.metadata[PRIMARY_COLUMN]
                                var table = $scope.view.metadata.table;
                                var tableParam = table + "_" + viewPrimaryColumn

                                var url = BAAS_SERVER + "/custom/module";
                                var parameters = {"pask":$scope.view.ask, "posk":$scope.view.osk, "customization":JSON.stringify({"label":newViewInfo.label, "callback":false, "level":newViewInfo.apply._val, "override":newViewInfo.override}), "viewid":$scope.view.viewid};
                                var param = {"ask":"appsstudio", "module":"CustomizeViewService", "method":"saveCopyView", "parameters":JSON.stringify(parameters)}

                                $appService.getDataFromJQuery(url, param, "POST", "JSON", "Loading...", function (data) {
                                    var v = $dataModel.getView($scope[COMPONENT_ID]);
                                    $appDomService.reOpen(v);
                                });


                            }
                            var quickviewcollection = {"label":$scope.view.metadata.quickviews[$scope.view.metadata.quickviewindex].label, "display":["label"], "callback":function (option) {
                                $appDomService.addView({viewid:option.id, ask:option.ask, osk:option.osk}, $dataModel.getScope($scope[COMPONENT_ID]).$parent);
                            }, "settingimage":"images/savequickview.png"};

                            var overrideOptions = ["Self", "Organization"];
                            if ($scope.developer) {
                                overrideOptions.push("Application");
                            }
                            var quickViewColumns = [
                                {"expression":"label", "type":"text", "width":200, "visibility":"Both", "label":"label"},
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
                        if ($scope.view.metadata.update) {
                            actions.push({"method":"update(action)", "label":"Update", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-update-button app-bar-button"});
                        }

                        if ($scope.view.metadata.appliedfilters) {
                            actions.push({"method":"", "label":"", "template":"<app-applied-filters></app-applied-filters>", "class":""});
                        }

                        if ($dataModel.isTrueOrUndefined($scope.view.metadata.navigation)) {
                            actions.push({"method":"", "label":"", "template":"<app-navigation></app-navigation>", "class":""});
                        }

                        if ($dataModel.isTrueOrUndefined($scope.view.metadata.resize)) {
                            actions.push({"method":"viewResize()", "label":"Resize", "template":"<div class='ACTION_CLASS' ng-show='resizeenabled' title='ACTION_LABEL' ng-click='ACTION_METHOD'></div>", "class":"app-resize-button app-bar-button"});
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

                    html += "<div class='app-bar-button app-column-order-by-button' ng-click=\"orderby($event)\" title='Order By'></div>";
                    html += "<div class='app-bar-button app-filter-view-button' ng-show='view.metadata.viewfilters' ng-click=\"filterview($event)\" title='Manage filters'></div>";
                    if ($scope.developer) {
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
            "<div ng-bind-template='{{appliedFilter.label + \":\" + getColumnValue(view.filterparameters,appliedFilter,false)}}' class='app-applied-filter-label' ></div>" +
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


appStrapDirectives.directive("appText", ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:'<input type=\"text\"  class="' + FOCUS_ELEMENT_CLASS + '" ng-model="row[colmetadata.expression]" placeholder="{{colmetadata.label}}" ng-model-onblur />'
    }
}]);

appStrapDirectives.directive("appCheckbox", ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<input class='app-height-auto' style='padding: 0px;margin: 5px 0px 0px;outline: none;' type=\"checkbox\" ng-model='row[colmetadata.expression]'/>"
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
                            template:"<div><app-row-action  ng-init=\"actions=view.metadata.rowactions\"></app-row-action></div>",
                            scope:rowActionScope,
                            hideonclick:true,
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
                    var metadata = $scope.view.metadata;
                    var rowactions = [];
                    if ($dataModel.isTrueOrUndefined(metadata.enablerowaction)) {
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
                            child.relatedcolumn = KEY;
                            child.childcolumn = column.childcolumn;
                            if (column.filter) {
                                child.filter = column.filter;
                            }
                            rowactions.push(child);
                        }


                    }


                    metadata.rowactions = rowactions;

                    var newColumns = [];

                    if ($dataModel.isTrueOrUndefined(metadata.enableselection)) {
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

                        var visibility = column.visibility;
                        if (!(visibility == 'Table' || visibility == 'Both')) {
                            continue;
                        }
                        if (column.subvisibility && column.subvisibility == 'Embed') {
                            continue;
                        }


                        var type = column.type;

                        if (type == 'lookup') {

                            var multiple = column.multiple;

                            if (multiple) {
                                column.editableCellTemplate = "<div class='app-float-left app-width-full app-white-backgroud-color'><bs-multi-refrence-typeahead ng-init='colmetadata=col;'></bs-multi-refrence-typeahead></div>";

                                column.width = 280;
                            } else {
                                column.editableCellTemplate = "<bs-typeahead style=\"border:none;\" ng-init=\"colmetadata=col\"></bs-typeahead>";
                            }

                            var primarycolumns = column.primarycolumns;
                            if (primarycolumns && !angular.isArray(primarycolumns)) {
                                primarycolumns = JSON.parse(primarycolumns);
                                column.primarycolumns = primarycolumns;
                            }
                            var filter = angular.copy(column)
                            var expression = filter.expression;

                            if ($dataModel.isFilterApplied($scope.componentid, expression)) {
                                appliedfilters.push(filter);
                            }
                            filters.push(filter);
                        } else if (type == 'duration') {
                            column.editableCellTemplate = "<app-duration style=\"border:none;\" ng-init = \"colmetadata=col;\"  ></app-duration>";
                        } else if (type == CURRENCY_TYPE) {
                            column.editableCellTemplate = "<app-currency style=\"border:none;\" ng-init = \"colmetadata=col;\"  ></app-currency>";
                        } else if (type === 'date' || type == 'datetime') {

                            column.editableCellTemplate = '<div class="app-grid-datepicker-parent">' +
                                '<input type="text" ng-init="colmetadata=col;"  ng-model="row[col.expression]"  data-date-format="dd/mm/yyyy" app-datepicker class="app-grid-date-picker-input ' + FOCUS_ELEMENT_CLASS + '">' +
                                '<input type="text" data-toggle="datepicker" class="app-grid-date-picker-calender-image" tabindex="-1"/>' +
                                '</div>'
                            column.cellTemplate = '<span ng-bind="row[col.expression]"></span>';

                            var filter = angular.copy(column)
                            filter.type = "datefilter";

                            var expression = filter.expression;

                            if ($dataModel.isFilterApplied($scope.componentid, expression)) {
                                appliedfilters.push(filter);
                            }
                            filters.push(filter);

                        } else if (type == 'up') {
                            column.cellTemplate = "<span ng-click='onUpClick(row)' class='app-panel-arrows-parent'><img width='15px' height='15px' src='images/up-arrow.png'></span>";
                        } else if (type == 'down') {
                            column.cellTemplate = "<span ng-click='onDownClick(row)' class='app-panel-arrows-parent'><img width='15px' height='15px' src='images/down_arrow_new.png'></span>";
                        } else if (type == 'boolean') {
                            column.editableCellTemplate = "<div class='app-text-align-center app-height-full'><app-checkbox  ng-init=\"colmetadata=col\" ></app-checkbox>";
                        } else if (type == 'file') {
                            column.editableCellTemplate = "<app-file-upload ng-init=\"colmetadata=col\" class='app-float-left app-width-full app-height-full'></app-file-upload>";

                            column.width = 300;
                        } else if (type == 'schedule') {
                            var dateColumn = angular.copy(column.datemetadata);
                            dateColumn.editableCellTemplate = '<div class="app-grid-datepicker-parent">' +
                                '<input type="text" ng-init="colmetadata=col;"  ng-model="row[col.expression]"  data-date-format="dd/mm/yyyy" app-datepicker class="app-grid-date-picker-input ' + FOCUS_ELEMENT_CLASS + '">' +
                                '<input type="text" data-toggle="datepicker" class="app-grid-date-picker-calender-image" tabindex="-1"/>' +
                                '</div>'
                            dateColumn.cellTemplate = '<span ng-bind="row[col.expression]"></span>';

                            var filter = angular.copy(dateColumn)
                            filter.type = "datefilter";

                            var expression = filter.expression;

                            if ($dataModel.isFilterApplied($scope.componentid, expression)) {
                                appliedfilters.push(filter);
                            }
                            filters.push(filter);

                            newColumns.push(dateColumn);

                            column.cellTemplate = "Schedule";
                            column.editableCellTemplate = "<app-schedule-pop-up ng-init=\"colmetadata=col;\"></app-schedule-pop-up>";
                            column.width = 495;

                        } else {
                            column.editableCellTemplate = "<app-text class='" + FOCUS_ELEMENT_CLASS + "' style=\"border:none;padding:0px;margin:0px;width:100%;height:100%;\" ng-init=\"colmetadata=col\" ></app-text>";
                        }
                        newColumns.push(column);
                    }
                    metadata.update = update;

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
                    if (metadata.refreshonload) {
                        $scope.refresh();
                    }
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
        template:"<div class='app-height-full app-font-weight-bold app-navigation'>" +
            "<div class='app-height-full app-left-arrow app-float-left app-height-twenty-px app-width-twenty-px app-cursor-pointer' ng-click='showRecords(false)'  ng-show='view.metadata.datastate.prev' style='padding-top:22px;'></div>" +
            "<div class='app-float-left' style='letter-spacing: 5px;padding: 6px 2px 0 6px'>{{view.metadata.datastate.fromindex +'-' + view.metadata.datastate.toindex}}</div>" +
            "<div class='app-height-full app-right-arrow app-float-left app-height-twenty-px app-width-twenty-px app-cursor-pointer' ng-click='showRecords(true)' ng-show='view.metadata.datastate.next' style='padding-top:22px;'></div>" +
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


appStrapDirectives.directive('appFileUpload', ['$appService', '$compile', function ($appService, $compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='" + FOCUS_ELEMENT_CLASS + "'>" +

            "<input ng-show='row[colmetadata.expression].length == 0' style='width: 80px;outline: none;'  class='app-float-left' type='file' id='uploadfile'/>" +
            "<div ng-show='row[colmetadata.expression].length > 0' class='app-float-left' tabindex='-1'style='padding-top:3px;padding-right: 24px;'><a ng-href='http://baas.applane.com:1337/file/download?filekey={{row[colmetadata.expression][0].key}}&ask=frontend'>{{row[colmetadata.expression][0].name}}</a></div>" +
            "<div  ng-show='row[colmetadata.expression] == 0'  class='app-float-left app-padding-top-four-px app-padding-left-ten-px'>No file Choosen</div>" +
            "<input ng-show='row[colmetadata.expression].length > 0'  type='button' class='app-none-background app-none-border app-text-decoration-underline app-remove-file-button' value='remove' ng-click='removeFile()'/>" +
            "</div>",
        compile:function () {
            return {
                post:function ($scope, iElement) {

                    $scope.removeFile = function () {
                        $scope.row[$scope.colmetadata.expression].splice(0, 1);
                    };

                    $scope.showFile = function (file) {
                        $scope.fileurl = BAAS_SERVER + "/file/download?filekey=" + file[0][FILE_KEY] + "&ask=frontend";
                        $scope.filename = file[0][FILE_NAME];
                        $scope.row[$scope.colmetadata.expression] = file;

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
                        $appService.getDataFromJQuery(BAAS_SERVER + '/file/upload', current_file, "POST", "JSON", "Uploading...", function (data) {
                            if (data != null && data != undefined && data.length > 0) {
                                $scope.showFile(data);
                            }
                        });
                    };

                    iElement.bind('change', function () {
                        $scope.$apply(function () {
                            $scope.oFReader = new FileReader();
                            if (document.getElementById('uploadfile').files.length === 0) {
                                return;
                            }
                            $scope.oFile = document.getElementById('uploadfile').files[0];
                            $scope.oFReader.onload = $scope.loadFile;
                            $scope.oFReader.readAsDataURL($scope.oFile);
                        });
                    });
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
                        var currentView = $scope.view;
                        var row = $scope.row;

//                        var nestedColumns = column.columns;
//                        for (var i = 0; i < nestedColumns.length; i++) {
//                            var nestedColumn = nestedColumns[i];
//                            delete nestedColumn.subvisibility;
//
//                        }

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


                        var expression = column.expression;

                        var nestedData = {};
                        nestedData.data = row[expression];
                        var view = {"metadata":nestedMetaData, "data":nestedData};
                        view.ask = currentView.ask;
                        view.osk = currentView.osk;
                        view.applicationid = currentView.applicationid;

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
            template:"<div class ='duration-container' composite-data-container='true'>" +
                "<div class='duration-time-container'>" +
                "<app-text class='duration-input' ng-init = \"colmetadata=colmetadata.timemetadata\" ></app-text>" +
                "</div>" +
                "<div class='duration-ref-container'>" +
                "<bs-typeahead ng-init=\"colmetadata=colmetadata.timeunitmetadata;\" class='border-style' ></bs-typeahead>" +
                "</div>" +
                "</div>",
            compile:function () {
                return {

                    post:function ($scope, iElement) {


//                        var durationValue = $scope.row[$scope.colmetadata.expression];
//                        if (!durationValue) {
//                            $scope.row[$scope.colmetadata.expression] = {};
//                        }
//
//                        var durationTemplate = "<app-text class='duration-input' ng-init = \"row=row[colmetadata.expression];colmetadata=colmetadata.timemetadata\" ></app-text>";
//                        var durationField = ($compile(durationTemplate)($scope));
//                        var divElements = iElement.find("div");
//                        $(divElements[0]).append(durationField);
//                        var durationTypeTemplate = "<bs-typeahead ng-init=\"row=row[colmetadata.expression];colmetadata=colmetadata.timeunitmetadata;\" class='border-style' ></bs-typeahead>";
//                        var durationTypeField = $compile(durationTypeTemplate)($scope);
//                        $(divElements[1]).append(durationTypeField);

                    }
                }
            }

        }
    }
]);

appStrapDirectives.directive('appCurrency', [
    '$compile',
    function ($compile) {
        return {
            restrict:"E",
            replace:true,
            scope:true,
            template:"<div class ='duration-container' composite-data-container='true'>" +
                "<div class='duration-time-container'>" +
                "<app-text class='duration-input' ng-init = \"colmetadata=colmetadata.amountmetadata\" ></app-text>" +
                "</div>" +
                "<div class='duration-ref-container'>" +
                "<bs-typeahead ng-init=\"colmetadata=colmetadata.typemetadata;\" class='border-style' ></bs-typeahead>" +
                "</div>" +
                "</div>",
            compile:function () {
                return {
                    post:function ($scope, iElement) {

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
            params.usergroup = 'baas';

            this.getDataFromJQuery(BAAS_SERVER + "/login", params, "POST", "JSON", "Loading...", function (callBackData) {
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


            var url = BAAS_SERVER + "/custom/module";


            var viewParameters = {};
            viewParameters.pask = pView.ask;
            viewParameters.posk = pView.osk;
            viewParameters.viewid = pView.viewid;

            var viewIdInfo = {};
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


            this.getDataFromJQuery(url, data, "POST", "JSON", "Loading...", function (callBackData) {
                callBack(callBackData);
            });


        }

        $appService.logOut = function (callBack) {
            var url = BAAS_SERVER + "/logout";
            this.getDataFromJQuery(url, {}, "POST", "JSON", "Sign out...", callBack);

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

            var usk;
            if (typeof(Storage) !== "undefined") {
                usk = localStorage.usk;
            }

            this.getDataFromJQuery(BAAS_SERVER + "/custom/module", {"usk":usk, "ask":"appsstudio", "module":"UserService", "method":"getUserState", parameters:JSON.stringify({"state":state, "usk":usk})}, "POST", "JSON", "Loading...", function (callBackData) {
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
            this.getDataFromJQuery(url, data, "POST", "JSON", bussyMessage, function (callBackData) {
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
            this.getDataFromJQuery(url, params, "POST", "JSON", "Saving...", function (callBackData) {
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

            var usk;
            if (typeof(Storage) !== "undefined") {
                usk = localStorage.usk;
            }
            requestBody.usk = usk;
            requestBody.enablelogs = true;

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

                    if (errcallback) {
                        errcallback(jqXHR, exception);
                    } else {
                        var m = jqXHR.responseText;
                        try {
                            var jsonError = JSON.parse(jqXHR.responseText)
                            if (jsonError.code == 1) {
                                alert("Session expired. Please login again.");
                                window.location.href = "/login.html";
                                return;
                            }
                            m = jsonError.stack
                        } catch (e) {

                        }

                        alert(m);
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
        $appDomService.reOpen = function (v) {
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
                if ($scope.viewgroupcollection) {
                    view.viewgroup = $scope.viewgroupcollection.label;
                }
                if ($scope.currentmenu) {
                    view.menulabel = $scope.currentmenu.label;
                }

                $appService.getView(view, function (v) {
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
                var parentScope = $dataModel.getScope(parentComponentId);
                if (parentScope) {
                    parentScope.resizeenabled = true;
                }
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
                view.metadata.resize = true;

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

        $appDomService.viewResize = function (componentid) {
            var view = $appDomService.views[componentid];
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
            var dataModelView = $dataModel.getView(componentId);
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

                    if (!dataModelView.metadata.fullMode) {
                        $("#" + superParentComponentId).css({left:0, right:'50%', 'display':"block"});
                        $("#" + parentViewComponentId).css({left:'50%', right:0, 'display':"block"});
                    } else {
                        $("#" + superParentComponentId).css({left:0, right:'0', 'display':"block"});
                        $("#" + parentViewComponentId).css({left:'0', right:0, 'display':"block"});
                    }
//                    $("#" + superParentComponentId).css({left:0, right:'50%'});
//                    $("#" + parentViewComponentId).css({left:'50%', right:0});

                    $("#" + superParentComponentId).resize();
                    $("#" + parentViewComponentId).resize();
                } else {
                    $("#" + parentViewComponentId).css({left:0, right:0, 'display':"block"});
                    var parentScope = $dataModel.getScope(parentViewComponentId);
                    if (parentScope) {
                        parentScope.resizeenabled = false;
                    }
                    $("#" + parentViewComponentId).resize();
                }
            }
        }

        $appDomService.closeChildView = function (componentId) {
            var parentViewInfo = this.views[componentId];

            //check if parent has already some child or not, if yes then remove all child
            if (parentViewInfo && parentViewInfo[CHILD_COMPONENT_ID]) {
                this.closeView(parentViewInfo[CHILD_COMPONENT_ID]);
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
            var index = this.getIndex(key, id, model.view.metadata[PRIMARY_COLUMN]);
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
            var index = this.getIndex(key, id, model.view.metadata[PRIMARY_COLUMN]);


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


        $dataModel.getIndex = function (key, id, primaryColumn) {
            var model = this.getModel(id);
            var view = model.view;
            var data = view.data.data;

            var dataCount = data.length;
            for (var i = 0; i < dataCount; i++) {
                var record = data[i];
                var recordKey = record[primaryColumn];
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

            if (model.view.data.moduleresult && model.view.data.moduleresult.aggregates) {
                scope.$watch("view.data.data", function (newvalue, oldvalue) {

                    var newValueCount = newvalue ? newvalue.length : 0;
                    var oldValueCount = oldvalue ? oldvalue.length : 0;

                    var columns = model.view.metadata.columns;
                    var columnsCount = columns ? columns.length : 0;

                    for (var i = 0; i < newValueCount; i++) {
                        var newRow = newvalue[i];
                        var newKey = newRow[KEY];

                        for (var j = 0; j < oldValueCount; j++) {
                            var oldRow = oldvalue[j];
                            var oldKey = oldRow[KEY];

                            if (newKey == oldKey) {
                                for (var k = 0; k < columnsCount; k++) {
                                    var column = columns[k];
                                    var type = column.type;
                                    if (type == 'duration') {
                                        var exp = column.timemetadata.expression;
                                        var newDurationvalue = newRow[exp];
                                        var oldDurationvalue = oldRow[exp];

                                        if (!angular.equals(newDurationvalue, oldDurationvalue)) {
                                            if (newDurationvalue) {
                                                if (typeof newDurationvalue == 'string') {
                                                    newDurationvalue = parseInt(newDurationvalue);
                                                }
                                            } else {
                                                newDurationvalue = 0;
                                            }

                                            if (oldDurationvalue) {
                                                if (typeof oldDurationvalue == 'string') {
                                                    oldDurationvalue = parseInt(oldDurationvalue);
                                                }
                                            } else {
                                                oldDurationvalue = 0;
                                            }

                                            model.view.data.moduleresult.aggregates[exp] -= oldDurationvalue;
                                            model.view.data.moduleresult.aggregates[exp] += newDurationvalue;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }, true);
            }
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
            this.updateColumnMetadata(id);
            var data = view.data.data;
            var dataLength = data ? data.length : 0;

            for (var i = 0; i < dataLength; i++) {
                this.populateMandatoryColumnTypeValue(id, data[i]);
            }

            var moduleResult = view.data.moduleresult;

            if (moduleResult && moduleResult.aggregates) {
                this.populateMandatoryColumnTypeValue(id, moduleResult.aggregates);
            }


            view.dataclone = angular.copy(data);
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
            this.populateDataState(id);
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
                        var parentView = that.getModel(parentComponentId).view;
                        var parentData = parentView.data.data;
                        var parentDataClone = parentView.dataclone;
                        var currentData = view.data.data[0];
                        var parentRecord = getRecord(parentData, currentData[view.metadata[PRIMARY_COLUMN]], view.metadata[PRIMARY_COLUMN]);

                        if (!parentRecord) {
                            throw new Error("Parent record not found");
                        }
                        var parentRecordClone = getRecord(parentDataClone, currentData[view.metadata[PRIMARY_COLUMN]], view.metadata[PRIMARY_COLUMN]);
                        if (!parentRecordClone) {
                            throw new Error("Parent record clone not found");
                        }
                        if (response && response.data && response.data.length > 0) {
                            var panelData = response.data[0];
                            var tempClonecolumns = angular.copy(columns);
                            that.updateColumnMetadata(componentid, tempClonecolumns);
                            that.populateMandatoryColumnTypeValue(componentid, panelData, tempClonecolumns);

                            for (var i = 0; i < columns.length; i++) {
                                var column = tempClonecolumns[i];
                                var parentExpData = panelData[column.expression];
                                if (parentExpData && angular.isArray(parentExpData)) {
                                    var oldRecord = parentRecord[column.expression];

                                    oldRecord.splice(0);
                                    for (var j = 0; j < parentExpData.length; j++) {
                                        oldRecord.push(parentExpData[j]);
                                    }
                                } else {
                                    parentRecord[column.expression] = parentExpData;
                                }
                                parentRecordClone[column.expression] = angular.copy(parentExpData);
                            }
                        }


                    } else {
                        view.data = response;
                        var records = view.data.data;
                        var recordCount = records ? records.length : 0;
                        for (var i = 0; i < recordCount; i++) {
                            var obj = records[i];
                            that.populateMandatoryColumnTypeValue(componentid, obj);
                        }
                        view.dataclone = angular.copy(records);
                    }

                    that.populateDataState(componentid);

                    if (refreshCallback) {
                        refreshCallback();
                    }

                    if (!model.scope.$$phase) {
                        model.scope.$apply();
                    }
                } catch (e) {
                    alert(e.message);
                }
            };

            if (columns.length > 0) {
                var query = {"table":view.metadata.table, "columns":columns, "filter":view.metadata.filter, "parameters":parameters};
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

        $dataModel.insert = function (componentid) {
            var model = this[componentid];
            var tempKey = this.getTempKey(model.view.metadata[PRIMARY_COLUMN]);
            var data = model.view.data.data;

            this.populateMandatoryColumnTypeValue(componentid, tempKey);

            data.splice(0, 0, tempKey);
            model.view.dataclone.splice(0, 0, angular.copy(tempKey));
        }

        $dataModel.deleteData = function (componentid) {
            var model = this[componentid];
            var view = model.view;
            var data = view.data.data;
            var primaryColumn = view.metadata[PRIMARY_COLUMN];

            var selectedItems = this.getSelectedKeys(componentid);

            var selectedCount = selectedItems.length;
            if (selectedCount > 0) {
                var deletedItems = [];

                for (var i = 0; i < selectedCount; i++) {
                    var selectedItem = selectedItems[i];
                    var key = selectedItem[primaryColumn];
                    var dataIndex = $dataModel.getIndex(key, componentid, primaryColumn);
                    if (dataIndex < 0) {
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

            filter[pFilterExp] = pParamValue;


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
                    if (saveCallBack) {
                        saveCallBack();
                    }
                    that.refresh(componentid);
                };
                $appService.save({"table":view.metadata.table, "filter":view.metadata.filter, "parameters":view.metadata.parameters, "operations":newInserts}, ask, osk, callBack);
            } else {
                alert("No data found for saving");
            }
        }

        $dataModel.getColumnUpdates = function (data, cloneData, columns, primaryColumn) {
            if (!primaryColumn) {
                throw new Error("Primary column not defined");
            }


            var noOfColumns = columns.length;

            var newInserts = [];
            var noOfDataRecords = data.length;
            for (var i = 0; i < noOfDataRecords; i++) {
                var record = data[i];
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
                    var multiple = column.multiple;
                    var oldValue = cloneRecord[expression];
                    var newValue = record[expression];
                    if (type == DURATION_TYPE) {
                        //merge values
                        var timeValue = record[expression + ".time"];
                        var timeUnitValue = record[expression + ".timeunit"];
                        if (timeValue && timeUnitValue) {
                            newValue = angular.copy(newValue);
                            newValue.time = timeValue
                            newValue.timeunit = timeUnitValue;
                        }

                        var oldTimeValue = cloneRecord[expression + ".time"];
                        var oldTimeUnitValue = cloneRecord[expression + ".timeunit"];
                        if (oldTimeValue && oldTimeUnitValue) {
                            oldValue = angular.copy(oldValue);
                            oldValue.time = oldTimeValue
                            oldValue.timeunit = oldTimeUnitValue;
                        }
                    }

                    if (type == CURRENCY_TYPE) {
                        var amountValue = record[expression + ".amount"];
                        var typeValue = record[expression + ".type"];
                        if (amountValue && typeValue) {
                            newValue = angular.copy(newValue);
                            newValue.amount = amountValue
                            newValue.type = typeValue;
                        } else {
                            newValue = undefined
                        }

                        var oldAmount = cloneRecord[expression + ".amount"];
                        var oldType = cloneRecord[expression + ".type"];
                        if (oldAmount && oldType) {
                            oldValue = angular.copy(oldValue);
                            oldValue.amount = oldAmount;
                            oldValue.type = oldType;
                        } else {
                            oldValue = undefined
                        }
                    }


                    if (!angular.equals(oldValue, newValue)) {
                        //clone newValue so that changes in this will nt reflect in data
                        if (newValue) {
                            newValue = angular.copy(newValue);
                        }


                        if (type == LOOK_UP_TYPE) {

                            if (multiple) {
                                newValue = {'data':newValue, "override":true};
                            } else {
                                if (column.options && column.options.length > 0) {
                                    if (angular.isObject(newValue)) {
                                        newValue = newValue._val
                                    }
                                }
                                if (angular.isObject(newValue)) {
                                    var _val = newValue._val
                                    delete newValue._val;
                                    if (Object.keys(newValue).length == 0) {
                                        newValue = undefined;
                                    }
                                    if (_val === null) {
                                        newValue = null;
                                    }
                                }

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

                        } else if (type == DURATION_TYPE) {
                            if (angular.isObject(newValue) && newValue.time && newValue.timeunit._val) {
                                newValue = {time:newValue.time, timeunit:newValue.timeunit._val}
                            }


                        } else if (type == 'object') {
                            if (newValue) {
                                if (multiple && angular.isArray(newValue)) {
                                    newValue = $dataModel.getColumnUpdates(newValue, oldValue, column.columns, KEY)

                                } else if (!(angular.isArray(newValue) || angular.isArray(newValue)) && newValue.toString().trim().length > 0) {
                                    newValue = JSON.parse(newValue);
                                }
                            } else {
                                newValue = null;
                            }
                        } else if (type == CURRENCY_TYPE) {

                            if (angular.isObject(newValue) && newValue.amount && newValue.type._val) {
                                delete newValue.type._val;
                            }
                        }

                        if (newValue !== undefined) {
                            hasUpdates = true;
                            updates[expression] = newValue;
                            //check for dotted expression
                            var dottexExpression = expression;
                            var dottIndex = dottexExpression.indexOf(".");
                            var dottedConcatExpression = "";
                            while (dottIndex >= 0) {
                                //we have to send _id value along with updates
                                var firstExpression = dottexExpression.substring(0, dottIndex);
                                var firstIndex = dottedConcatExpression + firstExpression + "._id";
                                var updatedFirstIndex = updates[firstIndex];
                                var oldRecordFirstIndex = cloneRecord[firstIndex];
                                var currentRecordFirstIndex = record[firstIndex];
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
                    var key = record[primaryColumn];
                    var dataRecord = getRecord(data, key, primaryColumn);
                    if (dataRecord == null) {
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
            var data = view.data.data;

            var primaryColumn = view.metadata[PRIMARY_COLUMN];

            var columns = model.view.metadata.columnsclone;
            return $dataModel.getColumnUpdates(data, cloneData, columns, primaryColumn);
        }

        function getRecord(data, pKey, primaryColumn) {
            var dataCount = data ? data.length : 0;
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

        $dataModel.updateColumnMetadata = function (componentid, columns) {
            if (!columns) {
                var model = this.getModel(componentid);
                columns = model.view.metadata.columns;
            }

            var columnsLength = columns ? columns.length : 0;

            for (var i = 0; i < columnsLength; i++) {
                var column = columns[i];
                var type = column.type;
                var exp = column.expression;
                var options = column.options;
                var optionCount = options ? options.length : 0;
                if (type == LOOK_UP_TYPE && optionCount > 0) {
                    //create new options
                    if (!angular.isObject(options[0])) {
                        var newOptions = [];
                        for (var j = 0; j < optionCount; j++) {
                            newOptions[j] = {_id:j };
                            newOptions[j][exp] = options[j]
                        }
                        column.options = newOptions;
                    }
                    if (!column.primarycolumns) {
                        column.primarycolumns = [
                            {expression:exp, type:"string"}
                        ];
                    }

                } else if (type == LOOK_UP_TYPE && column.primarycolumns) {
                    var lookupColumnCount = column.primarycolumns.length;
                    var newLookupColumns = [];
                    for (var j = 0; j < lookupColumnCount; j++) {
                        var c = column.primarycolumns[j];
                        if (c.type == 'lookup') {
                            continue;
                        }
                        newLookupColumns.push(c);
                    }
                    if (newLookupColumns.length == 0) {
                        throw new Error("No simple column exists in [" + column.expression + "]");
                    }
                    column.primarycolumns = newLookupColumns;

                } else if (type == SCHEDULE_TYPE) {
                    var dateMetadata = {};
                    dateMetadata.expression = column.expression + ".duedate";
                    dateMetadata.id = column.expression + "_duedate";
                    dateMetadata.label = "Started date";
                    dateMetadata.type = "date";
                    dateMetadata.width = 200;
                    dateMetadata.visibility = column.visibility;
                    column.datemetadata = dateMetadata;
                } else if (type == DURATION_TYPE) {
                    var timeMetadata = {};
                    timeMetadata.expression = column.expression + ".time";
                    timeMetadata.id = column.expression + "_time";
                    timeMetadata.label = "Time";
                    timeMetadata.type = "string";
                    column.timemetadata = timeMetadata;

                    var timeUnitMetadata = {};
                    var timeUnitMetadataExp = column.expression + ".timeunit"
                    timeUnitMetadata.expression = timeUnitMetadataExp;
                    timeUnitMetadata.id = column.expression + "_timeunit";
                    timeUnitMetadata.label = "Unit";
                    timeUnitMetadata.width = 60;
                    timeUnitMetadata.height = '100%';
                    timeUnitMetadata.type = "lookup";
                    var val1 = {_id:1};
                    val1[timeUnitMetadataExp] = "Hrs"

                    var val2 = {_id:2};
                    val2[timeUnitMetadataExp] = "Days"
                    timeUnitMetadata.options = [val1, val2];
                    timeUnitMetadata.primarycolumns = [
                        {"expression":timeUnitMetadataExp, "type":"string"}
                    ]

                    column.timeunitmetadata = timeUnitMetadata;
                } else if (type == CURRENCY_TYPE) {
                    var amountMetadata = {};
                    amountMetadata.expression = column.expression + ".amount";
                    amountMetadata.id = column.expression + "_amount";
                    amountMetadata.label = "Amount";
                    amountMetadata.type = "string";
                    column.amountmetadata = amountMetadata;

                    var typeMetadata = {};
                    typeMetadata.expression = column.expression + ".type";
                    typeMetadata.id = column.expression + "_type";
                    typeMetadata.label = "Type";
                    typeMetadata.width = 60;
                    typeMetadata.height = '100%';
                    typeMetadata.type = "lookup";

                    typeMetadata.table = 'currencies__baas';
                    typeMetadata.primarycolumns = [
                        { "type":"string", "expression":"currency"}
                    ]

                    column.typemetadata = typeMetadata;
                } else if (type == 'view') {

                    this.updateColumnMetadata(componentid, column.columns);
                }
            }
        }
        $dataModel.populateMandatoryColumnTypeValue = function (componentid, row, columns) {


            var model = this.getModel(componentid);
            if (!columns) {
                columns = model.view.metadata.columns;
            }

            var columnsLength = columns ? columns.length : 0;

            for (var i = 0; i < columnsLength; i++) {
                var column = columns[i];
                var type = column.type;
                var exp = column.expression;
                if (!exp || !type) {
                    continue;
                }
                var multiple = column.multiple;
                var value = row[exp];
                var options = column.options;
                var optionCount = options ? options.length : 0;

                if (value == null || value == undefined) {
                    if (type == SCHEDULE_TYPE) {
                        row[exp] = {"span":"None", "time":"1", "frequency":"1"};
                    } else if (type == DURATION_TYPE) {
                        row[exp] = {timeunit:{"_val":""}};
                    } else if (type == LOOK_UP_TYPE) {
                        if (multiple) {
                            row[exp] = [];
                        } else {
                            row[exp] = {"_val":""};
                        }
                    } else if (type == "view") {
                        row[exp] = [];
                    } else if (type == 'file' || type == 'image') {
                        row[exp] = "";
                    } else if (type == CURRENCY_TYPE) {
                        row[exp] = {type:{"_val":""}};
                    }
                } else {
                    if (type == STRING_TYPE && columns[i].multiple && value instanceof Array) {
                        row[exp] = JSON.stringify(value);

                    } else if (type == "object" && !multiple) {
                        if (value) {
                            row[exp] = JSON.stringify(value);
                        }

                    } else if (type == "view") {
                        var multipleColumns = column.columns;
                        var arrayValue = row[exp];

                        var arrayCount = arrayValue ? arrayValue.length : 0;
                        for (var j = 0; j < arrayCount; j++) {
                            $dataModel.populateMandatoryColumnTypeValue(componentid, arrayValue[j], multipleColumns);

                        }
                    } else if (type == LOOK_UP_TYPE) {
                        if (!multiple) {
                            if (optionCount > 0) {
                                if (!angular.isObject(value)) {
                                    var newValue = {};
                                    newValue[exp] = value;
                                    newValue._val = value;
                                    row[exp] = newValue;
                                }
                            } else {
                                if (!(row[exp]._val)) {
                                    row[exp]._val = value[column.primarycolumns[0].expression];
                                }
                            }
                        } else {
                            //for multiple lookup
                        }
                    } else if ((type == 'date' || type == 'datetime') && value.indexOf("-") >= 0) {
                        row[exp] = new Date(value).getFormattedDate(DATE_FORMAT);
                    } else if (type == DURATION_TYPE) {

                        if (angular.isObject(value) && value.timeunit && !angular.isObject(value.timeunit)) {
                            row[column.timemetadata.expression] = value.time;
                            row[column.timeunitmetadata.expression] = {_val:value.timeunit};
                            row[column.timeunitmetadata.expression][column.timeunitmetadata.expression] = value.timeunit;
                            delete row[exp].time;
                            delete row[exp].timeunit;
                        }


                    } else if ((type == SCHEDULE_TYPE) && value.duedate && value.duedate.indexOf("-") >= 0) {
                        row[exp + ".duedate" ] = new Date(value.duedate).getFormattedDate(DATE_FORMAT);
                        delete value.duedate;
                    } else if (type == CURRENCY_TYPE) {
                        console.log(">>>>>>Currency::::" + JSON.stringify(column));
                        if (angular.isObject(value) && value.type && value.amount) {
                            row[column.amountmetadata.expression] = value.amount;
                            row[column.typemetadata.expression] = value.type;
                            row[column.typemetadata.expression]._val = value.type.currency;
                            delete row[exp].amount;
                            delete row[exp].type;
                        }
                    }
                }
            }
        }

        return $dataModel;

    }
]);

appStrapDirectives.directive('appDatepicker', ['$timeout', function ($timeout) {
    'use strict';
    return {
        restrict:'A',
        require:'?ngModel',
        scope:true,
        link:function postLink(scope, element) {
            var width = scope.colmetadata.width;
            var height = scope.colmetadata.height;

            if (!width) {
                width = 200;
            }
            if (!height) {
                height = 24;
            }

            element.parent().width(width);
            element.parent().height(height);

            element.datepicker({autoclose:true, format:DATE_FORMAT});
            element.on('changeDate', function (e) {
                scope.row[scope.colmetadata.expression] = e.date.getFormattedDate(DATE_FORMAT);
                if (!scope.$$phase) {
                    scope.$apply();
                }
            })

            element.on('keyup', function (e) {
                var value = element.val();
                var date = JSON.stringify(new Date(value));

                if (date == 'null' || date == 'undefined') {
                    return;
                } else {
                    scope.row[scope.colmetadata.expression] = value;
                    if (!scope.$$phase) {
                        scope.$apply();
                    }
                }
            })
        }
    };
}]
);


appStrapDirectives.directive('appSchedulePopUp', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-float-left app-padding-top-bottom-ten-px app-white-backgroud-color'>" +

            "<app-schedule-repeat></app-schedule-repeat>" +

            "<app-schedule-months ng-show='row[colmetadata.expression].span == \"Daily\"' ></app-schedule-months>" +

            "<app-schedule-time ></app-schedule-time>" +

            "<app-schedule-dates ng-show='row[colmetadata.expression].span == \"Monthly\"'></app-schedule-dates>" +

            "<div class='app-float-left app-width-full app-padding-five-px'>" +
            "<div class='app-float-left app-font-weight-bold app-text-align-right app-width-one-twenty-px'>Repeated Every : </div>" +
            "<div class='app-float-left app-padding-left-twenty-px'>" +
            '<select style="width:100px;" ng-change="getRepeatOptionsValue()"  ng-model="row[colmetadata.expression].frequency" >' +
            '<option value="1">1</option>' +
            '<option value="2">2</option>' +
            '<option value="3">3</option>' +
            '</select>' +
            "{{row[colmetadata.expression].span}}" +
            "</div>" +
            "</div>" +


            "<app-schedule-weeks ng-show='row[colmetadata.expression].span == \"Weekly\"'></app-schedule-weeks>" +


            "<div class='app-float-left app-width-full app-padding-five-px'>" +
            "<div class='app-float-left app-font-weight-bold app-text-align-right app-width-one-twenty-px'>Strats On : </div>" +
            "<div class='app-float-left app-padding-left-twenty-px' >" +
            '<div class="app-grid-datepicker-parent app-text-box-border">' +
            '<input type="text" data-date-format="dd/mm/yyyy" app-datepicker ng-model="row[colmetadata.expression]" ng-init="colmetadata=colmetadata.datemetadata;"  class="app-grid-date-picker-input" >' +
            '<input type="text" data-toggle="datepicker" class="app-grid-date-picker-calender-image" tabindex="-1"/>' +
            '</div>' +
            "</div>" +
            "</div>" +

            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement) {

                }
            }
        }
    }
}]);


appStrapDirectives.directive('appScheduleRepeat', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        template:"<div class='app-float-left app-width-full app-padding-five-px'>" +
            "<div class='app-float-left app-font-weight-bold app-text-align-right app-width-one-twenty-px'>Repeat : </div>" +
            "<div class='app-float-left app-padding-left-twenty-px' >" +

            '<select ng-model="row[colmetadata.expression].span" class="' + FOCUS_ELEMENT_CLASS + '">' +
            '<option value="None">None</option>' +
            '<option value="Daily">Daily</option>' +
            '<option value="Weekly">Weekly</option>' +
            '<option value="Monthly">Monthly</option>' +
            '<option value="Yearly">Yearly</option>' +
            '<option value="Hourly">Hourly</option>' +

            '</select>' +
            "</div>" +
            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement) {

                }
            }
        }
    }
}]);


appStrapDirectives.directive('appScheduleWeeks', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-float-left app-width-full app-padding-five-px'>" +
            "<div class='app-float-left app-font-weight-bold app-text-align-right app-width-one-twenty-px'>Repeat On : </div>" +
            "<div class='app-float-left app-padding-left-twenty-px' >" +
            "<div class='app-float-left' ng-repeat='week in weeks' style='width:41px;'>" +
            "<span class='app-float-left'><input class='app-margin-top-one-px' ng-model='row[colmetadata.expression][week.label]' type='checkbox' ></span>" +
            "<span class='app-float-left' style='padding:0px 7px 0px 5px;' ng-bind='week.slabel'></span>" +
            "</div>" +
            "</div>" +
            "</div>",
        compile:function () {
            return  {
                pre:function ($scope, iElement) {
                    $scope.weeks = [
                        {'slabel':'S', 'label':"sunday"},
                        {'slabel':'M', 'label':"monday"},
                        {'slabel':'T', 'label':"tuesday"},
                        {'slabel':'W', 'label':"wednesday"},
                        {'slabel':'T', 'label':"thursday"},
                        {'slabel':'F', 'label':"friday"},
                        {'slabel':'S', 'label':"saturday"}
                    ];
                }
            }
        }
    }
}]);

appStrapDirectives.directive('appScheduleTime', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-float-left app-width-full app-padding-five-px' >" +
            "<div class='app-float-left app-font-weight-bold app-text-align-right app-width-one-twenty-px'>Time : </div>" +
            "<div class='app-float-left app-padding-left-twenty-px'  >" +
            '<select style="width: 100px"  ng-model="row[colmetadata.expression].time" >' +
            '<option value="1">1</option>' +
            '<option value="2">2</option>' +
            '<option value="3">3</option>' +
            '<option value="4">4</option>' +
            '<option value="5">5</option>' +
            '<option value="6">6</option>' +
            '<option value="7">7</option>' +
            '<option value="8">8</option>' +
            '<option value="9">9</option>' +
            '<option value="10">10</option>' +
            '<option value="11">11</option>' +
            '<option value="12">12</option>' +
            '<option value="13">13</option>' +
            '<option value="14">14</option>' +
            '<option value="15">15</option>' +
            '<option value="16">16</option>' +
            '<option value="17">17</option>' +
            '<option value="18">18</option>' +
            '<option value="19">19</option>' +
            '<option value="20">20</option>' +
            '<option value="21">21</option>' +
            '<option value="22">22</option>' +
            '<option value="23">23</option>' +
            '<option value="24">24</option>' +
            '</select>' +
            "</div>" +
            "</div>",
        compile:function () {
            return  {
                pre:function ($scope) {
                }
            }
        }
    }
}]);


appStrapDirectives.directive('appScheduleDates', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-float-left app-width-full app-padding-five-px'>" +
            "<div class='app-float-left app-font-weight-bold app-text-align-right app-width-one-twenty-px'>Dates : </div>" +
            "<div class='app-float-left app-padding-left-twenty-px ' style='width: 350px;line-height: 23px;' >" +
            "<div ng-repeat='date in dates'  class='app-display-inline-table app-dates-schedule'>" +
            "<span class='app-float-left'><input style='margin: 0px;' ng-model='row[date.label]' type='checkbox' ></span>" +
            "<span class='app-float-left' style='padding:0px 7px 0px 5px;' ng-bind='date.slabel'></span>" +
            "</div>" +
            "</div>" +
            "</div>",
        compile:function () {
            return  {
                post:function ($scope) {


                    $scope.dates = [
                        {slabel:'1', 'label':'one'},
                        {slabel:'2', 'label':'two'},
                        {slabel:'3', 'label':'three'},
                        {slabel:'4', 'label':'four'},
                        {slabel:'5', 'label':'five'},
                        {slabel:'6', 'label':'six'},
                        {slabel:'7', 'label':'seven'},
                        {slabel:'8', 'label':'eight'},
                        {slabel:'9', 'label':'nine'},
                        {slabel:'10', 'label':'ten'},
                        {slabel:'11', 'label':'eleven'},
                        {slabel:'12', 'label':'twelve'},
                        {slabel:'13', 'label':'thirten'},
                        {slabel:'14', 'label':'fourteen'},
                        {slabel:'15', 'label':'fifteen'},
                        {slabel:'16', 'label':'sixteen'},
                        {slabel:'17', 'label':'seventeen'},
                        {slabel:'18', 'label':'eightteen'},
                        {slabel:'19', 'label':'nineteen'},
                        {slabel:'20', 'label':'twenty'},
                        {slabel:'21', 'label':'twentyone'},
                        {slabel:'22', 'label':'twentytwo'},
                        {slabel:'23', 'label':'twentythree'},
                        {slabel:'24', 'label':'twentyfour'},
                        {slabel:'25', 'label':'twentyfive'},
                        {slabel:'26', 'label':'twentysix'},
                        {slabel:'27', 'label':'twentyseven'},
                        {slabel:'28', 'label':'twentyeight'},
                        {slabel:'29', 'label':'twentynine'},
                        {slabel:'30', 'label':'thirty'},
                        {slabel:'31', 'label':'thirtyone'}
                    ];
                }
            }
        }
    }
}]);


appStrapDirectives.directive('appScheduleMonths', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-float-left app-width-full app-padding-five-px' >" +
            "<div class='app-float-left app-font-weight-bold app-text-align-right app-width-one-twenty-px' >Months : </div>" +
            "<div class='app-float-left app-padding-left-twenty-px' style='width: 280px;line-height: 23px;'>" +
            "<div class='app-float-left' ng-repeat='month in months' style='width:41px;'>" +
            "<span class='app-float-left'><input class='app-margin-top-one-px' ng-model='row[colmetadata.expression][month.label]' type='checkbox' ></span>" +
            "<span class='app-float-left' style='padding:0px 7px 0px 5px;' ng-bind='month.slabel'></span>" +
            "</div>" +
            "</div>" +
            "</div>",
        compile:function () {
            return  {
                pre:function ($scope) {
                    $scope.months = [
                        {'slabel':'J', 'label':"jan"},
                        {'slabel':'F', 'label':"feb"},
                        {'slabel':'M', 'label':"mar"},
                        {'slabel':'A', 'label':"aprl"},
                        {'slabel':'M', 'label':"may"},
                        {'slabel':'J', 'label':"june"},
                        {'slabel':'J', 'label':"july"},
                        {'slabel':'A', 'label':"aug"},
                        {'slabel':'S', 'label':"sep"},
                        {'slabel':'O', 'label':"oct"},
                        {'slabel':'N', 'label':"nov"},
                        {'slabel':'D', 'label':"dec"}
                    ];

                }
            }
        }
    }
}]);


appStrapDirectives.directive('appDateFilter', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='input-append date datepicker app-text-box-border app-zero-border-radius app-zero-padding' style='position: relative;'>" +
            '<span class="app-date-filter-left-arrow app-vertical-align-middle app-cursor-pointer" ></span>' +
            "<input ng-click='showPopUp()' ng-init='value = getColumnValue(row,colmetadata,false)' ng-model='value' placeholder={{colmetadata.label}} data-date-format='dd/mm/yyyy' type='text' class='calender-input date-input' app-date-filter-attr style='height: 100%;left: 21px;padding: 0;position: absolute;right: 26px;'/>" +
            '<span class="app-date-filter-right-arrow app-vertical-align-middle app-cursor-pointer"></span>' +
            '<input type="text" data-toggle="datepicker" class="app-grid-date-picker-calender-image app-position-absolute" tabindex="-1" style="right: 4px;"/>' +
            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement) {
                    var width = $scope.colmetadata.width;
                    var height = $scope.colmetadata.height;

                    if (!height) {
                        height = 24;
                    }

                    if (!width) {
                        width = 200;
                    }
                    iElement.width(width);
                    iElement.height(height);


                }
            }
        }
    }
}]);


appStrapDirectives.directive("appDateFilterAttr", ["$compile", "$appDomService", function ($compile, $appDomService) {

    return {
        restrict:'A',
        replace:true,
        scope:true,
        compile:function () {
            return  {
                post:function ($scope, iElement) {

                    $scope.currentDate = new Date();
                    $scope.currentYear = $scope.currentDate.getFullYear();
                    $scope.currentMonth = $scope.currentDate.getMonth();
                    $scope.months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
                    $scope.selected = {dateSelected:true, monthSelected:false, yearSelected:false, weekSelected:false};


                    $scope.weekFilter = function () {
                        $scope.selected.weekSelected = true;
                        $scope.selected.yearSelected = false;
                        $scope.selected.monthSelected = false;
                        $scope.selected.dateSelected = false;

                        $scope.currentDate = new Date();

                        var first = $scope.currentDate.getDate() - $scope.currentDate.getDay();
                        var last = first + 6;
                        $scope.weekFirstDay = new Date($scope.currentDate);
                        $scope.weekFirstDay.setDate(first)
                        $scope.weekLastDay = new Date($scope.currentDate);
                        $scope.weekLastDay.setDate(last)


                        $scope.firstDay = $scope.weekFirstDay.getFormattedDate(DATE_FORMAT);
                        var nextWeekObj = new Date($scope.weekLastDay.getFullYear(), $scope.weekLastDay.getMonth(), $scope.weekLastDay.getDate() + 1);
                        $scope.lastDay = nextWeekObj.getFormattedDate(DATE_FORMAT);
                        var value = $scope.weekFirstDay.getFormattedDate(DATE_FORMAT) + ' - ' + $scope.weekLastDay.getFormattedDate(DATE_FORMAT);
                        iElement.val(value);
                        $scope.setValue(value);
                    }


                    $scope.setValue = function (val) {
                        $scope.row[$scope.colmetadata.expression] = {};
                        var firstSplit = $scope.firstDay.split("/");
                        $scope.row[$scope.colmetadata.expression][GREATER_EQ] = firstSplit[2] + "-" + firstSplit[1] + "-" + firstSplit[0];
                        var secondSplit = $scope.lastDay.split("/");
                        $scope.row[$scope.colmetadata.expression][LESS_THEN] = secondSplit[2] + "-" + secondSplit[1] + "-" + secondSplit[0];
                        $scope.row[$scope.colmetadata.expression][VAL] = val
                    }


                    $scope.yearFilter = function () {
                        $scope.selected.yearSelected = true;
                        $scope.selected.monthSelected = false;
                        $scope.selected.dateSelected = false;
                        $scope.selected.weekSelected = false;

                        $scope.currentDate = new Date();
                        $scope.currentYear = $scope.currentDate.getFullYear();
                        iElement.val($scope.currentYear);
                        $scope.getFirstAndNextDate($scope.currentYear);
                    }

                    $scope.monthFilter = function () {
                        $scope.selected.monthSelected = true;
                        $scope.selected.dateSelected = false;
                        $scope.selected.yearSelected = false;
                        $scope.selected.weekSelected = false

                        $scope.currentDate = new Date();
                        $scope.currentMonth = $scope.currentDate.getMonth();

                        var currentMonthFirstDay = new Date($scope.currentDate.getFullYear(), $scope.currentMonth, 1);
                        var nextMonthFirstDay = new Date($scope.currentDate.getFullYear(), $scope.currentMonth + 1, 1);
                        $scope.firstDay = currentMonthFirstDay.getFormattedDate(DATE_FORMAT);
                        $scope.lastDay = nextMonthFirstDay.getFormattedDate(DATE_FORMAT);
                        var value = $scope.months[currentMonthFirstDay.getMonth()] + ', ' + $scope.currentYear;
                        iElement.val(value);
                        $scope.setValue(value);
                    }

                    $scope.getFirstAndNextDate = function (val) {
                        if ($scope.selected.dateSelected) {
                            $scope.firstDay = $scope.currentDate.getFormattedDate(DATE_FORMAT);
                            val = $scope.firstDay;
                            var nextDate = new Date($scope.currentDate.getFullYear(), $scope.currentDate.getMonth(), $scope.currentDate.getDate() + 1);
                            $scope.lastDay = nextDate.getFormattedDate(DATE_FORMAT);
                        }
                        if ($scope.selected.yearSelected) {
                            $scope.firstDay = "01/01/" + $scope.currentYear;
                            var next = new Date(($scope.currentYear + 1), $scope.currentMonth, $scope.currentDate.getDate());
                            $scope.lastDay = "01/01/" + next.getFullYear();
                        }
                        if (!$scope.$$phase) {
                            $scope.$apply();
                        }
                        $scope.setValue(val);
                    }


                    $scope.dateFilter = function () {
                        $scope.selected.dateSelected = true;
                        $scope.selected.yearSelected = false;
                        $scope.selected.monthSelected = false;
                        $scope.selected.weekSelected = false;

                        $scope.currentDate = new Date();
                        var value = $scope.currentDate.getFormattedDate(DATE_FORMAT);
                        iElement.val(value);
                        $scope.getFirstAndNextDate(value);
                    }

                    $scope.showPopUp = function () {

                        var html = "<ul>" +
                            "<li class='app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color ng-scope ng-binding app-padding-five-px' ng-click='dateFilter()' >Date</li>" +
                            "<li class='app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color ng-scope ng-binding app-padding-five-px' ng-click='monthFilter()'>Month</li>" +
                            "<li class='app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color ng-scope ng-binding app-padding-five-px' ng-click='yearFilter()' >Year</li>" +
                            "<li class='app-cursor-pointer app-white-space-nowrap app-row-action app-light-gray-backgroud-color ng-scope ng-binding app-padding-five-px' ng-click='weekFilter()' >Week</li>" +
                            "</ul>";

                        var popup = {
                            template:html,
                            scope:$scope.$new(),
                            hideonclick:true,
                            element:iElement.parent(),
                            width:iElement.parent().width()
                        }
                        $appDomService.showPopup(popup);

                    }

                    iElement.next().on('click', function () {
                        $scope.dateFilterNavigation(true);
                    });

                    iElement.prev().on('click', function () {
                        $scope.dateFilterNavigation(false);
                    });


                    $scope.dateFilterNavigation = function (next) {
                        if ($scope.selected.weekSelected) {
                            $scope.currentDate = $scope.weekFirstDay;
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

                            var formattedFirstDay = $scope.weekFirstDay.getFormattedDate(DATE_FORMAT);
                            var formattedLastDay = $scope.weekLastDay.getFormattedDate(DATE_FORMAT);

                            $scope.firstDay = $scope.weekFirstDay.getFormattedDate(DATE_FORMAT);
                            var nextWeekObj = new Date($scope.weekLastDay.getFullYear(), $scope.weekLastDay.getMonth(), $scope.weekLastDay.getDate() + 1);
                            $scope.lastDay = nextWeekObj.getFormattedDate(DATE_FORMAT);
                            var value = formattedFirstDay + ' -- ' + formattedLastDay;
                            iElement.val(value);
                            $scope.setValue(value);
                        } else if ($scope.selected.dateSelected) {
                            if (next) {
                                $scope.currentDate = new Date($scope.currentDate.setDate($scope.currentDate.getDate() + 1));
                            } else {
                                $scope.currentDate = new Date($scope.currentDate.setDate($scope.currentDate.getDate() - 1));
                            }
                            iElement.val($scope.currentDate.getFormattedDate(DATE_FORMAT));
                            $scope.getFirstAndNextDate($scope.currentDate.getFormattedDate(DATE_FORMAT));

                        } else if ($scope.selected.monthSelected) {
                            if (next) {
                                $scope.currentMonth += 1;
                            } else {
                                $scope.currentMonth -= 1;
                            }

                            var currentMonthFirstDay = new Date($scope.currentDate.getFullYear(), $scope.currentMonth, 1);
                            var nextMonthFirstDay = new Date($scope.currentDate.getFullYear(), $scope.currentMonth + 1, 1);

                            $scope.firstDay = currentMonthFirstDay.getFormattedDate(DATE_FORMAT)
                            $scope.lastDay = nextMonthFirstDay.getFormattedDate(DATE_FORMAT);

                            var value = $scope.months[currentMonthFirstDay.getMonth()] + ', ' + $scope.currentYear;
                            iElement.val(value);
                            $scope.setValue(value);
                        } else if ($scope.selected.yearSelected) {

                            if (next) {
                                $scope.currentDate.setFullYear($scope.currentDate.getFullYear() + 1);
                            } else {
                                $scope.currentDate.setFullYear($scope.currentDate.getFullYear() - 1);
                            }
                            $scope.currentYear = $scope.currentDate.getFullYear();
                            iElement.val($scope.currentYear);
                            $scope.getFirstAndNextDate($scope.currentYear);
                        }

                    }

                    var component = iElement.siblings('[data-toggle="datepicker"]');
                    iElement.datepicker({autoclose:true, format:DATE_FORMAT});
                    iElement.on('changeDate', function (e) {
                        $scope.currentDate = new Date(e.date);
                        $scope.getFirstAndNextDate();

                    })

                }
            };
        }
    };
}]);


appStrapDirectives.directive('applaneFreezeGrid', [
    '$compile', '$timeout', '$dataModel',
    function ($compile, $timeout, $dataModel) {
        return {
            restrict:'A',
            replace:true,
            scope:true,
            template:"<table style='width: 100%;'>" +
                "<thead>" +
                "<th applane-grid-header-cell ng-repeat='col in view.metadata.columns'>" +
                "</th><th style='width:17px;'>&nbsp;</th>" +
                "</thead>" +
                "<tbody>" +
                "<tr style='height:25px;'  ng-class-even=\"'applane-grid-row-even'\" ng-class-odd=\"'applane-grid-row-odd'\"  ng-class=\"{'app-selected-tr':row[view.metadata.primarycolumn] == currentRow[view.metadata.primarycolumn]}\" ng-repeat='row in view.data.data'>" +
                "<td applane-grid-cell class='applane-grid-cell' ng-repeat='col in view.metadata.columns' tabindex=0>" +
                "</td>" +
                "</tr>" +
                "</tbody>" +
                "</table>",
            link:function (scope, element, attrs) {
                scope.editMode = false;
                scope.__selectall__ = false;
                scope.setEditMode = function (editMode) {
                    scope.editMode = editMode;

                };
                scope.setCurrentRow = function (row) {
                    scope.currentRow = row;
                    $dataModel.setCurrentRow(scope[COMPONENT_ID], row);
                }
            }
        };
    }
]);


appStrapDirectives.directive('applaneGrid', [
    '$compile', '$timeout', '$dataModel',
    function ($compile, $timeout, $dataModel) {
        return {
            restrict:'A',
            replace:true,
            scope:true,
            template:"<div style='display: table;table-layout:fixed;width: 100%;height: 100%;'>" +
                "<div style='overflow: hidden;display: table-row;'><div style='position: relative;width: 100%;'>" +
                "<div style='overflow-x: hidden;left: 0px;right: 0px;' id='{{componentid}}_head' applane-grid-header></div>" +
                "</div>" +
                "</div>" +
                "<div style='display: table-row;height: 100%;'>" +
                "<div style='position: relative;height: 100%;'>" +
                "<div style='position: absolute;top:0px;left:0px;right:0px;bottom:0px;overflow-X: auto;overflow-y: scroll;' applane-grid-body id='{{componentid}}_body'></div>" +
                "</div></div></div>",
            link:function (scope, element, attrs) {
                scope.editMode = false;
                scope.__selectall__ = false;

                scope.setEditMode = function (editMode) {
                    scope.editMode = editMode;
                };
                scope.setCurrentRow = function (row) {
                    scope.currentRow = row;
                    $dataModel.setCurrentRow(scope[COMPONENT_ID], row);
                }

                var componentId = scope.componentid;
                $timeout(function () {
                    var tableBodyId = "#" + componentId + "_body";
                    var tableHeaderId = "#" + componentId + "_head";
                    $(tableBodyId).scroll(function () {
                        $(tableHeaderId).scrollLeft($(tableBodyId).scrollLeft());
                    });
                }, 0);
            }
        };
    }
]);

appStrapDirectives.directive('applaneGridBody', [
    '$compile',
    function ($compile) {
        return {
            restrict:'A',
            scope:false,
            template:"<table style='table-layout: fixed;width: 100%;' cellpadding='0' cellspacing='0'  class='applane-grid-body'><tbody>" +
                "<tr style='height:25px;'  ng-class-even=\"'applane-grid-row-even'\" ng-class-odd=\"'applane-grid-row-odd'\"  ng-class=\"{'app-selected-tr':row[view.metadata.primarycolumn] == currentRow[view.metadata.primarycolumn]}\" ng-repeat='row in view.data.data'>" +
                "<td applane-grid-cell class='applane-grid-cell' ng-repeat='col in view.metadata.columns' tabindex=0>" +
                "</td>" +
                "</tr>" +

                "<tr ng-show='view.data.moduleresult.aggregates'  class='app-border-top-black app-border-bottom-black'>" +
                "<td style='border:none;' class='applane-grid-cell' ng-repeat='col in view.metadata.columns' ng-bind-html-unsafe='getColumnValue(view.data.moduleresult.aggregates,col,false)'>" +
                "</tr>" +

                "</tbody></table>"
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
                "<th applane-grid-header-cell ng-repeat='col in view.metadata.columns'></th>" +
                "<th style='width:17px;'>&nbsp;</th>" +
                "</thead></table>"
        };
    }
]);

appStrapDirectives.directive('applaneGridCell', [
    '$compile', '$timeout', '$dataModel', '$appDomService',
    function ($compile, $timeout, $dataModel, $appDomService) {
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
                                        var recordKey = record[$scope.view.metadata[PRIMARY_COLUMN]];
                                        var isEqual = angular.equals(recordKey, $scope.row[$scope.view.metadata[PRIMARY_COLUMN]]);
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
                                $scope.setCurrentRow($scope.row);
                                if (!$scope.$$phase) {
                                    $scope.$apply();
                                }
                            });
                            $scope.bindKeyDownOnTdElement();
                        };


                        $scope.showEditorCellIfAny = function ($event) {        // Add Listener on td element to show editor if any defined

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


                                $appDomService.showPopup(popup);
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
                            var rows = $scope.view.data.data;
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

/*************************************Controller********************************************************************/
var app = angular.module('applane', ['$appstrap.directives', '$appstrap.services'], function ($routeProvider, $locationProvider) {

});


app.controller('AppCtrl', function ($scope, $compile, $http, $location, $dataModel, $appService, $appDomService, $rootScope, $timeout) {
    $rootScope.busymessage = "...";
    $rootScope.showbusymessage = false;
    $scope.views = [];
    $scope.loggedin = false;

    $scope.setStartTime = function () {
        $scope.startDateTime = new Date().getTime();
    }

    $scope.getStartTime = function () {
        return $scope.startDateTime;
    }

    $scope.getTotalTime = function () {
        return (new Date().getTime() - $scope.startDateTime);
    }

    $scope.log = function (log, clear) {
        if (clear) {
            $("#log").val(log);
        } else {
            $("#log").val($("#log").val() + " - " + log);
        }

    }

    $scope.organizationcollection = {"label":"Organizations", "display":["label"], "callback":function (option) {
        $scope.getUserState({"organization":option.id});
    }};
    $scope.viewgroupcollection = {"label":"View groups", "display":["label"], "settingimage":"images/setting.png", "callback":function (option) {
        var state = {"viewgroup":option.label};
        if ($scope.currentorganization) {
            state.organization = $scope.currentorganization.id
        }
        $scope.getUserState(state);
    }};

    $scope.getUserState = function (state) {
        $appService.getUser(state, function (userinfo) {
            var login = userinfo.login;
            if (login == false) {
                window.location.href = "/login.html";
                return;
            } else {
                var username = userinfo.username;

                $scope.username = username;
                $scope.loggedin = true;

                var menus = userinfo.menus;
                if (!userinfo.menus) {
                    menus = [];
                }
                if (userinfo.menuindex !== undefined) {
                    $scope.currentmenu = {};
                    $scope.currentmenu.label = userinfo.menus[userinfo.menuindex].label;
                }
                var menuCount = menus.length;
                var developer = true;
                if (menuCount > 0) {
                    developer = menus[0].applicationid.developer;
                }
                $scope.developer = developer;

                $scope.viewgroupcollection.options = userinfo.viewgroups;
                var baasApplication = false;
                if (userinfo.viewgroups && userinfo.viewgroups.length > 0) {
                    $scope.viewgroupcollection.label = userinfo.viewgroups[userinfo.viewgroupindex].label;
                    if (userinfo.viewgroups[userinfo.viewgroupindex]._id == '5205dd8f0c24531c65000004') {
                        baasApplication = true;
                    }
                }


                var viewGroupData = [];
                var viewGroupcount = userinfo.viewgroups.length;
                for (var i = 0; i < viewGroupcount; i++) {
                    viewGroupData.push({"_id":userinfo.viewgroups[i][KEY], "label":userinfo.viewgroups[i].label});
                }


                var userGroupSettingView = {"viewid":"viewgroups__appsstudio", "ask":"appsstudio", "filter":{"_creator_":"{_CurrentUserId}"}};
                $scope.viewgroupcollection.settingview = userGroupSettingView
                if (developer || baasApplication) {
                    $scope.viewgroupcollection.showsettingimage = true;
                } else {
                    $scope.viewgroupcollection.showsettingimage = false;

                }


                $scope.organizationcollection.label = userinfo.organizations[userinfo.organizationindex].label;
                $scope.organizationcollection.options = userinfo.organizations;
                $scope.currentorganization = userinfo.organizations[userinfo.organizationindex];


                var menusData = [];
                for (var i = 0; i < menuCount; i++) {
                    menusData.push({"menus.label":menus[i].label, "menus.filter":menus[i].filter, "menus.applicationid":menus[i].applicationid, "menus.table":menus[i].table, "menus._id":menus[i]._id});
                }


                var settingMenu = {"imageurl":"images/setting.png", "view":{"data":{"data":[]}, "metadata":{refreshonload:true, "label":"Menus", "primarycolumn":"menus._id", "filter":{"_id":"{viewgroupid}"}, "parameters":{"viewgroupid":userinfo.viewgroups[userinfo.viewgroupindex]._id}, "enablemetadataaction":false, "columns":[
                    {"expression":"menus.label", "type":"text", "width":100, "visibility":"Both", "label":"Label"},
                    {"expression":"menus.applicationid", "type":"lookup", "width":150, "visibility":"Both", "label":"Application", "table":"applications__baas", "primarycolumns":[
                        {"expression":"id", "type":"string"}
                    ]},
                    {"expression":"menus.table", "filter":{"applications":"{menus.applicationid}"}, "type":"lookup", "width":150, "visibility":"Both", "label":"Table", "table":"tables__baas", "primarycolumns":[
                        {"expression":"id", "type":"string"}
                    ]},
                    {"expression":"menus.filter", "type":"object", "width":100, "visibility":"Both", "label":"Filter"}
                ], "type":"table", "table":"viewgroups__appsstudio", insertview:"table"}, "applicationid":"applanedeveloper", "ask":"appsstudio"}};

                if (developer) {
                    menus.push(settingMenu);
                }

                $scope.menus = menus;
                if (userinfo.view) {
                    $appDomService.addView(userinfo.view, $scope);
                } else {
                    if (!$scope.$$phase) {
                        $scope.$apply();
                    }
                }
            }
        });
    }

    $scope.getUserState({});

    $scope.logOut = function () {
        $appService.logOut(function (data) {
            //destroy usk here
            window.location.href = "/login.html";
            return;

        })
    }

    $scope.onQuickViewSelection = function (quickView) {

        $appDomService.addView({"viewid":quickView.viewid.viewid, "applicationid":quickView.applicationid.id, "ask":quickView.applicationid.oauthkey, "osk":$scope.currentorganization.oauthkey}, $scope);

    }

    $scope.getColumnValue = function (row, col, asHTML) {
        var val;
        if (asHTML) {
            val = "&nbsp;";
        } else {
            val = "";
        }

        var exp = col.expression;

        if (row && exp && row[exp]) {
            var type = col.type;
            var multiple = col.multiple;
            if (type == 'lookup') {

                if (multiple) {
                    val = row[exp];
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
                    var colValue = row[exp];
                    if (colValue && colValue._val && colValue._val.length > 0) {
                        val = colValue._val;
                    }
                }
            } else if (type == 'file') {

                if (row[exp].length > 0) {
                    var url = BAAS_SERVER + '/file/download?filekey=' + row[exp][0][FILE_KEY] + '&ask=frontend';
                    val = "<a tabindex='-1' href='" + url + "'>" + row[exp][0][FILE_NAME] + "</a>";
                }


            } else if (type == 'datefilter') {
                val = row[exp][VAL];

            } else if (type == 'string' || type == 'text' || type == 'boolean' || type == 'number' || type == 'decimal') {

                val = row[exp];
                if (multiple && val instanceof Array) {
                    val = JSON.stringify(val);
                }
            } else if (type == 'object') {
                val = row[exp];
                if (val instanceof Object) {
                    val = JSON.stringify(val);
                }
            } else if (type == 'duration') {
                var timeExp = col.timemetadata.expression;
                var timeUnitExp = col.timeunitmetadata.expression;

                var timeValue;
                if (row[timeExp]) {
                    timeValue = row[timeExp];
                }
                var timeUnitValue;
                if (row[timeUnitExp]) {
                    timeUnitValue = row[timeUnitExp]._val;
                }

                if (timeValue && timeUnitValue) {
                    val = timeValue + " " + timeUnitValue;
                } else {
                    val = "&nbsp;"
                }
            } else if (type == CURRENCY_TYPE) {

                var amountExp = col.amountmetadata.expression;
                var typeExp = col.typemetadata.expression;

                var amountValue;
                if (row[amountExp]) {
                    amountValue = row[amountExp];
                }
                var typeValue;
                if (row[typeExp]) {
                    typeValue = row[typeExp]._val;
                }

                if (amountValue && typeValue) {
                    val = amountValue + " " + typeValue;
                } else {
                    val = "&nbsp;"
                }
            } else {
                val = col.type + "-" + JSON.stringify(row[col.expression])
            }
        }

        return val;
    }
});
/*************************************END Controller********************************************************************/





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

            '<div style="width:{{columnGroup.cellWidth}}%;padding-top:10px;min-width:{{columnGroup.columnWidth + 7}}px;"' + ' class="app-float-left cell-width">' +

            '<div title="{{column.label}}" ng-show="columnGroup.showcolumnlabel"  ' +
            'class="app-float-left app-font-weight-bold {{alignclass}} app-overflow-hiiden app-white-space-nowrap"  style="font-size:13px;width:{{columnLabelStyle}}px;padding-left:7px;">' +
            '{{column.label}}:</div>' +

            '<div style="width:{{columnGroup.columnWidth}}px;height:{{columnGroup.height}}px;padding-left:7px;" class="app-float-left">' +
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


appStrapDirectives.directive("appPanel", ["$compile", "$dataModel", function ($compile, $dataModel) {
    return {
        restrict:"E",
        replace:true,
        template:"<div class='app-panel'>" +
            '<div class="app-container">' +
            '<div class="app-wrapper">' +
            '<div class="app-wrapper-child app-overflow-auto">' +
            '<div class="app-tool-bar" app-tool-bar></div>' + // Tool Bar for panel view
            '<app-column-group ng-repeat="columnGroup in view.metadata.newcolumnGroups"></app-column-group>' +
            '<app-bar ng-init="actions=view.metadata.footeractions" ></app-bar>' +
            '</div>' +
            '</div>' +
            '</div>' +
            "</div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {

                    var metadata = $scope.view.metadata;
                    var columnGroups = metadata.columnGroups;


                    if (!columnGroups) {
                        columnGroups = {
                            "onenlabel":{"showcolumnlabel":false, "title":"", "showtitle":false, "columnPerRow":1, "columnWidth":200, "labelWidth":150, "placeholder":"true", "height":30 },
                            "richtext":{"showcolumnlabel":true, "title":"", "showtitle":false, "columnPerRow":1, "columnWidth":621, "labelWidth":150, "placeholder":"true", "height":260 },
                            "twowlabel":{"showcolumnlabel":true, "title":"", "showtitle":false, "columnPerRow":2, "columnWidth":200, "labelWidth":150, "placeholder":"true", "height":30 }
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
                        if (columnType == 'lookup') {
                            var multiple = column.multiple;
                            if (multiple) {
                                column.editableCellTemplate = "<bs-multi-refrence-typeahead></bs-multi-refrence-typeahead>";
                            } else {
                                column.editableCellTemplate = "<bs-typeahead></bs-typeahead>";
                            }
                        } else if (columnType === 'duration') {
                            column.editableCellTemplate = "<app-duration></app-duration>";
                        } else if (columnType == CURRENCY_TYPE) {
                            column.editableCellTemplate = "<app-currency></app-currency>";
                        } else if (columnType === 'date' || columnType == 'datetime') {
                            column.editableCellTemplate = '<div class="app-grid-datepicker-parent">' +
                                '<input type="text" ng-init="colmetadata=colmetadata;"  ng-model="row[colmetadata.expression]" placeholder="{{colmetadata.label}}" data-date-format="dd/mm/yyyy" app-datepicker class="app-grid-date-picker-input"/>' +
                                '<input type="text" data-toggle="datepicker" class="app-grid-date-picker-calender-image" />' +
                                '</div>';


                        } else if (columnType === 'textarea') {
                            column.editableCellTemplate = '<textarea></textarea>';
                        } else if (columnType === 'boolean') {
                            column.editableCellTemplate = "<app-checkbox class='app-panel-input'  ></app-checkbox>";
                        } else if (columnType == 'view') {
                            column.editableCellTemplate = '<div class="app-width-full"><app-nested-view ></app-nested-view></div>';

                        } else if (columnType == 'file') {
                            column.editableCellTemplate = '<app-file-upload class="app-float-left"></app-file-upload>';
                        } else if (columnType == 'schedule') {
                            column.editableCellTemplate = "<div class='app-panel-schedule-pop-up app-float-left'><app-schedule-pop-up></app-schedule-pop-up><div>";
                        } else if (columnType == 'text') {
                            column.editableCellTemplate = "<app-text-area></app-text-area>";
                        } else if (columnType == 'richtext') {
                            column.editableCellTemplate = "<app-rich-text-area></app-rich-text-area>";
                        } else {
                            column.editableCellTemplate = "<app-text class='app-panel-input app-float-left' ></app-text>";
                        }

                        if (columnType == 'view') {
                            column.columnGroup = 'onenlabel';
                        } else if (columnType == 'richtext' || columnType == 'text') {
                            column.columnGroup = 'richtext';
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
                        column.height = lastcolumnGroup.height;
                        lastColumns.push(column);
                    }

                    metadata.newcolumnGroups = newcolumnGroups;

                    var dataRecords = $scope.view.data.data
                    var dataRecordCount = dataRecords.length;
                    if (dataRecordCount == 0) {
                        $dataModel.insert($scope[COMPONENT_ID]);
                    }

                    $scope.row = $scope.view.data.data[0];

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
                    var textareaTemplate = '<textarea name="textarea" ng-bind="row[colmetadata.expression]"></textarea>' +
                        '<div class="jqte">' +
                        '<div class="jqte_toolbar"  role="toolbar" unselectable></div><div class="jqte_linkform" style="display:none" role="dialog">' +
                        '<div class="jqte_linktypeselect" unselectable>' +
                        '<div class="jqte_linktypeview" unselectable></div><div class="jqte_linktypes" role="menu" unselectable></div>' +
                        '</div><input class="jqte_linkinput" type="text/css" value=""><div class="jqte_linkbutton" unselectable>OK</div> <div style="height:1px;float:none;clear:both"></div>' +
                        '</div><div class="jqte_editor" ng-bind-html-unsafe ="row[colmetadata.expression]"></div>' +
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

                                    $scope.row[$scope.colmetadata.expression] = thisElement.val();
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
                                    $scope.row[$scope.colmetadata.expression] = thisElement.val();
                                    if (!$scope.$$phase) {
                                        $scope.$apply();
                                    }
                                });
                        });
                    };


                    $scope.richTextBoxEditor();

//                    if ($scope[attrs.model][attrs.field]) {
//                        $scope.richTextBoxEditorValue($scope[attrs.model][attrs.field]);
//                    }
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

                    var textareaTemplate = '<textarea class="app-auto-resize-teztarea" ng-model="row[colmetadata.expression]"></textarea>' +
                        '<div ng-bind="row[colmetadata.expression]" class="app-auto-resize-div"></div>';

                    $(iElement).append($compile(textareaTemplate)($scope));

                    var textAreaElement = angular.element(iElement).find('textarea');
                    var $clone = angular.element(iElement).find('div');
                    $clone = $($clone);
                    if ($scope.colmetadata.width) {
                        $scope.colmetadata.width = 200;
                    }
                    $clone.width($scope.colmetadata.width);


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

                    if ($scope.row[$scope.colmetadata.expression]) {
                        textAreaElement.val($scope.row[$scope.colmetadata.expression]);
                        updateHeight();
                    }

                }
            }
        }
    }
}]);


/***************************End of auto resize text area**************************************/