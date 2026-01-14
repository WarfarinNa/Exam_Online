package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.dao.entity.Permission;
import org.development.exam_online.dao.entity.Role;
import org.development.exam_online.dao.entity.RolePermission;
import org.development.exam_online.dao.mapper.PermissionMapper;
import org.development.exam_online.dao.mapper.RoleMapper;
import org.development.exam_online.dao.mapper.RolePermissionMapper;
import org.development.exam_online.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 获取所有角色列表
     * @return 角色列表
     */
    @Override
    public List<Role> getRoleList() {
        return roleMapper.selectList(null);
    }

    /**
     * 根据ID获取角色详情
     * @param roleId 角色ID
     * @return 角色详情
     */
    @Override
    public Role getRoleById(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        return role;
    }

    /**
     * 根据角色名称获取角色详情
     * @param roleName 角色名称
     * @return 角色详情
     */
    @Override
    public Role getRoleByName(String roleName) {
        LambdaQueryWrapper<Role> query = new LambdaQueryWrapper<>();
        query.eq(Role::getRoleName, roleName);
        Role role = roleMapper.selectOne(query);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        return role;
    }

    /**
     * 获取角色的权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public List<Permission> getRolePermissions(Long roleId) {
        // 验证角色是否存在
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 查询角色权限关联关系
        LambdaQueryWrapper<RolePermission> rolePermissionQuery = new LambdaQueryWrapper<>();
        rolePermissionQuery.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(rolePermissionQuery);

        // 提取权限ID列表
        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());

        // 如果没有权限，返回空列表
        if (permissionIds.isEmpty()) {
            return List.of();
        }

        // 查询权限详情
        LambdaQueryWrapper<Permission> permissionQuery = new LambdaQueryWrapper<>();
        permissionQuery.in(Permission::getId, permissionIds);
        return permissionMapper.selectList(permissionQuery);
    }

    /**
     * 为角色分配权限（覆盖原有权限）
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 分配结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String assignPermissions(Long roleId, Long[] permissionIds) {
        // 验证角色是否存在
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 验证权限是否存在
        if (permissionIds != null && permissionIds.length > 0) {
            LambdaQueryWrapper<Permission> permissionQuery = new LambdaQueryWrapper<>();
            permissionQuery.in(Permission::getId, List.of(permissionIds));
            List<Permission> permissions = permissionMapper.selectList(permissionQuery);
            if (permissions.size() != permissionIds.length) {
                throw new RuntimeException("部分权限不存在");
            }
        }

        // 删除原有权限关联
        LambdaQueryWrapper<RolePermission> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(RolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(deleteQuery);

        // 添加新权限关联
        if (permissionIds != null && permissionIds.length > 0) {
            for (Long permissionId : permissionIds) {
                RolePermission rolePermission = RolePermission.builder()
                        .roleId(roleId)
                        .permissionId(permissionId)
                        .build();
                rolePermissionMapper.insert(rolePermission);
            }
        }

        return "权限分配成功";
    }

    /**
     * 为角色添加权限（追加方式，不覆盖原有权限）
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 添加结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addPermissions(Long roleId, Long[] permissionIds) {
        // 验证角色是否存在
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 验证权限是否存在
        if (permissionIds == null || permissionIds.length == 0) {
            throw new RuntimeException("权限ID列表不能为空");
        }

        LambdaQueryWrapper<Permission> permissionQuery = new LambdaQueryWrapper<>();
        permissionQuery.in(Permission::getId, List.of(permissionIds));
        List<Permission> permissions = permissionMapper.selectList(permissionQuery);
        if (permissions.size() != permissionIds.length) {
            throw new RuntimeException("部分权限不存在");
        }

        // 查询已有权限
        LambdaQueryWrapper<RolePermission> query = new LambdaQueryWrapper<>();
        query.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> existingRolePermissions = rolePermissionMapper.selectList(query);
        List<Long> existingPermissionIds = existingRolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());

        // 过滤掉已存在的权限，只添加新权限
        int addedCount = 0;
        for (Long permissionId : permissionIds) {
            if (!existingPermissionIds.contains(permissionId)) {
                RolePermission rolePermission = RolePermission.builder()
                        .roleId(roleId)
                        .permissionId(permissionId)
                        .build();
                rolePermissionMapper.insert(rolePermission);
                addedCount++;
            }
        }

        return "成功添加 " + addedCount + " 个权限";
    }

    /**
     * 移除角色的某个权限
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @return 移除结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String removePermission(Long roleId, Long permissionId) {
        // 验证角色是否存在
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 验证权限是否存在
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }

        // 删除权限关联
        LambdaQueryWrapper<RolePermission> query = new LambdaQueryWrapper<>();
        query.eq(RolePermission::getRoleId, roleId)
                .eq(RolePermission::getPermissionId, permissionId);
        int deleted = rolePermissionMapper.delete(query);

        if (deleted > 0) {
            return "权限移除成功";
        } else {
            throw new RuntimeException("该角色未拥有此权限");
        }
    }

    /**
     * 批量移除角色的权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 移除结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String removePermissions(Long roleId, Long[] permissionIds) {
        // 验证角色是否存在
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 验证权限是否存在
        if (permissionIds == null || permissionIds.length == 0) {
            throw new RuntimeException("权限ID列表不能为空");
        }

        LambdaQueryWrapper<Permission> permissionQuery = new LambdaQueryWrapper<>();
        permissionQuery.in(Permission::getId, List.of(permissionIds));
        List<Permission> permissions = permissionMapper.selectList(permissionQuery);
        if (permissions.size() != permissionIds.length) {
            throw new RuntimeException("部分权限不存在");
        }

        // 批量删除权限关联
        int removedCount = 0;
        for (Long permissionId : permissionIds) {
            LambdaQueryWrapper<RolePermission> query = new LambdaQueryWrapper<>();
            query.eq(RolePermission::getRoleId, roleId)
                    .eq(RolePermission::getPermissionId, permissionId);
            int deleted = rolePermissionMapper.delete(query);
            if (deleted > 0) {
                removedCount++;
            }
        }

        return "成功移除 " + removedCount + " 个权限";
    }
}

