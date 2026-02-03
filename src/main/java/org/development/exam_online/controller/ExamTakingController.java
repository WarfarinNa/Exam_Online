package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.Result;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.security.RequirePermission;
import org.development.exam_online.service.ExamTakingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 在线考试控制器
 * 提供学生参加考试的相关功能
 */
@Tag(name = "在线考试", description = "在线考试接口（开始考试、答题、提交等）")
@RestController
@RequestMapping("/api/exam-taking")
@RequiredArgsConstructor
@RequirePermission({"exam:participate"})
public class ExamTakingController {

    private final ExamTakingService examTakingService;

    /**
     * 开始考试
     * @param examId 考试ID
     * @param authorization Authorization header
     * @return 考试信息
     */
    @Operation(summary = "开始考试", description = "学生开始考试，创建考试记录")
    @PostMapping("/{examId}/start")
    public Result<Map<String, Object>> startExam(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.startExam(examId, userId);
        return Result.success(result);
    }

    /**
     * 获取考试状态
     * @param examId 考试ID
     * @param authorization Authorization header
     * @return 考试状态信息
     */
    @Operation(summary = "获取考试状态", description = "获取当前用户的考试状态（是否已开始、剩余时间等）")
    @GetMapping("/{examId}/status")
    public Result<Map<String, Object>> getExamStatus(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.getExamStatus(examId, userId);
        return Result.success(result);
    }

    /**
     * 获取考试题目列表（不含答案）
     * @param examId 考试ID
     * @param authorization Authorization header
     * @return 题目列表（包含已保存的答案）
     */
    @Operation(summary = "获取考试题目", description = "获取考试题目列表（不含正确答案，包含已保存的答案）")
    @GetMapping("/{examId}/questions")
    public Result<Map<String, Object>> getExamQuestions(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.getExamQuestions(examId, userId);
        return Result.success(result);
    }

    /**
     * 保存单个题目的答案
     * @param examId 考试ID
     * @param questionId 题目ID
     * @param request 请求体（包含答案）
     * @param authorization Authorization header
     * @return 保存结果
     */
    @Operation(summary = "保存答案", description = "实时保存单个题目的答案")
    @PostMapping("/{examId}/save-answer/{questionId}")
    public Result<String> saveAnswer(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        String answer = request.get("answer");
        if (answer == null) {
            answer = ""; // 允许清空答案
        }
        examTakingService.saveAnswer(examId, userId, questionId, answer);
        return Result.success("答案保存成功");
    }

    /**
     * 批量保存答案
     * @param examId 考试ID
     * @param request 请求体（包含答案Map：questionId -> answer）
     * @param authorization Authorization header
     * @return 保存结果
     */
    @Operation(summary = "批量保存答案", description = "批量保存多个题目的答案（用于一次性保存）")
    @PostMapping("/{examId}/save-answers")
    public Result<String> saveAnswers(
            @PathVariable Long examId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        
        @SuppressWarnings("unchecked")
        Map<String, String> answersMap = (Map<String, String>) request.get("answers");
        if (answersMap == null) {
            return Result.success("没有需要保存的答案");
        }

        // 转换为Long -> String的Map
        Map<Long, String> answers = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : answersMap.entrySet()) {
            try {
                Long questionId = Long.parseLong(entry.getKey());
                answers.put(questionId, entry.getValue() != null ? entry.getValue() : "");
            } catch (NumberFormatException e) {
                // 跳过无效的questionId
            }
        }

        examTakingService.saveAnswers(examId, userId, answers);
        return Result.success("答案批量保存成功");
    }

    /**
     * 记录切屏行为
     * @param examId 考试ID
     * @param request 请求体（包含切屏类型）
     * @param authorization Authorization header
     * @return 记录结果
     */
    @Operation(summary = "记录切屏", description = "记录学生的切屏行为（用于防作弊）")
    @PostMapping("/{examId}/cheat-log")
    public Result<String> logCheat(
            @PathVariable Long examId,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        String cheatType = request.get("cheatType");
        if (cheatType == null || cheatType.isEmpty()) {
            cheatType = "UNKNOWN";
        }
        examTakingService.logCheat(examId, userId, cheatType);
        return Result.success("切屏行为已记录");
    }

    /**
     * 获取剩余时间（秒）
     * @param examId 考试ID
     * @param authorization Authorization header
     * @return 剩余时间
     */
    @Operation(summary = "获取剩余时间", description = "获取考试剩余时间（秒），用于前端倒计时")
    @GetMapping("/{examId}/remaining-time")
    public Result<Map<String, Object>> getRemainingTime(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Long remainingTime = examTakingService.getRemainingTime(examId, userId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("remainingTime", remainingTime);
        return Result.success(result);
    }

    /**
     * 提交考试
     * @param examId 考试ID
     * @param authorization Authorization header
     * @return 提交结果（包含自动判卷信息）
     */
    @Operation(summary = "提交考试", description = "提交考试并自动判卷（客观题）")
    @PostMapping("/{examId}/submit")
    public Result<Map<String, Object>> submitExam(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.submitExam(examId, userId);
        return Result.success(result);
    }

    /**
     * 恢复考试（用于断网恢复、刷新页面等）
     * @param examId 考试ID
     * @param authorization Authorization header
     * @return 考试信息（包含题目、已保存答案、剩余时间等）
     */
    @Operation(summary = "恢复考试", description = "恢复考试状态（用于断网恢复、刷新页面），返回题目和已保存答案")
    @GetMapping("/{examId}/continue")
    public Result<Map<String, Object>> continueExam(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.continueExam(examId, userId);
        return Result.success(result);
    }
}
