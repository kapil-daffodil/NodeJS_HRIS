/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 4/8/13
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */

var RequestConstants = require("../../shared/RequestConstants.js");
var Constants = require("../../shared/Constants.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var ApplaneError = require("apputil/ApplaneError.js");
var DatabaseEngine = require("../../database/DatabaseEngine.js");
var UpdateEngine = require("../../database/UpdateEngine.js");
var QueryConstants = require("nodejsapi/Constants.js");
var ViewService = require("./ViewService.js");
var http = require('http');
var QueryString = require("querystring");


exports.getUserState = function (parameters, callback) {
    var user = parameters.user;
    var usk = parameters.usk;
    var logid = parameters.logid;
    if (!user || user.length == 0) {
        throw new ApplaneError(Constants.ErrorCode.SESSION_NOT_FOUND);
    }
    var state = parameters[RequestConstants.UserService.STATE];
    var userState = {};
    userState[Constants.UserService.USER_ID] = user[QueryConstants.Query._ID];
    userState[Constants.UserService.USER_NAME] = user[Constants.Baas.Users.USERNAME];
    userState[Constants.UserService.USER_GROUP] = user[Constants.Baas.Users.USERGROUPID];
    userState[Constants.UserService.LOGIN] = true;

    var userApplications = user[Constants.Baas.Users.APPLICATIONS];
    var userOrganizations = [];
    var userOrganizationsObj = {};
    getUserStates(user[QueryConstants.Query._ID], ApplaneCallback(callback, function (userStateUpdates) {
        if (userApplications && userApplications.length > 0) {
            var applicationsAsDeveloper = [];
            var globalOrganization = {};
            globalOrganization[Constants.Baas.Organizations.LABEL] = "Global";
            globalOrganization[Constants.Baas.Organizations.ID] = "global";
            globalOrganization[Constants.Baas.Organizations.OSK] = "52301c08dda05ef40700001f";
            globalOrganization.global = true;
            if (user[QueryConstants.Query._ID] == "5204c7600964c7dc09000001") {
                globalOrganization.admin = true;
            }
            for (var i = 0; i < userApplications.length; i++) {
                var userApplication = userApplications[i];
                if (userApplication.developer) {
                    applicationsAsDeveloper.push(userApplication[QueryConstants.Query._ID].toString());
                }
                var userOrganizationsArray = userApplication[Constants.Baas.Users.Applications.ORGANIZATIONS] ? userApplication[Constants.Baas.Users.Applications.ORGANIZATIONS] : [globalOrganization];
                userOrganizationsArray.forEach(function (userOrganization) {
                    var userOrganizationId = userOrganization[Constants.Baas.Organizations.ID];
                    var organization = userOrganizationsObj[userOrganizationId] ? userOrganizationsObj[userOrganizationId] : JSON.parse(JSON.stringify(userOrganization));
                    var organizationApplications = organization[Constants.Baas.Organizations.APPLICATIONS] ? organization[Constants.Baas.Organizations.APPLICATIONS] : [];
                    organizationApplications.push(userApplication);
                    organization[Constants.Baas.Organizations.APPLICATIONS] = organizationApplications;
                    userOrganizationsObj[userOrganizationId] = organization;
                });
            }
            for (var id in userOrganizationsObj) {
                userOrganizations.push(userOrganizationsObj[id]);
            }
            userState[Constants.UserService.ORGANIZATIONS] = userOrganizations;
            var selectedOrganizationIndex = getSelectedOrganizationIndex(userOrganizations, state, userStateUpdates);
            userState[Constants.UserService.ORGANIZATION_INDEX] = selectedOrganizationIndex;
            var selectedOrganization = userOrganizations[selectedOrganizationIndex];
            var organizationAdmin = selectedOrganization.admin;
            if (organizationAdmin) {
                userState[Constants.UserService.ORGANIZATION_ADMIN] = true;
            }
            var osk = selectedOrganization[Constants.Baas.Organizations.OSK];
            var userOrganizationApplications = selectedOrganization[Constants.Baas.Organizations.APPLICATIONS];
            if (userOrganizationApplications && userOrganizationApplications.length > 0) {
                getApplicationViewGroups(userOrganizationApplications, usk, user, logid, ApplaneCallback(callback, function (viewGroups) {
                    if (viewGroups && viewGroups.length > 0) {

                        userState[Constants.UserService.VIEW_GROUPS] = viewGroups;
                        var selectedViewGroupIndex = getSelectedViewGroupIndex(viewGroups, state, userStateUpdates);
                        userState[Constants.UserService.VIEW_GROUP_INDEX] = selectedViewGroupIndex;
                        var selectedViewGroup = viewGroups[selectedViewGroupIndex];
                        var selectedViewGroupLabel = selectedViewGroup[Constants.AppsStudio.ViewGroups.LABEL];
                        var viewGroupMenus = selectedViewGroup[Constants.AppsStudio.ViewGroups.MENUS];
                        if (viewGroupMenus && viewGroupMenus.length > 0) {
                            addExtraPropertiesInMenus(viewGroupMenus, osk, applicationsAsDeveloper);
                            sortMenus(viewGroupMenus);
                            userState[Constants.UserService.MENUS] = viewGroupMenus;
                            var selectedMenuDetails = getSelectedMenuDetails(viewGroupMenus, selectedViewGroupLabel, userStateUpdates);
                            var selectedMenuIndex = selectedMenuDetails.index;
                            var menuViewId = selectedMenuDetails.viewid;
                            var menuLabel = selectedMenuDetails.label;
                            userState[Constants.UserService.MENU_INDEX] = selectedMenuIndex;
                            var menu = viewGroupMenus[selectedMenuIndex];
                            var menuApplicationId = menu[Constants.AppsStudio.ViewGroups.Menus.APPLICATIONID];
                            var ask = menuApplicationId[Constants.Baas.Applications.ASK];
                            var viewInfo = {};
                            if (menu[Constants.AppsStudio.ViewGroups.Menus.FILTER]) {
                                viewInfo[QueryConstants.Query.FILTER] = menu[Constants.AppsStudio.ViewGroups.Menus.FILTER];
                            }
                            viewInfo[RequestConstants.AppsStudio.ViewInfo.VIEW_GROUP] = selectedViewGroupLabel;
                            viewInfo[RequestConstants.AppsStudio.ViewInfo.MENU_LABEL] = menuLabel;
                            viewInfo[RequestConstants.AppsStudio.ViewInfo.USER_STATE] = userStateUpdates;
                            if (organizationAdmin) {
                                viewInfo[RequestConstants.AppsStudio.ViewInfo.ORGANIZATION_ADMIN] = organizationAdmin;
                            }
                            if (menuApplicationId.developer) {
                                viewInfo[RequestConstants.AppsStudio.ViewInfo.APPLICATION_DEVELOPER] = true;
                            }
                            if (state.keepstructure) {
                                viewInfo.keepstructure = true;
                                var newMenusStructure = [];
                                for (var i = 0; i < viewGroupMenus.length; i++) {
                                    newMenusStructure.push({menus:viewGroupMenus[i]});
                                }
                                userState[Constants.UserService.MENUS] = newMenusStructure;
                            }
                            getConversionRates(ask, osk, ApplaneCallback(callback, function (conversionRates) {
                                var conversionRate = JSON.parse(conversionRates).response;
                                userState[Constants.UserService.CONVERSION_RATE] = conversionRate.conversionrate;
                                var vParameters = {};
                                vParameters[RequestConstants.AppsStudio.ASK] = ask;
                                vParameters[RequestConstants.AppsStudio.OSK] = osk;
                                vParameters[RequestConstants.AppsStudio.VIEWID] = menuViewId;
                                vParameters[RequestConstants.AppsStudio.VIEW_INFO] = viewInfo;
                                vParameters[RequestConstants.AppsStudio.USK] = parameters.usk;
                                vParameters[RequestConstants.AppsStudio.USER] = parameters.user;
                                vParameters[RequestConstants.AppsStudio.LOGID] = parameters.logid;
                                ViewService.getView(vParameters, function (err, view) {
                                    if (err) {
                                        userState.exception = err.stack;
                                    } else {
                                        userState[Constants.UserService.VIEW] = view;
                                    }
                                    callback(null, userState);
                                });
                            }));
                        } else {
                            userState[Constants.UserService.MENUS] = [];
                            updateUserState(user[QueryConstants.Query._ID], usk, userStateUpdates, ApplaneCallback(callback, function () {
                                callback(null, userState);
                            }));
                        }

                    } else {
                        callback(null, userState);
                    }
                }));

            } else {
                callback(null, userState);
            }
        } else {
            callback(null, userState);
        }
    }))
}

function sortMenus(menus) {
    var menuLength = menus.length;
    for (var i = 0; i < menuLength - 1; i++) {
        for (var j = 1; j < menuLength - i; j++) {
            var firstMenu = menus[j - 1];
            var firstMenuIndexObject = firstMenu[Constants.AppsStudio.ViewGroups.Menus.INDEX];
            var firstMenuIndex = 1;
            var firstMenuSubIndex;
            if (firstMenuIndexObject) {
                firstMenuIndex = firstMenuIndexObject[Constants.Baas.Tables.Columns.Type.Index.INDEX];
                firstMenuSubIndex = firstMenuIndexObject[Constants.Baas.Tables.Columns.Type.Index.SUB_INDEX];
            }
            var nextMenu = menus[j];
            var nextMenuIndexObject = nextMenu[Constants.AppsStudio.ViewGroups.Menus.INDEX];
            var nextMenuIndex = 1;
            var nextMenuSubIndex;
            if (nextMenuIndexObject) {
                nextMenuIndex = nextMenuIndexObject[Constants.Baas.Tables.Columns.Type.Index.INDEX];
                nextMenuSubIndex = nextMenuIndexObject[Constants.Baas.Tables.Columns.Type.Index.SUB_INDEX];
            }
            if (firstMenuIndex > nextMenuIndex || firstMenuSubIndex > nextMenuSubIndex) {
                menus[j - 1] = nextMenu;
                menus[j] = firstMenu;
            }
        }
    }
}

function addExtraPropertiesInMenus(viewGroupMenus, osk, applicationsAsDeveloper) {
    for (var i = 0; i < viewGroupMenus.length; i++) {
        var menu = viewGroupMenus[i];
        if (menu) {
            if (menu[Constants.AppsStudio.ViewGroups.Menus.FILTER]) {
                var menuFilter = menu[Constants.AppsStudio.ViewGroups.Menus.FILTER];
                if (typeof menuFilter == "string") {
                    menu[Constants.AppsStudio.ViewGroups.Menus.FILTER] = JSON.parse(menuFilter);
                }
            }
            var menuApplicationId = menu[Constants.AppsStudio.ViewGroups.Menus.APPLICATIONID];
            if (menuApplicationId && applicationsAsDeveloper.indexOf(menuApplicationId[QueryConstants.Query._ID].toString()) != -1) {
                menuApplicationId.developer = true;
            }
            menu.osk = osk;
        }
    }
}


function getSelectedOrganizationIndex(userOrganizations, state, userState) {
    if (state && state[RequestConstants.UserService.State.ORGANIZATION]) {
        var passedOrganization = state[RequestConstants.UserService.State.ORGANIZATION];
        for (var i = 0; i < userOrganizations.length; i++) {
            var userOrganization = userOrganizations[i];
            var organizationId = userOrganization[Constants.Baas.Organizations.ID];
            if (organizationId && organizationId == passedOrganization) {
                userState[Constants.Baas.Users.UserState.LAST_ORGANIZATION] = passedOrganization;
                return i;
            }
        }
    }
    if (userState && userState[Constants.Baas.Users.UserState.LAST_ORGANIZATION]) {
        var passedOrganization = userState[Constants.Baas.Users.UserState.LAST_ORGANIZATION];
        for (var i = 0; i < userOrganizations.length; i++) {
            var userOrganization = userOrganizations[i];
            var organizationId = userOrganization[Constants.Baas.Organizations.ID];
            if (organizationId && organizationId == passedOrganization) {
                return i;
            }
        }
    }
    userState[Constants.Baas.Users.UserState.LAST_ORGANIZATION] = userOrganizations[0][Constants.Baas.Organizations.ID];
    return 0;
}

function getSelectedViewGroupIndex(viewGroups, state, userState) {
    userState[Constants.Baas.Users.UserState.ORGANIZATION_WISE_STATE] = userState[Constants.Baas.Users.UserState.ORGANIZATION_WISE_STATE] || [];
    var organizationWiseStates = userState[Constants.Baas.Users.UserState.ORGANIZATION_WISE_STATE];
    var lastOrganization = userState[Constants.Baas.Users.UserState.LAST_ORGANIZATION];

    if (state && state[RequestConstants.UserService.State.VIEW_GROUP]) {
        var passedViewGroup = state[RequestConstants.UserService.State.VIEW_GROUP];
        for (var i = 0; i < viewGroups.length; i++) {
            var viewGroup = viewGroups[i];
            var viewGroupLabel = viewGroup[Constants.AppsStudio.ViewGroups.LABEL];
            if (viewGroupLabel && viewGroupLabel == passedViewGroup) {
                updateOrganizationWiseState(organizationWiseStates, lastOrganization, viewGroupLabel)
                return i;
            }
        }
    }
    if (organizationWiseStates.length > 0) {
        var passedViewGroup;
        for (var i = 0; i < organizationWiseStates.length; i++) {
            var organizationWiseState = organizationWiseStates[i];
            if (organizationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.ORGANIZATION] == lastOrganization) {
                passedViewGroup = organizationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.APPLICATION];
                break;
            }
        }
        if (passedViewGroup) {
            for (var i = 0; i < viewGroups.length; i++) {
                var viewGroup = viewGroups[i];
                var viewGroupLabel = viewGroup[Constants.AppsStudio.ViewGroups.LABEL];
                if (viewGroupLabel && viewGroupLabel == passedViewGroup) {
                    return i;
                }
            }
        }
    }
    var viewGroupLabel = viewGroups[0][Constants.AppsStudio.ViewGroups.LABEL];
    updateOrganizationWiseState(organizationWiseStates, lastOrganization, viewGroupLabel);
    return 0;
}

function updateOrganizationWiseState(organizationWiseStates, lastOrganization, viewGroupLabel) {
    var organizationApplicationWiseState = false;
    for (var j = 0; j < organizationWiseStates.length; j++) {
        var organizationWiseState = organizationWiseStates[j];
        if (organizationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.ORGANIZATION] == lastOrganization) {
            organizationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.APPLICATION] = viewGroupLabel;
            organizationApplicationWiseState = true;
            break;
        }
    }
    if (!organizationApplicationWiseState) {
        var organizationWiseState = {};
        organizationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.APPLICATION] = viewGroupLabel;
        organizationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.ORGANIZATION] = lastOrganization;
        organizationWiseStates.push(organizationWiseState);
    }
}

function getSelectedMenuDetails(viewGroupMenus, lastViewGroup, userState) {
    var applicationWiseStates = userState[Constants.Baas.Users.UserState.APPLICATION_WISE_STATE] || [];
    var lastOrganization = userState[Constants.Baas.Users.UserState.LAST_ORGANIZATION];

    var menuLabel;
    var menuViewId;
    if (applicationWiseStates.length > 0) {
        for (var i = 0; i < applicationWiseStates.length; i++) {
            var applicationWiseState = applicationWiseStates[i];
            if (applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.ORGANIZATION] == lastOrganization && applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.APPLICATION] == lastViewGroup) {
                menuLabel = applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.MENU];
                menuViewId = applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.QUICK_VIEW];
                break;
            }
        }
    }
    if (menuLabel) {
        for (var i = 0; i < viewGroupMenus.length; i++) {
            var viewGroupMenu = viewGroupMenus[i];
            var viewGroupLabel = viewGroupMenu ? viewGroupMenu[Constants.AppsStudio.ViewGroups.Menus.LABEL] : null;
            if (viewGroupLabel && viewGroupLabel == menuLabel) {
                var menuDetails = {};
                menuDetails.index = i;
                menuDetails.label = viewGroupLabel;
                menuDetails.viewid = menuViewId;
                return menuDetails;
            }
        }
    }
    var menuDetails = {};
    menuDetails.index = 0;
    menuDetails.label = viewGroupMenus[0][Constants.AppsStudio.ViewGroups.Menus.LABEL];
    menuDetails.viewid = viewGroupMenus[0][Constants.AppsStudio.ViewGroups.Menus.TABLE][Constants.Baas.Tables.ID];
    return menuDetails;
}


function getApplicationViewGroups(userOrganizationApplications, usk, user, logid, callback) {
    var applications = [];
    for (var i = 0; i < userOrganizationApplications.length; i++) {
        applications.push(userOrganizationApplications[i][QueryConstants.Query._ID]);
    }
    var orFilters = [];
    var applicationFilter = {};
    applicationFilter[Constants.AppsStudio.ViewGroups.MENUS + "." + Constants.AppsStudio.ViewGroups.Menus.APPLICATIONID + "." + QueryConstants.Query._ID] = {$in:applications};
    orFilters.push(applicationFilter);
    var creatorFilter = {};
    creatorFilter[Constants.AppsStudio.ViewGroups.CREATOR] = "{_CurrentUserId}";
    creatorFilter[Constants.AppsStudio.ViewGroups.MENUS] = null;
    orFilters.push(creatorFilter);

    var orders = [];
    var indexOrder = {};
    indexOrder[Constants.AppsStudio.ViewGroups.MENUS + "." + Constants.AppsStudio.ViewGroups.Menus.INDEX] = {$order:"asc"};
    orders.push(indexOrder);
    var parentViewGroupOrder = {};
    parentViewGroupOrder[Constants.AppsStudio.ViewGroups.PARENT_VIEW_GROUP] = {$order:"asc", $recursive:true};
    orders.push(parentViewGroupOrder);

    var viewQuery = {};
    viewQuery[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEW_GROUPS;
    viewQuery[QueryConstants.Query.FILTER] = {"$or":orFilters};
    viewQuery[QueryConstants.Query.ORDERS] = orders;
    DatabaseEngine.query({
        ask:Constants.AppsStudio.ASK,
        usk:usk,
        user:user,
        logid:logid,
        query:viewQuery,
        callback:ApplaneCallback(callback, function (viewGroupsResult) {
            var viewGroups = viewGroupsResult[QueryConstants.Query.Result.DATA];
            callback(null, viewGroups);
        })
    })
}

function updateUserState(userId, usk, userStateUpdates, callback) {
    if (userStateUpdates[Constants.Baas.Users.UserState.APPLICATION_WISE_STATE]) {
        delete userStateUpdates[Constants.Baas.Users.UserState.APPLICATION_WISE_STATE];
    }

    var updates = {};
    updates[QueryConstants.Query.TABLE] = Constants.Baas.USERS;
    var operation = {};
    operation[QueryConstants.Query._ID] = userId;
    operation.userstate = userStateUpdates;
    updates[QueryConstants.Update.OPERATIONS] = operation;
    updates.excludejobs = true;
    updates.excludemodules = true;
    UpdateEngine.performUpdate({
        ask:Constants.Baas.ASK,
        usk:usk,
        updates:updates,
        callback:callback
    });
}

function getUserStates(userId, callback) {
    var viewQuery = {};
    viewQuery[QueryConstants.Query.TABLE] = Constants.Baas.USERS;
    viewQuery[QueryConstants.Query.FILTER] = {"_id":userId};
    viewQuery[QueryConstants.Query.COLUMNS] = [Constants.Baas.Users.USER_STATE];

    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        query:viewQuery,
        callback:ApplaneCallback(callback, function (userStateResult) {
            var userStates = userStateResult[QueryConstants.Query.Result.DATA];
            var userState = {};
            if (userStates && userStates.length > 0 && userStates[0][Constants.Baas.Users.USER_STATE]) {
                userState = userStates[0][Constants.Baas.Users.USER_STATE];
            }
            callback(null, userState);
        })
    })
}


function getConversionRates(ask, osk, callback) {
    var options = {};
    options.ask = ask;
    options.osk = osk;

    var queryString = "";
    if (Object.keys(options).length > 0) {
        queryString = QueryString.stringify(options);
    }
    var serverOptions = {
        hostname:"baas.applane.com",
        port:1337,
        path:"/conversionrates",
        method:"POST",
        headers:{
            'Content-Type':'application/x-www-form-urlencoded',
            'Content-Length':queryString.length
        }
    };
    var req = http.request(serverOptions, function (res) {
        if (options.response) {
            res.setEncoding('binary');
        } else {
            res.setEncoding('utf8');
        }

        var body = '';
        res.on('data', function (chunk) {
            body += chunk;
        });

        res.on('end', function () {
            callback(null, body);
        });
    });

    req.on('error', function (err) {
        callback(err);
    });
    req.write(queryString);
    req.end();
}





