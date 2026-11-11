package com.sindibad.App.controller;

import com.sindibad.App.dto.ProfileImageResponse;
import com.sindibad.App.service.ProfileImageService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileImageService profileImageService;

    public ProfileController(ProfileImageService profileImageService) {
        this.profileImageService = profileImageService;
    }

    // ==================== Upload / Replace Profile Image ====================

    @PostMapping(
            value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProfileImageResponse> uploadProfileImage(
            @RequestParam("image") MultipartFile image,
            Authentication authentication
    ) {

        String email = authentication.getName();

        ProfileImageResponse response =
                profileImageService.uploadProfileImage(email, image);

        return ResponseEntity.ok(response);
    }

    // ==================== Delete Profile Image ====================

    @DeleteMapping("/image")
    public ResponseEntity<Void> deleteProfileImage(
            Authentication authentication
    ) {

        String email = authentication.getName();

        profileImageService.deleteProfileImage(email);

        return ResponseEntity.noContent().build();
    }
}