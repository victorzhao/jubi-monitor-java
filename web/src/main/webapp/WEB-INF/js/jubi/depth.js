$(function () {
    var t = new Date().getTime();
    var s = formatDateTime4(new Date(t));
    $("#timeInput").val(s);

    $('#timeInput').off("keydown").on("keydown", function (e) {
        if (e.keyCode == 13) {
            queryCurrentDepth();
        }
    });

    queryCurrentDepth()

});

var queryCurrentDepth = function () {
    var time = $("#timeInput").val();
    var url = ctx + "/depth/query?time=" + time;
    $.get(url, function (json) {
        if (json.status == 200) {
            var ds = json.data;
            render(buildShowData(ds));
        } else {
            alert(json.message);
        }
    });
}