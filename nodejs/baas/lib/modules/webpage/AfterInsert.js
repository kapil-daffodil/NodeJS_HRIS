// set redirect and deleted null in insert and update.
// set respose code 404 if deleted true in page job.
// response code work
// sitemap filter(deleted,unpbliished,redirect,response:200)
// page redirect job
function afterInsertUpdate() {
   if(this._operation=='insert' || this._operation=='update') {
    var uriTitle = this._context.params["urititle"];
    var uri = this._context.params["uri"];
    var metaDescription = this._context.params["metadescription"];
    var keywords = this._context.params["keywords"];
    var updateFrequency = this._context.params["updatefrequency"];
    var priority = this._context.params["priority"];
    var templateId = this._context.params["templateid"];

    var domainId = this._context.params["domainid"];
    writeLog("uri********"+uri);


    var update = {};
    update.urititle = getValue(this, uriTitle);
    update.uri = getValue(this, uri);
    if (!update.uri) {
        throw new JobError({code:2, message:"URI is mandatory"});
    }
    update.uri = update.uri.trim().toLowerCase();
    update.metadescription = getValue(this, metaDescription);
    update.keywords = getValue(this, keywords);
    update.updatefrequency = getValue(this, updateFrequency);
    update.priority = getValue(this, priority);
    update.templateid = getValue(this, templateId);
    writeLog("updagte tempalteid********"+update.templateid );
    update.domainid = getValue(this, domainId);
    update.deleted = null;
    update.redirect_to = null;
    if (!(update.templateid instanceof Object)) {
        update.templateid = {id:update.templateid, domainid:update.domainid};
    }
    return {pages__frontend:[{id:{uri:update.uri, domainid:update.domainid}, value:update}]};
    }

    function getValue(object, column) {
        var columnValue;
        if (column && column.indexOf("'") == 0) {
            columnValue = column.substring(1, column.length - 1);
        } else if (column) {
            columnValue = object[column];
        }
        return columnValue;
    }
}

function afterDelete() {

if(this._operation=='delete'){
    var uri = this._context.params["uri"];
    var domainId = this._context.params["domainid"];
    var id = {};
    id.uri = getValue(this, uri);
    id.domainid = getValue(this, domainId);
    return {pages__frontend:[{id:id, value:{deleted:true}}]};
  }

    function getValue(object, column) {
        var columnValue;
        if (column && column.indexOf("'") == 0) {
            columnValue = column.substring(1, column.length - 1);
        } else if (column) {
            columnValue = object[column];
        }
        return columnValue;
    }
    }

}
