/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 8/1/13
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var MetadataProvider = require('../metadata/MetadataProvider.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");

exports.beforeQuery = function (table, query, context, callback) {
    if (context.osk && table[Constants.Baas.Tables.ORGENABLED]) {
        MetadataProvider.getOrganization(context.osk, ApplaneCallback(callback, function (organization) {
            if (organization[Constants.Baas.Organizations.DBSTATUS] == Constants.Baas.Organizations.DBStatus.SHARED) {
                ensureColumns(table);
                query[APIConstants.Query.FILTER] = query[APIConstants.Query.FILTER] || {};
                query[APIConstants.Query.FILTER][Constants.ModulesConstants.OrganizationModule.ORGANIZATIONID] = organization[APIConstants.Query._ID].toString();
            }
            callback();

        }))
    } else {
        callback();
    }
}

exports.beforeInsert = function (table, operations, context, callback) {
    if (context.osk && table[Constants.Baas.Tables.ORGENABLED]) {
        MetadataProvider.getOrganization(context.osk, ApplaneCallback(callback, function (organization) {
            if (organization[Constants.Baas.Organizations.DBSTATUS] == Constants.Baas.Organizations.DBStatus.SHARED) {
                ensureColumns(table);
                operations.forEach(function (operation) {
                    operation[Constants.ModulesConstants.OrganizationModule.ORGANIZATIONID] = organization[APIConstants.Query._ID].toString();
                });
            }
            callback();
        }))
    } else {
        callback();
    }
}

function ensureColumns(table) {
    table[Constants.Baas.Tables.SYSTEM_COLUMNS] = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
    var systemColumns = table[Constants.Baas.Tables.SYSTEM_COLUMNS];
    var length = systemColumns.length;
    for (var i = 0; i < length; i++) {
        if (systemColumns[i][Constants.Baas.Tables.Columns.EXPRESSION] == Constants.ModulesConstants.OrganizationModule.ORGANIZATIONID) {
            return;
        }
    }
    var orgColumn = {};
    orgColumn[Constants.Baas.Tables.Columns.EXPRESSION] = Constants.ModulesConstants.OrganizationModule.ORGANIZATIONID;
    orgColumn[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.STRING;
    systemColumns.push(orgColumn);
}

exports.syncTable = function (table, callback) {
    if (table[Constants.Baas.Tables.ORGENABLED]) {
        table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
        var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
        var length = enableModules.length;
        for (var i = 0; i < length; i++) {
            if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.OrganizationModule.ID) {
                callback();
                return;
            }
        }
        var orgModule = {};
        orgModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.OrganizationModule.ID;
        orgModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.OrganizationModule.MODULE_PATH;
        enableModules.push(orgModule);
    }
    callback();

}


