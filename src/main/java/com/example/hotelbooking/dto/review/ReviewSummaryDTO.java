package com.example.hotelbooking.dto.review;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReviewSummaryDTO {

    private Long reviewId;

    private Long bookingId;

    private Integer rating;

    private String comment;

    private LocalDateTime createdAt;

    // Thông tin người dùng
    // private String userName;
    private String userFullName;
    private String userImage;

}
