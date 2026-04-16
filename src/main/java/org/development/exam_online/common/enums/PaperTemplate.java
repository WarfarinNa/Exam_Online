package org.development.exam_online.common.enums;

import lombok.Getter;
import org.development.exam_online.dao.dto.AutoGeneratePaperRule;

import java.util.LinkedHashMap;
import java.util.Map;


@Getter
public enum PaperTemplate {

    QUIZ("随堂练习", "适用于课堂即时检测，快速了解学生掌握情况", 20, 40) {
        @Override
        public Map<String, AutoGeneratePaperRule.TypeRule> getTypeRules() {
            Map<String, AutoGeneratePaperRule.TypeRule> rules = new LinkedHashMap<>();
            AutoGeneratePaperRule.TypeRule single = new AutoGeneratePaperRule.TypeRule();
            single.setCount(10);
            single.setScore(2.0);
            rules.put("single", single);

            AutoGeneratePaperRule.TypeRule multiple = new AutoGeneratePaperRule.TypeRule();
            multiple.setCount(5);
            multiple.setScore(2.0);
            rules.put("multiple", multiple);

            AutoGeneratePaperRule.TypeRule judge = new AutoGeneratePaperRule.TypeRule();
            judge.setCount(5);
            judge.setScore(2.0);
            rules.put("judge", judge);
            
            return rules;
        }

        @Override
        public Map<Integer, Double> getDefaultDifficultyRatio() {
            Map<Integer, Double> ratio = new LinkedHashMap<>();
            ratio.put(1, 0.6);
            ratio.put(2, 0.3);
            ratio.put(3, 0.1);
            return ratio;
        }
    },

    UNIT_TEST("单元测试", "适用于单元知识点检测，覆盖本单元重点内容", 30, 60) {
        @Override
        public Map<String, AutoGeneratePaperRule.TypeRule> getTypeRules() {
            Map<String, AutoGeneratePaperRule.TypeRule> rules = new LinkedHashMap<>();
            AutoGeneratePaperRule.TypeRule single = new AutoGeneratePaperRule.TypeRule();
            single.setCount(12);
            single.setScore(2.0);
            rules.put("single", single);

            AutoGeneratePaperRule.TypeRule multiple = new AutoGeneratePaperRule.TypeRule();
            multiple.setCount(6);
            multiple.setScore(3.0);
            rules.put("multiple", multiple);

            AutoGeneratePaperRule.TypeRule judge = new AutoGeneratePaperRule.TypeRule();
            judge.setCount(8);
            judge.setScore(1.0);
            rules.put("judge", judge);

            AutoGeneratePaperRule.TypeRule shortAnswer = new AutoGeneratePaperRule.TypeRule();
            shortAnswer.setCount(2);
            shortAnswer.setScore(5.0);
            rules.put("short", shortAnswer);
            
            return rules;
        }

        @Override
        public Map<Integer, Double> getDefaultDifficultyRatio() {
            Map<Integer, Double> ratio = new LinkedHashMap<>();
            ratio.put(1, 0.5);
            ratio.put(2, 0.4);
            ratio.put(3, 0.1);
            return ratio;
        }
    },

    MID_EXAM("期中考试", "适用于半学期综合测试，考查前半学期所学内容", 40, 80) {
        @Override
        public Map<String, AutoGeneratePaperRule.TypeRule> getTypeRules() {
            Map<String, AutoGeneratePaperRule.TypeRule> rules = new LinkedHashMap<>();
            AutoGeneratePaperRule.TypeRule single = new AutoGeneratePaperRule.TypeRule();
            single.setCount(15);
            single.setScore(2.0);
            rules.put("single", single);

            AutoGeneratePaperRule.TypeRule multiple = new AutoGeneratePaperRule.TypeRule();
            multiple.setCount(8);
            multiple.setScore(3.0);
            rules.put("multiple", multiple);

            AutoGeneratePaperRule.TypeRule judge = new AutoGeneratePaperRule.TypeRule();
            judge.setCount(10);
            judge.setScore(1.0);
            rules.put("judge", judge);

            AutoGeneratePaperRule.TypeRule blank = new AutoGeneratePaperRule.TypeRule();
            blank.setCount(3);
            blank.setScore(2.0);
            rules.put("blank", blank);

            AutoGeneratePaperRule.TypeRule shortAnswer = new AutoGeneratePaperRule.TypeRule();
            shortAnswer.setCount(2);
            shortAnswer.setScore(5.0);
            rules.put("short", shortAnswer);
            
            return rules;
        }

        @Override
        public Map<Integer, Double> getDefaultDifficultyRatio() {
            Map<Integer, Double> ratio = new LinkedHashMap<>();
            ratio.put(1, 0.4);
            ratio.put(2, 0.5);
            ratio.put(3, 0.1);
            return ratio;
        }
    },

    FINAL_EXAM("期末考试", "适用于学期综合测试，全面考查本学期所学内容", 50, 100) {
        @Override
        public Map<String, AutoGeneratePaperRule.TypeRule> getTypeRules() {
            Map<String, AutoGeneratePaperRule.TypeRule> rules = new LinkedHashMap<>();
            AutoGeneratePaperRule.TypeRule single = new AutoGeneratePaperRule.TypeRule();
            single.setCount(20);
            single.setScore(2.0);
            rules.put("single", single);

            AutoGeneratePaperRule.TypeRule multiple = new AutoGeneratePaperRule.TypeRule();
            multiple.setCount(10);
            multiple.setScore(3.0);
            rules.put("multiple", multiple);

            AutoGeneratePaperRule.TypeRule judge = new AutoGeneratePaperRule.TypeRule();
            judge.setCount(10);
            judge.setScore(1.0);
            rules.put("judge", judge);

            AutoGeneratePaperRule.TypeRule blank = new AutoGeneratePaperRule.TypeRule();
            blank.setCount(5);
            blank.setScore(2.0);
            rules.put("blank", blank);

            AutoGeneratePaperRule.TypeRule shortAnswer = new AutoGeneratePaperRule.TypeRule();
            shortAnswer.setCount(2);
            shortAnswer.setScore(5.0);
            rules.put("short", shortAnswer);
            
            return rules;
        }

        @Override
        public Map<Integer, Double> getDefaultDifficultyRatio() {
            Map<Integer, Double> ratio = new LinkedHashMap<>();
            ratio.put(1, 0.3);
            ratio.put(2, 0.5);
            ratio.put(3, 0.2);
            return ratio;
        }
    };

    private final String code;

    private final String name;

    private final String description;

    private final int defaultTotalQuestions;

    private final int defaultTotalScore;

    PaperTemplate(String name, String description, int defaultTotalQuestions, int defaultTotalScore) {
        this.code = name().toLowerCase();
        this.name = name;
        this.description = description;
        this.defaultTotalQuestions = defaultTotalQuestions;
        this.defaultTotalScore = defaultTotalScore;
    }

    public abstract Map<String, AutoGeneratePaperRule.TypeRule> getTypeRules();

    public abstract Map<Integer, Double> getDefaultDifficultyRatio();

    public static PaperTemplate fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PaperTemplate template : values()) {
            if (template.code.equalsIgnoreCase(code) || template.name().equalsIgnoreCase(code)) {
                return template;
            }
        }
        throw new IllegalArgumentException("未知的模板代码: " + code);
    }


}
