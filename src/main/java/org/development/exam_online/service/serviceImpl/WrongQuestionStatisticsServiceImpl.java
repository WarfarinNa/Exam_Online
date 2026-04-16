package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.dto.KnowledgeStatistics;
import org.development.exam_online.dao.dto.TypeStatistics;
import org.development.exam_online.dao.dto.WrongQuestionDetail;
import org.development.exam_online.dao.dto.WrongQuestionStatistics;
import org.development.exam_online.dao.entity.*;
import org.development.exam_online.dao.mapper.*;
import org.development.exam_online.service.WrongQuestionStatisticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrongQuestionStatisticsServiceImpl implements WrongQuestionStatisticsService {

    private final UserMapper userMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final ExamPaperQuestionSnapshotMapper snapshotMapper;
    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper knowledgeMapper;

    @Override
    public WrongQuestionStatistics getExamStatistics(Long userId, Long examId) {
        return getMultiExamStatistics(userId, Collections.singletonList(examId));
    }

    @Override
    public WrongQuestionStatistics getMultiExamStatistics(Long userId, List<Long> examIds) {
        if (userId == null || examIds == null || examIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户ID和考试ID不能为空");
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 获取考试记录
        LambdaQueryWrapper<ExamRecord> recordQuery = new LambdaQueryWrapper<>();
        recordQuery.eq(ExamRecord::getUserId, userId)
                .in(ExamRecord::getExamId, examIds)
                .eq(ExamRecord::getDeleted, 0);
        List<ExamRecord> records = examRecordMapper.selectList(recordQuery);
        
        if (records.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到该学生的考试记录");
        }

        // 初始化统计对象
        WrongQuestionStatistics statistics = new WrongQuestionStatistics();
        statistics.setUserId(userId);
        statistics.setUserName(user.getRealName());
        statistics.setExamCount(records.size());

        // 获取所有答题记录
        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ExamAnswer> answerQuery = new LambdaQueryWrapper<>();
        answerQuery.in(ExamAnswer::getRecordId, recordIds)
                .eq(ExamAnswer::getDeleted, 0);
        List<ExamAnswer> answers = examAnswerMapper.selectList(answerQuery);

        // 获取所有题目信息
        List<Long> questionIds = answers.stream()
                .map(ExamAnswer::getQuestionId)
                .distinct()
                .collect(Collectors.toList());
        
        if (questionIds.isEmpty()) {
            return initializeEmptyStatistics(statistics);
        }

        Map<Long, Question> questionMap = questionMapper.selectBatchIdsIgnoreDeleted(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 获取考试信息
        Map<Long, Exam> examMap = examMapper.selectBatchIds(examIds)
                .stream()
                .collect(Collectors.toMap(Exam::getId, e -> e));

        // 获取考试记录对应的考试ID映射
        Map<Long, Long> recordToExamMap = records.stream()
                .collect(Collectors.toMap(ExamRecord::getId, ExamRecord::getExamId));

        // 统计总题数和错题数
        int totalQuestions = answers.size();
        int wrongQuestions = 0;

        // 按题型统计
        Map<String, TypeStatistics> typeStatsMap = new HashMap<>();
        // 按知识点统计
        Map<Long, KnowledgeStatistics> knowledgeStatsMap = new HashMap<>();
        // 错题详情
        List<WrongQuestionDetail> wrongDetails = new ArrayList<>();

        for (ExamAnswer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) continue;

            String type = question.getType();
            boolean isWrong = isAnswerWrong(answer, question);
            
            if (isWrong) {
                wrongQuestions++;
            }

            // 统计题型
            updateTypeStatistics(typeStatsMap, type, isWrong);

            // 统计知识点
            updateKnowledgeStatistics(knowledgeStatsMap, question, isWrong);

            // 收集错题详情（最多20题）
            if (isWrong && wrongDetails.size() < 20) {
                WrongQuestionDetail detail = new WrongQuestionDetail();
                detail.setQuestionId(question.getId());
                detail.setStem(truncateString(question.getStem(), 100));
                detail.setType(type);
                detail.setTypeName(getTypeName(type));
                detail.setKnowledge(getQuestionKnowledge(question.getId()));
                detail.setUserAnswer(answer.getUserAnswer());
                detail.setCorrectAnswer(question.getAnswerJson());
                
                Long examId = recordToExamMap.get(answer.getRecordId());
                Exam exam = examMap.get(examId);
                detail.setExamName(exam != null ? exam.getName() : "未知考试");
                detail.setExamId(examId);
                
                wrongDetails.add(detail);
            }
        }

        // 计算正确率
        BigDecimal accuracy = totalQuestions > 0 
                ? BigDecimal.valueOf((totalQuestions - wrongQuestions) * 100.0 / totalQuestions)
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 填充统计数据
        statistics.setTotalQuestions(totalQuestions);
        statistics.setWrongQuestions(wrongQuestions);
        statistics.setAccuracy(accuracy);
        statistics.setByQuestionType(typeStatsMap);
        statistics.setByKnowledge(new ArrayList<>(knowledgeStatsMap.values()));
        statistics.setWrongQuestionDetails(wrongDetails);

        // 按错误率排序知识点
        statistics.getByKnowledge().sort((a, b) -> a.getAccuracy().compareTo(b.getAccuracy()));

        return statistics;
    }

    @Override
    public WrongQuestionStatistics getAllExamStatistics(Long userId) {
        // 获取该学生的所有考试记录
        LambdaQueryWrapper<ExamRecord> query = new LambdaQueryWrapper<>();
        query.eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getDeleted, 0);
        List<ExamRecord> records = examRecordMapper.selectList(query);
        
        if (records.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该学生没有考试记录");
        }

        List<Long> examIds = records.stream()
                .map(ExamRecord::getExamId)
                .distinct()
                .collect(Collectors.toList());

        return getMultiExamStatistics(userId, examIds);
    }

    @Override
    public List<KnowledgeStatistics> getKnowledgeMastery(Long userId) {
        WrongQuestionStatistics stats = getAllExamStatistics(userId);
        return stats.getByKnowledge();
    }

    /**
     * 判断答案是否错误
     */
    private boolean isAnswerWrong(ExamAnswer answer, Question question) {
        if (answer.getScore() == null) {
            return true; // 未评分视为错误
        }
        
        BigDecimal fullScore = question.getScore() != null ? question.getScore() : BigDecimal.ZERO;
        return answer.getScore().compareTo(fullScore) < 0;
    }

    /**
     * 更新题型统计
     */
    private void updateTypeStatistics(Map<String, TypeStatistics> statsMap, String type, boolean isWrong) {
        TypeStatistics stats = statsMap.computeIfAbsent(type, k -> {
            TypeStatistics s = new TypeStatistics();
            s.setTypeName(getTypeName(type));
            s.setTotal(0);
            s.setWrong(0);
            return s;
        });

        stats.setTotal(stats.getTotal() + 1);
        if (isWrong) {
            stats.setWrong(stats.getWrong() + 1);
        }

        // 计算正确率
        BigDecimal accuracy = stats.getTotal() > 0
                ? BigDecimal.valueOf((stats.getTotal() - stats.getWrong()) * 100.0 / stats.getTotal())
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        stats.setAccuracy(accuracy);
    }

    /**
     * 更新知识点统计
     */
    private void updateKnowledgeStatistics(Map<Long, KnowledgeStatistics> statsMap, Question question, boolean isWrong) {
        Long knowledgeId = question.getKnowledgeId();
        if (knowledgeId == null) return;

        KnowledgeStatistics stats = statsMap.computeIfAbsent(knowledgeId, k -> {
            KnowledgeStatistics s = new KnowledgeStatistics();
            s.setKnowledgeId(knowledgeId);
            s.setKnowledgeName(getKnowledgeName(knowledgeId));
            s.setTotal(0);
            s.setWrong(0);
            return s;
        });

        stats.setTotal(stats.getTotal() + 1);
        if (isWrong) {
            stats.setWrong(stats.getWrong() + 1);
        }

        // 计算正确率
        BigDecimal accuracy = stats.getTotal() > 0
                ? BigDecimal.valueOf((stats.getTotal() - stats.getWrong()) * 100.0 / stats.getTotal())
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        stats.setAccuracy(accuracy);
    }

    /**
     * 获取题型名称
     */
    private String getTypeName(String type) {
        if (QuestionType.SINGLE.getCode().equals(type)) return "单选题";
        if (QuestionType.MULTIPLE.getCode().equals(type)) return "多选题";
        if (QuestionType.JUDGE.getCode().equals(type)) return "判断题";
        if (QuestionType.BLANK.getCode().equals(type)) return "填空题";
        if (QuestionType.SHORT.getCode().equals(type)) return "简答题";
        return "未知题型";
    }

    /**
     * 获取知识点名称
     */
    private String getKnowledgeName(Long knowledgeId) {
        QuestionKnowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        return knowledge != null ? knowledge.getName() : "未知知识点";
    }

    /**
     * 获取题目的知识点名称
     */
    private String getQuestionKnowledge(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null || question.getKnowledgeId() == null) {
            return "未分类";
        }
        return getKnowledgeName(question.getKnowledgeId());
    }

    /**
     * 截断字符串
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 初始化空统计数据
     */
    private WrongQuestionStatistics initializeEmptyStatistics(WrongQuestionStatistics statistics) {
        statistics.setTotalQuestions(0);
        statistics.setWrongQuestions(0);
        statistics.setAccuracy(BigDecimal.ZERO);
        statistics.setByQuestionType(new HashMap<>());
        statistics.setByKnowledge(new ArrayList<>());
        statistics.setWrongQuestionDetails(new ArrayList<>());
        return statistics;
    }
}
