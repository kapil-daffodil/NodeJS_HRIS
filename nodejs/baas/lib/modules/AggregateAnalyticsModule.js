function transform() {
    var aggregateTable = this._context.params["aggregate table"];
    var selfRelationColumn = this._context.params["self relation column"];
    var selfAmountColumn = this._context.params["self amount column"];
    var selfDateColumn = this._context.params["self date column"];
    var aggregateRelationColumn = this._context.params["aggregate relation column"];
    var aggregateAmountColumn = this._context.params["aggregate amount column"];
    var aggregateTypeColumn = this._context.params["aggregate type column"];
    var aggregateYearColumn = this._context.params["aggregate year column"];
    var aggregateMonthColumn = this._context.params["aggregate  month column"];
    var aggregateDayColumn = this._context.params["aggregate day column"];
    var monthNames = [ "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december" ];
    var updates = [];

    if (this._operation == 'insert' || this._operation == 'update') {
        prepareUpdates(updates, this);
    }

    if (this._operation == 'update') {
        prepareUpdates(updates, this._old, true);
    }

    if (this._operation == 'delete') {
        prepareUpdates(updates, this, true);
    }
    writeLog("update are --**" + JSON.stringify(updates));
    var finalUpdates = {};
    finalUpdates[aggregateTable] = updates;
    return finalUpdates;
}

function prepareUpdates(updates, object, toggle) {
    var values = getValues([selfRelationColumn, selfAmountColumn], object);
    writeLog("accounts and amount JSONArray --  " + JSON.stringify(values));
    var dateValues = getValues([selfDateColumn], object);
    writeLog("dates JSONArray --  " + JSON.stringify(dateValues));
    var length = values.length;
    var dateLength = dateValues.length;
    for (var i = 0; i < length; i++) {
        var value = values[i];
        if (dateLength > 1) {
            for (var j = 0; j < dateLength; j++) {
                var dateValue = dateValues[j];
                var date = dateValue[0];
                prepareUpdate(updates, value[0], (toggle ? -1 * value[1] : value[1]), date);
            }
        } else {
            prepareUpdate(updates, value[0], (toggle ? -1 * value[1] : value[1]), dateValues[0]);
        }

    }
}

function getValues(columns, object) {
    var selfRelationColumn = columns[0];
    if (selfRelationColumn.indexOf(".") == -1) {
        var noOfColumns = columns.length;
        var value = [];
        for (var i = 0; i < noOfColumns; i++) {
            var exp = columns[i];
            value.push(object[exp]);
        }
        return value;
    } else {
        var beforeDotExpression = selfRelationColumn.substring(0, selfRelationColumn.indexOf("."));
        var values = [];
        var expresssionValue = object[beforeDotExpression];
        if (expresssionValue instanceof Array) {
            for (var i = 0; i < expresssionValue.length; i++) {
                values.push(getValues(getAfterDotExpression(columns), expresssionValue[i]));
            }
            return values;
        } else {
            getValues(getAfterDotExpression(columns), expresssionValue);
            return;
        }
    }
}


function getAfterDotExpression(columns) {
    var noOfColumns = columns.length;
    var value = [];
    for (var i = 0; i < noOfColumns; i++) {
        var expression = columns[i];
        value.push(expression.substring(expression.indexOf(".") + 1, expression.length));
    }
    return value;
}


function prepareUpdate(updates, account, amount, date) {
    if (typeof date == "string") {
        date = new Date(date);
    }

    var value = {};
    value[aggregateAmountColumn] = amount;

    //type :day
    var update = {};
    var id = {};
    id[aggregateRelationColumn] = account;
    id[aggregateTypeColumn] = "day";
    id[aggregateDayColumn] = date;
    update.id = id;
    update.value = {$inc:value};
    updates.push(update);

    //type:month
    var update = {};
    var id = {};
    id[aggregateRelationColumn] = account;
    id[aggregateTypeColumn] = "month";
    id[aggregateMonthColumn] = monthNames[date.getMonth() + "-" + date.getFullYear()];
    update.id = id;
    update.value = {$inc:value};
    updates.push(update);

    //type:year
    var update = {};
    var id = {};
    id[aggregateRelationColumn] = account;
    id[aggregateTypeColumn] = "year";
    id[aggregateYearColumn] = date.getFullYear();
    update.id = id;
    update.value = {$inc:value};
    updates.push(update);
}


//update code--------------------------------------------------------------------------------------------------------------------------------------------------------

function update() {
    var aggregateTable = this._context.params["aggregate table"];
    var aggregateColumn = this._context.params["aggregate column"];
    var aggregateRelationColumn = this._context.params["aggregate relation column"];
    var aggregateAmountColumn = this._context.params["aggregate amount column"];
    var aggregateTypeColumn = this._context.params["aggregate type column"];
    var aggregateYearColumn = this._context.params["aggregate year column"];
    var aggregateMonthColumn = this._context.params["aggregate  month column"];
    var aggregateDayColumn = this._context.params["aggregate day column"];

    var query = this._query;
    var tableName = query.table;
    var columns = query.columns;
    var filter = query.filter;
    delete query.filter;

    if (columns instanceof Array) {
        for (var key in columns) {
            var column = columns[key];
            var columnExpression;
            if (column instanceof Object) {
                columnExpression = column.expression;
            } else {
                columnExpression = column;
            }
            if (columnExpression.toString() == aggregateColumn) {
                columns.splice(key, 1);
                var subQuery = createSubQuery(filter);
                columns.push({aggregateColumn:subQuery});
            }
        }
    }
}

function createSubQuery(filter) {
    var query = {};
    query.table = aggregateTable;
    query.columns = [
        {"expression":aggregateColumn, "aggregate":"sum"}
    ];
    var newFilter;
    if (filter instanceof Object) {
        var dateSpan = filter.aggregateColumn;
        var years = [];
        var months = [];
        var dates = [];
        createDateFilter(dateSpan.$gte, dateSpan.$lt, years, months, dates);
        newFilter = {$or:[
            {day:{$in:dates}},
            {year:{$in:yeras}},
            {month:{$in:months}}
        ]};
    } else {
        newFilter = {day:day};
    }
    query.filter = newFilter;
    if (aggregateRelationColumn.indexOf(".") != -1) {
        query.relatedcolumn = aggregateRelationColumn;
        query.parentcolumn = aggregateRelationColumn.substring(0, aggregateRelationColumn.lastIndexOf("."));
    } else {
        query.relatedcolumn = aggregateRelationColumn;
        query.parentcolumn = _id;
    }
    return query;
}

function createDateFilter(fromDate, toDate, years, months, dates) {
    var monthNames = [ "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december" ];
    var fromYear = fromDate.getFullYear();
    var toYear = toDate.getFullYear();
    if ((toYear - fromYear) > 1) {
        for (i = fromYear + 1; i < toYear; i++) {
            years.push(i);
        }
        getMonths(fromDate, getLastDayOfYear(fromDate), months, dates, fromDate.getFullYear());
        getMonths(getFirstDayOfYear(toDate), toDate, months, dates, toDate.getFullYear());
    } else if ((toYear - fromYear) == 1) {
        getMonths(fromDate, getLastDayOfYear(fromDate), months, dates, fromDate.getFullYear());
        getMonths(getFirstDayOfYear(toDate), toDate, months, dates, toDate.getFullYear());
    } else {
        getMonths(fromDate, toDate, months, dates, fromDate.getFullYear());
    }

    function getMonths(fromDate, toDate, months, dates, year) {
        var fromMonth = fromDate.getMonth();
        var toMonth = toDate.getMonth();
        if ((toMonth - fromMonth) > 1) {
            var lastDay = getLastDayOfMonth(toDate);
            if (fromDate.getDate() == 1 && toDate.getDate() == lastDay) {
                for (i = fromMonth; i <= toMonth; i++) {
                    months.push(monthNames[i] + "-" + year);
                }
            } else if (fromDate.getDate() == 1 && toDate.getDate() != lastDay) {
                for (i = fromMonth; i < toMonth; i++) {
                    months.push(monthNames[i] + "-" + year);
                }
                getDaysofMonths(getFirstDateOfMonth(toDate), toDate, dates);
            } else if (fromDate.getDate() != 1 && toDate.getDate() == lastDay) {
                for (i = fromMonth + 1; i <= toMonth; i++) {
                    months.push(monthNames[i] + "-" + year);
                }
                getDaysofMonths(fromDate, getLastDateOfMonth(fromDate), dates);
            } else {
                for (i = fromMonth + 1; i < toMonth; i++) {
                    months.push(monthNames[i] + "-" + year);
                }
                getDaysofMonths(fromDate, toDate, dates);
            }
        } else {
            getDaysofMonths(fromDate, toDate, dates);
        }
    }

    function getDaysofMonths(fromDate, toDate, dates) {

        dates.push({$gte:fromDate, $lte:toDate});
    }

    function getLastDayOfYear(date) {
        var y = date.getFullYear();
        return new Date(y, 11 + 1, 0);
    }

    function getFirstDayOfYear(date) {
        var y = date.getFullYear();
        return new Date(y, 0, 1);
    }

    function getFirstDayOfMonth(date) {
        var y = date.getFullYear();
        var m = date.getMonth();
        var firstDate = new Date(y, m, 1);
        return firstDate.getDate();
    }

    function getLastDayOfMonth(date) {
        var y = date.getFullYear();
        var m = date.getMonth();
        var lastDate = new Date(y, m + 1, 0);
        return lastDate.getDate();
    }

    function getFirstDateOfMonth(date) {
        var y = date.getFullYear();
        var m = date.getMonth();
        return new Date(y, m, 1);
    }

    function getLastDateOfMonth(date) {
        var y = date.getFullYear();
        var m = date.getMonth();
        return  new Date(y, m + 1, 0);
    }
}