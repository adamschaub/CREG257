///////////////////////////////////////
//MODULES
///////////////////////////////////////
var net = require('net');
var express = require('express');
var app = express();
var hbs = require('hbs');
var Log = require('./src/Log');

///////////////////////////////////////
//ROUTES
///////////////////////////////////////
var index = require('./routes/index');
var login = require('./routes/login');
var logout = require('./routes/logout');
var register = require('./routes/register');

Log.level = Log.VERBOSE;
var cookieStr = "phnky";
var cookieAge = 3*60*1000;
///////////////////////////////////////
//HTTP SERVER
///////////////////////////////////////
app.use(express.static(__dirname + '/public'));

//cookies. May not use for the sake of security. 
app.use(express.cookieParser());
app.use(express.session({secret: cookieStr, cookie: {maxAge: cookieAge}}));

app.use(express.bodyParser());
app.use(express.methodOverride()); //put and post
app.set('view engine', 'html');
app.engine('html', hbs.__express); //set handlebars to render pages
hbs.registerPartials(__dirname + "/views/partials");

app.use(app.router);
app.get('/', index.get);
app.get('/login', login.get);
app.post('/login', login.post);
app.post('/logout', logout.post);
app.get('/register', register.get);
app.post('/register', register.post);

//404 status error. Use as lasts middleware (e.g. nothing was caught)
app.use(function(req, res) {
    res.status(404);

    var data = {
        title: "Page Not Found",
        loggedIn: req.session.userId
    }
    if(req.accepts('html')) {
        res.render('404', data);
        return;
    }

    res.type('txt').send('ERRNOROUTE');
});

var port = process.env.PORT || 5000;
app.listen(port, function(){
    Log.v("HTTP listening on " + port);
});

///////////////////////////////////////
//TCP SERVER
///////////////////////////////////////
var socks = {};
var reqStr = new RegExp(/^(.*):(.*)/);
var cmdStr = new RegExp(/(.*)/);

var server = net.createServer({allowHalfOpen:true}, function(con) {

    con.name = con.remoteAddress + ":" + con.remotePort;
    Log.v("New connection made to " + con.name);
    Log.v("Requesting deviceID");
    con.write("sendid");

    con.on('data', function(e) {
        e = e.toString().replace(/(\n|\r)+/, '').trim();

        var text = reqStr.exec(e) || "none";
        if(text[1] === "update") {
            Log.v("Update requested: " + text[2]);
        }
        else if(text[1] === "new") {
            Log.v("New user: " + text[2]);
        }
        else if(text[1] === "id") {
            Log.v("Registering ID " + text[2] + " to connection " + con.name);
            socks[con.name] = text[2];
        }
        else if(text === "none") {
            text = cmdStr.exec(e);
            if(text[1] === "getlocks") {
                con.write(JSON.stringify(socks));
            }
            else if(text[1] === "exit") {+9999999999999999999999999999999999
                con.end();
            }
        }
    });

    con.on('end', function () {
        Log.v(con.name + " has disconnected. Removing from connections.");
        delete socks[con.name];
    });
}).listen(8080);

Log.v("TCP listening on 8080");