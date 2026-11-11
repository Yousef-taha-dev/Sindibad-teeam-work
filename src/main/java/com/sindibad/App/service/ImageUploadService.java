package com.sindibad.App.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sindibad.App.dto.ImageUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class ImageUploadService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public ImageUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public ImageUploadResult uploadImage(
            MultipartFile file,
            String folder
    ) throws IOException {

        // Check if file exists
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an image.");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB."
            );
        }

        // Check image type
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "Only JPG, PNG and WEBP images are allowed."
            );
        }

        // Upload image to Cloudinary
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image"
                )
        );

        String secureUrl = (String) result.get("secure_url");
        String publicId = (String) result.get("public_id");

        if (secureUrl == null || publicId == null) {
            throw new IllegalStateException(
                    "Cloudinary did not return the image details."
            );
        }

        return new ImageUploadResult(secureUrl, publicId);
    }

    public void deleteImage(String publicId) throws IOException {

        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("Public ID is required.");
        }

        cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.emptyMap()
        );
    }
}