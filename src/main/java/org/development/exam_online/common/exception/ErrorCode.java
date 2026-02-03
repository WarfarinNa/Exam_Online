package org.development.exam_online.common.exception;

/**
 * 错误码枚举
 * 定义系统中常用的错误码和错误消息
 */
public enum ErrorCode {

    // 通用错误码 400-499
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    // 业务错误码 1000-1999
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    EMAIL_EXISTS(1003, "邮箱已被注册"),
    PASSWORD_ERROR(1004, "用户名或密码错误"),
    OLD_PASSWORD_ERROR(1005, "原密码错误"),

    ROLE_NOT_FOUND(1101, "角色不存在"),
    PERMISSION_NOT_FOUND(1102, "权限不存在"),

    QUESTION_NOT_FOUND(1201, "题目不存在"),
    QUESTION_TYPE_INVALID(1202, "题型格式错误"),
    QUESTION_CATEGORY_NOT_FOUND(1203, "题目分类不存在"),
    QUESTION_CATEGORY_HAS_QUESTIONS(1204, "该分类下存在题目，无法删除"),

    EXAM_PAPER_NOT_FOUND(1301, "试卷不存在"),
    EXAM_PAPER_HAS_EXAMS(1302, "该试卷已关联考试，无法删除"),
    EXAM_PAPER_NAME_EMPTY(1303, "试卷名称不能为空"),
    EXAM_PAPER_CREATE_FAILED(1304, "创建试卷失败"),
    EXAM_PAPER_UPDATE_FAILED(1305, "试卷信息更新失败"),
    EXAM_PAPER_DELETE_FAILED(1306, "试卷删除失败"),
    EXAM_PAPER_QUESTION_IDS_EMPTY(1307, "题目ID列表不能为空"),
    EXAM_PAPER_QUESTION_NOT_FOUND(1308, "试卷中部分题目不存在"),

    EXAM_NOT_FOUND(1309, "考试不存在"),
    EXAM_HAS_RECORDS(1310, "该考试已有考试记录，无法删除"),
    EXAM_NOT_STARTED(1311, "考试尚未开始"),
    EXAM_ENDED(1312, "考试已结束"),
    EXAM_ALREADY_STARTED(1313, "您已经开始了本次考试"),
    EXAM_NOT_STARTED_BY_USER(1314, "您尚未开始本次考试"),
    EXAM_ALREADY_SUBMITTED(1315, "考试已提交，无法再次提交"),
    EXAM_TIME_EXPIRED(1316, "考试时间已到"),
    EXAM_PERMISSION_DENIED(1317, "您无权限参加本次考试"),
    EXAM_RECORD_NOT_FOUND(1318, "考试记录不存在"),
    EXAM_ANSWER_NOT_FOUND(1319, "答案记录不存在"),
    EXAM_DURATION_INVALID(1320, "考试时长无效"),

    AUTO_GENERATE_RULE_EMPTY(1401, "自动组卷规则不能为空"),
    AUTO_GENERATE_TYPE_RULES_EMPTY(1402, "题型规则不能为空"),
    AUTO_GENERATE_TYPE_INVALID(1403, "无效的题型"),
    AUTO_GENERATE_QUESTION_NOT_ENOUGH(1404, "题型 {0} 的题目数量不足，需要 {1} 道，但只有 {2} 道"),
    AUTO_GENERATE_DIFFICULTY_NOT_ENOUGH(1405, "难度 {0} 的题目数量不足，需要 {1} 道，但只有 {2} 道"),
    AUTO_GENERATE_DIFFICULTY_RULE_INVALID(1406, "难度规则无效"),
    AUTO_GENERATE_DIFFICULTY_RATIO_INVALID(1407, "难度比例总和必须接近1.0"),

    // 判卷与成绩分析错误码 1500-1599
    EXAM_RECORD_NOT_SUBMITTED(1501, "考试记录尚未提交，无法判卷"),
    EXAM_RECORD_ALREADY_GRADED(1502, "该考试记录已经判卷完成"),
    SUBJECTIVE_QUESTION_SCORE_INVALID(1503, "主观题分值无效"),
    SUBJECTIVE_QUESTION_SCORE_EXCEED(1504, "主观题分值不能超过题目满分"),
    GRADING_PERMISSION_DENIED(1505, "无权限进行判卷操作"),

    // 系统错误码 5000-5999
    INTERNAL_ERROR(5000, "系统内部错误"),
    DATABASE_ERROR(5001, "数据库操作失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
