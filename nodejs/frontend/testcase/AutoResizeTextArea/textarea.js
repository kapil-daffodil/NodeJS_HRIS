var webApp = angular.module("webApp", []);

webApp.directive("textareaAutoResize", ["$compile" , function ($compile) {
    return {
        restrict:'E',
        template:"<div></div>",
        scope:true,
        replace:true,
        compile:function () {
            return{
                post:function ($scope, iElement, attrs) {
                    var toBind = attrs.model + "." + attrs.field;

                    var textareaTemplate = '<textarea class="app-auto-resize-teztarea" ng-model="' + toBind + '"></textarea>';
                    $(iElement).append($compile(textareaTemplate)($scope));

                    var textAreaElement = angular.element(iElement).find('textarea');


                    var $clone = $('<div></div>').css({
                        'position':'absolute',
                        'display':'none',
                        'word-wrap':'break-word',
                        'white-space':'pre-wrap',
                        'width':(textAreaElement.width() - 10) + "px"

                    }).appendTo(document.body);


                    var hasBoxModel = textAreaElement.css('box-sizing') == 'border-box' || textAreaElement.css('-moz-box-sizing') == 'border-box' || textAreaElement.css('-webkit-box-sizing') == 'border-box';
                    var heightCompensation = parseInt(textAreaElement.css('border-top-width')) + parseInt(textAreaElement.css('padding-top')) + parseInt(textAreaElement.css('padding-bottom')) + parseInt(textAreaElement.css('border-bottom-width'));
                    var textareaHeight = parseInt(textAreaElement.css('height'), 10);
                    var lineHeight = parseInt(textAreaElement.css('line-height'), 10) || parseInt(textAreaElement.css('font-size'), 10);
                    var minheight = lineHeight * 2 > textareaHeight ? lineHeight * 2 : textareaHeight;
                    var maxheight = parseInt(textAreaElement.css('max-height'), 10) > -1 ? parseInt(textAreaElement.css('max-height'), 10) : Number.MAX_VALUE;

                    function updateHeight() {
                        var textareaContent = textAreaElement.val().replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/&/g, '&amp;').replace(/\n/g, '<br/>');

                        $clone.html(textareaContent + '&nbsp;');
                        if (!$scope.$$phase) {
                            $scope.$apply();
                        }
                        setHeightAndOverflow();
                    }

                    function setHeightAndOverflow() {
                        var cloneHeight = $clone.height();
                        var overflow = 'hidden';
                        var height = hasBoxModel ? cloneHeight + lineHeight + heightCompensation : cloneHeight + lineHeight;
                        if (height > maxheight) {
                            height = maxheight;
                            overflow = 'auto';
                        } else if (height < minheight) {
                            height = minheight;
                        }
                        if (textAreaElement.height() !== height) {
                            textAreaElement.css({'overflow':overflow, 'height':height + 'px'});
                        }
                    }


                    textAreaElement.bind('keyup change cut paste', function () {
                        updateHeight();
                    });

                    textAreaElement.bind('blur', function () {
                        setHeightAndOverflow();
                    });

                    if ($scope[attrs.model][attrs.field]) {
                        textAreaElement.val($scope[attrs.model][attrs.field]);
                        updateHeight();
                    }

                }
            }
        }
    }
}]);


