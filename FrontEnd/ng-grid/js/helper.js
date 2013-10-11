var app = angular.module('applane', [], function () {

});

app.directive('appDataTable', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<table cellpadding='0' cellspacing='0' class='tableborder'>" +
            "<tr><td ng-repeat='col in gridoptions.columns' ng-bind='col' class='tdborder'></td></tr>" +
            "<tr ng-repeat='row in gridoptions.data'><td ng-repeat='col in gridoptions.columns' class='tdborder' ><app-data-table-cell></app-data-table-cell></td></tr>" +
            "</table>",
        compile:function () {
            return  {


            };
        }
    }
}]);

app.directive('appDataTableCell', ["$compile", function ($compile) {
    return {
        restrict:"E",
        replace:true,
        scope:true,

        compile:function () {
            return  {

                post:function ($scope, iElement) {
                    var val = $scope.row[$scope.col];
                    if (angular.isArray(val)) {
                        var columns = $scope.getColumns(val);
                        var gridoptions = {"columns":columns, "data":val};
                        $scope.gridoptions = gridoptions;
                        var html = "<app-data-table></app-data-table>";
                        var elem = $compile(html)($scope);
                        iElement.append(elem);
                    }else if (angular.isObject(val)) {
                        var columns = $scope.getColumns([val]);
                        var gridoptions = {"columns":columns, "data":[val]};
                        $scope.gridoptions = gridoptions;
                        var html = "<app-data-table></app-data-table>";
                        var elem = $compile(html)($scope);
                        iElement.append(elem);
                    } else {
                        iElement.append(val);
                    }

                }


            };
        }
    }
}]);


app.controller('AppCtrl', function ($scope) {
    $scope.query = '{"collection":"","database":"baas","query":{}}';
    $scope.getColumns = function (data) {

        var count = data.length;
        var childs = [];
        for (var j = 0; j < count; j++) {
            var record = data[j];

            for (var key in record) {
                var childCount = childs.length;
                var found = false;
                for (var i = 0; i < childCount; i++) {
                    if (childs[i] == key) {
                        found = true;
                        break;
                    }

                }
                if (!found) {
                    childs.push(key);
                }

            }

        }
        return childs;
    }

    $scope.gridoptions = [];


    $scope.sample = function(option){
        if(option=='update'){
         $scope.query = '{"updates":{"table":"","operations":[{}]},"ask":"baas"}';
        }
    }
    $scope.getdata = function (option) {
        var data = [];
        var columns = $scope.getColumns(data);
        var gridoptions = {"columns":columns, "data":data};

        $scope.gridoptions = gridoptions;
        if (!$scope.$$phase) {
            $scope.$apply();
        }


        var url = "http://127.0.0.1:8888/";
        var requestBody = $scope.query;
        var params = {};
        params[option] = requestBody;


        var callType = "GET";
        var dataType = "json";

        $.ajax({
            type:callType,
            url:url,
            data:params,
            crossDomain:true,
            success:function (returnData, status, xhr) {

                var data;
                if (angular.isArray(returnData)) {
                    data = returnData
                } else if (angular.isObject(returnData)) {
                    data = [returnData];
                }else if(returnData){
                    data = [{"Response":returnData}]
                }else{
                    data = [{"Response":"No response body"}]
                }
                var columns = $scope.getColumns(data);
                var gridoptions = {"columns":columns, "data":data};

                $scope.gridoptions = gridoptions;
                if (!$scope.$$phase) {
                    $scope.$apply();
                }


            },
            error:function (jqXHR, exception) {


                alert("exception in making [" + url + "] :[" + exception + "]");
            },
            timeout:1200000,
            dataType:dataType,
            async:true
        });


    }


});
