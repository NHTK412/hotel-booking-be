package com.example.hotelbooking.model;

import com.example.hotelbooking.enums.StatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "Rooms")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Rooms extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roomId")
    private Long roomId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "status", nullable = false)
    private StatusEnum status;

    @ManyToOne
    @JoinColumn(name = "roomtypeId", nullable = false)
    private RoomTypes roomType;

}
