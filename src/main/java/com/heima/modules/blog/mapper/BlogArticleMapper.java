package com.heima.modules.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.modules.blog.entity.BlogArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {
    @Select({"<script>", "SELECT a.* FROM blog_article a JOIN blog_article_version v ON v.article_id=a.id AND v.version_no=a.published_version_no",
            "WHERE a.deleted=0 AND a.status!='OFFLINE' AND a.published_version_no IS NOT NULL",
            "<if test='categoryId != null'> AND a.category_id=#{categoryId}</if>",
            "<if test='keyword != null and keyword != \"\"'> AND v.title LIKE CONCAT('%',#{keyword},'%')</if>",
            "ORDER BY a.update_time DESC,a.id DESC</script>"})
    Page<BlogArticle> selectPublicPage(Page<BlogArticle> page, @Param("categoryId") Long categoryId, @Param("keyword") String keyword);

    @Select({"<script>", "SELECT a.* FROM blog_article a JOIN blog_article_version v ON v.article_id=a.id AND v.version_no=a.latest_version_no",
            "WHERE a.deleted=0", "<if test='status != null and status != \"\"'> AND a.status=#{status}</if>",
            "<if test='keyword != null and keyword != \"\"'> AND v.title LIKE CONCAT('%',#{keyword},'%')</if>",
            "ORDER BY a.id DESC</script>"})
    Page<BlogArticle> selectAdminPage(Page<BlogArticle> page, @Param("status") String status, @Param("keyword") String keyword);
}
