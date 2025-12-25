package com.example.hotelbooking.model;

import java.util.List;

import com.example.hotelbooking.enums.AccommodationTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Accommodations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Accommodations extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accommodationId")
    private Long accommodationId;

    @Column(name = "accommodationName", nullable = false)
    private String accommodationName;

    @Column(name = "description")
    private String description;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "image")
    private String image;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AccommodationTypeEnum type;

    @OneToMany(mappedBy = "accommodation")
    private List<RoomTypes> rooms;

    @OneToMany(mappedBy = "accommodation")
    private List<AccommodationStaff> staffMembers;

    @ManyToMany
    @JoinTable(name = "Accommodations_UserFavorites", joinColumns = @JoinColumn(name = "accommodationId"), inverseJoinColumns = @JoinColumn(name = "userId"))
    private List<Users> favoritedByUsers;
}
