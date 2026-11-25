package com.heima.modules.forum.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.forum.dto.ForumDtos.PostQuery;
import com.heima.modules.forum.dto.ForumDtos.Query;
import com.heima.modules.forum.service.ForumService;
import com.heima.modules.forum.vo.ForumVos.BoardVO;
import com.heima.modules.forum.vo.ForumVos.PostVO;
import com.heima.modules.forum.vo.ForumVos.TopicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@Tag(name = "公开论坛")
public class PublicForumController {
    private final ForumService forumService;

    @GetMapping("/boards")
    @Operation(summary = "查询启用板块，最多100条")
    public R<List<BoardVO>> boards() { return R.ok(forumService.publicBoards()); }

    @GetMapping("/topics")
    @Operation(summary = "分页查询公开主题的当前公开版本")
    public R<PageResult<TopicVO>> topics(@Valid @ModelAttribute Query query) { return R.ok(forumService.publicTopics(query)); }

    @GetMapping("/topics/{id}")
    @Operation(summary = "查看主题当前公开版本并增加浏览量")
    public R<TopicVO> topic(@PathVariable Long id) { return R.ok(forumService.publicTopic(id)); }

    @GetMapping("/topics/{topicId}/posts")
    @Operation(summary = "分页查询主题公开跟帖，支持只看楼主")
    public R<PageResult<PostVO>> posts(@PathVariable Long topicId, @Valid @ModelAttribute PostQuery query) {
        return R.ok(forumService.publicPosts(topicId, query));
    }
}
