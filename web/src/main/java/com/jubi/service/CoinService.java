package com.jubi.service;

import com.jubi.dao.CoinDao;
import com.jubi.dao.entity.CoinEntity;
import com.jubi.service.vo.CoinVo;
import com.jubi.util.BeanMapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Administrator on 2017/7/30.
 */
@Service
public class CoinService {

    @Autowired
    private CoinDao coinDao;

    @Cacheable(value = "coin", keyGenerator = "defaultKeyGenerator")
    public List<CoinVo> getAllCoins() {
        List<CoinEntity> ds = coinDao.queryAll();
        return BeanMapperUtil.mapList(ds, CoinVo.class);
    }

}
