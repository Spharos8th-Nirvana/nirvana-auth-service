package com.nirvana.auth.dto.signup.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendSignupEmailRequest(@NotBlank @Email @Size(max = 255) String email) {
}
