package com.devedu.learningplatform.infrastructure.email;

import com.devedu.learningplatform.application.exception.VerificationEmailDeliveryException;
import jakarta.mail.Session;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpVerificationEmailSenderTest {

    @Test
    void sendsBrandedHtmlWithPlainTextFallback() throws Exception {
        var mailSender = mock(JavaMailSender.class);
        var message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        var sender = new SmtpVerificationEmailSender(mailSender, true, "no-reply@devedu.test");

        sender.sendRegistrationCode(
                "student@example.com", "Nguyễn <An>", "042731", Duration.ofMinutes(10)
        );

        verify(mailSender).send(message);
        message.saveChanges();
        var output = new ByteArrayOutputStream();
        message.writeTo(output);
        var raw = output.toString(java.nio.charset.StandardCharsets.UTF_8);
        var parts = textParts(message.getContent());
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("student@example.com");
        assertThat(message.getFrom()[0].toString()).isEqualTo("no-reply@devedu.test");
        assertThat(raw).contains("multipart/alternative", "042731", "#0d6efd");
        assertThat(parts).anySatisfy(part -> assertThat(part).contains("Nguyễn &lt;An&gt;", "042731"));
        assertThat(parts).anySatisfy(part -> assertThat(part).contains("Xin chào Nguyễn <An>", "10 phút"));
        assertThat(sender.isConfigured()).isTrue();
    }

    @Test
    void refusesToSendWhenSmtpIsDisabled() {
        var mailSender = mock(JavaMailSender.class);
        var sender = new SmtpVerificationEmailSender(mailSender, false, "no-reply@devedu.test");

        assertThatThrownBy(() -> sender.sendRegistrationCode(
                "student@example.com", "Student", "123456", Duration.ofMinutes(10)))
                .isInstanceOf(VerificationEmailDeliveryException.class)
                .hasMessageContaining("chưa được cấu hình");
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(sender.isConfigured()).isFalse();
    }

    @Test
    void mapsSmtpFailuresToAnApplicationError() {
        var mailSender = mock(JavaMailSender.class);
        var message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("smtp unavailable"))
                .when(mailSender).send(any(MimeMessage.class));
        var sender = new SmtpVerificationEmailSender(mailSender, true, "no-reply@devedu.test");

        assertThatThrownBy(() -> sender.sendRegistrationCode(
                "student@example.com", "Student", "123456", Duration.ofMinutes(10)))
                .isInstanceOf(VerificationEmailDeliveryException.class)
                .hasMessageContaining("Không thể gửi");
    }

    private List<String> textParts(Object content) throws Exception {
        var values = new ArrayList<String>();
        collectTextParts(content, values);
        return values;
    }

    private void collectTextParts(Object content, List<String> values) throws Exception {
        if (content instanceof String value) {
            values.add(value);
            return;
        }
        if (content instanceof Multipart multipart) {
            for (var index = 0; index < multipart.getCount(); index++) {
                collectTextParts(multipart.getBodyPart(index).getContent(), values);
            }
        }
    }
}
