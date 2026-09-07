package com.example.hotelbooking.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.AccommodationTypeEnum;
import com.example.hotelbooking.model.Accommodation;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

        Page<Accommodation> findByIsDeletedFalse(Pageable pageable);

        Page<Accommodation> findByIsDeletedFalseAndType(Pageable pageable, AccommodationTypeEnum type);

        Page<Accommodation> findByIsDeletedFalseAndFavoritedByUsers_id(Pageable pageable, Long id);

        @Query("""
                        SELECT a FROM Accommodation a
                        WHERE a.isDeleted = false
                        AND a.geohash LIKE :prefix%
                        """)
        List<Accommodation> findNearby(@Param("prefix") String prefix);

        @Query("""
                        SELECT a FROM Accommodation a
                        WHERE a.isDeleted = false
                        AND a.geohash LIKE :prefix%
                        AND a.type = :type
                        """)
        List<Accommodation> findNearbyWithType(@Param("prefix") String prefix, @Param("type") AccommodationTypeEnum type);

        @Query("""
                        SELECT a FROM Accommodation a WHERE a.isDeleted = false AND
                        (LOWER(a.accommodationName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                                LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
                                """)
        Page<Accommodation> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        @Query("""
                        SELECT a
                        FROM Accommodation a
                        WHERE a.isDeleted = false
                        AND (:locationId IS NULL OR a.location.locationId = :locationId)
                        AND (:type IS NULL OR a.type = :type)
                                """)
        Page<Accommodation> findByIsDeletedFalseAndLocationId(Pageable pageable, @Param("locationId") Long locationId,
                        @Param("type") AccommodationTypeEnum type);

        @Query("""
                        SELECT a
                        FROM Accommodation a
                        WHERE a.isDeleted = false
                        AND (:locationId IS NULL OR a.location.locationId = :locationId)
                        AND (:type IS NULL OR a.type = :type)
                        ORDER BY COALESCE(
                                (SELECT AVG(CAST(rt.star AS double))
                                        FROM RoomType rt
                                        WHERE rt.accommodation.accommodationId = a.accommodationId
                                        AND rt.isDeleted = false
                                        AND rt.star IS NOT NULL),
                                0.0) DESC
                                """)
        Page<Accommodation> findByLocationIdAndTypeSortedByStar(@Param("locationId") Long locationId,
                        @Param("type") AccommodationTypeEnum type, Pageable pageable);
}
