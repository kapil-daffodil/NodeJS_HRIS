var MetadataProvider = require('../metadata/MetadataProvider.js');
var DataBaseEngine = require('../database/DatabaseEngine.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var Constants = require('../shared/Constants.js');
var DataBaseEngine = require('../database/DatabaseEngine.js');
var UpdateEngine = require('../database/UpdateEngine.js');
var APIConstants = require("nodejsapi/Constants.js");
var UserService = require('../user/userservice.js');
var MongoDBManager = require("../database/mongodb/MongoDBManager.js");
var objectID = require("mongodb").ObjectID;
var Self = require("./LogUtility.js");

var http = require('http');
var urlParser = require('url');

var logs = {};

exports.getLogs = function (logid) {
    return logs[logid];
}

exports.removeLogs = function (logid) {
    delete logs[logid];
}


exports.checkLog = function (req, user, callback) {
    var logid = req.param("logid");
    if (logid) {
        callback(null, logid, false);
        return;
    }
    var ask = req.param("ask");
    var osk = req.param("osk");
    if (!ask) {
        callback();
        return;
    }
    MetadataProvider.getApplication(ask, ApplaneCallback(callback, function (application) {
        if (req.param(Constants.Baas.Applications.ENABLE_LOGS)) {
            var applicationId = application[APIConstants.Query._ID];
            var url = urlParser.parse(req.url, true);
            var uri = url.pathname;
            var domain = req.headers.host;
            var queryObj = url.query;
            if (osk) {
                MetadataProvider.getOrganization(osk, ApplaneCallback(callback, function (organization) {
                    var organizationId = organization[APIConstants.Query._ID];
                    Self.addLog(applicationId, organizationId, user, uri, domain, queryObj, callback);
                }))
            } else {
                Self.addLog(applicationId, null, user, uri, domain, queryObj, callback);
            }
        } else {
            callback();
        }
    }));
}

exports.addLog = function (applicationId, organizationId, user, uri, domain, params, callback) {
//    var updates = {};
//    updates.table = Constants.Baas.LOGS;
//    var operations = [];
    var operation = {};
    operation[Constants.Baas.Logs.USERID] = user ? user[APIConstants.Query._ID] : null;
    operation[Constants.Baas.Logs.ORGANIZATIONID] = organizationId;
    operation[Constants.Baas.Logs.APPLICATIONID] = applicationId;
    operation[Constants.Baas.Logs.DATE] = new Date().toString();
    operation[Constants.Baas.Logs.PARAMS] = JSON.stringify(params);
    operation[Constants.Baas.Logs.URI] = uri;
    operation[Constants.Baas.Logs.DOMAIN] = domain;
    operation[Constants.Baas.Logs.LOGS] = [];
//    operations.push(operation);
//    updates.operations = operations;
//
//    UpdateEngine.performUpdate({
//        ask:Constants.Baas.ASK,
//        updates:updates,
//        callback:ApplaneCallback(callback, function (result) {
//            callback(null, result[APIConstants.Update.Operation.Type.INSERT][0][APIConstants.Query._ID]);
//        })
//    });
    var logid = new objectID().toString();
    logs[logid] = operation;
    callback(null, logid, true);
}

exports.writeLog = function (logid, log) {
    if (logid && logs[logid]) {
//        var updates = {};
//        var pushObject = {};
        var logObj = {};
        logObj[Constants.Baas.Logs.Logs.LOG] = log;
        logObj[Constants.Baas.Logs.Logs.LOGDATE] = new Date();
//        pushObject [Constants.Baas.Logs.LOGS] = logObj;
//        updates.$push = pushObject;
//
//        var query = {};
//        query[APIConstants.Query._ID] = new objectID(logid.toString());
//
//        MongoDBManager.update(Constants.Baas.BAASDB, Constants.Baas.LOGS, query, updates, function (err, result) {
//            if (err) {
//                console.log("Problem while inseting logs>>>>" + err.stack);
//            }
//        })
        var logEntry = logs[logid];
        logEntry[Constants.Baas.Logs.LOGS].push(logObj);
    }
}
