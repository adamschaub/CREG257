var base = require('../src/Base.js');

exports.post = function(req, res) {
    req.session.userId = null;
    base.redirect(res, '/');
};