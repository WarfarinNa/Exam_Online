package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
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
        // 构建方式：0手动 1自动，未传则默认为手动
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
        // 查询题目并校验
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        if (questions == null || questions.size() != questionIds.size()) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_QUESTION_NOT_FOUND);
        }

        // 现有顺序基础上继续
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

            // 避免重复添加
            boolean already = existing.stream().anyMatch(epq -> epq.getQuestionId().equals(qId));
            if (already) continue;

            ExamPaperQuestion epq = new ExamPaperQuestion();
            epq.setPaperId(paperId);
            epq.setQuestionId(qId);
            // 分值：优先 scores，其次题目默认分值
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

        // 更新试卷总分
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

    @Override
    public List<Map<String, Object>> previewAutoGeneratedPapers(AutoGeneratePaperRule rule) {
        // 按需求：自动组卷暂不实现，这里返回明确提示
        throw new BusinessException(ErrorCode.AUTO_GENERATE_RULE_EMPTY.getCode(), "自动组卷功能暂未实现");
    }

    @Override
    public String saveAutoGeneratedPapers(Long paperId, List<Long> questionIds, List<Double> scores) {
        // 同上，明确提示前端
        throw new BusinessException(ErrorCode.AUTO_GENERATE_RULE_EMPTY.getCode(), "自动组卷功能暂未实现，无法保存结果");
    }

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
        // 不返回答案；Question 中 answerJson 字段前端自行忽略或后端去掉
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
        ExamPaper update = new ExamPaper();
        update.setId(paperId);
        update.setDeleted(1);
        int updated = examPaperMapper.updateById(update);
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
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
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
        ExamPaper update = new ExamPaper();
        update.setId(paperId);
        update.setTotalScore(total);
        examPaperMapper.updateById(update);
    }
}

