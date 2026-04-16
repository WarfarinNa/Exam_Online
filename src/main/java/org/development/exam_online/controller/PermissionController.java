package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.entity.Permission;
import org.development.exam_online.service.PermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 * 权限是系统预定义的，仅提供查询接口
 */
@Tag(name = "权限管理")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "获取权限列表")
    @GetMapping
    public Result<List<Permission>> getPermissionList() {
        List<Permission> permissions = permissionService.getPermissionList();
        return Result.success(permissions);
    }

    @Operation(summary = "获取权限详情")
    @GetMapping("/{permissionId}")
    public Result<Permission> getPermissionById(@PathVariable Long permissionId) {
        Permission permission = permissionService.getPermissionById(permissionId);
        return Result.success(permission);
    }

    @Operation(summary = "根据代码获取权限")
    @GetMapping("/code/{permissionCode}")
    public Result<Permission> getPermissionByCode(@PathVariable String permissionCode) {
        Permission permission = permissionService.getPermissionByCode(permissionCode);
        return Result.success(permission);
    }
}

