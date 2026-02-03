package org.development.exam_online.service;

import org.development.exam_online.dao.entity.QuestionKnowledge;

import java.util.List;

/**
 * 知识点服务接口
 */
public interface QuestionKnowledgeService {

    /**
     * 创建知识点
     */
    QuestionKnowledge createKnowledge(QuestionKnowledge knowledge);

    /**
     * 获取所有知识点列表（可选按分类过滤）
     */
    List<QuestionKnowledge> getKnowledgeList(Long categoryId);

    /**
     * 根据ID获取知识点详情
     */
    QuestionKnowledge getKnowledgeById(Long id);

    /**
     * 更新知识点
     */
    String updateKnowledge(Long id, QuestionKnowledge knowledge);

    /**
     * 删除知识点（逻辑删除）
     */
    String deleteKnowledge(Long id);
}

