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

@Tag(name = "判卷与成绩分析")
@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;

    @Operation(
        summary = "获取待阅卷记录")
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

    @Operation(
        summary = "获取考试所有答卷记录")
    @GetMapping("/exams/{examId}/records")
    @RequirePermission({"mark:manual","mark:auto","exam:manage"})
    public Result<List<Map<String, Object>>> getExamRecords(@PathVariable Long examId) {
        List<Map<String, Object>> result = gradingService.getExamRecords(examId);
        return Result.success(result);
    }

    @Operation(
        summary = "获取考试记录详情")
    @GetMapping("/records/{recordId}")
    @RequirePermission({"mark:manual","exam:manage"})
    public Result<Map<String, Object>> getExamRecordDetail(@PathVariable Long recordId) {
        Map<String, Object> result = gradingService.getExamRecordDetail(recordId);
        return Result.success(result);
    }

    @Operation(summary = "评分单个主观题")
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

    @Operation(summary = "批量评分")
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

    @Operation(
        summary = "自动判卷单个记录")
    @PostMapping("/records/{recordId}/auto-grade")
    @RequirePermission({"mark:auto","exam:manage"})
    public Result<String> autoGradeRecord(
            @PathVariable Long recordId,
            @RequestParam(defaultValue = "false") Boolean forceOverride) {
        gradingService.autoGradeRecord(recordId, forceOverride);
        return Result.success("自动判卷完成");
    }

    @Operation(
        summary = "获取考试记录得分信息")
    @GetMapping("/records/{recordId}/score")
    @RequirePermission({"mark:manual","mark:auto","exam:manage","score:view"})
    public Result<Map<String, Object>> getRecordScoreInfo(@PathVariable Long recordId) {
        Map<String, Object> result = gradingService.getRecordScoreInfo(recordId);
        return Result.success(result);
    }

    @Operation(
        summary = "批量自动判卷")
    @PostMapping("/exams/{examId}/auto-grade")
    @RequirePermission({"mark:auto","exam:manage"})
    public Result<Map<String, Object>> autoGradeExam(@PathVariable Long examId) {
        Map<String, Object> result = gradingService.autoGradeExam(examId);
        return Result.success(result);
    }

    @Operation(summary = "获取考试成绩统计")
    @GetMapping("/exams/{examId}/statistics")
    @RequirePermission({"mark:manual","mark:auto","exam:manage"})
    public Result<Map<String, Object>> getExamStatistics(@PathVariable Long examId) {
        Map<String, Object> result = gradingService.getExamStatistics(examId);
        return Result.success(result);
    }

    @Operation(
        summary = "确认评分（单个记录）")
    @PostMapping("/records/{recordId}/confirm")
    @RequirePermission({"mark:manual","exam:manage"})
    public Result<String> confirmGrading(@PathVariable Long recordId) {
        gradingService.confirmGrading(recordId);
        return Result.success("评分已确认");
    }

    @Operation(
        summary = "批量确认评分（整场考试）")
    @PostMapping("/exams/{examId}/confirm")
    @RequirePermission({"mark:manual","exam:manage"})
    public Result<Map<String, Object>> confirmExamGrading(@PathVariable Long examId) {
        int count = gradingService.confirmExamGrading(examId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("examId", examId);
        result.put("confirmedCount", count);
        result.put("message", "评分已确认，正在后台为" + count + "名学生生成AI分析报告");
        return Result.success(result);
    }

    @Operation(summary = "获取错题分析")
    @GetMapping("/exams/{examId}/wrong-analysis")
    @RequirePermission({"mark:manual","mark:auto","exam:manage"})
    public Result<List<Map<String, Object>>> getWrongQuestionAnalysis(@PathVariable Long examId) {
        List<Map<String, Object>> result = gradingService.getWrongQuestionAnalysis(examId);
        return Result.success(result);
    }

    @Operation(
        summary = "获取我的考试记录")
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

    @Operation(
        summary = "获取我的考试详情")
    @GetMapping("/my-records/{recordId}")
    @RequirePermission({"score:view"})
    public Result<Map<String, Object>> getStudentExamDetail(
            @PathVariable Long recordId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = gradingService.getStudentExamDetail(recordId, userId);
        return Result.success(result);
    }
    
    @Operation(
        summary = "获取考试记录的AI分析报告")
    @GetMapping("/records/{recordId}/ai-report")
    @RequirePermission({"score:view", "exam:manage"})
    public Result<Map<String, Object>> getExamRecordAiReport(@PathVariable Long recordId) {
        Map<String, Object> result = gradingService.getExamRecordAiReport(recordId);
        return Result.success(result);
    }
}
