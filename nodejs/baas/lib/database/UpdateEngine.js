var MongoDB = require('mongodb');
var mongoDBManager = require('./mongodb/MongoDBManager.js');
var Constants = require("../shared/Constants.js");
var QueryConstants = require("nodejsapi/Constants.js");
var metaDataProvider = require("../metadata/MetadataProvider.js");
var BaasError = require("apputil/ApplaneError.js");
var util = require("util");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var FilterUtil = require("../util/filterutil.js");

var ObjectID = MongoDB.ObjectID;
var apputil = require("apputil/util.js");
var DatabaseEngine = require("./DatabaseEngine.js");
var UpdateEngine = require("./UpdateEngine.js");
var CustomCodeManager = require("../customcode/customcodemanager.js");
var ModuleManager = require("./module/ModuleManager.js");

var START_UP_CALL_BACK_RESULT = "_startupcallbackresult_";

exports.performUpdate = function (options) {

    metaDataProvider.getTable(options.updates[QueryConstants.Query.TABLE], options, ApplaneCallback(options.callback, function (tableInfo) {
        var table = tableInfo.table;
        options.organization = tableInfo.organization;
        options.application = tableInfo.application;
        options.currentapplication = tableInfo.currentapplication;


        var db = tableInfo.database;


        var optionOperations = options.updates[QueryConstants.Update.OPERATIONS];
        if (!optionOperations) {
            options.callback(new BaasError(Constants.ErrorCode.UPDATES_NOT_FOUND));
            return;
        }
        var operations;
        if (optionOperations instanceof Array) {
            operations = optionOperations;
        } else if (optionOperations instanceof Object) {
            operations = [optionOperations];
        } else {
            options.callback(new BaasError(Constants.ErrorCode.IMPROPER_UPDATES));
            return;
        }
        var operationCount = operations.length;
        var insertOperations = [];
        var updateOperations = [];
        var deleteOperations = [];

        var filter = options.updates[QueryConstants.Query.FILTER];
        var parameters = options.updates[QueryConstants.Query.PARAMETERS];

        resolveFilter(filter, parameters, operations, table, options, ApplaneCallback(options.callback, function () {
            apputil.resolveDot(operations);
            operations = mergeOperations(operations);
            operationCount = operations.length;
            var opIndex = -1;
            var opResults = {};

            var operationCallBack = ApplaneCallback(options.callback, function () {

                opIndex = opIndex + 1;
                if (opIndex < operationCount) {
                    var operation = operations[opIndex];
                    if (operation instanceof Array || !(operation instanceof Object)) {
                        throw new Error("Individual operations should be object only: " + typeof operation);
                    }
                    var type = operation[QueryConstants.Update.Operation.TYPE];
                    if (!type) {
                        if (operation[QueryConstants.Query._ID]) {
                            type = QueryConstants.Update.Operation.Type.UPDATE;
                        } else {
                            type = QueryConstants.Update.Operation.Type.INSERT;
                        }
                        operation[QueryConstants.Update.Operation.TYPE] = type;
                    }

                    var operationCompleteCallback = ApplaneCallback(options.callback, function (result) {

                        if (!(result instanceof Object)) {
                            result = {};
                        }
                        var typeResult = opResults[type];
                        if (!typeResult) {
                            typeResult = [];
                            opResults[type] = typeResult;
                        }
                        if (operation._idtemp) {
                            result._idtemp = operation._idtemp;
                        }
                        var opId = operation._id;
                        if (!opId) {
                            opId = result._id;
                        }
                        if (!opId) {
                            throw new Error("_id must be avalilable either before old operation or aftert operation")
                        }
                        typeResult.push(result);

                        var reverseTableUpdatesArray = options.reverseupdates;

                        if (reverseTableUpdatesArray) {
                            delete options.reverseupdates;
                            var reverseTableUpdates = reverseTableUpdatesArray[0];

                            var reverseTable = reverseTableUpdates.reversetable;
                            var reverseColumn = reverseTableUpdates.reverserelationcolumn;
                            var reverseOperations = reverseTableUpdates.operations;

                            var newReverseUpdates = {"table":reverseTable};

                            var reverseOperationCount = reverseOperations.length;
                            for (var i = 0; i < reverseOperationCount; i++) {
                                var tableOpr = reverseOperations[i][reverseColumn];
                                if (!tableOpr) {
                                    tableOpr = {};
                                }
                                tableOpr._id = opId;
                                var tableOprType = reverseOperations[i][QueryConstants.Update.Operation.TYPE];
                                if (tableOprType) {
                                    tableOpr[QueryConstants.Update.Operation.TYPE] = tableOprType;
                                    delete reverseOperations[i][QueryConstants.Update.Operation.TYPE];
                                }

                                //check if _id does not provide then we have to put opreation type insertifnotexis
                                reverseOperations[i][QueryConstants.Update.Operation.TYPE] = QueryConstants.Update.Operation.Type.INSERT_IF_NOT_EXIST;
                                reverseOperations[i][reverseColumn] = tableOpr;
                            }
                            newReverseUpdates.operations = reverseOperations;
                            var newReverseUpdateOption = {"updates":newReverseUpdates, "ask":options.ask, "osk":options.osk, "callback":ApplaneCallback(options.callback, function (reverseUpdateResult) {
                                if (reverseUpdateResult) {
                                    result[reverseTable] = reverseUpdateResult
                                }

                                operationCallBack();

                            }) }
                            UpdateEngine.performUpdate(newReverseUpdateOption);


                        } else {
                            operationCallBack();
                        }


                    });

                    if (type == QueryConstants.Update.Operation.Type.INSERT) {
                        performInsertOperation(table, db, operation, options, operationCompleteCallback);
                    } else if (type == QueryConstants.Update.Operation.Type.UPDATE) {
                        performUpdateOperation(table, db, operation, options, operationCompleteCallback);
                    } else if (type == QueryConstants.Update.Operation.Type.DELETE) {
                        performDeleteOperation(table, db, operation, options, operationCompleteCallback);
                    } else if (type == QueryConstants.Update.Operation.Type.INSERT_IF_NOT_EXIST) {
                        if (operation[QueryConstants.Query._ID]) {
                            operation[QueryConstants.Update.Operation.TYPE] = QueryConstants.Update.Operation.Type.UPDATE
                            performUpdateOperation(table, db, operation, options, operationCompleteCallback);
                        } else {
                            //check if exists using primary columns

                            var columns = table[Constants.Baas.Tables.COLUMNS];
                            var columnsCount = columns.length

                            var findQuery = {};
                            for (var j = 0; j < columnsCount; j++) {
                                if (columns[j][Constants.Baas.Tables.Columns.PRIMARY]) {
                                    var primaryColumn = columns[j][Constants.Baas.Tables.Columns.EXPRESSION];
                                    if (operation[primaryColumn]) {
                                        findQuery[primaryColumn] = operation[primaryColumn];
                                    }
                                }

                            }
                            if (Object.keys(findQuery).length == 0) {
                                operation[QueryConstants.Update.Operation.TYPE] = QueryConstants.Update.Operation.Type.INSERT
                                performInsertOperation(table, db, operation, options, operationCompleteCallback);
                            } else {
                                // fire a query to find lookup data
                                DatabaseEngine.query(
                                    {
                                        "ask":options[QueryConstants.Query.ASK],
                                        "osk":options[QueryConstants.Query.OSK],
                                        "query":{
                                            "table":table[Constants.Baas.Tables.ID],
                                            "excludejobs":true,
                                            "filter":findQuery
                                        },
                                        "callback":ApplaneCallback(options.callback, function (lookupResult) {
                                            var lookupResultCount = lookupResult && lookupResult.data ? lookupResult.data.length : 0
                                            if (lookupResultCount == 1) {
                                                operation._id = lookupResult.data[0]._id;
                                                operation[QueryConstants.Update.Operation.TYPE] = QueryConstants.Update.Operation.Type.UPDATE
                                                performUpdateOperation(table, db, operation, options, operationCompleteCallback);

                                            } else if (lookupResultCount > 1) {
                                                throw new Error("More than one result found for column [" + colDef.expression + "] with value [" + JSON.stringify(lookupFindQuery) + "]")
                                            } else {
                                                operation[QueryConstants.Update.Operation.TYPE] = QueryConstants.Update.Operation.Type.INSERT
                                                performInsertOperation(table, db, operation, options, operationCompleteCallback);
                                            }
                                        })}
                                )
                            }

                        }
                    } else {
                        throw new BaasError(Constants.ErrorCode.UPDATE_TYPE_NOT_SUPPORTED, [type])
                    }
                } else {
                    //All operations completed........
                    options.callback(null, opResults);
                }
            });
            operationCallBack();
        }));
    }))
};

mergeOperations = function (operations) {

    var operationCount = operations ? operations.length : 0;
    if (operationCount == 0) {
        return operations;
    }
    var newOperations = [];
    for (var i = 0; i < operationCount; i++) {
        var o = operations[i];
        if (o[QueryConstants.Query._ID]) {
            var newOperationCount = newOperations.length;
            var found = false;
            for (var j = 0; j < newOperationCount; j++) {
                var newO = newOperations[j];
                if (newO[QueryConstants.Query._ID]) {
                    var newId = newO[QueryConstants.Query._ID].toString();
                    var oldId = o[QueryConstants.Query._ID].toString();
                    if (newId == oldId) {
                        //merge both operations

                        Object.keys(o).forEach(function (newKey) {
                            var newKeyValue = newO[newKey];
                            var oldKeyValue = o[newKey];
                            var val = merge(newKeyValue, oldKeyValue);

                            newO[newKey] = val


                        });
                        found = true;
                        break;
                    }

                }

            }
            if (!found) {
                newOperations.push(o);
            }
        } else {
            newOperations.push(o);
        }

    }
    return newOperations;

}

isNullOrUndefined = function (val) {
    return val == undefined || val == null
}
merge = function (value1, value2) {
    if (isNullOrUndefined(value1)) {
        return value2;
    } else if (isNullOrUndefined(value2)) {
        return value1;
    } else if (value1 instanceof ObjectID || value2 instanceof ObjectID) {
        var value1Str = value1.toString();
        var value2Str = value2.toString();
        if (value1Str == value2Str) {
            return value1Str;
        } else {
            return [value1, value2]
        }

    } else if (value1 instanceof Array || value2 instanceof Array) {
        var value1Array = value1 instanceof Array ? value1 : [value1];
        var value2Array = value2 instanceof Array ? value2 : [value2];
        return mergeArray(value1Array, value2Array);
    } else if (value1 instanceof Object && value2 instanceof Object) {
        return mergeObject(value1, value2);
    } else {
        return value2;
    }


}
mergeObject = function (obj1, obj2) {


    if (isNullOrUndefined(obj1)) {
        return obj2;
    } else if (isNullOrUndefined(obj2)) {
        return obj1;
    }
    if (obj1._id && obj2._id) {
        var value1Str = obj1._id.toString();
        var value2Str = obj2._id.toString();
        if (value1Str == value2Str) {

            return obj2
        } else {
            return [obj1, obj2]
        }

    } else {
        return [obj1, obj2]
    }


}
mergeArray = function (arr1, arr2) {
    var newArray = [];
    newArray.push.apply(newArray, arr1);
    newArray.push.apply(newArray, arr2);
    return newArray
}

resolveFilter = function (filter, parameters, operations, table, options, callback) {
    if (!filter) {
        callback();
        return;
    }
    FilterUtil.resolveFilters(filter, parameters, table, options, ApplaneCallback(callback, function () {
        //iterate filter and push it in all inserts
        var operationCount = operations ? operations.length : 0;
        for (var key in filter) {
            for (var i = 0; i < operationCount; i++) {
                operations[i][key] = filter[key];

            }
        }
        callback();
    }));

}

callModuleInternal = function (table, db, operation, when, operations, oldValues, newValues, options, callback) {
    var excludeModules = options.updates[QueryConstants.Update.EXCLUDE_MODULES] ? options.updates[QueryConstants.Update.EXCLUDE_MODULES] : false
    if (excludeModules) {
        callback();
        return;
    }
    var method = when + apputil.capitalize(operation);
    ModuleManager[method](table, operations, { currentapplication:options.currentapplication, application:options.application, organization:options.organization, database:db, ask:options.ask, osk:options.osk, usk:options.usk, user:options.user, oldvalues:oldValues, newvalues:newValues}, callback);

}

callJobInternal = function (table, operation, when, operations, oldValues, newValues, options, callback) {
    var requiredJobs = [];
    var excludeJobs = options.updates[QueryConstants.Update.EXCLUDE_JOBS] ? options.updates[QueryConstants.Update.EXCLUDE_JOBS] : false
    if (!excludeJobs) {
        var jobs = table[Constants.Baas.Tables.JOBS];
        var count = jobs ? jobs.length : 0;
        for (var i = 0; i < count; i++) {
            var job = jobs[i];
            var jobOperation = job[Constants.Baas.Tables.Jobs.OPERATION];
            var jobWhen = job[Constants.Baas.Tables.Jobs.WHEN];
            var jobType = job[Constants.Baas.Tables.Jobs.TYPE];
            var jobApplication = job[Constants.Baas.Tables.Jobs.APPLICATION];
            if ((!jobApplication || jobApplication[QueryConstants.Query._ID].toString() == options.currentapplication[QueryConstants.Query._ID].toString())) {
                if ((jobOperation == operation && jobWhen == when)) {
                    requiredJobs.push(job);
                }
                if ((jobType == Constants.Baas.Tables.Jobs.Type.UPDATE && when == 'before') || (jobType == Constants.Baas.Tables.Jobs.Type.TRANSFORM && when == 'after')) {
                    requiredJobs.push(job);
                }
            }
        }
    }
    apputil.iterateArray(requiredJobs, ApplaneCallback(callback, function () {
        callback(null, {operations:operations});
    }), function (requiredJob, callback) {

        CustomCodeManager.callJobs({
            "application":options.currentapplication,
            "organization":options.organization,
            "ask":options[QueryConstants.Query.ASK],
            "mainask":table[Constants.Baas.Tables.MAINAPPLICATION][Constants.Baas.Applications.ASK],
            "osk":options[QueryConstants.Query.OSK],
            "usk":options.usk,
            "logid":options.logid,
            "user":options.user,
            "code":requiredJob[Constants.Baas.Tables.Jobs.CODE],
            "module":requiredJob[Constants.Baas.Tables.Jobs.MODULE],
            "when":when,
            "operation":operation,
            "method":when + apputil.capitalize(operation),
            "parameters":{
                "operations":operations,
                "oldvalues":oldValues,
                "newvalues":newValues
            },
            "callback":ApplaneCallback(callback, function (params) {
                operations = params.operations;
                callback();
            })
        });

    })
}

callJob = function (table, db, operation, when, operations, oldValues, newValues, options, callback) {
    if (when == Constants.Baas.Tables.Jobs.When.BEFORE) {
        callJobInternal(table, operation, when, operations, oldValues, newValues, options, ApplaneCallback(callback, function (params) {
            operations = params.operations;
            callModuleInternal(table, db, operation, when, operations, oldValues, newValues, options, ApplaneCallback(callback, function () {
                apputil.resolveDot(operations);
                callback(null, operations);
            }));
        }));
    } else {
        callModuleInternal(table, db, operation, when, operations, oldValues, newValues, options, ApplaneCallback(callback, function () {
            callJobInternal(table, operation, when, operations, oldValues, newValues, options, ApplaneCallback(callback, function (params) {
                operations = params.operations;
                apputil.resolveDot(operations);
                callback(null, operations);
            }));
        }));
    }

}

performInsertOperation = function (table, db, operation, options, callback) {
    callJob(table, db, Constants.Baas.Tables.Jobs.Operation.INSERT, Constants.Baas.Tables.Jobs.When.BEFORE, [operation], [], [], options, ApplaneCallback(options.callback, function (operations) {

        var columns = table[Constants.Baas.Tables.SYSTEM_COLUMNS];
        var columnsCount = columns.length;
        var insert = operations[0];
        var compiledOp = {};
        var colDefIndex = 0;
        var compileOpCallBack = ApplaneCallback(options.callback, function (success) {
            colDefIndex = colDefIndex + 1;
            if (colDefIndex != columnsCount) {
                processColumnValue(columns[colDefIndex], insert, {}, compiledOp, options, table, compileOpCallBack);
            } else {
                if (Object.keys(compiledOp).length == 0) {
                    throw new Error("Empty object while insertion in [" + table[Constants.Baas.Tables.ID] + "]");
                }
                mongoDBManager.insert(db[Constants.Baas.Tables.DB], table[Constants.Baas.Tables.ID], compiledOp, ApplaneCallback(options.callback, function (data) {
                    data = data[0];
                    callJob(table, db, Constants.Baas.Tables.Jobs.Operation.INSERT, Constants.Baas.Tables.Jobs.When.AFTER, [insert], [], [data], options, ApplaneCallback(options.callback, function () {
                        callback(null, data);
                    }));

                }));
            }
        })
        processColumnValue(columns[colDefIndex], insert, undefined, compiledOp, options, table, compileOpCallBack);

    }));


}

getData = function (operation, options, table, callback) {
    //get old values

    DatabaseEngine.query({
        "ask":options[QueryConstants.Query.ASK],
        "osk":options[QueryConstants.Query.OSK],
        "query":{
            "table":table[Constants.Baas.Tables.ID],
            "excludejobs":true,
            "filter":{
                "_id":operation[QueryConstants.Query._ID].toString()
            }
        },
        "callback":ApplaneCallback(options.callback, function (oldValueData) {

            var data = oldValueData.data;
            if (!data || data.length == 0) {
                throw new Error("No record found for update with id" + JSON.stringify(operation[QueryConstants.Query._ID]) + ", table[" + table[Constants.Baas.Tables.ID] + "]");
            }
            callback(null, data[0]);

        })
    });
}

performUpdateOperation = function (table, db, operation, options, callback) {

    getData(operation, options, table, ApplaneCallback(options.callback, function (oldData) {
        callJob(table, db, Constants.Baas.Tables.Jobs.Operation.UPDATE, Constants.Baas.Tables.Jobs.When.BEFORE, [operation], [oldData], [], options, ApplaneCallback(options.callback, function (operations) {

            var columns = table[Constants.Baas.Tables.SYSTEM_COLUMNS];

            var columnsCount = columns.length;

            operation = operations[0];


            var colDefIndex = -1;

            var compiledOp = {};
            var $inc;
            if (operation.$inc) {
                $inc = operation.$inc;
                delete operation.$inc;
            }

            var columnCallback = ApplaneCallback(options.callback, function (result) {
                colDefIndex = colDefIndex + 1;
                if (colDefIndex != columnsCount) {
                    processColumnValue(columns[colDefIndex], operation, oldData, compiledOp, options, table, columnCallback);
                } else {
                    var query = {};
                    query[QueryConstants.Query._ID] = new ObjectID(operation[QueryConstants.Query._ID].toString());
                    var mongoUpdateObject = {"$set":compiledOp};
                    if ($inc) {
                        mongoUpdateObject.$inc = $inc;
                    }

                    mongoDBManager.update(db[Constants.Baas.Tables.DB], table[Constants.Baas.Tables.ID], query, mongoUpdateObject, {logid:options.logid}, ApplaneCallback(options.callback, function (result) {
                        getData(operation, options, table, ApplaneCallback(options.callback, function (newValue) {
                            callJob(table, db, Constants.Baas.Tables.Jobs.Operation.UPDATE, Constants.Baas.Tables.Jobs.When.AFTER, [operation], [oldData], [newValue], options, ApplaneCallback(options.callback, function () {
                                callback(null, result);
                            }));


                        }));

                    }));

                }
            });

            columnCallback();


        }));
    }));


}

performDeleteOperation = function (table, db, operation, options, callback) {
    getData(operation, options, table, ApplaneCallback(options.callback, function (oldData) {
        callJob(table, db, Constants.Baas.Tables.Jobs.Operation.DELETE, Constants.Baas.Tables.Jobs.When.BEFORE, [operation], [oldData], [], options, ApplaneCallback(options.callback, function (operations) {

            var columns = table[Constants.Baas.Tables.SYSTEM_COLUMNS];
            var columnsCount = columns.length;
            var operation = operations[0];
            if (!operation[QueryConstants.Query._ID]) {
                throw new Error("Please prove _id column to delete [" + JSON.stringify(operation) + "]");
            }
            var opCallBack = ApplaneCallback(options.callback, function (result) {
                callJob(table, db, Constants.Baas.Tables.Jobs.Operation.DELETE, Constants.Baas.Tables.Jobs.When.AFTER, [operation], [oldData], [], options, ApplaneCallback(options.callback, function (operations) {
                    callback(null, result);
                }));
            });
            var _id = operation[QueryConstants.Query._ID];
            var query = {};
            query[QueryConstants.Query._ID] = new ObjectID(_id.toString());
            mongoDBManager.remove(db[Constants.Baas.Tables.DB], table[Constants.Baas.Tables.ID], query, opCallBack);
        }));

    }));
}

processColumnValue = function (colDef, value, oldValue, compiledValue, options, table, callback) {

    var expression = colDef[Constants.Baas.Tables.Columns.EXPRESSION];
    //we will not process dotted expression as they will be combined up in one object earlier
    if (expression.indexOf(".") >= 0) {
        callback();
        return;
    }


    var updatedValue = value[expression];

    var oldColValue = oldValue ? oldValue[expression] : undefined;
    if (updatedValue === undefined) {
        callback();
        return;
    } else if (updatedValue == null) {
        compiledValue[expression] = updatedValue;
        callback();
        return;
    }

    var multiple = colDef[Constants.Baas.Tables.Columns.MULTIPLE];

    if (multiple) {


        var updateValueClone = value[expression];
        var override = false;
        if (!(updatedValue instanceof Array)) {
            if ((updatedValue instanceof Object)) {
                if (updatedValue.data) {
                    override = updatedValue[QueryConstants.Update.Operation.OVERRIDE]
                    updatedValue = updatedValue.data

                    if (!(updatedValue instanceof Array)) {
                        updatedValue = [updatedValue];
                    }
                } else {
                    updatedValue = [updatedValue];
                }
            } else {
                updatedValue = [updatedValue];
            }

        }
        var reverseRelationColumn = colDef[Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN];

        if (reverseRelationColumn) {
            if (override) {
                throw Error("Override array support not supported in reverse relation.....");
            }
            var reverseTable = colDef[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
            var reverseUpdates = options.reverseupdates;
            if (!reverseUpdates) {
                reverseUpdates = [];
                options.reverseupdates = reverseUpdates;
            }

            var reverseTableOperations = {"operations":updatedValue, "reversetable":reverseTable, "reverserelationcolumn":reverseRelationColumn};
            reverseUpdates.push(reverseTableOperations);

            callback();
            return;

        }


        var count = updatedValue.length;
        if (count == 0) {
            compiledValue[expression] = updatedValue;
            callback();
            return;
        }
        var multipleIndex = 0;
        var multipleInserts = [];
        var multipleCallBack = ApplaneCallback(callback, function (processedUpdatedValue) {
            multipleInserts.push(processedUpdatedValue);
            multipleIndex = multipleIndex + 1;
            if (multipleIndex == count) {
                var overideCallBack = ApplaneCallback(options.callback, function (multipleInsertsToUpdate) {
                    var cnt = multipleInsertsToUpdate ? multipleInsertsToUpdate.length : 0;
                    for (var i = 0; i < cnt; i++) {
                        if (multipleInsertsToUpdate[i][QueryConstants.Update.Operation.TYPE]) {
                            throw new Error("Operation should not exists after final iteration in multiple array[" + JSON.stringify(multipleInserts) + "]");
                        }

                    }
                    compiledValue[expression] = multipleInsertsToUpdate;
                    callback();
                    return;
                });
                var reverseRelationColumn = colDef[Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN];

                if (reverseRelationColumn) {
                    var _id = value[QueryConstants.Query._ID];
                    var reverseTable = colDef[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
                    var currentTable = table[Constants.Baas.Tables.ID];
                    var reverseUpdates = options.reverseupdates;
                    if (!reverseUpdates) {
                        reverseUpdates = {};
                        options.reverseupdates = reverseUpdates;
                    }
                    var reverseTableOperations = reverseUpdates[currentTable];
                    if (!reverseTableOperations) {
                        reverseTableOperations = {"operations":[], "value":value, "reversetable":reverseTable, "reverserelationcolumn":reverseRelationColumn};
                        reverseUpdates[currentTable] = reverseTableOperations;
                    }
                    reverseTableOperations.operations.push.apply(reverseTableOperations.operations, multipleInserts);

                    callback();
                    return;

                } else {
                    getMergeArrayValue(override, oldColValue, multipleInserts, overideCallBack);
                }


            } else {
                getColumnUpdatedValue(colDef, updatedValue[multipleIndex], getOldValueFromArray(updatedValue[multipleIndex], oldColValue), value, options, table, multipleCallBack);
            }
        });


        getColumnUpdatedValue(colDef, updatedValue[multipleIndex], getOldValueFromArray(updatedValue[multipleIndex], oldColValue), value, options, table, multipleCallBack);


    } else {
        var updateCallBack = ApplaneCallback(callback, function (processedUpdatedValue) {

            compiledValue[expression] = processedUpdatedValue;
            callback();
        });
        getColumnUpdatedValue(colDef, updatedValue, oldColValue, value, options, table, updateCallBack);

    }


};

getOldValueFromArray = function (toMatch, array) {
    if (!array || !(array instanceof Array)) {
        return
    }
    if (!toMatch || !toMatch._id) {
        return;
    }
    var toMatchID = toMatch._id.toString();
    var arrayCount = array.length;
    for (var i = 0; i < arrayCount; i++) {
        var a = array[i];
        if (a._id && a._id.toString() == toMatchID) {
            return a;
        }

    }

}

getMergeArrayValue = function (override, oldValues, newValues, callback) {

    var multipleInsertsToUpdate;

    if (override) {
        callback(null, newValues);
        return
    } else {
        if (oldValues && !(oldValues instanceof Array)) {
            oldValues = [oldValues];
        }
//        if (oldValues) {
//            oldValues = JSON.parse(JSON.stringify(oldValues));
//        }
        var oldCount = oldValues ? oldValues.length : 0;
        var newCount = newValues ? newValues.length : 0;
        if (oldCount == 0) {
            callback(null, newValues);
            return
        } else if (newCount == 0) {
            callback(null, oldValues);
            return
        } else {
            var newAray = [];
            for (var i = 0; i < oldCount; i++) {

                var o = oldValues[i];
                if (typeof o == 'object') {
                    var n = {};
                    Object.keys(o).forEach(function (k) {
                        n[k] = o[k];
                    });
                    newAray.push(n);
                } else {
                    newAray.push(o);
                }

            }
            for (var i = 0; i < newCount; i++) {
                var newValue = newValues[i];
                var newValueId;
                if (newValue instanceof Object) {
                    newValueId = newValue[QueryConstants.Query._ID].toString();
                } else {
                    newValueId = newValue;
                }
                var found = false;
                oldCount = newAray.length;
                var oldIndex = -1;
                for (var j = 0; j < oldCount; j++) {
                    var oldValue = newAray[j];
                    var oldValueId;

                    if (oldValue instanceof Object && oldValue[QueryConstants.Query._ID]) {

                        oldValueId = oldValue[QueryConstants.Query._ID].toString();

                    } else {
                        oldValueId = oldValue;
                    }


                    if (oldValueId == newValueId) {
                        oldIndex = j;
                        found = true;
                        break;
                    }
                }

                if (found) {
                    var opType = newValue[QueryConstants.Update.Operation.TYPE];
                    if (opType && opType == QueryConstants.Update.Operation.Type.DELETE) {
                        newAray.splice(oldIndex, 1);

                    } else {
                        //merge values
                        var toMerge = newAray[oldIndex];
                        if (typeof newValue == 'object') {
                            Object.keys(newValue).forEach(function (k) {
                                toMerge[k] = newValue[k];
                            });
                            newAray[oldIndex] = toMerge;
                        } else {
                            newAray[oldIndex] = newValue;
                        }

                    }
                } else {
                    newAray.push(newValue);
                }


            }

            callback(null, newAray);
            return
        }
    }
}
getColumnUpdatedValue = function (colDef, value, oldValue, operation, options, table, callback) {

    if (value == undefined) {
        callback(null);
        return;
    } else if (value == null) {
        callback(null, null);
        return;
    }

    var expression = colDef[Constants.Baas.Tables.Columns.EXPRESSION];
    var colType = colDef[Constants.Baas.Tables.Columns.TYPE];
    if (colType == Constants.Baas.Tables.Columns.Type.STRING || colType == Constants.Baas.Tables.Columns.Type.FILE || colType == Constants.Baas.Tables.Columns.Type.IMAGE || colType == Constants.Baas.Tables.Columns.Type.TEXT || colType == Constants.Baas.Tables.Columns.Type.RICHTEXT) {
        callback(null, value);
        return;
    } else if (colType == Constants.Baas.Tables.Columns.Type.NUMBER || colType == Constants.Baas.Tables.Columns.Type.DECIMAL) {
        if (value.toString().trim().length == 0) {
            callback(null, 0);
        } else {
            callback(null, Number(value));
        }

        return;
    } else if (colType == Constants.Baas.Tables.Columns.Type.DATE) {
        callback(null, apputil.parseDateValue(colDef, value));
        return;
    } else if (colType == Constants.Baas.Tables.Columns.Type.OBJECT) {
        if (!(value instanceof Object)) {
            throw new Error("Expression[" + expression + "] is of type object but its value is not object[" + JSON.stringify(value) + "]");
        }
        var lookupOperation = value[QueryConstants.Update.Operation.TYPE];
        if (lookupOperation && lookupOperation == QueryConstants.Update.Operation.Type.DELETE) {
            callback(null, value);
            return;
        }
        //check if other columns exists for replicated , for eg. role in developers in application
        var colDefs = colDef[Constants.Baas.Tables.COLUMNS];
        if (colDefs instanceof String) {
            colDefs = JSON.parse(colDefs);
        }
        var colDefsCount = colDefs ? colDefs.length : 0;
        if (colDefsCount == 0) {
            throw new Error("Expression[" + expression + "] is of type object but it does not have any columns to save");
        }
        var colDefIndex = -1;
        var _idValue;
        if (value[QueryConstants.Query._ID]) {
            _idValue = new ObjectID(value[QueryConstants.Query._ID].toString());
        } else {
            _idValue = apputil.getUnique();
        }
        var objectValueToSave = {};
        if (oldValue && oldValue instanceof Object && !colDef.multiple) {
            Object.keys(oldValue).forEach(function (k) {
                objectValueToSave[k] = oldValue[k];
            })
            var oldId = objectValueToSave._id;
            if (oldId) {
                objectValueToSave[QueryConstants.Query._ID] = new ObjectID(oldId.toString());
            } else {
                objectValueToSave[QueryConstants.Query._ID] = _idValue;
            }
        } else {
            objectValueToSave[QueryConstants.Query._ID] = _idValue;
        }

        var otherReplicateColDefCallback = ApplaneCallback(options.callback, function () {

            colDefIndex = colDefIndex + 1;
            if (colDefIndex == colDefsCount) {
                callback(null, objectValueToSave);
            } else {
                processColumnValue(colDefs[colDefIndex], value, oldValue, objectValueToSave, options, table, otherReplicateColDefCallback);
            }
        });
        otherReplicateColDefCallback(null, START_UP_CALL_BACK_RESULT);

    } else if (colType == Constants.Baas.Tables.Columns.Type.LOOKUP) {
        //check it has options
        var lookupOptions = colDef[Constants.Baas.Tables.Columns.OPTIONS];

        var lookupOptionsCount = (lookupOptions && (lookupOptions instanceof Array) ) ? lookupOptions.length : 0;
        if (lookupOptionsCount > 0) {
            //check for options
            var optionValue;
            for (var i = 0; i < lookupOptionsCount; i++) {
                var o = lookupOptions[i];
                if (o == value) {
                    optionValue = o;
                    break;
                }

            }
            if (optionValue) {
                callback(null, optionValue);
            } else {
                throw new Error("Lookup value does not match in options" + JSON.stringify(lookupOptions) + ", value found[" + JSON.stringify(value) + "]");
            }
            return;

        }

        if (value instanceof ObjectID || (!(value instanceof Object))) {
            var revisedUpdatedValue = {};
            revisedUpdatedValue[QueryConstants.Query._ID] = value.toString();
            value = revisedUpdatedValue;
        }
        var lookupOperation = value[QueryConstants.Update.Operation.TYPE];
        if (lookupOperation && lookupOperation == QueryConstants.Update.Operation.Type.DELETE) {
            callback(null, value);
            return;
        }
        var lookupTable = colDef[Constants.Baas.Tables.Columns.TABLE];


        lookupTable = (lookupTable instanceof Object ) ? lookupTable[Constants.Baas.Tables.ID] : lookupTable;
        var lookupPrimaryColumnsStr = colDef[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS];

        var lookupPrimaryColumns = lookupPrimaryColumnsStr ? (lookupPrimaryColumnsStr instanceof Array ? lookupPrimaryColumnsStr : JSON.parse(lookupPrimaryColumnsStr)) : [];
        var lookupPrimaryColumnsLength = lookupPrimaryColumns ? lookupPrimaryColumns.length : 0;

        var lookupReplicateColumnsStr = colDef[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS];
        var lookupReplicateColumns = lookupReplicateColumnsStr ? lookupReplicateColumnsStr instanceof Array ? lookupReplicateColumnsStr : JSON.parse(lookupReplicateColumnsStr) : [];
        var lookupReplicateColumnsLength = lookupReplicateColumns ? lookupReplicateColumns.length : 0;

        var updateCallBack = ApplaneCallback(options.callback, function () {
            var lookupFindQuery = {};
            //check if updated value contains _id columns
            if (value[QueryConstants.Query._ID]) {
                lookupFindQuery[QueryConstants.Query._ID] = value[QueryConstants.Query._ID];
            } else {
                if (lookupPrimaryColumnsLength == 0) {
                    throw new Error("No primary columns found for table[" + lookupTable + "], column" + JSON.stringify(colDef));
                }
                for (var j = 0; j < lookupPrimaryColumnsLength; j++) {
                    var lookupPrimaryColumn = lookupPrimaryColumns[j][Constants.Baas.Tables.Columns.EXPRESSION];

                    if (value[lookupPrimaryColumn]) {
                        lookupFindQuery[lookupPrimaryColumn] = value[lookupPrimaryColumn];
                    }

                }
            }
            // fire a query to find lookup data
            DatabaseEngine.query(
                {
                    "ask":options[QueryConstants.Query.ASK],
                    "osk":options[QueryConstants.Query.OSK],
                    "query":{
                        "table":lookupTable,
                        "excludejobs":true,
                        "filter":lookupFindQuery
                    },
                    "callback":ApplaneCallback(options.callback, function (lookupResult) {
                        var lookupResultCallBack = ApplaneCallback(options.callback, function (lookupResultValue) {
                            var lookupValueToSave = {};
                            lookupValueToSave[QueryConstants.Query._ID] = lookupResultValue[QueryConstants.Query._ID];


                            for (var j = 0; j < lookupReplicateColumnsLength; j++) {
                                var lookupReplicateColumn = lookupReplicateColumns[j][Constants.Baas.Tables.Columns.EXPRESSION];
                                lookupValueToSave[lookupReplicateColumn] = lookupResultValue[lookupReplicateColumn];
                            }
                            //check if other columns exists for replicated , for eg. role in developers in application
                            var otherColumnsStr = colDef[Constants.Baas.Tables.COLUMNS];

                            var otherColumns = otherColumnsStr ? otherColumnsStr instanceof Array ? otherColumnsStr : JSON.parse(otherColumnsStr) : [];

                            //For flex field, columns can also comes in operations
                            var operationColumns = operation[QueryConstants.Update.Operation.EXTRA_COLUMNS] ? operation[QueryConstants.Update.Operation.EXTRA_COLUMNS][expression] : null;

                            if (operationColumns && operationColumns instanceof Array && operationColumns.length > 0) {
                                otherColumns.push.apply(otherColumns, operationColumns);
                            }

                            var otherColCount = otherColumns ? otherColumns.length : 0;
                            var otherColIndex = -1;

                            var otherColumnCallback = ApplaneCallback(options.callback, function () {

                                otherColIndex = otherColIndex + 1;
                                if (otherColIndex == otherColCount) {
                                    callback(null, lookupValueToSave);
                                } else {
                                    processColumnValue(otherColumns[otherColIndex], value, oldValue, lookupValueToSave, options, table, otherColumnCallback);
                                }
                            });
                            otherColumnCallback(null, START_UP_CALL_BACK_RESULT);

                        })

                        var lookupResultCount = lookupResult && lookupResult.data ? lookupResult.data.length : 0


                        if (lookupResultCount == 1) {
                            lookupResultCallBack(null, lookupResult.data[0]);

                        } else if (lookupResultCount > 1) {
                            throw new Error("More than one result found for column [" + colDef.expression + "] with value [" + JSON.stringify(lookupFindQuery) + "]")

                        } else {
                            //we have to insert a new lookup value
                            var autoSave = colDef[[Constants.Baas.Tables.Columns.AUTOSAVE]];
                            if (!autoSave) {
                                throw new Error("Auto save is not allowed for column [" + colDef.expression + "] with value [" + JSON.stringify(lookupFindQuery) + "]");
                            }
                            var lookupUpdateOptionsUpdate = {};
                            lookupUpdateOptionsUpdate[QueryConstants.Query.TABLE] = lookupTable;
                            lookupUpdateOptionsUpdate[QueryConstants.Update.OPERATIONS] = value;

                            var lookupUpdateOptions = {"updates":lookupUpdateOptionsUpdate};

                            lookupUpdateOptions[QueryConstants.Query.ASK] = options[QueryConstants.Query.ASK];
                            lookupUpdateOptions[QueryConstants.Query.OSK] = options[QueryConstants.Query.OSK];
                            lookupUpdateOptions.callback = ApplaneCallback(options.callback, function (lookupNewValue) {
                                lookupNewValue = lookupNewValue[QueryConstants.Update.Operation.Type.INSERT];
                                if (!(lookupNewValue instanceof Array)) {
                                    throw new Error("Lookup inserted value should be instance of Array [" + JSON.stringify(value) + "]");
                                }
                                var lookupNewValueLength = lookupNewValue.length;
                                if (lookupNewValueLength == 0) {
                                    throw new Error("Lookup inserted value should not be blank [" + JSON.stringify(value) + "]");
                                }
                                //get first value
                                var lookupNewValueObject = lookupNewValue[0];
                                lookupResultCallBack(null, lookupNewValueObject);
                            });
                            UpdateEngine.performUpdate(lookupUpdateOptions);

                        }
                    })}
            );
        });
        var diff = false;
        var excludeJobs = options.updates[QueryConstants.Update.EXCLUDE_JOBS] ? options.updates[QueryConstants.Update.EXCLUDE_JOBS] : false
        var excludeModules = options.updates[QueryConstants.Update.EXCLUDE_MODULES] ? options.updates[QueryConstants.Update.EXCLUDE_MODULES] : false

        if (!excludeJobs && !excludeModules && oldValue && oldValue[QueryConstants.Query._ID] && value && value[QueryConstants.Query._ID] && value[QueryConstants.Query._ID].toString() == oldValue[QueryConstants.Query._ID].toString()) {
            //check if both are same
            //check if replicate columns are same

            for (var i = 0; i < lookupReplicateColumnsLength; i++) {
                var replicateColumn = lookupReplicateColumns[i].expression;

                var replicateColumnOld = oldValue[replicateColumn];
                var replicateColumnNew = value[replicateColumn];

                if (replicateColumnNew === undefined) {
                    continue;
                } else if ((replicateColumnOld === undefined || replicateColumnOld === null ) && (replicateColumnNew === undefined || replicateColumnNew === null )) {
                    continue;
                } else if (replicateColumnOld !== undefined && replicateColumnNew !== undefined && typeof replicateColumnOld != 'object' && typeof replicateColumnNew != 'object' && replicateColumnOld.toString().trim() == replicateColumnNew.toString().trim()) {
                    continue;
                } else if (replicateColumnOld === undefined || replicateColumnOld === null) {
                    diff = true;
                    break;
                } else {
                    if (replicateColumnOld._id && replicateColumnNew._id && replicateColumnOld._id.toString() === replicateColumnNew._id.toString()) {
                        continue;
                    } else {
                        var match = apputil.deepEqual(replicateColumnOld, replicateColumnNew)
                        if (!match) {
                            diff = true;
                            break;
                        }
                    }
                }


            }

        }
        if (diff) {
            var lookupUpdateOptionsUpdate = {};
            lookupUpdateOptionsUpdate[QueryConstants.Query.TABLE] = lookupTable;
            lookupUpdateOptionsUpdate[QueryConstants.Update.OPERATIONS] = value;
            var lookupUpdateOptions = {"updates":lookupUpdateOptionsUpdate};
            lookupUpdateOptions[QueryConstants.Query.ASK] = options[QueryConstants.Query.ASK];
            lookupUpdateOptions[QueryConstants.Query.OSK] = options[QueryConstants.Query.OSK];
            lookupUpdateOptions.callback = updateCallBack;
            UpdateEngine.performUpdate(lookupUpdateOptions);
        } else {
            updateCallBack();
        }


    } else if (colType == Constants.Baas.Tables.Columns.Type.BOOLEAN) {
        callback(null, value);
        return;
    } else if (colType == Constants.Baas.Tables.Columns.Type.DATE || colType == Constants.Baas.Tables.Columns.Type.DATETIME) {
        callback(null, new Date(value));
        return;
    } else {
        throw new Error("Unhandled column type[" + colType + "] in col def " + JSON.stringify(colDef));
    }


}
