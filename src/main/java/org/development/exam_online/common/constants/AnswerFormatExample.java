package org.development.exam_online.common.constants;

/**
 * 答案格式示例常量类
 * 用于统一题库管理和考试答题的答案数据格式
 * 
 * 【数据存储格式】：
 * - question.answer_json：题目正确答案（JSON字符串）
 * - exam_answer.user_answer：学生答案（JSON字符串）
 * 
 * 【前端提交格式】：
 * 前端提交原始数据类型，后端自动转换为JSON字符串存储
 */
public class AnswerFormatExample {
    
    /**
     * 单选题答案格式
     * 
     * 前端提交：
     * {
     *   "answer": "A"
     * }
     * 
     * 数据库存储（answer_json/user_answer）：
     * "A"
     * 
     * 注意：存储的是JSON字符串，包含引号
     */
    public static final String SINGLE_CHOICE_EXAMPLE = "\"A\"";
    
    /**
     * 多选题答案格式
     * 
     * 前端提交：
     * {
     *   "answer": ["A", "B", "C"]
     * }
     * 
     * 数据库存储（answer_json/user_answer）：
     * ["A","B","C"]
     * 
     * 注意：存储的是JSON数组字符串
     */
    public static final String MULTIPLE_CHOICE_EXAMPLE = "[\"A\",\"B\",\"C\"]";
    
    /**
     * 判断题答案格式
     * 
     * 前端提交：
     * {
     *   "answer": true
     * }
     * 
     * 数据库存储（answer_json/user_answer）：
     * true
     * 
     * 注意：存储的是JSON布尔值字符串（不带引号）
     */
    public static final String JUDGE_TRUE_EXAMPLE = "true";
    public static final String JUDGE_FALSE_EXAMPLE = "false";
    
    /**
     * 填空题答案格式（多个空）
     * 
     * 前端提交：
     * {
     *   "answer": ["答案1", "答案2", "答案3"]
     * }
     * 
     * 数据库存储（answer_json/user_answer）：
     * ["答案1","答案2","答案3"]
     * 
     * 注意：
     * - 存储的是JSON数组字符串
     * - 数组顺序必须与题目中的空格顺序一致
     * - 判分时会严格比对每个空的答案
     */
    public static final String BLANK_EXAMPLE = "[\"答案1\",\"答案2\",\"答案3\"]";
    
    /**
     * 简答题答案格式
     * 
     * 前端提交：
     * {
     *   "answer": "这是简答题的答案内容，可以是多行文本。"
     * }
     * 
     * 数据库存储（answer_json/user_answer）：
     * "这是简答题的答案内容，可以是多行文本。"
     * 
     * 注意：
     * - 存储的是JSON字符串
     * - 简答题需要教师手动评分，不进行自动判分
     */
    public static final String SHORT_ANSWER_EXAMPLE = "\"这是简答题的答案内容，可以是多行文本。\"";
    
    /**
     * 判分规则说明
     */
    public static final String GRADING_RULES = 
        "判分规则：\n" +
        "1. 客观题（单选、多选、判断、填空）：\n" +
        "   - 去除空格后比较 question.answer_json 和 exam_answer.user_answer\n" +
        "   - 完全匹配则得满分，否则0分\n" +
        "   - 使用试卷中设置的分值（exam_paper_question.question_score）\n" +
        "\n" +
        "2. 主观题（简答）：\n" +
        "   - 需要教师手动评分\n" +
        "   - 教师可以给0到满分之间的任意分数\n" +
        "\n" +
        "3. 教师可以对任何题目（包括客观题）进行手动评分，覆盖自动判分结果";
    
    /**
     * 前端提交示例（完整）
     */
    public static final String FRONTEND_SUBMIT_EXAMPLE = 
        "{\n" +
        "  \"answers\": {\n" +
        "    \"1\": \"A\",                    // 单选题\n" +
        "    \"2\": [\"A\", \"B\", \"C\"],      // 多选题\n" +
        "    \"3\": true,                    // 判断题\n" +
        "    \"4\": [\"答案1\", \"答案2\"],     // 填空题\n" +
        "    \"5\": \"这是简答题的答案\"        // 简答题\n" +
        "  }\n" +
        "}";
}
