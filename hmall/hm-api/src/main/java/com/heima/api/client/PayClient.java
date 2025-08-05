package com.heima.api.client;


import com.heima.api.dto.PayOrderDTO;
import com.heima.api.fallback.PayClientFallback;
import com.heima.api.vo.PayOrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(value = "pay-service", fallbackFactory = PayClientFallback.class)
public interface PayClient {
    /**
     * 根据交易订单id查询支付单
     * @param id 业务订单id
     * @return 支付单信息
     */
    @GetMapping("/pay-orders/biz/{id}")
    PayOrderDTO queryPayOrderByBizOrderNo(@PathVariable("id") Long id);

    @GetMapping("/pay-orders/{time}")
    List<PayOrderVO> queryPayOrderByTime(@PathVariable("time") LocalDateTime time);
}