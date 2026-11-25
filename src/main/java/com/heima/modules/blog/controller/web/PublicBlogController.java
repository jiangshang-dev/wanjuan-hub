package com.heima.modules.blog.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.blog.dto.BlogQueryDTO;
import com.heima.modules.blog.service.BlogService;
import com.heima.modules.blog.vo.BlogArticleVO;
import com.heima.modules.blog.vo.BlogCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "公开博客")
public class PublicBlogController {
    private final BlogService blogService;

    @GetMapping("/api/blogs")
    @Operation(summary = "分页查询当前公开版本")
    public R<PageResult<BlogArticleVO>> page(@Valid @ModelAttribute BlogQueryDTO query) {
        return R.ok(blogService.publicPage(query));
    }

    @GetMapping("/api/blogs/{id}")
    @Operation(summary = "查看当前公开版本")
    public R<BlogArticleVO> detail(@PathVariable Long id) {
        return R.ok(blogService.publicDetail(id));
    }

    @GetMapping("/api/blog-categories")
    @Operation(summary = "查询启用分类，最多100条")
    public R<List<BlogCategoryVO>> categories() {
        return R.ok(blogService.publicCategories());
    }
}
