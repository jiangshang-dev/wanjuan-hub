package com.heima.modules.operation.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.operation.dto.FeedbackDtos.CreateRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/feedbacks")
@RequiredArgsConstructor
@Tag(name = "用户意见反馈")
public class UserFeedbackController {
    private final FeedbackService feedbackService;

    @PostMapping @Operation(summary = "提交意见反馈")
    public R<String> create(@Valid @RequestBody CreateRequest request) {
        return R.ok(feedbackService.create(UserUtil.requireFrontUser().getId(), request));
    }

    @GetMapping @Operation(summary = "分页查看自己的反馈及回复")
    public R<PageResult<FeedbackVO>> page(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Pattern(regexp = "PENDING|REPLIED|CLOSED") String status) {
        return R.ok(feedbackService.userPage(UserUtil.requireFrontUser().getId(), status, page, size));
    }

    @GetMapping("/{id}") @Operation(summary = "查看自己的单条反馈及回复")
    public R<FeedbackVO> detail(@PathVariable @Positive Long id) {
        return R.ok(feedbackService.userDetail(UserUtil.requireFrontUser().getId(), id));
    }
}
