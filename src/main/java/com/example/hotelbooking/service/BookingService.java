package com.example.hotelbooking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.booking.BookingDetailDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.dto.booking.BookingSummaryDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.AccessDeniedException;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRepository;
import com.example.hotelbooking.repository.RoomTypeRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessagingException;

import jakarta.transaction.Transactional;

@Service
public class BookingService {

        private final BookingRepository bookingRepository;
        private final RoomTypeRepository roomTypeRepository;
        private final RoomRepository roomRepository;
        private final UserRepository userRepository;
        private final UserAuthProviderRepository userAuthProviderRepository;
        private final FcmService fcmService;

        public BookingService(BookingRepository bookingRepository, RoomTypeRepository roomTypeRepository,
                        RoomRepository roomRepository, UserRepository userRepository,
                        UserAuthProviderRepository userAuthProviderRepository, FcmService fcmService) {
                this.bookingRepository = bookingRepository;
                this.roomTypeRepository = roomTypeRepository;
                this.roomRepository = roomRepository;
                this.userRepository = userRepository;
                this.userAuthProviderRepository = userAuthProviderRepository;
                this.fcmService = fcmService;
        }

        public BookingDetailDTO createBooking(String username, BookingRequestDTO bookingRequestDTO) {

                final List<Room> availableRooms = roomRepository
                                .findRoomAvailableByRoomTypeId(bookingRequestDTO.getRoomTypeId(),
                                                bookingRequestDTO.getCheckInDate().atTime(14, 0),
                                                bookingRequestDTO.getCheckOutDate().atTime(12, 0));

                if (availableRooms.isEmpty()) {
                        throw new NotFoundException("No available rooms for the selected room type");
                }

                final UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(username)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                final User user = userAuthProvider.getUser();

                Booking booking = new Booking();
                booking.setRoom(availableRooms.get(0));
                booking.setUser(user);
                booking.setCustomerName(bookingRequestDTO.getCustomerName());
                booking.setCustomerPhone(bookingRequestDTO.getCustomerPhone());
                booking.setCustomerEmail(bookingRequestDTO.getCustomerEmail());
                booking.setCheckInAt(bookingRequestDTO.getCheckInDate().atTime(12, 0));
                booking.setCheckOutAt(bookingRequestDTO.getCheckOutDate().atTime(14, 0));

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
                booking.setStatus(BookingStatusEnum.WAITING_FOR_PAYMENT);
                booking.setExpiredAt(LocalDateTime.now().plusMinutes(3));

                bookingRepository.save(booking);

                return mapToBookingDetailDTO(booking);
        }

        @Transactional
        public BookingDetailDTO getBookingById(String providerId, Long bookingId) {

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                UserRoleEnum userRole = userAuthProvider.getUser().getRole();

                final Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new NotFoundException("Booking not found"));

                if (userRole == UserRoleEnum.ROLE_HOST) {
                        Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                        .map(staff -> staff.getAccommodation().getAccommodationId())
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
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                Pageable pageable = PageRequest.of(page, size);

                Page<Booking> bookingsPage = bookingRepository
                                .findByRoom_RoomType_Accommodation_AccommodationId(accommodationId, pageable);

                return bookingsPage.stream()
                                .map(booking -> BookingSummaryDTO.builder()
                                                .bookingId(booking.getBookingId())
                                                .customerName(booking.getCustomerName())
                                                .customerEmail(booking.getCustomerEmail())
                                                .customerPhone(booking.getCustomerPhone())
                                                .status(booking.getStatus().name())
                                                .finalPrice(booking.getFinalPrice())
                                                .build())
                                .toList();
        }

        private BookingDetailDTO mapToBookingDetailDTO(Booking booking) {
                return BookingDetailDTO.builder()
                                .bookingId(booking.getBookingId())
                                .customerName(booking.getCustomerName())
                                .customerPhone(booking.getCustomerPhone())
                                .customerEmail(booking.getCustomerEmail())
                                .checkInAt(booking.getCheckInAt())
                                .checkOutAt(booking.getCheckOutAt())
                                .originalPrice(booking.getOriginalPrice())
                                .discountedPrice(booking.getDiscountedPrice())
                                .finalPrice(booking.getFinalPrice())
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
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                final Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new NotFoundException("Booking not found"));

                Long bookingAccommodationId = booking.getRoom().getRoomType().getAccommodation().getAccommodationId();

                if (!staffAccommodations.contains(bookingAccommodationId)) {
                        throw new AccessDeniedException("Booking not found for the provider");
                }

                booking.setStatus(status);
                bookingRepository.save(booking);

                return mapToBookingDetailDTO(booking);
        }

        public List<BookingSummaryDTO> getBookingsByCustomerAndMonth(
                        String providerId, Integer day, Integer month, Integer year, BookingStatusEnum status, int page,
                        int size) {

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Pageable pageable = PageRequest.of(page, size);

                LocalDateTime start = null;
                LocalDateTime end = null;

                if (day != null && month != null && year != null) {
                        start = LocalDate.of(year, month, day).atStartOfDay();
                        end = start.plusDays(1);
                } else if (month != null && year != null) {
                        start = LocalDate.of(year, month, 1).atStartOfDay();
                        end = start.plusMonths(1);
                }

                Page<Booking> bookingsPage = bookingRepository.findBookingsByCustomer(
                                start,
                                end,
                                status,
                                userAuthProvider.getUser().getId(),
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

                final Booking booking = bookingRepository.findById(bookingId)
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

        public List<BookingSummaryDTO> getBookingsByAccommodationAndStatus(
                        String providerId, Long accommodationId, BookingStatusEnum status, int page, int size) {

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                Pageable pageable = PageRequest.of(page, size);

                Page<Booking> bookingsPage = bookingRepository
                                .findBookingsByHost(accommodationId, status, pageable);

                return bookingsPage.stream()
                                .map(booking -> BookingSummaryDTO.builder()
                                                .bookingId(booking.getBookingId())
                                                .customerName(booking.getCustomerName())
                                                .customerEmail(booking.getCustomerEmail())
                                                .customerPhone(booking.getCustomerPhone())
                                                .status(booking.getStatus().name())
                                                .finalPrice(booking.getFinalPrice())
                                                .build())
                                .toList();
        }

        public BookingDetailDTO updateBookingStatusByHost(
                        String providerId, Long bookingId, BookingStatusEnum status) {
                return updateBookingStatus(providerId, bookingId, status);
        }

        public Long getTodayGuests(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);

                return bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckInAt().isBefore(todayEnd)
                                                && booking.getCheckOutAt().isAfter(todayStart)
                                                && (booking.getStatus() == BookingStatusEnum.PENDING))
                                .count();
        }

        public Long getTodayCheckIns(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);

                return bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckInAt().isAfter(todayStart)
                                                && booking.getCheckInAt().isBefore(todayEnd)
                                                && (booking.getStatus() == BookingStatusEnum.CHECKED_IN))
                                .count();
        }

        public Double getTodayRevenue(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);

                return bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckOutAt().isAfter(todayStart)
                                                && booking.getCheckOutAt().isBefore(todayEnd)
                                                && booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .mapToDouble(Booking::getFinalPrice)
                                .sum();
        }

        public Double getMonthRevenue(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                LocalDateTime monthEnd = monthStart.plusMonths(1);

                return bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckOutAt().isAfter(monthStart)
                                                && booking.getCheckOutAt().isBefore(monthEnd)
                                                && booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .mapToDouble(Booking::getFinalPrice)
                                .sum();
        }

        public Double getRevenueInDateRange(String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
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

                return bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckOutAt().isAfter(startDateTime)
                                                && booking.getCheckOutAt().isBefore(endDateTime)
                                                && booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .mapToDouble(Booking::getFinalPrice)
                                .sum();
        }

        public List<Map<String, Double>> getMonthlyRevenue(String providerId, Long accommodationId, int year) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                return bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .filter(booking -> booking.getCheckOutAt().getYear() == year)
                                .collect(Collectors.groupingBy(
                                                booking -> booking.getCheckOutAt().getMonthValue(),
                                                Collectors.summingDouble(Booking::getFinalPrice)))
                                .entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entry -> Map.of("month", entry.getKey().doubleValue(), "revenue", entry.getValue()))
                                .collect(Collectors.toList());
        }

        public List<Map<String, Double>> getYearlyRevenue(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                return bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getStatus() == BookingStatusEnum.CHECKED_OUT)
                                .collect(Collectors.groupingBy(
                                                booking -> booking.getCheckOutAt().getYear(),
                                                Collectors.summingDouble(Booking::getFinalPrice)))
                                .entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entry -> Map.of("year", entry.getKey().doubleValue(), "revenue", entry.getValue()))
                                .collect(Collectors.toList());
        }

        public Map<String, Object> getBookingStatistics(String providerId, Long accommodationId,
                        LocalDate startDate, LocalDate endDate) {
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

                return bookingRepository.fetchBookingStatistics(accommodationId, startDateTime, endDateTime);
        }

        public List<Map<String, Object>> getRevenueByRoomType(String providerId, Long accommodationId,
                        LocalDate startDate, LocalDate endDate) {
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

                return bookingRepository.fetchBookingTrends(accommodationId, startDateTime, endDateTime);
        }

        public Double getTotalRevenueInDateRange(String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
                return getRevenueInDateRange(providerId, accommodationId, startDate, endDate);
        }

        public Long getTotalBookingsInDateRange(String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
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

                return bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckInAt().isAfter(startDateTime)
                                                && booking.getCheckInAt().isBefore(endDateTime))
                                .count();
        }

        public Long getTotalCanceledBookingsInDateRange(String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
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

                return bookingRepository.findBookingsByHost(
                                accommodationId,
                                null,
                                Pageable.unpaged()).stream()
                                .filter(booking -> booking.getCheckInAt().isAfter(startDateTime)
                                                && booking.getCheckInAt().isBefore(endDateTime)
                                                && booking.getStatus() == BookingStatusEnum.CANCELED)
                                .count();
        }

        public Long getTotalNightsInDateRange(String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
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

                return bookingRepository.findBookingsByHost(
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
        }

        @Scheduled(fixedDelay = 60000)
        public void expirePendingBookings() {
                List<Booking> expiredBookings = bookingRepository
                                .findByStatusAndExpiredAtBefore(BookingStatusEnum.WAITING_FOR_PAYMENT,
                                                LocalDateTime.now());

                for (Booking booking : expiredBookings) {
                        booking.setStatus(BookingStatusEnum.CANCELED);
                }

                bookingRepository.saveAll(expiredBookings);
        }

        @Scheduled(cron = "0 0 8 * * ?")
        @Transactional
        public void notificationForTodayCheckIns() throws FirebaseMessagingException {
                LocalDateTime todayStart = LocalDate.now().atStartOfDay();

                List<Booking> todayCheckIns = bookingRepository.findByStatusAndCheckInAtBefore(
                                BookingStatusEnum.PENDING, todayStart);

                for (Booking booking : todayCheckIns) {
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
