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
var ModuleHelper = require('./ModulesHelper.js');
var Moment = require('moment');


exports.afterQueryResult = function (table, query, result, context, callback) {
    //TODO Time Zone Work is still pending
    var requiredColumns = query[APIConstants.Query.COLUMNS] || [];
    var resultSet = result[APIConstants.Query.Result.DATA];
    var columns = table[Constants.Baas.Tables.SYSTEM_COLUMNS];
    var state = context.state;
    var defaultTimeZone = (-330 * 60 * 1000);
    var userSpecifiedTimeZone = state ? (state.timezone || defaultTimeZone) : defaultTimeZone;
    var collectedColumnsType = ModuleHelper.collectColumnsType(columns, false);
    updateDateColumn(requiredColumns, resultSet, collectedColumnsType, userSpecifiedTimeZone);
    callback();
}

function updateDateColumn(requiredColumns, resultSet, collectedColumnsType, userTimeZoneOffSet) {
    resultSet.forEach(function (record) {
        requiredColumns.forEach(function (column) {
            if (column instanceof Object) {
                var format = column[Constants.ModulesConstants.DateModule.Property.FORMAT];
                var nestedColumns = column[Constants.Baas.Tables.COLUMNS];
                var columnName = column[Constants.Baas.Tables.Columns.EXPRESSION];
                var columnValue = record[columnName];
                if (columnValue) {
                    if (format || nestedColumns) {
                        /*Check Date and DateTime Type Columns for Sub Documents*/
                        if (nestedColumns) {
                            var nestedResultSet = columnValue;
                            if (!(columnValue instanceof Array)) {
                                nestedResultSet = [];
                                nestedResultSet.push(columnValue);
                            }
                            var nestedColumnsDataType = collectedColumnsType[columnName];
                            if (nestedColumnsDataType) {
                                updateDateColumn(nestedColumns, nestedResultSet, nestedColumnsDataType, userTimeZoneOffSet);
                            }
                        } else if (format == Constants.ModulesConstants.DateModule.Property.Format.FROM_NOW) {
                            var timezoneOffset = columnValue.getTimezoneOffset() * 60 * 1000;
                            record[columnName] = (timezoneOffset == userTimeZoneOffSet) ? Moment(columnValue).fromNow() : Moment(columnValue).add("milliseconds", timezoneOffset).from(Moment(new Date()).add("milliseconds", timezoneOffset));
                        } else if (format == Constants.ModulesConstants.DateModule.Property.Format.CALENDAR_TIME) {
                            var timezoneOffset = columnValue.getTimezoneOffset() * 60 * 1000;
                            record[columnName] = (timezoneOffset == userTimeZoneOffSet) ? Moment(columnValue).calendar() : Moment(columnValue).add("milliseconds", timezoneOffset).subtract("milliseconds", userTimeZoneOffSet).calendar();
                        } else {
                            var timezoneOffset = columnValue.getTimezoneOffset() * 60 * 1000;
                            record[columnName] = (timezoneOffset == userTimeZoneOffSet) ? Moment(columnValue).format(format) : Moment(columnValue).add("milliseconds", timezoneOffset).subtract("milliseconds", userTimeZoneOffSet).format(format);
                        }
                    } else {
                        var dataType = collectedColumnsType[columnName];
                        if (columnValue && (dataType == Constants.Baas.Tables.Columns.Type.DATE || dataType == Constants.Baas.Tables.Columns.Type.DATETIME)) {
                            var defaultFormat = (dataType == Constants.Baas.Tables.Columns.Type.DATE) ? "YYYY-MM-DD" : "YYYY-MM-DD hh:mm:ss";
                            var timezoneOffset = columnValue.getTimezoneOffset() * 60 * 1000;
                            record[columnName] = (timezoneOffset == userTimeZoneOffSet) ? Moment(columnValue).format(defaultFormat) : Moment(columnValue).add("milliseconds", timezoneOffset).subtract("milliseconds", userTimeZoneOffSet).format(defaultFormat);
                        }
                    }
                }
            } else {
                var dataType = collectedColumnsType[column];
                var columnValue = record[column];
                if (columnValue && ( dataType == Constants.Baas.Tables.Columns.Type.DATE || dataType == Constants.Baas.Tables.Columns.Type.DATETIME)) {
                    var defaultFormat = (dataType == Constants.Baas.Tables.Columns.Type.DATE) ? "YYYY-MM-DD" : "YYYY-MM-DD hh:mm:ss";
                    var timezoneOffset = columnValue.getTimezoneOffset() * 60 * 1000;
                    record[column] = (timezoneOffset == userTimeZoneOffSet) ? Moment(columnValue).format(defaultFormat) : Moment(columnValue).add("milliseconds", timezoneOffset).subtract("milliseconds", userTimeZoneOffSet).format(defaultFormat);
                }
            }
        });
    });
}

/**
 * It will enable the module for table having column of date or date time type.
 * @param table
 * @param callback
 */
exports.syncTable = function (table, callback) {
//    var columns = table[Constants.Baas.Tables.COLUMNS] || [];
//    if (dateTypeColumnExist(columns)) {
    table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
    var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
    var length = enableModules.length;
    for (var i = 0; i < length; i++) {
        if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.DateModule.ID) {
            callback();
            return;
        }
    }
    var DateModule = {};
    DateModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.DateModule.ID;
    DateModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.DateModule.MODULE_PATH;
    enableModules.push(DateModule);
//    }
    callback();
}

function dateTypeColumnExist(columns) {
    var length = columns.length;
    if (length > 0) {
        for (var i = 0; i++; i < length) {
            var column = columns[i];
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnType == Constants.Baas.Tables.Columns.Type.DATE || columnType == Constants.Baas.Tables.Columns.Type.DATETIME) {
                return true;
            } else if (column[Constants.Baas.Tables.COLUMNS] && dateTypeColumnExist(column[Constants.Baas.Tables.COLUMNS])) {
                return true;
            }
        }
    }
    return false;
}