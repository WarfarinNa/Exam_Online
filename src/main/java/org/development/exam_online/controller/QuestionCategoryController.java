package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.entity.QuestionCategory;
import org.development.exam_online.security.RequirePermission;
import org.development.exam_online.service.QuestionCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "题目分类管理")
@RestController
@RequestMapping("/api/question-categories")
@RequiredArgsConstructor
@RequirePermission({"question:manage"})
public class QuestionCategoryController {

    private final QuestionCategoryService questionCategoryService;


    @Operation(summary = "创建题目分类")
    @PostMapping
    public Result<QuestionCategory> createCategory(@Valid @RequestBody QuestionCategory category) {
        QuestionCategory createdCategory = questionCategoryService.createCategory(category);
        return Result.success(createdCategory);
    }

    @Operation(summary = "获取分类列表")
    @GetMapping
    public Result<List<QuestionCategory>> getCategoryList() {
        List<QuestionCategory> categories = questionCategoryService.getCategoryList();
        return Result.success(categories);
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{categoryId}")
    public Result<QuestionCategory> getCategoryById(@PathVariable Long categoryId) {
        QuestionCategory category = questionCategoryService.getCategoryById(categoryId);
        return Result.success(category);
    }


    @Operation(summary = "更新题目分类")
    @PutMapping("/{categoryId}")
    public Result<String> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody QuestionCategory category) {
        String message = questionCategoryService.updateCategory(categoryId, category);
        return Result.success(message);
    }

    @Operation(
        summary = "删除题目分类")
    @DeleteMapping("/{categoryId}")
    public Result<String> deleteCategory(@PathVariable Long categoryId) {
        String message = questionCategoryService.deleteCategory(categoryId);
        return Result.success(message);
    }

    @Operation(summary = "获取分类题目数量")
    @GetMapping("/{categoryId}/count")
    public Result<Long> getQuestionCountByCategory(@PathVariable Long categoryId) {
        Long count = questionCategoryService.getQuestionCountByCategory(categoryId);
        return Result.success(count);
    }
}

