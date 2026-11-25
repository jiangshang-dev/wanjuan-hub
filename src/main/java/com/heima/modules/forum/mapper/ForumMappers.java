package com.heima.modules.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.forum.entity.ForumEntities.*;
import org.apache.ibatis.annotations.Mapper;

public final class ForumMappers {
    private ForumMappers() {}
    @Mapper public interface SectionMapper extends BaseMapper<Section> {}
    @Mapper public interface BoardMapper extends BaseMapper<Board> {}
    @Mapper public interface ModeratorMapper extends BaseMapper<Moderator> {}
    @Mapper public interface TopicMapper extends BaseMapper<Topic> {}
    @Mapper public interface TopicVersionMapper extends BaseMapper<TopicVersion> {}
    @Mapper public interface PostMapper extends BaseMapper<Post> {}
    @Mapper public interface PostVersionMapper extends BaseMapper<PostVersion> {}
}
