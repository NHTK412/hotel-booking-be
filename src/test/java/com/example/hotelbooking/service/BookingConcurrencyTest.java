package com.example.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.example.hotelbooking.dto.booking.BookingDetailDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
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
class BookingConcurrencyTest {

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

    private Room mockRoom;
    private RoomType mockRoomType;
    private User mockUser;
    private UserAuthProvider mockAuthProvider;
    private BookingRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        com.example.hotelbooking.model.Accommodation mockAccommodation = new com.example.hotelbooking.model.Accommodation();
        mockAccommodation.setAccommodationId(1L);
        mockAccommodation.setAccommodationName("Grand Luxury Hotel");

        mockRoomType = new RoomType();
        mockRoomType.setRoomtypeId(1L);
        mockRoomType.setName("Deluxe Ocean View");
        mockRoomType.setPrice(100.0);
        mockRoomType.setDiscount(10.0);
        mockRoomType.setAccommodation(mockAccommodation);

        mockRoom = new Room();
        mockRoom.setRoomId(101L);
        mockRoom.setName("Room 101");
        mockRoom.setStatus(StatusEnum.ACTIVE);
        mockRoom.setRoomType(mockRoomType);
        mockRoom.setVersion(1L);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("Nguyen Van A");
        mockUser.setEmail("test@gmail.com");
        mockUser.setRole(UserRoleEnum.ROLE_CUSTOMER);

        mockAuthProvider = new UserAuthProvider();
        mockAuthProvider.setProviderUserId("test@gmail.com");
        mockAuthProvider.setUser(mockUser);

        requestDTO = BookingRequestDTO.builder()
                .roomTypeId(1L)
                .customerName("Nguyen Van A")
                .customerEmail("test@gmail.com")
                .customerPhone("0901234567")
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .build();
    }

    @Test
    @DisplayName("Kiểm thử 10 requests đồng thời cùng đặt 1 phòng: Chỉ 1 request thành công, 9 requests còn lại bị chặn bởi Optimistic Lock")
    void testConcurrentBookingsForSingleRoom_OnlyOneSucceeds() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherFailures = new AtomicInteger(0);

        when(userAuthProviderRepository.findByProviderUserId(anyString()))
                .thenReturn(Optional.of(mockAuthProvider));

        when(roomRepository.findRoomAvailableByRoomTypeId(any(), any(), any()))
                .thenReturn(List.of(mockRoom));

        // Giả lập cơ chế Optimistic Locking: Luồng đầu tiên vào thành công, các luồng
        // tiếp theo gặp xung đột version
        AtomicInteger versionIncrementer = new AtomicInteger(1);
        when(roomRepository.findByIdWithOptimisticLock(101L)).thenAnswer(invocation -> {
            int currentVersion = versionIncrementer.getAndIncrement();
            if (currentVersion == 1) {
                return Optional.of(mockRoom);
            } else {
                // Các luồng sau gặp xung đột phiên bản và ném
                // ObjectOptimisticLockingFailureException
                throw new ObjectOptimisticLockingFailureException(Room.class, 101L);
            }
        });

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking savedBooking = invocation.getArgument(0);
            savedBooking.setBookingId(999L);
            return savedBooking;
        });

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Đợi tất cả 10 threads sẵn sàng để bắn cùng 1 mili-giây
                    BookingDetailDTO result = bookingService.createBooking("test@gmail.com", requestDTO);
                    if (result != null) {
                        successCount.incrementAndGet();
                    }
                } catch (ObjectOptimisticLockingFailureException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    otherFailures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Phát lệnh cho cả 10 luồng cùng chạy
        doneLatch.await(); // Đợi cả 10 luồng hoàn tất
        executorService.shutdown();


        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 request được phép đặt phòng thành công");
        assertEquals(9, conflictCount.get(), "9 requests còn lại bắt buộc phải nhận lỗi xung đột phiên bản");
        assertEquals(0, otherFailures.get(), "Không có lỗi bất thường nào khác xảy ra");
    }
}
