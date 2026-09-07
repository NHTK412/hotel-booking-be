package com.example.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.hotelbooking.dto.booking.BookingDetailDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.AccommodationTypeEnum;
import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.AccessDeniedException;
import com.example.hotelbooking.exception.BadRequestException;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Accommodation;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for BookingService")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private BookingService bookingService;

    private User customerUser;
    private UserAuthProvider customerAuthProvider;
    private User hostUser;
    private UserAuthProvider hostAuthProvider;
    private Accommodation mockAccommodation;
    private RoomType mockRoomType;
    private Room mockRoom;
    private Booking mockBooking;

    @BeforeEach
    void setUp() {
        customerUser = new User();
        customerUser.setId(1L);
        customerUser.setName("Customer A");
        customerUser.setEmail("customer@gmail.com");
        customerUser.setRole(UserRoleEnum.ROLE_CUSTOMER);
        customerUser.setIsActive(true);

        customerAuthProvider = new UserAuthProvider();
        customerAuthProvider.setId(101L);
        customerAuthProvider.setType(AuthProviderTypeEnum.LOCAL);
        customerAuthProvider.setProviderUserId("customer@gmail.com");
        customerAuthProvider.setUser(customerUser);

        mockAccommodation = new Accommodation();
        mockAccommodation.setAccommodationId(10L);
        mockAccommodation.setAccommodationName("Luxury Beach Resort");
        mockAccommodation.setAddress("123 Vo Nguyen Giap, Da Nang");
        mockAccommodation.setType(AccommodationTypeEnum.RESORT);
        mockAccommodation.setLatitude(16.0544);
        mockAccommodation.setLongitude(108.2022);

        mockRoomType = new RoomType();
        mockRoomType.setRoomtypeId(5L);
        mockRoomType.setName("Deluxe Ocean View");
        mockRoomType.setPrice(1000000.0);
        mockRoomType.setDiscount(10.0); // 10% discount
        mockRoomType.setAccommodation(mockAccommodation);

        mockRoom = new Room();
        mockRoom.setRoomId(501L);
        mockRoom.setName("Room 301");
        mockRoom.setRoomType(mockRoomType);
        mockRoom.setVersion(0L);

        mockBooking = new Booking();
        mockBooking.setBookingId(1001L);
        mockBooking.setUser(customerUser);
        mockBooking.setRoom(mockRoom);
        mockBooking.setCustomerName("Customer A");
        mockBooking.setCustomerPhone("0901234567");
        mockBooking.setCustomerEmail("customer@gmail.com");
        mockBooking.setOriginalPrice(2000000.0);
        mockBooking.setDiscountedPrice(10.0);
        mockBooking.setFinalPrice(1800000.0);
        mockBooking.setStatus(BookingStatusEnum.WAITING_FOR_PAYMENT);

        AccommodationStaff staff = new AccommodationStaff();
        staff.setAccommodation(mockAccommodation);
        staff.setUser(hostUser);
        staff.setRole(AccommodationStaffRoleEnum.ROLE_MANAGER);

        hostUser = new User();
        hostUser.setId(2L);
        hostUser.setName("Host B");
        hostUser.setEmail("host@hotel.com");
        hostUser.setRole(UserRoleEnum.ROLE_HOST);
        hostUser.setAccommodationStaffs(List.of(staff));

        hostAuthProvider = new UserAuthProvider();
        hostAuthProvider.setId(102L);
        hostAuthProvider.setType(AuthProviderTypeEnum.LOCAL);
        hostAuthProvider.setProviderUserId("host@hotel.com");
        hostAuthProvider.setUser(hostUser);
    }

    @Nested
    @DisplayName("Create Booking Tests")
    class CreateBookingTests {

        @Test
        @DisplayName("Đặt phòng thành công: Tính đúng giá gốc, chiết khấu, giá cuối và lưu trạng thái WAITING_FOR_PAYMENT")
        void testCreateBooking_Success() {
            BookingRequestDTO requestDTO = BookingRequestDTO.builder()
                    .roomTypeId(5L)
                    .customerName("Customer A")
                    .customerPhone("0901234567")
                    .customerEmail("customer@gmail.com")
                    .checkInDate(LocalDate.now().plusDays(2))
                    .checkOutDate(LocalDate.now().plusDays(4)) // 2 nights
                    .build();

            when(roomRepository.findRoomAvailableByRoomTypeId(any(), any(), any()))
                    .thenReturn(List.of(mockRoom));
            when(roomRepository.findByIdWithOptimisticLock(501L))
                    .thenReturn(Optional.of(mockRoom));
            when(userAuthProviderRepository.findByProviderUserId("customer@gmail.com"))
                    .thenReturn(Optional.of(customerAuthProvider));

            BookingDetailDTO result = bookingService.createBooking("customer@gmail.com", requestDTO);

            assertNotNull(result);
            assertEquals("Customer A", result.getCustomerName());
            assertEquals(2000000.0, result.getOriginalPrice()); // 1,000,000 * 2
            assertEquals(1800000.0, result.getFinalPrice()); // 2,000,000 - 10%
            assertEquals(BookingStatusEnum.WAITING_FOR_PAYMENT.name(), result.getStatus());

            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Đặt phòng thất bại khi ngày Check-in nằm trong quá khứ")
        void testCreateBooking_PastCheckInDate_ThrowsBadRequestException() {
            BookingRequestDTO requestDTO = BookingRequestDTO.builder()
                    .roomTypeId(5L)
                    .customerName("Customer A")
                    .customerPhone("0901234567")
                    .customerEmail("customer@gmail.com")
                    .checkInDate(LocalDate.now().minusDays(1))
                    .checkOutDate(LocalDate.now().plusDays(2))
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () -> {
                bookingService.createBooking("customer@gmail.com", requestDTO);
            });

            assertEquals("Check-in date cannot be in the past", ex.getMessage());
        }

        @Test
        @DisplayName("Đặt phòng thất bại khi ngày Check-out trước hoặc bằng ngày Check-in")
        void testCreateBooking_CheckOutBeforeCheckIn_ThrowsBadRequestException() {
            BookingRequestDTO requestDTO = BookingRequestDTO.builder()
                    .roomTypeId(5L)
                    .customerName("Customer A")
                    .customerPhone("0901234567")
                    .customerEmail("customer@gmail.com")
                    .checkInDate(LocalDate.now().plusDays(3))
                    .checkOutDate(LocalDate.now().plusDays(2))
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () -> {
                bookingService.createBooking("customer@gmail.com", requestDTO);
            });

            assertEquals("Check-out date must be after check-in date", ex.getMessage());
        }

        @Test
        @DisplayName("Đặt phòng thất bại khi hết phòng trống cho loại phòng và khung thời gian đã chọn")
        void testCreateBooking_NoRoomsAvailable_ThrowsNotFoundException() {
            BookingRequestDTO requestDTO = BookingRequestDTO.builder()
                    .roomTypeId(5L)
                    .customerName("Customer A")
                    .customerPhone("0901234567")
                    .customerEmail("customer@gmail.com")
                    .checkInDate(LocalDate.now().plusDays(2))
                    .checkOutDate(LocalDate.now().plusDays(4))
                    .build();

            when(roomRepository.findRoomAvailableByRoomTypeId(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> {
                bookingService.createBooking("customer@gmail.com", requestDTO);
            });

            assertEquals("No available rooms for the selected room type and dates", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Get Booking Tests & Access Control")
    class GetBookingTests {

        @Test
        @DisplayName("Khách hàng xem đơn đặt phòng của chính mình thành công")
        void testGetBookingById_OwnerCustomer_Success() {
            when(userAuthProviderRepository.findByProviderUserId("customer@gmail.com"))
                    .thenReturn(Optional.of(customerAuthProvider));
            when(bookingRepository.findById(1001L))
                    .thenReturn(Optional.of(mockBooking));

            BookingDetailDTO result = bookingService.getBookingById("customer@gmail.com", 1001L);

            assertNotNull(result);
            assertEquals(1001L, result.getBookingId());
            assertEquals("Customer A", result.getCustomerName());
        }

        @Test
        @DisplayName("Khách hàng khác cố ý xem đơn đặt phòng không thuộc về mình bị chặn (AccessDeniedException)")
        void testGetBookingById_UnauthorizedCustomer_ThrowsAccessDeniedException() {
            User otherUser = new User();
            otherUser.setId(99L);
            otherUser.setRole(UserRoleEnum.ROLE_CUSTOMER);

            UserAuthProvider otherProvider = new UserAuthProvider();
            otherProvider.setProviderUserId("intruder@gmail.com");
            otherProvider.setUser(otherUser);

            when(userAuthProviderRepository.findByProviderUserId("intruder@gmail.com"))
                    .thenReturn(Optional.of(otherProvider));
            when(bookingRepository.findById(1001L))
                    .thenReturn(Optional.of(mockBooking));

            assertThrows(AccessDeniedException.class, () -> {
                bookingService.getBookingById("intruder@gmail.com", 1001L);
            });
        }
    }

    @Nested
    @DisplayName("Cancel & Status Update Tests")
    class CancelAndStatusTests {

        @Test
        @DisplayName("Khách hàng hủy đơn đặt phòng thành công: Trạng thái chuyển sang CANCELED")
        void testCancelBookingByCustomer_Success() {
            when(userAuthProviderRepository.findByProviderUserId("customer@gmail.com"))
                    .thenReturn(Optional.of(customerAuthProvider));
            when(bookingRepository.findById(1001L))
                    .thenReturn(Optional.of(mockBooking));

            BookingDetailDTO result = bookingService.cancelBookingByCustomer("customer@gmail.com", 1001L);

            assertNotNull(result);
            assertEquals(BookingStatusEnum.CANCELED.name(), result.getStatus());
            verify(bookingRepository).save(mockBooking);
        }

        @Test
        @DisplayName("Hủy đơn thất bại khi đơn đặt phòng đã bị hủy từ trước (ConflictException)")
        void testCancelBookingByCustomer_AlreadyCanceled_ThrowsConflictException() {
            mockBooking.setStatus(BookingStatusEnum.CANCELED);

            when(userAuthProviderRepository.findByProviderUserId("customer@gmail.com"))
                    .thenReturn(Optional.of(customerAuthProvider));
            when(bookingRepository.findById(1001L))
                    .thenReturn(Optional.of(mockBooking));

            assertThrows(ConflictException.class, () -> {
                bookingService.cancelBookingByCustomer("customer@gmail.com", 1001L);
            });
        }

        @Test
        @DisplayName("Chủ khách sạn (Host) cập nhật trạng thái đơn đặt phòng thành công")
        void testUpdateBookingStatusByHost_Success() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));
            when(bookingRepository.findById(1001L))
                    .thenReturn(Optional.of(mockBooking));

            BookingDetailDTO result = bookingService.updateBookingStatusByHost("host@hotel.com", 1001L, BookingStatusEnum.CHECKED_IN);

            assertNotNull(result);
            assertEquals(BookingStatusEnum.CHECKED_IN.name(), result.getStatus());
            verify(bookingRepository).save(mockBooking);
        }
    }
}
