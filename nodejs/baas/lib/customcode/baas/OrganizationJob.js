/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 14/8/13
 * Time: 10:44 AM
 * To change this template use File | Settings | File Templates.
 */

var Utils = require("apputil/util.js");
var Constants = require('../../shared/Constants.js');
var ObjectID = require('mongodb').ObjectID;
var CacheService = require("../../cache/cacheservice.js");
var NodejsAPI = require("nodejsapi");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var APIConstants = require('nodejsapi/Constants.js');
var DataBaseEngine = require('../../database/DatabaseEngine.js');

exports.beforeInsert = function (params, callback) {
    Utils.iterateArray(params.operations, callback, function (organization, callback) {
        if (!organization[Constants.Baas.Organizations.OSK]) {
            organization[Constants.Baas.Organizations.OSK] = new ObjectID().toString();
        }
        callback();
    });
}

exports.beforeUpdate = function (params, callback) {
    Utils.iterateArray(params.operations, callback, function (organization, callback) {
        var cacheKey = ["Organizations", organization[Constants.Baas.Organizations.OSK]];
        CacheService.clear(cacheKey);
        callback();
    });
}

exports.afterInsert = function (params, callback) {
    Utils.iterateArrayWithIndex(params.operations, callback, function (index, organization, callback) {
        if (organization[Constants.Baas.Organizations.ADMINS]) {
            addAdminAsUser(params, params.newvalues[index], callback);
        } else {
            callback();
        }
    });
}

exports.afterUpdate = function (params, callback) {
    Utils.iterateArrayWithIndex(params.operations, callback, function (index, organization, callback) {
        if (organization[Constants.Baas.Organizations.ADMINS] || organization[Constants.Baas.Organizations.APPLICATIONS]) {
            addAdminAsUser(params, params.newvalues[index], callback);
        } else {
            callback();
        }
    });
}

function addAdminAsUser(params, organization, callback) {
    var admins = organization[Constants.Baas.Organizations.ADMINS];
    var orgApplications = organization[Constants.Baas.Organizations.APPLICATIONS];
    if (admins && orgApplications && orgApplications.length > 0) {
        var update = NodejsAPI.asUpdate(Constants.Baas.USERS, {ask:params.ask, osk:params.osk, usk:params.usk});
        admins.forEach(function (admin) {
            var operation = {};
            operation[APIConstants.Query._ID] = admin[APIConstants.Query._ID];
            var applicationUpdates = [];
            orgApplications.forEach(function (application) {
                var applicationUpdate = {};
                applicationUpdate[APIConstants.Query._ID] = application[APIConstants.Query._ID];
                var organizationUpdate = {};
                organizationUpdate[APIConstants.Query._ID] = organization[APIConstants.Query._ID];
                applicationUpdate[Constants.Baas.Users.Applications.ORGANIZATIONS] = [organizationUpdate];
                applicationUpdates.push(applicationUpdate);
            });
            operation[Constants.Baas.Users.APPLICATIONS] = applicationUpdates;
            update.update(admin[APIConstants.Query._ID], operation);
        });
        update.save(callback);
    } else {
        callback();
    }
}