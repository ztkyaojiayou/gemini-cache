package com.mirson.gemini.cache.core.cache.first;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mirson.gemini.cache.common.CacheConfigProperties;
import com.mirson.gemini.cache.core.cache.CacheService;
import com.mirson.gemini.cache.core.notify.NotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 一级/本地缓存实现，这里使用的是Caffeine
 *
 * @author mirson
 * @date 2021/9/26
 */
public class CaffeineCacheServiceImpl extends AbstractFirstCacheService {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineCacheServiceImpl.class);

    /**
     * Caffeine内部缓存
     */
    private ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    /**
     * Redis 发送服务接口
     * 用于保持缓存的一致性
     */
    private NotifyService notifyService;

    /**
     * 缓存配置参数
     */
    private CacheConfigProperties cacheConfigProperties;

    public CaffeineCacheServiceImpl(CacheService cacheService,
                                    NotifyService notifyService,
                                    CacheConfigProperties cacheConfigProperties) {
        super(cacheService);
        this.notifyService = notifyService;
        this.cacheConfigProperties = cacheConfigProperties;
    }

    /**
     * 清理缓存（支持批量清理）
     *
     * @param cacheNames
     */
    private void clearAndSend(String[] cacheNames) {
        for (String cacheName : cacheNames) {
            clearAndSend(cacheName, null, false);
        }
        // 发送Redis缓存更新消息
        notifyService.sendMessage(cacheNames, null);
    }

    /**
     * 清理缓存（支持批量清理）
     *
     * @param cacheNames
     * @param key
     */
    private void clearAndSend(String[] cacheNames, Object key) {
        for (String cacheName : cacheNames) {
            clearAndSend(cacheName, key, false);
        }
        // 发送Redis缓存更新消息
        notifyService.sendMessage(cacheNames, key);
    }


    /**
     * 保存并且发送缓存（支持批量清理）
     *
     * @param cacheNames
     * @param key
     */
    private void saveAndSend(String[] cacheNames, Object key, Object cacheValue) {
        for (String cacheName : cacheNames) {
            saveAndSend(cacheName, key, cacheValue, false);
        }
        // 发送Redis缓存更新消息, 所有cacheNames统一发送
        notifyService.sendMessage(cacheNames, key);
    }


    /**
     * 清理缓存（支持批量清理）
     *
     * @param cacheNames
     * @param key
     */
    public void clearNotSend(String[] cacheNames, Object key) {
        for (String cacheName : cacheNames) {
            clearAndSend(cacheName, key, false);
        }
    }

    /**
     * 保存本地缓存
     *
     * @param cacheName
     * @param key
     */
    private void saveAndSend(String cacheName, Object key, Object value, boolean isNeedSend) {
        // 获取缓存对象
        Cache caffeineCache = cacheMap.get(cacheName);
        if (caffeineCache == null) {
            caffeineCache = caffeineCache();
            cacheMap.putIfAbsent(cacheName, caffeineCache);
        }
        caffeineCache.put(key, value);

        if (isNeedSend) {
            // 发送Redis缓存更新消息
            notifyService.sendMessage(cacheName, key);
        }
    }


    /**
     * 初始化caffeine缓存对象
     *
     * @return
     */
    public Cache<Object, Object> caffeineCache() {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
        // Caffeine 缓存初始化参数配置
        if (cacheConfigProperties.getExpireAfterAccess() > 0) {
            cacheBuilder.expireAfterAccess(cacheConfigProperties.getExpireAfterAccess(), TimeUnit.MILLISECONDS);
        }
        if (cacheConfigProperties.getExpireAfterWrite() > 0) {
            cacheBuilder.expireAfterWrite(cacheConfigProperties.getExpireAfterWrite(), TimeUnit.MILLISECONDS);
        }
        if (cacheConfigProperties.getInitialCapacity() > 0) {
            cacheBuilder.initialCapacity(cacheConfigProperties.getInitialCapacity());
        }
        if (cacheConfigProperties.getMaximumSize() > 0) {
            cacheBuilder.maximumSize(cacheConfigProperties.getMaximumSize());
        }
        if (cacheConfigProperties.getRefreshAfterWrite() > 0) {
            cacheBuilder.refreshAfterWrite(cacheConfigProperties.getRefreshAfterWrite(), TimeUnit.MILLISECONDS);
        }
        return cacheBuilder.build();
    }

    /**
     * 清除本地缓存
     *
     * @param cacheName
     * @param key
     */
    private void clearAndSend(String cacheName, Object key, boolean isNeedSend) {
        // 获取缓存对象
        Cache caffeineCache = cacheMap.get(cacheName);
        if (caffeineCache == null) {
            return;
        }

        if (key == null) {
            // key键值为空， 则清空该缓存下面的所有条目
            caffeineCache.invalidateAll();
        } else {
            // 清除指定键值的缓存
            caffeineCache.invalidate(key);
        }

        if (isNeedSend) {
            // 发送Redis缓存更新消息
            notifyService.sendMessage(cacheName, key);
        }
    }

    /**
     * 获取缓存对象
     *
     * @param cacheName
     * @param cacheKey
     * @return
     */
    @Override
    public Object getFromCache(final String cacheName, final Object cacheKey) {
        Object result = null;
        Cache caffeineCache = cacheMap.get(cacheName);
        if (null != caffeineCache) {
            // 1.先从本地缓存获取
            result = caffeineCache.getIfPresent(cacheKey);
        }

        if (null == result) {
            // 2.从Redis缓存获取
            result = cacheService.getFromCache(cacheName, cacheKey);
            logger.info("getFromCache # fetch data from redis cache.");
            // 3.再保存更新Caffeine缓存
            saveCaffeineCache(cacheName, cacheKey, result, caffeineCache);
        }

        return result;
    }

    /**
     * 保存更新Caffeine缓存
     *
     * @param cacheName
     * @param cacheKey
     * @param result
     * @param caffeineCache
     */
    private void saveCaffeineCache(String cacheName, Object cacheKey, Object result, Cache caffeineCache) {
        if (null != result) {
            // 获取缓存对象
            if (caffeineCache == null) {
                caffeineCache = caffeineCache();
                cacheMap.putIfAbsent(cacheName, caffeineCache);
            }
            caffeineCache.put(cacheKey, result);
        }
    }

    @Override
    public boolean save(String[] cacheNames, Object cacheKey, Object cacheValue, long ttl) {
        boolean result = super.save(cacheNames, cacheKey, cacheValue, ttl);
        // 保存并广播更新二级缓存
        saveAndSend(cacheNames, cacheKey, cacheValue);
        return result;
    }

    @Override
    public boolean saveInRedisAsync(String[] cacheNames, Object cacheKey, Object cacheValue, long ttl) {
        boolean result = super.saveInRedisAsync(cacheNames, cacheKey, cacheValue, ttl);
        // 保存并广播更新二级缓存
        saveAndSend(cacheNames, cacheKey, cacheValue);
        return result;
    }

    @Override
    public boolean invalidateCache(String[] cacheNames, Object cacheKey) {
        boolean result = super.invalidateCache(cacheNames, cacheKey);
        clearAndSend(cacheNames, cacheKey);
        return result;
    }

    @Override
    public boolean invalidateCache(String[] cacheNames) {
        boolean result = super.invalidateCache(cacheNames);
        clearAndSend(cacheNames);
        return result;
    }

}
