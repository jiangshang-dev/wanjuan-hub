package com.heima.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLogEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String operatorType;
    private Long operatorId;
    private String operatorName;
    private String module;
    private String content;
    private String targetId;
    private String ip;
    private String location;
    private Boolean success;
    private String errorMessage;
    private Long costMs;
    private String traceId;
    private LocalDateTime createTime;
}
