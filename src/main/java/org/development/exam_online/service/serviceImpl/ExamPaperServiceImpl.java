package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.enums.PaperTemplate;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.dto.AutoGeneratePaperRule;
import org.development.exam_online.dao.dto.TemplateGenerateRequest;
import org.development.exam_online.dao.entity.Exam;
import org.development.exam_online.dao.entity.ExamPaper;
import org.development.exam_online.dao.entity.ExamPaperQuestion;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.dao.mapper.ExamMapper;
import org.development.exam_online.dao.mapper.ExamPaperMapper;
import org.development.exam_online.dao.mapper.ExamPaperQuestionMapper;
import org.development.exam_online.dao.mapper.QuestionMapper;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.service.ExamPaperService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl implements ExamPaperService {

    private final ExamPaperMapper examPaperMapper;
    private final ExamPaperQuestionMapper examPaperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final ExamMapper examMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamPaper createPaper(ExamPaper examPaper) {
        if (examPaper == null || !StringUtils.hasText(examPaper.getName())) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NAME_EMPTY);
        }
        examPaper.setId(null);
        examPaper.setDeleted(0);
        if (examPaper.getCreatedBy() == null) {
            Long userId = AuthContext.getUserId();
            if (userId != null) {
                examPaper.setCreatedBy(userId);
            }
        }
        // 组卷：0手动 1自动
        if (examPaper.getBuildType() == null) {
            examPaper.setBuildType(0);
        }
        int inserted = examPaperMapper.insert(examPaper);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_CREATE_FAILED);
        }
        return examPaperMapper.selectById(examPaper.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addQuestionsToPaper(Long paperId, List<Long> questionIds, List<Double> scores) {
        requireActivePaper(paperId);
        if (CollectionUtils.isEmpty(questionIds)) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_QUESTION_IDS_EMPTY);
        }
        // 校验
        List<Question> questions = questionMapper.selectBatchIdsIgnoreDeleted(questionIds);
        if (questions == null || questions.size() != questionIds.size()) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_QUESTION_NOT_FOUND);
        }


        LambdaQueryWrapper<ExamPaperQuestion> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> existing = examPaperQuestionMapper.selectList(q);
        int startOrder = existing.stream()
                .map(ExamPaperQuestion::getQuestionOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        for (int i = 0; i < questionIds.size(); i++) {
            Long qId = questionIds.get(i);
            if (qId == null) continue;

            // 去重
            boolean already = existing.stream().anyMatch(epq -> epq.getQuestionId().equals(qId));
            if (already) continue;

            ExamPaperQuestion epq = new ExamPaperQuestion();
            epq.setPaperId(paperId);
            epq.setQuestionId(qId);
            // 计算分值
            BigDecimal score = null;
            if (scores != null && i < scores.size() && scores.get(i) != null) {
                score = BigDecimal.valueOf(scores.get(i));
            } else {
                Question question = questions.stream()
                        .filter(qq -> qq.getId().equals(qId))
                        .findFirst()
                        .orElse(null);
                if (question != null) {
                    score = question.getScore();
                }
            }
            if (score == null) {
                score = BigDecimal.ZERO;
            }
            epq.setQuestionScore(score);
            epq.setQuestionOrder(++startOrder);
            examPaperQuestionMapper.insert(epq);
        }

        updatePaperTotalScore(paperId);
        return "添加题目成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String removeQuestionsFromPaper(Long paperId, List<Long> questionIds) {
        requireActivePaper(paperId);
        if (CollectionUtils.isEmpty(questionIds)) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_QUESTION_IDS_EMPTY);
        }
        LambdaQueryWrapper<ExamPaperQuestion> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestion::getPaperId, paperId)
                .in(ExamPaperQuestion::getQuestionId, questionIds);
        examPaperQuestionMapper.delete(q);
        updatePaperTotalScore(paperId);
        return "移除题目成功";
    }

//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public Map<String, Object> generatePaperByTemplate(TemplateGenerateRequest request) {
//        // 1. 获取模板
//        PaperTemplate template = PaperTemplate.fromCode(request.getTemplateCode());
//        if (template == null) {
//            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的模板代码: " + request.getTemplateCode());
//        }
//
//        // 2. 构建组卷规则
//        AutoGeneratePaperRule rule = buildRuleFromTemplate(template, request);
//
//        // 3. 选题（带警告信息）
//        List<SelectedQuestion> selectedQuestions = new ArrayList<>();
//        List<String> warnings = new ArrayList<>();
//        selectQuestionsWithWarnings(rule, selectedQuestions, warnings);
//
//        // 4. 创建试卷
//        ExamPaper paper = new ExamPaper();
//        paper.setName(request.getName());
//        paper.setDescription(request.getDescription());
//        paper.setDuration(request.getDuration());
//        paper.setBuildType(2); // 模板组卷
//        paper.setDeleted(0);
//        paper.setTotalScore(BigDecimal.ZERO); // 先设置为0，后面更新
//        Long userId = AuthContext.getUserId();
//        if (userId != null) {
//            paper.setCreatedBy(userId);
//        }
//        // 保存规则JSON
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            paper.setRuleJson(objectMapper.writeValueAsString(rule));
//        } catch (Exception e) {
//            paper.setRuleJson("{}");
//        }
//        examPaperMapper.insert(paper);
//
//        // 5. 批量插入试卷题目
//        BigDecimal totalScore = BigDecimal.ZERO;
//        int order = 0;
//        for (SelectedQuestion sq : selectedQuestions) {
//            ExamPaperQuestion epq = new ExamPaperQuestion();
//            epq.setPaperId(paper.getId());
//            epq.setQuestionId(sq.question.getId());
//            epq.setQuestionScore(sq.score);
//            epq.setQuestionOrder(++order);
//            examPaperQuestionMapper.insert(epq);
//            totalScore = totalScore.add(sq.score);
//        }
//
//        // 6. 更新总分
//        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExamPaper> updateWrapper =
//                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
//        updateWrapper.eq(ExamPaper::getId, paper.getId())
//                .set(ExamPaper::getTotalScore, totalScore);
//        examPaperMapper.update(null, updateWrapper);
//        paper.setTotalScore(totalScore);
//
//        // 7. 构建返回结果（包含警告信息）
//        Map<String, Object> result = buildAutoGenerateResult(paper, selectedQuestions);
//        if (!warnings.isEmpty()) {
//            result.put("warnings", warnings);
//            result.put("hasWarnings", true);
//        } else {
//            result.put("hasWarnings", false);
//        }
//        return result;
//    }

    @Override
    public Map<String, Object> previewTemplateGeneration(TemplateGenerateRequest request) {
        // 1. 获取模板
        PaperTemplate template = PaperTemplate.fromCode(request.getTemplateCode());
        if (template == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的模板代码: " + request.getTemplateCode());
        }

        // 2. 构建组卷规则
        AutoGeneratePaperRule rule = buildRuleFromTemplate(template, request);

        // 3. 选题
        List<SelectedQuestion> selectedQuestions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        selectQuestionsWithWarnings(rule, selectedQuestions, warnings);

        // 4. 构建预览结果/不创建试卷
        Map<String, Object> result = new HashMap<>();
        
        // 基本信息
        result.put("name", request.getName());
        result.put("description", request.getDescription());
        result.put("duration", request.getDuration());
        result.put("templateCode", request.getTemplateCode());
        result.put("templateName", template.getName());
        
        // 总分
        BigDecimal totalScore = selectedQuestions.stream()
                .map(sq -> sq.score)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalScore", totalScore);
        result.put("totalQuestions", selectedQuestions.size());

        // 题型分布统计
        Map<String, Integer> typeDistribution = new LinkedHashMap<>();
        Map<String, BigDecimal> typeScoreDistribution = new LinkedHashMap<>();
        // 难度分布统计
        Map<Integer, Integer> difficultyDistribution = new TreeMap<>();

        // 题目列表
        List<Map<String, Object>> questions = new ArrayList<>();
        int order = 0;
        for (SelectedQuestion sq : selectedQuestions) {
            // 统计
            String type = sq.question.getType();
            typeDistribution.merge(type, 1, Integer::sum);
            typeScoreDistribution.merge(type, sq.score, BigDecimal::add);
            int diff = sq.question.getDifficulty() != null ? sq.question.getDifficulty() : 2;
            difficultyDistribution.merge(diff, 1, Integer::sum);

            // 题目详情
            Map<String, Object> m = new HashMap<>();
            m.put("id", sq.question.getId());
            m.put("type", sq.question.getType());
            m.put("stem", sq.question.getStem());
            m.put("optionsJson", sq.question.getOptionsJson());
            m.put("answerJson", sq.question.getAnswerJson());
            m.put("difficulty", sq.question.getDifficulty());
            m.put("categoryId", sq.question.getCategoryId());
            m.put("knowledgeId", sq.question.getKnowledgeId());
            m.put("score", sq.score);
            m.put("order", ++order);
            questions.add(m);
        }

        result.put("typeDistribution", typeDistribution);
        result.put("typeScoreDistribution", typeScoreDistribution);
        result.put("difficultyDistribution", difficultyDistribution);
        result.put("questions", questions);
        
        // 警告信息
        if (!warnings.isEmpty()) {
            result.put("warnings", warnings);
            result.put("hasWarnings", true);
        } else {
            result.put("hasWarnings", false);
        }

        return result;
    }

    private AutoGeneratePaperRule buildRuleFromTemplate(
            PaperTemplate template,
            TemplateGenerateRequest request) {

        AutoGeneratePaperRule rule = new AutoGeneratePaperRule();

        // 1. 题型规则
        Map<String, AutoGeneratePaperRule.TypeRule> typeRules;

        if (request.getTypeRules() != null && !request.getTypeRules().isEmpty()) {
            // 使用用户自定义的题型规则
            typeRules = new LinkedHashMap<>();
            for (Map.Entry<String, TemplateGenerateRequest.TypeRule> entry
                    : request.getTypeRules().entrySet()) {
                String typeCode = entry.getKey();
               TemplateGenerateRequest.TypeRule userRule = entry.getValue();

                AutoGeneratePaperRule.TypeRule typeRule = new AutoGeneratePaperRule.TypeRule();
                typeRule.setCount(userRule.getCount());
                typeRule.setScore(userRule.getScore());
                typeRules.put(typeCode, typeRule);
            }
        } else {
            // 使用模板默认题型规则
            typeRules = new LinkedHashMap<>(template.getTypeRules());
        }

        rule.setTypeRules(typeRules);

        // 2. 难度规则
        rule.setDifficultyMode(AutoGeneratePaperRule.DifficultyMode.RATIO);
        if (request.getDifficultyRatio() != null && !request.getDifficultyRatio().isEmpty()) {
            // 使用自定义难度
            rule.setDifficultyRatio(request.getDifficultyRatio());
        } else {
            // 使用模板默认难度
            rule.setDifficultyRatio(template.getDefaultDifficultyRatio());
        }

        // 3. 分类和知识点筛选
        rule.setCategoryIds(request.getCategoryIds());
        rule.setKnowledgeIds(request.getKnowledgeIds());

        // 4. 其他配置（题目不足时不报错，使用IGNORE模式）
        rule.setAllowDuplicate(false);
        rule.setFallbackMode(AutoGeneratePaperRule.FallbackMode.IGNORE);

        return rule;
    }

    private void selectQuestionsWithWarnings(AutoGeneratePaperRule rule, 
                                            List<SelectedQuestion> selectedQuestions,
                                            List<String> warnings) {
        Random random = rule.getRandomSeed() != null ? new Random(rule.getRandomSeed()) : new Random();
        boolean allowDuplicate = Boolean.TRUE.equals(rule.getAllowDuplicate());
        Set<Long> usedQuestionIds = new HashSet<>();

        for (Map.Entry<String, AutoGeneratePaperRule.TypeRule> entry : rule.getTypeRules().entrySet()) {
            String typeCode = entry.getKey();
            AutoGeneratePaperRule.TypeRule typeRule = entry.getValue();
            int requiredCount = typeRule.getCount();
            BigDecimal scorePerQuestion = typeRule.getScore() != null
                    ? BigDecimal.valueOf(typeRule.getScore()) : null;

            // 该题型的题目
            LambdaQueryWrapper<Question> q = new LambdaQueryWrapper<>();
            q.eq(Question::getType, typeCode)
                    .eq(Question::getDeleted, 0);
            if (!CollectionUtils.isEmpty(rule.getCategoryIds())) {
                q.in(Question::getCategoryId, rule.getCategoryIds());
            }
            if (!CollectionUtils.isEmpty(rule.getKnowledgeIds())) {
                q.in(Question::getKnowledgeId, rule.getKnowledgeIds());
            }
            List<Question> candidates = questionMapper.selectList(q);

            if (candidates.isEmpty()) {
                String typeName = QuestionType.of(typeCode).getLabel();
                warnings.add(String.format("题型【%s】没有可用的题目，已跳过该题型", typeName));
                continue;
            }

            // 难度分组
            Map<Integer, List<Question>> byDifficulty = candidates.stream()
                    .collect(Collectors.groupingBy(question ->
                            question.getDifficulty() != null ? question.getDifficulty() : 2));

            // 每个难度需要的题量
            Map<Integer, Integer> difficultyNeeds = calculateDifficultyNeeds(
                    rule, requiredCount, byDifficulty.keySet());

            List<Question> selected = new ArrayList<>();

            if (difficultyNeeds != null && !difficultyNeeds.isEmpty()) {
                // 按难度分配
                int totalShortage = 0;

                for (Map.Entry<Integer, Integer> dEntry : difficultyNeeds.entrySet()) {
                    int difficulty = dEntry.getKey();
                    int need = dEntry.getValue();
                    List<Question> pool = byDifficulty.getOrDefault(difficulty, Collections.emptyList());

                    if (!allowDuplicate) {
                        pool = pool.stream()
                                .filter(qq -> !usedQuestionIds.contains(qq.getId()))
                                .collect(Collectors.toList());
                    }

                    List<Question> picked = pickRandom(pool, need, random);
                    selected.addAll(picked);

                    if (picked.size() < need) {
                        totalShortage += (need - picked.size());
                    }
                }

                // 不足的情况
                if (totalShortage > 0) {
                    Set<Long> selectedIds = selected.stream().map(Question::getId).collect(Collectors.toSet());
                    List<Question> remaining = candidates.stream()
                            .filter(qq -> !selectedIds.contains(qq.getId()))
                            .filter(qq -> allowDuplicate || !usedQuestionIds.contains(qq.getId()))
                            .collect(Collectors.toList());

                    List<Question> extra = pickRandom(remaining, totalShortage, random);
                    selected.addAll(extra);
                }
            } else {
                List<Question> pool = allowDuplicate ? candidates :
                        candidates.stream()
                                .filter(qq -> !usedQuestionIds.contains(qq.getId()))
                                .collect(Collectors.toList());
                selected = pickRandom(pool, requiredCount, random);
            }

            if (selected.size() < requiredCount) {
                String typeName = org.development.exam_online.common.enums.QuestionType.of(typeCode).getLabel();
                warnings.add(String.format("题型【%s】需要%d道题，但只找到%d道可用题目", 
                        typeName, requiredCount, selected.size()));
            }

            for (Question sq : selected) {
                usedQuestionIds.add(sq.getId());
                BigDecimal finalScore = scorePerQuestion != null ? scorePerQuestion : sq.getScore();
                if (finalScore == null) finalScore = BigDecimal.ZERO;
                selectedQuestions.add(new SelectedQuestion(sq, finalScore));
            }
        }

        if (selectedQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未能选出任何题目，请检查分类和知识点筛选条件");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveAutoGeneratedPapers(Long paperId, List<Long> questionIds, List<Double> scores) {
        requireActivePaper(paperId);
        if (CollectionUtils.isEmpty(questionIds)) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_QUESTION_IDS_EMPTY);
        }
        // 清空原有题目
        LambdaQueryWrapper<ExamPaperQuestion> clearQ = new LambdaQueryWrapper<>();
        clearQ.eq(ExamPaperQuestion::getPaperId, paperId);
        examPaperQuestionMapper.delete(clearQ);

        // 添加新题目
        for (int i = 0; i < questionIds.size(); i++) {
            ExamPaperQuestion epq = new ExamPaperQuestion();
            epq.setPaperId(paperId);
            epq.setQuestionId(questionIds.get(i));
            BigDecimal score = (scores != null && i < scores.size() && scores.get(i) != null)
                    ? BigDecimal.valueOf(scores.get(i))
                    : BigDecimal.ZERO;
            epq.setQuestionScore(score);
            epq.setQuestionOrder(i + 1);
            examPaperQuestionMapper.insert(epq);
        }
        updatePaperTotalScore(paperId);
        return "自动组卷结果保存成功";
    }

    // ==================== 自动组卷核心逻辑 ====================

    private static class SelectedQuestion {
        Question question;
        BigDecimal score;

        SelectedQuestion(Question question, BigDecimal score) {
            this.question = question;
            this.score = score;
        }
    }

    private Map<Integer, Integer> calculateDifficultyNeeds(
            AutoGeneratePaperRule rule, int totalCount, Set<Integer> availableDifficulties) {

        if (rule.getDifficultyMode() == null) {
            return null; // 不限制难度
        }

        Map<Integer, Integer> needs = new HashMap<>();

        if (rule.getDifficultyMode() == AutoGeneratePaperRule.DifficultyMode.QUOTA) {
            Map<Integer, Integer> quota = rule.getDifficultyQuota();
            if (quota != null) {
                needs.putAll(quota);
            }
        } else if (rule.getDifficultyMode() == AutoGeneratePaperRule.DifficultyMode.RATIO) {
            Map<Integer, Double> ratio = rule.getDifficultyRatio();
            if (ratio != null) {
                int assigned = 0;
                List<Map.Entry<Integer, Double>> entries = new ArrayList<>(ratio.entrySet());
                for (int i = 0; i < entries.size(); i++) {
                    Map.Entry<Integer, Double> e = entries.get(i);
                    int count;
                    if (i == entries.size() - 1) {
                        count = totalCount - assigned;
                    } else {
                        count = (int) Math.round(totalCount * e.getValue());
                    }
                    if (count > 0) {
                        needs.put(e.getKey(), count);
                        assigned += count;
                    }
                }
            }
        }

        return needs;
    }

    private List<Question> pickRandom(List<Question> pool, int count, Random random) {
        if (pool == null || pool.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }
        List<Question> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

//    private Map<String, Object> buildAutoGenerateResult(ExamPaper paper, List<SelectedQuestion> selectedQuestions) {
//        Map<String, Object> result = new HashMap<>();
//        result.put("paperId", paper.getId());
//        result.put("name", paper.getName());
//        result.put("description", paper.getDescription());
//        result.put("duration", paper.getDuration());
//        result.put("buildType", paper.getBuildType());
//        result.put("totalScore", paper.getTotalScore());
//        result.put("totalQuestions", selectedQuestions.size());
//
//        // 题型分布
//        Map<String, Integer> typeDistribution = new LinkedHashMap<>();
//        Map<String, BigDecimal> typeScoreDistribution = new LinkedHashMap<>();
//        // 难度分布
//        Map<Integer, Integer> difficultyDistribution = new TreeMap<>();
//
//        List<Map<String, Object>> questions = new ArrayList<>();
//        int order = 0;
//        for (SelectedQuestion sq : selectedQuestions) {
//            // 统计
//            String type = sq.question.getType();
//            typeDistribution.merge(type, 1, Integer::sum);
//            typeScoreDistribution.merge(type, sq.score, BigDecimal::add);
//            int diff = sq.question.getDifficulty() != null ? sq.question.getDifficulty() : 2;
//            difficultyDistribution.merge(diff, 1, Integer::sum);
//
//            Map<String, Object> m = new HashMap<>();
//            m.put("id", sq.question.getId());
//            m.put("type", type);
//            m.put("stem", sq.question.getStem());
//            m.put("optionsJson", sq.question.getOptionsJson());
//            m.put("answerJson", sq.question.getAnswerJson());
//            m.put("difficulty", sq.question.getDifficulty());
//            m.put("categoryId", sq.question.getCategoryId());
//            m.put("score", sq.score);
//            m.put("order", ++order);
//            questions.add(m);
//        }
//
//        result.put("typeDistribution", typeDistribution);
//        result.put("typeScoreDistribution", typeScoreDistribution);
//        result.put("difficultyDistribution", difficultyDistribution);
//        result.put("questions", questions);
//
//        return result;
//    }

    @Override
    public Map<String, Object> getPaperById(Long paperId) {
        ExamPaper paper = requireActivePaper(paperId);
        Map<String, Object> result = new HashMap<>();
        result.put("paper", paper);
        result.put("questions", getPaperQuestions(paperId));
        return result;
    }

    @Override
    public Map<String, Object> previewPaper(Long paperId) {
        ExamPaper paper = requireActivePaper(paperId);
        List<Map<String, Object>> questions = getPaperQuestions(paperId);
        questions.forEach(q -> q.remove("answerJson"));

        Map<String, Object> result = new HashMap<>();
        result.put("paper", paper);
        result.put("questions", questions);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updatePaper(Long paperId, ExamPaper examPaper) {
        requireActivePaper(paperId);
        if (examPaper == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "试卷信息不能为空");
        }
        if (!StringUtils.hasText(examPaper.getName())) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NAME_EMPTY);
        }
        examPaper.setId(paperId);
        int updated = examPaperMapper.updateById(examPaper);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_UPDATE_FAILED);
        }
        return "试卷信息更新成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deletePaper(Long paperId) {
        requireActivePaper(paperId);
        // 检查是否有关联考试
        LambdaQueryWrapper<Exam> q = new LambdaQueryWrapper<>();
        q.eq(Exam::getPaperId, paperId)
                .eq(Exam::getDeleted, 0);
        Long examCount = examMapper.selectCount(q);
        if (examCount != null && examCount > 0) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_HAS_EXAMS);
        }

        // 删除试卷-题目关联
        LambdaQueryWrapper<ExamPaperQuestion> epqQ = new LambdaQueryWrapper<>();
        epqQ.eq(ExamPaperQuestion::getPaperId, paperId);
        examPaperQuestionMapper.delete(epqQ);

        // 逻辑删除试卷
        LambdaUpdateWrapper<ExamPaper> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ExamPaper::getId, paperId)
                .set(ExamPaper::getDeleted, 1);
        int updated = examPaperMapper.update(null, updateWrapper);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_DELETE_FAILED);
        }
        return "试卷删除成功";
    }

    @Override
    public PageResult<ExamPaper> getPaperList(Integer pageNum, Integer pageSize, String keyword, String type, Long createdBy) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<ExamPaper> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaper::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            q.like(ExamPaper::getName, keyword);
        }
        if (StringUtils.hasText(type)) {
            // type 目前对应 buildType（0手动 1自动），兼容传入字符串
            if ("manual".equalsIgnoreCase(type)) {
                q.eq(ExamPaper::getBuildType, 0);
            } else if ("auto".equalsIgnoreCase(type)) {
                q.eq(ExamPaper::getBuildType, 1);
            } else {
                try {
                    Integer bt = Integer.valueOf(type);
                    q.eq(ExamPaper::getBuildType, bt);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (createdBy != null) {
            q.eq(ExamPaper::getCreatedBy, createdBy);
        }
        q.orderByDesc(ExamPaper::getCreatedAt);

        Page<ExamPaper> page = new Page<>(p, s);
        Page<ExamPaper> result = examPaperMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    public List<Map<String, Object>> getPaperQuestions(Long paperId) {
        requireActivePaper(paperId);
        LambdaQueryWrapper<ExamPaperQuestion> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestion::getPaperId, paperId)
                .orderByAsc(ExamPaperQuestion::getQuestionOrder);
        List<ExamPaperQuestion> epqs = examPaperQuestionMapper.selectList(q);
        if (epqs == null || epqs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = epqs.stream()
                .map(ExamPaperQuestion::getQuestionId)
                .distinct()
                .collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIdsIgnoreDeleted(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q2 -> q2));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamPaperQuestion epq : epqs) {
            Question qst = questionMap.get(epq.getQuestionId());
            if (qst == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", qst.getId());
            m.put("type", qst.getType());
            m.put("stem", qst.getStem());
            m.put("optionsJson", qst.getOptionsJson());
            // 注意：一般预览不应返回答案，调用方可选择忽略 answerJson
            m.put("answerJson", qst.getAnswerJson());
            m.put("analysis", qst.getAnalysis());
            m.put("score", epq.getQuestionScore());
            m.put("difficulty", qst.getDifficulty());
            m.put("categoryId", qst.getCategoryId());
            m.put("knowledgeId", qst.getKnowledgeId());
            m.put("order", epq.getQuestionOrder());
            result.add(m);
        }
        return result;
    }

    @Override
    public Double calculateTotalScore(Long paperId) {
        requireActivePaper(paperId);
        LambdaQueryWrapper<ExamPaperQuestion> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> epqs = examPaperQuestionMapper.selectList(q);
        if (epqs == null || epqs.isEmpty()) {
            return 0.0;
        }
        BigDecimal total = epqs.stream()
                .map(ExamPaperQuestion::getQuestionScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.doubleValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String clearAllQuestions(Long paperId) {
        requireActivePaper(paperId);
        LambdaQueryWrapper<ExamPaperQuestion> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestion::getPaperId, paperId);
        examPaperQuestionMapper.delete(q);
        updatePaperTotalScore(paperId);
        return "已清空试卷中的所有题目";
    }

    private ExamPaper requireActivePaper(Long paperId) {
        if (paperId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "试卷ID不能为空");
        }
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || Objects.equals(paper.getDeleted(), 1)) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NOT_FOUND);
        }
        return paper;
    }

    private void updatePaperTotalScore(Long paperId) {
        LambdaQueryWrapper<ExamPaperQuestion> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> epqs = examPaperQuestionMapper.selectList(q);
        BigDecimal total = BigDecimal.ZERO;
        if (epqs != null) {
            total = epqs.stream()
                    .map(ExamPaperQuestion::getQuestionScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        LambdaUpdateWrapper<ExamPaper> updateWrapper =
                new LambdaUpdateWrapper<>();
        updateWrapper.eq(ExamPaper::getId, paperId)
                .set(ExamPaper::getTotalScore, total);
        examPaperMapper.update(null, updateWrapper);
    }
}

