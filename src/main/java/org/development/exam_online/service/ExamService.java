package org.development.exam_online.service;

import org.development.exam_online.common.PageResult;
import org.development.exam_online.dao.entity.Exam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 考试服务接口
 */
public interface ExamService {

    /**
     * 创建考试
     * @param exam 考试信息
     * @return 创建的考试
     */
    Exam createExam(Exam exam);

    /**
     * 发布试卷为考试
     * @param paperId 试卷ID
     * @param exam 考试信息
     * @return 创建的考试
     */
    Exam publishPaper(Long paperId, Exam exam);

    /**
     * 根据ID获取考试详情
     * @param examId 考试ID
     * @return 考试详情
     */
    Exam getExamById(Long examId);

    /**
     * 更新考试信息
     * @param examId 考试ID
     * @param exam 考试信息
     * @return 更新结果消息
     */
    String updateExam(Long examId, Exam exam);

    /**
     * 设置考试时间
     * @param examId 考试ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 设置结果消息
     */
    String setExamTime(Long examId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 设置考试权限范围
     * @param examId 考试ID
     * @param allowRoles 允许的角色列表
     * @return 设置结果消息
     */
    String setExamPermissions(Long examId, List<String> allowRoles);

    /**
     * 删除考试
     * @param examId 考试ID
     * @return 删除结果消息
     */
    String deleteExam(Long examId);

    /**
     * 获取考试列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 搜索关键词
     * @param paperId 试卷ID筛选
     * @param startTime 开始时间筛选
     * @param endTime 结束时间筛选
     * @return 分页结果
     */
    PageResult<Exam> getExamList(Integer pageNum, Integer pageSize, String keyword, Long paperId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取已发布的考试列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResult<Exam> getPublishedExams(Integer pageNum, Integer pageSize);

    /**
     * 获取我创建的考试列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResult<Exam> getMyExams(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 取消发布考试
     * @param examId 考试ID
     * @return 取消结果消息
     */
    String unpublishExam(Long examId);

    /**
     * 获取考试的统计信息
     * @param examId 考试ID
     * @return 统计信息
     */
    Map<String, Object> getExamStatistics(Long examId);
}
