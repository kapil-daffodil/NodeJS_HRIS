/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 5/10/13
 * Time: 12:58 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../../shared/Constants.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var DatabaseEngine = require("../../database/DatabaseEngine.js");
var UpdateEngine = require("../../database/UpdateEngine.js");
var APIConstants = require("nodejsapi/Constants.js");
var Utils = require("apputil/util.js");
var MetadataProvider = require('../../metadata/MetadataProvider.js');
var GenerateXSLT = require("../../util/GenerateXSLT.js");
var ViewService = require("../appsstudio/ViewService.js");

exports.scheduleNotifications = function (parameters, callback) {
    getSchedules(parameters, ApplaneCallback(callback, function (schedules) {
        console.log(schedules.user);
        var userScheduleMap = sendUserNotifications(schedules.user);
        var applicationsMap = {};
        sendOrganizationNotifications(schedules.organization, applicationsMap, userScheduleMap, ApplaneCallback(callback, function (organizationScheduleMap) {
            sendApplicationNotifications(schedules.application, organizationScheduleMap, applicationsMap, userScheduleMap, callback);
        }));
    }))
}

function sendApplicationNotifications(applicationSchedules, organizationScheduleMap, applicationsMap, userScheduleMap, callback) {
    var applicationOrganizationsMap = {};
    Utils.iterateArray(applicationSchedules, callback, function (applicationSchedule, callback) {
        var applicationScheduleId = applicationSchedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID];
        var applicationScheduleVisibility = applicationSchedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.VISIBILITY];
        if (applicationScheduleVisibility == Constants.AppsStudio.Views.Schedules.Visibility.ON) {
            var viewId = applicationSchedule[Constants.AppsStudio.Views.VIEWID];
            var baasViewId = applicationSchedule[Constants.AppsStudio.Views.BAAS_VIEWID];
            getApplications(applicationsMap, viewId, baasViewId, ApplaneCallback(callback, function (applications) {
                Utils.iterateArray(applications, callback, function (application, callback) {
                    if (!application[Constants.Baas.Applications.ORGENABLED]) {
                        var organizationId = Constants.Baas.Organizations.GLOBAL_ORGANIZATION;
                        getUsersAndSendMail([application], organizationId[APIConstants.Query._ID], userScheduleMap, applicationSchedule);
                        callback();
                    } else {
                        getApplicationOrganizations(application[APIConstants.Query._ID], applicationOrganizationsMap, ApplaneCallback(callback, function (organizations) {
                            for (var i = 0; i < organizations.length; i++) {
                                var organization = organizations[i];
                                if (!organizationScheduleMap[applicationScheduleId[APIConstants.Query._ID + "__" + organization[APIConstants.Query._ID]]]) {
                                    getUsersAndSendMail([application], organization[APIConstants.Query._ID], userScheduleMap, applicationSchedule);
                                }
                            }
                            callback();
                        }))
                    }
                })
            }));
        }
        else {
            callback();
        }
    })
}

function sendOrganizationNotifications(organizationSchedules, applicationsMap, userScheduleMap, callback) {
    var organizationScheduleMap = {};
    Utils.iterateArray(organizationSchedules, ApplaneCallback(callback, function () {
        callback(null, organizationScheduleMap);
    }), function (organizationSchedule, callback) {
        var organizationScheduleId = organizationSchedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID];
        var organizationId = organizationSchedule[Constants.AppsStudio.Views.ORGANIZATIONID];
        organizationScheduleMap [organizationScheduleId[APIConstants.Query._ID] + "__" + organizationId[APIConstants.Query._ID]] = organizationSchedule;
        var organizationScheduleVisibility = organizationSchedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.VISIBILITY];
        if (organizationScheduleVisibility == Constants.AppsStudio.Views.Schedules.Visibility.ON) {
            var viewId = organizationSchedule[Constants.AppsStudio.Views.VIEWID];
            var baasViewId = organizationSchedule[Constants.AppsStudio.Views.BAAS_VIEWID];
            getApplications(applicationsMap, viewId, baasViewId, ApplaneCallback(callback, function (applications) {
                getUsersAndSendMail(applications, organizationId[APIConstants.Query._ID], userScheduleMap, organizationSchedule);
                callback();
            }));
        } else {
            callback();
        }
    })
}

function getApplicationOrganizations(applicationId, applicationOrganizationsMap, callback) {
    if (applicationOrganizationsMap[applicationId]) {
        callback(null, applicationOrganizationsMap[applicationId]);
    } else {
        var filter = {};
        filter[Constants.Baas.Organizations.APPLICATIONS + "." + APIConstants.Query._ID] = applicationId;
        var query = {};
        query[APIConstants.Query.TABLE] = Constants.Baas.ORGANIZATIONS;
        query[APIConstants.Query.FILTER] = filter;
        DatabaseEngine.query({
            ask:Constants.AppsStudio.ASK,
            query:query,
            callback:ApplaneCallback(callback, function (result) {
                var organizations = result[APIConstants.Query.Result.DATA] || [];
                applicationOrganizationsMap[applicationId] = organizations;
                callback(null, organizations);
            })
        })
    }
}

function sendUserNotifications(userSchedules) {
    var userScheduleMap = {};
    if (userSchedules && userSchedules.length > 0) {
        for (var i = 0; i < userSchedules.length; i++) {
            var userSchedule = userSchedules[i];
            var userScheduleId = userSchedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID];
            var userId = userSchedule[Constants.AppsStudio.Views.USERID];
            var organizationId = userSchedule[Constants.AppsStudio.Views.ORGANIZATIONID] || Constants.Baas.Organizations.GLOBAL_ORGANIZATION;
            userScheduleMap[userScheduleId[APIConstants.Query._ID] + "__" + organizationId[APIConstants.Query._ID] + "__" + userId[APIConstants.Query._ID]] = userSchedule;
            var userScheduleVisibility = userSchedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.VISIBILITY];
            if (userScheduleVisibility == Constants.AppsStudio.Views.Schedules.Visibility.ON) {
                sendMail(userSchedule);
            }
        }
    }
    return userScheduleMap;
}

function sendMail(schedule) {
    var scheduleId = schedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID];
    var viewId = scheduleId[Constants.AppsStudio.Schedules.VIEWID];
    var dateFilter = scheduleId[Constants.AppsStudio.Schedules.DATE_FILTER];
    var dateColumn = scheduleId[Constants.AppsStudio.Schedules.DATE_COLUMN];
    var parameters = {};
    ViewService.getView(parameters, function (err, view) {
        if (err) {
            //
        } else {
            GenerateXSLT.generateXSLT({});
        }
    });


}

function getSchedules(parameters, callback) {
    getApplicationSchedules(ApplaneCallback(callback, function (applicationSchedules) {
        var schedulesNoneOnApplication = getNoneSchedules(applicationSchedules, true);
        getOrganizationSchedules(schedulesNoneOnApplication, ApplaneCallback(callback, function (organizationSchedules) {
            var schedulesNoneOnOrganization = getNoneSchedules(organizationSchedules, false);
            getUserSchedules(schedulesNoneOnApplication, schedulesNoneOnOrganization, ApplaneCallback(callback, function (userSchedules) {
                var schedules = {};
                schedules.application = applicationSchedules || [];
                schedules.organization = organizationSchedules || [];
                schedules.user = userSchedules || [];
                callback(null, schedules);
            }));
        }));
    }));
}

function getNoneSchedules(schedules, splice) {
    var noneSchedules = [];
    if (schedules && schedules.length > 0) {
        for (var i = 0; i < schedules.length; i++) {
            var schedule = schedules[i];
            if (schedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.VISIBILITY] == Constants.AppsStudio.Views.Schedules.Visibility.NONE) {
                var scheduleId = schedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID];
                noneSchedules.push(scheduleId[APIConstants.Query._ID]);
                if (splice) {
                    noneSchedules.splice(i, 1);
                    i = i - 1;
                }
            }
        }
    }
    return noneSchedules;
}

function getApplications(applicationsMap, viewId, baasViewId, callback) {
    if (applicationsMap[viewId]) {
        callback(null, applicationsMap[viewId]);
    } else {
        var applications = [];
        if (baasViewId[Constants.Baas.Views.APPLICATION]) {
            var baasViewApplication = baasViewId[Constants.Baas.Views.APPLICATION];
            applications.push(baasViewApplication);
            applicationsMap[viewId] = applications;
            callback(null, applications);
        } else {
            var tableName = baasViewId[Constants.Baas.Views.TABLE][Constants.Baas.Tables.ID];
            MetadataProvider.getTable(tableName, ApplaneCallback(callback, function (tableData) {
                var tableApplications = tableData[Constants.Baas.Tables.APPLICATIONS];
                if (tableApplications && tableApplications.length > 0) {
                    for (var i = 0; i < tableApplications.length; i++) {
                        applications.push(tableApplications[i]);
                    }
                }
                applicationsMap[viewId] = applications;
                callback(null, applications);
            }));
        }
    }
}

function getUsersAndSendMail(applications, organizationId, userScheduleMap, schedule) {
    var applicationIds = [];
    for (var i = 0; i < applications.length; i++) {
        applicationIds.push(applications[APIConstants.Query._ID]);
    }
    var filter = {};
    filter[Constants.Baas.Users.APPLICATIONS + "." + APIConstants.Query._ID] = {$in:applicationIds};
    filter[Constants.Baas.Users.APPLICATIONS + "." + Constants.Baas.Users.Applications.ORGANIZATIONS + "." + APIConstants.Query._ID] = organizationId;
    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.USERS;
    query[APIConstants.Query.COLUMNS] = [APIConstants.Query._ID];
    query[APIConstants.Query.FILTER] = filter;
    DatabaseEngine.query({
        ask:Constants.AppsStudio.ASK,
        query:query,
        callback:function (err, result) {
            if (!err && result) {
                var users = result[APIConstants.Query.Result.DATA];
                var scheduleId = schedule[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID];
                if (users && users.length > 0) {
                    for (var i = 0; i < users.length; i++) {
                        var user = users[i];
                        if (!userScheduleMap[scheduleId[APIConstants.Query._ID] + "__" + organizationId + "__" + user[APIConstants.Query._ID]]) {
                            userScheduleMap[scheduleId[APIConstants.Query._ID] + "__" + organizationId + "__" + user[APIConstants.Query._ID]] = schedule;
                            sendMail(schedule);
                        }
                    }
                }
            }
        }
    })
}

function getUserSchedules(schedulesNoneOnApplication, schedulesNoneOnOrganization, callback) {
    for (var i = 0; i < schedulesNoneOnOrganization.length; i++) {
        if (schedulesNoneOnApplication.indexOf(schedulesNoneOnOrganization[i]) == -1) {
            schedulesNoneOnApplication.push(schedulesNoneOnOrganization[i]);
        }
    }
    var filter = {};
    filter[Constants.AppsStudio.Views.USERID] != null;
    filter[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID + "." + APIConstants.Query._ID ] = {$nin:schedulesNoneOnApplication};
    filter[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.WHEN + "." + Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE] = {$lte:new Date()};

    var query = {};
    query[APIConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
    query[APIConstants.Query.COLUMNS] = [Constants.AppsStudio.Views.ID, Constants.AppsStudio.Views.BAAS_VIEWID, Constants.AppsStudio.Views.USERID, Constants.AppsStudio.Views.ORGANIZATIONID, Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.WHEN, Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.VISIBILITY, Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID];
    query[APIConstants.Query.FILTER] = filter;
    console.log("query >>>>>>>>>>>>>" + JSON.stringify(query));
    DatabaseEngine.query({
        ask:Constants.AppsStudio.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            callback(null, result[APIConstants.Query.Result.DATA]);
        })
    })
}

function getOrganizationSchedules(schedulesNoneOnApplication, callback) {
    var filter = {};
    filter[Constants.AppsStudio.Views.USERID] = null;
    filter[Constants.AppsStudio.Views.ORGANIZATIONID] != null;
    filter[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID + "." + APIConstants.Query._ID ] = {$nin:schedulesNoneOnApplication};
    var orFilter = [];
    var noneVisibilityFilter = {};
    noneVisibilityFilter[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.VISIBILITY ] = "None";
    orFilter.push(noneVisibilityFilter);
    var lessDueDateFilter = {};
    lessDueDateFilter[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.WHEN + "." + Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE] = {$lte:new Date()};
    orFilter.push(lessDueDateFilter);
    filter.$or = orFilter;

    var query = {};
    query[APIConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
    query[APIConstants.Query.COLUMNS] = [Constants.AppsStudio.Views.ID, Constants.AppsStudio.Views.BAAS_VIEWID, Constants.AppsStudio.Views.ORGANIZATIONID, Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.WHEN, Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.VISIBILITY, Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID];
    query[APIConstants.Query.FILTER] = filter;
    DatabaseEngine.query({
        ask:Constants.AppsStudio.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            callback(null, result[APIConstants.Query.Result.DATA]);
        })
    })
}

function getApplicationSchedules(callback) {
    var filter = {};
    filter[Constants.AppsStudio.Views.USERID] = null;
    filter[Constants.AppsStudio.Views.ORGANIZATIONID] = null;
    var orFilter = [];
    var noneVisibilityFilter = {};
    noneVisibilityFilter[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.VISIBILITY ] = "None";
    orFilter.push(noneVisibilityFilter);
    var lessDueDateFilter = {};
    lessDueDateFilter[Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.WHEN + "." + Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE] = {$lte:new Date()};
    orFilter.push(lessDueDateFilter);
    filter.$or = orFilter;

    var query = {};
    query[APIConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
    query[APIConstants.Query.COLUMNS] = [Constants.AppsStudio.Views.ID, Constants.AppsStudio.Views.BAAS_VIEWID, Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.WHEN, Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.VISIBILITY, Constants.AppsStudio.Views.SCHEDULES + "." + Constants.AppsStudio.Views.Schedules.SCHEDULEID];
    query[APIConstants.Query.FILTER] = filter;
    DatabaseEngine.query({
        ask:Constants.AppsStudio.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            callback(null, result[APIConstants.Query.Result.DATA]);
        })
    })
}


this.scheduleNotifications({}, function (err, res) {
    if (err) {
        console.log(err);
    } else {
        console.log("success..........");
    }
})