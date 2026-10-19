package com.roshan.devopshub.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserResponse.from(user);
    }
    @Transactional(readOnly = true)
    public List<UserResponse> getAssignableUsers() {
        return userRepository
                .findByRoleInOrderByDisplayNameAsc(
                        List.of(UserRole.ADMIN, UserRole.RESPONDER)
                )
                .stream()
                .map(UserResponse::from)
                .toList();
    }
}