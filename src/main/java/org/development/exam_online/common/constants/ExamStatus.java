package org.development.exam_online.common.constants;

/**
 * 考试状态常量
 * 用于统一管理考试的状态值
 */
public class ExamStatus {
    
    /**
     * 草稿状态（未发布）
     */
    public static final int DRAFT = 0;
    
    /**
     * 已发布（学生可见）
     */
    public static final int PUBLISHED = 1;
    
    /**
     * 已结束/已取消
     */
    public static final int ENDED = 2;
    
    /**
     * 获取状态描述
     * @param status 状态值
     * @return 状态描述
     */
    public static String getDescription(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case DRAFT:
                return "草稿（未发布）";
            case PUBLISHED:
                return "已发布";
            case ENDED:
                return "已结束";
            default:
                return "未知";
        }
    }
}
