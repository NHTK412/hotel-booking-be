package com.example.hotelbooking.service;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.config.ZaloPayConfig;
import com.example.hotelbooking.crypto.HMACUtil;
import com.example.hotelbooking.dto.zalopay.CreateOrderRequest;
import com.example.hotelbooking.dto.zalopay.ZaloPayResponseDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.enums.PaymentStatusEnum;
import com.example.hotelbooking.enums.ProviderEnum;
// import com.example.hotelbooking.enums.OrderStatusEnum;
import com.example.hotelbooking.exception.customer.ConflictException;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.Bookings;
import com.example.hotelbooking.model.Payment;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.PaymentRepository;
// import com.example.hotelbooking.model.Order;
// import com.example.hotelbooking.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service xử lý logic nghiệp vụ tích hợp thanh toán ZaloPay
 * Class này chịu trách nhiệm giao tiếp với ZaloPay API để tạo đơn hàng thanh
 * toán
 */
@Service // Đánh dấu đây là một Spring Service bean
public class ZaloPayService {

    // @Autowired
    // private OrderRepository orderRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MailService mailService;

    /**
     * Tạo mã giao dịch duy nhất (app_trans_id)
     * Format: YYMMDD_XXXXXX (Ví dụ: 251202_123456)
     * 
     * @return Mã giao dịch duy nhất theo định dạng ngày + số ngẫu nhiên
     */
    private String getAppTransId(Long bookingId) {
        // Lấy ngày hiện tại định dạng YYMMDD (ví dụ: 251202 cho ngày 02/12/2025)
        String date = new SimpleDateFormat("yyMMdd").format(new Date());
        // Tạo số ngẫu nhiên từ 0-999999 để đảm bảo tính duy nhất
        int rand = new Random().nextInt(1000000);
        // Ghép ngày và số ngẫu nhiên với dấu gạch dưới
        return date + "_" + rand + "_" + bookingId;
    }

    /**
     * Tạo đơn hàng thanh toán trên hệ thống ZaloPay
     * 
     * Luồng xử lý:
     * 1. Chuẩn bị dữ liệu đơn hàng (app_id, amount, description,...)
     * 2. Tạo chữ ký MAC để bảo mật dữ liệu
     * 3. Gửi request tới ZaloPay API
     * 4. Nhận và trả về kết quả
     * 
     * @param req - Thông tin đơn hàng từ client
     * @return JSONObject chứa kết quả từ ZaloPay (return_code, return_message,
     *         order_url,...)
     * @throws Exception nếu có lỗi khi gọi API ZaloPay
     */
    // public JSONObject createOrder(CreateOrderRequest req) throws Exception {
    @Transactional
    public ZaloPayResponseDTO createOrder(String userId, CreateOrderRequest req) throws Exception {

        System.err.println("BOOKING ID: " + req.getBookingId());

        // Bookings b = bookingRepository.findByBookingId(req.getBookingId())
        // .orElseThrow(() -> new NotFoundException("Order not found"));

        Bookings b = bookingRepository.findById((req.getBookingId()))
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (b.getStatus() != BookingStatusEnum.WAITING_FOR_PAYMENT) {
            throw new ConflictException("Only bookings with status WAITING_FOR_PAYMENT can be paid via ZaloPay");
        }

        String appTransId = getAppTransId(req.getBookingId());

        // New payment record
        Payment p = new Payment();
        p.setBooking(b);
        p.setProvider(ProviderEnum.ZALOPAY);
        p.setAmount(b.getFinalPrice());
        p.setStatus(PaymentStatusEnum.PENDING);
        p.setProviderTransId(appTransId);
        paymentRepository.save(p);

        // if (o.getStatus() != OrderStatusEnum.PLACED) {
        // throw new ConflictException("Only orders with status PLACED can be paid via
        // ZaloPay");
        //
        // Tạo Map chứa các tham số đơn hàng theo yêu cầu của ZaloPay
        Map<String, Object> order = new HashMap<>();
        // Tạo mã giao dịch duy nhất
        // String appTransId = getAppTransId(o.getOrderId());
        // String appTransId = getAppTransId(req.getBookingId());
        // String appTransId = o.getOrderId().toString();

        // APP_ID: ID ứng dụng được cấp bởi ZaloPay khi đăng ký (giống như API key)
        order.put("app_id", ZaloPayConfig.APP_ID);
        // app_trans_id: Mã giao dịch duy nhất do merchant tạo ra
        order.put("app_trans_id", appTransId);
        // app_user: Username/ID của người dùng đang thanh toán
        // order.put("app_user", req.getAppUser());

        // String test_user = "customer1";
        // order.put("app_user", test_user);
        order.put("app_user", userId.toString());

        // amount: Số tiền thanh toán (VNĐ)
        // order.put("amount", req.getAmount());
        // order.put("amount", o.getTotalAmount().longValue() +
        // o.getShippingFee().longValue());
        order.put("amount", b.getFinalPrice().longValue());

        // app_time: Timestamp tạo đơn hàng (milliseconds)
        order.put("app_time", System.currentTimeMillis());
        // bank_code: Phương thức thanh toán ("zalopayapp" = ví ZaloPay)
        order.put("bank_code", "zalopayapp");
        // description: Mô tả đơn hàng
        order.put("description", req.getDescription());
        // item: Danh sách sản phẩm (JSON array) - hiện tại để trống
        order.put("item", "[]");
        // embed_data: Dữ liệu bổ sung (JSON object) - có thể chứa redirect_url
        // redirect_url: URL để redirect user sau khi thanh toán (không bắt buộc)
        JSONObject embedData = new JSONObject();
        embedData.put("redirecturl", ZaloPayConfig.REDIRECT_URL);
        order.put("embed_data", embedData.toString());
        // callback_url: URL mà ZaloPay sẽ gọi khi thanh toán thành công
        // ZaloPay sẽ POST dữ liệu callback về URL này (BẮT BUỘC phải là URL public)
        order.put("callback_url", ZaloPayConfig.CALLBACK_URL);

        // Tạo chuỗi data để tính MAC (Message Authentication Code)
        // Các trường phải được nối theo đúng thứ tự và cách thức quy định của ZaloPay
        // Mục đích: Đảm bảo tính toàn vẹn dữ liệu, ZaloPay sẽ verify MAC này
        String data = order.get("app_id") + "|" +
                order.get("app_trans_id") + "|" +
                order.get("app_user") + "|" +
                order.get("amount") + "|" +
                order.get("app_time") + "|" +
                order.get("embed_data") + "|" +
                order.get("item");

        // Tính MAC sử dụng thuật toán HMAC-SHA256
        // KEY1: Khóa bí mật được ZaloPay cấp để mã hóa
        // Kết quả: Chuỗi hex đại diện cho chữ ký điện tử của dữ liệu
        String mac = HMACUtil.HMacHexStringEncode(
                HMACUtil.HMACSHA256,
                ZaloPayConfig.KEY1,
                data);

        // Thêm MAC vào dữ liệu đơn hàng
        order.put("mac", mac);

        System.err.println("MAC: " + mac);

        // Tạo HTTP client để gửi request
        CloseableHttpClient client = HttpClients.createDefault();
        // Tạo POST request tới endpoint tạo đơn hàng của ZaloPay
        HttpPost post = new HttpPost(ZaloPayConfig.CREATE_ORDER_ENDPOINT);

        // Chuyển đổi Map thành danh sách NameValuePair (form data)
        List<NameValuePair> params = new ArrayList<>();
        for (Map.Entry<String, Object> entry : order.entrySet()) {
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
        }

        // Set entity cho request dưới dạng URL-encoded form data
        post.setEntity(new UrlEncodedFormEntity(params));

        // Thực thi request và nhận response từ ZaloPay
        CloseableHttpResponse response = client.execute(post);
        // Đọc nội dung response
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        // Đọc từng dòng response và ghép lại thành chuỗi JSON
        StringBuilder resultJsonStr = new StringBuilder();

        // ZaloPayResponseDTO responseDTO = new ZaloPayResponseDTO();
        String line;

        while ((line = rd.readLine()) != null) {
            resultJsonStr.append(line);
        }

        ObjectMapper mapper = new ObjectMapper();

        ZaloPayResponseDTO zaloPayResponse = mapper.readValue(resultJsonStr.toString(), ZaloPayResponseDTO.class);

        // Tiến hàn gửi email thông báo tạo đơn hàng thành công
        String emailBody = "Xin cảm ơn bạn đã đặt phòng tại khách sạn của chúng tôi.\n"
                + "Đơn hàng của bạn đã được tạo thành công và đang đã thanh toán qua ZaloPay.\n\n"
                + "Chi tiết đơn hàng:\n"
                + "Mã giao dịch: " + appTransId + "\n"
                + "Số tiền: " + b.getFinalPrice() + " VNĐ\n"
                + "Mô tả: " + req.getDescription() + "\n\n"
                + "Khách sạn: " + b.getRoom().getRoomType().getAccommodation().getAccommodationName() + "\n"
                + "Địa chỉ: " + b.getRoom().getRoomType().getAccommodation().getAddress() + "\n"
                + "Loại phòng: " + b.getRoom().getRoomType().getName() + "\n"
                + "Phòng: " + b.getRoom().getName() + "\n"
                + "Bạn có thể xem chi tiết đơn hàng trên app.\n"
                + "Trân trọng,\n"
                + "Khách sạn của chúng tôi.";

        // final String email = b.getUser().getEmail();
        final String email = b.getCustomerEmail();

        if (email != null && !email.isEmpty()) {
            try {
                mailService.sendEmail(email, "Thông báo đơn hàng ZaloPay", emailBody);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }

        return zaloPayResponse;

        // Chuyển chuỗi JSON thành JSONObject và trả về
        // Response thường chứa: return_code (1=thành công), return_message, order_url
        // (link thanh toán)
        // return new JSONObject(resultJsonStr.toString());

        // =================================================

        // // Tạo Map chứa các tham số đơn hàng theo yêu cầu của ZaloPay
        // Map<String, Object> order = new HashMap<>();
        // // Tạo mã giao dịch duy nhất
        // String appTransId = getAppTransId();

        // // APP_ID: ID ứng dụng được cấp bởi ZaloPay khi đăng ký (giống như API key)
        // order.put("app_id", ZaloPayConfig.APP_ID);
        // // app_trans_id: Mã giao dịch duy nhất do merchant tạo ra
        // order.put("app_trans_id", appTransId);
        // // app_user: Username/ID của người dùng đang thanh toán
        // order.put("app_user", req.getAppUser());
        // // amount: Số tiền thanh toán (VNĐ)
        // order.put("amount", req.getAmount());
        // // app_time: Timestamp tạo đơn hàng (milliseconds)
        // order.put("app_time", System.currentTimeMillis());
        // // bank_code: Phương thức thanh toán ("zalopayapp" = ví ZaloPay)
        // order.put("bank_code", "zalopayapp");
        // // description: Mô tả đơn hàng
        // order.put("description", req.getDescription());
        // // item: Danh sách sản phẩm (JSON array) - hiện tại để trống
        // order.put("item", "[]");
        // // embed_data: Dữ liệu bổ sung (JSON object) - có thể chứa redirect_url
        // // redirect_url: URL để redirect user sau khi thanh toán (không bắt buộc)
        // JSONObject embedData = new JSONObject();
        // embedData.put("redirecturl", ZaloPayConfig.REDIRECT_URL);
        // order.put("embed_data", embedData.toString());
        // // callback_url: URL mà ZaloPay sẽ gọi khi thanh toán thành công
        // // ZaloPay sẽ POST dữ liệu callback về URL này (BẮT BUỘC phải là URL public)
        // order.put("callback_url", ZaloPayConfig.CALLBACK_URL);

        // // Tạo chuỗi data để tính MAC (Message Authentication Code)
        // // Các trường phải được nối theo đúng thứ tự và cách thức quy định của
        // ZaloPay
        // // Mục đích: Đảm bảo tính toàn vẹn dữ liệu, ZaloPay sẽ verify MAC này
        // String data = order.get("app_id") + "|" +
        // order.get("app_trans_id") + "|" +
        // order.get("app_user") + "|" +
        // order.get("amount") + "|" +
        // order.get("app_time") + "|" +
        // order.get("embed_data") + "|" +
        // order.get("item");

        // // Tính MAC sử dụng thuật toán HMAC-SHA256
        // // KEY1: Khóa bí mật được ZaloPay cấp để mã hóa
        // // Kết quả: Chuỗi hex đại diện cho chữ ký điện tử của dữ liệu
        // String mac = HMACUtil.HMacHexStringEncode(
        // HMACUtil.HMACSHA256,
        // ZaloPayConfig.KEY1,
        // data);

        // // Thêm MAC vào dữ liệu đơn hàng
        // order.put("mac", mac);

        // // Tạo HTTP client để gửi request
        // CloseableHttpClient client = HttpClients.createDefault();
        // // Tạo POST request tới endpoint tạo đơn hàng của ZaloPay
        // HttpPost post = new HttpPost(ZaloPayConfig.CREATE_ORDER_ENDPOINT);

        // // Chuyển đổi Map thành danh sách NameValuePair (form data)
        // List<NameValuePair> params = new ArrayList<>();
        // for (Map.Entry<String, Object> entry : order.entrySet()) {
        // params.add(new BasicNameValuePair(entry.getKey(),
        // entry.getValue().toString()));
        // }

        // // Set entity cho request dưới dạng URL-encoded form data
        // post.setEntity(new UrlEncodedFormEntity(params));

        // // Thực thi request và nhận response từ ZaloPay
        // CloseableHttpResponse response = client.execute(post);
        // // Đọc nội dung response
        // BufferedReader rd = new BufferedReader(new
        // InputStreamReader(response.getEntity().getContent()));

        // // Đọc từng dòng response và ghép lại thành chuỗi JSON
        // StringBuilder resultJsonStr = new StringBuilder();
        // String line;

        // while ((line = rd.readLine()) != null) {
        // resultJsonStr.append(line);
        // }

        // // Chuyển chuỗi JSON thành JSONObject và trả về
        // // Response thường chứa: return_code (1=thành công), return_message,
        // order_url
        // // (link thanh toán)
        // return new JSONObject(resultJsonStr.toString());

    }

    /**
     * Xử lý callback từ ZaloPay khi thanh toán thành công
     * 
     * Luồng xử lý:
     * 1. Nhận data và mac từ ZaloPay
     * 2. Verify MAC để đảm bảo callback thực sự từ ZaloPay
     * 3. Parse data để lấy thông tin giao dịch
     * 4. Cập nhật trạng thái đơn hàng trong database
     * 5. Trả về kết quả cho ZaloPay
     * 
     * @param callbackData - Dữ liệu callback từ ZaloPay
     * @return JSONObject chứa return_code và return_message
     * @throws Exception nếu có lỗi xử lý
     */
    @Transactional
    public JSONObject handleCallback(String dataStr, String reqMac) throws Exception {
        JSONObject result = new JSONObject();

        try {
            // Bước 1: Tính MAC từ data nhận được
            // Sử dụng KEY2 để verify (khác với KEY1 dùng khi tạo đơn)
            String mac = HMACUtil.HMacHexStringEncode(
                    HMACUtil.HMACSHA256,
                    ZaloPayConfig.KEY2,
                    dataStr);

            // Bước 2: So sánh MAC tính được với MAC từ ZaloPay gửi về
            if (!mac.equals(reqMac)) {
                // MAC không khớp -> callback giả mạo hoặc dữ liệu bị sửa đổi
                result.put("return_code", -1);
                result.put("return_message", "mac not equal");

                return result;
            }

            // Bước 3: Parse data để lấy thông tin giao dịch
            JSONObject data = new JSONObject(dataStr);

            // Lấy các thông tin quan trọng từ callback
            String appTransId = data.getString("app_trans_id"); // Mã giao dịch
            Long amount = data.getLong("amount"); // Số tiền
            String appUser = data.getString("app_user"); // User ID
            // Long appTime = data.getLong("app_time"); // Thời gian tạo đơn
            Long zapTransId = data.getLong("zp_trans_id"); // Mã giao dịch ZaloPay

            Long bookingId = Long.parseLong(appTransId.split("_")[2]);

            System.err.println("appTransId: " + appTransId);
            System.err.println("bookingId: " + bookingId);

            Payment payment = paymentRepository.findByProviderAndProviderTransId(ProviderEnum.ZALOPAY, appTransId)
                    .orElseThrow(() -> new NotFoundException("Payment not found"));

            payment.setStatus(PaymentStatusEnum.SUCCESS);
            payment.setProviderTransId(appTransId);
            payment.setRawCallbackData(dataStr);
            paymentRepository.save(payment);

            Bookings booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new NotFoundException("Booking not found"));

            booking.setStatus(BookingStatusEnum.PENDING);
            bookingRepository.save(booking);

            // booking.setStatus(BookingStatusEnum.PENDING);
            // bookingRepository.save(booking);

            // Order o = orderRepository.findById(orderId)
            // .orElseThrow(() -> new NotFoundException("Order not found"));

            // if (o.getStatus() != OrderStatusEnum.PLACED) {
            // throw new ConflictException("Only orders with status PLACED can be paid via
            // ZaloPay");
            // }

            // // o.setVnpayCode(zapTransId.toString());
            // o.setZaloAppTransId(appTransId);

            // orderRepository.save(o);

            // TODO: Bước 4: Cập nhật trạng thái đơn hàng trong database
            // Ví dụ:
            // - Tìm đơn hàng theo app_trans_id
            // - Cập nhật trạng thái = "PAID" hoặc "SUCCESS"
            // - Lưu zp_trans_id để đối soát sau này
            // - Gửi email/notification cho khách hàng

            System.out.println("Thanh toán thành công:");
            System.out.println("- Mã GD: " + appTransId);
            System.out.println("Booking ID" + bookingId);
            System.out.println("- Số tiền: " + amount + " VNĐ");
            System.out.println("- User: " + appUser);
            System.out.println("- ZaloPay Trans ID: " + zapTransId);

            // Bước 5: Trả về thành công cho ZaloPay
            // return_code = 1 -> ZaloPay biết callback đã được xử lý thành công
            // Nếu trả về -1 -> ZaloPay sẽ retry callback nhiều lần
            result.put("return_code", 1);
            result.put("return_message", "success");

        } catch (Exception e) {
            // Có lỗi xảy ra -> trả về -1 để ZaloPay retry
            e.printStackTrace();
            result.put("return_code", -1);
            result.put("return_message", e.getMessage());
        }

        return result;
    }
}
