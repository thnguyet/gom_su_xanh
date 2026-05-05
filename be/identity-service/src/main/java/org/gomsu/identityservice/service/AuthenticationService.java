package org.gomsu.identityservice.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gomsu.identityservice.dto.request.AuthenticationRequest;
import org.gomsu.identityservice.dto.request.IntrospectRequest;
import org.gomsu.identityservice.dto.request.LogoutRequest;
import org.gomsu.identityservice.dto.response.AuthenticationResponse;
import org.gomsu.identityservice.dto.response.IntrospectResponse;
import org.gomsu.identityservice.entity.InvalidatedToken;
import org.gomsu.identityservice.entity.User;
import org.gomsu.identityservice.exception.AppException;
import org.gomsu.identityservice.exception.ErrorCode;
import org.gomsu.identityservice.repository.InvalidatedTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.experimental.NonFinal;
import org.gomsu.identityservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest)
    {
        //Kiem tra xem user nay co ton tai khong?
        var user = userRepository.findByEmail(authenticationRequest.getEmail())
                .orElseGet(() -> userRepository.findByPhone(authenticationRequest.getEmail())
                        .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!")));

        //Kiem tra xem password cua user nay co dung khong?
        boolean authenticated = passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new RuntimeException("Sai mật khẩu!");
        }

        var expiryTime = new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli());

        //Tao token
        var token = generateToken(user, expiryTime);

        var refreshToken = UUID.randomUUID().toString();

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .expiryTime(expiryTime)    // Trả về thời gian hết hạn
                .refreshToken(refreshToken) // Trả về Refresh Token
                .build();
    }

    public IntrospectResponse introspect(IntrospectRequest request) {
        var token = request.getToken();
        boolean isValid = true;

        try {
            // Hàm này check đủ 3 lớp: Chữ ký, Hết hạn, và Đã Logout chưa
            verifyToken(token);
        } catch (AppException | JOSEException | ParseException e) {
            // Nếu vi phạm bất kỳ điều kiện nào, ném lỗi -> isValid = false
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    public String generateToken(User user, Date expiryTime)
    {
        //Tao tieu de
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        //Noi dung token
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail()) // Dùng email làm định danh duy nhất thay cho username
                .issuer("org.gomsu.identityservice") //ai la nguoi cung cap?
                .issueTime(new Date()) //thoi gian tao?
                .expirationTime(expiryTime) //Het han sau 1 tieng
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", user.getId())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload); // gop header va noi dung token thanh 1 nhom

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes())); // lay con dau bi mat ra de ky
            return jwsObject.serialize(); // in ra
        } catch (JOSEException e) {
            throw new RuntimeException("Lỗi tạo token", e);
        }
    }

    public void logout(LogoutRequest request) throws JOSEException, ParseException {
        try {
            // Kiểm tra xem Token còn dùng được không mới cho Logout
            var signedJWT = verifyToken(request.getToken());

            String jit = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            // Lưu ID của Token vào bảng "đen" (InvalidatedToken)
            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e) {
            // Nếu Token đã hết hạn hoặc đã logout rồi, mình không cần làm gì nữa
            log.info("Token đã không còn hiệu lực, không cần thực hiện Logout.");
        }
    }

    // Trong file AuthenticationService.java -> hàm buildScope

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> {
                // ❌ Cũ (Lỗi): stringJoiner.add(role.getName());

                // ✅ Mới (Đúng): Thêm .name() để chuyển Enum thành String
                stringJoiner.add(role.getRoleName().name());
            });
        }
        return stringJoiner.toString();
    }

    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        // 1. Tạo bộ xác thực với Key bí mật của Nguyệt
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        // 2. Parse chuỗi Token gửi lên thành đối tượng JWT
        SignedJWT signedJWT = SignedJWT.parse(token);

        // 3. Lấy thời gian hết hạn
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        // 4. Kiểm tra chữ ký (verified)
        var verified = signedJWT.verify(verifier);

        // 5. Nếu chữ ký sai HOẶC token đã hết hạn thì ném lỗi ngay
        if (!(verified && expiryTime.after(new Date()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // 6. Kiểm tra xem Token này đã bị Logout trước đó chưa (Blacklist)
        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }
}
