package com.heima.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix="hm.cart")
public class CartProperties {
    private Integer maxNumber;

}
