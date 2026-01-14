package org.development.exam_online.service;

import org.development.exam_online.common.PageResult;
import org.development.exam_online.dao.dto.ChangePasswordRequest;
import org.development.exam_online.dao.dto.UpdateProfileRequest;
import org.development.exam_online.dao.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取当前登录用户的个人信息
     * @param userId 用户ID（从token中解析）
     * @return 用户信息
     */
    User getProfile(Long userId);

    /**
     * 更新当前登录用户的个人信息
     * @param userId 用户ID（从token中解析）
     * @param request 更新信息
     * @return 更新结果消息
     */
    String updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改当前登录用户的密码
     * @param userId 用户ID（从token中解析）
     * @param request 密码修改请求
     * @return 修改结果消息
     */
    String changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 获取用户列表（管理员功能）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 搜索关键词（用户名、真实姓名）
     * @param roleId 角色ID筛选
     * @return 分页结果
     */
    PageResult<User> getUserList(Integer pageNum, Integer pageSize, String keyword, Long roleId);

    /**
     * 根据ID获取用户详情
     * @param userId 用户ID
     * @return 用户详情
     */
    User getUserById(Long userId);

    /**
     * 更新用户信息（管理员功能）
     * @param userId 用户ID
     * @param request 更新信息
     * @return 更新结果消息
     */
    String updateUser(Long userId, UpdateProfileRequest request);

    /**
     * 删除用户（逻辑删除）
     * @param userId 用户ID
     * @return 删除结果消息
     */
    String deleteUser(Long userId);

    /**
     * 修改用户角色（管理员功能）
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 修改结果消息
     */
    String updateUserRole(Long userId, Long roleId);
}

