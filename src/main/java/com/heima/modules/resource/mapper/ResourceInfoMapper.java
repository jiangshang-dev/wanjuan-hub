package com.heima.modules.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.modules.resource.entity.ResourceInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ResourceInfoMapper extends BaseMapper<ResourceInfo> {
    @Select("""
            <script>
            SELECT i.* FROM resource_info i
            JOIN resource_version v ON v.resource_id = i.id AND v.version_no = i.published_version_no
            WHERE i.deleted = 0 AND i.online = 1 AND i.invalid = 0 AND i.published_version_no IS NOT NULL
            <if test="categoryId != null">AND i.category_id = #{categoryId}</if>
            <if test="accessType != null and accessType != ''">AND v.access_type = #{accessType}</if>
            <if test="keyword != null and keyword != ''">
                AND (v.title LIKE CONCAT('%', #{keyword}, '%') OR v.summary LIKE CONCAT('%', #{keyword}, '%')
                OR v.description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="tagId != null">AND EXISTS (SELECT 1 FROM resource_tag_relation tr WHERE tr.resource_id = i.id AND tr.tag_id = #{tagId})</if>
            ORDER BY i.id DESC
            </script>
            """)
    Page<ResourceInfo> selectPublicPage(Page<ResourceInfo> page, @Param("categoryId") Long categoryId,
                                        @Param("tagId") Long tagId, @Param("accessType") String accessType,
                                        @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT i.* FROM resource_info i
            JOIN resource_version v ON v.resource_id = i.id AND v.version_no = i.latest_version_no
            WHERE i.deleted = 0
            <if test="categoryId != null">AND i.category_id = #{categoryId}</if>
            <if test="status != null and status != ''">AND i.status = #{status}</if>
            <if test="keyword != null and keyword != ''">
                AND (v.title LIKE CONCAT('%', #{keyword}, '%') OR v.summary LIKE CONCAT('%', #{keyword}, '%')
                OR v.description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY i.id DESC
            </script>
            """)
    Page<ResourceInfo> selectAdminPage(Page<ResourceInfo> page, @Param("categoryId") Long categoryId,
                                       @Param("status") String status, @Param("keyword") String keyword);
}
