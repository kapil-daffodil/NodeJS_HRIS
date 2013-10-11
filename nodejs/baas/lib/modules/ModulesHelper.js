/**
 * Created with IntelliJ IDEA.
 * User: munish
 * Date: 21/9/13
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var MetadataProvider = require('../metadata/MetadataProvider.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var BaasError = require("apputil/ApplaneError.js");
var self = require("./ModulesHelper.js")

exports.collectColumnsInfo = function (columns, columnDataType, propertyToCollect) {
    var columnsInfo = {};
    columns.forEach(function (column) {
        var columnName = column[Constants.Baas.Tables.Columns.EXPRESSION];
        var columnType = column[Constants.Baas.Tables.Columns.TYPE];
        var multiple = column[Constants.Baas.Tables.Columns.MULTIPLE];
        if (columnDataType) {
            if (columnType == columnDataType) {
                if (propertyToCollect) {
                    columnsInfo[columnName] = column[propertyToCollect];
                } else {
                    columnsInfo[columnName] = column;
                }
            }
        } else {
            if (propertyToCollect) {
                columnsInfo[columnName] = column[propertyToCollect];
            } else {
                columnsInfo[columnName] = column;
            }
        }
        if ((columnType == Constants.Baas.Tables.Columns.Type.OBJECT || columnType == Constants.Baas.Tables.Columns.Type.LOOKUP) && column[Constants.Baas.Tables.COLUMNS]) {
            var nestedColumns = column[Constants.Baas.Tables.COLUMNS];
            var collectColumnsInfo = self.collectColumnsInfo(nestedColumns, columnDataType, propertyToCollect);
            collectColumnsInfo[Constants.Baas.Tables.Columns.TYPE] = columnType;
            collectColumnsInfo[Constants.Baas.Tables.Columns.MULTIPLE] = multiple;
            columnsInfo[columnName] = collectColumnsInfo;
        }
    });
    return columnsInfo;
}


exports.collectColumnsType = function (columns, nested) {
    var columnInfo = {};
    var length = columns.length;
    for (var i = 0; i < length; i++) {
        var column = columns[i];
        var columnName = column[Constants.Baas.Tables.Columns.EXPRESSION];
        var columnType = column[Constants.Baas.Tables.Columns.TYPE];
        if (columnType == Constants.Baas.Tables.Columns.Type.OBJECT || columnType == Constants.Baas.Tables.Columns.Type.LOOKUP) {
            var nestedColumns = column[Constants.Baas.Tables.COLUMNS];
            if (nestedColumns) {
                var collectedColumnInfo = self.collectColumnsType(nestedColumns, true);
                columnInfo[columnName] = collectedColumnInfo;
            }
        } else {
            columnInfo[columnName] = columnType;
        }
    }
    return columnInfo;
}