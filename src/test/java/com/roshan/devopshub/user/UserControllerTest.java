
package com.roshan.devopshub.user;

import com.roshan.devopshub.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getCurrentUserShouldReturnProfileWithoutPasswordFields()
            throws Exception {

        when(userService.getUserById(1L))
                .thenReturn(new UserResponse(
                        1L,
                        "test@example.com",
                        "Test User",
                        UserRole.VIEWER,
                        Instant.parse("2026-01-01T00:00:00Z")
                ));

        mockMvc.perform(get("/api/users/me")
                        .principal(
                                new UsernamePasswordAuthenticationToken("1", null)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getCurrentUserShouldReturn404WhenUserIsMissing()
            throws Exception {

        when(userService.getUserById(99L))
                .thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/me")
                        .principal(
                                new UsernamePasswordAuthenticationToken("99", null)
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("User not found"));
    }
}
