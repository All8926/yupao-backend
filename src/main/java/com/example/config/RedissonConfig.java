package com.example.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson 配置
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private String host;
    private String port;

    @Bean
    public RedissonClient redissonClient(){
        // 1.创建配置，useSingleServer 单一模式，集群的话可选集群模式
        String redissonAddress = String.format("redis://%s:%s",host,port);
        Config config = new Config();
        config.useSingleServer().setAddress(redissonAddress).setDatabase(2);

        // 2. 创建实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
