package com.sindibad.App.service;

import com.sindibad.App.dto.ImageUploadResult;
import com.sindibad.App.dto.ProfileImageResponse;
import com.sindibad.App.entity.Person;
import com.sindibad.App.repository.PersonRepository;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ProfileImageService {

    private static final Logger log =
            LoggerFactory.getLogger(ProfileImageService.class);

    private final PersonRepository personRepository;
    private final ImageUploadService imageUploadService;

    public ProfileImageService(
            PersonRepository personRepository,
            ImageUploadService imageUploadService
    ) {
        this.personRepository = personRepository;
        this.imageUploadService = imageUploadService;
    }

    // ==================== Upload / Replace Profile Image ====================

    @Transactional
    public ProfileImageResponse uploadProfileImage(
            String email,
            MultipartFile image
    ) {

        Person person = personRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );

        // Keep the old Cloudinary public ID
        String oldPublicId = person.getProfileImagePublicId();

        // Upload new image
        ImageUploadResult uploaded;

        try {
            uploaded = imageUploadService.uploadImage(image, "profiles");

        } catch (IOException exception) {
            log.error("Failed to upload profile image for {}", email, exception);

            throw new RuntimeException(
                    "Failed to upload profile image.",
                    exception
            );
        }

        try {

            // Update user profile
            person.setProfileImage(uploaded.secureUrl());
            person.setProfileImagePublicId(uploaded.publicId());

            Person savedPerson = personRepository.saveAndFlush(person);

            // Handle Cloudinary operations after transaction completion
            registerImageSynchronization(
                    uploaded.publicId(),
                    oldPublicId
            );

            return new ProfileImageResponse(
                    savedPerson.getProfileImage()
            );

        } catch (RuntimeException exception) {

            // If database update fails, remove the newly uploaded image
            deleteCloudinaryImage(uploaded.publicId());

            throw exception;
        }
    }

    // ==================== Delete Profile Image ====================

    @Transactional
    public void deleteProfileImage(String email) {

        Person person = personRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );

        String oldPublicId = person.getProfileImagePublicId();

        if (oldPublicId == null || oldPublicId.isBlank()) {
            throw new IllegalArgumentException(
                    "You do not have a profile image."
            );
        }

        // Remove image information from database
        person.setProfileImage(null);
        person.setProfileImagePublicId(null);

        personRepository.saveAndFlush(person);

        // Delete Cloudinary image only after database commit
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        deleteCloudinaryImage(oldPublicId);
                    }
                }
        );
    }

    // ==================== Transaction Synchronization ====================

    private void registerImageSynchronization(
            String newPublicId,
            String oldPublicId
    ) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        // Delete old image after successful database commit
                        if (oldPublicId != null
                                && !oldPublicId.isBlank()
                                && !oldPublicId.equals(newPublicId)) {

                            deleteCloudinaryImage(oldPublicId);
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {

                        // If transaction rolls back, remove the new image
                        if (status != STATUS_COMMITTED) {
                            deleteCloudinaryImage(newPublicId);
                        }
                    }
                }
        );
    }

    // ==================== Cloudinary Delete Helper ====================

    private void deleteCloudinaryImage(String publicId) {

        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            imageUploadService.deleteImage(publicId);

        } catch (Exception exception) {
            log.error(
                    "Failed to delete Cloudinary image: {}",
                    publicId,
                    exception
            );
        }
    }
}