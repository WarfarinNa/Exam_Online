package org.development.exam_online.service.grading;

import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.entity.ExamPaperQuestion;
import org.development.exam_online.dao.entity.Question;

import java.math.BigDecimal;

/**
 * 判分辅助类
 * 提供判分相关的工具方法
 */
public class GradingHelper {
    
    /**
     * 判断题目是否为客观题
     * 客观题包括：单选题、多选题、判断题、填空题
     * 
     * @param type 题目类型
     * @return true-客观题，false-主观题
     */
    public static boolean isObjectiveQuestion(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        return QuestionType.SINGLE.getCode().equals(type)
                || QuestionType.MULTIPLE.getCode().equals(type)
                || QuestionType.JUDGE.getCode().equals(type)
                || QuestionType.BLANK.getCode().equals(type);
    }
    
    /**
     * 判断题目是否为主观题
     * 主观题包括：简答题
     * 
     * @param type 题目类型
     * @return true-主观题，false-客观题
     */
    public static boolean isSubjectiveQuestion(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        return QuestionType.SHORT.getCode().equals(type);
    }
    
    /**
     * 获取题目在试卷中的分值
     * 优先使用试卷中设置的分值，如果没有则使用题目默认分值
     * 
     * @param epq 试卷题目关联
     * @param question 题目
     * @return 题目分值
     */
    public static BigDecimal getQuestionScore(ExamPaperQuestion epq, Question question) {
        // 优先使用试卷中设置的分值
        if (epq != null && epq.getQuestionScore() != null) {
            return epq.getQuestionScore();
        }
        // 其次使用题目默认分值
        if (question != null && question.getScore() != null) {
            return question.getScore();
        }
        // 默认0分
        return BigDecimal.ZERO;
    }
    
    /**
     * 判断客观题答案是否正确
     * 
     * @param correctAnswer 正确答案（JSON格式）
     * @param userAnswer 用户答案（JSON格式）
     * @return true-正确，false-错误
     */
    public static boolean isAnswerCorrect(String correctAnswer, String userAnswer) {
        if (correctAnswer == null && userAnswer == null) {
            return true;
        }
        if (correctAnswer == null || userAnswer == null) {
            return false;
        }
        // 去除空格后比较
        return correctAnswer.trim().equals(userAnswer.trim());
    }
    
    /**
     * 计算客观题得分
     * 
     * @param correctAnswer 正确答案
     * @param userAnswer 用户答案
     * @param fullScore 满分
     * @return 得分
     */
    public static BigDecimal calculateObjectiveScore(String correctAnswer, String userAnswer, BigDecimal fullScore) {
        if (isAnswerCorrect(correctAnswer, userAnswer)) {
            return fullScore;
        }
        return BigDecimal.ZERO;
    }
}
