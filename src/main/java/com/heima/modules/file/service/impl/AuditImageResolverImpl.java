package com.heima.modules.file.service.impl;

import com.heima.exception.ServiceException;
import com.heima.modules.audit.service.AuditImageResolver;
import com.heima.modules.file.entity.FileObject;
import com.heima.modules.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuditImageResolverImpl implements AuditImageResolver {
    private final FileService fileService;

    @Override
    public String resolveImageUrl(String content) {
        if (content == null || !content.startsWith("file:")) {
            throw new ServiceException(400, "图片审核定位符无效");
        }
        try {
            FileObject file = fileService.getRequired(Long.valueOf(content.substring(5)));
            return fileService.presign(file, Duration.ofMinutes(5));
        } catch (NumberFormatException e) {
            throw new ServiceException(400, "图片审核文件ID无效");
        }
    }
}
