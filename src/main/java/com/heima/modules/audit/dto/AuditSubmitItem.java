package com.heima.modules.audit.dto;

import com.heima.modules.audit.enums.AuditItemType;

public record AuditSubmitItem(AuditItemType type, String content) {
    public static AuditSubmitItem text(String content) {
        return new AuditSubmitItem(AuditItemType.TEXT, content);
    }

    public static AuditSubmitItem image(Long fileId) {
        return new AuditSubmitItem(AuditItemType.IMAGE, "file:" + fileId);
    }
}
