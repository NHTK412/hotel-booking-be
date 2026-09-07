package com.example.hotelbooking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.hotelbooking.dto.booking.BookingDetailDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.exception.GlobalExceptionHandler;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
@DisplayName("Integration Tests for BookingController (HTTP -> Controller -> ExceptionHandler)")
class BookingControllerIntegrationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private final CustomUserDetails mockUserDetails = CustomUserDetails.builder()
            .providerId("customer@gmail.com")
            .authorities(Collections.emptyList())
            .build();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /bookings Integration Tests")
    class CreateBookingIntegrationTests {

        @Test
        @DisplayName("400 Bad Request: Gửi booking request với dữ liệu không hợp lệ trả về 400 kèm field errors")
        void testCreateBooking_InvalidPayload_Returns400() throws Exception {
            BookingRequestDTO invalidDto = BookingRequestDTO.builder()
                    .customerName("")
                    .customerPhone("123")
                    .customerEmail("not-an-email")
                    .roomTypeId(null)
                    .build();

            mockMvc.perform(post("/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.customerName").exists())
                    .andExpect(jsonPath("$.data.customerEmail").exists())
                    .andExpect(jsonPath("$.data.roomTypeId").exists());
        }

        @Test
        @DisplayName("200 OK: Tạo đơn đặt phòng thành công")
        void testCreateBooking_Success_Returns200() throws Exception {
            BookingRequestDTO validDto = BookingRequestDTO.builder()
                    .customerName("Nguyen Van A")
                    .customerPhone("0901234567")
                    .customerEmail("customer@gmail.com")
                    .roomTypeId(1L)
                    .checkInDate(LocalDate.now().plusDays(1))
                    .checkOutDate(LocalDate.now().plusDays(3))
                    .build();

            BookingDetailDTO detailDTO = BookingDetailDTO.builder()
                    .bookingId(100L)
                    .customerName("Nguyen Van A")
                    .customerEmail("customer@gmail.com")
                    .status(BookingStatusEnum.WAITING_FOR_PAYMENT.name())
                    .finalPrice(1500000.0)
                    .build();

            when(bookingService.createBooking(any(), any())).thenReturn(detailDTO);

            mockMvc.perform(post("/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.bookingId").value(100L))
                    .andExpect(jsonPath("$.data.status").value("WAITING_FOR_PAYMENT"));
        }
    }

    @Nested
    @DisplayName("GET /bookings/{bookingId} Integration Tests")
    class GetBookingIntegrationTests {

        @Test
        @DisplayName("200 OK: Lấy thông tin chi tiết đơn đặt phòng thành công")
        void testGetBookingById_Success_Returns200() throws Exception {
            BookingDetailDTO detailDTO = BookingDetailDTO.builder()
                    .bookingId(100L)
                    .customerName("Nguyen Van A")
                    .customerEmail("customer@gmail.com")
                    .status(BookingStatusEnum.WAITING_FOR_PAYMENT.name())
                    .finalPrice(1500000.0)
                    .build();

            when(bookingService.getBookingById(eq("customer@gmail.com"), eq(100L))).thenReturn(detailDTO);

            mockMvc.perform(get("/bookings/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.bookingId").value(100L))
                    .andExpect(jsonPath("$.data.customerName").value("Nguyen Van A"));
        }
    }
}
