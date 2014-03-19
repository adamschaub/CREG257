var mongoose = require('mongoose');
var bcrypt = require('bcrypt');

var SALT_WORK_FACTOR = 10; //sets up the salt for bcrypt.
var LOCK_TIME = 60*60*1000; //lock for an hour on repeated attempts


var lockSchema = new mongoose.Schema({
    device: {type: String, required: true},
    password: {type: String, required: true},
    owner: String,
    closed: Boolean,
    remoteOpen: Boolean,
    disabled: Boolean
});

lockSchema.pre('save', function(next) {
    //hash password
    var lock = this;
    if(!this.isModified('password')) return next();

    bcrypt.genSalt(SALT_WORK_FACTOR, function(err, salt) {
        if(err) return next(err);

        bcrypt.hash(lock.password, salt, function(err, hash) {
            if(err) return next(err);

            lock.password = hash;
            next();
        });
    });
});

/**Quick way to check hashed passwords. Example code
 *
 *  var sampleLock = new Lock({'device': 'phnky1', 'password': '1234'});
 *  Lock.findOne({'device': sampleLock.name}, function(err, lock) {
 *      if(err) throw err;
 *      lock.comparePassword(sampleLock.password, function(err, match) {
 *          if(err) throw err;
 *          if(match) //login successful
 *      });
 *  });
 */
lockSchema.methods.comparePassword = function(comp) {
    return bcrypt.compareSync(comp, this.password);
};

exports.Lock = mongoose.model('Lock', lockSchema, 'lock');