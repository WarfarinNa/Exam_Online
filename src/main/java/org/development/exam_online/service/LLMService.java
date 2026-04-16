package org.development.exam_online.service;

import org.development.exam_online.dao.dto.WrongQuestionStatistics;

/**
 * 大语言模型服务接口
 */
public interface LLMService {
    
    /**
     * 调用大模型生成分析报告
     * @param statistics 错题统计数据
     * @param promptTemplate 提示词模板类型（single_exam/multi_exam）
     * @return 生成的分析报告（Markdown格式）
     */
    String generateAnalysisReport(WrongQuestionStatistics statistics, String promptTemplate);
    
    /**
     * 测试API连接
     * @return 是否连接成功
     */
    boolean testConnection();
}
