package com.heima.modules.comment.service;

import com.heima.exception.ServiceException;
import com.heima.modules.audit.dto.AuditSubmitItem;
import com.heima.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** S3 的统一审核入口；调用方必须在事务中执行。 */
@Service
@RequiredArgsConstructor
public class ContentAuditService {
    private final AuditService auditService;

    public Long submit(String bizType, Long bizId, Integer version, Long authorId,
                       String text, Collection<Long> imageFileIds) {
        try {
            Method unified = AuditService.class.getMethod("submit", String.class, Long.class, Integer.class,
                    Long.class, String.class, Collection.class);
            return (Long) unified.invoke(auditService, bizType, bizId, version, authorId, text, imageFileIds);
        } catch (NoSuchMethodException ignored) {
            List<AuditSubmitItem> items = new ArrayList<>();
            items.add(AuditSubmitItem.text(text));
            if (imageFileIds != null) imageFileIds.forEach(id -> items.add(AuditSubmitItem.image(id)));
            return auditService.submit(bizType, bizId, version, items);
        } catch (IllegalAccessException e) {
            throw new ServiceException(500, "审核服务不可访问", e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
            throw new ServiceException(500, "提交审核失败", e.getCause());
        }
    }
}
