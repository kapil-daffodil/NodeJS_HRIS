var firstday;
var lastday;
var curr;
var formattingStyle = "mmmm yyyy-dd-mm";
var year;
var selected = {dateTemp:false, monthTemp:false, yearTemp:false, weekTemp:false};

var currmonth;
function current(isDate,isMonth,isYear,isWeek) {
    curr = new Date; // get current date

    if(isDate){
        selected.dateTemp = true;
        selected.weekTemp = false;
        selected.monthTemp = false;
        selected.yearTemp = false;
    }else if(isMonth){
        selected.monthTemp = true;
        selected.weekTemp = false
        selected.dateTemp = false
        selected.yearTemp = false;
    }else if(isYear){
        selected.yearTemp = true;
        selected.weekTemp = false;
        selected.monthTemp = false;
        selected.dateTemp= false;
    }else if(isWeek){
        selected.weekTemp = true;
        selected.dateTemp = false;
        selected.monthTemp = false;
        selected.yearTemp = false;
    }


    if (selected.dateTemp) {
        $('#dateholder').html(curr.toUTCString());
        $('#dateholder').html(curr.format(formattingStyle));
    } else if (selected.weekTemp) {
//      week start
        var first = curr.getDate() - curr.getDay(); // First day is the day of the month - the day of the week
        //alert("First : " + first + " , date : " + curr.getDate() + " , day : " + curr.getDay());
        var last = first + 6; // last day is the first day + 6
        firstday = new Date(curr.setDate(first));
        lastday = new Date(curr.setDate(last));
//        $('#dateholder').html(firstday.toUTCString() + '-' + lastday.toUTCString());
        $('#dateholder').html(firstday.toUTCString() + '-' + lastday.format(forma));
    } else if (selected.monthTemp) {
        currmonth = curr.getMonth();
        var firstDayT = new Date(curr.getFullYear(), currmonth, 1);
        var lastDayT = new Date(curr.getFullYear(), currmonth + 1, 0);
//      alert("Current Month : "+ currmonth + " , first day : " + firstDayT + " ,last day : " + lastDayT);
        $('#dateholder').html("First day : " + firstDayT.format(formattingStyle) + " and last day : " + lastDayT.format(formattingStyle));
    }else if(selected.yearTemp){
        year = curr.getFullYear();
        $('#dateholder').html("Current year : " + year);
    }
}


function next() {

    if(selected.weekTemp){
        curr  = firstday;
        var first = curr .getDate() + 7 ;
        var last = first + 6; // last day is the first day + 6
        firstday = new Date(curr .setDate(first));
        lastday = new Date(curr .setDate(last));
        $('#dateholder').html(firstday.toUTCString() +  '-' + lastday.toUTCString());
    }else if(selected.dateTemp){
        curr = new Date(curr.setDate(curr.getDate() + 1));
//        $('#dateholder').html(curr.toUTCString());
        $('#dateholder').html(curr.format(formattingStyle));
    }else if(selected.monthTemp){
        currmonth += 1;
        var firstDayT = new Date(curr.getFullYear(),currmonth , 1);
        var lastDayT = new Date(curr.getFullYear(), currmonth + 1, 0);
        $('#dateholder').html("First day : " + firstDayT.format(formattingStyle) + " and last day : " + lastDayT.format(formattingStyle));
    }else if(selected.yearTemp){
        curr.setFullYear(curr.getFullYear() + 1);
        year = curr.getFullYear();
        $('#dateholder').html("Next year : " + year);
    }
}


function prev() {

    if(selected.weekTemp){
        curr = firstday;
        var first = curr.getDate() - 7;
        var last = first + 6; // last day is the first day + 6
        firstday = new Date(curr.setDate(first));
        lastday = new Date(curr.setDate(last));
        $('#dateholder').html(firstday.toUTCString() + '-' + lastday.toUTCString());
    }else if(selected.dateTemp){
        curr = new Date(curr.setDate(curr.getDate() + -1));  //  for previous date
//        $('#dateholder').html(curr.toUTCString());
        $('#dateholder').html(curr.format(formattingStyle));
    }else if(selected.monthTemp){
        currmonth -= 1;
        var firstDayT = new Date(curr.getFullYear(),currmonth , 1);
        var lastDayT = new Date(curr.getFullYear(), currmonth + 1, 0);
        $('#dateholder').html("First day : " + firstDayT.format(formattingStyle) + " and last day : " + lastDayT.format(formattingStyle));
    }else if(selected.yearTemp){
        curr.setFullYear(curr.getFullYear() - 1);
        year = curr.getFullYear();
        $('#dateholder').html("Previous year : " + year);
    }
}
function selectDate(){
    selected.dateTemp = true;
    selected.weekTemp,selected.yearTemp,selected.monthTemp = false;
}

function selectMonth(){
    selected.monthTemp = true;
    selected.weekTemp,selected.yearTemp,selected.dateTemp = false;
}

function selectYear(){
    selected.yearTemp = true;
    selected.weekTemp,selected.monthTemp,selected.dateTemp = false;
}

function selectWeek(){
    selected.weekTemp = true;
    selected.yearTemp,selected.monthTemp,selected.dateTemp = false;
}