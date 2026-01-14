package org.development.exam_online.service;

import org.development.exam_online.dao.entity.QuestionCategory;

import java.util.List;

/**
 * 题目分类服务接口
 */
public interface QuestionCategoryService {

    /**
     * 创建题目分类
     * @param category 分类信息
     * @return 创建的分类
     */
    QuestionCategory createCategory(QuestionCategory category);

    /**
     * 获取所有题目分类列表
     * @return 分类列表
     */
    List<QuestionCategory> getCategoryList();

    /**
     * 根据ID获取分类详情
     * @param categoryId 分类ID
     * @return 分类详情
     */
    QuestionCategory getCategoryById(Long categoryId);

    /**
     * 更新题目分类
     * @param categoryId 分类ID
     * @param category 分类信息
     * @return 更新结果消息
     */
    String updateCategory(Long categoryId, QuestionCategory category);

    /**
     * 删除题目分类
     * @param categoryId 分类ID
     * @return 删除结果消息
     */
    String deleteCategory(Long categoryId);

    /**
     * 获取分类下的题目数量
     * @param categoryId 分类ID
     * @return 题目数量
     */
    Long getQuestionCountByCategory(Long categoryId);
}

