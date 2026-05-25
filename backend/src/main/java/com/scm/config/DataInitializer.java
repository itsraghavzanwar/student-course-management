package com.scm.config;

import com.scm.entity.Role;
import com.scm.entity.User;
import com.scm.repository.RoleRepository;
import com.scm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedData(UserRepository userRepo,
                               RoleRepository roleRepo,
                               PasswordEncoder encoder) {
        return args -> {
            if (roleRepo.findByName(Role.ERole.ROLE_ADMIN).isEmpty()) {
                roleRepo.save(new Role(null, Role.ERole.ROLE_ADMIN));
            }
            if (roleRepo.findByName(Role.ERole.ROLE_STUDENT).isEmpty()) {
                roleRepo.save(new Role(null, Role.ERole.ROLE_STUDENT));
            }

            if (!userRepo.existsByUsername("admin")) {
                Role adminRole = roleRepo.findByName(Role.ERole.ROLE_ADMIN).orElseThrow();
                User admin = User.builder()
                    .username("admin")
                    .email("admin@edutrack.com")
                    .password(encoder.encode("admin123"))
                    .roles(Set.of(adminRole))
                    .build();
                userRepo.save(admin);
                log.info("Default admin created — username: admin / password: admin123");
            }
        };
    }
}
