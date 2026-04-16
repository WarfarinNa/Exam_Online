package org.development.exam_online.service;

import org.development.exam_online.common.PageResult;
import org.development.exam_online.dao.entity.Exam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 考试服务接口
 */
public interface ExamService {

    Exam createExam(Exam exam);

    Exam publishPaper(Long paperId, Exam exam);

    Exam getExamById(Long examId);

    String updateExam(Long examId, Exam exam);

    String setExamTime(Long examId, LocalDateTime startTime, LocalDateTime endTime);

    String setExamPermissions(Long examId, List<String> allowRoles);

    String deleteExam(Long examId);

    PageResult<Exam> getExamList(Integer pageNum, Integer pageSize, String keyword, Long paperId, LocalDateTime startTime, LocalDateTime endTime);

    PageResult<Exam> getPublishedExams(Integer pageNum, Integer pageSize);

    PageResult<Map<String, Object>> getMyExams(Long userId, Integer pageNum, Integer pageSize, String keyword);

    String unpublishExam(Long examId);

    Map<String, Object> getExamStatistics(Long examId);
}
