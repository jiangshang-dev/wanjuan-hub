package com.heima.modules.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.comment.entity.CommentEntities.Comment;
import com.heima.modules.comment.entity.CommentEntities.Version;
import org.apache.ibatis.annotations.Mapper;

public final class CommentMappers {
    private CommentMappers() {}
    @Mapper public interface CommentMapper extends BaseMapper<Comment> {}
    @Mapper public interface VersionMapper extends BaseMapper<Version> {}
}
