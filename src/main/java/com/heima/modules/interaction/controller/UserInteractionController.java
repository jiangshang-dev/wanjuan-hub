package com.heima.modules.interaction.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.interaction.dto.InteractionDtos.Query;
import com.heima.modules.interaction.dto.InteractionDtos.TargetRequest;
import com.heima.modules.interaction.service.InteractionService;
import com.heima.modules.interaction.vo.InteractionVos.FollowVO;
import com.heima.modules.interaction.vo.InteractionVos.TargetVO;
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
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户点赞、收藏与关注")
public class UserInteractionController {
    private final InteractionService interactionService;

    @PostMapping("/likes")
    @Operation(summary = "点赞公开目标，同一用户目标唯一")
    public R<Void> like(@Valid @RequestBody TargetRequest request) {
        interactionService.like(request, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @DeleteMapping("/likes")
    @Operation(summary = "取消自己的点赞并原子递减计数")
    public R<Void> unlike(@Valid @RequestBody TargetRequest request) {
        interactionService.unlike(request, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @GetMapping("/likes")
    @Operation(summary = "分页查询自己的点赞记录")
    public R<PageResult<TargetVO>> likes(@Valid @ModelAttribute Query query) {
        return R.ok(interactionService.likes(query, UserUtil.requireFrontUser().getId()));
    }

    @PostMapping("/favorites")
    @Operation(summary = "收藏公开目标，同一用户目标唯一")
    public R<Void> favorite(@Valid @RequestBody TargetRequest request) {
        interactionService.favorite(request, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @DeleteMapping("/favorites")
    @Operation(summary = "取消自己的收藏并原子递减计数")
    public R<Void> unfavorite(@Valid @RequestBody TargetRequest request) {
        interactionService.unfavorite(request, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @GetMapping("/favorites")
    @Operation(summary = "分页查询自己的收藏记录")
    public R<PageResult<TargetVO>> favorites(@Valid @ModelAttribute Query query) {
        return R.ok(interactionService.favorites(query, UserUtil.requireFrontUser().getId()));
    }

    @PostMapping("/follows/{followeeId}")
    @Operation(summary = "关注用户，禁止自关注和重复关注")
    public R<Void> follow(@PathVariable Long followeeId) {
        interactionService.follow(followeeId, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @DeleteMapping("/follows/{followeeId}")
    @Operation(summary = "取消关注")
    public R<Void> unfollow(@PathVariable Long followeeId) {
        interactionService.unfollow(followeeId, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @GetMapping("/follows/following")
    @Operation(summary = "分页查询自己的关注列表")
    public R<PageResult<FollowVO>> following(@Valid @ModelAttribute Query query) {
        return R.ok(interactionService.following(query, UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/follows/followers")
    @Operation(summary = "分页查询自己的粉丝列表")
    public R<PageResult<FollowVO>> followers(@Valid @ModelAttribute Query query) {
        return R.ok(interactionService.followers(query, UserUtil.requireFrontUser().getId()));
    }
}
