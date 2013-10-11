var ejs = require('ejs');
var orders = [
    {"index":{"$order":"asc"}},
    {"parentviewgroup":{"$order":"asc", "$recursive":true}}
];

var recursive = false;

for (var i = 0; i < orders.length; i++) {
    var order = orders[i];
    var exp = Object.keys(order)[0];
    if (order[exp].$recursive) {
        recursive = true;
    }

}

console.log(recursive);


///**
// * Created with IntelliJ IDEA.
// * User: daffodil
// * Date: 23/8/13
// * Time: 10:05 AM
// * To change this template use File | Settings | File Templates.
// */
//
//var schedules = "[{\"scheduleid\":{\"name\":\"test1\",\"viewid\":\"abc\",\"datecolumn\":\"date\",\"datefilter\":\"lastNdays\"},\"when\":{\"duedate\":\"24-09-1012\"},\"visibility\":\"On\"},{\"scheduleid\":{\"name\":\"test2\",\"viewid\":\"sbas\",\"datecolumn\":\"date1\",\"datefilter\":\"lastNdays1\"},\"when\":{\"duedate\":\"24-10-1012\"},\"visibility\":\"Off\"}]";
//var schedulesToMerge = "[{\"scheduleid\":{\"name\":\"test1\",\"viewid\":\"abc\",\"datecolumn\":\"date3\",\"datefilter\":\"lastNdays\"},\"when\":{\"duedate\":\"24-12-1012\"},\"visibility\":\"Off\"},{\"scheduleid\":{\"name\":\"test4\",\"viewid\":\"sbaasds\",\"datecolumn\":\"date1\",\"datefilter\":\"lastNdays11\"},\"when\":{\"duedate\":\"24-12-1012\"},\"visibility\":\"Off\"}]";
//schedules = mergeViewSchedules(schedules, schedulesToMerge);
//console.log(JSON.stringify(schedules));
//
//function mergeViewSchedules(schedules, schedulesToMerge) {
//    var schedulesMap = getScheduleMap(schedules);
//    var schedulesToMergeMap = getScheduleMap(schedulesToMerge);
//    for (var schedulesToMergeExp in schedulesToMergeMap) {
//        if (schedulesMap[schedulesToMergeExp]) {
//            var scheduleMapObject = schedulesMap[schedulesToMergeExp];
//            var scheduleToMergeMapObject = schedulesToMergeMap[schedulesToMergeExp];
//            for (var exp in scheduleToMergeMapObject) {
//                scheduleMapObject[exp] = scheduleToMergeMapObject[exp];
//            }
//        } else {
//            schedulesMap[schedulesToMergeExp] = schedulesToMergeMap[schedulesToMergeExp];
//        }
//    }
//    var newSchedules = [];
//    for (var exp in schedulesMap) {
//        newSchedules.push(schedulesMap[exp]);
//    }
//    return newSchedules;
//}
//
//function getScheduleMap(schedules) {
//    schedules = JSON.parse(schedules);
//    var scheduleObject = {};
//    for (var i = 0; i < schedules.length; i++) {
//        var schedule =schedules[i];
//        console.log("schedule >>>>>>>>>" + JSON.stringify(schedule));
//        var scheduleId = schedule.scheduleid;
//        console.log("scheduleId >>>>>>>>>" + JSON.stringify(scheduleId));
//        var scheduleName = scheduleId.name;
//        var scheduleViewId = scheduleId.viewid;
//        scheduleObject[scheduleName + "___" + scheduleViewId] = schedule;
//    }
//    return scheduleObject;
//}
//
////var duedate = "2013-10-10";
////var date = new Date(duedate);
////console.log(JSON.stringify(date));
////console.log(date.getHours());
////date.setHours(22, 30, 00, 0);
////console.log(JSON.stringify(date));
////var Constants = require(\"./lib/shared/Constants.js\");
////
////console.log(parseLabel("sachin.bansal_ashgda"))
////
////function parseLabel(expression) {
////    expression = expression.replace(/\./gi, " ");
////    expression = expression.replace(/_id/gi, " ");
////    expression = expression.replace(/_/gi, " ");
////    var split = expression.split(" ");
////    var newLabel = "";
////    for (var i = 0; i < split.length; i++) {
////        var word = split[i];
////        word = word.substr(0, 1).toUpperCase() + word.substr(1);
////        if(newLabel.length>0){
////            newLabel = newLabel + " ";
////        }
////        newLabel = newLabel + word;
////    }
////    return newLabel;
////}
//
//
////var today = new Date();
////console.log("toady >>>>> " + JSON.stringify(today));
////console.log(today.getYear());
////var getTimezoneOffset = today.getTimezoneOffset()
////console.log("toady getTimezoneOffset>>>>> " + JSON.stringify(getTimezoneOffset));
////console.log(getTimezoneOffset / 60)
////console.log(getTimezoneOffset % 60)
////console.log(parseInt(getTimezoneOffset / 60))
////var toLocaleTimeString = today.toLocaleTimeString();
////console.log("toady toLocaleTimeString>>>>> " + JSON.stringify(toLocaleTimeString));
////var toGMTString = today.toGMTString();
////console.log("toady toGMTString>>>>> " + JSON.stringify(toGMTString))
////var getDate = today.getDate();
////console.log("getDate > >> >> " + JSON.stringify(getDate));
////var day = today.getDay();
////console.log("day >>>>>> " + JSON.stringify(day));
////today.setDate(getDate + 10);
////console.log("today >>>>>> " + JSON.stringify(today));
////var getTime = today.getTime();
////console.log("getTime  >>>> > > " + JSON.stringify(getTime));
////var getHours = today.getHours();
////console.log("getHours  >>>> > > " + JSON.stringify(getHours));
////today.setHours(23, 30, 00, 0);
////console.log("today  >>>> > > " + JSON.stringify(today));
////
//////var maxDaysInMonth = getMaximumNoOfDays(today.getMonth(), today.getYear());
//////console.log("maxDaysInMonth >>>>>>>>> " + maxDaysInMonth)
////
////function getMaximumNoOfDays(month, year) {
////    var noOfDaysInMonths = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 ];
////    if (month == 2) {
////        if (isLeapYear(year)) {
////            return 29;
////        } else {
////            return  noOfDaysInMonths[month - 1];
////        }
////    } else {
////        return  noOfDaysInMonths[month - 1];
////    }
////}
////
////checkDueDate();
////
////function isLeapYear(year) {
////    if ((year % 100 != 0 && year % 4 == 0) || year % 400 == 0) {
////        return true;
////    }
////    return false;
////}
////
////function checkDueDate() {
////    var weeks = getWeeks();
////    var months = getMonths();
////    var days = getDays();
////    var columnValues = {"feb":true};
////    var span = "Yearly";
////    var dueDate;
////    if (span && (dueDate == undefined || dueDate == null)) {
////        var today = new Date();
////        console.log("today ***********" + JSON.stringify(today));
////        var day = today.getDate();
////        console.log("day ***********" + JSON.stringify(day));
////        var date = new Date();
////        if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.HOURLY) {
////
////        } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.DAILY) {
////
////        } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.WEEKLY) {
////            console.log("date ***********" + JSON.stringify(date));
////            var dayOfWeek = today.getDay();
////            console.log("dayOfWeek ***********" + JSON.stringify(dayOfWeek));
////            var scheduledWeekDays = getScheduledWeekDays(weeks, columnValues);
////            console.log("scheduledWeekDays ***********" + JSON.stringify(scheduledWeekDays));
////            var scheduledWeekDaysLength = scheduledWeekDays.length;
////            console.log("scheduledWeekDaysLength ***********" + JSON.stringify(scheduledWeekDaysLength));
////            if (scheduledWeekDaysLength == 0) {
////                date.setDate(day + (7 - (dayOfWeek + 1)));
////                console.log("date in scheduledWeekDaysLength 0 ***********" + JSON.stringify(date));
////            } else {
////                var setDate = false;
////                for (var i = 0; i < scheduledWeekDaysLength; i++) {
////                    var weekDay = scheduledWeekDays[i];
////                    console.log("weekDay ***********" + JSON.stringify(weekDay));
////                    if (weekDay >= dayOfWeek) {
////                        date.setDate(day - dayOfWeek + weekDay);
////                        console.log("date in loop weekly  ***********" + JSON.stringify(date));
////                        setDate = true;
////                        break;
////                    }
////                }
////                console.log("setDate ***********" + JSON.stringify(setDate));
////                if (!setDate) {
////                    date.setDate(day + (7 - dayOfWeek) + weekDay);
////                }
////                console.log("date in scheduledWeekDaysLength > ***********" + JSON.stringify(date));
////            }
////        } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.MONTHLY) {
////            var monthOfYear = today.getMonth();
////            var year = today.getYear();
////            var maxNoOfdaysInMonth = getMaximumNoOfDays(monthOfYear, year)
////            var scheduledDays = getScheduledDaysInMonth(days, columnValues);
////            var scheduledDaysLength = scheduledDays.length;
////            console.log("scheduledDays > ***********" + JSON.stringify(scheduledDays));
////            if (scheduledDaysLength == 0) {
////                date.setDate(maxNoOfdaysInMonth);
////            } else {
////                var setDate = false;
////                for (var i = 0; i < scheduledDaysLength; i++) {
////                    var scheduledDay = scheduledDays[i];
////                    console.log("scheduledDay > ***********" + JSON.stringify(scheduledDay));
////                    if (scheduledDay >= day) {
////                        if (scheduledDay > maxNoOfdaysInMonth) {
////                            date.setDate(maxNoOfdaysInMonth);
////                        } else {
////                            date.setDate(scheduledDay);
////                        }
////                        console.log("date in month **************" + JSON.stringify(date));
////                        setDate = true;
////                        break;
////                    }
////                }
////                if (!setDate) {
////                    date.setMonth(monthOfYear + 1);
////                    date.setDate(scheduledDays[0]);
////                }
////            }
////        } else if (span == Constants.Baas.Tables.Columns.Type.Schedule.Span.YEARLY) {
////            var date = new Date();
////            var year = today.getYear() + 1900;
////            console.log("year *************" + JSON.stringify(year));
////            var monthOfYear = today.getMonth();
////            console.log("monthOfYear *************" + JSON.stringify(monthOfYear));
////            var scheduledMonths = getScheduledMonthsInYear(months, columnValues);
////            console.log("scheduledMonths > ***********" + JSON.stringify(scheduledMonths));
////            var scheduledMonthsLength = scheduledMonths.length;
////            if (scheduledMonthsLength == 0) {
////                date.setFullYear(year, 11, 31);
////            } else {
////                var setDate = false;
////                for (var i = 0; i < scheduledMonths.length; i++) {
////                    var scheduledMonth = scheduledMonths[i];
////                    console.log("scheduledMonth > ***********" + JSON.stringify(scheduledMonth));
////                    if (scheduledMonth >= monthOfYear) {
////                        date.setFullYear(year, scheduledMonth - 1, getMaximumNoOfDays(scheduledMonth, year));
////                        console.log("date in scheduledMonth > ***********" + JSON.stringify(date));
////                        setDate = true;
////                        break;
////                    }
////                }
////                if (!setDate) {
////                    date.setFullYear(year + 1, scheduledMonths[0] - 1, getMaximumNoOfDays(scheduledMonths[0], year + 1));
////                }
////            }
////        }
////        dueDate = date;
////    }
////    console.log("duedate  >>>>>>>>>>> " + JSON.stringify(dueDate))
////}
////
////function getScheduledWeekDays(weeks, columnValues) {
////    var scheduledWeekDays = [];
////    for (var i = 0; i < weeks.length; i++) {
////        var dayOfWeek = weeks[i];
////        if (columnValues[dayOfWeek]) {
////            scheduledWeekDays.push(i)
////        }
////    }
////    return scheduledWeekDays;
////}
////
////function getScheduledDaysInMonth(days, columnValues) {
////    var scheduledDays = [];
////    for (var i = 0; i < days.length; i++) {
////        var day = days[i];
////        if (columnValues[day]) {
////            scheduledDays.push(i + 1);
////        }
////    }
////    return scheduledDays;
////}
////
////function getScheduledMonthsInYear(months, columnValues) {
////    var scheduledMonths = [];
////    for (var i = 0; i < months.length; i++) {
////        var month = months[i];
////        if (columnValues[month]) {
////            scheduledMonths.push(i + 1);
////        }
////    }
////    return scheduledMonths;
////}
////
////function getDays() {
////    var days = [];
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.ONE);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWO);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.THREE);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.FOUR);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.FIVE);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.SIX);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.SEVEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.EIGHT);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.NINE);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.ELEVEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWELVE);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.THIRTEEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.FOURTEEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.FIFTEEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.SIXTEEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.SEVENTEEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.EIGHTEEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.NINETEEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTY);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTYONE);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTYTWO);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTYTHREE);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTYFOUR);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTYFIVE);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTYSIX);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTYSEVEN);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTYEIGHT);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.TWENTYNINE);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.THIRTY);
////    days.push(Constants.Baas.Tables.Columns.Type.Schedule.THIRTYONE);
////    return days;
////}
////
////function getWeeks() {
////    var weeks = [];
////    weeks.push(Constants.Baas.Tables.Columns.Type.Schedule.SUNDAY);
////    weeks.push(Constants.Baas.Tables.Columns.Type.Schedule.MONDAY);
////    weeks.push(Constants.Baas.Tables.Columns.Type.Schedule.TUESDAY);
////    weeks.push(Constants.Baas.Tables.Columns.Type.Schedule.WEDNESDAY);
////    weeks.push(Constants.Baas.Tables.Columns.Type.Schedule.THURSDAY);
////    weeks.push(Constants.Baas.Tables.Columns.Type.Schedule.FRIDAY);
////    weeks.push(Constants.Baas.Tables.Columns.Type.Schedule.SATURDAY);
////    return weeks;
////}
////
////function getMonths() {
////    var months = [];
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.JAN);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.FEB);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.MAR);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.APR);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.MAY);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.JUN);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.JUL);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.AUG);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.SEP);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.OCT);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.NOV);
////    months.push(Constants.Baas.Tables.Columns.Type.Schedule.DEC);
////    return months;
////}