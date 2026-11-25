package com.heima.modules.operation.controller;

import com.heima.common.R;
import com.heima.modules.operation.service.OperationService;
import com.heima.modules.operation.vo.OperationVOs.AnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Tag(name = "公开运营公告")
public class PublicAnnouncementController {
    private final OperationService operationService;

    @GetMapping @Operation(summary = "当前发布时间范围内生效的公告")
    public R<List<AnnouncementVO>> announcements(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return R.ok(operationService.publicAnnouncements(limit));
    }
}
