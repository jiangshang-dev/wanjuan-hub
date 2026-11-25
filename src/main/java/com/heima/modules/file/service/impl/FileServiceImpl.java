package com.heima.modules.file.service.impl;

import com.heima.exception.ServiceException;
import com.heima.infrastructure.storage.ObjectStorageService;
import com.heima.infrastructure.storage.RustFsProperties;
import com.heima.modules.audit.dto.AuditSubmitItem;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.service.AuditService;
import com.heima.modules.file.entity.FileObject;
import com.heima.modules.file.entity.FileReference;
import com.heima.modules.file.enums.FileType;
import com.heima.modules.file.enums.FileVisibility;
import com.heima.modules.file.mapper.FileObjectMapper;
import com.heima.modules.file.mapper.FileReferenceMapper;
import com.heima.modules.file.service.FileService;
import com.heima.modules.file.vo.FileAccessVO;
import com.heima.modules.file.vo.FileUploadVO;
import com.heima.security.context.LoginUser;
import com.heima.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private static final Map<String, String> IMAGE_MIME = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "gif", "image/gif", "webp", "image/webp");
    private final FileObjectMapper fileMapper;
    private final FileReferenceMapper referenceMapper;
    private final ObjectStorageService storageService;
    private final RustFsProperties properties;
    private final AuditService auditService;

    @Override
    @Transactional
    public FileUploadVO uploadImage(MultipartFile multipartFile) {
        storageService.ensureAvailable();
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ServiceException(400, "请选择图片");
        }
        if (multipartFile.getSize() > properties.getMaxImageBytes()) {
            throw new ServiceException(400, "图片大小不能超过" + properties.getMaxImageBytes() / 1024 / 1024 + "MB");
        }
        String originalName = multipartFile.getOriginalFilename() == null ? "image" : multipartFile.getOriginalFilename();
        String extension = extension(originalName);
        String expectedMime = IMAGE_MIME.get(extension);
        String actualMime = multipartFile.getContentType() == null ? "" : multipartFile.getContentType().toLowerCase(Locale.ROOT);
        if (expectedMime == null || !expectedMime.equals(actualMime)) {
            throw new ServiceException(400, "仅支持 JPG、PNG、GIF、WEBP 图片且扩展名与MIME必须一致");
        }
        byte[] bytes;
        try {
            bytes = multipartFile.getBytes();
        } catch (IOException e) {
            throw new ServiceException(400, "读取上传图片失败", e);
        }
        if (bytes.length == 0 || bytes.length > properties.getMaxImageBytes() || !validMagic(bytes, extension)) {
            throw new ServiceException(400, "图片内容与扩展名不一致");
        }
        String sha256 = sha256(bytes);
        String key = "audit/" + LocalDateTime.now().toLocalDate() + "/" + UUID.randomUUID() + "." + extension;
        storageService.upload(properties.getAuditBucket(), key, bytes, expectedMime);
        try {
            FileObject file = new FileObject();
            file.setOwnerId(UserUtil.requireUserId());
            file.setFileType(FileType.IMAGE.name());
            file.setBucketName(properties.getAuditBucket());
            file.setObjectKey(key);
            file.setOriginalName(originalName.length() > 255 ? originalName.substring(originalName.length() - 255) : originalName);
            file.setExtension(extension);
            file.setMimeType(expectedMime);
            file.setSizeBytes((long) bytes.length);
            file.setSha256(sha256);
            file.setVisibility(FileVisibility.PRIVATE.name());
            file.setAuditStatus(AuditStatus.PENDING.name());
            fileMapper.insert(file);
            auditService.submit("FILE_IMAGE", file.getId(), 1, List.of(AuditSubmitItem.image(file.getId())));
            FileUploadVO vo = new FileUploadVO();
            BeanUtils.copyProperties(file, vo);
            vo.setAccessUrl(presign(file, properties.getPresignExpire()));
            return vo;
        } catch (RuntimeException e) {
            try {
                storageService.delete(properties.getAuditBucket(), key);
            } catch (RuntimeException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        }
    }

    @Override
    public FileAccessVO access(Long id) {
        LoginUser user = UserUtil.requireUser();
        FileObject file = getRequired(id);
        boolean publicApproved = FileVisibility.PUBLIC.name().equals(file.getVisibility())
                && AuditStatus.APPROVED.name().equals(file.getAuditStatus());
        if (!publicApproved && !user.isAdmin() && !file.getOwnerId().equals(user.getId())) {
            throw new ServiceException(403, "无权访问该文件");
        }
        Duration expire = properties.getPresignExpire();
        FileAccessVO vo = new FileAccessVO();
        vo.setId(id);
        vo.setUrl(presign(file, expire));
        vo.setExpiresAt(LocalDateTime.now().plus(expire));
        return vo;
    }

    @Override
    public FileObject getRequired(Long id) {
        FileObject file = fileMapper.selectById(id);
        if (file == null) {
            throw new ServiceException(404, "文件不存在");
        }
        return file;
    }

    @Override
    public List<FileObject> requireOwnedImages(Collection<Long> fileIds, Long ownerId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        Set<Long> distinct = new LinkedHashSet<>(fileIds);
        if (distinct.contains(null)) {
            throw new ServiceException(400, "文件ID不能为空");
        }
        List<FileObject> files = fileMapper.selectByIds(distinct);
        Map<Long, FileObject> indexed = files.stream().collect(Collectors.toMap(FileObject::getId, Function.identity()));
        List<FileObject> ordered = new ArrayList<>();
        for (Long id : distinct) {
            FileObject file = indexed.get(id);
            if (file == null || !FileType.IMAGE.name().equals(file.getFileType()) || !file.getOwnerId().equals(ownerId)) {
                throw new ServiceException(403, "图片不存在、类型错误或不属于当前用户：" + id);
            }
            ordered.add(file);
        }
        return ordered;
    }

    @Override
    @Transactional
    public void bindReferences(Collection<Long> fileIds, String bizType, Long bizId, Integer versionNo) {
        if (fileIds == null) return;
        for (Long fileId : new LinkedHashSet<>(fileIds)) {
            FileReference reference = new FileReference();
            reference.setFileId(fileId);
            reference.setBizType(bizType);
            reference.setBizId(bizId);
            reference.setVersionNo(versionNo);
            referenceMapper.insert(reference);
        }
    }

    @Override
    public String presign(FileObject file, Duration expire) {
        boolean usePublic = FileVisibility.PUBLIC.name().equals(file.getVisibility()) && file.getPublicBucket() != null;
        return storageService.presignGet(usePublic ? file.getPublicBucket() : file.getBucketName(),
                usePublic ? file.getPublicObjectKey() : file.getObjectKey(), expire);
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean validMagic(byte[] bytes, String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> startsWith(bytes, 'G', 'I', 'F', '8') && bytes.length >= 6 && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
            case "webp" -> bytes.length >= 12 && startsWith(bytes, 'R', 'I', 'F', 'F')
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != (prefix[i] & 0xFF)) return false;
        }
        return true;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
