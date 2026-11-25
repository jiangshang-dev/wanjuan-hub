package com.heima.utils;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

public final class IpUtils {

    private IpUtils() {
    }

    public static String getIpAddr(HttpServletRequest request) {
        return getIpAddr(request, false);
    }

    public static String getIpAddr(HttpServletRequest request, boolean trustForwardedHeaders) {
        if (request == null) {
            return "";
        }
        String ip = trustForwardedHeaders
                ? first(request.getHeader("X-Forwarded-For"), request.getHeader("X-Real-IP"), request.getRemoteAddr())
                : request.getRemoteAddr();
        if (StrUtil.isBlank(ip)) {
            return "";
        }
        if (ip.contains(",")) {
            ip = StrUtil.trim(ip.split(",")[0]);
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return Validator.isIpv4(ip) || Validator.isIpv6(ip) ? ip : StrUtil.sub(ip, 0, 64);
    }

    private static String first(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value) && !"unknown".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return "";
    }
}
