package com.nirvana.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginIdAvailabilityRequest(
        @NotBlank @Size(max = 50) String loginId
) {
}
