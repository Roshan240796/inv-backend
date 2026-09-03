package com.synergy.invoicedemo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountRepository userAccountRepository;

    public AuthController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtService jwtService,
                          RefreshTokenRepository refreshTokenRepository, UserAccountRepository userAccountRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(userDetails);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(new SecureRandom().generateSeed(32));
        UserAccount user = userAccountRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        refreshTokenRepository.save(new RefreshToken(hash(refreshToken), Instant.now().plusSeconds(604800), user));
        return new AuthResponse(token, userDetails.getUsername(), refreshToken, jwtService.getExpirationMs());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(request.refreshToken()))
            .filter(RefreshToken::isUsable).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired"));
        stored.revoke();
        UserDetails user = stored.getUser();
        String access = jwtService.generateToken(user);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(new SecureRandom().generateSeed(32));
        refreshTokenRepository.save(new RefreshToken(hash(refresh), Instant.now().plusSeconds(604800), stored.getUser()));
        return new AuthResponse(access, user.getUsername(), refresh, jwtService.getExpirationMs());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(hash(request.refreshToken())).ifPresent(token -> { token.revoke(); refreshTokenRepository.save(token); });
    }

    private static String hash(String value) {
        try { return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {}

    public record AuthResponse(
        String token,
        String username,
        String refreshToken,
        long expiresInMs
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}
}
