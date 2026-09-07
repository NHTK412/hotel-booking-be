package com.example.hotelbooking.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_location_geohash", columnList = "geoHash"),
    @Index(name = "idx_location_district_province", columnList = "districtName, provinceName")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Location extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "locationId")
    private Long locationId;

    @Column(name = "provinceName", length = 100)
    private String provinceName; // Ví dụ: Hồ Chí Minh

    @Column(name = "districtName", length = 100)
    private String districtName; // Ví dụ: Quận 1

    @Column(name = "searchVector", columnDefinition = "TEXT")
    private String searchVector; // Cột tổng hợp để search nhanh: "Quận 1, Hồ Chí Minh"

    @Column(name = "latitude", columnDefinition = "DOUBLE PRECISION")
    private Double latitude; // Tọa độ trung tâm của Quận (rất quan trọng)

    @Column(name = "longitude", columnDefinition = "DOUBLE PRECISION")
    private Double longitude; // Tọa độ trung tâm của Quận

    @Column(name = "geoHash", length = 12)
    private String geoHash; // Mã GeoHash của tọa độ trung tâm

    @OneToMany(mappedBy = "location")
    private List<Accommodation> accommodations;
}