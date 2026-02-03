package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.Result;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.security.RequirePermission;
import org.development.exam_online.service.GradingService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 判卷与成绩分析控制器
 * 提供手动判卷、成绩查询、统计分析等功能
 */
@Tag(name = "判卷与成绩分析", description = "判卷与成绩分析接口（手动判卷、成绩查询、统计分析等）")
@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;

    /**
     * 获取待阅卷的考试记录列表（教师端）
     * @param examId 考试ID（可选，null表示查询所有考试）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 待阅卷的考试记录列表
     */
    @Operation(summary = "获取待阅卷记录", description = "教师端获取待阅卷的考试记录列表，支持按考试ID筛选")
    @GetMapping("/pending")
    @RequirePermission({"mark:manual","mark:auto","exam:manage"})
    public Result<PageResult<Map<String, Object>>> getPendingGradingRecords(
            @RequestParam(required = false) Long examId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Map<String, Object>> result = gradingService.getPendingGradingRecords(
                examId, Long.valueOf(pageNum), Long.valueOf(pageSize));
        return Result.success(result);
    }

    /**
     * 获取单个考试记录的详细信息（用于阅卷）
     * @param recordId 考试记录ID
     * @return 考试记录详情（包含题目、学生答案、正确答案、得分等）
     */
    @Operation(summary = "获取考试记录详情", description = "获取单个考试记录的详细信息，用于教师阅卷")
    @GetMapping("/records/{recordId}")
    @RequirePermission({"mark:manual","exam:manage"})
    public Result<Map<String, Object>> getExamRecordDetail(@PathVariable Long recordId) {
        Map<String, Object> result = gradingService.getExamRecordDetail(recordId);
        return Result.success(result);
    }

    /**
     * 对单个主观题进行评分
     * @param recordId 考试记录ID
     * @param questionId 题目ID
     * @param request 请求体（包含得分）
     * @return 评分结果
     */
    @Operation(summary = "评分单个主观题", description = "对单个主观题进行评分，更新得分")
    @PostMapping("/records/{recordId}/questions/{questionId}/grade")
    @RequirePermission({"mark:manual","exam:manage"})
    public Result<String> gradeQuestion(
            @PathVariable Long recordId,
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> request) {
        BigDecimal score = new BigDecimal(request.get("score").toString());
        gradingService.gradeQuestion(recordId, questionId, score);
        return Result.success("评分成功");
    }

    /**
     * 批量评分（一次给多个主观题评分）
     * @param recordId 考试记录ID
     * @param request 请求体（包含评分Map：questionId -> score）
     * @return 评分结果
     */
    @Operation(summary = "批量评分", description = "批量对多个主观题进行评分")
    @PostMapping("/records/{recordId}/grade-all")
    @RequirePermission({"mark:manual","exam:manage"})
    public Result<String> gradeQuestions(
            @PathVariable Long recordId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> scoresMap = (Map<String, Object>) request.get("scores");
        if (scoresMap == null) {
            return Result.success("没有需要评分的题目");
        }

        // 转换为Long -> BigDecimal的Map
        Map<Long, BigDecimal> scores = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : scoresMap.entrySet()) {
            try {
                Long questionId = Long.parseLong(entry.getKey());
                BigDecimal score = new BigDecimal(entry.getValue().toString());
                scores.put(questionId, score);
            } catch (NumberFormatException e) {
                // 跳过无效的questionId
            }
        }

        gradingService.gradeQuestions(recordId, scores);
        return Result.success("批量评分成功");
    }

    /**
     * 手动触发自动判卷（客观题）
     * @param recordId 考试记录ID
     * @return 判卷结果
     */
    @Operation(summary = "自动判卷单个记录", description = "对单个考试记录进行自动判卷（客观题）")
    @PostMapping("/records/{recordId}/auto-grade")
    @RequirePermission({"mark:auto","exam:manage"})
    public Result<String> autoGradeRecord(@PathVariable Long recordId) {
        gradingService.autoGradeRecord(recordId);
        return Result.success("自动判卷完成");
    }

    /**
     * 批量自动判卷（对某个考试的所有记录）
     * @param examId 考试ID
     * @return 判卷结果统计
     */
    @Operation(summary = "批量自动判卷", description = "对指定考试的所有记录进行自动判卷（客观题），返回统计信息")
    @PostMapping("/exams/{examId}/auto-grade")
    @RequirePermission({"mark:auto","exam:manage"})
    public Result<Map<String, Object>> autoGradeExam(@PathVariable Long examId) {
        Map<String, Object> result = gradingService.autoGradeExam(examId);
        return Result.success(result);
    }

    /**
     * 获取考试成绩统计（教师端）
     * @param examId 考试ID
     * @return 统计信息（分数分布、平均分、最高分、最低分、及格率等）
     */
    @Operation(summary = "获取考试成绩统计", description = "获取考试的统计信息（分数分布、平均分、最高分、最低分、及格率等）")
    @GetMapping("/exams/{examId}/statistics")
    @RequirePermission({"mark:manual","mark:auto","exam:manage"})
    public Result<Map<String, Object>> getExamStatistics(@PathVariable Long examId) {
        Map<String, Object> result = gradingService.getExamStatistics(examId);
        return Result.success(result);
    }

    /**
     * 获取错题分析（教师端）
     * @param examId 考试ID
     * @return 错题统计（按题目统计错误率、错误次数等）
     */
    @Operation(summary = "获取错题分析", description = "获取考试的错题分析（按题目统计错误率、错误次数等）")
    @GetMapping("/exams/{examId}/wrong-analysis")
    @RequirePermission({"mark:manual","mark:auto","exam:manage"})
    public Result<List<Map<String, Object>>> getWrongQuestionAnalysis(@PathVariable Long examId) {
        List<Map<String, Object>> result = gradingService.getWrongQuestionAnalysis(examId);
        return Result.success(result);
    }

    /**
     * 获取学生的考试记录列表（学生端）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param authorization Authorization header
     * @return 考试记录列表
     */
    @Operation(summary = "获取我的考试记录", description = "学生端获取自己的考试记录列表")
    @GetMapping("/my-records")
    @RequirePermission({"score:view"})
    public Result<PageResult<Map<String, Object>>> getStudentRecords(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        PageResult<Map<String, Object>> result = gradingService.getStudentRecords(
                userId, Long.valueOf(pageNum), Long.valueOf(pageSize));
        return Result.success(result);
    }

    /**
     * 获取单个学生的考试详情（学生端查看成绩）
     * @param recordId 考试记录ID
     * @param authorization Authorization header
     * @return 考试详情（包含题目、答案、得分、正确答案等）
     */
    @Operation(summary = "获取我的考试详情", description = "学生端查看自己的考试详情（包含题目、答案、得分、正确答案等）")
    @GetMapping("/my-records/{recordId}")
    @RequirePermission({"score:view"})
    public Result<Map<String, Object>> getStudentExamDetail(
            @PathVariable Long recordId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = gradingService.getStudentExamDetail(recordId, userId);
        return Result.success(result);
    }
}
