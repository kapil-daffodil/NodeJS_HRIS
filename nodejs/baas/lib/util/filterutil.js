/**
 * Filter options
 * {userid:1}
 * {userid:"{userid}"} , parameters : {"userid":1}
 * {userid:{$gt:1}}
 * {userid:{$gt:1},$OR:[ ]}
 *
 *
 */
var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var BaasError = require("apputil/ApplaneError.js");
var ObjectID = require('mongodb').ObjectID;
var Utils = require("apputil/util.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var DataBaseEngine = require('../database/DatabaseEngine.js');
var self = require("./filterutil.js")
var vm = require('vm');
var Moment = require('moment');


function resolve(condition, parameters) {
    if (condition) {
//        console.log("condition " + condition)
//        console.log(parameters)
        return vm.runInNewContext(condition, parameters);
    }
    return false;
}

exports.resolveFilters = function (filter, parameters, table, options, callback) {
    if (filter) {
//        console.log("filter >>>>>>>>>>>>" + JSON.stringify(filter));
        if ((filter instanceof Array) && !(filter instanceof Object)) {
            throw new BaasError(Constants.ErrorCode.NOT_VALID_FIELDS_IN_OBJECT, [APIConstants.Query.FILTER, "filter", filter]);
        }
        var keys = Object.keys(filter);
        Utils.iterateArray(keys, callback, function (key, callback) {

            if (key == APIConstants.Query.Filter.Operator.WHEN) {
                var when = filter[key];
                delete filter[key];
                var condition = when[APIConstants.Query.Filter.Operator.When.CONDITION];
                var innerFilter = when[APIConstants.Query.Filter.Operator.When.FILTER];
                if (resolve(condition, parameters) == true) {
                    self.resolveFilters(innerFilter, parameters, table, options, ApplaneCallback(callback, function () {
                        for (innerKey in innerFilter) {
                            filter[innerKey] = innerFilter[innerKey];
                        }
                        callback();
                    }));
                } else {
                    callback();
                }
                return;
            }

            if (table && isLookUp(key, table[Constants.Baas.Tables.SYSTEM_COLUMNS])) {
                var newKey = key + "." + APIConstants.Query._ID;
                filter[newKey] = filter[key];
                delete filter[key];
                key = newKey;
            }
            var isDateColumn = table && checkColumnType(key, table[Constants.Baas.Tables.SYSTEM_COLUMNS], [Constants.Baas.Tables.Columns.Type.DATE, Constants.Baas.Tables.Columns.Type.DATETIME]);
            var value = filter[key];
            if (value instanceof Object && value[APIConstants.Query._ID]) {
                value = filter[key] = value[APIConstants.Query._ID];
            }
            if (value instanceof Array) {
                Utils.iterateArray(value, callback, function (innerFilter, callback) {
                    self.resolveFilters(innerFilter, parameters, table, options, callback);
                });
            } else if (!(value instanceof ObjectID) && value instanceof Object) {
                var valueKeys = Object.keys(value);
                Utils.iterateArray(valueKeys, callback, function (opr, callback) {
                    if (opr == APIConstants.Query.Filter.Operator.QUERY) {
                        var innerQuery = value[opr];
                        getData(innerQuery, parameters, options, ApplaneCallback(callback, function (data) {
                            value[APIConstants.Query.Filter.Operator.IN] = data;
                            delete value[opr];
                            callback();
                        }));
                    } else {
                        var filterValue = value[opr];
                        if (typeof(filterValue) == "string" && isParameter(filterValue)) {
                            var paramName = filterValue.substr(1, filterValue.length - 2);
                            value[opr] = getParameterValue(paramName, parameters, options.user);
                        }
                        if (isKeyColumn(key) && value[opr]) {
                            if (value[opr] instanceof Array) {
                                var objectids = [];
                                for (var i = 0; i < value[opr].length; i++) {
                                    value[opr][i] = new ObjectID(value[opr][i].toString());
                                }
                            } else {
                                value[opr] = new ObjectID(value[opr].toString());
                            }
                        }

                        if (isDateColumn && value[opr]) {
                            if (value[opr] instanceof Array) {
                                for (var i = 0; i < value[opr].length; i++) {
                                    value[opr][i] = (value[opr][i] instanceof Date) ? value[opr][i] : new Date(value[opr][i]);
                                }
                            } else {
                                value[opr] = (value[opr] instanceof Date) ? value[opr] : new Date(value[opr]);
                            }
                        }
                        callback();
                    }
                })
            } else {
                if (typeof(value) == "string" && isParameter(value)) {
                    var paramName = value.substr(1, value.length - 2);
                    filter[key] = getParameterValue(paramName, parameters, options.user);
                }
                if (isKeyColumn(key) && filter[key]) {
                    filter[key] = new ObjectID(filter[key].toString());
                }
                if (isDateColumn && filter[key] && !(filter[key] instanceof Date)) {
                    var dateValue = filter[key];
                    var lValue = dateValue.split("-");
                    if (lValue.length == 3 && lValue[0] == "last") {
                        var n = lValue[1];
                        var type = lValue[2];
                        var startDate = Moment().subtract(type, n).toDate();
                        var endDate = Moment().subtract("days", 1).toDate();
                        filter[key] = {$gte:startDate, $lte:endDate};
                    } else {
                        filter[key] = new Date(dateValue);
                    }
                }
                callback();
            }
        });
    } else {
        callback();
    }
}

function isLookUp(expression, columns, replicateColumns) {
    var index = expression.indexOf(".");
    if (index != -1) {
        var firstPart = expression.substr(0, index);
        var length = columns ? columns.length : 0;
        for (var i = 0; i < length; i++) {
            var column = columns[i];
            var columnExpression = column[Constants.Baas.Tables.Columns.EXPRESSION];
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnExpression == firstPart && (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP || columnType == Constants.Baas.Tables.Columns.Type.OBJECT)) {
                var secondPart = expression.substr(index + 1);
                return isLookUp(secondPart, column[Constants.Baas.Tables.COLUMNS], column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS]);
            }
        }
    } else {
        var length = columns ? columns.length : 0;
        for (var i = 0; i < length; i++) {
            var column = columns[i];
            var columnExpression = column[Constants.Baas.Tables.Columns.EXPRESSION];
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnExpression == expression && (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP || columnType == Constants.Baas.Tables.Columns.Type.OBJECT) && !column[Constants.Baas.Tables.Columns.OPTIONS]) {
                return true;
            }
        }
    }
    if (replicateColumns) {
        return isLookUp(expression, replicateColumns);
    }
    return false;
}


function checkColumnType(expression, columns, columnTypes, replicateColumns) {
    var index = expression.indexOf(".");
    if (index != -1) {
        var firstPart = expression.substr(0, index);
        var length = columns ? columns.length : 0;
        for (var i = 0; i < length; i++) {
            var column = columns[i];
            var columnExpression = column[Constants.Baas.Tables.Columns.EXPRESSION];
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnExpression == firstPart && (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP || columnType == Constants.Baas.Tables.Columns.Type.OBJECT)) {
                var secondPart = expression.substr(index + 1);
                return checkColumnType(secondPart, column[Constants.Baas.Tables.COLUMNS], columnTypes, column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS]);
            }
        }
    } else {
        var length = columns ? columns.length : 0;
        for (var i = 0; i < length; i++) {
            var column = columns[i];
            var columnExpression = column[Constants.Baas.Tables.Columns.EXPRESSION];
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnExpression == expression && columnTypes.indexOf(columnType) != -1) {
                return true;
            }
        }
    }
    if (replicateColumns) {
        return checkColumnType(expression, replicateColumns, columnTypes);
    }
    return false;
}


function isParameter(value) {
    var pattern = new RegExp("^({).+(})$");
    return pattern.test(value);
}

function getParameterValue(paramName, parameters, user) {
    var paramValue;
    if (parameters[paramName]) {
        paramValue = parameters[paramName];
    } else if (paramName == '_CurrentUserId') {
        if (user) {
            paramValue = user._id.toString();
        }

    } else {
//        paramValue = CurrentRequest.getVariable(paramName);
    }
    if (paramValue instanceof Object && paramValue[APIConstants.Query._ID]) {
        return paramValue[APIConstants.Query._ID];
    }
    if (!paramValue && paramName) {
        var index = paramName.indexOf(".")
        if (index != -1) {
            var firstColumn = paramName.substring(0, index);
            if (firstColumn && parameters[firstColumn] && parameters[firstColumn] instanceof Object) {
                return getParameterValue(paramName.substring(index + 1), parameters[firstColumn], user);
            }
        }
    }
    return paramValue;
}

function isKeyColumn(key) {
    return key == APIConstants.Query._ID || /.+(\._id)$/.test(key);
}

function getData(query, parameters, options, mainCallback) {
    var queryParameters = query[APIConstants.Query.PARAMETERS] = query[APIConstants.Query.PARAMETERS] || {};
    for (param in parameters) {
        if (!queryParameters[param]) {
            queryParameters[param] = parameters[param];
        }
    }
    if (!query[APIConstants.Query.COLUMNS]) {
        query[APIConstants.Query.COLUMNS] = [APIConstants.Query._ID];
    }
    DataBaseEngine.query({
        ask:options.ask,
        osk:options.osk,
        usk:options.usk,
        user:options.user,
        query:query,
        callback:ApplaneCallback(mainCallback, function (result) {
            data = result.data;
            var mainColumn = query[APIConstants.Query.COLUMNS][0];
            mainColumn = mainColumn instanceof Object ? mainColumn[APIConstants.Query.Columns.EXPRESSION] : mainColumn
            var newArray = [];
            data.forEach(function (row) {
                var columnValue = row[mainColumn];
                if (columnValue) {
                    if (columnValue instanceof Array) {
                        columnValue.forEach(function (valueObject) {
                            if ((valueObject instanceof Object) && !(valueObject instanceof ObjectID)) {
                                newArray.push(valueObject[APIConstants.Query._ID]);
                            } else {
                                newArray.push(valueObject);
                            }
                        });
                    } else if ((columnValue instanceof Object) && !(columnValue instanceof ObjectID)) {
                        newArray.push(columnValue[APIConstants.Query._ID]);
                    } else {
                        newArray.push(columnValue);
                    }
                }
            });
            mainCallback(null, newArray);
        })
    });
}