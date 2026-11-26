package com.nirvana.auth.dto.signup.email;

import java.util.UUID;

public record VerifySignupEmailResponse(UUID verificationToken) {
}
