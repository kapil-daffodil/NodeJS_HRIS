function DATECTL($scope){
    $scope.firstday;
    $scope.lastday;
    $scope.curr;
    $scope.formattingStyle = {dateFormat : "dddd, mmmm d, yyyy",startingWeekFormat : "mmmm d ",endingWeekFormat : "d, yyyy",monthFormat : "mmmm yyyy"};
    $scope.year;
    $scope.selected = {dateTemp:false, monthTemp:false, yearTemp:false, weekTemp:false};
    $scope.currmonth;

    $scope.current = function(isDate,isMonth,isYear,isWeek) {
        $scope.curr = new Date;

        if(isDate){
            $scope.selected.dateTemp = true;
            $scope.selected.weekTemp = false;
            $scope.selected.monthTemp = false;
            $scope.selected.yearTemp = false;
        }else if(isMonth){
            $scope.selected.monthTemp = true;
            $scope.selected.weekTemp = false
            $scope.selected.dateTemp = false
            $scope.selected.yearTemp = false;
        }else if(isYear){
            $scope.selected.yearTemp = true;
            $scope.selected.weekTemp = false;
            $scope.selected.monthTemp = false;
            $scope.selected.dateTemp= false;
        }else if(isWeek){
            $scope.selected.weekTemp = true;
            $scope.selected.dateTemp = false;
            $scope.selected.monthTemp = false;
            $scope.selected.yearTemp = false;
        }


        if ($scope.selected.dateTemp) {
            $scope.dateinput = $scope.curr.format($scope.formattingStyle.dateFormat);
        } else if ($scope.selected.weekTemp) {
            var first = $scope.curr.getDate() - $scope.curr.getDay();
            var last = first + 6;
            $scope.firstday = new Date($scope.curr.setDate(first));
            $scope.lastday = new Date($scope.curr.setDate(last));
			
			console.log("current : " + $scope.curr.format($scope.formattingStyle.dateFormat) + " , first day : " +$scope.firstday.format($scope.formattingStyle.startingWeekFormat) + ", last : " + $scope.lastday.format($scope.formattingStyle.startingWeekFormat));
			
            $scope.dateinput = $scope.firstday.format($scope.formattingStyle.startingWeekFormat) + ' - ' + $scope.lastday.format($scope.formattingStyle.endingWeekFormat);
        } else if ($scope.selected.monthTemp) {
            $scope.currmonth = $scope.curr.getMonth();
            var firstDayT = new Date($scope.curr.getFullYear(), $scope.currmonth, 1);
            var lastDayT = new Date($scope.curr.getFullYear(), $scope.currmonth + 1, 0);
            $scope.dateinput = firstDayT.format($scope.formattingStyle.monthFormat);
        }else if($scope.selected.yearTemp){
            $scope.year = $scope.curr.getFullYear();
            $scope.dateinput = $scope.year;
        }
    }


    $scope.next = function() {

        if($scope.selected.weekTemp){
            $scope.curr  = $scope.firstday;
			console.log("Current :" +$scope.curr.format($scope.formattingStyle.dateFormat));
            var first = $scope.curr.getDate() + 7 ;
            var last = first + 6;
			console.log("first : " + first + "  , last : " + last);
            $scope.firstday = new Date($scope.curr);
			$scope.firstday.setDate(first);
            $scope.lastday = new Date($scope.curr);
			$scope.lastday.setDate(last)
           
		   $scope.dateinput = $scope.firstday.format($scope.formattingStyle.startingWeekFormat) +  ' - ' + $scope.lastday.format($scope.formattingStyle.startingWeekFormat);
			
			//$scope.dateinput = $scope.firstday +  ' - ' + $scope.lastday;
			
        }else if($scope.selected.dateTemp){
            $scope.curr = new Date($scope.curr.setDate($scope.curr.getDate() + 1));
            $scope.dateinput = $scope.curr.format($scope.formattingStyle.dateFormat);
        }else if($scope.selected.monthTemp){
            $scope.currmonth += 1;
            var firstDayT = new Date($scope.curr.getFullYear(),$scope.currmonth , 1);
            var lastDayT = new Date($scope.curr.getFullYear(), $scope.currmonth + 1, 0);
            $scope.dateinput = firstDayT.format($scope.formattingStyle.monthFormat);
         }else if($scope.selected.yearTemp){
            $scope.curr.setFullYear($scope.curr.getFullYear() + 1);
            $scope.year = $scope.curr.getFullYear();
            $scope.dateinput =   $scope.year;
        }
    }


    $scope.prev = function() {

        if($scope.selected.weekTemp){

            $scope.curr = $scope.firstday;
            var first = $scope.curr.getDate() - 7;
            var last = first + 6; // last day is the first day + 6
            $scope.firstday = new Date($scope.curr.setDate(first));
            $scope.lastday = new Date($scope.curr.setDate(last));
            $scope.dateinput = $scope.firstday.format($scope.formattingStyle.startingWeekFormat) + ' - ' + $scope.lastday.format($scope.formattingStyle.endingWeekFormat);

        }else if($scope.selected.dateTemp){

            $scope.curr = new Date($scope.curr.setDate($scope.curr.getDate() + -1));  //  for previous date
            $scope.dateinput = $scope.curr.format($scope.formattingStyle.dateFormat);

        }else if($scope.selected.monthTemp){
            $scope.currmonth -= 1;
            var firstDayT = new Date($scope.curr.getFullYear(),$scope.currmonth , 1);
            var lastDayT = new Date($scope.curr.getFullYear(), $scope.currmonth + 1, 0);

            $scope.dateinput = firstDayT.format($scope.formattingStyle.monthFormat);


        }else if($scope.selected.yearTemp){
            $scope.curr.setFullYear($scope.curr.getFullYear() - 1);
            $scope.year = $scope.curr.getFullYear();
            $scope.dateinput =  $scope.year;
        }
    }
}