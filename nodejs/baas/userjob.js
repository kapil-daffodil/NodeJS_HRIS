/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 9/7/13
 * Time: 11:46 AM
 * To change this template use File | Settings | File Templates.
 */


exports.beforeInsert = function () {
    console.log("User Job >>" + this.name);

    var self = this;
    setTimeout(function () {
        console.log("After time outUser Job >>" + self.name);
    }, 5000);
}