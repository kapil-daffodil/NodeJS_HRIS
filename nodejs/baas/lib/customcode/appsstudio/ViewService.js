/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 26/7/13
 * Time: 10:51 AM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require("../../shared/Constants.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var AppsStudioError = require("apputil/ApplaneError.js");
var DatabaseEngine = require("../../database/DatabaseEngine.js");
var UpdateEngine = require("../../database/UpdateEngine.js");
var QueryConstants = require("nodejsapi/Constants.js");
var RequestConstants = require("../../shared/RequestConstants.js");
var Utils = require("apputil/util.js");
var Self = require("./ViewService.js");
var MetadataProvider = require('../../metadata/MetadataProvider.js');

exports.getView = function (parameters, callback) {
    var viewId = parameters[RequestConstants.AppsStudio.VIEWID];
    if (!viewId) {
        throw new AppsStudioError(Constants.ErrorCode.FIELDS_BLANK, ["viewid"]);
    }
    var osk = parameters[RequestConstants.AppsStudio.OSK];
    var ask = parameters[RequestConstants.AppsStudio.ASK];
    var user = parameters[RequestConstants.AppsStudio.USER];
    var logid = parameters[RequestConstants.AppsStudio.LOGID];
    if (!user) {
        throw new AppsStudioError(Constants.ErrorCode.SESSION_NOT_FOUND);
    }
    parameters[RequestConstants.AppsStudio.VIEW_INFO] = parameters[RequestConstants.AppsStudio.VIEW_INFO] || {};
    var viewInfo = parameters[RequestConstants.AppsStudio.VIEW_INFO];
    var organizationAdmin = viewInfo[RequestConstants.AppsStudio.ViewInfo.ORGANIZATION_ADMIN] || false;
    var applicationDeveloper = viewInfo[RequestConstants.AppsStudio.ViewInfo.APPLICATION_DEVELOPER] || false;
    parameters.logtime = {};
    var startTime = new Date().getTime();
    MetadataProvider.getOrganization(osk, ApplaneCallback(callback, function (organizationDetail) {
        var organizationId = organizationDetail ? organizationDetail[QueryConstants.Query._ID] : organizationDetail;
        Self.getViewDetails(viewId, user[QueryConstants.Query._ID], organizationId, logid, ApplaneCallback(callback, function (viewResult) {
            if (!viewResult || viewResult.length == 0) {
                throw new AppsStudioError(Constants.ErrorCode.VIEW_NOT_FOUND, [viewId]);
//                callback(null, "View Not Found for view [" + viewId + "]");
            } else {
                var viewData = viewResult[0];
                var customization = viewData [Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(viewData[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
                customization[Constants.AppsStudio.Views.Customization.COLUMNS] = customization[Constants.AppsStudio.Views.Customization.COLUMNS] || {};
                var schedules = viewData [Constants.AppsStudio.Views.SCHEDULES] || [];
                if (!viewData[Constants.AppsStudio.Views.USERID] && !viewData[Constants.AppsStudio.Views.ORGANIZATIONID]) {
                    var customizationColumns = customization[Constants.AppsStudio.Views.Customization.COLUMNS];
                    for (var expression in customizationColumns) {
                        var customizationColumn = customizationColumns[expression];
                        var columnVisibility = customizationColumn[Constants.AppsStudio.ViewColumns.VISIBILITY];
                        if (columnVisibility instanceof Object) {
                            columnVisibility = columnVisibility._val;
                        }
                        if (columnVisibility == "Off" && applicationDeveloper) {
                            customizationColumn.developer = true;
                        }
                    }
                }
                var baasTableName = viewData[Constants.AppsStudio.Views.BAAS_VIEWID] [Constants.Baas.Views.TABLE][Constants.Baas.Tables.ID];
                var baasViewId = viewData[Constants.AppsStudio.Views.BAAS_VIEWID] [Constants.Baas.Views.ID];
                var quickViewId = viewData[Constants.AppsStudio.Views.QUICK_VIEWID];
                parameters.baasTableName = baasTableName;
                parameters.baasViewId = baasViewId;
                parameters.quickViewId = quickViewId;
                if (organizationId) {
                    parameters.organizationid = organizationId;
                    parameters.organization = organizationDetail[Constants.Baas.Organizations.ID];
                    Self.getUserOrganizationViewDetails(viewId, null, organizationId, logid, ApplaneCallback(callback, function (organizationViewDetails) {
                        if (organizationViewDetails && organizationViewDetails.length > 0) {
                            var organizationViewDetail = organizationViewDetails[0];
                            var organizationViewCustomization = organizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(organizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
                            Self.mergeViewCustomization(customization, organizationViewCustomization, organizationAdmin);
                            var schedulesToMerge = organizationViewDetail[Constants.AppsStudio.Views.SCHEDULES] || [];
                            schedules = Self.mergeViewSchedules(schedules, schedulesToMerge);
                        }
                        Self.getUserOrganizationViewDetails(viewId, user[QueryConstants.Query._ID], organizationId, logid, ApplaneCallback(callback, function (userOrganizationViewDetails) {
                            if (userOrganizationViewDetails && userOrganizationViewDetails.length > 0) {
                                var userOrganizationViewDetail = userOrganizationViewDetails[0];
                                var userOrganizationViewCustomization = userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
                                Self.mergeViewCustomization(customization, userOrganizationViewCustomization);
                                var schedulesToMerge = userOrganizationViewDetail[Constants.AppsStudio.Views.SCHEDULES] || [];
                                schedules = Self.mergeViewSchedules(schedules, schedulesToMerge);
                            }
                            parameters.customization = customization;
                            parameters.logtime.timemergestartCustomization = new Date().getTime() - startTime;
                            populateView(viewId, schedules, parameters, callback);
                        }));
                    }));
                } else {
                    Self.getUserOrganizationViewDetails(viewId, user[QueryConstants.Query._ID], null, logid, ApplaneCallback(callback, function (userOrganizationViewDetails) {
                        if (userOrganizationViewDetails && userOrganizationViewDetails.length > 0) {
                            var userOrganizationViewDetail = userOrganizationViewDetails[0];
                            var userOrganizationViewCustomization = userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
                            Self.mergeViewCustomization(customization, userOrganizationViewCustomization);
                            var schedulesToMerge = userOrganizationViewDetail[Constants.AppsStudio.Views.SCHEDULES] || [];
                            schedules = Self.mergeViewSchedules(schedules, schedulesToMerge);
                        }
                        parameters.customization = customization;
                        parameters.logtime.timeinmergestartCustomization = new Date().getTime() - startTime;
                        populateView(viewId, schedules, parameters, callback);
                    }));
                }
            }
        }));
    }));
};

exports.mergeViewSchedules = function (schedules, schedulesToMerge) {
    var schedulesMap = getScheduleMap(schedules);
    var schedulesToMergeMap = getScheduleMap(schedulesToMerge);
    for (var schedulesToMergeExp in schedulesToMergeMap) {
        if (schedulesMap[schedulesToMergeExp]) {
            var scheduleMapObject = schedulesMap[schedulesToMergeExp];
            var scheduleToMergeMapObject = schedulesToMergeMap[schedulesToMergeExp];
            for (var exp in scheduleToMergeMapObject) {
                scheduleMapObject[exp] = scheduleToMergeMapObject[exp];
            }
        } else {
            schedulesMap[schedulesToMergeExp] = schedulesToMergeMap[schedulesToMergeExp];
        }
    }
    var newSchedules = [];
    for (var exp in schedulesMap) {
        newSchedules.push(schedulesMap[exp]);
    }
    return newSchedules;
}

function getScheduleMap(schedules) {
    if (typeof schedules == "string") {
        schedules = JSON.parse(schedules);
    }
    var scheduleObject = {};
    for (var i = 0; i < schedules.length; i++) {
        var schedule = schedules[i];
        var scheduleId = schedule[Constants.AppsStudio.Views.Schedules.SCHEDULEID];
        var scheduleName = scheduleId[Constants.AppsStudio.Schedules.NAME];
        var scheduleViewId = scheduleId[Constants.AppsStudio.Schedules.VIEWID];
        scheduleObject[scheduleName + "___" + scheduleViewId] = schedule;
    }
    return scheduleObject;
}

exports.mergeViewCustomization = function (customizationInMerge, customizationToMerge, organizationAdmin) {
    var customizationInMergeColumns = customizationInMerge[Constants.AppsStudio.Views.Customization.COLUMNS] || {};
    var customizationToMergeColumns = customizationToMerge[Constants.AppsStudio.Views.Customization.COLUMNS];
    if (customizationToMergeColumns) {
        for (var expression in customizationToMergeColumns) {
            var toMergeColumnObject = customizationToMergeColumns[expression];
            customizationInMergeColumns[expression] = customizationInMergeColumns[expression] || {};
            var inMergeColumnObject = customizationInMergeColumns[expression];
            for (var exp in toMergeColumnObject) {
                inMergeColumnObject[exp] = toMergeColumnObject[exp];
            }
            var columnVisibility = inMergeColumnObject[Constants.AppsStudio.ViewColumns.VISIBILITY];
            if (columnVisibility instanceof Object) {
                columnVisibility = columnVisibility._val;
            }
            if (columnVisibility == "Off" && organizationAdmin) {
                inMergeColumnObject.admin = true;
            }
        }
        customizationInMerge[Constants.AppsStudio.Views.Customization.COLUMNS] = customizationInMergeColumns;
    }
    var customizationToMergeSequence = customizationToMerge[Constants.AppsStudio.Views.Customization.SEQUENCE];
    if (customizationToMergeSequence) {
        customizationInMerge[Constants.AppsStudio.Views.Customization.SEQUENCE] = customizationToMergeSequence;
    }
    var customizationToMergeChilds = customizationToMerge[Constants.AppsStudio.Views.Customization.CHILDS];
    if (customizationToMergeChilds) {
        customizationInMerge[Constants.AppsStudio.Views.Customization.CHILDS] = customizationToMergeChilds;
    }
    var customizationToMergeOrders = customizationToMerge[Constants.AppsStudio.Views.Customization.ORDERS];
    if (customizationToMergeOrders) {
        customizationInMerge[Constants.AppsStudio.Views.Customization.ORDERS] = customizationToMergeOrders;
    }
    var customizationToMergeAppliedFilters = customizationToMerge[Constants.AppsStudio.Views.Customization.APPLIED_FILTERS];
    if (customizationToMergeAppliedFilters) {
        customizationInMerge[Constants.AppsStudio.Views.Customization.APPLIED_FILTERS] = customizationToMergeAppliedFilters;
    }
}

exports.getUserOrganizationViewDetails = function (viewId, userId, organizationId, logid, callback) {
    var filter = {};
    filter[Constants.AppsStudio.Views.ID] = "{viewid__}";
    filter[Constants.AppsStudio.Views.USERID] = "{userid__}";
    filter[Constants.AppsStudio.Views.ORGANIZATIONID] = "{orgid__}";

    var viewQuery = {};
    viewQuery[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
    viewQuery[QueryConstants.Query.FILTER] = filter;
    viewQuery[QueryConstants.Query.PARAMETERS] = {"viewid__":viewId, "userid__":userId, "orgid__":organizationId};

    DatabaseEngine.query({
        ask:Constants.AppsStudio.ASK,
        logid:logid,
        query:viewQuery,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[QueryConstants.Query.Result.DATA];
            callback(null, data);
        })
    })
}

exports.getViewDetails = function (viewId, userId, organizationId, logid, callback) {
    var filter = {};
    filter[Constants.AppsStudio.Views.ID] = "{id}";
    filter[Constants.AppsStudio.Views.USERID] = null;
    filter[Constants.AppsStudio.Views.ORGANIZATIONID] = null;

    var viewQuery = {};
    viewQuery[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
    viewQuery[QueryConstants.Query.FILTER] = filter;
    viewQuery[QueryConstants.Query.PARAMETERS] = {"id":viewId};

    DatabaseEngine.query({
        ask:Constants.AppsStudio.ASK,
        logid:logid,
        query:viewQuery,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[QueryConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data);
            } else {
                if (organizationId) {
                    Self.getUserOrganizationViewDetails(viewId, null, organizationId, logid, ApplaneCallback(callback, function (data) {
                        if (data && data.length > 0) {
                            callback(null, data);
                        } else {
                            Self.getUserOrganizationViewDetails(viewId, userId, organizationId, logid, ApplaneCallback(callback, function (data) {
                                callback(null, data);
                            }));
                        }
                    }));
                } else {
                    Self.getUserOrganizationViewDetails(viewId, userId, organizationId, logid, ApplaneCallback(callback, function (data) {
                        callback(null, data);
                    }));
                }
            }
        })
    })
}

function mergeCustomizationFilter(customization, dataQuery) {
    if (customization && customization[Constants.AppsStudio.Views.Customization.FILTER]) {
        var customizationFilter = customization[Constants.AppsStudio.Views.Customization.FILTER];
        var dataQueryFilter = dataQuery[QueryConstants.Query.FILTER] || {};
        for (var filterExp in customizationFilter) {
            dataQueryFilter[filterExp] = customizationFilter[filterExp];
        }
        dataQuery[QueryConstants.Query.FILTER] = dataQueryFilter;
    }
}

function populateNewMetadata(newMetadata, viewType, baasTableName, baasViewId, childColumn, customization) {
    newMetadata[RequestConstants.AppsStudio.Metadata.TYPE] = viewType;
    newMetadata[RequestConstants.AppsStudio.Metadata.TABLE] = baasTableName;
    newMetadata[RequestConstants.AppsStudio.Metadata.BAAS_VIEW_ID] = baasViewId;
    if (childColumn) {
        newMetadata.primarycolumn = childColumn + "." + QueryConstants.Query._ID;
    }
    newMetadata[Constants.AppsStudio.Views.Customization.MAIN_TABLE_ID] = customization[Constants.AppsStudio.Views.Customization.MAIN_TABLE_ID] || baasTableName;
}

function mergeOrders(newMetadata, customization, dataQuery) {
    var orders = customization[Constants.AppsStudio.Views.Customization.ORDERS];
    if (orders) {
        newMetadata[QueryConstants.Query.ORDERS] = orders;
        dataQuery[QueryConstants.Query.ORDERS] = orders;
    }
}

function getFlexibleChild(tableData, parameters) {
    var flexibleChild = {};
    flexibleChild.label = "Manage Flex Columns";
    flexibleChild.visibility = "Table";
    flexibleChild.id = "Manage Flex Columns";
    flexibleChild.relatedcolumn = Constants.Baas.Flexfields.COLUMNVALUE;
    flexibleChild.viewid = "flexfields__baas";
    flexibleChild.system = true;
    var flexibleChildFilter = {};
    flexibleChildFilter[Constants.Baas.Flexfields.TABLE] = tableData[Constants.Baas.Tables.ID];
    if (tableData[Constants.Baas.Tables.ORGENABLED]) {
        flexibleChildFilter[Constants.Baas.Flexfields.ORGANIZATIONID] = parameters.organizationid.toString();
    }
    flexibleChild.filter = flexibleChildFilter;
    return flexibleChild;
}

function populateMetadataChilds(customization, tableData, parameters, newMetadata) {
    var customizationChilds = customization[Constants.AppsStudio.Views.Customization.CHILDS] || [];
    if (tableData && tableData[Constants.Baas.Tables.FLEXIBLE]) {
        var flexibleChild = getFlexibleChild(tableData, parameters);
        customizationChilds.push(flexibleChild);
    }
    var noOfChilds = customizationChilds.length;
    for (var i = 0; i < noOfChilds; i++) {
        var customizationChild = customizationChilds[i];
        customizationChild[RequestConstants.AppsStudio.ASK] = parameters[RequestConstants.AppsStudio.ASK];
        customizationChild[RequestConstants.AppsStudio.OSK] = parameters[RequestConstants.AppsStudio.OSK];
    }
    newMetadata[RequestConstants.AppsStudio.Metadata.CHILDS] = customizationChilds;
}

function populateView(viewId, schedules, parameters, callback) {
    var user = parameters[RequestConstants.AppsStudio.USER];
    var viewInfo = parameters[RequestConstants.AppsStudio.VIEW_INFO];
    var baasTableName = parameters.baasTableName;
    var baasViewId = parameters.baasViewId;
    var customization = parameters.customization;
    var startTime = new Date().getTime();
    getMetadataResult(baasTableName, baasViewId, parameters, ApplaneCallback(callback, function (metadataResult) {
        var metadata = metadataResult[QueryConstants.Query.Result.METADATA];
        parameters.logtime.timeingettingmetadataresult = new Date().getTime() - startTime;
        var columns = metadata[QueryConstants.Query.COLUMNS];
        var dataQuery = {};
        var newMetadata = {};
        startTime = new Date().getTime();
        populateDataQuery(dataQuery, baasTableName, baasViewId, newMetadata, viewInfo);
        if (user && user[Constants.Baas.Users.RECORD_LIMIT]) {
            dataQuery[QueryConstants.Query.MAX_ROWS] = user[Constants.Baas.Users.RECORD_LIMIT];
            newMetadata[QueryConstants.Query.MAX_ROWS] = user[Constants.Baas.Users.RECORD_LIMIT];
        }
        if (schedules && schedules.length > 0) {
            newMetadata[RequestConstants.AppsStudio.Metadata.SCHEDULES] = schedules;
        }
        var viewType = viewInfo[RequestConstants.AppsStudio.ViewInfo.TYPE] || "table";
        var childColumn = viewInfo[RequestConstants.AppsStudio.ViewInfo.CHILD_COLUMN];
//        mergeCustomizationFilter(customization, dataQuery);
        mergeAppliedFilters(newMetadata, customization, dataQuery);
        mergeOrders(newMetadata, customization, dataQuery);
        parameters.logtime.timeinmergeFilters = new Date().getTime() - startTime;
        startTime = new Date().getTime();
        populateNewMetadata(newMetadata, viewType, baasTableName, baasViewId, childColumn, customization);
        parameters.logtime.timeinpopulateNewMetadata = new Date().getTime() - startTime;
        startTime = new Date().getTime();
        getMainTableData(newMetadata[Constants.AppsStudio.Views.Customization.MAIN_TABLE_ID], parameters, ApplaneCallback(callback, function (tableData) {
            parameters.logtime.timeingetMainTableData = new Date().getTime() - startTime;
            startTime = new Date().getTime();
            populateMetadataChilds(customization, tableData, parameters, newMetadata);
            parameters.logtime.timeinpopulateMetadataChilds = new Date().getTime() - startTime;
            startTime = new Date().getTime();
            getQuickViews(parameters, ApplaneCallback(callback, function (quickViewData) {
                if (quickViewData && quickViewData.length > 0) {
                    addQuickViews(newMetadata, quickViewData, viewId, parameters);
                }
                parameters.logtime.timeingetQuickViews = new Date().getTime() - startTime;
                startTime = new Date().getTime();
                populateMetadataColumns(columns, childColumn, null, parameters, ApplaneCallback(callback, function (columnsObj) {
                    parameters.logtime.timeinpopulateMetadataColumns = new Date().getTime() - startTime;
                    var newColumnsObj = {};
                    startTime = new Date().getTime();
                    addDottedColumnsInColumns(columnsObj, newColumnsObj);
                    parameters.logtime.timeinaddDottedColumnsInColumns = new Date().getTime() - startTime;
                    startTime = new Date().getTime();
                    mergeColumnsCustomization(newMetadata, dataQuery, newColumnsObj, customization, parameters, ApplaneCallback(callback, function () {
                        parameters.logtime.timeinmergeColumnsCustomization = new Date().getTime() - startTime;
                        getViewData(viewId, parameters, viewInfo, dataQuery, newMetadata, callback);
                    }));
                }));
            }));
        }));
    }))
}

function addDottedColumnsInColumns(columns, newColumns) {
    for (var expression in columns) {
        var columnMetadata = columns[expression];
        newColumns[expression] = columnMetadata;
        var columnType = columnMetadata[Constants.Baas.Tables.Columns.TYPE];
        var columnMetadataColumns = columnMetadata[Constants.Baas.Tables.COLUMNS];
        if (columnMetadataColumns && typeof columnMetadataColumns == "string") {
            columnMetadataColumns = JSON.parse(columnMetadataColumns);
            columnMetadata[Constants.Baas.Tables.COLUMNS] = columnMetadataColumns;
        }
        var columnMetadataReplicateColumns = columnMetadata[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS];
        if (columnMetadataReplicateColumns && typeof columnMetadataReplicateColumns == "string") {
            columnMetadataReplicateColumns = JSON.parse(columnMetadataReplicateColumns);
        }
        if (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP && columnMetadataReplicateColumns && columnMetadataReplicateColumns.length > 0) {
            for (var i = 0; i < columnMetadataReplicateColumns.length; i++) {
                var replicateColumn = JSON.parse(JSON.stringify(columnMetadataReplicateColumns[i]));
                replicateColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] = "None";
                var replicateColumnExpression = replicateColumn[Constants.Baas.Tables.Columns.EXPRESSION];
                replicateColumn[Constants.Baas.Tables.Columns.EXPRESSION] = expression + "." + replicateColumnExpression;
                newColumns[expression + "." + replicateColumnExpression] = replicateColumn;
            }
        }
        if ((columnType == Constants.Baas.Tables.Columns.Type.OBJECT || columnType == Constants.Baas.Tables.Columns.Type.LOOKUP) && columnMetadataColumns) {
            var innerColumns = columnMetadata[Constants.Baas.Tables.COLUMNS];
            var newInnerColumns = {};
            for (var i = 0; i < innerColumns.length; i++) {
                var innerColumn = JSON.parse(JSON.stringify(innerColumns[i]));
                innerColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] = "None";
                innerColumn.pexpression = expression;
                var innerColumnExpression = innerColumn[Constants.Baas.Tables.Columns.EXPRESSION];
                innerColumn[Constants.Baas.Tables.Columns.EXPRESSION] = expression + "." + innerColumnExpression;
                newColumns[expression + "." + innerColumnExpression] = innerColumn;
                newInnerColumns[expression + "." + innerColumnExpression] = innerColumn;
            }
            addDottedColumnsInColumns(newInnerColumns, newColumns);
        }
    }
}

function getMetadataResult(baasTableName, baasViewId, parameters, callback) {
    var metadataQuery = {};
    metadataQuery[QueryConstants.Query.TABLE] = baasTableName;
    metadataQuery[QueryConstants.Query.VIEW] = baasViewId;
    metadataQuery[QueryConstants.Query.METADATA] = true;
    metadataQuery[QueryConstants.Query.MAX_ROWS] = 0;

    DatabaseEngine.query({
        ask:parameters[RequestConstants.AppsStudio.ASK],
        osk:parameters[RequestConstants.AppsStudio.OSK],
        usk:parameters[RequestConstants.AppsStudio.USK],
        user:parameters[RequestConstants.AppsStudio.USER],
        logid:parameters[RequestConstants.AppsStudio.LOGID],
        query:metadataQuery,
        callback:ApplaneCallback(callback, function (metadataResult) {
            callback(null, metadataResult);
        })
    })
}

function getMainTableData(tableName, parameters, callback) {
    var tableFilter = {};
    tableFilter[Constants.Baas.Tables.ID] = "{tablename__}";

    var quickViewQuery = {};
    quickViewQuery[QueryConstants.Query.TABLE] = Constants.Baas.TABLES;
    quickViewQuery[QueryConstants.Query.FILTER] = tableFilter;
    quickViewQuery[QueryConstants.Query.PARAMETERS] = {"tablename__":tableName};

    DatabaseEngine.query({
        ask:Constants.AppsStudio.ASK,
        logid:parameters[RequestConstants.AppsStudio.LOGID],
        query:quickViewQuery,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[QueryConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data[0]);
            } else {
                callback();
            }
        })
    })
}

function populateDataQuery(dataQuery, baasTableName, baasViewId, metadata, viewInfo) {
    dataQuery[QueryConstants.Query.TABLE] = baasTableName;
    dataQuery[QueryConstants.Query.VIEW] = baasViewId;
    dataQuery[QueryConstants.Query.FILTER] = viewInfo[RequestConstants.AppsStudio.ViewInfo.FILTER];
    var viewParameters = viewInfo[RequestConstants.AppsStudio.ViewInfo.PARAMETERS] || {};
    if (viewInfo[RequestConstants.AppsStudio.ViewInfo.PARAMETER_MAPPINGS]) {
        var viewParameterMappings = viewInfo[RequestConstants.AppsStudio.ViewInfo.PARAMETER_MAPPINGS];
        metadata[RequestConstants.AppsStudio.Metadata.PARAMETER_MAPPINGS] = viewParameterMappings;
        viewParameters = resolveParameterMappings(dataQuery, viewParameters, viewParameterMappings);
    }
    dataQuery[QueryConstants.Query.PARAMETERS] = viewParameters;
}

function resolveParameterMappings(dataQuery, viewParameters, viewParameterMappings) {
    var newParameters = {};
    var selectedKeys = viewParameters.selectedkeys;
    if (selectedKeys) {
        newParameters.selectedkeys = selectedKeys;
    }
    for (var parameterExpression in viewParameters) {
        if (isParameter(parameterExpression)) {
            newParameters[parameterExpression] = viewParameters[parameterExpression];
        }
    }
    if (viewParameters._id) {
        newParameters._id = viewParameters._id;
    }
    Object.keys(viewParameterMappings).forEach(function (k) {
        newParameters[k] = viewParameters[viewParameterMappings[k]]
    });

    return newParameters;
}

function getQuickViews(parameters, callback) {
    var organizationId = parameters.organizationid;
    var userId = parameters[RequestConstants.AppsStudio.USER][QueryConstants.Query._ID];

    var quickViewFilter = {};
    quickViewFilter[Constants.AppsStudio.Views.QUICK_VIEWID] = "{id}";

    var quickViewQuery = {};
    quickViewQuery[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
    quickViewQuery[QueryConstants.Query.COLUMNS] = [Constants.AppsStudio.Views.ID, Constants.AppsStudio.Views.QUICK_VIEWID, Constants.AppsStudio.Views.LABEL, Constants.AppsStudio.Views.USERID, Constants.AppsStudio.Views.ORGANIZATIONID];
    quickViewQuery[QueryConstants.Query.FILTER] = quickViewFilter;
    quickViewQuery[QueryConstants.Query.PARAMETERS] = {"id":parameters.quickViewId};

    DatabaseEngine.query({
        ask:Constants.AppsStudio.ASK,
        logid:parameters[RequestConstants.AppsStudio.LOGID],
        query:quickViewQuery,
        callback:ApplaneCallback(callback, function (quickViewResult) {
            var quickViewData = quickViewResult[QueryConstants.Query.Result.DATA];
            if (quickViewData && quickViewData.length > 0) {
                var newQuickViewData = [];
                var quickViewsObj = {};
                for (var i = 0; i < quickViewData.length; i++) {
                    var quickView = quickViewData[i];
                    var viewId = quickView[Constants.AppsStudio.Views.ID];
                    if (quickViewsObj[viewId]) {
                        quickViewsObj[viewId].push(quickView);
                    } else {
                        var quickViewsArray = [];
                        quickViewsArray.push(quickView);
                        quickViewsObj[viewId] = quickViewsArray;
                    }
                }
                for (var viewId in quickViewsObj) {
                    var quickViewsArray = quickViewsObj[viewId];
                    if (quickViewsArray.length == 1) {
                        newQuickViewData.push(quickViewsArray[0]);
                    } else {
                        var addUserQuickView = false;
                        for (var i = 0; i < quickViewsArray.length; i++) {
                            var quickView = quickViewsArray[i];
                            var quickViewUserId = quickView[Constants.AppsStudio.Views.USERID];
                            var quickViewOrganizationId = quickView[Constants.AppsStudio.Views.ORGANIZATIONID];
                            if (quickViewUserId && quickViewUserId[QueryConstants.Query._ID] == userId && (!quickViewOrganizationId || quickViewOrganizationId[QueryConstants.Query._ID] == organizationId)) {
                                addUserQuickView = true;
                                newQuickViewData.push(quickView);
                                break;
                            }
                        }
                        if (!addUserQuickView) {
                            var addOrganizationQuickView = false;
                            for (var i = 0; i < quickViewsArray.length; i++) {
                                var quickView = quickViewsArray[i];
                                var quickViewUserId = quickView[Constants.AppsStudio.Views.USERID];
                                var quickViewOrganizationId = quickView[Constants.AppsStudio.Views.ORGANIZATIONID]
                                if ((!quickViewUserId || quickViewUserId[QueryConstants.Query._ID] == userId) && quickViewOrganizationId && quickViewOrganizationId[QueryConstants.Query._ID] == organizationId) {
                                    addOrganizationQuickView = true;
                                    newQuickViewData.push(quickView);
                                    break;
                                }
                            }
                            if (!addOrganizationQuickView) {
                                for (var i = 0; i < quickViewsArray.length; i++) {
                                    var quickView = quickViewsArray[i];
                                    var quickViewUserId = quickView[Constants.AppsStudio.Views.USERID];
                                    var quickViewOrganizationId = quickView[Constants.AppsStudio.Views.ORGANIZATIONID]
                                    if (!quickViewUserId && !quickViewOrganizationId) {
                                        newQuickViewData.push(quickView);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                callback(null, newQuickViewData);
            } else {
                callback();
            }
        })
    })
}

function mergeAppliedFilters(newMetadata, customization, dataQuery) {
    var appliedFilters = customization ? customization[Constants.AppsStudio.Views.Customization.APPLIED_FILTERS] || {} : {};
    if (Object.keys(appliedFilters).length > 0) {
        var appliedFiltersClone = JSON.parse(JSON.stringify(appliedFilters));
        dataQuery[QueryConstants.Query.FILTER] = dataQuery[QueryConstants.Query.FILTER] || {};
        var dataQueryFilters = dataQuery[QueryConstants.Query.FILTER];
        for (var appliedFilterExp in appliedFiltersClone) {
            var appliedFilter = appliedFiltersClone[appliedFilterExp];
            if (appliedFilter instanceof Object && !(appliedFilter instanceof Array) && appliedFilter.label) {
                delete appliedFilter.label;
            }
            dataQueryFilters[appliedFilterExp] = appliedFilter;
        }
        newMetadata.filterparameters = appliedFilters;
    }
}

function addQuickViews(newMetadata, quickViewData, viewId, parameters) {
    var quickViewCount = quickViewData.length;
    var quickViewIndex = 0;
    for (var i = 0; i < quickViewCount; i++) {
        var quickView = quickViewData[i];
        quickView.ask = parameters[RequestConstants.AppsStudio.ASK];
        quickView.osk = parameters[RequestConstants.AppsStudio.OSK];
        if (quickView[Constants.AppsStudio.Views.ID] == viewId) {
            quickViewIndex = i;
        }
    }
    newMetadata[RequestConstants.AppsStudio.Metadata.QUICK_VIEWS] = quickViewData;
    newMetadata[RequestConstants.AppsStudio.Metadata.QUICK_VIEW_INDEX] = quickViewIndex;
}

function getColumn(expression, columns) {
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        var columnExpression = column[QueryConstants.Query.Columns.EXPRESSION];
        if (expression == columnExpression) {
            return column;
        }
    }
}

function getMergedColumns(column) {
    var primaryColumnsExp = getPrimaryColumnExp(column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS]);
    var mergedColumns = column[Constants.Baas.Tables.COLUMNS] || [];
    var replicateColumns = column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS] || [];
    if (replicateColumns && replicateColumns.length > 0) {
        for (var i = 0; i < replicateColumns.length; i++) {
            var replicateColumn = replicateColumns[i];
            var replicateColumnExpression = replicateColumn[Constants.Baas.Tables.Columns.EXPRESSION];
            if (primaryColumnsExp[0] != replicateColumnExpression) {
                mergedColumns.push(replicateColumn);
            }
        }
    }
    return mergedColumns;
}

function getPrimaryColumnExp(primaryColumns) {
    var primaryColumnsExp = [];
    if (primaryColumns) {
        if (typeof primaryColumns == 'string') {
            primaryColumns = JSON.parse(primaryColumns);
        }
        for (var i = 0; i < primaryColumns.length; i++) {
            primaryColumnsExp.push(primaryColumns[i][Constants.Baas.Tables.Columns.EXPRESSION]);
        }
    }
    return primaryColumnsExp;
}

function addMultipleNotReplicateColumns(childLookupTable, columnExpression, columnsObj, parameters, callback) {
    var filter = {};
    filter[Constants.Baas.Tables.ID] = "{id}";

    var viewQuery = {};
    viewQuery[QueryConstants.Query.TABLE] = Constants.Baas.TABLES;
    viewQuery[QueryConstants.Query.COLUMNS] = [Constants.Baas.Tables.COLUMNS];
    viewQuery[QueryConstants.Query.FILTER] = filter;
    viewQuery[QueryConstants.Query.PARAMETERS] = {"id":childLookupTable};

    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        logid:parameters[RequestConstants.AppsStudio.LOGID],
        query:viewQuery,
        callback:ApplaneCallback(callback, function (result) {
            var columns = result[QueryConstants.Query.Result.DATA][0][Constants.Baas.Tables.COLUMNS];
            for (var i = 0; i < columns.length; i++) {
                var column = JSON.parse(JSON.stringify(columns[i]));
                var replicate = column[Constants.Baas.Tables.Columns.REPLICATE];
                var multiple = column[Constants.Baas.Tables.Columns.MULTIPLE];
                var columnType = column[Constants.Baas.Tables.Columns.TYPE];
                if ((!replicate || replicate != true) && (multiple && multiple == true) && (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP || columnType == Constants.Baas.Tables.Columns.Type.OBJECT)) {
//                column[Constants.AppsStudio.ViewColumns.LABEL] = column[Constants.AppsStudio.ViewColumns.LABEL] || column[Constants.AppsStudio.ViewColumns.EXPRESSION];
//                column[Constants.AppsStudio.ViewColumns.WIDTH] = column[Constants.AppsStudio.ViewColumns.WIDTH] || 200;
                    column[Constants.Baas.Tables.Columns.EXPRESSION] = columnExpression + "." + column[Constants.Baas.Tables.Columns.EXPRESSION];
                    column[Constants.AppsStudio.ViewColumns.VISIBILITY] = "Child";
                    columnsObj[column[Constants.Baas.Tables.Columns.EXPRESSION]] = column;
                }
            }
            callback();
        })
    })
}


function populateMetadataColumns(columns, childColumn, parentExp, parameters, callback) {
    var mainColumn;
    if (childColumn) {
        var indexOfDot = childColumn.indexOf(".");
        var mainExpression = indexOfDot > 0 ? childColumn.substr(0, indexOfDot) : childColumn;
        mainColumn = getColumn(mainExpression, columns);
        var mainInnerColumns = getMergedColumns(mainColumn);
        if (indexOfDot > 0) {
            return populateMetadataColumns(mainInnerColumns, childColumn.substr(indexOfDot + 1), (parentExp ? parentExp + "." + mainExpression : mainExpression), parameters, callback);
        }
        var columnsObj = {};
        var mainColumnType = mainColumn[Constants.Baas.Tables.Columns.TYPE];
        if (mainColumnType == Constants.Baas.Tables.Columns.Type.LOOKUP) {
//            mainColumn[Constants.AppsStudio.ViewColumns.LABEL] = mainColumn[Constants.AppsStudio.ViewColumns.LABEL] || mainColumn[Constants.Baas.Tables.Columns.EXPRESSION];
//            mainColumn[Constants.AppsStudio.ViewColumns.WIDTH] = mainColumn[Constants.AppsStudio.ViewColumns.WIDTH] || 200;
            mainColumn[Constants.Baas.Tables.Columns.EXPRESSION] = parentExp ? parentExp + "." + mainColumn[Constants.Baas.Tables.Columns.EXPRESSION] : mainColumn[Constants.Baas.Tables.Columns.EXPRESSION];
            mainColumn[Constants.Baas.Tables.Columns.MULTIPLE] = false;
            columnsObj[ mainColumn[Constants.Baas.Tables.Columns.EXPRESSION]] = mainColumn;
        }
        addDefaultIdColumns(columnsObj, parentExp ? parentExp + "." + childColumn + "." + QueryConstants.Query._ID : childColumn + "." + QueryConstants.Query._ID);
        for (var i = 0; i < mainInnerColumns.length; i++) {
            var mainInnerColumn = JSON.parse(JSON.stringify(mainInnerColumns[i]));
//            mainInnerColumn[Constants.AppsStudio.ViewColumns.LABEL] = mainInnerColumn[Constants.AppsStudio.ViewColumns.LABEL] || mainInnerColumn[Constants.Baas.Tables.Columns.EXPRESSION];
//            mainInnerColumn[Constants.AppsStudio.ViewColumns.WIDTH] = mainInnerColumn[Constants.AppsStudio.ViewColumns.WIDTH] || 200;
            mainInnerColumn[Constants.Baas.Tables.Columns.EXPRESSION] = parentExp ? parentExp + "." + childColumn + "." + mainInnerColumn[Constants.Baas.Tables.Columns.EXPRESSION] : childColumn + "." + mainInnerColumn[Constants.Baas.Tables.Columns.EXPRESSION];
            columnsObj[mainInnerColumn[Constants.Baas.Tables.Columns.EXPRESSION]] = mainInnerColumn;
        }
        if (mainColumnType == Constants.Baas.Tables.Columns.Type.LOOKUP) {
            var childLookupTable = mainColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
            addMultipleNotReplicateColumns(childLookupTable, (parentExp ? parentExp + "." + childColumn : childColumn), columnsObj, parameters, ApplaneCallback(callback, function () {
                callback(null, columnsObj);
            }))
        } else {
            callback(null, columnsObj);
        }
    } else {
        var columnsObj = {};
        for (var i = 0; i < columns.length; i++) {
            var column = columns[i];
            var expression = column[QueryConstants.Query.Columns.EXPRESSION];
            if (expression == "_organizationid_") {
                continue;
            }
//            column[Constants.AppsStudio.ViewColumns.LABEL] = column[Constants.AppsStudio.ViewColumns.LABEL] || expression;
//            column[Constants.AppsStudio.ViewColumns.WIDTH] = column[Constants.AppsStudio.ViewColumns.WIDTH] || 200;
            columnsObj[expression] = column;
        }
        callback(null, columnsObj);
    }
}

function addDefaultIdColumns(columns, columnExpression) {
    var childColumnProperties = {};
    childColumnProperties[Constants.AppsStudio.ViewColumns.LABEL] = "_Id";
    childColumnProperties[Constants.AppsStudio.ViewColumns.WIDTH] = 200;
    childColumnProperties[Constants.AppsStudio.ViewColumns.TYPE] = Constants.Baas.Tables.Columns.Type.STRING;
    childColumnProperties[Constants.AppsStudio.ViewColumns.VISIBILITY] = "Query";
    childColumnProperties[Constants.AppsStudio.ViewColumns.EXPRESSION] = columnExpression;
    columns[columnExpression] = childColumnProperties;
}

function mergeExtraProperties(customizeColumns, columnExpression, parameters, metadataColumn, newMetadataColumns) {
    var customizeColumn = customizeColumns[columnExpression];
    if (customizeColumn) {
        var columnVisibility = customizeColumn[Constants.AppsStudio.ViewColumns.VISIBILITY];
//        if (columnVisibility && columnVisibility == "Off") {
//            return;
//        }
        if (columnVisibility && columnVisibility == "Child") {
            customizeColumn.ask = parameters[RequestConstants.AppsStudio.ASK];
            customizeColumn.osk = parameters[RequestConstants.AppsStudio.OSK];
        }
    }
    for (var expression in customizeColumn) {
        metadataColumn[expression] = customizeColumn[expression];
    }
    newMetadataColumns.push(metadataColumn);
}

function getInnerColumns(postStr, metadataColumn) {
    var innerColumns = metadataColumn[Constants.Baas.Tables.COLUMNS];
    if (innerColumns && innerColumns.length > 0) {
        for (var j = 0; j < innerColumns.length; j++) {
            if (innerColumns[j][QueryConstants.Query.Columns.EXPRESSION] == postStr) {
                return JSON.parse(JSON.stringify(innerColumns[j]));
            }
        }
    }
    var replicateColumns = metadataColumn[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS];
    if (replicateColumns && replicateColumns.length > 0) {
        for (var j = 0; j < replicateColumns.length; j++) {
            if (replicateColumns[j][QueryConstants.Query.Columns.EXPRESSION] == postStr) {
                return JSON.parse(JSON.stringify(replicateColumns[j]));
            }
        }
    }
    return {};
}

function populateFlexColumns(flexibleColumms, newMetadataColumn) {
    var flexColumns = {};
    for (var i = 0; i < flexibleColumms.length; i++) {
        var flexColumn = flexibleColumms[i];
        if (flexColumn[Constants.Baas.Flexfields.COLUMNS] && typeof flexColumn[Constants.Baas.Flexfields.COLUMNS] == "string") {
            flexColumn[Constants.Baas.Flexfields.COLUMNS] = JSON.parse(flexColumn[Constants.Baas.Flexfields.COLUMNS]);
        }
        if (flexColumn[Constants.Baas.Flexfields.PRIMARY_COLUMNS] && typeof flexColumn[Constants.Baas.Flexfields.PRIMARY_COLUMNS] == "string") {
            flexColumn[Constants.Baas.Flexfields.PRIMARY_COLUMNS] = JSON.parse(flexColumn[Constants.Baas.Flexfields.PRIMARY_COLUMNS]);
        }
        if (flexColumn[Constants.Baas.Flexfields.REPLICATE_COLUMNS] && typeof flexColumn[Constants.Baas.Flexfields.REPLICATE_COLUMNS] == "string") {
            flexColumn[Constants.Baas.Flexfields.REPLICATE_COLUMNS] = JSON.parse(flexColumn[Constants.Baas.Flexfields.REPLICATE_COLUMNS]);
        }
        var flexColumnValue = flexColumn[Constants.Baas.Flexfields.COLUMNVALUE];
        if (flexColumns[flexColumnValue]) {
            flexColumns[flexColumnValue].push(flexColumn);
        } else {
            var flexColumnsArray = [];
            flexColumnsArray.push(flexColumn);
            flexColumns[flexColumnValue] = flexColumnsArray;
        }
    }
    newMetadataColumn.flexcolumns = flexColumns;
}

function mergeColumnsCustomization(newMetadata, dataQuery, metadataColumns, customization, parameters, callback) {
    var organizationAdmin = parameters[RequestConstants.AppsStudio.VIEW_INFO][RequestConstants.AppsStudio.ViewInfo.ORGANIZATION_ADMIN] || false;
    var applicationDeveloper = parameters[RequestConstants.AppsStudio.VIEW_INFO][RequestConstants.AppsStudio.ViewInfo.APPLICATION_DEVELOPER] || false;
    var newMetadataColumns = [];
    var sequence = customization ? customization[Constants.AppsStudio.Views.Customization.SEQUENCE] : null;
    var customizeColumns = customization ? customization[Constants.AppsStudio.Views.Customization.COLUMNS] : null;
    if (sequence && customizeColumns) {
        var columnsCount = sequence.length;
        for (var i = 0; i < columnsCount; i++) {
            var columnExpression = sequence[i];
            var metadataColumn = metadataColumns[columnExpression];
            if (metadataColumn) {
                mergeExtraProperties(customizeColumns, columnExpression, parameters, metadataColumn, newMetadataColumns);
            } else {
                var indexOf = columnExpression.indexOf(".");
                if (indexOf > 0) {
                    var preStr = columnExpression.substr(0, indexOf);
                    var postStr = columnExpression.substr(indexOf + 1);
                    metadataColumn = metadataColumns[preStr];
                    if (metadataColumn) {
                        var innerColumn = getInnerColumns(postStr, metadataColumn);
                        innerColumn[Constants.AppsStudio.ViewColumns.EXPRESSION] = columnExpression;
                        mergeExtraProperties(customizeColumns, columnExpression, parameters, innerColumn, newMetadataColumns)
                    }
                }
            }
        }
        for (var expression in metadataColumns) {
            if (sequence.indexOf(expression) == -1) {
                var metadataColumn = metadataColumns[expression]
                mergeExtraProperties(customizeColumns, expression, parameters, metadataColumn, newMetadataColumns);
            }
        }
    } else {
        for (var expression in metadataColumns) {
            newMetadataColumns.push(metadataColumns[expression]);
        }
    }
    var metadataColumnWithPExpression = [];
    for (var i = 0; i < newMetadataColumns.length; i++) {
        var newMetadataColumn = newMetadataColumns[i];
        var newColumnExpression = newMetadataColumn[Constants.Baas.Tables.Columns.EXPRESSION];
        if (newMetadataColumn.pexpression) {
            metadataColumnWithPExpression.push(newMetadataColumn);
        }
        var columnVisibility = newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY];
        if (columnVisibility instanceof Object) {
            columnVisibility = columnVisibility._val;
        }
        if (columnVisibility == "Embed") {
            for (var j = 0; j < newMetadataColumns.length; j++) {
                var newMetadataInnerColumn = newMetadataColumns[j];
                var innerColumnPExpression = newMetadataInnerColumn.pexpression
                if (innerColumnPExpression && (innerColumnPExpression == newColumnExpression || innerColumnPExpression.indexOf(newColumnExpression + ".") != -1)) {
                    newMetadataInnerColumn.subvisibility = "Embed";
                }
            }
        }
    }

    var queryColumns = [];
    var newMetadataColumnsArray = [];
    var columnExpressionsArray = [];
    Utils.iterateArray(newMetadataColumns, ApplaneCallback(callback, function () {
        mergeInnerColumns(newMetadataColumnsArray, metadataColumnWithPExpression);
        dataQuery[QueryConstants.Query.COLUMNS] = queryColumns;
        newMetadata[RequestConstants.AppsStudio.Metadata.COLUMNS] = newMetadataColumnsArray;
        newMetadata.columnexpressionsarray = columnExpressionsArray;
        callback();
    }), function (newMetadataColumn, callback) {
        var primaryColumns = newMetadataColumn[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS];
        if (primaryColumns && (typeof primaryColumns == 'string')) {
            newMetadataColumn[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] = JSON.parse(primaryColumns);
        }
        var replicateColumns = newMetadataColumn[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS];
        if (replicateColumns && (typeof replicateColumns == 'string')) {
            newMetadataColumn[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS] = JSON.parse(replicateColumns);
        }
        var newColumnExpression = newMetadataColumn[Constants.Baas.Tables.Columns.EXPRESSION];
        columnExpressionsArray.push(newColumnExpression);
        if (!newMetadataColumn[Constants.AppsStudio.ViewColumns.LABEL]) {
            if (newColumnExpression.lastIndexOf(".") != -1) {
                newMetadataColumn[Constants.AppsStudio.ViewColumns.LABEL] = parseLabel(newColumnExpression.substr(newColumnExpression.lastIndexOf(".") + 1));
            } else {
                newMetadataColumn[Constants.AppsStudio.ViewColumns.LABEL] = parseLabel(newColumnExpression);
            }
        }
        var columnVisibility = newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY];
        var columnMultiple = newMetadataColumn[Constants.AppsStudio.ViewColumns.MULTIPLE];
        var columnType = newMetadataColumn[Constants.Baas.Tables.Columns.TYPE];
        newMetadataColumn[Constants.AppsStudio.ViewColumns.WIDTH] = newMetadataColumn[Constants.AppsStudio.ViewColumns.WIDTH] || 200;
        if (columnVisibility) {
            if (columnVisibility instanceof Object) {
                columnVisibility = columnVisibility._val;
            }
            newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] = columnVisibility;
        } else {
//            (dataQuery[QueryConstants.Query.FILTER] && dataQuery[QueryConstants.Query.FILTER][newColumnExpression]) ||
            if (newMetadataColumn[Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN] || columnType == Constants.Baas.Tables.Columns.Type.OBJECT) {
                newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] = "None";
            } else if ((columnMultiple && columnType == Constants.Baas.Tables.Columns.Type.LOOKUP) || columnType == Constants.Baas.Tables.Columns.Type.TEXT || columnType == Constants.Baas.Tables.Columns.Type.RICHTEXT) {
                newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] = "Panel";
            } else {
                newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] = "Both";
            }
        }
        if (columnType == Constants.Baas.Tables.Columns.Type.DATE || columnType == Constants.Baas.Tables.Columns.Type.DATETIME) {
            if (!newMetadataColumn[Constants.AppsStudio.ViewColumns.FORMAT]) {
                newMetadataColumn[Constants.AppsStudio.ViewColumns.FORMAT] = "DD/MM/YYYY";
            }
        }
        if (newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] == "Table" || newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] == "Query" || newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] == "Both") {
            if (!newMetadataColumn.subvisibility) {
                queryColumns.push(newMetadataColumn);
            }
        }
        if (newMetadataColumn[Constants.AppsStudio.ViewColumns.VISIBILITY] == "Off") {
            var developer = newMetadataColumn.developer;
            var admin = newMetadataColumn.admin;
            delete newMetadataColumn.developer;
            delete newMetadataColumn.admin;
            if ((applicationDeveloper && developer) || (organizationAdmin && admin) || (!developer && !admin)) {
                newMetadataColumnsArray.push(newMetadataColumn);
            }
        } else {
            newMetadataColumnsArray.push(newMetadataColumn);
        }

        if (newMetadataColumn[Constants.Baas.Tables.Columns.FLEXIBLE] && newMetadataColumn[Constants.Baas.Tables.Columns.TABLE]) {
            var flexibleTableName = newMetadataColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
            getFlexibleColumns(flexibleTableName, parameters, ApplaneCallback(callback, function (flexibleColumms) {
                if (flexibleColumms && flexibleColumms.length > 0) {
                    populateFlexColumns(flexibleColumms, newMetadataColumn);
                    callback();
                } else {
                    callback();
                }
            }));
        } else {
            callback();
        }
    })
}

function mergeInnerColumns(columns, columnsWithPExpression) {
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        var columnExp = column[Constants.Baas.Tables.Columns.EXPRESSION];
        var columnType = column[Constants.Baas.Tables.Columns.TYPE];
        var columnVisibility = column[Constants.AppsStudio.ViewColumns.VISIBILITY];
        var innerColumns = column[Constants.Baas.Tables.COLUMNS];
        if (innerColumns && innerColumns.length > 0) {
            innerColumns = [];
            for (var j = 0; j < columnsWithPExpression.length; j++) {
                var innerColumn = JSON.parse(JSON.stringify(columnsWithPExpression[j]));
                var innerColumnPExpression = innerColumn.pexpression
                if (innerColumnPExpression && innerColumnPExpression == columnExp) {
                    delete innerColumn.subvisibility;
                    var innerColumnExpression = innerColumn[Constants.Baas.Tables.Columns.EXPRESSION];
                    var indexOF = innerColumnExpression.indexOf(columnExp + ".");
                    innerColumn[Constants.Baas.Tables.Columns.EXPRESSION] = innerColumnExpression.substr(innerColumnExpression.indexOf(columnExp + ".") + columnExp.length + 1);
                    innerColumns.push(innerColumn);
                }
            }
//            if (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP) {
//                innerColumns = addStringColumnOnFirst(innerColumns);
//            }
            column[Constants.Baas.Tables.COLUMNS] = innerColumns;
        }
    }
}

//function addStringColumnOnFirst(innerColumns) {
//    var newColumns = [];
//    var lastIndex;
//    var hasStringColumn = false;
//    for (var i = 0; i < innerColumns.length; i++) {
//        var innerColumn = innerColumns[i];
//        var innerColumnType = innerColumn[Constants.Baas.Tables.Columns.TYPE];
//        if (innerColumnType == Constants.Baas.Tables.Columns.Type.STRING) {
//            newColumns[0] = innerColumn;
//            lastIndex = i;
//            hasStringColumn = true;
//            break;
//        } else {
//            newColumns[i + 1] = innerColumn;
//        }
//    }
//    if (!hasStringColumn) {
//        return innerColumns;
//    } else {
//        for (var i = lastIndex + 1; i < innerColumns.length; i++) {
//            newColumns[i] = innerColumns[i];
//        }
//        return newColumns;
//    }
//}


function parseLabel(expression) {
    expression = expression.replace(/\./gi, " ");
    expression = expression.replace(/_id/gi, " ");
    expression = expression.replace(/__/gi, " ");
    expression = expression.replace(/_/gi, " ");
    var split = expression.split(" ");
    var newLabel = "";
    for (var i = 0; i < split.length; i++) {
        var word = split[i];
        word = word.substr(0, 1).toUpperCase() + word.substr(1);
        if (newLabel.length > 0) {
            newLabel = newLabel + " ";
        }
        newLabel = newLabel + word;
    }
    return newLabel;
}

function getFlexibleColumns(flexibleTableName, parameters, callback) {
    var filter = {};
    filter[Constants.Baas.Flexfields.TABLE] = "{flexibleTableName__}";
    filter[Constants.Baas.Flexfields.ORGANIZATION] = "{orgid__}";

    var viewQuery = {};
    viewQuery[QueryConstants.Query.TABLE] = Constants.Baas.FLEXFIELDS;
    viewQuery[QueryConstants.Query.FILTER] = filter;
    viewQuery[QueryConstants.Query.PARAMETERS] = {"flexibleTableName__":flexibleTableName, "orgid__":parameters.organizationid};

    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        logid:parameters[RequestConstants.AppsStudio.LOGID],
        query:viewQuery,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[QueryConstants.Query.Result.DATA];
            callback(null, data);
        })
    })
}

function getViewData(viewId, parameters, viewInfo, dataQuery, metadata, callback) {
    var startTime = new Date().getTime();
    dataQuery[QueryConstants.Query.FILTER] = dataQuery[QueryConstants.Query.FILTER] || {};
    var dataQueryFilters = dataQuery[QueryConstants.Query.FILTER];
    var dataQueryFiltersClone = JSON.parse(JSON.stringify(dataQueryFilters));
    delete  metadata.columnexpressionsarray;
//    var columnExpressionsArray = metadata.columnexpressionsarray;
//    for (var filterExp in dataQueryFiltersClone) {
//        if (columnExpressionsArray.indexOf(filterExp) == -1) {
//            delete dataQueryFilters[filterExp];
//        }
//    }

    metadata[RequestConstants.AppsStudio.Metadata.FILTER] = dataQueryFilters;
    metadata[RequestConstants.AppsStudio.Metadata.PARAMETERS] = dataQuery[QueryConstants.Query.PARAMETERS];
    addIndexColumnInOrders(dataQuery);
    metadata[RequestConstants.AppsStudio.Metadata.ORDERS] = dataQuery[QueryConstants.Query.ORDERS];
    var view = {};
    view.ask = parameters[RequestConstants.AppsStudio.ASK];
    view.osk = parameters[RequestConstants.AppsStudio.OSK];
    view.usk = parameters[RequestConstants.AppsStudio.USK];
    view[RequestConstants.AppsStudio.VIEWID] = viewId;
    view[QueryConstants.Query.Result.METADATA] = metadata;

    if (viewInfo.keepstructure) {
        dataQuery.keepstructure = true;
    }
    startTime = new Date().getTime();
    DatabaseEngine.query({
        ask:parameters[RequestConstants.AppsStudio.ASK],
        osk:parameters[RequestConstants.AppsStudio.OSK],
        usk:parameters[RequestConstants.AppsStudio.USK],
        user:parameters[RequestConstants.AppsStudio.USER],
        logid:parameters[RequestConstants.AppsStudio.LOGID],
        query:dataQuery,
        callback:function (err, result) {
            if (err) {
                view.dataexception = err.stack;
                view[QueryConstants.Query.Result.DATA] = {data:[]};
                view.logtime = parameters.logtime;
                callback(null, view);
            } else {
                parameters.logtime.timeingetData = new Date().getTime() - startTime;
                view[QueryConstants.Query.Result.DATA] = result;
                startTime = new Date().getTime();
                updateApplicationWiseUserState(viewId, viewInfo, parameters, ApplaneCallback(callback, function () {
                    parameters.logtime.timeinupdateApplicationWiseUserState = new Date().getTime() - startTime;
                    view.logtime = parameters.logtime;
                    callback(null, view);
                }))
            }
        }
    })
}

function addIndexColumnInOrders(dataQuery) {
    var dataQueryColumns = dataQuery[QueryConstants.Query.COLUMNS];
    for (var i = 0; i < dataQueryColumns.length; i++) {
        var column = dataQueryColumns[i];
        var columnType = column[Constants.Baas.Tables.Columns.TYPE];
        var columnExpression = column[Constants.Baas.Tables.Columns.EXPRESSION];
        if (columnType == Constants.Baas.Tables.Columns.Type.INDEX) {
            var dataQueryOrders = dataQuery[QueryConstants.Query.ORDERS];
            if (!dataQueryOrders) {
                dataQueryOrders = [];
            } else if (dataQueryOrders instanceof Object && !(dataQueryOrders instanceof Array)) {
                dataQueryOrders = [dataQueryOrders];
            }
            var newOrder = {};
            newOrder[columnExpression] = {$order:"asc"};
            dataQueryOrders.unshift(newOrder);
            dataQuery[QueryConstants.Query.ORDERS] = dataQueryOrders;
            break;
        }
    }
}

function updateUserState(userState, organization, viewGroup, menuLabel, viewId, parameters, user, callback) {
//    callback();
//    return;
    userState[Constants.Baas.Users.UserState.APPLICATION_WISE_STATE] = userState[Constants.Baas.Users.UserState.APPLICATION_WISE_STATE] || [];
    var applicationWiseStates = userState[Constants.Baas.Users.UserState.APPLICATION_WISE_STATE];
    var updateMenu = false;
    if (applicationWiseStates.length > 0) {
        for (var i = 0; i < applicationWiseStates.length; i++) {
            var applicationWiseState = applicationWiseStates[i];
            if (applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.ORGANIZATION] == organization && applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.APPLICATION] == viewGroup) {
                applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.MENU] = menuLabel;
                applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.QUICK_VIEW] = viewId;
                updateMenu = true;
                break;
            }
        }
    }
    if (!updateMenu) {
        var applicationWiseState = {};
        applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.ORGANIZATION] = organization;
        applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.APPLICATION] = viewGroup;
        applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.MENU] = menuLabel;
        applicationWiseState[Constants.Baas.Users.UserState.ApplicationWiseState.QUICK_VIEW] = viewId;
        applicationWiseStates.push(applicationWiseState)
    }

    var updates = {};
    updates[QueryConstants.Query.TABLE] = Constants.Baas.USERS;
    var operation = {};
    operation[QueryConstants.Query._ID] = user[QueryConstants.Query._ID];
    operation.userstate = userState;
    updates[QueryConstants.Update.OPERATIONS] = operation;
    updates.excludejobs = true;
    updates.excludemodules = true;
    UpdateEngine.performUpdate({
        ask:Constants.Baas.ASK,
        usk:parameters.usk,
        logid:parameters.logid,
        updates:updates,
        callback:callback
    });
}

function updateApplicationWiseUserState(viewId, viewInfo, parameters, callback) {
    var menuLabel = viewInfo[RequestConstants.AppsStudio.ViewInfo.MENU_LABEL];
    var viewGroup = viewInfo[RequestConstants.AppsStudio.ViewInfo.VIEW_GROUP];
    var organization = parameters.organization;
    var user = parameters.user;
    if (!menuLabel || !viewGroup || !organization) {
        callback();
    } else {
        var userState = viewInfo[RequestConstants.AppsStudio.ViewInfo.USER_STATE];
        if (!userState) {
            getUserStates(user[QueryConstants.Query._ID], ApplaneCallback(callback, function (userStateResult) {
                userState = userStateResult;
                updateUserState(userState, organization, viewGroup, menuLabel, viewId, parameters, user, callback);
            }));
        } else {
            updateUserState(userState, organization, viewGroup, menuLabel, viewId, parameters, user, callback);
        }
    }
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

function isParameter(value) {
    var pattern = new RegExp("^(_).+(_)$");
    return pattern.test(value);
}


