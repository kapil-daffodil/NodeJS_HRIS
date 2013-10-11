/**
 * Created with IntelliJ IDEA.
 * User: Manjeet1
 * Date: 9/28/13
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */

function beforeUpdateInsert() {
    if (this._operation == 'insert' || this._operation == 'update') {
        this.uri = this.uri.trim().toLowerCase();
    }
}

function afterUpdate() {
    if (this._operation == 'update') {
        var uri = this.uri;
        var oldUri = this._old.uri;
        if (uri != oldUri) {
            var update = {};
            update.uri = oldUri;
            update.redirect_to = uri;
            update.urititle = this.urititle;
            update.metadescription = this.metadescription;
            update.keywords = this.keywords;
            update.templateid = this.templateid;
            update.domainid = this.domainid;
            update.urititle = this.urititle;

            return {pages__frontend:[
                {id:{uri:oldUri, domainid:update.domainid}, value:update},
                {id:{redirect_to:oldUri, domainid:update.domainid}, value:{redirect_to:uri}, type:"update"}
            ]};
        }
    }
}

