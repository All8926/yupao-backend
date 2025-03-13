package com.example.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.model.domain.User;
import com.example.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热用户
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private List<Long> mainUserList =   Arrays.asList(6L);

    @Scheduled(cron = "0 21 22 * * *")
    public void doCacheReommendUser(){

        // 获取锁
        RLock lock = redissonClient.getLock("yupao:precachejob:docache:lock");

        try {
            /**
             * tryLock 是否有获取到锁
             * 0: 等待时长（如果锁正被其他客户端持有，你的线程将等待最多 0 秒钟尝试获取锁，因为这个任务是只执行一次，所以设置0不等待）
             * 30000：释放时间（锁自动释放前的持有时间）
             */
            if(lock.tryLock(0, -1, TimeUnit.MILLISECONDS)){
                System.out.println("getLock:"+Thread.currentThread().getId());
                Thread.sleep(30000);
                for (Long userId : mainUserList){
                    String redisKey = String.format("yupao:user:recommend:%s",userId);
                    ValueOperations valueOperations = redisTemplate.opsForValue();

                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userPage = userService.page(new Page<>(1,20),queryWrapper);
                    // 没有缓存则添加到 redis，设置过期时间为 30s
                    try {
                        valueOperations.set(redisKey,userPage,30000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("定时任务用户推荐缓存失败",e);
                    }
                }

            }
        } catch (InterruptedException e) {
            log.error("redisson error",e);
        }finally {
            // 只释放自己的锁
            if(lock.isHeldByCurrentThread()){
                System.out.println("unlock:"+Thread.currentThread().getId());
                lock.unlock();
            }
        }


    }
}
