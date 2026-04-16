package org.development.exam_online.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.development.exam_online.config.LLMConfig;
import org.development.exam_online.dao.dto.WrongQuestionStatistics;
import org.development.exam_online.dao.entity.AiAnalysisReport;
import org.development.exam_online.dao.mapper.AiAnalysisReportMapper;
import org.development.exam_online.service.AnalysisService;
import org.development.exam_online.service.LLMService;
import org.development.exam_online.service.WrongQuestionStatisticsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final WrongQuestionStatisticsService statisticsService;
    private final LLMService llmService;
    private final AiAnalysisReportMapper reportMapper;
    private final LLMConfig llmConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiAnalysisReport generateExamReport(Long userId, Long examId) {

        long startTime = System.currentTimeMillis();
        
        // 获取错题数据
        WrongQuestionStatistics statistics = statisticsService.getExamStatistics(userId, examId);
        
        // 调用大模型生成报告
        String aiReport = llmService.generateAnalysisReport(statistics, "single_exam");
        
        long generationTime = (int) (System.currentTimeMillis() - startTime);
        
        // 保存
        AiAnalysisReport report = new AiAnalysisReport();
        report.setUserId(userId);
        report.setReportType("single_exam");
        report.setExamIds(examId.toString());
        report.setAnalysisData(toJson(statistics));
        report.setAiReport(aiReport);
        report.setModelName(llmConfig.getDefaultModel());
        report.setGenerationTime((int) generationTime);
        report.setCreatedTime(LocalDateTime.now());
        report.setDeleted(0);
        
        reportMapper.insert(report);

        return report;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象转JSON失败", e);
            return "{}";
        }
    }
}
