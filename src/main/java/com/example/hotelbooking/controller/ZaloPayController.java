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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "10. Cổng Thanh Toán ZaloPay (Payments)", description = "Các API tạo đơn hàng thanh toán qua ZaloPay Gateway và tiếp nhận Webhook Callback IPN từ ZaloPay")
@RestController
@RequestMapping("/zalopay")
public class ZaloPayController {

    private final ZaloPayService zaloPayService;

    public ZaloPayController(ZaloPayService zaloPayService) {
        this.zaloPayService = zaloPayService;
    }

    @Operation(summary = "Tạo đơn hàng thanh toán ZaloPay (Khách hàng)", description = "Khởi tạo giao dịch thanh toán ZaloPay cho đơn đặt phòng và trả về đường link/app_trans_id để mở ứng dụng ZaloPay thanh toán")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<ZaloPayResponseDTO>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateOrderRequest req) throws Exception {

        String username = userDetails.getUsername();
        ZaloPayResponseDTO res = zaloPayService.createOrder(username, req);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tạo đơn hàng ZaloPay thành công", res));
    }

    @Operation(summary = "Tiếp nhận Webhook Callback từ ZaloPay (Công khai / Server-to-Server)", description = "Endpoint tiếp nhận thông báo kết quả thanh toán từ máy chủ ZaloPay (IPN), kiểm tra mã MAC chữ ký số và cập nhật trạng thái đơn đặt phòng sang PENDING/CHECKED_IN")
    @PostMapping("/callback")
    public String callback(@RequestBody Map<String, Object> cbdata) throws Exception {
        String dataStr = (String) cbdata.get("data");
        String reqMac = (String) cbdata.get("mac");

        JSONObject result = zaloPayService.handleCallback(dataStr, reqMac);
        return result.toString();
    }
}
