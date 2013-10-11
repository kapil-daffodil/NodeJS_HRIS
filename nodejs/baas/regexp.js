//var rePattern = new RegExp(/[a-z]*/);
//
//var arrMatches = "studentid.name".match(rePattern);
//console.log(arrMatches);

//match start with { and end with }
//var patt1=new RegExp("^(#{).+(}#)$");
//console.log(patt1.test("#{aasasasa}#"));



//For split using .
//var v = "student.id.classid.classname";
//console.log(v.split("."));


//For check if contains dotted
//var patt1=/\./;
//console.log(patt1.test("studentname"));


//match end with temp
var patt1 = /.+(temp)$/;
console.log(patt1.test("temp45451tempa"));