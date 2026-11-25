package com.heima.modules.system.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.system.entity.OperationLogEntity;
import com.heima.modules.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name="操作日志")
@RestController
@RequestMapping("/api/admin/system/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {
    private final SystemService service;
    @Operation(summary="操作日志分页") @PreAuthorize("hasAuthority('system:operation-log:list')") @GetMapping
    public R<PageResult<OperationLogEntity>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) String module,@RequestParam(required=false) Boolean success) { return R.ok(service.operationLogs(page,size,keyword,module,success)); }
}
