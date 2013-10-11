/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 28/8/13
 * Time: 12:56 PM
 * To change this template use File | Settings | File Templates.
 */

var xlsx = require('node-xlsx');

//var obj = xlsx.parse(__dirname + '/myFile.xlsx'); // parses a file
//
//var obj = xlsx.parse(fs.readFileSync(__dirname + '/myFile.xlsx')); // parses a buffer
//
//var xlsx = require('node-xlsx');

var buffer = xlsx.build({worksheets:[
    {"name":"mySheetName", "data":[
        ["A1", "B1"],
        [
            {"value":"A2", "formatCode":"General"},
            {"value":"B2", "formatCode":"General"}
        ]
    ]}
]});

console.log(buffer);

