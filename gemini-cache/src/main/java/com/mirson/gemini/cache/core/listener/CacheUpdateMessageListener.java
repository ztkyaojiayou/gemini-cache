package com.mirson.gemini.cache.core.listener;

import com.mirson.gemini.cache.common.CacheConfigProperties;
import com.mirson.gemini.cache.core.cache.TwoLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.listener.MessageListener;

/**
 * 缓存消息发布/订阅监听器
 * 使用的是redisson封装的监听机制
 * 主要就是用于删除本地缓存
 *
 * @author zoutongkun
 */
@Slf4j
public class CacheUpdateMessageListener implements MessageListener<CacheUpdateMessage> {

    /**
     * Caffeine 缓存管理实现接口
     */
    private TwoLevelCacheService twoLevelCacheService;

    public CacheUpdateMessageListener(TwoLevelCacheService twoLevelCacheService) {
        this.twoLevelCacheService = twoLevelCacheService;
    }

    /**
     * 监听Redis订阅缓存变化消息
     * 用于删除本地缓存，保持两级缓存的一致性
     * 注意：
     * 1.当读时，先读缓存，若没有则读数据库，再把数据写到redis，
     * 当写到redis后，对于本地缓存的处理逻辑为：
     * 1.1当前节点的本地缓存会更新
     * 1.2但其他节点的本地缓存是直接清除，而不是更新，
     * 因为更新没有意义，只有访问了其他节点时才考虑更新即可！
     * 2.当更新或删除时，先删redis数据，再删除所有节点的本地缓存数据
     *
     * @param channel
     * @param cacheUpdateMessage
     */
    @Override
    public void onMessage(String channel, CacheUpdateMessage cacheUpdateMessage) {
        log.info("onMessage # receive a redis message, channel: " + channel);
        try {
            // 如果是当前节点，则不做清除（这里主要是兼容redis更新的场景，而对于删除操作，则当前节点的本地缓存也需要删除！）
            if (!CacheConfigProperties.SYSTEM_ID.equals(cacheUpdateMessage.getSystemId())) {
                // 发送清理本地缓存的信息
                twoLevelCacheService.clearNotSend(cacheUpdateMessage.getCacheNames(), cacheUpdateMessage.getKey());
                log.info("onMessage # clear local cache {}, the key is {}",
                        cacheUpdateMessage.getCacheNames(), cacheUpdateMessage.getKey());
            }
        } catch (Exception e) {
            log.error("onMessage error: # " + e.getMessage(), e);
        }
    }

}
