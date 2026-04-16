package org.development.exam_online.service;

import org.development.exam_online.common.PageResult;
import org.development.exam_online.dao.dto.KnowledgeStatistics;
import org.development.exam_online.dao.entity.AiAnalysisReport;

import java.util.List;

/**
 * AI分析服务接口
 */
public interface AnalysisService {

    AiAnalysisReport generateExamReport(Long userId, Long examId);

}
