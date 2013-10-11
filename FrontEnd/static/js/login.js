var app = angular.module('applane', ['$appstrap.directives', '$appstrap.services'], function ($routeProvider, $locationProvider) {
    $locationProvider.html5Mode(true);
});

app.controller('login', function ($scope, $location,$appService,$timeout) {

    $scope.username="pawan.phutela@daffodilsw.com";
    $scope.password = "daffodil";
    $scope.showLoginScreen = false;

    $scope.handleKeyPress = function(key) {
        if(key==13){
            $scope.userLogin();
        }
    };


    $scope.showLoginScreen = true;
//    $appService.currentUser(function(data){  // Current user call for checking, login screen will have to show or redirect to /task
//        if(data == null){
//            alert("Data is null");
//        }
//
//        var response = data.response;
//        var username = response.username;
//
//        if(username != null){
//            $scope.showLoginScreen = false;
//            window.location.href = "/tasks";
//        }else{
//            $scope.showLoginScreen = true;
//        }
//    });



    $scope.userLogin = function(){    // Calls from the HTML from Sign In Button
        $('#password-error').remove();
        $('#username-error').remove();
        $('#response-error').remove();

        var userName = $scope.username;
        var password = $scope.password;

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

        $appService.login(userName,password,function(data){
            var username = data.username;
            var response = data.response;

            if(username){
                window.location.href = "/index.html";
            }else if(status == "error"){
                var html = "<span class='error-message' id='response-error'>"+response+"</span>";
                $(html).insertAfter('#password');
                return false;
            }

        });


    };




});

