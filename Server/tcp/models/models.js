var mongoose = require('mongoose');
var bcrypt = require('bcrypt');

var SALT_WORK_FACTOR = 10; //sets up the salt for bcrypt.
var LOCK_TIME = 60*60*1000; //lock for an hour on repeated attempts

exports.contactSchema = new mongoose.Schema({
    name: {
        first: String,
        last: String
    },
    email: String
});

exports.Contact = mongoose.model('Contact', exports.contactSchema, 'contact');
