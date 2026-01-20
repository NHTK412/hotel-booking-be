package com.example.hotelbooking.dto.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZaloCallbackRequest {
    private String data;
    private String mac;
}
