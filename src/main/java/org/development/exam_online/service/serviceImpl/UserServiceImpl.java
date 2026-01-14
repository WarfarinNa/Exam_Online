package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.dao.dto.ChangePasswordRequest;
import org.development.exam_online.dao.dto.UpdateProfileRequest;
import org.development.exam_online.dao.entity.Role;
import org.development.exam_online.dao.entity.User;
import org.development.exam_online.dao.mapper.RoleMapper;
import org.development.exam_online.dao.mapper.UserMapper;
import org.development.exam_online.service.UserService;
import org.development.exam_online.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    /**
     * 获取当前登录用户的个人信息
     * @param userId 用户ID
     * @return 用户信息（脱敏处理，不包含密码）
     */
    @Override
    public User getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 脱敏处理：不返回密码
        return User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleId(user.getRoleId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 更新当前登录用户的个人信息
     * @param userId 用户ID
     * @param request 更新信息
     * @return 更新结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查邮箱是否已被其他用户使用
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, request.getEmail())
                    .ne(User::getId, userId);
            User existingEmail = userMapper.selectOne(emailQuery);
            if (existingEmail != null) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
        }

        // 更新用户信息
        User updateUser = User.builder()
                .id(userId)
                .realName(request.getRealName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .updatedAt(LocalDateTime.now())
                .build();

        int result = userMapper.updateById(updateUser);
        if (result > 0) {
            return "个人信息更新成功";
        } else {
            throw new RuntimeException("个人信息更新失败");
        }
    }

    /**
     * 修改当前登录用户的密码
     * @param userId 用户ID
     * @param request 密码修改请求
     * @return 修改结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        if (!PasswordUtils.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 更新密码
        User updateUser = User.builder()
                .id(userId)
                .password(PasswordUtils.hashPassword(request.getNewPassword()))
                .updatedAt(LocalDateTime.now())
                .build();

        int result = userMapper.updateById(updateUser);
        if (result > 0) {
            return "密码修改成功";
        } else {
            throw new RuntimeException("密码修改失败");
        }
    }

    /**
     * 获取用户列表（管理员功能）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 搜索关键词
     * @param roleId 角色ID筛选
     * @return 分页结果
     */
    @Override
    public PageResult<User> getUserList(Integer pageNum, Integer pageSize, String keyword, Long roleId) {
        // 创建分页对象
        Page<User> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        
        // 关键词搜索（用户名或真实姓名）
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
            );
        }

        // 角色筛选
        if (roleId != null) {
            query.eq(User::getRoleId, roleId);
        }

        // 执行分页查询
        IPage<User> pageResult = userMapper.selectPage(page, query);

        // 脱敏处理：移除密码字段
        List<User> users = pageResult.getRecords().stream()
                .map(user -> User.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .roleId(user.getRoleId())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .toList();

        return PageResult.of(pageResult.getTotal(), pageNum, pageSize, users);
    }

    /**
     * 根据ID获取用户详情
     * @param userId 用户ID
     * @return 用户详情（脱敏处理，不包含密码）
     */
    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 脱敏处理：不返回密码
        return User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleId(user.getRoleId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 更新用户信息（管理员功能）
     * @param userId 用户ID
     * @param request 更新信息
     * @return 更新结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateUser(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查邮箱是否已被其他用户使用
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, request.getEmail())
                    .ne(User::getId, userId);
            User existingEmail = userMapper.selectOne(emailQuery);
            if (existingEmail != null) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
        }

        // 更新用户信息
        User updateUser = User.builder()
                .id(userId)
                .realName(request.getRealName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .updatedAt(LocalDateTime.now())
                .build();

        int result = userMapper.updateById(updateUser);
        if (result > 0) {
            return "用户信息更新成功";
        } else {
            throw new RuntimeException("用户信息更新失败");
        }
    }

    /**
     * 删除用户（逻辑删除）
     * @param userId 用户ID
     * @return 删除结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // MyBatis-Plus逻辑删除（根据配置的deleted字段）
        int result = userMapper.deleteById(userId);
        if (result > 0) {
            return "用户删除成功";
        } else {
            throw new RuntimeException("用户删除失败");
        }
    }

    /**
     * 修改用户角色（管理员功能）
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 修改结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateUserRole(Long userId, Long roleId) {
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证角色是否存在
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 更新用户角色
        User updateUser = User.builder()
                .id(userId)
                .roleId(roleId)
                .updatedAt(LocalDateTime.now())
                .build();

        int result = userMapper.updateById(updateUser);
        if (result > 0) {
            return "用户角色修改成功";
        } else {
            throw new RuntimeException("用户角色修改失败");
        }
    }
}

