function Cntl1($window, $scope,$parse){


    var getter = $parse('first + second == third && "Rohit" || "Bansal"');
    var setter = getter.assign;
    var context = {first:"Rohit",second:"Bansal",third:"RohitBansal"};
    var locals = {first:"Kumar",second:"Bansal",third:"Rohit Bansal"};
    $scope.result1 = getter(context);

    $scope.fname = "<b>Bansal Ji</b>"

    $scope.getColValue = function(){
        return "<b>Aggarwal sahib</b>"
    }




}