package com.example.hotelbooking.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.review.ReviewSummaryDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.exception.customer.ConflictException;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.Bookings;
import com.example.hotelbooking.model.Review;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.ReviewRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.util.ApiResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    final private ReviewRepository reviewRepository;

    final private UserAuthProviderRepository userAuthProviderRepository;

    final private BookingRepository bookingRepository;

    // Lấy danh sách review dựa trên room-type

    // Tạo mới một review
    @Transactional
    public ReviewSummaryDTO createReview(
            String providerId,
            com.example.hotelbooking.dto.review.ReviewRequestDTO reviewRequestDTO) {

        // Lấy user
        UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Lấy booking
        Bookings booking = bookingRepository.findById(reviewRequestDTO.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        // Kiểm tra booking có thuộc về user không
        if (!booking.getUser().getUserAuthProvider().getProviderUserId().equals(providerId)) {
            throw new NotFoundException("Booking not found for this user");
        }

        // Kiểm tra trạng thái booking
        if (booking.getStatus() != BookingStatusEnum.CHECKED_OUT) {
            throw new ConflictException("Cannot review a booking that is not checked out");
        }

        // Kiểm tra booking đã có review chưa
        if (reviewRepository.existsById(reviewRequestDTO.getBookingId())) {
            throw new ConflictException("Booking already has a review");
        }

        // Tạo review mới
        Review newReview = new Review();
        newReview.setRating(reviewRequestDTO.getRating());
        newReview.setComment(reviewRequestDTO.getComment());
        newReview.setUser(booking.getUser());
        newReview.setBooking(booking);
        newReview.setRoomType(booking.getRoom().getRoomType());

        Review savedReview = reviewRepository.save(newReview);

        return mapToReviewSummaryDTO(savedReview);

    }

    private ReviewSummaryDTO mapToReviewSummaryDTO(com.example.hotelbooking.model.Review review) {
        return ReviewSummaryDTO.builder()
                .reviewId(review.getReviewId())
                .bookingId(review.getBooking().getBookingId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreateAt())
                .userFullName(review.getBooking().getUser().getName())
                .userImage(review.getBooking().getUser().getAvatarUrl())
                .build();
    }

    public List<ReviewSummaryDTO> getReviewsByRoomType(Long roomType, Integer page,
            Integer size, Boolean sort) {

        Pageable pageable = PageRequest.of(page, size,
                (sort != null && sort) ? org.springframework.data.domain.Sort.by("createAt").ascending()
                        : org.springframework.data.domain.Sort.by("createAt").descending());

        Page<Review> reviewPage = reviewRepository.findByRoomType_RoomtypeId(roomType, pageable);

        List<ReviewSummaryDTO> reviewDTOs = reviewPage.stream()
                .map(this::mapToReviewSummaryDTO)
                .toList();

        return reviewDTOs;

    }

}
