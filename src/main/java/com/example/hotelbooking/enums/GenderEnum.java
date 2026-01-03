package com.example.hotelbooking.enums;

public enum GenderEnum {
    MALE("Nam"), // Nam
    FEMALE("Nữ"), // Nữ
    OTHER("Khác"); // Khác

    private final String displayName;

    GenderEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}