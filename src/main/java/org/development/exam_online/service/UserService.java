package org.development.exam_online.service;

import org.development.exam_online.common.PageResult;
import org.development.exam_online.dao.dto.ChangePasswordRequest;
import org.development.exam_online.dao.dto.UpdateProfileRequest;
import org.development.exam_online.dao.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    User getProfile(Long userId);

    String updateProfile(Long userId, UpdateProfileRequest request);

    String changePassword(Long userId, ChangePasswordRequest request);

    PageResult<User> getUserList(Integer pageNum, Integer pageSize, String keyword, Long roleId);

    User getUserById(Long userId);

    String updateUser(Long userId, UpdateProfileRequest request);

    String deleteUser(Long userId);

    String updateUserRole(Long userId, Long roleId);
}

