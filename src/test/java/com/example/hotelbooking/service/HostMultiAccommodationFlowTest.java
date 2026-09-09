package com.example.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.hotelbooking.dto.accommodation.AccommodationSummaryDTO;
import com.example.hotelbooking.dto.booking.BookingSummaryDTO;
import com.example.hotelbooking.dto.room.RoomSummaryDTO;
import com.example.hotelbooking.dto.room.UpdateRoomDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeSummaryDTO;
import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.AccommodationTypeEnum;
import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.AccessDeniedException;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Accommodation;
import com.example.hotelbooking.model.AccommodationStaff;
import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.RoomType;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.AccommodationRepository;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomTypeRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for Host Multi-Accommodation Flow")
class HostMultiAccommodationFlowTest {

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private AccommodationService accommodationService;

    @InjectMocks
    private RoomTypeService roomTypeService;

    @InjectMocks
    private BookingService bookingService;

    private User hostUser;
    private UserAuthProvider hostAuthProvider;
    private Accommodation hotel1;
    private Accommodation hotel2;

    @BeforeEach
    void setUp() {
        hostUser = new User();
        hostUser.setId(1L);
        hostUser.setName("Chủ Khách Sạn");
        hostUser.setEmail("host@hotel.com");
        hostUser.setRole(UserRoleEnum.ROLE_HOST);

        hostAuthProvider = new UserAuthProvider();
        hostAuthProvider.setId(101L);
        hostAuthProvider.setType(AuthProviderTypeEnum.LOCAL);
        hostAuthProvider.setProviderUserId("host@hotel.com");
        hostAuthProvider.setUser(hostUser);

        hotel1 = new Accommodation();
        hotel1.setAccommodationId(10L);
        hotel1.setAccommodationName("Grand Luxury Hotel");
        hotel1.setType(AccommodationTypeEnum.HOTEL);
        hotel1.setAddress("123 Lê Lợi");
        hotel1.setIsDeleted(false);

        hotel2 = new Accommodation();
        hotel2.setAccommodationId(20L);
        hotel2.setAccommodationName("Seaside Resort");
        hotel2.setType(AccommodationTypeEnum.RESORT);
        hotel2.setAddress("45 Trần Phú");
        hotel2.setIsDeleted(false);

        AccommodationStaff staff1 = new AccommodationStaff();
        staff1.setAccommodationStaffId(1L);
        staff1.setAccommodation(hotel1);
        staff1.setUser(hostUser);
        staff1.setRole(AccommodationStaffRoleEnum.ROLE_MANAGER);

        AccommodationStaff staff2 = new AccommodationStaff();
        staff2.setAccommodationStaffId(2L);
        staff2.setAccommodation(hotel2);
        staff2.setUser(hostUser);
        staff2.setRole(AccommodationStaffRoleEnum.ROLE_MANAGER);

        List<AccommodationStaff> staffList = new ArrayList<>();
        staffList.add(staff1);
        staffList.add(staff2);
        hostUser.setAccommodationStaffs(staffList);
    }

    @Nested
    @DisplayName("Tests for AccommodationService.getMyAccommodations")
    class GetMyAccommodationsTests {

        @Test
        @DisplayName("Should return accommodations managed by Host")
        void shouldReturnHostAccommodations() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            List<AccommodationSummaryDTO> results = accommodationService.getMyAccommodations("host@hotel.com");

            assertNotNull(results);
            assertEquals(2, results.size());
            List<Long> ids = results.stream().map(AccommodationSummaryDTO::getAccommodationId).toList();
            assertTrue(ids.contains(10L));
            assertTrue(ids.contains(20L));
        }

        @Test
        @DisplayName("Should exclude soft-deleted accommodations")
        void shouldExcludeDeletedAccommodations() {
            hotel2.setIsDeleted(true);
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            List<AccommodationSummaryDTO> results = accommodationService.getMyAccommodations("host@hotel.com");

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals(10L, results.get(0).getAccommodationId());
        }

        @Test
        @DisplayName("Should throw NotFoundException if provider not found")
        void shouldThrowIfProviderNotFound() {
            when(userAuthProviderRepository.findByProviderUserId("unknown"))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> accommodationService.getMyAccommodations("unknown"));
        }
    }

    @Nested
    @DisplayName("Tests for RoomTypeService.getHostRoomTypes")
    class GetHostRoomTypesTests {

        @Test
        @DisplayName("Should return room types for all accommodations if accommodationId is null")
        void shouldReturnAllRoomTypesWhenNoAccommodationId() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            RoomType rt1 = new RoomType();
            rt1.setRoomtypeId(101L);
            rt1.setName("Deluxe Room");
            rt1.setPrice(1000000.0);
            rt1.setAccommodation(hotel1);
            rt1.setIsDeleted(false);

            RoomType rt2 = new RoomType();
            rt2.setRoomtypeId(201L);
            rt2.setName("Resort Villa");
            rt2.setPrice(3000000.0);
            rt2.setAccommodation(hotel2);
            rt2.setIsDeleted(false);

            Pageable pageable = PageRequest.of(0, 10);
            Page<RoomType> page = new PageImpl<>(List.of(rt1, rt2), pageable, 2);

            when(roomTypeRepository.findByAccommodation_AccommodationIdInAndIsDeletedFalse(any(), eq(pageable)))
                    .thenReturn(page);

            List<RoomTypeSummaryDTO> results = roomTypeService.getHostRoomTypes("host@hotel.com", null, pageable);

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("Grand Luxury Hotel", results.get(0).getAccommodationName());
            assertEquals("Seaside Resort", results.get(1).getAccommodationName());
        }

        @Test
        @DisplayName("Should return room types for specific accommodation if authorized")
        void shouldReturnRoomTypesForSpecificAccommodation() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            RoomType rt1 = new RoomType();
            rt1.setRoomtypeId(101L);
            rt1.setName("Deluxe Room");
            rt1.setPrice(1000000.0);
            rt1.setAccommodation(hotel1);
            rt1.setIsDeleted(false);

            RoomType rt2 = new RoomType();
            rt2.setRoomtypeId(102L);
            rt2.setName("Suite Room");
            rt2.setPrice(2000000.0);
            rt2.setAccommodation(hotel1);
            rt2.setIsDeleted(false);

            Pageable pageable = PageRequest.of(0, 10);
            Page<RoomType> page = new PageImpl<>(List.of(rt1, rt2), pageable, 2);

            when(roomTypeRepository.findByAccommodation_AccommodationIdAndIsDeletedFalse(10L, pageable))
                    .thenReturn(page);

            List<RoomTypeSummaryDTO> results = roomTypeService.getHostRoomTypes("host@hotel.com", 10L, pageable);

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals(10L, results.get(0).getAccommodationId());
        }

        @Test
        @DisplayName("Should throw AccessDeniedException if accommodationId does not belong to Host")
        void shouldThrowAccessDeniedWhenUnauthorized() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            Pageable pageable = PageRequest.of(0, 10);

            assertThrows(AccessDeniedException.class,
                    () -> roomTypeService.getHostRoomTypes("host@hotel.com", 999L, pageable));
        }
    }

    @Nested
    @DisplayName("Tests for RoomTypeService.updateRoom (Physical Room)")
    class UpdatePhysicalRoomTests {

        @Test
        @DisplayName("Should update physical room successfully")
        void shouldUpdatePhysicalRoomSuccessfully() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            Room r1 = new Room();
            r1.setRoomId(501L);
            r1.setName("101");
            r1.setStatus(StatusEnum.ACTIVE);
            r1.setIsDeleted(false);

            RoomType rt = new RoomType();
            rt.setRoomtypeId(101L);
            rt.setAccommodation(hotel1);
            rt.setRooms(new ArrayList<>(List.of(r1)));

            when(roomTypeRepository.findById(101L)).thenReturn(Optional.of(rt));

            UpdateRoomDTO dto = UpdateRoomDTO.builder()
                    .roomNumber("101A")
                    .status(StatusEnum.INACTIVE)
                    .build();

            RoomSummaryDTO result = roomTypeService.updateRoom("host@hotel.com", 101L, 501L, dto);

            assertNotNull(result);
            assertEquals("101A", result.getRoomNumber());
            assertEquals(StatusEnum.INACTIVE, result.getStatus());
        }

        @Test
        @DisplayName("Should throw ConflictException when new roomNumber duplicates an existing room")
        void shouldThrowConflictWhenRoomNumberDuplicates() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            Room r1 = new Room();
            r1.setRoomId(501L);
            r1.setName("101");
            r1.setStatus(StatusEnum.ACTIVE);
            r1.setIsDeleted(false);

            Room r2 = new Room();
            r2.setRoomId(502L);
            r2.setName("102");
            r2.setStatus(StatusEnum.ACTIVE);
            r2.setIsDeleted(false);

            RoomType rt = new RoomType();
            rt.setRoomtypeId(101L);
            rt.setAccommodation(hotel1);
            rt.setRooms(new ArrayList<>(List.of(r1, r2)));

            when(roomTypeRepository.findById(101L)).thenReturn(Optional.of(rt));

            UpdateRoomDTO dto = UpdateRoomDTO.builder()
                    .roomNumber("102")
                    .build();

            assertThrows(ConflictException.class,
                    () -> roomTypeService.updateRoom("host@hotel.com", 101L, 501L, dto));
        }

        @Test
        @DisplayName("Should throw AccessDeniedException if Host does not manage accommodation")
        void shouldThrowAccessDeniedWhenHostDoesNotManageAccommodation() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            Accommodation unmanaged = new Accommodation();
            unmanaged.setAccommodationId(999L);

            RoomType rt = new RoomType();
            rt.setRoomtypeId(101L);
            rt.setAccommodation(unmanaged);

            when(roomTypeRepository.findById(101L)).thenReturn(Optional.of(rt));

            UpdateRoomDTO dto = UpdateRoomDTO.builder().roomNumber("101A").build();

            assertThrows(AccessDeniedException.class,
                    () -> roomTypeService.updateRoom("host@hotel.com", 101L, 501L, dto));
        }
    }

    @Nested
    @DisplayName("Tests for BookingService.getBookingsForHost")
    class GetBookingsForHostTests {

        @Test
        @DisplayName("Should return bookings for all host accommodations when accommodationId is null")
        void shouldReturnBookingsForAllHostAccommodations() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            RoomType rt = new RoomType();
            rt.setRoomtypeId(101L);
            rt.setName("Deluxe Room");
            rt.setAccommodation(hotel1);

            Room room = new Room();
            room.setRoomId(501L);
            room.setName("101");
            room.setRoomType(rt);

            Booking b1 = new Booking();
            b1.setBookingId(1001L);
            b1.setCheckInAt(LocalDateTime.now().plusDays(1));
            b1.setCheckOutAt(LocalDateTime.now().plusDays(3));
            b1.setCustomerName("Khách A");
            b1.setCustomerPhone("0901111222");
            b1.setOriginalPrice(2000000.0);
            b1.setFinalPrice(2000000.0);
            b1.setStatus(BookingStatusEnum.PENDING);
            b1.setRoom(room);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Booking> page = new PageImpl<>(List.of(b1), pageable, 1);

            when(bookingRepository.findBookingsByHostMultiple(any(), eq(null), eq(pageable)))
                    .thenReturn(page);

            List<BookingSummaryDTO> results = bookingService.getBookingsForHost("host@hotel.com", null, null, 0, 10);

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("Grand Luxury Hotel", results.get(0).getAccommodationName());
            assertEquals("Deluxe Room", results.get(0).getRoomTypeName());
            assertEquals("101", results.get(0).getRoomNumber());
        }

        @Test
        @DisplayName("Should throw AccessDeniedException if accommodationId does not belong to Host")
        void shouldThrowAccessDeniedWhenBookingAccommodationUnauthorized() {
            when(userAuthProviderRepository.findByProviderUserId("host@hotel.com"))
                    .thenReturn(Optional.of(hostAuthProvider));

            assertThrows(AccessDeniedException.class,
                    () -> bookingService.getBookingsForHost("host@hotel.com", 999L, null, 0, 10));
        }
    }
}
