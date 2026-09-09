package com.example.hotelbooking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.user.CreateHostDTO;
import com.example.hotelbooking.dto.user.HostGroupResponseDTO;
import com.example.hotelbooking.dto.user.StaffResponseDTO;
import com.example.hotelbooking.dto.user.UserRequestDTO;
import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.UserService;
import com.example.hotelbooking.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "7. Người Dùng & Hồ Sơ (Users & Profiles)", description = "Các API xem thông tin cá nhân, cập nhật hồ sơ người dùng, quản lý nhân sự và Admin tạo tài khoản Host")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Xem thông tin người dùng theo ID (Admin / Công khai)", description = "Lấy thông tin hồ sơ cơ bản của người dùng bằng userId")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable Long userId) {
        UserResponseDTO userResponseDTO = userService.getUserById(userId);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Lấy thông tin người dùng thành công", userResponseDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xem thông tin hồ sơ cá nhân hiện tại (Đã đăng nhập)", description = "Lấy toàn bộ thông tin tài khoản của người dùng đang đăng nhập qua JWT Token")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails) {
        String providerId = customerUserDetails.getUsername();
        UserResponseDTO userResponseDTO = userService.getUserByProviderId(providerId);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Lấy thông tin hồ sơ thành công", userResponseDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cập nhật thông tin hồ sơ cá nhân (Đã đăng nhập)", description = "Chỉnh sửa tên, số điện thoại, ngày sinh, giới tính, địa chỉ của người dùng hiện tại")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateCurrentUser(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @Valid @RequestBody UserRequestDTO userRequestDTO) {
        String providerId = customerUserDetails.getUsername();
        UserResponseDTO userResponseDTO = userService.updateUserByProviderId(providerId, userRequestDTO);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Cập nhật hồ sơ thành công", userResponseDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Đăng ký tài khoản Chủ khách sạn / Lễ tân (Admin & Host)", description = "Admin có quyền cấp tài khoản Chủ khách sạn (ROLE_MANAGER) và Lễ tân (ROLE_RECEPTIONIST) cho mọi khách sạn. Chủ khách sạn có quyền cấp tài khoản Lễ tân cho khách sạn mình quản lý.")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PostMapping("/host")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerHost(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @Valid @RequestBody CreateHostDTO createHostDTO) {
        String providerId = customerUserDetails.getUsername();
        UserResponseDTO userResponseDTO = userService.registerHost(providerId, createHostDTO);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Đăng ký tài khoản thành công", userResponseDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách nhân sự theo cơ sở lưu trú (Admin & Host)", description = "Admin xem được mọi khách sạn. Host chỉ xem được khách sạn mình quản lý. Hỗ trợ lọc theo trạng thái đã nghỉ việc (isDeleted).")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @GetMapping("/accommodation/{accommodationId}")
    public ResponseEntity<ApiResponse<List<StaffResponseDTO>>> getStaffByAccommodation(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @PathVariable Long accommodationId,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted) {
        String providerId = customerUserDetails.getUsername();
        List<StaffResponseDTO> staffList = userService.getStaffByAccommodation(providerId, accommodationId, isDeleted);
        ApiResponse<List<StaffResponseDTO>> response = new ApiResponse<>(true, "Lấy danh sách nhân sự thành công", staffList);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách toàn bộ nhân sự / quản lý hệ thống (Admin & Host)", description = "Lấy danh sách nhân sự (không bao gồm khách hàng). Admin lấy toàn bộ hoặc lọc theo cơ sở, Host lấy trong phạm vi cơ sở mình quản lý. Hỗ trợ lọc theo trạng thái đã nghỉ việc (isDeleted).")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<StaffResponseDTO>>> getAllStaff(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(required = false) AccommodationStaffRoleEnum role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted) {
        String providerId = customerUserDetails.getUsername();
        List<StaffResponseDTO> staffList = userService.getAllStaff(providerId, accommodationId, role, keyword, isDeleted);
        ApiResponse<List<StaffResponseDTO>> response = new ApiResponse<>(true, "Lấy danh sách người dùng hệ thống thành công", staffList);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách Host / Nhân sự gom nhóm theo người dùng kèm danh sách đơn vị trực thuộc (Admin & Host)", 
               description = "Hiển thị danh sách người dùng với mảng các đơn vị trực thuộc (accommodations). " +
                             "Với Admin: thấy toàn bộ đơn vị trực thuộc của host/nhân sự. " +
                             "Với Host: chỉ thấy các đơn vị trực thuộc nằm trong phạm vi cơ sở mình quản lý.")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @GetMapping({"/hosts", "/hosts-grouped"})
    public ResponseEntity<ApiResponse<List<HostGroupResponseDTO>>> getHostsGrouped(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(required = false) AccommodationStaffRoleEnum role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted) {
        String providerId = customerUserDetails.getUsername();
        List<HostGroupResponseDTO> hostList = userService.getHostsGrouped(providerId, accommodationId, role, keyword, isDeleted);
        ApiResponse<List<HostGroupResponseDTO>> response = new ApiResponse<>(true, "Lấy danh sách host và đơn vị trực thuộc thành công", hostList);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Khóa hoặc Mở khóa tài khoản người dùng (Admin)", description = "Khóa hoặc kích hoạt lại tài khoản người dùng bằng cách đổi trạng thái status (ACTIVE / INACTIVE).")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserStatus(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @PathVariable Long userId,
            @RequestParam StatusEnum status) {
        String providerId = customerUserDetails.getUsername();
        UserResponseDTO userResponseDTO = userService.updateUserStatus(providerId, userId, status);
        String message = status == StatusEnum.ACTIVE ? "Mở khóa tài khoản thành công" : "Khóa tài khoản thành công";
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, message, userResponseDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Khóa hoặc Mở khóa nhân sự tại một cơ sở lưu trú (Admin & Host)", description = "Khóa hoặc mở khóa quyền làm việc của nhân sự tại một cơ sở lưu trú cụ thể (ACTIVE / INACTIVE).")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PatchMapping("/staff/{accommodationStaffId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStaffStatus(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @PathVariable Long accommodationStaffId,
            @RequestParam StatusEnum status) {
        String providerId = customerUserDetails.getUsername();
        userService.updateStaffStatus(providerId, accommodationStaffId, status);
        String message = status == StatusEnum.ACTIVE ? "Mở khóa nhân sự tại cơ sở thành công" : "Khóa nhân sự tại cơ sở thành công";
        ApiResponse<Void> response = new ApiResponse<>(true, message, null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa mềm nhân viên nghỉ việc tại cơ sở lưu trú (Admin & Host)", description = "Đánh dấu nhân viên đã nghỉ làm việc tại cơ sở lưu trú (isDeleted = true). Nhân viên sẽ mất quyền truy cập vào khách sạn này.")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @DeleteMapping("/staff/{accommodationStaffId}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @PathVariable Long accommodationStaffId) {
        String providerId = customerUserDetails.getUsername();
        userService.deleteStaff(providerId, accommodationStaffId);
        ApiResponse<Void> response = new ApiResponse<>(true, "Đánh dấu nhân viên nghỉ việc thành công", null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Khôi phục nhân viên đi làm lại tại cơ sở lưu trú (Admin & Host)", description = "Khôi phục trạng thái làm việc của nhân viên tại cơ sở lưu trú (isDeleted = false).")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PatchMapping("/staff/{accommodationStaffId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreStaff(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @PathVariable Long accommodationStaffId) {
        String providerId = customerUserDetails.getUsername();
        userService.restoreStaff(providerId, accommodationStaffId);
        ApiResponse<Void> response = new ApiResponse<>(true, "Khôi phục nhân viên đi làm lại thành công", null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa mềm người dùng (Chỉ dành cho Admin)", description = "Xóa mềm tài khoản người dùng trong hệ thống (isDeleted = true)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @PathVariable Long userId) {
        String providerId = customerUserDetails.getUsername();
        userService.deleteUser(providerId, userId);
        ApiResponse<Void> response = new ApiResponse<>(true, "Xóa người dùng thành công", null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Khôi phục người dùng đã xóa mềm (Chỉ dành cho Admin)", description = "Khôi phục tài khoản người dùng đã bị xóa mềm (isDeleted = false)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreUser(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @PathVariable Long userId) {
        String providerId = customerUserDetails.getUsername();
        userService.restoreUser(providerId, userId);
        ApiResponse<Void> response = new ApiResponse<>(true, "Khôi phục người dùng thành công", null);
        return ResponseEntity.ok(response);
    }
}
