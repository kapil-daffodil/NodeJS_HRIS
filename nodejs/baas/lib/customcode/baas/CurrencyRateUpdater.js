/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 17/9/13
 * Time: 6:38 PM
 * To change this template use File | Settings | File Templates.
 */

var Constants = require('../../shared/Constants.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var DataBaseEngine = require('../../database/DatabaseEngine.js');
var UpdateEngine = require('../../database/UpdateEngine.js');
var APIConstants = require("nodejsapi/Constants.js");
var Utils = require("apputil/util.js");
var http = require('http');
var QueryString = require("querystring");
var xml2js = require("xml2js");


exports.updateCurrencyRates = function (parameters, callback) {
    getCurrencies(ApplaneCallback(callback, function (currenciesData) {
        var currenciesLength = currenciesData ? currenciesData.length : 0;
        if (currenciesLength > 1) {
            var updates = [];
            Utils.iterateArrayWithIndex(currenciesData, ApplaneCallback(callback, function () {
                if (updates.length > 0) {
                    var newUpdates = {};
                    newUpdates[APIConstants.Query.TABLE] = Constants.Baas.CURRENCY_CONVERSION_RATES;
                    newUpdates[APIConstants.Update.OPERATIONS] = updates;
                    UpdateEngine.performUpdate({
                        ask:Constants.Baas.ASK,
                        updates:newUpdates,
                        callback:callback
                    })
                } else {
                    callback();
                }
            }), function (fromCurrencyIndex, fromCurrencyObj, callback) {
                var fromCurrency = fromCurrencyObj[Constants.Baas.Currencies.CURRENCY];
                Utils.iterateArrayWithIndex(currenciesData, callback, function (toCurrencyIndex, toCurrencyObj, callback) {
                    var toCurrency = toCurrencyObj[Constants.Baas.Currencies.CURRENCY];
                    if (fromCurrencyIndex != toCurrencyIndex) {
                        getConversionRate(fromCurrency, toCurrency, ApplaneCallback(callback, function (conversionRate) {
                            if (conversionRate) {
                                var currencyRateRecord = {};
                                currencyRateRecord[Constants.Baas.CurrencyConversionRates.FROM] = fromCurrency;
                                currencyRateRecord[Constants.Baas.CurrencyConversionRates.TO] = toCurrency;
                                currencyRateRecord[Constants.Baas.CurrencyConversionRates.RATE] = conversionRate;
                                currencyRateRecord[APIConstants.Update.Operation.TYPE] = APIConstants.Update.Operation.Type.INSERT_IF_NOT_EXIST;
                                updates.push(currencyRateRecord);
                            }
                            callback();
                        }))
                    } else {
                        var currencyRateRecord = {};
                        currencyRateRecord[Constants.Baas.CurrencyConversionRates.FROM] = fromCurrency;
                        currencyRateRecord[Constants.Baas.CurrencyConversionRates.TO] = toCurrency;
                        currencyRateRecord[Constants.Baas.CurrencyConversionRates.RATE] = 1;
                        currencyRateRecord[APIConstants.Update.Operation.TYPE] = APIConstants.Update.Operation.Type.INSERT_IF_NOT_EXIST;
                        updates.push(currencyRateRecord);
                        callback();
                    }
                })
            })
        } else {
            callback();
        }
    }))
}

function getConversionRate(fromCurrency, toCurrency, callback) {
    var options = {};
    options.FromCurrency = fromCurrency;
    options.ToCurrency = toCurrency;

    var queryString = "";
    if (Object.keys(options).length > 0) {
        queryString = QueryString.stringify(options);
    }
    var serverOptions = {
        hostname:"www.webservicex.net",
        port:80,
        path:"/CurrencyConvertor.asmx/ConversionRate",
        method:"POST",
        headers:{
            'Content-Type':'application/x-www-form-urlencoded',
            'Content-Length':queryString.length
        }
    };
    var req = http.request(serverOptions, function (res) {
        if (options.response) {
            res.setEncoding('binary');
        } else {
            res.setEncoding('utf8');
        }

        var body = '';
        res.on('data', function (chunk) {
            body += chunk;
        });

        res.on('end', function () {
            try {
                var parser = new xml2js.Parser();
                parser.parseString(body, function (err, res) {
                    if (err) {
                        callback(err);
                    } else {
                        var conversionRate = parseFloat(res.double._);
                        callback(null, conversionRate);
                    }

                });
            } catch (err) {
                callback(err);
            }

        });
    });

    req.on('error', function (err) {
        callback(err);
    });
    req.write(queryString);
    req.end();
}


function getCurrencies(callback) {
    var query = {};
    query[APIConstants.Query.TABLE] = Constants.Baas.CURRENCIES;
    DataBaseEngine.query({
        ask:Constants.Baas.ASK,
        query:query,
        callback:ApplaneCallback(callback, function (result) {
            callback(null, result[APIConstants.Query.Result.DATA]);
        })
    })
}

//this.updateCurrencyRates(function (err, res) {
//    if (err) {
//        console.log(err.stack);
//    } else {
//        console.log("success...");
//    }
//})