package com.mirson.gemini.cache.core.listener;

import com.mirson.gemini.cache.common.CacheConfigProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 缓存发布/订阅传输消息对象
 *
 * @author zoutongkun
 */
@Data
public class CacheUpdateMessage implements Serializable {
    /**
     * 系统唯一标识
     * 用于区分是否为当前节点的本地缓存数据
     */
    private String systemId = CacheConfigProperties.SYSTEM_ID;

    /**
     * 缓存名称
     */
    private String[] cacheNames;

    /**
     * 缓存KEY键值
     */
    private Object key;

    public CacheUpdateMessage(String[] cacheName, Object key) {
        this.cacheNames = cacheName;
        this.key = key;
    }

    public CacheUpdateMessage(String cacheName, Object key) {
        this.cacheNames = new String[]{cacheName};
        this.key = key;
    }

}
