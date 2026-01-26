# Hotel Booking Application

Ứng dụng đặt phòng khách sạn được xây dựng với Spring Boot, Firebase, Redis, và các công nghệ web hiện đại khác.

## Tính Năng Chính

- Quản lý thông tin khách sạn và phòng
- Xác thực và quản lý người dùng
- Tích hợp thanh toán ZaloPay
- Bảo mật với Firebase
- Caching với Redis
- API Documentation với Swagger
- Đánh giá và nhận xét phòng

## Yêu Cầu Hệ Thống

- Java 21+
- Maven 3.6+
- MySQL/PostgreSQL
- Redis
- Firebase Account

## Cài Đặt Và Chạy

### 1. Clone Repository
```bash
git clone https://github.com/NHTK412/hotel-booking-be
cd hotel-booking-be
```

### 2. Cấu Hình Biến Môi Trường
Sao chép file `application.properties.example` thành `application.properties` và cập nhật các giá trị:

```bash
# Linux/Mac
cp src/main/resources/application.properties.example src/main/resources/application.properties

# Windows
copy src\main\resources\application.properties.example src\main\resources\application.properties
```

Các biến cần cấu hình:
- Kết nối cơ sở dữ liệu (URL, tên người dùng, mật khẩu)
- Cấu hình Firebase (API key, project ID)
- Cấu hình Redis (host, port)
- Thông tin đăng nhập ZaloPay API
- JWT secret key

### 3. Tải Firebase Admin SDK
Đặt file `booking-hotel-app-43981-firebase-adminsdk-fbsvc-c91a1a3701.json` trong thư mục root của project

### 4. Chạy Ứng Dụng

#### Tùy chọn 1: Cài đặt dependencies và chạy
```bash
mvn install
mvn spring-boot:run
```

#### Tùy chọn 2: Build JAR và chạy
```bash
mvn clean package
java -jar target/hotelbooking-0.0.1-SNAPSHOT.jar
```

## Cấu Trúc Dự Án

```
src/main/java/com/example/hotelbooking/
├── config/              # Cấu hình (Firebase, Redis, Security, Swagger)
├── controller/          # Các endpoint API
├── service/             # Logic nghiệp vụ
├── repository/          # Truy cập cơ sở dữ liệu
├── model/               # Các model entity
├── dto/                 # Các Data Transfer Objects
├── util/                # Các lớp tiện ích
├── exception/           # Các ngoại lệ tùy chỉnh
├── filter/              # Các filter HTTP
├── security/            # Cấu hình bảo mật
├── crypto/              # Mã hóa/Giải mã
├── enums/               # Các enum
└── specification/       # Các JPA Specifications
```

## API Documentation

Tài liệu API tương tác có sẵn tại:
```
http://localhost:8080/api/swagger-ui/index.html
```

## Công Nghệ Sử Dụng

- **Framework**: Spring Boot
- **Database**: JPA/Hibernate
- **Security**: Spring Security, JWT, Firebase
- **Caching**: Redis
- **Payment**: ZaloPay API
- **API Doc**: Springfox Swagger
- **Build**: Maven

## Cấu Hình Port

Ứng dụng mặc định chạy trên port `8080`. Để thay đổi, cập nhật trong `application.properties`:

```properties
server.port=8080
```

## Các Endpoint Chính

- `/api/auth/**` - Xác thực và quản lý người dùng
- `/api/accommodations/**` - Quản lý thông tin khách sạn
- `/api/rooms/**` - Quản lý thông tin phòng
- `/api/bookings/**` - Quản lý đặt phòng
- `/api/payments/**` - Xử lý thanh toán
- `/api/reviews/**` - Quản lý đánh giá và nhận xét

## Troubleshooting

### Lỗi kết nối Redis
- Kiểm tra Redis đang chạy
- Xác minh host và port trong `application.properties`

### Lỗi Firebase
- Kiểm tra file JSON credentials có tồn tại
- Xác minh Firebase project ID

### Lỗi kết nối cơ sở dữ liệu
- Kiểm tra service cơ sở dữ liệu đang chạy
- Xác minh URL, username, password trong `application.properties`
- Chạy migration script tạo database nếu cần thiết
