package com.example.hotelbooking.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("""
                SELECT l
                FROM Location l
                WHERE l.searchVector LIKE %:keyword%
            """)
    Page<Location> findByKeyword(String keyword, Pageable pageable);

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

    @Query("SELECT DISTINCT l.provinceName FROM Location l WHERE l.provinceName IS NOT NULL AND TRIM(l.provinceName) != '' ORDER BY l.provinceName ASC")
    List<String> findAllDistinctProvinces();

    @Query("SELECT l FROM Location l WHERE LOWER(l.provinceName) = LOWER(:provinceName) ORDER BY l.districtName ASC")
    List<Location> findByProvinceNameIgnoreCase(@Param("provinceName") String provinceName);

    @Query("SELECT l FROM Location l ORDER BY l.provinceName ASC, l.districtName ASC")
    List<Location> findAllOrdered();
}

