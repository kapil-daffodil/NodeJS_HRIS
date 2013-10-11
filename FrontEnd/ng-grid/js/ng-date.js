var firstday;
var lastday;
var curr;
function current() {


    curr = new Date; // get current date
//        var first = curr.getDate() - curr.getDay(); // First day is the day of the month - the day of the week
//        var last = first + 6; // last day is the first day + 6
//
//        firstday = new Date(curr.setDate(first));
//        lastday = new Date(curr.setDate(last));
//
//        $('#dateholder').html(firstday.toUTCString() +  '-' + lastday.toUTCString());
    $('#dateholder').html(curr.toUTCString());
}
function next() {

//       curr  = firstday;
//        var first = curr .getDate() + 7 ;
//        var last = first + 6; // last day is the first day + 6
//
//        firstday = new Date(curr .setDate(first));
//        lastday = new Date(curr .setDate(last));
//       $('#dateholder').html(firstday.toUTCString() +  '-' + lastday.toUTCString());

    curr = new Date(curr.setDate(curr.getDate() + 1));
    $('#dateholder').html(curr.toUTCString());


}
function prev() {

//    curr = firstday;
//    var first = curr.getDate() - 7;
//    var last = first + 6; // last day is the first day + 6
//
//    firstday = new Date(curr.setDate(first));
//    lastday = new Date(curr.setDate(last));
//    $('#dateholder').html(firstday.toUTCString() + '-' + lastday.toUTCString());
    curr = new Date(curr.setDate(curr.getDate() + -1));
    $('#dateholder').html(curr.toUTCString());
}

