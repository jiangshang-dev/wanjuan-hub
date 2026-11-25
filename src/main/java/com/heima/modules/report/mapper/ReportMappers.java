package com.heima.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.report.entity.ReportEntities.Handle;
import com.heima.modules.report.entity.ReportEntities.Report;
import org.apache.ibatis.annotations.Mapper;

public final class ReportMappers {
    private ReportMappers() {}
    @Mapper public interface ReportMapper extends BaseMapper<Report> {}
    @Mapper public interface HandleMapper extends BaseMapper<Handle> {}
}
