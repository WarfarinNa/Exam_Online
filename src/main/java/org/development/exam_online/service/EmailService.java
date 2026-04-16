package org.development.exam_online.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送验证码邮件
     * @param email 邮箱地址
     * @param code 验证码
     * @param type 验证码类型（REGISTER, RESET_PASSWORD）
     */
    void sendVerificationCode(String email, String code, String type);

    /**
     * 发送重置密码链接邮件
     * @param email 邮箱地址
     * @param resetLink 重置链接
     */
    void sendResetPasswordLink(String email, String resetLink);

    /**
     * 生成6位数字验证码
     * @return 验证码
     */
    String generateVerificationCode();
}