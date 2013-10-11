/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 7/16/13
 * Time: 12:53 PM
 * To change this template use File | Settings | File Templates.
 */

var MongoDBManager = require('./mongodb/MongoDBManager.js');
var MetadataProvider = require('../metadata/MetadataProvider.js');
var Constants = require('../shared/Constants.js');
var APIConstants = require('nodejsapi/Constants.js');
var BaasError = require("apputil/ApplaneError.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var FilterUtil = require("../util/filterutil.js");
var TableSchema = require("../util/TableSchema.js");
var ModuleManager = require("./module/ModuleManager.js");
var Utils = require("apputil/util.js");
var ObjectID = require('mongodb').ObjectID;
var CustomCodeManager = require("../customcode/customcodemanager.js");
var LogUtility = require("../util/LogUtility.js");

exports.query = function (options) {
    queryInternal(options.query, {ask:options.ask, osk:options.osk, usk:options.usk, user:options.user, logid:options.logid, state:options.state}, options.callback);
}

function queryInternal(query, options, callback) {
//    console.log("Applane Query: " + JSON.stringify(query));
    if (!query) {
        callback(new BaasError(Constants.ErrorCode.FIELDS_BLANK, ["query"]));
        return;
    }
    // make clone
    query = JSON.parse(JSON.stringify(query));
    var tableName = query[APIConstants.Query.TABLE];
    if (!tableName) {
        callback(new BaasError(Constants.ErrorCode.FIELDS_BLANK_IN_OBJECT, [APIConstants.Query.TABLE, "query", query]));
        return;
    }

    MetadataProvider.getTable(tableName, options, ApplaneCallback(callback, function (data) {
        var table = data.table;
        var db = data.database;
        options.application = data.application;
        options.currentapplication = data.currentapplication;
        if (query[APIConstants.Query.VIEW]) {
            MetadataProvider.getView(tableName, query[APIConstants.Query.VIEW], ApplaneCallback(callback, function (view) {
                if (view[Constants.Baas.Views.QUERY]) {
                    query = mergeQuery(query, view[Constants.Baas.Views.QUERY]);
                }
                compositeQuery(db, query, table, options, callback);
            }));
        } else {
            compositeQuery(db, query, table, options, callback);
        }
    }));
}

callJobModule = function (query, result, table, when, options, callback) {
    if (when == Constants.Baas.Tables.Jobs.When.BEFORE) {
        callJob(query, result, table, when, options, ApplaneCallback(callback, function (params) {
            query = params.query;
            ModuleManager.beforeQuery(table, query, options, ApplaneCallback(callback, function () {
                callback(null, query);
            }));
        }));
    } else {
        ModuleManager.afterQueryResult(table, query, result, options, ApplaneCallback(callback, function () {
            callJob(query, result, table, when, options, ApplaneCallback(callback, function (params) {
                callback(null, params.result);
            }));
        }));
    }
}

function callJob(query, result, table, when, options, callback) {
    var excludeJobs = query[APIConstants.Query.EXCLUDE_JOBS] || false;
    var requiredJobs = [];
    if (!excludeJobs) {
        var jobs = table[Constants.Baas.Tables.JOBS];
        var count = jobs ? jobs.length : 0;
        for (var i = 0; i < count; i++) {
            var job = jobs[i];
            var jobOperation = job[Constants.Baas.Tables.Jobs.OPERATION];
            var jobWhen = job[Constants.Baas.Tables.Jobs.WHEN];
            var jobApplication = job[Constants.Baas.Tables.Jobs.APPLICATION];
            var jobType = job[Constants.Baas.Tables.Jobs.TYPE];
            if ((!jobApplication || jobApplication[APIConstants.Query._ID].toString() == options.currentapplication[APIConstants.Query._ID].toString())) {
                if (jobOperation == Constants.Baas.Tables.Jobs.Operation.QUERY && jobWhen == when) {
                    requiredJobs.push(job);
                }
                if ((jobType == Constants.Baas.Tables.Jobs.Type.COMMANDQUERY && when == 'before') || (jobType == Constants.Baas.Tables.Jobs.Type.QUERY && when == 'after')) {
                    requiredJobs.push(job);
                }
            }
        }
    }

    Utils.iterateArray(requiredJobs, ApplaneCallback(callback, function () {
        callback(null, {query:query, result:result})
    }), function (requiredJob, callback) {
        CustomCodeManager.callJobs({
            ask:options.ask,
            "mainask":table[Constants.Baas.Tables.MAINAPPLICATION][Constants.Baas.Applications.ASK],
            "osk":options.osk,
            "usk":options.usk,
            "user":options.user,
            "code":requiredJob[Constants.Baas.Tables.Jobs.CODE],
            "when":when,
            "operation":Constants.Baas.Tables.Jobs.Operation.QUERY,
            "module":requiredJob[Constants.Baas.Tables.Jobs.MODULE],
            "method":when + Utils.capitalize(Constants.Baas.Tables.Jobs.Operation.QUERY),
            "parameters":{
                query:query,
                result:result
            },
            "callback":ApplaneCallback(callback, function (params) {
                query = params.query;
                result = params.result;
                callback();
            })
        });
    });
}

function getQueryMetadata(table, query) {
    var metadata = {};
    var tableColumns = JSON.parse(JSON.stringify(table[APIConstants.Query.COLUMNS]));
    if (query[APIConstants.Query.COLUMNS]) {
        var columns = query[APIConstants.Query.COLUMNS];
        var tableColumns = tableColumns;
        var newColumns = [];
        columns.forEach(function (column) {
            var expression = typeof(column) == 'string' ? column : column[Constants.Baas.Tables.Columns.EXPRESSION];
            var newColumn = getColumn(expression, tableColumns);
            if (newColumn) {
                newColumn = JSON.parse(JSON.stringify(newColumn));
                newColumn[Constants.Baas.Tables.Columns.EXPRESSION] = expression;
                newColumns.push(newColumn);
            }
        });
        metadata[APIConstants.Query.COLUMNS] = newColumns;
    } else {
        metadata[APIConstants.Query.COLUMNS] = tableColumns;
    }
    return  metadata;
}

function compositeQuery(database, query, table, options, callback) {
    var queryClone = JSON.parse(JSON.stringify(query));
    callJobModule(query, null, table, Constants.Baas.Tables.Jobs.When.BEFORE, options, ApplaneCallback(callback, function (query) {

        if (query[APIConstants.Query.MAX_ROWS] == 0) {
            var result = {};
            result[APIConstants.Query.Result.DATA] = [];
            if (query[APIConstants.Query.METADATA]) {
                result[APIConstants.Query.Result.METADATA] = getQueryMetadata(table, query);
            }
            callback(null, result);
            return;
        }
        var filter = query[APIConstants.Query.FILTER];
        var parameters = query[APIConstants.Query.PARAMETERS];
        if (parameters) {
            if ((parameters instanceof Array) && !(parameters instanceof Object)) {
                throw new BaasError(Constants.ErrorCode.NOT_VALID_FIELDS_IN_OBJECT, [APIConstants.Query.PARAMETERS, "query", query]);
            }
        } else {
            parameters = {};
        }

        FilterUtil.resolveFilters(filter, parameters, table, options, ApplaneCallback(callback, function () {
            var queryOptions = {logid:options.logid};
            var columns = query[APIConstants.Query.COLUMNS];
            var expressions = [];
            var groups = {};
            if (columns) {
                if (!(columns instanceof Array)) {
                    throw new BaasError(Constants.ErrorCode.NOT_VALID_FIELDS_IN_OBJECT, [APIConstants.Query.COLUMNS, "query", query]);
                }
                var fields = {};
                var unwindColumns = [];
                var reverseUnwindColumns = [];
                populateFields(table, query, columns, fields, unwindColumns, reverseUnwindColumns, expressions, groups);
                if (Object.keys(fields).length > 0) {
                    queryOptions.fields = fields;
                }
                queryOptions.unwindcolumns = unwindColumns;
            }
            var orders = query[APIConstants.Query.ORDERS];
            var recursiveOrders = {};
            if (orders) {
                var sort = {};
                populateSort(sort, orders, table, recursiveOrders);
                queryOptions.sort = sort;
            }
            var maxRows = query[APIConstants.Query.MAX_ROWS];
            var cursor = query[APIConstants.Query.CURSOR];
            if (maxRows && maxRows > 0) {
                queryOptions.limit = maxRows + 1;
                if (cursor) {
                    queryOptions.skip = cursor;
                }
            }
            var groupColumns = query[APIConstants.Query.GROUPS];
            if (groupColumns && groupColumns.length > 0) {
                var groupColumnLength = groupColumns.length;
                if (groupColumnLength > 1) {
                    var groupIds = {};
                    for (var i = 0; i < groupColumnLength; i++) {
                        var groupColumn = groupColumns[i];
                        groupIds[groupColumn] = "$" + groupColumn;
                    }
                    groups[APIConstants.Query._ID] = groupIds;
                } else {
                    groups[APIConstants.Query._ID] = "$" + groupColumns[0];
                }
            }
            if (Object.keys(groups).length > 0) {
                if (!groups[APIConstants.Query._ID]) {
                    groups[APIConstants.Query._ID] = null;
                }
                queryOptions.groups = groups;
            }

            runOnMongoDB(database, table[Constants.Baas.Tables.ID], filter, queryOptions, ApplaneCallback(callback, function (data) {
                var nextCursor;
                if (maxRows && maxRows > 0) {
                    var dataLength = data.length;
                    if (dataLength > maxRows) {
                        data.pop();
                        nextCursor = cursor ? cursor + maxRows : maxRows
                    }
                }
                populateChildData(query, data, options, ApplaneCallback(callback, function () {
                    if (reverseUnwindColumns && reverseUnwindColumns.length > 0) {
                        data = unwindReverseColumnData(reverseUnwindColumns, data);
                    }
                    if (expressions.length > 0) {
                        populateResultForDotedColumn(expressions, data, !query[APIConstants.Query.KEEP_STRUCTURE]);
                    }

                    if (Object.keys(recursiveOrders).length > 0) {
                        data = populateRecursiveData(recursiveOrders, data);
                    }

//                    console.log("DataCount>>>>>>>" + data.length);
                    var result = {};
                    result[APIConstants.Query.Result.DATA] = data;
                    if (query[APIConstants.Query.METADATA]) {
                        result[APIConstants.Query.Result.METADATA] = getQueryMetadata(table, query);
                    }
                    result[APIConstants.Query.Result.CURSOR] = nextCursor;
                    callJobModule(queryClone, result, table, Constants.Baas.Tables.Jobs.When.AFTER, options, ApplaneCallback(callback, function (result) {
                        callback(null, result);
                    }));
                }));
            }));
        }));
    }));
}

function populateSort(sort, orders, table, recursiveOrder) {
    if (orders instanceof Array) {
        var length = orders.length;
        for (var i = 0; i < length; i++) {
            populateSort(sort, orders[i], table, recursiveOrder);
        }
    } else if (orders instanceof Object) {
        var objectKeys = Object.keys(orders);
        if (objectKeys.length != 1) {
            throw new BaasError(Constants.ErrorCode.NOT_VALID_FIELDS_IN_OBJECT, [APIConstants.Query.ORDERS, "query", JSON.stringify(orders)]);
        }
        var orderColumn = objectKeys[0];
        var orderColumnObject = TableSchema.getColumn(orderColumn, table);
        if (orderColumnObject && orderColumnObject[Constants.Baas.Tables.Columns.TYPE] == Constants.Baas.Tables.Columns.Type.LOOKUP && orderColumnObject[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] && orderColumnObject[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS].length > 0) {
            orderColumn = orderColumn + "." + orderColumnObject[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS][0][Constants.Baas.Tables.Columns.EXPRESSION];
        }
        var orderType = orders[objectKeys[0]];
        if (orderType instanceof Object) {
            if (orderType[APIConstants.Query.Orders.RECURSIVE]) {
                if (Object.keys(recursiveOrder).length > 0) {
                    throw new BaasError(Constants.ErrorCode.NOT_VALID_FIELDS_IN_OBJECT, [APIConstants.Query.ORDERS, "query", JSON.stringify(orders)]);
                }
                recursiveOrder[objectKeys[0]] = orderType;
            }
            orderType = orderType[APIConstants.Query.Orders.ORDER] || APIConstants.Query.Orders.ASC;
        }
        if (orderType != APIConstants.Query.Orders.ASC && orderType != APIConstants.Query.Orders.DESC) {
            throw new BaasError(Constants.ErrorCode.NOT_VALID_FIELDS_IN_OBJECT, [APIConstants.Query.ORDERS, "query", JSON.stringify(orders)]);
        }
        sort[orderColumn] = orderType == APIConstants.Query.Orders.ASC ? 1 : -1;
    } else if (typeof(orders) == "string") {
        var orderColumnObject = TableSchema.getColumn(orders, table);
        if (orderColumnObject && orderColumnObject[Constants.Baas.Tables.Columns.TYPE] == Constants.Baas.Tables.Columns.Type.LOOKUP && orderColumnObject[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] && orderColumnObject[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS].length > 0) {
            orders = orders + "." + orderColumnObject[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS][0][Constants.Baas.Tables.Columns.EXPRESSION];
        }
        sort[orders] = 1;
    } else {
        throw new BaasError(Constants.ErrorCode.NOT_VALID_FIELDS_IN_OBJECT, [APIConstants.Query.ORDERS, "query", JSON.stringify(orders)]);
    }
}
function populateFields(table, query, columns, fields, unwindcolumns, reverseUnwindcolumns, expressions, groups, pExp) {
    var length = columns.length;
    for (var i = 0; i < length; i++) {
        var column = columns[i];
        if (column instanceof Object) {
            if (column[APIConstants.Query.Columns.EXPRESSION]) {
                var expression = column[APIConstants.Query.Columns.EXPRESSION];
                var alias = column[APIConstants.Query.Columns.ALIAS] || expression;
                var mainExpression = {};
                expressions.push(mainExpression);
                mainExpression.expression = expression;
                mainExpression.alias = alias;
                if (checkReverseMultipleLookupColumns(column, table, query, reverseUnwindcolumns)) {
                    continue;
                }
                checkAsUnwindColumns(table[Constants.Baas.Tables.SYSTEM_COLUMNS], query, expression, unwindcolumns);
                if (column[APIConstants.Query.Columns.AGGERGATE]) {
                    var aggregate = column[APIConstants.Query.Columns.AGGERGATE];
                    var dataAlias = alias.replace(/\./gi, "_");
                    mainExpression.dataalias = dataAlias;
                    groups[dataAlias] = {};
                    if (aggregate == APIConstants.Query.Columns.Aggregates.COUNT) {
                        groups[dataAlias]["$" + APIConstants.Query.Columns.Aggregates.SUM] = 1;
                    } else {
                        groups[dataAlias]["$" + aggregate] = "$" + expression;
                    }
                } else if (column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS]) {
                    fields[(pExp ? pExp + "." + expression : expression) + "." + APIConstants.Query._ID] = 1;
                    populateFields(table, query, column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS], fields, unwindcolumns, reverseUnwindcolumns, expressions, groups, pExp ? pExp + "." + expression : expression);
                } else {
                    fields[pExp ? pExp + "." + expression : expression] = 1;
                }
            } else {
                throw new BaasError(Constants.ErrorCode.NOT_VALID_FIELDS_IN_OBJECT, [APIConstants.Query.COLUMNS, "query", JSON.stringify(query)]);
            }
        } else if (typeof(column) == "string") {
            expressions.push({expression:column});
            if (checkReverseMultipleLookupColumns(column, table, query, reverseUnwindcolumns)) {
                continue;
            }
            checkAsUnwindColumns(table[Constants.Baas.Tables.SYSTEM_COLUMNS], query, column, unwindcolumns);
            fields[pExp ? pExp + "." + column : column] = 1;
        } else {
            throw new BaasError(Constants.ErrorCode.NOT_VALID_FIELDS_IN_OBJECT, [APIConstants.Query.COLUMNS, "query", query]);
        }
    }

}

function columnExists(expression, columns) {
    if (columns) {
        var length = columns.length;
        for (var i = 0; i < length; i++) {
            var column = columns[i];
            if (column[Constants.Baas.Tables.Columns.EXPRESSION] == expression) {
                return true;
            }
        }
    }
    return false;
}

function checkAsUnwindColumns(columns, query, expression, unwindcolumns, preExpression) {
    var index = expression.indexOf(".");
    if (index != -1) {
        var firstPart = expression.substr(0, index);
        var secondPart = expression.substr(index + 1);
        var column = getColumn(firstPart, columns);
        firstPart = preExpression ? preExpression + "." + firstPart : firstPart;
        if (column && column[Constants.Baas.Tables.Columns.MULTIPLE]) {
            if (!columnExists(firstPart, unwindcolumns)) {
                var unwindColumn = {};
                unwindColumn[Constants.Baas.Tables.Columns.EXPRESSION] = firstPart;
                var childQueryFilter = {};
                var queryFilter = query[APIConstants.Query.FILTER];
                removeFilters(firstPart, queryFilter, childQueryFilter, true);
                unwindColumn[APIConstants.Query.FILTER] = childQueryFilter;
                unwindcolumns.push(unwindColumn);
            }
        }
        if (column && column[Constants.Baas.Tables.COLUMNS]) {
            checkAsUnwindColumns(column[Constants.Baas.Tables.COLUMNS], query, secondPart, unwindcolumns, firstPart)
        }
    }

}

function getColumn(expression, columns) {
    if (columns) {
        var index = expression.indexOf(".");
        if (index != -1) {
            var firstPart = expression.substr(0, index);
            var length = columns.length;
            for (var i = 0; i < length; i++) {
                var column = columns[i];
                if (column[Constants.Baas.Tables.Columns.EXPRESSION] == firstPart) {
                    var secondPart = expression.substr(index + 1);
                    var column = getColumn(secondPart, column[Constants.Baas.Tables.Columns.COLUMNS]);
                    if (column) {
                        return column;
                    }
                    return getColumn(secondPart, column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS]);
                }
            }
        } else {
            var length = columns.length;
            for (var i = 0; i < length; i++) {
                var column = columns[i];
                if (column[Constants.Baas.Tables.Columns.EXPRESSION] == expression) {
                    return column;
                }
            }
        }
    }
}

function runOnMongoDB(database, collection, query, options, callback) {
    ensureIdColumns(options.fields);
    if ((options.unwindcolumns && options.unwindcolumns.length > 0) || options.groups) {
        var pipeline = [];
        if (query) {
            pipeline.push({$match:query});
        }
        var length = options.unwindcolumns ? options.unwindcolumns.length : 0;
        for (var i = 0; i < length; i++) {
            var unwindColumn = options.unwindcolumns[i];
            pipeline.push({$unwind:"$" + unwindColumn[Constants.Baas.Tables.Columns.EXPRESSION]});
            if (unwindColumn[APIConstants.Query.FILTER] && Object.keys(unwindColumn[APIConstants.Query.FILTER]).length > 0) {
                pipeline.push({$match:unwindColumn[APIConstants.Query.FILTER]});
            }
        }

        if (options.groups) {
            pipeline.push({$group:options.groups});
        }

        if (options.fields) {
            pipeline.push({$project:options.fields});
        }
        if (options.sort) {
            pipeline.push({$sort:options.sort});
        }

        if (options.skip) {
            pipeline.push({$skip:options.skip});
        }

        if (options.limit) {
            pipeline.push({$limit:options.limit});
        }
        MongoDBManager.aggregate(database[Constants.Baas.Tables.DB], collection, pipeline, options, callback);
    } else {
        MongoDBManager.find(database[Constants.Baas.Tables.DB], collection, query, options, callback);
    }
}

function ensureIdColumns(fields) {
    if (fields) {
        for (column in fields) {
            var expression = column;
            var index = expression.indexOf(".");
            var pre = null;
            while (index > 0) {
                var firstPart = expression.substring(0, index);
                expression = expression.substring(index + 1);
                fields[(pre ? (pre + '.') : "") + firstPart + "." + APIConstants.Query._ID] = 1;
                pre = (pre ? (pre + '.') : "") + firstPart;
                index = expression.indexOf(".");
            }
        }
    }
}

function populateResultForDotedColumn(expressions, data, handleDot) {
    if (expressions && data && data.length > 0) {
        expressions.forEach(function (expressionObject) {
            var expression = expressionObject.expression;
            var alias = expressionObject.alias || expression;
            var dataAlias = expressionObject.dataalias;
            var index = expression.indexOf(".");
            data.forEach(function (row) {

                    if (dataAlias) {
                        if (dataAlias != alias) {
                            row[alias] = row[dataAlias];
                            delete row[dataAlias];
                        }
                        return;
                    }
                    if (handleDot && index != -1) {
                        var firstPart = expression.substr(0, index);
                        var secondPart = expression.substr(index + 1);
                        var value = row[firstPart];
                        if (value instanceof Object) {
                            if (secondPart.indexOf(".") == -1) {
                                if (value[secondPart]) {
                                    row[expression] = value[secondPart];
                                    delete value[secondPart];
                                }
                            } else {
                                populateResultForDotedColumn([
                                    {expression:secondPart}
                                ], [value], true);
                                row[expression] = value[secondPart];
                                delete value[secondPart];
                            }
                            for (innerKey in value) {
                                if (isKeyColumn(innerKey)) {
                                    row[firstPart + "." + innerKey] = value[innerKey];
                                    delete value[innerKey];
                                }
                            }
                            if (Object.keys(value).length == 0) {
                                delete row[firstPart];
                            }
                        }

                    }
                    if (alias != expression) {
                        row[alias] = row[expression];
                        delete row[expression];
                    }
                }
            );
        });
    }
}

function isKeyColumn(key) {
    return key == APIConstants.Query._ID || new RegExp(".+(._id)$").test(key);
}

function populateChildData(query, data, options, callback) {
    var childs = query[APIConstants.Query.CHILDS];
    Utils.iterateArray(childs, callback, function (child, callback) {
        var alias = child[APIConstants.Query.Childs.ALIAS];
        var childQuery = JSON.parse(JSON.stringify(child[APIConstants.Query.Childs.QUERY]));
        var relatedColumn = child[APIConstants.Query.Childs.RELATED_COLUMN];
        var parentColumn = child[APIConstants.Query.Childs.PARENT_COLUMN];
        parentColumn = parentColumn || APIConstants.Query._ID;
        var userIn = child[APIConstants.Query.Childs.USE_IN];
        ensureColumnInQuery(relatedColumn, childQuery);
        if (userIn) {
            var parentColumnValues = [];
            var rowCount = data.length;
            for (var i = 0; i < rowCount; i++) {
                var row = data[i];
                if (row[parentColumn]) {
                    parentColumnValues.push(row[parentColumn]);
                }
            }
            var childFilter = childQuery[APIConstants.Query.FILTER] || {};
            childFilter[relatedColumn] = {"$in":parentColumnValues};
            childQuery[APIConstants.Query.FILTER] = childFilter;
            queryInternal(childQuery, options, ApplaneCallback(callback, function (childData) {
                for (var i = 0; i < rowCount; i++) {
                    var row = data[i];
                    var parentColumnValue = row[parentColumn];
                    if (parentColumnValue) {
                        row[alias] = getChildData(relatedColumn, getColumnValue(row, parentColumn), childData.data);
                    }
                }
                callback();
            }));
        } else {
            var childFilter = childQuery[APIConstants.Query.FILTER] || {};
            childFilter[relatedColumn] = "{" + parentColumn + "}";
            childQuery[APIConstants.Query.FILTER] = childFilter;
            var parameters = childQuery[APIConstants.Query.PARAMETERS] || {};
            childQuery[APIConstants.Query.PARAMETERS] = parameters;
            Utils.iterateArray(data, callback, function (row, callback) {
                parameters[parentColumn] = row[parentColumn];
                queryInternal(childQuery, options, ApplaneCallback(callback, function (childData) {
                    row[alias] = childData.data;
                    callback();
                }));
            });
        }
    });
}

function getColumnValue(row, column, all) {
    if (row[column]) {
        var value = row[column];
        if (!all && !(value instanceof ObjectID) && value instanceof Object && value[APIConstants.Query._ID]) {
            return value[APIConstants.Query._ID]
        }
        return value;
    } else {
        var index = column.indexOf(".")
        if (index != -1) {
            var firstColumn = column.substring(0, index);
            if (firstColumn && row[firstColumn] && row[firstColumn] instanceof Object) {
                return getColumnValue(row[firstColumn], column.substring(index + 1), all);
            }
        }
    }
    return null;
}

function getChildData(relatedColumn, parentValue, data) {
    var rowCount = data.length;
    var childData = [];
    for (var i = 0; i < rowCount; i++) {
        var row = data[i];
        var relatedColumnValue = row[relatedColumn];
        var contains = false;
        if (relatedColumnValue instanceof Array) {
            var valueLength = relatedColumnValue.length;
            for (var j = 0; j < valueLength; j++) {
                var value = relatedColumnValue[j];
                if (!(value instanceof ObjectID) && value instanceof Object) {
                    contains = value[APIConstants.Query._ID].toString() == parentValue.toString();
                } else {
                    contains = value.toString() == parentValue.toString();
                }
                if (contains) {
                    break;
                }
            }
        } else if (!(relatedColumnValue instanceof ObjectID) && relatedColumnValue instanceof Object) {
            contains = relatedColumnValue[APIConstants.Query._ID].toString() == parentValue.toString();
        } else {
            contains = relatedColumnValue.toString() == parentValue.toString();
        }
        if (contains) {
            childData.push(row);
        }
    }
    return childData;
}


function checkReverseMultipleLookupColumns(column, table, query, reverseUnwindcolumns) {
    var expression = column;
    var innerColumns;
    if (column instanceof Object) {
        expression = column[Constants.Baas.Tables.Columns.EXPRESSION];
        innerColumns = column[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS];
    }

    var index = expression.indexOf(".");
    var firstPart = expression;
    var secondPart;
    if (index != -1) {
        firstPart = expression.substr(0, index);
        secondPart = expression.substr(index + 1);
    }
    var columns = table[Constants.Baas.Tables.SYSTEM_COLUMNS];
    var savedColumn = getColumn(firstPart, columns);
    if (savedColumn && savedColumn[Constants.Baas.Tables.Columns.MULTIPLE] && savedColumn[Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN]) {
        var childs = query[APIConstants.Query.CHILDS] || [];
        query[APIConstants.Query.CHILDS] = childs;
        var childAlias = savedColumn[Constants.Baas.Tables.Columns.EXPRESSION];
        var child;
        childs.forEach(function (existingChild) {
            if (existingChild[APIConstants.Query.Childs.ALIAS] == childAlias) {
                child = existingChild;
            }
        });
        if (!child) {
            child = {};
            child[APIConstants.Query.Childs.ALIAS] = childAlias;
            child[APIConstants.Query.Childs.USE_IN] = true;
            child[APIConstants.Query.Childs.RELATED_COLUMN] = savedColumn[Constants.Baas.Tables.Columns.REVERSE_RELATION_COLUMN];
            var childQuery = {};
            child[APIConstants.Query.Childs.QUERY] = childQuery;
            childQuery[APIConstants.Query.COLUMNS] = [];
            childQuery[APIConstants.Query.TABLE] = savedColumn[Constants.Baas.Tables.Columns.TABLE][Constants.Baas.Tables.ID];
            var childQueryFilter = {};
            var queryFilter = query[APIConstants.Query.FILTER];
            removeFilters(childAlias, queryFilter, childQueryFilter);
            childQuery[APIConstants.Query.FILTER] = childQueryFilter;

            childs.push(child);
        }
        var childQuery = child[APIConstants.Query.Childs.QUERY]
        var childColumns = childQuery[APIConstants.Query.COLUMNS] || [];
        if (secondPart) {
            if (reverseUnwindcolumns.indexOf(firstPart) == -1) {
                reverseUnwindcolumns.push(firstPart);
            }
            if (innerColumns) {
                var innerObject = {};
                innerObject[Constants.Baas.Tables.Columns.EXPRESSION] = secondPart;
                innerObject[Constants.Baas.Tables.Columns.PRIMARY_COLUMNS] = innerColumns;
                childColumns.push(innerObject);
            } else {
                childColumns.push(secondPart);
            }
        } else if (innerColumns) {
            childColumns.push.apply(childColumns, innerColumns);
        }
        childQuery[APIConstants.Query.COLUMNS] = childColumns;
        return true;
    }
    return false;
}

function removeFilters(expression, filter, removedFilters, samekey) {
    if (filter) {
        for (key in filter) {
            var indexOfDot = key.indexOf(expression + ".");
            if (indexOfDot != -1) {
                var secondFilterPart = key.substr(indexOfDot + expression.length + 1);
                removedFilters[samekey ? key : secondFilterPart] = filter[key];
                delete filter[key];
            }
        }
    }
}

function unwindReverseColumnData(reverseUnwindColumns, data) {
    var newData = [];
    var column = reverseUnwindColumns.pop();
    data.forEach(function (row) {
        var childData = row[column];
        delete row[column];
        if (childData) {
            childData.forEach(function (childRow) {
                var newRow = JSON.parse(JSON.stringify(row));
                newRow[column] = childRow;
                newData.push(newRow);
            });
        }
    });
    if (reverseUnwindColumns.length > 0) {
        return unwindReverseColumnData(reverseUnwindColumns, newData);
    }
    return newData;
}

function ensureColumnInQuery(relatedColumn, childQuery) {
    var columns = childQuery[APIConstants.Query.COLUMNS];
    if (columns) {
        var length = columns ? columns.length : 0;
        for (var i = 0; i < length; i++) {
            var column = columns[i];
            if (column instanceof Object) {
                if (column[APIConstants.Query.Columns.EXPRESSION]) {
                    var expression = column[APIConstants.Query.Columns.EXPRESSION];
                    if (expression == relatedColumn) {
                        return;
                    }
                }
            } else if (typeof(column) == "string" && column == relatedColumn) {
                return;
            }
        }
        columns.push(relatedColumn);
    }
}

function mergeQuery(query, viewQuery) {
    viewQuery = JSON.parse(JSON.stringify(viewQuery));
    var newQuery = JSON.parse(JSON.stringify(query));
    newQuery[APIConstants.Query.COLUMNS] = newQuery[APIConstants.Query.COLUMNS] || viewQuery[APIConstants.Query.COLUMNS];
    newQuery[APIConstants.Query.CHILDS] = newQuery[APIConstants.Query.CHILDS] || viewQuery[APIConstants.Query.CHILDS];
    newQuery[APIConstants.Query.FILTER] = mergeJSONObject(viewQuery[APIConstants.Query.FILTER], newQuery[APIConstants.Query.FILTER]);
    newQuery[APIConstants.Query.PARAMETERS] = mergeJSONObject(viewQuery[APIConstants.Query.PARAMETERS], newQuery[APIConstants.Query.PARAMETERS]);
    newQuery[APIConstants.Query.MODULE_PARAMETERS] = mergeJSONObject(viewQuery[APIConstants.Query.MODULE_PARAMETERS], newQuery[APIConstants.Query.MODULE_PARAMETERS]);
    newQuery[APIConstants.Query.ORDERS] = mergeJSONObject(viewQuery[APIConstants.Query.ORDERS], newQuery[APIConstants.Query.ORDERS]);
    newQuery[APIConstants.Query.GROUPS] = mergeJSONObject(viewQuery[APIConstants.Query.GROUPS], newQuery[APIConstants.Query.GROUPS]);
    return newQuery;
}

function mergeJSONObject(object1, object2) {
    if (!object1) {
        return  object2;
    }

    if (!object2) {
        return  object1;
    }

    for (key in object2) {
        object1[key] = object2[key];
    }
    return object1;
}

function mergeJSONArray(object1, object2) {
    if (!object1) {
        return  object2;
    }

    if (!object2) {
        return  object1;
    }
    object1.push.apply(object1, object2);
}

function populateRecursiveData(recursiveOrders, data) {
    if (data && data.length > 0) {
        var recursiveColumn = Object.keys(recursiveOrders)[0];
        var result = recursiveOrders[recursiveColumn][APIConstants.Query.Orders.RESULT] || APIConstants.Query.Orders.Result.LEVEL;
        var index = recursiveColumn.lastIndexOf(".");
        var parentColumn = index == -1 ? APIConstants.Query._ID : recursiveColumn.substring(0, index);
        var recColumn = index == -1 ? recursiveColumn : recursiveColumn.substring(index + 1);
        var dataMap = populateDataMap(data, parentColumn);
        var newData = [];
        data.forEach(function (row) {
            populateTree(row, parentColumn, recColumn, dataMap, newData);
        });
        if (result == APIConstants.Query.Orders.Result.LEVEL) {
            var levelData = [];
            populateTreeLevelResult(newData, levelData, 0);
            newData = levelData;
        }
        data = newData;
    }
    return data;
}

function populateTreeLevelResult(data, newData, level) {
    data.forEach(function (row) {
        row[APIConstants.Query.Result.Tree.LEVEL] = level;
        newData.push(row);
        if (row[APIConstants.Query.Result.Tree.CHILDREN]) {
            populateTreeLevelResult(row[APIConstants.Query.Result.Tree.CHILDREN], newData, level + 1);
            delete row[APIConstants.Query.Result.Tree.CHILDREN];
            row[APIConstants.Query.Result.Tree.MODE] = 1;
        } else {
            row[APIConstants.Query.Result.Tree.MODE] = -1;
        }
    });
}


function populateTree(row, parentColumn, recColumn, dataMap, newData) {
    var parentColumnValue = getColumnValue(row, parentColumn);
    var recursiveColumnValue = getColumnValue(row, recColumn);
    if (recursiveColumnValue) {
        recursiveColumnValue = recursiveColumnValue.toString();
        var parentRow = dataMap[recursiveColumnValue];
        if (parentRow) {
            var children = parentRow[APIConstants.Query.Result.Tree.CHILDREN] = parentRow[APIConstants.Query.Result.Tree.CHILDREN] || [];
            children.push(row);
        } else {
            var parentColumnValueObject = getColumnValue(row, parentColumn, true);
            var recursiveColumnValueObject = getColumnValue(row, recColumn, true);
            var newRow = (parentColumn == APIConstants.Query._ID) ? JSON.parse(JSON.stringify(recursiveColumnValueObject)) : {parentColumn:JSON.parse(JSON.stringify(parentColumnValueObject))};
            dataMap[getColumnValue(newRow, parentColumn).toString()] = newRow;
            populateTree(newRow, parentColumn, recColumn, dataMap, newData);
            populateTree(row, parentColumn, recColumn, dataMap, newData);
        }
    } else {
        newData.push(row);
    }
}

function populateDataMap(data, keyColumn) {
    var dataMap = {};
    data.forEach(function (row) {
        var keyValue = getColumnValue(row, keyColumn);
        keyValue = keyValue ? keyValue.toString() : keyValue;
        dataMap[keyValue] = row;
    });
    return dataMap;
}

