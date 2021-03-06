/**
 * BBD Service Inc
 * All Rights Reserved @2016
 */
package com.jubi.service;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jubi.common.Constants;
import com.jubi.dao.TickerRateDao;
import com.jubi.dao.TickerRateExtDao;
import com.jubi.dao.entity.TickerRateEntity;
import com.jubi.dao.entity.TickerRateEntityExample;
import com.jubi.dao.vo.TickerRateSpanParam;
import com.jubi.service.vo.TickerPriceVo;
import com.jubi.service.vo.TickerRateVo;
import com.jubi.util.BeanMapperUtil;
import com.jubi.util.DateUtils;
import com.mybatis.domain.PageBounds;
import com.mybatis.domain.SortBy;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 涨幅服务
 *
 * @author tjwang
 * @version $Id: CoinRateService.java, v 0.1 2017/8/1 0001 15:20 tjwang Exp $
 */
@Service
public class TickerRateService {

    @Autowired
    private TickerRateDao tickerRateDao;

    @Autowired
    private TickerRateExtDao tickerRateExtDao;

    @Autowired
    private TickerService tickerService;

    @Autowired
    private CoinService coinService;

    @Cacheable(value = "ticker-rate", keyGenerator = "defaultKeyGenerator")
    public List<TickerRateVo> queryTickerRate(String coin, int span) {
        Preconditions.checkArgument(StringUtils.isNotBlank(coin), "币不能为空");

        List<TickerRateVo> result = Lists.newArrayList();

        Date now = new Date();
        int end = Long.valueOf(now.getTime() / 1000).intValue();
        if (span < 60) {
            span = 60; // 1分钟
        }

        TickerRateSpanParam param = new TickerRateSpanParam();
        param.setCoin(coin);
        param.setSpan(span);
        param.setEnd(end);

        List<TickerRateEntity> ds = null;
        if (span <= 3600) {
            PageBounds pb = new PageBounds(1, Constants.PAGE_COUNT_LIMIT, false);
            pb.setOrders(Arrays.asList(SortBy.create("pk", "desc")));
            ds = tickerRateExtDao.queryTickerRate(param, pb);
        } else {
            ds = tickerRateExtDao.queryHourTickerRate(param, Constants.PAGE_COUNT_LIMIT);
        }

        if (ds.size() == 0) {
            return result;
        }

        result = BeanMapperUtil.mapList(ds, TickerRateVo.class);
        return result;

    }

    @Cacheable(value = "ticker-rate-history", keyGenerator = "defaultKeyGenerator")
    public List<TickerRateVo> queryHistoryTickerRate(String coin, int year, int month, int day) {
        Preconditions.checkArgument(StringUtils.isNotBlank(coin));

        int span = 60 * 10; // 十分钟
        PageBounds pb = new PageBounds(1, 2000, false);
        SortBy sy = SortBy.create("pk", SortBy.Direction.DESC.toString());
        pb.setOrders(Arrays.asList(sy));

        Date date = new DateTime(year, month, day, 0, 0, 0).toDate();

        Integer start = DateUtils.getDayBeginTime(date);
        Integer end = start + Constants.DAY_LONG;

        return queryTickerRate(coin, span, start, end);
    }

    /**
     * 查询当前行情涨幅
     *
     * @param coin
     * @param size
     * @param beginTime
     * @return
     */
    @Cacheable(value = "ticker-rate", keyGenerator = "defaultKeyGenerator")
    public List<TickerRateVo> queryRecentTickerRate(String coin, int size, int beginTime) {
        Preconditions.checkArgument(StringUtils.isNotBlank(coin));
        Preconditions.checkArgument(size > 0);
        Preconditions.checkArgument(beginTime > 0);

        List<TickerRateVo> result = Lists.newArrayList();

        Optional<TickerPriceVo> tickerOpt = tickerService.queryLastTicker(coin);
        if (!tickerOpt.isPresent()) {
            return result;
        }

        int end = tickerOpt.get().getPk();
        if (end < beginTime) {
            return result;
        }

        int span = inferSpan(end - beginTime, size);

        result = queryTickerRate(coin, span, beginTime, end);

        return result;
    }

    /**
     * 获取涨幅排行
     *
     * @return
     */
    public List<TickerRateVo> queryRankedTickerRate() {
        Integer lastPk = queryLastPk();

        TickerRateEntityExample exam = new TickerRateEntityExample();
        exam.createCriteria().andPkEqualTo(lastPk);
        exam.setOrderByClause("rate desc");

        List<TickerRateEntity> ds = tickerRateDao.selectByExample(exam);

        Map<String, String> coinMap = coinService.getAllCoinsMap();
        List<TickerRateVo> result = BeanMapperUtil.mapList(ds, TickerRateVo.class);
        for (TickerRateVo vo : result) {
            vo.setName(coinMap.get(vo.getCoin()));
        }
        return result;
    }


    private List<TickerRateVo> queryTickerRate(String coin, int span, Integer start, Integer end) {
        if (span == 0) {
            span = 300; // 5分钟
        }

        List<TickerRateVo> result = Lists.newArrayList();

        PageBounds pb = new PageBounds(1, 2000, false);
        SortBy sy = SortBy.create("pk", SortBy.Direction.DESC.toString());
        pb.setOrders(Arrays.asList(sy));

        TickerRateSpanParam param = new TickerRateSpanParam();
        param.setCoin(coin);
        param.setSpan(span);
        param.setStart(start);
        param.setEnd(end);
        List<TickerRateEntity> ds = tickerRateExtDao.queryTickerRate(param, pb);
        if (ds.size() == 0) {
            return result;
        }

        result = BeanMapperUtil.mapList(ds, TickerRateVo.class);
        return result;
    }

    /**
     * 获取涨幅最大的10个币信息
     *
     * @return
     */
    public List<TickerRateVo> queryMaxPlusCoins() {
        Integer lastPk = queryLastPk();
        TickerRateEntityExample exam = new TickerRateEntityExample();
        exam.createCriteria().andPkEqualTo(lastPk);
        exam.setOrderByClause("rate desc");
        PageBounds pb = new PageBounds(0, 10, false);

        List<TickerRateEntity> ds = tickerRateDao.selectByExampleWithPageBounds(exam, pb);
        return BeanMapperUtil.mapList(ds, TickerRateVo.class);

    }

    /**
     * 获取跌幅最大的10个币信息
     *
     * @return
     */
    public List<TickerRateVo> queryMaxMinusCoins() {
        Integer lastPk = queryLastPk();
        TickerRateEntityExample exam = new TickerRateEntityExample();
        exam.createCriteria().andPkEqualTo(lastPk);
        exam.setOrderByClause("rate asc");
        PageBounds pb = new PageBounds(1, 10, false);

        List<TickerRateEntity> ds = tickerRateDao.selectByExampleWithPageBounds(exam, pb);
        return BeanMapperUtil.mapList(ds, TickerRateVo.class);
    }

    /**
     * 推断间隔
     *
     * @param timeSpan : 时间间隔
     * @param count    : 币种类数量
     * @return
     */
    private int inferSpan(int timeSpan, int count) {
        int[] cadidates = {60, 60 * 5, 60 * 10, 60 * 30, 60 * 60, 60 * 60 * 8, 60 * 60 * 24};
        // 单个币最大展示数量
        int maxCount = Constants.PAGE_COUNT_LIMIT / count;
        int rawSpan = timeSpan / maxCount;
        for (int c : cadidates) {
            if (rawSpan < c) {
                return c;
            }
        }
        return cadidates[cadidates.length - 1];
    }

    /**
     * 获取最近一次抓取时间
     *
     * @return
     */
    private Integer queryLastPk() {
        TickerRateEntityExample exam = new TickerRateEntityExample();
        exam.setOrderByClause("pk desc");
        PageBounds pb = new PageBounds(1, 1, false);
        List<TickerRateEntity> ds = tickerRateDao.selectByExampleWithPageBounds(exam, pb);
        if (ds.size() == 0) {
            return 0;
        }
        return ds.get(0).getPk();
    }
}
