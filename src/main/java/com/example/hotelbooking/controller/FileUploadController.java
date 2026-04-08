package com.example.hotelbooking.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.hotelbooking.service.FileUploadService;
import com.example.hotelbooking.util.ApiResponse;
import com.example.hotelbooking.dto.fileupload.FileUploadResponseDTO;
import org.springframework.web.bind.annotation.RequestBody;

// import com.example.hotelbooking.dto.fileupload.FileUploadResponseDTO;
// import com.example.hotelbooking.service.FileUploadService;
// import com.example.evsalesmanagement.utils.ApiResponse;

@RestController
@RequestMapping("file-upload")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping(value = "/cdn", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponseDTO>> uploadToCdn(@RequestParam("file") MultipartFile file)
            throws IOException {
        FileUploadResponseDTO fileUploadResponseDTO = fileUploadService.uploadFileToCloudinary(file);
        return ResponseEntity.ok(new ApiResponse<>(true, "Upload successful", fileUploadResponseDTO));
    }

    @PostMapping(value = "/cdn/multiple", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<List<FileUploadResponseDTO>>> uploadMultipleFilesToCdn(
            @RequestParam("files") List<MultipartFile> files)
            throws IOException {
        List<FileUploadResponseDTO> fileUploadResponseDTOs = fileUploadService.uploadMultipleFilesToCloudinary(files);
        return ResponseEntity.ok(new ApiResponse<>(true, "Upload successful", fileUploadResponseDTOs));
    }
}
