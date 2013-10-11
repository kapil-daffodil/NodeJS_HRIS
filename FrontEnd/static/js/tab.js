var app = angular.module('myApp', ['ngGrid', '$strap.directives'], function ($routeProvider, $locationProvider) {
//    $routeProvider.when('/ng-grid/login', {
//        templateUrl: '/ng-grid/login.html',
//        controller: LoginCtrl
//
//    });
//    $routeProvider.when('/ng-grid/index.html', {
//        templateUrl: '/ng-grid/index.html',
//        controller: MyCtrl
//    });
//
//    $routeProvider.otherwise({redirectTo: '/phones'});

    // configure html5 to get links working on jsfiddle
    $locationProvider.html5Mode(true);

});


app.directive("appText", function () {
    return {
        restrict:"E",
        scope:{
            row:"=",
            col:"="
        },
        template:"<input ng-class=\"'colt' + col.index\" type=\"text\" ng-model='row.entity[col.field]'/>"
    }
});

app.controller('LoginCtrl', function ($scope, $http, $location, $window) {
    $scope.username = "Rohit";
    $scope.password = "daffodil";
});

app.controller('MyCtrl', function ($scope, $http, $location) {

    $scope.save = function(){
        alert("save called");
    }
    $scope.actions=  [
        {
            "template": "<div class='app-button' ng-click='save()'>Save</div>",
            action : $scope.save

        },
        {
            "template": "<div class='app-button' >Delete</div>",
            action : $scope.save

        },
        {
            "template": "<div class='app-button' >Refresh</div>",
            action : $scope.save

        }
    ]

});
