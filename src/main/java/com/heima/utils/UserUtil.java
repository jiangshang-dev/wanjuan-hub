package com.heima.utils;

import com.heima.exception.ServiceException;
import com.heima.security.context.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class UserUtil {

    private UserUtil() {
    }

    public static LoginUser getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof LoginUser loginUser ? loginUser : null;
    }

    public static LoginUser requireUser() {
        LoginUser user = getUser();
        if (user == null || user.getId() == null) {
            throw new ServiceException(401, "请先登录");
        }
        return user;
    }

    public static Long getUserId() {
        LoginUser user = getUser();
        return user == null ? null : user.getId();
    }

    public static Long requireUserId() {
        return requireUser().getId();
    }

    public static LoginUser requireFrontUser() {
        LoginUser user = requireUser();
        if (!"USER".equals(user.getRole())) {
            throw new ServiceException(403, "请使用前台用户账号登录");
        }
        return user;
    }

    public static LoginUser requireAdmin() {
        LoginUser user = requireUser();
        if (!user.isAdmin()) {
            throw new ServiceException(403, "请使用管理员账号登录");
        }
        return user;
    }

    public static LoginUser frontUserOrNull() {
        LoginUser user = getUser();
        return user != null && "USER".equals(user.getRole()) ? user : null;
    }
}
