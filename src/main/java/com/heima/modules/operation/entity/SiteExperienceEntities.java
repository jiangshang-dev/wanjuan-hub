package com.heima.modules.operation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

public final class SiteExperienceEntities {
    private SiteExperienceEntities() {}

    @Data @TableName("operation_gray_day") @EqualsAndHashCode(callSuper = true)
    public static class GrayDay extends BaseEntity {
        private LocalDate grayDate;
        private String reason;
        private Integer status;
    }

    @Data @TableName("operation_background_music") @EqualsAndHashCode(callSuper = true)
    public static class BackgroundMusic extends BaseEntity {
        private Integer enabled;
        private String audioUrl;
        private String playMode;
        private Integer playerVisible;
    }
}
