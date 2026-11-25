package com.heima.modules.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "后台博客状态变更")
public class AdminBlogStatusDTO {
    @NotBlank @Pattern(regexp = "PUBLISHED|OFFLINE")
    @Schema(description = "恢复公开或下线", allowableValues = {"PUBLISHED", "OFFLINE"}) private String status;
    @NotBlank @Size(max = 500) @Schema(description = "状态变更原因") private String reason;
}
