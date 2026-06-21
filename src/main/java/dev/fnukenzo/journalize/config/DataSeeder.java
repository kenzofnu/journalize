package dev.fnukenzo.journalize.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import dev.fnukenzo.journalize.user.User;
import dev.fnukenzo.journalize.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User user = new User();
            user.setUsername("kenzo");
            user.setEmail("kenzo@mail.com");
            user.setPassword(passwordEncoder.encode("password123"));
            User saved = userRepository.save(user);

            log.info("Seeded default user with id = {}", saved.getId());

        } else {
            log.info("User already exists");
        }

    }

}
