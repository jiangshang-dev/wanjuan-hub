package com.heima.modules.file.service;

import com.heima.modules.file.entity.FileObject;
import com.heima.modules.file.vo.FileAccessVO;
import com.heima.modules.file.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

public interface FileService {
    FileUploadVO uploadImage(MultipartFile multipartFile);
    FileAccessVO access(Long id);
    FileObject getRequired(Long id);
    List<FileObject> requireOwnedImages(Collection<Long> fileIds, Long ownerId);
    void bindReferences(Collection<Long> fileIds, String bizType, Long bizId, Integer versionNo);
    String presign(FileObject file, Duration expire);
}
