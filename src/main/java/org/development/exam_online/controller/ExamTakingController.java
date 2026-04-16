package org.development.exam_online.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.Result;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.security.RequirePermission;
import org.development.exam_online.service.ExamTakingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "在线考试", description = "在线考试接口（开始考试、答题、提交等）")
@RestController
@RequestMapping("/api/exam-taking")
@RequiredArgsConstructor
@RequirePermission({"exam:participate"})
public class ExamTakingController {

    private final ExamTakingService examTakingService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "开始考试")
    @PostMapping("/{examId}/start")
    public Result<Map<String, Object>> startExam(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.startExam(examId, userId);
        return Result.success(result);
    }

    @Operation(summary = "获取考试状态")
    @GetMapping("/{examId}/status")
    public Result<Map<String, Object>> getExamStatus(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.getExamStatus(examId, userId);
        return Result.success(result);
    }

    @Operation(summary = "获取考试题目")
    @GetMapping("/{examId}/questions")
    public Result<Map<String, Object>> getExamQuestions(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.getExamQuestions(examId, userId);
        return Result.success(result);
    }

    @Operation(summary = "保存答案")
    @PostMapping("/{examId}/save-answer/{questionId}")
    public Result<String> saveAnswer(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Object answerNode = request.get("answer");
        String answerJson;
        try {
            answerJson = answerNode == null ? "null" : objectMapper.writeValueAsString(answerNode);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "答案必须是合法JSON");
        }
        examTakingService.saveAnswer(examId, userId, questionId, answerJson);
        return Result.success("答案保存成功");
    }

    @Operation(
        summary = "批量保存答案")
    @PostMapping("/{examId}/save-answers")
    public Result<String> saveAnswers(
            @PathVariable Long examId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();

        @SuppressWarnings("unchecked")
        Map<String, Object> answersMap = (Map<String, Object>) request.get("answers");
        if (answersMap == null) {
            return Result.success("没有需要保存的答案");
        }

        Map<Long, String> answers = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : answersMap.entrySet()) {
            try {
                Long questionId = Long.parseLong(entry.getKey());
                Object val = entry.getValue();
                String json = val == null ? "null" : objectMapper.writeValueAsString(val);
                answers.put(questionId, json);
            } catch (NumberFormatException | JsonProcessingException e) {
            }
        }

        examTakingService.saveAnswers(examId, userId, answers);
        return Result.success("答案批量保存成功");
    }

    @Operation(summary = "记录切屏")
    @PostMapping("/{examId}/cheat-log")
    public Result<String> logCheat(
            @PathVariable Long examId,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        String cheatType = request.get("cheatType");
        if (cheatType == null || cheatType.isEmpty()) {
            cheatType = "UNKNOWN";
        }
        examTakingService.logCheat(examId, userId, cheatType);
        return Result.success("切屏行为已记录");
    }

    @Operation(summary = "获取剩余时间")
    @GetMapping("/{examId}/remaining-time")
    public Result<Map<String, Object>> getRemainingTime(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Long remainingTime = examTakingService.getRemainingTime(examId, userId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("remainingTime", remainingTime);
        return Result.success(result);
    }

    @Operation(summary = "提交考试")
    @PostMapping("/{examId}/submit")
    public Result<Map<String, Object>> submitExam(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.submitExam(examId, userId);
        return Result.success(result);
    }

    @Operation(summary = "恢复考试")
    @GetMapping("/{examId}/continue")
    public Result<Map<String, Object>> continueExam(
            @PathVariable Long examId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthContext.getUserId();
        Map<String, Object> result = examTakingService.continueExam(examId, userId);
        return Result.success(result);
    }
}
