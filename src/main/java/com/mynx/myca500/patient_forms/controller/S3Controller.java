package com.mynx.myca500.patient_forms.controller;


import com.mynx.myca500.patient_forms.exception.ValidationException;
import com.mynx.myca500.patient_forms.service.S3PresignedUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/presigned-forms")
public class S3Controller {

    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;
    private static final String FORMS_COMMON_FOLDER = "001";

    @GetMapping("/generate-upload-url")
    public String generateUploadUrl(
            @RequestParam String folder,
            @RequestParam String fileName) {

        if (!FORMS_COMMON_FOLDER.equals(folder)) {
            throw new ValidationException("Invalid folder specified.");
        }
        if (fileName == null || fileName.isBlank()
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new ValidationException("Invalid file name.");
        }

        return s3PresignedUrlService.createPreSignedPutUrl(FORMS_COMMON_FOLDER, fileName);
    }

    @GetMapping("/generate-download-url")
    public String generateDownloadUrl(
            @RequestParam String folder,
            @RequestParam String fileName) {

        if (!FORMS_COMMON_FOLDER.equals(folder)) {
            throw new ValidationException("Invalid folder specified.");
        }
        if (fileName == null || fileName.isBlank()
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new ValidationException("Invalid file name.");
        }

        return s3PresignedUrlService.createPreSignedGetUrl(FORMS_COMMON_FOLDER, fileName);
    }

}
