package com.heima.modules.interaction.controller;

import com.heima.common.R;
import com.heima.modules.interaction.service.InteractionService;
import com.heima.modules.interaction.vo.InteractionVos.ReactionVO;
import com.heima.security.context.LoginUser;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "公开赞踩状态")
public class PublicInteractionController {
    private final InteractionService interactionService;

    @GetMapping("/api/reactions/status")
    @Operation(summary = "查询目标赞踩数量、当前用户反应、作者开关及灰色日状态")
    public R<ReactionVO> status(
            @Parameter(description = "目标类型", required = true)
            @RequestParam @Pattern(regexp = "BLOG|FORUM|FORUM_POST|COMMENT") String targetType,
            @Parameter(description = "目标ID", required = true)
            @RequestParam @NotNull Long targetId) {
        LoginUser user = UserUtil.frontUserOrNull();
        return R.ok(interactionService.reactionStatus(targetType, targetId, user == null ? null : user.getId()));
    }
}
