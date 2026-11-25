package com.heima.modules.blog.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.blog.dto.BlogQueryDTO;
import com.heima.modules.blog.dto.BlogSaveDTO;
import com.heima.modules.blog.service.BlogService;
import com.heima.modules.blog.vo.BlogArticleVO;
import com.heima.modules.blog.vo.BlogVersionVO;
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
@RequestMapping("/api/user/blogs")
@RequiredArgsConstructor
@Tag(name = "用户博客")
public class UserBlogController {
    private final BlogService blogService;

    @GetMapping
    @Operation(summary = "查询自己的文章")
    public R<PageResult<BlogArticleVO>> page(@Valid @ModelAttribute BlogQueryDTO query) {
        return R.ok(blogService.userPage(query, UserUtil.requireFrontUser().getId()));
    }

    @PostMapping
    @Operation(summary = "创建草稿")
    public R<Long> create(@Valid @RequestBody BlogSaveDTO request) {
        return R.ok(blogService.create(request, UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看自己的最新版本及审核状态")
    public R<BlogArticleVO> detail(@PathVariable Long id) {
        return R.ok(blogService.userDetail(id, UserUtil.requireFrontUser().getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑并生成新版本，旧公开版本继续展示")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody BlogSaveDTO request) {
        blogService.update(id, request, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章入口，保留审核与版本记录")
    public R<Void> delete(@PathVariable Long id) {
        blogService.delete(id, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交最新草稿审核")
    public R<Void> submit(@PathVariable Long id) {
        blogService.submit(id, UserUtil.requireFrontUser().getId());
        return R.ok();
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "分页查看自己的历史版本")
    public R<PageResult<BlogVersionVO>> versions(@PathVariable Long id, @Valid @ModelAttribute BlogQueryDTO query) {
        return R.ok(blogService.versions(id, query, UserUtil.requireFrontUser().getId()));
    }
}
