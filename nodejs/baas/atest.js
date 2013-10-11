/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 7/19/13
 * Time: 12:10 PM
 * To change this template use File | Settings | File Templates.
 */

var mongoDb = require("database/mongodb/MongoDBManager.js");
var currentDate = new Date();
var date = currentDate.getDate();
var month = currentDate.getMonth() + 1;
var year = currentDate.getFullYear();
var hours = currentDate.getHours();
var minutes = currentDate.getMinutes();
var seconds = currentDate.getSeconds();

var query = false;

//query = true;


if (query) {
    db.metric.aggregate(
        {$match:{"time":{$gte:ISODate("2013-07-24T00:00:00Z")}}},
        {$group:{_id:{date:{$dayOfYear:"$time"}}}},
        {$group:{_id:{date:'$_id.date'}, count:{$sum:1}}}
    )


    mongoDb.find("baas", "employees", {'created_on.date':{$lt:14}, 'created_on.month':{$lt:14}}, function (err, data) {

        if (err) {
            console.log("err" + err.stack);
        } else {
            console.log(data);
        }
    });
}


if (!query) {
//    mongoDb.insert("baas", "employees", {"created_on":{"month":month,"year":year,"date":date,"time":hours+":"+seconds+":"+minutes},"task":"test"}, function (err, data) {
    mongoDb.insert("baas", "employees", {"created_on":{"date":new Date("2013-07-24")}, "task":"test"}, function (err, data) {
        if (err) {
            console.log("err" + err.stack);
        } else {
            console.log(data)
        }
    });
}

