var app = angular.module('applane', ['ngGrid', '$strap.directives', '$appstrap.directives', '$appstrap.services'], function ($routeProvider, $locationProvider) {

});


// override the default input to update on blur
app.directive('ngModelOnblur', function() {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, elm, attr, ngModelCtrl) {
            if (attr.type === 'radio' || attr.type === 'checkbox') return;

            elm.unbind('input').unbind('keydown').unbind('change');
            elm.bind('blur', function() {
                scope.$apply(function() {
                    ngModelCtrl.$setViewValue(elm.val());
                });
            });
        }
    };
});

app.controller('AppCtrl', function ($scope, $http, $location, $dataModel, $appService, $timeout) {
    $scope.row = {"name":"Rohit Bansal"};
    $scope.col = {"expression":"name","type":"text","label":"Name","requiredcolumns":"name"};


    $scope.change = function(){
        console.log("value changed");
    }

    $scope.listener = $scope.$watch("row.name", function (newValue, oldValue){
        console.log(oldValue);
        console.log(newValue);


    })

    $scope.clear = function(){
        //clear the watch
        $scope.listener();
    }


});
