var mongoose = require('mongoose');
var bcrypt = require('bcrypt');

var SALT_WORK_FACTOR = 10; //sets up the salt for bcrypt.
var LOCK_TIME = 60*60*1000; //lock for an hour on repeated attempts

exports.userSchema = new mongoose.Schema({
    username: {type: String, required: true},
    email: {type: String, required: true},
    emailVerified: Boolean,
    phone: {
        MAC: String,
        number: String
    },
    name: {
        first: String,
        last: String
    },
    password: {type: String, required: true},
    salt: String,
    lastLogon: Date,
    lockedUntil: Date
});

//hash all passwords using bcrypt before uploads
exports.userSchema.pre('save', function(next) {
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

/**Quick way to check hashed passwords. 
 *
 *  var sampleUser = new User({'username': 'me', 'password': '1234'});
 *  User.findOne({'username': user.name}, function(err, user) {
 *      if(err) throw err;
 *      user.comparePassword(sampleUser.password, function(err, match) {
 *          if(err) throw err;
 *          if(match) //login successful
 *      });
 *  });
 */
exports.userSchema.methods.comparePassword = function(comp, callback) {
    bcrypt.compare(comp, this.password, function(err, match) {
        if(err) callback(err);
        callback(null, match);
    });
};

exports.userSchema.virtual('isLocked').get(function() {
    return !!(this.lockedUntil && this.lockedUntil > Date.now());
});

exports.userSchema.statics.failedLogin = {
    NOT_FOUND: 0,
    INCORRECT_PASSWORD: 1,
    MAX_ATTEMPTS: 2
};

exports.User = mongoose.model('User', exports.userSchema, 'user');

exports.lockSchema = new mongoose.Schema({
    device: {type: mongoose.Schema.ObjectId, ref:'Lock'},
    owner: {type: mongoose.Schema.ObjectId, ref:'User'},
    closed: Boolean,
    remoteOpen: Boolean,
    disabled: Boolean
});

exports.Lock = mongoose.model('Lock', exports.lockSchema, 'lock');

exports.eKeySchema = new mongoose.Schema({
    lock: {type: mongoose.Schema.ObjectId, ref:'Lock'},
    contact: {type: mongoose.Schema.ObjectId, ref:'Contact'},
    time: [{
        start: Date,
        end: Date
    }]
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

exports.connection = null;
exports.connect = function() {
    var MONGO_URI = process.env.MONGO_URI || process.env.MONGO_URL;
    if(this.connection) return this.connection;
    else {
        this.connection = mongoose.connect(MONGO_URI).connection;
        this.connection.on('open', function(err) {
            //TODO: handle error
            return this;
        });
    }
};