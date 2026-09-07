# TÀI LIỆU TRÍCH XUẤT API SWAGGER & ĐẶC TẢ TÍCH HỢP FRONTEND ADMIN (ADMIN & HOST PORTAL)

> **Dành cho AI Agent / Kỹ sư Frontend**: File này chứa toàn bộ đặc tả API, TypeScript Interfaces, sơ đồ phân quyền RBAC và hướng dẫn kiến trúc giao diện quản trị (Admin & Host Dashboard) cho hệ thống **Hotel Booking**. Sử dụng tài liệu này để sinh mã nguồn giao diện nhanh chóng, chuẩn xác và đồng bộ 100% với Backend.

---

## Bảng Mục Lục (Table of Contents)
1. [Tổng Quan & Cấu Hình Môi Trường](#1-tổng-quan--cấu-hình-môi-trường)
2. [Phân Quyền & Luồng Xác Thực (Authentication & RBAC)](#2-phân-quyền--luồng-xác-thực-authentication--rbac)
3. [Chuẩn Phản Hồi & Xử Lý Lỗi (API Response Envelope)](#3-chuẩn-phản-hồi--xử-lý-lỗi-api-response-envelope)
4. [Khai Báo Kiểu Dữ Liệu TypeScript (TypeScript Definitions)](#4-khai-báo-kiểu-dữ-liệu-typescript-typescript-definitions)
5. [Danh Sách API Dành Cho ADMIN (Admin Portal APIs)](#5-danh-sách-api-dành-cho-admin-admin-portal-apis)
6. [Danh Sách API Dành Cho HOST (Host Portal APIs)](#6-danh-sách-api-dành-cho-host-host-portal-apis)
7. [API Chung (Chung cho Admin & Host)](#7-api-chung-chung-cho-admin--host)
8. [Hướng Dẫn Kiến Trúc & Cài Đặt Frontend (FE Agent Guidelines)](#8-hướng-dẫn-kiến-trúc--cài-đặt-frontend-fe-agent-guidelines)

---

## 1. Tổng Quan & Cấu Hình Môi Trường

- **Base URL**: `http://localhost:8080/api` (Môi trường Local / Docker)
- **Content-Type**: `application/json` (trừ các API Upload ảnh sử dụng `multipart/form-data`)
- **Tài khoản mặc định (Seed Account)**:
  - **Admin**: `admin@gmail.com` / `admin123`
  - **Host mẫu**: Được Admin cấp thông qua API `POST /api/users/host`

---

## 2. Phân Quyền & Luồng Xác Thực (Authentication & RBAC)

### 2.1. Các Vai Trò Trong Hệ Thống (Roles)
- `ROLE_ADMIN`: Quản trị viên toàn hệ thống.
  - Quản lý tạo mới, cập nhật, xóa khách sạn (Accommodations).
  - Khởi tạo tài khoản Chủ khách sạn / Nhân viên (Host) và gán quyền quản lý khách sạn tương ứng.
  - Xem danh sách người dùng, quản lý địa điểm toàn quốc.
- `ROLE_HOST`: Chủ khách sạn / Quản lý chỗ nghỉ.
  - Quản lý danh mục loại phòng (Room Types), giá niêm yết, chiết khấu.
  - Quản lý danh sách phòng vật lý (Physical Rooms).
  - Quản lý đơn đặt phòng (Bookings): Duyệt đơn, Check-in, Check-out, Hủy đơn.
  - Truy cập trung tâm báo cáo phân tích doanh thu thời gian thực (Real-time Analytics Dashboard: doanh thu hôm nay, tháng này, biểu đồ 12 tháng, doanh thu theo loại phòng, tỷ lệ lấp đầy).
- `ROLE_CUSTOMER`: Khách hàng cuối (Đặt phòng, thanh toán, đánh giá).

### 2.2. Cơ Chế JWT & Token Rotation
- Mọi request yêu cầu xác thực phải gửi kèm Header:
  ```http
  Authorization: Bearer <accessToken>
  ```
- **Access Token**: Có hiệu lực trong **15 phút**.
- **Refresh Token**: Có hiệu lực trong **7 ngày**, lưu trữ trên Redis.
- **Cơ chế Token Rotation**: Khi Access Token hết hạn (nhận mã HTTP `401 Unauthorized`), Frontend tự động gọi API `POST /api/auth/refresh-token` với `refreshToken` hiện tại để nhận cặp token mới. Nếu `refreshToken` không hợp lệ, chuyển hướng người dùng về trang `/login`.

---

## 3. Chuẩn Phản Hồi & Xử Lý Lỗi (API Response Envelope)

Tất cả các API đều trả về cấu trúc chuẩn `ApiResponse<T>`:

### 3.1. Phản Hồi Thành Công (HTTP 200 OK)
```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": { ... }
}
```

### 3.2. Phản Hồi Lỗi Xác Thực Dữ Liệu (HTTP 400 Bad Request)
Khi dữ liệu gửi lên không thỏa mãn Bean Validation, trường `data` sẽ chứa `fieldErrors` dạng `Map<String, String>` giúp Frontend map trực tiếp vào form input:
```json
{
  "success": false,
  "message": "Lỗi xác thực dữ liệu: Email không đúng định dạng",
  "data": {
    "email": "Email không đúng định dạng",
    "phone": "Số điện thoại không đúng định dạng Việt Nam"
  }
}
```

### 3.3. Phản Hồi Lỗi Tranh Chấp Đặt Phòng (HTTP 409 Conflict)
```json
{
  "success": false,
  "message": "Phòng này vừa được khách hàng khác hoàn tất đặt trước, vui lòng thử lại hoặc chọn phòng khác.",
  "data": null
}
```

---

## 4. Khai Báo Kiểu Dữ Liệu TypeScript (TypeScript Definitions)

Frontend Agent có thể copy trực tiếp các định nghĩa TypeScript sau vào file `src/types/api.ts`:

```typescript
// ==========================================
// Generic API Response Envelope
// ==========================================
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

// ==========================================
// Enums
// ==========================================
export enum UserRoleEnum {
  ROLE_ADMIN = 'ROLE_ADMIN',
  ROLE_CUSTOMER = 'ROLE_CUSTOMER',
  ROLE_HOST = 'ROLE_HOST',
}

export enum BookingStatusEnum {
  WAITING_FOR_PAYMENT = 'WAITING_FOR_PAYMENT',
  PENDING = 'PENDING',
  CHECKED_IN = 'CHECKED_IN',
  CHECKED_OUT = 'CHECKED_OUT',
  CANCELED = 'CANCELED',
}

export enum AccommodationTypeEnum {
  HOTEL = 'HOTEL',
  HOSTEL = 'HOSTEL',
  APARTMENT = 'APARTMENT',
  HOMESTAY = 'HOMESTAY',
  RESORT = 'RESORT',
}

export enum AmenityEnum {
  WIFI = 'WIFI',
  AIR_CONDITIONING = 'AIR_CONDITIONING',
  TV = 'TV',
  MINI_BAR = 'MINI_BAR',
  ROOM_SERVICE = 'ROOM_SERVICE',
  SWIMMING_POOL = 'SWIMMING_POOL',
  GYM = 'GYM',
  SPA = 'SPA',
  PARKING = 'PARKING',
  BREAKFAST_INCLUDED = 'BREAKFAST_INCLUDED',
}

export enum AccommodationStaffRoleEnum {
  HOST = 'HOST',
  RECEPTIONIST = 'RECEPTIONIST',
  STAFF = 'STAFF',
}

export enum GenderEnum {
  MALE = 'MALE',
  FEMALE = 'FEMALE',
  OTHER = 'OTHER',
}

// ==========================================
// Authentication DTOs
// ==========================================
export interface AuthLoginDTO {
  email: string;
  password?: string;
}

export interface RefreshTokenRequestDTO {
  refreshToken: string;
}

export interface AuthResponseDTO {
  accessToken: string;
  refreshToken: string;
  user: UserResponseDTO;
}

// ==========================================
// User & Host DTOs
// ==========================================
export interface UserResponseDTO {
  id: number;
  name: string;
  email: string;
  phone: string;
  birthday?: string;
  gender?: string;
  address?: string;
  avatarUrl?: string;
}

export interface CreateHostDTO {
  name: string;
  email: string;
  phone: string;
  accommodationId: number;
  hostRole: AccommodationStaffRoleEnum;
  birthday?: string;
  gender?: GenderEnum;
  address?: string;
  avatarUrl?: string;
}

export interface UserRequestDTO {
  name?: string;
  phone?: string;
  birthday?: string;
  gender?: GenderEnum;
  address?: string;
  avatarUrl?: string;
}

// ==========================================
// Accommodation DTOs
// ==========================================
export interface AccommodationSummaryDTO {
  accommodationId: number;
  accommodationName: string;
  description: string;
  address: string;
  city: string;
  latitude: number;
  longitude: number;
  type: AccommodationTypeEnum | string;
  image: string;
  starRating: number;
  locationId: number;
}

export interface AccommodationDetailDTO extends AccommodationSummaryDTO {
  roomTypes: RoomTypeSummaryDTO[];
  isFavorite?: boolean;
}

export interface AccommodationRequestDTO {
  accommodationName: string;
  description?: string;
  address: string;
  city?: string;
  latitude?: number;
  longitude?: number;
  image?: string;
  type: AccommodationTypeEnum;
  locationId: number;
}

// ==========================================
// Room Type & Room DTOs
// ==========================================
export interface RoomTypeSummaryDTO {
  roomTypeId: number;
  name: string;
  price: number;
  discount: number;
  image: string;
  capacity: number;
  bedroom: number;
  description?: string;
  availableRooms?: number;
}

export interface RoomTypeDetailDTO extends RoomTypeSummaryDTO {
  accommodationId: number;
  imagesPreview: string[];
  amenities: AmenityEnum[];
  rooms: RoomSummaryDTO[];
}

export interface RoomTypeRequestDTO {
  name: string;
  price: number;
  discount: number;
  accommodationId: number;
  capacity: number;
  bedroom: number;
  description?: string;
  image?: string;
  imagesPreview?: string[];
  amenities?: AmenityEnum[];
}

export interface RoomSummaryDTO {
  roomId: number;
  roomNumber: string;
  isAvailable?: boolean;
}

export interface RoomRequestDTO {
  roomNumbers: string[];
}

// ==========================================
// Booking DTOs
// ==========================================
export interface BookingSummaryDTO {
  bookingId: number;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  status: BookingStatusEnum | string;
  finalPrice: number;
  checkInAt: string;
  checkOutAt: string;
}

export interface BookingDetailDTO {
  bookingId: number;
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  checkInAt: string;
  checkOutAt: string;
  originalPrice: number;
  discountedPrice: number;
  finalPrice: number;
  status: BookingStatusEnum | string;
  accommodationName: string;
  roomType: string;
  roomNumber: string;
  lat?: number;
  lng?: number;
  reviewId?: number;
}

// ==========================================
// Analytics & Statistics DTOs
// ==========================================
export interface BookingStatisticsDTO {
  totalBookings: number;
  completedBookings: number;
  canceledBookings: number;
  totalRevenue: number;
  averageBookingValue: number;
  [key: string]: any;
}

export interface MonthlyRevenueItem {
  [month: string]: number; // e.g. { "1": 15000000, "2": 18500000, ... }
}

export interface YearlyRevenueItem {
  [year: string]: number; // e.g. { "2025": 120000000, "2026": 180000000 }
}

export interface RoomTypeRevenueBreakdown {
  roomTypeId: number;
  roomTypeName: string;
  revenue: number;
  totalBookings: number;
  [key: string]: any;
}

// ==========================================
// Location DTOs
// ==========================================
export interface LocationResponseDTO {
  locationId: number;
  provinceName: string;
  districtName: string;
  latitude: number;
  longitude: number;
  searchVector?: string;
}

export interface FileUploadResponseDTO {
  url: string;
  publicId?: string;
}
```

---

## 5. Danh Sách API Dành Cho ADMIN (Admin Portal APIs)

Các API này yêu cầu Header `Authorization: Bearer <accessToken>` với tài khoản có quyền `ROLE_ADMIN`.

### 5.1. Quản Trị Khách Sạn (Accommodations Management)

| Phương Thức | Endpoint | Mô Tả | Quyền Hạn | Body / Params |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/accommodations` | Lấy danh sách toàn bộ khách sạn (có phân trang) | Public / Admin | `page`, `size`, `type`, `locationId` |
| `GET` | `/accommodations/{id}` | Xem chi tiết khách sạn | Public / Admin | Path: `id` |
| `POST` | `/accommodations` | **Tạo mới một khách sạn/chỗ nghỉ** | `ROLE_ADMIN` | Body: `AccommodationRequestDTO` |
| `PUT` | `/accommodations/{id}` | **Cập nhật thông tin khách sạn** | `ROLE_ADMIN`, `ROLE_HOST` | Path: `id`, Body: `AccommodationRequestDTO` |
| `DELETE` | `/accommodations/{id}` | **Xóa khách sạn khỏi hệ thống** | `ROLE_ADMIN`, `ROLE_HOST` | Path: `id` |

#### Payload mẫu `POST /api/accommodations`:
```json
{
  "accommodationName": "Grand Saigon Riverside Hotel",
  "description": "Khách sạn 5 sao sang trọng nhìn ra sông Sài Gòn",
  "address": "08 Đồng Khởi, Phường Bến Nghé, Quận 1",
  "city": "Hồ Chí Minh",
  "latitude": 10.7769,
  "longitude": 106.7009,
  "image": "https://res.cloudinary.com/demo/image/upload/hotel1.jpg",
  "type": "HOTEL",
  "locationId": 1
}
```

---

### 5.2. Quản Trị & Cấp Tài Khoản Host (Host Management)

| Phương Thức | Endpoint | Mô Tả | Quyền Hạn | Body / Params |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/users/host` | **Admin tạo tài khoản Host và gán vào khách sạn** | `ROLE_ADMIN` | Body: `CreateHostDTO` |
| `GET` | `/users/{id}` | Xem thông tin chi tiết người dùng theo User ID | `ROLE_ADMIN` | Path: `id` |

#### Payload mẫu `POST /api/users/host`:
```json
{
  "name": "Nguyễn Văn Quản Lý",
  "email": "manager.grandsaigon@gmail.com",
  "phone": "0909123456",
  "accommodationId": 1,
  "hostRole": "HOST",
  "gender": "MALE",
  "address": "Quận 1, TP.HCM"
}
```

---

## 6. Danh Sách API Dành Cho HOST (Host Portal APIs)

Các API này yêu cầu Header `Authorization: Bearer <accessToken>` với tài khoản có quyền `ROLE_HOST`.

### 6.1. Quản Trị Loại Phòng (Room Types Management)

| Phương Thức | Endpoint | Mô Tả | Body / Params |
| :--- | :--- | :--- | :--- |
| `GET` | `/room-types/accommodations/{accommodationId}` | Lấy danh sách loại phòng của khách sạn | Path: `accommodationId`, Query: `page`, `size` |
| `GET` | `/room-types/{roomTypeId}` | Xem chi tiết một loại phòng | Path: `roomTypeId` |
| `POST` | `/room-types` | **Tạo loại phòng mới** | Body: `RoomTypeRequestDTO` |
| `PUT` | `/room-types/{roomTypeId}` | **Cập nhật thông tin loại phòng** | Path: `roomTypeId`, Body: `RoomTypeRequestDTO` |
| `PATCH` | `/room-types/{roomTypeId}` | **Cập nhật nhanh Giá & Giảm giá** | Query: `price`, `discount` |
| `DELETE` | `/room-types/{roomTypeId}` | **Xóa loại phòng** | Path: `roomTypeId` |

---

### 6.2. Quản Trị Phòng Vật Lý (Physical Room Inventory)

| Phương Thức | Endpoint | Mô Tả | Body / Params |
| :--- | :--- | :--- | :--- |
| `GET` | `/room-types/{roomTypeId}/rooms` | Lấy danh sách số phòng thực tế của loại phòng | Path: `roomTypeId` |
| `POST` | `/room-types/{roomTypeId}/rooms` | **Thêm danh sách số phòng vật lý (101, 102...)** | Body: `{ "roomNumbers": ["101", "102", "103"] }` |
| `DELETE` | `/room-types/{roomTypeId}/rooms` | **Xóa các phòng vật lý theo Room IDs** | Body: `[1, 2, 3]` |

---

### 6.3. Quản Lý Đơn Đặt Phòng (Booking Operations)

| Phương Thức | Endpoint | Mô Tả | Body / Params |
| :--- | :--- | :--- | :--- |
| `GET` | `/bookings/accommodation/{accommodationId}` | Lấy toàn bộ đơn đặt phòng của khách sạn | Query: `page`, `size` |
| `GET` | `/bookings/host/accommodation/{accommodationId}` | **Lọc đơn đặt theo trạng thái** | Query: `status` (`PENDING`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELED`), `page`, `size` |
| `GET` | `/bookings/{bookingId}` | Xem chi tiết 1 đơn đặt phòng cụ thể | Path: `bookingId` |
| `PATCH` | `/bookings/{bookingId}/status` | **Cập nhật trạng thái đơn (Check-in, Check-out, Cancel)** | Query: `status=CHECKED_IN` |

---

### 6.4. Báo Cáo Doanh Thu & Trung Tâm Phân Tích (Analytics & Revenue Dashboard)

| Phân Loại | Endpoint | Phương Thức | Mô Tả & Ý Nghĩa Hiển Thị | Trả Về |
| :--- | :--- | :---: | :--- | :--- |
| **KPI Hôm Nay** | `/bookings/host/{id}/today-guests` | `GET` | Tổng số lượng khách đang lưu trú hôm nay | `Long` |
| **KPI Hôm Nay** | `/bookings/host/{id}/today-checkins` | `GET` | Số lượt nhận phòng (Check-in) trong ngày | `Long` |
| **KPI Hôm Nay** | `/bookings/host/{id}/today-revenue` | `GET` | Doanh thu phát sinh trong ngày hôm nay | `Double` (VNĐ) |
| **KPI Tháng Này**| `/bookings/host/{id}/month-revenue` | `GET` | Doanh thu lũy kế của tháng hiện tại | `Double` (VNĐ) |
| **Biểu Đồ Tháng**| `/bookings/host/{id}/monthly-revenue`| `GET` | Doanh thu chi tiết 12 tháng (Bar/Line Chart). Query: `year=2026` | `List<Map<String, Double>>` |
| **Biểu Đồ Năm** | `/bookings/host/{id}/yearly-revenue` | `GET` | So sánh doanh thu giữa các năm | `List<Map<String, Double>>` |
| **Lọc Khoảng Ngày**| `/bookings/host/{id}/revenue` | `GET` | Doanh thu trong khoảng thời gian tùy chọn. Query: `startDate`, `endDate` | `Double` |
| **Báo Cáo Tổng Hợp**| `/bookings/host/{id}/statistics` | `GET` | Thống kê số đơn thành công, đơn hủy, tổng thu và giá trị đơn trung bình. Query: `startDate`, `endDate` | `Map<String, Object>` |
| **Cơ Cấu Doanh Thu**| `/bookings/host/{id}/revenue-by-room-type` | `GET` | Phân tích đóng góp doanh thu của từng loại phòng (Pie Chart). Query: `startDate`, `endDate` | `List<Map<String, Object>>` |
| **Báo Cáo Số Lượng**| `/bookings/host/{id}/report/total-bookings`| `GET` | Tổng số đơn đặt trong kỳ | `Long` |
| **Báo Cáo Hủy Đơn**| `/bookings/host/{id}/report/total-canceled`| `GET` | Tổng số đơn bị hủy trong kỳ | `Long` |
| **Báo Cáo Đêm Ở** | `/bookings/host/{id}/report/total-nights` | `GET` | Tổng số đêm khách đã lưu trú | `Long` |

---

## 7. API Chung (Chung cho Admin & Host)

### 7.1. Tra Cứu Địa Điểm Hành Chính (Locations - Tỉnh / Quận / Huyện)

Các API này hỗ trợ việc hiển thị dropdown Cascader (Chọn Tỉnh/Thành ➔ Chọn Quận/Huyện) khi tạo khách sạn hoặc lọc tìm kiếm:

| Phương Thức | Endpoint | Mô Tả & Ý Nghĩa | Dữ Liệu Trả Về |
| :--- | :--- | :--- | :--- |
| `GET` | `/locations/provinces` | **Lấy danh sách tất cả các Tỉnh / Thành phố duy nhất** (Hà Nội, Hồ Chí Minh, Đà Nẵng, v.v.) | `List<String>` |
| `GET` | `/locations/districts?province={tênTỉnh}` | **Lấy danh sách Quận / Huyện thuộc Tỉnh được chọn** kèm `locationId` & tọa độ | `List<LocationResponseDTO>` |
| `GET` | `/locations/all` | **Lấy toàn bộ danh sách địa điểm** đã sắp xếp theo Tỉnh & Quận | `List<LocationResponseDTO>` |
| `GET` | `/locations/search?keyword={kw}` | Tìm kiếm địa điểm theo từ khóa nhập vào | `List<LocationResponseDTO>` |
| `GET` | `/locations/{locationId}` | Lấy chi tiết địa điểm theo ID | `LocationResponseDTO` |

---

### 7.2. Tải Ảnh Lên Cloudinary CDN (File Upload)
- `POST /api/file-upload/cdn` (`multipart/form-data`):
  - Form Data Key: `file` (Tệp ảnh)
  - Trả về: `{ "success": true, "data": { "url": "https://res.cloudinary.com/..." } }`
- `POST /api/file-upload/cdn/multiple` (`multipart/form-data`):
  - Form Data Key: `files` (Danh sách nhiều tệp ảnh)

---

## 8. Hướng Dẫn Kiến Trúc & Cài Đặt Frontend (FE Agent Guidelines)

### 8.1. Đề Xuất Công Nghệ Frontend (Tech Stack)
- **Framework**: Next.js 14+ (App Router) hoặc Vite + React 18 / 19 + TypeScript
- **Styling & UI Components**: TailwindCSS + **shadcn/ui** hoặc **Ant Design** (Rất phù hợp cho giao diện Dashboard quản trị)
- **Icons**: `lucide-react`
- **Quản lý Server State & Cache**: `@tanstack/react-query` (TanStack Query v5)
- **Form & Validation**: `react-hook-form` + `zod`
- **Vẽ Biểu Đồ Thống Kê**: `recharts`
- **Quản lý Auth State**: `zustand` lưu User Info & Tokens.

---

### 8.2. Mẫu Axios Client Tích Hợp Tự Động Refresh Token (Axios Interceptor)

```typescript
import axios from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: any) => void }> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const { data } = await axios.post(`${API_BASE_URL}/auth/refresh-token`, {
          refreshToken,
        });

        const newAccessToken = data.data.accessToken;
        const newRefreshToken = data.data.refreshToken;

        localStorage.setItem('accessToken', newAccessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        apiClient.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
        processQueue(null, newAccessToken);

        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
```
