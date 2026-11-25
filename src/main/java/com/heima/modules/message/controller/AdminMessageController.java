package com.heima.modules.message.controller;

import com.heima.common.R;
import com.heima.modules.message.dto.MessageDtos.SystemMessageRequest;
import com.heima.modules.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
@Tag(name = "后台消息")
public class AdminMessageController {
    private final MessageService messageService;

    @PostMapping("/system")
    @Operation(summary = "发送指定用户或全站系统公告消息")
    @PreAuthorize("hasAuthority('message:system:send')")
    public R<Integer> send(@Valid @RequestBody SystemMessageRequest request) {
        return R.ok(messageService.sendSystemMessage(request));
    }
}
