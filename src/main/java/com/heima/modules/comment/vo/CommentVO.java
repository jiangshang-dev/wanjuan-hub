package com.heima.modules.comment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data @Schema(description = "统一评论")
public class CommentVO {
    @Schema(description = "评论ID") private Long id;
    @Schema(description = "目标类型") private String targetType;
    @Schema(description = "目标ID") private Long targetId;
    @Schema(description = "作者ID") private Long authorId;
    @Schema(description = "根评论ID") private Long rootId;
    @Schema(description = "父评论ID") private Long parentId;
    @Schema(description = "被回复评论ID") private Long replyToId;
    @Schema(description = "评论内容") private String content;
    @Schema(description = "图片文件ID") private List<Long> imageFileIds;
    @Schema(description = "工作流状态，公开接口不返回") private String status;
    @Schema(description = "审核原因，仅作者和后台返回") private String auditReason;
    @Schema(description = "最新版本号") private Integer latestVersionNo;
    @Schema(description = "公开版本号") private Integer publishedVersionNo;
    @Schema(description = "当前内容版本号") private Integer versionNo;
    @Schema(description = "回复数") private Long replyCount;
    @Schema(description = "点赞数") private Long likeCount;
    @Schema(description = "点踩数") private Long dislikeCount;
    @Schema(description = "创建时间") private LocalDateTime createTime;
    @Schema(description = "更新时间") private LocalDateTime updateTime;
}
