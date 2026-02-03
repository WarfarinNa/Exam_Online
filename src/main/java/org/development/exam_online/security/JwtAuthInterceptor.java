package org.development.exam_online.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.util.JwtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

/**
 * JWT 鉴权拦截器：解析 token，写入 AuthContext，并支持 @RequireRole 校验。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final PermissionResolver permissionResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 非 controller 方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        String token = extractToken(authorization);
        if (token == null || !jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未授权，请先登录");
        }

        Long userId = jwtUtils.getUserIdFromToken(token);
        Long roleId = jwtUtils.getRoleIdFromToken(token);
        if (userId == null || roleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token无效或已过期");
        }

        AuthContext.set(userId, roleId);

        // 解析权限码并写入上下文
        Set<String> permissionCodes = permissionResolver.resolvePermissionCodesByRoleId(roleId);
        AuthContext.setPermissionCodes(permissionCodes);

        // 角色校验：方法注解优先，其次类注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null) {
            boolean ok = Arrays.stream(requireRole.value()).anyMatch(r -> r == roleId);
            if (!ok) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
            }
        }

        // 权限码校验：方法注解优先，其次类注解（满足任意一个即可）
        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            requirePermission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (requirePermission != null) {
            Set<String> perms = AuthContext.getPermissionCodes();
            boolean ok = perms != null && Arrays.stream(requirePermission.value()).anyMatch(perms::contains);
            if (!ok) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private static String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) return null;
        return authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
    }
}

