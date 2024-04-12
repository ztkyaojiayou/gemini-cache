package com.mirson.gemini.cache.service.cache.first;

import com.mirson.gemini.cache.service.cache.CacheService;

/**
 * 一级缓存实现的抽象类
 *
 * @author mirson
 * @date 2021/9/26
 */
public abstract class AbstractFirstCacheService implements CacheService {

    protected CacheService cacheService;

    public AbstractFirstCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public Object getFromCache(String cacheName, Object cacheKey) {
        return cacheService.getFromCache(cacheName, cacheKey);
    }

    @Override
    public boolean save(String[] cacheNames, Object cacheKey, Object cacheValue, long ttl) {
        return cacheService.save(cacheNames, cacheKey, cacheValue, ttl);
    }

    @Override
    public boolean invalidateCache(String[] cacheNames, Object cacheKey) {
        return cacheService.invalidateCache(cacheNames, cacheKey);
    }

    @Override
    public boolean invalidateCache(String[] cacheNames) {
        return cacheService.invalidateCache(cacheNames);
    }

    @Override
    public boolean saveInRedisAsync(String[] cacheNames, Object cacheKey, Object cacheValue, long ttl) {
        return cacheService.saveInRedisAsync(cacheNames, cacheKey, cacheValue, ttl);
    }

    @Override
    public boolean invalidateCacheAsync(String[] cacheNames, Object cacheKey) {
        return cacheService.invalidateCacheAsync(cacheNames, cacheKey);
    }

    @Override
    public boolean invalidateCacheAsync(String[] cacheNames) {
        return cacheService.invalidateCacheAsync(cacheNames);
    }
}
