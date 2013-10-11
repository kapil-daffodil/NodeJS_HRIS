var webApp = angular.module("webApp", []);

webApp.directive("redactor", function(){
    return {
        restrict: "A",
        require: "?ngModel",

        link: function ($scope,element,attrs){

            element.redactor({
                changeCallback: function(html){
                    $scope.row.rte1 = html;
                    if (!$scope.$$phase) {
                        $scope.$apply();
                    }
                }
            });
            element.redactor('set',$scope.row.rte1);
        }
    }
});

webApp.controller('redactor', function ($scope, $location, $timeout) {
    $scope.row = {rte1:"<b>rohit bansal</b>"};
});