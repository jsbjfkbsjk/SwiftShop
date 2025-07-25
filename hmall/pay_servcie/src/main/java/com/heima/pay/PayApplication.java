package com.heima.pay;

import com.heima.api.config.defaultConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.heima.pay.mapper")
@SpringBootApplication
@EnableFeignClients(basePackages = "com.heima.api.client",defaultConfiguration = defaultConfig.class)
public class PayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayApplication.class, args);
    }
}