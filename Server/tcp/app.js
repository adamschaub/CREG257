///////////////////////////////////////
//TCP SERVER
///////////////////////////////////////
var net = require('net');
var mongoose = require('mongoose');
var cronJob = require('cron').CronJob;
var async = require('async');
var eKey = require('./models/e_keys.js').eKey;
var Lock = require('./models/lock.js').Lock;

var socks = {};
var reqStr = new RegExp(/^(.*):(.*)/);
var cmdStr = new RegExp(/(.*)/);

var dbConfig = require('./config/Database.js');

mongoose.connect(dbConfig.uri, function(err) {
    if(err) {
        console.log("Couldn't connect to " + dbConfig.uri + ": Connecting to localhost/phnky");
        mongoose.connect('localhost/phnky');
    }
});

var job = new cronJob('*/10 * * * * *', function() {
    console.log("All locks: ");
    async.forEach(Object.keys(socks),
            function(key, next) {
                console.log(key + ": " + socks[key].id);

                eKey.find({lockId: socks[key].id}, function(err, ekeys) {
                    socks[key].con.write("You have " + ekeys.length + " eKeys");
                });

                next();
            },
            function(err) {
                if(err) {
                    console.err(err);
                }
                else {
                    console.log("Done pushing updates");
                }
            });
}, null, true, "America/Los_Angeles");

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
            console.log("Status from " + socks[con.name].id);
            console.log(text[2]);
        }
        else if(text[1] === "id") {
            Lock.findOne({device: text[2]}, function(err, lock) {
                if(lock) {
                    console.log("Registering ID " + text[2] + " to connection " + con.name);
                    socks[con.name] = {
                        'id': text[2],
                        'con': con
                    };
                    con.write("sendid:success");
                }
                else{
                    con.write("sendid:failure")
                }
            });
            //check for lock in database
        }
        else if(text === "none") {
            text = cmdStr.exec(e);
            if(text[1] === "exit") {
                con.end();
            }
        }
    });

    con.on('end', function () {
        console.log(con.name + " has disconnected. Removing from connections.");
        delete socks[con.name];
    });
}).listen(process.env.PORT);

console.log("TCP listening on " + process.env.PORT);

