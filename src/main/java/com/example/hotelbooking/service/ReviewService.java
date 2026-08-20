package com.example.hotelbooking.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.review.ReviewRequestDTO;
import com.example.hotelbooking.dto.review.ReviewSummaryDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.Review;
import com.example.hotelbooking.model.RoomType;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.ReviewRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public ReviewSummaryDTO createReview(String providerId, ReviewRequestDTO reviewRequestDTO) {

        UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Booking booking = bookingRepository.findById(reviewRequestDTO.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (!booking.getUser().getUserAuthProvider().getProviderUserId().equals(providerId)) {
            throw new NotFoundException("Booking not found for this user");
        }

        if (booking.getStatus() != BookingStatusEnum.CHECKED_OUT) {
            throw new ConflictException("Cannot review a booking that is not checked out");
        }

        if (reviewRepository.existsById(reviewRequestDTO.getBookingId())) {
            throw new ConflictException("Booking already has a review");
        }

        Review newReview = new Review();
        newReview.setRating(reviewRequestDTO.getRating());
        newReview.setComment(reviewRequestDTO.getComment());
        newReview.setUser(booking.getUser());
        newReview.setBooking(booking);
        newReview.setRoomType(booking.getRoom().getRoomType());

        Review savedReview = reviewRepository.save(newReview);

        booking.setReview(savedReview);
        bookingRepository.save(booking);

        RoomType roomType = booking.getRoom().getRoomType();
        int currentAvgRating = roomType.getStar() != null ? roomType.getStar() : 0;
        int currentReviewCount = roomType.getReviews() != null ? roomType.getReviews().size() : 1;

        double newAvgRating = (currentAvgRating * (currentReviewCount - 1) + savedReview.getRating())
                / (currentReviewCount == 0 ? 1 : currentReviewCount);
                
        roomType.setStar((int) Math.round(newAvgRating));

        return mapToReviewSummaryDTO(savedReview);
    }

    private ReviewSummaryDTO mapToReviewSummaryDTO(Review review) {
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

    public List<ReviewSummaryDTO> getReviewsByRoomType(Long roomType, Integer page, Integer size, Boolean sort) {
        Pageable pageable = PageRequest.of(page, size,
                (sort != null && sort) ? Sort.by("createAt").ascending()
                        : Sort.by("createAt").descending());

        Page<Review> reviewPage = reviewRepository.findByRoomType_RoomtypeId(roomType, pageable);

        return reviewPage.stream()
                .map(this::mapToReviewSummaryDTO)
                .toList();
    }
}
