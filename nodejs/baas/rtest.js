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


var updates = {
    ask:"mtm",
    updates:{"table":"applications__baas", "filter":{"users__baas__applications.applications.organizations":"{organizations__baas__id}", "applications._id":"{organizations__baas_applications._id}"}, "parameters":{"_id":"523ae9239ebd19430100000d", "organizations__baas__id":"523ae9239ebd19430100000d", "organizations__baas_applications._id":"523ae7ef9ebd194301000007", "applications._id":"523ae7ef9ebd194301000007"}, "operations":[
        {"_id":"523ae7ef9ebd194301000007","users__baas__applications":{"_id":"523ae83f9ebd194301000009", "usergroupid":{"_id":"51f90621f86dfee017000001", "id":"baas"}, "username":"kapil.dalal@daffodilsw.com"}, "users__baas__applications._id":"523ae83f9ebd194301000009", "users__baas__applications.usergroupid":{"_id":"51f90621f86dfee017000001", "id":"baas"}, "users__baas__applications.username":"kapil.dalal@daffodilsw.com", "users__baas__applications.usergroupid.id":"baas"}
    ]},
    callback:function (err, data) {
        if (err) {
            console.log("Error::" + err.stack);

        } else {
            console.log("Result:....." + JSON.stringify(data))
        }
    }
}

UpdateEngine.performUpdate(updates);


//var operatoins = [{"name":"Pawan Singh","cityid.name":"Hansi","languages.name":"English","languages.countries.name":"India"}];
//var operatoins = [{name:"Rohit",languages:[{name:"Hindi","countries.name":"India"}]}]
//
//Util.resolveDot(operatoins);
//
//console.log(JSON.stringify(operatoins))

//query.columns = {$elemMatch:{"table.id":"authors__northwind"}};
//var updates = {};
////var pushObject = {};
////var logObj = {};
////logObj._id = new objectID();
////logObj.log = "test1111112222222";
////logObj.logdate = new Date();
////pushObject.logs = logObj;
////updates.$set = {"logs.$.log":"sachin122222"};
////var columnValue = {"columns.$.table.orgenabled":true};
////var columnValue = {"userid.emailid":"pawan.phutela@daffodilsw.com"}
//var updates = {$set:columnValue};
//
//var query = {"$or":[
//    {"userdelete":null},
//    {"userdelete":false}
//]};
//MongoDBManager.find("baas", "users__baas", query, {_id:1}, function (err, result) {
//    if (err) {
//        console.log("Problem while query>>>>" + err.stack);
//    } else {
//        console.log("Result: " + JSON.stringify(result));
//    }
//})

//QueryEngine.query({ask:"appsstudio", query:{"table":"viewgroups__appsstudio", "columns":[
//    {"_id":"5245458a3b5d183c11000007", "type":"index", "expression":"menus.index", "__createdon":"2013-09-27T08:44:58.397Z", "__updatedby":null, "__updatedon":"2013-09-27T08:45:09.376Z", "width":200, "label":"Index", "visibility":"Both"},
//    {"_id":"5209ea13b0dda6a407000004", "type":"string", "expression":"menus.label", "mandatory":true, "primary":true, "replicate":true, "__updatedby":null, "__updatedon":"2013-09-27T08:45:09.376Z", "width":200, "label":"Label", "visibility":"Both"},
//    {"_id":"5209ea7ab0dda6a407000008", "type":"lookup", "expression":"menus.applicationid", "table":{"_id":"51ece1cdacf26b5018000005", "id":"applications__baas", "orgenabled":null}, "primarycolumns":[
//        {"_id":"5209e4bf26965de412000001", "type":"string", "expression":"id", "replicate":true, "primary":true}
//    ], "replicatecolumns":[
//        {"_id":"5209e4bf26965de412000001", "type":"string", "expression":"id", "replicate":true, "primary":true},
//        {"_id":"5209e4bf26965de412000002", "type":"string", "expression":"ask", "replicate":true},
//        {"_id":"5209e4bf26965de412000003", "type":"string", "expression":"label", "replicate":true, "mandatory":true},
//        {"_id":"522eda45d870c3ec1700021e", "type":"boolean", "expression":"orgenabled", "mandatory":false, "replicate":true}
//    ], "__updatedby":null, "__updatedon":"2013-09-27T08:45:09.376Z", "undefined":[], "width":200, "label":"Applicationid", "visibility":"Both"},
//    {"_id":"5209ea7bb0dda6a407000009", "type":"lookup", "expression":"menus.table", "table":{"_id":"51ece15facf26b5018000003", "id":"tables__baas", "orgenabled":null}, "primarycolumns":[
//        {"_id":"51f0e27905435ef815000002", "type":"string", "expression":"id", "replicate":true, "primary":true, "__updatedby":null, "__updatedon":"2013-09-27T08:00:12.943Z"}
//    ], "replicatecolumns":[
//        {"_id":"51f0e27905435ef815000002", "type":"string", "expression":"id", "replicate":true, "primary":true, "__updatedby":null, "__updatedon":"2013-09-27T08:00:12.943Z"},
//        {"_id":"52035305a94f8c7017000004", "type":"boolean", "expression":"orgenabled", "replicate":true, "__updatedby":null, "__updatedon":"2013-09-27T08:00:12.943Z"}
//    ], "__updatedby":null, "__updatedon":"2013-09-27T08:45:09.376Z", "undefined":[], "width":200, "label":"Table", "visibility":"Both"},
//    {"_id":"521ee48d8f47169c01000006", "type":"string", "expression":"menus.filter", "__updatedby":null, "__updatedon":"2013-09-27T08:45:09.376Z", "width":200, "label":"Filter", "visibility":"Both"}
//], "filter":{"_id":"{viewgroups__appsstudio__id}"}, "parameters":{"_id":"5226b968ac2fb5300d000003", "viewgroups__appsstudio__id":"5226b968ac2fb5300d000003"}, "max_rows":1}, callback:function (err, result) {
//    if (err) {
//        console.log("Problem while query>>>>" + err.stack);
//    } else {
//        console.log(JSON.stringify(result));
//    }
//}})
//MongoDBManager.update("baas", "users__baas", query, updates, {multi:true}, function (err, result) {
//    if (err) {
//        console.log("Problem while inseting logs>>>>" + err.stack);
//    } else {
//        console.log("success");
//    }
//})



//
//var oldv = {"_id":"521d9168bfbe4e7854000008","designation":"Desig","emailid":"parveenmca05@gmail.com","firstname":"Parveen","lastname":"Sihag","picture":null,"status":true,"usergroupid":{"_id":"520f45da055ac21c14000001","id":"mtm"},"username":"admin"};
//var newv = {"_id":"521d9168bfbe4e7854000008","designation":"Desig","emailid":"parveenmca05@gmail.com","firstname":"Parveen","lastname":"Sihag","picture":null,"status":true,"usergroupid":{"_id":"520f45da055ac21c14000001","id":"mtm"},"username":"admin"};
//var equalV = Util.deepEqual(oldv,newv);
//console.log(equalV)