package com.heima.modules.resource.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.resource.dto.ResourceInvalidReportDTO;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.dto.ResourceSaveDTO;
import com.heima.modules.resource.service.ResourceEntitlementService;
import com.heima.modules.resource.service.ResourceReportService;
import com.heima.modules.resource.service.ResourceService;
import com.heima.modules.resource.service.ResourceTradeService;
import com.heima.modules.resource.vo.ResourceEntitlementVO;
import com.heima.modules.resource.vo.ResourceVO;
import com.heima.modules.resource.vo.ResourceVersionVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/resources")
@RequiredArgsConstructor
@Tag(name = "用户资源")
public class UserResourceController {
    private final ResourceService resourceService;
    private final ResourceTradeService tradeService;
    private final ResourceEntitlementService entitlementService;
    private final ResourceReportService reportService;

    @GetMapping
    @Operation(summary = "查询自己的资源最新版本")
    public R<PageResult<ResourceVO>> page(@Valid @ModelAttribute ResourceQueryDTO query) {
        return R.ok(resourceService.userPage(query, UserUtil.requireFrontUser().getId()));
    }

    @PostMapping
    @Operation(summary = "创建资源草稿并加密保存多网盘链接")
    public R<Long> create(@Valid @RequestBody ResourceSaveDTO request) {
        return R.ok(resourceService.create(request, UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/entitlements")
    @Operation(summary = "分页查询自己的资源权益")
    public R<PageResult<ResourceEntitlementVO>> entitlements(@Valid @ModelAttribute ResourceQueryDTO query) {
        return R.ok(entitlementService.userPage(UserUtil.requireFrontUser().getId(), query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看自己的最新版本及审核状态")
    public R<ResourceVO> detail(@PathVariable Long id) {
        return R.ok(resourceService.userDetail(id, UserUtil.requireFrontUser().getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑并生成新版本，旧公开版本继续展示")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ResourceSaveDTO request) {
        resourceService.update(id, request, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资源入口并停止公开访问")
    public R<Void> delete(@PathVariable Long id) {
        resourceService.delete(id, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "校验链接和加密配置后提交最新草稿审核")
    public R<Void> submit(@PathVariable Long id) {
        resourceService.submit(id, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "分页查看自己的历史版本")
    public R<PageResult<ResourceVersionVO>> versions(@PathVariable Long id, @Valid @ModelAttribute ResourceQueryDTO query) {
        return R.ok(resourceService.versions(id, query, UserUtil.requireFrontUser().getId()));
    }

    @PostMapping("/{id}/redeem")
    @Operation(summary = "事务化积分兑换，重复兑换不扣积分")
    public R<Void> redeem(@PathVariable Long id) {
        tradeService.redeemPoints(UserUtil.requireFrontUser().getId(), id);
        return R.ok();
    }

    @PostMapping("/{id}/invalid-reports")
    @Operation(summary = "举报公开资源或指定链接失效")
    public R<Long> report(@PathVariable Long id, @Valid @RequestBody ResourceInvalidReportDTO request) {
        return R.ok(reportService.create(id, request, UserUtil.requireFrontUser().getId()));
    }
}
