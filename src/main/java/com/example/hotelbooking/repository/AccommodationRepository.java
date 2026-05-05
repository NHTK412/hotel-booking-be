package com.example.hotelbooking.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.AccommodationTypeEnum;
import com.example.hotelbooking.model.Accommodations;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodations, Long> {

        Page<Accommodations> findByIsDeletedFalse(Pageable pageable);

        Page<Accommodations> findByIsDeletedFalseAndType(Pageable pageable,
                        com.example.hotelbooking.enums.AccommodationTypeEnum type);

        Page<Accommodations> findByIsDeletedFalseAndFavoritedByUsers_id(
                        Pageable pageable,
                        Long id);

        @Query("""
                        SELECT a FROM Accommodations a WHERE a.geohash LIKE :prefix%
                        """)
        List<Accommodations> findNearby(@Param("prefix") String prefix);

        @Query("""
                        SELECT a FROM Accommodations a WHERE a.geohash LIKE :prefix%  AND a.type = :type
                        """)
        List<Accommodations> findNearbyWithType(@Param("prefix") String prefix, @Param("type") String type);

        @Query("""
                        SELECT a FROM Accommodations a WHERE a.isDeleted = false AND
                        (LOWER(a.accommodationName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                                LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
                                """)
        Page<Accommodations> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        @Query("""
                        SELECT a
                        FROM Accommodations a
                        WHERE a.isDeleted = false
                        AND (:locationId IS NULL OR a.location.id = :locationId)
                        AND (:type IS NULL OR a.type = :type)
                                """)
        Page<Accommodations> findByIsDeletedFalseAndLocationId(Pageable pageable, Long locationId,
                        AccommodationTypeEnum type);

        @Query("""
                        SELECT a
                        FROM Accommodations a
                        WHERE a.isDeleted = false
                        AND a.location.id = :locationId
                        AND a.type = :type
                        ORDER BY COALESCE(
                                (SELECT AVG(CAST(rt.star AS double))
                                        FROM RoomTypes rt
                                        WHERE rt.accommodation.accommodationId = a.accommodationId
                                        AND rt.star IS NOT NULL),
                                0) ASC
                                """)
        Page<Accommodations> findByLocationIdAndTypeSortedByStar(@Param("locationId") Long locationId,
                        @Param("type") AccommodationTypeEnum type, Pageable pageable);

        List<Accommodations> findByStaffMembers_User_UserAuthProvider_ProviderUserId(String providerUserId);
}
