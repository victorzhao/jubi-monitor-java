$(function () {
    $("#okBtn").off("click").on("click", function () {
        fetchAndRender();
    });

    $("#maxPlusBtn").off("click").on("click", function () {
        var ctx = $("#ctx").val()
        var url = ctx + "/rate/max/plus?t=" + Math.random();
        $.get(url, function (json) {
            if (json.status == 200) {
                doFetchAndRender(json.data);
            } else {
                alert(json.message);
            }
        });
    });

    $("#maxMunisBtn").off("click").on("click", function () {
        var ctx = $("#ctx").val()
        var url = ctx + "/rate/max/minus?t=" + Math.random();
        $.get(url, function (json) {
            if (json.status == 200) {
                doFetchAndRender(json.data);
            } else {
                alert(json.message);
            }
        });
    })

});

function fetchAndRender() {
    var cs = $(".coinChk:checked")
    if (cs.length == 0) {
        return
    }
    var coins = []
    for (var i = 0; i < cs.length; i++) {
        coins.push($(cs[i]).val())
    }
    doFetchAndRender(coins);
}

function doFetchAndRender(coins) {
    var coinsParam = ""
    for (var i = 0; i < coins.length; i++) {
        coinsParam += "&coins=" + coins[i]
    }

    var ctx = $("#ctx").val()
    var url = ctx + "/rate/recent?t=" + Math.random() + coinsParam;

    $.getJSON(url, function (json) {
        if (json.status != '200') {
            alert(json.message)
            return
        }
        var arr = prepareData(json.data.reverse())
        render(arr)
    });
}

function prepareData(ds) {
    var r = {}
    var len = ds.length
    var xds = []
    for (var i = 0; i < len; i++) {
        var coin = ds[i].coin
        var d = r[coin]
        if (!d) {
            d = {
                xds: [],
                yds: []
            }
            r[coin] = d
        }
        var pk = ds[i].pk
        if (xds.length == 0 || pk > xds[xds.length - 1])
            xds.push(pk)
        d.yds.push(ds[i].rate)
    }
    var xdsStr = []
    for (var i = 0; i < xds.length; i++) {
        xdsStr.push(formatDateTimeSecsForX(xds[i]))
    }
    return [xdsStr, r]
}

/**
 *
 * @param xds
 * @param yds
 */
function render(data) {
    // 基于准备好的dom，初始化echarts图表
    var myChart = echarts.init(document.getElementById('main'), 'macarons');

    var xds = data[0]
    var seriesJson = data[1]
    var seriesArr = []
    var coins = []

    for (var coin in seriesJson) {
        var item = {
            name: coin,
            type: 'line',
            data: seriesJson[coin].yds
        }
        coins.push(coin)
        seriesArr.push(item)
    }

    var option = {
        title: {
            text: '涨幅情况'
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: coins
        },
        toolbox: {
            show: false,
        },
        calculable: true,
        xAxis: [
            {
                type: 'category',
                boundaryGap: true,
                data: xds
            }
        ],
        yAxis: [
            {
                type: 'value',
                scale: true,
                axisLabel: {
                    formatter: '{value} %'
                }
            }
        ],
        series: seriesArr
    };

    // 为echarts对象加载数据
    myChart.setOption(option);
}