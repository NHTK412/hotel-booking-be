package com.example.hotelbooking.enums;

public enum AccommodationTypeEnum {
    HOTEL("Khách sạn"),
    HOSTEL("Nhà nghỉ thanh niên"),
    APARTMENT("Căn hộ dịch vụ"),
    HOMESTAY("Homestay"),
    RESORT("Khu nghỉ dưỡng");

    private final String description;

    AccommodationTypeEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}