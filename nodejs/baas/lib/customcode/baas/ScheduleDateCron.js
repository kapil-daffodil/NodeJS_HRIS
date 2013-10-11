/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 23/8/13
 * Time: 6:41 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../../shared/Constants.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var DatabaseEngine = require("../../database/DatabaseEngine.js");
var UpdateEngine = require("../../database/UpdateEngine.js");
var APIConstants = require("nodejsapi/Constants.js");
var Utils = require("apputil/util.js");
var Weeks = {"Sun":0, "Mon":1, "Tue":2, "Wed":3, "Thu":4, "Fri":5, "Sat":6};
var Days = {"1":1, "2":2, "3":3, "4":4, "5":5, "6":6, "7":7, "8":8, "9":9, "10":10, "11":11, "12":12, "13":13, "14":14, "15":15, "16":16, "17":17, "18":18, "19":19, "20":20, "21":21, "22":22, "23":23, "24":24, "25":25, "26":26, "27":27, "28":28, "29":29, "30":30, "31":31};
var Months = {"Jan":0, "Feb":1, "Mar":2, "Apr":3, "May":4, "Jun":5, "Jul":6, "Aug":7, "Sep":8, "Oct":9, "Nov":10, "Dec":11};

exports.scheduleDate = function (parameters, callback) {
    getWhenTypeColumnTables(ApplaneCallback(callback, function (tables) {
        if (!tables || tables.length == 0) {
            callback();
        } else {
            Utils.iterateArray(tables, callback, function (table, callback) {
                var tableName = table[Constants.Baas.Tables.ID];
                var tableColumns = table[Constants.Baas.Tables.COLUMNS] || [];
                var tableApplications = table[Constants.Baas.Tables.APPLICATIONS] || [];
                var orgEnabled = table[Constants.Baas.Tables.ORGENABLED];
                if (tableApplications && tableApplications.length > 0) {
                    Utils.iterateArray(tableColumns, callback, function (tableColumn, callback) {
                        var tableColumnType = tableColumn[Constants.Baas.Tables.Columns.TYPE];
                        var tableColumnExpression = tableColumn[Constants.Baas.Tables.Columns.EXPRESSION];
                        if (tableColumnType == Constants.Baas.Tables.Columns.Type.SCHEDULE) {
                            Utils.iterateArray(tableApplications, callback, function (tableApplication, callback) {
                                var tableask = tableApplication[Constants.Baas.Applications.ASK];
                                var tableApplicationId = tableApplication[APIConstants.Query._ID];
                                if (orgEnabled) {
                                    getApplicationOrganizations(tableApplicationId, ApplaneCallback(callback, function (organizations) {
                                        if (organizations && organizations.length > 0) {
                                            Utils.iterateArray(organizations, callback, function (organization, callback) {
                                                var tableosk = organization[Constants.Baas.Organizations.OSK];
                                                updateScheduleData(tableName, tableask, tableosk, tableColumnExpression, callback);
                                            })
                                        } else {
                                            callback();
                                        }
                                    }))
                                } else {
                                    updateScheduleData(tableName, tableask, null, tableColumnExpression, callback);
                                }

                            })
                        } else {
                            callback();
                        }
                    })
                } else {
                    callback();
                }
            })
        }
    }));
}

function getWhenTypeColumnTables(callback) {
    var tableFilter = {};
    tableFilter[Constants.Baas.Tables.COLUMNS + "." + Constants.Baas.Tables.Columns.TYPE] = "{columntype__}";
    var tableQuery = {};
    tableQuery[APIConstants.Query.TABLE] = Constants.Baas.TABLES;
    tableQuery[APIConstants.Query.FILTER] = tableFilter;
    tableQuery[APIConstants.Query.PARAMETERS] = {"columntype__":Constants.Baas.Tables.Columns.Type.SCHEDULE};
    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        query:tableQuery,
        callback:ApplaneCallback(callback, function (result) {
            callback(null, result[APIConstants.Query.Result.DATA]);
        })
    })
}

function getApplicationOrganizations(tableApplicationId, callback) {
    var tableFilter = {};
    tableFilter[Constants.Baas.Organizations.APPLICATIONS] = "{applicationid__}";
    var tableQuery = {};
    tableQuery[APIConstants.Query.TABLE] = Constants.Baas.ORGANIZATIONS;
    tableQuery[APIConstants.Query.COLUMNS] = [Constants.Baas.Organizations.OSK];
    tableQuery[APIConstants.Query.FILTER] = tableFilter;
    tableQuery[APIConstants.Query.PARAMETERS] = {"applicationid__":tableApplicationId};
    DatabaseEngine.query({
        ask:Constants.Baas.ASK,
        query:tableQuery,
        callback:ApplaneCallback(callback, function (result) {
            callback(null, result[APIConstants.Query.Result.DATA]);
        })
    })
}

function updateScheduleData(tableName, tableAsk, tableOsk, tableColumnExpression, callback) {
    getScheduledData(tableName, tableAsk, tableOsk, tableColumnExpression, ApplaneCallback(callback, function (scheduleData) {
            if (scheduleData && scheduleData.length > 0) {
                Utils.iterateArray(scheduleData, callback, function (record, callback) {
                    var columnValues = record[tableColumnExpression];
                    if (Object.keys(columnValues).length > 0) {
                        var span = columnValues[Constants.Baas.Tables.Columns.Type.Schedule.SPAN];
                        if (!span || span.trim().length == 0 || span == Constants.Baas.Tables.Columns.Type.Schedule.NONE) {
                            callback();
                        } else {
                            populateNewDueDate(span, columnValues);
//                            console.log("record ****************" + JSON.stringify(record));
//                            console.log("tableName ****************" + JSON.stringify(tableName));
//                            console.log("tableAsk ****************" + JSON.stringify(tableAsk));
//                            console.log("tableOsk ****************" + JSON.stringify(tableOsk));

                            var updates = {};
                            updates[APIConstants.Query.TABLE] = tableName;
                            updates[APIConstants.Update.OPERATIONS] = record;
                            UpdateEngine.performUpdate({
                                ask:tableAsk,
                                osk:tableOsk,
                                updates:updates,
                                callback:callback
                            });
                        }
                    } else {
                        callback();
                    }
                })
            } else {
                callback();
            }
        }
    ))
}

function populateNewDueDate(span, columnValues) {
    var repeatedOn = columnValues[Constants.Baas.Tables.Columns.Type.Schedule.REPEATED_ON] || [];
    repeatedOn = sortRepeatedOn(repeatedOn, span);
    var repeatedOnLength = repeatedOn.length;
    var frequency = columnValues[Constants.Baas.Tables.Columns.Type.Schedule.FREQUENCY] || 1;
    var dueDate = new Date(columnValues[Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE]);

    var newDate = new Date();
    var day = newDate.getDate();
    var dayOfWeek = newDate.getDay();
    var monthOfYear = newDate.getMonth();
    var year = newDate.getYear() + 1900;
    var currentTimeInMins = (newDate.getHours() * 60) + newDate.getMinutes();
    var scheduleTimeInMins = (dueDate.getHours() * 60) + dueDate.getMinutes();

    var defaultTimeZoneInMs = -19800000;
    var currentTimeZoneInMs = newDate.getTimezoneOffset() * 60 * 1000;
    var timeZoneDiffInMinutes = (defaultTimeZoneInMs - currentTimeZoneInMs) / (1000 * 60);
    var hourDiff = parseInt(timeZoneDiffInMinutes / 60);
    var minDiff = timeZoneDiffInMinutes % 60;
    if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.HOURLY) {
        var dueDateHrs = dueDate.getHours();
        var dueDateMins = dueDate.getMinutes();
        var dueDateTimeInMins = dueDateHrs * 60 + dueDateMins;
        var newDueDateTimeInMins = (dueDateTimeInMins + frequency * 60);
        if (newDueDateTimeInMins >= (24 * 60)) {
            newDate.setDate(day + 1);
            newDueDateTimeInMins = newDueDateTimeInMins - (24 * 60);
        }
        var newHours = parseInt(newDueDateTimeInMins / 60);
        var newMinutes = newDueDateTimeInMins % 60;
        newDate.setHours(newHours + hourDiff, newMinutes + minDiff, dueDate.getSeconds(), dueDate.getMilliseconds());
        var time = newHours + ":" + newMinutes + " " + (newHours < 12 ? "AM" : "PM");
        columnValues[Constants.Baas.Tables.Columns.Type.Schedule.TIME] = time;
    } else {
        if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.DAILY) {
            newDate.setDate(day + frequency);
        } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.WEEKLY) {
            if (repeatedOnLength == 0) {
                newDate.setDate(day + ((7 * frequency) - (dayOfWeek + 1)));
            } else {
                var setDate = false;
                for (var i = 0; i < repeatedOnLength; i++) {
                    var weekDay = repeatedOn[i];
                    if (weekDay > dayOfWeek) {
                        newDate.setDate(day - dayOfWeek + weekDay);
                        if (newDate.getDate() != day || scheduleTimeInMins > currentTimeInMins) {
                            setDate = true;
                            break;
                        }
                    }
                }
                if (!setDate) {
                    newDate.setDate(day - dayOfWeek + (7 * frequency ) + repeatedOn[0]);
                }
            }
        } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.MONTHLY) {
            var newMonth = monthOfYear + frequency;
            var newYear = year;
            if (newMonth > 11) {
                newMonth = newMonth - 12;
                newYear = year + 1;
            }
            if (repeatedOnLength == 0) {
                newDate.setFullYear(newYear, newMonth, getMaximumNoOfDays(newMonth, newYear));
            } else {
                var setDate = false;
                for (var i = 0; i < repeatedOnLength; i++) {
                    var scheduledDay = repeatedOn[i];
                    if (scheduledDay > day) {
                        var newDay = scheduledDay > getMaximumNoOfDays(monthOfYear, year) ? getMaximumNoOfDays(monthOfYear, year) : scheduledDay;
                        newDate.setDate(newDay);
                        if (newDate.getDate() != day || scheduleTimeInMins > currentTimeInMins) {
                            setDate = true;
                            break;
                        }
                    }
                }
                if (!setDate) {
                    var newDay = repeatedOn[0] > getMaximumNoOfDays(newMonth, newYear) ? getMaximumNoOfDays(newMonth, newYear) : repeatedOn[0];
                    newDate.setFullYear(newYear, newMonth, newDay);
                }
            }
        } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.YEARLY) {
            var newYear = year + frequency;
            if (repeatedOnLength == 0) {
                newDate.setFullYear(newYear, 11, 31);
            } else {
                var setDate = false;
                for (var i = 0; i < repeatedOnLength; i++) {
                    var scheduledMonth = repeatedOn[i];
                    if (scheduledMonth > monthOfYear) {
                        newDate.setFullYear(year, scheduledMonth, getMaximumNoOfDays(scheduledMonth, year));
                        if (newDate.getDate() != day || newDate.getMonth() == monthOfYear || scheduleTimeInMins > currentTimeInMins) {
                            setDate = true;
                            break;
                        }
                    }
                }
                if (!setDate) {
                    var newDay = getMaximumNoOfDays(repeatedOn[0], newYear);
                    newDate.setFullYear(newYear, repeatedOn[0], newDay);
                }
            }
        }
        var time = dueDate.getHours() + ":" + dueDate.getMinutes() + " " + (dueDate.getHours() < 12 ? "AM" : "PM");
        columnValues[Constants.Baas.Tables.Columns.Type.Schedule.TIME] = time;
        newDate.setHours(dueDate.getHours(), dueDate.getMinutes(), dueDate.getSeconds(), dueDate.getMilliseconds());
    }
    columnValues[Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE] = newDate;
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

function getScheduledData(tableName, tableask, tableosk, tableColumnExpression, callback) {
    var date = new Date();
    date.setMinutes(date.getMinutes() - 15);
    var filter = {};
    filter[tableColumnExpression + "." + Constants.Baas.Tables.Columns.Type.Schedule.REPEAT] = true;
    filter[tableColumnExpression + "." + Constants.Baas.Tables.Columns.Type.Schedule.DUE_DATE] = {"$lt":date};
    var columns = [];
    columns.push(tableColumnExpression);
    var tableQuery = {};
    tableQuery[APIConstants.Query.TABLE] = tableName;
    tableQuery[APIConstants.Query.COLUMNS] = columns;
    tableQuery[APIConstants.Query.FILTER] = filter;
    DatabaseEngine.query({
        ask:tableask,
        osk:tableosk,
        query:tableQuery,
        callback:ApplaneCallback(callback, function (result) {
            callback(null, result[APIConstants.Query.Result.DATA]);
        })
    })
}


function getMaximumNoOfDays(month, year) {
    return [31, (isLeapYear(year) ? 29 : 28), 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month];
}

function isLeapYear(year) {
    return (((year % 4 === 0) && (year % 100 !== 0)) || (year % 400 === 0));
}

//this.scheduleDate(function (err, res) {
//    if (err) {
//        console.log(err);
//    } else {
//        console.log("success..........");
//    }
//})







