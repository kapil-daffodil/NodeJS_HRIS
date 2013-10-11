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

                        var visibleExpression = column.visibleexpression;
                        if (visibleExpression != undefined && !visibleExpression) {
//                                continue;
                        }
                        var type = column.type;

                        if (type === 'lookup' ) {
                            if (type === 'select') {
                                column.display_column = [column.expression];
                            }
                            column.editableCellTemplate = "<bs-typeahead  ng-init=\"row=row;colmetadata=colmetadata;\"></bs-typeahead>";


                        } else if (type === 'duration') {
                            var timeUnitMetaData = {};
                            timeUnitMetaData.expression = "timeunit";
                            timeUnitMetaData.id = column.id + "_timeunit";
                            timeUnitMetaData.refferedresource = {"resource":"duration_timeunits"};
                            timeUnitMetaData.display_column = ["unit", "__key__"];
                            timeUnitMetaData.type = "reference";
                            timeUnitMetaData.label = "Unit";

                            column.timeunitmetadata = timeUnitMetaData;

                            var timeMetaData = {};
                            timeMetaData.expression = "time";
                            timeMetaData.id = column.id + "_time";
                            timeMetaData.type = "text";
                            timeMetaData.label = "Time";

                            column.timemetadata = timeMetaData;
                            column.editableCellTemplate = "<app-duration  ng-init = \"row=row;colmetadata=colmetadata;\"></app-duration>";
                        } else if (type === 'date' || type == 'datetime') {
                            column.editableCellTemplate = '<div class="app-grid-datepicker-parent">' +
                                '<input type="text" ng-model="row[colmetadata.expression]" data-date-format="dd/mm/yyyy" app-datepicker class="app-grid-date-picker-input" style="min-height: 20px;">' +
                                '<input type="text" data-toggle="datepicker" class="app-grid-date-picker-calender-image" />' +
                                '</div>';
                        } else if (type === 'textarea') {
                            column.editableCellTemplate = '<textarea></textarea>';

                        } else {
                            column.editableCellTemplate = "<app-text class='app-panel-input' ng-init=\"row=row;colmetadata=colmetadata\" ></app-text>";
                        }
                    }



                    var resourceName = metadata.resource;

                    var dataRecords = $scope.view.data[resourceName];
                    var dataRecordCount = dataRecords.length;
                    if (dataRecordCount == 0) {
                        $dataModel.insert($scope[COMPONENT_ID]);
                    }

                    $scope.row = $scope.view.data[resourceName][0];


                    var toolBarTemplate = "<app-bar ng-init = \"actions=view.metadata.headeractions; \" ></app-bar>";
                    var toolBarElement = $compile(toolBarTemplate)($scope);
                    iElement.append(toolBarElement);


                    var html = '';

                    var newcolumns = [];
                    var newColumnIndex = -1;


                    if (columnCount > 0) {

                        html += "<div class='columns-group-parent'>";


                        for (var j = 0; j < columnCount; j++) {
                            newcolumns.push(columns[j]);
                            newColumnIndex++;
                            var label = columns[j].label;
                            var type = columns[j].type;

                            var classNameTemp = className;

                            if (type === 'textarea') {
                                classNameTemp = "app-panel-full-row";
                            }

                            html += "<div class='" + classNameTemp + "'>";
                            var width = columns[j].width;
                            if (!width) {
                                width = 200;
                            }
                            width += "px";
                            if (label != null && label != undefined) {
                                html += '<div class="app-panel-label">' + label + '</div>';
                            }

                            html += "<app-column style='width:" + width + "' class='app-panel-input-parent' ng-init='colmetadata=newcolumns[" + newColumnIndex + "]'></app-column>";
                            html += "</div>";


                        }
                        html += "</div>";
                    }


                    $scope.newcolumns = newcolumns;
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
