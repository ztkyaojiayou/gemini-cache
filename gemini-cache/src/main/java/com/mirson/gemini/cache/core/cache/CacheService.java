package com.mirson.gemini.cache.core.cache;

/**
 * 缓存服务接口
 * 分为一级/本地换存和二级缓存
 *
 * @author zoutongkun
 */
public interface CacheService {

    Object getFromCache(String cacheName, Object cacheKey);

    boolean save(String[] cacheNames, Object cacheKey, Object cacheValue, long ttl);

    boolean invalidateCache(String[] cacheNames, Object cacheKey);

    boolean invalidateCache(String[] cacheNames);

    boolean saveInRedisAsync(String[] cacheNames, Object cacheKey, Object cacheValue, long ttl);

    boolean invalidateCacheAsync(String[] cacheNames, Object cacheKey);

    boolean invalidateCacheAsync(String[] cacheNames);

}
