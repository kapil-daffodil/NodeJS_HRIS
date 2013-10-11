

appStrapDirectives.directive('appAdvancesearch', ["$compile", "$dataModel", "$appDomService", function ($compile, $dataModel, $appDomService) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class=\"app-button app-button-border app-button-margin app-button-padding app-button-shadow ng-scope\" ng-click=\"advanceSearch()\" ng-bind='action.label'></div>",
        compile:function () {
            return {
                pre:function ($scope, iElement) {

                    $scope.advanceSearch = function () {
                        var action = $scope.action;
                        var config = {

                            template:"<div class='app-popup-title'>" + action.label + "</div>" +
                                "<div class=\"app-popup-body advance-search-parent-div\">" +

                                "<div  ng-repeat='advancefilter in view.metadata.advancefilters'>" +

                                "<div ng-switch on='advancefilter.type'>" +
                                "<div ng-switch-when='referencefilter'>" +
                                "<bs-typeahead class='applied-search-filter-input' ng-init=\"row=view.appliedfilters;colmetadata=advancefilter\" onselection =\"onSelection\" ></bs-typeahead>" +
                                "</div>" +
                                "<div ng-switch-when='datefilter'>" +
                                "<app-calender ng-init=\"colmetadata=advancefilter\"></app-calender>" +
                                "</div>" +
                                "<div ng-switch-default></div>" +
                                "</div>" +
                                "</div>" +
                                "<div class=\"advance-search-button-row\"><input type=\"button\" ng-click='reset()' value='Reset' class=\"app-button app-button-border app-button-margin app-button-shadow app-advance-search-bttn-padding\" />" +
                                "<input type=\"button\" ng-click='apply()' value='Apply' class=\"app-button app-button-border app-button-margin app-button-shadow app-advance-search-bttn-padding\"/>" +
                                "<input type=\"button\" ng-click='cancel()' value='Cancel' class=\"app-button app-button-border app-button-margin app-button-shadow app-advance-search-bttn-padding\"/>" +
                                "</div>" +

                                "</div>",
                            scope:$scope,
                            autohide:false
                        };
                        $scope.filterPopup = $appDomService.showPopup(config);

                    };
                    $scope.onSelection = function (value, action) {
//                        var exp = action.expression;
//                        var filter = exp + "={" + exp + "}";
//                        var paramName = exp;
//                        var paramValue = value;
//                        $dataModel.addFilter($scope[COMPONENT_ID], action.id, filter, paramName, paramValue);
                    }
                    $scope.resetfilter = function () {

                    }
                    $scope.applyfilter = function () {
                        //iterate applied filters

                        var modelFilters = [];
                        for (var key in $scope.view.appliedfilters) {
                            if ($scope.view.appliedfilters.hasOwnProperty(key)) {
                                var keyValue = $scope.view.appliedfilters[key];
                                var exp = key;
                                var filter = exp + "={" + exp + "}";
                                var paramName = exp;
                                var paramValue = keyValue;

                                var advanceFilters = $scope.metadata.advancefilters
                                var count = advanceFilters.length;
                                for (var i = 0; i < count; i++) {
                                    var advanceFilter = advanceFilters[i];
                                    var expression = advanceFilter.expression;
                                    if (expression == exp) {
                                        if ($dataModel.isNullOrUndefined(paramValue)) {
                                            advanceFilter.editmode = false;
                                        } else {
                                            advanceFilter.editmode = true;
                                        }
                                    }

                                }
                                $dataModel.addFilter($scope[COMPONENT_ID], exp, filter, paramName, paramValue);
                            }
                        }
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

                }
            }
        }
    }
}]);


appStrapDirectives.directive("appAppliedfilters", ["$compile", "$dataModel", function ($compile, $dataModel) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='applied-filter-parent'  ng-show='appliedFilter.editmode' ng-repeat='appliedFilter in metadata.advancefilters '>" +

            "<div ng-show=\"!appliedFilter.editable\" class=\"app-button app-button-border app-button-margin app-button-padding app-button-shadow\">" +
            "<div ng-click=\"appliedFilter.editable=true\" ng-bind-template='{{appliedFilter.label + \":\" + view.appliedfilters[advancefilter.expression][advancefilter.display_column[0]] }}' class='app-applied-filter-label' ></div>" +
            "<div ng-click=\"removeFilter(appliedFilter)\" class='app-remove-filter'>X</div>" +
            "</div>" +
            "<div  ng-show = \"appliedFilter.editable\"  ><bs-typeahead onblurfn =\"onFilterBlur\" onselection =\"onFilterSelection\" style='width:150px;'  ng-init=\"row=view.appliedfilters;colmetadata=appliedFilter\"></bs-typeahead></div>" +
            "</div>",


        compile:function () {
            return {
                pre:function ($scope, iElement) {

                    $scope.removeFilter = function (appliedFilter) {
                        appliedFilter.editmode = false;

                    };

                    $scope.onFilterSelection = function (value, action) {
                        var exp = action.expression;
                        var filter = exp + "={" + exp + "}";
                        var paramName = exp;
                        var paramValue = value;
                        $dataModel.addFilter($scope[COMPONENT_ID], action.id, filter, paramName, paramValue);
                        $dataModel.refresh($scope[COMPONENT_ID]);
                    };

                    $scope.onFilterBlur = function (value, action) {
                        action.editable = false;
                    }


                }
            }
        }
    }
}]);


/**************** ASHISH JAIN ****************/
appStrapDirectives.directive('appFilters', ["$compile", "$dataModel", "$appDomService", function ($compile, $dataModel, $appDomService) {
    return {
        restrict:"E",
        replace:false,
        scope:true,
        template:"<div><input type='text'/>&nbsp;&nbsp;<input type='text'/><input type='button' value='delete' ng-click='deleteFilterRow()'/></div>",
        compile:function () {
            return {
                post : function($scope,iElement){
                    $scope.deleteFilterRow = function(){
                        iElement.remove();
                    }
                }
            }
        }
    }
}]);

$scope.applyFilters = function($event){
    var filterTemplate = "<div style='display:table;height: 100%;position: absolute;right: 0;top: 6px;width: 500px;'>" +
        "<div  style='display: table-row;width: 100%' class='app-popup-title'>Apply Filter</div>" +
        "<div style='border:1px solid black;background: none repeat scroll 0 0 white;padding: 10px;'>" +

        "<app-filters ></app-filters>"+

        "<div><input type='button' value='Add Filters' ng-click='addFilterRow($event)'/> </div>" +
        "</div>" +
        "</div>"

    var popup = {
        template:filterTemplate,
        scope:$scope.$new(),
        element:$event.target
    };
    $appDomService.showPopup(popup);
};

$scope.addFilterRow = function($event){
    var html = "<app-filters ></app-filters>";
    var element =  $event.target;
    var parent = $(element).parent();
    $($compile(html)($scope)).insertBefore(parent);
}

/*************************************/

