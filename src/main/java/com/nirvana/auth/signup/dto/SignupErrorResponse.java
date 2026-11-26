package com.nirvana.auth.signup.dto;

import com.nirvana.auth.common.web.ApiResponse;

public record SignupErrorResponse(boolean success, Void data, ApiResponse.ApiError error) {
}
