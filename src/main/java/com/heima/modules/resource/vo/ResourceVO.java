package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "资源信息，不包含网盘地址、提取码或密文")
public class ResourceVO {
    @Schema(description = "资源ID") private Long id;
    @Schema(description = "作者ID") private Long authorId;
    @Schema(description = "分类ID") private Long categoryId;
    @Schema(description = "标题") private String title;
    @Schema(description = "摘要") private String summary;
    @Schema(description = "描述") private String description;
    @Schema(description = "封面文件ID") private Long coverFileId;
    @Schema(description = "附件文件ID") private List<Long> attachmentFileIds;
    @Schema(description = "标签ID") private List<Long> tagIds;
    @Schema(description = "标签名称") private List<String> tags;
    @Schema(description = "访问类型") private String accessType;
    @Schema(description = "积分价格") private Integer pointsPrice;
    @Schema(description = "现金价格") private BigDecimal price;
    @Schema(description = "版权声明") private String copyrightNotice;
    @Schema(description = "工作流状态，公开接口不返回") private String status;
    @Schema(description = "版本状态，公开接口不返回") private String versionStatus;
    @Schema(description = "审核原因，仅作者和后台可见") private String auditReason;
    @Schema(description = "最新版本号") private Integer latestVersionNo;
    @Schema(description = "公开版本号") private Integer publishedVersionNo;
    @Schema(description = "当前内容版本号") private Integer versionNo;
    @Schema(description = "浏览量") private Long viewCount;
    @Schema(description = "是否已授权访问链接和附件") private Boolean accessGranted;
    @Schema(description = "创建时间") private LocalDateTime createTime;
    @Schema(description = "更新时间") private LocalDateTime updateTime;
}
