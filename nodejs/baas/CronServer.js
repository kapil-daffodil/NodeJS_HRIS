var DatabaseEngine = require("./lib/database/DatabaseEngine.js");
var UpdateEngine = require("./lib/database/UpdateEngine.js");
var CustomCodeManager = require("./lib/customcode/customcodemanager.js");
var MetadataEngine = require("./lib/metadata/MetadataProvider.js");
var nodemailer = require("nodemailer");
var APIConstants = require('nodejsapi/Constants.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var BaasError = require("apputil/ApplaneError.js");
var MailService = require("./lib/mail/MailService.js");
var Constants = require("./lib/shared/Constants.js");
var LogUtility = require("./lib/util/LogUtility.js");
var http = require('http');
var QueryString = require("querystring");
var Utils = require("apputil/util.js");


//process.on('uncaughtException', function (err) {
//    console.log("Uncaught exception arrives......")
//    var options = {};
//    options.to = "rohit.bansal@daffodilsw.com";
//    options.subject = "Uncaught exception on CronServer";
//    options.text = err.stack;
//    var transport = nodemailer.createTransport("SMTP", {auth:{user:"developer@daffodilsw.com", pass:"dak2applane"}, service:"gmail"});
//    transport.sendMail(options, function () {
//
//    });
//
//});

setInterval(function () {
    //find all cron jobs
    DatabaseEngine.query({"query":{
        "table":"crons__baas"
    }, "ask":"baas", "callback":function (err, records) {
        if (err) {
            console.log(err.stack);
            return;
        }
        var crons = records.data;
        var count = crons ? crons.length : 0;
        for (var i = 0; i < count; i++) {
            var cron = crons[i];
            var dueDate = cron.when.duedate;
            var dueDate = cron.when.duedate;
            var lastRunDate = cron.lastrundate;
            var date = new Date();
            console.log("Now: " + date);
            console.log("Last: " + lastRunDate);
            console.log("DueDate: " + dueDate);
            if (date >= dueDate && (!lastRunDate || dueDate > lastRunDate)) {
                console.log("We have to run cron[" + cron.label + "]");
                if (cron.module) {
                    runPathCron(cron);
                } else {
                    runCron(cron);
                }
            } else {
                console.log("We will not run cron[" + cron.label + "]");
            }
        }
    }
    });
}, 900000);

runCron = function (cron) {
    var application = cron.applicationid;
    MetadataEngine.getApplication(application.ask, function (err, application) {
        if (err) {
            console.log("errorr while get Application " + err.stack);
        } else {
            var orgEnabled = application.orgenabled;
            if (orgEnabled) {
                getOrganizations(application._id, ask, function (err, organizations) {
                    if (err) {
                        console.log("error while fetching organizations" + err.stack);
                    } else {
                        if (organizations) {
                            for (var i = 0; i < organizations.length; i++) {
                                var organization = organizations[i];
                                setTimeout(function () {
                                    run(cron, application, organization);
                                }, 100);
                            }
                        }
                    }
                });
                updateLastRunDate(cron, function (err, result) {
                    if (err) {
                        console.log(err.stack);
                    }
                });
            } else {
                setTimeout(function () {
                    run(cron, application);
                }, 100);
                updateLastRunDate(cron, function (err, result) {
                    if (err) {
                        console.log(err.stack);
                    }
                });

            }
        }
    });
}
function runPathCron(cron) {
    updateLogs(cron, cron.applicationid._id, null, "waiting for emit", null, null, function (err, result) {
        if (err) {
            console.log("Error while updating cron logs" + err.stack);
        } else {
            var insertedData = result["insert"];
            var cronLogsId = insertedData[0]._id;
            LogUtility.addLog(cron.applicationid._id, null, null, null, null, undefined, function (err, logid) {
                CustomCodeManager.callJobs({
                    "ask":cron.applicationid.ask,
                    "module":cron.module,
                    "method":cron.method,
                    "parameters":{
                        logid:logid
                    },
                    "callback":function (err, response) {
                        var status = "OK";
                        if (err) {
                            status = err.stack;
                        }
                        var userLogs = [];
                        var logs = LogUtility.getLogs(logid);
                        for (var i = 0; i < logs.logs.length; i++) {
                            var log = logs.logs[i];
                            userLogs.push({log:log.log});
                        }
                        var currentDate = new Date();
                        //update this entry
                        UpdateEngine.performUpdate({
                            updates:{
                                table:"crons__baas",
                                operations:[
                                    {
                                        _id:cron._id, lastrundate:currentDate
                                    }
                                ]
                            }, ask:"baas",
                            callback:function (err, updateResponse) {
                                UpdateEngine.performUpdate({
                                    updates:{
                                        table:"cronlogs__baas",
                                        operations:[
                                            {
                                                id:cronLogsId,
                                                logs:userLogs,
                                                rundate:currentDate,
                                                status:status,
                                                cronid:cron._id
                                            }
                                        ]
                                    }, ask:"baas",
                                    callback:function (err, updateResponse) {
                                        console.log("Cron logs created successfully for " + cron.label);
                                    }
                                });
                            }
                        });


                    }
                });
            });
        }
    });
}

function run(cron, application, organization) {
    try {
        var osk;
        if (organization) {
            osk = organization.osk;
        }
        var options = {
            "ask":application.ask,
            "osk":osk,
            "application":application,
            "organization":organization
        };
        if (cron.query) {
            var query = JSON.parse(cron.query);
        }
        if (query) {
            var queryData = {};
            var keys = Object.keys(query);
            Utils.iterateArray(keys, function (err, result) {
                if (err) {
                    updateLogs(cron, application, organization, err.stack);
                } else {
                    executeCode(cron, queryData, options);
                }
            }, function (key, callback) {
                var alias = key;
                var userQuery = query[key];
                getQueryData(userQuery, application.ask, osk, function (err, result) {
                    if (err) {
                        callback(err);
                    } else {
                        queryData[alias] = result;
                        callback();
                    }
                });
            });
        }
        else {
            executeCode(cron, null, options);
        }
    } catch (err) {
        console.log(err.stack);
        updateLogs(cron, application, organization, err.stack);
    }
}


function executeCode(cron, queryData, options) {
    updateLogs(cron, options.application, options.organization, "waiting for emit", null, null, function (err, result) {
        if (err) {
            console.log("Error while updating cron logs" + err.stack);
        } else {
            var logs = [];
            var insertedData = result["insert"];
            var cronLogsId = insertedData[0]._id;
            new function () {
                if (queryData) {
                    for (var key in queryData) {
                        this[key] = queryData[key];
                    }
                }
                try {
                    Function("JobError", "writeLog", "sendMail", "httpCall", "emit", cron.code).apply(this, [BaasError, function (log) {
                        logs.push({log:log});
                    } , sendMail(options), httpCall(), function (err, updates) {
                        if (err) {
                            console.log(err.stack);
                            updateLogs(cron, options.application, options.organization, err.stack, logs, cronLogsId);
                        } else if (updates) {
                            CustomCodeManager.executeUpdates(updates, options, function (err, result) {
                                if (err) {
                                    updateLogs(cron, options.application, options.organization, err.stack, logs, cronLogsId);
                                } else {
                                    updateLogs(cron, options.application, options.organization, "success", logs, cronLogsId);
                                }
                            });
                        } else {
                            updateLogs(cron, options.application, options.organization, "success", logs, cronLogsId);
                        }
                    }]);
                } catch (err) {
                    console.log(err.stack);
                }
            }
        }
    });
}

function getQueryData(query, ask, osk, callback) {
    DatabaseEngine.query({
        query:query,
        "osk":osk,
        "ask":ask,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data);
            } else {
                callback();
            }
        })
    })
}

function getOrganizations(applicationId, ask, callback) {
    var query = {};
    var filter = {};
    query[APIConstants.Query.TABLE] = "organizations__baas";
    filter["applications"] = applicationId;
    query[APIConstants.Query.FILTER] = filter;
    DataBaseEngine.query({
        "ask":ask,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data);
            } else {
                callback();
            }
        })
    })
}

var sendMail = function (options) {
    return function (mailcontents) {
        MailService.sendMail(mailcontents, {application:options.application, oraganization:options.organization});
    }
}

var getAppUsers = function (options) {
    return function (callback) {
        getUsers(options, callback);
    }
}

function updateLastRunDate(cron, callback) {
    var currentDate = new Date();
    UpdateEngine.performUpdate({
        updates:{
            table:"crons__baas",
            operations:[
                {
                    _id:cron._id, lastrundate:currentDate
                }
            ]
        }, ask:"baas",
        callback:function () {
        }
    });
}

function updateLogs(cron, application, organization, status, logs, cronLogsId, callback) {
    var currentDate = new Date();
    var organizationid;
    if (organization) {
        organizationid = organization._id;
    }
    UpdateEngine.performUpdate({
        updates:{
            table:"cronlogs__baas",
            operations:[
                {
                    _id:cronLogsId,
                    rundate:currentDate,
                    status:status,
                    cronid:cron._id,
                    applicationid:application._id,
                    organizationid:organizationid,
                    logs:logs
                }
            ]
        },
        ask:"baas",
        callback:(callback || function (err) {
            if (err) {
                console.log(err.stack);
            }
        })
    })
    ;
}


function getUsers(options, callback) {
    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.USERS;
    var filter = {};
    filter[Constants.Baas.Users.APPLICATIONS] = options.application._id;
    if (options.organization) {
        filter[Constants.Baas.Users.Application.ORGANIZATION] = options.organization._id;
    }

    query[APIConstants.Query.FILTER] = filter;
    DatabaseEngine.query({
        "ask":options.ask,
        "osk":options.osk,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            var data = result[APIConstants.Query.Result.DATA];
            if (data && data.length > 0) {
                callback(null, data);
            } else {
                callback();
            }
        })
    })
}


var httpCall = function () {
    return function (service, params, callback) {
        call(service, params, callback);
    }
}


call = function (service, params, callback) {
    var path = service.path
    var queryString = "";
    if (Object.keys(params).length > 0) {
        queryString = QueryString.stringify(params);
    }
    var serverOptions = {
        hostname:service.hostname,
        port:service.port,
        path:path,
        method:service.method,
        headers:{
            'Content-Type':'application/x-www-form-urlencoded',
            'Content-Length':queryString.length
        }
    };
    console.log("serverOptions>>>" + JSON.stringify(serverOptions));
    var req = http.request(serverOptions, function (res) {
        if (params.response) {
            res.setEncoding('binary');
        } else {
            res.setEncoding('utf8');
        }
        var body = '';
        res.on('data', function (chunk) {
            body += chunk;
        });
        res.on('end', function () {
            callback(null, body);
        });

    });
    req.on('error', function (err) {
        console.log("err>>" + err);
        callback(err);
    });
    req.write(queryString);
    req.end();
}
