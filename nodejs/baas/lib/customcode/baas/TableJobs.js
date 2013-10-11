/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 8/2/13
 * Time: 6:11 PM
 * To change this template use File | Settings | File Templates.
 */

var Utils = require("apputil/util.js");
var Constants = require('../../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var ModuleManager = require("../../database/module/ModuleManager.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var NodejsAPI = require("nodejsapi");
var MetadataProvider = require('../../metadata/MetadataProvider.js');
var CacheService = require("../../cache/cacheservice.js");
var TableSynchronizer = require("../../metadata/TableSynchronizer.js");
var DatabaseEngine = require("../../database/DatabaseEngine.js");
var UpdateEngine = require("../../database/UpdateEngine.js");


exports.beforeInsert = function (params, callback) {
    Utils.iterateArray(params.operations, callback, function (table, callback) {
        var applications = table[Constants.Baas.Tables.APPLICATIONS];
        var applicationId;
        if (applications instanceof Array && applications.length > 0) {
            applicationId = applications[0]._id;
        } else if (applications instanceof Object && applications._id) {
            applicationId = applications._id;
        } else if (applications && applications.toString().length > 0) {
            applicationId = applications.toString();
        }
        if (!applicationId) {
            throw new Error("No application attached with table to create[" + table.alias + "]");
        }

        DatabaseEngine.query({
            ask:"baas",
            query:{"table":"applications__baas", "filter":{"_id":applicationId}},
            callback:ApplaneCallback(callback, function (applicationInfo) {

                var appId = applicationInfo.data[0].id;
                var tableId = table[Constants.Baas.Tables.ALIAS] + "__" + appId
                //check tableId for uniqueness

                DatabaseEngine.query({
                    ask:"baas",
                    query:{"table":"tables__baas", "filter":{"id":tableId}},
                    callback:ApplaneCallback(callback, function (tableInfo) {
                        if (tableInfo && tableInfo.data && tableInfo.data.length > 0) {
                            throw new Error("Table already exists[" + tableId + "]");
                        }
                        table[Constants.Baas.Tables.ID] = tableId;
                        table[Constants.Baas.Tables.MAINAPPLICATION] = applicationInfo.data[0]._id;

                        var columns = table[Constants.Baas.Tables.COLUMNS];
                        if (columns) {
                            if (columns instanceof Object && !(columns instanceof Array) && Object.keys(columns).length > 0) {
                                addValidations(columns, true, ApplaneCallback(callback, function () {
                                    ModuleManager.syncTable(table, callback);
                                }));
                            } else if (columns instanceof Array && columns.length > 0) {
                                Utils.iterateArray(columns, callback, function (column, callback) {
                                    addValidations(column, true, ApplaneCallback(callback, function () {
                                        ModuleManager.syncTable(table, callback);
                                    }));
                                })
                            } else {
                                ModuleManager.syncTable(table, callback);
                            }
                        } else {
                            callback();
                        }
                    })
                })
            })
        })
    });
}

function addValidations(column, insert, callback) {
    var columnType = column[Constants.Baas.Tables.Columns.TYPE];
    if (!insert) {
        if (!column[Constants.Baas.Tables.Columns.EXPRESSION]) {
            throw new Error("Column Expression can't be blank");
        }
        if (!columnType) {
            throw new Error("Column Type is mandatory for column [" + column[Constants.Baas.Tables.Columns.EXPRESSION] + "]");
        }
    }
    if (column[Constants.Baas.Tables.Columns.EXPRESSION]) {
        column[Constants.Baas.Tables.Columns.EXPRESSION] = column[Constants.Baas.Tables.Columns.EXPRESSION].trim();
    }
    if (!columnType) {
        callback();
        return;
    }
    if (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP) {
        if (!column[Constants.Baas.Tables.Columns.TABLE] && (!column[Constants.Baas.Tables.Columns.OPTIONS] || column[Constants.Baas.Tables.Columns.OPTIONS].length == 0 )) {
            throw new Error("Either Table or Options is mandatory for lookup column [" + column[Constants.Baas.Tables.Columns.EXPRESSION] + "]");
        }
        if (column[Constants.Baas.Tables.Columns.TABLE] && column[Constants.Baas.Tables.Columns.OPTIONS] && column[Constants.Baas.Tables.Columns.OPTIONS].length > 0) {
            throw new Error("Table and Options both should not be defined for lookup column [" + column[Constants.Baas.Tables.Columns.EXPRESSION] + "].Please define one of them.");
        }
        if (column[Constants.Baas.Tables.Columns.TABLE]) {
            var lookupTableName = column[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
            checkPrimaryColumnInTable(lookupTableName, ApplaneCallback(callback, function (primaryColumn) {
                if (!primaryColumn || primaryColumn.length == 0) {
                    throw new Error("No primary column defined in lookup table [" + lookupTableName + "]");
                }
                callback();
            }))
        } else {
            callback();
        }
    } else {
        if (column[Constants.Baas.Tables.Columns.TABLE]) {
            throw new Error("Table can't be defined for column type [" + columnType + "] for column [" + column[Constants.Baas.Tables.Columns.EXPRESSION] + "]");
        }
        if (column[Constants.Baas.Tables.Columns.OPTIONS] && column[Constants.Baas.Tables.Columns.OPTIONS].length > 0) {
            throw new Error("Options can't be defined for column type [" + columnType + "] for column [" + column[Constants.Baas.Tables.Columns.EXPRESSION] + "]");
        }
        if (column[Constants.Baas.Tables.Columns.AUTOSAVE]) {
            throw new Error("Autosave can't be defined for column type [" + columnType + "] for column [" + column[Constants.Baas.Tables.Columns.EXPRESSION] + "]");
        }
        callback();
    }
}

exports.beforeUpdate = function (params, callback) {
    console.log("table before update called...");
    var oldValues = params.oldvalues;
    Utils.iterateArrayWithIndex(params.operations, callback, function (index, table, callback) {
        var oldValue = oldValues[index];
        if (table[Constants.Baas.Tables.ID]) {
            throw new Error("TableId for Table [" + oldValue[Constants.Baas.Table.ID] + "] can't be changed");
        }
        var columns = table[Constants.Baas.Tables.COLUMNS];
        if (columns) {
            if (columns instanceof Object && !(columns instanceof Array) && Object.keys(columns).length > 0) {
                mergeOldValueColumns(oldValue.columns || [], columns);
                addValidations(columns, false, callback);
            } else if (columns instanceof Array && columns.length > 0) {
                Utils.iterateArray(columns, callback, function (column, callback) {
                    mergeOldValueColumns(oldValue.columns || [], column);
                    addValidations(column, false, callback);
                })
            } else {
                callback();
            }
        }
        else {
            callback();
        }
    });
}

function mergeOldValueColumns(oldValueColumns, column) {
    for (var i = 0; i < oldValueColumns.length; i++) {
        var oldValueColumn = oldValueColumns[i];
        if (oldValueColumn[APIConstants.Query._ID] == column[APIConstants.Query._ID]) {
            for (var exp in oldValueColumn) {
                if (column[exp] == undefined) {
                    column[exp] = oldValueColumn[exp];
                }
            }
            break;
        }
    }
}

function checkPrimaryColumnInTable(lookupTableName, callback) {
    var filter = {};
    filter[Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.PRIMARY] = true;
    filter[Constants.Baas.Tables.ID ] = "{tableid__}";
    var orFilter = [];
    orFilter.push({"columns.type":"string"});
    orFilter.push({"columns.type":"number"});
    filter.$or = orFilter;

    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.TABLES;
    query[APIConstants.Query.COLUMNS] = [APIConstants.Query._ID];
    query[APIConstants.Query.FILTER] = filter;
    query[APIConstants.Query.PARAMETERS] = {"tableid__":lookupTableName};
    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            callback(null, data);
        })
    })
}

exports.afterInsert = function (params, callback) {
    var options = {ask:params.ask, osk:params.osk, usk:params.usk};
    Utils.iterateArray(params.newvalues, callback, function (table, callback) {
        clearCache(table[Constants.Baas.Tables.ID]);
        saveBaasViewEntry(table, options, ApplaneCallback(callback, function () {
            var columns = table[Constants.Baas.Tables.COLUMNS];
            if (columns && columns.length > 0) {
                syncLookupReplicateColumns(table, table[Constants.Baas.Tables.COLUMNS], options, ApplaneCallback(callback, function () {
                    var tableUpdate = {};
                    if (table[Constants.Baas.Tables.COLUMNS] && table[Constants.Baas.Tables.COLUMNS].length > 0) {
                        tableUpdate[Constants.Baas.Tables.COLUMNS] = table[Constants.Baas.Tables.COLUMNS];
                    }
                    var updates = {};
                    tableUpdate [APIConstants.Query._ID] = table[APIConstants.Query._ID];
                    updates[APIConstants.Query.TABLE] = Constants.Baas.TABLES;
                    updates[APIConstants.Update.OPERATIONS] = tableUpdate;
                    updates.excludejobs = true;
                    UpdateEngine.performUpdate({
                        ask:params.ask,
                        osk:params.osk,
                        usk:params.usk,
                        updates:updates,
                        callback:ApplaneCallback(callback, function () {
                            TableSynchronizer.synchronize(table[Constants.Baas.Tables.ID], callback);
                        })
                    });
                }));
            } else {
                callback();
            }
        }));
    });
}

exports.afterUpdate = function (params, callback) {
    var options = {ask:params.ask, osk:params.osk, usk:params.usk};
    Utils.iterateArray(params.newvalues, callback, function (table, callback) {
        clearCache(table[Constants.Baas.Tables.ID]);
        var columns = table[Constants.Baas.Tables.COLUMNS];
        if (columns && columns.length > 0) {
            syncLookupReplicateColumns(table, columns, options, ApplaneCallback(callback, function () {
                table[Constants.Baas.Tables.ENABLE_MODULES] = [];
                ModuleManager.syncTable(table, ApplaneCallback(callback, function () {
                    var tableUpdate = {};
                    tableUpdate[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES];
                    tableUpdate[Constants.Baas.Tables.COLUMNS] = table[Constants.Baas.Tables.COLUMNS];
                    if (!table[Constants.Baas.Tables.MAINAPPLICATION] && table[Constants.Baas.Tables.APPLICATIONS] && table[Constants.Baas.Tables.APPLICATIONS].length > 0) {
                        tableUpdate[Constants.Baas.Tables.MAINAPPLICATION] = table[Constants.Baas.Tables.APPLICATIONS][0];
                    }

                    var updates = {};
                    tableUpdate [APIConstants.Query._ID] = table[APIConstants.Query._ID];
                    updates[APIConstants.Query.TABLE] = Constants.Baas.TABLES;
                    updates[APIConstants.Update.OPERATIONS] = tableUpdate;
                    updates.excludejobs = true;
                    UpdateEngine.performUpdate({
                        ask:params.ask,
                        osk:params.osk,
                        usk:params.usk,
                        updates:updates,
                        callback:ApplaneCallback(callback, function () {
                            TableSynchronizer.synchronize(table[Constants.Baas.Tables.ID], callback);
                        })
                    });
                }));
            }));
        } else {
            callback();
        }
    });
}

exports.saveView = function (table, ask, callback) {
    var tableName = table[Constants.Baas.Tables.ID];
    var tableId = table[APIConstants.Query._ID];
    var query = {};
    query[APIConstants.Query.TABLE] = tableName;

    var view = {};
    view[Constants.Baas.Views.ID] = tableName;
    view[Constants.Baas.Views.QUERY] = query;
    view[Constants.Baas.Views.TABLE] = {};
    view[Constants.Baas.Views.TABLE][APIConstants.Query._ID] = tableId;

    var updates = {};
    updates[APIConstants.Query.TABLE] = Constants.Baas.VIEWS;
    updates[APIConstants.Update.OPERATIONS] = view;
    UpdateEngine.performUpdate({
        ask:ask,
        updates:updates,
        callback:callback
    });
}

function saveBaasViewEntry(table, options, callback) {
    var tableName = table[Constants.Baas.Tables.ID];
    var tableId = table[APIConstants.Query._ID];
    var query = {};
    query[APIConstants.Query.TABLE] = tableName;

    var view = {};
    view[Constants.Baas.Views.ID] = tableName;
    view[Constants.Baas.Views.QUERY] = JSON.stringify(query);
    view[Constants.Baas.Views.TABLE] = {};
    view[Constants.Baas.Views.TABLE][APIConstants.Query._ID] = tableId;

    NodejsAPI.asUpdate(Constants.Baas.VIEWS, options).insert(view).save(callback);
}

function handleDotedColumns(columns) {
    var columnCount = columns ? columns.length : 0;
    var columnCache = {};
    for (var i = 0; i < columnCount; i++) {
        var column = columns[i];
        var primary = column[Constants.Baas.Tables.Columns.PRIMARY];
        if (primary && primary == true) {
            column[Constants.Baas.Tables.Columns.REPLICATE] = true;
        }
        delete column[APIConstants.Query.COLUMNS];
        var expression = column[Constants.Baas.Tables.Columns.EXPRESSION];
        var dottedIndex = expression.indexOf(".");
        if (dottedIndex >= 0) {
            var firstPart = expression.substring(0, dottedIndex);
            var secondPart = expression.substring(dottedIndex + 1);
            var firstColDef = columnCache[firstPart];
            if (!firstColDef) {
                columnCache[firstPart] = firstColDef = {};
            }
            var firstColDefColumns = firstColDef[APIConstants.Query.COLUMNS];
            if (!firstColDefColumns) {
                firstColDef[APIConstants.Query.COLUMNS] = firstColDefColumns = [];
            }
            var clone = JSON.parse(JSON.stringify(column));
            clone[Constants.Baas.Tables.Columns.EXPRESSION] = secondPart;
            firstColDefColumns.push(clone);
        } else {
            if (columnCache[expression] && columnCache[expression][APIConstants.Query.COLUMNS]) {
                var firstColDefColumns = column[APIConstants.Query.COLUMNS];
                if (!firstColDefColumns) {
                    column[APIConstants.Query.COLUMNS] = firstColDefColumns = [];
                }
                var cacheColumns = columnCache[expression][APIConstants.Query.COLUMNS];
                cacheColumns.forEach(function (cacheColumn) {
                    firstColDefColumns.push(cacheColumn);
                });
            }
            columnCache[expression] = column
        }
    }
    for (var i = 0; i < columnCount; i++) {
        var column = columns[i];
        if (column[APIConstants.Query.COLUMNS]) {
            var innerColumns = column[APIConstants.Query.COLUMNS];
            handleDotedColumns(innerColumns);
            column[APIConstants.Query.COLUMNS] = JSON.stringify(innerColumns);
        }
    }
}

function syncLookupReplicateColumns(table, columns, options, callback) {
    Utils.iterateArray(columns, ApplaneCallback(callback, function () {
        handleDotedColumns(columns);
        callback();
    }), function (column, callback) {
        var type = column[Constants.Baas.Tables.Columns.TYPE];
        var columnOptions = column[Constants.Baas.Tables.Columns.OPTIONS];
        if (columnOptions && typeof columnOptions == "string") {
            columnOptions = JSON.parse(columnOptions);
        }
        if (type == Constants.Baas.Tables.Columns.Type.LOOKUP && (!columnOptions || columnOptions.length == 0)) {
            var lookupTableName = column[Constants.Baas.Tables.Columns.TABLE];
            if (lookupTableName instanceof Object) {
                lookupTableName = lookupTableName[Constants.Baas.Tables.ID];
            }
            MetadataProvider.getTable(lookupTableName, ApplaneCallback(callback, function (lookupTable) {
                var lookupTableColumns = lookupTable[Constants.Baas.Tables.COLUMNS];
                var lookupColumnsCount = lookupTableColumns ? lookupTableColumns.length : 0;
                var replicateColumns = [];
                var primaryColumns = [];
                for (var j = 0; j < lookupColumnsCount; j++) {
                    var lookupColumn = lookupTableColumns[j];
                    var tableColumns = lookupColumn[APIConstants.Query.COLUMNS];
                    if (!tableColumns || tableColumns.length == 0) {
                        delete lookupColumn[APIConstants.Query.COLUMNS];
                    }
                    if (lookupColumn[Constants.Baas.Tables.Columns.REPLICATE] || lookupColumn[Constants.Baas.Tables.Columns.PRIMARY]) {
                        replicateColumns.push(lookupColumn);
                    }
                    if (lookupColumn[Constants.Baas.Tables.Columns.PRIMARY]) {
                        primaryColumns.push(lookupColumn);
                    }
                }
                if (primaryColumns.length > 0) {
                    var moved = moveStringOrNumberColumnOnFirstPosition(primaryColumns, "string");
                    if (!moved) {
                        moveStringOrNumberColumnOnFirstPosition(primaryColumns, "number");
                    }
                }
                column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS] = JSON.stringify(replicateColumns);
                column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] = JSON.stringify(primaryColumns);

                //populate reverse multiple column
                var lookupColumnExpression = column[Constants.Baas.Tables.Columns.EXPRESSION];
                if (!lookupTable[Constants.Baas.Tables.PUBLIC] && column[Constants.Baas.Tables.Columns.MULTIPLE] && lookupColumnExpression.indexOf(".") == -1 && !column[Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN]) {
                    var reverseColumnName = table[Constants.Baas.Tables.ID] + "__" + lookupColumnExpression;
                    var lookupCacheColumns = lookupTable[Constants.Baas.Tables.SYSTEM_COLUMNS];
                    var rColumn = getColumn(reverseColumnName, lookupCacheColumns);
                    if (!rColumn) {
                        var reverseColumn = {};
                        reverseColumn[Constants.Baas.Tables.Columns.EXPRESSION] = reverseColumnName;
                        reverseColumn[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.LOOKUP;
                        reverseColumn[Constants.Baas.Tables.Columns.MULTIPLE] = true;
                        reverseColumn[Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN] = lookupColumnExpression;
                        reverseColumn[Constants.Baas.Tables.Columns.TABLE] = {};
                        reverseColumn[Constants.Baas.Tables.Columns.TABLE][APIConstants.Query._ID] = table[APIConstants.Query._ID];
                        reverseColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID] = table[Constants.Baas.Tables.ID];
                        var tableUpdate = {};
                        tableUpdate[Constants.Baas.Tables.COLUMNS] = [reverseColumn];
                        NodejsAPI.asUpdate(Constants.Baas.TABLES, options).update(lookupTable[APIConstants.Query._ID], tableUpdate).save(callback);
                    } else {
                        callback();
                    }
                } else {
                    callback();
                }
            }));
        } else {
            callback();
        }
    });
}

function moveStringOrNumberColumnOnFirstPosition(primaryColumns, movedType) {
    for (var i = 0; i < primaryColumns.length; i++) {
        var primaryColumn = primaryColumns[i];
        var primaryColumnType = primaryColumn[Constants.Baas.Tables.Columns.TYPE];
        if (primaryColumnType == movedType) {
            primaryColumns.splice(i, 1);
            primaryColumns.unshift(primaryColumn);
            return true;
        }
    }
    return false;
}


function clearCache(tableName) {
    var cacheKey = ["Tables", tableName];
    CacheService.clear(cacheKey);
}

function getColumn(expression, columns) {
    if (columns) {
        var length = columns.length;
        for (var i = 0; i < length; i++) {
            var column = columns[i];
            if (column[Constants.Baas.Tables.Columns.EXPRESSION] == expression) {
                return column;
            }
        }
    }
}