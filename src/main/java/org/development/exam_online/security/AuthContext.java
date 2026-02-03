package org.development.exam_online.security;

/**
 * 当前请求的认证上下文（基于 ThreadLocal）
 */
public final class AuthContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> ROLE_ID = new ThreadLocal<>();
    private static final ThreadLocal<java.util.Set<String>> PERMISSION_CODES = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(Long userId, Long roleId) {
        USER_ID.set(userId);
        ROLE_ID.set(roleId);
    }

    public static void setPermissionCodes(java.util.Set<String> permissionCodes) {
        PERMISSION_CODES.set(permissionCodes);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Long getRoleId() {
        return ROLE_ID.get();
    }

    public static java.util.Set<String> getPermissionCodes() {
        return PERMISSION_CODES.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLE_ID.remove();
        PERMISSION_CODES.remove();
    }
}

