# 📖 TÀI LIỆU MÔ TẢ CHI TIẾT CÁC TASK (TASK SPECIFICATIONS)

> Tài liệu này mô tả chi tiết nguyên nhân (Root Cause), tác động (Impact), giải pháp kỹ thuật (Implementation Plan), và tiêu chí nghiệm thu (Acceptance Criteria) cho từng task.

---

## 🔴 PHASE 1: SỬA CÁC BUG LOGIC NGHIÊM TRỌNG

### [BUG-01] Sai chính tả Enum `'CANCELLED'` trong Query khiến phòng đã hủy không thể đặt lại
- **Mức độ**: 🔴 Critical
- **Vị trí**:
  - `src/main/java/com/example/hotelbooking/repository/RoomRepository.java` (Dòng 28)
  - `src/main/java/com/example/hotelbooking/repository/RoomTypeRepository.java` (Dòng 31)
- **Nguyên nhân gốc (Root Cause)**:
  - Query JPQL đang kiểm tra chuỗi cứng `AND b.status != 'CANCELLED'` (2 chữ `L`).
  - Trong khi đó, enum trong `com.example.hotelbooking.enums.BookingStatusEnum` được định nghĩa là `CANCELED` (1 chữ `L`).
- **Hậu quả (Impact)**:
  - Biểu thức `b.status != 'CANCELLED'` luôn luôn trả về `true` đối với tất cả các đơn đặt phòng (kể cả những đơn có `status = CANCELED`).
  - Hệ thống hiểu nhầm các đơn đã hủy vẫn đang giữ phòng, làm cho các phòng này không bao giờ xuất hiện lại trong kết quả tìm kiếm và không thể đặt lại được.
- **Giải pháp (Implementation)**:
  - Đổi điều kiện JPQL từ `b.status != 'CANCELLED'` thành:
    ```sql
    b.status != com.example.hotelbooking.enums.BookingStatusEnum.CANCELED
    ```
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Khách hàng hủy đặt phòng -> Trạng thái phòng chuyển sang khả dụng trong khung thời gian đó.
  - [ ] API `GET /room-types/search` và `findRoomAvailableByRoomTypeId` trả về đúng phòng đã hủy cho khách khác đặt.

---

### [BUG-02] Sai tên thuộc tính JPQL `a.location.id` gây Crash Runtime
- **Mức độ**: 🔴 Critical
- **Vị trí**: `src/main/java/com/example/hotelbooking/repository/AccommodationRepository.java` (Dòng 45, 55)
- **Nguyên nhân gốc (Root Cause)**:
  - Trong `AccommodationRepository`, câu query JPQL viết:
    ```sql
    WHERE (:locationId IS NULL OR a.location.id = :locationId)
    ```
  - Trong thực thể `Location.java`, trường khóa chính được đặt tên là `locationId`, không phải `id`.
- **Hậu quả (Impact)**:
  - Khi người dùng lọc danh sách khách sạn theo địa điểm hoặc theo số sao, Hibernate ném `PropertyReferenceException: No property 'id' found for type 'Location'`, làm API trả về `500 Server Error`.
- **Giải pháp (Implementation)**:
  - Sửa `a.location.id` thành `a.location.locationId`.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Gọi `GET /accommodations?locationId=1` hoạt động trơn tru, trả về danh sách khách sạn đúng địa điểm mà không bị crash.

---

### [BUG-03] Chặn quyền khách vãng lai truy cập các Endpoint công khai trong `SecurityConfig`
- **Mức độ**: 🔴 High
- **Vị trí**: `src/main/java/com/example/hotelbooking/config/SecurityConfig.java` (Dòng 61-71)
- **Nguyên nhân gốc (Root Cause)**:
  - `SecurityConfig` đang cấu hình `.anyRequest().authenticated()` và chỉ bỏ qua xác thực cho một số ít URL (`/auth/**`, `/images/**`, `/zalopay/callback`).
  - Các API xem thông tin khách sạn (`/accommodations/search`), xem chi tiết loại phòng (`/room-types/**`), tìm kiếm địa điểm (`/locations/search`, `/locations/me`), xem đánh giá (`GET /reviews`) không nằm trong danh sách `permitAll()`.
- **Hậu quả (Impact)**:
  - Khách vãng lai (chưa đăng nhập) vào website/app tìm kiếm khách sạn sẽ bị chặn ngay từ đầu với mã lỗi `401 Unauthorized`.
- **Giải pháp (Implementation)**:
  - Bổ sung cấu hình `requestMatchers` với phương thức cụ thể:
    ```java
    .requestMatchers(
        "/auth/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/images/**",
        "/locations/**",
        "/room-types/**",
        "/accommodations/search",
        "/zalopay/callback"
    ).permitAll()
    .requestMatchers(HttpMethod.GET, "/accommodations/**", "/reviews/**").permitAll()
    ```
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Không gửi header `Authorization: Bearer <token>` vẫn gọi được API tìm kiếm khách sạn, xem chi tiết phòng, xem reviews.

---

### [BUG-04] Logic tính điểm đánh giá trung bình sai trong `ReviewService`
- **Mức độ**: 🟡 Medium
- **Vị trí**: `src/main/java/com/example/hotelbooking/service/ReviewService.java` (Dòng 73-80)
- **Nguyên nhân gốc (Root Cause)**:
  - Công thức tính trung bình cộng đang dựa vào `roomType.getReviews().size()` và `(currentReviewCount - 1)`. Trong phiên làm việc JPA, `roomType.getReviews()` có thể chưa cập nhật bản ghi mới lưu hoặc nếu chỉ có 1 review thì phép chia gặp sai số.
  - Trường `star` trong `RoomType` là `Integer` khiến điểm số bị làm tròn thô bạo (ví dụ: đánh giá 4.6 sẽ bị ép thành 5).
- **Giải pháp (Implementation)**:
  - Viết câu truy vấn trực tiếp trong `ReviewRepository`:
    ```java
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.roomType.roomtypeId = :roomTypeId")
    Double calculateAverageRatingByRoomTypeId(@Param("roomTypeId") Long roomTypeId);
    ```
  - Cập nhật trường `star` (hoặc `rating` kiểu `Double`) chính xác.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Sau khi thêm đánh giá, điểm sao của `RoomType` được cập nhật chính xác theo trung bình cộng của tất cả đánh giá thực tế trong DB.

---

### [BUG-05] Xử lý OAuth Login lưu tạm `idToken` vào CSDL
- **Mức độ**: 🟡 Medium
- **Vị trí**: `src/main/java/com/example/hotelbooking/service/AuthService.java` (Dòng 90-105)
- **Nguyên nhân gốc (Root Cause)**:
  - `userAuthProvider.setProviderUserId(oauthLoginDTO.getIdToken())`: `idToken` là chuỗi JWT tạm thời của Google/Facebook và sẽ thay đổi sau mỗi lần đăng nhập.
- **Hậu quả (Impact)**:
  - Người dùng đăng nhập lần thứ 2 với Google sẽ có `idToken` mới -> hệ thống tìm không thấy `providerUserId` cũ và tự động sinh ra một User trùng lặp mới.
- **Giải pháp (Implementation)**:
  - Yêu cầu DTO truyền `sub` (Google Subject ID / Facebook User ID cố định) hoặc giải mã JWT verify ID Token từ Google Auth Library để lấy `payload.getSubject()`.
  - Cập nhật đúng email và tên từ Google Profile nếu có.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Đăng nhập Google nhiều lần trên cùng một tài khoản chỉ map vào đúng 1 bản ghi `User` duy nhất.

---

## 🟡 PHASE 2: TỐI ƯU HIỆU NĂNG & CONCURRENCY

### [TASK-06] Giải quyết lỗi Race Condition (Overbooking / Double Booking)
- **Mức độ**: 🔴 High (Điểm cộng lớn khi phỏng vấn)
- **Vấn đề (Problem)**:
  - Trong `BookingService.createBooking()`, luồng xử lý gồm 2 bước rời rạc:
    1. Kiểm tra phòng trống: `findRoomAvailableByRoomTypeId(...)`
    2. Tạo đơn đặt: `bookingRepository.save(booking)`
  - Khi có 2 request gửi đồng thời (High Concurrency) cho 1 phòng duy nhất còn lại, cả 2 luồng đều thấy phòng còn trống và cùng tạo 2 đơn đặt đè lên nhau.
- **Giải pháp (Technical Solution)**:
  - **Cách 1 (Database Pessimistic Lock)**: Sử dụng `@Lock(LockModeType.PESSIMISTIC_WRITE)` khi truy vấn `Room` để khóa bản ghi phòng trong suốt transaction tạo đơn:
    ```java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.roomId = :roomId")
    Optional<Room> findByIdWithLock(@Param("roomId") Long roomId);
    ```
  - **Cách 2 (Distributed Lock với Redis)**: Khóa theo `roomTypeId + checkInDate + checkOutDate` bằng Redisson / Redis Template trước khi cho phép vào logic tạo booking.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Viết kịch bản test đồng thời (Concurrency Test) với 10 threads cùng đặt 1 phòng: Chỉ duy nhất 1 thread thành công, 9 threads còn lại nhận thông báo "Phòng đã hết".

---

### [TASK-07] Xóa bỏ In-Memory Processing cho các API Thống Kê & Báo Cáo
- **Mức độ**: 🔴 High
- **Vấn đề (Problem)**:
  - Các hàm trong `BookingService` (`getTodayGuests`, `getTodayRevenue`, `getMonthRevenue`, `getYearlyRevenue`, `getTotalNightsInDateRange`) đang lấy toàn bộ danh sách `List<Booking>` của khách sạn bằng `Pageable.unpaged()` vào bộ nhớ RAM rồi chạy Stream filter.
  - Khi dữ liệu lên tới hàng chục nghìn bookings, server sẽ bị nghẽn CPU và tràn bộ nhớ RAM (OutOfMemoryError).
- **Giải pháp (Technical Solution)**:
  - Viết các câu truy vấn Aggregation trực tiếp trong `BookingRepository`:
    ```java
    @Query("""
        SELECT COALESCE(SUM(b.finalPrice), 0.0)
        FROM Booking b
        WHERE b.room.roomType.accommodation.accommodationId = :accommodationId
          AND b.status = 'CHECKED_OUT'
          AND b.checkOutAt >= :start AND b.checkOutAt < :end
    """)
    Double calculateRevenueBetween(
        @Param("accommodationId") Long accommodationId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);
    ```
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Tốc độ phản hồi các API thống kê đạt dưới `50ms`.
  - [ ] Không còn bất kỳ câu gọi `Pageable.unpaged()` nào để kéo toàn bộ entity về xử lý bằng Java Stream.

---

### [TASK-08] Đánh Index cho Database (Indexing Strategy)
- **Mức độ**: 🟡 Medium
- **Giải pháp (Technical Solution)**:
  - Bổ sung Index trên các cột tìm kiếm và lọc thường xuyên:
    ```java
    // Booking.java
    @Table(name = "Bookings", indexes = {
        @Index(name = "idx_booking_status", columnList = "status"),
        @Index(name = "idx_booking_dates", columnList = "checkInAt, checkOutAt"),
        @Index(name = "idx_booking_user", columnList = "userId"),
        @Index(name = "idx_booking_room", columnList = "roomId")
    })
    
    // Accommodation.java
    @Table(name = "Accommodations", indexes = {
        @Index(name = "idx_accommodation_geohash", columnList = "geohash"),
        @Index(name = "idx_accommodation_location", columnList = "locationId, isDeleted")
    })
    ```
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Khi chạy `EXPLAIN` câu lệnh query tìm kiếm, MySQL/PostgreSQL sử dụng Index thay vì Full Table Scan (`ALL`).

---

## 🟢 PHASE 3: HOÀN THIỆN BẢO MẬT & VALIDATION

### [TASK-09] Triển khai Cơ chế Refresh Token & Token Rotation
- **Mức độ**: 🔴 High
- **Vấn đề (Problem)**:
  - Khi Access Token hết hạn (ví dụ sau 15-60 phút), hệ thống chưa có API cấp lại Access Token qua Refresh Token, khiến người dùng bị buộc phải đăng nhập lại liên tục.
- **Giải pháp (Technical Solution)**:
  - Khi login/register: Tạo cặp `accessToken` (ngắn hạn: 15 phút) và `refreshToken` (dài hạn: 7 ngày).
  - Lưu Refresh Token vào **Redis** với TTL:
    ```java
    redisTemplate.opsForValue().set("RT::" + userId, refreshToken, 7, TimeUnit.DAYS);
    ```
  - Tạo endpoint `POST /auth/refresh-token`:
    - Nhận `refreshToken`, kiểm tra tính hợp lệ và đối chiếu với Redis.
    - Tạo `accessToken` mới (kèm cơ chế **Token Rotation** cấp luôn `refreshToken` mới và hủy token cũ).
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Gửi request `POST /auth/refresh-token` với token hợp lệ trả về access token mới.
  - [ ] Gửi token giả mạo hoặc token đã bị thu hồi trả về lỗi `401/403`.

---

### [TASK-10] Chuẩn hóa `PasswordEncoder` thành Spring Bean
- **Mức độ**: 🟢 Low
- **Giải pháp (Technical Solution)**:
  - Trong `SecurityConfig.java`, định nghĩa `@Bean`:
    ```java
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    ```
  - Thay thế toàn bộ `new BCryptPasswordEncoder()` trong `AuthService`, `UserService`, `DataInitializer` bằng Dependency Injection.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Khởi động ứng dụng không có cảnh báo khởi tạo thủ công, mã hóa mật khẩu diễn ra đồng nhất.

---

### [TASK-11] Bổ sung Validation cho DTO (`spring-boot-starter-validation`)
- **Mức độ**: 🟡 Medium
- **Giải pháp (Technical Solution)**:
  - Thêm dependency vào `pom.xml`:
    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    ```
  - Gắn annotation kiểm tra dữ liệu vào các DTO:
    - `AuthRegisterDTO`: `@NotBlank`, `@Email`, `@Size(min = 6)`
    - `BookingRequestDTO`: `@NotNull`, `@FutureOrPresent`, kiểm tra `checkOutDate` > `checkInDate`
    - `RoomTypeRequestDTO`: `@Positive(price)`, `@Min(1, capacity)`
  - Thêm `@Valid` vào tất cả các Controller (`@RequestBody @Valid ...`).
  - Cấu hình bắt lỗi `MethodArgumentNotValidException` trong `GlobalExceptionHandler` trả về danh sách field bị lỗi cụ thể.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Gửi body không hợp lệ (ví dụ email sai định dạng) trả về mã `400 Bad Request` với message rõ ràng thay vì lỗi `500`.

---

### [TASK-12] Chuyển Cấu Hình ZaloPay & Bí Mật sang `application.yml`
- **Mức độ**: 🟢 Low
- **Giải pháp (Technical Solution)**:
  - Tạo `ZaloPayProperties.java` với `@ConfigurationProperties(prefix = "zalopay")`:
    ```java
    @Getter
    @Setter
    @Configuration
    @ConfigurationProperties(prefix = "zalopay")
    public class ZaloPayProperties {
        private String appId;
        private String key1;
        private String key2;
        private String endpoint;
        private String callbackUrl;
    }
    ```
  - Cấu hình trong `application.yml` kèm giá trị mặc định hoặc đọc từ biến môi trường (`${ZALOPAY_APP_ID:2553}`).
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Không còn bất kỳ secret key nào bị hardcode trực tiếp trong mã nguồn Java.

---

## 🔵 PHASE 4: TỰ ĐỘNG HÓA, TÀI LIỆU & DEVOPS

### [TASK-13] Cấu hình Swagger / OpenAPI Documentation
- **Mức độ**: 🟡 Medium
- **Giải pháp (Technical Solution)**:
  - Tạo `OpenApiConfig.java` cấu hình OpenAPI Spec 3.0 với Bearer Authentication:
    ```java
    @Configuration
    @OpenAPIDefinition(
        info = @Info(title = "Hotel Booking API", version = "1.0", description = "REST API for Hotel Booking System"),
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
    )
    public class OpenApiConfig {}
    ```
  - Gắn `@Tag` vào các Controller để phân nhóm API rõ ràng trên UI.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Truy cập `http://localhost:8080/swagger-ui/index.html` hiển thị đầy đủ danh sách API, cho phép nhập JWT token và test trực tiếp trên giao diện.

---

### [TASK-14] Viết Unit Tests & Integration Tests
- **Mức độ**: 🔴 High (Yếu tố quyết định trong CV Backend)
- **Giải pháp (Technical Solution)**:
  - **Unit Test**: Viết `AuthServiceTest.java`, `BookingServiceTest.java` sử dụng **JUnit 5 + Mockito** test các kịch bản:
    - Đăng ký thành công / thất bại khi trùng email.
    - Đăng nhập đúng / sai mật khẩu.
    - Đặt phòng thành công / thất bại khi hết phòng / tính đúng số tiền.
  - **Integration Test**: Viết `AuthControllerIntegrationTest.java` sử dụng `MockMvc` kiểm thử toàn bộ luồng HTTP Request -> Controller -> Response.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Chạy `mvn test` đạt 100% pass với độ phủ (Code Coverage) các service chính đạt trên 80%.

---

### [TASK-15] Container hóa với Docker & Docker Compose
- **Mức độ**: 🔴 High
- **Giải pháp (Technical Solution)**:
  - **`Dockerfile` Multi-Stage**:
    - Stage 1: Build source code bằng `eclipse-temurin:21-jdk-alpine`.
    - Stage 2: Chạy ứng dụng bằng `eclipse-temurin:21-jre-alpine` (dung lượng image < 200MB).
  - **`docker-compose.yml`**:
    - Service `backend`: Ứng dụng Spring Boot.
    - Service `mysql`: Cơ sở dữ liệu MySQL 8.0.
    - Service `redis`: Redis cache.
    - Khởi tạo network và volume tự động.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [ ] Chỉ cần chạy lệnh `docker compose up -d`, toàn bộ hệ thống (App + DB + Redis) tự động dựng lên và kết nối thành công.
