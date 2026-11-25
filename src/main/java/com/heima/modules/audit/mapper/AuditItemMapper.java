package com.heima.modules.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.audit.entity.AuditItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditItemMapper extends BaseMapper<AuditItem> {
}
