/**
 * Created with IntelliJ IDEA.
 * User: munish
 * Date: 22/8/13
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
var nodemailer = require("nodemailer");
var ejs = require('ejs');
var Constants = require('../shared/Constants.js');
var BaasError = require("apputil/ApplaneError.js");

exports.sendMail = function (mailContent, credential, callback) {
    if ('function' === typeof credential) callback = credential, credential = null;
    console.log("mailContent >>>>>>>>>>>" + JSON.stringify(mailContent));
    /*Resolve Template if Exist*/
    var mailTemplate = mailContent[Constants.MailService.Options.TEMPLATE];
    if (mailTemplate) {
        var templateData = mailContent[Constants.MailService.Options.TEMPLATE_DATA];
        /*Replace tags from html with actual value from JSON if templateData exists*/
        var html = templateData ? ejs.render(mailTemplate, templateData) : mailTemplate;
        delete mailContent[Constants.MailService.Options.TEMPLATE];
        delete mailContent[Constants.MailService.Options.TEMPLATE_DATA];
        mailContent[Constants.MailService.Options.HTML] = html;
    }
    /*Now send Mail*/
    var transport = nodemailer.createTransport("SMTP", getCredentials(credential, mailContent));
    if (!callback) {
        callback = function (err) {
            if (err) {
                console.log("Error while send mail" + JSON.stringify(err));
            } else {
                console.log("Mail Sent>>>>>>>>>");
            }
        };
    }
    transport.sendMail(mailContent, callback);
}

function getCredentials(credentail, mailContent) {
    var from = mailContent[Constants.MailService.Options.FROM];
    var fullCredentials;
    if (credentail) {
        var organization = credentail.organization;
        var application = credentail.application;
        if (organization && organization[Constants.Baas.Organizations.MAIL_CREDENTIALS] && organization[Constants.Baas.Organizations.MAIL_CREDENTIALS].length > 0) {
            var organizationMailCredentials = organization[Constants.Baas.Organizations.MAIL_CREDENTIALS];
            for (var i = 0; i < organizationMailCredentials.length; i++) {
                var organizationMailCredential = organizationMailCredentials[i];
                var name = organizationMailCredential[Constants.Baas.Organizations.MailCredentials.NAME];
                if (from && from.toLowerCase() == name.toLowerCase()) {
                    mailContent[Constants.MailService.Options.FROM] = from + "\<" + organizationMailCredential[Constants.Baas.Applications.MailCredentials.EMAILID] + "\>";
                    fullCredentials = populateCredentials(organizationMailCredential[Constants.Baas.Organizations.MailCredentials.EMAILID], organizationMailCredential[Constants.Baas.Organizations.MailCredentials.PASSWORD], organizationMailCredential[Constants.Baas.Organizations.MailCredentials.SERVICE]);
                    break;
                }
            }
            if (!fullCredentials) {
                var organizationMailCredential = getDefaultMailCredentitals(organizationMailCredentials);
                mailContent[Constants.MailService.Options.FROM] = organizationMailCredential[Constants.Baas.Applications.MailCredentials.NAME] + "\<" + organizationMailCredential[Constants.Baas.Applications.MailCredentials.EMAILID] + "\>";
                fullCredentials = populateCredentials(organizationMailCredential[Constants.Baas.Organizations.MailCredentials.EMAILID], organizationMailCredential[Constants.Baas.Organizations.MailCredentials.PASSWORD], organizationMailCredential[Constants.Baas.Organizations.MailCredentials.SERVICE]);
            }
        } else if (application && application[Constants.Baas.Applications.MAIL_CREDENTIALS] && application[Constants.Baas.Applications.MAIL_CREDENTIALS].length > 0) {
            var applicationMailCredentials = application[Constants.Baas.Applications.MAIL_CREDENTIALS];
            for (var i = 0; i < applicationMailCredentials.length; i++) {
                var applicationMailCredential = applicationMailCredentials[i];
                var name = applicationMailCredential[Constants.Baas.Applications.MailCredentials.NAME];
                if (from && from.toLowerCase() == name.toLowerCase()) {
                    mailContent[Constants.MailService.Options.FROM] = from + "\<" + applicationMailCredential[Constants.Baas.Applications.MailCredentials.EMAILID] + "\>";
                    fullCredentials = populateCredentials(applicationMailCredential[Constants.Baas.Applications.MailCredentials.EMAILID], applicationMailCredential[Constants.Baas.Applications.MailCredentials.PASSWORD], applicationMailCredential[Constants.Baas.Applications.MailCredentials.SERVICE]);
                    break;
                }
            }
            if (!fullCredentials) {
                var applicationMailCredential = getDefaultMailCredentitals(applicationMailCredentials);
                mailContent[Constants.MailService.Options.FROM] = applicationMailCredential[Constants.Baas.Applications.MailCredentials.NAME] + "\<" + applicationMailCredential[Constants.Baas.Applications.MailCredentials.EMAILID] + "\>";
                fullCredentials = populateCredentials(applicationMailCredential[Constants.Baas.Applications.MailCredentials.EMAILID], applicationMailCredential[Constants.Baas.Applications.MailCredentials.PASSWORD], applicationMailCredential[Constants.Baas.Applications.MailCredentials.SERVICE]);
            }
        } else {
            fullCredentials = getDefaultCredentials(mailContent);
        }
    } else {
        fullCredentials = getDefaultCredentials(mailContent);
    }
    return fullCredentials;
}

function getDefaultMailCredentitals(mailCredentials) {
    for (var i = 0; i < mailCredentials.length; i++) {
        var mailCredential = mailCredentials[i];
        var defaultValue = mailCredential[Constants.Baas.Organizations.MailCredentials.DEFAULT];
        if (defaultValue) {
            return mailCredential;
        }
    }
    return mailCredentials[0];
}

function getDefaultCredentials(mailContent) {
    mailContent[Constants.MailService.Options.FROM] = "Developer" + "\<" + "developer@daffodilsw.com" + "\>";
    return populateCredentials("developer@daffodilsw.com", "dak2applane", "Gmail");
}

function populateCredentials(userName, password, service) {
    if (!userName) {
        throw new BaasError(Constants.ErrorCode.FIELDS_BLANK, [Constants.Baas.Organizations.MailCredentials.EMAILID]);
    }
    if (!password) {
        throw new BaasError(Constants.ErrorCode.FIELDS_BLANK, [Constants.Baas.Organizations.MailCredentials.PASSWORD]);
    }
    if (!service) {
        service = "Gmail";
    }

    var auth = {};
    auth[Constants.MailService.Credential.USER_NAME] = userName;
    auth[Constants.MailService.Credential.PASSWORD] = password;
    var fullCredentail = {};
    fullCredentail[Constants.MailService.Credential.SERVICE] = service;
    fullCredentail.auth = auth;
    return fullCredentail;
}
