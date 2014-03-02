//requires
var nodemailer = require('nodemailer');
var Lock = require('../app/models/lock.js').Lock;

module.exports = function(app, passport) {

    ///////////////
    //INDEX
    ///////////////
    app.get('/', function(req, res) {
    var data = {
        title: 'Home'
    };

    if(req.isAuthenticated()) {
        data.loggedIn = req.user.username;
    }

        res.render('index', data);
    });

    ///////////////
    //REGISTER
    ///////////////
    app.get('/register', function(req, res) {
        var data = {
            title: 'Register',
            message: req.flash('registerMessage')
        };
        if(req.isAuthenticated()) {
            data.loggedIn = req.user.username;
        }
        res.render('register', data);
    });

    app.get('/mobile-register', function(req, res) {
        var msg = req.query.status;
        if(!req.accepts('json')) {
            res.send(406, 'Error: must accept JSON');
        }

        var data = {};

        if(msg === 'success') {
            data.loggedIn = true;
            data.userId = req.user.id;
            data.username = req.user.username;
            res.send(data);
        }
        else {
            data.loggedIn = false;
            data.message = req.flash('registerMessage');
            res.send(data);
        }
    });

    app.post('/register', passport.authenticate('local-register', {
        successRedirect: '/',
        failureRedirect: '/register',
        failureFlash: true
    }));

    app.post('/mobile-register', passport.authenticate('local-register', {
        successRedirect: '/mobile-register?status=success',
        failureRedirect: '/mobile-register?status=failure',
        failureFlash: true
    }));

    ///////////////
    //LOGIN
    ///////////////
    app.get('/login', function(req, res) {
        var data = {
            title: 'Register',
            message: req.flash('loginMessage')
        };
        if(req.isAuthenticated()) {
            data.loggedIn = req.user.username;
        }
        res.render('login', data);
    });

    app.post('/login', passport.authenticate('local-login', {
        successRedirect: '/', 
        failureRedirect: '/login',
        failureFlash: true
    }));

    app.get('/mobile-login', function(req, res) {
        var msg = req.query.status;
        if(!req.accepts('json')) {
            res.send(406, 'Error: must accept JSON');
        }

        var data = {};

        if(msg === 'success') {
            data.loggedIn = true;
            data.userId = req.user.id;
            data.username = req.user.username;
            res.send(data);
        }
        else {
            data.loggedIn = false;
            data.message = req.flash('loginMessage');
            res.send(data);
        }
    });

    app.post('/mobile-login', passport.authenticate('local-login', {
        successRedirect: '/mobile-login?status=success',
        failureRedirect: '/mobile-login?status=failure',
        failureFlash: true
    }));

    ///////////////
    //CONTACTS
    ///////////////

    app.get('/contacts', isLoggedIn, function(req, res, next) {
        var data = {
            loggedIn: req.user.username
        };
        res.render('contacts', data);
    });

    app.post('/contacts', isLoggedIn, function(req, res, next) {
        var data = {};
        data.recipient = "phonekeymail@gmail.com";
        sendMail(req.user, data);
        res.redirect('/');
    });

    app.get('/update', isLoggedIn, function(req, res) {
        var msg = req.query.status;
        if(!req.accepts('json')) {
            res.send(406, 'Error: must accept JSON');
        }
        data = {};
        data.status = msg;
        res.send(data);
    });

    app.post('/update/:user', isLoggedIn, function(req, res) {
        var user = req.params.user;

        //ensure current user is calling update
        if(user !== req.user.username) {
            res.send(401, 'You are not authorized to update this account');
        }
        var info = req.body;
        req.user.phone.MAC = info.MAC || req.user.phone.MAC;
        req.user.phone.number = info.number || req.user.phone.number;
        req.user.name.first = info.firstname || req.user.name.first;
        req.user.name.last = info.lastname || req.user.name.last;
        req.user.save(function(err) {
            if(err) res.redirect('/update?status=failure');
            else res.redirect('/update?status=success');
        });
    });

    ///////////////
    //LOGOUT
    ///////////////
    app.get('/logout', function(req, res) {
        req.logout();
        req.session.lock = null;
        res.redirect('/');
    });

    ///////////////
    //E-KEYS
    ///////////////
    app.get('/e_keys', isLoggedIn, function(req, res) {
        var data = {
            title: 'eKeys',
            loggedIn: req.user.username,
        };

        if(req.session.lock) {
            data.device = req.session.lock.device;
        }
        
        res.render('e_keys', data);
    });

    app.post('/e_keys', isLoggedIn, function(req, res, next) {
        var deviceID = req.body.device;
        var password = req.body.password;
    
        Lock.findOne({device: deviceID}, function(err, lock) {
            if(err) return next(err);
            if(lock) {
                if(lock.comparePassword(password)) {
                    if(!lock.owner) {
                        lock.update({$set: {owner: req.user.username}}).exec();
                    }
                    else if(lock.owner === req.user.username) {
                        req.session.lock = lock;
                        res.redirect('/e_keys');
                    }
                    res.redirect('/');
                }
                else {
                    res.redirect('/e_keys', req.flash('deviceMessage', 'Invalid Device/Password'));
                }
            }
            else {
                res.redirect('/e_keys', req.flash('deviceMessage', 'Invalid Device/Password.'));
            }
        });
    });

    app.post('/add_key', isLoggedIn, function(req, res, next) {
        res.send(req.body);
    });

    ///////////////
    //404-NOT FOUND
    ///////////////
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


    ///////////////
    //500-ERROR
    ///////////////
    app.use(function(err, req, res, next) {
        console.error(err.stack);
        res.send(500, "We have a problem!");
    });

};

//middleware used for guarding pages
//e.g. app.get('/xyz', isLoggedIn, function(req, res) {...});
function isLoggedIn(req, res, next) {
    if(req.isAuthenticated()) 
        return next();
    else {
        res.redirect('/');
    }
}

var smsProviders = {
    att: "@txt.att.net",
    verizon: "@vtext.com",
    sprint: "@messaging.sprintpcs.com",
    tmobile: "@tmomail.net",
    virginmobile: "@vmobl.com"
};

function sendMail(user, data) {
    var smtpTransport = nodemailer.createTransport("SMTP", {
        service: "Gmail",
        auth: {
            user: "phonekeymail@gmail.com",
            pass: "iwwtbacs:GM"
        }
    });

    var opts = {
        from: "PhoneKey <phonekeymail@gmail.com>",
        to: data.recipient
    };

    opts.subject = user.name.first + ' has Shared an eKey!';
    opts.text = 'Come join us at PhoneKey!';
    opts.html = '<h1>Welcome to PhoneKey!</h1>'+'<a href="phone-key-website.herokuapp.com/register">Sign up now!</a>';

    smtpTransport.sendMail(opts, function(err, res) {
        if(err) {
            console.log(err);
            return false;
        }
        else {
            return res;
        }
    });
}