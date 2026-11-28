package com.mynx.myca500.patient_forms.service;


import com.mynx.myca500.patient_forms.config.ImageProperties;
import com.mynx.myca500.patient_forms.exception.ImageUploadException;
import com.mynx.myca500.patient_forms.exception.ValidationException;
import com.mynx.myca500.patient_forms.validation.S3Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateImageUploader {

    private static final Map<String, String> EXT = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/webp", "webp");

    private final S3Client s3Client;
    private final S3PresignedUrlService presignService;
   private final ImageProperties props;




    public Map<String, String> uploadAll(Map<String, MultipartFile> filesByRef) {
        Map<String, String> uploaded = new LinkedHashMap<>();
        try {
            filesByRef.forEach((ref, file) -> uploaded.put(ref, upload(file)));
            return uploaded;
        } catch (RuntimeException ex) {
            deleteQuietly(uploaded.values());
            throw ex;
        }
    }

    private String upload(MultipartFile file) {
        if (file.isEmpty()) throw new ValidationException("Image file is empty.");
        if (file.getSize() > props.maxBytes()) throw new ValidationException("Image exceeds maximum size.");

        String type;
        try (InputStream in = file.getInputStream()) {
            type = sniff(in.readNBytes(12));
        } catch (IOException e) {
            throw new ImageUploadException("Unable to read uploaded image", e);
        }
        if (type == null) throw new ValidationException("Unsupported image type. Allowed: PNG, JPEG, WEBP.");

        String fileName = UUID.randomUUID() + "." + EXT.get(type);
        String key = S3Constants.FORMS_COMMON_FOLDER + "/" + fileName;

        try (InputStream in = file.getInputStream()) {
            s3Client.putObject(
                    b -> b.bucket(presignService.getBucketName()).key(key).contentType(type),
                    RequestBody.fromInputStream(in, file.getSize()));
        } catch (IOException | SdkException e) {
            throw new ImageUploadException("S3 upload failed", e);
        }
        return fileName;
    }

    public void deleteQuietly(Collection<String> fileNames) {
        for (String f : fileNames) {
            try {
                s3Client.deleteObject(b -> b.bucket(presignService.getBucketName())
                        .key(S3Constants.FORMS_COMMON_FOLDER + "/" + f));
            } catch (Exception e) {
                log.warn("Orphaned S3 object (cleanup failed): {}/{}", S3Constants.FORMS_COMMON_FOLDER, f, e);
            }
        }
    }

    static String sniff(byte[] h) {
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "image/png";
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (h.length >= 12 && h[0]=='R' && h[1]=='I' && h[2]=='F' && h[3]=='F'
                && h[8]=='W' && h[9]=='E' && h[10]=='B' && h[11]=='P') return "image/webp";
        return null;
    }
}


