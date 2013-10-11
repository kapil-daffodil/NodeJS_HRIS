/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 8/2/13
 * Time: 6:16 PM
 * To change this template use File | Settings | File Templates.
 */

var Utils = require("apputil/util.js");
var Constants = require('../../shared/Constants.js');
var ObjectID = require('mongodb').ObjectID;
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var APIConstants = require('nodejsapi/Constants.js');
var NodejsAPI = require('nodejsapi');
var CacheService = require("../../cache/cacheservice.js");
var DatabaseEngine = require("../../database/DatabaseEngine.js");
var UpdateEngine = require("../../database/UpdateEngine.js");

exports.beforeInsert = function (params, callback) {
    Utils.iterateArray(params.operations, callback, function (application, callback) {
        //check if application already exists
        var appId = application[Constants.Baas.Applications.ID];
        DatabaseEngine.query({
            ask:"baas",
            query:{"table":"applications__baas", "filter":{"id":appId}},
            callback:ApplaneCallback(callback, function (applicationInfo) {
                if (applicationInfo && applicationInfo.data && applicationInfo.data.length > 0) {
                    throw new Error("Application already exists[" + appId + "]");
                }
                if (!application[Constants.Baas.Applications.ASK]) {
                    application[Constants.Baas.Applications.ASK] = new ObjectID().toString();
                }
                if (!application[Constants.Baas.Applications.DEVELOPERS + "." + Constants.Baas.Applications.Developers.ROLE]) {
                    application[Constants.Baas.Applications.DEVELOPERS + "." + Constants.Baas.Applications.Developers.ROLE] = "admin";
                }
                if (application[Constants.Baas.Applications.ORGENABLED]) {
                    application[Constants.Baas.Applications.ORGANIZATIONS__BAAS__APPLICATIONS] = [Constants.Baas.Organizations.GLOBAL_ORGANIZATION];
                }
                callback();
            })
        })
    });
}

exports.beforeUpdate = function (params, callback) {
    var oldValues = params.oldvalues;
    Utils.iterateArrayWithIndex(params.operations, callback, function (index, application, callback) {
        var oldValue = oldValues[index];
        if (application[Constants.Baas.Applications.ID] && application[Constants.Baas.Applications.ID] != oldValue[Constants.Baas.Applications.ID]) {
            throw new Error("Applicationid for Application [" + oldValue[Constants.Baas.Applications.ID] + "] can't be changed");
        }
        var cacheKey = ["Applications", application[Constants.Baas.Applications.ASK]];
        CacheService.clear(cacheKey);
        callback();
    });
}

exports.afterInsert = function (params, callback) {
    var newValues = params.newvalues;
    Utils.iterateArrayWithIndex(params.operations, callback, function (index, application, callback) {
        var newValue = newValues[index];
        if (application[Constants.Baas.Applications.DEVELOPERS]) {
            addDeveloperAsUser(params, newValue, callback);
        } else {
            callback();
        }
    });
}

exports.afterUpdate = function (params, callback) {
    var oldValues = params.oldvalues;
    var newValues = params.newvalues;
    Utils.iterateArrayWithIndex(params.operations, callback, function (index, application, callback) {
        var oldValue = oldValues[index];
        var newValue = newValues[index];
        var applicationDevelopers = application[Constants.Baas.Applications.DEVELOPERS];
        var add = false;
        if (applicationDevelopers) {
            var newApplicationDevelopers = newValue[Constants.Baas.Applications.DEVELOPERS];
            var oldApplicationDevelopers = oldValue[Constants.Baas.Applications.DEVELOPERS];
            var oldValueDevelopersArray = [];
            if (oldApplicationDevelopers && oldApplicationDevelopers.length > 0) {
                for (var i = 0; i < oldApplicationDevelopers.length; i++) {
                    var oldApplicationDeveloper = oldApplicationDevelopers[i];
                    oldValueDevelopersArray.push(oldApplicationDeveloper[APIConstants.Query._ID].toString());
                }
            }
            for (var i = 0; i < newApplicationDevelopers.length; i++) {
                var applicationDeveloper = newApplicationDevelopers[i];
                var applicationDeveloperId = applicationDeveloper[APIConstants.Query._ID];
                if (oldValueDevelopersArray.indexOf(applicationDeveloperId.toString()) == -1) {
                    add = true;
                    break;
                } else {
                    oldValueDevelopersArray.splice(oldValueDevelopersArray.indexOf(applicationDeveloperId.toString()), 1);
                }
            }
            if (!add && oldValueDevelopersArray.length > 0) {
                add = true;
            }
        }
        if (add) {
            addDeveloperAsUser(params, newValue, callback);
        } else {
            callback();
        }
    });
}

function addDeveloperAsUser(params, application, callback) {
    var developers = application[Constants.Baas.Applications.DEVELOPERS];
    if (developers) {
        var update = NodejsAPI.asUpdate(Constants.Baas.USERS, {ask:params.ask, osk:params.osk, usk:params.usk});
        developers.forEach(function (developer) {
            var operation = {};
            operation[APIConstants.Query._ID] = developer[APIConstants.Query._ID];
            var applicationUpdate = {};
            applicationUpdate[APIConstants.Query._ID] = application[APIConstants.Query._ID];
            if (application[Constants.Baas.Applications.ORGENABLED]) {
                applicationUpdate[Constants.Baas.Users.Applications.ORGANIZATIONS] = [Constants.Baas.Organizations.GLOBAL_ORGANIZATION];
            }
            operation[Constants.Baas.Users.APPLICATIONS] = [applicationUpdate];
            update.update(developer[APIConstants.Query._ID], operation);
        });
        update.save(callback);
    } else {
        callback();
    }
}