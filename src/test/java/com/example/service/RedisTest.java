package com.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    public void test(){
        // 操作 String 类型
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("key3","value1");
        Object value = valueOperations.get("key1");
    }
}
