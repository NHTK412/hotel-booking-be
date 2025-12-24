package com.example.hotelbooking.model;

import java.util.List;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Amenties")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Amenties extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AmentiesId")
    private Long amentiesId;

    @Column(name = "AmentiesName", nullable = false)
    private String amentiesName;

    @ManyToMany(mappedBy = "amenities")
    private List<RoomTypes> rooms;
}
