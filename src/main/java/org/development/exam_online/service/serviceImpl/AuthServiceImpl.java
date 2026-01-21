package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.dto.LoginRequest;
import org.development.exam_online.dao.dto.LoginResponse;
import org.development.exam_online.dao.dto.RegisterRequest;
import org.development.exam_online.dao.entity.Role;
import org.development.exam_online.dao.entity.User;
import org.development.exam_online.dao.mapper.RoleMapper;
import org.development.exam_online.dao.mapper.UserMapper;
import org.development.exam_online.service.AuthService;
import org.development.exam_online.util.JwtUtils;
import org.development.exam_online.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务实现类
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final JwtUtils jwtUtils;

    // 学生角色名称常量
    private static final String STUDENT_ROLE_NAME = "STUDENT";

    /**
     * 用户注册
     * @param request 注册请求
     * @return 注册结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(User::getUsername, request.getUsername());
        User existingUser = userMapper.selectOne(userQuery);
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 检查邮箱是否已存在（如果提供了邮箱）
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, request.getEmail());
            User existingEmail = userMapper.selectOne(emailQuery);
            if (existingEmail != null) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
        }

        // 获取学生角色ID（默认角色）
        LambdaQueryWrapper<Role> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.eq(Role::getRoleName, STUDENT_ROLE_NAME);
        Role studentRole = roleMapper.selectOne(roleQuery);
        if (studentRole == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "系统错误：学生角色不存在");
        }

        // 创建新用户
        User user = User.builder()
                .username(request.getUsername())
                .password(PasswordUtils.hashPassword(request.getPassword()))
                .realName(request.getRealName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .roleId(studentRole.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        int result = userMapper.insert(user);
        if (result > 0) {
            return "注册成功";
        } else {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "注册失败");
        }
    }

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录响应（包含token和用户信息）
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 根据用户名查询用户
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(query);

        if (user == null) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 验证密码
        if (!PasswordUtils.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 生成JWT token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRoleId());

        // 脱敏处理：不返回密码
        User userWithoutPassword = User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleId(user.getRoleId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        // 构建登录响应
        return LoginResponse.builder()
                .token(token)
                .user(userWithoutPassword)
                .build();
    }

    /**
     * 用户登出
     * @param authorization Authorization header（格式：Bearer {token}或直接为token）
     * @return 登出结果消息
     */
    @Override
    public String logout(String authorization) {
        // 从Authorization header中提取token（格式：Bearer {token}）
        String token = null;
        if (authorization != null && !authorization.isEmpty()) {
            if (authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            } else {
                token = authorization;
            }
        }

        // 由于JWT是无状态的，登出操作主要是客户端删除token
        // 如果需要服务端控制，可以实现token黑名单机制（使用Redis等）
        // 这里简单返回成功消息
        return "登出成功";
    }
}

