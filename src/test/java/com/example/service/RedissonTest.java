package com.example.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBatch;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void test(){
        RList<Object> rlist = redissonClient.getList("test-list");
//        rlist.add("rlist-111111");
        rlist.remove(0);
    }
}
