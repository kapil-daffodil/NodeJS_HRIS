var BAAS_SERVER;
var windowUrl = (window.location.href);
if (windowUrl.indexOf("127.0.0.1") >= 0 || windowUrl.indexOf("localhost") >= 0) {
    BAAS_SERVER = "http://127.0.0.1:1337";
} else {
    BAAS_SERVER = "http://173.255.119.199:1337";
}
var DATA_NOT_FOUND = 'No Data Found'; // when data is not found in lookup column, show no data found

/*****************Lookup********************************/


appStrapDirectives.factory('$appDataSource', ['$http', '$timeout', '$rootScope', '$dataModel',
    function ($http, $timeout, $rootScope, $dataModel) {
        var $appDataSource = {
        };

        $appDataSource.getLookupDataSource = function (lookupOptions) {
            this.lookupOptions = lookupOptions;

            this.getData = function (query, callBack, errorCallBack) {
                var componentId = this.lookupOptions["componentid"];
                var currentRow = $dataModel.getCurrentRow(componentId);
                var displayExpression = this.lookupOptions.primarycolumns[0].expression;

                var resourceTable = this.lookupOptions.table;
                if (angular.isObject(resourceTable)) {
                    resourceTable = resourceTable.id;
                }
                var resourceQuery = {"table":resourceTable, "columns":this.lookupOptions.primarycolumns};
                var filter;
                if (this.lookupOptions.filter) {
                    filter = this.lookupOptions.filter
                } else {
                    filter = {};
                }
                filter[displayExpression] = {"$regex":"^(" + query + ")", "$options":"-i"};
                var parameters = {};
                if (currentRow) {
                    parameters = angular.copy(currentRow);
                }
                var currentView = $dataModel.getView(componentId);
                if (currentView) {
                    var currentViewParameters = currentView.metadata.parameters;
                    if (currentViewParameters) {
                        Object.keys(currentViewParameters).forEach(function (k) {
                            if (parameters[k] === undefined) {
                                parameters[k] = currentViewParameters[k];
                            }
                        })
                    }
                }

                resourceQuery.filter = filter;
                resourceQuery.parameters = parameters;
                var usk;
                if (typeof(Storage) !== "undefined") {
                    usk = localStorage.usk;
                }
                var urlParams = {ask:this.lookupOptions.ask, osk:this.lookupOptions.osk, query:JSON.stringify(resourceQuery) };
                if (usk) {
                    urlParams.usk = usk;
                }
                $appDataSource.getDataFromService(BAAS_SERVER + "/data", urlParams, "GET", "JSON", "Loading...", callBack, errorCallBack);
            }
        }


        $appDataSource.getDataFromService = function (url, requestBody, callType, dataType, pBusyMessage, callback, errcallback) {

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
        return $appDataSource;

    }
]);

/*********************************************************************Lookup type ahead**********************************************************************************/

appStrapDirectives.directive('ngModelRemove', function () {
    return {
        restrict:'A',
        require:'ngModel',
        link:function (scope, elm, attr, ngModelCtrl) {
            if (attr.type === 'radio' || attr.type === 'checkbox') return;

//            elm.unbind('input').unbind('keydown').unbind('change');
            elm.unbind('input').unbind('change');
//            elm.bind('blur', function () {
//                scope.$apply(function () {
//                    ngModelCtrl.$setViewValue(elm.val());
//                });
//            });
        }
    };
});

appStrapDirectives.directive('appMultipleRefrence', ['$timeout', '$appUtil', function ($timeout, $appUtil) {
    'use strict';
    return {
        restrict:'E',
        scope:true,
        replace:true,
        template:"<div class='app-multirefrence-parent'>" +
            "<span ng-bind='getValue' class='app-multirefrence-text'></span>" +
            "<input  type='text' tabindex='-1' class='app-multirefrence-cross app-cursor-pointer' style='color:#000000;width:10px;background: none;border:none;padding: 0px;margin: 0px;' ng-click='cancel($event)' value='X'/>" +
            "</div>",
        compile:function () {
            return{
                pre:function ($scope, iElement) {
                    $scope.cancel = function ($event) {
                        var selectedIndex = ($scope.$index);
                        var model = $appUtil.getModel($scope, $scope.modelexpression, false)

                        if (model) {
                            model.splice(selectedIndex, 1);
                            $event.preventDefault();
                            $event.stopPropagation();
                        }


                    };
                    $scope.getValue = function () {
                        if ($scope.option instanceof Object) {
                            return $scope.option[$scope.fieldexpression];
                        } else {
                            return $scope.option;
                        }
                    };
                }
            };
        }
    };
}
]);

'use strict';
appStrapDirectives.directive('appLookup', ['$compile', '$timeout', '$appUtil', function ($compile, $timeout, $appUtil) {
    'use strict';
    return {
        restrict:'E',
        template:"<div></div>",
        scope:true,
        replace:true,
        compile:function () {
            return  {

                post:function ($scope, iElement, attrs) {
                    var optionsDiv = $('#app_popup');
                    if (optionsDiv.length == 0) {
                        $(document.body).append("<div id='app_popup'></div>");
                    }

                    var modelExpression = attrs.model;
                    var fieldExpression = attrs.field;

                    var bindType = attrs.bindtype;
                    var ds = attrs.datasource;
                    if (!bindType) {
                        if (ds && $scope[ds] && $scope[ds].length > 0 && (typeof $scope[ds][0] !== 'object')) {
                            bindType = "string";
                        } else if (fieldExpression.indexOf(".") >= 0) {
                            bindType = "object";
                        } else {
                            bindType = "string";
                        }
                    }
                    var multiple = attrs.multiple;

                    var fieldFirstExpression;
                    var fieldLastDotExpression;
                    var dottIndex = fieldExpression.lastIndexOf(".");
                    if (dottIndex >= 0) {
                        fieldFirstExpression = fieldExpression.substring(0, dottIndex);
                        fieldLastDotExpression = fieldExpression.substring(dottIndex + 1);

                    } else {
                        fieldFirstExpression = fieldExpression;
                    }


                    if (multiple === undefined) {
                        var modelValue = $appUtil.getModel($scope, modelExpression, false);
                        if (!modelValue) {
                            multiple = false;
                        } else {
                            modelValue = $appUtil.getModel(modelValue, fieldFirstExpression, false);
                            if (modelValue) {
                                if (modelValue instanceof Array) {
                                    multiple = true
                                } else {
                                    multiple = false;
                                }
                            }
                        }
                    }

                    var toBind;


                    toBind = modelExpression + "." + fieldFirstExpression;
                    if (multiple) {
                        if (bindType == 'string' && fieldLastDotExpression) {
                            toBind += "." + fieldLastDotExpression;
                            fieldLastDotExpression = undefined;
                        }
                        modelExpression = toBind;
                    } else {
                        if (fieldLastDotExpression) {
                            modelExpression = toBind;
                            toBind += "." + fieldLastDotExpression;
                        } else {
                            fieldLastDotExpression = fieldFirstExpression;
                        }
                    }

                    if (multiple || multiple == 'true') {
                        multiple = true;
                    } else {
                        multiple = false;
                    }
                    $scope.multiple = multiple;
                    $scope.bindtype = bindType;
                    $scope.modelexpression = modelExpression;
                    $scope.fieldexpression = fieldLastDotExpression;


                    var placeholder = attrs.placeholder;
                    if (!placeholder) {
                        placeholder = '';
                    }

                    var border = attrs.border;
                    if (border === undefined || border == true || border == 'true') {
                        border = true
                    } else {
                        border = false;
                    }

                    var lookUpTemplate = "<div  class ='app-look-up-container app-look-up-container-background' ng-class=\"{'app-border':" + border + "}\">" +
                        "<div class='app-look-up-text-container'>" +
                        "<input  ng-model-remove class='ref-input focus-element' type = 'text'   placeholder='" + placeholder + "' ng-model='" + toBind + "' data-popup-id='app_popup' />" +

                        "</div>" +
                        "<input type='text' class='second-input' style='color: white' tabindex='-1'  class='ref-button app-cursor-pointer'>" +
                        "</div>" +
                        "</div>";
                    var multipleTemplate = "<div class='app-float-left' style='height: auto;width: 100%;' ng-class=\"{'app-border':" + border + "}\">" +

                        "<div class='app-float-left' style='width:80%;' class='app-white-backgroud-color' >" +
                        "<app-multiple-refrence ng-repeat='option in " + toBind + "'></app-multiple-refrence>" +

                        '<input    type ="text" placeholder="' + placeholder + '" class="app-float-left focus-element"  size="{{placeholder.length}}"  data-popup-id="app_popup" style="border:none;margin-left: 2px;height:' + $(iElement).height() + 'px;"/>' +
                        "</div>" +

                        "<div class='app-float-left' style='width: 20%;height: 100%;' class='app-white-backgroud-color'>" +
                        "<input type='text' style='color: white' class='app-multi-refrence-down-arrow' tabindex='-1' >" +
                        "</div>" +

                        "</div>"
                    if (multiple) {
                        $(iElement).append($compile(multipleTemplate)($scope));
                    } else {
                        $(iElement).append($compile(lookUpTemplate)($scope));
                    }

                    $scope.$menu = $('<ul class="typeahead dropdown-menu"></ul>');
                    $scope.item = '<li><a href="#"></a></li>';
                    $scope.minLength = 1;
                    var inputElements = angular.element(iElement).find('input');
                    var inputElement = inputElements[0];
                    var showAllDivElement = inputElements[1];
                    $scope.timeout = null;
                    $scope.$showAllElement = $(showAllDivElement);
                    $scope.$element = $(inputElement)
                    $scope.shown = false
                    $scope.showingLoadingImage = false
                    $scope.lastSelected = null;
                    $scope.select = function () {
                        var selectedIndex = $scope.$menu.find('.active').index();
                        $scope.updater($scope.data[selectedIndex]);
                        return $scope.hide()
                    };
                    $scope.show = function () {

                        var dataPopupId = $scope.$element.attr("data-popup-id");
                        var dataPopupElement = $("#" + dataPopupId);

                        var offset = $scope.$element.offset();

                        var posY = offset.top - $(window).scrollTop();
                        var posX = offset.left - $(window).scrollLeft();
                        var elemHeight = $scope.$element[0].offsetHeight;

                        var top = offset.top
                        var windowHeight = $(window).height();


                        dataPopupElement
                            .append($scope.$menu)
                            .css({
                                top:((top + $scope.$menu.height() + $scope.$element.height()) >= windowHeight) ? (top - $scope.$element.height() - $scope.$menu.height() ) : posY + elemHeight,
                                left:posX,
                                position:"absolute"
                            });
                        $scope.$menu.show();

                        $scope.shown = true

                        return this
                    };
                    $scope.hide = function () {
                        $scope.$menu.hide();
                        var dataPopupId = $scope.$element.attr("data-popup-id");
                        var dataPopupElement = $("#" + dataPopupId);
                        $scope.shown = false
                        return this
                    };

                    $scope.highlighter = function (item) {
                        var query = $scope.query.replace(/[\-\[\]{}()*+?.,\\\^$|#\s]/g, '\\$&')
                        return item.replace(new RegExp('(' + query + ')', 'ig'), function ($1, match) {
                            return '<strong>' + match + '</strong>'
                        })
                    };
                    $scope.render = function (items) {
                        $scope.data = items;
                        items = $(items).map(function (i, item) {
                            if (angular.isObject(item)) {
                                item = item[$scope.fieldexpression];
                            }

                            i = $($scope.item);
                            i.find('a').html($scope.highlighter(item))
                            return i[0]
                        })

                        items.first().addClass('active')
                        $scope.$menu.html(items)
                        return this
                    };
                    $scope.next = function (event) {
                        var active = $scope.$menu.find('.active').removeClass('active')
                            , next = active.next()

                        var ulHeight = $scope.$menu.height();
                        var ulTop = $scope.$menu.scrollTop();
                        var activeLiTop = active.position().top;
                        var liHeight = $scope.$menu.find('li').height();


                        if (!next.length) {
                            next = $($scope.$menu.find('li')[0])
                            $scope.$menu.scrollTop(0);
                        } else {
                            if ((activeLiTop + liHeight) > ulHeight) {
                                var setScrollTop = ulTop + liHeight;
                                $scope.$menu.scrollTop(setScrollTop);
                            }
                        }

                        next.addClass('active')
                    };
                    $scope.prev = function (event) {
                        var active = $scope.$menu.find('.active').removeClass('active')
                            , prev = active.prev()

                        var ulHeight = $scope.$menu.height();
                        var ulTop = $scope.$menu.scrollTop();
                        var liHeight = $scope.$menu.find('li').height();

                        if (!prev.length) {
                            prev = $scope.$menu.find('li').last()
                            $scope.$menu.scrollTop($scope.$menu.find('li').last().position().top);
                        } else {
                            var activeLiTop = prev.position().top;
                            if (activeLiTop < 0) {
                                var setScrollTop = ulTop - liHeight;
                                $scope.$menu.scrollTop(setScrollTop);
                            }
                        }

                        prev.addClass('active')
                    };
                    $scope.listen = function () {
                        $scope.$element
                            .on('focus', $scope.focus)
                            .on('blur', $scope.blur)
                            .on('keypress', $scope.keypress)
                            .on('keyup', $scope.keyup)
                            .on('keydown', $scope.keydown)
                        $scope.$menu
                            .on('click', $scope.click)
                            .on('mouseenter', 'li', $scope.mouseenter)
                            .on('mouseleave', 'li', $scope.mouseleave)

                        $scope.$showAllElement
                            .on('click', $scope.showAllElementClick)
                    };

                    $scope.move = function (e) {
                        if (!$scope.shown) return

                        switch (e.keyCode) {
                            case 9: // tab
                            case 13: // enter
                            case 27: // escape
                                e.preventDefault()
                                break

                            case 38: // up arrow
                                e.preventDefault()
                                $scope.prev()
                                break

                            case 40: // down arrow
                                e.preventDefault()
                                $scope.next()
                                break
                        }

                        e.stopPropagation()
                    };
                    $scope.keydown = function (e) {
                        $scope.suppressKeyPressRepeat = ~$.inArray(e.keyCode, [40, 38, 9, 13, 27])
                        $scope.move(e)
                    };
                    $scope.keypress = function (e) {
                        if ($scope.suppressKeyPressRepeat) return
                        $scope.move(e)
                    };
                    $scope.keyup = function (e) {
                        switch (e.keyCode) {
                            case 40: // down arrow
                            case 38: // up arrow
                            case 16: // shift
                            case 17: // ctrl
                            case 18: // alt
                                break

                            case 9: // tab
                            case 13: // enter

                                if (!$scope.shown) return
                                $scope.select()
                                break

                            case 27: // escape
                                if (!$scope.shown) return
                                $scope.hide()
                                break

                            default:
                                $scope.lookup(e, false)
                        }

                        e.stopPropagation()
                        e.preventDefault()
                    };
                    $scope.focus = function (e) {
                        $scope.focused = true
                    };
                    $scope.blur = function (e) {
                        $timeout(function () {
                            if ($scope.$element.is(":focus")) {
                                return;
                            }
                            $scope.focused = false;
                            var value = $scope.$element.val();
                            $scope.updater(value);

                            if (!$scope.mousedover && $scope.shown) $scope.hide()
                        }, 200);

                    };
                    $scope.click = function (e) {
                        e.stopPropagation()
                        e.preventDefault()
                        $scope.select()
                        $scope.$element.focus()
                    };
                    $scope.mouseenter = function (e) {
                        $scope.mousedover = true
                        $scope.$menu.find('.active').removeClass('active')
                        $(e.currentTarget).addClass('active')
                    };
                    $scope.mouseleave = function (e) {
                        $scope.mousedover = false
                        if (!$scope.focused && $scope.shown) $scope.hide()
                    };
                    $scope.showAllElementClick = function (e) {
                        $scope.$element.focus();
                        $scope.lookup(e, true);
                    };
                    $scope.lookup = function (ev, showAll) {
                        var items;
                        if (showAll) {
                            $scope.query = '';
                        } else {
                            $scope.query = $scope.$element.val();
                        }
                        $scope.source($scope.query, $scope.process);

                    };
                    $scope.source = function (query, callBack) {

                        if ($scope[attrs.datasource] instanceof Array) {
                            var options = $scope[attrs.datasource];
                            var optionsCount = options.length;
                            var optionsToShow = [];

                            if (query && query.length > 0) {
                                for (var i = 0; i < optionsCount; i++) {
                                    var option = options[i];
                                    if (option.toLowerCase().match("^" + query.toLowerCase())) {
                                        optionsToShow.push(option);
                                    }
                                }
                            } else {
                                optionsToShow = options;
                            }

                            callBack(optionsToShow);
                            return;
                        }
                        var items;

                        if ($scope.timeout != null) {
                            $timeout.cancel($scope.timeout);
                        }


                        $scope.timeout = $timeout(function () {

                            var loadingHtml = '<img src="../images/loading.gif" class="app-input-loading-image">';
                            $(loadingHtml).insertAfter($scope.$element);
                            $scope.showingLoadingImage = true;

                            $scope[attrs.datasource].getData(query, function (data) {
                                $timeout.cancel($scope.timeout);
                                $scope.timeout = null;

                                $($scope.$element).next().remove();
                                $scope.showingLoadingImage = false;

                                if (!$scope.$element.is(":focus")) {
                                    return;
                                }

                                if (query == '' || query == $scope.$element.val()) {
                                    var optionData = data.data;
                                    if (optionData.length == 0) {
                                        optionData.push(DATA_NOT_FOUND);
                                    }
                                    callBack(optionData);
                                }
                            });
                        }, 200);


                    };
                    $scope.process = function (items) {

                        if (!items.length) {
                            return $scope.shown ? $scope.hide() : this
                        }

                        return $scope.render(items.slice(0, $scope.items)).show()
                    };

                    $scope.updater = function (value) {

                        var multiple = $scope.multiple;
                        var modelExpression = $scope.modelexpression;
                        var fieldExpression = $scope.fieldexpression;
                        var bindType = $scope.bindtype;
                        var confirmType;
                        if (multiple) {
                            confirmType = "array";
                        } else {
                            confirmType = "object";
                        }


                        var lastValue = $scope.lastUpdatedValue;
                        if (lastValue) {
                            if ((lastValue instanceof Object) && fieldExpression) {
                                lastValue = lastValue[fieldExpression];
                            }
                            var v = value;
                            if ((v instanceof Object) && fieldExpression) {
                                v = v[fieldExpression];
                            }
                            if (v == lastValue) {
                                return;
                            }

                        }
                        $scope.lastUpdatedValue = value;


                        var model = $appUtil.getModel($scope, modelExpression, true, confirmType);


                        if (multiple) {
                            $scope.$element.val("");
                            if (value === undefined || value.toString().trim().length == 0) {
                                return;
                            }
                            if (bindType == 'object' && !(value instanceof Object)) {
                                var v = {}
                                v[fieldExpression] = value;
                                value = v;
                            }
                            model.push(value);
                        } else {

                            if (bindType == 'object') {
                                Object.keys(model).forEach(function (k) {
                                    delete model[k];
                                })
                                if (value instanceof Object) {
                                    Object.keys(value).forEach(function (k) {
                                        model[k] = value[k];
                                    })
                                } else {
                                    model[fieldExpression] = value;
                                }

                            } else {
                                model[fieldExpression] = value;

                            }
                        }


                        var onChangeFn = $scope.$element.attr('onselection');
                        if (onChangeFn) {
//                            scope[onChangeFn](updatedValue, scope.column);
                        }
                        if (!$scope.$$phase) {
                            $scope.$apply();
                        }

                        return value;


                    }
                    $scope.listen();
                }

            };
        }

    };
}
]);
/*********************************************************************End of Lookup type ahead**********************************************************************************/

/*****************END of Lookup************************/