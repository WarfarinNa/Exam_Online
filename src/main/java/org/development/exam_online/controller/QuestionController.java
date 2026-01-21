package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.service.QuestionService;
import org.development.exam_online.util.QuestionTypeUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 题目管理控制器
 * 支持单选题、多选题、判断题、填空题、简答题等多题型的录入、编辑、删除与搜索
 */
@Tag(name = "题目管理", description = "题目管理接口（支持多题型录入、编辑、删除、搜索）")
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 创建题目
     * @param question 题目信息（包含题型、内容、选项、答案、分值、分类等）
     * @return 创建结果
     */
    @Operation(summary = "创建题目", description = "创建新题目，支持单选题、多选题、判断题、填空题、简答题等")
    @PostMapping
    public Result<Question> createQuestion(@Valid @RequestBody Question question) {
        Question createdQuestion = questionService.createQuestion(question);
        return Result.success(createdQuestion);
    }

    /**
     * 根据ID获取题目详情
     * @param questionId 题目ID
     * @return 题目详情
     */
    @Operation(summary = "获取题目详情", description = "根据ID获取题目的详细信息")
    @GetMapping("/{questionId}")
    public Result<Question> getQuestionById(@PathVariable Long questionId) {
        Question question = questionService.getQuestionById(questionId);
        return Result.success(question);
    }

    /**
     * 更新题目
     * @param questionId 题目ID
     * @param question 题目信息
     * @return 更新结果
     */
    @Operation(summary = "更新题目", description = "更新指定题目的信息")
    @PutMapping("/{questionId}")
    public Result<String> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody Question question) {
        String message = questionService.updateQuestion(questionId, question);
        return Result.success(message);
    }

    /**
     * 删除题目
     * @param questionId 题目ID
     * @return 删除结果
     */
    @Operation(summary = "删除题目", description = "删除指定题目")
    @DeleteMapping("/{questionId}")
    public Result<String> deleteQuestion(@PathVariable Long questionId) {
        String message = questionService.deleteQuestion(questionId);
        return Result.success(message);
    }

    /**
     * 批量删除题目
     * @param questionIds 题目ID列表
     * @return 删除结果
     */
    @Operation(summary = "批量删除题目", description = "批量删除多个题目")
    @DeleteMapping("/batch")
    public Result<String> deleteQuestions(@RequestBody List<Long> questionIds) {
        String message = questionService.deleteQuestions(questionIds);
        return Result.success(message);
    }

    /**
     * 获取题目列表（支持分页和搜索）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param type 题型代码筛选（参考QuestionType枚举：single、multiple、judge、blank、short）
     * @param categoryId 分类ID筛选
     * @param keyword 搜索关键词（题目内容）
     * @param createdBy 创建者ID筛选
     * @return 题目列表
     */
    @Operation(summary = "获取题目列表", description = "获取题目列表，支持分页、题型筛选、分类筛选和关键词搜索")
    @GetMapping
    public Result<PageResult<Question>> getQuestionList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long createdBy) {
        PageResult<Question> result = questionService.getQuestionList(pageNum, pageSize, type, categoryId, keyword, createdBy);
        return Result.success(result);
    }

    /**
     * 搜索题目
     * @param keyword 搜索关键词
     * @param type 题型代码筛选（参考QuestionType枚举：single、multiple、judge、blank、short）
     * @param categoryId 分类ID筛选
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 搜索结果
     */
    @Operation(summary = "搜索题目", description = "根据关键词、题型、分类等条件搜索题目")
    @GetMapping("/search")
    public Result<PageResult<Question>> searchQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Question> result = questionService.searchQuestions(keyword, type, categoryId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 批量导入题目
     * @param file Excel文件
     * @return 导入结果
     */
    @Operation(summary = "批量导入题目", description = "通过Excel文件批量导入题目")
    @PostMapping("/import")
    public Result<String> importQuestions(@RequestParam("file") MultipartFile file) {
        String message = questionService.importQuestions(file);
        return Result.success(message);
    }

    /**
     * 导出题目模板
     * @return 导出结果
     */
    @Operation(summary = "导出题目模板", description = "导出Excel题目导入模板")
    @GetMapping("/template")
    public Result<String> exportTemplate() {
        String message = questionService.exportTemplate();
        return Result.success(message);
    }

    /**
     * 根据分类获取题目列表
     * @param categoryId 分类ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 题目列表
     */
    @Operation(summary = "根据分类获取题目", description = "根据题目分类获取该分类下的所有题目")
    @GetMapping("/category/{categoryId}")
    public Result<PageResult<Question>> getQuestionsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Question> result = questionService.getQuestionsByCategory(categoryId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 根据题型获取题目列表
     * @param type 题型代码（参考QuestionType枚举：single、multiple、judge、blank、short）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 题目列表
     */
    @Operation(summary = "根据题型获取题目", description = "根据题型代码获取题目列表（single-单选题、multiple-多选题、judge-判断题、blank-填空题、short-简答题）")
    @GetMapping("/type/{type}")
    public Result<PageResult<Question>> getQuestionsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Question> result = questionService.getQuestionsByType(type, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取所有题目类型列表
     * @return 题目类型列表（包含code和label）
     */
    @Operation(summary = "获取题目类型列表", description = "获取所有可用的题目类型（基于QuestionType枚举）")
    @GetMapping("/types")
    public Result<List<Map<String, String>>> getQuestionTypes() {
        List<Map<String, String>> types = QuestionTypeUtil.getTypeList();
        return Result.success(types);
    }
}

