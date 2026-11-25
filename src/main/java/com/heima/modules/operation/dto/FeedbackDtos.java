package com.heima.modules.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class FeedbackDtos {
    private FeedbackDtos() {}

    @Data @Schema(description = "提交意见反馈请求")
    public static class CreateRequest {
        @NotBlank @Size(max = 2000) @Schema(description = "反馈内容") private String content;
    }

    @Data @Schema(description = "回复意见反馈请求")
    public static class ReplyRequest {
        @NotBlank @Size(max = 2000) @Schema(description = "管理员回复内容") private String reply;
    }
}
