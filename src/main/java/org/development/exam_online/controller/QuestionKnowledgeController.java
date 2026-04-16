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


@Tag(name = "知识点管理")
@RestController
@RequestMapping("/api/question-knowledge")
@RequiredArgsConstructor
@RequirePermission({"question:manage"})
public class QuestionKnowledgeController {

    private final QuestionKnowledgeService questionKnowledgeService;

    @Operation(summary = "创建知识点")
    @PostMapping
    public Result<QuestionKnowledge> createKnowledge(@Valid @RequestBody QuestionKnowledge knowledge) {
        QuestionKnowledge created = questionKnowledgeService.createKnowledge(knowledge);
        return Result.success(created);
    }

    @Operation(summary = "获取知识点列表")
    @GetMapping
    public Result<List<QuestionKnowledge>> getKnowledgeList(
            @RequestParam(required = false) Long categoryId) {
        List<QuestionKnowledge> list = questionKnowledgeService.getKnowledgeList(categoryId);
        return Result.success(list);
    }

    @Operation(summary = "获取知识点详情")
    @GetMapping("/{id}")
    public Result<QuestionKnowledge> getKnowledgeById(@PathVariable Long id) {
        QuestionKnowledge knowledge = questionKnowledgeService.getKnowledgeById(id);
        return Result.success(knowledge);
    }

    @Operation(summary = "更新知识点")
    @PutMapping("/{id}")
    public Result<String> updateKnowledge(
            @PathVariable Long id,
            @Valid @RequestBody QuestionKnowledge knowledge) {
        String message = questionKnowledgeService.updateKnowledge(id, knowledge);
        return Result.success(message);
    }

    @Operation(
        summary = "删除知识点")
    @DeleteMapping("/{id}")
    public Result<String> deleteKnowledge(@PathVariable Long id) {
        String message = questionKnowledgeService.deleteKnowledge(id);
        return Result.success(message);
    }
}

