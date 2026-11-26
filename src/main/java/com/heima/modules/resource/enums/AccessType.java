package com.heima.modules.resource.enums;

public enum AccessType {
    FREE, POINTS, MEMBER,
    /** 以下旧值仅用于兼容历史数据和旧客户端，保存时会归一化为三类之一。 */
    GUEST_FREE, LOGIN_FREE, REPLY_REQUIRED, PAID;

    public AccessType normalize() {
        return switch (this) {
            case GUEST_FREE, LOGIN_FREE, REPLY_REQUIRED -> FREE;
            case PAID -> MEMBER;
            default -> this;
        };
    }
}
