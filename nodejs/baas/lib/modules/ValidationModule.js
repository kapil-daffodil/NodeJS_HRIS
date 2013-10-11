/**
 * Created with IntelliJ IDEA.
 * User: Munish Kumar
 * Date: 8/17/13
 * Time: 10:22 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var MetadataProvider = require('../metadata/MetadataProvider.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var BaasError = require("apputil/ApplaneError.js");

exports.beforeInsert = function (table, operations, context, callback) {
    try {
        var columns = table[Constants.Baas.Tables.COLUMNS] || [];
        var oldValues = context.oldvalues;
        validateColumnsProperties(columns, operations, oldValues);
        callback();
    } catch (err) {
        callback(err);
    }
}

exports.beforeUpdate = function (table, operations, context, callback) {
    try {
        var columns = table[Constants.Baas.Tables.COLUMNS] || [];
        var oldValues = context.oldvalues;
        validateColumnsProperties(columns, operations, oldValues);
        callback();
    } catch (err) {
        callback(err);
    }
}

/**
 * It will enable the module for all tables.
 * @param table
 * @param callback
 */
exports.syncTable = function (table, callback) {
    table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
    var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
    var length = enableModules.length;
    for (var i = 0; i < length; i++) {
        if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.ValidationModule.ID) {
            callback();
            return;
        }
    }
    var validationModule = {};
    validationModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.ValidationModule.ID;
    validationModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.ValidationModule.MODULE_PATH;
    enableModules.push(validationModule);
    callback();
}


function validateColumnsProperties(columns, operations, oldValues) {
    operations.forEach(function (operation) {
            var insertOp = operation._id ? false : true;
            if (operation.__type__ && operation.__type__ == 'delete') {
                return;
            }
            columns.forEach(function (columnInfo) {
                var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
                var mandatory = columnInfo[Constants.Baas.Tables.Columns.MANDATORY];
                var column = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
                var multiple = columnInfo[Constants.Baas.Tables.Columns.MULTIPLE];
                var primary = columnInfo[Constants.Baas.Tables.Columns.PRIMARY];
                var columnValue = operation[column];
                if (mandatory) {
                    if (insertOp && isNullOrUndefined(columnValue)) {
                        throw new BaasError(Constants.ErrorCode.FIELDS_BLANK, [column]);
                    }

                    if (!insertOp && isNullValue(columnValue)) {
                        throw new BaasError(Constants.ErrorCode.FIELDS_BLANK, [column]);
                    }
                }
                if ((columnType == Constants.Baas.Tables.Columns.Type.OBJECT || columnType == Constants.Baas.Tables.Columns.Type.LOOKUP ) && !isNullOrUndefined(columnValue)) {
                    var lineItemColumns = columnInfo[Constants.Baas.Tables.COLUMNS];
                    var dataType = getDataType(columnValue);
                    if (!(columnValue instanceof Array)) {
                        columnValue = [columnValue];
                    }
                    var inLineOldValues = oldValues ? oldValues[column] : null;
                    if (inLineOldValues && !(inLineOldValues instanceof Array)) {
                        inLineOldValues = [inLineOldValues];
                    }
                    if (lineItemColumns) {
                        if (multiple) {
//                            checkUniqueForSubDoc(lineItemColumns, columnValue, inLineOldValues);
                        }
                        validateColumnsProperties(lineItemColumns, (columnValue ), inLineOldValues);
                    }
                } else {
                    /*Code to enforce data type constraints*/
                    validateDataType(columnValue, columnType);
                }
            });
        }
    )
}

function getPrimaryColumns(columns) {
    var length = columns ? columns.length : 0;
    var primaryColumns = [];
    for (var i = 0; i < length; i++) {
        var columnInfo = columns[i];
        var primary = columnInfo[Constants.Baas.Tables.Columns.PRIMARY];
        var multiple = columnInfo[Constants.Baas.Tables.Columns.MULTIPLE];
        if (primary && !multiple) {
            primaryColumns.push(columnInfo);
        }
    }
    return primaryColumns;
}

function getAccumulatedValue(primaryColumns, operation) {
    var length = primaryColumns.length;
    var accumulatedValue = "";
    for (var i = 0; i < length; i++) {
        var columnInfo = primaryColumns[i];
        var column = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
        var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
        var primary = columnInfo[Constants.Baas.Tables.Columns.PRIMARY];
        var multiple = columnInfo[Constants.Baas.Tables.Columns.MULTIPLE];
        if (multiple) {
            return;
        }
        var isIdColumn = (columnType == Constants.Baas.Tables.Columns.Type.OBJECT || columnType == Constants.Baas.Tables.Columns.Type.LOOKUP );
        var columnValue = operation[column];
        if (primary && columnValue) {
            if (isIdColumn) {
                columnValue = columnValue.id;
            }
            accumulatedValue = accumulatedValue + columnValue;
        }
    }
    return accumulatedValue.length > 0 ? accumulatedValue : null;
}

function checkUniqueForSubDoc(columns, operations, oldValues) {
    var length = operations.length;
    var oldValuesOplength = oldValues ? oldValues.length : 0;
    var primaryColumns = getPrimaryColumns(columns);
    var primaryColumnsCount = primaryColumns.length;
    if (primaryColumnsCount == 0) {
        return;
    }
    for (var i = 0; i < length; i++) {
        var operation = operations[i];
        var accumulatedValue = getAccumulatedValue(primaryColumns, operation);
        /*Check if value is not null*/
        if (accumulatedValue) {
            for (var j = i + 1; j < length; j++) {
                var nextOperations = operations[j];
                var nextAccumulatedValue = getAccumulatedValue(primaryColumns, nextOperations);
                if (nextAccumulatedValue && nextAccumulatedValue == accumulatedValue) {
                    throw new BaasError(Constants.ErrorCode.UNIQUE_VALIDATION_FAILED, ["duplicate value for primary columns." ]);
                }
            }
            for (var k = 0; k++; k < oldValuesOplength) {
                var oldRow = oldValues[k];
                var oldAccumulatedValue = getAccumulatedValue(primaryColumns, oldRow);
                if (oldAccumulatedValue && oldAccumulatedValue == accumulatedValue) {
                    throw new BaasError(Constants.ErrorCode.UNIQUE_VALIDATION_FAILED, ["duplicate value for primary columns."]);
                }
            }
        }
    }

}

function validateDataType(columnValue, columnType) {
    if (columnValue) {
        var dataType = getDataType(columnValue);
        if (columnType == Constants.Baas.Tables.Columns.Type.STRING && (dataType == 'Undefined' || dataType == 'Null')) {
            throw new BaasError(Constants.ErrorCode.INVALID_DATA_TYPE, [dataType + ' expected was string']);
        }
        if (columnType == Constants.Baas.Tables.Columns.Type.NUMBER || columnType == Constants.Baas.Tables.Columns.Type.DECIMAL) {
            if (dataType.toLowerCase() == Constants.Baas.Tables.Columns.Type.STRING) {
                try {
                    columnValue = Number(columnValue);
                    dataType = getDataType(columnValue);
                } catch (err) {
                }
            }
            if (dataType != "Number") {
                throw new BaasError(Constants.ErrorCode.INVALID_DATA_TYPE, [dataType + ' expected was ' + columnType]);
            }
            if (columnType == Constants.Baas.Tables.Columns.Type.NUMBER && !isInt(columnValue)) {
                throw new BaasError(Constants.ErrorCode.INVALID_DATA_TYPE, [dataType + ' expected was ' + columnType]);
            }
            if (columnType == Constants.Baas.Tables.Columns.Type.DECIMAL && !isFloat(columnValue) && !isInt(columnValue)) {
                throw new BaasError(Constants.ErrorCode.INVALID_DATA_TYPE, [dataType + ' expected was ' + columnType]);
            }
        }
        if (columnType == Constants.Baas.Tables.Columns.Type.BOOLEAN && dataType.toLowerCase() != Constants.Baas.Tables.Columns.Type.BOOLEAN) {
            throw new BaasError(Constants.ErrorCode.INVALID_DATA_TYPE, [dataType + ' expected was ' + columnType]);
        }
    }
}

function getDataType(data) {
    if (undefined === data) {
        return 'Undefined';
    }
    if (data === null) {
        return 'Null';
    }
    return {}.toString.call(data).slice(8, -1);
}

function isInt(data) {
    return typeof data === 'number' && data % 1 == 0;
}

function isFloat(data) {
    return typeof data === 'number' && !isNaN(data);
}

isNullOrUndefined = function (val) {
    return val == undefined || val == null || val.toString().trim().length == 0
}

isNullValue = function (val) {

    return val != undefined && (val == null || val.toString().trim().length == 0)
}