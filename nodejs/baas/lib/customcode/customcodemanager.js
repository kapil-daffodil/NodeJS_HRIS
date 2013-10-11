var Constants = require("../shared/Constants.js");
var APIConstants = require('nodejsapi/Constants.js');
var MetaDataProvider = require("../metadata/MetadataProvider.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var BaasError = require("apputil/ApplaneError.js");
var http = require("http");
var cacheService = require("../cache/cacheservice.js");
var QueryString = require("querystring");
var MailService = require("../mail/MailService.js");
var LogUtility = require("../util/LogUtility.js");
var DataBaseEngine = require('../database/DatabaseEngine.js');
var UpdateEngine = require('../database/UpdateEngine.js');
var Self = require('./customcodemanager.js');
var Utils = require("apputil/util.js");

exports.callModule = function (options) {
    options.asmodule = true;
    execute(options);
}

exports.callService = function (options) {
    options.asservice = true;
    execute(options);
}

exports.callJobs = function (options) {

    options.asjob = true;
    if (options.code) {
        executeCode(options);
    } else {
        execute(options);
    }
}

executeCode = function (options) {
    var when = options.when;
    if (options.operation == Constants.Baas.Tables.Jobs.Operation.QUERY) {
        executeQueryJob(options);
    } else {
        if (when == 'before') {
            executeBeforeJob(options, true);
        } else {
            setTimeout(function () {
                executeAfterJob(options);
            }, 1000);
            options.callback(null, {operations:options.parameters.operations});
        }
    }
}

function executeBeforeJob(options) {
    var code = options.code;
    var operations = options.parameters.operations;
    var length = operations.length;
    for (var i = 0; i < length; i++) {
        var operation = operations[i];
        beforeJob(options, operation, options.parameters.oldvalues[i], options.parameters.newvalues[i]);
    }
    options.callback(null, {operations:operations});
}

function executeAfterJob(options) {
    try {
        var code = options.code;
        var operations = options.parameters.operations;
        var length = operations.length;
        for (var i = 0; i < length; i++) {
            var operation = operations[i];
            afterJob(options, operation, options.parameters.oldvalues ? options.parameters.oldvalues[i] : null, options.parameters.newvalues ? options.parameters.newvalues[i] : null);
        }
    } catch (err) {
        console.log("Error while after job" + err);
        console.log(err.stack);
    }
}

beforeJob = function (options, operation, oldValue, newValues) {
    var method = options.method;
    if (method == 'beforeInsert') {
        new beforeInsert(options, operation);
    }
    if (method == 'beforeUpdate') {
        new beforeUpdate(options, oldValue, operation);
    }
    if (method == 'beforeDelete') {
        new beforeDelete(options, operation, oldValue);
    }
}

afterJob = function (options, operation, oldValue, newValues) {

    var method = options.method;
    if (method == 'afterInsert') {
        new afterInsert(options, operation, newValues);
    }
    if (method == 'afterUpdate') {
        new afterUpdate(options, operation, newValues, oldValue);
    }
    if (method == 'afterDelete') {
        new afterDelete(options, oldValue);
    }
}

var writeLog = function (options) {
    return function (log) {
        console.log("Job Log>>>>>>>" + JSON.stringify(log));
        LogUtility.writeLog(options.logid, log);
    }
}

var sendMail = function (options) {
    return function (mailcontents) {
        MailService.sendMail(mailcontents, {application:options.application, oraganization:options.organization});
    }
}


beforeInsert = function (options, operations) {
    for (key in operations) {
        this[key] = operations[key];
    }
    this._context = JSON.parse(JSON.stringify(options));
    if (options.operation) {
        this._operation = options.operation;
    }
    Function("JobError", "writeLog", options.code).apply(this, [BaasError, writeLog(options)]);
    for (key in this) {
        if (key != '_old' && key != '_context') {
            operations[key] = this[key];
        }
    }
}
afterInsert = function (options, operations, newValues) {
    for (key in newValues) {
        this[key] = newValues[key];
    }
    if (options.operation) {
        this._operation = options.operation;
    }
    this._context = JSON.parse(JSON.stringify(options));
    var updates = Function("JobError", "sendMail", "writeLog", options.code).apply(this, [BaasError, sendMail(options), writeLog(options)]);
    Self.executeUpdates(updates, options);
}


beforeUpdate = function (options, oldValues, operations) {
    var oldValueClone = JSON.parse(JSON.stringify(oldValues));
    merge(oldValueClone, operations);
    for (key in oldValueClone) {
        this[key] = oldValueClone[key];
    }
    if (options.operation) {
        this._operation = options.operation;
    }
    this._context = JSON.parse(JSON.stringify(options));
    if (oldValues) {
        this._old = JSON.parse(JSON.stringify(oldValues));
    }
    Function("JobError", "writeLog", options.code).apply(this, [BaasError, writeLog(options)]);
    mergeToOperation(this, operations);
}

afterUpdate = function (options, operations, newValues, oldValues) {
    for (key in newValues) {
        this[key] = newValues[key];
    }
    if (options.operation) {
        this._operation = options.operation;
    }
    this._context = JSON.parse(JSON.stringify(options));
    if (oldValues) {
        this._old = JSON.parse(JSON.stringify(oldValues));
    }
    var updates = Function("JobError", "sendMail", "writeLog", options.code).apply(this, [BaasError, sendMail(options), writeLog(options)]);
    Self.executeUpdates(updates, options);
}

beforeDelete = function (options, operations, oldValues) {
    for (key in oldValues) {
        this[key] = oldValues[key];
    }
    if (options.operation) {
        this._operation = options.operation;
    }
    this._context = JSON.parse(JSON.stringify(options));
    Function("JobError", "writeLog", options.code).apply(this, [BaasError, writeLog(options)]);
}

afterDelete = function (options, oldValues) {
    for (key in oldValues) {
        this[key] = oldValues[key];
    }
    if (options.operation) {
        this._operation = options.operation;
    }
    this._context = JSON.parse(JSON.stringify(options));
    var updates = Function("JobError", "sendMail", "writeLog", options.code).apply(this, [BaasError, sendMail(options), writeLog(options)]);
    Self.executeUpdates(updates, options);
}

exports.executeUpdates = function (updates, options, callback) {
    if (updates) {
        callback = callback || function (err) {
            if (err) {
                console.log("Error while save after upates " + err.stack);
            }
        };
        var keys = Object.keys(updates);
        Utils.iterateArray(keys, callback, function (key, callback) {
            var tableName = key;
            var update = updates[key];
            if (update instanceof Array) {
                Utils.iterateArray(update, callback, function (innerUpdate, callback) {
                    executeAfterUpdates(innerUpdate, tableName, options, callback);
                });
            } else if (update instanceof Object) {
                executeAfterUpdates(update, tableName, options, callback);
            }
        });
    }
}
executeAfterUpdates = function (update, tableName, options, callback) {
    var id = update.id;
    var value = update.value;
    var type = update.type;
    if (id) {
        getId(id, tableName, options, ApplaneCallback(callback, function (_id) {
            if (_id) {
                if (type && type.toString() == 'delete') {
                    deleteQuery(_id, tableName, options, callback);
                } else {
                    updateQuery(_id, tableName, value, options, callback);
                }
            }
            else {
                if (!(type && type.toString() == 'update')) {
                    insertQuery(id, value, tableName, options, callback);
                } else {
                    callback();
                }
            }
        }));
    } else {
        insertQuery(null, value, tableName, options, callback);
    }
}


insertQuery = function (id, value, tableName, options, callback) {
    if (id) {
        for (var key in id) {
            value[key] = id[key];
        }
    }
    if (value) {
        for (var key in value) {
            if (key == '$inc') {
                var incValue = value[key];
                delete value[key];
                for (var newKey in incValue) {
                    value[newKey] = incValue[newKey];
                }
            }
        }
    }
    var operations = [];
    operations.push(value);
    var newUpdates = {};
    newUpdates[APIConstants.Query.TABLE] = tableName;
    newUpdates[APIConstants.Update.OPERATIONS] = operations;
    UpdateEngine.performUpdate({
        "ask":options[APIConstants.Query.ASK],
        "osk":options[APIConstants.Query.OSK],
        "usk":options.usk,
        "logid":options.logid,
        "user":options.user,
        updates:newUpdates,
        callback:callback
    })

}
updateQuery = function (_id, tableName, value, options, callback) {
    value[APIConstants.Query._ID] = _id;
    var operations = [];
    operations.push(value);
    var newUpdates = {};
    newUpdates[APIConstants.Query.TABLE] = tableName;
    newUpdates[APIConstants.Update.OPERATIONS] = operations;
    UpdateEngine.performUpdate({
        "ask":options[APIConstants.Query.ASK],
        "osk":options[APIConstants.Query.OSK],
        "usk":options.usk,
        "logid":options.logid,
        "user":options.user,
        updates:newUpdates,
        callback:callback
    })
}
deleteQuery = function (_id, tableName, options, callback) {
    var value = {};
    value[APIConstants.Query._ID] = _id;
    value[APIConstants.Update.Operation.TYPE] = APIConstants.Update.Operation.Type.DELETE;
    var operations = [];
    operations.push(value);
    var newUpdates = {};
    newUpdates[APIConstants.Query.TABLE] = tableName;
    newUpdates[APIConstants.Update.OPERATIONS] = operations;
    UpdateEngine.performUpdate({
        "ask":options[APIConstants.Query.ASK],
        "osk":options[APIConstants.Query.OSK],
        "usk":options.usk,
        "logid":options.logid,
        "user":options.user,
        updates:newUpdates,
        callback:callback
    })
}

getId = function (id, tableName, options, callback) {
    var query = {};
    query[APIConstants.Query.TABLE] = tableName;
    query[APIConstants.Query.COLUMNS] = [APIConstants.Query._ID];
    query[APIConstants.Query.FILTER] = id;
    DataBaseEngine.query({
        "ask":options[APIConstants.Query.ASK],
        "osk":options[APIConstants.Query.OSK],
        "usk":options.usk,
        "logid":options.logid,
        "user":options.user,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data[0][APIConstants.Query._ID]);

            } else {
                callback();
            }
        })
    })
}


function mergeToOperation(object, operations) {
    var inc = object.$inc;
    for (key in object) {
        if ((!inc || !inc[key]) && key != '_old' && key != '_context') {
            var opValue = operations[key];
            var objectValue = object[key];
            if (opValue instanceof Array && opValue.length > 0 && opValue[0] instanceof Object && Object.keys(opValue[0]).length > 0) {
                var length = opValue.length;
                for (var i = 0; i < length; i++) {
                    var obj = opValue[i];
                    var id = obj._id;
                    if (id) {
                        if (obj.__type__ == "delete") {
                            objectValue.push(obj);
                        }
                    }
                }
            }
            if (opValue instanceof Object) {
                if (opValue.override == "true") {
                    var newData = {};
                    newData.data = opValue.data;
                    newData.override = true;
                    objectValue = newData;
                }
            }
            operations[key] = objectValue;
        }
    }
}

function merge(oldValueClone, opertion) {
    for (var key in  opertion) {
        var value = opertion[key];
        if (value instanceof Array && value.length > 0 && value[0] instanceof Object && Object.keys(value[0]).length > 0) {
            var oldValue = oldValueClone[key];
            if (!oldValue) {
                oldValueClone[key] = value;
            } else {
                var length = value.length;
                for (var i = 0; i < length; i++) {
                    var obj = value[i];
                    var id = obj._id;
                    var found = false;
                    if (id) {
                        for (var j = 0; j < oldValue.length; j++) {
                            var obj1 = oldValue [j];
                            var oldId = obj1._id;
                            if (oldId && id.toString() == oldId.toString()) {
                                if (obj.__type__ == "delete") {
                                    oldValue.splice(j, 1);
                                    j = j - 1;
                                } else {
                                    merge(obj1, obj);
                                }
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        oldValue.push(obj);
                    }
                }
            }
        } else if (value instanceof Object && Object.keys(value).length > 0) {
            if (value.override == "true") {
                var newData = value.data;
                oldValueClone[key] = newData;
            } else {
                if (!oldValueClone[key]) {
                    oldValueClone[key] = value;
                } else {
                    merge(oldValueClone[key], opertion[key])
                }
            }
        }
        else {
            if (oldValueClone) {
                oldValueClone[key] = opertion[key];
            }
        }
    }
    return oldValueClone;
}


execute = function (options) {
    var usk = options.usk;
    var ask = options.ask;
    var mainAsk = options.mainask || options.ask;
    MetaDataProvider.getApplication(mainAsk, ApplaneCallback(options.callback, function (application) {
        var appId = application[Constants.Baas.Applications.ID];
//        var customcodeurl = application[Constants.Baas.Applications.CUSTOMCODEURL];
        var module = options[Constants.Baas.Tables.Jobs.MODULE];
//        var index = customcodeurl.indexOf(":");
//        var port = customcodeurl.substr(index + 1);
//        customcodeurl = customcodeurl.substr(0, index);

        var method = options.method;
        var parameters = options.parameters;

        parameters.ask = ask;
        parameters.osk = options.osk;
        parameters.usk = usk;
        if (usk) {
            var user = cacheService.get(usk);
            if (user) {
                parameters.user = user;
            }
        }

        var callback = options.callback;
        var modulePath = "./" + appId + "/" + module;
        var moduleObject = require(modulePath);
        if (options.asservice) {
            moduleObject[method](parameters);
            callback();
        } else {
            moduleObject[method](parameters, ApplaneCallback(callback, function (result) {
                if (options.asjob) {
                    result = parameters;
                }
                callback(null, result);

            }));
        }


//        var path = "/";
//        if (options.asmodule) {
//            path += "module";
//        } else if (options.asservice) {
//            path += "service";
//        } else {
//            path += "job";
//        }
//        var queryString = {ask:ask, module:module, method:method, parameters:JSON.stringify(parameters), appid:appId};
//
//
//        if (usk) {
//            var user = cacheService.get(usk);
//            if (user) {
//                queryString.usk = usk;
//                queryString.user = JSON.stringify(user);
//            }
//        }
//        if (options.osk) {
//            queryString.osk = options.osk;
//        }
//
//        var qs = QueryString.stringify(queryString);
//
//        var serverOptions = {
//            hostname:customcodeurl,
//            port:port,
//            path:path,
//            method:"POST",
//            headers:{
//                'Content-Type':'application/x-www-form-urlencoded',
//                'Content-Length':qs.length
//            }
//        };
//        console.log(".........Custom code...... module[" + module + "], method[" + method + "], host [" + customcodeurl + "]");
//        var req = http.request(serverOptions, function (res) {
//            res.setEncoding('utf8');
//            var body = '';
//            res.on('data', function (chunk) {
//                body += chunk;
//            });
//
//            res.on('end', function () {
//                try {
//                    response = JSON.parse(body);
//                    if (response.status == "error") {
//                        var error = new BaasError({message:response.response, code:response.code})
//                        if (response.stack) {
//                            error.stack = response.stack;
//                        }
//                        throw error;
//                    }
//                    console.log(".........Custom code callback received...... module[" + module + "], method[" + method + "], host [" + customcodeurl + "]");
//                    options.callback(null, response.response);
//                } catch (err) {
//                    options.callback(err);
//                }
//            });
//        });
//
//        req.on('error', function (err) {
//            options.callback(err);
//        });
//        req.write(qs);
//        req.end();
    }));
}

executeQueryJob = function (options) {
    if (options.when == Constants.Baas.Tables.Jobs.When.BEFORE) {
        new function () {
            this._query = options.parameters.query;
            this._context = options;
            Function("JobError", "writeLog", options.code).apply(this, [BaasError, writeLog(options)]);
        }
    } else {
        var data = options.parameters.result.data;
        data.forEach(function (row) {
            new function () {
                for (key in row) {
                    this[key] = row[key];
                }
                this._context = options;
                Function("JobError", "writeLog", options.code).apply(this, [BaasError, writeLog(options)]);
                for (key in this) {
                    if (key != "_context") {
                        row[key] = this[key];
                    }
                }
            }
        })
    }
    options.callback(null, {query:options.parameters.query, result:options.parameters.result});
}