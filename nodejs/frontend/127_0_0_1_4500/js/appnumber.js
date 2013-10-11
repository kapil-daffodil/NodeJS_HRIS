appStrapDirectives.directive('appNumber', ["$compile", '$appUtil', function ($compile, $appUtil) {
    return {
        restrict:"E",
        replace:true,
        scope:true,
        template:"<div class='app-number-container'></div>",
        compile:function () {
            return  {
                pre:function ($scope, element) {
                },
                post:function ($scope, element, attrs) {

                    var modelExpression = attrs.model;
                    var fieldExpression = attrs.field;
                    var toBind = modelExpression + "." + fieldExpression;
                    $appUtil.rebindFieldExpression($scope, modelExpression, fieldExpression);


                    var html = "<input type='text' ng-model='" + toBind + "' ng-change='operatorValidation()' ng-click='operatorValidation()' />";
                    element.append($compile(html)($scope));


                    $scope.operatorValidation = function (event) {

                        var model = $appUtil.getModel($scope, $scope.modelexpression, true);


                        var val = model[$scope.fieldexpression];
                        if (!(val == "+" || val == "-")) {
                            if (!Number(val)) {
                                model[$scope.fieldexpression] = "";
                            }
                        }
                    }
                    /*-- for preventing the Alphabets and special characters to be printed--*/
                    element.bind("keydown", function (event) {
                            var model = $appUtil.getModel($scope, $scope.modelexpression, true);
                            var val = model[$scope.fieldexpression];
                            var i = event.which;
                            //65 - A  to  90-Z
                            if ((i >= 65 && i <= 90) || (i >= 186 && i <= 189) || (i == 111) || (i == 106) || (i >= 191 && i <= 192) || (i >= 219 && i <= 222) || (i == 32)) {
                                if (!((event.ctrlKey == true && i == 65) || (event.ctrlKey == true && i == 67) || (event.ctrlKey == true && i == 86) || (event.ctrlKey == true && i == 88))) {
                                    event.preventDefault();
                                }
                            }
                            else {
                                if (i == 110 || i == 190) {
                                    if (val.indexOf(".") != -1) {
                                        event.preventDefault();
                                    }
                                }
                                if (i == 107 || i == 109) {
                                    if (val !== undefined && val.toString().length > 0) {
                                        event.preventDefault();
                                    }
                                }

                            }
                        }
                    )
                }
            };
        }
    }
}]);


