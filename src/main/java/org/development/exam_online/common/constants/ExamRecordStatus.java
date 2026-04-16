package org.development.exam_online.common.constants;

/**
 * 考试记录状态常量
 *
 * 使用单个 status 字段同时表示【考生视角】和【教师视角】的叠加状态。
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │  状态值  │  考生视角      │  教师视角      │  说明            │
 * ├──────────┼───────────────┼───────────────┼─────────────────┤
 * │    0     │  未参加        │  —            │  记录已创建未开始 │
 * │    1     │  进行中        │  —            │  学生正在答题     │
 * │    2     │  已提交        │  未评分        │  提交但未判分     │
 * │    3     │  已提交        │  评分中        │  客观题已自动判分 │
 * │    4     │  已提交        │  已评分        │  教师完成全部评分 │
 * │    5     │  已结束        │  已评分        │  成绩已最终确认   │
 * └──────────────────────────────────────────────────────────────┘
 *
 * 状态流转：
 *   0(未参加) → 1(进行中) → 2(已提交/未评分) → 3(已提交/评分中) → 4(已提交/已评分) → 5(已结束/已评分)
 *
 * 说明：
 *   - 提交考试时自动判客观题，直接进入状态3
 *   - 如果试卷只有客观题，自动判分后可直接进入状态4
 *   - 教师手动阅卷完成后进入状态4
 *   - 教师确认成绩发布后进入状态5（最终状态）
 */
public class ExamRecordStatus {

    /** 0 - 未参加：记录已创建但学生未开始答题 */
    public static final int NOT_STARTED = 0;

    /** 1 - 进行中：学生正在答题 */
    public static final int IN_PROGRESS = 1;

    /** 2 - 已提交/未评分：学生已提交，尚未进行任何判分 */
    public static final int SUBMITTED_UNGRADED = 2;

    /** 3 - 已提交/评分中：客观题已自动判分，主观题待教师评分 */
    public static final int SUBMITTED_GRADING = 3;

    /** 4 - 已提交/已评分：所有题目评分完成 */
    public static final int SUBMITTED_GRADED = 4;

    /** 5 - 已结束/已评分：成绩最终确认，不可再修改 */
    public static final int FINISHED = 5;

    // ==================== 考生视角判断 ====================

    /** 考生是否已提交（status >= 2） */
    public static boolean isSubmitted(Integer status) {
        return status != null && status >= SUBMITTED_UNGRADED;
    }

    /** 考生是否正在答题 */
    public static boolean isInProgress(Integer status) {
        return status != null && status == IN_PROGRESS;
    }

    /** 考生视角：考试是否已结束（成绩已最终确认） */
    public static boolean isFinished(Integer status) {
        return status != null && status == FINISHED;
    }

    // ==================== 教师视角判断 ====================

    /** 教师视角：是否待评分（已提交但未进行任何评分） */
    public static boolean isUngraded(Integer status) {
        return status != null && status == SUBMITTED_UNGRADED;
    }

    /** 教师视角：是否评分中（客观题已判，主观题待评） */
    public static boolean isGrading(Integer status) {
        return status != null && status == SUBMITTED_GRADING;
    }

    /** 教师视角：是否已完成评分 */
    public static boolean isGraded(Integer status) {
        return status != null && status >= SUBMITTED_GRADED;
    }

    /** 教师视角：是否需要人工阅卷（状态2或3） */
    public static boolean needsManualGrading(Integer status) {
        return status != null && (status == SUBMITTED_UNGRADED || status == SUBMITTED_GRADING);
    }

    // ==================== 状态描述 ====================

    /** 获取完整状态描述 */
    public static String getDescription(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case NOT_STARTED:       return "未参加";
            case IN_PROGRESS:       return "进行中";
            case SUBMITTED_UNGRADED: return "已提交/未评分";
            case SUBMITTED_GRADING: return "已提交/评分中";
            case SUBMITTED_GRADED:  return "已提交/已评分";
            case FINISHED:          return "已结束/已评分";
            default:                return "未知";
        }
    }

    /** 获取考生视角状态描述 */
    public static String getStudentView(Integer status) {
        if (status == null) return "未知";
        if (status == NOT_STARTED) return "未参加";
        if (status == IN_PROGRESS) return "进行中";
        if (status >= SUBMITTED_UNGRADED && status <= SUBMITTED_GRADED) return "已提交";
        if (status == FINISHED) return "已结束";
        return "未知";
    }

    /** 获取教师视角状态描述 */
    public static String getTeacherView(Integer status) {
        if (status == null) return "未知";
        if (status <= IN_PROGRESS) return "—";
        if (status == SUBMITTED_UNGRADED) return "未评分";
        if (status == SUBMITTED_GRADING) return "评分中";
        if (status >= SUBMITTED_GRADED) return "已评分";
        return "未知";
    }

    /** Knife4j文档用的状态说明 */
    public static final String STATUS_DOC =
        "## exam_record.status 状态说明\n\n" +
        "| 值 | 考生视角 | 教师视角 | 说明 |\n" +
        "|---|---------|---------|------|\n" +
        "| 0 | 未参加   | —       | 记录已创建但未开始答题 |\n" +
        "| 1 | 进行中   | —       | 学生正在答题 |\n" +
        "| 2 | 已提交   | 未评分   | 提交但未进行任何判分 |\n" +
        "| 3 | 已提交   | 评分中   | 客观题已自动判分，主观题待教师评分 |\n" +
        "| 4 | 已提交   | 已评分   | 所有题目评分完成 |\n" +
        "| 5 | 已结束   | 已评分   | 成绩最终确认，不可再修改 |\n\n" +
        "**状态流转：** 0 → 1 → 2 → 3 → 4 → 5\n\n" +
        "**说明：**\n" +
        "- 提交考试时自动判客观题，通常直接进入状态3\n" +
        "- 如果试卷只有客观题，自动判分后直接进入状态4\n" +
        "- 教师手动阅卷完成后进入状态4\n" +
        "- 教师确认成绩发布后进入状态5";
}
