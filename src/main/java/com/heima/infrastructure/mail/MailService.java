package com.heima.infrastructure.mail;

import com.heima.exception.ServiceException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender sender;
    @Value("${wanjuan.mail.enabled:false}")
    private boolean enabled;
    @Value("${wanjuan.mail.from:}")
    private String from;

    public void sendCode(String email, String scene, String code) {
        if (!enabled || from == null || from.isBlank()) throw new ServiceException(503, "邮件服务未配置");
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("万卷汇验证码");
            helper.setText("您正在进行" + scene + "操作，验证码为：" + code + "，10分钟内有效。请勿向他人泄露。", false);
            sender.send(message);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(503, "邮件发送失败，请稍后重试");
        }
    }
}
