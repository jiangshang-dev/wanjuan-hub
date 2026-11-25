package com.heima.modules.forum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

public final class ForumDtos {
    private ForumDtos() {}

    @Data @Schema(description = "论坛分页查询")
    public static class Query {
        @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
        @Min(1) @Max(100) @Schema(description = "每页数量，最大100", defaultValue = "20") private long size = 20;
        @Schema(description = "板块ID") private Long boardId;
        @Size(max = 120) @Schema(description = "标题关键词") private String keyword;
        @Schema(description = "工作流状态，仅个人和后台使用") private String status;
        @Pattern(regexp = "LATEST|HOT|ESSENCE") @Schema(description = "排序：最新、热门、精华", allowableValues = {"LATEST", "HOT", "ESSENCE"}) private String sort = "LATEST";
    }

    @Data @EqualsAndHashCode(callSuper = true) @Schema(description = "跟帖分页查询")
    public static class PostQuery extends Query {
        @Schema(description = "主题ID，用户接口由路径指定") private Long topicId;
        @Schema(description = "只看楼主") private Boolean authorOnly = false;
        @Pattern(regexp = "ASC|DESC") @Schema(description = "楼层排序：正序或倒序", allowableValues = {"ASC", "DESC"}) private String order = "ASC";
    }

    @Data @Schema(description = "板块保存请求")
    public static class BoardRequest {
        @NotNull @Schema(description = "分区ID") private Long sectionId;
        @Schema(description = "父板块ID，为空表示一级板块") private Long parentId;
        @NotBlank @Size(max = 50) @Schema(description = "板块名称") private String name;
        @Size(max = 500) @Schema(description = "板块简介") private String description;
        @Schema(description = "排序值") private Integer sortOrder = 0;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status = 1;
        @NotBlank @Pattern(regexp = "USER|MODERATOR|ADMIN") @Schema(description = "发帖权限") private String postPermission = "USER";
        @NotBlank @Pattern(regexp = "USER|MODERATOR|ADMIN") @Schema(description = "回复权限") private String replyPermission = "USER";
    }

    @Data @Schema(description = "版主关联请求")
    public static class ModeratorRequest {
        @NotNull @Schema(description = "板块ID") private Long boardId;
        @NotNull @Schema(description = "管理员ID") private Long adminId;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status = 1;
    }

    @Data @Schema(description = "主题保存请求，提交后立即进入审核")
    public static class TopicRequest {
        @NotNull @Schema(description = "板块ID") private Long boardId;
        @Schema(description = "作者ID，仅后台创建时使用") private Long authorId;
        @NotBlank @Size(max = 120) @Schema(description = "标题") private String title;
        @NotBlank @Size(max = 100000) @Schema(description = "正文") private String content;
        @Size(max = 20) @Schema(description = "正文图片文件ID") private List<@NotNull Long> imageFileIds;
    }

    @Data @Schema(description = "跟帖保存请求，提交后立即进入审核")
    public static class PostRequest {
        @Schema(description = "主题ID，用户接口由路径指定，后台创建必填") private Long topicId;
        @Schema(description = "作者ID，仅后台创建时使用") private Long authorId;
        @NotBlank @Size(max = 50000) @Schema(description = "跟帖内容") private String content;
        @Schema(description = "父跟帖ID，形成楼中楼") private Long parentId;
        @Schema(description = "引用的跟帖ID") private Long replyToId;
        @Size(max = 20) @Schema(description = "图片文件ID") private List<@NotNull Long> imageFileIds;
    }

    @Data @Schema(description = "后台内容状态请求")
    public static class StatusRequest {
        @NotBlank @Pattern(regexp = "PUBLISHED|OFFLINE") @Schema(description = "恢复公开或下线", allowableValues = {"PUBLISHED", "OFFLINE"}) private String status;
        @NotBlank @Size(max = 500) @Schema(description = "操作原因") private String reason;
    }

    @Data @Schema(description = "主题管理操作")
    public static class ModerationRequest {
        @Min(0) @Max(1) @Schema(description = "是否锁帖") private Integer locked;
        @Min(0) @Max(1) @Schema(description = "是否置顶") private Integer topped;
        @Min(0) @Max(1) @Schema(description = "是否精华") private Integer essence;
        @Schema(description = "移动到目标板块") private Long boardId;
    }
}
