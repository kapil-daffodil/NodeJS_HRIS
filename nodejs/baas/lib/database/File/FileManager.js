/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 8/13/13
 * Time: 2:22 PM
 * To change this template use File | Settings | File Templates.
 */

var MongoDBManager = require('../mongodb/MongoDBManager.js');
var RequestAuthenticator = require("../../server/RequestAuthenticator.js")
var Constants = require("../../shared/Constants.js");
var Formidable = require("formidable");
var Utils = require("apputil/util.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var BaasError = require("apputil/ApplaneError.js");

exports.uploadFiles = function (req, callback) {
    var ask, osk;
    var files = [];
    var fields = {};
    var form = new Formidable.IncomingForm();
    form.on('error', callback);
    form.on('field', function (name, val) {
        if (name == "ask") {
            ask = val;
        } else if (name == "osk") {
            osk = val;
        } else {
            fields[name] = val;
        }
    });

    form.onPart = function (part) {
        if (!part.filename) {
            form.handlePart(part);
            return;
        }
        var data = [];
        var fileName = part.filename;
        console.log("File name: " + fileName)
        part.on('data', function (buffer) {
            data.push(buffer);
        });

        part.on('end', function () {
            files.push({filename:fileName, data:data});
        });
    };

    form.on('end', function () {
        RequestAuthenticator.authenticate(ask, osk, ApplaneCallback(callback, function (data) {

            if (fields.contents) {
                var contents = fields.contents.split(',').pop();
                var fileBuffer = new Buffer(contents, "base64");
                var fileName = fields.name;
                files.push({filename:fileName, data:[fileBuffer]});
            }

            var application = data.application;
            var organization = data.organization;
            var database = null;
            if (!application[Constants.Baas.Applications.ORGENABLED]) {
                database = application[Constants.Baas.Applications.DB] ? application[Constants.Baas.Applications.DB] : Constants.Baas.DEFAULT_GLOBALDB;
            } else {
                database = organization[Constants.Baas.Organizations.DB] ? organization[Constants.Baas.Organizations.DB] : (application[Constants.Baas.Applications.DB] ? application[Constants.Baas.Applications.DB] : Constants.Baas.DEFAULT_ORGSHAREDDB);
            }
            var fileKeys = [];
            Utils.iterateArray(files, ApplaneCallback(callback, function () {
                callback(null, fileKeys);
            }), function (file, callback) {
                MongoDBManager.uploadFile(database, file.filename, file.data, ApplaneCallback(callback, function (fileKey) {
                    fileKeys.push({key:fileKey, name:file.filename});
                    callback();
                }))
            });
        }));
    });
    form.parse(req);
}

exports.downloadFile = function (fileKey, options, callback) {
    if (!fileKey) {
        callback(new BaasError(Constants.ErrorCode.FIELDS_BLANK, ["filekey"]));
        return;
    }
    if ('function' === typeof options) callback = options, options = {};
    RequestAuthenticator.authenticate(options.ask, options.osk, ApplaneCallback(callback, function (data) {
        var application = data.application;
        var organization = data.organization;
        var database = null;
        if (!application[Constants.Baas.Applications.ORGENABLED]) {
            database = application[Constants.Baas.Applications.DB] ? application[Constants.Baas.Applications.DB] : Constants.Baas.DEFAULT_GLOBALDB;
        } else {
            database = organization[Constants.Baas.Organizations.DB] ? organization[Constants.Baas.Organizations.DB] : (application[Constants.Baas.Applications.DB] ? application[Constants.Baas.Applications.DB] : Constants.Baas.DEFAULT_ORGSHAREDDB);
        }
        MongoDBManager.downloadFile(database, fileKey, callback);
    }));
}
