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
var DataBaseEngine = require('../database/DatabaseEngine.js');
var Utils = require("apputil/util.js");
var RequestAuthenticator = require("../server/RequestAuthenticator.js")

exports.beforeQuery = function (table, query, context, callback) {
    var systemColumns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
    ensureColumns(systemColumns);
    var columns = query[Constants.Baas.Tables.COLUMNS] || [];
    changeDurationOrCurrencyColumnForAggregate(columns);
    changeOrdersForIndexColumn(query, columns);
    callback();
}

exports.afterQueryResult = function (table, query, result, context, callback) {
    query[Constants.Baas.Tables.COLUMNS] = query[Constants.Baas.Tables.COLUMNS] || [];
    var columns = query[Constants.Baas.Tables.COLUMNS];
    getBaseAndAllowedCurrency(context, ApplaneCallback(callback, function (currencies) {
        convertDurationOrCurrencyColumnValue(columns, result[APIConstants.Query.Result.DATA], currencies.basecurrency);
        callback();
    }))


}

exports.beforeInsert = function (table, operations, context, callback) {
    try {
        var systemColumns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
        ensureColumns(systemColumns);
        var columns = table[Constants.Baas.Tables.COLUMNS] || [];
        validateColumnsProperties(columns, operations);
        populateConvertedTimeAndIndex(columns, operations);
        getBaseAndAllowedCurrency(context, ApplaneCallback(callback, function (currencies) {
            convertCurrencyInBaseCurrency(columns, operations, currencies.basecurrency, callback);
        }))
    } catch (err) {
        callback(err);
    }
}

exports.beforeUpdate = function (table, operations, context, callback) {
    try {
        var systemColumns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
        ensureColumns(systemColumns);
        var columns = table[Constants.Baas.Tables.COLUMNS] || [];
        validateColumnsProperties(columns, operations);
        populateConvertedTimeAndIndex(columns, operations);
        getBaseAndAllowedCurrency(context, ApplaneCallback(callback, function (currencies) {
            convertCurrencyInBaseCurrency(columns, operations, currencies.basecurrency, callback);
        }))
    } catch (err) {
        callback(err);
    }
}

function changeOrdersForIndexColumn(query, columns) {
    var orders = query[APIConstants.Query.ORDERS];
    if (orders) {
        var indexTypeColumns = [];
        for (var i = 0; i < columns.length; i++) {
            var column = columns[i];
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnType == Constants.Baas.Tables.Columns.Type.INDEX) {
                indexTypeColumns.push(column[Constants.Baas.Tables.Columns.EXPRESSION]);
            }
        }
        if (indexTypeColumns.length > 0) {
            for (var i = 0; i < indexTypeColumns.length; i++) {
                var indexColumnExp = indexTypeColumns[i];
                if (orders instanceof Object && !(orders instanceof Array)) {
                    if (orders[indexColumnExp]) {
                        var orderType = orders[indexColumnExp];
                        orders = [];
                        var indexObj = {};
                        indexObj[indexColumnExp + "." + Constants.Baas.Tables.Columns.Type.Index.INDEX] = orderType;
                        orders.push(indexObj);
                        var subIndexObj = {};
                        subIndexObj[indexColumnExp + "." + Constants.Baas.Tables.Columns.Type.Index.SUB_INDEX] = orderType;
                        orders.push(subIndexObj);
                        query[APIConstants.Query.ORDERS] = orders;
                    }
                } else if (orders instanceof Array) {
                    var newOrders = [];
                    for (var j = 0; j < orders.length; j++) {
                        var order = orders[j];
                        if (order[indexColumnExp]) {
                            var orderType = order[indexColumnExp];
                            var indexObj = {};
                            indexObj[indexColumnExp + "." + Constants.Baas.Tables.Columns.Type.Index.INDEX] = orderType;
                            newOrders.push(indexObj);
                            var subIndexObj = {};
                            subIndexObj[indexColumnExp + "." + Constants.Baas.Tables.Columns.Type.Index.SUB_INDEX] = orderType;
                            newOrders.push(subIndexObj);
                        } else {
                            newOrders.push(order);
                        }
                    }
                    orders = newOrders;
                    query[APIConstants.Query.ORDERS] = newOrders;
                }
            }
        }
    }
}

function convertCurrencyInBaseCurrency(columns, operations, baseCurrency, callback) {
    Utils.iterateArray(operations, callback, function (operation, callback) {
        var insertOp = operation._id ? false : true;
        if (operation.__type__ && operation.__type__ == 'delete') {
            callback();
        } else {
            Utils.iterateArray(columns, callback, function (columnInfo, callback) {
                var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
                var columnExpression = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
                var columnValue = operation[columnExpression];
                if (columnType == Constants.Baas.Tables.Columns.Type.CURRENCY) {
                    if (columnValue) {
                        var amount = columnValue[Constants.Baas.Tables.Columns.Type.Currency.AMOUNT];
                        var amountType = columnValue[Constants.Baas.Tables.Columns.Type.Currency.TYPE];
                        if (amountType[Constants.Baas.Currencies.CURRENCY] == baseCurrency[Constants.Baas.Currencies.CURRENCY]) {
                            var convertedValue = amount * 1 * 100;
                            columnValue[Constants.Baas.Tables.Columns.Type.Currency.CONVERTED_VALUE] = convertedValue;
                            columnValue[Constants.Baas.Tables.Columns.Type.Currency.BASE_CURRENCY] = baseCurrency;
                            columnValue[Constants.Baas.Tables.Columns.Type.Currency.CONVERSION_RATE] = 1;
                            callback();
                        } else {
                            getConversionRate(amountType[Constants.Baas.Currencies.CURRENCY], baseCurrency[Constants.Baas.Currencies.CURRENCY], ApplaneCallback(callback, function (conversionRate) {
                                if (!conversionRate || conversionRate.length == 0) {
                                    callback();
                                } else {
                                    var convertedValue = ((amount * conversionRate).toFixed(2)) * 100;
                                    columnValue[Constants.Baas.Tables.Columns.Type.Currency.CONVERTED_VALUE] = parseInt(convertedValue);
                                    columnValue[Constants.Baas.Tables.Columns.Type.Currency.BASE_CURRENCY] = baseCurrency;
                                    columnValue[Constants.Baas.Tables.Columns.Type.Currency.CONVERSION_RATE] = conversionRate;
                                    callback();
                                }
                            }));
                        }
                    } else {
                        callback();
                    }
                } else {
                    if (columnValue && columnInfo[Constants.Baas.Tables.COLUMNS]) {
                        convertCurrencyInBaseCurrency(columnInfo[Constants.Baas.Tables.COLUMNS], (columnValue instanceof Array ? columnValue : [columnValue]), baseCurrency, callback)
                    } else {
                        callback();
                    }
                }
            });
        }
    })
}

function getConversionRate(fromCurrency, toCurrency, callback) {
    var filter = {};
    filter[Constants.Baas.CurrencyConversionRates.FROM] = fromCurrency;
    filter[Constants.Baas.CurrencyConversionRates.TO] = toCurrency;
    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.CURRENCY_CONVERSION_RATES;
    query[APIConstants.Query.COLUMNS] = [Constants.Baas.CurrencyConversionRates.RATE];
    query[APIConstants.Query.FILTER] = filter;
    DataBaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data[0][Constants.Baas.CurrencyConversionRates.RATE]);
            } else {
                callback();
            }
        })
    })
}

function getBaseAndAllowedCurrency(context, callback) {
    var ask = context.ask;
    var osk = context.osk;
    var baseCurrency = {};
    baseCurrency[Constants.Baas.Currencies.CURRENCY] = "USD";
    baseCurrency[APIConstants.Query._ID] = "5231665a7fa1deb810000002";
    var allowedCurrencies;
    RequestAuthenticator.authenticate(ask, osk, ApplaneCallback(callback, function (data) {
        var application = data.application;
        var organization = data.organization;
        if (organization && organization[Constants.Baas.Organizations.BASE_CURRENCY]) {
            baseCurrency = organization[Constants.Baas.Organizations.BASE_CURRENCY];
            allowedCurrencies = organization[Constants.Baas.Organizations.ALLOWED_CURRENCIES];
        } else if (application && application[Constants.Baas.Applications.BASE_CURRENCY]) {
            baseCurrency = application[Constants.Baas.Applications.BASE_CURRENCY];
            allowedCurrencies = organization[Constants.Baas.Applications.ALLOWED_CURRENCIES]
        }
        var currencies = {};
        currencies.basecurrency = baseCurrency;
        if (allowedCurrencies) {
            currencies.allowedcurrencies = allowedCurrencies;
        }
        callback(null, currencies);
    }));
}

function convertDurationOrCurrencyColumnValue(columns, result, baseCurrency) {
    columns.forEach(function (columnInfo) {
        var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
        var columnExpression = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
        result.forEach(function (dataInfo) {
            if (columnInfo[APIConstants.Query.Columns.AGGERGATE] && columnType == Constants.Baas.Tables.Columns.Type.DURATION) {
                var columnValue = dataInfo[columnExpression];
                var newValue = {};
                newValue[Constants.Baas.Tables.Columns.Type.Duration.TIME] = columnValue / 60;
                newValue[Constants.Baas.Tables.Columns.Type.Duration.TIME_UNIT] = Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.HRS;
                dataInfo[columnExpression] = newValue;
            } else if (columnInfo[APIConstants.Query.Columns.AGGERGATE] && columnType == Constants.Baas.Tables.Columns.Type.CURRENCY) {
                var convertedValue = dataInfo[columnExpression];
                if (!(convertedValue instanceof Object)) {
                    var amount = (convertedValue % 100 == 0) ? parseInt(convertedValue / 100) : (convertedValue / 100);
                    var newValue = {};
                    newValue[Constants.Baas.Tables.Columns.Type.Currency.AMOUNT] = amount;
                    newValue[Constants.Baas.Tables.Columns.Type.Currency.TYPE] = baseCurrency;
                    dataInfo[columnExpression] = newValue;
                }
            }
        });
    });
}

function changeDurationOrCurrencyColumnForAggregate(columns) {
    columns.forEach(function (columnInfo) {
        var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
        var columnExpression = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
        if (columnInfo[APIConstants.Query.Columns.AGGERGATE] && columnType == Constants.Baas.Tables.Columns.Type.DURATION) {
            columnInfo[Constants.Baas.Tables.Columns.ALIAS] = columnExpression;
            columnInfo[Constants.Baas.Tables.Columns.EXPRESSION] = columnExpression + "." + Constants.Baas.Tables.Columns.Type.Duration.CONVERTED_VALUE;
            columnInfo[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.DECIMAL;
        } else if (columnInfo[APIConstants.Query.Columns.AGGERGATE] && columnType == Constants.Baas.Tables.Columns.Type.CURRENCY) {
            columnInfo[Constants.Baas.Tables.Columns.ALIAS] = columnExpression;
            columnInfo[Constants.Baas.Tables.Columns.EXPRESSION] = columnExpression + "." + Constants.Baas.Tables.Columns.Type.Currency.CONVERTED_VALUE;
            columnInfo[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.DECIMAL;
        }
    });
}

function populateConvertedTimeAndIndex(columns, operations) {
    operations.forEach(function (operation) {
            var insertOp = operation._id ? false : true;
            if (operation.__type__ && operation.__type__ == 'delete') {
                return;
            }
            columns.forEach(function (columnInfo) {
                var columnExpression = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
                var columnValue = operation[columnExpression];
                var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
                if (columnType == Constants.Baas.Tables.Columns.Type.DURATION) {
                    if (columnValue) {
                        var time = columnValue[Constants.Baas.Tables.Columns.Type.Duration.TIME];
                        var timeUnit = columnValue[Constants.Baas.Tables.Columns.Type.Duration.TIME_UNIT];
                        var timeInMins = convertTime(time, timeUnit);
                        columnValue[Constants.Baas.Tables.Columns.Type.Duration.CONVERTED_VALUE] = timeInMins;
                    }
                } else if (columnType == Constants.Baas.Tables.Columns.Type.INDEX) {
                    if (insertOp) {
                        if (columnValue) {
                            if (columnValue instanceof Object) {
                                if (!columnValue[Constants.Baas.Tables.Columns.Type.Index.INDEX]) {
                                    columnValue[Constants.Baas.Tables.Columns.Type.Index.INDEX] = 1;
                                }
                                if (!columnValue[Constants.Baas.Tables.Columns.Type.Index.SUB_INDEX]) {
                                    columnValue[Constants.Baas.Tables.Columns.Type.Index.SUB_INDEX] = new Date().getTime();
                                }
                            } else {
                                var newColumnValue = {};
                                newColumnValue[Constants.Baas.Tables.Columns.Type.Index.INDEX] = parseInt(columnValue) % 10 == 0 ? parseInt(columnValue) : parseFloat(columnValue);
                                newColumnValue[Constants.Baas.Tables.Columns.Type.Index.SUB_INDEX] = new Date().getTime();
                                columnValue = newColumnValue;
                                operation[columnExpression] = newColumnValue;
                            }
                        } else {
                            columnValue = {};
                            columnValue[Constants.Baas.Tables.Columns.Type.Index.INDEX] = 1;
                            columnValue[Constants.Baas.Tables.Columns.Type.Index.SUB_INDEX] = new Date().getTime();
                            operation[columnExpression] = columnValue;
                        }
                    } else {
                        if (columnValue != undefined && !(columnValue instanceof Object)) {
                            var newColumnValue = {};
                            if (columnValue.length > 0) {
                                newColumnValue[Constants.Baas.Tables.Columns.Type.Index.INDEX] = parseInt(columnValue) % 10 == 0 ? parseInt(columnValue) : parseFloat(columnValue);
                            } else {
                                newColumnValue[Constants.Baas.Tables.Columns.Type.Index.INDEX] = 1;
                            }
                            columnValue = newColumnValue;
                            operation[columnExpression] = newColumnValue;
                        }
                    }
                }
                if (columnValue && columnInfo[Constants.Baas.Tables.COLUMNS]) {
                    populateConvertedTimeAndIndex(columnInfo[Constants.Baas.Tables.COLUMNS], (columnValue instanceof Array ? columnValue : [columnValue]))
                }
            });
        }
    )
}

function convertTime(time, timeUnit) {
    if (timeUnit == Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.HRS) {
        return time * 60;
    } else if (timeUnit == Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.MINUTES) {
        return time;
    } else if (timeUnit == Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.DAYS) {
        return time * 8 * 60;
    }
}

function ensureColumns(columns) {
    columns.forEach(function (columnInfo) {
        var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
        var column = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
        if (columnType == Constants.Baas.Tables.Columns.Type.PHONE_NO || columnType == Constants.Baas.Tables.Columns.Type.EMAILID) {
            columnInfo[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.STRING;
        } else if (columnType == Constants.Baas.Tables.Columns.Type.DURATION) {
            columnInfo[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.OBJECT;
            var innerColumns = [];
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Duration.TIME, Constants.Baas.Tables.Columns.Type.DECIMAL, true);
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Duration.TIME_UNIT, Constants.Baas.Tables.Columns.Type.LOOKUP, true, [Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.HRS, Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.MINUTES, Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.DAYS]);
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Duration.CONVERTED_VALUE, Constants.Baas.Tables.Columns.Type.DECIMAL, true);
            columnInfo[Constants.Baas.Tables.COLUMNS] = innerColumns;
        } else if (columnType == Constants.Baas.Tables.Columns.Type.CURRENCY) {
            columnInfo[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.OBJECT;
            var innerColumns = [];
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Currency.AMOUNT, Constants.Baas.Tables.Columns.Type.DECIMAL, true);
            var table = {};
            table[Constants.Baas.Tables.ID] = Constants.Baas.CURRENCIES;
            var primaryColumns = [];
            var primaryColumn = {};
            primaryColumn[Constants.Baas.Tables.Columns.EXPRESSION] = Constants.Baas.Currencies.CURRENCY;
            primaryColumn[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.STRING;
            primaryColumns.push(primaryColumn);
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Currency.TYPE, Constants.Baas.Tables.Columns.Type.LOOKUP, true, null, table, primaryColumns, primaryColumns);
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Currency.CONVERTED_VALUE, Constants.Baas.Tables.Columns.Type.DECIMAL, true);
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Currency.BASE_CURRENCY, Constants.Baas.Tables.Columns.Type.LOOKUP, true, null, table, primaryColumns, primaryColumns);
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Currency.CONVERSION_RATE, Constants.Baas.Tables.Columns.Type.DECIMAL, true);
            columnInfo[Constants.Baas.Tables.COLUMNS] = innerColumns;
        } else if (columnType == Constants.Baas.Tables.Columns.Type.INDEX) {
            columnInfo[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.OBJECT;
            var innerColumns = [];
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Index.INDEX, Constants.Baas.Tables.Columns.Type.DECIMAL, true);
            addColumn(innerColumns, Constants.Baas.Tables.Columns.Type.Index.SUB_INDEX, Constants.Baas.Tables.Columns.Type.DECIMAL, true);
            columnInfo[Constants.Baas.Tables.COLUMNS] = innerColumns;
        } else if (columnInfo.columns) {
            ensureColumns(columnInfo.columns);
        }
    });
}

function addColumn(columns, expression, columnType, mandatory, options, table, primaryColumns, replicateColumns) {
    var column = {};
    column[Constants.Baas.Tables.Columns.EXPRESSION] = expression;
    column[Constants.Baas.Tables.Columns.TYPE] = columnType;
    if (mandatory) {
        column[Constants.Baas.Tables.Columns.MANDATORY] = true;
    }
    if (options) {
        column[Constants.Baas.Tables.Columns.OPTIONS] = options;
    }
    if (table) {
        column[Constants.Baas.Tables.Columns.TABLE] = table;
    }
    if (primaryColumns) {
        column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] = primaryColumns;
    }
    if (replicateColumns) {
        column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS] = replicateColumns;
    }
    columns.push(column);
}

/**
 * It will enable the module for all tables.
 * @param table
 * @param callback
 */
exports.syncTable = function (table, callback) {
    if (udtColumnExists(table)) {
        table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
        var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
        var length = enableModules.length;
        for (var i = 0; i < length; i++) {
            if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.UDTModule.ID) {
                callback();
                return;
            }
        }
        var UDTModule = {};
        UDTModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.UDTModule.ID;
        UDTModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.UDTModule.MODULE_PATH;
        enableModules.push(UDTModule);
    }
    callback();
}

function validateColumnsProperties(columns, operations) {
    operations.forEach(function (operation) {
            var insertOp = operation._id ? false : true;
            if (operation.__type__ && operation.__type__ == 'delete') {
                return;
            }
            columns.forEach(function (columnInfo) {
                var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
                var column = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
                var multiple = columnInfo[Constants.Baas.Tables.Columns.MULTIPLE];
                var columnValue = operation[column];
                if (multiple && columnValue instanceof Array) {
                    columnValue.forEach(function (colValue) {
                        validateDataType(colValue, columnType);
                    })
                } else {
                    /*Code to enforce data type constraints*/
                    validateDataType(columnValue, columnType);
                }
                if (columnValue && columnInfo[Constants.Baas.Tables.COLUMNS]) {
                    validateColumnsProperties(columnInfo[Constants.Baas.Tables.COLUMNS], (columnValue instanceof Array ? columnValue : [columnValue]))
                }
            });
        }
    )
}

function validateDataType(columnValue, columnType) {
    if (columnValue) {
        var dataType = getDataType(columnValue);
        /*Validate Data Type as well as Email format*/
        if (columnType == Constants.Baas.Tables.Columns.Type.EMAILID) {
            if (!(dataType.toLowerCase() == Constants.Baas.Tables.Columns.Type.STRING && isEmailId(columnValue))) {
                throw new BaasError(Constants.ErrorCode.INVALID_DATA_TYPE, ['Invalid email id format.']);
            }
        }
        if (columnType == Constants.Baas.Tables.Columns.Type.PHONE_NO) {
            if (!(dataType.toLowerCase() == Constants.Baas.Tables.Columns.Type.STRING && isPhoneNumber(columnValue))) {
                throw new BaasError(Constants.ErrorCode.INVALID_DATA_TYPE, ['Invalid phone number format.']);
            }
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
    return typeof data === 'number' && parseFloat(data) == parseInt(data, 10) && !isNaN(data);
}

isNullOrUndefined = function (val) {
    return val == undefined || val == null || val.toString().trim().length == 0
}

isNullValue = function (val) {

    return val != undefined && (val == null || val.toString().trim().length == 0)
}

/**
 * Function to validate emailId
 * <b>Supported formats are.</b>
 * +xx xxxx xxxxxx, +xx-xxxx-xxxxxx, +xx.xxxx.xxxxxx
 * @param inputtxt  phone number to validate
 * @return {*}
 */
function isPhoneNumber(inputTxt) {
    var phoneno = /^\+?([0-9]{2})\)?[-. ]?([0-9]{4})[-. ]?([0-9]{6})$/;
    return new RegExp(phoneno).test(inputTxt)
}

/**
 * Function to validate emailid
 * @param inputTxt  EmailId to validate
 * @return {*}
 */
function isEmailId(inputTxt) {
    var email = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/;
    return new RegExp(email).test(inputTxt)
}


function udtColumnExists(table) {
    var columns = table[Constants.Baas.Tables.COLUMNS] || [];
    var length = columns.length;
    for (var i = 0; i < length; i++) {
        var columnInfo = columns[i];
        var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
        if (columnType == Constants.Baas.Tables.Columns.Type.PHONE_NO || columnType == Constants.Baas.Tables.Columns.Type.EMAILID || columnType == Constants.Baas.Tables.Columns.Type.DURATION || columnType == Constants.Baas.Tables.Columns.Type.CURRENCY || columnType == Constants.Baas.Tables.Columns.Type.INDEX) {
            return true;
        }
    }
    return false;
}

exports.getConversionRates = function (options, callback) {
    var conversionRate = {};
    var durationConversionRate = {};
    durationConversionRate.baseunit = Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.HRS;
    durationConversionRate.leafunit = 60;
    var durationConversionValue = {};
    durationConversionValue[Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.DAYS] = 8;
    durationConversionValue[Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.HRS] = 1;
    durationConversionValue[Constants.Baas.Tables.Columns.Type.Duration.TimeUnits.MINUTES] = 0.0166667;
    durationConversionRate.conversionvalue = durationConversionValue;
    conversionRate.duration = durationConversionRate;
    var ask = options.ask;
    var osk = options.osk;
    if (ask) {
        var params = {};
        params.ask = ask;
        params.osk = osk;
        getBaseAndAllowedCurrency(params, ApplaneCallback(callback, function (currencies) {
            var baseCurrency = currencies.basecurrency;
            var allowedCurrencies = currencies.allowedcurrencies;
            getCurrencyConversionValues(baseCurrency, allowedCurrencies, ApplaneCallback(callback, function (conversionValues) {
                var currencyConversionRate = {};
                currencyConversionRate.baseunit = baseCurrency;
                currencyConversionRate.leafunit = 100;
                currencyConversionRate.conversionvalue = conversionValues;
                conversionRate.currency = currencyConversionRate;
                callback(null, {"conversionrate":conversionRate});
            }))

        }))
    } else {
        callback(null, {"conversionrate":conversionRate});
    }
}

function getCurrencyConversionValues(baseCurrency, allowedCurrencies, callback) {
    var filter = {};
    filter[Constants.Baas.CurrencyConversionRates.FROM] = baseCurrency[Constants.Baas.Currencies.CURRENCY];
    if (allowedCurrencies && allowedCurrencies.length > 0) {
        var allowedCurrenciesArray = [];
        for (var i = 0; i < allowedCurrencies.length; i++) {
            var allowedCurrency = allowedCurrencies[i];
            allowedCurrenciesArray.push(allowedCurrency[Constants.Baas.Currencies.CURRENCY]);
        }
        filter[Constants.Baas.CurrencyConversionRates.TO] = {$in:allowedCurrenciesArray};
    }
    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.CURRENCY_CONVERSION_RATES;
    query[APIConstants.Query.COLUMNS] = [Constants.Baas.CurrencyConversionRates.RATE, Constants.Baas.CurrencyConversionRates.TO];
    query[APIConstants.Query.FILTER] = filter;
    DataBaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                var conversionValues = {};
                for (var i = 0; i < data.length; i++) {
                    var row = data[i];
                    conversionValues[row[Constants.Baas.CurrencyConversionRates.TO]] = row[Constants.Baas.CurrencyConversionRates.RATE];
                }
                callback(null, conversionValues);
            } else {
                callback();
            }
        })
    })
}
