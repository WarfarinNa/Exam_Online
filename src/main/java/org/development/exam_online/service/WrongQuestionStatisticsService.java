package org.development.exam_online.service;

import org.development.exam_online.dao.dto.KnowledgeStatistics;
import org.development.exam_online.dao.dto.WrongQuestionStatistics;

import java.util.List;

/**
 * 错题统计服务接口
 */
public interface WrongQuestionStatisticsService {
    
    /**
     * 获取学生单次考试的错题统计
     * @param userId 学生ID
     * @param examId 考试ID
     * @return 错题统计数据
     */
    WrongQuestionStatistics getExamStatistics(Long userId, Long examId);
    
    /**
     * 获取学生多次考试的综合统计
     * @param userId 学生ID
     * @param examIds 考试ID列表
     * @return 综合统计数据
     */
    WrongQuestionStatistics getMultiExamStatistics(Long userId, List<Long> examIds);
    
    /**
     * 获取学生所有考试的统计
     * @param userId 学生ID
     * @return 所有考试的统计数据
     */
    WrongQuestionStatistics getAllExamStatistics(Long userId);
    
    /**
     * 获取学生的知识点掌握度
     * @param userId 学生ID
     * @return 知识点掌握度列表
     */
    List<KnowledgeStatistics> getKnowledgeMastery(Long userId);
}
