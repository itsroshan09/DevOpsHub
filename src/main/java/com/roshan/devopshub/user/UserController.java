package com.roshan.devopshub.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());

        return userService.getUserById(userId);
    }

    @GetMapping("/assignable")
    public List<UserResponse> getAssignableUsers() {
        return userService.getAssignableUsers();
    }
}