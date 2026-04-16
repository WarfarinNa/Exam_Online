package org.development.exam_online.service;

import org.development.exam_online.dao.entity.QuestionKnowledge;

import java.util.List;

/**
 * 知识点服务接口
 */
public interface QuestionKnowledgeService {

    QuestionKnowledge createKnowledge(QuestionKnowledge knowledge);

    List<QuestionKnowledge> getKnowledgeList(Long categoryId);

    QuestionKnowledge getKnowledgeById(Long id);

    String updateKnowledge(Long id, QuestionKnowledge knowledge);

    String deleteKnowledge(Long id);
}

