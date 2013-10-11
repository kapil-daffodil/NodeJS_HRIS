/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 21/8/13
 * Time: 6:40 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../shared/Constants.js');
var Weeks = {"Sun":0, "Mon":1, "Tue":2, "Wed":3, "Thu":4, "Fri":5, "Sat":6};
var Days = {"1":1, "2":2, "3":3, "4":4, "5":5, "6":6, "7":7, "8":8, "9":9, "10":10, "11":11, "12":12, "13":13, "14":14, "15":15, "16":16, "17":17, "18":18, "19":19, "20":20, "21":21, "22":22, "23":23, "24":24, "25":25, "26":26, "27":27, "28":28, "29":29, "30":30, "31":31};
var Months = {"Jan":0, "Feb":1, "Mar":2, "Apr":3, "May":4, "Jun":5, "Jul":6, "Aug":7, "Sep":8, "Oct":9, "Nov":10, "Dec":11};


exports.beforeQuery = function (table, query, context, callback) {
    var systemColumns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
    ensureColumns(systemColumns);
    callback();
}

exports.beforeInsert = function (table, operations, context, callback) {
    var systemColumns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
    ensureColumns(systemColumns);
    var columns = table[Constants.Baas.Tables.COLUMNS] || [];
    updateDueDate(columns, operations);
    callback();
}

exports.beforeUpdate = function (table, operations, context, callback) {
    var systemColumns = table[Constants.Baas.Tables.SYSTEM_COLUMNS] || [];
    ensureColumns(systemColumns);
    var columns = table[Constants.Baas.Tables.COLUMNS] || [];
    updateDueDate(columns, operations);
    callback();
}

function updateDueDate(columns, operations) {
    for (var i = 0; i < operations.length; i++) {
        var operation = operations[i];
        for (var k = 0; k < columns.length; k++) {
            var columnInfo = columns[k];
            var columnExpression = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
            var columnValues = operation[columnExpression];
            var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
            if (columnType == Constants.Baas.Tables.Columns.Type.SCHEDULE) {
                if (columnValues && columnValues instanceof Object && Object.keys(columnValues).length > 0) {
                    var span = columnValues[Constants.Baas.Tables.Columns.Type.Schedule.SPAN];
                    if (!span || span.trim().length == 0 || span == Constants.Baas.Tables.Columns.Type.Schedule.NONE) {
                        continue;
                    }
                    columnValues[Constants.Baas.Tables.Columns.Type.Schedule.REPEAT] = true;
                    var dueDate = columnValues[Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE];
                    var repeatedOn = columnValues[Constants.Baas.Tables.Columns.Type.Schedule.REPEATED_ON] || [];
                    repeatedOn = sortRepeatedOn(repeatedOn, span);
                    var time = columnValues[Constants.Baas.Tables.Columns.Type.Schedule.TIME] || "00:00 AM";
                    var timeObj = {};
                    timeObj.hrs = parseInt(time.substr(0, time.indexOf(":")));
                    timeObj.mins = parseInt(time.substr(time.indexOf(":") + 1, time.indexOf(" ")));
                    var timeUnit = time.substr(time.indexOf(" ") + 1);
                    if (timeUnit == "PM" && timeObj.hrs < 12) {
                        timeObj.hrs = timeObj.hrs + 12;
                    }
                    var scheduleTimeInMins = (timeObj.hrs * 60) + timeObj.mins;

                    var date = new Date();
                    var defaultTimeZoneInMs = -19800000;
                    var currentTimeZoneInMs = date.getTimezoneOffset() * 60 * 1000;
                    var timeZoneDiffInMinutes = (defaultTimeZoneInMs - currentTimeZoneInMs) / (1000 * 60);
                    var hourDiff = parseInt(timeZoneDiffInMinutes / 60);
                    var minDiff = timeZoneDiffInMinutes % 60;

                    var day = date.getDate();
                    var dayOfWeek = date.getDay();
                    var monthOfYear = date.getMonth();
                    var year = date.getYear() + 1900;
                    var currentTimeInMins = (date.getHours() * 60) + date.getMinutes();
                    if (dueDate != undefined && dueDate != null) {
                        dueDate = new Date(dueDate);
                        if (dueDate > date && scheduleTimeInMins > currentTimeInMins) {
                            dueDate.setHours(timeObj.hrs + hourDiff, timeObj.mins + minDiff, dueDate.getSeconds(), dueDate.getMilliseconds());
                            columnValues[Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE] = dueDate;
                            continue;
                        }
                    }
                    if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.HOURLY || span == Constants.Baas.Tables.Columns.Type.Schedule.Span.DAILY) {
                        if (date.getDate() == day) {
                            if (scheduleTimeInMins < currentTimeInMins) {
                                date.setDate(day + 1);
                            }
                        } else {
                            date.setDate(day + 1);
                        }
                    } else {
                        var repeatedOnLength = repeatedOn.length;
                        if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.WEEKLY) {
                            if (repeatedOnLength == 0) {
                                date.setDate(day + (7 - (dayOfWeek + 1)));
                                if (date.getDate() == day && scheduleTimeInMins < currentTimeInMins) {
                                    date.setDate(day + (7 - (dayOfWeek + 1)));
                                }
                            } else {
                                var setDate = false;
                                for (var i = 0; i < repeatedOnLength; i++) {
                                    var weekDay = repeatedOn[i];
                                    if (weekDay >= dayOfWeek) {
                                        date.setDate(day - dayOfWeek + weekDay);
                                        if (date.getDate() != day || scheduleTimeInMins > currentTimeInMins) {
                                            setDate = true;
                                            break;
                                        }
                                    }
                                }
                                if (!setDate) {
                                    date.setDate(day + (7 - dayOfWeek) + repeatedOn[0]);
                                }
                            }
                        } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.MONTHLY) {
                            var newMonth = monthOfYear + 1;
                            var newYear = year;
                            if (newMonth > 11) {
                                newMonth = newMonth - 12;
                                newYear = year + 1;
                            }
                            if (repeatedOnLength == 0) {
                                date.setDate(getMaximumNoOfDays(monthOfYear, year));
                                if (day == date.getDate() && scheduleTimeInMins < currentTimeInMins) {
                                    date.setDate(getMaximumNoOfDays(newMonth, newYear));
                                }
                            } else {
                                var setDate = false;
                                for (var i = 0; i < repeatedOnLength; i++) {
                                    var scheduledDay = repeatedOn[i];
                                    if (scheduledDay >= day) {
                                        var newDay = scheduledDay > getMaximumNoOfDays(monthOfYear, year) ? getMaximumNoOfDays(monthOfYear, year) : scheduledDay;
                                        date.setDate(newDay);
                                        if (date.getDate() != day || scheduleTimeInMins > currentTimeInMins) {
                                            setDate = true;
                                            break;
                                        }
                                    }
                                }
                                if (!setDate) {
                                    var newDay = repeatedOn[0] > getMaximumNoOfDays(newMonth, newYear) ? getMaximumNoOfDays(newMonth, newYear) : repeatedOn[0];
                                    date.setFullYear(newYear, newMonth, newDay);
                                }
                            }
                        } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.YEARLY) {
                            var newYear = year + 1;
                            if (repeatedOnLength == 0) {
                                date.setFullYear(year, 11, 31);
                                if (date.getDate() == day && date.getMonth() == monthOfYear && scheduleTimeInMins < currentTimeInMins) {
                                    date.setFullYear(newYear, 11, 31);
                                }
                            } else {
                                var setDate = false;
                                for (var i = 0; i < repeatedOnLength; i++) {
                                    var scheduledMonth = repeatedOn[i];
                                    if (scheduledMonth >= monthOfYear) {
                                        date.setFullYear(year, scheduledMonth, getMaximumNoOfDays(scheduledMonth, year));
                                        if (date.getDate() != day || date.getMonth() == monthOfYear || scheduleTimeInMins > currentTimeInMins) {
                                            setDate = true;
                                            break;
                                        }
                                    }
                                }
                                if (!setDate) {
                                    var newDay = getMaximumNoOfDays(repeatedOn[0], newYear);
                                    date.setFullYear(newYear, repeatedOn[0], newDay);
                                }
                            }
                        }
                    }
                    date.setHours(timeObj.hrs + hourDiff, timeObj.mins + minDiff, date.getSeconds(), date.getMilliseconds());
                    console.log("date ***********************" + JSON.stringify(date));
//                    if (true) {
//                        date.setHours(10, 00, 00, 0);
//                    }
                    columnValues[Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE] = date;
                }
            } else if (columnInfo.columns) {
                updateDueDate(columnInfo.columns, (columnValues && columnValues instanceof Array ? columnValues : columnValues ? [columnValues] : []));
            }
        }
    }
}

function sortRepeatedOn(repeatedOn, span) {
    if (repeatedOn.length == 0) {
        return repeatedOn;
    }
    var sortedRepeatedOn = [];
    if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.WEEKLY) {
        for (var weekDay in Weeks) {
            if (repeatedOn.indexOf(weekDay) != -1) {
                sortedRepeatedOn.push(Weeks[weekDay]);
            }
        }
    }
    else
    if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.MONTHLY) {
        for (var day in Days) {
            if (repeatedOn.indexOf(day) != -1) {
                sortedRepeatedOn.push(Days[day]);
            }
        }
    } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.YEARLY) {
        for (var month in Months) {
            if (repeatedOn.indexOf(month) != -1) {
                sortedRepeatedOn.push(Months[month]);
            }
        }
    }
    return sortedRepeatedOn;
}

function getMaximumNoOfDays(month, year) {
    return [31, (isLeapYear(year) ? 29 : 28), 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month];
}

function isLeapYear(year) {
    return (((year % 4 === 0) && (year % 100 !== 0)) || (year % 400 === 0));
}


function ensureColumns(columns) {
    columns.forEach(function (columnInfo) {
        var columnType = columnInfo[Constants.Baas.Tables.Columns.TYPE];
        var column = columnInfo[Constants.Baas.Tables.Columns.EXPRESSION];
        if (columnType == Constants.Baas.Tables.Columns.Type.SCHEDULE) {
            columnInfo[Constants.Baas.Tables.Columns.TYPE] = Constants.Baas.Tables.Columns.Type.OBJECT;
            var scheduleColumns = [];
            addColumn(Constants.Baas.Tables.Columns.Type.Schedule.SPAN, Constants.Baas.Tables.Columns.Type.STRING, false, scheduleColumns);
            addColumn(Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE, Constants.Baas.Tables.Columns.Type.DATE, false, scheduleColumns);
            addColumn(Constants.Baas.Tables.Columns.Type.Schedule.REPEAT, Constants.Baas.Tables.Columns.Type.BOOLEAN, false, scheduleColumns);
            addColumn(Constants.Baas.Tables.Columns.Type.Schedule.TIME, Constants.Baas.Tables.Columns.Type.STRING, false, scheduleColumns);
            addColumn(Constants.Baas.Tables.Columns.Type.Schedule.REPEATED_ON, Constants.Baas.Tables.Columns.Type.STRING, false, scheduleColumns);
            addColumn(Constants.Baas.Tables.Columns.Type.Schedule.FREQUENCY, Constants.Baas.Tables.Columns.Type.NUMBER, false, scheduleColumns);
            columnInfo[Constants.Baas.Tables.COLUMNS] = scheduleColumns;
        } else if (columnInfo.columns) {
            ensureColumns(columnInfo.columns);
        }
    });
}

function addColumn(expression, columnType, mandatory, columns) {
    var column = {};
    column[Constants.Baas.Tables.Columns.EXPRESSION] = expression;
    column[Constants.Baas.Tables.Columns.TYPE] = columnType;
    if (mandatory) {
        column[Constants.Baas.Tables.Columns.MANDATORY] = true;
    }
    columns.push(column);
}


exports.syncTable = function (table, callback) {
    var tableColumns = table [Constants.Baas.Tables.COLUMNS];
    var addModule = false;
    tableColumns.forEach(function (columnInfo) {
        if (columnInfo[Constants.Baas.Tables.Columns.TYPE] == Constants.Baas.Tables.Columns.Type.SCHEDULE) {
            addModule = true;
        }
    })
    if (addModule) {
        table[Constants.Baas.Tables.ENABLE_MODULES] = table[Constants.Baas.Tables.ENABLE_MODULES] || [];
        var enableModules = table[Constants.Baas.Tables.ENABLE_MODULES];
        var length = enableModules.length;
        for (var i = 0; i < length; i++) {
            if (enableModules[i][Constants.Baas.Modules.ID] == Constants.ModulesConstants.WhenModule.ID) {
                callback();
                return;
            }
        }
        var whenModule = {};
        whenModule[Constants.Baas.Modules.ID] = Constants.ModulesConstants.WhenModule.ID;
        whenModule[Constants.Baas.Modules.MODULE_PATH] = Constants.ModulesConstants.WhenModule.MODULE_PATH;
        enableModules.push(whenModule);
    }
    callback();
}