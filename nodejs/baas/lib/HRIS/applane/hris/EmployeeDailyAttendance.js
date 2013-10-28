function beforeInsertUpdate() {
    if(this._operation=='insert' || this._operation=='update') {
        var employees = this.employees;
        if(this.applanePunchNumber){
            for(var i in employees){
                this.employee_id = employees[i]._id;
                writeLog("employeeId >> "+this.employee_id);
            }
        }
        /*punch in type ids
        * In = 525f87fe3c7e777f660008e7
        * Out = 525f87fe3c7e777f660008e6
        * */
        var punchingData = this.employee_punching_data;
        var map = new Object();
        for(var punchingDataCounter in punchingData) {
            var punchTypeId = punchingData[punchingDataCounter].punch_type_id._id;
            var punchDate = punchingData[punchingDataCounter].date;
            var punchTime = punchingData[punchingDataCounter].time;
            var punchDateTime = new Date();
            punchDateTime.setDate(punchDate);
            punchDateTime.setTime(punchTime);
            var myJSONObject = {"punchTypeId":punchTypeId,"punchTime":punchTime,"punchDate":punchDate};
            map[punchDateTime.getMilliseconds()] = myJSONObject;
            writeLog("punch time >> "+punchDateTime.getTime());
        }
        writeLog("map >> "+JSON.stringify(map));
    }
}