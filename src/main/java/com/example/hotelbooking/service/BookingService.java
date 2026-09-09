package com.example.hotelbooking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.AccessDeniedException;
import com.example.hotelbooking.exception.BadRequestException;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.AccommodationStaff;
import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.RoomType;
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
@Transactional
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

        private Set<Long> getHostAccommodationIds(UserAuthProvider userAuthProvider) {
                if (userAuthProvider == null || userAuthProvider.getUser() == null || userAuthProvider.getUser().getAccommodationStaffs() == null) {
                        return Collections.emptySet();
                }
                return userAuthProvider.getUser().getAccommodationStaffs().stream()
                                .filter(staff -> !Boolean.TRUE.equals(staff.getIsDeleted()))
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .collect(Collectors.toSet());
        }

        @Transactional
        public BookingDetailDTO createBooking(String username, BookingRequestDTO bookingRequestDTO) {

                if (bookingRequestDTO.getCheckInDate() == null || bookingRequestDTO.getCheckOutDate() == null) {
                        throw new BadRequestException("Check-in and check-out dates are required");
                }
                if (bookingRequestDTO.getCheckInDate().isBefore(LocalDate.now())) {
                        throw new BadRequestException("Check-in date cannot be in the past");
                }
                if (!bookingRequestDTO.getCheckOutDate().isAfter(bookingRequestDTO.getCheckInDate())) {
                        throw new BadRequestException("Check-out date must be after check-in date");
                }

                final List<Room> availableRooms = roomRepository
                                .findRoomAvailableByRoomTypeId(bookingRequestDTO.getRoomTypeId(),
                                                bookingRequestDTO.getCheckInDate().atTime(14, 0),
                                                bookingRequestDTO.getCheckOutDate().atTime(12, 0));

                if (availableRooms.isEmpty()) {
                        roomTypeRepository.findById(bookingRequestDTO.getRoomTypeId()).ifPresent(rt -> {
                                if (Boolean.TRUE.equals(rt.getIsDeleted())) {
                                        throw new NotFoundException("Loại phòng này không tồn tại hoặc đã bị xóa.");
                                }
                                if (rt.getStatus() == StatusEnum.INACTIVE) {
                                        throw new BadRequestException("Loại phòng này hiện đang tạm ngưng nhận đặt (INACTIVE), quý khách chỉ có thể xem thông tin.");
                                }
                        });
                        throw new NotFoundException("No available rooms for the selected room type and dates");
                }

                // Cưỡng chế kiểm tra & tăng version của phòng bằng Optimistic Lock
                final Room lockedRoom = roomRepository.findByIdWithOptimisticLock(availableRooms.get(0).getRoomId())
                                .orElseThrow(() -> new NotFoundException("Selected room is no longer available"));

                if (lockedRoom.getRoomType() != null) {
                        if (Boolean.TRUE.equals(lockedRoom.getRoomType().getIsDeleted())) {
                                throw new NotFoundException("Loại phòng này không tồn tại hoặc đã bị xóa.");
                        }
                        if (lockedRoom.getRoomType().getStatus() == StatusEnum.INACTIVE) {
                                throw new BadRequestException("Loại phòng này hiện đang tạm ngưng nhận đặt (INACTIVE), quý khách chỉ có thể xem thông tin.");
                        }
                }

                final UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(username)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                final User user = userAuthProvider.getUser();

                Booking booking = new Booking();
                booking.setRoom(lockedRoom);
                booking.setUser(user);
                booking.setCustomerName(bookingRequestDTO.getCustomerName());
                booking.setCustomerPhone(bookingRequestDTO.getCustomerPhone());
                booking.setCustomerEmail(bookingRequestDTO.getCustomerEmail());
                booking.setCheckInAt(bookingRequestDTO.getCheckInDate().atTime(12, 0));
                booking.setCheckOutAt(bookingRequestDTO.getCheckOutDate().atTime(14, 0));

                Integer numOfNights = (int) (bookingRequestDTO.getCheckOutDate().toEpochDay()
                                - bookingRequestDTO.getCheckInDate().toEpochDay());
                Double originalPrice = lockedRoom.getRoomType().getPrice() * numOfNights;

                booking.setOriginalPrice(originalPrice);
                Double discount = lockedRoom.getRoomType().getDiscount();
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
                        Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

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

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

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

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

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

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                Pageable pageable = PageRequest.of(page, size);

                Page<Booking> bookingsPage = bookingRepository
                                .findBookingsByHost(accommodationId, status, pageable);

                return bookingsPage.stream()
                                .map(this::mapToBookingSummaryDTO)
                                .toList();
        }

        public List<BookingSummaryDTO> getBookingsForHost(
                        String providerId, Long accommodationId, BookingStatusEnum status, int page, int size) {

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                List<AccommodationStaff> staffs = userAuthProvider.getUser().getAccommodationStaffs();
                if (staffs == null || staffs.isEmpty()) {
                        return List.of();
                }

                List<Long> managedAccommodationIds = staffs.stream()
                                .filter(staff -> !Boolean.TRUE.equals(staff.getIsDeleted()))
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .distinct()
                                .toList();

                Pageable pageable = PageRequest.of(page, size);
                Page<Booking> bookingsPage;

                if (accommodationId != null) {
                        if (!managedAccommodationIds.contains(accommodationId)) {
                                throw new AccessDeniedException("Accommodation not found for the provider");
                        }
                        bookingsPage = bookingRepository.findBookingsByHost(accommodationId, status, pageable);
                } else {
                        bookingsPage = bookingRepository.findBookingsByHostMultiple(managedAccommodationIds, status, pageable);
                }

                return bookingsPage.stream()
                                .map(this::mapToBookingSummaryDTO)
                                .toList();
        }

        private BookingSummaryDTO mapToBookingSummaryDTO(Booking booking) {
                Long accommodationId = null;
                String accommodationName = null;
                String roomTypeName = null;
                String roomNumber = null;

                if (booking.getRoom() != null) {
                        roomNumber = booking.getRoom().getName();
                        if (booking.getRoom().getRoomType() != null) {
                                roomTypeName = booking.getRoom().getRoomType().getName();
                                if (booking.getRoom().getRoomType().getAccommodation() != null) {
                                        accommodationId = booking.getRoom().getRoomType().getAccommodation().getAccommodationId();
                                        accommodationName = booking.getRoom().getRoomType().getAccommodation().getAccommodationName();
                                }
                        }
                }

                return BookingSummaryDTO.builder()
                                .bookingId(booking.getBookingId())
                                .customerName(booking.getCustomerName())
                                .customerEmail(booking.getCustomerEmail())
                                .customerPhone(booking.getCustomerPhone())
                                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                                .finalPrice(booking.getFinalPrice())
                                .checkInAt(booking.getCheckInAt())
                                .checkOutAt(booking.getCheckOutAt())
                                .accommodationId(accommodationId)
                                .accommodationName(accommodationName)
                                .roomTypeName(roomTypeName)
                                .roomNumber(roomNumber)
                                .build();
        }

        public BookingDetailDTO updateBookingStatusByHost(
                        String providerId, Long bookingId, BookingStatusEnum status) {
                return updateBookingStatus(providerId, bookingId, status);
        }

        public Long getTodayGuests(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);

                return bookingRepository.countGuestsInPeriod(
                                accommodationId,
                                BookingStatusEnum.PENDING,
                                todayStart,
                                todayEnd);
        }

        public Long getTodayCheckIns(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);

                return bookingRepository.countCheckInsBetween(
                                accommodationId,
                                BookingStatusEnum.CHECKED_IN,
                                todayStart,
                                todayEnd);
        }

        public Double getTodayRevenue(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);

                return bookingRepository.calculateRevenueBetween(
                                accommodationId,
                                BookingStatusEnum.CHECKED_OUT,
                                todayStart,
                                todayEnd);
        }

        public Double getMonthRevenue(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                LocalDateTime monthEnd = monthStart.plusMonths(1);

                return bookingRepository.calculateRevenueBetween(
                                accommodationId,
                                BookingStatusEnum.CHECKED_OUT,
                                monthStart,
                                monthEnd);
        }

        public Double getRevenueInDateRange(String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                return bookingRepository.calculateRevenueBetween(
                                accommodationId,
                                BookingStatusEnum.CHECKED_OUT,
                                startDateTime,
                                endDateTime);
        }

        public List<Map<String, Double>> getMonthlyRevenue(String providerId, Long accommodationId, int year) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                List<Map<String, Object>> rawList = bookingRepository.fetchMonthlyRevenue(accommodationId, year);
                return rawList.stream()
                                .map(entry -> Map.of(
                                                "month", ((Number) entry.get("month")).doubleValue(),
                                                "revenue", ((Number) entry.get("revenue")).doubleValue()))
                                .collect(Collectors.toList());
        }

        public List<Map<String, Double>> getYearlyRevenue(String providerId, Long accommodationId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                List<Map<String, Object>> rawList = bookingRepository.fetchYearlyRevenue(accommodationId);
                return rawList.stream()
                                .map(entry -> Map.of(
                                                "year", ((Number) entry.get("year")).doubleValue(),
                                                "revenue", ((Number) entry.get("revenue")).doubleValue()))
                                .collect(Collectors.toList());
        }

        public Map<String, Object> getBookingStatistics(String providerId, Long accommodationId,
                        LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

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

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

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

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                return bookingRepository.countBookingsByCheckInBetween(accommodationId, startDateTime, endDateTime);
        }

        public Long getTotalCanceledBookingsInDateRange(String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                return bookingRepository.countCanceledBookingsBetween(accommodationId, startDateTime, endDateTime);
        }

        public Long getTotalNightsInDateRange(String providerId, Long accommodationId, LocalDate startDate, LocalDate endDate) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User auth provider not found"));

                Set<Long> staffAccommodations = getHostAccommodationIds(userAuthProvider);

                if (!staffAccommodations.contains(accommodationId)) {
                        throw new AccessDeniedException("Accommodation not found for the provider");
                }

                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                return bookingRepository.calculateTotalNightsBetween(accommodationId, startDateTime, endDateTime);
        }

        @Scheduled(fixedDelay = 60000)
        @Transactional
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
