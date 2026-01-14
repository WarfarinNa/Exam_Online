package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.enums.QuestionType;
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

            // 获取题目分值
            BigDecimal score;
            if (scores != null && i < scores.size() && scores.get(i) != null) {
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
     * 自动组卷
     * @param paperId 试卷ID
     * @param rule 自动组卷规则
     * @return 组卷结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String autoGeneratePaper(Long paperId, AutoGeneratePaperRule rule) {
        // 验证试卷是否存在
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        if (rule == null || rule.getTypeRules() == null || rule.getTypeRules().isEmpty()) {
            throw new RuntimeException("自动组卷规则不能为空");
        }

        // 清空试卷中的现有题目
        clearAllQuestions(paperId);

        // 使用随机种子（如果提供）
        Random random = rule.getRandomSeed() != null 
                ? new Random(rule.getRandomSeed()) 
                : new Random();

        List<Long> selectedQuestionIds = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        // 根据题型规则选择题目
        for (Map.Entry<String, Integer> entry : rule.getTypeRules().entrySet()) {
            String questionType = entry.getKey();
            Integer count = entry.getValue();

            // 验证题型是否有效
            if (!QuestionType.isValid(questionType)) {
                throw new RuntimeException("无效的题型：" + questionType);
            }

            // 构建查询条件
            LambdaQueryWrapper<Question> questionQuery = new LambdaQueryWrapper<>();
            questionQuery.eq(Question::getType, questionType);

            // 分类筛选
            if (rule.getCategoryIds() != null && !rule.getCategoryIds().isEmpty()) {
                questionQuery.in(Question::getCategoryId, rule.getCategoryIds());
            }

            // 查询符合条件的题目
            List<Question> availableQuestions = questionMapper.selectList(questionQuery);

            if (availableQuestions.isEmpty()) {
                throw new RuntimeException("题型 " + questionType + " 没有符合条件的题目");
            }

            if (availableQuestions.size() < count) {
                throw new RuntimeException("题型 " + questionType + " 的题目数量不足，需要 " + count + " 道，但只有 " + availableQuestions.size() + " 道");
            }

            // 随机选择题目
            List<Question> selectedQuestions;
            if (rule.getAllowDuplicate() != null && rule.getAllowDuplicate()) {
                // 允许重复，随机选择
                selectedQuestions = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    selectedQuestions.add(availableQuestions.get(random.nextInt(availableQuestions.size())));
                }
            } else {
                // 不允许重复，随机打乱后选择
                List<Question> shuffled = new ArrayList<>(availableQuestions);
                Collections.shuffle(shuffled, random);
                selectedQuestions = shuffled.subList(0, count);
            }

            // 添加到选中列表
            for (Question question : selectedQuestions) {
                selectedQuestionIds.add(question.getId());
                if (question.getScore() != null) {
                    totalScore = totalScore.add(question.getScore());
                }
            }
        }

        // 批量添加题目到试卷
        for (Long questionId : selectedQuestionIds) {
            Question question = questionMapper.selectById(questionId);
            BigDecimal score = question.getScore() != null ? question.getScore() : BigDecimal.ZERO;

            ExamPaperQuestion paperQuestion = new ExamPaperQuestion();
            paperQuestion.setPaperId(paperId);
            paperQuestion.setQuestionId(questionId);
            paperQuestion.setScore(score);

            examPaperQuestionMapper.insert(paperQuestion);
        }

        // 更新试卷总分
        ExamPaper updatePaper = new ExamPaper();
        updatePaper.setId(paperId);
        updatePaper.setTotalScore(totalScore);
        examPaperMapper.updateById(updatePaper);

        return "自动组卷成功，共添加 " + selectedQuestionIds.size() + " 道题目，试卷总分：" + totalScore;
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
