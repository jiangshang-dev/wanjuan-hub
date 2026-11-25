package com.heima.modules.file.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.infrastructure.storage.ObjectStorageService;
import com.heima.infrastructure.storage.RustFsProperties;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.file.entity.FileObject;
import com.heima.modules.file.entity.FileReference;
import com.heima.modules.file.enums.FileType;
import com.heima.modules.file.enums.FileVisibility;
import com.heima.modules.file.mapper.FileObjectMapper;
import com.heima.modules.file.mapper.FileReferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FileAuditListener {
    private final FileObjectMapper fileMapper;
    private final FileReferenceMapper referenceMapper;
    private final ObjectStorageService storageService;
    private final RustFsProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCompleted(AuditCompletedEvent event) {
        if ("FILE_IMAGE".equals(event.bizType())) {
            FileObject file = fileMapper.selectById(event.bizId());
            if (file != null) {
                if (event.status() == AuditStatus.APPROVED) {
                    promote(file);
                } else {
                    fileMapper.update(null, new LambdaUpdateWrapper<FileObject>().eq(FileObject::getId, file.getId())
                            .ne(FileObject::getAuditStatus, AuditStatus.APPROVED.name())
                            .set(FileObject::getAuditStatus, event.status().name()).set(FileObject::getAuditReason, event.reason()));
                }
            }
            return;
        }
        if (event.status() == AuditStatus.APPROVED) {
            List<FileReference> references = referenceMapper.selectList(new LambdaQueryWrapper<FileReference>()
                    .eq(FileReference::getBizType, event.bizType()).eq(FileReference::getBizId, event.bizId())
                    .eq(FileReference::getVersionNo, event.versionNo()));
            for (FileReference reference : references) {
                FileObject file = fileMapper.selectById(reference.getFileId());
                if (file != null) promote(file);
            }
        }
    }

    private void promote(FileObject file) {
        if (!FileType.IMAGE.name().equals(file.getFileType())) {
            fileMapper.update(null, new LambdaUpdateWrapper<FileObject>().eq(FileObject::getId, file.getId())
                    .set(FileObject::getAuditStatus, AuditStatus.APPROVED.name())
                    .set(FileObject::getVisibility, FileVisibility.PRIVATE.name()).set(FileObject::getAuditReason, "附件审核通过，保持私有"));
            return;
        }
        String publicKey = "images/" + file.getSha256() + "/" + file.getId() + "." + file.getExtension();
        if (!FileVisibility.PUBLIC.name().equals(file.getVisibility())) {
            storageService.copy(file.getBucketName(), file.getObjectKey(), properties.getPublicBucket(), publicKey);
        }
        fileMapper.update(null, new LambdaUpdateWrapper<FileObject>().eq(FileObject::getId, file.getId())
                .set(FileObject::getAuditStatus, AuditStatus.APPROVED.name()).set(FileObject::getAuditReason, "审核通过")
                .set(FileObject::getVisibility, FileVisibility.PUBLIC.name())
                .set(FileObject::getPublicBucket, properties.getPublicBucket()).set(FileObject::getPublicObjectKey, publicKey));
    }
}
