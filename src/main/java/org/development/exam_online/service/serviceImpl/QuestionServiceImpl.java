package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.alibaba.excel.EasyExcel;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.dto.QuestionExcelDTO;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.dao.entity.QuestionCategory;
import org.development.exam_online.dao.entity.QuestionKnowledge;
import org.development.exam_online.dao.mapper.QuestionCategoryMapper;
import org.development.exam_online.dao.mapper.QuestionKnowledgeMapper;
import org.development.exam_online.dao.mapper.QuestionMapper;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.service.QuestionService;
import org.development.exam_online.service.listener.QuestionImportListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionCategoryMapper questionCategoryMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;

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
        requireActiveQuestion(questionId);

        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Question> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(Question::getId, questionId)
                .set(Question::getDeleted, 1);
        int updated = questionMapper.update(null, updateWrapper);
        
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
    public PageResult<Question> getQuestionList(Integer pageNum, Integer pageSize, String type, Long categoryId, Long knowledgeId, Integer difficulty, String keyword, Long createdBy) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Question> q = buildBaseQuery(type, categoryId, knowledgeId, difficulty, keyword);
        if (createdBy != null) {
            q.eq(Question::getCreatedBy, createdBy);
        }
        q.orderByDesc(Question::getCreatedAt);

        Page<Question> page = new Page<>(p, s);
        Page<Question> result = questionMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    public PageResult<Question> searchQuestions(String keyword, String type, Long categoryId, Long knowledgeId, Integer difficulty, Integer pageNum, Integer pageSize) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Question> q = buildBaseQuery(type, categoryId, knowledgeId, difficulty, keyword);
        q.orderByDesc(Question::getCreatedAt);

        Page<Question> page = new Page<>(p, s);
        Page<Question> result = questionMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importQuestions(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件格式错误，仅支持.xlsx或.xls格式");
        }

        Long currentUserId = AuthContext.getUserId();
        QuestionImportListener listener = new QuestionImportListener();

        try {
            EasyExcel.read(file.getInputStream(), QuestionExcelDTO.class, listener).sheet().doRead();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件读取失败: " + e.getMessage());
        }

        List<String> errors = listener.getErrors();
        List<QuestionExcelDTO> validRows = listener.getValidRows();

        if (validRows.isEmpty()) {
            if (!errors.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, 
                    "导入失败，所有行都存在错误：\n" + String.join("\n", errors));
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel文件中没有有效数据");
        }

        // 批量处理
        int successCount = 0;
        List<String> importErrors = new ArrayList<>(errors);

        for (int i = 0; i < validRows.size(); i++) {
            QuestionExcelDTO dto = validRows.get(i);
            int rowNum = i + 2 + errors.size();
            String prefix = "第" + rowNum + "行: ";

            try {
                // 1. 解析分类和知识点
                CategoryKnowledgeResult ckResult = parseCategoryKnowledge(dto.getCategoryKnowledge(), currentUserId);
                
                // 2. 转换选项为JSON
                String optionsJson = convertOptionsToJson(dto.getType(), dto.getOptions());
                
                // 3. 转换答案为JSON
                String answerJson = convertAnswerToJson(dto.getType(), dto.getAnswer());
                
                // 4. 创建实体
                Question question = new Question();
                question.setType(dto.getType());
                question.setStem(dto.getStem().trim());
                question.setOptionsJson(optionsJson);
                question.setAnswerJson(answerJson);
                question.setAnalysis(dto.getAnalysis());
                question.setScore(BigDecimal.valueOf(dto.getScore()));
                question.setDifficulty(Integer.parseInt(dto.getDifficulty()));
                question.setCategoryId(ckResult.categoryId);
                question.setKnowledgeId(ckResult.knowledgeId);
                question.setCreatedBy(currentUserId);
                question.setDeleted(0);
                
                // 5. 插入数据库
                questionMapper.insert(question);
                successCount++;
                
            } catch (Exception e) {
                importErrors.add(prefix + e.getMessage());
            }
        }


        StringBuilder result = new StringBuilder();
        result.append("导入完成！成功导入 ").append(successCount).append(" 条题目");
        if (!importErrors.isEmpty()) {
            result.append("，").append(importErrors.size()).append(" 条失败：\n");
        }
        
        return result.toString();
    }

    public void exportTemplateFile(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("题目导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<QuestionExcelDTO> data = new ArrayList<>();
            
            // 单选
            QuestionExcelDTO single = new QuestionExcelDTO();
            single.setType("单选题");
            single.setStem("Java中哪个关键字用于定义常量？");
            single.setOptions("A.const;B.final;C.static;D.constant");
            single.setAnswer("B");
            single.setAnalysis("final关键字用于定义常量");
            single.setScore(2.0);
            single.setDifficulty("简单");
            single.setCategoryKnowledge("Java/基础语法");
            data.add(single);

            // 多选
            QuestionExcelDTO multiple = new QuestionExcelDTO();
            multiple.setType("多选题");
            multiple.setStem("以下哪些是Java集合框架的接口？");
            multiple.setOptions("A.List;B.Set;C.Map;D.Collection");
            multiple.setAnswer("A,B,C,D");
            multiple.setAnalysis("List、Set、Map、Collection都是Java集合框架的核心接口");
            multiple.setScore(3.0);
            multiple.setDifficulty("普通");
            multiple.setCategoryKnowledge("Java/集合");
            data.add(multiple);

            // 判断
            QuestionExcelDTO judge = new QuestionExcelDTO();
            judge.setType("判断题");
            judge.setStem("Java中int是对象类型");
            judge.setOptions("");
            judge.setAnswer("错误");
            judge.setAnalysis("int是基本数据类型，Integer才是对象类型");
            judge.setScore(1.0);
            judge.setDifficulty("简单");
            judge.setCategoryKnowledge("Java/数据类型");
            data.add(judge);

            // 填空
            QuestionExcelDTO blank = new QuestionExcelDTO();
            blank.setType("填空题");
            blank.setStem("Java中有___种基本数据类型，分别是___、___、___等");
            blank.setOptions("");
            blank.setAnswer("8;byte;short;int");
            blank.setAnalysis("Java有8种基本数据类型");
            blank.setScore(2.0);
            blank.setDifficulty("普通");
            blank.setCategoryKnowledge("Java/数据类型");
            data.add(blank);

            // 简答
            QuestionExcelDTO shortAnswer = new QuestionExcelDTO();
            shortAnswer.setType("简答题");
            shortAnswer.setStem("请简述Java中ArrayList和LinkedList的区别");
            shortAnswer.setOptions("");
            shortAnswer.setAnswer("ArrayList基于数组实现，查询快；LinkedList基于链表实现，插入删除快");
            shortAnswer.setAnalysis("考查对集合底层实现的理解");
            shortAnswer.setScore(10.0);
            shortAnswer.setDifficulty("困难");
            shortAnswer.setCategoryKnowledge("Java/集合");
            data.add(shortAnswer);

            EasyExcel.write(response.getOutputStream(), QuestionExcelDTO.class)
                    .sheet("题目导入模板")
                    .doWrite(data);
                    
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模板导出失败: " + e.getMessage());
        }
    }

    private static class CategoryKnowledgeResult {
        Long categoryId;
        Long knowledgeId;
    }

    private CategoryKnowledgeResult parseCategoryKnowledge(String categoryKnowledge, Long createdBy) {
        CategoryKnowledgeResult result = new CategoryKnowledgeResult();
        
        if (!StringUtils.hasText(categoryKnowledge)) {
            return result; // 允许为空
        }

        String trimmed = categoryKnowledge.trim();
        String[] parts = trimmed.split("/");
        
        if (parts.length == 0) {
            return result;
        }

        // 解析分类名
        String categoryName = parts[0].trim();
        if (StringUtils.hasText(categoryName)) {
            result.categoryId = getOrCreateCategory(categoryName, createdBy);
        }

        // 解析知识点名
        if (parts.length > 1) {
            String knowledgeName = parts[1].trim();
            if (StringUtils.hasText(knowledgeName) && result.categoryId != null) {
                result.knowledgeId = getOrCreateKnowledge(result.categoryId, knowledgeName, createdBy);
            }
        }

        return result;
    }

    private Long getOrCreateCategory(String name, Long createdBy) {
        LambdaQueryWrapper<QuestionCategory> q = new LambdaQueryWrapper<>();
        q.eq(QuestionCategory::getName, name)
         .eq(QuestionCategory::getDeleted, 0)
         .last("LIMIT 1");
        QuestionCategory existing = questionCategoryMapper.selectOne(q);
        
        if (existing != null) {
            return existing.getId();
        }

        // 创建新分类
        QuestionCategory category = new QuestionCategory();
        category.setName(name);
        category.setCreatedBy(createdBy);
        category.setDeleted(0);
        questionCategoryMapper.insert(category);
        return category.getId();
    }

    private Long getOrCreateKnowledge(Long categoryId, String name, Long createdBy) {
        LambdaQueryWrapper<QuestionKnowledge> q = new LambdaQueryWrapper<>();
        q.eq(QuestionKnowledge::getCategoryId, categoryId)
         .eq(QuestionKnowledge::getName, name)
         .eq(QuestionKnowledge::getDeleted, 0)
         .last("LIMIT 1");
        QuestionKnowledge existing = questionKnowledgeMapper.selectOne(q);
        
        if (existing != null) {
            return existing.getId();
        }

        // 创建新知识点
        QuestionKnowledge knowledge = new QuestionKnowledge();
        knowledge.setCategoryId(categoryId);
        knowledge.setName(name);
        knowledge.setCreatedBy(createdBy);
        knowledge.setDeleted(0);
        questionKnowledgeMapper.insert(knowledge);
        return knowledge.getId();
    }

    private String convertOptionsToJson(String typeCode, String options) {
        if (!StringUtils.hasText(options)) {
            return null;
        }

        String[] parts = options.split("[;；]");
        Map<String, String> optionMap = new LinkedHashMap<>();
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            int sepIdx = -1;
            for (int i = 0; i < Math.min(trimmed.length(), 4); i++) {
                char c = trimmed.charAt(i);
                if (c == '.' || c == ':' || c == '、' || c == '．' || c == '：') {
                    sepIdx = i;
                    break;
                }
            }

            String key, value;
            if (sepIdx > 0) {
                key = trimmed.substring(0, sepIdx).trim();
                value = trimmed.substring(sepIdx + 1).trim();
            } else {
                key = String.valueOf((char) ('A' + optionMap.size()));
                value = trimmed;
            }
            optionMap.put(key, value);
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(optionMap);
        } catch (Exception e) {
            throw new RuntimeException("选项格式转换失败: " + e.getMessage());
        }
    }

    private String convertAnswerToJson(String typeCode, String answer) {
        if (!StringUtils.hasText(answer)) {
            throw new RuntimeException("答案不能为空");
        }
        
        String trimmed = answer.trim();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            
            switch (typeCode) {
                case "single":
                    return mapper.writeValueAsString(trimmed);

                case "multiple":
                    String[] choices = trimmed.split("[,;，；、]");
                    List<String> choiceList = Arrays.stream(choices)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    return mapper.writeValueAsString(choiceList);

                case "judge":
                    String lower = trimmed.toLowerCase();
                    if ("true".equals(lower) || "正确".equals(trimmed) || "对".equals(trimmed) || "是".equals(trimmed)) {
                        return "true";
                    } else if ("false".equals(lower) || "错误".equals(trimmed) || "错".equals(trimmed) || "否".equals(trimmed)) {
                        return "false";
                    } else {
                        throw new RuntimeException("判断题答案格式错误，支持：true/false/正确/错误/对/错");
                    }

                case "blank":
                    String[] blanks = trimmed.split("[;；]");
                    List<String> blankList = Arrays.stream(blanks)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    return mapper.writeValueAsString(blankList);

                case "short":
                    return mapper.writeValueAsString(trimmed);

                default:
                    throw new RuntimeException("未知题型: " + typeCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("答案格式转换失败: " + e.getMessage());
        }
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

    private LambdaQueryWrapper<Question> buildBaseQuery(String type, Long categoryId, Long knowledgeId, Integer difficulty, String keyword) {
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
        if (knowledgeId != null) {
            q.eq(Question::getKnowledgeId, knowledgeId);
        }
        if (difficulty != null) {
            q.eq(Question::getDifficulty, difficulty);
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

