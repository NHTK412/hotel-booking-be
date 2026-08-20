package com.example.hotelbooking.controller;

import java.util.Map;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.zalopay.CreateOrderRequest;
import com.example.hotelbooking.dto.zalopay.ZaloPayResponseDTO;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.ZaloPayService;
import com.example.hotelbooking.util.ApiResponse;

/**
 * Controller xử lý các API liên quan đến thanh toán ZaloPay
 */
@RestController
@RequestMapping("/zalopay")
public class ZaloPayController {

    private final ZaloPayService zaloPayService;

    public ZaloPayController(ZaloPayService zaloPayService) {
        this.zaloPayService = zaloPayService;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<ZaloPayResponseDTO>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateOrderRequest req) throws Exception {

        String username = userDetails.getUsername();
        System.err.println("Username in ZaloPayController: " + username);

        ZaloPayResponseDTO res = zaloPayService.createOrder(username, req);
        return ResponseEntity.ok(new ApiResponse<>(true, "Create ZaloPay order successfully", res));
    }

    @PostMapping("/callback")
    public String callback(@RequestBody Map<String, Object> cbdata) throws Exception {
        System.out.println("=== ZALOPAY CALLBACK RECEIVED ===");
        System.out.println("Callback data: " + cbdata);

        String dataStr = (String) cbdata.get("data");
        String reqMac = (String) cbdata.get("mac");

        JSONObject result = zaloPayService.handleCallback(dataStr, reqMac);
        return result.toString();
    }
}
