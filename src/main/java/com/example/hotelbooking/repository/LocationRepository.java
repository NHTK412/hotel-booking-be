package com.example.hotelbooking.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.Accommodations;
import com.example.hotelbooking.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    // @Query("""
    // SELECT l
    // FROM Location l
    // WHERE l.searchVector LIKE %:keyword%
    // """)
    // List<Location> findByKeyword(String keyword);

    @Query("""
                SELECT l
                FROM Location l
                WHERE l.searchVector LIKE %:keyword%
            """)
    Page<Location> findByKeyword(String keyword, org.springframework.data.domain.Pageable pageable);

    List<Location> findByDistrictNameContainingIgnoreCase(String districtName);

    List<Location> findByProvinceNameContainingIgnoreCase(String provinceName);

    @Query("""
                SELECT l
                FROM Location l
                WHERE l.districtName LIKE %:subAdministrativeArea% AND l.provinceName LIKE %:administrativeArea%
            """)
    List<Location> findCurrentLocation(String subAdministrativeArea, String administrativeArea);

    @Query("""
            SELECT l FROM Location l WHERE l.geoHash LIKE :prefix%
            """)
    List<Location> findNearby(@Param("prefix") String prefix);
}
