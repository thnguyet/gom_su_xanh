package com.gomsu.workshopservice.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod; // SỬA DÒNG NÀY
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // QUAN TRỌNG: Để dùng được @PreAuthorize
public class SecurityConfig {

    @Autowired
    private CustomJwtDecoder customJwtDecoder;

    @Value("${jwt.signerKey}")
    protected String signerKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request ->
                request.requestMatchers(HttpMethod.GET, "/workshops/all").permitAll() // Ví dụ: Khách xem sp thoải mái
                        .requestMatchers(HttpMethod.GET, "/workshops/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/workshops/s/**").permitAll()
                        .anyRequest().authenticated()); // Các thao tác khác phải đăng nhập

        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer ->
                        jwtConfigurer.decoder(customJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    // Hàm này cực kỳ quan trọng để biến "SCOPE_ADMIN" -> "ROLE_ADMIN"
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

}