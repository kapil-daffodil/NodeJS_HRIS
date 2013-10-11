var DatabaseEngine = require("../database/DatabaseEngine.js")
var RequestAuthenticator = require("../server/RequestAuthenticator.js")
var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var BaasError = require("apputil/ApplaneError.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var UpdateEngine = require("../database/UpdateEngine.js");
var CacheService = require("../cache/cacheservice.js");
var MongoDBManager = require("../database/mongodb/MongoDBManager.js");
var objectID = require("mongodb").ObjectID;
var UserService = require("./userservice.js");


var CURRENT_USER_ID = "_CurrentUserId"
var CURRENT_USERGROUP_ID = "_CurrentUserGroupId"

exports.login = function (username, password, options, callback) {
    if (!username || !password) {
        callback(new BaasError(Constants.ErrorCode.FIELDS_BLANK, ["username, password"]));
        return;
    }
    if (!options.ask && options.usergroup) {
        loginWithUserGroup(username, password, options, callback);
        return;
    }
    RequestAuthenticator.authenticate(options.ask, options.osk, ApplaneCallback(callback, function (data) {
        var application = data.application;
        var organization = data.organization;
        var userGroup = getUserGroup(options.usergroup, application);
        var filter = {};
        filter[Constants.Baas.Users.USERNAME] = username;
        filter[Constants.Baas.Users.PASSWORD] = password;
        filter[Constants.Baas.Users.USERGROUPID + "." + APIConstants.Query._ID] = userGroup[APIConstants.Query._ID];
        var query = {};
        query[APIConstants.Query.TABLE] = Constants.Baas.USERS;
        query[APIConstants.Query.FILTER] = filter;
        query[APIConstants.Query.EXCLUDE_JOBS] = true;
        DatabaseEngine.query({
            ask:Constants.Baas.ASK,
            query:query,
            callback:ApplaneCallback(callback, function (result) {
                var data = result[APIConstants.Query.Result.DATA];
                if (data && data.length > 0) {
                    var user = data[0];
                    var applications = user[Constants.Baas.Users.APPLICATIONS];
                    var appFound = false;
                    var orgFound = false
                    if (applications) {
                        var length = applications.length;
                        for (var i = 0; i < length; i++) {
                            if (applications[i][APIConstants.Query._ID].toString() == application[APIConstants.Query._ID].toString()) {
                                appFound = true;
                                if (application[Constants.Baas.Applications.ORGENABLED]) {
                                    var userAppOrganizations = applications[i][Constants.Baas.Users.Applications.ORGANIZATIONS];
                                    userAppOrganizations.forEach(function (userAppOrg) {
                                        if (userAppOrg[APIConstants.Query._ID].toString() == organization[APIConstants.Query._ID].toString()) {
                                            orgFound = true;
                                        }
                                    });
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    if (!appFound) {
                        throw new BaasError(Constants.ErrorCode.User.NOT_APP_ACCESS, [application[Constants.Baas.Applications.ID]]);
                    }

                    if (application[Constants.Baas.Applications.ORGENABLED]) {
                        if (!orgFound) {
                            throw new BaasError(Constants.ErrorCode.User.NOT_ORG_ACCESS, [organization[Constants.Baas.Organizations.ID]]);
                        }
                    }
                    var objectId = new objectID().toString();
                    user.usk = objectId;
                    var userId = user[APIConstants.Query._ID];
                    user.lastactivitydate = new Date();
                    setSession(objectId, userId, ApplaneCallback(callback, function (result) {
                    }));
                    CacheService.put(objectId, user);
                    callback(null, user);
                } else {
                    throw new BaasError(Constants.ErrorCode.User.CREDENTIAL_MISSMATCH);
                }
            })
        });
    }))
}


function loginWithUserGroup(username, password, options, callback) {
    var filter = {};
    filter[Constants.Baas.Users.USERNAME] = username;
    filter[Constants.Baas.Users.PASSWORD] = password;
    filter[Constants.Baas.Users.USERGROUPID + "." + Constants.Baas.UserGroups.ID] = options.usergroup;
    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.USERS;
    query[APIConstants.Query.FILTER] = filter;
    query[APIConstants.Query.EXCLUDE_JOBS] = true;
    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                var user = data[0];
                var objectId = new objectID().toString();
                user.usk = objectId;
                user.lastactivitydate = new Date();
                UserService.addDeveloperAndAdmins(user, ApplaneCallback(callback, function () {
                    setSession(objectId, user[APIConstants.Query._ID], ApplaneCallback(callback, function (result) {
                    }));
                    CacheService.put(objectId, user);
                    callback(null, user);
                }));
            } else {
                throw new BaasError(Constants.ErrorCode.User.CREDENTIAL_MISSMATCH);
            }
        })
    });
}

exports.addDeveloperAndAdmins = function (user, callback) {
    var username = user[Constants.Baas.Users.USERNAME];
    var applicationIds = [];
    var organizationIds = [];
    var userApplications = user[Constants.Baas.Users.APPLICATIONS];
    for (var i = 0; i < userApplications.length; i++) {
        var userApplication = userApplications[i];
        applicationIds.push(userApplication[APIConstants.Query._ID]);
        var userAppOrganizations = userApplication[Constants.Baas.Users.Applications.ORGANIZATIONS];
        if (userAppOrganizations) {
            userAppOrganizations.forEach(function (userAppOrg) {
                var userAppOrgId = userAppOrg[APIConstants.Query._ID];
                if (organizationIds.indexOf(userAppOrgId) == -1) {
                    organizationIds.push(userAppOrgId);
                }
            });
        }
    }
    getUserApplicationsAsDeveloper(applicationIds, username, ApplaneCallback(callback, function (userApplicationsAsDeveloper) {
        getUserOrganizationsAsAdmin(organizationIds, username, ApplaneCallback(callback, function (userOrganizationsAsAdmin) {
            for (var i = 0; i < userApplications.length; i++) {
                var userApplication = userApplications[i];
                if (userApplicationsAsDeveloper.indexOf(userApplication[APIConstants.Query._ID].toString()) != -1) {
                    userApplication.developer = true;
                }
                var userAppOrganizations = userApplication[Constants.Baas.Users.Applications.ORGANIZATIONS];
                if (userAppOrganizations) {
                    userAppOrganizations.forEach(function (userAppOrg) {
                        if (userOrganizationsAsAdmin.indexOf(userAppOrg[APIConstants.Query._ID].toString()) != -1) {
                            userAppOrg.admin = true;
                        }
                    });
                }
            }
            callback();
        }));
    }));
}

function getUserOrganizationsAsAdmin(organizationIds, username, callback) {
    var userOrganizationsAsAdmins = [];
    if (organizationIds.length > 0) {
        var filter = {};
        filter[Constants.Baas.Organizations.ADMINS + "." + Constants.Baas.Users.USERNAME] = "{username__}";
        filter[APIConstants.Query._ID] = {$in:organizationIds};

        var query = {};
        query[APIConstants.Query.TABLE] = Constants.Baas.ORGANIZATIONS;
        query[APIConstants.Query.COLUMNS] = [APIConstants.Query._ID];
        query[APIConstants.Query.FILTER] = filter;
        query[APIConstants.Query.PARAMETERS] = {"username__":username};
        DatabaseEngine.query({
            ask:Constants.Baas.ASK,
            query:query,
            callback:ApplaneCallback(callback, function (result) {
                var data = result[APIConstants.Query.Result.DATA] || [];
                for (var i = 0; i < data.length; i++) {
                    var row = data[i];
                    userOrganizationsAsAdmins.push(row[APIConstants.Query._ID].toString());
                }
                callback(null, userOrganizationsAsAdmins);
            })
        });
    } else {
        callback(null, userOrganizationsAsAdmins);
    }
}

function getUserApplicationsAsDeveloper(applicationIds, username, callback) {
    var userApplicationsAsDeveloper = [];
    if (applicationIds.length > 0) {
        var filter = {};
        filter[Constants.Baas.Applications.DEVELOPERS + "." + Constants.Baas.Users.USERNAME] = "{username__}";
        filter[APIConstants.Query._ID] = {$in:applicationIds};

        var query = {};
        query[APIConstants.Query.TABLE] = Constants.Baas.APPLICATIONS;
        query[APIConstants.Query.COLUMNS] = [APIConstants.Query._ID];
        query[APIConstants.Query.FILTER] = filter;
        query[APIConstants.Query.PARAMETERS] = {"username__":username};
        DatabaseEngine.query({
            ask:Constants.Baas.ASK,
            query:query,
            callback:ApplaneCallback(callback, function (result) {
                var data = result[APIConstants.Query.Result.DATA] || [];
                for (var i = 0; i < data.length; i++) {
                    var row = data[i];
                    userApplicationsAsDeveloper.push(row[APIConstants.Query._ID].toString());
                }
                callback(null, userApplicationsAsDeveloper);
            })
        });
    } else {
        callback(null, userApplicationsAsDeveloper);
    }
}

function contains(objectArray, key, column) {
    if (objectArray) {
        var length = objectArray.length;
        for (var i = 0; i < length; i++) {
            if (objectArray[i][APIConstants.Query._ID].toString() == key.toString()) {
                return column ? objectArray[i][column] : true;
            }
        }
    }
    return column ? null : false;
}

function getUserGroup(userGroup, application) {
    var userGroups = application[Constants.Baas.Applications.USERGROUPS];
    if (userGroup) {
        var length = userGroups.length;
        for (var i = 0; i < length; i++) {
            if (userGroups[i][Constants.Baas.UserGroups.ID] == userGroup) {
                return userGroups[i];
            }
        }
        throw new BaasError(Constants.ErrorCode.User.UAG_NOT_FOUND, [userGroup]);
    } else {
        return userGroups[0];
    }
}


exports.createUser = function (username, password, extraColumns, flexColumns, options, callback) {
    if (!username) {
        callback(new BaasError(Constants.ErrorCode.FIELDS_BLANK, ["username"]));
        return;
    }
    RequestAuthenticator.authenticate(options.ask, options.osk, ApplaneCallback(callback, function (data) {
        var application = data.application;
        var organization = data.organization;
        var userGroup = getUserGroup(options.usergroup, application);
        var filter = {};
        filter[Constants.Baas.Users.USERNAME] = username;
        filter[Constants.Baas.Users.USERGROUPID + "." + APIConstants.Query._ID] = userGroup[APIConstants.Query._ID];
        var query = {};
        query[APIConstants.Query.TABLE] = Constants.Baas.USERS;
        query[APIConstants.Query.FILTER] = filter;
        query[APIConstants.Query.EXCLUDE_JOBS] = true;
        DatabaseEngine.query({
            ask:Constants.Baas.ASK,
            query:query,
            callback:ApplaneCallback(callback, function (result) {
                var data = result[APIConstants.Query.Result.DATA];

                var updateUser = {};

                var userApp = {};
                userApp[APIConstants.Query._ID] = application[APIConstants.Query._ID];
                userApp[Constants.Baas.Applications.ID] = application[Constants.Baas.Applications.ID];

                if (data && data.length > 0) {
                    var user = data[0];
                    updateUser[APIConstants.Query._ID] = user[APIConstants.Query._ID];
                    var applications = user[Constants.Baas.Users.APPLICATIONS];
                    if (applications) {
                        var length = applications.length;
                        for (var i = 0; i < length; i++) {
                            if (applications[i][APIConstants.Query._ID].toString() == application[APIConstants.Query._ID].toString()) {
                                if (application[Constants.Baas.Applications.ORGENABLED]) {
                                    var userAppOrganizations = applications[i][Constants.Baas.Users.Applications.ORGANIZATIONS];
                                    userAppOrganizations.forEach(function (userAppOrg) {
                                        if (userAppOrg[APIConstants.Query._ID].toString() == organization[APIConstants.Query._ID].toString()) {
                                            throw new BaasError(Constants.ErrorCode.User.USER_ALREADY_EXISTS_IN_ORG, [organization[Constants.Baas.Organizations.ID]]);
                                        }
                                    });
                                } else {
                                    throw new BaasError(Constants.ErrorCode.User.USER_ALREADY_EXISTS_IN_APP, [application[Constants.Baas.Applications.ID]]);
                                }
                            }
                        }
                    }
                } else {
                    updateUser[Constants.Baas.Users.USERNAME] = username;
                    updateUser[Constants.Baas.Users.PASSWORD] = password;
                    if (extraColumns) {
                        for (var exp in extraColumns) {
                            updateUser[exp] = extraColumns[exp];
                        }
                    }
                }

                userGroup = JSON.parse(JSON.stringify(userGroup));
                if (flexColumns) {
                    for (col in flexColumns) {
                        userGroup[col] = flexColumns[col];
                    }
                }
                updateUser[Constants.Baas.Users.USERGROUPID] = userGroup;

                if (application[Constants.Baas.Applications.ORGENABLED]) {
                    var userOrg = {};
                    userOrg[APIConstants.Query._ID] = organization[APIConstants.Query._ID];
                    userOrg[Constants.Baas.Organizations.ID] = organization[Constants.Baas.Organizations.ID];
                    userApp[Constants.Baas.Users.Applications.ORGANIZATIONS] = [userOrg];
                }

                updateUser[Constants.Baas.Users.APPLICATIONS] = [userApp];

                var updateOptions = {};
                updateOptions[APIConstants.Query.ASK] = Constants.Baas.ASK;
                var updates = {};
                updates[APIConstants.Query.TABLE] = Constants.Baas.USERS;
                updates[APIConstants.Update.OPERATIONS] = [updateUser];
                updateOptions.updates = updates;
                updateOptions.callback = ApplaneCallback(callback, function (data) {
                    var userId = updateUser[APIConstants.Query._ID] || data.insert[0][APIConstants.Query._ID];
                    getUser(userId, userGroup[APIConstants.Query._ID], callback);
                });
                UpdateEngine.performUpdate(updateOptions);
            })
        });
    }))
}

function getUser(userid, userGroupId, callback) {
    var filter = {};
    filter[APIConstants.Query._ID] = userid;
    filter[Constants.Baas.Users.USERGROUPID + "." + APIConstants.Query._ID] = userGroupId;
    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.USERS;
    query[APIConstants.Query.FILTER] = filter;
    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            console.log(data);
            if (data && data.length > 0) {
                var user = data[0];
                callback(null, user);
            } else {
                callback();
            }
        })
    });
}

exports.forgotPassword = function (userName, options, callback) {
    if (!userName) {
        callback(new BaasError(Constants.ErrorCode.FIELDS_BLANK, ["username"]));
        return;
    }
    if (!options.ask && options.usergroup) {
        forgotPasswordWithUserGroup(userName, options, callback);
        return;
    }
    RequestAuthenticator.authenticate(options.ask, options.osk, ApplaneCallback(callback, function (data) {
        var application = data.application;
        var organization = data.organization;
        var userGroup = getUserGroup(options.usergroup, application);
        var filter = {};
        filter[Constants.Baas.Users.USERNAME] = userName;
        filter[Constants.Baas.Users.USERGROUPID + "." + APIConstants.Query._ID] = userGroup[APIConstants.Query._ID];
        var query = {};
        query[APIConstants.Query.TABLE] = Constants.Baas.USERS;
        query[APIConstants.Query.FILTER] = filter;
        query[APIConstants.Query.EXCLUDE_JOBS] = true;
        DatabaseEngine.query({
            ask:Constants.Baas.ASK,
            query:query,
            callback:ApplaneCallback(callback, function (result) {
                var data = result[APIConstants.Query.Result.DATA];
                var updateUser = {};
                if (data && data.length > 0) {
                    var user = data[0];
                    updateUser[APIConstants.Query._ID] = user[APIConstants.Query._ID];
                    var forgotPasswordCode = new objectID().toString();
                    updateUser[Constants.Baas.Users.FORGOT_PASSWORD_CODE] = forgotPasswordCode;
                    var updateOptions = {};
                    updateOptions[APIConstants.Query.ASK] = Constants.Baas.ASK;
                    var updates = {};
                    updates[APIConstants.Query.TABLE] = Constants.Baas.USERS;
                    updates[APIConstants.Update.OPERATIONS] = [updateUser];
                    updateOptions.updates = updates;
                    updateOptions.callback = ApplaneCallback(callback, function () {
                        callback(null, forgotPasswordCode);
                    });
                    UpdateEngine.performUpdate(updateOptions);
                } else {
                    throw new BaasError(Constants.ErrorCode.User.USER_NOT_FOUND);
                }
            })
        });
    }));
}

function forgotPasswordWithUserGroup(userName, options, callback) {
    var filter = {};
    filter[Constants.Baas.Users.USERNAME] = userName;
    filter[Constants.Baas.Users.USERGROUPID + "." + Constants.Baas.UserGroups.ID] = options.usergroup;
    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.USERS;
    query[APIConstants.Query.FILTER] = filter;
    query[APIConstants.Query.EXCLUDE_JOBS] = true;
    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            var updateUser = {};
            if (data && data.length > 0) {
                var user = data[0];
                updateUser[APIConstants.Query._ID] = user[APIConstants.Query._ID];
                var forgotPasswordCode = new objectID().toString();
                updateUser[Constants.Baas.Users.FORGOT_PASSWORD_CODE] = forgotPasswordCode;
                var updateOptions = {};
                updateOptions[APIConstants.Query.ASK] = Constants.Baas.ASK;
                var updates = {};
                updates[APIConstants.Query.TABLE] = Constants.Baas.USERS;
                updates[APIConstants.Update.OPERATIONS] = [updateUser];
                updateOptions.updates = updates;
                updateOptions.callback = ApplaneCallback(callback, function () {
                    callback(null, forgotPasswordCode);
                });
                UpdateEngine.performUpdate(updateOptions);
            } else {
                throw new BaasError(Constants.ErrorCode.User.USER_NOT_FOUND);
            }
        })
    });
}

exports.resetPassword = function (options, callback) {
    var forgotPasswordCode = options.forgotPasswordCode;
    var newPassword = options.newPassword;
    var usk = options.usk;
    var oldPassword = options.oldPassword;
    if ((!forgotPasswordCode && (!usk || !oldPassword))) {
        callback(new BaasError(Constants.ErrorCode.FIELDS_BLANK, ["usk, forgotpasswordcode, oldpassword"]));
        return;
    }
    if (!newPassword) {
        callback(new BaasError(Constants.ErrorCode.FIELDS_BLANK, ["newPassword"]));
        return;
    }
    var userId;
    if (forgotPasswordCode) {
        var query = {};
        query[APIConstants.Query.TABLE] = Constants.Baas.USERS;
        query[APIConstants.Query.EXCLUDE_JOBS] = true;
        var filter = {};
        filter[Constants.Baas.Users.FORGOT_PASSWORD_CODE] = "{code__}";
        query[APIConstants.Query.FILTER] = filter;
        query[APIConstants.Query.PARAMETERS] = {"code__":forgotPasswordCode};
        DatabaseEngine.query({
            ask:Constants.Baas.ASK,
            query:query,
            callback:ApplaneCallback(callback, function (result) {
                var data = result[APIConstants.Query.Result.DATA];
                if (data && data.length > 0) {
                    userId = data[0][APIConstants.Query._ID];
                    updatePassword(userId, newPassword, callback);
                } else {
                    throw new BaasError(Constants.ErrorCode.User.USER_NOT_FOUND);
                }
            })
        });
    } else {
        var currentUser = CacheService.get(usk);
        if (currentUser && currentUser[Constants.Baas.Users.PASSWORD] == oldPassword) {
            userId = currentUser[APIConstants.Query._ID];
            updatePassword(userId, newPassword, callback);
        } else {
            throw new BaasError(Constants.ErrorCode.User.CREDENTIAL_MISSMATCH);
        }
    }

}

function updatePassword(userId, newPassword, callback) {
    var updateUser = {};
    updateUser[APIConstants.Query._ID] = userId;
    updateUser[Constants.Baas.Users.PASSWORD] = newPassword;
    var updateOptions = {};
    updateOptions[APIConstants.Query.ASK] = Constants.Baas.ASK;
    var updates = {};
    updates[APIConstants.Query.TABLE] = Constants.Baas.USERS;
    updates[APIConstants.Update.OPERATIONS] = [updateUser];
    updateOptions.updates = updates;
    updateOptions.callback = ApplaneCallback(callback, function () {
        callback(null, "Reset Password Successfully..");
    });
    UpdateEngine.performUpdate(updateOptions);
}

exports.logout = function (usk, callback) {
    if (usk) {
        CacheService.clear(usk);
        removeUserFromSession(usk, ApplaneCallback(callback, function (result) {
        }));
    }
    callback(null, "Logout Successfully..");
}

exports.getCurrentUser = function (usk, callback) {
    callback(null, CacheService.get(usk));
}


removeUserFromSession = function (usk, callback) {
    var query = {};
    query["usk"] = usk;
    MongoDBManager.remove("baas", "sessions__baas", query, {}, callback);
}


setSession = function (usk, userId, callback) {
    var operations = [];
    var update = {};
    update["usk"] = usk;
    update["user"] = userId;
    update["lastactivitydate"] = new Date();
    operations.push(update);
    var newUpdates = {};
    newUpdates[APIConstants.Query.TABLE] = "sessions__baas";
    newUpdates[APIConstants.Update.OPERATIONS] = operations;
    UpdateEngine.performUpdate({
        "ask":"baas",
        updates:newUpdates,
        callback:callback
    })
}
