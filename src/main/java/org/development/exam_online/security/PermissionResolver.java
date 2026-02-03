package org.development.exam_online.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.dao.entity.Permission;
import org.development.exam_online.dao.entity.RolePermission;
import org.development.exam_online.dao.mapper.PermissionMapper;
import org.development.exam_online.dao.mapper.RolePermissionMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 根据角色ID解析权限码集合
 */
@Component
@RequiredArgsConstructor
public class PermissionResolver {

    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    public Set<String> resolvePermissionCodesByRoleId(Long roleId) {
        if (roleId == null) return Collections.emptySet();

        LambdaQueryWrapper<RolePermission> rpQ = new LambdaQueryWrapper<>();
        rpQ.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> rps = rolePermissionMapper.selectList(rpQ);
        if (rps == null || rps.isEmpty()) return Collections.emptySet();

        List<Long> permissionIds = rps.stream()
                .map(RolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (permissionIds.isEmpty()) return Collections.emptySet();

        List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
        if (permissions == null || permissions.isEmpty()) return Collections.emptySet();

        return permissions.stream()
                .filter(p -> p.getDeleted() == null || p.getDeleted() == 0)
                .map(Permission::getPermissionCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}

