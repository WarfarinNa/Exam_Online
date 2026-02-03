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

/**
 * 用户信息管理控制器
 * 处理用户个人信息维护和密码修改功能
 */
@Tag(name = "用户管理", description = "用户个人信息管理接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户的个人信息
     * @return 用户信息
     */
    @Operation(summary = "获取个人信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/profile")
    public Result<User> getProfile() {
        Long userId = AuthContext.getUserId();
        User user = userService.getProfile(userId);
        return Result.success(user);
    }

    /**
     * 更新当前登录用户的个人信息
     * @param request 更新信息（真实姓名、邮箱、手机号）
     * @return 更新结果
     */
    @Operation(summary = "更新个人信息", description = "更新当前登录用户的个人信息（真实姓名、邮箱、手机号）")
    @PutMapping("/profile")
    public Result<String> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = AuthContext.getUserId();
        String message = userService.updateProfile(userId, request);
        return Result.success(message);
    }

    /**
     * 修改当前登录用户的密码
     * @param request 密码修改请求（旧密码、新密码）
     * @return 修改结果
     */
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @PutMapping("/password")
    public Result<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = AuthContext.getUserId();
        String message = userService.changePassword(userId, request);
        return Result.success(message);
    }

    /**
     * 获取用户列表（管理员功能）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 搜索关键词（用户名、真实姓名）
     * @param roleId 角色ID筛选
     * @return 用户列表
     */
    @Operation(summary = "获取用户列表", description = "管理员查看用户列表，支持分页和搜索")
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

    /**
     * 根据ID获取用户详情（管理员功能）
     * @param userId 用户ID
     * @return 用户详细信息
     */
    @Operation(summary = "获取用户详情", description = "管理员查看指定用户的详细信息")
    @GetMapping("/{userId}")
    @RequirePermission({"user:manage"})
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 更新用户信息（管理员功能）
     * @param userId 用户ID
     * @param request 更新信息
     * @return 更新结果
     */
    @Operation(summary = "更新用户信息", description = "管理员更新指定用户的信息")
    @PutMapping("/{userId}")
    @RequirePermission({"user:manage"})
    public Result<String> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        String message = userService.updateUser(userId, request);
        return Result.success(message);
    }

    /**
     * 删除用户（管理员功能，逻辑删除）
     * @param userId 用户ID
     * @return 删除结果
     */
    @Operation(summary = "删除用户", description = "管理员删除指定用户（逻辑删除）")
    @DeleteMapping("/{userId}")
    @RequirePermission({"user:manage"})
    public Result<String> deleteUser(@PathVariable Long userId) {
        String message = userService.deleteUser(userId);
        return Result.success(message);
    }

    /**
     * 修改用户角色（管理员功能）
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 修改结果
     */
    @Operation(summary = "修改用户角色", description = "管理员修改指定用户的角色")
    @PutMapping("/{userId}/role")
    @RequirePermission({"user:manage"})
    public Result<String> updateUserRole(
            @PathVariable Long userId,
            @RequestParam Long roleId) {
        String message = userService.updateUserRole(userId, roleId);
        return Result.success(message);
    }
}

