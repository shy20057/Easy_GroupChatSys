package com.easychat.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@Slf4j
public class RedisConfig<V> {

    @Value("${spring.redis.host:}")
    private String redisHost;

    @Value("${spring.redis.port:}")
    private Integer redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    // 创建并配置Redisson客户端实例
    @Bean(name="redissonClient",destroyMethod = "shutdown")  // 销毁方法：shutdown 确保正确释放资源
    public RedissonClient redissonClient() {

        try {
            Config config = new Config();
            config.useSingleServer()
                  .setAddress("redis://"+redisHost+":"+redisPort);

            // 如果配置了密码，则设置密码
            if (redisPassword != null && !redisPassword.isEmpty()) {
                config.useSingleServer().setPassword(redisPassword);
            }
            RedissonClient redissonClient = Redisson.create(config);



            return redissonClient;

        } catch (Exception e) {
           log.error("redis配置异常，请检查redis配置",e);
        }
        return null;
    }


    @Bean("redisTemplate")    // 创建Redis连接工厂
    public RedisTemplate<String, V> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, V> template = new RedisTemplate<>(); // 创建RedisTemplate对象 redis模板对象 简化Redis操作 封装 序列化 连接管理
        template.setConnectionFactory(factory); // 设置Redis连接工厂 自动管理Redis连接的获取与释放
        // 设置key的序列化方式 将键序列化为字符串格式
        template.setKeySerializer(RedisSerializer.string());
        // 设置value的序列化方式 将值序列化为JSON格式
        template.setValueSerializer(RedisSerializer.json());
        // 设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        // 设置hash的value序列化方式
        template.setHashValueSerializer(RedisSerializer.json());
        template.afterPropertiesSet();
        return template;
    }
}