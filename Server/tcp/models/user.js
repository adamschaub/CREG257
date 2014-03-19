var mongoose = require('mongoose');
var bcrypt = require('bcrypt');

var SALT_WORK_FACTOR = 10; //sets up the salt for bcrypt.
var LOCK_TIME = 60*60*1000; //lock for an hour on repeated attempts

var userSchema = new mongoose.Schema({
    username: {type: String, required: true},
    email: {type: String, required: true},
    emailVerified: Boolean,
    phone: {
        MAC: String,
        number: String,
        provider: String,
        textCode: String,
        verified: Boolean
    },
    name: {
        first: String,
        last: String
    },
    password: {type: String, required: true},
    lastLogon: Date,
    lockedUntil: Date,
    update: Boolean,
    contacts: [{type: mongoose.Schema.Types.ObjectId, ref: 'User'}]
});

//hash all passwords using bcrypt before saves
userSchema.pre('save', function(next) {
    //hash password
    var user = this;
    if(!this.isModified('password')) return next();

    bcrypt.genSalt(SALT_WORK_FACTOR, function(err, salt) {
        if(err) return next(err);

        bcrypt.hash(user.password, salt, function(err, hash) {
            if(err) return next(err);

            user.password = hash;
            next();
        });
    });
});

userSchema.methods.comparePassword = function(comp) {
    return bcrypt.compareSync(comp, this.password);
};

//Not implemented, for further account security
userSchema.virtual('isLocked').get(function() {
    return !!(this.lockedUntil && this.lockedUntil > Date.now());
});

userSchema.statics.failedLogin = {
    NOT_FOUND: 0,
    INCORRECT_PASSWORD: 1,
    MAX_ATTEMPTS: 2
};

exports.User = mongoose.model('User', userSchema, 'user');