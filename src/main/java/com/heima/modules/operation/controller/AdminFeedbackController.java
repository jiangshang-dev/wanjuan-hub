package com.heima.modules.operation.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.operation.dto.FeedbackDtos.ReplyRequest;
import com.heima.modules.operation.service.FeedbackService;
import com.heima.modules.operation.vo.FeedbackVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/operation/feedbacks")
@RequiredArgsConstructor
@Tag(name = "后台意见反馈处理")
public class AdminFeedbackController {
    private final FeedbackService feedbackService;

    @GetMapping @Operation(summary = "分页查看用户意见反馈")
    @PreAuthorize("hasAuthority('operation:feedback:list')")
    public R<PageResult<FeedbackVO>> page(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Pattern(regexp = "PENDING|REPLIED|CLOSED") String status,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @Size(max = 200) String keyword) {
        return R.ok(feedbackService.adminPage(status, userId, keyword, page, size));
    }

    @PutMapping("/{id}/reply") @Operation(summary = "回复意见反馈并提醒提交用户")
    @OperationLog(module = "意见反馈", value = "回复反馈", targetId = "#id")
    @PreAuthorize("hasAuthority('operation:feedback:reply')")
    public R<Void> reply(@PathVariable @Positive Long id, @Valid @RequestBody ReplyRequest request) {
        feedbackService.reply(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }

    @PutMapping("/{id}/close") @Operation(summary = "关闭意见反馈")
    @OperationLog(module = "意见反馈", value = "关闭反馈", targetId = "#id")
    @PreAuthorize("hasAuthority('operation:feedback:close')")
    public R<Void> close(@PathVariable @Positive Long id) {
        feedbackService.close(id);
        return R.ok();
    }
}
