package com.heima.modules.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

public final class CommentDtos {
    private CommentDtos() {}

    @Data @Schema(description = "评论分页查询")
    public static class Query {
        @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
        @Min(1) @Max(100) @Schema(description = "每页数量，最大100", defaultValue = "20") private long size = 20;
        @Schema(description = "目标类型") private String targetType;
        @Schema(description = "目标ID") private Long targetId;
        @Schema(description = "状态，仅后台使用") private String status;
    }

    @Data @Schema(description = "发表评论请求")
    public static class CreateRequest {
        @NotBlank @Pattern(regexp = "BLOG|FORUM|RESOURCE") @Schema(description = "目标类型", allowableValues = {"BLOG", "FORUM", "RESOURCE"}) private String targetType;
        @NotNull @Schema(description = "目标ID") private Long targetId;
        @Schema(description = "父评论ID；一级评论为空") private Long parentId;
        @Schema(description = "被回复评论ID") private Long replyToId;
        @NotBlank @Size(max = 10000) @Schema(description = "评论内容") private String content;
        @Size(max = 9) @Schema(description = "图片文件ID") private List<@NotNull Long> imageFileIds;
    }

    @Data @Schema(description = "后台评论状态请求")
    public static class StatusRequest {
        @NotBlank @Pattern(regexp = "PUBLISHED|OFFLINE") @Schema(description = "恢复公开或下线", allowableValues = {"PUBLISHED", "OFFLINE"}) private String status;
        @Size(max = 500) @Schema(description = "操作原因") private String reason;
    }
}
