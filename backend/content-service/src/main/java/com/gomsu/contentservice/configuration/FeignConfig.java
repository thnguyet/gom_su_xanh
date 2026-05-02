package com.gomsu.contentservice.configuration;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                // Lấy Token từ Header "Authorization" của request hiện tại
                String authorizationHeader = attributes.getRequest().getHeader("Authorization");
                if (authorizationHeader != null) {
                    // Đính kèm nó vào request gửi sang Order Service
                    requestTemplate.header("Authorization", authorizationHeader);
                }
            }
        };
    }
}