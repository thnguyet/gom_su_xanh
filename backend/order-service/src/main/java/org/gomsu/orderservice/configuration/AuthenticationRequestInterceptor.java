package org.gomsu.orderservice.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthenticationRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 1. Lấy thông tin request hiện tại mà User gửi đến Order Service
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            // 2. Lấy cái Header "Authorization" (chứa Token Bearer ...)
            String authHeader = attributes.getRequest().getHeader("Authorization");

            // 3. Nếu có Token thì gắn nó vào Request của FeignClient trước khi gọi sang Product Service
            if (StringUtils.hasText(authHeader)) {
                template.header("Authorization", authHeader);
            }
        }
    }
}