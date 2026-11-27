package com.nirvana.auth.controller;

import com.nirvana.auth.dto.ApiResponse;

record SignupErrorResponse(boolean success, Void data, ApiResponse.ApiError error) {
}
