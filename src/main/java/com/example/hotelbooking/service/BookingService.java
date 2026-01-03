package com.example.hotelbooking.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.booking.BookingDetailDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.Bookings;
import com.example.hotelbooking.model.Rooms;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRespository;
import com.example.hotelbooking.repository.RoomTypeRepository;
import com.example.hotelbooking.repository.UserRepository;

@Service
public class BookingService {

    final BookingRepository bookingRepository;

    final RoomTypeRepository roomTypeRepository;

    final RoomRespository roomRespository;

    final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, RoomTypeRepository roomTypeRepository,
            RoomRespository roomRespository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRespository = roomRespository;
        this.userRepository = userRepository;
    }

    public BookingDetailDTO createBooking(BookingRequestDTO bookingRequestDTO) {

        // final RoomTypes roomTypes =
        // roomTypeRepository.findById(bookingRequestDTO.getRoomTypeId())
        // .orElseThrow(() -> new NotFoundException("Room type not found"));

        final List<Rooms> availableRooms = roomRespository
                .findByRoomType_roomtypeIdAndStatus(bookingRequestDTO.getRoomTypeId(), StatusEnum.ACTIVE);

        if (availableRooms.isEmpty()) {
            throw new NotFoundException("No available rooms for the selected room type");
        }

        final Users user = userRepository.findById(Long.valueOf(4))
                .orElseThrow(() -> new NotFoundException("User not found"));

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
        booking.setCheckInDate(bookingRequestDTO.getCheckInDate());
        booking.setCheckOutDate(bookingRequestDTO.getCheckOutDate());

        // set prices
        booking.setOriginalPrice(bookingRequestDTO.getOriginalPrice());
        booking.setDiscountedPrice(bookingRequestDTO.getDiscountedPrice());
        booking.setFinalPrice(bookingRequestDTO.getFinalPrice());

        // set status
        booking.setStatus(BookingStatusEnum.PENDING);

        bookingRepository.save(booking);

        // return new BookingDetailDTO(booking);
        return mapToBookingDetailDTO(booking);
    }

    private BookingDetailDTO mapToBookingDetailDTO(Bookings booking) {
        return BookingDetailDTO.builder()
                .bookingId(booking.getBookingId())
                // customer info
                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .customerEmail(booking.getCustomerEmail())
                // booking dates
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                // prices
                .originalPrice(booking.getOriginalPrice())
                .discountedPrice(booking.getDiscountedPrice())
                .finalPrice(booking.getFinalPrice())
                // booking status
                .status(booking.getStatus().name())
                .build();
    }
}
