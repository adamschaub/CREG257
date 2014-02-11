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

    database.User.findOne({'username': username}, function(err, user) {
        if(err) throw err;
        else if(user) registrationError.usernameError = true;
        else database.User.findOne({'email': email}, function(err, user) {
            if(err) throw err;
            else if(user) registrationError.emailError = true;
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
                    base.redirect(res, '/');
                });
            }
        });
    });
};