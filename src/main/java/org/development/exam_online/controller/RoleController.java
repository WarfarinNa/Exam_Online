//package org.development.exam_online.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.development.exam_online.common.Result;
//import org.development.exam_online.dao.entity.Permission;
//import org.development.exam_online.dao.entity.Role;
//import org.development.exam_online.service.RoleService;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
///**
// * 角色管理控制器
// * 角色是系统预定义的（ADMIN、TEACHER、STUDENT），仅提供查询和权限分配接口
// */
//@Tag(name = "角色管理", description = "角色管理接口（角色为系统预定义，管理员可分配权限）")
//@RestController
//@RequestMapping("/api/roles")
//@RequiredArgsConstructor
//public class RoleController {
//
//    private final RoleService roleService;
//
//    /**
//     * 获取所有角色列表
//     * @return 角色列表（ADMIN、TEACHER、STUDENT）
//     */
//    @Operation(summary = "获取角色列表", description = "获取系统中所有预定义的角色列表")
//    @GetMapping
//    public Result<List<Role>> getRoleList() {
//        List<Role> roles = roleService.getRoleList();
//        return Result.success(roles);
//    }
//
//    /**
//     * 根据ID获取角色详情
//     * @param roleId 角色ID
//     * @return 角色详情
//     */
//    @Operation(summary = "获取角色详情", description = "根据ID获取角色详细信息")
//    @GetMapping("/{roleId}")
//    public Result<Role> getRoleById(@PathVariable Long roleId) {
//        Role role = roleService.getRoleById(roleId);
//        return Result.success(role);
//    }
//
//    /**
//     * 根据角色名称获取角色详情
//     * @param roleName 角色名称（ADMIN、TEACHER、STUDENT）
//     * @return 角色详情
//     */
//    @Operation(summary = "根据名称获取角色", description = "根据角色名称获取角色详细信息")
//    @GetMapping("/name/{roleName}")
//    public Result<Role> getRoleByName(@PathVariable String roleName) {
//        Role role = roleService.getRoleByName(roleName);
//        return Result.success(role);
//    }
//
//    /**
//     * 获取角色的权限列表
//     * @param roleId 角色ID
//     * @return 权限列表
//     */
//    @Operation(summary = "获取角色权限", description = "获取指定角色拥有的所有权限")
//    @GetMapping("/{roleId}/permissions")
//    public Result<List<Permission>> getRolePermissions(@PathVariable Long roleId) {
//        List<Permission> permissions = roleService.getRolePermissions(roleId);
//        return Result.success(permissions);
//    }
//
//    /**
//     * 为角色分配权限（管理员功能）
//     * @param roleId 角色ID
//     * @param permissionIds 权限ID列表
//     * @return 分配结果
//     */
//    @Operation(summary = "分配角色权限", description = "管理员为指定角色分配权限（覆盖原有权限）")
//    @PostMapping("/{roleId}/permissions")
//    public Result<String> assignPermissions(
//            @PathVariable Long roleId,
//            @RequestBody Long[] permissionIds) {
//        String message = roleService.assignPermissions(roleId, permissionIds);
//        return Result.success(message);
//    }
//
//    /**
//     * 批量添加角色权限（管理员功能）
//     * @param roleId 角色ID
//     * @param permissionIds 权限ID列表
//     * @return 添加结果
//     */
//    @Operation(summary = "添加角色权限", description = "管理员为指定角色添加权限（追加方式，不覆盖原有权限）")
//    @PutMapping("/{roleId}/permissions")
//    public Result<String> addPermissions(
//            @PathVariable Long roleId,
//            @RequestBody Long[] permissionIds) {
//        String message = roleService.addPermissions(roleId, permissionIds);
//        return Result.success(message);
//    }
//
//    /**
//     * 移除角色的权限（管理员功能）
//     * @param roleId 角色ID
//     * @param permissionId 权限ID
//     * @return 移除结果
//     */
//    @Operation(summary = "移除角色权限", description = "管理员移除指定角色的某个权限")
//    @DeleteMapping("/{roleId}/permissions/{permissionId}")
//    public Result<String> removePermission(
//            @PathVariable Long roleId,
//            @PathVariable Long permissionId) {
//        String message = roleService.removePermission(roleId, permissionId);
//        return Result.success(message);
//    }
//
//    /**
//     * 批量移除角色权限（管理员功能）
//     * @param roleId 角色ID
//     * @param permissionIds 权限ID列表
//     * @return 移除结果
//     */
//    @Operation(summary = "批量移除角色权限", description = "管理员批量移除指定角色的多个权限")
//    @DeleteMapping("/{roleId}/permissions")
//    public Result<String> removePermissions(
//            @PathVariable Long roleId,
//            @RequestBody Long[] permissionIds) {
//        String message = roleService.removePermissions(roleId, permissionIds);
//        return Result.success(message);
//    }
//}
//
