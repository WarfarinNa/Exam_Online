package org.development.exam_online.service;

import org.development.exam_online.common.PageResult;
import org.development.exam_online.dao.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 题目服务接口
 */
public interface QuestionService {

    /**
     * 创建题目
     * @param question 题目信息
     * @return 创建的题目
     */
    Question createQuestion(Question question);

    /**
     * 根据ID获取题目详情
     * @param questionId 题目ID
     * @return 题目详情
     */
    Question getQuestionById(Long questionId);

    /**
     * 更新题目
     * @param questionId 题目ID
     * @param question 题目信息
     * @return 更新结果消息
     */
    String updateQuestion(Long questionId, Question question);

    /**
     * 删除题目
     * @param questionId 题目ID
     * @return 删除结果消息
     */
    String deleteQuestion(Long questionId);

    /**
     * 批量删除题目
     * @param questionIds 题目ID列表
     * @return 删除结果消息
     */
    String deleteQuestions(List<Long> questionIds);

    /**
     * 获取题目列表（支持分页和搜索）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param type 题型筛选
     * @param categoryId 分类ID筛选
     * @param keyword 搜索关键词
     * @param createdBy 创建者ID筛选
     * @return 分页结果
     */
    PageResult<Question> getQuestionList(Integer pageNum, Integer pageSize, String type, Long categoryId, String keyword, Long createdBy);

    /**
     * 搜索题目
     * @param keyword 搜索关键词
     * @param type 题型筛选
     * @param categoryId 分类ID筛选
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResult<Question> searchQuestions(String keyword, String type, Long categoryId, Integer pageNum, Integer pageSize);

    /**
     * 批量导入题目
     * @param file Excel文件
     * @return 导入结果消息
     */
    String importQuestions(MultipartFile file);

    /**
     * 导出题目模板
     * @return 导出结果消息（返回文件路径或提示信息）
     */
    String exportTemplate();

    /**
     * 根据分类获取题目列表
     * @param categoryId 分类ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResult<Question> getQuestionsByCategory(Long categoryId, Integer pageNum, Integer pageSize);

    /**
     * 根据题型获取题目列表
     * @param type 题型
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResult<Question> getQuestionsByType(String type, Integer pageNum, Integer pageSize);
}

