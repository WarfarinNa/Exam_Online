package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.dto.LoginRequest;
import org.development.exam_online.dao.dto.LoginResponse;
import org.development.exam_online.dao.dto.RegisterRequest;
import org.development.exam_online.dao.entity.User;
import org.development.exam_online.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器
 * 处理用户注册和登录功能
 */
@Tag(name = "认证管理", description = "用户注册、登录接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * @param request 注册请求信息
     * @return 注册结果
     */
    @Operation(summary = "用户注册", description = "新用户注册账号，默认角色为学生")
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return Result.success(message);
    }

    /**
     * 用户登录
     * @param request 登录请求信息（用户名、密码）
     * @return 登录响应（包含token和用户信息）
     */
    @Operation(summary = "用户登录", description = "用户通过用户名和密码登录系统")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 用户登出
     * @param authorization Authorization header（格式：Bearer {token}）
     * @return 登出结果
     */
    @Operation(summary = "用户登出", description = "用户退出登录，清除token")
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String message = authService.logout(authorization);
        return Result.success(message);
    }

    @Operation(summary = "获取当前登录用户", description = "从JWT中解析用户身份，返回当前登录用户信息（脱敏）")
    @GetMapping("/me")
    public Result<User> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(authService.me(authorization));
    }

    @Operation(summary = "获取当前用户权限码", description = "返回当前用户所属角色绑定的 permission_code 列表")
    @GetMapping("/permissions")
    public Result<List<String>> myPermissions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(authService.myPermissionCodes(authorization));
    }
}

