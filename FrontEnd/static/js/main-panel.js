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
    $scope.viewgroupcollection = {"label":"View groups", "display":["label"], "settingimage":"images/setting.png","callback":function(option){
             $scope.getUserState({"viewgroup":option.label});
    }};

    $scope.getUserState = function(state){
        $appService.getUser(state, function (userinfo) {


            var login = userinfo.login;
            if (login == false) {
                window.location.href = "/login.html";
                return;
            } else {
                var username = userinfo.username;

                $scope.username = username;
                $scope.loggedin = true;

                $scope.viewgroupcollection.options = userinfo.viewgroups;
                $scope.viewgroupcollection.label = userinfo.viewgroups[userinfo.viewgroupindex].label;


                var viewGroupData = [];
                var viewGroupcount = userinfo.viewgroups.length;
                for (var i = 0; i < viewGroupcount; i++) {
//                viewGroupData.push({"__key__":userinfo.viewgroups[i].__key__, "viewgroup":userinfo.viewgroups[i].viewgroupid.viewgroup});
                    viewGroupData.push({"_id":userinfo.viewgroups[i][KEY],"label":userinfo.viewgroups[userinfo.viewgroupindex].label});

                }

                $scope.viewgroupcollection.settingview = {"data":{"data":viewGroupData}, "metadata":{"label":"Viewgroups", "enablemetadataaction":false, "columns":[
                    {"expression":"label", "type":"text", "width":200, "visibility":"Both", "label":"label"}
                ], "type":"table","table":{"id":"viewgroups__appsstudio"}}, "ask":"appsstudio", "showas":"popup", "width":300, "height":300};


                $scope.organizationcollection.label = userinfo.organizations[userinfo.organizationindex].label;
                $scope.organizationcollection.options = userinfo.organizations;
                $scope.currentorganization = userinfo.organizations[userinfo.organizationindex];

                var menus = userinfo.menus;
                if(userinfo.menus)

                    var menuCount = menus.length;
                var menusData = [];
                for (var i = 0; i < menuCount; i++) {
                    menusData.push({"menus.label":menus[i].label, "menus.applicationid":menus[i].applicationid,"menus.table":menus[i].table,"menus._id":menus[i]._id});
                }



                var settingMenu = {"imageurl":"images/setting.png", "view":{"data":{"data":menusData}, "metadata":{"label":"Menus", "primarycolumn":"menus._id","filter":{"_id":"{viewgroupid}"},"parameters":{"viewgroupid":userinfo.viewgroups[userinfo.viewgroupindex]._id},"enablemetadataaction":false, "columns":[
                    {"expression":"menus.label", "type":"text", "width":100, "visibility":"Both", "label":"Label"},
                    {"expression":"menus.applicationid", "type":"lookup", "width":150, "visibility":"Both", "label":"Application", "table":"app_applications", "columns":[
                        {"expression":"id", "type":"string"}
                    ]},
                    {"expression":"menus.table", "type":"lookup", "width":150, "visibility":"Both", "label":"Table", "table":"resources", "columns":[
                        {"expression":"id", "type":"string"}
                    ]}
                ], "type":"table","table":"viewgroups__appsstudio"}, "applicationid":"applanedeveloper", "ask":"appsstudio", "showas":"popup", "width":650, "height":500}};

                menus.push(settingMenu);
                $scope.menus = menus;
                if(userinfo.view){
                    $appDomService.addView(userinfo.view, $scope);
                }

            }

        });


    }

    $scope.getUserState({});

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
