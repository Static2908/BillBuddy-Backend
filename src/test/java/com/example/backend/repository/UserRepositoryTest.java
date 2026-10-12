package com.example.backend.repository;

import com.example.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindByEmail() {
        User user = new User("Amit Sharma", "amit@example.com", "$2a$10$abcdefghijklmnopqrstuvwxyzABCDE123456");
        userRepository.save(user);

        Optional<User> foundUser = userRepository.findByEmail("amit@example.com");

        assertTrue(foundUser.isPresent());
        assertTrue(userRepository.existsByEmail("amit@example.com"));
    }
}
