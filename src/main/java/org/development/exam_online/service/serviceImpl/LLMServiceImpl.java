package org.development.exam_online.service.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.development.exam_online.config.LLMConfig;
import org.development.exam_online.dao.dto.KnowledgeStatistics;
import org.development.exam_online.dao.dto.TypeStatistics;
import org.development.exam_online.dao.dto.WrongQuestionDetail;
import org.development.exam_online.dao.dto.WrongQuestionStatistics;
import org.development.exam_online.service.LLMService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {

    private final LLMConfig llmConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateAnalysisReport(WrongQuestionStatistics statistics, String promptTemplate) {
        if (!llmConfig.getEnabled()) {
            return "分析功能未使用";
        }
        int maxRetries = llmConfig.getMaxRetries();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 构建提示词
                String prompt = buildPrompt(statistics, promptTemplate);
                
                // 调用API
                String response = callLLMAPI(prompt);
                return response;
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    try {
                        long waitTime = (long) (2000 * Math.pow(2, attempt - 1));
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return generateFallbackReport(statistics);
    }

    @Override
    public boolean testConnection() {
        try {
            String testPrompt = "请回复：连接成功";
            String response = callLLMAPI(testPrompt);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            log.error("测试API连接失败", e);
            return false;
        }
    }

    /**
     * 调用大模型API
     */
    private String callLLMAPI(String userPrompt) throws Exception {
        // 请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmConfig.getDefaultModel());
        requestBody.put("max_tokens", llmConfig.getMaxTokens());
        requestBody.put("temperature", llmConfig.getTemperature());
        
        // 消息列表
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一位经验丰富的教育专家，擅长分析学生的学习情况并提供个性化的学习建议。");
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        
        requestBody.put("messages", List.of(systemMessage, userMessage));

        // 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + llmConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 发送请求
        log.info("调用大模型API: {}", llmConfig.getApiUrl());
        ResponseEntity<String> response = restTemplate.exchange(
                llmConfig.getApiUrl(),
                HttpMethod.POST,
                entity,
                String.class
        );

        // 解析
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    return message.get("content").asText();
                }
            }
        }

        throw new RuntimeException("API响应格式错误");
    }

    private String buildPrompt(WrongQuestionStatistics statistics, String promptTemplate) {
        if ("single_exam".equals(promptTemplate)) {
            return buildSingleExamPrompt(statistics);
        } else if ("multi_exam".equals(promptTemplate)) {
            return buildMultiExamPrompt(statistics);
        }
        return buildSingleExamPrompt(statistics);
    }

    private String buildSingleExamPrompt(WrongQuestionStatistics statistics) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下学生在单次考试中的表现，生成一份个性化的错题分析报告。\n\n");
        
        prompt.append("【学生信息】\n");
        prompt.append("- 姓名：").append(statistics.getUserName()).append("\n");
        prompt.append("- 总答题数：").append(statistics.getTotalQuestions()).append("\n");
        prompt.append("- 错题数：").append(statistics.getWrongQuestions()).append("\n");
        prompt.append("- 正确率：").append(statistics.getAccuracy()).append("%\n\n");

        prompt.append("【题型掌握情况】\n");
        if (statistics.getByQuestionType() != null && !statistics.getByQuestionType().isEmpty()) {
            for (Map.Entry<String, TypeStatistics> entry : statistics.getByQuestionType().entrySet()) {
                TypeStatistics ts = entry.getValue();
                prompt.append("- ").append(ts.getTypeName())
                      .append("：总数 ").append(ts.getTotal())
                      .append("，错误 ").append(ts.getWrong())
                      .append("，正确率 ").append(ts.getAccuracy()).append("%\n");
            }
        }
        prompt.append("\n");

        prompt.append("【知识点掌握情况】\n");
        if (statistics.getByKnowledge() != null && !statistics.getByKnowledge().isEmpty()) {
            for (KnowledgeStatistics ks : statistics.getByKnowledge()) {
                prompt.append("- ").append(ks.getKnowledgeName())
                      .append("：总数 ").append(ks.getTotal())
                      .append("，错误 ").append(ks.getWrong())
                      .append("，正确率 ").append(ks.getAccuracy()).append("%\n");
            }
        }
        prompt.append("\n");

        prompt.append("【典型错题示例（最多5题）】\n");
        if (statistics.getWrongQuestionDetails() != null && !statistics.getWrongQuestionDetails().isEmpty()) {
            int count = Math.min(5, statistics.getWrongQuestionDetails().size());
            for (int i = 0; i < count; i++) {
                WrongQuestionDetail detail = statistics.getWrongQuestionDetails().get(i);
                prompt.append(i + 1).append(". ").append(detail.getStem()).append("\n");
                prompt.append("   - 题型：").append(detail.getTypeName()).append("\n");
                prompt.append("   - 知识点：").append(detail.getKnowledge()).append("\n");
                prompt.append("   - 学生答案：").append(detail.getUserAnswer()).append("\n");
                prompt.append("   - 正确答案：").append(detail.getCorrectAnswer()).append("\n\n");
            }
        }

        prompt.append("请从以下几个方面进行分析，用Markdown格式输出：\n\n");
        prompt.append("## 1. 本次考试总体表现\n");
        prompt.append("简要评价学生的整体表现（2-3句话）\n\n");
        prompt.append("## 2. 薄弱知识点分析\n");
        prompt.append("列出掌握率低于70%的知识点，并分析可能的原因\n\n");
        prompt.append("## 3. 题型分析\n");
        prompt.append("分析各题型的掌握情况，指出需要加强的题型\n\n");
        prompt.append("## 4. 学习建议\n");
        prompt.append("提供3-5条具体可行的学习建议\n\n");
        prompt.append("## 5. 推荐练习重点\n");
        prompt.append("列出优先需要加强的知识点和题型\n\n");
        prompt.append("请用友好、鼓励的语气，避免使用过于专业的术语。");

        return prompt.toString();
    }

    private String buildMultiExamPrompt(WrongQuestionStatistics statistics) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下学生在多次考试中的表现，生成一份综合性的错题分析报告。\n\n");
        
        prompt.append("【学生信息】\n");
        prompt.append("- 姓名：").append(statistics.getUserName()).append("\n");
        prompt.append("- 参与考试次数：").append(statistics.getExamCount()).append("\n");
        prompt.append("- 总答题数：").append(statistics.getTotalQuestions()).append("\n");
        prompt.append("- 总错题数：").append(statistics.getWrongQuestions()).append("\n");
        prompt.append("- 平均正确率：").append(statistics.getAccuracy()).append("%\n\n");

        prompt.append("【题型掌握情况（综合统计）】\n");
        if (statistics.getByQuestionType() != null && !statistics.getByQuestionType().isEmpty()) {
            for (Map.Entry<String, TypeStatistics> entry : statistics.getByQuestionType().entrySet()) {
                TypeStatistics ts = entry.getValue();
                prompt.append("- ").append(ts.getTypeName())
                      .append("：总数 ").append(ts.getTotal())
                      .append("，错误 ").append(ts.getWrong())
                      .append("，正确率 ").append(ts.getAccuracy()).append("%\n");
            }
        }
        prompt.append("\n");

        prompt.append("【知识点掌握情况（综合统计）】\n");
        if (statistics.getByKnowledge() != null && !statistics.getByKnowledge().isEmpty()) {
            for (KnowledgeStatistics ks : statistics.getByKnowledge()) {
                prompt.append("- ").append(ks.getKnowledgeName())
                      .append("：总数 ").append(ks.getTotal())
                      .append("，错误 ").append(ks.getWrong())
                      .append("，正确率 ").append(ks.getAccuracy()).append("%\n");
            }
        }
        prompt.append("\n");

        prompt.append("请从以下几个方面进行分析，用Markdown格式输出：\n\n");
        prompt.append("## 1. 学习进展总结\n");
        prompt.append("总结学生在多次考试中的整体表现和进步情况\n\n");
        prompt.append("## 2. 持续薄弱的知识点\n");
        prompt.append("列出在多次考试中反复出错的知识点\n\n");
        prompt.append("## 3. 题型掌握趋势\n");
        prompt.append("分析各题型的掌握情况和变化趋势\n\n");
        prompt.append("## 4. 综合学习建议\n");
        prompt.append("提供系统性的学习改进建议（3-5条）\n\n");
        prompt.append("## 5. 长期学习规划\n");
        prompt.append("建议学生制定的学习计划和重点方向\n\n");
        prompt.append("请用友好、鼓励的语气，关注学生的进步和潜力。");

        return prompt.toString();
    }

    private String generateFallbackReport(WrongQuestionStatistics statistics) {
        StringBuilder report = new StringBuilder();
        report.append("# 错题分析报告\n\n");
        report.append("> 注意：由于AI服务暂时不可用，以下是基于规则生成的简化报告。\n\n");
        
        report.append("## 基本信息\n\n");
        report.append("- 学生：").append(statistics.getUserName()).append("\n");
        report.append("- 总答题数：").append(statistics.getTotalQuestions()).append("\n");
        report.append("- 错题数：").append(statistics.getWrongQuestions()).append("\n");
        report.append("- 正确率：").append(statistics.getAccuracy()).append("%\n\n");

        report.append("## 薄弱知识点\n\n");
        if (statistics.getByKnowledge() != null && !statistics.getByKnowledge().isEmpty()) {
            for (KnowledgeStatistics ks : statistics.getByKnowledge()) {
                if (ks.getAccuracy().doubleValue() < 70) {
                    report.append("- **").append(ks.getKnowledgeName()).append("**：正确率 ")
                          .append(ks.getAccuracy()).append("%，需要重点加强\n");
                }
            }
        }

        report.append("\n## 学习建议\n\n");
        report.append("1. 针对薄弱知识点进行专项练习\n");
        report.append("2. 复习错题，理解错误原因\n");
        report.append("3. 定期进行知识点巩固测试\n");

        return report.toString();
    }
}
