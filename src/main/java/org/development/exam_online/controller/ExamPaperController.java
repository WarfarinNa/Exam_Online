package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.dto.AutoGeneratePaperRule;
import org.development.exam_online.dao.entity.ExamPaper;
import org.development.exam_online.service.ExamPaperService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 试卷管理控制器
 * 提供手动组卷与自动组卷两种方式，支持试卷预览、发布、考试时间设置和权限范围控制
 */
@Tag(name = "试卷管理", description = "试卷管理接口（手动组卷、自动组卷、试卷预览、发布等）")
@RestController
@RequestMapping("/api/exam-papers")
@RequiredArgsConstructor
public class ExamPaperController {

    private final ExamPaperService examPaperService;

    /**
     * 创建试卷（手动组卷）
     * @param examPaper 试卷基本信息
     * @return 创建的试卷
     */
    @Operation(summary = "创建试卷", description = "创建新试卷，手动组卷方式")
    @PostMapping
    public Result<ExamPaper> createPaper(@Valid @RequestBody ExamPaper examPaper) {
        ExamPaper createdPaper = examPaperService.createPaper(examPaper);
        return Result.success(createdPaper);
    }

    /**
     * 手动组卷 - 添加题目到试卷
     * @param paperId 试卷ID
     * @param questionIds 题目ID列表
     * @param scores 对应题目的分值列表（可选，如果不提供则使用题目的默认分值）
     * @return 添加结果
     */
    @Operation(summary = "手动组卷-添加题目", description = "教师自主选择题目添加到试卷中")
    @PostMapping("/{paperId}/questions")
    public Result<String> addQuestionsToPaper(
            @PathVariable Long paperId,
            @RequestBody List<Long> questionIds,
            @RequestParam(required = false) List<Double> scores) {
        String message = examPaperService.addQuestionsToPaper(paperId, questionIds, scores);
        return Result.success(message);
    }

    /**
     * 手动组卷 - 移除试卷中的题目
     * @param paperId 试卷ID
     * @param questionIds 要移除的题目ID列表
     * @return 移除结果
     */
    @Operation(summary = "手动组卷-移除题目", description = "从试卷中移除指定的题目")
    @DeleteMapping("/{paperId}/questions")
    public Result<String> removeQuestionsFromPaper(
            @PathVariable Long paperId,
            @RequestBody List<Long> questionIds) {
        String message = examPaperService.removeQuestionsFromPaper(paperId, questionIds);
        return Result.success(message);
    }

    /**
     * 自动组卷
     * @param paperId 试卷ID（已创建的试卷）
     * @param rule 自动组卷规则（题型、难度比例、题量等）
     * @return 组卷结果
     */
    @Operation(summary = "自动组卷", description = "依据题型、难度比例、题量等规则自动生成试卷")
    @PostMapping("/{paperId}/auto-generate")
    public Result<String> autoGeneratePaper(
            @PathVariable Long paperId,
            @Valid @RequestBody AutoGeneratePaperRule rule) {
        String message = examPaperService.autoGeneratePaper(paperId, rule);
        return Result.success(message);
    }

    /**
     * 根据ID获取试卷详情
     * @param paperId 试卷ID
     * @return 试卷详情（包含题目列表）
     */
    @Operation(summary = "获取试卷详情", description = "根据ID获取试卷详细信息，包含试卷中的所有题目")
    @GetMapping("/{paperId}")
    public Result<Map<String, Object>> getPaperById(@PathVariable Long paperId) {
        Map<String, Object> result = examPaperService.getPaperById(paperId);
        return Result.success(result);
    }

    /**
     * 预览试卷
     * @param paperId 试卷ID
     * @return 试卷预览信息（包含题目内容，但不包含答案）
     */
    @Operation(summary = "预览试卷", description = "预览试卷内容，显示题目但不显示答案，用于教师查看")
    @GetMapping("/{paperId}/preview")
    public Result<Map<String, Object>> previewPaper(@PathVariable Long paperId) {
        Map<String, Object> result = examPaperService.previewPaper(paperId);
        return Result.success(result);
    }

    /**
     * 更新试卷基本信息
     * @param paperId 试卷ID
     * @param examPaper 试卷信息
     * @return 更新结果
     */
    @Operation(summary = "更新试卷信息", description = "更新试卷的基本信息（名称、描述、时长等）")
    @PutMapping("/{paperId}")
    public Result<String> updatePaper(
            @PathVariable Long paperId,
            @Valid @RequestBody ExamPaper examPaper) {
        String message = examPaperService.updatePaper(paperId, examPaper);
        return Result.success(message);
    }

    /**
     * 删除试卷
     * @param paperId 试卷ID
     * @return 删除结果
     */
    @Operation(summary = "删除试卷", description = "删除指定试卷（需要检查是否有关联的考试）")
    @DeleteMapping("/{paperId}")
    public Result<String> deletePaper(@PathVariable Long paperId) {
        String message = examPaperService.deletePaper(paperId);
        return Result.success(message);
    }

    /**
     * 获取试卷列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 搜索关键词（试卷名称）
     * @param type 试卷类型筛选
     * @param createdBy 创建者ID筛选
     * @return 试卷列表
     */
    @Operation(summary = "获取试卷列表", description = "获取试卷列表，支持分页、搜索和筛选")
    @GetMapping
    public Result<PageResult<ExamPaper>> getPaperList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long createdBy) {
        PageResult<ExamPaper> result = examPaperService.getPaperList(pageNum, pageSize, keyword, type, createdBy);
        return Result.success(result);
    }

    /**
     * 获取试卷中的题目列表
     * @param paperId 试卷ID
     * @return 题目列表
     */
    @Operation(summary = "获取试卷题目列表", description = "获取指定试卷中的所有题目")
    @GetMapping("/{paperId}/questions")
    public Result<List<Map<String, Object>>> getPaperQuestions(@PathVariable Long paperId) {
        List<Map<String, Object>> questions = examPaperService.getPaperQuestions(paperId);
        return Result.success(questions);
    }

    /**
     * 计算试卷总分
     * @param paperId 试卷ID
     * @return 试卷总分
     */
    @Operation(summary = "计算试卷总分", description = "自动计算试卷的总分（所有题目分值的总和）")
    @GetMapping("/{paperId}/total-score")
    public Result<Double> calculateTotalScore(@PathVariable Long paperId) {
        Double totalScore = examPaperService.calculateTotalScore(paperId);
        return Result.success(totalScore);
    }

    /**
     * 清空试卷中的所有题目
     * @param paperId 试卷ID
     * @return 清空结果
     */
    @Operation(summary = "清空试卷题目", description = "清空试卷中的所有题目，用于重新组卷")
    @DeleteMapping("/{paperId}/questions/all")
    public Result<String> clearAllQuestions(@PathVariable Long paperId) {
        String message = examPaperService.clearAllQuestions(paperId);
        return Result.success(message);
    }
}

