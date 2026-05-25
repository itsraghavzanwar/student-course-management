package com.scm.service;

import com.scm.dto.AuthDTOs;
import com.scm.entity.*;
import com.scm.exception.ResourceAlreadyExistsException;
import com.scm.repository.*;
import com.scm.security.JwtUtils;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {

    private final UserRepository    userRepository;
    private final RoleRepository    roleRepository;
    private final PasswordEncoder   encoder;
    private final AuthenticationManager authManager;
    private final JwtUtils          jwtUtils;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder encoder,
                       AuthenticationManager authManager,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder        = encoder;
        this.authManager    = authManager;
        this.jwtUtils       = jwtUtils;
    }

    public void register(AuthDTOs.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email is already in use.");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(encoder.encode(request.getPassword()))
            .build();

        Set<String> strRoles = request.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            roles.add(getRole(Role.ERole.ROLE_STUDENT));
        } else {
            strRoles.forEach(r -> {
                if ("admin".equalsIgnoreCase(r)) {
                    roles.add(getRole(Role.ERole.ROLE_ADMIN));
                } else {
                    roles.add(getRole(Role.ERole.ROLE_STUDENT));
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);
    }

    public AuthDTOs.JwtResponse login(AuthDTOs.LoginRequest request) {
        Authentication authentication = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String jwt = jwtUtils.generateJwtToken(authentication);

        org.springframework.security.core.userdetails.User userDetails =
            (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        return AuthDTOs.JwtResponse.builder()
            .token(jwt)
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(roles)
            .build();
    }

    private Role getRole(Role.ERole name) {
        return roleRepository.findByName(name)
            .orElseThrow(() -> new RuntimeException("Role not found: " + name));
    }
}
