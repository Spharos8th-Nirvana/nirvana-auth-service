package com.nirvana.auth.signup.email.dto;

import java.util.UUID;

public record VerifySignupEmailResponse(UUID verificationToken) {
}
