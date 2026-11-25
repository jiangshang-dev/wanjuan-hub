package com.heima.modules.comment.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.comment.dto.CommentDtos.Query;
import com.heima.modules.comment.service.CommentService;
import com.heima.modules.comment.vo.CommentVO;
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

@Validated
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "公开统一评论")
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "分页查询目标的一级公开评论")
    public R<PageResult<CommentVO>> roots(@Valid @ModelAttribute Query query) { return R.ok(commentService.publicRoots(query)); }

    @GetMapping("/{rootId}/replies")
    @Operation(summary = "分页查询根评论下的公开回复")
    public R<PageResult<CommentVO>> replies(@PathVariable Long rootId, @Valid @ModelAttribute Query query) {
        return R.ok(commentService.publicReplies(rootId, query));
    }
}
