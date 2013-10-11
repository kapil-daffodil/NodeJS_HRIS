/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 7/19/13
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */

var map = {};
var util = require("util");


exports.get = function (key) {
    var cacheMap = map;
    var cacheKey = key;
    if (util.isArray(key) && key.length > 0) {
        var length = key.length - 1;
        for (var i = 0; i < length; i++) {
            cacheKey = key[i];
            cacheMap = cacheMap[cacheKey];
            if (!cacheMap) {
                return null;
            }
        }
        cacheKey = key[length];
    }
    return cacheMap[cacheKey];
};

exports.put = function (key, value) {
    var cacheMap = map;
    var cacheKey = key;
    if (util.isArray(key) && key.length > 0) {
        var length = key.length - 1;
        for (var i = 0; i < length; i++) {
            cacheKey = key[i];
            if (!cacheMap[cacheKey]) {
                cacheMap[cacheKey] = {};
            }
            cacheMap = cacheMap[cacheKey];
        }
        cacheKey = key[length];
    }
    cacheMap[cacheKey] = value;
};

exports.clear = function (key) {
    var cacheMap = map;
    var cacheKey = key;
    if (util.isArray(key) && key.length > 0) {
        var length = key.length - 1;
        for (var i = 0; i < length; i++) {
            cacheKey = key[i];
            cacheMap = cacheMap[cacheKey];
            if (!cacheMap) {
                return;
            }
        }
        cacheKey = key[length];
    }
    delete cacheMap[cacheKey];
};
