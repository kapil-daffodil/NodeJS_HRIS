/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 7/31/13
 * Time: 6:07 PM
 * To change this template use File | Settings | File Templates.
 */

var DataBaseEngine = require('./lib/database/DatabaseEngine.js');
var UpdateEngine = require('./lib/database/UpdateEngine.js');
var UserService = require("./lib/user/userservice.js");
var BaasError = require("apputil/ApplaneError.js");
var Constants = require('./lib/shared/Constants.js');
var jsonResponseType = {"Content Type":"application/json", "Access-Control-Allow-Origin":"*", "Access-Control-Allow-Methods":"GET, POST, OPTIONS", "Access-Control-Allow-Credentials":true, "SET-COOKIE":"rohit=bansal"};
var CustomCodeManager = require("./lib/customcode/customcodemanager.js");
var CustomCodeService = require("customcode/CustomCodeService.js");
var express = require('express');
var app = express();
var APIConstants = require('nodejsapi/Constants.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");

var urlParser = require('url');
var FileManager = require('./lib/database/File/FileManager.js');
var CacheService = require("./lib/cache/cacheservice.js");
var ExportService = require('./lib/export/ExportService.js');
var MailService = require("./lib/mail/MailService.js");
var LogUtility = require("./lib/util/LogUtility.js");
var UdtModule = require('./lib/modules/UDTModule.js');

var https = require("https");
var fs = require("fs");


process.on('uncaughtException', function (err) {
    var options = {};
    options[Constants.MailService.Options.TO] = "rohit.bansal@daffodilsw.com";
    options[Constants.MailService.Options.SUBJECT] = "Uncaught exception on BaaSServer";
    options[Constants.MailService.Options.TEXT] = err.stack;
    MailService.sendMail(options, function () {

    });
});


//filters

// filters
app.use(function (req, res, next) {
    var url = urlParser.parse(req.url, true);
    if (url.pathname == "/file/upload") {
        next();
    } else {
        express.bodyParser()(req, res, next);
    }
});


app.use(function (req, res, next) {
    var usk = req.param("usk");
    if (usk && usk.toString().trim().length > 0) {
        var user = CacheService.get(usk);
        if (!user) {
            getUserFromSession(usk, function (err, result) {
                if (err) {
                    writeJSONResponse(res, err);
                    return;
                } else if (!result) {
                    writeJSONResponse(res, new BaasError(Constants.ErrorCode.SESSION_NOT_FOUND));
                    return;
                } else {
                    var userId = result.user;
                    var lastActivityDateInSession = result.lastactivitydate;
                    getUser(userId, function (err, user) {
                        if (err) {
                            writeJSONResponse(res, err);
                            return;
                        } else {
                            user.usk = usk;
                            user.lastactivitydate = lastActivityDateInSession;
                            UserService.addDeveloperAndAdmins(user, function (err, res) {
                                if (err) {
                                    writeJSONResponse(res, err);
                                    return;
                                } else {
                                    updateLastActivity(user);
                                    CacheService.put(usk, user);
                                    next();
                                }
                            })
                        }
                    });
                }
            });
        } else {
            updateLastActivity(user);
            next();
        }
    } else {
        next();
    }

});

updateLastActivity = function (user) {
    var lastActivityDate = new Date(user.lastactivitydate);
    var currentDate = new Date();
    var lastActivity = (lastActivityDate.getYear() + 1900) + "-" + (lastActivityDate.getMonth() + 1) + "-" + lastActivityDate.getDate();
    var current = (currentDate.getYear() + 1900) + "-" + (currentDate.getMonth() + 1) + "-" + currentDate.getDate();
    if (Date.parse(lastActivity) != Date.parse(current)) {
        getUserFromSession(user.usk, function (err, result) {
            if (result) {
                var sessionId = result[APIConstants.Query._ID];
                updateSession(sessionId);
            }
        });
    }
}

//servlet
app.all("/data", function (req, res) {
    var startTime = new Date().getTime();
    var usk = req.param("usk");
    var ask = req.param("ask");
    var osk = req.param("osk");
    var user = usk ? CacheService.get(usk) : null;
    var state = req.param("state");
    state = state ? JSON.parse(state) : state;
    LogUtility.checkLog(req, user, function (err, logid, isNew) {
        if (err) {
            writeJSONResponse(res, err);
        } else {
            if (req.param("query")) {
                DataBaseEngine.query({
                    ask:ask,
                    osk:osk,
                    usk:usk,
                    user:user,
                    state:state,
                    logid:logid,
                    query:JSON.parse(req.param("query")),
                    callback:function (err, result) {
                        var logs;
                        if (logid && isNew) {
                            logs = LogUtility.getLogs(logid);
                            LogUtility.removeLogs(logid);
                        }
                        writeJSONResponse(res, (err ? err : result), (new Date().getTime() - startTime), logs);
                    }
                });
            } else if (req.param("updates")) {
                console.log("Updates :[" + req.param("updates") + "]")
                UpdateEngine.performUpdate({
                    ask:ask,
                    osk:osk,
                    usk:usk,
                    user:user,
                    state:state,
                    logid:logid,
                    updates:JSON.parse(req.param("updates")),
                    callback:function (err, result) {
                        var logs;
                        if (logid && isNew) {
                            logs = LogUtility.getLogs(logid);
                            LogUtility.removeLogs(logid);
                        }
                        writeJSONResponse(res, (err ? err : result), (new Date().getTime() - startTime), logs);
                    }
                });
            } else {
                res.writeHead(404, {"Content Type":"text/plain"});
                res.write("404 not found");
                res.end();
            }
        }
    })
});

app.all("/login", function (req, res) {
    UserService.login(req.param("username"), req.param("password"), {ask:req.param("ask"), osk:req.param("osk"), usergroup:req.param("usergroup")}, function (err, user) {
        writeJSONResponse(res, (err ? err : user));
    })
});

app.all("/createuser", function (req, res) {
    var phone_no = req.param("phoneno");
    var emailid = req.param("emailid");

    var firstName = req.param("firstname");
    var lastName = req.param("lastname");
//    var picture = req.param("picture");
    var organization = req.param("organization");
    var designation = req.param("designation");
    var membershipDate = req.param("membershipdate");
    var renewalDate = req.param("renewaldate");
    var isAdmin = req.param("isadmin");
    var flexColumns = req.param("extracolumns");
    if (flexColumns) {
        flexColumns = JSON.parse(flexColumns);
    }


    var extraColumns = {};
    extraColumns.phoneno = phone_no;
    extraColumns.emailid = emailid;
    extraColumns.firstname = firstName;
    extraColumns.lastname = lastName;
    extraColumns.organization = organization;
    extraColumns.designation = designation;
    extraColumns.membershipdate = membershipDate;
    extraColumns.renewaldate = renewalDate;
    extraColumns.isadmin = isAdmin ? isAdmin == 'true' ? true : false : null;


    UserService.createUser(req.param("username"), req.param("password"), extraColumns, flexColumns, {ask:req.param("ask"), osk:req.param("osk"), usergroup:req.param("usergroup")}, function (err, user) {
        writeJSONResponse(res, (err ? err : user));
    })
});

app.all("/log", function (req, res) {
    LogUtility.writeLog(req.param("logid"), req.param("log"));
    writeJSONResponse(res, "log added Successfully");
});

app.all("/currentuser", function (req, res) {
    UserService.getCurrentUser(req.param("usk"), function (err, user) {
        writeJSONResponse(res, (err ? err : user));
    })
});

app.all("/logout", function (req, res) {
    UserService.logout(req.param("usk"), function (err, message) {
        writeJSONResponse(res, (err ? err : message));
    })
});

app.all("/forgotpassword", function (req, res) {
    UserService.forgotPassword(req.param("username"), {ask:req.param("ask"), osk:req.param("osk"), usergroup:req.param("usergroup")}, function (err, code) {
        writeJSONResponse(res, (err ? err : code));
    })
});

app.all("/resetpassword", function (req, res) {
    UserService.resetPassword({usk:req.param("usk"), newPassword:req.param("pwd"), oldPassword:req.param("oldpwd"), forgotPasswordCode:req.param("fpcode")}, function (err, message) {
        writeJSONResponse(res, (err ? err : message));
    })
});

app.all("/conversionrates", function (req, res) {
    UdtModule.getConversionRates({ask:req.param("ask"), osk:req.param("osk")}, function (err, message) {
        writeJSONResponse(res, (err ? err : message));
    })
})

app.all("/custom/service", function (req, res) {
    var startTime = new Date().getTime();
    var module = req.param("module");
    var method = req.param("method");
    var ask = req.param("ask");
    var osk = req.param("osk");
    var usk = req.param("usk");
    var user = usk ? CacheService.get(usk) : null;
    var parameters = JSON.parse(req.param("parameters"));
    LogUtility.checkLog(req, user, function (err, logid, isNew) {
        if (err) {
            writeJSONResponse(res, err);
        } else {
            CustomCodeManager.callService({
                "module":module,
                "method":method,
                "ask":ask,
                "osk":osk,
                "usk":usk,
                "parameters":parameters,
                "callback":function (err, data) {
                    var logs;
                    if (logid && isNew) {
                        logs = LogUtility.getLogs(logid);
                        LogUtility.removeLogs(logid);
                    }
                    writeJSONResponse(res, (err ? err : data), (new Date().getTime() - startTime), logs);
                }
            });
        }
    });
});

app.all("/custom/module", function (req, res) {
    var startTime = new Date().getTime();
    var module = req.param("module");
    var method = req.param("method");
    var ask = req.param("ask");
    var osk = req.param("osk");
    var usk = req.param("usk");
    var user = usk ? CacheService.get(usk) : null;
    var parameters = {};
    if (req.param("parameters")) {
        parameters = JSON.parse(req.param("parameters"));
    }
    LogUtility.checkLog(req, user, function (err, logid, isNew) {
        if (err) {
            writeJSONResponse(res, err);
        } else {
            CustomCodeManager.callModule({
                "module":module,
                "method":method,
                "ask":ask,
                "osk":osk,
                "usk":usk,
                "parameters":parameters,
                "callback":function (err, data) {
                    var logs;
                    if (logid && isNew) {
                        logs = LogUtility.getLogs(logid);
                        LogUtility.removeLogs(logid);
                    }
                    writeJSONResponse(res, (err ? err : data), (new Date().getTime() - startTime), logs);
                }

            });
        }
    });
});

function handleBaasCustomCodeRequest(req, res) {
    CustomCodeService.handleRequest(req, res, function (module, appid) {
        var modulePath = "./lib/jobs/" + module;
        return require(modulePath);
    });
}
app.all("/module", handleBaasCustomCodeRequest);
app.all("/job", handleBaasCustomCodeRequest);
app.all("/service", handleBaasCustomCodeRequest);

///file handling
app.all("/file/upload", function (req, res) {
    FileManager.uploadFiles(req, function (err, fileKeys) {
        writeJSONResponse(res, err || fileKeys);
    })
});

app.all("/file/download", function (req, res) {
    FileManager.downloadFile(req.param("filekey"), {ask:req.param("ask"), osk:req.param("osk"), usk:req.param("usk")}, function (err, file) {
        if (err) {
            writeJSONResponse(res, err);
        } else {
            res.writeHead(200, {
                "Content Type":file.metadata.contentType,
                "Content-Disposition":"attachment; Filename=\"" + file.metadata.filename + "\"",
                "Access-Control-Allow-Origin":"*",
                "Access-Control-Allow-Methods":"GET, POST, OPTIONS",
                "Access-Control-Allow-Credentials":true
            });
            res.write(file.data);
            res.end();
        }
    });
});

app.all("/file/render", function (req, res) {
    var ask = req.param("ask");
    var osk = req.param("osk");
    var usk = req.param("usk");
    var fileKey = req.param("filekey");

    FileManager.downloadFile(fileKey, {ask:ask, osk:osk, usk:usk}, function (err, file) {
        if (err) {
            writeJSONResponse(res, err);
        } else {
            var fileName = file.metadata.filename;
            var extension = fileName.split('.').pop(),
                extensionTypes = {
                    'css':'text/css',
                    'gif':'image/gif',
                    'jpg':'image/jpeg',
                    'jpeg':'image/jpeg',
                    'js':'application/javascript',
                    'png':'image/png',
                    'mp4':"video/mp4"
                };


            var contentType = extensionTypes[extension];
            if (!contentType) {
                contentType = file.metadata.contentType;
            }
            console.log("content-type>>>" + contentType);
            res.writeHead(200, {

                "Content-Type":contentType,
                "Access-Control-Allow-Origin":"*",
                "Access-Control-Allow-Methods":"GET, POST, OPTIONS",
                "Access-Control-Allow-Credentials":true
            });
            res.write(file.data);
            res.end();
        }
    });
});

///export handling

app.all("/export", function (req, res) {
    ExportService.exportFile(req, function (err, buffer) {
        if (err) {
            writeJSONResponse(res, err);
        } else {
            res.writeHead(200, {
                "Content Type":"application/vnd.openxmlformats",
                "Content-Disposition":"attachment; Filename=\"" + (req.param("filename") || "ExportFile") + ".xlsx\"",
                "Access-Control-Allow-Origin":"*",
                "Access-Control-Allow-Methods":"GET, POST, OPTIONS",
                "Access-Control-Allow-Credentials":true
            });
            res.write(buffer);
            res.end();
        }
    })
});
app.all("/cache", function (req, res) {
    var cacheKey = JSON.parse(req.param("key"));
    var clear = req.param("clear");
    var value = CacheService.get(cacheKey);
    if (clear) {
        CacheService.clear(cacheKey);
    }
    writeJSONResponse(res, value);
});

app.listen(1337);


console.log('Server running at port : 1337');
function writeJSONResponse(res, result, time, logs) {
    if (result instanceof BaasError) {
        res.writeHead(417, jsonResponseType);
        res.write(JSON.stringify({response:result.message, status:"error", code:result.code, stack:result.stack, logs:logs}));
    } else if (result instanceof Error) {
        res.writeHead(417, jsonResponseType);
        res.write(JSON.stringify({response:Constants.ErrorCode.UNKNOWN_ERROR.message, status:"error", code:Constants.ErrorCode.UNKNOWN_ERROR.code, stack:result.stack, logs:logs}));
    } else {
        res.writeHead(200, jsonResponseType);
        res.write(JSON.stringify({response:result, status:"ok", code:200, time:time, logs:logs}));
    }
    res.end();
}


getUserFromSession = function (usk, callback) {
    var query = {};
    query[APIConstants.Query.TABLE] = "sessions__baas";
    query[APIConstants.Query.COLUMNS] = ["user", "lastactivitydate"];
    query[APIConstants.Query.FILTER] = {"usk":usk};
    DataBaseEngine.query({
        "ask":"baas",
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data[0]);
            } else {
                callback();
            }
        })
    })
}

updateSession = function (sessionId) {
    var value = {};
    value[APIConstants.Query._ID] = sessionId;
    value.lastactivitydate = new Date();
    var operations = [];
    operations.push(value);
    var newUpdates = {};
    newUpdates[APIConstants.Query.TABLE] = "sessions__baas";
    newUpdates[APIConstants.Update.OPERATIONS] = operations;
    UpdateEngine.perfo
    rmUpdate({
        "ask":"baas",
        updates:newUpdates,
        callback:function (err) {
            console.log("error while update session>>>>>>>>" + err.stack);
        }
    })
}


getUser = function (userid, callback) {
    var query = {};
    query[APIConstants.Query.TABLE] = "users__baas";
    query[APIConstants.Query.FILTER] = {"_id":userid};
    query[APIConstants.Query.EXCLUDE_JOBS] = true;
    DataBaseEngine.query({
        "ask":"baas",
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data[0]);
            } else {
                callback();
            }
        })
    })
}

