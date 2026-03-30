package com.example.hotelbooking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties.Apiversion.Use;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
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
import com.google.firebase.messaging.FirebaseMessagingException;

import jakarta.transaction.Transactional;

@Service
public class BookingService {

        final BookingRepository bookingRepository;

        final RoomTypeRepository roomTypeRepository;

        final RoomRespository roomRespository;

        final UserRepository userRepository;

        final UserAuthProviderRepository userAuthProviderRepository;

        final FcmService fcmService;

        public BookingService(BookingRepository bookingRepository, RoomTypeRepository roomTypeRepository,
                        RoomRespository roomRespository, UserRepository userRepository,
                        UserAuthProviderRepository userAuthProviderRepository, FcmService fcmService) {
                this.bookingRepository = bookingRepository;
                this.roomTypeRepository = roomTypeRepository;
                this.roomRespository = roomRespository;
                this.userRepository = userRepository;
                this.userAuthProviderRepository = userAuthProviderRepository;
                this.fcmService = fcmService;
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

                // Set expiredAt (thời gian hủy)
                booking.setExpiredAt(LocalDateTime.now().plusMinutes(3));

                bookingRepository.save(booking);

                // update room status to DELETED (not available)
                // final Rooms room = availableRooms.get(0);
                // room.setStatus(StatusEnum.DELETED);
                // roomRespository.save(room);

                // return new BookingDetailDTO(booking);
                return mapToBookingDetailDTO(booking);
        }

        @Transactional
        public BookingDetailDTO getBookingById(String providerId, Long bookingId) {

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                UserRoleEnum userRole = userAuthProvider.getUser().getRole();

                final Bookings booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new NotFoundException("Booking not found"));

                // System.err.println("Review: " + booking.getReview().getReviewId());

                if (userRole == UserRoleEnum.ROLE_HOST) {
                        Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                        .map(
                                                        staff -> staff.getAccommodation().getAccommodationId())
                                        .collect(Collectors.toSet());

                        Long bookingAccommodationId = booking.getRoom().getRoomType().getAccommodation()
                                        .getAccommodationId();

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

        @Transactional
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
                                .accommodationName(booking.getRoom().getRoomType().getAccommodation()
                                                .getAccommodationName())
                                .roomType(booking.getRoom().getRoomType().getName())
                                .roomNumber(booking.getRoom().getName())
                                .lat(booking.getRoom().getRoomType().getAccommodation().getLatitude())
                                .lng(booking.getRoom().getRoomType().getAccommodation().getLongitude())
                                .reviewId((booking.getReview() != null) ? booking.getReview().getReviewId() : null)
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

                // if (status == BookingStatusEnum.CHECKED_OUT || status ==
                // BookingStatusEnum.CANCELED) {
                // throw new ConflictException("Cannot update booking status to CHECKED_OUT or
                // CANCELED");
                // }

                booking.setStatus(status);
                bookingRepository.save(booking);

                return mapToBookingDetailDTO(booking);
        }

        public List<BookingSummaryDTO> getBookingsByCustomerAndMonth(
                        String providerId, Integer day, Integer month, Integer year, BookingStatusEnum status, int page,
                        int size) {

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                System.err.println("userAuthProvider: " + userAuthProvider.getProviderUserId());

                Pageable pageable = PageRequest.of(page, size);

                LocalDateTime start = null;
                LocalDateTime end = null;

                if (day != null && month != null && year != null) {
                        start = LocalDate.of(year, month, day).atStartOfDay();
                        end = start.plusDays(1); // nghĩa là đến hết ngày đó
                } else if (month != null && year != null) {
                        start = LocalDate.of(year, month, 1).atStartOfDay();
                        end = start.plusMonths(1); // nghĩa là đến hết tháng đó
                }

                Page<Bookings> bookingsPage = bookingRepository.findBookingsByCustomer(
                                start,
                                end,
                                status,
                                userAuthProvider.getProviderUserId(),
                                pageable);

                return bookingsPage.stream()
                                .map(booking -> BookingSummaryDTO.builder()
                                                .bookingId(booking.getBookingId())
                                                .customerName(booking.getCustomerName())
                                                .customerEmail(booking.getCustomerEmail())
                                                .customerPhone(booking.getCustomerPhone())
                                                .status(booking.getStatus().name())
                                                .finalPrice(booking.getFinalPrice())
                                                .checkInAt(booking.getCheckInAt())
                                                .checkOutAt(booking.getCheckOutAt())
                                                .build())
                                .toList();

        }

        @Transactional
        public BookingDetailDTO cancelBookingByCustomer(String providerId, Long bookingId) {

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                final Bookings booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new NotFoundException("Booking not found"));
                Long bookingUserId = booking.getUser().getId();
                Long providerUserId = userAuthProvider.getUser().getId();
                if (!bookingUserId.equals(providerUserId)) {
                        throw new AccessDeniedException("Booking not found for the customer");
                }
                if (booking.getStatus() == BookingStatusEnum.CANCELED) {
                        throw new ConflictException("Booking is already canceled");
                }
                booking.setStatus(BookingStatusEnum.CANCELED);
                bookingRepository.save(booking);

                return mapToBookingDetailDTO(booking);

        }

        // Các endpoint check booking của host

        public List<BookingSummaryDTO> getBookingsByAccommodationAndStatus(
                        String providerId, Long accommodationId, BookingStatusEnum status, int page, int size) {

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
                                .findBookingsByHost(
                                                accommodationId, status, pageable);

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

        public BookingDetailDTO updateBookingStatusByHost(
                        String providerId, Long bookingId, BookingStatusEnum status) {
                return updateBookingStatus(providerId, bookingId, status);
        }

        public Long getTodayGuests(
                        String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(
                                                staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);

                Long count = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckInAt().isBefore(todayEnd)
                                                && booking.getCheckOutAt().isAfter(todayStart)
                                                && (booking.getStatus() == BookingStatusEnum.PENDING
                                                                || booking.getStatus() == BookingStatusEnum.PENDING))
                                .count();

                return count;
        }

        public Long getTodayCheckIns(
                        String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(
                                                staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);

                Long count = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckInAt().isAfter(todayStart)
                                                && booking.getCheckInAt().isBefore(todayEnd)
                                                && (booking.getStatus() == BookingStatusEnum.CHECKED_IN))
                                .count();

                return count;
        }

        public Double getTodayRevenue(
                        String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(
                                                staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);

                Double revenue = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckOutAt().isAfter(todayStart)
                                                && booking.getCheckOutAt().isBefore(todayEnd)
                                                && booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .mapToDouble(booking -> booking.getFinalPrice())
                                .sum();

                return revenue;
        }

        public Double getMonthRevenue(
                        String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(
                                                staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                LocalDateTime monthEnd = monthStart.plusMonths(1);

                Double revenue = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckOutAt().isAfter(monthStart)
                                                && booking.getCheckOutAt().isBefore(monthEnd)
                                                && booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .mapToDouble(booking -> booking.getFinalPrice())
                                .sum();

                return revenue;
        }

        public Double getRevenueInDateRange(
                        String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(
                                                staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                Double revenue = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckOutAt().isAfter(startDateTime)
                                                && booking.getCheckOutAt().isBefore(endDateTime)
                                                && booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .mapToDouble(booking -> booking.getFinalPrice())
                                .sum();

                return revenue;
        }

        public List<Map<String, Double>> getMonthlyRevenue(
                        String providerId, Long accommodationId, int year) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(
                                                staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                List<Map<String, Double>> monthlyRevenues = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .filter(booking -> booking.getCheckOutAt().getYear() == year)
                                .collect(Collectors.groupingBy(
                                                booking -> booking.getCheckOutAt().getMonthValue(),
                                                Collectors.summingDouble(booking -> booking.getFinalPrice())))
                                .entrySet().stream()
                                .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                                .map(entry -> Map.of("month", entry.getKey().doubleValue(), "revenue",
                                                entry.getValue()))
                                .collect(Collectors.toList());

                return monthlyRevenues;

        }

        public List<Map<String, Double>> getYearlyRevenue(
                        String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(
                                                staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                List<Map<String, Double>> yearlyRevenues = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .collect(Collectors.groupingBy(
                                                booking -> booking.getCheckOutAt().getYear(),
                                                Collectors.summingDouble(booking -> booking.getFinalPrice())))
                                .entrySet().stream()
                                .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                                .map(entry -> Map.of("year", entry.getKey().doubleValue(), "revenue", entry.getValue()))
                                .collect(Collectors.toList());

                return yearlyRevenues;

        }

        public Map<String, Object> getBookingStatistics(
                        String providerId, Long accommodationId,
                        LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(
                                                staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                Map<String, Object> statistics = bookingRepository.fetchBookingStatistics(
                                accommodationId, startDateTime, endDateTime);

                return statistics;

        }

        public List<Map<String, Object>> getRevenueByRoomType(
                        String providerId, Long accommodationId,
                        LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(
                                                staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                List<Map<String, Object>> trends = bookingRepository.fetchBookingTrends(
                                accommodationId, startDateTime, endDateTime);

                return trends;

        }

        public Double getTotalRevenueInDateRange(
                        String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                Double totalRevenue = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckOutAt().isAfter(startDateTime)
                                                && booking.getCheckOutAt().isBefore(endDateTime)
                                                && booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .mapToDouble(booking -> booking.getFinalPrice())
                                .sum();

                return totalRevenue;
        }

        public Long getTotalBookingsInDateRange(
                        String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                Long totalBookings = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckInAt().isAfter(startDateTime)
                                                && booking.getCheckInAt().isBefore(endDateTime))
                                .count();

                return totalBookings;
        }

        public Long getTotalCanceledBookingsInDateRange(
                        String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                Long totalCanceled = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckInAt().isAfter(startDateTime)
                                                && booking.getCheckInAt().isBefore(endDateTime)
                                                && booking.getStatus() == BookingStatusEnum.CANCELED)
                                .count();

                return totalCanceled;
        }

        public Long getTotalNightsInDateRange(
                        String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                Long totalNights = bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckInAt().isAfter(startDateTime)
                                                && booking.getCheckOutAt().isBefore(endDateTime)
                                                && booking.getStatus() != BookingStatusEnum.CANCELED)
                                .mapToLong(booking -> {
                                        long nights = java.time.temporal.ChronoUnit.DAYS.between(
                                                        booking.getCheckInAt().toLocalDate(),
                                                        booking.getCheckOutAt().toLocalDate());
                                        return nights > 0 ? nights : 0;
                                })
                                .sum();

                return totalNights;
        }

        @Scheduled(fixedDelay = 60000)
        public void expirePendingBookings() {
                List<Bookings> expiredBookings = bookingRepository
                                .findByStatusAndExpiredAtBefore(BookingStatusEnum.WAITING_FOR_PAYMENT,
                                                LocalDateTime.now());

                for (Bookings booking : expiredBookings) {
                        booking.setStatus(BookingStatusEnum.CANCELED);
                        // bookingRepository.save(booking);
                }

                bookingRepository.saveAll(expiredBookings);
        }

        @Scheduled(cron = "0 0 8 * * ?") // Chạy vào lúc 8 giờ sáng hàng ngày
        @Transactional
        public void notificationForTodayCheckIns() throws FirebaseMessagingException {
                LocalDateTime todayStart = LocalDate.now().atStartOfDay();

                // Lấy danh sách booking có check-in trong ngày hôm nay
                List<Bookings> todayCheckIns = bookingRepository.findByStatusAndCheckInAtBefore(
                                BookingStatusEnum.PENDING, todayStart);

                for (Bookings booking : todayCheckIns) {
                        // Gửi thông báo cho khách hàng
                        if (booking.getUser().getDevices() != null && !booking.getUser().getDevices().isEmpty()) {
                                fcmService.sendNotification(
                                                "Reminder: Upcoming Check-in Today",
                                                "Dear " + booking.getCustomerName() + ", your check-in for booking ID "
                                                                + booking.getBookingId()
                                                                + " is scheduled for today. Please be prepared!",
                                                booking.getUser().getDevices().getLast().getFcmToken());

                        }
                }
        }

}
