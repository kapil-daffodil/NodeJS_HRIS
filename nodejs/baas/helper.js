var http = require('http');
var urlParser = require('url');
var DatabaseEngine = require('./lib/database/mongodb/mongodbmanager.js');
var MongoDB = require('mongodb');
var ObjectID = MongoDB.ObjectID;
var MetadataEngine = require("./lib/metadata/metadataprovider.js");
var UpdateEngine = require("./lib/database/updateengine.js");
var CacheService = require("./lib/cache/cacheservice.js");

http.createServer(
    function (req, res) {
        try {
            var url = urlParser.parse(req.url, true);
            var queryParams = url.query;
            var callback = function (err, data) {

                if (err) {
                    res.writeHead(200, responseType);
                    var error = {code:err.code, message:err.message, stack:err.stack};
                    res.write(JSON.stringify(error));
                    res.end();
                } else {
                    res.writeHead(200, responseType);
                    res.write(JSON.stringify(data));
                    res.end();
                }
            }

            var responseType = {"Content Type":"applicatoin/json", "Access-Control-Allow-Origin":"*", "Access-Control-Allow-Methods":"GET, POST, OPTIONS"};

            if (queryParams.update != null) {

                var update = JSON.parse(queryParams.update);

                var findQuery = update.query;
                if (findQuery) {

                    if (findQuery._id) {
                        findQuery._id = new ObjectID(findQuery._id);
                    }
                } else {
                    findQuery = {};
                }

                DatabaseEngine.update(update.database, update.collection, findQuery, {"$set":update.updates}, callback);
            } else if (queryParams.bassupdate != null) {
                var update = JSON.parse(queryParams.bassupdate);
                update.callback = callback;
                UpdateEngine.performUpdate(update)

            } else if (queryParams.insert != null) {
                var insert = JSON.parse(queryParams.insert);
                DatabaseEngine.insert(insert.database, insert.collection, insert.updates, callback);
            } else if (queryParams.query != null) {
                var query = JSON.parse(queryParams.query);
                var findQuery = query.query;
                var pipeLine = query.pipeline;
                if (pipeLine) {
                    console.log("Pipeline comes" + JSON.stringify(pipeLine));
                    for (var i = 0; i < pipeLine.length; i++) {
                        var pipe = pipeLine[i];
                        if (pipe.$match) {
                            var pipeMatch = pipe.$match;
                            Object.keys(pipeMatch).forEach(function (k) {
//                                if(k==("_id")){
                                pipeMatch[k] = new ObjectID(pipeMatch[k].toString());
//                                }
                            })

                        }

                    }
                    DatabaseEngine.aggregate(query.database, query.collection, pipeLine, callback);
                } else {
                    if (findQuery) {

                        Object.keys(findQuery).forEach(function (k) {
                            if (k.indexOf("_id") >= 0) {
                                findQuery[k] = new ObjectID(findQuery[k].toString());
                            }
                        })

                    } else {
                        findQuery = {};
                    }
                    DatabaseEngine.find(query.database, query.collection, findQuery, query.options, callback);
                }

            } else if (queryParams.remove != null) {
                var remove = JSON.parse(queryParams.remove);
                var findQuery = remove.query;
                if (findQuery) {

                    if (findQuery._id) {
                        findQuery._id = new ObjectID(findQuery._id);
                    }
                } else {
                    findQuery = {};
                }
                DatabaseEngine.remove(remove.database, remove.collection, findQuery, callback);
            } else if (queryParams.table != null) {
                var table = JSON.parse(queryParams.table);
                MetadataEngine.getTable(table.id, callback);

            } else if (queryParams.generateview != null) {
//                var table = JSON.parse(queryParams.generateview);
//                console.log(JSON.stringify(table));
//                MetadataEngine.getTable(table.id, {"ask":table.ask}, function (err, t) {
//
//                    TableJob.saveView(t.table, table.ask, callback);
//                });  var table = JSON.parse(queryParams.generateview);
//                console.log(JSON.stringify(table));
//                MetadataEngine.getTable(table.id, {"ask":table.ask}, function (err, t) {
//
//                    TableJob.saveView(t.table, table.ask, callback);
//                });

            } else if (queryParams.cachekey != null) {
                var cacheKey = JSON.parse(queryParams.cachekey);
                var clear = cacheKey.clear;
                cacheKey = cacheKey.id;
                var value = CacheService.get(cacheKey);
                if (clear) {
                    CacheService.clear(cacheKey);
                }
                res.writeHead(200, responseType);
                res.write(JSON.stringify({cache:value ? value : "Not found in cache"}));
                res.end();
            } else {
                res.writeHead(404, responseType);
                res.write("404 not found");
                res.end();
            }

        } catch (err) {
            res.writeHead(404, responseType);
            res.write("Error[" + err.stack + "]");
            res.end();
        }


    }).listen(8888);
console.log('Server running at port : 8888');