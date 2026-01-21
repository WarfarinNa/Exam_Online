package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.dto.AutoGeneratePaperRule;
import org.development.exam_online.dao.entity.Exam;
import org.development.exam_online.dao.entity.ExamPaper;
import org.development.exam_online.dao.entity.ExamPaperQuestion;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.dao.mapper.ExamMapper;
import org.development.exam_online.dao.mapper.ExamPaperMapper;
import org.development.exam_online.dao.mapper.ExamPaperQuestionMapper;
import org.development.exam_online.dao.mapper.QuestionMapper;
import org.development.exam_online.service.ExamPaperService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 试卷服务实现类
 */
@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl implements ExamPaperService {

    private final ExamPaperMapper examPaperMapper;
    private final ExamPaperQuestionMapper examPaperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final ExamMapper examMapper;

    /**
     * 创建试卷
     * @param examPaper 试卷信息
     * @return 创建的试卷
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamPaper createPaper(ExamPaper examPaper) {
        // 验证试卷名称
        if (!StringUtils.hasText(examPaper.getName())) {
            throw new RuntimeException("试卷名称不能为空");
        }

        // 设置创建时间
        examPaper.setCreatedAt(LocalDateTime.now());

        // 初始化总分为0
        if (examPaper.getTotalScore() == null) {
            examPaper.setTotalScore(BigDecimal.ZERO);
        }

        int result = examPaperMapper.insert(examPaper);
        if (result > 0) {
            return examPaperMapper.selectById(examPaper.getId());
        } else {
            throw new RuntimeException("创建试卷失败");
        }
    }

    /**
     * 手动组卷 - 添加题目到试卷
     * @param paperId 试卷ID
     * @param questionIds 题目ID列表
     * @param scores 对应题目的分值列表（可选）
     * @return 添加结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addQuestionsToPaper(Long paperId, List<Long> questionIds, List<Double> scores) {
        // 验证试卷是否存在
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("题目ID列表不能为空");
        }

        // 验证题目是否存在
        LambdaQueryWrapper<Question> questionQuery = new LambdaQueryWrapper<>();
        questionQuery.in(Question::getId, questionIds);
        List<Question> questions = questionMapper.selectList(questionQuery);
        if (questions.size() != questionIds.size()) {
            throw new RuntimeException("部分题目不存在");
        }

        // 查询试卷中已有的题目
        LambdaQueryWrapper<ExamPaperQuestion> existingQuery = new LambdaQueryWrapper<>();
        existingQuery.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> existingQuestions = examPaperQuestionMapper.selectList(existingQuery);
        Set<Long> existingQuestionIds = existingQuestions.stream()
                .map(ExamPaperQuestion::getQuestionId)
                .collect(Collectors.toSet());

        int addedCount = 0;
        BigDecimal totalScoreAdded = BigDecimal.ZERO;

        for (int i = 0; i < questionIds.size(); i++) {
            Long questionId = questionIds.get(i);
            
            // 检查题目是否已存在（如果不允许重复）
            if (existingQuestionIds.contains(questionId)) {
                continue;
            }

            // 获取题目分值：如果规则指定了分值则使用规则分值，否则使用题目的默认分值
            BigDecimal score;
            if (scores != null && i < scores.size() && scores.get(i) != null) {
                // 使用规则中指定的分值
                score = BigDecimal.valueOf(scores.get(i));
            } else {
                // 使用题目的默认分值
                Question question = questions.stream()
                        .filter(q -> q.getId().equals(questionId))
                        .findFirst()
                        .orElse(null);
                if (question != null && question.getScore() != null) {
                    score = question.getScore();
                } else {
                    score = BigDecimal.ZERO;
                }
            }

            // 创建试卷题目关联
            ExamPaperQuestion paperQuestion = new ExamPaperQuestion();
            paperQuestion.setPaperId(paperId);
            paperQuestion.setQuestionId(questionId);
            paperQuestion.setScore(score);

            examPaperQuestionMapper.insert(paperQuestion);
            addedCount++;
            totalScoreAdded = totalScoreAdded.add(score);
        }

        // 更新试卷总分
        BigDecimal newTotalScore = paper.getTotalScore().add(totalScoreAdded);
        ExamPaper updatePaper = new ExamPaper();
        updatePaper.setId(paperId);
        updatePaper.setTotalScore(newTotalScore);
        examPaperMapper.updateById(updatePaper);

        return "成功添加 " + addedCount + " 道题目，试卷总分更新为 " + newTotalScore;
    }

    /**
     * 手动组卷 - 移除试卷中的题目
     * @param paperId 试卷ID
     * @param questionIds 要移除的题目ID列表
     * @return 移除结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String removeQuestionsFromPaper(Long paperId, List<Long> questionIds) {
        // 验证试卷是否存在
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("题目ID列表不能为空");
        }

        // 查询要移除的题目分值
        LambdaQueryWrapper<ExamPaperQuestion> query = new LambdaQueryWrapper<>();
        query.eq(ExamPaperQuestion::getPaperId, paperId)
                .in(ExamPaperQuestion::getQuestionId, questionIds);
        List<ExamPaperQuestion> paperQuestions = examPaperQuestionMapper.selectList(query);

        BigDecimal totalScoreRemoved = paperQuestions.stream()
                .map(ExamPaperQuestion::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 删除试卷题目关联
        int removedCount = examPaperQuestionMapper.delete(query);

        // 更新试卷总分
        BigDecimal newTotalScore = paper.getTotalScore().subtract(totalScoreRemoved);
        if (newTotalScore.compareTo(BigDecimal.ZERO) < 0) {
            newTotalScore = BigDecimal.ZERO;
        }
        ExamPaper updatePaper = new ExamPaper();
        updatePaper.setId(paperId);
        updatePaper.setTotalScore(newTotalScore);
        examPaperMapper.updateById(updatePaper);

        return "成功移除 " + removedCount + " 道题目，试卷总分更新为 " + newTotalScore;
    }

    /**
     * 预览自动生成的题目列表
     * @param rule 自动组卷规则
     * @return 预览的题目列表（包含题目详情和建议分值）
     */
    @Override
    public List<Map<String, Object>> previewAutoGeneratedPapers(AutoGeneratePaperRule rule) {
        // 验证规则
        if (rule == null || rule.getTypeRules() == null || rule.getTypeRules().isEmpty()) {
            throw new BusinessException(ErrorCode.AUTO_GENERATE_RULE_EMPTY);
        }

        // 验证题型规则
        for (Map.Entry<String, AutoGeneratePaperRule.TypeRule> entry : rule.getTypeRules().entrySet()) {
            String questionType = entry.getKey();
            AutoGeneratePaperRule.TypeRule typeRule = entry.getValue();
            
            if (!QuestionType.isValid(questionType)) {
                throw new BusinessException(ErrorCode.AUTO_GENERATE_TYPE_INVALID, "无效的题型：" + questionType);
            }
            
            if (typeRule == null || typeRule.getCount() == null || typeRule.getCount() <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "题型 " + questionType + " 的题量必须大于0");
            }
        }

        // 使用随机种子（如果提供）
        Random random = rule.getRandomSeed() != null
                ? new Random(rule.getRandomSeed())
                : new Random();

        // 计算难度配额
        Map<Integer, Integer> difficultyQuota = calculateDifficultyQuota(rule);

        List<Map<String, Object>> selectedQuestions = new ArrayList<>();
        Set<Long> usedQuestionIds = new HashSet<>(); // 用于去重

        // 按题型和难度配额选择题目
        for (Map.Entry<String, AutoGeneratePaperRule.TypeRule> entry : rule.getTypeRules().entrySet()) {
            String questionType = entry.getKey();
            AutoGeneratePaperRule.TypeRule typeRule = entry.getValue();
            Integer totalCount = typeRule.getCount();
            Double defaultScore = typeRule.getScore();

            // 计算该题型各难度的配额分布
            Map<Integer, Integer> typeDifficultyQuota = distributeDifficultyQuotaForType(
                    totalCount, difficultyQuota, rule);

            // 按难度配额分别查询题目
            List<Question> selectedForType = selectQuestionsByTypeAndDifficulty(
                    questionType, typeDifficultyQuota, rule, random, usedQuestionIds);

            // 如果某个难度不足，处理兜底逻辑
            if (selectedForType.size() < totalCount) {
                selectedForType = handleFallback(selectedForType, questionType, totalCount,
                        typeDifficultyQuota, rule, random, usedQuestionIds);
            }

            // 转换为返回格式
            for (Question question : selectedForType) {
                Map<String, Object> questionInfo = new HashMap<>();
                questionInfo.put("id", question.getId());
                questionInfo.put("type", question.getType());
                questionInfo.put("content", question.getContent());
                questionInfo.put("options", question.getOptions());
                questionInfo.put("answer", question.getAnswer());
                questionInfo.put("difficulty", question.getDifficulty());
                questionInfo.put("categoryId", question.getCategoryId());
                // 分值处理逻辑：如果规则指定了分值则使用规则分值，否则使用题目的默认分值
                double score = defaultScore != null ? defaultScore
                        : (question.getScore() != null ? question.getScore().doubleValue() : 0.0);
                questionInfo.put("suggestedScore", score);

                selectedQuestions.add(questionInfo);
            }
        }

        return selectedQuestions;
    }

    /**
     * 保存自动生成的题目列表到试卷
     * @param paperId 试卷ID
     * @param questionIds 题目ID列表
     * @param scores 对应题目的分值列表
     * @return 保存结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveAutoGeneratedPapers(Long paperId, List<Long> questionIds, List<Double> scores) {
        // 验证试卷是否存在
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("题目ID列表不能为空");
        }

        // 清空试卷中的现有题目
        clearAllQuestions(paperId);

        BigDecimal totalScore = BigDecimal.ZERO;

        // 批量添加题目到试卷
        for (int i = 0; i < questionIds.size(); i++) {
            Long questionId = questionIds.get(i);
            Double scoreValue = (scores != null && i < scores.size()) ? scores.get(i) : null;

            // 验证题目是否存在
            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                throw new RuntimeException("题目不存在：" + questionId);
            }

            // 分值处理逻辑：如果规则指定了分值则使用规则分值，否则使用题目的默认分值
            BigDecimal score = scoreValue != null
                    ? BigDecimal.valueOf(scoreValue)
                    : (question.getScore() != null ? question.getScore() : BigDecimal.ZERO);

            ExamPaperQuestion paperQuestion = new ExamPaperQuestion();
            paperQuestion.setPaperId(paperId);
            paperQuestion.setQuestionId(questionId);
            paperQuestion.setScore(score);

            examPaperQuestionMapper.insert(paperQuestion);
            totalScore = totalScore.add(score);
        }

        // 更新试卷总分
        ExamPaper updatePaper = new ExamPaper();
        updatePaper.setId(paperId);
        updatePaper.setTotalScore(totalScore);
        examPaperMapper.updateById(updatePaper);

        return "保存成功，共添加 " + questionIds.size() + " 道题目，试卷总分：" + totalScore;
    }

    /**
     * 计算难度配额
     * @param rule 组卷规则
     * @return 难度配额Map (难度等级 -> 题量)
     */
    private Map<Integer, Integer> calculateDifficultyQuota(AutoGeneratePaperRule rule) {
        Map<Integer, Integer> quota = new HashMap<>();

        if (rule.getDifficultyMode() == null) {
            // 如果没有设置难度模式，返回空配额（不限制难度）
            return quota;
        }

        if (rule.getDifficultyMode() == AutoGeneratePaperRule.DifficultyMode.QUOTA) {
            // 配额模式：直接使用
            if (rule.getDifficultyQuota() != null) {
                quota.putAll(rule.getDifficultyQuota());
            }
        } else if (rule.getDifficultyMode() == AutoGeneratePaperRule.DifficultyMode.RATIO) {
            // 比例模式：需要先计算总题量，然后按比例分配
            if (rule.getDifficultyRatio() == null || rule.getDifficultyRatio().isEmpty()) {
                return quota;
            }

            // 验证比例总和
            double ratioSum = rule.getDifficultyRatio().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            if (Math.abs(ratioSum - 1.0) > 0.01) {
                throw new BusinessException(ErrorCode.AUTO_GENERATE_DIFFICULTY_RATIO_INVALID,
                        "难度比例总和必须接近1.0，当前总和：" + ratioSum);
            }

            // 计算总题量
            int totalCount = rule.getTypeRules().values().stream()
                    .mapToInt(AutoGeneratePaperRule.TypeRule::getCount)
                    .sum();

            // 按比例分配配额
            int allocatedCount = 0;
            List<Map.Entry<Integer, Double>> sortedRatios = rule.getDifficultyRatio().entrySet().stream()
                    .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

            for (int i = 0; i < sortedRatios.size(); i++) {
                Map.Entry<Integer, Double> entry = sortedRatios.get(i);
                Integer difficulty = entry.getKey();
                Double ratio = entry.getValue();
                
                int count;
                if (i == sortedRatios.size() - 1) {
                    // 最后一个难度，补齐剩余数量，确保总和等于totalCount
                    count = totalCount - allocatedCount;
                } else {
                    count = (int) Math.round(totalCount * ratio);
                    allocatedCount += count;
                }
                
                if (count > 0) {
                    quota.put(difficulty, count);
                }
            }
        }

        return quota;
    }

    /**
     * 为某个题型分配难度配额
     * @param typeCount 该题型的总题量
     * @param totalDifficultyQuota 总难度配额
     * @param rule 组卷规则
     * @return 该题型各难度的配额分布
     */
    private Map<Integer, Integer> distributeDifficultyQuotaForType(
            Integer typeCount, Map<Integer, Integer> totalDifficultyQuota, AutoGeneratePaperRule rule) {
        
        Map<Integer, Integer> typeQuota = new HashMap<>();

        if (totalDifficultyQuota == null || totalDifficultyQuota.isEmpty()) {
            // 如果没有难度配额，不限制难度
            return typeQuota;
        }

        // 计算总配额
        int totalQuota = totalDifficultyQuota.values().stream().mapToInt(Integer::intValue).sum();
        if (totalQuota == 0) {
            return typeQuota;
        }

        // 计算总题量（所有题型的总和）
        int allTypesCount = rule.getTypeRules().values().stream()
                .mapToInt(AutoGeneratePaperRule.TypeRule::getCount)
                .sum();

        // 按比例分配该题型的难度配额
        int allocatedCount = 0;
        List<Map.Entry<Integer, Integer>> sortedQuotas = totalDifficultyQuota.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < sortedQuotas.size(); i++) {
            Map.Entry<Integer, Integer> entry = sortedQuotas.get(i);
            Integer difficulty = entry.getKey();
            Integer quota = entry.getValue();

            int count;
            if (i == sortedQuotas.size() - 1) {
                // 最后一个难度，补齐剩余数量
                count = typeCount - allocatedCount;
            } else {
                // 按比例分配：该难度配额 / 总配额 * 题型题量
                count = (int) Math.round((double) quota / allTypesCount * typeCount);
                allocatedCount += count;
            }

            if (count > 0) {
                typeQuota.put(difficulty, count);
            }
        }

        return typeQuota;
    }

    /**
     * 按题型和难度配额查询题目
     * @param questionType 题型
     * @param difficultyQuota 难度配额
     * @param rule 组卷规则
     * @param random 随机数生成器
     * @param usedQuestionIds 已使用的题目ID集合（用于去重）
     * @return 选中的题目列表
     */
    private List<Question> selectQuestionsByTypeAndDifficulty(
            String questionType, Map<Integer, Integer> difficultyQuota,
            AutoGeneratePaperRule rule, Random random, Set<Long> usedQuestionIds) {

        List<Question> selected = new ArrayList<>();
        boolean allowDuplicate = rule.getAllowDuplicate() != null && rule.getAllowDuplicate();

        if (difficultyQuota == null || difficultyQuota.isEmpty()) {
            // 没有难度配额，查询所有符合条件的题目
            List<Question> allQuestions = findQuestionsByType(
                    questionType, rule.getCategoryIds(), null, allowDuplicate, usedQuestionIds);
            
            // 随机打乱并选择
            List<Question> shuffled = new ArrayList<>(allQuestions);
            Collections.shuffle(shuffled, random);
            
            // 计算总题量
            int totalCount = rule.getTypeRules().get(questionType).getCount();
            int selectCount = Math.min(totalCount, shuffled.size());
            selected.addAll(shuffled.subList(0, selectCount));
            
            if (!allowDuplicate) {
                selected.forEach(q -> usedQuestionIds.add(q.getId()));
            }
        } else {
            // 按难度配额分别查询
            for (Map.Entry<Integer, Integer> entry : difficultyQuota.entrySet()) {
                Integer difficulty = entry.getKey();
                Integer quota = entry.getValue();

                List<Question> questions = findQuestionsByType(
                        questionType, rule.getCategoryIds(), difficulty, allowDuplicate, usedQuestionIds);

                // 随机打乱
                List<Question> shuffled = new ArrayList<>(questions);
                Collections.shuffle(shuffled, random);

                // 选择指定数量
                int selectCount = Math.min(quota, shuffled.size());
                List<Question> selectedForDifficulty = shuffled.subList(0, selectCount);
                selected.addAll(selectedForDifficulty);

                if (!allowDuplicate) {
                    selectedForDifficulty.forEach(q -> usedQuestionIds.add(q.getId()));
                }
            }
        }

        return selected;
    }

    /**
     * 查找指定题型和难度的题目
     * @param questionType 题型
     * @param categoryIds 分类ID列表
     * @param difficulty 难度等级（null表示不限制）
     * @param allowDuplicate 是否允许重复
     * @param usedQuestionIds 已使用的题目ID集合
     * @return 题目列表
     */
    private List<Question> findQuestionsByType(
            String questionType, List<Long> categoryIds, Integer difficulty,
            boolean allowDuplicate, Set<Long> usedQuestionIds) {

        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<>();
        query.eq(Question::getType, questionType);

        // 分类筛选
        if (categoryIds != null && !categoryIds.isEmpty()) {
            query.in(Question::getCategoryId, categoryIds);
        }

        // 难度筛选
        if (difficulty != null) {
            query.eq(Question::getDifficulty, difficulty);
        }

        List<Question> questions = questionMapper.selectList(query);

        // 去重筛选（在Java代码中过滤，因为MyBatis-Plus的notIn可能有限制）
        if (!allowDuplicate && !usedQuestionIds.isEmpty()) {
            questions = questions.stream()
                    .filter(q -> !usedQuestionIds.contains(q.getId()))
                    .collect(Collectors.toList());
        }

        return questions;
    }

    /**
     * 处理兜底逻辑（当某个难度题目不足时）
     * @param selected 已选中的题目
     * @param questionType 题型
     * @param requiredCount 需要的总题量
     * @param difficultyQuota 难度配额
     * @param rule 组卷规则
     * @param random 随机数生成器
     * @param usedQuestionIds 已使用的题目ID集合
     * @return 补充后的题目列表
     */
    private List<Question> handleFallback(
            List<Question> selected, String questionType, Integer requiredCount,
            Map<Integer, Integer> difficultyQuota, AutoGeneratePaperRule rule,
            Random random, Set<Long> usedQuestionIds) {

        int currentCount = selected.size();
        int missingCount = requiredCount - currentCount;

        if (missingCount <= 0) {
            return selected;
        }

        AutoGeneratePaperRule.FallbackMode fallbackMode = rule.getFallbackMode();
        if (fallbackMode == null) {
            fallbackMode = AutoGeneratePaperRule.FallbackMode.ERROR;
        }

        switch (fallbackMode) {
            case ERROR:
                // 报错模式
                throw new BusinessException(ErrorCode.AUTO_GENERATE_QUESTION_NOT_ENOUGH,
                        String.format("题型 %s 的题目数量不足，需要 %d 道，但只有 %d 道", questionType, requiredCount, currentCount));

            case IGNORE:
                // 忽略难度限制，从其他难度补足
                List<Question> additionalQuestions = findQuestionsByType(
                        questionType, rule.getCategoryIds(), null,
                        rule.getAllowDuplicate() != null && rule.getAllowDuplicate(), usedQuestionIds);

                // 去除已选中的题目
                Set<Long> selectedIds = selected.stream()
                        .map(Question::getId)
                        .collect(Collectors.toSet());
                additionalQuestions = additionalQuestions.stream()
                        .filter(q -> !selectedIds.contains(q.getId()))
                        .collect(Collectors.toList());

                Collections.shuffle(additionalQuestions, random);
                int addCount = Math.min(missingCount, additionalQuestions.size());
                selected.addAll(additionalQuestions.subList(0, addCount));

                if (addCount < missingCount) {
                    throw new BusinessException(ErrorCode.AUTO_GENERATE_QUESTION_NOT_ENOUGH,
                            String.format("题型 %s 的题目数量不足，需要 %d 道，但只有 %d 道", questionType, requiredCount, selected.size()));
                }
                break;

            case BALANCE:
                // 平衡模式：将不足的配额分配给其他难度
                if (difficultyQuota == null || difficultyQuota.isEmpty()) {
                    // 如果没有难度配额，直接从所有题目中补足
                    List<Question> allQuestions = findQuestionsByType(
                            questionType, rule.getCategoryIds(), null,
                            rule.getAllowDuplicate() != null && rule.getAllowDuplicate(), usedQuestionIds);

                    Set<Long> selectedIds2 = selected.stream()
                            .map(Question::getId)
                            .collect(Collectors.toSet());
                    allQuestions = allQuestions.stream()
                            .filter(q -> !selectedIds2.contains(q.getId()))
                            .collect(Collectors.toList());

                    Collections.shuffle(allQuestions, random);
                    int addCount2 = Math.min(missingCount, allQuestions.size());
                    selected.addAll(allQuestions.subList(0, addCount2));
                } else {
                    // 将不足的配额按比例分配给其他难度
                    List<Integer> availableDifficulties = difficultyQuota.keySet().stream()
                            .sorted()
                            .collect(Collectors.toList());

                    for (Integer diff : availableDifficulties) {
                        if (missingCount <= 0) {
                            break;
                        }

                        List<Question> questions = findQuestionsByType(
                                questionType, rule.getCategoryIds(), diff,
                                rule.getAllowDuplicate() != null && rule.getAllowDuplicate(), usedQuestionIds);

                        Set<Long> selectedIds3 = selected.stream()
                                .map(Question::getId)
                                .collect(Collectors.toSet());
                        questions = questions.stream()
                                .filter(q -> !selectedIds3.contains(q.getId()))
                                .collect(Collectors.toList());

                        Collections.shuffle(questions, random);
                        int addCount3 = Math.min(missingCount, questions.size());
                        selected.addAll(questions.subList(0, addCount3));
                        missingCount -= addCount3;
                    }

                    if (missingCount > 0) {
                        throw new BusinessException(ErrorCode.AUTO_GENERATE_QUESTION_NOT_ENOUGH,
                                String.format("题型 %s 的题目数量不足，需要 %d 道，但只有 %d 道", questionType, requiredCount, selected.size()));
                    }
                }
                break;
        }

        return selected;
    }

    /**
     * 根据ID获取试卷详情（包含题目列表）
     * @param paperId 试卷ID
     * @return 试卷详情（包含题目列表）
     */
    @Override
    public Map<String, Object> getPaperById(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 获取试卷中的题目
        List<Map<String, Object>> questions = getPaperQuestions(paperId);

        Map<String, Object> result = new HashMap<>();
        result.put("paper", paper);
        result.put("questions", questions);
        result.put("questionCount", questions.size());

        return result;
    }

    /**
     * 预览试卷（不包含答案）
     * @param paperId 试卷ID
     * @return 试卷预览信息
     */
    @Override
    public Map<String, Object> previewPaper(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 获取试卷中的题目（不包含答案）
        LambdaQueryWrapper<ExamPaperQuestion> query = new LambdaQueryWrapper<>();
        query.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> paperQuestions = examPaperQuestionMapper.selectList(query);

        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamPaperQuestion paperQuestion : paperQuestions) {
            Question question = questionMapper.selectById(paperQuestion.getQuestionId());
            if (question != null) {
                Map<String, Object> questionMap = new HashMap<>();
                questionMap.put("id", question.getId());
                questionMap.put("type", question.getType());
                questionMap.put("content", question.getContent());
                questionMap.put("options", question.getOptions());
                questionMap.put("score", paperQuestion.getScore());
                // 不包含答案
                questions.add(questionMap);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paper", paper);
        result.put("questions", questions);
        result.put("questionCount", questions.size());

        return result;
    }

    /**
     * 更新试卷基本信息
     * @param paperId 试卷ID
     * @param examPaper 试卷信息
     * @return 更新结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updatePaper(Long paperId, ExamPaper examPaper) {
        ExamPaper existingPaper = examPaperMapper.selectById(paperId);
        if (existingPaper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 设置ID
        examPaper.setId(paperId);

        int result = examPaperMapper.updateById(examPaper);
        if (result > 0) {
            return "试卷信息更新成功";
        } else {
            throw new RuntimeException("试卷信息更新失败");
        }
    }

    /**
     * 删除试卷
     * @param paperId 试卷ID
     * @return 删除结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deletePaper(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 检查是否有关联的考试
        LambdaQueryWrapper<Exam> examQuery = new LambdaQueryWrapper<>();
        examQuery.eq(Exam::getPaperId, paperId);
        Long examCount = examMapper.selectCount(examQuery);

        if (examCount > 0) {
            throw new RuntimeException("该试卷已关联 " + examCount + " 个考试，无法删除");
        }

        // 删除试卷题目关联
        LambdaQueryWrapper<ExamPaperQuestion> questionQuery = new LambdaQueryWrapper<>();
        questionQuery.eq(ExamPaperQuestion::getPaperId, paperId);
        examPaperQuestionMapper.delete(questionQuery);

        // 删除试卷
        int result = examPaperMapper.deleteById(paperId);
        if (result > 0) {
            return "试卷删除成功";
        } else {
            throw new RuntimeException("试卷删除失败");
        }
    }

    /**
     * 获取试卷列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 搜索关键词
     * @param type 试卷类型筛选
     * @param createdBy 创建者ID筛选
     * @return 分页结果
     */
    @Override
    public PageResult<ExamPaper> getPaperList(Integer pageNum, Integer pageSize, String keyword, String type, Long createdBy) {
        Page<ExamPaper> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ExamPaper> query = new LambdaQueryWrapper<>();

        // 关键词搜索（试卷名称）
        if (StringUtils.hasText(keyword)) {
            query.like(ExamPaper::getName, keyword);
        }

        // 试卷类型筛选
        if (StringUtils.hasText(type)) {
            query.eq(ExamPaper::getType, type);
        }

        // 创建者筛选
        if (createdBy != null) {
            query.eq(ExamPaper::getCreatedBy, createdBy);
        }

        // 按创建时间倒序排列
        query.orderByDesc(ExamPaper::getCreatedAt);

        IPage<ExamPaper> pageResult = examPaperMapper.selectPage(page, query);

        return PageResult.of(pageResult.getTotal(), pageNum, pageSize, pageResult.getRecords());
    }

    /**
     * 获取试卷中的题目列表
     * @param paperId 试卷ID
     * @return 题目列表
     */
    @Override
    public List<Map<String, Object>> getPaperQuestions(Long paperId) {
        // 验证试卷是否存在
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 查询试卷题目关联
        LambdaQueryWrapper<ExamPaperQuestion> query = new LambdaQueryWrapper<>();
        query.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> paperQuestions = examPaperQuestionMapper.selectList(query);

        // 获取题目详情
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamPaperQuestion paperQuestion : paperQuestions) {
            Question question = questionMapper.selectById(paperQuestion.getQuestionId());
            if (question != null) {
                Map<String, Object> questionMap = new HashMap<>();
                questionMap.put("id", question.getId());
                questionMap.put("type", question.getType());
                questionMap.put("content", question.getContent());
                questionMap.put("options", question.getOptions());
                questionMap.put("answer", question.getAnswer());
                questionMap.put("score", paperQuestion.getScore());
                questionMap.put("categoryId", question.getCategoryId());
                questions.add(questionMap);
            }
        }

        return questions;
    }

    /**
     * 计算试卷总分
     * @param paperId 试卷ID
     * @return 试卷总分
     */
    @Override
    public Double calculateTotalScore(Long paperId) {
        // 验证试卷是否存在
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 查询试卷中的所有题目分值
        LambdaQueryWrapper<ExamPaperQuestion> query = new LambdaQueryWrapper<>();
        query.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> paperQuestions = examPaperQuestionMapper.selectList(query);

        BigDecimal totalScore = paperQuestions.stream()
                .map(ExamPaperQuestion::getScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 更新试卷总分
        ExamPaper updatePaper = new ExamPaper();
        updatePaper.setId(paperId);
        updatePaper.setTotalScore(totalScore);
        examPaperMapper.updateById(updatePaper);

        return totalScore.doubleValue();
    }

    /**
     * 清空试卷中的所有题目
     * @param paperId 试卷ID
     * @return 清空结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String clearAllQuestions(Long paperId) {
        // 验证试卷是否存在
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 删除所有试卷题目关联
        LambdaQueryWrapper<ExamPaperQuestion> query = new LambdaQueryWrapper<>();
        query.eq(ExamPaperQuestion::getPaperId, paperId);
        int deletedCount = examPaperQuestionMapper.delete(query);

        // 重置试卷总分
        ExamPaper updatePaper = new ExamPaper();
        updatePaper.setId(paperId);
        updatePaper.setTotalScore(BigDecimal.ZERO);
        examPaperMapper.updateById(updatePaper);

        return "成功清空 " + deletedCount + " 道题目，试卷总分已重置为0";
    }
}
