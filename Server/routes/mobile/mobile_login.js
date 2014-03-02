exports.get = function(req, res, next) {

}

exports.post = function(req, res) {
    var username = req.body.username;
    var password = req.body.password;


    res.json({
        "auth_token": //generate auth_token
    });
}

exports.genMobileAppToken = function(username) {
    //use username and logon time
}