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
  - Trong `BookingService.createBooking()`, luồng xử lý gồm 2 bước rời rạc và không có `@Transactional`:
    1. Kiểm tra phòng trống: `findRoomAvailableByRoomTypeId(...)`
    2. Tạo đơn đặt: `bookingRepository.save(booking)`
  - Khi có nhiều request gửi đồng thời cho 1 phòng duy nhất còn lại, tất cả các luồng đều thấy phòng còn trống và cùng tạo nhiều đơn đặt đè lên nhau.
- **Giải pháp (Technical Solution)**:
  - **Optimistic Locking (`@Version` + `LockModeType.OPTIMISTIC_FORCE_INCREMENT`)**:
    1. Thêm trường `@Version private Long version;` vào Entity `Room`.
    2. Trong `RoomRepository`, dùng `@Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)` khi truy vấn phòng được chọn để cưỡng chế tăng `version` khi commit.
    3. Bao bọc `createBooking` trong `@Transactional`.
    4. Bắt `ObjectOptimisticLockingFailureException` trong `GlobalExceptionHandler` và trả về `409 Conflict`.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [x] Đặt phòng đồng thời trên cùng một phòng: chỉ 1 request thành công, các request đồng thời khác nhận `409 Conflict`.
  - [x] Phương thức `createBooking` có `@Transactional` và validate ngày Check-in/Check-out.

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
  - [x] Tốc độ phản hồi các API thống kê đạt dưới `50ms`.
  - [x] Đã xóa bỏ toàn bộ các lời gọi `Pageable.unpaged()` và chuyển sang JPQL / Native SQL Aggregate Queries trực tiếp (`COUNT`, `SUM`, `GROUP BY`, `DATEDIFF`).

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
  - [x] Đã bổ sung Composite Indexes và Single-column Indexes trên toàn bộ 10 JPA Entities (`Bookings`, `Accommodations`, `RoomTypes`, `Rooms`, `UserAuthProvider`, `Reviews`, `Locations`, `Payments`, `Users`, `Devices`) bao phủ các câu query tìm kiếm, filter theo status/isDeleted/dates, foreign key joins, và auth lookups.
  - [x] Đã kiểm thử schema generation và test suite thành công (`BUILD SUCCESS`).

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
  - [x] Gửi request `POST /auth/refresh-token` với token hợp lệ trả về access token mới và refresh token mới (Token Rotation).
  - [x] Gửi token giả mạo, token hết hạn hoặc token đã bị thu hồi trả về lỗi `401 Unauthorized` (`InvalidCredentialsException`).
  - [x] Đã viết trọn bộ 5 Unit Test tự động cho `AuthServiceTest` (`testLogin_Success_StoresRefreshTokenInRedis`, `testRefreshToken_Success_RotatesTokens`, `testRefreshToken_InvalidToken_ThrowsException`, `testRefreshToken_InactiveUser_ThrowsExceptionAndCleansUp`, `testLogout_Success_RemovesTokensFromRedis`) và pass 100%.

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
  - [x] Đã định nghĩa `@Bean public PasswordEncoder passwordEncoder()` trong `SecurityConfig.java`.
  - [x] Đã thay thế toàn bộ các lời gọi `new BCryptPasswordEncoder()` thủ công trong `AuthService`, `UserService`, `DataInitializer` bằng cơ chế Dependency Injection chuẩn của Spring Boot.
  - [x] Toàn bộ test suite chạy thành công không có lỗi (`BUILD SUCCESS`).

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
  - [x] Đã tích hợp `spring-boot-starter-validation` vào `pom.xml`.
  - [x] Đã gắn Bean Validation annotations (`@NotBlank`, `@Email`, `@Pattern`, `@Size`, `@Positive`, `@NotNull`, `@FutureOrPresent`, `@Future`, `@Min`, `@Max`, `@NotEmpty`) cho toàn bộ request DTOs (`AuthRegisterDTO`, `AuthLoginDTO`, `RefreshTokenRequestDTO`, `BookingRequestDTO`, `RoomTypeRequestDTO`, `AccommodationRequestDTO`, `ReviewRequestDTO`, `CreateHostDTO`, `DeviceRegistrationRequest`, `RoomRequestDTO`, `UserRequestDTO`, `CreateOrderRequest`).
  - [x] Đã thêm `@Valid` vào tất cả `@RequestBody` parameters trong các REST Controllers (`AuthController`, `BookingController`, `AccommodationController`, `RoomTypeController`, `ReviewController`, `UserController`, `DeviceController`, `ZaloPayController`).
  - [x] Đã bắt ngoại lệ `MethodArgumentNotValidException` và `ConstraintViolationException` tập trung trong `GlobalExceptionHandler`, trả về mã `400 Bad Request` kèm chi tiết lỗi từng trường cụ thể.
  - [x] Đã viết Unit Test `DtoValidationTest` với 19 test cases phủ toàn bộ DTO validation constraints và pass 100% (`BUILD SUCCESS`).

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
  - [x] Đã tạo `ZaloPayProperties.java` sử dụng `@ConfigurationProperties(prefix = "zalopay")` để ánh xạ tự động toàn bộ thuộc tính (`appId`, `key1`, `key2`, `endpoint`, `callbackUrl`, `redirectUrl`).
  - [x] Đã cấu hình các biến `zalopay.*` trong `application.properties`, `application-prod.properties`, và `application.properties.example` hỗ trợ nạp từ biến môi trường (`${ZALOPAY_APP_ID:2553}`, `${ZALOPAY_KEY1:...}`, etc.).
  - [x] Đã xóa bỏ class tĩnh `ZaloPayConfig.java` và chuyển đổi `ZaloPayService.java` sang sử dụng Dependency Injection với `ZaloPayProperties`, không còn bất kỳ secret key nào bị hardcode trực tiếp trong mã nguồn Java.
  - [x] Đã viết Unit Test `ZaloPayPropertiesTest.java` xác thực binding thành công và toàn bộ 29 unit tests đều PASS (`BUILD SUCCESS`).

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
  - [x] Truy cập `http://localhost:8080/api/swagger-ui.html` hiển thị đầy đủ danh sách API, kèm Bearer JWT authorize button và theme switcher (Light / Dark mode).

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
  - [x] Đã hoàn thành bộ Unit Test đầy đủ cho `AuthServiceTest.java` (13 test cases) và `BookingServiceTest.java` (9 test cases) kiểm thử toàn diện các luồng nghiệp vụ xác thực, đăng ký, đăng nhập, token rotation, đặt phòng, tính tiền, kiểm tra ngày, và kiểm soát quyền truy cập.
  - [x] Đã hoàn thành bộ Integration Test `AuthControllerIntegrationTest.java` (6 test cases) và `BookingControllerIntegrationTest.java` (3 test cases) sử dụng `MockMvc` kiểm thử toàn bộ tầng HTTP Controller, Bean Validation và Exception Handling qua `GlobalExceptionHandler`.
  - [x] Chạy lệnh `.\mvnw.cmd test` đạt **100% pass với 57/57 tests thành công (`BUILD SUCCESS`)**.

---

### [TASK-15] Container hóa với Docker & Docker Compose
- **Mức độ**: 🔴 High
- **Giải pháp (Technical Solution)**:
  - **`Dockerfile` Multi-Stage**:
    - Stage 1: Build source code bằng `eclipse-temurin:21-jdk-alpine`.
    - Stage 2: Chạy ứng dụng bằng `eclipse-temurin:21-jre-alpine` với non-root user `spring:spring` và container-aware JVM flags (`MaxRAMPercentage=75.0`).
  - **`docker-compose.yml`**:
    - Service `backend`: Ứng dụng Spring Boot kết nối MySQL & Redis với điều kiện `service_healthy`.
    - Service `mysql`: Cơ sở dữ liệu MySQL 8.0 kèm volume lưu trữ bền vững và healthcheck tự động.
    - Service `redis`: Redis 7 Alpine cache kèm volume lưu trữ bền vững và healthcheck `redis-cli ping`.
    - Khởi tạo network `hotelbooking-network` và volume tự động.
    - File mẫu `.env.example` cấu hình đầy đủ biến môi trường.
- **Tiêu chí nghiệm thu (Acceptance Criteria)**:
  - [x] Đã tối ưu hóa `Dockerfile` đa tầng (Multi-stage build) với base image Alpine siêu nhẹ, tạo non-root user `spring:spring` bảo mật và cấu hình cờ JVM container-friendly.
  - [x] Đã cấu hình hoàn chỉnh `docker-compose.yml` liên kết 3 services (`mysql`, `redis`, `backend`), healthcheck phụ thuộc và mạng bridge biệt lập.
  - [x] Đã tạo file mẫu `.env.example` chuẩn hóa toàn bộ biến môi trường cho việc deploy môi trường Staging / Production.

