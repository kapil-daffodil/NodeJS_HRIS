/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 4/8/13
 * Time: 2:26 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require("../../shared/Constants.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var ApplaneError = require("apputil/ApplaneError.js");
var DatabaseEngine = require("../../database/DatabaseEngine.js");
var UpdateEngine = require("../../database/UpdateEngine.js");
var QueryConstants = require("nodejsapi/Constants.js");
var Utils = require("apputil/util.js");
var ViewService = require("./ViewService.js");

exports.beforeInsert = function (params, callback) {
    Utils.iterateArray(params.operations, callback, function (viewGroup, callback) {
        if (!viewGroup[Constants.AppsStudio.ViewGroups.CREATOR]) {
            viewGroup[Constants.AppsStudio.ViewGroups.CREATOR] = params.user._id;
        }
        callback();
    });
}

exports.afterInsert = function (params, callback) {
    var that = this;
    var logid = params.logid;
    Utils.iterateArray(params.newvalues, callback, function (viewGroup, callback) {
        var menus = viewGroup[Constants.AppsStudio.ViewGroups.MENUS];
        if (menus && menus.length > 0) {
            populateMenuBaasViews(that, viewGroup, menus, logid, callback);
        } else {
            callback();
        }
    });
};

exports.afterUpdate = function (params, callback) {
    var that = this;
    var logid = params.logid;
    Utils.iterateArray(params.newvalues, callback, function (viewGroup, callback) {
        var menus = viewGroup[Constants.AppsStudio.ViewGroups.MENUS];
        if (menus && menus.length > 0) {
            populateMenuBaasViews(that, viewGroup, menus, logid, callback);
        } else {
            callback();
        }
    });
};

function populateMenuBaasViews(that, viewGroup, menus, logid, callback) {
    var viewGroupId = viewGroup[QueryConstants.Query._ID];
    Utils.iterateArray(menus, ApplaneCallback(callback, function () {
        addViewIdInMenus(menus, viewGroupId, logid, callback);
    }), function (menu, callback) {
        var menuTable = menu[Constants.AppsStudio.ViewGroups.Menus.TABLE];
        var menuLabel = menu[Constants.AppsStudio.ViewGroups.Menus.LABEL];
        var menuFilter = menu[Constants.AppsStudio.ViewGroups.Menus.FILTER];
        if (typeof menuFilter == "string") {
            if (menuFilter.trim().length == 0) {
                delete  menu[Constants.AppsStudio.ViewGroups.Menus.FILTER];
            } else {
                menu[Constants.AppsStudio.ViewGroups.Menus.FILTER] = JSON.parse(menuFilter);
            }
        }
        if (menuTable) {
            that.populateBaasViews(menuTable, menuLabel, logid, ApplaneCallback(callback, function (view) {
                if (view) {
                    menu[Constants.AppsStudio.ViewGroups.Menus.VIEWID] = view;
                }
                callback();
            }));
        } else {
            callback();
        }
    });

}

function addViewIdInMenus(menus, viewGroupId, logid, callback) {
    var viewGroupOperation = {};
    viewGroupOperation[Constants.AppsStudio.ViewGroups.MENUS] = menus;

    var updates = {};
    updates[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEW_GROUPS;
    viewGroupOperation[QueryConstants.Query._ID] = viewGroupId;
    updates[QueryConstants.Update.OPERATIONS] = viewGroupOperation;
    updates.excludejobs = true;
    UpdateEngine.performUpdate({
        ask:Constants.AppsStudio.ASK,
        logid:logid,
        updates:updates,
        callback:callback
    });
}

exports.populateBaasViews = function (table, label, logid, callback) {
    var baasViewFilter = {};
    var baasViewParameter = {};
    if (table[QueryConstants.Query._ID]) {
        baasViewFilter[Constants.Baas.Views.TABLE] = "{menuTableId__}";
        baasViewParameter.menuTableId__ = table[QueryConstants.Query._ID];
    } else {
        baasViewFilter[Constants.Baas.Views.TABLE + "." + Constants.Baas.Views.Tables.ID] = "{menuTableId__}";
        baasViewParameter.menuTableId__ = table[Constants.Baas.Views.Tables.ID];
    }
    var baasViewsQuery = {};
    baasViewsQuery[QueryConstants.Query.TABLE] = Constants.Baas.VIEWS;
    baasViewsQuery[QueryConstants.Query.COLUMNS] = [Constants.Baas.Views.ID];
    baasViewsQuery[QueryConstants.Query.FILTER] = baasViewFilter;
    baasViewsQuery[QueryConstants.Query.PARAMETERS] = baasViewParameter;

    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        logid:logid,
        query:baasViewsQuery,
        callback:ApplaneCallback(callback, function (baasViewsResult) {
            var baasViewsData = baasViewsResult[QueryConstants.Query.Result.DATA];
            if (baasViewsData && baasViewsData.length > 0) {
                var basicView = null;
                Utils.iterateArray(baasViewsData, ApplaneCallback(callback, function () {
                    callback(null, basicView);
                }), function (baasView, callback) {
                    populateViews(table, baasView, label, logid, ApplaneCallback(callback, function (view) {
                        if (view && !basicView) {
                            basicView = view;
                        }
                        callback();
                    }));
                });
            } else {
                callback();
            }
        })
    })
}

function populateViews(table, baasView, label, logid, callback) {
    var baasViewId = baasView[Constants.Baas.Views.ID];
    ViewService.getViewDetails(baasViewId, null, null, logid, ApplaneCallback(callback, function (viewData) {
        if (!viewData || viewData.length == 0) {
            var viewOperation = {};
            viewOperation[Constants.AppsStudio.Views.ID] = baasViewId;
            viewOperation[Constants.AppsStudio.Views.LABEL] = table[Constants.Baas.Tables.ID] == baasViewId ? label : baasViewId;
            var baasViewObject = {};
            baasViewObject[QueryConstants.Query._ID] = baasView[QueryConstants.Query._ID];
            viewOperation[Constants.AppsStudio.Views.BAAS_VIEWID] = baasViewObject;
            viewOperation[Constants.AppsStudio.Views.QUICK_VIEWID] = table[Constants.Baas.Tables.ID];

            var updates = {};
            updates[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
            updates[QueryConstants.Update.OPERATIONS] = viewOperation;
            UpdateEngine.performUpdate({
                ask:Constants.AppsStudio.ASK,
                logid:logid,
                updates:updates,
                callback:ApplaneCallback(callback, function (viewUpdateResult) {
                    var view = viewUpdateResult[QueryConstants.Update.Operation.Type.INSERT][0];
                    callback(null, view);
                })
            });
        } else {
            callback(null, viewData[0]);
        }
    }));
}
