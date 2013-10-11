var app = angular.module('applane', ['$appstrap.services'], function ($routeProvider, $locationProvider) {
    $locationProvider.html5Mode(true);
});

app.controller('signup', function ($scope, $location,$appService,$timeout) {

    $scope.userSignUp = function(){
        $('#password-error').remove();
        $('#username-error').remove();
        $('#response-error').remove();

        var userName = $scope.username;
        var password = $scope.password;
        var confirmpassword = $scope.confirmpassword;

        if(userName == null || userName.trim().length == 0){
            var html = "<span class='error-message' id='username-error'>Enter your Username</span>";
            $(html).insertAfter('#username');
            return false;
        }

        if(password == null || password.trim().length == 0){
            var html = "<span class='error-message' id='password-error'>Enter your Password</span>";
            $(html).insertAfter('#password');
            return false;
        }
        if(confirmpassword == null || confirmpassword.trim().length == 0){
            var html = "<span class='error-message' id='password-error'>Confirm your Password</span>";
            $(html).insertAfter('#confirm-password');
            return false;
        }

        if(confirmpassword.toLowerCase() != password.toLowerCase()){
            var html = "<span class='error-message' id='password-error'>Passwords are not matched</span>";
            $(html).insertAfter('#confirm-password');
            return false;
        }
  //      isValidCaptcha();
        $appService.signUp(userName,password, function(data){
            var status = data.status;
            var response = data.response;

            if(status == "error"){
                alert(response);
            }

            var username = response.username;
            var isUserAlreadyExist = response.user_already_exist;


            if(status == "ok"){
                alert("SignUp Successfully");
            }

//            if(status == "ok"){
//                window.location.href = "/tasks";
//            }else if(status == "error"){
//                var html = "<span class='error-message' id='response-error'>"+response+"</span>";
//                $(html).insertAfter('#password');
//                return false;
//            }

        });


    };

    function isValidCaptcha(){
        var xmlhttpTemp =   GetXmlHttpObject();
        if (xmlhttpTemp == null){
            alert ("Your browser does not support AJAX!");
            return;
        }
        var url="/hs/recaptcha";
        var ch = document.getElementsByName('recaptcha_challenge_field')[0].value;
        var re=document.getElementsByName('recaptcha_response_field')[0].value;
        if(re.length == 0){
            alert("Please enter corrent value in Captcha");
            return false;
        }
        xmlhttpTemp.open("POST",url,true);
        xmlhttpTemp.setRequestHeader("content-type", "application/x-www-form-urlencoded");
        xmlhttpTemp.send('recaptcha_challenge_field='+ch+'&recaptcha_response_field='+re);
        xmlhttpTemp.onreadystatechange = function() {
            if (xmlhttpTemp.readyState==4 || xmlhttpTemp.readyState=="complete")
            {
                returnValue=xmlhttpTemp.responseText;
                if(returnValue=='equal'){
                    document.forms[0].submit();
                }
                else{
                    alert("Please enter corrent value in Captcha");
                }
            }
        };
    }

    function GetXmlHttpObject() {
        if (window.XMLHttpRequest) {
            return new XMLHttpRequest();
        } else {
            try {
                return new ActiveXObject('MSXML2.XMLHTTP.3.0');
            }catch (e) {
                return new ActiveXObject("Microsoft.XMLHTTP");
            }
        }
    }




});

