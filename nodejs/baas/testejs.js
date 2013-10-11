try {

    function getName() {
        return "Rohit Bansal";
    }

    var ejs = require('ejs');
    var HEAd = "<div>tHIS IS HEAD</div>";
    var footer = "<div>this is fotter</div>"
    var center = "<div>fd</div>";
    var params = {"name":"Rohit", "getName":function (name) {
        return "hello " + name;
    }};
    var template = "<div><%=getName(name)%></div>"
    var html = ejs.render(template, params);
    console.log(html);

} catch (err) {
    console.log(err.stack);
}


