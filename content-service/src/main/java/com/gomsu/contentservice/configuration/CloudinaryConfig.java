package com.gomsu.contentservice.configuration;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration // <--- Dòng này cực kỳ quan trọng, báo cho Spring biết đây là file cấu hình
public class CloudinaryConfig {

    // Nó sẽ tự lấy thông tin từ file application.yml của bạn
    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean // <--- Dòng này tạo ra cái "Bean Cloudinary" mà file Service của bạn đang đòi!
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        return new Cloudinary(config);
    }
}
