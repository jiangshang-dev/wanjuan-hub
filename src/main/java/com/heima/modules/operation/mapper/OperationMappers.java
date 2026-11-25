package com.heima.modules.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.operation.entity.OperationEntities.Advertisement;
import com.heima.modules.operation.entity.OperationEntities.Announcement;
import com.heima.modules.operation.entity.OperationEntities.FriendLink;
import com.heima.modules.operation.entity.OperationEntities.HotKeyword;
import com.heima.modules.operation.entity.OperationEntities.Recommendation;
import com.heima.modules.operation.entity.OperationEntities.SiteConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public final class OperationMappers {
    private OperationMappers() {}

    @Mapper
    public interface AnnouncementMapper extends BaseMapper<Announcement> {
        @Select("""
                SELECT * FROM operation_notice
                WHERE status=1 AND (publish_start IS NULL OR publish_start<=#{now})
                  AND (publish_end IS NULL OR publish_end>#{now})
                ORDER BY sort_order ASC,id DESC LIMIT #{limit}
                """)
        List<Announcement> selectPublic(@Param("now") LocalDateTime now, @Param("limit") int limit);
    }

    @Mapper
    public interface RecommendationMapper extends BaseMapper<Recommendation> {
        @Select("""
                SELECT r.* FROM operation_recommend r
                WHERE r.status=1 AND (
                  (r.content_type='BLOG' AND EXISTS (SELECT 1 FROM blog_article b
                    WHERE b.id=r.content_id AND b.deleted=0 AND b.status!='OFFLINE' AND b.published_version_no IS NOT NULL))
                  OR (r.content_type='RESOURCE' AND EXISTS (SELECT 1 FROM resource_info i
                    WHERE i.id=r.content_id AND i.deleted=0 AND i.online=1 AND i.invalid=0 AND i.published_version_no IS NOT NULL))
                  OR (r.content_type='FORUM_TOPIC' AND EXISTS (SELECT 1 FROM forum_topic t
                    WHERE t.id=r.content_id AND t.deleted=0 AND t.status!='OFFLINE' AND t.published_version_no IS NOT NULL))
                )
                ORDER BY r.sort_order ASC,r.id DESC LIMIT #{limit}
                """)
        List<Recommendation> selectPublic(@Param("limit") int limit);
    }

    @Mapper
    public interface AdvertisementMapper extends BaseMapper<Advertisement> {
        @Select("""
                <script>
                SELECT * FROM operation_advertisement
                WHERE status=1 AND (start_time IS NULL OR start_time&lt;=#{now})
                  AND (end_time IS NULL OR end_time&gt;#{now})
                <if test="position != null and position != ''">AND position=#{position}</if>
                ORDER BY sort_order ASC,id DESC LIMIT #{limit}
                </script>
                """)
        List<Advertisement> selectPublic(@Param("position") String position, @Param("now") LocalDateTime now,
                                         @Param("limit") int limit);
    }

    @Mapper
    public interface FriendLinkMapper extends BaseMapper<FriendLink> {
        @Select("SELECT * FROM operation_friend_link WHERE status=1 ORDER BY sort_order ASC,id DESC LIMIT #{limit}")
        List<FriendLink> selectPublic(@Param("limit") int limit);
    }

    @Mapper
    public interface HotKeywordMapper extends BaseMapper<HotKeyword> {
        @Select("SELECT * FROM search_hot_keyword WHERE status=1 ORDER BY sort_order ASC,heat DESC,id DESC LIMIT #{limit}")
        List<HotKeyword> selectPublic(@Param("limit") int limit);
    }

    @Mapper
    public interface ConfigMapper extends BaseMapper<SiteConfig> {
        @Select("SELECT * FROM sys_config WHERE public_flag=1 AND status=1 ORDER BY id ASC LIMIT #{limit}")
        List<SiteConfig> selectPublic(@Param("limit") int limit);
    }
}
