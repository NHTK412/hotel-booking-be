package com.example.hotelbooking.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.hotelbooking.dto.fileupload.FileUploadResponseDTO;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.UploadedFile;
import com.example.hotelbooking.repository.UploadedFileRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    @Value("${image-upload-path}")
    private String imagePathString;

    private final Cloudinary cloudinary;

    private final UploadedFileRepository uploadedFileRepository;

    private final static Logger logger = org.slf4j.LoggerFactory.getLogger(FileUploadService.class);

    @Transactional
    public FileUploadResponseDTO uploadFileToCloudinary(MultipartFile file) throws IOException {

        var uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "uploads",
                "resource_type", "auto"));

        FileUploadResponseDTO fileUploadResponseDTO = new FileUploadResponseDTO();
        fileUploadResponseDTO.setFileName((String) uploadResult.get("public_id"));
        fileUploadResponseDTO.setFilePath((String) uploadResult.get("secure_url"));

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setFileUrl(fileUploadResponseDTO.getFilePath());
        uploadedFile.setExpireAt(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        uploadedFileRepository.save(uploadedFile);

        return fileUploadResponseDTO;

    }

    @Scheduled(fixedDelay = 3600000)
    public void deleteExpiredFiles() {
        List<UploadedFile> expiredFiles = uploadedFileRepository.findAllExpiredFiles(System.currentTimeMillis());

        for (UploadedFile file : expiredFiles) {
            try {
                cloudinary.uploader().destroy(file.getFileUrl(), ObjectUtils.asMap(
                        "resource_type", "auto"));
            } catch (Exception e) {
                logger.error("Failed to delete file from Cloudinary: " + file.getFileUrl(), e);
            }
            uploadedFileRepository.delete(file);
        }
    }

    @Transactional
    public boolean deleteFile(String fileUrl) {

        UploadedFile uploadedFile = uploadedFileRepository.findByFileUrl(fileUrl)
                .orElseThrow(() -> new NotFoundException("File Not Found"));

        uploadedFileRepository.delete(uploadedFile);
        return true;

    }

}
