/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 3/8/13
 * Time: 4:49 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require("../../shared/Constants.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var AppsStudioError = require("apputil/ApplaneError.js");
var QueryConstants = require("nodejsapi/Constants.js");
var RequestConstants = require("../../shared/RequestConstants.js");
var ViewService = require("./ViewService.js");
var ViewGroupMenusJob = require("./ViewGroupMenusJob.js");
var Utils = require("apputil/util.js");
var DatabaseEngine = require("../../database/DatabaseEngine.js");
var UpdateEngine = require("../../database/UpdateEngine.js");
var MetadataProvider = require('../../metadata/MetadataProvider.js');


exports.saveViewCustomization = function (parameters, callback) {
    var viewId = parameters[RequestConstants.AppsStudio.VIEWID];
    if (!viewId) {
        throw new AppsStudioError(Constants.ErrorCode.FIELDS_BLANK, ["viewid"]);
    }
    var osk = parameters[RequestConstants.AppsStudio.OSK];
    var user = parameters[RequestConstants.AppsStudio.USER];
    var logid = parameters[RequestConstants.AppsStudio.LOGID];
    MetadataProvider.getOrganization(osk, ApplaneCallback(callback, function (organizationDetail) {
        var organizationId = organizationDetail ? organizationDetail[QueryConstants.Query._ID] : organizationDetail;
        ViewService.getViewDetails(viewId, user[QueryConstants.Query._ID], organizationId, logid, ApplaneCallback(callback, function (viewData) {
            if (!viewData || viewData.length == 0) {
                throw new AppsStudioError(Constants.ErrorCode.VIEW_NOT_FOUND, [viewId]);
            }
            var viewDetails = viewData[0];
            var savedCustomization = viewDetails[Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(viewDetails[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
            viewDetails[Constants.AppsStudio.Views.CUSTOMIZATION] = savedCustomization;
            var baasViewDetails = viewDetails[Constants.AppsStudio.Views.BAAS_VIEWID];

            var customization = parameters[RequestConstants.AppsStudio.CUSTOMIZATION] ? JSON.parse(parameters[RequestConstants.AppsStudio.CUSTOMIZATION]) : {};
            var childs = customization[RequestConstants.AppsStudio.Customization.CHILDS];
            parameters[RequestConstants.AppsStudio.CUSTOMIZATION] = customization;

            var updates = {};
            ViewService.getUserOrganizationViewDetails(viewId, user[QueryConstants.Query._ID], organizationId, logid, ApplaneCallback(callback, function (userOrganizationViewDetails) {
                var viewKey;
                if (userOrganizationViewDetails && userOrganizationViewDetails.length > 0) {
                    var userOrganizationViewDetail = userOrganizationViewDetails[0];
                    viewKey = userOrganizationViewDetail[QueryConstants.Query._ID];
                    savedCustomization = userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
                    baasViewDetails = userOrganizationViewDetail[Constants.AppsStudio.Views.BAAS_VIEWID];
                } else {
                    updates = JSON.parse(JSON.stringify(viewDetails));
                    delete updates[QueryConstants.Query._ID];
                    updates[Constants.AppsStudio.Views.USERID] = user[QueryConstants.Query._ID];
                    updates[Constants.AppsStudio.Views.ORGANIZATIONID] = organizationId;
                }
                if (!childs) {
                    populateColumnsCustomization(parameters, savedCustomization, baasViewDetails, viewKey, updates, callback);
                } else {
                    populateChilds(parameters, childs, savedCustomization, baasViewDetails, viewKey, updates, callback);
                }
            }));
        }));
    }));
}

function populateChilds(parameters, childs, savedCustomization, baasViewDetails, viewKey, updates, callback) {
    var logid = parameters[RequestConstants.AppsStudio.LOGID];
    if (childs && childs.length > 0) {
        var newChilds = [];
        Utils.iterateArray(childs, ApplaneCallback(callback, function () {
            savedCustomization[Constants.AppsStudio.Views.Customization.CHILDS] = newChilds;
            updates[Constants.AppsStudio.Views.CUSTOMIZATION] = JSON.stringify(savedCustomization);

            var updatesObj = {};
            updatesObj[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
            updates[QueryConstants.Query._ID] = viewKey;
            updatesObj[QueryConstants.Update.OPERATIONS] = updates;
            UpdateEngine.performUpdate({
                ask:Constants.AppsStudio.ASK,
                logid:logid,
                updates:updatesObj,
                callback:ApplaneCallback(callback, function (customizationUpdateResult) {
                    getView(parameters, callback);
                })
            });
        }), function (child, callback) {
            var childVisibility = child[Constants.AppsStudio.ViewColumns.VISIBILITY];
            if (childVisibility instanceof Object) {
                childVisibility = childVisibility._val;
                child[Constants.AppsStudio.ViewColumns.VISIBILITY] = childVisibility
            }
            child[Constants.AppsStudio.ViewColumns.ID] = child[Constants.AppsStudio.ViewColumns.ID] || child[Constants.AppsStudio.ViewColumns.LABEL];
            var childParameterMappings = child[Constants.AppsStudio.ViewColumns.PARAMETER_MAPPINGS];
            if (typeof childParameterMappings == "string") {
                child[Constants.AppsStudio.ViewColumns.PARAMETER_MAPPINGS] = JSON.parse(childParameterMappings);
            }
            var childFilter = child[Constants.AppsStudio.ViewColumns.FILTER];
            if (typeof childFilter == "string") {
                console.log(childFilter.trim().length);
                if (childFilter.trim().length == 0) {
                    delete child[Constants.AppsStudio.ViewColumns.FILTER];
                } else {
                    child[Constants.AppsStudio.ViewColumns.FILTER] = JSON.parse(childFilter);
                }
            }
            if (childVisibility && childVisibility == "None") {
                callback();
            } else {
                var newChildObj = JSON.parse(JSON.stringify(child));
                if (newChildObj.viewid) {
                    newChilds.push(newChildObj);
                    callback();
                } else {
                    parameters.baasViewDetails = baasViewDetails;
                    var childTable = child[Constants.AppsStudio.ViewColumns.TABLE];
                    var childLabel = child[Constants.AppsStudio.ViewColumns.LABEL];
                    var childTableName = childTable[Constants.Baas.Tables.ID];
                    getTableColumns(childTableName, logid, ApplaneCallback(callback, function (tableResultData) {
                        var tableColumns = tableResultData && tableResultData.length > 0 ? tableResultData[0][Constants.Baas.Tables.COLUMNS] : [];
                        var relatedColumn = getRelatedColumn(tableColumns, parameters);
                        if (relatedColumn) {
                            newChildObj[Constants.AppsStudio.ViewColumns.RELATED_COLUMN] = relatedColumn;
                            ViewGroupMenusJob.populateBaasViews(childTable, childLabel, logid, ApplaneCallback(callback, function (view) {
                                if (view[Constants.Baas.Views.ID]) {
                                    newChildObj.viewid = view[Constants.Baas.Views.ID];
                                    newChilds.push(newChildObj);
                                    var childViewCustomization = view[Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(view[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
                                    var childViewColumns = childViewCustomization[Constants.AppsStudio.Views.Customization.COLUMNS] || {};
                                    var childViewSequences = childViewCustomization[Constants.AppsStudio.Views.Customization.SEQUENCE] || [];
                                    Utils.iterateArray(tableColumns, ApplaneCallback(callback, function () {
                                        childViewCustomization[Constants.AppsStudio.Views.Customization.COLUMNS] = childViewColumns;
                                        childViewCustomization[Constants.AppsStudio.Views.Customization.SEQUENCE] = childViewSequences;
                                        var updates = {};
                                        updates[Constants.AppsStudio.Views.CUSTOMIZATION] = JSON.stringify(childViewCustomization);

                                        var updatesObj = {};
                                        updatesObj[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
                                        updates[QueryConstants.Query._ID] = view[QueryConstants.Query._ID];
                                        updatesObj[QueryConstants.Update.OPERATIONS] = updates;
                                        UpdateEngine.performUpdate({
                                            ask:Constants.AppsStudio.ASK,
                                            logid:logid,
                                            updates:updatesObj,
                                            callback:callback
                                        });
                                    }), function (tableColumn, callback) {
                                        if (tableColumn[Constants.Baas.Tables.Columns.TABLE] && tableColumn[Constants.Baas.Tables.Columns.EXPRESSION].indexOf(".") == -1) {
                                            var lookupTable = tableColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
                                            getTableColumns(lookupTable, logid, ApplaneCallback(callback, function (lookupTableResult) {
                                                addFilterInLookupColumns(lookupTableResult, parameters, tableColumn, childViewSequences, childViewColumns, false);
                                                callback();
                                            }));
                                        } else {
                                            callback();
                                        }
                                    });
                                } else {
                                    callback();
                                }
                            }));
                        } else {
                            callback();
                        }
                    }))
                }
            }
        });
    }
}

function populateColumnsCustomization(parameters, savedCustomization, baasViewDetails, viewKey, updates, callback) {
    var customization = parameters[RequestConstants.AppsStudio.CUSTOMIZATION];
    var customizationColumns = customization [RequestConstants.AppsStudio.Customization.COLUMNS];
    var appliedFilters = customization[RequestConstants.AppsStudio.Customization.APPLIED_FILTERS];
    var orders = customization[RequestConstants.AppsStudio.Customization.ORDERS];
    var schedules = customization[RequestConstants.AppsStudio.Customization.SCHEDULES];
    if (customizationColumns && customizationColumns.length > 0) {
        var savedColumns = savedCustomization[Constants.AppsStudio.Views.Customization.COLUMNS];
        var savedColumnSequences = savedCustomization[Constants.AppsStudio.Views.Customization.SEQUENCE];
        var columns = savedColumns ? JSON.parse(JSON.stringify(savedColumns)) : {};
        var sequences = [];
        Utils.iterateArray(customizationColumns, ApplaneCallback(callback, function () {
            for (var expression in savedColumns) {
                if (sequences.indexOf(expression) == -1) {
                    sequences.push(expression);
                    var columnVisibility = savedColumns[expression] [RequestConstants.AppsStudio.Customization.Columns.VISIBILITY];
                    if (columnVisibility instanceof Object) {
                        savedColumns[expression] [RequestConstants.AppsStudio.Customization.Columns.VISIBILITY] = columnVisibility._val;
                    }
                }
            }
            savedCustomization[Constants.AppsStudio.Views.Customization.COLUMNS] = columns;
            savedCustomization[Constants.AppsStudio.Views.Customization.SEQUENCE] = sequences;
            updateCustomization(appliedFilters, orders, schedules, savedCustomization, updates, viewKey, parameters, callback);
        }), function (customizationColumn, callback) {
            var columnExpression = customizationColumn[RequestConstants.AppsStudio.Customization.Columns.EXPRESSION];
            if (sequences.indexOf(columnExpression) == -1) {
                sequences.push(columnExpression);
            }
            var columnProperties = populateCustomizationProperties(customizationColumn);
            columns[columnExpression] = columnProperties;

            var columnVisibility = customizationColumn[RequestConstants.AppsStudio.Customization.Columns.VISIBILITY];
            if (columnVisibility && columnVisibility instanceof Object) {
                columnVisibility = columnVisibility._val;
            }
            var baasTableId = baasViewDetails[Constants.Baas.Views.TABLE][Constants.Baas.Tables.ID];
            if (!savedCustomization[Constants.AppsStudio.Views.Customization.MAIN_TABLE_ID]) {
                savedCustomization[Constants.AppsStudio.Views.Customization.MAIN_TABLE_ID] = baasTableId;
            }
            if (columnVisibility && columnVisibility == "Child") {
                parameters.baasViewDetails = baasViewDetails;
                addColumnChildView(baasTableId, columnExpression, null, null, null, columnProperties, parameters, callback);
            } else {
                callback();
            }
        });
    } else {
        updateCustomization(appliedFilters, orders, schedules, savedCustomization, updates, viewKey, parameters, callback);
    }
}

function updateCustomization(appliedFilters, orders, schedules, savedCustomization, updates, viewKey, parameters, callback) {
    if (appliedFilters) {
        savedCustomization[Constants.AppsStudio.Views.Customization.APPLIED_FILTERS] = appliedFilters;
    }
    if (orders) {
        savedCustomization[Constants.AppsStudio.Views.Customization.ORDERS] = orders;
    }
    if (schedules && schedules.length > 0) {
        for (var i = 0; i < schedules.length; i++) {
            var schedule = schedules[i];
            if (schedule[QueryConstants.Query._ID] && schedule[QueryConstants.Query._ID].indexOf("temp") != -1) {
                delete schedule[QueryConstants.Query._ID];
            }
            if (schedule[Constants.AppsStudio.Views.Schedules.SCHEDULEID]) {
                var scheduleObject = schedule[Constants.AppsStudio.Views.Schedules.SCHEDULEID];
                if (!scheduleObject[Constants.AppsStudio.Schedules.VIEWID]) {
                    scheduleObject[Constants.AppsStudio.Schedules.VIEWID] = parameters[RequestConstants.AppsStudio.VIEWID];
                }
            }
            if (schedule[Constants.AppsStudio.Views.Schedules.VISIBILITY] == Constants.AppsStudio.Views.Schedules.Visibility.NONE) {
                schedule[QueryConstants.Update.Operation.TYPE] = QueryConstants.Update.Operation.Type.DELETE;
            }
        }
        updates[Constants.AppsStudio.Views.SCHEDULES] = schedules;
    }
    updates[Constants.AppsStudio.Views.CUSTOMIZATION] = JSON.stringify(savedCustomization);
    var updatesObj = {};
    updatesObj[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
    updates[QueryConstants.Query._ID] = viewKey;
    updatesObj[QueryConstants.Update.OPERATIONS] = updates;
    UpdateEngine.performUpdate({
        ask:Constants.AppsStudio.ASK,
        logid:parameters.logid,
        updates:updatesObj,
        callback:ApplaneCallback(callback, function (customizationUpdateResult) {
            getView(parameters, callback)
        })
    });
}

function populateCustomizationProperties(column) {
    var columnProperties = {};
    columnProperties[Constants.AppsStudio.ViewColumns.WIDTH] = column[Constants.AppsStudio.ViewColumns.WIDTH] || 200;
    columnProperties[Constants.AppsStudio.ViewColumns.LABEL] = column[Constants.AppsStudio.ViewColumns.LABEL] || column[Constants.AppsStudio.ViewColumns.EXPRESSION];
    columnProperties[Constants.AppsStudio.ViewColumns.ID] = column[Constants.AppsStudio.ViewColumns.ID];
    var visibility = column[Constants.AppsStudio.ViewColumns.VISIBILITY];
    if (visibility instanceof Object) {
        visibility = visibility._val;
    }
    columnProperties[Constants.AppsStudio.ViewColumns.VISIBILITY] = visibility;
    columnProperties[Constants.AppsStudio.ViewColumns.MULTIPLE] = column[Constants.AppsStudio.ViewColumns.MULTIPLE];
    columnProperties[Constants.AppsStudio.ViewColumns.UPDATE] = column[Constants.AppsStudio.ViewColumns.UPDATE];
    columnProperties[Constants.AppsStudio.ViewColumns.TOTAL_AGGREGATE] = column[Constants.AppsStudio.ViewColumns.TOTAL_AGGREGATE];
    columnProperties[Constants.AppsStudio.ViewColumns.VISIBLE_EXPRESSION] = column[Constants.AppsStudio.ViewColumns.VISIBLE_EXPRESSION];
    var filter = column[Constants.AppsStudio.ViewColumns.FILTER];
    if (filter && typeof filter == "string") {
        filter = JSON.parse(filter);
        columnProperties[Constants.AppsStudio.ViewColumns.FILTER] = filter;
    }
    columnProperties[Constants.AppsStudio.ViewColumns.VIEWID] = column[Constants.AppsStudio.ViewColumns.VIEWID];
    return columnProperties;
}

function addColumnChildView(lookupTable, columnExpression, parentTable, parentExpression, parentColDef, columnProperties, parameters, callback) {
    var logid = parameters[RequestConstants.AppsStudio.LOGID];
    getLookupTableData(lookupTable, parentTable, logid, ApplaneCallback(callback, function (data) {
        var columns = data[Constants.Baas.Tables.COLUMNS];
        var indexOfDot = columnExpression.indexOf(".");
        if (indexOfDot > 0) {
            var preColumnExpression = columnExpression.substr(0, indexOfDot);
            var postColumnExpression = columnExpression.substr(indexOfDot + 1);
            if (parentColDef) {
                var parentColumns = parentColDef[Constants.Baas.Tables.COLUMNS];
                if (parentColumns && typeof parentColumns == 'string') {
                    if (parentColumns.trim().length > 0) {
                        parentColumns = JSON.parse(parentColumns);
                    } else {
                        parentColumns = [];
                    }
                }
                for (var i = 0; i < parentColumns.length; i++) {
                    var parentColumn = parentColumns[i];
                    if (parentColumn[Constants.Baas.Tables.Columns.EXPRESSION] == preColumnExpression) {
                        var parentColumnType = parentColumn[Constants.Baas.Tables.Columns.TYPE];
                        if (parentExpression) {
                            parentExpression = parentExpression + "." + preColumnExpression;
                        }
                        if (parentColumnType == Constants.Baas.Tables.Columns.Type.LOOKUP) {
                            var tableName = parentColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
                            addColumnChildView(tableName, postColumnExpression, parentTable ? parentTable : data, parentExpression, parentColumn, columnProperties, parameters, callback);
                        } else if (parentColumnType == Constants.Baas.Tables.Columns.Type.OBJECT) {
                            addColumnChildView(null, postColumnExpression, parentTable ? parentTable : data, parentExpression, parentColumn, columnProperties, parameters, callback);
                        }
                        return;
                    }
                }
            }
            for (var i = 0; i < columns.length; i++) {
                var column = columns[i];
                var columnType = column[Constants.Baas.Tables.Columns.TYPE];
                var reverseRelationColumn = column[Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN];
                if (column[Constants.Baas.Tables.Columns.EXPRESSION] == preColumnExpression) {
                    if (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP && reverseRelationColumn) {
                        var tableName = column[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
                        addColumnChildView(tableName, postColumnExpression, null, null, null, columnProperties, parameters, callback);
                    } else {
                        if (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP) {
                            var tableName = column[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
                            if (parentExpression) {
                                parentExpression = parentExpression + "." + preColumnExpression;
                            }
                            addColumnChildView(tableName, postColumnExpression, data, preColumnExpression, column, columnProperties, parameters, callback);
                        } else if (columnType == Constants.Baas.Tables.Columns.Type.OBJECT) {
                            addColumnChildView(null, postColumnExpression, data, preColumnExpression, column, columnProperties, parameters, callback);
                        }
                    }
                    return;
                }
            }
        }

        if (parentColDef) {
            var parentColumns = parentColDef[Constants.Baas.Tables.COLUMNS];
            if (parentColumns && typeof parentColumns == 'string') {
                if (parentColumns.trim().length > 0) {
                    parentColumns = JSON.parse(parentColumns);
                } else {
                    parentColumns = [];
                }
            }
            for (var i = 0; i < parentColumns.length; i++) {
                var parentColumn = parentColumns[i];
                if (parentColumn[Constants.Baas.Tables.Columns.EXPRESSION] == columnExpression) {
                    if (parentExpression) {
                        columnExpression = parentExpression + "." + columnExpression;
                    }
                    if (parentTable) {
                        lookupTable = parentTable[Constants.Baas.Tables.ID];
                        columns = parentTable[Constants.Baas.Tables.COLUMNS];
                    }
                    break;
                }
            }
        }
        var childViewId = columnExpression + "__" + lookupTable;
        columnProperties[Constants.AppsStudio.ViewColumns.VIEWID] = childViewId;
        columnProperties[Constants.AppsStudio.ViewColumns.CHILD_COLUMN] = columnExpression;
        ViewService.getViewDetails(childViewId, null, null, logid, ApplaneCallback(callback, function (viewData) {
            if (!viewData || viewData.length == 0) {
                var childViewCustomization = {};
                var childColumns = {};
                var childSequences = [];
                childViewCustomization[Constants.AppsStudio.Views.Customization.COLUMNS] = childColumns;
                childViewCustomization[Constants.AppsStudio.Views.Customization.SEQUENCE] = childSequences;

                var childColumn;
                for (var i = 0; i < columns.length; i++) {
                    var column = columns[i];
                    if (column[Constants.Baas.Tables.Columns.EXPRESSION] == columnExpression) {
                        childColumn = column;
                    }
                }

                if (!childColumn) {
                    callback();
                    return;
                }

                var type = childColumn[Constants.Baas.Tables.Columns.TYPE];
                var multiple = childColumn[Constants.Baas.Tables.Columns.MULTIPLE];
                if ((multiple == undefined || !multiple) && (type != Constants.Baas.Tables.Columns.Type.LOOKUP && type != Constants.Baas.Tables.Columns.Type.OBJECT)) {
                    throw new AppsStudioError("Visibility Child should be defined for lookup or object type column.");
                }

                if (type == Constants.Baas.Tables.Columns.Type.LOOKUP) {
                    childViewCustomization[Constants.AppsStudio.Views.Customization.MAIN_TABLE_ID] = childColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
                } else {
                    childViewCustomization[Constants.AppsStudio.Views.Customization.MAIN_TABLE_ID] = lookupTable;
                }

                getBaasView(lookupTable, logid, ApplaneCallback(callback, function (baasViewId) {
                    var baasViewObject = {};
                    baasViewObject[QueryConstants.Query._ID] = baasViewId;
                    var viewOperation = {};
                    viewOperation[Constants.AppsStudio.Views.ID] = childViewId;
                    viewOperation[Constants.AppsStudio.Views.LABEL] = childViewId;
                    viewOperation[Constants.AppsStudio.Views.BAAS_VIEWID] = baasViewObject;
                    viewOperation[Constants.AppsStudio.Views.QUICK_VIEWID] = childViewId;
                    if (childColumn[Constants.Baas.Tables.Columns.TABLE]) {
                        var lookupTable = childColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
                        getTableColumns(lookupTable, logid, ApplaneCallback(callback, function (lookupTableResult) {
                            addFilterInLookupColumns(lookupTableResult, parameters, childColumn, childSequences, childColumns, true);
                            viewOperation[Constants.AppsStudio.Views.CUSTOMIZATION] = JSON.stringify(childViewCustomization);
                            var updatesObj = {};
                            updatesObj[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
                            updatesObj[QueryConstants.Update.OPERATIONS] = viewOperation;
                            UpdateEngine.performUpdate({
                                ask:Constants.AppsStudio.ASK,
                                logid:logid,
                                updates:updatesObj,
                                callback:callback
                            });
                        }));
                    } else {
                        viewOperation[Constants.AppsStudio.Views.CUSTOMIZATION] = JSON.stringify(childViewCustomization);
                        var updatesObj = {};
                        updatesObj[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
                        updatesObj[QueryConstants.Update.OPERATIONS] = viewOperation;
                        UpdateEngine.performUpdate({
                            ask:Constants.AppsStudio.ASK,
                            logid:logid,
                            updates:updatesObj,
                            callback:callback
                        });
                    }
                }));
            } else {
                callback();
            }
        }));
    }));
}

function getLookupTableData(lookupTable, parentTable, logid, callback) {
    if (!lookupTable) {
        callback(null, parentTable);
    } else {
        var filter = {};
        filter[Constants.Baas.Tables.ID] = "{id}";

        var viewQuery = {};
        viewQuery[QueryConstants.Query.TABLE] = Constants.Baas.TABLES;
        viewQuery[QueryConstants.Query.COLUMNS] = [Constants.Baas.Tables.COLUMNS, Constants.Baas.Tables.ID];
        viewQuery[QueryConstants.Query.FILTER] = filter;
        viewQuery[QueryConstants.Query.PARAMETERS] = {"id":lookupTable};

        DatabaseEngine.query({
            ask:Constants.Baas.ASK,
            logid:logid,
            query:viewQuery,
            callback:ApplaneCallback(callback, function (result) {
                var data = result[QueryConstants.Query.Result.DATA][0];
                callback(null, data);
            })
        })
    }
}

function getBaasView(lookupTable, logid, callback) {
    var filter = {};
    filter[Constants.Baas.Views.TABLE + "." + Constants.Baas.Tables.ID] = "{id}";

    var viewQuery = {};
    viewQuery[QueryConstants.Query.TABLE] = Constants.Baas.VIEWS;
    viewQuery[QueryConstants.Query.COLUMNS] = [QueryConstants.Query._ID];
    viewQuery[QueryConstants.Query.FILTER] = filter;
    viewQuery[QueryConstants.Query.PARAMETERS] = {"id":lookupTable};

    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        logid:logid,
        query:viewQuery,
        callback:ApplaneCallback(callback, function (result) {
            var baasViewId = result[QueryConstants.Query.Result.DATA][0][QueryConstants.Query._ID];
            callback(null, baasViewId);
        })
    })
}

function addFilterInLookupColumns(lookupTableResult, parameters, tableColumn, childViewSequences, childViewColumns, addMultipleFalse) {
    var lookupTableColumns = lookupTableResult && lookupTableResult.length > 0 ? lookupTableResult[0][Constants.Baas.Tables.COLUMNS] : [];
    var columnExp = null;
    for (var i = 0; i < lookupTableColumns.length; i++) {
        var lookupTableColumn = lookupTableColumns[i];
        if (!lookupTableColumn[Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN] && lookupTableColumn[Constants.Baas.Tables.Columns.TABLE]) {
            if (lookupTableColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID] == parameters.baasViewDetails[Constants.Baas.Views.TABLE][Constants.Baas.Tables.ID]) {
                columnExp = lookupTableColumn[Constants.Baas.Tables.Columns.EXPRESSION];
                break;
            }
        }
    }
    if (columnExp) {
        var tableColumnExpression = tableColumn[Constants.Baas.Tables.Columns.EXPRESSION];
        if (childViewSequences.indexOf(tableColumnExpression) == -1) {
            childViewSequences.push(tableColumnExpression);
        }
        var tableColumnCustomization = childViewColumns[tableColumnExpression] ? childViewColumns[tableColumnExpression] : populateCustomizationProperties(tableColumn);
        tableColumnCustomization[Constants.Baas.Tables.Columns.MULTIPLE] = addMultipleFalse ? false : tableColumn[Constants.Baas.Tables.Columns.MULTIPLE];

        if (tableColumnCustomization[QueryConstants.Query.FILTER]) {
            tableColumnCustomization[QueryConstants.Query.FILTER][columnExp] = "{" + parameters[RequestConstants.AppsStudio.CUSTOMIZATION].relatedcolumnparamname + "}";
        } else {
            var columnFilter = {};
            columnFilter[columnExp] = "{_id}";
            tableColumnCustomization[QueryConstants.Query.FILTER] = columnFilter;
        }
        childViewColumns[tableColumnExpression] = tableColumnCustomization;
    }
}


function getRelatedColumn(tableColumns, parameters) {
    for (var i = 0; i < tableColumns.length; i++) {
        var tableColumn = tableColumns[i];
        if (tableColumn[Constants.Baas.Tables.Columns.TABLE]) {
            var lookupTableName = tableColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
            if (lookupTableName == parameters.baasViewDetails[Constants.Baas.Views.TABLE][Constants.Baas.Tables.ID]) {
                return  tableColumn[Constants.Baas.Tables.Columns.EXPRESSION];
            }
        }
    }
}

function getTableColumns(tableName, logid, callback) {
    var tableFilter = {};
    tableFilter[Constants.Baas.Tables.ID] = "{tableId__}";
    var tableQuery = {};
    tableQuery[QueryConstants.Query.TABLE] = Constants.Baas.TABLES;
    tableQuery[QueryConstants.Query.COLUMNS] = [Constants.Baas.Tables.COLUMNS];
    tableQuery[QueryConstants.Query.FILTER] = tableFilter;
    tableQuery[QueryConstants.Query.PARAMETERS] = {"tableId__":tableName};
    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        logid:logid,
        query:tableQuery,
        callback:ApplaneCallback(callback, function (result) {
            callback(null, result[QueryConstants.Query.Result.DATA]);
        })
    })
}

function getView(parameters, callback) {
    var viewCallBack = parameters[RequestConstants.AppsStudio.CUSTOMIZATION][RequestConstants.AppsStudio.Customization.CALLBACK];
    if (viewCallBack == undefined || viewCallBack == true) {
        var viewParameters = {};
        viewParameters[RequestConstants.AppsStudio.ASK] = parameters[RequestConstants.AppsStudio.ASK];
        viewParameters[RequestConstants.AppsStudio.OSK] = parameters[RequestConstants.AppsStudio.OSK];
        viewParameters[RequestConstants.AppsStudio.USK] = parameters[RequestConstants.AppsStudio.USK];
        viewParameters[RequestConstants.AppsStudio.USER] = parameters[RequestConstants.AppsStudio.USER];
        viewParameters[RequestConstants.AppsStudio.LOGID] = parameters[RequestConstants.AppsStudio.LOGID];
        viewParameters[RequestConstants.AppsStudio.VIEWID] = parameters[RequestConstants.AppsStudio.VIEWID];
        ViewService.getView(viewParameters, callback);
    } else {
        callback(null, "success");
    }
}

exports.saveCopyView = function (parameters, callback) {
    var viewId = parameters[RequestConstants.AppsStudio.VIEWID];
    if (!viewId) {
        throw new AppsStudioError(Constants.ErrorCode.FIELDS_BLANK, ["viewid"]);
    }
    var osk = parameters[RequestConstants.AppsStudio.OSK];
    var ask = parameters[RequestConstants.AppsStudio.ASK];
    var user = parameters[RequestConstants.AppsStudio.USER];
    var logid = parameters[RequestConstants.AppsStudio.LOGID];
    var customization = parameters[RequestConstants.AppsStudio.CUSTOMIZATION] ? JSON.parse(parameters[RequestConstants.AppsStudio.CUSTOMIZATION]) : {};
    var level = customization[RequestConstants.AppsStudio.Customization.LEVEL] || RequestConstants.AppsStudio.Customization.Level.SELF;
    checkAccessRights(user, ask, osk, level);
    var copyViewLabel = customization[RequestConstants.AppsStudio.Customization.LABEL];
    var override = customization[RequestConstants.AppsStudio.Customization.OVERRIDE];
    MetadataProvider.getOrganization(osk, ApplaneCallback(callback, function (organizationDetail) {
        var organizationId = organizationDetail ? organizationDetail[QueryConstants.Query._ID] : organizationDetail;
        ViewService.getUserOrganizationViewDetails(viewId, user[QueryConstants.Query._ID], organizationId, logid, ApplaneCallback(callback, function (userOrganizationViewDetails) {
            var userOrganizationViewDetail = userOrganizationViewDetails && userOrganizationViewDetails.length > 0 ? userOrganizationViewDetails[0] : null;
            ViewService.getUserOrganizationViewDetails(viewId, null, organizationId, logid, ApplaneCallback(callback, function (organizationViewDetails) {
                var organizationViewDetail = organizationViewDetails && organizationViewDetails.length > 0 ? organizationViewDetails[0] : null;
                ViewService.getViewDetails(viewId, user[QueryConstants.Query._ID], organizationId, logid, ApplaneCallback(callback, function (viewDetails) {
                    if (!viewDetails || viewDetails.length == 0) {
                        throw new AppsStudioError(Constants.ErrorCode.VIEW_NOT_FOUND, [viewId]);
                    }
                    var viewDetail = viewDetails[0];
                    var viewKey;
                    var viewInCustomization;
                    var schedules;
                    if (override) {
                        if (level == RequestConstants.AppsStudio.Customization.Level.SELF) {
                            if (userOrganizationViewDetail) {
                                viewKey = userOrganizationViewDetail[QueryConstants.Query._ID];
                                viewDetail[Constants.AppsStudio.Views.LABEL] = userOrganizationViewDetail[Constants.AppsStudio.Views.LABEL] || viewDetail[Constants.AppsStudio.Views.LABEL];
                            }
                        } else if (level == RequestConstants.AppsStudio.Customization.Level.ORGANIZATION) {
                            if (userOrganizationViewDetail) {
                                viewInCustomization = userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION];
                                schedules = userOrganizationViewDetail[Constants.AppsStudio.Views.SCHEDULES];
                                viewDetail[Constants.AppsStudio.Views.LABEL] = userOrganizationViewDetail[Constants.AppsStudio.Views.LABEL] || viewDetail[Constants.AppsStudio.Views.LABEL];
                            }
                            if (organizationViewDetail) {
                                viewKey = organizationViewDetail[QueryConstants.Query._ID];
                                viewDetail[Constants.AppsStudio.Views.LABEL] = organizationViewDetail[Constants.AppsStudio.Views.LABEL] || viewDetail[Constants.AppsStudio.Views.LABEL];
                            }
                        } else {
                            if (userOrganizationViewDetail) {
                                viewInCustomization = userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION];
                                schedules = userOrganizationViewDetail[Constants.AppsStudio.Views.SCHEDULES];
                                viewDetail[Constants.AppsStudio.Views.LABEL] = userOrganizationViewDetail[Constants.AppsStudio.Views.LABEL] || viewDetail[Constants.AppsStudio.Views.LABEL];
                            } else if (organizationViewDetail) {
                                viewInCustomization = organizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION];
                                schedules = organizationViewDetail[Constants.AppsStudio.Views.SCHEDULES];
                                viewDetail[Constants.AppsStudio.Views.LABEL] = organizationViewDetail[Constants.AppsStudio.Views.LABEL] || viewDetail[Constants.AppsStudio.Views.LABEL];
                            }
                            viewKey = viewDetail[QueryConstants.Query._ID];
                        }
                    }
                    else {
                        viewInCustomization = viewDetail[Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(viewDetail[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
                        schedules = viewDetail[Constants.AppsStudio.Views.SCHEDULES] || [];
                        if (organizationViewDetail) {
                            var viewToCustomization = organizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(organizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
                            ViewService.mergeViewCustomization(viewInCustomization, viewToCustomization);
                            var schedulesToMerge = organizationViewDetail[Constants.AppsStudio.Views.SCHEDULES] || [];
                            schedules = ViewService.mergeViewSchedules(schedules, schedulesToMerge);
                            viewDetail[Constants.AppsStudio.Views.LABEL] = organizationViewDetail[Constants.AppsStudio.Views.LABEL] || viewDetail[Constants.AppsStudio.Views.LABEL];
                        }
                        if (userOrganizationViewDetail) {
                            var viewToCustomization = userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION] ? JSON.parse(userOrganizationViewDetail[Constants.AppsStudio.Views.CUSTOMIZATION]) : {};
                            ViewService.mergeViewCustomization(viewInCustomization, viewToCustomization);
                            var schedulesToMerge = userOrganizationViewDetail[Constants.AppsStudio.Views.SCHEDULES] || [];
                            schedules = ViewService.mergeViewSchedules(schedules, schedulesToMerge);
                            viewDetail[Constants.AppsStudio.Views.LABEL] = userOrganizationViewDetail[Constants.AppsStudio.Views.LABEL] || viewDetail[Constants.AppsStudio.Views.LABEL];
                        }
                    }
                    var viewToCopy = {};
                    if (!viewKey) {
                        var baasView = {};
                        baasView[QueryConstants.Query._ID] = viewDetail[Constants.AppsStudio.Views.BAAS_VIEWID][QueryConstants.Query._ID];
                        viewToCopy[Constants.AppsStudio.Views.BAAS_VIEWID] = baasView;
                        viewToCopy[Constants.AppsStudio.Views.QUICK_VIEWID] = viewId;
                        if (level == RequestConstants.AppsStudio.Customization.Level.SELF) {
                            viewToCopy[Constants.AppsStudio.Views.USERID] = user[QueryConstants.Query._ID];
                            viewToCopy[Constants.AppsStudio.Views.ORGANIZATIONID] = organizationId;
                        } else if (level == RequestConstants.AppsStudio.Customization.Level.ORGANIZATION) {
                            viewToCopy[Constants.AppsStudio.Views.ORGANIZATIONID] = organizationId;
                        }
                        if (override == undefined || override != true) {
                            viewToCopy[Constants.AppsStudio.Views.ID] = viewId + "__" + new Date().getTime();
                        } else {
                            viewToCopy[Constants.AppsStudio.Views.ID] = viewId;
                        }
                    }
                    if (viewInCustomization) {
                        viewToCopy[Constants.AppsStudio.Views.CUSTOMIZATION] = (typeof viewInCustomization == "string") ? viewInCustomization : JSON.stringify(viewInCustomization);
                    }
                    if (copyViewLabel && copyViewLabel.trim().length > 0) {
                        viewToCopy[Constants.AppsStudio.Views.LABEL] = copyViewLabel;
                    } else if (!viewKey) {
                        viewToCopy[Constants.AppsStudio.Views.LABEL] = viewDetail[Constants.AppsStudio.Views.LABEL] || viewId;
                    }
                    if (schedules && schedules.length > 0) {
                        for (var i = 0; i < schedules.length; i++) {
                            var schedule = schedules[i];
                            if (!viewKey && schedule[Constants.AppsStudio.Views.Schedules.SCHEDULEID]) {
                                var scheduleObject = schedule[Constants.AppsStudio.Views.Schedules.SCHEDULEID];
                                delete scheduleObject[QueryConstants.Query._ID];
                                scheduleObject[Constants.AppsStudio.Schedules.VIEWID] = viewToCopy[Constants.AppsStudio.Views.ID];
                            }
                        }
                        viewToCopy[Constants.AppsStudio.Views.SCHEDULES] = schedules;
                    }
                    var updatesObj = {};
                    updatesObj[QueryConstants.Query.TABLE] = Constants.AppsStudio.VIEWS;
                    viewToCopy[QueryConstants.Query._ID] = viewKey
                    updatesObj[QueryConstants.Update.OPERATIONS] = viewToCopy;
                    UpdateEngine.performUpdate({
                        ask:Constants.AppsStudio.ASK,
                        logid:logid,
                        updates:updatesObj,
                        callback:ApplaneCallback(callback, function () {
                            if (viewToCopy[Constants.AppsStudio.Views.ID]) {
                                parameters[RequestConstants.AppsStudio.VIEWID] = viewToCopy[Constants.AppsStudio.Views.ID];
                            }
                            getView(parameters, callback);
                        })
                    });
                }));
            }));
        }));
    }));
}

function checkAccessRights(user, ask, osk, level) {
    var userApplications = user[Constants.Baas.Users.APPLICATIONS];
    if (level == RequestConstants.AppsStudio.Customization.Level.ORGANIZATION) {
        if (userApplications && userApplications.length > 0) {
            for (var i = 0; i < userApplications.length; i++) {
                var userApplication = userApplications[i];
                var userApplicationOrganization = userApplication[Constants.Baas.Users.Applications.ORGANIZATIONID];
                if (userApplicationOrganization && userApplicationOrganization[Constants.Baas.Organizations.OSK] == osk && userApplicationOrganization.admin == true) {
                    return true;
                }
            }
        }
        throw new AppsStudioError(Constants.ErrorCode.User.NOT_ORG_ACCESS);
    } else if (level == RequestConstants.AppsStudio.Customization.Level.APPLICATION) {
        if (userApplications && userApplications.length > 0) {
            for (var i = 0; i < userApplications.length; i++) {
                var userApplication = userApplications[i];
                if (userApplication[Constants.Baas.Applications.ASK] == ask && userApplication.developer == true) {
                    return true;
                }
            }
        }
        throw new AppsStudioError(Constants.ErrorCode.User.NOT_APP_ACCESS);
    }
}



