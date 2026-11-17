package com.library.smart_internship.controller;

import com.library.smart_internship.entity.Student;
import com.library.smart_internship.service.AccountLinkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountLinkController {

    private final AccountLinkService accountLinkService;

    @PostMapping("/link")
    public ResponseEntity<AccountLinkResponse> linkExistingAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AccountLinkRequest request) {
        Student student = accountLinkService.linkExistingAccount(jwt, request.existingPassword());
        return ResponseEntity.ok(new AccountLinkResponse(
                student.getId(),
                student.getEmail(),
                student.getRole(),
                true
        ));
    }

    public record AccountLinkRequest(@NotBlank String existingPassword) {}

    public record AccountLinkResponse(Long profileId, String email, String legacyRole, boolean linked) {}
}
