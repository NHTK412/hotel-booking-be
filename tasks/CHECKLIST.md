# 📋 BẢNG CHECKLIST TIẾN ĐỘ TASK NÂNG CẤP DỰ ÁN (PROJECT ROADMAP)

> **Mục tiêu**: Nâng cấp toàn diện chất lượng Backend dự án Hotel Booking phục vụ đưa vào CV (Portfolio) ứng tuyển vị trí Backend / Java Spring Boot Developer.

---

## 📊 Tổng Quan Tiến Độ

| Phase | Số Lượng Task | Trạng Thái | Hoàn Thành |
| :--- | :---: | :---: | :---: |
| **Phase 1: Critical Bug Fixes & Logic** | 5 tasks | ✅ Hoàn thành | `5 / 5` |
| **Phase 2: Performance & Concurrency** | 3 tasks | 🔄 In Progress | `1 / 3` |
| **Phase 3: Security & Validation** | 4 tasks | ⏳ Sẵn sàng | `0 / 4` |
| **Phase 4: Testing, Docs & DevOps** | 3 tasks | 🔄 In Progress | `1 / 3` |
| **TỔNG CỘNG** | **15 tasks** | **IN PROGRESS** | **`7 / 15` (46.7%)** |

---

## 🔴 Phase 1: Sửa Các Bug Logic & Query Nghiêm Trọng (Critical Bugs)

- [x] **[BUG-01]** Sửa lỗi chính tả enum `'CANCELLED'` (2 chữ L) trong `RoomRepository` và `RoomTypeRepository`.
  - **Priority**: 🔴 `CRITICAL`
  - **Estimate**: `15 mins`
  - **Assignee**: Backend Engineer
  - **Status**: `COMPLETED` ✅
  - **Chi tiết**: Xem [BUG-01 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#bug-01-sai-ch%C3%ADnh-t%E1%BA%A3-enum-cancelled-trong-query-khi%E1%BA%BFn-ph%C3%B2ng-%C4%91%C3%A3-h%E1%BB%A7y-kh%C3%B4ng-th%E1%BB%83-%C4%91%E1%BA%B7t-l%E1%BA%A1i)

- [x] **[BUG-02]** Sửa lỗi thuộc tính `a.location.id` thành `a.location.locationId` trong `AccommodationRepository`.
  - **Priority**: 🔴 `CRITICAL`
  - **Estimate**: `10 mins`
  - **Assignee**: Backend Engineer
  - **Status**: `COMPLETED` ✅
  - **Chi tiết**: Xem [BUG-02 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#bug-02-sai-t%C3%AAn-thu%E1%BB%99c-t%C3%ADnh-jpql-alocationid-g%C3%A2y-crash-runtime)

- [x] **[BUG-03]** Mở quyền truy cập công khai (`permitAll`) cho các API tra cứu công cộng trong `SecurityConfig`.
  - **Priority**: 🔴 `HIGH`
  - **Estimate**: `15 mins`
  - **Assignee**: Backend Engineer
  - **Status**: `COMPLETED` ✅
  - **Chi tiết**: Xem [BUG-03 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#bug-03-ch%E1%BA%B7n-quy%E1%BB%81n-kh%C3%A1ch-v%C3%A3ng-lai-truy-c%E1%BA%ADp-c%C3%A1c-endpoint-c%C3%B4ng-khai-trong-securityconfig)

- [x] **[BUG-04]** Chuẩn hóa logic tính trung bình số sao đánh giá trong `ReviewService`.
  - **Priority**: 🟡 `MEDIUM`
  - **Estimate**: `20 mins`
  - **Assignee**: Backend Engineer
  - **Status**: `COMPLETED` ✅
  - **Chi tiết**: Xem [BUG-04 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#bug-04-logic-t%C3%ADnh-%C4%91i%E1%BB%83m-%C4%91%C3%A1nh-gi%C3%A1-trung-b%C3%ACnh-sai-trong-reviewservice)

- [x] **[BUG-05]** Chuẩn hóa cơ chế OAuth Login (lưu Google/Facebook `sub` thay vì `idToken` tạm thời).
  - **Priority**: 🟡 `MEDIUM`
  - **Estimate**: `30 mins`
  - **Assignee**: Backend Engineer
  - **Status**: `COMPLETED` ✅
  - **Chi tiết**: Xem [BUG-05 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#bug-05-x%E1%BB%AD-l%C3%BD-oauth-login-l%C6%B0u-t%E1%BA%A1m-idtoken-v%C3%A0o-csdl)

---

## 🟡 Phase 2: Tối Ưu Hiệu Năng & Concurrency (Performance & Architecture)

- [x] **[TASK-06]** Xử lý tranh chấp phòng khi đặt đồng thời (Race Condition / Overbooking) bằng Optimistic Lock (`@Version` + `OPTIMISTIC_FORCE_INCREMENT`).
  - **Priority**: 🔴 `HIGH`
  - **Estimate**: `1 - 2 hours`
  - **Assignee**: Backend Engineer
  - **Status**: `COMPLETED` ✅
  - **Chi tiết**: Xem [TASK-06 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-06-gi%E1%BA%A3i-quy%E1%BA%BFt-l%E1%BB%97i-race-condition-overbooking--double-booking)

- [ ] **[TASK-07]** Tối ưu hóa các API thống kê báo cáo: Chuyển từ In-Memory Stream sang Database Aggregate Query.
  - **Priority**: 🔴 `HIGH`
  - **Estimate**: `1 hour`
  - **Assignee**: Backend Engineer
  - **Status**: `TODO`
  - **Chi tiết**: Xem [TASK-07 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-07-x%C3%B3a-b%E1%BB%8F-in-memory-processing-cho-c%C3%A1c-api-th%E1%BB%91ng-k%C3%AA--b%C3%A1o-c%C3%A1o)

- [ ] **[TASK-08]** Đánh Index tối ưu hiệu năng truy vấn cho các bảng cơ sở dữ liệu.
  - **Priority**: 🟡 `MEDIUM`
  - **Estimate**: `30 mins`
  - **Assignee**: Backend Engineer
  - **Status**: `TODO`
  - **Chi tiết**: Xem [TASK-08 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-08-%C4%91%C3%A1nh-index-cho-database-indexing-strategy)

---

## 🟢 Phase 3: Hoàn Thiện Bảo Mật & Validation (Security & Data Integrity)

- [ ] **[TASK-09]** Triển khai cơ chế lưu trữ Refresh Token trên Redis và API `/auth/refresh-token`.
  - **Priority**: 🔴 `HIGH`
  - **Estimate**: `1 - 2 hours`
  - **Assignee**: Backend Engineer
  - **Status**: `TODO`
  - **Chi tiết**: Xem [TASK-09 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-09-tri%E1%BB%83n-khai-c%C6%A1-ch%E1%BA%BF-refresh-token--token-rotation)

- [ ] **[TASK-10]** Chuẩn hóa `PasswordEncoder` thành Spring Bean dùng chung.
  - **Priority**: 🟢 `LOW`
  - **Estimate**: `15 mins`
  - **Assignee**: Backend Engineer
  - **Status**: `TODO`
  - **Chi tiết**: Xem [TASK-10 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-10-chu%E1%BA%A9n-h%C3%B3a-passwordencoder-th%C3%A0nh-spring-bean)

- [ ] **[TASK-11]** Tích hợp `spring-boot-starter-validation` và validate dữ liệu DTO đầu vào.
  - **Priority**: 🟡 `MEDIUM`
  - **Estimate**: `1 hour`
  - **Assignee**: Backend Engineer
  - **Status**: `TODO`
  - **Chi tiết**: Xem [TASK-11 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-11-b%E1%BB%95-sung-validation-cho-dto-spring-boot-starter-validation)

- [ ] **[TASK-12]** Chuyển cấu hình ZaloPay và các khóa bảo mật vào `application.yml` / `@ConfigurationProperties`.
  - **Priority**: 🟢 `LOW`
  - **Estimate**: `20 mins`
  - **Assignee**: Backend Engineer
  - **Status**: `TODO`
  - **Chi tiết**: Xem [TASK-12 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-12-chuy%E1%BB%83n-c%E1%BA%A5u-h%C3%ACnh-zalopay--b%C3%AD-m%E1%BA%ADt-sang-applicationyml)

---

## 🔵 Phase 4: Tự Động Hóa, Tài Liệu & DevOps (CV Showcase)

- [x] **[TASK-13]** Cấu hình Swagger / OpenAPI 3.0 với Authorize Bearer JWT Header + Light/Dark mode switcher.
  - **Priority**: 🟡 `MEDIUM`
  - **Estimate**: `45 mins`
  - **Assignee**: Backend Engineer
  - **Status**: `COMPLETED` ✅
  - **Chi tiết**: Xem [TASK-13 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-13-c%E1%BA%A5u-h%C3%ACnh-swagger--openapi-documentation)

- [ ] **[TASK-14]** Viết Unit Test (JUnit 5 + Mockito) và Integration Test cho Booking & Auth Service.
  - **Priority**: 🔴 `HIGH`
  - **Estimate**: `2 - 3 hours`
  - **Assignee**: Backend Engineer
  - **Status**: `TODO`
  - **Chi tiết**: Xem [TASK-14 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-14-vi%E1%BA%BFt-unit-tests--integration-tests)

- [ ] **[TASK-15]** Tối ưu hóa Dockerfile (Multi-stage) và cấu hình Docker Compose (App + MySQL + Redis).
  - **Priority**: 🔴 `HIGH`
  - **Estimate**: `1 hour`
  - **Assignee**: DevOps / Backend Engineer
  - **Status**: `TODO`
  - **Chi tiết**: Xem [TASK-15 trong TASK_DETAILS.md](file:///d:/Programming_Language/Project_CV/Hotel_Booking/hotel-booking-be/tasks/TASK_DETAILS.md#task-15-container-h%C3%B3a-v%E1%BB%9Bi-docker--docker-compose)
