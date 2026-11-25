package com.heima.modules.resource.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ResourceReplyMapper {
    @Select("""
            SELECT DISTINCT p.id FROM forum_post p
            JOIN content_resource_relation r ON r.content_type = 'FORUM_TOPIC' AND r.content_id = p.topic_id
            WHERE r.resource_id = #{resourceId} AND p.author_id = #{userId} AND p.deleted = 0
              AND p.status = 'PUBLISHED' AND p.published_version_no IS NOT NULL
            """)
    List<Long> selectPublishedPostIds(@Param("resourceId") Long resourceId, @Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT c.id FROM content_comment c
            LEFT JOIN content_resource_relation r ON r.content_type = c.target_type AND r.content_id = c.target_id
            WHERE c.author_id = #{userId} AND c.deleted = 0 AND c.status = 'PUBLISHED'
              AND c.published_version_no IS NOT NULL
              AND ((c.target_type = 'RESOURCE' AND c.target_id = #{resourceId}) OR r.resource_id = #{resourceId})
            """)
    List<Long> selectPublishedCommentIds(@Param("resourceId") Long resourceId, @Param("userId") Long userId);
}
