package com.heima.modules.forum.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.forum.dto.ForumDtos.ModerationRequest;
import com.heima.modules.forum.dto.ForumDtos.PostQuery;
import com.heima.modules.forum.dto.ForumDtos.PostRequest;
import com.heima.modules.forum.dto.ForumDtos.Query;
import com.heima.modules.forum.dto.ForumDtos.StatusRequest;
import com.heima.modules.forum.dto.ForumDtos.TopicRequest;
import com.heima.modules.forum.service.ForumService;
import com.heima.modules.forum.vo.ForumVos.PostVO;
import com.heima.modules.forum.vo.ForumVos.TopicVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/forum")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "后台论坛内容管理")
public class AdminForumContentController {
    private final ForumService forumService;

    @GetMapping("/topics")
    @Operation(summary = "分页查询权限范围内主题最新版本")
    @PreAuthorize("hasAuthority('forum:topic:list')")
    public R<PageResult<TopicVO>> topics(@Valid @ModelAttribute Query query) {
        return R.ok(forumService.adminTopics(query, UserUtil.requireAdmin().getId()));
    }

    @PostMapping("/topics")
    @Operation(summary = "代用户创建主题并立即提交审核")
    @PreAuthorize("hasAuthority('forum:topic:create')")
    public R<Long> createTopic(@Valid @RequestBody TopicRequest request) {
        return R.ok(forumService.createTopicForAdmin(request, UserUtil.requireAdmin().getId()));
    }

    @GetMapping("/topics/{id}")
    @PreAuthorize("hasAuthority('forum:topic:detail')")
    @Operation(summary = "查看权限范围内主题最新版本")
    public R<TopicVO> topic(@PathVariable Long id) {
        return R.ok(forumService.adminTopic(id, UserUtil.requireAdmin().getId()));
    }

    @PutMapping("/topics/{id}")
    @PreAuthorize("hasAuthority('forum:topic:update')")
    @Operation(summary = "编辑主题并生成新审核版本")
    public R<Void> updateTopic(@PathVariable Long id, @Valid @RequestBody TopicRequest request) {
        forumService.updateTopicForAdmin(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @DeleteMapping("/topics/{id}")
    @PreAuthorize("hasAuthority('forum:topic:delete')")
    @Operation(summary = "删除权限范围内主题")
    public R<Void> deleteTopic(@PathVariable Long id) {
        forumService.deleteTopicForAdmin(id, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @PutMapping("/topics/{id}/status")
    @PreAuthorize("hasAuthority('forum:topic:status')")
    @Operation(summary = "恢复已审核公开版本或下线主题")
    public R<Void> topicStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        forumService.topicStatus(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @PutMapping("/topics/{id}/moderation")
    @PreAuthorize("hasAuthority('forum:topic:moderate')")
    @Operation(summary = "锁帖、置顶、加精或移动主题")
    public R<Void> moderateTopic(@PathVariable Long id, @Valid @RequestBody ModerationRequest request) {
        forumService.moderateTopic(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @GetMapping("/posts")
    @PreAuthorize("hasAuthority('forum:post:list')")
    @Operation(summary = "分页查询权限范围内跟帖最新版本")
    public R<PageResult<PostVO>> posts(@Valid @ModelAttribute PostQuery query) {
        return R.ok(forumService.adminPosts(query, UserUtil.requireAdmin().getId()));
    }

    @PostMapping("/posts")
    @PreAuthorize("hasAuthority('forum:post:create')")
    @Operation(summary = "代用户创建跟帖并立即提交审核")
    public R<Long> createPostForAdmin(@Valid @RequestBody PostRequest request) {
        return R.ok(forumService.createPostForAdmin(request, UserUtil.requireAdmin().getId()));
    }

    @GetMapping("/posts/{id}")
    @PreAuthorize("hasAuthority('forum:post:detail')")
    @Operation(summary = "查看权限范围内跟帖最新版本")
    public R<PostVO> post(@PathVariable Long id) {
        return R.ok(forumService.adminPost(id, UserUtil.requireAdmin().getId()));
    }

    @PutMapping("/posts/{id}")
    @Operation(summary = "编辑跟帖并生成新审核版本")
    @PreAuthorize("hasAuthority('forum:post:update')")
    public R<Void> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        forumService.updatePostForAdmin(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasAuthority('forum:post:delete')")
    @Operation(summary = "删除权限范围内跟帖")
    public R<Void> deletePost(@PathVariable Long id) {
        forumService.deletePostForAdmin(id, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @PutMapping("/posts/{id}/status")
    @PreAuthorize("hasAuthority('forum:post:status')")
    @Operation(summary = "恢复已审核公开版本或下线跟帖")
    public R<Void> postStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        forumService.postStatus(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }
}
