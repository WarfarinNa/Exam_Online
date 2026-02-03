package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.entity.Permission;
import org.development.exam_online.dao.mapper.PermissionMapper;
import org.development.exam_online.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限服务实现
 *
 * <p>权限集合由初始化脚本（如 init_design.sql）预置，默认仅提供查询能力。</p>
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    public List<Permission> getPermissionList() {
        LambdaQueryWrapper<Permission> q = new LambdaQueryWrapper<>();
        q.eq(Permission::getDeleted, 0)
                .orderByAsc(Permission::getId);
        return permissionMapper.selectList(q);
    }

    @Override
    public Permission getPermissionById(Long permissionId) {
        if (permissionId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "permissionId不能为空");
        }
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null || (permission.getDeleted() != null && permission.getDeleted() != 0)) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return permission;
    }

    @Override
    public Permission getPermissionByCode(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "permissionCode不能为空");
        }
        LambdaQueryWrapper<Permission> q = new LambdaQueryWrapper<>();
        q.eq(Permission::getPermissionCode, permissionCode)
                .eq(Permission::getDeleted, 0)
                .last("limit 1");
        Permission permission = permissionMapper.selectOne(q);
        if (permission == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return permission;
    }
}

