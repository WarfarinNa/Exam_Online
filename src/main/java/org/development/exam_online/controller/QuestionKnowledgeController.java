package org.development.exam_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.Result;
import org.development.exam_online.dao.entity.QuestionKnowledge;
import org.development.exam_online.security.RequirePermission;
import org.development.exam_online.service.QuestionKnowledgeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识点管理控制器
 * 管理题目的知识点信息，为题库提供更细粒度的知识点维度
 */
@Tag(name = "知识点管理", description = "题目知识点管理接口（支持知识点的增删改查）")
@RestController
@RequestMapping("/api/question-knowledge")
@RequiredArgsConstructor
@RequirePermission({"question:manage"})
public class QuestionKnowledgeController {

    private final QuestionKnowledgeService questionKnowledgeService;

    /**
     * 创建知识点
     */
    @Operation(summary = "创建知识点", description = "在指定分类下创建新的知识点")
    @PostMapping
    public Result<QuestionKnowledge> createKnowledge(@Valid @RequestBody QuestionKnowledge knowledge) {
        QuestionKnowledge created = questionKnowledgeService.createKnowledge(knowledge);
        return Result.success(created);
    }

    /**
     * 获取知识点列表（可按分类筛选）
     */
    @Operation(summary = "获取知识点列表", description = "获取所有知识点列表，可按分类ID筛选")
    @GetMapping
    public Result<List<QuestionKnowledge>> getKnowledgeList(
            @RequestParam(required = false) Long categoryId) {
        List<QuestionKnowledge> list = questionKnowledgeService.getKnowledgeList(categoryId);
        return Result.success(list);
    }

    /**
     * 获取知识点详情
     */
    @Operation(summary = "获取知识点详情", description = "根据ID获取知识点详细信息")
    @GetMapping("/{id}")
    public Result<QuestionKnowledge> getKnowledgeById(@PathVariable Long id) {
        QuestionKnowledge knowledge = questionKnowledgeService.getKnowledgeById(id);
        return Result.success(knowledge);
    }

    /**
     * 更新知识点
     */
    @Operation(summary = "更新知识点", description = "更新指定知识点的信息")
    @PutMapping("/{id}")
    public Result<String> updateKnowledge(
            @PathVariable Long id,
            @Valid @RequestBody QuestionKnowledge knowledge) {
        String message = questionKnowledgeService.updateKnowledge(id, knowledge);
        return Result.success(message);
    }

    /**
     * 删除知识点（逻辑删除）
     */
    @Operation(summary = "删除知识点", description = "删除指定知识点（逻辑删除，需确保无题目引用该知识点）")
    @DeleteMapping("/{id}")
    public Result<String> deleteKnowledge(@PathVariable Long id) {
        String message = questionKnowledgeService.deleteKnowledge(id);
        return Result.success(message);
    }
}

