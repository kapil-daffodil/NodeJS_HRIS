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
 * Date: 9/19/13
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var MetadataProvider = require('../metadata/MetadataProvider.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var BaasError = require("apputil/ApplaneError.js");
var MongoDbManager = require("../database/mongodb/MongoDBManager.js");

function deleteDeReferencedFiles(newValue, oldValue, invokeDeleteFileFunction) {
    if (oldValue) {
        if (!newValue || newValue.length == 0) {
            /*Delete all uploaded files*/
            oldValue.forEach(function (file) {
                invokeDeleteFileFunction(file);
            });
        }
        oldValue.forEach(function (file) {
            var fileExist = false;
            var oldFileKey = file.key;
            var length = newValue.length;
            for (var i = 0; i < length; i++) {
                var fileKey = newValue[i].key;
                if (fileKey == oldFileKey) {
                    fileExist = true;
                    break;
                }
            }
            if (!fileExist) {
                invokeDeleteFileFunction(file);
                fileExist = false;
            }
        });
    }

}


exports.afterUpdate = function (table, operations, context, callback) {
    try {
        var columns = table[Constants.Baas.Tables.COLUMNS] || [];
        var oldValues = context.oldvalues || [];
        var newValues = context.newvalues || [];
        columns.forEach(function (column) {
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnType == Constants.Baas.Tables.Columns.Type.FILE || columnType == Constants.Baas.Tables.Columns.Type.IMAGE) {
                var columnName = column[Constants.Baas.Tables.Columns.EXPRESSION];
                var length = operations.length;
                for (var i = 0; i < length; i++) {
                    var operation = operations[i];
                    var uploadedFilesInfo = operation[columnName];
                    if (uploadedFilesInfo) {
                        deleteDeReferencedFiles(newValues[i][columnName], oldValues[i][columnName], function (file) {
                            MongoDbManager.deleteFle(context.database[Constants.Baas.Tables.DB], file.key, ApplaneCallback(callback, function (result) {
                                //Nothing to do here
                            }));
                        });
                    }
                }
            }
        });
        callback();
    } catch (err) {
        callback(err);
    }
}

exports.afterDelete = function (table, operations, context, callback) {
    try {
        var columns = table[Constants.Baas.Tables.COLUMNS] || [];
        var oldValues = context.oldvalues || [];
        columns.forEach(function (column) {
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnType == Constants.Baas.Tables.Columns.Type.FILE || columnType == Constants.Baas.Tables.Columns.Type.IMAGE) {
                var columnName = column[Constants.Baas.Tables.Columns.EXPRESSION];
                oldValues.forEach(function (row) {
                    var filesInfo = row[columnName];
                    if (filesInfo && filesInfo instanceof Array) {
                        filesInfo.forEach(function (file) {
                            MongoDbManager.deleteFle(context.database[Constants.Baas.Tables.DB], file.key, ApplaneCallback(callback, function (result) {
                                //Nothing to do here
                            }));
                        });
                    }
                });
            }
        });
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
    var columns = table[Constants.Baas.Tables.COLUMNS] || [];
    if (enableIfFileTypeColumnExist(columns)) {
        table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
        var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
        var length = enableModules.length;
        for (var i = 0; i < length; i++) {
            if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.FileModule.ID) {
                callback();
                return;
            }
        }
        var fileModule = {};
        fileModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.FileModule.ID;
        fileModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.FileModule.MODULE_PATH;
        enableModules.push(fileModule);
    }
    callback();
}

function enableIfFileTypeColumnExist(columns) {
    var length = columns.length;
    if (length > 0) {
        for (var i = 0; i++; i < length) {
            var column = columns[i];
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnType == Constants.Baas.Tables.Columns.Type.IMAGE || columnType == Constants.Baas.Tables.Columns.Type.FILE) {
                return true;
            } else if (column[Constants.Baas.Tables.columns] && enableIfFileTypeColumnExist(column[Constants.Baas.Tables.columns])) {
                return true;
            }
        }
    }
    return false;
}

