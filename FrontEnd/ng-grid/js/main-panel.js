var app = angular.module('applane', ['$strap.directives', '$appstrap.directives', '$appstrap.services'], function ($routeProvider, $locationProvider) {

});


app.controller('AppCtrl', function ($scope, $compile, $http, $location, $dataModel, $appService, $appDomService, $rootScope, $timeout) {
    $rootScope.busymessage = "...";
    $rootScope.showbusymessage = false;
    $scope.views = [];
    $scope.loggedin = false;

    $scope.setStartTime = function () {
        $scope.startDateTime = new Date().getTime();
    }

    $scope.getStartTime = function () {
        return $scope.startDateTime;
    }

    $scope.getTotalTime = function () {
        return (new Date().getTime() - $scope.startDateTime);
    }

    $scope.log = function (log, clear) {
        if (clear) {
            $("#log").val(log);
        } else {
            $("#log").val($("#log").val() + " - " + log);
        }

    }

    $scope.organizationcollection = {"label":"Organizations", "display":["label"]};
    $scope.viewgroupcollection = {"label":"View groups", "display":["viewgroupid", "viewgroup"], "settingimage":"images/setting.png"};

    $appService.getUser(function (userinfo) {


        var login = userinfo.login;
        if (login == false) {
            window.location.href = "/login";
            return;
        } else {
            var username = userinfo.username;

            $scope.username = username;
            $scope.loggedin = true;


            $scope.viewgroupcollection.label = userinfo.viewgroup.viewgroupid.viewgroup;
            $scope.viewgroupcollection.options = userinfo.viewgroups;

            var viewGroupData = [];
            var viewGroupcount = userinfo.viewgroups.length;
            for (var i = 0; i < viewGroupcount; i++) {
                viewGroupData.push({"__key__":userinfo.viewgroups[i].__key__, "viewgroup":userinfo.viewgroups[i].viewgroupid.viewgroup});

            }
            $scope.viewgroupcollection.settingview = {"data":{"data":viewGroupData}, "metadata":{"label":"Viewgroups", "enablemetadataaction":false, "columns":[
                {"expression":"viewgroup", "type":"text", "width":200, "visibility":"Both", "label":"View group"}
            ], "type":"table"}, "applicationid":"applanedeveloper", "ask":"0530db61-c658-4864-856b-6747e3a00476", "showas":"popup", "width":300, "height":300};

            $scope.organizationcollection.label = userinfo.organization.label;
            $scope.organizationcollection.options = userinfo.organizations;
            $scope.currentorganization = userinfo.organization;

            var menus = userinfo.menus;

            var menuCount = menus.length;
            for (var i = 0; i < menuCount; i++) {
                menus[i].viewgroupid = userinfo.viewgroup.viewgroupid;

            }

            var settingMenu = {"imageurl":"images/setting.png", "view":{"data":{"data":angular.copy(menus)}, "metadata":{"label":"Menus", "enablemetadataaction":false, "columns":[
                {"expression":"label", "type":"text", "width":100, "visibility":"Both", "label":"Menu"},
                {"expression":"viewgroupid", "type":"lookup", "width":150, "visibility":"Both", "label":"View group", "table":"viewgroups__applanedeveloper", "columns":[
                    {"expression":"viewgroup", "type":"string"}
                ]},
                {"expression":"applicationid", "type":"lookup", "width":150, "visibility":"Both", "label":"Application", "table":"app_applications", "columns":[
                    {"expression":"id", "type":"string"}
                ]},
                {"expression":"table", "type":"lookup", "width":150, "visibility":"Both", "label":"Table", "table":"resources", "columns":[
                    {"expression":"id", "type":"string"}
                ]}
            ], "type":"table"}, "applicationid":"applanedeveloper", "ask":"0530db61-c658-4864-856b-6747e3a00476", "showas":"popup", "width":650, "height":500}};

            menus.push(settingMenu);
            $scope.menus = menus;
            $appDomService.addView(userinfo.view, $scope);

        }

    });


    $scope.logOut = function () {
        $appService.logOut(function (data) {
            window.location.href = "/login";
            return;

        })
    }

    $scope.onQuickViewSelection = function (quickView) {

        $appDomService.addView({"viewid":quickView.viewid.viewid, "applicationid":quickView.applicationid.id, "ask":quickView.applicationid.oauthkey, "osk":$scope.currentorganization.oauthkey}, $scope);

    }

    $scope.getColumnValue = function (row, col, asHTML) {
        var val;
        if (asHTML) {
            val = "&nbsp;";
        } else {
            val = "";
        }
        var exp = col.expression;


        if (row && exp && row[exp]) {
            var type = col.type;

            if (type == 'lookup') {
                var multiple = col.multiple;
                if (multiple) {
                    val = row[exp];
                    if (val && val.length > 0) {
                        val = val[0][col.columns[0].expression];
                    }
                } else {
                    var colValue = row[exp];
                    if (angular.isObject(colValue)) {
                        val = colValue[col.columns[0].expression]
                    } else {
                        val = colValue;
                    }
                }
            } else if (type == 'string' || type == 'text' || type == 'boolean') {
                val = row[exp];
            } else {
                val = col.type + "-" + JSON.stringify(row[col.expression])
            }
        }

        return val;


    }


});
