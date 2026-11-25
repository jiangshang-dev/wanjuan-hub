package com.heima.modules.operation.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class SiteExperienceVOs {
    private SiteExperienceVOs() {}

    @Schema(description = "后台特殊灰色日")
    public record GrayDayVO(@Schema(description = "灰色日ID，字符串") String id,
            @Schema(description = "灰色日期") LocalDate grayDate,
            @Schema(description = "灰色原因") String reason,
            @Schema(description = "状态：0停用，1启用") Integer status,
            @Schema(description = "创建时间") LocalDateTime createTime,
            @Schema(description = "更新时间") LocalDateTime updateTime) {}

    @Schema(description = "当天灰色状态")
    public record GrayStatusVO(@Schema(description = "当天是否为灰色日") boolean gray,
            @Schema(description = "灰色原因，非灰日为null") String reason) {}

    @Schema(description = "后台背景音乐配置")
    public record BackgroundMusicVO(@Schema(description = "是否开启") boolean enabled,
            @Schema(description = "音频地址") String audioUrl,
            @Schema(description = "播放方式：AUTO或MANUAL") String playMode,
            @Schema(description = "是否显示播放器") boolean playerVisible,
            @Schema(description = "更新时间") LocalDateTime updateTime) {}

    @Schema(description = "公开背景音乐配置，仅包含播放器必要参数")
    public record PublicBackgroundMusicVO(@Schema(description = "是否开启") boolean enabled,
            @Schema(description = "音频公开访问地址") String audioUrl,
            @Schema(description = "播放方式：AUTO或MANUAL") String playMode,
            @Schema(description = "是否显示播放器") boolean playerVisible) {}
}
