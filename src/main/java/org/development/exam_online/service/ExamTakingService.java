package org.development.exam_online.service;

import java.util.List;
import java.util.Map;

/**
 * 在线考试服务接口
 * 提供学生参加考试的相关功能
 */
public interface ExamTakingService {

    /**
     * 开始考试
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 考试信息（包含题目数量、总时长等）
     */
    Map<String, Object> startExam(Long examId, Long userId);

    /**
     * 获取考试状态
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 考试状态信息（是否已开始、剩余时间等）
     */
    Map<String, Object> getExamStatus(Long examId, Long userId);

    /**
     * 获取考试题目列表（不含答案）
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 题目列表（包含已保存的答案）
     */
    Map<String, Object> getExamQuestions(Long examId, Long userId);

    /**
     * 保存单个题目的答案
     * @param examId 考试ID
     * @param userId 用户ID
     * @param questionId 题目ID
     * @param answer 答案
     */
    void saveAnswer(Long examId, Long userId, Long questionId, String answer);

    /**
     * 批量保存答案
     * @param examId 考试ID
     * @param userId 用户ID
     * @param answers 答案Map（questionId -> answer）
     */
    void saveAnswers(Long examId, Long userId, Map<Long, String> answers);

    /**
     * 记录切屏行为
     * @param examId 考试ID
     * @param userId 用户ID
     * @param cheatType 切屏类型（如：SWITCH_TAB, COPY, PASTE等）
     */
    void logCheat(Long examId, Long userId, String cheatType);

    /**
     * 获取剩余时间（秒）
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 剩余时间（秒），如果考试已结束或未开始则返回0
     */
    Long getRemainingTime(Long examId, Long userId);

    /**
     * 提交考试
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 提交结果（包含自动判卷信息）
     */
    Map<String, Object> submitExam(Long examId, Long userId);

    /**
     * 恢复考试（用于断网恢复、刷新页面等）
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 考试信息（包含题目、已保存答案、剩余时间等）
     */
    Map<String, Object> continueExam(Long examId, Long userId);
}
