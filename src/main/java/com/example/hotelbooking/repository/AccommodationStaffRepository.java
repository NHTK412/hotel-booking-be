package com.example.hotelbooking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.model.AccommodationStaff;

@Repository
public interface AccommodationStaffRepository extends JpaRepository<AccommodationStaff, Long> {

    @EntityGraph(attributePaths = {"user", "accommodation"})
    List<AccommodationStaff> findByAccommodation_AccommodationId(Long accommodationId);

    @EntityGraph(attributePaths = {"user", "accommodation"})
    List<AccommodationStaff> findByAccommodation_AccommodationIdIn(List<Long> accommodationIds);

    @EntityGraph(attributePaths = {"user", "accommodation"})
    @Query("SELECT s FROM AccommodationStaff s WHERE " +
           "(:accommodationId IS NULL OR s.accommodation.accommodationId = :accommodationId) AND " +
           "(:role IS NULL OR s.role = :role) AND " +
           "(:keyword IS NULL OR LOWER(s.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR s.user.phone LIKE CONCAT('%', :keyword, '%'))")
    List<AccommodationStaff> searchStaff(
            @Param("accommodationId") Long accommodationId,
            @Param("role") AccommodationStaffRoleEnum role,
            @Param("keyword") String keyword);

    @EntityGraph(attributePaths = {"user", "accommodation"})
    @Query("SELECT s FROM AccommodationStaff s WHERE " +
           "s.accommodation.accommodationId IN :accommodationIds AND " +
           "(:accommodationId IS NULL OR s.accommodation.accommodationId = :accommodationId) AND " +
           "(:role IS NULL OR s.role = :role) AND " +
           "(:keyword IS NULL OR LOWER(s.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR s.user.phone LIKE CONCAT('%', :keyword, '%'))")
    List<AccommodationStaff> searchStaffForAccommodations(
            @Param("accommodationIds") List<Long> accommodationIds,
            @Param("accommodationId") Long accommodationId,
            @Param("role") AccommodationStaffRoleEnum role,
            @Param("keyword") String keyword);

    @EntityGraph(attributePaths = {"user", "accommodation"})
    List<AccommodationStaff> findByUser_Id(Long userId);
}
