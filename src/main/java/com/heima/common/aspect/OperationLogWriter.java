package com.heima.common.aspect;

import com.heima.modules.system.entity.OperationLogEntity;
import com.heima.modules.system.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogWriter {
    private final OperationLogMapper mapper;
    @Async("operationLogExecutor")
    public void save(OperationLogEntity entity) {
        try { mapper.insert(entity); } catch (Exception e) { log.error("Unable to persist operation log, traceId={}",entity.getTraceId(),e); }
    }
}
