package com.example.hotelbooking.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.hotelbooking.enums.GenderEnum;
import com.example.hotelbooking.enums.UserRoleEnum;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "birthday")
    private LocalDateTime birthday;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private GenderEnum gender;

    @Column(name = "address")
    private String address;

    @Column(name = "avatarUrl")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "roleUser", nullable = false)
    private UserRoleEnum role;

    @Column(name = "isActive", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<AccommodationStaff> accommodationStaffs = new ArrayList<>();

    @ManyToMany(mappedBy = "favoritedByUsers")
    private List<Accommodation> favoriteAccommodations;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAuthProvider> authProviders = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Device> devices;
}
