package com.example.backend.service;

import com.example.backend.dto.AuthDtos;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiExceptions.ConflictException;
import com.example.backend.repository.BillLineItemRepository;
import com.example.backend.repository.BillRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final BillRepository billRepository;
    private final BillLineItemRepository billLineItemRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            BillRepository billRepository,
            BillLineItemRepository billLineItemRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.billRepository = billRepository;
        this.billLineItemRepository = billLineItemRepository;
    }

    @Transactional
    public AuthDtos.UserResponse register(AuthDtos.RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password())
        );
        return toResponse(userRepository.save(user));
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        UserDetails details = userDetailsService.loadUserByUsername(email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user no longer exists"
                ));

        return new AuthDtos.AuthResponse(jwtService.generateToken(details), toResponse(user));
    }

    @Transactional
    public AuthDtos.ProfileUpdateResponse updateProfile(
            String authenticatedEmail,
            AuthDtos.UpdateProfileRequest request
    ) {
        String currentEmail = normalizeEmail(authenticatedEmail);
        String requestedEmail = normalizeEmail(request.email());

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user no longer exists"
                ));

        if (!currentEmail.equals(requestedEmail)
                && userRepository.existsByEmail(requestedEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(requestedEmail);
        User savedUser = userRepository.save(user);

        // JWT subject is the username/email, so issue a token for the new email.
        UserDetails updatedDetails =
                userDetailsService.loadUserByUsername(savedUser.getEmail());
        String refreshedToken = jwtService.generateToken(updatedDetails);

        return new AuthDtos.ProfileUpdateResponse(
                refreshedToken,
                toResponse(savedUser)
        );
    }

    @Transactional
    public void changePassword(
            String email,
            AuthDtos.ChangePasswordRequest request
    ) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user no longer exists"
                ));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(
            String email,
            AuthDtos.DeleteAccountRequest request
    ) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user no longer exists"
                ));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        Long userId = user.getId();
        billLineItemRepository.deleteByBillUserId(userId);
        billRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    public AuthDtos.UserResponse toResponse(User user) {
        return new AuthDtos.UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
