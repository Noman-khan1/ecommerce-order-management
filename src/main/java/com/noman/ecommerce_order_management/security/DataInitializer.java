package com.noman.ecommerce_order_management.security;

import com.noman.ecommerce_order_management.user.Role;
import com.noman.ecommerce_order_management.user.User;
import com.noman.ecommerce_order_management.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        createUserIfMissing(
                "Admin User",
                "admin@ecommerce.com",
                "admin123",
                Role.ADMIN
        );

        createUserIfMissing(
                "Customer User",
                "customer@ecommerce.com",
                "customer123",
                Role.CUSTOMER
        );

        createUserIfMissing(
                "Warehouse User",
                "warehouse@ecommerce.com",
                "warehouse123",
                Role.WAREHOUSE_STAFF
        );
    }

    private void createUserIfMissing(
            String name,
            String email,
            String password,
            Role role
    ) {

        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        userRepository.save(user);
    }
}