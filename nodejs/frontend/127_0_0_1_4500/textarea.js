var webApp = angular.module("webApp", []);

webApp.directive("textareaAutoResize", function(){
    return {
        restrict: "E",
        replace : true,
        scope : true,
        template : '<textarea></textarea>',

        post: function ($scope,element,attrs){

                element.flexible();


        }
    }
});







webApp.controller('textarea', function ($scope, $location, $timeout) {
    $scope.row = {rte1:"rohit bansalrohit bansalrohit bansalrohit bansalrohit bansal"};
});