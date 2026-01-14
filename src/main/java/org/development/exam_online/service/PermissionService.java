package org.development.exam_online.service;

import org.development.exam_online.dao.entity.Permission;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 获取所有权限列表
     * @return 权限列表
     */
    List<Permission> getPermissionList();

    /**
     * 根据ID获取权限详情
     * @param permissionId 权限ID
     * @return 权限详情
     */
    Permission getPermissionById(Long permissionId);

    /**
     * 根据权限代码获取权限详情
     * @param permissionCode 权限代码（如：user:manage）
     * @return 权限详情
     */
    Permission getPermissionByCode(String permissionCode);
}

