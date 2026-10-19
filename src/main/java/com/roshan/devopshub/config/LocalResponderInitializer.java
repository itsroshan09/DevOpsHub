package com.roshan.devopshub.config;

import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRepository;
import com.roshan.devopshub.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;


@ConditionalOnProperty(
        name = "devopshub.local-responder.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Configuration
@Profile("local")
public class LocalResponderInitializer {

    @Bean
    CommandLineRunner initializeLocalResponder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${devopshub.local-responder.email}")
            String email,
            @Value("${devopshub.local-responder.password}")
            String password
    ) {
        return args -> {
            String normalizedEmail =
                    email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

            if (normalizedEmail.isBlank()
                    || password == null
                    || password.isBlank()) {
                throw new IllegalStateException(
                        "Local responder email and password must be configured."
                );
            }

            var existingUser =
                    userRepository.findByEmail(normalizedEmail);

            if (existingUser.isPresent()) {
                if (existingUser.get().getRole() != UserRole.RESPONDER) {
                    throw new IllegalStateException(
                            "The configured local responder email belongs "
                                    + "to a user who is not a RESPONDER. "
                                    + "Choose another email; existing roles "
                                    + "are never changed."
                    );
                }

                System.out.println(
                        "Local development RESPONDER account already exists: "
                                + normalizedEmail
                );
                return;
            }

            User responder = new User(
                    normalizedEmail,
                    "Local Development Responder",
                    UserRole.RESPONDER
            );

            responder.setPasswordHash(passwordEncoder.encode(password));
            userRepository.save(responder);

            System.out.println(
                    "Local development RESPONDER account created: "
                            + normalizedEmail
            );
        };
    }
}