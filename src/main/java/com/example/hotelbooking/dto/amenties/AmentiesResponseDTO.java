package com.example.hotelbooking.dto.amenties;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AmentiesResponseDTO {

    private Long amentiesId;

    private String amentiesName;
}
