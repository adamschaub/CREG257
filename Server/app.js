///////////////////////////////////////
//MODULES
///////////////////////////////////////
var net = require('net');
var express = require('express');
var app = express();
var hbs = require('hbs');
var mongoose = require('mongoose');
var passport = require('passport');
var flash = require('connect-flash');

var cookieStr = "phnkyphnkyphnky";
var cookieAge = 10*60*1000;
///////////////////////////////////////
//HTTP SERVER
///////////////////////////////////////
var dbConfig = require('./config/Database.js');

mongoose.connect(dbConfig.uri || 'localhost/phnky');

app.use(express.static(__dirname + '/public'));
hbs.registerPartials(__dirname + "/views/partials"); 

app.configure(function(){
    app.use(express.logger('dev'));
    app.use(express.cookieParser());
    app.use(express.bodyParser());

    app.set('view engine', 'html');

    app.use(express.session({secret: cookieStr, cookie: {maxAge: cookieAge}}));
    app.use(passport.initialize());
    app.use(passport.session());
    app.engine('html', hbs.__express); //set handlebars to render pages   
    app.use(flash());
});

require('./config/passport')(passport);
require('./routes/routes.js')(app, passport);

var port = process.env.PORT || 5000;
app.listen(port, function(){
    console.log("HTTP listening on " + port);
});


///////////////////////////////////////
//TCP SERVER
///////////////////////////////////////
var socks = {};
var reqStr = new RegExp(/^(.*):(.*)/);
var cmdStr = new RegExp(/(.*)/);

var server = net.createServer({allowHalfOpen:true}, function(con) {

    con.name = con.remoteAddress + ":" + con.remotePort;
    console.log("New connection made to " + con.name);
    console.log("Requesting deviceID");
    con.write("sendid");

    con.on('data', function(e) {
        e = e.toString().replace(/(\n|\r)+/, '').trim();

        var text = reqStr.exec(e) || "none";
        if(text[1] === "update") {
            console.log("Update requested: " + text[2]);
        }
        else if(text[1] === "status" && socks[con.name]) {
            console.log("Status from " + socks[con.name]);
            console.log(text[2]);
        }
        else if(text[1] === "id") {
            console.log("Registering ID " + text[2] + " to connection " + con.name);
            socks[con.name] = text[2];
            //check for lock in database
        }
        else if(text === "none") {
            text = cmdStr.exec(e);
            if(text[1] === "getlocks") {
                con.write(JSON.stringify(socks));
            }
            else if(text[1] === "exit") {
                con.end();
            }
        }
    });

    con.on('end', function () {
        console.log(con.name + " has disconnected. Removing from connections.");
        delete socks[con.name];
    });
}).listen(8080);

console.log("TCP listening on 8080");