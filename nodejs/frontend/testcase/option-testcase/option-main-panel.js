var app = angular.module('applane', ['$strap.directives', '$appstrap.directives'], function () {

});


app.controller('AppCtrl', function ($scope, $rootScope) {
    $scope.colmetadata = {"type":"schedule","expression":"when"} ;
    $scope.row = {"when":{"span":"Daily"}};

});


