var app = angular.module('applane', ['$appModule'], function ($routeProvider, $locationProvider) {
    $locationProvider.html5Mode(true);
});

app.controller('AppCtrl', function ($scope, $location, $timeout, $appDataSource, $appModel) {
    $scope.appData = {viewGroups:{label:"","display":["label"],options:[]}, organizations:{label:"","display":["label"],options:[]}};
    $scope.$watch('appData.userLogin', function (newValue, oldValue) {
        if (!angular.equals(newValue, oldValue)) {
            if (!newValue) {
                window.location.href = "/login.html";
            }
        }

    }, true);
    $appModel.init($scope,$appDataSource);
});

