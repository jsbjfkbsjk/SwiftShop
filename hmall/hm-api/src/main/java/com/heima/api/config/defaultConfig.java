package com.heima.api.config;

import com.heima.api.fallback.ItemClientFallBack;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

public class defaultConfig {
    @Bean
    public Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor userInfoInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                Long id = UserContext.getUser();
                if(id!=null){
                    requestTemplate.header("user-info",id.toString());
                }
            }
        };
    }
    @Bean
    public ItemClientFallBack itemClientFallBack(){
        return new ItemClientFallBack();
    }




}
