package org.development.exam_online.service.grading;

import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.entity.ExamPaperQuestion;
import org.development.exam_online.dao.entity.Question;

import java.math.BigDecimal;

public class GradingHelper {

    public static boolean isObjectiveQuestion(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        return QuestionType.SINGLE.getCode().equals(type)
                || QuestionType.MULTIPLE.getCode().equals(type)
                || QuestionType.JUDGE.getCode().equals(type)
                || QuestionType.BLANK.getCode().equals(type);
    }

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

    public static BigDecimal calculateObjectiveScore(String correctAnswer, String userAnswer, BigDecimal fullScore) {
        if (isAnswerCorrect(correctAnswer, userAnswer)) {
            return fullScore;
        }
        return BigDecimal.ZERO;
    }
}
