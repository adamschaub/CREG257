
//Requires
var LocalStrategy = require('passport-local').Strategy;
var mongoose = require('mongoose');

//Models
var User = require('../app/models/user').User;
var Lock = require('../app/models/lock').Lock;

module.exports = function(passport) {

    //sets up sessions
    passport.serializeUser(function(user, next) {
        next(null, user.id);
    });

    //deserialize to find user by ID
    passport.deserializeUser(function(id, next) {
        User.findById(id, function(err, user) {
            next(err, user);
        });
    });

    passport.use('local-login', new LocalStrategy({
        passReqToCallback: true
    },
    function(req, username, password, next) {

        process.nextTick(function () {
            User.findOne({'username': username}, function(err, user) {
                if(err) return next(err);
                if(user) {
                    if(user.comparePassword(password)) {
                        Lock.findOne({'owner': user.username}, function(err, lock) {
                            if(err) return next(err);
                            if(lock) {
                                req.session.lock = lock;
                            }
                            return next(null, user);
                        });
                    }
                    else return next(null, false, req.flash('loginMessage', 'Incorrect Username/Password'));
                }
                else {
                    return next(null, false, req.flash('loginMessage', username));
                }

            });

        });

    }));

    passport.use('local-register', new LocalStrategy({
        passReqToCallback: true
    },
    function(req, username, password, next) {

        process.nextTick(function() {

        User.findOne({'username': username}, function(err, user) {
            if(err) return next(err);

            //user exists
            if(user) {
                return next(null, false, req.flash('registerMessage', 'A user under that name already exists.'));
            }
            else {
                var nUser = new User({
                    'username': username,
                    'password': password,
                    'name': {
                        'first': req.body.firstname,
                        'last': req.body.lastname
                    },
                    'email': req.body.email,
                    'emailVerified': false,
                    'lastLogon': Date.now(),
                    'lockedUntil': 0
                });


                nUser.save(function(err) {
                    if(err) throw err;
                    return next(null, nUser);
                });
            }
        });
        });
    }));
}
