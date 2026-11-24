package com.nirvana.auth.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.nirvana.auth.service.UserAccountService;

class SignupControllerTest {

    private static final String PATH = "/signup/login-id-availability";

    private final UserAccountService accounts = mock(UserAccountService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SignupController(accounts)).build();

    @Test
    void returnsAvailabilityForNewAndExistingLoginIds() throws Exception {
        when(accounts.isLoginIdAvailable("new-user")).thenReturn(true);
        when(accounts.isLoginIdAvailable("existing-user")).thenReturn(false);

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\"new-user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\"existing-user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void rejectsBlankOrOverlongLoginIdsBeforeCallingTheService() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + "a".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accounts);
    }
}
