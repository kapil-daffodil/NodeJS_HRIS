/**
 * Created with IntelliJ IDEA. User: Administrator Date: 7/16/13 Time: 1:19 PM To change this template use File | Settings | File Templates.
 */

var MongoDB = require('mongodb');
var MongoClient = MongoDB.MongoClient;
var ObjectID = MongoDB.ObjectID;
var BaasError = require("apputil/ApplaneError.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var Constants = require('../../shared/Constants.js');
var GridStore = require('mongodb').GridStore;
var ObjectID = require('mongodb').ObjectID;
var databases = {};
var Formidable = require("formidable");
var Utils = require("apputil/util.js");
var LogUtility = require("../../util/LogUtility.js");

var DATABASE_URL = "mongodb://173.255.119.199:27017/";


exports.find = function (database, collection, query, options, callback) {
    if ('function' === typeof options)
        callback = options, options = {};
    if (!options) {
        options = {};
    }
    var startTime = new Date().getTime();
    connect(database, ApplaneCallback(callback, function (db) {
        var collectionObject = db.collection(collection);
        collectionObject.find(query, options).toArray(ApplaneCallback(callback, function (data) {
            var endTime = new Date().getTime() - startTime;
            LogUtility.writeLog(options.logid, {"Find >> Database":database, "collection":collection, "query":query, "options":options, "Total time":endTime});
            callback(null, data);
        }));
    }))
};

exports.findOne = function (database, collection, query, options, callback) {
    if ('function' === typeof options)
        callback = options, options = {};
//    console.log("FIND One >> database>>" + database + " collection >> " + collection + " query>>" + JSON.stringify(query) + " options >> " + JSON.stringify(options));
    connect(database, ApplaneCallback(callback, function (db) {
        var collectionObject = db.collection(collection);
        collectionObject.findOne(query, options, callback);
    }))
};

exports.aggregate = function (database, collection, pipeline, options, callback) {
    if ('function' === typeof options)
        callback = options, options = {};
    if (!options) {
        options = {};
    }
    var startTime = new Date().getTime();
    connect(database, ApplaneCallback(callback, function (db) {
        var collectionObject = db.collection(collection);
        collectionObject.aggregate(pipeline, ApplaneCallback(callback, function (data) {
            var endTime = new Date().getTime() - startTime;
            LogUtility.writeLog(options.logid, {"Aggregate >> database":database, "collection":collection, "pipeline":pipeline, "Total Time":endTime});
            callback(null, data);
        }));
    }));
}

exports.insert = function (database, collection, inserts, options, callback) {
    if ('function' === typeof options)
        callback = options, options = {};
//    console.log("Insert >> database>>" + database + " collection >> " + collection + "  Inserts >>" + JSON.stringify(inserts));
    connect(database, ApplaneCallback(callback, function (db) {
        var collectionObject = db.collection(collection);
        collectionObject.insert(inserts, {
            w:1
        }, function (err, result) {
            if (err) {
                var status = (err instanceof Object) ? err.code : -1;
                if (status && (status == 11000 || status == 11001)) {
                    var duplicateValue = getDuplicateValue(err);
                    err = new BaasError(Constants.ErrorCode.UNIQUE_VALIDATION_FAILED, [duplicateValue]);
                }
                callback(err);
            } else {
                callback(null, result);
            }
        });
    }))
};

exports.update = function (database, collection, query, updates, options, callback) {
//    console.log(">>>>>>>>>>Updates>>>>>>" + JSON.stringify(updates));
    if ('function' === typeof options)
        callback = options, options = {};
    options.w = 1;
    connect(database, ApplaneCallback(callback, function (db) {
        var collectionObject = db.collection(collection);
        collectionObject.update(query, updates, options, function (err, result) {
            LogUtility.writeLog(options.logid, {update:true, collection:collection, query:query, updates:updates});
            if (err) {
                var status = (err instanceof Object) ? err.code : -1;
                if (status && (status == 11000 || status == 11001)) {
                    var duplicateValue = getDuplicateValue(err);
                    err = new BaasError(Constants.ErrorCode.UNIQUE_VALIDATION_FAILED, [duplicateValue]);
                }
                callback(err);
            } else {
                callback(null, result);
            }
        });
    }))
};
function getDuplicateValue(err) {
    var dup;
    var errorDetail = err.err;
    if (errorDetail) {
        dup = errorDetail.substring(errorDetail.indexOf("dup key:"));
    }
    return dup;
}


exports.remove = function (database, collection, query, options, callback) {
    if ('function' === typeof options)
        callback = options, options = {};
    connect(database, ApplaneCallback(callback, function (db) {
        var collectionObject = db.collection(collection);
        collectionObject.remove(query, {
            w:1
        }, callback);
    }));
};

exports.uploadFile = function (database, fileName, dataArray, callback) {
    connect(database, ApplaneCallback(callback, function (db) {
        var objectId = new ObjectID();
        var gridStore = new GridStore(db, objectId, fileName, "w");
        gridStore.open(ApplaneCallback(callback, function () {
            Utils.iterateArray(dataArray, ApplaneCallback(callback, function () {
                gridStore.close(ApplaneCallback(callback, function () {
                    callback(null, objectId.toString());
                }));
            }), function (buffer, callback) {
                gridStore.write(buffer, callback);
            });
        }));
    }));
};

exports.downloadFile = function (database, fileKey, callback) {
    fileKey = new ObjectID(fileKey.toString());
    connect(database, ApplaneCallback(callback, function (db) {
        var gridStore = new GridStore(db, fileKey, "r");
        gridStore.open(ApplaneCallback(callback, function () {
            gridStore.seek(0, ApplaneCallback(callback, function () {
                gridStore.read(ApplaneCallback(callback, function (data) {
                    callback(null, {metadata:{filename:gridStore.filename, contentType:gridStore.contentType}, data:data});
                }));
            }));
        }));
    }));
};

exports.deleteFle = function (database, fileKey, callback) {
    fileKey = new ObjectID(fileKey.toString());
    connect(database, ApplaneCallback(callback, function (db) {
        var gridStore = new GridStore(db, fileKey, "w");
        gridStore.open(ApplaneCallback(callback, function () {
            gridStore.unlink(callback);
        }));
    }));
};

/**
 * Using this method you can save indexes of various types which are supported in MongoDB
 * @param database  Name of the database
 * @param table  Name of the table
 * @param indexedFields
 * @param property
 * @param callback callback function
 */
exports.synchronizeIndexes = function (database, table, indexedFields, property, callback) {
//    console.log("Unix Index Add>>Database" + database + " Table:" + table + " primaryColumns: " + JSON.stringify(primaryColumns));
    connect(database, ApplaneCallback(callback, function (db) {
            var collectionObject = db.collection(table);
            collectionObject.ensureIndex(indexedFields, property, callback);
        }
    ));
}


exports.synchronizeUniqueIndexes = function (database, table, primaryColumns, callback) {
//    console.log("Unix Index Add>>Database" + database + " Table:" + table + " primaryColumns: " + JSON.stringify(primaryColumns));
    connect(database, ApplaneCallback(callback, function (db) {
        var indexName = database + "_" + table + "_unique";
        var collectionObject = db.collection(table);
        collectionObject.indexes(ApplaneCallback(callback, function (indexes) {
//            console.log("Existing Indexes>>>>>>>>>>>>" + JSON.stringify(indexes));
            var indexProperties = {unique:true, background:true, name:indexName, dropDups:true};
            var uniqueIndex = indexes.length > 1 ? getUniqueIndex(indexes, indexName) : null;
            if (uniqueIndex && reIndexingRequired(uniqueIndex, primaryColumns)) {
                collectionObject.dropIndex(indexName, ApplaneCallback(callback, function () {
                    collectionObject.ensureIndex(primaryColumns, indexProperties, callback);
                }));
            } else {
                collectionObject.ensureIndex(primaryColumns, indexProperties, callback);
            }
        }));
    }));
}

function getUniqueIndex(indexes, uniqueIndexName) {
    var length = indexes.length;
    for (var i = 0; i < length; i++) {
        var indexInfo = indexes[i];
        if (indexInfo.name == uniqueIndexName) {
            return indexInfo;
        }
    }
    return null;
}

function reIndexingRequired(uniqueIndex, primaryColumns) {
    var existingUniqueColumnNames = uniqueIndex.key;
    var existingUColLength = Object.keys(existingUniqueColumnNames).length;
    var updateUniqueColNames = Object.keys(primaryColumns);
    var updatedUniqueColLength = updateUniqueColNames.length;
    if (existingUColLength != updatedUniqueColLength) {
        return true;
    } else {
        for (var i = 0; i < updatedUniqueColLength; i++) {
            var key = updateUniqueColNames[i];
            var columnExist = existingUniqueColumnNames[key];
            if (!columnExist) {
                return true;
            }
        }
    }
    return false;
}

function connect(database, callback) {
    if (!database) {
        callback(new BaasError(Constants.ErrorCode.DATABASE_NOT_FOUND));
        return;
    }
    if (databases[database]) {
        callback(null, databases[database])
    } else {
        MongoClient.connect(DATABASE_URL + database, function (err, db) {
            if (err) {

                if (DATABASE_URL == 'mongodb://127.0.0.1:27017/' && err.stack.indexOf("failed to connect to [127.0.0.1:27017]") >= 0) {
                    DATABASE_URL = "mongodb://173.255.119.199:27017/";
                    connect(database, callback);
                } else {
                    callback(err);
                }

            } else {
                databases[database] = db;
                callback(null, db);
            }
        })


    }
}

exports.mapReduce = function (database, collection, mapFn, reduceFn, options, callback) {


    connect(database, ApplaneCallback(callback, function (db) {
        var collectionObject = db.collection(collection);
        collectionObject.mapReduce(mapFn, reduceFn, options, callback);
    }));

}
