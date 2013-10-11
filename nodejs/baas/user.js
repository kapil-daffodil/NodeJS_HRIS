var http = require('http');
var urlParser = require('url');


http.createServer(
    function (req, res) {
        try {
            var url = urlParser.parse(req.url, true);
            var queryParams = url.query;

            var responseType = {"Content Type":"applicatoin/json", "Access-Control-Allow-Origin":"*", "Access-Control-Allow-Methods":"GET, POST, OPTIONS"};
            res.writeHead(200, responseType);
            var response = {"response":{         "menus":[
                {"_id":3, "label":"Applications", "table":{"_id":20561, "id":"taskmanager_tasks", "organizationspecific":1, "label":"Applications", "table":"taskmanager_tasks", "primarycolumn":"_id"}, "applicationid":{"_id":1151, "id":"task_manager", "oauthkey":"b0a25e52-8af3-431b-a362-196272a0fa13", "label":"Task Manager", "primarycolumn":"_id"}, "primarycolumn":"_id"}
            ], "organization":{"_id":5, "organization":"Global", "oauthkey":"f7122da8-8475-4bee-ad9a-0130cd0af0a3", "label":"Global", "primarycolumn":"_id"}, "username":"rohit.bansal@daffodilsw.com", "organizations":[
                {"_id":5, "organization":"Global", "oauthkey":"f7122da8-8475-4bee-ad9a-0130cd0af0a3", "label":"Global", "primarycolumn":"_id"}
            ], "uagname":"afb", "menu":{"_id":3, "label":"Applications", "table":{"_id":20561, "id":"taskmanager_tasks", "organizationspecific":1, "label":"Applications", "table":"taskmanager_tasks", "primarycolumn":"_id"}, "applicationid":{"_id":1151, "id":"task_manager", "oauthkey":"b0a25e52-8af3-431b-a362-196272a0fa13", "label":"Task Manager", "primarycolumn":"_id"}, "primarycolumn":"_id"}, "viewgroups":[
                {"_id":2, "viewgroupid":{"primarycolumn":"_id", "_id":3, "viewgroup":"BaaS"}, "applicationid":{"primarycolumn":"_id", "_id":1151, "id":"task_manager", "oauthkey":"b0a25e52-8af3-431b-a362-196272a0fa13", "label":"BaaS"}, "primarycolumn":"_id"}
            ], "userid":1, "login":true,

                "view":{         "viewid":"taskmanager_tasks",
                    "data":{"cursor":"10", "requestid":"", "data":[]}, "ask":"baas",

                    "metadata":{

                        "columns":[
                            {"_id":001, "expression":"ask", "type":"string", "label":"ASK", "width":100, "visibility":"Both"},
                            {"_id":002, "expression":"id", "type":"string", "label":"ID", "width":100, "visibility":"Both"},
                            {"_id":003, "expression":"label", "type":"string", "label":"Label", "width":100, "visibility":"Both"},
                            {"_id":004, "expression":"orgenabled", "type":"boolean", "label":"Org enabled", "width":100, "visibility":"Both"} ,
                            {"_id":004, "expression":"customcodeurl", "type":"string", "label":"Custom code url", "width":200, "visibility":"Both"}
                        ],
                        "primarycolumn":"_id",
                        "label":"Applications",
                        "table":{"id":"applications__baas", "_id":20561},
                        "type":"table",
                        "childs":[
                            {"_id":1, "id":"developers", "relatedcolumn":"_id", "visibility":"Both", "label":"Manage developers", "table":{"_id":"51ece1cdacf26b5018000005", "id":"applications__baas"}, "metadata":{"table":{"_id":"51ece1cdacf26b5018000005", "id":"applications__baas"}, "type":"table", "filter":{"_id":"{_id}"},

                                "columns":[
                                    {"expression":"developers._id", "type":"string", "label":"Userid", "visibility":"Query", "width":200},
                                    {"expression":"developers.username", "type":"lookup", "label":"User name", "visibility":"Both", "width":200, "options":["pawan.phutela@daffodilsw.com", "rohit.bansal@daffodilsw.com"]},

                                    {"expression":"developers.role", "type":"lookup", "label":"Role", "visibility":"Both", "width":200, "options":["admin", "developer"]}
                                ], "primarycolumn":"developers._id"}, "data":{"data":[]}, "applicationid":"baas", "ask":"baas"},
                            {"_id":2, "id":"table", "relatedcolumn":"_id", "visibility":"Both", "label":"Manage Table", "table":{"_id":"51ece1cdacf26b5018000005", "id":"applications__baas"}, "metadata":{"table":{"_id":"51ece1cdacf26b5018000009", "id":"applications__baas"}, "type":"table", "filter":{"_id":"{_id}"}, "columns":[
                                {"expression":"tables._id", "type":"string", "label":"Table Id", "visibility":"Query", "width":200},
                                {"expression":"tables.id", "type":"string", "label":"Id", "visibility":"Both", "width":200},
                                {"expression":"tables.orgenabled", "type":"boolean", "label":"Org Enabled", "visibility":"Both", "width":200}
                            ], "primarycolumn":"tables._id",

                                "childs":[
                                    {"_id":8, "id":"columns", "relatedcolumn":"_id", "visibility":"Both", "label":"Manage Columns", "table":{"_id":"51ece15facf26b5018000003", "id":"tables__baas"}, "metadata":{"table":{"_id":"51ece15facf26b5018000003", "id":"tables__baas"}, "type":"table", "filter":{"_id":"{tables._id}"}, "columns":[
                                        {"expression":"columns.expression", "type":"string", "label":"Expression", "visibility":"Both", "width":200},
                                        {"expression":"columns.table", "type":"lookup", "label":"Table", "visibility":"Both", "width":200, "columns":[
                                            {"expression":"id", "type":"string"}
                                        ], "table":"tables__baas"},
                                        {"expression":"columns.primary", "type":"boolean", "label":"Primary", "visibility":"Both", "width":200},
                                        {"expression":"columns.type", "type":"lookup", "label":"Type", "visibility":"Both", "width":200, "options":["string", "lookup", "boolean", "object"]},
                                        {"expression":"columns.replicate", "type":"boolean", "label":"Replicate", "visibility":"Both", "width":200},
                                        {"expression":"columns.multiple", "type":"boolean", "label":"Multiple", "visibility":"Both", "width":200},
                                        {"expression":"columns._id", "type":"string", "label":"Id", "visibility":"Query", "width":200}
                                    ], "primarycolumn":"columns._id"},

                                        "data":{"data":[]}, "applicationid":"baas", "ask":"baas"} ,
                                    {"_id":9, "id":"jobs", "relatedcolumn":"_id", "visibility":"Both", "label":"Manage Jobs", "table":{"_id":"51ece15facf26b5018000003", "id":"tables__baas"}, "metadata":{"table":{"_id":"51ece15facf26b5018000003", "id":"tables__baas"}, "type":"table", "filter":{"_id":"{tables._id}"}, "columns":[
                                        {"expression":"jobs.id", "type":"string", "label":"Id", "visibility":"Both", "width":200},
                                        {"expression":"jobs.module", "type":"string", "label":"Module", "visibility":"Both", "width":200},
                                        {"expression":"jobs.operation", "type":"lookup", "label":"Type", "visibility":"Both", "width":200, "options":["insert", "update", "delete"]},
                                        {"expression":"jobs.when", "type":"lookup", "label":"Type", "visibility":"Both", "width":200, "options":["before", "after"]},
                                        {"expression":"jobs._id", "type":"string", "label":"Id", "visibility":"Query", "width":200}
                                    ], "primarycolumn":"jobs._id"},

                                        "data":{"data":[]}, "applicationid":"baas", "ask":"baas"}
                                ]}, "data":{"data":[]}, "applicationid":"baas", "ask":"baas"}
                        ]



                    },


                    "applicationid":"baas"}, "viewgroup":{"_id":2, "viewgroupid":{"_id":3, "viewgroup":"BaaS", "primarycolumn":"_id"}, "applicationid":{"_id":1151, "id":"task_manager", "oauthkey":"b0a25e52-8af3-431b-a362-196272a0fa13", "label":"BaaS", "primarycolumn":"_id"}, "primarycolumn":"_id"}                  }, "status":"ok", "code":200};

            res.write(JSON.stringify(response));
            res.end();


        } catch (err) {
            res.writeHead(404, responseType);
            res.write("Error[" + err.stack + "]");
            res.end();
        }


    }).listen(5100);
console.log('Server running at port : 5100');