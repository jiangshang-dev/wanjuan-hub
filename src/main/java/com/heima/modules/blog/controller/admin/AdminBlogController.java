package com.heima.modules.blog.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.blog.dto.AdminBlogStatusDTO;
import com.heima.modules.blog.dto.BlogQueryDTO;
import com.heima.modules.blog.service.BlogService;
import com.heima.modules.blog.vo.BlogArticleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/blogs")
@RequiredArgsConstructor
@Tag(name = "后台博客管理")
public class AdminBlogController {
    private final BlogService blogService;

    @GetMapping
    @Operation(summary = "分页查询文章最新版本")
    @PreAuthorize("hasAuthority('blog:article:list')")
    public R<PageResult<BlogArticleVO>> page(@Valid @ModelAttribute BlogQueryDTO query) {
        return R.ok(blogService.adminPage(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询文章最新版本详情")
    @PreAuthorize("hasAuthority('blog:article:detail')")
    public R<BlogArticleVO> detail(@PathVariable Long id) {
        return R.ok(blogService.adminDetail(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "恢复公开或下线文章")
    @PreAuthorize("hasAuthority('blog:article:status')")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody AdminBlogStatusDTO request) {
        blogService.adminStatus(id, request);
        return R.ok();
    }
}
