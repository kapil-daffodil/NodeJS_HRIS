var webApp = angular.module("webApp", []);

webApp.directive("redactor", function(){
    return {
        restrict: "A",
        require: "?ngModel",

        link: function ($scope,element,attrs){

            element.jqte();
			element.jqteVal("New article!");
			if (!$scope.$$phase) {
                        $scope.$apply();
                    }
           
        }
    }
});

webApp.controller('redactor', function ($scope, $location, $timeout) {
    $scope.row = {rte1:"<b>rohit bansal</b>"};
});