package com.mirson.gemini.cache.core.listener;

import com.mirson.gemini.cache.common.CacheConfigProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 缓存发布/订阅传输消息对象
 * @author zoutongkun
 */
@Data
public class CacheMessage implements Serializable {
    /**
     * 系统唯一标识
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

	public CacheMessage() {
    }

    public CacheMessage(String[] cacheName, Object key) {
	    this.cacheNames = cacheName;
	    this.key = key;
    }

    public CacheMessage(String cacheName, Object key) {
        this.cacheNames = new String[]{cacheName};
        this.key = key;
    }

}
