package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.dto.LoginRequest;
import org.development.exam_online.dao.dto.LoginResponse;
import org.development.exam_online.dao.dto.RegisterRequest;
import org.development.exam_online.dao.dto.SendVerificationCodeRequest;
import org.development.exam_online.dao.dto.ResetPasswordRequest;
import org.development.exam_online.dao.entity.User;
import org.development.exam_online.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "发送验证码")
    @PostMapping("/send-code")
    public Result<String> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        String message = authService.sendVerificationCode(request);
        return Result.success(message);
    }


    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return Result.success(message);
    }


    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }


    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String message = authService.logout(authorization);
        return Result.success(message);
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public Result<User> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(authService.me(authorization));
    }

    @Operation(summary = "获取当前用户权限码")
    @GetMapping("/permissions")
    public Result<List<String>> myPermissions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(authService.myPermissionCodes(authorization));
    }


    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String message = authService.resetPassword(request);
        return Result.success(message);
    }
}

