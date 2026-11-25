package com.heima.modules.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.modules.search.entity.SearchDocument;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SearchDocumentMapper extends BaseMapper<SearchDocument> {
    @Select("""
            <script>
            SELECT id,content_type,content_id,title,summary,content,url,create_time,update_time,
            MATCH(title,summary,content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) AS score
            FROM search_document
            WHERE MATCH(title,summary,content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)
            <if test="type != null and type != ''">AND content_type=#{type}</if>
            ORDER BY score DESC,id DESC
            </script>
            """)
    Page<SearchDocument> search(Page<SearchDocument> page, @Param("keyword") String keyword,
                                @Param("type") String type);

    @Insert("""
            INSERT INTO search_document(content_type,content_id,title,summary,content,url)
            VALUES(#{contentType},#{contentId},#{title},#{summary},#{content},#{url})
            ON DUPLICATE KEY UPDATE title=VALUES(title),summary=VALUES(summary),
            content=VALUES(content),url=VALUES(url)
            """)
    int upsert(SearchDocument document);

    @Insert("INSERT INTO search_hot_keyword(keyword,heat) VALUES(#{keyword},1) ON DUPLICATE KEY UPDATE heat=heat+1")
    int incrementHotKeyword(@Param("keyword") String keyword);
}
