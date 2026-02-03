package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.dao.entity.QuestionCategory;
import org.development.exam_online.dao.entity.QuestionKnowledge;
import org.development.exam_online.dao.mapper.QuestionCategoryMapper;
import org.development.exam_online.dao.mapper.QuestionKnowledgeMapper;
import org.development.exam_online.dao.mapper.QuestionMapper;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.service.QuestionKnowledgeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuestionKnowledgeServiceImpl implements QuestionKnowledgeService {

    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final QuestionCategoryMapper questionCategoryMapper;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionKnowledge createKnowledge(QuestionKnowledge knowledge) {
        if (knowledge == null || !StringUtils.hasText(knowledge.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识点名称不能为空");
        }
        if (knowledge.getCategoryId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "所属分类不能为空");
        }
        QuestionCategory category = questionCategoryMapper.selectById(knowledge.getCategoryId());
        if (category == null || !Objects.equals(category.getDeleted(), 0)) {
            throw new BusinessException(ErrorCode.QUESTION_CATEGORY_NOT_FOUND);
        }

        // 同一分类下知识点名称唯一
        LambdaQueryWrapper<QuestionKnowledge> q = new LambdaQueryWrapper<>();
        q.eq(QuestionKnowledge::getDeleted, 0)
                .eq(QuestionKnowledge::getCategoryId, knowledge.getCategoryId())
                .eq(QuestionKnowledge::getName, knowledge.getName());
        Long count = questionKnowledgeMapper.selectCount(q);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "该分类下知识点名称已存在");
        }

        knowledge.setId(null);
        knowledge.setDeleted(0);
        if (knowledge.getCreatedBy() == null) {
            Long currentUserId = AuthContext.getUserId();
            if (currentUserId != null) {
                knowledge.setCreatedBy(currentUserId);
            }
        }

        int inserted = questionKnowledgeMapper.insert(knowledge);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "创建知识点失败");
        }
        return questionKnowledgeMapper.selectById(knowledge.getId());
    }

    @Override
    public List<QuestionKnowledge> getKnowledgeList(Long categoryId) {
        LambdaQueryWrapper<QuestionKnowledge> q = new LambdaQueryWrapper<>();
        q.eq(QuestionKnowledge::getDeleted, 0);
        if (categoryId != null) {
            q.eq(QuestionKnowledge::getCategoryId, categoryId);
        }
        q.orderByAsc(QuestionKnowledge::getCategoryId).orderByAsc(QuestionKnowledge::getId);
        return questionKnowledgeMapper.selectList(q);
    }

    @Override
    public QuestionKnowledge getKnowledgeById(Long id) {
        return requireActiveKnowledge(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateKnowledge(Long id, QuestionKnowledge knowledge) {
        QuestionKnowledge existing = requireActiveKnowledge(id);
        if (knowledge == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识点信息不能为空");
        }

        if (knowledge.getCategoryId() != null && !knowledge.getCategoryId().equals(existing.getCategoryId())) {
            QuestionCategory category = questionCategoryMapper.selectById(knowledge.getCategoryId());
            if (category == null || !Objects.equals(category.getDeleted(), 0)) {
                throw new BusinessException(ErrorCode.QUESTION_CATEGORY_NOT_FOUND);
            }
        }

        if (StringUtils.hasText(knowledge.getName()) &&
                (!knowledge.getName().equals(existing.getName()) ||
                        (knowledge.getCategoryId() != null && !knowledge.getCategoryId().equals(existing.getCategoryId())))) {
            Long newCategoryId = knowledge.getCategoryId() != null ? knowledge.getCategoryId() : existing.getCategoryId();
            LambdaQueryWrapper<QuestionKnowledge> q = new LambdaQueryWrapper<>();
            q.eq(QuestionKnowledge::getDeleted, 0)
                    .eq(QuestionKnowledge::getCategoryId, newCategoryId)
                    .eq(QuestionKnowledge::getName, knowledge.getName());
            Long count = questionKnowledgeMapper.selectCount(q);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT.getCode(), "该分类下知识点名称已存在");
            }
        }

        knowledge.setId(id);
        int updated = questionKnowledgeMapper.updateById(knowledge);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "更新知识点失败");
        }
        return "更新成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteKnowledge(Long id) {
        QuestionKnowledge existing = requireActiveKnowledge(id);

        // 检查是否有题目引用该知识点
        LambdaQueryWrapper<Question> q = new LambdaQueryWrapper<>();
        q.eq(Question::getDeleted, 0)
                .eq(Question::getKnowledgeId, existing.getId());
        Long count = questionMapper.selectCount(q);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "该知识点下存在题目，无法删除");
        }

        QuestionKnowledge update = new QuestionKnowledge();
        update.setId(existing.getId());
        update.setDeleted(1);
        int updated = questionKnowledgeMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "删除知识点失败");
        }
        return "删除成功";
    }

    private QuestionKnowledge requireActiveKnowledge(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识点ID不能为空");
        }
        QuestionKnowledge knowledge = questionKnowledgeMapper.selectById(id);
        if (knowledge == null || !Objects.equals(knowledge.getDeleted(), 0)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "知识点不存在");
        }
        return knowledge;
    }
}

