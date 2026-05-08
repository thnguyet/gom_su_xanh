package org.gomsu.identityservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.identityservice.entity.Otp;
import org.gomsu.identityservice.repository.OtpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Transactional
    public void generateAndSendOtp(String email, String type) {
        // 1. Generate 6-digit OTP
        String otpCode = String.format("%06d", new Random().nextInt(1000000));
        
        // 2. Save to DB (expires in 5 minutes)
        otpRepository.deleteByEmail(email); // Clean up old OTPs for this email
        Otp otp = Otp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(otp);

        // 3. Send via email based on type
        if ("UPDATE".equalsIgnoreCase(type)) {
            emailService.sendEmailUpdateOtp(email, otpCode);
        } else if ("RESET_PASSWORD".equalsIgnoreCase(type)) {
            emailService.sendPasswordResetOtp(email, otpCode);
        } else {
            emailService.sendRegistrationOtp(email, otpCode);
        }
    }

    public boolean verifyOtp(String email, String otpCode) {
        Optional<Otp> otpOpt = otpRepository.findTopByEmailOrderByExpiryTimeDesc(email);
        
        if (otpOpt.isEmpty()) return false;
        
        Otp otp = otpOpt.get();
        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        return otp.getOtpCode().equals(otpCode);
    }

    @Transactional
    public void deleteOtp(String email) {
        otpRepository.deleteByEmail(email);
    }
}
