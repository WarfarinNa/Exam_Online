package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.security.RequirePermission;
import org.development.exam_online.service.QuestionService;
import org.development.exam_online.util.QuestionTypeUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "题目管理")
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@RequirePermission({"question:manage"})
public class QuestionController {

    private final QuestionService questionService;

    @Operation(
        summary = "创建题目")
    @PostMapping
    public Result<Question> createQuestion(@Valid @RequestBody Question question) {
        Question createdQuestion = questionService.createQuestion(question);
        return Result.success(createdQuestion);
    }

    @Operation(summary = "获取题目详情", description = "根据ID获取题目的详细信息")
    @GetMapping("/{questionId}")
    public Result<Question> getQuestionById(@PathVariable Long questionId) {
        Question question = questionService.getQuestionById(questionId);
        return Result.success(question);
    }

    @Operation(summary = "更新题目", description = "更新指定题目的信息")
    @PutMapping("/{questionId}")
    public Result<String> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody Question question) {
        String message = questionService.updateQuestion(questionId, question);
        return Result.success(message);
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{questionId}")
    public Result<String> deleteQuestion(@PathVariable Long questionId) {
        String message = questionService.deleteQuestion(questionId);
        return Result.success(message);
    }

    @Operation(summary = "批量删除题目")
    @DeleteMapping("/batch")
    public Result<String> deleteQuestions(@RequestBody List<Long> questionIds) {
        String message = questionService.deleteQuestions(questionIds);
        return Result.success(message);
    }

    @Operation(summary = "获取题目列表")
    @GetMapping
    public Result<PageResult<Question>> getQuestionList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long knowledgeId,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long createdBy) {
        PageResult<Question> result = questionService.getQuestionList(pageNum, pageSize, type, categoryId, knowledgeId, difficulty, keyword, createdBy);
        return Result.success(result);
    }

    @Operation(summary = "搜索题目")
    @GetMapping("/search")
    public Result<PageResult<Question>> searchQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long knowledgeId,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Question> result = questionService.searchQuestions(keyword, type, categoryId, knowledgeId, difficulty, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(
        summary = "批量导入题目")
    @PostMapping("/import")
    public Result<String> importQuestions(@RequestParam("file") MultipartFile file) {
        String message = questionService.importQuestions(file);
        return Result.success(message);
    }

    @Operation(
        summary = "下载题目导入模板")
    @GetMapping("/template/download")
    public void downloadTemplate(HttpServletResponse response) {
        questionService.exportTemplateFile(response);
    }

    @Operation(summary = "根据分类获取题目")
    @GetMapping("/category/{categoryId}")
    public Result<PageResult<Question>> getQuestionsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Question> result = questionService.getQuestionsByCategory(categoryId, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "根据题型获取题目")
    @GetMapping("/type/{type}")
    public Result<PageResult<Question>> getQuestionsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Question> result = questionService.getQuestionsByType(type, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取题目类型列表")
    @GetMapping("/types")
    public Result<List<Map<String, String>>> getQuestionTypes() {
        List<Map<String, String>> types = QuestionTypeUtil.getTypeList();
        return Result.success(types);
    }
}

