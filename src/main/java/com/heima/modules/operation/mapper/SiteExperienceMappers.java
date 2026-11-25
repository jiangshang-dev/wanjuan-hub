package com.heima.modules.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.operation.entity.SiteExperienceEntities.BackgroundMusic;
import com.heima.modules.operation.entity.SiteExperienceEntities.GrayDay;
import org.apache.ibatis.annotations.Mapper;

public final class SiteExperienceMappers {
    private SiteExperienceMappers() {}

    @Mapper
    public interface GrayDayMapper extends BaseMapper<GrayDay> {}

    @Mapper
    public interface BackgroundMusicMapper extends BaseMapper<BackgroundMusic> {}
}
