exports.Frontend = new function () {
    this._ID = '_id';

    this.Application = new function () {
        this.ASK = 'frontend';
        this.ID = 'frontend';
        this.LABEL = 'Frontend';
    };

    this.RESOURCES = 'resources__frontend';
    this.Resources = new function () {
        this.ID = 'id';
        this.RESOURCES = 'resources';
        this.CONTENTS = 'contents';
        this.FILE = "file";
        this.TYPE = 'type';
        this.Type = new function () {
            this.CSS = 'css';
            this.JS = 'js';
        };

    };

    this.Query = new function () {
        this.TABLE = 'table';
        this.COLUMNS = 'columns';
        this.FILTER = 'filter';
        this.PARAMETERS = "parameters";
        this.DATA = 'data';
    };


    this.DOMAINS = 'domains__frontend';
    this.Domains = new function () {
        this.DOMAIN = 'domain';
    };

    this.ALTERNATEDOMAINS = 'alternatedomains__frontend';
    this.AlternateDomains = new function () {

    };

    this.PAGES = 'pages__frontend';
    this.Pages = new function () {
        this.URI = 'uri';
        this.TEMPLATEID = 'templateid';
        this.DOMAINID = 'domainid';
        this.PAGE_NOT_FOUND_URI = 'pagenotfounduri';
        this.LABEL = "label";
        this.METADESCRIPTION = "metadescription";
        this.KEYWORDS = "keywords";
        this.URITITLE = "urititle";
        this.DELETED = "deleted";
        this.UNPUBLISH = "unpublish";
        this.LASTUPDATE = "last_update";
        this.UPDATEFREQUENCY = "update_frequency";
        this.PRIORITY = "priority";
        this.REDIRECT_TO = "redirect_to";
        this.RESPONSECODE = "responsecode";
    };

    this.TEMPLATES = 'templates__frontend';
    this.Templates = new function () {
        this.DOMAINID = 'domainid';
        this.ID = 'id';
        this.TEMPLATE = 'template';

    };

    this.Errors = new function () {
        this.RESOURCES_NOT_FOUND = {"message":"'Resources not found ['+params[0]+']'"};
        this.PAGE_NOT_FOUND = {"message":"'404 error, page not found and Template id not find for ['+params[0]+']'"};
        this.DOMAIN_NOT_EXIST = {"message":"'['+params[0]+'] domain does not exist.'"};
        this.TEMPLATEID_NOT_FIND = {"message":"'Template Id not find for ['+params[0]+']'"};
        this.TEMPLATE_NOT_FIND = {"message":"'Template not find for ['+params[0]+']'"};
    };

};


exports.ResponseCode = new function () {
    var codes = {};
    codes[100] = 'Continue';
    codes[101] = 'Switching Protocols';
    codes[200] = 'OK';
    codes[201] = 'Created';
    codes[201] = 'Accepted';
    codes[203] = 'Non-Authoritative Information';
    codes[204] = 'No Content';
    codes[205] = 'Reset Content';
    codes[206] = 'Partial Content';
    codes[207] = 'Multi-Status';
    codes[208] = 'Already Reported';
    codes[226] = 'IM Used';
    codes[300] = 'Multiple Choices';
    codes[301] = 'Moved Permanently';
    codes[302] = 'Found';
    codes[303] = 'See Other';
    codes[304] = 'Not Modified';
    codes[305] = 'Use Proxy';
    codes[306] = 'Switch Proxy';
    codes[307] = 'Temporary Redirect ';
    codes[308] = 'Permanent Redirect';
    codes[400] = 'Bad Request';
    codes[401] = 'Unauthorized';
    codes[402] = 'Payment Required';
    codes[403] = 'Forbidden';
    codes[404] = 'Not Found';
    codes[405] = 'Method Not Allowed';
    codes[406] = 'Not Acceptable';
    codes[407] = 'Proxy Authentication Required';
    codes[408] = 'Request Timeout';
    codes[409] = 'Conflict';
    codes[410] = 'Gone';
    codes[400] = 'Request-URI Too Long';
    codes[400] = 'Unsupported Media Type';
    codes[494] = 'Request Header Too Large';
    codes[444] = 'No Response';
    codes[497] = 'HTTP to HTTPS';
    codes[500] = 'Internal Server Error';
    codes[502] = 'Bad Gateway';
    codes[501] = 'Not Implemented';
    codes[503] = 'Service Unavailable';
    codes[505] = 'HTTP Version Not Supported';
    this.CODES = codes;
}



















