/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 7/17/13
 * Time: 11:05 AM
 * To change this template use File | Settings | File Templates.
 */
var Constants = require("../shared/Constants.js");
var APIConstants = require('nodejsapi/Constants.js');
var MongoDBManager = require("../database/mongodb/MongoDBManager.js");
var CacheService = require("../cache/cacheservice.js");
var BaasError = require("apputil/ApplaneError.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var RequestAuthenticator = require("../server/RequestAuthenticator.js")
var self = require("./MetadataProvider.js")

exports.getOrganization = function (osk, callback) {
    if (!osk) {
        callback();
        return;
    }
    var cacheKey = ["Organizations", osk, 'self'];
    var organization = CacheService.get(cacheKey);
    if (organization) {
        callback(null, organization);
        return;
    }

    var query = {};
    query[Constants.Baas.Organizations.OSK] = osk;
    MongoDBManager.findOne(Constants.Baas.BAASDB, Constants.Baas.ORGANIZATIONS, query,
        function (err, result) {
            if (err) {
                callback(err, null);
            } else {
                if (!result) {
                    callback(new BaasError(Constants.ErrorCode.ORGANIZATION_NOT_FOUND, [osk]));
                } else {
                    CacheService.put(cacheKey, result);
                    callback(null, result);
                }
            }
        }
    );
};

exports.getApplication = function (ask, callback) {
    if (!ask) {
        callback();
        return;
    }
    var cacheKey = ["Applications", ask, "self"];
    var application = CacheService.get(cacheKey);
    if (application) {
        callback(null, application);
        return;
    }

    var query = {};
    query[Constants.Baas.Applications.ASK] = ask;
    MongoDBManager.findOne(Constants.Baas.BAASDB, Constants.Baas.APPLICATIONS, query,
        function (err, result) {
            if (err) {
                callback(err, null);
            } else {
                if (!result) {
                    callback(new BaasError(Constants.ErrorCode.APPLICATION_NOT_FOUND, [ask]));
                } else {
                    CacheService.put(cacheKey, result);
                    callback(null, result);
                }
            }
        }
    );
};

exports.getView = function (tableName, viewId, callback) {
    if (tableName == undefined || viewId == undefined) {
        throw new Error("Table or view can not be undefined");
    }
    var cacheKey = ["Tables", tableName, "view", viewId];
    var view = CacheService.get(cacheKey);
    if (view) {
        callback(null, view);
        return;
    }


    var query = {};
    query[Constants.Baas.Views.ID] = viewId;
    query[Constants.Baas.Views.TABLE + "." + Constants.Baas.Tables.ID] = tableName;
    MongoDBManager.findOne(Constants.Baas.BAASDB, Constants.Baas.VIEWS, query,
        ApplaneCallback(callback, function (view) {
            if (!view) {
                throw new BaasError(Constants.ErrorCode.VIEW_NOT_FOUND, [viewId, tableName]);
            }
            var viewQuery = view[Constants.Baas.Views.QUERY];
            try {
                view[Constants.Baas.Views.QUERY] = viewQuery ? (typeof(viewQuery) == 'string' ? JSON.parse(viewQuery) : viewQuery) : {table:tableName};
            } catch (err) {
                view[Constants.Baas.Views.QUERY] = {table:tableName};
            }
            CacheService.put(cacheKey, view);
            callback(null, view);
        })
    );

}
exports.getTable = function (tableName, authentication, callback) {
    if ('function' === typeof authentication) callback = authentication, authentication = null;
    if (!authentication) {
        loadTable(tableName, callback);
        return;
    }
    var ask = authentication.ask;
    var osk = authentication.osk;

    RequestAuthenticator.authenticate(ask, osk, ApplaneCallback(callback, function (data) {
        var application = data.application;
        var organization = data.organization;
        loadTable(tableName, ApplaneCallback(callback, function (table) {
            if (!table[Constants.Baas.Tables.PUBLIC]) {
                var tableApplications = table[Constants.Baas.Tables.APPLICATIONS];
                var tableMapped = false;
                var length = tableApplications ? tableApplications.length : 0;
                for (var i = 0; i < length; i++) {
                    if (tableApplications[i][Constants.Baas.Applications.ID] == application[Constants.Baas.Applications.ID]) {
                        tableMapped = true;
                        break;
                    }
                }
                if (!tableMapped) {
                    throw new BaasError(Constants.ErrorCode.TABLE_APPLICATION_NOT_MAPPED, [tableName, application[Constants.Baas.Applications.ID]]);
                }
            }
            getDatabase(table, (table[Constants.Baas.Tables.MAINAPPLICATION] || application), organization, application, callback);
        }));
    }));
}

function getDatabase(table, application, organization, currentApplication, callback) {
    var orgEnabled = table[Constants.Baas.Tables.ORGENABLED];
    var database = {};
    if (!orgEnabled) {
        database[Constants.Baas.Tables.DB] = application[Constants.Baas.Applications.DB] ? application[Constants.Baas.Applications.DB] : Constants.Baas.DEFAULT_GLOBALDB;
    } else {
//        console.log("organization: " + JSON.stringify(organization))
//        console.log("application: " + JSON.stringify(application))
        var db = organization[Constants.Baas.Organizations.DB] ? organization[Constants.Baas.Organizations.DB] : (application[Constants.Baas.Applications.DB] ? application[Constants.Baas.Applications.DB] : Constants.Baas.DEFAULT_ORGSHAREDDB);
        var dbstatus = organization[Constants.Baas.Organizations.DBSTATUS];
        database[Constants.Baas.Tables.DB] = db;
        database[Constants.Baas.Tables.DBSTATUS] = dbstatus;
    }
    callback(null, {database:database, table:table, application:application, organization:organization, currentapplication:currentApplication});
}

function loadTable(tableName, callback) {
    if (tableName == undefined) {
        throw new Error("Tables can not be undefined");
    }
    var cacheKey = ["Tables", tableName, "self"];
    var table = CacheService.get(cacheKey);
    if (table) {
//        console.log("Table found in cache " + tableName);
        callback(null, table);
        return;
    }


    var query = {};
    query[Constants.Baas.Tables.ID] = tableName;
    MongoDBManager.findOne(Constants.Baas.BAASDB, Constants.Baas.TABLES, query,
        ApplaneCallback(callback, function (result) {

            if (!result) {
                throw new BaasError(Constants.ErrorCode.TABLE_NOT_FOUND, [tableName]);
            }
            var columns = result[Constants.Baas.Tables.COLUMNS];
            result[Constants.Baas.Tables.COLUMNS] = removeDottedColumn(columns);
            result[Constants.Baas.Tables.SYSTEM_COLUMNS] = JSON.parse(JSON.stringify(result[Constants.Baas.Tables.COLUMNS]));
            if (result[Constants.Baas.Tables.MAINAPPLICATION]) {
                self.getApplication(result[Constants.Baas.Tables.MAINAPPLICATION][Constants.Baas.Applications.ASK], ApplaneCallback(callback, function (mainApplication) {
                    result[Constants.Baas.Tables.MAINAPPLICATION] = mainApplication;
                    CacheService.put(cacheKey, result);
                    callback(null, result);
                }));
            } else {
                CacheService.put(cacheKey, result);
                callback(null, result);
            }
        })
    );
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
