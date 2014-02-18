var database = require('../src/Database.js');
var base = require('../src/Base.js');

exports.get = function(req, res) {
    var data ={};
    req.session.lastPage = '/register';
    res.render('register', data);
};

exports.post = function(req, res) {
    var db = database.connect();

    var username = req.body.username;
    var password = req.body.password; //client checks passwords match
    var email = req.body.email;
    var firstname = req.body.firstname;
    var lastname = req.body.lastname;

    var registrationError = {
        usernameError: false,
        emailError: false
    };

    //TODO: catch and handle errors
    database.User.find({'username': username}, function(err, users) {
        if(err) throw err;
        else if(users.length !== 0) {
            registrationError.usernameError = true;
            base.redirect(res, '/register');
        }
        else database.User.find({'email': email}, function(err, users) {
            if(err) throw err;
            else if(users.length !== 0) {
                registrationError.emailError = true;
                base.redirect(res, '/register');
            }
            else {
                var nUser = new database.User({
                    'username': username,
                    'password': password,
                    'name': {
                        'first': firstname,
                        'last': lastname
                    },
                    'email': email,
                    'lastLogon': Date.now(),
                    'lockedUntil': 0
                });
                nUser.save(function(err, user, numAffected) {
                    if(err) throw err;
                    req.session.userId=user.username;
                    base.redirect(res, '/')
;                });
            }
        });
    });
};