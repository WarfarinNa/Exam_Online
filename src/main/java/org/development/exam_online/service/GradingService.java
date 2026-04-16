package org.development.exam_online.service;

import org.development.exam_online.common.PageResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface GradingService {

    PageResult<Map<String, Object>> getPendingGradingRecords(Long examId, Long page, Long size);

    List<Map<String, Object>> getExamRecords(Long examId);

    Map<String, Object> getExamRecordDetail(Long recordId);

    void gradeQuestion(Long recordId, Long questionId, BigDecimal score);

    void gradeQuestions(Long recordId, Map<Long, BigDecimal> scores);

    void autoGradeRecord(Long recordId);

    void autoGradeRecord(Long recordId, boolean forceOverride);

    Map<String, Object> autoGradeExam(Long examId);

    Map<String, Object> getExamStatistics(Long examId);

    List<Map<String, Object>> getWrongQuestionAnalysis(Long examId);

    Map<String, Object> getRecordScoreInfo(Long recordId);

    PageResult<Map<String, Object>> getStudentRecords(Long userId, Long page, Long size);

    Map<String, Object> getStudentExamDetail(Long recordId, Long userId);

    void confirmGrading(Long recordId);

    int confirmExamGrading(Long examId);

    Map<String, Object> getExamRecordAiReport(Long recordId);
}
