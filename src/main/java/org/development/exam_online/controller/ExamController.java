package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.entity.Exam;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.security.RequirePermission;
import org.development.exam_online.service.ExamService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "考试管理", description = "考试管理接口（考试时间设置、权限范围控制、试卷发布等）")
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @Operation(summary = "创建考试")
    @PostMapping
    @RequirePermission({"exam:manage"})
    public Result<Exam> createExam(@Valid @RequestBody Exam exam) {
        Exam createdExam = examService.createExam(exam);
        return Result.success(createdExam);
    }
    @Operation(summary = "发布试卷")
    @PostMapping("/publish/{paperId}")
    @RequirePermission({"exam:manage"})
    public Result<Exam> publishPaper(
            @PathVariable Long paperId,
            @Valid @RequestBody Exam exam) {
        Exam createdExam = examService.publishPaper(paperId, exam);
        return Result.success(createdExam);
    }

    @Operation(summary = "获取考试详情")
    @GetMapping("/{examId}")
    public Result<Exam> getExamById(@PathVariable Long examId) {
        Exam exam = examService.getExamById(examId);
        return Result.success(exam);
    }

    @Operation(summary = "更新考试信息")
    @PutMapping("/{examId}")
    @RequirePermission({"exam:manage"})
    public Result<String> updateExam(
            @PathVariable Long examId,
            @Valid @RequestBody Exam exam) {
        String message = examService.updateExam(examId, exam);
        return Result.success(message);
    }

    @Operation(summary = "设置考试时间")
    @PutMapping("/{examId}/time")
    @RequirePermission({"exam:manage"})
    public Result<String> setExamTime(
            @PathVariable Long examId,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        String message = examService.setExamTime(examId, startTime, endTime);
        return Result.success(message);
    }

    @Operation(summary = "设置考试权限")
    @PutMapping("/{examId}/permissions")
    @RequirePermission({"exam:manage"})
    public Result<String> setExamPermissions(
            @PathVariable Long examId,
            @RequestBody List<String> allowRoles) {
        String message = examService.setExamPermissions(examId, allowRoles);
        return Result.success(message);
    }

    @Operation(summary = "删除考试")
    @DeleteMapping("/{examId}")
    @RequirePermission({"exam:manage"})
    public Result<String> deleteExam(@PathVariable Long examId) {
        String message = examService.deleteExam(examId);
        return Result.success(message);
    }

    @Operation(summary = "获取考试列表")
    @GetMapping
    @RequirePermission({"exam:manage"})
    public Result<PageResult<Exam>> getExamList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long paperId,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        PageResult<Exam> result = examService.getExamList(pageNum, pageSize, keyword, paperId, startTime, endTime);
        return Result.success(result);
    }

    @Operation(summary = "获取已发布考试列表")
    @GetMapping("/published")
    public Result<PageResult<Exam>> getPublishedExams(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Exam> result = examService.getPublishedExams(pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(
        summary = "获取我创建的考试")
    @GetMapping("/my-exams")
    @RequirePermission({"exam:manage"})
    public Result<PageResult<Map<String, Object>>> getMyExams(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        PageResult<Map<String, Object>> result = examService.getMyExams(userId, pageNum, pageSize, keyword);
        return Result.success(result);
    }

    @Operation(summary = "取消发布考试")
    @PutMapping("/{examId}/unpublish")
    @RequirePermission({"exam:manage"})
    public Result<String> unpublishExam(@PathVariable Long examId) {
        String message = examService.unpublishExam(examId);
        return Result.success(message);
    }


    @Operation(summary = "获取考试统计")
    @GetMapping("/{examId}/statistics")
    @RequirePermission({"exam:manage"})
    public Result<Map<String, Object>> getExamStatistics(@PathVariable Long examId) {
        Map<String, Object> statistics = examService.getExamStatistics(examId);
        return Result.success(statistics);
    }
}

