package com.mirson.gemini.cache.core.aspect;

import com.mirson.gemini.cache.annotation.CacheDelete;
import com.mirson.gemini.cache.annotation.CacheUpdate;
import com.mirson.gemini.cache.annotation.CacheAdd;
import com.mirson.gemini.cache.common.CacheConfigProperties;
import com.mirson.gemini.cache.core.cache.CacheService;
import com.mirson.gemini.cache.utils.CacheUtil;
import com.mirson.gemini.cache.utils.KeyGenerators;
import com.mirson.gemini.cache.utils.SpElUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 二级缓存AOP切面
 * 整合二级缓存逻辑，非常简单！
 *
 * @author zoutongkun
 */
@Aspect
@Order(101)
@Component
@Slf4j
@ConditionalOnProperty(name = "app.cache.enable", havingValue = "true")
public class CacheManagerAspect {

    @Autowired
    SpElUtil spElUtil;

    @Autowired
    CacheService redisCacheService;

    @Autowired
    private CacheConfigProperties cacheConfigProperties;

    @Pointcut("execution(* com.mirson..*.*(..)) && @annotation(com.mirson.gemini.cache.annotation.CacheAdd)")
    public void executionOfCacheableMethod() {
    }

    @Pointcut("execution(* com.mirson..*.*(..)) && @annotation(com.mirson.gemini.cache.annotation.CacheUpdate)")
    public void executionOfCachePutMethod() {
    }

    @Pointcut("execution(* com.mirson..*.*(..))  && @annotation(com.mirson.gemini.cache.annotation.CacheDelete)")
    public void executionOfCacheDeleteMethod() {
    }

    @AfterReturning(pointcut = "executionOfCachePutMethod()", returning = "returnObject")
    public void putInCache(final JoinPoint joinPoint, final Object returnObject) {

        try {
            if (returnObject == null) {
                return;
            }

            if (!cacheConfigProperties.isEnableCache()) {
                return;
            }
            CacheUpdate cacheUpdateAnnotation = getAnnotation(joinPoint, CacheUpdate.class);
            Object cacheKey = spElUtil
                    .parseAndGetCacheKeyFromExpression(cacheUpdateAnnotation.keyExpression(), returnObject,
                            joinPoint.getArgs(), cacheUpdateAnnotation.keyGenerator());

            if (cacheUpdateAnnotation.isAsync()) {
                redisCacheService.saveInRedisAsync(cacheUpdateAnnotation.cacheNames(), cacheKey, returnObject, cacheUpdateAnnotation.TTL());
            } else {
                redisCacheService.save(cacheUpdateAnnotation.cacheNames(), cacheKey, returnObject, cacheUpdateAnnotation.TTL());
            }

        } catch (Exception e) {
            log.error("putInCache # Data save failed ## " + e.getMessage(), e);
        }
    }

    @AfterReturning(pointcut = "executionOfCacheDeleteMethod()", returning = "returnObject")
    public void deleteCache(final JoinPoint joinPoint, final Object returnObject) {

        try {
            if (!cacheConfigProperties.isEnableCache()) {
                return;
            }
            CacheDelete cacheDeleteAnnotation = getAnnotation(joinPoint, CacheDelete.class);

            String[] cacheNames = cacheDeleteAnnotation.cacheNames();
            Object cacheKey = null;
            if (!cacheDeleteAnnotation.removeAll()) {
                cacheKey = spElUtil
                        .parseAndGetCacheKeyFromExpression(cacheDeleteAnnotation.keyExpression(), returnObject,
                                joinPoint.getArgs(), cacheDeleteAnnotation.keyGenerator());
            }
            if (cacheDeleteAnnotation.isAsync()) {
                if (cacheDeleteAnnotation.removeAll())
                    redisCacheService.invalidateCache(cacheNames);
            } else {
                redisCacheService.invalidateCache(cacheNames, cacheKey);
            }

        } catch (Exception e) {
            log.error("putInCache # Data delete failed! ## " + e.getMessage(), e);
        }
    }

    @Around("executionOfCacheableMethod()")
    public Object getAndSaveInCache(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        if (!cacheConfigProperties.isEnableCache()) {
            return callActualMethod(proceedingJoinPoint);
        }

        Object returnObject = null;

        CacheAdd cacheAddAnnotation = null;
        Object cacheKey = null;
        try {
            cacheAddAnnotation = getAnnotation(proceedingJoinPoint, CacheAdd.class);

            KeyGenerators keyGenerator = cacheAddAnnotation.keyGenerator();

            if (StringUtils.isEmpty(cacheAddAnnotation.keyExpression())) {
                cacheKey = CacheUtil.buildCacheKey(proceedingJoinPoint.getArgs());
            } else {
                cacheKey = spElUtil
                        .parseAndGetCacheKeyFromExpression(cacheAddAnnotation.keyExpression(), null,
                                proceedingJoinPoint.getArgs(), keyGenerator);
            }
            //从缓存中获取数据
            returnObject = redisCacheService.getFromCache(cacheAddAnnotation.cacheName(), cacheKey);

        } catch (Exception e) {
            log.error("getAndSaveInCache # Redis op Exception while trying to get from cache ## " + e.getMessage(), e);
        }
        if (returnObject != null) {
            return returnObject;
        } else {
            returnObject = callActualMethod(proceedingJoinPoint);

            if (returnObject != null) {
                try {
                    assert cacheAddAnnotation != null;
                    if (cacheAddAnnotation.isAsync()) {
                        redisCacheService
                                .saveInRedisAsync(new String[]{cacheAddAnnotation.cacheName()}, cacheKey,
                                        returnObject, cacheAddAnnotation.TTL());
                    } else {
                        redisCacheService
                                .save(new String[]{cacheAddAnnotation.cacheName()}, cacheKey,
                                        returnObject, cacheAddAnnotation.TTL());
                    }
                } catch (Exception e) {
                    log.error("getAndSaveInCache # Exception occurred while trying to save data in redis##" + e.getMessage(),
                            e);
                }
            }
        }
        return returnObject;
    }

    /**
     * 执行目标方法，相当于是从数据库取
     *
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    private Object callActualMethod(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        return proceedingJoinPoint.proceed();

    }

    private <T extends Annotation> T getAnnotation(JoinPoint proceedingJoinPoint,
                                                   Class<T> annotationClass) throws NoSuchMethodException {

        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        if (method.getDeclaringClass().isInterface()) {
            method = proceedingJoinPoint.getTarget().getClass().getDeclaredMethod(methodName,
                    method.getParameterTypes());
        }
        return method.getAnnotation(annotationClass);
    }

}
