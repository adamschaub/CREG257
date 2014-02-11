var base = require('../src/Base.js');
var database = require('../src/Database.js');

exports.get = function(req, res) {
    var data = {
        title: "Login",
        loggedIn: req.session.userId
    }
    if(req.query.err) data.error = true;
    req.session.lastPage = '/login';
    res.render('login', data);
};

exports.post = function(req, res, next) {
    var db = database.connect();
    
    var username = req.body.username;
    var password = req.body.password;
    database.User.findOne({'username': username}, function(err, user) {
        if(err) next(err);

        if(user) {
            user.comparePassword(password, function(err, match) {
                if(err) next(err);
                if(match) {
                    req.session.userId = user.username;
                    if(req.session.lastPage)
                        base.redirect(res, req.session.lastPage);
                    else
                        base.redirect(res, '/');
                    //set last login time
                }
                else {
                    //consider counting failed attempts over a time period
                    base.redirect(res, '/login?err=bad_login');
                }
            });
        }
        else {
            base.redirect(res, '/login?err=bad_login');
        }
    });
};