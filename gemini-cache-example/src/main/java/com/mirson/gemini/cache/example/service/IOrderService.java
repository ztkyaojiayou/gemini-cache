package com.mirson.gemini.cache.example.service; /**
 * 
 *
 * @author mirson
 * @date 2021/10/3
 */
public interface IOrderService {

    /**
     * 获取订单详情
     * @param orderNo
     * @return
     */
    String getOrder(String orderNo);

    /**
     * 更新订单
     * @param orderNo
     * @return
     */
    String updateOrder(String orderNo);
}
