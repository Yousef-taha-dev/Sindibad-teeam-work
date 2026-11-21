package com.sindibad.App.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.sindibad.App.dto.ImageUploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    private static final byte[] TEXT =
            "this is not an image".getBytes(StandardCharsets.UTF_8);

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private ImageUploadService service;

    @BeforeEach
    void setUp() {
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        service = new ImageUploadService(cloudinary);
    }

    @Test
    void validJpegIsAccepted() throws Exception {
        byte[] jpeg = raster("jpg", 2, 2);
        assertTrue(startsWith(jpeg, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
        stubUpload();

        ImageUploadResult result = service.uploadImage(
                part(jpeg, "image/jpeg"),
                "trips"
        );

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/v1/sample.jpg",
                result.secureUrl()
        );
        assertEquals("trips/sample", result.publicId());
        assertUploaded(jpeg, "trips");
    }

    @Test
    void validPngIsAccepted() throws Exception {
        byte[] png = raster("png", 2, 2);
        stubUpload();

        ImageUploadResult result = service.uploadImage(
                part(png, "image/png"),
                "profiles"
        );

        assertEquals("trips/sample", result.publicId());
        assertUploaded(png, "profiles");
    }

    @Test
    void validWebpContainerIsAcceptedWithoutPixelDecoding() throws Exception {
        // RIFF/WEBP container with a VP8 chunk header. This is not a
        // fully decoded WebP image; the service only checks the container.
        byte[] webp = webpContainer(new byte[10]);
        stubUpload();

        ImageUploadResult result = service.uploadImage(
                part(webp, "image/webp"),
                "profiles"
        );

        assertEquals("trips/sample", result.publicId());
        assertUploaded(webp, "profiles");
    }

    @Test
    void structurallyValidWebpWithInvalidVp8PayloadIsNotFullyDecoded()
            throws Exception {
        byte[] invalidVp8Payload = new byte[10];
        assertFalse(containsVp8KeyframeStartCode(invalidVp8Payload));
        assertFalse(
                ImageIO.getImageReadersByFormatName("webp").hasNext(),
                "No WebP decoder is available, so this payload cannot be fully decoded"
        );

        byte[] webp = webpContainer(invalidVp8Payload);
        stubUpload();

        ImageUploadResult result = service.uploadImage(
                part(webp, "image/webp"),
                "trips"
        );

        assertEquals(
                new ImageUploadResult(
                        "https://res.cloudinary.com/demo/image/upload/v1/sample.jpg",
                        "trips/sample"
                ),
                result
        );
        assertUploaded(webp, "trips");
    }

    @Test
    void textDeclaredAsJpegIsRejected() throws Exception {
        reject(part(TEXT, "image/jpeg"));
    }

    @Test
    void textDeclaredAsPngIsRejected() throws Exception {
        reject(part(TEXT, "image/png"));
    }

    @Test
    void textDeclaredAsWebpIsRejected() throws Exception {
        reject(part(TEXT, "image/webp"));
    }

    @Test
    void jpegDeclaredAsPngIsRejected() throws Exception {
        reject(part(raster("jpg", 2, 2), "image/png"));
    }

    @Test
    void pngDeclaredAsJpegIsRejected() throws Exception {
        reject(part(raster("png", 2, 2), "image/jpeg"));
    }

    @Test
    void truncatedJpegIsRejected() throws Exception {
        byte[] jpeg = raster("jpg", 2, 2);
        reject(part(slice(jpeg, Math.max(3, jpeg.length / 2)), "image/jpeg"));
    }

    @Test
    void truncatedPngIsRejected() throws Exception {
        byte[] png = raster("png", 2, 2);
        reject(part(slice(png, Math.max(8, png.length / 2)), "image/png"));
    }

    @Test
    void malformedWebpIsRejected() throws Exception {
        byte[] truncated = webpContainer(new byte[4]);
        // Claim a 10-byte VP8 payload while only 4 bytes are present.
        writeUint32Le(truncated, 16, 10);
        writeUint32Le(truncated, 4, truncated.length - 8L);

        reject(part(truncated, "image/webp"));
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadImage(file, "trips")
        );

        assertEquals("Please select an image.", exception.getMessage());
        verify(file, never()).getBytes();
        verify(uploader, never()).upload(any(), any());
    }

    @Test
    void nullFileIsRejected() throws Exception {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadImage(null, "trips")
        );

        assertEquals("Please select an image.", exception.getMessage());
        verify(uploader, never()).upload(any(), any());
    }

    @Test
    void fileLargerThanFiveMbIsRejected() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((5L * 1024L * 1024L) + 1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadImage(file, "trips")
        );

        assertEquals(
                "Image size must not exceed 5 MB.",
                exception.getMessage()
        );
        verify(file, never()).getBytes();
        verify(uploader, never()).upload(any(), any());
    }

    @Test
    void imageAtEdgeLimitIsAccepted() throws Exception {
        byte[] jpeg = raster("jpg", ImageUploadService.MAX_EDGE_PIXELS, 1);
        stubUpload();

        service.uploadImage(part(jpeg, "image/jpeg"), "trips");

        assertUploaded(jpeg, "trips");
    }

    @Test
    void imageExceedingEdgeLimitIsRejected() throws Exception {
        byte[] jpeg = raster("jpg", ImageUploadService.MAX_EDGE_PIXELS + 1, 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadImage(part(jpeg, "image/jpeg"), "trips")
        );

        assertEquals(
                "Image dimensions are not allowed.",
                exception.getMessage()
        );
        verify(uploader, never()).upload(any(), any());
    }

    @Test
    void imageExceedingPixelLimitIsRejected() throws Exception {
        int width = 4_000;
        int height = (int) (ImageUploadService.MAX_TOTAL_PIXELS / width) + 1;
        assertTrue(width <= ImageUploadService.MAX_EDGE_PIXELS);
        assertTrue(height <= ImageUploadService.MAX_EDGE_PIXELS);
        assertTrue((long) width * height > ImageUploadService.MAX_TOTAL_PIXELS);
        byte[] png = pngHeader(width, height);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadImage(part(png, "image/png"), "trips")
        );

        assertEquals(
                "Image dimensions are not allowed.",
                exception.getMessage()
        );
        verify(uploader, never()).upload(any(), any());
    }

    private void reject(MultipartFile file) throws Exception {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadImage(file, "trips")
        );

        assertEquals(
                "Only JPG, PNG and WEBP images are allowed.",
                exception.getMessage()
        );
        verify(uploader, never()).upload(any(), any());
    }

    private void assertUploaded(byte[] bytes, String folder) throws Exception {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);

        verify(uploader).upload(payload.capture(), options.capture());

        assertSame(bytes, payload.getValue());
        assertArrayEquals(bytes, (byte[]) payload.getValue());
        assertEquals(folder, options.getValue().get("folder"));
        assertEquals("image", options.getValue().get("resource_type"));
    }

    private void stubUpload() throws Exception {
        when(uploader.upload(any(), any())).thenReturn(cloudinaryResponse());
    }

    private MultipartFile part(byte[] bytes, String contentType) throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) bytes.length);
        when(file.getBytes()).thenReturn(bytes);
        when(file.getContentType()).thenReturn(contentType);
        return file;
    }

    private static Map<String, Object> cloudinaryResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put(
                "secure_url",
                "https://res.cloudinary.com/demo/image/upload/v1/sample.jpg"
        );
        response.put("public_id", "trips/sample");
        return response;
    }

    private static byte[] raster(String format, int width, int height)
            throws IOException {
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );
        image.setRGB(0, 0, 0x336699);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output));
        return output.toByteArray();
    }

    private static byte[] slice(byte[] bytes, int length) {
        byte[] sliced = new byte[length];
        System.arraycopy(bytes, 0, sliced, 0, length);
        return sliced;
    }

    private static boolean containsVp8KeyframeStartCode(byte[] payload) {
        byte[] startCode = {(byte) 0x9D, 0x01, 0x2A};
        if (payload.length < startCode.length) {
            return false;
        }
        for (int index = 0; index <= payload.length - startCode.length; index++) {
            boolean matches = true;
            for (int offset = 0; offset < startCode.length; offset++) {
                if (payload[index + offset] != startCode[offset]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    /**
     * PNG signature plus an IHDR chunk. Used to expose huge dimensions
     * without allocating the decoded bitmap. This is not a displayable PNG.
     */
    private static byte[] pngHeader(int width, int height) throws IOException {
        byte[] ihdr = new byte[13];
        writeInt(ihdr, 0, width);
        writeInt(ihdr, 4, height);
        ihdr[8] = 8;
        ihdr[9] = 2;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        });
        writeChunk(output, "IHDR", ihdr);
        writeChunk(output, "IEND", new byte[0]);
        return output.toByteArray();
    }

    private static void writeChunk(
            ByteArrayOutputStream output,
            String type,
            byte[] data
    ) throws IOException {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        writeInt(output, data.length);
        output.write(typeBytes);
        output.write(data);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        writeInt(output, (int) crc.getValue());
    }

    private static void writeInt(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >>> 24);
        target[offset + 1] = (byte) (value >>> 16);
        target[offset + 2] = (byte) (value >>> 8);
        target[offset + 3] = (byte) value;
    }

    private static void writeInt(ByteArrayOutputStream output, int value) {
        output.write((value >>> 24) & 0xFF);
        output.write((value >>> 16) & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }

    /**
     * Builds a RIFF container whose declared size matches its length.
     * The VP8 payload is not a decoded picture.
     */
    private static byte[] webpContainer(byte[] vp8Payload) {
        int riffSize = 4 + 8 + vp8Payload.length;
        byte[] file = new byte[8 + riffSize];
        file[0] = 'R';
        file[1] = 'I';
        file[2] = 'F';
        file[3] = 'F';
        writeUint32Le(file, 4, riffSize);
        file[8] = 'W';
        file[9] = 'E';
        file[10] = 'B';
        file[11] = 'P';
        file[12] = 'V';
        file[13] = 'P';
        file[14] = '8';
        file[15] = ' ';
        writeUint32Le(file, 16, vp8Payload.length);
        System.arraycopy(vp8Payload, 0, file, 20, vp8Payload.length);
        return file;
    }

    private static void writeUint32Le(byte[] target, int offset, long value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
        target[offset + 2] = (byte) (value >>> 16);
        target[offset + 3] = (byte) (value >>> 24);
    }
}
