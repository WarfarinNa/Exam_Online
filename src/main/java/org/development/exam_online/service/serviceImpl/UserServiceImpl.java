package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.dto.ChangePasswordRequest;
import org.development.exam_online.dao.dto.UpdateProfileRequest;
import org.development.exam_online.dao.entity.User;
import org.development.exam_online.dao.mapper.UserMapper;
import org.development.exam_online.service.UserService;
import org.development.exam_online.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public User getProfile(Long userId) {
        User user = requireActiveUser(userId);
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateProfile(Long userId, UpdateProfileRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "更新信息不能为空");
        }
        requireActiveUser(userId);

        User update = new User();
        update.setId(userId);
        update.setRealName(request.getRealName());
        update.setEmail(request.getEmail());
        update.setPhone(request.getPhone());

        int updated = userMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "更新失败");
        }
        return "更新成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String changePassword(Long userId, ChangePasswordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码修改信息不能为空");
        }
        User user = requireActiveUser(userId);

        if (!PasswordUtils.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR);
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(PasswordUtils.hashPassword(request.getNewPassword()));
        int updated = userMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "修改失败");
        }
        return "密码修改成功";
    }

    @Override
    public PageResult<User> getUserList(Integer pageNum, Integer pageSize, String keyword, Long roleId) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        if (roleId != null) {
            q.eq(User::getRoleId, roleId);
        }
        q.orderByDesc(User::getCreatedAt);

        Page<User> page = new Page<>(p, s);
        Page<User> result = userMapper.selectPage(page, q);
        result.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    public User getUserById(Long userId) {
        User user = requireActiveUser(userId);
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateUser(Long userId, UpdateProfileRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "更新信息不能为空");
        }
        requireActiveUser(userId);

        User update = new User();
        update.setId(userId);
        update.setRealName(request.getRealName());
        update.setEmail(request.getEmail());
        update.setPhone(request.getPhone());
        int updated = userMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "更新失败");
        }
        return "更新成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteUser(Long userId) {
        User user = requireActiveUser(userId);
        User update = new User();
        update.setId(user.getId());
        update.setDeleted(1);
        int updated = userMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "删除失败");
        }
        return "删除成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateUserRole(Long userId, Long roleId) {
        if (roleId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "roleId不能为空");
        }
        requireActiveUser(userId);

        User update = new User();
        update.setId(userId);
        update.setRoleId(roleId);
        int updated = userMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "修改失败");
        }
        return "角色修改成功";
    }

    private User requireActiveUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userId不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getDeleted(), 0)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}

