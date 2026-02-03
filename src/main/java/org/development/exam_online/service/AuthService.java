package org.development.exam_online.service;

import org.development.exam_online.dao.dto.LoginRequest;
import org.development.exam_online.dao.dto.LoginResponse;
import org.development.exam_online.dao.dto.RegisterRequest;
import org.development.exam_online.dao.entity.User;

import java.util.List;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     * @param request 注册请求
     * @return 注册结果消息
     */
    String register(RegisterRequest request);

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录响应（包含token和用户信息）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出
     * @param authorization Authorization header（格式：Bearer {token}或直接为token）
     * @return 登出结果消息
     */
    String logout(String authorization);

    /**
     * 获取当前登录用户信息
     * @param authorization Authorization header
     * @return 当前用户（已脱敏）
     */
    User me(String authorization);

    /**
     * 获取当前登录用户的权限码列表
     * @param authorization Authorization header
     * @return permission_code 列表
     */
    List<String> myPermissionCodes(String authorization);
}

