package com.jubi.controller;

import com.jubi.RestResult;
import com.jubi.service.TickerService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * Created by Administrator on 2017/7/30.
 */
@RestController
@RequestMapping("/ticker")
public class TickerController {

    @Autowired
    private TickerService tickerService;

    @RequestMapping("/{coin}")
    public RestResult queryTickers(@PathVariable("coin") String coin, Integer span) {
        return RestResult.ok(tickerService.queryTickers(coin, span));
    }

    @RequestMapping("/recent/{coin}")
    public RestResult queryRecentlyTickers(@PathVariable("coin") String coin) {
        return RestResult.ok(tickerService.queryRecentlyTickers(coin));
    }

    @RequestMapping("/history/{coin}")
    public RestResult queryHistoryTickers(@PathVariable("coin") String coin, @DateTimeFormat(pattern = "yyyy-MM-dd") Date time) {
        DateTime dateTime = new DateTime(time);
        int year = dateTime.getYear();
        int month = dateTime.getMonthOfYear();
        int day = dateTime.getDayOfMonth();
        return RestResult.ok(tickerService.queryHistoryTickers(coin, year, month, day));
    }

}
