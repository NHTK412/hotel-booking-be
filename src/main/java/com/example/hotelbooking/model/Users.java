package com.example.hotelbooking.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.hotelbooking.enums.GenderEnum;
import com.example.hotelbooking.enums.UserRoleEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class Users extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "password", nullable = false)
    private String password;

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
    @Column(name = "role", nullable = false)
    private UserRoleEnum role;

    @Column(name = "isActive", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "user")
    private List<AccommodationStaff> accommodationStaffs;

}
