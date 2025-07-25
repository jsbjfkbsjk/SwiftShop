package com.heima.api.client;


import com.heima.api.config.defaultConfig;
import com.heima.api.dto.ItemDTO;
import com.heima.api.dto.OrderDetailDTO;
import com.heima.api.fallback.ItemClientFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(value = "item-service",
        configuration = defaultConfig.class,
        fallbackFactory = ItemClientFallBack.class

)
public interface ItemClient {
    @GetMapping("/items")
     List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);

    @PutMapping("/items/stock/deduct")
    void deductStock(@RequestBody List<OrderDetailDTO> items);

}
