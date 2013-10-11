var appStrapDirectives = angular.module('$appstrap.directives', []);
var SPAN = 'span';

appStrapDirectives.directive('test', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-float-left app-padding-top-bottom-ten-px' style='background: white;'>" +


            "<option-directive></option-directive>" +


            "<div class='app-float-left app-width-full app-padding-five-px'>" +
            "<input type='button' value='Schedule' ng-click='schedule()' class='app-button app-button-border app-button-margin app-button-padding app-button-shadow'/>" +
            "</div>" +


            "</div>",
        compile:function () {
            return  {
                post:function ($scope, iElement) {
                    $scope.repeatedeverycollection = [
                        {"label":"1"},
                        {"label":"2"},
                        {"label":"3"}
                    ];
                }
            }
        }
    }
}]);

appStrapDirectives.directive('optionDirective', function () {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template :
            "<div class='app-float-left app-width-full app-padding-five-px'>" +
            "<div class='app-float-left app-font-weight-bold app-text-align-right app-width-one-twenty-px'>Repeat : </div>" +
            "<div class='app-float-left app-padding-left-twenty-px' >" +
            '<select  ng-change="changeValue();" ng-model="row[colmetadata.expression].span"  ng-options="option.label as option.label for option in repeatoptions"></select>' +
            "</div>" +
            "</div>",
        compile:function () {
            return  {
                pre:function ($scope, iElement) {
//                   console.log("Row : " + JSON.stringify($scope.row) + " , col : "  + JSON.stringify($scope.colmetadata) + " , span : " + $scope.row[$scope.colmetadata.expression].span)
                    $scope.repeatoptions = [
                        {label:"Daily", slabel:'days'},
                        {label:"Weekly", slabel:'weeks'},
                        {label:"Monthly", slabel:'months'},
                        {label:"Yearly", slabel:'years'} ,
                        {label:"Hourly", slabel:'hour'}
                    ]

                    $scope.changeValue = function () {
                        console.log($scope.row[$scope.colmetadata.expression][SPAN])
                    }


                }
            }
        }
    }
});