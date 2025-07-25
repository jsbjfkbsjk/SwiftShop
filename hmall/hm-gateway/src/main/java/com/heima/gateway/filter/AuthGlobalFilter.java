package com.heima.gateway.filter;


import com.heima.gateway.config.AuthProperties;
import com.heima.gateway.util.JwtTool;
import com.hmall.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements Ordered, GlobalFilter {
    private final AuthProperties authProperties;
    private final JwtTool jwtTool;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if(isExclude(request.getPath().toString())){
            return chain.filter(exchange);
        }
        String token = null;
        List<String> authorization = request.getHeaders().get("authorization");
        if(authorization!=null&&authorization.size()>0){
            token = authorization.get(0);
        }
        Long id = null;
        try{
            id = jwtTool.parseToken(token);
        }catch(UnauthorizedException e){
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //加上用户信息
        String userId = id.toString();
        ServerWebExchange newExchange = exchange.mutate()
                .request(builder -> builder.header("user-info", userId))
                .build();


        return chain.filter(newExchange);
    }

    private boolean isExclude(String string) {
        List<String> excludePaths = authProperties.getExcludePaths();
        for(String str:excludePaths){
            if(antPathMatcher.match(str,string))return true;

        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
