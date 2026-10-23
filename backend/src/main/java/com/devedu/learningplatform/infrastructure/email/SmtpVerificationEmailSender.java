package com.devedu.learningplatform.infrastructure.email;

import com.devedu.learningplatform.application.exception.VerificationEmailDeliveryException;
import com.devedu.learningplatform.application.port.out.VerificationEmailSender;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.nio.charset.StandardCharsets;

@Component
public final class SmtpVerificationEmailSender implements VerificationEmailSender {

    private final JavaMailSender mailSender;
    private final boolean configured;
    private final String from;

    public SmtpVerificationEmailSender(
            JavaMailSender mailSender,
            @Value("${devedu.email-verification.enabled:false}") boolean configured,
            @Value("${devedu.email-verification.from:no-reply@devedu.local}") String from
    ) {
        this.mailSender = mailSender;
        this.configured = configured;
        this.from = from;
    }

    @Override
    public void sendRegistrationCode(
            String recipientEmail,
            String recipientName,
            String code,
            Duration validFor
    ) {
        if (!configured) {
            throw new VerificationEmailDeliveryException(
                    "Dịch vụ gửi email chưa được cấu hình. Vui lòng liên hệ quản trị viên"
            );
        }

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipientEmail);
            helper.setSubject("Mã xác nhận email DevEdu");
            helper.setText(plainText(recipientName, code, validFor), html(recipientName, code, validFor));
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            throw new VerificationEmailDeliveryException(
                    "Không thể gửi mã xác nhận lúc này. Vui lòng thử lại sau",
                    exception
            );
        }
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    private String plainText(String recipientName, String code, Duration validFor) {
        return "Xin chào " + recipientName + ",\n\n"
                + "Mã xác nhận email DevEdu của bạn là: " + code + "\n\n"
                + "Mã có hiệu lực trong " + validFor.toMinutes() + " phút. "
                + "Nếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.\n\n"
                + "DevEdu";
    }

    private String html(String recipientName, String code, Duration validFor) {
        var safeName = escapeHtml(recipientName);
        var safeCode = escapeHtml(code);
        var minutes = validFor.toMinutes();
        return """
                <!doctype html>
                <html lang="vi">
                  <body style="margin:0;padding:0;background:#f4f7fb;font-family:'Segoe UI',Arial,sans-serif;color:#172033;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f7fb;padding:32px 16px;">
                      <tr><td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #dbe5f1;border-radius:16px;overflow:hidden;box-shadow:0 12px 32px rgba(13,110,253,.10);">
                          <tr><td style="background:#0d6efd;padding:24px 32px;color:#ffffff;">
                            <div style="font-size:22px;font-weight:800;letter-spacing:-.3px;">&lt;/&gt; DevEdu</div>
                            <div style="margin-top:5px;font-size:13px;color:#dceaff;">Nền tảng học lập trình</div>
                          </td></tr>
                          <tr><td style="padding:32px;">
                            <h1 style="margin:0 0 12px;font-size:24px;line-height:1.3;color:#111827;">Xác nhận địa chỉ email</h1>
                            <p style="margin:0 0 22px;font-size:15px;line-height:1.7;color:#566176;">Xin chào <strong style="color:#172033;">%s</strong>, dùng mã dưới đây để hoàn tất đăng ký tài khoản DevEdu.</p>
                            <div style="margin:0 0 22px;padding:20px;text-align:center;background:#eff6ff;border:1px solid #bfdbfe;border-radius:12px;">
                              <div style="margin-bottom:8px;font-size:11px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;color:#50709f;">Mã xác nhận</div>
                              <div style="font-family:Consolas,'Courier New',monospace;font-size:34px;font-weight:800;letter-spacing:10px;color:#0d6efd;">%s</div>
                            </div>
                            <p style="margin:0;font-size:14px;line-height:1.7;color:#566176;">Mã có hiệu lực trong <strong style="color:#172033;">%d phút</strong>. Không chia sẻ mã này với bất kỳ ai.</p>
                            <div style="height:1px;background:#e5eaf1;margin:26px 0 20px;"></div>
                            <p style="margin:0;font-size:12px;line-height:1.6;color:#7b8798;">Nếu bạn không yêu cầu đăng ký tài khoản, hãy bỏ qua email này.</p>
                          </td></tr>
                        </table>
                        <p style="margin:18px 0 0;font-size:11px;color:#8b96a8;">Email tự động từ DevEdu · Vui lòng không trả lời</p>
                      </td></tr>
                    </table>
                  </body>
                </html>
                """.formatted(safeName, safeCode, minutes);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
