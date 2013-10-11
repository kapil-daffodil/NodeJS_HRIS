var appModule = angular.module('$appModule', []);
appModule.directive('appText', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div></div>",
        compile:function () {
            return  {
                pre:function ($scope, element) {
                },
                post:function ($scope, element) {
                    var toBind = $scope.model + "." + $scope.field;
                    var html = "<input type='text' ng-model='" + toBind + "' />";
                    element.append($compile(html)($scope));

                }
            };
        }
    }
}]);


