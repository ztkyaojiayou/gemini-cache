package com.mirson.gemini.cache.core.notify;

import com.mirson.gemini.cache.common.CacheConfigProperties;
import com.mirson.gemini.cache.core.listener.CacheUpdateMessage;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis发送服务实现接口
 *
 * @author mirson
 * @date 2021/9/26
 */
public class NotifyByRedisImpl implements NotifyService {

    private static final Logger logger = LoggerFactory.getLogger(NotifyByRedisImpl.class);

    /**
     * 缓存配置属性
     */
    private CacheConfigProperties cacheConfigProperties;

    /**
     * Redis操作接口
     */
    private RedissonClient redissonClient;

    public NotifyByRedisImpl(CacheConfigProperties cacheConfigProperties,
                             RedissonClient redissonClient) {
        this.cacheConfigProperties = cacheConfigProperties;
        this.redissonClient = redissonClient;
    }

    /**
     * 发送缓存变更消息
     *
     * @param cacheNames
     */
    @Override
    public void sendMessage(String[] cacheNames) {
        sendMessage(cacheNames, null);
    }

    /**
     * 发送缓存变更消息
     *
     * @param cacheName
     */
    @Override
    public void sendMessage(String cacheName) {
        sendMessage(new String[]{cacheName}, null);
    }

    /**
     * 发送缓存变更消息
     *
     * @param cacheName
     * @param key
     */
    @Override
    public void sendMessage(String cacheName, Object key) {
        sendMessage(new String[]{cacheName}, key);
    }

    /**
     * 发送缓存变更消息
     *
     * @param cacheNames
     * @param key
     */
    @Override
    public void sendMessage(String[] cacheNames, Object key) {
        RTopic rTopic = redissonClient.getTopic(cacheConfigProperties.getTopic());
        long receive = rTopic.publish(new CacheUpdateMessage(cacheNames, key));
        logger.info("sendMessage receive clients: " + receive);
    }

}
