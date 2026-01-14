package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.entity.QuestionCategory;
import org.development.exam_online.service.QuestionCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目分类管理控制器
 * 管理题目的分类信息，为题目提供分类管理功能
 */
@Tag(name = "题目分类管理", description = "题目分类管理接口（支持分类的增删改查）")
@RestController
@RequestMapping("/api/question-categories")
@RequiredArgsConstructor
public class QuestionCategoryController {

    private final QuestionCategoryService questionCategoryService;

    /**
     * 创建题目分类
     * @param category 分类信息（名称、描述）
     * @return 创建结果
     */
    @Operation(summary = "创建题目分类", description = "创建新的题目分类")
    @PostMapping
    public Result<QuestionCategory> createCategory(@Valid @RequestBody QuestionCategory category) {
        QuestionCategory createdCategory = questionCategoryService.createCategory(category);
        return Result.success(createdCategory);
    }

    /**
     * 获取所有题目分类列表
     * @return 分类列表
     */
    @Operation(summary = "获取分类列表", description = "获取所有题目分类列表")
    @GetMapping
    public Result<List<QuestionCategory>> getCategoryList() {
        List<QuestionCategory> categories = questionCategoryService.getCategoryList();
        return Result.success(categories);
    }

    /**
     * 根据ID获取分类详情
     * @param categoryId 分类ID
     * @return 分类详情
     */
    @Operation(summary = "获取分类详情", description = "根据ID获取题目分类的详细信息")
    @GetMapping("/{categoryId}")
    public Result<QuestionCategory> getCategoryById(@PathVariable Long categoryId) {
        QuestionCategory category = questionCategoryService.getCategoryById(categoryId);
        return Result.success(category);
    }

    /**
     * 更新题目分类
     * @param categoryId 分类ID
     * @param category 分类信息
     * @return 更新结果
     */
    @Operation(summary = "更新题目分类", description = "更新指定题目分类的信息")
    @PutMapping("/{categoryId}")
    public Result<String> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody QuestionCategory category) {
        String message = questionCategoryService.updateCategory(categoryId, category);
        return Result.success(message);
    }

    /**
     * 删除题目分类
     * @param categoryId 分类ID
     * @return 删除结果
     */
    @Operation(summary = "删除题目分类", description = "删除指定题目分类（需要检查该分类下是否有题目）")
    @DeleteMapping("/{categoryId}")
    public Result<String> deleteCategory(@PathVariable Long categoryId) {
        String message = questionCategoryService.deleteCategory(categoryId);
        return Result.success(message);
    }

    /**
     * 获取分类下的题目数量
     * @param categoryId 分类ID
     * @return 题目数量
     */
    @Operation(summary = "获取分类题目数量", description = "获取指定分类下的题目数量")
    @GetMapping("/{categoryId}/count")
    public Result<Long> getQuestionCountByCategory(@PathVariable Long categoryId) {
        Long count = questionCategoryService.getQuestionCountByCategory(categoryId);
        return Result.success(count);
    }
}

