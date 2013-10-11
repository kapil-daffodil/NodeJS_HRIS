/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 7/22/13
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */

function ApplaneCallback(error, success) {
    if (!error) {
        throw new Error("Error function is undefined in applane callback");
    }
    if (!success) {
        throw new Error("Success function is undefined in applane callback");
    }
    return function (err, data) {
        if (err) {
            error(err);
        } else {
            try {
                success(data);
            } catch (err) {
                error(err);
            }
        }
    }
}


module.exports = ApplaneCallback;

