package com.heima.modules.file.controller;

import com.heima.common.R;
import com.heima.modules.file.service.FileService;
import com.heima.modules.file.vo.FileAccessVO;
import com.heima.modules.file.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件管理")
public class FileController {
    private final FileService fileService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传图片到私有待审核区")
    public R<FileUploadVO> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(fileService.uploadImage(file));
    }

    @GetMapping("/{id}/access")
    @Operation(summary = "鉴权获取短时效访问地址")
    public R<FileAccessVO> access(@PathVariable Long id) {
        return R.ok(fileService.access(id));
    }
}
