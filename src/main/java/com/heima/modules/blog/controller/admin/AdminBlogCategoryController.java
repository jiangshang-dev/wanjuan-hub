package com.heima.modules.blog.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.blog.dto.BlogCategoryDTO;
import com.heima.modules.blog.dto.BlogQueryDTO;
import com.heima.modules.blog.service.BlogService;
import com.heima.modules.blog.vo.BlogCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin/blog-categories")
@RequiredArgsConstructor
@Tag(name = "后台博客分类")
public class AdminBlogCategoryController {
    private final BlogService blogService;

    @GetMapping
    @Operation(summary = "分页查询分类")
    @PreAuthorize("hasAuthority('blog:category:list')")
    public R<PageResult<BlogCategoryVO>> page(@Valid @ModelAttribute BlogQueryDTO query) {
        return R.ok(blogService.categoryPage(query));
    }

    @PostMapping
    @Operation(summary = "新增分类")
    @PreAuthorize("hasAuthority('blog:category:create')")
    public R<Long> create(@Valid @RequestBody BlogCategoryDTO request) {
        return R.ok(blogService.createCategory(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改分类")
    @PreAuthorize("hasAuthority('blog:category:update')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody BlogCategoryDTO request) {
        blogService.updateCategory(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除未使用分类")
    @PreAuthorize("hasAuthority('blog:category:delete')")
    public R<Void> delete(@PathVariable Long id) {
        blogService.deleteCategory(id);
        return R.ok();
    }
}
