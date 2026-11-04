package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.commons.exceptions.AccessDeniedException;
import com.mynx.myca500.commons.exceptions.DuplicateResourceException;
import com.mynx.myca500.commons.exceptions.ResourceNotFoundException;
import com.mynx.myca500.commons.exceptions.UnauthorizedAccessException;
import com.mynx.myca500.commons.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@Slf4j
public class S3PresignedUrlService {

    @Value("${s3.bucket.name}")
    private String bucketName;

    @Value("${presigned.url.ttl}")
    private Long expiry;

    private final S3Presigner s3Presigner;

    public S3PresignedUrlService(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    public String createPreSignedPutUrl(String folder, String fileName) {
        try {
            log.info("Inside createPreSignedPutUrl()");
            String sanitizedBucketName = bucketName.replace("/", "");
            String key = folder + "/" + fileName;

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(builder -> builder
                            .bucket(sanitizedBucketName)
                            .key(key))
                    .build();

            PresignedPutObjectRequest preSignedRequest = s3Presigner.presignPutObject(presignRequest);
            return preSignedRequest.url().toExternalForm();
        } catch (ResourceNotFoundException e) {
            log.error("ResourceNotFoundException {}", e.getMessage(), e);
            throw new ResourceNotFoundException(e.getMessage());
        } catch (DuplicateResourceException e) {
            log.error("DuplicateResourceException {}", e.getMessage(), e);
            throw new DuplicateResourceException(e.getMessage());
        } catch (ValidationException e) {
            log.error("ValidationException {}", e.getMessage(), e);
            throw new ValidationException(e.getMessage());
        } catch (AccessDeniedException e) {
            log.error("AccessDeniedException {}", e.getMessage(), e);
            throw new AccessDeniedException(e.getMessage());
        } catch (UnauthorizedAccessException e) {
            log.error("UnauthorizedAccessException {}", e.getMessage(), e);
            throw new UnauthorizedAccessException(e.getMessage());
        } catch (Exception e) {
            log.error("Exception {}", e.getMessage(), e);
            throw new RuntimeException("Exception while generating presigned Put URL: " + e.getMessage(), e);
        }
    }

    public String createPreSignedGetUrl(String folder, String fileName) {
        try {
            log.info("Inside createPreSignedGetUrl()");
            String sanitizedBucketName = bucketName.replace("/", "");

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiry))
                    .getObjectRequest(builder -> builder
                            .bucket(sanitizedBucketName)
                            .key(folder + "/" + fileName))
                    .build();

            PresignedGetObjectRequest preSignedRequest = s3Presigner.presignGetObject(presignRequest);
            return preSignedRequest.url().toExternalForm();
        } catch (ResourceNotFoundException e) {
            log.error("ResourceNotFoundException {}", e.getMessage(), e);
            throw new ResourceNotFoundException(e.getMessage());
        } catch (DuplicateResourceException e) {
            log.error("DuplicateResourceException {}", e.getMessage(), e);
            throw new DuplicateResourceException(e.getMessage());
        } catch (ValidationException e) {
            log.error("ValidationException {}", e.getMessage(), e);
            throw new ValidationException(e.getMessage());
        } catch (AccessDeniedException e) {
            log.error("AccessDeniedException {}", e.getMessage(), e);
            throw new AccessDeniedException(e.getMessage());
        } catch (UnauthorizedAccessException e) {
            log.error("UnauthorizedAccessException {}", e.getMessage(), e);
            throw new UnauthorizedAccessException(e.getMessage());
        } catch (Exception e) {
            log.error("Exception {}", e.getMessage(), e);
            throw new RuntimeException("Exception while generating presigned Get URL: " + e.getMessage(), e);
        }
    }
}
