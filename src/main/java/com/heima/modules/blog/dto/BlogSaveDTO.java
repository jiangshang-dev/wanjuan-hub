package com.heima.modules.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "博客草稿保存请求")
public class BlogSaveDTO {
    @NotBlank @Size(max = 120) @Schema(description = "标题") private String title;
    @Size(max = 500) @Schema(description = "摘要，为空时截取正文") private String summary;
    @Schema(description = "封面图片文件ID") private Long coverFileId;
    @NotBlank @Schema(description = "正文，禁止危险HTML") private String content;
    @NotNull @Schema(description = "分类ID") private Long categoryId;
    @Size(max = 10) @Schema(description = "标签ID，最多10个") private List<@NotNull Long> tagIds;
    @NotBlank @Pattern(regexp = "ORIGINAL|REPRINT") @Schema(description = "原创或转载", allowableValues = {"ORIGINAL", "REPRINT"}) private String type;
    @Size(max = 100) @Schema(description = "转载来源名称，转载必填") private String sourceName;
    @Size(max = 500) @Schema(description = "转载来源HTTP地址，转载必填") private String sourceUrl;
}
