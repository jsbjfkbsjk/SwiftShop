package com.heima.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.trade.domain.dto.OrderFormDTO;
import com.heima.trade.domain.po.Order;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jsbjfkbsjk
 * @since 2023-05-05
 */
public interface IOrderService extends IService<Order> {

    Long createOrder(OrderFormDTO orderFormDTO);

    void markOrderPaySuccess(Long orderId);

    void cancelOrder(Long orderId,String msg_id);
}
