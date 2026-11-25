package com.heima.modules.comment.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.comment.dto.CommentDtos.Query;
import com.heima.modules.comment.dto.CommentDtos.StatusRequest;
import com.heima.modules.comment.service.CommentService;
import com.heima.modules.comment.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "后台统一评论")
public class AdminCommentController {
    private final CommentService commentService;

    @GetMapping
    @PreAuthorize("hasAuthority('comment:manage:list')")
    @Operation(summary = "分页查询评论最新版本及审核状态")
    public R<PageResult<CommentVO>> page(@Valid @ModelAttribute Query query) { return R.ok(commentService.adminPage(query)); }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('comment:manage:status')")
    @Operation(summary = "恢复已审核公开版本或下线评论")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        commentService.adminStatus(id, request);
        return R.ok();
    }
}
