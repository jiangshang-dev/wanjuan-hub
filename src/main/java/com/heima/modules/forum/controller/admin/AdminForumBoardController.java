package com.heima.modules.forum.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.forum.dto.ForumDtos.BoardRequest;
import com.heima.modules.forum.dto.ForumDtos.ModeratorRequest;
import com.heima.modules.forum.dto.ForumDtos.Query;
import com.heima.modules.forum.service.ForumService;
import com.heima.modules.forum.vo.ForumVos.BoardVO;
import com.heima.modules.forum.vo.ForumVos.ModeratorVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/forum")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "后台论坛板块与版主")
public class AdminForumBoardController {
    private final ForumService forumService;

    @GetMapping("/boards")
    @Operation(summary = "分页查询板块")
    @PreAuthorize("hasAuthority('forum:board:list')")
    public R<PageResult<BoardVO>> boards(@Valid @ModelAttribute Query query) { return R.ok(forumService.adminBoards(query)); }

    @PostMapping("/boards")
    @PreAuthorize("hasAuthority('forum:board:create')")
    @Operation(summary = "创建多级板块")
    public R<Long> createBoard(@Valid @RequestBody BoardRequest request) {
        return R.ok(forumService.createBoard(request, UserUtil.requireAdmin().getId()));
    }

    @PutMapping("/boards/{id}")
    @PreAuthorize("hasAuthority('forum:board:update')")
    @Operation(summary = "修改板块并校验层级循环")
    public R<Void> updateBoard(@PathVariable Long id, @Valid @RequestBody BoardRequest request) {
        forumService.updateBoard(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @DeleteMapping("/boards/{id}")
    @PreAuthorize("hasAuthority('forum:board:delete')")
    @Operation(summary = "删除无子板块且无主题的板块")
    public R<Void> deleteBoard(@PathVariable Long id) {
        forumService.deleteBoard(id, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @GetMapping("/moderators")
    @PreAuthorize("hasAuthority('forum:moderator:list')")
    @Operation(summary = "查询版主关联，最多100条")
    public R<List<ModeratorVO>> moderators(@RequestParam(required = false) Long boardId) {
        return R.ok(forumService.moderators(boardId, UserUtil.requireAdmin().getId()));
    }

    @PostMapping("/moderators")
    @PreAuthorize("hasAuthority('forum:moderator:create')")
    @Operation(summary = "新增板块版主")
    public R<Long> createModerator(@Valid @RequestBody ModeratorRequest request) {
        return R.ok(forumService.createModerator(request, UserUtil.requireAdmin().getId()));
    }

    @PutMapping("/moderators/{id}")
    @PreAuthorize("hasAuthority('forum:moderator:update')")
    @Operation(summary = "修改版主关联或启停状态")
    public R<Void> updateModerator(@PathVariable Long id, @Valid @RequestBody ModeratorRequest request) {
        forumService.updateModerator(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @DeleteMapping("/moderators/{id}")
    @PreAuthorize("hasAuthority('forum:moderator:delete')")
    @Operation(summary = "删除版主关联")
    public R<Void> deleteModerator(@PathVariable Long id) {
        forumService.deleteModerator(id, UserUtil.requireAdmin().getId());
        return R.ok();
    }
}
