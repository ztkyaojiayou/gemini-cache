package com.mirson.gemini.cache.core.cache;

/**
 * 缓存服务接口
 * 分为一级/本地换存和二级缓存
 *
 * @author zoutongkun
 */
public interface CacheService {
    /**
     * 从缓存中获取数据
     *
     * @param cacheName
     * @param cacheKey
     * @return
     */
    Object get(String cacheName, Object cacheKey);

    /**
     * 保存数据到缓存
     *
     * @param cacheNames
     * @param cacheKey
     * @param cacheValue
     * @param ttl
     * @return
     */
    boolean save(String[] cacheNames, Object cacheKey, Object cacheValue, long ttl);

    /**
     * 清理缓存
     *
     * @param cacheNames
     * @param cacheKey
     * @return
     */
    boolean delete(String[] cacheNames, Object cacheKey);

    /**
     * 清理缓存
     *
     * @param cacheNames
     * @return
     */
    boolean delete(String[] cacheNames);

    /**
     * 异步保存数据到缓存
     *
     * @param cacheNames
     * @param cacheKey
     * @param cacheValue
     * @param ttl
     * @return
     */
    boolean saveByAsync(String[] cacheNames, Object cacheKey, Object cacheValue, long ttl);

    /**
     * 异步清理缓存
     *
     * @param cacheNames
     * @param cacheKey
     * @return
     */
    boolean deleteByAsync(String[] cacheNames, Object cacheKey);

    /**
     * 异步清理缓存
     *
     * @param cacheNames
     * @return
     */
    boolean deleteByAsync(String[] cacheNames);

}
