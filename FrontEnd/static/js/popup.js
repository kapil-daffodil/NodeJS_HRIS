var openPopup = new Array();
var counter = 0;
var Z_INDEX = 5;
function Popup(autohide, modal, callback) {
    if (isNotNull(autohide)) {
        this.autohide = autohide;
    } else {
        this.autohide = true;
    }
    if (isNotNull(modal)) {
        this.modal = modal;
    } else {
        this.modal = false;
    }
    if (isNotNull(callback)) {
        this.callback = callback;
    } else {
        this.callback = null;
    }


    counter++;


    if (modal && autohide) {
        this.parentid = 'innerpopup' + counter;

    } else {
        this.parentid = 'popup' + counter;
    }
    this.id = 'popup' + counter;
    this.escEnabled = true;
}

Popup.prototype.setEscEnabled = function (pEscEnabled) {
    /** ********pop up not closed on esc key press and on click outside******************* */
    this.escEnabled = pEscEnabled;
};

Popup.prototype.setHideOnClick = function (pHideOnClick) {
    /*Popup will be closed on click any where*/
    this.hideOnClick = pHideOnClick;
};

Popup.prototype.setWidth = function (width) {
    this.width = width;

};

Popup.prototype.setHeight = function (height) {
    this.height = height;

};

Popup.prototype.setHTML = function (html) {
    this.html = html;

};

Popup.prototype.setDeffered = function (deffered) {
    this.deffered = deffered;

};

Popup.prototype.showPopup = function (relativeElement) {
    var that = this;
    if (that.deffered) {
        setTimeout(function () {
            that.show(relativeElement);
        }, 0);
    } else {
        that.show(relativeElement);
    }

};

Popup.prototype.show = function (relativeElement) {
    removeOtherAutoHidePopup();
    if (this.callback != null) {
        this.callback(true);
    }


    var left = 0;
    var top = 0;



    var popupStyle = "";
    if (this.modal) {
        popupStyle = "top:0px;left:0px;bottom:0px;right:0px;overflow:auto;";

    } else {
        popupStyle = "top:" + top + "px; left:" + left + "px;";
    }


    popupStyle += ' z-index: ' + Z_INDEX + ';';
    var innerStyle = "";
    if(this.width){
        innerStyle+="width:"+this.width+"px;";
    }
    if(this.height){
        innerStyle+="height:"+this.height+"px;";
    }
    var htmlToShow = "<div id='" + this.id + "' style='" + popupStyle + "' class='popupContainer'><div id='inner" + this.id + "' style='" + innerStyle + "' class='popupwrapper'>" + "</div></div>";
    $('body').append(htmlToShow);
    openPopup.push(this);

    var popElement = $('#' + this.id);
    var innerElement = $('#inner' + this.id);
    innerElement.append($(this.html));


    if (this.position === 'center') {
        var windowHeight = $(window).height();
        var windowWidth = $(window).width();
        var actualHeight = innerElement.height();

        var innerWidth = innerElement.width();
        top = parseInt((windowHeight - actualHeight) / 2);
        left = parseInt((windowWidth - innerWidth) / 2);
        if (top < 0) {
            top = 0;
        }
        if (left < 0) {
            left = 0;
        }


        if (this.modal) {
            innerElement.css('left', left);
            innerElement.css('top', top);
        } else {
            popElement.css('left', left);
            popElement.css('top', top);
        }
    } else if (this.position === 'top-center') {

        var windowWidth = $(window).width();

        var innerWidth = innerElement.width();
        top = 5;
        left = parseInt((windowWidth - innerWidth) / 2);

        if (left < 0) {
            left = 0;
        }

        if (this.modal) {
            innerElement.css('left', left);
            innerElement.css('top', top);
        } else {
            popElement.css('left', left);
            popElement.css('top', top);
        }
    } else  if (relativeElement) {
        var offset = $(relativeElement).offset();
        var posY = offset.top - $(window).scrollTop();
        var posX = offset.left - $(window).scrollLeft();
        var elemHeight = $(relativeElement).offsetHeight;
        if (!elemHeight) {
            elemHeight = $(relativeElement).height();
        }
        var windowWidth = $(window).width();
        var innerWidth = innerElement.width();
        if((posX + innerWidth)>windowWidth && posX >=innerWidth){
            posX = posX - innerWidth;
        }
        console.log("inner width: ["+innerWidth+"]");
        var elementToSet = popElement;
        if(this.modal){
            elementToSet = innerElement;
        }
        elementToSet
            .css({
            top:posY + elemHeight,
            left:posX,
            position:"absolute"
        });

    } else {
        throw "Either position should be defined or relative Element should be passed";
    }


    popElement.css('visibility', 'visible');
};


Popup.prototype.setPosition = function (position) {
    this.position = position;
};
Popup.prototype.hide = function () {

    if (this.callback != null) {
        var needToHide = this.callback(false);
        if (isNotNull(needToHide) && needToHide == false) {
            return;
        }
    }
    $('#' + this.id).remove();
    var popupLength = openPopup.length;
    if (popupLength > 0) {
        openPopup.splice(popupLength - 1, 1);

    }
};

function removeOtherAutoHidePopup() {
    var popupLength = openPopup.length;
    if (popupLength == 0) {
        return false;
    }
    var lastPopup = openPopup[popupLength - 1];
    var autoHide = lastPopup.autohide;
    var modal = lastPopup.modal;
    if (autoHide && !modal) {
        lastPopup.hide(null);
    }
    return true;
}
function isNull(data) {
    return data == undefined || data == null;
}

function isNotNull(data) {
    return !(data == undefined || data == null);
}

function listen() {

}
$(document).on('keydown', function (e) {
    if (e.keyCode === 27) {
        var popupLength = openPopup.length;
        if (popupLength == 0) {
            return;
        }
        var lastPopup = openPopup[popupLength - 1];


        if (lastPopup.escEnabled) {
            lastPopup.hide(null);
        }
    }
});

$(document).on('click', function (e) {

    var popupLength = openPopup.length;
    if (popupLength == 0) {
        return;
    }
    var lastPopup = openPopup[popupLength - 1];

    var lastPopupId = lastPopup.parentid;

    var modal = lastPopup.modal;

    var targetElement = (e.target || e.srcElement);

    if (isNull(targetElement)) {
        return;
    }

    if(lastPopup.hideOnClick) {
        lastPopup.hide();
    }

    var innerElementFound = false;
    var targetParent = targetElement;

    var parentIdForDetail = targetParent.id;// to close detail form
    if (isNotNull(parentIdForDetail) && parentIdForDetail == lastPopup.id && lastPopup.escEnabled) {
        lastPopup.hide();
        return;
    }

    if (!innerElementFound) {
        while (true) {
            var parentId = targetParent.id;
            if (isNotNull(parentId) && parentId == lastPopupId) {
                innerElementFound = true;
                break;
            }

            targetParent = targetParent.parentNode;
            if (isNotNull(targetParent)) {
                var tagName = targetParent.tagName;
                if (isNull(tagName) || tagName.toLowerCase() == 'html' || tagName.toLowerCase() == 'body') {
                    break;
                }
            } else {
                break;
            }

        }
    }

    if (modal) {
        return;
    }
    if (!innerElementFound && lastPopup.escEnabled) {
        lastPopup.hide();
    }

});
