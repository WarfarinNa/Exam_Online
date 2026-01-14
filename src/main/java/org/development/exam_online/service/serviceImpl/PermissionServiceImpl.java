package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.dao.entity.Permission;
import org.development.exam_online.dao.mapper.PermissionMapper;
import org.development.exam_online.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限服务实现类
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    /**
     * 获取所有权限列表
     * @return 权限列表
     */
    @Override
    public List<Permission> getPermissionList() {
        return permissionMapper.selectList(null);
    }

    /**
     * 根据ID获取权限详情
     * @param permissionId 权限ID
     * @return 权限详情
     */
    @Override
    public Permission getPermissionById(Long permissionId) {
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }
        return permission;
    }

    /**
     * 根据权限代码获取权限详情
     * @param permissionCode 权限代码（如：user:manage）
     * @return 权限详情
     */
    @Override
    public Permission getPermissionByCode(String permissionCode) {
        LambdaQueryWrapper<Permission> query = new LambdaQueryWrapper<>();
        query.eq(Permission::getPermissionCode, permissionCode);
        Permission permission = permissionMapper.selectOne(query);
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }
        return permission;
    }
}

