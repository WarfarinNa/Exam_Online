package org.development.exam_online.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.development.exam_online.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String email, String code, String type) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            
            String subject;
            String content;
            
            if ("REGISTER".equals(type)) {
                subject = "在线考试系统 - 注册验证码";
                content = String.format(
                    "您好！\n\n" +
                    "您正在注册在线考试系统账号，验证码为：%s\n\n" +
                    "验证码有效期为5分钟，请及时使用。\n" +
                    "如果这不是您的操作，请忽略此邮件。\n\n" +
                    "在线考试系统", code);
            } else {
                subject = "在线考试系统 - 密码重置验证码";
                content = String.format(
                    "您好！\n\n" +
                    "您正在重置在线考试系统密码，验证码为：%s\n\n" +
                    "验证码有效期为5分钟，请及时使用。\n" +
                    "如果这不是您的操作，请忽略此邮件。\n\n" +
                    "在线考试系统", code);
            }
            
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("验证码邮件发送成功，邮箱：{}，类型：{}", email, type);
        } catch (Exception e) {
            log.error("验证码邮件发送失败，邮箱：{}，错误：{}", email, e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    @Override
    public void sendResetPasswordLink(String email, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("在线考试系统 - 密码重置链接");
            
            String content = String.format(
                "您好！\n\n" +
                "您申请重置在线考试系统密码，请点击以下链接进行密码重置：\n\n" +
                "%s\n\n" +
                "链接有效期为30分钟，请及时使用。\n" +
                "如果这不是您的操作，请忽略此邮件。\n\n" +
                "在线考试系统", resetLink);
            
            message.setText(content);
            mailSender.send(message);
            log.info("密码重置链接邮件发送成功，邮箱：{}", email);
        } catch (Exception e) {
            log.error("密码重置链接邮件发送失败，邮箱：{}，错误：{}", email, e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    @Override
    public String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}