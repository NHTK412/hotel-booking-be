package com.example.hotelbooking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.review.ReviewRequestDTO;
import com.example.hotelbooking.dto.review.ReviewSummaryDTO;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.ReviewService;
import com.example.hotelbooking.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "5. Đánh Giá & Nhận Xét (Reviews)", description = "Các API xem đánh giá loại phòng và khách hàng gửi đánh giá điểm sao (1 - 5 sao) sau khi trả phòng")
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Lấy danh sách đánh giá của loại phòng (Công khai)", description = "Truy xuất danh sách đánh giá, số sao và bình luận của loại phòng có phân trang và sắp xếp")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewSummaryDTO>>> getReviewsByRoomType(
            @RequestParam Long roomType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) Boolean sort) {
        List<ReviewSummaryDTO> reviews = reviewService.getReviewsByRoomType(roomType, page, size, sort);
        ApiResponse<List<ReviewSummaryDTO>> response = new ApiResponse<>(true, "Lấy danh sách đánh giá thành công", reviews);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Gửi đánh giá cho đơn đặt phòng (Khách hàng)", description = "Khách hàng gửi điểm đánh giá (1-5 sao) và nhận xét cho đơn đặt phòng đã hoàn tất (CHECKED_OUT)")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewSummaryDTO>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewRequestDTO reviewRequestDTO) {

        String providerId = userDetails.getUsername();
        ReviewSummaryDTO createdReview = reviewService.createReview(providerId, reviewRequestDTO);

        ApiResponse<ReviewSummaryDTO> response = new ApiResponse<>(true, "Gửi đánh giá thành công", createdReview);
        return ResponseEntity.ok(response);
    }
}
