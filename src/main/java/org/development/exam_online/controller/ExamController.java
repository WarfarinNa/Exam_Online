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

/**
 * 考试管理控制器
 * 管理考试信息，包括考试时间设置、权限范围控制、试卷发布等
 */
@Tag(name = "考试管理", description = "考试管理接口（考试时间设置、权限范围控制、试卷发布等）")
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    /**
     * 创建考试
     * @param exam 考试信息（包含试卷ID、名称、开始时间、结束时间、允许的角色等）
     * @return 创建的考试
     */
    @Operation(summary = "创建考试", description = "创建新考试，需要关联试卷，设置考试时间和权限范围")
    @PostMapping
    @RequirePermission({"exam:manage"})
    public Result<Exam> createExam(@Valid @RequestBody Exam exam) {
        Exam createdExam = examService.createExam(exam);
        return Result.success(createdExam);
    }

    /**
     * 发布试卷为考试
     * @param paperId 试卷ID
     * @param exam 考试信息
     * @return 创建的考试
     */
    @Operation(summary = "发布试卷", description = "将试卷发布为考试，设置考试时间和权限范围")
    @PostMapping("/publish/{paperId}")
    @RequirePermission({"exam:manage"})
    public Result<Exam> publishPaper(
            @PathVariable Long paperId,
            @Valid @RequestBody Exam exam) {
        Exam createdExam = examService.publishPaper(paperId, exam);
        return Result.success(createdExam);
    }

    /**
     * 根据ID获取考试详情
     * @param examId 考试ID
     * @return 考试详情
     */
    @Operation(summary = "获取考试详情", description = "根据ID获取考试的详细信息")
    @GetMapping("/{examId}")
    public Result<Exam> getExamById(@PathVariable Long examId) {
        Exam exam = examService.getExamById(examId);
        return Result.success(exam);
    }

    /**
     * 更新考试信息
     * @param examId 考试ID
     * @param exam 考试信息
     * @return 更新结果
     */
    @Operation(summary = "更新考试信息", description = "更新考试的基本信息（名称、时间、权限等）")
    @PutMapping("/{examId}")
    @RequirePermission({"exam:manage"})
    public Result<String> updateExam(
            @PathVariable Long examId,
            @Valid @RequestBody Exam exam) {
        String message = examService.updateExam(examId, exam);
        return Result.success(message);
    }

    /**
     * 设置考试时间
     * @param examId 考试ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 设置结果
     */
    @Operation(summary = "设置考试时间", description = "设置或修改考试的开始时间和结束时间")
    @PutMapping("/{examId}/time")
    @RequirePermission({"exam:manage"})
    public Result<String> setExamTime(
            @PathVariable Long examId,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        String message = examService.setExamTime(examId, startTime, endTime);
        return Result.success(message);
    }

    /**
     * 设置考试权限范围
     * @param examId 考试ID
     * @param allowRoles 允许的角色列表（角色ID列表或角色名称列表）
     * @return 设置结果
     */
    @Operation(summary = "设置考试权限", description = "设置考试允许参与的角色（如：学生、特定班级等）")
    @PutMapping("/{examId}/permissions")
    @RequirePermission({"exam:manage"})
    public Result<String> setExamPermissions(
            @PathVariable Long examId,
            @RequestBody List<String> allowRoles) {
        String message = examService.setExamPermissions(examId, allowRoles);
        return Result.success(message);
    }

    /**
     * 删除考试
     * @param examId 考试ID
     * @return 删除结果
     */
    @Operation(summary = "删除考试", description = "删除指定考试（需要检查是否有考试记录）")
    @DeleteMapping("/{examId}")
    @RequirePermission({"exam:manage"})
    public Result<String> deleteExam(@PathVariable Long examId) {
        String message = examService.deleteExam(examId);
        return Result.success(message);
    }

    /**
     * 获取考试列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 搜索关键词（考试名称）
     * @param paperId 试卷ID筛选
     * @param startTime 开始时间筛选（查询此时间之后的考试）
     * @param endTime 结束时间筛选（查询此时间之前的考试）
     * @return 考试列表
     */
    @Operation(summary = "获取考试列表", description = "获取考试列表，支持分页、搜索和时间筛选")
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

    /**
     * 获取已发布的考试列表（供学生查看）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 已发布的考试列表
     */
    @Operation(summary = "获取已发布考试列表", description = "获取当前时间在考试时间范围内的已发布考试列表")
    @GetMapping("/published")
    public Result<PageResult<Exam>> getPublishedExams(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Exam> result = examService.getPublishedExams(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取我创建的考试列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param authorization Authorization header
     * @return 我创建的考试列表
     */
    @Operation(summary = "获取我创建的考试", description = "获取当前登录用户创建的考试列表")
    @GetMapping("/my-exams")
    @RequirePermission({"exam:manage"})
    public Result<PageResult<Exam>> getMyExams(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        PageResult<Exam> result = examService.getMyExams(userId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 取消发布考试
     * @param examId 考试ID
     * @return 取消结果
     */
    @Operation(summary = "取消发布考试", description = "取消已发布的考试，学生将无法看到和参加该考试")
    @PutMapping("/{examId}/unpublish")
    @RequirePermission({"exam:manage"})
    public Result<String> unpublishExam(@PathVariable Long examId) {
        String message = examService.unpublishExam(examId);
        return Result.success(message);
    }

    /**
     * 获取考试的统计信息
     * @param examId 考试ID
     * @return 统计信息（参与人数、已完成人数等）
     */
    @Operation(summary = "获取考试统计", description = "获取考试的统计信息（参与人数、已完成人数、平均分等）")
    @GetMapping("/{examId}/statistics")
    @RequirePermission({"exam:manage"})
    public Result<Map<String, Object>> getExamStatistics(@PathVariable Long examId) {
        Map<String, Object> statistics = examService.getExamStatistics(examId);
        return Result.success(statistics);
    }
}

