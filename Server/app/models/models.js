var mongoose = require('mongoose');
var bcrypt = require('bcrypt');

var SALT_WORK_FACTOR = 10; //sets up the salt for bcrypt.
var LOCK_TIME = 60*60*1000; //lock for an hour on repeated attempts

/** intervalStates Enum
 *  none: event does not repeat
 *  daily: event repeats daily
 *  weekly: event repeats weekly
 */
var intervalStates = 'none daily weekly'.split(' ');

exports.eKeySchema = new mongoose.Schema({
    lockOwner: {type: String, required: true},
    lockId: {type: String, required: true},
    contact: {type: mongoose.Schema.ObjectId, ref:'Contact'},
    start: Date,
    end: Date,
    interval: {type: String, enum: intervalStates},
    repeat: {type: Number, min:0}, 
    endTime: {type: Date, default: 0}
});

exports.eKey = mongoose.model('eKey', exports.eKeySchema, 'ekey');

exports.contactSchema = new mongoose.Schema({
    name: {
        first: String,
        last: String
    },
    phone: {
        MAC: String,
        number: String
    }
});

exports.Contact = mongoose.model('Contact', exports.contactSchema, 'contact');
