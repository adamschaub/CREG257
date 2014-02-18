exports.get = function(req, res) {
    var data = {
        title: 'Home',
        loggedIn: req.session.userId
    };

    req.session.lastPage = '/';
    res.render('index', data);
}