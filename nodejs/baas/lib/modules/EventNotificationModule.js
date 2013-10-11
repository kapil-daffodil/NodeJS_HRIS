/**
* Created with IntelliJ IDEA.
* User: munish
* Date: 4/9/13
* Time: 11:11 AM
* To change this template use File | Settings | File Templates.
*/
var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var MetadataProvider = require('../metadata/MetadataProvider.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var BaasError = require("apputil/ApplaneError.js");
var DataBaseEngine = require('../database/DatabaseEngine.js');
var MailService = require('../mail/MailService.js');
var Utils = require("apputil/util.js");
var vm = require('vm');


function resolve(condition, parameters) {
    if (condition) {
        console.log("condition " + condition)
        console.log(parameters)
        return vm.runInNewContext(condition, parameters);
    }
    return false;
}

exports.afterInsert = function (table, operations, context, callback) {
    callback();
    return;
    console.log("Notification module called.............");
    var tableId = table[APIConstants.Query._ID];
    var application = context.application;
    var applicationId = application[APIConstants.Query._ID];
    fetchNotificationDetails(tableId, applicationId, "update", ApplaneCallback(callback, function (notifications) {
        console.log("notifications " + JSON.stringify(notifications));
        var newValues = context.newvalues;
        Utils.iterateArray(notifications, callback, function (notification, callback) {
            var updatedColumns = notification[Constants.Baas.EventNotifications.UPDATED_COLUMNS];
            var condition = notification[Constants.Baas.EventNotifications.CONDITION];
            var count = 0;
            Utils.iterateArray(operations, callback, function (operation, callback) {
                var newValue = newValues[count++];
                if (!condition && !updatedColumns) {
                    sendMail(notification, newValue, callback);
                } else if (condition && resolve(condition, newValues) && updatedColumns && columnsUpdated(updatedColumns, operation)) {
                    sendMail(notification, newValue, callback);
                } else if (updatedColumns && columnsUpdated(updatedColumns, operation)) {
                    sendMail(notification, newValue, callback);
                } else if (condition && resolve(condition, newValues)) {
                    sendMail(notification, newValue, callback);
                } else {
                    callback();
                }
            });
        });
    }));
}


exports.afterUpdate = function (table, operations, context, callback) {
    callback();
    return;
    console.log("Notification module called.............");
    var tableId = table[APIConstants.Query._ID];
    var application = context.application;
    var applicationId = application[APIConstants.Query._ID];
    fetchNotificationDetails(tableId, applicationId, "update", ApplaneCallback(callback, function (notifications) {
        var newValues = context.newvalues;
        Utils.iterateArray(notifications, callback, function (notification, callback) {
            var updatedColumns = notification[Constants.Baas.EventNotifications.UPDATED_COLUMNS];
            var condition = notification[Constants.Baas.EventNotifications.CONDITION];
            var count = 0;
            Utils.iterateArray(operations, callback, function (operation, callback) {
                var newValue = newValues[count++];
                if (!condition && !updatedColumns) {
                    sendMail(notification, newValue, callback);
                } else if (condition && resolve(condition, newValues) && updatedColumns && columnsUpdated(updatedColumns, operation)) {
                    sendMail(notification, newValue, callback);
                } else if (updatedColumns && columnsUpdated(updatedColumns, operation)) {
                    sendMail(notification, newValue, callback);
                } else if (condition && resolve(condition, newValues)) {
                    sendMail(notification, newValue, callback);
                } else {
                    callback();
                }
            });
        });
    }));

}

exports.afterDelete = function (table, operations, context, callback) {
    callback();
    return;
    var tableId = table[APIConstants.Query._ID];
    var application = context.application;
    var applicationId = application[APIConstants.Query._ID];
    fetchNotificationDetails(tableId, applicationId, "update", ApplaneCallback(callback, function (notifications) {
        var oldValues = context.oldvalues;
        Utils.iterateArray(notifications, callback, function (notification, callback) {
            var condition = notification[Constants.Baas.EventNotifications.CONDITION];
            var operationsCount = operations.length;
            var count = 0;
            Utils.iterateArray(operations, callback, function (operation, callback) {
                var oldValues = oldValues[count++];
                if (!condition) {
                    sendMail(notification, oldValues, callback);
                } else if (resolve(condition, oldValues)) {
                    sendMail(notification, oldValues, callback);
                } else {
                    callback();
                }
            });
        });
    }));
}

/**
* It will enable the module for all tables.
* @param table
* @param callback
*/
exports.syncTable = function (table, callback) {
    var tableId = table[APIConstants.Query._ID];
    notificationExist(tableId, ApplaneCallback(callback, function (notifications) {
        if (notifications.length == 0) {
            callback();
            return;
        }
        table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
        var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
        var length = enableModules.length;
        for (var i = 0; i < length; i++) {
            if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.EventNotificationModule.ID) {
                callback();
                return;
            }
        }
        var eventNotificationModule = {};
        eventNotificationModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.EventNotificationModule.ID;
        eventNotificationModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.EventNotificationModule.MODULE_PATH;
        enableModules.push(eventNotificationModule);
        callback();
    }));

}

/**
* There can be multiple notification available for a particular operation
* @param tableid
* @param operationName
* @param callback
*/
function fetchNotificationDetails(tableid, applicationid, operationName, callback) {
    var filter = {};
    filter[Constants.Baas.EventNotifications.TABLE + "." + APIConstants.Query._ID] = tableid;
    filter[Constants.Baas.EventNotifications.OPERATION] = operationName;
    filter[Constants.Baas.EventNotifications.APPLICATION] = applicationid;

    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.EVENT_NOTIFICATIONS;
    query[APIConstants.Query.FILTER] = filter;

    DataBaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (data) {
            data = data.data;
            callback(null, data);
        })
    });
}


function notificationExist(tableid, callback) {
    var filter = {};
    filter[Constants.Baas.EventNotifications.TABLE + "." + APIConstants.Query._ID] = tableid;

    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.EVENT_NOTIFICATIONS;
    query[APIConstants.Query.FILTER] = filter;

    DataBaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (data) {
            data = data.data;
            callback(null, data);
        })
    });
}


columnsUpdated = function (updatedColumns, newValue) {
    var length = updatedColumns.length;
    for (var i = 0; i < length; i++) {
        var column = updatedColumns[i];
        var colValue = newValue[column];
        if (!colValue) {
            return false;
        }
    }
    return true;
};
function sendMail(notification, row, callback) {
    var toExpression = notification[Constants.Baas.EventNotifications.TO];
    var ccExpression = notification[Constants.Baas.EventNotifications.CC];
    var bccExpression = notification[Constants.Baas.EventNotifications.BCC];
    var to = toExpression ? getComaSeparatedEmailIds(toExpression, row) : null;
    var cc = ccExpression ? getComaSeparatedEmailIds(ccExpression, row) : null;
    var bcc = bccExpression ? getComaSeparatedEmailIds(bccExpression, row) : null;
    if (!to && !cc && !bcc) {
        throw new BaasError(Constants.ErrorCode.FIELDS_BLANK, ["No recipient emailId found."]);
    }

    var subject = notification[Constants.Baas.EventNotifications.SUBJECT];
    var mailTemplate = notification[Constants.Baas.EventNotifications.TEMPLATE];

    var mailInfo = {};
    mailInfo[Constants.MailService.Options.TO] = to;
    mailInfo[Constants.MailService.Options.CC] = cc;
    mailInfo[Constants.MailService.Options.BCC] = bcc;
    mailInfo[Constants.MailService.Options.SUBJECT] = subject;
    mailInfo[Constants.MailService.Options.TEMPLATE] = mailTemplate;
    mailInfo[Constants.MailService.Options.TEMPLATE_DATA] = row;

    console.log("mailInfo >>>>>" + JSON.stringify(mailInfo));
    MailService.sendMail(mailInfo, callback)

}

/**
* Function to validate emailid
* @param text  EmailId to validate
* @return {*}
*/
function getComaSeparatedEmailIds(text, row) {
    var emailIdList = "";
    var array = text.split(";");
    var length = array.length;
    var emailRegex = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/;
    for (var i = 0; i < length; i++) {
        var recipient = array[i];
        if (new RegExp(emailRegex).test(recipient)) {
            emailIdList = emailIdList + emailRegex + ", ";
        } else {
            var emailIds = getEmailIds(recipient, row);
            if (emailIds && emailIds.length > 0) {
                emailIdList = emailIdList + emailIds + ", ";
            }
        }
    }
    return emailIdList.length == 0 ? null : emailIdList.substring(0, emailIdList.lastIndexOf(","));
}

function getEmailIds(column, row) {
    var firstIndexOfDot = column.indexOf(".");
    if (firstIndexOfDot == -1) {
        var value = row[column];
        return  prepareCommaSeparatedList(value);
    } else {
        var preDotColumnName = column.substring(0, firstIndexOfDot);
        var postDotColumnName = column.substring(firstIndexOfDot + 1);
        var value = row[preDotColumnName];
        if (value instanceof Array) {
            var rowCount = value.length;
            var emailIdList = "";
            for (var j = 0; j < rowCount; j++) {
                var emailIds = getEmailIds(postDotColumnName, value[j]);
                if (emailIds) {
                    emailIdList = emailIds + ", "
                }
            }
            return emailIdList.length > 0 ? emailIdList.substring(0, emailIdList.lastIndexOf(",")) : emailIdList;
        } else {
            return getEmailIds(postDotColumnName, value);
        }
    }
}

function prepareCommaSeparatedList(value) {
    if (!value) {
        return "";
    } else if (value instanceof Array) {
        var length = value.length;
        var emailIds = "";
        for (var i = 0; i < length; i++) {
            emailIds = emailIds + value[i] + ", ";
        }
        return emailIds.length > 0 ? emailIds.substring(0, emailIds.lastIndexOf(",")) : emailIds;
    } else {
        return value;
    }
}
