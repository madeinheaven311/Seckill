package com.xxxx.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxxx.seckill.mapper.OrderMapper;
import com.xxxx.seckill.mapper.SeckillOrderMapper;
import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.service.ISeckillOrderService;
import com.xxxx.seckill.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeckillOrderService extends ServiceImpl<SeckillOrderMapper, SeckillOrder> implements ISeckillOrderService {

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * orderId: 成功  -1 秒杀失败，0 排队中
     *
     * */
    //判断用户当前订单是秒杀成功，排队中，还是失败
    @Override
    public Long getResult(User user, Long goodsId) {
        //select * from seckill where userid= xxx and goods_id = xxx
        SeckillOrder seckillOrder = seckillOrderMapper.selectOne(new QueryWrapper<SeckillOrder>().
                eq("user_id", user.getId()).eq("goods_id", goodsId));
        if (null != seckillOrder) {
            return seckillOrder.getOrderId();
            //MQ异步处理请求时，这些请求先判断此订单是否生成，若为生成则生成订单，若订单生成则返回订单id，
            // 若订单为生产则看是否有库存，若无库存则说明此次请求失败，否则有库存又为生成订单的情况即该请求在等待中。
            // 首先，不能以redis中的库存作为依据，因为可以当前还在mq中等待，如果成功，则上个判断进，
            // 此时， 如果出现这个key，并且没有订单，没买到，并且没库存
            // 则是失败
        } else if (redisTemplate.hasKey("isStockEmpty"+goodsId)) {
            return -1L;
        } else {
            return 0L;
        }
    }
}
