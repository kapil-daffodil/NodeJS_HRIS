var http = require('http');
var urlParser = require('url');
var NodejsAPI = require("nodejsapi");

var AppsStudioError = require("apputil/ApplaneError.js");
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var Constants = require("./lib/shared/Constants.js");
var ejs = require('ejs');
var Moment = require('moment');

var express = require('express');
var app = express();
var fs = require("fs");
var https = require("https");


var onRequest = function (req, res) {

    try {

        var url = urlParser.parse(req.url, true);
        var uri = url.pathname;
        var domain = req.headers.host;
        var queryParams = url.query;
        var responseType = {"Content-Type":"text/html", "Access-Control-Allow-Origin":"*", "Access-Control-Allow-Methods":"GET, POST, OPTIONS"};
        if (domain) {
            var param = {};
            param.domain = domain;
            param.uri = uri;
            param.queryparam = queryParams;
            getData(param, res, function (err, result) {
                if (err) {

                    res.writeHead(200, responseType);
                    var error = {code:err.code, message:err.message, stack:err.stack};
                    res.write(JSON.stringify(error));
                    res.end();
                } else {
                    res.writeHead(200, responseType);
                    res.write(result);
                    res.end();
                }
            });
        }
    } catch (err) {
        res.writeHead(404, responseType);
        res.write("Error[" + err.stack + "]");
        res.end();
    }
}
http.createServer(onRequest).listen(4500);
//http.createServer(onRequest).listen(4501);
try {
    https.createServer(options, app).listen(4502);
    var options = {key:fs.readFileSync('/mnt/drives/ssl_certificates/daffodilapps/ryans-key.pem'), cert:fs.readFileSync('/mnt/drives/ssl_certificates/daffodilapps/ssl-bundle.crt')};
} catch (e) {

}

//http.createServer(onRequest).listen(80);


function getResource(id, domainId, callback) {
    NodejsAPI.asQuery(
        {"table":Constants.Frontend.RESOURCES, "filter":{"id":id, "domainid":domainId}}, {"ask":Constants.Frontend.Application.ASK}
    ).execute(ApplaneCallback(callback, function (result) {

        var data = result[Constants.Frontend.Query.DATA].length > 0 ? result[Constants.Frontend.Query.DATA] : null;

        callback(null, data);
    }));
}

function getData(param, res, callback) {
    var domain = param.domain;
    var uri = param.uri;
    var queryParam = param.queryparam;
    var startTime = new Date().getTime();
    getDomain(domain, ApplaneCallback(callback, function (domainInfo) {

            var imageIndex = uri.indexOf("/images/");
            var jsIndex = uri.indexOf("/js/");
            var cssIndex = uri.indexOf("/css/");
            var fontIndex = uri.indexOf("/font/");
            if (imageIndex >= 0 || jsIndex >= 0 || cssIndex >= 0 || fontIndex >= 0) {
                var resourceName;
                if (imageIndex >= 0) {
                    resourceName = uri.substring(imageIndex + 8);
                } else if (jsIndex >= 0) {
                    resourceName = uri.substring(jsIndex + 4);
                } else if (cssIndex >= 0) {
                    resourceName = uri.substring(cssIndex + 5);
                } else if (fontIndex >= 0) {
                    resourceName = uri.substring(fontIndex + 6);
                }

                var extension = uri.split('.').pop(),
                    extensionTypes = {
                        'css':'text/css',
                        'gif':'image/gif',
                        'jpg':'image/jpeg',
                        'jpeg':'image/jpeg',
                        'js':'application/javascript',
                        'png':'image/png',
                        'svg':'image/svg+xml',
                        'ttf':'application/x-font-truetype',
                        'otf':'application/x-font-opentype',
                        'woff':'application/font-woff',
                        'eot':'application/vnd.ms-fontobject'
                    };
                var expires = Moment(new Date()).add("days", 7).format("ddd, DD MMM YYYY HH:MM:SS") + " GMT";
                if (domainInfo.path) {

                    var imagePath = domainInfo.path;
                    res.writeHead(200, {'Content-Type':extensionTypes[extension], Expires:expires});
                    res.end(fs.readFileSync('./' + imagePath + uri));
                    return;
                } else {
                    getResource(resourceName, domainInfo._id, ApplaneCallback(callback, function (resourceInfo) {
                        if (resourceInfo == null || resourceInfo.length == 0) {
                            throw new Error("Resource not found[" + resourceName + "]");

                        } else {
                            var fileInfo = resourceInfo[0].file;
                            if (fileInfo && fileInfo.length > 0) {
                                fileInfo = fileInfo[0];
                                NodejsAPI.renderFile({"filekey":fileInfo.key, "response":res, "ask":"frontend"}, ApplaneCallback(callback, function (fileBody) {
                                    res.writeHead(200, {'Content-Type':extensionTypes[extension], expires:expires});
                                    res.end(fileBody, "binary");
                                }));
                            }

                        }
                    }));
                }


                return;


            }

            if (uri.toString() == "/sitemap.xml") {
                var domainId = domainInfo._id;
                var html = '';
                getPages(domainId, ApplaneCallback(callback, function (pages) {
                    createSiteMap(pages, domain, ApplaneCallback(callback, function (html) {
                        res.writeHead(200, {'Content-Type':'application/xml'});
                        res.end(html);
                        return;
                    }));
                }));
                return;
            }


            getPageInfo(uri, domainInfo, ApplaneCallback(callback, function (pageInfo) {
                var templateId = pageInfo[Constants.Frontend.Pages.TEMPLATEID][Constants.Frontend.Templates.ID];
                var responseCode = pageInfo.responsecode;
                if (responseCode && responseCode.toString() != "200") {
                    var msg = Constants.ResponseCode.CODES[responseCode];
                    res.writeHead(responseCode, {'Content-Type':'text/html'});
                    res.end(responseCode + "-" + msg);
                    return;
                }
                // merging query and page parameters
                var pageParameters;
                if (pageInfo && pageInfo.parameters) {
                    pageParameters = pageInfo.parameters;
                } else {
                    pageParameters = {};
                }
                if (queryParam) {
                    for (var key in queryParam) {
                        pageParameters[key] = queryParam[key];
                    }
                }
                pageInfo.parameters = pageParameters;
                var templates = [];
                var resources = [];

                getTemplate(templateId, pageInfo, domainInfo, templates, resources, ApplaneCallback(callback, function (templateHtml) {

                    resolveTemplates(templateHtml, templates, domainInfo, pageInfo, resources, ApplaneCallback(callback, function (resolvedHtml) {
                        getHTML(pageInfo, resolvedHtml, domainInfo, resources, ApplaneCallback(callback, function (html) {
                            console.log("Total time to server [" + uri + "] is (" + (new Date().getTime() - startTime) + ")")
                            callback(null, html);
                        }));
                    }));
                }));
            }));
        }
    ));
}
function createSiteMap(pages, domain, callback) {
    var html = '';
    html += '<?xml version="1.0" encoding="UTF-8"?>';
    html += '\n';
    html += '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">';
    html += '\n';
    if (pages && pages.length > 0) {
        var noOfPages = pages.length;
        for (var i = 0; i < noOfPages; i++) {
            var pageData = pages[i];
            if (!pageData) {
                break;
            }
            var responseCode = pageData[Constants.Frontend.Pages.RESPONSECODE];
            if (!pageData[Constants.Frontend.Pages.DELETED] && !pageData[Constants.Frontend.Pages.UNPUBLISH] && (responseCode == undefined || responseCode.trim().length == 0 || responseCode == 200 )) {
                var siteMapUrl = "http://www." + domain + pageData[Constants.Frontend.Pages.URI];
                siteMapUrl = siteMapUrl.toLowerCase();
                var changeFrequency = pageData[Constants.Frontend.Pages.UPDATEFREQUENCY];
                changeFrequency = (changeFrequency == null || changeFrequency.trim().length == 0) ? "weekly" : changeFrequency;
                var priority = pageData[Constants.Frontend.Pages.PRIORITY];
                priority = (priority == null || priority.trim().length == 0) ? "0.5" : priority;
                var lastModify = pageData.__updatedon;
                var createdOn = pageData.__createdon;
                var modifyDate = lastModify == null ? createdOn : lastModify;
                html += '<url>';
                html += '\n';
                html += '<loc>' + siteMapUrl + '</loc>'
                html += '\n';
                html += '<lastmod>' + modifyDate + '</lastmod>';
                html += '\n';
                html += '<changefreq>' + changeFrequency + '</changefreq>';
                html += '\n';
                html += '<priority>' + priority + '</priority>';
                html += '\n';
                html += '</url>';
                html += '\n';
            }
        }
        html += '</urlset>';
    } else {
        html += '</urlset>';
    }
    return callback(null, html);
}


function getPages(domainId, callback) {
    var query = {};
    query[Constants.Frontend.Query.TABLE] = Constants.Frontend.PAGES;
    query[Constants.Frontend.Query.COLUMNS] = [Constants.Frontend.Pages.URI, Constants.Frontend.Pages.LASTUPDATE, Constants.Frontend.Pages.UPDATEFREQUENCY, Constants.Frontend.Pages.PRIORITY, "__updatedon", "_createdon", Constants.Frontend.Pages.DELETED, Constants.Frontend.Pages.UNPUBLISH, Constants.Frontend.Pages.RESPONSECODE];
    var filter = {};
    filter.domainid = domainId;
    filter.redirect_to = null;
    query[Constants.Frontend.Query.FILTER] = filter;
    NodejsAPI.asQuery(
        query, {"ask":Constants.Frontend.Application.ASK}
    ).execute(ApplaneCallback(callback, function (result) {
        var data = result[Constants.Frontend.Query.DATA].length > 0 ? result[Constants.Frontend.Query.DATA] : null;
        callback(null, data);
    }));
}

function resolveTemplates(html, templates, domainInfo, pageInfo, resources, callback) {
    var templateCount = templates ? templates.length : 0;
    if (templateCount == 0) {
        callback(null, html);
        return;
    }

    var templateIndex = -1;

    var templateCallback = ApplaneCallback(callback, function (tempHtml) {
        if (tempHtml) {
            var templateId = templates[templateIndex];
            html = html.replace("####" + templateId + "####", tempHtml);
        }
        templateIndex = templateIndex + 1;

        if (templateIndex == templateCount) {
            callback(null, html);
            return;
        } else {
            var interTemplates = [];

            getTemplate(templates[templateIndex], pageInfo, domainInfo, interTemplates, resources, ApplaneCallback(callback, function (interTemplateHTML) {
                resolveTemplates(interTemplateHTML, interTemplates, domainInfo, pageInfo, resources, ApplaneCallback(callback, function (resolvedInterTemplate) {
                    templateCallback(null, resolvedInterTemplate);
                }));
            }));
        }
    });
    templateCallback();
}

function getHTML(pageInfo, templateHtml, domainInfo, resources, callback) {
    //merging template level and website level resources
    var domainResources = domainInfo.resources;
    if (domainResources) {
        for (var i = 0; i < domainResources.length; i++) {
            var obj = domainResources[i];
            var resourceId = obj._id;
            if (resources.indexOf(resourceId.toString()) == -1) {
                resources.push(resourceId.toString());
            }
        }
    }
    var resourcesCount = resources.length;
    if (resourcesCount > 0) {
        getPageResources(resources, ApplaneCallback(callback, function (result) {
            if (result != undefined && result != null) {
                var html = getHTMLWithPageResources(result, templateHtml, pageInfo);
                callback(null, html);
            }
        }));
    } else {
        var html = getHTMLWithPageResources(null, templateHtml, pageInfo);
        callback(null, html);
    }
}

function getHTMLWithPageResources(resources, templateHtml, pageInfo) {

    var css = '';
    var js = '';
    var cssFiles = [];
    var jsFiles = [];
    if (resources != null && resources != undefined) {
        var resourcesCount = resources.length;

        for (var i = 0; i < resourcesCount; i++) {
            var resource = resources[i];

            if (resource != null && resource != undefined) {
                var resourceType = resource[Constants.Frontend.Resources.TYPE];
                var resouceContent = resource[Constants.Frontend.Resources.CONTENTS];
                var resourceId = resource[Constants.Frontend.Resources.ID];
                var file = resource[Constants.Frontend.Resources.FILE];
                if (resouceContent && resouceContent.length > 0) {
                    if (resourceType == Constants.Frontend.Resources.Type.JS) {
                        js += resouceContent;
                    } else if (resourceType == Constants.Frontend.Resources.Type.CSS) {
                        css += resouceContent;
                    }
                } else if (file && file.length > 0) {
                    if (resourceType == Constants.Frontend.Resources.Type.JS) {
                        jsFiles.push(resourceId)
                    } else if (resourceType == Constants.Frontend.Resources.Type.CSS) {
                        cssFiles.push(resourceId)
                    }
                }
            }
        }
    }
    if (templateHtml && templateHtml.indexOf("</html>") >= 0) {
        return templateHtml;
    } else {
        var html = '<!DOCTYPE HTML>';
        html += '<html>';
        html += '<head>';
        if (css && css.length > 0) {
            html += '<style type="text/css">';
            html += css;
            html += '</style>';
        }

        if (js != null && js != undefined) {
            var indexOfScript = js.indexOf("<script");
            if (indexOfScript < 0) {
                html += '<script type="text/javascript">';
            }
            html += js;
            if (indexOfScript < 0) {
                html += '</script>';
            }

        }

        if (cssFiles.length > 0) {
            for (var i = 0; i < cssFiles.length; i++) {
                var cssId = cssFiles[i];
                html += '<link href="/css/' + cssId + '" rel="stylesheet" />';
            }
        }

        if (jsFiles.length > 0) {
            for (var i = 0; i < jsFiles.length; i++) {
                var jsId = jsFiles[i];
                html += '<script src="/js/' + jsId + '" type="text/javascript" ></script>';
            }
        }
        html += '<title>' + pageInfo[Constants.Frontend.Pages.URITITLE] + '</title>';
        html += '<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />';
        html += '<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">';
        if (pageInfo[Constants.Frontend.Pages.METADESCRIPTION]) {
            html += '<meta name="description" content=' + pageInfo[Constants.Frontend.Pages.METADESCRIPTION] + '>';
        }
        html += '<link type="image/ico" href="/images/favicon.ico" rel="SHORTCUT ICON">';
        html += '</head>';

        html += '<body>';
        html += templateHtml;
        html += '</body>';
        html += '</html>';
        return html;
    }
}

function getPageResources(resources, callback) {
    var filter = {};
    filter[Constants.Frontend._ID] = {"$in":resources};

    var query = {};
    query[Constants.Frontend.Query.TABLE] = Constants.Frontend.RESOURCES;
    query[Constants.Frontend.Query.FILTER] = filter;

    NodejsAPI.asQuery(
        query, {"ask":Constants.Frontend.Application.ASK}
    ).execute(ApplaneCallback(callback, function (result) {

        var data = result[Constants.Frontend.Query.DATA].length > 0 ? result[Constants.Frontend.Query.DATA] : null;
        if (data == null || data.length == 0) {
            throw new AppsStudioError(Constants.Frontend.Errors.RESOURCES_NOT_FOUND, [resources]);
        }
        var newData = [];
        var dataCount = data.length;
        for (var j = 0; j < resources.length; j++) {
            var obj = resources[j];
            for (var i = 0; i < dataCount; i++) {
                var o = data[i];
                if (obj.toString() == o._id.toString()) {
                    newData.push(o);
                    break;
                }
            }

        }
        callback(null, newData);
    }));
}

function getDomain(domain, callback) {
    var filter = {};
    filter[Constants.Frontend.Domains.DOMAIN] = "{domain}";
    var domainQuery = {};
    domainQuery[Constants.Frontend.Query.TABLE] = Constants.Frontend.DOMAINS;
    domainQuery[Constants.Frontend.Query.FILTER] = filter;
    domainQuery[Constants.Frontend.Query.PARAMETERS] = {"domain":domain};

    NodejsAPI.asQuery(
        domainQuery, {"ask":Constants.Frontend.Application.ASK}
    ).execute(ApplaneCallback(callback, function (result) {

        var data = result[Constants.Frontend.Query.DATA].length > 0 ? result[Constants.Frontend.Query.DATA] : null;

        if (data == null || data.length == 0) {
            domainQuery[Constants.Frontend.Query.TABLE] = Constants.Frontend.ALTERNATEDOMAINS;
            NodejsAPI.asQuery(
                domainQuery, {"ask":Constants.Frontend.Application.ASK}
            ).execute(ApplaneCallback(callback, function (result) {

                var data = result[Constants.Frontend.Query.DATA].length > 0 ? result[Constants.Frontend.Query.DATA] : null;
                if (data == null || data.length == 0) {
                    throw new AppsStudioError(Constants.Frontend.Errors.DOMAIN_NOT_EXIST);
                } else {
                    var domainId = data[0].domainid._id;

                    var filter = {"_id":domainId};

                    var domainQuery = {};
                    domainQuery[Constants.Frontend.Query.TABLE] = Constants.Frontend.DOMAINS;
                    domainQuery[Constants.Frontend.Query.FILTER] = filter;
                    NodejsAPI.asQuery(
                        domainQuery, {"ask":Constants.Frontend.Application.ASK}
                    ).execute(ApplaneCallback(callback, function (result) {

                        callback(null, result.data[0]);
                    }));


                }
            }));
        } else {
            callback(null, data[0]);
        }
    }));
}


function getTemplate(templateId, pageInfo, domainInfo, templates, resources, callback) {
    var osk;
    if (domainInfo.organizationid) {
        osk = domainInfo.organizationid.osk;
    }
    var filter = {};
    filter[Constants.Frontend.Templates.ID] = "{templateid}";
    filter[Constants.Frontend.Pages.DOMAINID + '.' + Constants.Frontend._ID] = "{domainid}";
    var query = {};
    query[Constants.Frontend.Query.TABLE] = Constants.Frontend.TEMPLATES;
    query[Constants.Frontend.Query.FILTER] = filter;
    query[Constants.Frontend.Query.PARAMETERS] = {"templateid":templateId, "domainid":domainInfo[Constants.Frontend._ID]};


    NodejsAPI.asQuery(
        query, {"ask":Constants.Frontend.Application.ASK}
    ).execute(ApplaneCallback(callback, function (result) {

        var data = result[Constants.Frontend.Query.DATA].length > 0 ? result[Constants.Frontend.Query.DATA] : null;

        if (data == null || data.length == 0) {
            throw new AppsStudioError(Constants.Frontend.Errors.TEMPLATE_NOT_FIND, [templateId]);
        }

        var templateInfo = data[0];

        var dataCallBack = ApplaneCallback(callback, function (viewData) {
            var resoucesTemp = templateInfo[Constants.Frontend.Resources.RESOURCES];
            if (resoucesTemp != null && resoucesTemp != undefined) {
                var resorcesCount = resoucesTemp.length;
                if (resorcesCount > 0) {
                    for (var i = 0; i < resorcesCount; i++) {
                        var resouceData = resoucesTemp[i];
                        var resourceId = resouceData[Constants.Frontend._ID];
                        resources.push(resourceId);
                    }
                }
            }
            var params = {"domain":domainInfo, "page":pageInfo, "getTemplate":function (templateid) {
                templates.push(templateid);
                return "####" + templateid + "####";
            }};
            if (viewData) {
                params.data = viewData;
            }

            var template = templateInfo[Constants.Frontend.Templates.TEMPLATE];
            var templateHtml = ejs.render(template, params);

            callback(null, templateHtml);
        });

        var templateApplicationId = templateInfo.applicationid;
        var templateViewId = templateInfo.viewid;
        var dataQueryparameters;
        if (pageInfo && pageInfo.parameters) {
            dataQueryparameters = pageInfo.parameters
        }
        if (!dataQueryparameters) {
            dataQueryparameters = {};
        }
        dataQueryparameters._CurrentPageId = pageInfo._id;

        if (templateViewId && templateApplicationId) {
            NodejsAPI.asQuery({view:templateViewId.id, table:templateViewId.table.id, parameters:dataQueryparameters}, {"ask":templateApplicationId.ask, osk:osk}).execute(dataCallBack);
        } else {
            dataCallBack();
        }


    }));
}


function getPageInfo(uri, domainInfo, callback) {
    var filter = {};

    filter[Constants.Frontend.Pages.URI] = "{uri}";
    filter[Constants.Frontend.Pages.DOMAINID + '.' + Constants.Frontend._ID] = "{domainid}";
    var query = {};
    query[Constants.Frontend.Query.TABLE] = Constants.Frontend.PAGES;
    query[Constants.Frontend.Query.FILTER] = filter;
    query[Constants.Frontend.Query.PARAMETERS] = {"uri":uri, "domainid":domainInfo[Constants.Frontend._ID]};

    NodejsAPI.asQuery(
        query, {"ask":Constants.Frontend.Application.ASK}
    ).execute(ApplaneCallback(callback, function (result) {

        var data = result[Constants.Frontend.Query.DATA].length > 0 ? result[Constants.Frontend.Query.DATA] : null;

        if (data == null || data.length == 0) {
            var pageNotFoundUri = domainInfo[Constants.Frontend.Pages.PAGE_NOT_FOUND_URI];
            if (!pageNotFoundUri || uri == pageNotFoundUri) {
                throw new AppsStudioError(Constants.Frontend.Errors.PAGE_NOT_FOUND, [uri]);
            } else {
                getPageInfo(pageNotFoundUri, domainInfo, callback);
            }

        } else {
            callback(null, data[0]);
        }

    }));
}


console.log('Server running at port : 4500');
