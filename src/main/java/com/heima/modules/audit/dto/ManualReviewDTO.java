package com.heima.modules.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "人工审核请求")
public class ManualReviewDTO {
    @NotBlank
    @Pattern(regexp = "APPROVE|REJECT", message = "审核动作只能为APPROVE或REJECT")
    @Schema(description = "审核动作", allowableValues = {"APPROVE", "REJECT"})
    private String action;

    @NotBlank
    @Size(max = 500)
    @Schema(description = "人工复核意见")
    private String opinion;
}
