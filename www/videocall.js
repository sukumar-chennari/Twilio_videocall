var exec = require('cordova/exec');

exports.coolMethod = function (arg0,arg1,arg2,arg3, success, error) {
    exec(success, error, 'videocall', 'coolMethod', [arg0,arg1,arg2,arg3]);
};
// exports.add = function (param1,param2, success, error) {
//     exec(success, error, 'videocall', 'add', [param1,param2]);
// };


