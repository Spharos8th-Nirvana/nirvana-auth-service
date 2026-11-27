package com.nirvana.auth.dto.signup;

import java.util.UUID;

public record SignupCommand(String email, String password, String phone, String name,
                            String countryCode, UUID verificationToken) {
    @Override
    public String toString() {
        return "SignupCommand";
    }
}
