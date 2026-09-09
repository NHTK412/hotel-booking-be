package com.example.hotelbooking.dto.user;

import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.AccommodationTypeEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin đơn vị lưu trú trực thuộc của nhân sự / host")
public class StaffAccommodationAssignmentDTO {

    @Schema(description = "ID bản ghi phân công nhân sự (dùng để xóa mềm / khôi phục nhân viên tại đơn vị này)", example = "101")
    private Long accommodationStaffId;

    @Schema(description = "ID cơ sở lưu trú", example = "5")
    private Long accommodationId;

    @Schema(description = "Tên cơ sở lưu trú", example = "Vinpearl Luxury Nha Trang")
    private String accommodationName;

    @Schema(description = "Loại hình cơ sở lưu trú", example = "RESORT")
    private AccommodationTypeEnum hotelType;

    @Schema(description = "Vai trò tại cơ sở lưu trú này (ROLE_MANAGER / ROLE_RECEPTIONIST)", example = "ROLE_MANAGER")
    private AccommodationStaffRoleEnum role;

    @Schema(description = "Trạng thái phân công tại cơ sở này (ACTIVE: đang làm việc, INACTIVE: bị khóa tại cơ sở)", example = "ACTIVE")
    private com.example.hotelbooking.enums.StatusEnum status;

    @Schema(description = "Trạng thái nhân sự tại đơn vị này (true: đã nghỉ việc, false: đang làm việc)", example = "false")
    private Boolean isDeleted;
}
