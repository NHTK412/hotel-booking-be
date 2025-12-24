package com.example.hotelbooking.model;

import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AccommodationStaff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationStaff extends Base {

    @Id
    @Column(name = "accommodationStaffId")
    private Long accommodationStaffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private AccommodationStaffRoleEnum role;

    @ManyToOne
    @JoinColumn(name = "accommodationId")
    private Accommodations accommodation;

    @ManyToOne
    @JoinColumn(name = "userId")
    private Users user;
}
