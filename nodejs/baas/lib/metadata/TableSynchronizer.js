/**
 * Created with IntelliJ IDEA.
 * User: munish
 * Date: 29/8/13
 * Time: 10:47 AM
 * To change this template use File | Settings | File Templates.
 */
var Constants = require('../shared/Constants.js');
var MetadataProvider = require("./MetadataProvider.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var MongoDBManager = require("../database/mongodb/MongoDBManager.js");
var NodejsAPI = require("nodejsapi");
var Utils = require("apputil/util.js");
var APIConstants = require('nodejsapi/Constants.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");


exports.synchronize = function (tableName, callback) {
    MetadataProvider.getTable(tableName, ApplaneCallback(callback, function (table) {
        var tableApplications = table[Constants.Baas.Tables.APPLICATIONS] || [];
        var orgEnabled = table[Constants.Baas.Tables.ORGENABLED];
        var databases = [];
        if (tableApplications && tableApplications.length > 0) {
            Utils.iterateArray(tableApplications, ApplaneCallback(callback, function () {
                Utils.iterateArray(databases, callback, function (database, callback) {
                    syncTable(database, table, callback);
                });
            }), function (application, callback) {
                MetadataProvider.getApplication(application[Constants.Baas.Applications.ASK], ApplaneCallback(callback, function (application) {
                    var tableApplicationId = application[APIConstants.Query._ID];
                    if (orgEnabled) {
                        getApplicationOrganizations(tableApplicationId, ApplaneCallback(callback, function (organizations) {
                            if (organizations && organizations.length > 0) {
                                Utils.iterateArray(organizations, callback, function (organization, callback) {
                                    var database = organization[Constants.Baas.Organizations.DB] ? organization[Constants.Baas.Organizations.DB] : (application[Constants.Baas.Applications.DB] ? application[Constants.Baas.Applications.DB] : Constants.Baas.DEFAULT_ORGSHAREDDB);
                                    if (databases.indexOf(database) == -1) {
                                        databases.push(database);
                                    }
                                    callback();
                                })
                            } else {
                                callback();
                            }
                        }))
                    } else {
                        var database = application[Constants.Baas.Applications.DB] ? application[Constants.Baas.Applications.DB] : Constants.Baas.DEFAULT_GLOBALDB;
                        if (databases.indexOf(database) == -1) {
                            databases.push(database);
                        }
                        callback();
                    }
                }));
            })
        } else {
            callback();
        }
    }));
}


function syncTable(database, table, callback) {
    var columns = table[Constants.Baas.Tables.COLUMNS] || [];
    var indexes = table[Constants.Baas.Tables.INDEXES] || [];
    var primaryColumns = {};
    /*add unique indexes*/
    populatePrimaryColumnsMongoFormat(columns, primaryColumns, null) || [];
    if (Object.keys(primaryColumns).length > 0) {
        MongoDBManager.synchronizeUniqueIndexes(database, table[Constants.Baas.Tables.ID], primaryColumns, function (err) {
            if (err) {
                throw new Error(err.stack);
            }
        });
    }
    /*User can also add indexes for query*/
    indexes.forEach(function (indexDetail) {
        var fields = indexDetail[Constants.Baas.Tables.Indexes.FIELDS];
        var name = indexDetail[Constants.Baas.Tables.Indexes.NAME];
        var formatedIndexes = {};
        fields.forEach(function (field) {
            formatedIndexes[field] = 1;
        });
        if (Object.keys(formatedIndexes).length > 0) {
            var property = {background:true, name:name, dropDups:true};
            MongoDBManager.synchronizeIndexes(database, table, formatedIndexes, property, function (err) {
                if (err) {
                    throw new Error(err.stack);
                }
            });
        }
    });
    callback();
}

function populatePrimaryColumnsMongoFormat(columns, primaryColumns, expressionPrefix) {
    var length = columns.length;
    for (var i = 0; i < length; i++) {
        var columnInfo = columns[i];
        var isPrimary = columnInfo[Constants.Baas.Tables.Columns.PRIMARY];
        var expression = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
        var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
        var multiple = columnInfo[Constants.Baas.Tables.Columns.MULTIPLE];
        if (isPrimary) {
            var primaryColumn = expressionPrefix ? expressionPrefix + "." + expression : expression;
            if ((columnType == Constants.Baas.Tables.Columns.Type.LOOKUP || columnType == Constants.Baas.Tables.Columns.Type.OBJECT) && !multiple && !columnInfo[Constants.Baas.Tables.Columns.OPTIONS]) {
                primaryColumn = primaryColumn + "." + APIConstants.Query._ID;
            }
            primaryColumns[primaryColumn] = 1;
        }
        if ((columnType == Constants.Baas.Tables.Columns.Type.OBJECT || columnType == Constants.Baas.Tables.Columns.Type.LOOKUP) && !multiple && !columnInfo[Constants.Baas.Tables.Columns.OPTIONS]) {
            var subColumns = columnInfo[Constants.Baas.Tables.COLUMNS] || [];
            populatePrimaryColumnsMongoFormat(subColumns, primaryColumns, (expressionPrefix ? expressionPrefix + "." + expression : expression));
        }
    }
}


function getApplicationOrganizations(tableApplicationId, callback) {
    var tableFilter = {};
    tableFilter[Constants.Baas.Organizations.APPLICATIONS] = "{applicationid__}";
    var tableQuery = {};
    tableQuery[APIConstants.Query.TABLE] = Constants.Baas.ORGANIZATIONS;
    tableQuery[APIConstants.Query.COLUMNS] = [Constants.Baas.Organizations.OSK];
    tableQuery[APIConstants.Query.FILTER] = tableFilter;
    tableQuery[APIConstants.Query.PARAMETERS] = {"applicationid__":tableApplicationId};
    NodejsAPI.asQuery(
        tableQuery, {"ask":Constants.Baas.ASK}
    ).execute(ApplaneCallback(callback, function (result) {
        callback(null, result[APIConstants.Query.Result.DATA]);
    }));
}
