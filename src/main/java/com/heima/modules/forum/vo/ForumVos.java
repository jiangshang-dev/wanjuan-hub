package com.heima.modules.forum.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

public final class ForumVos {
    private ForumVos() {}

    @Data @Schema(description = "论坛板块")
    public static class BoardVO {
        @Schema(description = "板块ID") private Long id;
        @Schema(description = "分区ID") private Long sectionId;
        @Schema(description = "父板块ID") private Long parentId;
        @Schema(description = "名称") private String name;
        @Schema(description = "简介") private String description;
        @Schema(description = "排序值") private Integer sortOrder;
        @Schema(description = "状态") private Integer status;
        @Schema(description = "发帖权限") private String postPermission;
        @Schema(description = "回复权限") private String replyPermission;
        @Schema(description = "公开主题数") private Long topicCount;
        @Schema(description = "创建时间") private LocalDateTime createTime;
        @Schema(description = "更新时间") private LocalDateTime updateTime;
    }

    @Data @Schema(description = "版主关联")
    public static class ModeratorVO {
        @Schema(description = "关联ID") private Long id;
        @Schema(description = "板块ID") private Long boardId;
        @Schema(description = "管理员ID") private Long adminId;
        @Schema(description = "状态") private Integer status;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }

    @Data @Schema(description = "论坛主题")
    public static class TopicVO {
        @Schema(description = "主题ID") private Long id;
        @Schema(description = "板块ID") private Long boardId;
        @Schema(description = "作者ID") private Long authorId;
        @Schema(description = "标题") private String title;
        @Schema(description = "正文") private String content;
        @Schema(description = "图片文件ID") private List<Long> imageFileIds;
        @Schema(description = "工作流状态，公开接口不返回") private String status;
        @Schema(description = "审核原因，仅作者和后台返回") private String auditReason;
        @Schema(description = "最新版本号") private Integer latestVersionNo;
        @Schema(description = "公开版本号") private Integer publishedVersionNo;
        @Schema(description = "当前内容版本号") private Integer versionNo;
        @Schema(description = "是否锁帖") private Integer locked;
        @Schema(description = "是否置顶") private Integer topped;
        @Schema(description = "是否精华") private Integer essence;
        @Schema(description = "公开跟帖数") private Long postCount;
        @Schema(description = "公开评论数") private Long commentCount;
        @Schema(description = "点赞数") private Long likeCount;
        @Schema(description = "点踩数") private Long dislikeCount;
        @Schema(description = "收藏数") private Long favoriteCount;
        @Schema(description = "浏览量") private Long viewCount;
        @Schema(description = "创建时间") private LocalDateTime createTime;
        @Schema(description = "更新时间") private LocalDateTime updateTime;
    }

    @Data @Schema(description = "论坛跟帖")
    public static class PostVO {
        @Schema(description = "跟帖ID") private Long id;
        @Schema(description = "主题ID") private Long topicId;
        @Schema(description = "板块ID") private Long boardId;
        @Schema(description = "作者ID") private Long authorId;
        @Schema(description = "楼中楼根ID") private Long rootId;
        @Schema(description = "父跟帖ID") private Long parentId;
        @Schema(description = "引用跟帖ID") private Long replyToId;
        @Schema(description = "楼层号") private Integer floorNo;
        @Schema(description = "正文") private String content;
        @Schema(description = "图片文件ID") private List<Long> imageFileIds;
        @Schema(description = "工作流状态，公开接口不返回") private String status;
        @Schema(description = "审核原因，仅作者和后台返回") private String auditReason;
        @Schema(description = "最新版本号") private Integer latestVersionNo;
        @Schema(description = "公开版本号") private Integer publishedVersionNo;
        @Schema(description = "当前内容版本号") private Integer versionNo;
        @Schema(description = "点赞数") private Long likeCount;
        @Schema(description = "点踩数") private Long dislikeCount;
        @Schema(description = "创建时间") private LocalDateTime createTime;
        @Schema(description = "更新时间") private LocalDateTime updateTime;
    }
}
