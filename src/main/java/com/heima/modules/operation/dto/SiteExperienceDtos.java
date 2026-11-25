package com.heima.modules.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

public final class SiteExperienceDtos {
    private SiteExperienceDtos() {}

    @Data @Schema(description = "特殊灰色日保存请求")
    public static class GrayDayRequest {
        @NotNull @Schema(description = "灰色日期", example = "2026-12-13") private LocalDate grayDate;
        @NotBlank @Size(max = 500) @Schema(description = "灰色原因") private String reason;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
    }

    @Data @Schema(description = "背景音乐配置保存请求")
    public static class BackgroundMusicRequest {
        @NotNull @Schema(description = "是否开启背景音乐") private Boolean enabled;
        @Size(max = 1000) @Pattern(regexp = "^$|https?://\\S+", flags = Pattern.Flag.CASE_INSENSITIVE)
        @Schema(description = "音频公开访问地址，支持 RustFS HTTP/HTTPS 地址；开启时必填", example = "https://rustfs.example.com/public/bgm.mp3")
        private String audioUrl = "";
        @NotNull @Pattern(regexp = "AUTO|MANUAL") @Schema(description = "播放方式", allowableValues = {"AUTO", "MANUAL"}) private String playMode;
        @NotNull @Schema(description = "是否显示播放器") private Boolean playerVisible;
    }
}
