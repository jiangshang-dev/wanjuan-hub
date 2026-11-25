package com.heima.modules.comment.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.comment.dto.CommentDtos.CreateRequest;
import com.heima.modules.comment.dto.CommentDtos.Query;
import com.heima.modules.comment.service.CommentService;
import com.heima.modules.comment.vo.CommentVO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/comments")
@RequiredArgsConstructor
@Tag(name = "用户统一评论")
public class UserCommentController {
    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "查询自己的评论及审核状态")
    public R<PageResult<CommentVO>> page(@Valid @ModelAttribute Query query) {
        return R.ok(commentService.userPage(query, UserUtil.requireFrontUser().getId()));
    }

    @PostMapping
    @Operation(summary = "发表评论或回复并立即提交审核")
    public R<Long> create(@Valid @RequestBody CreateRequest request) {
        return R.ok(commentService.create(request, UserUtil.requireFrontUser().getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除自己的评论，保留审核版本")
    public R<Void> delete(@PathVariable Long id) {
        commentService.delete(id, UserUtil.requireFrontUser().getId());
        return R.ok();
    }
}
