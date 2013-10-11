var watch1 = angular.module("watch1", []);


watch1.directive('ngBindModel', function ($compile) {
    return{
        compile:function (tEl, tAtr) {
            tEl[0].removeAttribute('ng-bind-model')
            return function (scope) {
                tEl[0].setAttribute('ng-model', scope.$eval(tAtr.ngBindModel))
                $compile(tEl[0])(scope)
                console.info('new compiled element:', tEl[0])
            }
        }
    }
})
watch1.directive('appTable', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        template:"<div>" +
            "<app-tr ng-repeat='row in data'>" +

            "</app-tr>" +
            "</div>",

        compile:function () {
            return  {
                post:function ($scope, element) {
                }
            };
        }
    }
}]);

watch1.directive('appTr', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        template:"<div><input type='button' ng-click='trClick()' value='TR'>" +
            "   <div ng-repeat='col in columns'>" +
            "     <app-editor></app-editor>  " +
            "   </div>" +
            "</div>",

        compile:function () {
            return  {
                pre:function ($scope, element) {

                   alert("PRE  appTr: Row>>>>" + JSON.stringify($scope.row));
                    Object.keys($scope.row).forEach(function (k) {
                        $scope[k] = $scope.row[k];
                    })


                },
                post:function ($scope, element) {
                    alert("POST appTr: Row>>>>" + JSON.stringify($scope.row));
//                    $scope.trClick = function () {
//                        alert($scope.sname)
//                    }
                }

            };
        }
    }
}]);
watch1.directive('appEditor', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        template:"<div><input type='text' ng-bind-model='col.expression'><span ng-click='editorClick()' ng-bind='row[col.expression]'></span> </div>",

        compile:function () {
            return  {
                pre:function ($scope, element) {
                    alert("PRE aappEditor: Row>>>>" + JSON.stringify($scope.col));
                },
                post:function ($scope, element) {
                    alert("POST aappEditor: Row>>>>" + JSON.stringify($scope.col));

                }
            };
        }
    }
}]);
watch1.controller('WatchCtrl', function ($scope, $location, $timeout) {
    $scope.data = [
        {"name":"rohit", "age":22},
        {"name":"Ashish", "age":10}
    ];
    $scope.columns = [
        {expression:"name"},
        {expression:"age"}
    ]

});

