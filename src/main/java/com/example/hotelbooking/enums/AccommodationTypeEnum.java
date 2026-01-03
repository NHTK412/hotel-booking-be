package com.example.hotelbooking.enums;

public enum AccommodationTypeEnum {
    HOTEL("Khách sạn"), // Khách sạn
    HOSTEL("Nhà trọ"), // Nhà trọ
    APARTMENT("Căn hộ"), // Căn hộ
    HOMESTAY("Nhà ở"), // Nhà ở
    RESORT("Khu nghỉ dưỡng"); // Khu nghỉ dưỡng

    private final String description;

    AccommodationTypeEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}