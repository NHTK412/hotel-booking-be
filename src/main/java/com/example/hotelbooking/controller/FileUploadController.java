package com.example.hotelbooking.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.hotelbooking.dto.fileupload.FileUploadResponseDTO;
import com.example.hotelbooking.service.FileUploadService;
import com.example.hotelbooking.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "9. Tải Tệp & Hình Ảnh (File Upload CDN)", description = "Các API tải ảnh đơn lẻ hoặc upload hàng loạt lên dịch vụ đám mây Cloudinary CDN")
@RestController
@RequestMapping("/file-upload")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @Operation(summary = "Tải một ảnh lên Cloudinary CDN", description = "Tải một tệp hình ảnh (JPG, PNG, WebP) lên máy chủ CDN và trả về đường dẫn URL công khai")
    @PostMapping(value = "/cdn", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponseDTO>> uploadToCdn(@RequestParam("file") MultipartFile file)
            throws IOException {
        FileUploadResponseDTO fileUploadResponseDTO = fileUploadService.uploadFileToCloudinary(file);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tải ảnh lên CDN thành công", fileUploadResponseDTO));
    }

    @Operation(summary = "Tải nhiều ảnh cùng lúc lên Cloudinary CDN", description = "Tải một danh sách nhiều hình ảnh cùng một lúc lên CDN và nhận về danh sách các URL tương ứng")
    @PostMapping(value = "/cdn/multiple", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<List<FileUploadResponseDTO>>> uploadMultipleFilesToCdn(
            @RequestParam("files") List<MultipartFile> files)
            throws IOException {
        List<FileUploadResponseDTO> fileUploadResponseDTOs = fileUploadService.uploadMultipleFilesToCloudinary(files);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tải nhiều ảnh lên CDN thành công", fileUploadResponseDTOs));
    }
}
