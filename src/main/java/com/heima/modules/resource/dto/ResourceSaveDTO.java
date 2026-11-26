package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "资源草稿保存请求")
public class ResourceSaveDTO {
    @NotBlank @Size(max = 120) @Schema(description = "标题") private String title;
    @NotBlank @Size(max = 500) @Schema(description = "摘要") private String summary;
    @NotBlank @Schema(description = "资源描述，禁止危险HTML") private String description;
    @Schema(description = "封面图片文件ID") private Long coverFileId;
    @Size(max = 20) @Schema(description = "附件图片文件ID，最多20个") private List<@NotNull Long> attachmentFileIds;
    @NotNull @Schema(description = "分类ID") private Long categoryId;
    @Size(max = 10) @Schema(description = "标签ID，最多10个") private List<@NotNull Long> tagIds;
    @NotBlank @Pattern(regexp = "FREE|POINTS|MEMBER|GUEST_FREE|LOGIN_FREE|REPLY_REQUIRED|PAID") @Schema(description = "访问类型，统一为免费/积分/会员；旧枚举仅兼容旧客户端", allowableValues = {"FREE", "POINTS", "MEMBER", "GUEST_FREE", "LOGIN_FREE", "REPLY_REQUIRED", "PAID"}) private String accessType;
    @Min(1) @Schema(description = "积分价格，POINTS类型必填") private Integer pointsPrice;
    @DecimalMin(value = "0.01") @Schema(description = "历史现金价格字段；新资源不再单独现金售卖") private BigDecimal price;
    @NotBlank @Size(max = 1000) @Schema(description = "版权声明") private String copyrightNotice;
    @Size(max = 20) @Schema(description = "多网盘链接，最多20个") private List<@Valid ResourceLinkDTO> links;
}
