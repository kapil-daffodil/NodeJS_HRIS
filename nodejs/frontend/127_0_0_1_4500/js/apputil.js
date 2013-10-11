appStrapDirectives.factory('$appUtil', ['$timeout',
    function ($timeout) {
        var $appUtil = {

        };
        $appUtil.rebindFieldExpression = function ($scope, model, field) {
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
        $appUtil.resolveDot = function (model, expression, confirm, confirmType) {
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


                if (!model[exp] && !confirm) {
                    return;
                }
                if (model[exp]) {
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

        $appUtil.getModel = function ($scope, modelExpression, confirm, confirmType) {
            return this.resolveDot($scope, modelExpression, confirm, confirmType);
        }

        return $appUtil;

    }
])