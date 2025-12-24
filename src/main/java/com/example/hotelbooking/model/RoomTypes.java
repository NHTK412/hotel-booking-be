package com.example.hotelbooking.model;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "RoomTypes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypes extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roomtypeId")
    private Long roomtypeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "star")
    private Integer star;

    @Column(name = "price", nullable = false)
    private Double price;

    @ManyToOne()
    @JoinColumn(name = "accommodationId", nullable = false)
    private Accommodations accommodation;

    // @ElementCollection(targetClass = RoomAmenityEnum.class)
    // @Enumerated(EnumType.STRING)
    // @CollectionTable(name = "roomAmenities", joinColumns = @JoinColumn(name =
    // "roomId"))
    // @Column(name = "amenity")
    // private List<RoomAmenityEnum> amenity;

    @ElementCollection
    @CollectionTable(name = "roomImagesPreview", joinColumns = @JoinColumn(name = "roomtypeId"))
    @Column(name = "imagePreview")
    private List<String> imagesPreview;

    @Column(name = "image")
    private String image;

    @ManyToMany
    @JoinTable(name = "roomAmenities", joinColumns = @JoinColumn(name = "roomtypeId"), inverseJoinColumns = @JoinColumn(name = "amentiesId"))
    private List<Amenties> amenities;

    @OneToMany(mappedBy = "roomType")
    private List<Rooms> rooms;

}
