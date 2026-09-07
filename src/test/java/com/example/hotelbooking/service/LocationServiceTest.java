package com.example.hotelbooking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.hotelbooking.dto.location.LocationResponseDTO;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Location;
import com.example.hotelbooking.repository.LocationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for LocationService")
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationService locationService;

    private Location locationHcmQ1;
    private Location locationHcmQ3;
    private Location locationHnBaDinh;

    @BeforeEach
    void setUp() {
        locationHcmQ1 = new Location();
        locationHcmQ1.setLocationId(1L);
        locationHcmQ1.setProvinceName("Hồ Chí Minh");
        locationHcmQ1.setDistrictName("Quận 1");
        locationHcmQ1.setLatitude(10.7769);
        locationHcmQ1.setLongitude(106.7009);
        locationHcmQ1.setSearchVector("Quận 1, Hồ Chí Minh");

        locationHcmQ3 = new Location();
        locationHcmQ3.setLocationId(2L);
        locationHcmQ3.setProvinceName("Hồ Chí Minh");
        locationHcmQ3.setDistrictName("Quận 3");
        locationHcmQ3.setLatitude(10.7832);
        locationHcmQ3.setLongitude(106.6854);
        locationHcmQ3.setSearchVector("Quận 3, Hồ Chí Minh");

        locationHnBaDinh = new Location();
        locationHnBaDinh.setLocationId(3L);
        locationHnBaDinh.setProvinceName("Hà Nội");
        locationHnBaDinh.setDistrictName("Ba Đình");
        locationHnBaDinh.setLatitude(21.0333);
        locationHnBaDinh.setLongitude(105.8500);
        locationHnBaDinh.setSearchVector("Ba Đình, Hà Nội");
    }

    @Test
    @DisplayName("getAllProvinces - Should return distinct list of province names")
    void getAllProvinces_ShouldReturnDistinctProvinces() {
        when(locationRepository.findAllDistinctProvinces()).thenReturn(List.of("Hà Nội", "Hồ Chí Minh", "Đà Nẵng"));

        List<String> provinces = locationService.getAllProvinces();

        assertThat(provinces).hasSize(3);
        assertThat(provinces).containsExactly("Hà Nội", "Hồ Chí Minh", "Đà Nẵng");
        verify(locationRepository).findAllDistinctProvinces();
    }

    @Test
    @DisplayName("getDistrictsByProvince - Should return districts for given province")
    void getDistrictsByProvince_ShouldReturnDistricts_WhenProvinceExists() {
        when(locationRepository.findByProvinceNameIgnoreCase("Hồ Chí Minh"))
                .thenReturn(List.of(locationHcmQ1, locationHcmQ3));

        List<LocationResponseDTO> result = locationService.getDistrictsByProvince("Hồ Chí Minh");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDistrictName()).isEqualTo("Quận 1");
        assertThat(result.get(1).getDistrictName()).isEqualTo("Quận 3");
        verify(locationRepository).findByProvinceNameIgnoreCase("Hồ Chí Minh");
    }

    @Test
    @DisplayName("getDistrictsByProvince - Should return empty list when province is null or blank")
    void getDistrictsByProvince_ShouldReturnEmpty_WhenProvinceIsBlank() {
        List<LocationResponseDTO> resultNull = locationService.getDistrictsByProvince(null);
        List<LocationResponseDTO> resultEmpty = locationService.getDistrictsByProvince("   ");

        assertThat(resultNull).isEmpty();
        assertThat(resultEmpty).isEmpty();
    }

    @Test
    @DisplayName("getAllLocations - Should return all locations ordered")
    void getAllLocations_ShouldReturnAllOrdered() {
        when(locationRepository.findAllOrdered())
                .thenReturn(List.of(locationHnBaDinh, locationHcmQ1, locationHcmQ3));

        List<LocationResponseDTO> result = locationService.getAllLocations();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getProvinceName()).isEqualTo("Hà Nội");
        assertThat(result.get(1).getProvinceName()).isEqualTo("Hồ Chí Minh");
        verify(locationRepository).findAllOrdered();
    }

    @Test
    @DisplayName("getLocationById - Should return location when ID exists")
    void getLocationById_ShouldReturnLocation_WhenIdExists() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(locationHcmQ1));

        LocationResponseDTO result = locationService.getLocationById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getLocationId()).isEqualTo(1L);
        assertThat(result.getProvinceName()).isEqualTo("Hồ Chí Minh");
        assertThat(result.getDistrictName()).isEqualTo("Quận 1");
        verify(locationRepository).findById(1L);
    }

    @Test
    @DisplayName("getLocationById - Should throw NotFoundException when ID does not exist")
    void getLocationById_ShouldThrowNotFound_WhenIdDoesNotExist() {
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getLocationById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Location not found");
    }

    @Test
    @DisplayName("getLocationByKeyword - Should return paginated location list matching keyword")
    void getLocationByKeyword_ShouldReturnPagedLocations() {
        when(locationRepository.findByKeyword(eq("Hồ Chí Minh"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(locationHcmQ1, locationHcmQ3)));

        List<LocationResponseDTO> result = locationService.getLocationByKeyword("Hồ Chí Minh", 0, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDistrictName()).isEqualTo("Quận 1");
    }
}
