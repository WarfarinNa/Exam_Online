-- 创建邮箱验证码表
CREATE TABLE email_verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    email VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    code VARCHAR(10) NOT NULL COMMENT '验证码',
    type VARCHAR(20) NOT NULL COMMENT '验证码类型：REGISTER-注册，RESET_PASSWORD-重置密码',
    used TINYINT DEFAULT 0 COMMENT '是否已使用：0-未使用，1-已使用',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_email_type (email, type),
    INDEX idx_expire_time (expire_time),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';