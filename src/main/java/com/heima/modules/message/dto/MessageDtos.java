package com.heima.modules.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public final class MessageDtos {
    private MessageDtos() {}

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MessageVO {
        @Schema(description = "消息ID") private Long id;
        @Schema(description = "消息类型") private String type;
        @Schema(description = "标题") private String title;
        @Schema(description = "正文") private String content;
        @Schema(description = "业务类型") private String bizType;
        @Schema(description = "业务ID") private String bizId;
        @Schema(description = "是否已读") private Boolean read;
        @Schema(description = "已读时间") private LocalDateTime readTime;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }

    @Data
    public static class PreferenceRequest {
        @NotNull @Schema(description = "是否接收审核消息") private Boolean auditEnabled;
        @NotNull @Schema(description = "是否接收支付消息") private Boolean paymentEnabled;
        @NotNull @Schema(description = "是否接收系统消息") private Boolean systemEnabled;
        @NotNull @Schema(description = "是否接收互动消息") private Boolean interactionEnabled;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PreferenceVO {
        @Schema(description = "是否接收审核消息") private Boolean auditEnabled;
        @Schema(description = "是否接收支付消息") private Boolean paymentEnabled;
        @Schema(description = "是否接收系统消息") private Boolean systemEnabled;
        @Schema(description = "是否接收互动消息") private Boolean interactionEnabled;
    }

    @Data
    public static class SystemMessageRequest {
        @Schema(description = "指定用户ID，为空则广播全部用户") private Long userId;
        @NotBlank @Size(max = 120) @Schema(description = "消息标题") private String title;
        @NotBlank @Size(max = 2000) @Schema(description = "消息正文") private String content;
        @Size(max = 120) @Schema(description = "可选幂等键，同次广播共用") private String dedupKey;
    }
}
