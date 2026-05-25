package com.easychat.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis工具类
 */
@Component("redisUtils")
@Slf4j
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;  // 会自动注入RedisConfig中配置好的这个bean 进行底层的序列化处理 和 连接管理

    /**
     * 设置指定key的值
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置指定key的值并设置过期时间
     * @param key 键
     * @param value 值
     * @param timeout 时间(秒)
     */
    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取指定key的值
     * @param key 键
     * @return 值
     */
    public Object get(String key) {

        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取指定key的List值
     * @param key 键
     * @return List值
     */
    public List<String> getList(String key) {
        try {
            // 使用带泛型的 RedisTemplate 或手动转换
            List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
            if (rawList == null) {
                return null;
            }
            return rawList.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("WRONGTYPE")) {
                log.warn("Redis key '{}' has wrong type for LIST operation", key);
                return null;
            }
            throw e;
        }
    }


    /**
     * 删除指定key
     * @param key 键
     * @return true成功 false失败
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return true存在 false不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     * @param key 键
     * @param timeout 时间(秒)
     * @return true成功 false失败
     */
    public Boolean expire(String key, long timeout) {
        return redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取过期时间
     * @param key 键
     * @return 时间(秒) 返回0代表为永久有效
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }


    /**
     * 将多个值推入指定key的list头部
     * @param key 键
     * @param values 值列表
     * @param timeout 过期时间(秒)
     */
    // 修改 RedisUtils.java 中的 lpushAll 方法
    public void lpushAll(String key, List<String> values, long timeout) {
        if (values != null && !values.isEmpty()) {
            // 确保每个值都是字符串类型
            List<String> stringValues = values.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());

            redisTemplate.opsForList().leftPushAll(key, stringValues);
            if (timeout > 0) {
                redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 将一个值推入指定key的list头部
     * @param key 键
     * @param value 值
     * @param timeout 过期时间(秒)
     */
    public void lpush(String key, String value, long timeout) {
        redisTemplate.opsForList().leftPush(key, value);
        if (timeout > 0) {
            redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        }
    }

    /**
     * 从列表中移除指定值
     * @param key 列表的键
     * @param value 要移除的值
     */
    public void remove(String key, Object value) {
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        // 使用LTRIM和LREM组合操作来安全地删除元素
        listOps.remove(key, 1, value);
    }
}
