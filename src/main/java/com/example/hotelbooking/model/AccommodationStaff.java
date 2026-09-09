package com.example.hotelbooking.model;

import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
public class AccommodationStaff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accommodationStaffId")
    private Long accommodationStaffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "roleStaff", nullable = false)
    private AccommodationStaffRoleEnum role;

    @Column(name = "isDeleted", nullable = false)
    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "accommodationId")
    private Accommodation accommodation;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;
}
