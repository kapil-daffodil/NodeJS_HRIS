function beforeInsertUpdate() {
    if (this._operation == 'insert' || this._operation == 'update') {
        var monthMap = new Object();
        monthMap[1] = "January";
        monthMap[2] = "February";
        monthMap[3] = "March";
        monthMap[4] = "April";
        monthMap[5] = "May";
        monthMap[6] = "June";
        monthMap[7] = "July";
        monthMap[8] = "August";
        monthMap[9] = "September";
        monthMap[10] = "October";
        monthMap[11] = "November";
        monthMap[12] = "December";

        if (this.applanePunchNumber) {
            var employees = this.employees;
            for (var i in employees) {
                this.employee_id = employees[i]._id;
            }
        }
        /*punch in type ids
         * In = 525f87fe3c7e777f660008e7
         * Out = 525f87fe3c7e777f660008e6
         * */
        var punchingData = this.employee_punching_data;
        var attendanceDateObject = this.attendance_date;

        var attendanceDate = new Date(attendanceDateObject);
        var attendanceMonth = attendanceDate.getMonth() + 1;
        var attendanceYear = attendanceDate.getFullYear();
        this.month_id = {"name": monthMap[attendanceMonth]};
        this.year_id = {"name": "" + attendanceYear + ""};


        var map = {};
        var myPunchTimes = [];
        var counter = 0;
        for (var punchingDataCounter in punchingData) {
            var punchTypeId = punchingData[punchingDataCounter].punch_type_id._id;
            var punchDate = punchingData[punchingDataCounter].date;
            var punchTime = punchingData[punchingDataCounter].time;
            var date = new Date(punchDate);

            var addInHours = 0;
            var timeArray = punchTime.split(" ");
            var actualTime = timeArray[0];
            var maditarion = timeArray[1];

            var actualTimeArray = actualTime.split(":");
            var hours = actualTimeArray[0];
            if (maditarion === 'PM' && Number(hours) < 12) {
                addInHours = 12;
            }
            if (Number(hours) > 12 && maditarion === 'PM') {
                punchTime = (Number(hours) - 12) + ":" + actualTimeArray[1] + " PM";
            }
            hours = Number(hours) + Number(addInHours);
            var minutes = actualTimeArray[1];
            var year = date.getFullYear();
            var month = date.getMonth();
            var day = date.getDate();
            var seconds = "00";
            var milliseconds = "00";
            if (actualTimeArray.length > 2) {
                seconds = actualTimeArray[2];
            }
            var dateTime = new Date(year, month, day, hours, minutes, seconds, milliseconds);
            var totalMilliSeconds = dateTime.getTime();
            var myJSONObject = {"punchTypeId": punchTypeId, "punchTime": punchTime, "punchDate": punchDate, "dateTime": dateTime};

            map[totalMilliSeconds] = myJSONObject;
            myPunchTimes[counter] = totalMilliSeconds;
            counter = counter + 1;
        }
        myPunchTimes.sort();
        if (myPunchTimes.length == 1) {
            var milliSeconds = myPunchTimes[0];
            var jsonDetails = map[milliSeconds];
            var punchType = jsonDetails.punchTypeId;
            var punchTime = jsonDetails.punchTime;
            if (punchType === '525f87fe3c7e777f660008e7') {
                this.first_in_time = punchTime;
            }
            if (punchType === '525f87fe3c7e777f660008e6') {
                this.last_out_time = punchTime;
            }
            this.attendance_type_id = {"name": "Present"};
        }
        if (myPunchTimes.length > 1) {
            var foundIn = 0;
            var isInConsistent = 0;
            var totalInHours = 0;
            var totalOutHours = 0;
            var i = 0;
            counter = 0;
            for (i = 1; i < myPunchTimes.length; i++) {
                var milliSecondsPrevious = myPunchTimes[i - 1];
                var milliSecondsPresent = myPunchTimes[i];

                var jsonDetailsPrevious = map[milliSecondsPrevious];
                var jsonDetailsPresent = map[milliSecondsPresent];

                var punchTypePrevious = jsonDetailsPrevious.punchTypeId;
                var punchTimePrevious = jsonDetailsPrevious.punchTime;

                var punchTypePresent = jsonDetailsPresent.punchTypeId;
                var punchTimePresent = jsonDetailsPresent.punchTime;
                if (punchTypePrevious === '525f87fe3c7e777f660008e7' && punchTypePresent === '525f87fe3c7e777f660008e6') {
                    var hours = ((milliSecondsPresent - milliSecondsPrevious) / ( 60 * 1000 * 60));
                    totalInHours = totalInHours + hours;
                }
                if (punchTypePrevious === '525f87fe3c7e777f660008e6' && punchTypePresent === '525f87fe3c7e777f660008e7') {
                    var hours = ((milliSecondsPresent - milliSecondsPrevious) / ( 60 * 1000 * 60));
                    totalOutHours = totalOutHours + hours;
                }
                if (punchTypePrevious === '525f87fe3c7e777f660008e7' && punchTypePresent === '525f87fe3c7e777f660008e7') {
                    isInConsistent = 1;
                }
                if (punchTypePrevious === '525f87fe3c7e777f660008e6' && punchTypePresent === '525f87fe3c7e777f660008e6') {
                    isInConsistent = 1;
                }
                if (counter == 0 && punchTypePrevious === '525f87fe3c7e777f660008e7') {
                    counter = 1;
                    this.first_in_time = punchTimePrevious;
                }
                if (counter == 0 && punchTypePresent === '525f87fe3c7e777f660008e7') {
                    counter = 1;
                    this.first_in_time = punchTimePresent;
                }
                if (punchTypePrevious === '525f87fe3c7e777f660008e6') {
                    this.last_out_time = punchTimePresent;
                }
                if (punchTypePresent === '525f87fe3c7e777f660008e6') {
                    this.last_out_time = punchTimePresent;
                }
            }
            if (this.was_manual_update == undefined) {
                this.attendance_type_id = {"name": "Present"};
            }
            if (this.was_manual_update == false) {
                this.attendance_type_id = {"name": "Present"};
            }

            if (Number(totalInHours) > 0) {
                var finalInHours = (Number(totalInHours) - 0.50).toFixed(0);
                var totalInMinutes = ((Number(totalInHours).toFixed(2) - finalInHours) * 100).toFixed(0);
                totalInMinutes = ((Number(totalInMinutes) * 60) / 100).toFixed(0);
                if (Number(finalInHours) == -0) {
                    finalInHours = 0;
                }
                this.total_time_in_office = finalInHours + " Hr. " + totalInMinutes + " Min.";
            } else {
                this.total_time_in_office = "0.0 Hr. 0.0 Min.";
            }
            if (Number(totalOutHours) > 0) {
                var finalOutHours = (Number(totalOutHours) - 0.50).toFixed(0);
                var totalOutMinutes = ((Number(totalOutHours).toFixed(2) - finalOutHours) * 100).toFixed(0);
                totalOutMinutes = ((Number(totalOutMinutes) * 60) / 100).toFixed(0);
                if (Number(finalOutHours) == -0) {
                    finalOutHours = 0;
                }
                this.total_break_time = finalOutHours + " Hr. " + totalOutMinutes + " Min.";
            } else {
                this.total_break_time = "0.0 Hr. 0.0 Min.";
            }
            if (isInConsistent == 1) {
                this.punch_status_id = {"name": "In-Consistence"};
            } else {
                this.punch_status_id = {"name": "Consistence"};
            }
        }
    }
}

function afterInsertUpdate() {
    if (this._operation == 'insert' || this._operation == 'update') {
        var PRESENT = "526f63b1a2d3e27a5b00000b";
        var ABSENT = "5271025ef4cbee99700006df";

        var TOUR = "527208e230a74a251500082a";
        var HOLIDAY = "527208e230a74a2515000829";
        var OFF = "527208e230a74a2515000828";
        var EWD_FULL = "527208e230a74a2515000827";
        var EWD_HALF = "527208e230a74a2515000826";
        var UNKNOWN = "527208e230a74a2515000825";
        var WORK_FROM_HOME = "527208e230a74a2515000824";
        var WORK_FROM_HOME_HALF_DAY = "527208e230a74a2515000823";
        var HALF_DAY_OFF = "527208e230a74a2515000822";
        var HALF_DAY_ABSENT = "527208e230a74a2515000821";
        var FULL_DAY_LEAVE = "5272089930a74a251500081b";
        var FIRST_HALF_DAY_LEAVE = "5272089930a74a251500081a";
        var SECOND_HALF_DAY_LEAVE = "5272089930a74a2515000819";


        var employee_monthly_attendance = this.employee_monthly_attendance;
        var employee_daily_attendance = this.employee_daily_attendance;

        var monthlyAttendanceId = null;
        var monthlyDetailsId = null;
        for (var i in employee_monthly_attendance) {
            monthlyAttendanceId = employee_monthly_attendance[i]._id;
            var details = employee_monthly_attendance[i].details;
            for (var j in details) {
                var isFreezed = details[j].is_freezed;
                writeLog("isFreezed >> " + isFreezed);
                if (isFreezed == undefined) {
                    monthlyDetailsId = details[j]._id;
                }
                if (isFreezed == false) {
                    monthlyDetailsId = details[j]._id;
                }
            }
        }
        var totalPresent = 0;
        var totalAbsent = 0;
        var totalHoliday = 0;
        var totalOff = 0;
        var totalEWDs = 0;
        var totalUnknown = 0;
        var totalWorkFromHome = 0;
        var totalHalfDayOff = 0;
        var totalHalfDayAbsent = 0;
        var totalLeaves = 0;
        for (var i in employee_daily_attendance) {
            var attendanceType = "" + employee_daily_attendance[i].attendance_type_id._id + "";
            var isPaid = Boolean(employee_daily_attendance[i].is_attendance_paid);
            if (attendanceType === PRESENT) {
                totalPresent = totalPresent + 1;
            }
            if (attendanceType === ABSENT) {
                totalAbsent = totalAbsent + 1;
            }
            if (attendanceType === TOUR) {
                totalPresent = totalPresent + 1;
            }
            if (attendanceType === HOLIDAY) {
                totalHoliday = totalHoliday + 1;
            }
            if (attendanceType === OFF) {
                totalOff = totalOff + 1;
            }
            if (attendanceType === EWD_FULL && isPaid == true) {
                totalEWDs = totalEWDs + 1;
            }
            if (attendanceType === EWD_HALF && isPaid == true) {
                totalEWDs = totalEWDs + 0.50;
            }
            if (attendanceType === UNKNOWN) {
                totalUnknown = totalUnknown + 1;
            }
            if (attendanceType === WORK_FROM_HOME) {
                totalPresent = totalPresent + 1;
            }
            if (attendanceType === WORK_FROM_HOME_HALF_DAY) {
                totalLeaves = totalLeaves + 0.50;
                totalPresent = totalPresent + 0.50;
            }
            if (attendanceType === HALF_DAY_OFF) {
                totalHalfDayOff = totalHalfDayOff + 1;
                totalPresent = totalPresent + 0.50;
            }
            if (attendanceType === HALF_DAY_ABSENT) {
                totalHalfDayAbsent = totalHalfDayAbsent + 1;
                totalPresent = totalPresent + 0.50;
            }
            if (attendanceType === FULL_DAY_LEAVE) {
                totalLeaves = totalLeaves + 1;
            }
            if (attendanceType === FIRST_HALF_DAY_LEAVE) {
                totalLeaves = totalLeaves + 0.50;
                totalPresent = totalPresent + 0.50;
            }
            if (attendanceType === SECOND_HALF_DAY_LEAVE) {
                totalLeaves = totalLeaves + 0.50;
                totalPresent = totalPresent + 0.50;
            }
        }
        var employeeId = this.employee_id._id;
        delete this.employee_id;
        writeLog("monthlyDetailsId >> " + monthlyDetailsId);
        var compiledDate = new Date();
        if (monthlyAttendanceId && monthlyDetailsId) {
            return {"monthly_attendance__attendance": {"id": {"_id": monthlyAttendanceId}, "value": {"details": [
                {"present_days": totalPresent, "absent_days": totalAbsent, "_id": monthlyDetailsId, "holidays": totalHoliday, "extra_working_days": totalEWDs, "leave_days": totalLeaves, "details.compensatory_off": totalOff}
            ], "compiled_date": compiledDate}}};
        }
        if (monthlyAttendanceId) {
            return {"monthly_attendance__attendance": {"id": {"_id": monthlyAttendanceId}, "value": {"details": [
                {"present_days": totalPresent, "absent_days": totalAbsent, "holidays": totalHoliday, "extra_working_days": totalEWDs, "leave_days": totalLeaves}
            ], "compiled_date": compiledDate}}};
        } else {
            return {"monthly_attendance__attendance": {"value": {"details.present_days": totalPresent, "details.absent_days": totalAbsent, "details.holidays": totalHoliday, "details.extra_working_days": totalEWDs, "details.leave_days": totalLeaves, "month_id": this.month_id._id, "year_id": this.year_id._id, "employee_id": employeeId, "compiled_date": compiledDate}}};
        }

    }
}