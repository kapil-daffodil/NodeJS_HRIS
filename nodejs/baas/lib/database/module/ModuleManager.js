/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 8/1/13
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */
var QueryConstants = require("nodejsapi/Constants.js");
var CustomCodeManager = require("../../customcode/customcodemanager.js");
var Constants = require('../../shared/Constants.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var Utils = require("apputil/util.js");
var BaasError = require("apputil/ApplaneError.js");
var MongoDBManager = require("../mongodb/MongoDBManager.js");
var DatabaseEngine = require("../DatabaseEngine.js");

exports.beforeInsert = function (table, operations, context, callback) {
    callForOperations(table, operations, context, "beforeInsert", "before", "insert", callback);
}

exports.afterInsert = function (table, operations, context, callback) {
    callForOperations(table, operations, context, "afterInsert", "after", "insert", callback);
}

exports.beforeUpdate = function (table, operations, context, callback) {
    callForOperations(table, operations, context, "beforeUpdate", "before", "update", callback);
}

exports.afterUpdate = function (table, operations, context, callback) {
    callForOperations(table, operations, context, "afterUpdate", "after", "update", callback);
}

exports.beforeDelete = function (table, operations, context, callback) {
    callForOperations(table, operations, context, "beforeDelete", "before", "delete", callback);
}

exports.afterDelete = function (table, operations, context, callback) {
    callForOperations(table, operations, context, "afterDelete", "after", "delete", callback);
}

function callForOperations(table, operations, context, methodName, when, operation, callback) {
    var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
    Utils.iterateArrayWithIndex(enableModules, callback, function (index, module, callback) {
        var moduleObject = getModule(module[Constants.Baas.Modules.MODULE_PATH]);
        if (module[Constants.Baas.Modules.MODULE_PATH]) {
            if (moduleObject && moduleObject[methodName]) {
                moduleObject[methodName](table, operations, context, callback);
            } else {
                callback();
            }
        } else {
            var moduleType = module.type;
            if (moduleType) {
                if ((moduleType.indexOf("update") != 1 && when == 'before') || (moduleType.indexOf("transform") != 1 && when == 'after')) {
                    executeModuleCode(module, operations, context, when, operation, callback);
                } else {
                    callback();
                }
            } else {
                executeModuleCode(module, operations, context, when, operation, callback);
            }
        }
    });
}

exports.beforeQuery = function (table, query, context, callback) {
    var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
    Utils.iterateArray(enableModules, callback, function (module, callback) {
        var moduleObject = getModule(module[Constants.Baas.Modules.MODULE_PATH]);
        if (module[Constants.Baas.Modules.MODULE_PATH]) {
            if (moduleObject && moduleObject.beforeQuery) {
                moduleObject.beforeQuery(table, query, context, callback);
            } else {
                callback();
            }
        } else {
            var moduleType = module.type;
            if (moduleType) {
                if (moduleType.indexOf("command query") != -1) {
                    executeModuleQueryJob(module, table, query, context, "before", "query", callback);
                } else {
                    callback();
                }
            } else {
                executeModuleQueryJob(module, table, query, context, "before", "query", callback);
            }
        }
    });
}

exports.afterQueryResult = function (table, query, result, context, callback) {
    var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
    Utils.iterateArray(enableModules, callback, function (module, callback) {
        var moduleObject = getModule(module[Constants.Baas.Modules.MODULE_PATH]);
        if (module[Constants.Baas.Modules.MODULE_PATH]) {
            if (moduleObject && moduleObject.afterQueryResult) {
                moduleObject.afterQueryResult(table, query, result, context, callback);
            } else {
                callback();
            }
        } else {
            var moduleType = module.type;
            if (moduleType) {
                if (moduleType.indexOf("query") != -1) {
                    executeModuleQueryJob(module, table, query, result, context, "after", "query", callback);
                } else {
                    callback();
                }
            } else {
                executeModuleQueryJob(module, table, query, result, context, "after", "query", callback);
            }
        }
    });
}

exports.syncTable = function (table, callback) {
    var orderBy = {};
    orderBy[Constants.Baas.Modules.INDEX] = 1;
    MongoDBManager.find(Constants.Baas.BAASDB, Constants.Baas.MODULES, {}, {sort:orderBy}, ApplaneCallback(callback, function (modules) {
        Utils.iterateArray(modules, callback, function (module, callback) {
            var moduleObject = getModule(module[Constants.Baas.Modules.MODULE_PATH]);
//            if (module[Constants.Baas.Modules.MODULE_PATH]) {
            if (moduleObject && moduleObject.syncTable) {
                moduleObject.syncTable(table, callback);
            } else {
                callback();
            }
//            }
//            } else {
//                executeConfirmModuleCode(module, table, oldTable, "after", "update", callback);
//            }

        });
    }));
}


function getModule(modulepath) {
    if (!modulepath) {
        return null;
    }
    try {
        return require(modulepath);
    } catch (err) {
        if (err.code == 'MODULE_NOT_FOUND') {
//            throw new BaasError(Constants.ErrorCode.MODULE_NOT_FOUND, [modulepath]);
            return false;
        } else {
            throw err;
        }
    }
}


executeModuleCode = function (module, operations, context, when, operation, callback) {
    var moduleParametersSet = module.moduleparameterset;
    var parameters = [];
    if (moduleParametersSet) {
        var noOfParameters = moduleParametersSet.length;
        Utils.iterateArray(moduleParametersSet, callback, function (parameterSet, callback) {
            var values = parameterSet.moduleparametervalues;
            if (values) {
                var params = {};
                for (var i = 0; i < values.length; i++) {
                    var value = values[i];
                    params[value.name] = value.value;
                }
                runJobModule(module, operations, context, when, operation, params, callback);
            } else {
                callback();
            }
        });
    }
    else {
        runJobModule(module, operations, context, when, operation, params, callback);
    }
}

getModuleData = function (_id, callback) {
    var filter = {};
    filter._id = _id;
    var query = {};
    query[QueryConstants.Query.TABLE] = "modules__baas";
    query[QueryConstants.Query.FILTER] = filter;
    DatabaseEngine.query({
        "ask":"baas",
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[QueryConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data[0]);
            } else {
                callback();
            }
        })
    })
}


executeModuleQueryJob = function (module, table, query, result, context, when, operation, callback) {
    var moduleParametersSet = module.moduleparameterset;
    if (moduleParametersSet) {
        var noOfParameters = moduleParametersSet.length;
        Utils.iterateArray(moduleParametersSet, callback, function (parameterSet, callback) {
            var values = parameterSet.moduleparametervalues;
            if (values) {
                var params = {};
                for (var i = 0; i < values.length; i++) {
                    var value = values[i];
                    params[value.name] = value.value;
                }
                runQueryModule(module, table, query, result, context, when, operation, params, callback);
            } else {
                callback();
            }
        });
    }
    else {
        runQueryModule(module, table, query, context, when, operation, null, callback);
    }
}


executeConfirmModuleCode = function (module, table, oldTable, when, operation, callback) {
    var moduleParametersSet = module.moduleparameterset;
    if (moduleParametersSet) {
        var noOfParameters = moduleParametersSet.length;
        Utils.iterateArray(moduleParametersSet, callback, function (parameterSet, callback) {
            var values = parameterSet.moduleparametervalues;
            if (values) {
                var params = {};
                for (var i = 0; i < values.length; i++) {
                    var value = values[i];
                    params[value.name] = value.value;
                }
                runConfigureModule(module, table, oldTable, when, operation, params, callback);
            }   else{
                callback();
            }
        });
    } else {
        runConfigureModule(module, table, oldTable, when, operation, null, callback);
    }
}

runJobModule = function (module, operations, context, when, operation, params, callback) {
    getModuleData(module._id, ApplaneCallback(callback, function (result) {
        var logics = result.logic;
        Utils.iterateArray(logics, callback, function (logic, callback) {
            var logicType = logic.type;
            if (logicType && ((logicType == "transform" && when == "after") || (logicType == "update" && when == "before"))) {
                CustomCodeManager.callJobs({
                    "ask":context[QueryConstants.Query.ASK],
                    "osk":context[QueryConstants.Query.OSK],
                    "usk":context.usk,
                    "logid":context.logid,
                    "user":context.user,
                    "code":logic.code,
                    "when":when,
                    "method":when + Utils.capitalize(operation),
                    "operation":operation,
                    "params":params,
                    "parameters":{
                        "operations":operations,
                        "oldvalues":context.oldvalues,
                        "newvalues":context.newvalues
                    },
                    "callback":ApplaneCallback(callback, function (params) {
                        operations = params.operations;
                        callback();
                    })
                });
            }
            else {
                callback();
            }
        })
    }));

}

runConfigureModule = function (module, table, oldTable, when, operation, params, callback) {
    getModuleData(module._id, ApplaneCallback(callback, function (result) {
        var logics = result.logic;
        Utils.iterateArray(logics, callback, function (logic, callback) {
            var logicType = logic.type;
            if (logicType && (logicType == "configure" && when == "after")) {
                CustomCodeManager.callJobs({
                    "ask":"baas",
                    "mainask":table[Constants.Baas.Tables.MAINAPPLICATION][Constants.Baas.Applications.ASK],
                    "osk":"baas",
                    "code":logic.code,
                    "when":when,
                    "method":when + Utils.capitalize(operation),
                    "operation":operation,
                    "params":params,
                    "table":table,
                    "parameters":{
                        "operations":table,
                        "oldvalues":oldTable,
                        "newvalues":context.newvalues
                    },
                    "callback":ApplaneCallback(callback, function (params) {
                        operations = params.operations;
                        callback();
                    })
                });
            }
            else {
                callback();
            }
        })
    }));
}

runQueryModule = function (module, table, query, result, context, when, operation, params, callback) {
    getModuleData(module._id, ApplaneCallback(callback, function (moduleData) {
        var logics = moduleData.logic;
        Utils.iterateArray(logics, callback, function (logic, callback) {
            var logicType = logic.type;
            if (logicType && ((logicType == "query" && when == "after") || (logicType == "command query" && when == "before"))) {
                CustomCodeManager.callJobs({
                    "ask":context[QueryConstants.Query.ASK],
                    "mainask":table[Constants.Baas.Tables.MAINAPPLICATION][Constants.Baas.Applications.ASK],
                    "osk":context[QueryConstants.Query.OSK],
                    "usk":context.usk,
                    "logid":context.logid,
                    "user":context.user,
                    "params":parameters,
                    "table":table,
                    "code":logic.code,
                    "when":when,
                    "operation":operation,
                    "method":when + Utils.capitalize(Constants.Baas.Tables.Jobs.Operation.QUERY),
                    "parameters":{
                        query:query,
                        result:result
                    },
                    "callback":ApplaneCallback(callback, function (params) {
                        query = params.query;
                        result = params.result;
                        callback();
                    })
                });
            }
            else {
                callback();
            }
        })
    }));
}