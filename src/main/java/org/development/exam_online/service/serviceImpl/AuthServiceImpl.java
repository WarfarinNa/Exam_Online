package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.dto.LoginRequest;
import org.development.exam_online.dao.dto.LoginResponse;
import org.development.exam_online.dao.dto.RegisterRequest;
import org.development.exam_online.dao.entity.Permission;
import org.development.exam_online.dao.entity.RolePermission;
import org.development.exam_online.dao.entity.User;
import org.development.exam_online.dao.mapper.PermissionMapper;
import org.development.exam_online.dao.mapper.RolePermissionMapper;
import org.development.exam_online.dao.mapper.UserMapper;
import org.development.exam_online.service.AuthService;
import org.development.exam_online.util.JwtUtils;
import org.development.exam_online.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * 固定角色ID（按需求写死）
     */
    private static final long ROLE_STUDENT = 3L;

    private final UserMapper userMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "注册信息不能为空");
        }

        // 用户名唯一
        LambdaQueryWrapper<User> existsQ = new LambdaQueryWrapper<>();
        existsQ.eq(User::getUsername, request.getUsername())
                .eq(User::getDeleted, 0);
        if (userMapper.selectCount(existsQ) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtils.hashPassword(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRoleId(ROLE_STUDENT);
        user.setDeleted(0);

        int inserted = userMapper.insert(user);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "注册失败");
        }
        return "注册成功";
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录信息不能为空");
        }

        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, request.getUsername())
                .eq(User::getDeleted, 0)
                .last("limit 1");
        User user = userMapper.selectOne(q);
        if (user == null || !PasswordUtils.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRoleId());

        User safe = sanitizeUser(user);
        return LoginResponse.builder()
                .token(token)
                .user(safe)
                .build();
    }

    @Override
    public String logout(String authorization) {
        // JWT 无状态：前端丢弃 token 即可；如需强制失效，可引入 token blacklist/redis
        return "登出成功";
    }

    @Override
    public User me(String authorization) {
        Long userId = requireUserId(authorization);
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getDeleted(), 0)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return sanitizeUser(user);
    }

    @Override
    public List<String> myPermissionCodes(String authorization) {
        Long roleId = requireRoleId(authorization);
        if (roleId == null) return Collections.emptyList();

        // 固定角色也允许绑定 permission_code（由你自定义）
        LambdaQueryWrapper<RolePermission> rpQ = new LambdaQueryWrapper<>();
        rpQ.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> rps = rolePermissionMapper.selectList(rpQ);
        if (rps == null || rps.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> permissionIds = rps.stream()
                .map(RolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (permissionIds.isEmpty()) return Collections.emptyList();

        List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
        if (permissions == null || permissions.isEmpty()) return Collections.emptyList();

        return permissions.stream()
                .filter(p -> p.getDeleted() == null || p.getDeleted() == 0)
                .map(Permission::getPermissionCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long requireUserId(String authorization) {
        String token = extractToken(authorization);
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token无效或已过期");
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无法从Token中获取用户信息");
        }
        return userId;
    }

    private Long requireRoleId(String authorization) {
        String token = extractToken(authorization);
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token无效或已过期");
        }
        return jwtUtils.getRoleIdFromToken(token);
    }

    private static String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) return null;
        return authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
    }

    private static User sanitizeUser(User user) {
        if (user == null) return null;
        User safe = new User();
        safe.setId(user.getId());
        safe.setUsername(user.getUsername());
        safe.setRealName(user.getRealName());
        safe.setEmail(user.getEmail());
        safe.setPhone(user.getPhone());
        safe.setRoleId(user.getRoleId());
        safe.setCreatedBy(user.getCreatedBy());
        safe.setDeleted(user.getDeleted());
        safe.setCreatedAt(user.getCreatedAt());
        safe.setUpdatedAt(user.getUpdatedAt());
        // password 不返回
        safe.setPassword(null);
        return safe;
    }
}

