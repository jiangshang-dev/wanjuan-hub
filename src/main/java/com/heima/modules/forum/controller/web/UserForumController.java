package com.heima.modules.forum.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.forum.dto.ForumDtos.PostQuery;
import com.heima.modules.forum.dto.ForumDtos.PostRequest;
import com.heima.modules.forum.dto.ForumDtos.Query;
import com.heima.modules.forum.dto.ForumDtos.TopicRequest;
import com.heima.modules.forum.service.ForumService;
import com.heima.modules.forum.vo.ForumVos.PostVO;
import com.heima.modules.forum.vo.ForumVos.TopicVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/user/forum")
@RequiredArgsConstructor
@Tag(name = "用户论坛")
public class UserForumController {
    private final ForumService forumService;

    @GetMapping("/topics")
    @Operation(summary = "查询自己的主题及审核状态")
    public R<PageResult<TopicVO>> topics(@Valid @ModelAttribute Query query) {
        return R.ok(forumService.userTopics(query, UserUtil.requireFrontUser().getId()));
    }

    @PostMapping("/topics")
    @Operation(summary = "创建主题并立即提交审核")
    public R<Long> createTopic(@Valid @RequestBody TopicRequest request) {
        return R.ok(forumService.createTopic(request, UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/topics/{id}")
    @Operation(summary = "查看自己的主题最新版本")
    public R<TopicVO> topic(@PathVariable Long id) {
        return R.ok(forumService.userTopic(id, UserUtil.requireFrontUser().getId()));
    }

    @PutMapping("/topics/{id}")
    @Operation(summary = "编辑主题生成新审核版本，旧公开版本继续展示")
    public R<Void> updateTopic(@PathVariable Long id, @Valid @RequestBody TopicRequest request) {
        forumService.updateTopic(id, request, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @DeleteMapping("/topics/{id}")
    @Operation(summary = "删除自己的主题，保留审核版本")
    public R<Void> deleteTopic(@PathVariable Long id) {
        forumService.deleteTopic(id, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @GetMapping("/posts")
    @Operation(summary = "查询自己的跟帖及审核状态")
    public R<PageResult<PostVO>> posts(@Valid @ModelAttribute PostQuery query) {
        return R.ok(forumService.userPosts(query, UserUtil.requireFrontUser().getId()));
    }

    @PostMapping("/topics/{topicId}/posts")
    @Operation(summary = "创建跟帖或楼中楼并立即提交审核")
    public R<Long> createPost(@PathVariable Long topicId, @Valid @RequestBody PostRequest request) {
        return R.ok(forumService.createPost(topicId, request, UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "查看自己的跟帖最新版本")
    public R<PostVO> post(@PathVariable Long id) {
        return R.ok(forumService.userPost(id, UserUtil.requireFrontUser().getId()));
    }

    @PutMapping("/posts/{id}")
    @Operation(summary = "编辑跟帖生成新审核版本")
    public R<Void> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        forumService.updatePost(id, request, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @DeleteMapping("/posts/{id}")
    @Operation(summary = "删除自己的跟帖，保留审核版本")
    public R<Void> deletePost(@PathVariable Long id) {
        forumService.deletePost(id, UserUtil.requireFrontUser().getId());
        return R.ok();
    }
}
