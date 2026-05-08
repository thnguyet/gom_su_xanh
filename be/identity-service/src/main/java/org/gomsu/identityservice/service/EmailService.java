package org.gomsu.identityservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendRegistrationOtp(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Gốm Sứ Xanh <noreply@gomsu.vn>");
        message.setTo(to);
        message.setSubject("Mã OTP xác thực đăng ký tài khoản - Gốm Sứ Xanh");
        message.setText("Chào bạn,\n\n"
                + "Bạn đang thực hiện đăng ký tài khoản tại Gốm Sứ Xanh.\n"
                + "Mã OTP của bạn là: " + otp + "\n"
                + "Mã này sẽ hết hạn sau 5 phút.\n\n"
                + "Nếu không phải bạn thực hiện yêu cầu này, vui lòng bỏ qua email.\n"
                + "Trân trọng,\n"
                + "Đội ngũ Gốm Sứ Xanh.");
        
        mailSender.send(message);
    }

    public void sendEmailUpdateOtp(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Gốm Sứ Xanh <noreply@gomsu.vn>");
        message.setTo(to);
        message.setSubject("Mã OTP xác thực thay đổi Email - Gốm Sứ Xanh");
        message.setText("Chào bạn,\n\n"
                + "Bạn đang yêu cầu thay đổi email đăng nhập tại Gốm Sứ Xanh.\n"
                + "Mã OTP xác thực của bạn là: " + otp + "\n"
                + "Mã này sẽ hết hạn sau 5 phút. Vui lòng không cung cấp mã này cho bất kỳ ai.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ Gốm Sứ Xanh.");

        mailSender.send(message);
    }

    public void sendPasswordResetOtp(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Gốm Sứ Xanh <noreply@gomsu.vn>");
        message.setTo(to);
        message.setSubject("[Gốm Sứ Xanh] Xác nhận đặt lại mật khẩu");
        message.setText("Xin chào,\n\n"
                + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email này tại Gốm Sứ Xanh.\n"
                + "Mã xác thực (OTP) của bạn là: " + otp + "\n"
                + "Mã này có hiệu lực trong vòng 5 phút. Để đảm bảo an toàn, vui lòng không chia sẻ mã này với bất kỳ ai.\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này hoặc liên hệ với bộ phận hỗ trợ của chúng tôi để được trợ giúp.\n\n"
                + "Trân trọng,\n"
                + "Ban quản trị Gốm Sứ Xanh.");

        mailSender.send(message);
    }
}
