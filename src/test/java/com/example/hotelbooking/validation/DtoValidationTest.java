package com.example.hotelbooking.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.hotelbooking.dto.accommodation.AccommodationRequestDTO;
import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthRegisterDTO;
import com.example.hotelbooking.dto.auth.RefreshTokenRequestDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.dto.device.DeviceRegistrationRequest;
import com.example.hotelbooking.dto.review.ReviewRequestDTO;
import com.example.hotelbooking.dto.room.RoomRequestDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeRequestDTO;
import com.example.hotelbooking.dto.user.CreateHostDTO;
import com.example.hotelbooking.dto.zalopay.CreateOrderRequest;
import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.AccommodationTypeEnum;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("Unit Tests for DTO Validation Constraints")
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("AuthRegisterDTO Validation")
    class AuthRegisterDtoValidation {

        @Test
        @DisplayName("Should pass when all fields are valid")
        void shouldPassWithValidData() {
            AuthRegisterDTO dto = new AuthRegisterDTO();
            dto.setName("Nguyen Van A");
            dto.setEmail("user@example.com");
            dto.setPassword("Secret123!");
            dto.setPhone("0912345678");

            Set<ConstraintViolation<AuthRegisterDTO>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(), "Expected no violations for valid AuthRegisterDTO");
        }

        @Test
        @DisplayName("Should fail when email is invalid format")
        void shouldFailWithInvalidEmail() {
            AuthRegisterDTO dto = new AuthRegisterDTO();
            dto.setName("Nguyen Van A");
            dto.setEmail("invalid-email");
            dto.setPassword("Secret123!");
            dto.setPhone("0912345678");

            Set<ConstraintViolation<AuthRegisterDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        }

        @Test
        @DisplayName("Should fail when password is less than 6 characters")
        void shouldFailWithShortPassword() {
            AuthRegisterDTO dto = new AuthRegisterDTO();
            dto.setName("Nguyen Van A");
            dto.setEmail("user@example.com");
            dto.setPassword("123");
            dto.setPhone("0912345678");

            Set<ConstraintViolation<AuthRegisterDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        }

        @Test
        @DisplayName("Should fail when phone format is invalid")
        void shouldFailWithInvalidPhone() {
            AuthRegisterDTO dto = new AuthRegisterDTO();
            dto.setName("Nguyen Van A");
            dto.setEmail("user@example.com");
            dto.setPassword("Secret123!");
            dto.setPhone("12345");

            Set<ConstraintViolation<AuthRegisterDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));
        }
    }

    @Nested
    @DisplayName("AuthLoginDTO Validation")
    class AuthLoginDtoValidation {

        @Test
        @DisplayName("Should pass with valid email and password")
        void shouldPassWithValidLogin() {
            AuthLoginDTO dto = new AuthLoginDTO();
            dto.setEmail("user@example.com");
            dto.setPassword("password123");

            Set<ConstraintViolation<AuthLoginDTO>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when email or password is blank")
        void shouldFailWithBlankFields() {
            AuthLoginDTO dto = new AuthLoginDTO();
            dto.setEmail("");
            dto.setPassword("");

            Set<ConstraintViolation<AuthLoginDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.size() >= 2);
        }
    }

    @Nested
    @DisplayName("BookingRequestDTO Validation")
    class BookingRequestDtoValidation {

        @Test
        @DisplayName("Should pass with valid booking request")
        void shouldPassWithValidBooking() {
            BookingRequestDTO dto = new BookingRequestDTO();
            dto.setRoomTypeId(1L);
            dto.setCustomerName("Nguyen Van A");
            dto.setCustomerPhone("0912345678");
            dto.setCustomerEmail("user@example.com");
            dto.setCheckInDate(LocalDate.now().plusDays(1));
            dto.setCheckOutDate(LocalDate.now().plusDays(3));

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when checkInDate is in the past")
        void shouldFailWithPastCheckInDate() {
            BookingRequestDTO dto = new BookingRequestDTO();
            dto.setRoomTypeId(1L);
            dto.setCustomerName("Nguyen Van A");
            dto.setCustomerPhone("0912345678");
            dto.setCustomerEmail("user@example.com");
            dto.setCheckInDate(LocalDate.now().minusDays(2));
            dto.setCheckOutDate(LocalDate.now().plusDays(3));

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("checkInDate")));
        }

        @Test
        @DisplayName("Should fail when roomTypeId is null or negative")
        void shouldFailWithInvalidRoomTypeId() {
            BookingRequestDTO dto = new BookingRequestDTO();
            dto.setRoomTypeId(-1L);
            dto.setCustomerName("Nguyen Van A");
            dto.setCustomerPhone("0912345678");
            dto.setCustomerEmail("user@example.com");
            dto.setCheckInDate(LocalDate.now().plusDays(1));
            dto.setCheckOutDate(LocalDate.now().plusDays(2));

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("roomTypeId")));
        }
    }

    @Nested
    @DisplayName("ReviewRequestDTO Validation")
    class ReviewRequestDtoValidation {

        @Test
        @DisplayName("Should pass when rating is between 1 and 5")
        void shouldPassWithValidRating() {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setBookingId(100L);
            dto.setRating(5);
            dto.setComment("Dịch vụ tuyệt vời!");

            Set<ConstraintViolation<ReviewRequestDTO>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when rating exceeds 5 or is less than 1")
        void shouldFailWithInvalidRating() {
            ReviewRequestDTO dtoHigh = new ReviewRequestDTO();
            dtoHigh.setBookingId(100L);
            dtoHigh.setRating(6);

            Set<ConstraintViolation<ReviewRequestDTO>> violationsHigh = validator.validate(dtoHigh);
            assertFalse(violationsHigh.isEmpty());
            assertTrue(violationsHigh.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rating")));

            ReviewRequestDTO dtoLow = new ReviewRequestDTO();
            dtoLow.setBookingId(100L);
            dtoLow.setRating(0);

            Set<ConstraintViolation<ReviewRequestDTO>> violationsLow = validator.validate(dtoLow);
            assertFalse(violationsLow.isEmpty());
            assertTrue(violationsLow.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rating")));
        }
    }

    @Nested
    @DisplayName("RoomTypeRequestDTO Validation")
    class RoomTypeRequestDtoValidation {

        @Test
        @DisplayName("Should pass with valid room type request")
        void shouldPassWithValidRoomType() {
            RoomTypeRequestDTO dto = new RoomTypeRequestDTO();
            dto.setAccommodationId(1L);
            dto.setName("Deluxe Ocean View");
            dto.setPrice(1500000.0);
            dto.setDiscount(0.1);
            dto.setCapacity(2);
            dto.setBedroom(1);

            Set<ConstraintViolation<RoomTypeRequestDTO>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when price is negative or zero")
        void shouldFailWithNegativePrice() {
            RoomTypeRequestDTO dto = new RoomTypeRequestDTO();
            dto.setAccommodationId(1L);
            dto.setName("Deluxe Ocean View");
            dto.setPrice(-500.0);
            dto.setCapacity(2);
            dto.setBedroom(1);

            Set<ConstraintViolation<RoomTypeRequestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
        }
    }

    @Nested
    @DisplayName("AccommodationRequestDTO Validation")
    class AccommodationRequestDtoValidation {

        @Test
        @DisplayName("Should pass with valid accommodation request")
        void shouldPassWithValidAccommodation() {
            AccommodationRequestDTO dto = new AccommodationRequestDTO();
            dto.setAccommodationName("Grand Luxury Hotel");
            dto.setAddress("123 Lê Lợi");
            dto.setType(AccommodationTypeEnum.HOTEL);
            dto.setLocationId(1L);

            Set<ConstraintViolation<AccommodationRequestDTO>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when name or address is blank")
        void shouldFailWithBlankNameAndAddress() {
            AccommodationRequestDTO dto = new AccommodationRequestDTO();
            dto.setAccommodationName("");
            dto.setAddress("");
            dto.setType(AccommodationTypeEnum.HOTEL);
            dto.setLocationId(1L);

            Set<ConstraintViolation<AccommodationRequestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("accommodationName")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("address")));
        }
    }

    @Nested
    @DisplayName("CreateHostDTO Validation")
    class CreateHostDtoValidation {

        @Test
        @DisplayName("Should pass with valid host request")
        void shouldPassWithValidHost() {
            CreateHostDTO dto = new CreateHostDTO();
            dto.setName("Host 1");
            dto.setEmail("host@example.com");
            dto.setPhone("0912345678");
            dto.setAccommodationId(1L);
            dto.setHostRole(AccommodationStaffRoleEnum.ROLE_MANAGER);

            Set<ConstraintViolation<CreateHostDTO>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when phone format or email format is invalid")
        void shouldFailWithInvalidPhoneAndEmail() {
            CreateHostDTO dto = new CreateHostDTO();
            dto.setName("Host 1");
            dto.setEmail("bad-email");
            dto.setPhone("0123");
            dto.setAccommodationId(1L);
            dto.setHostRole(AccommodationStaffRoleEnum.ROLE_MANAGER);

            Set<ConstraintViolation<CreateHostDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));
        }
    }

    @Nested
    @DisplayName("DeviceRegistrationRequest & ZaloPay Validation")
    class OtherDtoValidation {

        @Test
        @DisplayName("Should fail when FCM token is blank")
        void shouldFailWhenFcmTokenBlank() {
            DeviceRegistrationRequest req = new DeviceRegistrationRequest();
            req.setFcmToken("   ");

            Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fcmToken")));
        }

        @Test
        @DisplayName("Should fail when ZaloPay bookingId is null")
        void shouldFailWhenZaloPayBookingIdNull() {
            CreateOrderRequest req = new CreateOrderRequest();
            req.setBookingId(null);

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bookingId")));
        }

        @Test
        @DisplayName("Should fail when room numbers list is empty")
        void shouldFailWhenRoomNumbersEmpty() {
            RoomRequestDTO req = new RoomRequestDTO();
            req.setRoomNumbers(Collections.emptyList());

            Set<ConstraintViolation<RoomRequestDTO>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("roomNumbers")));
        }

        @Test
        @DisplayName("Should fail when RefreshToken is blank")
        void shouldFailWhenRefreshTokenBlank() {
            RefreshTokenRequestDTO req = new RefreshTokenRequestDTO();
            req.setRefreshToken("");

            Set<ConstraintViolation<RefreshTokenRequestDTO>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken")));
        }
    }
}
