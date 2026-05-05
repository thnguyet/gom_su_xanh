package org.gomsu.productservice.configuration;

import org.gomsu.productservice.dto.request.IntrospectRequest;
import org.gomsu.productservice.repository.httpclient.AuthenticationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signerKey}")
    private String signerKey;

    @Autowired
    private AuthenticationClient authenticationClient;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // Gọi trực tiếp, không cần .getResult() vì không có ApiResponse
            var response = authenticationClient.introspect(IntrospectRequest.builder()
                    .token(token)
                    .build());

            if (!response.isValid()) {
                throw new JwtException("Token không hợp lệ hoặc đã đăng xuất!");
            }
        } catch (Exception e) {
            // Lỗi này xảy ra khi Identity Service chết hoặc Token sai định dạng
            throw new JwtException("Lỗi xác thực với Identity Service: " + e.getMessage());
        }

        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS256");
            nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }

        return nimbusJwtDecoder.decode(token);
    }
}