var ejs = require('ejs');


function renderTemplate(template, data) {
    console.log("now in renderTemplate" + template, " Data " + JSON.stringify(data));
    return ejs.render(template, {data:data});
}

var data = [
    {task:"My first task"},
    {task:"My second task"}
];
var templates = {taskstable:"<table> <tr><td>Task</td></tr> <% for(var i=0; i<data.length; i++) {%><tr><td><%= data[i].task %></td></tr><% } %> </table>"};
console.log(ejs.render("<html><%-renderTemplate(templates.taskstable,data)%></html>", {data:data, templates:templates, renderTemplate:renderTemplate}));