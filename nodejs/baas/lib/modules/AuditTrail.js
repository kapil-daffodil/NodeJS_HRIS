/**
 * Created with IntelliJ IDEA.
 * User: munish
 * Date: 19/9/13
 * Time: 11:53 AM
 * To change this template use File | Settings | File Templates.
 */
/**
 * Created with IntelliJ IDEA.
 * User: Munish Kumar
 * Date: 9/21/13
 * Time: 10:23 AM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var MetadataProvider = require('../metadata/MetadataProvider.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var ModuleHelper = require("./ModulesHelper.js");

exports.beforeQuery = function (table, query, context, callback) {
    var columns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
    ensureColumns(columns);
    callback();
}
exports.afterQueryResult = function (table, query, result, context, callback) {
    var columns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
    ensureColumns(columns);
    callback();
}
exports.beforeInsert = function (table, operations, context, callback) {
    try {
        var columns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
        ensureColumns(columns);
        var collectedColumns = ModuleHelper.collectColumnsInfo(columns, Constants.Baas.Tables.Columns.Type.OBJECT, null);
        var user = context.user;
        var userId = user ? user[APIConstants.Query.ID] : null;
        insertAuditTrailIfNotExist(operations, collectedColumns, userId);
        callback();
    } catch (err) {
        callback(err);
    }
}

exports.beforeUpdate = function (table, operations, context, callback) {
    try {
        var columns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
        ensureColumns(columns);
        var collectedColumns = ModuleHelper.collectColumnsInfo(columns, Constants.Baas.Tables.Columns.Type.OBJECT, null);
        var user = context.user;
        var userId = user ? user[APIConstants.Query.ID] : null;
        updateAuditTrailIfNotExist(operations, collectedColumns, userId);
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
        if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.AuditTrail.ID) {
            callback();
            return;
        }
    }
    var fileModule = {};
    fileModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.AuditTrail.ID;
    fileModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.AuditTrail.MODULE_PATH;
    enableModules.push(fileModule);
    callback();
}


function insertAuditTrailIfNotExist(opertions, collectedColumns, userId) {
    opertions.forEach(function (row) {
        var createdBy = row[Constants.ModulesConstants.AuditTrail.Columns.CREATED_BY];
        var createdOn = row[Constants.ModulesConstants.AuditTrail.Columns.CREATED_ON];
        row[Constants.ModulesConstants.AuditTrail.Columns.CREATED_BY] = userId;
        row[Constants.ModulesConstants.AuditTrail.Columns.CREATED_ON] = new Date();
        /*Insert Audit Trail for nested columns*/
        for (var columnName in collectedColumns) {
            var nestedColumnsInfo = collectedColumns[columnName];
            var columnType = nestedColumnsInfo[Constants.Baas.Tables.Columns.TYPE];
            var multiple = nestedColumnsInfo[Constants.Baas.Tables.Columns.MULTIPLE];
            if (columnType == Constants.Baas.Tables.Columns.Type.OBJECT && multiple) {
                var nestedRows = row[columnName];
                var rows = nestedRows;
                if (nestedRows) {
                    if (!(nestedRows instanceof Array)) {
                        rows = [];
                        rows.push(nestedRows);
                    }
                    insertAuditTrailIfNotExist(rows, nestedColumnsInfo, userId);
                }
            }
        }
    });
}

function updateAuditTrailIfNotExist(opertions, collectedColumns, userId) {
    opertions.forEach(function (row) {
        var createdBy = row[Constants.ModulesConstants.AuditTrail.Columns.CREATED_BY];
        var createdOn = row[Constants.ModulesConstants.AuditTrail.Columns.CREATED_ON];
        var rowId = row[APIConstants.Query._ID];
        if (!rowId) {
            row[Constants.ModulesConstants.AuditTrail.Columns.CREATED_ON] = new Date();
            row[Constants.ModulesConstants.AuditTrail.Columns.CREATED_BY] = userId;
        } else {
            row[Constants.ModulesConstants.AuditTrail.Columns.UPDATED_BY] = userId;
            row[Constants.ModulesConstants.AuditTrail.Columns.UPDATED_ON] = new Date();
        }
        /*Insert/Update Audit Trail for nested columns*/
        for (var columnName in collectedColumns) {
            var nestedColumnsInfo = collectedColumns[columnName];
            var columnType = nestedColumnsInfo[Constants.Baas.Tables.Columns.TYPE];
            var multiple = nestedColumnsInfo[Constants.Baas.Tables.Columns.MULTIPLE];
            if (columnType == Constants.Baas.Tables.Columns.Type.OBJECT && multiple) {
                var nestedRows = row[columnName];
                if (nestedRows) {
                    var rows = nestedRows;
                    if (!(nestedRows instanceof Array)) {
                        rows = [];
                        rows.push(nestedRows);
                    }
                    updateAuditTrailIfNotExist(rows, nestedColumnsInfo, userId);
                }
            }
        }
    });

}

function ensureColumns(columns) {
    var createdByExist = false;
    var updatedByExist = false;
    var createdOnExist = false;
    var updatedOnExist = false;
    columns.forEach(function (columnInfo) {
        var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
        var columnName = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
        var multiple = columnInfo[Constants.Baas.Tables.Columns.MULTIPLE];
        if (columnName == Constants.ModulesConstants.AuditTrail.Columns.CREATED_BY) {
            createdByExist = true;
        } else if (columnName == Constants.ModulesConstants.AuditTrail.Columns.CREATED_ON) {
            createdOnExist = true;
        } else if (columnName == Constants.ModulesConstants.AuditTrail.Columns.UPDATED_BY) {
            updatedByExist = true;
        } else if (columnName == Constants.ModulesConstants.AuditTrail.Columns.UPDATED_ON) {
            updatedOnExist = true;
        } else if (multiple && columnType == Constants.Baas.Tables.Columns.Type.OBJECT) {
            ensureColumns(columnInfo.columns || []);
        }
    });
    addColumnsIfNotExit(columns, createdByExist, createdOnExist, updatedByExist, updatedOnExist);
}

function addColumnsIfNotExit(columns, createdByExist, createdOnExist, updatedByExist, updatedOnExist) {
    if (!createdByExist) {
        addColumn(columns, Constants.ModulesConstants.AuditTrail.Columns.CREATED_BY, Constants.Baas.Tables.Columns.Type.STRING);
    }
    if (!createdOnExist) {
        addColumn(columns, Constants.ModulesConstants.AuditTrail.Columns.CREATED_ON, Constants.Baas.Tables.Columns.Type.DATETIME);
    }
    if (!updatedByExist) {
        addColumn(columns, Constants.ModulesConstants.AuditTrail.Columns.UPDATED_BY, Constants.Baas.Tables.Columns.Type.STRING);
    }
    if (!updatedOnExist) {
        addColumn(columns, Constants.ModulesConstants.AuditTrail.Columns.UPDATED_ON, Constants.Baas.Tables.Columns.Type.DATETIME);
    }
}

function addColumn(columns, expression, columnType) {
    var column = {};
    column[Constants.Baas.Tables.Columns.EXPRESSION] = expression;
    column[Constants.Baas.Tables.Columns.TYPE] = columnType;
    columns.push(column);
}
