package org.development.exam_online.dao.dto;

import lombok.Builder;
import lombok.Data;
import org.development.exam_online.dao.entity.User;

/**
 * 登录响应，返回 token 以及脱敏后的用户信息
 */
@Data
@Builder
public class LoginResponse {
    private String token;
    private User user;
}


