package com.heima.modules.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.interaction.entity.InteractionEntities.*;
import org.apache.ibatis.annotations.Mapper;

public final class InteractionMappers {
    private InteractionMappers() {}
    @Mapper public interface LikeMapper extends BaseMapper<Like> {}
    @Mapper public interface FavoriteMapper extends BaseMapper<Favorite> {}
    @Mapper public interface FollowMapper extends BaseMapper<Follow> {}
}
