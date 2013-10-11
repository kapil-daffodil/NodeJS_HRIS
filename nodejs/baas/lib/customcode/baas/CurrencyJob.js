/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 18/9/13
 * Time: 1:16 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../../shared/Constants.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var APIConstants = require('nodejsapi/Constants.js');
var MetadataProvider = require('../../metadata/MetadataProvider.js');

exports.beforeQuery = function (params, callback) {
    var ask = params.ask;
    if (ask == Constants.Baas.ASK) {
        callback();
    } else {
        addAllowedCurrencyFilterInQuery(params, callback);
    }
}

function addFilter(allowedCurrencies, filter) {
    var allowedCurrenciesArray = [];
    for (var i = 0; i < allowedCurrencies.length; i++) {
        allowedCurrenciesArray.push(allowedCurrencies[i][APIConstants.Query._ID]);
    }
    filter[APIConstants.Query._ID] = {$in:allowedCurrenciesArray};
}

function addAllowedCurrencyFilterInQuery(params, callback) {
    var ask = params.ask;
    var osk = params.osk;
    var query = params.query;
    query[APIConstants.Query.FILTER] = query[APIConstants.Query.FILTER] || {};
    var filter = query[APIConstants.Query.FILTER];
    if (osk) {
        MetadataProvider.getOrganization(osk, ApplaneCallback(callback, function (organization) {
            if (organization && organization.length > 0 && organization[0][Constants.Baas.Organizations.ALLOWED_CURRENCIES]) {
                var allowedCurrencies = organization[0][Constants.Baas.Organizations.ALLOWED_CURRENCIES];
                addFilter(allowedCurrencies, filter)
                callback();
            } else {
                MetadataProvider.getApplication(ask, ApplaneCallback(callback, function (application) {
                    if (application && application.length > 0 && application[0][Constants.Baas.Organizations.ALLOWED_CURRENCIES]) {
                        var allowedCurrencies = application[0][Constants.Baas.Organizations.ALLOWED_CURRENCIES];
                        addFilter(allowedCurrencies, filter);
                    }
                    callback();
                }));
            }
        }))
    } else {
        MetadataProvider.getApplication(ask, ApplaneCallback(callback, function (application) {
            if (application && application.length > 0 && application[0][Constants.Baas.Organizations.ALLOWED_CURRENCIES]) {
                var allowedCurrencies = application[0][Constants.Baas.Organizations.ALLOWED_CURRENCIES];
                addFilter(allowedCurrencies, filter)
            }
            callback();
        }));
    }
}

