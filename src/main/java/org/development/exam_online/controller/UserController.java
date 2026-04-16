package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.dto.ChangePasswordRequest;
import org.development.exam_online.dao.dto.UpdateProfileRequest;
import org.development.exam_online.dao.entity.User;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.security.RequirePermission;
import org.development.exam_online.service.UserService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<User> getProfile() {
        Long userId = AuthContext.getUserId();
        User user = userService.getProfile(userId);
        return Result.success(user);
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<String> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = AuthContext.getUserId();
        String message = userService.updateProfile(userId, request);
        return Result.success(message);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = AuthContext.getUserId();
        String message = userService.changePassword(userId, request);
        return Result.success(message);
    }

    @Operation(summary = "获取用户列表")
    @GetMapping
    @RequirePermission({"user:manage"})
    public Result<PageResult<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId) {
        PageResult<User> result = userService.getUserList(pageNum, pageSize, keyword, roleId);
        return Result.success(result);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{userId}")
    @RequirePermission({"user:manage"})
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{userId}")
    @RequirePermission({"user:manage"})
    public Result<String> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        String message = userService.updateUser(userId, request);
        return Result.success(message);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{userId}")
    @RequirePermission({"user:manage"})
    public Result<String> deleteUser(@PathVariable Long userId) {
        String message = userService.deleteUser(userId);
        return Result.success(message);
    }

    @Operation(summary = "修改用户角色")
    @PutMapping("/{userId}/role")
    @RequirePermission({"user:manage"})
    public Result<String> updateUserRole(
            @PathVariable Long userId,
            @RequestParam Long roleId) {
        String message = userService.updateUserRole(userId, roleId);
        return Result.success(message);
    }
}

