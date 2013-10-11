//TODO
//getTempKey, use of appUtil
//get column update, check for temp key for delete
var tempKey = 0;
var tempComponents = 0;
function DataModel() {

}

DataModel.prototype.init = function ($scope, $data, $query) {
    this.$scope = $scope;
    this.$data = $data;
    this.$query = $query;

    var model = this[id];
    if (!model) {
        model = {};
        this[id] = model;
    }

    if (!$query.primarycolumn) {
        $query.primarycolumn = KEY;
    }


    this.dataClone = angular.copy(this.$data);
    this.$selectedKeys = [];


    if (!query.filterparameters) {
        query.filterparameters = {};
    }

    this.populateDataState();

};

DataModel.prototype.moveUp = function (row) {
    try {
        //check index column if exists
        var columns = this.$query.columns;
        var columnCount = columns ? columns.length : 0;
        var indexColumn;
        for (var i = 0; i < columnCount; i++) {
            if (columns[i].type == 'index') {
                indexColumn = columns[i];
            }
        }
        if (!indexColumn) {
            alert("Index column is not defined")
            throw Error("Index column is not defined");
        }

        var expression = indexColumn.expression;
        var indexExp = expression + ".index";
        var subIndexExp = expression + ".subindex";
        var indexValue = AppUtil.resolveDot(row, indexExp);
        var subIndexValue = AppUtil.resolveDot(row, subIndexExp);

        var key = AppUtil.resolveDot(row, this.$query.primarycolumn);
        var index = this.getIndex(key, this.$query.primarycolumn);
        var prevIndex = index - 1;

        var currentRow = this.data[index];

        if (prevIndex >= 0) {
            var previous = this.data[prevIndex];
            var preIndexValue = AppUtil.resolveDot(previous, indexExp);
            var preSubIndexValue = AppUtil.resolveDot(previous, subIndexExp);
            if (angular.equals(preIndexValue, indexValue)) {
                AppUtil.putDottedValue(row, subIndexExp, preSubIndexValue);
                AppUtil.putDottedValue(previous, subIndexExp, subIndexValue);
            } else {
                AppUtil.putDottedValue(row, indexExp, preIndexValue);
                AppUtil.putDottedValue(previous, indexExp, indexValue);
            }

            this.data[index] = previous;
            this.data[prevIndex] = currentRow;
        }
    } catch (e) {
        alert(e);
    }

}
DataModel.prototype.moveDown = function (row) {

    var columns = this.$query.columns;
    var columnCount = columns.length;
    var indexColumn;
    for (var i = 0; i < columnCount; i++) {
        if (columns[i].type == 'index') {
            indexColumn = columns[i];
        }
    }
    if (!indexColumn) {
        alert("Index column is not defined")
        throw Error("Index column is not defined");
    }

    var expression = indexColumn.expression;
    var indexExp = expression + ".index";
    var subIndexExp = expression + ".subindex";
    var indexValue = AppUtil.resolveDot(row, indexExp);
    var subIndexValue = AppUtil.resolveDot(row, subIndexExp);

    var key = AppUtil.resolveDot(row, this.$query.primarycolumn);
    var index = this.getIndex(key, this.$query.primarycolumn);
    var nextIndex = index + 1;


    var currentRow = this.data[index];

    if (nextIndex < this.data.length) {
        var previous = this.data[nextIndex];
        var preIndexValue = AppUtil.resolveDot(previous, indexExp);
        var preSubIndexValue = AppUtil.resolveDot(previous, subIndexExp);
        if (angular.equals(preIndexValue, indexValue)) {
            AppUtil.putDottedValue(row, subIndexExp, preSubIndexValue);
            AppUtil.putDottedValue(previous, subIndexExp, subIndexValue);
        } else {
            AppUtil.putDottedValue(row, indexExp, preIndexValue);
            AppUtil.putDottedValue(previous, indexExp, indexValue);
        }

        this.data[index] = previous;
        this.data[nextIndex] = currentRow;
    }

}
DataModel.prototype.getIndex = function (key, primaryColumn) {


    var dataCount = this.data.length;
    for (var i = 0; i < dataCount; i++) {
        var record = this.data[i];
        var recordKey = AppUtil.resolveDot(record, primaryColumn);
        var isEqual = angular.equals(recordKey, key);
        if (isEqual) {
            return i;
        }
    }
    return -1;
}

DataModel.getComponentId = function () {
    tempComponents = tempComponents + 1;
    return COMPONENT_ID_KEY + tempComponents;
}
DataModel.getTempKey = function (primaryColumn) {
    tempKey = tempKey + 1;
    var temp = {};
    temp[primaryColumn] = tempKey + "temp";
    return temp;

}

DataModel.prototype.addWatch = function () {
    var that = this;
    this.scope.$watch(this.data, function (newvalue, oldvalue) {
        if (!angular.equals(newvalue, oldvalue)) {
            var columns = that.$query.columns;
            var totalAggregates = that.$data.totalaggregateresult;
            var columnCount = columns.length;
            var updates = that.getColumnUpdates(newvalue, oldvalue, columns, that.$query.primarycolumns, true);
            var updateCount = updates ? updates.length : 0;

            for (var i = 0; i < updateCount; i++) {
                var updatedRow = updates[i];
                for (var j = 0; j < columnCount; j++) {
                    var column = columns[j];
                    var type = column.type;
                    var expression = column.expression;
                    var oldExpression = "old_" + expression;
                    var totalAggregate = column.totalaggregates;
                    var oldColumnValue = AppUtil.resolveDot(updatedRow, oldExpression);
                    var newColumnvalue = AppUtil.resolveDot(updatedRow, expression);
                    if (newColumnvalue) {

                        if (totalAggregates && totalAggregate && totalAggregate == "sum") {

                            var conversionRate = scope.appData.conversionrate[type];
                            var aggregateColumnValue = AppUtil.resolveDot(totalAggregates, expression);

                            var valExp;
                            var typeExp;
                            var mainTypeExp;
                            if (type == "duration") {
                                valExp = "time";
                                typeExp = "timeunit";
                                mainTypeExp = "timeunit";
                            } else if (type == "currency") {
                                valExp = "amount";
                                typeExp = "type.currency";
                                mainTypeExp = "type";
                            }
                            var oldVal = AppUtil.resolveDot(oldColumnValue, valExp);
                            var oldType = AppUtil.resolveDot(oldColumnValue, typeExp);
                            var newVal = AppUtil.resolveDot(newColumnvalue, valExp);
                            var newType = AppUtil.resolveDot(newColumnvalue, typeExp);
                            var aggregateVal = AppUtil.resolveDot(aggregateColumnValue, valExp);
                            var aggregateType = conversionRate.baseunit;
                            var oldConversionRate = conversionRate.conversionvalue[oldType];
                            var newConversionRate = conversionRate.conversionvalue[newType];
                            var leafValue = conversionRate.leafunit;

                            if (!aggregateVal) {
                                aggregateVal = 0;
                            } else {
                                aggregateVal = aggregateVal * leafValue;
                            }
                            if (!oldVal || !oldConversionRate) {
                                oldVal = 0;
                            } else {
                                oldVal = (oldVal * leafValue) / oldConversionRate;
                            }
                            if (!newVal || !newConversionRate) {
                                newVal = 0;
                            } else {
                                newVal = (newVal * leafValue) / newConversionRate;
                            }


                            aggregateVal = aggregateVal + newVal - oldVal;
                            aggregateVal = aggregateVal / leafValue;
                            aggregateVal = aggregateVal.toFixed(2);
                            var newAggregateVal = {};
                            newAggregateVal[mainTypeExp] = aggregateType;
                            newAggregateVal[valExp] = aggregateVal;
                            AppUtil.putDottedValue(totalAggregates, expression, newAggregateVal);

                        }
                    }


                }


            }

        }
    }, true);

}


DataModel.prototype.populateDataState = function () {


    var dataCount = this.$data ? this.$data.length : 0;

    var maxRow = this.$query.max_rows;

    var dataState = this.dataState;
    if (!dataState) {
        dataState = {fromindex:0, toindex:0, prev:false, next:false, querycursor:metadata.cursor, datacursor:data.cursor}
        this.dataState = dataState;
    }

    var queryCursor = dataState.querycursor;
    if (!queryCursor) {
        queryCursor = 0;
    }

    var dataCursor = data.cursor;
    if (!dataCursor) {
        dataCursor = 0;
    }


    var fromIndex = dataState.fromindex;
    var toIndex = dataState.toindex;
    var prev = dataState.prev;
    var next = dataState.next;

    if (dataCount > 0) {
        fromIndex = queryCursor + 1;
        toIndex = fromIndex - 1 + dataCount;

        if (queryCursor == 0) {
            prev = false
        } else {
            prev = true;
        }
        if (dataCursor) {
            next = true;
        } else {
            next = false;
        }

    } else {
        prev = false;
        next = false;
    }

    dataState.next = next;
    dataState.prev = prev;
    dataState.fromindex = fromIndex;
    dataState.toindex = toIndex;
    dataState.querycursor = queryCursor;
    dataState.datacursor = dataCursor;
}
DataModel.prototype.next = function () {

    this.dataState.querycursor = this.dataState.querycursor + this.dataState.$query.max_rows;
    this.refresh();
}
DataModel.prev = function (componentid) {
    this.dataState.querycursor = this.dataState.querycursor - this.dataState.$query.max_rows;
    this.refresh();
}

DataModel.prototype.setCurrentRow = function (componentid, row) {
    var model = this[componentid];
    model.view.currentrow = row;
};
DataModel.prototype.getCurrentRow = function (componentid) {
    var model = this[componentid];
    return model.view.currentrow;
};
DataModel.prototype.getSelectedKeys = function (componentid) {
    var model = this[componentid];
    var selectedKeys = model.view.selectedkeys;
    if (selectedKeys == null || selectedKeys == undefined) {
        alert("selectedKeys does not exists for componentid [" + componentid + "]");
    }
    return selectedKeys;
}


DataModel.prototype.refresh = function (componentid, refreshCallback) {

    var model = this.getModel(componentid);

    if (model == null || model == undefined) {
        throw "No model found for refresh[" + componentid + "]";
    }

    var view = model.view;
    var ask = view.ask;
    var osk = view.osk;

    var that = this;
    var viewType = view.metadata.type;
    var parameters = view.metadata.parameters;
    var columns;
    var cursor = view.data.cursor;


    if (viewType == 'table') {
        columns = [];
        var viewColumns = view.metadata.columnsclone;
        var viewColumnCount = viewColumns ? viewColumns.length : 0;
        for (var i = 0; i < viewColumnCount; i++) {
            var viewColumn = viewColumns[i];
            if (viewColumn.visibility == 'Table' || viewColumn.visibility == 'Both' || viewColumn.visibility == 'Query') {
                if (viewColumn.subvisibility && viewColumn.subvisibility == 'Embed') {
                    continue;
                }
                columns.push(viewColumn);
            }

        }
    } else if (viewType == 'panel') {
        columns = [];
        var viewColumns = view.metadata.columnsclone;
        var viewColumnCount = viewColumns ? viewColumns.length : 0;
        for (var i = 0; i < viewColumnCount; i++) {
            var viewColumn = viewColumns[i];
            if (viewColumn.visibility == 'Panel') {
                if (viewColumn.subvisibility && viewColumn.subvisibility == 'Embed') {
                    continue;
                }
                columns.push(viewColumn);
            }

        }
    }

    var callBack = function (response) {
        try {
            var parentComponentId = view[PARENT_COMPONENT_ID];


            if (viewType == "panel") {
                //We have to merge the data in table
                //get Old Record
                var parentView = that.getModel(parentComponentId).view;
                var parentData = parentView.data.data;
                var parentDataClone = parentView.dataclone;
                var currentData = view.data.data[0];
                var parentRecord = getRecord(parentData, AppUtil.resolveDot(currentData, view.metadata[PRIMARY_COLUMN]), view.metadata[PRIMARY_COLUMN]);

                if (!parentRecord) {
                    throw new Error("Parent record not found");
                }
                var parentRecordClone = getRecord(parentDataClone, AppUtil.resolveDot(currentData, view.metadata[PRIMARY_COLUMN]), view.metadata[PRIMARY_COLUMN]);
                if (!parentRecordClone) {
                    throw new Error("Parent record clone not found");
                }
                if (response && response.data && response.data.length > 0) {
                    var panelData = response.data[0];
                    var tempClonecolumns = angular.copy(columns);
                    that.updateColumnMetadata(componentid, tempClonecolumns);


                    for (var i = 0; i < columns.length; i++) {
                        var column = tempClonecolumns[i];
                        var parentExpData = AppUtil.resolveDot(panelData, column.expression);
                        if (parentExpData && angular.isArray(parentExpData)) {
                            var oldRecord = AppUtil.resolveDot(parentRecord, column.expression);
                            if (oldRecord) {
                                oldRecord.splice(0);
                            } else {
                                oldRecord = [];
                                AppUtil.putDottedValue(parentRecord, column.expression, oldRecord)
                            }

                            for (var j = 0; j < parentExpData.length; j++) {
                                oldRecord.push(parentExpData[j]);
                }
                        } else {
                            if (parentExpData !== undefined) {
                                AppUtil.putDottedValue(parentRecord, column.expression, parentExpData)
                            }

                        }
                        AppUtil.putDottedValue(parentRecordClone, column.expression, angular.copy(parentExpData))

                    }
                }


            } else {
                view.data = response;
                var records = view.data.data;
                var recordCount = records ? records.length : 0;

                view.dataclone = angular.copy(records);
            }

            that.populateDataState(componentid);

            if (refreshCallback) {
                refreshCallback(response.data);
            }

            if (!model.scope.$$phase) {
                model.scope.$apply();
            }
        } catch (e) {
            alert(e.message);
        }
    };

    if (columns.length > 0) {
        var query = {"keepstructure":true, "table":view.metadata.table, "columns":columns, "filter":view.metadata.filter, "parameters":parameters};
        if (view.metadata.max_rows !== undefined) {
            query.max_rows = view.metadata.max_rows;
        }

        if (view.metadata.orders != null && view.metadata.orders != undefined) {
            query.orders = view.metadata.orders;
        }

        query.cursor = view.metadata.datastate.querycursor;


        $appService.getData(query, ask, osk, "Loading...", callBack);
    } else {
        callBack();
    }


}

DataModel.prototype.insert = function (componentid) {
    var model = this[componentid];
    var tempKey = this.getTempKey(model.view.metadata[PRIMARY_COLUMN]);
    var data = model.view.data.data;
    data.splice(0, 0, tempKey);
    model.view.dataclone.splice(0, 0, angular.copy(tempKey));
}

DataModel.prototype.deleteData = function (componentid) {
    var model = this[componentid];
    var view = model.view;
    var data = view.data.data;
    var primaryColumn = view.metadata[PRIMARY_COLUMN];

    var selectedItems = this.getSelectedKeys(componentid);

    var selectedCount = selectedItems.length;
    if (selectedCount > 0) {
        var deletedItems = [];

        for (var i = 0; i < selectedCount; i++) {
            var selectedItem = selectedItems[i];
            var key = AppUtil.resolveDot(selectedItem, primaryColumn);
            var dataIndex = $dataModel.getIndex(key, componentid, primaryColumn);
            if (dataIndex < 0) {
                alert("No row exists for delete");
                throw new Error("No row exists for delete");
            }
            data.splice(dataIndex, 1);
            var deletedOperation = {};
            deletedOperation[primaryColumn] = key;
            var dottedIndex = primaryColumn.lastIndexOf(".");
            var operationPrefix = "";
            if (dottedIndex >= 0) {
                operationPrefix = primaryColumn.substring(0, dottedIndex) + ".";
            }
            operationPrefix += "__type__";
            deletedOperation[operationPrefix] = "delete";
            deletedItems.push(deletedOperation);
        }

        //empty selected items
        selectedItems.splice(0);
        if (view.metadata.embed) {
            if (!model.scope.$$phase) {
                model.scope.$apply();
            }
        } else {
            var that = this;
            var callBack = function (data) {
                that.refresh(componentid);
            };
            $appService.save({"table":view.metadata.table, "filter":view.metadata.filter, "parameters":view.metadata.parameters, "operations":deletedItems}, view.ask, view.osk, callBack);
        }

    } else {
        alert("No row found for delete");
    }
}

$dataModel.isNullOrUndefined = function (obj) {
    return (obj === undefined || obj === null) ? true : false;

}

$dataModel.isTrueOrUndefined = function (obj) {
    return (obj === undefined || obj === null || obj == true) ? true : false;

}


$dataModel.addFilter = function (componentId, pFilterExp, pParamValue) {
    var model = this.getModel(componentId);
    var view = model.view;
    var metadata = view.metadata;
    var filter = metadata.filter;

    if (this.isNullOrUndefined(filter)) {
        filter = {};
    }

    metadata.filter = filter;
    if (pParamValue === undefined) {
        delete filter[pFilterExp];
    } else {
        filter[pFilterExp] = pParamValue;
    }


};

$dataModel.isTempKey = function (id) {
    var dottedPattern = /.+(temp)$/;
    return (dottedPattern.test(id));
}

$dataModel.save = function (componentid, saveCallBack) {


    var model = this.getModel(componentid);
    var view = model.view;
    var ask = view.ask;
    var osk = view.osk;

    var newInserts = $dataModel.getUpdates(componentid);

    var newInsertsCount = newInserts.length;
    if (newInsertsCount > 0) {

        var that = this;
        var callBack = function (data) {

            that.refresh(componentid, function (data) {
                if (saveCallBack) {
                    saveCallBack(data);
                }
            });
        };
        $appService.save({"table":view.metadata.table, "filter":view.metadata.filter, "parameters":view.metadata.parameters, "operations":newInserts}, ask, osk, callBack);
    } else {
        alert("No data found for saving");
    }

}

$dataModel.getColumnUpdates = function (data, cloneData, columns, primaryColumn, requireOldValue) {
    if (!primaryColumn) {
        throw new Error("Primary column not defined");
    }


    var noOfColumns = columns.length;

    var newInserts = [];
    var noOfDataRecords = data.length;
    for (var i = 0; i < noOfDataRecords; i++) {
        var record = data[i];
        var key = AppUtil.resolveDot(record, primaryColumn);
        var cloneRecord = getRecord(cloneData, key, primaryColumn);
        var hasUpdates = false;

        if (cloneRecord == null) {
            // case of insert
            cloneRecord = {};
        }
        var updates = {};

        for (var j = 0; j < noOfColumns; j++) {
            var column = columns[j];
            var expression = column.expression;
            var type = column.type;
            var multiple = column.multiple;


            var oldValue = AppUtil.resolveDot(cloneRecord, expression);
            var newValue = AppUtil.resolveDot(record, expression);

            if (!angular.equals(oldValue, newValue)) {
                //clone newValue so that changes in this will nt reflect in data
                if (newValue) {
                    newValue = angular.copy(newValue);
                }


                if (type == LOOK_UP_TYPE) {

                    if (multiple) {
                        newValue = {'data':newValue, "override":true};
                    }
                } else if (type == STRING_TYPE && column.multiple) {
                    if (!(newValue instanceof Array)) {
                        if (newValue.toString().trim().length > 0) {
                            newValue = JSON.parse(newValue.toString().trim());
                        } else {
                            newValue = undefined
                        }

                    }

                } else if (type == DATE_TYPE || type == DATE_TIME_TYPE) {
                    if (newValue.indexOf("/") >= 0) {
                        var splitValue = newValue.split("/")
                        newValue = splitValue[2] + "-" + splitValue[1] + "-" + splitValue[0];
                    }

                } else if (type == SCHEDULE_TYPE) {
                    var dueDate = AppUtil.resolveDot(newValue, "duedate");
                    if (dueDate && dueDate.indexOf("/") >= 0) {
                        var splitValue = dueDate.split("/")
                        dueDate = splitValue[2] + "-" + splitValue[1] + "-" + splitValue[0];
                        AppUtil.putDottedValue(newValue, "duedate", dueDate);
                    }

                } else if (type == 'object') {
                    if (newValue) {
                        if (multiple && angular.isArray(newValue)) {
                            newValue = $dataModel.getColumnUpdates(newValue, oldValue, column.columns, KEY)

                        } else if (!(angular.isArray(newValue) || angular.isObject(newValue)) && newValue.toString().trim().length > 0) {
                            newValue = JSON.parse(newValue);
                        }
                        //check if newValue and oldValue are same
                        if (angular.equals(newValue, oldValue)) {
                            newValue = undefined;
                        }
                    }
                }

                if (newValue !== undefined) {
                    hasUpdates = true;
                    updates[expression] = newValue;
                    if (requireOldValue) {
                        updates["old_" + expression] = oldValue;
                    }
                    //check for dotted expression
                    var dottexExpression = expression;
                    var dottIndex = dottexExpression.indexOf(".");
                    var dottedConcatExpression = "";
                    while (dottIndex >= 0) {
                        //we have to send _id value along with updates
                        var firstExpression = dottexExpression.substring(0, dottIndex);
                        var firstIndex = dottedConcatExpression + firstExpression + "._id";

                        var updatedFirstIndex = AppUtil.resolveDot(updates, firstIndex);
                        var oldRecordFirstIndex = AppUtil.resolveDot(cloneRecord, firstIndex);
                        var currentRecordFirstIndex = AppUtil.resolveDot(record, firstIndex);

                        if (!updatedFirstIndex) {
                            if (currentRecordFirstIndex) {
                                updates[firstIndex] = currentRecordFirstIndex;
                            } else if (oldRecordFirstIndex) {
                                updates[firstIndex] = oldRecordFirstIndex;
                            }
                        }
                        dottexExpression = dottexExpression.substring(dottIndex + 1);
                        dottIndex = dottexExpression.indexOf(".");
                        dottedConcatExpression = dottedConcatExpression + firstExpression + ".";

                    }
                }
            }
        }


        if (hasUpdates) {
            updates[primaryColumn] = key;
            var primaryColumnValue = updates[primaryColumn];
            if (primaryColumnValue && /.+(temp)$/.test(primaryColumnValue)) {
                delete updates[primaryColumn];
                updates[primaryColumn + "__temp"] = primaryColumnValue;
            }
            newInserts.push(updates);
        }

    }

    //check for delete
    if (cloneData) {
        var cloneRecordsCount = cloneData.length;
        for (var i = 0; i < cloneRecordsCount; i++) {
            var record = cloneData[i];
            var key = AppUtil.resolveDot(record, primaryColumn);
            var dataRecord = getRecord(data, key, primaryColumn);
            if (dataRecord == null) {
                //case of delete
                var deletedOperation = {};
                deletedOperation[primaryColumn] = key;
                var dottedIndex = primaryColumn.lastIndexOf(".");
                var operationPrefix = "";
                if (dottedIndex >= 0) {
                    operationPrefix = primaryColumn.substring(0, dottedIndex) + ".";
                }
                operationPrefix += "__type__";
                deletedOperation[operationPrefix] = "delete";
                newInserts.push(deletedOperation);
            }

        }
    }

    return newInserts;

}

$dataModel.getUpdates = function (componentid) {
    var model = this.getModel(componentid);
    var view = model.view;


    var cloneData = view.dataclone;
    var data = view.data.data;

    var primaryColumn = view.metadata[PRIMARY_COLUMN];

    var columns = model.view.metadata.columnsclone;
    return $dataModel.getColumnUpdates(data, cloneData, columns, primaryColumn);
}

function getRecord(data, pKey, primaryColumn) {
    var dataCount = data ? data.length : 0;
    for (var i = 0; i < dataCount; i++) {
        var record = data[i];
        var recordKey = AppUtil.resolveDot(record, primaryColumn);
        var isEqual = angular.equals(recordKey, pKey);
        if (isEqual) {
            return record;
        }
    }
    return null;
}

$dataModel.updateColumnMetadata = function (componentid, columns) {
    if (!columns) {
        var model = this.getModel(componentid);
        columns = model.view.metadata.columns;
    }

    var columnsLength = columns ? columns.length : 0;

    for (var i = 0; i < columnsLength; i++) {
        var column = columns[i];
        var type = column.type;
        var exp = column.expression;

        if (type == LOOK_UP_TYPE && column.primarycolumns) {
            var lookupColumnCount = column.primarycolumns.length;
            var newLookupColumns = [];
            for (var j = 0; j < lookupColumnCount; j++) {
                var c = column.primarycolumns[j];
                if (c.type == 'lookup') {
                    continue;
                }
                newLookupColumns.push(c);
            }
            if (newLookupColumns.length == 0) {
                alert("No simple column exists in [" + column.expression + "]");
                throw new Error("No simple column exists in [" + column.expression + "]");
            }
            column.primarycolumns = newLookupColumns;

        } else if (type == 'view' && column.columns) {
            this.updateColumnMetadata(componentid, column.columns);
        }
    }
}



