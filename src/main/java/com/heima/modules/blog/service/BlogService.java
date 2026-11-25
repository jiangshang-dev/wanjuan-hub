package com.heima.modules.blog.service;

import com.heima.common.PageResult;
import com.heima.modules.blog.dto.AdminBlogStatusDTO;
import com.heima.modules.blog.dto.BlogCategoryDTO;
import com.heima.modules.blog.dto.BlogQueryDTO;
import com.heima.modules.blog.dto.BlogSaveDTO;
import com.heima.modules.blog.vo.BlogArticleVO;
import com.heima.modules.blog.vo.BlogCategoryVO;
import com.heima.modules.blog.vo.BlogVersionVO;

import java.util.List;

public interface BlogService {
    PageResult<BlogArticleVO> publicPage(BlogQueryDTO query);
    BlogArticleVO publicDetail(Long id);
    List<BlogCategoryVO> publicCategories();
    PageResult<BlogArticleVO> userPage(BlogQueryDTO query, Long userId);
    BlogArticleVO userDetail(Long id, Long userId);
    Long create(BlogSaveDTO request, Long userId);
    void update(Long id, BlogSaveDTO request, Long userId);
    void delete(Long id, Long userId);
    void submit(Long id, Long userId);
    PageResult<BlogVersionVO> versions(Long id, BlogQueryDTO query, Long userId);
    PageResult<BlogArticleVO> adminPage(BlogQueryDTO query);
    BlogArticleVO adminDetail(Long id);
    void adminStatus(Long id, AdminBlogStatusDTO request);
    PageResult<BlogCategoryVO> categoryPage(BlogQueryDTO query);
    Long createCategory(BlogCategoryDTO request);
    void updateCategory(Long id, BlogCategoryDTO request);
    void deleteCategory(Long id);
}
