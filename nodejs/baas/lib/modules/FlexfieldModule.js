/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 9/3/13
 * Time: 12:19 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var Utils = require("apputil/util.js");
var DataBaseEngine = require('../database/DatabaseEngine.js');
var MetadataProvider = require('../metadata/MetadataProvider.js');
var ObjectID = require('mongodb').ObjectID;


exports.beforeInsert = function (table, operations, context, callback) {
    if (context.osk) {
        MetadataProvider.getOrganization(context.osk, ApplaneCallback(callback, function (organization) {
            addExtraColumns(table, operations, context, organization, callback);
        }));
    } else {
        addExtraColumns(table, operations, context, null, callback);
    }
}

exports.beforeUpdate = function (table, operations, context, callback) {
    if (context.osk) {
        MetadataProvider.getOrganization(context.osk, ApplaneCallback(callback, function (organization) {
            addExtraColumns(table, operations, context, organization, callback);
        }));
    } else {
        addExtraColumns(table, operations, context, null, callback);
    }
}

function getColumnValue(expression, operation, oldOperation) {
    var value = operation[expression] || (oldOperation ? oldOperation[expression] : null);
    if (value && !(value instanceof ObjectID) && value instanceof Object && value[APIConstants.Query._ID]) {
        value = value[APIConstants.Query._ID]
    }
    return value ? value.toString() : value;
}

function addExtraColumns(table, operations, context, organization, callback) {
    var columns = table[Constants.Baas.Tables.COLUMNS];
    Utils.iterateArray(columns, callback, function (column, callback) {
        var columnType = column[Constants.Baas.Tables.Columns.TYPE];
        if (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP && column[Constants.Baas.Tables.Columns.FLEXIBLE]) {
            var expression = column[Constants.Baas.Tables.Columns.EXPRESSION];
            var optionColumnMap = {};
            Utils.iterateArrayWithIndex(operations, callback, function (index, operation, callback) {
                var flexColumnValue = getColumnValue(expression, operation, (context.oldvalues ? context.oldvalues[index] : null));
                if (flexColumnValue) {
                    getOptionColumns(flexColumnValue, column, optionColumnMap, organization, ApplaneCallback(callback, function (optionColumns) {
                        var extraColumns = operation[APIConstants.Update.Operation.EXTRA_COLUMNS] = operation[APIConstants.Update.Operation.EXTRA_COLUMNS] || {};
                        extraColumns[expression] = optionColumns;
                        callback();
                    }));
                } else {
                    callback();
                }
            })
        } else {
            callback();
        }
    });
}

exports.syncTable = function (table, callback) {
    if (flexColumnExists(table)) {
        table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
        var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
        var length = enableModules.length;
        for (var i = 0; i < length; i++) {
            if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.FlexfieldModule.ID) {
                callback();
                return;
            }
        }
        var flexfieldModule = {};
        flexfieldModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.FlexfieldModule.ID;
        flexfieldModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.FlexfieldModule.MODULE_PATH;
        enableModules.push(flexfieldModule);
    }
    callback();
}

function flexColumnExists(table) {
    var columns = table[Constants.Baas.Tables.COLUMNS] || [];
    var length = columns.length;
    for (var i = 0; i < length; i++) {
        var columnInfo = columns[i];
        var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
        if (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP && columnInfo[Constants.Baas.Tables.Columns.FLEXIBLE]) {
            return true;
        }
    }
    return false;
}

function getOptionColumns(flexColumnValue, column, optionColumnMap, organization, callback) {
    if (optionColumnMap[flexColumnValue]) {
        callback(optionColumnMap[flexColumnValue]);
        return;
    }
    var refferedTable = column[Constants.Baas.Tables.Columns.TABLE];
    var refferedTableName = column[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
    var orgEnabled = refferedTable[Constants.Baas.Tables.ORGENABLED];

    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.FLEXFIELDS;
    var filter = {};
    filter[Constants.Baas.Flexfields.TABLE] = refferedTableName;
    filter[Constants.Baas.Flexfields.COLUMNVALUE] = flexColumnValue;
    if (orgEnabled) {
        filter[Constants.Baas.Flexfields.ORGANIZATION] = organization[APIConstants.Query._ID].toString();
    }
    query[APIConstants.Query.FILTER] = filter;

    DataBaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (data) {
            data = data.data;
            if (data && data.length > 0) {
                data = removeDottedColumn(data);
                optionColumnMap[flexColumnValue] = data;
                callback(null, data);
            } else {
                callback(null, []);
            }
        })
    });
}

function removeDottedColumn(columns) {
    var newColumns = [];
    var columnCount = columns ? columns.length : 0;
    for (var i = 0; i < columnCount; i++) {
        var column = columns[i];
        var expression = column[Constants.Baas.Tables.Columns.EXPRESSION];
        if (column[Constants.Baas.Tables.COLUMNS]) {
            var pColumns = column[Constants.Baas.Tables.COLUMNS];
            if (pColumns instanceof Array) {
                column[Constants.Baas.Tables.COLUMNS] = pColumns;
            } else {
                column[Constants.Baas.Tables.COLUMNS] = JSON.parse(pColumns);
            }
            column[Constants.Baas.Tables.COLUMNS] = removeDottedColumn(column[Constants.Baas.Tables.COLUMNS]);
        }
        if (column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS]) {
            var replicateColumns = column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS];

            if (replicateColumns instanceof Array) {
                column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS] = replicateColumns;
            } else {
                column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS] = JSON.parse(replicateColumns);
            }
            column[Constants.Baas.Tables.REPLICATE_COLUMNS] = removeDottedColumn(column[Constants.Baas.Tables.REPLICATE_COLUMNS]);
        }
        if (column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS]) {
            var primaryColumns = column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS];
            if (primaryColumns instanceof Array) {
                column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] = primaryColumns;
            } else {
                column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] = JSON.parse(primaryColumns);
            }
            column[Constants.Baas.Tables.PRIMARY_COLUMNS] = removeDottedColumn(column[Constants.Baas.Tables.PRIMARY_COLUMNS]);
        }
        var dottedIndex = expression.indexOf(".");
        if (dottedIndex == -1) {
            newColumns.push(column);
        }
    }
    return newColumns;

}