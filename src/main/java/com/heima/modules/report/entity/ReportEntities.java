package com.heima.modules.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

public final class ReportEntities {
    private ReportEntities() {}

    @Data @TableName("content_report") @EqualsAndHashCode(callSuper = true)
    public static class Report extends BaseEntity {
        private Long reporterId;
        private String targetType;
        private Long targetId;
        private String reason;
        private String detail;
        private String status;
    }

    @Data @TableName("content_report_handle") @EqualsAndHashCode(callSuper = true)
    public static class Handle extends BaseEntity {
        private Long reportId;
        private Long handlerId;
        private String action;
        private String note;
    }
}
