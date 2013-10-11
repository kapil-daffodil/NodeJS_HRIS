//var primaryColumns = [
//    {type:"number", expression:"age"},
//    {type:"string", expression:"name"},
//    {type:"date", expression:"date"},
//    {type:"number", expression:"roll"}
//];
//
//for (var i = 0; i < primaryColumns.length; i++) {
//    var primaryColumn = primaryColumns[i];
//    var type = primaryColumn.type;
//    if (type == "string") {
//        primaryColumns.splice(i, 1);
//        primaryColumns.unshift(primaryColumn);
//        break;
//    }
//}
//
//console.log(JSON.stringify(primaryColumns));

var UpdateEngine = require("./lib/database/updateengine.js");
var QueryEngine = require("./lib/database/databaseengine.js");
var MongoDBManager = require('./lib/database/mongodb/MongoDBManager.js');
var AppCallback = require("apputil/applanecallback.js");
var MetaData = require("./lib/metadata/metadataprovider.js");
var Util = require("apputil/util.js");
var objectID = require("mongodb").ObjectID;
var assert = require("assert");


var callback = function (data) {
    console.log("Result:" + JSON.stringify(data));
}

var errcallback = function (err) {
    console.log("Error: " + err.stack);
}

var test=null;
console.log(JSON.stringify(test));

//var updates = {
//    ask:"52258a352e93f0641f000002",
//    query:{"table":"persons__northwind","filter":{"_id":{$nin:[]}}},
//    callback:function (err, data) {
//        if (err) {
//            console.log("Error::" + err.stack);
//
//        } else {
//            console.log("Result:....." + JSON.stringify(data))
//        }
//    }
//}
//QueryEngine.query(updates);

//UpdateEngine.performUpdate(updates);
//MongoDBManager.find("baas", "currencyconversionrates__baas", {"from":"USD", "to":{"$in":["USD"]}}, function (err, res) {
//    if (err) {
//        console.log("error : " + err.stack);
//    } else {
//        console.log("result : " + JSON.stringify(res));
//    }
//})
//
////UpdateEngine.performUpdate(updates);
//
//
////var operatoins = [{"name":"Pawan Singh","cityid.name":"Hansi","languages.name":"English","languages.countries.name":"India"}];
////var operatoins = [{name:"Rohit",languages:[{name:"Hindi","countries.name":"India"}]}]
////
////Util.resolveDot(operatoins);
////
////console.log(JSON.stringify(operatoins))
//
////query.columns = {$elemMatch:{"table.id":"authors__northwind"}};
////var updates = {};
//////var pushObject = {};
//////var logObj = {};
//////logObj._id = new objectID();
//////logObj.log = "test1111112222222";
//////logObj.logdate = new Date();
//////pushObject.logs = logObj;
//////updates.$set = {"logs.$.log":"sachin122222"};
//////var columnValue = {"columns.$.table.orgenabled":true};
//////var columnValue = {"userid.emailid":"pawan.phutela@daffodilsw.com"}
////var updates = {$set:columnValue};
////
////var query = {"$or":[
////    {"userdelete":null},
////    {"userdelete":false}
////]};
////MongoDBManager.find("baas", "users__baas", query, {_id:1}, function (err, result) {
////    if (err) {
////        console.log("Problem while query>>>>" + err.stack);
////    } else {
////        console.log("Result: " + JSON.stringify(result));
////    }
////})
//
////QueryEngine.query({ask:"mtm", query:{"table":"users__baas", "filter":{"$or":[
////    {"userdelete":null},
////    {"userdelete":false}
////]}, "columns":["_id"]}, callback:function (err, result) {
////    if (err) {
////        console.log("Problem while query>>>>" + err.stack);
////    } else {
////        console.log(JSON.stringify(result));
////    }
////}})
////MongoDBManager.update("baas", "users__baas", query, updates, {multi:true}, function (err, result) {
////    if (err) {
////        console.log("Problem while inseting logs>>>>" + err.stack);
////    } else {
////        console.log("success");
////    }
////})
//
//
//
