package com.example.hotelbooking.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties.Apiversion.Use;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.booking.BookingDetailDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.dto.booking.BookingSummaryDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.customer.AccessDeniedException;
import com.example.hotelbooking.exception.customer.ConflictException;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.AccommodationStaff;
import com.example.hotelbooking.model.Bookings;
import com.example.hotelbooking.model.Rooms;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRespository;
import com.example.hotelbooking.repository.RoomTypeRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class BookingService {

    final BookingRepository bookingRepository;

    final RoomTypeRepository roomTypeRepository;

    final RoomRespository roomRespository;

    final UserRepository userRepository;

    final UserAuthProviderRepository userAuthProviderRepository;

    public BookingService(BookingRepository bookingRepository, RoomTypeRepository roomTypeRepository,
            RoomRespository roomRespository, UserRepository userRepository,
            UserAuthProviderRepository userAuthProviderRepository) {
        this.bookingRepository = bookingRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRespository = roomRespository;
        this.userRepository = userRepository;
        this.userAuthProviderRepository = userAuthProviderRepository;
    }

    public BookingDetailDTO createBooking(String username,
            BookingRequestDTO bookingRequestDTO) {

        // final RoomTypes roomTypes =
        // roomTypeRepository.findById(bookingRequestDTO.getRoomTypeId())
        // .orElseThrow(() -> new NotFoundException("Room type not found"));

        final List<Rooms> availableRooms = roomRespository
                .findRoomAvailableByRoomTypeId(bookingRequestDTO.getRoomTypeId(),
                        bookingRequestDTO.getCheckInDate().atTime(14, 0),
                        bookingRequestDTO.getCheckOutDate().atTime(12, 0));

        if (availableRooms.isEmpty()) {
            throw new NotFoundException("No available rooms for the selected room type");
        }

        // final Users user = userRepository.findById(Long.valueOf())
        // .orElseThrow(() -> new NotFoundException("User not found"));

        final UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(username)
                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

        final Users user = userAuthProvider.getUser();

        Bookings booking = new Bookings();

        // set room
        booking.setRoom(availableRooms.get(0));

        // set user
        booking.setUser(user);

        // set customer info
        booking.setCustomerName(bookingRequestDTO.getCustomerName());
        booking.setCustomerPhone(bookingRequestDTO.getCustomerPhone());
        booking.setCustomerEmail(bookingRequestDTO.getCustomerEmail());

        // set booking dates
        booking.setCheckInAt(bookingRequestDTO.getCheckInDate().atTime(12, 0));
        booking.setCheckOutAt(bookingRequestDTO.getCheckOutDate().atTime(14, 0));

        // set prices
        Integer numOfNights = (int) (bookingRequestDTO.getCheckOutDate().toEpochDay()
                - bookingRequestDTO.getCheckInDate().toEpochDay());
        Double originalPrice = availableRooms.get(0).getRoomType().getPrice() * numOfNights;

        booking.setOriginalPrice(originalPrice);
        Double discount = availableRooms.get(0).getRoomType().getDiscount();
        if (discount == null) {
            discount = 0.0;
        }
        booking.setDiscountedPrice(discount);
        Double finalPrice = booking.getOriginalPrice() - (booking.getOriginalPrice() * discount / 100);
        booking.setFinalPrice(finalPrice);

        // set status
        booking.setStatus(BookingStatusEnum.WAITING_FOR_PAYMENT);

        bookingRepository.save(booking);

        // update room status to DELETED (not available)
        // final Rooms room = availableRooms.get(0);
        // room.setStatus(StatusEnum.DELETED);
        // roomRespository.save(room);

        // return new BookingDetailDTO(booking);
        return mapToBookingDetailDTO(booking);
    }

    public BookingDetailDTO getBookingById(String providerId, Long bookingId) {

        UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

        UserRoleEnum userRole = userAuthProvider.getUser().getRole();

        final Bookings booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (userRole == UserRoleEnum.ROLE_HOST) {
            Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                    .map(
                            staff -> staff.getAccommodation().getAccommodationId())
                    .collect(Collectors.toSet());

            Long bookingAccommodationId = booking.getRoom().getRoomType().getAccommodation().getAccommodationId();

            if (!staffAccommodations.contains(bookingAccommodationId)) {
                throw new AccessDeniedException("Booking not found for the provider");
            }

        } else if (userRole == UserRoleEnum.ROLE_CUSTOMER) {
            Long bookingUserId = booking.getUser().getId();
            Long providerUserId = userAuthProvider.getUser().getId();

            if (!bookingUserId.equals(providerUserId)) {
                throw new AccessDeniedException("Booking not found for the customer");
            }
        }

        return mapToBookingDetailDTO(booking);
    }

    public List<BookingSummaryDTO> getBookingByAccommodationId(String providerId, Long accommodationId, int page,
            int size) {

        UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

        Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                .map(
                        staff -> staff.getAccommodation().getAccommodationId())
                .collect(Collectors.toSet());

        if (!staffAccommodations.contains(accommodationId)) {
            throw new AccessDeniedException("Accommodation not found for the provider");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Bookings> bookingsPage = bookingRepository
                .findByRoom_RoomType_Accommodation_AccommodationId(accommodationId, pageable);

        List<BookingSummaryDTO> bookingSummaryDTOs = bookingsPage.stream()
                .map(booking -> BookingSummaryDTO.builder()
                        .bookingId(booking.getBookingId())
                        .customerName(booking.getCustomerName())
                        .customerEmail(booking.getCustomerEmail())
                        .customerPhone(booking.getCustomerPhone())
                        .status(booking.getStatus().name())
                        .finalPrice(booking.getFinalPrice())
                        .build())
                .toList();

        return bookingSummaryDTOs;
    }

    private BookingDetailDTO mapToBookingDetailDTO(Bookings booking) {
        return BookingDetailDTO.builder()
                .bookingId(booking.getBookingId())
                // customer info
                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .customerEmail(booking.getCustomerEmail())
                // booking dates
                .checkInAt(booking.getCheckInAt())
                .checkOutAt(booking.getCheckOutAt())
                // prices
                .originalPrice(booking.getOriginalPrice())
                .discountedPrice(booking.getDiscountedPrice())
                .finalPrice(booking.getFinalPrice())
                // booking status
                .status(booking.getStatus().name())
                .build();
    }

    @Transactional
    public BookingDetailDTO updateBookingStatus(String providerId, Long bookingId, BookingStatusEnum status) {

        UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

        Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                .map(
                        staff -> staff.getAccommodation().getAccommodationId())
                .collect(Collectors.toSet());

        final Bookings booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        Long bookingAccommodationId = booking.getRoom().getRoomType().getAccommodation().getAccommodationId();

        if (!staffAccommodations.contains(bookingAccommodationId)) {
            throw new AccessDeniedException("Booking not found for the provider");
        }

        if (status == BookingStatusEnum.CHECKED_OUT || status == BookingStatusEnum.CANCELED) {
            throw new ConflictException("Cannot update booking status to CHECKED_OUT or CANCELED");
        }

        booking.setStatus(status);
        bookingRepository.save(booking);

        return mapToBookingDetailDTO(booking);
    }
}
