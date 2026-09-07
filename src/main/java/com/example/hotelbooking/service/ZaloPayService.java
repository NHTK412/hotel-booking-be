package com.example.hotelbooking.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.config.ZaloPayProperties;
import com.example.hotelbooking.dto.zalopay.CreateOrderRequest;
import com.example.hotelbooking.dto.zalopay.ZaloPayResponseDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.enums.PaymentStatusEnum;
import com.example.hotelbooking.enums.ProviderEnum;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.Payment;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.PaymentRepository;
import com.example.hotelbooking.util.crypto.HMACUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic nghiệp vụ tích hợp thanh toán ZaloPay
 */
@Service
@RequiredArgsConstructor
public class ZaloPayService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final MailService mailService;
    private final ZaloPayProperties zaloPayProperties;

    private String getAppTransId(Long bookingId) {
        String date = new SimpleDateFormat("yyMMdd").format(new Date());
        int rand = new Random().nextInt(1000000);
        return date + "_" + rand + "_" + bookingId;
    }

    @Transactional
    public ZaloPayResponseDTO createOrder(String userId, CreateOrderRequest req) throws Exception {

        System.err.println("BOOKING ID: " + req.getBookingId());

        Booking b = bookingRepository.findById((req.getBookingId()))
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (b.getStatus() != BookingStatusEnum.WAITING_FOR_PAYMENT) {
            throw new ConflictException("Only bookings with status WAITING_FOR_PAYMENT can be paid via ZaloPay");
        }

        String appTransId = getAppTransId(req.getBookingId());

        Payment p = new Payment();
        p.setBooking(b);
        p.setProvider(ProviderEnum.ZALOPAY);
        p.setAmount(b.getFinalPrice());
        p.setStatus(PaymentStatusEnum.PENDING);
        p.setProviderTransId(appTransId);
        paymentRepository.save(p);

        Map<String, Object> order = new HashMap<>();
        order.put("app_id", zaloPayProperties.getAppId());
        order.put("app_trans_id", appTransId);
        order.put("app_user", userId);
        order.put("amount", b.getFinalPrice().longValue());
        order.put("app_time", System.currentTimeMillis());
        order.put("bank_code", "zalopayapp");
        order.put("description", req.getDescription());
        order.put("item", "[]");

        JSONObject embedData = new JSONObject();
        embedData.put("redirecturl", zaloPayProperties.getRedirectUrl());
        order.put("embed_data", embedData.toString());
        order.put("callback_url", zaloPayProperties.getCallbackUrl());

        String data = order.get("app_id") + "|" +
                order.get("app_trans_id") + "|" +
                order.get("app_user") + "|" +
                order.get("amount") + "|" +
                order.get("app_time") + "|" +
                order.get("embed_data") + "|" +
                order.get("item");

        String mac = HMACUtil.HMacHexStringEncode(
                HMACUtil.HMACSHA256,
                zaloPayProperties.getKey1(),
                data);

        order.put("mac", mac);

        System.err.println("MAC: " + mac);

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(zaloPayProperties.getEndpoint());

        List<NameValuePair> params = new ArrayList<>();
        for (Map.Entry<String, Object> entry : order.entrySet()) {
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
        }

        post.setEntity(new UrlEncodedFormEntity(params));

        CloseableHttpResponse response = client.execute(post);
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        StringBuilder resultJsonStr = new StringBuilder();
        String line;

        while ((line = rd.readLine()) != null) {
            resultJsonStr.append(line);
        }

        ObjectMapper mapper = new ObjectMapper();
        ZaloPayResponseDTO zaloPayResponse = mapper.readValue(resultJsonStr.toString(), ZaloPayResponseDTO.class);

        System.err.println("ZaloPay Response: " + zaloPayResponse.getOrderUrl());

        return zaloPayResponse;
    }

    @Transactional
    public JSONObject handleCallback(String dataStr, String reqMac) throws Exception {
        JSONObject result = new JSONObject();

        try {
            String mac = HMACUtil.HMacHexStringEncode(
                    HMACUtil.HMACSHA256,
                    zaloPayProperties.getKey2(),
                    dataStr);

            if (!mac.equals(reqMac)) {
                result.put("return_code", -1);
                result.put("return_message", "mac not equal");
                return result;
            }

            JSONObject data = new JSONObject(dataStr);
            String appTransId = data.getString("app_trans_id");
            Long amount = data.getLong("amount");
            String appUser = data.getString("app_user");
            Long zapTransId = data.getLong("zp_trans_id");

            Long bookingId = Long.parseLong(appTransId.split("_")[2]);

            System.err.println("appTransId: " + appTransId);
            System.err.println("bookingId: " + bookingId);

            Payment payment = paymentRepository.findByProviderAndProviderTransId(ProviderEnum.ZALOPAY, appTransId)
                    .orElseThrow(() -> new NotFoundException("Payment not found"));

            payment.setStatus(PaymentStatusEnum.SUCCESS);
            payment.setProviderTransId(appTransId);
            payment.setRawCallbackData(dataStr);
            paymentRepository.save(payment);

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new NotFoundException("Booking not found"));

            booking.setStatus(BookingStatusEnum.PENDING);
            bookingRepository.save(booking);

            System.out.println("Thanh toán thành công:");
            System.out.println("- Mã GD: " + appTransId);
            System.out.println("Booking ID: " + bookingId);
            System.out.println("- Số tiền: " + amount + " VNĐ");
            System.out.println("- User: " + appUser);
            System.out.println("- ZaloPay Trans ID: " + zapTransId);

            result.put("return_code", 1);
            result.put("return_message", "success");

            Booking b = bookingRepository.findById((bookingId))
                    .orElseThrow(() -> new NotFoundException("Order not found"));

            String emailBody = "Xin cảm ơn bạn đã đặt phòng tại khách sạn của chúng tôi.\n"
                    + "Đơn hàng của bạn đã được tạo thành công và đã thanh toán qua ZaloPay.\n\n"
                    + "Chi tiết đơn hàng:\n"
                    + "Mã giao dịch: " + appTransId + "\n"
                    + "Số tiền: " + b.getFinalPrice() + " VNĐ\n"
                    + "Mô tả: Thanh toán hóa đơn\n\n"
                    + "Khách sạn: " + b.getRoom().getRoomType().getAccommodation().getAccommodationName() + "\n"
                    + "Địa chỉ: " + b.getRoom().getRoomType().getAccommodation().getAddress() + "\n"
                    + "Loại phòng: " + b.getRoom().getRoomType().getName() + "\n"
                    + "Phòng: " + b.getRoom().getName() + "\n"
                    + "Bạn có thể xem chi tiết đơn hàng trên app.\n"
                    + "Trân trọng,\n"
                    + "Khách sạn của chúng tôi.";

            final String email = b.getCustomerEmail();

            if (email != null && !email.isEmpty()) {
                try {
                    mailService.sendEmail(email, "Thông báo đơn hàng ZaloPay", emailBody);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("return_code", -1);
            result.put("return_message", e.getMessage());
        }

        return result;
    }
}
