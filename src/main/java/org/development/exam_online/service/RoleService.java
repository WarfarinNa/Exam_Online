package org.development.exam_online.service;

import org.development.exam_online.dao.entity.Permission;
import org.development.exam_online.dao.entity.Role;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 获取所有角色列表
     * @return 角色列表
     */
    List<Role> getRoleList();

    /**
     * 根据ID获取角色详情
     * @param roleId 角色ID
     * @return 角色详情
     */
    Role getRoleById(Long roleId);

    /**
     * 根据角色名称获取角色详情
     * @param roleName 角色名称
     * @return 角色详情
     */
    Role getRoleByName(String roleName);

    /**
     * 获取角色的权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> getRolePermissions(Long roleId);

    /**
     * 为角色分配权限（覆盖原有权限）
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 分配结果消息
     */
    String assignPermissions(Long roleId, Long[] permissionIds);

    /**
     * 为角色添加权限（追加方式，不覆盖原有权限）
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 添加结果消息
     */
    String addPermissions(Long roleId, Long[] permissionIds);

    /**
     * 移除角色的某个权限
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @return 移除结果消息
     */
    String removePermission(Long roleId, Long permissionId);

    /**
     * 批量移除角色的权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 移除结果消息
     */
    String removePermissions(Long roleId, Long[] permissionIds);
}

