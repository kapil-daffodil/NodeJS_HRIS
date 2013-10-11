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
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var NodejsAPI = require("nodejsapi");
var MetadataProvider = require('../../metadata/MetadataProvider.js');


exports.beforeInsert = function (params, callback) {

    Utils.iterateArray(params.operations, callback, function (operation, callback) {
        operation[Constants.Baas.Tables.Columns.EXPRESSION] = operation[Constants.Baas.Flexfields.LABEL];
        callback();
    });


}

exports.beforeUpdate = function (params, callback) {
    console.log("Before updated called..." + JSON.stringify(params));

    Utils.iterateArray(params.operations, callback, function (operation, callback) {
        if (operation[Constants.Baas.Flexfields.LABEL]) {
            operation[Constants.Baas.Tables.Columns.EXPRESSION] = operation[Constants.Baas.Flexfields.LABEL];
        }
        callback();

    });


}


exports.afterInsert = function (params, callback) {
    processValue(params,callback);
}

function processValue (params, callback){

    Utils.iterateArray(params.newvalues, callback, function (flexfield, callback) {
        var flexTable = flexfield[Constants.Baas.Flexfields.TABLE];
        var flexValue = flexfield[Constants.Baas.Flexfields.COLUMNVALUE];
        var organization = flexfield[Constants.Baas.Flexfields.ORGANIZATION];
        var query = {};
        query[APIConstants.Query.TABLE] = Constants.Baas.FLEXFIELDS;
        var filter = {};
        filter[Constants.Baas.Flexfields.TABLE] = flexTable;
        filter[Constants.Baas.Flexfields.COLUMNVALUE] = flexValue;
        if (organization) {
            filter[Constants.Baas.Flexfields.ORGANIZATION] = organization;
        }
        query[APIConstants.Query.FILTER] = filter;

        NodejsAPI.asQuery(query,{ask:Constants.Baas.ASK}).execute(ApplaneCallback(callback,function(data){
            var options = {ask:params.ask, osk:params.osk, usk:params.usk};
            syncLookupReplicateColumns(data.data, options, ApplaneCallback(callback, function () {
                var update = NodejsAPI.asUpdate(Constants.Baas.FLEXFIELDS, {"excludejobs":true, ask:params.ask, osk:params.osk, usk:params.usk});
                data.data.forEach(function (record) {
                    update.update(record[APIConstants.Query._ID], record);
                })
                update.save(callback);
            }));
        }));

    });


}

exports.afterUpdate = function (params, callback) {
    processValue(params,callback);
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

function syncLookupReplicateColumns(columns, options, callback) {
    Utils.iterateArray(columns, ApplaneCallback(callback, function () {
        handleDotedColumns(columns);
        callback();
    }), function (column, callback) {
        var type = column[Constants.Baas.Tables.Columns.TYPE];
        if (type == Constants.Baas.Tables.Columns.Type.LOOKUP && !column[Constants.Baas.Tables.Columns.OPTIONS]) {
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
                column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS] = JSON.stringify(replicateColumns);
                column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] = JSON.stringify(primaryColumns);
                callback();
            }));
        } else {
            callback();
        }
    });
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