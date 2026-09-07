package com.example.hotelbooking.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.hotelbooking.dto.location.LocationResponseDTO;
import com.example.hotelbooking.exception.GlobalExceptionHandler;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.service.LocationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Integration Tests for LocationController (HTTP -> Controller -> ExceptionHandler)")
class LocationControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationController locationController;

    private LocationResponseDTO locationHcmQ1;
    private LocationResponseDTO locationHcmQ3;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(locationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        locationHcmQ1 = LocationResponseDTO.builder()
                .locationId(1L)
                .provinceName("Hồ Chí Minh")
                .districtName("Quận 1")
                .latitude(10.7769)
                .longitude(106.7009)
                .searchVector("Quận 1, Hồ Chí Minh")
                .build();

        locationHcmQ3 = LocationResponseDTO.builder()
                .locationId(2L)
                .provinceName("Hồ Chí Minh")
                .districtName("Quận 3")
                .latitude(10.7832)
                .longitude(106.6854)
                .searchVector("Quận 3, Hồ Chí Minh")
                .build();
    }

    @Nested
    @DisplayName("GET /locations/provinces Integration Tests")
    class GetAllProvincesIntegrationTests {

        @Test
        @DisplayName("200 OK: Trả về danh sách tên các tỉnh/thành phố")
        void testGetAllProvinces_Returns200() throws Exception {
            when(locationService.getAllProvinces()).thenReturn(List.of("Hà Nội", "Hồ Chí Minh", "Đà Nẵng"));

            mockMvc.perform(get("/locations/provinces"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Lấy danh sách tỉnh/thành phố thành công"))
                    .andExpect(jsonPath("$.data[0]").value("Hà Nội"))
                    .andExpect(jsonPath("$.data[1]").value("Hồ Chí Minh"))
                    .andExpect(jsonPath("$.data[2]").value("Đà Nẵng"));
        }
    }

    @Nested
    @DisplayName("GET /locations/districts Integration Tests")
    class GetDistrictsByProvinceIntegrationTests {

        @Test
        @DisplayName("200 OK: Trả về danh sách quận/huyện thuộc tỉnh thành")
        void testGetDistrictsByProvince_Returns200() throws Exception {
            when(locationService.getDistrictsByProvince("Hồ Chí Minh"))
                    .thenReturn(List.of(locationHcmQ1, locationHcmQ3));

            mockMvc.perform(get("/locations/districts").param("province", "Hồ Chí Minh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Lấy danh sách quận/huyện thành công"))
                    .andExpect(jsonPath("$.data[0].districtName").value("Quận 1"))
                    .andExpect(jsonPath("$.data[0].locationId").value(1))
                    .andExpect(jsonPath("$.data[1].districtName").value("Quận 3"))
                    .andExpect(jsonPath("$.data[1].locationId").value(2));
        }
    }

    @Nested
    @DisplayName("GET /locations/all Integration Tests")
    class GetAllLocationsIntegrationTests {

        @Test
        @DisplayName("200 OK: Trả về toàn bộ danh sách địa điểm sắp xếp")
        void testGetAllLocations_Returns200() throws Exception {
            when(locationService.getAllLocations()).thenReturn(List.of(locationHcmQ1, locationHcmQ3));

            mockMvc.perform(get("/locations/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }
    }

    @Nested
    @DisplayName("GET /locations/{locationId} Integration Tests")
    class GetLocationByIdIntegrationTests {

        @Test
        @DisplayName("200 OK: Lấy thông tin địa điểm theo ID thành công")
        void testGetLocationById_Success_Returns200() throws Exception {
            when(locationService.getLocationById(1L)).thenReturn(locationHcmQ1);

            mockMvc.perform(get("/locations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.locationId").value(1))
                    .andExpect(jsonPath("$.data.provinceName").value("Hồ Chí Minh"))
                    .andExpect(jsonPath("$.data.districtName").value("Quận 1"));
        }

        @Test
        @DisplayName("404 Not Found: Địa điểm không tồn tại trả về lỗi qua GlobalExceptionHandler")
        void testGetLocationById_NotFound_Returns404() throws Exception {
            when(locationService.getLocationById(999L)).thenThrow(new NotFoundException("Location not found"));

            mockMvc.perform(get("/locations/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Location not found"));
        }
    }
}
