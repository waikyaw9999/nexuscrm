package com.nexuscrm.domain.auth;

import com.nexuscrm.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.TokenResponse register(@Valid @RequestBody RegisterBody body) {
        return authService.register(new AuthService.RegisterRequest(
                body.email(), body.password(), body.fullName(), body.organizationName()));
    }

    @PostMapping("/login")
    public AuthService.TokenResponse login(@Valid @RequestBody LoginBody body) {
        return authService.login(new AuthService.LoginRequest(body.email(), body.password(), body.tenantSlug()));
    }

    @PostMapping("/refresh")
    public AuthService.TokenResponse refresh(@Valid @RequestBody RefreshBody body) {
        return authService.refresh(body.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) RefreshBody body) {
        if (body != null) {
            authService.logout(body.refreshToken());
        }
    }

    @GetMapping("/me")
    public AuthService.MeResponse me() {
        return authService.me(TenantContext.userId(), TenantContext.requireTenantId());
    }

    public record RegisterBody(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String fullName,
            @NotBlank String organizationName) {}

    public record LoginBody(
            @Email @NotBlank String email,
            @NotBlank String password,
            String tenantSlug) {}

    public record RefreshBody(@NotBlank String refreshToken) {}
}
