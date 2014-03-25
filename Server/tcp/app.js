///////////////////////////////////////
//TCP SERVER
///////////////////////////////////////
var net = require('net');
var mongoose = require('mongoose');
var cronJob = require('cron').CronJob;
var async = require('async');
var eKey = require('./models/e_keys.js').eKey;
var Lock = require('./models/lock.js').Lock;
var User = require('./models/user.js').User;

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
    async.forEach(Object.keys(socks),
            function(key, next) {

                var now = new Date(Date.now());
                console.log(now.toJSON());
                eKey.find({lockId: socks[key].id , nextActive: {$lte: now} , end: {$gte: now}}).populate('contact').exec(function(err, ekeys) {
                    Lock.find({device: socks[key].id}, function(err, lock) {
                        console.log(ekeys.length + " ekeys");
                        var updateStr = "update:";

                        if(lock.remoteOpen) updateStr += "o";
                        else updateStr += "c";

                        if(lock.disabled) updateStr += "d";
                        else updateStr += "e";

                        var numUpdates = 0;
                        var macUpdate = "";
                        for(var i = 0; i < ekeys.length; ++i) {
                            //check if key must be added, deleted, disabled, or enabled
                            if(ekeys[i].contact.phone.MAC && ekeys[i].nextDisable >= now) {
                                numUpdates++;
                                macUpdate += ekeys[i].contact.phone.MAC;
                            }

                            if(ekeys[i].nextDisable <= now) {
                                if(ekeys[i].interval === "none") {
                                    ekeys[i].nextActive = ekeys[i].end;
                                }
                                else if(ekeys[i].interval === "daily") {
                                    ekeys[i].nextActive.addDays(1);
                                    ekeys[i].nextDisable.addDays(1);
                                }
                                else if(ekeys[i].interval === "weekly") {
                                    ekeys[i].nextActive.addDays(7);
                                    ekeys[i].nextDisable.addDays(7);
                                }

                                ekeys[i].markModified('nextActive');
                                ekeys[i].markModified('nextDisable');
                                ekeys[i].save(function(err, ekey) {
                                    if(err) console.log(err);
                                });
                            }

                        }

                        updateStr += formatNumberLength(numUpdates, 2) + macUpdate;
                        console.log(updateStr);
                        socks[key].con.write(updateStr + "\r");
                    });
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
}, null, true, "GMT");

//format number
function formatNumberLength(num, len) {
    var n = "" + num;
    while(n.length < len) {
        n = "0" + n;
    }
    return n;
}

Date.prototype.addHours = function(h) {
    this.setHours(this.getHours() + h);
}

Date.prototype.addMinutes = function(m) {
    this.setMinutes(this.getMinutes() + m);
}

Date.prototype.addDays = function(d) {
    this.setDate(this.getDate() + d);
}

var port = process.env.PORT || 5000;
var server = net.createServer({allowHalfOpen:true}, function(con) {

    con.name = con.remoteAddress + ":" + con.remotePort;
    console.log("New connection made to " + con.name);
    console.log("Requesting deviceID");
    con.write("sendid\r");

    setTimeout(requestId, 3000, con);

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
                    con.write("sendid:success\r");
                }
                else{
                    con.write("sendid:failure\r")
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

    con.on('error', function() {
        console.log("Connection closed");
    });

}).listen(port);

function requestId(con) {
    if(!socks[con.name] && con.writable) {
        console.log("Requesting DeviceID");
        con.write("sendid\r");
        setTimeout(requestId, 3000, con);
    }
}

console.log("TCP listening on " + process.env.PORT);

