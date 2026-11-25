package com.heima.modules.interaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

public final class InteractionDtos {
    private InteractionDtos() {}

    @Data @Schema(description = "互动目标")
    public static class TargetRequest {
        @NotBlank @Pattern(regexp = "BLOG|FORUM|FORUM_POST|COMMENT|RESOURCE") @Schema(description = "目标类型", allowableValues = {"BLOG", "FORUM", "FORUM_POST", "COMMENT", "RESOURCE"}) private String targetType;
        @NotNull @Schema(description = "目标ID") private Long targetId;
        @Pattern(regexp = "LIKE|DISLIKE") @Schema(description = "反应类型，默认LIKE", allowableValues = {"LIKE", "DISLIKE"}, defaultValue = "LIKE") private String reactionType = "LIKE";
    }

    @Data @Schema(description = "互动分页查询")
    public static class Query {
        @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
        @Min(1) @Max(100) @Schema(description = "每页数量，最大100", defaultValue = "20") private long size = 20;
        @Pattern(regexp = "BLOG|FORUM|FORUM_POST|COMMENT|RESOURCE") @Schema(description = "按目标类型筛选") private String targetType;
    }
}
