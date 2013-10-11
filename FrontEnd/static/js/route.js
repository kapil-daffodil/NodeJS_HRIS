angular.module('ngView', [], function($routeProvider, $locationProvider) {
    $routeProvider.when('/ng-grid/Book/:bookId', {
        templateUrl: '/ng-grid/book.html',
        controller: BookCntl,
        resolve: {
            // I will cause a 1 second delay
            delay: function($q, $timeout) {
                var delay = $q.defer();
                $timeout(delay.resolve, 1000);
                return delay.promise;
            }
        }
    });
    $routeProvider.when('/ng-grid/Book/:bookId/ch/:chapterId', {
        templateUrl: '/ng-grid/chapter.html',
        controller: ChapterCntl
    });

    // configure html5 to get links working on jsfiddle
    $locationProvider.html5Mode(true);
});

function MainCntl($scope, $route, $routeParams, $location) {
    $scope.$route = $route;
    $scope.$location = $location;
    $scope.$routeParams = $routeParams;
}

function BookCntl($scope, $routeParams) {
    $scope.name = "BookCntl";
    $scope.params = $routeParams;
}

function ChapterCntl($scope, $routeParams) {
    $scope.name = "ChapterCntl";
    $scope.params = $routeParams;
}