package org.development.exam_online.service;

import jakarta.servlet.http.HttpServletResponse;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.dao.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {


    Question createQuestion(Question question);

    Question getQuestionById(Long questionId);

    String updateQuestion(Long questionId, Question question);

    String deleteQuestion(Long questionId);

    String deleteQuestions(List<Long> questionIds);

    PageResult<Question> getQuestionList(Integer pageNum, Integer pageSize, String type, Long categoryId, Long knowledgeId, Integer difficulty, String keyword, Long createdBy);

    PageResult<Question> searchQuestions(String keyword, String type, Long categoryId, Long knowledgeId, Integer difficulty, Integer pageNum, Integer pageSize);

    String importQuestions(MultipartFile file);

    void exportTemplateFile(HttpServletResponse response);

    PageResult<Question> getQuestionsByCategory(Long categoryId, Integer pageNum, Integer pageSize);

    PageResult<Question> getQuestionsByType(String type, Integer pageNum, Integer pageSize);
}

