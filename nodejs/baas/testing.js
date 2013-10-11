var opertion = {"_id":"0000", "efforts":{"time":"123"}, "commnets":[
    {"comment":"yeeeee", "by":"rr"}
]};
var oldValue =
{"task":"tt", "efforts":{"time":"1212", "timeunit":"Hr"}, "commnets":[
    {"_id":"12121", "comment":"ass", "by":"pp"}
]};
merge(opertion, oldValue);
console.log(opertion);
function merge(operation, oldValues) {
    for (var key in oldValues) {
        var value = oldValues[key];
        if (!operation[key] || typeof value == "string") {
            operation[key] = value;
        } else {
            if (value instanceof Object) {
                merge(operation[key] || {}, value);
            } else if (value instanceof Array) {
                for (var obj in value) {
                    if (obj instanceof Object) {
                        var objKey = obj._id;
                        if (operation[key]) {
                            var operationArray = operation[key];
                            for (var obj1 in operationArray) {
                                var obj1Key = obj1._id;
                                if (objKey.toString() == obj1Key.toString()) {
                                    merge(obj1, obj);
                                }
                            }
                        } else {
                            operation[key] = value;
                            break;
                        }
                    }
                }
            }
        }
    }
}