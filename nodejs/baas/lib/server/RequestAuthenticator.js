/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 7/25/13
 * Time: 6:35 PM
 * To change this template use File | Settings | File Templates.
 */

var MetadataProvider = require('../metadata/MetadataProvider.js');
var ApplaneCallback = require("apputil/ApplaneCallback.js");
var Constants = require('../shared/Constants.js');
var BaasError = require("apputil/ApplaneError.js");

exports.authenticate = function (ask, osk, callback) {
    if (!ask) {
        callback(new BaasError(Constants.ErrorCode.NO_APP_SECRET_KEY));
        return;
    }
    MetadataProvider.getApplication(ask, ApplaneCallback(callback, function (application) {
        if (application[Constants.Baas.Applications.ORGENABLED]) {
            if (!osk) {
                throw new BaasError(Constants.ErrorCode.NO_ORG_SECRET_KEY);
            }
            MetadataProvider.getOrganization(osk, ApplaneCallback(callback, function (organization) {
                if (organization[Constants.Baas.Organizations.DEACTIVATED]) {
                    throw new BaasError(Constants.ErrorCode.ORG_DEACTIVATED, [organization[Constants.Baas.Organizations.ID]]);
                }
                var applications = organization[Constants.Baas.Organizations.APPLICATIONS];
                var mapped = false;
                var length = applications ? applications.length : 0;
                for (var i = 0; i < length; i++) {
                    if (applications[i][Constants.Baas.Applications.ID]==(application[Constants.Baas.Applications.ID])) {
                        mapped = true;
                        break;
                    }
                }
                if (!mapped) {
                    throw new BaasError(Constants.ErrorCode.NO_APP_MAPPED_WITH_ORG, [application[Constants.Baas.Applications.ID], organization[Constants.Baas.Organizations.ID]]);
                }
                callback(null, {application:application, organization:organization});
            }));
        } else {
            callback(null, {application:application});
        }
    }));
}
