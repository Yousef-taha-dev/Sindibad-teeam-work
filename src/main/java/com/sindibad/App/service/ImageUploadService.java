package com.sindibad.App.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sindibad.App.dto.ImageUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Service
public class ImageUploadService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * Trip and profile photos do not need more than 6000 pixels on a side.
     * The decoded ARGB buffer for the pixel cap below is about 48 MB.
     */
    static final int MAX_EDGE_PIXELS = 6_000;

    static final long MAX_TOTAL_PIXELS = 12_000_000L;

    private static final byte[] JPEG_SIGNATURE = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final byte[] RIFF_SIGNATURE = {'R', 'I', 'F', 'F'};

    private static final byte[] WEBP_SIGNATURE = {'W', 'E', 'B', 'P'};

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

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an image.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB."
            );
        }

        byte[] bytes = file.getBytes();

        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Please select an image.");
        }

        if (bytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB."
            );
        }

        String contentType = file.getContentType();

        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Only JPG, PNG and WEBP images are allowed."
            );
        }

        validateContent(bytes, contentType);

        Map<?, ?> result = cloudinary.uploader().upload(
                bytes,
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

    private void validateContent(byte[] bytes, String contentType) {

        switch (contentType) {
            case "image/jpeg" -> {
                requireSignature(bytes, JPEG_SIGNATURE);
                decodeBoundedImage(bytes, "JPEG");
            }
            case "image/png" -> {
                requireSignature(bytes, PNG_SIGNATURE);
                decodeBoundedImage(bytes, "PNG");
            }
            case "image/webp" -> validateWebpContainer(bytes);
            default -> throw invalidImage();
        }
    }

    private static void requireSignature(byte[] bytes, byte[] signature) {

        if (!matchesAt(bytes, 0, signature)) {
            throw invalidImage();
        }
    }

    /**
     * Reads width and height from the image header, rejects oversized
     * dimensions, and only then decodes the raster.
     */
    private static void decodeBoundedImage(byte[] bytes, String formatName) {

        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes))) {

            if (input == null) {
                throw invalidImage();
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReadersByFormatName(formatName);

            if (!readers.hasNext()) {
                throw invalidImage();
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input, false, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0
                        || height <= 0
                        || width > MAX_EDGE_PIXELS
                        || height > MAX_EDGE_PIXELS
                        || (long) width * (long) height > MAX_TOTAL_PIXELS) {
                    throw invalidDimensions();
                }

                BufferedImage image = reader.read(0);

                if (image == null) {
                    throw invalidImage();
                }

                image.flush();

            } finally {
                reader.dispose();
            }

        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidImage();
        }
    }

    /**
     * Structural RIFF/WEBP check only. No WebP decoder is available:
     * JDK ImageIO has no WebP reader, and the project dependencies do not
     * include one. Passing this check does not prove that the image is valid.
     */
    private static void validateWebpContainer(byte[] bytes) {

        if (!matchesAt(bytes, 0, RIFF_SIGNATURE)
                || !matchesAt(bytes, 8, WEBP_SIGNATURE)) {
            throw invalidImage();
        }

        long declaredSize = readUint32Le(bytes, 4);

        if (declaredSize != bytes.length - 8L) {
            throw invalidImage();
        }

        int offset = 12;
        boolean firstChunk = true;
        boolean sawImagePayload = false;

        while (offset != bytes.length) {

            if (offset > bytes.length || bytes.length - offset < 8) {
                throw invalidImage();
            }

            String tag = readTag(bytes, offset);
            long chunkSize = readUint32Le(bytes, offset + 4);
            long padding = (chunkSize & 1L) == 1L ? 1L : 0L;
            long payloadStart = offset + 8L;
            long next = payloadStart + chunkSize + padding;

            if (chunkSize < 0
                    || payloadStart > bytes.length
                    || next > bytes.length) {
                throw invalidImage();
            }

            if (firstChunk
                    && !"VP8 ".equals(tag)
                    && !"VP8L".equals(tag)
                    && !"VP8X".equals(tag)) {
                throw invalidImage();
            }

            firstChunk = false;

            if ("VP8 ".equals(tag)) {
                if (chunkSize < 10) {
                    throw invalidImage();
                }
                sawImagePayload = true;
            } else if ("VP8L".equals(tag)) {
                if (chunkSize < 5) {
                    throw invalidImage();
                }
                sawImagePayload = true;
            } else if ("VP8X".equals(tag)) {
                if (chunkSize < 10) {
                    throw invalidImage();
                }
            } else if ("ANMF".equals(tag) && chunkSize > 0) {
                sawImagePayload = true;
            }

            offset = (int) next;
        }

        if (!sawImagePayload) {
            throw invalidImage();
        }
    }

    private static boolean matchesAt(
            byte[] bytes,
            int offset,
            byte[] signature) {

        if (offset < 0 || bytes.length < offset + signature.length) {
            return false;
        }

        for (int index = 0; index < signature.length; index++) {
            if (bytes[offset + index] != signature[index]) {
                return false;
            }
        }

        return true;
    }

    private static long readUint32Le(byte[] bytes, int offset) {

        if (bytes.length < offset + 4) {
            throw invalidImage();
        }

        return (bytes[offset] & 0xFFL)
                | ((bytes[offset + 1] & 0xFFL) << 8)
                | ((bytes[offset + 2] & 0xFFL) << 16)
                | ((bytes[offset + 3] & 0xFFL) << 24);
    }

    private static String readTag(byte[] bytes, int offset) {

        return new String(
                bytes,
                offset,
                4,
                StandardCharsets.US_ASCII
        );
    }

    private static IllegalArgumentException invalidImage() {
        return new IllegalArgumentException(
                "Only JPG, PNG and WEBP images are allowed."
        );
    }

    private static IllegalArgumentException invalidDimensions() {
        return new IllegalArgumentException(
                "Image dimensions are not allowed."
        );
    }
}
