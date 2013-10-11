/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 8/30/13
 * Time: 3:06 PM
 * To change this template use File | Settings | File Templates.
 */



var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var MetadataProvider = require('../metadata/MetadataProvider.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var DataBaseEngine = require('../database/DatabaseEngine.js');


function accumulateCurrency(expression, result) {
    var length = result.length;
    var sum = 0;
    var baseCurrency = null;
    for (var i = 0; i < length; i++) {
        var row = result[i];
        var colValue = row[expression];
        if (colValue) {
            var convertedValue = colValue[Constants.Baas.Tables.Columns.Type.Currency.CONVERTED_VALUE];
            baseCurrency = colValue[Constants.Baas.Tables.Columns.Type.Currency.BASE_CURRENCY];
            sum = sum + convertedValue;
        }
    }
    var accumulatedValue = {};
    sum = (sum % 100 == 0) ? parseInt(sum / 100) : (sum / 100);
    accumulatedValue[Constants.Baas.Tables.Columns.Type.Currency.AMOUNT] = sum;
    accumulatedValue[Constants.Baas.Tables.Columns.Type.Currency.TYPE] = baseCurrency;
    return accumulatedValue;
}

function accumulateDuration(expression, result) {
    var length = result.length;
    var sum = 0;
    for (var i = 0; i < length; i++) {
        var row = result[i];
        var colValue = row[expression];
        if (colValue) {
            var convertedValue = colValue[Constants.Baas.Tables.Columns.Type.Duration.CONVERTED_VALUE];
            sum = sum + convertedValue;
        }
    }
    var accumulatedValue = {};
    accumulatedValue[Constants.Baas.Tables.Columns.Type.Duration.TIME] = sum / 60;
    accumulatedValue[Constants.Baas.Tables.Columns.Type.Duration.TIME_UNIT] = Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.HRS;
    return accumulatedValue;
}

function accumulateValue(expression, result) {
    var sum = 0;
    var length = result.length;
    for (var i = 0; i < length; i++) {
        var row = result[i];
        var colValue = row[expression];
        if (colValue) {
            sum = sum + colValue;
        }
    }
    return sum;
}

function evaluateFunction(expression, columnDataType, function_, result) {
    var length = result.length;
    if (length == 0) {
        return null;
    }
    if (Constants.Baas.Tables.Columns.TotalAggregates.SUM == function_) {
        if (columnDataType == Constants.Baas.Tables.Columns.Type.CURRENCY) {
            return accumulateCurrency(expression, result);
        } else if (columnDataType == Constants.Baas.Tables.Columns.Type.DURATION) {
            return accumulateDuration(expression, result);
        } else {
            return accumulateValue(expression, result);
        }
    } else if (Constants.Baas.Tables.Columns.TotalAggregates.AVG == function_) {
        return 0;
    } else if (Constants.Baas.Tables.Columns.TotalAggregates.COUNT == function_) {
        return 0;
    }
    return null;
}

function collectColumnsType(columns, nested) {
    var columnInfo = {};
    var length = columns.length;
    for (var i = 0; i < length; i++) {
        var column = columns[i];
        var columnName = column[Constants.Baas.Tables.Columns.EXPRESSION];
        var columnType = column[Constants.Baas.Tables.Columns.TYPE];
        if (columnType == Constants.Baas.Tables.Columns.Type.OBJECT || columnType == Constants.Baas.Tables.Columns.Type.LOOKUP) {
            var nestedColumns = column[Constants.Baas.Tables.COLUMNS];
            if (nestedColumns) {
                var collectedColumnInfo = collectColumnsType(nestedColumns, true);
                columnInfo[columnName] = collectedColumnInfo;
            }
        } else if (nested && (columnType == Constants.Baas.Tables.Columns.Type.CURRENCY || columnType == Constants.Baas.Tables.Columns.Type.DURATION)) {
            columnInfo[columnName] = columnType;
        }
    }
    return columnInfo;
}

function evaluateFunctions(columns, columnsTypeInfo, resultSet, callback, nested) {
    var functionalColumns = [];
    var functionalResult = {};
    columns.forEach(function (columnInfo) {
        if (columnInfo instanceof Object) {
            var function_ = columnInfo[Constants.Baas.Tables.Columns.TOTAL_AGGREGATES];
            var nestedColumns = columnInfo[Constants.Baas.Tables.COLUMNS];
            var expression = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
            if (function_) {
                if (nested) {
                    var columnDataType = columnsTypeInfo[expression];
                    var evaluated = evaluateFunction(expression, columnDataType, function_, resultSet);
                    functionalResult[expression] = evaluated;
                } else {
                    columnInfo[APIConstants.Query.Columns.AGGERGATE] = columnInfo[Constants.Baas.Tables.Columns.TOTAL_AGGREGATES];
                    delete columnInfo[Constants.Baas.Tables.Columns.TOTAL_AGGREGATES];
                    functionalColumns.push(columnInfo);
                }
            } else if (nestedColumns && columnInfo[Constants.Baas.Tables.Columns.MULTIPLE]) {
                var nestedColumnsTypeInfo = columnsTypeInfo[expression];
                resultSet.forEach(function (row) {
                    var moduleResult = row[APIConstants.Query.Result.MODULE_RESULT];
                    if (!moduleResult) {
                        moduleResult = {};
                        row[APIConstants.Query.Result.MODULE_RESULT] = moduleResult;
                    }
                    var nestedResult = row[expression];
                    var rowPK = row[APIConstants.Query._ID];
                    if (nestedResult) {
                        evaluateFunctions(nestedColumns, nestedColumnsTypeInfo, nestedResult, function (rowFunctionalResult) {
                            moduleResult[expression] = {};
                            moduleResult[expression][APIConstants.Query.Result.ModuleResult.AGGREGATES] = rowFunctionalResult;

                        }, true);
                    }
                });
            }
        }
    });
    if (!nested) {
        callback(functionalColumns);
    } else {
        callback(functionalResult);
    }
}

function mergeFunctionsEvaluations(to, from) {
    for (var key in from) {
        to[key] = from[key];
    }
}

function getBaseCurrency(context, callback) {
    var ask = context.ask;
    var osk = context.osk;
    var baseCurrency = {};
    baseCurrency[Constants.Baas.Currencies.CURRENCY] = "USD";
    baseCurrency[APIConstants.Query._ID] = "5231665a7fa1deb810000002";
    if (osk) {
        MetadataProvider.getOrganization(osk, ApplaneCallback(callback, function (organization) {
            if (organization && organization.length > 0 && organization[0][Constants.Baas.Organizations.BASE_CURRENCY]) {
                baseCurrency = organization[0][Constants.Baas.Organizations.BASE_CURRENCY];
                callback(null, baseCurrency);
            } else {
                MetadataProvider.getApplication(ask, ApplaneCallback(callback, function (application) {
                    if (application && application.length > 0 && application[0][Constants.Baas.Organizations.BASE_CURRENCY]) {
                        baseCurrency = application[0][Constants.Baas.Organizations.BASE_CURRENCY];
                    }
                    callback(null, baseCurrency);
                }));
            }
        }));
    } else {
        MetadataProvider.getApplication(ask, ApplaneCallback(callback, function (application) {
            if (application && application.length > 0 && application[0][Constants.Baas.Organizations.BASE_CURRENCY]) {
                baseCurrency = application[0][Constants.Baas.Organizations.BASE_CURRENCY];
            }
            callback(null, baseCurrency);
        }));
    }
}

exports.afterQueryResult = function (table, query, result, context, callback) {
    var requiredColumns = query[APIConstants.Query.COLUMNS] || [];
//    console.log("required columns are : " + JSON.stringify(requiredColumns));
    var resultSet = result[APIConstants.Query.Result.DATA];
    var columns = table[Constants.Baas.Tables.COLUMNS];
    var columnsType = collectColumnsType(columns, false);
    evaluateFunctions(requiredColumns, columnsType, resultSet, function (functionalColumns) {
        if (functionalColumns && functionalColumns.length > 0) {
            var aggregateQuery = {};
            aggregateQuery[APIConstants.Query.TABLE] = query[APIConstants.Query.TABLE];
            aggregateQuery[APIConstants.Query.COLUMNS] = functionalColumns;
            aggregateQuery[APIConstants.Query.FILTER] = query[APIConstants.Query.FILTER];
            aggregateQuery[APIConstants.Query.PARAMETERS] = query[APIConstants.Query.PARAMETERS];
            DataBaseEngine.query({
                ask:context.ask,
                osk:context.osk,
                usk:context.usk,
                user:context.user,
                query:aggregateQuery,
                callback:ApplaneCallback(callback, function (aggregateData) {
                    if (aggregateData.data && aggregateData.data.length > 0) {
                        pushAggregateResult(result, aggregateData.data[0]);
                    }
                    callback();
                })
            });
        } else {
            callback();
        }
    }, false);
}

function pushAggregateResult(result, data) {
    var moduleResult = result[APIConstants.Query.Result.MODULE_RESULT];
    if (!moduleResult) {
        moduleResult = {};
        result[APIConstants.Query.Result.MODULE_RESULT] = moduleResult;
    }
    var moduleResultData = moduleResult.data;
    if (!moduleResultData) {
        moduleResult.data = moduleResultData = {};
    }
    moduleResultData[APIConstants.Query.Result.ModuleResult.AGGREGATES] = data;
}

exports.syncTable = function (table, callback) {
    table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
    var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
    var length = enableModules.length;
    for (var i = 0; i < length; i++) {
        if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.AggregateModule.ID) {
            callback();
            return;
        }
    }
    var aggregateModule = {};
    aggregateModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.AggregateModule.ID;
    aggregateModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.AggregateModule.MODULE_PATH;
    enableModules.push(aggregateModule);
    callback();
}