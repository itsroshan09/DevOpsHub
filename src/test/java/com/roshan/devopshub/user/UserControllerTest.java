
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
import java.util.List;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@ImportAutoConfiguration(
        exclude = UserDetailsServiceAutoConfiguration.class
)
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
    @Test
    void getAssignableUsersShouldReturnEligibleUsersWithoutPasswordFields()
            throws Exception {

        when(userService.getAssignableUsers())
                .thenReturn(List.of(
                        new UserResponse(
                                2L,
                                "admin@example.com",
                                "Admin User",
                                UserRole.ADMIN,
                                Instant.parse("2026-01-01T00:00:00Z")
                        ),
                        new UserResponse(
                                3L,
                                "responder@example.com",
                                "Responder User",
                                UserRole.RESPONDER,
                                Instant.parse("2026-01-02T00:00:00Z")
                        )
                ));

        mockMvc.perform(get("/api/users/assignable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].role").value("RESPONDER"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[1].passwordHash").doesNotExist());

        verify(userService).getAssignableUsers();
    }

    @Test
    void getAssignableUsersShouldReturnEmptyArrayWhenNoEligibleUsersExist()
            throws Exception {

        when(userService.getAssignableUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users/assignable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
