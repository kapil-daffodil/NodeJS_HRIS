function AppUtil() {
}


AppUtil.rebindFieldExpression = function ($scope, model, field) {
    var fieldFirstExpression;
    var fieldLastDotExpression;
    var dottIndex = field.lastIndexOf(".");
    if (dottIndex >= 0) {
        model = model + "." + field.substring(0, dottIndex);
        field = field.substring(dottIndex + 1);
    }
    $scope.modelexpression = model;
    $scope.fieldexpression = field;
}

AppUtil.removeDottedValue = function (model, expression) {
    if (!model) {
        return;
    }
    var firstDottedIndex = expression.indexOf(".");
    if (firstDottedIndex >= 0) {
        expression = expression.substring(0, firstDottedIndex);
    }
    delete model[expression];

}
AppUtil.putDottedValue = function (model, expression, value) {
    if (!model) {
        alert("Model does not exits for putting dotted value")
        return;
    }
    var lastDottedIndex = expression.lastIndexOf(".");
    if (lastDottedIndex >= 0) {
        var firstExpression = expression.substring(0, lastDottedIndex);
        expression = expression.substring(lastDottedIndex + 1);
        model = $appUtil.resolveDot(model, firstExpression, true);
    }
    model[expression] = value;

}
AppUtil.resolveDot = function (model, expression, confirm, confirmType) {
    if (!model) {
        return;
    }

    while (expression !== undefined) {
        var fieldIndex = expression.toString().indexOf(".");
        var exp;
        if (fieldIndex >= 0) {
            exp = expression.substring(0, fieldIndex);
            expression = expression.substring(fieldIndex + 1);

        } else {
            exp = expression;
            expression = undefined;

        }

        //

        var arrayFirstIndex = exp.toString().indexOf("[");
        var arrayEndIndex = exp.toString().indexOf("]");

        if (arrayFirstIndex >= 0 && arrayEndIndex >= 0) {
            var arrayIndex = Number(exp.substring(arrayFirstIndex + 1, arrayEndIndex));
            exp = exp.substring(0, arrayFirstIndex);
            if (expression) {
                expression = arrayIndex + "." + expression;
            } else {
                expression = arrayIndex;
            }
        }


        if (model[exp] === undefined && !confirm) {
            return;
        }
        if (model[exp] !== undefined) {
            model = model[exp];
        } else {
            if (expression) {
                model[exp] = {}
            } else {
                if (confirmType == 'array') {
                    model[exp] = [];
                } else {
                    model[exp] = {}
                }
            }
            model = model[exp];
        }

    }
    return model;
}

AppUtil.getModel = function ($scope, modelExpression, confirm, confirmType) {
    return this.resolveDot($scope, modelExpression, confirm, confirmType);
}
AppUtil.getDataFromService = function (url, requestBody, callType, dataType, pBusyMessage, callback, errcallback) {

    var usk;
    if (typeof(Storage) !== "undefined") {
        usk = localStorage.usk;
    }
    requestBody.usk = usk;
    requestBody.enablelogs = true;

    if (pBusyMessage) {
        $rootScope.busymessage = pBusyMessage;
        $rootScope.showbusymessage = true
        if (!$rootScope.$$phase) {
            $rootScope.$apply();
        }
    }


    $.ajax({
        type:callType,
        url:url,
        data:requestBody,
        crossDomain:true,
        success:function (returnData, status, xhr) {
            $rootScope.showbusymessage = false
            if (!$rootScope.$$phase) {
                $rootScope.$apply();
            }
            callback(returnData.response ? returnData.response : returnData);


        },
        error:function (jqXHR, exception) {

            $rootScope.showbusymessage = false
            if (!$rootScope.$$phase) {
                $rootScope.$apply();
            }
            var stack = jqXHR.responseText;
            var jsonError
            try {
                jsonError = JSON.parse(jqXHR.responseText)
                stack = jsonError.stack
            } catch (e) {
                jsonError = {code:-1, stack:stack};
            }

            if (errcallback) {
                errcallback(jsonError);
            } else {
                alert(stack);
            }

        },
        timeout:1200000,
        dataType:dataType,
        async:true
    });


}

