package org.development.exam_online.service;

import org.development.exam_online.common.PageResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 判卷与成绩分析服务接口
 * 提供手动判卷、成绩查询、统计分析等功能
 */
public interface GradingService {

    /**
     * 获取待阅卷的考试记录列表（教师端）
     * @param examId 考试ID（可选，null表示查询所有考试）
     * @param page 页码
     * @param size 每页数量
     * @return 待阅卷的考试记录列表
     */
    PageResult<Map<String, Object>> getPendingGradingRecords(Long examId, Long page, Long size);

    /**
     * 获取单个考试记录的详细信息（用于阅卷）
     * @param recordId 考试记录ID
     * @return 考试记录详情（包含题目、学生答案、正确答案、得分等）
     */
    Map<String, Object> getExamRecordDetail(Long recordId);

    /**
     * 对单个主观题进行评分
     * @param recordId 考试记录ID
     * @param questionId 题目ID
     * @param score 得分
     */
    void gradeQuestion(Long recordId, Long questionId, BigDecimal score);

    /**
     * 批量评分（一次给多个主观题评分）
     * @param recordId 考试记录ID
     * @param scores 评分Map（questionId -> score）
     */
    void gradeQuestions(Long recordId, Map<Long, BigDecimal> scores);

    /**
     * 手动触发自动判卷（客观题）
     * @param recordId 考试记录ID
     */
    void autoGradeRecord(Long recordId);

    /**
     * 批量自动判卷（对某个考试的所有记录）
     * @param examId 考试ID
     * @return 判卷结果统计
     */
    Map<String, Object> autoGradeExam(Long examId);

    /**
     * 获取考试成绩统计（教师端）
     * @param examId 考试ID
     * @return 统计信息（分数分布、平均分、最高分、最低分、及格率等）
     */
    Map<String, Object> getExamStatistics(Long examId);

    /**
     * 获取错题分析（教师端）
     * @param examId 考试ID
     * @return 错题统计（按题目统计错误率、错误次数等）
     */
    List<Map<String, Object>> getWrongQuestionAnalysis(Long examId);

    /**
     * 获取学生的考试记录列表（学生端）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 考试记录列表
     */
    PageResult<Map<String, Object>> getStudentRecords(Long userId, Long page, Long size);

    /**
     * 获取单个学生的考试详情（学生端查看成绩）
     * @param recordId 考试记录ID
     * @param userId 用户ID（用于权限验证）
     * @return 考试详情（包含题目、答案、得分、正确答案等）
     */
    Map<String, Object> getStudentExamDetail(Long recordId, Long userId);
}
