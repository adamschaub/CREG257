(function(module) {

    module.VERBOSE = 2;
    module.INFO = 3;
    module.WARN = 4;
    module.ERROR = 5;

    module.level = module.WARN;

    module.v = function(msg) {
        message(msg, module.VERBOSE);
    }
    module.i = function(msg) {
        message(msg, module.INFO);
    }
    module.w = function(msg) {
        message(msg, module.WARN);
    }
    module.e = function(msg) {
        message(msg, module.ERROR);
    }
    
    function message(msg, level) {
        if(level >= module.level) {
            switch(level) {
                case module.VERBOSE:
                    console.log(msg);
                    break;
                case module.INFO: 
                    console.info(msg);
                    break;
                case module.WARN:
                    console.warn(msg);
                    break;
                case module.ERROR:
                    console.error(msg);
                    break;
                default:
                    console.log(msg);
            }
        }
    }

})(exports);