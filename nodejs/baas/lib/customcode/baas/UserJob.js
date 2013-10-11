/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 14/8/13
 * Time: 4:25 PM
 * To change this template use File | Settings | File Templates.
 */

var Utils = require("apputil/util.js");
var Constants = require('../../shared/Constants.js');
var ApplaneError = require("apputil/ApplaneError.js");
var APIConstants = require("nodejsapi/Constants.js");
var MetadataProvider = require('../../metadata/MetadataProvider.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var APIConstants = require('nodejsapi/Constants.js');
var ObjectID = require('mongodb').ObjectID;
var MailService = require("../../mail/MailService.js");
var CacheService = require("../../cache/cacheservice.js");

exports.beforeInsert = function (params, callback) {
    Utils.iterateArray(params.operations, callback, function (user, callback) {
        if (!user[Constants.Baas.Users.EMAILID] && Utils.isEmailId(user[Constants.Baas.Users.USERNAME])) {
            user[Constants.Baas.Users.EMAILID] = user[Constants.Baas.Users.USERNAME];
        }
        MetadataProvider.getApplication(params.ask, ApplaneCallback(callback, function (application) {
            if (!user[Constants.Baas.Users.APPLICATIONS]) {
                var applicationUpdate = {};
                applicationUpdate[APIConstants.Query._ID] = application[APIConstants.Query._ID];
                user[Constants.Baas.Users.APPLICATIONS] = [applicationUpdate];
            }
            if (!user[Constants.Baas.Users.USERGROUPID]) {
                var currentUserGroupId = params.user ? params.user[Constants.Baas.Users.USERGROUPID] : null;
                if (!currentUserGroupId) {
                    currentUserGroupId = application[Constants.Baas.Applications.USERGROUPS][0];
                }
                user[Constants.Baas.Users.USERGROUPID] = currentUserGroupId;
            }
            if (!user[Constants.Baas.Users.PASSWORD]) {
                var password = new ObjectID().toString();
                password = password.length > 8 ? (password.substr(0, 4) + password.substr(password.length - 5)) : password
                user[Constants.Baas.Users.PASSWORD] = password;
            }
            callback();
        }));
    });
}

exports.beforeQuery = function (params, callback) {
    MetadataProvider.getApplication(params.ask, ApplaneCallback(callback, function (application) {
        var query = params.query;
        var filter = query[APIConstants.Query.FILTER] || {};
        if (!filter[Constants.Baas.Users.APPLICATIONS] && !filter[Constants.Baas.Users.APPLICATIONS + "." + APIConstants.Query._ID] && !filter[Constants.Baas.Users.APPLICATIONS + "." + Constants.Baas.Users.Applications.ORGANIZATIONID] && !filter[Constants.Baas.Users.APPLICATIONS + "." + Constants.Baas.Users.Applications.ORGANIZATIONID + "." + APIConstants.Query._ID]) {
            filter[Constants.Baas.Users.APPLICATIONS] = application[APIConstants.Query._ID];

            if (!filter[Constants.Baas.Users.USERGROUPID] && !filter[Constants.Baas.Users.USERGROUPID + "." + APIConstants.Query._ID]) {
                var currentUserGroupId = params.user ? params.user[Constants.Baas.Users.USERGROUPID][APIConstants.Query._ID] : null;
                if (!currentUserGroupId) {
                    currentUserGroupId = application[Constants.Baas.Applications.USERGROUPS][0][APIConstants.Query._ID];
                }
                filter[Constants.Baas.Users.USERGROUPID] = currentUserGroupId;
            }
        }

        query[APIConstants.Query.FILTER] = filter;
        callback();
    }));

}

exports.afterInsert = function (params, callback) {
    Utils.iterateArray(params.newvalues, callback, function (user, callback) {
        if (user[Constants.Baas.Users.EMAILID] && user[Constants.Baas.Users.APPLICATIONS] && user[Constants.Baas.Users.APPLICATIONS].length > 0) {
            MetadataProvider.getApplication(user[Constants.Baas.Users.APPLICATIONS][0][Constants.Baas.Applications.ASK], ApplaneCallback(callback, function (application) {
                if (application[Constants.Baas.Applications.WELCOMEMAILTEMPLATE]) {
                    var options = {};
                    options[Constants.MailService.Options.TO] = user[Constants.Baas.Users.EMAILID];
                    options[Constants.MailService.Options.SUBJECT] = application[Constants.Baas.Applications.WELCOMEMAILTEMPLATE_SUBJECT] || "Welcome user";
                    options[Constants.MailService.Options.TEMPLATE] = application[Constants.Baas.Applications.WELCOMEMAILTEMPLATE];
                    options[Constants.MailService.Options.TEMPLATE_DATA] = user;
                    MailService.sendMail(options, {"application":application}, callback);
                } else {
                    callback();
                }
            }));
        } else {
            callback();
        }
    });
}

exports.afterUpdate = function (params, callback) {
    var newValues = params.newvalues;
    var oldValues = params.oldvalues;
    Utils.iterateArrayWithIndex(params.operations, callback, function (index, user, callback) {
        var newValue = newValues[index];
        var oldValue = oldValues[index];
        if (params.user) {
            var userId = newValue[APIConstants.Query._ID];
            var currentUserId = params.user[APIConstants.Query._ID];
            if (userId.toString() == currentUserId.toString()) {
                var usk = params.usk;
                CacheService.put(usk, newValue);
            }
        }
        if (newValue[Constants.Baas.Users.EMAILID] && newValue[Constants.Baas.Users.APPLICATIONS] && newValue[Constants.Baas.Users.APPLICATIONS].length > 0) {
            MetadataProvider.getApplication(newValue[Constants.Baas.Users.APPLICATIONS][0][Constants.Baas.Applications.ASK], ApplaneCallback(callback, function (application) {
                var newForgotPasswordCode = newValue[Constants.Baas.Users.FORGOT_PASSWORD_CODE];
                var oldForgotPasswordCode = oldValue[Constants.Baas.Users.FORGOT_PASSWORD_CODE];
                var newPassword = newValue[Constants.Baas.Users.PASSWORD];
                var oldPassword = newValue[Constants.Baas.Users.PASSWORD];
                if (newForgotPasswordCode && newForgotPasswordCode != oldForgotPasswordCode && application[Constants.Baas.Applications.FORGOT_PASSWORD_MAILTEMPLATE]) {
                    var options = {};
                    options[Constants.MailService.Options.TO] = newValue[Constants.Baas.Users.EMAILID];
                    options[Constants.MailService.Options.SUBJECT] = application[Constants.Baas.Applications.FORGOT_PASSWORD_MAILTEMPLATE_SUBJECT] || "Forgot Password Code";
                    options[Constants.MailService.Options.TEMPLATE] = application[Constants.Baas.Applications.FORGOT_PASSWORD_MAILTEMPLATE];
                    options[Constants.MailService.Options.TEMPLATE_DATA] = newValue;
                    MailService.sendMail(options, {"application":application}, callback);
                } else if (newPassword && newPassword != oldPassword && application[Constants.Baas.Applications.RESET_PASSWORD_MAILTEMPLATE]) {
                    var options = {};
                    options[Constants.MailService.Options.TO] = newValue[Constants.Baas.Users.EMAILID];
                    options[Constants.MailService.Options.SUBJECT] = application[Constants.Baas.Applications.RESET_PASSWORD_MAILTEMPLATE_SUBJECT] || "Reset Password";
                    options[Constants.MailService.Options.TEMPLATE] = application[Constants.Baas.Applications.RESET_PASSWORD_MAILTEMPLATE];
                    options[Constants.MailService.Options.TEMPLATE_DATA] = newValue;
                    MailService.sendMail(options, {"application":application}, callback);
                } else {
                    callback();
                }
            }))
        } else {
            callback();
        }
    });
}
