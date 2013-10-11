var Constants = require('../shared/Constants.js');

exports.getColumn = function (expression, table) {
    return getColumnObject(expression, table[Constants.Baas.Tables.SYSTEM_COLUMNS])
}


function getColumnObject(expression, columns, replicateColumns) {
    var index = expression.indexOf(".");
    if (index != -1) {
        var firstPart = expression.substr(0, index);
        var length = columns ? columns.length : 0;
        for (var i = 0; i < length; i++) {
            var column = columns[i];
            var columnExpression = column[Constants.Baas.Tables.Columns.EXPRESSION];
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnExpression == firstPart && (columnType == Constants.Baas.Tables.Columns.Type.LOOKUP || columnType == Constants.Baas.Tables.Columns.Type.OBJECT)) {
                var secondPart = expression.substr(index + 1);
                return getColumnObject(secondPart, column[Constants.Baas.Tables.COLUMNS], column[Constants.Baas.Tables.Columns.REPLICATE_COLUMNS]);
            }
        }
    } else {
        var length = columns ? columns.length : 0;
        for (var i = 0; i < length; i++) {
            var column = columns[i];
            var columnExpression = column[Constants.Baas.Tables.Columns.EXPRESSION];
            var columnType = column[Constants.Baas.Tables.Columns.TYPE];
            if (columnExpression == expression) {
                return column;
            }
        }
    }
    if (replicateColumns) {
        return getColumnObject(expression, replicateColumns);
    }
    return null;
}
