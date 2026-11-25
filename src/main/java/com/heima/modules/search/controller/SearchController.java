package com.heima.modules.search.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.search.dto.SearchDtos.SearchResultVO;
import com.heima.modules.search.service.SearchService;
import com.heima.security.context.LoginUser;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "公开内容搜索")
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "使用MySQL ngram全文索引搜索公开内容")
    public R<PageResult<SearchResultVO>> search(@RequestParam(defaultValue = "") @Size(max = 100) String keyword,
                                                @RequestParam(required = false) @Size(max = 24) String type,
                                                @RequestParam(defaultValue = "1") @Min(1) long page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        LoginUser user = UserUtil.frontUserOrNull();
        return R.ok(searchService.search(keyword, type, page, size, user == null ? null : user.getId()));
    }

    @GetMapping("/hot-keywords")
    @Operation(summary = "真实热门搜索词")
    public R<List<String>> hotKeywords(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return R.ok(searchService.hotKeywords(limit));
    }

    @GetMapping("/history")
    @Operation(summary = "当前登录用户搜索历史，游客返回空列表")
    public R<List<String>> history(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        LoginUser user = UserUtil.frontUserOrNull();
        return R.ok(searchService.history(user == null ? null : user.getId(), limit));
    }
}
