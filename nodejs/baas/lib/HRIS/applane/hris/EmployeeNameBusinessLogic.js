function beforeInsertUpdate() {
    if (this._operation == 'insert' || this._operation == 'update') {
        writeLog("firstName >> " + firstName + " << middleName >> " + middleName + " << lastName >> " + lastName);
        var firstName = this.first_name;
        var middleName = this.middle_name;
        var lastName = this.last_name;
        var name = "";
        if (this.first_name) {
            name = name + this.first_name;
        }
        if (this.middle_name) {
            if (name != '') {
                name = name + " ";
            }
            name = name + this.middle_name;
        }
        if (this.last_name) {
            if (name != '') {
                name = name + " ";
            }
            name = name + this.last_name;
        }
        return name;
        var officialInformation = JSON.stringify(this.official_information);
        var officialEmailid = null;
        if (officialInformation != null) {
            officialEmailid = this.official_information.official_emailid;
        }
        writeLog("this >> officialEmailid >> " + officialEmailid + " << this._organizationid_ >> " + this._organizationid_);
    }
}
function afterInsertUpdate() {
    if (this._operation == 'insert') {
        var name = this.name;
        var officialInformation = JSON.stringify(this.official_information);
        var officialEmailid = null;
        if (officialInformation != null) {
            officialEmailid = this.official_information.official_emailid;
        }
        var firstName = "";
        var lastName = "";
        if (this.first_name != null) {
            firstName = this.first_name;
        }
        if (this.last_name != null) {
            lastName = this.last_name;
        }
        writeLog("after >> id >> " + this._id);
        return {"entities__busuiness_partners": {"value": {"name": name, "category_id": "525fc8816f4aaa5150000065", "entity_type_id": "525fd91c10963bfb57000017"}}, "users__baas": {"value": {"username": name,
            "fullname": name, "password": "daffodil", "emailid": officialEmailid, "firstname": firstName, "lastname": lastName, "usergroupid": {"id": "baas"}, "applications": [
                {"id": "hris_basic", "organizations": [
                    {"_id": this._organizationid_}
                ]}
            ]}}};
    }
}