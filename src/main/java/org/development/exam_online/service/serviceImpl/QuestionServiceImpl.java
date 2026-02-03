package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.dao.entity.QuestionCategory;
import org.development.exam_online.dao.mapper.QuestionCategoryMapper;
import org.development.exam_online.dao.mapper.QuestionMapper;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.service.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionCategoryMapper questionCategoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Question createQuestion(Question question) {
        validateQuestionForCreateOrUpdate(question);

        question.setId(null);
        question.setDeleted(0);
        if (question.getCreatedBy() == null) {
            Long currentUserId = AuthContext.getUserId();
            if (currentUserId != null) {
                question.setCreatedBy(currentUserId);
            }
        }

        int inserted = questionMapper.insert(question);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "创建题目失败");
        }
        return questionMapper.selectById(question.getId());
    }

    @Override
    public Question getQuestionById(Long questionId) {
        Question question = requireActiveQuestion(questionId);
        return question;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateQuestion(Long questionId, Question question) {
        requireActiveQuestion(questionId);
        validateQuestionForCreateOrUpdate(question);

        question.setId(questionId);
        int updated = questionMapper.updateById(question);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "更新题目失败");
        }
        return "更新成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteQuestion(Long questionId) {
        Question existing = requireActiveQuestion(questionId);
        Question update = new Question();
        update.setId(existing.getId());
        update.setDeleted(1);
        int updated = questionMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "删除题目失败");
        }
        return "删除成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "题目ID列表不能为空");
        }
        for (Long id : questionIds) {
            if (id != null) {
                deleteQuestion(id);
            }
        }
        return "批量删除成功";
    }

    @Override
    public PageResult<Question> getQuestionList(Integer pageNum, Integer pageSize, String type, Long categoryId, String keyword, Long createdBy) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Question> q = buildBaseQuery(type, categoryId, keyword);
        if (createdBy != null) {
            q.eq(Question::getCreatedBy, createdBy);
        }
        q.orderByDesc(Question::getCreatedAt);

        Page<Question> page = new Page<>(p, s);
        Page<Question> result = questionMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    public PageResult<Question> searchQuestions(String keyword, String type, Long categoryId, Integer pageNum, Integer pageSize) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Question> q = buildBaseQuery(type, categoryId, keyword);
        q.orderByDesc(Question::getCreatedAt);

        Page<Question> page = new Page<>(p, s);
        Page<Question> result = questionMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    public String importQuestions(MultipartFile file) {
        // 这里可以后续扩展为解析 Excel 的真实导入逻辑
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        // 为避免引入额外依赖，这里暂返回提示信息
        return "批量导入功能暂未实现，请后续补充 Excel 解析逻辑";
    }

    @Override
    public String exportTemplate() {
        // 可返回一个前端可用的说明或静态模板地址，这里先返回简单提示
        return "题目导入模板请参考文档：包含列 type, stem, options_json, answer_json, analysis, score, difficulty, category_id, knowledge_id";
    }

    @Override
    public PageResult<Question> getQuestionsByCategory(Long categoryId, Integer pageNum, Integer pageSize) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类ID不能为空");
        }
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Question> q = new LambdaQueryWrapper<>();
        q.eq(Question::getDeleted, 0)
                .eq(Question::getCategoryId, categoryId)
                .orderByDesc(Question::getCreatedAt);

        Page<Question> page = new Page<>(p, s);
        Page<Question> result = questionMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    public PageResult<Question> getQuestionsByType(String type, Integer pageNum, Integer pageSize) {
        if (!StringUtils.hasText(type) || !QuestionType.isValid(type)) {
            throw new BusinessException(ErrorCode.QUESTION_TYPE_INVALID, "题型代码无效");
        }
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Question> q = new LambdaQueryWrapper<>();
        q.eq(Question::getDeleted, 0)
                .eq(Question::getType, type)
                .orderByDesc(Question::getCreatedAt);

        Page<Question> page = new Page<>(p, s);
        Page<Question> result = questionMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    private LambdaQueryWrapper<Question> buildBaseQuery(String type, Long categoryId, String keyword) {
        LambdaQueryWrapper<Question> q = new LambdaQueryWrapper<>();
        q.eq(Question::getDeleted, 0);

        if (StringUtils.hasText(type)) {
            if (!QuestionType.isValid(type)) {
                throw new BusinessException(ErrorCode.QUESTION_TYPE_INVALID, "题型代码无效");
            }
            q.eq(Question::getType, type);
        }
        if (categoryId != null) {
            q.eq(Question::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            q.like(Question::getStem, keyword);
        }
        return q;
    }

    private void validateQuestionForCreateOrUpdate(Question question) {
        if (question == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "题目信息不能为空");
        }
        if (!StringUtils.hasText(question.getType()) || !QuestionType.isValid(question.getType())) {
            throw new BusinessException(ErrorCode.QUESTION_TYPE_INVALID, "题型代码无效");
        }
        if (!StringUtils.hasText(question.getStem())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "题干不能为空");
        }
        BigDecimal score = question.getScore();
        if (score == null || score.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分值必须大于0");
        }
        Integer difficulty = question.getDifficulty();
        if (difficulty == null || difficulty < 1 || difficulty > 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "难度必须为1-3");
        }
        if (question.getCategoryId() != null) {
            QuestionCategory category = questionCategoryMapper.selectById(question.getCategoryId());
            if (category == null || !Objects.equals(category.getDeleted(), 0)) {
                throw new BusinessException(ErrorCode.QUESTION_CATEGORY_NOT_FOUND);
            }
        }
        // optionsJson / answerJson 格式校验可以后续扩展（如根据题型做 JSON 结构校验）
    }

    private Question requireActiveQuestion(Long questionId) {
        if (questionId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "题目ID不能为空");
        }
        Question question = questionMapper.selectById(questionId);
        if (question == null || !Objects.equals(question.getDeleted(), 0)) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        return question;
    }
}

