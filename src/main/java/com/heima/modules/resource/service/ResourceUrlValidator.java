package com.heima.modules.resource.service;

import com.heima.exception.ServiceException;
import com.heima.modules.resource.entity.ResourceDiskType;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ResourceUrlValidator {
    public String validate(String value, ResourceDiskType diskType) {
        if (diskType == null || diskType.getStatus() == null || diskType.getStatus() != 1) {
            throw new ServiceException(400, "网盘类型不存在或已停用");
        }
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            String host = normalizeHost(uri.getHost());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null
                    || !hosts(diskType.getAllowedHosts()).stream().anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed))) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException e) {
            throw new ServiceException(400, "链接必须是所选网盘白名单域名下的有效HTTPS地址");
        }
    }

    public String normalizeAllowedHosts(String allowedHosts) {
        try {
            Set<String> hosts = Arrays.stream(allowedHosts == null ? new String[0] : allowedHosts.split(","))
                    .map(String::trim).filter(item -> !item.isEmpty()).map(this::normalizeHost)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            if (hosts.isEmpty() || hosts.contains(null)) throw new IllegalArgumentException();
            return String.join(",", hosts);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(400, "域名白名单格式错误，仅支持逗号分隔的有效域名");
        }
    }

    private Set<String> hosts(String allowedHosts) {
        return Arrays.stream(allowedHosts == null ? new String[0] : allowedHosts.split(","))
                .map(String::trim).filter(item -> !item.isEmpty()).map(this::normalizeHost)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    }

    private String normalizeHost(String host) {
        if (host == null || host.isBlank() || host.contains("/") || host.contains(":") || host.contains("@")) return null;
        try {
            String normalized = IDN.toASCII(host.endsWith(".") ? host.substring(0, host.length() - 1) : host)
                    .toLowerCase(Locale.ROOT);
            if (normalized.isBlank() || !normalized.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")) return null;
            return normalized;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
