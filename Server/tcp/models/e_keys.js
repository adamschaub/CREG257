var mongoose = require('mongoose');

var intervalStates = 'none daily weekly'.split(' ');

/** intervalStates Enum
 *  none: event does not repeat
 *  daily: event repeats daily
 *  weekly: event repeats weekly
 */
exports.eKeySchema = new mongoose.Schema({
    lockOwner: {type: String, required: true},
    lockId: {type: String, required: true},
    contact: {type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true},
    start: Date,
    end: Date,
    nextActive: Date,
    nextDisable: Date,
    interval: {type: String, enum: intervalStates},
    repeat: {type: Number, min:0}
});

exports.eKey = mongoose.model('eKey', exports.eKeySchema, 'ekey');