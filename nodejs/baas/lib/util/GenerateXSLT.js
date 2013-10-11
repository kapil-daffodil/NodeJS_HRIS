/**
 * Created with IntelliJ IDEA.
 * User: daffodil
 * Date: 10/10/13
 * Time: 12:53 AM
 * To change this template use File | Settings | File Templates.
 */
var ejs = require('ejs');

exports.generateXSLT = function (options) {
    var data = options.data;
    var metadata = options.metadata;
    var orders = metadata.orders;
    var tableName = metadata.table;
    var templateName = treeRequired(orders) ? "renderastree__" : "renderastable__";
    var html = "";
    if (templateName != "renderastable__") {
        html += "<table width=\"100%\" style=\"min-height:100%;border-collapse:collapse\">";
        html += "<tbody>";
        html += "<tr style=\"min-height:3%;border-collapse:collapse\"><td width=\"1%\">&#160;</td><td bgcolor=\"#6699EE\" style=\"color:white;font-size:20px\"><b>Summary:</b></td><td width=\"1%\">&#160;</td></tr>";
        html += "<tr style=\"min-height:89%;border-collapse:collapse\">";
        html += "<td width=\"1%\">&#160;</td>";
        html += "<td width=\"100%\" bgcolor=\"#B4DAFF\" style=\"min-height:100%;border-top:1px solid black;border-bottom:1px solid black;border-collapse:collapse\">";
        html += "<div style=\"min-height:100%\">";
//        html += "<xsl:call-template name='" + templateName + "'> <xsl:with-param name='data' select='/root/" + tableName + "'></xsl:with-param><xsl:with-param name='showdetail'>true</xsl:with-param> 	</xsl:call-template>";
        html += "</div>";
        html += "</td></tr></tbody></table>";
        html += "<br/>";
    }

    html += "<table width=\"100%\" style=\"min-height:100%;border-collapse:collapse\">";
    html += "<tbody>";
    html += "<tr style=\"min-height:3%;border-collapse:collapse\"><td width=\"1%\">&#160;</td><td bgcolor=\"#6699EE\" style=\"color:white;font-size:20px\"><b>Details:</b></td><td width=\"1%\">&#160;</td></tr>";
    html += "<tr style=\"min-height:89%;border-collapse:collapse\">";
    html += "<td width=\"1%\">&#160;</td>";
    html += "<td width=\"100%\" bgcolor=\"#B4DAFF\" style=\"min-height:100%;border-top:1px solid black;border-bottom:1px solid black;border-collapse:collapse\">";
    html += "<div style=\"min-height:100%\">";
//    html += "<xsl:call-template name='" + templateName + "'> <xsl:with-param name='data' select='/root/" + tableName + "'></xsl:with-param><xsl:with-param name='showdetail'>true</xsl:with-param> 	</xsl:call-template>";
    html += "</div>";
    html += "</td></tr></tbody></table>";
    addTemplates("", html);
}

function addTemplate(alias, html) {

}

function addTemplateRenderAsTable(templatePostfix, html) {
    var templateName = "renderastable__" + templatePostfix;
    html += "<xsl:template name=\"" + templateName + "\">";
    html+= "<xsl:param name=\"data\"/>";
    html+= "<xsl:param name=\"showdetail\"/>";
    html.append("<table cellspacing=\"0\" border=\"1\" width=\"100%\" style=\"border-collapse:collapse;vertical-align:top;font-family:Times,serif;font-size:12px\">");
    html.append("<tbody>");
    html.append("<xsl:if test=\"count($data) != 0\">");
    html.append("<tr>");
}


function treeRequired(orders) {
    if (orders && orders instanceof Array && orders.length > 0) {
        for (var i = 0; i < orders.length; i++) {
            var order = orders[i];
            var exp = Object.keys(order)[0];
            if (order[exp].$recursive) {
                return true;
            }
        }
    }
}

this.generateXSLT({});

