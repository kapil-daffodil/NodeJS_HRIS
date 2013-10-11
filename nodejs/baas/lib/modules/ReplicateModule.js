/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 7/9/13
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../shared/Constants.js');
var APIConstants = require("nodejsapi/Constants.js");
var DataBaseEngine = require('../database/DatabaseEngine.js');
var UpdateEngine = require('../database/UpdateEngine.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var MongoDBManager = require("../database/mongodb/MongoDBManager.js");
var Utils = require("apputil/util.js");
var objectID = require("mongodb").ObjectID;
var NodejsAPI = require("nodejsapi");
var ModuleManager = require("../database/module/ModuleManager.js");
var CacheService = require("../cache/cacheservice.js");
var TableSynchronizer = require("../metadata/TableSynchronizer.js");
var MetadataProvider = require('../metadata/MetadataProvider.js');

exports.beforeInsert = function (table, operations, context, callback) {
    var tableName = table[Constants.Baas.Tables.ID];
    callback();
}

exports.beforeUpdate = function (table, operations, context, callback) {
    var tableName = table[Constants.Baas.Tables.ID];
    callback();
}

exports.afterInsert = function (table, operations, context, callback) {
    var tableName = table[Constants.Baas.Tables.ID];
    callback();
}

exports.afterUpdate = function (table, operations, context, callback) {
    var tableName = table[Constants.Baas.Tables.ID];
    if (tableName == Constants.Baas.TABLES) {
        updateTableReplicateAndPrimaryColumns(operations, context, ApplaneCallback(callback, function () {
            updateReplicateColumnData(table, operations, context, callback);
        }))
    } else {
        updateReplicateColumnData(table, operations, context, callback);
    }
}

function updateTableReplicateAndPrimaryColumns(operations, context, callback) {
    var oldValues = context.oldvalues;
    var newValues = context.newvalues;
    Utils.iterateArrayWithIndex(operations, callback, function (index, operation, callback) {
        var oldValue = oldValues ? oldValues[index] : null;
        var oldValueColumns = oldValue ? oldValue[Constants.Baas.Tables.COLUMNS] || [] : [];
        var newValue = newValues ? newValues[index] : null;
        var newValueColumns = newValue ? newValue[Constants.Baas.Tables.COLUMNS] || [] : [];

        var oldValueReplicateColumns = [];
        var oldValuePrimaryColumns = [];
        var newValueReplicateColumns = [];
        var newValuePrimaryColumns = [];

        getReplicateAndPrimaryColumns(oldValueColumns, oldValueReplicateColumns, oldValuePrimaryColumns);
        getReplicateAndPrimaryColumns(newValueColumns, newValueReplicateColumns, newValuePrimaryColumns);

        var replicateColumnUpdateRequired = isUpdateRequired(oldValueReplicateColumns, newValueReplicateColumns);
        var primaryColumnUpdateRequired = isUpdateRequired(oldValuePrimaryColumns, newValuePrimaryColumns);

        if (replicateColumnUpdateRequired || primaryColumnUpdateRequired) {
            var filter = {};
            filter[Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.TABLE + "." + APIConstants.Query._ID] = "{tableid__}";
            filter[Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN] = null;
            var query = {};
            query[APIConstants.Query.TABLE] = Constants.Baas.TABLES;
            query[APIConstants.Query.COLUMNS] = [Constants.Baas.Tables.COLUMNS + "." + APIConstants.Query._ID];
            query[APIConstants.Query.FILTER] = filter;
            query[APIConstants.Query.PARAMETERS] = {"tableid__":operation[APIConstants.Query._ID]};
            DataBaseEngine.query({
                ask:Constants.Baas.ASK,
                query:query,
                callback:ApplaneCallback(callback, function (result) {
                    if (result && result[APIConstants.Query.Result.DATA] && result[APIConstants.Query.Result.DATA].length > 0) {
                        var data = result[APIConstants.Query.Result.DATA];
                        Utils.iterateArray(data, callback, function (row, callback) {
                            var updates = {};
                            updates[APIConstants.Query.TABLE] = Constants.Baas.TABLES;
                            var operations = [];
                            var operation = {};
                            operation[APIConstants.Query._ID] = row[APIConstants.Query._ID];
                            var columns = [];
                            var column = {};
                            columns.push(column);
                            column[APIConstants.Query._ID] = row[Constants.Baas.Tables.COLUMNS + "." + APIConstants.Query._ID];
                            if (replicateColumnUpdateRequired) {
                                column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS] = JSON.stringify(newValueReplicateColumns);
                            }
                            if (primaryColumnUpdateRequired) {
                                column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] = JSON.stringify(newValuePrimaryColumns);
                            }
                            operation[Constants.Baas.Tables.COLUMNS] = columns;
                            operations.push(operation);
                            updates[APIConstants.Update.OPERATIONS] = operations;
                            updates.excludejobs = true;
                            updates.excludemodules = true;
                            UpdateEngine.performUpdate({
                                ask:Constants.Baas.ASK,
                                logid:context.logid,
                                updates:updates,
                                callback:callback
                            });
                        })
                    } else {
                        callback();
                    }
                })
            });
        } else {
            callback();
        }
    })
}

function isUpdateRequired(oldColumns, newColumns) {
    var oldColumnsLength = oldColumns.length;
    var newColumnsLength = newColumns.length;
    if (oldColumnsLength != newColumnsLength) {
        return true;
    }
    var oldColumnExpressions = [];
    for (var i = 0; i < oldColumnsLength; i++) {
        oldColumnExpressions.push(oldColumns[i][Constants.Baas.Tables.Columns.EXPRESSION]);
    }
    var newColumnExpressions = [];
    for (var i = 0; i < newColumnsLength; i++) {
        var newColumnExpression = newColumns[i][Constants.Baas.Tables.Columns.EXPRESSION];
        if (oldColumnExpressions.indexOf(newColumnExpression) == -1) {
            return true;
        } else {
            newColumnExpressions.push(newColumnExpression);
        }
    }
    for (var i = 0; i < oldColumnExpressions.length; i++) {
        if (newColumnExpressions.indexOf(oldColumnExpressions[i]) == -1) {
            return true;
        }
    }
    return false;
}

function getReplicateAndPrimaryColumns(columns, replicateColumns, primaryColumns) {
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        if (column[Constants.Baas.Tables.Columns.REPLICATE]) {
            replicateColumns.push(column);
        }
        if (column[Constants.Baas.Tables.Columns.PRIMARY]) {
            primaryColumns.push(column);
        }
    }
}

function updateReplicateColumnData(table, operations, context, callback) {
    var columns = table[Constants.Baas.Tables.COLUMNS] || [];
    var replicateAndPrimaryColumns = [];
    for (var i = 0; i < columns.length; i++) {
        var columnInfo = columns[i];
        if (columnInfo[Constants.Baas.Tables.Columns.REPLICATE] || columnInfo[Constants.Baas.Tables.Columns.PRIMARY]) {
            replicateAndPrimaryColumns.push(columnInfo[Constants.Baas.Tables.Columns.EXPRESSION]);
        }
    }
    if (replicateAndPrimaryColumns.length > 0) {
        var filter = {};
        filter[Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.TABLE + "." + Constants.Baas.Tables.ID] = "{tableid__}";
        filter[Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN] = null;
        filter[Constants.Baas.Tables.APPLICATIONS] = "{applicationid__}";
        var query = {};
        query[APIConstants.Query.TABLE] = Constants.Baas.TABLES;
        query[APIConstants.Query.COLUMNS] = [Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.EXPRESSION, Constants.Baas.Tables.ID, Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.MULTIPLE];
        query[APIConstants.Query.FILTER] = filter;
        query[APIConstants.Query.PARAMETERS] = {"tableid__":table[Constants.Baas.Tables.ID], "applicationid__":context.currentapplication._id};
        DataBaseEngine.query({
            ask:Constants.Baas.ASK,
            query:query,
            callback:ApplaneCallback(callback, function (result) {
                if (result && result[APIConstants.Query.Result.DATA] && result[APIConstants.Query.Result.DATA].length > 0) {
                    var data = result[APIConstants.Query.Result.DATA];
                    Utils.iterateArray(operations, callback, function (operation, callback) {
                        Utils.iterateArray(data, callback, function (row, callback) {
                            var columnExpression = row[Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.EXPRESSION];
                            var columnMultiple = row[Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.MULTIPLE];
                            var tableId = row[Constants.Baas.Tables.ID];
                            var columnValue = {};
                            var indexOf = columnExpression.indexOf(".");
                            if (indexOf != -1) {
                                MetadataProvider.getTable(tableId, ApplaneCallback(callback, function (lookupTable) {
                                    var lookupTableColumns = lookupTable[Constants.Baas.Tables.COLUMNS];
                                    if (lookupTableColumns && lookupTableColumns.length > 0) {
                                        var multipleColumnsCount = getMultipleColumnsCount(0, columnExpression, null, lookupTableColumns);
                                        if (multipleColumnsCount == 0) {
                                            populateColumnValue(operation, columnMultiple, columnValue, columnExpression, replicateAndPrimaryColumns);
                                            updateValues(columnExpression, operation, columnValue, tableId, context, callback);
                                        } else if (multipleColumnsCount == 1) {
                                            var newColumnValueExpression = resolveSingleMultipleDottedColumns(columnExpression, null, null, lookupTableColumns);
                                            populateColumnValue(operation, columnMultiple, columnValue, newColumnValueExpression, replicateAndPrimaryColumns);
                                            updateValues(columnExpression, operation, columnValue, tableId, context, callback);
                                        } else {
                                            callback();
                                        }
                                    } else {
                                        updateValues(columnExpression, operation, columnValue, tableId, context, callback);
                                    }
                                }));
                            } else {
                                populateColumnValue(operation, columnMultiple, columnValue, columnExpression, replicateAndPrimaryColumns);
                                updateValues(columnExpression, operation, columnValue, tableId, context, callback);
                            }
                        })
                    })
                } else {
                    callback();
                }
            })
        })
    } else {
        callback();
    }
}

function resolveSingleMultipleDottedColumns(columnExpression, parentExpression, newExpression, tableColumns) {
    var indexOfFirstDot = columnExpression.indexOf(".");
    if (indexOfFirstDot != -1) {
        var firstPart = columnExpression.substr(0, indexOfFirstDot);
        var secondPart = columnExpression.substr(indexOfFirstDot + 1);
        var length = tableColumns.length;
        parentExpression = parentExpression ? parentExpression + "." + firstPart : firstPart;
        for (var i = 0; i < length; i++) {
            var tableColumn = tableColumns[i];
            var tableColumnExpression = tableColumn[Constants.Baas.Tables.Columns.EXPRESSION];
            var columnType = tableColumn[Constants.Baas.Tables.Columns.TYPE];
            if (tableColumnExpression == parentExpression && (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP || columnType == Constants.Baas.Tables.Columns.Type.OBJECT)) {
                var tableColumnMultiple = tableColumn[Constants.Baas.Tables.Columns.MULTIPLE];
                if (tableColumnMultiple) {
                    newExpression = newExpression ? newExpression + firstPart + ".$." : firstPart + ".$.";
                } else {
                    newExpression = newExpression ? newExpression + firstPart + "." : firstPart + ".";
                }
                return resolveSingleMultipleDottedColumns(secondPart, parentExpression, newExpression, tableColumns);
            }
        }
    }
    newExpression = newExpression ? newExpression + columnExpression : columnExpression;
    return newExpression;
}

function getMultipleColumnsCount(multipleColumnsCount, columnExpression, parentExpression, tableColumns) {
    var indexOfFirstDot = columnExpression.indexOf(".");
    if (indexOfFirstDot != -1) {
        var firstPart = columnExpression.substr(0, indexOfFirstDot);
        var secondPart = columnExpression.substr(indexOfFirstDot + 1);
        var length = tableColumns.length;
        parentExpression = parentExpression ? parentExpression + "." + firstPart : firstPart;
        for (var i = 0; i < length; i++) {
            var tableColumn = tableColumns[i];
            var tableColumnExpression = tableColumn[Constants.Baas.Tables.Columns.EXPRESSION];
            var columnType = tableColumn[Constants.Baas.Tables.Columns.TYPE];
            if (tableColumnExpression == parentExpression && (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP || columnType == Constants.Baas.Tables.Columns.Type.OBJECT)) {
                var tableColumnMultiple = tableColumn[Constants.Baas.Tables.Columns.MULTIPLE];
                if (tableColumnMultiple) {
                    multipleColumnsCount = multipleColumnsCount + 1;
                }
                return getMultipleColumnsCount(multipleColumnsCount, secondPart, parentExpression, tableColumns);
            }
        }
    }
    parentExpression = parentExpression ? parentExpression + "." + columnExpression : columnExpression;
    for (var i = 0; i < length; i++) {
        var tableColumn = tableColumns[i];
        var tableColumnExpression = tableColumn[Constants.Baas.Tables.Columns.EXPRESSION];
        var columnType = tableColumn[Constants.Baas.Tables.Columns.TYPE];
        if (tableColumnExpression == parentExpression && (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP || columnType == Constants.Baas.Tables.Columns.Type.OBJECT)) {
            var tableColumnMultiple = tableColumn[Constants.Baas.Tables.Columns.MULTIPLE];
            if (tableColumnMultiple) {
                multipleColumnsCount = multipleColumnsCount + 1;
            }
        }
    }
    return multipleColumnsCount;
}

function updateValues(columnExpression, operation, columnValue, tableId, context, callback) {
    if (Object.keys(columnValue).length > 0) {
        var updateQuery = {};
        updateQuery[columnExpression + "." + APIConstants.Query._ID] = new objectID(operation[APIConstants.Query._ID].toString());
        var updates = {$set:columnValue};
        MetadataProvider.getTable(tableId, context, ApplaneCallback(callback, function (data) {
            var database = data.database[Constants.Baas.Tables.DB];
            MongoDBManager.update(database, tableId, updateQuery, updates, {multi:true}, callback);
        }))
    } else {
        callback();
    }
}

function populateColumnValue(operation, columnMultiple, columnValue, columnExpression, replicateAndPrimaryColumns) {
    for (var i = 0; i < replicateAndPrimaryColumns.length; i++) {
        var replicateOrPrimaryColumnExp = replicateAndPrimaryColumns[i];
        if (operation[replicateOrPrimaryColumnExp] != undefined) {
            if (columnMultiple) {
                columnValue[columnExpression + ".$." + replicateOrPrimaryColumnExp] = operation[replicateOrPrimaryColumnExp];
            } else {
                columnValue[columnExpression + "." + replicateOrPrimaryColumnExp] = operation[replicateOrPrimaryColumnExp];
            }
        }
    }
}

exports.syncTable = function (table, callback) {
    var tableColumns = table [Constants.Baas.Tables.COLUMNS];
    var tableName = table [Constants.Baas.Tables.ID];
    var addModule = false;
    tableColumns.forEach(function (columnInfo) {
        if (columnInfo[Constants.Baas.Tables.Columns.REPLICATE]) {
            addModule = true;
        }
    })
    if (addModule) {
        table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
        var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
        var length = enableModules.length;
        for (var i = 0; i < length; i++) {
            if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.ReplicateModule.ID) {
                callback();
                return;
            }
        }
        var replicateModule = {};
        replicateModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.ReplicateModule.ID;
        replicateModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.ReplicateModule.MODULE_PATH;
        enableModules.push(replicateModule);
    }
    callback();
}