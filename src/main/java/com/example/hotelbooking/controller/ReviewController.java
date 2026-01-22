package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.review.ReviewSummaryDTO;
import com.example.hotelbooking.security.CustomerUserDetails;
import com.example.hotelbooking.service.ReviewService;
import com.example.hotelbooking.util.ApiResponse;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    final private ReviewService reviewService;

    // Lấy danh sách review dựa trên room-type

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewSummaryDTO>>> getReviewsByRoomType(@RequestParam Long roomType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) Boolean sort) {
        List<ReviewSummaryDTO> reviews = reviewService.getReviewsByRoomType(roomType, page, size, sort);
        ApiResponse<List<ReviewSummaryDTO>> response = new ApiResponse<>(true, "Reviews fetched successfully", reviews);
        return ResponseEntity.ok(response);
    }

    // Tạo mới một review

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewSummaryDTO>> createReview(
            @AuthenticationPrincipal CustomerUserDetails userDetails,

            @RequestBody com.example.hotelbooking.dto.review.ReviewRequestDTO reviewRequestDTO) {

        String providerId = userDetails.getUsername();

        ReviewSummaryDTO createdReview = reviewService.createReview(providerId, reviewRequestDTO);

        ApiResponse<ReviewSummaryDTO> response = new ApiResponse<>(true, "Review created successfully", createdReview);
        return ResponseEntity.ok(response);
    }

}
