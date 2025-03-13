package com.example.once;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.example.mapper.UserMapper;
import com.example.model.domain.User;
import com.example.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

@Component
@Slf4j
public class InsertUsers {

    @Resource
    private UserService userService;

//    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doInsertUsers() {

        final int batchSize = 50000; // 一组多少数量
        final int threadSize = 20;  // 分多少组(线程数)

        /**
         * 自定义配置的线程池
         */
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(40, 100, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<CompletableFuture> futureList = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < threadSize; i++) {
            List<User> userList = new ArrayList<>();
            while (true) {
                j++;
                // 生成随机四位
                Random random = new Random();
                int randomNum = random.nextInt(99999 - 10000) + 1000;
                log.info("randomNum: {}", randomNum);

                User user = new User();
                user.setUsername("假用户" + randomNum);
                user.setUserAccount("user" + randomNum);
                user.setAvatarUrl("https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg");
                user.setGender(0);
                user.setUserPassword("df70c4404b6954c7ea6e07f52d88c245");
                user.setPhone("188999" + randomNum);
                user.setEmail(randomNum + "@qq.com");
                user.setPlanetCode(randomNum + "");
                user.setTags("[]");
                user.setProfile("简介11111111111");

                userList.add(user);
                log.info("user: {}", user);
                if (j % batchSize == 0) {
                    break;
                }
            }

            /**
             * runAsync 用于异步执行不返回结果的任务，不会阻塞发起调用的主线程或当前线程。
             */
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("线程名称："+Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize); // batchSize 表示一组多少条条
            },threadPoolExecutor);
            futureList.add(future);

        }

        /**
         * allOf 这个方法本身是非阻塞的，不会等待所有异步任务实际完成，加上 join()方法后会阻塞当前线程，直到这些异步任务全部完成
         */
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();

        stopWatch.stop();
        System.out.println("耗时总毫秒：" + stopWatch.getTotalTimeMillis());
    }
}
