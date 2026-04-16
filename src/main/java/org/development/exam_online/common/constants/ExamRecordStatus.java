package org.development.exam_online.common.constants;

public class ExamRecordStatus {

    public static final int NOT_STARTED = 0;

    public static final int IN_PROGRESS = 1;

    public static final int SUBMITTED_UNGRADED = 2;

    public static final int SUBMITTED_GRADING = 3;

    public static final int SUBMITTED_GRADED = 4;

    public static final int FINISHED = 5;

    public static boolean isSubmitted(Integer status) {
        return status != null && status >= SUBMITTED_UNGRADED;
    }

    public static boolean isInProgress(Integer status) {
        return status != null && status == IN_PROGRESS;
    }

    public static boolean isFinished(Integer status) {
        return status != null && status == FINISHED;
    }

    public static boolean isUngraded(Integer status) {
        return status != null && status == SUBMITTED_UNGRADED;
    }

    public static boolean isGrading(Integer status) {
        return status != null && status == SUBMITTED_GRADING;
    }

    public static boolean isGraded(Integer status) {
        return status != null && status >= SUBMITTED_GRADED;
    }

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

    public static String getStudentView(Integer status) {
        if (status == null) return "未知";
        if (status == NOT_STARTED) return "未参加";
        if (status == IN_PROGRESS) return "进行中";
        if (status >= SUBMITTED_UNGRADED && status <= SUBMITTED_GRADED) return "已提交";
        if (status == FINISHED) return "已结束";
        return "未知";
    }

    public static String getTeacherView(Integer status) {
        if (status == null) return "未知";
        if (status <= IN_PROGRESS) return "—";
        if (status == SUBMITTED_UNGRADED) return "未评分";
        if (status == SUBMITTED_GRADING) return "评分中";
        if (status >= SUBMITTED_GRADED) return "已评分";
        return "未知";
    }
}
