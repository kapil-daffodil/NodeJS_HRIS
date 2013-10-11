var app = angular.module('applane', ['$strap.directives', '$appstrap.directives', '$appstrap.services'], function ($routeProvider, $locationProvider) {

});


app.controller('AppCtrl', function ($scope, $compile, $http, $location, $dataModel, $appService, $appDomService, $rootScope, $timeout) {
//    {"response":{"Server Time":206,"requestid":"","taskmanager_businessfunctions":[{"__key__":1,"name":"Management "},{"__key__":2,"name":"Delivery"},{"__key__":3,"name":"Accounts"},{"__key__":4,"name":"HR"},{"__key__":5,"name":"Admin"},{"__key__":6,"name":"SysAdmin"},{"__key__":7,"name":"Sales"},{"__key__":8,"name":"Marketing"},{"__key__":9,"name":"Legal"},{"__key__":10,"name":"Finance"}]},"status":"ok","code":200}
    $scope.view = {"metadata":{"tablequery":{"resource":"taskmanager_tasks"}},"applicationid":"task_manager","ask":"b0a25e52-8af3-431b-a362-196272a0fa13","osk":"f7122da8-8475-4bee-ad9a-0130cd0af0a3","data":{"taskmanager_tasks":[]}};
    $scope.componentid = "tasks1";
    $dataModel.putView($scope.componentid,$scope.view);
    $dataModel.setScope($scope.componentid,$scope);

   $scope.row = {"businessfunction_id":[{"__key__":1,"name":"Management"},{"__key__":2,"name":"Delivery"}]};
    $scope.colmetadata = {"__key__":2,"expression":"businessfunction_id","refferedresource":"taskmanager_businessfunctions","display_column":["name"],"visibility":"Both","businessfunction_id":["name"],"width":200,"fkcolumn":"businessfunction_id","label":"Business Functions","type":"lookup","multiple":true};

    $scope.getColumnValue = function (row, col) {


        if (row[col.expression]) {
            var type = col.type;
            var val = "";
            if (type == 'lookup') {
                var multiple = col.multiple;
                if (multiple) {
                    val = row[col.expression];
                    if (val && val.length > 0) {
                        val = val[0][col.display_column[0]];
                    }
                } else {
                    var colValue = row[col.expression];
                    if (angular.isObject(colValue)) {
                        val = colValue[col.display_column[0]]
                    } else {
                        val = colValue;
                    }
                }

            }

        }
        return val;


    }

});
