package com.heima.modules.message.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.message.dto.MessageDtos.MessageVO;
import com.heima.modules.message.dto.MessageDtos.PreferenceRequest;
import com.heima.modules.message.dto.MessageDtos.PreferenceVO;
import com.heima.modules.message.service.MessageService;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "用户消息")
public class UserMessageController {
    private final MessageService messageService;

    @GetMapping("/api/user/messages")
    @Operation(summary = "我的消息分页")
    public R<PageResult<MessageVO>> page(@RequestParam(required = false) @Size(max = 24) String type,
                                         @RequestParam(required = false) Boolean read,
                                         @RequestParam(defaultValue = "1") @Min(1) long page,
                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return R.ok(messageService.page(UserUtil.requireFrontUser().getId(), type, read, page, size));
    }

    @GetMapping("/api/user/messages/unread-count")
    @Operation(summary = "未读消息数")
    public R<Long> unreadCount() {
        return R.ok(messageService.unreadCount(UserUtil.requireFrontUser().getId()));
    }

    @PutMapping("/api/user/messages/{id}/read")
    @Operation(summary = "标记我的单条消息已读")
    public R<Void> read(@PathVariable Long id) {
        messageService.markRead(UserUtil.requireFrontUser().getId(), id);
        return R.ok();
    }

    @PutMapping("/api/user/messages/read-all")
    @Operation(summary = "我的消息全部已读")
    public R<Integer> readAll() {
        return R.ok(messageService.markAllRead(UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/api/user/message-preferences")
    @Operation(summary = "消息偏好")
    public R<PreferenceVO> preferences() {
        return R.ok(messageService.preferences(UserUtil.requireFrontUser().getId()));
    }

    @PutMapping("/api/user/message-preferences")
    @Operation(summary = "修改消息偏好")
    public R<PreferenceVO> updatePreferences(@Valid @RequestBody PreferenceRequest request) {
        return R.ok(messageService.updatePreferences(UserUtil.requireFrontUser().getId(), request));
    }
}
