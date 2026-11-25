package com.heima.modules.file.service.impl;

import com.heima.exception.ServiceException;
import com.heima.infrastructure.storage.ObjectStorageService;
import com.heima.modules.audit.service.AuditImageResolver;
import com.heima.modules.file.entity.FileObject;
import com.heima.modules.file.mapper.FileObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuditImageResolverImpl implements AuditImageResolver {
    private final FileObjectMapper fileMapper;
    private final ObjectStorageService storageService;

    @Override
    public String resolveImageUrl(String content) {
        if (content == null || !content.startsWith("file:")) {
            throw new ServiceException(400, "图片审核定位符无效");
        }
        try {
            FileObject file = fileMapper.selectById(Long.valueOf(content.substring(5)));
            if (file == null) throw new ServiceException(404, "文件不存在");
            return storageService.presignGet(file.getBucketName(), file.getObjectKey(), Duration.ofMinutes(5));
        } catch (NumberFormatException e) {
            throw new ServiceException(400, "图片审核文件ID无效");
        }
    }
}
