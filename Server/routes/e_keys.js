var database = require('../src/Database.js');
var base = require('../src/Base.js');

exports.get = function(req, res, next) {
    var data = {
        title: "eKeys",
        loggedIn: req.session.userId
    }
    if(req.query.err) data.error = true;

    //also check if registered with lock
    if(req.session.userId) {
        var db = database.connect(next);
        database.Lock.findOne({'owner': req.session.userObjectId}, function(err, lock) {
            if(err) next(err);
            else if(lock){
                data.device = lock.device;
            }
            req.session.lastPage = '/e_keys';
            res.render('e_keys', data);
        });
    }
    else {
        req.session.lastPage = '/e_keys';
        res.render('e_keys', data);
    }
};

exports.post = function(req, res, next) {
    var db = database.connect();

    var device = req.body.device;
    var password = req.body.password;

    database.Lock.findOne({'device': device}, function(err, lock) {
        if(err) next(err);

        if(lock) {
            lock.comparePassword(password, function(err, match) {
                if(err) next(err);
                if(match) {
                    //successfully identified lock, link to user account
                    lock.owner = req.session.userObjectId;
                }
                else {
                    //consider counting failed attempts over a time period
                    base.redirect(res, '/e_keys');
                }
            });
        }
        else {
            base.redirect(res, '/e_keys?err=bad_login');
        }
    });
};