/**
 *
 * handling of selected keys
 * zero width : editor issue
 */
var CELL_TEMPLATE = "<span ng-bind-html-unsafe='getColumnValue(row,col)'></span>";
var appStrapDirectives = angular.module('$appstrap.directives', []);
var ID_COLUMN = "_id";
var ACTION_METHOD = /ACTION_METHOD/g;
var ACTION_LABEL = /ACTION_LABEL/g;
var ACTION_CLASS = /ACTION_CLASS/g;
var DEFAULT_ACTION_TEMPLATE = "<div class='ACTION_CLASS' title='ACTION_LABEL' ng-click='ACTION_METHOD'></div>";
var CURRENCY_TYPE = 'currency';

appStrapDirectives.factory('$appUtil', ['$timeout', '$rootScope',
    function ($timeout, $rootScope) {
        var $appUtil = {

        };
        $appUtil.rebindFieldExpression = function ($scope, model, field) {
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

        $appUtil.removeDottedValue = function (model, expression) {
            if (!model) {
                return;
            }
            var firstDottedIndex = expression.indexOf(".");
            if (firstDottedIndex >= 0) {
                expression = expression.substring(0, firstDottedIndex);
            }
            delete model[expression];

        }
        $appUtil.putDottedValue = function (model, expression, value) {
            if (!model) {
                alert("Model does not exits for putting dotted value")
                return;
            }
            var lastDottedIndex = expression.lastIndexOf(".");
            if (lastDottedIndex >= 0) {
                var firstExpression = expression.substring(0, lastDottedIndex);
                expression = expression.substring(lastDottedIndex + 1);
                model = $appUtil.resolveDot(model, firstExpression, true);
            }
            model[expression] = value;

        }
        $appUtil.resolveDot = function (model, expression, confirm, confirmType) {
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

        $appUtil.getModel = function ($scope, modelExpression, confirm, confirmType) {
            return this.resolveDot($scope, modelExpression, confirm, confirmType);
        }
        $appUtil.getDataFromService = function (url, requestBody, callType, dataType, pBusyMessage, callback, errcallback) {

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

        return $appUtil;

    }
])

appStrapDirectives.directive("appGrid", ["$compile", "$appUtil", "$timeout", function ($compile, $appUtil, $timeout) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-grid'></div>",
        compile:function () {
            return {
                pre:function ($scope, iElement, attrs) {
                    $scope.getColumnValue = function (row, col, groupCol) {
                        var val = "";
                        var exp;
                        var colValue;
                        if (col) {
                            exp = col.expression;
                            colValue = $appUtil.resolveDot(row, exp, false)
                        } else {
                            colValue = row;
                        }

                        if (!col || !col.type) {
                            if (col && row._group_ !== undefined && !groupCol) {
                                return "";
                            }
                            if (colValue instanceof Array) {
                                var arrayCount = colValue.length;
                                for (var i = 0; i < arrayCount; i++) {
                                    val += $scope.getColumnValue(colValue[i]) + ";";

                                }
                            } else if (colValue instanceof Object) {
                                Object.keys(colValue).forEach(function (k) {
                                    if (k != $scope.gridOptions.idColumn) {
                                        val += $scope.getColumnValue(colValue[k]) + " ";
                                    }
                                });

                            } else if (!col) {
                                return colValue === undefined ? "" : colValue
                            } else if (!col.type) {
                                return colValue === undefined ? "&nbsp;" : colValue
                            }
                        } else if (row && exp && colValue !== undefined) {

                            var type = col.type;
                            var multiple = col.multiple;

                            if (row._group_ !== undefined && type != 'mode' && !groupCol) {
                                return "";
                            }
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
                                                    valTemp += '; ';
                                                }
                                            }
                                            val = valTemp;
                                        }
                                    }
                                } else {
                                    val = colValue;
                                    if (angular.isObject(val)) {
                                        val = val[col.primarycolumns[0].expression];
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
                            } else if (type == 'mode') {


                                var groupColumns = $scope.gridOptions.appliedGroups[row._group_].columns;
                                val += " ";
                                for (var i = 0; i < groupColumns.length; i++) {
                                    var groupCol = groupColumns[i];
                                    val += groupCol.expression + ' : ';
                                    val += $scope.getColumnValue(row, groupCol, true);
                                    if (i != (groupColumns.length - 1)) {
                                        val += ', ';
                                    }
                                }


                            } else {
                                val = col.type + "-" + JSON.stringify(colValue)
                            }


                        }
                        if (val === "") {
                            val = "&nbsp;"
                        }
                        return val;
                    }

                    $scope.editMode = false;
                    $scope.__selectall__ = false;

                    $scope.setEditMode = function (editMode) {
                        $scope.editMode = editMode;
                    };
                    $scope.setCurrentRow = function (row) {
                        $scope.currentRow = row;
//                    $dataModel.setCurrentRow(scope[COMPONENT_ID], row);
                    }
                    $scope.toggleGroup = function (row, col, index) {

                        var groupStage = (row[col.expression]);
                        var _display_ = false;
                        if (groupStage == 0) {
                            _display_ = !_display_;
                            row[col.expression] = 1
                        } else {
                            row[col.expression] = 0;
                        }
                        var parentGroupIndex = row._group_;
                        var data = $appUtil.resolveDot($scope, $scope.gridOptions.data);
                        for (var i = row._id + 1; i < data.length; i++) {
                            var obj = data[i];
                            if (obj.parentgroup == parentGroupIndex) {
                                obj._display_ = _display_;
                            } else {
                                break;
                            }
                        }
                    }
                },
                post:function ($scope, iElement, attrs) {
                    try {

                        $scope.componentid = $scope.gridOptions.id;
                        var componentId = $scope.componentid;
                        var columns = $scope.gridOptions.columns;
                        var idColumn = $scope.gridOptions.idColumn;
                        if (!idColumn) {
                            idColumn = ID_COLUMN;
                        }
                        if (!columns) {
                            //We will try to fetch column from first row
                            columns = [];
                            var data = $appUtil.resolveDot($scope, $scope.gridOptions.data);
                            var dataCount = data ? data.length : 0;
                            if (dataCount > 0) {
                                Object.keys(data[0]).forEach(function (k) {
                                    if (k != idColumn) {
                                        columns.push({expression:k, label:k});
                                    }

                                })
                            }
                            if (columns.length == 0) {
                                throw ("No columns found in grid[" + $scope.componentid + "]");
                            }
                            $scope.gridOptions.columns = columns;

                        }

                        var newColumns = [];
                        if ($scope.gridOptions.selectable) {
                            //push a selection column
                            var selectionColumn = {editable:false, "width":20, cellTemplate:"<input style='margin-left: 3px;' tabindex='-1' type='checkbox' ng-model='row.__selected__' />", "type":"selection", "tabindex":-1};
                            if ($scope.gridOptions.selectionTemplate) {
                                selectionColumn.cellTemplate = $scope.gridOptions.selectionTemplate;
                            }
                            newColumns.push(selectionColumn);
                        }
                        //check for label
                        var columCount = columns.length;

                        var hasZeroWidthColumn;
                        for (var i = 0; i < columCount; i++) {
                            var column = columns[i];
                            if (!column.width) {
                                hasZeroWidthColumn = true;
                                column.minWidth = 200;
                            }
                            if (column.label === undefined && column.expression) {
                                column.label = column.expression
                            }
                            if (!column.id) {
                                column.id = componentId + "_" + i;
                            }
                            if (!column.style) {
                                column.style = {};
                            }
                            if (column.editable && !column.editableCellTemplate) {
                                if (!column.type) {
                                    column.editableCellTemplate = "<app-text model='row' field='" + column.expression + "' border=false></app-text>";
                                } else {
                                    if (type == 'lookup') {
                                        var field = column.expression;
                                        var lookupSource = $scope[COMPONENT_ID] + "_" + i
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

//                                        if ($dataModel.isFilterApplied($scope.componentid, expression)) {
//                                            appliedfilters.push(filter);
//                                        }
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

//                                        if ($dataModel.isFilterApplied($scope.componentid, expression)) {
//                                            appliedfilters.push(filter);
//                                        }
                                        filters.push(filter);

                                    } else if (type == 'index') {
                                        column.cellTemplate = "<span ng-show='$parent.$parent.$first==false' ng-click='onUpClick(row)' style='cursor: pointer;float: left;text-align: center;width: 20px'><img width='15px' height='15px' src='images/up-arrow.png'></span><span ng-show='$parent.$parent.$last==false' ng-click='onDownClick(row)' style='cursor: pointer;float: left;text-align: center;width: 20px'><img width='15px' height='15px' src='images/down_arrow_new.png'></span>";
                                        column.editableCellTemplate = "<app-text model='row' field='" + column.expression + ".index' border=false></app-text>";
                                    } else if (type == 'mode') {
                                        column.cellTemplate = "<span style='cursor: pointer;float: left;text-align: center;width: 20px'><img width='15px' height='15px' src='images/up-arrow.png'></span>";
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
                                }
                            }
                            newColumns.push(column);

                        }
                        var zeroWidthColumn = {editable:false, "cellTemplate":"", "tabindex":-1, style:{}};
                        if (hasZeroWidthColumn) {
                            zeroWidthColumn.style.width = "0px";
                            zeroWidthColumn.style.border = "none";
                        }
                        newColumns.push(zeroWidthColumn);
                        $scope.columns = newColumns;
                        var template;
                        if ($scope.gridOptions.scrollable) {
                            template = "<div class='app-container'><div class='app-wrapper'><div class='app-wrapper-child'><div style='display: table;table-layout:fixed;width: 100%;height: 100%;'>" +
                                "<div style='overflow: hidden;display: table-row;'><div style='position: relative;width: 100%;'>" +
                                "<div style='overflow-x: hidden;left: 0px;right: 0px;' id='{{componentid}}_head' applane-grid-header></div>" +
                                "</div>" +
                                "</div>" +
                                "<div style='display: table-row;height: 100%;'>" +
                                "<div style='position: relative;height: 100%;'>" +
                                "<div style='position: absolute;top:0px;left:0px;right:0px;bottom:0px;' class='grid-scroll' applane-grid-body id='{{componentid}}_body'></div>" +
                                "</div></div></div></div></div></div>";

                        } else {
                            template = "<table style='width: 100%;'>" +
                                "<thead>" +
                                "<th applane-grid-header-cell ng-repeat='col in columns '></th>" +
                                "</thead>" +
                                "<tbody>" +
                                "<tr style='height:25px;'  ng-class-even=\"'applane-grid-row-even'\" ng-class-odd=\"'applane-grid-row-odd'\"  ng-repeat='row in " + $scope.gridOptions.data + "'>" +
                                "<td applane-grid-cell class='applane-grid-cell' ng-repeat=' col in columns ' tabindex=0>" +
                                "</td>" +
                                "</tr>" +
                                "</tbody>" +
                                "</table>";

                        }
                        var toolBarTemplate = "<div class='app-tool-bar'></div>";
                        var toolBarElement = $compile(toolBarTemplate)($scope);
                        iElement.append(toolBarElement);
                        var element = $compile(template)($scope);
                        iElement.append(element);
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
                    } catch (e) {
                        alert(e);
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
//                        var selectedRowRule = "\"{'app-selected-tr':row." + $scope.view.metadata.primarycolumn + "==currentRow." + $scope.view.metadata.primarycolumn + "}\"";
                        var selectedRowRule = "";
                        var template = "<table style='table-layout: fixed;width: 100%;' cellpadding='0' cellspacing='0'  class='applane-grid-body'><tbody>" +
                            "<tr  ng-show='row._display_' style='height:25px;'  ng-class-even=\"'applane-grid-row-even'\" ng-class-odd=\"'applane-grid-row-odd'\"  ng-repeat='row in " + $scope.gridOptions.data + "'>" +
                            "<td applane-grid-cell class='applane-grid-cell' ng-repeat='col in columns' tabindex=0 ng-style='col.style'>" +
                            "</td>" +
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
    function ($compile, $timeout) {
        return {
            restrict:'A',
            replace:true,
            compile:function () {
                return {
                    post:function ($scope, iElement, iAttr) {
                        var template = "<table style='table-layout: fixed;width: 100%;' class='applane-grid-header' cellpadding='0' cellspacing='0' ><thead>" +
                            "<th applane-grid-header-cell ng-repeat='col in columns' ng-style='col.style'></th>";
                        if ($scope.gridOptions.scrollable) {
                            template += "<th style='width:17px;border:none;'>&nbsp;</th>";
                        }
                        template += "</thead></table>";
                        $(iElement).append($compile(template)($scope));
                    }
                }
            }
        };
    }
]);

appStrapDirectives.directive('applaneGridCell', [
    '$compile', '$timeout',
    function ($compile, $timeout) {
        return {
            restrict:'A',
            replace:true,
            compile:function () {
                return  {
                    pre:function ($scope, iElement, iAttr) {
                        if ($scope.row.style) {
                            Object.keys($scope.row.style).forEach(function (k) {
                                $(iElement).css(k, $scope.row.style[k]);
                            })
                        }
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

                            var cellTemplate;
                            if ($scope.col.overflow) {

                                if ($scope.row[$scope.col.expression] !== undefined) {
                                    cellTemplate = "<div class='app-white-space-nowrap'>" +
                                        "<span ng-click='toggleGroup(row,col)' style='width: 13px;padding-left:5px;' class='app-text-align-center app-float-left'>" +
                                        "<img class='app-cursor-pointer' ng-class=\"{'app-right-arrow-transform':row[col.expression] == 0 , 'app-right-down-arrow-transform':row[col.expression] == 1}\"  width='12px' height='12px' src='images/up-arrow.png'>" +
                                        "</span>" +
                                        "<span style = 'margin-left: 6px;'  ng-bind-html-unsafe='getColumnValue(row,col)' class='app-text-transform-capitilize app-font-weight-bold app-white-space-nowrap'></span>" +
                                        "</div>";

                                } else {
                                    cellTemplate = "<span style = 'white-space: nowrap;margin-left: 6px;'  ng-bind-html-unsafe='getColumnValue(row,col)'></span>";
                                }

                            } else {
                                cellTemplate = "<div style='white-space: nowrap;overflow: hidden;";
                                cellTemplate += "' ng-dblclick='editCell($event)' ";
                                if ($scope.col.style && $scope.col.style.width == 0) {
                                    cellTemplate += "class='applane-grid-cell-inner'"
                                }
                                cellTemplate += ">" + columnCellTemplate + "</div>";
                            }


                            var cell = $compile(cellTemplate)($scope);
                            var width = $scope.col.width;

                            if (width) {
                                $(cell).width(width);
                                $(iElement).width(width);
                            }
                            $(iElement).html("");
                            iElement.append(cell);
//                            if ($scope.col.type == 'selection') {
//                                $scope.$watch('row.__selected__', function (newValue, oldValue) {
//                                    var selectedKeys = $scope.view.selectedkeys;
//                                    var selectedRowCount = selectedKeys.length;
//                                    var index = -1;
//                                    for (var i = 0; i < selectedRowCount; i++) {
//                                        var record = selectedKeys[i];
//                                        var recordKey = record[$scope.view.metadata[PRIMARY_COLUMN]];
//                                        var isEqual = angular.equals(recordKey, $scope.row[$scope.view.metadata[PRIMARY_COLUMN]]);
//                                        if (isEqual) {
//                                            index = i;
//                                            break;
//                                        }
//                                    }
//                                    if (index == -1 && newValue) {
//                                        selectedKeys.push($scope.row);
//                                    } else if (index > -1 && !newValue) {
//                                        selectedKeys.splice(i, 1);
//                                    }
//                                    if (!newValue) {
//                                        $scope.gridOptions.__selectall__ = false;
//                                    }
//                                });
//                            }

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

//                                var currentRow = $dataModel.getCurrentRow($scope[COMPONENT_ID]);

                                $scope.setCurrentRow($scope.row);

//                                if ($scope.view[CHILD_COMPONENT_ID]) {
//                                    if ($scope.view.metadata.editMode === false && $scope.view.metadata.lastRowAction) {
//                                        //assume click as row action click if child view is open
//                                        if ((!angular.equals(currentRow, $scope.row))) {
//                                            $scope.onRowActionClick($scope.view.metadata.lastRowAction)
//                                        }
//                                        return;
//                                    }
//                                } else {
//                                    $scope.view.metadata.editMode = true;
//                                    $scope.view.metadata.lastRowAction = undefined;
//                                }

                                if (!$scope.$$phase) {
                                    $scope.$apply();
                                }


                            });
                            $scope.bindKeyDownOnTdElement();
                        };


                        $scope.showEditorCellIfAny = function ($event) {        // Add Listener on td element to show editor if any defined

//                            if ($scope.view.metadata.editMode === false) {
//                                return;
//                            }
                            if (!$scope.col.editable) {
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


                                var popupScope = $scope.$new();
                                var p = new Popup({
                                    autoHide:true,
                                    deffered:false,
                                    html:$compile(editableCellTemplate)(popupScope),
                                    scope:popupScope,
                                    element:iElement,
                                    width:$scope.col.width,
                                    position:'onchild',
                                    escEnabled:true,
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
                                });
                                p.showPopup();

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
    '$compile', '$appUtil',
    function ($compile, $appUtil) {
        return {
            restrict:'A',
            replace:true,
            compile:function () {
                return  {
                    pre:function ($scope, iElement, iAttr) {

                        var cellTemplate = '<div >{{col.label}}';
                        if ($scope.col.order) {
                            cellTemplate += '<img style="margin-left: 10px;" width="11px" height="11px" ng-src="images/{{col.order}}-arrow.png" ng-show="col.order"/>';
                        }
                        cellTemplate += '</div>';

                        if ($scope.col.type == 'selection') {
                            cellTemplate = "<div><input type='checkbox' ng-model='gridOptions.__selectall__'></div>";
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

                            var checkBoxElement;
                            if (selectionElement.length == 0) {
                                checkBoxElement = selectionElement;
                            } else {
                                checkBoxElement = selectionElement[0];
                            }
                            $(checkBoxElement).change(function () {
                                var data = $appUtil.resolveDot($scope, $scope.gridOptions.data);
                                var selected = $(this).is(":checked");
                                for (var i = 0; i < data.length; i++) {
                                    data[i].__selected__ = selected;
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

appStrapDirectives.directive("appText", ["$compile", "$appUtil", function ($compile, $appUtil) {
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

                    var fieldValue = $appUtil.resolveDot($scope, toBind);
                    if (fieldValue && fieldValue instanceof Object) {
                        $appUtil.putDottedValue($scope, toBind, JSON.stringify(fieldValue));
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

appStrapDirectives.directive('appToolBar', [ "$compile", "$appUtil",
    function ($compile, $appUtil) {
        'use strict';
        return {
            restrict:'C',
            scope:false,
            compile:function () {
                return {
                    pre:function ($scope) {


                    },
                    post:function ($scope, iElement) {
                        var html = "";

                        html += '<app-bar-close-resize-view></app-bar-close-resize-view>';
                        html += "<app-bar-basic></app-bar-basic>";

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
        template:"<div class='app-float-right'></div>",
        compile:function () {
            return{
                pre:function ($scope, iElement) {
                    var actions = [];


                    var template = "<div class='app-close-button-background app-float-left'>" + DEFAULT_ACTION_TEMPLATE + "</div>";
                    actions.push({"method":"close(action)", "label":"Close", "template":template, "class":"app-padding-top-twenty-px app-close-button app-bar-button"})

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


appStrapDirectives.directive('appBarBasic', [
    '$compile',
    function ($compile) {
        return {
            restrict:'E',
            replace:true,
            scope:true,
            template:"<div ng-class=\"{'app-float-right-important app-border-right-white':view.metadata.basicposition == 'right'}\" class='app-bar-basic'></div>",
            compile:function () {
                return  {
                    pre:function ($scope, iElement) {

                        var ACTIONS = {save:{"method":"save(action)", "label":"Save", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-save-button app-bar-button app-float-left"},
                            delete:{"method":"deleteData(action)", "label":"Delete", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-delete-button app-bar-button"},
                            refresh:{"method":"refresh(action)", "label":"Refresh", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-refresh-button app-bar-button"},
                            insert:{"method":"insert(action)", "label":"Insert", "template":DEFAULT_ACTION_TEMPLATE, "class":"app-insert-button app-bar-button"}
                        }

                        var actions = $scope.gridOptions.toolbar;

                        var actionCount = actions.length;
                        for (var i = 0; i < actionCount; i++) {
                            var action = ACTIONS[actions[i]];
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
