var http = require('http');

var onRequest = function (req, res) {
    var responseType = {"Content Type":"applicatoin/json", "Access-Control-Allow-Origin":"*", "Access-Control-Allow-Methods":"GET, POST, OPTIONS"};
    res.writeHead(200, responseType);
    res.write("Welcome to 80 Port");
    res.end();
}
http.createServer(onRequest).listen(80);
console.log('Server running at port : 80');
