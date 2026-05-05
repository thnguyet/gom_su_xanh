package com.gomsu.workshopservice.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    // Lấy Header Authorization (Token) từ request hiện tại của User
                    String authorizationHeader = attributes.getRequest().getHeader("Authorization");

                    if (authorizationHeader != null) {
                        // Đính kèm nó vào request gọi sang Identity Service
                        template.header("Authorization", authorizationHeader);
                    }
                }
            }
        };
    }
}