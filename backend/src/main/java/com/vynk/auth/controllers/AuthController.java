package com.vynk.auth.controllers;

import com.vynk.identity.entities.Profile;
import com.vynk.identity.entities.UserAccount;
import com.vynk.identity.repositories.ProfileRepository;
import com.vynk.identity.repositories.UserAccountRepository;
import com.vynk.shared.api.ApiPaths;
import com.vynk.shared.error.ApiErrorCode;
import com.vynk.shared.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin", "administrator", "root", "system", "support", "help", "api", "www", "mail", "test", "null", "undefined");

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager,
                          UserAccountRepository userAccountRepository,
                          ProfileRepository profileRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        String username = request.username().toLowerCase();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (BadCredentialsException exception) {
            log.warn("Bad credentials for username '{}'.", username);
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Username or password incorrect");
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        httpRequest.getSession(true);
        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse httpResponse) {
        String username = request.username().toLowerCase();
        if (RESERVED_USERNAMES.contains(username)) {
            throw new ApiException(ApiErrorCode.CONFLICT, "This username is not allowed.");
        }
        if (userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(ApiErrorCode.CONFLICT, "This username is already taken.");
        }

        UserAccount account = new UserAccount(username, passwordEncoder.encode(request.password()));
        try {
            userAccountRepository.saveAndFlush(account);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw ApiException.conflict("This username is already taken.");
        }

        Profile profile = new Profile(
                account,
                request.firstName().trim(),
                request.lastName().trim(),
                request.headline().trim(),
                request.summary().trim());
        profileRepository.save(profile);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("username", username));
    }

    @PostMapping("/password")
    @Transactional
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                            Authentication authentication,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Authentication required.");
        }
        String username = authentication.getName();
        UserAccount account = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "User not found."));
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Current password is incorrect.");
        }
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(account);

        Authentication newAuth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.newPassword()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    @DeleteMapping("/account")
    @Transactional
    public ResponseEntity<?> deleteAccount(Authentication authentication,
                                           HttpServletRequest httpRequest) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Authentication required.");
        }
        String username = authentication.getName();
        UserAccount account = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "User not found."));
        profileRepository.findByUserAccountUsernameIgnoreCase(username)
                .ifPresent(profileRepository::delete);
        userAccountRepository.delete(account);
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Authentication required.");
        }
        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }

    public record LoginRequest(
            @NotBlank
            @Pattern(regexp = "^[a-z]{3,30}$")
            String username,
            @NotBlank
            String password) {
    }

    public record RegisterRequest(
            @NotBlank
            @Pattern(regexp = "^[a-z]{3,30}$")
            String username,
            @NotBlank
            @Size(min = 12, max = 255)
            String password,
            @NotBlank
            @Size(max = 100)
            String firstName,
            @NotBlank
            @Size(max = 100)
            String lastName,
            @NotBlank
            @Size(max = 200)
            String headline,
            @NotBlank
            @Size(max = 2000)
            String summary) {
    }

    public record PasswordChangeRequest(
            @NotBlank
            String currentPassword,
            @NotBlank
            @Size(min = 12, max = 255)
            String newPassword) {
    }
}
