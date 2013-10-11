var CELL_TEMPLATE = "<span ng-bind-html-unsafe='getColumnValue(row,col,true)'></span>";

appStrapDirectives.directive('applaneFreezeGrid', [
    '$compile', '$timeout',
    function ($compile, $timeout) {
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
                "<tr style='height:25px;'  ng-class-even=\"'applane-grid-row-even'\" ng-class-odd=\"'applane-grid-row-odd'\"  ng-class=\"{'app-selected-tr':row[view.metadata.primarycolumn] == currentRow[view.metadata.primarycolumn]}\" ng-repeat='row in view.data[view.metadata.tablequery.resource]'>" +
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

                }

            }
        };
    }
]);


appStrapDirectives.directive('applaneGrid', [
    '$compile', '$timeout',
    function ($compile, $timeout) {
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
    '$compile', '$timeout', '$dataModel',
    function ($compile, $timeout, $dataModel) {
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
                                    $scope.bindLeftKeyDownEvent();
                                } else if (evt.keyCode == 38) {       // up arrow
                                    $scope.bindUpKeyDownEvent();
                                } else if (evt.keyCode == 39) {      // right arrow
                                    evt.preventDefault();
                                    $scope.bindRightKeyDownEvent();
                                } else if (evt.keyCode == 40) {       // down arrow
                                    $scope.bindDownKeyDownEvent();
                                } else if (evt.keyCode == 113) {      // for F2 key press
                                    $scope.editCell();
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
                                            break;
                                        }
                                    }
                                }
                            } else {
                                alert("Previous Element Length is zero");
                            }
                        }

                        $scope.showRenderingCell = function () {
                            console.log('applane blur');
                            $scope.removeStyleFromElement();
                            var columnCellTemplate = $scope.col.cellTemplate;
                            if ((!columnCellTemplate) && ($scope.col.expression)) {
                                columnCellTemplate = CELL_TEMPLATE;
                            }
                            var cellTemplate = "<div style='white-space: nowrap;overflow: hidden;' ng-dblclick='editCell()' class='applane-grid-cell-inner'>" + columnCellTemplate + "</div>";

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
//                            $(iElement).attr("tabIndex", "0");

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


                        $scope.showEditorCellIfAny = function () { // Add Listener on td element to show editor if any defined

                            if (!$scope.editMode) {
                                return;
                            }
                            $scope.setCurrentRow($scope.row);
                            var editableCellTemplate = $scope.col.editableCellTemplate;
                            if (editableCellTemplate) {
                                $(iElement).attr("tabIndex", "-1");
                                var elementHeight = $(iElement).height();
                                var elementWidth = $(iElement).width();

                                var editable = $compile(editableCellTemplate)($scope);
                                $(iElement).html("");


                                editable.height(elementHeight);
                                editable.width(elementWidth);
                                $(iElement).append(editable);
                                $timeout(function () {
                                    $scope.addListenersToHideEditor(editable)
                                }, 0);
                                if (!$scope.$$phase) {
                                    $scope.$apply();
                                }
                            }
                        }

                        $scope.addListenersToHideEditor = function (editorElement) {
                            var childInputElements = angular.element(editorElement).find('input');
                            if (childInputElements.length == 0) {
                                editorElement.focus();
                                editorElement.blur($scope.showRenderingCell);
                                $scope.addStyleToElement();
                                $scope.bindEscapeKeyDown(editorElement); //  bind escape key down event
                                iElement.unbind('keydown');
                            } else if (childInputElements.length == 1) {
                                childInputElements[0].blur($scope.showRenderingCell);
                                $timeout(function () {
                                    angular.element(childInputElements[0]).focus();
                                }, 0);
                                $scope.addStyleToElement();
                                $scope.bindEscapeKeyDown(editorElement); //  bind escape key down event
                                iElement.unbind('keydown');
                            } else {
                                iElement.unbind('keydown');
                                angular.element(childInputElements[0]).focus();
                                $scope.addStyleToElement();
                                var noOfInputElements = childInputElements.length;
                                for (var i = 0; i < noOfInputElements; i++) {
                                    var childInput = childInputElements[i];
                                    $(childInput).attr("data-index", i);
                                    $(childInput).blur(function () {
                                        $timeout(function () {
                                            var focusedElement = angular.element(editorElement).find('input:focus');
                                            if (!focusedElement || focusedElement.length == 0) {
                                                $scope.showRenderingCell();
                                            }
                                        }, 0)
                                    });

                                    angular.element(childInput).bind('keydown', function (evt) {
                                        var childIndex = parseInt($(evt.target).attr("data-index"));
                                        var changeFocus = false;

                                        if (evt.keyCode == 9) { // for tab key
                                            if (evt.shiftKey && childIndex > 0) {
                                                childIndex = childIndex - 1;
                                                changeFocus = true;
                                            } else if (!evt.shiftKey && childIndex < (noOfInputElements - 1)) {
                                                childIndex = childIndex + 1;
                                                var nextElement = childInputElements[childIndex];
                                                var tabIndexValue = $(nextElement).attr('tabindex');
                                                if (!(tabIndexValue && tabIndexValue == -1)) { // In case of input type button in reference, tabindex will be -1
                                                    changeFocus = true;
                                                }
                                            }
                                        } else if (evt.keyCode == 27) {    // for escape key
                                            $scope.setEditMode(false);
                                            $(evt.target).blur();
                                            $(iElement).focus();
                                        }

                                        if (changeFocus) {
                                            $timeout(function () {
                                                angular.element(childInputElements[childIndex]).focus();
                                            }, 0);
                                        }
                                    })
                                }
                            }
                        };


                        $scope.bindEscapeKeyDown = function (editorElement) {
                            angular.element(editorElement).bind('keydown', function (evt) {
                                if (evt.keyCode == 27) {    // for escape key
                                    $scope.setEditMode(false);
                                    $(evt.target).blur();
                                    $(iElement).focus();
                                }
                            });
                        }


                        $scope.addStyleToElement = function () {
                            $(iElement).addClass('applane-grid-cell-focus');
                        };

                        $scope.removeStyleFromElement = function () {
                            if ($(iElement).hasClass('applane-grid-cell-focus')) {
                                $(iElement).removeClass('applane-grid-cell-focus');
                            }
                        };

                        $scope.editCell = function ($event) {
                            $scope.setEditMode(true);
                            $scope.showEditorCellIfAny();
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

                        var cellTemplate = '<div>{{col.label}}</div>';
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