/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 29/8/13
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */

var DataBaseEngine = require('../database/DatabaseEngine.js');
var CacheService = require("../cache/cacheservice.js");
var xlsx = require('node-xlsx');
var Constants = require('../shared/Constants.js');
var QueryConstants = require("nodejsapi/Constants.js");

exports.exportFile = function (req, callback) {
    var ask = req.param("ask");
    var osk = req.param("osk");
    var usk = req.param("usk");
//    var user = usk ? CacheService.get(usk) : null;
    var query = JSON.parse(req.param("query"))
    var queryColumns = query[QueryConstants.Query.COLUMNS];
    var fileName = req.param("filename");
    var header = req.param("header");
    DataBaseEngine.query({
        ask:ask,
        osk:osk,
        usk:usk,
//        user:user,
        query:query,
        callback:function (err, result) {
            if (err) {
                callback(err);
            } else {
                var queryData = result ? result[QueryConstants.Query.Result.DATA] : [];
                var worksheets = [];
                var worksheetObj = {};
                worksheetObj.name = fileName ? fileName : "ExportFile.xlsx";
                var worksheetData = [];
                if (queryData && queryData.length > 0) {
                    worksheetData = populateWorksheetData(queryData, queryColumns, header);
                } else {
                    worksheetData.push([]);
                    worksheetData.push([]);
                }
                worksheetObj.data = worksheetData;
                worksheets.push(worksheetObj);
                var buffer = xlsx.build({worksheets:worksheets});
                callback(null, buffer);
            }
        }
    });
}

function populateWorksheetData(queryData, queryColumns, header) {
    var worksheetData = [];
    if (header == undefined || header != "false") {
        var headers = [];
        for (var j = 0; j < queryColumns.length; j++) {
            var queryColumn = queryColumns[j];
            var columnLabel = queryColumn.label || queryColumn [QueryConstants.Query.Columns.EXPRESSION];
            headers.push(columnLabel);
        }
        worksheetData.push(headers);
    }
    for (var i = 0; i < queryData.length; i++) {
        var data = [];
        var row = queryData[i];
        for (var j = 0; j < queryColumns.length; j++) {
            var newRowData = {};
            var queryColumn = queryColumns[j];
            var columnExpression = queryColumn [QueryConstants.Query.Columns.EXPRESSION];
            newRowData.value = row[columnExpression] ? row[columnExpression] : "";
            data.push(newRowData);
        }
        worksheetData.push(data);
    }
    return worksheetData;
}

//var buffer = xlsx.build({worksheets:[
//    {"name":"mySheetName", "data":[
//        ["A1", "B1"],
//        [
//            {"value":"A2", "formatCode":"General"},
//            {"value":"B2", "formatCode":"General"}
//        ],
//        [
//            {"value":"A3", "formatCode":"General"},
//            {"value":5},
//            {"value":true}
//        ]
//    ]}
//]});